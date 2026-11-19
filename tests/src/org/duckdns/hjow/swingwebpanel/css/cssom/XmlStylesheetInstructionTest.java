package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.Map;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the pseudo-attributes of the <code>xml-stylesheet</code> processing instruction (plan item P07-07-c, LEG-12; Associating Style
 * Sheets with XML documents 1.0).
 * <p>
 * <code>xml-stylesheet</code> 처리 명령의 의사 속성 시험(계획 P07-07-c, LEG-12, Associating Style Sheets with XML documents 1.0).
 */
public class XmlStylesheetInstructionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XmlStylesheetInstructionTest() {
    }

    /**
     * Pairs with single and double quotes, extra white space, and the first occurrence of a name.
     * <p>
     * 작은따옴표와 큰따옴표 쌍, 여분의 공백, 같은 이름의 처음 것.
     */
    public void testPairs() {
        Map<String, String> a = XmlStylesheetInstruction.parse("type=\"text/css\" href='style.css'  media=\"screen, print\"\n title = \"Main\"");
        Check.equal("text/css", a.get("type"), "double quotes");
        Check.equal("style.css", a.get("href"), "single quotes");
        Check.equal("screen, print", a.get("media"), "a value with a comma and a space");
        Check.equal("Main", a.get("title"), "white space around =");
        Check.equal(4, a.size(), "four pairs");
        Map<String, String> b = XmlStylesheetInstruction.parse("href=\"a.css\" href=\"b.css\" TYPE=\"x\"");
        Check.equal("a.css", b.get("href"), "the first occurrence wins");
        Check.equal("x", b.get("TYPE"), "names are case-sensitive");
        Check.isNull(b.get("type"), "so type is missing");
        Check.equal(0, XmlStylesheetInstruction.parse("").size(), "empty data");
        Check.equal(0, XmlStylesheetInstruction.parse(null).size(), "null data");
        Check.equal(0, XmlStylesheetInstruction.parse("   ").size(), "white space only");
    }

    /**
     * The predefined entities and the character references are decoded; data that is not a list of name-value pairs has no pseudo-attributes.
     * <p>
     * 미리 정의된 엔티티와 문자 참조를 푼다. 이름-값 쌍의 목록이 아닌 데이터는 의사 속성이 없다.
     */
    public void testEntitiesAndErrors() {
        Map<String, String> a = XmlStylesheetInstruction.parse("href=\"a.css?x=1&amp;y=2&lt;&gt;&quot;&apos;&#65;&#x42;&unknown;\"");
        Check.equal("a.css?x=1&y=2<>\"'AB&unknown;", a.get("href"), "entities and references");
        Check.equal(0, XmlStylesheetInstruction.parse("href=a.css").size(), "an unquoted value");
        Check.equal(0, XmlStylesheetInstruction.parse("href=\"a.css").size(), "an unterminated value");
        Check.equal(0, XmlStylesheetInstruction.parse("href").size(), "a name without a value");
        Check.equal(0, XmlStylesheetInstruction.parse("=\"x\"").size(), "a value without a name");
        Check.equal(0, XmlStylesheetInstruction.parse("type=\"text/css\" garbage").size(), "garbage after a pair makes the whole instruction unreadable");
    }
}
