package org.duckdns.hjow.swingwebpanel.media.vtt;

/**
 * The small syntaxes of WebVTT that several parsers share: the timestamp (WebVTT 6.3 "collect a WebVTT timestamp"), its text form (6.5) and the
 * percentage (6.2 "parse a percentage string"). A parse that fails returns {@link Double#NaN}; a successful parse never does.
 * <p>
 * 여러 파서가 공유하는 WebVTT 의 작은 구문: 타임스탬프(WebVTT 6.3 "collect a WebVTT timestamp"), 그 텍스트 형태(6.5), 백분율(6.2 "parse a percentage
 * string"). 해석이 실패하면 {@link Double#NaN} 을 반환하며 성공한 해석은 NaN 을 반환하지 않는다.
 */
final class VttTimestamps {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private VttTimestamps() {
    }

    /**
     * Tells whether a character is an ASCII digit.
     * <p>
     * 문자가 ASCII 숫자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for 0 to 9 / 0~9 이면 true
     */
    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Tells whether a character is ASCII whitespace (space, tab, line feed, form feed, carriage return).
     * <p>
     * 문자가 ASCII 공백(스페이스, 탭, 줄 바꿈, 폼 피드, 캐리지 리턴)인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for ASCII whitespace / ASCII 공백이면 true
     */
    static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Collects the digits at a position and returns them (the position moves past them).
     * <p>
     * 위치의 숫자를 모아 반환한다(위치는 숫자를 지나 움직인다).
     *
     * @param in  the text / 텍스트
     * @param pos the position, one element / 위치(원소 하나)
     * @return the digits, possibly empty / 숫자(비어 있을 수 있음)
     */
    private static String digits(String in, int[] pos) {
        int start = pos[0];
        // 숫자가 이어지는 동안 위치를 옮긴다.
        while (pos[0] < in.length() && isDigit(in.charAt(pos[0]))) {
            pos[0]++;
        }
        return in.substring(start, pos[0]);
    }

    /**
     * Collects a WebVTT timestamp (<code>mm:ss.ttt</code> or <code>hh:mm:ss.ttt</code>, with as many hour digits as needed).
     * <p>
     * WebVTT 타임스탬프(<code>mm:ss.ttt</code> 또는 <code>hh:mm:ss.ttt</code>, 시 자리는 필요한 만큼)를 모은다.
     *
     * @param in  the text / 텍스트
     * @param pos the position, one element; it moves past the timestamp / 위치(원소 하나). 타임스탬프를 지나 움직인다
     * @return the time in seconds, or NaN on failure / 시각(초). 실패하면 NaN
     */
    static double collectTimestamp(String in, int[] pos) {
        boolean hours = false;
        if (pos[0] >= in.length() || !isDigit(in.charAt(pos[0]))) {
            return Double.NaN;
        }
        String s1 = digits(in, pos);
        // 처음 덩어리가 두 자리가 아니거나 59 를 넘으면 시 단위이다(너무 긴 숫자는 시로 보고 값은 한도로 자른다).
        long v1 = s1.length() > 9 ? Long.MAX_VALUE / 4 : Long.parseLong(s1);
        if (s1.length() != 2 || v1 > 59) {
            hours = true;
        }
        if (pos[0] >= in.length() || in.charAt(pos[0]) != ':') {
            return Double.NaN;
        }
        pos[0]++;
        String s2 = digits(in, pos);
        if (s2.length() != 2) {
            return Double.NaN;
        }
        long v2 = Long.parseLong(s2);
        long v3;
        if (hours || (pos[0] < in.length() && in.charAt(pos[0]) == ':')) {
            // 시 단위이거나 콜론이 하나 더 있으면 세 번째 덩어리(초)를 읽는다.
            if (pos[0] >= in.length() || in.charAt(pos[0]) != ':') {
                return Double.NaN;
            }
            pos[0]++;
            String s3 = digits(in, pos);
            if (s3.length() != 2) {
                return Double.NaN;
            }
            v3 = Long.parseLong(s3);
        } else {
            // 분:초 형태: 읽은 두 덩어리를 분과 초로 옮기고 시는 0 으로 한다.
            v3 = v2;
            v2 = v1;
            v1 = 0;
        }
        if (pos[0] >= in.length() || in.charAt(pos[0]) != '.') {
            return Double.NaN;
        }
        pos[0]++;
        String s4 = digits(in, pos);
        if (s4.length() != 3) {
            return Double.NaN;
        }
        long v4 = Long.parseLong(s4);
        if (v2 > 59 || v3 > 59) {
            return Double.NaN;
        }
        return v1 * 3600.0 + v2 * 60.0 + v3 + v4 / 1000.0;
    }

    /**
     * Writes a time as a WebVTT timestamp with all its components (<code>hh:mm:ss.ttt</code>; the hours have at least two digits).
     * <p>
     * 시각을 모든 구성 요소가 있는 WebVTT 타임스탬프(<code>hh:mm:ss.ttt</code>, 시는 두 자리 이상)로 쓴다.
     *
     * @param seconds the time, not negative / 시각(음수가 아님)
     * @return the text / 텍스트
     */
    static String format(double seconds) {
        long total = Math.round(seconds * 1000.0);
        long ms = total % 1000;
        long secs = total / 1000;
        long h = secs / 3600;
        long m = (secs / 60) % 60;
        long s = secs % 60;
        return two(h) + ":" + two(m) + ":" + two(s) + "." + (ms < 10 ? "00" : ms < 100 ? "0" : "") + ms;
    }

    /**
     * Writes a number with at least two digits.
     * <p>
     * 숫자를 두 자리 이상으로 쓴다.
     *
     * @param n the number / 숫자
     * @return the text / 텍스트
     */
    private static String two(long n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    /**
     * Parses a WebVTT percentage: digits, optionally a point and digits, and a percent sign, in the range 0 to 100.
     * <p>
     * WebVTT 백분율을 해석한다: 숫자, 선택적으로 소수점과 숫자, 퍼센트 기호이며 값은 0에서 100.
     *
     * @param text the text, with the percent sign / 텍스트(퍼센트 기호 포함)
     * @return the percentage, or NaN if the text is not one / 백분율. 아니면 NaN
     */
    static double parsePercentage(String text) {
        if (text == null || text.length() < 2 || text.charAt(text.length() - 1) != '%') {
            return Double.NaN;
        }
        String body = text.substring(0, text.length() - 1);
        int[] pos = { 0 };
        String whole = digits(body, pos);
        if (whole.length() == 0) {
            return Double.NaN;
        }
        // 소수점이 있으면 뒤에 숫자가 하나 이상 있어야 한다.
        if (pos[0] < body.length() && body.charAt(pos[0]) == '.') {
            pos[0]++;
            if (digits(body, pos).length() == 0) {
                return Double.NaN;
            }
        }
        if (pos[0] != body.length()) {
            return Double.NaN;
        }
        double v = Double.parseDouble(body);
        return v < 0 || v > 100 ? Double.NaN : v;
    }
}
