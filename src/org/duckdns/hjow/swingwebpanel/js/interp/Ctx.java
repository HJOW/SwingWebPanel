package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * The execution context of one function activation, script, module or eval code (ECMA-262 6th edition 8.3): strictness,
 * the variable environment that receives <code>var</code> declarations, the running function and, for generator bodies, the
 * state needed to resume.
 * <p>
 * 함수 활성, 스크립트, 모듈, eval 코드 하나의 실행 문맥(ECMA-262 6판 8.3): 엄격 여부, <code>var</code> 선언을 받는 변수 환경,
 * 실행 중인 함수, 제너레이터 본문에서는 재개에 필요한 상태.
 */
final class Ctx {

    /**
     * True for strict mode code.
     * <p>
     * 엄격 모드 코드이면 true.
     */
    final boolean strict;

    /**
     * The variable environment (target of var declarations and eval-created functions).
     * <p>
     * 변수 환경(var 선언과 eval 이 만든 함수의 대상).
     */
    Environment varEnv;

    /**
     * The function being run, or null for scripts and modules.
     * <p>
     * 실행 중인 함수. 스크립트와 모듈이면 null.
     */
    final ScriptFunction function;

    /**
     * The function environment of the activation (holds this and new.target), or null for scripts.
     * <p>
     * 활성의 함수 환경(this 와 new.target 을 가짐). 스크립트이면 null.
     */
    Environment funcEnv;

    /**
     * The generator state when running a generator body, otherwise null.
     * <p>
     * 제너레이터 본문을 실행 중이면 제너레이터 상태, 아니면 null.
     */
    GenState gen;

    /**
     * True if a call in tail position of this function body may be turned into a tail call (strict mode function called through
     * [[Call]], not a generator, ES2015 14.6).
     * <p>
     * 이 함수 본문의 꼬리 위치 호출을 꼬리 호출로 바꿀 수 있으면 true([[Call]] 로 호출된 엄격 모드 함수이며 제너레이터가 아님, ES2015 14.6).
     */
    boolean tailAllowed;

    /**
     * Nesting depth of statements in which a call is never in tail position (try statements and for-in/for-of loops, which
     * must clean up after the call returns).
     * <p>
     * 호출이 꼬리 위치가 될 수 없는 문장(호출이 돌아온 뒤 정리가 필요한 try 문과 for-in/for-of 반복문)의 중첩 깊이.
     */
    int noTail;

    /**
     * The function of a pending tail call set by the return statement, or null.
     * <p>
     * return 문이 지정한 대기 중인 꼬리 호출의 함수. 없으면 null.
     */
    ScriptFunction tailTarget;

    /**
     * The this value of the pending tail call.
     * <p>
     * 대기 중인 꼬리 호출의 this 값.
     */
    JsValue tailThis;

    /**
     * The arguments of the pending tail call.
     * <p>
     * 대기 중인 꼬리 호출의 인수.
     */
    JsValue[] tailArgs;

    /**
     * Creates a context.
     * <p>
     * 문맥을 만든다.
     *
     * @param strict   strict mode flag / 엄격 모드 여부
     * @param varEnv   the variable environment / 변수 환경
     * @param function the function or null / 함수 또는 null
     */
    Ctx(boolean strict, Environment varEnv, ScriptFunction function) {
        this.strict = strict;
        this.varEnv = varEnv;
        this.function = function;
    }
}
