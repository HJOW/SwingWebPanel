package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.Op;
import org.duckdns.hjow.swingwebpanel.js.ast.TemplateParts;

/**
 * Evaluates expressions (ECMA-262 6th edition clause 12). All evaluation goes through {@link #eval(Node, Environment, Ctx)}.
 * <p>
 * <b>Generators.</b> A node that contains a yield (<code>Node.hasYield</code>) is evaluated with a {@link NodeState}. Child
 * values that are already computed are memoized in the state in evaluation order ({@link #memo}); when a generator is resumed the
 * body is walked again and every node replays its memoized children and re-enters the one that was suspended. Nodes without a
 * yield run with a null state and pay nothing for this mechanism.
 * <p>
 * 표현식을 평가한다(ECMA-262 6판 12장). 모든 평가는 {@link #eval(Node, Environment, Ctx)} 를 거친다.
 * <p>
 * <b>제너레이터.</b> yield 를 포함한 노드(<code>Node.hasYield</code>)는 {@link NodeState} 와 함께 평가한다. 이미 계산한 자식 값은
 * 평가 순서대로 상태에 저장하며({@link #memo}), 제너레이터를 재개하면 본문을 다시 따라가면서 각 노드가 저장된 자식을 재사용하고
 * 중단된 자식으로 다시 들어간다. yield 가 없는 노드는 null 상태로 실행되어 이 장치의 비용을 내지 않는다.
 */
final class Expressions {

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
    final JsRealm realm;

    /**
     * Creates the evaluator.
     * <p>
     * 평가기를 만든다.
     *
     * @param in the interpreter / 인터프리터
     */
    Expressions(Interpreter in) {
        this.in = in;
        this.realm = in.realm;
    }

    // ------------------------------------------------------------------ dispatch and memo helpers

    /**
     * Evaluates an expression (GetValue of the resulting reference).
     * <p>
     * 표현식을 평가한다(결과 참조의 GetValue).
     *
     * @param n   the node / 노드
     * @param env the lexical environment / 렉시컬 환경
     * @param ctx the execution context / 실행 문맥
     * @return the value / 값
     */
    JsValue eval(Node n, Environment env, Ctx ctx) {
        if (n.hasYield) {
            GenState gen = ctx.gen;
            NodeState st = gen.enter(n);
            try {
                return evalNode(n, env, ctx, st);
            } finally {
                gen.leave(n);
            }
        }
        return evalNode(n, env, ctx, null);
    }

    /**
     * Evaluates a child, or replays its memoized value when the generator walks again.
     * <p>
     * 자식을 평가한다. 제너레이터가 다시 순회하는 중이면 저장된 값을 재사용한다.
     *
     * @param st    the node state or null / 노드 상태 또는 null
     * @param child the child node / 자식 노드
     * @param env   the environment / 환경
     * @param ctx   the context / 문맥
     * @return the value / 값
     */
    private JsValue memo(NodeState st, Node child, Environment env, Ctx ctx) {
        if (st == null) {
            return eval(child, env, ctx);
        }
        if (st.cursor < st.count) {
            return (JsValue) st.vals[st.cursor++];
        }
        JsValue v = eval(child, env, ctx);
        st.add(v);
        st.cursor++;
        return v;
    }

    /**
     * Tests whether the next memo slot already holds a value from before the suspension.
     * <p>
     * 다음 저장 칸에 중단 전의 값이 이미 있는지 검사한다.
     *
     * @param st the node state or null / 노드 상태 또는 null
     * @return true if the slot is filled / 칸이 차 있으면 true
     */
    private static boolean replay(NodeState st) {
        return st != null && st.cursor < st.count;
    }

    /**
     * Takes the next memoized object.
     * <p>
     * 저장된 다음 객체를 꺼낸다.
     *
     * @param st the node state / 노드 상태
     * @return the object / 객체
     */
    private static Object take(NodeState st) {
        return st.vals[st.cursor++];
    }

    /**
     * Memoizes a computed object in the next slot (no effect without a state).
     * <p>
     * 계산한 객체를 다음 칸에 저장한다(상태가 없으면 아무 일도 하지 않는다).
     *
     * @param st the node state or null / 노드 상태 또는 null
     * @param v  the object / 객체
     */
    private static void save(NodeState st, Object v) {
        if (st != null) {
            st.add(v);
            st.cursor++;
        }
    }

    // ------------------------------------------------------------------ main switch

    /**
     * Evaluates one node.
     * <p>
     * 노드 하나를 평가한다.
     *
     * @param n   the node / 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state (null unless the node contains a yield) / 노드 상태(yield 를 포함하지 않으면 null)
     * @return the value / 값
     */
    private JsValue evalNode(Node n, Environment env, Ctx ctx, NodeState st) {
        switch (n.type) {
        case NodeType.LITERAL:
            return (JsValue) n.value;
        case NodeType.IDENT:
            return identifierValue(n.name, env, ctx);
        case NodeType.THIS:
            return env.thisEnvironment().getThis();
        case NodeType.NEW_TARGET: {
            JsObject nt = env.thisEnvironment().newTarget;
            return nt == null ? Js.UNDEFINED : nt;
        }
        case NodeType.TEMPLATE:
            return evalTemplate(n, env, ctx, st);
        case NodeType.TAGGED_TEMPLATE:
            return evalTagged(n, env, ctx, st);
        case NodeType.REGEXP:
            return evalRegExp(n);
        case NodeType.ARRAY:
            return evalArray(n, env, ctx, st);
        case NodeType.OBJECT:
            return evalObject(n, env, ctx, st);
        case NodeType.FUNCTION:
            return evalFunction(n, env, null);
        case NodeType.CLASS:
            return in.cls.evaluate(n, env, ctx, null);
        case NodeType.UNARY:
            return evalUnary(n, env, ctx, st);
        case NodeType.UPDATE:
            return evalUpdate(n, env, ctx, st);
        case NodeType.BINARY: {
            JsValue l = memo(st, n.a, env, ctx);
            JsValue r = memo(st, n.b, env, ctx);
            return applyBinary(n.op, l, r);
        }
        case NodeType.LOGICAL: {
            JsValue l = memo(st, n.a, env, ctx);
            boolean truthy = Ops.toBoolean(l);
            if (n.op == Op.LOGAND ? !truthy : truthy) {
                return l;
            }
            return memo(st, n.b, env, ctx);
        }
        case NodeType.ASSIGN:
            return evalAssign(n, env, ctx, st);
        case NodeType.CONDITIONAL: {
            JsValue t = memo(st, n.a, env, ctx);
            return memo(st, Ops.toBoolean(t) ? n.b : n.c, env, ctx);
        }
        case NodeType.CALL:
            return evalCall(n, env, ctx, st);
        case NodeType.NEW:
            return evalNew(n, env, ctx, st);
        case NodeType.MEMBER:
            return evalMember(n, env, ctx, st);
        case NodeType.SEQUENCE: {
            JsValue last = Js.UNDEFINED;
            // 쉼표 식의 모든 식을 순서대로 평가하고 마지막 값을 돌려준다.
            for (int i = 0; i < n.list.length; i++) {
                last = memo(st, n.list[i], env, ctx);
            }
            return last;
        }
        case NodeType.YIELD:
            return n.flag ? evalYieldStar(n, env, ctx, st) : evalYield(n, env, ctx, st);
        default:
            throw JsException.syntaxError("Unsupported expression (node type " + n.type + ")", null, n.line, n.column);
        }
    }

