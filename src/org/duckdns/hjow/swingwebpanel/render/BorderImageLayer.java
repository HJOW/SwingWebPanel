package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The nine part image of <code>border-image</code> (CSS Backgrounds and Borders Level 3, <a href="https://www.w3.org/TR/css-backgrounds-3/#border-images">section 5</a>)
 * and of <code>mask-border</code> (CSS Masking Level 1, <a href="https://www.w3.org/TR/css-masking-1/#the-mask-border">https://www.w3.org/TR/css-masking-1/#the-mask-border</a>,
 * which uses the same slicing and drawing process): the image (or gradient) of the source property is cut by the slice property into the four corners, the four edges
 * and the middle, which are drawn into the border image area (the border box grown by the outset property) with the sizes of the width property. A corner is
 * scaled to its region; an edge is scaled to the thickness of its region and stretched, repeated, rounded or spaced along its length (the repeat property); the middle
 * is drawn only with <code>fill</code>. The tiles of every part are sub images of their own, so a slice never blends with the pixels of its neighbors.
 * <p>
 * For <code>border-image</code> the layer is painted in place of the border styles (section 5.1: a source that is <code>none</code> or cannot be displayed leaves
 * the border styles in force) and is not clipped by <code>border-radius</code> (section 4.3). For <code>mask-border</code> the pixels of the layer are the mask
 * (alpha or luminance as <code>mask-border-mode</code> says) that {@link MaskLayers} composites on top of the layers of <code>mask-image</code>.
 * <p>
 * <code>border-image</code>(CSS Backgrounds and Borders Level 3 5 절)와 <code>mask-border</code>(CSS Masking Level 1; 같은 자르기와 그리기 과정을 쓴다)의 아홉 조각 이미지:
 * 원본 속성의 이미지(또는 그라디언트)를 slice 속성으로 네 모서리, 네 가장자리, 가운데로 잘라 width 속성의 크기로 border 이미지 영역(outset 속성만큼 넓힌 border box)에 그린다.
 * 모서리는 자기 영역 크기로 늘여 그리고, 가장자리는 영역의 두께에 맞춰 크기를 조정한 뒤 길이 방향으로 늘이거나(stretch) 되풀이하거나(repeat) 개수를 정수로 맞추거나(round)
 * 간격을 두어(space) 그리며(repeat 속성), 가운데는 <code>fill</code> 일 때만 그린다. 조각마다 자기만의 부분 이미지로 그리므로 조각이 이웃 화소와 섞이지 않는다.
 * <p>
 * <code>border-image</code> 에서는 이 레이어를 border 스타일 대신 그리며(5.1 절: 원본이 <code>none</code> 이거나 표시할 수 없으면 border 스타일이 그대로 쓰인다)
 * <code>border-radius</code> 로 자르지 않는다(4.3 절). <code>mask-border</code> 에서는 레이어의 화소가 마스크(<code>mask-border-mode</code> 에 따라 알파나 휘도)이며
 * {@link MaskLayers} 가 <code>mask-image</code> 레이어 위에 합성한다.
 */
final class BorderImageLayer extends DisplayCommand {

    /**
     * The property ids of <code>mask-border</code>: source, slice, width, outset and repeat.
     * <p>
     * <code>mask-border</code> 의 속성 id: 원본, slice, width, outset, repeat.
     */
    static final int[] MASK = {Prop.MASK_BORDER_SOURCE, Prop.MASK_BORDER_SLICE, Prop.MASK_BORDER_WIDTH, Prop.MASK_BORDER_OUTSET, Prop.MASK_BORDER_REPEAT};

    /**
     * The property ids of <code>border-image</code>: source, slice, width, outset and repeat.
     * <p>
     * <code>border-image</code> 의 속성 id: 원본, slice, width, outset, repeat.
     */
    static final int[] BORDER = {Prop.BORDER_IMAGE_SOURCE, Prop.BORDER_IMAGE_SLICE, Prop.BORDER_IMAGE_WIDTH, Prop.BORDER_IMAGE_OUTSET, Prop.BORDER_IMAGE_REPEAT};

