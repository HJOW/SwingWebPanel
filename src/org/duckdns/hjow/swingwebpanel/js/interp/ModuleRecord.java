package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.ProgramInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.ScopeInfo;
import org.duckdns.hjow.swingwebpanel.js.parser.Parser;

/**
 * A Source Text Module Record (ECMA-262 6th edition 15.2.1.16): the parsed module with its import and export tables,
 * environment, requested modules and evaluation state. Loading resolves and parses the module graph through the host's
 * {@link ModuleLoader}; linking creates the environments and connects imports to the exporting bindings (so imports are live);
 * evaluation runs the module bodies depth first. Cyclic dependencies are supported: a module that is already being evaluated is
 * not entered again, so it may observe uninitialized bindings of a cycle member exactly like in the specification.
 * <p>
 * 소스 텍스트 모듈 레코드(ECMA-262 6판 15.2.1.16): import/export 표, 환경, 요청 모듈, 평가 상태를 가진 파싱된 모듈. 로딩은 호스트의
 * {@link ModuleLoader} 로 모듈 그래프를 해석하고 파싱하며, 링크는 환경을 만들고 import 를 export 하는 바인딩에 연결하고(그래서
 * import 는 실시간 연결), 평가는 모듈 본문을 깊이 우선으로 실행한다. 순환 의존성을 지원한다: 이미 평가 중인 모듈에는 다시 들어가지
 * 않으므로 규격과 똑같이 순환의 다른 모듈의 초기화되지 않은 바인딩을 볼 수 있다.
 */
final class ModuleRecord {

    /**
     * Status: loaded but not linked.
     * <p>
     * 상태: 불러왔지만 링크되지 않음.
     */
    private static final int UNLINKED = 0;

    /**
     * Status: linking in progress.
     * <p>
     * 상태: 링크 중.
     */
    private static final int LINKING = 1;

    /**
     * Status: linked.
     * <p>
     * 상태: 링크됨.
     */
    private static final int LINKED = 2;

    /**
     * Status: evaluating.
     * <p>
     * 상태: 평가 중.
     */
    private static final int EVALUATING = 3;

    /**
     * Status: evaluated successfully.
     * <p>
     * 상태: 평가 성공.
     */
    private static final int EVALUATED = 4;

    /**
     * Status: evaluation failed.
     * <p>
     * 상태: 평가 실패.
     */
    private static final int FAILED = 5;

    /**
     * The result of an export resolution: the module and binding name, or ambiguity.
     * <p>
     * export 해석 결과: 모듈과 바인딩 이름, 또는 모호함.
     */
    private static final class Resolved {
        /**
         * The module that holds the binding.
         * <p>
         * 바인딩을 가진 모듈.
         */
        final ModuleRecord module;
        /**
         * The binding name in that module.
         * <p>
         * 그 모듈에서의 바인딩 이름.
         */
        final String name;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param module the module / 모듈
         * @param name   the binding name / 바인딩 이름
         */
        Resolved(ModuleRecord module, String name) {
            this.module = module;
            this.name = name;
        }
    }

    /**
     * Marker for ambiguous star exports.
     * <p>
     * 모호한 별표 export 의 표시.
     */
    private static final Resolved AMBIGUOUS = new Resolved(null, null);

    /**
     * The interpreter.
     * <p>
     * 인터프리터.
     */
    private final Interpreter in;

    /**
     * The unique key of the module.
     * <p>
     * 모듈의 고유 키.
     */
    final String key;

    /**
     * The PROGRAM node.
     * <p>
     * PROGRAM 노드.
     */
    private final Node program;

    /**
     * The module information (import and export tables).
     * <p>
     * 모듈 정보(import/export 표).
     */
    private final ProgramInfo info;

    /**
     * The requested modules in the order of {@link ProgramInfo#requestedModules}.
     * <p>
     * {@link ProgramInfo#requestedModules} 순서의 요청 모듈.
     */
    private final List<ModuleRecord> requested = new ArrayList<ModuleRecord>();

    /**
     * The module environment.
     * <p>
     * 모듈 환경.
     */
    Environment env;

    /**
     * The lifecycle status.
     * <p>
     * 생명주기 상태.
     */
    private int status = UNLINKED;

    /**
     * The failure of a failed evaluation (rethrown on later imports).
     * <p>
     * 실패한 평가의 오류(이후 import 에서 다시 던진다).
     */
    private JsException failure;

    /**
     * The namespace object (created on demand).
     * <p>
     * 네임스페이스 객체(필요할 때 만든다).
     */
    private ModuleNamespace namespace;