    // ------------------------------------------------------------------ identifiers and members

    /**
     * Reads the value of an identifier reference.
     * <p>
     * 식별자 참조의 값을 읽는다.
     *
     * @param name the name / 이름
     * @param env  the environment / 환경
     * @param ctx  the context / 문맥
     * @return the value / 값
     */
    JsValue identifierValue(String name, Environment env, Ctx ctx) {
        Environment e = env.lookup(name);
        if (e == null) {
            throw JsException.referenceError(name + " is not defined");
        }
        return e.get(name, ctx.strict);
    }

    /**
     * GetV for any base value with a property key.
     * <p>
     * 임의의 기준 값에 대한 GetV.
     *
     * @param base the base value / 기준 값
     * @param key  the property key (String or symbol) / 프로퍼티 키(String 또는 심볼)
     * @return the value / 값
     */
    JsValue getV(JsValue base, Object key) {
        if (base instanceof JsObject) {
            return ((JsObject) base).get(key, base);
        }
        if (base instanceof JsString) {
            JsString s = (JsString) base;
            if (key instanceof String) {
                String k = (String) key;
                if (k.equals("length")) {
                    return JsNumber.of(s.length());
                }
                long idx = PropertyKeys.arrayIndex(k);
                if (idx >= 0 && idx < s.length()) {
                    return JsString.of(String.valueOf(s.charAt((int) idx)));
                }
            }
            return realm.getStringPrototype().get(key, base);
        }
        if (base instanceof JsNumber) {
            return realm.getNumberPrototype().get(key, base);
        }
        if (base instanceof JsBoolean) {
            return realm.getBooleanPrototype().get(key, base);
        }
        if (base instanceof JsSymbol) {
            return realm.getSymbolPrototype().get(key, base);
        }
        throw JsException.typeError("Cannot read property '" + PropertyKeys.describe(key) + "' of " + base);
    }

    /**
     * Reads <code>base[keyValue]</code> with fast paths for array and string indices.
     * <p>
     * 배열과 문자열 인덱스의 빠른 경로를 갖춘 <code>base[keyValue]</code> 읽기.
     *
     * @param base     the base value / 기준 값
     * @param keyValue the key value / 키 값
     * @return the value / 값
     */
    JsValue getElement(JsValue base, JsValue keyValue) {
        if (keyValue instanceof JsNumber) {
            double d = ((JsNumber) keyValue).getValue();
            int i = (int) d;
            if (i == d && i >= 0) {
                if (base instanceof JsArray) {
                    JsValue v = ((JsArray) base).getOwnElement(i);
                    if (v != null) {
                        return v;
                    }
                } else if (base instanceof JsString && i < ((JsString) base).length()) {
                    return JsString.of(String.valueOf(((JsString) base).charAt(i)));
                }
            }
        }
        if (base.isNullOrUndefined()) {
            throw JsException.typeError("Cannot read property '" + describeKey(keyValue) + "' of " + base);
        }
        return getV(base, Ops.toPropertyKey(keyValue));
    }

    /**
     * Describes a key value for an error message without running script code.
     * <p>
     * 스크립트 코드를 실행하지 않고 오류 메시지용으로 키 값을 설명한다.
     *
     * @param keyValue the key value / 키 값
     * @return the text / 문자열
     */
    private static String describeKey(JsValue keyValue) {
        return keyValue instanceof JsObject ? "[object]" : keyValue.toString();
    }

    /**
     * PutValue for a property reference.
     * <p>
     * 프로퍼티 참조의 PutValue.
     *
     * @param base   the base value / 기준 값
     * @param key    the property key / 프로퍼티 키
     * @param value  the value / 값
     * @param strict true in strict mode code / 엄격 모드 코드이면 true
     */
    void putV(JsValue base, Object key, JsValue value, boolean strict) {
        if (base instanceof JsObject) {
            if (!((JsObject) base).set(key, value, base) && strict) {
                throw JsException.typeError("Cannot assign to read only property '" + PropertyKeys.describe(key) + "' of object");
            }
            return;
        }
        if (base.isNullOrUndefined()) {
            throw JsException.typeError("Cannot set property '" + PropertyKeys.describe(key) + "' of " + base);
        }
        boolean ok = Ops.toObject(realm, base).set(key, value, base);
        if (!ok && strict) {
            throw JsException.typeError("Cannot create property '" + PropertyKeys.describe(key) + "' on " + base.typeOf() + " '" + base + "'");
        }
    }

    /**
     * Writes <code>base[keyValue] = value</code> with a fast path for existing array elements.
     * <p>
     * 기존 배열 요소의 빠른 경로를 갖춘 <code>base[keyValue] = value</code> 쓰기.
     *
     * @param base     the base value / 기준 값
     * @param keyValue the key value / 키 값
     * @param value    the value / 값
     * @param strict   true in strict mode code / 엄격 모드 코드이면 true
     */
    void putElement(JsValue base, JsValue keyValue, JsValue value, boolean strict) {
        if (base instanceof JsArray && keyValue instanceof JsNumber) {
            double d = ((JsNumber) keyValue).getValue();
            int i = (int) d;
            if (i == d && i >= 0 && ((JsArray) base).setOwnElement(i, value)) {
                return;
            }
        }
        if (base.isNullOrUndefined()) {
            throw JsException.typeError("Cannot set property '" + describeKey(keyValue) + "' of " + base);
        }
        putV(base, Ops.toPropertyKey(keyValue), value, strict);
    }

    /**
     * Returns the this value and home prototype for a super property access.
     * <p>
     * super 프로퍼티 접근에 쓰는 this 값을 반환한다.
     *
     * @param env the environment / 환경
     * @return the this value / this 값
     */
    private JsValue superThis(Environment env) {
        return env.thisEnvironment().getThis();
    }

