package org.duckdns.hjow.swingwebpanel.css;

import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of media queries (plan item P08-06): types, not and only, and/or/not conditions, plain, boolean and range features, units, preferences,
 * unknown features, error recovery and serialization.
 * <p>
 * 미디어 질의 시험(계획 P08-06): 종류, not 과 only, and/or/not 조건, 일반/불리언/범위 특성, 단위, 선호, 알 수 없는 특성, 오류 복구, 직렬화.
 */
public class MediaQueryTest {

    /** An 800 by 600 screen. / 800 x 600 화면. */
    private final MediaEnvironment env = new MediaEnvironment(800, 600);

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public MediaQueryTest() {
    }

    /**
     * Tells whether a query matches the test environment. 질의가 시험 환경과 일치하는지 알려 준다.
     *
     * @param q query text / 질의 문자열
     * @return true if it matches / 일치하면 true
     */
    private boolean m(String q) {
        return MediaQueryList.parse(q).matches(env);
    }

    /**
     * Media types and their modifiers. 미디어 종류와 한정자.
     */
    public void testTypes() {
        Check.isTrue(m(""), "an empty list is all");
        Check.isTrue(m("all"), "all");
        Check.isTrue(m("screen"), "screen");
        Check.isFalse(m("print"), "print");
        Check.isTrue(m("not print"), "not print");
        Check.isFalse(m("not screen"), "not screen");
        Check.isTrue(m("only screen"), "only screen");
        Check.isFalse(m("tty"), "an obsolete type never matches");
        Check.isTrue(m("not tty"), "not of an obsolete type");
        Check.isTrue(m("SCREEN"), "types are case-insensitive");
        env.setMediaType("print");
        Check.isTrue(m("print"), "print environment");
        Check.isFalse(m("screen"), "screen in a print environment");
    }

    /**
     * Plain features and the min- and max- prefixes. 일반 특성과 min-, max- 접두사.
     */
    public void testPlainFeatures() {
        Check.isTrue(m("(min-width: 600px)"), "min-width");
        Check.isFalse(m("(min-width: 900px)"), "min-width too large");
        Check.isTrue(m("(max-width: 800px)"), "max-width at the boundary");
        Check.isFalse(m("(max-width: 799px)"), "max-width just below");
        Check.isTrue(m("(width: 800px)"), "exact width");
        Check.isTrue(m("(min-height: 600px) and (max-height: 600px)"), "height");
        Check.isTrue(m("(min-width: 40em)"), "em is 16px in media queries (640px)");
        Check.isFalse(m("(min-width: 60em)"), "960px is wider than the viewport");
        Check.isTrue(m("(min-width: 0)"), "unitless zero");
        Check.isTrue(m("(min-width: 600PX)"), "units are case-insensitive");
        Check.isTrue(m("(MIN-WIDTH: 600px)"), "feature names are case-insensitive");
        Check.isTrue(m("(min-device-width: 800px)"), "device width defaults to the viewport");
        env.setDeviceSize(1920, 1080);
        Check.isTrue(m("(min-device-width: 1920px)"), "device width");
        Check.isTrue(m("(device-aspect-ratio: 16/9)"), "device aspect ratio");
    }

    /**
     * Range syntax. 범위 문법.
     */
    public void testRanges() {
        Check.isTrue(m("(width > 799px)"), "width greater");
        Check.isTrue(m("(width >= 800px)"), "width greater or equal");
        Check.isFalse(m("(width > 800px)"), "width not greater");
        Check.isTrue(m("(width <= 800px)"), "width less or equal");
        Check.isTrue(m("(800px >= width)"), "value first");
        Check.isFalse(m("(400px <= width <= 700px)"), "between, outside");
        Check.isTrue(m("(400px <= width < 900px)"), "between, inside");
        Check.isTrue(m("(400px < width <= 800px)"), "between with the upper bound");
        Check.isFalse(m("(400px < width = 800px)"), "equals cannot be part of a double range");
        Check.isTrue(m("(width = 800px)"), "equals");
        Check.isFalse(m("(height < 500px)"), "height range");
        Check.isTrue(m("(aspect-ratio > 1)"), "aspect ratio against a number");
        Check.isTrue(m("(aspect-ratio: 4/3)"), "aspect ratio");
        Check.isFalse(m("(min-aspect-ratio: 16/9)"), "wider than the viewport");
        Check.isTrue(m("(max-aspect-ratio: 16/9)"), "narrower than 16/9");
    }

