# ENGINE 작업 기록 (P26 JavaScript 엔진 분리와 Java Scripting API 지원)

작업자: ENGINE (브리프 `docs/progress/briefs/engine.md`). commit 번호 대역 `20261122-200` 부터. 모든 검증은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로 했고, Nashorn 이름 충돌 확인만 JDK 8(`jdk1.8.0_202`)을 따로 썼다. Maven 은 쓰지 않았다(P23 은 맨 마지막).

## 1. P26-01 경계 확정과 분리

### 1.1 경계 확인 (작업 시작 시 다시 확인)
- 엔진 후보 패키지(`js` 루트, `js.ast`, `js.parser`, `js.interp`, `js.builtins`(하위 `corelib/datajson/meta/stringarray/typed` 포함), `js.date`, `js.num`, `js.regexp`, `js.text`)에서 `java.awt`, `javax.swing`, 브라우저 패키지(`core dom css html layout render net storage media content download`, 루트의 `SwingWebPanel`/`ConsoleEvent`/`ConsoleListener`, `js.host`, `js.dom`, `js.worker`, `testing`)를 가리키는 `import` 와 완전한 이름(fully qualified name)은 **0건**(grep). 엔진이 쓰는 JRE 패키지는 `java.util`, `java.math`, `java.text`, `java.lang.ref`, `java.lang.reflect`(내장 함수 호출용 `Method`/`Proxy` 6곳)뿐이고 `java.io`/`java.net`/파일은 쓰지 않는다(`JsRealm` 의 기본 콘솔만 `System.out/err`).
- 엔진 시험(`tests/.../js/{*.java,builtins,date,interp,num,parser,regexp,text}` 84개 파일)도 브라우저 패키지를 쓰지 않고 `testing.{Check,Block,AssertionFailure}` 만 쓴다. `TestHttpServer/TestFtpServer/TestSsl` 는 쓰지 않는다.

### 1.2 확정한 결정 (브리프의 결정 사항을 그대로 따랐고 달라진 것은 표시)
| 항목 | 결정 |
| --- | --- |
| 엔진 모듈 | 디렉터리 `engine/`, 산출물 `swingwebpanel-js.jar`(엔진만) |
| 브라우저 모듈 | 기존 `src/`, 산출물 `swingwebpanel-browser.jar`(엔진 클래스를 **포함하지 않음**, 실행에는 엔진 JAR 필요) |
| 합친 JAR | **`swingwebpanel.jar` 는 엔진+브라우저를 합친 단일 JAR 로 유지**(기존 사용자가 JAR 하나만 클래스패스에 두는 방식이 깨지지 않음, 브리프의 우선순위). 브리프 예시의 `swingwebpanel-all.jar` 이름은 만들지 않는다. P23 에서 Maven 모듈 산출물 이름(예: `swingwebpanel`=브라우저만)을 정할 때 합친 JAR 의 이름을 다시 정해야 한다 → "조정자 요청" |
| 패키지 이름 | **바꾸지 않음**. `org.duckdns.hjow.swingwebpanel.js...` 를 유지한다. 엔진은 `js` 아래 패키지 중 `host/dom/worker` 를 뺀 것이고 두 모듈의 패키지는 겹치지 않는다(분할 패키지 없음). 바꾸면 브라우저 코드 수백 개와 시험 전체가 영향을 받는다 |
| 소스 디렉터리 | 엔진 `engine/src/org/duckdns/hjow/swingwebpanel/js/...`, 엔진 시험 `engine/tests/src/...`, 시험 자료 `engine/tests/resources/...`, 엔진 리소스(`META-INF/services` 등) `engine/resources/`. P23 에서 `engine` 이 그대로 Maven 모듈이 된다(`src`→`src/main/java` 등 표준 구조로 옮기면 됨) |
| 시험 실행기 | `testing.{TestRunner,Check,Block,AssertionFailure,SkipException}` 와 `testing.selftest.{RunnerSelfTest,failing.FailingSuite}` 는 **엔진 시험 디렉터리**로 옮겼다(엔진 단독 시험에도 필요하므로). 브라우저 시험은 엔진 시험 클래스를 클래스패스에 두고(Maven 의 test-jar 의존에 해당) `TestHttpServer/TestFtpServer/TestSsl`, `JarContentTest` 만 자기 쪽에 둔다 |
| 경계 강제 | (1) 엔진 컴파일 클래스패스·소스패스를 **빈 디렉터리 하나**로 제한(브라우저 클래스나 라이브러리를 참조하면 컴파일 실패), (2) 브라우저 컴파일은 엔진 **소스패스 없이** 컴파일된 엔진 클래스만 참조, (3) `tools/sourcecheck` 의 새 규칙 `engine-boundary`(AWT/Swing/네트워크/파일/브라우저 패키지 금지), (4) 엔진 시험을 `java.awt.headless=true` + 엔진 클래스만 클래스패스에 두고 실행, (5) 엔진 클래스 상수 풀 검사 시험(P26-05/07 에서 보강) |

### 1.3 옮긴 파일
- 제품 소스 181개: `src/.../js/*.java`(42, `JsRealm`, `JsObject`, `ModuleLoader`, `ConsoleHook` 등)와 `ast parser interp builtins date num regexp text` 디렉터리 전체 → `engine/src/.../js/`. 브라우저에는 `js/host`, `js/dom`, `js/worker` 만 남았다.
- 시험 소스 84개: `tests/src/.../js/{*.java,builtins,date,interp,num,parser,regexp,text}` → `engine/tests/src/.../js/`, `testing` 의 실행기 5개와 `testing/selftest` 의 `RunnerSelfTest`, `failing/FailingSuite` → `engine/tests/src/.../testing/`.
- 시험 자료: `tests/resources/org/duckdns/hjow/swingwebpanel/js/interp/*.js` 10개 → `engine/tests/resources/...`.
- `git mv` 대신 `mv` 후 `git add -A` 로 옮겼고(이 환경이 복합 git 명령을 막음) git 이 275개를 rename 으로 인식한다.

