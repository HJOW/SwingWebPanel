package sourcecheck;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.List;

/**
 * File encoding rules of the project: java/md/sh files must be strict UTF-8 without a byte order mark (sh files also without
 * carriage returns); bat files must be MS949 (code page 949) with CRLF line ends and must not contain UTF-8 Korean text.
 * <p>
 * 프로젝트의 파일 인코딩 규칙: java/md/sh 파일은 BOM 없는 엄격한 UTF-8 이어야 하며(sh 는 캐리지 리턴도 없어야 함),
 * bat 파일은 CRLF 줄 끝의 MS949(코드 페이지 949)여야 하고 UTF-8 한국어가 섞이면 안 된다.
 */
final class EncodingChecks {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EncodingChecks() {
    }

    /**
     * Result of decoding a byte array as UTF-8.
     * <p>
     * 바이트 배열을 UTF-8 로 디코딩한 결과.
     */
    static final class Utf8Result {
        /**
         * Decoded text, or null if the bytes are not valid UTF-8.
         * <p>
         * 디코딩된 텍스트. 올바른 UTF-8 이 아니면 null.
         */
        final String text;

        /**
         * Byte offset of the first error, or -1.
         * <p>
         * 첫 오류의 바이트 위치. 오류가 없으면 -1.
         */
        final int errorOffset;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param text        decoded text or null / 디코딩된 텍스트 또는 null
         * @param errorOffset error offset or -1 / 오류 위치 또는 -1
         */
        Utf8Result(String text, int errorOffset) {
            this.text = text;
            this.errorOffset = errorOffset;
        }
    }

