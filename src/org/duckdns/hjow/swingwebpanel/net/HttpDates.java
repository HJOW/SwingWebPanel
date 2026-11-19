package org.duckdns.hjow.swingwebpanel.net;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Parsing and formatting of HTTP dates (RFC 9110 section 5.6.7): the preferred IMF-fixdate format and the two obsolete formats (RFC 850 and
 * ANSI C <code>asctime</code>) that a recipient must still accept. Used by the cache for <code>Date</code>, <code>Expires</code> and
 * <code>Last-Modified</code>. All methods are thread-safe because every call builds its own format object.
 * <p>
 * HTTP 날짜(RFC 9110 5.6.7)의 해석과 서식: 권장 형식인 IMF-fixdate 와, 받는 쪽이 여전히 받아들여야 하는 두 가지 폐기된 형식(RFC 850, ANSI C
 * <code>asctime</code>). 캐시가 <code>Date</code>, <code>Expires</code>, <code>Last-Modified</code> 에 쓴다. 호출마다 새 서식 객체를 만들므로
 * 모든 메서드는 스레드 안전하다.
 */
final class HttpDates {

    /**
     * The accepted formats in the order they are tried.
     * <p>
     * 시도하는 순서대로 나열한 허용 형식.
     */
    private static final String[] PATTERNS = { "EEE, dd MMM yyyy HH:mm:ss zzz", "EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HttpDates() {
    }

    /**
     * Parses an HTTP date.
     * <p>
     * HTTP 날짜를 해석한다.
     *
     * @param text the header value, may be null / 헤더 값. null 가능
     * @return milliseconds since 1970-01-01T00:00:00Z, or -1 if the text is missing or not a valid date / 1970-01-01T00:00:00Z 이후 밀리초. 없거나 올바른 날짜가 아니면 -1
     */
    static long parse(String text) {
        if (text == null) {
            return -1;
        }
        String s = text.trim();
        if (s.length() == 0) {
            return -1;
        }
        // 허용 형식을 하나씩 시도해 처음 성공한 것을 쓴다.
        for (int i = 0; i < PATTERNS.length; i++) {
            SimpleDateFormat f = new SimpleDateFormat(PATTERNS[i], Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            f.setLenient(false);
            try {
                Date d = f.parse(s);
                return d.getTime();
            } catch (ParseException e) {
                // 이 형식이 아니면 다음 형식을 시도한다.
                continue;
            }
        }
        return -1;
    }

    /**
     * Formats a time as an IMF-fixdate such as <code>Sun, 06 Nov 1994 08:49:37 GMT</code>.
     * <p>
     * 시각을 <code>Sun, 06 Nov 1994 08:49:37 GMT</code> 같은 IMF-fixdate 로 만든다.
     *
     * @param millis milliseconds since 1970-01-01T00:00:00Z / 1970-01-01T00:00:00Z 이후 밀리초
     * @return the formatted date / 서식을 적용한 날짜
     */
    static String format(long millis) {
        SimpleDateFormat f = new SimpleDateFormat(PATTERNS[0], Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(new Date(millis));
    }
}
