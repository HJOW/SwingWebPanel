package org.duckdns.hjow.swingwebpanel.tls13;

/**
 * Hexadecimal text for the test vectors of the TLS module. Example: <code>byte[] key = Hex.decode("00 01 02");</code>.
 * <p>
 * TLS 모듈 시험 벡터용 16진수 문자열. 사용 예: <code>byte[] key = Hex.decode("00 01 02");</code>.
 */
public final class Hex {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Hex() {
    }

    /**
     * Decodes hexadecimal text; white space and colons between digits are ignored.
     * <p>
     * 16진수 문자열을 바이트로 바꾼다. 숫자 사이의 공백과 콜론은 무시한다.
     *
     * @param text the text / 문자열
     * @return the bytes / 바이트
     */
    public static byte[] decode(String text) {
        StringBuilder digits = new StringBuilder();
        // 16진 숫자만 모은다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.digit(c, 16) >= 0) {
                digits.append(c);
            } else if (!Character.isWhitespace(c) && c != ':') {
                throw new IllegalArgumentException("not a hex digit: " + c);
            }
        }
        if (digits.length() % 2 != 0) {
            throw new IllegalArgumentException("odd number of hex digits");
        }
        byte[] out = new byte[digits.length() / 2];
        // 두 글자씩 한 바이트로 만든다.
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(digits.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }

    /**
     * Encodes bytes as lower case hexadecimal text without separators.
     * <p>
     * 바이트를 구분자 없는 소문자 16진수 문자열로 바꾼다.
     *
     * @param data the bytes / 바이트
     * @return the text / 문자열
     */
    public static String encode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        // 바이트마다 두 글자를 쓴다.
        for (int i = 0; i < data.length; i++) {
            sb.append(Character.forDigit((data[i] >> 4) & 15, 16)).append(Character.forDigit(data[i] & 15, 16));
        }
        return sb.toString();
    }
}
