package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import org.duckdns.hjow.swingwebpanel.core.PrintLayout;

/**
 * A page of a {@link SwingWebPanel} laid out for printing and divided into paper pages (extension, API contract 5.30). It is made by
 * {@link SwingWebPanel#createPrintout(PageFormat, boolean, PrintoutListener)} and is a snapshot: it does not change when the page of the
 * component changes, and it can be painted on any thread and as often as needed (a preview and then the printer). It implements the
 * standard interfaces of <code>java.awt.print</code>, so it is given to a <code>PrinterJob</code> as it is.
 * <p>
 * Every page has a format of its own ({@link #getPageFormat(int)}): the paper size and, as the imageable area, the page area that the
 * content is painted into. They come from the paper that the host passed, changed by the <code>@page</code> rules of the document
 * (size and margins). The content never paints outside the page area, so a host can draw headers and footers into the margins by
 * wrapping the printout in a <code>Printable</code> of its own.
 * <p>
 * {@link SwingWebPanel} 의 페이지를 인쇄용으로 배치하고 종이 쪽으로 나눈 것(확장, API 계약 5.30).
 * {@link SwingWebPanel#createPrintout(PageFormat, boolean, PrintoutListener)} 가 만들며 스냅샷이다: 컴포넌트의 페이지가 바뀌어도 바뀌지 않고,
 * 어느 스레드에서나 필요한 만큼 여러 번 그릴 수 있다(미리 보기, 그다음 프린터). <code>java.awt.print</code> 의 표준 인터페이스를 구현하므로
 * 그대로 <code>PrinterJob</code> 에 넘긴다.
 * <p>
 * 쪽마다 자기 형식이 있다({@link #getPageFormat(int)}): 용지 크기와, 인쇄 가능 영역으로서 내용이 그려지는 쪽 영역. 호스트가 넘긴 용지를 문서의
 * <code>@page</code> 규칙(크기와 여백)으로 바꾼 것이다. 내용은 쪽 영역 밖에 그리지 않으므로, 호스트는 인쇄물을 자기 <code>Printable</code> 로
 * 감싸 여백에 머리글과 바닥글을 그릴 수 있다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * final PrinterJob job = PrinterJob.getPrinterJob();
 * panel.createPrintout(job.defaultPage(), false, new PrintoutListener() {
 *     public void printoutReady(PagePrintout printout) {
 *         if (printout != null) {
 *             job.setPageable(printout);
 *             // show job.printDialog() and call job.print() on a thread of your own
 *         }
 *     }
 * });
 * </pre>
 */
public final class PagePrintout implements Pageable, Printable {

    /** The layout of the pages. / 쪽의 레이아웃. */
    private final PrintLayout layout;

    /** The title of the page that was printed. / 인쇄한 페이지의 제목. */
    private final String title;

    /** The address of the page that was printed. / 인쇄한 페이지의 주소. */
    private final String url;

    /** True if backgrounds are painted. / 배경을 그리면 true. */
    private final boolean backgrounds;

    /**
     * Creates a printout; the component does this.
     * <p>
     * 인쇄물을 만든다. 컴포넌트가 만든다.
     *
     * @param layout      the layout of the pages / 쪽의 레이아웃
     * @param title       the title of the page / 페이지의 제목
     * @param url         the address of the page / 페이지의 주소
     * @param backgrounds true if backgrounds are painted / 배경을 그리면 true
     */
    PagePrintout(PrintLayout layout, String title, String url, boolean backgrounds) {
        this.layout = layout;
        this.title = title == null ? "" : title;
        this.url = url == null ? "" : url;
        this.backgrounds = backgrounds;
    }

    /**
     * Returns the number of paper pages.
     * <p>
     * 종이 쪽 수를 반환한다.
     *
     * @return the number, at least 1 / 쪽 수. 1 이상
     */
    public int getNumberOfPages() {
        return layout.getPageCount();
    }

    /**
     * Returns the format of a page: its paper size and orientation, and as the imageable area the page area that the content is painted
     * into. The values are in points (1/72 inch).
     * <p>
     * 한 쪽의 형식을 반환한다: 용지 크기와 방향, 그리고 인쇄 가능 영역으로서 내용이 그려지는 쪽 영역. 값의 단위는 포인트(1/72 인치)이다.
     *
     * @param pageIndex the page, starting at 0 / 쪽(0 부터)
     * @return a copy of the format / 형식의 복사본
     * @throws IndexOutOfBoundsException for a page that does not exist / 없는 쪽이면
     */
    public PageFormat getPageFormat(int pageIndex) {
        return layout.getPageFormat(pageIndex);
    }