    /**
     * Returns the object whose prototype chain starts the lookup of <code>super.x</code> (the home object's prototype).
     * <p>
     * <code>super.x</code> 조회가 시작되는 객체(홈 객체의 프로토타입)를 반환한다.
     *
     * @param env the environment / 환경
     * @return the prototype or null / 프로토타입 또는 null
     */
    private JsObject superBase(Environment env) {
        Environment te = env.thisEnvironment();
        if (te == null || te.function == null || te.function.homeObject == null) {
            throw JsException.syntaxError("'super' keyword unexpected here");
        }
        return te.function.homeObject.getPrototypeOf();
    }

    /**
     * Evaluates a member access expression.
     * <p>
     * 멤버 접근 표현식을 평가한다.
     *
     * @param n   the MEMBER node / MEMBER 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the value / 값
     */
    private JsValue evalMember(Node n, Environment env, Ctx ctx, NodeState st) {
        if (n.a.type == NodeType.SUPER) {
            JsValue thisV = superThis(env);
            Object key = n.flag ? Ops.toPropertyKey(memo(st, n.b, env, ctx)) : n.name;
            JsObject base = superBase(env);
            return base == null ? Js.UNDEFINED : base.get(key, thisV);
        }
        JsValue base = memo(st, n.a, env, ctx);
        if (n.flag) {
            JsValue key = memo(st, n.b, env, ctx);
            return getElement(base, key);
        }
        if (base.isNullOrUndefined()) {
            throw JsException.typeError("Cannot read property '" + n.name + "' of " + base);
        }
        return getV(base, n.name);
    }

    // ------------------------------------------------------------------ operators

    /**
     * Applies a binary operator to two evaluated operands.
     * <p>
     * 평가가 끝난 두 피연산자에 이항 연산자를 적용한다.
     *
     * @param op the operator code / 연산자 코드
     * @param l  the left operand / 왼쪽 피연산자
     * @param r  the right operand / 오른쪽 피연산자
     * @return the result / 결과
     */
    JsValue applyBinary(int op, JsValue l, JsValue r) {
        switch (op) {
        case Op.ADD: {
            JsValue sum = Ops.add(l, r);
            if (sum instanceof JsString) {
                in.limits.checkStringLength(((JsString) sum).length());
            }
            return sum;
        }
        case Op.SUB:
            return JsNumber.of(Ops.toNumber(l) - Ops.toNumber(r));
        case Op.MUL:
            return JsNumber.of(Ops.toNumber(l) * Ops.toNumber(r));
        case Op.DIV:
            return JsNumber.of(Ops.toNumber(l) / Ops.toNumber(r));
        case Op.MOD: {
            double a = Ops.toNumber(l);
            double b = Ops.toNumber(r);
            return JsNumber.of(a % b);
        }
        case Op.SHL:
            return JsNumber.of(Ops.toInt32(l) << (int) (Ops.toUint32(r) & 31));
        case Op.SHR:
            return JsNumber.of(Ops.toInt32(l) >> (int) (Ops.toUint32(r) & 31));
        case Op.USHR:
            return JsNumber.of((double) (Ops.toUint32(l) >>> (int) (Ops.toUint32(r) & 31)));
        case Op.LT:
            return JsBoolean.of(Ops.lessThan(l, r, true) == 1);
        case Op.GT:
            return JsBoolean.of(Ops.lessThan(r, l, false) == 1);
        case Op.LE:
            return JsBoolean.of(Ops.lessThan(r, l, false) == 0);
        case Op.GE:
            return JsBoolean.of(Ops.lessThan(l, r, true) == 0);
        case Op.EQ:
            return JsBoolean.of(Ops.looseEquals(l, r));
        case Op.NE:
            return JsBoolean.of(!Ops.looseEquals(l, r));
        case Op.SEQ:
            return JsBoolean.of(Ops.strictEquals(l, r));
        case Op.SNE:
            return JsBoolean.of(!Ops.strictEquals(l, r));
        case Op.BITAND:
            return JsNumber.of(Ops.toInt32(l) & Ops.toInt32(r));
        case Op.BITOR:
            return JsNumber.of(Ops.toInt32(l) | Ops.toInt32(r));
        case Op.BITXOR:
            return JsNumber.of(Ops.toInt32(l) ^ Ops.toInt32(r));
        case Op.IN: {
            if (!(r instanceof JsObject)) {
                throw JsException.typeError("Cannot use 'in' operator to search for a key in " + Interpreter.describe(r));
            }
            return JsBoolean.of(((JsObject) r).hasProperty(Ops.toPropertyKey(l)));
        }
        case Op.INSTANCEOF:
            return JsBoolean.of(Ops.instanceOf(r, l));
        default:
            throw new IllegalStateException("unknown binary operator " + op);
        }
    }

    /**
     * Evaluates unary operators including typeof and delete.
     * <p>
     * typeof 와 delete 를 포함한 단항 연산자를 평가한다.
     *
     * @param n   the UNARY node / UNARY 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the value / 값
     */
    private JsValue evalUnary(Node n, Environment env, Ctx ctx, NodeState st) {
        switch (n.op) {
        case Op.TYPEOF: {
            if (n.a.type == NodeType.IDENT) {
                // typeof 는 해석되지 않는 참조에서 ReferenceError 를 던지지 않고 "undefined" 를 돌려준다.
                Reference ref = Reference.identifier(this, env, n.a.name, ctx.strict);
                if (ref.isUnresolvableReference()) {
                    return JsString.of("undefined");
                }
                return JsString.of(ref.getValue().typeOf());
            }
            return JsString.of(memo(st, n.a, env, ctx).typeOf());
        }
        case Op.VOID:
            memo(st, n.a, env, ctx);
            return Js.UNDEFINED;
        case Op.NOT:
            return JsBoolean.of(!Ops.toBoolean(memo(st, n.a, env, ctx)));
        case Op.NEG:
            return JsNumber.of(-Ops.toNumber(memo(st, n.a, env, ctx)));
        case Op.POS:
            return JsNumber.of(Ops.toNumber(memo(st, n.a, env, ctx)));
        case Op.BITNOT:
            return JsNumber.of(~Ops.toInt32(memo(st, n.a, env, ctx)));
        case Op.DELETE:
            return evalDelete(n.a, env, ctx, st);
        default:
            throw new IllegalStateException("unknown unary operator " + n.op);
        }
    }

    /**
     * Evaluates the delete operator.
     * <p>
     * delete 연산자를 평가한다.
     *
     * @param target the operand / 피연산자
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param st     the node state or null / 노드 상태 또는 null
     * @return true if the property is gone / 프로퍼티가 없어졌으면 true
     */
    private JsValue evalDelete(Node target, Environment env, Ctx ctx, NodeState st) {
        if (target.type == NodeType.MEMBER || target.type == NodeType.IDENT) {
            return JsBoolean.of(evalReference(target, env, ctx, st).delete());
        }
        // 참조가 아닌 피연산자는 평가만 하고 true 를 돌려준다.
        memo(st, target, env, ctx);
        return Js.TRUE;
    }

