package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the decoration of boxes (plan item P10-04, CSS Backgrounds and Borders Level 3, CSS Images Level 3): <code>border-radius</code>
 * (circular and elliptical corners, scaling of overlapping radii, the border ring, the clip of the background and of
 * <code>overflow:hidden</code>), <code>box-shadow</code> (offset, blur, spread, inset, several shadows, the shadow inside an opacity
 * group), linear, radial, conic and repeating gradients (angles, side and corner keywords, color stops, hard stops, transparent stops
 * in premultiplied space, shapes and sizes), multiple background layers with <code>background-size</code>,
 * <code>background-position</code> (with edge offsets), <code>background-repeat</code> (<code>repeat</code>, <code>space</code>,
 * <code>round</code>), <code>background-origin</code> and <code>background-clip</code>. The tests paint a page into an image and check the
 * pixels at points whose expected color follows from the specification; the points keep a distance from anti-aliased edges.
 * <p>
 * 상자 장식 시험(계획 P10-04, CSS Backgrounds and Borders Level 3, CSS Images Level 3): <code>border-radius</code>(원형과 타원형 모서리, 겹치는 반지름의 축소,
 * border 고리, 배경과 <code>overflow:hidden</code> 의 클립), <code>box-shadow</code>(오프셋, 흐림, 퍼짐, inset, 여러 그림자, 불투명도 그룹 안의 그림자),
 * 선형, 방사형, 원뿔형, 반복 그라디언트(각도, 변과 모서리 키워드, 색 정지점, 딱딱한 정지점, 사전 곱셈 공간의 투명 정지점, 모양과 크기), 여러 배경 레이어와
 * <code>background-size</code>, <code>background-position</code>(변 오프셋), <code>background-repeat</code>(<code>repeat</code>, <code>space</code>, <code>round</code>),
 * <code>background-origin</code>, <code>background-clip</code>. 페이지를 이미지에 그려 규격으로 기대 색이 정해지는 점의 화소를 확인하며 점은 안티앨리어싱된 가장자리에서
 * 떨어져 있다.
 */
