package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>XMLHttpRequest</code> on a real script page with a fake request service (plan item P20-08): the states, the events and their
 * order, request headers and bodies, response types, aborting, timeouts, network errors, late results of reopened requests, and the
 * exceptions of the standard. Real HTTP is covered by the component test <code>WebRequestApiTest</code>.
 * <p>
 * 실제 스크립트 페이지와 가짜 요청 서비스로 <code>XMLHttpRequest</code> 를 시험한다(계획 P20-08): 상태, 이벤트와 그 순서, 요청 헤더와 본문, 응답 종류,
 * 중단, 제한 시간, 네트워크 오류, 다시 연 요청의 늦은 결과, 표준의 예외. 실제 HTTP 는 컴포넌트 시험 <code>WebRequestApiTest</code> 가 다룬다.
 */
public class XhrTest {

    /**
     * The script that records every event of a request in the array <code>ev</code>.
     * <p>
     * 요청의 모든 이벤트를 배열 <code>ev</code> 에 기록하는 스크립트.
     */
    private static final String RECORD = "var ev = []; var x = new XMLHttpRequest(); "
            + "['loadstart','progress','abort','error','load','timeout','loadend'].forEach(function (n) { x.addEventListener(n, function (e) { "
            + "ev.push(n + ':' + x.readyState + (e.lengthComputable ? ':c' + e.loaded + '/' + e.total : ':n' + e.loaded)); }); }); "
            + "x.onreadystatechange = function () { ev.push('rsc' + x.readyState); };";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XhrTest() {
    }

    /**
     * A normal request: the states and events in the order of the standard, the response properties and the request that reached the
     * request layer.
     * <p>
     * 정상 요청: 표준 순서의 상태와 이벤트, 응답 프로퍼티, 요청 계층에 도달한 요청.
     */
    public void testNormalRequest() {
        WebFixture f = new WebFixture();
        f.js(RECORD);
        Check.equal("0", f.js("String(x.readyState)"), "unsent");
        f.js("x.open('GET', 'data?x=1');");
        Check.equal("rsc1", f.js("ev.join()"), "open fires readystatechange");
        f.js("x.send();");
        Check.equal("rsc1,loadstart:1:n0", f.js("ev.join()"), "send fires loadstart");
        Check.equal(1, f.env.started.size(), "one request");
        WebRequest r = f.env.started.get(0).request;
        Check.equal("http://example.test/dir/data?x=1", r.getUrl(), "the URL is resolved against the page");
        Check.equal("GET", r.getMethod(), "method");
        Check.equal("cors", r.getMode(), "XHR uses CORS mode");
        Check.equal("same-origin", r.getCredentials(), "credentials mode same-origin by default");
        Check.isNull(r.getBody(), "no body");
        f.answer(0, WebFixture.response(200, "text/plain; charset=utf-8", "hello"));
        Check.equal("rsc1,loadstart:1:n0,rsc2,rsc3,progress:3:c5/5,progress:3:c5/5,rsc4,load:4:c5/5,loadend:4:c5/5", f.js("ev.join()"),
                "events in order, a readystatechange for every state");
        Check.equal("4|200|OK|hello|hello|http://example.test/dir/data", f.js("[x.readyState, x.status, x.statusText, x.responseText, x.response, "
                + "x.responseURL].join('|')"), "response properties");
        Check.equal("text/plain; charset=utf-8", f.js("x.getResponseHeader('Content-Type')"), "getResponseHeader is case-insensitive");
        Check.equal("null", f.js("String(x.getResponseHeader('x-missing'))"), "missing header is null");
        Check.equal("content-length: 5\r\ncontent-type: text/plain; charset=utf-8\r\n", f.js("x.getAllResponseHeaders()"), "all headers, sorted");
    }

