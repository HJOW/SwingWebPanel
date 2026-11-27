package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * What the browser prints for one page view (plan item P31-10): the pages of a printout of the component, and around each of them,
 * if the settings ask for it, a header (the title on the left, the date on the right) and a footer (the address on the left, the page
 * number and the number of pages on the right) in the page margins. It is given to a print job, or painted into the preview, as
 * <code>java.awt.print.Pageable</code>. A header or footer is left out on a page whose margin is too small for it. The class does not
 * talk to a printer.
 * <p>
 * 브라우저가 페이지 보기 하나에 대해 인쇄하는 것(계획 P31-10): 컴포넌트의 인쇄물의 쪽들과, 설정이 요청하면 그 둘레의 쪽 여백에 그리는
 * 머리글(왼쪽에 제목, 오른쪽에 날짜)과 바닥글(왼쪽에 주소, 오른쪽에 쪽 번호와 전체 쪽 수). <code>java.awt.print.Pageable</code> 로서 인쇄 작업에
 * 넘기거나 미리 보기에 그린다. 여백이 너무 좁은 쪽에서는 머리글이나 바닥글을 뺀다. 이 클래스는 프린터와 통신하지 않는다.
 */
public final class PrintDocument implements Pageable, Printable {

    /** The size of the font of the header and the footer in points. / 머리글과 바닥글의 글꼴 크기(포인트). */
    private static final int FONT_SIZE = 8;

    /** The distance between the page area and the header or footer in points. / 쪽 영역과 머리글·바닥글 사이의 거리(포인트). */
    private static final double GAP = 6;

    /** The pages of the content. / 내용의 쪽들. */
    private final Pageable content;

    /** True if headers and footers are drawn. / 머리글과 바닥글을 그리면 true. */
    private final boolean headers;

    /** The title of the page. / 페이지의 제목. */
    private final String title;

    /** The address of the page. / 페이지의 주소. */
    private final String url;

    /** The date for the header. / 머리글에 쓸 날짜. */
    private final String date;

