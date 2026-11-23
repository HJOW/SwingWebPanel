package org.duckdns.hjow.swingwebpanel.core;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interim decoder that turns the received bytes of a document into the source text returned by <code>getContent()</code>.
 * Order of decisions: byte order mark, charset of the Content-Type header, <code>meta</code> charset in the first 1024 bytes, then
 * UTF-8 if the bytes are valid UTF-8 and windows-1252 otherwise. Line ends and spaces are never changed. The HTML layer of a later
 * stage (plan item P07-01) takes over with the complete encoding sniffing algorithm and the retained raw bytes.
 * <p>
 * 문서의 수신 바이트를 <code>getContent()</code> 가 반환하는 원문으로 바꾸는 임시 해독기. 판단 순서: 바이트 순서 표시, Content-Type 헤더의
 * 문자셋, 앞 1024 바이트의 <code>meta</code> 문자셋, 그다음 올바른 UTF-8 이면 UTF-8, 아니면 windows-1252. 줄 끝과 공백은 바꾸지 않는다.
 * 이후 단계의 HTML 계층(계획 P07-01)이 완전한 인코딩 판정 알고리즘과 보관한 원본 바이트로 이를 대체한다.
 */
final class TextDecoder {

    /**
     * Finds a charset declaration in a meta element.
     * <p>
     * meta 요소의 문자셋 선언을 찾는다.
     */
    private static final Pattern META_CHARSET = Pattern.compile("(?is)<meta[^>]+charset\\s*=\\s*[\"']?\\s*([A-Za-z0-9_\\-:.]+)");

    /**
     * Number of leading bytes searched for a meta declaration.
     * <p>
     * meta 선언을 찾는 앞쪽 바이트 수.
     */
    private static final int META_SCAN_BYTES = 1024;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TextDecoder() {
    }

    /**
     * Decodes document bytes.
     * <p>
     * 문서 바이트를 해독한다.
     *
     * @param data          received bytes, not null / 수신 바이트(null 불가)
     * @param headerCharset charset of the Content-Type header or null / Content-Type 헤더의 문자셋 또는 null
     * @return the source text / 원문
     */
    static String decode(byte[] data, String headerCharset) {
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return decodeWith(data, 3, data.length - 3, "UTF-8");
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            return decodeWith(data, 2, data.length - 2, "UTF-16BE");
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            return decodeWith(data, 2, data.length - 2, "UTF-16LE");
        }
        String cs = supported(headerCharset);
        if (cs == null) {
            cs = supported(metaCharset(data));
        }
        if (cs != null) {
            return decodeWith(data, 0, data.length, cs);
        }
        if (isValidUtf8(data)) {
            return decodeWith(data, 0, data.length, "UTF-8");
        }
        return decodeWith(data, 0, data.length, "windows-1252");
    }

    /**
     * Returns the charset name if it is supported by this JRE, otherwise null.
     * <p>
     * 이 JRE 가 지원하는 문자셋이면 이름을, 아니면 null 을 돌려준다.
     *
     * @param name charset name or null / 문자셋 이름 또는 null
     * @return supported name or null / 지원하는 이름 또는 null
     */
    private static String supported(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        try {
            return Charset.isSupported(name.trim()) ? name.trim() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Looks for a meta charset declaration in the first bytes.
     * <p>
     * 앞쪽 바이트에서 meta 문자셋 선언을 찾는다.
     *
     * @param data document bytes / 문서 바이트
     * @return the declared name or null / 선언된 이름 또는 null
     */
    private static String metaCharset(byte[] data) {
        int n = Math.min(data.length, META_SCAN_BYTES);
        String head = decodeWith(data, 0, n, "ISO-8859-1");
        Matcher m = META_CHARSET.matcher(head);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Tells whether the bytes are valid UTF-8.
     * <p>
     * 바이트가 올바른 UTF-8 인지 알려 준다.
     *
     * @param data bytes / 바이트
     * @return true if valid / 올바르면 true
     */
    private static boolean isValidUtf8(byte[] data) {
        try {
            Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * Decodes a byte range with a named charset, replacing malformed input.
     * <p>
     * 이름 있는 문자셋으로 바이트 범위를 해독하며 잘못된 입력은 대체 문자로 바꾼다.
     *
     * @param data    bytes / 바이트
     * @param offset  start offset / 시작 위치
     * @param length  number of bytes / 바이트 수
     * @param charset charset name / 문자셋 이름
     * @return decoded text / 해독한 문자열
     */
    private static String decodeWith(byte[] data, int offset, int length, String charset) {
        try {
            return new String(data, offset, length, charset);
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(data, offset, length, Charset.forName("UTF-8"));
        }
    }
}
