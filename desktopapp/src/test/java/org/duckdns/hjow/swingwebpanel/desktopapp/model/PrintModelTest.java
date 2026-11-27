package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The print models of the browser (plan item P31-10): the paper of the page setup as a setting, and the document that adds headers and
 * footers to the pages of a printout. Nothing here reaches a printer; pages are painted into images.
 * <p>
 * 브라우저의 인쇄 모델(계획 P31-10): 설정으로서의 페이지 설정 용지, 그리고 인쇄물의 쪽에 머리글과 바닥글을 더하는 문서. 여기의 어떤 것도
 * 프린터에 닿지 않는다. 쪽은 이미지에 그린다.
 */
public class PrintModelTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PrintModelTest() {
    }

    /**
     * Pages of a fixed format that paint one red square in the middle of the page area.
     * <p>
     * 쪽 영역 한가운데에 빨간 정사각형 하나를 그리는, 정해진 형식의 쪽들.
     */
    private static final class Content implements Pageable, Printable {

        /** The format of every page. / 모든 쪽의 형식. */
        private final PageFormat format;

        /** The number of pages. / 쪽 수. */
        private final int pages;

        /**
         * Creates the pages.
         * <p>
         * 쪽들을 만든다.
         *
         * @param format the format / 형식
         * @param pages  the number of pages / 쪽 수
         */
        Content(PageFormat format, int pages) {
            this.format = format;
            this.pages = pages;
        }

        /** Returns the number of pages. / 쪽 수를 반환한다.
         * @return the number / 쪽 수 */
        public int getNumberOfPages() {
            return pages;
        }

        /** Returns the format. / 형식을 반환한다.
         * @param pageIndex the page / 쪽
         * @return the format / 형식 */
        public PageFormat getPageFormat(int pageIndex) {
            return format;
        }

        /** Returns this object. / 이 객체를 반환한다.
         * @param pageIndex the page / 쪽
         * @return this / 이 객체 */
        public Printable getPrintable(int pageIndex) {
            return this;
        }

        /** Paints the square. / 정사각형을 그린다.
         * @param g the graphics / 그래픽스
         * @param f the format / 형식
         * @param pageIndex the page / 쪽
         * @return whether the page exists / 쪽이 있는지 */
        public int print(Graphics g, PageFormat f, int pageIndex) {
            if (pageIndex >= pages) {
                return NO_SUCH_PAGE;
            }
            g.setColor(Color.RED);
            g.fillRect((int) (format.getImageableX() + format.getImageableWidth() / 2) - 10, (int) (format.getImageableY() + format.getImageableHeight() / 2) - 10, 20, 20);
            return PAGE_EXISTS;
        }
    }

    /**
     * Paints a page of a document into an image of the size of the paper, two pixels per point.
     * <p>
     * 문서의 한 쪽을 용지 크기의 이미지에 포인트당 두 화소로 그린다.
     *
     * @param doc   the document / 문서
     * @param index the page / 쪽
     * @return the image / 이미지
     * @throws Exception on failure / 실패 시
     */
    private static BufferedImage paint(PrintDocument doc, int index) throws Exception {
        PageFormat f = doc.getPageFormat(index);
        BufferedImage image = new BufferedImage((int) (f.getWidth() * 2), (int) (f.getHeight() * 2), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.scale(2, 2);
            Check.equal(Printable.PAGE_EXISTS, doc.print(g, f, index), "the page exists");
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Counts the pixels that are not white in a band of rows of an image.
     * <p>
     * 이미지의 행 구간에서 흰색이 아닌 화소를 센다.
     *
     * @param image the image / 이미지
     * @param from  the first row / 첫 행
     * @param to    the row behind the last / 마지막 행의 다음
     * @return the number of pixels / 화소 수
     */
    private static int marks(BufferedImage image, int from, int to) {
        int n = 0;
        // 구간의 화소마다 흰색보다 어두운지 본다.
        for (int y = Math.max(0, from); y < to && y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                if (((c >> 16) & 0xFF) < 200 || ((c >> 8) & 0xFF) < 200 || (c & 0xFF) < 200) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * The paper setting: the default paper follows the locale, a format survives writing and reading in every orientation, and text
     * that makes no sense gives the default paper.
     * <p>
     * 용지 설정: 기본 용지는 로캘을 따르고, 형식은 어느 방향이든 쓰고 읽은 뒤에도 같으며, 말이 안 되는 글은 기본 용지를 준다.
     */
    public void testPaperSetting() {
        PageFormat a4 = PaperFormat.defaultFormat(Locale.KOREA);
        Check.near(595.28, a4.getWidth(), 0.01, "A4 is the default paper outside North America");
        Check.near(841.89, a4.getHeight(), 0.01, "A4 height");
        Check.near(36, a4.getImageableX(), 0.01, "the default margins are half an inch");
        Check.near(612, PaperFormat.defaultFormat(Locale.US).getWidth(), 0.01, "Letter is the default paper in the United States");
        Check.equal("595.28 841.89 36 36 36 36 portrait", PaperFormat.format(a4), "the text of the default paper");
        Check.equal("210 x 297 mm", PaperFormat.describe(a4), "the description for the user");
        PageFormat wide = PaperFormat.make(842, 595, 72, 36, 18, 54, PageFormat.LANDSCAPE);
        Check.near(842, wide.getWidth(), 0.01, "a landscape page is as wide as asked");
        Check.near(72, wide.getImageableX(), 0.01, "its left margin");
        Check.near(36, wide.getImageableY(), 0.01, "its top margin");
        Check.near(842 - 72 - 18, wide.getImageableWidth(), 0.01, "its page area width");
        Check.near(595 - 36 - 54, wide.getImageableHeight(), 0.01, "its page area height");
        String text = PaperFormat.format(wide);
        Check.equal("842 595 72 36 18 54 landscape", text, "the text of a landscape page");
        PageFormat back = PaperFormat.parse(text, Locale.KOREA);
        Check.equal(PageFormat.LANDSCAPE, back.getOrientation(), "the orientation survives");
        Check.equal(text, PaperFormat.format(back), "reading and writing gives the same text");
        PageFormat reverse = PaperFormat.make(842, 595, 72, 36, 18, 54, PageFormat.REVERSE_LANDSCAPE);
        Check.equal("842 595 72 36 18 54 reverse-landscape", PaperFormat.format(reverse), "the reverse landscape orientation keeps its margins");
        Check.equal(PaperFormat.format(reverse), PaperFormat.format(PaperFormat.parse(PaperFormat.format(reverse), null)), "and survives too");
        String[] bad = { null, "", "nonsense", "595 842 36 36 36 36", "595 842 36 36 36 36 sideways", "595 842 300 36 300 36 portrait",
            "10 842 1 1 1 1 portrait", "595 842 -5 36 36 36 portrait", "595 x 36 36 36 36 portrait", "NaN 842 36 36 36 36 portrait" };
        // 말이 안 되는 글마다 기본 용지가 나오는지 본다.
        for (int i = 0; i < bad.length; i++) {
            Check.equal(PaperFormat.format(a4), PaperFormat.format(PaperFormat.parse(bad[i], Locale.KOREA)), "the default paper for: " + bad[i]);
        }
    }

    /**
     * The print document: it has the pages of the content, draws the header and the footer into the margins only when asked, leaves
     * them out where the margin is too small, and shortens long texts.
     * <p>
     * 인쇄 문서: 내용의 쪽을 가지고, 요청했을 때만 머리글과 바닥글을 여백에 그리며, 여백이 너무 좁으면 빼고, 긴 글은 줄인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeadersAndFooters() throws Exception {
        PageFormat a4 = PaperFormat.defaultFormat(Locale.KOREA);
        final PrintDocument with = new PrintDocument(new Content(a4, 3), true, "The title", "http://example.test/page", "2026-11-27");
        Check.equal(3, with.getNumberOfPages(), "the document has the pages of the content");
        Check.isTrue(with.hasHeaders(), "headers are on");
        String[] texts = with.getMarginTexts(1);
        Check.equal("The title", texts[0], "the header starts with the title");
        Check.equal("2026-11-27", texts[1], "and ends with the date");
        Check.equal("http://example.test/page", texts[2], "the footer starts with the address");
        Check.equal("2 / 3", texts[3], "and ends with the page number and the count");
        BufferedImage page = paint(with, 1);
        int top = (int) (a4.getImageableY() * 2);
        int bottom = (int) ((a4.getImageableY() + a4.getImageableHeight()) * 2);
        Check.isTrue(marks(page, 0, top) > 50, "the header is drawn into the top margin: " + marks(page, 0, top));
        Check.isTrue(marks(page, bottom, page.getHeight()) > 50, "the footer is drawn into the bottom margin");
        Check.isTrue(marks(page, top, bottom) > 1000, "the content is painted in the page area");
        PrintDocument without = new PrintDocument(new Content(a4, 3), false, "The title", "http://example.test/page", "2026-11-27");
        BufferedImage bare = paint(without, 1);
        Check.equal(0, marks(bare, 0, top), "without headers the top margin stays empty");
        Check.equal(0, marks(bare, bottom, bare.getHeight()), "and the bottom margin too");
        Check.equal(marks(page, top, bottom), marks(bare, top, bottom), "the content is the same with and without headers");
        PageFormat tight = PaperFormat.make(595, 842, 36, 4, 36, 4, PageFormat.PORTRAIT);
        BufferedImage cramped = paint(new PrintDocument(new Content(tight, 1), true, "t", "u", "d"), 0);
        Check.equal(0, marks(cramped, 0, 8), "a margin that is too small gets no header");
        Check.equal(0, marks(cramped, (int) ((842 - 4) * 2), cramped.getHeight()), "and no footer");
        BufferedImage scratch = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            java.awt.FontMetrics fm = g.getFontMetrics(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 8));
            Check.equal("short", PrintDocument.shorten("short", fm, 500), "a text that fits stays");
            String cut = PrintDocument.shorten("a very long title that does not fit into the space that is left", fm, 80);
            Check.isTrue(cut.endsWith("...") && fm.stringWidth(cut) <= 80, "a long text is shortened to fit: " + cut);
            Check.equal("", PrintDocument.shorten("anything", fm, 1), "nothing is left where nothing fits");
            Check.equal(Printable.NO_SUCH_PAGE, with.print(g, a4, 3), "there is no page behind the last one");
        } finally {
            g.dispose();
        }
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Creates a document without content. / 내용 없는 문서를 만든다. */
            public void run() {
                new PrintDocument(null, true, "", "", "");
            }
        }, "a document needs content");
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            /** Asks for a page that does not exist. / 없는 쪽을 요청한다. */
            public void run() {
                with.getPrintable(3);
            }
        }, "a page that does not exist has no painter");
    }
}
