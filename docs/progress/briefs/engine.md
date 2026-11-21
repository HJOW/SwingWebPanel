# ENGINE 작업자 브리프 (P26 JavaScript 엔진 분리와 `javax.script` 제공자)

먼저 `docs/progress/briefs/common.md`(공통 규칙)를 읽는다. 이 문서의 P26 본문은 `AGENTS.md` 의 "P26. JavaScript 엔진 분리와 Java Scripting API 지원"(P26-01~07)과 같으며 아래에 이 저장소 사정에 맞춘 결정과 제약을 더했다.

## 작업 위치
- 당신은 git worktree 에서 일한다(작업 디렉터리가 그 worktree 이다). 모든 명령은 그 디렉터리 안에서 실행한다. 메인 체크아웃의 파일은 읽기만 한다.
- 기준 브랜치는 `main` 이다(`master` 없음). commit 메시지 번호 대역은 **`20261122-200` 부터 1씩**(조정자는 001~099, WebAudio 작업자는 100~199).
- **이 저장소는 아직 Maven 을 쓰지 않으며 P23(Maven 전환)은 사용자 요청으로 맨 마지막이다.** 따라서 모듈 분리는 Maven 없이 **디렉터리 + 기존 sh/bat 스크립트**로 하고, P23 에서 각 디렉터리를 그대로 Maven 모듈로 옮길 수 있게 만든다. Maven POM 은 이번에 만들지 않는다.
- 빌드/시험은 JDK 6u45 로만(`build.sh`, `test.sh`, `scripts/pkgtest.sh`, `scripts/snaptest.sh`). 전체 회귀는 수십 분이므로 단계마다 필요한 범위를 돌리고, 마지막에 **분리 전과 같은 통과/실패/건너뜀 수**(분리 전 기준: 조정자가 마지막 전체 회귀 결과를 알려 준다. 모르면 `main` 의 HEAD 를 먼저 같은 방법으로 돌려 기준 수를 기록한다)로 끝나는지 전체 회귀를 한 번 돌린다.
- 이 작업은 `build.sh`, `test.sh`, `scripts/**`, `tools/sourcecheck`, `tools/uicheck` 등을 **고쳐야 한다**. 이번 작업에 한해 허용한다(공통 규칙의 "조정자만" 예외). 단 `AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md` 는 여전히 조정자 몫이다(필요한 문장은 `docs/progress/engine.md` 의 "조정자 요청"에 적는다).

## 현재 상태(2026-11-22 조정자 확인)
- 엔진 핵심 패키지: `js`(루트), `js.ast`, `js.parser`, `js.interp`, `js.builtins`(하위 `meta` 포함), `js.date`, `js.num`, `js.regexp`, `js.text`. 이들은 브라우저 패키지(`core`, `dom`, `css`, `html`, `layout`, `render`, `net`, `storage`, `media`, `content`, `download`, `SwingWebPanel`, `js.host`, `js.dom`, `js.worker`)나 AWT/Swing 을 import 하지 않는다(grep 으로 확인함. 작업 시작 시 다시 확인한다).
- 콘솔 출력 경계는 `js.ConsoleHook`(문자열 수준/텍스트)과 P24 에서 추가한 `js.ConsoleRecord`/`ConsoleRecordHook`(구조화된 기록: 메서드, 수준, 인수 문자열, 호출 위치)이다. 엔진 단독 사용자도 `JsRealm.setConsole` 로 받는다.
- 호출 위치 추적: `JsRealm.setCallSite/getCallSourceName/getCallLine/getCallColumn`(인터프리터가 기본 함수 호출 직전에 기록).
- 엔진 시험은 `tests/src/org/duckdns/hjow/swingwebpanel/js/{interp,builtins,date,num,regexp,text,parser,ast}/**` 등에 있고, `testing.*`(실행기, `Check`, 시험 서버)를 쓴다. 브라우저 시험은 `js/host`, `js/dom`, `js/worker` 등.

