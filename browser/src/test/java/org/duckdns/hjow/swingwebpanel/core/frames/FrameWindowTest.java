package org.duckdns.hjow.swingwebpanel.core.frames;

import java.util.Vector;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.AssertionFailure;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The window relations of nested browsing contexts through the real component (plan item P20-02, stage c): <code>parent</code>,
 * <code>top</code>, <code>frames</code>, <code>length</code>, <code>frameElement</code>, <code>name</code>, <code>contentWindow</code> and
 * <code>contentDocument</code>; windows of the same origin are the real global objects, windows of another origin show only the members that
 * the standard allows and throw a <code>SecurityError</code> otherwise; documents that inherit their origin (<code>srcdoc</code>,
 * <code>about:blank</code>) are of the same origin as their creator; messages cross the frames.
 * <p>
 * 실제 컴포넌트로 확인하는 중첩 브라우징 문맥의 window 관계(계획 P20-02, 단계 c): <code>parent</code>, <code>top</code>, <code>frames</code>,
 * <code>length</code>, <code>frameElement</code>, <code>name</code>, <code>contentWindow</code>, <code>contentDocument</code>. 같은 오리진의 window 는
 * 실제 전역 객체이고, 다른 오리진의 window 는 표준이 허용하는 멤버만 보이며 그 밖에는 <code>SecurityError</code> 를 던진다. 오리진을 물려받는 문서
 * (<code>srcdoc</code>, <code>about:blank</code>)는 생성자와 같은 오리진이다. 메시지는 프레임을 넘나든다.
 */
public class FrameWindowTest {

