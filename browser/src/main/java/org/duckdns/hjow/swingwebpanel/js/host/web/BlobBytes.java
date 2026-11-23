package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * Read access to the bytes of a script <code>Blob</code> (or <code>File</code>) for the other host APIs that take a blob as an input, for example
 * <code>createImageBitmap(blob)</code> of the canvas package. The <code>Blob</code> object itself stays private to the web request package; this
 * class only answers whether a value is a blob and gives its immutable bytes and type.
 * <p>
 * 스크립트 <code>Blob</code>(또는 <code>File</code>)의 바이트를 읽는 통로. 블롭을 입력으로 받는 다른 호스트 API(예: 캔버스 패키지의
 * <code>createImageBitmap(blob)</code>)를 위한 것이다. <code>Blob</code> 객체 자체는 웹 요청 패키지 안에 두고, 이 클래스는 값이 블롭인지와 바뀌지 않는
 * 바이트, 타입만 알려 준다.
 */
public final class BlobBytes {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BlobBytes() {
    }

    /**
     * Tells whether a value is a <code>Blob</code> or a <code>File</code>.
     * <p>
     * 값이 <code>Blob</code> 이나 <code>File</code> 인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a blob / 블롭이면 true
     */
    public static boolean isBlob(JsValue v) {
        return v instanceof BlobApi.BlobObject;
    }

    /**
     * Returns the bytes of a blob; the array is shared and must not be changed.
     * <p>
     * 블롭의 바이트를 반환한다. 배열은 공유되며 바꾸면 안 된다.
     *
     * @param v the value / 값
     * @return the bytes, or null if the value is not a blob / 바이트. 블롭이 아니면 null
     */
    public static byte[] bytes(JsValue v) {
        return v instanceof BlobApi.BlobObject ? ((BlobApi.BlobObject) v).data : null;
    }

    /**
     * Returns the MIME type of a blob (lower case, or an empty string).
     * <p>
     * 블롭의 MIME 타입을 반환한다(소문자 또는 빈 문자열).
     *
     * @param v the value / 값
     * @return the type, or null if the value is not a blob / 타입. 블롭이 아니면 null
     */
    public static String type(JsValue v) {
        return v instanceof BlobApi.BlobObject ? ((BlobApi.BlobObject) v).type : null;
    }
}
