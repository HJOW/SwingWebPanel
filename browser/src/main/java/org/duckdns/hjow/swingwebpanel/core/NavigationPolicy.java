package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Decides whether a page may navigate to an address on its own (a link, a script, later a form). The component never leaves the schemes
 * of {@link SchemePolicy}, and a document from the network must not reach the application's class path: only a page that was itself
 * loaded from the class path (or a blank or generated page) may link to <code>classpath://</code>.
 * <p>
 * 페이지가 스스로(링크, 스크립트, 이후에는 폼) 어떤 주소로 이동해도 되는지 판정한다. 컴포넌트는 {@link SchemePolicy} 의 스킴 밖으로 나가지
 * 않으며, 네트워크의 문서가 애플리케이션의 클래스패스에 닿아서는 안 된다. 클래스패스에서 읽은(또는 빈, 생성된) 페이지만
 * <code>classpath://</code> 로 연결할 수 있다.
 */
final class NavigationPolicy {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private NavigationPolicy() {
    }

    /**
     * Tells whether a page may navigate to an address.
     * <p>
     * 페이지가 주소로 이동해도 되는지 알려 준다.
     *
     * @param sourceUrl the URL of the page that navigates / 이동하는 페이지의 URL
     * @param target    the absolute target address / 대상 절대 주소
     * @return true if the navigation may start / 이동을 시작해도 되면 true
     */
    static boolean allowed(String sourceUrl, String target) {
        if (target == null || !SchemePolicy.isNavigationAllowed(target)) {
            return false;
        }
        String source = UrlUtil.getScheme(sourceUrl);
        if ("blob".equals(source)) {
            source = Origin.of(sourceUrl).getScheme();
        }
        return !"classpath".equals(UrlUtil.getScheme(target)) || !("http".equals(source) || "https".equals(source) || "ftp".equals(source));
    }

    /**
     * Tells whether two addresses denote the same document and differ at most in the fragment, so that moving between them does not need
     * a new request.
     * <p>
     * 두 주소가 같은 문서를 가리키며 프래그먼트만 다른지 알려 준다. 그러면 둘 사이의 이동에는 새 요청이 필요 없다.
     *
     * @param current the current URL / 현재 URL
     * @param target  the target URL / 대상 URL
     * @return true if only the fragment differs / 프래그먼트만 다르면 true
     */
    static boolean sameDocument(String current, String target) {
        if (current == null || target == null) {
            return false;
        }
        return UrlUtil.stripFragment(current).equals(UrlUtil.stripFragment(target));
    }

    /**
     * Tells whether an address has a fragment (even an empty one: <code>page.html#</code>).
     * <p>
     * 주소에 프래그먼트가 있는지(<code>page.html#</code> 처럼 빈 것도) 알려 준다.
     *
     * @param url the address / 주소
     * @return true if it has a <code>#</code> / <code>#</code> 가 있으면 true
     */
    static boolean hasFragment(String url) {
        return url != null && url.indexOf('#') >= 0;
    }
}
