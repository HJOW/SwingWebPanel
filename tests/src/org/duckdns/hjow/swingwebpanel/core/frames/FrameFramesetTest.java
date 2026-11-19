package org.duckdns.hjow.swingwebpanel.core.frames;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The old <code>frameset</code>, <code>frame</code> and <code>noframes</code> elements through the real component (plan item P20-02, stage f;
 * HTML5 REC 2014-10-28 10.6 and HTML 4.01 16.2): the lists in <code>rows</code> and <code>cols</code> (pixels, percentages and relative
 * lengths) cut the viewport into the cells of the frames, nested framesets recurse, the border between the frames has the thickness and
 * color of the attributes, named frames receive link targets, the windows and elements of the frames work like those of
 * <code>iframe</code>, the margins come from the attributes of the container frame, and the event handler attributes of the frameset belong
 * to the window.
 * <p>
 * 실제 컴포넌트로 확인하는 옛 <code>frameset</code>, <code>frame</code>, <code>noframes</code> 요소(계획 P20-02, 단계 f. HTML5 REC 2014-10-28 10.6,
 * HTML 4.01 16.2): <code>rows</code>, <code>cols</code> 의 목록(픽셀, 백분율, 상대 길이)이 뷰포트를 프레임의 칸으로 나누고, 중첩된 frameset 은 재귀하며,
 * 프레임 사이 경계선은 속성의 두께와 색을 가지고, 이름 있는 프레임은 링크 대상을 받고, 프레임의 window 와 요소는 <code>iframe</code> 과 같게 동작하고,
 * 여백은 컨테이너 프레임의 속성에서 오고, frameset 의 이벤트 핸들러 속성은 window 의 것이다.
 */
