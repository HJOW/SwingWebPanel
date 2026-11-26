package org.duckdns.hjow.swingwebpanel.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.WebMessageListener;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Exercises document-to-host messaging and replies through a SwingWebPanel.
 * <p>
 * SwingWebPanel에서 문서발 호스트 메시지와 응답을 검증한다.
 */
public final class HostInboundMessageTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HostInboundMessageTest() {
    }

    /**
     * Checks script-ready signaling, EDT replies and event fields.
     * <p>
     * 스크립트 준비 신호, EDT에서의 응답과 이벤트 필드를 확인한다.
     *
     * @throws Exception on network or waiting failure / 네트워크 또는 대기 실패 시
     */
    public void testRoundTripFromEdt() throws Exception {
        TestHttpServer site = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        try {
            site.routeFixed("/room", TestHttpServer.Response.html("<script>var openerAtStart=opener;"
                    + "onmessage=e=>e.source.postMessage({data:e.data,host:e.origin,same:e.source===openerAtStart},e.origin);"
                    + "openerAtStart.postMessage('ready','*');</script>"));
            site.start();
            final Inbox inbox = new Inbox();
            final String allowed = site.url("");
            panel.addWebMessageListener(new WebMessageListener() {
                /** Replies from the EDT to the document that sent its readiness signal. / 준비 신호를 보낸 문서에 EDT에서 응답한다. */
                public void messageReceived(WebMessageEvent event) {
                    if ("ready".equals(event.getData()) && allowed.equals(event.getOrigin())) {
                        event.reply("welcome");
                    }
                }
            });
            panel.addWebMessageListener(inbox);
            long started = System.currentTimeMillis();
            panel.setPage(site.url("/room"));
            WebMessageEvent ready = inbox.next();
            Check.equal("ready", ready.getData(), "first message");
            Check.same(panel, ready.getSource(), "Swing component source");
            Check.equal(site.url(""), ready.getOrigin(), "serialized HTTP origin");
            Check.equal(site.url("/room"), ready.getDocumentUrl(), "original document URL");
            Check.equal(WebMessageEvent.SourceKind.DOCUMENT, ready.getSourceKind(), "top-level sender");
            Check.isTrue(ready.getTimestamp() >= started && ready.getTimestamp() <= System.currentTimeMillis(), "epoch milliseconds");
            WebMessageEvent reply = inbox.next();
            Check.isTrue(reply.getSequence() > ready.getSequence(), "component acceptance order");
            Map<?, ?> echo = (Map<?, ?>) reply.getData();
            Check.equal("welcome", echo.get("data"), "Java reply from listener");
            Check.equal(SwingWebPanel.HOST_ORIGIN, echo.get("host"), "host origin in the received JavaScript event");
            Check.equal(Boolean.TRUE, echo.get("same"), "same host window as the opener");
            Check.isTrue(inbox.allEdt, "all listeners run on the EDT");
        } finally {
            panel.dispose();
            site.stop();
        }
    }

    /**
     * Checks script target validation, buffer transfer and clone failures.
     * <p>
     * 스크립트 대상 검사, 버퍼 전달과 복제 실패를 확인한다.
     *
     * @throws Exception on network or waiting failure / 네트워크 또는 대기 실패 시
     */
    public void testTargetOptionsAndTransfers() throws Exception {
        TestHttpServer site = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        try {
            site.routeFixed("/send", TestHttpServer.Response.html("<script>"
                    + "const host=opener, origin='" + SwingWebPanel.HOST_ORIGIN + "';"
                    + "const f=()=>1;let result=[];function check(fn){try{fn();return 'ok';}catch(e){return e.name;}}"
                    + "host.postMessage('drop default');host.postMessage('drop slash','/');"
                    + "host.postMessage('drop other','https://example.test');"
                    + "result.push(check(()=>host.postMessage('relative','relative')));"
                    + "const channel=new MessageChannel(), bad=new ArrayBuffer(3);"
                    + "result.push(check(()=>host.postMessage(f,'*',[bad])),String(bad.detached));"
                    + "result.push(check(()=>host.postMessage('port',{targetOrigin:origin,transfer:[channel.port1]})));"
                    + "const b=new ArrayBuffer(3);new Uint8Array(b).set([1,7,9]);"
                    + "host.postMessage(b,{targetOrigin:origin,transfer:[b]});result.push(String(b.detached));"
                    + "host.postMessage(result.join(','),origin);channel.port1.close();channel.port2.close();</script>"));
            site.start();
            Inbox inbox = new Inbox();
            panel.addWebMessageListener(inbox);
            panel.setPage(site.url("/send"));
            WebMessageEvent bytes = inbox.next();
            Check.isTrue(bytes.getData() instanceof byte[], "only the matching transfer arrived");
            Check.isTrue(Arrays.equals(new byte[] {1, 7, 9}, (byte[]) bytes.getData()), "transferred bytes");
            Check.equal("SyntaxError,DataCloneError,false,DataCloneError,true", inbox.next().getData(),
                    "target, failed transfer and successful transfer results");
        } finally {
            panel.dispose();
            site.stop();
        }
    }

    /**
     * Replies to two frames and checks that a replacement never receives a reply to its old document.
     * <p>
     * 프레임 두 개에 응답하고 이동 뒤 새 문서에 이전 문서의 응답이 닿지 않는지 검사한다.
     *
     * @throws Exception on network or waiting failure / 네트워크 또는 대기 실패 시
     */
    public void testFrameRepliesAndReplacement() throws Exception {
        TestHttpServer root = new TestHttpServer();
        TestHttpServer external = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        try {
            external.routeFixed("/frame", TestHttpServer.Response.html(framePage("external")));
            external.start();
            root.routeFixed("/same", TestHttpServer.Response.html(framePage("same")));
            root.routeFixed("/moved", TestHttpServer.Response.html(framePage("moved")));
            root.routeFixed("/root", TestHttpServer.Response.html("<iframe id=s src='/same'></iframe>"
                    + "<iframe src='" + external.url("/frame") + "'></iframe>"
                    + "<script>onmessage=e=>{if(e.data==='move')document.getElementById('s').src='/moved';};</script>"));
            root.start();
            Inbox inbox = new Inbox();
            panel.addWebMessageListener(inbox);
            panel.setPage(root.url("/root"));
            Map<Object, WebMessageEvent> frames = new LinkedHashMap<Object, WebMessageEvent>();
            WebMessageEvent first = inbox.next();
            WebMessageEvent second = inbox.next();
            frames.put(first.getData(), first);
            frames.put(second.getData(), second);
            WebMessageEvent same = frames.get("same:true");
            WebMessageEvent other = frames.get("external:true");
            Check.notNull(same, "same-origin frame can use top.opener and has a null own opener");
            Check.notNull(other, "cross-origin frame can use top.opener and has a null own opener");
            Check.equal(WebMessageEvent.SourceKind.FRAME, other.getSourceKind(), "frame source kind");
            Check.equal(external.url(""), other.getOrigin(), "cross-origin frame sender origin");
            Check.equal(external.url("/frame"), other.getDocumentUrl(), "cross-origin frame URL");
            same.reply("one");
            other.reply("two");
            Object a = inbox.next().getData();
            Object b = inbox.next().getData();
            Check.isTrue(Arrays.asList(a, b).containsAll(Arrays.asList("same:one", "external:two")), "each reply reached its own frame");

            panel.postMessage("move", root.url("/root"));
            WebMessageEvent refreshed = inbox.next();
            Check.equal("moved:true", refreshed.getData(), "the second frame document is installed");
            same.reply("stale");
            refreshed.reply("fresh");
            Check.equal("moved:fresh", inbox.next().getData(), "a reply bound to the old document was discarded");
        } finally {
            panel.dispose();
            root.stop();
            external.stop();
        }
    }

    /**
     * Generates a page that announces its frame and echoes replies.
     * <p>
     * 자신의 프레임을 알리고 응답을 되돌려 보내는 페이지를 만든다.
     *
     * @param label source label / 발신 이름
     * @return document markup / 문서 마크업
     */
    private static String framePage(String label) {
        return "<script>onmessage=e=>e.source.postMessage('" + label + ":'+e.data,e.origin);"
                + "top.opener.postMessage('" + label + ":'+(opener===null),'*');</script>";
    }

    /**
     * Thread-safe inbox of events observed on the EDT.
     * <p>
     * EDT에서 받은 이벤트를 스레드 안전하게 모으는 수신함.
     */
    static final class Inbox implements WebMessageListener {
        /** Events in delivery order. / 전달 순서의 이벤트. */
        private final BlockingQueue<WebMessageEvent> events = new LinkedBlockingQueue<WebMessageEvent>();
        /** Whether all observed calls were on the EDT. / 모든 호출이 EDT였는지 여부. */
        volatile boolean allEdt = true;

        /** Records one EDT delivery. / EDT 전달 하나를 기록한다. */
        public void messageReceived(WebMessageEvent event) {
            allEdt &= SwingUtilities.isEventDispatchThread();
            events.add(event);
        }

        /**
         * Waits for the next event.
         * <p>
         * 다음 이벤트를 기다린다.
         *
         * @return delivered event / 전달된 이벤트
         * @throws InterruptedException when interrupted / 인터럽트되면
         */
        WebMessageEvent next() throws InterruptedException {
            WebMessageEvent event = events.poll(15, TimeUnit.SECONDS);
            Check.notNull(event, "a web message arrived");
            return event;
        }
    }
}
