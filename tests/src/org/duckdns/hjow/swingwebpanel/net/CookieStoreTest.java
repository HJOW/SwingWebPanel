package org.duckdns.hjow.swingwebpanel.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link CookieStore} (plan item P04-10, API_CONTRACT 5.16) with a controllable clock: attributes, matching,
 * deletion, ordering, Secure and HttpOnly handling and the script view.
 * <p>
 * 시계를 조절할 수 있는 {@link CookieStore} 시험(계획 P04-10, API_CONTRACT 5.16): 속성, 일치, 삭제, 정렬, Secure/HttpOnly 처리,
 * 스크립트용 보기.
 */
public class CookieStoreTest {

    /**
     * Current fake time in milliseconds.
     * <p>
     * 가짜 현재 시각(밀리초).
     */
    private long now = 1000000000000L;

    /**
     * Store under test.
     * <p>
     * 시험 대상 저장소.
     */
    private final CookieStore store = new CookieStore(new CookieStore.Clock() {
        /**
         * Returns the fake time.
         * <p>
         * 가짜 시각을 돌려준다.
         *
         * @return time / 시각
         */
        public long now() {
            return now;
        }
    });

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CookieStoreTest() {
    }

    /**
     * Stores one Set-Cookie value for a URL.
     * <p>
     * URL 에 대한 Set-Cookie 값 하나를 저장한다.
     *
     * @param url    response URL / 응답 URL
     * @param header Set-Cookie value / Set-Cookie 값
     * @return true if accepted / 받아들였으면 true
     */
    private boolean set(String url, String header) {
        List<String> l = new ArrayList<String>();
        l.add(header);
        return store.storeFromResponse(url, l) > 0;
    }

    /**
     * A plain cookie is host-only, gets the default path and is sent back.
     * <p>
     * 단순 쿠키는 호스트 전용이며 기본 경로를 받고 다시 전송된다.
     */
    public void testHostOnlyAndDefaultPath() {
        Check.isTrue(set("http://a.example/dir/page.html", "k=v"), "accepted");
        Check.equal("k=v", store.getCookieHeader("http://a.example/dir/other"), "default path /dir matches /dir/other");
        Check.isNull(store.getCookieHeader("http://a.example/"), "not sent above the default path");
        Check.isNull(store.getCookieHeader("http://sub.a.example/dir/x"), "host-only does not match sub domains");
        Check.isNull(store.getCookieHeader("http://b.example/dir/x"), "other host");
        Check.isNull(store.getCookieHeader("http://a.example/directory"), "path prefix must end at a segment boundary");
    }

    /**
     * Domain cookies match sub domains; invalid domains are rejected.
     * <p>
     * Domain 쿠키는 하위 도메인에도 일치하고, 잘못된 도메인은 거부된다.
     */
    public void testDomainAttribute() {
        Check.isTrue(set("http://www.a.example/", "d=1; Domain=.a.example"), "parent domain accepted");
        Check.equal("d=1", store.getCookieHeader("http://x.y.a.example/"), "deep sub domain");
        Check.equal("d=1", store.getCookieHeader("http://a.example/"), "the domain itself");
        Check.isFalse(set("http://www.a.example/", "e=1; Domain=b.example"), "foreign domain rejected");
        Check.isFalse(set("http://www.a.example/", "f=1; Domain=com"), "single label domain rejected");
        Check.isFalse(set("http://a.example/", "g=1; Domain=sub.a.example"), "child domain rejected for a parent host");
        Check.isTrue(set("http://localhost/", "h=1; Domain=localhost"), "single label equal to the host is fine");
        Check.isFalse(set("http://127.0.0.1/", "i=1; Domain=0.0.1"), "ip addresses cannot widen the domain");
    }

