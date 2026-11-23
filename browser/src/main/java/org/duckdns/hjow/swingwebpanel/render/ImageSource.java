package org.duckdns.hjow.swingwebpanel.render;

/**
 * Supplies the images and media states that the renderer needs while it builds a display list. The host implements it on top of its resource
 * loader and decoders (<code>media.image.ImageDecoders</code>); the renderer only reads states and never starts a fetch. A method may be called
 * from the document thread many times per build, so it must be cheap and must not block.
 * <p>
 * 렌더러가 디스플레이 리스트를 만들 때 필요한 이미지와 미디어 상태를 제공한다. 호스트가 리소스 로더와 디코더(<code>media.image.ImageDecoders</code>)
 * 위에 구현하며 렌더러는 상태만 읽고 가져오기를 시작하지 않는다. 메서드는 한 번 만드는 동안 문서 스레드에서 여러 번 불릴 수 있으므로 값싸고 막지 않아야 한다.
 */
public interface ImageSource {

    /**
     * Returns the state of an image (<code>img</code>, background image).
     * <p>
     * 이미지(<code>img</code>, 배경 이미지)의 상태를 반환한다.
     *
     * @param absoluteUrl the absolute URL of the image / 이미지의 절대 URL
     * @return the state; null means "not requested yet" and is treated as pending / 상태. null 은 "아직 요청하지 않음"이며 진행 중으로 본다
     */
    ImageResource getImage(String absoluteUrl);

    /**
     * Returns the state of an audio resource.
     * <p>
     * 오디오 리소스의 상태를 반환한다.
     *
     * @param absoluteUrl the absolute URL of the audio file / 오디오 파일의 절대 URL
     * @return the state ({@link ImageResource#AVAILABLE} when it can be played); null is treated as pending / 상태(재생할 수 있으면 {@link ImageResource#AVAILABLE}). null 은 진행 중으로 본다
     */
    ImageResource getMedia(String absoluteUrl);
}
