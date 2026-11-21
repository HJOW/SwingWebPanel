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

## 조정자 요청
(작업이 끝나면 AGENTS.md / PROGRESS.md / FEATURE_MATRIX.md / ARCHITECTURE.md 에 넣을 문장을 여기에 모은다.)

## 체크박스 후보
(P26-01~07 이 끝나면 적는다.)