    /**
     * The style that removes margins and borders so that the pixels of the tests have known positions.
     * <p>
     * 시험 화소의 위치를 알 수 있도록 여백과 경계선을 없애는 스타일.
     */
    private static final String RESET = "<style>html,body{margin:0;padding:0;background:#fff}iframe{border:0;display:block}</style>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FrameWindowTest() {
    }

    /**
     * Escapes a document for the value of a <code>srcdoc</code> attribute.
     * <p>
     * 문서를 <code>srcdoc</code> 속성 값으로 쓰도록 이스케이프한다.
     *
     * @param html the document / 문서
     * @return the attribute text / 속성 텍스트
     */
    private static String attr(String html) {
        return html.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Serves a page and loads it.
     * <p>
     * 페이지를 제공하고 로드한다.
     *
     * @param panel  the component / 컴포넌트
     * @param server the server / 서버
     * @param html   the page / 페이지
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private static void serve(SwingWebPanel panel, TestHttpServer server, String html) throws Exception {
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.start();
        panel.setPage(server.url("/page"));
    }

    /**
     * Checks globalThis from both sides of a same-origin iframe.
     * <p>
     * 같은 오리진의 iframe에서 내부와 부모가 보는 globalThis를 확인한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGlobalThis() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>window.ownGlobalIsThis=globalThis===window && window===this && parent!==this;</script>";
            serve(panel, server, "<!DOCTYPE html><iframe id=\"f\" srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<script>window.onload=function(){var child=document.getElementById('f').contentWindow;"
                    + " var d=Object.getOwnPropertyDescriptor(child,'globalThis');"
                    + " sessionStorage.setItem('identity',String(child.ownGlobalIsThis && d.value===child && child.globalThis===child"
                    + " && child!==globalThis && d.writable && !d.enumerable && d.configurable));};</script>");
            FrameTestSupport.awaitStored(panel, "identity", "true");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Checks private stamping on local and cross-origin WindowProxy and Location objects.
     * <p>
     * 현재 오리진과 교차 오리진의 WindowProxy 및 Location에 private 요소 추가를 확인한다.
     *
     * @throws Exception if a test host cannot be started or the page does not report its result / 시험 서버나 페이지 응답 실패
     */
    public void testPrivateElementsOnHostObjects() throws Exception {
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            other.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>cross</title>"));
            other.start();
            serve(panel, server, "<!doctype html><iframe id=f src='" + other.url("/child") + "'></iframe>"
                    + "<script>window.onload=function(){var n=0, child=document.getElementById('f').contentWindow;"
                    + "class Return { constructor(o){return o;} }"
                    + "class Field extends Return { #value=++n; static has(o){return #value in o;} }"
                    + "class Method extends Return { #value(){} ordinary=++n; }"
                    + "var errs=[window,location,child,child.location].map(function(o){"
                    + "var a;try{new Field(o);}catch(e){a=e.name;}"
                    + "try{new Method(o);}catch(e){return a+':'+e.name+':'+Field.has(o);}});"
                    + "class Public extends Return { ordinary=1; } new Public(window);"
                    + "var proxy=new Proxy(window,{}); new Field(proxy);"
                    + "sessionStorage.setItem('private-host',errs.join('|')+'|'+[n,window.ordinary,Field.has(proxy)].join());};</script>");
            FrameTestSupport.awaitStored(panel, "private-host",
                    "TypeError:TypeError:false|TypeError:TypeError:false|TypeError:TypeError:false|TypeError:TypeError:false|5,1,true");
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
    }

    /**
     * Verifies a same-origin window transfers a resizable buffer and its length-tracking views back to the parent.
     * <p>
     * 같은 오리진 창이 부모에게 가변 버퍼와 길이 추적 뷰를 돌려주는지 확인한다.
     *
     * @throws Exception on page or test server failure / 페이지 또는 시험 서버 실패
     */
    public void testResizableBufferBetweenWindows() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>addEventListener('message',e=>{const d=e.data;d.buf.resize(6);"
                    + "parent.postMessage(d,e.origin,[d.buf]);});</script>";
            serve(panel, server, "<!doctype html><iframe id=f srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<script>addEventListener('message',e=>{const d=e.data;d.buf.resize(8);"
                    + "sessionStorage.setItem('resizable-window',[d.buf.resizable,d.buf.maxByteLength,d.values.length,"
                    + "d.view.byteLength,d.values[0],d.values.buffer===d.buf&&d.view.buffer===d.buf].join());});"
                    + "window.onload=()=>{const b=new ArrayBuffer(2,{maxByteLength:8}),a=new Float16Array(b);a[0]=1.5;"
                    + "document.getElementById('f').contentWindow.postMessage({buf:b,values:a,view:new DataView(b)},location.origin,[b]);"
                    + "sessionStorage.setItem('window-detached',String(b.detached));};</script>");
            FrameTestSupport.awaitStored(panel, "resizable-window", "true,8,4,8,1.5,true");
            FrameTestSupport.awaitStored(panel, "window-detached", "true");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Runs eval in a child realm while resolving imports and caching their records in the calling document.
     * <p>
     * 자식 렐름에서 eval을 실행하면서 호출한 문서에서 import를 해석하고 모듈 레코드를 캐시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossRealmEvalImportsFromActiveRecord() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/child/frame", TestHttpServer.Response.html("<script>var marker='child';</script>"));
            server.routeFixed("/outer/load.js", TestHttpServer.Response.ok("text/javascript",
                    "export default [globalThis.marker,import.meta.url.endsWith('/outer/load.js')].join('|');"));
            server.routeFixed("/outer/main.js", TestHttpServer.Response.ok("text/javascript",
                    "var marker='parent'; window.onload=()=>{const child=document.getElementById('f').contentWindow;"
                    + "const order=[];child.Promise.resolve().then(()=>order.push('child'));"
                    + "child.queueMicrotask(()=>order.push('queue'));Promise.resolve().then(()=>order.push('parent'));"
                    + "const run=child.eval('import(\"./load.js\")'), registered=Promise.resolve('import(\"./load.js\")').then(child.eval);"
                    + "Promise.all([run,registered,import('./load.js')]).then(a=>sessionStorage.setItem('foreign-import',"
                    + "[child.eval('marker'),run instanceof child.Promise,a[0].default,a[0]===a[1]&&a[1]===a[2],order.join()].join(';')),"
                    + "e=>sessionStorage.setItem('foreign-import',String(e)));};"));
            serve(panel, server, "<!doctype html><iframe id=f src='/child/frame'></iframe><script src='/outer/main.js'></script>");
            FrameTestSupport.awaitStored(panel, "foreign-import", "child;true;parent|true;true;child,queue,parent");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Checks the same frame reference before and after matching and nonmatching origin navigations.
     * <p>
     * 같은 프레임 참조를 유지한 채 같은 오리진과 다른 오리진으로 이동했을 때를 확인한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStableWindowProxyAcrossNavigation() throws Exception {
        TestHttpServer first = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        final Vector<String> console = new Vector<String>();
        panel.addConsoleListener(new ConsoleListener() {
            public void consoleMessage(ConsoleEvent event) {
                console.add(event.getMessage());
            }
        });
        try {
            other.routeFixed("/remote", TestHttpServer.Response.html("<script>var secret=9;"
                    + "onmessage=function(e){if(e.data==='ping')e.source.postMessage('pong','*');};</script>"));
            other.start();
            first.routeFixed("/one", TestHttpServer.Response.html("<script>var marker='one';</script>"));
            first.routeFixed("/two", TestHttpServer.Response.html("<script>var marker='two';</script>"));
            serve(panel, first, "<!DOCTYPE html><iframe id=\"f\" src=\"/one\"></iframe><script>"
                    + "var f=document.getElementById('f'), held;"
                    + "window.onload=function(){held=f.contentWindow;"
                    + " sessionStorage.setItem('first',[held.marker,held===held.globalThis,held===frames[0]].join('|'));"
                    + " f.addEventListener('load',same); setTimeout(function(){f.setAttribute('src','/two');},100);};"
                    + "function same(){f.removeEventListener('load',same);"
                    + " sessionStorage.setItem('same',[held===f.contentWindow,held.marker,held===held.globalThis].join('|'));"
                    + " f.addEventListener('load',cross); f.setAttribute('src','" + other.url("/remote") + "');}"
                    + "function cross(){f.removeEventListener('load',cross); var denied=''; try{denied=String(held.secret);}catch(e){denied=e.name;}"
                    + " sessionStorage.setItem('cross',[held===f.contentWindow,held===held.self,held.parent===window,denied].join('|'));"
                    + " window.onmessage=function(e){if(e.data==='pong'){sessionStorage.setItem('message',String(e.source===held));"
                    + " f.parentNode.removeChild(f); var timer=setInterval(function(){if(held.closed){clearInterval(timer);"
                    + " sessionStorage.setItem('closed','true');}},20);}};"
                    + " held.postMessage('ping','*');}</script>");
            FrameTestSupport.awaitStored(panel, "first", "one|true|true");
            FrameTestSupport.awaitStored(panel, "same", "true|two|true");
            FrameTestSupport.awaitStored(panel, "cross", "true|true|true|SecurityError");
            FrameTestSupport.awaitStored(panel, "message", "true");
            FrameTestSupport.awaitStored(panel, "closed", "true");
        } catch (AssertionFailure failure) {
            throw new AssertionFailure(failure.getMessage() + ", second-request=" + first.count("/two")
                    + ", cross-origin-request=" + other.count("/remote") + ", console=" + console);
        } finally {
            panel.dispose();
            first.stop();
            other.stop();
        }
    }

    /**
     * The top-level document is its own parent and top and has no frame element; a frame sees its parent, the top-level window and its frame
     * element, and the parent sees its frames by index, by name and through the elements.
     * <p>
     * 최상위 문서는 자기가 부모이자 최상위이며 프레임 요소가 없다. 프레임은 부모, 최상위 window, 자기 프레임 요소를 보고, 부모는 인덱스, 이름, 요소로 프레임을 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testParentTopFramesAndFrameElement() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>window.marker='inner';sessionStorage.setItem('rel',[parent===top,top.document.title,window.frameElement.id,"
                    + "parent.document.body.id,window.name,parent.frames.length,window!==parent].join('|'))</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "<title>T</title></head><body id=\"b\">"
                    + "<iframe id=\"f\" name=\"inner\" width=\"50\" height=\"20\" srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<script>window.onload=function(){var f=document.getElementById('f');"
                    + "sessionStorage.setItem('top',[window.parent===window,window.top===window,window.frameElement===null,window.length,"
                    + "window.frames===window,window[0]===f.contentWindow,window.inner===window[0],frames[0].marker,"
                    + "f.contentDocument===f.contentWindow.document,f.contentWindow.parent===window,f.contentWindow.name].join('|'))}</script>"
                    + "</body></html>");
            FrameTestSupport.awaitStored(panel, "top", "true|true|true|1|true|true|true|inner|true|true|inner");
            FrameTestSupport.awaitStored(panel, "rel", "true|T|f|b|inner|1|true");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Same-origin windows are the real global objects: the parent calls functions of the frame and the frame changes the document of the
     * parent; a document of <code>srcdoc</code> and an initial <code>about:blank</code> frame inherit the origin of the creator and can be
     * changed from the parent, which shows in the picture.
     * <p>
     * 같은 오리진의 window 는 실제 전역 객체이다: 부모가 프레임의 함수를 부르고 프레임이 부모의 문서를 바꾼다. <code>srcdoc</code> 문서와 처음의
     * <code>about:blank</code> 프레임은 생성자의 오리진을 물려받아 부모가 바꿀 수 있으며 그것이 그림에 나타난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSameOriginWindowsAreUsableAndInheritTheOrigin() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>function hello(n){return 'hi '+n}function paint(){parent.document.getElementById('p').style.background='#0f0'}"
                    + "localStorage.setItem('x','1')</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<div id=\"p\" style=\"height:20px;background:#f00\"></div>"
                    + "<iframe id=\"f\" width=\"50\" height=\"20\" srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<iframe id=\"blank\" width=\"50\" height=\"20\"></iframe>"
                    + "<script>window.onload=function(){var f=document.getElementById('f');"
                    + "sessionStorage.setItem('call',f.contentWindow.hello('there'));f.contentWindow.paint();"
                    + "var d=document.getElementById('blank').contentDocument;d.body.style.background='#00f';"
                    + "sessionStorage.setItem('blank',d.URL+'|'+d.location.href)}</script></body></html>");
            FrameTestSupport.awaitStored(panel, "call", "hi there");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the frame changed the parent document");
            FrameTestSupport.awaitPixel(panel, 10, 50, 0x0000FF, "the parent changed the initial about:blank document of its frame");
            FrameTestSupport.awaitStored(panel, "blank", "about:blank|about:blank");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A window of another origin shows only the allowed members: <code>contentDocument</code> is null, the document and other members throw
     * <code>SecurityError</code>, <code>length</code> and <code>postMessage</code> work, <code>frameElement</code> is null and the frame sees
     * the parent only through the allowed members.
     * <p>
     * 다른 오리진의 window 는 허용된 멤버만 보인다: <code>contentDocument</code> 는 null 이고 문서와 그 밖의 멤버는 <code>SecurityError</code> 를 던지며
     * <code>length</code> 와 <code>postMessage</code> 는 동작하고 <code>frameElement</code> 는 null 이며 프레임은 허용된 멤버를 통해서만 부모를 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginWindowsAreRestricted() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/inner.html", TestHttpServer.Response.html("<body><script>"
                    + "function t(f){try{f();return 'ok'}catch(e){return e.name}}"
                    + "sessionStorage.setItem('inner',[window.frameElement===null,t(function(){return parent.document}),"
                    + "t(function(){return parent.location.href}),parent.length,parent===top,typeof parent.postMessage,"
                    + "t(function(){return top.foo})].join('|'));"
                    + "window.addEventListener('message',function(e){sessionStorage.setItem('got',e.data+'|'+e.origin)})"
                    + "</script></body>"));
            String page = "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe id=\"f\" src=\"http://127.0.0.1:PORT/inner.html\" width=\"50\" height=\"20\"></iframe>"
                    + "<script>function t(f){try{f();return 'ok'}catch(e){return e.name}}"
                    + "window.onload=function(){var f=document.getElementById('f');var w=f.contentWindow;"
                    + "sessionStorage.setItem('outer',[f.contentDocument===null,w!==null,t(function(){return w.document}),"
                    + "t(function(){return w.localStorage}),w.length,w.parent===window,w.top===window,w.self===w,w.closed,"
                    + "t(function(){w.postMessage('hello','*')}),t(function(){w.postMessage('x','not a url')}),typeof w.location,"
                    + "t(function(){return w.location.href})].join('|'))}</script></body></html>";
            server.start();
            // 프레임의 주소는 같은 서버지만 호스트 이름이 달라 오리진이 다르다(127.0.0.1 대 localhost).
            server.routeFixed("/page", TestHttpServer.Response.html(page.replace("PORT", String.valueOf(portOf(server)))));
            panel.setPage(server.url("/page"));
            FrameTestSupport.awaitStored(panel, "outer", "true|true|SecurityError|SecurityError|0|true|true|true|false|ok|SyntaxError|object|SecurityError");
            // 부모는 프레임 하나를 가지므로 parent.length 는 1 이고, parent 와 top 은 같은 프록시이다.
            FrameTestSupport.awaitStored(panel, "inner", "true|SecurityError|SecurityError|1|true|function|SecurityError");
            // 부모가 보낸 메시지는 프레임이 받으며 origin 은 보낸 문서(부모)의 오리진이다.
            String origin = server.url("/").substring(0, server.url("/").length() - 1);
            FrameTestSupport.awaitStored(panel, "got", "hello|" + origin);
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Returns the port of a started server.
     * <p>
     * 시작한 서버의 포트를 반환한다.
     *
     * @param server the server / 서버
     * @return the port / 포트
     */
    private static int portOf(TestHttpServer server) {
        String url = server.url("/");
        int colon = url.lastIndexOf(':');
        return Integer.parseInt(url.substring(colon + 1, url.indexOf('/', colon)));
    }

    /**
     * The names of frames are properties of the window and follow renames, and the window length follows frames that come and go.
     * <p>
     * 프레임의 이름은 window 의 프로퍼티이고 이름 변경을 따라가며, window 의 length 는 생기고 없어지는 프레임을 따라간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFrameNamesAndLengthFollowTheFrames() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe name=\"a\" width=\"10\" height=\"10\" srcdoc=\"" + attr("<script>setTimeout(function(){window.name='renamed'},100)</script>")
                    + "\"></iframe>"
                    + "<iframe name=\"b\" width=\"10\" height=\"10\"></iframe>"
                    + "<script>function w(){return [window.length,typeof window.a,typeof window.renamed,typeof window.b,typeof window[1],typeof window[2]].join('|')}"
                    + "window.onload=function(){sessionStorage.setItem('s1',w());"
                    + "setTimeout(function(){sessionStorage.setItem('s2',w());document.body.removeChild(document.getElementsByTagName('iframe')[1]);"
                    + "setTimeout(function(){sessionStorage.setItem('s3',w())},600)},600)}</script></body></html>");
            FrameTestSupport.awaitStored(panel, "s1", "2|object|undefined|object|object|undefined");
            FrameTestSupport.awaitStored(panel, "s2", "2|undefined|object|object|object|undefined");
            FrameTestSupport.awaitStored(panel, "s3", "1|undefined|object|undefined|undefined|undefined");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A frame that a script creates has no window while it is not part of the document, gets one when it is inserted, and loses it when it is
     * removed; the initial <code>about:blank</code> document of such a frame can be written with <code>document.open/write/close</code> through
     * <code>contentWindow</code>, which is the classic way to fill a frame, and the scripts that it contains run.
     * <p>
     * 스크립트가 만든 프레임은 문서의 일부가 아닌 동안 window 가 없고, 삽입되면 window 를 얻고, 제거되면 잃는다. 이런 프레임의 처음의
     * <code>about:blank</code> 문서는 <code>contentWindow</code> 로 <code>document.open/write/close</code> 하여 채울 수 있고(프레임을 채우는 고전적인 방법),
     * 거기에 든 스크립트는 실행된다. 페이지가 확정되기 전에 실행되는 스크립트(파싱 중, window load)는 프레임의 문서를 소유한 문서 스레드에서 실행되지
     * 않으므로 타이머로 확정 뒤에 실행한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptCreatedFrameCanBeWrittenThroughItsWindow() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<script>window.onload=function(){setTimeout(function(){var f=document.createElement('iframe');f.width=50;f.height=20;"
                    + "var before=[f.contentWindow===null,f.contentDocument===null];document.body.appendChild(f);"
                    + "var w=f.contentWindow;var d=w.document;d.open();"
                    + "d.write('<body style=\"margin:0;background:#00f\"><script>parent.sessionStorage.setItem(\"inner\",\"ran\")<\\/script>');d.close();"
                    + "sessionStorage.setItem('create',before.join('|')+'|'+[w!==null,w.parent===window,w.length,d.body!==null,f.contentDocument===d].join('|'));"
                    + "document.body.removeChild(f);"
                    + "sessionStorage.setItem('removed',[f.contentWindow===null,f.contentDocument===null].join('|'))},0)}</script></body></html>");
            FrameTestSupport.awaitStored(panel, "create", "true|true|true|true|0|true|true");
            FrameTestSupport.awaitStored(panel, "inner", "ran");
            FrameTestSupport.awaitStored(panel, "removed", "true|true");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A message that a frame of the same origin posts to its parent arrives with the origin of the frame, and a message of the parent reaches
     * the frame.
     * <p>
     * 같은 오리진 프레임이 부모에게 보낸 메시지는 프레임의 오리진과 함께 도착하고, 부모의 메시지는 프레임에 도착한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMessagesCrossTheFrames() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>window.addEventListener('message',function(e){sessionStorage.setItem('inner',e.data)});"
                    + "window.onload=function(){parent.postMessage('up','*')}</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe id=\"f\" width=\"10\" height=\"10\" srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<script>window.addEventListener('message',function(e){sessionStorage.setItem('outer',e.data+'|'+e.origin);"
                    + "document.getElementById('f').contentWindow.postMessage('down','*')})</script></body></html>");
            FrameTestSupport.awaitStored(panel, "inner", "down");
            String origin = server.url("/").substring(0, server.url("/").length() - 1);
            FrameTestSupport.awaitStored(panel, "outer", "up|" + origin);
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A shared buffer is received by a same-origin frame, while a different origin receives messageerror.
     * <p>
     * 같은 오리진 프레임은 공유 버퍼를 받고 다른 오리진은 messageerror를 받는다.
     *
     * @throws Exception on test infrastructure failure / 시험 환경이 실패하면
     */
    public void testSharedMemoryMessageAcrossFrameOrigins() throws Exception {
        TestHttpServer outer = new TestHttpServer();
        TestHttpServer cross = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            cross.routeFixed("/cross", TestHttpServer.Response.html(
                    "<!doctype html><script>onmessageerror=e=>parent.postMessage(['denied',e.type,e.data===null,e.origin,e.ports.length],'*');"
                    + "onmessage=e=>parent.postMessage(['crossPlain',typeof e.data==='string'?e.data:'unexpected shared buffer'],'*');"
                    + "onload=()=>parent.postMessage(['crossReady'],'*');</script>"));
            cross.start();
            String inner = "<script>onmessage=e=>{const v=new Int32Array(e.data);"
                    + "Atomics.store(v,0,7);parent.postMessage(['sameDone',e.data instanceof SharedArrayBuffer],'*');};"
                    + "onload=()=>parent.postMessage(['sameReady'],'*');</script>";
            serve(panel, outer, "<!doctype html><script>const b=new SharedArrayBuffer(4), v=new Int32Array(b);"
                    + "onmessage=e=>{const d=e.data;"
                    + "if(d[0]==='sameReady')e.source.postMessage(b,'*');"
                    + "else if(d[0]==='crossReady'){e.source.postMessage({nested:b},'*');e.source.postMessage('alive','*');}"
                    + "else if(d[0]==='sameDone')sessionStorage.setItem('sameDone',[d[1],v[0]].join('|'));"
                    + "else if(d[0]==='denied')sessionStorage.setItem('denied',d.slice(1).join('|'));"
                    + "else if(d[0]==='crossPlain')sessionStorage.setItem('crossPlain',d[1]);};</script>"
                    + "<iframe srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<iframe src=\"" + cross.url("/cross") + "\"></iframe>");
            FrameTestSupport.awaitStored(panel, "sameDone", "true|7");
            String origin = outer.url("/page");
            FrameTestSupport.awaitStored(panel, "denied", "messageerror|true|" + origin.substring(0, origin.length() - 5) + "|0");
            FrameTestSupport.awaitStored(panel, "crossPlain", "alive");
        } finally {
            panel.dispose();
            cross.stop();
            outer.stop();
        }
    }
}
