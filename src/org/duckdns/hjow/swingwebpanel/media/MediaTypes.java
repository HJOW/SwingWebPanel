package org.duckdns.hjow.swingwebpanel.media;

import java.util.Locale;

/**
 * Helpers for MIME type strings used by the media decoders: removes parameters, white space and letter case so that a Content-Type value
 * such as <code>Image/PNG; charset=binary</code> can be compared with <code>image/png</code>.
 * <p>
 * 미디어 디코더가 쓰는 MIME 타입 문자열 도우미: 매개변수, 공백, 대소문자를 제거해 <code>Image/PNG; charset=binary</code> 같은 Content-Type
 * 값을 <code>image/png</code> 와 비교할 수 있게 한다.
 */
public final class MediaTypes {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private MediaTypes() {
    }

    /**
     * Returns the lower-case <code>type/subtype</code> part of a Content-Type value, or null if there is none.
     * <p>
     * Content-Type 값에서 소문자 <code>type/subtype</code> 부분을 반환하며 없으면 null 이다.
     *
     * @param contentType Content-Type value, may be null / Content-Type 값. null 가능
     * @return normalized type, or null for null/blank/malformed input / 정규화된 타입. null/빈 값/잘못된 값이면 null
     */
    public static String normalize(String contentType) {
        if (contentType == null) {
            return null;
        }
        String s = contentType;
        int semi = s.indexOf(';');
        if (semi >= 0) {
            s = s.substring(0, semi);
        }
        s = s.trim().toLowerCase(Locale.ENGLISH);
        // type 과 subtype 이 모두 있어야 올바른 값이다.
        int slash = s.indexOf('/');
        if (slash <= 0 || slash == s.length() - 1) {
            return null;
        }
        return s;
    }
}
