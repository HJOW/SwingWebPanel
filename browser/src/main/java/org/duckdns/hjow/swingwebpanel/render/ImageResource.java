package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The state of an image or media resource as the renderer sees it when it builds a display list (plan items P09-05, P18-06): loaded
 * (with the decoded image for images), failed (with the reason) or still pending. Instances are immutable; when a resource changes the host
 * builds a new display list.
 * <p>
 * 디스플레이 리스트를 만들 때 렌더러가 보는 이미지/미디어 리소스의 상태(계획 P09-05, P18-06): 읽음(이미지는 디코딩된 이미지 포함), 실패(이유 포함),
 * 아직 진행 중. 인스턴스는 불변이며 리소스가 바뀌면 호스트가 새 디스플레이 리스트를 만든다.
 */
public final class ImageResource {

    /**
     * The state of a resource.
     * <p>
     * 리소스의 상태.
     */
    public enum State {
        /** The resource is available. 리소스를 쓸 수 있다. */
        LOADED,
        /** The resource could not be used (access failure, unsupported format, damaged data). 리소스를 쓸 수 없다(접근 실패, 미지원 형식, 손상). */
        FAILED,
        /** The resource is still being fetched or decoded. 리소스를 아직 가져오거나 디코딩하는 중이다. */
        PENDING
    }

    /**
     * A media resource that is available but has no image (audio).
     * <p>
     * 이미지가 없는 사용 가능한 미디어 리소스(오디오).
     */
    public static final ImageResource AVAILABLE = new ImageResource(State.LOADED, null, "");

    /**
     * A resource that is still pending.
     * <p>
     * 아직 진행 중인 리소스.
     */
    public static final ImageResource PENDING = new ImageResource(State.PENDING, null, "");

    /**
     * The state.
     * <p>
     * 상태.
     */
    private final State state;

    /**
     * The decoded image or null.
     * <p>
     * 디코딩된 이미지 또는 null.
     */
    private final DecodedImage image;

    /**
     * The failure message or the empty string.
     * <p>
     * 실패 메시지 또는 빈 문자열.
     */
    private final String message;

    /**
     * Creates a resource state.
     * <p>
     * 리소스 상태를 만든다.
     *
     * @param state   state / 상태
     * @param image   decoded image or null / 디코딩된 이미지 또는 null
     * @param message failure message / 실패 메시지
     */
    private ImageResource(State state, DecodedImage image, String message) {
        this(state, image, message, null, null, null);
    }

    /**
     * The final URL that the image was read from (after redirects), or null if unknown.
     * <p>
     * 이미지를 읽어 온 최종 URL(리디렉션 뒤). 모르면 null.
     */
    private final String sourceUrl;

    /**
     * The value of the <code>Access-Control-Allow-Origin</code> header of the response, or null.
     * <p>
     * 응답의 <code>Access-Control-Allow-Origin</code> 헤더 값. 없으면 null.
     */
    private final String allowOrigin;

    /**
     * The value of the <code>Access-Control-Allow-Credentials</code> header of the response, or null.
     * <p>
     * 응답의 <code>Access-Control-Allow-Credentials</code> 헤더 값. 없으면 null.
     */
    private final String allowCredentials;

    /**
     * Creates a resource state with the origin information of the response (for the <code>drawImage()</code> of a canvas, which taints
     * itself when the image came from another origin without permission).
     * <p>
     * 응답의 오리진 정보가 있는 리소스 상태를 만든다(다른 오리진에서 허락 없이 온 이미지를 그리면 스스로 오염되는 캔버스의 <code>drawImage()</code> 용).
     *
     * @param state            state / 상태
     * @param image            decoded image or null / 디코딩된 이미지 또는 null
     * @param message          failure message / 실패 메시지
     * @param sourceUrl        the final URL or null / 최종 URL 또는 null
     * @param allowOrigin      the CORS allow-origin header or null / CORS allow-origin 헤더 또는 null
     * @param allowCredentials the CORS allow-credentials header or null / CORS allow-credentials 헤더 또는 null
     */
    private ImageResource(State state, DecodedImage image, String message, String sourceUrl, String allowOrigin, String allowCredentials) {
        this.state = state;
        this.image = image;
        this.message = message == null ? "" : message;
        this.sourceUrl = sourceUrl;
        this.allowOrigin = allowOrigin;
        this.allowCredentials = allowCredentials;
    }

    /**
     * Creates the state of a loaded image together with where it came from and what its response allowed.
     * <p>
     * 읽은 이미지의 상태를 그 출처와 응답이 허용한 것과 함께 만든다.
     *
     * @param image            the decoded image, not null / 디코딩된 이미지(null 불가)
     * @param sourceUrl        the final URL of the response / 응답의 최종 URL
     * @param allowOrigin      the <code>Access-Control-Allow-Origin</code> header or null / <code>Access-Control-Allow-Origin</code> 헤더 또는 null
     * @param allowCredentials the <code>Access-Control-Allow-Credentials</code> header or null / <code>Access-Control-Allow-Credentials</code> 헤더 또는 null
     * @return the resource / 리소스
     */
    public static ImageResource loaded(DecodedImage image, String sourceUrl, String allowOrigin, String allowCredentials) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        return new ImageResource(State.LOADED, image, "", sourceUrl, allowOrigin, allowCredentials);
    }

    /**
     * Returns the final URL that the image was read from.
     * <p>
     * 이미지를 읽어 온 최종 URL 을 반환한다.
     *
     * @return the URL, or null if unknown / URL. 모르면 null
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * Returns the <code>Access-Control-Allow-Origin</code> header of the response.
     * <p>
     * 응답의 <code>Access-Control-Allow-Origin</code> 헤더를 반환한다.
     *
     * @return the value or null / 값 또는 null
     */
    public String getAllowOrigin() {
        return allowOrigin;
    }

    /**
     * Returns the <code>Access-Control-Allow-Credentials</code> header of the response.
     * <p>
     * 응답의 <code>Access-Control-Allow-Credentials</code> 헤더를 반환한다.
     *
     * @return the value or null / 값 또는 null
     */
    public String getAllowCredentials() {
        return allowCredentials;
    }

    /**
     * Creates the state of a loaded image.
     * <p>
     * 읽은 이미지의 상태를 만든다.
     *
     * @param image the decoded image, not null / 디코딩된 이미지(null 불가)
     * @return the resource / 리소스
     */
    public static ImageResource loaded(DecodedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        return new ImageResource(State.LOADED, image, "");
    }

    /**
     * Creates the state of a failed resource.
     * <p>
     * 실패한 리소스의 상태를 만든다.
     *
     * @param message the reason, may be empty / 이유. 비어 있어도 된다
     * @return the resource / 리소스
     */
    public static ImageResource failed(String message) {
        return new ImageResource(State.FAILED, null, message);
    }

    /**
     * Returns the state.
     * <p>
     * 상태를 반환한다.
     *
     * @return the state / 상태
     */
    public State getState() {
        return state;
    }

    /**
     * Returns the decoded image.
     * <p>
     * 디코딩된 이미지를 반환한다.
     *
     * @return the image, or null if there is none / 이미지. 없으면 null
     */
    public DecodedImage getImage() {
        return image;
    }

    /**
     * Returns the failure message.
     * <p>
     * 실패 메시지를 반환한다.
     *
     * @return the message, the empty string if there is none / 메시지. 없으면 빈 문자열
     */
    public String getMessage() {
        return message;
    }
}
