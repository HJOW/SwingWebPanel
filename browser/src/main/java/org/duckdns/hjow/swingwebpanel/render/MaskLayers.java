package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.media.image.svg.SvgReference;

/**
 * The mask of a box (plan item P10-08, CSS Masking Module Level 1, https://www.w3.org/TR/css-masking-1/, Candidate Recommendation Draft
 * 2021-08-05, retrieved 2026-11-17, section 7 "Masking"): the layers of <code>mask-image</code> with their <code>mask-size</code>,
 * <code>mask-position</code>, <code>mask-repeat</code>, <code>mask-origin</code>, <code>mask-clip</code>, <code>mask-mode</code> and
 * <code>mask-composite</code>. The tiles of a layer are painted by the same code as the background layers
 * ({@link BackgroundLayers#build(java.util.List, ComputedStyle, RoundedBox, double[], double[], boolean, org.duckdns.hjow.swingwebpanel.dom.Node, int[], java.util.List)}
 * with the mask property ids). Every layer is turned into mask values (the alpha channel, or for <code>luminance</code> the luminance
 * <code>0.2125 R + 0.7154 G + 0.0721 B</code> of the color that is not premultiplied times the alpha, section 7.10.1), the layers are
 * composited from the bottom up with the Porter-Duff operator of the layer over the layers below it (<code>add</code> is source over,
 * <code>subtract</code> source out, <code>intersect</code> source in, <code>exclude</code> xor; the operator of the bottom layer is
 * ignored), and the result is multiplied into the alpha of the painted content. A layer that is <code>none</code>, whose image is not
 * loaded or cannot be shown is a transparent black layer, and everything outside of the tiles of the layers has the mask value 0, so
 * a box with a mask whose image has not arrived yet is not painted. <code>match-source</code> is the alpha mode (a CSS image is not a
 * mask element). Not supported: <code>mask-border</code> and mask references to SVG <code>mask</code> elements; <code>margin-box</code> counts as
 * <code>border-box</code> for <code>mask-clip</code> and <code>mask-origin</code>.
 * <p>
 * 상자의 마스크(계획 P10-08, CSS Masking Module Level 1, https://www.w3.org/TR/css-masking-1/, Candidate Recommendation Draft 2021-08-05, 조회 2026-11-17, 7 절 "Masking"):
 * <code>mask-image</code> 의 레이어와 각 레이어의 <code>mask-size</code>, <code>mask-position</code>, <code>mask-repeat</code>, <code>mask-origin</code>, <code>mask-clip</code>,
 * <code>mask-mode</code>, <code>mask-composite</code>. 레이어의 타일은 배경 레이어와 같은 코드가 그린다(마스크 속성 id 를 준
 * {@link BackgroundLayers#build(java.util.List, ComputedStyle, RoundedBox, double[], double[], boolean, org.duckdns.hjow.swingwebpanel.dom.Node, int[], java.util.List)}).
 * 레이어마다 마스크 값으로 바꾸고(알파 채널, <code>luminance</code> 이면 미리 곱하지 않은 색의 휘도 <code>0.2125 R + 0.7154 G + 0.0721 B</code> 에 알파를 곱한 값, 7.10.1 절)
 * 아래 레이어부터 그 레이어의 Porter-Duff 연산자로 아래 레이어들 위에 합성하며(<code>add</code> 는 source over, <code>subtract</code> 는 source out, <code>intersect</code> 는 source in,
 * <code>exclude</code> 는 xor. 맨 아래 레이어의 연산자는 무시한다) 결과를 그려진 내용의 알파에 곱한다. <code>none</code> 이거나 이미지를 아직 읽지 못했거나 보여 줄 수 없는
 * 레이어는 투명한 검정 레이어이고, 레이어의 타일 밖은 모두 마스크 값 0 이므로 이미지가 아직 도착하지 않은 마스크를 가진 상자는 그려지지 않는다. <code>match-source</code> 는 알파
 * 모드이다(CSS 이미지는 마스크 요소가 아니다). 지원하지 않음: <code>mask-border</code>, SVG <code>mask</code> 요소를 가리키는 마스크 참조. <code>mask-clip</code> 과
 * <code>mask-origin</code> 에서 <code>margin-box</code> 는 <code>border-box</code> 로 본다.
 */
final class MaskLayers {

    /** Operator: add (source over). 연산자: add(source over). */
    static final int ADD = 0;
    /** Operator: subtract (source out). 연산자: subtract(source out). */
    static final int SUBTRACT = 1;
    /** Operator: intersect (source in). 연산자: intersect(source in). */
    static final int INTERSECT = 2;
    /** Operator: exclude (xor). 연산자: exclude(xor). */
    static final int EXCLUDE = 3;

