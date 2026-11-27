package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.PageStyles;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.PageRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.selector.ElementState;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.layout.CssStyleSource;
import org.duckdns.hjow.swingwebpanel.layout.PageBreaks;
import org.duckdns.hjow.swingwebpanel.layout.WebFontSource;
import org.duckdns.hjow.swingwebpanel.render.ImageSource;
import org.duckdns.hjow.swingwebpanel.render.PageRenderer;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * A page that was laid out for printing and cut into paper pages (plan item P31-10, API contract 5.30). It is made on the document
 * thread from the live document, with the styles of the <code>print</code> medium and the <code>@page</code> rules, and is immutable
 * afterwards: any thread can paint its pages. Nothing of it is shared with the rendering for the screen, so making one changes neither
 * what the component shows nor its scroll position.
 * <p>
 * Steps: the page area of the first page (the paper of the host, changed by <code>@page</code> size and margins) gives the width of the
 * layout; the document is laid out once at that width; if its content is wider than the page area it is shrunk to fit (not below 40
 * percent); {@link PageBreaks} cuts the layout into the bands of the pages; every page gets its own format (paper size and page area),
 * because <code>@page :first</code>, <code>:left</code> and <code>:right</code> can give pages different margins.
 * <p>
 * 인쇄용으로 배치하고 종이 쪽으로 자른 페이지(계획 P31-10, API 계약 5.30). 문서 스레드에서 살아 있는 문서로부터 <code>print</code> 매체의
 * 스타일과 <code>@page</code> 규칙으로 만들며, 그 뒤에는 불변이다: 어느 스레드에서나 쪽을 그릴 수 있다. 화면용 렌더링과 아무것도 공유하지
 * 않으므로, 만들어도 컴포넌트가 보이는 것과 스크롤 위치는 바뀌지 않는다.
 * <p>
 * 단계: 첫 쪽의 쪽 영역(호스트의 용지를 <code>@page</code> 의 크기와 여백으로 바꾼 것)이 레이아웃의 폭을 준다. 문서를 그 폭으로 한 번 배치한다.
 * 내용이 쪽 영역보다 넓으면 맞게 줄인다(40 % 아래로는 줄이지 않는다). {@link PageBreaks} 가 레이아웃을 쪽의 띠로 자른다. <code>@page
 * :first</code>, <code>:left</code>, <code>:right</code> 가 쪽마다 다른 여백을 줄 수 있으므로 쪽마다 자기 형식(용지 크기와 쪽 영역)을 가진다.
 */
public final class PrintLayout {

    /** CSS pixels per point (1/72 inch). / 포인트(1/72 인치)당 CSS 픽셀 수. */
    private static final double PX_PER_PT = 96.0 / 72.0;

    /** The smallest factor that a page that is too wide is shrunk by. / 너무 넓은 페이지를 줄이는 가장 작은 배율. */
    private static final double MIN_FIT = 0.4;

    /** The rendering for the print medium, or null for a document without content. / 인쇄 매체용 렌더링. 내용 없는 문서이면 null. */
    private final RenderedPage rendering;

    /** The edges of the pages in CSS pixels: page i shows the band from edge i to edge i + 1. / 쪽의 가장자리(CSS 픽셀): 쪽 i 는 가장자리 i 부터 i + 1 까지의 띠를 보인다. */
    private final double[] edges;

    /** The format of every page. / 쪽마다의 형식. */
    private final PageFormat[] formats;

    /** Points per CSS pixel when painting (0.75 unless the page was shrunk to fit). / 그릴 때 CSS 픽셀당 포인트 수(맞게 줄이지 않았으면 0.75). */
    private final double scale;

    /** The ARGB color of the canvas (the background of the root element or of the body) that fills the page area of every page, 0 for none. / 쪽마다 쪽 영역을 채우는 캔버스의 ARGB 색(루트 요소나 body 의 배경). 없으면 0. */
    private final int canvas;

