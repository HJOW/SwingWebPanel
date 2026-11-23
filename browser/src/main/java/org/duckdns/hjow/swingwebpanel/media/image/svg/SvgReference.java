package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The SVG definitions that an HTML element refers to with <code>filter: url(#id)</code>, <code>clip-path: url(#id)</code> or
 * <code>mask-image: url(#id)</code> (CSS Filter Effects Level 1 section 5, https://www.w3.org/TR/filter-effects-1/#FilterProperty, and CSS Masking Level 1
 * sections 5 and 7, https://www.w3.org/TR/css-masking-1/): the <code>svg</code> element that contains the definitions, parsed once, and the operations that
 * apply a <code>filter</code>, a <code>clipPath</code> or a <code>mask</code> element of it to the pixels of an HTML element. The same engine as for SVG
 * images does the work (every filter primitive, nested clip paths and masks, <code>use</code>, units), so the result is the one that an SVG element with the
 * same attribute would have. The HTML element plays the part of the filtered element: its border box is the bounding box (<code>objectBoundingBox</code>
 * units) and its top left corner is the origin of the user space (<code>userSpaceOnUse</code> units); the lengths of percentages are relative to the box.
 * Only definitions in the same document are used: nothing is loaded and no script runs. The methods change the helper element of the document and are
 * therefore synchronized.
 * <p>
 * HTML 요소가 <code>filter: url(#id)</code>, <code>clip-path: url(#id)</code>, <code>mask-image: url(#id)</code> 로 가리키는 SVG 정의(CSS Filter Effects Level 1 5 절,
 * https://www.w3.org/TR/filter-effects-1/#FilterProperty, CSS Masking Level 1 5, 7 절, https://www.w3.org/TR/css-masking-1/): 정의가 든 <code>svg</code> 요소를 한 번
 * 해석해 두고, 그 안의 <code>filter</code>, <code>clipPath</code>, <code>mask</code> 요소를 HTML 요소의 픽셀에 적용하는 연산을 제공한다. SVG 이미지와 같은 엔진이 일하므로(모든
 * 필터 기본 요소, 중첩된 클립 경로와 마스크, <code>use</code>, 단위) 같은 속성을 가진 SVG 요소와 같은 결과이다. HTML 요소가 필터가 걸리는 요소 역할을 한다: border box 가
 * 경계 상자(<code>objectBoundingBox</code> 단위)이고 왼쪽 위 모서리가 사용자 공간의 원점(<code>userSpaceOnUse</code> 단위)이며 백분율 길이는 상자 기준이다. 같은 문서의 정의만
 * 쓴다: 아무것도 가져오지 않고 스크립트도 실행하지 않는다. 메서드는 문서의 보조 요소를 바꾸므로 동기화되어 있다.
 */
public final class SvgReference {

    /** The id of the helper element that stands for the HTML element. HTML 요소를 대신하는 보조 요소의 id. */
    private static final String TARGET_ID = "swp-target";

    /** The parsed document. 해석한 문서. */
    private final SvgImage doc;

    /** The rectangle that stands for the border box of the HTML element (its attributes are set for every operation). HTML 요소의 border box 를 대신하는 사각형(연산마다 속성을 정한다). */
    private final SvgElement target;

    /**
     * Creates the reference from a parsed document.
     * <p>
     * 해석한 문서로 참조를 만든다.
     *
     * @param doc the document / 문서
     */
    private SvgReference(SvgImage doc) {
        this.doc = doc;
        this.target = new SvgElement("rect", SvgXmlParser.SVG_NS);
        this.target.parent = doc.root;
        this.target.attrs.put("id", TARGET_ID);
    }

    /**
     * Parses an <code>svg</code> element that contains the definitions.
     * <p>
     * 정의가 든 <code>svg</code> 요소를 해석한다.
     *
     * @param xml the element as an XML document in UTF-8 / UTF-8 XML 문서로 나타낸 요소
     * @return the reference / 참조
     * @throws MediaException if the document is not well formed or too large / 문서가 올바르지 않거나 너무 크면
     */
    public static SvgReference parse(byte[] xml) throws MediaException {
        return new SvgReference(SvgImage.parseDefinitions(xml, DecodeLimits.DEFAULT));
    }

    /**
     * Tells whether the document has an element of a kind with an id.
     * <p>
     * 문서에 id 가 있는 어떤 종류의 요소가 있는지 알려 준다.
     *
     * @param id   the id / id
     * @param kind <code>filter</code>, <code>clipPath</code> or <code>mask</code> / <code>filter</code>, <code>clipPath</code>, <code>mask</code>
     * @return true if there is such an element / 있으면 true
     */
    public boolean has(String id, String kind) {
        SvgElement e = doc.ids.get(id);
        return e != null && e.is(kind);
    }

    /**
     * Creates a renderer that draws into a layer with the user space of the HTML element, and sets the helper element to the border box.
     * <p>
     * HTML 요소의 사용자 공간으로 레이어에 그리는 렌더러를 만들고 보조 요소를 border box 로 맞춘다.
     *
     * @param layer the layer (premultiplied ARGB) / 레이어(알파 미리 곱한 ARGB)
     * @param ctm   the transform from the user space of the element to the layer / 요소의 사용자 공간에서 레이어로의 변환
     * @param w     the width of the border box / border box 너비
     * @param h     the height of the border box / border box 높이
     * @return the renderer / 렌더러
     */
    private SvgRenderer renderer(BufferedImage layer, AffineTransform ctm, double w, double h) {
        SvgRenderer r = new SvgRenderer(doc, layer);
        r.g = r.createGraphics(layer);
        r.g.setTransform(ctm);
        r.curImg = layer;
        r.vpW = w;
        r.vpH = h;
        target.attrs.put("x", "0");
        target.attrs.put("y", "0");
        target.attrs.put("width", String.valueOf(w));
        target.attrs.put("height", String.valueOf(h));
        return r;
    }

    /**
     * Returns the style of the helper element.
     * <p>
     * 보조 요소의 스타일을 반환한다.
     *
     * @param r the renderer / 렌더러
     * @return the style / 스타일
     */
    private SvgStyle targetStyle(SvgRenderer r) {
        return new SvgStyle(new SvgStyle(r.vpW, r.vpH), target, r.vpW, r.vpH);
    }

    /**
     * Returns how far the filter region of a <code>filter</code> element reaches beyond the border box of the element it filters.
     * <p>
     * <code>filter</code> 요소의 필터 영역이 필터가 걸리는 요소의 border box 밖으로 얼마나 뻗는지 반환한다.
     *
     * @param id the id of the filter element / 필터 요소의 id
     * @param w  the width of the border box / border box 너비
     * @param h  the height of the border box / border box 높이
     * @return {left, top, right, bottom} in user space units (not negative) / 사용자 공간 단위의 {왼쪽, 위쪽, 오른쪽, 아래쪽}(음수가 아님)
     */
    public synchronized double[] filterReach(String id, double w, double h) {
        SvgElement filter = doc.ids.get(id);
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        SvgRenderer r = renderer(dummy, new AffineTransform(), w, h);
        try {
            if (filter == null || !filter.is("filter")) {
                return new double[4];
            }
            boolean userUnits = "userSpaceOnUse".equals(SvgPaintServers.serverAttr(r, filter, "filterUnits", null));
            Rectangle2D region = SvgFilters.userRegion(r, filter, r.styleFor(filter), userUnits, new Rectangle2D.Double(0, 0, w, h));
            if (region == null) {
                return new double[4];
            }
            return new double[] { Math.max(0, -region.getMinX()), Math.max(0, -region.getMinY()), Math.max(0, region.getMaxX() - w), Math.max(0, region.getMaxY() - h) };
        } finally {
            r.g.dispose();
        }
    }

    /**
     * Applies a <code>filter</code> element to the pixels of an HTML element: the result replaces the pixels and everything outside of the filter region is
     * removed. The pixels of a layer that is not wide enough for the filter region are cut off at the edge of the layer.
     * <p>
     * HTML 요소의 픽셀에 <code>filter</code> 요소를 적용한다: 결과가 픽셀을 대체하고 필터 영역 밖은 모두 지워진다. 필터 영역에 비해 좁은 레이어의 픽셀은 레이어
     * 가장자리에서 잘린다.
     *
     * @param id    the id of the filter element / 필터 요소의 id
     * @param layer the pixels (ARGB, not premultiplied), changed in place / 픽셀(알파를 미리 곱하지 않은 ARGB). 제자리에서 바뀐다
     * @param ctm   the transform from the user space of the element (the origin is the top left corner of its border box) to the layer / 요소의 사용자 공간(원점은
     *              border box 의 왼쪽 위 모서리)에서 레이어로의 변환
     * @param w     the width of the border box / border box 너비
     * @param h     the height of the border box / border box 높이
     * @return true if the filter was applied, false if it could not be (a filter that is not available, or the drawing stopped because of the limits) / 필터를
     *         적용했으면 true. 쓸 수 없는 필터이거나 제한 때문에 그리기가 멈추면 false
     */
    public synchronized boolean applyFilter(String id, BufferedImage layer, AffineTransform ctm, double w, double h) {
        if (!has(id, "filter")) {
            return false;
        }
        BufferedImage pre = premultiplied(layer);
        SvgRenderer r = renderer(pre, ctm, w, h);
        try {
            SvgStyle ts = targetStyle(r);
            List<Object> filters = SvgFilters.parseFilterList(r, "url(#" + id + ")", ts);
            if (filters == null || filters.isEmpty()) {
                return false;
            }
            SvgFilters.apply(r, pre, filters, target, ts);
        } catch (SvgRenderer.Abort a) {
            return false;
        } finally {
            r.g.dispose();
        }
        copyBack(pre, layer);
        return true;
    }

    /**
     * Clips the pixels of an HTML element with a <code>clipPath</code> element.
     * <p>
     * HTML 요소의 픽셀을 <code>clipPath</code> 요소로 자른다.
     *
     * @param id    the id of the clipPath element / clipPath 요소의 id
     * @param layer the pixels (ARGB, not premultiplied), changed in place / 픽셀(알파를 미리 곱하지 않은 ARGB). 제자리에서 바뀐다
     * @param ctm   the transform from the user space of the element to the layer / 요소의 사용자 공간에서 레이어로의 변환
     * @param w     the width of the border box / border box 너비
     * @param h     the height of the border box / border box 높이
     * @return true if the pixels were clipped / 잘랐으면 true
     */
    public synchronized boolean applyClipPath(String id, BufferedImage layer, AffineTransform ctm, double w, double h) {
        if (!has(id, "clipPath")) {
            return false;
        }
        BufferedImage pre = premultiplied(layer);
        SvgRenderer r = renderer(pre, ctm, w, h);
        try {
            SvgMasks.applyClipPath(r, pre, doc.ids.get(id), target, targetStyle(r), ctm);
        } catch (SvgRenderer.Abort a) {
            return false;
        } finally {
            r.g.dispose();
        }
        copyBack(pre, layer);
        return true;
    }

    /**
     * Returns the coverage of a <code>mask</code> element over a layer: the value that multiplies the alpha of the pixels of the element, from 0 to 1 for
     * every pixel (the luminance or the alpha of the mask according to <code>mask-type</code>, with the region and the units of the element).
     * <p>
     * <code>mask</code> 요소가 레이어에 주는 덮임을 반환한다: 요소의 픽셀 알파에 곱하는 값이며 픽셀마다 0~1 이다(<code>mask-type</code> 에 따른 마스크의 휘도나 알파, 요소의 영역과
     * 단위를 적용).
     *
     * @param id     the id of the mask element / 마스크 요소의 id
     * @param width  the width of the layer / 레이어 너비
     * @param height the height of the layer / 레이어 높이
     * @param ctm    the transform from the user space of the element to the layer / 요소의 사용자 공간에서 레이어로의 변환
     * @param w      the width of the border box / border box 너비
     * @param h      the height of the border box / border box 높이
     * @return one value per pixel, row by row, or null if the mask is not available (the element is then not masked) / 픽셀마다 값 하나(행 순서). 마스크를 쓸 수
     *         없으면 null(그러면 요소를 가리지 않는다)
     */
    public synchronized float[] maskCoverage(String id, int width, int height, AffineTransform ctm, double w, double h) {
        if (!has(id, "mask") || width < 1 || height < 1) {
            return null;
        }
        BufferedImage pre = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = pre.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        SvgRenderer r = renderer(pre, ctm, w, h);
        try {
            SvgMasks.applyMask(r, pre, doc.ids.get(id), target, targetStyle(r), ctm);
        } catch (SvgRenderer.Abort a) {
            return null;
        } finally {
            r.g.dispose();
        }
        int[] px = pre.getRGB(0, 0, width, height, null, 0, width);
        float[] out = new float[px.length];
        // 마스크를 적용한 흰 레이어의 알파가 마스크 값이다.
        for (int i = 0; i < px.length; i++) {
            out[i] = (px[i] >>> 24) / 255f;
        }
        return out;
    }

    /**
     * Copies a layer into a premultiplied image of the same size.
     * <p>
     * 레이어를 같은 크기의 알파를 미리 곱한 이미지로 복사한다.
     *
     * @param layer the layer (ARGB) / 레이어(ARGB)
     * @return the copy / 복사본
     */
    private static BufferedImage premultiplied(BufferedImage layer) {
        BufferedImage pre = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = pre.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(layer, 0, 0, null);
        } finally {
            g.dispose();
        }
        return pre;
    }

    /**
     * Copies a premultiplied image back into a layer, replacing its pixels.
     * <p>
     * 알파를 미리 곱한 이미지를 레이어로 되복사하여 픽셀을 대체한다.
     *
     * @param pre   the premultiplied image / 알파를 미리 곱한 이미지
     * @param layer the layer (ARGB) / 레이어(ARGB)
     */
    private static void copyBack(BufferedImage pre, BufferedImage layer) {
        Graphics2D g = layer.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(pre, 0, 0, null);
        } finally {
            g.dispose();
        }
    }
}
