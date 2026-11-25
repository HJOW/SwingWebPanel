package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution;

/**
 * A Promise JobCallback with optional HTML incumbent settings and active script.
 * <p>
 * HTML의 incumbent settings와 활성 스크립트를 선택적으로 담는 Promise JobCallback.
 */
final class PromiseJobCallback {

    /** The callable invoked by the job. / 작업이 호출하는 콜백. */
    private final JsValue callback;

    /** The HTML incumbent realm, or null for the default host. / HTML incumbent realm. 기본 호스트이면 null. */
    private final JsRealm incumbent;

    /** The active script or module key at registration. / 등록 시의 활성 스크립트 또는 모듈 키. */
    private final String source;

    /**
     * Creates a job callback with the host state captured at registration.
     * <p>
     * 등록 시의 호스트 상태를 보존한 작업 콜백을 만든다.
     *
     * @param realm the realm performing registration / 등록하는 realm
     * @param callback the callable / 호출할 값
     */
    PromiseJobCallback(JsRealm realm, JsValue callback) {
        this.callback = callback;
        // 브라우저는 등록 시 incumbent와 원문 키를 보존하고 독립 엔진은 기본 콜백 동작을 쓴다.
        if (realm.usesHtmlJobCallbacks()) {
            JsRealm current = RealmExecution.currentIncumbent();
            incumbent = current == null ? realm : current;
            source = RealmExecution.currentScriptSource();
        } else {
            incumbent = null;
            source = null;
        }
    }

    /**
     * Invokes the callback and restores the prior execution contexts even on abrupt completion.
     * <p>
     * 콜백을 호출한 뒤 예외가 발생해도 이전 실행 문맥을 복원한다.
     *
     * @param receiver this value / this 값
     * @param args the arguments / 인수
     * @return the callback's result / 콜백 결과
     */
    JsValue call(JsValue receiver, JsValue[] args) {
        if (incumbent == null) {
            return Ops.call(callback, receiver, args);
        }
        RealmExecution.CallbackScope previous = RealmExecution.enterJobCallback(incumbent, source);
        try {
            return Ops.call(callback, receiver, args);
        } finally {
            RealmExecution.restoreJobCallback(previous);
        }
    }
}