    /**
     * Creates the layout.
     * <p>
     * 레이아웃을 만든다.
     *
     * @param rendering the rendering or null / 렌더링 또는 null
     * @param edges     the page edges in CSS pixels / 쪽 가장자리(CSS 픽셀)
     * @param formats   the format of every page / 쪽마다의 형식
     * @param scale     points per CSS pixel / CSS 픽셀당 포인트 수
     * @param canvas    the ARGB color that fills the page areas, 0 for none / 쪽 영역을 채우는 ARGB 색. 없으면 0
     */
    private PrintLayout(RenderedPage rendering, double[] edges, PageFormat[] formats, double scale, int canvas) {
        this.canvas = canvas;
        this.rendering = rendering;
        this.edges = edges;
        this.formats = formats;
        this.scale = scale;
    }

    /**
     * Makes the printout of a page that has no document to lay out (the blank page): one empty page of the paper of the host.
     * <p>
     * 배치할 문서가 없는 페이지(빈 페이지)의 인쇄물을 만든다: 호스트 용지의 빈 쪽 하나.
     *
     * @param paper the paper of the host / 호스트의 용지
     * @return the layout / 레이아웃
     */
    public static PrintLayout empty(PageFormat paper) {
        PageFormat f = (PageFormat) paper.clone();
        return new PrintLayout(null, new double[] { 0, Math.max(1, f.getImageableHeight() * PX_PER_PT) }, new PageFormat[] { f }, 1 / PX_PER_PT, 0);
    }

    /**
     * The size and the margins of the pages of one printout, in CSS pixels.
     * <p>
     * 한 인쇄물의 쪽의 크기와 여백(CSS 픽셀).
     */
    private static final class Geometry implements PageBreaks.Heights {

        /** The <code>@page</code> rules that apply to the print medium. / 인쇄 매체에 적용되는 <code>@page</code> 규칙. */
        private final List<PageRule> rules;

        /** The width of the paper of the host. / 호스트 용지의 너비. */
        private final double paperWidth;

        /** The height of the paper of the host. / 호스트 용지의 높이. */
        private final double paperHeight;

        /** The margins of the paper of the host: top, right, bottom, left. / 호스트 용지의 여백: 위, 오른쪽, 아래, 왼쪽. */
        private final double[] paperMargins;

        /** The factor that the content is shrunk by; the bands of the pages are that much taller. / 내용을 줄이는 배율. 쪽의 띠는 그만큼 더 길다. */
        private double fit = 1;

        /**
         * Reads the paper of the host.
         * <p>
         * 호스트의 용지를 읽는다.
         *
         * @param rules the <code>@page</code> rules / <code>@page</code> 규칙
         * @param paper the paper of the host / 호스트의 용지
         */
        Geometry(List<PageRule> rules, PageFormat paper) {
            this.rules = rules;
            paperWidth = paper.getWidth() * PX_PER_PT;
            paperHeight = paper.getHeight() * PX_PER_PT;
            double left = paper.getImageableX() * PX_PER_PT;
            double top = paper.getImageableY() * PX_PER_PT;
            paperMargins = new double[] { top, Math.max(0, paperWidth - left - paper.getImageableWidth() * PX_PER_PT),
                    Math.max(0, paperHeight - top - paper.getImageableHeight() * PX_PER_PT), left };
        }

        /**
         * Works out the box of a page: {width, height, margin top, right, bottom, left}. A margin that no <code>@page</code> rule sets
         * is the margin of the paper of the host; margins never leave less than a tenth of the page for the content.
         * <p>
         * 한 쪽의 상자를 구한다: {너비, 높이, 위·오른쪽·아래·왼쪽 여백}. 어떤 <code>@page</code> 규칙도 정하지 않은 여백은 호스트 용지의
         * 여백이다. 여백은 내용에 쪽의 10분의 1 보다 적게 남기지 않는다.
         *
         * @param number the page number, starting at 1 / 쪽 번호(1 부터)
         * @return the box / 상자
         */
        double[] box(int number) {
            // 왼쪽에서 오른쪽으로 읽는 문서의 첫 쪽은 오른쪽 쪽이다(CSS Paged Media 3).
            PageStyles.PageBox b = PageStyles.resolve(rules, null, number, false, number % 2 == 1, paperWidth, paperHeight);
            double w = b.getWidth() > 1 ? b.getWidth() : paperWidth;
            double h = b.getHeight() > 1 ? b.getHeight() : paperHeight;
            double[] m = new double[4];
            // 변마다 규칙이 정한 여백이나 호스트 용지의 여백을 쓴다.
            for (int i = 0; i < 4; i++) {
                m[i] = b.isMarginSet(i) ? Math.max(0, b.getMargin(i)) : paperMargins[i];
            }
            // 여백이 쪽을 다 차지하지 않게 줄인다.
            if (m[1] + m[3] > w * 0.9) {
                double k = w * 0.9 / (m[1] + m[3]);
                m[1] *= k;
                m[3] *= k;
            }
            if (m[0] + m[2] > h * 0.9) {
                double k = h * 0.9 / (m[0] + m[2]);
                m[0] *= k;
                m[2] *= k;
            }
            return new double[] { w, h, m[0], m[1], m[2], m[3] };
        }