### 1.4 바뀐 스크립트와 도구
| 파일 | 변경 |
| --- | --- |
| `engine/build.sh`, `engine/build.bat` (새) | 엔진만 컴파일해 `build/engine-classes`, `build/swingwebpanel-js.jar` 생성 |
| `engine/test.sh`, `engine/test.bat` (새) | 엔진 시험을 컴파일하고 엔진 클래스만으로 headless 실행. `SWP_ENGINE_BUILT=1` 이면 엔진 빌드를 건너뜀 |
| `build.sh`, `build.bat` | 엔진 빌드 → 브라우저를 `build/engine-classes` 에만 의존해 컴파일(`build/browser-classes`) → 합친 클래스 디렉터리 `build/classes`(시험, 예제, 도구가 그대로 사용) → `swingwebpanel-js.jar`, `swingwebpanel-browser.jar`, `swingwebpanel.jar`(합친 것) |
| `test.sh`, `test.bat` | 1단계 엔진 시험(엔진 전용), 2단계 브라우저 시험(합친 클래스). 두 단계의 요약 줄을 합산해 `=== TOTAL (engine + browser): ... ===` 를 출력. 러너 종료 코드 자체 점검 유지 |
| `scripts/pkgtest.sh` | 패키지 이름을 `src`/`tests/src` 와 `engine/src`/`engine/tests/src` 양쪽에서 찾는다(`js` 는 두 곳의 합). `-sourcepath` 에 두 소스 루트 |
| `scripts/snaptest.sh` | 변경 없음(`git archive HEAD` 후 `pkgtest.sh` 호출이라 새 경로를 그대로 쓴다) |
| `examples/run.sh`, `examples/run.bat` | 요청한 예제와 그것이 쓰는 예제 클래스만 컴파일(`-sourcepath examples/src`), 하위 패키지 예제는 점으로 지정(`script.ScriptEngineExample`). 그래서 `SWP_CLASSES=build/engine-classes` 로 엔진 예제를 엔진 클래스만으로 컴파일·실행할 수 있다. 기존 예제(`BlankPageExample`, `StaticPageExample`, `ConsoleExample` `--smoke`)는 새 스크립트로도 통과 확인 |
| `tools/uicheck` 등 | 변경 없음 — `build/classes`(합친 디렉터리)를 계속 쓴다(`tools/uicheck/build.sh` 가 새 구조에서 컴파일됨을 확인) |
| `.gitattributes` | `engine/resources/META-INF/services/*` 를 LF 로 고정 |
| `engine/test.sh|bat` 환경 변수 | `SWP_ENGINE_FROM_JAR=1` 이면 클래스 디렉터리 대신 `swingwebpanel-js.jar` 하나만 클래스패스에 둔다 |
| `tools/sourcecheck` | 기본 검사 경로에 `engine` 추가, `engine/tests/resources/` 내용 검사 제외, 새 규칙 `engine-boundary`, `javax.script` 규칙을 "제공자 패키지와 예제 패키지만 허용"으로 변경(아래 P26-05), 자체 시험 샘플 추가 |
| `.classpath` | 소스 폴더 `engine/src` 추가 |
| `tests/.../core/ThreadCreationAuditTest` | 스레드 생성 소스 점검 범위에 `engine/src` 추가 |
| `TestRunner` | `--scan` 을 여러 번 쓸 수 있게 확장 |

### 1.5 검증 (JDK 6u45, P26-01 시점의 값. 최종 값은 6절)
- `sh engine/build.sh` → `[OK] build/swingwebpanel-js.jar created (javac 1.6.0_45)`: **엔진이 빈 클래스패스/소스패스로 컴파일된다**(경계 강제 1).
- `sh build.sh` → 세 JAR 생성. 합친 JAR 3303 항목, 엔진 JAR 636, 브라우저 JAR 2674.
- 엔진 시험 단계(`engine/test.sh`, headless, 엔진 클래스만): **601 통과 / 0 실패 / 0 건너뜀**(12 초).
- 브라우저 시험 소스 484개가 합친 클래스 + 엔진 시험 클래스로 컴파일된다. 대표 스위트(`SwingWebPanelTest`, `ScriptLoadingTest`, `StaticStateAuditTest`, `ThreadCreationAuditTest`, `JarContentTest`) 26건 통과.
- `sh tools/sourcecheck/run.sh --selftest` → `selftest OK`, `sh tools/sourcecheck/run.sh engine src tests scripts build.sh build.bat test.sh test.bat tools/sourcecheck` → `0 violation(s) in 2673 file(s) checked`.

## 2. P26-02 독립 공개 API (`org.duckdns.hjow.swingwebpanel.js.embed`)

브라우저 없이 엔진을 쓰는 진입점이다. 패키지 이름은 `embed`(내장 API)로 정했다(`js.host` 는 브라우저 패키지라 피함).

### 2.1 클래스
| 클래스 | 역할 |
| --- | --- |
| `JsEngine` | 파사드. realm 하나(표준 내장 객체 전부)를 가진다. `eval(String[, name])`, `compile`+`eval(ParsedScript)`, `evalModule(key, source)`, 전역 `get/put/has/remove/isFunction/globalNames`, `call`, `invokeMethod`, `newObject/newArray`, `toPlain`, `convert`, `getInterface`, `runJobs`, `getLimits/cancel/resetCancel`, `setConsole`, `setModuleLoader`, `getRealm`(고급) |
| `JsHostFunction` | 스크립트가 Java 코드를 부르는 **유일한 통로**: `Object call(Object thisValue, Object[] args) throws Exception` |
| `JsHandle`, `JsObjectHandle`(`Map<String,Object>`), `JsArrayHandle`(`List<Object>`), `JsFunctionHandle`(+`JsCallable`) | 스크립트 객체/배열/함수에 대한 **살아 있는**(복사 없는) 보기 |
| `JsScriptError` | 잡히지 않은 스크립트 오류: 이름, 메시지, 스크립트 이름, 줄, 열, 던진 값, 엔진 예외, 호스트 함수가 던진 Java 예외(원인) |
| `JavaValues`, `InterfaceProxy` (패키지 비공개) | 값 변환, Java 인터페이스 프록시 |

엔진 쪽 보강(브라우저 동작은 바뀌지 않음): `JsRealm.parseScript(String,String)`/`evaluateScript(ParsedScript)` + 공개 인터페이스 `ParsedScript`(구현 `interp/ParsedProgram`)로 한 번 파싱해 여러 번 실행, `JsException` 에 **던져진 위치**(`getThrowSourceName/Line/Column`, `recordThrowSite`)와 호스트 함수 예외 원인(`withHostCause/getHostCause`) 추가, 인터프리터가 표현식을 빠져나가는 예외와 `throw` 문의 위치를 기록(`Expressions.eval`, `Statements` THROW). 구문 오류의 위치와 `getMessage()` 는 그대로이며 런타임 오류의 위치는 메시지에 넣지 않는다(브라우저의 콘솔 메시지가 바뀌지 않게).

### 2.2 값 변환 규칙 (클래스 Javadoc 에도 표로 있음)
- Java→스크립트: `null`→null, `JsEngine.UNDEFINED`→undefined, `Boolean`, 모든 `Number`(`doubleValue()`; `long` 이 2^53 을 넘으면 정밀도 손실), `String`/`CharSequence`/`Character`/`Enum`(`name()`)→문자열, `Map`→새 일반 객체(복사), `Collection`/모든 배열(기본형 포함)→새 배열(복사), `JsHostFunction`→함수, `JsHandle`→스크립트 객체 자체, `JsValue`→그대로(고급). **그 밖의 모든 Java 객체(Object, Date, File, 스트림, Class, Thread...)는 `IllegalArgumentException`** — 스크립트로 Java 객체, 클래스, 메서드가 새지 않는다. 순환하는 `Map`/`List` 는 모양을 유지한다.
- 스크립트→Java: undefined→`JsEngine.UNDEFINED`, null→null, boolean→`Boolean`, **number→항상 `Double`**(NaN/무한대/-0 유지), string→`String`, symbol→`"Symbol(설명)"` 문자열, 배열→`JsArrayHandle`, 함수→`JsFunctionHandle`, 그 밖의 객체(Date, RegExp, Map, promise, 오류 객체 포함)→`JsObjectHandle`. `toPlain` 은 `ArrayList`/`LinkedHashMap` 으로 깊게 복사한다(함수는 핸들).
- 핸들 `equals` 는 "같은 스크립트 객체"(Map/List 의 항목 기반 equals 는 자기 자신을 포함한 객체에서 끝나지 않으므로 쓰지 않음).

