package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Locale;

/**
 * Clip paths and masks (plan item P18-04). A clip path becomes a coverage image drawn with anti-aliasing (white shapes of the
 * <code>clipPath</code> children on a transparent image) and the element layer is multiplied with it, so clipped edges are smooth. A mask
 * becomes the luminance (or alpha) of its rendered content. Both support <code>objectBoundingBox</code> units, a transform on the clip path,
 * nested <code>clip-path</code> references and circular reference protection. Elements of the clip path other than shapes, text and
 * <code>use</code> of those are ignored, as the specification requires.
 * <p>
 * 클립 경로와 마스크(계획 P18-04). 클립 경로는 안티앨리어싱된 덮임 이미지(투명 이미지에 그린 <code>clipPath</code> 자식의 흰색 도형)가 되고 요소
 * 레이어가 그것과 곱해지므로 잘린 가장자리가 부드럽다. 마스크는 렌더링한 내용의 휘도(또는 알파)가 된다. 둘 다 <code>objectBoundingBox</code>
 * 단위, 클립 경로의 변환, 중첩된 <code>clip-path</code> 참조, 순환 참조 방지를 지원한다. 규격에 따라 클립 경로에서 도형, 문자, 그리고 그것들의
 * <code>use</code> 가 아닌 요소는 무시한다.
 */
final class SvgMasks {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgMasks() {
    }

    /**
     * Multiplies a layer with a coverage image: the layer keeps only what lies under the opaque part of the mask.
     * <p>
     * 레이어에 덮임 이미지를 곱한다. 레이어는 마스크의 불투명한 부분 아래에 있는 것만 남긴다.
     *
     * @param r     the renderer / 렌더러
     * @param layer the layer image / 레이어 이미지
     * @param mask  the coverage image of the same size / 같은 크기의 덮임 이미지
     */
    static void multiply(SvgRenderer r, BufferedImage layer, BufferedImage mask) {
        Graphics2D lg = r.createGraphics(layer);
        try {
            lg.setComposite(AlphaComposite.DstIn);
            lg.drawImage(mask, 0, 0, null);
        } finally {
            lg.dispose();
        }
    }

    /**
     * Clips a layer to a shape given in the user space of the transform <code>ctm</code>, with an anti-aliased edge.
     * <p>
     * 레이어를 변환 <code>ctm</code> 의 사용자 공간에서 주어진 도형으로 자른다. 가장자리는 안티앨리어싱한다.
     *
     * @param r     the renderer / 렌더러
     * @param layer the layer image / 레이어 이미지
     * @param s     the clip shape / 자를 도형
     * @param ctm   the transform from user space to the image / 사용자 공간에서 이미지로의 변환
     */
    static void clipToShape(SvgRenderer r, BufferedImage layer, Shape s, AffineTransform ctm) {
        BufferedImage m = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D mg = r.createGraphics(m);
        try {
            mg.setTransform(ctm);
            mg.setColor(Color.WHITE);
            mg.fill(s);
        } finally {
            mg.dispose();
        }
        multiply(r, layer, m);
    }

    /**
     * Applies a <code>clipPath</code> to a layer.
     * <p>
     * 레이어에 <code>clipPath</code> 를 적용한다.
     *
     * @param r      the renderer / 렌더러
     * @param layer  the layer image / 레이어 이미지
     * @param clip   the clipPath element / clipPath 요소
     * @param target the element being clipped (for object bounding box units) / 잘리는 요소(객체 경계 상자 단위용)
     * @param ts     the style of the target / 대상의 스타일
     * @param ctm    the transform from the user space of the target to the layer / 대상의 사용자 공간에서 레이어로의 변환
     */
    static void applyClipPath(SvgRenderer r, BufferedImage layer, SvgElement clip, SvgElement target, SvgStyle ts, AffineTransform ctm) {
        BufferedImage m = clipMask(r, clip, target, ts, ctm, 0);
        multiply(r, layer, m);
    }

