package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import org.duckdns.hjow.swingwebpanel.html.EncodingLabels;
import org.duckdns.hjow.swingwebpanel.html.EncodingSniffer;

/**
 * Decodes the bytes of an XML or XHTML document (plan item P17-05). The order of the decision follows XML 1.0 Appendix F and
 * RFC 7303 with the usual browser behaviour:
 * <ol>
 * <li>a byte order mark (UTF-8, UTF-16LE, UTF-16BE) wins;</li>
 * <li>otherwise the <code>charset</code> parameter of the transport layer (HTTP Content-Type), if the label is a known and decodable
 * encoding;</li>
 * <li>otherwise a UTF-16 document without byte order mark is recognized by its first bytes (<code>3C 00 3F 00</code>,
 * <code>00 3C 00 3F</code>);</li>
 * <li>otherwise the <code>encoding</code> of the XML declaration (a UTF-16 label on bytes that are ASCII compatible is ignored and UTF-8
 * is used, as the HTML Standard does for such a label);</li>
 * <li>otherwise UTF-8, the default of XML.</li>
 * </ol>
 * Unlike HTML, malformed byte sequences are <em>not</em> replaced by U+FFFD: an XML document that is not valid in its encoding is not
 * well-formed, so the result carries an error with the line and column where the text stops. Labels are matched by the Encoding
 * Standard table of {@link EncodingLabels} (<code>euc-kr</code>, <code>windows-949</code>, <code>utf8</code> and so on).
 * <p>
 * XML/XHTML 문서의 바이트를 디코딩한다(계획 P17-05). 판정 순서는 XML 1.0 부록 F 와 RFC 7303 에 일반적인 브라우저 동작을 더한 것이다.
 * <ol>
 * <li>바이트 순서 표시(UTF-8, UTF-16LE, UTF-16BE)가 이긴다;</li>
 * <li>없으면 전송 계층(HTTP Content-Type)의 <code>charset</code> 매개변수. 라벨이 알려진, 디코딩 가능한 인코딩일 때;</li>
 * <li>없으면 바이트 순서 표시가 없는 UTF-16 문서를 첫 바이트(<code>3C 00 3F 00</code>, <code>00 3C 00 3F</code>)로 알아본다;</li>
 * <li>없으면 XML 선언의 <code>encoding</code>(ASCII 호환 바이트에 UTF-16 라벨이 붙었으면 무시하고 UTF-8 을 쓴다. HTML 표준이 그런 라벨을
 * 다루는 방식과 같다);</li>
 * <li>없으면 XML 의 기본값 UTF-8.</li>
 * </ol>
 * HTML 과 달리 잘못된 바이트 순서를 U+FFFD 로 바꾸지 <em>않는다</em>: 인코딩에 맞지 않는 XML 문서는 well-formed 가 아니므로 결과가 텍스트가 멈춘
 * 줄과 열이 있는 오류를 가진다. 라벨은 {@link EncodingLabels} 의 Encoding 표준 표로 맞춘다(<code>euc-kr</code>, <code>windows-949</code>,
 * <code>utf8</code> 등).
 */
public final class XmlDecoder {

    /**
     * Number of bytes that are looked at for the XML declaration.
     * <p>
     * XML 선언을 찾으려고 살피는 바이트 수.
     */
    private static final int DECLARATION_PEEK = 512;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private XmlDecoder() {
    }

    /**
     * Decodes the bytes of an XML document.
     * <p>
     * XML 문서의 바이트를 디코딩한다.
     *
     * @param bytes       the document bytes, null is treated as empty / 문서 바이트(null 은 빈 배열)
     * @param httpCharset the charset label of the Content-Type header, or null / Content-Type 헤더의 charset 라벨 또는 null
     * @return the decoded text with the decision / 판정이 담긴 디코딩 결과
     */
    public static XmlText decode(byte[] bytes, String httpCharset) {
        byte[] b = bytes == null ? new byte[0] : bytes;
        String bom = EncodingSniffer.bomEncoding(b);
        if (bom != null) {
            return decodeWith(b, EncodingSniffer.bomLength(b), bom, XmlText.SOURCE_BOM);
        }
        String http = EncodingLabels.canonicalName(httpCharset);
        if (http != null && !EncodingLabels.REPLACEMENT.equals(http) && EncodingLabels.isDecodable(http)) {
            return decodeWith(b, 0, http, XmlText.SOURCE_HTTP);
        }
        if (b.length >= 4 && b[0] == 0x3C && b[1] == 0 && b[2] == 0x3F && b[3] == 0) {
            return decodeWith(b, 0, EncodingLabels.UTF_16LE, XmlText.SOURCE_AUTODETECT);
        }
        if (b.length >= 4 && b[0] == 0 && b[1] == 0x3C && b[2] == 0 && b[3] == 0x3F) {
            return decodeWith(b, 0, EncodingLabels.UTF_16BE, XmlText.SOURCE_AUTODETECT);
        }
        String declared = declaredEncoding(b);
        if (declared != null) {
            return decodeWith(b, 0, declared, XmlText.SOURCE_DECLARATION);
        }
        return decodeWith(b, 0, EncodingLabels.UTF_8, XmlText.SOURCE_DEFAULT);
    }

