package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests dynamic import, import.meta and asynchronous module evaluation (P28-03-e).
 * <p>
 * 동적 import, import.meta, 비동기 모듈 평가 시험(P28-03-e).
 */
public class ModernDynamicImportTest {

    /**
     * Simple in-memory module loader.
     * <p>
     * 메모리 안 모듈 로더.
     */
    private static final class Loader implements ModuleLoader {
        /** Sources by key. / 키별 소스. */
        final Map<String, String> sources = new HashMap<String, String>();
        /** Fetch count by key. / 키별 가져오기 횟수. */
        final Map<String, Integer> fetches = new HashMap<String, Integer>();

        /**
         * Resolves relative module keys.
         * <p>
         * 상대 모듈 키를 해석한다.
         */
        public String resolve(String specifier, String referrer) {
            if (!specifier.startsWith(".")) {
                return specifier;
            }
            int slash = referrer.lastIndexOf('/');
            String base = slash < 0 ? "" : referrer.substring(0, slash + 1);
            String path = base + specifier;
            java.util.List<String> parts = new java.util.ArrayList<String>();
            String[] split = path.split("/");
            // 상대 경로 요소를 정규화하면서 ".." 는 직전 요소를 제거한다.
            for (int i = 0; i < split.length; i++) {
                if (split[i].equals(".") || split[i].length() == 0) {
                    continue;
                }
                if (split[i].equals("..")) {
                    if (!parts.isEmpty()) {
                        parts.remove(parts.size() - 1);
                    }
                } else {
                    parts.add(split[i]);
                }
            }
            StringBuilder out = new StringBuilder();
            // 정규화한 경로 요소를 슬래시로 다시 이어 붙인다.
            for (int i = 0; i < parts.size(); i++) {
                if (out.length() > 0) {
                    out.append('/');
                }
                out.append(parts.get(i));
            }
            return out.toString();
        }

        /**
         * Fetches one source and counts the fetch.
         * <p>
         * 소스 하나를 가져와 횟수를 센다.
         */
        public String fetch(String key) {
            Integer old = fetches.get(key);
            fetches.put(key, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
            String source = sources.get(key);
            if (source == null) {
                throw JsException.typeError("Cannot find module '" + key + "'");
            }
            return source;
        }
    }

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModernDynamicImportTest() {
    }

    /**
     * Prepares a realm.
     * <p>
     * realm 을 준비한다.
     */
    private Object[] realm(String key, String source) {
        Loader loader = new Loader();
        loader.sources.put(key, source);
        JsRealm realm = Scripts.realm(new Scripts.Capture());
        realm.setModuleLoader(loader);
        return new Object[] { realm, loader };
    }

    /**
     * Checks dynamic import, import.meta and one fetch per duplicate import.
     * <p>
     * 동적 import, import.meta, 중복 import 의 한 번 가져오기를 검사한다.
     */
    public void testDynamicImportAndMeta() {
        Object[] setup = realm("root/dep.js", "export var value=7; export var url=import.meta.url;");
        JsRealm realm = (JsRealm) setup[0];
        Loader loader = (Loader) setup[1];
        realm.evaluateScript("globalThis.result = ''; Promise.all([import('./dep.js'), import('./dep.js')])"
                + ".then(function(a){ result = a[0].value + ':' + a[1].url; });", "root/main.js");
        realm.runJobs();
        Check.equal("7:root/dep.js", Ops.toString(realm.getGlobalObject().get("result")), "dynamic import result");
        Check.equal(Integer.valueOf(1), loader.fetches.get("root/dep.js"), "module is fetched once");
    }

    /**
     * Checks specifier coercion and loader failure as Promise rejection.
     * <p>
     * 지정자 변환과 로더 실패가 Promise 거부인지 검사한다.
     */
    public void testDynamicImportRejection() {
        Object[] setup = realm("root/ok.js", "export default 1;");
        JsRealm realm = (JsRealm) setup[0];
        realm.evaluateScript("globalThis.coerce=''; globalThis.missing='';"
                + "import({toString:function(){ return './ok.js'; }}).then(function(m){ coerce=m.default; });"
                + "import('./missing.js').then(function(){},function(e){ missing=e.name; });", "root/main.js");
        realm.runJobs();
        Check.equal("1", Ops.toString(realm.getGlobalObject().get("coerce")), "specifier coercion");
        Check.equal("TypeError", Ops.toString(realm.getGlobalObject().get("missing")), "missing module rejection");
    }

    /**
     * Checks top-level await and dependency ordering.
     * <p>
     * top-level await 와 의존성 순서를 검사한다.
     */
    public void testTopLevelAwait() {
        Loader loader = new Loader();
        loader.sources.put("m/dep.js", "await Promise.resolve(); export const value=4;");
        loader.sources.put("m/main.js", "import { value } from './dep.js'; await 0; export const answer=value+1;");
        JsRealm realm = Scripts.realm(new Scripts.Capture());
        realm.setModuleLoader(loader);
        realm.evaluateScript("globalThis.answer=0; import('./main.js').then(function(m){ answer=m.answer; });", "m/entry.js");
        realm.runJobs();
        Check.equal("5", Ops.toString(realm.getGlobalObject().get("answer")), "module resumes after TLA");
    }

    /**
     * Checks a synchronous cycle through dynamic import remains evaluable.
     * <p>
     * 동적 import 를 통한 동기 순환도 평가 가능한지 검사한다.
     */
    public void testDynamicImportCycle() {
        Loader loader = new Loader();
        loader.sources.put("c/a.js", "import { b } from './b.js'; export const a=1; export const seen=b;");
        loader.sources.put("c/b.js", "import { a } from './a.js'; export const b=2;");
        JsRealm realm = Scripts.realm(new Scripts.Capture());
        realm.setModuleLoader(loader);
        realm.evaluateScript("globalThis.cycle=''; import('./a.js').then(function(m){ cycle=m.a+':'+m.seen; },"
                + "function(e){ cycle='ERR'; });", "c/main.js");
        realm.runJobs();
        Check.equal("1:2", Ops.toString(realm.getGlobalObject().get("cycle")), "cycle evaluation");
    }
}

