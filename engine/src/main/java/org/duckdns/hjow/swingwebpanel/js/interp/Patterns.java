package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;

/**
 * Destructuring and binding initialization (ECMA-262 6th edition 12.14.5, 13.3.3): array patterns that read an iterator and
 * close it when they finish early, object patterns, default values, rest elements and the three target modes: initialize a
 * lexical binding, assign to a variable, or assign to arbitrary targets of a destructuring assignment.
 * <p>
 * 구조 분해와 바인딩 초기화(ECMA-262 6판 12.14.5, 13.3.3): 이터레이터를 읽고 일찍 끝나면 닫는 배열 패턴, 객체 패턴, 기본값,
 * 나머지 요소, 그리고 세 가지 대상 모드: 렉시컬 바인딩 초기화, 변수 대입, 구조 분해 대입의 임의 대상에 대입.
 */
final class Patterns {

    /**
     * Mode: initialize the binding of a let, const, class, parameter or catch name in the given environment.
     * <p>
     * 모드: 주어진 환경에서 let, const, class, 매개변수, catch 이름의 바인딩을 초기화한다.
     */
    static final int INIT = 0;

    /**
     * Mode: assign to the resolved variable or property (var declarations and destructuring assignment).
     * <p>
     * 모드: 찾은 변수나 프로퍼티에 대입한다(var 선언과 구조 분해 대입).
     */
    static final int ASSIGN = 1;

    /**
     * The interpreter.
     * <p>
     * 인터프리터.
     */
    private final Interpreter in;

    /**
     * The realm that owns this component.
     * <p>
     * 이 구성 요소를 소유한 realm.
     */
    private final JsRealm realm;

    /**
     * Creates the helper.
     * <p>
     * 도우미를 만든다.
     *
     * @param in the interpreter / 인터프리터
     */
    Patterns(Interpreter in) {
        this.in = in;
        this.realm = in.realm;
    }

    /**
     * Binds a value to a target: an identifier, a member expression (assignment mode) or a pattern.
     * <p>
     * 값을 대상에 바인딩한다: 식별자, 멤버 표현식(대입 모드), 패턴.
     *
     * @param target the target node / 대상 노드
     * @param value  the value / 값
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param mode   {@link #INIT} or {@link #ASSIGN} / {@link #INIT} 또는 {@link #ASSIGN}
     */
    void bind(Node target, JsValue value, Environment env, Ctx ctx, int mode) {
        switch (target.type) {
        case NodeType.IDENT:
            bindName(target.name, value, env, ctx, mode);
            return;
        case NodeType.MEMBER:
            assignMember(target, value, env, ctx);
            return;
        case NodeType.ARRAY_PATTERN:
            bindArray(target, value, env, ctx, mode);
            return;
        case NodeType.OBJECT_PATTERN:
            bindObject(target, value, env, ctx, mode);
            return;
        case NodeType.DEFAULT:
            bindElement(target, value, env, ctx, mode);
            return;
        default:
            throw JsException.syntaxError("Invalid destructuring target");
        }
    }

    /**
     * Binds an element that may carry a default value (<code>target = default</code>).
     * <p>
     * 기본값(<code>target = default</code>)을 가질 수 있는 요소를 바인딩한다.
     *
     * @param elem  the element: a target or a DEFAULT node / 요소: 대상 또는 DEFAULT 노드
     * @param value the value / 값
     * @param env   the environment / 환경
     * @param ctx   the context / 문맥
     * @param mode  the mode / 모드
     */
    void bindElement(Node elem, JsValue value, Environment env, Ctx ctx, int mode) {
        if (elem.type == NodeType.DEFAULT) {
            JsValue v = value;
            if (v.isUndefined()) {
                Node def = elem.b;
                if (elem.a.type == NodeType.IDENT && in.ex.isAnonymous(def)) {
                    v = in.ex.evalNamed(def, elem.a.name, env, ctx);
                } else {
                    v = in.ex.eval(def, env, ctx);
                }
            }
            bind(elem.a, v, env, ctx, mode);
        } else {
            bind(elem, value, env, ctx, mode);
        }
    }

    /**
     * Initializes or assigns a named binding.
     * <p>
     * 이름 있는 바인딩을 초기화하거나 대입한다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     * @param env   the environment / 환경
     * @param ctx   the context / 문맥
     * @param mode  the mode / 모드
     */
    void bindName(String name, JsValue value, Environment env, Ctx ctx, int mode) {
        if (mode == INIT) {
            Environment e = env.own(name) != null ? env : env.lookup(name);
            Binding b = e == null ? null : e.own(name);
            if (b == null) {
                throw JsException.referenceError(name + " is not defined");
            }
            if (!b.initialized) {
                b.value = value;
                b.initialized = true;
            } else {
                b.value = value;
            }
            return;
        }
        Environment e = env.lookup(name);
        if (e == null) {
            if (ctx.strict) {
                throw JsException.referenceError(name + " is not defined");
            }
            realm.getGlobalObject().set(name, value);
        } else {
            e.set(name, value, ctx.strict);
        }
    }

