package org.duckdns.hjow.swingwebpanel.render;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting of boxes, borders, backgrounds, clipping, opacity, partial repaint, scaling and the state of the graphics (plan items
 * P09-04, P09-05, P09-09). Every test builds the page with the real parser, CSS engine and layout engine and checks pixels of a headless image.
 * <p>
 * 상자, 경계선, 배경, 클리핑, 불투명도, 부분 다시 그리기, 확대, 그래픽스 상태의 그리기 시험(계획 P09-04, P09-05, P09-09). 시험마다 실제 파서, CSS 엔진,
 * 레이아웃 엔진으로 페이지를 만들고 헤드리스 이미지의 픽셀을 확인한다.
 */
public class PaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PaintTest() {
    }

    /**
     * A background color and four solid borders of different colors appear at the expected coordinates.
     * <p>
     * 배경색과 서로 다른 색의 실선 경계선 네 개가 기대한 좌표에 나타난다.
     */
    public void testBackgroundAndBorders() {
        RenderedPage p = RenderTestSupport.page("<div style='width:100px;height:50px;background:#ff0000;border-style:solid;border-width:4px;"
                + "border-top-color:#0000ff;border-right-color:#00ff00;border-bottom-color:#ffff00;border-left-color:#ff00ff'></div>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 25), "background inside");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 50, 1), "top border");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 106, 25), "right border");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 50, 56), "bottom border");
        Check.equal(0xFF00FF, RenderTestSupport.rgb(img, 1, 25), "left border");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 120, 25), "outside the box stays white");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 50, 70), "below the box stays white");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 4, 4), "the content starts after the border");
        int corner = RenderTestSupport.rgb(img, 0, 0);
        Check.isTrue(corner == 0x0000FF || corner == 0xFF00FF, "the corner belongs to the top or the left side, was " + Integer.toHexString(corner));
    }

    /**
     * Each border side has its own width and the join is mitred (the corner pixel is covered by exactly one side, never blank).
     * <p>
     * 경계선의 변마다 너비가 다르고 모서리는 미터 조인이다(모서리 픽셀은 정확히 한 변이 덮으며 비지 않는다).
     */
    public void testPerSideWidthsAndMiterCorners() {
        RenderedPage p = RenderTestSupport.page("<div style='width:40px;height:40px;border-style:solid;border-color:#000;"
                + "border-top-width:6px;border-left-width:2px;border-right-width:0;border-bottom-width:10px'></div>", 100, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 100, 100);
        Check.equal(0x000000, RenderTestSupport.rgb(img, 20, 5), "top border is 6px thick");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 20, 6), "the content follows the 6px top border");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 1, 20), "left border is 2px thick");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 2, 20), "the content follows the left border");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 20, 55), "bottom border is 10px thick");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 20, 45), "the content ends before the bottom border");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 41, 20), "no right border");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 0, 0), "the top-left corner is painted");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 0, 55), "the bottom-left corner is painted");
    }

    /**
     * Dashed borders have gaps, double borders have a gap between two lines, groove borders have two shades.
     * <p>
     * 파선 경계선은 틈이 있고, 이중선은 두 선 사이에 틈이 있으며, groove 는 두 가지 명암이 있다.
     */
    public void testBorderStyles() {
        RenderedPage p = RenderTestSupport.page("<div style='width:90px;height:20px;margin-bottom:20px;border:6px dashed #000'></div>"
                + "<div style='width:90px;height:20px;margin-bottom:20px;border:9px double #000'></div>"
                + "<div style='width:90px;height:20px;border:8px groove #808080'></div>", 200, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 200);
        int dark = 0;
        int light = 0;
        // 파선의 위쪽 변을 따라 어두운 픽셀과 밝은 픽셀을 센다.
        for (int x = 8; x < 90; x++) {
            if (RenderTestSupport.rgb(img, x, 3) == 0) {
                dark++;
            } else {
                light++;
            }
        }
        Check.isTrue(dark > 10 && light > 10, "the dashed border alternates dashes and gaps: dark=" + dark + " light=" + light);
        Check.equal(0x000000, RenderTestSupport.rgb(img, 40, 52), "double border: outer line");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 40, 55), "double border: the gap between the lines");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 40, 58), "double border: inner line");
        int outer = RenderTestSupport.rgb(img, 40, 111);
        int inner = RenderTestSupport.rgb(img, 40, 115);
        Check.isTrue(outer != inner, "groove: the two halves have different shades");
    }

    /**
     * The background of body fills the whole canvas, also beyond the content.
     * <p>
     * body 의 배경은 콘텐츠를 넘어 캔버스 전체를 채운다.
     */
    public void testCanvasBackground() {
        RenderedPage p = RenderTestSupport.page("<style>body{background:#00ff00}</style><div style='height:10px'>x</div>", 300, 300, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 300);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 299, 299), "the canvas is painted far outside the content");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 150, 200), "the canvas below the content");
        RenderedPage q = RenderTestSupport.page("<style>html{background:#ff0000} body{background:#00ff00}</style><div>x</div>", 100, 100, null);
        BufferedImage img2 = RenderTestSupport.paint(q, 100, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img2, 99, 99), "the root background wins over the body background");
    }

    /**
     * Content of a box with overflow:hidden is clipped to its padding box; overflow:visible is not clipped.
     * <p>
     * overflow:hidden 인 상자의 콘텐츠는 padding box 로 잘리고 overflow:visible 은 잘리지 않는다.
     */
    public void testOverflowClipping() {
        RenderedPage p = RenderTestSupport.page("<div style='width:50px;height:20px;overflow:hidden'>"
                + "<div style='width:200px;height:200px;background:#ff0000'></div></div>"
                + "<div style='margin-top:40px;width:50px;height:20px'>"
                + "<div style='width:200px;height:100px;background:#0000ff'></div></div>", 300, 300, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 300);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 10, 10), "inside the clip");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 60, 10), "clipped on the right");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 10, 30), "clipped at the bottom");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 150, 80), "overflow:visible is not clipped");
    }

    /**
     * An element with opacity is blended as a group.
     * <p>
     * opacity 가 있는 요소는 그룹으로 섞인다.
     */
    public void testOpacityGroup() {
        RenderedPage p = RenderTestSupport.page("<div style='opacity:0.5;width:100px;height:20px;background:#000'>"
                + "<div style='width:50px;height:20px;background:#000'></div></div>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        int c = RenderTestSupport.rgb(img, 75, 10);
        Check.isTrue(RenderTestSupport.near(c, 0x808080, 3), "half transparent black over white is mid gray, was " + Integer.toHexString(c));
        int c2 = RenderTestSupport.rgb(img, 25, 10);
        Check.isTrue(RenderTestSupport.near(c2, 0x808080, 3), "the group is blended once: overlapping children do not darken, was " + Integer.toHexString(c2));
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 150, 10), "outside the group");
    }

    /**
     * Painting only a clip rectangle gives the same pixels inside the rectangle as a full paint and leaves the rest untouched.
     * <p>
     * 클립 사각형만 그리면 사각형 안에서는 전체를 그린 것과 같은 픽셀이고 나머지는 건드리지 않는다.
     */
    public void testPartialRepaintEqualsFullRepaint() {
        RenderedPage p = RenderTestSupport.page("<h1 style='font-size:20px'>Hello</h1><div style='width:80px;height:30px;background:#336699;"
                + "border:3px solid #cc0000'>text <b>bold</b></div><p style='opacity:0.7;background:#ffcc00'>group</p>", 200, 160, null);
        BufferedImage full = RenderTestSupport.paint(p, 200, 160);
        BufferedImage part = RenderTestSupport.blank(200, 160);
        Graphics2D g = part.createGraphics();
        Rectangle clip = new Rectangle(20, 10, 70, 60);
        p.paint(g, clip, 0, 0);
        g.dispose();
        // 클립 안은 전체 그리기와 같아야 하고 밖은 흰색 그대로여야 한다.
        for (int y = 0; y < 160; y++) {
            for (int x = 0; x < 200; x++) {
                boolean inside = clip.contains(x, y);
                int expected = inside ? full.getRGB(x, y) : 0xFFFFFFFF;
                if (part.getRGB(x, y) != expected) {
                    Check.fail("pixel (" + x + "," + y + ") differs: " + Integer.toHexString(part.getRGB(x, y)) + " vs " + Integer.toHexString(expected));
                }
            }
        }
    }

    /**
     * The state of the graphics is restored completely, also when the clip and transform were set by the caller.
     * <p>
     * 호출자가 클립과 변환을 정해 두었어도 그래픽스의 상태가 완전히 복원된다.
     */
    public void testGraphicsStateRestored() {
        RenderedPage p = RenderTestSupport.page("<div style='opacity:0.5;background:#f00;width:50px;height:20px'>text</div>", 100, 100, null);
        BufferedImage img = RenderTestSupport.blank(100, 100);
        Graphics2D g = img.createGraphics();
        AffineTransform tx = AffineTransform.getTranslateInstance(5, 7);
        g.setTransform(tx);
        g.setClip(new Rectangle(0, 0, 90, 90));
        g.setColor(new Color(10, 20, 30));
        g.setStroke(new BasicStroke(3f));
        Font font = new Font("Serif", Font.BOLD, 17);
        g.setFont(font);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        p.paint(g, new Rectangle(0, 0, 80, 80), 0, 0);
        Check.equal(tx, g.getTransform(), "transform");
        Check.equal(new Rectangle(0, 0, 90, 90), g.getClip().getBounds(), "clip");
        Check.equal(new Color(10, 20, 30), g.getColor(), "color");
        Check.equal(3.0, (double) ((BasicStroke) g.getStroke()).getLineWidth(), "stroke");
        Check.equal(font, g.getFont(), "font");
        Check.equal(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f), g.getComposite(), "composite");
        Check.same(RenderingHints.VALUE_ANTIALIAS_ON, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING), "antialiasing hint");
        Check.same(RenderingHints.VALUE_INTERPOLATION_BICUBIC, g.getRenderingHint(RenderingHints.KEY_INTERPOLATION), "interpolation hint");
        g.dispose();
    }

    /**
     * Scroll offset and scale map the document to the component: device = (document - scroll) * scale.
     * <p>
     * 스크롤 오프셋과 배율이 문서를 컴포넌트에 대응시킨다: 기기 = (문서 − 스크롤) × 배율.
     */
    public void testScrollAndScale() {
        RenderedPage p = RenderTestSupport.page("<div style='height:30px'></div><div style='width:40px;height:20px;background:#ff0000'></div>"
                + "<div style='height:300px'></div>", 100, 100, null);
        BufferedImage img = RenderTestSupport.blank(100, 100);
        Graphics2D g = img.createGraphics();
        p.paint(g, new Rectangle(0, 0, 100, 100), 0, 10, 2.0, 0L);
        g.dispose();
        // 문서 y=30..50 의 빨간 상자는 스크롤 10, 배율 2 에서 기기 y=40..80, x=0..80 에 나타난다.
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 40, 60), "scaled box is red inside");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 79, 41), "right edge of the scaled box");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 81, 60), "outside to the right");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 40, 38), "above the box");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 40, 79), "bottom edge of the scaled box");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 40, 81), "below the box");
    }

    /**
     * Display list groups are properly nested: every start has an end after it and ends match their start.
     * <p>
     * 디스플레이 리스트의 그룹은 올바르게 중첩된다: 모든 시작은 뒤에 끝이 있고 끝은 시작과 짝이 맞는다.
     */
    public void testDisplayListGroupsAreNested() {
        RenderedPage p = RenderTestSupport.page("<div style='overflow:hidden;width:50px;height:20px;opacity:0.5'>"
                + "<div style='overflow:hidden;height:5px'>a</div></div><span style='display:inline-block;overflow:hidden;width:5px'>x</span>", 100, 100, null);
        DisplayList l = p.getDisplayList();
        int depth = 0;
        int starts = 0;
        // 명령을 순서대로 보며 시작과 끝의 짝을 센다.
        for (int i = 0; i < l.size(); i++) {
            DisplayCommand c = l.get(i);
            if (c.getType() == DisplayCommand.Type.CLIP_START || c.getType() == DisplayCommand.Type.OPACITY_START) {
                starts++;
                depth++;
                Check.isTrue(c.getGroupEnd() > i, "the end follows the start");
                DisplayCommand end = l.get(c.getGroupEnd());
                Check.isTrue(end.getType() == (c.getType() == DisplayCommand.Type.CLIP_START ? DisplayCommand.Type.CLIP_END
                        : DisplayCommand.Type.OPACITY_END), "the end has the matching type");
            } else if (c.getType() == DisplayCommand.Type.CLIP_END || c.getType() == DisplayCommand.Type.OPACITY_END) {
                depth--;
                Check.isTrue(depth >= 0, "no end without a start");
            }
        }
        Check.equal(0, depth, "all groups are closed");
        Check.isTrue(starts >= 3, "clip and opacity groups exist: " + starts);
    }

    /**
     * A box with visibility:hidden paints nothing but its visible descendants still paint.
     * <p>
     * visibility:hidden 상자는 아무것도 그리지 않지만 보이는 자손은 그린다.
     */
    public void testVisibilityHidden() {
        RenderedPage p = RenderTestSupport.page("<div style='visibility:hidden;width:50px;height:20px;background:#f00'>"
                + "<div style='visibility:visible;width:20px;height:20px;background:#00f'></div></div>", 100, 50, null);
        BufferedImage img = RenderTestSupport.paint(p, 100, 50);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 40, 10), "hidden parent paints no background");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 10, 10), "visible child paints");
    }
}
