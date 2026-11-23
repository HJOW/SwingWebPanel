package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Markers on the vertices of paths, lines, polylines and polygons (plan item P18-04): <code>marker-start</code>, <code>marker-mid</code>
 * and <code>marker-end</code> reference a <code>marker</code> element whose content is drawn at the vertices, with
 * <code>orient</code> (an angle, <code>auto</code>, <code>auto-start-reverse</code>), <code>markerUnits</code>, <code>markerWidth</code>,
 * <code>markerHeight</code>, <code>refX</code>, <code>refY</code>, <code>viewBox</code> and the overflow clip. The direction at a vertex
 * is the bisector of the incoming and outgoing directions of the segments. The marker content inherits from the place where the marker is
 * defined; <code>context-fill</code> and <code>context-stroke</code> refer to the paint of the marked element.
 * <p>
 * 경로, 선분, 꺾은선, 다각형의 꼭짓점에 붙는 마커(계획 P18-04): <code>marker-start</code>, <code>marker-mid</code>, <code>marker-end</code> 가
 * <code>marker</code> 요소를 참조하며 그 내용을 꼭짓점에 그린다. <code>orient</code>(각도, <code>auto</code>, <code>auto-start-reverse</code>),
 * <code>markerUnits</code>, <code>markerWidth</code>, <code>markerHeight</code>, <code>refX</code>, <code>refY</code>, <code>viewBox</code>,
 * overflow 자르기를 지원한다. 꼭짓점의 방향은 들어오는 구간과 나가는 구간 방향의 이등분선이다. 마커 내용은 마커가 정의된 위치에서 상속하며
 * <code>context-fill</code> 과 <code>context-stroke</code> 는 마커가 붙은 요소의 칠을 가리킨다.
 */
final class SvgMarkers {

    /**
     * One vertex of the path with its marker direction.
     * <p>
     * 마커 방향을 가진 경로의 꼭짓점 하나.
     */
    private static final class Vertex {

        /**
         * The x coordinate.
         * <p>
         * x 좌표.
         */
        final double x;

        /**
         * The y coordinate.
         * <p>
         * y 좌표.
         */
        final double y;

        /**
         * The direction angle in radians.
         * <p>
         * 방향 각도(라디안).
         */
        final double angle;

