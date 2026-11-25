package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of messaging in scripts (plan item P20-09, first part): <code>structuredClone()</code>, <code>window.postMessage</code> with the
 * target origin rules and the transfer list, <code>MessageEvent</code>, <code>MessageChannel</code> and <code>MessagePort</code> (queue,
 * <code>start</code>, <code>onmessage</code>, <code>close</code>, order, both directions), and that every message is delivered later as a task
 * of the page and never while the sender runs.
 * <p>
 * 스크립트의 메시징 시험(계획 P20-09 의 첫 부분): <code>structuredClone()</code>, 대상 오리진 규칙과 전달 목록이 있는 <code>window.postMessage</code>,
 * <code>MessageEvent</code>, <code>MessageChannel</code>, <code>MessagePort</code>(큐, <code>start</code>, <code>onmessage</code>, <code>close</code>, 순서,
 * 양방향), 모든 메시지가 보내는 쪽이 실행되는 동안이 아니라 나중에 페이지의 작업으로 전달됨.
 */
public class MessagingApiTest {

    /**
     * The fixture of the page of the case.
     * <p>
     * 사례 페이지의 도구.
     */
    private PageFixture fixture;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MessagingApiTest() {
    }

    /**
     * Loads a small page before each case.
     * <p>
     * 사례마다 작은 페이지를 로드한다.
     */
    public void setUp() {
        fixture = new PageFixture();
        fixture.load("http://example.test/dir/page.html", "<!doctype html><body><p>page</p></body>");
    }

    /**
     * Evaluates a script that must not throw and returns its completion value as text.
     * <p>
     * 던지지 않아야 하는 스크립트를 실행해 완료 값을 문자열로 반환한다.
     *
     * @param source the script / 스크립트
     * @return the text / 문자열
     */
    private String run(String source) {
        String result = fixture.scripts.evaluate(source, "test");
        Check.notNull(result, "the script threw: " + source + " " + fixture.console);
        return result;
    }

    /**
     * Runs the tasks that are due (the delivery of messages).
     * <p>
     * 실행할 때가 된 작업(메시지 전달)을 실행한다.
     */
    private void deliver() {
        fixture.advance(0);
    }

    /**
     * Returns the text of the error that a statement throws (name, code).
     * <p>
     * 문장이 던지는 오류의 내용(이름, 코드)을 반환한다.
     *
     * @param statement the statement / 문장
     * @return <code>name:code</code>, or "no error" / <code>이름:코드</code>. 오류가 없으면 "no error"
     */
    private String errorOf(String statement) {
        return run("(function () { try { " + statement + "; return 'no error'; } catch (e) { return e.name + ':' + e.code; } })()");
    }

    /**
     * structuredClone: deep copy with cycles, DataCloneError, the transfer option that detaches a buffer.
     * <p>
     * structuredClone: 순환이 있는 깊은 복사, DataCloneError, 버퍼를 분리하는 transfer 옵션.
     */
    public void testStructuredClone() {
        Check.equal("true:false:true:2:true", run("var src = { a: [1, { b: 2 }], d: new Date(5), m: new Map([[1, 'x']]) }; src.self = src;"
                + " var c = structuredClone(src); (c.self === c) + ':' + (c === src) + ':' + (c.d instanceof Date) + ':' + c.a[1].b + ':' + (c.m.get(1) === 'x')"),
                "a copy with a cycle");
        Check.equal("true:true", run("(c.a !== src.a) + ':' + (c.a[1].b === src.a[1].b)"), "the copy is separate but equal");
        Check.equal("DataCloneError:25", errorOf("structuredClone(function () {})"), "a function");
        Check.equal("DataCloneError:25", errorOf("structuredClone({ s: Symbol('x') })"), "a symbol");
        Check.equal("DataCloneError:25", errorOf("structuredClone(new Error('node',{cause:document.createElement('div')}))"),
                "an unserializable host object in an error cause");
        Check.equal("TypeError:undefined", errorOf("structuredClone()"), "no argument");
        Check.equal("8:0:true", run("var buf = new ArrayBuffer(8); var moved = structuredClone(buf, { transfer: [buf] });"
                + " moved.byteLength + ':' + buf.byteLength + ':' + (moved instanceof ArrayBuffer)"), "transfer detaches the source");
        Check.equal("DataCloneError:25", errorOf("var b2 = new ArrayBuffer(2); structuredClone(b2, { transfer: [b2, b2] })"), "duplicate transfer entries");
        Check.equal("DataCloneError:25", errorOf("structuredClone({}, { transfer: [{}] })"), "a plain object is not transferable");
        Check.equal("DataCloneError:25", errorOf("var detached = new ArrayBuffer(1); structuredClone(detached, { transfer: [detached] }); structuredClone(detached)"),
                "a detached buffer cannot be cloned");
    }

