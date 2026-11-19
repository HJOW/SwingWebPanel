package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Shape;
import java.awt.geom.Path2D;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.media.image.svg.PathShapes;

/**
 * The equivalent paths of the SVG shape elements of a document for <code>offset-path: url(#id)</code> (CSS Motion Path 1 section 2.1, SVG 2 section 10, the
 * equivalent path of each shape): <code>path</code> (its <code>d</code>), <code>rect</code> (from the left end of the top edge after the corner, clockwise),
 * <code>circle</code> and <code>ellipse</code> (from the rightmost point, clockwise), <code>line</code>, <code>polyline</code> and <code>polygon</code>. The
 * coordinates are user units of 1px whose origin is the top-left corner of the <code>&lt;coord-box&gt;</code>, and percentages are relative to that box (the
 * viewport). A reference that is not a shape element of the document behaves as <code>path("m 0 0")</code>: a path of one point at the origin.
 * <p>
 * 문서의 SVG 도형 요소의 동등한 경로를 <code>offset-path: url(#id)</code> 를 위해 만든다(CSS Motion Path 1 2.1 절, SVG 2 10 절의 도형별 동등한 경로): <code>path</code>(그
 * <code>d</code>), <code>rect</code>(윗변에서 모서리 뒤 왼쪽 끝부터 시계 방향), <code>circle</code> 과 <code>ellipse</code>(가장 오른쪽 점부터 시계 방향), <code>line</code>,
 * <code>polyline</code>, <code>polygon</code>. 좌표는 원점이 <code>&lt;coord-box&gt;</code> 의 왼쪽 위인 1px 의 사용자 단위이고 백분율은 그 상자(뷰포트) 기준이다. 문서의 도형 요소가
 * 아닌 참조는 <code>path("m 0 0")</code>(원점의 점 하나인 경로)처럼 동작한다.
 */
final class SvgShapePaths {

    /**
     * Not instantiable; the class only offers static methods.
     * <p>
     * 인스턴스를 만들지 않는다. 정적 메서드만 제공한다.
     */
    private SvgShapePaths() {
    }

    /**
     * Returns the path of a reference.
     * <p>
     * 참조의 경로를 반환한다.
     *
     * @param node the node of the box that refers (for its document) / 참조하는 상자의 노드(문서용)
     * @param url  the URL as written (<code>#id</code>) / 쓴 그대로의 URL(<code>#id</code>)
     * @param ref  the coordinate box {x, y, width, height} / 좌표 상자 {x, y, 너비, 높이}
     * @return the path in document coordinates / 문서 좌표의 경로
     */
    static Shape of(Node node, String url, double[] ref) {
        Shape s = shape(node, url, ref[2], ref[3]);
        Path2D.Double out = new Path2D.Double();
        if (s == null) {
            out.moveTo(ref[0], ref[1]);
            return out;
        }
        out.append(s.getPathIterator(java.awt.geom.AffineTransform.getTranslateInstance(ref[0], ref[1])), false);
        return out;
    }

