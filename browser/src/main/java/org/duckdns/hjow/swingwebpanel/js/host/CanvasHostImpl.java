package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasHost;
import org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasImage;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.OriginPolicy;
import org.duckdns.hjow.swingwebpanel.render.ImageResource;

/**
 * The adapter that lets the canvas package use a {@link PageScripts}: the realm, the bridge, the document, the style resolver, the pixels of the
 * images that the page loaded (an <code>img</code> is "available" when its load ended without error and "broken" when it failed) with the
 * origin-clean rule of the canvas specification, and the change notification. An image is origin-clean when it came from the origin of the
 * document, or when the <code>img</code> has a <code>crossorigin</code> attribute and the response allowed the document origin with an
 * <code>Access-Control-Allow-Origin</code> header (and <code>Access-Control-Allow-Credentials: true</code> for <code>use-credentials</code>). The
 * request of an image does not carry an <code>Origin</code> header (only requests of the web request API do, P20-08-c), so a server that only
 * answers a request with that header does not make an image origin-clean yet. A page whose own origin is opaque (<code>about:blank</code>, <code>about:text</code>) treats every image as foreign.
 * For an animated image the first frame is drawn.
 * <p>
 * 캔버스 패키지가 {@link PageScripts} 를 쓰게 하는 어댑터: realm, 브리지, 문서, 스타일 리졸버, 페이지가 읽은 이미지의 화소(<code>img</code> 는 로드가 오류
 * 없이 끝나면 "사용 가능", 실패하면 "깨짐")와 캔버스 규격의 origin-clean 규칙, 변경 알림. 이미지는 문서의 오리진에서 왔거나, <code>img</code> 에
 * <code>crossorigin</code> 속성이 있고 응답이 <code>Access-Control-Allow-Origin</code> 헤더로 문서 오리진을 허용했으면(<code>use-credentials</code> 이면
 * <code>Access-Control-Allow-Credentials: true</code> 도) origin-clean 이다. 이미지 요청은
 * <code>Origin</code> 요청 헤더를 싣지 않으므로(싣는 것은 웹 요청 API 의 요청뿐, P20-08-c) 그 헤더가 있어야 답하는 서버는 아직 이미지를 origin-clean 으로 만들지
 * 못한다. 자기 오리진이 불투명한 페이지(<code>about:blank</code>, <code>about:text</code>)는
 * 모든 이미지를 외부 것으로 본다. 애니메이션 이미지는 첫 프레임을 그린다.
 */
final class CanvasHostImpl implements CanvasHost {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Creates the adapter.
     * <p>
     * 어댑터를 만든다.
     *
     * @param page the page / 페이지
     */
    CanvasHostImpl(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the realm of the page.
     * <p>
     * 페이지의 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public JsRealm realm() {
        return page.getRealm();
    }

    /**
     * Returns the DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    public DomBridge bridge() {
        return page.getBridge();
    }

    /**
     * Returns the document of the page.
     * <p>
     * 페이지의 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document document() {
        return page.getDocument();
    }

    /**
     * Returns the style resolver of the environment.
     * <p>
     * 환경의 스타일 리졸버를 반환한다.
     *
     * @return the resolver or null / 리졸버 또는 null
     */
    public StyleResolver styleResolver() {
        return page.getEnvironment().getStyleResolver();
    }

    /**
     * Passes a canvas change to the environment.
     * <p>
     * 캔버스 변경을 환경에 전달한다.
     *
     * @param canvas the canvas element / 캔버스 요소
     */
    public void canvasChanged(Element canvas) {
        page.getEnvironment().canvasChanged(canvas);
    }

    /**
     * Tells whether the page was disposed.
     * <p>
     * 페이지가 폐기되었는지 알려 준다.
     *
     * @return true after disposal / 폐기 뒤에는 true
     */
    public boolean isDisposed() {
        return page.isDisposedPage();
    }

    /**
     * Returns the pixels of the image of an <code>img</code> element.
     * <p>
     * <code>img</code> 요소의 이미지 화소를 반환한다.
     *
     * @param img the element / 요소
     * @return the pixels, or null if the image is not available yet / 화소. 이미지가 아직 없으면 null
     * @throws DomException InvalidStateError if the image is broken / 이미지가 깨졌으면 InvalidStateError
     */
    public CanvasImage imagePixels(Element img) {
        ImageLoads loads = page.images();
        String src = loads.currentSrc(img);
        if (!loads.isComplete(img) || src.length() == 0) {
            return null;
        }
        if (loads.isBroken(img)) {
            throw DomException.invalidState("The HTMLImageElement provided is in the 'broken' state.");
        }
        // crossorigin 속성이 있는 요소의 이미지는 CORS 모드로 가져온 항목에서 읽는다.
        ImageResource r = page.getEnvironment().getLoadedImage(src, org.duckdns.hjow.swingwebpanel.render.ImageLookup.corsMode(img));
        if (r == null || r.getState() == ImageResource.State.PENDING) {
            return null;
        }
        if (r.getState() == ImageResource.State.FAILED || r.getImage() == null) {
            throw DomException.invalidState("The HTMLImageElement provided is in the 'broken' state.");
        }
        DecodedImage di = r.getImage();
        return new CanvasImage(di.getWidth(), di.getHeight(), di.getFrames().get(0).getArgb(), isOriginClean(img, src, r));
    }

    /**
     * Applies the origin-clean rule to an image.
     * <p>
     * 이미지에 origin-clean 규칙을 적용한다.
     *
     * @param img the element / 요소
     * @param src the URL that was requested / 요청한 URL
     * @param r   the resource / 리소스
     * @return true if scripts may read the pixels back / 스크립트가 화소를 다시 읽어도 되면 true
     */
    private boolean isOriginClean(Element img, String src, ImageResource r) {
        Origin doc = Origin.of(page.getDocument().getOriginUrl());
        Origin from = Origin.of(r.getSourceUrl() == null ? src : r.getSourceUrl());
        if (!doc.isOpaque() && doc.isSameOrigin(from)) {
            return true;
        }
        String cors = img.getAttribute("crossorigin");
        // crossorigin 속성이 있으면 응답의 CORS 허용 헤더가 문서 오리진을 허용해야 한다.
        if (cors != null) {
            boolean credentials = "use-credentials".equalsIgnoreCase(cors.trim());
            return OriginPolicy.corsAllowsDocument(doc, r.getAllowOrigin(), r.getAllowCredentials(), credentials);
        }
        return false;
    }
}
