package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link XmlDecoder} (plan item P17-05): the order of the encoding decision (byte order mark, transport charset, UTF-16 without
 * byte order mark, XML declaration, default), the labels of the Encoding Standard, strict handling of malformed bytes and the
 * integration with {@link XhtmlParser}.
 * <p>
 * {@link XmlDecoder} 의 시험(계획 P17-05): 인코딩 판정 순서(바이트 순서 표시, 전송 계층 charset, 바이트 순서 표시 없는 UTF-16, XML 선언, 기본값),
 * Encoding 표준의 라벨, 잘못된 바이트의 엄격한 처리, {@link XhtmlParser} 와의 통합.
 */
public class XmlDecoderTest {

    /**
     * The Korean word "Hangul" used as non-ASCII content.
     * <p>
     * ASCII 가 아닌 내용으로 쓰는 한글 단어.
     */
    private static final String KOREAN = "한글";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XmlDecoderTest() {
    }

    /**
     * Encodes a text.
     * <p>
     * 텍스트를 인코딩한다.
     *
     * @param text    the text / 텍스트
     * @param charset the Java charset name / Java charset 이름
     * @param prefix  bytes in front of the text (for example a byte order mark) / 텍스트 앞의 바이트(예: 바이트 순서 표시)
     * @return the bytes / 바이트
     * @throws IOException if the charset is unknown / charset 을 모르면
     */
    private static byte[] bytes(String text, String charset, int... prefix) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 앞에 붙일 바이트를 먼저 쓴다.
        for (int i = 0; i < prefix.length; i++) {
            out.write(prefix[i]);
        }
        out.write(text.getBytes(charset));
        return out.toByteArray();
    }

    /**
     * Checks one decoding.
     * <p>
     * 디코딩 하나를 확인한다.
     *
     * @param t        the result / 결과
     * @param text     the expected text / 기대하는 텍스트
     * @param encoding the expected canonical encoding / 기대하는 표준 인코딩
     * @param source   the expected source of the decision / 기대하는 판정 출처
     */
    private static void check(XmlText t, String text, String encoding, String source) {
        Check.isNull(t.getErrorMessage(), "no error");
        Check.equal(text, t.getText(), "text");
        Check.equal(encoding, t.getEncoding(), "encoding");
        Check.equal(source, t.getSource(), "source of the decision");
    }

    /**
     * A byte order mark decides, is not part of the text and beats the transport charset and the declaration.
     * <p>
     * 바이트 순서 표시가 판정하고, 텍스트에 포함되지 않으며, 전송 계층 charset 과 선언보다 우선한다.
     *
     * @throws Exception if encoding fails / 인코딩에 실패하면
     */
    public void testByteOrderMarks() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><a>" + KOREAN + "</a>";
        check(XmlDecoder.decode(bytes(xml, "UTF-8", 0xEF, 0xBB, 0xBF), "euc-kr"), xml, "UTF-8", XmlText.SOURCE_BOM);
        check(XmlDecoder.decode(bytes(xml, "UTF-16LE", 0xFF, 0xFE), null), xml, "UTF-16LE", XmlText.SOURCE_BOM);
        check(XmlDecoder.decode(bytes(xml, "UTF-16BE", 0xFE, 0xFF), "utf-8"), xml, "UTF-16BE", XmlText.SOURCE_BOM);
    }

    /**
     * UTF-16 without byte order mark is recognized by the first bytes of <code>&lt;?</code>.
     * <p>
     * 바이트 순서 표시가 없는 UTF-16 은 <code>&lt;?</code> 의 첫 바이트로 알아본다.
     *
     * @throws Exception if encoding fails / 인코딩에 실패하면
     */
    public void testUtf16WithoutByteOrderMark() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><a>" + KOREAN + "</a>";
        check(XmlDecoder.decode(bytes(xml, "UTF-16LE"), null), xml, "UTF-16LE", XmlText.SOURCE_AUTODETECT);
        check(XmlDecoder.decode(bytes(xml, "UTF-16BE"), null), xml, "UTF-16BE", XmlText.SOURCE_AUTODETECT);
    }

    /**
     * The transport charset beats the declaration; an unknown or unusable label is ignored; the declaration is used otherwise.
     * <p>
     * 전송 계층 charset 이 선언보다 우선하고, 모르거나 쓸 수 없는 라벨은 무시하며, 그 밖에는 선언을 쓴다.
     *
     * @throws Exception if encoding fails / 인코딩에 실패하면
     */
    public void testTransportCharsetAndDeclaration() throws Exception {
        String declared = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>" + KOREAN + "</a>";
        check(XmlDecoder.decode(bytes(declared, "EUC-KR"), "euc-kr"), declared, "EUC-KR", XmlText.SOURCE_HTTP);
        check(XmlDecoder.decode(bytes(declared, "EUC-KR"), "windows-949"), declared, "EUC-KR", XmlText.SOURCE_HTTP);
        String euc = "<?xml version='1.0' encoding='EUC-KR'?><a>" + KOREAN + "</a>";
        check(XmlDecoder.decode(bytes(euc, "EUC-KR"), null), euc, "EUC-KR", XmlText.SOURCE_DECLARATION);
        check(XmlDecoder.decode(bytes(euc, "EUC-KR"), "no-such-charset"), euc, "EUC-KR", XmlText.SOURCE_DECLARATION);
        check(XmlDecoder.decode(bytes(euc, "EUC-KR"), "replacement"), euc, "EUC-KR", XmlText.SOURCE_DECLARATION);
        String latin = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a>" + (char) 0xE9 + "</a>";
        check(XmlDecoder.decode(bytes(latin, "ISO-8859-1"), null), latin, "windows-1252", XmlText.SOURCE_DECLARATION);
        // 바이트 순서 표시가 없고 ASCII 호환인 바이트에 UTF-16 라벨이 붙은 경우는 UTF-8 로 본다.
        String ascii16 = "<?xml version=\"1.0\" encoding=\"utf-16\"?><a>abc</a>";
        check(XmlDecoder.decode(bytes(ascii16, "UTF-8"), null), ascii16, "UTF-8", XmlText.SOURCE_DECLARATION);
    }

    /**
     * Without any information the default of XML, UTF-8, is used; empty and null input are fine.
     * <p>
     * 정보가 하나도 없으면 XML 의 기본값 UTF-8 을 쓰며, 빈 입력과 null 도 문제없다.
     *
     * @throws Exception if encoding fails / 인코딩에 실패하면
     */
    public void testDefaults() throws Exception {
        String xml = "<a>" + KOREAN + "</a>";
        check(XmlDecoder.decode(bytes(xml, "UTF-8"), null), xml, "UTF-8", XmlText.SOURCE_DEFAULT);
        check(XmlDecoder.decode(new byte[0], null), "", "UTF-8", XmlText.SOURCE_DEFAULT);
        check(XmlDecoder.decode(null, null), "", "UTF-8", XmlText.SOURCE_DEFAULT);
        check(XmlText.ofString("abc"), "abc", "UTF-8", XmlText.SOURCE_STRING);
        check(XmlText.ofString(null), "", "UTF-8", XmlText.SOURCE_STRING);
    }

    /**
     * Malformed bytes are not replaced by U+FFFD: the text stops before them and the result tells line and column.
     * <p>
     * 잘못된 바이트는 U+FFFD 로 바뀌지 않는다: 텍스트가 그 앞에서 멈추고 결과가 줄과 열을 알려 준다.
     *
     * @throws Exception if encoding fails / 인코딩에 실패하면
     */
    public void testMalformedBytes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("<a>\n<b>x".getBytes("UTF-8"));
        out.write(0xC3);
        out.write(0x28);
        out.write("</b></a>".getBytes("UTF-8"));
        XmlText t = XmlDecoder.decode(out.toByteArray(), null);
        Check.isTrue(t.hasError(), "malformed UTF-8 is an error");
        Check.equal("<a>\n<b>x", t.getText(), "the text stops before the malformed bytes");
        Check.equal(2, t.getErrorLine(), "line");
        Check.equal(5, t.getErrorColumn(), "column of the first malformed byte");
        Check.contains(t.getErrorMessage(), "UTF-8", "message names the encoding");
        XmlText euc = XmlDecoder.decode(new byte[] {'<', 'a', '>', (byte) 0xB0}, "euc-kr");
        Check.isTrue(euc.hasError(), "a lone lead byte of EUC-KR is an error");
        Check.equal("<a>", euc.getText(), "text before the error");
        XmlText lone = XmlDecoder.decode(new byte[] {'<', 'a', '/', '>', (byte) 0xFF}, null);
        Check.isTrue(lone.hasError(), "0xFF is never valid in UTF-8");
    }

    /**
     * The parser decodes the bytes, reports the encoding in the document, and turns a decoding error into an {@link XmlParseError} of
     * the kind {@link XmlParseError.Kind#ENCODING} (never a silent replacement).
     * <p>
     * 파서는 바이트를 디코딩하고 문서에 인코딩을 알리며, 디코딩 오류를 {@link XmlParseError.Kind#ENCODING} 종류의 {@link XmlParseError} 로 만든다(조용한
     * 대체는 없다).
     *
     * @throws Exception if encoding fails / 인코딩에 실패하면
     */
    public void testParserIntegration() throws Exception {
        String euc = "<?xml version=\"1.0\" encoding=\"EUC-KR\"?>\n<a xmlns=\"http://www.w3.org/1999/xhtml\" t=\"" + KOREAN + "\">" + KOREAN + "</a>";
        XhtmlResult r = XhtmlParser.parseDocument(bytes(euc, "EUC-KR"), null, "http://example.org/k.xhtml");
        Check.isNull(r.getError(), "well-formed: " + r.getError());
        Check.equal(KOREAN, r.getDocument().getDocumentElement().getTextContent(), "decoded text in the DOM");
        Check.equal(KOREAN, r.getDocument().getDocumentElement().getAttribute("t"), "decoded attribute in the DOM");
        Check.equal("EUC-KR", r.getDocument().getCharacterSet(), "character set of the document");
        Check.equal(euc, r.getSource(), "decoded source text");
        Check.equal(XmlText.SOURCE_DECLARATION, r.getText().getSource(), "decision source");
        // UTF-16 문서도 바이트 순서 표시로 읽는다.
        XhtmlResult u = XhtmlParser.parseDocument(bytes("<a>" + KOREAN + "</a>", "UTF-16LE", 0xFF, 0xFE), null, null);
        Check.isNull(u.getError(), "UTF-16 document");
        Check.equal(KOREAN, u.getDocument().getDocumentElement().getTextContent(), "UTF-16 text");
        Check.equal("UTF-16LE", u.getDocument().getCharacterSet(), "UTF-16 character set");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("<?xml version=\"1.0\"?>\n<a>\n<b>".getBytes("UTF-8"));
        out.write(0xC3);
        out.write(0x28);
        out.write("</b>\n</a>".getBytes("UTF-8"));
        XhtmlResult bad = XhtmlParser.parseDocument(out.toByteArray(), null, null);
        Check.notNull(bad.getError(), "invalid UTF-8 is an error");
        Check.equal(XmlParseError.Kind.ENCODING, bad.getError().getKind(), "kind: " + bad.getError());
        Check.equal(3, bad.getError().getLine(), "line of the encoding error");
        Check.equal("<b>", bad.getError().getSourceLine(), "source line up to the error");
        // 인코딩 오류보다 앞에서 난 구문 오류는 그대로 보고한다.
        ByteArrayOutputStream early = new ByteArrayOutputStream();
        early.write("<a>\n<b></c>\n<d>".getBytes("UTF-8"));
        early.write(0xFF);
        XhtmlResult first = XhtmlParser.parseDocument(early.toByteArray(), null, null);
        Check.equal(XmlParseError.Kind.NOT_WELL_FORMED, first.getError().getKind(), "the first problem wins: " + first.getError());
        Check.equal(2, first.getError().getLine(), "line of the syntax error");
    }
}
