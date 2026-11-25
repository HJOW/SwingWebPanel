package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks weak collections and references with eligible symbols and garbage collection.
 * <p>
 * 약한 참조에 사용할 수 있는 심볼과 가비지 수거를 포함해 약한 컬렉션과 참조를 검증한다.
 */
public class Es2026WeakSymbolTest {

    /**
     * Non-registered and well-known symbols are valid keys, targets and unregister tokens.
     * <p>
     * 비등록 심볼과 well-known 심볼은 키, 대상 및 등록 해제 토큰으로 사용할 수 있다.
     */
    public void testWeakSymbolIdentityAndTokens() {
        String script = "const key=Symbol('same'), other=Symbol('same'), known=Symbol.iterator;"
                + "const m=new WeakMap([[key,undefined],[known,7]]), s=new WeakSet([key,known]);"
                + "const r=new FinalizationRegistry(()=>{}); r.register(key,1,other); r.register(known,2,known);"
                + "const w=new WeakRef(key), wk=new WeakRef(known);"
                + "[m.get(key)===undefined,m.has(key),m.get(other)===undefined,m.has(other),m.get(known),"
                + "s.has(other),s.has(known),s.delete(key),s.has(key),m.delete(key),m.has(key),"
                + "w.deref()===key,wk.deref()===known,r.unregister(key),r.unregister(other),r.unregister(other),"
                + "r.unregister(known)].join('|')";
        Check.equal("true|true|true|false|7|false|true|true|false|true|false|true|true|false|true|false|true",
                evaluate(JsRealm.create(), script), "weak symbol keys compare by identity");
    }

    /**
     * Registered symbols fail insertions and weak references, with empty results for lookup and deletion.
     * <p>
     * 등록 심볼은 삽입과 약한 참조에서 오류를 내고 조회와 삭제에서는 항목이 없다고 반환한다.
     */
    public void testRegisteredSymbolsAndPrimitiveTargets() {
        String script = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const key=Symbol.for('weak-key-test'), unregistered=Symbol('weak-key-test');"
                + "const m=new WeakMap(), s=new WeakSet(), r=new FinalizationRegistry(()=>{});"
                + "[m.get(key)===undefined,m.has(key),m.delete(key),s.has(key),s.delete(key),"
                + "fail(()=>m.set(key,1)),fail(()=>s.add(key)),fail(()=>new WeakMap([[key,2]])),"
                + "fail(()=>new WeakSet([key])),fail(()=>new WeakRef(key)),fail(()=>new WeakRef(12)),"
                + "fail(()=>r.register(key,1)),fail(()=>r.register(unregistered,1,key)),"
                + "fail(()=>r.unregister(key)),fail(()=>r.register(unregistered,unregistered))].join('|')";
        Check.equal("true|false|false|false|false|TypeError|TypeError|TypeError|TypeError|TypeError|TypeError|"
                + "TypeError|TypeError|TypeError|TypeError", evaluate(JsRealm.create(), script),
                "registered symbols and other primitives cannot be installed as weak targets");
    }

    /**
     * The current task keeps a newly referenced symbol alive, but an association that points back to its key permits later collection.
     * <p>
     * 현재 작업은 새 약한 참조의 심볼을 유지하며, 값이 키를 가리키는 WeakMap 항목도 이후에는 수거할 수 있다.
     *
     * @throws Exception if the test thread is interrupted / 시험 스레드가 인터럽트되면
     */
    public void testWeakSymbolCollectedWithBackReference() throws Exception {
        JsRealm r = JsRealm.create();
        r.evaluateScript("var held=Symbol('collectible'); var map=new WeakMap(), set=new WeakSet(), kept=[],"
                + "registry=new FinalizationRegistry(v=>kept.push(v));"
                + "var ref=new WeakRef(held);map.set(held,{back:held});set.add(held);registry.register(held,'released',held);",
                "weak-symbol-lifetime.js");
        WeakReference<JsValue> observer = new WeakReference<JsValue>(r.getGlobalObject().get("held"));
        r.evaluateScript("held=undefined;", "weak-symbol-drop.js");
        System.gc();
        Check.notNull(observer.get(), "the constructor kept the symbol alive through the current job");
        r.runJobs();
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        // GC가 참조를 지우면 realm의 체크포인트에서 정리 콜백을 실행한다.
        while (!"released".equals(evaluate(r, "kept.join()")) && System.nanoTime() - end < 0) {
            System.gc();
            Thread.sleep(10);
            r.runJobs();
        }
        Check.isNull(observer.get(), "the weak map's back-reference did not pin the symbol");
        Check.equal("true|released", evaluate(r, "[ref.deref()===undefined,kept.join()].join('|')"),
                "a cleared symbol reference and a single finalization callback");
    }

    /**
     * A permanently rooted symbol must not keep a WeakMap alive through one of its entry values.
     * <p>
     * 엔진이 유지하는 well-known 심볼이 항목 값을 통해 WeakMap을 계속 유지하지 않는지 확인한다.
     *
     * @throws Exception if the test thread is interrupted / 시험 스레드가 인터럽트되면
     */
    public void testWellKnownSymbolDoesNotRetainWeakMap() throws Exception {
        WeakReference<JsValue> map = mapWithWellKnownKey();
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        // 값에서 맵을 가리켜도 엔진 전역 심볼을 통해 맵이 계속 살아 있지 않아야 한다.
        while (map.get() != null && System.nanoTime() - end < 0) {
            System.gc();
            Thread.sleep(10);
        }
        Check.isNull(map.get(), "a permanent symbol did not retain a discarded map");
    }

    /**
     * Returns a weak observation of a map whose well-known entry value refers to the map.
     * <p>
     * well-known 심볼 항목의 값이 맵을 참조하는 경우 그 맵의 약한 참조를 반환한다.
     *
     * @return the observation reference / 관찰용 참조
     */
    private static WeakReference<JsValue> mapWithWellKnownKey() {
        JsRealm realm = JsRealm.create();
        return new WeakReference<JsValue>(realm.evaluateScript("var m=new WeakMap();m.set(Symbol.iterator,{map:m});m", "weak-symbol-map.js"));
    }

    /**
     * Converts a script result to the form used by assertions.
     * <p>
     * 스크립트 결과를 검증에 사용하는 형태로 변환한다.
     *
     * @param realm the realm / realm
     * @param script test script / 시험 스크립트
     * @return the result as text / 문자열 결과
     */
    private static String evaluate(JsRealm realm, String script) {
        return Ops.toString(realm.evaluateScript(script, "weak-symbol.js"));
    }
}