    /**
     * Evaluates ++ and -- on identifiers and members.
     * <p>
     * 식별자와 멤버에 대한 ++, -- 를 평가한다.
     *
     * @param n   the UPDATE node / UPDATE 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the value / 값
     */
    private JsValue evalUpdate(Node n, Environment env, Ctx ctx, NodeState st) {
        double delta = n.op == Op.INC ? 1 : -1;
        // 대상을 Reference 로 한 번만 평가하고, 옛 값은 GetValue 한 번으로 읽어 저장한 뒤 PutValue 한다.
        Reference ref = evalReference(n.a, env, ctx, st);
        double old;
        if (replay(st)) {
            old = ((Double) take(st)).doubleValue();
        } else {
            old = Ops.toNumber(ref.getValue());
            save(st, Double.valueOf(old));
        }
        JsNumber nv = JsNumber.of(old + delta);
        ref.putValue(nv);
        return n.flag ? nv : JsNumber.of(old);
    }

    /**
     * Reads a named property with the null/undefined check.
     * <p>
     * null/undefined 검사를 포함해 이름 있는 프로퍼티를 읽는다.
     *
     * @param base the base value / 기준 값
     * @param name the property name / 프로퍼티 이름
     * @return the value / 값
     */
    JsValue getPropertyChecked(JsValue base, String name) {
        if (base.isNullOrUndefined()) {
            throw JsException.typeError("Cannot read property '" + name + "' of " + base);
        }
        return getV(base, name);
    }

    // ------------------------------------------------------------------ assignment

    /**
     * Evaluates an identifier or a member expression to a {@link Reference} (ES2015 12.1.6 ResolveBinding and 12.3.2 property
     * accessors) without reading or writing it. Sub expressions and the environment lookup are memoized for generators.
     * <p>
     * 식별자나 멤버 표현식을 읽거나 쓰지 않고 {@link Reference} 로 평가한다(ES2015 12.1.6 ResolveBinding, 12.3.2 프로퍼티 접근자).
     * 하위 식과 환경 조회는 제너레이터를 위해 저장한다.
     *
     * @param t   the IDENT or MEMBER node / IDENT 또는 MEMBER 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the reference / 참조
     */
    Reference evalReference(Node t, Environment env, Ctx ctx, NodeState st) {
        if (t.type == NodeType.IDENT) {
            Environment e;
            if (replay(st)) {
                e = (Environment) take(st);
            } else {
                e = env.lookup(t.name);
                save(st, e);
            }
            return Reference.resolved(this, e, t.name, ctx.strict);
        }
        if (t.a.type == NodeType.SUPER) {
            JsValue thisV = superThis(env);
            Object key = t.flag ? Ops.toPropertyKey(memo(st, t.b, env, ctx)) : t.name;
            return Reference.superProperty(this, thisV, superBase(env), key, ctx.strict);
        }
        JsValue base = memo(st, t.a, env, ctx);
        if (t.flag) {
            return Reference.element(this, base, memo(st, t.b, env, ctx), ctx.strict);
        }
        return Reference.property(this, base, t.name, ctx.strict);
    }

    /**
     * Evaluates assignment and compound assignment.
     * <p>
     * 대입과 복합 대입을 평가한다.
     *
     * @param n   the ASSIGN node / ASSIGN 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the assigned value / 대입된 값
     */
    private JsValue evalAssign(Node n, Environment env, Ctx ctx, NodeState st) {
        Node target = n.a;
        int op = n.op;
        if (target.type == NodeType.IDENT || target.type == NodeType.MEMBER) {
            // 대상을 Reference 로 평가한 뒤 (복합 대입이면 GetValue), 오른쪽을 평가하고 PutValue 한다.
            Reference ref = evalReference(target, env, ctx, st);
            JsValue value;
            if (op == 0) {
                value = target.type == NodeType.IDENT && isAnonymous(n.b) ? evalNamed(n.b, target.name, env, ctx) : memo(st, n.b, env, ctx);
            } else {
                JsValue old;
                if (replay(st)) {
                    old = (JsValue) take(st);
                } else {
                    old = ref.getValue();
                    save(st, old);
                }
                value = applyBinary(op, old, memo(st, n.b, env, ctx));
            }
            ref.putValue(value);
            return value;
        }
        // 구조 분해 대입: 오른쪽 값을 먼저 평가한 뒤 패턴에 나눠 넣는다.
        JsValue value = memo(st, n.b, env, ctx);
        in.pat.bind(target, value, env, ctx, Patterns.ASSIGN);
        return value;
    }

    // ------------------------------------------------------------------ functions

    /**
     * Tests whether a node is an anonymous function or class definition (IsAnonymousFunctionDefinition).
     * <p>
     * 노드가 익명 함수나 클래스 정의인지 검사한다(IsAnonymousFunctionDefinition).
     *
     * @param n the node / 노드
     * @return true if anonymous / 익명이면 true
     */
    boolean isAnonymous(Node n) {
        if (n.type == NodeType.FUNCTION) {
            return n.fn.name.length() == 0;
        }
        return n.type == NodeType.CLASS && n.name == null;
    }

    /**
     * Evaluates an anonymous function or class definition giving it an inferred name.
     * <p>
     * 익명 함수나 클래스 정의를 평가하며 추론한 이름을 준다.
     *
     * @param n    the anonymous definition / 익명 정의
     * @param name the name to use / 쓸 이름
     * @param env  the environment / 환경
     * @param ctx  the context / 문맥
     * @return the function or class / 함수 또는 클래스
     */
    JsValue evalNamed(Node n, String name, Environment env, Ctx ctx) {
        if (n.type == NodeType.FUNCTION) {
            return evalFunction(n, env, name);
        }
        return in.cls.evaluate(n, env, ctx, name);
    }

    /**
     * Evaluates a function expression (InstantiateFunctionObject for expressions): a named function expression gets an own
     * scope holding its immutable name binding.
     * <p>
     * 함수 표현식을 평가한다. 이름 있는 함수 표현식은 불변 이름 바인딩을 가진 자신의 스코프를 얻는다.
     *
     * @param n    the FUNCTION node / FUNCTION 노드
     * @param env  the environment / 환경
     * @param hint the inferred name for anonymous functions or null / 익명 함수의 추론 이름 또는 null
     * @return the function / 함수
     */
    JsValue evalFunction(Node n, Environment env, String hint) {
        FunctionInfo info = n.fn;
        if (!info.arrow && info.name.length() > 0) {
            Environment fe = new Environment(env);
            ScriptFunction f = in.createClosure(n, fe, null);
            Binding b = fe.declare(info.name, false, true, f);
            b.silent = true;
            return f;
        }
        return in.createClosure(n, env, hint);
    }