    /**
     * Builds the coverage image of a clip path.
     * <p>
     * 클립 경로의 덮임 이미지를 만든다.
     *
     * @param r      the renderer / 렌더러
     * @param clip   the clipPath element / clipPath 요소
     * @param target the element being clipped / 잘리는 요소
     * @param ts     the style of the target / 대상의 스타일
     * @param ctm    the transform from the target's user space to the image / 대상의 사용자 공간에서 이미지로의 변환
     * @param depth  the nesting of clip path references / 클립 경로 참조의 중첩 깊이
     * @return the coverage image / 덮임 이미지
     */
    private static BufferedImage clipMask(SvgRenderer r, SvgElement clip, SvgElement target, SvgStyle ts, AffineTransform ctm, int depth) {
        BufferedImage m = new BufferedImage(r.canvasW, r.canvasH, BufferedImage.TYPE_INT_ARGB_PRE);
        if (depth > 8 || r.active.contains(clip)) {
            return m;
        }
        r.active.add(clip);
        SvgRenderer.Layer l = r.redirect(m, ctm);
        try {
            r.g.setClip(null);
            String units = clip.attrs.get("clipPathUnits");
            if ("objectBoundingBox".equals(units)) {
                Rectangle2D bb = SvgBounds.bbox(r, target, ts);
                if (bb == null || bb.getWidth() <= 0 || bb.getHeight() <= 0) {
                    return m;
                }
                r.g.translate(bb.getX(), bb.getY());
                r.g.scale(bb.getWidth(), bb.getHeight());
            }
            SvgStyle cst = r.styleFor(clip);
            AffineTransform ct = r.transformOf(clip, cst);
            if (ct == null) {
                return m;
            }
            r.g.transform(ct);
            // 클립 경로의 자식(도형, 문자, 그것들의 use)을 흰색으로 그려 덮임을 만든다.
            for (int i = 0; i < clip.children.size(); i++) {
                SvgElement c = clip.children.get(i);
                if (!c.isSvg()) {
                    continue;
                }
                r.tick();
                SvgStyle cs = new SvgStyle(cst, c, r.vpW, r.vpH);
                drawClipChild(r, c, cs, 0);
            }
        } finally {
            r.endLayer(l);
            r.active.remove(clip);
        }
        String outerId = r.styleFor(clip).urlProperty("clip-path");
        if (outerId != null) {
            SvgElement outer = r.doc.ids.get(outerId);
            if (outer != null && outer.is("clipPath")) {
                multiply(r, m, clipMask(r, outer, target, ts, ctm, depth + 1));
            }
        }
        return m;
    }