    /**
     * window.postMessage: delivery as a later task with a MessageEvent that carries a copy, the origin and the source; onmessage handler.
     * <p>
     * window.postMessage: 사본, 오리진, 소스를 실은 MessageEvent 와 함께 나중의 작업으로 전달됨. onmessage 핸들러.
     */
    public void testWindowPostMessage() {
        Check.equal("sent|", run("var log = []; var payload = { n: [1, 2], when: new Date(7) };"
                + " window.addEventListener('message', function (e) { log.push('got:' + e.data.n[1] + ':' + (e.data !== payload) + ':' + e.origin + ':' + (e.source === window)"
                + " + ':' + e.ports.length + ':' + (e instanceof MessageEvent) + ':' + e.bubbles + ':' + e.cancelable + ':' + e.isTrusted + ':' + e.type + ':' + (e.data.when instanceof Date)); });"
                + " postMessage(payload, '*'); log.push('sent'); log.join('|') + '|'"), "the message is not delivered while the sender runs");
        deliver();
        Check.equal("sent|got:2:true:http://example.test:true:0:true:false:false:true:message:true", run("log.join('|')"), "delivered later");
        run("var order = []; window.onmessage = function (e) { order.push(e.data); }; postMessage('a', '/'); postMessage('b', 'http://example.test/some/path');");
        deliver();
        Check.equal("a|b", run("order.join('|')"), "the handler property gets the messages in order, '/' and a URL of the same origin send");
        run("order.length = 0; postMessage('lost', 'http://other.test'); postMessage('lost2', 'https://example.test');");
        deliver();
        Check.equal("0", run("String(order.length)"), "a message for another origin is dropped silently");
    }

    /**
     * window.postMessage errors: an invalid target origin, a missing argument and values that cannot be sent.
     * <p>
     * window.postMessage 오류: 잘못된 대상 오리진, 인수 누락, 보낼 수 없는 값.
     */
    public void testWindowPostMessageErrors() {
        Check.equal("SyntaxError:12", errorOf("postMessage('x', 'not a url')"), "the target origin must be *, / or an absolute URL");
        Check.equal("SyntaxError:12", errorOf("postMessage('x', '')"), "an empty target origin");
        Check.equal("TypeError:undefined", errorOf("postMessage('x')"), "the target origin is required");
        Check.equal("DataCloneError:25", errorOf("postMessage(function () {}, '*')"), "a function cannot be sent");
        Check.equal("DataCloneError:25", errorOf("var bb = new ArrayBuffer(4); postMessage(bb, '*', [bb, bb])"), "duplicate entries in the transfer list");
        Check.equal("0", run("var got = []; onmessage = function (e) { got.push(e.data); }; var tb = new ArrayBuffer(4); new Uint8Array(tb).set([1, 2, 3, 4]);"
                + " postMessage(tb, '*', [tb]); String(tb.byteLength)"), "a transferred buffer is detached at once");
        deliver();
        Check.equal("4:1,2,3,4", run("got[0].byteLength + ':' + Array.prototype.join.call(new Uint8Array(got[0]), ',')"), "and arrives with its bytes");
    }

    /**
     * MessageChannel: ports are entangled, messages wait until a port starts, start() and onmessage deliver them in order, both directions.
     * <p>
     * MessageChannel: 포트는 서로 얽혀 있고, 메시지는 포트가 시작할 때까지 기다리며, start() 와 onmessage 가 순서대로 전달한다. 양방향.
     */
    public void testChannelQueueAndStart() {
        run("var ch = new MessageChannel(); var p1 = ch.port1, p2 = ch.port2;");
        Check.equal("true:true:[object MessagePort]:[object MessageChannel]", run("(ch.port1 === p1) + ':' + (p1 !== p2) + ':' + Object.prototype.toString.call(p1)"
                + " + ':' + Object.prototype.toString.call(ch)"), "the channel");
        Check.equal("true", run("String(p1 instanceof EventTarget)"), "a port is an event target");
        run("var seen = []; p2.addEventListener('message', function (e) { seen.push((e.target === p2) + ':' + JSON.stringify(e.data)); }); p1.postMessage('one');"
                + " p1.postMessage({ k: 2 });");
        deliver();
        Check.equal("0", run("String(seen.length)"), "nothing is delivered before the port starts");
        run("p2.start();");
        Check.equal("0", run("String(seen.length)"), "start() does not deliver while the caller runs");
        deliver();
        Check.equal("true:\"one\",true:{\"k\":2}", run("seen.join(',')"), "both queued messages arrive after start(), in order");
        run("var back = []; p1.onmessage = function (e) { back.push(e.data.who); }; p2.postMessage({ who: 'two' });");
        deliver();
        Check.equal("two", run("back.join(',')"), "assigning onmessage starts the port, messages go both ways");
        run("p2.postMessage({ who: 'three' }); p2.postMessage({ who: 'four' }); p2.postMessage({ who: 'five' });");
        deliver();
        Check.equal("two,three,four,five", run("back.join(',')"), "more messages arrive in order");
        run("var ev = []; p1.onmessage = function (e) { ev.push(e); }; p2.postMessage(5);");
        deliver();
        Check.equal("5:true:message:0:true:", run("var e0 = ev[0]; e0.data + ':' + (e0 instanceof MessageEvent) + ':' + e0.type + ':' + e0.ports.length + ':' + (e0.source === null)"
                + " + ':' + e0.origin"), "the event of a channel message");
    }

