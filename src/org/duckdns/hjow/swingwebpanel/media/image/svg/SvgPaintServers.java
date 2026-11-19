package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The paint servers of SVG (plan item P18-04): <code>linearGradient</code>, <code>radialGradient</code> (with <code>href</code>
 * inheritance of attributes and stops, <code>gradientUnits</code>, <code>gradientTransform</code>, <code>spreadMethod</code>, the
 * degenerate cases of the specification) which become Java2D gradient paints, and <code>pattern</code> (tile geometry, view box,
 * <code>patternUnits</code>/<code>patternContentUnits</code>, <code>patternTransform</code>) which is drawn as a texture of one tile that is
 * rendered at the device resolution and repeated.
 * <p>
 * SVG 의 페인트 서버(계획 P18-04): Java2D 그라디언트 페인트가 되는 <code>linearGradient</code>, <code>radialGradient</code>(속성과 stop 의
 * <code>href</code> 상속, <code>gradientUnits</code>, <code>gradientTransform</code>, <code>spreadMethod</code>, 규격의 퇴화 경우)와,
 * 타일 하나를 장치 해상도로 그려 텍스처로 반복해서 칠하는 <code>pattern</code>(타일 기하, viewBox, <code>patternUnits</code>/
 * <code>patternContentUnits</code>, <code>patternTransform</code>).
 */
final class SvgPaintServers {

    /**
     * Maximum width or height of the tile texture in pixels.
     * <p>
     * 타일 텍스처의 최대 너비/높이(픽셀).
     */
    static final int MAX_TEXTURE = 2048;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgPaintServers() {
    }

    /**
     * Looks up an attribute of a paint server, following the <code>href</code> chain to other paint servers of the same family.
     * <p>
     * 페인트 서버의 속성을 찾는다. <code>href</code> 연쇄를 따라 같은 계열의 다른 페인트 서버까지 올라간다.
     *
     * @param r      the renderer / 렌더러
     * @param server the first paint server / 첫 페인트 서버
     * @param name   the attribute name / 속성 이름
     * @param type   if not null, only servers of this element name are consulted (geometry attributes) / null 이 아니면 이 이름의 요소만 참조한다(기하 속성)
     * @return the value or null / 값 또는 null
     */
    static String serverAttr(SvgRenderer r, SvgElement server, String name, String type) {
        Set<SvgElement> seen = new HashSet<SvgElement>();
        SvgElement cur = server;
        // href 연쇄를 따라가며 처음 나오는 값을 찾는다(순환은 끊는다).
        while (cur != null && seen.add(cur)) {
            if (type == null || cur.name.equals(type)) {
                String v = cur.attrs.get(name);
                if (v != null) {
                    return v;
                }
            }
            cur = r.lookup(cur.href());
            if (cur != null && !(cur.is("linearGradient") || cur.is("radialGradient") || cur.is("pattern"))) {
                break;
            }
            if (cur != null && server.is("pattern") != cur.is("pattern")) {
                break;
            }
        }
        return null;
    }

    /**
     * Finds the first paint server along the <code>href</code> chain that has children of the wanted kind.
     * <p>
     * <code>href</code> 연쇄에서 원하는 종류의 자식을 가진 첫 페인트 서버를 찾는다.
     *
     * @param r      the renderer / 렌더러
     * @param server the first paint server / 첫 페인트 서버
     * @return the server that supplies the children, or null / 자식을 제공하는 서버. 없으면 null
     */
    private static SvgElement childSource(SvgRenderer r, SvgElement server) {
        Set<SvgElement> seen = new HashSet<SvgElement>();
        SvgElement cur = server;
        // 연쇄를 따라가며 자식(stop 또는 내용)이 있는 첫 서버를 찾는다.
        while (cur != null && seen.add(cur)) {
            boolean has = false;
            // 자식 요소 중 의미 있는 것이 있는지 본다.
            for (int i = 0; i < cur.children.size(); i++) {
                SvgElement c = cur.children.get(i);
                if (c.isSvg() && (server.is("pattern") ? SvgRenderer.isGraphicalName(c.name) : c.is("stop"))) {
                    has = true;
                    break;
                }
            }
            if (has) {
                return cur;
            }
            SvgElement next = r.lookup(cur.href());
            if (next == null || (server.is("pattern") != next.is("pattern"))
                    || !(next.is("linearGradient") || next.is("radialGradient") || next.is("pattern"))) {
                return null;
            }
            cur = next;
        }
        return null;
    }