### 2.3 스레드·제한·오류 정책
- **스레드 모델**: 모든 메서드와 핸들 연산이 엔진(= `JsEngine` 인스턴스의 모니터)을 잠근다. 어느 스레드에서나 쓸 수 있고 연산은 **한 번에 하나씩 직렬화**되며 같은 스레드의 중첩(호스트 함수 안에서 엔진 재호출)은 허용된다. `cancel()` 만 잠그지 않는다. 시험 `testOneEngineFromManyThreads`(스레드 4개 x 300번 증가, 카운터 정확), `testCancelFromAnotherThread`.
- **제한**: Java 가 시작하는 모든 호출(`call`, 핸들 호출, 인터페이스 프록시)은 `beginTask/endTask` 로 감싸 단계 수와 시간이 0 부터 센다. 한도 초과/취소는 `ScriptAbortedException` 으로 호스트 함수를 통과한다(스크립트가 잡을 수 없음).
- **마이크로태스크**: 최상위 `eval/call/evalModule/...` 가 끝날 때마다 `runJobs()`(브라우저의 마이크로태스크 체크포인트와 같음).
- **오류**: 구문 오류/런타임 오류/`throw` → `JsScriptError`(이름, 메시지, 위치). 위치는 구문 오류는 잘못된 토큰, 런타임 오류는 평가 중이던 가장 안쪽 표현식, `throw` 는 그 문장의 줄이다. 호스트 함수가 던진 예외는 스크립트에 `Error` 로 전달되고(스크립트가 잡을 수 있음) 잡히지 않으면 원래 예외를 원인으로 가진 `JsScriptError` 가 된다.

### 2.4 P26-03 독립 모드 전역 범위 (결정)
- 전역에 있는 것: ECMA-262 6판 표준 내장 객체 전부, `globalThis`, `console`(콘솔 훅으로), `print`(`log` 수준 한 줄; 시험/편의용). 없는 것: `window`, `document`, DOM, `fetch`/`XMLHttpRequest`, Storage, **타이머**(`setTimeout` 등), `TextEncoder`, `URL`, `structuredClone`, `queueMicrotask`(이 엔진에 없음).
- **타이머 등 호스트 API 를 켜는 선택지는 만들지 않았다.** 근거: 독립 엔진에는 이벤트 루프가 없고, 루프를 누가 돌릴지(호출 스레드를 막는 `sleep`? 별도 스레드? 가상 시계?)는 호스트마다 다르다. 필요한 호스트는 `putFunction("setTimeout", ...)` 로 자기 함수를 등록하고 자기 루프에서 `JsEngine.call(...)`/`runJobs()` 를 부른다(예제 `EngineExample`). 엔진이 보장하는 것은 promise 작업의 실행뿐이다.
- 시험 `testEvalReturnsCompletionValueAndRunsEs6`(브라우저 전역이 없음을 확인), `testOtherJavaObjectsAreRefused`(전역 이름에 java/jdk/sun 이 없고 `java`/`Packages`/`JavaImporter`/`Java` 가 undefined).

### 2.5 시험 (JDK 6u45, 엔진 단계)
`js.embed.JsEngineBasicsTest`(12건: ES6 실행, 독립성, 전역, 호출, 컴파일/재실행, 오류 위치, catch, promise 작업, 모듈+로더, 콘솔, 제한, 취소, 다중 스레드)와 `JsEngineValuesTest`(12건: 양방향 변환, 순환, 거부 목록, 객체/배열/함수 핸들, `toPlain`, 호스트 함수와 오류, 중단 통과, 인터페이스 프록시) **24건 통과**. 엔진 단계 전체 **625 통과 / 0 실패 / 0 건너뜀**(601 + 24).

## 3. P26-04 `javax.script`(JSR 223) 제공자 (`org.duckdns.hjow.swingwebpanel.js.script`)

### 3.1 클래스와 등록
| 클래스 | 역할 |
| --- | --- |
| `JsScriptEngineFactory` | `ScriptEngineFactory`. 이름/확장자/MIME, `getParameter`(`THREADING`=`MULTITHREADED`), `getMethodCallSyntax/getOutputStatement/getProgram` |
| `JsScriptEngine` | `AbstractScriptEngine` + `Compilable` + `Invocable`. 팩터리마다 새 엔진(= 새 `JsEngine`/realm)이라 엔진 인스턴스끼리 독립 |
| `JsCompiledScript` | `CompiledScript`: `JsRealm.parseScript` 로 한 번 파싱한 AST 를 몇 번이든 실행(다른 realm 에서도) |
| `JsGlobalBindings` | 기본 컨텍스트의 `ENGINE_SCOPE` = 엔진 전역 객체의 **살아 있는** `Bindings`(`engine.put("x",1)` = `globalThis.x=1`) |
| `ContextConsole` | `print`/`console.*` → 실행 중인 `ScriptContext` 의 `getWriter()`(log/info/debug/print) 와 `getErrorWriter()`(warn/error), 한 메시지 한 줄 |
| 등록 | `engine/resources/META-INF/services/javax.script.ScriptEngineFactory`(엔진 JAR 루트, `.gitattributes` 로 LF 고정) |