    /**
     * Request headers: forbidden headers are ignored, equal names are combined, the Content-Type of a string body is UTF-8.
     * <p>
     * 요청 헤더: 금지된 헤더는 무시하고 같은 이름은 합치며 문자열 본문의 Content-Type 은 UTF-8 이다.
     */
    public void testRequestHeadersAndBody() {
        WebFixture f = new WebFixture();
        f.js("var x = new XMLHttpRequest(); x.open('POST', '/api'); x.setRequestHeader('X-A', '1'); x.setRequestHeader('x-a', '2');"
                + "x.setRequestHeader('Content-Type', 'application/json; charset=ISO-8859-1'); x.setRequestHeader('Cookie', 'a=b'); "
                + "x.setRequestHeader('User-Agent', 'evil'); x.setRequestHeader('Host', 'evil'); x.setRequestHeader('Origin', 'http://evil');"
                + "x.setRequestHeader('Referer', 'http://evil'); x.setRequestHeader('Sec-Fetch-Mode', 'x'); x.setRequestHeader('Proxy-Authorization', 'x');"
                + "x.setRequestHeader('X-HTTP-Method-Override', 'TRACE'); x.setRequestHeader('X-Method-Override', 'PUT'); "
                + "x.send('café');");
        WebRequest r = f.env.started.get(0).request;
        StringBuilder names = new StringBuilder();
        // 요청 계층에 전달된 작성자 헤더를 이름=값 으로 모은다.
        for (int i = 0; i < r.getHeaders().size(); i++) {
            names.append(r.getHeaders().get(i)[0].toLowerCase()).append('=').append(r.getHeaders().get(i)[1]).append(';');
        }
        Check.equal("x-a=1, 2;x-method-override=PUT;", names.toString(), "only allowed headers, equal names combined");
        Check.equal("application/json;charset=UTF-8", r.getBodyType(), "charset forced to UTF-8");
        Check.equal("café", new String(r.getBody(), java.nio.charset.Charset.forName("UTF-8")), "body is UTF-8");
        Check.equal("/api", "/api", "sanity");
        Check.equal("http://example.test/api", r.getUrl(), "absolute-path URL");
    }

    /**
     * The extraction of the other body types and their content types, and that GET ignores the body.
     * <p>
     * 다른 본문 형식의 추출과 콘텐츠 타입, 그리고 GET 이 본문을 무시하는 것.
     */
    public void testBodyTypes() {
        WebFixture f = new WebFixture();
        f.js("var u = new URLSearchParams({a: 'x y'}); var x1 = new XMLHttpRequest(); x1.open('POST', 'u'); x1.send(u);"
                + "var x2 = new XMLHttpRequest(); x2.open('POST', 'u'); x2.send(new Blob(['bb'], {type: 'a/b'}));"
                + "var x3 = new XMLHttpRequest(); x3.open('POST', 'u'); x3.send(new Uint8Array([1, 2, 3]));"
                + "var fd = new FormData(); fd.append('k', 'v'); var x4 = new XMLHttpRequest(); x4.open('POST', 'u'); x4.send(fd);"
                + "var x5 = new XMLHttpRequest(); x5.open('GET', 'u'); x5.send('ignored');"
                + "var x6 = new XMLHttpRequest(); x6.open('POST', 'u'); x6.send();"
                + "var x7 = new XMLHttpRequest(); x7.open('POST', 'u'); x7.send('plain');");
        Check.equal("application/x-www-form-urlencoded;charset=UTF-8", f.env.started.get(0).request.getBodyType(), "URLSearchParams");
        Check.equal("a=x+y", new String(f.env.started.get(0).request.getBody()), "URLSearchParams body");
        Check.equal("a/b", f.env.started.get(1).request.getBodyType(), "Blob type");
        Check.isNull(f.env.started.get(2).request.getBodyType(), "buffer has no type");
        Check.equal(3, f.env.started.get(2).request.getBody().length, "buffer body");
        Check.isTrue(f.env.started.get(3).request.getBodyType().startsWith("multipart/form-data; boundary=----SwingWebPanelFormBoundary"),
                "FormData is multipart");
        Check.isTrue(new String(f.env.started.get(3).request.getBody()).indexOf("name=\"k\"\r\n\r\nv\r\n") > 0, "the multipart body has the field");
        Check.isNull(f.env.started.get(4).request.getBody(), "GET sends no body");
        Check.isNull(f.env.started.get(5).request.getBody(), "send() without a body");
        Check.equal("text/plain;charset=UTF-8", f.env.started.get(6).request.getBodyType(), "string");
    }

