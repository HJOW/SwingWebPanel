package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpProgram;

/**
 * A RegExp instance: an ordinary object with the internal slots [[OriginalSource]], [[OriginalFlags]] and [[RegExpMatcher]] (the compiled
 * program of the package js.regexp) and the own data property <code>lastIndex</code> (writable, not enumerable, not configurable).
 * <p>
 * RegExp 인스턴스: 내부 슬롯 [[OriginalSource]], [[OriginalFlags]], [[RegExpMatcher]](js.regexp 패키지의 컴파일된 프로그램)와
 * 자체 데이터 프로퍼티 <code>lastIndex</code>(쓰기 가능, 열거 불가, 구성 불가)를 가진 일반 객체.
 */
final class RegExpObject extends JsObject {

    /**
     * The compiled program, or null while the object is allocated but not yet initialized.
     * <p>
     * 컴파일된 프로그램. 할당만 되고 아직 초기화되지 않았으면 null.
     */
    private RegExpProgram program;

    /**
     * The pattern text as given (before any escaping).
     * <p>
     * 주어진 그대로의 패턴 문자열(이스케이프 전).
     */
    private String source = "";

    /**
     * The flags text as given.
     * <p>
     * 주어진 그대로의 플래그 문자열.
     */
    private String originalFlags = "";

    /**
     * Creates an allocated RegExp object whose <code>lastIndex</code> property is 0.
     * <p>
     * <code>lastIndex</code> 프로퍼티가 0 인 할당된 RegExp 객체를 만든다.
     *
     * @param proto the prototype / 프로토타입
     */
    RegExpObject(JsObject proto) {
        super(proto);
        defineData("lastIndex", Js.num(0), true, false, false);
    }

    /**
     * Stores the compiled program and the original texts (RegExpInitialize after successful compilation).
     * <p>
     * 컴파일된 프로그램과 원래 문자열을 저장한다(컴파일 성공 후의 RegExpInitialize).
     *
     * @param p     the compiled program / 컴파일된 프로그램
     * @param src   the pattern text / 패턴 문자열
     * @param flags the flags text / 플래그 문자열
     */
    void initialize(RegExpProgram p, String src, String flags) {
        this.program = p;
        this.source = src;
        this.originalFlags = flags;
    }

    /**
     * Tells whether the object has been initialized (has a [[RegExpMatcher]]).
     * <p>
     * 객체가 초기화되었는지(= [[RegExpMatcher]] 가 있는지) 알려 준다.
     *
     * @return true once initialized / 초기화되었으면 true
     */
    boolean isInitialized() {
        return program != null;
    }

    /**
     * Returns the compiled program.
     * <p>
     * 컴파일된 프로그램을 반환한다.
     *
     * @return the program / 프로그램
     */
    RegExpProgram getProgram() {
        return program;
    }

    /**
     * Returns the pattern text as given.
     * <p>
     * 주어진 그대로의 패턴 문자열을 반환한다.
     *
     * @return the pattern / 패턴
     */
    String getSource() {
        return source;
    }

    /**
     * Returns the flags text as given.
     * <p>
     * 주어진 그대로의 플래그 문자열을 반환한다.
     *
     * @return the flags / 플래그
     */
    String getOriginalFlags() {
        return originalFlags;
    }

    /**
     * Returns the class name used by Object.prototype.toString before the toStringTag lookup.
     * <p>
     * toStringTag 조회 전에 Object.prototype.toString 이 쓰는 클래스 이름을 반환한다.
     *
     * @return "RegExp" / "RegExp"
     */
    public String className() {
        return "RegExp";
    }
}
