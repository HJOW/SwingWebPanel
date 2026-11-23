package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>WebSocket</code> and <code>CloseEvent</code> under the scheme policy of the component (plan item P20-10, feature matrix
 * EXC-06): the constructor checks what it can check without a network, and the connection of a valid request always fails like a refused
 * connection (<code>error</code>, then <code>close</code> with code 1006), so that pages with a polling fallback keep working.
 * <p>
 * 컴포넌트의 스킴 정책 아래에서 <code>WebSocket</code> 과 <code>CloseEvent</code> 의 시험(계획 P20-10, 기능 표 EXC-06): 생성자는 네트워크 없이 확인할
 * 수 있는 것을 검사하고, 올바른 요청의 연결은 항상 거부된 연결처럼 실패하므로(<code>error</code>, 이어서 코드 1006 의 <code>close</code>) 폴링으로
 * 되돌아가는 페이지는 계속 동작한다.
 */
public class WebSocketTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebSocketTest() {
    }

    /**
     * The interface, its constants and the tag exist; Java is not visible.
     * <p>
     * 인터페이스, 상수, 태그가 있다. Java 는 보이지 않는다.
     */
    public void testInterface() {
        WebFixture f = new WebFixture();
        Check.equal("function|0,1,2,3|[object WebSocket]|function", f.js("[typeof WebSocket, [WebSocket.CONNECTING, WebSocket.OPEN, WebSocket.CLOSING, "
                + "WebSocket.CLOSED].join(), Object.prototype.toString.call(new WebSocket('ws://example.invalid/x')), typeof CloseEvent].join('|')"),
                "interface, constants, tag");
        Check.equal("true", f.js("String(new WebSocket('wss://example.invalid/') instanceof EventTarget)"), "an event target");
    }

    /**
     * The constructor rejects what the specification rejects before any connection is tried.
     * <p>
     * 생성자는 규격이 연결을 시도하기 전에 거부하는 것을 거부한다.
     */
    public void testConstructorChecks() {
        WebFixture f = new WebFixture();
        Check.equal("SyntaxError", f.js("try { new WebSocket('ftp://example.invalid/x'); 'no' } catch (e) { e.name }"), "a scheme other than ws, wss, http, https");
        Check.equal("SyntaxError", f.js("try { new WebSocket('ws://example.invalid/x#frag'); 'no' } catch (e) { e.name }"), "a fragment");
        Check.equal("SyntaxError", f.js("try { new WebSocket('ws://example.invalid/x', ['a', 'a']); 'no' } catch (e) { e.name }"), "a repeated sub-protocol");
        Check.equal("SyntaxError", f.js("try { new WebSocket('ws://example.invalid/x', 'bad name'); 'no' } catch (e) { e.name }"), "a sub-protocol that is not a token");
        Check.equal("TypeError", f.js("try { new WebSocket(); 'no' } catch (e) { e.name }"), "the URL is required");
        Check.equal("TypeError", f.js("try { WebSocket('ws://example.invalid/x'); 'no' } catch (e) { e.name }"), "new is required");
        Check.equal("ws://example.invalid/x?a=1|0|1", f.js("var s = new WebSocket('ws://example.invalid/x?a=1', ['chat', 'v2']); [s.url, s.readyState, "
                + "typeof s.bufferedAmount === 'number' ? 1 : 0].join('|')"), "a valid request starts in the CONNECTING state");
        Check.equal("|", f.js("[s.protocol, s.extensions].join('|')"), "nothing is negotiated");
    }

    /**
     * The connection fails as a task: <code>error</code>, <code>readyState</code> 3 and <code>close</code> with 1006 and a not clean close; the
     * handlers and listeners both see it, in that order, and <code>send</code> follows the state.
     * <p>
     * 연결은 작업으로 실패한다: <code>error</code>, <code>readyState</code> 3, 정상이 아닌 1006 의 <code>close</code>. 핸들러와 리스너가 모두
     * 그 순서로 보며 <code>send</code> 는 상태를 따른다.
     */
    public void testConnectionFails() {
        WebFixture f = new WebFixture();
        f.js("window.log = []; window.s = new WebSocket('wss://example.invalid/chat'); "
                + "s.onerror = function (e) { log.push('error:' + s.readyState + ':' + e.type); };"
                + "s.addEventListener('close', function (e) { log.push('close:' + s.readyState + ':' + e.wasClean + ':' + e.code + ':[' + e.reason + ']'); });"
                + "s.onopen = function () { log.push('open'); }; s.onmessage = function () { log.push('message'); };");
        Check.equal("InvalidStateError", f.js("try { s.send('x'); 'no' } catch (e) { e.name }"), "send while connecting");
        Check.equal("0", f.js("String(log.length)"), "nothing yet: the failure is a task");
        f.advance(1);
        Check.equal("error:0:error|close:3:false:1006:[]", f.js("log.join('|')"), "error, then close");
        Check.equal("undefined", f.js("String(s.send('x'))"), "send after the failure does nothing");
        Check.equal("true", f.js("String(s instanceof WebSocket && s.readyState === WebSocket.CLOSED)"), "the final state");
    }

    /**
     * <code>close</code> checks its arguments; closing a connecting socket moves it to CLOSING and the failure then ends it.
     * <p>
     * <code>close</code> 는 인수를 검사한다. 연결 중인 소켓을 닫으면 CLOSING 이 되고 실패가 그것을 끝낸다.
     */
    public void testClose() {
        WebFixture f = new WebFixture();
        f.js("window.log = []; window.s = new WebSocket('ws://example.invalid/x'); s.onerror = function () { log.push('error'); };"
                + "s.onclose = function () { log.push('close'); };");
        Check.equal("InvalidAccessError", f.js("try { s.close(1001); 'no' } catch (e) { e.name }"), "a code that is not 1000 or 3000-4999");
        Check.equal("SyntaxError", f.js("try { s.close(1000, new Array(125).join('a')); 'no' } catch (e) { e.name }"), "a reason longer than 123 bytes");
        Check.equal("2", f.js("s.close(3001, 'bye'); String(s.readyState)"), "closing a connecting socket");
        f.advance(1);
        Check.equal("error|close|3", f.js("log.join('|') + '|' + s.readyState"), "the failure ends the closing socket");
        Check.equal("blob|arraybuffer|arraybuffer", f.js("var b = [s.binaryType]; s.binaryType = 'arraybuffer'; b.push(s.binaryType); s.binaryType = 'x'; b.push(s.binaryType); "
                + "b.join('|')"), "binaryType accepts only its two values");
    }
}
