package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.Op;

/**
 * Evaluates class definitions (ClassDefinitionEvaluation, ECMA-262 6th edition 14.5.14): the heritage clause, the prototype
 * object, the constructor (explicit or implicit), methods, accessors and static members, the immutable inner name binding and the
 * rule that class bodies are strict. Class constructors cannot be called without <code>new</code>; derived constructors get
 * their <code>this</code> from <code>super()</code>.
 * <p>
 * 클래스 정의를 평가한다(ClassDefinitionEvaluation, ECMA-262 6판 14.5.14): extends 절, 프로토타입 객체, 생성자(명시 또는 암시),
 * 메서드, 접근자, static 멤버, 불변 내부 이름 바인딩, 클래스 본문이 엄격하다는 규칙. 클래스 생성자는 <code>new</code> 없이 호출할 수 없고
 * 파생 생성자는 <code>super()</code> 로 <code>this</code> 를 얻는다.
 */
final class Classes {

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
     * Creates the evaluator.
     * <p>
     * 평가기를 만든다.
     *
     * @param in the interpreter / 인터프리터
     */
    Classes(Interpreter in) {
        this.in = in;
        this.realm = in.realm;
    }

    /**
     * Evaluates a class definition and returns its constructor function.
     * <p>
     * 클래스 정의를 평가하고 생성자 함수를 반환한다.
     *
     * @param cls      the CLASS node / CLASS 노드
     * @param env      the environment / 환경
     * @param ctx      the context / 문맥
     * @param nameHint the inferred name for anonymous classes or null / 익명 클래스의 추론 이름 또는 null
     * @return the class constructor / 클래스 생성자
     */
    JsValue evaluate(Node cls, Environment env, Ctx ctx, String nameHint) {
        String className = cls.name != null ? cls.name : (nameHint != null ? nameHint : "");
        Environment classEnv = new Environment(env);
        if (cls.name != null) {
            classEnv.declare(cls.name, false, false, null);
        }
        Ctx strictCtx = ctx;
        JsObject protoParent = realm.getObjectPrototype();
        JsObject constructorParent = realm.getFunctionPrototype();
        if (cls.a != null) {
            JsValue sup = in.ex.eval(cls.a, classEnv, strictCtx);
            if (sup.isNull()) {
                protoParent = null;
            } else if (!(sup instanceof JsObject) || !((JsObject) sup).isConstructor()) {
                throw JsException.typeError("Class extends value " + Interpreter.describe(sup) + " is not a constructor or null");
            } else {
                JsValue pp = ((JsObject) sup).get("prototype");
                if (pp instanceof JsObject) {
                    protoParent = (JsObject) pp;
                } else if (pp.isNull()) {
                    protoParent = null;
                } else {
                    throw JsException.typeError("Class extends value does not have valid prototype property " + Interpreter.describe(pp));
                }
                constructorParent = (JsObject) sup;
            }
        }
        JsObject proto = new JsObject(protoParent);
        ScriptFunction f;
        if (cls.b != null) {
            f = in.createClosure(cls.b, classEnv, className);
            f.info.start = cls.start;
            f.info.end = cls.end;
        } else {
            FunctionInfo di = new FunctionInfo();
            di.classConstructor = true;
            di.derived = cls.a != null;
            di.strict = true;
            di.method = true;
            di.name = className;
            di.source = cls.value instanceof String ? (String) cls.value : null;
            di.start = cls.start;
            di.end = cls.end;
            f = new ScriptFunction(in, realm, realm.getFunctionPrototype(), di, classEnv, className);
            f.defaultConstructor = true;
        }
        f.classConstructor = true;
        f.homeObject = proto;
        f.defineOwnProperty("name", PropertyDescriptor.data(JsString.of(className), false, false, true));
        if (constructorParent != realm.getFunctionPrototype()) {
            f.setPrototypeOf(constructorParent);
        }
        f.prototypeHandled();
        f.defineOwnProperty("prototype", PropertyDescriptor.data(proto, false, false, false));
        proto.defineBuiltin("constructor", f);
        // 메서드와 접근자를 정의한다. static 멤버는 생성자에, 나머지는 프로토타입에 둔다. 클래스 멤버는 열거할 수 없다.
        for (int i = 0; i < cls.list.length; i++) {
            Node m = cls.list[i];
            JsObject target = m.flag ? f : proto;
            Object key = m.value == Boolean.TRUE ? Ops.toPropertyKey(in.ex.eval(m.a, classEnv, strictCtx)) : m.a.name;
            if (m.op == Op.PROP_METHOD) {
                ScriptFunction mf = in.ex.createMethod(m.b, classEnv, target, key, null);
                target.definePropertyOrThrow(key, PropertyDescriptor.data(mf, true, false, true));
            } else {
                boolean getter = m.op == Op.PROP_GET;
                ScriptFunction mf = in.ex.createMethod(m.b, classEnv, target, key, getter ? "get " : "set ");
                PropertyDescriptor d = new PropertyDescriptor();
                if (getter) {
                    d.get = mf;
                } else {
                    d.set = mf;
                }
                d.enumerable = Boolean.FALSE;
                d.configurable = Boolean.TRUE;
                target.definePropertyOrThrow(key, d);
            }
        }
        if (cls.name != null) {
            classEnv.initialize(cls.name, f);
        }
        return f;
    }
}
