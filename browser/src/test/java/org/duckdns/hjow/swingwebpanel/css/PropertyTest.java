package org.duckdns.hjow.swingwebpanel.css;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssPending;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the property table and the declaration parser (plan items P08-04, P08-05, P08-07): the ids agree with the table, every initial value parses,
 * longhand validation, CSS-wide keywords, var(), aliases, unsupported names and every shorthand expansion.
 * <p>
 * 속성 표와 선언 파서 시험(계획 P08-04, P08-05, P08-07): id 가 표와 일치, 모든 초기값 파싱, 개별 속성 검증, CSS 전역 키워드, var(), 별칭, 지원하지 않는 이름,
 * 모든 단축 속성 전개.
 */
public class PropertyTest {

    /** The registry under test. / 시험 대상 등록부. */
    private final PropertyRegistry reg = PropertyRegistry.getInstance();

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public PropertyTest() {
    }

    /**
     * Parses a declaration and returns the text of one longhand, or the result code name. 선언을 파싱해 개별 속성 하나의 문자열 또는 결과 코드 이름을 반환한다.
     *
     * @param name     property name / 속성 이름
     * @param value    value text / 값 문자열
     * @param longhand longhand to read / 읽을 개별 속성
     * @return text or UNKNOWN, UNSUPPORTED, INVALID, MISSING / 문자열 또는 결과 코드 이름
     */
    private String v(String name, String value, String longhand) {
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int r = reg.parse(name, value, out);
        if (r == PropertyRegistry.UNKNOWN) {
            return "UNKNOWN";
        }
        if (r == PropertyRegistry.UNSUPPORTED) {
            return "UNSUPPORTED";
        }
        if (r == PropertyRegistry.INVALID) {
            return "INVALID";
        }
        CssValue x = out.get(Integer.valueOf(reg.byName(longhand).getId()));
        return x == null ? "MISSING" : x.toCssText();
    }

    /**
     * Returns the number of longhands a declaration sets, or -1 if it is not valid. 선언이 정하는 개별 속성의 수를 반환한다. 올바르지 않으면 -1.
     *
     * @param name  property name / 속성 이름
     * @param value value text / 값 문자열
     * @return count or -1 / 개수 또는 -1
     */
    private int count(String name, String value) {
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        return reg.parse(name, value, out) == PropertyRegistry.OK ? out.size() : -1;
    }

