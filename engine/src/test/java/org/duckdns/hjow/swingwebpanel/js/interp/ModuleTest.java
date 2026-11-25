package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of ES2015 modules (plan item P13-10): import/export forms, live bindings, namespace objects, re-exports, cyclic
 * dependencies, instantiation errors and evaluation order, through a map based {@link ModuleLoader}.
 * <p>
 * ES2015 모듈 시험(계획 항목 P13-10): import/export 형태, 실시간 바인딩, 네임스페이스 객체, 재수출, 순환 의존성, 인스턴스화 오류, 평가
 * 순서를 맵 기반 {@link ModuleLoader} 로 시험한다.
 */
public class ModuleTest {

    /**
     * A loader that serves modules from a map and resolves relative specifiers against the referrer's directory.
     * <p>
     * 맵에서 모듈을 제공하고 상대 지정자를 참조 모듈의 디렉터리 기준으로 해석하는 로더.
     */
    private static final class MapLoader implements ModuleLoader {
        /**
         * The module sources by key.
         * <p>
         * 키별 모듈 소스.
         */
        final Map<String, String> sources = new HashMap<String, String>();
        /**
         * The keys fetched, in order.
         * <p>
         * 가져온 키를 순서대로 기록.
         */
        final List<String> fetched = new ArrayList<String>();

        /**
         * Resolves a specifier: <code>./x.js</code> and <code>../x.js</code> are relative to the referrer, others are used as is.
         * <p>
         * 지정자를 해석한다: <code>./x.js</code>, <code>../x.js</code> 는 참조 모듈 기준이고 그 밖에는 그대로 쓴다.
         *
         * @param specifier the specifier / 지정자
         * @param referrer  the referring module key / 참조하는 모듈 키
         * @return the key / 키
         */
        public String resolve(String specifier, String referrer) {
            if (!specifier.startsWith("./") && !specifier.startsWith("../")) {
                return specifier;
            }
            String dir = referrer.substring(0, referrer.lastIndexOf('/') + 1);
            String path = dir + specifier;
            // "./" 과 "x/../" 조각을 제거해 경로를 정규화한다.
            while (path.indexOf("/./") >= 0) {
                path = path.replace("/./", "/");
            }
            int up;
            // 연속한 "./" 조각을 모두 제거한다.
            while ((up = path.indexOf("/../")) >= 0) {
                int prev = path.lastIndexOf('/', up - 1);
                path = path.substring(0, prev + 1) + path.substring(up + 4);
            }
            return path;
        }

        /**
         * Returns the source of a module.
         * <p>
         * 모듈 소스를 반환한다.
         *
         * @param key the key / 키
         * @return the source / 소스
         */
        public String fetch(String key) {
            fetched.add(key);
            String s = sources.get(key);
            if (s == null) {
                throw JsException.typeError("Cannot find module '" + key + "'");
            }
            return s;
        }
    }

    /**
     * The loader of the current case.
     * <p>
     * 현재 사례의 로더.
     */
    private MapLoader loader;
    /**
     * The captured console of the current case.
     * <p>
     * 현재 사례에서 모은 콘솔 출력.
     */
    private Scripts.Capture capture;
    /**
     * The realm of the current case.
     * <p>
     * 현재 사례의 realm.
     */
    private JsRealm realm;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModuleTest() {
    }

    /**
     * Prepares a fresh realm with a map loader. The arguments are alternating keys and sources.
     * <p>
     * 맵 로더가 있는 새 realm 을 준비한다. 인수는 키와 소스를 번갈아 준다.
     *
     * @param pairs key, source, key, source, ... / 키, 소스, 키, 소스, ...
     */
    private void setUp(String... pairs) {
        loader = new MapLoader();
        capture = new Scripts.Capture();
        realm = Scripts.realm(capture);
        realm.setModuleLoader(loader);
        // 키와 소스 쌍을 로더에 등록한다.
        for (int i = 0; i < pairs.length; i += 2) {
            loader.sources.put(pairs[i], pairs[i + 1]);
        }
    }

    /**
     * Evaluates a module and returns what it printed joined with commas.
     * <p>
     * 모듈을 실행하고 출력한 내용을 쉼표로 이어 반환한다.
     *
     * @param key the entry module key / 진입 모듈 키
     * @return the printed lines / 출력한 줄
     */
    private String run(String key) {
        realm.evaluateModule(key, null);
        StringBuilder sb = new StringBuilder();
        // 출력한 줄을 쉼표로 잇는다.
        for (int i = 0; i < capture.lines.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(capture.lines.get(i));
        }
        return sb.toString();
    }

