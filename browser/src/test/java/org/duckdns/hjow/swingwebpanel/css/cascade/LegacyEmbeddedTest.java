package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the legacy presentation attributes of the horizontal rule, the embedded and replaced elements, <code>br</code>,
 * paragraphs and headings, <code>fieldset</code> and <code>legend</code> (plan item P07-07-b-8, HTML5 2014 sections 10.3.12, 10.3.13,
 * 10.4.1 to 10.4.3 and the <code>br</code> and alignment rules of 10.3.3 and 10.3.4). The computed styles are checked first, then the
 * boxes and the painted pixels.
 * <p>
 * 가로줄, 삽입/치환 요소, <code>br</code>, 문단과 제목, <code>fieldset</code> 과 <code>legend</code> 의 이전 방식 표현 속성 시험(계획 P07-07-b-8, HTML5 2014
 * §10.3.12, §10.3.13, §10.4.1~10.4.3 및 §10.3.3, §10.3.4 의 <code>br</code>·정렬 규칙). 계산 스타일을 먼저 확인하고 상자와 화소를 확인한다.
 */
public class LegacyEmbeddedTest {

    /** The shared fixture. / 공용 도구. */
    private final LegacyFixture fx = new LegacyFixture();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyEmbeddedTest() {
    }

    /**
     * Releases the CSS engine after each case.
     * <p>
     * 각 사례 뒤에 CSS 엔진을 해제한다.
     */
    public void tearDown() {
        fx.dispose();
    }

    /**
     * Returns the four border widths of an element as <code>top right bottom left</code> in pixels.
     * <p>
     * 요소의 네 border 두께를 <code>top right bottom left</code> 픽셀로 반환한다.
     *
     * @param id the id / id
     * @return the widths, for example <code>1 1 1 1</code> / 두께. 예: <code>1 1 1 1</code>
     */
    private String widths(String id) {
        ComputedStyle s = fx.style(id);
        return fmt(s.px(Prop.BORDER_TOP_WIDTH)) + " " + fmt(s.px(Prop.BORDER_RIGHT_WIDTH)) + " " + fmt(s.px(Prop.BORDER_BOTTOM_WIDTH)) + " "
                + fmt(s.px(Prop.BORDER_LEFT_WIDTH));
    }

    /**
     * Returns the four border styles of an element as <code>top right bottom left</code>.
     * <p>
     * 요소의 네 border 스타일을 <code>top right bottom left</code> 로 반환한다.
     *
     * @param id the id / id
     * @return the style keywords / 스타일 키워드
     */
    private String styles(String id) {
        ComputedStyle s = fx.style(id);
        return s.keyword(Prop.BORDER_TOP_STYLE) + " " + s.keyword(Prop.BORDER_RIGHT_STYLE) + " " + s.keyword(Prop.BORDER_BOTTOM_STYLE) + " "
                + s.keyword(Prop.BORDER_LEFT_STYLE);
    }

    /**
     * Formats a pixel number without a needless fraction.
     * <p>
     * 픽셀 숫자를 불필요한 소수 없이 만든다.
     *
     * @param v the number / 숫자
     * @return the text / 문자열
     */
    private static String fmt(double v) {
        return v == Math.floor(v) ? Long.toString((long) v) : Double.toString(v);
    }

    /**
     * An <code>hr</code> keeps the 1px inset rule by default; a <code>color</code> or <code>noshade</code> attribute makes it solid.
     * <p>
     * <code>hr</code> 은 기본으로 1px inset 선이고 <code>color</code> 나 <code>noshade</code> 속성이 있으면 실선이 된다.
     */
    public void testRuleDefaultsAndSolid() {
        fx.init("<hr id=a><hr id=b color=red><hr id=c noshade><hr id=d color=''><hr id=e color=red noshade>", "");
        Check.equal("inset inset inset inset", styles("a"), "default style");
        Check.equal("1 1 1 1", widths("a"), "default width");
        Check.equal(0xFF808080, fx.style("a").color(Prop.COLOR), "default color");
        Check.equal("solid solid solid solid", styles("b"), "color makes the rule solid");
        Check.equal(0xFFFF0000, fx.style("b").color(Prop.COLOR), "color attribute colors the rule");
        Check.equal(0xFFFF0000, fx.style("b").color(Prop.BORDER_TOP_COLOR), "the border follows the color");
        Check.equal("solid solid solid solid", styles("c"), "noshade makes the rule solid");
        Check.equal(0xFF808080, fx.style("c").color(Prop.COLOR), "noshade keeps the gray");
        Check.equal("solid solid solid solid", styles("d"), "the presence of color is enough, even when its value is an error");
        Check.equal(0xFF808080, fx.style("d").color(Prop.COLOR), "an empty color is an error");
        Check.equal("solid solid solid solid", styles("e"), "both");
    }