        /**
         * Returns the height of the band of the layout that a page shows.
         * <p>
         * 한 쪽이 보이는 레이아웃 띠의 높이를 반환한다.
         *
         * @param pageNumber the page number, starting at 1 / 쪽 번호(1 부터)
         * @return the height in CSS pixels of the layout / 레이아웃의 CSS 픽셀 높이
         */
        public double pageHeight(int pageNumber) {
            double[] b = box(pageNumber);
            return Math.max(1, (b[1] - b[2] - b[4]) / fit);
        }

        /**
         * Makes the format of a page for the print API of Java: the paper size and the page area in points.
         * <p>
         * Java 의 인쇄 API 를 위한 쪽의 형식을 만든다: 포인트 단위의 용지 크기와 쪽 영역.
         *
         * @param number the page number, starting at 1 / 쪽 번호(1 부터)
         * @return the format / 형식
         */
        PageFormat format(int number) {
            double[] b = box(number);
            double w = b[0] / PX_PER_PT;
            double h = b[1] / PX_PER_PT;
            double top = b[2] / PX_PER_PT;
            double right = b[3] / PX_PER_PT;
            double bottom = b[4] / PX_PER_PT;
            double left = b[5] / PX_PER_PT;
            double areaWidth = Math.max(1, w - left - right);
            double areaHeight = Math.max(1, h - top - bottom);
            PageFormat f = new PageFormat();
            Paper p = new Paper();
            if (w <= h) {
                p.setSize(w, h);
                p.setImageableArea(left, top, areaWidth, areaHeight);
                f.setOrientation(PageFormat.PORTRAIT);
            } else {
                // 가로 방향의 쪽은 세로로 놓인 용지를 돌린 것이다: 용지의 좌표로 쪽 영역을 옮겨 적는다.
                p.setSize(h, w);
                p.setImageableArea(top, right, areaHeight, areaWidth);
                f.setOrientation(PageFormat.LANDSCAPE);
            }
            f.setPaper(p);
            return f;
        }
    }

