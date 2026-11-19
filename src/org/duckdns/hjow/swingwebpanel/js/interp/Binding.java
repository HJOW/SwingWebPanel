package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * A binding of a name to a value in a declarative environment record (ECMA-262 6th edition 8.1.1.1). It tracks the
 * temporal dead zone (uninitialized), immutability (const, import, function name) and, for module imports, an indirection
 * to the exporting module's binding so that imports are live.
 * <p>
 * 선언적 환경 레코드에서 이름과 값의 바인딩(ECMA-262 6판 8.1.1.1). 일시적 사각지대(초기화 전), 불변(const, import, 함수 이름),
 * 모듈 import 의 경우 export 하는 모듈 바인딩으로의 간접 참조(import 가 실시간 연결이 되도록)를 추적한다.
 */
final class Binding {

    /**
     * The current value (meaningful once initialized).
     * <p>
     * 현재 값(초기화된 뒤에 의미가 있다).
     */
    JsValue value;

    /**
     * False while the binding is in the temporal dead zone.
     * <p>
     * 일시적 사각지대에 있는 동안 false.
     */
    boolean initialized;

    /**
     * True if the binding can be assigned.
     * <p>
     * 대입할 수 있으면 true.
     */
    boolean mutable = true;

    /**
     * True if assignment to an immutable binding is silently ignored in sloppy mode (the name binding of a function expression).
     * <p>
     * 불변 바인딩에 대한 대입이 느슨한 모드에서 조용히 무시되면 true(함수 표현식의 이름 바인딩).
     */
    boolean silent;

    /**
     * True if the binding may be deleted (bindings created by eval code).
     * <p>
     * 삭제할 수 있으면 true(eval 코드가 만든 바인딩).
     */
    boolean deletable;

    /**
     * True for bindings of let, const and class declarations (used to detect conflicts with var declarations of eval code).
     * <p>
     * let, const, class 선언의 바인딩이면 true(eval 코드의 var 선언과 충돌하는지 검사하는 데 쓴다).
     */
    boolean lexical;

    /**
     * For import bindings: the binding in the exporting module's environment (null otherwise).
     * <p>
     * import 바인딩의 경우 export 하는 모듈 환경의 바인딩(아니면 null).
     */
    Binding link;

    /**
     * Reads the value, following import links; throws a ReferenceError inside the temporal dead zone.
     * <p>
     * 값을 읽는다. import 연결을 따라가며, 일시적 사각지대 안이면 ReferenceError 를 던진다.
     *
     * @param name the name for error messages / 오류 메시지용 이름
     * @return the value / 값
     */
    JsValue read(String name) {
        Binding b = this;
        // import 바인딩이면 실제 export 바인딩까지 따라간다.
        while (b.link != null) {
            b = b.link;
        }
        if (!b.initialized) {
            throw JsException.referenceError("Cannot access '" + name + "' before initialization");
        }
        return b.value;
    }
}