    /**
     * The <code>size</code> of an <code>hr</code> is half of the border width of a solid rule, and a content height of size minus two
     * for the shaded rule; <code>size=1</code> removes the bottom border of the shaded rule.
     * <p>
     * <code>hr</code> 의 <code>size</code> 는 실선일 때 border 두께의 절반이고 음영 선일 때 size 에서 2 를 뺀 내용 높이이며, <code>size=1</code> 은 음영 선의 아래 border 를 없앤다.
     */
    public void testRuleSize() {
        fx.init("<hr id=a noshade size=10><hr id=b color=red size=4><hr id=c noshade size=3><hr id=d noshade size=0><hr id=e size=10>"
                + "<hr id=f size=1><hr id=g size=2><hr id=h size=0><hr id=i size=abc noshade><hr id=j size=-4><hr id=k size=' +6x' noshade>", "");
        Check.equal("5 5 5 5", widths("a"), "noshade size=10");
        Check.equal("2 2 2 2", widths("b"), "color size=4");
        Check.equal("1.5 1.5 1.5 1.5", widths("c"), "noshade size=3 is 1.5px");
        Check.equal("0 0 0 0", widths("d"), "noshade size=0");
        Check.near(8, fx.style("e").px(Prop.HEIGHT), 0.001, "shaded size=10 has a content height of 8");
        Check.equal("1 1 1 1", widths("e"), "shaded size keeps the 1px inset border");
        Check.equal("1 1 0 1", widths("f"), "size=1 removes the bottom border");
        Check.near(0, fx.style("g").px(Prop.HEIGHT), 0.001, "size=2 has a content height of 0");
        Check.equal("1 1 1 1", widths("h"), "size=0 does nothing");
        Check.equal("1 1 1 1", widths("i"), "an unparsable size does nothing");
        Check.equal("1 1 1 1", widths("j"), "a negative size does nothing");
        Check.equal("3 3 3 3", widths("k"), "leading space, sign and trailing text");
    }

    /**
     * <code>width</code> and <code>align</code> of an <code>hr</code>: the width is a dimension, and the alignment sets the margins.
     * <p>
     * <code>hr</code> 의 <code>width</code> 와 <code>align</code>: 너비는 차원 값이고 정렬은 margin 을 정한다.
     */
    public void testRuleWidthAndAlign() {
        fx.init("<hr id=a width=100 align=left><hr id=b width=50% align=RIGHT><hr id=c width=80 align=center><hr id=d width=60 align=' left'>"
                + "<hr id=e width=abc>", "");
        RenderedPage page = fx.render();
        LayoutBox a = fx.box(page, "a");
        Check.near(100, a.getContentWidth(), 0.01, "width in pixels");
        Check.near(0, a.getX(), 0.01, "align=left keeps the rule at the left edge");
        LayoutBox b = fx.box(page, "b");
        Check.near(100, b.getWidth() - b.getBorderLeft() - b.getBorderRight(), 0.01, "width 50% of the 200px viewport");
        Check.near(200, b.getX() + b.getWidth(), 0.01, "align=RIGHT keeps the rule at the right edge");
        LayoutBox c = fx.box(page, "c");
        Check.near(59, c.getX(), 0.01, "align=center centers the 82px wide rule (80 + borders)");
        LayoutBox d = fx.box(page, "d");
        Check.near(69, d.getX(), 0.01, "a space before the keyword is not a match: the default auto margins center the rule");
        LayoutBox e = fx.box(page, "e");
        Check.near(200, e.getWidth(), 0.01, "an invalid width keeps the automatic width");
    }