    /**
     * Creates a module record from a parsed module.
     * <p>
     * 파싱된 모듈로 모듈 레코드를 만든다.
     *
     * @param in      the interpreter / 인터프리터
     * @param key     the module key / 모듈 키
     * @param program the PROGRAM node / PROGRAM 노드
     */
    private ModuleRecord(Interpreter in, String key, Node program) {
        this.in = in;
        this.key = key;
        this.program = program;
        this.info = (ProgramInfo) program.value;
    }

    // ------------------------------------------------------------------ loading

    /**
     * Loads a module and, recursively, everything it imports: resolves keys through the host loader, fetches and parses
     * sources, and registers the records before loading dependencies so that cycles terminate.
     * <p>
     * 모듈과 그것이 import 하는 모든 모듈을 재귀적으로 불러온다: 호스트 로더로 키를 해석하고 소스를 가져와 파싱하며, 순환이 끝나도록
     * 의존 모듈을 불러오기 전에 레코드를 등록한다.
     *
     * @param in     the interpreter / 인터프리터
     * @param key    the module key / 모듈 키
     * @param source the source text or null to fetch it through the loader / 소스 텍스트. null 이면 로더로 가져온다
     * @return the module record / 모듈 레코드
     */
    static ModuleRecord load(Interpreter in, String key, String source) {
        ModuleRecord existing = in.modules.get(key);
        if (existing != null) {
            return existing;
        }
        ModuleLoader loader = in.realm.getModuleLoader();
        String text = source;
        if (text == null) {
            if (loader == null) {
                throw JsException.typeError("Cannot load module '" + key + "': no module loader is configured");
            }
            text = loader.fetch(key);
        }
        Node prog = Parser.parseModule(text, key);
        ModuleRecord m = new ModuleRecord(in, key, prog);
        in.modules.put(key, m);
        try {
            // 요청된 모듈을 순서대로 해석해 불러온다.
            for (int i = 0; i < m.info.requestedModules.size(); i++) {
                String spec = m.info.requestedModules.get(i);
                if (loader == null) {
                    throw JsException.typeError("Cannot resolve module '" + spec + "': no module loader is configured");
                }
                String depKey = loader.resolve(spec, key);
                m.requested.add(load(in, depKey, null));
            }
        } catch (RuntimeException e) {
            in.modules.remove(key);
            throw e;
        }
        return m;
    }

    /**
     * Finds the requested module for a specifier.
     * <p>
     * 지정자에 해당하는 요청 모듈을 찾는다.
     *
     * @param specifier the module specifier / 모듈 지정자
     * @return the module / 모듈
     */
    private ModuleRecord requestedModule(String specifier) {
        return requested.get(info.requestedModules.indexOf(specifier));
    }

    // ------------------------------------------------------------------ linking

    /**
     * Links the module and its dependencies (Instantiate): creates environments with all local declarations first, then
     * connects the imports to the exporting bindings.
     * <p>
     * 모듈과 의존 모듈을 링크한다(Instantiate): 먼저 모든 지역 선언이 있는 환경을 만들고, 그다음 import 를 export 하는 바인딩에 연결한다.
     */
    void link() {
        if (status != UNLINKED) {
            return;
        }
        status = LINKING;
        try {
            createEnvironment();
            // 요청한 모듈을 순서대로 링크한다.
            for (int i = 0; i < requested.size(); i++) {
                requested.get(i).link();
            }
            connectImports();
            checkIndirectExports();
            status = LINKED;
        } catch (RuntimeException e) {
            status = UNLINKED;
            env = null;
            throw e;
        }
    }

    /**
     * Creates the module environment with its var, function and lexical declarations (ModuleDeclarationInstantiation).
     * <p>
     * var, 함수, 렉시컬 선언이 있는 모듈 환경을 만든다(ModuleDeclarationInstantiation).
     */
    private void createEnvironment() {
        env = new Environment(in.globalEnv);
        env.hasThis = true;
        env.thisValue = Js.UNDEFINED;
        env.thisBound = true;
        env.module = this;
        ScopeInfo scope = program.scope;
        // 모듈 var 이름을 undefined 로 선언한다.
        for (int i = 0; i < scope.varNames.length; i++) {
            env.declare(scope.varNames[i], true, true, Js.UNDEFINED);
        }
        in.declareLexicals(scope, env);
        in.instantiateBlockFunctions(scope, env);
    }