    /**
     * Lays a document out for printing and cuts it into pages. Call it on the thread that owns the document, with the lock of its
     * presentation.
     * <p>
     * 문서를 인쇄용으로 배치하고 쪽으로 자른다. 문서를 소유한 스레드에서, 그 표현의 잠금을 쥐고 부른다.
     *
     * @param document    the document / 문서
     * @param css         the style engine of the page (its style sheets are read, nothing of it is changed) / 페이지의 스타일
     *                    엔진(스타일 시트를 읽기만 하고 아무것도 바꾸지 않는다)
     * @param images      the image source of the page / 페이지의 이미지 소스
     * @param fonts       the web fonts of the page, or null / 페이지의 웹 글꼴. 없으면 null
     * @param screen      the media environment of the screen, the base of the print environment / 화면의 미디어 환경. 인쇄 환경의 바탕
     * @param state       the dynamic state of the elements, or null / 요소의 동적 상태. 없으면 null
     * @param defaults    the default fonts of the component / 컴포넌트의 기본 글꼴
     * @param userSheets  the user style sheets of the component / 컴포넌트의 사용자 스타일 시트
     * @param paper       the paper of the host / 호스트의 용지
     * @param backgrounds false to leave out backgrounds / 배경을 빼려면 false
     * @return the layout / 레이아웃
     */
    static PrintLayout build(Document document, CssEngine css, ImageSource images, WebFontSource fonts, MediaEnvironment screen, ElementState state,
            FontDefaults defaults, List<StyleSheet> userSheets, PageFormat paper, boolean backgrounds) {
        MediaEnvironment env = screen == null ? new MediaEnvironment() : screen.copy();
        env.setMediaType("print");
        Geometry paperOnly = new Geometry(new ArrayList<PageRule>(), paper);
        double[] first = paperOnly.box(1);
        env.setViewport(first[0] - first[3] - first[5], first[1] - first[2] - first[4]);
        StyleResolver resolver = new StyleResolver(document, env);
        try {
            resolver.setDefaultFontSize(defaults.getSize());
            resolver.setUserStyleSheets(userSheets);
            if (state != null) {
                resolver.setElementState(state);
            }
            resolver.setStyleSheets(css.getStyleSheets());
            // 쪽 규칙은 인쇄 매체에서 적용되는 것을 쓴다. 규칙이 쪽 영역을 바꾸면 미디어 환경의 뷰포트도 그 크기가 된다.
            Geometry geometry = new Geometry(resolver.getPageRules(), paper);
            first = geometry.box(1);
            double areaWidth = Math.max(1, first[0] - first[3] - first[5]);
            double areaHeight = Math.max(1, first[1] - first[2] - first[4]);
            env.setViewport(areaWidth, areaHeight);
            resolver.setMediaEnvironment(env);
            RenderedPage page = PageRenderer.renderForPrint(document, new CssStyleSource(css, resolver), images, areaWidth, areaHeight, fonts,
                    backgrounds);
            double contentWidth = page.getLayout().getContentWidth();
            // 쪽 영역보다 넓은 내용은 쪽에 맞게 줄인다(너무 작아지지 않게 한도를 둔다).
            if (contentWidth > areaWidth + 0.5) {
                geometry.fit = Math.max(MIN_FIT, areaWidth / contentWidth);
            }
            double[] edges = PageBreaks.cut(page.getLayout(), geometry);
            PageFormat[] formats = new PageFormat[edges.length - 1];
            // 쪽마다 자기 형식을 만든다.
            for (int i = 0; i < formats.length; i++) {
                formats[i] = geometry.format(i + 1);
            }
            return new PrintLayout(page, edges, formats, geometry.fit / PX_PER_PT, backgrounds ? canvasColor(document, resolver) : 0);
        } finally {
            resolver.dispose();
        }
    }