    /**
     * Draws one child of a clip path (a shape, text, or a use that refers to one of them) as white coverage.
     * <p>
     * 클립 경로의 자식 하나(도형, 문자, 또는 그것들을 가리키는 use)를 흰색 덮임으로 그린다.
     *
     * @param r     the renderer (its graphics is the coverage image) / 렌더러(그래픽스가 덮임 이미지이다)
     * @param c     the child / 자식
     * @param cs    its style / 그 스타일
     * @param depth the use nesting / use 중첩 깊이
     */
    private static void drawClipChild(SvgRenderer r, SvgElement c, SvgStyle cs, int depth) {
        String n = c.name;
        boolean shape = "rect".equals(n) || "circle".equals(n) || "ellipse".equals(n) || "line".equals(n) || "polyline".equals(n)
                || "polygon".equals(n) || "path".equals(n);
        boolean text = "text".equals(n);
        boolean use = "use".equals(n);
        if (!(shape || text || use) || !cs.displayed() || !cs.visible() || !r.conditionsPass(c)) {
            return;
        }
        AffineTransform saved = r.g.getTransform();
        try {
            AffineTransform t = r.transformOf(c, cs);
            if (t == null) {
                return;
            }
            r.g.transform(t);
            Shape s;
            if (use) {
                SvgElement target = r.lookup(c.href());
                if (target == null || depth > 8 || r.active.contains(target)) {
                    return;
                }
                double x = SvgRenderer.lengthAttr(c, "x", cs, 0, 0);
                double y = SvgRenderer.lengthAttr(c, "y", cs, 0, 1);
                r.g.translate(x, y);
                r.active.add(target);
                try {
                    drawClipChild(r, target, new SvgStyle(cs, target, r.vpW, r.vpH), depth + 1);
                } finally {
                    r.active.remove(target);
                }
                return;
            } else if (text) {
                s = SvgText.clipShape(r, c, cs);
            } else if ("path".equals(n)) {
                s = r.pathOf(c, cs).shape;
            } else {
                s = r.basicShape(c, cs);
            }
            if (s == null) {
                return;
            }
            if (s instanceof Path2D) {
                Path2D.Double copy = new Path2D.Double(s);
                copy.setWindingRule("evenodd".equals(cs.keyword("clip-rule")) ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
                s = copy;
            }
            String ownClip = cs.urlProperty("clip-path");
            SvgElement clipEl = ownClip == null ? null : r.doc.ids.get(ownClip);
            if (clipEl != null && clipEl.is("clipPath")) {
                // 자식에 자기 clip-path 가 있으면 임시 레이어에서 잘라 합친다.
                SvgRenderer.Layer l = r.pushLayer();
                boolean ok = false;
                try {
                    r.g.setColor(Color.WHITE);
                    r.g.fill(s);
                    applyClipPath(r, l.img, clipEl, c, cs, l.ctm);
                    ok = true;
                } finally {
                    if (ok) {
                        r.compositeLayer(l, 1, null);
                    } else {
                        r.endLayer(l);
                    }
                }
            } else {
                r.g.setColor(Color.WHITE);
                r.g.fill(s);
            }
        } finally {
            r.g.setTransform(saved);
        }
    }

    /**
     * Applies a <code>mask</code> element to a layer.
     * <p>
     * 레이어에 <code>mask</code> 요소를 적용한다.
     *
     * @param r      the renderer / 렌더러
     * @param layer  the layer image / 레이어 이미지
     * @param mask   the mask element / mask 요소
     * @param target the masked element (for object bounding box units) / 마스크가 걸리는 요소(객체 경계 상자 단위용)
     * @param ts     the style of the target / 대상의 스타일
     * @param ctm    the transform from the target's user space to the layer / 대상의 사용자 공간에서 레이어로의 변환
     */
    static void applyMask(SvgRenderer r, BufferedImage layer, SvgElement mask, SvgElement target, SvgStyle ts, AffineTransform ctm) {
        if (r.active.contains(mask)) {
            clearLayer(layer);
            return;
        }
        boolean userSpaceUnits = "userSpaceOnUse".equals(mask.attrs.get("maskUnits"));
        boolean contentObb = "objectBoundingBox".equals(mask.attrs.get("maskContentUnits"));
        Rectangle2D bb = null;
        if (!userSpaceUnits || contentObb) {
            bb = SvgBounds.bbox(r, target, ts);
            if (bb == null || bb.getWidth() <= 0 || bb.getHeight() <= 0) {
                clearLayer(layer);
                return;
            }
        }
        Rectangle2D region = maskRegion(r, mask, ts, userSpaceUnits, bb);
        if (region == null) {
            clearLayer(layer);
            return;
        }
        BufferedImage mimg = new BufferedImage(r.canvasW, r.canvasH, BufferedImage.TYPE_INT_ARGB_PRE);
        r.active.add(mask);
        SvgRenderer.Layer l = r.redirect(mimg, ctm);
        try {
            r.g.setClip(null);
            if (contentObb) {
                r.g.translate(bb.getX(), bb.getY());
                r.g.scale(bb.getWidth(), bb.getHeight());
            }
            r.renderChildren(mask, r.styleFor(mask));
        } finally {
            r.endLayer(l);
            r.active.remove(mask);
        }
        clipToShape(r, mimg, region, ctm);
        String type = mask.declared == null ? null : mask.declared.get("mask-type");
        if (type == null) {
            type = mask.attrs.get("mask-type");
        }
        boolean alphaOnly = type != null && "alpha".equals(type.trim().toLowerCase(Locale.ENGLISH));
        multiplyByMask(layer, mimg, alphaOnly);
    }

    /**
     * Computes the mask region rectangle in user space.
     * <p>
     * 마스크 영역 사각형을 사용자 공간에서 계산한다.
     *
     * @param r        the renderer / 렌더러
     * @param mask     the mask element / mask 요소
     * @param ts       the style of the target / 대상의 스타일
     * @param userUnit true for <code>userSpaceOnUse</code> / <code>userSpaceOnUse</code> 이면 true
     * @param bb       the bounding box of the target (for object bounding box units) / 대상의 경계 상자(객체 경계 상자 단위용)
     * @return the region or null if it is empty / 영역. 비었으면 null
     */
    private static Rectangle2D maskRegion(SvgRenderer r, SvgElement mask, SvgStyle ts, boolean userUnit, Rectangle2D bb) {
        double x;
        double y;
        double w;
        double h;
        if (userUnit) {
            SvgStyle ms = r.styleFor(mask);
            x = SvgRenderer.lengthAttr(mask, "x", ms, -0.1 * r.vpW, 0);
            y = SvgRenderer.lengthAttr(mask, "y", ms, -0.1 * r.vpH, 1);
            w = SvgRenderer.lengthAttr(mask, "width", ms, 1.2 * r.vpW, 0);
            h = SvgRenderer.lengthAttr(mask, "height", ms, 1.2 * r.vpH, 1);
        } else {
            double fx = fraction(mask.attrs.get("x"), -0.1);
            double fy = fraction(mask.attrs.get("y"), -0.1);
            double fw = fraction(mask.attrs.get("width"), 1.2);
            double fh = fraction(mask.attrs.get("height"), 1.2);
            x = bb.getX() + fx * bb.getWidth();
            y = bb.getY() + fy * bb.getHeight();
            w = fw * bb.getWidth();
            h = fh * bb.getHeight();
        }
        if (!(w > 0) || !(h > 0)) {
            return null;
        }
        return new Rectangle2D.Double(x, y, w, h);
    }

    /**
     * Reads a fraction attribute of object bounding box units (a number or a percentage).
     * <p>
     * 객체 경계 상자 단위의 비율 속성을 읽는다(수 또는 백분율).
     *
     * @param v   the attribute text or null / 속성 텍스트 또는 null
     * @param def the default / 기본값
     * @return the fraction / 비율
     */
    static double fraction(String v, double def) {
        if (v == null) {
            return def;
        }
        SvgLength l = SvgLength.parse(v);
        if (l == null) {
            return def;
        }
        return l.isPercent() ? l.value / 100 : l.value;
    }

    /**
     * Removes everything from a layer.
     * <p>
     * 레이어의 모든 내용을 지운다.
     *
     * @param layer the layer image / 레이어 이미지
     */
    private static void clearLayer(BufferedImage layer) {
        int[] px = ((DataBufferInt) layer.getRaster().getDataBuffer()).getData();
        java.util.Arrays.fill(px, 0);
    }

    /**
     * Multiplies the layer pixels with the luminance (or alpha) of the mask image.
     * <p>
     * 레이어 픽셀에 마스크 이미지의 휘도(또는 알파)를 곱한다.
     *
     * @param layer     the layer image (premultiplied) / 레이어 이미지(알파 미리 곱함)
     * @param mimg      the rendered mask content (premultiplied) / 렌더링한 마스크 내용(알파 미리 곱함)
     * @param alphaOnly true to use only the alpha channel / 알파 채널만 쓰려면 true
     */
    private static void multiplyByMask(BufferedImage layer, BufferedImage mimg, boolean alphaOnly) {
        int[] lp = ((DataBufferInt) layer.getRaster().getDataBuffer()).getData();
        int[] mp = ((DataBufferInt) mimg.getRaster().getDataBuffer()).getData();
        int n = Math.min(lp.length, mp.length);
        // 모든 픽셀에 마스크 값(0..255)을 곱한다. 미리 곱한 색이므로 휘도는 알파가 이미 반영되어 있다.
        for (int i = 0; i < n; i++) {
            int p = lp[i];
            if (p == 0) {
                continue;
            }
            int q = mp[i];
            int m;
            if (alphaOnly) {
                m = q >>> 24;
            } else {
                int rr = (q >> 16) & 0xFF;
                int gg = (q >> 8) & 0xFF;
                int bb = q & 0xFF;
                m = (int) Math.round(0.2125 * rr + 0.7154 * gg + 0.0721 * bb);
                if (m > 255) {
                    m = 255;
                }
            }
            if (m == 255) {
                continue;
            }
            if (m == 0) {
                lp[i] = 0;
                continue;
            }
            int a = (((p >>> 24) * m) + 127) / 255;
            int rr = ((((p >> 16) & 0xFF) * m) + 127) / 255;
            int gg = ((((p >> 8) & 0xFF) * m) + 127) / 255;
            int bb = (((p & 0xFF) * m) + 127) / 255;
            lp[i] = (a << 24) | (rr << 16) | (gg << 8) | bb;
        }
    }
}