    /**
     * Returns the object that paints a page: this printout.
     * <p>
     * 쪽을 그리는 객체를 반환한다: 이 인쇄물이다.
     *
     * @param pageIndex the page, starting at 0 / 쪽(0 부터)
     * @return this printout / 이 인쇄물
     * @throws IndexOutOfBoundsException for a page that does not exist / 없는 쪽이면
     */
    public Printable getPrintable(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= layout.getPageCount()) {
            throw new IndexOutOfBoundsException("no such page: " + pageIndex);
        }
        return this;
    }

    /**
     * Paints a page as <code>java.awt.print.Printable</code> asks: the origin of the graphics is the top left corner of the paper and
     * the unit is the point. The page is painted at the place of its own format ({@link #getPageFormat(int)}); the format argument is
     * not used, because the layout was made for the formats of this printout.
     * <p>
     * <code>java.awt.print.Printable</code> 이 요구하는 대로 한 쪽을 그린다: 그래픽스의 원점은 용지의 왼쪽 위이고 단위는 포인트이다. 쪽은 자기
     * 형식({@link #getPageFormat(int)})의 자리에 그려진다. 레이아웃이 이 인쇄물의 형식에 맞춰 만들어졌으므로 형식 인수는 쓰지 않는다.
     *
     * @param graphics   the graphics of the printer or of a preview / 프린터나 미리 보기의 그래픽스
     * @param pageFormat ignored / 쓰지 않는다
     * @param pageIndex  the page, starting at 0 / 쪽(0 부터)
     * @return <code>PAGE_EXISTS</code>, or <code>NO_SUCH_PAGE</code> behind the last page / <code>PAGE_EXISTS</code>. 마지막 쪽 뒤이면
     *         <code>NO_SUCH_PAGE</code>
     * @throws PrinterException never / 던지지 않는다
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex < 0 || pageIndex >= layout.getPageCount()) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            layout.paint(g, pageIndex);
        } finally {
            g.dispose();
        }
        return PAGE_EXISTS;
    }

    /**
     * Returns the title that the page had when the printout was made.
     * <p>
     * 인쇄물을 만들 때 페이지가 가졌던 제목을 반환한다.
     *
     * @return the title, or the empty string / 제목. 없으면 빈 문자열
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the address of the page that the printout was made of.
     * <p>
     * 인쇄물을 만든 페이지의 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public String getUrl() {
        return url;
    }

    /**
     * Tells whether the printout paints background colors, background images and box shadows.
     * <p>
     * 인쇄물이 배경색, 배경 이미지, 상자 그림자를 그리는지 알려 준다.
     *
     * @return true if backgrounds are painted / 배경을 그리면 true
     */
    public boolean isBackgroundPrinted() {
        return backgrounds;
    }

    /**
     * Returns the factor that the page was shrunk by so that content wider than the page area fits the paper.
     * <p>
     * 쪽 영역보다 넓은 내용이 용지에 들어가도록 페이지를 줄인 배율을 반환한다.
     *
     * @return the factor from 0.4 to 1; 1 if nothing was shrunk / 0.4 부터 1 까지의 배율. 줄이지 않았으면 1
     */
    public double getShrinkFactor() {
        return layout.getFit();
    }

    /**
     * Returns the vertical positions of the laid-out document, in CSS pixels, at which the pages begin and end: page <code>i</code>
     * shows the part from entry <code>i</code> to entry <code>i + 1</code>. Meant for tests and tools.
     * <p>
     * 쪽이 시작하고 끝나는, 배치된 문서의 세로 위치를 CSS 픽셀로 반환한다: 쪽 <code>i</code> 는 항목 <code>i</code> 부터 항목 <code>i + 1</code>
     * 까지의 부분을 보인다. 시험과 도구용이다.
     *
     * @return a copy with one more entry than there are pages / 쪽 수보다 항목이 하나 많은 복사본
     */
    public double[] getPageBreaks() {
        return layout.getPageEdges();
    }
}
