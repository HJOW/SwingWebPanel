package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the value types of the web request APIs (plan item P20-08): <code>Headers</code> with its guards, <code>URLSearchParams</code>,
 * <code>URL</code>, <code>Blob</code>/<code>File</code>, <code>FormData</code> and <code>AbortController</code>/<code>AbortSignal</code>. The page is a real
 * script environment; nothing here touches the network.
 * <p>
 * 웹 요청 API의 값 형식 시험(계획 P20-08): guard 를 가진 <code>Headers</code>, <code>URLSearchParams</code>, <code>URL</code>,
 * <code>Blob</code>/<code>File</code>, <code>FormData</code>, <code>AbortController</code>/<code>AbortSignal</code>. 페이지는 실제 스크립트 환경이며
 * 여기서는 네트워크에 닿지 않는다.
 */
public class WebTypesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebTypesTest() {
    }

    /**
     * The interfaces exist on the global object, are functions and have the standard tags; Java is not visible.
     * <p>
     * 인터페이스가 전역 객체에 있고 함수이며 표준 태그를 가진다. Java 는 보이지 않는다.
     */
    public void testInterfacesExist() {
        WebFixture f = new WebFixture();
        String names = "XMLHttpRequest,XMLHttpRequestUpload,XMLHttpRequestEventTarget,ProgressEvent,Request,Response,Headers,Blob,File,FormData,"
                + "URL,URLSearchParams,AbortController,AbortSignal,FileReader,TextEncoder,TextDecoder";
        Check.equal("true", f.js("var ok = true; '" + names + "'.split(',').forEach(function (n) { if (typeof window[n] !== 'function') ok = false; });"
                + "String(ok && typeof fetch === 'function')"), "all interfaces and fetch exist");
        Check.equal("[object Headers],[object URL],[object FormData],[object Blob],[object AbortSignal]", f.js("[new Headers(), new URL('http://a/'), "
                + "new FormData(), new Blob([]), new AbortController().signal].map(function (o) { return Object.prototype.toString.call(o); })"
                + ".join()"), "toStringTag");
        Check.equal("undefined|undefined|undefined", f.js("[typeof Java, typeof java, typeof Packages].join('|')"), "no Java object");
    }

    /**
     * Headers: names are case-insensitive, values of one name are combined, iteration is sorted, Set-Cookie stays separate.
     * <p>
     * Headers: 이름은 대소문자를 구분하지 않고, 같은 이름의 값은 합쳐지며, 반복은 이름순이고, Set-Cookie 는 따로 남는다.
     */
    public void testHeadersListRules() {
        WebFixture f = new WebFixture();
        Check.equal("a, b|b|true|false", f.js("var h = new Headers({'X-Test': 'a'}); h.append('x-test', ' b '); h.append('Set-Cookie', 'c=1');"
                + "h.append('set-cookie', 'd=2'); [h.get('X-TEST'), new Headers([['k','b']]).get('K'), h.has('x-Test'), h.has('nope')].join('|')"),
                "get combines");
        Check.equal("set-cookie=c=1;set-cookie=d=2;x-test=a, b", f.js("var o = []; h.forEach(function (v, k) { o.push(k + '=' + v); }); o.join(';')"),
                "sorted iteration, set-cookie separate");
        Check.equal("c=1,d=2", f.js("h.getSetCookie().join()"), "getSetCookie");
        Check.equal("x-test,set-cookie,set-cookie".split(",").length + "", f.js("String(Array.from(h.keys()).length)"), "keys");
        Check.equal("z", f.js("h.set('x-test', 'z'); h.get('x-test')"), "set replaces");
        Check.equal("false", f.js("h.delete('X-Test'); String(h.has('x-test'))"), "delete");
        Check.equal("TypeError", f.js("try { new Headers().append('bad name', 'x'); 'no' } catch (e) { e.name }"), "invalid name");
        Check.equal("TypeError", f.js("try { new Headers().append('x', 'a\\nb'); 'no' } catch (e) { e.name }"), "invalid value");
        Check.equal("TypeError", f.js("try { new Headers().append('x', 'caf\\u00e9\\u20ac'); 'no' } catch (e) { e.name }"), "not a ByteString");
        Check.equal("x=1;y=2", f.js("var m = new Headers(new Headers({x: '1', y: '2'})); Array.from(m).map(function (p) { return p.join('='); }).join(';')"),
                "init from a Headers object");
    }

    /**
     * Creates object addresses for Blobs and Files and exposes their URL origin.
     * <p>
     * Blob과 File의 객체 주소를 만들고 URL 오리진을 노출한다.
     */
    public void testBlobUrlBindings() {
        WebFixture f = new WebFixture();
        Check.equal("blob:|http://example.test|true|true|undefined", f.js(
                "var b=new File(['abc'],'example.txt');var a=URL.createObjectURL(b),c=URL.createObjectURL(b);"
                + "[new URL(a).protocol,new URL(c).origin,a!==c,new URL('#id',a).href===a+'#id',String(URL.revokeObjectURL(a))].join('|')"),
                "the Blob URL has a unique identity and its creator's origin");
        Check.equal("TypeError", f.js("try{URL.createObjectURL({size:3});}catch(e){e.name}"), "a Blob is required");
    }

    /**
     * URLSearchParams: parsing, serialization, mutation, sort, iteration and the link with URL.
     * <p>
     * URLSearchParams: 해석, 직렬화, 변경, 정렬, 반복, URL 과의 연결.
     */
    public void testUrlSearchParams() {
        WebFixture f = new WebFixture();
        Check.equal("a=1&b=2&a=3&c=x+y%26z%C3%A9", f.js("var p = new URLSearchParams('?a=1&b=2&a=3&c=x%20y%26z%C3%A9'); p.toString()"), "parse and serialize");
        Check.equal("1|3|1,3|true|false", f.js("[p.get('a'), p.getAll('a')[1], p.getAll('a').join(), p.has('b'), p.has('b', '9')].join('|')"), "get/getAll/has");
        Check.equal("a=9&b=2&c=x+y%26z%C3%A9", f.js("p.set('a', '9'); p.toString()"), "set replaces the first and removes the rest");
        Check.equal("a=9&c=x+y%26z%C3%A9", f.js("p.delete('b'); p.toString()"), "delete");
        Check.equal("a=1&a=2&b=3", f.js("var q = new URLSearchParams([['b','3'],['a','1'],['a','2']]); q.sort(); q.toString()"), "sort is stable, init from pairs");
        Check.equal("x=1&y=2", f.js("new URLSearchParams({x: 1, y: 2}).toString()"), "init from a record");
        Check.equal("2", f.js("String(new URLSearchParams('a=1&b=2').size)"), "size");
        Check.equal("a=1;b=2", f.js("var o = []; new URLSearchParams('a=1&b=2').forEach(function (v, k) { o.push(k + '=' + v); }); o.join(';')"), "forEach");
        Check.equal("a:1,b:2", f.js("Array.from(new URLSearchParams('a=1&b=2')).map(function (e) { return e[0] + ':' + e[1]; }).join()"), "iterable");
        Check.equal("%EF%BF%BD", f.js("new URLSearchParams([['x', '\\ud800']]).toString().substring(2)"), "lone surrogate becomes U+FFFD");
    }

    /**
     * URL: components, resolution against a base, setters and the linked searchParams.
     * <p>
     * URL: 구성요소, 기준 URL에 대한 해석, setter, 연결된 searchParams.
     */
    public void testUrlObject() {
        WebFixture f = new WebFixture();
        Check.equal("https:|user|pw|example.com:8080|example.com|8080|/a/b|?x=1&y=2|#frag|https://example.com:8080",
                f.js("var u = new URL('HTTPS://user:pw@EXAMPLE.com:8080/a/./c/../b?x=1&y=2#frag'); [u.protocol, u.username, u.password, u.host, u.hostname, "
                        + "u.port, u.pathname, u.search, u.hash, u.origin].join('|')"), "components");
        Check.equal("http://example.com/a/b?q=1", f.js("new URL('/a/b?q=1', 'http://example.com/x/y').href"), "absolute-path reference");
        Check.equal("http://example.com/x/rel", f.js("new URL('rel', 'http://example.com/x/y').href"), "relative reference");
        Check.equal("http://example.com/", f.js("new URL('http://example.com').href"), "empty path becomes /");
        Check.equal("http://example.com/", f.js("new URL('http://example.com:80/').href"), "default port removed");
        Check.equal("http://a/b/c", f.js("new URL('http:\\\\\\\\a\\\\b\\\\c').href"), "backslashes are slashes for special schemes");
        Check.equal("TypeError", f.js("try { new URL('not a url'); 'no' } catch (e) { e.name }"), "invalid URL throws");
        Check.equal("true|false|true", f.js("[URL.canParse('http://a/'), URL.canParse('x'), URL.canParse('x', 'http://a/')].join('|')"), "canParse");
        Check.equal("null|http://a/x", f.js("[String(URL.parse('x')), URL.parse('x', 'http://a/').href].join('|')"), "parse");
        Check.equal("null", f.js("new URL('data:text/plain,hi').origin"), "opaque origin");
        Check.equal("http://example.com/p?q=2&r=3#h", f.js("var v = new URL('http://example.com/p?q=1'); v.searchParams.set('q', '2'); "
                + "v.searchParams.append('r', '3'); v.hash = 'h'; v.href"), "searchParams updates the query");
        Check.equal("q=9&z=1", f.js("v.search = '?q=9&z=1'; v.searchParams.toString()"), "search rebuilds the list");
        Check.equal("http://example.com:81/new", f.js("v.search = ''; v.hash = ''; v.port = '81'; v.pathname = 'new'; v.href"), "port and pathname setters");
        Check.equal("https://h2.test:81/new", f.js("v.protocol = 'https'; v.hostname = 'h2.test'; v.href"), "protocol and hostname setters");
        Check.equal("http://u:p@a.test/", f.js("var w = new URL('http://a.test/'); w.username = 'u'; w.password = 'p'; w.href"), "user info setters");
        Check.equal("http://x.test/", f.js("v.href = 'http://x.test/'; v.href"), "href setter");
        Check.equal("\"http://x.test/\"", f.js("JSON.stringify(v)"), "toJSON");
        Check.equal("true", f.js("String(v.searchParams === v.searchParams)"), "searchParams is the same object");
        Check.equal("http://xn--bcher-kva.example/", f.js("new URL('http://b\\u00fccher.example/').href"), "IDN host becomes ASCII");
    }

    /**
     * Blob and File: construction from parts, size, type, slice and the promise based readers.
     * <p>
     * Blob 과 File: 부분으로 만들기, 크기, 타입, 자르기, 프라미스 기반 읽기.
     */
    public void testBlobAndFile() {
        WebFixture f = new WebFixture();
        Check.equal("7|text/plain|true", f.js("var b = new Blob(['ab', new Uint8Array([99, 100]), new Blob(['ef']), 'g'], {type: 'Text/Plain'}); "
                + "[b.size, b.type, b instanceof Blob].join('|')"), "size and lower-cased type");
        Check.equal("", f.js("new Blob([], {type: 'caf\\u00e9'}).type"), "a type with a non-ASCII character becomes empty");
        Check.equal("3|1|0", f.js("[b.slice(2, 5).size, b.slice(-3, -2).size, b.slice(3, 1).size].join('|')"), "slice sizes");
        f.js("var out = {}; b.slice(2, 5, 'x/y').text().then(function (t) { out.t = t; }); b.arrayBuffer().then(function (a) { out.n = a.byteLength; }); "
                + "b.bytes().then(function (u) { out.u = u[0]; })");
        Check.equal("cde|7|97", f.js("[out.t, out.n, out.u].join('|')"), "text, arrayBuffer and bytes");
        Check.equal("x/y", f.js("b.slice(2, 5, 'X/Y').type"), "slice type");
        Check.equal("a.txt|text/plain|1234|true|true", f.js("var file = new File(['hi'], 'a/b.txt', {type: 'text/plain', lastModified: 1234}); "
                + "[file.name.replace(':', '.').replace('a.b', 'a'), file.type, file.lastModified, file instanceof Blob, file instanceof File].join('|')"),
                "file properties");
        Check.equal("2", f.js("String(file.size)"), "file size");
        Check.equal("TypeError", f.js("try { new File(['x']); 'no' } catch (e) { e.name }"), "File needs a name");
        Check.equal("TypeError", f.js("try { Blob(); 'no' } catch (e) { e.name }"), "Blob needs new");
    }

    /**
     * FormData: entries, replacement rules, Blob values become files, iteration.
     * <p>
     * FormData: 항목, 교체 규칙, Blob 값은 파일이 됨, 반복.
     */
    public void testFormData() {
        WebFixture f = new WebFixture();
        Check.equal("1|2|1,2|true|false|a", f.js("var d = new FormData(); d.append('a', 1); d.append('a', '2'); d.append('b', 'x'); "
                + "[d.get('a'), d.getAll('a')[1], d.getAll('a').join(), d.has('b'), d.has('c'), d.keys().next().value].join('|')"), "append/get/getAll/has");
        Check.equal("a=9;b=x", f.js("d.set('a', '9'); var o = []; d.forEach(function (v, k) { o.push(k + '=' + v); }); o.join(';')"), "set replaces the first");
        Check.equal("b=x", f.js("d.delete('a'); Array.from(d).map(function (e) { return e[0] + '=' + e[1]; }).join(';')"), "delete");
        Check.equal("blob|true|true|3", f.js("d.append('f', new Blob(['abc'], {type: 't/x'})); var fv = d.get('f'); "
                + "[fv.name, fv instanceof Blob, fv instanceof File, fv.size].join('|')"), "a Blob becomes a File named blob");
        Check.equal("n.txt|t/x", f.js("d.set('f', fv, 'n.txt'); [d.get('f').name, d.get('f').type].join('|')"), "file name argument");
        Check.equal("TypeError", f.js("try { d.append('x', 'not a blob', 'n'); 'no' } catch (e) { e.name }"), "file name needs a Blob");
        WebFixture g = new WebFixture("http://example.test/p", "<html><body><form id=f><input name=a value=1><input name=b type=checkbox checked>"
                + "<input name=c type=checkbox value=v><input name=h type=hidden><input type=submit name=s value=go><input name=dis disabled value=x>"
                + "<textarea name=t>text</textarea><select name=sel><option value=o1>One</option><option value=o2 selected>Two</option></select>"
                + "<input name=nm></form></body></html>");
        Check.equal("a=1;b=on;h=;t=text;sel=o2;nm=", g.js("Array.from(new FormData(document.getElementById('f'))).map(function (e) { return e[0] + '=' + e[1]; })"
                + ".join(';')"), "the entries of a form");
        Check.equal("TypeError", g.js("try { new FormData({}); 'no' } catch (e) { e.name }"), "not a form");
    }

    /**
     * Multipart encoding of a FormData: boundaries, headers, file parts and escaping.
     * <p>
     * FormData 의 multipart 인코딩: 경계, 헤더, 파일 부분, 이스케이프.
     */
    public void testMultipartEncoding() {
        java.util.List<Object[]> entries = new java.util.ArrayList<Object[]>();
        entries.add(new Object[] { "a\"b", "line1\nline2" });
        byte[] body = FormDataApi.multipart(entries, "BOUND");
        String text = Encodings.utf8Raw(body);
        Check.equal("--BOUND\r\nContent-Disposition: form-data; name=\"a%22b\"\r\n\r\nline1\r\nline2\r\n--BOUND--\r\n", text, "multipart text part");
    }

    /**
     * AbortController and AbortSignal: events, reason, static helpers and the timeout on the page clock.
     * <p>
     * AbortController 와 AbortSignal: 이벤트, 이유, 정적 도우미, 페이지 시계의 타임아웃.
     */
    public void testAbort() {
        WebFixture f = new WebFixture();
        Check.equal("false|undefined", f.js("var c = new AbortController(); var s = c.signal; [s.aborted, String(s.reason)].join('|')"), "initial state");
        f.js("var log = []; s.addEventListener('abort', function (e) { log.push('listener:' + e.type + ':' + e.target.aborted); }); "
                + "s.onabort = function () { log.push('onabort'); }; c.abort(); c.abort();");
        Check.equal("listener:abort:true,onabort", f.js("log.join()"), "abort fires once, listeners first then the handler");
        Check.equal("true|AbortError|20", f.js("[s.aborted, s.reason.name, s.reason.code].join('|')"), "default reason is an AbortError DOMException");
        Check.equal("AbortError", f.js("try { s.throwIfAborted(); 'no' } catch (e) { e.name }"), "throwIfAborted");
        Check.equal("why", f.js("var c2 = new AbortController(); c2.abort('why'); String(c2.signal.reason)"), "custom reason");
        Check.equal("true|AbortError", f.js("var a = AbortSignal.abort(); [a.aborted, a.reason.name].join('|')"), "AbortSignal.abort()");
        f.js("var t = AbortSignal.timeout(50); var tlog = []; t.addEventListener('abort', function () { tlog.push(t.reason.name); });");
        f.advance(49);
        Check.equal("false|", f.js("[t.aborted, tlog.join()].join('|')"), "not yet");
        f.advance(2);
        Check.equal("true|TimeoutError", f.js("[t.aborted, tlog.join()].join('|')"), "timeout aborts with a TimeoutError");
        Check.equal("true|why2", f.js("var c3 = new AbortController(); var c4 = new AbortController(); var any = AbortSignal.any([c3.signal, c4.signal]); "
                + "c4.abort('why2'); [any.aborted, String(any.reason)].join('|')"), "AbortSignal.any follows the first abort");
        Check.equal("true", f.js("String(AbortSignal.any([AbortSignal.abort('x')]).aborted)"), "any of an aborted signal");
        Check.equal("TypeError", f.js("try { new AbortSignal(); 'no' } catch (e) { e.name }"), "AbortSignal cannot be constructed");
    }

    /**
     * TextEncoder and TextDecoder: UTF-8 encoding, encodeInto, labels, fatal mode, the byte order mark and streaming.
     * <p>
     * TextEncoder 와 TextDecoder: UTF-8 인코딩, encodeInto, 레이블, fatal 모드, 바이트 순서 표시, 스트리밍.
     */
    public void testTextEncoderAndDecoder() {
        WebFixture f = new WebFixture();
        Check.equal("utf-8|9|104,105,226,130,172,240,159,152,128", f.js("var te = new TextEncoder(); var u = te.encode('hi\\u20ac\\ud83d\\ude00'); "
                + "[te.encoding, u.length, Array.from(u).join()].join('|')"), "encode");
        Check.equal("239,191,189|0", f.js("[Array.from(te.encode('\\ud800')).join(), te.encode().length].join('|')"), "lone surrogate, default argument");
        Check.equal("2|4|104,226,130,172,0", f.js("var dst = new Uint8Array(5); var r = te.encodeInto('h\\u20ac\\u20ac', dst); "
                + "[r.read, r.written, Array.from(dst).join()].join('|')"), "encodeInto writes whole code points only");
        Check.equal("utf-8|false|false|hi" + (char) 0x20AC, f.js("var td = new TextDecoder(); [td.encoding, td.fatal, td.ignoreBOM, "
                + "td.decode(new Uint8Array([104, 105, 226, 130, 172]))].join('|')"), "decode");
        Check.equal("A|2", f.js("[new TextDecoder().decode(new Uint8Array([239, 187, 191, 65])), "
                + "new TextDecoder('utf-8', {ignoreBOM: true}).decode(new Uint8Array([239, 187, 191, 65])).length].join('|')"), "BOM handling");
        Check.equal("windows-1252|" + (char) 0xE9 + "|" + (char) 0xAC00 + "|AB", f.js("[new TextDecoder('latin1').encoding, "
                + "new TextDecoder('latin1').decode(new Uint8Array([233])), new TextDecoder('euc-kr').decode(new Uint8Array([176, 161])), "
                + "new TextDecoder('utf-16le').decode(new Uint8Array([65, 0, 66, 0]))].join('|')"), "labels");
        Check.equal("TypeError|" + (char) 0xFFFD, f.js("var out = []; try { new TextDecoder('utf-8', {fatal: true}).decode(new Uint8Array([255])); out.push('no'); } "
                + "catch (e) { out.push(e.name); } out.push(new TextDecoder().decode(new Uint8Array([255]))); out.join('|')"), "fatal and replacement");
        Check.equal("|" + (char) 0x20AC + "|", f.js("var sd = new TextDecoder(); [sd.decode(new Uint8Array([226, 130]), {stream: true}), "
                + "sd.decode(new Uint8Array([172])), sd.decode()].join('|')"), "streaming keeps the bytes of an incomplete sequence");
        Check.equal("RangeError", f.js("try { new TextDecoder('no-such-encoding'); 'no' } catch (e) { e.name }"), "unknown label");
        Check.equal("TypeError", f.js("try { new TextDecoder().decode('text'); 'no' } catch (e) { e.name }"), "decode needs a buffer");
    }

    /**
     * Checks shared fixed-length storage, observable option conversion and invalid Encoding API buffer types.
     * <p>
     * 고정 길이 공유 저장소, 관찰 가능한 옵션 변환, Encoding API의 잘못된 버퍼 타입을 확인한다.
     */
    public void testTextEncodingWithSharedMemory() {
        WebFixture f = new WebFixture(true);
        String source = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "var b=new SharedArrayBuffer(6), small=new SharedArrayBuffer(1);"
                + "var dst=new Uint8Array(b,1,4), te=new TextEncoder(), td=new TextDecoder();"
                + "var r=te.encodeInto('€AA',dst); var u=new Uint8Array(small); u[0]=65;"
                + "var d=td.decode(new DataView(b,1,4));"
                + "var opts={get stream(){Atomics.store(u,0,90);return false;}};"
                + "var grown=new SharedArrayBuffer(4,{maxByteLength:8}), growview=new Uint8Array(grown,0,4);"
                + "var ordinary=new ArrayBuffer(4), detached=new Uint8Array(ordinary); ordinary.transfer();"
                + "var empty=te.encodeInto('a',detached);"
                + "[r.read,r.written,new Uint8Array(b).toHex(),d,td.decode(small,opts),"
                + "fail(()=>td.decode(grown)),fail(()=>td.decode(growview)),fail(()=>te.encodeInto('a',growview)),"
                + "fail(()=>te.encodeInto('a',new Uint8ClampedArray(4))),td.decode(detached).length,empty.read,empty.written].join('|')";
        Check.equal("2|4|00e282ac4100|€A|Z|TypeError|TypeError|TypeError|TypeError|0|0|0", f.js(source),
                "Encoding accepts fixed shared buffers and accesses their actual bytes");
    }

    /**
     * Checks the Web IDL union conversion for raw shared buffers and rejection of shared views and resizable buffers.
     * <p>
     * 공유 버퍼 자체의 Web IDL 합집합 변환과 공유 뷰 및 가변 버퍼 거부를 확인한다.
     */
    public void testSharedMemoryBufferSourceBoundaries() {
        WebFixture f = new WebFixture(true);
        String source = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "var b=new SharedArrayBuffer(4), u=new Uint8Array(b), d=new DataView(b), r=new ArrayBuffer(4,{maxByteLength:8});"
                + "b.toString=()=> 'raw-tag';"
                + "var xhr=new XMLHttpRequest();xhr.open('POST','http://example.test/shared');"
                + "[new Blob([b]).size,new Response(b).headers.get('content-type'),"
                + "fail(()=>new Blob([u])),fail(()=>new Blob([r])),fail(()=>new Response(d)),"
                + "fail(()=>new Request('http://example.test/shared',{method:'POST',body:u})),fail(()=>xhr.send(u)),"
                + "fail(()=>crypto.getRandomValues(u)),fail(()=>crypto.getRandomValues(new Uint8Array(r))),"
                + "fail(()=>crypto.getRandomValues(new Float16Array(1)))].join('|')";
        Check.equal("7|text/plain;charset=UTF-8|TypeError|TypeError|TypeError|TypeError|TypeError|TypeError|TypeError|TypeMismatchError",
                f.js(source), "raw shared values use the string union member and ineligible binary views are rejected");
    }

    /**
     * FileReader: the four read methods, the events and their order, aborting between the tasks, and the exceptions.
     * <p>
     * FileReader: 읽기 메서드 넷, 이벤트와 그 순서, 작업 사이의 중단, 예외.
     */
    public void testFileReader() {
        WebFixture f = new WebFixture();
        f.js("var log = []; var r = new FileReader(); ['loadstart','progress','load','abort','error','loadend'].forEach(function (n) { "
                + "r.addEventListener(n, function (e) { log.push(n + ':' + r.readyState + ':' + e.loaded + '/' + e.total); }); }); "
                + "r.readAsText(new Blob(['\\ud55c\\uae00 text']));");
        Check.equal("1|null|", f.js("[r.readyState, String(r.result), log.join()].join('|')"), "reading started, nothing yet");
        f.run();
        Check.equal("loadstart:1:0/11,progress:2:11/11,load:2:11/11,loadend:2:11/11", f.js("log.join()"), "events");
        Check.equal("2|" + (char) 0xD55C + (char) 0xAE00 + " text|null", f.js("[r.readyState, r.result, String(r.error)].join('|')"), "result");
        f.js("var o = {}; var r2 = new FileReader(); r2.onload = function () { o.ab = r2.result.byteLength + ':' + (r2.result instanceof ArrayBuffer); }; "
                + "r2.readAsArrayBuffer(new Blob(['abc']));"
                + "var r3 = new FileReader(); r3.onload = function () { o.bin = r3.result.length + ':' + r3.result.charCodeAt(0) + ',' + r3.result.charCodeAt(2); }; "
                + "r3.readAsBinaryString(new Blob([new Uint8Array([226, 130, 172])]));"
                + "var r4 = new FileReader(); r4.onload = function () { o.url = r4.result; }; r4.readAsDataURL(new Blob(['hi'], {type: 'text/plain'}));"
                + "var r5 = new FileReader(); r5.onload = function () { o.kr = r5.result; }; r5.readAsText(new Blob([new Uint8Array([176, 161])]), 'euc-kr');"
                + "var r6 = new FileReader(); r6.onload = function () { o.def = r6.result; }; r6.readAsDataURL(new Blob(['x']));");
        f.run();
        Check.equal("3:true", f.js("o.ab"), "readAsArrayBuffer");
        Check.equal("3:226,172", f.js("o.bin"), "readAsBinaryString");
        Check.equal("data:text/plain;base64,aGk=", f.js("o.url"), "readAsDataURL");
        Check.equal(String.valueOf((char) 0xAC00), f.js("o.kr"), "readAsText with an encoding");
        Check.equal("data:application/octet-stream;base64,eA==", f.js("o.def"), "readAsDataURL without a type");
        f.js("var al = []; var r7 = new FileReader(); ['abort','load','loadend','loadstart'].forEach(function (n) { "
                + "r7.addEventListener(n, function () { al.push(n + r7.readyState); }); }); r7.readAsText(new Blob(['x'])); r7.abort();");
        f.run();
        Check.equal("abort2,loadend2|null", f.js("al.join() + '|' + String(r7.result)"), "abort before the tasks run");
        f.js("var al2 = []; var r8 = new FileReader(); r8.onloadstart = function () { r8.abort(); }; r8.onload = function () { al2.push('load'); }; "
                + "r8.onabort = function () { al2.push('abort'); }; r8.readAsText(new Blob(['x']));");
        f.run();
        Check.equal("abort", f.js("al2.join()"), "abort from the loadstart handler stops the result");
        Check.equal("InvalidStateError", f.js("var r9 = new FileReader(); r9.readAsText(new Blob([])); try { r9.readAsText(new Blob([])); 'no' } "
                + "catch (e) { e.name }"), "busy reader");
        Check.equal("TypeError", f.js("try { new FileReader().readAsText('x'); 'no' } catch (e) { e.name }"), "not a Blob");
        Check.equal("0|1|2", f.js("[FileReader.EMPTY, FileReader.LOADING, FileReader.DONE].join('|')"), "constants");
    }
}