    /**
     * Returns the description of the exception thrown by evaluating the module, or null.
     * <p>
     * 모듈 실행이 던진 예외의 설명을 반환한다. 없으면 null.
     *
     * @param key the entry module key / 진입 모듈 키
     * @return the exception text or null / 예외 문자열 또는 null
     */
    private String error(String key) {
        try {
            realm.evaluateModule(key, null);
            return null;
        } catch (JsException e) {
            return e.getMessage();
        }
    }

    /**
     * Rejects an unsupported attribute instead of fetching the unqualified request.
     * <p>
     * 지원하지 않는 속성을 거부하고 속성 없는 모듈 요청으로 바꾸어 가져오지 않는다.
     */
    public void testUnsupportedStaticAttribute() {
        setUp("root.js", "export * from 'dep.js' with {unknown:'x'};",
                "dep.js", "export const value = 1;");
        Check.isTrue(error("root.js").indexOf("import attribute") >= 0, "unsupported import attribute");
        Check.equal("[root.js]", loader.fetched.toString(), "no dependency fetch");
    }

    /**
     * Named exports and imports, renaming, and the namespace import.
     * <p>
     * 이름 있는 export 와 import, 이름 바꾸기, 네임스페이스 import.
     */
    public void testNamedImportsAndExports() {
        setUp("m/a.js", "export var v = 1; export let l = 2; export const c = 3; export function f() { return 4; } export class K { }"
                + "var hidden = 5; export { hidden as shown }; export { v as v2 };",
                "m/main.js", "import { v, l, c, f, K, shown, v2 } from './a.js'; print(v + ',' + l + ',' + c + ',' + f() + ',' + typeof K + ',' + shown + ',' + v2);"
                + "import { v as renamed } from './a.js'; print(renamed);"
                + "import * as ns from './a.js'; print(ns.c + ',' + ns.shown);");
        Check.equal("1,2,3,4,function,5,1,1,3,5", run("m/main.js"), "named imports");
    }

    /**
     * Default exports in all their forms.
     * <p>
     * 모든 형태의 default export.
     */
    public void testDefaultExports() {
        setUp("d/expr.js", "export default 40 + 2;",
                "d/fn.js", "export default function () { return 'anon'; }",
                "d/namedfn.js", "export default function named() { return typeof named; }",
                "d/cls.js", "export default class { static who() { return 'cls'; } }",
                "d/namedcls.js", "export default class Named { }",
                "d/asdefault.js", "var local = 'L'; export { local as default };",
                "d/gen.js", "export default function* () { yield 1; yield 2; }",
                "d/main.js", "import e from './expr.js'; import f from './fn.js'; import nf from './namedfn.js'; import c from './cls.js'; import nc from './namedcls.js';"
                + "import a from './asdefault.js'; import g from './gen.js'; import { default as e2 } from './expr.js';"
                + "print(e + ',' + f() + ',' + nf() + ',' + c.who() + ',' + nc.name + ',' + a + ',' + [...g()].join('') + ',' + e2);"
                + "print(f.name + '|' + c.name + '|' + nf.name);");
        Check.equal("42,anon,function,cls,Named,L,12,42,default|default|named", run("d/main.js"), "default exports");
    }

    /**
     * Live bindings: importers see later changes of the exporting module's variables.
     * <p>
     * 실시간 바인딩: import 한 쪽이 export 하는 모듈의 변수 변경을 나중에도 본다.
     */
    public void testLiveBindings() {
        setUp("l/counter.js", "export let count = 0; export function inc() { count++; } export var viaVar = 'a'; export function setVar(x) { viaVar = x; }",
                "l/main.js", "import { count, inc, viaVar, setVar } from './counter.js'; import * as ns from './counter.js';"
                + "print(count); inc(); inc(); print(count); print(ns.count); setVar('b'); print(viaVar + ns.viaVar);");
        Check.equal("0,2,2,bb", run("l/main.js"), "live bindings");
    }

