package org.duckdns.hjow.swingwebpanel.js;

/**
 * A bound function exotic object (ECMA-262 6th edition 9.4.1), the result of Function.prototype.bind. Calling it calls the
 * target function with the bound this value and the bound arguments followed by the call arguments.
 * <p>
 * 바운드 함수 이국적 객체(ECMA-262 6판 9.4.1). Function.prototype.bind 의 결과이다. 호출하면 바인딩된 this 와
 * 바인딩된 인수 뒤에 호출 인수를 붙여 대상 함수를 호출한다.
 */
public final class BoundFunction extends JsFunction {

    /**
     * The target function.
     * <p>
     * 대상 함수.
     */
    private final JsObject target;

    /**
     * The bound this value.
     * <p>
     * 바인딩된 this 값.
     */
    private final JsValue boundThis;

    /**
     * The bound leading arguments.
     * <p>
     * 바인딩된 앞쪽 인수.
     */
    private final JsValue[] boundArgs;

    /**
     * Creates a bound function. The name and length properties are set by the caller (Function.prototype.bind) because
     * they depend on the target's properties.
     * <p>
     * 바운드 함수를 만든다. name, length 프로퍼티는 대상의 프로퍼티에 따라 달라지므로 호출자(Function.prototype.bind)가 설정한다.
     *
     * @param realm     the realm / realm
     * @param target    target function / 대상 함수
     * @param boundThis bound this / 바인딩된 this
     * @param boundArgs bound arguments / 바인딩된 인수
     */
    public BoundFunction(JsRealm realm, JsObject target, JsValue boundThis, JsValue[] boundArgs) {
        super(realm, target.getPrototypeOf());
        this.target = target;
        this.boundThis = boundThis;
        this.boundArgs = boundArgs;
    }

    /**
     * Returns the target function.
     * <p>
     * 대상 함수를 반환한다.
     *
     * @return the target / 대상
     */
    public JsObject getTarget() {
        return target;
    }

    /**
     * A bound function is a constructor if its target is.
     * <p>
     * 대상이 생성자이면 바운드 함수도 생성자이다.
     *
     * @return true if constructor / 생성자이면 true
     */
    public boolean isConstructor() {
        return target.isConstructor();
    }

    /**
     * Calls the target with the bound this and arguments.
     * <p>
     * 바인딩된 this 와 인수로 대상을 호출한다.
     *
     * @param thisValue ignored / 무시됨
     * @param args      call arguments / 호출 인수
     * @return the result / 결과
     */
    public JsValue call(JsValue thisValue, JsValue[] args) {
        return target.call(boundThis, concat(args));
    }

    /**
     * Constructs the target with the bound arguments; new.target defaults to the target when it was this function.
     * <p>
     * 바인딩된 인수로 대상을 생성한다. new.target 이 이 함수였다면 대상으로 바꾼다.
     *
     * @param args      call arguments / 호출 인수
     * @param newTarget new.target / new.target
     * @return the new object / 새 객체
     */
    public JsObject construct(JsValue[] args, JsObject newTarget) {
        JsObject nt = newTarget == this ? target : newTarget;
        return target.construct(concat(args), nt);
    }

    /**
     * Prepends the bound arguments.
     * <p>
     * 바인딩된 인수를 앞에 붙인다.
     *
     * @param args call arguments / 호출 인수
     * @return the combined list / 합친 목록
     */
    private JsValue[] concat(JsValue[] args) {
        if (boundArgs.length == 0) {
            return args;
        }
        JsValue[] all = new JsValue[boundArgs.length + args.length];
        System.arraycopy(boundArgs, 0, all, 0, boundArgs.length);
        System.arraycopy(args, 0, all, boundArgs.length, args.length);
        return all;
    }
}