    /**
     * Messages are copies, transfer lists move buffers and ports, and a port cannot be transferred through itself.
     * <p>
     * 메시지는 사본이고, 전달 목록은 버퍼와 포트를 옮기며, 포트는 자기 자신을 통해 전달할 수 없다.
     */
    public void testChannelMessagesAreCopiesAndTransfers() {
        run("var ch = new MessageChannel(); var got = []; ch.port2.onmessage = function (e) { got.push(e.data); }; var obj = { list: [1, 2] }; ch.port1.postMessage(obj);"
                + " obj.list.push(3);");
        Check.equal("0", run("String(got.length)"), "nothing is delivered while the sender runs");
        deliver();
        Check.equal("2:true", run("got[0].list.length + ':' + (got[0] !== obj)"), "the copy was made when the message was posted");
        run("var bf = new ArrayBuffer(3); ch.port1.postMessage(bf, [bf]);");
        Check.equal("0", run("String(bf.byteLength)"), "the buffer is detached when it is posted");
        deliver();
        Check.equal("3", run("String(got[1].byteLength)"), "and it arrives");
        Check.equal("no error", errorOf("var other = new MessageChannel(); ch.port1.postMessage('x', [other.port1])"), "a port can be transferred (P20-09-b; the transfer is tested in WorkerPortTest)");
        Check.equal("DataCloneError:25", errorOf("ch.port1.postMessage('x', [ch.port1])"), "a port cannot be transferred through itself");
        Check.equal("DataCloneError:25", errorOf("ch.port1.postMessage(function () {})"), "a function cannot be sent");
        Check.equal("TypeError:undefined", errorOf("ch.port1.postMessage()"), "a message is required");
    }

    /**
     * close(): the closed port delivers nothing more and sends nothing; messages posted to the other port earlier stay deliverable.
     * <p>
     * close(): 닫힌 포트는 더 이상 전달하거나 보내지 않으며, 이전에 반대쪽 포트로 보낸 메시지는 전달될 수 있다.
     */
    public void testChannelClose() {
        run("var ch = new MessageChannel(); var rec1 = [], rec2 = []; ch.port1.onmessage = function (e) { rec1.push(e.data); }; ch.port2.onmessage = function (e) { rec2.push(e.data); };"
                + " ch.port1.postMessage('before close'); ch.port1.close(); ch.port1.postMessage('after close'); ch.port2.postMessage('to a closed port');");
        deliver();
        Check.equal("before close", run("rec2.join('|')"), "the message that was posted before close() still arrives");
        Check.equal("", run("rec1.join('|')"), "a closed port receives nothing");
        run("var ch2 = new MessageChannel(); var seen2 = []; ch2.port2.onmessage = function (e) { seen2.push(e.data); }; ch2.port1.postMessage('queued'); ch2.port2.close();");
        deliver();
        Check.equal("", run("seen2.join('|')"), "close() drops the messages that wait for the port");
        Check.equal("ok", run("ch2.port2.close(); ch2.port1.close(); 'ok'"), "closing again does nothing");
    }

    /**
     * MessageEvent can be constructed with the members of its init dictionary.
     * <p>
     * MessageEvent 는 초기화 사전의 멤버로 생성할 수 있다.
     */
    public void testMessageEventConstructor() {
        Check.equal("null::::true", run("var e = new MessageEvent('message'); e.data + ':' + e.origin + ':' + e.lastEventId + ':' + (e.source === null ? '' : 'x') + ':' + (e.ports.length === 0)"),
                "defaults");
        Check.equal("[object Object]:x:id:true:1:true:true", run("var mc = new MessageChannel(); var e2 = new MessageEvent('message', { data: { a: 1 }, origin: 'x', lastEventId: 'id', source: window,"
                + " ports: [mc.port1], bubbles: true }); e2.data + ':' + e2.origin + ':' + e2.lastEventId + ':' + (e2.source === window) + ':' + e2.ports.length + ':' + (e2.ports[0] === mc.port1)"
                + " + ':' + e2.bubbles"), "members");
        Check.equal("TypeError", run("(function () { try { new MessageEvent('m', { ports: [{}] }); } catch (e) { return e.name; } })()"), "ports must be MessagePort objects");
        Check.equal("TypeError", run("(function () { try { MessageEvent('m'); } catch (e) { return e.name; } })()"), "needs new");
    }
}
