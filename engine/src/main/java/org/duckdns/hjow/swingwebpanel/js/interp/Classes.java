package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
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
        ClassRuntime.Brand brand = new ClassRuntime.Brand(env.privateBrand);
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
        classEnv.privateBrand = brand;
        JsObject proto = new JsObject(protoParent);
        ScriptFunction f;
        if (cls.b != null) {
            f = in.createClosure(cls.b, classEnv, className);
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
        f.classBrand = brand;
        brand.constructor = f;
        f.homeObject = proto;
        f.defineOwnProperty("name", PropertyDescriptor.data(JsString.of(className), false, false, true));
        if (constructorParent != realm.getFunctionPrototype()) {
            f.setPrototypeOf(constructorParent);
        }
        f.prototypeHandled();
        f.defineOwnProperty("prototype", PropertyDescriptor.data(proto, false, false, false));
        proto.defineBuiltin("constructor", f);
        preparePrivateElements(brand, cls);
        // 메서드·필드·static 블록을 소스 순서대로 정의/실행한다.
        for (int i = 0; i < cls.list.length; i++) {
            Node m = cls.list[i];
            if (m.type == NodeType.CLASS_STATIC_BLOCK) {
                runStaticBlock(m, f, classEnv, ctx);
                continue;
            }
            if (m.op == Op.PROP_FIELD) {
                defineField(m, brand, f, proto, classEnv, ctx);
                continue;
            }
            JsObject target = m.flag ? f : proto;
            Object key = m.value == Boolean.TRUE ? Ops.toPropertyKey(in.ex.eval(m.a, classEnv, strictCtx)) : m.a.name;
            if (m.op == Op.PROP_METHOD) {
                ScriptFunction mf = in.ex.createMethod(m.b, classEnv, target, key, null);
                mf.classBrand = brand;
                if (m.privateName) {
                    brand.privates.get(key.toString()).method = mf;
                } else {
                    target.definePropertyOrThrow(key, PropertyDescriptor.data(mf, true, false, true));
                }
            } else {
                boolean getter = m.op == Op.PROP_GET;
                ScriptFunction mf = in.ex.createMethod(m.b, classEnv, target, key, getter ? "get " : "set ");
                mf.classBrand = brand;
                if (m.privateName) {
                    ClassRuntime.PrivateDefinition pd = brand.privates.get(key.toString());
                    if (getter) {
                        pd.getter = mf;
                    } else {
                        pd.setter = mf;
                    }
                } else {
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
        }
        if (cls.name != null) {
            classEnv.initialize(cls.name, f);
        }
        return f;
    }

    /**
     * Predeclares all private elements of the class before any method or initializer closes over them.
     * <p>
     * 메서드나 초기화 식이 닫기 전에 클래스의 모든 private 요소를 선언한다.
     *
     * @param brand class brand / 클래스 brand
     * @param cls class AST / 클래스 AST
     */
    private void preparePrivateElements(ClassRuntime.Brand brand, Node cls) {
        // private 필드·메서드·접근자를 먼저 모두 등록해 앞 요소도 뒤 요소 이름을 볼 수 있게 한다.
        for (int i = 0; i < cls.list.length; i++) {
            Node m = cls.list[i];
            if (!m.privateName) {
                continue;
            }
            ClassRuntime.PrivateDefinition pd = brand.privates.get(m.a.name);
            if (pd == null) {
                pd = new ClassRuntime.PrivateDefinition(m.flag);
                brand.privates.put(m.a.name, pd);
            }
            if (m.op == Op.PROP_FIELD) {
                pd.field = true;
            } else if (!m.flag) {
                brand.hasInstancePrivateMethods = true;
            }
        }
    }

    /**
     * Defines one static field or records one instance field.
     * <p>
     * static 필드 하나를 정의하거나 인스턴스 필드 하나를 기록한다.
     *
     * @param member field node / 필드 노드
     * @param brand class brand / 클래스 brand
     * @param constructor constructor / 생성자
     * @param prototype instance prototype / 인스턴스 프로토타입
     * @param classEnv class lexical environment / 클래스 렉시컬 환경
     * @param outer surrounding context / 바깥 문맥
     */
    private void defineField(Node member, ClassRuntime.Brand brand, ScriptFunction constructor, JsObject prototype,
            Environment classEnv, Ctx outer) {
        Object key = member.value == Boolean.TRUE ? Ops.toPropertyKey(in.ex.eval(member.a, classEnv, outer)) : member.a.name;
        if (member.flag) {
            JsValue value = member.b == null ? Js.UNDEFINED
                    : evalStatic(member.b, constructor, classEnv, outer.sourceName);
            if (member.privateName) {
                ClassRuntime.PrivateDefinition pd = brand.privates.get(key.toString());
                pd.staticValue = value;
            } else {
                constructor.createDataPropertyOrThrow(key, value);
            }
        } else {
            brand.fields.add(new ClassRuntime.Field(key, member.privateName, member.b));
        }
    }

    /**
     * Evaluates a static field initializer in a class-static this/super context.
     * <p>
     * 클래스 static this/super 문맥에서 static 필드 초기화 식을 평가한다.
     */
    private JsValue evalStatic(Node expression, ScriptFunction constructor, Environment classEnv, String sourceName) {
        Environment env = staticEnvironment(constructor, classEnv);
        Ctx ctx = new Ctx(true, env, constructor);
        ctx.funcEnv = env;
        ctx.sourceName = sourceName;
        return in.ex.eval(expression, env, ctx);
    }

    /**
     * Runs one class static initialization block.
     * <p>
     * 클래스 static 초기화 블록 하나를 실행한다.
     */
    private void runStaticBlock(Node block, ScriptFunction constructor, Environment classEnv, Ctx outer) {
        Environment env = staticEnvironment(constructor, classEnv);
        in.declareLexicals(block.scope, env);
        in.instantiateBlockFunctions(block.scope, env);
        // static 블록의 var 이름을 초기화한 뒤 문장 실행에 사용한다.
        for (int i = 0; i < block.scope.varNames.length; i++) {
            if (env.own(block.scope.varNames[i]) == null) {
                env.declare(block.scope.varNames[i], true, true, Js.UNDEFINED);
            }
        }
        Ctx ctx = new Ctx(true, env, constructor);
        ctx.funcEnv = env;
        ctx.sourceName = outer.sourceName;
        in.st.execList(block.list, env, ctx);
    }

    /**
     * Creates the this environment for static field and block evaluation.
     * <p>
     * static 필드와 블록 평가용 this 환경을 만든다.
     */
    private static Environment staticEnvironment(ScriptFunction constructor, Environment classEnv) {
        Environment env = new Environment(classEnv);
        env.function = constructor;
        env.hasThis = true;
        env.thisBound = true;
        env.thisValue = constructor;
        env.classStaticContext = true;
        return env;
    }

    /**
     * Initializes all non-static fields of one class brand on an instance.
     * <p>
     * 클래스 brand 하나의 모든 인스턴스 필드를 객체에 초기화한다.
     *
     * @param constructor class constructor / 클래스 생성자
     * @param object new instance / 새 인스턴스
     */
    void initializeInstanceFields(ScriptFunction constructor, JsObject object) {
        ClassRuntime.Brand brand = constructor.classBrand;
        if (brand == null) {
            return;
        }
        // private 메서드와 접근자를 먼저 추가하고, 호스트가 거부하면 필드 초기화를 시작하지 않는다.
        if (brand.hasInstancePrivateMethods) {
            ClassRuntime.addMethods(object, brand);
        }
        Environment fieldEnv = new Environment(constructor.scope);
        fieldEnv.function = constructor;
        fieldEnv.hasThis = true;
        fieldEnv.thisBound = true;
        fieldEnv.thisValue = object;
        fieldEnv.newTarget = constructor;
        Ctx ctx = new Ctx(true, fieldEnv, constructor);
        ctx.funcEnv = fieldEnv;
        ctx.sourceName = constructor.info.sourceName == null ? "" : constructor.info.sourceName;
        // 소스 순서에 따라 초기화 식이 끝난 필드만 추가한다.
        for (int i = 0; i < brand.fields.size(); i++) {
            ClassRuntime.Field field = brand.fields.get(i);
            JsValue value = field.initializer == null ? Js.UNDEFINED : in.ex.eval(field.initializer, fieldEnv, ctx);
            if (field.privateField) {
                ClassRuntime.addField(object, brand, (String) field.key, value);
            } else {
                object.createDataPropertyOrThrow(field.key, value);
            }
        }
    }
}
