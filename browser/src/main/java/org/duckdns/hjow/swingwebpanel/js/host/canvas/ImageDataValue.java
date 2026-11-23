package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * An <code>ImageData</code> value: a width, a height and the RGBA bytes (<code>width * height * 4</code>) that the <code>data</code> attribute
 * shows as a <code>Uint8ClampedArray</code>. The bytes are shared with that typed array, so scripts that change <code>data</code> change the
 * pixels that <code>putImageData()</code> writes.
 * <p>
 * <code>ImageData</code> 값: 너비, 높이, 그리고 <code>data</code> 속성이 <code>Uint8ClampedArray</code> 로 보여 주는 RGBA 바이트
 * (<code>width * height * 4</code>). 바이트는 그 형식화 배열과 공유되므로 스크립트가 <code>data</code> 를 바꾸면 <code>putImageData()</code> 가 쓰는
 * 화소도 바뀐다.
 */
public final class ImageDataValue {

    /**
     * The width in pixels.
     * <p>
     * 너비(화소).
     */
    private final int width;

    /**
     * The height in pixels.
     * <p>
     * 높이(화소).
     */
    private final int height;

    /**
     * The bytes shared with the typed array.
     * <p>
     * 형식화 배열과 공유하는 바이트.
     */
    private final byte[] bytes;

    /**
     * The <code>Uint8ClampedArray</code> object that <code>data</code> returns, created once by the bindings.
     * <p>
     * <code>data</code> 가 반환하는 <code>Uint8ClampedArray</code> 객체. 연결 코드가 한 번 만든다.
     */
    private JsObject data;

    /**
     * Creates the value.
     * <p>
     * 값을 만든다.
     *
     * @param width  width in pixels / 너비(화소)
     * @param height height in pixels / 높이(화소)
     * @param bytes  the RGBA bytes (used, not copied) / RGBA 바이트(복사하지 않고 쓴다)
     */
    ImageDataValue(int width, int height, byte[] bytes) {
        this.width = width;
        this.height = height;
        this.bytes = bytes;
    }

    /**
     * Returns the width.
     * <p>
     * 너비를 반환한다.
     *
     * @return width in pixels / 너비(화소)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height.
     * <p>
     * 높이를 반환한다.
     *
     * @return height in pixels / 높이(화소)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the color space of the data, always <code>srgb</code>.
     * <p>
     * 데이터의 색 공간을 반환한다. 항상 <code>srgb</code>.
     *
     * @return the color space name / 색 공간 이름
     */
    public String getColorSpace() {
        return "srgb";
    }

    /**
     * Returns the bytes (not a copy).
     * <p>
     * 바이트를 반환한다(복사본이 아님).
     *
     * @return the RGBA bytes / RGBA 바이트
     */
    byte[] bytes() {
        return bytes;
    }

    /**
     * Returns the typed array object, or null before the bindings made it.
     * <p>
     * 형식화 배열 객체를 반환한다. 연결 코드가 만들기 전에는 null.
     *
     * @return the array / 배열
     */
    JsObject dataObject() {
        return data;
    }

    /**
     * Remembers the typed array object.
     * <p>
     * 형식화 배열 객체를 기억한다.
     *
     * @param array the array / 배열
     */
    void setDataObject(JsObject array) {
        this.data = array;
    }
}
