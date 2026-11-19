package org.duckdns.hjow.swingwebpanel.render;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * A background that is clipped to the glyphs of the text of its element (<code>background-clip: text</code>, CSS Backgrounds and Borders
 * Level 4, https://www.w3.org/TR/css-backgrounds-4/#background-clip; also the prefixed <code>-webkit-background-clip: text</code>). The parts (the
 * background color and the layers) are painted into a buffer, the text of the element is painted into a second buffer with an opaque color
 * through the same text commands as the page uses (so the glyph shapes, the spacing, the bidirectional order and the decorations are exactly
 * those of the text), and the first buffer keeps only what lies under the opaque pixels of the second one (the composite of the alpha
 * channel of the text onto the background). The text itself is painted by its own commands in its own color afterwards, as usual
 * (typically transparent).
 * <p>
 * 요소의 텍스트 글리프로 자른 배경(<code>background-clip: text</code>, CSS Backgrounds and Borders Level 4. 접두사 붙은 <code>-webkit-background-clip: text</code> 도 같다).
 * 부분(배경색과 레이어)은 버퍼 하나에 그리고, 요소의 텍스트는 페이지가 쓰는 것과 같은 텍스트 명령으로 불투명한 색으로 둘째 버퍼에 그리므로(글리프 모양, 간격, 양방향 순서, 장식선이
 * 텍스트와 정확히 같다) 첫 버퍼에는 둘째 버퍼의 불투명한 화소 아래에 놓인 것만 남는다(텍스트의 알파 채널을 배경에 합성). 텍스트 자체는 평소처럼 나중에 자신의 명령이 자신의 색으로
 * 그린다(보통 투명).
 */
final class TextClipBackground extends DisplayCommand {

    /** The commands of the background (the color fill and the layers), bottom first. 배경의 명령(색 채우기와 레이어). 아래가 먼저. */
    private final DisplayCommand[] parts;

    /** The text commands of the element in an opaque color. 요소의 텍스트 명령(불투명한 색). */
    private final DisplayCommand[] mask;

    /**
     * Creates the command; its bounds are the part of the background that the text can cover.
     * <p>
     * 명령을 만든다. 영역은 텍스트가 덮을 수 있는 배경의 부분이다.
     *
     * @param parts the background commands / 배경 명령
     * @param mask  the text commands in an opaque color / 불투명한 색의 텍스트 명령
     * @param node  the DOM node or null / DOM 노드 또는 null
     */
    TextClipBackground(List<DisplayCommand> parts, List<DisplayCommand> mask, Node node) {
        super(Type.BACKGROUND_IMAGE, area(parts, mask)[0], area(parts, mask)[1], area(parts, mask)[2], area(parts, mask)[3], node);
        this.parts = parts.toArray(new DisplayCommand[parts.size()]);
        this.mask = mask.toArray(new DisplayCommand[mask.size()]);
    }

    /**
     * Returns the intersection of the bounds of the background and the bounds of the text, or an empty area if there is no text.
     * <p>
     * 배경의 영역과 텍스트의 영역의 교집합을 반환한다. 텍스트가 없으면 빈 영역이다.
     *
     * @param parts the background commands / 배경 명령
     * @param mask  the text commands / 텍스트 명령
     * @return {x, y, w, h} / {x, y, w, h}
     */
    private static double[] area(List<DisplayCommand> parts, List<DisplayCommand> mask) {
        Rectangle2D bg = union(parts);
        Rectangle2D tx = union(mask);
        if (bg == null || tx == null) {
            return new double[4];
        }
        Rectangle2D r = bg.createIntersection(tx);
        return r.isEmpty() ? new double[4] : new double[] { r.getX(), r.getY(), r.getWidth(), r.getHeight() };
    }

    /**
     * Returns the union of the bounds of commands, or null for no commands.
     * <p>
     * 명령들의 영역의 합집합을 반환한다. 명령이 없으면 null.
     *
     * @param list the commands / 명령
     * @return the union or null / 합집합. 없으면 null
     */
    private static Rectangle2D union(List<DisplayCommand> list) {
        Rectangle2D r = null;
        // 명령의 영역을 하나씩 합친다.
        for (int i = 0; i < list.size(); i++) {
            Rectangle2D b = list.get(i).getBounds();
            r = r == null ? b : r.createUnion(b);
        }
        return r;
    }

    /**
     * Paints the background into a buffer, keeps what is under the text and draws the buffer.
     * <p>
     * 배경을 버퍼에 그리고 텍스트 아래의 것만 남겨 버퍼를 그린다.
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        Graphics2D g = ctx.g;
        Rectangle2D area = getBounds().createIntersection(ctx.clipDoc);
        if (area.isEmpty() || mask.length == 0) {
            return;
        }
        AffineTransform at = g.getTransform();
        Rectangle2D dev = at.createTransformedShape(area).getBounds2D();
        int x0 = (int) Math.floor(dev.getMinX());
        int y0 = (int) Math.floor(dev.getMinY());
        int w = (int) Math.ceil(dev.getMaxX()) - x0;
        int h = (int) Math.ceil(dev.getMaxY()) - y0;
        if (w <= 0 || h <= 0 || (long) w * h > 64L * 1024 * 1024) {
            return;
        }
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage text = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        paintInto(buf, ctx, parts, at, x0, y0, area);
        paintInto(text, ctx, mask, at, x0, y0, area);
        Graphics2D gb = buf.createGraphics();
        try {
            // 배경에는 텍스트의 알파가 있는 곳만 남긴다.
            gb.setComposite(AlphaComposite.DstIn);
            gb.drawImage(text, 0, 0, null);
        } finally {
            gb.dispose();
        }
        Composite old = g.getComposite();
        g.setTransform(new AffineTransform());
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(buf, x0, y0, null);
        g.setComposite(old);
        g.setTransform(at);
    }

    /**
     * Paints commands into a buffer that maps to the device area at (x0, y0).
     * <p>
     * 기기 영역 (x0, y0) 에 대응하는 버퍼에 명령을 그린다.
     *
     * @param buf  the buffer / 버퍼
     * @param ctx  the context / 컨텍스트
     * @param cmds the commands / 명령
     * @param at   the transform of the graphics of the page / 페이지 그래픽스의 변환
     * @param x0   left of the buffer in device coordinates / 기기 좌표에서 버퍼의 왼쪽
     * @param y0   top of the buffer / 버퍼의 위쪽
     * @param area the visible area in user space / 사용자 공간에서 보이는 영역
     */
    private static void paintInto(BufferedImage buf, PaintContext ctx, DisplayCommand[] cmds, AffineTransform at, int x0, int y0, Rectangle2D area) {
        Graphics2D g2 = buf.createGraphics();
        try {
            g2.setRenderingHints(ctx.g.getRenderingHints());
            AffineTransform t2 = new AffineTransform();
            t2.translate(-x0, -y0);
            t2.concatenate(at);
            g2.setTransform(t2);
            g2.clip(area);
            PaintContext inner = ctx.with(g2);
            // 명령을 차례로 그린다.
            for (int i = 0; i < cmds.length; i++) {
                if (cmds[i].intersects(ctx.clipDoc)) {
                    cmds[i].paint(inner);
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
