package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * What the canvas bindings need from the page that owns them: the realm, the DOM bridge, the document, the style resolver (for the font size and
 * color of a canvas element), the pixels of the images that scripts draw, the notification that a canvas changed, and the state of the page.
 * The page implements it ({@link org.duckdns.hjow.swingwebpanel.js.host.PageScripts} through a small adapter), so the canvas package does not depend on the
 * internals of the script host.
 * <p>
 * 캔버스 연결이 그것을 소유한 페이지에 필요로 하는 것: realm, DOM 브리지, 문서, 스타일 리졸버(캔버스 요소의 글꼴 크기와 색을 위해), 스크립트가 그리는
 * 이미지의 화소, 캔버스가 바뀌었다는 알림, 페이지의 상태. 페이지가 이를 구현하므로({@link org.duckdns.hjow.swingwebpanel.js.host.PageScripts} 가 작은 어댑터로),
 * 캔버스 패키지는 스크립트 호스트의 내부에 의존하지 않는다.
 */
public interface CanvasHost {

    /**
     * Returns the realm of the page (use it only while the page lock is held, that is inside script calls).
     * <p>
     * 페이지의 realm 을 반환한다(페이지 잠금을 잡은 동안, 즉 스크립트 호출 안에서만 쓴다).
     *
     * @return the realm / realm
     */
    JsRealm realm();

    /**
     * Returns the DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    DomBridge bridge();

    /**
     * Returns the document of the page.
     * <p>
     * 페이지의 문서를 반환한다.
     *
     * @return the document / 문서
     */
    Document document();

    /**
     * Returns the style resolver of the page, or null if the page has none.
     * <p>
     * 페이지의 스타일 리졸버를 반환한다. 없으면 null.
     *
     * @return the resolver or null / 리졸버 또는 null
     */
    StyleResolver styleResolver();

    /**
     * Returns the pixels of an <code>img</code> element that <code>drawImage()</code> or <code>createPattern()</code> may read.
     * <p>
     * <code>drawImage()</code> 나 <code>createPattern()</code> 이 읽을 수 있는 <code>img</code> 요소의 화소를 반환한다.
     *
     * @param img the <code>img</code> element / <code>img</code> 요소
     * @return the pixels, or null if the image is not (yet) available, in which case nothing is drawn / 화소. 이미지가 (아직) 없으면 null이며 그러면 아무것도 그리지 않는다
     * @throws DomException InvalidStateError if the image is broken / 이미지가 깨졌으면 InvalidStateError
     */
    CanvasImage imagePixels(Element img);

    /**
     * Tells the page that the picture of a canvas changed, so that it is drawn again. It may be called while the surface is locked and must only
     * post work.
     * <p>
     * 캔버스의 그림이 바뀌었음을 페이지에 알려 다시 그리게 한다. 표면이 잠긴 채 불릴 수 있으므로 작업을 올리기만 해야 한다.
     *
     * @param canvas the canvas element / 캔버스 요소
     */
    void canvasChanged(Element canvas);

    /**
     * Tells whether the page was disposed (scripts no longer run).
     * <p>
     * 페이지가 폐기되었는지(스크립트가 더 이상 실행되지 않는지) 알려 준다.
     *
     * @return true after disposal / 폐기 뒤에는 true
     */
    boolean isDisposed();
}