    /**
     * The exceptions of the standard.
     * <p>
     * 표준의 예외.
     */
    public void testExceptions() {
        WebFixture f = new WebFixture();
        Check.equal("InvalidStateError", f.js("var x = new XMLHttpRequest(); try { x.setRequestHeader('a', 'b'); 'no' } catch (e) { e.name }"),
                "setRequestHeader before open");
        Check.equal("InvalidStateError", f.js("try { x.send(); 'no' } catch (e) { e.name }"), "send before open");
        Check.equal("SyntaxError", f.js("try { x.open('bad method', '/'); 'no' } catch (e) { e.name }"), "bad method");
        Check.equal("SecurityError", f.js("try { x.open('TRACE', '/'); 'no' } catch (e) { e.name }"), "TRACE is forbidden");
        Check.equal("SecurityError", f.js("try { x.open('connect', '/'); 'no' } catch (e) { e.name }"), "CONNECT in any case is forbidden");
        Check.equal("SyntaxError", f.js("try { x.open('GET', 'http://[bad'); 'no' } catch (e) { e.name }"), "bad URL");
        Check.equal("InvalidAccessError", f.js("try { x.open('GET', '/', false); 'no' } catch (e) { e.name }"), "synchronous requests are refused");
        Check.equal("0", f.js("String(x.readyState)"), "a failed open changes nothing");
        f.js("x.open('get', '/x'); x.setRequestHeader('X', 'y');");
        Check.equal("SyntaxError", f.js("try { x.setRequestHeader('bad name', 'v'); 'no' } catch (e) { e.name }"), "bad header name");
        Check.equal("SyntaxError", f.js("try { x.setRequestHeader('x', 'a\\nb'); 'no' } catch (e) { e.name }"), "bad header value");
        Check.equal("TypeError", f.js("try { x.setRequestHeader('x', 'caf\\u00e9\\u20ac'); 'no' } catch (e) { e.name }"), "not a ByteString");
        f.js("x.send();");
        Check.equal("InvalidStateError", f.js("try { x.send(); 'no' } catch (e) { e.name }"), "send twice");
        Check.equal("InvalidStateError", f.js("try { x.setRequestHeader('a', 'b'); 'no' } catch (e) { e.name }"), "setRequestHeader after send");
        Check.equal("InvalidStateError", f.js("try { x.withCredentials = true; 'no' } catch (e) { e.name }"), "withCredentials after send");
        Check.equal("TypeError", f.js("try { XMLHttpRequest.prototype.send.call({}); 'no' } catch (e) { e.name }"), "illegal invocation");
        Check.equal("TypeError", f.js("try { XMLHttpRequest(); 'no' } catch (e) { e.name }"), "needs new");
        Check.equal("0|1|2|3|4", f.js("[XMLHttpRequest.UNSENT, XMLHttpRequest.OPENED, XMLHttpRequest.HEADERS_RECEIVED, XMLHttpRequest.LOADING, XMLHttpRequest.DONE]"
                + ".join('|')"), "constants");
    }

    /**
     * abort(): events, state, cancellation of the call and a late answer that is ignored.
     * <p>
     * abort(): 이벤트, 상태, 호출 취소, 무시되는 늦은 응답.
     */
    public void testAbort() {
        WebFixture f = new WebFixture();
        f.js(RECORD + "x.open('GET', 'data'); x.send(); ev.length = 0; x.abort();");
        Check.equal("rsc4,abort:4:n0,loadend:4:n0", f.js("ev.join()"), "abort events");
        Check.equal("0|0|", f.js("[x.readyState, x.status, x.responseText].join('|')"), "state is unsent again");
        Check.isTrue(f.env.started.get(0).cancelled, "the call was cancelled");
        f.js("ev.length = 0;");
        f.answer(0, WebFixture.response(200, "text/plain", "late"));
        Check.equal("", f.js("ev.join()"), "a late answer is ignored");
        Check.equal("0", f.js("String(x.readyState)"), "still unsent");
        f.js("x.open('GET', 'data2'); ev.length = 0; x.abort();");
        Check.equal("", f.js("ev.join()"), "abort of an opened request that was not sent fires nothing");
    }

    /**
     * Reopening a request invalidates the answer of the earlier send.
     * <p>
     * 요청을 다시 열면 앞선 send 의 응답이 무효가 된다.
     */
    public void testReopenIgnoresOldAnswer() {
        WebFixture f = new WebFixture();
        f.js(RECORD + "x.open('GET', 'one'); x.send(); x.open('GET', 'two'); ev.length = 0;");
        f.answer(0, WebFixture.response(200, "text/plain", "old"));
        Check.equal("", f.js("ev.join()"), "the old answer fires nothing");
        f.js("x.send();");
        f.answer(1, WebFixture.response(200, "text/plain", "new"));
        Check.equal("new", f.js("x.responseText"), "the new answer is used");
        Check.isTrue(f.env.started.get(0).cancelled, "the old call was cancelled");
    }

