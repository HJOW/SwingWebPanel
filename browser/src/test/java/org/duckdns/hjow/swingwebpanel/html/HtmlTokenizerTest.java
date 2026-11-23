package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the HTML tokenizer (plan items P07-02, P07-03 and the character reference part of P07-04): every token kind, the state
 * groups (data, RCDATA, RAWTEXT, script data with escapes, PLAINTEXT, tags, attributes, comments, DOCTYPE, CDATA) and the error codes.
 * Tokens are written compactly: characters as <code>"text"</code>, tags as <code>&lt;a b="c"&gt;</code>, self-closing tags as
 * <code>&lt;br/&gt;</code>, comments as <code>&lt;!--x--&gt;</code>, DOCTYPE as <code>D(name,public,system,quirks)</code> with <code>-</code> for
 * a missing value, and non-ASCII or control characters as <code>{U+XXXX}</code>.
 * <p>
 * HTML 토큰화기 시험(계획 P07-02, P07-03, P07-04 의 문자 참조 부분): 모든 토큰 종류, 상태 그룹(data, RCDATA, RAWTEXT, 이스케이프가 있는 script data,
 * PLAINTEXT, 태그, 속성, 주석, DOCTYPE, CDATA), 오류 코드. 토큰은 간단히 표기한다: 문자는 <code>"text"</code>, 태그는
 * <code>&lt;a b="c"&gt;</code>, 자기 닫기 태그는 <code>&lt;br/&gt;</code>, 주석은 <code>&lt;!--x--&gt;</code>, DOCTYPE 은
 * <code>D(이름,공개,시스템,quirks)</code>(없는 값은 <code>-</code>), ASCII 가 아니거나 제어 문자는 <code>{U+XXXX}</code>.
 */