### 3.2 `ScriptContext` 의 ENGINE/GLOBAL 범위와 JavaScript 전역의 연결 규칙
- 기본 `ENGINE_SCOPE`: 엔진의 전역 객체를 그대로 보여 주는 `JsGlobalBindings`. 변환은 `JsEngine` 규칙을 따르되 **undefined 는 null**(Rhino 와 같음). 키 규약은 `Bindings`(null→NPE, String 아님→ClassCastException, 빈 문자열→IAE). 전역 `var` 는 JavaScript 에서 삭제할 수 없으므로 `remove` 는 undefined 로 만든다.
- **다른 `ENGINE_SCOPE` 바인딩**(`eval(script, bindings)` 또는 다른 바인딩을 준 컨텍스트): 엔진은 **`Bindings` 객체(동일성)마다 완전한 realm 하나**를 둔다(약한 참조 목록, 수거되면 정리). 실행 전 바인딩 항목을 그 realm 의 전역으로 복사하고, 실행 뒤 **새로 생겼거나 값이 바뀐 열거 가능 전역**만 바인딩으로 복사하며 스크립트가 지운 전역은 바인딩에서 지운다(그래서 바뀌지 않은 `Integer` 는 `Integer` 로 남음). 같은 바인딩 객체는 `eval` 사이에 변수를 유지하고 서로 다른 바인딩은 변수도 내장 객체 변경도 공유하지 않는다. 새 realm 은 기본 엔진의 실행 제한과 모듈 로더를 복사한다. 한계: `let/const/class` 최상위 선언은 realm 의 선언적 스코프에 남고 바인딩 항목이 아니다(같은 스코프의 이후 스크립트는 보지만 같은 이름을 다시 선언하면 브라우저처럼 SyntaxError).
- `GLOBAL_SCOPE`(`ScriptEngineManager` 의 `put`): 엔진 스코프에 없는 이름을 읽을 때만 찾는다(`JsRealm.NamedGlobals`). 읽기 전용이며 대입은 엔진 스코프로 간다. 같은 매니저의 다른 엔진이 공유한다.
- 변환할 수 없는 Java 객체(`new Object()` 등)를 바인딩에 넣으면 live 바인딩의 `put` 은 `IllegalArgumentException`, `eval(script, bindings)` 의 항목은 `ScriptException` 이다. **Rhino/Nashorn 처럼 Java 객체를 스크립트에 노출하지 않는다.** 스크립트가 Java 를 부르려면 `JsHostFunction` 값을 바인딩에 넣는다.
- `Invocable`: `invokeFunction` 은 기본 컨텍스트 전역 스코프의 함수(없으면 `NoSuchMethodException`), `invokeMethod(thiz, ...)` 의 `thiz` 는 `eval`/`invoke` 가 돌려준 스크립트 객체(`JsHandle`; null/그 밖은 `IllegalArgumentException`), `getInterface` 는 `java.lang.reflect.Proxy`(인터페이스의 **모든** 메서드가 함수로 있어야 하고 아니면 null, 대상이 함수 자신이면 메서드 하나짜리 인터페이스).
- 오류: `ScriptException(메시지="Name: message", 파일 이름, 줄, 열)` + 원인 `JsScriptError`. 파일 이름은 컨텍스트 속성 `javax.script.filename`(없으면 `<eval>`), 위치를 모르면 -1. 제한/취소로 멈춘 스크립트는 원인이 `ScriptAbortedException` 인 `ScriptException`. `compile` 의 구문 오류도 같은 형식.
- 스레드: `THREADING`=`MULTITHREADED`. 같은 전역 스코프의 연산은 엔진 잠금으로 직렬화된다(시험 4스레드 x 200번 호출, 카운터 정확).
- 재진입: 호스트 함수가 같은 스크립트 엔진의 `eval` 을 다시 불러도 안쪽이 끝나면 바깥 스크립트의 `GLOBAL_SCOPE` 조회와 출력 컨텍스트를 되돌린다(`Scope.lookup`). 되돌리지 않는 변형으로 시험이 `ReferenceError: fromManager is not defined (<eval> 1:12)` 로 실패함을 확인했다(`testNestedEvalKeepsTheGlobalScopeLookup`).
- 처리되지 않은 promise 거부(`JsEngine`): 모든 작업이 끝난 뒤에도 처리기 없는 거부는 콘솔 연결에 `Uncaught (in promise) ...` 오류(`ConsoleRecord.Kind.UNHANDLED_REJECTION`)로 보고한다(브라우저의 `PageScripts` 와 같은 규칙; `testUnhandledRejectionsAreReported`). `javax.script` 에서는 컨텍스트의 에러 라이터로 간다.
- `JsScriptEngine.getJsEngine()` 으로 기본 스코프의 `JsEngine` 을 얻어 실행 제한/시간 제한/취소/모듈 로더를 지정한다.

## 4. P26-05 이름 충돌과 금지 규칙 정리

### 4.1 이름 정책 (결정과 근거)
- JRE 6~7 의 Rhino 와 JRE 8~14 의 Nashorn 이 `js`/`JavaScript`/`javascript`/`ECMAScript`/`ecmascript`, 확장자 `js`, MIME `application/javascript` 등으로 등록돼 있어 `getEngineByName("javascript")` 가 어느 팩터리를 돌려주는지는 클래스패스 검색 순서에 달려 있고(실제로는 JRE 쪽이 먼저), 이 엔진이 일반 이름을 가로채면 **기존 프로그램이 조용히 다른 엔진(ES6, Java 객체 노출 없음)을 받게 된다.** 그래서 팩터리는 **고유한 식별자만** 등록한다: 이름 `swingwebpanel-js`(첫째), `swingwebpanel-javascript`, 확장자 `swjs`, MIME `application/x-swingwebpanel-javascript`.
- 일반 이름이 필요한 프로그램(예: 자체 엔진이 없는 Java 15+)은 JVM 옵션 `-Dswingwebpanel.js.genericNames=true`(팩터리 메서드가 호출 시점에 읽음; `ScriptEngineManager` 를 만들기 전에 지정)로 일반 이름/확장자 `js`/일반 MIME 도 등록할 수 있다. 그때도 어느 엔진이 답하는지는 JRE 의 검색 순서에 달려 있다.
- 이 엔진은 Rhino/Nashorn 등 다른 엔진에 위임하지 않는다(`EngineIndependenceTest` 가 엔진 클래스의 상수 풀에서 `javax/script`(제공자 패키지 제외), `org/mozilla`, `jdk/nashorn`, `sun/org/mozilla` 등을 찾는다).

### 4.2 시험이 증명하는 것 (`ScriptEngineDiscoveryTest`)
- `getEngineByName("swingwebpanel-js")` 가 `instanceof JsScriptEngine`/`getFactory() instanceof JsScriptEngineFactory` (Rhino/Nashorn 으로 평가된 결과를 합격으로 인정하지 않음), 두 번째 이름/확장자/MIME 도 같음.
- 매니저의 팩터리 중 이 프로젝트의 것은 정확히 하나, ES2015 문법(화살표 함수, 템플릿 문자열; Rhino 와 JRE 8 Nashorn 은 파싱하지 못함)과 숫자 타입(`Double`)으로 평가 주체를 확인.
- 기본 설정에서 `getEngineByName("javascript")`, `getEngineByExtension("js")`, `getEngineByMimeType("application/javascript")` 가 **이 엔진을 돌려주지 않음**, 속성을 주면 일반 이름이 추가되고 고유 이름이 첫째로 유지됨.