    /** The most tiles along an axis that are drawn. 한 축에 그리는 타일 수의 상한. */
    private static final int MAX_TILES = 2000;

    /** The image, or null for a gradient. 이미지. 그라디언트이면 null. */
    private final DecodedImage image;

    /** The gradient, or null for an image. 그라디언트. 이미지이면 null. */
    private final GradientSpec gradient;

    /** The border image area {x, y, width, height}. border 이미지 영역 {x, y, 너비, 높이}. */
    private final double[] area;

    /** The slice sizes top, right, bottom, left in the pixels of the source image. 원본 이미지 화소 단위의 자르는 크기(위, 오른쪽, 아래, 왼쪽). */
    private final double[] slice;

    /** The sizes of the border image regions top, right, bottom, left in pixels. border 이미지 영역의 크기(위, 오른쪽, 아래, 왼쪽)(픽셀). */
    private final double[] widths;

    /** True if the middle part is drawn. 가운데 부분을 그리면 true. */
    private final boolean fill;

    /** The repeat modes of the horizontal and the vertical edges. 가로 가장자리와 세로 가장자리의 반복 방식. */
    private final String[] repeat;

    /** The picture of an image function of CSS Images 4 (<code>cross-fade()</code>, <code>image()</code>, <code>element()</code>), or null. CSS Images 4 이미지 함수의 그림. 없으면 null. */
    private GeneratedImages.Picture picture;

    /** The size {w, h} in pixels of the raster that the picture is made into. 그림을 만드는 래스터의 화소 크기 {w, h}. */
    private double[] pictureSize;

    /**
     * Creates the layer.
     * <p>
     * 레이어를 만든다.
     *
     * @param type     the command type / 명령 종류
     * @param image    the image or null / 이미지 또는 null
     * @param gradient the gradient or null / 그라디언트 또는 null
     * @param area     the border image area / border 이미지 영역
     * @param slice    the slice sizes in source pixels / 원본 화소 단위의 자르는 크기
     * @param widths   the sizes of the regions / 영역의 크기
     * @param fill     true to draw the middle / 가운데를 그리면 true
     * @param repeat   the repeat modes / 반복 방식
     * @param node     the DOM node or null / DOM 노드 또는 null
     */
    private BorderImageLayer(Type type, DecodedImage image, GradientSpec gradient, double[] area, double[] slice, double[] widths, boolean fill, String[] repeat, Node node) {
        super(type, area[0], area[1], area[2], area[3], node);
        this.image = image;
        this.gradient = gradient;
        this.area = area;
        this.slice = slice;
        this.widths = widths;
        this.fill = fill;
        this.repeat = repeat;
    }

    /**
     * Tells whether the style names a source (a source other than <code>none</code>), whether or not the source can be displayed yet.
     * <p>
     * 스타일이 원본을 가리키는지(<code>none</code> 이 아닌 원본)를 알려 준다. 원본을 아직 표시할 수 있는지는 상관없다.
     *
     * @param s     the style / 스타일
     * @param props the property ids, {@link #MASK} or {@link #BORDER} / 속성 id. {@link #MASK} 나 {@link #BORDER}
     * @return true if there is a source / 원본이 있으면 true
     */
    static boolean hasSource(ComputedStyle s, int[] props) {
        CssValue source = s == null ? null : s.get(props[0]);
        return source != null && !source.isKeyword("none");
    }

    /**
     * Returns how far the border image area reaches beyond the border box (the outset property), or null if the style has no source.
     * <p>
     * border 이미지 영역이 border box 밖으로 뻗는 거리(outset 속성)를 반환한다. 스타일에 원본이 없으면 null.
     *
     * @param box   the box / 상자
     * @param s     the style / 스타일
     * @param props the property ids / 속성 id
     * @return the outsets top, right, bottom, left, or null / 위, 오른쪽, 아래, 왼쪽 outset. 없으면 null
     */
    static double[] outsetOf(LayoutBox box, ComputedStyle s, int[] props) {
        if (!hasSource(s, props)) {
            return null;
        }
        double[] borders = {box.getBorderTop(), box.getBorderRight(), box.getBorderBottom(), box.getBorderLeft()};
        return fourValues(s.get(props[3]), borders, null, 0, null);
    }