    /**
     * Secure cookies are accepted only from https and sent only over https.
     * <p>
     * Secure 쿠키는 https 에서만 받고 https 로만 보낸다.
     */
    public void testSecure() {
        Check.isFalse(set("http://s.example/", "a=1; Secure"), "Secure from http rejected");
        Check.isTrue(set("https://s.example/", "b=2; Secure"), "Secure from https accepted");
        Check.isTrue(set("https://s.example/", "c=3"), "plain cookie from https");
        Check.equal("b=2; c=3", store.getCookieHeader("https://s.example/"), "both sent over https");
        Check.equal("c=3", store.getCookieHeader("http://s.example/"), "Secure cookie is not sent over http");
        Check.isFalse(set("http://s.example/", "b=overwritten"), "http cannot overwrite a Secure cookie");
    }

    /**
     * HttpOnly cookies are in the request header but hidden from document.cookie and from scripts.
     * <p>
     * HttpOnly 쿠키는 요청 헤더에는 있지만 document.cookie 와 스크립트에서는 숨겨진다.
     */
    public void testHttpOnlyAndScriptView() {
        set("http://h.example/", "srv=1; HttpOnly");
        set("http://h.example/", "vis=2");
        Check.equal("srv=1; vis=2", store.getCookieHeader("http://h.example/"), "header includes HttpOnly");
        Check.equal("vis=2", store.getDocumentCookie("http://h.example/"), "document.cookie hides HttpOnly");
        Check.isFalse(store.setFromScript("http://h.example/", "x=1; HttpOnly"), "scripts cannot create HttpOnly cookies");
        Check.isFalse(store.setFromScript("http://h.example/", "srv=hacked"), "scripts cannot overwrite HttpOnly cookies");
        Check.isTrue(store.setFromScript("http://h.example/", "fromjs=3; Path=/"), "ordinary script cookie");
        Check.equal("srv=1; vis=2; fromjs=3", store.getCookieHeader("http://h.example/"), "script cookie is sent too");
        Check.equal("", store.getDocumentCookie("http://other.example/"), "empty string when nothing applies");
    }

    /**
     * Max-Age and Expires expire cookies; Max-Age wins; deletion works.
     * <p>
     * Max-Age 와 Expires 가 쿠키를 만료시킨다. Max-Age 가 우선하며 삭제도 동작한다.
     */
    public void testExpiryAndDeletion() {
        set("http://e.example/", "short=1; Max-Age=10");
        set("http://e.example/", "sess=2");
        set("http://e.example/", "long=3; Expires=Wed, 21 Oct 2099 07:28:00 GMT");
        Check.equal("short=1; sess=2; long=3", store.getCookieHeader("http://e.example/"), "all present at first");
        now += 11000L;
        Check.equal("sess=2; long=3", store.getCookieHeader("http://e.example/"), "Max-Age=10 expired after 11 s");
        set("http://e.example/", "sess=gone; Max-Age=0");
        Check.equal("long=3", store.getCookieHeader("http://e.example/"), "Max-Age=0 deletes");
        set("http://e.example/", "long=x; Expires=Thu, 01 Jan 1970 00:00:01 GMT");
        Check.isNull(store.getCookieHeader("http://e.example/"), "a past Expires deletes");
        set("http://e.example/", "both=1; Max-Age=1000; Expires=Thu, 01 Jan 1970 00:00:01 GMT");
        Check.equal("both=1", store.getCookieHeader("http://e.example/"), "Max-Age takes precedence over Expires");
        Check.equal(1, store.size(), "expired cookies are not counted");
    }

