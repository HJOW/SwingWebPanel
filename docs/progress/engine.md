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
| `examples/run.sh|bat`, `tools/uicheck` 등 | 변경 없음 — `build/classes`(합친 디렉터리)를 계속 쓴다 |
| `tools/sourcecheck` | 기본 검사 경로에 `engine` 추가, `engine/tests/resources/` 내용 검사 제외, 새 규칙 `engine-boundary`, `javax.script` 규칙을 "제공자 패키지와 예제 패키지만 허용"으로 변경(아래 P26-05), 자체 시험 샘플 추가 |
| `.classpath` | 소스 폴더 `engine/src` 추가 |
| `tests/.../core/ThreadCreationAuditTest` | 스레드 생성 소스 점검 범위에 `engine/src` 추가 |
| `TestRunner` | `--scan` 을 여러 번 쓸 수 있게 확장 |

### 1.5 검증 (JDK 6u45)
(결과는 작업을 진행하며 아래에 추가한다.)
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

## 조정자 요청
(작업이 끝나면 AGENTS.md / PROGRESS.md / FEATURE_MATRIX.md / ARCHITECTURE.md 에 넣을 문장을 여기에 모은다.)

## 체크박스 후보
(P26-01~07 이 끝나면 적는다.)
