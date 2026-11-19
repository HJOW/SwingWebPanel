package org.duckdns.hjow.swingwebpanel.dom.html;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.html.EncodingLabels;

/**
 * The three encodings of an entry list (HTML Standard 4.10.22.6 to 4.10.22.9 and the URL Standard's <code>application/x-www-form-urlencoded</code>
 * serializer): <code>application/x-www-form-urlencoded</code>, <code>multipart/form-data</code> and <code>text/plain</code>. All text is
 * encoded with the encoding of the form; a character that the encoding cannot represent becomes a decimal numeric character reference
 * (<code>&amp;#9731;</code>), which the URL and multipart encodings then escape like any other text. Line breaks of names and values are
 * normalized to CRLF first. A file entry is represented by its file name in the two text encodings.
 * <p>
 * 항목 목록의 세 가지 인코딩(HTML 표준 4.10.22.6~4.10.22.9 와 URL 표준의 <code>application/x-www-form-urlencoded</code> 직렬화기):
 * <code>application/x-www-form-urlencoded</code>, <code>multipart/form-data</code>, <code>text/plain</code>. 모든 글은 폼의 인코딩으로 인코딩하며, 인코딩이 표현할 수
 * 없는 문자는 십진 숫자 문자 참조(<code>&amp;#9731;</code>)가 되고 URL 과 multipart 인코딩은 이것을 다른 글처럼 이스케이프한다. 이름과 값의 줄 바꿈은 먼저
 * CRLF 로 정규화한다. 파일 항목은 두 텍스트 인코딩에서 파일 이름으로 표현한다.
 */
final class FormEncoders {

    /** The characters of a multipart boundary. / multipart 경계에 쓰는 문자. */
    private static final String BOUNDARY_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /** The source of boundary characters. / 경계 문자의 출처. */
    private static final Random RANDOM = new Random();

    /** Hexadecimal digits (upper case). / 16진수 문자(대문자). */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormEncoders() {
    }

    /**
     * Returns the Java charset for the name of an encoding of the Encoding Standard (UTF-8 if the JRE has none).
     * <p>
     * 인코딩 표준의 인코딩 이름에 해당하는 Java charset 을 반환한다(JRE 에 없으면 UTF-8).
     *
     * @param canonical the canonical encoding name / 표준 인코딩 이름
     * @return the charset / charset
     */
    static Charset charsetOf(String canonical) {
        String j = EncodingLabels.javaCharsetName(canonical);
        try {
            if (j != null) {
                return Charset.forName(j);
            }
            if (canonical != null && !EncodingLabels.REPLACEMENT.equals(canonical) && !EncodingLabels.X_USER_DEFINED.equals(canonical)) {
                return Charset.forName(canonical);
            }
        } catch (RuntimeException e) {
            // 알 수 없는 charset 이름이면 UTF-8 로 대신한다.
            return Charset.forName("UTF-8");
        }
        return Charset.forName("UTF-8");
    }