    /**
     * The alignment keywords of images and embedded elements: left and right float, the vertical keywords map one to one, and
     * <code>center</code> and <code>middle</code> align the middle of the element with the baseline.
     * <p>
     * 이미지와 삽입 요소의 정렬 키워드: left, right 는 float, 세로 키워드는 일대일 대응, <code>center</code> 와 <code>middle</code> 은 요소의 가운데를 기준선에 맞춘다.
     */
    public void testReplacedAlignment() {
        String[] values = { "left", "right", "top", "baseline", "texttop", "absmiddle", "abscenter", "bottom", "middle", "center", "LEFT", "absbottom", "bogus" };
        String[] floats = { "left", "right", "none", "none", "none", "none", "none", "none", "none", "none", "left", "none", "none" };
        String[] aligns = { "baseline", "baseline", "top", "baseline", "text-top", "middle", "middle", "bottom", "-webkit-baseline-middle",
                "-webkit-baseline-middle", "baseline", "baseline", "baseline" };
        StringBuilder markup = new StringBuilder();
        // 요소 종류와 align 값마다 요소를 하나씩 만든다.
        for (int i = 0; i < values.length; i++) {
            markup.append("<img id=i").append(i).append(" align=").append(values[i]).append(" width=10 height=10>");
            markup.append("<object id=o").append(i).append(" align=").append(values[i]).append("></object>");
            markup.append("<embed id=e").append(i).append(" align=").append(values[i]).append(">");
            markup.append("<iframe id=f").append(i).append(" align=").append(values[i]).append("></iframe>");
            markup.append("<input type=image id=n").append(i).append(" align=").append(values[i]).append(">");
        }
        fx.init(markup.toString(), "");
        String[] prefixes = { "i", "o", "e", "f", "n" };
        // 값마다 다섯 종류의 요소를 확인한다.
        for (int i = 0; i < values.length; i++) {
            for (int k = 0; k < prefixes.length; k++) {
                String id = prefixes[k] + i;
                Check.equal(floats[i], fx.style(id).keyword(Prop.FLOAT), id + " float for align=" + values[i]);
                Check.equal(aligns[i], fx.style(id).keyword(Prop.VERTICAL_ALIGN), id + " vertical-align for align=" + values[i]);
            }
        }
    }

    /**
     * The boxes follow the vertical alignment keywords: the middle of an element with <code>align=middle</code> is on the baseline, the
     * middle of <code>align=absmiddle</code> is half an x-height above it, and a baseline-aligned box stands on the baseline.
     * <p>
     * 상자는 세로 정렬 키워드를 따른다: <code>align=middle</code> 인 요소의 가운데는 기준선 위에 있고, <code>align=absmiddle</code> 의 가운데는 그보다 x 높이의
     * 절반 위에 있으며, 기준선 정렬 상자는 기준선 위에 선다.
     */
    public void testBaselineMiddleGeometry() {
        String box = "display:inline-block;width:20px;height:20px";
        fx.init("<span id=c style='" + box + "'></span><span id=b style='" + box + ";vertical-align:-webkit-baseline-middle'></span>"
                + "<span id=m style='" + box + ";vertical-align:middle'></span><object id=o align=middle style='" + box + "'></object>"
                + "<object id=p align=absmiddle style='" + box + "'></object>", "");
        RenderedPage page = fx.render();
        double baseline = fx.box(page, "c").getY() + fx.box(page, "c").getHeight();
        Check.near(baseline - 10, fx.box(page, "b").getY(), 0.6, "baseline-middle: the middle (10px) is on the baseline");
        Check.near(baseline - 15, fx.box(page, "m").getY(), 0.6, "middle: the middle is a quarter of the font size above the baseline");
        Check.near(baseline - 10, fx.box(page, "o").getY(), 0.6, "object align=middle uses the baseline-middle");
        Check.near(baseline - 15, fx.box(page, "p").getY(), 0.6, "object align=absmiddle uses vertical-align: middle");
    }

