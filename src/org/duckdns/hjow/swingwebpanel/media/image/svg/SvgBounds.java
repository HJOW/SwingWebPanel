package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

/**
 * Bounding box computation for SVG elements (plan item P18-04): the tight bounds of a Java2D shape (curve control points do not count) and
 * the object bounding box of an element, the union of the geometry of its graphics (fill area without stroke) in the coordinate system of
 * the element. Paint servers, clip paths, masks and filters with object bounding box units need it.
 * <p>
 * SVG 요소의 경계 상자 계산(계획 P18-04): Java2D 도형의 정확한 경계(곡선의 제어점은 포함하지 않음)와 요소의 객체 경계 상자(요소의 좌표계에서
 * 선을 뺀 채우기 영역의 기하 합집합). 객체 경계 상자 단위를 쓰는 페인트 서버, 클립 경로, 마스크, 필터가 필요로 한다.
 */
final class SvgBounds {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgBounds() {
    }

    /**
     * A growing rectangle.
     * <p>
     * 커지는 사각형.
     */
    static final class Acc {

        /**
         * Smallest x coordinate seen so far.
         * <p>
         * 지금까지 본 가장 작은 x 좌표.
         */
        double minX = Double.POSITIVE_INFINITY;

        /**
         * Smallest y coordinate seen so far.
         * <p>
         * 지금까지 본 가장 작은 y 좌표.
         */
        double minY = Double.POSITIVE_INFINITY;

        /**
         * Largest x coordinate seen so far.
         * <p>
         * 지금까지 본 가장 큰 x 좌표.
         */
        double maxX = Double.NEGATIVE_INFINITY;

        /**
         * Largest y coordinate seen so far.
         * <p>
         * 지금까지 본 가장 큰 y 좌표.
         */
        double maxY = Double.NEGATIVE_INFINITY;

        /**
         * Adds a point.
         * <p>
         * 점을 더한다.
         *
         * @param x x / x
         * @param y y / y
         */
        void add(double x, double y) {
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        /**
         * Adds the flattened outline of a shape (transformed by a matrix).
         * <p>
         * 도형의 평탄화한 윤곽을 (행렬로 변환해) 더한다.
         *
         * @param s the shape / 도형
         * @param m the transform or null / 변환 또는 null
         */
        void addShape(Shape s, AffineTransform m) {
            PathIterator it = s.getPathIterator(m, 0.02);
            double[] c = new double[6];
            // 평탄화한 경로의 모든 점을 더한다.
            while (!it.isDone()) {
                int t = it.currentSegment(c);
                if (t != PathIterator.SEG_CLOSE) {
                    add(c[0], c[1]);
                }
                it.next();
            }
        }

        /**
         * Adds a rectangle (transformed by a matrix).
         * <p>
         * 사각형을 (행렬로 변환해) 더한다.
         *
         * @param r the rectangle / 사각형
         * @param m the transform or null / 변환 또는 null
         */
        void addRect(Rectangle2D r, AffineTransform m) {
            if (m == null) {
                add(r.getMinX(), r.getMinY());
                add(r.getMaxX(), r.getMaxY());
                return;
            }
            double[] p = { r.getMinX(), r.getMinY(), r.getMaxX(), r.getMinY(), r.getMaxX(), r.getMaxY(), r.getMinX(), r.getMaxY() };
            m.transform(p, 0, p, 0, 4);
            // 변환된 네 모서리를 더한다.
            for (int i = 0; i < 8; i += 2) {
                add(p[i], p[i + 1]);
            }
        }

        /**
         * Returns the rectangle or null if nothing was added or a value is not finite.
         * <p>
         * 사각형을 반환한다. 더한 것이 없거나 값이 유한하지 않으면 null.
         *
         * @return the rectangle or null / 사각형 또는 null
         */
        Rectangle2D result() {
            if (minX > maxX || minY > maxY || Double.isNaN(minX + minY + maxX + maxY) || Double.isInfinite(minX + minY + maxX + maxY)) {
                return null;
            }
            return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        }
    }

    /**
     * Returns the tight bounds of a shape, or null for an empty or non-finite shape.
     * <p>
     * 도형의 정확한 경계를 반환한다. 비었거나 유한하지 않은 도형이면 null.
     *
     * @param s the shape / 도형
     * @return the bounds or null / 경계 또는 null
     */
    static Rectangle2D shapeBounds(Shape s) {
        if (s instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) s;
            return new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }
        Acc a = new Acc();
        a.addShape(s, null);
        return a.result();
    }

    /**
     * Computes the object bounding box of an element in its own coordinate system (its own transform is not applied).
     * <p>
     * 요소의 객체 경계 상자를 요소 자신의 좌표계에서 계산한다(요소 자신의 변환은 적용하지 않는다).
     *
     * @param r  the renderer / 렌더러
     * @param e  the element / 요소
     * @param st the style of the element / 요소의 스타일
     * @return the box or null if the element has no geometry / 상자. 기하가 없으면 null
     */
    static Rectangle2D bbox(SvgRenderer r, SvgElement e, SvgStyle st) {
        Acc a = new Acc();
        accumulate(r, e, st, new AffineTransform(), a, 0);
        return a.result();
    }

