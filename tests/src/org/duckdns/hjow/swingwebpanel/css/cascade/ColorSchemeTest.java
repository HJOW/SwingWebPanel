package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the color scheme (plan item P10-08, CSS Color Adjustment Module Level 1, https://www.w3.org/TR/css-color-adjust-1/, and
 * <code>light-dark()</code> of CSS Color Level 5): <code>color-scheme</code> decides the scheme of an element (<code>dark</code> alone, or
 * <code>light dark</code> if the user agent prefers dark, which <code>prefers-color-scheme</code> of the media environment says), the system colors
 * and <code>light-dark()</code> take the value of that scheme, the root element of a dark scheme has the light <code>CanvasText</code> as its text
 * color, and the scheme is inherited.
 * <p>
 * 색 구성표 시험(계획 P10-08, CSS Color Adjustment Module Level 1, https://www.w3.org/TR/css-color-adjust-1/, 그리고 CSS Color Level 5 의 <code>light-dark()</code>):
 * <code>color-scheme</code> 이 요소의 구성표를 정하고(<code>dark</code> 만 있거나, 사용자 에이전트가 어둡게를 선호하면 <code>light dark</code>. 선호는 미디어 환경의
 * <code>prefers-color-scheme</code>), 시스템 색과 <code>light-dark()</code> 는 그 구성표의 값이 되며, 어두운 구성표의 루트 요소는 밝은 <code>CanvasText</code> 가 글자색이고,
 * 구성표는 상속된다.
 */
public class ColorSchemeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ColorSchemeTest() {
    }

    /**
     * Returns the computed style of the element <code>t</code> of a page.
     * <p>
     * 페이지의 요소 <code>t</code> 의 계산 스타일을 반환한다.
     *
     * @param css       the style sheet / 스타일시트
     * @param body      the body markup / 본문 마크업
     * @param preferred the preferred scheme of the environment / 환경의 선호 구성표
     * @return the computed style / 계산 스타일
     */
    private static ComputedStyle style(String css, String body, String preferred) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + body + "</body></html>",
                "http://t.test/page.html", null, true);
        MediaEnvironment env = new MediaEnvironment(300, 200);
        env.setColorScheme(preferred);
        CssEngine e = new CssEngine(doc, null, env);
        e.loadStyleSheets();
        return e.getComputedStyle(doc.getElementById("t"));
    }

    /**
     * Returns the color of a property as 0xRRGGBB.
     * <p>
     * 속성의 색을 0xRRGGBB 로 반환한다.
     *
     * @param s  the style / 스타일
     * @param id the property / 속성
     * @return the color / 색
     */
    private static int rgb(ComputedStyle s, int id) {
        return s.color(id) & 0xFFFFFF;
    }

    /**
     * Without <code>color-scheme</code> the scheme is light: the system colors have their light values and <code>light-dark()</code> its first.
     * <p>
     * <code>color-scheme</code> 이 없으면 구성표는 밝다: 시스템 색은 밝은 값이고 <code>light-dark()</code> 는 첫 값이다.
     */
    public void testLightByDefault() {
        ComputedStyle s = style("#t { color: canvastext; background-color: canvas; border-color: light-dark(red, blue); outline-color: linktext }", "<div id=t>x</div>", "light");
        Check.equal(0x000000, rgb(s, Prop.COLOR), "CanvasText is black");
        Check.equal(0xFFFFFF, rgb(s, Prop.BACKGROUND_COLOR), "Canvas is white");
        Check.equal(0xFF0000, rgb(s, Prop.BORDER_TOP_COLOR), "light-dark() is its first color");
        Check.equal(0x0000EE, rgb(s, Prop.OUTLINE_COLOR), "LinkText");
        Check.isFalse(s.usesDarkScheme(), "the scheme is light");
        Check.isFalse(style("#t { color-scheme: light }", "<div id=t>x</div>", "dark").usesDarkScheme(), "light stays light although the user agent prefers dark");
        Check.isFalse(style("#t { color-scheme: normal }", "<div id=t>x</div>", "dark").usesDarkScheme(), "normal is light");
    }

    /**
     * <code>color-scheme: dark</code> gives the dark values; the root element of the scheme has the light text color; the scheme is inherited.
     * <p>
     * <code>color-scheme: dark</code> 는 어두운 값을 준다. 그 구성표의 루트 요소는 밝은 글자색을 가진다. 구성표는 상속된다.
     */
    public void testDark() {
        ComputedStyle s = style(":root { color-scheme: dark } #t { background-color: canvas; border-color: light-dark(red, blue); outline-color: linktext }",
                "<div id=t>x</div>", "light");
        Check.isTrue(s.usesDarkScheme(), "inherited from the root element");
        Check.equal(0x121212, rgb(s, Prop.BACKGROUND_COLOR), "Canvas is dark");
        Check.equal(0x0000FF, rgb(s, Prop.BORDER_TOP_COLOR), "light-dark() is its second color");
        Check.equal(0x9E9EFF, rgb(s, Prop.OUTLINE_COLOR), "LinkText is light blue");
        Check.equal(0xFFFFFF, rgb(s, Prop.COLOR), "the text color of the dark root is light and inherited");
        ComputedStyle root = style(":root { color-scheme: dark }", "<div id=t>x</div>", "light");
        Check.equal(0xFFFFFF, rgb(root, Prop.COLOR), "an undeclared color of the dark root");
        ComputedStyle declared = style(":root { color-scheme: dark; color: #123456 }", "<div id=t>x</div>", "light");
        Check.equal(0x123456, rgb(declared, Prop.COLOR), "a declared color wins");
    }

    /**
     * <code>light dark</code> follows the preference of the user agent, and an element can change the scheme of its subtree.
     * <p>
     * <code>light dark</code> 는 사용자 에이전트의 선호를 따르고, 요소는 자기 서브트리의 구성표를 바꿀 수 있다.
     */
    public void testPreferenceAndOverride() {
        String css = ":root { color-scheme: light dark } #t { border-color: light-dark(red, blue) }";
        Check.equal(0xFF0000, rgb(style(css, "<div id=t>x</div>", "light"), Prop.BORDER_TOP_COLOR), "light preference");
        Check.equal(0x0000FF, rgb(style(css, "<div id=t>x</div>", "dark"), Prop.BORDER_TOP_COLOR), "dark preference");
        Check.isTrue(style(css, "<div id=t>x</div>", "dark").usesDarkScheme(), "the flag follows");
        String sub = ":root { color-scheme: dark } .l { color-scheme: light } #t { background-color: canvas }";
        Check.equal(0xFFFFFF, rgb(style(sub, "<div class=l><p id=t>x</p></div>", "dark"), Prop.BACKGROUND_COLOR), "a light subtree of a dark page");
        Check.equal(0x121212, rgb(style(sub, "<p id=t>x</p>", "light"), Prop.BACKGROUND_COLOR), "outside it the page is dark");
    }

    /**
     * Styles that are copied (an overlay, a changed value) keep the scheme.
     * <p>
     * 복사한 스타일(덮은 스타일, 바꾼 값)은 구성표를 유지한다.
     */
    public void testCopiesKeepTheScheme() {
        ComputedStyle s = style(":root { color-scheme: dark }", "<div id=t>x</div>", "light");
        Check.isTrue(s.with(Prop.OPACITY, new org.duckdns.hjow.swingwebpanel.css.values.CssNumber(0.5, false)).usesDarkScheme(), "with()");
    }
}