    /**
     * Computes the property name text used for function names from a property key.
     * <p>
     * 프로퍼티 키에서 함수 이름으로 쓰는 문자열을 계산한다.
     *
     * @param key the key (String or symbol) / 키(String 또는 심볼)
     * @return the name / 이름
     */
    static String nameFromKey(Object key) {
        if (key instanceof JsSymbol) {
            String d = ((JsSymbol) key).getDescription();
            return d == null ? "" : "[" + d + "]";
        }
        return (String) key;
    }

    /**
     * Creates a method closure with home object and name (DefineMethod).
     * <p>
     * 홈 객체와 이름을 가진 메서드 클로저를 만든다(DefineMethod).
     *
     * @param fnNode the FUNCTION node / FUNCTION 노드
     * @param env    the environment / 환경
     * @param home   the home object / 홈 객체
     * @param key    the property key / 프로퍼티 키
     * @param prefix "get ", "set " or null / "get ", "set " 또는 null
     * @return the method function / 메서드 함수
     */
    ScriptFunction createMethod(Node fnNode, Environment env, JsObject home, Object key, String prefix) {
        ScriptFunction f = in.createClosure(fnNode, env, null);
        f.homeObject = home;
        String name = (prefix == null ? "" : prefix) + nameFromKey(key);
        f.defineOwnProperty("name", PropertyDescriptor.data(JsString.of(name), false, false, true));
        return f;
    }

    // ------------------------------------------------------------------ calls

    /**
     * Evaluates the callee of a call or tagged template and determines the this value.
     * <p>
     * 호출이나 태그 템플릿의 callee 를 평가하고 this 값을 정한다.
     *
     * @param callee  the callee node / callee 노드
     * @param env     the environment / 환경
     * @param ctx     the context / 문맥
     * @param st      the node state or null / 노드 상태 또는 null
     * @param thisOut receives the this value in element 0 / 0번 원소에 this 값을 받는다
     * @return the function value / 함수 값
     */
    private JsValue evalCallee(Node callee, Environment env, Ctx ctx, NodeState st, JsValue[] thisOut) {
        thisOut[0] = Js.UNDEFINED;
        if (callee.type == NodeType.MEMBER || callee.type == NodeType.IDENT) {
            // callee 를 Reference 로 평가하고, 값은 한 번만 읽어 저장하며(재개 때 다시 읽지 않는다) this 값은 Reference 에서 구한다.
            Reference ref = evalReference(callee, env, ctx, st);
            JsValue func;
            if (replay(st)) {
                func = (JsValue) take(st);
            } else {
                func = ref.getValue();
                save(st, func);
            }
            thisOut[0] = ref.thisValueForCall();
            return func;
        }
        return memo(st, callee, env, ctx);
    }

    /**
     * Evaluates the arguments of a call or new expression (ArgumentListEvaluation), expanding spread elements.
     * <p>
     * 호출이나 new 표현식의 인수를 평가한다(ArgumentListEvaluation). 전개 요소를 펼친다.
     *
     * @param list the argument nodes / 인수 노드들
     * @param env  the environment / 환경
     * @param ctx  the context / 문맥
     * @param st   the node state or null / 노드 상태 또는 null
     * @return the argument values / 인수 값들
     */
    private JsValue[] evalArgs(Node[] list, Environment env, Ctx ctx, NodeState st) {
        boolean spread = false;
        // 인수 목록에 전개 요소가 있는지 확인한다.
        for (int i = 0; i < list.length; i++) {
            if (list[i].type == NodeType.SPREAD) {
                spread = true;
            }
        }
        if (!spread) {
            JsValue[] out = new JsValue[list.length];
            // 전개가 없으면 인수를 순서대로 평가한다.
            for (int i = 0; i < list.length; i++) {
                out[i] = memo(st, list[i], env, ctx);
            }
            return out;
        }
        List<JsValue> out = new ArrayList<JsValue>();
        // 전개 요소는 반복 가능 객체를 펼쳐 넣고, 펼친 결과를 저장해 재개 때 다시 반복하지 않게 한다.
        for (int i = 0; i < list.length; i++) {
            Node a = list[i];
            if (a.type == NodeType.SPREAD) {
                JsValue[] vals;
                if (replay(st)) {
                    vals = (JsValue[]) take(st);
                } else {
                    JsValue it = eval(a.a, env, ctx);
                    List<JsValue> l = IteratorHelper.toList(realm, it);
                    vals = l.toArray(new JsValue[l.size()]);
                    save(st, vals);
                }
                // 전개한 값을 결과 목록에 추가한다.
                for (int k = 0; k < vals.length; k++) {
                    out.add(vals[k]);
                }
            } else {
                out.add(memo(st, a, env, ctx));
            }
        }
        return out.toArray(new JsValue[out.size()]);
    }

    /**
     * Describes a callee expression for error messages such as "a.b is not a function".
     * <p>
     * "a.b is not a function" 같은 오류 메시지를 위해 callee 표현식을 설명한다.
     *
     * @param n the node / 노드
     * @return the text / 문자열
     */
    static String describeNode(Node n) {
        switch (n.type) {
        case NodeType.IDENT:
            return n.name;
        case NodeType.THIS:
            return "this";
        case NodeType.SUPER:
            return "super";
        case NodeType.MEMBER:
            return n.flag ? describeNode(n.a) + "[...]" : describeNode(n.a) + "." + n.name;
        case NodeType.CALL:
            return describeNode(n.a) + "(...)";
        case NodeType.LITERAL:
            return String.valueOf(n.value);
        default:
            return "expression";
        }
    }

    /**
     * Evaluates a call expression, including direct eval and super calls.
     * <p>
     * 직접 eval 과 super 호출을 포함한 호출 표현식을 평가한다.
     *
     * @param n   the CALL node / CALL 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the result / 결과
     */
    private JsValue evalCall(Node n, Environment env, Ctx ctx, NodeState st) {
        return callNode(n, env, ctx, st, false);
    }

