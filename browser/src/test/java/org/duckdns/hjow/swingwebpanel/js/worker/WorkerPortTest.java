package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Transferring <code>MessagePort</code> objects through the real component (plan item P20-09-b; HTML Standard "Message ports", transfer steps):
 * a port in the transfer list of <code>postMessage</code> (of a worker, of a port, of a window and of <code>structuredClone</code>) arrives as a
 * new port of the receiving context, still entangled with the other end of its channel and with the messages that were waiting for it, started
 * only when the receiver starts it; the source object is detached and does nothing any more; ports of other realms (a page, a worker, another
 * worker) talk to each other without the page in between; and a port that is not in the transfer list, a closed port and a detached port cannot
 * be sent.
 * <p>
 * 실제 컴포넌트로 확인하는 <code>MessagePort</code> 객체의 전달(계획 P20-09-b. HTML 표준 "Message ports"의 전달 단계): (worker, 포트, window,
 * <code>structuredClone</code> 의) <code>postMessage</code> 전달 목록에 든 포트는 받는 문맥의 새 포트로 도착하며, 채널의 반대쪽 끝과 여전히 얽혀 있고 기다리던
 * 메시지를 가지며, 받는 쪽이 시작해야 전달을 시작한다. 원본 객체는 분리되어 더 이상 아무 일도 하지 않고, 다른 realm 의 포트(페이지, worker, 다른 worker)는
 * 페이지를 거치지 않고 서로 대화하며, 전달 목록에 없는 포트, 닫힌 포트, 분리된 포트는 보낼 수 없다.
 */
