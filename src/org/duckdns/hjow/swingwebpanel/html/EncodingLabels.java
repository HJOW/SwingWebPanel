package org.duckdns.hjow.swingwebpanel.html;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Label and name handling of the Encoding Standard: it maps the labels that appear in HTTP headers and <code>meta</code> elements
 * (<code>utf8</code>, <code>euc-kr</code>, <code>ks_c_5601-1987</code>, ...) to the canonical encoding names
 * (<code>UTF-8</code>, <code>EUC-KR</code>, ...) using the generated table {@link EncodingLabelData}, and maps each canonical name to a
 * charset that the running JRE can decode with. The table is built once on first use.
 * <p>
 * Encoding 표준의 라벨과 이름 처리: HTTP 헤더와 <code>meta</code> 요소에 나오는 라벨(<code>utf8</code>, <code>euc-kr</code>,
 * <code>ks_c_5601-1987</code> 등)을 생성된 표 {@link EncodingLabelData} 로 표준 인코딩 이름(<code>UTF-8</code>, <code>EUC-KR</code> 등)에 대응시키고,
 * 각 표준 이름을 실행 중인 JRE 가 디코딩할 수 있는 charset 에 대응시킨다. 표는 처음 쓸 때 한 번 만든다.
 */
public final class EncodingLabels {

    /**
     * Canonical name of UTF-8.
     * <p>
     * UTF-8 의 표준 이름.
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * Canonical name of UTF-16 big endian.
     * <p>
     * UTF-16 빅 엔디언의 표준 이름.
     */
    public static final String UTF_16BE = "UTF-16BE";

    /**
     * Canonical name of UTF-16 little endian.
     * <p>
     * UTF-16 리틀 엔디언의 표준 이름.
     */
    public static final String UTF_16LE = "UTF-16LE";

    /**
     * Canonical name of windows-1252, the default encoding of this implementation.
     * <p>
     * 이 구현의 기본 인코딩인 windows-1252 의 표준 이름.
     */
    public static final String WINDOWS_1252 = "windows-1252";

    /**
     * Canonical name of the legacy single-byte encoding that maps bytes 0x80-0xFF to the private use area.
     * <p>
     * 바이트 0x80-0xFF 를 사설 영역으로 대응시키는 옛 단일 바이트 인코딩의 표준 이름.
     */
    public static final String X_USER_DEFINED = "x-user-defined";

    /**
     * Canonical name of the "replacement" encoding (decodes to one U+FFFD).
     * <p>
     * "replacement" 인코딩(U+FFFD 하나로 디코딩됨)의 표준 이름.
     */
    public static final String REPLACEMENT = "replacement";

    /**
     * Candidate Java charset names for each canonical name; the first candidate that the running JRE supports is used. Candidates of
     * one entry are separated by <code>|</code>.
     * <p>
     * 표준 이름마다의 Java charset 이름 후보. 실행 중인 JRE 가 지원하는 첫 후보를 쓴다. 한 항목의 후보는 <code>|</code> 로 구분한다.
     */
    private static final String[][] JAVA_NAMES = {
            {"UTF-8", "UTF-8"}, {"IBM866", "IBM866|Cp866"}, {"ISO-8859-2", "ISO-8859-2"}, {"ISO-8859-3", "ISO-8859-3"},
            {"ISO-8859-4", "ISO-8859-4"}, {"ISO-8859-5", "ISO-8859-5"}, {"ISO-8859-6", "ISO-8859-6"}, {"ISO-8859-7", "ISO-8859-7"},
            {"ISO-8859-8", "ISO-8859-8"}, {"ISO-8859-8-I", "ISO-8859-8"}, {"ISO-8859-10", "ISO-8859-10"}, {"ISO-8859-13", "ISO-8859-13"},
            {"ISO-8859-14", "ISO-8859-14"}, {"ISO-8859-15", "ISO-8859-15"}, {"ISO-8859-16", "ISO-8859-16"}, {"KOI8-R", "KOI8-R"},
            {"KOI8-U", "KOI8-U"}, {"macintosh", "x-MacRoman|MacRoman"}, {"windows-874", "x-windows-874|windows-874"},
            {"windows-1250", "windows-1250"}, {"windows-1251", "windows-1251"}, {"windows-1252", "windows-1252"},
            {"windows-1253", "windows-1253"}, {"windows-1254", "windows-1254"}, {"windows-1255", "windows-1255"},
            {"windows-1256", "windows-1256"}, {"windows-1257", "windows-1257"}, {"windows-1258", "windows-1258"},
            {"x-mac-cyrillic", "x-MacCyrillic|MacCyrillic"}, {"GBK", "GBK"}, {"gb18030", "GB18030"}, {"Big5", "Big5-HKSCS|Big5"},
            {"EUC-JP", "EUC-JP"}, {"ISO-2022-JP", "ISO-2022-JP"}, {"Shift_JIS", "windows-31j|Shift_JIS"},
            {"EUC-KR", "x-windows-949|EUC-KR"}, {"UTF-16BE", "UTF-16BE"}, {"UTF-16LE", "UTF-16LE"}};