### 4.3 `tools/sourcecheck` 규칙 변경
- 옛 규칙(모든 `javax.script` 사용 금지)을 `checkScriptingApi` 로 교체: `javax.script` 는 **엔진 제공자 패키지**(`engine/**/swingwebpanel/js/script/`, 소스와 시험)와 **예제 패키지**(`examples/src/**/examples/script/`)에서만 허용하고 나머지는 모두 `forbidden-engine` 위반이다(제품 코드의 `ScriptEngineManager` 사용 금지 유지, 브라우저 시험도 불허).
- 새 규칙 `engine-boundary`(P26-01): `engine/` 아래 소스는 `java.awt/applet`, `javax.swing/imageio/sound/print/accessibility`, `java.net/rmi/sql/nio`, `javax.net/sql/rmi/naming`, `java.io.File*`, 브라우저 패키지(`core dom css html layout render net storage media content download`, 루트 `SwingWebPanel`/`Console*`, `js.host/dom/worker`)를 쓰면 위반(엔진 시험은 AWT/Swing/브라우저 패키지만 금지하고 `testing`·파일은 허용).
- 규칙을 바꾼 뒤 확인(JDK 6u45, 실제 실행): 임시 샘플 7개(`src/.../core`, `engine/src/.../js/embed`, `engine/src/.../js/script`, `engine/tests/.../js/script`, `examples/.../examples/script`, `examples/.../examples`, `tests/.../js/script`)에 `javax.script` import 를 넣어 돌리면 **4건 위반**(core, embed, 일반 examples, 브라우저 tests)이고 제공자 소스/시험/예제 script 패키지는 통과. `engine/src` 에 임시로 `java.awt.Color` 를 쓰는 클래스를 넣으면 sourcecheck 종료 코드 1, 클래스 상수 풀 시험 실패(`TempLeak.class references java/awt/`), 브라우저 패키지를 쓰면 엔진 컴파일 실패(`package ...core does not exist`). 임시 파일은 지웠다. `--selftest` 에 불량/양호 샘플 추가(`selftest OK`).

### 4.4 시험 결과 (엔진 단계, P26-05 시점. 최종 값은 6절)
- JDK 6u45: `js.script` 17건 통과(`JsScriptEngineTest` 13, `ScriptEngineDiscoveryTest` 4), `EngineIndependenceTest` 4건 통과(새 2건 포함), 엔진 단계 전체 **644 통과 / 0 실패 / 0 건너뜀**.
- **JDK 8u202(Nashorn 있음)**: 같은 시험 클래스를 JDK 8 의 `java` 로 실행해 `js.script` 17건 통과(`getEngineByName("javascript")` 는 Nashorn 이 답하고 이 엔진이 아님을 확인). 이 실행은 JRE 6 검증이 아니며 이름 충돌 확인용이다.

## 5. P26-06 브라우저 컴포넌트와 엔진의 관계, JAR 배포 형태

### 5.1 브라우저는 엔진의 공개 API 만 쓴다 (컴파일러가 강제)
브라우저 모듈은 **엔진 소스 없이 컴파일된 엔진 클래스(`build/engine-classes`)만 클래스패스에** 두고 컴파일된다(`build.sh`/`build.bat`). 따라서 엔진의 비공개/패키지 비공개 멤버를 쓰면 컴파일이 실패하며, 지금 컴파일되므로 이미 공개 API 만 쓰고 있다. 브라우저가 쓰는 엔진 클래스: 루트(`Js`, `JsValue`, `JsObject`, `JsRealm`, `JsException`, `Ops`, `NativeFunction`, `WellKnownSymbols`, `ConsoleRecord(Hook)`, `ModuleLoader/Graph` 등 약 30종. `JsValue`/`JsObject` 는 각각 94개 파일, `JsRealm` 은 87개 파일이 import)와, 루트 밖에서는 13개 파일이 쓰는 공개 클래스 몇 개: `builtins.typed.{TypedArrays,TypedArrayObject,ElementType,ArrayBufferObject,DataViewObject}`, `builtins.meta.{PromiseJobs,PromiseRejectionTracker}`, `builtins.datajson.DataAccess`, `regexp.{RegExpCompiler,RegExpProgram,RegExpFlags,RegExpSyntaxException,RegExpLimitException}`. 이들을 더 좁은 공개 API 로 감쌀지는 정하지 않았다(엔진 쪽 위의 클래스들은 이미 `public` 이고 시험으로 검증됨). **브라우저가 `js.embed`/`js.script` 를 쓰지는 않는다**(새 API 는 엔진 독립 사용자를 위한 것이며 `SwingWebPanel` 의 동작·성능·공개 API 는 바뀌지 않았다. 유일한 엔진 쪽 변경은 `Expressions.eval` 의 try/catch(예외 위치 기록)와 `parseScript`/`ParsedScript`, 속도 차이는 5.3).

### 5.2 JAR 배포 형태 (결정)
`build.sh`/`build.bat` 가 세 JAR 를 만든다. 결정 근거: 기존 사용자가 `swingwebpanel.jar` **하나만** 클래스패스에 두는 방식이 깨지면 안 된다(브리프 우선순위).
- `swingwebpanel.jar` = 엔진 + 브라우저(합침, 기존 이름과 사용 그대로). 합친 클래스 디렉터리 `build/classes` 에서 만든다(시험, 예제, 도구가 그대로 사용).
- `swingwebpanel-js.jar` = 엔진만(`META-INF/services/javax.script.ScriptEngineFactory` 포함).
- `swingwebpanel-browser.jar` = 브라우저만(엔진 클래스를 **포함하지 않음**, 실행에는 엔진 JAR 필요).
브리프 예시의 이름 `swingwebpanel-all.jar` 는 만들지 않았다. P23(Maven)에서 모듈 산출물 이름을 정할 때 합친 JAR 의 이름(예: Maven 의 `swingwebpanel` 은 브라우저만, 합친 것은 `swingwebpanel-all`)을 다시 정해야 한다 → "조정자 요청".

### 5.3 엔진 쪽 변경이 속도에 주는 영향 (측정)
`Expressions.eval` 가 `JsException` 을 잡아 위치를 기록하는 try/catch 를 얻었다. JDK 6u45, 같은 PC, 워밍업 뒤 `fib(24)` 와 30만 번 속성 읽기 반복을 변경 전(`65cb181` 의 엔진)과 변경 후로 두 번씩 돌린 결과 변경 전 47~68 ms / 145~161 ms, 변경 후 46~72 ms / 148~168 ms 로 **차이가 잡음 안**이다(공유 PC, 4코어). realm 생성은 평균 1.4 ms 이다(워밍업 뒤 30회) — 다른 바인딩마다 realm 을 두는 설계(3.2절)의 비용이 작다는 근거.

