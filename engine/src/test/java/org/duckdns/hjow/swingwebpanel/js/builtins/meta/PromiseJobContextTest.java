package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies the default host behavior and the opt-in HTML Promise callback scopes.
 * <p>
 * 기본 호스트 동작과 선택적으로 활성화하는 HTML Promise 콜백 범위를 검증한다.
 */
public final class PromiseJobContextTest {

    /**
     * A native callback that observes the job scope and can also act as thenable.then.
     * <p>
     * 작업 범위를 확인하며 thenable.then에도 사용할 수 있는 네이티브 콜백.
     */
    private static final class Probe implements NativeFunction.Call {

        /** The expected realm for the incumbent comparison. / 비교할 incumbent realm. */
        private final JsRealm author;

        /** Recorded script key and incumbent checks. / 기록한 스크립트 키와 incumbent 검사 값. */
        final List<String> calls = new ArrayList<String>();

        /**
         * Creates a probe for a realm.
         * <p>
         * 주어진 realm을 검사할 콜백을 만든다.
         *
         * @param author expected incumbent / 예상 incumbent
         */
        Probe(JsRealm author) {
            this.author = author;
        }

        /**
         * Records the restored scope, resolves a thenable, or throws for the rejection variant.
         * <p>
         * 복원한 범위를 기록하고 thenable을 해결하거나 거부 시험에서 예외를 던진다.
         *
         * @param realm callback realm / 콜백 realm
         * @param receiver this value / this 값
         * @param args callback arguments / 콜백 인수
         * @return the first argument / 첫 인수
         */
        public JsValue call(JsRealm realm, JsValue receiver, JsValue[] args) {
            calls.add(RealmExecution.currentScriptSource() + ":" + (RealmExecution.currentIncumbent() == author));
            JsValue value = Js.arg(args, 0);
            if (value.isCallable()) {
                return Ops.call(value, Js.UNDEFINED, new JsValue[] { Js.str("thenable") });
            }
            if ("throw".equals(Ops.toString(value))) {
                throw new JsException(Js.str("callback failure"));
            }
            return value;
        }
    }

    /** Creates the suite. / 시험 묶음을 만든다. */
    public PromiseJobContextTest() {
    }

    /**
     * An independent realm uses the default ECMAScript JobCallback behavior.
     * <p>
     * 독립 realm은 기본 ECMAScript JobCallback 동작을 사용한다.
     */
    public void testDefaultJobCallbackScope() {
        JsRealm realm = JsRealm.create();
        Probe probe = new Probe(realm);
        realm.getGlobalObject().defineBuiltin("probe", realm.newFunction("probe", 1, probe));
        realm.evaluateScript("Promise.resolve('ordinary').then(probe);", "standalone/entry.js");
        realm.runJobs();
        Check.equal("[null:false]", probe.calls.toString(), "the default native job sees no saved HTML context");
    }

    /**
     * Reactions and thenables capture the registering script and clear both contexts after exceptions.
     * <p>
     * 반응과 thenable은 등록 스크립트를 보존하며 예외가 발생한 뒤 두 문맥을 복원한다.
     */
    public void testHtmlJobCallbackScopeAcrossRealms() {
        JsRealm author = JsRealm.create();
        JsRealm owner = JsRealm.create();
        author.enableHtmlJobCallbacks();
        owner.enableHtmlJobCallbacks();
        Probe probe = new Probe(author);
        owner.evaluateScript("var ok=Promise.withResolvers(), fail=Promise.withResolvers();", "owner/define.js");
        author.getGlobalObject().defineBuiltin("probe", owner.newFunction("probe", 1, probe));
        author.getGlobalObject().defineBuiltin("ForeignPromise", owner.getGlobalObject().get("Promise"));
        author.getGlobalObject().defineBuiltin("ok", owner.getGlobalObject().get("ok"));
        author.getGlobalObject().defineBuiltin("fail", owner.getGlobalObject().get("fail"));
        author.evaluateScript("ok.promise.then(probe);fail.promise.catch(probe).catch(e=>globalThis.reason=e);", "author/register.js");
        owner.evaluateScript("ok.resolve(1);fail.reject('throw');", "owner/settle.js");
        author.evaluateScript("ForeignPromise.resolve({then:probe}).then(v=>globalThis.delivery=v);", "author/resolve.js");
        owner.runJobs();
        Check.equal("[author/register.js:true, author/register.js:true, author/resolve.js:true]", probe.calls.toString(),
                "fulfillment, rejection, and thenable jobs use their registration context");
        Check.equal("callback failure", Ops.toString(author.getGlobalObject().get("reason")), "abrupt completion rejects the derived promise");
        Check.equal("thenable", Ops.toString(author.getGlobalObject().get("delivery")), "thenable result reaches the derived promise");
        Check.isNull(RealmExecution.currentScriptSource(), "source scope restored");
        Check.isNull(RealmExecution.currentIncumbent(), "incumbent scope restored");
    }
}
