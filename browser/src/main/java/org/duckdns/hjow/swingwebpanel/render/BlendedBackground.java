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
 * The background of a box whose layers are blended with each other (plan item P10-08, <code>background-blend-mode</code> of CSS
 * Compositing and Blending Level 1 section 3.4.3): the background color and the layers (the bottom layer first) are painted into a
 * buffer of their own, every layer with its blend mode over what is below it in the buffer, and the buffer is drawn onto the page
 * normally. The background of the box therefore does not blend with what is behind the box (that is <code>mix-blend-mode</code>).
 * <p>
 * 레이어끼리 혼합하는 상자의 배경(계획 P10-08, CSS Compositing and Blending Level 1 3.4.3 절의 <code>background-blend-mode</code>): 배경색과 레이어(아래 레이어부터)를 자신의
 * 버퍼에 그리되 레이어마다 버퍼에서 그 아래 있는 것 위에 자신의 혼합 모드로 그리고, 버퍼를 페이지에 보통 방식으로 그린다. 따라서 상자의 배경은 상자 뒤에 있는 것과는 혼합하지 않는다
 * (그것은 <code>mix-blend-mode</code> 이다).
 */
final class BlendedBackground extends DisplayCommand {

    /** The commands of the color and the layers, bottom first. 배경색과 레이어의 명령(아래가 먼저). */
    private final DisplayCommand[] parts;

    /** The blend mode of every part ({@link BlendModes}). 부분마다의 혼합 모드({@link BlendModes}). */
    private final int[] modes;

    /**
     * Creates the command.
     * <p>
     * 명령을 만든다.
     *
     * @param parts the commands of the color and the layers, bottom first / 배경색과 레이어의 명령(아래가 먼저)
     * @param modes the blend mode of every part / 부분마다의 혼합 모드
     * @param node  the DOM node or null / DOM 노드 또는 null
     */
    BlendedBackground(List<DisplayCommand> parts, int[] modes, Node node) {
        super(Type.BACKGROUND_IMAGE, union(parts)[0], union(parts)[1], union(parts)[2], union(parts)[3], node);
        this.parts = parts.toArray(new DisplayCommand[parts.size()]);
        this.modes = modes.clone();
    }

    /**
     * Returns the union of the areas of the parts.
     * <p>
     * 부분들의 영역의 합집합을 반환한다.
     *
     * @param parts the parts / 부분
     * @return {x, y, width, height} / {x, y, 너비, 높이}
     */
    private static double[] union(List<DisplayCommand> parts) {
        Rectangle2D r = null;
        // 부분의 영역을 하나씩 합친다.
        for (int i = 0; i < parts.size(); i++) {
            Rectangle2D b = parts.get(i).getBounds();
            r = r == null ? b : r.createUnion(b);
        }
        return r == null ? new double[4] : new double[] { r.getX(), r.getY(), r.getWidth(), r.getHeight() };
    }

    /**
     * Paints the parts into a buffer with their blend modes and draws the buffer.
     * <p>
     * 부분을 혼합 모드로 버퍼에 그리고 버퍼를 그린다.
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        Graphics2D g = ctx.g;
        Rectangle2D area = getBounds().createIntersection(ctx.clipDoc);
        if (area.isEmpty()) {
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
        Graphics2D g2 = buf.createGraphics();
        try {
            g2.setRenderingHints(g.getRenderingHints());
            AffineTransform t2 = new AffineTransform();
            t2.translate(-x0, -y0);
            t2.concatenate(at);
            g2.setTransform(t2);
            g2.clip(area);
            PaintContext inner = ctx.with(g2);
            // 아래 부분부터 자신의 혼합 모드로 그 아래의 것 위에 그린다.
            for (int i = 0; i < parts.length; i++) {
                g2.setComposite(modes[i] == BlendModes.NORMAL ? AlphaComposite.SrcOver : BlendModes.composite(modes[i], 1f));
                if (parts[i].intersects(ctx.clipDoc)) {
                    parts[i].paint(inner);
                }
            }
        } finally {
            g2.dispose();
        }
        Composite old = g.getComposite();
        g.setTransform(new AffineTransform());
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(buf, x0, y0, null);
        g.setComposite(old);
        g.setTransform(at);
    }
}