    /**
     * Adds the geometry of an element and its descendants to an accumulator.
     * <p>
     * 요소와 자손의 기하를 누적기에 더한다.
     *
     * @param r     the renderer / 렌더러
     * @param e     the element / 요소
     * @param st    the style of the element / 요소의 스타일
     * @param m     the transform from the element's coordinates to the target space / 요소 좌표에서 대상 공간으로의 변환
     * @param a     the accumulator / 누적기
     * @param depth the recursion depth / 재귀 깊이
     */
    private static void accumulate(SvgRenderer r, SvgElement e, SvgStyle st, AffineTransform m, Acc a, int depth) {
        if (depth > SvgRenderer.MAX_REFERENCE_DEPTH * 4) {
            return;
        }
        r.tick();
        String n = e.name;
        if ("g".equals(n) || "a".equals(n) || "switch".equals(n) || "svg".equals(n) || "symbol".equals(n)) {
            AffineTransform inner = m;
            if (depth > 0 && ("svg".equals(n) || "symbol".equals(n))) {
                inner = viewportMatrix(r, e, st, m);
            }
            // 자식 중 그려지는 요소를 변환과 함께 누적한다.
            for (int i = 0; i < e.children.size(); i++) {
                SvgElement c = e.children.get(i);
                if (!c.isSvg()) {
                    continue;
                }
                if ("switch".equals(n) && !r.conditionsPass(c)) {
                    continue;
                }
                addChild(r, c, st, inner, a, depth);
                if ("switch".equals(n)) {
                    break;
                }
            }
        } else if ("use".equals(n)) {
            SvgElement target = r.lookup(e.href());
            if (target == null || r.active.contains(target)) {
                return;
            }
            double x = SvgRenderer.lengthAttr(e, "x", st, 0, 0);
            double y = SvgRenderer.lengthAttr(e, "y", st, 0, 1);
            AffineTransform mm = new AffineTransform(m);
            mm.translate(x, y);
            r.active.add(target);
            try {
                if (target.is("symbol") || target.is("svg")) {
                    SvgStyle ts = new SvgStyle(st, target, r.vpW, r.vpH);
                    accumulate(r, target, ts, viewportMatrix(r, target, ts, mm), a, depth + 1);
                } else {
                    addChild(r, target, st, mm, a, depth + 1);
                }
            } finally {
                r.active.remove(target);
            }
        } else if ("text".equals(n)) {
            Rectangle2D tb = SvgText.bounds(r, e, st);
            if (tb != null) {
                a.addRect(tb, m);
            }
        } else if ("image".equals(n)) {
            double x = SvgRenderer.lengthAttr(e, "x", st, 0, 0);
            double y = SvgRenderer.lengthAttr(e, "y", st, 0, 1);
            double w = SvgRenderer.lengthAttr(e, "width", st, 0, 0);
            double h = SvgRenderer.lengthAttr(e, "height", st, 0, 1);
            if (w > 0 && h > 0) {
                a.addRect(new Rectangle2D.Double(x, y, w, h), m);
            }
        } else if ("path".equals(n)) {
            a.addShape(r.pathOf(e, st).shape, m);
        } else {
            Shape s = r.basicShape(e, st);
            if (s != null) {
                a.addShape(s, m);
            }
        }
    }

    /**
     * Adds a child element with its own style and transform.
     * <p>
     * 자식 요소를 그 스타일과 변환과 함께 더한다.
     *
     * @param r     the renderer / 렌더러
     * @param c     the child / 자식
     * @param st    the parent style / 부모 스타일
     * @param m     the transform of the parent space / 부모 공간의 변환
     * @param a     the accumulator / 누적기
     * @param depth the recursion depth / 재귀 깊이
     */
    private static void addChild(SvgRenderer r, SvgElement c, SvgStyle st, AffineTransform m, Acc a, int depth) {
        if (!SvgRenderer.isGraphicalName(c.name)) {
            return;
        }
        SvgStyle cs = new SvgStyle(st, c, r.vpW, r.vpH);
        if (!cs.displayed()) {
            return;
        }
        AffineTransform t = r.transformOf(c, cs);
        if (t == null) {
            return;
        }
        AffineTransform mm = new AffineTransform(m);
        mm.concatenate(t);
        accumulate(r, c, cs, mm, a, depth + 1);
    }

    /**
     * Returns the matrix of a nested viewport (translation to x, y and the view box fit) appended to a base matrix.
     * <p>
     * 중첩 뷰포트의 행렬(x, y 이동과 viewBox 맞춤)을 기준 행렬 뒤에 붙여 반환한다.
     *
     * @param r  the renderer / 렌더러
     * @param e  the svg or symbol element / svg 또는 symbol 요소
     * @param st its style / 그 스타일
     * @param m  the base matrix / 기준 행렬
     * @return the combined matrix / 합친 행렬
     */
    private static AffineTransform viewportMatrix(SvgRenderer r, SvgElement e, SvgStyle st, AffineTransform m) {
        AffineTransform mm = new AffineTransform(m);
        double x = SvgRenderer.lengthAttr(e, "x", st, 0, 0);
        double y = SvgRenderer.lengthAttr(e, "y", st, 0, 1);
        mm.translate(x, y);
        double[] vb = SvgImage.parseViewBox(e.attr("viewBox"));
        if (vb != null) {
            double w = SvgRenderer.lengthAttr(e, "width", st, r.vpW, 0);
            double h = SvgRenderer.lengthAttr(e, "height", st, r.vpH, 1);
            mm.concatenate(SvgRenderer.viewBoxTransform(vb[0], vb[1], vb[2], vb[3], e.attr("preserveAspectRatio"), w, h));
        }
        return mm;
    }
}