### 5.4 시험
- 엔진 단계 `js.script.EngineJarTest`(2건): 엔진 JAR 에는 엔진 클래스와 등록 파일만 있고(브라우저 클래스, `testing`, `examples`, 도구 없음, 등록 파일은 팩터리 이름), **부모가 없는 `URLClassLoader` 가 이 JAR 하나만 보고** `JsEngine` 으로 스크립트를 실행하고 `ScriptEngineManager(loader)` 로 엔진을 찾으며 `SwingWebPanel`, `js.host.PageScripts` 는 `ClassNotFoundException`.
- 브라우저 단계 `testing.selftest.BrowserJarsTest`(3건): 브라우저 JAR 에는 엔진 항목이 하나도 없고 엔진 JAR 에는 브라우저 항목이 하나도 없으며 둘은 겹치지 않음, **단일 JAR = 두 JAR 의 합집합**(항목 단위), 부모 없는 클래스 로더가 **브라우저 JAR + 엔진 JAR** 와 **단일 JAR 하나**로 각각 `SwingWebPanel` 을 만들어 `console.log` 를 실행하는 페이지의 메시지를 `ConsoleListener` 로 받음. `JarContentTest` 는 단일 JAR 에 엔진과 등록 파일이 있는지도 확인.
- 결과(JDK 6u45): `EngineJarTest` 2/2, `BrowserJarsTest` 3/3(`testPageScriptsRunFromTheJars` 1.5 초), `JarContentTest` 1/1.

## 6. P26-07 최종 검증 결과 (JDK 6u45, 이 PC: Windows 10, 4코어, 다른 작업자와 공유)

### 6.1 전체 회귀 (`BUILD_DIR=build/full sh test.sh`, 커밋 `9ec5c8c` 의 트리)
- 엔진 단계(엔진 클래스만, `java.awt.headless=true`): **646 통과 / 0 실패 / 0 건너뜀**.
- 브라우저 단계(합친 클래스): **3174 통과 / 0 실패 / 14 건너뜀**(건너뜀 14건은 분리 전과 같은 선택적 Opus 원본 벡터 12건 + Java Sound 믹서가 필요한 2건).
- 합계 `=== TOTAL (engine + browser): 3820 passed, 0 failed, 14 skipped ===`, 러너 종료 코드 자체 점검 통과, 종료 코드 0.
- **분리 전 기준(조정자가 준 `main` 마지막 전체 회귀 3771/0/14)과 비교**: 실패 0, 건너뜀 14 로 같고 통과가 +49 이다. 이 가운데 **새 시험이 48건**이다(엔진 단계 45 = `js.embed` 24 + `js.script` 17 + `EngineIndependenceTest` 2 + `EngineJarTest` 2, 브라우저 단계 3 = `BrowserJarsTest`). 나머지 +1 은 원인을 찾지 못했다(소스의 `public void test*()` 메서드 수는 `main` 3788 대비 새 시험 만큼만 늘었고, 기준 수치가 소스와 1건 어긋나 있었을 가능성이 있다). 분리로 사라진 시험은 없다.

### 6.2 전체 회귀 뒤에 바꾼 것과 그 검증 (전체 회귀를 다시 돌리지는 않았다)
전체 회귀 뒤의 변경은 `js.embed`/`js.script`(브라우저가 쓰지 않는 패키지)와 시험·문서뿐이고 브라우저 클래스는 바뀌지 않았으므로 브라우저 단계 결과는 그대로 유효하다. 바뀐 부분은 아래로 다시 확인했다.
- 바뀐 것: `JsEngine` 의 처리되지 않은 promise 거부 보고, `JsScriptEngine` 의 재진입 수정, 새 시험 `testUnhandledRejectionsAreReported`, `testNestedEvalKeepsTheGlobalScopeLookup`, 브라우저 시험 `js.host.HostIndependenceTest`(브라우저 모듈에 남은 `js.host/dom/worker` 클래스에서 금지 이름을 찾던 옛 `EngineIndependenceTest` 의 범위를 이어받음), 문서와 README.
- `HostIndependenceTest` 는 합친 클래스로 1/1 통과(전체 회귀에는 포함되지 않음: 전체 회귀의 시험 컴파일 뒤에 추가함).
- 새 시험이 실제로 결함을 잡는지: 재진입 수정을 되돌린 변형에서 `testNestedEvalKeepsTheGlobalScopeLookup` 이 실패하고 복원하면 통과함을 확인했다(4절과 3.2절).

### 6.3 최종 트리에서 다시 돌린 것 (모두 JDK 6u45, 별도 표시한 것 제외)
| 무엇 | 결과 |
| --- | --- |
| 엔진 단계, 클래스 디렉터리, headless (`sh engine/test.sh`) | **648 통과 / 0 실패 / 0 건너뜀** (예상: 전체 회귀의 646 + 새 시험 2) |
| 엔진 단계, **엔진 JAR 하나만 클래스패스**, headless (`SWP_ENGINE_FROM_JAR=1`) | 645 통과 / 0 실패 / 3 건너뜀(상수 풀 검사 3건: JAR 에서는 클래스 디렉터리를 스캔할 수 없다는 이유) |
| `js.script` 시험 20건을 **JDK 8u202(Nashorn 있음)** 로 실행 | 20 통과 / 0 실패 / 0 건너뜀(이름 충돌 확인용, JRE 6 검증 아님) |
| 엔진 단계 전체를 JDK 8 로, 엔진 JAR 만 | 644 통과 / **1 실패** / 3 건너뜀. 실패는 `js.date.DateTest.testRoundTrips`(`Asia/Seoul`, 1900년 이전 시각의 왕복: JRE 6 과 8 의 시간대 데이터 차이로 보이며 이 작업이 옮기기만 한 시험이다. 원인을 더 조사하지 않았다). JRE 6 에서는 통과한다 |
| `examples/run.sh`: `EngineExample --smoke`, `script.ScriptEngineExample --smoke` (`SWP_CLASSES=build/engine-classes`, 엔진 클래스만) | 둘 다 `smoke OK` |
| `script.ScriptEngineExample --smoke` 를 엔진 JAR 만으로 JRE 6 과 JRE 8 에서 | 둘 다 `smoke OK engine=...js.script.JsScriptEngine` |
| 기존 브라우저 예제 `BlankPageExample`, `StaticPageExample`, `ConsoleExample` `--smoke` (새 `run.sh`) | 통과. `ConsoleExample` 은 CPU 가 바쁜 동안(전체 회귀 실행 중) 8번 중 3번 `consoleEvents=0`(1초 창 안에 메시지가 오지 않음)이었고, 한가할 때는 5번 모두 `consoleEvents=2` 였다. 부하 때문으로 보이나 분리 전 코드로 같은 부하에서 비교하지는 않았다 |
| `tools/sourcecheck` 전체(`engine src tests examples scripts tools docs` 등) | 0 위반 (마지막 실행: README/examples/engine/tests/스크립트 1742개 파일, 이후 변경분 306개 파일) |
| Windows cmd: `.\build.bat`, `engine\test.bat --filter ...`, `.\test.bat --filter BrowserJarsTest` | 모두 `[OK]`/통과(`test.bat` 는 두 단계와 러너 종료 코드 점검까지 실행됨) |
| 속도: 변경 전/후 엔진(`fib(24)`, 반복 30만 번) | 차이는 잡음 안(5.3절) |

## 7. 남은 한계와 확인하지 못한 것 (정직한 기록)

