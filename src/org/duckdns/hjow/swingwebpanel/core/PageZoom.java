package org.duckdns.hjow.swingwebpanel.core;

/**
 * The rules of the page zoom (plan item P11-08): the allowed range of the zoom factor and the steps that the keyboard and the wheel go
 * through. The factor 1 shows the page at 100 percent. The steps are the ones that browsers use. Everything here is a pure function.
 * <p>
 * 페이지 확대의 규칙(계획 P11-08): 확대 배율의 허용 범위와 키보드와 휠이 지나가는 단계. 배율 1 은 페이지를 100 % 로 보여 준다. 단계는 브라우저가 쓰는 것이다.
 * 여기의 모든 것은 순수 함수이다.
 */
public final class PageZoom {

    /**
     * The smallest zoom factor (25 percent).
     * <p>
     * 가장 작은 확대 배율(25 %).
     */
    public static final double MIN = 0.25;

    /**
     * The largest zoom factor (500 percent).
     * <p>
     * 가장 큰 확대 배율(500 %).
     */
    public static final double MAX = 5.0;

    /**
     * The zoom factor of a page that is not zoomed (100 percent).
     * <p>
     * 확대하지 않은 페이지의 배율(100 %).
     */
    public static final double DEFAULT = 1.0;

    /**
     * The steps of Ctrl+plus and Ctrl+minus, in ascending order.
     * <p>
     * Ctrl+플러스와 Ctrl+마이너스의 단계. 오름차순.
     */
    private static final double[] STEPS = { 0.25, 0.33, 0.5, 0.67, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 5.0 };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PageZoom() {
    }

    /**
     * Keeps a factor inside the allowed range.
     * <p>
     * 배율을 허용 범위 안으로 맞춘다.
     *
     * @param zoom the factor, a finite positive number / 배율. 유한한 양수
     * @return the factor between {@link #MIN} and {@link #MAX} / {@link #MIN} 과 {@link #MAX} 사이의 배율
     * @throws IllegalArgumentException for NaN, infinite or not positive numbers / NaN, 무한, 양수가 아닌 수일 때
     */
    public static double clamp(double zoom) {
        if (Double.isNaN(zoom) || Double.isInfinite(zoom) || zoom <= 0) {
            throw new IllegalArgumentException("zoom must be a finite positive number: " + zoom);
        }
        return Math.max(MIN, Math.min(MAX, zoom));
    }

    /**
     * Returns the next larger step (the largest step if there is none).
     * <p>
     * 다음으로 큰 단계를 반환한다(없으면 가장 큰 단계).
     *
     * @param zoom the current factor / 현재 배율
     * @return the factor of the step / 단계의 배율
     */
    public static double stepUp(double zoom) {
        // 현재 값보다 (오차를 두고) 큰 첫 단계를 고른다.
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i] > zoom + 0.001) {
                return STEPS[i];
            }
        }
        return MAX;
    }

    /**
     * Returns the next smaller step (the smallest step if there is none).
     * <p>
     * 다음으로 작은 단계를 반환한다(없으면 가장 작은 단계).
     *
     * @param zoom the current factor / 현재 배율
     * @return the factor of the step / 단계의 배율
     */
    public static double stepDown(double zoom) {
        // 현재 값보다 (오차를 두고) 작은 마지막 단계를 고른다.
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (STEPS[i] < zoom - 0.001) {
                return STEPS[i];
            }
        }
        return MIN;
    }
}