    /**
     * Reads the <code>encoding</code> of the XML declaration at the start of ASCII compatible bytes.
     * <p>
     * ASCII 호환 바이트의 앞에 있는 XML 선언의 <code>encoding</code> 을 읽는다.
     *
     * @param b the bytes / 바이트
     * @return the canonical encoding name, or null if there is none or it cannot be used / 표준 인코딩 이름. 없거나 쓸 수 없으면 null
     */
    private static String declaredEncoding(byte[] b) {
        int n = Math.min(b.length, DECLARATION_PEEK);
        StringBuilder sb = new StringBuilder(n);
        // 앞부분 바이트를 한 바이트씩 문자로 바꾼다(선언은 ASCII 이다).
        for (int i = 0; i < n; i++) {
            sb.append((char) (b[i] & 0xFF));
        }
        String label = XmlProlog.scan(sb.toString()).getXmlEncoding();
        String canonical = EncodingLabels.canonicalName(label);
        if (canonical == null || EncodingLabels.REPLACEMENT.equals(canonical) || !EncodingLabels.isDecodable(canonical)) {
            return null;
        }
        // ASCII 호환 바이트에 UTF-16 라벨이 붙은 경우는 UTF-8 로 본다.
        if (EncodingLabels.UTF_16LE.equals(canonical) || EncodingLabels.UTF_16BE.equals(canonical)) {
            return EncodingLabels.UTF_8;
        }
        return canonical;
    }

    /**
     * Decodes bytes in a given encoding, strictly where the JRE offers a decoder that can report errors.
     * <p>
     * 주어진 인코딩으로 바이트를 디코딩한다. 오류를 보고할 수 있는 JRE 디코더가 있으면 엄격하게 디코딩한다.
     *
     * @param b         the bytes / 바이트
     * @param offset    index of the first byte to decode (after a byte order mark) / 디코딩할 첫 바이트의 위치(바이트 순서 표시 뒤)
     * @param canonical canonical encoding name / 표준 인코딩 이름
     * @param source    source constant / 출처 상수
     * @return the result / 결과
     */
    private static XmlText decodeWith(byte[] b, int offset, String canonical, String source) {
        String java = EncodingLabels.javaCharsetName(canonical);
        if (java == null || !Charset.isSupported(java) || EncodingLabels.WINDOWS_1252.equals(canonical)) {
            // 이 구현이 직접 디코딩하는 인코딩은 잘못된 바이트가 없다(U+FFFD 도 나오지 않는다).
            return new XmlText(EncodingLabels.decode(b, offset, canonical), canonical, source, null, 0, 0);
        }
        CharsetDecoder dec = Charset.forName(java).newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(b, offset, b.length - offset);
        CharBuffer out = CharBuffer.allocate((int) Math.min(Integer.MAX_VALUE - 16, (long) (in.remaining() * (double) dec.maxCharsPerByte()) + 16));
        CoderResult r = dec.decode(in, out, true);
        if (!r.isError()) {
            r = dec.flush(out);
        }
        if (r.isOverflow()) {
            // 버퍼를 넉넉히 잡았으므로 일어나지 않지만, 일어나면 오류로 알린다.
            return failure(out, canonical, source, "The decoded text is too large", in.position());
        }
        if (r.isError()) {
            String why = r.isMalformed() ? "Invalid byte sequence for " + canonical : "Unmappable byte sequence for " + canonical;
            return failure(out, canonical, source, why, in.position());
        }
        out.flip();
        return new XmlText(out.toString(), canonical, source, null, 0, 0);
    }

    /**
     * Builds the result for a decoding error: the text up to the error and the position of the error.
     * <p>
     * 디코딩 오류의 결과를 만든다: 오류 직전까지의 텍스트와 오류의 위치.
     *
     * @param out       buffer with the characters decoded so far / 지금까지 디코딩한 문자가 든 버퍼
     * @param canonical encoding name / 인코딩 이름
     * @param source    source constant / 출처 상수
     * @param why       description / 설명
     * @param byteIndex index of the first bad byte / 첫 잘못된 바이트의 위치
     * @return the result / 결과
     */
    private static XmlText failure(CharBuffer out, String canonical, String source, String why, int byteIndex) {
        out.flip();
        String text = out.toString();
        int line = 1;
        int column = 1;
        // 오류 직전까지의 텍스트에서 줄과 열을 센다(줄 바꿈은 LF, CR LF, CR 모두 한 줄 바꿈이다).
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' && (i + 1 >= text.length() || text.charAt(i + 1) != '\n')) {
                line++;
                column = 1;
            } else if (c != '\r') {
                column++;
            }
        }
        return new XmlText(text, canonical, source, why + " at byte offset " + byteIndex, line, column);
    }
}
