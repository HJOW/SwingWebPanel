package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.geom.Path2D;

/**
 * A public entry to the SVG path data parser for code outside of the SVG decoder: it turns the string of a path (the value of the
 * <code>d</code> attribute of an SVG <code>path</code>, or the string of the CSS <code>path()</code> function of <code>clip-path</code>) into a
 * Java2D shape. The parser keeps what it could read up to the first error, so {@link #parse} reports a path that has no drawing segment at
 * all as invalid (null).
 * <p>
 * SVG 디코더 밖의 코드를 위한 SVG 경로 데이터 파서의 공개 진입점: 경로 문자열(SVG <code>path</code> 의 <code>d</code> 속성 값, 또는 <code>clip-path</code> 의
 * CSS <code>path()</code> 함수의 문자열)을 Java2D 도형으로 바꾼다. 파서는 첫 오류까지 읽은 것을 유지하므로 그리는 구간이 하나도 없는 경로는 {@link #parse} 가 올바르지
 * 않은 것(null)으로 알린다.
 */
public final class PathShapes {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PathShapes() {
    }

    /**
     * Parses path data.
     * <p>
     * 경로 데이터를 파싱한다.
     *
     * @param d      the path data / 경로 데이터
     * @param evenOdd true for the even-odd fill rule, false for non-zero / 짝홀 채움 규칙이면 true, 논제로이면 false
     * @return a new shape, or null if the data has no drawing segment / 새 도형. 그리는 구간이 없으면 null
     */
    public static Path2D parse(String d, boolean evenOdd) {
        SvgPath p = SvgPath.parse(d);
        boolean drawn = false;
        // 그리는 구간이 하나라도 있는지 본다.
        for (int i = 0; i < p.segs.size(); i++) {
            if (p.segs.get(i).kind == SvgPath.DRAW) {
                drawn = true;
                break;
            }
        }
        if (!drawn) {
            return null;
        }
        Path2D.Double out = new Path2D.Double(evenOdd ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        out.append(p.shape, false);
        return out;
    }
}
