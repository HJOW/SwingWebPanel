package org.duckdns.hjow.swingwebpanel.js;

/**
 * Tracks the running realm on the current Java thread while JavaScript or a built-in is executing.
 * <p>
 * JavaScript 또는 내장 함수가 실행되는 동안 현재 Java 스레드의 실행 realm을 추적한다.
 */
public final class RealmExecution {

    /** Innermost executing realm for each thread. / 스레드마다 가장 안쪽에서 실행하는 realm. */
    private static final ThreadLocal<JsRealm> ACTIVE = new ThreadLocal<JsRealm>();

    /** Innermost executing ECMAScript code's realm. / 가장 안쪽에서 ECMAScript 코드를 실행하는 realm. */
    private static final ThreadLocal<JsRealm> SCRIPT = new ThreadLocal<JsRealm>();

    /** Innermost non-null script or module record. / 가장 안쪽의 null이 아닌 스크립트 또는 모듈 레코드. */
    private static final ThreadLocal<ScriptReference> SCRIPT_SOURCE = new ThreadLocal<ScriptReference>();

    /** Backup incumbent realm supplied by a browser job callback. / 브라우저 작업 콜백이 제공하는 예비 incumbent realm. */
    private static final ThreadLocal<JsRealm> INCUMBENT = new ThreadLocal<JsRealm>();

    /** Static execution scope. / 정적 실행 범위. */
    private RealmExecution() {
    }

    /**
     * Returns the executing realm, or null outside an execution scope.
     * <p>
     * 실행 중인 realm을 반환한다. 실행 범위 밖이면 null이다.
     *
     * @return current realm / 현재 realm
     */
    public static JsRealm current() {
        return ACTIVE.get();
    }

    /**
     * Returns the current script realm through calls to built-in functions.
     * <p>
     * 내장 함수 실행 중에도 현재 스크립트 realm을 반환한다.
     *
     * @return current script realm, or null / 현재 스크립트 realm 또는 null
     */
    public static JsRealm currentScript() {
        return SCRIPT.get();
    }

    /**
     * Returns the source key used as the referrer for host module loading.
     * <p>
     * 호스트 모듈을 불러올 때 참조 기준으로 쓰는 원문 키를 반환한다.
     *
     * @return the active key or null if there is no active script record / 활성 키 또는 활성 스크립트 레코드가 없으면 null
     */
    public static String currentScriptSource() {
        ScriptReference reference = currentScriptReference();
        return reference == null ? null : reference.getKey();
    }

    /**
     * Returns the first script or module record found inward to outward on the execution stack.
     * <p>
     * 실행 스택 안쪽부터 바깥쪽으로 찾아 처음 발견한 스크립트 또는 모듈 레코드를 반환한다.
     *
     * @return the active record or null / 활성 레코드 또는 null
     */
    public static ScriptReference currentScriptReference() {
        return SCRIPT_SOURCE.get();
    }

    /**
     * Identifies the script or module whose realm and source key govern host module loads.
     * <p>
     * 호스트 모듈 가져오기에 쓰는 렐름과 원문 키를 가진 스크립트 또는 모듈을 식별한다.
     */
    public static final class ScriptReference {
        /** The realm that created the record. / 레코드를 만든 렐름. */
        private final JsRealm realm;

        /** The module loader's referrer key. / 모듈 로더의 참조 키. */
        private final String key;

        /**
         * Records the original owner and referrer key.
         * <p>
         * 원래 소유 렐름과 참조 키를 기록한다.
         *
         * @param realm the owner / 소유 렐름
         * @param key the referrer key / 참조 키
         */
        public ScriptReference(JsRealm realm, String key) {
            this.realm = realm;
            this.key = key;
        }

        /**
         * Returns the owner that supplies the loader and module map.
         * <p>
         * 로더와 모듈 맵을 제공하는 소유 렐름을 반환한다.
         *
         * @return the owner / 소유 렐름
         */
        public JsRealm getRealm() {
            return realm;
        }

        /**
         * Returns the key used to resolve relative imports.
         * <p>
         * 상대 import를 해석할 때 사용하는 키를 반환한다.
         *
         * @return the referrer key / 참조 키
         */
        public String getKey() {
            return key;
        }
    }