    /**
     * A shaded and a solid rule paint as bars of the heights that <code>size</code> gives: a solid, red <code>size=10</code> rule is a
     * ten pixel high red bar of the given width, and the default rule is a two pixel gray inset line.
     * <p>
     * 음영 선과 실선은 <code>size</code> 가 주는 높이의 막대로 그려진다: 빨간 실선 <code>size=10</code> 은 지정한 너비의 10 화소 높이 빨간 막대이고 기본 선은 2 화소 회색 inset 선이다.
     */
    public void testRulePixels() {
        fx.init("<hr color=#ff0000 size=10 width=100 align=left style='margin-top:10px;margin-bottom:0'>"
                + "<hr style='margin-top:20px;margin-bottom:0'>", "");
        BufferedImage image = LegacyFixture.paint(fx.render());
        Check.equal(0xFF0000, image.getRGB(50, 15) & 0xFFFFFF, "inside the red bar");
        Check.equal(0xFFFFFF, image.getRGB(50, 9) & 0xFFFFFF, "above the bar");
        Check.equal(0xFFFFFF, image.getRGB(50, 21) & 0xFFFFFF, "below the bar");
        Check.equal(0xFF0000, image.getRGB(109, 15) & 0xFFFFFF, "the right edge of the 110px wide bar (100 + 2 * 5)");
        Check.equal(0xFFFFFF, image.getRGB(111, 15) & 0xFFFFFF, "outside the bar");
        // 기본 가로줄은 세로 2화소(위 1, 아래 1)의 회색 계열 inset 선이다.
        int top = image.getRGB(100, 40) & 0xFFFFFF;
        int bottom = image.getRGB(100, 41) & 0xFFFFFF;
        int outside = image.getRGB(100, 39) & 0xFFFFFF;
        Check.isTrue(top != 0xFFFFFF && bottom != 0xFFFFFF, "the default rule is two pixels high: " + Integer.toHexString(top) + " " + Integer.toHexString(bottom));
        Check.isTrue(top != bottom, "the inset shading differs between the top and the bottom line");
        Check.equal(0xFFFFFF, outside, "the pixel above the default rule is empty");
    }

    /**
     * <code>hspace</code> and <code>vspace</code> are margins (a length or a percentage) of the elements that the section names, and of
     * none of the others.
     * <p>
     * <code>hspace</code> 와 <code>vspace</code> 는 절이 말하는 요소의 margin(길이 또는 백분율)이고 다른 요소에는 효과가 없다.
     */
    public void testReplacedSpacing() {
        fx.init("<img id=a hspace=5 vspace=7 width=10 height=10><object id=b hspace=3 vspace=4></object><embed id=c hspace=6>"
                + "<iframe id=d vspace=8></iframe><input type=image id=e hspace=2 vspace=9><applet id=f hspace=1></applet>"
                + "<img id=g hspace=10% vspace=abc><video id=h hspace=5 vspace=5></video><canvas id=i hspace=5></canvas>"
                + "<input type=text id=j hspace=5><img id=k hspace=' 12x' vspace=-3>", "");
        Check.near(5, fx.style("a").px(Prop.MARGIN_LEFT), 0.001, "img hspace left");
        Check.near(5, fx.style("a").px(Prop.MARGIN_RIGHT), 0.001, "img hspace right");
        Check.near(7, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "img vspace top");
        Check.near(7, fx.style("a").px(Prop.MARGIN_BOTTOM), 0.001, "img vspace bottom");
        Check.near(3, fx.style("b").px(Prop.MARGIN_LEFT), 0.001, "object hspace");
        Check.near(4, fx.style("b").px(Prop.MARGIN_BOTTOM), 0.001, "object vspace");
        Check.near(6, fx.style("c").px(Prop.MARGIN_RIGHT), 0.001, "embed hspace");
        Check.near(8, fx.style("d").px(Prop.MARGIN_TOP), 0.001, "iframe vspace");
        Check.near(2, fx.style("e").px(Prop.MARGIN_LEFT), 0.001, "input type=image hspace");
        Check.near(9, fx.style("e").px(Prop.MARGIN_BOTTOM), 0.001, "input type=image vspace");
        Check.near(1, fx.style("f").px(Prop.MARGIN_LEFT), 0.001, "applet hspace");
        Check.equal("10%", fx.style("g").getPropertyValue("margin-left"), "a percentage hspace");
        Check.near(0, fx.style("g").px(Prop.MARGIN_TOP), 0.001, "a garbage vspace");
        Check.near(0, fx.style("h").px(Prop.MARGIN_LEFT), 0.001, "video has no hspace hint");
        Check.near(0, fx.style("i").px(Prop.MARGIN_LEFT), 0.001, "canvas has no hspace hint");
        Check.near(0, fx.style("j").px(Prop.MARGIN_LEFT), 0.001, "a text input has no hspace hint");
        Check.near(12, fx.style("k").px(Prop.MARGIN_LEFT), 0.001, "a dimension value accepts leading space and trailing text");
        Check.near(0, fx.style("k").px(Prop.MARGIN_TOP), 0.001, "a negative vspace is an error");
    }