    /**
     * Cookie date parsing follows RFC 6265 section 5.1.1.
     * <p>
     * 쿠키 날짜 해석은 RFC 6265 5.1.1 을 따른다.
     */
    public void testDateParsing() {
        Check.equal(1445412480000L, CookieStore.parseDate("Wed, 21 Oct 2015 07:28:00 GMT"), "IMF-fixdate");
        Check.equal(784111777000L, CookieStore.parseDate("Sunday, 06-Nov-94 08:49:37 GMT"), "RFC 850 with two digit year");
        Check.equal(784111777000L, CookieStore.parseDate("Sun Nov  6 08:49:37 1994"), "asctime");
        Check.equal(Long.MIN_VALUE, CookieStore.parseDate("garbage"), "unparsable");
        Check.equal(Long.MIN_VALUE, CookieStore.parseDate("Wed, 31 Feb 2015 07:28:00 GMT"), "impossible date");
        Check.equal(Long.MIN_VALUE, CookieStore.parseDate("Wed, 21 Oct 2015 25:28:00 GMT"), "bad hour");
    }

    /**
     * Cookies with the same name but different path or domain are all kept; the same triple is replaced.
     * <p>
     * 이름이 같아도 경로나 도메인이 다른 쿠키는 모두 유지하고, 같은 조합은 교체한다.
     */
    public void testSameNameCookiesAreKept() {
        set("http://x.example/", "id=root; Path=/");
        set("http://x.example/a/b", "id=deep; Path=/a");
        set("http://y.example/", "id=other");
        Check.equal("id=deep; id=root", store.getCookieHeader("http://x.example/a/c"), "longer path first");
        Check.equal("id=root", store.getCookieHeader("http://x.example/zzz"), "path scoping");
        Check.equal("id=root; id=deep; id=other", store.getCookies(), "getCookies lists all of them sorted by domain, path, name");
        set("http://x.example/", "id=replaced; Path=/");
        Check.equal("id=replaced; id=deep; id=other", store.getCookies(), "same name+domain+path is replaced");
    }

    /**
     * getCookies() orders by domain, then path, then name and uses the name=value; name=value format.
     * <p>
     * getCookies() 는 도메인, 경로, 이름 순으로 정렬하고 name=value; name=value 형식을 쓴다.
     */
    public void testGetCookiesOrderAndFormat() {
        Check.equal("", store.getCookies(), "empty store");
        set("http://b.example/", "z=1");
        set("http://a.example/", "m=2");
        set("http://a.example/", "b=3");
        set("http://a.example/p/", "a=4; Path=/p");
        Check.equal("b=3; m=2; a=4; z=1", store.getCookies(), "domain, then path, then name");
        set("http://a.example/", "empty=");
        Check.contains(store.getCookies(), "empty=", "empty values are kept");
    }

    /**
     * Malformed cookies are ignored without error: no equals sign, empty name, control characters, oversize.
     * <p>
     * 잘못된 쿠키는 오류 없이 무시한다. 등호 없음, 빈 이름, 제어 문자, 크기 초과.
     */
    public void testMalformedCookiesAreIgnored() {
        Check.isFalse(set("http://m.example/", "novalue"), "no equals sign");
        Check.isFalse(set("http://m.example/", "=v"), "empty name");
        Check.isFalse(set("http://m.example/", "a=b" + (char) 1 + "c"), "control character");
        char[] big = new char[5000];
        Arrays.fill(big, 'x');
        Check.isFalse(set("http://m.example/", "big=" + new String(big)), "oversize");
        Check.isTrue(set("http://m.example/", "ok=1; Unknown=attr; Path=relative"), "unknown attributes and bad Path are ignored");
        Check.equal("ok=1", store.getCookieHeader("http://m.example/"), "bad Path falls back to the default path");
        Check.isFalse(set("not a url", "k=v"), "invalid URL");
    }

    /**
     * Stores are independent per component and can be cleared.
     * <p>
     * 저장소는 컴포넌트마다 독립적이며 비울 수 있다.
     */
    public void testStoresAreIndependentAndClearable() {
        CookieStore other = new CookieStore();
        set("http://i.example/", "k=v");
        Check.equal("", other.getCookies(), "another store is empty");
        store.clear();
        Check.equal("", store.getCookies(), "cleared");
        Check.isNull(store.getCookieHeader("http://i.example/"), "nothing is sent after clearing");
    }
}