public class HtmlTokenizerTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HtmlTokenizerTest() {
    }

    /**
     * Result of one tokenizer run: the token dump and the error codes.
     * <p>
     * 토큰화기를 한 번 돌린 결과: 토큰 덤프와 오류 코드.
     */
    private static final class Run {
        /**
         * Token dump without the final end-of-file token.
         * <p>
         * 마지막 파일 끝 토큰을 뺀 토큰 덤프.
         */
        String tokens;

        /**
         * Error codes joined by commas.
         * <p>
         * 쉼표로 이은 오류 코드.
         */
        String errors;
    }

    /**
     * Appends a character or code point to a dump, escaping everything outside printable ASCII.
     * <p>
     * 덤프에 문자나 코드 포인트를 덧붙이되 출력 가능한 ASCII 밖의 것은 이스케이프한다.
     *
     * @param sb  output / 출력
     * @param s   text / 문자열
     */
    private static void appendEscaped(StringBuilder sb, String s) {
        int i = 0;
        // 코드 포인트 단위로 훑으며 ASCII 가 아닌 것은 {U+XXXX} 로 바꾼다.
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\n') {
                sb.append("\\n");
            } else if (cp >= 0x20 && cp < 0x7F) {
                sb.append((char) cp);
            } else {
                String h = Integer.toHexString(cp).toUpperCase();
                // 16진수를 네 자리로 맞춘다.
                while (h.length() < 4) {
                    h = "0" + h;
                }
                sb.append("{U+").append(h).append('}');
            }
        }
    }

    /**
     * Runs the tokenizer over an input and dumps the tokens.
     * <p>
     * 입력에 토큰화기를 돌려 토큰을 덤프한다.
     *
     * @param input     input (already normalized) / 입력(이미 정규화됨)
     * @param state     initial state / 시작 상태
     * @param lastStart name of the last start tag, or null / 마지막 시작 태그 이름 또는 null
     * @param cdata     whether CDATA sections are allowed / CDATA 섹션을 허용하는지
     * @return the run result / 실행 결과
     */
    private static Run run(String input, int state, String lastStart, boolean cdata) {
        ParseErrorLog log = new ParseErrorLog(input, 1000, null);
        HtmlTokenizer t = new HtmlTokenizer(input, log);
        t.setState(state);
        t.setLastStartTag(lastStart);
        t.setAllowCdata(cdata);
        StringBuilder sb = new StringBuilder();
        StringBuilder chars = new StringBuilder();
        // EOF 토큰이 나올 때까지 토큰을 받아 덤프한다(이어지는 문자 토큰은 합친다).
        while (true) {
            HtmlToken k = t.nextToken();
            if (k.getType() == HtmlToken.CHARACTER) {
                chars.append(k.getData());
                continue;
            }
            if (chars.length() > 0) {
                sb.append('"');
                appendEscaped(sb, chars.toString());
                sb.append('"');
                chars.setLength(0);
            }
            if (k.getType() == HtmlToken.EOF) {
                break;
            }
            switch (k.getType()) {
            case HtmlToken.START_TAG:
                sb.append('<');
                appendEscaped(sb, k.getName());
                // 속성을 차례로 덤프한다.
                for (int i = 0; i < k.getAttributeCount(); i++) {
                    sb.append(' ');
                    appendEscaped(sb, k.getAttributeName(i));
                    sb.append("=\"");
                    appendEscaped(sb, k.getAttributeValue(i));
                    sb.append('"');
                }
                sb.append(k.isSelfClosing() ? "/>" : ">");
                break;
            case HtmlToken.END_TAG:
                sb.append("</");
                appendEscaped(sb, k.getName());
                sb.append('>');
                break;
            case HtmlToken.COMMENT:
                sb.append("<!--");
                appendEscaped(sb, k.getData());
                sb.append("-->");
                break;
            default:
                sb.append("D(");
                appendEscaped(sb, k.getName() == null ? "-" : k.getName());
                sb.append(',');
                appendEscaped(sb, k.getPublicId() == null ? "-" : k.getPublicId());
                sb.append(',');
                appendEscaped(sb, k.getSystemId() == null ? "-" : k.getSystemId());
                sb.append(',').append(k.isForceQuirks() ? '1' : '0').append(')');
                break;
            }
        }
        Run r = new Run();
        r.tokens = sb.toString();
        List<String> codes = log.getCodes();
        StringBuilder e = new StringBuilder();
        // 오류 코드를 쉼표로 잇는다.
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) {
                e.append(',');
            }
            e.append(codes.get(i));
        }
        r.errors = e.toString();
        return r;
    }

    /**
     * Checks the tokens of an input in the data state.
     * <p>
     * data 상태에서 입력의 토큰을 확인한다.
     *
     * @param input    input / 입력
     * @param tokens   expected token dump / 기대하는 토큰 덤프
     * @param errors   expected error codes joined by commas / 기대하는 오류 코드(쉼표로 이음)
     */
    private static void check(String input, String tokens, String errors) {
        Run r = run(input, HtmlTokenizer.DATA, null, false);
        Check.equal(tokens, r.tokens, "tokens of: " + input);
        Check.equal(errors, r.errors, "errors of: " + input);
    }

    /**
     * Plain text and tags: names are lower-cased, attributes in the three quoting styles, self-closing and end tags.
     * <p>
     * 일반 텍스트와 태그: 이름은 소문자가 되고, 속성은 세 가지 따옴표 방식, 자기 닫기와 종료 태그.
     */
    public void testTagsAndText() {
        check("abc", "\"abc\"", "");
        check("<a>", "<a>", "");
        check("<A HREF=x>", "<a href=\"x\">", "");
        check("<a b c=d e='f' g=\"h\">", "<a b=\"\" c=\"d\" e=\"f\" g=\"h\">", "");
        check("<br/>", "<br/>", "");
        check("<br />", "<br/>", "");
        check("</a>", "</a>", "");
        check("a<b>c</b>d", "\"a\"<b>\"c\"</b>\"d\"", "");
        check("<DIV Class=\"X\">", "<div class=\"X\">", "");
        check("<a\tb\n=\n\"c\"\f>", "<a b=\"c\">", "");
        check("</a b>", "</a>", "end-tag-with-attributes");
        check("</a/>", "</a>", "end-tag-with-trailing-solidus");
        check("<a a=1 a=2 A=3>", "<a a=\"1\">", "duplicate-attribute,duplicate-attribute");
        check("<a b=\"1\"c=\"2\">", "<a b=\"1\" c=\"2\">", "missing-whitespace-between-attributes");
        check("<a b=>", "<a b=\"\">", "missing-attribute-value");
        check("<a/b>", "<a b=\"\">", "unexpected-solidus-in-tag");
        check("<a \"b\">", "<a \"b\"=\"\">", "unexpected-character-in-attribute-name,unexpected-character-in-attribute-name");
        check("<a =b>", "<a =b=\"\">", "unexpected-equals-sign-before-attribute-name");
        check("<a b=c\"d'e<f=g`h>", "<a b=\"c\"d'e<f=g`h\">",
                "unexpected-character-in-unquoted-attribute-value,unexpected-character-in-unquoted-attribute-value,"
                        + "unexpected-character-in-unquoted-attribute-value,unexpected-character-in-unquoted-attribute-value,"
                        + "unexpected-character-in-unquoted-attribute-value");
        check("<a b='x\"y'>", "<a b=\"x\"y\">", "");
        check("<a\u0000b>", "<a{U+FFFD}b>", "unexpected-null-character");
        check("<a b\u0000=c>", "<a b{U+FFFD}=\"c\">", "unexpected-null-character");
        check("<a b=\"\u0000\">", "<a b=\"{U+FFFD}\">", "unexpected-null-character");
    }

    /**
     * Tags and text cut by the end of the input, and invalid tag openings.
     * <p>
     * 입력 끝에서 잘린 태그와 텍스트, 잘못된 태그 시작.
     */
    public void testTagErrors() {
        check("<", "\"<\"", "eof-before-tag-name");
        check("</", "\"</\"", "eof-before-tag-name");
        check("<a", "", "eof-in-tag");
        check("<a b", "", "eof-in-tag");
        check("<a b=", "", "eof-in-tag");
        check("<a b=\"c", "", "eof-in-tag");
        check("<a b='c", "", "eof-in-tag");
        check("<a b=c", "", "eof-in-tag");
        check("<a b=\"c\"", "", "eof-in-tag");
        check("<a /", "", "eof-in-tag");
        check("</a", "", "eof-in-tag");
        check("a < b", "\"a < b\"", "invalid-first-character-of-tag-name");
        check("a<3", "\"a<3\"", "invalid-first-character-of-tag-name");
        check("</>", "", "missing-end-tag-name");
        check("</3>", "<!--3-->", "invalid-first-character-of-tag-name");
        check("<?php echo 1 ?>", "<!--?php echo 1 ?-->", "unexpected-question-mark-instead-of-tag-name");
        check("<!foo>", "<!--foo-->", "incorrectly-opened-comment");
        check("<!", "<!---->", "incorrectly-opened-comment");
        check("x\u0000y", "\"x{U+0000}y\"", "unexpected-null-character");
    }

    /**
     * Comments: the normal form, the abrupt endings, the bang ending, nested openings, bogus comments and the end of the input.
     * <p>
     * 주석: 정상 형식, 갑작스러운 끝, 느낌표 끝, 중첩된 시작, bogus 주석, 입력 끝.
     */
    public void testComments() {
        check("<!-- c -->", "<!-- c -->", "");
        check("<!---->", "<!---->", "");
        check("<!-->", "<!---->", "abrupt-closing-of-empty-comment");
        check("<!--->", "<!---->", "abrupt-closing-of-empty-comment");
        check("<!-- a --!>", "<!-- a -->", "incorrectly-closed-comment");
        check("<!-- a --!x-->", "<!-- a --!x-->", "");
        check("<!-- a - b -- c -->", "<!-- a - b -- c -->", "");
        check("<!-- <!-- -->", "<!-- <!-- -->", "nested-comment");
        check("<!-- <!--> x", "<!-- <!-->\" x\"", "");
        check("<!--a", "<!--a-->", "eof-in-comment");
        check("<!--a-", "<!--a-->", "eof-in-comment");
        check("<!--a--", "<!--a-->", "eof-in-comment");
        check("<!--a--!", "<!--a-->", "eof-in-comment");
        check("<!--", "<!---->", "eof-in-comment");
        check("<!--\u0000-->", "<!--{U+FFFD}-->", "unexpected-null-character");
        check("<!--a\nb-->", "<!--a\\nb-->", "");
        check("<![CDATA[x]]>", "<!--[CDATA[x]]-->", "cdata-in-html-content");
        check("<!DOCTYP html>", "<!--DOCTYP html-->", "incorrectly-opened-comment");
        check("<!x\u0000y>", "<!--x{U+FFFD}y-->", "incorrectly-opened-comment,unexpected-null-character");
    }

    /**
     * DOCTYPE tokens with the public and system identifiers and all of their error paths.
     * <p>
     * 공개/시스템 식별자가 있는 DOCTYPE 토큰과 모든 오류 경로.
     */
    public void testDoctype() {
        check("<!DOCTYPE html>", "D(html,-,-,0)", "");
        check("<!doctype HTML>", "D(html,-,-,0)", "");
        check("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">",
                "D(html,-//W3C//DTD HTML 4.01//EN,http://www.w3.org/TR/html4/strict.dtd,0)", "");
        check("<!DOCTYPE html PUBLIC '-//x//' 'y'>", "D(html,-//x//,y,0)", "");
        check("<!DOCTYPE html PUBLIC \"p\">", "D(html,p,-,0)", "");
        check("<!DOCTYPE html SYSTEM \"about:legacy-compat\">", "D(html,-,about:legacy-compat,0)", "");
        check("<!DOCTYPE html SYSTEM 'a'>", "D(html,-,a,0)", "");
        check("<!DOCTYPE html public \"p\" system \"s\">", "D(html,p,-,1)", "missing-quote-before-doctype-system-identifier");
        check("<!DOCTYPE>", "D(-,-,-,1)", "missing-doctype-name");
        check("<!DOCTYPE >", "D(-,-,-,1)", "missing-doctype-name");
        check("<!DOCTYPEhtml>", "D(html,-,-,0)", "missing-whitespace-before-doctype-name");
        check("<!DOCTYPE html", "D(html,-,-,1)", "eof-in-doctype");
        check("<!DOCTYPE", "D(-,-,-,1)", "eof-in-doctype");
        check("<!DOCTYPE ", "D(-,-,-,1)", "eof-in-doctype");
        check("<!DOCTYPE html x>", "D(html,-,-,1)", "invalid-character-sequence-after-doctype-name");
        check("<!DOCTYPE html PUBLIC>", "D(html,-,-,1)", "missing-doctype-public-identifier");
        check("<!DOCTYPE html PUBLIC >", "D(html,-,-,1)", "missing-doctype-public-identifier");
        check("<!DOCTYPE html PUBLIC x>", "D(html,-,-,1)", "missing-quote-before-doctype-public-identifier");
        check("<!DOCTYPE html PUBLIC\"p\">", "D(html,p,-,0)", "missing-whitespace-after-doctype-public-keyword");
        check("<!DOCTYPE html PUBLIC \"p>", "D(html,p,-,1)", "abrupt-doctype-public-identifier");
        check("<!DOCTYPE html PUBLIC \"p\"\"s\">", "D(html,p,s,0)",
                "missing-whitespace-between-doctype-public-and-system-identifiers");
        check("<!DOCTYPE html PUBLIC \"p\" x>", "D(html,p,-,1)", "missing-quote-before-doctype-system-identifier");
        check("<!DOCTYPE html SYSTEM>", "D(html,-,-,1)", "missing-doctype-system-identifier");
        check("<!DOCTYPE html SYSTEM\"s\">", "D(html,-,s,0)", "missing-whitespace-after-doctype-system-keyword");
        check("<!DOCTYPE html SYSTEM x>", "D(html,-,-,1)", "missing-quote-before-doctype-system-identifier");
        check("<!DOCTYPE html SYSTEM \"s>", "D(html,-,s,1)", "abrupt-doctype-system-identifier");
        check("<!DOCTYPE html SYSTEM \"s\" x>", "D(html,-,s,0)", "unexpected-character-after-doctype-system-identifier");
        check("<!DOCTYPE html SYSTEM \"s\"", "D(html,-,s,1)", "eof-in-doctype");
        check("<!DOCTYPE \u0000>", "D({U+FFFD},-,-,0)", "unexpected-null-character");
    }

    /**
     * Character references in text: named (with and without semicolon, unknown, multi-code-point, supplementary) and numeric.
     * <p>
     * 텍스트 안의 문자 참조: 이름 있는 참조(세미콜론 유무, 알 수 없는 이름, 코드 포인트 둘, 보충 문자)와 숫자 참조.
     */
    public void testCharacterReferences() {
        check("&amp;", "\"&\"", "");
        check("&amp", "\"&\"", "missing-semicolon-after-character-reference");
        check("&AMP;&AMPx", "\"&&x\"", "missing-semicolon-after-character-reference");
        check("&lt;&gt;&quot;&apos;", "\"<>\"'\"", "");
        check("&notit;", "\"{U+00AC}it;\"", "missing-semicolon-after-character-reference");
        check("&not;", "\"{U+00AC}\"", "");
        check("&nosuch;", "\"&nosuch;\"", "unknown-named-character-reference");
        check("&nosuch", "\"&nosuch\"", "");
        check("&", "\"&\"", "");
        check("& ", "\"& \"", "");
        check("&1", "\"&1\"", "");
        check("&NotEqualTilde;", "\"{U+2242}{U+0338}\"", "");
        check("&fjlig;", "\"fj\"", "");
        check("&Afr;", "\"{U+1D504}\"", "");
        check("&eacute;&Eacute", "\"{U+00E9}{U+00C9}\"", "missing-semicolon-after-character-reference");
        check("&CounterClockwiseContourIntegral;", "\"{U+2233}\"", "");
        check("&#65;", "\"A\"", "");
        check("&#x41;", "\"A\"", "");
        check("&#X4a;", "\"J\"", "");
        check("&#65", "\"A\"", "missing-semicolon-after-character-reference");
        check("&#x41 ", "\"A \"", "missing-semicolon-after-character-reference");
        check("&#0;", "\"{U+FFFD}\"", "null-character-reference");
        check("&#x110000;", "\"{U+FFFD}\"", "character-reference-outside-unicode-range");
        check("&#99999999999;", "\"{U+FFFD}\"", "character-reference-outside-unicode-range");
        check("&#xD800;&#xDFFF;", "\"{U+FFFD}{U+FFFD}\"", "surrogate-character-reference,surrogate-character-reference");
        check("&#128;", "\"{U+20AC}\"", "control-character-reference");
        check("&#x9F;", "\"{U+0178}\"", "control-character-reference");
        check("&#x81;", "\"{U+0081}\"", "control-character-reference");
        check("&#xFDD0;", "\"{U+FDD0}\"", "noncharacter-character-reference");
        check("&#xFFFF;", "\"{U+FFFF}\"", "noncharacter-character-reference");
        check("&#13;", "\"{U+000D}\"", "control-character-reference");
        check("&#9;&#10;&#12;&#32;", "\"{U+0009}\\n{U+000C} \"", "");
        check("&#127;", "\"{U+007F}\"", "control-character-reference");
        check("&#x1F600;", "\"{U+1F600}\"", "");
        check("&#;", "\"&#;\"", "absence-of-digits-in-numeric-character-reference");
        check("&#x;", "\"&#x;\"", "absence-of-digits-in-numeric-character-reference");
        check("&#xg;", "\"&#xg;\"", "absence-of-digits-in-numeric-character-reference");
        check("&#", "\"&#\"", "absence-of-digits-in-numeric-character-reference");
    }

    /**
     * Character references in attribute values: the historical rule for names without a semicolon.
     * <p>
     * 속성 값 안의 문자 참조: 세미콜론이 없는 이름에 대한 역사적 규칙.
     */
    public void testCharacterReferencesInAttributes() {
        check("<a b=\"&copy;\">", "<a b=\"{U+00A9}\">", "");
        check("<a b=\"&copy\">", "<a b=\"{U+00A9}\">", "missing-semicolon-after-character-reference");
        check("<a b=\"&copy=\">", "<a b=\"&copy=\">", "");
        check("<a b=\"&copya\">", "<a b=\"&copya\">", "");
        check("<a b=\"?a=1&copy=2&amp;c\">", "<a b=\"?a=1&copy=2&c\">", "");
        check("<a b=\"&notit;\">", "<a b=\"&notit;\">", "");
        check("<a b=\"&amp=\">", "<a b=\"&amp=\">", "");
        check("<a b=\"&amp;=\">", "<a b=\"&=\">", "");
        check("<a b='&lt;&#x41;'>", "<a b=\"<A\">", "");
        check("<a b=&lt;x>", "<a b=\"<x\">", "");
        check("<a b=&copy>", "<a b=\"{U+00A9}\">", "missing-semicolon-after-character-reference");
        check("<a b=\"&nosuch;\">", "<a b=\"&nosuch;\">", "unknown-named-character-reference");
        check("<a b=\"&#65\">", "<a b=\"A\">", "missing-semicolon-after-character-reference");
    }

    /**
     * RCDATA, RAWTEXT, script data and PLAINTEXT states and the appropriate end tag rule.
     * <p>
     * RCDATA, RAWTEXT, script data, PLAINTEXT 상태와 적절한 종료 태그 규칙.
     */
    public void testSpecialStates() {
        Run r = run("a<b>&amp;</title>x", HtmlTokenizer.RCDATA, "title", false);
        Check.equal("\"a<b>&\"</title>\"x\"", r.tokens, "rcdata");
        r = run("a<b>&amp;</style>x", HtmlTokenizer.RAWTEXT, "style", false);
        Check.equal("\"a<b>&amp;\"</style>\"x\"", r.tokens, "rawtext keeps references");
        r = run("</div></style >x", HtmlTokenizer.RAWTEXT, "style", false);
        Check.equal("\"</div>\"</style>\"x\"", r.tokens, "rawtext inappropriate end tag");
        r = run("</STYLE>", HtmlTokenizer.RAWTEXT, "style", false);
        Check.equal("</style>", r.tokens, "end tag name is case-insensitive");
        r = run("</styles></style/>", HtmlTokenizer.RAWTEXT, "style", false);
        Check.equal("\"</styles>\"</style>", r.tokens, "name must match exactly");
        Check.equal("end-tag-with-trailing-solidus", r.errors, "solidus error");
        r = run("a\u0000b", HtmlTokenizer.RCDATA, "title", false);
        Check.equal("\"a{U+FFFD}b\"", r.tokens, "nul in rcdata");
        r = run("a</b>\u0000<c>&amp;", HtmlTokenizer.PLAINTEXT, null, false);
        Check.equal("\"a</b>{U+FFFD}<c>&amp;\"", r.tokens, "plaintext");
        r = run("if (a<b && c>d) {}</script>x", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"if (a<b && c>d) {}\"</script>\"x\"", r.tokens, "script data");
        r = run("<!-- a </script>", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"<!-- a \"</script>", r.tokens, "escaped script ends at the end tag");
        r = run("<!--<script>a</script>--></script>x", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"<!--<script>a</script>-->\"</script>\"x\"", r.tokens, "double escaped script hides the inner end tag");
        r = run("<!--<script>a</script></script>", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"<!--<script>a</script>\"</script>", r.tokens, "double escape ends at the first </script>");
        r = run("<!--a", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"<!--a\"", r.tokens, "eof in escaped script");
        Check.equal("eof-in-script-html-comment-like-text", r.errors, "eof error");
        r = run("<!--<script>a", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("eof-in-script-html-comment-like-text", r.errors, "eof in double escaped script");
        r = run("<!-->", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"<!-->\"", r.tokens, "escape start then dashes");
        r = run("<!--->x</script>", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"<!--->x\"</script>", r.tokens, "dash dash greater-than leaves the escape");
        r = run("a<", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"a<\"", r.tokens, "lone less-than in script");
        r = run("</sc", HtmlTokenizer.SCRIPT_DATA, "script", false);
        Check.equal("\"</sc\"", r.tokens, "partial end tag at eof");
    }

    /**
     * CDATA sections: honoured when allowed (foreign content), bogus comments otherwise.
     * <p>
     * CDATA 섹션: 허용될 때(외부 콘텐츠) 인정하고 아니면 bogus 주석이 된다.
     */
    public void testCdata() {
        Run r = run("<![CDATA[x<y]]>z", HtmlTokenizer.DATA, null, true);
        Check.equal("\"x<yz\"", r.tokens, "cdata allowed");
        r = run("<![CDATA[a]b]]c]]]>", HtmlTokenizer.DATA, null, true);
        Check.equal("\"a]b]]c]\"", r.tokens, "brackets inside cdata");
        r = run("<![CDATA[abc", HtmlTokenizer.DATA, null, true);
        Check.equal("\"abc\"", r.tokens, "cdata eof tokens");
        Check.equal("eof-in-cdata", r.errors, "cdata eof error");
        r = run("<![CDATA[a&amp;]]>", HtmlTokenizer.DATA, null, true);
        Check.equal("\"a&amp;\"", r.tokens, "no references in cdata");
        r = run("<![CDATA[x]]>", HtmlTokenizer.DATA, null, false);
        Check.equal("<!--[CDATA[x]]-->", r.tokens, "cdata not allowed");
        Check.equal("cdata-in-html-content", r.errors, "cdata in html");
    }

    /**
     * Token offsets, the position, inserted text (document.write) and the end of the input.
     * <p>
     * 토큰 위치, 현재 위치, 삽입된 문자열(document.write), 입력 끝.
     */
    public void testPositionAndInsertion() {
        String input = "ab<p>cd</p>";
        HtmlTokenizer t = new HtmlTokenizer(input, new ParseErrorLog(input, 10, null));
        HtmlToken k = t.nextToken();
        Check.equal("ab", k.getData(), "text token");
        k = t.nextToken();
        Check.equal(2, k.getOffset(), "start tag offset");
        t.insertText("<b>x</b>");
        Check.equal("<b>", t.nextToken().toString(), "inserted markup is read next");
        Check.equal("chars(x)", t.nextToken().toString(), "inserted text");
        Check.equal("</b>", t.nextToken().toString(), "inserted end tag");
        Check.equal("chars(cd)", t.nextToken().toString(), "the rest");
        Check.equal("</p>", t.nextToken().toString(), "last tag");
        Check.isFalse(t.isFinished(), "not finished before eof");
        Check.equal(HtmlToken.EOF, t.nextToken().getType(), "eof");
        Check.isTrue(t.isFinished(), "finished");
        Check.equal(HtmlToken.EOF, t.nextToken().getType(), "eof repeats");
    }

    /**
     * Line and column numbers of errors.
     * <p>
     * 오류의 줄과 열 번호.
     */
    public void testErrorPositions() {
        String input = "ok\n<a b=1 b=2>\n&#0;";
        ParseErrorLog log = new ParseErrorLog(input, 10, null);
        HtmlTokenizer t = new HtmlTokenizer(input, log);
        List<HtmlToken> tokens = new ArrayList<HtmlToken>();
        // EOF 까지 토큰을 모아 오류가 모두 기록되게 한다.
        while (true) {
            HtmlToken k = t.nextToken();
            tokens.add(k);
            if (k.getType() == HtmlToken.EOF) {
                break;
            }
        }
        Check.equal(2, log.getErrors().size(), "two errors");
        ParseError dup = log.getErrors().get(0);
        Check.equal("duplicate-attribute", dup.getCode(), "first code");
        Check.equal(2, dup.getLine(), "duplicate line");
        Check.equal(10, dup.getColumn(), "duplicate column");
        ParseError nul = log.getErrors().get(1);
        Check.equal("null-character-reference", nul.getCode(), "second code");
        Check.equal(3, nul.getLine(), "reference line");
        Check.equal(2, log.getTotal() - 0, "total");
    }
}