    /**
     * A <code>border</code> greater than zero draws a solid border of that width around <code>img</code>, <code>object</code> and image
     * buttons; zero and errors do nothing, and other elements have no such hint.
     * <p>
     * 0 보다 큰 <code>border</code> 는 <code>img</code>, <code>object</code>, 이미지 버튼 둘레에 그 두께의 실선 border 를 그린다. 0 과 오류는 효과가 없고
     * 다른 요소에는 이 힌트가 없다.
     */
    public void testReplacedBorder() {
        fx.init("<img id=a border=3 width=10 height=10><img id=b border=0><img id=c border=abc><object id=d border=2></object>"
                + "<input type=image id=e border=4><embed id=f border=5><iframe id=g border=6></iframe><img id=h border=' +1x'><img id=i border=''>"
                + "<input type=IMAGE id=j border=2>", "");
        Check.equal("solid solid solid solid", styles("a"), "img border style");
        Check.equal("3 3 3 3", widths("a"), "img border width");
        Check.equal("none none none none", styles("b"), "border=0");
        Check.equal("none none none none", styles("c"), "border=abc is an error and not greater than zero");
        Check.equal("2 2 2 2", widths("d"), "object border");
        Check.equal("solid solid solid solid", styles("d"), "object border style");
        Check.equal("4 4 4 4", widths("e"), "input type=image border");
        Check.equal("none none none none", styles("f"), "embed has no border hint");
        Check.equal("inset inset inset inset", styles("g"), "iframe keeps the UA inset border");
        Check.equal("1 1 1 1", widths("h"), "border=' +1x'");
        Check.equal("none none none none", styles("i"), "an empty border is an error");
        Check.equal("solid solid solid solid", styles("j"), "the image type keyword is matched without regard to case");
    }

    /**
     * <code>frameborder</code> of an <code>iframe</code> with the value <code>0</code> or <code>no</code> removes the border.
     * <p>
     * 값이 <code>0</code> 이나 <code>no</code> 인 <code>iframe</code> 의 <code>frameborder</code> 는 border 를 없앤다.
     */
    public void testFrameBorder() {
        fx.init("<iframe id=a frameborder=0></iframe><iframe id=b frameborder=no></iframe><iframe id=c frameborder=NO></iframe>"
                + "<iframe id=d frameborder=1></iframe><iframe id=e frameborder=' 0'></iframe><iframe id=f></iframe>", "");
        Check.equal("none none none none", styles("a"), "frameborder=0");
        Check.equal("none none none none", styles("b"), "frameborder=no");
        Check.equal("none none none none", styles("c"), "frameborder=NO");
        Check.equal("inset inset inset inset", styles("d"), "frameborder=1");
        Check.equal("inset inset inset inset", styles("e"), "a value with a space is not a match");
        Check.equal("inset inset inset inset", styles("f"), "default");
        Check.equal("2 2 2 2", widths("f"), "default width");
    }

    /**
     * The <code>width</code> and <code>height</code> attributes map to the dimensions of the embedded elements and of an image button.
     * <p>
     * <code>width</code> 와 <code>height</code> 속성은 삽입 요소와 이미지 버튼의 크기로 대응한다.
     */
    public void testReplacedDimensions() {
        fx.init("<embed id=a width=50 height=30><object id=b width=60%><param></object><iframe id=c width=70 height=40></iframe>"
                + "<applet id=d width=20 height=10></applet><video id=e width=80 height=45></video><input type=image id=f width=33 height=22>"
                + "<img id=g width=15 height=16><input type=text id=h width=99>", "");
        Check.near(50, fx.style("a").px(Prop.WIDTH), 0.001, "embed width");
        Check.near(30, fx.style("a").px(Prop.HEIGHT), 0.001, "embed height");
        Check.equal("60%", fx.style("b").getPropertyValue("width"), "object width percentage");
        Check.near(70, fx.style("c").px(Prop.WIDTH), 0.001, "iframe width");
        Check.near(10, fx.style("d").px(Prop.HEIGHT), 0.001, "applet height");
        Check.near(80, fx.style("e").px(Prop.WIDTH), 0.001, "video width");
        Check.near(33, fx.style("f").px(Prop.WIDTH), 0.001, "input type=image width");
        Check.near(22, fx.style("f").px(Prop.HEIGHT), 0.001, "input type=image height");
        Check.near(15, fx.style("g").px(Prop.WIDTH), 0.001, "img width");
        Check.equal("auto", fx.style("h").getPropertyValue("width"), "a text input has no width hint");
    }

