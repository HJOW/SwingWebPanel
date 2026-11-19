package org.duckdns.hjow.swingwebpanel.render;

/**
 * An {@link ImageSource} that can also give the state of an image that is fetched in CORS mode, for an <code>img</code> element with a
 * <code>crossorigin</code> attribute. The same address may be an image of a plain element and of a <code>crossorigin</code> element at once,
 * and the two are different resources (the CORS one carries <code>Origin</code> and fails when the server gives no permission), so the renderer
 * asks for the one that belongs to the element ({@link ImageLookup}). A source that does not implement this interface knows only plain images.
 * <p>
 * CORS 모드로 가져오는 이미지의 상태도 줄 수 있는 {@link ImageSource}. <code>crossorigin</code> 속성이 있는 <code>img</code> 요소를 위한 것이다. 같은 주소가 일반 요소와
 * <code>crossorigin</code> 요소의 이미지일 수 있고 둘은 다른 리소스이므로(CORS 쪽은 <code>Origin</code> 을 싣고 서버가 허락하지 않으면 실패한다) 렌더러는 요소에
 * 속하는 쪽을 묻는다({@link ImageLookup}). 이 인터페이스를 구현하지 않는 소스는 일반 이미지만 안다.
 */
public interface CorsImageSource extends ImageSource {

    /**
     * Returns the state of an image in a CORS mode, starting the fetch on the first lookup.
     * <p>
     * CORS 모드 이미지의 상태를 반환한다. 첫 조회에 가져오기를 시작한다.
     *
     * @param absoluteUrl the absolute URL of the image / 이미지의 절대 URL
     * @param cors        <code>anonymous</code> or <code>use-credentials</code> / <code>anonymous</code> 또는 <code>use-credentials</code>
     * @return the state; null means "not requested yet" / 상태. null 은 "아직 요청하지 않음"
     */
    ImageResource getImage(String absoluteUrl, String cors);
}
