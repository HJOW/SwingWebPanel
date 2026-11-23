package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.nio.charset.Charset;

/**
 * Decodes the bytes of a style sheet to text, with the encoding detection of CSS Syntax Level 3: a byte order mark wins, then the charset of the
 * <code>Content-Type</code> header, then an <code>@charset "name";</code> rule at the very start, then the encoding of the referring document or sheet,
 * and finally UTF-8. A UTF-16 name in <code>@charset</code> is treated as UTF-8, as the specification says. Malformed input is replaced, never fatal.
 * <p>
 * 스타일 시트의 바이트를 CSS Syntax Level 3 의 인코딩 판정으로 텍스트로 디코딩한다: 바이트 순서 표시가 우선이고, 다음은 <code>Content-Type</code> 헤더의 charset, 맨 앞의
 * <code>@charset "name";</code> 규칙, 참조한 문서나 시트의 인코딩, 마지막으로 UTF-8 이다. <code>@charset</code> 의 UTF-16 이름은 규격대로 UTF-8 로 본다. 잘못된 입력은
 * 치환하며 치명적이지 않다.
 */
public final class StyleSheetDecoder {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private StyleSheetDecoder() {
    }

    /**
     * Decodes a style sheet.
     * <p>
     * 스타일 시트를 디코딩한다.
     *
     * @param bytes           the bytes / 바이트
     * @param httpCharset     the charset parameter of the Content-Type header or null / Content-Type 헤더의 charset 매개변수 또는 null
     * @param referrerCharset the encoding of the referring document or null / 참조한 문서의 인코딩 또는 null
     * @return the text / 텍스트
     */
    public static String decode(byte[] bytes, String httpCharset, String referrerCharset) {
        int skip = 0;
        Charset cs = null;
        if (bytes.length >= 3 && (bytes[0] & 255) == 0xEF && (bytes[1] & 255) == 0xBB && (bytes[2] & 255) == 0xBF) {
            cs = Charset.forName("UTF-8");
            skip = 3;
        } else if (bytes.length >= 2 && (bytes[0] & 255) == 0xFE && (bytes[1] & 255) == 0xFF) {
            cs = Charset.forName("UTF-16BE");
            skip = 2;
        } else if (bytes.length >= 2 && (bytes[0] & 255) == 0xFF && (bytes[1] & 255) == 0xFE) {
            cs = Charset.forName("UTF-16LE");
            skip = 2;
        }
        if (cs == null) {
            cs = lookup(httpCharset);
        }
        if (cs == null) {
            cs = atCharset(bytes);
        }
        if (cs == null) {
            cs = lookup(referrerCharset);
        }
        if (cs == null) {
            cs = Charset.forName("UTF-8");
        }
        return new String(bytes, skip, bytes.length - skip, cs);
    }

    /**
     * Finds a charset by name.
     * <p>
     * 이름으로 charset 을 찾는다.
     *
     * @param name the name or null / 이름 또는 null
     * @return the charset or null if unknown / charset. 모르면 null
     */
    private static Charset lookup(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        try {
            return Charset.isSupported(name.trim()) ? Charset.forName(name.trim()) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Reads an <code>@charset "name";</code> rule at the start of the bytes.
     * <p>
     * 바이트 맨 앞의 <code>@charset "name";</code> 규칙을 읽는다.
     *
     * @param b the bytes / 바이트
     * @return the charset (UTF-8 for UTF-16 names) or null / charset(UTF-16 이름이면 UTF-8). 없으면 null
     */
    private static Charset atCharset(byte[] b) {
        byte[] head = {'@', 'c', 'h', 'a', 'r', 's', 'e', 't', ' ', '"'};
        if (b.length < head.length + 2) {
            return null;
        }
        // 맨 앞이 정확히 @charset " 로 시작해야 한다.
        for (int i = 0; i < head.length; i++) {
            if (b[i] != head[i]) {
                return null;
            }
        }
        int i = head.length;
        StringBuilder name = new StringBuilder();
        // 닫는 따옴표까지 이름을 읽는다(너무 길면 중단).
        while (i < b.length && i < 1024 && b[i] != '"') {
            name.append((char) (b[i] & 255));
            i++;
        }
        if (i + 1 >= b.length || b[i] != '"' || b[i + 1] != ';') {
            return null;
        }
        String n = name.toString().toLowerCase();
        if (n.startsWith("utf-16") || n.startsWith("utf16")) {
            return Charset.forName("UTF-8");
        }
        return lookup(n);
    }
}