    /**
     * Connects every import entry to the binding of the exporting module.
     * <p>
     * 모든 import 항목을 export 하는 모듈의 바인딩에 연결한다.
     */
    private void connectImports() {
        // 가져온 이름마다 export 를 찾아 바인딩을 연결한다. 네임스페이스 가져오기는 네임스페이스 객체를 넣는다.
        for (int i = 0; i < info.imports.size(); i++) {
            ProgramInfo.ImportEntry ie = info.imports.get(i);
            ModuleRecord target = requestedModule(ie.moduleRequest);
            Binding local = env.own(ie.localName);
            if ("*".equals(ie.importName)) {
                local.value = target.getNamespace();
                local.initialized = true;
                local.mutable = false;
                continue;
            }
            Resolved r = target.resolveExport(ie.importName, new ArrayList<Object[]>());
            if (r == null || r == AMBIGUOUS) {
                throw JsException.syntaxError("The requested module '" + ie.moduleRequest + "' "
                        + (r == null ? "does not provide an export named '" : "has an ambiguous export named '") + ie.importName + "'");
            }
            Binding exported = r.module.env.own(r.name);
            if (exported == null) {
                throw JsException.syntaxError("The requested module '" + ie.moduleRequest + "' does not provide an export named '" + ie.importName + "'");
            }
            local.link = exported;
            local.mutable = false;
        }
    }

    /**
     * Verifies that every re-export can be resolved.
     * <p>
     * 모든 다시 내보내기가 해석되는지 확인한다.
     */
    private void checkIndirectExports() {
        // 간접 export 가 해석되는지 확인한다.
        for (int i = 0; i < info.indirectExports.size(); i++) {
            ProgramInfo.ExportEntry e = info.indirectExports.get(i);
            Resolved r = resolveExport(e.exportName, new ArrayList<Object[]>());
            if (r == null || r == AMBIGUOUS) {
                throw JsException.syntaxError("The requested module '" + e.moduleRequest + "' does not provide an export named '" + e.importName + "'");
            }
        }
    }

    /**
     * ResolveExport (15.2.1.16.3): finds the module and binding behind an exported name.
     * <p>
     * ResolveExport(15.2.1.16.3): export 된 이름 뒤의 모듈과 바인딩을 찾는다.
     *
     * @param name       the exported name / export 이름
     * @param resolveSet the (module, name) pairs being resolved, to stop cycles / 순환을 막기 위한 해석 중인 (모듈, 이름) 쌍
     * @return the resolution, {@link #AMBIGUOUS} or null if not found / 해석 결과. 모호하면 {@link #AMBIGUOUS}, 없으면 null
     */
    private Resolved resolveExport(String name, List<Object[]> resolveSet) {
        // 이미 해석 중인 쌍이면 순환이므로 없는 것으로 본다.
        for (int i = 0; i < resolveSet.size(); i++) {
            Object[] p = resolveSet.get(i);
            if (p[0] == this && name.equals(p[1])) {
                return null;
            }
        }
        resolveSet.add(new Object[] { this, name });
        // 지역 export 에서 이름을 찾는다.
        for (int i = 0; i < info.localExports.size(); i++) {
            ProgramInfo.ExportEntry e = info.localExports.get(i);
            if (name.equals(e.exportName)) {
                ProgramInfo.ImportEntry imp = importOf(e.localName);
                if (imp != null && !"*".equals(imp.importName)) {
                    return requestedModule(imp.moduleRequest).resolveExport(imp.importName, resolveSet);
                }
                return new Resolved(this, e.localName);
            }
        }
        // 간접 export 에서 이름을 찾는다.
        for (int i = 0; i < info.indirectExports.size(); i++) {
            ProgramInfo.ExportEntry e = info.indirectExports.get(i);
            if (name.equals(e.exportName)) {
                return requestedModule(e.moduleRequest).resolveExport(e.importName, resolveSet);
            }
        }
        if (name.equals("default")) {
            return null;
        }
        Resolved star = null;
        // export * from 로 가져온 모듈들에서 이름을 찾는다. 서로 다른 결과가 둘이면 모호하다.
        for (int i = 0; i < info.starExports.size(); i++) {
            ProgramInfo.ExportEntry e = info.starExports.get(i);
            Resolved r = requestedModule(e.moduleRequest).resolveExport(name, resolveSet);
            if (r == AMBIGUOUS) {
                return AMBIGUOUS;
            }
            if (r != null) {
                if (star == null) {
                    star = r;
                } else if (star.module != r.module || !star.name.equals(r.name)) {
                    return AMBIGUOUS;
                }
            }
        }
        return star;
    }