    /**
     * Builds the layer of a box over its border box, or returns null if the source is <code>none</code> or cannot be used (an image that has not arrived).
     * <p>
     * 상자의 border box 위에 놓이는 레이어를 만든다. 원본이 <code>none</code> 이거나 쓸 수 없으면(아직 도착하지 않은 이미지) null.
     *
     * @param box     the box / 상자
     * @param s       its style / 스타일
     * @param builder the builder that knows the images / 이미지를 아는 빌더
     * @param props   the property ids, {@link #MASK} or {@link #BORDER} / 속성 id. {@link #MASK} 나 {@link #BORDER}
     * @return the layer or null / 레이어. 없으면 null
     */
    static BorderImageLayer of(LayoutBox box, ComputedStyle s, BackgroundLayers builder, int[] props) {
        return of(box, box.getX(), box.getY(), box.getWidth(), box.getHeight(), s, builder, props);
    }

    /**
     * Builds the layer of a box over the given rectangle (the grid of a table is not its whole box), or returns null if the source is <code>none</code> or cannot
     * be used (an image that has not arrived).
     * <p>
     * 상자의 주어진 사각형 위에 놓이는 레이어를 만든다(표의 격자는 표 상자 전체가 아니다). 원본이 <code>none</code> 이거나 쓸 수 없으면(아직 도착하지 않은 이미지) null.
     *
     * @param box     the box (for the border widths and the node) / 상자(border 너비와 노드용)
     * @param x       left of the border box / border box 의 왼쪽
     * @param y       top of the border box / border box 의 위쪽
     * @param w       width of the border box / border box 의 너비
     * @param h       height of the border box / border box 의 높이
     * @param s       its style / 스타일
     * @param builder the builder that knows the images / 이미지를 아는 빌더
     * @param props   the property ids, {@link #MASK} or {@link #BORDER} / 속성 id. {@link #MASK} 나 {@link #BORDER}
     * @return the layer or null / 레이어. 없으면 null
     */
    static BorderImageLayer of(LayoutBox box, double x, double y, double w, double h, ComputedStyle s, BackgroundLayers builder, int[] props) {
        CssValue source = s.get(props[0]);
        if (source == null || source.isKeyword("none")) {
            return null;
        }
        GeneratedImages.Picture pic = builder.picture(source, box.getNode(), s);
        DecodedImage di = pic == null ? builder.decodedImage(source) : null;
        GradientSpec grad = di == null && pic == null ? builder.gradient(source, s.color(Prop.COLOR)) : null;
        if (di == null && grad == null && pic == null) {
            return null;
        }
        // 고유 크기가 있는 그림은 그 크기를 원본 이미지의 크기로 쓰고, 없으면 그라디언트처럼 영역 크기를 쓴다.
        boolean sized = di != null || (pic != null && pic.hasNaturalSize());
        double[] borders = {box.getBorderTop(), box.getBorderRight(), box.getBorderBottom(), box.getBorderLeft()};
        double[] outset = fourValues(s.get(props[3]), borders, null, 0, null);
        double ax = x - outset[3];
        double ay = y - outset[0];
        double aw = w + outset[1] + outset[3];
        double ah = h + outset[0] + outset[2];
        if (aw <= 0 || ah <= 0) {
            return null;
        }
        double iw = di != null ? di.getWidth() : (sized ? pic.naturalWidth() : aw);
        double ih = di != null ? di.getHeight() : (sized ? pic.naturalHeight() : ah);
        CssValue sv = s.get(props[1]);
        boolean fill = sv != null && (" " + sv.toCssText() + " ").indexOf(" fill ") >= 0;
        double[] sl = sliceOf(sv, iw, ih);
        // auto 인 너비는 이미지 조각의 크기를 쓴다. 고유 크기가 없는 그라디언트이면 border 너비를 쓴다(CSS Backgrounds 3 5.3).
        double[] wd = fourValues(s.get(props[2]), borders, new double[] {aw, ah}, Double.NaN, sized ? sl : borders);
        // 두 반대쪽 너비의 합이 영역을 넘으면 모두 같은 비율로 줄인다(5.3).
        double f = 1;
        if (wd[1] + wd[3] > aw && wd[1] + wd[3] > 0) {
            f = Math.min(f, aw / (wd[1] + wd[3]));
        }
        if (wd[0] + wd[2] > ah && wd[0] + wd[2] > 0) {
            f = Math.min(f, ah / (wd[0] + wd[2]));
        }
        // 비율이 1 보다 작으면 네 너비를 모두 줄인다.
        for (int i = 0; i < 4; i++) {
            wd[i] *= f;
        }
        CssValue rv = s.get(props[4]);
        String[] rep = {"stretch", "stretch"};
        if (rv != null) {
            List<CssValue> items = rv instanceof CssList ? ((CssList) rv).getItems() : java.util.Collections.singletonList(rv);
            rep[0] = items.get(0) instanceof CssKeyword ? ((CssKeyword) items.get(0)).getName() : "stretch";
            rep[1] = items.size() > 1 && items.get(1) instanceof CssKeyword ? ((CssKeyword) items.get(1)).getName() : rep[0];
        }
        Type type = props == BORDER ? Type.BORDER : Type.BACKGROUND_IMAGE;
        BorderImageLayer layer = new BorderImageLayer(type, di, grad, new double[] {ax, ay, aw, ah}, sl, wd, fill, rep, box.getNode());
        layer.picture = pic;
        layer.pictureSize = new double[] {iw, ih};
        return layer;
    }

