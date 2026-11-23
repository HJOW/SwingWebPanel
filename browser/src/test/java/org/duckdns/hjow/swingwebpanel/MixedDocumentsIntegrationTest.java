package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Mixed documents run through the real component (plan item P20-11): a modern HTML5 page with inline SVG and MathML, a legacy table with
 * presentational attributes, a form, an <code>iframe</code> with <code>srcdoc</code>, plug-in elements with fallback content and a
 * <code>video</code>; a page in quirks mode with the obsolete elements and attributes; XHTML with SVG; the document modes of the old DOCTYPEs;
 * a <code>frameset</code>; and the plug-in elements <code>applet</code> and <code>embed</code>. Every page runs a script that stores what the DOM
 * reports in <code>sessionStorage</code>, and the test also checks pixels that the component paints and that the source returned by
 * <code>getContent()</code> is the markup that was set.
 * <p>
 * 실제 컴포넌트로 실행하는 혼합 문서(계획 P20-11): 인라인 SVG 와 MathML 이 있는 현대 HTML5 페이지, 표현 속성이 있는 옛 표, 폼, <code>srcdoc</code> 가 있는
 * <code>iframe</code>, 대체 내용이 있는 플러그인 요소, <code>video</code>; 폐기된 요소와 속성이 있는 호환 모드 페이지; SVG 가 든 XHTML; 옛 DOCTYPE 의
 * 문서 모드; <code>frameset</code>; 플러그인 요소 <code>applet</code> 과 <code>embed</code>. 모든 페이지는 DOM 이 알려 주는 것을 <code>sessionStorage</code> 에
 * 저장하는 스크립트를 실행하고, 시험은 컴포넌트가 그린 화소와 <code>getContent()</code> 가 돌려주는 원문이 설정한 마크업인지도 확인한다.
 */
public class MixedDocumentsIntegrationTest {

    /**
     * The component of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private SwingWebPanel panel;

    /**
     * The test server for the frameset.
     * <p>
     * frameset 용 시험 서버.
     */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MixedDocumentsIntegrationTest() {
    }

    /**
     * Starts the component and the server.
     * <p>
     * 컴포넌트와 서버를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/frames", TestHttpServer.Response.html("<!doctype html><html><head><title>fs</title></head>"
                + "<frameset cols=\"50%,50%\" onload=\"sessionStorage.setItem('fs', window.length + ':' + frames[0].document.body.textContent + ':' "
                + "+ frames[1].document.body.textContent + ':' + document.getElementsByTagName('frame').length)\">"
                + "<frame src=\"/left\" name=\"l\"><frame src=\"/right\" name=\"r\"><noframes><body>no frames here</body></noframes></frameset></html>"));
        server.routeFixed("/left", TestHttpServer.Response.html("<body style=\"margin:0;background:#ff0000\">left</body>"));
        server.routeFixed("/right", TestHttpServer.Response.html("<body style=\"margin:0;background:#0000ff\">right</body>"));
        server.start();
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component on the EDT.
             * <p>
             * EDT 에서 컴포넌트를 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(400, 300);
            }
        });
        panel = holder[0];
    }

    /**
     * Disposes the component and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        panel.dispose();
        server.stop();
    }

    /**
     * Serves a page of the given content type from the test server and opens it in the component (pages with a web address have an origin, so
     * the script may use <code>sessionStorage</code>).
     * <p>
     * 주어진 콘텐츠 종류의 페이지를 시험 서버에서 내려주고 컴포넌트에서 연다(웹 주소가 있는 페이지는 오리진이 있으므로 스크립트가
     * <code>sessionStorage</code> 를 쓸 수 있다).
     *
     * @param path the path / 경로
     * @param type the content type / 콘텐츠 종류
     * @param body the source / 원문
     */
    private void open(String path, String type, String body) {
        server.routeFixed(path, TestHttpServer.Response.ok(type, body));
        panel.setPage(server.url(path));
    }