    /**
     * Imported bindings are read only and namespace objects are immutable views.
     * <p>
     * import 한 바인딩은 읽기 전용이고 네임스페이스 객체는 변경할 수 없는 뷰이다.
     */
    public void testImportsAreReadOnly() {
        setUp("r/a.js", "export let x = 1; export function bump() { x = 2; }",
                "r/main.js", "import { x, bump } from './a.js'; import * as ns from './a.js';"
                + "var r = [];"
                + "try { x = 5; } catch (e) { r.push(e.name); }"
                + "try { x++; } catch (e) { r.push(e.name); }"
                + "try { ns.x = 9; } catch (e) { r.push('ns:' + e.name); }"
                + "try { ns.y = 9; } catch (e) { r.push('add:' + e.name); }"
                + "try { delete ns.x; } catch (e) { r.push('del:' + e.name); }"
                + "bump(); r.push(x); print(r.join('|'));");
        Check.equal("TypeError|TypeError|ns:TypeError|add:TypeError|del:TypeError|2", run("r/main.js"), "read-only imports");
    }

    /**
     * The module namespace object: keys, attributes, tag, extensibility and prototype.
     * <p>
     * 모듈 네임스페이스 객체: 키, 속성, 태그, 확장성, 프로토타입.
     */
    public void testNamespaceObject() {
        setUp("n/a.js", "export var b = 2; export var a = 1; export default 'dflt'; export function z() { }",
                "n/main.js", "import * as ns from './a.js';"
                + "print(Object.keys(ns).join('')); print(Object.getPrototypeOf(ns)); print(Object.prototype.toString.call(ns));"
                + "print(Object.isExtensible(ns)); print(ns[Symbol.toStringTag]);"
                + "var d = Object.getOwnPropertyDescriptor(ns, 'a'); print(d.writable + ',' + d.enumerable + ',' + d.configurable + ',' + d.value);"
                + "print(ns.default); print('a' in ns); print('q' in ns); print(typeof ns); print(Reflect_ish(ns));"
                + "function Reflect_ish(o) { var n = 0; for (var k in o) n++; return n; }");
        Check.equal("abdefaultz,null,[object Module],false,Module,true,true,false,1,dflt,true,false,object,4", run("n/main.js"), "namespace object");
    }

    /**
     * Re-exports: <code>export * from</code>, <code>export { x as y } from</code> and conflicts.
     * <p>
     * 재수출: <code>export * from</code>, <code>export { x as y } from</code>, 충돌.
     */
    public void testReExports() {
        setUp("x/base.js", "export var one = 1; export var two = 2; export default 'basedefault';",
                "x/other.js", "export var three = 3; export var two = 'other-two';",
                "x/barrel.js", "export * from './base.js'; export * from './other.js'; export { one as uno } from './base.js'; export { default as baseDefault } from './base.js';"
                + "export var own = 'own';",
                "x/main.js", "import * as b from './barrel.js'; import { one, three, uno, baseDefault, own } from './barrel.js';"
                + "print(one + ',' + three + ',' + uno + ',' + baseDefault + ',' + own); print(Object.keys(b).join('|'));"
                + "print(b.default === undefined);",
                "x/ambiguous.js", "import { two } from './barrel.js'; print(two);");
        Check.equal("1,3,1,basedefault,own,baseDefault|one|own|three|uno,true", run("x/main.js"), "re-exports");
        Check.contains(error("x/ambiguous.js"), "SyntaxError", "an ambiguous star export cannot be imported");
    }

    /**
     * Export forms with declarations, destructuring and lists.
     * <p>
     * 선언, 분해, 목록을 쓰는 export 형태.
     */
    public void testExportForms() {
        setUp("e/a.js", "export let p = 1, q = 2; export var { r, s: [t] } = { r: 3, s: [4] }; export const [u, ...w] = [5, 6, 7];"
                + "function g1() { return 'g1'; } function g2() { return 'g2'; } export { g1, g2 as renamedG2 };"
                + "export function* gen() { yield 'gen'; } export class C2 { } export { C2 as AlsoC2 };",
                "e/main.js", "import * as m from './a.js'; print(m.p + ',' + m.q + ',' + m.r + ',' + m.t + ',' + m.u + ',' + m.w.join('')); print(m.g1() + m.renamedG2());"
                + "print(m.C2 === m.AlsoC2); print(m.gen().next().value);");
        Check.equal("1,2,3,4,5,67,g1g2,true,gen", run("e/main.js"), "export forms");
    }

