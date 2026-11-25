package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Installs the fundamental ECMAScript 2015 built-in objects of this package into a realm: the Error family, Object, the methods of
 * Function.prototype, Boolean, Symbol, the global functions and value properties, Number and Math. The engine's
 * <code>StandardBuiltins</code> loads this class by name after its own minimal versions of Object, Function, Error, Symbol and Array,
 * so the constructors and prototype members installed here replace the minimal ones (the prototype objects themselves are the realm's
 * own, so objects that already exist keep working). It can also be used on a bare <code>new JsRealm()</code>.
 * <p>
 * 이 패키지의 ECMAScript 2015 기본 내장 객체를 realm 에 설치한다: Error 계열, Object, Function.prototype 의 메서드, Boolean, Symbol, 전역 함수와
 * 전역 값 프로퍼티, Number, Math. 엔진의 <code>StandardBuiltins</code> 가 Object, Function, Error, Symbol, Array 의 자체 최소 구현을 설치한 뒤
 * 이 클래스를 이름으로 불러 오므로 여기서 설치하는 생성자와 프로토타입 멤버가 최소 구현을 대체한다(프로토타입 객체 자체는 realm 의 것이라
 * 이미 만들어진 객체는 계속 동작한다). 빈 <code>new JsRealm()</code> 에도 쓸 수 있다.
 */
public final class CoreLibInstaller implements BuiltinInstaller {

    /**
     * Creates the installer (required for loading by class name).
     * <p>
     * 설치기를 만든다(클래스 이름으로 불러 오는 데 필요).
     */
    public CoreLibInstaller() {
    }

    /**
     * Installs everything into the realm.
     * <p>
     * realm 에 모든 것을 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(JsRealm realm) {
        ErrorLib.install(realm);
        ObjectLib.install(realm);
        FunctionLib.install(realm);
        BooleanLib.install(realm);
        SymbolLib.install(realm);
        JsObject[] parsers = GlobalLib.install(realm);
        NumberLib.install(realm, parsers);
        BigIntLib.install(realm);
        MathLib.install(realm);
    }
}
