package org.duckdns.hjow.swingwebpanel.js.host.canvas;

/**
 * A <code>Path2D</code> value: a path that is not tied to a context. Its points are kept in user space (the matrix of a context is applied when
 * the path is filled, stroked or used to clip), unlike the current default path of a context whose points are transformed when they are added.
 * <p>
 * <code>Path2D</code> 값: 컨텍스트에 묶이지 않은 경로. 점은 사용자 좌표로 보관하며(컨텍스트의 행렬은 경로를 채우거나 긋거나 클립에 쓸 때 적용한다), 점을 더할 때
 * 변환하는 컨텍스트의 현재 기본 경로와 다르다.
 */
public final class Path2DValue {

    /**
     * The path in user space.
     * <p>
     * 사용자 좌표의 경로.
     */
    private final PathBuilder path = new PathBuilder();

    /**
     * Creates an empty path.
     * <p>
     * 빈 경로를 만든다.
     */
    public Path2DValue() {
    }

    /**
     * Creates a path from SVG path data (the form <code>new Path2D("M10 10 h 80")</code>). Data that cannot be parsed to the end gives the
     * path up to the first error, as SVG renders it.
     * <p>
     * SVG 경로 데이터(<code>new Path2D("M10 10 h 80")</code> 형태)로 경로를 만든다. 끝까지 해석할 수 없는 데이터는 SVG 가 그리듯 첫 오류까지의 경로가 된다.
     *
     * @param data the path data / 경로 데이터
     */
    public Path2DValue(String data) {
        PathData.parse(data, path);
    }

    /**
     * Creates a copy of another path.
     * <p>
     * 다른 경로의 복사본을 만든다.
     *
     * @param other the path to copy / 복사할 경로
     */
    public Path2DValue(Path2DValue other) {
        path.append(other.path, new java.awt.geom.AffineTransform());
    }

    /**
     * Returns the builder that holds the path.
     * <p>
     * 경로를 담은 빌더를 반환한다.
     *
     * @return the builder / 빌더
     */
    PathBuilder path() {
        return path;
    }
}
