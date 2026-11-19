package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The size rules of the <code>canvas</code> element: the <code>width</code> and <code>height</code> attributes are parsed with the rules for
 * non-negative integers and fall back to 300 and 150 when they are absent or invalid (each independently). The layout uses the result as the
 * natural size of the replaced element and the canvas bindings as the size of the bitmap, so both always agree.
 * <p>
 * <code>canvas</code> 요소의 크기 규칙: <code>width</code> 와 <code>height</code> 속성은 음이 아닌 정수 해석 규칙으로 읽으며, 없거나 올바르지 않으면
 * (각각 독립적으로) 300 과 150 을 쓴다. 레이아웃은 결과를 치환 요소의 자연 크기로, 캔버스 연결은 비트맵의 크기로 쓰므로 둘은 언제나 일치한다.
 */
public final class CanvasSizes {

    /**
     * The default width in pixels.
     * <p>
     * 기본 너비(화소).
     */
    public static final int DEFAULT_WIDTH = 300;

    /**
     * The default height in pixels.
     * <p>
     * 기본 높이(화소).
     */
    public static final int DEFAULT_HEIGHT = 150;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CanvasSizes() {
    }

    /**
     * Parses a dimension attribute with the rules for non-negative integers (leading white space and a plus sign are allowed, trailing
     * characters after the digits are ignored; a minus sign, no digits or a value above 2147483647 is an error).
     * <p>
     * 크기 속성을 음이 아닌 정수 해석 규칙으로 읽는다(앞의 공백과 더하기 부호는 허용하고, 숫자 뒤의 문자는 무시한다. 빼기 부호, 숫자 없음, 2147483647 초과는
     * 오류이다).
     *
     * @param value    the attribute value or null / 속성 값 또는 null
     * @param fallback the value for an absent or invalid attribute / 없거나 올바르지 않은 속성의 값
     * @return the number / 숫자
     */
    public static int parse(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        int i = 0;
        // 앞의 공백을 건너뛴다.
        while (i < value.length() && isSpace(value.charAt(i))) {
            i++;
        }
        if (i < value.length() && value.charAt(i) == '+') {
            i++;
        }
        long n = 0;
        int digits = 0;
        // 숫자를 읽는다. 값이 한도를 넘으면 오류로 본다.
        while (i < value.length() && value.charAt(i) >= '0' && value.charAt(i) <= '9') {
            n = n * 10 + (value.charAt(i) - '0');
            if (n > Integer.MAX_VALUE) {
                return fallback;
            }
            digits++;
            i++;
        }
        return digits == 0 ? fallback : (int) n;
    }

    /**
     * Tells whether a character is HTML white space.
     * <p>
     * 문자가 HTML 공백인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for space, tab, line feed, form feed and carriage return / 스페이스, 탭, 줄바꿈, 폼 피드, 캐리지 리턴이면 true
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Returns the width of a canvas element.
     * <p>
     * 캔버스 요소의 너비를 반환한다.
     *
     * @param canvas the element / 요소
     * @return the width in pixels / 너비(화소)
     */
    public static int width(Element canvas) {
        return parse(canvas.getAttribute("width"), DEFAULT_WIDTH);
    }

    /**
     * Returns the height of a canvas element.
     * <p>
     * 캔버스 요소의 높이를 반환한다.
     *
     * @param canvas the element / 요소
     * @return the height in pixels / 높이(화소)
     */
    public static int height(Element canvas) {
        return parse(canvas.getAttribute("height"), DEFAULT_HEIGHT);
    }
}
