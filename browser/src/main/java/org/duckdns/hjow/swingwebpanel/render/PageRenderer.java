package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.layout.CssStyleSource;
import org.duckdns.hjow.swingwebpanel.layout.LayoutEngine;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;
import org.duckdns.hjow.swingwebpanel.layout.ReplacedResolver;
import org.duckdns.hjow.swingwebpanel.layout.StyleSource;
import org.duckdns.hjow.swingwebpanel.layout.WebFontSource;

/**
 * Renders a document: layout followed by the display list (plan items P09-04 to P09-07). Call it on the document thread (it reads the DOM
 * and the styles) whenever the viewport width, the DOM, the styles or a resource changed; it always lays out from scratch, so a caller that
 * relayouts on width changes must compare the new width with the last one to avoid repeated work. The result is immutable and meant to be
 * handed to the event dispatch thread.
 * <p>
 * 문서를 렌더링한다: 레이아웃 다음에 디스플레이 리스트(계획 P09-04~P09-07). DOM 과 스타일을 읽으므로 문서 스레드에서 부르며, 뷰포트 너비, DOM,
 * 스타일, 리소스가 바뀌었을 때 부른다. 항상 처음부터 배치하므로 너비 변경에 맞춰 다시 배치하는 호출자는 반복 작업을 피하도록 새 너비를 직전 너비와
 * 비교해야 한다. 결과는 불변이며 이벤트 디스패치 스레드로 넘기기 위한 것이다.
 */
public final class PageRenderer {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PageRenderer() {
    }

    /**
     * Renders a document with a CSS engine: the natural sizes of loaded images come from the image source.
     * <p>
     * CSS 엔진으로 문서를 렌더링한다. 읽은 이미지의 자연 크기는 이미지 소스에서 온다.
     *
     * @param doc       the document / 문서
     * @param css       the CSS engine of the document / 문서의 CSS 엔진
     * @param images    image and media states, may be null / 이미지와 미디어 상태. null 일 수 있다
     * @param viewportW viewport width in px / 뷰포트 너비(px)
     * @param viewportH viewport height in px / 뷰포트 높이(px)
     * @return the rendered page / 렌더링된 페이지
     */
    public static RenderedPage render(Document doc, CssEngine css, ImageSource images, double viewportW, double viewportH) {
        return render(doc, new CssStyleSource(css), new ImageReplacedResolver(images, viewportW, viewportH), images, viewportW, viewportH);
    }

    /**
     * Renders a document with explicit style and replaced-content sources.
     * <p>
     * 스타일과 치환 콘텐츠 소스를 직접 지정해 문서를 렌더링한다.
     *
     * @param doc       the document / 문서
     * @param styles    the style source / 스타일 소스
     * @param resolver  the replaced-content resolver, may be null / 치환 콘텐츠 리졸버. null 일 수 있다
     * @param images    image and media states, may be null / 이미지와 미디어 상태. null 일 수 있다
     * @param viewportW viewport width / 뷰포트 너비
     * @param viewportH viewport height / 뷰포트 높이
     * @return the rendered page / 렌더링된 페이지
     */
    public static RenderedPage render(Document doc, StyleSource styles, ReplacedResolver resolver, ImageSource images, double viewportW,
            double viewportH) {
        return render(doc, styles, resolver, images, viewportW, viewportH, null);
    }

    /**
     * Renders a document with the web fonts of the page (P10-05).
     * <p>
     * 페이지의 웹 글꼴로 문서를 렌더링한다(P10-05).
     *
     * @param doc       the document / 문서
     * @param styles    the style source / 스타일 소스
     * @param resolver  the replaced-content resolver, may be null / 치환 콘텐츠 리졸버. null 일 수 있다
     * @param images    image and media states, may be null / 이미지와 미디어 상태. null 일 수 있다
     * @param viewportW viewport width / 뷰포트 너비
     * @param viewportH viewport height / 뷰포트 높이
     * @param webFonts  the web fonts of the page, may be null / 페이지의 웹 글꼴. null 일 수 있다
     * @return the rendered page / 렌더링된 페이지
     */
    public static RenderedPage render(Document doc, StyleSource styles, ReplacedResolver resolver, ImageSource images, double viewportW,
            double viewportH, WebFontSource webFonts) {
        LayoutResult layout = LayoutEngine.layout(doc, styles, resolver, viewportW, viewportH, webFonts);
        return new RenderedPage(layout, DisplayListBuilder.build(layout, images));
    }