    /**
     * In quirks mode an <code>img</code> with a left or right alignment gets a 3px margin on the text side, and the margin of
     * <code>hspace</code> wins over it.
     * <p>
     * 호환 모드에서 왼쪽/오른쪽 정렬된 <code>img</code> 는 글 쪽에 3px margin 을 받고 <code>hspace</code> 의 margin 이 그보다 우선한다.
     */
    public void testQuirksImageAlignMargins() {
        String markup = "<img id=a align=left width=10 height=10><img id=b align=right width=10 height=10><img id=c align=left hspace=7 width=10 height=10>"
                + "<img id=d width=10 height=10>";
        fx.initQuirks(markup, "");
        Check.near(3, fx.style("a").px(Prop.MARGIN_RIGHT), 0.001, "quirks: left image");
        Check.near(0, fx.style("a").px(Prop.MARGIN_LEFT), 0.001, "quirks: left image has no left margin");
        Check.near(3, fx.style("b").px(Prop.MARGIN_LEFT), 0.001, "quirks: right image");
        Check.near(7, fx.style("c").px(Prop.MARGIN_RIGHT), 0.001, "quirks: hspace wins");
        Check.near(0, fx.style("d").px(Prop.MARGIN_RIGHT), 0.001, "quirks: no align, no margin");
        fx.init(markup, "");
        Check.near(0, fx.style("a").px(Prop.MARGIN_RIGHT), 0.001, "standards: no extra margin");
    }

    /**
     * <code>br clear</code> maps <code>left</code>, <code>right</code>, <code>all</code> and <code>both</code> to <code>clear</code>,
     * ignoring case but not spaces.
     * <p>
     * <code>br clear</code> 는 <code>left</code>, <code>right</code>, <code>all</code>, <code>both</code> 를 <code>clear</code> 로 대응하며 대소문자는 무시하고 공백은 허용하지 않는다.
     */
    public void testBreakClear() {
        fx.init("<br id=a clear=left><br id=b clear=RIGHT><br id=c clear=all><br id=d clear=both><br id=e clear=' all'><br id=f clear=bogus><br id=g>", "");
        Check.equal("left", fx.style("a").keyword(Prop.CLEAR), "left");
        Check.equal("right", fx.style("b").keyword(Prop.CLEAR), "RIGHT");
        Check.equal("both", fx.style("c").keyword(Prop.CLEAR), "all");
        Check.equal("both", fx.style("d").keyword(Prop.CLEAR), "both");
        Check.equal("none", fx.style("e").keyword(Prop.CLEAR), "a leading space is not a match");
        Check.equal("none", fx.style("f").keyword(Prop.CLEAR), "bogus");
        Check.equal("none", fx.style("g").keyword(Prop.CLEAR), "no attribute");
    }

    /**
     * A <code>br</code> with <code>clear</code> moves the next line below the floats: the text after the break starts under a left float.
     * <p>
     * <code>clear</code> 가 있는 <code>br</code> 은 다음 줄을 float 아래로 내린다: 줄바꿈 뒤의 글은 왼쪽 float 아래에서 시작한다.
     */
    public void testBreakClearLayout() {
        fx.init("<div style='float:left;width:50px;height:60px'></div><span id=a>aaa</span><br clear=left><span id=b>bbb</span><br><span id=c>ccc</span>", "");
        RenderedPage page = fx.render();
        LayoutBox a = fx.box(page, "a");
        LayoutBox b = fx.box(page, "b");
        LayoutBox c = fx.box(page, "c");
        Check.near(50, a.getX(), 0.5, "the first line starts beside the float");
        Check.isTrue(b.getY() >= 60 - 0.01, "the line after br clear=left starts below the float: " + b.getY());
        Check.near(0, b.getX(), 0.5, "and begins at the left edge");
        Check.isTrue(c.getY() > b.getY(), "a plain break moves on to the next line");
    }