    /**
     * The constants of Prop agree with the rows of the table. Prop 의 상수가 표의 행과 일치한다.
     */
    public void testPropIdsMatchTable() throws Exception {
        Field[] fields = Prop.class.getFields();
        int seen = 0;
        // 모든 상수가 같은 이름의 속성을 가리키는지 확인한다.
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals("COUNT")) {
                continue;
            }
            int id = fields[i].getInt(null);
            String expected = fields[i].getName().toLowerCase().replace('_', '-');
            Check.equal(expected, reg.byId(id).getName(), "constant " + fields[i].getName());
            Check.equal(id, reg.byId(id).getId(), "id of " + expected);
            seen++;
        }
        Check.equal(Prop.COUNT, seen, "every row has a constant");
        Check.isTrue(Prop.COUNT > 170, "the table has more than 170 longhands");
    }

    /**
     * Inheritance, initial values and flags. 상속, 초기값, 플래그.
     */
    public void testInitialValues() {
        Property color = reg.byId(Prop.COLOR);
        Check.isTrue(color.isInherited(), "color is inherited");
        Check.equal("black", color.getInitial().toCssText(), "color initial");
        Check.isFalse(reg.byId(Prop.DISPLAY).isInherited(), "display is not inherited");
        Check.equal("inline", reg.byId(Prop.DISPLAY).getInitial().toCssText(), "display initial");
        Check.isTrue(reg.byId(Prop.FONT_FAMILY).isInherited(), "font-family is inherited");
        Check.isFalse(reg.byId(Prop.MARGIN_TOP).isInherited(), "margin is not inherited");
        Check.equal("0px", reg.byId(Prop.MARGIN_TOP).getInitial().toCssText(), "margin initial");
        Check.equal("currentcolor", reg.byId(Prop.BORDER_TOP_COLOR).getInitial().toCssText(), "border color initial");
        Check.equal("none", reg.byId(Prop.BACKGROUND_IMAGE).getInitial().toCssText(), "background image initial is a one-layer list");
        Check.isTrue(reg.byId(Prop.GRID_TEMPLATE_COLUMNS).isParsedOnly(), "grid is parsed only");
        Check.isFalse(reg.byId(Prop.WIDTH).isParsedOnly(), "width is applied");
        Check.same(reg.byId(Prop.WIDTH), reg.byName("WIDTH"), "names are case-insensitive");
        Check.same(reg.byId(Prop.OVERFLOW_WRAP), reg.byName("word-wrap"), "alias");
        Check.isNull(reg.byName("margin"), "a shorthand is not a longhand");
        Check.notNull(reg.getShorthand("margin"), "the shorthand exists");
    }

    /**
     * Longhand validation. 개별 속성 검증.
     */
    public void testLonghands() {
        Check.equal("red", v("color", "red", "color"), "color");
        Check.equal("calc(10px + 5%)", v("width", "calc(10px + 5%)", "width"), "calc width");
        Check.equal("INVALID", v("width", "-1px", "width"), "negative width");
        Check.equal("flex", v("display", "flex", "display"), "display");
        Check.equal("INVALID", v("display", "foo", "display"), "bad display");
        Check.equal("INVALID", v("display", "", "display"), "empty value");
        Check.equal("INVALID", v("display", "block inline", "display"), "two values");
        Check.equal("UNKNOWN", v("foo", "1", "color"), "unknown property");
        Check.equal("UNSUPPORTED", v("-webkit-foo", "1", "color"), "unknown vendor property");
        Check.equal("UNSUPPORTED", v("perspective", "100px", "color"), "an unsupported property");
        Check.equal("1px", v("margin-inline", "1px", "margin-inline-start"), "a logical shorthand sets the start longhand");
        Check.equal("2px", v("margin-inline", "1px 2px", "margin-inline-end"), "a logical shorthand sets the end longhand");
        Check.equal("UNSUPPORTED", v("perspective", "100px", "color"), "3D property");
        Check.equal("INVALID", v("transform", "translate3d(1px, 2px, 3px)", "transform"), "3D transform function");
        Check.equal("translate(1px, 2px) rotate(45deg)", v("transform", "translate(1px,2px) rotate(45deg)", "transform"), "2D transform list");
        Check.equal("50", v("z-index", "50", "z-index"), "z-index integer");
        Check.equal("INVALID", v("z-index", "1.5", "z-index"), "z-index real");
        Check.equal("0.5", v("opacity", ".5", "opacity"), "opacity number");
        Check.equal("50%", v("opacity", "50%", "opacity"), "opacity percentage");
        Check.equal("\"Times New Roman\", serif", v("font-family", "\"Times New Roman\", serif", "font-family"), "font family list");
        Check.equal("Times New Roman, Arial", v("font-family", "Times New Roman, Arial", "font-family"), "unquoted family names");
        Check.equal("INVALID", v("font-family", "Arial,", "font-family"), "trailing comma");
        Check.equal("bold", v("font-weight", "bold", "font-weight"), "font weight keyword");
        Check.equal("700", v("font-weight", "700", "font-weight"), "font weight number");
        Check.equal("INVALID", v("font-weight", "0", "font-weight"), "font weight range");
        Check.equal("1.5", v("line-height", "1.5", "line-height"), "line height number");
        Check.equal("1px 2px 3px red", v("box-shadow", "1px 2px 3px red", "box-shadow"), "box shadow");
        Check.equal("1px 2px red, inset 0px 0px 4px blue", v("box-shadow", "1px 2px red, inset 0 0 4px blue", "box-shadow"), "box shadow list");
        Check.equal("url(\"a.png\"), none", v("background-image", "url(a.png), none", "background-image"), "background images");
        Check.equal("10px", v("grid-template-columns", "10px", "grid-template-columns"), "parsed-only property still parses");
        Check.equal("INVALID", v("clip", "rect(1px, 2px)", "clip"), "clip needs four values");
        Check.equal("rect(1px, 2px, 3px, auto)", v("clip", "rect(1px, 2px, 3px, auto)", "clip"), "clip rect");
        Check.equal("pointer", v("cursor", "pointer", "cursor"), "cursor");
        Check.equal("url(\"a.cur\"), pointer", v("cursor", "url(a.cur), pointer", "cursor"), "cursor with an image");
    }

    /**
     * CSS-wide keywords and var(). CSS 전역 키워드와 var().
     */
    public void testWideKeywordsAndVar() {
        Check.equal("inherit", v("margin", "inherit", "margin-left"), "inherit reaches every longhand of a shorthand");
        Check.equal(4, count("margin", "initial"), "the shorthand has four longhands");
        Check.equal("unset", v("color", "UNSET", "color"), "keywords are case-insensitive");
        Check.equal("revert", v("display", "revert", "display"), "revert");
        Check.isTrue(count("all", "unset") > 150, "all reaches nearly every longhand");
        Check.equal("INVALID", v("all", "red", "color"), "all accepts only CSS-wide keywords");
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        Check.equal(PropertyRegistry.OK, reg.parse("margin", "var(--m) 2px", out), "var() in a shorthand is accepted unparsed");
        Check.equal(4, out.size(), "every longhand is pending");
        Check.isTrue(out.get(Integer.valueOf(Prop.MARGIN_TOP)) instanceof CssPending, "pending value");
        Check.equal("var(--m) 2px", out.get(Integer.valueOf(Prop.MARGIN_LEFT)).toCssText(), "the written text is kept");
        out.clear();
        Check.equal(PropertyRegistry.OK, reg.parse("color", "var(--c, red)", out), "var() with a fallback");
        Check.equal(PropertyRegistry.UNKNOWN, reg.parse("nothing", "var(--c)", out), "var() does not make an unknown property known");
    }

    /**
     * Four-sided shorthands. 네 방향 단축 속성.
     */
    public void testBoxShorthands() {
        Check.equal("1px", v("margin", "1px", "margin-bottom"), "one value");
        Check.equal("2px", v("margin", "1px 2px", "margin-left"), "two values, left");
        Check.equal("1px", v("margin", "1px 2px", "margin-bottom"), "two values, bottom");
        Check.equal("3px", v("margin", "1px 2px 3px", "margin-bottom"), "three values, bottom");
        Check.equal("2px", v("margin", "1px 2px 3px", "margin-left"), "three values, left");
        Check.equal("4px", v("margin", "1px 2px 3px 4px", "margin-left"), "four values, left");
        Check.equal("INVALID", v("margin", "1px 2px 3px 4px 5px", "margin-left"), "five values");
        Check.equal("INVALID", v("margin", "1px bad", "margin-left"), "a bad value drops the declaration");
        Check.equal("auto", v("margin", "0 auto", "margin-right"), "auto margin");
        Check.equal("INVALID", v("padding", "-1px", "padding-top"), "negative padding");
        Check.equal("thin", v("border-width", "thin thick", "border-top-width"), "border-width, top");
        Check.equal("thick", v("border-width", "thin thick", "border-left-width"), "border-width, left");
        Check.equal("green", v("border-color", "red green blue", "border-left-color"), "border-color, left");
        Check.equal("dotted", v("border-style", "dotted", "border-bottom-style"), "border-style");
        Check.equal("10px", v("inset", "10px", "left"), "inset");
        Check.equal("auto", v("inset", "10px auto", "left"), "inset two values");
    }

    /**
     * The border shorthands. border 단축 속성.
     */
    public void testBorderShorthands() {
        Check.equal("1px", v("border", "1px solid red", "border-left-width"), "border width, left side");
        Check.equal("solid", v("border", "1px solid red", "border-top-style"), "border style");
        Check.equal("red", v("border", "1px solid red", "border-bottom-color"), "border color");
        Check.equal("initial", v("border", "1px solid red", "border-image-source"), "border resets the image");
        Check.equal("red", v("border", "red 1px solid", "border-top-color"), "any order");
        Check.equal("initial", v("border", "solid", "border-top-width"), "missing width is initial");
        Check.equal("none", v("border", "none", "border-top-style"), "border: none");
        Check.equal("INVALID", v("border", "1px solid red foo", "border-top-color"), "extra component");
        Check.equal("INVALID", v("border", "1px 2px", "border-top-width"), "a part cannot repeat");
        Check.equal("INVALID", v("border", "", "border-top-width"), "empty value");
        Check.equal("2px", v("border-top", "2px dashed", "border-top-width"), "one-sided border");
        Check.equal("MISSING", v("border-top", "2px dashed", "border-left-width"), "other sides untouched");
        Check.equal(17, count("border", "1px solid red"), "border sets 12 side longhands and 5 image longhands");
        Check.equal(3, count("border-left", "1px solid red"), "a side sets three longhands");
        Check.equal("10px", v("border-radius", "10px", "border-bottom-left-radius"), "radius, one value");
        Check.equal("20px", v("border-radius", "10px 20px", "border-top-right-radius"), "radius, two values");
        Check.equal("10px 5px", v("border-radius", "10px / 5px", "border-top-left-radius"), "elliptical radius is kept as a pair");
        Check.equal("INVALID", v("border-radius", "-1px", "border-top-left-radius"), "negative radius");
        Check.equal("10px", v("border-radius", "10px 20px 30px", "border-top-left-radius"), "radius, three values");
        Check.equal("20px", v("border-radius", "10px 20px 30px", "border-bottom-left-radius"), "radius, three values, bottom left");
        Check.equal("2px", v("outline", "2px solid red", "outline-width"), "outline");
        Check.equal("INVALID", v("outline", "hidden", "outline-style"), "outline style has no hidden");
    }

    /**
     * The font shorthand. font 단축 속성.
     */
    public void testFont() {
        Check.equal("12px", v("font", "12px/1.5 Arial, sans-serif", "font-size"), "size");
        Check.equal("1.5", v("font", "12px/1.5 Arial, sans-serif", "line-height"), "line height");
        Check.equal("Arial, sans-serif", v("font", "12px/1.5 Arial, sans-serif", "font-family"), "family");
        Check.equal("italic", v("font", "italic small-caps bold 14px serif", "font-style"), "style");
        Check.equal("small-caps", v("font", "italic small-caps bold 14px serif", "font-variant-caps"), "variant");
        Check.equal("bold", v("font", "italic small-caps bold 14px serif", "font-weight"), "weight");
        Check.equal("initial", v("font", "14px serif", "font-weight"), "unwritten parts are initial");
        Check.equal("initial", v("font", "14px serif", "line-height"), "unwritten line height is initial");
        Check.equal("condensed", v("font", "normal normal 400 condensed 1em/normal \"A B\"", "font-stretch"), "stretch keyword");
        Check.equal("400", v("font", "normal normal 400 condensed 1em/normal \"A B\"", "font-weight"), "numeric weight");
        Check.equal("INVALID", v("font", "bold 14px", "font-size"), "the family is required");
        Check.equal("INVALID", v("font", "serif", "font-size"), "the size is required");
        Check.equal("INVALID", v("font", "12px/ Arial", "font-size"), "a slash needs a line height");
        Check.equal("13px", v("font", "caption", "font-size"), "system font size");
        Check.equal("sans-serif", v("font", "menu", "font-family"), "system font family");
        Check.equal("initial", v("font", "12px serif", "font-kerning"), "font resets kerning");
    }

    /**
     * The flex family. flex 계열.
     */
    public void testFlex() {
        Check.equal("1", v("flex", "1", "flex-grow"), "flex: 1 grows");
        Check.equal("1", v("flex", "1", "flex-shrink"), "flex: 1 shrinks");
        Check.equal("0%", v("flex", "1", "flex-basis"), "flex: 1 has a zero basis");
        Check.equal("0", v("flex", "none", "flex-grow"), "flex: none");
        Check.equal("auto", v("flex", "none", "flex-basis"), "flex: none basis");
        Check.equal("1", v("flex", "auto", "flex-grow"), "flex: auto");
        Check.equal("3", v("flex", "2 3 10px", "flex-shrink"), "flex with all parts");
        Check.equal("10px", v("flex", "2 3 10px", "flex-basis"), "flex basis");
        Check.equal("10px", v("flex", "10px", "flex-basis"), "flex: basis only");
        Check.equal("1", v("flex", "10px", "flex-grow"), "flex: basis only grows");
        Check.equal("0px", v("flex", "1 1 0", "flex-basis"), "a third unitless zero is a basis");
        Check.equal("2", v("flex", "2 1", "flex-grow"), "two factors");
        Check.equal("INVALID", v("flex", "1 2 3 4", "flex-grow"), "too many parts");
        Check.equal("INVALID", v("flex", "-1", "flex-grow"), "negative factor");
        Check.equal("column", v("flex-flow", "column wrap", "flex-direction"), "flex-flow");
        Check.equal("wrap", v("flex-flow", "wrap column", "flex-wrap"), "flex-flow any order");
        Check.equal("center", v("place-content", "center space-between", "align-content"), "place-content align");
        Check.equal("space-between", v("place-content", "center space-between", "justify-content"), "place-content justify");
        Check.equal("10px", v("gap", "10px 20px", "row-gap"), "gap row");
        Check.equal("20px", v("gap", "10px 20px", "column-gap"), "gap column");
        Check.equal("10px", v("gap", "10px", "column-gap"), "gap with one value");
        Check.equal("hidden", v("overflow", "hidden auto", "overflow-x"), "overflow x");
        Check.equal("auto", v("overflow", "hidden auto", "overflow-y"), "overflow y");
        Check.equal("scroll", v("overflow", "scroll", "overflow-y"), "overflow with one value");
    }

    /**
     * The background shorthand. background 단축 속성.
     */
    public void testBackground() {
        Check.equal("red", v("background", "red", "background-color"), "color only");
        Check.equal("none", v("background", "red", "background-image"), "image defaults to none");
        Check.equal("repeat", v("background", "red", "background-repeat"), "repeat defaults");
        Check.equal("url(\"a.png\")", v("background", "url(a.png) no-repeat center / cover red", "background-image"), "image");
        Check.equal("no-repeat", v("background", "url(a.png) no-repeat center / cover red", "background-repeat"), "repeat");
        Check.equal("center", v("background", "url(a.png) no-repeat center / cover red", "background-position"), "position");
        Check.equal("cover", v("background", "url(a.png) no-repeat center / cover red", "background-size"), "size");
        Check.equal("red", v("background", "url(a.png) no-repeat center / cover red", "background-color"), "color");
        Check.equal("url(\"a.png\"), url(\"b.png\")", v("background", "url(a.png) top left, url(b.png) 10px 20px / 50% repeat-x blue", "background-image"),
                "two layers, images");
        Check.equal("blue", v("background", "url(a.png) top left, url(b.png) 10px 20px / 50% repeat-x blue", "background-color"), "the color of the last layer");
        Check.equal("auto, 50%", v("background", "url(a.png) top left, url(b.png) 10px 20px / 50% repeat-x blue", "background-size"), "two layers, sizes");
        Check.equal("INVALID", v("background", "red, blue", "background-color"), "a color is only allowed in the last layer");
        Check.equal("padding-box", v("background", "red", "background-origin"), "origin default");
        Check.equal("content-box", v("background", "content-box red", "background-origin"), "one box sets the origin");
        Check.equal("content-box", v("background", "content-box red", "background-clip"), "one box sets the clip as well");
        Check.equal("border-box", v("background", "padding-box border-box", "background-clip"), "two boxes: the second is the clip");
        Check.equal("INVALID", v("background", "red red", "background-color"), "two colors");
        Check.equal("INVALID", v("background", "foo", "background-color"), "garbage");
        Check.equal("INVALID", v("background", "", "background-color"), "empty");
    }

    /**
     * Transition, animation and the other list shorthands. transition, animation과 그 밖의 목록 단축 속성.
     */
    public void testAnimationShorthands() {
        Check.equal("opacity, color", v("transition", "opacity 1s ease-in 2s, color .5s", "transition-property"), "transition properties");
        Check.equal("1s, 0.5s", v("transition", "opacity 1s ease-in 2s, color .5s", "transition-duration"), "transition durations");
        Check.equal("ease-in, ease", v("transition", "opacity 1s ease-in 2s, color .5s", "transition-timing-function"), "transition easing");
        Check.equal("2s, 0s", v("transition", "opacity 1s ease-in 2s, color .5s", "transition-delay"), "transition delays");
        Check.equal("all", v("transition", "1s", "transition-property"), "the property defaults to all");
        Check.equal("INVALID", v("transition", "opacity 1s 2s 3s", "transition-delay"), "three times");
        Check.equal("spin", v("animation", "spin 2s linear infinite", "animation-name"), "animation name");
        Check.equal("linear", v("animation", "spin 2s linear infinite", "animation-timing-function"), "animation easing");
        Check.equal("infinite", v("animation", "spin 2s linear infinite", "animation-iteration-count"), "animation iterations");
        Check.equal("alternate", v("animation", "2s alternate spin", "animation-direction"), "animation direction");
        Check.equal("forwards", v("animation", "spin 1s forwards", "animation-fill-mode"), "animation fill mode");
        Check.equal("paused", v("animation", "spin 1s paused", "animation-play-state"), "animation play state");
        Check.equal("1s", v("animation", "spin 1s 2s", "animation-duration"), "first time is the duration");
        Check.equal("2s", v("animation", "spin 1s 2s", "animation-delay"), "second time is the delay");
        Check.equal("none", v("list-style", "none", "list-style-type"), "list-style: none");
        Check.equal("inside", v("list-style", "square inside", "list-style-position"), "list-style position");
        Check.equal("underline", v("text-decoration", "underline wavy red", "text-decoration-line"), "text-decoration line");
        Check.equal("wavy", v("text-decoration", "underline wavy red", "text-decoration-style"), "text-decoration style");
        Check.equal("red", v("text-decoration", "underline wavy red", "text-decoration-color"), "text-decoration color");
        Check.equal("3", v("columns", "3 100px", "column-count"), "columns count");
        Check.equal("100px", v("columns", "3 100px", "column-width"), "columns width");
        Check.equal("2px", v("column-rule", "2px solid red", "column-rule-width"), "column rule");
    }

    /**
     * Aliases map vendor-prefixed names to the standard ones. 접두사 붙은 이름은 표준 이름으로 연결된다.
     */
    public void testAliases() {
        Check.equal("1px 1px red", v("-webkit-box-shadow", "1px 1px red", "box-shadow"), "-webkit-box-shadow");
        Check.equal("break-word", v("word-wrap", "break-word", "overflow-wrap"), "word-wrap");
        Check.equal("border-box", v("-moz-box-sizing", "border-box", "box-sizing"), "-moz-box-sizing");
        Check.equal("5px", v("-webkit-border-radius", "5px", "border-top-left-radius"), "-webkit-border-radius");
        Check.equal("rotate(10deg)", v("-ms-transform", "rotate(10deg)", "transform"), "-ms-transform");
        Check.equal("10px", v("grid-gap", "10px", "row-gap"), "grid-gap");
        Check.isTrue(reg.names().contains("margin") && reg.names().contains("color") && reg.names().contains("all"), "the names list");
    }

    /**
     * Component values may arrive with surrounding whitespace and comments. 구성 요소 값에 공백과 주석이 붙어도 된다.
     */
    public void testWhitespaceTolerance() {
        Check.equal("1px", v("margin", "  1px  /* c */ ", "margin-top"), "whitespace and comments");
        Check.equal("red", v("color", "\n red \n", "color"), "newlines");
    }
}
