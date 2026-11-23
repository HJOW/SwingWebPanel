package org.duckdns.hjow.swingwebpanel.css;

import java.util.LinkedHashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the grid shorthands that were added for the grid layout (plan item P10-03): <code>grid-row</code>,
 * <code>grid-column</code>, <code>grid-area</code>, <code>grid-template</code> (the forms <code>none</code>,
 * <code>rows / columns</code> and the one with the strings of the areas) and <code>grid</code> (the template form and the two
 * <code>auto-flow</code> forms), their expansion into the longhands and the errors.
 * <p>
 * 그리드 레이아웃(계획 P10-03)을 위해 추가한 그리드 단축 속성의 시험: <code>grid-row</code>, <code>grid-column</code>, <code>grid-area</code>,
 * <code>grid-template</code>(<code>none</code>, <code>행 / 열</code>, 영역 문자열을 쓴 형태), <code>grid</code>(템플릿 형태와 <code>auto-flow</code> 형태 둘)의
 * 개별 속성 전개와 오류.
 */
public class GridShorthandTest {

    /** The registry. 등록부. */
    private final PropertyRegistry reg = PropertyRegistry.getInstance();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public GridShorthandTest() {
    }

    /**
     * Parses a declaration and returns the text of one longhand or the result name.
     * <p>
     * 선언을 해석하고 개별 속성 하나의 문자열 또는 결과 이름을 반환한다.
     *
     * @param name     property name / 속성 이름
     * @param value    value text / 값 문자열
     * @param longhand the longhand to read / 읽을 개별 속성
     * @return text, or UNKNOWN, INVALID, MISSING / 문자열 또는 UNKNOWN, INVALID, MISSING
     */
    private String v(String name, String value, String longhand) {
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int r = reg.parse(name, value, out);
        if (r == PropertyRegistry.UNKNOWN) {
            return "UNKNOWN";
        }
        if (r != PropertyRegistry.OK) {
            return "INVALID";
        }
        CssValue x = out.get(Integer.valueOf(reg.byName(longhand).getId()));
        return x == null ? "MISSING" : x.toCssText();
    }

    /**
     * <code>grid-row</code> and <code>grid-column</code> split the lines at the slash; one identifier is used for both lines, one
     * number or span gives <code>auto</code> as the end.
     * <p>
     * <code>grid-row</code> 와 <code>grid-column</code> 은 슬래시에서 선을 나눈다. 식별자 하나는 두 선에 쓰고 숫자나 span 하나는 끝이 <code>auto</code> 이다.
     */
    public void testRowAndColumn() {
        Check.equal("1", v("grid-row", "1 / 3", "grid-row-start"), "start");
        Check.equal("3", v("grid-row", "1 / 3", "grid-row-end"), "end");
        Check.equal("span 2", v("grid-column", "span 2", "grid-column-start"), "a span alone");
        Check.equal("auto", v("grid-column", "span 2", "grid-column-end"), "the end of a lone span is auto");
        Check.equal("auto", v("grid-row", "2", "grid-row-end"), "the end of a lone number is auto");
        Check.equal("main", v("grid-column", "main", "grid-column-end"), "a lone name is used for both lines");
        Check.equal("2 foo", v("grid-column", "2 foo / -1", "grid-column-start"), "a numbered name");
        Check.equal("-1", v("grid-column", "2 foo / -1", "grid-column-end"), "negative end line");
        Check.equal("INVALID", v("grid-row", "1 / 2 / 3", "grid-row-start"), "three parts are invalid");
        Check.equal("INVALID", v("grid-row", "1 /", "grid-row-start"), "a missing end after the slash");
    }

    /**
     * <code>grid-area</code> takes up to four lines in the order row-start, column-start, row-end, column-end; omitted lines copy
     * a name or are <code>auto</code>.
     * <p>
     * <code>grid-area</code> 는 row-start, column-start, row-end, column-end 순서로 선을 최대 넷 받는다. 생략한 선은 이름을 따르거나 <code>auto</code> 이다.
     */
    public void testArea() {
        Check.equal("head", v("grid-area", "head", "grid-row-start"), "a name sets all four lines");
        Check.equal("head", v("grid-area", "head", "grid-column-end"), "the column end too");
        Check.equal("1", v("grid-area", "1 / 2 / 3 / 4", "grid-row-start"), "row start");
        Check.equal("2", v("grid-area", "1 / 2 / 3 / 4", "grid-column-start"), "column start");
        Check.equal("3", v("grid-area", "1 / 2 / 3 / 4", "grid-row-end"), "row end");
        Check.equal("4", v("grid-area", "1 / 2 / 3 / 4", "grid-column-end"), "column end");
        Check.equal("auto", v("grid-area", "1 / 2", "grid-row-end"), "omitted lines are auto");
        Check.equal("auto", v("grid-area", "1 / 2", "grid-column-end"), "omitted column end");
        Check.equal("INVALID", v("grid-area", "1 / 2 / 3 / 4 / 5", "grid-row-start"), "five lines are invalid");
    }