    /** The tile commands of the layers, the top layer first; null for a transparent black layer. 레이어의 타일 명령(맨 위 레이어가 먼저). 투명한 검정 레이어는 null. */
    private final DisplayCommand[] layers;

    /** True where the layer is a luminance mask. 레이어가 휘도 마스크이면 true. */
    private final boolean[] luminance;

    /** The compositing operators of the layers. 레이어의 합성 연산자. */
    private final int[] ops;

    /** The SVG definitions of the layers that are <code>url(#id)</code> references to <code>mask</code> elements (null for the other layers), or null if there are none. <code>mask</code> 요소를 가리키는 <code>url(#id)</code> 참조인 레이어의 SVG 정의(다른 레이어는 null). 하나도 없으면 null. */
    private final SvgReference[] references;

    /** The ids of the referenced <code>mask</code> elements. 참조한 <code>mask</code> 요소의 id. */
    private final String[] referenceIds;

    /** The border box of the element, the user space of the references (null without references). 요소의 border box. 참조의 사용자 공간(참조가 없으면 null). */
    private final Rectangle2D box;

    /**
     * Creates a mask from its layers.
     * <p>
     * 레이어로 마스크를 만든다.
     *
     * @param layers    the tile commands, the top layer first / 타일 명령(맨 위 레이어가 먼저)
     * @param luminance true where a layer is a luminance mask / 레이어가 휘도 마스크이면 true
     * @param ops       the compositing operators / 합성 연산자
     */
    MaskLayers(DisplayCommand[] layers, boolean[] luminance, int[] ops) {
        this(layers, luminance, ops, null, null, null);
    }

    /**
     * Creates a mask from its layers, some of which may refer to SVG <code>mask</code> elements.
     * <p>
     * 레이어로 마스크를 만든다. 레이어 중 일부는 SVG <code>mask</code> 요소를 가리킬 수 있다.
     *
     * @param layers       the tile commands, the top layer first / 타일 명령(맨 위 레이어가 먼저)
     * @param luminance    true where a layer is a luminance mask / 레이어가 휘도 마스크이면 true
     * @param ops          the compositing operators / 합성 연산자
     * @param references   the definitions of the reference layers (null for the others), or null / 참조 레이어의 정의(다른 레이어는 null). 없으면 null
     * @param referenceIds the ids of the referenced elements, or null / 참조한 요소의 id. 없으면 null
     * @param box          the border box of the element, or null / 요소의 border box. 없으면 null
     */
    MaskLayers(DisplayCommand[] layers, boolean[] luminance, int[] ops, SvgReference[] references, String[] referenceIds, Rectangle2D box) {
        this.layers = layers.clone();
        this.luminance = luminance.clone();
        this.ops = ops.clone();
        this.references = references == null ? null : references.clone();
        this.referenceIds = referenceIds == null ? null : referenceIds.clone();
        this.box = box;
    }

    /**
     * Builds the mask of a box.
     * <p>
     * 상자의 마스크를 만든다.
     *
     * @param box     the box / 상자
     * @param builder the builder of the layer tiles / 레이어 타일을 만드는 빌더
     * @return the mask, or null if the box has none / 마스크. 없으면 null
     */
    static MaskLayers of(LayoutBox box, BackgroundLayers builder) {
        return of(box, builder, null);
    }