    /**
     * Encodes text with a charset: characters that the charset cannot represent (and lone surrogates) are written as decimal numeric
     * character references.
     * <p>
     * 글을 charset 으로 인코딩한다: charset 이 표현할 수 없는 문자(와 짝 없는 서로게이트)는 십진 숫자 문자 참조로 쓴다.
     *
     * @param text    the text / 글
     * @param charset the charset / charset
     * @return the bytes / 바이트
     */
    static byte[] encode(String text, Charset charset) {
        CharsetEncoder enc = charset.newEncoder();
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length() + 8);
        int i = 0;
        // 코드 포인트를 하나씩 인코딩하고 표현할 수 없으면 &#숫자; 로 쓴다.
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            String piece = text.substring(i, i + len);
            boolean lone = cp >= 0xD800 && cp <= 0xDFFF;
            if (cp < 0x80 && !"ISO-2022-JP".equalsIgnoreCase(charset.name())) {
                out.write(cp);
            } else if (!lone && enc.canEncode(piece)) {
                byte[] b = piece.getBytes(charset);
                out.write(b, 0, b.length);
            } else {
                byte[] ref = ("&#" + (lone ? 0xFFFD : cp) + ";").getBytes(asciiCharset());
                out.write(ref, 0, ref.length);
            }
            i += len;
        }
        return out.toByteArray();
    }

    /**
     * Returns the US-ASCII charset.
     * <p>
     * US-ASCII charset 을 반환한다.
     *
     * @return the charset / charset
     */
    private static Charset asciiCharset() {
        return Charset.forName("US-ASCII");
    }

    /**
     * Appends bytes to a stream.
     * <p>
     * 바이트를 스트림에 덧붙인다.
     *
     * @param out the stream / 스트림
     * @param b   the bytes / 바이트
     */
    private static void write(ByteArrayOutputStream out, byte[] b) {
        out.write(b, 0, b.length);
    }

    /**
     * Serializes pairs as <code>application/x-www-form-urlencoded</code>: <code>name=value</code> pairs joined with <code>&amp;</code>, every byte
     * except ASCII letters, digits and <code>*-._</code> percent-escaped, a space written as <code>+</code>.
     * <p>
     * 쌍을 <code>application/x-www-form-urlencoded</code> 로 직렬화한다: <code>&amp;</code> 로 이은 <code>name=value</code> 쌍. ASCII 영숫자와
     * <code>*-._</code> 를 뺀 모든 바이트는 퍼센트 이스케이프하고 공백은 <code>+</code> 로 쓴다.
     *
     * @param entries the entries (files are written as their names) / 항목(파일은 이름으로 쓴다)
     * @param charset the encoding / 인코딩
     * @return the ASCII bytes / ASCII 바이트
     */
    static byte[] urlEncoded(List<FormEntry> entries, Charset charset) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 항목마다 이름과 값을 퍼센트 인코딩해 & 로 잇는다.
        for (int i = 0; i < entries.size(); i++) {
            FormEntry e = entries.get(i);
            if (i > 0) {
                out.write('&');
            }
            percent(out, encode(HtmlText.toCrlf(e.getName()), charset));
            out.write('=');
            percent(out, encode(HtmlText.toCrlf(e.isFile() ? e.getFile().getName() : e.getValue()), charset));
        }
        return out.toByteArray();
    }

    /**
     * Writes bytes percent-escaped in the form-urlencoded way.
     * <p>
     * 바이트를 form-urlencoded 방식으로 퍼센트 이스케이프해 쓴다.
     *
     * @param out   the output / 출력
     * @param bytes the bytes / 바이트
     */
    private static void percent(ByteArrayOutputStream out, byte[] bytes) {
        // 바이트마다 허용 문자는 그대로, 공백은 +, 나머지는 %XX 로 쓴다.
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || b == '*' || b == '-' || b == '.' || b == '_') {
                out.write(b);
            } else if (b == ' ') {
                out.write('+');
            } else {
                out.write('%');
                out.write(HEX[b >> 4]);
                out.write(HEX[b & 15]);
            }
        }
    }

    /**
     * Serializes pairs as <code>text/plain</code>: <code>name=value</code> and a CRLF for every pair.
     * <p>
     * 쌍을 <code>text/plain</code> 으로 직렬화한다: 쌍마다 <code>name=value</code> 와 CRLF.
     *
     * @param entries the entries (files are written as their names) / 항목(파일은 이름으로 쓴다)
     * @param charset the encoding / 인코딩
     * @return the bytes / 바이트
     */
    static byte[] textPlain(List<FormEntry> entries, Charset charset) {
        StringBuilder b = new StringBuilder();
        // 항목마다 이름=값 과 CRLF 를 이어 붙인다.
        for (int i = 0; i < entries.size(); i++) {
            FormEntry e = entries.get(i);
            b.append(HtmlText.toCrlf(e.getName())).append('=').append(HtmlText.toCrlf(e.isFile() ? e.getFile().getName() : e.getValue()))
                    .append("\r\n");
        }
        return encode(b.toString(), charset);
    }

    /**
     * Creates a random multipart boundary.
     * <p>
     * 무작위 multipart 경계를 만든다.
     *
     * @return the boundary / 경계
     */
    static String newBoundary() {
        StringBuilder b = new StringBuilder("----SwingWebPanelFormBoundary");
        // 영문자와 숫자 16개를 무작위로 덧붙인다.
        for (int i = 0; i < 16; i++) {
            b.append(BOUNDARY_CHARS.charAt(RANDOM.nextInt(BOUNDARY_CHARS.length())));
        }
        return b.toString();
    }

    /**
     * Encodes an entry list as <code>multipart/form-data</code>: every entry is a part with a <code>Content-Disposition</code> header whose
     * name (and file name) is escaped (LF, CR and the double quote become <code>%0A</code>, <code>%0D</code>, <code>%22</code>), file parts have a
     * <code>Content-Type</code>, string parts have none.
     * <p>
     * 항목 목록을 <code>multipart/form-data</code> 로 인코딩한다: 항목마다 <code>Content-Disposition</code> 헤더가 있는 부분이며 이름(과 파일 이름)은 이스케이프한다
     * (LF, CR, 큰따옴표는 <code>%0A</code>, <code>%0D</code>, <code>%22</code>). 파일 부분에는 <code>Content-Type</code> 이 있고 문자열 부분에는 없다.
     *
     * @param entries  the entries / 항목
     * @param charset  the encoding / 인코딩
     * @param boundary the boundary / 경계
     * @return the body / 본문
     */
    static byte[] multipart(List<FormEntry> entries, Charset charset, String boundary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] crlf = new byte[] {'\r', '\n'};
        // 항목마다 경계 줄, 헤더, 빈 줄, 값, CRLF 순서로 쓴다.
        for (int i = 0; i < entries.size(); i++) {
            FormEntry e = entries.get(i);
            write(out, ("--" + boundary + "\r\n").getBytes(asciiCharset()));
            write(out, "Content-Disposition: form-data; name=\"".getBytes(asciiCharset()));
            write(out, escape(encode(HtmlText.toCrlf(e.getName()), charset)));
            out.write('"');
            if (e.isFile()) {
                FileEntry f = e.getFile();
                write(out, "; filename=\"".getBytes(asciiCharset()));
                write(out, escape(encode(f.getName(), charset)));
                out.write('"');
                write(out, crlf);
                String type = f.getType().length() == 0 ? "application/octet-stream" : f.getType();
                write(out, ("Content-Type: " + type + "\r\n\r\n").getBytes(asciiCharset()));
                write(out, f.contentRef());
            } else {
                write(out, crlf);
                write(out, crlf);
                write(out, encode(HtmlText.toCrlf(e.getValue()), charset));
            }
            write(out, crlf);
        }
        write(out, ("--" + boundary + "--\r\n").getBytes(asciiCharset()));
        return out.toByteArray();
    }

    /**
     * Escapes the bytes of a name or file name: LF, CR and the double quote are percent-encoded, nothing else.
     * <p>
     * 이름이나 파일 이름의 바이트를 이스케이프한다: LF, CR, 큰따옴표만 퍼센트 인코딩하고 다른 것은 그대로 둔다.
     *
     * @param b the bytes / 바이트
     * @return the escaped bytes / 이스케이프한 바이트
     */
    private static byte[] escape(byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length + 8);
        // 바이트마다 LF, CR, 큰따옴표를 %0A, %0D, %22 로 바꾼다.
        for (int i = 0; i < b.length; i++) {
            if (b[i] == '\n') {
                write(out, new byte[] {'%', '0', 'A'});
            } else if (b[i] == '\r') {
                write(out, new byte[] {'%', '0', 'D'});
            } else if (b[i] == '"') {
                write(out, new byte[] {'%', '2', '2'});
            } else {
                out.write(b[i]);
            }
        }
        return out.toByteArray();
    }
}