    /**
     * Paragraphs and headings take <code>left</code>, <code>right</code>, <code>center</code> and <code>justify</code> from
     * <code>align</code>; <code>middle</code> is not one of them (only <code>div</code> accepts it).
     * <p>
     * 문단과 제목은 <code>align</code> 에서 <code>left</code>, <code>right</code>, <code>center</code>, <code>justify</code> 를 받고 <code>middle</code> 은 그 목록에 없다(<code>div</code> 만 받는다).
     */
    public void testParagraphAndHeadingAlignment() {
        fx.init("<p id=a align=left>x</p><p id=b align=RIGHT>x</p><p id=c align=center>x</p><p id=d align=justify>x</p><p id=e align=middle>x</p>"
                + "<h1 id=f align=center>x</h1><h3 id=g align=right>x</h3><h6 id=h align=middle>x</h6><div id=i align=middle>x</div><p id=j align=' center'>x</p>", "");
        Check.equal("left", fx.style("a").keyword(Prop.TEXT_ALIGN), "p left");
        Check.equal("right", fx.style("b").keyword(Prop.TEXT_ALIGN), "p RIGHT");
        Check.equal("center", fx.style("c").keyword(Prop.TEXT_ALIGN), "p center");
        Check.equal("justify", fx.style("d").keyword(Prop.TEXT_ALIGN), "p justify");
        Check.equal("start", fx.style("e").keyword(Prop.TEXT_ALIGN), "p middle has no effect");
        Check.equal("center", fx.style("f").keyword(Prop.TEXT_ALIGN), "h1 center");
        Check.equal("right", fx.style("g").keyword(Prop.TEXT_ALIGN), "h3 right");
        Check.equal("start", fx.style("h").keyword(Prop.TEXT_ALIGN), "h6 middle has no effect");
        Check.equal("center", fx.style("i").keyword(Prop.TEXT_ALIGN), "div middle centers");
        Check.equal("start", fx.style("j").keyword(Prop.TEXT_ALIGN), "a leading space is not a match");
    }

    /**
     * The user agent rules of <code>fieldset</code> and <code>legend</code> follow the section: 2px side margins, a 2px groove border in
     * the face color, paddings of 0.35em, 0.625em, 0.75em and a 2px side padding of the legend.
     * <p>
     * <code>fieldset</code> 과 <code>legend</code> 의 UA 규칙은 절을 따른다: 좌우 margin 2px, 면 색의 2px groove border, 안쪽 여백 0.35em/0.625em/0.75em, legend 의 좌우 여백 2px.
     */
    public void testFieldsetDefaults() {
        fx.init("<fieldset id=f><legend id=l>L</legend>x</fieldset>", "");
        ComputedStyle s = fx.style("f");
        Check.near(2, s.px(Prop.MARGIN_LEFT), 0.001, "margin left");
        Check.near(2, s.px(Prop.MARGIN_RIGHT), 0.001, "margin right");
        Check.near(0, s.px(Prop.MARGIN_TOP), 0.001, "margin top");
        Check.equal("groove groove groove groove", styles("f"), "border style");
        Check.equal("2 2 2 2", widths("f"), "border width");
        Check.equal(0xFFEFEFEF, s.color(Prop.BORDER_TOP_COLOR), "ThreeDFace");
        Check.near(0.35 * 20, s.px(Prop.PADDING_TOP), 0.001, "padding top");
        Check.near(0.625 * 20, s.px(Prop.PADDING_LEFT), 0.001, "padding left");
        Check.near(0.625 * 20, s.px(Prop.PADDING_RIGHT), 0.001, "padding right");
        Check.near(0.75 * 20, s.px(Prop.PADDING_BOTTOM), 0.001, "padding bottom");
        Check.near(2, fx.style("l").px(Prop.PADDING_LEFT), 0.001, "legend padding left");
        Check.near(2, fx.style("l").px(Prop.PADDING_RIGHT), 0.001, "legend padding right");
        Check.near(0, fx.style("l").px(Prop.PADDING_TOP), 0.001, "legend padding top");
    }

