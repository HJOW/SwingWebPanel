package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Installer of the object-model built-ins: Proxy (26.2), Reflect (26.1), Promise (25.4) and the Annex B global functions
 * <code>escape</code> and <code>unescape</code> (B.2.1). The engine core loads it by class name after the other library installers, so it
 * may rely on Object.prototype, Function.prototype, the Symbol well-known symbols and the iterator helpers of the core.
 * Hosts: set the job queue with {@link PromiseJobs#setQueue(JsRealm, org.duckdns.hjow.swingwebpanel.js.JobQueue)} (or provide one on the realm) and
 * observe unhandled rejections with {@link PromiseJobs#setRejectionTracker(JsRealm, PromiseRejectionTracker)}.
 * <p>
 * 객체 모델 내장 객체의 설치기: Proxy(26.2), Reflect(26.1), Promise(25.4), 부속서 B 의 전역 함수 <code>escape</code>,
 * <code>unescape</code>(B.2.1). 엔진 코어가 다른 라이브러리 설치기 뒤에 클래스 이름으로 불러오므로 코어의 Object.prototype,
 * Function.prototype, Symbol 의 잘 알려진 심볼, 이터레이터 도우미를 쓸 수 있다.
 * 호스트: 작업 큐는 {@link PromiseJobs#setQueue(JsRealm, org.duckdns.hjow.swingwebpanel.js.JobQueue)} 로 지정하고(또는 realm 이 제공하고),
 * 처리되지 않은 거부는 {@link PromiseJobs#setRejectionTracker(JsRealm, PromiseRejectionTracker)} 로 관찰한다.
 */
public final class MetaLibInstaller implements BuiltinInstaller {

    /**
     * Creates the installer (public no-argument constructor, required by the by-name loading of the core).
     * <p>
     * 설치기를 만든다(코어의 이름 기반 적재가 요구하는 public 인수 없는 생성자).
     */
    public MetaLibInstaller() {
    }

    /**
     * Installs Proxy, Reflect, Promise, escape and unescape into the realm.
     * <p>
     * Proxy, Reflect, Promise, escape, unescape 를 realm 에 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(JsRealm realm) {
        ProxyBuiltins.install(realm);
        ReflectBuiltins.install(realm);
        PromiseBuiltins.install(realm);
        WeakReferenceBuiltins.install(realm);
        LegacyBuiltins.install(realm);
        EnumerateSupport.forInPrototype(realm);
    }
}