## 이번 작업의 결정 사항(바꾸려면 근거를 `docs/progress/engine.md` 에 적는다)
1. **분리 단위**: 엔진 모듈(JAR `swingwebpanel-js.jar`)과 브라우저 모듈(기존 `swingwebpanel.jar`, 엔진 클래스를 **포함하지 않고** 엔진 JAR 에 의존). 단, Maven 을 쓰지 않는 사용자를 위해 **두 모듈을 합친 단일 JAR**(`swingwebpanel-all.jar`, 기존 사용 방식 그대로) 도 빌드 스크립트가 만든다. `swingwebpanel.jar` 이름을 단일 JAR 로 둘지 분리 JAR 로 둘지는 P26-06 의 판단이다: 기존 사용자(JAR 하나)가 깨지지 않게 하는 쪽을 우선한다(예: `swingwebpanel.jar` = 합친 것, `swingwebpanel-js.jar` = 엔진만).
2. **패키지 이름**: 엔진 패키지 이름(`org.duckdns.hjow.swingwebpanel.js...`)은 **바꾸지 않는다**(바꾸면 브라우저 코드 수백 개와 시험 전체가 영향을 받는다). 영향과 이유를 기록한다.
3. **소스 디렉터리**: 엔진은 `engine/src/org/duckdns/hjow/swingwebpanel/js/...`(해당 패키지만 `git mv`), 엔진 시험은 `engine/tests/src/...`(엔진 클래스와 `testing` 실행기만 필요한 시험만), 브라우저는 기존 `src`, `tests/src`. P23 에서 `engine` 이 Maven 모듈 하나(`swingwebpanel-js`)가 된다. 디렉터리 이름을 바꾸려면 근거를 적는다.
4. **경계 강제**: 엔진 소스를 **엔진 클래스패스만으로 컴파일**하는 단계를 빌드 스크립트에 넣어 브라우저/AWT 의존이 생기면 컴파일이 깨지게 한다(AWT 는 JRE 에 있으므로 컴파일만으로는 못 막는다 → `tools/sourcecheck` 에 "엔진 소스는 `java.awt`/`javax.swing`/브라우저 패키지 import 금지" 규칙을 더하거나, 엔진 JAR 의 클래스 상수 풀을 훑는 검사). 어느 쪽이든 실패 사례(위반 소스를 넣으면 종료 코드 0 이 아님)를 확인한다.
5. **독립 모드 전역**: ECMAScript 표준 내장은 전부, `console`(훅으로), `print` 는 시험용. `window`/`document`/DOM/`fetch`/Storage 는 없다. 타이머(`setTimeout` 등)/`TextEncoder`/`URL`/`structuredClone` 은 이번에 **켜지 않는 것이 기본**이다(호스트 API 이므로). 켜는 선택지를 만들지는 근거와 함께 결정한다(만든다면 이벤트 루프를 누가 돌리는지 문서화).
6. **Worker 의 범용 부분과 `ModuleLoader`**: `ModuleLoader`/`ModuleGraph` 는 엔진 루트에 이미 있어 엔진으로 간다. `js.worker` 는 DOM/브라우저 의존이 있으므로 브라우저에 남긴다.