        /**
         * Creates a vertex.
         * <p>
         * 꼭짓점을 만든다.
         *
         * @param x     x / x
         * @param y     y / y
         * @param angle angle / 각도
         */
        Vertex(double x, double y, double angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgMarkers() {
    }

    /**
     * Draws the markers of a geometry element.
     * <p>
     * 기하 요소의 마커를 그린다.
     *
     * @param r    the renderer / 렌더러
     * @param e    the element / 요소
     * @param st   its style / 스타일
     * @param path the path with the original segments / 원래 구간을 가진 경로
     */
    static void draw(SvgRenderer r, SvgElement e, SvgStyle st, SvgPath path) {
        SvgElement ms = markerOf(r, st, "marker-start");
        SvgElement mm = markerOf(r, st, "marker-mid");
        SvgElement me = markerOf(r, st, "marker-end");
        if (ms == null && mm == null && me == null) {
            return;
        }
        List<Vertex> v = vertices(path);
        if (v.isEmpty()) {
            return;
        }
        SvgPaint savedFill = r.contextFill;
        SvgPaint savedStroke = r.contextStroke;
        r.contextFill = st.paint("fill");
        r.contextStroke = st.paint("stroke");
        try {
            SvgLength swl = st.length("stroke-width");
            double sw = swl == null ? 1 : st.px(swl, 2);
            // 첫 꼭짓점은 start, 마지막은 end, 나머지는 mid 마커를 쓴다.
            for (int i = 0; i < v.size(); i++) {
                Vertex vx = v.get(i);
                if (i == 0 && ms != null) {
                    drawMarker(r, ms, vx, sw, true);
                }
                if (i > 0 && i < v.size() - 1 && mm != null) {
                    drawMarker(r, mm, vx, sw, false);
                }
                if (i == v.size() - 1 && me != null) {
                    drawMarker(r, me, vx, sw, false);
                }
            }
        } finally {
            r.contextFill = savedFill;
            r.contextStroke = savedStroke;
        }
    }

    /**
     * Resolves a marker property to a <code>marker</code> element.
     * <p>
     * 마커 속성을 <code>marker</code> 요소로 해석한다.
     *
     * @param r    the renderer / 렌더러
     * @param st   the style / 스타일
     * @param prop the property name / 속성 이름
     * @return the marker element or null / marker 요소 또는 null
     */
    private static SvgElement markerOf(SvgRenderer r, SvgStyle st, String prop) {
        String v = st.get(prop);
        if (v == null) {
            return null;
        }
        String id = SvgStyle.urlId(v.trim());
        if (id == null) {
            return null;
        }
        SvgElement m = r.doc.ids.get(id);
        return m != null && m.is("marker") ? m : null;
    }

    /**
     * Computes the vertices of a path with the direction of the marker at each.
     * <p>
     * 경로의 꼭짓점과 각 꼭짓점의 마커 방향을 계산한다.
     *
     * @param path the path / 경로
     * @return the vertices / 꼭짓점
     */
    private static List<Vertex> vertices(SvgPath path) {
        List<SvgPath.Seg> segs = path.segs;
        List<Vertex> out = new ArrayList<Vertex>();
        int n = segs.size();
        // 구간마다 꼭짓점 하나를 만든다. 들어오는 방향은 현재 구간의 끝, 나가는 방향은 다음 구간의 시작이다.
        for (int i = 0; i < n; i++) {
            SvgPath.Seg s = segs.get(i);
            boolean hasIn = false;
            double inX = 0;
            double inY = 0;
            boolean hasOut = false;
            double outX = 0;
            double outY = 0;
            if (s.kind != SvgPath.MOVE) {
                hasIn = true;
                inX = s.endDx;
                inY = s.endDy;
            }
            if (i + 1 < n && segs.get(i + 1).kind != SvgPath.MOVE) {
                hasOut = true;
                outX = segs.get(i + 1).startDx;
                outY = segs.get(i + 1).startDy;
            }
            if (s.kind == SvgPath.MOVE) {
                // 닫힌 부분 경로의 시작점은 닫는 구간의 도착 방향도 고려한다.
                int close = findClose(segs, i);
                if (close >= 0) {
                    hasIn = true;
                    inX = segs.get(close).endDx;
                    inY = segs.get(close).endDy;
                }
            } else if (s.kind == SvgPath.CLOSE) {
                // 닫는 구간의 끝은 부분 경로 시작점이므로 첫 구간의 시작 방향을 쓴다.
                int start = findStart(segs, i);
                if (start >= 0 && start + 1 < n && segs.get(start + 1).kind == SvgPath.DRAW) {
                    hasOut = true;
                    outX = segs.get(start + 1).startDx;
                    outY = segs.get(start + 1).startDy;
                }
            }
            out.add(new Vertex(s.x, s.y, bisect(hasIn, inX, inY, hasOut, outX, outY)));
        }
        return out;
    }

    /**
     * Finds the closing segment of the subpath that starts at a move segment.
     * <p>
     * 이동 구간에서 시작하는 부분 경로의 닫는 구간을 찾는다.
     *
     * @param segs the segments / 구간
     * @param move the index of the move segment / 이동 구간의 위치
     * @return the index of the close segment or -1 if the subpath is open / 닫는 구간의 위치. 열린 경로면 -1
     */
    private static int findClose(List<SvgPath.Seg> segs, int move) {
        // 다음 이동 구간이 나오기 전에 닫는 구간이 있는지 찾는다.
        for (int i = move + 1; i < segs.size(); i++) {
            int k = segs.get(i).kind;
            if (k == SvgPath.CLOSE) {
                return i;
            }
            if (k == SvgPath.MOVE) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Finds the move segment that starts the subpath of a close segment.
     * <p>
     * 닫는 구간이 속한 부분 경로를 시작하는 이동 구간을 찾는다.
     *
     * @param segs  the segments / 구간
     * @param close the index of the close segment / 닫는 구간의 위치
     * @return the index of the move segment or -1 / 이동 구간의 위치. 없으면 -1
     */
    private static int findStart(List<SvgPath.Seg> segs, int close) {
        // 닫는 구간에서 거꾸로 올라가며 이동 구간을 찾는다.
        for (int i = close - 1; i >= 0; i--) {
            if (segs.get(i).kind == SvgPath.MOVE) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Computes the bisector angle of an incoming and an outgoing direction.
     * <p>
     * 들어오는 방향과 나가는 방향의 이등분선 각도를 계산한다.
     *
     * @param hasIn  true if the incoming direction exists / 들어오는 방향이 있으면 true
     * @param inX    incoming x / 들어오는 x
     * @param inY    incoming y / 들어오는 y
     * @param hasOut true if the outgoing direction exists / 나가는 방향이 있으면 true
     * @param outX   outgoing x / 나가는 x
     * @param outY   outgoing y / 나가는 y
     * @return the angle in radians / 각도(라디안)
     */
    private static double bisect(boolean hasIn, double inX, double inY, boolean hasOut, double outX, double outY) {
        if (hasIn && hasOut) {
            double a1 = Math.atan2(inY, inX);
            double a2 = Math.atan2(outY, outX);
            double d = a2 - a1;
            // 두 각도 차이가 반 바퀴를 넘으면 반대쪽으로 돌아 이등분한다.
            if (d > Math.PI) {
                a1 += 2 * Math.PI;
            } else if (d < -Math.PI) {
                a2 += 2 * Math.PI;
            }
            return (a1 + a2) / 2;
        }
        if (hasIn) {
            return Math.atan2(inY, inX);
        }
        if (hasOut) {
            return Math.atan2(outY, outX);
        }
        return 0;
    }

    /**
     * Draws one marker at a vertex.
     * <p>
     * 꼭짓점 하나에 마커를 그린다.
     *
     * @param r       the renderer / 렌더러
     * @param marker  the marker element / marker 요소
     * @param v       the vertex / 꼭짓점
     * @param sw      the stroke width of the marked element / 마커가 붙은 요소의 선 너비
     * @param isStart true for the start marker (for <code>auto-start-reverse</code>) / 시작 마커이면 true(<code>auto-start-reverse</code> 용)
     */
    private static void drawMarker(SvgRenderer r, SvgElement marker, Vertex v, double sw, boolean isStart) {
        if (r.active.contains(marker) || r.active.size() > SvgRenderer.MAX_REFERENCE_DEPTH) {
            return;
        }
        SvgStyle ms = r.styleFor(marker);
        double mw = SvgRenderer.lengthAttr(marker, "markerWidth", ms, 3, 0);
        double mh = SvgRenderer.lengthAttr(marker, "markerHeight", ms, 3, 1);
        if (!(mw > 0) || !(mh > 0)) {
            return;
        }
        boolean strokeUnits = !"userSpaceOnUse".equals(marker.attrs.get("markerUnits"));
        double refX = SvgRenderer.lengthAttr(marker, "refX", ms, 0, 0);
        double refY = SvgRenderer.lengthAttr(marker, "refY", ms, 0, 1);
        double[] vb = SvgImage.parseViewBox(marker.attr("viewBox"));
        AffineTransform vbT = new AffineTransform();
        double cw = mw;
        double ch = mh;
        if (vb != null) {
            vbT = SvgRenderer.viewBoxTransform(vb[0], vb[1], vb[2], vb[3], marker.attr("preserveAspectRatio"), mw, mh);
            cw = vb[2];
            ch = vb[3];
        }
        double[] ref = { refX, refY };
        vbT.transform(ref, 0, ref, 0, 1);
        double angle = 0;
        String orient = marker.attrs.get("orient");
        if (orient != null) {
            String o = orient.trim().toLowerCase(Locale.ENGLISH);
            if ("auto".equals(o)) {
                angle = v.angle;
            } else if ("auto-start-reverse".equals(o)) {
                angle = isStart ? v.angle + Math.PI : v.angle;
            } else {
                SvgScanner sc = new SvgScanner(o);
                if (sc.number()) {
                    double deg = SvgTransforms.angleToDegrees(sc.value, sc.letters());
                    angle = Double.isNaN(deg) ? 0 : Math.toRadians(deg);
                }
            }
        }
        AffineTransform saved = r.g.getTransform();
        double svW = r.vpW;
        double svH = r.vpH;
        r.active.add(marker);
        SvgRenderer.Layer layer = null;
        boolean ok = false;
        try {
            r.g.translate(v.x, v.y);
            r.g.rotate(angle);
            if (strokeUnits) {
                r.g.scale(sw, sw);
            }
            r.g.translate(-ref[0], -ref[1]);
            String overflow = ms.own("overflow");
            boolean clip = overflow == null || !(overflow.equalsIgnoreCase("visible") || overflow.equalsIgnoreCase("auto"));
            Rectangle2D clipRect = new Rectangle2D.Double(0, 0, mw, mh);
            if (clip && !r.coversCanvas(clipRect)) {
                layer = r.pushLayer();
            }
            r.g.transform(vbT);
            r.vpW = cw;
            r.vpH = ch;
            r.renderChildren(marker, ms);
            ok = true;
            if (layer != null) {
                SvgMasks.clipToShape(r, layer.img, clipRect, layer.ctm);
            }
        } finally {
            r.vpW = svW;
            r.vpH = svH;
            r.active.remove(marker);
            // 레이어를 먼저 끝내 부모 그래픽스로 돌아온 뒤에 변환을 복원해야 한다.
            if (layer != null) {
                if (ok) {
                    r.compositeLayer(layer, 1, null);
                } else {
                    r.endLayer(layer);
                }
            }
            r.g.setTransform(saved);
        }
    }
}