    /**
     * The rendered legend sits over the top border edge, shrink-wrapped; <code>align</code> puts it left, center or right, and without
     * a usable value it is on the left (on the right when the direction is right-to-left).
     * <p>
     * 렌더링되는 legend 는 위쪽 border 가장자리 위에 오므로 내용에 맞게 줄어든다. <code>align</code> 은 왼쪽, 가운데, 오른쪽에 두고 쓸 수 있는 값이 없으면 왼쪽이다(방향이 오른쪽에서 왼쪽이면 오른쪽).
     */
    public void testLegendAlignment() {
        String[] aligns = { "", " align=left", " align=center", " align=right", " align=CENTER", " align=bogus", " align=' center'", " align=middle" };
        String[] where = { "left", "left", "center", "right", "center", "left", "left", "left" };
        StringBuilder markup = new StringBuilder();
        // align 값마다 fieldset 을 하나씩 만든다.
        for (int i = 0; i < aligns.length; i++) {
            markup.append("<fieldset id=f").append(i).append(" style='width:176px'><legend id=l").append(i).append(aligns[i]).append(">ab</legend>x</fieldset>");
        }
        markup.append("<fieldset id=fr dir=rtl style='width:176px'><legend id=lr>ab</legend>x</fieldset>");
        fx.init(markup.toString(), "");
        RenderedPage page = fx.render();
        // align 값마다 legend 의 가로 위치를 확인한다.
        for (int i = 0; i < aligns.length; i++) {
            LayoutBox f = fx.box(page, "f" + i);
            LayoutBox l = fx.box(page, "l" + i);
            double left = f.getContentX();
            double room = f.getContentWidth() - l.getWidth();
            double expected = where[i].equals("left") ? left : where[i].equals("center") ? left + room / 2 : left + room;
            Check.near(expected, l.getX(), 0.6, "legend position for" + aligns[i]);
            Check.isTrue(l.getWidth() < f.getContentWidth() / 2, "the legend shrink-wraps: " + l.getWidth());
            // legend 는 위쪽 border 위에 걸친다: 세로 가운데가 border 영역 안에 있다.
            double middle = l.getY() + l.getHeight() / 2;
            Check.isTrue(middle <= f.getY() + f.getBorderTop() + 1 && middle >= f.getY() - l.getHeight(), "the legend sits over the top border: " + middle);
        }
        LayoutBox fr = fx.box(page, "fr");
        LayoutBox lr = fx.box(page, "lr");
        Check.near(fr.getContentX() + fr.getContentWidth() - lr.getWidth(), lr.getX(), 0.6, "right-to-left puts the legend on the right");
    }

    /**
     * A fieldset establishes a block formatting context, so it contains its floats; and an image button pixel test: the legend breaks
     * the top border of the fieldset.
     * <p>
     * fieldset 은 블록 서식 문맥을 만들므로 float 를 포함하며, legend 는 fieldset 의 위쪽 border 를 끊는다.
     */
    public void testFieldsetContainsFloatsAndBreaksBorder() {
        fx.init("<fieldset id=f style='position:absolute;left:10px;top:30px;width:150px;border:2px solid #ff0000;padding:0'>"
                + "<legend id=l style='color:#000'>MMMM</legend><div id=d style='float:left;width:50px;height:50px;background:#00ff00'></div></fieldset>", "");
        RenderedPage page = fx.render();
        LayoutBox f = fx.box(page, "f");
        LayoutBox d = fx.box(page, "d");
        Check.isTrue(f.getHeight() >= 50 + 2 + 2 - 0.01 - 1, "the fieldset contains its float: " + f.getHeight());
        Check.isTrue(d.getY() + d.getHeight() <= f.getY() + f.getHeight() + 0.01, "the float is inside the fieldset box");
        BufferedImage image = LegacyFixture.paint(page);
        LayoutBox l = fx.box(page, "l");
        int topY = (int) Math.round(f.getY() + l.getHeight() / 2 + 0.0);
        int ox = (int) Math.round(l.getX() + l.getWidth() / 2);
        int topBorderY = (int) Math.round(f.getY() + (f.getBorderTop() > 0 ? f.getBorderTop() / 2 : 0));
        int right = (int) Math.round(f.getX() + f.getWidth()) - 20;
        int through = image.getRGB(right, topBorderY) & 0xFFFFFF;
        Check.equal(0xFF0000, through, "the top border is drawn beyond the legend: " + Integer.toHexString(through));
        int under = image.getRGB(ox, topBorderY) & 0xFFFFFF;
        Check.isTrue(under != 0xFF0000, "the top border is cut under the legend: " + Integer.toHexString(under) + " at " + ox + "," + topBorderY + " " + topY);
    }
}