    /**
     * Evaluates an expression in tail position of a strict function body (ES2015 14.6.1): a call that is the result of the
     * expression (directly, or through a conditional, logical or comma expression) does not run here when its target is a script
     * function; the target, this value and arguments are stored in the context and the trampoline of
     * {@link Interpreter#callFunction} runs the call after this function body has finished.
     * <p>
     * 엄격 모드 함수 본문의 꼬리 위치 표현식을 평가한다(ES2015 14.6.1): 표현식의 결과가 되는 호출(직접, 또는 조건/논리/쉼표 식을 통한)은 대상이
     * 스크립트 함수이면 여기서 실행하지 않고, 대상과 this 와 인수를 문맥에 저장해 이 함수 본문이 끝난 뒤
     * {@link Interpreter#callFunction} 의 트램펄린이 실행한다.
     *
     * @param n   the expression / 표현식
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @return the value, or undefined when a tail call was stored in the context / 값. 꼬리 호출을 문맥에 저장했으면 undefined
     */
    JsValue evalTail(Node n, Environment env, Ctx ctx) {
        switch (n.type) {
        case NodeType.CALL:
            return callNode(n, env, ctx, null, true);
        case NodeType.CONDITIONAL:
            return evalTail(Ops.toBoolean(eval(n.a, env, ctx)) ? n.b : n.c, env, ctx);
        case NodeType.LOGICAL: {
            JsValue l = eval(n.a, env, ctx);
            boolean truthy = Ops.toBoolean(l);
            if (n.op == Op.LOGAND ? !truthy : truthy) {
                return l;
            }
            return evalTail(n.b, env, ctx);
        }
        case NodeType.SEQUENCE: {
            // 쉼표 식은 마지막 식만 꼬리 위치이다.
            for (int i = 0; i < n.list.length - 1; i++) {
                eval(n.list[i], env, ctx);
            }
            return evalTail(n.list[n.list.length - 1], env, ctx);
        }
        default:
            return eval(n, env, ctx);
        }
    }

    /**
     * Evaluates a call expression, optionally as a tail call.
     * <p>
     * 호출 표현식을 평가한다. 꼬리 호출일 수 있다.
     *
     * @param n    the CALL node / CALL 노드
     * @param env  the environment / 환경
     * @param ctx  the context / 문맥
     * @param st   the node state or null / 노드 상태 또는 null
     * @param tail true if the call is in tail position of a function that may use tail calls / 꼬리 호출을 쓸 수 있는 함수의 꼬리 위치이면 true
     * @return the result, or undefined when a tail call was stored in the context / 결과. 꼬리 호출을 문맥에 저장했으면 undefined
     */
    private JsValue callNode(Node n, Environment env, Ctx ctx, NodeState st, boolean tail) {
        Node callee = n.a;
        if (callee.type == NodeType.SUPER) {
            return evalSuperCall(n, env, ctx, st);
        }
        JsValue[] thisOut = new JsValue[1];
        JsValue func = evalCallee(callee, env, ctx, st, thisOut);
        JsValue[] args = evalArgs(n.list, env, ctx, st);
        if (!(func instanceof JsObject) || !func.isCallable()) {
            throw JsException.typeError(describeNode(callee) + " is not a function");
        }
        if (callee.type == NodeType.IDENT && callee.name.equals("eval") && func == realm.getIntrinsic("%eval%")) {
            if (args.length == 0 || !(args[0] instanceof JsString)) {
                return args.length == 0 ? Js.UNDEFINED : args[0];
            }
            return in.performEval(((JsString) args[0]).getValue(), true, env, ctx);
        }
        if (tail && func instanceof ScriptFunction && !((ScriptFunction) func).classConstructor) {
            // 꼬리 호출: 호출자의 트램펄린이 이 함수 본문이 끝난 뒤 실행한다.
            ctx.tailTarget = (ScriptFunction) func;
            ctx.tailThis = thisOut[0];
            ctx.tailArgs = args;
            return Js.UNDEFINED;
        }
        // 기본 함수(console.log 등)가 자신이 호출된 스크립트 위치를 알 수 있도록 호출 직전에 위치를 기억해 둔다.
        if (func instanceof NativeFunction) {
            realm.setCallSite(ctx.sourceName, n.line, n.column);
        }
        return ((JsObject) func).call(thisOut[0], args);
    }

    /**
     * Evaluates <code>super(...)</code> in a derived constructor.
     * <p>
     * 파생 생성자 안의 <code>super(...)</code> 를 평가한다.
     *
     * @param n   the CALL node / CALL 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the constructed object / 만들어진 객체
     */
    private JsValue evalSuperCall(Node n, Environment env, Ctx ctx, NodeState st) {
        Environment te = env.thisEnvironment();
        ScriptFunction active = te.function;
        JsObject parent = active.getPrototypeOf();
        JsValue[] args = evalArgs(n.list, env, ctx, st);
        if (parent == null || !parent.isConstructor()) {
            throw JsException.typeError("Super constructor " + JsException.describe(parent) + " of anonymous class is not a constructor");
        }
        JsObject result = parent.construct(args, te.newTarget);
        te.bindThis(result);
        return result;
    }

    /**
     * Evaluates a new expression.
     * <p>
     * new 표현식을 평가한다.
     *
     * @param n   the NEW node / NEW 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the new object / 새 객체
     */
    private JsValue evalNew(Node n, Environment env, Ctx ctx, NodeState st) {
        JsValue ctor = memo(st, n.a, env, ctx);
        JsValue[] args = evalArgs(n.list, env, ctx, st);
        if (!(ctor instanceof JsObject) || !((JsObject) ctor).isConstructor()) {
            throw JsException.typeError(describeNode(n.a) + " is not a constructor");
        }
        return ((JsObject) ctor).construct(args, (JsObject) ctor);
    }

    // ------------------------------------------------------------------ literals

    /**
     * Evaluates a regular expression literal by constructing a RegExp object.
     * <p>
     * 정규식 리터럴을 RegExp 객체를 만들어 평가한다.
     *
     * @param n the REGEXP node / REGEXP 노드
     * @return the RegExp object / RegExp 객체
     */
    private JsValue evalRegExp(Node n) {
        JsObject ctor = realm.getIntrinsic("%RegExp%");
        if (ctor == null) {
            throw JsException.syntaxError("Regular expression literals are not available in this realm (no RegExp built-in installed)");
        }
        return ctor.construct(new JsValue[] { JsString.of(n.name), JsString.of((String) n.value) }, ctor);
    }

    /**
     * Evaluates a template literal (substitutions are converted with ToString).
     * <p>
     * 템플릿 리터럴을 평가한다(치환 값은 ToString 으로 변환).
     *
     * @param n   the TEMPLATE node / TEMPLATE 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the string / 문자열
     */
    private JsValue evalTemplate(Node n, Environment env, Ctx ctx, NodeState st) {
        TemplateParts tp = (TemplateParts) n.value;
        StringBuilder sb = new StringBuilder(tp.cooked[0]);
        // 문자열 조각과 치환 값을 번갈아 이어 붙인다.
        for (int i = 0; i < n.list.length; i++) {
            JsValue v = memo(st, n.list[i], env, ctx);
            sb.append(Ops.toString(v)).append(tp.cooked[i + 1]);
            in.limits.checkStringLength(sb.length());
        }
        return JsString.of(sb.toString());
    }

