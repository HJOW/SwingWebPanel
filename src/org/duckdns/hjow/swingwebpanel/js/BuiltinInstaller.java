package org.duckdns.hjow.swingwebpanel.js;

/**
 * Installs a group of built-in objects into a realm (for example "Math", or "Map and Set"). Implementations create the
 * constructor and prototype objects, define their properties and register them on the realm
 * (<code>realm.getGlobalObject().defineBuiltin("Math", math)</code> and, for objects needed elsewhere,
 * <code>realm.setIntrinsic("%MapPrototype%", proto)</code>).
 * Installers run once per realm, in the order given by the realm factory.
 * <p>
 * 내장 객체 묶음(예: "Math", "Map 과 Set")을 realm 에 설치한다. 구현체는 생성자와 프로토타입 객체를 만들고 프로퍼티를 정의한 뒤
 * realm 에 등록한다(<code>realm.getGlobalObject().defineBuiltin("Math", math)</code>, 다른 곳에서 필요한 객체는
 * <code>realm.setIntrinsic("%MapPrototype%", proto)</code>). 설치기는 realm 마다 한 번, realm 팩터리가 정한 순서로 실행된다.
 */
public interface BuiltinInstaller {

    /**
     * Installs the built-ins.
     * <p>
     * 내장 객체를 설치한다.
     *
     * @param realm the realm to populate / 채울 realm
     */
    void install(JsRealm realm);
}
