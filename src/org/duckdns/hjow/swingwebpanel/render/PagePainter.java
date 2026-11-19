package org.duckdns.hjow.swingwebpanel.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Paints a {@link DisplayList} with Java2D (plan items P09-04, P09-05). The painter maps the document to the component with
 * <code>device = (document - scroll) * scale</code>, the same transform that {@link HitTester} inverts for input, skips every command (and
 * every clip or opacity group) outside the clip rectangle, and puts all state of the {@link Graphics2D} back when it is done (transform,
 * clip, color, paint, stroke, font, composite and all rendering hints). It sets only hints of the graphics it was given: shapes are drawn
 * without anti-aliasing so that boxes and borders are crisp, text is anti-aliased, no JVM-wide Java2D property is touched and nothing
 * depends on OpenGL or Direct3D. Opacity groups are painted into an off-screen image and blended once. The painter reads the display list
 * only, so it may run on the event dispatch thread while the document thread builds the next list. A fixed group
 * ({@link DisplayCommand.Type#FIXED_START}) undoes the scroll offset, so a <code>position:fixed</code> box stays at the same place of the
 * viewport.
 * <p>
 * {@link DisplayList} 를 Java2D 로 그린다(계획 P09-04, P09-05). 문서를 컴포넌트에 <code>기기 좌표 = (문서 좌표 − 스크롤) × 배율</code> 로 대응시키며
 * 이는 {@link HitTester} 가 입력을 위해 역변환하는 것과 같은 변환이다. 클립 사각형 밖의 모든 명령(과 클립/불투명도 그룹)을 건너뛰고 끝나면
 * {@link Graphics2D} 의 모든 상태(변환, 클립, 색, 페인트, 스트로크, 글꼴, 합성, 모든 렌더링 힌트)를 되돌린다. 받은 그래픽스의 힌트만 설정한다: 도형은
 * 상자와 경계선이 또렷하도록 안티앨리어싱 없이 그리고 텍스트는 안티앨리어싱을 쓰며, JVM 전역 Java2D 속성을 건드리지 않고 OpenGL 이나 Direct3D 에
 * 의존하지 않는다. 불투명도 그룹은 화면 밖 이미지에 그린 뒤 한 번 섞는다. 디스플레이 리스트만 읽으므로 문서 스레드가 다음 리스트를 만드는 동안 이벤트
 * 디스패치 스레드에서 실행할 수 있다. fixed 그룹({@link DisplayCommand.Type#FIXED_START})은 스크롤 오프셋을 되돌리므로 <code>position:fixed</code> 상자는
 * 뷰포트의 같은 자리에 머문다.
 */
public final class PagePainter {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PagePainter() {
    }

    /**
     * Paints a display list at animation time 0.
     * <p>
     * 디스플레이 리스트를 애니메이션 시각 0 으로 그린다.
     *
     * @param g       the graphics of the component / 컴포넌트의 그래픽스
     * @param list    the display list / 디스플레이 리스트
     * @param clip    the area to paint in component (device) coordinates, null for the clip of the graphics / 컴포넌트(기기) 좌표로 그릴 영역. null 이면 그래픽스의 클립
     * @param scrollX horizontal scroll offset in document px / 문서 px 단위 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset in document px / 세로 스크롤 오프셋
     * @param scale   the zoom factor, 1 for 100 percent / 확대 배율. 1 이면 100 %
     */
    public static void paint(Graphics2D g, DisplayList list, Rectangle clip, int scrollX, int scrollY, double scale) {
        paint(g, list, clip, scrollX, scrollY, scale, 0L);
    }

    /**
     * Paints a display list.
     * <p>
     * 디스플레이 리스트를 그린다.
     *
     * @param g          the graphics of the component / 컴포넌트의 그래픽스
     * @param list       the display list / 디스플레이 리스트
     * @param clip       the area to paint in device coordinates, null for the clip of the graphics / 기기 좌표로 그릴 영역. null 이면 그래픽스의 클립
     * @param scrollX    horizontal scroll offset in document px / 문서 px 단위 가로 스크롤 오프셋
     * @param scrollY    vertical scroll offset in document px / 세로 스크롤 오프셋
     * @param scale      the zoom factor / 확대 배율
     * @param timeMillis animation time in milliseconds (selects the frame of animated images) / 애니메이션 시각(밀리초). 애니메이션 이미지의 프레임을 고른다
     */
    public static void paint(Graphics2D g, DisplayList list, Rectangle clip, int scrollX, int scrollY, double scale, long timeMillis) {
        paint(g, list, clip, scrollX, scrollY, scale, timeMillis, scrollX, scrollY);
    }

    /**
     * Paints a display list of a page whose scroll is done by the host: the origin of the graphics may already be moved by the scroll (a
     * <code>JScrollPane</code> moves the component), so the offset that is applied here and the position of the viewport are separate. The
     * viewport position is what <code>position:fixed</code> boxes stay at and what <code>position:sticky</code> boxes stick to; the size of
     * the viewport is the one that the layout used.
     * <p>
     * 스크롤을 호스트가 하는 페이지의 디스플레이 리스트를 그린다: 그래픽스의 원점이 이미 스크롤만큼 옮겨져 있을 수 있으므로(<code>JScrollPane</code> 은 컴포넌트를 옮긴다)
     * 여기서 적용하는 오프셋과 뷰포트의 위치를 따로 받는다. 뷰포트의 위치는 <code>position:fixed</code> 상자가 머무르고 <code>position:sticky</code> 상자가 붙는
     * 기준이며 뷰포트의 크기는 레이아웃이 쓴 것이다.
     *
     * @param g          the graphics of the component / 컴포넌트의 그래픽스
     * @param list       the display list / 디스플레이 리스트
     * @param clip       the area to paint in device coordinates, null for the clip of the graphics / 기기 좌표로 그릴 영역. null 이면 그래픽스의 클립
     * @param scrollX    horizontal offset that is applied to the graphics here (0 if the host moved it already) / 여기서 그래픽스에 적용하는 가로 오프셋(호스트가 이미 옮겼으면 0)
     * @param scrollY    vertical offset applied here / 여기서 적용하는 세로 오프셋
     * @param scale      the zoom factor / 확대 배율
     * @param timeMillis animation time in milliseconds / 애니메이션 시각(밀리초)
     * @param viewX      x of the top-left corner of the viewport in document px / 뷰포트 왼쪽 위 모서리의 문서 px 단위 x
     * @param viewY      y of the top-left corner of the viewport in document px / 뷰포트 왼쪽 위 모서리의 y
     */
    public static void paint(Graphics2D g, DisplayList list, Rectangle clip, int scrollX, int scrollY, double scale, long timeMillis, int viewX, int viewY) {
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be positive");
        }
        Rectangle area = clip != null ? new Rectangle(clip) : g.getClipBounds();
        if (area == null) {
            area = new Rectangle(0, 0, (int) Math.ceil(list.getWidth() * scale), (int) Math.ceil(list.getHeight() * scale));
        }
        AffineTransform oldTransform = g.getTransform();
        Shape oldClip = g.getClip();
        Color oldColor = g.getColor();
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();
        Composite oldComposite = g.getComposite();
        RenderingHints oldHints = (RenderingHints) g.getRenderingHints().clone();
        try {
            g.clip(area);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            // backdrop-filter 가 있으면 배경을 읽을 수 있도록 영역 크기의 이미지에 그린 뒤 마지막에 한 번 옮긴다.
            Graphics2D t = g;
            BufferedImage surface = null;
            if (list.hasBackdrops() && area.width > 0 && area.height > 0 && (long) area.width * area.height <= 64L * 1024 * 1024) {
                surface = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_ARGB);
                t = surface.createGraphics();
                t.setRenderingHints(g.getRenderingHints());
                t.translate(-area.x, -area.y);
                t.clip(area);
            }
            try {
                t.scale(scale, scale);
                t.translate(-scrollX, -scrollY);
                Rectangle2D clipDoc = new Rectangle2D.Double(area.x / scale + scrollX - 1, area.y / scale + scrollY - 1, area.width / scale + 2,
                        area.height / scale + 2);
                Rectangle2D view = new Rectangle2D.Double(viewX, viewY, list.getViewportWidth(), list.getViewportHeight());
                PaintContext ctx = new PaintContext(t, clipDoc, timeMillis, viewX, viewY, surface, view);
                paintRange(ctx, list.array(), 0, list.size());
            } finally {
                if (surface != null) {
                    t.dispose();
                }
            }
            if (surface != null) {
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(surface, area.x, area.y, null);
            }
        } finally {
            g.setTransform(oldTransform);
            g.setClip(oldClip);
            g.setRenderingHints(oldHints);
            g.setComposite(oldComposite);
            g.setFont(oldFont);
            g.setStroke(oldStroke);
            g.setPaint(oldPaint);
            g.setColor(oldColor);
        }
    }

    /**
     * Paints a whole command array (its groups end inside the array) with a context; the picture of <code>element()</code> uses it.
     * <p>
     * 명령 배열 전체(그룹은 배열 안에서 끝난다)를 컨텍스트로 그린다. <code>element()</code> 의 그림이 쓴다.
     *
     * @param ctx  the context / 컨텍스트
     * @param cmds the commands / 명령
     */
    static void paintCommands(PaintContext ctx, DisplayCommand[] cmds) {
        paintRange(ctx, cmds, 0, cmds.length);
    }

    /**
     * Paints the commands from <code>from</code> (inclusive) to <code>to</code> (exclusive), handling the groups structurally.
     * <p>
     * <code>from</code> 부터 <code>to</code> 앞까지의 명령을 그리며 그룹을 구조적으로 처리한다.
     *
     * @param ctx  the context / 컨텍스트
     * @param cmds the commands / 명령
     * @param from first index / 처음 위치
     * @param to   end index / 끝 위치
     */
    private static void paintRange(PaintContext ctx, DisplayCommand[] cmds, int from, int to) {
        int i = from;
        // 명령을 순서대로 그리며 그룹 시작을 만나면 그룹 전체를 한 번에 처리한다.
        while (i < to) {
            DisplayCommand c = cmds[i];
            DisplayCommand.Type t = c.getType();
            if (t == DisplayCommand.Type.CLIP_START) {
                int end = c.getGroupEnd();
                if (c.intersects(ctx.clipDoc)) {
                    Shape old = ctx.g.getClip();
                    ctx.g.clip(c.getClipShape());
                    paintRange(ctx, cmds, i + 1, end);
                    ctx.g.setClip(old);
                }
                i = end + 1;
            } else if (t == DisplayCommand.Type.FIXED_START) {
                int end = c.getGroupEnd();
                PaintContext inner = ctx.fixedInside();
                // 그룹의 좌표는 스크롤 오프셋 0 일 때의 것이므로 보이는 영역도 같은 좌표로 비교한다.
                if (c.intersects(inner.clipDoc)) {
                    AffineTransform saved = ctx.g.getTransform();
                    ctx.g.translate(ctx.scrollX, ctx.scrollY);
                    paintRange(inner, cmds, i + 1, end);
                    ctx.g.setTransform(saved);
                }
                i = end + 1;
            } else if (t == DisplayCommand.Type.STICKY_START) {
                int end = c.getGroupEnd();
                paintStickyGroup(ctx, cmds, (StickyGroup) c, i + 1, end);
                i = end + 1;
            } else if (t == DisplayCommand.Type.TRANSFORM_START) {
                int end = c.getGroupEnd();
                if (c.intersects(ctx.clipDoc)) {
                    paintTransformGroup(ctx, cmds, (TransformGroup) c, i + 1, end);
                }
                i = end + 1;
            } else if (t == DisplayCommand.Type.EFFECT_START) {
                int end = c.getGroupEnd();
                if (c.intersects(ctx.clipDoc)) {
                    paintEffectGroup(ctx, cmds, (EffectGroup) c, i + 1, end);
                }
                i = end + 1;
            } else if (t == DisplayCommand.Type.OPACITY_START) {
                int end = c.getGroupEnd();
                if (c.intersects(ctx.clipDoc)) {
                    paintOpacityGroup(ctx, cmds, c, i + 1, end);
                }
                i = end + 1;
            } else {
                if (c.intersects(ctx.clipDoc)) {
                    c.paint(ctx);
                }
                i++;
            }
        }
    }

    /**
     * Paints a transform group (P10-06): the transform of the group is concatenated to that of the graphics, the visible area is mapped
     * back to the local coordinates of the group for the commands inside, and shapes are anti-aliased when the transform turns or skews
     * (so rotated edges are not jagged). A transform that cannot be inverted paints nothing.
     * <p>
     * 변환 그룹을 그린다(P10-06): 그룹의 변환을 그래픽스의 변환에 이어 붙이고, 안쪽 명령을 위해 보이는 영역을 그룹의 지역 좌표로 되돌리며, 변환이 회전이나 기울임이면
     * 도형을 안티앨리어싱한다(회전한 가장자리가 들쭉날쭉하지 않도록). 역변환할 수 없는 변환은 아무것도 그리지 않는다.
     *
     * @param ctx   the context / 컨텍스트
     * @param cmds  the commands / 명령
     * @param group the group start command / 그룹 시작 명령
     * @param from  first index inside the group / 그룹 안의 처음 위치
     * @param to    end index of the group / 그룹의 끝 위치
     */
    private static void paintTransformGroup(PaintContext ctx, DisplayCommand[] cmds, TransformGroup group, int from, int to) {
        AffineTransform at = group.getTransform();
        AffineTransform inv;
        try {
            inv = at.createInverse();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            return;
        }
        Rectangle2D local = inv.createTransformedShape(ctx.clipDoc).getBounds2D();
        Graphics2D g = ctx.g;
        AffineTransform saved = g.getTransform();
        Object oldAa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        try {
            g.transform(at);
            // 회전이나 기울임이면(축에 나란하지 않으면) 도형의 가장자리를 안티앨리어싱한다.
            if ((at.getType() & (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM)) != 0
                    && Math.abs(at.getShearX()) + Math.abs(at.getShearY()) > 1e-9) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            paintRange(ctx.withClip(local, inv), cmds, from, to);
        } finally {
            g.setTransform(saved);
            if (oldAa != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAa);
            }
        }
    }

    /**
     * Paints the group of a <code>position:sticky</code> box: the shift that the constraint gives for the viewport in the current user
     * space moves the graphics and the visible area, so the content stays at the edge of the viewport and stops at the edge of its containing
     * block (see {@link org.duckdns.hjow.swingwebpanel.layout.StickyConstraint}). Without a known viewport nothing is shifted.
     * <p>
     * <code>position:sticky</code> 상자의 그룹을 그린다: 지금 사용자 공간의 뷰포트에 대해 제약이 정하는 이동량만큼 그래픽스와 보이는 영역을 옮기므로 내용은 뷰포트의 가장자리에
     * 머물다가 포함 블록의 가장자리에서 멈춘다({@link org.duckdns.hjow.swingwebpanel.layout.StickyConstraint} 참조). 뷰포트를 모르면 옮기지 않는다.
     *
     * @param ctx   the context / 컨텍스트
     * @param cmds  the commands / 명령
     * @param group the group start command / 그룹 시작 명령
     * @param from  first index inside the group / 그룹 안의 처음 위치
     * @param to    end index of the group / 그룹의 끝 위치
     */
    private static void paintStickyGroup(PaintContext ctx, DisplayCommand[] cmds, StickyGroup group, int from, int to) {
        double[] d = ctx.viewport == null ? new double[] { 0, 0 } : group.getConstraint().shift(ctx.viewport);
        PaintContext inner = d[0] == 0 && d[1] == 0 ? ctx : ctx.shifted(d[0], d[1]);
        // 옮긴 내용이 보이는지는 보이는 영역을 이동량만큼 되돌려 옮기기 전 영역과 비교한다.
        if (!group.intersects(inner.clipDoc)) {
            return;
        }
        Graphics2D g = ctx.g;
        AffineTransform saved = g.getTransform();
        try {
            g.translate(d[0], d[1]);
            paintRange(inner, cmds, from, to);
        } finally {
            g.setTransform(saved);
        }
    }

    /**
     * Paints an opacity group into an off-screen image and blends it with the group opacity.
     * <p>
     * 불투명도 그룹을 화면 밖 이미지에 그린 뒤 그룹 불투명도로 섞는다.
     *
     * @param ctx   the context / 컨텍스트
     * @param cmds  the commands / 명령
     * @param start the group start command / 그룹 시작 명령
     * @param from  first index inside the group / 그룹 안의 처음 위치
     * @param to    end index of the group / 그룹의 끝 위치
     */
    private static void paintOpacityGroup(PaintContext ctx, DisplayCommand[] cmds, DisplayCommand start, int from, int to) {
        Graphics2D g = ctx.g;
        Rectangle2D area = start.getBounds().createIntersection(ctx.clipDoc);
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
            paintRange(ctx.with(g2, buf), cmds, from, to);
        } finally {
            g2.dispose();
        }
        float alpha = ((DisplayCommands.Group) start).getAlpha();
        int blend = ((DisplayCommands.Group) start).getBlend();
        Composite old = g.getComposite();
        g.setTransform(new AffineTransform());
        // 혼합 모드가 있으면 배경과 혼합하는 합성기로, 없으면 불투명도만 적용하는 source-over 로 그린다.
        g.setComposite(blend == BlendModes.NORMAL ? AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha) : BlendModes.composite(blend, alpha));
        g.drawImage(buf, x0, y0, null);
        g.setComposite(old);
        g.setTransform(at);
    }

    /**
     * Paints an effect group (P10-08): the content is painted into an off-screen image that covers the area of the group (the visible
     * part of it, enlarged by the distance that the filter functions reach so that content outside of the visible area is blurred
     * into it), the filter functions are applied to the image and the image is drawn onto the page.
     * <p>
     * 효과 그룹을 그린다(P10-08): 내용을 그룹 영역을 덮는 화면 밖 이미지에 그리고(보이는 부분을 필터 함수가 닿는 거리만큼 넓혀서, 보이는 영역 밖의 내용이 그 안으로
     * 번지게 한다), 이미지에 필터 함수를 적용한 뒤 페이지에 그린다.
     *
     * @param ctx   the context / 컨텍스트
     * @param cmds  the commands / 명령
     * @param group the group start command / 그룹 시작 명령
     * @param from  first index inside the group / 그룹 안의 처음 위치
     * @param to    end index of the group / 그룹의 끝 위치
     */
    private static void paintEffectGroup(PaintContext ctx, DisplayCommand[] cmds, EffectGroup group, int from, int to) {
        Graphics2D g = ctx.g;
        double[] ext = group.getExtent();
        Rectangle2D c = ctx.clipDoc;
        Rectangle2D vis = new Rectangle2D.Double(c.getX() - ext[0], c.getY() - ext[1], c.getWidth() + ext[0] + ext[2], c.getHeight() + ext[1] + ext[3]);
        Rectangle2D area = group.getBounds().createIntersection(vis);
        if (area.isEmpty()) {
            return;
        }
        // 배경 필터는 요소를 그리기 전에 지금까지 그려진 내용(배경)을 바꾼다.
        if (group.getBackdrop() != null) {
            BackdropFilters.apply(ctx, group.getBackdrop(), group.getBackdropShape());
        }
        // 필터도 마스크도 클립 경로 참조도 없이 배경 필터만 있으면 내용은 버퍼 없이 그대로 그린다.
        if (group.getFilter() == null && group.getMask() == null && group.getClipReference() == null) {
            paintRange(ctx, cmds, from, to);
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
            paintRange(ctx.with(g2, vis, buf), cmds, from, to);
        } finally {
            g2.dispose();
        }
        AffineTransform toBuffer = new AffineTransform();
        toBuffer.translate(-x0, -y0);
        toBuffer.concatenate(at);
        if (group.getFilter() != null) {
            FilterRenderer.apply(buf, group.getFilter(), toBuffer, group.getBox());
        }
        // clip-path: url(#id) 는 필터 다음에 SVG clipPath 요소로 자른다(합성 모델의 순서: 필터, 자르기, 마스크, 불투명도). 사용자 공간은 요소의 border box 이다.
        if (group.getClipReference() != null && group.getBox() != null) {
            AffineTransform ctm = new AffineTransform(toBuffer);
            ctm.translate(group.getBox().getX(), group.getBox().getY());
            group.getClipReference().applyClipPath(group.getClipId(), buf, ctm, group.getBox().getWidth(), group.getBox().getHeight());
        }
        // 마스크는 자르기 다음에 적용한다.
        if (group.getMask() != null) {
            group.getMask().apply(buf, toBuffer, ctx);
        }
        Composite old = g.getComposite();
        g.setTransform(new AffineTransform());
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(buf, x0, y0, null);
        g.setComposite(old);
        g.setTransform(at);
    }
}