    /**
     * Evaluates a tagged template: calls the tag with the (cached) template object and the substitution values.
     * <p>
     * 태그 템플릿을 평가한다: 태그를 (캐시된) 템플릿 객체와 치환 값으로 호출한다.
     *
     * @param n   the TAGGED_TEMPLATE node / TAGGED_TEMPLATE 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the result / 결과
     */
    private JsValue evalTagged(Node n, Environment env, Ctx ctx, NodeState st) {
        JsValue[] thisOut = new JsValue[1];
        JsValue func = evalCallee(n.a, env, ctx, st, thisOut);
        Node tpl = n.b;
        JsValue[] args = new JsValue[tpl.list.length + 1];
        args[0] = templateObject(tpl);
        // 치환 값을 순서대로 평가해 인수로 넘긴다.
        for (int i = 0; i < tpl.list.length; i++) {
            args[i + 1] = memo(st, tpl.list[i], env, ctx);
        }
        if (!(func instanceof JsObject) || !func.isCallable()) {
            throw JsException.typeError(describeNode(n.a) + " is not a function");
        }
        return ((JsObject) func).call(thisOut[0], args);
    }

    /**
     * Returns the template object of a tagged template site (GetTemplateObject): a frozen array of cooked strings with a frozen
     * <code>raw</code> array, the same object for every evaluation of the site.
     * <p>
     * 태그 템플릿 위치의 템플릿 객체를 반환한다(GetTemplateObject): 해석된 문자열의 얼린 배열과 얼린 <code>raw</code> 배열이며, 같은 위치를
     * 평가할 때마다 같은 객체이다.
     *
     * @param tpl the TEMPLATE node / TEMPLATE 노드
     * @return the template object / 템플릿 객체
     */
    private JsObject templateObject(Node tpl) {
        Object[] cache = (Object[]) tpl.cache;
        if (cache != null && cache[0] == realm) {
            return (JsObject) cache[1];
        }
        TemplateParts tp = (TemplateParts) tpl.value;
        JsArray cooked = realm.newArray();
        JsArray raw = realm.newArray();
        // 해석된 문자열(실패하면 undefined)과 원문 문자열을 배열에 담는다.
        for (int i = 0; i < tp.cooked.length; i++) {
            cooked.add(tp.cooked[i] == null ? Js.UNDEFINED : JsString.of(tp.cooked[i]));
            raw.add(JsString.of(tp.raw[i]));
        }
        freeze(raw);
        cooked.defineOwnProperty("raw", PropertyDescriptor.data(raw, false, false, false));
        freeze(cooked);
        tpl.cache = new Object[] { realm, cooked };
        return cooked;
    }

    /**
     * Freezes an object (SetIntegrityLevel frozen).
     * <p>
     * 객체를 얼린다(SetIntegrityLevel frozen).
     *
     * @param o the object / 객체
     */
    void freeze(JsObject o) {
        o.preventExtensions();
        // 모든 자체 프로퍼티를 설정 불가로 만들고 데이터 프로퍼티는 쓰기 불가로 만든다.
        for (Object k : o.ownKeys()) {
            PropertyDescriptor d = o.getOwnProperty(k);
            if (d == null) {
                continue;
            }
            PropertyDescriptor nd = new PropertyDescriptor();
            nd.configurable = Boolean.FALSE;
            if (!d.isAccessor()) {
                nd.writable = Boolean.FALSE;
            }
            o.defineOwnProperty(k, nd);
        }
    }

    /**
     * Evaluates an array literal.
     * <p>
     * 배열 리터럴을 평가한다.
     *
     * @param n   the ARRAY node / ARRAY 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the array / 배열
     */
    @SuppressWarnings("unchecked")
    private JsValue evalArray(Node n, Environment env, Ctx ctx, NodeState st) {
        ArrayList<JsValue> list;
        int start = 0;
        if (st != null && st.o1 != null) {
            list = (ArrayList<JsValue>) st.o1;
            start = st.idx;
        } else {
            list = new ArrayList<JsValue>();
            if (st != null) {
                st.o1 = list;
            }
        }
        // 요소를 앞에서부터 읽어 목록에 넣는다. 중단되면 지금까지의 목록이 상태에 남는다.
        for (int i = start; i < n.list.length; i++) {
            if (st != null) {
                st.idx = i;
            }
            Node e = n.list[i];
            if (e == null) {
                list.add(null);
            } else if (e.type == NodeType.SPREAD) {
                JsValue it = eval(e.a, env, ctx);
                list.addAll(IteratorHelper.toList(realm, it));
            } else {
                list.add(eval(e, env, ctx));
            }
        }
        in.limits.tick();
        return new JsArray(realm.getArrayPrototype(), list.toArray(new JsValue[list.size()]));
    }

    /**
     * Evaluates an object literal (PropertyDefinitionEvaluation).
     * <p>
     * 객체 리터럴을 평가한다(PropertyDefinitionEvaluation).
     *
     * @param n   the OBJECT node / OBJECT 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the object / 객체
     */
    private JsValue evalObject(Node n, Environment env, Ctx ctx, NodeState st) {
        JsObject obj;
        int start = 0;
        if (st != null && st.o1 != null) {
            obj = (JsObject) st.o1;
            start = st.idx;
        } else {
            obj = realm.newObject();
            if (st != null) {
                st.o1 = obj;
            }
        }
        // 프로퍼티를 앞에서부터 정의한다. 계산된 키는 값 평가 전에 한 번만 계산해 상태에 둔다.
        for (int i = start; i < n.list.length; i++) {
            if (st != null) {
                st.idx = i;
            }
            defineProperty(obj, n.list[i], env, ctx, st);
            if (st != null) {
                st.idx2 = 0;
                st.o2 = null;
            }
        }
        return obj;
    }

