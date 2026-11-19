package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Reference specification type (ECMA-262 6th edition 6.2.3, plan item P13-01): the derived operations
 * IsPropertyReference, IsUnresolvableReference, IsSuperReference and GetThisValue, GetValue, PutValue and delete for every kind
 * of reference, and the places where the interpreter uses references (assignment, update, delete, typeof, call).
 * <p>
 * Reference 명세 자료형 시험(ECMA-262 6판 6.2.3, 계획 항목 P13-01): 모든 종류의 참조에 대한 파생 연산 IsPropertyReference,
 * IsUnresolvableReference, IsSuperReference, GetThisValue 와 GetValue, PutValue, delete, 그리고 인터프리터가 참조를 쓰는 곳(대입, 갱신,
 * delete, typeof, 호출).
 */
public class ReferenceTest {

    /**
     * The realm of the current case.
     * <p>
     * 현재 사례의 realm.
     */
    private JsRealm realm;

    /**
     * The expression helpers of the realm's interpreter.
     * <p>
     * realm 의 인터프리터가 가진 표현식 도우미.
     */
    private Expressions ex;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ReferenceTest() {
    }

    /**
     * Creates a fresh realm for a case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    private void setUp() {
        realm = Scripts.realm(new Scripts.Capture());
        ex = realm.getInterpreter().ex;
    }

    /**
     * Runs a block and returns the name of the script error it throws (or "none").
     * <p>
     * 블록을 실행하고 던지는 스크립트 오류의 이름을 반환한다(없으면 "none").
     *
     * @param b the block / 블록
     * @return the error name / 오류 이름
     */
    private String errorOf(Block b) {
        try {
            b.run();
        } catch (JsException e) {
            String m = e.getMessage();
            return m.substring(0, m.indexOf(':'));
        } catch (Exception e) {
            throw new IllegalStateException("unexpected exception: " + e);
        }
        return "none";
    }

    /**
     * An identifier that resolves to nothing: ReferenceError on read, a global property on sloppy write, ReferenceError on strict
     * write, delete is true, and the call this value is undefined.
     * <p>
     * 어디에도 해석되지 않는 식별자: 읽으면 ReferenceError, 느슨한 쓰기는 전역 프로퍼티를 만들고, 엄격한 쓰기는 ReferenceError, delete 는 true,
     * 호출의 this 값은 undefined.
     */
    public void testUnresolvableReference() {
        setUp();
        final Reference sloppy = Reference.resolved(ex, null, "missingName", false);
        Check.isTrue(sloppy.isUnresolvableReference(), "unresolvable");
        Check.isFalse(sloppy.isPropertyReference(), "not a property reference");
        Check.isFalse(sloppy.isSuperReference(), "not a super reference");
        Check.isFalse(sloppy.isStrictReference(), "sloppy flag");
        Check.equal(Reference.UNRESOLVABLE, sloppy.getKind(), "kind");
        Check.equal("missingName", sloppy.getReferencedName(), "referenced name");
        Check.equal("ReferenceError", errorOf(new Block() {
            public void run() {
                sloppy.getValue();
            }
        }), "GetValue of an unresolvable reference");
        Check.isTrue(sloppy.delete(), "delete of an unresolvable reference is true");
        Check.isTrue(sloppy.thisValueForCall().isUndefined(), "this value for a call");
        sloppy.putValue(Js.num(5));
        Check.equal("5", Ops.toString(realm.getGlobalObject().get("missingName")), "sloppy PutValue creates a global property");
        final Reference strict = Reference.resolved(ex, null, "otherMissing", true);
        Check.isTrue(strict.isStrictReference(), "strict flag");
        Check.equal("ReferenceError", errorOf(new Block() {
            public void run() {
                strict.putValue(Js.num(1));
            }
        }), "strict PutValue of an unresolvable reference");
        Check.isFalse(realm.getGlobalObject().hasProperty("otherMissing"), "nothing was created");
    }