    /**
     * Builds the mask of a box; a layer whose image is <code>url(#id)</code> refers to an SVG <code>mask</code> element of the document (CSS Masking 1
     * section 7.1, a mask reference), which gives the mask values of the layer; a reference to an element that does not exist (or is no
     * <code>mask</code> element) is a transparent black layer.
     * <p>
     * 상자의 마스크를 만든다. 이미지가 <code>url(#id)</code> 인 레이어는 문서의 SVG <code>mask</code> 요소를 가리키며(CSS Masking 1 7.1 절, 마스크 참조) 그 요소가 레이어의
     * 마스크 값을 준다. 없는 요소(또는 <code>mask</code> 요소가 아닌 것)의 참조는 투명한 검정 레이어이다.
     *
     * @param box     the box / 상자
     * @param builder the builder of the layer tiles / 레이어 타일을 만드는 빌더
     * @param refs    the finder of the SVG elements of the document, or null / 문서의 SVG 요소를 찾는 쪽. 없으면 null
     * @return the mask, or null if the box has none / 마스크. 없으면 null
     */
    static MaskLayers of(LayoutBox box, BackgroundLayers builder, SvgReferences refs) {
        ComputedStyle s = box.getStyle();
        if (s == null || !org.duckdns.hjow.swingwebpanel.layout.Effects.hasMask(s)) {
            return null;
        }
        // mask-border-source 가 쓸 수 없는 원본(아직 도착하지 않았거나 읽을 수 없음)이고 mask-image 도 없으면 마스크를 쓰지 않는다(none 처럼).
        BorderImageLayer border = BorderImageLayer.of(box, s, builder, BorderImageLayer.MASK);
        if (border == null && !usesMaskImage(s)) {
            return null;
        }
        int count = BackgroundLayers.layers(s.get(Prop.MASK_IMAGE)).size();
        RoundedBox outer = RoundedBox.ofBorderBox(s, box.getX(), box.getY(), box.getWidth(), box.getHeight());
        double[] borders = { box.getBorderTop(), box.getBorderRight(), box.getBorderBottom(), box.getBorderLeft() };
        double[] paddings = { box.getPaddingTop(), box.getPaddingRight(), box.getPaddingBottom(), box.getPaddingLeft() };
        List<DisplayCommand> cmds = new ArrayList<DisplayCommand>();
        List<Integer> idx = new ArrayList<Integer>();
        builder.build(cmds, s, outer, borders, paddings, false, box.getNode(), BackgroundLayers.MASK, idx);
        DisplayCommand[] layers = new DisplayCommand[count];
        // 만든 타일 명령을 레이어 번호 자리에 넣는다(없는 번호는 투명한 검정 레이어이다).
        for (int j = 0; j < cmds.size(); j++) {
            layers[idx.get(j).intValue()] = cmds.get(j);
        }
        boolean[] lum = new boolean[count];
        int[] ops = new int[count];
        SvgReference[] refLayers = new SvgReference[count];
        String[] refIds = new String[count];
        boolean anyReference = false;
        List<CssValue> images = BackgroundLayers.layers(s.get(Prop.MASK_IMAGE));
        // 레이어마다 마스크 모드와 합성 연산자를 읽는다(목록은 되풀이한다). 같은 문서의 mask 요소를 가리키는 레이어는 그 요소를 찾는다.
        for (int i = 0; i < count; i++) {
            lum[i] = "luminance".equals(keyword(BackgroundLayers.item(s, Prop.MASK_MODE, i)));
            String op = keyword(BackgroundLayers.item(s, Prop.MASK_COMPOSITE, i));
            ops[i] = "subtract".equals(op) ? SUBTRACT : ("intersect".equals(op) ? INTERSECT : ("exclude".equals(op) ? EXCLUDE : ADD));
            String id = BackgroundLayers.fragmentReference(images.get(i));
            if (id != null && refs != null) {
                refIds[i] = id;
                refLayers[i] = refs.mask(id);
                anyReference |= refLayers[i] != null;
            }
        }
        Rectangle2D border2d = anyReference ? new Rectangle2D.Double(box.getX(), box.getY(), box.getWidth(), box.getHeight()) : null;
        // mask-border 는 mask-image 의 레이어 위에 맨 위 레이어로 더한다(CSS Masking 1: mask-border).
        if (border != null) {
            DisplayCommand[] more = new DisplayCommand[count + 1];
            boolean[] moreLum = new boolean[count + 1];
            int[] moreOps = new int[count + 1];
            SvgReference[] moreRefs = new SvgReference[count + 1];
            String[] moreIds = new String[count + 1];
            more[0] = border;
            moreLum[0] = "luminance".equals(keyword(s.get(Prop.MASK_BORDER_MODE)));
            moreOps[0] = ADD;
            System.arraycopy(layers, 0, more, 1, count);
            System.arraycopy(lum, 0, moreLum, 1, count);
            System.arraycopy(ops, 0, moreOps, 1, count);
            System.arraycopy(refLayers, 0, moreRefs, 1, count);
            System.arraycopy(refIds, 0, moreIds, 1, count);
            return new MaskLayers(more, moreLum, moreOps, anyReference ? moreRefs : null, anyReference ? moreIds : null, border2d);
        }
        return new MaskLayers(layers, lum, ops, anyReference ? refLayers : null, anyReference ? refIds : null, border2d);
    }

