package org.duckdns.hjow.swingwebpanel.html;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of encoding detection and decoding (plan item P07-01): byte order marks, the HTTP charset, the meta prescan with all of its
 * traps, label aliases, Korean and other East Asian encodings, the default and the UTF-8 heuristic, late re-decoding and the input
 * preprocessing that is kept separate from the received text.
 * <p>
 * 인코딩 판정과 디코딩 시험(계획 P07-01): 바이트 순서 표시, HTTP charset, 함정이 많은 meta 사전 검사, 라벨 별칭, 한국어와 그 밖의 동아시아 인코딩,
 * 기본값과 UTF-8 추측, 늦은 재디코딩, 받은 텍스트와 분리된 입력 전처리.
 */
public class EncodingSnifferTest {

    /**
     * EUC-KR bytes of the two Korean syllables for "hangul" (한글).
     * <p>
     * "한글" 두 음절의 EUC-KR 바이트.
     */
    private static final String HANGUL_EUCKR = "C7D1B1DB";

    /**
     * UTF-8 bytes of the same two syllables.
     * <p>
     * 같은 두 음절의 UTF-8 바이트.
     */
    private static final String HANGUL_UTF8 = "ED959CEAB880";

    /**
     * The same two syllables as a Java string.
     * <p>
     * 같은 두 음절의 자바 문자열.
     */
    private static final String HANGUL = "한글";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EncodingSnifferTest() {
    }

    /**
     * Builds a string from code points (this keeps unusual characters out of the source file).
     * <p>
     * 코드 포인트로 문자열을 만든다(특이한 문자를 소스 파일에 직접 쓰지 않게 해 준다).
     *
     * @param codePoints code points / 코드 포인트
     * @return the string / 문자열
     */
    static String s(int... codePoints) {
        StringBuilder sb = new StringBuilder();
        // 코드 포인트를 차례로 문자열에 덧붙인다.
        for (int i = 0; i < codePoints.length; i++) {
            sb.appendCodePoint(codePoints[i]);
        }
        return sb.toString();
    }

    /**
     * Converts a string of hexadecimal digits to bytes.
     * <p>
     * 16진수 문자열을 바이트로 바꾼다.
     *
     * @param hex hexadecimal digits / 16진수 숫자
     * @return bytes / 바이트
     */
    private static byte[] hex(String hex) {
        byte[] b = new byte[hex.length() / 2];
        // 두 자리씩 읽어 바이트 하나로 만든다.
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return b;
    }