## 일의 순서
1. **P26-01**: 경계 재확인(grep), 위 결정의 확정, `docs/progress/engine.md` 설계 절 작성, `git mv` 로 엔진 패키지와 엔진 시험을 옮기고 스크립트(`build.sh`/`bat`, `test.sh`/`bat`, `scripts/pkgtest.sh`, `scripts/snaptest.sh`, `examples/run.*`, `tools/*` 의 경로 참조, `.classpath`)를 갱신한다. 옮긴 직후 전체 컴파일과 엔진·브라우저 시험이 이전과 같은 결과인지 확인하고 **commit** 한다(작은 단위).
2. **P26-02**: 독립 공개 API. `JsRealm` 이 이미 비슷한 것을 가졌으니 읽고, 브라우저 없이 쓰는 **파사드**(이름 예: `org.duckdns.hjow.swingwebpanel.js.JsEngine` 와 값 변환 규칙)를 설계·구현한다: Script/Module 평가, 전역 변수 읽기/쓰기, JavaScript 함수 호출, Java↔JavaScript 값 변환(문자열/숫자/불리언/null·undefined/배열/객체/함수), 예외 변환(`JsException`, SyntaxError 위치), 실행 제한(`ExecutionLimits`)과 중단, `JobQueue` 소비, 콘솔 훅, 모듈 해석 훅, 스레드 모델(realm 하나는 한 번에 한 스레드). **자동으로 Java 클래스/리플렉션/파일/프로세스를 노출하지 않는다**(호스트가 명시적으로 등록한 함수/객체만). 영어+한국어 Javadoc 과 사용 예.
3. **P26-03**: 독립 모드 전역 범위(결정 5)와 문서.
4. **P26-04**: `javax.script` 제공자(JSR 223): `ScriptEngineFactory`, `ScriptEngine`(`AbstractScriptEngine` 상속 가능), `Compilable`/`CompiledScript`, `Invocable`(`invokeFunction`/`invokeMethod`/`getInterface`: `java.lang.reflect.Proxy`), `ScriptContext` 의 ENGINE/GLOBAL 범위 `Bindings` 와 JavaScript 전역 연결, `Reader` 입력, `getWriter`/`getErrorWriter` 와 `console.log`/`console.error` 연결, 파일 이름·줄·열이 있는 `ScriptException`, `ScriptEngine.FILENAME` 등 표준 키, 팩토리 `getParameter`(`THREADING` 포함)/`getMethodCallSyntax`/`getOutputStatement`/`getProgram`. 엔진 JAR 의 `META-INF/services/javax.script.ScriptEngineFactory` 로 등록(JAR 만들 때 `META-INF` 를 포함하도록 스크립트 수정).
5. **P26-05**: 이름 충돌과 금지 규칙 정리. JRE 6~7 의 Rhino 와 JRE 8~14 의 Nashorn 이 `js`/`javascript`/`JavaScript`/`ECMAScript` 등으로 등록돼 있어 `getEngineByName("javascript")` 가 어느 엔진인지 보장되지 않는다. 고유 이름 `swingwebpanel-js` 를 반드시 등록하고, 일반 이름/MIME/확장자의 등록 여부와 한계를 문서화한다. 시험은 반환된 엔진의 클래스가 이 프로젝트의 것임을 확인하고, **Rhino/Nashorn 으로 평가된 결과를 합격으로 인정하지 않는다**. `AGENTS.md` P00-09 와 `docs/ARCHITECTURE.md` 1.1절의 "다른 엔진/`ScriptEngineManager` 우회 사용 금지"는 유지하되 자체 엔진을 `javax.script` 제공자로 공개하는 것은 허용됨을 명시한다(ARCHITECTURE 문구는 "조정자 요청"에 적는다). `tools/sourcecheck` 의 `javax.script` 금지 규칙은 **제공자 구현 패키지와 그 시험**에만 예외를 두도록 고치고, 제품 코드의 `ScriptEngineManager` 사용 금지는 유지한다. 규칙을 바꾼 뒤 위반 사례(예외 밖에서 `ScriptEngineManager`)가 여전히 잡히는지 확인한다.
6. **P26-06**: 브라우저 컴포넌트가 엔진의 공개 API 만 쓰도록 정리하고(필요하면 엔진 내부 클래스 접근을 점검), 기존 `SwingWebPanel` 동작·성능·공개 API 가 바뀌지 않게 한다. JAR 배포 형태(결정 1)를 스크립트로 구현하고, 제품 JAR 을 검사하는 시험(`PackagedJarTest` 류)과 문서를 갱신한다.
7. **P26-07**: 검증(AGENTS.md 본문대로). (1) 엔진 JAR 만 클래스패스에 두고 `java.awt.headless=true` 로 ES6 회귀 시험 전체와 독립 API 시험을 JRE 6 에서 통과, (2) `ScriptEngineManager` 로 고유 이름을 조회해 `eval`, `Bindings`, `Compilable`, `Invocable`, `getInterface`, `ScriptException` 위치, Writer 연결, 엔진 인스턴스 분리, 스레드 정책을 **JRE 6(JDK 6u45)** 과 **Nashorn 이 있는 JRE 8(`C:\Program Files\Java\jdk1.8.0_202`)** 에서 확인하고 다른 엔진이 선택되지 않음을 검증, (3) 브라우저 모듈 전체 회귀가 분리 전과 같은 수로 끝남, (4) 엔진 모듈이 브라우저/AWT/Swing 을 참조하지 않음을 자동 검사. `README.md` 에 독립 엔진 사용법과 `javax.script` 예제를 추가하고 `examples/` 에 엔진 단독/`javax.script` 예제(JDK 6 으로 컴파일되는 것)를 만든다.

## 정직하게 기록할 것
- JRE 6u45 에서 `javax.script` 는 존재하지만(JSR 223 은 Java 6 부터) 내장 Rhino 가 같이 있다. 이름 충돌 시험은 반드시 두 JRE 에서 따로 결과를 적는다.
- 옮긴 파일 수, 바뀐 스크립트/도구, 실행한 명령과 실제 결과(통과/실패/건너뜀), 분리 전 기준 수, 남은 한계, 조정자 요청(AGENTS/PROGRESS/FEATURE_MATRIX/ARCHITECTURE 에 넣을 문장)을 `docs/progress/engine.md` 에 적는다. 체크박스 후보만 제안한다.