    /**
     * Reads a property of one to four values (width or outset): a number multiplies the border width of the side, a length is in pixels, a percentage is relative to
     * the border image area, and <code>auto</code> (or a value that is missing) is the fallback.
     * <p>
     * 값이 한 개에서 네 개인 속성(width 나 outset)을 읽는다: 숫자는 그 변의 border 너비를 곱하고, 길이는 픽셀이며, 백분율은 border 이미지 영역 기준이고,
     * <code>auto</code>(또는 없는 값)는 대체 값이다.
     *
     * @param v        the value / 값
     * @param borders  the border widths top, right, bottom, left / border 너비
     * @param base     the width and the height of the area for percentages, or null if percentages are not allowed / 백분율의 기준 영역 너비와 높이. 백분율을 쓸 수 없으면 null
     * @param def      the value of a missing property or of an <code>auto</code> if <code>autoValues</code> is null / 값이 없을 때의 값. <code>autoValues</code> 가 null 이면 <code>auto</code> 의 값
     * @param autoValues the four values that <code>auto</code> stands for, or null / <code>auto</code> 가 뜻하는 네 값. 없으면 null
     * @return the four sizes in pixels / 네 크기(픽셀)
     */
    private static double[] fourValues(CssValue v, double[] borders, double[] base, double def, double[] autoValues) {
        List<CssValue> items = v == null ? java.util.Collections.<CssValue>emptyList() : (v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v));
        double[] out = new double[4];
        // 네 변마다 값을 정한다(값이 하나이면 모두, 둘이면 위아래/좌우, 셋이면 왼쪽은 오른쪽을 따른다).
        for (int side = 0; side < 4; side++) {
            int k = items.isEmpty() ? -1 : (items.size() == 1 ? 0 : (items.size() == 2 ? side % 2 : (items.size() == 3 ? (side == 3 ? 1 : side) : side)));
            CssValue it = k < 0 ? null : items.get(k);
            if (it instanceof CssNumber) {
                out[side] = ((CssNumber) it).getValue() * borders[side];
            } else if (it instanceof CssLength) {
                out[side] = ((CssLength) it).getValue();
            } else if (it instanceof CssPercentage && base != null) {
                out[side] = ((CssPercentage) it).getValue() / 100.0 * (side % 2 == 0 ? base[1] : base[0]);
            } else {
                out[side] = autoValues != null ? autoValues[side] : def;
            }
            if (Double.isNaN(out[side]) || out[side] < 0) {
                out[side] = 0;
            }
        }
        return out;
    }

    /**
     * Reads the slice property: numbers are pixels of the image, percentages are relative to the size of the image (the width for the right and the left
     * slices, the height for the top and bottom ones); a slice is not larger than the image (CSS Backgrounds 3 5.2).
     * <p>
     * slice 속성을 읽는다: 숫자는 이미지의 화소이고 백분율은 이미지 크기 기준이다(오른쪽과 왼쪽 조각은 너비, 위와 아래 조각은 높이). 조각은 이미지보다 클 수 없다(CSS Backgrounds 3 5.2).
     *
     * @param v  the value / 값
     * @param iw the width of the image / 이미지 너비
     * @param ih the height of the image / 이미지 높이
     * @return the four slice sizes top, right, bottom, left / 네 조각 크기(위, 오른쪽, 아래, 왼쪽)
     */
    private static double[] sliceOf(CssValue v, double iw, double ih) {
        List<CssValue> items = new java.util.ArrayList<CssValue>();
        // fill 키워드를 뺀 수치만 모은다.
        if (v != null) {
            List<CssValue> all = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
            // 값의 항목 중 수치(숫자, 백분율)만 고른다.
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i) instanceof CssNumber || all.get(i) instanceof CssPercentage) {
                    items.add(all.get(i));
                }
            }
        }
        double[] out = new double[4];
        // 네 변마다 값을 정한다(값이 하나이면 모두, 둘이면 위아래/좌우, 셋이면 왼쪽은 오른쪽을 따른다).
        for (int side = 0; side < 4; side++) {
            int k = items.isEmpty() ? -1 : (items.size() == 1 ? 0 : (items.size() == 2 ? side % 2 : (items.size() == 3 ? (side == 3 ? 1 : side) : side)));
            CssValue it = k < 0 ? null : items.get(k);
            double size = side % 2 == 0 ? ih : iw;
            double val = 0;
            if (it instanceof CssNumber) {
                val = ((CssNumber) it).getValue();
            } else if (it instanceof CssPercentage) {
                val = ((CssPercentage) it).getValue() / 100.0 * size;
            }
            out[side] = Math.max(0, Math.min(size, val));
        }
        return out;
    }

    /**
     * Paints the nine parts into the area; for a mask the pixels that they cover are the mask. The parts are painted inside the clip that is already set (an ancestor's
     * clip stays in force).
     * <p>
     * 아홉 조각을 영역에 그린다. 마스크이면 그것이 덮는 화소가 마스크이다. 이미 설정된 클립 안에서 그린다(조상의 클립이 그대로 적용된다).
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        BufferedImage src = source(ctx);
        if (src == null) {
            return;
        }
        Graphics2D g = ctx.g;
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Shape oldClip = g.getClip();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int iw = src.getWidth();
            int ih = src.getHeight();
            // 원본의 열과 행 경계: 왼쪽 조각, 가운데, 오른쪽 조각과 위 조각, 가운데, 아래 조각.
            double[] sx = {0, slice[3], iw - slice[1], iw};
            double[] sy = {0, slice[0], ih - slice[2], ih};
            // 영역의 열과 행 경계.
            double[] dx = {area[0], area[0] + widths[3], area[0] + area[2] - widths[1], area[0] + area[2]};
            double[] dy = {area[1], area[1] + widths[0], area[1] + area[3] - widths[2], area[1] + area[3]};
            // 위 줄에서 아래 줄까지, 왼쪽 열에서 오른쪽 열까지 아홉 조각을 그린다(가운데는 fill 일 때만).
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (row == 1 && col == 1 && !fill) {
                        continue;
                    }
                    double sw = sx[col + 1] - sx[col];
                    double sh = sy[row + 1] - sy[row];
                    double dw = dx[col + 1] - dx[col];
                    double dh = dy[row + 1] - dy[row];
                    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) {
                        continue;
                    }
                    // 조각마다 자기 영역으로 자른다(이미 있는 클립과 겹친 부분만 그려진다).
                    g.setClip(oldClip);
                    g.clip(new Rectangle2D.Double(dx[col], dy[row], dw, dh));
                    part(g, src, row, col, sx[col], sy[row], sw, sh, dx[col], dy[row], dw, dh);
                }
            }
        } finally {
            g.setClip(oldClip);
            if (oldHint != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            }
        }
    }

    /**
     * Draws one part (CSS Backgrounds 3 5.6): the slice <code>(sx, sy, sw, sh)</code> of the image into the region <code>(dx, dy, dw, dh)</code>. The tile is scaled to
     * the region (a corner), or to the thickness of the region and proportionally along it (an edge), or by the factors of the edges (the middle); then it is stretched,
     * or repeated (the first tile centered), or rounded (the tile size makes the number whole) or spaced (whole tiles, the rest of the room equal before, between and
     * after them) along the axes.
     * <p>
     * 조각 하나를 그린다(CSS Backgrounds 3 5.6): 이미지의 조각 <code>(sx, sy, sw, sh)</code> 를 영역 <code>(dx, dy, dw, dh)</code> 에 그린다. 타일은 영역 크기로(모서리),
     * 영역의 두께로 그리고 길이 방향으로는 같은 비율로(가장자리), 또는 가장자리의 비율로(가운데) 크기를 맞춘 뒤 축마다 늘이거나(stretch) 되풀이하거나(repeat. 첫 타일은 가운데에 둔다)
     * 개수를 정수로 맞추거나(round) 정수 개 타일을 놓고 남는 공간을 앞, 사이, 뒤에 똑같이 나눈다(space).
     *
     * @param g   the graphics / 그래픽스
     * @param src the source image / 원본 이미지
     * @param row the row of the part (0 top, 1 middle, 2 bottom) / 조각의 행(0 위, 1 가운데, 2 아래)
     * @param col the column of the part (0 left, 1 middle, 2 right) / 조각의 열(0 왼쪽, 1 가운데, 2 오른쪽)
     * @param sx  the left of the slice / 조각의 왼쪽
     * @param sy  the top of the slice / 조각의 위쪽
     * @param sw  the width of the slice / 조각의 너비
     * @param sh  the height of the slice / 조각의 높이
     * @param dx  the left of the region / 영역의 왼쪽
     * @param dy  the top of the region / 영역의 위쪽
     * @param dw  the width of the region / 영역의 너비
     * @param dh  the height of the region / 영역의 높이
     */
    private void part(Graphics2D g, BufferedImage src, int row, int col, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh) {
        BufferedImage tile = sub(src, sx, sy, sw, sh);
        if (tile == null) {
            return;
        }
        double tw;
        double th;
        String modeX;
        String modeY;
        // 모서리는 영역 크기로, 위아래 가장자리는 높이를 영역에 맞추고 너비를 같은 비율로, 왼쪽 오른쪽 가장자리는 너비를 맞추고 높이를 같은 비율로 조정한다.
        if (row != 1 && col != 1) {
            modeX = "stretch";
            modeY = "stretch";
            tw = dw;
            th = dh;
        } else if (row != 1) {
            modeX = repeat[0];
            modeY = "stretch";
            th = dh;
            tw = sw * (dh / sh);
        } else if (col != 1) {
            modeX = "stretch";
            modeY = repeat[1];
            tw = dw;
            th = sh * (dw / sw);
        } else {
            // 가운데의 비율은 위 가장자리(없으면 아래)와 왼쪽 가장자리(없으면 오른쪽)가 정한다.
            modeX = repeat[0];
            modeY = repeat[1];
            tw = sw * factor(widths[0] / slice[0], widths[2] / slice[2]);
            th = sh * factor(widths[3] / slice[3], widths[1] / slice[1]);
        }
        double[] ax = axis(modeX, dw, tw);
        double[] ay = axis(modeY, dh, th);
        int nx = (int) ax[0];
        int ny = (int) ay[0];
        if (nx <= 0 || ny <= 0) {
            return;
        }
        boolean touchX = Math.abs(ax[2] - ax[3]) < 1e-9;
        boolean touchY = Math.abs(ay[2] - ay[3]) < 1e-9;
        // 타일을 가로, 세로로 놓는다(영역 밖은 클립이 자른다). 맞닿은 타일은 같은 경계 값을 써서 틈이 생기지 않게 한다.
        for (int j = 0; j < ny; j++) {
            double y0 = dy + ay[1] + j * ay[2];
            double y1 = touchY ? dy + ay[1] + (j + 1) * ay[2] : y0 + ay[3];
            // 한 줄의 타일을 왼쪽에서 오른쪽으로 놓는다.
            for (int i = 0; i < nx; i++) {
                double x0 = dx + ax[1] + i * ax[2];
                double x1 = touchX ? dx + ax[1] + (i + 1) * ax[2] : x0 + ax[3];
                int px0 = (int) Math.round(x0);
                int py0 = (int) Math.round(y0);
                int pw = (int) Math.round(x1) - px0;
                int ph = (int) Math.round(y1) - py0;
                if (pw > 0 && ph > 0) {
                    g.drawImage(tile, px0, py0, pw, ph, null);
                }
            }
        }
    }

    /**
     * Returns the scale factor of the middle part: the one of the first edge unless it is zero, infinite or not a number, else the one of the second edge, else 1
     * (CSS Backgrounds 3 5.6).
     * <p>
     * 가운데 조각의 배율을 반환한다: 첫째 가장자리의 배율. 그것이 0, 무한, 숫자가 아니면 둘째 가장자리의 배율, 그것도 안 되면 1(CSS Backgrounds 3 5.6).
     *
     * @param first  the factor of the first edge / 첫째 가장자리의 배율
     * @param second the factor of the second edge / 둘째 가장자리의 배율
     * @return the factor / 배율
     */
    private static double factor(double first, double second) {
        if (first > 0 && !Double.isInfinite(first)) {
            return first;
        }
        if (second > 0 && !Double.isInfinite(second)) {
            return second;
        }
        return 1;
    }

    /**
     * Lays out the tiles along one axis of a region for a repeat mode (CSS Backgrounds 3 5.5 and 5.6).
     * <p>
     * 영역의 한 축을 따라 반복 방식에 맞게 타일을 배치한다(CSS Backgrounds 3 5.5, 5.6 절).
     *
     * @param mode   the repeat mode / 반복 방식
     * @param region the length of the region / 영역의 길이
     * @param tile   the length of a tile before the repeat mode / 반복 방식을 적용하기 전 타일의 길이
     * @return {the number of tiles (0 for none), the offset of the first tile from the start of the region, the step from a tile to the next, the length of a tile}
     *         / {타일 수(없으면 0), 영역 시작에서 첫 타일까지의 거리, 타일에서 다음 타일까지의 간격, 타일의 길이}
     */
    static double[] axis(String mode, double region, double tile) {
        if ("stretch".equals(mode) || Double.isNaN(tile) || Double.isInfinite(tile) || tile <= 0) {
            return new double[] {1, 0, region, region};
        }
        if ("round".equals(mode)) {
            // 타일 수를 가장 가까운 정수(최소 1)로 맞추고 그 수로 타일 길이를 다시 정한다.
            int n = (int) Math.max(1, Math.min(MAX_TILES, Math.round(region / tile)));
            return new double[] {n, 0, region / n, region / n};
        }
        if ("space".equals(mode)) {
            // 들어가는 정수 개의 타일을 놓고 남는 공간을 앞, 사이, 뒤에 똑같이 나눈다. 한 개도 안 들어가면 그리지 않는다.
            double fit = Math.floor(region / tile + 1e-9);
            if (fit < 1) {
                return new double[] {0, 0, tile, tile};
            }
            int n = (int) Math.min(MAX_TILES, fit);
            double gap = (region - n * tile) / (n + 1);
            return new double[] {n, gap, tile + gap, tile};
        }
        // repeat: 첫 타일을 영역 가운데에 놓고 양쪽으로 영역을 덮을 만큼 되풀이한다(가운데 맞춤을 지키려고 개수는 홀수).
        int n = (int) Math.min(MAX_TILES, Math.ceil(region / tile - 1e-9));
        if (n % 2 == 0) {
            n++;
        }
        return new double[] {n, (region - n * tile) / 2, tile, tile};
    }

    /**
     * Returns the part of the source image that a slice covers as an image of its own, or null if the slice is empty. A slice that is narrower than one pixel is
     * widened to one pixel.
     * <p>
     * 조각이 덮는 원본 이미지의 부분을 따로 된 이미지로 반환한다. 조각이 비어 있으면 null. 한 화소보다 좁은 조각은 한 화소로 넓힌다.
     *
     * @param src the source image / 원본 이미지
     * @param sx  the left of the slice / 조각의 왼쪽
     * @param sy  the top of the slice / 조각의 위쪽
     * @param sw  the width of the slice / 조각의 너비
     * @param sh  the height of the slice / 조각의 높이
     * @return the sub image or null / 부분 이미지. 없으면 null
     */
    private static BufferedImage sub(BufferedImage src, double sx, double sy, double sw, double sh) {
        int w = src.getWidth();
        int h = src.getHeight();
        int x0 = Math.max(0, Math.min(w - 1, (int) Math.round(sx)));
        int y0 = Math.max(0, Math.min(h - 1, (int) Math.round(sy)));
        int x1 = Math.max(x0 + 1, Math.min(w, (int) Math.round(sx + sw)));
        int y1 = Math.max(y0 + 1, Math.min(h, (int) Math.round(sy + sh)));
        if (sw <= 0 || sh <= 0 || w <= 0 || h <= 0) {
            return null;
        }
        return src.getSubimage(x0, y0, x1 - x0, y1 - y0);
    }

    /**
     * Returns the source raster: the first frame of the image, or the gradient painted into an image as large as the area.
     * <p>
     * 원본 래스터를 반환한다: 이미지의 첫 프레임, 또는 영역 크기의 이미지에 그린 그라디언트.
     *
     * @param ctx the context / 컨텍스트
     * @return the raster / 래스터
     */
    private BufferedImage source(PaintContext ctx) {
        if (image != null) {
            return DisplayCommands.imageOf(DisplayCommands.frameAt(image, ctx.timeMillis), image.getWidth(), image.getHeight());
        }
        if (picture != null) {
            // 그림은 원본 크기(고유 크기나 영역 크기)의 래스터로 만든다.
            int pw = (int) Math.ceil(pictureSize[0]);
            int ph = (int) Math.ceil(pictureSize[1]);
            if (pw <= 0 || ph <= 0 || (long) pw * ph > 16L * 1024 * 1024) {
                return null;
            }
            return picture.raster(pw, ph, ctx.timeMillis);
        }
        int w = (int) Math.ceil(area[2]);
        int h = (int) Math.ceil(area[3]);
        if (w <= 0 || h <= 0 || (long) w * h > 16L * 1024 * 1024) {
            return null;
        }
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setPaint(gradient.createPaint(w, h));
            g.fill(new Rectangle2D.Double(0, 0, w, h));
        } finally {
            g.dispose();
        }
        return bi;
    }
}
