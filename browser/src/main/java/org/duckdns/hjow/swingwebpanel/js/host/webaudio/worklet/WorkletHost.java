package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;

/**
 * What a {@link WorkletScope} needs from the page that owns it: the page (the environment for the request layer, the console, the address that relative module URLs resolve
 * against) and the way to run something as a task of the page, on the thread that owns the page's script realm.
 * <p>
 * {@link WorkletScope} 가 자신을 소유한 페이지에 필요로 하는 것: 페이지(요청 계층 환경, 콘솔, 상대 모듈 URL 의 기준 주소)와, 페이지의 스크립트 realm 을 소유한 스레드에서 페이지의 작업으로 무언가를
 * 실행하는 방법.
 */
public interface WorkletHost {

    /**
     * Returns the page.
     * <p>
     * 페이지를 반환한다.
     *
     * @return the page / 페이지
     */
    PageScripts getPage();

    /**
     * Posts a task to the page: it runs later as a task of the page, on the thread that owns the page's realm, followed by the microtask checkpoint. It is dropped once the page was
     * disposed. Callable from any thread, and never runs while the caller runs.
     * <p>
     * 페이지에 작업을 올린다: 나중에 페이지의 작업으로, 페이지 realm 을 소유한 스레드에서, 마이크로태스크 검사점이 뒤따르며 실행된다. 페이지가 폐기된 뒤에는 버려진다. 어느 스레드에서나 부를 수
     * 있으며 호출자가 실행되는 동안에는 절대 실행되지 않는다.
     *
     * @param task the work / 작업
     */
    void postToPage(Runnable task);

    /**
     * Tells whether the page lives and runs scripts.
     * <p>
     * 페이지가 살아 있고 스크립트를 실행하는지 알려 준다.
     *
     * @return true while it does / 그렇다면 true
     */
    boolean isPageActive();
}
