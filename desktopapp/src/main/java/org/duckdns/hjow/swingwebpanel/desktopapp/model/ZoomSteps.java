package org.duckdns.hjow.swingwebpanel.desktopapp.model;

/**
 * The zoom factors that the zoom commands of the browser step through (plan item P31-05). They are the same steps that the component
 * itself uses for Ctrl+plus and Ctrl+minus, so that a menu command and a key pressed inside the page give the same result.
 * <p>
 * 브라우저의 확대 명령이 거치는 확대 배율(계획 P31-05). 컴포넌트가 Ctrl+플러스와 Ctrl+마이너스에 쓰는 단계와 같으므로, 메뉴 명령과 페이지 안에서
 * 누른 키가 같은 결과를 낸다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * panel.setZoom(ZoomSteps.larger(panel.getZoom()));
 * </pre>
 */
public final class ZoomSteps {

    /** The smallest factor. / 가장 작은 배율. */
    public static final double MIN = 0.25;

    /** The largest factor. / 가장 큰 배율. */
    public static final double MAX = 5.0;

    /** The steps from the smallest to the largest. / 가장 작은 것부터 가장 큰 것까지의 단계. */
    private static final double[] STEPS = { 0.25, 0.33, 0.5, 0.67, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 5.0 };

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private ZoomSteps() {
    }

    /**
     * Returns the next larger step.
     * <p>
     * 다음으로 큰 단계를 반환한다.
     *
     * @param zoom the factor now / 지금의 배율
     * @return the next step above it, or the largest step / 그보다 큰 다음 단계. 없으면 가장 큰 단계
     */
    public static double larger(double zoom) {
        // 지금 배율보다 큰 첫 단계를 찾는다.
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i] > zoom + 0.001) {
                return STEPS[i];
            }
        }
        return MAX;
    }

    /**
     * Returns the next smaller step.
     * <p>
     * 다음으로 작은 단계를 반환한다.
     *
     * @param zoom the factor now / 지금의 배율
     * @return the next step below it, or the smallest step / 그보다 작은 다음 단계. 없으면 가장 작은 단계
     */
    public static double smaller(double zoom) {
        // 지금 배율보다 작은 첫 단계를 큰 쪽부터 찾는다.
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (STEPS[i] < zoom - 0.001) {
                return STEPS[i];
            }
        }
        return MIN;
    }

    /**
     * Reads a zoom factor from a percentage text such as <code>125</code> or <code>125%</code>.
     * <p>
     * <code>125</code> 나 <code>125%</code> 같은 백분율 글에서 확대 배율을 읽는다.
     *
     * @param percent  the text / 글
     * @param fallback the factor for text that is not a number / 수가 아닌 글일 때의 배율
     * @return the factor, cut to the range of the steps / 배율. 단계의 범위로 자른다
     */
    public static double parsePercent(String percent, double fallback) {
        String t = percent == null ? "" : percent.trim();
        if (t.endsWith("%")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        try {
            double v = Double.parseDouble(t) / 100.0;
            // 수가 아니거나 양수가 아니면 대신할 배율을 쓴다.
            if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) {
                return fallback;
            }
            return Math.max(MIN, Math.min(MAX, v));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Writes a zoom factor as a whole percentage, for example <code>125</code>.
     * <p>
     * 확대 배율을 정수 백분율로 적는다. 예: <code>125</code>.
     *
     * @param zoom the factor / 배율
     * @return the percentage without a sign / 기호 없는 백분율
     */
    public static String toPercent(double zoom) {
        return String.valueOf(Math.round(zoom * 100.0));
    }
}
