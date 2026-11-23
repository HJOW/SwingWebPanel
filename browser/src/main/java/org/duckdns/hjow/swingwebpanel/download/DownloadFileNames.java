package org.duckdns.hjow.swingwebpanel.download;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;

/**
 * Chooses the file name of a download and makes it safe to use as a file name on every operating system (plan item P11-09). The name comes
 * from the <code>filename*</code> (RFC 8187/6266, with charset) or <code>filename</code> parameter of the Content-Disposition header, then
 * from the last segment of the URL path, then falls back to <code>download</code>. A server can never choose a directory: path
 * separators, drive letters, control and format characters (including the right-to-left override), characters that Windows forbids,
 * trailing dots and spaces and the reserved device names of Windows are removed or replaced, and the length is limited.
 * <p>
 * 다운로드의 파일 이름을 정하고 모든 운영체제에서 파일 이름으로 안전하게 쓸 수 있게 만든다(계획 P11-09). 이름은 Content-Disposition 헤더의
 * <code>filename*</code>(RFC 8187/6266, 문자셋 포함) 또는 <code>filename</code> 매개변수에서, 없으면 URL 경로의 마지막 구간에서 얻고, 그것도
 * 없으면 <code>download</code> 로 한다. 서버가 디렉터리를 고를 수는 없다: 경로 구분자, 드라이브 문자, 제어/서식 문자(오른쪽에서 왼쪽 덮어쓰기
 * 포함), Windows 가 금지한 문자, 끝의 마침표와 공백, Windows 예약 장치 이름은 제거하거나 바꾸고 길이를 제한한다.
 */
public final class DownloadFileNames {

    /** Name used when nothing usable is left. 쓸 만한 이름이 남지 않았을 때 쓰는 이름. */
    public static final String FALLBACK = "download";

    /** Maximum length of a chosen name in characters. 정한 이름의 최대 길이(문자 수). */
    public static final int MAX_LENGTH = 150;

    /** Reserved device names of Windows (upper case). Windows 예약 장치 이름(대문자). */
    private static final String[] RESERVED = { "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8",
            "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9" };

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private DownloadFileNames() {
    }

    /**
     * Chooses the name of a download.
     * <p>
     * 다운로드의 이름을 정한다.
     *
     * @param contentDisposition raw Content-Disposition header value or null / Content-Disposition 헤더의 원래 값 또는 null
     * @param url                the final URL of the download / 다운로드의 최종 URL
     * @return a safe, non-empty file name without any directory part / 디렉터리 부분이 없는 안전하고 비어 있지 않은 파일 이름
     */
    public static String choose(String contentDisposition, String url) {
        String header = fromContentDisposition(contentDisposition);
        String safe = header == null ? null : sanitize(header);
        // 헤더의 이름이 없거나 쓸 수 없는 이름(예: ..)이면 URL 경로의 마지막 구간을 쓴다.
        if (safe == null || (safe.equals(FALLBACK) && !FALLBACK.equals(header))) {
            String fromUrl = fromUrl(url);
            if (fromUrl != null) {
                safe = sanitize(fromUrl);
            } else if (safe == null) {
                safe = FALLBACK;
            }
        }
        return safe;
    }

    /**
     * Makes a proposed name safe: see the class description.
     * <p>
     * 제안된 이름을 안전하게 만든다. 클래스 설명을 참조한다.
     *
     * @param proposed the proposed name, may contain anything / 제안된 이름(무엇이든 들어 있을 수 있다)
     * @return a safe, non-empty file name / 안전하고 비어 있지 않은 파일 이름
     */
    public static String sanitize(String proposed) {
        String s = proposed == null ? "" : proposed;
        int cut = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (cut >= 0) {
            s = s.substring(cut + 1);
        }
        StringBuilder sb = new StringBuilder(s.length());
        // 문자마다 제어/서식 문자와 Windows 에서 금지된 문자를 밑줄로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int type = Character.getType(c);
            if (c < 0x20 || c == 0x7F || type == Character.CONTROL || type == Character.FORMAT || "<>:\"|?*".indexOf(c) >= 0) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        int end = sb.length();
        // 끝의 마침표와 공백은 Windows 가 지우므로 미리 지운다.
        while (end > 0 && (sb.charAt(end - 1) == '.' || sb.charAt(end - 1) == ' ')) {
            end--;
        }
        sb.setLength(end);
        int start = 0;
        // 앞의 공백은 지운다.
        while (start < sb.length() && sb.charAt(start) == ' ') {
            start++;
        }
        String name = sb.substring(start);
        if (name.length() == 0 || isAllDots(name)) {
            return FALLBACK;
        }
        name = limitLength(name);
        if (isReserved(name)) {
            name = "_" + name;
        }
        return name;
    }