    /**
     * Renders a document with a CSS engine and the web fonts of the page (P10-05).
     * <p>
     * CSS 엔진과 페이지의 웹 글꼴로 문서를 렌더링한다(P10-05).
     *
     * @param doc       the document / 문서
     * @param css       the CSS engine of the document / 문서의 CSS 엔진
     * @param images    image and media states, may be null / 이미지와 미디어 상태. null 일 수 있다
     * @param viewportW viewport width in px / 뷰포트 너비(px)
     * @param viewportH viewport height in px / 뷰포트 높이(px)
     * @param webFonts  the web fonts of the page, may be null / 페이지의 웹 글꼴. null 일 수 있다
     * @return the rendered page / 렌더링된 페이지
     */
    public static RenderedPage render(Document doc, CssEngine css, ImageSource images, double viewportW, double viewportH, WebFontSource webFonts) {
        return render(doc, new CssStyleSource(css), new ImageReplacedResolver(images, viewportW, viewportH), images, viewportW, viewportH, webFonts);
    }

    /**
     * Lays a document out for printing and builds its drawing commands: the styles come from a style source of the print medium, the
     * viewport is the page area, and the backgrounds can be left out. Nothing of the result is shared with the rendering for the screen.
     * <p>
     * 문서를 인쇄용으로 배치하고 그림 명령을 만든다: 스타일은 인쇄 매체의 스타일 소스에서 오고, 뷰포트는 쪽 영역이며, 배경을 뺄 수 있다. 결과의
     * 어떤 것도 화면용 렌더링과 공유하지 않는다.
     *
     * @param doc         the document / 문서
     * @param styles      the styles of the print medium / 인쇄 매체의 스타일
     * @param images      the image source of the page / 페이지의 이미지 소스
     * @param pageWidth   the width of the page area in CSS pixels / 쪽 영역의 너비(CSS 픽셀)
     * @param pageHeight  the height of the page area in CSS pixels / 쪽 영역의 높이(CSS 픽셀)
     * @param webFonts    the web fonts of the page, or null / 페이지의 웹 글꼴. 없으면 null
     * @param backgrounds false to leave out background colors, background images and box shadows / 배경색, 배경 이미지, 상자 그림자를
     *                    빼려면 false
     * @return the rendering / 렌더링
     */
    public static RenderedPage renderForPrint(Document doc, StyleSource styles, ImageSource images, double pageWidth, double pageHeight,
            WebFontSource webFonts, boolean backgrounds) {
        LayoutResult layout = LayoutEngine.layout(doc, styles, new ImageReplacedResolver(images, pageWidth, pageHeight), pageWidth, pageHeight,
                webFonts);
        return new RenderedPage(layout, DisplayListBuilder.build(layout, images, backgrounds));
    }

    /**
     * Builds the page of an existing layout (for a host that runs the layout itself).
     * <p>
     * 이미 있는 레이아웃의 페이지를 만든다(레이아웃을 직접 실행하는 호스트용).
     *
     * @param layout the layout / 레이아웃
     * @param images image and media states, may be null / 이미지와 미디어 상태. null 일 수 있다
     * @return the rendered page / 렌더링된 페이지
     */
    public static RenderedPage fromLayout(LayoutResult layout, ImageSource images) {
        return new RenderedPage(layout, DisplayListBuilder.build(layout, images));
    }
}
