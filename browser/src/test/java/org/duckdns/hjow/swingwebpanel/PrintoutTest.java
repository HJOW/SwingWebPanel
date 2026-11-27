package org.duckdns.hjow.swingwebpanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Printing of a component (API contract 5.30, cases C-PRINT-1 to C-PRINT-6): where the pages are divided, the print medium and the
 * <code>@page</code> rules, printing with and without backgrounds, the print events and <code>window.print()</code>, what stays as it
 * is on the screen, and the rules of the methods. No printer is used: the pages are painted into images only.
 * <p>
 * 컴포넌트의 인쇄(API 계약 5.30, 사례 C-PRINT-1 ~ C-PRINT-6): 쪽을 나누는 곳, 인쇄 매체와 <code>@page</code> 규칙, 배경을 넣거나 뺀 인쇄, 인쇄
 * 이벤트와 <code>window.print()</code>, 화면에서 그대로인 것, 메서드의 규칙. 프린터는 쓰지 않는다: 쪽은 이미지에만 그린다.
 */
public class PrintoutTest {

    /** The component of the case. / 사례의 컴포넌트. */
    private SwingWebPanel panel;

    /** How often <code>printRequested()</code> was called, and whether on the event dispatch thread. / <code>printRequested()</code> 가 불린 횟수와 이벤트 디스패치 스레드 여부. */
    private final List<String> requests = new ArrayList<String>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PrintoutTest() {
    }