    /**
     * Returns a value of the session storage by key, or null.
     * <p>
     * 세션 저장소에서 키로 값을 찾는다. 없으면 null.
     *
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private String session(String key) {
        Collection<StorageEntry> entries = panel.getSessionStorage();
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : entries) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until the script of the page stored a value, and returns it.
     * <p>
     * 페이지의 스크립트가 값을 저장할 때까지 기다리고 그 값을 반환한다.
     *
     * @param key the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private String await(String key) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 기다린다.
        while (session(key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.notNull(session(key), "the script stored " + key);
        return session(key);
    }

    /**
     * Paints the component on the EDT and waits until a pixel has a color.
     * <p>
     * 컴포넌트를 EDT 에서 그리고 화소가 어떤 색이 될 때까지 기다린다.
     *
     * @param x       x / x
     * @param y       y / y
     * @param rgb     the expected color / 기대한 색
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitPixel(int x, int y, int rgb, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int actual = 0;
        // 문서 작업과 EDT 게시가 끝날 때까지 짧게 쉬며 그려진 화소를 확인한다.
        do {
            final BufferedImage[] image = new BufferedImage[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Paints into an image without a native window.
                 * <p>
                 * 네이티브 창 없이 이미지에 그린다.
                 */
                public void run() {
                    image[0] = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = image[0].createGraphics();
                    try {
                        panel.paint(g);
                    } finally {
                        g.dispose();
                    }
                }
            });
            actual = image[0].getRGB(x, y) & 0xFFFFFF;
            if (actual == rgb) {
                return;
            }
            Thread.sleep(15);
        } while (System.currentTimeMillis() < end);
        Check.equal(Integer.toHexString(rgb), Integer.toHexString(actual), message);
    }

    /**
     * The script of every page: it stores the values that the page reports and, last, <code>done</code>.
     * <p>
     * 모든 페이지의 스크립트: 페이지가 알려 주는 값을 저장하고 마지막에 <code>done</code> 을 저장한다.
     */
    private static final String REPORT = "<script>window.onerror = function (m) { sessionStorage.setItem('error', String(m)); };"
            + "function put(k, v) { sessionStorage.setItem(k, String(v)); }</script>";

    /**
     * A modern HTML5 page with inline SVG, MathML, a legacy table, a form, an iframe, plug-in elements and a video: the script sees the right
     * interfaces, the component paints the SVG with the pixels of its shapes, and the markup is returned unchanged.
     * <p>
     * 인라인 SVG, MathML, 옛 표, 폼, iframe, 플러그인 요소, 비디오가 있는 현대 HTML5 페이지: 스크립트가 올바른 인터페이스를 보고, 컴포넌트가 SVG 를 도형의
     * 화소로 그리며, 마크업이 그대로 반환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testModernPageWithSvgAndMathMl() throws Exception {
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>mix</title><style>body{margin:0}"
                + "#box{position:absolute;left:0;top:0;width:60px;height:30px;background:#00ff00}</style></head><body>"
                + "<svg id=\"s\" width=\"100\" height=\"60\" style=\"position:absolute;left:100px;top:0\">"
                + "<rect width=\"100\" height=\"60\" fill=\"#ff0000\"></rect><circle cx=\"50\" cy=\"30\" r=\"10\" fill=\"#0000ff\"></circle></svg>"
                + "<math id=\"m\" style=\"position:absolute;left:0;top:100px\"><mi>x</mi><mo>+</mo><mn>1</mn></math><div id=\"box\"></div>"
                + "<table id=\"t\" border=\"1\" cellpadding=\"4\" bgcolor=\"#ffff00\" style=\"position:absolute;left:0;top:140px\"><tr><td>a</td><td>b</td></tr></table>"
                + "<form id=\"f\" action=\"/x\"><input name=\"q\" value=\"1\"><select name=\"s\"><option>o</option></select></form>"
                + "<iframe id=\"fr\" srcdoc=\"&lt;p id=in&gt;inner&lt;/p&gt;\" style=\"position:absolute;left:200px;top:100px;width:100px;height:40px;border:0\"></iframe>"
                + "<object id=\"ob\" data=\"/missing.swf\" style=\"position:absolute;left:0;top:200px\"><span id=\"fb\">fallback text</span></object>"
                + "<video id=\"vd\" width=\"80\" height=\"40\" style=\"position:absolute;left:200px;top:200px\"></video>"
                + REPORT + "<script>window.onload = function () { var s = document.getElementById('s'), m = document.getElementById('m'), t = document.getElementById('t'),"
                + " f = document.getElementById('f'), fr = document.getElementById('fr');"
                + "put('compat', document.compatMode); put('svg', [s.namespaceURI, s.localName, Object.prototype.toString.call(s), s.querySelectorAll('circle').length, "
                + "s.querySelector('rect').namespaceURI].join('|'));"
                + "put('math', [m.namespaceURI, m.textContent, m.firstElementChild.localName].join('|'));"
                + "put('table', [t.rows.length, t.rows[0].cells.length, t.border, t.bgColor, t.cellPadding, getComputedStyle(t).backgroundColor].join('|'));"
                + "put('form', [f.elements.length, f.elements.q.value, f.elements.s.options.length].join('|'));"
                + "put('frame', fr.contentDocument.getElementById('in').textContent);"
                + "put('fallback', document.getElementById('fb').getBoundingClientRect().width > 0);"
                + "put('video', [document.getElementById('vd').getBoundingClientRect().width, document.getElementById('vd').getBoundingClientRect().height].join('x'));"
                + "put('done', '1'); };</script></body></html>";
        open("/modern", "text/html; charset=UTF-8", html);
        await("done");
        Check.isNull(session("error"), "the page raised an error: " + session("error"));
        Check.equal("CSS1Compat", session("compat"), "the HTML5 doctype gives standards mode");
        Check.equal("http://www.w3.org/2000/svg|svg|[object SVGElement]|1|http://www.w3.org/2000/svg", session("svg"), "inline SVG elements are in the SVG namespace");
        Check.equal("http://www.w3.org/1998/Math/MathML|x+1|mi", session("math"), "inline MathML elements are in the MathML namespace");
        Check.equal("1|2|1|#ffff00|4|rgb(255, 255, 0)", session("table"), "the legacy table attributes reach the DOM and the style");
        Check.equal("2|1|1", session("form"), "the form elements");
        Check.equal("inner", session("frame"), "the srcdoc document of the iframe");
        Check.equal("true", session("fallback"), "the fallback content of an object without a usable resource is laid out");
        Check.equal("80x40", session("video"), "the video keeps its place and size");
        Check.equal(html, panel.getContent(), "getContent() returns the markup that was set");
        awaitPixel(10, 10, 0x00FF00, "the green box");
        awaitPixel(110, 10, 0xFF0000, "the red rectangle of the inline SVG");
        awaitPixel(150, 30, 0x0000FF, "the blue circle of the inline SVG");
    }

    /**
     * A page in quirks mode (no DOCTYPE) with the obsolete elements and attributes: body colours, <code>center</code>, <code>font</code>, a table
     * of full width and an image that cannot be loaded (its alt text is shown).
     * <p>
     * 호환 모드(DOCTYPE 없음)의 페이지와 폐기된 요소, 속성: body 색, <code>center</code>, <code>font</code>, 전체 너비의 표, 읽을 수 없는 이미지(alt 텍스트를 보임).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQuirksPageWithObsoleteElements() throws Exception {
        String html = "<html><head><title>old</title></head><body bgcolor=\"#0000ff\" text=\"#ffffff\" link=\"#00ff00\">"
                + "<center id=\"c\"><font id=\"fo\" color=\"red\" size=\"5\">hello</font></center>"
                + "<table id=\"t\" width=\"100%\"><tr><td id=\"td\" align=\"right\">x</td></tr></table>"
                + "<img id=\"im\" src=\"/none.png\" alt=\"gone\" width=\"40\" height=\"20\">"
                + REPORT + "<script>window.onload = function () { var fo = document.getElementById('fo'), td = document.getElementById('td');"
                + "put('compat', document.compatMode);"
                + "put('body', [document.body.bgColor, document.fgColor, document.linkColor, getComputedStyle(document.body).backgroundColor].join('|'));"
                + "put('font', [getComputedStyle(fo).color, getComputedStyle(fo).fontSize].join('|'));"
                + "put('center', getComputedStyle(document.getElementById('c')).textAlign);"
                + "put('cell', getComputedStyle(td).textAlign);"
                + "put('width', document.getElementById('t').getBoundingClientRect().width);"
                + "put('done', '1'); };</script></body></html>";
        open("/quirks", "text/html; charset=UTF-8", html);
        await("done");
        Check.isNull(session("error"), "the page raised an error: " + session("error"));
        Check.equal("BackCompat", session("compat"), "a page without DOCTYPE is in quirks mode");
        Check.equal("#0000ff|#ffffff|#00ff00|rgb(0, 0, 255)", session("body"), "the body colour attributes and the colour attributes of the document");
        Check.equal("rgb(255, 0, 0)|24px", session("font"), "font color and size 5");
        Check.equal("center", session("center"), "center aligns its text");
        Check.equal("right", session("cell"), "the align attribute of a cell");
        Check.equal("384", session("width"), "a table of width 100 percent fills the viewport inside the 8 pixel margins of the body");
        Check.equal(html, panel.getContent(), "getContent() returns the markup that was set");
        awaitPixel(390, 290, 0x0000FF, "the bgcolor of the body paints the canvas");
    }

    /**
     * XHTML with an SVG: the namespaces, the content type and the painted shape.
     * <p>
     * SVG 가 있는 XHTML: 네임스페이스, 콘텐츠 종류, 그려진 도형.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testXhtmlWithSvg() throws Exception {
        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>x</title>"
                + "<script type=\"text/javascript\"><![CDATA[window.onload = function () { sessionStorage.setItem('xhtml', [document.contentType, "
                + "document.documentElement.namespaceURI, document.documentElement.localName, document.getElementsByTagName('svg')[0].namespaceURI, "
                + "document.compatMode].join('|')); sessionStorage.setItem('done', '1'); };]]></script></head>"
                + "<body style=\"margin:0\"><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"40\" height=\"40\"><rect width=\"40\" height=\"40\" fill=\"#00ff00\"/></svg></body></html>";
        open("/xhtml", "application/xhtml+xml; charset=UTF-8", xhtml);
        await("done");
        Check.equal("application/xhtml+xml|http://www.w3.org/1999/xhtml|html|http://www.w3.org/2000/svg|CSS1Compat", session("xhtml"),
                "the XHTML document and its SVG");
        Check.equal(xhtml, panel.getContent(), "getContent() returns the XHTML source");
        awaitPixel(10, 10, 0x00FF00, "the SVG rectangle in the XHTML document");
    }

    /**
     * The document modes of the old DOCTYPEs: HTML 2.0 and 3.2 and the transitional types without a system identifier are quirks, the
     * transitional types with a system identifier are limited quirks (standards for the script), the strict types are standards.
     * <p>
     * 옛 DOCTYPE 의 문서 모드: HTML 2.0, 3.2 와 시스템 식별자 없는 transitional 은 호환, 시스템 식별자가 있는 transitional 은 제한 호환(스크립트에는 표준), strict 는
     * 표준이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOldDoctypeModes() throws Exception {
        String[][] cases = {
            { "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">", "BackCompat" },
            { "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">", "BackCompat" },
            { "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">", "BackCompat" },
            { "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">", "CSS1Compat" },
            { "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">", "CSS1Compat" },
            { "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", "CSS1Compat" },
            { "<!DOCTYPE html>", "CSS1Compat" },
            { "", "BackCompat" } };
        // 종류마다 페이지를 새로 설정하고 스크립트가 보고하는 compatMode 를 확인한다.
        for (int i = 0; i < cases.length; i++) {
            open("/mode" + i, "text/html; charset=UTF-8", cases[i][0] + "<html><body><script>sessionStorage.setItem('mode" + i + "', document.compatMode);</script></body></html>");
            Check.equal(cases[i][1], await("mode" + i), "the document mode of " + cases[i][0]);
        }
    }

    /**
     * A frameset with two frames and <code>noframes</code>: both documents load into their own browsing contexts, the script of the frameset
     * reads them, the pixels of both are painted and the <code>noframes</code> content is not shown.
     * <p>
     * 프레임 둘과 <code>noframes</code> 가 있는 frameset: 두 문서가 각자의 브라우징 문맥에 읽히고, frameset 의 스크립트가 그것을 읽으며, 둘의 화소가 그려지고
     * <code>noframes</code> 내용은 보이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFrameset() throws Exception {
        panel.setPage(server.url("/frames"));
        Check.equal("2:left:right:2", await("fs"), "the frames of the frameset and their documents");
        awaitPixel(20, 150, 0xFF0000, "the left frame");
        awaitPixel(380, 150, 0x0000FF, "the right frame");
    }

    /**
     * The plug-in elements <code>applet</code> and <code>embed</code> do not run anything: the fallback content of the applet is laid out
     * and the embed keeps its size.
     * <p>
     * 플러그인 요소 <code>applet</code> 과 <code>embed</code> 는 아무것도 실행하지 않는다: applet 의 대체 내용은 배치되고 embed 는 크기를 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPluginElementsShowFallback() throws Exception {
        String html = "<!doctype html><body style=\"margin:0\"><applet id=\"ap\" code=\"X.class\" width=\"100\" height=\"30\"><span id=\"af\">applet fallback</span></applet>"
                + "<embed id=\"em\" src=\"/x.swf\" type=\"application/x-shockwave-flash\" width=\"50\" height=\"20\" style=\"position:absolute;left:0;top:100px\">"
                + "<object id=\"ob\" data=\"/x.bin\" style=\"position:absolute;left:0;top:140px;width:50px;height:20px\"><param name=\"p\" value=\"1\"></object>"
                + REPORT + "<script>window.onload = function () { put('applet', document.getElementById('af').getBoundingClientRect().width > 0); "
                + "var r = document.getElementById('em').getBoundingClientRect(); put('embed', r.width + 'x' + r.height); put('done', '1'); };</script></body>";
        open("/plugins", "text/html; charset=UTF-8", html);
        await("done");
        Check.isNull(session("error"), "the page raised an error: " + session("error"));
        Check.equal("true", session("applet"), "the fallback content of the applet");
        Check.equal("50x20", session("embed"), "the embed keeps its size");
        awaitPixel(25, 102, 0xF4F4F4, "the embed shows the notice of an unsupported plug-in");
        awaitPixel(25, 142, 0xF4F4F4, "an object without fallback content shows the notice of an unsupported plug-in");
    }
}
