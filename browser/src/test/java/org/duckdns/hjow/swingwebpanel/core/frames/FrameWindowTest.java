package org.duckdns.hjow.swingwebpanel.core.frames;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
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
}