    /**
     * Timeout: the timeout event fires on the page clock, the call is cancelled and a late answer is ignored.
     * <p>
     * 제한 시간: timeout 이벤트가 페이지 시계로 발생하고 호출이 취소되며 늦은 응답은 무시된다.
     */
    public void testTimeout() {
        WebFixture f = new WebFixture();
        f.js(RECORD + "x.timeout = 100; x.open('GET', 'slow'); x.send(); ev.length = 0;");
        f.advance(99);
        Check.equal("", f.js("ev.join()"), "not yet");
        f.advance(2);
        Check.equal("rsc4,timeout:4:n0,loadend:4:n0", f.js("ev.join()"), "timeout events");
        Check.isTrue(f.env.started.get(0).cancelled, "cancelled");
        f.answer(0, WebFixture.response(200, "text/plain", "late"));
        Check.equal("rsc4,timeout:4:n0,loadend:4:n0", f.js("ev.join()"), "a late answer is ignored");
        f.js("var y = new XMLHttpRequest(); y.timeout = 100; y.open('GET', 'ok'); y.send(); var done = []; y.onload = function () { done.push(1); };");
        f.advance(50);
        f.answer(1, WebFixture.response(200, "text/plain", "in time"));
        f.advance(500);
        Check.equal("1", f.js("done.join()"), "the timer of a finished request does nothing");
        f.js("var z = new XMLHttpRequest(); z.open('GET', 'late-timeout'); z.send(); z.timeout = 30; var zt = []; z.ontimeout = function () { zt.push(1); };");
        f.advance(31);
        Check.equal("1", f.js("zt.join()"), "a timeout set while the request runs is measured from its start");
    }

    /**
     * A network error: error and loadend events, status 0.
     * <p>
     * 네트워크 오류: error 와 loadend 이벤트, 상태 0.
     */
    public void testNetworkError() {
        WebFixture f = new WebFixture();
        f.js(RECORD + "x.open('GET', 'down'); x.send(); ev.length = 0;");
        f.answer(0, WebResponse.error("connection refused", false));
        Check.equal("rsc4,error:4:n0,loadend:4:n0", f.js("ev.join()"), "error events");
        Check.equal("4|0||", f.js("[x.readyState, x.status, x.statusText, x.responseText].join('|')"), "no response");
        Check.equal("null", f.js("String(x.getResponseHeader('content-type'))"), "no headers");
        Check.equal("", f.js("x.getAllResponseHeaders()"), "no header text");
    }

    /**
     * HTTP error statuses are normal responses.
     * <p>
     * HTTP 오류 상태는 일반 응답이다.
     */
    public void testHttpErrorStatusIsAResponse() {
        WebFixture f = new WebFixture();
        f.js(RECORD + "x.open('GET', 'missing'); x.send(); ev.length = 0;");
        f.answer(0, WebFixture.response(404, "text/html", "<p>nope</p>"));
        Check.equal("load:4:c11/11", f.js("ev.filter(function (e) { return e.indexOf('load:') === 0; }).join()"), "load fires for a 404");
        Check.equal("404|Status 404|<p>nope</p>", f.js("[x.status, x.statusText, x.responseText].join('|')"), "404 response");
    }

