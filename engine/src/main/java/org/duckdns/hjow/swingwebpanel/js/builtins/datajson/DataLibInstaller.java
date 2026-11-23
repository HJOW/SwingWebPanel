package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Installs the built-in objects of this package into a realm: Date, RegExp, JSON, Map, Set, WeakMap and WeakSet. The standard realm factory
 * loads this class by its name after the core, corelib and stringarray installers. Installation order inside this class does not matter
 * except that the intrinsics <code>%IteratorPrototype%</code> (created with the realm) and the global object must exist.
 * <p>
 * 이 패키지의 내장 객체 Date, RegExp, JSON, Map, Set, WeakMap, WeakSet 을 realm 에 설치한다. 표준 realm 팩터리가 core, corelib,
 * stringarray 설치기 다음에 이 클래스를 이름으로 불러 쓴다. realm 과 함께 만들어지는 <code>%IteratorPrototype%</code> 와 전역 객체가 있어야 하는
 * 것 외에는 이 클래스 안의 설치 순서가 중요하지 않다.
 */
public final class DataLibInstaller implements BuiltinInstaller {

    /**
     * Creates the installer (the standard realm factory calls this constructor by reflection).
     * <p>
     * 설치기를 만든다(표준 realm 팩터리가 리플렉션으로 이 생성자를 호출한다).
     */
    public DataLibInstaller() {
    }

    /**
     * Installs all objects.
     * <p>
     * 모든 객체를 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(JsRealm realm) {
        DateBuiltins.install(realm);
        RegExpBuiltins.install(realm);
        JsonBuiltins.install(realm);
        CollectionBuiltins.install(realm);
    }
}
