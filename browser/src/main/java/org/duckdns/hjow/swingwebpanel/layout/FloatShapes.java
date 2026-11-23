package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The float area of a float with <code>shape-outside</code> (plan item P10-08, CSS Shapes Module Level 1, https://www.w3.org/TR/css-shapes-1/):
 * the basic shape <code>inset()</code>, <code>circle()</code>, <code>ellipse()</code>, <code>polygon()</code> (and the <code>rect()</code>,
 * <code>xywh()</code>, <code>path()</code> that {@link ClipPaths} reads) or a reference box alone, relative to the reference box (the
 * <code>margin-box</code> by default, or the <code>border-box</code>, <code>padding-box</code> or <code>content-box</code> that the value names),
 * grown outwards by <code>shape-margin</code> and cut off at the margin box of the float. The lines of the text next to the float wrap
 * around this area instead of around the margin box; boxes that establish a block formatting context and other floats still use the margin
 * box. A shape that is an image (<code>url()</code>, <code>image-set()</code> or a gradient, section 4 "Shapes from Image") is the area where the
 * alpha of the image is greater than <code>shape-image-threshold</code>: the image is sized and positioned as a replaced element whose width and
 * height are the content box of the float (so it is stretched to it) and, for an animated image, its first frame is used; an image without a
 * pixel above the threshold gives an empty area that does not affect wrapping, while an image that is not available (loading, failed, or a
 * cross-origin image without permission) leaves the margin box in force until it arrived. The area is built from the pixels at a resolution of
 * at most {@link #MAX_SAMPLES} samples per axis (the rows with equal runs are merged), so the shape follows the image within a sample.
 * <p>
 * <code>shape-outside</code> 가 있는 float 의 float 영역(계획 P10-08, CSS Shapes Module Level 1, https://www.w3.org/TR/css-shapes-1/): 기본 도형 <code>inset()</code>,
 * <code>circle()</code>, <code>ellipse()</code>, <code>polygon()</code>({@link ClipPaths} 가 읽는 <code>rect()</code>, <code>xywh()</code>, <code>path()</code> 포함)이나 기준 상자만을
 * 기준 상자(기본은 <code>margin-box</code>, 값이 가리키면 <code>border-box</code>, <code>padding-box</code>, <code>content-box</code>)에 상대적으로 놓고
 * <code>shape-margin</code> 만큼 바깥으로 넓힌 뒤 float 의 margin box 에서 자른 것. float 옆 텍스트의 줄은 margin box 대신 이 영역을 따라 감싼다. 블록 서식 문맥을 만드는 상자와 다른
 * float 는 여전히 margin box 를 쓴다. 이미지인 모양(<code>url()</code>, <code>image-set()</code>, 그라디언트. 4 절 "Shapes from Image")은 이미지의 알파가
 * <code>shape-image-threshold</code> 보다 큰 영역이다: 이미지는 너비와 높이가 float 의 콘텐츠 상자인 치환 요소처럼 크기와 위치가 정해지고(그 크기로 늘어남) 애니메이션 이미지는
 * 첫 프레임을 쓴다. 기준보다 큰 픽셀이 하나도 없는 이미지는 줄 감싸기에 영향이 없는 빈 영역이 되고, 쓸 수 없는 이미지(가져오는 중, 실패, 허락 없는 다른 오리진의 이미지)는
 * 도착할 때까지 margin box 가 그대로 쓰인다. 영역은 축마다 최대 {@link #MAX_SAMPLES} 개의 표본 해상도로 픽셀에서 만들며(같은 구간이 이어진 행은 합친다) 모양은 표본 하나의
 * 오차 안에서 이미지를 따른다.
 */
final class FloatShapes {

    /** The largest number of samples per axis that the area of an image shape is built from. 이미지 모양의 영역을 만드는 축마다의 최대 표본 수. */
    static final int MAX_SAMPLES = 512;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FloatShapes() {
    }

    /**
     * Returns the only item of a list of one item (the grammar of <code>shape-outside</code> may wrap a single function in a list), or the value.
     * <p>
     * 항목이 하나인 목록의 그 항목을 반환한다(<code>shape-outside</code> 의 문법은 함수 하나를 목록에 담을 수 있다). 아니면 값 그대로.
     *
     * @param v the value / 값
     * @return the value without the list / 목록을 벗긴 값
     */
    private static CssValue single(CssValue v) {
        return v instanceof CssList && ((CssList) v).getItems().size() == 1 ? ((CssList) v).getItems().get(0) : v;
    }

    /**
     * Tells whether a value of <code>shape-outside</code> is an image (<code>url()</code>, <code>image-set()</code> or a gradient) and not a
     * basic shape or a reference box.
     * <p>
     * <code>shape-outside</code> 값이 기본 도형이나 기준 상자가 아니라 이미지(<code>url()</code>, <code>image-set()</code>, 그라디언트)인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for an image / 이미지이면 true
     */
    static boolean isImage(CssValue value) {
        CssValue v = single(value);
        if (v instanceof CssUrl) {
            return true;
        }
        if (!(v instanceof CssFunctionValue)) {
            return false;
        }
        String n = ((CssFunctionValue) v).getName().toLowerCase(Locale.ENGLISH);
        return n.endsWith("gradient") || n.endsWith("image-set");
    }

    /**
     * Returns the address of the image that a <code>url()</code> or an <code>image-set()</code> names (the candidate with the smallest resolution
     * that is at least 1, else the largest one), or null for a gradient.
     * <p>
     * <code>url()</code> 이나 <code>image-set()</code> 이 가리키는 이미지의 주소를 반환한다(해상도가 1 이상인 후보 중 가장 작은 것, 없으면 가장 큰 것). 그라디언트이면 null.
     *
     * @param v the value / 값
     * @return the absolute address, or null / 절대 주소. 없으면 null
     */
    static String urlOf(CssValue value) {
        CssValue v = single(value);
        if (v instanceof CssUrl) {
            return ((CssUrl) v).getResolvedUrl();
        }
        if (!(v instanceof CssFunctionValue) || !((CssFunctionValue) v).getName().toLowerCase(Locale.ENGLISH).endsWith("image-set")) {
            return null;
        }
        String best = null;
        double bestRes = 0;
        List<CssValue> args = ((CssFunctionValue) v).getArguments();
        // 인수는 쉼표로 나뉜 후보이고, 후보는 값 하나이거나 공백으로 이은 값의 목록이다.
        for (int i = 0; i < args.size(); i++) {
            List<CssValue> cur = args.get(i) instanceof CssList ? ((CssList) args.get(i)).getItems() : java.util.Collections.singletonList(args.get(i));
            String url = null;
            double res = 1;
            // 후보 안에서 이미지와 해상도를 찾는다.
            for (int k = 0; k < cur.size(); k++) {
                CssValue c = cur.get(k);
                if (c instanceof CssUrl && url == null) {
                    url = ((CssUrl) c).getResolvedUrl();
                } else if (c instanceof CssString && url == null) {
                    url = ((CssString) c).getValue();
                } else if (c instanceof CssDimension) {
                    String unit = ((CssDimension) c).getUnit().toLowerCase(Locale.ENGLISH);
                    double d = ((CssDimension) c).getValue();
                    res = "x".equals(unit) || "dppx".equals(unit) ? d : "dpi".equals(unit) ? d / 96.0 : "dpcm".equals(unit) ? d * 2.54 / 96.0 : res;
                }
            }
            if (url == null || res <= 0) {
                continue;
            }
            // 1 이상인 해상도 중 가장 작은 것을 고르고, 그런 것이 없으면 가장 큰 것을 고른다.
            boolean better = best == null || (bestRes < 1 && res > bestRes) || (res >= 1 && (bestRes < 1 || res < bestRes));
            if (better) {
                best = url;
                bestRes = res;
            }
        }
        return best;
    }

    /**
     * Returns the float area of a float, in the coordinates of the block formatting context.
     * <p>
     * float 의 float 영역을 블록 서식 문맥의 좌표로 반환한다.
     *
     * @param fb the float box that has been laid out / 배치가 끝난 float 상자
     * @param mx left of the margin box in the coordinates of the context / 문맥 좌표의 margin box 왼쪽
     * @param my top of the margin box / margin box 위쪽
     * @param mw width of the margin box / margin box 너비
     * @param mh height of the margin box / margin box 높이
     * @param cw width of the containing block (the base of a percentage of <code>shape-margin</code>) / 포함 블록의 너비(<code>shape-margin</code> 백분율의 기준)
     * @return the float area, or null if the float is wrapped around by its margin box / float 영역. margin box 로 감싸면 null
     */
    static Area areaOf(LayoutBox fb, double mx, double my, double mw, double mh, double cw) {
        ComputedStyle s = fb.getStyle();
        if (s == null || s.get(Prop.SHAPE_OUTSIDE) == null || s.get(Prop.SHAPE_OUTSIDE).isKeyword("none")) {
            return null;
        }
        CssValue value = single(s.get(Prop.SHAPE_OUTSIDE));
        Shape shape;
        // 이미지인 값은 알파 채널에서, 나머지는 기본 도형이나 기준 상자에서 모양을 만든다.
        if (isImage(value)) {
            shape = imageShape(fb, s, value, mx + fb.getMarginLeft() + fb.getBorderLeft() + fb.getPaddingLeft(),
                    my + fb.getMarginTop() + fb.getBorderTop() + fb.getPaddingTop());
        } else {
            shape = ClipPaths.shapeFor(fb, Prop.SHAPE_OUTSIDE, "margin-box", mx + fb.getMarginLeft(), my + fb.getMarginTop());
        }
        if (shape == null) {
            return null;
        }
        Area area = new Area(shape);
        double m = s.resolve(Prop.SHAPE_MARGIN, cw);
        if (!Double.isNaN(m) && m > 0) {
            // 모양을 shape-margin 만큼 바깥으로 넓힌다(둥근 연결의 굵은 윤곽선을 합친다).
            area.add(new Area(new BasicStroke((float) (2 * m), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(shape)));
        }
        area.intersect(new Area(new Rectangle2D.Double(mx, my, mw, mh)));
        return area;
    }

    /**
     * Builds the shape of an image: the rectangles of the pixels whose alpha is greater than <code>shape-image-threshold</code>, with the image
     * stretched over the content box of the float.
     * <p>
     * 이미지의 모양을 만든다: 알파가 <code>shape-image-threshold</code> 보다 큰 픽셀의 사각형들이며, 이미지는 float 의 콘텐츠 상자에 맞게 늘린다.
     *
     * @param fb    the float / float
     * @param s     its style / 스타일
     * @param value the <code>shape-outside</code> value that is an image / 이미지인 <code>shape-outside</code> 값
     * @param cx    left of the content box in the coordinates of the context / 문맥 좌표의 콘텐츠 상자 왼쪽
     * @param cy    top of the content box / 콘텐츠 상자 위쪽
     * @return the shape (empty if no pixel is above the threshold), or null if the image is not available / 모양(기준보다 큰 픽셀이 없으면 빈 모양). 이미지를 쓸 수 없으면 null
     */
    private static Shape imageShape(LayoutBox fb, ComputedStyle s, CssValue value, double cx, double cy) {
        ShapeImageSource source = fb.shapeSource;
        double w = fb.getContentWidth();
        double h = fb.getContentHeight();
        if (source == null || !(w > 0) || !(h > 0)) {
            return null;
        }
        String url = urlOf(value);
        ShapeImage image = url != null ? source.getShapeImage(url)
                : value instanceof CssFunctionValue ? source.getShapeGradient((CssFunctionValue) value, s.color(Prop.COLOR), (int) Math.ceil(w), (int) Math.ceil(h)) : null;
        if (image == null) {
            return null;
        }
        double t = s.number(Prop.SHAPE_IMAGE_THRESHOLD);
        int limit = Double.isNaN(t) ? 0 : (int) Math.floor(Math.max(0, Math.min(1, t)) * 255 + 1e-9);
        int nx = (int) Math.max(1, Math.min(MAX_SAMPLES, Math.ceil(w)));
        int ny = (int) Math.max(1, Math.min(MAX_SAMPLES, Math.ceil(h)));
        double sw = w / nx;
        double sh = h / ny;
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        int[] runs = new int[nx + 2];
        int[] prev = new int[nx + 2];
        int prevCount = 0;
        int prevStart = 0;
        // 표본 행마다 알파가 기준보다 큰 구간(열의 시작과 끝)을 구하고, 앞 행과 같은 구간이면 하나의 사각형으로 합친다.
        for (int row = 0; row <= ny; row++) {
            int count = 0;
            if (row < ny) {
                int iy = Math.min(image.getHeight() - 1, (int) ((row + 0.5) * image.getHeight() / ny));
                int start = -1;
                // 표본 열을 훑으며 기준보다 큰 연속 구간을 찾는다.
                for (int col = 0; col <= nx; col++) {
                    boolean on = false;
                    if (col < nx) {
                        int ix = Math.min(image.getWidth() - 1, (int) ((col + 0.5) * image.getWidth() / nx));
                        on = image.alphaAt(ix, iy) > limit;
                    }
                    if (on && start < 0) {
                        start = col;
                    } else if (!on && start >= 0) {
                        runs[count++] = start;
                        runs[count++] = col;
                        start = -1;
                    }
                }
            }
            boolean same = count == prevCount;
            // 앞 행과 구간이 같은지 비교한다.
            for (int k = 0; same && k < count; k++) {
                same = runs[k] == prev[k];
            }
            if (same && row < ny) {
                continue;
            }
            // 구간이 바뀌었으므로 앞에서 이어진 행들을 사각형으로 낸다.
            for (int k = 0; k < prevCount; k += 2) {
                path.append(new Rectangle2D.Double(cx + prev[k] * sw, cy + prevStart * sh, (prev[k + 1] - prev[k]) * sw, (row - prevStart) * sh), false);
            }
            int[] swap = prev;
            prev = runs;
            runs = swap;
            prevCount = count;
            prevStart = row;
        }
        return path;
    }
}