    /**
     * Boolean, discrete and preference features. 불리언, 이산, 선호 특성.
     */
    public void testDiscreteFeatures() {
        Check.isTrue(m("(color)"), "color");
        Check.isFalse(m("(monochrome)"), "monochrome");
        Check.isTrue(m("(min-color: 8)"), "color bits");
        Check.isTrue(m("(orientation: landscape)"), "landscape");
        Check.isFalse(m("(orientation: portrait)"), "portrait");
        env.setViewport(600, 800);
        Check.isTrue(m("(orientation: portrait)"), "portrait after resizing");
        Check.isTrue(m("(hover)"), "hover capability");
        Check.isTrue(m("(hover: hover) and (pointer: fine)"), "mouse");
        Check.isFalse(m("(hover: none)"), "no hover");
        env.setInput("none", "coarse");
        Check.isTrue(m("(hover: none) and (pointer: coarse)"), "touch");
        Check.isFalse(m("(hover)"), "hover boolean is false for none");
        Check.isTrue(m("(prefers-color-scheme: light)"), "light");
        Check.isFalse(m("(prefers-color-scheme: dark)"), "not dark");
        env.setColorScheme("dark");
        Check.isTrue(m("(prefers-color-scheme: dark)"), "dark after the change");
        Check.isTrue(m("(prefers-reduced-motion: no-preference)"), "motion");
        Check.isFalse(m("(prefers-reduced-motion)"), "boolean no-preference");
        env.setReducedMotion("reduce");
        Check.isTrue(m("(prefers-reduced-motion: reduce)"), "reduce");
        Check.isTrue(m("(scripting: enabled)"), "scripting");
        env.setScripting(false);
        Check.isTrue(m("(scripting: none)"), "scripting disabled");
        Check.isTrue(m("(display-mode: browser)"), "display mode");
        Check.isTrue(m("(forced-colors: none)"), "forced colors");
        Check.isTrue(m("(update: fast) and (scan: progressive)"), "update and scan");
    }

    /**
     * Resolution features and units. 해상도 특성과 단위.
     */
    public void testResolution() {
        Check.isTrue(m("(min-resolution: 1dppx)"), "dppx");
        Check.isTrue(m("(min-resolution: 96dpi)"), "dpi");
        Check.isFalse(m("(min-resolution: 2dppx)"), "double density");
        Check.isFalse(m("(-webkit-min-device-pixel-ratio: 2)"), "prefixed ratio");
        env.setResolution(2);
        Check.isTrue(m("(min-resolution: 192dpi)"), "dpi after the change");
        Check.isTrue(m("(resolution >= 2dppx)"), "range resolution");
        Check.isTrue(m("(-webkit-min-device-pixel-ratio: 2)"), "prefixed ratio after the change");
        Check.isTrue(m("(min-resolution: 2x)"), "x unit");
        Check.isTrue(m("(min-resolution: 75dpcm)"), "dpcm");
    }