### 7.1 구현 한계
- 최상위 `let`/`const`/`class` 선언은 전역 객체의 프로퍼티가 아니므로 `JsEngine.get`/`JsGlobalBindings`/`engine.get` 으로 읽을 수 없다(스크립트와 같은 스코프의 이후 `eval` 은 본다). 읽으려면 `eval("name")`.
- 전역 `var`/함수 선언은 JavaScript 규칙상 삭제할 수 없어 `Bindings.remove` 는 값을 undefined 로 만들 뿐 키는 남는다(`containsKey` 는 true).
- 다른 `ENGINE_SCOPE` 바인딩은 realm 을 따로 가진다: 바인딩 항목은 실행 전후에 값으로 복사되므로(함수/객체는 핸들로 양쪽에서 같은 객체를 가리킴) 바인딩 값이 `Integer` 였다가 스크립트가 바꾸면 `Double` 로 돌아온다. 한 realm 의 객체를 다른 바인딩의 스크립트에 넘기면 두 realm 의 내장 객체가 섞인다(안전하지만 `instanceof Array` 같은 비교는 realm 마다 다름).
- `java.util.Date`/`Calendar`/`File`/스트림 등은 어느 방향으로도 자동 변환하지 않는다(`IllegalArgumentException`). JS `Date` 는 `JsObjectHandle`(`callMember("getTime")`).
- 런타임 오류의 줄/열은 **평가 중이던 가장 안쪽 표현식의 시작 위치**와 `throw` 문의 위치이며, 호스트 함수 안에서 난 오류는 그 호스트 함수를 부른 표현식의 위치를 가진다. 함수 호출 스택(역추적)은 제공하지 않는다.
- 독립 모드에는 타이머, `queueMicrotask`, `fetch`, `URL`, `TextEncoder`, Storage 가 없다(2.4절).
- `JsScriptEngineFactory` 는 일반 이름을 기본으로 등록하지 않는다(4.1절). `getEngineByName("javascript")` 를 이 엔진으로 받으려면 JVM 속성이 필요하고 그래도 JRE 의 검색 순서에 따른다.
- 엔진 JAR 만 클래스패스에 두고 엔진 시험을 돌리면 클래스 파일 상수 풀을 훑는 시험 3건은 "클래스 디렉터리가 아니라 JAR 에서 실행 중"이라는 이유로 건너뛴다(디렉터리 실행 때는 통과). 엔진 JAR 가 부모 없는 클래스 로더에서 동작하는 것은 `EngineJarTest` 가 따로 확인한다.

### 7.2 확인하지 못한 것
- **P23(Maven)은 하지 않았다**(지시). `engine/` 을 모듈로 옮기는 방법은 "조정자 요청"에 적었다.
- 엔진 단독 모드를 **JRE 6 이외의 최신 JRE(8 이외)** 에서는 돌리지 않았다. JRE 8(`jdk1.8.0_202`)에서 `js.script` 시험 20건과 `javax.script` 예제만 통과를 확인했고 엔진 시험 전체는 `DateTest.testRoundTrips` 1건이 실패한다(6.3). JRE 7 에서는 확인하지 않았다.
- `javax.script` 제공자를 Rhino 가 있는 JRE 6 에서 `ScriptEngineManager` 의 **여러 팩터리가 같은 이름을 다투는 경우**는 일반 이름 옵션을 켜고 확인하지 않았다(`testCommonNamesAreOptIn` 은 팩터리 메서드의 목록만 확인).
- `tools/uicheck`(실제 Swing 창)와 실제 오디오 장치는 이번 작업과 무관해 다시 확인하지 않았다(코드 이동만 했고 시험은 합친 클래스로 돌렸다).
- `build.bat`/`test.bat`/`engine\build.bat`/`engine\test.bat` 는 이 Windows PC 에서 cmd 로 실행해 `[OK]` 를 확인했다(`test.bat` 전체 실행은 시간 때문에 `--filter` 를 주어 확인).

## 조정자 요청

### A. `docs/ARCHITECTURE.md` 1.1절 (표의 `javax.script` 행 교체 + 모듈 구성 문단 추가)
표의 행을 다음으로 바꾼다.

```text
| 다른 엔진을 `javax.script.ScriptEngineManager` 로 호출하는 것 | 내장 Rhino/Nashorn 우회 사용 금지. 단 **이 프로젝트의 자체 엔진을 `javax.script` 제공자로 공개하는 것은 허용**한다: `js.script` 패키지(와 그 시험, `examples/**/script` 예제)만 `javax.script` 를 쓸 수 있고(`tools/sourcecheck` 의 `forbidden-engine` 규칙이 강제) 나머지 제품 코드의 `ScriptEngineManager` 사용은 금지다. 엔진은 고유 이름 `swingwebpanel-js` 로만 등록한다(P26-05). |
```

그리고 구조 절에 "모듈 구성(P26)" 문단: 엔진 모듈 `engine/`(`org.duckdns.hjow.swingwebpanel.js` 아래 `ast parser interp builtins date num regexp text embed script` 와 루트 클래스; 브라우저, AWT/Swing, 네트워크, 파일에 의존하지 않음. 빈 클래스패스로 컴파일, `tools/sourcecheck` `engine-boundary`, 클래스 상수 풀 시험, headless 엔진 단계 시험으로 강제), 브라우저 모듈 `src/`(`js.host`, `js.dom`, `js.worker` 포함; 엔진 클래스만 참조). 산출물 JAR 세 개(5.2절). 도구 목록은 변경 없음(`tools/sourcecheck` 규칙만 확장).