    /**
     * Identifiers bound in an environment: reads, writes, the temporal dead zone, constants, deletable bindings and
     * <code>with</code> object environments.
     * <p>
     * 환경에 바인딩된 식별자: 읽기, 쓰기, 일시적 사각지대, 상수, 삭제 가능한 바인딩, <code>with</code> 객체 환경.
     */
    public void testEnvironmentReference() {
        setUp();
        final Environment env = new Environment(null);
        env.declare("v", true, true, Js.num(1));
        env.declare("k", false, true, Js.num(2));
        env.declare("tdz", true, false, null);
        env.declare("del", true, true, Js.num(3)).deletable = true;
        final Reference v = Reference.resolved(ex, env, "v", false);
        Check.isFalse(v.isUnresolvableReference(), "resolved");
        Check.isFalse(v.isPropertyReference(), "identifier references are not property references");
        Check.equal(Reference.ENVIRONMENT, v.getKind(), "kind");
        Check.same(env, v.getEnvironment(), "environment");
        Check.equal("1", Ops.toString(v.getValue()), "GetValue");
        v.putValue(Js.num(9));
        Check.equal("9", Ops.toString(v.getValue()), "PutValue");
        Check.isFalse(v.delete(), "ordinary bindings cannot be deleted");
        Check.isTrue(v.thisValueForCall().isUndefined(), "this value for a call");
        final Reference k = Reference.resolved(ex, env, "k", true);
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                k.putValue(Js.num(5));
            }
        }), "assignment to a constant");
        final Reference tdz = Reference.resolved(ex, env, "tdz", false);
        Check.equal("ReferenceError", errorOf(new Block() {
            public void run() {
                tdz.getValue();
            }
        }), "read in the temporal dead zone");
        Check.equal("ReferenceError", errorOf(new Block() {
            public void run() {
                tdz.putValue(Js.num(1));
            }
        }), "write in the temporal dead zone");
        Reference del = Reference.resolved(ex, env, "del", false);
        Check.isTrue(del.delete(), "bindings created by eval code are deletable");
        Check.isNull(env.own("del"), "the binding is gone");
        JsObject withObject = realm.newObject();
        withObject.set("p", Js.num(4));
        Environment w = new Environment(env);
        w.withEnv = true;
        w.objectRecord = withObject;
        Reference p = Reference.resolved(ex, w, "p", false);
        Check.same(withObject, p.thisValueForCall(), "an identifier found in a with object calls with the object as this");
        Check.equal("4", Ops.toString(p.getValue()), "GetValue reads the object property");
        p.putValue(Js.num(6));
        Check.equal("6", Ops.toString(withObject.get("p")), "PutValue writes the object property");
        Check.isTrue(p.delete(), "delete removes the object property");
        Check.isFalse(withObject.hasProperty("p"), "the property is gone");
    }

    /**
     * Property references: base.name and base[key], primitives, null bases, read-only and non-configurable properties in both
     * modes, and the lazy key conversion of element references.
     * <p>
     * 프로퍼티 참조: base.name 과 base[key], 원시 값, null 기준, 두 모드에서의 읽기 전용/구성 불가 프로퍼티, 요소 참조의 지연된 키 변환.
     */
    public void testPropertyReference() {
        setUp();
        final JsObject o = realm.newObject();
        o.set("a", Js.num(1));
        Reference a = Reference.property(ex, o, "a", false);
        Check.isTrue(a.isPropertyReference(), "property reference");
        Check.isFalse(a.isUnresolvableReference(), "not unresolvable");
        Check.isFalse(a.isSuperReference(), "not super");
        Check.same(o, a.getBaseValue(), "base");
        Check.same(o, a.getThisValue(), "GetThisValue is the base");
        Check.same(o, a.thisValueForCall(), "this value for a call is the base");
        Check.equal("a", a.getReferencedName(), "referenced name");
        Check.equal("1", Ops.toString(a.getValue()), "GetValue");
        a.putValue(Js.num(2));
        Check.equal("2", Ops.toString(o.get("a")), "PutValue");
        Check.isTrue(a.delete(), "delete");
        Check.isFalse(o.hasProperty("a"), "deleted");
        Check.isTrue(Reference.property(ex, o, "never", false).delete(), "deleting a missing property is true");
        // 쓰기 금지와 구성 불가 프로퍼티는 모드에 따라 오류이거나 무시된다.
        realm.evaluateScript("var frozen = Object.freeze({ x: 1 });", "a.js");
        final JsObject frozen = (JsObject) realm.getGlobalObject().get("frozen");
        final Reference sloppyX = Reference.property(ex, frozen, "x", false);
        sloppyX.putValue(Js.num(2));
        Check.equal("1", Ops.toString(frozen.get("x")), "sloppy write to a read-only property is ignored");
        Check.isFalse(sloppyX.delete(), "sloppy delete of a non-configurable property is false");
        final Reference strictX = Reference.property(ex, frozen, "x", true);
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                strictX.putValue(Js.num(2));
            }
        }), "strict write to a read-only property");
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                strictX.delete();
            }
        }), "strict delete of a non-configurable property");
        // 원시 값 기준과 null 기준.
        Reference len = Reference.property(ex, JsString.of("abc"), "length", false);
        Check.equal("3", Ops.toString(len.getValue()), "a property of a primitive");
        final Reference onPrimitive = Reference.property(ex, Js.num(5), "p", true);
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                onPrimitive.putValue(Js.num(1));
            }
        }), "strict write to a property of a primitive");
        final Reference onNull = Reference.property(ex, Js.NULL, "p", false);
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                onNull.getValue();
            }
        }), "read of a property of null");
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                onNull.putValue(Js.num(1));
            }
        }), "write to a property of null");
        Check.equal("TypeError", errorOf(new Block() {
            public void run() {
                onNull.delete();
            }
        }), "delete of a property of null");
    }

    /**
     * Element references convert the key once per use and fast paths keep array semantics.
     * <p>
     * 요소 참조는 사용할 때 키를 변환하며, 빠른 경로도 배열 의미를 유지한다.
     */
    public void testElementReference() {
        setUp();
        realm.evaluateScript("var count = 0; var key = { toString: function () { count++; return 'k'; } }; var o = {}; var arr = [10, 20];", "a.js");
        JsObject o = (JsObject) realm.getGlobalObject().get("o");
        JsValue key = realm.getGlobalObject().get("key");
        Reference r = Reference.element(ex, o, key, false);
        Check.equal("0", Ops.toString(realm.getGlobalObject().get("count")), "creating the reference does not convert the key");
        r.putValue(Js.num(7));
        Check.equal("1", Ops.toString(realm.getGlobalObject().get("count")), "PutValue converts the key once");
        Check.equal("7", Ops.toString(o.get("k")), "the converted key is used");
        Check.equal("7", Ops.toString(r.getValue()), "GetValue");
        Check.equal("k", r.getReferencedName(), "GetReferencedName converts the key");
        JsObject arr = (JsObject) realm.getGlobalObject().get("arr");
        Reference e1 = Reference.element(ex, arr, Js.num(1), false);
        Check.equal("20", Ops.toString(e1.getValue()), "array element read");
        e1.putValue(Js.num(21));
        Check.equal("21", Ops.toString(arr.get("1")), "array element write");
        Reference grow = Reference.element(ex, arr, Js.num(5), false);
        grow.putValue(Js.num(1));
        Check.equal("6", Ops.toString(arr.get("length")), "writing past the end grows the array");
        Check.isTrue(Reference.element(ex, arr, Js.num(5), false).delete(), "delete of an element");
        Check.isFalse(arr.hasProperty("5"), "element gone");
        Check.equal("b", Ops.toString(Reference.element(ex, JsString.of("abc"), Js.num(1), false).getValue()), "string index");
    }

    /**
     * Super references read with the lookup start object but use the explicit this value for accessors and writes.
     * <p>
     * super 참조는 조회 시작 객체에서 읽지만 접근자와 쓰기에는 따로 저장한 this 값을 쓴다.
     */
    public void testSuperReference() {
        setUp();
        realm.evaluateScript("var proto = { get who() { return this.tag; }, plain: 'p' }; var self = { tag: 'me' };", "a.js");
        JsObject proto = (JsObject) realm.getGlobalObject().get("proto");
        final JsObject self = (JsObject) realm.getGlobalObject().get("self");
        Reference who = Reference.superProperty(ex, self, proto, "who", true);
        Check.isTrue(who.isSuperReference(), "super reference");
        Check.isTrue(who.isPropertyReference(), "a super reference is a property reference");
        Check.same(self, who.getThisValue(), "GetThisValue is the explicit this");
        Check.same(self, who.thisValueForCall(), "calls through super use this");
        Check.equal("me", Ops.toString(who.getValue()), "the accessor runs with this as receiver");
        Check.equal("who", who.getReferencedName(), "name");
        Reference w = Reference.superProperty(ex, self, proto, "written", false);
        w.putValue(Js.num(3));
        Check.equal("3", Ops.toString(self.get("written")), "the write goes to this");
        Check.isFalse(proto.hasProperty("written"), "not to the prototype");
        Check.equal("ReferenceError", errorOf(new Block() {
            public void run() {
                Reference.superProperty(ex, self, null, "x", false).delete();
            }
        }), "delete of a super reference");
        Check.isTrue(Reference.superProperty(ex, self, null, "x", false).getValue().isUndefined(), "no home prototype gives undefined");
    }

    /**
     * The interpreter derives references and this values from the syntax: assignment, update, delete, typeof and calls.
     * <p>
     * 인터프리터는 구문에서 참조와 this 값을 구한다: 대입, 갱신, delete, typeof, 호출.
     */
    public void testInterpreterUsesReferences() {
        Scripts.checkInline(
            "var o = { v: 1, f: function () { return this; }, g: function () { 'use strict'; return this; } };"
            // 호출의 this 값: 프로퍼티 참조의 기준, 괄호는 참조를 유지하고 쉼표는 값으로 바꾼다.
            + "t('call through a member', o.f() === o, true);"
            + "t('call through a computed member', o['f']() === o, true);"
            + "t('parenthesized member keeps the reference', (o.f)() === o, true);"
            + "t('comma makes it a plain value', (0, o.f)() === globalThis, true);"
            + "t('assignment expression makes it a plain value', (o.f = o.f)() === globalThis, true);"
            + "t('strict function through a plain identifier', (function () { var g = o.g; return g(); })(), undefined);"
            + "t('with object is the this value', (function () { var w = { h: function () { return this; } }; with (w) { return h() === w; } })(), true);"
            // 갱신과 복합 대입은 참조를 한 번만 평가한다.
            + "var log = []; function base() { log.push('b'); return o; } function key() { log.push('k'); return 'v'; }"
            + "base()[key()]++; base()[key()] += 5; --base()[key()];"
            + "t('references are evaluated once per operation', log.join(''), 'bkbkbk');"
            + "t('update result', o.v, 6);"
            + "t('postfix returns the old value', (function () { var x = 5; return x++ + x; })(), 11);"
            // typeof 는 해석되지 않는 참조를 오류 없이 처리한다.
            + "t('typeof unresolvable', typeof neverDeclared, 'undefined');"
            + "t('typeof TDZ throws', (function () { try { typeof tdzVar; let tdzVar; } catch (e) { return e.name; } })(), 'ReferenceError');"
            + "t('typeof member of unresolvable throws', (function () { try { typeof neverDeclared.x; } catch (e) { return e.name; } })(), 'ReferenceError');"
            // delete 는 참조의 종류에 따라 다르다.
            + "t('delete property', delete o.v, true);"
            + "t('delete missing', delete o.nothing, true);"
            + "t('delete unresolvable', delete neverDeclared2, true);"
            + "t('delete variable', (function () { var q; return delete q; })(), false);"
            + "t('delete a non reference', delete 5, true);"
            + "t('delete evaluates the base once', (function () { var n = 0; function b() { n++; return {}; } delete b().x; return n; })(), 1);"
            // 대입은 해석되지 않는 참조에서 엄격 모드 여부에 따라 다르다.
            + "undeclaredSloppy = 1; t('sloppy assignment creates a global', globalThis.undeclaredSloppy, 1);"
            + "t('strict assignment is a ReferenceError', (function () { 'use strict'; try { undeclaredStrict = 1; } catch (e) { return e.name; } })(), 'ReferenceError');"
            + "t('binding resolved before the right side', (function () { var r = 'outer'; function f() { var x = (r = 'seen', 1); return x; } return f() + r; })(), '1seen');"
            + "t('super reference in a method', (function () { var p = { m: function () { return 'p'; } }; var c = { __proto__: p, m() { return super.m() + 'c'; } }; return c.m(); })(), 'pc');"
            + "t('super reference keeps this', (function () { var p = { who: function () { return this.id; } }; var c = { __proto__: p, id: 'c', w() { return super.who(); } }; return c.w(); })(), 'c');"
            // 꼬리 호출도 같은 Reference 에서 this 값을 구한다.
            + "t('strict tail call through a member', (function () { 'use strict'; var s = { id: 7, m: function (n) { return n === 0 ? this.id : this.m(n - 1); } }; return s.m(50000); })(), 7);",
            25);
    }

    /**
     * References are evaluated before the right side and the same way inside a generator that suspends in between.
     * <p>
     * 참조는 오른쪽보다 먼저 평가되며, 그 사이에 중단하는 제너레이터 안에서도 같게 평가된다.
     */
    public void testReferencesInGenerators() {
        Scripts.checkInline(
            "var log = [];"
            + "function mk(n) { log.push('mk' + n); return { v: 1 }; }"
            + "function* g() { mk(1).v += yield 'a'; mk(2).v = yield 'b'; var o = mk(3); o.v++; (yield 'c').x = 1; return log.join(); }"
            + "var it = g(); it.next(); it.next(5); it.next(7); var target = {}; var r = it.next(target);"
            + "t('each reference is evaluated once across suspensions', r.value, 'mk1,mk2,mk3');"
            + "t('the value after resume is used', target.x, 1);"
            + "function* h() { var o = { n: 1 }; o.n += yield 1; o.n++; return o.n; }"
            + "var hi = h(); hi.next(); t('compound assignment in a generator', hi.next(10).value, 12);"
            + "function* c() { var f = function () { return 'called'; }; return f(yield 1); }"
            + "var ci = c(); ci.next(); t('call in a generator', ci.next(0).value, 'called');"
            + "function* d() { var o = { p: 1 }; var r = delete o[yield 'k']; return r + ',' + ('p' in o); }"
            + "var di = d(); di.next(); t('delete with a suspended key', di.next('p').value, 'true,false');",
            5);
    }

    /**
     * Native functions registered with a host object receive the base object of a member call as this (and undefined when detached).
     * <p>
     * 호스트 객체에 등록한 네이티브 함수도 스크립트 함수처럼 멤버 호출의 기준 객체를 this 로 받는다.
     */
    public void testHostFunctionsReceiveReferenceBase() {
        setUp();
        final JsObject host = realm.newObject();
        final JsValue[] seen = new JsValue[1];
        realm.method(host, "who", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen[0] = thisValue;
                return Js.UNDEFINED;
            }
        });
        realm.getGlobalObject().defineBuiltin("host", host);
        realm.evaluateScript("host.who(); var w = host.who; w();", "a.js");
        Check.isTrue(seen[0].isUndefined(), "a detached call of a host function passes undefined");
        realm.evaluateScript("host.who();", "b.js");
        Check.same(host, seen[0], "a member call passes the base object");
    }
}