    /**
     * Reads a gradient coordinate attribute as a number in the units of the gradient.
     * <p>
     * 그라디언트 좌표 속성을 그라디언트 단위의 수로 읽는다.
     *
     * @param r        the renderer / 렌더러
     * @param server   the gradient / 그라디언트
     * @param name     the attribute / 속성
     * @param type     the element name that carries the attribute / 속성을 가진 요소 이름
     * @param def      the default value (fraction for object bounding box units) / 기본값(객체 경계 상자 단위이면 비율)
     * @param obb      true for object bounding box units / 객체 경계 상자 단위이면 true
     * @param axis     0 for x, 1 for y, 2 for the diagonal / 0 이면 x, 1 이면 y, 2 이면 대각선
     * @param st       the style for units / 단위를 위한 스타일
     * @return the value in gradient units / 그라디언트 단위의 값
     */
    private static double coordinate(SvgRenderer r, SvgElement server, String name, String type, double def, boolean obb, int axis, SvgStyle st) {
        String v = serverAttr(r, server, name, type);
        if (v == null) {
            return def;
        }
        SvgLength l = SvgLength.parse(v);
        if (l == null) {
            return def;
        }
        if (obb) {
            return l.isPercent() ? l.value / 100 : l.value;
        }
        return st.px(l, axis);
    }