    /**
     * Response types: json, arraybuffer, blob, text, document; the exceptions of responseText and responseXML.
     * <p>
     * 응답 종류: json, arraybuffer, blob, text, document. responseText 와 responseXML 의 예외.
     */
    public void testResponseTypes() {
        WebFixture f = new WebFixture();
        f.js("function get(type, ct, body, status) { var r = new XMLHttpRequest(); r.open('GET', 'd'); r.responseType = type; r.send(); "
                + "window.last = r; return r; }");
        f.js("var j = get('json');");
        Check.equal("null", f.js("String(j.response)"), "response is null before the end");
        f.answer(0, WebFixture.response(200, "application/json", "{\"a\":[1,2,{\"b\":true}]}"));
        Check.equal("object|2|true", f.js("[typeof j.response, j.response.a[1], j.response.a[2].b].join('|')"), "json");
        Check.equal("InvalidStateError", f.js("try { j.responseText; 'no' } catch (e) { e.name }"), "responseText with responseType json");
        Check.equal("json", f.js("j.responseType"), "responseType getter");
        f.js("var j2 = get('json');");
        f.answer(1, WebFixture.response(200, "application/json", "{bad json"));
        Check.equal("null", f.js("String(j2.response)"), "invalid JSON gives null");
        f.js("var ab = get('arraybuffer');");
        f.answer(2, WebFixture.response(200, "application/octet-stream", "abc"));
        Check.equal("true|3|98", f.js("[ab.response instanceof ArrayBuffer, ab.response.byteLength, new Uint8Array(ab.response)[1]].join('|')"), "arraybuffer");
        f.js("var bl = get('blob');");
        f.answer(3, WebFixture.response(200, "text/plain; charset=utf-8", "blobby"));
        Check.equal("true|6|text/plain;charset=utf-8", f.js("[bl.response instanceof Blob, bl.response.size, bl.response.type].join('|')"), "blob");
        f.js("var tx = get('text');");
        f.answer(4, WebFixture.response(200, "text/plain", "plain"));
        Check.equal("plain", f.js("tx.response"), "text");
        f.js("var dc = get('document');");
        f.answer(5, WebFixture.response(200, "text/html", "<!doctype html><title>T</title><p id=x>hi</p>"));
        Check.equal("T|hi|true", f.js("[dc.response.title, dc.response.getElementById('x').textContent, dc.responseXML === dc.response].join('|')"), "html document");
        f.js("var xm = new XMLHttpRequest(); xm.open('GET', 'd'); xm.send();");
        f.answer(6, WebFixture.response(200, "application/xml", "<root><item n=\"1\">a</item><item n=\"2\">b</item></root>"));
        Check.equal("2|b", f.js("[xm.responseXML.getElementsByTagName('item').length, xm.responseXML.documentElement.lastChild.textContent].join('|')"),
                "xml document through responseXML");
        f.js("var bad = new XMLHttpRequest(); bad.open('GET', 'd'); bad.send();");
        f.answer(7, WebFixture.response(200, "application/xml", "<root><unclosed></root>"));
        Check.equal("null", f.js("String(bad.responseXML)"), "malformed XML gives null");
        f.js("var ht = new XMLHttpRequest(); ht.open('GET', 'd'); ht.send();");
        f.answer(8, WebFixture.response(200, "text/html", "<p>x</p>"));
        Check.equal("null", f.js("String(ht.responseXML)"), "HTML is not a document unless responseType is document");
        Check.equal("InvalidStateError", f.js("try { j.responseXML; 'no' } catch (e) { e.name }"), "responseXML with responseType json");
        Check.equal("ok", f.js("try { var q = new XMLHttpRequest(); q.open('GET', 'd'); q.send(); q.responseType = 'json'; 'ok' } catch (e) { e.name }"),
                "responseType can still change while the request runs");
        Check.equal("text", f.js("var z = new XMLHttpRequest(); z.responseType = 'bogus'; z.responseType = 'text'; z.responseType"), "invalid values are ignored");
    }

    /**
     * Charsets and overrideMimeType.
     * <p>
     * 문자셋과 overrideMimeType.
     */
    public void testCharsetAndOverrideMimeType() {
        WebFixture f = new WebFixture();
        WebResponse latin = new WebResponse(WebResponse.TYPE_BASIC);
        latin.setStatus(200, "OK").setUrl("http://example.test/dir/d").addHeader("Content-Type", "text/plain; charset=ISO-8859-1");
        latin.setBody(new byte[] { 'c', 'a', 'f', (byte) 0xE9 });
        f.js("var x = new XMLHttpRequest(); x.open('GET', 'd'); x.send();");
        f.answer(0, latin);
        Check.equal("café", f.js("x.responseText"), "the charset of the response is used");
        f.js("var y = new XMLHttpRequest(); y.open('GET', 'd'); y.overrideMimeType('text/plain; charset=UTF-8'); y.send();");
        f.answer(1, latin);
        Check.equal("caf" + (char) 0xFFFD, f.js("y.responseText"), "overrideMimeType changes the charset");
        Check.equal("InvalidStateError", f.js("try { y.overrideMimeType('text/plain'); 'no' } catch (e) { e.name }"), "not after the end");
    }

