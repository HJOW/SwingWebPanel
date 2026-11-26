package org.duckdns.hjow.swingwebpanel;

import org.duckdns.hjow.swingwebpanel.net.CookieStore;

/**
 * A cookie store that several {@link SwingWebPanel} components share (extension, API contract 5.24). By default every component has a
 * store of its own, so a login in one component is not seen by another. A host that shows several pages of one user (the tabs and
 * windows of a browser) creates one jar and passes it to every component:
 * <pre>
 * CookieJar jar = new CookieJar();
 * SwingWebPanel first = new SwingWebPanel(jar);
 * SwingWebPanel second = new SwingWebPanel(jar);   // sees the cookies that pages of the first one set
 * jar.clear();                                     // forgets the cookies of all of them
 * </pre>
 * The jar lives as long as something refers to it: disposing a component does not clear a shared jar. The methods may be called from any
 * thread.
 * <p>
 * 여러 {@link SwingWebPanel} 컴포넌트가 함께 쓰는 쿠키 저장소(확장, API 계약 5.24). 기본적으로 컴포넌트마다 자기 저장소가 있어서 한 컴포넌트의
 * 로그인이 다른 컴포넌트에 보이지 않는다. 한 사용자의 여러 페이지를 보여 주는 호스트(브라우저의 탭과 창)는 저장소 하나를 만들어 모든 컴포넌트에
 * 넘긴다(위 예). 저장소는 참조되는 동안 살아 있다. 컴포넌트를 폐기해도 공유 저장소는 비워지지 않는다. 메서드는 어느 스레드에서나 부를 수 있다.
 */
public final class CookieJar {

    /** The cookies; the components that share the jar use this store. / 쿠키. 저장소를 공유하는 컴포넌트들이 이 객체를 쓴다. */
    private final CookieStore store = new CookieStore();

    /**
     * Creates an empty jar.
     * <p>
     * 빈 저장소를 만든다.
     */
    public CookieJar() {
    }

    /**
     * Returns the store for the components of this package.
     * <p>
     * 이 패키지의 컴포넌트가 쓸 저장소를 반환한다.
     *
     * @return the store / 저장소
     */
    CookieStore store() {
        return store;
    }

    /**
     * Returns all cookies of the jar that have not expired as one string "name=value; name=value", in the format and order of
     * {@link SwingWebPanel#getCookies()}.
     * <p>
     * 저장소의 만료되지 않은 쿠키 전부를 "name=value; name=value" 한 문자열로 반환한다. 형식과 순서는 {@link SwingWebPanel#getCookies()} 와 같다.
     *
     * @return the cookies, empty if there are none / 쿠키. 없으면 빈 문자열
     */
    public String getCookies() {
        return store.getCookies();
    }

    /**
     * Removes all cookies of the jar, for every component that shares it. Pages that are shown keep running; their next requests are
     * sent without the removed cookies.
     * <p>
     * 저장소의 모든 쿠키를 지운다. 저장소를 공유하는 모든 컴포넌트에 적용된다. 보이는 페이지는 계속 실행되며 이후의 요청은 지운 쿠키 없이 나간다.
     */
    public void clear() {
        store.clear();
    }
}