    /**
     * Assigns to a member expression target of a destructuring assignment.
     * <p>
     * 구조 분해 대입의 멤버 표현식 대상에 대입한다.
     *
     * @param t     the MEMBER node / MEMBER 노드
     * @param value the value / 값
     * @param env   the environment / 환경
     * @param ctx   the context / 문맥
     */
    private void assignMember(Node t, JsValue value, Environment env, Ctx ctx) {
        if (t.a.type == NodeType.SUPER) {
            throw JsException.syntaxError("Unsupported destructuring target 'super.x'");
        }
        JsValue base = in.ex.eval(t.a, env, ctx);
        if (t.flag) {
            JsValue key = in.ex.eval(t.b, env, ctx);
            in.ex.putElement(base, key, value, ctx.strict);
        } else {
            in.ex.putV(base, t.name, value, ctx.strict);
        }
    }

    /**
     * Binds an array pattern: reads an iterator and closes it if the pattern does not consume it entirely.
     * <p>
     * 배열 패턴을 바인딩한다: 이터레이터를 읽고, 패턴이 모두 소비하지 않으면 닫는다.
     *
     * @param pattern the ARRAY_PATTERN node / ARRAY_PATTERN 노드
     * @param value   the value to destructure / 구조 분해할 값
     * @param env     the environment / 환경
     * @param ctx     the context / 문맥
     * @param mode    the mode / 모드
     */
    private void bindArray(Node pattern, JsValue value, Environment env, Ctx ctx, int mode) {
        JsObject it = IteratorHelper.getIterator(realm, value);
        boolean[] done = new boolean[1];
        try {
            // 패턴의 요소를 앞에서부터 반복자의 값과 짝지어 바인딩한다.
            for (int i = 0; i < pattern.list.length; i++) {
                Node e = pattern.list[i];
                if (e == null) {
                    nextValue(it, done);
                } else if (e.type == NodeType.REST) {
                    List<JsValue> rest = new ArrayList<JsValue>();
                    // 이터레이터가 끝날 때까지 나머지 값을 모은다.
                    while (!done[0]) {
                        JsValue v = nextValue(it, done);
                        if (!done[0]) {
                            rest.add(v);
                        }
                    }
                    bind(e.a, realm.newArray(rest), env, ctx, mode);
                } else {
                    JsValue v = nextValue(it, done);
                    bindElement(e, v == null ? Js.UNDEFINED : v, env, ctx, mode);
                }
            }
        } catch (JsException ex) {
            if (!done[0]) {
                IteratorHelper.closeQuietly(it);
            }
            throw ex;
        } catch (GeneratorReturn gr) {
            if (!done[0]) {
                IteratorHelper.closeQuietly(it);
            }
            throw gr;
        }
        if (!done[0]) {
            IteratorHelper.close(it);
        }
    }

    /**
     * Steps the iterator; marks it done when it is exhausted or throws.
     * <p>
     * 이터레이터를 한 걸음 진행한다. 소진되었거나 예외가 나면 끝난 것으로 표시한다.
     *
     * @param it   the iterator / 이터레이터
     * @param done element 0 is set to true when finished / 끝나면 0번 원소를 true 로 한다
     * @return the value, or undefined if the iterator is (now) done / 값. 이터레이터가 끝났으면 undefined
     */
    private JsValue nextValue(JsObject it, boolean[] done) {
        if (done[0]) {
            return Js.UNDEFINED;
        }
        JsObject r;
        try {
            r = IteratorHelper.step(it);
        } catch (JsException e) {
            done[0] = true;
            throw e;
        }
        if (r == null) {
            done[0] = true;
            return Js.UNDEFINED;
        }
        try {
            return IteratorHelper.value(r);
        } catch (JsException e) {
            done[0] = true;
            throw e;
        }
    }

    /**
     * Binds an object pattern.
     * <p>
     * 객체 패턴을 바인딩한다.
     *
     * @param pattern the OBJECT_PATTERN node / OBJECT_PATTERN 노드
     * @param value   the value to destructure / 구조 분해할 값
     * @param env     the environment / 환경
     * @param ctx     the context / 문맥
     * @param mode    the mode / 모드
     */
    private void bindObject(Node pattern, JsValue value, Environment env, Ctx ctx, int mode) {
        if (value.isNullOrUndefined()) {
            throw JsException.typeError("Cannot destructure '" + value + "' as it is " + value + ".");
        }
        Set<Object> excluded = new HashSet<Object>();
        // 각 프로퍼티의 키를 정하고 값을 읽어 대상에 바인딩한다.
        for (int i = 0; i < pattern.list.length; i++) {
            Node p = pattern.list[i];
            if (p.type == NodeType.REST) {
                JsObject source = Ops.toObject(realm, value);
                JsObject rest = realm.newObject();
                List<Object> keys = source.ownKeys();
                // 제외하지 않은 열거 가능 자체 프로퍼티만 getter 를 호출해 새 객체에 복사한다.
                for (int k = 0; k < keys.size(); k++) {
                    Object key = keys.get(k);
                    if (excluded.contains(key)) {
                        continue;
                    }
                    PropertyDescriptor desc = source.getOwnProperty(key);
                    if (desc != null && Boolean.TRUE.equals(desc.enumerable)) {
                        rest.createDataPropertyOrThrow(key, source.get(key, source));
                    }
                }
                bind(p.a, rest, env, ctx, mode);
                continue;
            }
            Object key = p.flag ? Ops.toPropertyKey(in.ex.eval(p.a, env, ctx)) : p.a.name;
            excluded.add(key);
            JsValue v = in.ex.getV(value, key);
            bindElement(p.b, v, env, ctx, mode);
        }
    }
}
