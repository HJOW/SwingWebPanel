package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Fetch API on a real script page with a fake request service (plan item P20-08): <code>fetch()</code> and the promise order,
 * the mapping of the options to the request that reaches the request layer, <code>Request</code> and <code>Response</code> objects, the Body mixin,
 * <code>Headers</code> guards on those objects, aborting with signals, network errors and subresource integrity. Real HTTP is covered by the
 * component test <code>WebRequestApiTest</code>.
 * <p>
 * 실제 스크립트 페이지와 가짜 요청 서비스로 Fetch API 를 시험한다(계획 P20-08): <code>fetch()</code> 와 프라미스 순서, 옵션이 요청 계층에 도달한 요청으로
 * 옮겨지는 방식, <code>Request</code> 와 <code>Response</code> 객체, Body mixin, 그 객체들의 <code>Headers</code> guard, 신호로 중단하기, 네트워크 오류,
 * 하위 리소스 무결성. 실제 HTTP 는 컴포넌트 시험 <code>WebRequestApiTest</code> 가 다룬다.
 */
public class FetchTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FetchTest() {
    }

    /**
     * A normal fetch: the response properties, the body readers and the defaults of the request.
     * <p>
     * 정상 fetch: 응답 프로퍼티, 본문 읽기, 요청의 기본값.
     */
    public void testNormalFetch() {
        WebFixture f = new WebFixture();
        f.js("var res, text, order = []; var p = fetch('data?q=1'); order.push('sync'); p.then(function (r) { res = r; order.push('response'); "
                + "return r.text(); }).then(function (t) { text = t; order.push('text'); });");
        Check.equal(1, f.env.started.size(), "one request");
        WebRequest r = f.env.started.get(0).request;
        Check.equal("http://example.test/dir/data?q=1", r.getUrl(), "URL resolved against the page");
        Check.equal("GET|cors|same-origin|default|follow|client|", r.getMethod() + "|" + r.getMode() + "|" + r.getCredentials() + "|" + r.getCache() + "|"
                + r.getRedirect() + "|" + r.getReferrer() + "|" + r.getReferrerPolicy(), "defaults of the request");
        Check.equal("sync", f.js("order.join()"), "the promise is pending");
        f.answer(0, WebFixture.response(200, "text/plain;charset=utf-8", "hello"));
        Check.equal("sync,response,text", f.js("order.join()"), "then callbacks run as microtasks after the response");
        Check.equal("200|OK|true|basic|false|http://example.test/dir/data|text/plain;charset=utf-8|hello",
                f.js("[res.status, res.statusText, res.ok, res.type, res.redirected, res.url, res.headers.get('content-type'), text].join('|')"),
                "response properties");
        Check.equal("true|true|true", f.js("[res instanceof Response,res.body instanceof ReadableStream,res.body.locked].join('|')"),
                "the consumed response still exposes its locked stream");
    }

    /**
     * The options reach the request layer: method, headers, body, credentials, mode, cache, redirect, referrer.
     * <p>
     * 옵션이 요청 계층에 도달한다: 메서드, 헤더, 본문, 자격 증명, 모드, 캐시, 리디렉션, 참조.
     */
    public void testInitOptions() {
        WebFixture f = new WebFixture();
        f.js("fetch('/api/x', {method: 'put', headers: {'X-One': '1', 'Content-Type': 'application/json'}, body: JSON.stringify({a: 1}), credentials: 'include', "
                + "mode: 'cors', cache: 'no-store', redirect: 'error', referrer: '', referrerPolicy: 'origin', keepalive: true, priority: 'low'});");
        WebRequest r = f.env.started.get(0).request;
        Check.equal("PUT|include|cors|no-store|error|no-referrer|origin", r.getMethod() + "|" + r.getCredentials() + "|" + r.getMode() + "|" + r.getCache() + "|"
                + r.getRedirect() + "|" + r.getReferrer() + "|" + r.getReferrerPolicy(), "options");
        Check.equal("{\"a\":1}", new String(r.getBody()), "body");
        Check.equal("application/json", r.getBodyType(), "the Content-Type goes with the body");
        Check.equal("x-one=1", r.getHeaders().get(0)[0] + "=" + r.getHeaders().get(0)[1], "other headers");
        Check.equal(1, r.getHeaders().size(), "the body type is not repeated as a header");
        f.js("fetch('/b', {method: 'POST', body: 'text'}); fetch('/c', {method: 'POST', body: new URLSearchParams('a=b')}); "
                + "var fd = new FormData(); fd.append('f', 'v'); fetch('/d', {method: 'POST', body: fd}); fetch('/e', {method: 'POST', body: new Blob(['x'], {type: 't/u'})});");
        Check.equal("text/plain;charset=UTF-8", f.env.started.get(1).request.getBodyType(), "string body type");
        Check.equal("application/x-www-form-urlencoded;charset=UTF-8", f.env.started.get(2).request.getBodyType(), "URLSearchParams body type");
        Check.isTrue(f.env.started.get(3).request.getBodyType().startsWith("multipart/form-data; boundary="), "FormData body type");
        Check.equal("t/u", f.env.started.get(4).request.getBodyType(), "Blob body type");
        f.js("fetch('/f', {headers: {'Cookie': 'x=1', 'User-Agent': 'evil', 'Host': 'h', 'Origin': 'o', 'Referer': 'r', 'Sec-Test': '1', 'Accept': 'text/x'}});");
        Check.equal(1, f.env.started.get(5).request.getHeaders().size(), "forbidden request headers are dropped silently");
        Check.equal("accept", f.env.started.get(5).request.getHeaders().get(0)[0], "Accept stays");
        f.js("fetch('/g', {headers: [['a', '1'], ['A', '2']]});");
        Check.equal("a=1, 2", f.env.started.get(6).request.getHeaders().get(0)[0] + "=" + f.env.started.get(6).request.getHeaders().get(0)[1],
                "equal header names are combined");
        f.js("fetch('/h', {referrer: '/dir/other.html'});");
        Check.equal("http://example.test/dir/other.html", f.env.started.get(7).request.getReferrer(), "same-origin referrer");
        f.js("fetch('/i', {referrer: 'http://other.test/x'});");
        Check.equal("client", f.env.started.get(8).request.getReferrer(), "cross-origin referrer is ignored");
    }

    /**
     * The body readers: json, arrayBuffer, bytes, blob, formData; the body can be read only once; clone works.
     * <p>
     * 본문 읽기: json, arrayBuffer, bytes, blob, formData. 본문은 한 번만 읽을 수 있고 clone 이 동작한다.
     */
    public void testBodyReaders() {
        WebFixture f = new WebFixture();
        f.js("var out = {}; fetch('d').then(function (r) { out.r = r; out.used0 = r.bodyUsed; var c = r.clone(); "
                + "r.json().then(function (j) { out.json = j.a; out.used1 = r.bodyUsed; return r.text(); }).catch(function (e) { out.again = e.name; });"
                + "c.text().then(function (t) { out.clone = t; }); });");
        f.answer(0, WebFixture.response(200, "application/json", "{\"a\":[1,2]}"));
        Check.equal("1,2|true|TypeError|{\"a\":[1,2]}|false", f.js("[out.json, out.used1, out.again, out.clone, out.used0].join('|')"),
                "json, bodyUsed, reading twice and clone");
        f.js("var o2 = {}; fetch('d2').then(function (r) { o2.blobType = null; return Promise.all([r.clone().arrayBuffer(), r.clone().bytes(), r.clone().blob(), "
                + "r.clone().formData()]).then(function (v) { o2.ab = v[0].byteLength; o2.u8 = v[1][0]; o2.blob = v[2].size + ':' + v[2].type; "
                + "o2.fd = Array.from(v[3]).map(function (e) { return e.join('='); }).join('&'); }); });");
        f.answer(1, WebFixture.response(200, "application/x-www-form-urlencoded", "a=1&b=%C3%A9"));
        Check.equal("12|97|12:application/x-www-form-urlencoded|a=1&b=é", f.js("[o2.ab, o2.u8, o2.blob, o2.fd].join('|')"),
                "arrayBuffer, bytes, blob, formData");
        f.js("var o3 = {}; fetch('d3').then(function (r) { return r.json(); }).catch(function (e) { o3.name = e.name; });");
        f.answer(2, WebFixture.response(200, "application/json", "{not json"));
        Check.equal("SyntaxError", f.js("o3.name"), "json() rejects with a SyntaxError");
        f.js("var o4 = {}; fetch('d4').then(function (r) { return r.formData(); }).catch(function (e) { o4.name = e.name; });");
        f.answer(3, WebFixture.response(200, "text/plain", "x"));
        Check.equal("TypeError", f.js("o4.name"), "formData() of another type rejects");
        f.js("var o5 = {}; fetch('d5').then(function (r) { return r.formData(); }).then(function (fd) { o5.v = fd.get('k') + ':' + (fd.get('file') instanceof File) "
                + "+ ':' + fd.get('file').name; });");
        f.answer(4, WebFixture.response(200, "multipart/form-data; boundary=XX", "--XX\r\nContent-Disposition: form-data; name=\"k\"\r\n\r\nvalue\r\n"
                + "--XX\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.txt\"\r\nContent-Type: text/plain\r\n\r\nfile body\r\n--XX--\r\n"));
        Check.equal("value:true:a.txt", f.js("o5.v"), "multipart formData()");
    }

    /**
     * Checks stream identity, lock versus disturbance and byte independence after cloning.
     * <p>
     * 스트림 동일성, 잠금과 소비 상태, 복제 뒤 바이트 독립성을 검사한다.
     */
    public void testBufferedBodyStream() {
        WebFixture f = new WebFixture();
        f.js("var log=[]; (async()=>{const res=new Response(new Uint8Array([65,66]));"
                + "const original=res.body;log.push(original===res.body,res.bodyUsed);"
                + "const hold=original.getReader();log.push(res.bodyUsed,original.locked);"
                + "try{await res.text();}catch(e){log.push(e.name); }"
                + "try{res.clone();}catch(e){log.push(e.name); } hold.releaseLock();"
                + "const clone=res.clone();log.push(original.locked,res.body!==original,clone.body!==res.body);"
                + "const reader=res.body.getReader();const chunk=await reader.read();"
                + "log.push(chunk.value instanceof Uint8Array,res.bodyUsed,clone.bodyUsed);chunk.value[0]=33;"
                + "log.push(await clone.text(),(await reader.read()).done,clone.body.locked);"
                + "reader.releaseLock();try{await res.arrayBuffer();}catch(e){log.push(e.name); }})();");
        f.run();
        Check.equal("true,false,false,true,TypeError,TypeError,true,true,true,true,true,false,AB,true,true,TypeError", f.js("log.join(',')"),
                "body streams and one-shot readers use the same consumption state");
        f.js("var done; var empty=new Response('');empty.body.getReader().read().then(r=>done=r.done);");
        f.run();
        Check.equal("true|true|true", f.js("[done,empty.bodyUsed,new Response().body===null].join('|')"), "empty body versus missing body");
    }

    /**
     * HTTP error statuses resolve; network errors reject with a TypeError and are logged.
     * <p>
     * HTTP 오류 상태는 이행하고 네트워크 오류는 TypeError 로 거부하며 기록된다.
     */
    public void testStatusesAndNetworkErrors() {
        WebFixture f = new WebFixture();
        f.js("var o = {}; fetch('missing').then(function (r) { o.s = r.status + ':' + r.ok; });");
        f.answer(0, WebFixture.response(404, "text/plain", "nope"));
        Check.equal("404:false", f.js("o.s"), "404 resolves with ok false");
        f.js("fetch('down').catch(function (e) { o.e = e.name + ':' + (e instanceof TypeError) + ':' + e.message; });");
        f.answer(1, WebResponse.error("connection refused", false));
        Check.equal("TypeError:true:Failed to fetch", f.js("o.e"), "network error rejects with TypeError");
        Check.isTrue(f.log().indexOf("connection refused") >= 0, "the reason is logged: " + f.log());
        WebFixture g = new WebFixture();
        g.env.hasSource = false;
        g.js("var o = {}; fetch('x').catch(function (e) { o.e = e.name; });");
        g.run();
        Check.equal("TypeError", g.js("o.e"), "no service rejects with TypeError");
    }

    /**
     * Aborting with an AbortSignal: before, during and after a request.
     * <p>
     * AbortSignal 로 중단하기: 요청 전, 도중, 뒤.
     */
    public void testAbortSignal() {
        WebFixture f = new WebFixture();
        f.js("var o = {}; var ac = new AbortController(); fetch('slow', {signal: ac.signal}).catch(function (e) { o.e = e.name; }); ac.abort();");
        Check.equal("AbortError", f.js("o.e"), "abort rejects with the reason");
        Check.isTrue(f.env.started.get(0).cancelled, "the call was cancelled");
        f.answer(0, WebFixture.response(200, "text/plain", "late"));
        Check.equal("AbortError", f.js("o.e"), "a late answer changes nothing");
        f.js("var done = false; fetch('x', {signal: AbortSignal.abort()}).catch(function (e) { o.pre = e.name; });");
        Check.equal("AbortError", f.js("o.pre"), "an already aborted signal rejects at once");
        Check.equal(1, f.env.started.size(), "no request for an aborted signal");
        f.js("var ac2 = new AbortController(); fetch('x2', {signal: ac2.signal}).then(function (r) { o.ok = r.status; });");
        f.answer(1, WebFixture.response(200, "text/plain", "x"));
        f.js("ac2.abort();");
        Check.equal("200", f.js("String(o.ok)"), "aborting after the response changes nothing");
        f.js("fetch('x3', {signal: AbortSignal.timeout(20)}).catch(function (e) { o.t = e.name; });");
        f.advance(21);
        Check.equal("TimeoutError", f.js("o.t"), "timeout signal rejects with TimeoutError");
        Check.isTrue(f.env.started.get(2).cancelled, "timeout cancels the call");
        f.js("var reason = {}; var ac3 = new AbortController(); fetch('x4', {signal: ac3.signal}).catch(function (e) { o.r = e === reason; }); ac3.abort(reason);");
        Check.equal("true", f.js("String(o.r)"), "the custom reason is the rejection value");
    }

    /**
     * Request objects: properties, defaults, guards, errors, clone and the use of a request by fetch.
     * <p>
     * Request 객체: 프로퍼티, 기본값, guard, 오류, clone, fetch 가 요청을 쓰는 방식.
     */
    public void testRequestObject() {
        WebFixture f = new WebFixture();
        Check.equal("POST|http://example.test/dir/a|cors|same-origin|default|follow|about:client||half|true", f.js("var q = new Request('a', {method: 'post', "
                + "body: 'b', headers: {'X-T': '1', 'Cookie': 'no'}}); [q.method, q.url, q.mode, q.credentials, q.cache, q.redirect, q.referrer, q.referrerPolicy, "
                + "q.duplex, q.signal instanceof AbortSignal].join('|')"), "properties");
        Check.equal("1|false", f.js("[q.headers.get('x-t'), q.headers.has('cookie')].join('|')"), "request guard drops forbidden headers");
        Check.equal("TypeError", f.js("try { new Request('a', {method: 'CONNECT'}); 'no' } catch (e) { e.name }"), "forbidden method");
        Check.equal("TypeError", f.js("try { new Request('a', {body: 'x'}); 'no' } catch (e) { e.name }"), "GET with a body");
        Check.equal("TypeError", f.js("try { new Request('http://u:p@h/'); 'no' } catch (e) { e.name }"), "credentials in the URL");
        Check.equal("TypeError", f.js("try { new Request('a', {mode: 'navigate'}); 'no' } catch (e) { e.name }"), "navigate mode");
        Check.equal("TypeError", f.js("try { new Request('a', {mode: 'bogus'}); 'no' } catch (e) { e.name }"), "invalid enum value");
        Check.equal("TypeError", f.js("try { new Request('a', {cache: 'only-if-cached'}); 'no' } catch (e) { e.name }"), "only-if-cached needs same-origin mode");
        Check.equal("TypeError", f.js("try { new Request('a', {mode: 'no-cors', method: 'PUT'}); 'no' } catch (e) { e.name }"), "no-cors needs a safelisted method");
        Check.equal("TypeError", f.js("try { new Request(); 'no' } catch (e) { e.name }"), "needs an argument");
        Check.equal("TypeError", f.js("try { Request('a'); 'no' } catch (e) { e.name }"), "needs new");
        Check.equal("true|PUT", f.js("var q2 = new Request(q, {method: 'put'}); [q.bodyUsed, q2.method].join('|')"), "a copy with a new method");
        Check.equal("true", f.js("String(q.bodyUsed)"), "the source of a body copy is used up");
        f.js("var q3 = new Request('c', {method: 'POST', body: 'xyz'}); var q4 = q3.clone(); var o = {}; q4.text().then(function (t) { o.t = t; });");
        Check.equal("xyz|false|true", f.js("[o.t, q3.bodyUsed, q4.bodyUsed].join('|')"), "clone has its own body");
        f.js("fetch(q3);");
        Check.equal("POST|xyz", f.env.started.get(0).request.getMethod() + "|" + new String(f.env.started.get(0).request.getBody()), "fetch(request)");
        Check.equal("true", f.js("String(q3.bodyUsed)"), "fetch uses up the body of the request");
        f.js("var q5 = new Request('d', {mode: 'no-cors'}); q5.headers.append('Accept', 'x/y'); q5.headers.append('X-Other', 'z');");
        Check.equal("x/y|null", f.js("[q5.headers.get('accept'), String(q5.headers.get('x-other'))].join('|')"), "no-cors request headers are limited");
        Check.equal("same-origin", f.js("new Request('e', {mode: 'same-origin'}).mode"), "mode");
    }

    /**
     * Response objects: the constructor, statics and guards.
     * <p>
     * Response 객체: 생성자, 정적 메서드, guard.
     */
    public void testResponseObject() {
        WebFixture f = new WebFixture();
        Check.equal("201|Created|true|default|text/plain;charset=UTF-8|1|false", f.js("var r = new Response('body', {status: 201, statusText: 'Created', "
                + "headers: {'X-R': '1', 'Set-Cookie': 'a=b'}}); [r.status, r.statusText, r.ok, r.type, r.headers.get('content-type'), r.headers.get('x-r'), "
                + "r.headers.has('set-cookie')].join('|')"), "constructor with init and the response guard");
        f.js("var o = {}; r.text().then(function (t) { o.t = t; });");
        Check.equal("body", f.js("o.t"), "text");
        Check.equal("200||true", f.js("var e = new Response(); [e.status, e.statusText, e.ok].join('|')"), "defaults");
        Check.equal("RangeError", f.js("try { new Response('x', {status: 199}); 'no' } catch (e) { e.name }"), "status below 200");
        Check.equal("RangeError", f.js("try { new Response('x', {status: 600}); 'no' } catch (e) { e.name }"), "status above 599");
        Check.equal("TypeError", f.js("try { new Response('x', {status: 204}); 'no' } catch (e) { e.name }"), "null body status with a body");
        Check.equal("TypeError", f.js("try { new Response(null, {statusText: 'bad\\nreason'}); 'no' } catch (e) { e.name }"), "bad statusText");
        Check.equal("error|0|false", f.js("var er = Response.error(); [er.type, er.status, er.ok].join('|')"), "Response.error()");
        Check.equal("TypeError", f.js("try { er.headers.set('a', 'b'); 'no' } catch (e) { e.name }"), "immutable headers");
        Check.equal("302|http://example.test/next", f.js("var rd = Response.redirect('/next'); [rd.status, rd.headers.get('location')].join('|')"), "Response.redirect");
        Check.equal("RangeError", f.js("try { Response.redirect('/x', 200); 'no' } catch (e) { e.name }"), "redirect status");
        f.js("var js = Response.json({a: [1]}, {status: 202}); js.json().then(function (j) { o.j = j.a[0]; });");
        Check.equal("202|application/json|1", f.js("[js.status, js.headers.get('content-type'), o.j].join('|')"), "Response.json");
        f.js("var cl = new Response('c'); var cc = cl.clone(); cl.text().then(function () { return cc.text(); }).then(function (t) { o.c = t; });");
        Check.equal("c", f.js("o.c"), "clone of a response");
        f.js("var ub = new Response('u'); ub.text(); o.again = ''; ub.text().catch(function (e) { o.again = e.name; });");
        Check.equal("TypeError", f.js("o.again"), "a body can be read once");
        Check.equal("TypeError", f.js("try { ub.clone(); 'no' } catch (e) { e.name }"), "clone of a used response");
    }

    /**
     * The Response of a fetch has immutable headers, hides what the request layer hid, and reports the type and the redirect flag.
     * <p>
     * fetch 의 Response 는 불변 헤더를 가지고, 요청 계층이 숨긴 것을 숨기며, 종류와 리디렉션 표시를 알린다.
     */
    public void testFetchedResponseProperties() {
        WebFixture f = new WebFixture();
        f.js("var o = {}; fetch('d').then(function (r) { o.r = r; });");
        WebResponse w = new WebResponse(WebResponse.TYPE_CORS);
        w.setStatus(200, "OK").setUrl("http://other.test/final#frag").setRedirected(true).setBody(new byte[] { 'x' }).addHeader("X-Exposed", "1");
        f.answer(0, w);
        Check.equal("cors|true|http://other.test/final|1", f.js("[o.r.type, o.r.redirected, o.r.url, o.r.headers.get('x-exposed')].join('|')"), "cors response");
        Check.equal("TypeError", f.js("try { o.r.headers.append('a', 'b'); 'no' } catch (e) { e.name }"), "immutable headers");
        f.js("var p = {}; fetch('o', {mode: 'no-cors'}).then(function (r) { p.r = r; return r.text(); }).then(function (t) { p.t = '[' + t + ']'; });");
        f.answer(1, new WebResponse(WebResponse.TYPE_OPAQUE));
        Check.equal("opaque|0|false|[]", f.js("[p.r.type, p.r.status, p.r.ok, p.t].join('|')"), "opaque response");
        f.js("var h = {}; fetch('h', {method: 'HEAD'}).then(function (r) { return r.text(); }).then(function (t) { h.t = t; });");
        f.answer(2, WebFixture.response(200, "text/plain", ""));
        Check.equal("", f.js("h.t"), "HEAD response has an empty body");
        f.js("var n = {}; fetch('n').then(function (r) { n.b = r.body; return r.text(); }).then(function (t) { n.t = t; });");
        f.answer(3, WebFixture.response(204, null, ""));
        Check.equal("null|", f.js("[String(n.b), n.t].join('|')"), "a null body status has no body");
    }

    /**
     * Options that the request layer cannot honour are refused, not ignored.
     * <p>
     * 요청 계층이 지킬 수 없는 옵션은 무시하지 않고 거부한다.
     */
    public void testUnsupportedOptionsAreRefused() {
        WebFixture f = new WebFixture();
        f.js("var o = {}; fetch('x', {redirect: 'manual'}).catch(function (e) { o.m = e.name + ':' + e.message; }); "
                + "fetch('y', {cache: 'only-if-cached', mode: 'same-origin'}).catch(function (e) { o.c = e.name + ':' + e.message; });");
        Check.isTrue(f.js("o.m").startsWith("TypeError:"), "redirect manual: " + f.js("o.m"));
        Check.isTrue(f.js("o.c").startsWith("TypeError:"), "only-if-cached: " + f.js("o.c"));
        Check.equal(0, f.env.started.size(), "nothing was sent");
    }

    /**
     * Subresource integrity: a matching digest passes, a different one rejects, an unknown algorithm is ignored.
     * <p>
     * 하위 리소스 무결성: 일치하는 다이제스트는 통과하고 다른 것은 거부하며 모르는 알고리즘은 무시한다.
     */
    public void testIntegrity() {
        Check.isTrue(FetchApi.integrityOk("sha256-LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=", ascii("hello")), "the sha256 of hello");
        Check.isFalse(FetchApi.integrityOk("sha256-LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=", ascii("hellp")), "another body");
        Check.isTrue(FetchApi.integrityOk("md5-xxxx", ascii("anything")), "an unknown algorithm is ignored");
        Check.isTrue(FetchApi.integrityOk("sha256-AAAA sha256-LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=", ascii("hello")), "one of several digests");
        Check.isFalse(FetchApi.integrityOk("sha256-LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ= sha512-AAAA", ascii("hello")),
                "the strongest listed algorithm decides");
        WebFixture f = new WebFixture();
        f.js("var o = {}; fetch('a', {integrity: 'sha256-LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ='}).then(function (r) { o.a = r.status; }); "
                + "fetch('b', {integrity: 'sha256-LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ='}).catch(function (e) { o.b = e.name; });");
        f.answer(0, WebFixture.response(200, "text/plain", "hello"));
        f.answer(1, WebFixture.response(200, "text/plain", "tampered"));
        Check.equal("200|TypeError", f.js("[o.a, o.b].join('|')"), "fetch with integrity");
    }

    /**
     * Pending fetches are cancelled when the page is told to stop; a late answer never runs script.
     * <p>
     * 페이지에 중지를 요청하면 대기 중인 fetch 가 취소되고 늦은 응답은 스크립트를 실행하지 않는다.
     */
    public void testDisposalCancelsFetches() {
        WebFixture f = new WebFixture();
        f.js("var o = []; fetch('a').then(function () { o.push('late'); }, function () { o.push('rejected'); });");
        f.scripts.requestDisposal();
        Check.isTrue(f.env.started.get(0).cancelled, "the call was cancelled by requestDisposal");
        f.env.started.get(0).callback.onComplete(WebFixture.response(200, "text/plain", "x"));
        f.run();
        f.scripts.dispose();
        Check.isNull(f.js("o.join()"), "a disposed page runs no script");
        Check.equal("", "", "no exception");
    }

    /**
     * Returns the ASCII bytes of a text.
     * <p>
     * 텍스트의 ASCII 바이트를 반환한다.
     *
     * @param s the text / 텍스트
     * @return the bytes / 바이트
     */
    private static byte[] ascii(String s) {
        byte[] b = new byte[s.length()];
        // 문자 하나가 바이트 하나이다.
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }
}
