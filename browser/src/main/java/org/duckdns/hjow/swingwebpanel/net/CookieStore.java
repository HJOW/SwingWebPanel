package org.duckdns.hjow.swingwebpanel.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A per-component cookie jar implementing RFC 6265 storage and retrieval without any JVM wide state
 * (<code>java.net.CookieHandler.setDefault</code> is never used). It parses <code>Set-Cookie</code> values
 * (<code>Domain</code>, <code>Path</code>, <code>Expires</code>, <code>Max-Age</code>, <code>Secure</code>,
 * <code>HttpOnly</code>), handles deletion (past expiry, <code>Max-Age</code> &lt;= 0), keeps cookies with the same name
 * but different domain/path, and produces three views:
 * <ul>
 * <li>{@link #getCookieHeader(String)}: the <code>Cookie</code> request header for a URL (includes HttpOnly, Secure only for https);</li>
 * <li>{@link #getDocumentCookie(String)}: what <code>document.cookie</code> returns for a URL (hides HttpOnly);</li>
 * <li>{@link #getCookies()}: every unexpired cookie as <code>name=value; name=value</code> ordered by domain, path and name.</li>
 * </ul>
 * Deliberate policies: a <code>Secure</code> cookie is accepted only from an https URL; a script (see
 * {@link #setFromScript(String, String)}) cannot create or overwrite HttpOnly cookies; Domain attributes that are a single
 * label (for example "com") are rejected unless they equal the request host, because no public suffix list is available
 * (multi label public suffixes such as "co.kr" are therefore NOT rejected - a documented limitation).
 * All methods are thread safe.
 * <p>
 * 컴포넌트별 쿠키 저장소. JVM 전역 상태 없이(<code>java.net.CookieHandler.setDefault</code> 를 쓰지 않음) RFC 6265 의 저장과
 * 조회를 구현한다. <code>Set-Cookie</code> 값(<code>Domain</code>, <code>Path</code>, <code>Expires</code>, <code>Max-Age</code>,
 * <code>Secure</code>, <code>HttpOnly</code>)을 해석하고, 삭제(지난 만료, <code>Max-Age</code> &lt;= 0)를 처리하며, 이름이 같아도
 * 도메인/경로가 다른 쿠키를 모두 보존하고, 세 가지 보기를 제공한다.
 * <ul>
 * <li>{@link #getCookieHeader(String)}: URL 의 <code>Cookie</code> 요청 헤더(HttpOnly 포함, Secure 는 https 만);</li>
 * <li>{@link #getDocumentCookie(String)}: URL 에서 <code>document.cookie</code> 가 돌려줄 값(HttpOnly 숨김);</li>
 * <li>{@link #getCookies()}: 만료되지 않은 모든 쿠키를 도메인, 경로, 이름 순의 <code>name=value; name=value</code> 로.</li>
 * </ul>
 * 의도한 정책: <code>Secure</code> 쿠키는 https URL 에서만 받는다. 스크립트({@link #setFromScript(String, String)})는 HttpOnly
 * 쿠키를 만들거나 덮어쓸 수 없다. 단일 레이블 Domain 속성(예: "com")은 요청 호스트와 같을 때만 허용한다(공개 접미사 목록이
 * 없기 때문이며, "co.kr" 같은 다중 레이블 공개 접미사는 거부되지 않는다는 한계가 있다). 모든 메서드는 스레드 안전하다.
 */
public final class CookieStore {

    /**
     * Source of the current time (replaceable in tests).
     * <p>
     * 현재 시각의 출처(시험에서 교체 가능).
     */
    public interface Clock {
        /**
         * Returns the current time in milliseconds.
         * <p>
         * 현재 시각을 밀리초로 돌려준다.
         *
         * @return time in milliseconds / 밀리초 시각
         */
        long now();
    }

    /**
     * Maximum length of name plus value; longer cookies are ignored.
     * <p>
     * 이름과 값을 합친 최대 길이. 더 긴 쿠키는 무시한다.
     */
    public static final int MAX_COOKIE_LENGTH = 4096;

    /**
     * Maximum number of cookies kept; the oldest are evicted first.
     * <p>
     * 보관하는 최대 쿠키 수. 가장 오래된 것부터 내보낸다.
     */
    public static final int MAX_COOKIES = 3000;

    /**
     * Time token pattern of RFC 6265 5.1.1 (hh:mm:ss).
     * <p>
     * RFC 6265 5.1.1 의 시간 토큰 패턴(hh:mm:ss).
     */
    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{1,2}):(\\d{1,2})(?:[^\\d].*)?$");
    /**
     * Day-of-month token pattern.
     * <p>
     * 일 토큰 패턴.
     */
    private static final Pattern DAY = Pattern.compile("^(\\d{1,2})(?:[^\\d].*)?$");
    /**
     * Year token pattern.
     * <p>
     * 연도 토큰 패턴.
     */
    private static final Pattern YEAR = Pattern.compile("^(\\d{2,4})(?:[^\\d].*)?$");
    /**
     * IPv4 address pattern.
     * <p>
     * IPv4 주소 패턴.
     */
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    /**
     * Month names for date parsing.
     * <p>
     * 날짜 해석용 월 이름.
     */
    private static final String[] MONTHS = { "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };

    /**
     * Stored cookies.
     * <p>
     * 저장된 쿠키.
     */
    private final List<Cookie> cookies = new ArrayList<Cookie>();
    /**
     * Clock used for expiry decisions.
     * <p>
     * 만료 판단에 쓰는 시계.
     */
    private final Clock clock;
    /**
     * Next creation order number.
     * <p>
     * 다음 생성 순서 번호.
     */
    private long sequence;

    /**
     * Creates a store using the system clock.
     * <p>
     * 시스템 시계를 쓰는 저장소를 만든다.
     */
    public CookieStore() {
        this(new Clock() {
            /**
             * Returns the system time.
             * <p>
             * 시스템 시각을 돌려준다.
             *
             * @return time / 시각
             */
            public long now() {
                return System.currentTimeMillis();
            }
        });
    }

    /**
     * Creates a store with a custom clock.
     * <p>
     * 지정한 시계를 쓰는 저장소를 만든다.
     *
     * @param clock time source / 시각 출처
     */
    public CookieStore(Clock clock) {
        this.clock = clock;
    }

    /**
     * Stores the cookies of an HTTP response.
     * <p>
     * HTTP 응답의 쿠키를 저장한다.
     *
     * @param url              URL of the response / 응답의 URL
     * @param setCookieHeaders the <code>Set-Cookie</code> header values / <code>Set-Cookie</code> 헤더 값들
     * @return number of cookies accepted (deletions count as accepted) / 받아들인 쿠키 수(삭제도 포함)
     */
    public synchronized int storeFromResponse(String url, List<String> setCookieHeaders) {
        int accepted = 0;
        // 헤더 값마다 하나의 쿠키 문자열로 보고 저장을 시도한다.
        for (int i = 0; setCookieHeaders != null && i < setCookieHeaders.size(); i++) {
            if (store(url, setCookieHeaders.get(i), false)) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * Stores a cookie set by a script (<code>document.cookie = ...</code>). HttpOnly attributes are not allowed and an
     * existing HttpOnly cookie cannot be overwritten.
     * <p>
     * 스크립트가 설정한 쿠키(<code>document.cookie = ...</code>)를 저장한다. HttpOnly 속성은 허용하지 않으며 기존 HttpOnly
     * 쿠키는 덮어쓸 수 없다.
     *
     * @param url          URL of the document / 문서의 URL
     * @param cookieString cookie text / 쿠키 문자열
     * @return true if accepted / 받아들였으면 true
     */
    public synchronized boolean setFromScript(String url, String cookieString) {
        return store(url, cookieString, true);
    }

    /**
     * Parses and stores one <code>Set-Cookie</code> value.
     * <p>
     * <code>Set-Cookie</code> 값 하나를 해석해 저장한다.
     *
     * @param url        request URL / 요청 URL
     * @param header     cookie text / 쿠키 문자열
     * @param fromScript true when set by a script / 스크립트가 설정했으면 true
     * @return true if accepted / 받아들였으면 true
     */
    private boolean store(String url, String header, boolean fromScript) {
        if (header == null) {
            return false;
        }
        UrlUtil.Parts u;
        try {
            u = UrlUtil.parse(url);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (u.host == null || u.host.length() == 0) {
            return false;
        }
        String host = u.host;
        boolean https = "https".equals(u.scheme);

        String[] pieces = header.split(";", -1);
        String pair = pieces[0];
        int eq = pair.indexOf('=');
        if (eq < 0) {
            return false;
        }
        String name = pair.substring(0, eq).trim();
        String value = pair.substring(eq + 1).trim();
        if (name.length() == 0 || name.length() + value.length() > MAX_COOKIE_LENGTH || hasControl(name) || hasControl(value)) {
            return false;
        }

        String domainAttr = null;
        String pathAttr = null;
        long maxAgeSeconds = Long.MIN_VALUE;
        boolean hasMaxAge = false;
        long expires = Long.MIN_VALUE;
        boolean hasExpires = false;
        boolean secure = false;
        boolean httpOnly = false;
        // 속성 조각을 하나씩 읽어 알려진 이름만 반영한다(모르는 속성은 무시).
        for (int i = 1; i < pieces.length; i++) {
            String av = pieces[i];
            int e2 = av.indexOf('=');
            String an = (e2 < 0 ? av : av.substring(0, e2)).trim().toLowerCase();
            String avv = e2 < 0 ? "" : av.substring(e2 + 1).trim();
            if ("expires".equals(an)) {
                long t = parseDate(avv);
                if (t != Long.MIN_VALUE) {
                    hasExpires = true;
                    expires = t;
                }
            } else if ("max-age".equals(an)) {
                if (avv.matches("-?\\d{1,15}")) {
                    hasMaxAge = true;
                    maxAgeSeconds = Long.parseLong(avv);
                }
            } else if ("domain".equals(an)) {
                if (avv.length() > 0) {
                    domainAttr = (avv.startsWith(".") ? avv.substring(1) : avv).toLowerCase();
                }
            } else if ("path".equals(an)) {
                if (avv.startsWith("/")) {
                    pathAttr = avv;
                }
            } else if ("secure".equals(an)) {
                secure = true;
            } else if ("httponly".equals(an)) {
                httpOnly = true;
            }
        }
        if (fromScript && httpOnly) {
            return false;
        }
        // Secure 쿠키는 https 주소에서만 받는다(안전하지 않은 출처가 만든 Secure 쿠키 거부).
        if (secure && !https) {
            return false;
        }

        String domain;
        boolean hostOnly;
        if (domainAttr != null) {
            if (domainAttr.indexOf('.') < 0 && !domainAttr.equals(host)) {
                return false;
            }
            if (!domainMatches(host, domainAttr)) {
                return false;
            }
            domain = domainAttr;
            hostOnly = domainAttr.equals(host);
        } else {
            domain = host;
            hostOnly = true;
        }
        String path = pathAttr != null ? pathAttr : defaultPath(u.path);

        long now = clock.now();
        long expiresAt;
        if (hasMaxAge) {
            // Max-Age 가 Expires 보다 우선하며 0 이하이면 즉시 만료(삭제)이다.
            expiresAt = maxAgeSeconds <= 0 ? Long.MIN_VALUE : (now + Math.min(maxAgeSeconds, 400L * 24 * 3600) * 1000L);
        } else if (hasExpires) {
            expiresAt = expires;
        } else {
            expiresAt = Long.MAX_VALUE;
        }

        // 같은 (이름, 도메인, 경로)의 기존 쿠키를 찾는다.
        int existingIndex = -1;
        for (int i = 0; i < cookies.size(); i++) {
            Cookie c = cookies.get(i);
            if (c.getName().equals(name) && c.getDomain().equals(domain) && c.getPath().equals(path) && c.isHostOnly() == hostOnly) {
                existingIndex = i;
                break;
            }
        }
        long creation = sequence++;
        if (existingIndex >= 0) {
            Cookie old = cookies.get(existingIndex);
            if (old.isHttpOnly() && fromScript) {
                return false;
            }
            if (old.isSecure() && !https) {
                return false;
            }
            creation = old.getCreation();
            cookies.remove(existingIndex);
        }
        if (expiresAt <= now) {
            // 이미 만료된 쿠키는 저장하지 않으며 위에서 기존 쿠키를 지워 삭제가 끝난다.
            return true;
        }
        cookies.add(new Cookie(name, value, domain, hostOnly, path, expiresAt, secure, httpOnly, creation));
        evictIfNeeded();
        return true;
    }

    /**
     * Tells whether text contains control characters (NUL, CR, LF, other C0).
     * <p>
     * 문자열에 제어 문자(NUL, CR, LF 등)가 있는지 알려 준다.
     *
     * @param s text / 문자열
     * @return true if it has control characters / 제어 문자가 있으면 true
     */
    private static boolean hasControl(String s) {
        // 탭을 제외한 제어 문자가 하나라도 있으면 거부 대상이다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c < 0x20 && c != '\t') || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes expired cookies and, above the limit, the oldest ones.
     * <p>
     * 만료된 쿠키를 지우고 한도를 넘으면 가장 오래된 쿠키를 지운다.
     */
    private void evictIfNeeded() {
        if (cookies.size() <= MAX_COOKIES) {
            return;
        }
        removeExpired();
        // 한도 안으로 들어올 때까지 생성 순서가 가장 앞선 쿠키를 지운다.
        while (cookies.size() > MAX_COOKIES) {
            int oldest = 0;
            // 생성 순서 번호가 가장 작은 쿠키를 찾는다.
            for (int i = 1; i < cookies.size(); i++) {
                if (cookies.get(i).getCreation() < cookies.get(oldest).getCreation()) {
                    oldest = i;
                }
            }
            cookies.remove(oldest);
        }
    }

    /**
     * Removes expired cookies.
     * <p>
     * 만료된 쿠키를 지운다.
     */
    private void removeExpired() {
        long now = clock.now();
        // 뒤에서부터 지워 인덱스가 밀리지 않게 한다.
        for (int i = cookies.size() - 1; i >= 0; i--) {
            if (cookies.get(i).getExpiresAt() <= now) {
                cookies.remove(i);
            }
        }
    }

    /**
     * Returns the default cookie path for a request path (RFC 6265 5.1.4).
     * <p>
     * 요청 경로에 대한 기본 쿠키 경로를 돌려준다(RFC 6265 5.1.4).
     *
     * @param requestPath path of the request URL / 요청 URL 의 경로
     * @return default path / 기본 경로
     */
    static String defaultPath(String requestPath) {
        if (requestPath == null || requestPath.length() == 0 || requestPath.charAt(0) != '/') {
            return "/";
        }
        int last = requestPath.lastIndexOf('/');
        return last == 0 ? "/" : requestPath.substring(0, last);
    }

    /**
     * Domain matching of RFC 6265 5.1.3.
     * <p>
     * RFC 6265 5.1.3 의 도메인 일치.
     *
     * @param host   request host / 요청 호스트
     * @param domain cookie domain / 쿠키 도메인
     * @return true if the host matches the domain / 호스트가 도메인에 일치하면 true
     */
    static boolean domainMatches(String host, String domain) {
        if (host.equals(domain)) {
            return true;
        }
        return host.endsWith("." + domain) && !isIpAddress(host);
    }

    /**
     * Tells whether a host is an IP address literal.
     * <p>
     * 호스트가 IP 주소 리터럴인지 알려 준다.
     *
     * @param host host text / 호스트 문자열
     * @return true for IPv4 or IPv6 / IPv4 또는 IPv6 이면 true
     */
    private static boolean isIpAddress(String host) {
        return host.startsWith("[") || host.indexOf(':') >= 0 || IPV4.matcher(host).matches();
    }

    /**
     * Path matching of RFC 6265 5.1.4.
     * <p>
     * RFC 6265 5.1.4 의 경로 일치.
     *
     * @param requestPath request path / 요청 경로
     * @param cookiePath  cookie path / 쿠키 경로
     * @return true if matching / 일치하면 true
     */
    static boolean pathMatches(String requestPath, String cookiePath) {
        if (requestPath.equals(cookiePath)) {
            return true;
        }
        if (requestPath.startsWith(cookiePath)) {
            return cookiePath.endsWith("/") || requestPath.charAt(cookiePath.length()) == '/';
        }
        return false;
    }

    /**
     * Parses a cookie date (RFC 6265 5.1.1, tolerant).
     * <p>
     * 쿠키 날짜를 해석한다(RFC 6265 5.1.1, 관대하게).
     *
     * @param text date text / 날짜 문자열
     * @return time in milliseconds or Long.MIN_VALUE if unparsable / 밀리초 시각, 해석할 수 없으면 Long.MIN_VALUE
     */
    static long parseDate(String text) {
        int hour = -1;
        int minute = -1;
        int second = -1;
        int day = -1;
        int month = -1;
        int year = -1;
        // 구분 문자로 토큰을 나누어 시간, 일, 월, 연 순서로 처음 맞는 토큰을 채운다.
        String[] tokens = text.split("[\\t\\x20-\\x2F\\x3B-\\x40\\x5B-\\x60\\x7B-\\x7E]+");
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.length() == 0) {
                continue;
            }
            Matcher m;
            if (hour < 0 && (m = TIME.matcher(t)).matches()) {
                hour = Integer.parseInt(m.group(1));
                minute = Integer.parseInt(m.group(2));
                second = Integer.parseInt(m.group(3));
            } else if (day < 0 && (m = DAY.matcher(t)).matches()) {
                day = Integer.parseInt(m.group(1));
            } else if (month < 0 && t.length() >= 3 && monthIndex(t.substring(0, 3)) >= 0) {
                month = monthIndex(t.substring(0, 3));
            } else if (year < 0 && (m = YEAR.matcher(t)).matches()) {
                year = Integer.parseInt(m.group(1));
            }
        }
        if (hour < 0 || day < 0 || month < 0 || year < 0) {
            return Long.MIN_VALUE;
        }
        if (year >= 70 && year <= 99) {
            year += 1900;
        } else if (year >= 0 && year <= 69) {
            year += 2000;
        }
        if (day < 1 || day > 31 || year < 1601 || hour > 23 || minute > 59 || second > 59) {
            return Long.MIN_VALUE;
        }
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setLenient(false);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        try {
            return cal.getTimeInMillis();
        } catch (IllegalArgumentException e) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Returns the 0-based month index for a three letter name, or -1.
     * <p>
     * 세 글자 월 이름의 0 기반 번호를 돌려준다. 없으면 -1.
     *
     * @param abbr three letters / 세 글자
     * @return index or -1 / 번호 또는 -1
     */
    private static int monthIndex(String abbr) {
        String a = abbr.toLowerCase();
        // 월 이름 표에서 일치하는 항목을 찾는다.
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equals(a)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Collects the unexpired cookies that apply to a URL, in the order required for the <code>Cookie</code> header
     * (longer paths first, then older cookies first).
     * <p>
     * URL 에 적용되는 만료되지 않은 쿠키를 <code>Cookie</code> 헤더에 필요한 순서(긴 경로 먼저, 같으면 오래된 것 먼저)로 모은다.
     *
     * @param url         request URL / 요청 URL
     * @param includeHttpOnly whether HttpOnly cookies are included / HttpOnly 쿠키를 포함할지 여부
     * @return matching cookies / 일치하는 쿠키
     */
    private List<Cookie> matching(String url, boolean includeHttpOnly) {
        UrlUtil.Parts u;
        try {
            u = UrlUtil.parse(url);
        } catch (IllegalArgumentException e) {
            return new ArrayList<Cookie>();
        }
        if (u.host == null) {
            return new ArrayList<Cookie>();
        }
        boolean https = "https".equals(u.scheme);
        String path = u.path.length() == 0 ? "/" : u.path;
        long now = clock.now();
        List<Cookie> result = new ArrayList<Cookie>();
        // 모든 쿠키 중 만료/도메인/경로/Secure/HttpOnly 조건을 모두 만족하는 것만 고른다.
        for (int i = 0; i < cookies.size(); i++) {
            Cookie c = cookies.get(i);
            if (c.getExpiresAt() <= now) {
                continue;
            }
            boolean domainOk = c.isHostOnly() ? c.getDomain().equals(u.host) : domainMatches(u.host, c.getDomain());
            if (!domainOk || !pathMatches(path, c.getPath())) {
                continue;
            }
            if (c.isSecure() && !https) {
                continue;
            }
            if (c.isHttpOnly() && !includeHttpOnly) {
                continue;
            }
            result.add(c);
        }
        Collections.sort(result, new Comparator<Cookie>() {
            /**
             * Longer path first, then earlier creation first.
             * <p>
             * 긴 경로가 먼저, 같으면 먼저 만든 쿠키가 먼저.
             *
             * @param a first cookie / 첫 쿠키
             * @param b second cookie / 두 번째 쿠키
             * @return comparison / 비교 결과
             */
            public int compare(Cookie a, Cookie b) {
                if (a.getPath().length() != b.getPath().length()) {
                    return b.getPath().length() - a.getPath().length();
                }
                return a.getCreation() < b.getCreation() ? -1 : (a.getCreation() > b.getCreation() ? 1 : 0);
            }
        });
        return result;
    }

    /**
     * Builds the value of the <code>Cookie</code> request header for a URL.
     * <p>
     * URL 의 <code>Cookie</code> 요청 헤더 값을 만든다.
     *
     * @param url request URL / 요청 URL
     * @return header value, or null if no cookie applies / 헤더 값. 적용되는 쿠키가 없으면 null
     */
    public synchronized String getCookieHeader(String url) {
        return join(matching(url, true));
    }

    /**
     * Returns what <code>document.cookie</code> yields for a document URL: applicable cookies without HttpOnly ones.
     * <p>
     * 문서 URL 에서 <code>document.cookie</code> 가 돌려줄 값을 만든다. 적용되는 쿠키 중 HttpOnly 는 제외한다.
     *
     * @param url document URL / 문서 URL
     * @return cookie string, "" if none / 쿠키 문자열. 없으면 ""
     */
    public synchronized String getDocumentCookie(String url) {
        String s = join(matching(url, false));
        return s == null ? "" : s;
    }

    /**
     * Joins cookies as <code>name=value; name=value</code>.
     * <p>
     * 쿠키를 <code>name=value; name=value</code> 로 잇는다.
     *
     * @param list cookies / 쿠키들
     * @return joined text or null if empty / 이은 문자열. 비어 있으면 null
     */
    private static String join(List<Cookie> list) {
        if (list.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 쿠키마다 이름=값 을 "; " 로 구분해 잇는다.
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(list.get(i).toPair());
        }
        return sb.toString();
    }

    /**
     * Returns every unexpired cookie of the store as <code>name=value; name=value</code>, ordered by domain, path and name
     * (same-name cookies of different domain or path are all included). Used by the public getCookies() method.
     * <p>
     * 저장소의 만료되지 않은 모든 쿠키를 도메인, 경로, 이름 순의 <code>name=value; name=value</code> 로 돌려준다(이름이 같아도
     * 도메인이나 경로가 다르면 모두 포함). 공개 getCookies() 메서드가 사용한다.
     *
     * @return cookie string, "" if none / 쿠키 문자열. 없으면 ""
     */
    public synchronized String getCookies() {
        List<Cookie> list = snapshot();
        String s = join(list);
        return s == null ? "" : s;
    }

    /**
     * Returns the unexpired cookies sorted by domain, path, name (then creation).
     * <p>
     * 만료되지 않은 쿠키를 도메인, 경로, 이름(그다음 생성 순서)으로 정렬해 돌려준다.
     *
     * @return sorted copy / 정렬된 복사본
     */
    public synchronized List<Cookie> snapshot() {
        removeExpired();
        List<Cookie> list = new ArrayList<Cookie>(cookies);
        Collections.sort(list, new Comparator<Cookie>() {
            /**
             * Orders by domain, path, name, creation.
             * <p>
             * 도메인, 경로, 이름, 생성 순서로 정렬한다.
             *
             * @param a first cookie / 첫 쿠키
             * @param b second cookie / 두 번째 쿠키
             * @return comparison / 비교 결과
             */
            public int compare(Cookie a, Cookie b) {
                int r = a.getDomain().compareTo(b.getDomain());
                if (r != 0) {
                    return r;
                }
                r = a.getPath().compareTo(b.getPath());
                if (r != 0) {
                    return r;
                }
                r = a.getName().compareTo(b.getName());
                if (r != 0) {
                    return r;
                }
                return a.getCreation() < b.getCreation() ? -1 : (a.getCreation() > b.getCreation() ? 1 : 0);
            }
        });
        return list;
    }

    /**
     * Removes all cookies.
     * <p>
     * 모든 쿠키를 지운다.
     */
    public synchronized void clear() {
        cookies.clear();
    }

    /**
     * Returns the number of stored cookies (expired ones are removed first).
     * <p>
     * 저장된 쿠키 수를 돌려준다(만료된 쿠키는 먼저 지운다).
     *
     * @return count / 개수
     */
    public synchronized int size() {
        removeExpired();
        return cookies.size();
    }
}
