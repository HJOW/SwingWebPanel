package org.duckdns.hjow.swingwebpanel.css.cascade;

/**
 * Turns the <code>rows</code> and <code>cols</code> attributes of a <code>frameset</code> (HTML 4.01 section 16.2.1: a comma-separated list of
 * pixel lengths, percentages and relative lengths such as <code>*</code> and <code>2*</code>) into the track list of a CSS grid: pixels become
 * <code>px</code> tracks, percentages stay percentages and a relative length becomes a flexible <code>fr</code> track that shares what remains.
 * A missing, empty or unreadable list is one track that fills the whole frameset.
 * <p>
 * <code>frameset</code> 의 <code>rows</code>, <code>cols</code> 속성(HTML 4.01 16.2.1: 픽셀 길이, 백분율, <code>*</code>/<code>2*</code> 같은 상대 길이를
 * 쉼표로 나열한 목록)을 CSS 그리드의 트랙 목록으로 바꾼다: 픽셀은 <code>px</code> 트랙, 백분율은 그대로 백분율, 상대 길이는 남은 공간을 나누는 유연한
 * <code>fr</code> 트랙이 된다. 없거나 비었거나 읽을 수 없는 목록은 프레임셋 전체를 채우는 트랙 하나이다.
 */
public final class FramesetTracks {

    /**
     * The track that fills everything.
     * <p>
     * 전체를 채우는 트랙.
     */
    private static final String FILL = "minmax(0,1fr)";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FramesetTracks() {
    }

    /**
     * Converts the attribute value into the value of <code>grid-template-rows</code> or <code>grid-template-columns</code>.
     * <p>
     * 속성 값을 <code>grid-template-rows</code> 또는 <code>grid-template-columns</code> 의 값으로 바꾼다.
     *
     * @param attribute the attribute value or null / 속성 값 또는 null
     * @return the track list, never empty / 트랙 목록(비어 있지 않음)
     */
    public static String toTracks(String attribute) {
        if (attribute == null || attribute.trim().length() == 0) {
            return FILL;
        }
        String[] parts = attribute.split(",");
        StringBuilder out = new StringBuilder();
        // 목록의 항목을 하나씩 트랙으로 바꾼다.
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(track(parts[i].trim()));
        }
        return out.toString();
    }

    /**
     * Returns the number of tracks that the attribute defines.
     * <p>
     * 속성이 정의하는 트랙의 수를 반환한다.
     *
     * @param attribute the attribute value or null / 속성 값 또는 null
     * @return the number of tracks, at least 1 / 트랙 수(1 이상)
     */
    public static int count(String attribute) {
        if (attribute == null || attribute.trim().length() == 0) {
            return 1;
        }
        return attribute.split(",").length;
    }

    /**
     * Converts one list item.
     * <p>
     * 목록 항목 하나를 바꾼다.
     *
     * @param item the trimmed item / 공백을 뺀 항목
     * @return the track / 트랙
     */
    private static String track(String item) {
        if (item.length() == 0) {
            return FILL;
        }
        char last = item.charAt(item.length() - 1);
        if (last == '*') {
            double n = number(item.substring(0, item.length() - 1));
            // 숫자가 없거나 0 인 상대 길이는 1 로 본다.
            return "minmax(0," + format(n > 0 ? n : 1) + "fr)";
        }
        if (last == '%') {
            double n = number(item.substring(0, item.length() - 1));
            return n >= 0 ? format(n) + "%" : FILL;
        }
        double n = number(item);
        return n >= 0 ? format(n) + "px" : FILL;
    }

    /**
     * Reads the leading number of an item (digits with an optional fraction); trailing text such as a unit is ignored.
     * <p>
     * 항목의 앞쪽 숫자(소수 가능)를 읽는다. 단위 같은 뒤쪽 문자열은 무시한다.
     *
     * @param text the text / 문자열
     * @return the number, or -1 if there is none / 숫자. 없으면 -1
     */
    private static double number(String text) {
        String t = text.trim();
        int end = 0;
        boolean dot = false;
        // 숫자와 소수점 하나까지만 읽는다.
        while (end < t.length() && (Character.isDigit(t.charAt(end)) && t.charAt(end) < 128 || t.charAt(end) == '.' && !dot)) {
            if (t.charAt(end) == '.') {
                dot = true;
            }
            end++;
        }
        if (end == 0 || t.substring(0, end).equals(".")) {
            return -1;
        }
        try {
            return Double.parseDouble(t.substring(0, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formats a number without a needless fraction.
     * <p>
     * 불필요한 소수 없이 숫자를 쓴다.
     *
     * @param n the number / 숫자
     * @return the text / 문자열
     */
    private static String format(double n) {
        if (n == Math.floor(n) && n < 1e9) {
            return Long.toString((long) n);
        }
        return Double.toString(n);
    }
}