    /**
     * Returns the equivalent path of the referenced shape element in its user space, or null if the reference is not a shape element.
     * <p>
     * 참조한 도형 요소의 동등한 경로를 사용자 공간에서 반환한다. 도형 요소가 아니면 null.
     *
     * @param node the node / 노드
     * @param url  the URL / URL
     * @param vw   the width of the viewport / 뷰포트 너비
     * @param vh   the height of the viewport / 뷰포트 높이
     * @return the path or null / 경로 또는 null
     */
    private static Shape shape(Node node, String url, double vw, double vh) {
        if (node == null || url == null || !url.startsWith("#") || url.length() < 2) {
            return null;
        }
        Document doc = node instanceof Document ? (Document) node : node.getOwnerDocument();
        Element e = doc == null ? null : doc.getElementById(url.substring(1));
        if (e == null || !Namespaces.SVG.equals(e.getNamespaceURI())) {
            return null;
        }
        String n = e.getLocalName();
        double diag = Math.sqrt((vw * vw + vh * vh) / 2);
        if ("path".equals(n)) {
            String d = e.getAttribute("d");
            return d == null ? null : PathShapes.parse(d, false);
        }
        if ("rect".equals(n)) {
            double x = num(e, "x", vw);
            double y = num(e, "y", vh);
            double w = num(e, "width", vw);
            double h = num(e, "height", vh);
            if (w <= 0 || h <= 0) {
                return null;
            }
            double rx = e.hasAttribute("rx") ? num(e, "rx", vw) : Double.NaN;
            double ry = e.hasAttribute("ry") ? num(e, "ry", vh) : Double.NaN;
            // rx 와 ry 중 하나만 있으면 다른 하나도 같은 값이고, 반지름은 변의 절반을 넘지 않는다(SVG 2 10.2 절).
            if (Double.isNaN(rx)) {
                rx = Double.isNaN(ry) ? 0 : ry;
            }
            if (Double.isNaN(ry)) {
                ry = rx;
            }
            rx = Math.max(0, Math.min(rx, w / 2));
            ry = Math.max(0, Math.min(ry, h / 2));
            return ClipPaths.roundedRect(x, y, w, h, new double[] {rx, ry, rx, ry, rx, ry, rx, ry});
        }
        if ("circle".equals(n) || "ellipse".equals(n)) {
            double cx = num(e, "cx", vw);
            double cy = num(e, "cy", vh);
            double rx = "circle".equals(n) ? num(e, "r", diag) : num(e, "rx", vw);
            double ry = "circle".equals(n) ? rx : num(e, "ry", vh);
            if (rx <= 0 || ry <= 0) {
                return null;
            }
            return ellipse(cx, cy, rx, ry);
        }
        if ("line".equals(n)) {
            Path2D.Double p = new Path2D.Double();
            p.moveTo(num(e, "x1", vw), num(e, "y1", vh));
            p.lineTo(num(e, "x2", vw), num(e, "y2", vh));
            return p;
        }
        if ("polyline".equals(n) || "polygon".equals(n)) {
            String pts = e.getAttribute("points");
            String[] t = pts == null ? new String[0] : pts.trim().split("[\\s,]+");
            if (t.length < 4) {
                return null;
            }
            Path2D.Double p = new Path2D.Double();
            // 좌표를 둘씩 읽어 잇는다(홀수 개이면 마지막 하나는 버린다).
            for (int i = 0; i + 1 < t.length; i += 2) {
                double x = parse(t[i]);
                double y = parse(t[i + 1]);
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    return null;
                }
                if (i == 0) {
                    p.moveTo(x, y);
                } else {
                    p.lineTo(x, y);
                }
            }
            if ("polygon".equals(n)) {
                p.closePath();
            }
            return p;
        }
        return null;
    }

    /**
     * Makes the outline of an ellipse that starts at the rightmost point and runs clockwise in four quarter arcs (SVG 2 section 10.4).
     * <p>
     * 가장 오른쪽 점에서 시작해 시계 방향으로 사분원 호 넷을 도는 타원의 윤곽을 만든다(SVG 2 10.4 절).
     *
     * @param cx the center x / 중심 x
     * @param cy the center y / 중심 y
     * @param rx the horizontal radius / 가로 반지름
     * @param ry the vertical radius / 세로 반지름
     * @return the path / 경로
     */
    private static Shape ellipse(double cx, double cy, double rx, double ry) {
        double k = 0.5522847498307936;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(cx + rx, cy);
        p.curveTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry);
        p.curveTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy);
        p.curveTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry);
        p.curveTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy);
        p.closePath();
        return p;
    }

    /**
     * Reads a coordinate or a length attribute: a number of user units (an optional <code>px</code>) or a percentage of a base; a missing attribute is 0.
     * <p>
     * 좌표나 길이 속성을 읽는다: 사용자 단위의 수(<code>px</code> 는 붙어도 됨)나 기준의 백분율. 없는 속성은 0.
     *
     * @param e    the element / 요소
     * @param name the attribute / 속성
     * @param base the base of a percentage / 백분율의 기준
     * @return the value, 0 if it cannot be read / 값. 읽을 수 없으면 0
     */
    private static double num(Element e, String name, double base) {
        String v = e.getAttribute(name);
        if (v == null) {
            return 0;
        }
        v = v.trim();
        if (v.endsWith("%")) {
            double p = parse(v.substring(0, v.length() - 1));
            return Double.isNaN(p) ? 0 : p * base / 100.0;
        }
        if (v.endsWith("px")) {
            v = v.substring(0, v.length() - 2);
        }
        double d = parse(v);
        return Double.isNaN(d) ? 0 : d;
    }

    /**
     * Parses a number.
     * <p>
     * 수를 파싱한다.
     *
     * @param s the text / 텍스트
     * @return the number or NaN / 수 또는 NaN
     */
    private static double parse(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