    /**
     * Defines one property of an object literal.
     * <p>
     * 객체 리터럴의 프로퍼티 하나를 정의한다.
     *
     * @param obj the object / 객체
     * @param p   the PROPERTY node / PROPERTY 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the object literal's state or null / 객체 리터럴의 상태 또는 null
     */
    private void defineProperty(JsObject obj, Node p, Environment env, Ctx ctx, NodeState st) {
        Object key;
        if (!p.flag) {
            key = p.a.name;
        } else if (st != null && st.idx2 == 1) {
            key = st.o2;
        } else {
            key = Ops.toPropertyKey(eval(p.a, env, ctx));
            if (st != null) {
                st.idx2 = 1;
                st.o2 = key;
            }
        }
        switch (p.op) {
        case Op.PROP_INIT: {
            if (!p.flag && "__proto__".equals(key)) {
                JsValue v = eval(p.b, env, ctx);
                if (v instanceof JsObject) {
                    obj.setPrototypeOf((JsObject) v);
                } else if (v.isNull()) {
                    obj.setPrototypeOf(null);
                }
                return;
            }
            JsValue v = isAnonymous(p.b) ? evalNamed(p.b, nameFromKey(key), env, ctx) : eval(p.b, env, ctx);
            obj.createDataPropertyOrThrow(key, v);
            return;
        }
        case Op.PROP_SHORTHAND:
            obj.createDataPropertyOrThrow(key, identifierValue(p.b.name, env, ctx));
            return;
        case Op.PROP_METHOD:
            obj.createDataPropertyOrThrow(key, createMethod(p.b, env, obj, key, null));
            return;
        case Op.PROP_GET: {
            ScriptFunction f = createMethod(p.b, env, obj, key, "get ");
            PropertyDescriptor d = new PropertyDescriptor();
            d.get = f;
            d.enumerable = Boolean.TRUE;
            d.configurable = Boolean.TRUE;
            obj.definePropertyOrThrow(key, d);
            return;
        }
        case Op.PROP_SET: {
            ScriptFunction f = createMethod(p.b, env, obj, key, "set ");
            PropertyDescriptor d = new PropertyDescriptor();
            d.set = f;
            d.enumerable = Boolean.TRUE;
            d.configurable = Boolean.TRUE;
            obj.definePropertyOrThrow(key, d);
            return;
        }
        default:
            throw JsException.syntaxError("Invalid property definition in object literal");
        }
    }

    // ------------------------------------------------------------------ yield

    /**
     * Evaluates a yield expression: suspends the generator, or on resumption delivers the value, exception or return request.
     * <p>
     * yield 표현식을 평가한다: 제너레이터를 중단하거나, 재개되었을 때 값, 예외, return 요청을 전달한다.
     *
     * @param n   the YIELD node / YIELD 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state / 노드 상태
     * @return the value sent by next() / next() 가 보낸 값
     */
    private JsValue evalYield(Node n, Environment env, Ctx ctx, NodeState st) {
        GenState gen = ctx.gen;
        JsValue v = n.a == null ? Js.UNDEFINED : memo(st, n.a, env, ctx);
        if (st.phase == 0) {
            st.phase = 1;
            gen.suspending = true;
            gen.yielded = v;
            gen.yieldedRaw = false;
            throw YieldSignal.INSTANCE;
        }
        return resumeValue(gen);
    }

    /**
     * Delivers the resume request at a suspended yield.
     * <p>
     * 중단된 yield 에서 재개 요청을 전달한다.
     *
     * @param gen the generator state / 제너레이터 상태
     * @return the value for next() / next() 의 값
     */
    private JsValue resumeValue(GenState gen) {
        switch (gen.resumeMode) {
        case GenState.THROW:
            throw new JsException(gen.resumeValue);
        case GenState.RETURN:
            throw new GeneratorReturn(gen.resumeValue);
        default:
            return gen.resumeValue;
        }
    }

    /**
     * Evaluates <code>yield* iterable</code> (delegation, ECMA-262 6th edition 14.4.14): forwards next, throw and return
     * requests to the inner iterator and passes its result objects through unchanged.
     * <p>
     * <code>yield* iterable</code> 을 평가한다(위임, ECMA-262 6판 14.4.14): next, throw, return 요청을 안쪽 이터레이터에 전달하고 그
     * 결과 객체를 그대로 전달한다.
     *
     * @param n   the YIELD node / YIELD 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state / 노드 상태
     * @return the value of the delegated generator's completion / 위임한 제너레이터 완료의 값
     */
    private JsValue evalYieldStar(Node n, Environment env, Ctx ctx, NodeState st) {
        GenState gen = ctx.gen;
        JsObject iterator;
        int mode;
        JsValue received;
        if (st.phase == 0) {
            JsValue iterable = memo(st, n.a, env, ctx);
            iterator = IteratorHelper.getIterator(realm, iterable);
            st.o1 = iterator;
            mode = GenState.NEXT;
            received = Js.UNDEFINED;
        } else {
            iterator = (JsObject) st.o1;
            mode = gen.resumeMode;
            received = gen.resumeValue;
        }
        // 안쪽 이터레이터가 끝날 때까지 요청을 전달한다.
        while (true) {
            JsValue innerResult;
            if (mode == GenState.NEXT) {
                innerResult = Ops.call(iterator.get("next"), iterator, new JsValue[] { received });
            } else if (mode == GenState.THROW) {
                JsValue thr = Ops.getMethod(iterator, "throw");
                if (thr.isUndefined()) {
                    IteratorHelper.closeQuietly(iterator);
                    throw JsException.typeError("The iterator does not provide a 'throw' method");
                }
                innerResult = Ops.call(thr, iterator, new JsValue[] { received });
            } else {
                JsValue ret = Ops.getMethod(iterator, "return");
                if (ret.isUndefined()) {
                    throw new GeneratorReturn(received);
                }
                innerResult = Ops.call(ret, iterator, new JsValue[] { received });
                if (!(innerResult instanceof JsObject)) {
                    throw JsException.typeError("Iterator result " + JsException.describe(innerResult) + " is not an object");
                }
                if (IteratorHelper.isDone((JsObject) innerResult)) {
                    throw new GeneratorReturn(IteratorHelper.value((JsObject) innerResult));
                }
            }
            if (!(innerResult instanceof JsObject)) {
                throw JsException.typeError("Iterator result " + JsException.describe(innerResult) + " is not an object");
            }
            JsObject r = (JsObject) innerResult;
            if (mode != GenState.RETURN && IteratorHelper.isDone(r)) {
                return IteratorHelper.value(r);
            }
            st.phase = 1;
            gen.suspending = true;
            gen.yielded = r;
            gen.yieldedRaw = true;
            throw YieldSignal.INSTANCE;
        }
    }

    /**
     * Creates a native function value from a Java behaviour (used by the interpreter for small helper functions).
     * <p>
     * Java 동작으로 네이티브 함수 값을 만든다(인터프리터의 작은 도우미 함수용).
     *
     * @param name   the name / 이름
     * @param length the length / length
     * @param call   the behaviour / 동작
     * @return the function / 함수
     */
    NativeFunction nativeFunction(String name, int length, NativeFunction.Call call) {
        return realm.newFunction(name, length, call);
    }
}
