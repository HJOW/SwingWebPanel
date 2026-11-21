package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Installs the String and Array built-in objects into a realm. The realm's standard built-in list loads this class by its name
 * (<code>org.duckdns.hjow.swingwebpanel.js.builtins.stringarray.StringArrayInstaller</code>) after the minimal built-ins of the core and after the core library installer
 * (Object, Function, Number, Math ...). It replaces the minimal <code>Array.prototype</code> methods of the core with the complete ones, and creates the
 * String constructor, <code>String.prototype</code> methods, the Array constructor and the iterator prototypes
 * <code>%ArrayIteratorPrototype%</code> and <code>%StringIteratorPrototype%</code>.
 * <p>
 * String 과 Array 내장 객체를 realm 에 설치한다. realm 의 표준 내장 목록이 코어의 최소 내장 객체와 코어 라이브러리 설치기(Object, Function, Number, Math ...)
 * 뒤에 이 클래스를 이름(<code>org.duckdns.hjow.swingwebpanel.js.builtins.stringarray.StringArrayInstaller</code>)으로 적재한다. 코어의 최소 <code>Array.prototype</code>
 * 메서드를 완전한 것으로 교체하고, String 생성자, <code>String.prototype</code> 메서드, Array 생성자, 이터레이터 프로토타입
 * <code>%ArrayIteratorPrototype%</code>, <code>%StringIteratorPrototype%</code> 를 만든다.
 */
public final class StringArrayInstaller implements BuiltinInstaller {

    /**
     * Creates the installer (required for loading by name).
     * <p>
     * 설치기를 만든다(이름으로 적재하려면 필요하다).
     */
    public StringArrayInstaller() {
    }

    /**
     * Installs String and Array into the realm.
     * <p>
     * String 과 Array 를 realm 에 설치한다.
     *
     * @param realm the realm to install into / 설치할 realm
     */
    public void install(JsRealm realm) {
        StringBuiltins.install(realm);
        ArrayBuiltins.install(realm);
        ArrayMethods.install(realm);
    }
}