    /**
     * Credentials flag, upload events and progress events created by scripts.
     * <p>
     * 자격 증명 표시, 업로드 이벤트, 스크립트가 만드는 진행 이벤트.
     */
    public void testCredentialsUploadAndProgressEvent() {
        WebFixture f = new WebFixture();
        f.js("var x = new XMLHttpRequest(); x.open('POST', 'u'); x.withCredentials = true; var up = []; "
                + "['loadstart','progress','load','loadend'].forEach(function (n) { x.upload.addEventListener(n, function (e) { up.push(n + ':' + e.loaded + '/' "
                + "+ e.total); }); }); x.send('12345');");
        Check.equal("include", f.env.started.get(0).request.getCredentials(), "withCredentials maps to include");
        Check.equal("loadstart:0/5", f.js("up.join()"), "upload loadstart");
        f.answer(0, WebFixture.response(200, "text/plain", "ok"));
        Check.equal("loadstart:0/5,progress:5/5,load:5/5,loadend:5/5", f.js("up.join()"), "upload events when the answer starts");
        Check.equal("true", f.js("String(x.upload === x.upload && x.upload instanceof XMLHttpRequestUpload)"), "same upload object");
        Check.equal("true|7|20|true", f.js("var pe = new ProgressEvent('x', {loaded: 7, total: 20, lengthComputable: true}); "
                + "[pe instanceof Event, pe.loaded, pe.total, pe.lengthComputable].join('|')"), "ProgressEvent constructor");
        Check.equal("false|0|0|false", f.js("var pd = new ProgressEvent('y'); [pd.bubbles, pd.loaded, pd.total, pd.lengthComputable].join('|')"), "defaults");
    }

    /**
     * Events are trusted, <code>this</code> and the target are the request, and an error in a listener does not stop the others.
     * <p>
     * 이벤트는 신뢰되고 <code>this</code> 와 대상은 요청이며 리스너의 오류가 다른 리스너를 멈추지 않는다.
     */
    public void testEventObjectsAndListenerErrors() {
        WebFixture f = new WebFixture();
        f.js("var x = new XMLHttpRequest(); var seen = []; x.onload = function (e) { seen.push([this === x, e.target === x, e.isTrusted, e.type].join()); }; "
                + "x.addEventListener('load', function () { throw new Error('listener failed'); }); "
                + "x.addEventListener('load', function () { seen.push('after'); }); x.open('GET', 'd'); x.send();");
        f.answer(0, WebFixture.response(200, "text/plain", "x"));
        Check.equal("true,true,true,load|after", f.js("seen.join('|')"), "this, target, trusted and listener isolation");
        Check.isTrue(f.log().indexOf("listener failed") >= 0, "the error was reported: " + f.log());
    }

    /**
     * A host without a request service, and scripts that are switched off or disposed: nothing runs late.
     * <p>
     * 요청 서비스가 없는 호스트, 꺼지거나 폐기된 스크립트: 늦게 아무것도 실행되지 않는다.
     */
    public void testNoServiceAndDisposal() {
        WebFixture f = new WebFixture();
        f.env.hasSource = false;
        f.js(RECORD + "x.open('GET', 'd'); x.send(); ev.length = 0;");
        f.run();
        Check.equal("rsc4,error:4:n0,loadend:4:n0", f.js("ev.join()"), "no service is a network error");
        WebFixture g = new WebFixture();
        g.js(RECORD + "x.open('GET', 'd'); x.send(); ev.length = 0;");
        g.env.scripting = false;
        g.env.started.get(0).callback.onComplete(WebFixture.response(200, "text/plain", "x"));
        g.run();
        g.env.scripting = true;
        Check.equal("", g.js("ev.join()"), "nothing runs while scripts are off");
        WebFixture h = new WebFixture();
        h.js(RECORD + "x.open('GET', 'd'); x.send(); ev.length = 0;");
        h.scripts.requestDisposal();
        Check.isTrue(h.env.started.get(0).cancelled, "requestDisposal cancels the call without waiting");
        h.env.started.get(0).callback.onComplete(WebFixture.response(200, "text/plain", "x"));
        h.run();
        h.scripts.dispose();
        Check.isNull(h.js("ev.join()"), "a disposed page runs nothing");
    }

    /**
     * Many requests can be started; each one is answered independently and in any order.
     * <p>
     * 요청을 많이 시작할 수 있으며 각각 독립적으로 어떤 순서로도 응답받는다.
     */
    public void testConcurrentRequestsAnswerInAnyOrder() {
        WebFixture f = new WebFixture();
        f.js("var out = []; function go(n) { var x = new XMLHttpRequest(); x.open('GET', 'r' + n); x.onload = function () { out.push(n + ':' + x.responseText); }; "
                + "x.send(); } go(0); go(1); go(2);");
        f.answer(2, WebFixture.response(200, "text/plain", "c"));
        f.answer(0, WebFixture.response(200, "text/plain", "a"));
        f.answer(1, WebFixture.response(200, "text/plain", "b"));
        Check.equal("2:c,0:a,1:b", f.js("out.join()"), "results arrive in the order of completion");
        Check.equal("http://example.test/dir/r2", f.env.started.get(2).request.getUrl(), "each request has its own URL");
    }
}
