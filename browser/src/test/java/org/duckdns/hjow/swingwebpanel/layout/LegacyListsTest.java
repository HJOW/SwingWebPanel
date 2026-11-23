package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the legacy list attributes (plan item P07-07-b-6, HTML5 2014 sections 4.5.5 to 4.5.8 and 10.3.8): the presentational
 * hints of <code>type</code> on <code>ol</code>, <code>ul</code> and <code>li</code>, and the "rules for parsing integers" of
 * <code>ol start</code> and <code>li value</code>. The marker text of the list items shows the used list style and the ordinal value.
 * <p>
 * 이전 방식의 목록 속성 시험(계획 P07-07-b-6, HTML5 2014 §4.5.5~4.5.8, §10.3.8): <code>ol</code>, <code>ul</code>, <code>li</code> 의 <code>type</code>
 * 표현 힌트와 <code>ol start</code>, <code>li value</code> 의 "정수 해석 규칙". 목록 항목의 표식 문자열이 사용된 목록 스타일과 순서 값을 보여 준다.
 */
public class LegacyListsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyListsTest() {
    }

    /**
     * Returns the marker text of a list item.
     * <p>
     * 목록 항목의 표식 문자열을 반환한다.
     *
     * @param r  the result / 결과
     * @param id id of the list item / 목록 항목의 id
     * @return the marker text / 표식 문자열
     */
    private static String marker(LayoutResult r, String id) {
        LayoutBox li = LayoutTestSupport.box(r, id);
        // 목록 항목의 자식 중 표식 상자를 찾는다.
        for (LayoutBox c : li.getChildren()) {
            if (c.getType() == LayoutBox.Type.MARKER) {
                return c.getText();
            }
        }
        Check.fail("list item #" + id + " has no marker");
        return null;
    }

    /**
     * <code>ol type</code> accepts exactly the five case-sensitive keys; other values keep the decimal default.
     * <p>
     * <code>ol type</code> 은 대소문자를 구분하는 다섯 값만 받으며 다른 값은 기본 십진수를 유지한다.
     */
    public void testOrderedListTypes() {
        String[] types = { "1", "a", "A", "i", "I", "b", "disc", "", "a ", "ａ" };
        String[] expected = { "3. ", "c. ", "C. ", "iii. ", "III. ", "3. ", "3. ", "3. ", "3. ", "3. " };
        // type 값마다 새 문서를 배치해 세 번째 항목의 표식을 확인한다.
        for (int i = 0; i < types.length; i++) {
            LayoutResult r = LayoutTestSupport.layout("<ol type='" + types[i] + "'><li>x</li><li>x</li><li id=c>x</li></ol>", 400);
            Check.equal(expected[i], marker(r, "c"), "ol type='" + types[i] + "'");
        }
    }

    /**
     * <code>ul type</code> accepts the bullet names without regard to ASCII case; the numbering keys have no effect on a bulleted list.
     * <p>
     * <code>ul type</code> 은 ASCII 대소문자와 무관하게 글머리 이름을 받으며 번호 값은 글머리 목록에 효과가 없다.
     */
    public void testUnorderedListTypes() {
        String[] types = { "disc", "circle", "square", "CIRCLE", "Square", "a", "1", "none", "" };
        String[] expected = { ListMarkers.text("disc", 1), ListMarkers.text("circle", 1), ListMarkers.text("square", 1),
                ListMarkers.text("circle", 1), ListMarkers.text("square", 1), ListMarkers.text("disc", 1), ListMarkers.text("disc", 1),
                ListMarkers.text("disc", 1), ListMarkers.text("disc", 1) };
        // type 값마다 새 문서를 배치해 표식을 확인한다.
        for (int i = 0; i < types.length; i++) {
            LayoutResult r = LayoutTestSupport.layout("<ul type='" + types[i] + "'><li id=a>x</li></ul>", 400);
            Check.equal(expected[i], marker(r, "a"), "ul type='" + types[i] + "'");
        }
    }

    /**
     * An <code>li</code> follows its own <code>type</code> whatever list contains it: both key sets apply, and the item overrides
     * the type of its list.
     * <p>
     * <code>li</code> 는 어떤 목록에 들어 있든 자신의 <code>type</code> 을 따른다: 두 값 집합이 모두 적용되고 항목이 목록의 type 을 덮어쓴다.
     */
    public void testListItemTypeIsIndependentOfTheParent() {
        LayoutResult r = LayoutTestSupport.layout("<ul><li id=a type=a>x</li><li id=b type=I>x</li><li id=c type=square>x</li><li id=d>x</li></ul>"
                + "<ol><li id=e type=circle>x</li><li id=f type=A>x</li><li id=g>x</li></ol>"
                + "<ol type=a><li id=h type=i>x</li><li id=i type=DISC>x</li><li id=j type=zzz>x</li></ol>"
                + "<menu><li id=k type=1>x</li></menu><dir><li id=l type=disc>x</li></dir>", 400);
        Check.equal("a. ", marker(r, "a"), "li type=a in ul uses the ordinal value 1");
        Check.equal("II. ", marker(r, "b"), "li type=I in ul: second item");
        Check.equal(ListMarkers.text("square", 3), marker(r, "c"), "li type=square in ul");
        Check.equal(ListMarkers.text("disc", 4), marker(r, "d"), "li without type keeps the list style");
        Check.equal(ListMarkers.text("circle", 1), marker(r, "e"), "li type=circle in ol");
        Check.equal("B. ", marker(r, "f"), "li type=A in ol");
        Check.equal("3. ", marker(r, "g"), "li without type in ol");
        Check.equal("i. ", marker(r, "h"), "li type overrides ol type=a");
        Check.equal(ListMarkers.text("disc", 2), marker(r, "i"), "li type matched without regard to ASCII case");
        Check.equal("c. ", marker(r, "j"), "unknown li type keeps the ol type");
        Check.equal("1. ", marker(r, "k"), "li in menu");
        Check.equal(ListMarkers.text("disc", 1), marker(r, "l"), "li in dir");
    }

    /**
     * Author CSS wins over the <code>type</code> hints, and the hint wins over the user-agent sheet.
     * <p>
     * 작성자 CSS 는 <code>type</code> 힌트보다 우선하고 힌트는 사용자 에이전트 시트보다 우선한다.
     */
    public void testAuthorStyleOverridesTypeHint() {
        LayoutResult r = LayoutTestSupport.layout("<style>.u{list-style-type:upper-roman}</style>"
                + "<ol type=a class=u><li id=a>x</li></ol><ol type=a><li id=b class=u>x</li></ol><ol type=a><li id=c>x</li></ol>", 400);
        Check.equal("I. ", marker(r, "a"), "author rule on ol");
        Check.equal("I. ", marker(r, "b"), "author rule on li");
        Check.equal("a. ", marker(r, "c"), "hint over the UA sheet");
    }

    /**
     * <code>start</code> and <code>value</code> use the "rules for parsing integers": white space, a sign and trailing text are
     * accepted, text without digits and numbers that are too large use the default.
     * <p>
     * <code>start</code> 와 <code>value</code> 는 "정수 해석 규칙"을 따른다: 공백, 부호, 뒤따르는 글자는 받고 숫자가 없거나 너무 큰 값은 기본값을 쓴다.
     */
    public void testStartAndValueIntegerParsing() {
        String[] starts = { "5", "+3", " 4x", "-2", "x", "", "99999999999", "0", "\n7" };
        String[] expected = { "5. ", "3. ", "4. ", "-2. ", "1. ", "1. ", "1. ", "0. ", "7. " };
        // start 값마다 새 문서를 배치해 첫 항목의 표식을 확인한다.
        for (int i = 0; i < starts.length; i++) {
            LayoutResult r = LayoutTestSupport.layout("<ol start='" + starts[i] + "'><li id=a>x</li></ol>", 400);
            Check.equal(expected[i], marker(r, "a"), "start='" + starts[i].replace("\n", "\\n") + "'");
        }
        LayoutResult r = LayoutTestSupport.layout("<ol><li id=a value='+8'>x</li><li id=b>x</li><li id=c value=' 2z'>x</li><li id=d value=abc>x</li>"
                + "<li id=e value=''>x</li><li id=f value='-1'>x</li><li id=g>x</li></ol>", 400);
        Check.equal("8. ", marker(r, "a"), "value=+8");
        Check.equal("9. ", marker(r, "b"), "next item follows");
        Check.equal("2. ", marker(r, "c"), "value with leading space and trailing text");
        Check.equal("3. ", marker(r, "d"), "value without digits is treated as absent");
        Check.equal("4. ", marker(r, "e"), "empty value is treated as absent");
        Check.equal("-1. ", marker(r, "f"), "negative value");
        Check.equal("0. ", marker(r, "g"), "continues after a negative value");
    }

    /**
     * A reversed list starts at its start attribute, or at the number of <code>li</code> children when the attribute cannot be parsed,
     * and counts down; a <code>value</code> in the middle restarts the sequence.
     * <p>
     * reversed 목록은 start 속성에서, 해석할 수 없으면 <code>li</code> 자식 수에서 시작해 거꾸로 센다. 중간의 <code>value</code> 는 순서를 다시 시작한다.
     */
    public void testReversedLists() {
        LayoutResult r = LayoutTestSupport.layout("<ol reversed start=' 10x'><li id=a>x</li><li id=b>x</li></ol>"
                + "<ol reversed start=bad><li id=c>x</li><li id=d>x</li><li id=e>x</li><li><li></ol>"
                + "<ol reversed><li id=f>x</li><li id=g value=7>x</li><li id=h>x</li></ol>"
                + "<ol reversed type=i><li id=i>x</li><li id=j>x</li></ol>", 400);
        Check.equal("10. ", marker(r, "a"), "start 10");
        Check.equal("9. ", marker(r, "b"), "counts down");
        Check.equal("5. ", marker(r, "c"), "the unparsable start uses the number of li children");
        Check.equal("3. ", marker(r, "e"), "third item of five");
        Check.equal("3. ", marker(r, "f"), "number of li children");
        Check.equal("7. ", marker(r, "g"), "value restarts");
        Check.equal("6. ", marker(r, "h"), "then counts down");
        Check.equal("ii. ", marker(r, "i"), "reversed with a type hint");
        Check.equal("i. ", marker(r, "j"), "last item");
    }
}