    /**
     * Tells whether <code>mask-image</code> has a layer that is not <code>none</code>.
     * <p>
     * <code>mask-image</code> 에 <code>none</code> 이 아닌 레이어가 있는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true if a mask image is used / 마스크 이미지를 쓰면 true
     */
    private static boolean usesMaskImage(ComputedStyle s) {
        List<CssValue> layers = BackgroundLayers.layers(s.get(Prop.MASK_IMAGE));
        // 레이어 중 none 이 아닌 것이 있는지 본다.
        for (int i = 0; i < layers.size(); i++) {
            if (!layers.get(i).isKeyword("none")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the keyword of a value.
     * <p>
     * 값의 키워드를 반환한다.
     *
     * @param v the value / 값
     * @return the keyword or "" / 키워드. 없으면 ""
     */
    private static String keyword(CssValue v) {
        return v instanceof CssKeyword ? ((CssKeyword) v).getName() : "";
    }

    /**
     * Multiplies the mask values into the alpha channel of a buffer.
     * <p>
     * 마스크 값을 버퍼의 알파 채널에 곱한다.
     *
     * @param buf      the buffer (ARGB, not premultiplied) / 버퍼(ARGB, 미리 곱하지 않음)
     * @param toBuffer the transform from document coordinates to the pixels of the buffer / 문서 좌표에서 버퍼 픽셀로의 변환
     * @param ctx      the paint context (for the animation time and the scroll offset) / 그리기 컨텍스트(애니메이션 시각과 스크롤 오프셋용)
     */
    void apply(BufferedImage buf, AffineTransform toBuffer, PaintContext ctx) {
        int w = buf.getWidth();
        int h = buf.getHeight();
        int n = w * h;
        Rectangle2D visible;
        try {
            visible = toBuffer.createInverse().createTransformedShape(new Rectangle(0, 0, w, h)).getBounds2D();
        } catch (NoninvertibleTransformException e) {
            return;
        }
        float[] dest = null;
        // 아래 레이어(목록의 끝)부터 위로 합성한다.
        for (int i = layers.length - 1; i >= 0; i--) {
            float[] src = new float[n];
            if (references != null && references[i] != null) {
                // mask 요소를 가리키는 레이어는 요소의 border box 를 사용자 공간으로 하여 마스크 값을 얻는다.
                AffineTransform ctm = new AffineTransform(toBuffer);
                ctm.translate(box.getX(), box.getY());
                float[] cover = references[i].maskCoverage(referenceIds[i], w, h, ctm, box.getWidth(), box.getHeight());
                if (cover != null) {
                    src = cover;
                }
            } else if (layers[i] != null && layers[i].intersects(visible)) {
                BufferedImage lb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = lb.createGraphics();
                try {
                    g.setTransform(toBuffer);
                    layers[i].paint(new PaintContext(g, visible, ctx.timeMillis, ctx.scrollX, ctx.scrollY));
                } finally {
                    g.dispose();
                }
                int[] px = lb.getRGB(0, 0, w, h, null, 0, w);
                // 픽셀마다 알파나 휘도와 알파의 곱을 마스크 값으로 쓴다.
                for (int k = 0; k < n; k++) {
                    int p = px[k];
                    int a = p >>> 24;
                    if (a == 0) {
                        continue;
                    }
                    if (luminance[i]) {
                        float l = (0.2125f * ((p >> 16) & 0xFF) + 0.7154f * ((p >> 8) & 0xFF) + 0.0721f * (p & 0xFF)) / 255f;
                        src[k] = Math.min(1f, l) * (a / 255f);
                    } else {
                        src[k] = a / 255f;
                    }
                }
            }
            if (dest == null) {
                dest = src;
            } else {
                combine(dest, src, ops[i]);
            }
        }
        int[] px = buf.getRGB(0, 0, w, h, null, 0, w);
        // 마스크 값을 내용의 알파에 곱한다.
        for (int k = 0; k < n; k++) {
            int a = px[k] >>> 24;
            if (a == 0) {
                continue;
            }
            float m = dest == null ? 0f : dest[k];
            int na = Math.max(0, Math.min(255, (int) (a * m + 0.5f)));
            px[k] = (na << 24) | (px[k] & 0xFFFFFF);
        }
        buf.setRGB(0, 0, w, h, px, 0, w);
    }

    /**
     * Composites a layer (the source) over the layers below it (the destination) with a Porter-Duff operator, in place in the
     * destination.
     * <p>
     * 레이어(원본)를 그 아래 레이어들(대상) 위에 Porter-Duff 연산자로 합성한다(대상에 제자리로).
     *
     * @param dest the layers below, receives the result / 아래 레이어들. 결과를 받는다
     * @param src  the layer / 레이어
     * @param op   the operator / 연산자
     */
    static void combine(float[] dest, float[] src, int op) {
        // 픽셀마다 연산자의 식으로 새 마스크 값을 구한다.
        for (int k = 0; k < dest.length; k++) {
            float s = src[k];
            float d = dest[k];
            switch (op) {
            case SUBTRACT:
                dest[k] = s * (1 - d);
                break;
            case INTERSECT:
                dest[k] = s * d;
                break;
            case EXCLUDE:
                dest[k] = s * (1 - d) + d * (1 - s);
                break;
            default:
                dest[k] = s + d * (1 - s);
                break;
            }
        }
    }
}