    /**
     * Returns the active script realm, falling back to the host's saved incumbent during native callbacks.
     * <p>
     * 활성 스크립트 realm을 반환한다. 네이티브 콜백에서 활성 스크립트가 없으면 호스트가 보관한 incumbent를 반환한다.
     *
     * @return the incumbent realm, or null / incumbent realm 또는 null
     */
    public static JsRealm currentIncumbent() {
        JsRealm script = SCRIPT.get();
        return script == null ? INCUMBENT.get() : script;
    }

    /**
     * Enters the host state saved when a Promise job callback was registered.
     * <p>
     * Promise 작업 콜백 등록 시 저장한 호스트 상태에 들어간다.
     *
     * @param incumbent the saved incumbent realm / 저장한 incumbent realm
     * @param source the saved active record, or null / 저장한 활성 레코드 또는 null
     * @return prior state for restoration / 복원할 이전 상태
     */
    public static CallbackScope enterJobCallback(JsRealm incumbent, ScriptReference source) {
        CallbackScope previous = new CallbackScope(INCUMBENT.get(), SCRIPT_SOURCE.get());
        INCUMBENT.set(incumbent);
        // 실제 스크립트가 없으면 호출자를 바꾸지 않고 예비 realm만 제공한다.
        if (source != null) {
            SCRIPT_SOURCE.set(source);
        }
        return previous;
    }

    /**
     * Restores the host state from before a Promise job callback.
     * <p>
     * Promise 작업 콜백을 실행하기 전 호스트 상태를 복원한다.
     *
     * @param previous the saved prior state / 저장한 이전 상태
     */
    public static void restoreJobCallback(CallbackScope previous) {
        if (previous.incumbent == null) {
            INCUMBENT.remove();
        } else {
            INCUMBENT.set(previous.incumbent);
        }
        setScriptSource(previous.source);
    }

    /**
     * Replaces or clears the active record.
     * <p>
     * 활성 레코드를 교체하거나 지운다.
     *
     * @param source the new record or null / 새 레코드 또는 null
     */
    private static void setScriptSource(ScriptReference source) {
        if (source == null) {
            SCRIPT_SOURCE.remove();
        } else {
            SCRIPT_SOURCE.set(source);
        }
    }

    /**
     * The host state interrupted by a job callback.
     * <p>
     * 작업 콜백이 실행되기 전의 호스트 상태.
     */
    public static final class CallbackScope {

        /** Previous incumbent realm. / 이전 incumbent realm. */
        private final JsRealm incumbent;

        /** Previous active record. / 이전 활성 레코드. */
        private final ScriptReference source;

        /**
         * Saves the state for restoration.
         * <p>
         * 나중에 복원할 상태를 저장한다.
         *
         * @param incumbent prior incumbent realm / 이전 incumbent realm
         * @param source prior active record / 이전 활성 레코드
         */
        private CallbackScope(JsRealm incumbent, ScriptReference source) {
            this.incumbent = incumbent;
            this.source = source;
        }
    }

    /**
     * Enters the supplied realm. The implementation must restore the returned value in a finally block.
     * <p>
     * 지정한 realm에서 실행을 시작한다. 반환값은 finally 블록에서 복원해야 한다.
     *
     * @param realm entering realm / 시작할 realm
     * @return previous realm, or null / 이전 realm 또는 null
     */
    public static JsRealm enter(JsRealm realm) {
        JsRealm previous = ACTIVE.get();
        if (previous != realm) {
            ACTIVE.set(realm);
        }
        return previous;
    }

    /**
     * Restores the caller's execution scope without retaining ended scopes in thread-local storage.
     * <p>
     * 호출자의 실행 범위를 복원하고 종료한 범위를 스레드 로컬에 보관하지 않는다.
     *
     * @param previous previously returned realm, or null / 시작할 때 반환한 이전 realm 또는 null
     */
    public static void restore(JsRealm previous) {
        if (previous == null) {
            ACTIVE.remove();
        } else if (ACTIVE.get() != previous) {
            ACTIVE.set(previous);
        }
    }

    /**
     * Enters ECMAScript code and tracks its realm separately from any built-in it calls.
     * <p>
     * ECMAScript 코드에 들어가며 코드의 realm을 그 코드가 호출한 내장 함수와 별도로 추적한다.
     *
     * @param realm entering realm / 실행할 realm
     * @return previous execution scope, or null if unchanged / 이전 실행 범위. 바뀌지 않으면 null
     */
    public static ScriptScope enterScript(JsRealm realm) {
        return enterScript(realm, null);
    }

