package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * A place where script runs: the page (its realm is owned by the document thread) or a dedicated worker (a realm of its own on a thread of
 * its own). The messaging code works with this interface only, so a message can be delivered to any context without sharing a script object:
 * it is posted as a task and the context runs it on the thread that owns its realm.
 * <p>
 * 스크립트가 실행되는 곳: 페이지(realm 은 문서 스레드가 소유) 또는 전용 worker(자기 스레드의 자기 realm). 메시징 코드는 이 인터페이스만 쓰므로 스크립트
 * 객체를 공유하지 않고도 어느 문맥에든 메시지를 전달할 수 있다. 메시지는 작업으로 올려 보내고 문맥이 자기 realm 을 소유한 스레드에서 실행한다.
 */
public interface ScriptContext {

    /**
     * Returns the realm of the context; touch it only from a task of the context.
     * <p>
     * 문맥의 realm 을 반환한다. 문맥의 작업 안에서만 만진다.
     *
     * @return the realm / realm
     */
    JsRealm getRealm();

    /**
     * Returns the DOM bridge of the context (event interfaces and wrappers); touch it only from a task of the context.
     * <p>
     * 문맥의 DOM 브리지(이벤트 인터페이스와 래퍼)를 반환한다. 문맥의 작업 안에서만 만진다.
     *
     * @return the bridge / 브리지
     */
    DomBridge getBridge();

    /**
     * Posts a task that runs later, as a task of this context (on the thread that owns its realm, followed by the microtask checkpoint). It
     * never runs while the caller runs, and it is dropped if the context has ended. Callable from any thread.
     * <p>
     * 나중에 이 문맥의 작업으로(realm 을 소유한 스레드에서, 마이크로태스크 검사점이 뒤따르며) 실행할 작업을 올린다. 호출자가 실행되는 동안에는 절대
     * 실행되지 않으며 문맥이 끝났으면 버려진다. 어느 스레드에서나 부를 수 있다.
     *
     * @param task the work / 작업
     */
    void post(Runnable task);

    /**
     * Tells whether the context still runs scripts. Callable from any thread.
     * <p>
     * 문맥이 아직 스크립트를 실행하는지 알려 준다. 어느 스레드에서나 부를 수 있다.
     *
     * @return true while it runs / 실행 중이면 true
     */
    boolean isActive();

    /**
     * Returns the serialized origin of the context (<code>scheme://host:port</code>, or <code>null</code> for an opaque origin).
     * <p>
     * 문맥의 직렬화된 오리진(<code>scheme://host:port</code>, 불투명 오리진이면 <code>null</code>)을 반환한다.
     *
     * @return the origin text / 오리진 문자열
     */
    String getOrigin();

    /**
     * Returns the handler that receives the failures of the listeners of the event targets of this context. Script errors never get there
     * (they are reported through the DOM bridge); what does is an abort of a running listener (a limit, a cancellation) and a defect, which a
     * worker reports to its page and a page prints.
     * <p>
     * 이 문맥의 이벤트 대상의 리스너 실패를 받는 핸들러를 반환한다. 스크립트 오류는 여기까지 오지 않는다(DOM 브리지로 보고된다). 여기 오는 것은 실행 중이던
     * 리스너의 중단(제한, 취소)과 결함이며, worker 는 이를 페이지에 보고하고 페이지는 출력한다.
     *
     * @return the handler / 핸들러
     */
    ListenerErrorHandler getListenerErrors();
}

