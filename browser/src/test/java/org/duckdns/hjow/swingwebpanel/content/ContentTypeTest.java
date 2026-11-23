package org.duckdns.hjow.swingwebpanel.content;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ContentType} (plan item P17-01): case and white space rules, parameters, quoted values with escapes, the first
 * parameter wins, malformed values (treated as "missing"), structured suffixes and the canonical text form.
 * <p>
 * {@link ContentType} 의 시험(계획 P17-01): 대소문자와 공백 규칙, 매개변수, 이스케이프가 있는 따옴표 값, 첫 매개변수 우선, 잘못된 값("없음"으로 취급),
 * 구조적 접미사, 정규 문자열 형태.
 */
public class ContentTypeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContentTypeTest() {
    }

    /**
     * Checks the media type and the charset of a header value.
     * <p>
     * 헤더 값의 미디어 타입과 charset 을 확인한다.
     *
     * @param header  the header value / 헤더 값
     * @param mime    the expected media type / 기대하는 미디어 타입
     * @param charset the expected charset or null / 기대하는 charset 또는 null
     */
    private static void check(String header, String mime, String charset) {
        ContentType t = ContentType.parse(header);
        Check.equal(mime, t.getMime(), "mime of [" + header + "]");
        Check.equal(charset, t.getCharset(), "charset of [" + header + "]");
        Check.equal(mime.length() > 0, t.isValid(), "validity of [" + header + "]");
    }

    /**
     * Plain values, letter case and white space around the parts.
     * <p>
     * 평범한 값, 대소문자, 각 부분 주위의 공백.
     */
    public void testBasics() {
        check("text/html", "text/html", null);
        check("TEXT/HTML", "text/html", null);
        check("Text/Html; Charset=UTF-8", "text/html", "UTF-8");
        check("  text/html  ", "text/html", null);
        check("text/html ;charset=utf-8", "text/html", "utf-8");
        check("text/html;\tcharset=utf-8\t", "text/html", "utf-8");
        check("text/html;\r\n charset=utf-8", "text/html", "utf-8");
        check("application/xhtml+xml; charset=euc-kr", "application/xhtml+xml", "euc-kr");
        check("text/html;charset=", "text/html", null);
        check("text/html; charset=\"\"", "text/html", null);
        ContentType t = ContentType.parse("Text/HTML");
        Check.equal("text", t.getType(), "type");
        Check.equal("html", t.getSubtype(), "subtype");
        Check.equal("text/html", ContentType.mimeOf("TEXT/html;x=y"), "mimeOf");
        Check.equal("", ContentType.mimeOf(null), "mimeOf null");
    }

    /**
     * Quoted values: escapes, separators inside quotes, an unterminated quote and text after the closing quote.
     * <p>
     * 따옴표 값: 이스케이프, 따옴표 안의 구분자, 닫히지 않은 따옴표, 닫는 따옴표 뒤의 텍스트.
     */
    public void testQuotedValues() {
        check("text/html; charset=\"utf-8\"", "text/html", "utf-8");
        check("text/html; charset=\"utf\\-8\"", "text/html", "utf-8");
        check("text/html; charset=\"ut\\\"f\"", "text/html", "ut\"f");
        check("text/html; charset=\"a;b\"; x=y", "text/html", "a;b");
        check("text/html; charset=\"utf-8", "text/html", "utf-8");
        check("text/html; charset=\"utf-8\" junk; x=y", "text/html", "utf-8");
        check("text/html; charset='utf-8'", "text/html", "'utf-8'");
        Check.equal("a b", ContentType.parse("text/html; name=\"a b\"").getParameter("name"), "quoted value with a space");
        Check.equal("a b", ContentType.parse("text/html; name=\"a b\"").getParameter("NAME"), "parameter names are case insensitive");
    }

    /**
     * Parameters: order, the first of a name wins, broken parameters are skipped, names are lower-cased.
     * <p>
     * 매개변수: 순서, 같은 이름은 첫 것이 이김, 깨진 매개변수는 건너뜀, 이름은 소문자.
     */
    public void testParameters() {
        ContentType t = ContentType.parse("text/html; B=2; a=1; b=3; ;; novalue; empty=; c=\"x\" ; charset=ok; d e=f; g=h i");
        Check.equal("{b=2, a=1, c=x, charset=ok}", t.getParameters().toString(), "kept parameters in order of appearance");
        Check.equal("ok", t.getCharset(), "charset");
        Check.equal("2", t.getParameter("b"), "the first b wins");
        Check.isNull(t.getParameter("novalue"), "no value");
        Check.isNull(t.getParameter("missing"), "missing");
        Check.isNull(t.getParameter(null), "null name");
        check("text/html;charset=a;charset=b", "text/html", "a");
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            /**
             * Changes the read only map.
             * <p>
             * 읽기 전용 맵을 바꾼다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                ContentType.parse("text/html;a=b").getParameters().put("x", "y");
            }
        }, "parameters are read only");
        check("text/html; charset=utf-8; boundary=\"" + (char) 1 + "\"", "text/html", "utf-8");
        Check.isNull(ContentType.parse("text/html; q=\"" + (char) 1 + "\"").getParameter("q"), "control character in a quoted value");
    }

    /**
     * Values that are not a media type are invalid and give the empty type: the content layer treats them as "missing".
     * <p>
     * 미디어 타입이 아닌 값은 올바르지 않으며 빈 타입이 된다: 콘텐츠 계층은 이를 "없음"으로 본다.
     */
    public void testInvalidValues() {
        String[] bad = {null, "", " ", "\t\r\n", "text", "/html", "text/", "te xt/html", "text/ht ml", "text/html, text/plain", "*/*", "text/html/x", ";",
            ";charset=utf-8", "text;/html", "te(xt/html", "text/hétml", "é/html", "text/html\u0000"};
        // 잘못된 값마다 올바르지 않고 미디어 타입이 비어 있어야 한다.
        for (int i = 0; i < bad.length; i++) {
            ContentType t = ContentType.parse(bad[i]);
            Check.isFalse(t.isValid(), "invalid: [" + bad[i] + "]");
            Check.equal("", t.getMime(), "empty mime: [" + bad[i] + "]");
            Check.equal("", t.toString(), "empty text: [" + bad[i] + "]");
            Check.isNull(t.getCharset(), "no charset: [" + bad[i] + "]");
            Check.isFalse(t.isXml() || t.isJson(), "not xml or json: [" + bad[i] + "]");
            Check.equal(0, t.getParameters().size(), "no parameters: [" + bad[i] + "]");
        }
    }

    /**
     * Structured suffixes, the XML and JSON families and the comparison helper.
     * <p>
     * 구조적 접미사, XML 과 JSON 계열, 비교 도우미.
     */
    public void testFamilies() {
        Check.equal("xml", ContentType.parse("application/xhtml+xml").getSuffix(), "suffix of xhtml");
        Check.equal("", ContentType.parse("text/html").getSuffix(), "no suffix");
        Check.equal("json", ContentType.parse("application/ld+json").getSuffix(), "json suffix");
        Check.isTrue(ContentType.parse("application/xml").isXml(), "application/xml");
        Check.isTrue(ContentType.parse("text/xml").isXml(), "text/xml");
        Check.isTrue(ContentType.parse("image/svg+xml").isXml(), "+xml");
        Check.isTrue(ContentType.parse("application/rss+xml").isXml(), "rss");
        Check.isFalse(ContentType.parse("text/html").isXml(), "html is not xml");
        Check.isFalse(ContentType.parse("application/xmlx").isXml(), "xmlx");
        Check.isTrue(ContentType.parse("application/json").isJson(), "json");
        Check.isTrue(ContentType.parse("text/json").isJson(), "text/json");
        Check.isTrue(ContentType.parse("application/vnd.api+json").isJson(), "+json");
        Check.isFalse(ContentType.parse("application/jsonp").isJson(), "jsonp");
        Check.isTrue(ContentType.parse("Text/HTML; charset=x").isMime("text/HTML"), "isMime ignores case");
        Check.isFalse(ContentType.parse("text/html").isMime("text/plain"), "isMime");
        Check.isFalse(ContentType.parse("text/html").isMime(null), "isMime null");
        Check.isFalse(ContentType.parse("bogus").isMime("bogus"), "an invalid value matches nothing");
    }

    /**
     * The canonical text form: lower case, quoting only where needed, escaped quotes.
     * <p>
     * 정규 문자열 형태: 소문자, 필요한 곳에만 따옴표, 이스케이프한 따옴표.
     */
    public void testCanonicalForm() {
        Check.equal("text/html; charset=UTF-8", ContentType.parse("TEXT/HTML;Charset=\"UTF-8\"").toString(), "token value");
        Check.equal("text/html; a=\"b c\"; d=e", ContentType.parse("text/html;a=\"b c\";d=e").toString(), "value with a space is quoted");
        Check.equal("text/html; a=\"q\\\"r\\\\s\"", ContentType.parse("text/html; a=\"q\\\"r\\\\s\"").toString(), "quote and backslash are escaped");
        Check.equal("text/html", ContentType.parse("text/html;").toString(), "no parameters");
        Check.equal(ContentType.parse("text/html;charset=a").toString(), ContentType.parse(ContentType.parse("text/html;charset=a").toString()).toString(), "stable");
    }
}