### B. `docs/FEATURE_MATRIX.md` 새 행 (API-28, API-29 와 MOD-01)
```text
| API-28 | 독립 JavaScript 엔진 API `js.embed.JsEngine` + 핸들(`JsObjectHandle`/`JsArrayHandle`/`JsFunctionHandle`), `JsHostFunction`, `JsScriptError` (P26-02/03, 확장 API) | ECMA-262 6판(엔진), 값 변환 규칙은 `docs/progress/engine.md` 2.2 | `js.embed.*`, `js.JsRealm.parseScript`, `js.ParsedScript`, `js.JsException`(던진 위치) | `js.embed.JsEngineBasicsTest`(13건), `js.embed.JsEngineValuesTest`(12건) | 검증 완료 | 독립 모드 전역(ES2015 내장 + console + print, 타이머/브라우저 객체 없음), 값 양방향 변환(숫자 항상 Double, Java 객체 거부), 오류 위치, 제한/취소, 직렬화된 다중 스레드 사용. 한계: 최상위 let/const/class 는 `get` 으로 못 읽음, Date/File 등 자동 변환 없음 |
| API-29 | Java Scripting API(JSR 223) 제공자 `js.script.*`, 서비스 파일 `META-INF/services/javax.script.ScriptEngineFactory` (P26-04/05, 확장 API) | JSR 223 (Java 6 `javax.script`) | `js.script.JsScriptEngineFactory`/`JsScriptEngine`/`JsCompiledScript`/`JsGlobalBindings`/`ContextConsole` | `js.script.ScriptEngineDiscoveryTest`(4건), `js.script.JsScriptEngineTest`(13건), `js.script.EngineJarTest`(2건), `js.EngineIndependenceTest`(4건) | 검증 완료 | `eval`(문자열/Reader/바인딩/컨텍스트), `Compilable`, `Invocable`(`getInterface`), ENGINE/GLOBAL 범위, 컨텍스트 라이터, 위치가 있는 `ScriptException`, 고유 이름만 등록(일반 이름은 `-Dswingwebpanel.js.genericNames=true`). JRE 6 과 JRE 8(Nashorn 있음)에서 확인. 한계는 engine.md 3.2, 7.1 |
| MOD-01 | 엔진/브라우저 모듈 분리(`engine/`, `src/`), JAR 세 개 (P26-01/06) | - | `engine/**`, `build.sh`/`engine/build.sh`, `tools/sourcecheck`(`engine-boundary`) | `js.EngineIndependenceTest`(경계 2건), `js.script.EngineJarTest`, `testing.selftest.BrowserJarsTest`, `testing.selftest.JarContentTest`, 엔진 단계 시험(엔진 클래스만, headless) | 검증 완료 | 엔진은 빈 클래스패스로 컴파일됨. 합친 JAR 는 두 JAR 의 합집합 |
```

### C. `docs/PROGRESS.md` 작업 로그 항목(초안)
"2026-11-22 · P26 JavaScript 엔진 분리와 `javax.script` 제공자(ENGINE 작업자, 브랜치 `worktree-agent-a8e4159237316313b`)": 한 일 = 6장 요약(엔진 `engine/` 분리 275개 파일 이동, `js.embed` 독립 API, `js.script` 제공자, 이름 정책, JAR 세 개, 경계 강제 4중), 변경 파일 = 이 문서 1.3~1.4, 2.1, 3.1, 4.3, 5, 검증 결과 = 6절, 미해결 = 7절, 다음 = P23(Maven; 아래 D).

### D. P23(Maven 전환) 때 `engine/` 을 모듈로 옮기는 방법
| 지금 | Maven 모듈 `swingwebpanel-js` |
| --- | --- |
| `engine/src` | `engine/src/main/java` |
| `engine/resources`(`META-INF/services`) | `engine/src/main/resources` |
| `engine/tests/src`(엔진 시험 + **시험 실행기 `testing.TestRunner/Check/...`**) | `engine/src/test/java`. 실행기는 브라우저 시험도 쓰므로 `test-jar` 로 내보내 브라우저 모듈이 `test` 범위로 의존한다 |
| `engine/tests/resources` | `engine/src/test/resources` |
| `src`, `tests/src`, `tests/resources` | 브라우저 모듈(`swingwebpanel`): `src/main/java`, `src/test/java`, `src/test/resources`. 엔진 모듈에 의존 |
- `engine/build.sh|bat`, `engine/test.sh|bat`, 루트 `build.sh|bat`/`test.sh|bat`, `scripts/pkgtest.sh` 의 `engine/` 분기, `examples/run.sh|bat` 의 `SWP_CLASSES=build/engine-classes` 는 Maven 으로 대체한다. 엔진을 컴파일 클래스패스 없이 빌드하는 규칙은 Maven 에서는 엔진 모듈의 `dependencies` 를 비워 두는 것(의존성이 생기면 빌드가 깨짐)으로 유지한다. 합친 단일 JAR(`swingwebpanel.jar`, 5.2절)은 `maven-shade-plugin`/`assembly` 로 만든다.
- 시험 실행은 자체 실행기 `TestRunner` 를 JDK 6 `java` 로(P23-04). 두 단계(엔진 단계는 엔진 클래스만 + `java.awt.headless=true`, 브라우저 단계)는 두 모듈의 `test` 단계로 그대로 대응한다. `swp.jar`/`swp.engine.jar`/`swp.browser.jar` 시스템 속성은 `package` 단계 뒤의 JAR 경로를 가리키게 한다.
- `.gitattributes` 에 `engine/resources/META-INF/services/* text eol=lf` 를 추가했다(서비스 파일의 줄 끝 고정).

### E. `AGENTS.md`
- P26-01~07 의 체크 후보는 아래 "체크박스 후보". P26-01 의 "엔진 패키지 이름의 유지/변경과 그 영향" 은 2절/1.2 표(유지, 근거: 브라우저 코드 수백 개와 시험 전체 영향), "Worker 의 범용 부분과 `ModuleLoader`" 는 `ModuleLoader/ModuleGraph` 가 엔진 루트에 있어 엔진으로 갔고 `js.worker` 는 DOM/브라우저 의존이라 브라우저에 남았다.
- 계획 문서의 P26-02 문구 "JavaScript 함수 호출, Java 값과 JavaScript 값의 변환 규칙" 은 2.2절. P26-03 의 "타이머 등을 선택적으로 켤 수 있게 할지" 는 **켜지 않는 것으로 결정**(2.4절).

## 체크박스 후보 (조정자가 판단)
- **P26-01** 완료 후보: 경계 확인(grep 0건), 결정 기록(1.2), `engine/` 분리, 빈 클래스패스 컴파일, `engine-boundary` 규칙, 헤드리스 엔진 단계 시험, 클래스 상수 풀 시험. 근거 1절, 4.3, 5절, 6절.
- **P26-02** 완료 후보: `js.embed` 공개 API 와 값 변환/예외/제한/큐/콘솔/모듈/스레드 규칙(2절), 시험 25건.
- **P26-03** 완료 후보: 독립 모드 전역 범위와 "타이머 등은 켜지 않음" 결정(2.4절), `testEvalReturnsCompletionValueAndRunsEs6` 등.
- **P26-04** 완료 후보: `javax.script` 제공자 전체(3절), 시험 `js.script.*`.
- **P26-05** 완료 후보: 고유 이름만 등록 + 옵트인 일반 이름, `ScriptEngineDiscoveryTest`, sourcecheck 규칙 변경과 실패 사례 확인(4절). 문서 문구는 "조정자 요청 A"를 적용해야 완결(ARCHITECTURE 1.1).
- **P26-06** 완료 후보: 브라우저는 컴파일된 엔진 클래스의 공개 API 만 사용(5.1), JAR 세 개와 시험(5.2~5.4), 문서(README).
- **P26-07** 완료 후보: (1) 엔진 JAR 하나/클래스 디렉터리 + headless 엔진 단계 시험 JRE 6 통과, (2) `ScriptEngineManager` 로 고유 이름 조회 검증을 JRE 6 과 JRE 8 에서 확인, (3) 브라우저 전체 회귀 결과(6절), (4) 자동 경계 검사 3종, README/예제 완료. 상위 **P26** 은 P26-01~07 이 모두 체크된 뒤.
- 체크하지 말아야 할 것: P23(Maven), P22 의 남은 항목. 이 작업은 P23 을 하지 않았다.