    /**
     * Modules are evaluated once, in dependency order, even when imported many times.
     * <p>
     * 모듈은 여러 번 import 되어도 의존성 순서로 한 번만 평가된다.
     */
    public void testEvaluationOrderAndSingleEvaluation() {
        setUp("o/leaf.js", "print('leaf'); export var id = 'leaf';",
                "o/left.js", "import { id } from './leaf.js'; print('left'); export var l = id;",
                "o/right.js", "import './leaf.js'; import { id } from './leaf.js'; print('right'); export var r = id;",
                "o/main.js", "print('main-start'); import { l } from './left.js'; import { r } from './right.js'; import './leaf.js'; print('main:' + l + r);");
        Check.equal("leaf,left,right,main-start,main:leafleaf", run("o/main.js"), "imports are hoisted and each module runs once");
        Check.equal(1, countOf(loader.fetched, "o/leaf.js"), "the leaf was fetched once");
    }

    /**
     * Counts the occurrences of a string in a list.
     * <p>
     * 목록에서 문자열이 나온 횟수를 센다.
     *
     * @param list the list / 목록
     * @param s    the string / 문자열
     * @return the count / 횟수
     */
    private static int countOf(List<String> list, String s) {
        int n = 0;
        // 같은 키가 몇 번 나왔는지 센다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(s)) {
                n++;
            }
        }
        return n;
    }

    /**
     * A second import of the same module key reuses the instance and its state.
     * <p>
     * 같은 모듈 키를 다시 import 하면 인스턴스와 상태를 재사용한다.
     */
    public void testModuleInstancesAreShared() {
        setUp("s/state.js", "export var n = 0; export function bump() { return ++n; }",
                "s/one.js", "import { bump } from './state.js'; bump(); bump();",
                "s/two.js", "import { n } from './state.js'; import './one.js'; print(n);");
        Check.equal("2", run("s/two.js"), "state shared between importers");
        JsObject ns1 = realm.evaluateModule("s/state.js", null);
        JsObject ns2 = realm.evaluateModule("s/state.js", null);
        Check.same(ns1, ns2, "the same namespace object is returned for the same key");
        Check.equal("2", Ops.toString(ns1.get("n")), "the host sees the live value");
    }

    /**
     * Cyclic imports: function declarations are usable early, let/const are in their TDZ, and evaluation order follows the
     * depth first traversal.
     * <p>
     * 순환 import: 함수 선언은 일찍 쓸 수 있고 let/const 는 TDZ 에 있으며 평가 순서는 깊이 우선 순회를 따른다.
     */
    public void testCyclicDependencies() {
        setUp("c/a.js", "import { b, bfn } from './b.js'; print('a-start'); export let a = 'A'; export function afn() { return 'afn'; } print('a-sees:' + b + bfn());",
                "c/b.js", "import { a, afn } from './a.js'; print('b-start'); print(afn());"
                + "var early; try { early = a; } catch (e) { early = e.name; } print('b-early:' + early);"
                + "export let b = 'B'; export function bfn() { return 'bfn'; }",
                "c/main.js", "import './a.js'; print('main');");
        Check.equal("b-start,afn,b-early:ReferenceError,a-start,a-sees:Bbfn,main", run("c/main.js"), "cycle a <-> b");
    }

    /**
     * A module importing itself and a three module cycle.
     * <p>
     * 자기 자신을 import 하는 모듈과 모듈 세 개의 순환.
     */
    public void testSelfAndTriangleCycles() {
        setUp("t/self.js", "import { v as w } from './self.js'; export var v = 'self'; print(w);",
                "t/a.js", "import { b } from './b.js'; export var a = 'a'; print('a:' + b);",
                "t/b.js", "import { c } from './c.js'; export var b = 'b'; print('b:' + c);",
                "t/c.js", "import { a } from './a.js'; export var c = 'c'; print('c:' + a);");
        Check.equal("self", run("t/self.js"), "self import");
        capture.lines.clear();
        setUp("t/a.js", "import { b } from './b.js'; export var a = 'a'; print('a:' + b);",
                "t/b.js", "import { c } from './c.js'; export var b = 'b'; print('b:' + c);",
                "t/c.js", "import { a } from './a.js'; export var c = 'c'; print('c:' + a);");
        Check.equal("c:undefined,b:c,a:b", run("t/a.js"), "triangle cycle (var bindings start as undefined)");
    }

    /**
     * Instantiation errors: missing and ambiguous exports are SyntaxErrors detected before any module body runs.
     * <p>
     * 인스턴스화 오류: 없는 export 와 모호한 export 는 어떤 모듈 본문도 실행하기 전에 감지되는 SyntaxError 이다.
     */
    public void testLinkErrors() {
        setUp("k/a.js", "print('a ran'); export var x = 1;",
                "k/missing.js", "import { nothere } from './a.js'; print('no');",
                "k/star-missing.js", "export * from './a.js'; import { nope } from './star-missing.js';",
                "k/reexport-missing.js", "export { zzz } from './a.js';",
                "k/ok.js", "import { x } from './a.js'; print(x);");
        String e1 = error("k/missing.js");
        Check.contains(e1, "SyntaxError", "missing import: " + e1);
        Check.contains(e1, "nothere", "the message names the binding: " + e1);
        Check.equal("", String.valueOf(capture.lines.size() == 0 ? "" : capture.lines), "no module body ran before the link error");
        String e2 = error("k/star-missing.js");
        Check.contains(e2, "SyntaxError", "missing import through a self star export: " + e2);
        String e3 = error("k/reexport-missing.js");
        Check.contains(e3, "SyntaxError", "re-export of a missing binding: " + e3);
        Check.equal("a ran,1", run("k/ok.js"), "a valid module still loads after failures");
    }

    /**
     * Loading errors: unknown modules, syntax errors in dependencies and the absence of a loader.
     * <p>
     * 로딩 오류: 알 수 없는 모듈, 의존 모듈의 구문 오류, 로더 부재.
     */
    public void testLoadErrors() {
        setUp("z/main.js", "import './missing.js'; print('never');",
                "z/bad.js", "export var x = ;",
                "z/importsbad.js", "import './bad.js'; print('never');");
        String e1 = error("z/main.js");
        Check.contains(e1, "Cannot find module", "missing dependency: " + e1);
        String e2 = error("z/importsbad.js");
        Check.contains(e2, "SyntaxError", "syntax error in a dependency: " + e2);
        Check.equal(0, capture.lines.size(), "nothing ran");
        JsRealm bare = Scripts.realm(new Scripts.Capture());
        try {
            bare.evaluateModule("noloader.js", "import './x.js';");
            Check.fail("an import without a loader must fail");
        } catch (JsException e) {
            Check.contains(e.getMessage(), "loader", "no loader message: " + e.getMessage());
        }
        Check.equal("5", Ops.toString(bare.evaluateModule("noimports.js", "export var five = 5;").get("five")), "a module without imports needs no loader");
    }

    /**
     * An exception during evaluation propagates to the host, and later imports of the same failed module throw again.
     * <p>
     * 평가 중 예외는 호스트로 전파되며, 실패한 같은 모듈을 나중에 import 해도 다시 던진다.
     */
    public void testEvaluationErrors() {
        setUp("v/boom.js", "print('boom runs'); throw new RangeError('exploded');",
                "v/main.js", "import './boom.js'; print('never');",
                "v/again.js", "import './boom.js'; print('never2');");
        String e1 = error("v/main.js");
        Check.equal("RangeError: exploded", e1, "the thrown error reaches the host");
        String e2 = error("v/again.js");
        Check.contains(String.valueOf(e2), "RangeError", "a failed module stays failed: " + e2);
        Check.equal(1, countOf(capture.lines, "boom runs"), "the failing body ran once");
    }

    /**
     * Module code is strict and has its own scope: top-level this is undefined and declarations are not global.
     * <p>
     * 모듈 코드는 엄격 모드이며 자기 스코프를 가진다: 최상위 this 는 undefined 이고 선언은 전역이 아니다.
     */
    public void testModuleScopeAndStrictness() {
        setUp("q/main.js", "var topVar = 1; let topLet = 2; function topFn() { }"
                + "print(this === undefined); print(typeof topVar + typeof globalThis.topVar); print(typeof globalThis.topFn);"
                + "print((function () { return this; })() === undefined);"
                + "try { undeclared = 1; print('sloppy'); } catch (e) { print(e.name); }"
                + "print(typeof import_meta_absent);",
                "q/with.js", "with ({}) { }",
                "q/octal.js", "var n = 010;",
                "q/dup.js", "export var a = 1; export var a = 2;",
                "q/dupimport.js", "import { x } from './main.js'; import { x } from './main.js';",
                "q/exportundeclared.js", "export { notDeclared };",
                "q/delete.js", "var o = 1; delete o;");
        Check.equal("true,numberundefined,undefined,true,ReferenceError,undefined", run("q/main.js"), "module scope");
        Check.contains(error("q/with.js"), "SyntaxError", "with in a module");
        Check.contains(error("q/octal.js"), "SyntaxError", "legacy octal in a module");
        Check.contains(error("q/dup.js"), "SyntaxError", "duplicate export name");
        Check.contains(error("q/dupimport.js"), "SyntaxError", "duplicate import binding");
        Check.contains(error("q/exportundeclared.js"), "SyntaxError", "export of an undeclared name");
        Check.contains(error("q/delete.js"), "SyntaxError", "delete of an identifier in strict code");
    }

    /**
     * Import and export declarations are only valid at the top level of a module and are not valid in scripts.
     * <p>
     * import/export 선언은 모듈의 최상위에서만 유효하며 스크립트에서는 유효하지 않다.
     */
    public void testImportExportPlacement() {
        setUp();
        String[] scripts = { "import x from 'm';", "export var a = 1;", "{ import x from 'm'; }" };
        // 스크립트에서 import/export 구문은 SyntaxError 이다.
        for (int i = 0; i < scripts.length; i++) {
            Check.contains(Scripts.thrown(scripts[i]), "SyntaxError", "script: " + scripts[i]);
        }
        loader.sources.put("p/nested.js", "{ export var a = 1; }");
        loader.sources.put("p/infunction.js", "function f() { import x from 'm'; }");
        Check.contains(error("p/nested.js"), "SyntaxError", "export in a block");
        Check.contains(error("p/infunction.js"), "SyntaxError", "import in a function");
    }

    /**
     * Import forms: default plus named, default plus namespace, side-effect only and trailing commas.
     * <p>
     * import 형태: default 와 이름, default 와 네임스페이스, 부작용만, 뒤쪽 쉼표.
     */
    public void testImportForms() {
        setUp("i/a.js", "export default 'D'; export var n1 = 1; export var n2 = 2; print('a ran');",
                "i/main.js", "import d, { n1, n2 as two, } from './a.js'; import d2, * as ns from './a.js'; import './a.js';"
                + "print(d + n1 + two + ns.n1 + d2);");
        Check.equal("a ran,D121D", run("i/main.js"), "import forms");
    }

    /**
     * Execution limits apply while a module body runs, and the realm stays usable afterwards.
     * <p>
     * 모듈 본문 실행에도 실행 제한이 적용되며 이후에도 realm 을 쓸 수 있다.
     */
    public void testLimitsApplyToModules() {
        setUp("lim/loop.js", "while (true) { }", "lim/ok.js", "export var fine = 'fine';");
        realm.getLimits().setMaxSteps(200000L);
        try {
            realm.evaluateModule("lim/loop.js", null);
            Check.fail("an endless module must be aborted");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, e.getReason(), "reason");
        }
        Check.equal("fine", Ops.toString(realm.evaluateModule("lim/ok.js", null).get("fine")), "the realm is still usable");
    }

    /**
     * Modules can use classes, generators, destructuring and closures like scripts do, and can export all of them.
     * <p>
     * 모듈도 스크립트처럼 클래스, 제너레이터, 분해, 클로저를 쓸 수 있고 모두 export 할 수 있다.
     */
    public void testModulesRunFullLanguage() {
        setUp("f/lib.js", "export class Stack { constructor() { this.items = []; } push(x) { this.items.push(x); return this; } get size() { return this.items.length; } }"
                + "export function* range(a, b) { for (var i = a; i < b; i++) yield i; }"
                + "export const pair = ([a, b]) => ({ a, b });",
                "f/main.js", "import { Stack, range, pair } from './lib.js';"
                + "var s = new Stack().push(1).push(2); var { a, b } = pair([...range(5, 7)]);"
                + "print(s.size + ',' + a + ',' + b);");
        Check.equal("2,5,6", run("f/main.js"), "full language inside modules");
    }

    /**
     * The host can evaluate a module from given source without a loader entry and read exports from its namespace.
     * <p>
     * 호스트는 로더 항목 없이 주어진 소스로 모듈을 실행하고 네임스페이스에서 export 를 읽을 수 있다.
     */
    public void testHostEvaluatesModuleSource() {
        setUp("h/dep.js", "export var base = 10;");
        JsObject ns = realm.evaluateModule("h/entry.js", "import { base } from './dep.js'; export var doubled = base * 2; export default 'hd';");
        Check.equal("20", Ops.toString(ns.get("doubled")), "export value");
        Check.equal("hd", Ops.toString(ns.get("default")), "default export");
        Check.equal(1, countOf(loader.fetched, "h/dep.js"), "the dependency was fetched through the loader");
        Check.equal(0, countOf(loader.fetched, "h/entry.js"), "the entry source was given, not fetched");
    }
}