    /**
     * Strictly decodes bytes as UTF-8, reporting the offset of the first malformed sequence.
     * <p>
     * 바이트를 엄격하게 UTF-8 로 디코딩하고 처음 잘못된 시퀀스의 위치를 알려 준다.
     *
     * @param data bytes / 바이트
     * @return decoding result / 디코딩 결과
     */
    static Utf8Result decodeUtf8(byte[] data) {
        CharsetDecoder dec = Charset.forName("UTF-8").newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPORT);
        dec.onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(data);
        CharBuffer cb = CharBuffer.allocate(data.length + 1);
        CoderResult r = dec.decode(in, cb, true);
        if (r.isError()) {
            return new Utf8Result(null, in.position());
        }
        r = dec.flush(cb);
        if (r.isError()) {
            return new Utf8Result(null, in.position());
        }
        cb.flip();
        return new Utf8Result(cb.toString(), -1);
    }

    /**
     * Counts the line number (1-based) of a byte offset.
     * <p>
     * 바이트 위치의 줄 번호(1 기반)를 센다.
     *
     * @param data   bytes / 바이트
     * @param offset byte offset / 바이트 위치
     * @return line number / 줄 번호
     */
    static int lineOfOffset(byte[] data, int offset) {
        int line = 1;
        // 위치 앞의 줄바꿈 개수를 센다.
        for (int i = 0; i < offset && i < data.length; i++) {
            if (data[i] == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * Counts the line number (1-based) of a character index in decoded text.
     * <p>
     * 디코딩된 텍스트에서 문자 위치의 줄 번호(1 기반)를 센다.
     *
     * @param text  decoded text / 디코딩된 텍스트
     * @param index character index / 문자 위치
     * @return line number / 줄 번호
     */
    static int lineOfChar(String text, int index) {
        int line = 1;
        // 위치 앞의 줄바꿈 개수를 센다.
        for (int i = 0; i < index && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * Tests whether a character is invisible or easily confused with a normal character (no-break and exotic spaces,
     * zero-width and bidi controls, line/paragraph separators, byte order mark, C0/C1 controls other than tab, LF, CR, FF).
     * <p>
     * 문자가 보이지 않거나 일반 문자와 혼동되기 쉬운지 확인한다(줄 바꿈 없는 공백과 특수 공백, 폭 없는 문자와 방향 제어 문자,
     * 줄/문단 구분자, 바이트 순서 표시, 탭/LF/CR/FF 를 제외한 C0/C1 제어 문자).
     *
     * @param c character / 문자
     * @return true if it should not appear raw in source / 소스에 원문자로 있으면 안 되면 true
     */
    static boolean isInvisible(char c) {
        if (c < 0x20) {
            return c != '\t' && c != '\n' && c != '\r' && c != '\f';
        }
        return c == 0x7F || (c >= 0x80 && c <= 0x9F) || c == 0xA0 || c == 0xAD || c == 0x1680
                || (c >= 0x2000 && c <= 0x200F) || (c >= 0x2028 && c <= 0x202F) || (c >= 0x205F && c <= 0x2064)
                || c == 0x3000 || c == 0xFEFF;
    }

    /**
     * Checks a java/md/sh file. Returns the decoded text when the bytes are valid UTF-8 so callers can scan it.
     * <p>
     * java/md/sh 파일을 검사한다. 올바른 UTF-8 이면 디코딩한 텍스트를 반환하여 호출자가 이어서 검사할 수 있게 한다.
     *
     * @param path display path / 표시 경로
     * @param data file bytes / 파일 바이트
     * @param ext  lower-case extension without dot / 점 없는 소문자 확장자
     * @param out  list receiving violations / 위반을 받을 목록
     * @return decoded text or null / 디코딩한 텍스트 또는 null
     */
    static String checkUtf8Text(String path, byte[] data, String ext, List<Violation> out) {
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            out.add(new Violation(path, 1, "bom", "UTF-8 byte order mark is not allowed"));
        }
        Utf8Result r = decodeUtf8(data);
        if (r.text == null) {
            out.add(new Violation(path, lineOfOffset(data, r.errorOffset), "utf8", "invalid UTF-8 byte sequence at offset " + r.errorOffset));
            return null;
        }
        int bad = r.text.indexOf((char) 0xFFFD);
        if (bad >= 0) {
            out.add(new Violation(path, lineOfChar(r.text, bad), "replacement-char", "contains U+FFFD (a previous bad conversion?)"));
        }
        if ("java".equals(ext) || "sh".equals(ext)) {
            int reported = 0;
            // 보이지 않거나 혼동되는 문자(도구가 \\u 이스케이프를 원문자로 바꾼 경우 등)를 찾는다.
            for (int i = 0; i < r.text.length() && reported < 5; i++) {
                char c = r.text.charAt(i);
                if (isInvisible(c) && !(i == 0 && c == (char) 0xFEFF)) {
                    String hex = Integer.toHexString(c).toUpperCase();
                    // 16진수 표기가 네 자리가 되도록 앞에 0 을 채운다.
                    while (hex.length() < 4) {
                        hex = "0" + hex;
                    }
                    out.add(new Violation(path, lineOfChar(r.text, i), "invisible-char",
                            "invisible or confusable character U+" + hex + " in source; write it as (char) 0x" + hex + " instead"));
                    reported++;
                }
            }
        }
        if ("sh".equals(ext)) {
            // sh 파일에 캐리지 리턴이 있으면 셸이 오동작하므로 첫 위치를 알려 준다.
            for (int i = 0; i < data.length; i++) {
                if (data[i] == '\r') {
                    out.add(new Violation(path, lineOfOffset(data, i), "sh-crlf", "sh files must use LF line ends only"));
                    break;
                }
            }
        }
        return r.text;
    }

    /**
     * Checks a bat file: MS949 only, CRLF line ends, no UTF-8 Korean text.
     * <p>
     * bat 파일을 검사한다: MS949 만, CRLF 줄 끝, UTF-8 한국어 금지.
     *
     * @param path display path / 표시 경로
     * @param data file bytes / 파일 바이트
     * @param out  list receiving violations / 위반을 받을 목록
     */
    static void checkBat(String path, byte[] data, List<Violation> out) {
        boolean high = false;
        // 비 ASCII 바이트가 하나라도 있는지 확인한다.
        for (int i = 0; i < data.length; i++) {
            if ((data[i] & 0x80) != 0) {
                high = true;
                break;
            }
        }
        if (high) {
            Utf8Result u = decodeUtf8(data);
            if (u.text != null) {
                out.add(new Violation(path, 1, "bat-utf8", "bat file contains UTF-8 text (must be MS949)"));
            } else {
                CharsetDecoder dec = Charset.forName("x-windows-949").newDecoder();
                dec.onMalformedInput(CodingErrorAction.REPORT);
                dec.onUnmappableCharacter(CodingErrorAction.REPORT);
                ByteBuffer in = ByteBuffer.wrap(data);
                CoderResult r = dec.decode(in, CharBuffer.allocate(data.length + 1), true);
                if (r.isError()) {
                    out.add(new Violation(path, lineOfOffset(data, in.position()), "bat-ms949", "bytes are not valid MS949"));
                }
            }
        }
        // 줄바꿈 앞에 캐리지 리턴이 없는 곳을 찾는다.
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n' && (i == 0 || data[i - 1] != '\r')) {
                out.add(new Violation(path, lineOfOffset(data, i), "bat-crlf", "bat files must use CRLF line ends"));
                break;
            }
        }
    }
}