public class BoxDecorationPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BoxDecorationPaintTest() {
    }

    /**
     * Paints a body into a 400 x 300 white image; a box with the id <code>a</code> of 100 x 60 sits at (40, 40) because of its margin.
     * <p>
     * body 를 400 x 300 흰색 이미지에 그린다. id 가 <code>a</code> 인 100 x 60 상자는 margin 때문에 (40, 40) 에 놓인다.
     *
     * @param boxStyle style of the box <code>a</code> / 상자 <code>a</code> 의 스타일
     * @return the image / 이미지
     */
    private static BufferedImage box(String boxStyle) {
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:40px;width:100px;height:60px;" + boxStyle + "'></div>", 400, 300, null);
        return RenderTestSupport.paint(p, 400, 300);
    }

    /**
     * Returns the RGB value of a pixel.
     * <p>
     * 화소의 RGB 값을 반환한다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return 0xRRGGBB / 0xRRGGBB
     */
    private static int px(BufferedImage img, int x, int y) {
        return RenderTestSupport.rgb(img, x, y);
    }

    /**
     * Checks that a pixel has a color within a tolerance per channel.
     * <p>
     * 화소가 채널마다 허용 오차 안의 색인지 확인한다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @param rgb the expected color / 기대 색
     * @param tol the tolerance per channel / 채널마다의 허용 오차
     * @param msg the message / 메시지
     */
    private static void color(BufferedImage img, int x, int y, int rgb, int tol, String msg) {
        int c = px(img, x, y);
        boolean ok = Math.abs(((c >> 16) & 255) - ((rgb >> 16) & 255)) <= tol && Math.abs(((c >> 8) & 255) - ((rgb >> 8) & 255)) <= tol
                && Math.abs((c & 255) - (rgb & 255)) <= tol;
        Check.isTrue(ok, msg + ": pixel (" + x + "," + y + ") is " + Integer.toHexString(c) + ", expected " + Integer.toHexString(rgb) + " +-" + tol);
    }

    /**
     * Returns the grey level (average of the channels) of a pixel.
     * <p>
     * 화소의 회색 수준(채널 평균)을 반환한다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return 0..255 / 0~255
     */
    private static int grey(BufferedImage img, int x, int y) {
        int c = px(img, x, y);
        return (((c >> 16) & 255) + ((c >> 8) & 255) + (c & 255)) / 3;
    }

    /**
     * The corners of a box with <code>border-radius</code> are cut away and the rest keeps its background.
     * <p>
     * <code>border-radius</code> 가 있는 상자의 모서리는 잘려 나가고 나머지는 배경을 유지한다.
     */
    public void testBackgroundIsClippedToTheRoundedCorners() {
        BufferedImage img = box("background:#f00;border-radius:20px");
        color(img, 41, 41, 0xFFFFFF, 0, "the very corner is outside the circle");
        color(img, 49, 49, 0xFF0000, 12, "a point inside the arc");
        color(img, 90, 70, 0xFF0000, 0, "the center");
        color(img, 139, 41, 0xFFFFFF, 0, "the top right corner");
        color(img, 41, 99, 0xFFFFFF, 0, "the bottom left corner");
        color(img, 139, 99, 0xFFFFFF, 0, "the bottom right corner");
        color(img, 90, 41, 0xFF0000, 0, "the straight top edge");
    }

    /**
     * An elliptical radius (<code>40px / 20px</code>): the horizontal radius is 40, the vertical radius 20.
     * <p>
     * 타원 반지름(<code>40px / 20px</code>): 가로 반지름은 40, 세로 반지름은 20.
     */
    public void testEllipticalRadius() {
        BufferedImage img = box("background:#00f;border-radius:40px / 20px");
        // 모서리 타원의 중심은 (80, 60), 반지름은 (40, 20) 이다. 점 (52, 48) 은 타원 안: (28/40)^2 + (12/20)^2 = 0.85.
        color(img, 52, 48, 0x0000FF, 0, "inside the ellipse");
        // 점 (44, 44) 는 타원 밖: (36/40)^2 + (16/20)^2 = 1.45.
        color(img, 44, 44, 0xFFFFFF, 0, "outside the ellipse");
        // 같은 가로 거리에서 원이었다면(반지름 40) 안쪽일 점 (50, 70) 은 타원에서도 가운데 높이라 채워진다.
        color(img, 41, 70, 0x0000FF, 0, "at the middle height the box reaches the left edge");
    }

    /**
     * Radii that overlap are scaled down together so that they fit the box (section 5.5): 100px on a 100 x 60 box becomes 30px.
     * <p>
     * 겹치는 반지름은 상자에 맞게 함께 줄어든다(5.5 절): 100 x 60 상자의 100px 는 30px 가 된다.
     */
    public void testOverlappingRadiiAreScaled() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:40px;width:100px;height:60px;background:#0a0;border-radius:100px'></div>", 400, 300, null);
        LayoutBox b = RenderTestSupport.box(p, "a");
        RoundedBox r = RoundedBox.ofBorderBox(b.getStyle(), b.getX(), b.getY(), b.getWidth(), b.getHeight());
        // 변마다 반지름의 합이 변의 길이를 넘으므로 f = min(100/200, 60/200) = 0.3 배가 된다.
        Check.near(30, r.r[0], 1e-9, "top-left horizontal radius");
        Check.near(30, r.r[1], 1e-9, "top-left vertical radius");
        Check.near(30, r.r[4], 1e-9, "bottom-right horizontal radius");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        color(img, 43, 43, 0xFFFFFF, 0, "the corner is cut");
        color(img, 90, 70, 0x00AA00, 0, "the center is filled");
    }

    /**
     * The border of a rounded box is a ring along the rounded edges: the sides keep their colors, the corner arcs belong to the sides at
     * the diagonal, the inside keeps the background.
     * <p>
     * 둥근 상자의 border 는 둥근 가장자리를 따라가는 고리이다: 변은 자기 색을 유지하고 모서리 호는 대각선에서 갈라진 변에 속하며 안쪽은 배경을 유지한다.
     */
    public void testBorderRing() {
        BufferedImage img = box("background:#ff0;border:6px solid #00f;border-radius:20px;width:88px;height:48px");
        color(img, 90, 42, 0x0000FF, 0, "top border");
        color(img, 41, 70, 0x0000FF, 0, "left border");
        color(img, 90, 70, 0xFFFF00, 0, "inside");
        color(img, 41, 41, 0xFFFFFF, 0, "outside the rounded corner");
        // 모서리 호의 고리: 바깥 원 반지름 20 중심 (60,60), 안쪽 반지름 14. (60-16*0.707, 60-16*0.707) = (48.7, 48.7)
        color(img, 49, 49, 0x0000FF, 20, "the corner arc is part of the ring");
        // 고리 안쪽(반지름 14 안쪽)은 배경이다. 중심에서 (8, 8) 떨어진 점.
        color(img, 52, 52, 0xFFFF00, 12, "inside the inner edge of the arc");
    }

    /**
     * Different border colors on the sides of a rounded box: the top is red, the bottom is blue (each side up to the diagonal).
     * <p>
     * 둥근 상자의 변마다 다른 border 색: 위는 빨강, 아래는 파랑(변마다 대각선까지).
     */
    public void testBorderSideColorsOnRoundedBox() {
        BufferedImage img = box("border:8px solid;border-color:#f00 #0f0 #00f #ff0;border-radius:16px;width:84px;height:44px");
        color(img, 90, 43, 0xFF0000, 0, "top");
        color(img, 133, 70, 0x00FF00, 0, "right");
        color(img, 90, 96, 0x0000FF, 0, "bottom");
        color(img, 43, 70, 0xFFFF00, 0, "left");
    }

    /**
     * Dashed and dotted rounded borders paint only part of the ring, double paints two bands.
     * <p>
     * 둥근 dashed, dotted border 는 고리의 일부만 칠하고 double 은 두 띠를 칠한다.
     */
    public void testBorderStylesOnRoundedBox() {
        BufferedImage dashed = box("border:6px dashed #000;border-radius:12px;width:88px;height:48px");
        int ink = 0;
        // 위쪽 변을 따라 x 마다 칠해진 점을 센다(대시와 간격이 번갈아 나온다).
        for (int x = 60; x < 120; x++) {
            if (grey(dashed, x, 43) < 128) {
                ink++;
            }
        }
        Check.isTrue(ink > 15 && ink < 50, "dashes alternate with gaps: " + ink + " of 60 pixels are painted");
        BufferedImage dbl = box("border:9px double #000;border-radius:12px;width:82px;height:42px");
        Check.isTrue(grey(dbl, 90, 41) < 128, "the outer band of double is painted");
        Check.isTrue(grey(dbl, 90, 44) > 200, "the gap between the bands is empty");
        Check.isTrue(grey(dbl, 90, 47) < 128, "the inner band is painted");
    }

    /**
     * <code>overflow:hidden</code> clips the content along the rounded padding edge.
     * <p>
     * <code>overflow:hidden</code> 은 내용을 둥근 padding 가장자리를 따라 자른다.
     */
    public void testOverflowClipsAlongTheRoundedEdge() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:40px;width:100px;height:60px;overflow:hidden;border-radius:25px'>"
                + "<div style='width:300px;height:300px;background:#f0f'></div></div>", 400, 300, null);
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        color(img, 42, 42, 0xFFFFFF, 0, "the content is clipped at the corner");
        color(img, 90, 70, 0xFF00FF, 0, "and visible inside");
        color(img, 160, 70, 0xFFFFFF, 0, "and clipped at the right edge");
        color(img, 90, 110, 0xFFFFFF, 0, "and clipped at the bottom edge");
    }

    /**
     * An outer shadow without blur is the border box moved by the offset, clipped to the outside of the border box.
     * <p>
     * 흐림 없는 바깥 그림자는 오프셋만큼 옮긴 border box 이며 border box 바깥으로 잘린다.
     */
    public void testHardOuterShadow() {
        BufferedImage img = box("background:#fff;box-shadow:10px 10px 0 #000");
        color(img, 145, 105, 0x000000, 0, "the shadow right below the box");
        color(img, 90, 70, 0xFFFFFF, 0, "the box itself covers the shadow");
        color(img, 45, 105, 0xFFFFFF, 0, "left of the shadow");
        color(img, 145, 45, 0xFFFFFF, 0, "above the shadow");
        color(img, 125, 105, 0x000000, 0, "below the box under its right part");
    }

    /**
     * The spread grows the shadow on every side; with a radius the corners stay rounded.
     * <p>
     * 퍼짐은 그림자를 모든 변으로 키운다. 반지름이 있으면 모서리는 둥글게 남는다.
     */
    public void testSpreadAndSeveralShadows() {
        BufferedImage img = box("background:#fff;box-shadow:0 0 0 5px #f00, 0 0 0 10px #00f");
        color(img, 37, 70, 0xFF0000, 0, "the first shadow (5px) is on top");
        color(img, 32, 70, 0x0000FF, 0, "the second shadow (10px) is below it");
        color(img, 28, 70, 0xFFFFFF, 0, "nothing beyond the larger shadow");
        color(img, 90, 37, 0xFF0000, 0, "above the box");
        BufferedImage neg = box("background:#fff;box-shadow:10px 10px 0 -5px #000");
        color(img, 41, 41, 0xFFFFFF, 0, "control: the box has no shadow at its top left corner");
        color(neg, 143, 80, 0x000000, 0, "the shadow of a negative spread: its right edge is at 145 (10 - 5)");
        color(neg, 148, 80, 0xFFFFFF, 0, "and is not as wide as a shadow without spread");
    }

    /**
     * A blurred shadow fades with the distance from the edge: about half the intensity at the edge of the shadow shape, darker closer
     * to the shape, white far away.
     * <p>
     * 흐린 그림자는 가장자리에서 멀어질수록 옅어진다: 그림자 도형의 가장자리에서 세기는 절반 정도이고 도형에 가까울수록 진하며 멀리서는 흰색이다.
     */
    public void testBlurredShadowFades() {
        BufferedImage img = box("background:#fff;box-shadow:0 0 20px #000");
        int edge = grey(img, 38, 70);
        Check.isTrue(edge > 100 && edge < 160, "about half the intensity at the edge: " + edge);
        int a = grey(img, 33, 70);
        int b = grey(img, 27, 70);
        int c = grey(img, 22, 70);
        Check.isTrue(edge < a && a < b && b < c, "lighter with the distance: " + edge + " " + a + " " + b + " " + c);
        Check.isTrue(grey(img, 10, 70) > 250, "white far away: " + grey(img, 10, 70));
        Check.isTrue(grey(img, 90, 70) > 250, "the box covers the shadow");
        // 그림자는 모서리 쪽이 변 가운데보다 옅다(모서리는 이웃이 적다).
        Check.isTrue(grey(img, 38, 38) > edge, "weaker at the corner than at the middle of a side");
    }

    /**
     * An inset shadow is painted inside the padding box above the background: dark near the edge, nothing at the center.
     * <p>
     * 안쪽 그림자는 padding box 안쪽 배경 위에 그려진다: 가장자리 근처는 진하고 가운데는 없다.
     */
    public void testInsetShadow() {
        BufferedImage img = box("background:#fff;box-shadow:inset 0 0 12px #000");
        Check.isTrue(grey(img, 41, 70) < 165 && grey(img, 41, 70) < grey(img, 47, 70) && grey(img, 47, 70) < grey(img, 55, 70),
                "dark near the edge and lighter inwards: " + grey(img, 41, 70) + " " + grey(img, 47, 70) + " " + grey(img, 55, 70));
        Check.isTrue(grey(img, 90, 70) > 250, "white at the center: " + grey(img, 90, 70));
        Check.isTrue(grey(img, 150, 70) > 250, "nothing outside the box");
        BufferedImage hard = box("background:#fff;box-shadow:inset 10px 10px 0 #036");
        color(hard, 45, 70, 0x003366, 0, "the offset inset shadow covers the left part");
        color(hard, 90, 45, 0x003366, 0, "and the top part");
        color(hard, 90, 70, 0xFFFFFF, 0, "and leaves the rest");
        color(hard, 135, 70, 0xFFFFFF, 0, "not the right edge");
    }

    /**
     * The shadow of a box inside an opacity group is part of the group: it is blended with the group's opacity too, and the group is
     * large enough to hold it.
     * <p>
     * 불투명도 그룹 안 상자의 그림자는 그룹의 일부이다: 그룹의 불투명도로 섞이고 그룹은 그림자를 담을 만큼 크다.
     */
    public void testShadowInsideOpacityGroup() {
        BufferedImage img = box("background:#fff;opacity:0.5;box-shadow:10px 10px 0 #000");
        int g = grey(img, 145, 105);
        Check.isTrue(g > 115 && g < 140, "half transparent black over white: " + g);
    }

    /**
     * A linear gradient to the right goes from the first to the last color along the width.
     * <p>
     * 오른쪽으로 가는 선형 그라디언트는 너비를 따라 첫 색에서 마지막 색으로 간다.
     */
    public void testLinearGradientToRight() {
        BufferedImage img = box("background:linear-gradient(to right, #f00, #00f)");
        color(img, 41, 70, 0xFF0000, 8, "left end red");
        color(img, 90, 70, 0x7F007F, 12, "the middle is purple");
        color(img, 138, 70, 0x0000FF, 8, "right end blue");
        color(img, 90, 42, 0x7F007F, 12, "the color does not change along the height");
    }

    /**
     * The default direction is to the bottom, angles are clockwise from the top, <code>to top right</code> is a magic corner.
     * <p>
     * 기본 방향은 아래쪽이고 각도는 위쪽에서 시계 방향이며 <code>to top right</code> 는 마법 모서리이다.
     */
    public void testGradientDirections() {
        BufferedImage down = box("background:linear-gradient(#000, #fff)");
        Check.isTrue(grey(down, 90, 42) < 40 && grey(down, 90, 97) > 215, "top black, bottom white");
        BufferedImage deg = box("background:linear-gradient(90deg, #000, #fff)");
        Check.isTrue(grey(deg, 42, 70) < 40 && grey(deg, 138, 70) > 215, "90deg goes to the right");
        BufferedImage up = box("background:linear-gradient(0deg, #000, #fff)");
        Check.isTrue(grey(up, 90, 97) < 40 && grey(up, 90, 42) > 215, "0deg goes up");
        BufferedImage corner = box("background:linear-gradient(to top right, #000, #fff)");
        Check.isTrue(grey(corner, 42, 97) < 25 && grey(corner, 138, 42) > 230, "the corners are the end points of the magic corner");
        // 마법 모서리에서 다른 두 모서리(왼쪽 위, 오른쪽 아래)는 정확히 50% 이다.
        int tl = grey(corner, 42, 42);
        int br = grey(corner, 138, 97);
        Check.isTrue(Math.abs(tl - 127) < 12 && Math.abs(br - 127) < 12, "the other two corners are in the middle: " + tl + " " + br);
    }

    /**
     * Color stops with positions, a hard stop and a three-color gradient.
     * <p>
     * 위치가 있는 색 정지점, 딱딱한 정지점, 세 색 그라디언트.
     */
    public void testColorStops() {
        BufferedImage hard = box("background:linear-gradient(to right, #f00 50%, #00f 50%)");
        color(hard, 60, 70, 0xFF0000, 0, "before the hard stop");
        color(hard, 120, 70, 0x0000FF, 0, "after the hard stop");
        BufferedImage three = box("background:linear-gradient(to right, #f00, #0f0 30%, #00f)");
        color(three, 41, 70, 0xFF0000, 10, "start");
        color(three, 70, 70, 0x00FF00, 20, "at 30%");
        color(three, 138, 70, 0x0000FF, 10, "end");
        BufferedImage pad = box("background:linear-gradient(to right, #f00 20px, #00f 60px)");
        color(pad, 45, 70, 0xFF0000, 0, "before the first stop the color is padded");
        color(pad, 130, 70, 0x0000FF, 0, "after the last stop the color is padded");
        color(pad, 80, 70, 0x7F007F, 14, "between the stops: halfway at 40px");
    }

    /**
     * The interpolation is done in premultiplied space: a transparent stop does not turn the gradient gray or black, the color only
     * fades out.
     * <p>
     * 보간은 사전 곱셈 공간에서 한다: 투명 정지점은 그라디언트를 회색이나 검정으로 만들지 않고 색이 옅어지기만 한다.
     */
    public void testTransparentStopIsPremultiplied() {
        BufferedImage img = box("background:linear-gradient(to right, transparent, #00f)");
        int mid = px(img, 90, 70);
        // 투명 → 파랑의 중간은 불투명도 50% 의 파랑(흰 바탕 위에서 R=G=127, B=255)이다. 사전 곱셈이 아니면 B 가 191 이 된다.
        Check.isTrue((mid & 255) > 235, "blue stays blue: " + Integer.toHexString(mid));
        Check.isTrue(((mid >> 16) & 255) > 110 && ((mid >> 16) & 255) < 145, "and fades towards white: " + Integer.toHexString(mid));
        color(img, 41, 70, 0xFFFFFF, 12, "fully transparent at the start");
        color(img, 138, 70, 0x0000FF, 10, "opaque at the end");
    }

    /**
     * A color hint moves the midpoint of the transition.
     * <p>
     * 색 힌트는 전이의 중간점을 옮긴다.
     */
    public void testColorHint() {
        BufferedImage img = box("background:linear-gradient(to right, #000, 20%, #fff)");
        // 힌트가 20% 이면 20% 지점이 50% 회색이다(x = 40 + 20 = 60).
        int g = grey(img, 60, 70);
        Check.isTrue(g > 112 && g < 142, "the hint position is the midpoint: " + g);
        BufferedImage plain = box("background:linear-gradient(to right, #000, #fff)");
        Check.isTrue(grey(plain, 60, 70) < 70, "without the hint the same point is dark: " + grey(plain, 60, 70));
    }

    /**
     * A repeating linear gradient repeats the pattern between the first and the last stop.
     * <p>
     * 반복 선형 그라디언트는 첫 정지점과 마지막 정지점 사이의 무늬를 되풀이한다.
     */
    public void testRepeatingLinearGradient() {
        BufferedImage img = box("background:repeating-linear-gradient(to right, #f00 0, #f00 10px, #00f 10px, #00f 20px)");
        color(img, 45, 70, 0xFF0000, 0, "first stripe");
        color(img, 55, 70, 0x0000FF, 0, "second stripe");
        color(img, 65, 70, 0xFF0000, 0, "third stripe");
        color(img, 75, 70, 0x0000FF, 0, "fourth stripe");
        color(img, 135, 70, 0x0000FF, 0, "and the pattern goes on to the end");
    }

    /**
     * A radial gradient: the center has the first color, the farthest corner the last (the default size), <code>closest-side</code>
     * ends at the nearest side, and a circle has the same radius in both directions.
     * <p>
     * 방사형 그라디언트: 중심은 첫 색이고 가장 먼 모서리가 마지막 색이다(기본 크기). <code>closest-side</code> 는 가장 가까운 변에서 끝나고 원은 두 방향의 반지름이 같다.
     */
    public void testRadialGradient() {
        BufferedImage img = box("background:radial-gradient(#fff, #000)");
        Check.isTrue(grey(img, 90, 70) > 245, "the center is the first color");
        Check.isTrue(grey(img, 41, 41) < 30, "the corner is the last color");
        BufferedImage side = box("background:radial-gradient(circle closest-side, #fff, #000);background-color:#f00");
        // 가장 가까운 변까지의 거리는 30 이므로 반지름 30 에서 검정이 되고 그 바깥(가로로 먼 곳)은 마지막 색으로 채워진다.
        Check.isTrue(grey(side, 90, 70) > 245, "the center is white");
        Check.isTrue(grey(side, 90, 98) < 30, "28px below the center, near the end of the radius (30px), it is almost black");
        Check.isTrue(grey(side, 130, 70) < 30, "and so is a point to the right beyond the circle radius (padded)");
        Check.isTrue(Math.abs(grey(side, 100, 70) - grey(side, 90, 80)) < 14, "a circle: the same color at the same distance (10px) in both directions");
        BufferedImage ell = box("background:radial-gradient(ellipse closest-side, #fff, #000)");
        // 타원은 가로 반지름 50 세로 반지름 30: 가운데에서 가로 25 와 세로 15 는 같은 50% 이다.
        Check.isTrue(Math.abs(grey(ell, 115, 70) - grey(ell, 90, 85)) < 14, "an ellipse: 25px right and 15px down are both at 50%");
    }

    /**
     * The <code>at</code> position moves the center of a radial gradient.
     * <p>
     * <code>at</code> 위치는 방사형 그라디언트의 중심을 옮긴다.
     */
    public void testRadialGradientPosition() {
        BufferedImage img = box("background:radial-gradient(circle farthest-side at 25% 50%, #fff, #000)");
        Check.isTrue(grey(img, 65, 70) > 245, "the center is at 25% of the width (65)");
        Check.isTrue(grey(img, 115, 70) < grey(img, 80, 70) - 60, "darker to the right");
    }

    /**
     * A conic gradient goes clockwise from the top: the quadrant of the first color is the top right.
     * <p>
     * 원뿔형 그라디언트는 위쪽에서 시계 방향으로 간다: 첫 색의 사분면은 오른쪽 위이다.
     */
    public void testConicGradient() {
        BufferedImage img = box("background:conic-gradient(#f00 0deg 90deg, #00f 90deg 360deg)");
        color(img, 110, 55, 0xFF0000, 0, "top right is red (angle 45 degrees)");
        color(img, 70, 55, 0x0000FF, 0, "top left is blue");
        color(img, 70, 85, 0x0000FF, 0, "bottom left is blue");
        color(img, 110, 85, 0x0000FF, 0, "bottom right is blue (angle 135 degrees)");
        BufferedImage from = box("background:conic-gradient(from 90deg, #f00 0deg 90deg, #00f 90deg 360deg)");
        color(from, 110, 85, 0xFF0000, 0, "from 90deg rotates the start: bottom right is red");
        color(from, 110, 55, 0x0000FF, 0, "and the top right is blue");
    }

    /**
     * A gradient on an element with radius is clipped to the rounded shape.
     * <p>
     * 반지름이 있는 요소의 그라디언트는 둥근 도형으로 잘린다.
     */
    public void testGradientIsClippedToRadius() {
        BufferedImage img = box("background:linear-gradient(#f00, #f00);border-radius:30px");
        color(img, 41, 41, 0xFFFFFF, 0, "the corner is cut");
        color(img, 90, 70, 0xFF0000, 0, "the center is painted");
    }

    /**
     * <code>background-size</code> and <code>no-repeat</code> place one small gradient tile; the four-value position measures from the
     * right and bottom edges.
     * <p>
     * <code>background-size</code> 와 <code>no-repeat</code> 는 작은 그라디언트 타일 하나를 놓고, 4 값 위치는 오른쪽과 아래쪽 가장자리에서 잰다.
     */
    public void testSizePositionAndEdgeOffsets() {
        BufferedImage img = box("background:linear-gradient(#f00,#f00) right 10px bottom 5px / 20px 15px no-repeat");
        // 상자는 (40,40)-(140,100). 타일은 오른쪽에서 10px, 아래에서 5px 떨어진 20 x 15: x 110..130, y 80..95.
        color(img, 120, 88, 0xFF0000, 0, "inside the tile");
        color(img, 108, 88, 0xFFFFFF, 0, "left of the tile");
        color(img, 132, 88, 0xFFFFFF, 0, "right of the tile");
        color(img, 120, 78, 0xFFFFFF, 0, "above the tile");
        color(img, 120, 97, 0xFFFFFF, 0, "below the tile");
        BufferedImage pct = box("background:linear-gradient(#0a0,#0a0) 100% 100% / 50% 50% no-repeat");
        color(pct, 120, 85, 0x00AA00, 0, "50% x 50% tile at the bottom right");
        color(pct, 70, 55, 0xFFFFFF, 0, "nothing at the top left");
    }

    /**
     * <code>repeat</code> tiles in both directions, <code>space</code> spreads whole tiles with gaps, <code>round</code> scales the tiles
     * to a whole number.
     * <p>
     * <code>repeat</code> 는 두 방향으로 타일을 놓고, <code>space</code> 는 온전한 타일을 틈과 함께 펼치며, <code>round</code> 는 타일을 정수 개가 되도록 키운다.
     */
    public void testRepeatSpaceRound() {
        String tile = "linear-gradient(90deg,#f00 50%,#00f 50%)";
        BufferedImage rep = box("background:" + tile + " 0 0 / 30px 30px repeat-x");
        color(rep, 50, 50, 0xFF0000, 0, "first tile, red half");
        color(rep, 62, 50, 0x0000FF, 0, "first tile, blue half");
        color(rep, 80, 50, 0xFF0000, 0, "second tile (30px)");
        color(rep, 50, 75, 0xFFFFFF, 0, "repeat-x does not repeat downwards (the tile is 30px high)");
        // space: 100 / 30 = 3 타일, 남은 10px 는 틈 두 곳에 5px 씩.
        BufferedImage sp = box("background:" + tile + " 0 0 / 30px 30px space no-repeat;background-repeat:space no-repeat");
        color(sp, 45, 50, 0xFF0000, 0, "first tile");
        color(sp, 72, 50, 0xFFFFFF, 0, "the gap between the first and the second tile (x = 70..75)");
        color(sp, 80, 50, 0xFF0000, 0, "second tile starts at 75");
        // round: 100 / 30 = 3.33 → 3 타일, 타일 너비 33.33.
        BufferedImage rd = box("background:" + tile + " 0 0 / 30px 30px;background-repeat:round no-repeat");
        color(rd, 46, 50, 0xFF0000, 0, "round: first tile red half (0..16.7)");
        color(rd, 62, 50, 0x0000FF, 0, "round: first tile blue half (16.7..33.3)");
        color(rd, 80, 50, 0xFF0000, 0, "round: second tile starts at 33.3 (x = 73.3)");
        color(rd, 138, 50, 0x0000FF, 0, "round: the last tile ends exactly at the right edge");
    }

    /**
     * Several layers: the first layer is on top; the background color is below all layers.
     * <p>
     * 여러 레이어: 첫 레이어가 가장 위에 있고 배경색은 모든 레이어 아래에 있다.
     */
    public void testSeveralLayers() {
        BufferedImage img = box("background:linear-gradient(#f00,#f00) 0 0 / 50px 30px no-repeat, linear-gradient(#0f0,#0f0) 0 0 / 80px 60px no-repeat, #00f");
        color(img, 60, 50, 0xFF0000, 0, "the first layer is on top");
        color(img, 100, 50, 0x00FF00, 0, "the second layer below it");
        color(img, 130, 50, 0x0000FF, 0, "the color below all layers");
    }

    /**
     * <code>background-clip</code> and <code>background-origin</code> choose the area of a layer: with a border and padding the
     * <code>padding-box</code> clip leaves the border area, the <code>content-box</code> clip the padding too.
     * <p>
     * <code>background-clip</code> 과 <code>background-origin</code> 은 레이어의 영역을 고른다: border 와 padding 이 있을 때 <code>padding-box</code> 클립은 border 영역을 비우고
     * <code>content-box</code> 클립은 padding 도 비운다.
     */
    public void testClipAndOrigin() {
        String base = "width:60px;height:20px;padding:10px;border:10px solid rgba(0,0,0,0);background:linear-gradient(#f00,#f00)";
        BufferedImage border = box(base + ";background-clip:border-box");
        color(border, 45, 70, 0xFF0000, 0, "border-box: the border area is painted");
        BufferedImage padding = box(base + ";background-clip:padding-box");
        color(padding, 45, 70, 0xFFFFFF, 0, "padding-box: the border area is empty");
        color(padding, 55, 70, 0xFF0000, 0, "padding-box: the padding area is painted");
        BufferedImage content = box(base + ";background-clip:content-box");
        color(content, 55, 70, 0xFFFFFF, 0, "content-box: the padding area is empty");
        color(content, 75, 70, 0xFF0000, 0, "content-box: the content area is painted");
        // origin: border-box 이면 그라디언트(크기 auto = 위치 기준 영역)가 border box 전체에 걸친다.
        BufferedImage origin = box(base.replace("linear-gradient(#f00,#f00)", "linear-gradient(to right,#f00,#00f)") + ";background-origin:border-box;background-clip:border-box");
        color(origin, 41, 70, 0xFF0000, 12, "origin border-box: the gradient starts at the border edge");
        color(origin, 138, 70, 0x0000FF, 12, "and ends at the opposite border edge");
    }

    /**
     * A bitmap layer above a gradient layer, and an image with <code>round</code>/<code>space</code> repeat.
     * <p>
     * 그라디언트 레이어 위의 비트맵 레이어, <code>round</code>/<code>space</code> 반복의 이미지.
     */
    public void testImageAndGradientLayers() {
        int[] argb = new int[4];
        Arrays.fill(argb, 0xFF00FF00);
        RenderTestSupport.FixedImages fi = new RenderTestSupport.FixedImages();
        fi.put("http://t.test/dir/g.png", ImageResource.loaded(new DecodedImage(2, 2, argb)));
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:40px;width:100px;height:60px;"
                + "background:url(g.png) 10px 10px / 20px 20px no-repeat, linear-gradient(#f00,#f00)'></div>", 400, 300, fi);
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        color(img, 60, 60, 0x00FF00, 0, "the bitmap layer is on top");
        color(img, 100, 60, 0xFF0000, 0, "the gradient below it");
        RenderedPage q = RenderTestSupport.page("<div id=a style='margin:40px;width:100px;height:60px;"
                + "background:url(g.png) 0 0 / 30px 60px;background-repeat:space no-repeat'></div>", 400, 300, fi);
        BufferedImage iq = RenderTestSupport.paint(q, 400, 300);
        color(iq, 50, 70, 0x00FF00, 0, "space: first tile");
        color(iq, 72, 70, 0xFFFFFF, 0, "space: the gap");
        color(iq, 80, 70, 0x00FF00, 0, "space: second tile");
        List<DisplayCommand> cmds = q.getDisplayList().getCommands();
        int layers = 0;
        // 배경 이미지 명령의 수를 센다.
        for (int i = 0; i < cmds.size(); i++) {
            if (cmds.get(i).getType() == DisplayCommand.Type.BACKGROUND_IMAGE) {
                layers++;
            }
        }
        Check.equal(1, layers, "one layer command for the tiled image");
    }

    /**
     * A background image that is still loading paints nothing (the gradient layer below it still shows); when the image has been loaded
     * and the page is built again, the layer is there.
     * <p>
     * 아직 읽는 중인 배경 이미지는 아무것도 그리지 않는다(아래 그라디언트 레이어는 보인다). 이미지를 읽고 페이지를 다시 만들면 레이어가 나타난다.
     */
    public void testImageLayerAppearsWhenLoaded() {
        String html = "<div id=a style='margin:40px;width:100px;height:60px;background:url(late.png) 0 0 / 100% 100% no-repeat, linear-gradient(#f00,#f00)'></div>";
        RenderTestSupport.FixedImages pending = new RenderTestSupport.FixedImages().put("http://t.test/dir/late.png", ImageResource.PENDING);
        BufferedImage before = RenderTestSupport.paint(RenderTestSupport.page(html, 400, 300, pending), 400, 300);
        color(before, 90, 70, 0xFF0000, 0, "pending: only the gradient below is painted");
        int[] argb = new int[4];
        Arrays.fill(argb, 0xFF0000FF);
        RenderTestSupport.FixedImages loaded = new RenderTestSupport.FixedImages().put("http://t.test/dir/late.png", ImageResource.loaded(new DecodedImage(2, 2, argb)));
        BufferedImage after = RenderTestSupport.paint(RenderTestSupport.page(html, 400, 300, loaded), 400, 300);
        color(after, 90, 70, 0x0000FF, 0, "loaded: the image layer covers the gradient");
        RenderTestSupport.FixedImages failed = new RenderTestSupport.FixedImages().put("http://t.test/dir/late.png", ImageResource.failed("404"));
        BufferedImage bad = RenderTestSupport.paint(RenderTestSupport.page(html, 400, 300, failed), 400, 300);
        color(bad, 90, 70, 0xFF0000, 0, "failed: the layer is skipped and the gradient shows");
    }

    /**
     * The gradient of the root element or <code>body</code> covers the whole canvas.
     * <p>
     * 루트 요소나 <code>body</code> 의 그라디언트는 캔버스 전체를 덮는다.
     */
    public void testCanvasGradient() {
        RenderedPage p = RenderTestSupport.page("<div style='height:10px'></div>", 400, 300, null);
        BufferedImage plain = RenderTestSupport.paint(p, 400, 300);
        color(plain, 200, 200, 0xFFFFFF, 0, "no background without a style");
        RenderedPage g = RenderTestSupport.page("<style>body{background:linear-gradient(#000,#fff)}</style><div style='height:10px'></div>", 400, 300, null);
        BufferedImage img = RenderTestSupport.paint(g, 400, 300);
        Check.isTrue(grey(img, 200, 5) < 20, "the top of the canvas is black: " + grey(img, 200, 5));
        Check.isTrue(grey(img, 200, 295) > 235, "the bottom of the canvas is white: " + grey(img, 200, 295));
    }
}
