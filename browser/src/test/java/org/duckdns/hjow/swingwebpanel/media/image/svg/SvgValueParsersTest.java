package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the small parsers of the SVG decoder (plan item P18-04): colours, lengths, number scanning, transform lists and path data.
 * <p>
 * SVG 디코더의 작은 해석기 시험(계획 P18-04): 색, 길이, 수 스캔, 변환 목록, 경로 데이터.
 */
public class SvgValueParsersTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SvgValueParsersTest() {
    }

    /**
     * Named colours, hexadecimal forms and invalid texts.
     * <p>
     * 이름 있는 색, 16진수 형식, 올바르지 않은 텍스트.
     */
    public void testColorsNamedAndHex() {
        Check.equal(0xFFFF0000L, SvgColors.parse("red").intValue() & 0xFFFFFFFFL, "red");
        Check.equal(0xFF663399L, SvgColors.parse("RebeccaPurple").intValue() & 0xFFFFFFFFL, "case insensitive name");
        Check.equal(0xFF778899L, SvgColors.parse("lightslategrey").intValue() & 0xFFFFFFFFL, "grey spelling");
        Check.equal(0L, SvgColors.parse("transparent").intValue() & 0xFFFFFFFFL, "transparent");
        Check.equal(0xFFAA00CCL, SvgColors.parse("  #A0C  ").intValue() & 0xFFFFFFFFL, "short hex with spaces");
        Check.equal(0x88FF0000L, SvgColors.parse("#f008").intValue() & 0xFFFFFFFFL, "hex with alpha (4)");
        Check.equal(0x800000FFL, SvgColors.parse("#0000ff80").intValue() & 0xFFFFFFFFL, "hex with alpha (8)");
        Check.isNull(SvgColors.parse("#12345"), "five hex digits");
        Check.isNull(SvgColors.parse("#ggg"), "not hex");
        Check.isNull(SvgColors.parse("nonsense"), "unknown name");
        Check.isNull(SvgColors.parse(""), "empty");
        Check.isNull(SvgColors.parse(null), "null");
        Check.isNull(SvgColors.parse("currentColor"), "currentColor is the business of the caller");
        Check.equal(0xFF000000L, SvgColors.parse("buttontext").intValue() & 0xFFFFFFFFL, "system colour");
    }

    /**
     * The functional notations rgb, rgba, hsl, hsla and hwb.
     * <p>
     * 함수 표기 rgb, rgba, hsl, hsla, hwb.
     */
    public void testColorsFunctions() {
        Check.equal(0xFF0A141EL, SvgColors.parse("rgb(10, 20, 30)").intValue() & 0xFFFFFFFFL, "rgb commas");
        Check.equal(0xFF0A141EL, SvgColors.parse("rgb(10 20 30)").intValue() & 0xFFFFFFFFL, "rgb spaces");
        Check.equal(0xFFFF8000L, SvgColors.parse("rgb(100%, 50%, 0%)").intValue() & 0xFFFFFFFFL, "rgb percent");
        Check.equal(0x400000FFL, SvgColors.parse("rgba(0, 0, 255, .25)").intValue() & 0xFFFFFFFFL, "rgba");
        Check.equal(0x40FF0080L, SvgColors.parse("rgb(255 0 128 / 25%)").intValue() & 0xFFFFFFFFL, "rgb slash alpha");
        Check.equal(0xFF00FF00L, SvgColors.parse("rgb(0, 300, 0)").intValue() & 0xFFFFFFFFL, "channels are clamped");
        Check.equal(0xFF00CC00L, SvgColors.parse("hsl(120, 100%, 40%)").intValue() & 0xFFFFFFFFL, "hsl green");
        Check.equal(0xFF0000FFL, SvgColors.parse("HSL(240deg 100% 50%)").intValue() & 0xFFFFFFFFL, "hsl with angle unit, upper case");
        Check.equal(0xFF00FFFFL, SvgColors.parse("hsl(0.5turn, 100%, 50%)").intValue() & 0xFFFFFFFFL, "turn unit");
        Check.equal(0xFF33CC33L, SvgColors.parse("hwb(120 20% 20%)").intValue() & 0xFFFFFFFFL, "hwb");
        Check.isNull(SvgColors.parse("rgb(10,20)"), "two arguments");
        Check.isNull(SvgColors.parse("rgb(a, b, c)"), "not numbers");
        Check.isNull(SvgColors.parse("lab(10 20 30)"), "unsupported function");
    }

    /**
     * Lengths with all units and percentages.
     * <p>
     * 모든 단위와 백분율이 있는 길이.
     */
    public void testLengths() {
        Check.near(10, SvgLength.parse("10").toPx(16, 0, 0, 0), 1e-9, "plain number");
        Check.near(10, SvgLength.parse("10px").toPx(16, 0, 0, 0), 1e-9, "px");
        Check.near(96, SvgLength.parse("1in").toPx(16, 0, 0, 0), 1e-9, "in");
        Check.near(96 / 2.54, SvgLength.parse("1cm").toPx(16, 0, 0, 0), 1e-9, "cm");
        Check.near(96 / 25.4, SvgLength.parse("1mm").toPx(16, 0, 0, 0), 1e-9, "mm");
        Check.near(4.0 / 3, SvgLength.parse("1pt").toPx(16, 0, 0, 0), 1e-9, "pt");
        Check.near(16, SvgLength.parse("1pc").toPx(16, 0, 0, 0), 1e-9, "pc");
        Check.near(30, SvgLength.parse("3em").toPx(10, 0, 0, 0), 1e-9, "em");
        Check.near(15, SvgLength.parse("3ex").toPx(10, 0, 0, 0), 1e-9, "ex");
        Check.near(16, SvgLength.parse("1rem").toPx(99, 0, 0, 0), 1e-9, "rem");
        Check.near(25, SvgLength.parse("25%").toPx(10, 100, 0, 0), 1e-9, "percent");
        Check.near(5, SvgLength.parse("5vw").toPx(10, 100, 100, 200), 1e-9, "vw");
        Check.near(10, SvgLength.parse("5vh").toPx(10, 100, 100, 200), 1e-9, "vh");
        Check.near(-2.5e2, SvgLength.parse("-2.5e2").toPx(10, 0, 0, 0), 1e-9, "exponent");
        Check.near(0.5, SvgLength.parse(".5").toPx(10, 0, 0, 0), 1e-9, "leading dot");
        Check.isNull(SvgLength.parse("10 px"), "space before the unit");
        Check.isNull(SvgLength.parse("10qq"), "unknown unit");
        Check.isNull(SvgLength.parse("abc"), "not a number");
        Check.isNull(SvgLength.parse("10px 20px"), "two lengths");
        Check.equal(3, SvgLength.parseList("1px, 2 3em").length, "list");
        Check.isNull(SvgLength.parseList("1 2 x"), "list with an error");
    }

    /**
     * Number scanning: the SVG grammar allows numbers without separators.
     * <p>
     * 수 스캔: SVG 문법은 구분자 없는 수를 허용한다.
     */
    public void testNumberScanner() {
        double[] a = SvgScanner.numberList("10-20 1.5.5 1e2 .5e-1 +3");
        Check.notNull(a, "list parsed");
        Check.equal(7, a.length, "count of numbers");
        Check.near(10, a[0], 0, "10");
        Check.near(-20, a[1], 0, "-20");
        Check.near(1.5, a[2], 0, "1.5");
        Check.near(0.5, a[3], 0, ".5");
        Check.near(100, a[4], 0, "1e2");
        Check.near(0.05, a[5], 1e-12, ".5e-1");
        Check.near(3, a[6], 0, "+3");
        Check.isNull(SvgScanner.numberList("1 2 x"), "garbage");
        Check.isNull(SvgScanner.numberList("1e"), "a lone exponent marker is not a number");
        Check.equal(0, SvgScanner.numberList("  , ,").length, "only separators");
        SvgScanner sc = new SvgScanner("5em");
        Check.isTrue(sc.number(), "number before unit");
        Check.equal("em", sc.letters(), "unit after the number: e is not an exponent without digits");
    }

    /**
     * Transform lists in the attribute syntax.
     * <p>
     * 속성 문법의 변환 목록.
     */
    public void testTransformAttribute() {
        AffineTransform t = SvgTransforms.parse("translate(10,20) scale(2)", false, 0, 0);
        Check.near(10, t.getTranslateX(), 1e-12, "translate x");
        Check.near(20, t.getTranslateY(), 1e-12, "translate y");
        Check.near(2, t.getScaleX(), 1e-12, "scale");
        double[] p = { 1, 1 };
        t.transform(p, 0, p, 0, 1);
        Check.near(12, p[0], 1e-12, "applied left to right: scale first then translate (x)");
        Check.near(22, p[1], 1e-12, "(y)");
        AffineTransform r = SvgTransforms.parse("rotate(90 10 10)", false, 0, 0);
        double[] q = { 20, 10 };
        r.transform(q, 0, q, 0, 1);
        Check.near(10, q[0], 1e-9, "rotate around a centre x");
        Check.near(20, q[1], 1e-9, "rotate around a centre y");
        AffineTransform m = SvgTransforms.parse("matrix(1 2 3 4 5 6)", false, 0, 0);
        Check.near(2, m.getShearY(), 0, "matrix b");
        Check.near(3, m.getShearX(), 0, "matrix c");
        AffineTransform s = SvgTransforms.parse("skewX(45) skewY(45)", false, 0, 0);
        Check.near(1, s.getShearX(), 1e-9, "skewX 45");
        Check.near(1, s.getShearY(), 1e-9, "skewY 45");
        Check.isTrue(SvgTransforms.parse("  translate( 5 , 5 )  ,  rotate( 10 ) ", false, 0, 0) != null, "spaces and commas between functions");
        Check.isTrue(SvgTransforms.parse("", false, 0, 0).isIdentity(), "empty is the identity");
        Check.isNull(SvgTransforms.parse("translate(10,20) bogus(3)", false, 0, 0), "an unknown function invalidates the list");
        Check.isNull(SvgTransforms.parse("translate(1,2,3)", false, 0, 0), "too many arguments");
        Check.isNull(SvgTransforms.parse("scale(", false, 0, 0), "unterminated");
        Check.isNull(SvgTransforms.parse("translate(10px)", false, 0, 0), "units are not allowed in the attribute syntax");
    }

    /**
     * Transform lists in the CSS syntax.
     * <p>
     * CSS 문법의 변환 목록.
     */
    public void testTransformCss() {
        AffineTransform t = SvgTransforms.parse("translate(20px, 10px) rotate(0.25turn)", true, 100, 100);
        double[] p = { 1, 0 };
        t.transform(p, 0, p, 0, 1);
        Check.near(20, p[0], 1e-9, "rotate 90 degrees then translate (x)");
        Check.near(11, p[1], 1e-9, "(y)");
        AffineTransform pc = SvgTransforms.parse("translate(50%, 10%)", true, 200, 100);
        Check.near(100, pc.getTranslateX(), 1e-9, "percent of the reference width");
        Check.near(10, pc.getTranslateY(), 1e-9, "percent of the reference height");
        Check.near(0.5, SvgTransforms.parse("scale(.5)", true, 0, 0).getScaleY(), 1e-12, "scale with one argument");
        Check.near(Math.PI / 4, Math.toRadians(SvgTransforms.angleToDegrees(50, "grad")), 1e-9, "grad");
        Check.isTrue(Double.isNaN(SvgTransforms.angleToDegrees(1, "px")), "px is not an angle");
        Check.isNull(SvgTransforms.parse("translate3d(1px,2px,3px)", true, 0, 0), "3D functions are not supported");
        Check.isNull(SvgTransforms.parse("rotate(1 2 3)", true, 0, 0), "three-argument rotate is the attribute syntax only");
    }

    /**
     * Path data: all commands and the relative forms.
     * <p>
     * 경로 데이터: 모든 명령과 상대 형식.
     */
    public void testPathCommands() {
        Rectangle2D b = SvgBounds.shapeBounds(SvgPath.parse("M10 10 L50 10 H70 V40 h-10 v10 z").shape);
        Check.near(10, b.getMinX(), 1e-9, "min x");
        Check.near(70, b.getMaxX(), 1e-9, "max x");
        Check.near(10, b.getMinY(), 1e-9, "min y");
        Check.near(50, b.getMaxY(), 1e-9, "max y");
        Rectangle2D c = SvgBounds.shapeBounds(SvgPath.parse("M0 0 C 0 100, 100 100, 100 0").shape);
        Check.near(75, c.getMaxY(), 1e-6, "tight bounds of a cubic curve (control points do not count)");
        Rectangle2D q = SvgBounds.shapeBounds(SvgPath.parse("M0 0 Q 50 100 100 0").shape);
        Check.near(50, q.getMaxY(), 1e-6, "tight bounds of a quadratic curve");
        Rectangle2D s = SvgBounds.shapeBounds(SvgPath.parse("M0 0 C 0 10 10 10 10 0 S 20 -10 20 0").shape);
        Check.near(20, s.getMaxX(), 1e-9, "smooth cubic ends at its point");
        Rectangle2D t = SvgBounds.shapeBounds(SvgPath.parse("M0 0 Q 10 20 20 0 T 40 0").shape);
        Check.near(40, t.getMaxX(), 1e-9, "smooth quadratic ends at its point");
        Check.near(-10, t.getMinY(), 1e-6, "the reflected control point mirrors the curve");
        Rectangle2D r = SvgBounds.shapeBounds(SvgPath.parse("m10 10 l10 0 l0 10 z m5 5 l1 1").shape);
        Check.near(10, r.getMinX(), 1e-9, "relative commands");
        Check.near(20, r.getMaxX(), 1e-9, "the first subpath reaches x=20");
        Check.near(16, SvgBounds.shapeBounds(SvgPath.parse("m10 10 l10 0 l0 10 z m5 5 l1 1").shape).getMaxY() - 4, 1e-9, "after z the current point is the start of the subpath, so m5 5 gives (15,15) and l1 1 gives (16,16)");
    }

    /**
     * The current point after a close path command and implicit commands.
     * <p>
     * 경로 닫기 명령 뒤의 현재점과 생략된 명령.
     */
    public void testPathImplicitAndClose() {
        SvgPath p = SvgPath.parse("M10 10 20 20 30 10");
        Check.equal(3, p.segs.size(), "an implicit lineto follows moveto");
        Check.equal(SvgPath.DRAW, p.segs.get(1).kind, "second segment is a line");
        SvgPath rel = SvgPath.parse("m10 10 10 10");
        Check.near(20, rel.segs.get(1).x, 0, "implicit relative lineto x");
        Check.near(20, rel.segs.get(1).y, 0, "implicit relative lineto y");
        SvgPath z = SvgPath.parse("M10 10 L20 10 L20 20 z l 5 5");
        Check.equal(SvgPath.CLOSE, z.segs.get(3).kind, "close segment");
        Check.equal(SvgPath.MOVE, z.segs.get(4).kind, "a drawing command after z starts a new subpath");
        Check.near(10, z.segs.get(4).x, 0, "at the start point of the closed subpath");
        Check.near(15, z.segs.get(5).x, 0, "relative to that point");
        SvgPath compact = SvgPath.parse("M10-5L50 40.5.5 90");
        Check.equal(3, compact.segs.size(), "numbers without separators");
        Check.near(0.5, compact.segs.get(2).x, 1e-12, "40.5.5 is two numbers");
    }

    /**
     * Errors in path data: the part before the error is kept.
     * <p>
     * 경로 데이터의 오류: 오류 앞부분은 유지한다.
     */
    public void testPathErrors() {
        SvgPath a = SvgPath.parse("M10 10 L90 10 L90 90 L 50 50 Q");
        Check.equal(4, a.segs.size(), "the segments before the error are kept");
        Check.equal(0, SvgPath.parse("L 10 50 L 90 60").segs.size(), "path data must start with a moveto");
        Check.equal(2, SvgPath.parse("M10 95 L90 95 X 5 5").segs.size(), "an unknown command ends the path");
        Check.equal(0, SvgPath.parse("M 10").segs.size(), "missing coordinate gives no segment");
        Check.equal(0, SvgPath.parse(null).segs.size(), "null");
        Check.equal(0, SvgPath.parse("").segs.size(), "empty");
        Check.equal(2, SvgPath.parse("M0 0 L 1 1 2").segs.size(), "an incomplete argument set at the end is dropped");
    }

    /**
     * Elliptical arcs: flags without separators, radius scaling, degenerate cases, and the tangents for markers.
     * <p>
     * 타원 호: 구분자 없는 플래그, 반지름 보정, 퇴화 경우, 마커를 위한 접선.
     */
    public void testPathArcs() {
        Rectangle2D semi = SvgBounds.shapeBounds(SvgPath.parse("M0 0 A 50 50 0 0 1 100 0").shape);
        Check.near(0, semi.getMinX(), 1e-6, "semicircle min x");
        Check.near(100, semi.getMaxX(), 1e-6, "semicircle max x");
        Check.near(-50, semi.getMinY(), 1e-6, "sweep=1 goes through negative y (clockwise on screen)");
        Rectangle2D down = SvgBounds.shapeBounds(SvgPath.parse("M0 0 A 50 50 0 0 0 100 0").shape);
        Check.near(50, down.getMaxY(), 1e-6, "sweep=0 goes the other way");
        Rectangle2D big = SvgBounds.shapeBounds(SvgPath.parse("M0 0 A 60 60 0 1 1 100 0").shape);
        Check.near(60 + Math.sqrt(1100), big.getHeight(), 0.01, "large arc flag selects the larger arc: radius plus the distance of the centre from the chord");
        Rectangle2D packed = SvgBounds.shapeBounds(SvgPath.parse("M20 50 a30 20 0 1090 0").shape);
        Check.near(110, packed.getMaxX(), 1e-6, "flags written without separators: a30 20 0 1 0 90 0");
        Rectangle2D scaled = SvgBounds.shapeBounds(SvgPath.parse("M0 0 A 1 1 0 0 1 100 0").shape);
        Check.near(100, scaled.getMaxX(), 1e-6, "too small radii are scaled up so that the arc reaches its end point");
        Check.near(-50, scaled.getMinY(), 1e-6, "scaled to a half circle");
        SvgPath line = SvgPath.parse("M0 0 A 0 10 0 0 1 50 0");
        Check.equal(2, line.segs.size(), "a zero radius gives a straight line");
        Check.near(50, line.segs.get(1).x, 1e-12, "line end");
        Check.equal(1, SvgPath.parse("M5 5 A 10 10 0 0 1 5 5").segs.size(), "an arc to the current point is omitted");
        SvgPath marker = SvgPath.parse("M0 0 A 50 50 0 0 1 100 0");
        Check.equal(2, marker.segs.size(), "an arc is one vertex for markers although it is drawn as several curves");
        Check.near(Math.PI / 2, Math.atan2(marker.segs.get(1).endDy, marker.segs.get(1).endDx), 0.01, "arrives downwards at the end");
        Check.isTrue(Math.abs(Math.atan2(marker.segs.get(1).startDy, marker.segs.get(1).startDx) + Math.PI / 2) < 0.01, "leaves upwards at the start");
    }
}