    /**
     * Builds the Java2D paint of a gradient element for a shape.
     * <p>
     * 도형을 위한 그라디언트 요소의 Java2D 페인트를 만든다.
     *
     * @param r        the renderer / 렌더러
     * @param server   the gradient element / 그라디언트 요소
     * @param st       the style of the painted element / 칠해지는 요소의 스타일
     * @param bbox     the bounding box of the painted element / 칠해지는 요소의 경계 상자
     * @param fallback the fallback paint when the gradient cannot be used, or null / 그라디언트를 쓸 수 없을 때의 대체 페인트. 없으면 null
     * @return the paint, or null if nothing is painted / 페인트. 아무것도 칠하지 않으면 null
     */
    static Paint gradient(SvgRenderer r, SvgElement server, SvgStyle st, Rectangle2D bbox, SvgPaint fallback) {
        boolean linear = server.is("linearGradient");
        String units = serverAttr(r, server, "gradientUnits", null);
        boolean obb = !"userSpaceOnUse".equals(units);
        if (obb && (bbox == null || bbox.getWidth() <= 0 || bbox.getHeight() <= 0)) {
            return fallbackPaint(fallback);
        }
        List<float[]> stops = new ArrayList<float[]>();
        List<Integer> colors = new ArrayList<Integer>();
        collectStops(r, server, stops, colors);
        if (colors.isEmpty()) {
            // stop 이 하나도 없는 그라디언트는 칠하지 않는다(대체 색도 쓰지 않는다).
            return null;
        }
        if (colors.size() == 1) {
            return new Color(colors.get(0).intValue(), true);
        }
        AffineTransform gt = new AffineTransform();
        String gts = serverAttr(r, server, "gradientTransform", null);
        if (gts != null) {
            AffineTransform t = SvgTransforms.parse(gts, false, 0, 0);
            if (t != null) {
                gt = t;
            }
        }
        AffineTransform full = new AffineTransform();
        if (obb) {
            full.translate(bbox.getX(), bbox.getY());
            full.scale(bbox.getWidth(), bbox.getHeight());
        }
        full.concatenate(gt);
        if (full.getDeterminant() == 0) {
            return null;
        }
        MultipleGradientPaint.CycleMethod cycle = MultipleGradientPaint.CycleMethod.NO_CYCLE;
        String spread = serverAttr(r, server, "spreadMethod", null);
        if ("reflect".equals(spread)) {
            cycle = MultipleGradientPaint.CycleMethod.REFLECT;
        } else if ("repeat".equals(spread)) {
            cycle = MultipleGradientPaint.CycleMethod.REPEAT;
        }
        float[] fractions = new float[stops.size()];
        Color[] cols = new Color[stops.size()];
        // 오프셋은 줄어들지 않게 하고 같은 값은 아주 조금 벌려 Java2D 의 엄격한 증가 조건을 맞춘다.
        float prev = -1;
        for (int i = 0; i < fractions.length; i++) {
            float f = stops.get(i)[0];
            if (f <= prev) {
                f = Math.nextUp(prev + 1e-6f);
            }
            fractions[i] = f;
            prev = f;
            cols[i] = new Color(colors.get(i).intValue(), true);
        }
        // 마지막이 1 을 넘으면 뒤에서부터 앞으로 당겨 모든 값이 [0, 1] 안에 있게 한다.
        if (fractions[fractions.length - 1] > 1f) {
            fractions[fractions.length - 1] = 1f;
            // 뒤에서부터 앞쪽 값이 뒤쪽 값보다 작게 당긴다.
            for (int i = fractions.length - 2; i >= 0; i--) {
                if (fractions[i] >= fractions[i + 1]) {
                    fractions[i] = Math.nextAfter(fractions[i + 1], 0.0) - 1e-6f;
                }
            }
            if (fractions[0] < 0f) {
                fractions[0] = 0f;
            }
        }
        Color last = cols[cols.length - 1];
        if (linear) {
            double x1 = coordinate(r, server, "x1", "linearGradient", 0, obb, 0, st);
            double y1 = coordinate(r, server, "y1", "linearGradient", 0, obb, 1, st);
            double x2 = coordinate(r, server, "x2", "linearGradient", obb ? 1 : st.vpW, obb, 0, st);
            double y2 = coordinate(r, server, "y2", "linearGradient", 0, obb, 1, st);
            if (x1 == x2 && y1 == y2) {
                return last;
            }
            return new LinearGradientPaint(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), fractions, cols, cycle,
                    MultipleGradientPaint.ColorSpaceType.SRGB, full);
        }
        double cx = coordinate(r, server, "cx", "radialGradient", obb ? 0.5 : st.vpW / 2, obb, 0, st);
        double cy = coordinate(r, server, "cy", "radialGradient", obb ? 0.5 : st.vpH / 2, obb, 1, st);
        double rad = coordinate(r, server, "r", "radialGradient", obb ? 0.5 : Math.sqrt((st.vpW * st.vpW + st.vpH * st.vpH) / 2) / 2, obb, 2, st);
        double fx = coordinate(r, server, "fx", "radialGradient", cx, obb, 0, st);
        double fy = coordinate(r, server, "fy", "radialGradient", cy, obb, 1, st);
        if (!(rad > 0)) {
            return last;
        }
        return new RadialGradientPaint(new Point2D.Double(cx, cy), (float) rad, new Point2D.Double(fx, fy), fractions, cols, cycle,
                MultipleGradientPaint.ColorSpaceType.SRGB, full);
    }

    /**
     * Returns the fallback colour of a paint reference as a Java paint, or null.
     * <p>
     * 페인트 참조의 대체 색을 Java 페인트로 반환하며 없으면 null.
     *
     * @param fb the fallback paint or null / 대체 페인트 또는 null
     * @return a colour or null / 색 또는 null
     */
    private static Paint fallbackPaint(SvgPaint fb) {
        if (fb != null && fb.kind == SvgPaint.COLOR) {
            return new Color(fb.argb, true);
        }
        return null;
    }

    /**
     * Reads the stops of a gradient (following the href chain to the first gradient that has stops).
     * <p>
     * 그라디언트의 stop 을 읽는다(href 연쇄를 따라 stop 이 있는 첫 그라디언트까지 간다).
     *
     * @param r      the renderer / 렌더러
     * @param server the gradient / 그라디언트
     * @param stops  receives {offset} per stop / stop 마다 {offset} 을 받는다
     * @param colors receives the colours (with stop-opacity applied) / 색(stop-opacity 적용)을 받는다
     */
    private static void collectStops(SvgRenderer r, SvgElement server, List<float[]> stops, List<Integer> colors) {
        SvgElement src = childSource(r, server);
        if (src == null) {
            return;
        }
        SvgStyle parentStyle = r.styleFor(src);
        double prev = 0;
        // stop 요소를 순서대로 읽고 오프셋이 줄어들지 않게 한다.
        for (int i = 0; i < src.children.size(); i++) {
            SvgElement c = src.children.get(i);
            if (!c.is("stop")) {
                continue;
            }
            SvgStyle cs = new SvgStyle(parentStyle, c, r.vpW, r.vpH);
            double off = fraction(c.attrs.get("offset"), 0);
            if (off < 0) {
                off = 0;
            }
            if (off > 1) {
                off = 1;
            }
            if (off < prev) {
                off = prev;
            }
            prev = off;
            String sc = cs.own("stop-color");
            int color = 0xFF000000;
            if (sc != null) {
                if ("currentcolor".equalsIgnoreCase(sc)) {
                    color = cs.color();
                } else {
                    Integer pc = SvgColors.parse(sc);
                    if (pc != null) {
                        color = pc.intValue();
                    }
                }
            }
            String so = cs.own("stop-opacity");
            double opacity = 1;
            if (so != null) {
                SvgLength ol = SvgLength.parse(so);
                if (ol != null) {
                    opacity = ol.isPercent() ? ol.value / 100 : ol.value;
                    opacity = Math.max(0, Math.min(1, opacity));
                }
            }
            int a = (int) Math.round((color >>> 24) * opacity);
            stops.add(new float[] { (float) off });
            colors.add(Integer.valueOf((a << 24) | (color & 0xFFFFFF)));
        }
    }

    /**
     * Reads a number or percentage as a fraction.
     * <p>
     * 수나 백분율을 비율로 읽는다.
     *
     * @param v   the text or null / 텍스트 또는 null
     * @param def the default / 기본값
     * @return the fraction / 비율
     */
    private static double fraction(String v, double def) {
        return SvgMasks.fraction(v, def);
    }

    /**
     * Fills a shape with a pattern.
     * <p>
     * 도형을 패턴으로 채운다.
     *
     * @param r       the renderer / 렌더러
     * @param pattern the pattern element / pattern 요소
     * @param shape   the shape to fill (user space) / 채울 도형(사용자 공간)
     * @param opacity the opacity / 투명도
     * @param st      the style of the painted element / 칠해지는 요소의 스타일
     * @param bbox    the bounding box of the painted element / 칠해지는 요소의 경계 상자
     */
    static void fillPattern(SvgRenderer r, SvgElement pattern, Shape shape, double opacity, SvgStyle st, Rectangle2D bbox) {
        if (r.active.contains(pattern) || r.active.size() > SvgRenderer.MAX_REFERENCE_DEPTH) {
            return;
        }
        String units = serverAttr(r, pattern, "patternUnits", null);
        boolean obb = !"userSpaceOnUse".equals(units);
        String cunits = serverAttr(r, pattern, "patternContentUnits", null);
        boolean contentObb = "objectBoundingBox".equals(cunits);
        if ((obb || contentObb) && (bbox == null || bbox.getWidth() <= 0 || bbox.getHeight() <= 0)) {
            return;
        }
        double fx = coordinate(r, pattern, "x", null, 0, obb, 0, st);
        double fy = coordinate(r, pattern, "y", null, 0, obb, 1, st);
        double fw = coordinate(r, pattern, "width", null, 0, obb, 0, st);
        double fh = coordinate(r, pattern, "height", null, 0, obb, 1, st);
        double tx = obb ? bbox.getX() + fx * bbox.getWidth() : fx;
        double ty = obb ? bbox.getY() + fy * bbox.getHeight() : fy;
        double tw = obb ? fw * bbox.getWidth() : fw;
        double th = obb ? fh * bbox.getHeight() : fh;
        if (!(tw > 0) || !(th > 0)) {
            return;
        }
        AffineTransform pt = new AffineTransform();
        String pts = serverAttr(r, pattern, "patternTransform", null);
        if (pts != null) {
            AffineTransform t = SvgTransforms.parse(pts, false, 0, 0);
            if (t != null) {
                pt = t;
            }
        }
        if (pt.getDeterminant() == 0) {
            return;
        }
        SvgElement src = childSource(r, pattern);
        if (src == null) {
            return;
        }
        double[] vb = SvgImage.parseViewBox(serverAttr(r, pattern, "viewBox", null));
        AffineTransform content = new AffineTransform();
        double cw = tw;
        double ch = th;
        if (vb != null) {
            content = SvgRenderer.viewBoxTransform(vb[0], vb[1], vb[2], vb[3], serverAttr(r, pattern, "preserveAspectRatio", null), tw, th);
            cw = vb[2];
            ch = vb[3];
        } else if (contentObb) {
            content = AffineTransform.getScaleInstance(bbox.getWidth(), bbox.getHeight());
        }
        SvgStyle ps = r.styleFor(src);
        boolean clipTile = true;
        String overflow = ps.own("overflow");
        if (overflow != null && (overflow.equalsIgnoreCase("visible") || overflow.equalsIgnoreCase("auto"))) {
            clipTile = false;
        }
        r.active.add(pattern);
        try {
            fillWithTexture(r, src, ps, shape, opacity, tx, ty, tw, th, pt, content, cw, ch, clipTile);
        } finally {
            r.active.remove(pattern);
        }
    }

    /**
     * Fills a shape with a texture made of one rendered tile (used when the pattern has too many tiles to draw one by one).
     * <p>
     * 타일 하나를 그려 만든 텍스처로 도형을 채운다(타일이 너무 많아 하나씩 그릴 수 없을 때 쓴다).
     *
     * @param r        the renderer / 렌더러
     * @param src      the element that supplies the content / 내용을 제공하는 요소
     * @param ps       the style of that element / 그 요소의 스타일
     * @param shape    the shape to fill / 채울 도형
     * @param opacity  the opacity / 투명도
     * @param tx       tile x / 타일 x
     * @param ty       tile y / 타일 y
     * @param tw       tile width / 타일 너비
     * @param th       tile height / 타일 높이
     * @param pt       the pattern transform / 패턴 변환
     * @param content  the content transform / 내용 변환
     * @param cw       the content viewport width / 내용 뷰포트 너비
     * @param ch       the content viewport height / 내용 뷰포트 높이
     * @param clipTile true to clip to the tile / 타일로 자르려면 true
     */
    private static void fillWithTexture(SvgRenderer r, SvgElement src, SvgStyle ps, Shape shape, double opacity, double tx, double ty,
            double tw, double th, AffineTransform pt, AffineTransform content, double cw, double ch, boolean clipTile) {
        AffineTransform dev = new AffineTransform(r.g.getTransform());
        dev.concatenate(pt);
        double sx = Math.sqrt(dev.getScaleX() * dev.getScaleX() + dev.getShearY() * dev.getShearY());
        double sy = Math.sqrt(dev.getShearX() * dev.getShearX() + dev.getScaleY() * dev.getScaleY());
        int iw = (int) Math.max(1, Math.min(MAX_TEXTURE, Math.ceil(tw * sx)));
        int ih = (int) Math.max(1, Math.min(MAX_TEXTURE, Math.ceil(th * sy)));
        BufferedImage tile = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB_PRE);
        SvgRenderer.Layer l = r.redirect(tile, AffineTransform.getScaleInstance(iw / tw, ih / th));
        double svW = r.vpW;
        double svH = r.vpH;
        try {
            r.g.setClip(null);
            r.vpW = cw;
            r.vpH = ch;
            if (clipTile) {
                r.g.clip(new Rectangle2D.Double(0, 0, tw, th));
            }
            r.g.transform(content);
            r.renderChildren(src, ps);
        } finally {
            r.vpW = svW;
            r.vpH = svH;
            r.endLayer(l);
        }
        java.awt.Shape oldClip = r.g.getClip();
        AffineTransform saved = r.g.getTransform();
        java.awt.Composite oldComp = r.g.getComposite();
        try {
            r.g.transform(pt);
            if (opacity < 1) {
                r.g.setComposite(AlphaComposite.SrcOver.derive((float) opacity));
            }
            r.g.setPaint(new TexturePaint(tile, new Rectangle2D.Double(tx, ty, tw, th)));
            // 텍스처는 패턴 공간에 놓이므로 칠할 도형을 패턴 공간으로 역변환해 같은 공간에서 채운다.
            Shape inPattern = pt.createInverse().createTransformedShape(shape);
            r.g.fill(inPattern);
        } catch (NoninvertibleTransformException e) {
            return;
        } finally {
            r.g.setComposite(oldComp);
            r.g.setTransform(saved);
            r.g.setClip(oldClip);
        }
    }
}