    /**
     * Creates the document.
     * <p>
     * 문서를 만든다.
     *
     * @param content the pages of the content (a printout of the component) / 내용의 쪽들(컴포넌트의 인쇄물)
     * @param headers true to draw headers and footers / 머리글과 바닥글을 그리려면 true
     * @param title   the title of the page / 페이지의 제목
     * @param url     the address of the page / 페이지의 주소
     * @param date    the date for the header, already formatted / 머리글에 쓸 날짜(이미 서식을 갖춘 글)
     */
    public PrintDocument(Pageable content, boolean headers, String title, String url, String date) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        this.content = content;
        this.headers = headers;
        this.title = title == null ? "" : title;
        this.url = url == null ? "" : url;
        this.date = date == null ? "" : date;
    }

    /**
     * Returns the number of pages.
     * <p>
     * 쪽 수를 반환한다.
     *
     * @return the number of pages of the content / 내용의 쪽 수
     */
    public int getNumberOfPages() {
        return content.getNumberOfPages();
    }

    /**
     * Returns the format of a page (that of the content).
     * <p>
     * 한 쪽의 형식(내용의 형식)을 반환한다.
     *
     * @param pageIndex the page, starting at 0 / 쪽(0 부터)
     * @return the format / 형식
     */
    public PageFormat getPageFormat(int pageIndex) {
        return content.getPageFormat(pageIndex);
    }

    /**
     * Returns the object that paints a page: this document.
     * <p>
     * 쪽을 그리는 객체를 반환한다: 이 문서이다.
     *
     * @param pageIndex the page, starting at 0 / 쪽(0 부터)
     * @return this document / 이 문서
     */
    public Printable getPrintable(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= content.getNumberOfPages()) {
            throw new IndexOutOfBoundsException("no such page: " + pageIndex);
        }
        return this;
    }

    /**
     * Tells whether headers and footers are drawn.
     * <p>
     * 머리글과 바닥글을 그리는지 알려 준다.
     *
     * @return true if they are drawn / 그리면 true
     */
    public boolean hasHeaders() {
        return headers;
    }

    /**
     * Returns the text of the header or the footer of a page.
     * <p>
     * 한 쪽의 머리글이나 바닥글의 글을 반환한다.
     *
     * @param pageIndex the page, starting at 0 / 쪽(0 부터)
     * @return {header left, header right, footer left, footer right} / {머리글 왼쪽, 머리글 오른쪽, 바닥글 왼쪽, 바닥글 오른쪽}
     */
    public String[] getMarginTexts(int pageIndex) {
        return new String[] { title, date, url, (pageIndex + 1) + " / " + content.getNumberOfPages() };
    }

    /**
     * Paints a page: the content, and the header and footer in the margins. The origin of the graphics is the top left corner of the
     * paper and the unit is the point, as for every <code>Printable</code>.
     * <p>
     * 한 쪽을 그린다: 내용과, 여백의 머리글·바닥글. 모든 <code>Printable</code> 처럼 그래픽스의 원점은 용지의 왼쪽 위이고 단위는 포인트이다.
     *
     * @param graphics   the graphics / 그래픽스
     * @param pageFormat ignored; the page has the format of {@link #getPageFormat(int)} / 쓰지 않는다. 쪽은
     *                   {@link #getPageFormat(int)} 의 형식을 가진다
     * @param pageIndex  the page, starting at 0 / 쪽(0 부터)
     * @return <code>PAGE_EXISTS</code> or <code>NO_SUCH_PAGE</code> / <code>PAGE_EXISTS</code> 또는 <code>NO_SUCH_PAGE</code>
     * @throws PrinterException if the content fails to print / 내용의 인쇄가 실패하면
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex < 0 || pageIndex >= content.getNumberOfPages()) {
            return NO_SUCH_PAGE;
        }
        PageFormat f = content.getPageFormat(pageIndex);
        int result = content.getPrintable(pageIndex).print(graphics, f, pageIndex);
        if (result != PAGE_EXISTS || !headers) {
            return result;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE));
            g.setColor(Color.DARK_GRAY);
            FontMetrics fm = g.getFontMetrics();
            String[] texts = getMarginTexts(pageIndex);
            double left = f.getImageableX();
            double width = f.getImageableWidth();
            double top = f.getImageableY();
            double bottom = top + f.getImageableHeight();
            // 머리글은 쪽 영역 위의 여백에, 글자가 들어갈 만큼 여백이 있을 때만 그린다.
            if (top >= fm.getHeight() + GAP) {
                line(g, fm, texts[0], texts[1], left, width, top - GAP - fm.getDescent());
            }
            if (f.getHeight() - bottom >= fm.getHeight() + GAP) {
                line(g, fm, texts[2], texts[3], left, width, bottom + GAP + fm.getAscent());
            }
        } finally {
            g.dispose();
        }
        return PAGE_EXISTS;
    }

    /**
     * Draws one line with a text at the left and a text at the right; the left text is shortened so that the two do not overlap.
     * <p>
     * 왼쪽 글과 오른쪽 글이 있는 한 줄을 그린다. 둘이 겹치지 않게 왼쪽 글을 줄인다.
     *
     * @param g        the graphics / 그래픽스
     * @param fm       the metrics of its font / 그 글꼴의 측정값
     * @param leftText the text at the left / 왼쪽 글
     * @param rightText the text at the right / 오른쪽 글
     * @param x        the left edge of the page area / 쪽 영역의 왼쪽 가장자리
     * @param width    the width of the page area / 쪽 영역의 너비
     * @param baseline the baseline of the line / 줄의 기준선
     */
    private static void line(Graphics2D g, FontMetrics fm, String leftText, String rightText, double x, double width, double baseline) {
        int rightWidth = fm.stringWidth(rightText);
        g.drawString(rightText, (float) (x + width - rightWidth), (float) baseline);
        g.drawString(shorten(leftText, fm, Math.max(0, width - rightWidth - 12)), (float) x, (float) baseline);
    }

    /**
     * Shortens a text with an ellipsis until it fits a width.
     * <p>
     * 글이 너비에 들어갈 때까지 줄임표로 줄인다.
     *
     * @param text  the text / 글
     * @param fm    the font metrics / 글꼴 측정값
     * @param width the width that is available / 쓸 수 있는 너비
     * @return the text, or a shortened text that ends with three dots / 글. 또는 점 세 개로 끝나는 줄인 글
     */
    static String shorten(String text, FontMetrics fm, double width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String dots = "...";
        int n = text.length();
        // 들어갈 때까지 끝에서 한 글자씩 줄인다.
        while (n > 0 && fm.stringWidth(text.substring(0, n) + dots) > width) {
            n--;
        }
        return n == 0 ? "" : text.substring(0, n) + dots;
    }
}