    /**
     * <code>grid-template</code> in the form <code>rows / columns</code> and <code>none</code>.
     * <p>
     * <code>grid-template</code> 의 <code>행 / 열</code> 형태와 <code>none</code>.
     */
    public void testTemplate() {
        Check.equal("100px 1fr", v("grid-template", "100px 1fr / auto 2fr", "grid-template-rows"), "rows");
        Check.equal("auto 2fr", v("grid-template", "100px 1fr / auto 2fr", "grid-template-columns"), "columns");
        Check.equal("none", v("grid-template", "100px 1fr / auto 2fr", "grid-template-areas"), "no areas");
        Check.equal("none", v("grid-template", "none", "grid-template-rows"), "none rows");
        Check.equal("none", v("grid-template", "none", "grid-template-columns"), "none columns");
        Check.equal("INVALID", v("grid-template", "100px", "grid-template-rows"), "a single list without a slash is invalid");
    }
    /**
     * <code>grid-template</code> with the strings of the areas: a string is a row of the areas, a size behind it is the height of the
     * row (<code>auto</code> if there is none), square brackets name the lines, and the columns follow a slash.
     * <p>
     * 영역 문자열을 쓴 <code>grid-template</code>: 문자열 하나가 영역의 행 하나이고 뒤의 크기가 행의 높이이며(없으면 <code>auto</code>), 대괄호가 선의 이름이고 열은 슬래시 뒤에 온다.
     */
    public void testTemplateWithAreas() {
        String t = "\"a b\" 20px \"c d\" / 1fr 1fr";
        Check.equal("20px auto", v("grid-template", t, "grid-template-rows"), "a row without a size is auto");
        Check.equal("1fr 1fr", v("grid-template", t, "grid-template-columns"), "columns behind the slash");
        Check.equal("\"a b\" \"c d\"", v("grid-template", t, "grid-template-areas"), "the strings are the areas");
        Check.equal("[top] 40px [mid] 1fr [bot]", v("grid-template", "[top] \"a a\" 40px [mid] \"b b\" 1fr [bot] / auto 1fr", "grid-template-rows"), "line names around the rows");
        Check.equal("none", v("grid-template", "\"a\" \"b\"", "grid-template-columns"), "without a slash there are no explicit columns");
        Check.equal("INVALID", v("grid-template", "\"a b\" 20px 30px", "grid-template-rows"), "two sizes behind one string");
        Check.equal("INVALID", v("grid-template", "20px \"a\"", "grid-template-rows"), "a size in front of the first string");
    }

    /**
     * The three forms of <code>grid</code>: a template, rows with an auto-flow column, and an auto-flow row with columns; each one
     * resets the longhands it does not set.
     * <p>
     * <code>grid</code> 의 세 형태: 템플릿, 행과 auto-flow 열, auto-flow 행과 열. 각 형태는 정하지 않는 개별 속성을 되돌린다.
     */
    public void testGridShorthand() {
        Check.equal("100px 1fr", v("grid", "100px 1fr / auto-flow 40px", "grid-template-rows"), "rows of the second form");
        Check.equal("column", v("grid", "100px 1fr / auto-flow 40px", "grid-auto-flow"), "the flow of the second form is column");
        Check.equal("40px", v("grid", "100px 1fr / auto-flow 40px", "grid-auto-columns"), "the implicit column size");
        Check.equal("none", v("grid", "100px 1fr / auto-flow 40px", "grid-template-columns"), "no explicit columns");
        Check.equal("row dense", v("grid", "auto-flow dense 30px / 1fr 2fr", "grid-auto-flow"), "the flow of the third form");
        Check.equal("30px", v("grid", "auto-flow dense 30px / 1fr 2fr", "grid-auto-rows"), "the implicit row size");
        Check.equal("1fr 2fr", v("grid", "auto-flow dense 30px / 1fr 2fr", "grid-template-columns"), "the explicit columns");
        Check.equal("row dense", v("grid", "dense auto-flow / 1fr", "grid-auto-flow"), "dense may come first");
        Check.equal("auto", v("grid", "dense auto-flow / 1fr", "grid-auto-rows"), "the implicit row size is auto if missing");
        Check.equal("100px", v("grid", "100px / 1fr", "grid-template-rows"), "the template form");
        Check.equal("row", v("grid", "100px / 1fr", "grid-auto-flow"), "the template form resets the flow");
        Check.equal("auto", v("grid", "100px / 1fr", "grid-auto-columns"), "and the implicit sizes");
        Check.equal("\"a b\"", v("grid", "\"a b\" 1fr / auto", "grid-template-areas"), "the template form with areas");
        Check.equal("INVALID", v("grid", "auto-flow / auto-flow", "grid-auto-flow"), "auto-flow on both sides");
        Check.equal("INVALID", v("grid", "1fr", "grid-template-rows"), "a lone track list is not a grid");
    }
}