    /**
     * Creates the component.
     * <p>
     * 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates a component that records print requests. / 인쇄 요청을 기록하는 컴포넌트를 만든다. */
            public void run() {
                panel = new SwingWebPanel() {
                    /** Serialization version. / 직렬화 버전. */
                    private static final long serialVersionUID = 1L;

                    /** Records the request. / 요청을 기록한다. */
                    protected void printRequested() {
                        synchronized (requests) {
                            requests.add(SwingUtilities.isEventDispatchThread() ? "edt" : "other");
                        }
                    }
                };
                panel.setSize(500, 300);
            }
        });
    }

    /**
     * Disposes the component.
     * <p>
     * 컴포넌트를 폐기한다.
     */
    public void tearDown() {
        panel.dispose();
    }

    /**
     * Makes an A4 paper in portrait orientation with margins of half an inch: 595 x 842 points, page area 523 x 770 points, which is
     * 697.33 x 1026.67 CSS pixels.
     * <p>
     * 여백이 반 인치인 세로 방향 A4 용지를 만든다: 595 x 842 포인트, 쪽 영역 523 x 770 포인트, 곧 697.33 x 1026.67 CSS 픽셀.
     *
     * @return the paper / 용지
     */
    private static PageFormat a4() {
        Paper p = new Paper();
        p.setSize(595, 842);
        p.setImageableArea(36, 36, 523, 770);
        PageFormat f = new PageFormat();
        f.setPaper(p);
        return f;
    }

    /**
     * Shows a page and waits until it is loaded.
     * <p>
     * 페이지를 보이고 다 읽을 때까지 기다린다.
     *
     * @param title the title of the page / 페이지의 제목
     * @param head  markup for the head (styles) / head 에 넣을 마크업(스타일)
     * @param body  the markup of the body / 본문 마크업
     * @throws Exception on failure / 실패 시
     */
    private void show(String title, String head, String body) throws Exception {
        panel.setContent("<!doctype html><title>" + title + "</title>" + head + "<body style='margin:0'>" + body + "</body>", "text/html");
        long end = System.currentTimeMillis() + 30000;
        // 제목이 도착하고 읽기가 끝날 때까지 짧게 쉬며 기다린다.
        while (!(title.equals(panel.getTitle()) && panel.getProgress() == 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(title, panel.getTitle(), "the page is shown");
    }

    /**
     * Asks for a printout and waits for it.
     * <p>
     * 인쇄물을 요청하고 기다린다.
     *
     * @param paper       the paper / 용지
     * @param backgrounds true to print backgrounds / 배경을 인쇄하려면 true
     * @return the printout, or null if none was made / 인쇄물. 만들지 못했으면 null
     * @throws Exception on failure / 실패 시
     */
    private PagePrintout printout(PageFormat paper, boolean backgrounds) throws Exception {
        final PagePrintout[] got = new PagePrintout[1];
        final boolean[] edt = new boolean[1];
        final CountDownLatch done = new CountDownLatch(1);
        panel.createPrintout(paper, backgrounds, new PrintoutListener() {
            /** Keeps the printout. / 인쇄물을 보관한다. */
            public void printoutReady(PagePrintout printout) {
                got[0] = printout;
                edt[0] = SwingUtilities.isEventDispatchThread();
                done.countDown();
            }
        });
        Check.isTrue(done.await(30, TimeUnit.SECONDS), "the printout arrives");
        Check.isTrue(edt[0], "the listener is called on the event dispatch thread");
        return got[0];
    }

    /**
     * Paints a page of a printout into an image of the size of its paper, two pixels per point, on white.
     * <p>
     * 인쇄물의 한 쪽을 그 용지 크기의 이미지에 포인트당 두 화소로, 흰 바탕 위에 그린다.
     *
     * @param p     the printout / 인쇄물
     * @param index the page / 쪽
     * @return the image / 이미지
     * @throws Exception on failure / 실패 시
     */
    private static BufferedImage image(PagePrintout p, int index) throws Exception {
        PageFormat f = p.getPageFormat(index);
        BufferedImage image = new BufferedImage((int) Math.ceil(f.getWidth() * 2), (int) Math.ceil(f.getHeight() * 2), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.scale(2, 2);
            Check.equal(Printable.PAGE_EXISTS, p.print(g, f, index), "the page exists");
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Counts the pixels of an image that are (nearly) a pure color.
     * <p>
     * 이미지에서 (거의) 순수한 한 색인 화소를 센다.
     *
     * @param image the image / 이미지
     * @param rgb   0xFF0000 for red, 0x00FF00 for green or 0x0000FF for blue / 빨강이면 0xFF0000, 초록이면 0x00FF00, 파랑이면 0x0000FF
     * @return the number of pixels / 화소 수
     */
    private static int count(BufferedImage image, int rgb) {
        int n = 0;
        // 화소마다 고른 채널만 밝고 나머지는 어두운지 본다.
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                boolean red = r > 200 && g < 60 && b < 60;
                boolean green = g > 200 && r < 60 && b < 60;
                boolean blue = b > 200 && r < 60 && g < 60;
                if (rgb == 0xFF0000 && red || rgb == 0x00FF00 && green || rgb == 0x0000FF && blue) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Counts the dark pixels (text and borders) of an image.
     * <p>
     * 이미지의 어두운 화소(글자와 border)를 센다.
     *
     * @param image the image / 이미지
     * @return the number of pixels / 화소 수
     */
    private static int dark(BufferedImage image) {
        int n = 0;
        // 화소마다 세 채널이 모두 어두운지 본다.
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                if (((c >> 16) & 0xFF) < 128 && ((c >> 8) & 0xFF) < 128 && (c & 0xFF) < 128) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Paints the component as the screen shows it.
     * <p>
     * 컴포넌트를 화면이 보이는 대로 그린다.
     *
     * @return the image / 이미지
     * @throws Exception on failure / 실패 시
     */
    private BufferedImage screen() throws Exception {
        final BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        // 화면용 배치가 게시될 시간을 준 뒤 그린다.
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints the component. / 컴포넌트를 그린다. */
            public void run() {
                Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
            }
        });
        return image;
    }

    /**
     * C-PRINT-1: pages end at the page height but not inside a line, a replaced element or a box that avoids breaks; forced breaks
     * end a page early; the last page ends with the content.
     * <p>
     * C-PRINT-1: 쪽은 쪽 높이에서 끝나되 줄, 치환 요소, 나눔을 피하는 상자 안에서는 끝나지 않는다. 강제 나눔은 쪽을 일찍 끝낸다. 마지막 쪽은
     * 내용과 함께 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_PRINT_1_PageBreaks() throws Exception {
        StringBuilder lines = new StringBuilder("<p style='margin:0;font:16px/20px monospace'>");
        // 높이 20px 인 줄 120개를 만든다.
        for (int i = 1; i <= 120; i++) {
            lines.append("line ").append(i).append("<br>");
        }
        show("lines", "", lines.append("</p>").toString());
        PagePrintout p = printout(a4(), false);
        Check.notNull(p, "a printout was made");
        double[] breaks = p.getPageBreaks();
        Check.equal(3, p.getNumberOfPages(), "2400px of lines need three pages of 1026.67px");
        Check.near(0, breaks[0], 0.01, "the first page starts at the top");
        Check.near(1020, breaks[1], 0.01, "the first page ends between two lines, not at 1026.67 inside line 52");
        Check.near(2040, breaks[2], 0.01, "the second page takes the next 51 lines");
        Check.near(2400, breaks[3], 0.01, "the last page ends with the content");
        Check.near(595, p.getPageFormat(0).getWidth(), 0.01, "the pages have the paper of the host");
        Check.near(36, p.getPageFormat(2).getImageableX(), 0.01, "and its margins");
        Check.near(1.0, p.getShrinkFactor(), 0.0001, "content that fits is not shrunk");
        Check.isTrue(dark(image(p, 0)) > 2000, "the text of the first page is painted");
        Check.isTrue(dark(image(p, 2)) > 500, "and the text of the last page");

        show("forced", "", "<div style='height:100px;border:1px solid black'>a</div>"
                + "<h2 style='break-before:page;page-break-after:always;margin:0;height:50px;font:16px/20px monospace'>title</h2>"
                + "<div style='height:30px;border:1px solid black'>b</div>");
        p = printout(a4(), false);
        breaks = p.getPageBreaks();
        Check.equal(3, p.getNumberOfPages(), "two forced breaks make three pages");
        Check.near(102, breaks[1], 0.01, "break-before: page ends the page above the heading");
        Check.near(152, breaks[2], 0.01, "page-break-after: always ends the page below it");
        Check.near(184, breaks[3], 0.01, "the rest is on the third page");

        show("avoid", "", "<div style='height:900px;border:1px solid black'>a</div>"
                + "<div id=k style='break-inside:avoid;height:300px;border:1px solid black'>keep</div>");
        p = printout(a4(), false);
        Check.near(902, p.getPageBreaks()[1], 0.01, "a box that avoids breaks moves to the next page as a whole");
        show("sliced", "", "<div style='height:900px;border:1px solid black'>a</div>"
                + "<div style='height:300px;border:1px solid black'>split</div>");
        p = printout(a4(), false);
        Check.near(1026.67, p.getPageBreaks()[1], 0.01, "a plain box without lines at the edge is sliced at the page height");
        show("replaced", "", "<div style='height:900px'></div><canvas width=100 height=300 style='display:block;border:0'></canvas><p>tail</p>");
        p = printout(a4(), false);
        Check.near(900, p.getPageBreaks()[1], 0.01, "a replaced element is not cut");
        show("orphans", "", "<div style='height:1000px'></div>"
                + "<p style='margin:0;font:16px/20px monospace;orphans:3;widows:3'>one<br>two<br>three<br>four<br>five<br>six<br>seven</p>");
        p = printout(a4(), false);
        Check.near(1000, p.getPageBreaks()[1], 0.01, "one line would fit, but orphans: 3 keeps the first three lines together");
    }

    /**
     * C-PRINT-2: the layout is made for the print medium, and the <code>@page</code> rules give the pages their size and margins.
     * <p>
     * C-PRINT-2: 배치는 인쇄 매체용으로 만들고, <code>@page</code> 규칙이 쪽의 크기와 여백을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_PRINT_2_PrintMediumAndPageRules() throws Exception {
        String boxes = "<div class=s style='height:60px;background:rgb(0,0,255)'></div><div class=p style='height:60px;background:rgb(255,0,0)'></div>";
        show("medium", "<style>@media print { .s { display: none } } @media screen { .p { display: none } }</style>", boxes);
        PagePrintout p = printout(a4(), true);
        BufferedImage page = image(p, 0);
        Check.isTrue(count(page, 0xFF0000) > 5000, "the box of the print medium is printed: " + count(page, 0xFF0000));
        Check.equal(0, count(page, 0x0000FF), "the box of the screen medium is not printed");
        BufferedImage shown = screen();
        Check.isTrue(count(shown, 0x0000FF) > 5000, "the screen still shows the box of the screen medium");
        Check.equal(0, count(shown, 0xFF0000), "and not the box of the print medium");

        show("page", "<style>@page { size: 400px 300px; margin: 20px } @page :first { margin-top: 100px }</style>",
                "<div style='height:500px;border:1px solid black'>tall</div>");
        p = printout(a4(), false);
        PageFormat first = p.getPageFormat(0);
        PageFormat second = p.getPageFormat(1);
        Check.near(300, first.getWidth(), 0.01, "size: 400px is 300 points wide");
        Check.near(225, first.getHeight(), 0.01, "and 300px is 225 points high");
        Check.equal(PageFormat.LANDSCAPE, first.getOrientation(), "a page that is wider than high is a landscape page");
        Check.near(15, first.getImageableX(), 0.01, "margin: 20px is 15 points");
        Check.near(270, first.getImageableWidth(), 0.01, "the page area is what the margins leave");
        Check.near(75, first.getImageableY(), 0.01, "the first page has its own top margin");
        Check.near(15, second.getImageableY(), 0.01, "the other pages have the common one");
        Check.near(180, p.getPageBreaks()[1], 0.01, "the first page area is 300 - 100 - 20 = 180px high");
        Check.near(440, p.getPageBreaks()[2], 0.01, "the second is 260px high");
        Check.equal(3, p.getNumberOfPages(), "502px need three such pages");

        show("landscape", "<style>@page { size: landscape; margin: 0.5in 1in }</style>", "<p>wide</p>");
        p = printout(a4(), false);
        first = p.getPageFormat(0);
        Check.near(842, first.getWidth(), 0.01, "size: landscape turns the paper of the host");
        Check.near(595, first.getHeight(), 0.01, "its height is the short side");
        Check.near(72, first.getImageableX(), 0.01, "the left margin is one inch");
        Check.near(36, first.getImageableY(), 0.01, "the top margin is half an inch");
        Check.near(842 - 144, first.getImageableWidth(), 0.01, "the page area lies between the margins");
        Check.isTrue(dark(image(p, 0)) > 20, "the text is painted on the turned page");
    }

    /**
     * C-PRINT-3: without backgrounds, background colors are left out but text and borders are printed; a box that asks for exact
     * colors keeps its background; with backgrounds everything is printed.
     * <p>
     * C-PRINT-3: 배경 없이 인쇄하면 배경색은 빠지지만 글자와 border 는 인쇄된다. 정확한 색을 요청한 상자는 배경을 유지한다. 배경과 함께
     * 인쇄하면 모두 인쇄된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_PRINT_3_Backgrounds() throws Exception {
        show("bg", "<style>body { background: rgb(0,0,255) }</style>", "<div style='height:80px;background:rgb(255,0,0);border:4px solid black;"
                + "font:20px monospace'>text</div><div style='height:40px;background:rgb(0,255,0);print-color-adjust:exact'></div>");
        PagePrintout plain = printout(a4(), false);
        BufferedImage a = image(plain, 0);
        Check.isFalse(plain.isBackgroundPrinted(), "the printout tells that backgrounds are left out");
        Check.equal(0, count(a, 0xFF0000), "the background of the box is left out");
        Check.equal(0, count(a, 0x0000FF), "the background of the page is left out");
        Check.isTrue(dark(a) > 3000, "the border and the text are printed: " + dark(a));
        Check.isTrue(count(a, 0x00FF00) > 5000, "print-color-adjust: exact keeps a background: " + count(a, 0x00FF00));
        PagePrintout full = printout(a4(), true);
        BufferedImage b = image(full, 0);
        Check.isTrue(full.isBackgroundPrinted(), "the printout tells that backgrounds are printed");
        Check.isTrue(count(b, 0xFF0000) > 5000, "the background of the box is printed");
        Check.isTrue(count(b, 0x0000FF) > 500000, "the background of the page fills the page area below the content: " + count(b, 0x0000FF));
        Check.equal(0xFFFFFF, b.getRGB(5, 5) & 0xFFFFFF, "nothing is painted into the margins");
    }

    /**
     * C-PRINT-4: the page hears <code>beforeprint</code> before the layout and <code>afterprint</code> after it; what the first changes
     * is printed; <code>window.print()</code> reaches <code>printRequested()</code> on the event dispatch thread.
     * <p>
     * C-PRINT-4: 페이지는 배치 전에 <code>beforeprint</code> 를, 뒤에 <code>afterprint</code> 를 듣는다. 앞의 것이 바꾼 것이 인쇄된다.
     * <code>window.print()</code> 는 이벤트 디스패치 스레드에서 <code>printRequested()</code> 에 닿는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_PRINT_4_EventsAndScriptPrint() throws Exception {
        show("events", "", "<div id=box style='height:60px;background:rgb(0,0,255)'></div><script>var log = [];"
                + "window.addEventListener('beforeprint', function () { log.push('before'); document.getElementById('box').style.background = 'rgb(255,0,0)'; });"
                + "window.onafterprint = function () { log.push('after'); document.getElementById('box').style.background = 'rgb(0,0,255)';"
                + " document.title = log.join(','); };"
                + "window.addEventListener('message', function () { var r = window.print(); document.title = 'asked:' + r; });</" + "script>");
        PagePrintout p = printout(a4(), true);
        BufferedImage page = image(p, 0);
        Check.isTrue(count(page, 0xFF0000) > 5000, "the change of the beforeprint listener is printed");
        Check.equal(0, count(page, 0x0000FF), "the state before it is not");
        long end = System.currentTimeMillis() + 10000;
        // afterprint 가 제목을 바꿀 때까지 기다린다.
        while (!"before,after".equals(panel.getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("before,after", panel.getTitle(), "both events arrive, in this order, once");
        Check.equal("events", p.getTitle(), "the printout has the title of the moment it was asked for");
        Check.equal("about:text", p.getUrl(), "and the address of the page");
        BufferedImage shown = screen();
        Check.isTrue(count(shown, 0x0000FF) > 5000, "after printing the screen shows what afterprint restored");
        panel.postMessage("print");
        end = System.currentTimeMillis() + 10000;
        // 스크립트의 인쇄 요청이 호스트에 닿을 때까지 기다린다.
        while (System.currentTimeMillis() < end) {
            synchronized (requests) {
                if (!requests.isEmpty()) {
                    break;
                }
            }
            Thread.sleep(10);
        }
        synchronized (requests) {
            Check.equal(1, requests.size(), "window.print() reaches the host once");
            Check.equal("edt", requests.get(0), "on the event dispatch thread");
        }
        end = System.currentTimeMillis() + 10000;
        // 스크립트가 반환값을 제목에 적을 때까지 기다린다.
        while (!"asked:undefined".equals(panel.getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("asked:undefined", panel.getTitle(), "window.print() returns at once with undefined");
    }

    /**
     * C-PRINT-5: content that is wider than the page area is shrunk to fit; a box that is fixed to the viewport is printed on every
     * page; the zoom and the scroll position of the screen do not matter and are not changed.
     * <p>
     * C-PRINT-5: 쪽 영역보다 넓은 내용은 맞게 줄인다. 뷰포트에 고정된 상자는 쪽마다 인쇄된다. 화면의 확대 비율과 스크롤 위치는 상관이 없고
     * 바뀌지도 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_PRINT_5_FitFixedAndScreenState() throws Exception {
        show("wide", "", "<div style='width:1394.66px;height:100px;border:0;background:rgb(255,0,0)'></div>");
        PagePrintout p = printout(a4(), true);
        Check.near(0.5, p.getShrinkFactor(), 0.01, "content twice as wide as the page area is shrunk to half");
        BufferedImage page = image(p, 0);
        Check.isTrue(Math.abs(count(page, 0xFF0000) - 1046 * 75) < 5000, "the box fills the width of the page area at half size: " + count(page, 0xFF0000));

        show("fixed", "", "<div style='position:fixed;top:0;left:0;width:100px;height:40px;background:rgb(0,255,0);print-color-adjust:exact'></div>"
                + "<div style='height:2500px'><p style='margin-top:60px'>long</p></div><p>end</p>");
        panel.setZoom(2.0);
        p = printout(a4(), false);
        Check.equal(3, p.getNumberOfPages(), "the zoom of the screen does not change the pages");
        // 쪽마다 고정된 상자가 위쪽에 그려졌는지 본다.
        for (int i = 0; i < p.getNumberOfPages(); i++) {
            int green = count(image(p, i).getSubimage(0, 0, 400, 200), 0x00FF00);
            Check.isTrue(green > 6000, "the fixed box is printed at the top of page " + (i + 1) + ": " + green);
        }
        Check.near(2.0, panel.getZoom(), 0.0001, "printing leaves the zoom as it is");
        BufferedImage shown = screen();
        Check.isTrue(count(shown, 0x00FF00) > 5000, "the screen still shows the page, zoomed");
    }

    /**
     * C-PRINT-6: the rules of the methods: null arguments are refused, the blank page prints one empty page, pages behind the last do
     * not exist, a text page and a page without scripts can be printed, the default of <code>printRequested()</code> does nothing, and
     * a disposed component refuses.
     * <p>
     * C-PRINT-6: 메서드의 규칙: null 인수는 거부하고, 빈 페이지는 빈 쪽 하나를 인쇄하며, 마지막 쪽 뒤의 쪽은 없고, 평문 페이지와 스크립트 없는
     * 페이지를 인쇄할 수 있으며, <code>printRequested()</code> 의 기본 구현은 아무것도 하지 않고, 폐기된 컴포넌트는 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_PRINT_6_Rules() throws Exception {
        final PrintoutListener ignore = new PrintoutListener() {
            /** Ignores the printout. / 인쇄물을 무시한다. */
            public void printoutReady(PagePrintout printout) {
                return;
            }
        };
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Passes no paper. / 용지를 넘기지 않는다. */
            public void run() {
                panel.createPrintout(null, false, ignore);
            }
        }, "a null paper is refused");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Passes no listener. / 리스너를 넘기지 않는다. */
            public void run() {
                panel.createPrintout(a4(), false, null);
            }
        }, "a null listener is refused");
        final PagePrintout blank = printout(a4(), false);
        Check.equal(1, blank.getNumberOfPages(), "the blank page prints one page");
        Check.equal(0, dark(image(blank, 0)), "which is empty");
        BufferedImage scratch = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            Check.equal(Printable.NO_SUCH_PAGE, blank.print(g, a4(), 1), "there is no page behind the last one");
            Check.equal(Printable.NO_SUCH_PAGE, blank.print(g, a4(), -1), "and none before the first");
        } finally {
            g.dispose();
        }
        Check.same(blank, blank.getPrintable(0), "the printout paints its own pages");
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            /** Asks for a page that does not exist. / 없는 쪽을 요청한다. */
            public void run() {
                blank.getPageFormat(1);
            }
        }, "a page that does not exist has no format");
        panel.setJavascriptEnabled(false);
        panel.setContent("plain text\nfor the printer", "text/plain");
        long end = System.currentTimeMillis() + 30000;
        // 평문 페이지가 도착할 때까지 기다린다.
        while (!("text/plain".equals(panel.getMimeType()) && panel.getProgress() == 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        PagePrintout text = printout(a4(), false);
        Check.equal(1, text.getNumberOfPages(), "a text page without scripts is printed");
        Check.isTrue(dark(image(text, 0)) > 100, "its text is painted");
        final SwingWebPanel plain = new SwingWebPanel();
        plain.setContent("<script>window.print(); document.title = 'printed';</" + "script>", "text/html");
        end = System.currentTimeMillis() + 30000;
        // 스크립트가 끝까지 실행될 때까지 기다린다.
        while (!"printed".equals(plain.getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("printed", plain.getTitle(), "without a host that prints, window.print() does nothing and the script goes on");
        plain.dispose();
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Asks a disposed component for a printout. / 폐기된 컴포넌트에 인쇄물을 요청한다. */
            public void run() {
                plain.createPrintout(a4(), false, ignore);
            }
        }, "a disposed component refuses");
    }
}