public class FrameFramesetTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FrameFramesetTest() {
    }

    /**
     * Routes the three documents that the tests show in frames: red, green and blue pages without margins.
     * <p>
     * 시험이 프레임에 보여 주는 세 문서를 등록한다: 여백이 없는 빨강, 초록, 파랑 페이지.
     *
     * @param server the server / 서버
     */
    private static void routeColors(TestHttpServer server) {
        server.routeFixed("/a", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"></body>"));
        server.routeFixed("/b", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
        server.routeFixed("/c", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\"></body>"));
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
     * <code>cols</code> and <code>rows</code> cut the viewport into cells: a percentage and a relative length share the width, an absolute
     * row and a relative row share the height of a nested frameset, and the frames fill their cells exactly.
     * <p>
     * <code>cols</code> 와 <code>rows</code> 는 뷰포트를 칸으로 나눈다: 백분율과 상대 길이가 너비를 나누고, 중첩된 frameset 의 절대 길이 행과 상대 길이 행이
     * 높이를 나누며, 프레임은 칸을 정확히 채운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testColsRowsAndNestedFramesetsCutTheViewport() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            routeColors(server);
            serve(panel, server, "<html><head><title>fs</title></head><frameset cols=\"30%,*\" frameborder=\"0\">"
                    + "<frame src=\"/a\" name=\"a\"><frameset rows=\"100,*\"><frame src=\"/b\"><frame src=\"/c\"></frameset></frameset></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the left frame (30%)");
            FrameTestSupport.awaitPixel(panel, 200, 50, 0x00FF00, "the upper right frame (100 pixels high)");
            FrameTestSupport.awaitPixel(panel, 200, 200, 0x0000FF, "the lower right frame (the rest)");
            FrameTestSupport.awaitPixel(panel, 119, 10, 0xFF0000, "the left frame ends at 120");
            FrameTestSupport.awaitPixel(panel, 121, 10, 0x00FF00, "the right frames start at 120");
            FrameTestSupport.awaitPixel(panel, 200, 99, 0x00FF00, "the upper right frame ends at 100");
            FrameTestSupport.awaitPixel(panel, 200, 101, 0x0000FF, "the lower right frame starts at 100");
            FrameTestSupport.awaitPixel(panel, 399, 299, 0x0000FF, "the frames reach the corner");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The border between frames takes the thickness of <code>border</code> and the color of <code>bordercolor</code>, defaults to six
     * pixels of gray, and is absent with <code>frameborder="0"</code>, which a nested frameset inherits from its parent.
     * <p>
     * 프레임 사이 경계선은 <code>border</code> 의 두께와 <code>bordercolor</code> 의 색을 가지고, 기본은 회색 6 픽셀이며, <code>frameborder="0"</code>
     * 이면 없다. 중첩된 frameset 은 이를 부모에게서 물려받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBorderBetweenFrames() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            routeColors(server);
            serve(panel, server, "<html><frameset cols=\"50%,50%\"><frame src=\"/a\"><frame src=\"/b\"></frameset></html>");
            // 칸은 200 씩이고 프레임마다 안쪽에 3 픽셀 경계선이 있으므로 197~203 이 회색이다.
            FrameTestSupport.awaitPixel(panel, 100, 100, 0xFF0000, "the left frame");
            FrameTestSupport.awaitPixel(panel, 300, 100, 0x00FF00, "the right frame");
            FrameTestSupport.awaitPixel(panel, 200, 100, 0x808080, "the default border is gray");
            FrameTestSupport.awaitPixel(panel, 196, 100, 0xFF0000, "the left frame ends where its border starts (197)");
            FrameTestSupport.awaitPixel(panel, 204, 100, 0x00FF00, "the right frame starts where its border ends (203)");
        } finally {
            panel.dispose();
            server.stop();
        }
        server = new TestHttpServer();
        panel = FrameTestSupport.newPanel(400, 300);
        try {
            routeColors(server);
            serve(panel, server, "<html><frameset rows=\"50%,50%\" border=\"20\" bordercolor=\"#ff0\" frameborder=\"1\">"
                    + "<frame src=\"/a\"><frameset cols=\"*,*\" frameborder=\"no\"><frame src=\"/b\"><frame src=\"/c\"></frameset></frameset></html>");
            // 행은 150 씩이고 프레임 경계선은 두께의 절반(10)씩이므로 위 프레임의 아래쪽 10 픽셀(140~150)과 바깥쪽 10 픽셀이 노란색이다.
            // 안쪽 frameset 은 frameborder 가 no 여서 아래 프레임에는 경계선이 없다.
            FrameTestSupport.awaitPixel(panel, 100, 50, 0xFF0000, "the upper frame");
            FrameTestSupport.awaitPixel(panel, 100, 145, 0xFFFF00, "the border has the color of the attribute");
            FrameTestSupport.awaitPixel(panel, 100, 5, 0xFFFF00, "the outer border of a frameset that has a border");
            FrameTestSupport.awaitPixel(panel, 100, 135, 0xFF0000, "the border is half of the thickness");
            FrameTestSupport.awaitPixel(panel, 100, 155, 0x00FF00, "no border around the frames of the nested frameset");
            FrameTestSupport.awaitPixel(panel, 100, 200, 0x00FF00, "the lower left frame");
            FrameTestSupport.awaitPixel(panel, 300, 200, 0x0000FF, "the lower right frame");
            FrameTestSupport.awaitPixel(panel, 199, 200, 0x00FF00, "no border inside the nested frameset (left)");
            FrameTestSupport.awaitPixel(panel, 201, 200, 0x0000FF, "no border inside the nested frameset (right)");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Frames are named and addressable like those of <code>iframe</code>: a link with a target in a navigation frame loads another frame, the
     * windows see each other through <code>parent</code>, <code>top</code> and <code>frames</code>, <code>document.body</code> of a frameset
     * document is the frameset, the <code>frame</code> elements have <code>contentWindow</code>, and the <code>onload</code> attribute of the
     * frameset runs when the frames have loaded.
     * <p>
     * 프레임은 <code>iframe</code> 처럼 이름이 있고 접근할 수 있다: 탐색 프레임의 대상이 있는 링크는 다른 프레임을 로드하고, window 는 <code>parent</code>,
     * <code>top</code>, <code>frames</code> 로 서로를 보고, frameset 문서의 <code>document.body</code> 는 frameset 이고, <code>frame</code> 요소에는
     * <code>contentWindow</code> 가 있고, frameset 의 <code>onload</code> 속성은 프레임이 로드된 뒤 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNamedFramesLinksAndWindows() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            routeColors(server);
            server.routeFixed("/nav", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"><a href=\"/c\" target=\"main\" "
                    + "style=\"display:block;height:60px;background:#ff0\">go</a><script>sessionStorage.setItem('nav',[parent===top,"
                    + "parent.frames.length,parent.frames[0]===window,parent.frames['main']===parent.frames[1],window.name,parent.document.body.tagName].join('|'))"
                    + "</script></body>"));
            serve(panel, server, "<html><frameset cols=\"100,*\" frameborder=\"0\" onload=\"sessionStorage.setItem('onload',[window.length,document.body.tagName,"
                    + "document.getElementsByTagName('frame')[1].contentWindow===frames[1],frames.main.name].join('|'))\">"
                    + "<frame src=\"/nav\" name=\"nav\"><frame src=\"/b\" name=\"main\"></frameset></html>");
            FrameTestSupport.awaitStored(panel, "nav", "true|2|true|true|nav|FRAMESET");
            FrameTestSupport.awaitStored(panel, "onload", "2|FRAMESET|true|main");
            FrameTestSupport.awaitPixel(panel, 200, 100, 0x00FF00, "the main frame shows the green document");
            // 링크(노란 칸)를 누르면 main 이라는 이름의 프레임이 /c 로 이동한다.
            FrameTestSupport.click(panel, 50, 20);
            FrameTestSupport.awaitPixel(panel, 200, 100, 0x0000FF, "the target of the link is the frame named main");
            FrameTestSupport.awaitPixel(panel, 50, 200, 0xFF0000, "the navigation frame stays");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(2, panel.getHistories().size(), "the history of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Missing frames leave the border color in their cell, frames beyond the cells are not shown, and the content of
     * <code>noframes</code> is not displayed.
     * <p>
     * 없는 프레임은 칸에 경계선 색을 남기고, 칸을 넘는 프레임은 보이지 않으며, <code>noframes</code> 의 내용은 표시되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMissingExtraAndNoframes() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            routeColors(server);
            serve(panel, server, "<html><frameset cols=\"50%,50%\" frameborder=\"0\"><frame src=\"/a\"></frameset>"
                    + "<noframes><body><p style=\"background:#00f\">no frames</p></body></noframes></html>");
            FrameTestSupport.awaitPixel(panel, 100, 100, 0xFF0000, "the only frame fills its cell");
            FrameTestSupport.awaitPixel(panel, 300, 100, 0x808080, "the empty cell shows the border color");
        } finally {
            panel.dispose();
            server.stop();
        }
        server = new TestHttpServer();
        panel = FrameTestSupport.newPanel(400, 300);
        try {
            routeColors(server);
            serve(panel, server, "<html><frameset cols=\"100%\" frameborder=\"0\"><frame src=\"/a\"><frame src=\"/b\"></frameset></html>");
            FrameTestSupport.awaitPixel(panel, 200, 100, 0xFF0000, "the first frame fills the only cell");
            FrameTestSupport.awaitPixel(panel, 399, 299, 0xFF0000, "the second frame has no cell");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The margins of the document in a frame come from the <code>marginwidth</code> and <code>marginheight</code> of its container frame
     * element, <code>frame</code> and <code>iframe</code> alike, when the body has none (HTML5 REC 2014-10-28 10.3.2).
     * <p>
     * 본문에 속성이 없으면 프레임 안 문서의 여백은 컨테이너 프레임 요소의 <code>marginwidth</code>, <code>marginheight</code> 에서 온다. <code>frame</code> 과
     * <code>iframe</code> 모두 마찬가지이다(HTML5 REC 2014-10-28 10.3.2).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMarginsComeFromTheContainerFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/m", TestHttpServer.Response.html("<body><div style=\"background:#f00;height:10px;width:10px\"></div></body>"));
            serve(panel, server, "<html><frameset cols=\"200,200\" frameborder=\"0\"><frame src=\"/m\" marginwidth=\"30\" marginheight=\"20\">"
                    + "<frame src=\"/m\"></frameset></html>");
            // 첫 프레임: 여백 30x20 이므로 빨간 칸은 (30,20)~(40,30). 둘째 프레임은 기본 여백 8 이므로 (208,8)~(218,18).
            FrameTestSupport.awaitPixel(panel, 35, 25, 0xFF0000, "the frame margins 30 and 20");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFFFFFF, "inside the margin of the first frame");
            FrameTestSupport.awaitPixel(panel, 213, 13, 0xFF0000, "the default margin of 8 in the second frame");
        } finally {
            panel.dispose();
            server.stop();
        }
        server = new TestHttpServer();
        panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/m", TestHttpServer.Response.html("<body><div style=\"background:#f00;height:10px;width:10px\"></div></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head><style>html,body{margin:0}iframe{border:0;display:block}</style></head><body>"
                    + "<iframe src=\"/m\" marginwidth=\"30\" marginheight=\"20\" width=\"100\" height=\"80\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 35, 25, 0xFF0000, "the iframe margins 30 and 20");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFFFFFF, "inside the margin of the iframe");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
