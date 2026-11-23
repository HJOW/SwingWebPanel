package org.duckdns.hjow.swingwebpanel.render;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;

/**
 * Applies <code>backdrop-filter</code> (CSS Filter Effects Level 2, <a href="https://drafts.fxtf.org/filter-effects-2/#BackdropFilterProperty">https://drafts.fxtf.org/filter-effects-2/#BackdropFilterProperty</a>,
 * editor's draft): the filter functions of the property are applied to what has been painted behind the element (the image that the paint pass has built so
 * far inside the nearest group that has its own buffer, which stands for the backdrop root), and the result replaces that content inside the border box
 * of the element, with its rounded corners. The element is painted on top afterwards. The area outside the box is not read: the image that the filter
 * gets is the part inside the box, extended beyond its edges by mirroring (the edge mode of the specification), so that a blur does not fade to
 * transparent at the edges. The pixel work is done by {@link FilterRenderer}, the same code as for <code>filter</code>.
 * <p>
 * <code>backdrop-filter</code> 를 적용한다(CSS Filter Effects Level 2, 편집자 초안): 속성의 필터 함수를 요소 뒤에 이미 그려진 내용(백드롭 루트에 해당하는 자신의 버퍼를 가진 가장
 * 가까운 그룹 안에서 지금까지 만든 이미지)에 적용하고, 그 결과를 요소의 둥근 모서리가 있는 border box 안의 내용으로 바꾼다. 요소는 그 위에 나중에 그린다. 상자 밖의 영역은
 * 읽지 않는다: 필터가 받는 이미지는 상자 안의 부분이며 가장자리 밖은 거울처럼 이어 붙여(규격의 edge mode) 흐림이 가장자리에서 투명으로 옅어지지 않게 한다. 화소 처리는
 * <code>filter</code> 와 같은 {@link FilterRenderer} 가 한다.
 */
final class BackdropFilters {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BackdropFilters() {
    }

    /**
     * Filters the backdrop of an element. Nothing happens if the context does not know the image that is painted on, or if the box is outside of it.
     * <p>
     * 요소의 배경을 필터링한다. 컨텍스트가 그리는 이미지를 모르거나 상자가 이미지 밖이면 아무 일도 하지 않는다.
     *
     * @param ctx    the paint context (its transform maps the document to the image) / 그리기 컨텍스트(변환이 문서를 이미지에 대응시킨다)
     * @param spec   the filter functions / 필터 함수
     * @param border the border box of the element in document coordinates / 요소의 border box(문서 좌표)
     */
    static void apply(PaintContext ctx, FilterSpec spec, Shape border) {
        BufferedImage surface = ctx.surface;
        if (surface == null || spec == null || border == null) {
            return;
        }
        Graphics2D g = ctx.g;
        AffineTransform at = g.getTransform();
        Shape dev = at.createTransformedShape(border);
        Rectangle2D b = dev.getBounds2D().createIntersection(new Rectangle2D.Double(0, 0, surface.getWidth(), surface.getHeight()));
        int x0 = (int) Math.floor(b.getMinX());
        int y0 = (int) Math.floor(b.getMinY());
        int w = (int) Math.ceil(b.getMaxX()) - x0;
        int h = (int) Math.ceil(b.getMaxY()) - y0;
        if (w <= 0 || h <= 0 || (long) w * h > 16L * 1024 * 1024) {
            return;
        }
        // 흐림이 닿는 거리만큼 거울로 이어 붙인 입력 이미지를 만든다(기기 화소 단위. 변환 배율을 곱한다).
        double[] ext = spec.extent();
        double scale = Math.sqrt(Math.abs(at.getDeterminant()));
        int pad = (int) Math.ceil(Math.max(Math.max(ext[0], ext[2]), Math.max(ext[1], ext[3])) * scale);
        pad = Math.min(pad, 512);
        BufferedImage tmp = mirrored(surface, x0, y0, w, h, pad);
        FilterRenderer.apply(tmp, spec, at);
        BufferedImage result = tmp.getSubimage(pad, pad, w, h);
        Composite old = g.getComposite();
        Shape oldClip = g.getClip();
        g.setTransform(new AffineTransform());
        // 요소의 모양 안에서만 배경을 대체한다(기존 클립은 그대로 교차한다).
        g.clip(dev);
        g.setComposite(AlphaComposite.Src);
        g.drawImage(result, x0, y0, null);
        g.setComposite(old);
        g.setTransform(at);
        g.setClip(oldClip);
    }

    /**
     * Copies a rectangle of an image and extends it on every side by <code>pad</code> pixels, mirroring the content at the edges.
     * <p>
     * 이미지의 사각형을 복사하고 모든 변에서 <code>pad</code> 화소만큼 가장자리의 내용을 거울처럼 이어 붙여 넓힌다.
     *
     * @param src the image / 이미지
     * @param x0  left of the rectangle / 사각형의 왼쪽
     * @param y0  top / 위쪽
     * @param w   width / 너비
     * @param h   height / 높이
     * @param pad the extension / 넓힐 화소 수
     * @return a new image of (w + 2 pad) x (h + 2 pad) pixels / (w + 2 pad) x (h + 2 pad) 화소의 새 이미지
     */
    static BufferedImage mirrored(BufferedImage src, int x0, int y0, int w, int h, int pad) {
        BufferedImage out = new BufferedImage(w + 2 * pad, h + 2 * pad, BufferedImage.TYPE_INT_ARGB);
        int[] row = new int[w];
        // 출력의 행마다 거울 원본 행을 읽어 가로로도 거울 확장해 쓴다.
        for (int y = -pad; y < h + pad; y++) {
            int sy = reflect(y, h);
            src.getRGB(x0, y0 + sy, w, 1, row, 0, w);
            int[] line = new int[w + 2 * pad];
            // 한 행의 화소를 가로로 거울 확장한다.
            for (int x = -pad; x < w + pad; x++) {
                line[x + pad] = row[reflect(x, w)];
            }
            out.setRGB(0, y + pad, w + 2 * pad, 1, line, 0, w + 2 * pad);
        }
        return out;
    }

    /**
     * Maps an index outside <code>[0, n)</code> into it by mirroring at the edges (the edge pixel is repeated once, as in a mirror edge mode).
     * <p>
     * <code>[0, n)</code> 밖의 인덱스를 가장자리에서 거울처럼 접어 그 안으로 옮긴다(가장자리 화소는 한 번 반복된다).
     *
     * @param i the index / 인덱스
     * @param n the size, positive / 크기. 양수
     * @return the index inside the range / 범위 안의 인덱스
     */
    static int reflect(int i, int n) {
        if (n == 1) {
            return 0;
        }
        int period = 2 * n;
        int m = ((i % period) + period) % period;
        return m < n ? m : period - 1 - m;
    }
}