    /**
     * Logical operators and parentheses. 논리 연산자와 괄호.
     */
    public void testLogic() {
        Check.isTrue(m("screen and (min-width: 600px) and (max-width: 900px)"), "type and conditions");
        Check.isFalse(m("screen and (min-width: 900px)"), "one false condition");
        Check.isFalse(m("print and (min-width: 600px)"), "wrong type");
        Check.isTrue(m("(min-width: 900px) or (max-width: 900px)"), "or, second true");
        Check.isFalse(m("(min-width: 900px) or (max-width: 100px)"), "or, both false");
        Check.isTrue(m("not (min-width: 900px)"), "not");
        Check.isFalse(m("not (min-width: 600px)"), "not of a true condition");
        Check.isFalse(m("((min-width: 600px) and (max-width: 900px)) or print"), "a media type cannot be an operand of or");
        Check.isTrue(m("((min-width: 900px) or (max-width: 900px)) and (orientation: landscape)"), "nested or inside and");
        Check.isFalse(m("(min-width: 600px) and (max-width: 900px) or (orientation: portrait)"), "mixing and with or without parentheses is invalid");
        Check.isTrue(m("print, screen"), "any query of the list");
        Check.isFalse(m("print, tty"), "no query matches");
        Check.isTrue(m("screen, print and (min-width: 9999px)"), "first query matches");
        Check.isFalse(m("not screen and (min-width: 600px)"), "not applies to the whole query");
        Check.isTrue(m("not screen and (min-width: 900px)"), "not of a false query");
    }

    /**
     * Unknown features and syntax errors. 알 수 없는 특성과 문법 오류.
     */
    public void testErrors() {
        Check.isFalse(m("(unknown-feature)"), "unknown feature is false");
        Check.isFalse(m("(unknown-feature: 1)"), "unknown plain feature");
        Check.isFalse(m("not (unknown-feature: 1)"), "an unknown feature cannot be negated into true");
        Check.isTrue(m("(unknown-feature: 1), screen"), "other queries of the list still work");
        Check.isFalse(m("screen and"), "a dangling and");
        Check.isFalse(m("and (min-width: 1px)"), "a leading and");
        Check.isFalse(m("(min-width: red)"), "a keyword for a length feature");
        Check.isFalse(m("(orientation: 1)"), "a number for a keyword feature");
        Check.isFalse(m("(min-orientation: landscape)"), "min- prefix on a discrete feature");
        Check.isFalse(m("(width: 5)"), "a number where a length is required");
        Check.isFalse(m("foo bar"), "two words");
        Check.isTrue(m("foo bar, screen"), "an invalid query becomes not all");
        Check.isTrue(m("not foo bar, screen"), "an invalid query is not all, regardless of not");
        Check.isFalse(m("style(--x: 1)"), "a function is unknown");
        Check.isFalse(m("()"), "empty parentheses");
        Check.isTrue(m("(min-width: 600px"), "an unclosed parenthesis is closed at the end");
    }

    /**
     * Serialization of query lists. 질의 목록의 직렬화.
     */
    public void testSerialization() {
        Check.equal("screen and (min-width: 600px), print", MediaQueryList.parse("screen   and (min-width:600px) , print").toCssText(), "list");
        Check.equal("not all", MediaQueryList.parse("foo bar").toCssText(), "an invalid query");
        Check.equal("all", MediaQueryList.parse("").toCssText(), "the empty list");
        Check.equal("not screen", MediaQueryList.parse("NOT Screen").toCssText(), "case is normalized");
        Check.equal("(width >= 400px)", MediaQueryList.parse("(width>=400px)").toCssText(), "range");
        Check.equal("(color)", MediaQueryList.parse("(color)").toCssText(), "boolean feature");
        Check.equal("(min-width: 1px) and (max-width: 2px)", MediaQueryList.parse("(min-width: 1px) AND (max-width: 2px)").toCssText(), "and");
        Check.equal(2, MediaQueryList.parse("screen, print").size(), "size");
        Check.equal("print", MediaQueryList.parse("print").toString(), "toString");
    }

    /**
     * Environments can be compared and copied. 환경은 비교하고 복사할 수 있다.
     */
    public void testEnvironment() {
        MediaEnvironment a = new MediaEnvironment(800, 600);
        MediaEnvironment b = a.copy();
        Check.isTrue(a.sameAs(b), "a copy is the same");
        b.setViewport(801, 600);
        Check.isFalse(a.sameAs(b), "a different size");
        Check.isTrue(a.sameAs(a.copy()), "the original is unchanged by changes to the copy");
        Check.isFalse(a.sameAs(null), "null");
        b = a.copy();
        b.setColorScheme("dark");
        Check.isFalse(a.sameAs(b), "a different preference");
    }
}
