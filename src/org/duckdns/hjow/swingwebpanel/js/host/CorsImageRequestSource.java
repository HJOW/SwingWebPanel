package org.duckdns.hjow.swingwebpanel.js.host;

/**
 * An {@link ImageRequestSource} that also fetches images in CORS mode, for an <code>img</code> element with a <code>crossorigin</code>
 * attribute: a cross-origin request carries the <code>Origin</code> header, the cookies follow the mode, and a response that does not grant
 * access to the document ends the load with an error (so the <code>error</code> event fires and a canvas never gets the pixels), while a
 * granted image can be read back without tainting a canvas. A host that offers only the plain interface loads such images without CORS.
 * <p>
 * <code>crossorigin</code> 속성이 있는 <code>img</code> 요소를 위해 이미지를 CORS 모드로도 가져오는 {@link ImageRequestSource}: 교차 오리진 요청은
 * <code>Origin</code> 헤더를 싣고, 쿠키는 모드를 따르며, 문서에 접근을 허용하지 않는 응답은 로드를 오류로 끝내고(그래서 <code>error</code> 이벤트가 나고 캔버스는
 * 화소를 받지 못한다) 허용된 이미지는 캔버스를 오염시키지 않고 다시 읽을 수 있다. 일반 인터페이스만 주는 호스트는 그런 이미지를 CORS 없이 로드한다.
 */
public interface CorsImageRequestSource extends ImageRequestSource {

    /**
     * Starts loading an image in CORS mode, or joins a load of the same URL and mode that is running or has finished. The callback is called
     * exactly once unless the page is closed first.
     * <p>
     * 이미지 로드를 CORS 모드로 시작한다. 같은 URL 과 모드의 진행 중이거나 끝난 로드가 있으면 거기에 합류한다. 그 전에 페이지가 닫히지 않으면 콜백은 정확히
     * 한 번 호출된다.
     *
     * @param absoluteUrl the absolute URL of the image / 이미지의 절대 URL
     * @param crossOrigin <code>anonymous</code> (cookies only for the origin of the document) or <code>use-credentials</code> (cookies for every
     *                    origin, the response must allow credentials) / <code>anonymous</code>(쿠키는 문서의 오리진에만) 또는 <code>use-credentials</code>(모든
     *                    오리진에 쿠키, 응답이 자격 증명을 허용해야 함)
     * @param callback    the receiver / 받는 쪽
     */
    void request(String absoluteUrl, String crossOrigin, Callback callback);
}
