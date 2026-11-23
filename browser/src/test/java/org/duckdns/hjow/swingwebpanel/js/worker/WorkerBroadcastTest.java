package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * <code>BroadcastChannel</code> through the real component (plan item P20-09-b; HTML Standard "Broadcasting to other browsing contexts"):
 * channels of the same name in the page and in its workers hear each other and nobody hears itself, other names are not heard, messages
 * are structured clones that arrive as <code>message</code> events of the receiving context (never during the run of the sender) with the
 * origin of the sender, a closed channel is silent and refuses to post, the channels of a terminated worker are gone, and a new page does
 * not hear the workers of the page before.
 * <p>
 * 실제 컴포넌트로 확인하는 <code>BroadcastChannel</code>(계획 P20-09-b. HTML 표준 "Broadcasting to other browsing contexts"): 페이지와 그 worker 의 같은
 * 이름 채널은 서로 듣고 자기 자신은 듣지 않으며, 다른 이름은 듣지 않고, 메시지는 보낸 쪽의 오리진과 함께 받는 문맥의 <code>message</code> 이벤트로(보내는 쪽이
 * 실행되는 동안에는 절대 아님) 도착하는 구조화 복제본이고, 닫힌 채널은 조용하며 보내기를 거부하고, 종료된 worker 의 채널은 사라지며, 새 페이지는 이전
 * 페이지의 worker 를 듣지 않는다.
 */
