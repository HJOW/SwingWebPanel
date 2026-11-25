package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution.ScriptScope;

/**
 * A generator object (ECMA-262 6th edition 25.3): the iterator returned by calling a generator function. Its body does not run
 * on a separate Java thread. {@link #resume(int, JsValue)} runs the body on the caller's stack until the next yield; the
 * saved {@link GenState} lets the next call walk the body again and continue exactly after the suspended yield. All state
 * lives in this object, so an abandoned generator is collected like any other object.
 * <p>
 * 제너레이터 객체(ECMA-262 6판 25.3): 제너레이터 함수를 호출하면 반환되는 이터레이터. 본문은 별도의 Java 스레드에서 실행되지 않는다.
 * {@link #resume(int, JsValue)} 은 호출자의 스택에서 다음 yield 까지 본문을 실행하며, 저장된 {@link GenState} 덕분에 다음 호출이
 * 본문을 다시 따라가 중단된 yield 바로 뒤에서 이어 간다. 모든 상태가 이 객체 안에 있으므로 버려진 제너레이터는 다른 객체처럼 수거된다.
 */
public final class GeneratorObject extends JsObject {

    /**
     * Resume request: next(value).
     * <p>
     * 재개 요청: next(value).
     */
    public static final int NEXT = GenState.NEXT;

    /**
     * Resume request: throw(value).
     * <p>
     * 재개 요청: throw(value).
     */
    public static final int THROW = GenState.THROW;

    /**
     * Resume request: return(value).
     * <p>
     * 재개 요청: return(value).
     */
    public static final int RETURN = GenState.RETURN;

    /**
     * State: created, body not started.
     * <p>
     * 상태: 만들어졌으나 본문이 시작되지 않음.
     */
    private static final int START = 0;

    /**
     * State: suspended at a yield.
     * <p>
     * 상태: yield 에서 중단됨.
     */
    private static final int SUSPENDED = 1;

    /**
     * State: the body is running.
     * <p>
     * 상태: 본문이 실행 중.
     */
    private static final int RUNNING = 2;

    /**
     * State: finished.
     * <p>
     * 상태: 끝남.
     */
    private static final int DONE = 3;

    /**
     * The interpreter.
     * <p>
     * 인터프리터.
     */
    private final Interpreter interp;

    /**
     * The generator function.
     * <p>
     * 제너레이터 함수.
     */
    private final ScriptFunction function;

    /**
     * The execution context of the activation (environments and resume state).
     * <p>
     * 활성의 실행 문맥(환경과 재개 상태).
     */
    private final Ctx ctx;

    /**
     * The lifecycle state.
     * <p>
     * 생명주기 상태.
     */
    private int state = START;

    /**
     * Creates a generator object.
     * <p>
     * 제너레이터 객체를 만든다.
     *
     * @param proto    the prototype / 프로토타입
     * @param interp   the interpreter / 인터프리터
     * @param function the generator function / 제너레이터 함수
     * @param ctx      the prepared context / 준비된 문맥
     */
    private GeneratorObject(JsObject proto, Interpreter interp, ScriptFunction function, Ctx ctx) {
        super(proto);
        this.interp = interp;
        this.function = function;
        this.ctx = ctx;
    }

    /**
     * Creates the generator object for a call of a generator function (after the arguments were bound).
     * <p>
     * 제너레이터 함수 호출에 대한 제너레이터 객체를 만든다(인수 바인딩이 끝난 뒤).
     *
     * @param in  the interpreter / 인터프리터
     * @param f   the generator function / 제너레이터 함수
     * @param ctx the prepared context / 준비된 문맥
     * @return the generator object / 제너레이터 객체
     */
    static JsObject create(Interpreter in, ScriptFunction f, Ctx ctx) {
        JsValue p = f.get("prototype");
        JsObject proto = p instanceof JsObject ? (JsObject) p : in.realm.getIntrinsic("%GeneratorPrototype%");
        return new GeneratorObject(proto, in, f, ctx);
    }

    /**
     * Returns "Generator".
     * <p>
     * "Generator" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Generator";
    }

    /**
     * Resumes the generator (GeneratorResume, GeneratorResumeAbrupt) and returns the iterator result object.
     * <p>
     * 제너레이터를 재개하고(GeneratorResume, GeneratorResumeAbrupt) 이터레이터 결과 객체를 반환한다.
     *
     * @param mode  {@link #NEXT}, {@link #THROW} or {@link #RETURN} / {@link #NEXT}, {@link #THROW}, {@link #RETURN} 중 하나
     * @param value the value passed to next, throw or return / next, throw, return 에 전달된 값
     * @return the result object <code>{ value, done }</code> / 결과 객체 <code>{ value, done }</code>
     */
    public JsObject resume(int mode, JsValue value) {
        JsRealm realm = interp.realm;
        if (state == RUNNING) {
            throw JsException.typeError("Generator is already running");
        }
        if (state == DONE) {
            if (mode == THROW) {
                throw new JsException(value);
            }
            return IteratorHelper.createResult(realm, mode == RETURN ? value : Js.UNDEFINED, true);
        }
        if (state == START) {
            if (mode == RETURN) {
                state = DONE;
                return IteratorHelper.createResult(realm, value, true);
            }
            if (mode == THROW) {
                state = DONE;
                throw new JsException(value);
            }
        }
        GenState gen = ctx.gen;
        gen.suspending = false;
        gen.resumeMode = mode;
        gen.resumeValue = value;
        ScriptScope previous = interp.enter(function.scriptSource);
        state = RUNNING;
        boolean suspended = false;
        try {
            JsValue ret = interp.runBody(function, ctx);
            return IteratorHelper.createResult(realm, ret, true);
        } catch (YieldSignal y) {
            suspended = true;
            JsValue yv = gen.yielded;
            gen.yielded = null;
            return gen.yieldedRaw ? (JsObject) yv : IteratorHelper.createResult(realm, yv, false);
        } catch (GeneratorReturn r) {
            return IteratorHelper.createResult(realm, r.value, true);
        } finally {
            interp.leave(previous);
            state = suspended ? SUSPENDED : DONE;
            if (!suspended) {
                gen.saved.clear();
            }
        }
    }
}