    /**
     * Returns the color of the canvas of a document: the background color of the root element, or of the <code>body</code> if the root
     * has none (CSS Backgrounds 3 section 2.11). The layout paints the canvas only as far as the content reaches, so the page areas are
     * filled with this color first; a background image of the canvas is not repeated below the content.
     * <p>
     * 문서의 캔버스 색을 반환한다: 루트 요소의 배경색, 루트에 없으면 <code>body</code> 의 배경색(CSS Backgrounds 3 2.11 절). 레이아웃은 내용이
     * 닿는 데까지만 캔버스를 그리므로, 쪽 영역을 먼저 이 색으로 채운다. 캔버스의 배경 이미지는 내용 아래로 되풀이하지 않는다.
     *
     * @param document the document / 문서
     * @param resolver the resolver of the print medium / 인쇄 매체의 리졸버
     * @return the ARGB color, 0 for none / ARGB 색. 없으면 0
     */
    private static int canvasColor(Document document, StyleResolver resolver) {
        org.duckdns.hjow.swingwebpanel.dom.Element root = document.getDocumentElement();
        if (root == null) {
            return 0;
        }
        int color = resolver.getComputedStyle(root).color(org.duckdns.hjow.swingwebpanel.css.props.Prop.BACKGROUND_COLOR);
        if ((color >>> 24) != 0) {
            return color;
        }
        // 루트에 배경색이 없으면 body 요소의 배경색이 캔버스로 퍼진다.
        for (org.duckdns.hjow.swingwebpanel.dom.Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof org.duckdns.hjow.swingwebpanel.dom.Element && "body".equals(((org.duckdns.hjow.swingwebpanel.dom.Element) n).getLocalName())) {
                return resolver.getComputedStyle((org.duckdns.hjow.swingwebpanel.dom.Element) n)
                        .color(org.duckdns.hjow.swingwebpanel.css.props.Prop.BACKGROUND_COLOR);
            }
        }
        return 0;
    }

    /**
     * Returns the number of pages.
     * <p>
     * 쪽 수를 반환한다.
     *
     * @return the number, at least 1 / 쪽 수. 1 이상
     */
    public int getPageCount() {
        return formats.length;
    }

    /**
     * Returns the format of a page: the paper size, the orientation and the page area (the imageable area) in points.
     * <p>
     * 한 쪽의 형식을 반환한다: 포인트 단위의 용지 크기, 방향, 쪽 영역(인쇄 가능 영역).
     *
     * @param index the page, starting at 0 / 쪽(0 부터)
     * @return a copy of the format / 형식의 복사본
     * @throws IndexOutOfBoundsException for a page that does not exist / 없는 쪽이면
     */
    public PageFormat getPageFormat(int index) {
        if (index < 0 || index >= formats.length) {
            throw new IndexOutOfBoundsException("no such page: " + index);
        }
        return (PageFormat) formats[index].clone();
    }

    /**
     * Returns the factor that the page was shrunk by to fit the paper: 1 unless the content was wider than the page area.
     * <p>
     * 페이지를 용지에 맞추려고 줄인 배율을 반환한다: 내용이 쪽 영역보다 넓지 않았으면 1.
     *
     * @return the factor, from 0.4 to 1 / 배율. 0.4 부터 1 까지
     */
    public double getFit() {
        return scale * PX_PER_PT;
    }

    /**
     * Returns the vertical positions of the layout at which the pages begin and end, in CSS pixels; for tests.
     * <p>
     * 쪽이 시작하고 끝나는 레이아웃의 세로 위치를 CSS 픽셀로 반환한다. 시험용이다.
     *
     * @return a copy: one more entry than there are pages / 복사본: 쪽 수보다 하나 많은 항목
     */
    public double[] getPageEdges() {
        return edges.clone();
    }

    /**
     * Paints a page. The graphics must have its origin at the top left corner of the paper and points as its unit, as the graphics of
     * <code>java.awt.print.Printable.print</code> has; only the page area is painted and the state of the graphics is put back.
     * <p>
     * 한 쪽을 그린다. 그래픽스는 <code>java.awt.print.Printable.print</code> 의 그래픽스처럼 원점이 용지의 왼쪽 위이고 단위가 포인트여야 한다.
     * 쪽 영역만 그리고 그래픽스의 상태를 되돌린다.
     *
     * @param g     the graphics / 그래픽스
     * @param index the page, starting at 0 / 쪽(0 부터)
     * @throws IndexOutOfBoundsException for a page that does not exist / 없는 쪽이면
     */
    public void paint(Graphics2D g, int index) {
        if (index < 0 || index >= formats.length) {
            throw new IndexOutOfBoundsException("no such page: " + index);
        }
        if (rendering == null) {
            return;
        }
        PageFormat f = formats[index];
        double top = edges[index];
        double bandHeight = (edges[index + 1] - top) * scale;
        AffineTransform saved = g.getTransform();
        Shape savedClip = g.getClip();
        try {
            g.translate(f.getImageableX(), f.getImageableY());
            // 캔버스의 색은 내용이 끝난 아래쪽까지 쪽 영역 전체를 채운다.
            if ((canvas >>> 24) != 0) {
                java.awt.Color old = g.getColor();
                g.setColor(new java.awt.Color(canvas, true));
                g.fill(new Rectangle2D.Double(0, 0, f.getImageableWidth(), f.getImageableHeight()));
                g.setColor(old);
            }
            // 쪽의 띠 밖(다음 쪽의 첫 줄, 쪽 영역 밖)은 그리지 않는다.
            g.clip(new Rectangle2D.Double(0, 0, f.getImageableWidth(), Math.min(f.getImageableHeight(), bandHeight)));
            g.translate(0, -top * scale);
            Rectangle area = new Rectangle(0, (int) Math.floor(top * scale) - 1, (int) Math.ceil(f.getImageableWidth()) + 2,
                    (int) Math.ceil(bandHeight) + 3);
            // 뷰포트에 고정된 상자는 쪽마다 그 쪽의 위쪽을 기준으로 다시 그려진다.
            rendering.paint(g, area, 0, 0, scale, 0L, 0, (int) Math.round(top));
        } finally {
            g.setTransform(saved);
            g.setClip(savedClip);
        }
    }
}
