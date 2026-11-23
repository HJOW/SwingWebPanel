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
