package org.duckdns.hjow.swingwebpanel.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Exercises Java-to-document messaging through the component's public entry point.
 * <p>
 * 컴포넌트 공개 진입점으로 Java에서 문서로 보내는 메시지를 검증한다.
 */
public final class HostOutboundMessageTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HostOutboundMessageTest() {
    }

    /**
     * Checks trusted event fields, ordered listeners and exact target origins.
     * <p>
     * 신뢰 이벤트 필드, 리스너의 메시지 순서와 정확한 대상 오리진을 확인한다.
     *
     * @throws Exception on setup, waiting or network failure / 환경 구성, 대기 또는 네트워크 실패 시
     */
    public void testTrustedDeliveryAndTargetOrigin() throws Exception {
        TestHttpServer site = new TestHttpServer();
        final SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        try {
            site.routeFixed("/page", TestHttpServer.Response.html("<script>"
                    + "let host=window.opener, seen=[];"
                    + "window.onmessage=e=>{sessionStorage.setItem('host-event',[e instanceof MessageEvent,e.type,e.origin,"
                    + "e.source===host,e.source.closed,e.ports.length,e.lastEventId,e.isTrusted,e.bubbles,e.cancelable].join('|'));"
                    + "window.opener=null;};"
                    + "addEventListener('message',e=>{seen.push(e.data);sessionStorage.setItem('host-order',seen.join('|'));});"
                    + "let denied;try{host.document;}catch(e){denied=e.name;}"
                    + "host.focus();host.close();host.blur();"
                    + "sessionStorage.setItem('host-ready',[typeof host.postMessage,host.closed,denied].join());</script>"));
            site.routeFixed("/plain", TestHttpServer.Response.ok("text/plain", "no scripting"));
            site.start();
            panel.setPage(site.url("/page"));
            FrameTestSupport.awaitStored(panel, "host-ready", "function,false,SecurityError");

            panel.postMessage("wrong port", "http://127.0.0.1:1");
            panel.postMessage("wrong scheme", site.url("/page").replace("http:", "https:"));
            panel.postMessage("opaque", "about:blank");
            panel.postMessage("exact", site.url("/another/path?x=1"));
            panel.postMessage("wildcard");
            FrameTestSupport.awaitStored(panel, "host-order", "exact|wildcard");
            FrameTestSupport.awaitStored(panel, "host-event", "true|message|" + SwingWebPanel.HOST_ORIGIN + "|true|false|0||true|false|false");

            // 상대 주소와 잘못된 포트가 발신 스레드에서 거부되는지 확인한다.
            for (final String invalid : new String[] {null, "", "/", "/page", "http://", "http://x:65536/"}) {
                Check.throwsException(IllegalArgumentException.class, new Block() {
                    public void run() {
                        panel.postMessage("invalid", invalid);
                    }
                }, "invalid Java target");
            }
            panel.setJavascriptEnabled(false);
            panel.postMessage("disabled");
            panel.setPage(site.url("/plain"));
            FrameTestSupport.awaitProgress(panel, 100);
            Check.equal(site.url("/plain"), panel.getPage(), "plain text committed");
            panel.postMessage("plain");
            Check.equal("exact|wildcard", FrameTestSupport.stored(panel, "host-order"), "script-disabled delivery is discarded");
        } finally {
            panel.dispose();
            site.stop();
        }
        Check.throwsException(IllegalStateException.class, new Block() {
            public void run() {
                panel.postMessage("after disposal");
            }
        }, "postMessage after disposal");
    }

    /**
     * Posts while the receiver's parser still owns the document, then cancels a second pending receiver.
     * <p>
     * 파서가 아직 수신 문서를 소유할 때 메시지를 보내고 두 번째 대기 수신 문서는 취소한다.
     *
     * @throws Exception on setup, waiting or network failure / 환경 구성, 대기 또는 네트워크 실패 시
     */
    public void testInstalledParserTargetAndCancelledMessage() throws Exception {
        TestHttpServer site = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        CountDownLatch nextRelease = new CountDownLatch(1);
        CountDownLatch cancelRelease = new CountDownLatch(1);
        try {
            site.routeFixed("/old", TestHttpServer.Response.html(receivingPage("old", null)));
            site.routeFixed("/next", TestHttpServer.Response.html(receivingPage("next", "/hold-next.js")));
            site.routeFixed("/cancel", TestHttpServer.Response.html(receivingPage("cancel", "/hold-cancel.js")));
            site.routeFixed("/final", TestHttpServer.Response.html(receivingPage("final", null)));
            CountDownLatch nextEntered = routeGate(site, "/hold-next.js", nextRelease);
            CountDownLatch cancelEntered = routeGate(site, "/hold-cancel.js", cancelRelease);
            site.start();
            panel.setPage(site.url("/old"));
            FrameTestSupport.awaitStored(panel, "old-ready", "yes");
            FrameTestSupport.awaitProgress(panel, 100);

            panel.setPage(site.url("/next"));
            Check.isTrue(nextEntered.await(10, TimeUnit.SECONDS), "the next parser started loading its blocking script");
            FrameTestSupport.awaitStored(panel, "next-ready", "yes");
            Check.equal(site.url("/old"), panel.getPage(), "old page remains confirmed");
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("tag", "captured");
            panel.postMessage(payload, site.url("/next"));
            payload.put("tag", "changed");
            Check.isNull(FrameTestSupport.stored(panel, "next-data"), "the parser's message is not delivered inline");
            nextRelease.countDown();
            FrameTestSupport.awaitStored(panel, "next-data", "captured");
            Check.isNull(FrameTestSupport.stored(panel, "old-data"), "the confirmed but superseded receiver got no copy");

            panel.setPage(site.url("/cancel"));
            Check.isTrue(cancelEntered.await(10, TimeUnit.SECONDS), "the cancellable parser is active");
            FrameTestSupport.awaitStored(panel, "cancel-ready", "yes");
            panel.postMessage("cancelled");
            panel.setPage(site.url("/final"));
            cancelRelease.countDown();
            FrameTestSupport.awaitStored(panel, "final-ready", "yes");
            panel.postMessage("final");
            FrameTestSupport.awaitStored(panel, "final-data", "final");
            Check.isNull(FrameTestSupport.stored(panel, "cancel-data"), "cancellation removed the queued message");
        } finally {
            nextRelease.countDown();
            cancelRelease.countDown();
            panel.dispose();
            site.stop();
        }
    }

    /**
     * Creates an HTTP page recording its host data and readiness.
     * <p>
     * 호스트 데이터와 준비 여부를 기록하는 HTTP 페이지를 만든다.
     *
     * @param key key prefix / 키 접두어
     * @param blocking optional parser-blocking script URL / 선택 파서 차단 스크립트 주소
     * @return page markup / 페이지 마크업
     */
    private static String receivingPage(String key, String blocking) {
        return "<script>onmessage=e=>{var k='" + key + "-data';var old=sessionStorage.getItem(k);"
                + "sessionStorage.setItem(k,(old===null?'':old+'|')+(typeof e.data==='object'?e.data.tag:e.data));};"
                + "sessionStorage.setItem('" + key + "-ready','yes');</script>"
                + (blocking == null ? "" : "<script src='" + blocking + "'></script>");
    }

    /**
     * Starts a gate for a parser-blocking script.
     * <p>
     * 파서를 막는 스크립트의 대기를 설정한다.
     *
     * @param site the HTTP server / HTTP 서버
     * @param path script request path / 스크립트의 요청 경로
     * @param release signals when the server may answer / 서버가 응답해도 되는 신호
     * @return signals when the request arrived / 요청이 도착한 신호
     */
    static CountDownLatch routeGate(TestHttpServer site, String path, final CountDownLatch release) {
        final CountDownLatch entered = new CountDownLatch(1);
        site.route(path, new TestHttpServer.Handler() {
            /** Pauses until the test releases the response. / 시험이 응답을 풀 때까지 기다린다. */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                entered.countDown();
                release.await(20, TimeUnit.SECONDS);
                return TestHttpServer.Response.ok("application/javascript", "void 0;");
            }
        });
        return entered;
    }
}
