package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Byte and text conversions of the web request APIs: the UTF-8 encoder that turns lone surrogates into U+FFFD, the UTF-8 decoders ("UTF-8
 * decode" with and without BOM), the decoding of response text by the charset of the response, percent coding for
 * <code>application/x-www-form-urlencoded</code>, and a small Base64 decoder (the Java 6 JRE has no public Base64 class) for the
 * subresource-integrity check. Pure functions; no state.
 * <p>
 * 웹 요청 API의 바이트·텍스트 변환: 짝 없는 서로게이트를 U+FFFD 로 바꾸는 UTF-8 인코더, UTF-8 디코더(BOM 처리 여부별 "UTF-8 decode"), 응답 문자셋에 따른
 * 응답 텍스트 디코딩, <code>application/x-www-form-urlencoded</code> 의 퍼센트 코딩, 하위 리소스 무결성 검사용 작은 Base64 디코더(Java 6 JRE 에는 공개
 * Base64 클래스가 없다). 상태 없는 순수 함수이다.
 */
final class Encodings {

    /** The hexadecimal digits in upper case. / 대문자 16진수 숫자. */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Encodings() {
    }

    /**
     * Encodes a string as UTF-8; unpaired surrogates become U+FFFD first.
     * <p>
     * 문자열을 UTF-8 로 인코딩한다. 짝 없는 서로게이트는 먼저 U+FFFD 가 된다.
     *
     * @param s the string / 문자열
     * @return the bytes / 바이트
     */
    static byte[] utf8(String s) {
        try {
            return Natives.usv(s).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decodes bytes as UTF-8 without removing a BOM; malformed bytes become U+FFFD.
     * <p>
     * 바이트를 BOM 을 제거하지 않고 UTF-8 로 디코딩한다. 잘못된 바이트는 U+FFFD 가 된다.
     *
     * @param b the bytes / 바이트
     * @return the text / 텍스트
     */
    static String utf8Raw(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The "UTF-8 decode" of the Encoding Standard: a leading BOM is removed, malformed bytes become U+FFFD.
     * <p>
     * Encoding 표준의 "UTF-8 decode": 앞의 BOM 을 제거하고 잘못된 바이트는 U+FFFD 가 된다.
     *
     * @param b the bytes / 바이트
     * @return the text / 텍스트
     */
    static String utf8Decode(byte[] b) {
        int skip = b.length >= 3 && (b[0] & 255) == 0xEF && (b[1] & 255) == 0xBB && (b[2] & 255) == 0xBF ? 3 : 0;
        try {
            return new String(b, skip, b.length - skip, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decodes response bytes with the charset of the response (a byte order mark wins), falling back to UTF-8 for a missing or unknown
     * charset label.
     * <p>
     * 응답 바이트를 응답의 문자셋으로 디코딩한다(바이트 순서 표시가 우선). 문자셋 레이블이 없거나 모르면 UTF-8 이다.
     *
     * @param b       the bytes / 바이트
     * @param charset the charset label or null / 문자셋 레이블 또는 null
     * @return the text / 텍스트
     */
    static String decode(byte[] b, String charset) {
        String name = "UTF-8";
        int skip = 0;
        // 바이트 순서 표시가 있으면 선언된 문자셋보다 우선한다.
        if (b.length >= 3 && (b[0] & 255) == 0xEF && (b[1] & 255) == 0xBB && (b[2] & 255) == 0xBF) {
            skip = 3;
        } else if (b.length >= 2 && (b[0] & 255) == 0xFE && (b[1] & 255) == 0xFF) {
            name = "UTF-16BE";
            skip = 2;
        } else if (b.length >= 2 && (b[0] & 255) == 0xFF && (b[1] & 255) == 0xFE) {
            name = "UTF-16LE";
            skip = 2;
        } else if (charset != null && charset.length() > 0) {
            try {
                if (Charset.isSupported(charset)) {
                    name = charset;
                }
            } catch (IllegalArgumentException e) {
                name = "UTF-8";
            }
        }
        try {
            return new String(b, skip, b.length - skip, name);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The percent-encode set of <code>application/x-www-form-urlencoded</code>: the characters that stay as they are.
     * <p>
     * <code>application/x-www-form-urlencoded</code> 의 퍼센트 인코딩 집합: 그대로 두는 문자.
     *
     * @param c a byte value / 바이트 값
     * @return true for ASCII letters, digits and <code>*-._</code> / ASCII 영문자, 숫자, <code>*-._</code> 이면 true
     */
    private static boolean formSafe(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '*' || c == '-' || c == '.' || c == '_';
    }

    /**
     * Serializes a string with the <code>application/x-www-form-urlencoded</code> byte serializer.
     * <p>
     * 문자열을 <code>application/x-www-form-urlencoded</code> 바이트 직렬화기로 직렬화한다.
     *
     * @param s the string / 문자열
     * @return the encoded text / 인코딩한 텍스트
     */
    static String formEncode(String s) {
        byte[] b = utf8(s);
        StringBuilder out = new StringBuilder(b.length + 8);
        // 바이트마다 안전한 문자는 그대로, 공백은 '+', 나머지는 %XX 로 쓴다.
        for (int i = 0; i < b.length; i++) {
            int c = b[i] & 255;
            if (c == ' ') {
                out.append('+');
            } else if (formSafe(c)) {
                out.append((char) c);
            } else {
                out.append('%').append(HEX[c >> 4]).append(HEX[c & 15]);
            }
        }
        return out.toString();
    }

    /**
     * Decodes one component of <code>application/x-www-form-urlencoded</code> data: '+' is a space, <code>%XX</code> is a byte, the result is
     * decoded as UTF-8 (a malformed percent escape stays as it is).
     * <p>
     * <code>application/x-www-form-urlencoded</code> 데이터의 한 구성요소를 디코딩한다. '+' 는 공백, <code>%XX</code> 는 바이트이며 결과는 UTF-8 로
     * 디코딩한다(잘못된 퍼센트 이스케이프는 그대로 둔다).
     *
     * @param s the encoded component / 인코딩된 구성요소
     * @return the decoded text / 디코딩한 텍스트
     */
    static String formDecode(String s) {
        byte[] raw = utf8(s);
        byte[] out = new byte[raw.length];
        int n = 0;
        // 바이트를 하나씩 보며 '+' 와 %XX 를 풀어 출력 배열에 모은다.
        for (int i = 0; i < raw.length; i++) {
            int c = raw[i] & 255;
            if (c == '+') {
                out[n++] = ' ';
            } else if (c == '%' && i + 2 < raw.length && hex(raw[i + 1]) >= 0 && hex(raw[i + 2]) >= 0) {
                out[n++] = (byte) (hex(raw[i + 1]) * 16 + hex(raw[i + 2]));
                i += 2;
            } else {
                out[n++] = raw[i];
            }
        }
        byte[] done = new byte[n];
        System.arraycopy(out, 0, done, 0, n);
        return utf8Raw(done);
    }

    /**
     * Returns the value of a hexadecimal digit.
     * <p>
     * 16진수 숫자의 값을 반환한다.
     *
     * @param c the ASCII byte / ASCII 바이트
     * @return 0..15, or -1 if it is not a digit / 0..15. 숫자가 아니면 -1
     */
    private static int hex(byte c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /**
     * Parses <code>application/x-www-form-urlencoded</code> text into (name, value) pairs; empty sequences are skipped.
     * <p>
     * <code>application/x-www-form-urlencoded</code> 텍스트를 (이름, 값) 쌍으로 해석한다. 빈 조각은 건너뛴다.
     *
     * @param input the text without a leading '?' / 앞의 '?' 가 없는 텍스트
     * @return the pairs / 쌍 목록
     */
    static List<String[]> formParse(String input) {
        List<String[]> out = new ArrayList<String[]>();
        int start = 0;
        // '&' 로 나눈 조각마다 첫 '=' 앞은 이름, 뒤는 값으로 본다.
        while (start <= input.length()) {
            int amp = input.indexOf('&', start);
            String piece = amp < 0 ? input.substring(start) : input.substring(start, amp);
            if (piece.length() > 0) {
                int eq = piece.indexOf('=');
                String name = eq < 0 ? piece : piece.substring(0, eq);
                String value = eq < 0 ? "" : piece.substring(eq + 1);
                out.add(new String[] { formDecode(name), formDecode(value) });
            }
            if (amp < 0) {
                break;
            }
            start = amp + 1;
        }
        return out;
    }

    /**
     * Serializes (name, value) pairs as <code>application/x-www-form-urlencoded</code>.
     * <p>
     * (이름, 값) 쌍을 <code>application/x-www-form-urlencoded</code> 로 직렬화한다.
     *
     * @param pairs the pairs / 쌍 목록
     * @return the text / 텍스트
     */
    static String formSerialize(List<String[]> pairs) {
        StringBuilder out = new StringBuilder();
        // 쌍마다 이름=값 을 '&' 로 이어 붙인다.
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0) {
                out.append('&');
            }
            out.append(formEncode(pairs.get(i)[0])).append('=').append(formEncode(pairs.get(i)[1]));
        }
        return out.toString();
    }

    /**
     * Decodes standard Base64 text (the alphabet with '+' and '/', optional '=' padding, whitespace ignored).
     * <p>
     * 표준 Base64 텍스트를 디코딩한다('+' 와 '/' 알파벳, 선택적인 '=' 패딩, 공백 무시).
     *
     * @param text the Base64 text / Base64 텍스트
     * @return the bytes, or null if the text is not valid Base64 / 바이트. Base64 가 아니면 null
     */
    static byte[] base64Decode(String text) {
        byte[] out = new byte[text.length() * 3 / 4 + 3];
        int n = 0;
        int bits = 0;
        int count = 0;
        int pad = 0;
        // 문자를 하나씩 보며 6비트씩 모아 8비트가 차면 바이트로 내보낸다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if (c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if (c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if (c == '+' || c == '-') {
                v = 62;
            } else if (c == '/' || c == '_') {
                v = 63;
            } else if (c == '=') {
                pad++;
                continue;
            } else if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                continue;
            } else {
                return null;
            }
            if (pad > 0) {
                return null;
            }
            bits = (bits << 6) | v;
            count++;
            if (count == 4) {
                out[n++] = (byte) (bits >> 16);
                out[n++] = (byte) (bits >> 8);
                out[n++] = (byte) bits;
                bits = 0;
                count = 0;
            }
        }
        if (count == 2) {
            out[n++] = (byte) (bits >> 4);
        } else if (count == 3) {
            out[n++] = (byte) (bits >> 10);
            out[n++] = (byte) (bits >> 2);
        } else if (count == 1) {
            return null;
        }
        byte[] done = new byte[n];
        System.arraycopy(out, 0, done, 0, n);
        return done;
    }

    /**
     * Encodes bytes as standard Base64 with padding (no line breaks).
     * <p>
     * 바이트를 패딩이 있는 표준 Base64 로 인코딩한다(줄바꿈 없음).
     *
     * @param data the bytes / 바이트
     * @return the Base64 text / Base64 텍스트
     */
    static String base64Encode(byte[] data) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder out = new StringBuilder((data.length + 2) / 3 * 4);
        // 세 바이트마다 네 개의 6비트 값으로 나누어 문자로 바꾼다. 모자라는 자리는 '=' 로 채운다.
        for (int i = 0; i < data.length; i += 3) {
            int b0 = data[i] & 255;
            int b1 = i + 1 < data.length ? data[i + 1] & 255 : 0;
            int b2 = i + 2 < data.length ? data[i + 2] & 255 : 0;
            out.append(alphabet.charAt(b0 >> 2));
            out.append(alphabet.charAt(((b0 & 3) << 4) | (b1 >> 4)));
            out.append(i + 1 < data.length ? alphabet.charAt(((b1 & 15) << 2) | (b2 >> 6)) : '=');
            out.append(i + 2 < data.length ? alphabet.charAt(b2 & 63) : '=');
        }
        return out.toString();
    }
}
