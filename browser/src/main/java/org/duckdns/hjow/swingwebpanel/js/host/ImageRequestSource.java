package org.duckdns.hjow.swingwebpanel.js.host;

/**
 * The service through which a page asks the component to load the images of its <code>img</code> elements (plan item P18-05). The component
 * fetches and decodes the image with the common request layer and keeps the result in the cache that the renderer paints from, so loading an
 * image for the <code>load</code> event and painting it share one fetch. The page only learns whether the image became available and its
 * natural size; it never receives the pixels. A page without such a service (a test host) simply never loads images.
 * <p>
 * 페이지가 <code>img</code> 요소의 이미지를 컴포넌트에 가져오게 하는 서비스(계획 P18-05). 컴포넌트가 공통 요청 계층으로 이미지를 가져와 디코딩하고
 * 결과를 렌더러가 그리는 캐시에 보관하므로, <code>load</code> 이벤트를 위한 로드와 그리기가 한 번의 가져오기를 공유한다. 페이지는 이미지를 쓸 수
 * 있게 되었는지와 자연 크기만 알고 픽셀은 받지 않는다. 이 서비스가 없는 페이지(시험 호스트)는 이미지를 로드하지 않는다.
 */
public interface ImageRequestSource {

    /**
     * Receives the result of one image request.
     * <p>
     * 이미지 요청 하나의 결과를 받는다.
     */
    interface Callback {
        /**
         * Reports the result. It may be called on any thread, also before {@link ImageRequestSource#request(String, Callback)} returned.
         * <p>
         * 결과를 알린다. 어느 스레드에서나 호출될 수 있으며 {@link ImageRequestSource#request(String, Callback)} 가 돌아오기 전에도 호출될 수 있다.
         *
         * @param width  the natural width in px (0 when the image failed) / 자연 너비(px). 실패했으면 0
         * @param height the natural height in px (0 when the image failed) / 자연 높이(px). 실패했으면 0
         * @param error  null if the image can be shown, else a short description of the failure / 보여 줄 수 있으면 null, 아니면 실패의 짧은 설명
         */
        void done(int width, int height, String error);
    }

    /**
     * Starts loading an image, or joins a load of the same URL that is running or has finished. The callback is called exactly once unless the
     * page is closed first.
     * <p>
     * 이미지 로드를 시작한다. 같은 URL 의 진행 중이거나 끝난 로드가 있으면 거기에 합류한다. 그 전에 페이지가 닫히지 않으면 콜백은 정확히 한 번 호출된다.
     *
     * @param absoluteUrl the absolute URL of the image / 이미지의 절대 URL
     * @param callback    the receiver / 받는 쪽
     */
    void request(String absoluteUrl, Callback callback);
}
