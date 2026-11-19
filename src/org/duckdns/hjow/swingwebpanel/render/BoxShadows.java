package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>box-shadow</code> of a box (plan item P10-04, CSS Backgrounds and Borders Level 3 section 7.2): every shadow has an
 * offset, a blur radius, a spread distance, a color and may be inset. An outer shadow is the silhouette of the border box grown by
 * the spread, moved by the offset, blurred and clipped to the outside of the border box; an inset shadow is the area of the padding
 * box outside the (contracted and moved) padding box silhouette, blurred and clipped to the padding box. The first shadow is on top.
 * The blur is a Gaussian with the standard deviation of half the blur radius, approximated by three box blurs (the SVG filter method);
 * the blurred silhouette is computed into an image when the display list is built, so painting is a plain image drawing.
 * <p>
 * Documented limits: the blur is computed for at most {@link #MAX_PIXELS} pixels per shadow, a larger shadow is painted without blur;
 * the corners of the spread silhouette of a corner with a zero radius stay square.
 * <p>
 * 상자의 <code>box-shadow</code>(계획 P10-04, CSS Backgrounds and Borders Level 3 7.2 절): 그림자마다 오프셋, 흐림 반지름, 퍼짐 거리, 색이 있고 inset 일 수 있다.
 * 바깥 그림자는 border box 의 실루엣을 퍼짐만큼 키워 오프셋만큼 옮기고 흐린 뒤 border box 바깥으로 자른 것이고, 안쪽 그림자는 padding box 에서 (줄어들고 옮긴)
 * padding box 실루엣 바깥 영역을 흐린 뒤 padding box 로 자른 것이다. 첫 그림자가 가장 위에 있다. 흐림은 흐림 반지름의 절반을 표준 편차로 하는 가우시안이며
 * 세 번의 박스 흐림으로 근사한다(SVG 필터 방식). 흐려진 실루엣은 디스플레이 리스트를 만들 때 이미지로 계산하므로 그리기는 이미지 그리기일 뿐이다.
 * <p>
 * 문서화된 한계: 흐림은 그림자마다 {@link #MAX_PIXELS} 화소까지만 계산하고 더 큰 그림자는 흐림 없이 그린다. 반지름이 0 인 모서리는 퍼짐에서도 각진 채로 남는다.
 */
final class BoxShadows {

    /** Largest number of pixels of a blurred shadow image. 흐려진 그림자 이미지의 최대 화소 수. */
    static final int MAX_PIXELS = 6000000;

    /** One parsed shadow. 해석한 그림자 하나. */
    private static final class Item {
        /** True for an inset shadow. inset 그림자이면 true. */
        boolean inset;
        /** Horizontal offset. 가로 오프셋. */
        double dx;
        /** Vertical offset. 세로 오프셋. */
        double dy;
        /** Blur radius, at least 0. 흐림 반지름(0 이상). */
        double blur;
        /** Spread distance. 퍼짐 거리. */
        double spread;
        /** The color of the shadow as ARGB. 그림자의 색상(ARGB 값). */
        int argb;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BoxShadows() {
    }

    /**
     * Reads the shadows of a style.
     * <p>
     * 스타일의 그림자를 읽는다.
     *
     * @param s the style / 스타일
     * @return the shadows in the order of the declaration (the first is on top) / 선언 순서의 그림자(첫 그림자가 가장 위)
     */
    private static List<Item> read(ComputedStyle s) {
        List<Item> list = new ArrayList<Item>();
        CssValue v = s.get(Prop.BOX_SHADOW);
        if (v == null || v.isKeyword("none")) {
            return list;
        }
        List<CssValue> layers = BackgroundLayers.layers(v);
        int currentColor = s.color(Prop.COLOR);
        // 그림자마다 색, inset, 길이를 토큰에서 읽는다.
        for (int i = 0; i < layers.size(); i++) {
            Item it = new Item();
            it.argb = currentColor;
            List<Double> nums = new ArrayList<Double>();
            flatten(layers.get(i), it, nums, currentColor);
            if (nums.size() < 2) {
                continue;
            }
            it.dx = nums.get(0).doubleValue();
            it.dy = nums.get(1).doubleValue();
            it.blur = nums.size() > 2 ? Math.max(0, nums.get(2).doubleValue()) : 0;
            it.spread = nums.size() > 3 ? nums.get(3).doubleValue() : 0;
            list.add(it);
        }
        return list;
    }

    /**
     * Collects the lengths, the color and the inset keyword of a shadow value (nested lists are flattened).
     * <p>
     * 그림자 값의 길이, 색, inset 키워드를 모은다(중첩 목록은 풀어 낸다).
     *
     * @param v            the value / 값
     * @param it           receives the color and the inset flag / 색과 inset 표시를 받는다
     * @param nums         receives the lengths / 길이를 받는다
     * @param currentColor the color of <code>currentcolor</code> / <code>currentcolor</code> 의 색
     */
    private static void flatten(CssValue v, Item it, List<Double> nums, int currentColor) {
        if (v instanceof CssList) {
            // 목록의 항목을 차례로 푼다.
            for (int i = 0; i < ((CssList) v).size(); i++) {
                flatten(((CssList) v).get(i), it, nums, currentColor);
            }
        } else if (v instanceof CssColor) {
            it.argb = ((CssColor) v).getArgb();
        } else if (v instanceof CssKeyword) {
            if (v.isKeyword("inset")) {
                it.inset = true;
            } else if (v.isKeyword("currentcolor")) {
                it.argb = currentColor;
            }
        } else {
            double d = BackgroundLayers.lengthOf(v, 0);
            if (!Double.isNaN(d)) {
                nums.add(Double.valueOf(d));
            }
        }
    }

    /**
     * Returns how far the outer shadows of a style reach beyond the border box: {left, top, right, bottom}, each at least 0.
     * <p>
     * 스타일의 바깥 그림자가 border box 밖으로 얼마나 뻗는지 반환한다: {왼쪽, 위쪽, 오른쪽, 아래쪽}, 각각 0 이상.
     *
     * @param s the style or null / 스타일 또는 null
     * @return the extents, or null if there is no outer shadow / 뻗는 거리. 바깥 그림자가 없으면 null
     */
    static double[] extent(ComputedStyle s) {
        if (s == null) {
            return null;
        }
        List<Item> l = read(s);
        double[] e = null;
        // 바깥 그림자마다 오프셋, 퍼짐, 흐림 번짐(표준 편차의 세 배)으로 뻗는 거리를 구한다.
        for (int i = 0; i < l.size(); i++) {
            Item it = l.get(i);
            if (it.inset || (it.argb >>> 24) == 0) {
                continue;
            }
            double reach = Math.max(0, it.spread) + Math.ceil(it.blur * 1.5);
            if (e == null) {
                e = new double[4];
            }
            e[0] = Math.max(e[0], reach - it.dx);
            e[1] = Math.max(e[1], reach - it.dy);
            e[2] = Math.max(e[2], reach + it.dx);
            e[3] = Math.max(e[3], reach + it.dy);
        }
        return e;
    }

    /**
     * Adds the commands of the outer shadows (painted before the background, the last shadow first).
     * <p>
     * 바깥 그림자의 명령을 더한다(배경보다 먼저 그리며 마지막 그림자부터).
     *
     * @param out    receives the commands / 명령을 받는다
     * @param s      the style / 스타일
     * @param border the rounded border edge of the box / 상자의 둥근 border 가장자리
     * @param node   the DOM node / DOM 노드
     */
    static void addOuter(List<DisplayCommand> out, ComputedStyle s, RoundedBox border, Node node) {
        List<Item> l = read(s);
        // 마지막 그림자부터 그려 첫 그림자가 가장 위에 오게 한다.
        for (int i = l.size() - 1; i >= 0; i--) {
            Item it = l.get(i);
            if (it.inset || (it.argb >>> 24) == 0) {
                continue;
            }
            RoundedBox shape = border.spread(it.spread).translate(it.dx, it.dy);
            Rectangle2D sb = shape.toShape().getBounds2D();
            double m = Math.ceil(it.blur * 1.5) + 2;
            Rectangle2D area = new Rectangle2D.Double(sb.getX() - m, sb.getY() - m, sb.getWidth() + 2 * m, sb.getHeight() + 2 * m);
            Area outside = new Area(area);
            outside.subtract(new Area(border.toShape()));
            out.add(make(area, shape.toShape(), null, it, outside, node));
        }
    }

    /**
     * Adds the commands of the inset shadows (painted after the background, the last shadow first).
     * <p>
     * 안쪽 그림자의 명령을 더한다(배경 뒤에 그리며 마지막 그림자부터).
     *
     * @param out     receives the commands / 명령을 받는다
     * @param s       the style / 스타일
     * @param padding the rounded padding edge of the box / 상자의 둥근 padding 가장자리
     * @param node    the DOM node / DOM 노드
     */
    static void addInset(List<DisplayCommand> out, ComputedStyle s, RoundedBox padding, Node node) {
        List<Item> l = read(s);
        // 마지막 그림자부터 그려 첫 그림자가 가장 위에 오게 한다.
        for (int i = l.size() - 1; i >= 0; i--) {
            Item it = l.get(i);
            if (!it.inset || (it.argb >>> 24) == 0 || padding.w <= 0 || padding.h <= 0) {
                continue;
            }
            RoundedBox hole = padding.spread(-it.spread).translate(it.dx, it.dy);
            double m = Math.ceil(it.blur * 1.5) + 2 + Math.abs(it.dx) + Math.abs(it.dy) + Math.abs(it.spread);
            Rectangle2D pb = padding.toShape().getBounds2D();
            Rectangle2D area = new Rectangle2D.Double(pb.getX() - m, pb.getY() - m, pb.getWidth() + 2 * m, pb.getHeight() + 2 * m);
            Area silhouette = new Area(area);
            silhouette.subtract(new Area(hole.toShape()));
            out.add(make(pb, silhouette, area, it, new Area(padding.toShape()), node));
        }
    }

    /**
     * Creates the command of one shadow: the silhouette is drawn into an image and blurred if there is a blur radius.
     * <p>
     * 그림자 하나의 명령을 만든다: 실루엣을 이미지에 그리고 흐림 반지름이 있으면 흐린다.
     *
     * @param bounds     the area that the command paints / 명령이 그리는 영역
     * @param silhouette the silhouette shape / 실루엣 도형
     * @param canvas     the area of the image for an inset shadow (null: the bounds grown by the blur margin) / 안쪽 그림자의 이미지 영역(null 이면 흐림 여백만큼 키운 경계)
     * @param it         the shadow / 그림자
     * @param clip       the area the shadow is clipped to / 그림자를 자르는 영역
     * @param node       the DOM node / DOM 노드
     * @return the command / 명령
     */
    private static DisplayCommand make(Rectangle2D bounds, Shape silhouette, Rectangle2D canvas, Item it, Shape clip, Node node) {
        double sigma = it.blur / 2;
        if (sigma < 0.3) {
            return new BoxCommands.Shadow(bounds, null, 0, 0, silhouette, it.argb, clip, node);
        }
        double m = Math.ceil(sigma * 3);
        Rectangle2D sb = silhouette.getBounds2D();
        Rectangle2D region = canvas != null ? canvas : new Rectangle2D.Double(sb.getX() - m, sb.getY() - m, sb.getWidth() + 2 * m, sb.getHeight() + 2 * m);
        int ix = (int) Math.floor(region.getX());
        int iy = (int) Math.floor(region.getY());
        int iw = (int) Math.ceil(region.getMaxX()) - ix;
        int ih = (int) Math.ceil(region.getMaxY()) - iy;
        if (iw <= 0 || ih <= 0 || (long) iw * ih > MAX_PIXELS) {
            return new BoxCommands.Shadow(bounds, null, 0, 0, silhouette, it.argb, clip, node);
        }
        BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(-ix, -iy);
            g.setColor(Color.WHITE);
            g.fill(silhouette);
        } finally {
            g.dispose();
        }
        int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int[] alpha = new int[data.length];
        // 실루엣의 알파 채널을 꺼낸다.
        for (int i = 0; i < data.length; i++) {
            alpha[i] = data[i] >>> 24;
        }
        blur(alpha, iw, ih, sigma);
        int rgb = it.argb & 0xFFFFFF;
        int ca = it.argb >>> 24;
        // 흐려진 알파에 그림자 색과 불투명도를 입힌다.
        for (int i = 0; i < data.length; i++) {
            data[i] = ((alpha[i] * ca / 255) << 24) | rgb;
        }
        return new BoxCommands.Shadow(bounds, img, ix, iy, null, 0, clip, node);
    }

    /**
     * Blurs an alpha mask with a Gaussian of the given standard deviation, approximated by three box blurs in each direction.
     * <p>
     * 알파 마스크를 주어진 표준 편차의 가우시안으로 흐린다. 방향마다 박스 흐림 세 번으로 근사한다.
     *
     * @param a     the alpha values (0..255), changed in place / 알파 값(0~255). 제자리에서 바뀐다
     * @param w     width / 너비
     * @param h     height / 높이
     * @param sigma the standard deviation / 표준 편차
     */
    static void blur(int[] a, int w, int h, double sigma) {
        int d = (int) Math.floor(sigma * 3 * Math.sqrt(2 * Math.PI) / 4 + 0.5);
        int r = Math.max(1, (d - 1) / 2);
        int[] tmp = new int[a.length];
        // 가로로 세 번 박스 흐림을 적용한다.
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(a, tmp, w, h, r, true);
            System.arraycopy(tmp, 0, a, 0, a.length);
        }
        // 세로 방향도 세 번 적용한다.
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(a, tmp, w, h, r, false);
            System.arraycopy(tmp, 0, a, 0, a.length);
        }
    }

    /**
     * One box blur pass along rows or columns (the outside counts as zero).
     * <p>
     * 행이나 열을 따라 박스 흐림 한 번(바깥은 0 으로 본다).
     *
     * @param src        the source / 원본
     * @param dst        the destination / 대상
     * @param w          width / 너비
     * @param h          height / 높이
     * @param r          the radius of the box (the box has 2r+1 pixels) / 박스의 반지름(박스는 2r+1 화소)
     * @param horizontal true to blur along rows / 행을 따라 흐리면 true
     */
    private static void boxBlur(int[] src, int[] dst, int w, int h, int r, boolean horizontal) {
        int n = horizontal ? w : h;
        int lines = horizontal ? h : w;
        int div = 2 * r + 1;
        // 줄(행이나 열)마다 슬라이딩 합으로 평균을 구한다.
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line;
            int stride = horizontal ? 1 : w;
            long sum = 0;
            // 위치 0 의 창(-r..r)의 합을 만든다.
            for (int k = 0; k <= r && k < n; k++) {
                sum += src[base + k * stride];
            }
            // 창을 한 칸씩 밀며 평균을 낸다.
            for (int i = 0; i < n; i++) {
                dst[base + i * stride] = (int) (sum / div);
                int add = i + r + 1;
                int rem = i - r;
                if (add < n) {
                    sum += src[base + add * stride];
                }
                if (rem >= 0) {
                    sum -= src[base + rem * stride];
                }
            }
        }
    }
}