    /**
     * Upper half (0x80-0x9F) of windows-1252 as the Encoding Standard defines it: the five bytes that Windows leaves undefined map to
     * the C1 control with the same value (the JRE would produce U+FFFD for them).
     * <p>
     * Encoding 표준이 정의한 windows-1252 의 윗부분(0x80-0x9F): Windows 가 정의하지 않은 다섯 바이트는 같은 값의 C1 제어 문자에 대응한다
     * (JRE 는 이 바이트에 U+FFFD 를 만든다).
     */
    private static final int[] WINDOWS_1252_HIGH = {0x20AC, 0x0081, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6, 0x2030, 0x0160,
            0x2039, 0x0152, 0x008D, 0x017D, 0x008F, 0x0090, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161,
            0x203A, 0x0153, 0x009D, 0x017E, 0x0178};

    /**
     * Label (lower case) to canonical name, built lazily.
     * <p>
     * 라벨(소문자)에서 표준 이름으로의 표. 처음 쓸 때 만든다.
     */
    private static Map<String, String> labelTable;

    /**
     * Canonical name to the supported Java charset name (null value: not supported by this JRE), built lazily.
     * <p>
     * 표준 이름에서 지원되는 Java charset 이름으로의 표(값이 null 이면 이 JRE 가 지원하지 않음). 처음 쓸 때 만든다.
     */
    private static Map<String, String> javaTable;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EncodingLabels() {
    }

    /**
     * Builds the tables on first use.
     * <p>
     * 처음 쓸 때 표를 만든다.
     */
    private static synchronized void init() {
        if (labelTable != null) {
            return;
        }
        Map<String, String> labels = new HashMap<String, String>();
        // 생성된 조각을 차례로 풀어 라벨 표를 채운다.
        for (int c = 0; c < EncodingLabelData.CHUNKS.length; c++) {
            String[] entries = EncodingLabelData.CHUNKS[c].split("\\|");
            // 항목마다 이름과 라벨 목록을 나눈다.
            for (int i = 0; i < entries.length; i++) {
                int eq = entries[i].indexOf('=');
                String name = entries[i].substring(0, eq);
                String[] ls = entries[i].substring(eq + 1).split(",");
                // 라벨마다 표준 이름을 대응시킨다.
                for (int k = 0; k < ls.length; k++) {
                    labels.put(ls[k], name);
                }
            }
        }
        Map<String, String> java = new HashMap<String, String>();
        // 표준 이름마다 JRE 가 지원하는 첫 후보를 고른다.
        for (int i = 0; i < JAVA_NAMES.length; i++) {
            String[] cands = JAVA_NAMES[i][1].split("\\|");
            String chosen = null;
            // 후보를 차례로 시험해 JRE 가 지원하는 첫 이름을 고른다.
            for (int k = 0; k < cands.length && chosen == null; k++) {
                if (isSupported(cands[k])) {
                    chosen = cands[k];
                }
            }
            java.put(JAVA_NAMES[i][0], chosen);
        }
        javaTable = java;
        labelTable = labels;
    }