public class WorkerPortTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerPortTest() {
    }

    /**
     * A page gives one end of a channel to a worker: the messages that the page had posted before the transfer wait in the queue that travels
     * with the port and arrive, in order, only after the worker started the port; both directions work afterwards; the event of the worker
     * carries the port in <code>ports</code>; the port object of the page is detached.
     * <p>
     * 페이지가 채널의 한쪽 끝을 worker 에게 준다: 페이지가 전달 전에 보낸 메시지는 포트와 함께 가는 큐에서 기다리다가 worker 가 포트를 시작한 뒤에야 순서대로
     * 도착하고, 그 뒤에는 양방향이 동작하며, worker 의 이벤트는 <code>ports</code> 에 포트를 싣고, 페이지의 포트 객체는 분리된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageGivesOneEndOfAChannelToAWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var log = [];"
                    + "onmessage = function (e) {"
                    + " var p = e.ports[0]; log.push('init:' + e.ports.length + ':' + (p instanceof MessagePort) + ':' + (e.data === 'give'));"
                    + " setTimeout(function () {"
                    + "  log.push('before-start');"
                    + "  p.onmessage = function (m) { log.push('got:' + m.data); if (m.data === 'last') { p.postMessage('worker saw ' + log.join(',')); } };"
                    + " }, 150);"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var ch = new MessageChannel(); var mine = ch.port1;"
                    + "mine.postMessage('early1'); mine.postMessage('early2');"
                    + "var theirs = ch.port2;"
                    + "w.postMessage('give', [theirs]);"
                    + "var errAfter = (function () { try { w.postMessage('again', [theirs]); return 'no error'; } catch (e) { return e.name + ':' + e.code; } })();"
                    + "theirs.postMessage('into the void'); theirs.start(); theirs.close();"
                    + "mine.onmessage = function (e) { put('final', errAfter + '|' + e.data); };"
                    + "mine.postMessage('late'); setTimeout(function () { mine.postMessage('last'); }, 400);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("DataCloneError:25|worker saw init:1:true:true,before-start,got:early1,got:early2,got:late,got:last", WorkerTestSupport.await(panel, "final"),
                    "the queue travels with the port, delivery starts when the worker starts the port, the old object is detached, the worker answers through the port");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A worker creates a channel of its own and sends one end to the page: the page gets a port of its own realm in <code>ports</code>, and
     * messages go both ways, with structured clones; a port that is not in the transfer list is a <code>DataCloneError</code> (in the worker
     * too), and a closed port cannot be transferred.
     * <p>
     * worker 가 자기 채널을 만들어 한쪽 끝을 페이지에 보낸다: 페이지는 <code>ports</code> 에서 자기 realm 의 포트를 받고 메시지는 구조화 복제본으로 양방향으로
     * 오간다. 전달 목록에 없는 포트는 (worker 에서도) <code>DataCloneError</code> 이고 닫힌 포트는 전달할 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWorkerSendsOneEndOfItsOwnChannelToThePage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "var ch = new MessageChannel();"
                    + "ch.port1.onmessage = function (e) { ch.port1.postMessage({ echo: e.data.n * 2, list: e.data.list.concat(['w']) }); };"
                    + "var notListed = err(function () { postMessage({ p: ch.port2 }); });"
                    + "var closed = new MessageChannel(); closed.port1.close(); var closedErr = err(function () { postMessage('x', [closed.port1]); });"
                    + "postMessage({ hello: 'from worker', notListed: notListed, closedErr: closedErr }, [ch.port2]);"
                    + "var detachedErr = err(function () { postMessage('again', [ch.port2]); });"
                    + "postMessage({ detachedErr: detachedErr });"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var info = [];"
                    + "w.onmessage = function (e) {"
                    + " if (e.data.detachedErr) { info.push(e.data.detachedErr); return; }"
                    + " var p = e.ports[0]; info.push(e.data.notListed, e.data.closedErr, e.ports.length + ':' + (p instanceof MessagePort) + ':' + (p.constructor === MessagePort));"
                    + " p.onmessage = function (m) { put('result', info.join('|') + '|' + m.data.echo + ':' + m.data.list.join('/')); };"
                    + " p.postMessage({ n: 21, list: ['p'] }); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("DataCloneError:25|DataCloneError:25|1:true:true|DataCloneError:25|42:p/w", WorkerTestSupport.await(panel, "result"),
                    "ports of the worker arrive as ports of the page; errors for ports that cannot be sent");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Two workers talk through a channel that the page created and gave away, one end to each, without the page in between; the page can
     * close its own copy of nothing, and when one worker is terminated the other end is just silent.
     * <p>
     * 두 worker 가, 페이지가 만들어 한쪽 끝씩 나눠 준 채널로 페이지를 거치지 않고 대화한다. 한 worker 를 종료하면 반대쪽 끝은 조용해질 뿐이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTwoWorkersTalkThroughAChannelOfThePage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/a.js", WorkerTestSupport.js(
                    "onmessage = function (e) { var p = e.ports[0]; p.onmessage = function (m) { postMessage('A heard ' + m.data); p.postMessage('A->B ' + m.data); };"
                    + " postMessage('A ready'); };"));
            server.routeFixed("/b.js", WorkerTestSupport.js(
                    "onmessage = function (e) { var p = e.ports[0]; p.onmessage = function (m) { postMessage('B heard ' + m.data); };"
                    + " postMessage('B ready'); p.postMessage('hello from B'); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var a = new Worker('/a.js'), b = new Worker('/b.js'); var ch = new MessageChannel(); var log = [];"
                    + "function note(e) { log.push(e.data); if (log.length === 4) { log.sort(); put('log', log.join(',')); } }"
                    + "a.onmessage = note; b.onmessage = note;"
                    + "a.postMessage('port', [ch.port1]); b.postMessage('port', [ch.port2]);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("A heard hello from B,A ready,B heard A->B hello from B,B ready", WorkerTestSupport.await(panel, "log"),
                    "worker A and worker B talked to each other through the two ends of the channel");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The target origin filter of <code>window.postMessage</code> (both forms): <code>*</code>, <code>/</code> (the default of the options
     * form), the origin of the page itself and a URL of that origin deliver; another port, another scheme and another host are dropped without
     * an error; an unparseable target origin is a <code>SyntaxError</code>; the events of a worker have an empty origin.
     * <p>
     * <code>window.postMessage</code> 의 대상 오리진 걸러내기(두 형식): <code>*</code>, <code>/</code>(옵션 형식의 기본값), 페이지 자신의 오리진, 그 오리진의
     * URL은 전달되고, 다른 포트, 다른 스킴, 다른 호스트는 오류 없이 버려지며, 해석할 수 없는 대상 오리진은 <code>SyntaxError</code> 이다. worker 의 이벤트는
     * 오리진이 비어 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTargetOriginFilterOfWindowPostMessage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js("postMessage('from worker');"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "var log = []; var own = location.origin;"
                    + "window.addEventListener('message', function (e) { log.push(e.data + '@' + (e.origin === own)); });"
                    + "postMessage('star', '*'); postMessage('slash', '/'); postMessage('own', own); postMessage('ownPath', own + '/some/path?q#h');"
                    + "postMessage('optDefault', {}); postMessage('optStar', { targetOrigin: '*' }); postMessage('optOwn', { targetOrigin: own });"
                    + "postMessage('wrongPort', 'http://localhost:1'); postMessage('wrongScheme', 'https://' + location.host);"
                    + "postMessage('wrongHost', { targetOrigin: 'http://example.invalid' });"
                    + "var errs = [err(function () { postMessage('x', 'not a url'); }), err(function () { postMessage('x', { targetOrigin: 'bad' }); }),"
                    + " err(function () { postMessage('x'); })];"
                    + "var w = new Worker('/w.js'); var workerOrigin;"
                    + "w.onmessage = function (e) { workerOrigin = JSON.stringify(e.origin); };"
                    + "setTimeout(function () { put('filter', log.join(',') + '|' + errs.join(',') + '|' + workerOrigin); }, 700);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("star@true,slash@true,own@true,ownPath@true,optDefault@true,optStar@true,optOwn@true|SyntaxError:12,SyntaxError:12,TypeError:undefined|\"\"",
                    WorkerTestSupport.await(panel, "filter"), "the target origin filter; messages from a worker have an empty origin");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The page side: a port in the transfer list of <code>window.postMessage</code> (both forms, with the target origin filter),
     * <code>structuredClone</code> and <code>port.postMessage</code> arrives as a new port that is still entangled; a message to another
     * origin is dropped but the ports of its transfer list are given up; a port that is posted through its own other end is lost with the
     * message; a port in a message that is not in the transfer list is a <code>DataCloneError</code>.
     * <p>
     * 페이지 쪽: <code>window.postMessage</code>(두 형식, 대상 오리진 걸러내기), <code>structuredClone</code>, <code>port.postMessage</code> 의 전달 목록에 든
     * 포트는 여전히 얽힌 새 포트로 도착한다. 다른 오리진으로 가는 메시지는 버려지지만 전달 목록의 포트는 내놓는다. 자기 반대쪽 끝을 통해 보낸 포트는 메시지와 함께
     * 사라진다. 전달 목록에 없는 포트가 든 메시지는 <code>DataCloneError</code> 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPortsInWindowMessagesAndStructuredClone() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "var log = [];"
                    + "window.addEventListener('message', function (e) {"
                    + " var p = e.ports[0]; log.push('window:' + e.data + ':' + e.ports.length + ':' + (p instanceof MessagePort));"
                    + " if (p) { p.onmessage = function (m) { log.push('port:' + m.data); }; } });"
                    + "var c1 = new MessageChannel(); c1.port1.onmessage = function (m) { log.push('c1:' + m.data); };"
                    + "postMessage('legacy', '*', [c1.port2]);"
                    + "var c2 = new MessageChannel(); c2.port1.onmessage = function (m) { log.push('c2:' + m.data); };"
                    + "postMessage('options', { targetOrigin: '/', transfer: [c2.port2] });"
                    + "var c3 = new MessageChannel(); c3.port1.onmessage = function (m) { log.push('c3-NEVER:' + m.data); };"
                    + "postMessage('other origin', 'http://other.invalid', [c3.port2]);"
                    + "var c3gone = err(function () { c3.port2.postMessage('x', [c3.port2]); });"
                    + "var c4 = new MessageChannel(); var c4err = err(function () { postMessage({ p: c4.port2 }, '*'); });"
                    + "var c5 = new MessageChannel(); var cloned = structuredClone({ p: c5.port2, n: 1 }, { transfer: [c5.port2] });"
                    + "c5.port1.postMessage('to the clone'); cloned.p.onmessage = function (m) { log.push('clone:' + m.data); };"
                    + "var doomed = new MessageChannel(); doomed.port1.onmessage = function () { log.push('DOOMED'); }; doomed.port2.onmessage = function () { log.push('DOOMED2'); };"
                    + "doomed.port1.postMessage('lost', [doomed.port2]);"
                    + "setTimeout(function () { c1.port1.postMessage('to c1 port'); }, 20);"
                    + "setTimeout(function () { var l = log.slice().sort(); put('log', [c3gone, c4err, (cloned.p instanceof MessagePort), l.join(',')].join('|')); }, 700);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("DataCloneError:25|DataCloneError:25|true|clone:to the clone,port:to c1 port,window:legacy:1:true,window:options:1:true",
                    WorkerTestSupport.await(panel, "log"), "ports travel through window messages (both forms) and structuredClone; other origin and lost channels deliver nothing");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