public class WorkerBroadcastTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerBroadcastTest() {
    }

    /**
     * A page and a worker talk through a channel: the structured clone arrives with the origin of the sender, the sender does not hear itself,
     * another name is not heard, the message is not delivered during the run of the sender, and the order is kept.
     * <p>
     * 페이지와 worker 가 채널로 대화한다: 구조화 복제본이 보낸 쪽의 오리진과 함께 도착하고, 보낸 쪽은 자기 메시지를 듣지 않으며, 다른 이름은 듣지 않고,
     * 메시지는 보내는 쪽이 실행되는 동안 전달되지 않으며, 순서가 유지된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageAndWorkerTalkThroughAChannel() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var bc = new BroadcastChannel('talk'); var other = new BroadcastChannel('other'); var got = [];"
                    + "bc.onmessage = function (e) { got.push(typeof e.data === 'object' ? e.data.n + ':' + e.data.list.length : e.data);"
                    + " if (e.data === 'ping') { bc.postMessage({ reply: 'pong', origin: e.origin, sameTarget: e.target === bc, ports: e.ports.length, source: String(e.source) }); }"
                    + " if (e.data === 'report') { bc.postMessage({ reply: 'report', got: got.join(',') }); } };"
                    + "other.onmessage = function (e) { got.push('OTHER'); };"
                    + "var selfHeard = 0; bc.addEventListener('message', function (e) { if (e.data && e.data.reply) { selfHeard++; } });"
                    + "bc.postMessage({ reply: 'ready', name: bc.name });"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var bc = new BroadcastChannel('talk'); var replies = []; var pageHeard = [];"
                    + "bc.onmessage = function (e) { var d = e.data; if (!d.reply) { return; } pageHeard.push(d.reply);"
                    + " if (d.reply === 'ready') { var list = [1, 2]; bc.postMessage('ping'); bc.postMessage({ n: 7, list: list }); list.push(3); put('syncAfterPost', pageHeard.join(',') || 'nothing'); }"
                    + " else if (d.reply === 'pong') { replies.push([d.origin === location.origin, d.sameTarget, d.ports, d.source].join('/')); bc.postMessage('report'); }"
                    + " else if (d.reply === 'report') { put('final', replies.join(';') + '|' + d.got + '|' + pageHeard.join(',')); } };"
                    + "var oc = new BroadcastChannel('nobody'); oc.postMessage('not heard by anyone');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("ready", WorkerTestSupport.await(panel, "syncAfterPost"), "nothing was delivered to the page while its own script ran after posting");
            Check.equal("true/true/0/null|ping,7:2,report|ready,pong,report", WorkerTestSupport.await(panel, "final"),
                    "origin, target, ports and source of the event; the clone was taken at posting time; order kept; other names are not heard; nobody hears itself");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Two workers of a page hear each other and the page hears both; a closed channel is silent, refuses to post
     * (<code>InvalidStateError</code>) and can be closed again; a terminated worker is no longer a receiver.
     * <p>
     * 한 페이지의 두 worker 는 서로 듣고 페이지는 둘 다 듣는다. 닫힌 채널은 조용하고 보내기를 거부하며(<code>InvalidStateError</code>) 다시 닫을 수 있다.
     * 종료된 worker 는 더 이상 받는 쪽이 아니다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWorkersHearEachOtherAndClosedChannelsAreSilent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var bc = new BroadcastChannel('room'); var mine = ''; var heard = [];"
                    + "onmessage = function (e) { mine = e.data; bc.postMessage('hello from ' + mine); postMessage('ready:' + mine); };"
                    + "postMessage('up');"
                    + "bc.onmessage = function (e) { heard.push(e.data); postMessage('heard:' + mine + ':' + e.data); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var a = new Worker('/w.js'); var b = new Worker('/w.js'); var log = [];"
                    + "var bc = new BroadcastChannel('room'); var late = new BroadcastChannel('room');"
                    + "var closedPost = (function () { late.close(); late.close(); try { late.postMessage('x'); return 'no error'; } catch (e) { return e.name + ':' + e.code; } })();"
                    + "late.onmessage = function () { log.push('LATE-CLOSED-HEARD'); };"
                    + "bc.onmessage = function (e) { log.push('page:' + e.data); };"
                    + "var ups = 0;"
                    + "function note(e) { log.push(e.data); if (e.data === 'up' && ++ups === 2) { a.postMessage('A'); b.postMessage('B');"
                    + " setTimeout(function () { a.terminate(); bc.postMessage('after-terminate'); }, 700);"
                    + " setTimeout(function () { log.sort(); put('log', closedPost + '|' + log.join(',')); }, 1400); } }"
                    + "a.onmessage = note; b.onmessage = note;")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("InvalidStateError:11|heard:A:hello from B,heard:B:after-terminate,heard:B:hello from A,page:hello from A,page:hello from B,ready:A,ready:B,up,up",
                    WorkerTestSupport.await(panel, "log"),
                    "workers and page hear each other, the closed channel is silent, the terminated worker heard nothing after termination");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The channels of a page live and die with it: a page that was left (navigation) takes its workers and channels with it, so the next
     * page neither hears them nor is heard by them, and the channels of a worker cannot be created beyond the limit.
     * <p>
     * 페이지의 채널은 페이지와 운명을 같이 한다: 떠난 페이지(이동)는 worker 와 채널을 함께 가져가므로 다음 페이지는 그것을 듣지도 들리지도 않으며, worker 의
     * 채널은 한도를 넘어서 만들 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChannelsEndWithThePageAndAreLimited() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var bc = new BroadcastChannel('page-one'); bc.onmessage = function (e) { postMessage('WORKER-HEARD:' + e.data); };"
                    + "setInterval(function () { bc.postMessage('from the old worker'); }, 10); postMessage('up');"));
            server.routeFixed("/one", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); w.onmessage = function (e) { if (e.data === 'up') { put('one', 'up'); } };")));
            server.routeFixed("/two", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var bc = new BroadcastChannel('page-one'); var heard = 0; bc.onmessage = function () { heard++; };"
                    + "var made = 0, err = ''; var keep = [];"
                    + "try { for (var i = 0; i < 5000; i++) { keep.push(new BroadcastChannel('many')); made++; } } catch (e) { err = e.name + ':' + e.code; }"
                    + "setTimeout(function () { bc.postMessage('from page two'); }, 10);"
                    + "setTimeout(function () { put('two', heard + '|' + made + '|' + err); }, 700);")));
            server.start();
            panel.setPage(server.url("/one"));
            Check.equal("up", WorkerTestSupport.await(panel, "one"), "page one started its worker");
            panel.setPage(server.url("/two"));
            // 두 번째 페이지가 확정되고 결과가 올라올 때까지 기다린다.
            Check.equal("0|999|QuotaExceededError:22", WorkerTestSupport.await(panel, "two"),
                    "the old worker is gone with its page and the channels of page two are limited (the one of its own counts)");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