    /**
     * Enters ECMAScript code with its script or module reference key.
     * <p>
     * 스크립트나 모듈의 참조 키를 사용해 ECMAScript 코드로 들어간다.
     *
     * @param realm entering realm / 실행할 realm
     * @param source active script or module key, or null / 활성 스크립트나 모듈 키 또는 null
     * @return previous execution scope, or null if unchanged / 이전 실행 범위. 바뀌지 않으면 null
     */
    public static ScriptScope enterScript(JsRealm realm, String source) {
        return enterScriptRecord(realm, source == null ? null : new ScriptReference(realm, source));
    }

    /**
     * Enters code using its own record or the nearest outer record when this context has none.
     * <p>
     * 코드에 진입하고 자체 레코드를 사용한다. 없으면 가장 가까운 바깥의 레코드를 사용한다.
     *
     * @param realm entering realm / 실행할 렐름
     * @param source the code's record, or null / 코드의 레코드 또는 null
     * @return previous state for restoration / 복원할 이전 상태
     */
    public static ScriptScope enterScriptRecord(JsRealm realm, ScriptReference source) {
        return replaceScriptRecord(realm, source == null ? SCRIPT_SOURCE.get() : source);
    }

    /**
     * Enters a host compilation scope with no outer script record.
     * Use this to construct an event handler whose ScriptOrModule slot must be null.
     * <p>
     * 바깥 스크립트 레코드 없이 호스트 컴파일 범위에 진입한다.
     * ScriptOrModule 슬롯이 null이어야 하는 이벤트 처리기를 만들 때 사용한다.
     *
     * @param realm entering realm / 실행할 렐름
     * @return previous state for restoration / 복원할 이전 상태
     */
    public static ScriptScope enterRecordlessCompilation(JsRealm realm) {
        return replaceScriptRecord(realm, null);
    }

    /**
     * Installs the effective execution state and returns the fields that were replaced.
     * <p>
     * 적용할 실행 상태를 설정하고 교체된 필드를 반환한다.
     *
     * @param realm entering realm / 실행할 렐름
     * @param source the effective record / 적용할 레코드
     * @return previous state for restoration / 복원할 이전 상태
     */
    private static ScriptScope replaceScriptRecord(JsRealm realm, ScriptReference source) {
        JsRealm previousScript = SCRIPT.get();
        JsRealm previous = enter(realm);
        ScriptReference previousSource = SCRIPT_SOURCE.get();
        if (previous == realm && previousScript == realm && previousSource == source) {
            return null;
        }
        if (previousScript != realm) {
            SCRIPT.set(realm);
        }
        if (source != previousSource) {
            setScriptSource(source);
        }
        return new ScriptScope(previous, previousScript, previousSource);
    }

    /**
     * Restores execution scopes after ECMAScript code stops.
     * <p>
     * ECMAScript 코드가 멈추면 이전 실행 범위를 복원한다.
     *
     * @param scope previous scope from enterScript / enterScript가 반환한 이전 범위
     */
    public static void restoreScript(ScriptScope scope) {
        if (scope != null) {
            restore(scope.previous);
            if (scope.previousScript == null) {
                SCRIPT.remove();
            } else if (SCRIPT.get() != scope.previousScript) {
                SCRIPT.set(scope.previousScript);
            }
            if (SCRIPT_SOURCE.get() != scope.previousSource) {
                setScriptSource(scope.previousSource);
            }
        }
    }

    /**
     * State saved when entering code in a different realm.
     * <p>
     * 다른 realm의 코드에 진입할 때 저장한 상태.
     */
    public static final class ScriptScope {
        /** Prior realm for operations. / 연산에 쓰던 realm. */
        private final JsRealm previous;

        /** Prior realm executing script source. / 스크립트 원문을 실행하던 realm. */
        private final JsRealm previousScript;

        /** Prior active record. / 이전 활성 레코드. */
        private final ScriptReference previousSource;

        /**
         * Stores the realms and source for restoration.
         * <p>
         * 나중에 복원할 두 realm과 원문 키를 저장한다.
         *
         * @param previous prior running realm / 이전 실행 realm
         * @param previousScript prior script realm / 이전 스크립트 realm
         * @param previousSource prior active record / 이전 활성 레코드
         */
        private ScriptScope(JsRealm previous, JsRealm previousScript, ScriptReference previousSource) {
            this.previous = previous;
            this.previousScript = previousScript;
            this.previousSource = previousSource;
        }
    }
}