    /**
     * Finds the import entry that declares a local name.
     * <p>
     * 로컬 이름을 선언하는 import 항목을 찾는다.
     *
     * @param localName the local name / 로컬 이름
     * @return the entry or null / 항목 또는 null
     */
    private ProgramInfo.ImportEntry importOf(String localName) {
        // import 항목에서 지역 이름을 찾는다.
        for (int i = 0; i < info.imports.size(); i++) {
            if (info.imports.get(i).localName.equals(localName)) {
                return info.imports.get(i);
            }
        }
        return null;
    }

    /**
     * GetExportedNames (15.2.1.16.2): all names this module exports, including those of star exports.
     * <p>
     * GetExportedNames(15.2.1.16.2): 별표 export 를 포함해 이 모듈이 내보내는 모든 이름.
     *
     * @param visited the modules already visited (cycles) / 이미 방문한 모듈(순환)
     * @return the names / 이름들
     */
    private List<String> exportedNames(Set<ModuleRecord> visited) {
        List<String> names = new ArrayList<String>();
        if (!visited.add(this)) {
            return names;
        }
        // 지역 export 이름을 모은다.
        for (int i = 0; i < info.localExports.size(); i++) {
            names.add(info.localExports.get(i).exportName);
        }
        // 간접 export 이름을 모은다.
        for (int i = 0; i < info.indirectExports.size(); i++) {
            names.add(info.indirectExports.get(i).exportName);
        }
        // 별표 export 의 이름을 추가한다. default 는 별표로 전달되지 않는다.
        for (int i = 0; i < info.starExports.size(); i++) {
            List<String> star = requestedModule(info.starExports.get(i).moduleRequest).exportedNames(visited);
            // star export 로 가져온 이름을 모은다.
            for (int k = 0; k < star.size(); k++) {
                String n = star.get(k);
                if (!n.equals("default") && !names.contains(n)) {
                    names.add(n);
                }
            }
        }
        return names;
    }

    /**
     * Returns the namespace object (GetModuleNamespace), creating it on first use.
     * <p>
     * 네임스페이스 객체를 반환한다(GetModuleNamespace). 처음 쓸 때 만든다.
     *
     * @return the namespace object / 네임스페이스 객체
     */
    ModuleNamespace getNamespace() {
        if (namespace != null) {
            return namespace;
        }
        namespace = new ModuleNamespace();
        List<String> names = exportedNames(new HashSet<ModuleRecord>());
        // 이름마다 해석해 모호하지 않은 export 만 네임스페이스에 넣는다.
        for (int i = 0; i < names.size(); i++) {
            Resolved r = resolveExport(names.get(i), new ArrayList<Object[]>());
            if (r != null && r != AMBIGUOUS && r.module.env != null) {
                Binding b = r.module.env.own(r.name);
                if (b != null) {
                    namespace.addExport(names.get(i), b);
                }
            }
        }
        return namespace;
    }

    // ------------------------------------------------------------------ evaluation

    /**
     * Evaluates the module and its dependencies depth first (Evaluate). A failure is remembered and rethrown for every
     * later import of the module.
     * <p>
     * 모듈과 의존 모듈을 깊이 우선으로 평가한다(Evaluate). 실패는 기억해 두었다가 이후 이 모듈의 모든 import 에서 다시 던진다.
     */
    void evaluate() {
        if (status == EVALUATED || status == EVALUATING) {
            return;
        }
        if (status == FAILED) {
            throw failure;
        }
        status = EVALUATING;
        try {
            // 요청한 모듈을 순서대로 평가한다.
            for (int i = 0; i < requested.size(); i++) {
                requested.get(i).evaluate();
            }
            Ctx ctx = new Ctx(true, env, null);
            ctx.sourceName = key;
            ctx.funcEnv = env;
            in.st.execList(program.list, env, ctx);
            status = EVALUATED;
        } catch (JsException e) {
            status = FAILED;
            failure = e;
            throw e;
        } catch (RuntimeException e) {
            status = FAILED;
            failure = new JsException(Js.UNDEFINED);
            throw e;
        }
    }

    /**
     * Returns the value of an export for tests and host code.
     * <p>
     * 시험과 호스트 코드를 위해 export 의 값을 반환한다.
     *
     * @param name the export name / export 이름
     * @return the value or undefined / 값. 없으면 undefined
     */
    JsValue exportValue(String name) {
        return getNamespace().valueOf(name);
    }

    /**
     * Returns the namespace as a plain object reference.
     * <p>
     * 네임스페이스를 일반 객체 참조로 반환한다.
     *
     * @return the namespace object / 네임스페이스 객체
     */
    JsObject namespaceObject() {
        return getNamespace();
    }
}
