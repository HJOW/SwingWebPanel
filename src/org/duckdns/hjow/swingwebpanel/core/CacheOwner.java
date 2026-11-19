package org.duckdns.hjow.swingwebpanel.core;

/**
 * Implemented by a {@link PageLoader} that keeps caches for its component (the HTTP cache of the network loader, later also caches of
 * decoded resources). {@link BrowserSession#clean()} and {@link BrowserSession#dispose()} call {@link #clearCache()} so that "clean" really
 * removes every cache of the component (plan items P01-15, P19-05, P21-02).
 * <p>
 * 컴포넌트를 위해 캐시를 가진 {@link PageLoader} 가 구현한다(네트워크 로더의 HTTP 캐시, 이후 디코딩된 리소스의 캐시). {@link BrowserSession#clean()} 과
 * {@link BrowserSession#dispose()} 가 {@link #clearCache()} 를 호출하므로 "clean" 이 컴포넌트의 모든 캐시를 실제로 지운다(계획 P01-15, P19-05, P21-02).
 */
public interface CacheOwner {

    /**
     * Removes everything from the caches of the component. Must be safe to call from any thread.
     * <p>
     * 컴포넌트의 캐시를 모두 비운다. 어느 스레드에서나 호출해도 안전해야 한다.
     */
    void clearCache();
}