    /**
     * Tells whether the running JRE supports a charset name, without throwing.
     * <p>
     * 실행 중인 JRE 가 charset 이름을 지원하는지 예외 없이 알려 준다.
     *
     * @param name charset name / charset 이름
     * @return true if supported / 지원하면 true
     */
    private static boolean isSupported(String name) {
        try {
            return Charset.isSupported(name);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Trims ASCII white space from both ends of a label and lower-cases it (ASCII only).
     * <p>
     * 라벨 양 끝의 ASCII 공백을 없애고 소문자로 바꾼다(ASCII 만).
     *
     * @param label label / 라벨
     * @return normalized label / 정규화된 라벨
     */
    private static String normalizeLabel(String label) {
        int s = 0;
        int e = label.length();
        // 앞뒤의 ASCII 공백(탭, 줄바꿈, 폼피드, 캐리지리턴, 공백)을 건너뛴다.
        while (s < e && isAsciiWhitespace(label.charAt(s))) {
            s++;
        }
        // 뒤쪽의 공백을 하나씩 줄여 끝 위치를 맞춘다.
        while (e > s && isAsciiWhitespace(label.charAt(e - 1))) {
            e--;
        }
        StringBuilder sb = new StringBuilder(e - s);
        // 대문자 ASCII 만 소문자로 바꾼다(로케일의 영향을 받지 않도록 직접 변환한다).
        for (int i = s; i < e; i++) {
            char c = label.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    /**
     * Tells whether a character is ASCII white space (tab, LF, FF, CR, space).
     * <p>
     * 문자가 ASCII 공백(탭, LF, FF, CR, 공백)인지 알려 준다.
     *
     * @param c character / 문자
     * @return true for white space / 공백이면 true
     */
    private static boolean isAsciiWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Returns the canonical encoding name for a label ("get an encoding"), ignoring ASCII case and surrounding ASCII white space.
     * <p>
     * 라벨에 대한 표준 인코딩 이름을 반환한다("get an encoding"). ASCII 대소문자와 앞뒤 ASCII 공백은 무시한다.
     *
     * @param label label such as <code>utf8</code> or <code>EUC-KR</code>, may be null / <code>utf8</code>, <code>EUC-KR</code> 같은 라벨, null 가능
     * @return canonical name (for example <code>UTF-8</code>), or null if the label is unknown / 표준 이름(예: <code>UTF-8</code>), 알 수 없는 라벨이면 null
     */
    public static String canonicalName(String label) {
        if (label == null) {
            return null;
        }
        init();
        return labelTable.get(normalizeLabel(label));
    }

    /**
     * Returns the name of a Java charset that decodes a canonical encoding, or null when the JRE has none (or the encoding is decoded
     * by this implementation itself: <code>x-user-defined</code>, <code>windows-1252</code>, <code>replacement</code>).
     * <p>
     * 표준 인코딩을 디코딩하는 Java charset 이름을 반환한다. JRE 에 없거나 이 구현이 직접 디코딩하는 인코딩(<code>x-user-defined</code>,
     * <code>windows-1252</code>, <code>replacement</code>)이면 null.
     *
     * @param canonical canonical name / 표준 이름
     * @return Java charset name or null / Java charset 이름 또는 null
     */
    public static String javaCharsetName(String canonical) {
        init();
        return javaTable.get(canonical);
    }

    /**
     * Tells whether this JRE can decode a canonical encoding (through a Java charset or this class's own decoders).
     * <p>
     * 이 JRE 가 표준 인코딩을 디코딩할 수 있는지(Java charset 또는 이 클래스의 자체 디코더로) 알려 준다.
     *
     * @param canonical canonical name / 표준 이름
     * @return true if text in that encoding can be decoded / 그 인코딩의 텍스트를 디코딩할 수 있으면 true
     */
    public static boolean isDecodable(String canonical) {
        if (canonical == null) {
            return false;
        }
        if (X_USER_DEFINED.equals(canonical) || WINDOWS_1252.equals(canonical) || REPLACEMENT.equals(canonical)) {
            return true;
        }
        return javaCharsetName(canonical) != null;
    }

    /**
     * Decodes bytes in a canonical encoding. Malformed or unmappable input becomes U+FFFD, as the Encoding Standard requires; no
     * exception is thrown. An encoding that this JRE cannot decode is decoded as windows-1252.
     * <p>
     * 표준 인코딩의 바이트를 디코딩한다. Encoding 표준대로 잘못되었거나 대응할 수 없는 입력은 U+FFFD 가 되며 예외는 던지지 않는다. 이 JRE 가
     * 디코딩할 수 없는 인코딩은 windows-1252 로 디코딩한다.
     *
     * @param bytes     bytes / 바이트
     * @param offset    first byte to decode / 디코딩할 첫 바이트의 위치
     * @param canonical canonical name / 표준 이름
     * @return decoded text / 디코딩된 문자열
     */
    public static String decode(byte[] bytes, int offset, String canonical) {
        int len = bytes.length - offset;
        if (X_USER_DEFINED.equals(canonical)) {
            StringBuilder sb = new StringBuilder(len);
            // 0x80 이상의 바이트는 사설 영역 U+F780 부터에 대응시킨다.
            for (int i = offset; i < bytes.length; i++) {
                int b = bytes[i] & 0xFF;
                sb.append((char) (b < 0x80 ? b : 0xF780 + (b - 0x80)));
            }
            return sb.toString();
        }
        if (REPLACEMENT.equals(canonical)) {
            return len == 0 ? "" : String.valueOf((char) 0xFFFD);
        }
        String java = canonical == null ? null : javaCharsetName(canonical);
        if (java == null || WINDOWS_1252.equals(canonical)) {
            return decodeWindows1252(bytes, offset);
        }
        try {
            return new String(bytes, offset, len, java);
        } catch (java.io.UnsupportedEncodingException e) {
            return decodeWindows1252(bytes, offset);
        }
    }

    /**
     * Decodes windows-1252 exactly as the Encoding Standard defines it.
     * <p>
     * Encoding 표준이 정의한 그대로 windows-1252 를 디코딩한다.
     *
     * @param bytes  bytes / 바이트
     * @param offset first byte / 첫 바이트의 위치
     * @return decoded text / 디코딩된 문자열
     */
    private static String decodeWindows1252(byte[] bytes, int offset) {
        char[] out = new char[bytes.length - offset];
        // 바이트마다 0x80-0x9F 는 표에서, 나머지는 같은 값의 문자로 바꾼다.
        for (int i = offset; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i - offset] = (char) (b >= 0x80 && b < 0xA0 ? WINDOWS_1252_HIGH[b - 0x80] : b);
        }
        return new String(out);
    }
}