    /**
     * Tells whether a name consists of dots only.
     * <p>
     * 이름이 마침표로만 이루어졌는지 알려 준다.
     *
     * @param s the name / 이름
     * @return true if only dots / 마침표뿐이면 true
     */
    private static boolean isAllDots(String s) {
        // 마침표가 아닌 문자가 하나라도 있으면 아니다.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Shortens a long name but keeps its extension.
     * <p>
     * 긴 이름을 줄이되 확장자는 유지한다.
     *
     * @param name the name / 이름
     * @return name of at most {@link #MAX_LENGTH} characters / 최대 {@link #MAX_LENGTH} 자의 이름
     */
    private static String limitLength(String name) {
        if (name.length() <= MAX_LENGTH) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String ext = dot > 0 && name.length() - dot <= 16 ? name.substring(dot) : "";
        int keep = MAX_LENGTH - ext.length();
        // 서로게이트 쌍의 가운데에서 자르지 않는다.
        if (keep > 0 && Character.isHighSurrogate(name.charAt(keep - 1))) {
            keep--;
        }
        return name.substring(0, keep) + ext;
    }

    /**
     * Tells whether the part of a name before its first dot is a reserved device name of Windows.
     * <p>
     * 이름에서 첫 마침표 앞 부분이 Windows 예약 장치 이름인지 알려 준다.
     *
     * @param name the name / 이름
     * @return true if reserved / 예약 이름이면 true
     */
    private static boolean isReserved(String name) {
        int dot = name.indexOf('.');
        String base = (dot < 0 ? name : name.substring(0, dot)).trim().toUpperCase(Locale.ENGLISH);
        // 예약 이름 목록과 비교한다.
        for (int i = 0; i < RESERVED.length; i++) {
            if (RESERVED[i].equals(base)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the file name of a Content-Disposition header value.
     * <p>
     * Content-Disposition 헤더 값에서 파일 이름을 읽는다.
     *
     * @param header the header value or null / 헤더 값 또는 null
     * @return the raw file name or null if there is none / 원래 파일 이름. 없으면 null
     */
    static String fromContentDisposition(String header) {
        if (header == null) {
            return null;
        }
        String extended = null;
        String plain = null;
        int i = header.indexOf(';');
        // 세미콜론으로 구분된 매개변수를 하나씩 읽는다.
        while (i >= 0 && i < header.length()) {
            i++;
            int eq = header.indexOf('=', i);
            if (eq < 0) {
                break;
            }
            String key = header.substring(i, eq).trim().toLowerCase(Locale.ENGLISH);
            int[] end = new int[1];
            String value = readValue(header, eq + 1, end);
            if ("filename*".equals(key) && extended == null) {
                extended = decodeExtended(value);
            } else if ("filename".equals(key) && plain == null) {
                plain = repairUtf8(value);
            }
            i = header.indexOf(';', end[0]);
        }
        if (extended != null && extended.length() > 0) {
            return extended;
        }
        return plain != null && plain.length() > 0 ? plain : null;
    }

    /**
     * Reads a parameter value (a quoted string with backslash escapes or a token).
     * <p>
     * 매개변수 값(백슬래시 이스케이프가 있는 따옴표 문자열 또는 토큰)을 읽는다.
     *
     * @param s     the header / 헤더
     * @param from  index after the equals sign / 등호 다음 위치
     * @param endAt receives the index after the value / 값 다음 위치를 받는다
     * @return the value / 값
     */
    private static String readValue(String s, int from, int[] endAt) {
        int p = from;
        // 값 앞의 공백을 건너뛴다.
        while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == '\t')) {
            p++;
        }
        StringBuilder sb = new StringBuilder();
        if (p < s.length() && s.charAt(p) == '"') {
            p++;
            // 닫는 따옴표까지 읽는다. 백슬래시는 따옴표와 백슬래시 앞에서만 이스케이프로 보고 그 외에는 경로 구분자로 남긴다.
            while (p < s.length() && s.charAt(p) != '"') {
                if (s.charAt(p) == '\\' && p + 1 < s.length() && (s.charAt(p + 1) == '"' || s.charAt(p + 1) == '\\')) {
                    p++;
                }
                sb.append(s.charAt(p));
                p++;
            }
            if (p < s.length()) {
                p++;
            }
        } else {
            // 따옴표 없는 값은 세미콜론 전까지이다.
            while (p < s.length() && s.charAt(p) != ';') {
                sb.append(s.charAt(p));
                p++;
            }
        }
        endAt[0] = p;
        return sb.toString().trim();
    }

    /**
     * Decodes an extended parameter value <code>charset'language'percent-encoded</code>.
     * <p>
     * 확장 매개변수 값 <code>charset'language'percent-encoded</code> 를 해독한다.
     *
     * @param value the value / 값
     * @return the decoded name, or null if it is malformed / 해독한 이름. 형식이 잘못되었으면 null
     */
    private static String decodeExtended(String value) {
        int q1 = value.indexOf('\'');
        int q2 = q1 < 0 ? -1 : value.indexOf('\'', q1 + 1);
        if (q2 < 0) {
            return null;
        }
        String charset = value.substring(0, q1).trim();
        String encoded = value.substring(q2 + 1);
        byte[] bytes = percentDecode(encoded);
        if (bytes == null) {
            return null;
        }
        try {
            Charset cs = Charset.forName(charset.length() == 0 ? "UTF-8" : charset);
            return cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Decodes percent escapes to bytes.
     * <p>
     * 퍼센트 이스케이프를 바이트로 해독한다.
     *
     * @param s the encoded text / 인코딩된 텍스트
     * @return the bytes, or null on a malformed escape / 바이트. 이스케이프가 잘못되었으면 null
     */
    static byte[] percentDecode(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(s.length());
        // 문자마다 퍼센트 이스케이프는 바이트로 풀고 나머지는 ASCII 로 취급한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%') {
                if (i + 2 >= s.length()) {
                    return null;
                }
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    return null;
                }
                out.write(hi * 16 + lo);
                i += 2;
            } else if (c < 0x80) {
                out.write(c);
            } else {
                try {
                    byte[] b = String.valueOf(c).getBytes("UTF-8");
                    out.write(b, 0, b.length);
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
        }
        return out.toByteArray();
    }

    /**
     * Repairs a plain file name whose UTF-8 bytes were read one byte per character (as Java does for header values).
     * <p>
     * 헤더 값을 Java 가 하듯 바이트 하나를 문자 하나로 읽는 바람에 UTF-8 바이트가 망가진 일반 파일 이름을 되살린다.
     *
     * @param s the name as read / 읽은 이름
     * @return the repaired name, or the same name if it is not such a case / 되살린 이름. 그런 경우가 아니면 같은 이름
     */
    private static String repairUtf8(String s) {
        boolean high = false;
        // 모든 문자가 0xFF 이하이고 0x80 이상이 있을 때만 후보이다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 0xFF) {
                return s;
            }
            if (c >= 0x80) {
                high = true;
            }
        }
        if (!high) {
            return s;
        }
        try {
            byte[] raw = s.getBytes("ISO-8859-1");
            return Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException e) {
            return s;
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    /**
     * Takes the last segment of the path of a URL, percent-decoded as UTF-8.
     * <p>
     * URL 경로의 마지막 구간을 UTF-8 퍼센트 해독하여 얻는다.
     *
     * @param url the URL / URL
     * @return the segment, or null if the path ends with a slash or is empty / 구간. 경로가 슬래시로 끝나거나 비었으면 null
     */
    static String fromUrl(String url) {
        if (url == null) {
            return null;
        }
        String u = url;
        int cut = u.indexOf('#');
        if (cut >= 0) {
            u = u.substring(0, cut);
        }
        cut = u.indexOf('?');
        if (cut >= 0) {
            u = u.substring(0, cut);
        }
        int scheme = u.indexOf("://");
        int pathStart = scheme < 0 ? 0 : u.indexOf('/', scheme + 3);
        if (pathStart < 0) {
            return null;
        }
        String path = u.substring(pathStart);
        int slash = path.lastIndexOf('/');
        String last = path.substring(slash + 1);
        if (last.length() == 0) {
            return null;
        }
        byte[] bytes = percentDecode(last);
        if (bytes == null) {
            return last;
        }
        try {
            return Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return last;
        }
    }
}