    /**
     * Concatenates byte arrays, where a string argument stands for its ASCII bytes.
     * <p>
     * 바이트 배열을 이어 붙인다. 문자열 인수는 그 ASCII 바이트를 뜻한다.
     *
     * @param parts byte arrays or ASCII strings / 바이트 배열 또는 ASCII 문자열
     * @return the bytes / 바이트
     */
    private static byte[] cat(Object... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 조각마다 바이트 배열이면 그대로, 문자열이면 ASCII 바이트로 쓴다.
        for (int i = 0; i < parts.length; i++) {
            try {
                byte[] b = parts[i] instanceof byte[] ? (byte[]) parts[i] : ((String) parts[i]).getBytes("US-ASCII");
                out.write(b, 0, b.length);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.toString());
            }
        }
        return out.toByteArray();
    }

    /**
     * Byte order marks decide with certainty and are not part of the text.
     * <p>
     * 바이트 순서 표시는 확실하게 판정하며 텍스트에 포함되지 않는다.
     */
    public void testByteOrderMarks() {
        DecodedText t = EncodingSniffer.decode(cat(hex("EFBBBF"), "hi"), "euc-kr");
        Check.equal("hi", t.getText(), "utf-8 bom text");
        Check.equal("UTF-8", t.getEncoding(), "utf-8 bom beats http");
        Check.isTrue(t.isCertain(), "bom is certain");
        Check.equal(DecodedText.SOURCE_BOM, t.getSource(), "bom source");
        t = EncodingSniffer.decode(hex("FFFE68006900"), null);
        Check.equal("hi", t.getText(), "utf-16le text");
        Check.equal("UTF-16LE", t.getEncoding(), "utf-16le");
        t = EncodingSniffer.decode(hex("FEFF00680069"), null);
        Check.equal("hi", t.getText(), "utf-16be text");
        Check.equal("UTF-16BE", t.getEncoding(), "utf-16be");
        t = EncodingSniffer.decode(cat(hex("EFBBBF"), hex(HANGUL_UTF8)), null);
        Check.equal(HANGUL, t.getText(), "korean after bom");
        Check.equal(3, EncodingSniffer.bomLength(cat(hex("EFBBBF"), "a")), "bom length 3");
        Check.equal(2, EncodingSniffer.bomLength(hex("FFFE")), "bom length 2");
        Check.equal(0, EncodingSniffer.bomLength(cat("abc")), "no bom");
        Check.equal(0, EncodingSniffer.bomLength(new byte[0]), "empty input has no bom");
    }

    /**
     * The transport layer label is certain, understands aliases and ignores unknown labels.
     * <p>
     * 전송 계층 라벨은 확실하며 별칭을 이해하고 알 수 없는 라벨은 무시한다.
     */
    public void testHttpCharset() {
        DecodedText t = EncodingSniffer.decode(hex(HANGUL_EUCKR), "EUC-KR");
        Check.equal(HANGUL, t.getText(), "euc-kr text");
        Check.equal("EUC-KR", t.getEncoding(), "euc-kr name");
        Check.isTrue(t.isCertain(), "http is certain");
        Check.equal(DecodedText.SOURCE_HTTP, t.getSource(), "http source");
        t = EncodingSniffer.decode(hex(HANGUL_EUCKR), "  Windows-949 ");
        Check.equal("EUC-KR", t.getEncoding(), "windows-949 is EUC-KR");
        Check.equal(HANGUL, t.getText(), "windows-949 text");
        t = EncodingSniffer.decode(hex(HANGUL_UTF8), "utf8");
        Check.equal("UTF-8", t.getEncoding(), "utf8 alias");
        Check.equal(HANGUL, t.getText(), "utf8 text");
        t = EncodingSniffer.decode(cat("abc"), "bogus-charset");
        Check.equal(DecodedText.SOURCE_DEFAULT, t.getSource(), "unknown label falls back");
        Check.equal("windows-1252", t.getEncoding(), "default encoding");
        Check.isFalse(t.isCertain(), "default is tentative");
        t = EncodingSniffer.decode(cat("<meta charset=utf-8>"), "bogus");
        Check.equal("UTF-8", t.getEncoding(), "meta still used after unknown http label");
        t = EncodingSniffer.decode(cat("abc"), "replacement-not-a-label");
        Check.equal("windows-1252", t.getEncoding(), "unknown label");
        t = EncodingSniffer.decode(cat("abc"), "iso-2022-kr");
        Check.equal("windows-1252", t.getEncoding(), "replacement encodings are not used");
    }

    /**
     * Label lookup of the Encoding Standard: aliases, case, white space.
     * <p>
     * Encoding 표준의 라벨 조회: 별칭, 대소문자, 공백.
     */
    public void testLabels() {
        Check.equal("UTF-8", EncodingLabels.canonicalName("UTF-8"), "utf-8");
        Check.equal("UTF-8", EncodingLabels.canonicalName("unicode-1-1-utf-8"), "old utf-8 label");
        Check.equal("UTF-8", EncodingLabels.canonicalName("\t UTF8\n"), "white space and case");
        Check.equal("windows-1252", EncodingLabels.canonicalName("latin1"), "latin1");
        Check.equal("windows-1252", EncodingLabels.canonicalName("iso-8859-1"), "iso-8859-1");
        Check.equal("windows-1252", EncodingLabels.canonicalName("us-ascii"), "us-ascii");
        Check.equal("EUC-KR", EncodingLabels.canonicalName("ks_c_5601-1987"), "ks_c_5601-1987");
        Check.equal("EUC-KR", EncodingLabels.canonicalName("korean"), "korean");
        Check.equal("EUC-KR", EncodingLabels.canonicalName("euc-kr"), "euc-kr");
        Check.equal("Shift_JIS", EncodingLabels.canonicalName("sjis"), "sjis");
        Check.equal("Shift_JIS", EncodingLabels.canonicalName("shift_jis"), "shift_jis");
        Check.equal("GBK", EncodingLabels.canonicalName("gb2312"), "gb2312");
        Check.equal("gb18030", EncodingLabels.canonicalName("gb18030"), "gb18030");
        Check.equal("Big5", EncodingLabels.canonicalName("big5-hkscs"), "big5-hkscs");
        Check.equal("EUC-JP", EncodingLabels.canonicalName("x-euc-jp"), "x-euc-jp");
        Check.equal("UTF-16LE", EncodingLabels.canonicalName("utf-16"), "utf-16 means little endian");
        Check.equal("UTF-16BE", EncodingLabels.canonicalName("utf-16be"), "utf-16be");
        Check.equal("x-user-defined", EncodingLabels.canonicalName("x-user-defined"), "x-user-defined");
        Check.equal("replacement", EncodingLabels.canonicalName("hz-gb-2312"), "replacement label");
        Check.equal("ISO-8859-2", EncodingLabels.canonicalName("latin2"), "latin2");
        Check.equal("windows-1251", EncodingLabels.canonicalName("cp1251"), "cp1251");
        Check.equal("KOI8-R", EncodingLabels.canonicalName("koi8"), "koi8");
        Check.isNull(EncodingLabels.canonicalName("bogus"), "unknown label");
        Check.isNull(EncodingLabels.canonicalName(null), "null label");
        Check.isNull(EncodingLabels.canonicalName(""), "empty label");
        Check.isTrue(EncodingLabels.isDecodable("EUC-KR"), "euc-kr decodable");
        Check.isFalse(EncodingLabels.isDecodable(null), "null not decodable");
    }

    /**
     * The meta prescan: charset attribute, http-equiv with content, quoting styles, case, and the places where a meta is ignored.
     * <p>
     * meta 사전 검사: charset 속성, content 가 있는 http-equiv, 따옴표 방식, 대소문자, meta 가 무시되는 경우.
     */
    public void testMetaPrescan() {
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<html><head><meta charset=\"euc-kr\">")), "double quoted charset");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<META CHARSET='EUC-KR'>")), "single quoted upper case");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<meta charset=euc-kr>")), "unquoted");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<meta charset = euc-kr >")), "spaces around equals");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<meta/charset=euc-kr>")), "slash separator");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=EUC-KR\">")),
                "http-equiv content type");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<meta content=\"text/html; charset=euc-kr\" http-equiv=content-type>")),
                "content before http-equiv");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat("<meta http-equiv=content-type content='text/html;charset=\"euc-kr\"'>")),
                "quoted charset in content");
        Check.isNull(EncodingSniffer.prescan(cat("<meta content=\"text/html; charset=euc-kr\">")), "content without http-equiv");
        Check.isNull(EncodingSniffer.prescan(cat("<meta http-equiv=\"refresh\" content=\"0; charset=euc-kr\">")), "refresh pragma");
        Check.isNull(EncodingSniffer.prescan(cat("<!-- <meta charset=euc-kr> -->")), "meta inside a comment");
        Check.equal("UTF-8", EncodingSniffer.prescan(cat("<!-- <meta charset=euc-kr> --><meta charset=utf-8>")), "meta after a comment");
        Check.equal("UTF-8", EncodingSniffer.prescan(cat("<!--><meta charset=utf-8>")), "comment closed by the opening dashes");
        Check.isNull(EncodingSniffer.prescan(cat("<meta charset=bogus>")), "unknown label");
        Check.isNull(EncodingSniffer.prescan(cat("<meta charset=bogus charset=euc-kr>")), "first charset attribute wins, even if unknown");
        Check.equal("UTF-8", EncodingSniffer.prescan(cat("<meta charset=utf-16le>")), "utf-16 declaration means utf-8");
        Check.equal("windows-1252", EncodingSniffer.prescan(cat("<meta charset=x-user-defined>")), "x-user-defined means windows-1252");
        Check.equal("UTF-8", EncodingSniffer.prescan(cat("<a href=\"<meta charset=euc-kr>\"><meta charset=utf-8>")),
                "meta inside an attribute value");
        Check.equal("UTF-8", EncodingSniffer.prescan(cat("<p class=x></p><!doctype html><?xml version=1?><meta charset=utf-8>")),
                "other markup is skipped");
        Check.equal("UTF-8", EncodingSniffer.prescan(cat("<metadata charset=euc-kr><meta charset=utf-8>")), "metadata is not meta");
        Check.isNull(EncodingSniffer.prescan(cat("<meta charset")), "cut off tag");
        Check.isNull(EncodingSniffer.prescan(new byte[0]), "empty input");
        StringBuilder pad = new StringBuilder();
        while (pad.length() < 1030) {
            // 1024 바이트가 넘도록 공백을 채워 meta 가 검사 범위 밖에 있게 한다.
            pad.append(' ');
        }
        Check.isNull(EncodingSniffer.prescan(cat(pad.toString(), "<meta charset=euc-kr>")), "meta after the first 1024 bytes");
        Check.equal("EUC-KR", EncodingSniffer.prescan(cat(pad.substring(0, 1000), "<meta charset=euc-kr>")), "meta inside the first 1024 bytes");
    }

    /**
     * Full decoding by meta declarations: tentative decision and Korean text.
     * <p>
     * meta 선언에 의한 전체 디코딩: 잠정적 판정과 한국어 텍스트.
     */
    public void testMetaDecoding() {
        DecodedText t = EncodingSniffer.decode(cat("<meta charset=euc-kr>", hex(HANGUL_EUCKR)), null);
        Check.equal("<meta charset=euc-kr>" + HANGUL, t.getText(), "euc-kr page text");
        Check.equal("EUC-KR", t.getEncoding(), "meta encoding");
        Check.isFalse(t.isCertain(), "meta is tentative");
        Check.equal(DecodedText.SOURCE_META, t.getSource(), "meta source");
        t = EncodingSniffer.decode(cat("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">", hex(HANGUL_UTF8)), null);
        Check.isTrue(t.getText().endsWith(HANGUL), "utf-8 page text");
        t = EncodingSniffer.decode(cat("<meta charset=shift_jis>", hex("82A0")), null);
        Check.isTrue(t.getText().endsWith("あ"), "shift_jis hiragana a");
        t = EncodingSniffer.decode(cat("<meta charset=euc-jp>", hex("A4A2")), null);
        Check.isTrue(t.getText().endsWith("あ"), "euc-jp hiragana a");
        t = EncodingSniffer.decode(cat("<meta charset=gbk>", hex("C4E3")), null);
        Check.isTrue(t.getText().endsWith("你"), "gbk ni");
        t = EncodingSniffer.decode(cat("<meta charset=big5>", hex("A4A4")), null);
        Check.isTrue(t.getText().endsWith("中"), "big5 zhong");
        t = EncodingSniffer.decode(cat("<meta charset=koi8-r>", hex("C1")), null);
        Check.isTrue(t.getText().endsWith("а"), "koi8-r a");
        t = EncodingSniffer.decode(cat("<meta charset=windows-1251>", hex("E0")), null);
        Check.isTrue(t.getText().endsWith("а"), "windows-1251 a");
        t = EncodingSniffer.decode(cat("<meta charset=iso-8859-2>", hex("B1")), null);
        Check.isTrue(t.getText().endsWith("ą"), "iso-8859-2 a ogonek");
    }

    /**
     * The default encoding, the UTF-8 heuristic and the user supplied fallback.
     * <p>
     * 기본 인코딩, UTF-8 추측, 사용자가 지정한 대체 인코딩.
     */
    public void testDefaultsAndHeuristic() {
        DecodedText t = EncodingSniffer.decode(hex("808182"), null);
        Check.equal("windows-1252", t.getEncoding(), "invalid utf-8 gives the default");
        Check.equal(s(0x20AC, 0x81, 0x201A), t.getText(), "windows-1252 as the standard defines it");
        t = EncodingSniffer.decode(hex("E9"), null);
        Check.equal("é", t.getText(), "latin-1 range");
        t = EncodingSniffer.decode(hex(HANGUL_UTF8), null);
        Check.equal("UTF-8", t.getEncoding(), "valid utf-8 is guessed");
        Check.equal(HANGUL, t.getText(), "guessed text");
        Check.isFalse(t.isCertain(), "guess is tentative");
        Check.equal(DecodedText.SOURCE_GUESS, t.getSource(), "guess source");
        t = EncodingSniffer.decode(cat("plain ascii"), null);
        Check.equal("windows-1252", t.getEncoding(), "ascii only keeps the default");
        t = EncodingSniffer.decode(hex("ED95"), null);
        Check.equal("windows-1252", t.getEncoding(), "truncated utf-8 is not guessed");
        t = EncodingSniffer.decode(hex("C080"), null);
        Check.equal("windows-1252", t.getEncoding(), "overlong utf-8 is not guessed");
        t = EncodingSniffer.decode(hex("EDA080"), null);
        Check.equal("windows-1252", t.getEncoding(), "encoded surrogate is not guessed");
        t = EncodingSniffer.decode(hex(HANGUL_EUCKR), null, "euc-kr");
        Check.equal("EUC-KR", t.getEncoding(), "user fallback");
        Check.equal(HANGUL, t.getText(), "user fallback text");
        t = EncodingSniffer.decode(hex(HANGUL_EUCKR), null, "bogus");
        Check.equal("windows-1252", t.getEncoding(), "bad user fallback");
        t = EncodingSniffer.decode(new byte[0], null);
        Check.equal("", t.getText(), "empty input");
    }

    /**
     * Special encodings and invalid input become U+FFFD instead of failing.
     * <p>
     * 특수 인코딩과 잘못된 입력은 실패하지 않고 U+FFFD 가 된다.
     */
    public void testSpecialDecoding() {
        Check.equal("a" + s(0xF780, 0xF7FF), EncodingLabels.decode(hex("6180FF"), 0, "x-user-defined"), "x-user-defined");
        Check.equal(s(0xFFFD), EncodingLabels.decode(hex("616263"), 0, "replacement"), "replacement");
        Check.equal("", EncodingLabels.decode(new byte[0], 0, "replacement"), "empty replacement");
        Check.equal("bc", EncodingLabels.decode(hex("616263"), 1, "UTF-8"), "offset");
        Check.equal("a" + s(0xFFFD) + "b", EncodingLabels.decode(hex("61FF62"), 0, "UTF-8"), "malformed utf-8");
        Check.equal("aé", EncodingLabels.decode(hex("61E9"), 0, "nonexistent-encoding"), "unknown encoding decodes as windows-1252");
        Check.equal(s(0xFFFD), EncodingLabels.decode(hex("B1"), 0, "EUC-KR"), "truncated euc-kr");
        DecodedText t = DecodedText.ofString("x\r\ny");
        Check.equal("x\r\ny", t.getText(), "string keeps the line ends");
        Check.isNull(t.getRawBytes(), "no bytes for a string");
        Check.same(t, t.redecode("EUC-KR"), "a string cannot be decoded again");
    }

    /**
     * Late re-decoding: the bytes are kept, so another encoding can be applied.
     * <p>
     * 늦은 재디코딩: 바이트를 보관하므로 다른 인코딩을 적용할 수 있다.
     */
    public void testRedecode() {
        byte[] raw = hex(HANGUL_EUCKR);
        DecodedText t = EncodingSniffer.decode(raw, null);
        Check.equal("windows-1252", t.getEncoding(), "first guess");
        DecodedText u = t.redecode("EUC-KR");
        Check.equal(HANGUL, u.getText(), "re-decoded text");
        Check.isTrue(u.isCertain(), "re-decoded is certain");
        Check.same(raw, u.getRawBytes(), "bytes are shared");
        Check.same(u, u.redecode("EUC-KR"), "same encoding returns the same object");
        Check.same(t, t.redecode(null), "null encoding is ignored");
    }

    /**
     * Content-Type parameter extraction.
     * <p>
     * Content-Type 매개변수 추출.
     */
    public void testContentTypeCharset() {
        Check.equal("euc-kr", EncodingSniffer.charsetOfContentType("text/html; charset=euc-kr"), "plain");
        Check.equal("EUC-KR", EncodingSniffer.charsetOfContentType("text/html;charset=\"EUC-KR\""), "quoted");
        Check.equal("utf-8", EncodingSniffer.charsetOfContentType("text/html; boundary=x; Charset='utf-8'; q=1"), "among parameters");
        Check.isNull(EncodingSniffer.charsetOfContentType("text/html"), "no parameter");
        Check.isNull(EncodingSniffer.charsetOfContentType("text/html; charset="), "empty value");
        Check.isNull(EncodingSniffer.charsetOfContentType(null), "null");
        Check.equal("EUC-KR", EncodingSniffer.extractFromMetaContent("text/html; charset=euc-kr"), "meta content");
        Check.equal("EUC-KR", EncodingSniffer.extractFromMetaContent("text/html; CHARSET = 'euc-kr'"), "meta content spaces");
        Check.isNull(EncodingSniffer.extractFromMetaContent("text/html; charset='euc-kr"), "unterminated quote");
        Check.isNull(EncodingSniffer.extractFromMetaContent("text/html"), "no charset");
        Check.equal("UTF-8", EncodingSniffer.extractFromMetaContent("charsetx; charset=utf-8"), "second occurrence");
    }

    /**
     * Input preprocessing: new lines are normalized in a copy, the received text stays untouched; input stream errors are reported.
     * <p>
     * 입력 전처리: 줄바꿈은 복사본에서 정규화하고 받은 텍스트는 그대로 둔다. 입력 스트림 오류를 보고한다.
     */
    public void testPreprocessing() {
        Check.equal("a\nb\nc\n\nd", InputPreprocessor.normalize("a\r\nb\rc\n\r\nd"), "crlf and cr become lf");
        String plain = "no carriage return";
        Check.same(plain, InputPreprocessor.normalize(plain), "unchanged text is not copied");
        Check.equal("\n\n", InputPreprocessor.normalize("\r\r"), "two lone cr");
        Check.equal("\n", InputPreprocessor.normalize("\r\n"), "one crlf");
        DecodedText t = EncodingSniffer.decode(cat("a\r\nb"), null);
        Check.equal("a\r\nb", t.getText(), "received text keeps the line ends");
        String text = "ok" + s(1) + " " + s(0x7F, 0xFDD0, 0xFFFF) + " " + s(0x1F600) + " " + s(0xD800) + " " + s(0x1FFFF);
        ParseErrorLog log = new ParseErrorLog(text, 100, null);
        InputPreprocessor.scanErrors(text, log);
        Check.equal("control-character-in-input-stream,control-character-in-input-stream,noncharacter-in-input-stream,"
                + "noncharacter-in-input-stream,surrogate-in-input-stream,noncharacter-in-input-stream", join(log.getCodes()), "input errors");
        ParseErrorLog quiet = new ParseErrorLog("a\tb\nc\fd", 100, null);
        InputPreprocessor.scanErrors("a\tb\nc\fd", quiet);
        Check.equal(0, quiet.getTotal(), "white space is not an error");
    }

    /**
     * Joins strings with commas.
     * <p>
     * 문자열을 쉼표로 잇는다.
     *
     * @param list strings / 문자열
     * @return joined text / 이은 문자열
     */
    private static String join(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder();
        // 목록의 문자열을 쉼표로 구분해 하나로 모은다.
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * The named character reference table: size, legacy entries, multi code point and supplementary entries.
     * <p>
     * 이름 있는 문자 참조 표: 크기, 옛 항목, 코드 포인트 둘 이상인 항목, 보충 문자 항목.
     */
    public void testNamedReferenceTable() {
        Check.equal(2231, NamedCharacterReferences.size(), "entry count of entities.json");
        Check.equal("&", NamedCharacterReferences.lookup("amp;"), "amp;");
        Check.equal("&", NamedCharacterReferences.lookup("amp"), "legacy amp");
        Check.equal("&", NamedCharacterReferences.lookup("AMP"), "legacy AMP");
        Check.isNull(NamedCharacterReferences.lookup("Amp"), "no Amp without semicolon");
        Check.isNull(NamedCharacterReferences.lookup("hellip"), "hellip needs its semicolon");
        Check.equal("…", NamedCharacterReferences.lookup("hellip;"), "hellip;");
        Check.equal("é", NamedCharacterReferences.lookup("eacute;"), "eacute;");
        Check.equal("≂̸", NamedCharacterReferences.lookup("NotEqualTilde;"), "two code points");
        Check.equal(new String(Character.toChars(0x1D504)), NamedCharacterReferences.lookup("Afr;"), "supplementary");
        Check.equal("∳", NamedCharacterReferences.lookup("CounterClockwiseContourIntegral;"), "longest name");
        Check.equal(NamedCharacterReferences.MAX_NAME_LENGTH, "CounterClockwiseContourIntegral;".length(), "max name length constant");
        Check.isNull(NamedCharacterReferences.lookup("nosuch;"), "unknown");
    }
}
