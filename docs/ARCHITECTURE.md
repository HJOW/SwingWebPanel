| 페이지 | 메시지 포트와 큐(`js.host.MessagingApi`), 구조화 복제 결과(`js.host.structured`, 불변) | 포트와 큐는 페이지의 `JsRealm` 객체이고 전달은 페이지 스케줄러 작업이다(`PageRuntime.close()` 의 스케줄러 정리와 realm 해제로 사라짐, 전역 등록 없음). 전용 해제 훅과 수거 시험은 없다(작업자 보고 기준, P21-04 에서 확인) | 해제 | 해제 | 해제 | 큐 10000 한도 |
# SwingWebPanel 구조 문서

작성 시작: 2026-11-16 (P00-01, P00-09, P02-01, P02-02)

## 1. 목표와 대원칙

Java Swing 에서 쓰는 웹 브라우저 컴포넌트 `org.duckdns.hjow.swingwebpanel.SwingWebPanel` (`JPanel` 상속, `Scrollable` 구현)을
외부 라이브러리 없이 순수 Java 로 구현한다. HTML 파서, DOM, CSS 처리기, 레이아웃/렌더링 엔진,
JavaScript(ES2026 및 이전 버전 ES 호환; 2026-11-25 ES6에서 목표 상향) 엔진을 모두 자체 개발한다. Java 6 이상, JRE 6 에서 동작해야 한다.

### 1.1 사용하지 않는 것 (P00-09)

다음은 엔진/브라우저의 **대체 수단으로 사용하지 않는다**. 제품, 테스트, 예제, 도구 어디에도 의존성으로 넣지 않는다.

| 금지 대상 | 비고 |
| --- | --- |
| Rhino, Nashorn, GraalJS, 그 밖의 JavaScript 엔진 | JRE 에 내장된 `sun.org.mozilla.javascript.*` 포함 |
| 다른 엔진을 `javax.script.ScriptEngineManager` 로 호출하는 것 | 내장 Rhino/Nashorn 우회 사용 금지. 단 **이 프로젝트의 자체 엔진을 `javax.script` 제공자로 공개하는 것은 허용**한다: `js.script` 패키지(와 그 시험, `examples/**/script` 예제)만 `javax.script` 를 쓸 수 있고(`tools/sourcecheck` 의 `forbidden-engine` 규칙이 강제) 나머지 제품 코드의 `ScriptEngineManager` 사용은 금지다. 엔진은 고유 이름 `swingwebpanel-js` 로만 등록한다(P26-05) |
| JavaFX WebView, JCEF, 외부/OS 브라우저 실행 | `Desktop.browse`, `Runtime.exec`, `ProcessBuilder` 로 브라우저 호출 금지 |
| `javax.swing.JEditorPane`/`HTMLEditorKit` 등 Swing 내장 HTML 렌더러 | 실제 HTML/CSS 렌더링 위임 금지 |
| 네이티브 코드(C/C++), JNI(`native`, `System.loadLibrary`), JNA, `.dll/.so/.dylib` | 도구 포함 |
| `com.sun.*`, `sun.*` 패키지 | `com.sun.net.httpserver` 는 테스트에서도 금지 |
| OS 코덱, 외부 실행 파일로 미디어 디코딩 | 미디어 디코더는 순수 Java |
| 외부 라이브러리(JAR) | JRE 표준 공개 API 만 사용 |

### 1.2 사용할 수 있는 것

JRE 표준 **공개 API**(`java.*`, `javax.*`)는 허용한다: Java2D(`Graphics2D`, `TextLayout`, `Font`), `ImageIO`,
Java Sound(`javax.sound.sampled`), JSSE(`HttpsURLConnection`, `SSLContext`), `java.net`, `java.util.regex` 등.
그 API 를 구현하는 JRE 내부는 금지 대상이 아니다. JRE 가 제공하지 않는 부분(웹 글꼴 디코딩, WebP/AVIF/MP3/AAC/Vorbis 등)은 순수 Java 로 직접 구현한다.

### 1.3 Java 6 언어 규칙

람다, 다이아몬드(`<>`), try-with-resources, `switch` 문자열, 멀티 catch, NIO.2(`java.nio.file`),
`java.nio.charset.StandardCharsets`, `java.util.Objects`, Java 7+ API 를 쓰지 않는다. 컴파일은
`javac -encoding UTF-8 -source 1.6 -target 1.6` (JDK 6). JavaScript 엔진의 ES6~ES2026 처리를 Java 8 이후 문법으로 대신 구현하지 않는다.

## 2. 디렉터리와 패키지 (P02-01)

```
AGENTS.md                    프로젝트 지침과 개발 계획
pom.xml                      부모 POM과 Maven 모듈 목록
docs/                        진행 기록, 기능 표, 구조, API 계약, 오프라인 규격서(es2026/, es2022/, ES2025 PDF)
engine/src/main/java/         JavaScript 엔진 소스
engine/src/main/resources/    javax.script 제공자 등록 파일
engine/src/test/              엔진 시험 java/와 resources/. 공용 시험 실행기는 java/ 아래 testing.* 패키지
browser/src/main/java/        Swing 브라우저 소스
browser/src/main/resources/   브라우저 제품 리소스
browser/src/test/             브라우저 시험 java/와 resources/
examples/src/main/            예제 java/와 resources/
tools/<도구>/                 개발 보조 도구(제품 reactor와 분리, Java 6)
<모듈>/target/                Maven 생성물
build/                        도구 작업 자료
```

주 패키지는 `org.duckdns.hjow.swingwebpanel` 이다(2026-11-19 에 `swingwebpanel` 에서 변경). 아래 표의 패키지는 모두 이 이름을 접두사로 한다.
User-Agent 기본값 `swingwebpanel/1.0`, JAR 이름 `swingwebpanel.jar`, 스레드 이름(`swingwebpanel-…`)과 임시 디렉터리 이름은 패키지명이 아니므로 바꾸지 않았다.

| 패키지 | 책임 |
| --- | --- |
| `org.duckdns.hjow.swingwebpanel` | 공개 진입점 `SwingWebPanel` |
| `org.duckdns.hjow.swingwebpanel.core` | 컴포넌트 상태: `BrowserSession`(컴포넌트별 설정/쿠키/캐시/세션 저장소), 이동 제어기, 이력, 상태/진행률 모델, EDT 호출 도우미, 수명 토큰 |
| `org.duckdns.hjow.swingwebpanel.net` | URL 처리, HTTP/HTTPS 요청기, 쿠키 저장소, 오리진, FTP 클라이언트, 클래스패스 로더, 리소스 응답/캐시 |
| `org.duckdns.hjow.swingwebpanel.html` | 인코딩 판정, HTML 토큰화기, 트리 생성기, 문자 참조 표 |
| `org.duckdns.hjow.swingwebpanel.dom` | Document/Element/Text 등 DOM 노드, 컬렉션, 이벤트(`Event`, `EventTarget`), 변경 통지 |
| `org.duckdns.hjow.swingwebpanel.css` | CSS 토큰화/파서, 스타일시트, 선택자 일치기, 계산 스타일, 미디어 질의 |
| `org.duckdns.hjow.swingwebpanel.layout` | 레이아웃 트리(`LayoutBox`), 블록/인라인/표/플렉스 등 레이아웃 알고리즘, 글꼴 측정 |
| `org.duckdns.hjow.swingwebpanel.render` | 그림 명령(`DisplayList`), Java2D 렌더러, 좌표 변환, hit testing |
| `org.duckdns.hjow.swingwebpanel.js` | JavaScript 값/객체/렉서/파서/인터프리터/내장 객체(**엔진 모듈 `engine/`**: 루트와 `ast parser interp builtins date num regexp text embed script`), 브라우저 모듈의 `js.host`(페이지 호스트·이벤트 루프·웹 API), `js.dom`(DOM 바인딩), `js.worker`(Worker) |
| `org.duckdns.hjow.swingwebpanel.storage` | `StorageEntry`, 오리진별 `StorageArea`, 프로세스 공용 localStorage 파일 저장소, 컴포넌트별 sessionStorage |
| `org.duckdns.hjow.swingwebpanel.media` | 이미지/오디오 디코더, 미디어 요소 상태, SVG, 오디오 재생, **Web Audio 그래프 엔진 `media.webaudio`(+`.dsp`)** |
| `org.duckdns.hjow.swingwebpanel.content` | MIME 별 콘텐츠 처리기(plain/markdown/xhtml), 오류 페이지 생성기 |
| `org.duckdns.hjow.swingwebpanel.download` | 다운로드 판정, 다운로드 관리자, Swing 다운로드 팝업 |

클래스 하나에 여러 엔진을 몰아넣지 않는다. 패키지 의존 방향: `core → (net, content, html, dom, css, layout, render, js, storage, media, download)`,
`js ↔ dom` 은 `js.dom` 바인딩 계층으로만 연결한다. `dom`/`html`/`css`/`layout`/`net` 은 `js` 에 의존하지 않는다.

### 2.1 모듈 구성 (P26, 2026-11-22)

- **엔진 모듈 `engine/`**: `org.duckdns.hjow.swingwebpanel.js` 아래 루트 클래스와 `ast parser interp builtins date num regexp text embed script`. 브라우저, AWT/Swing, 네트워크, 파일에 의존하지 않는다. 브라우저 없이 `js.embed.JsEngine` 으로 쓰거나 `js.script`(JSR 223, 고유 이름 `swingwebpanel-js`)로 쓴다. 경계는 (1) 빈 클래스패스/소스패스 컴파일, (2) `tools/sourcecheck` 의 `engine-boundary` 규칙, (3) 클래스 상수 풀 시험, (4) AWT 없는 엔진 단계 시험으로 강제한다. 패키지 이름은 바꾸지 않았다(두 모듈의 패키지는 겹치지 않는다).
- **브라우저 모듈 `browser/`**에는 나머지 패키지와 `js.host`/`js.dom`/`js.worker`가 있다. 컴파일된 엔진 클래스의 공개 API를 사용한다.
- 의존 방향은 예제 → 브라우저 → 엔진이다. Maven 전환 전후 경로는 [Maven 전환 기록](progress/maven.md)에 있다.
- 시험 실행기(`testing.TestRunner/Check/...`)는 엔진 시험 소스에 있고 브라우저가 `test` 범위의 test JAR로 사용한다.

ECMA-262 제17판의 내장 객체, 모듈과 공유 메모리 자료형은 엔진 모듈에서 구현한다. 브라우저 모듈은 `StructuredClone`에서 수신 렐름의 프로토타입과 버퍼·뷰의 별칭을 보존하고, `SharedMemoryScope`에서 문서·Worker·AudioWorklet의 오리진·격리·에이전트 클러스터를 검사한다. `RealmExecution`은 스크립트·모듈 키와 렐름을 실행 범위에 기록하며 HTML의 Promise 콜백은 등록 시점의 활성 스크립트와 incumbent 설정을 보존한다. 실행 범위가 끝나면 해당 스레드의 이전 값을 복원한다.

`tools/unicodegen`이 Unicode 18 UCD로 `UnicodePropertyData`를 만든다. 렉서·정규식은 `UnicodeProperties`와 `UnicodeCase`를 사용하며, 문자열 정규화와 정준 동치 비교는 `UnicodeNormalization`의 데이터를 공유한다. 엔진은 동일한 데이터로 JDK 6에서도 네 정규화 형식을 적용하며, 지역화 기본 형식과 호스트 예외는 [기능 표 7절](FEATURE_MATRIX.md#7-ecmascript-es2026-목표-및-이전-버전-호환-p00-04-p28-p29)에 기록한다.

## 3. 책임 분리와 데이터 흐름 (P02-02)

| 자료형 | 패키지 | 책임 |
| --- | --- | --- |
| `ResourceResponse` | net | 응답 코드, 헤더, MIME, 문자셋, 압축 해제된 본문 바이트, 최종 URL, 오리진, 오류 여부 |
| `PageSnapshot` | core | 확정된 페이지의 URL, 수신 원문(String), MIME, 기준 URL, 문서(DOM) 참조, 이력 복원 정보. 한 번에 교체되는 불변에 가까운 스냅샷 |
| `Document` | dom | DOM 트리와 이벤트 대상. 원문을 덮어쓰지 않는다 |
| `ComputedStyle` | css | 요소 하나의 계산 값(상속/구체성/우선순위 적용 후) |
| `LayoutBox` | layout | 위치/크기/상자 모델/줄 상자. DOM 과 분리된 트리이며 DOM 노드 참조를 보관 |
| `DisplayList` | render | 그림 명령 목록(배경, 경계선, 텍스트, 이미지, 클리핑, 변환). EDT 에서 `Graphics2D` 로 재생 |
| `JsRealm` | js | 페이지마다 하나인 전역 객체/내장 객체 집합/이벤트 루프 큐/DOM 바인딩 |

데이터 흐름:

```
setPage(url) ─▶ NavigationController(요청 번호 N) ─▶ net.ResourceFetcher ─▶ ResourceResponse
                                                                    │
                       content.ContentClassifier(MIME 분기) ◀──────────┘
                                   │ html 문자열
                                   ▼
          html.Tokenizer/TreeBuilder ─▶ dom.Document ──▶ css.StyleResolver ─▶ ComputedStyle
                                   │                                         │
                          js.JsRealm(스크립트 실행)                           ▼
                                   │                            layout.LayoutEngine ─▶ LayoutBox
                                   └────────── DOM 변경 통지 ─────────────────┤
                                                                              ▼
                                                              render.Painter ─▶ DisplayList ─▶ Graphics2D(EDT)
```

원문(`getContent()`)은 `PageSnapshot` 에 문자열로 보관하며 DOM 이 바뀌어도 변하지 않는다.

## 4. 스레드 모델

- 모든 공개 메서드는 EDT/일반 스레드에서 호출 가능하다. 입력 검증은 호출 스레드에서 즉시 수행한다.
- 화면 상태 변경(`revalidate`, `repaint`, 속성 변경 통지, 하이퍼링크 통지)은 EDT 에서 수행한다.
- 네트워크 로딩, 파싱, 스크립트 실행은 컴포넌트당 하나의 **문서 소유 스레드**(직렬 실행기)에서 처리한다. DOM 과 JsRealm 은 이 스레드만 수정한다.
- 렌더러는 변경되지 않는 스냅샷(`DisplayList`)을 EDT 로 전달해 그린다. 이동 요청마다 요청 번호를 올려 오래된 완료 콜백을 폐기한다.
- 조회 메서드는 마지막 확정 상태의 복사본/불변 값을 반환한다.

## 5. 빌드와 검증 구조 (P02)

| 스크립트 | 설명 |
| --- | --- |
| `mvn package` / `build.sh` / `build.bat` | 제품 JAR 세 개와 예제 모듈을 빌드한다. 두 스크립트는 시험 컴파일과 실행을 생략하는 Maven 래퍼다. |
| `mvn test` / `mvn verify` / `test.sh` / `test.bat` | JDK 6의 `java`로 엔진(Headless)과 브라우저의 `TestRunner`를 실행한다. 각 모듈이 통과·실패·건너뜀 수를 출력하며 시험 실패는 Maven 실패로 전파된다. |
| `mvn -pl engine test` | 엔진 제품과 엔진 시험만 클래스패스에 두고 실행한다. |
| `tools/<도구>/build.*`, `run.*` | 각 도구의 빌드/실행 |

Maven 실행 JDK는 8 이상이다. Maven이 컴파일과 시험에 쓰는 JDK 6은 `JAVA6_HOME`으로 지정한다.
기본 경로는 Windows의 `C:\Program Files\Java\jdk1.6.0_45`와 Linux의 `/usr/lib/jvm/java-6-openjdk`다.
JAR 내용을 읽는 회귀 시험을 위해 제품 JAR는 `test` 전에 만든다. 합본에는 엔진·브라우저 JAR의 제품 항목을 담는다.
bat 는 MS949(현재는 ASCII 만 사용), sh/java/md 는 UTF-8 이다.

## 6. 도구 목록 (`tools/`)

| 도구 | 용도 | 문서 |
| --- | --- | --- |
| `tools/sourcecheck` | 인코딩(UTF-8/MS949), 금지 API/네이티브/Java 6 위반, 영어+한국어 Javadoc, 반복문 한국어 주석 검사(P02-07/08/09, P22-08). `tools/sourcecheck/run.sh` 또는 `run.bat` | `tools/sourcecheck/AGENTS.md`, `README.md` |
| `tools/webfetch` | 규격/Chromium 소스 등 웹 자료 조회. 출처 URL, 조회 날짜, JRE 를 stderr 에 기록. JDK 6u45 의 HTTPS 는 실패하므로 `WEBFETCH_JAVA_HOME` 에 새 JRE 를 지정해 실행 | `tools/webfetch/AGENTS.md`, `README.md` |
| `tools/entitygen` | HTML 문자 참조와 인코딩 라벨 JSON에서 Java 상수 표 생성(P07-01/04). 입력 옆 메타데이터의 출처와 조회일을 생성 소스에 기록 | `tools/entitygen/AGENTS.md`, `README.md` |
| `tools/vogen` | Unicode `VerticalOrientation.txt`(UAX #50)에서 세로쓰기 글자 방향 표를 생성(P10-03-e). 입력 판본 Unicode 18.0.0, 조회 2026-11-17. 사용법은 `tools/vogen/AGENTS.md`, `README.md` |
| `tools/uicheck` | 실제 Swing 창(JFrame+JScrollPane+SwingWebPanel 또는 예제 창)을 띄우고 단계(열기/스크롤/클릭/뒤·앞/새로고침/폐기)를 실행하며 `java.awt.Robot` 으로 실제 화면 스크린샷을 저장(P09-08-b-2, P22-10). 기본은 AWT 이벤트 큐로 클릭하고 `--realmouse` 는 OS 마우스를 쓴다. 사용법은 `tools/uicheck/AGENTS.md`, `README.md`, 결과는 `docs/progress/uicheck.md` |
| `tools/brotlidict` | RFC 7932 부록 A/B 의 텍스트에서 Brotli 정적 사전(122,784바이트)·변환 표·문맥 조회표를 읽고 RFC 가 적은 CRC-32 와 대조한 뒤 `media.font.BrotliData` 를 생성(P10-08-e) | `tools/brotlidict/AGENTS.md`, `README.md` |
| `tools/hrtfgen` | MIT KEMAR compact 자료(`tools/webfetch` 로 받은 `compact.zip`)를 귀마다 최소 위상 커널 + 지연으로 바꿔 제품 자원 `media/webaudio/dsp/kemar-compact.hrtf` 를 생성(P25 A2). 저자 표기 조건과 원본 URL·조회일은 도구 문서와 README 에 있음 | `tools/hrtfgen/AGENTS.md`, `README.md` |
| `tools/netstormparse` | 외부 NetstormReborn 경로의 webpack 번들, `debug.html` inline module, `dist` 밖 모든 ES 모듈과 상대 static import를 JDK 6의 자체 JavaScript 파서로 검사한다(P28-02-f). 자산은 저장소에 복사하지 않는다 | `tools/netstormparse/AGENTS.md`, `README.md` |
| `tools/unicodegen` | Unicode 18.0.0 UCD의 속성 구간, 정확한 별칭과 대소문자 매핑을 `js.text.UnicodePropertyData` 소스로 생성한다(P29-02-g). 렉서와 정규식이 `UnicodeProperties`를 공유하며 문자열 대소문자 변환과 정규식이 `UnicodeCase`를 사용한다. 이모지 전용 속성에는 EXC-15가 적용된다. | `tools/unicodegen/AGENTS.md`, `README.md` |

(도구가 추가되면 이 표에 갱신한다.)

## 7. 참고 구현 기록 (Chromium)

판정 기준은 P00 에서 고정한 규격 판본이다. Chromium 은 구조/오류 복구/경계 사례의 참고 구현이다.
참고한 Chromium 파일 경로와 리비전(커밋 해시 또는 조회 날짜)을 이 절 또는 기능 표에 남긴다.

| 기능 | Chromium 경로 | 리비전/조회일 | 차용한 정도 |
| --- | --- | --- | --- |
| (아직 없음) | | | |

## 8. 명시적 예외 (상단 요구사항 기반)

비디오 재생, 3D 가속/WebGL/WebGPU, 외부 플러그인 실행은 지원하지 않는다. 자세한 동작은 `FEATURE_MATRIX.md` 의 EXC 항목과 `API_CONTRACT.md` 를 따른다.

## 9. core 패키지 구성 (P03, 2026-11-16)

| 클래스 | 책임 |
| --- | --- |
| `org.duckdns.hjow.swingwebpanel.SwingWebPanel` | 공개 컴포넌트(JPanel, Scrollable). 모든 상태/동작은 `BrowserSession` 에 위임하는 얇은 껍데기. 리스너 정리, `configureSsl`(protected) 확장 지점 |
| `core.BrowserSession` | 컴포넌트 하나의 전체 상태(UA, 헤더, JS 설정, 확정 페이지, 이력, 상태/진행률). 이동 시작/확정/취소, clean/dispose |
| `core.NavigationController` | 요청 번호 발급, 읽기 스레드 풀(이동마다 독립 데몬 스레드), **단일 문서 스레드**(확정과 이후 DOM/스크립트 작업을 직렬 실행), 취소 |
| `core.PageLoader` / `DefaultPageLoader` | 이동 요청을 `PageSnapshot` 으로 바꾸는 전략. 기본 로더는 about:blank/about:text 만 처리하고 네트워크는 정직한 "미구현" 오류 페이지, 금지 스킴은 "지원하지 않는 주소" 오류 페이지 |
| `core.PageSnapshot`, `PageKind`, `PageDocument` | 확정 페이지(URL/원문/MIME/기준 URL/종류/제목/상태 코드)와 살아 있는 객체(DOM 등)의 해제 연결 |
| `core.History` | 이력 목록과 위치(분기 삭제, 복사본) |
| `core.NavigationRequest`, `NavigationType`, `ProgressSink` | 로더 입력(요청 번호, 설정 스냅샷, 취소 토큰, 진행률 보고) |
| `core.SessionListener`, `HyperlinkDispatcher`, `ConsoleDispatcher`, `PageConsole`, `ViewMetrics` | EDT 통지, 호스트 하이퍼링크 리스너 등록부, 호스트 콘솔 리스너 등록부(EDT 전달·대기 1000개 상한)와 페이지별 콘솔 연결(P24), 레이아웃이 알려 주는 크기(Scrollable 정책) |

스레드: 호출 스레드(EDT 포함) → 요청 번호 발급 → 읽기 스레드에서 `PageLoader.load` → **문서 스레드에서 `commit`**(요청 번호가 여전히 현재일 때만 이력/현재 페이지 교체) → 통지는 EDT.
오래된 완료는 폐기되며 그 `PageSnapshot.dispose()` 가 호출된다. 읽기 스레드와 문서 스레드는 5초 유휴 후 종료하는 데몬 스레드이고 dispose 는 즉시 `shutdownNow` 한다.

## 10. JDK 6 외의 JDK 사용 시 (P02-04)

`SWP_ALLOW_NON_JDK6=1` 로 다른 JDK 를 명시하면 빌드는 경고와 함께 진행되지만 **JRE 6 검증이 아니다.** JDK 8 로 `-source 1.6 -target 1.6` 컴파일하면
`bootstrap class path not set` 경고가 나며 Java 7/8 API 를 호출해도 컴파일될 수 있다. 다른 JDK 로 API 호환을 검사하려면
`-bootclasspath <JDK6>/jre/lib/rt.jar` 를 지정해야 하며, 그래도 최종 근거는 JDK 6 컴파일과 JRE 6 실행이다. JDK 25 는 `-source 6` 을 지원하지 않는다.

## 11. dom 패키지 구성 (P06, 2026-11-16)

DOM Standard(Living, 2026-11-15) 기준의 자체 구현. 모든 노드는 소유 `Document` 의 문서 스레드에서만 쓴다.

| 클래스 | 책임 |
| --- | --- |
| `Node`(+`EventTarget`) | 트리 연결(이중 연결 리스트), 삽입/교체/제거/adopt/복제 알고리즘과 유효성 규칙, textContent, normalize, 위치 비교. 변경은 공통 경로를 지나며 문서의 변경 버전을 올리고 `MutationRecord` 를 보고 |
| `Document`, `DocumentFragment`, `DocumentType`, `Element`, `Attr`, `Text`, `CDATASection`, `Comment`, `ProcessingInstruction`, `CharacterData` | 노드 종류. HTML/XML 이름 규칙(HTML 문서의 HTML 요소는 소문자 이름/대문자 tagName), 네임스페이스 검증은 `Namespaces` |
| `ElementCollection(s)`, `ChildNodeList`, `StaticNodeList`, `ClassList`, `NamedAttributeMap` | 라이브 컬렉션(문서 변경 버전으로 캐시 갱신), 정적 목록, DOMTokenList, NamedNodeMap |
| `Event`, `CustomEvent`, `EventListener`, `EventTarget` | 캡처→대상→버블 전달, once/passive, 중복 무시, 전달 중 추가/제거, 리스너 예외 격리(`ListenerErrorHandler`), 경로는 전달 시작 시 고정, 문서→window(load 제외) |
| `DomMutationListener`, `MutationRecord` | 레이아웃/스타일 무효화와 MutationObserver 의 기반. `connected` 플래그로 분리된 하위 트리 변경을 구분 |
| `SelectorEngine`, `MarkupSupport`, `ElementFactory` | 다른 계층이 설치하는 서비스(선택자=P08-09, 마크업 파서/직렬화기=P07-06/P20-04, HTML 요소 클래스=P20-01). 설치 전에는 NotSupportedError |
| `Element.templateContents` | template 내용은 일반 자식 트리 밖에서 요소가 직접 소유한다. `ParserNodeData`는 접근을 위임하며 전역 표에 조각을 저장하지 않는다. deep clone/import 및 adopt는 중첩 내용에도 적용된다 |
| `DomException` | 표준 오류 이름과 옛 코드 |

## 12. storage 패키지 구성 (P16, 2026-11-16)

HTML Living Standard의 Web Storage 절(조회일 2026-11-16, 판본은 계획의 기준 고정 규칙에 따라 FEATURE_MATRIX WEB-04에 기록)을 따르는 자체 구현.

| 클래스 | 책임 |
| --- | --- |
| `StorageEntry` | Java 조회용 읽기 전용 항목(origin, key, value). 값 동등성 |
| `WebStorage` | 한 오리진의 `Storage` 인터페이스(length/key/getItem/setItem/removeItem/clear). JS 노출은 P15에서 이 인터페이스를 감싼다 |
| `StorageArea` | 오리진 하나의 삽입 순서 보존 키-값 모음. 쿼터(키+값 코드 단위 수, 기본 2,621,440)와 항목 수 제한 |
| `SessionStorage` | 컴포넌트 하나의 sessionStorage. `BrowserSession` 의 일반 필드이며 정적 참조가 없어 컴포넌트와 함께 수거된다 |
| `LocalStorageManager` | 프로세스 공용 localStorage. 임시 파일 하나가 원본이며 매 연산 전에 파일 변화를 확인해 다시 읽는다 |
| `StorageFile` | 파일 형식 `SWPLSTOR` + 버전 1 + 길이 접두 UTF-8 + CRC-32. Java 직렬화를 쓰지 않으며 길이·제한·체크섬을 검증 |
| `StorageListener`, `StorageListeners`, `StorageEvent` | 같은 오리진의 다른 문서에 변경을 알리는 통지(변경한 문서 제외). localStorage 쪽 리스너는 약한 참조 |
| `StorageLimits`, `StorageQuotaException` | 제한 상수와 초과 오류(`QuotaExceededError` 로 JS에 전달 예정) |

**제한**: 오리진당 2,621,440 코드 단위(키+값, 5 MiB 문자 수), 전체 16,777,216 코드 단위, 오리진 1,000개, 오리진당 항목 100,000개. 초과하면 `StorageQuotaException` 이며 저장소는 변하지 않는다. 같은 값을 다시 쓰면 통지하지 않는다.

**오리진 정책**: 오리진은 스키마/호스트/유효 포트(기본 포트 정규화)이다. `about:blank`, `about:text`(setContent), 차단 스키마의 오리진은 불투명이며 `forOrigin` 이 SecurityError(`DomException`)를 던진다. `classpath://` 문서는 모두 하나의 오리진(`classpath://`)을 공유하며 웹 오리진과는 분리된다.

**파일 정책**: 파일 이름은 `<tmp>/swingwebpanel/localstorage-<pid>-<시각>-<난수>.dat`로 프로세스마다 다르다. 파일 부재는 빈 저장소이며 관리자는 파일을 감시하거나 복구하지 않는다(다음 읽기에서 부재를 처리). 해석 실패(손상, 잘림, 알 수 없는 버전, 체크섬 불일치, 제한 초과)는 저장소를 초기화하고 빈 파일로 교체한다. 쓰기는 `.tmp` 에 쓴 뒤 `renameTo` 로 교체하며, 기존 파일 위로 이름 바꾸기가 실패하는 JRE 6/Windows 에서는 `.bak` 로 옮긴 뒤 교체하고 실패 시 되돌린다(JDK 6u45/Windows 에서 실제로 이 경로가 실행되어 시험으로 확인). 쓰기 자체가 실패하면 성공할 때까지 메모리 저장소가 기준이다.

**clean/dispose 와 공용 localStorage**: `clean()` 과 `dispose()` 는 이 컴포넌트의 sessionStorage 뿐 아니라 프로세스 공용 localStorage 전체를 비운다(AGENTS.md "clean" 요구). 같은 프로세스의 다른 컴포넌트에도 영향을 주며, 다른 컴포넌트는 다음 조회에서 빈 저장소를 보고 오리진마다 clear 이벤트를 통지받는다.

**수명**: `LocalStorageManager` 는 `BrowserSession`/문서를 강한 참조로 붙잡지 않는다(리스너 약한 참조). `StorageIntegrationTest.testComponentIsCollectable` 이 공용 관리자가 살아 있어도 dispose 된 컴포넌트가 수거됨을 확인한다(GC 시점 자체는 보장하지 않으며 30초까지 반복 요청).

**남은 연결(P15)**: JS의 `Storage` 객체(이름 있는 프로퍼티 접근), `window.localStorage/sessionStorage`, 문서로의 `storage` 이벤트 전달은 JS–DOM 연결 단계에서 `WebStorage`/`StorageListener` 위에 붙인다.

## 13. HTTP 캐시 (P19-03/04/06, 2026-11-16)

`net.HttpCache` 는 컴포넌트 하나의 사설 캐시(RFC 9111 부분집합)이며 `ResourceFetcher.setCache` 로 설치한다(`SwingWebPanel.createNetworkLoader` 가 컴포넌트마다 새 캐시를 만든다). 컴포넌트 사이에 응답을 공유하지 않는다.

| 항목 | 동작 |
| --- | --- |
| 저장 대상 | GET, 메모리로 받은 상태 200/203/204/300/404/405/410/414/501, 본문 8 MiB 이하, 리디렉션·HTTP 대체 재시도·실패·스트리밍 응답은 제외 |
| 저장 금지 | `Cache-Control: no-store`, `Vary: *`, `Vary: Cookie`(쿠키는 캐시가 비교할 수 없음). `Set-Cookie` 헤더는 저장/재생하지 않음 |
| 신선도 | `max-age` → `Expires − Date` → 휴리스틱(`Last-Modified` 이후 시간의 10 %, 최대 24 h). 나이는 RFC 9111 4.2.3(Age 헤더, 응답 지연, 거주 시간). `no-cache` 는 항상 재검증 |
| 재검증 | 오래된 사본에 `ETag`/`Last-Modified` 가 있으면 `If-None-Match`/`If-Modified-Since` 조건부 요청. 304 는 저장 사본의 헤더를 갱신(Content-* 제외)하고 저장 본문을 상태 200 으로 반환(`ResourceResponse.isRevalidated()`) |
| Vary | 이 계층이 보내는 요청 헤더(User-Agent, Referer, 추가 헤더)로 변형을 고른다 |
| 모드 | `DEFAULT`(위 규칙), `FORCE_RELOAD`(저장본 건너뜀 + `Cache-Control: no-cache`/`Pragma: no-cache`, 새 응답으로 교체), `PREFER_CACHE`(이력 이동: 오래된 사본도 사용, 없으면 네트워크) |
| 무효화 | 성공한(<400) POST/PUT/DELETE/PATCH 는 요청 URL 과 같은 오리진의 `Location`/`Content-Location` 사본을 지운다 |
| 용량 | 기본 32 MiB, 항목 8 MiB, LRU 제거. 요청 자체의 `Cache-Control: no-cache/no-store/max-age=0`, `Pragma: no-cache`(추가 헤더)는 조회를 건너뛴다 |
| 본문 처리기 | 저장본에도 `ResponseHandler` 가 결정한다(거부 → `FailureKind.IO`, 스트림 → 스트리밍 응답). 다운로드 판정이 캐시 적중에도 같다 |
| 수명 | `core.CacheOwner.clearCache()` 를 `NetworkPageLoader` 가 구현하고 `BrowserSession.clean/dispose` 가 호출한다 |

한계: 리디렉션을 거친 응답은 저장하지 않는다(중간 리디렉션 응답이 보이지 않아 영구/임시 구분 불가). `s-maxage`, `stale-while-revalidate`, `stale-if-error`, `Range`, `immutable` 최적화는 지원하지 않는다. 디코딩 데이터/레이아웃 캐시(P19-05)는 해당 단계에서 `CacheOwner` 로 추가한다.

## 14. 페이지 파이프라인에 DOM 연결 (P07 통합, 2026-11-16)

`SwingWebPanel.createNetworkLoader()` 는 `ParsingPageLoader(NetworkPageLoader)` 를 만든다. `NetworkPageLoader` 가 수신 원문을 `PageSnapshot` 으로 만들면(HTML/오류 본문은 `html.EncodingSniffer` 로 디코딩, 그 밖의 형식은 `core.TextDecoder`),
`ParsingPageLoader` 가 MIME 에 따라 `HtmlPageDocument`(DOM `Document` 보관)를 붙인다: `text/html` → `HtmlParser`, `text/markdown` → `MarkdownConverter.toHtmlDocument` 결과를 `HtmlParser` 로(스냅샷 원문은 Markdown 유지),
`text/plain` → `pre` 하나의 문서(마크업 해석 없음), 그 밖의 형식은 문서 없음. 문서에는 `HtmlMarkupSupport` 와 `CssSelectorEngine` 이 설치된다. DOM 은 로딩 스레드에서 만들어 스냅샷과 함께 문서 스레드로 넘어가며
확정 전에는 아무도 접근하지 않는다. `getContent()` 는 항상 스냅샷의 수신 원문이다. 사용자 정의 `PageLoader`(protected 생성자)는 감싸지 않는다.

## 15. Swing 화면 연결 (P09-07-a, 2026-11-16)

`core.PageView`는 확정된 HtmlPageDocument에 CssEngine을 설치하고 `BrowserSession.submitDocumentTask`의 단일 문서 큐에서 `PageRenderer.render`를 실행한다.
내부/style 속성과 외부 CSS/이미지를 같은 요청 계층으로 연결한다(P09-07-b).

EDT는 불변 RenderedPage를 그리며 ViewMetrics를 갱신하고 revalidate/repaint한다. JScrollPane이 컴포넌트를 이동하므로 별도 스크롤 변환은 적용하지 않는다.
폭은 JViewport의 extent(부모가 뷰포트가 아니면 컴포넌트 크기)를 사용한다. 같은 extent는 무시하고 변경 요청은 합친다.
연결된 DOM 변경만 다시 배치한다. 페이지 객체 동일성은 문서 작업 전/후, EDT 콜백, paint에서 확인하며 이전 관찰자와 CssEngine은 문서 큐에서 해제한다.
문서 실행기와 리소스 데몬 작업자는 5초 유휴 후 종료한다. 네트워크·디코딩은 문서 큐나 EDT에서 실행하지 않는다.

`PageResources`는 페이지별 ImageSource/StyleSheetLoader이다. 미요청 주소는 제한 실행기(컴포넌트당 4개 작업자/큐 256)에 넣고 대기 상태를 반환한다.
CSS 완료는 시트 수집을 반복하여 새로 발견된 @import도 가져온다. 이미지 완료는 자연 크기와 DisplayList를 재계산한다.
페이지당 서로 다른 주소 256개/디코딩 결과 64 MiB를 한도로 보관한다. 이미지 디코더 한도는 입력 8 MiB, 프레임 8 Mi 픽셀/전체 16 Mi 픽셀/256프레임이다.
`PageRequestContext`는 이동 시 UA/추가 헤더/force만 복사하고 요청·세션·이전 페이지를 저장하지 않는다. 표준 PageLoader와 뷰는 하나의 ResourceFetcher/쿠키/캐시/SSL 훅을 공유한다.
리소스는 페이지별 CancelToken으로 교체·clean·dispose 시 취소하며 취소된 응답은 HTTP 캐시에 다시 저장하지 않는다. 디코딩 결과는 화면 교체 후 참조가 없어지면 수거된다.
오디오, DOM load/error, srcset 및 종합 진행률은 각각 P18/P19 후속이다.

`core.PageInput`은 컴포넌트의 내부 MouseListener/MouseMotionListener다. 좌표는 JScrollPane 안에서도 컴포넌트=문서 좌표이며 hit test와 DOM 전달은 문서 큐에서 수행한다.
Swing 링크 이벤트는 EDT에 전달하고 기본 이동은 별도로 BrowserSession에 요청한다. navigationCount와 페이지 동일성으로 리스너가 직접 시작한 이동을 감지한다.
호버 조회는 최신 포인터 번호로 합치고 지연된 결과를 무시한다. BrowserSession의 hoverText는 실제 로딩 상태를 덮어쓰지 않는 표시용 오버레이다.

## 16. 과거 HTML 표현 힌트와 인라인 표시 (P07-07-b-1~4)

`StatefulPresentationalHints`는 기존 `PresentationalHints`의 선택적 확장이다. StyleResolver의 MatchContext를 받아 동적 선택자 조회를 기록한다.
기본 HtmlPresentationalHints는 body의 link/vlink/alink를 :link/:visited/:active 링크 색으로 적용하며 일반 작성자 CSS보다 앞서는 구체성 0 힌트다.
상태 변경은 기존 stateChanged 경로를 쓰고 body 링크 색/본문 교체는 문서 전체를 무효화한다(본문 밖 링크에도 적용되기 때문).
규격은 HTML5 REC 2014-10-28 §10.3.2, 조회 2026-11-16이다. 실제 포인터/방문 상태 공급은 P11에서 연결한다.

body 여백은 marginwidth/height가 존재하면 그 값만 시도하고 없을 때 개별 여백 속성을 사용한다. 잘못된 값은 힌트를 내보내지 않아 UA 8px 또는 CSS 재정의를 따른다.
HTML 정수 접두사(ASCII 공백/선택적 부호/숫자)를 유한 CSS double 픽셀 값으로 처리한다. 컨테이너 프레임 속성 상속은 P20-02에 남는다.

`center`의 텍스트 정렬은 속성이 없는 요소에도 `HtmlPresentationalHints`가 `text-align:center`를 제공한다(P07-07-b-4).
HTML5 2014 §10.3.3의 표현 힌트 출처를 적용하여 일반 사용자 CSS보다 높고 일반 작성자 CSS보다 낮으며,
중요 사용자 CSS가 우선한다. 자손 블록의 사용 여백을 조정하는 align-descendants 동작은 별도 P07-07-b-5이다.

`wbr`는 글리프 대신 `InlineItem.Kind.OPPORTUNITY`로 전달된다. `BoxBuilder`가 wbr 자체의 계산 스타일을 보존하고,
`InlineLayouter`는 `white-space`가 허용할 때 이전 단위를 끝내고 다음 내용 앞의 줄바꿈 기회를 보관한다.
다음 내용이 nowrap이어도 그 앞에서는 필요할 때만 나눌 수 있으며, 빈 인라인 가장자리는 기회를 소모하지 않는다.
이 항목은 폭·장식·줄 상자를 스스로 만들지 않고, 내재 너비 측정과 `::first-line` 탐색도 같은 경로를 쓴다.
`nobr wbr{white-space:normal}`과 `blink{text-decoration:blink}`는 UA 시트의 일반 규칙이며 작성자 CSS로 재정의할 수 있다.
blink의 그림은 기능 표의 정책에 따라 정적 텍스트이고 별도 깜박임 타이머를 만들지 않는다.

## 17. 페이지 타이머의 문서 큐 인계 (P15-08-a)

`core.DocumentScheduler`는 PageScripts의 Scheduler 계약을 구현한다. 페이지 런타임이 파싱 전 생성하고 확정 후 activate(snapshot), 교체·clean·dispose 시 close()한다(런타임 설치는 P15-08-b).
파싱 중 post는 기한만 보관하며 활성화 뒤 원래 기한순/동일 기한 등록순으로 문서 큐에 전달한다. 타이머·입력·렌더링이 같은 문서 큐를 사용하여 DOM 동시 접근을 피한다.

프로세스 공용 ScheduledThreadPoolExecutor는 `swingwebpanel-clock` 데몬 하나다. 시계와 문서 큐의 전달 객체는 페이지 소유 Pending 핸들을 약하게만 참조한다.
핸들은 콜백·세션을 보관하지만 페이지 객체 그래프 안에만 강하게 남는다. ReferenceQueue 정리기가 1초마다 소유자가 수거된 먼 기한의 future도 제거한다.
명시적 취소는 JRE 6 공개 API의 cancel(false)/remove로 future를 제거한다. close는 문서 스레드/EDT의 종료를 기다리지 않는다.
Pending 상한은 기존 JS 타이머 한도와 같은 100000이며, 시간 공급원은 내부 진단에서만 교체 가능하다. 이 기반만으로 SwingWebPanel의 스크립트 실행이 연결된 것은 아니다.

`PageScripts.requestDisposal()`는 페이지 잠금을 얻지 않고 disposed/실행 취소를 표시한다(P15-08-b-1). EDT에서는 이 요청만 수행하고 완전한 dispose는 정리 작업자에서 수행해야 한다.

창의 `globalThis`와 프레임의 `contentWindow`는 브라우징 문맥별 `WindowProxy`를 사용한다(P29-02-h). `RealmExecution`이 기록한 실행 realm으로 `FrameApi`가 접근 오리진을 판정하고 현재 전역 객체 또는 제한된 창을 선택한다. 창 메시지에는 호출 스크립트 쪽 프록시를 실어 보낸다. 프레임 요소의 `load`는 현재 이동의 문서가 설치된 뒤에 발생한다.
`core.PageRuntime`이 이 연결을 수행한다(P15-08-b-2). 표준 컴포넌트는 생성 중 `ParsingPageLoader.bindScripts`로 세션/요청 계층/쿠키를 한 번 전달한다.
HTML·Markdown의 DOM을 만든 뒤 파싱 전에 PageScripts를 설치하고 scriptReady를 전달한다. 파싱 중 DOM은 로더 소유이며 확정 뒤 문서 큐로 인계한다.
HtmlPageDocument는 런타임을 소유하며 BrowserSession.commit이 확정한 정확한 스냅샷으로 스케줄러를 활성화한다. 수신 원문은 변경하지 않는다.

세션은 현재 및 파싱 중 런타임을 추적한다. JS 비활성화는 등록된 모든 호스트에 영구 중단을 요청하고, 새 이동/clean/dispose의 취소 토큰도 같은 close로 연결한다.
close는 잠금 없이 실행 취소를 표시하고 스케줄러/스크립트 리소스를 닫는다. 최종 PageScripts.dispose는 파서 소유권 반환을 기다린 뒤 별도 데몬에서 수행한다.
이는 파서 대기 목록과 해제의 동시 접근 및 종료된 문서 큐에서 해제 작업 유실을 방지한다. 공용 localStorage 리스너는 기존 약한 참조 정책을 유지한다.

외부 script는 컴포넌트의 ResourceFetcher, UA/추가 헤더/쿠키/SSL/오리진/캐시·force 설정을 공유한다. 런타임은 NavigationRequest나 과거 스냅샷을 보관하지 않는다.
문서 인코딩 상속, module MIME/CORS, CSS 차단·async 순서, CSS/배치 조회, location/history 기본 동작은 P15-08의 남은 통합 범위다.

P15-08-b-3-a에서 `PageInput → HtmlPageDocument.dispatchInput → PageScripts.dispatchTrustedEvent`를 연결했다.
실제 입력은 isTrusted를 유지하며 하나의 작업 끝에 Promise 작업과 MutationObserver를 처리한다. DOM 취소와 입력 중 새 이동을 확인한 뒤 링크 기본 동작을 실행한다.
스크립트 중단은 DomBridge의 volatile 영구 콜백 차단을 먼저 세운다. 최종 해제 전 직접 DOM 이벤트가 와도 인라인·함수·handleEvent 객체를 실행하지 않는다.
중단된 페이지의 Java DOM 리스너/기본 링크는 계속 동작하며, 새 페이지의 DomBridge에서만 JS 실행을 다시 허용한다. CSS/배치 조회와 공통 이동의 나머지 연결은 후속이다.

P15-08-b-3-b-1에서 확정 뒤 PageRuntime의 getStyleResolver/getElementBounds를 동일 PageView의 CSS/배치 결과에 연결했다.
조회는 문서 큐에서 연결된 변경을 먼저 배치한다. 문서 확정 시 preparePage를 먼저 수행하여 EDT 통지보다 타이머가 빨라도 표현 상태가 준비된다.
EDT pageConfirmed는 같은 스냅샷의 상태를 재생성하지 않는다. 아직 파서가 소유하는 문서에는 이 경로를 적용하지 않는다(b-3-b-2).

SwingWebPanel은 부모 JViewport의 ChangeListener로 extent/위치를 EDT에서 복사한다. PageView는 위치를 새 Point로 게시하며 문서 큐는 이를 읽어
border box 합집합의 문서 좌표를 뷰포트 좌표로 바꾼다. 부모 변경 시 이전 관찰자를 제거하고 dispose 후에도 EDT에서 외부 뷰포트 연결을 해제한다.
확정 후 조회는 네트워크를 기다리지 않으며 PageResources의 완료 상태를 사용한다. 외부 CSS가 스크립트를 차단하는 파싱 규칙은 별도 후속 범위다.

P15-08-b-3-b-2-a에서 가변 표현 상태를 `core.PagePresentation`으로 옮겼다. bound ParsingPageLoader가 파싱 전 생성하며,
HtmlPageDocument가 소유하고 PageRuntime의 파싱 중 조회 및 PageView.State의 확정 뒤 배치가 같은 CssEngine/PageResources를 사용한다.
자체 mutation/resource 관찰은 dirty/sheetsChanged만 표시하고, 확정 뒤에만 설치하는 콜백이 문서 큐의 뷰 갱신을 예약한다.
PagePresentation은 변경/크기가 같으면 불변 배치를 재사용한다. 파서 소유 중 사각형은 새 페이지의 스크롤 원점, 확정 뒤에는 복사한 뷰포트 위치를 사용한다.

스크립트 중단과 표현 폐기를 구분한다. JS 비활성화는 타이머/스크립트만 중단하므로 지연 CSS/이미지와 배치가 계속 동작한다.
이동 토큰 취소/문서 폐기는 표현을 retire하며 리소스를 즉시 닫는다. `core.PageCleanup`의 유휴 후 종료하는 공용 데몬이
파서 반환 장벽과 표현 잠금 뒤 DOM/CSS 관찰자를 해제한다. 스크립트 실행을 먼저 중단한 뒤 표현을 해제하는 순서를 지킨다.
외부 스타일시트 대기를 위한 contextual loader와 일반/defer 스크립트 차단은 다음 하위 항목이다.

P15-08-b-3-b-2-b에서 style/link 파서 콜백이 시트를 비동기로 시작하고 초기 차단 조건을 기록한다. 선택적
`css.cssom.ContextualStyleSheetLoader`는 소유 요소의 조건과 import media를 자식 로더에 상속한다. 기존 일반 StyleSheetLoader 구현의 동작은 바꾸지 않는다.
PagePresentation.prepareParserScript는 일반 파서/외부 defer 스크립트 직전에만 해당 범위를 동기 수집한다. PageResources.awaitStyleSheet는
로더 스레드에서 완료/오류 또는 별도 스크립트 취소를 기다린다. JS 비활성화는 대기만 풀고, 페이지 이동/폐기는 리소스도 취소한다.
async/동적 스크립트, 동적으로 생성한 스타일 및 비활성/미일치 미디어는 파서 CSS 차단을 얻지 않는다. 확정 뒤 렌더링은 계속 비동기 상태만 조회한다.

P15-04의 기존 async 목록 구현은 준비 시 실행과 병렬 가져오기를 충족하지 않아 재개방했다. 다음 단계는 취소 가능한 외부 소스 요청 상태,
파서 대기/EOF와 문서 큐 사이의 실행 소유권, DOMContentLoaded/load의 별도 완료 조건이다. ES2015 모듈 언어와 브라우저 module MIME/CORS/credentials를 구분하여 추적한다.

## 비동기 웹 요청 API와 서비스 계층 (P20-08)

`js.host.WebRequestSource`(`HostEnvironment.getWebRequestSource()`)는 페이지가 요청(`WebRequest`: URL, 메서드, 작성자 헤더, 본문, 자격 증명/요청/캐시/리디렉션 모드, 참조 정책)을 주고 컴포넌트가 요청 스레드에서 실행하여 `WebResponse` 를 돌려주는 계약이다. `core.PageWebRequests` 가 이를 구현하며 문서·스크립트·스타일·이미지와 같은 `ResourceFetcher` 를 쓰므로 User-Agent, 추가 헤더, SSL 훅, HTTP 캐시, 쿠키 저장소, 스키마·크기 제한과 취소가 그대로 적용되고 동일 출처 정책과 CORS 응답 검사(`net.OriginPolicy`)를 집행한다. `js.host.web.WebApis` 가 페이지마다 하나 설치되어 XHR/fetch/AbortController/FormData/Blob/URL/TextEncoder 등을 만들고, 결과는 페이지 `Scheduler` 의 페이지 작업으로 한 건씩 실행한다. 페이지 교체/폐기/스크립트 중단은 `requestDisposal()` → 요청 취소로 이어져 늦은 응답이 스크립트를 실행하지 못한다. `net.ResourceRequest` 에 `sameOriginCredentialsOnly`, `corsHeadersChecked` 플래그가 추가되었다. 교차 오리진 사전 요청은 JRE 6 `HttpURLConnection` 의 제한(`Origin` 을 보낼 수 없음) 때문에 아직 지원하지 않는다(P20-08-c).

## 외부 스크립트 병렬 로딩과 소유 스레드 전달 (P15-04-a/b-1)

`js.host.ScriptLoads`는 한 페이지의 외부 스크립트 가져오기 모음이다. 파서가 스크립트를 발굴하는 즉시(async/defer/module/동적 삽입 포함) 데몬 스레드에서 가져오기를 시작한다.
배경 가져오기는 동시 6개(`MAX_BACKGROUND_FETCHES`)로 제한하고 파서를 막는 스크립트는 제한을 받지 않는다. 완료는 DOM 소유 스레드에만 전달한다.
파싱 중에는 로더 스레드가 스크립트 경계와 파서 차단 스크립트 대기 중에 `pollReady`로 끝난 async를 가져와 실행하고, `completeParsing`이 DOMContentLoaded를 보낸 뒤
`Scheduler`(문서 큐)가 있으면 `setNotifier`로 전달을 문서 큐에 넘긴다(페이지는 먼저 확정되고 남은 async와 load는 큐에서 처리). 큐가 없는 호스트는 로더 스레드가 기다린다.
`PageScripts.requestDisposal`/`dispose`는 모음을 닫아 늦은 응답과 대기를 모두 무효화한다. 동적으로 삽입한 script는 mutation 통지에서 준비하며 `script.async=false`는 삽입 순서 목록으로 실행한다.
실행 깊이(`depth`)로 중첩 스크립트가 마이크로태스크 검사점을 일찍 돌리지 않게 하며, 파서 제어는 바깥 스크립트가 소유한 `document.write` 텍스트를 보존한다.

## 캐시 계층과 확정 뒤 로딩 진행률 (P19-05, P19-08, 2026-11-17)

저장 대상마다 범위, 용량, 제거 정책이 다르며 서로 섞지 않는다.

| 계층 | 담는 것 | 범위 | 용량 | 제거 정책 | 구현 |
| --- | --- | --- | --- | --- | --- |
| 수신 응답(HTTP 캐시) | 주 문서·스타일시트·이미지·스크립트·XHR 응답의 인코딩된 바이트(오디오는 재생 때 같은 요청 계층으로 받는다) | 컴포넌트(다른 컴포넌트와 공유하지 않음) | 32 MiB, 항목 8 MiB | LRU, `clean()`/`dispose()` 와 강제 새로고침은 비움/건너뜀 | `net.HttpCache`(13절) |
| 디코딩 리소스 | 한 페이지가 쓰는 스타일시트 텍스트와 디코딩한 이미지 프레임 | 페이지(요청 주소별 한 항목, 렌더러와 스크립트 이미지 로더가 같은 항목을 씀) | 256 항목, 보관 64 MiB(초과분은 실패로 끝남, 읽거나 시작하지 않음) | 페이지 안에서는 제거하지 않음. 페이지를 닫으면(`close()`) 취소하고 전부 놓음 | `core.PageResources` |
| 레이아웃·그림 명령 | 마지막 배치 결과(`RenderedPage`)와 DOM 에 연결된 레이아웃 상자 | 페이지 | 마지막 하나 | 다시 배치할 때 교체, 페이지를 닫으면 놓음 | `core.PagePresentation` |
| 글꼴·글리프 측정 | `Font` 와 측정값 | 프로세스(정적) | 각 512 항목 | LRU | `layout.FontResolver` |

- 이미 응답 객체나 디코딩 상태로 넘어간 데이터는 불변 배열/값이므로 HTTP 캐시가 항목을 쫓아내도 화면이 참조 중인 데이터는 그대로 안전하다. 페이지 교체 뒤에는 표현 상태·DOM 이 어디에서도 도달되지 않으며(이력에는 `withoutDocument()` 사본만 남는다) 수거될 수 있다 — `core.PageReleaseIntegrationTest`(컴포넌트를 살려 둔 채 페이지만 교체해도 표현 상태와 DOM 이 약한 참조에서 지워짐), `ComponentLeakIntegrationTest`(컴포넌트 전체).
- 디코딩된 오디오는 캐시하지 않는다. 오디오는 블록 단위로 디코딩해 출력 장치로 보내고 PCM 전체를 보관하지 않는다(P18-08).
- 한계: 한 페이지의 디코딩 리소스는 페이지가 열려 있는 동안 늘기만 한다(스크립트가 이미지 주소를 계속 바꾸면 256 항목 한도에서 새 주소가 실패한다). 항목별 LRU 로 바꾸려면 화면이 참조 중인 항목을 표시할 방법이 필요하다.

**확정 뒤 진행률(P19-08)**: 본문 수신(0~50, 길이를 모르면 -1) 뒤 페이지가 확정되면, 스타일시트·이미지·스크립트가 남을 수 있는 문서는 `Loading resources (n/m)`(50~94)로 이어 가고 모두 끝나면 `Done`(100)이 된다.
`PageResources` 가 시작/끝난 가져오기 수를 세고(실패도 끝난 것) `PageView.State` 가 배치할 때마다 `BrowserSession.pageLoadProgress` 로 보고하며, 스크립트 쪽은 window `load` 가 그 리소스와 스크립트를 기다린 뒤(`HostEnvironment.hasPendingResources/pageLoaded`) 발생한다.
새 이동이 이미 진행 중이거나 페이지가 교체되었으면 이전 페이지의 보고는 무시되며(`nav.currentId() != confirmedRequestId`), 진행률은 줄어들지 않고, 끝난 페이지의 늦은 리소스(스크립트가 나중에 만든 이미지)는 진행률을 되돌리지 않는다. 오류 페이지는 확정 때 `Error N`(100)으로 끝난다.

## 소켓 기반 HTTP 연결과 CORS 사전 요청 (P20-08-c, 2026-11-17)

JRE 의 `HttpURLConnection` 은 `Origin`, `Access-Control-Request-*` 같은 제한 요청 헤더를 버리고 `PATCH` 를 거부한다. `net.SocketHttpClient`(요청 하나를 `Socket`/`SSLSocket` 으로 보내고 응답 머리와 본문 경계를 읽는 HTTP/1.1 클라이언트)와 이를 `HttpURLConnection`/`HttpsURLConnection` API 로 감싼 `SocketHttpURLConnection`/`SocketHttpsURLConnection` 이 그 일을 한다. `HttpFetcher.open` 이 `Origin` 을 싣는 요청에만, 그리고 JVM 프록시가 적용되지 않을 때만 이 연결을 고르므로 쿠키·리디렉션·압축·제한·취소·SSL 확장 지점(`configureSsl`)은 하나의 코드 경로에 남는다. `net.HostnameMatcher` 는 인증서의 호스트 이름 확인(RFC 2818)을 맡는다. CORS 사전 요청은 `HttpFetcher.preflightDenial`(요청 구성)과 `OriginPolicy.checkPreflight`/`PreflightGrant`(응답 판정과 허락)로 나뉘고, 허락 캐시는 `HttpFetcher` 인스턴스 안에 있다(컴포넌트별, 상한 256).

## 웹 글꼴과 CSS 애니메이션의 소유(P10-05, P10-07, 2026-11-17)

- **웹 글꼴**: 글꼴 바이트는 페이지 소유 리소스(`PageResources` 항목, 파일당 8 MiB, 32 면 한도)이고 디코딩한 `java.awt.Font` 는 페이지 소유 `layout.WebFontSet` 이 가지며 페이지 교체/폐기 때 함께 해제된다. WOFF 1.0 은 `layout.Woff` 가 순수 Java 로 복원하고 JRE 6 의 `Font.createFont` 는 임시 파일을 만들며 글꼴이 수거되거나 JVM 이 끝날 때 지운다. 가져오는 동안은 `src` 의 다음 family 를 쓰다가 글꼴이 도착하면 다시 배치한다.
- **애니메이션**: `PagePresentation` 이 `css.anim.AnimationEngine`(문서 스레드, `StyleResolver` 훅)과 프레임 타이머(데몬 스레드 하나, 애니메이션이 있을 때만, 페이지 `retire` 에서 취소)를 소유한다. 시계는 `AnimationClock` 으로 주입할 수 있어 시험은 수동 시계를 쓴다. 틱마다 계산 스타일이 바뀌어 전체 재배치가 일어난다(그리기만 다시 하는 경로는 없다).
- **Web Animations(2026-11-19, CSS-47)**: `css.anim` 에 Web Animations 1 모델이 있다: `EffectTiming`(시간 계산), `WebAnimation`(재생 상태 기계, `ready`/`finished`), `KeyframeEffectModel`(키프레임 효과와 합성 `replace/add/accumulate`), `WebAnimationSet`(문서 등록부, 교체 제거), `WebTimeline`, `Compositor`. `AnimationEngine` 이 이것을 소유하고 효과 스택을 CSS 전환 < CSS 애니메이션 < 스크립트 애니메이션 순서로 합성한다. 스크립트 연결은 새 패키지 `js.host.anim`(`WebAnimationsApi`, `KeyframeArguments`, `AnimationPage`)과 `js.host.AnimationHostImpl` 이다. Web Animations 의 갱신 단계는 `Timers` 의 애니메이션 프레임에서 `requestAnimationFrame` 콜백 직전에 같은 시각으로 실행되고, 스크립트 중지(이동·폐기·JavaScript 끄기)는 `WebAnimationSet.detach`(어느 스레드) → 문서 스레드의 `AnimationEngine.tick` 의 `flushStopped` 로 정리된다(이벤트 없이). 공유 파일의 가산 변경: `HostEnvironment.animationsChanged()`, `StyleResolver.getAnimator()`, `js.dom.DomInterfaces` 분류 한 줄, `Timers`(`scheduleFrame`/`setAnimationHook`), `WindowGlobals`(설치·중지).
- **CSS-M(2026-11-19, P10-08-h)**: 이미지 함수 `cross-fade()`/`image()`/`element()` 는 `render.GeneratedImages` 가 그린다(배경·`border-image`·마스크·바깥 목록 표식이 같은 경로; `element()` 는 참조 요소의 그리기 명령으로 이미지를 만들고 순환은 투명). `::highlight()` 는 `render.HighlightOverlay` 가 그리며 등록부는 문서 소유 `dom.Highlight`/`dom.HighlightRegistry`, 스크립트 연결은 `js.host.HighlightApi`(`CSS.highlights`). `::backdrop` 과 모달 `dialog` 는 `Document` 의 최상위 계층 목록을 쓴다. `@font-feature-values` 는 `css.cssom.FontFeatureValuesRule`, 웹 글꼴의 대체 글리프는 `layout.GlyphSubstitution`(GSUB lookup 1/3/7 을 읽어 `cmap` 을 다시 써서 `Font.createFont` 로 만든 대체 글꼴). `contain-intrinsic-*` 의 마지막으로 기억한 크기는 `Document` 의 등록부. Motion Path 의 SVG 도형 참조는 `layout.SvgShapePaths`, 경로 보간은 `css.values.ShapeInterpolation`(`css.anim.Interpolator` 의 `offset-path` 한 줄). 공유 파일의 가산 변경: `Prop.COUNT` 317(`contain-intrinsic-inline-size` 316, `block-size` 317), `js.host.PageScripts.onTaskEnd`(검사점 끝의 하이라이트 다시 그리기), `js.host.ElementStates`(dialog 최상위 계층).

## 자원 소유 목록과 정리 담당(P21-01, 2026-11-17)

정리가 일어나는 경우: **교체**(새 페이지가 확정되어 이전 페이지가 밀려남), **clean**(`SwingWebPanel.clean()`: `input.reset` → `downloads.cancelAll` → `session.clean`(진행 중 작업 취소, 빈 페이지, 이력 초기화, 캐시·sessionStorage·localStorage 비움) → `view.clear` → HTTP 캐시 비움), **dispose**(입력/키보드/다운로드/세션/뷰를 차례로 폐기하고 HTTP 캐시와 하이퍼링크 통지를 비우며 모든 사용자 리스너를 제거, 세션은 `nav.shutdown()` 으로 문서·읽기 스레드 풀을 닫고 헤더·쿠키·리스너를 잊음), **취소**(새 이동, `stop()`, 실패).
페이지 소유 자원은 모두 이동 요청의 `CancelToken` 에 매달린 훅으로 취소하고, DOM·JS 영역의 해제는 파싱 소유권이 돌아온 뒤 `core.PageCleanup` 의 단일 데몬 작업자(유휴 5초 후 종료)가 EDT 와 문서 큐 밖에서 한다.

| 소유자 | 자원 | 정리 담당(클래스) | 교체 | clean | dispose | 취소/실패 |
| --- | --- | --- | --- | --- | --- | --- |
| 페이지 | 주 문서 요청과 스트림 | `NavigationController`(요청 번호·`CancelToken`), `NetworkPageLoader`/`net.HttpFetcher` | 새 번호로 이전 요청 취소 | `session.clean` 이 취소 | `session.dispose` | 오래된 번호의 완료는 버려짐 |
| 페이지 | 하위 리소스(스타일시트·이미지·오디오) 요청과 디코딩 상태 | `PageResources`(`close()`: 취소, 항목·보관량 비움) ← `PagePresentation.retire()` | 이전 페이지 `retire` | 같음 | 같음 | 닫은 뒤의 완료는 게시하지 않음 |
| 페이지 | DOM 문서, CSS 엔진, 배치 결과(`RenderedPage`) | `HtmlPageDocument.dispose`, `PagePresentation.release()`(관찰자 제거·참조 null; `PageCleanup`) | `PageSnapshot.dispose` | 같음 | 같음 | 파싱 소유권 반환 뒤 해제 |
| 페이지 | `JsRealm`/`PageScripts`, 마이크로태스크·타이머 | `PageRuntime.close()`(즉시 `requestDisposal`, `DocumentScheduler.close`, 요청 `CancelToken` 취소, 세션 등록 해제) → `PageCleanup` 이 `scripts.dispose()` | 같음 | 같음 | 같음 | JavaScript 를 끄면 `close()` 와 진행률 재판정 |
| 페이지 | XMLHttpRequest/fetch 요청 | `PageWebRequests`(페이지 `CancelToken`) | 취소 | 취소 | 취소 | 늦은 응답은 스크립트를 실행하지 않음 |
| 페이지 | Canvas 2D 비트맵·컨텍스트·그라디언트·패턴(`js.host.canvas`) | `PageScripts.dispose()` → `CanvasBindings.dispose()`(표면 해제, 문서 리스너 제거, 요소는 약한 참조), 렌더러는 변경 없는 스냅샷(`render.CanvasSource`)만 읽음 | 해제 | 해제 | 해제 | `CanvasPixelsTest.testCollectedWithThePage` |
| 페이지 | Web Audio(`js.host.webaudio`): 실시간/오프라인 컨텍스트, 렌더링 스레드(`swingwebpanel-audio-render-N`, `swingwebpanel-audio-offline-N`), 디코딩 서비스(`swingwebpanel-audio-decode-N`), 오디오 출력 라인(`OutputRenderDriver` 가 구간마다 열고 닫음) | `WindowGlobals.cancelRequests()` → `WebAudioApi.dispose()` | 모두 닫음 | 모두 닫음 | 모두 닫음 | 늦은 결과는 버림(페이지 작업으로만 전달), 스크립트를 끈 페이지는 아무것도 만들지 않음 |
| 페이지 | 오디오 worklet(`js.host.webaudio.worklet`): `WorkletScope` 의 realm, worklet 스레드(`swingwebpanel-audio-worklet-N`), 작업 큐 | `WebAudioApi.unregister`/`dispose()` → `WorkletApi.contextClosed`/`dispose()` → `WorkletScope.dispose()` | 모두 닫음 | 모두 닫음 | 모두 닫음 | 컨텍스트 `close()`·오프라인 렌더링 완료 때도 끝남, 늦은 결과는 버림 |
| 페이지 | 폼 호스트, 편집기(Swing 자식), 선택 목록, 검증 말풍선 `javax.swing.Timer` | `PageFormHost.close()`, `ControlOverlays.clearLater()`(`PageView.closeActive`) | 제거 | 제거 | 제거 | 렌더링 관찰자는 약한 참조 |
| 페이지 | `<audio>` 플레이어(요소당 데몬 스레드, 오디오 라인) | `MediaElements`/`MediaPlayer`(`close`: stop/flush/close) | 닫음 | 닫음 | 닫음 | 버려진 플레이어는 약한 참조 리스너로 스스로 끝남 |
| 페이지 | 프레임(`iframe`/`frame`) | (P20-02 구현 뒤 추가) | - | - | - | - |
| 컴포넌트 | 이력 항목(`PageSnapshot` 사본, POST 데이터 4 MiB 까지) | `History`(`BrowserSession.resetToBlank`) | 앞쪽 항목 삭제 시 `dispose` | 하나로 초기화 | 초기화 | - |
| 컴포넌트 | HTTP 캐시(32 MiB LRU) | `net.HttpCache` ← `core.CacheOwner.clearCache`, `SwingWebPanel.fetcher.getCache().clear()` | 유지 | **비움** | 비움 | - |
| 컴포넌트 | sessionStorage | `BrowserSession` 의 필드(인스턴스) | 유지 | 비움 | 컴포넌트와 함께 수거 | 전역 맵에 등록하지 않음 |
| 프로세스 | localStorage 관리자 | `storage.LocalStorageManager`(리스너 약한 참조) | 유지 | **전체 비움**(다른 컴포넌트도 영향) | 비움 | 컴포넌트를 강하게 참조하지 않음 |
| 컴포넌트 | 다운로드 작업과 팝업 | `download.SwingDownloadHandler`(`cancelAll`, `dispose`) | 유지 | **취소·팝업 닫음**(완료된 파일은 지우지 않음) | 취소·닫음 | - |
| 컴포넌트 | 사용자 리스너(하이퍼링크·콘솔·PropertyChange 등), 내부 마우스/키보드/포커스 리스너 | `HyperlinkDispatcher.clear`, `ConsoleDispatcher.close`, `SwingWebPanel.removeAttachedListeners`, `PageInput.dispose`, `PageKeyboard.dispose` | 유지 | 유지(하이퍼링크 통지는 `removeHyperlinkEvents`) | **제거** | - |
| 컴포넌트 | 쿠키 저장소, 추가 헤더, User-Agent, JavaScript 설정 | `BrowserSession` | 유지 | **유지**(계약 5.x) | 잊음(`headers.clear`, `cookies.clear`) | - |
| 컴포넌트 | 스레드: 읽기 풀·문서 스레드(`NavigationController`), 뷰의 리소스 작업자(`PageView`, 유휴 5초 후 종료) | `NavigationController.shutdown`, `PageView.dispose` | 유휴 시 종료 | 유휴 시 종료 | 즉시 닫음 | 데몬 스레드 |
| 프로세스 | 정적 스레드: `DocumentScheduler` 시계, `PageCleanup`, `js.host.ThreadScheduler`, 글꼴 캐시 | 공용(컴포넌트·페이지를 강하게 붙잡지 않음, 유휴 종료/상한 있는 LRU) | - | - | - | `ComponentLeakIntegrationTest`, `PageReleaseIntegrationTest` 가 약한 참조로 확인 |

검증 근거: 교체 뒤 이전 페이지의 표현·DOM 해제(`PageReleaseIntegrationTest`), 쓰고 폐기한 컴포넌트 9개의 수거와 스레드 비누적(`ComponentLeakIntegrationTest`), clean/dispose 의 이력·저장소·원문·설정 상태(`LifecycleTest`, `StorageIntegrationTest`), 실제 컴포넌트에서의 clean/dispose 반복은 P21-04 에서 더 확인한다.

## 텍스트 선택, 복사, 페이지 확대 (P11-08, 2026-11-17)

선택 모델은 기존 DOM `Selection`/`Range` 하나이며 사용자 선택과 스크립트 선택을 모두 나타낸다(`dom.SelectionListener` 가 변경을 통지). 화면 계층은 문서 스레드에서 선택 기하를 계산하고 EDT 가 강조를 덧칠한다.

| 계층 | 클래스 | 책임 |
| --- | --- | --- |
| dom | `SelectionListener`, `ClipboardEvent`, `ClipboardData` | 선택 변경 통지, `copy` 이벤트와 `clipboardData` |
| js.dom | `ClipboardBindings` | `ClipboardEvent`, `DataTransfer` 의 `getData/setData/clearData/types` |
| render | `TextSelections`, `SelectionHighlight` | 선택 기하(점 → 캐럿 위치, 범위 → 강조 도형·복사 문자열, 낱말, 문단, 키보드 걸음)와 불변 강조 도형 |
| core | `PageSelection`, `PageZoom`, `ZoomControl`, `ClipboardSink` | 제스처와 `selectionchange`, 확대 범위와 단계, 클립보드 쓰기 |
| core | `PageInput`, `PageKeyboard`, `PageView`, `ControlOverlays` | 입력 좌표를 배율로 나눈 문서 좌표로 바꾸고, 뷰포트·히트 테스트·스크롤·폼 편집기를 같은 배율로 맞춘다 |
| SwingWebPanel | `setZoom/getZoom`, `PROPERTY_ZOOM`, `copyToClipboard`(protected) | 확장 API(계약 5.18) |

스레드: DOM·레이아웃 읽기는 문서 큐, 클립보드 쓰기·repaint·확대 적용은 EDT. 좌표: 컴포넌트 좌표 = 문서 좌표 × 배율, 스크롤은 `JScrollPane` 이 컴포넌트를 옮기므로 이미 반영된다. 자세한 규칙과 한계는 `docs/progress/textsel.md`.

## 과거 HTML 표·삽입 요소·호환 모드 규칙의 구성 (P07-07-b-7~b-10, 2026-11-17)

`css/cascade` 에 `LegacyAttributes`(속성 값 해석과 선언 출력 공용 도우미: 음이 아닌 정수, 차원 값, 키워드 맞춤, 구조로 표 찾기), `TableHints`(표·셀·행·열 표현 힌트), `QuirkHints`(호환 모드 힌트와 DOM 변경 시 캐시 무효화)가 있고 `css/props/QuirkyValues`(단위 없는 길이, # 없는 16진수 색)와 `css/values/ParseEnv` 의 호환 모드 플래그가 문서 시트/`@import`/style 속성 해석에 쓰인다. `UaStyleSheet.htmlOnly()` 는 HTML 문서 전용 UA 시트(표 안 form 숨김 등)이고 `layout.LayoutEngine.fillViewport` 는 호환 모드에서 html/body 를 뷰포트에 맞춘다. 규칙과 한계는 `docs/progress/legacy.md`.

## 컨테이너 질의, 중첩, 등록 속성, CSSOM 스크립트 연결 (P10-08 CSS-H, DOM-13, 2026-11-17)

레이아웃은 `ContainerSource` 를 구현한 스타일 공급자(`CssStyleSource`)에게 컨테이너 크기를 알리고 `usesContainerQueries()` 이면 최대 4 번 다시 배치한다(`LayoutEngine.layoutWithContainers`). `CounterStyleSource` 로 문서의 `@counter-style` 을 얻고, `LengthContext` 에 색 구성표와 컨테이너 크기가 실린다. `css/cssom` 에 `ContainerCondition`, `ScopeSpec`, `PropertyRule`, `CounterStyleRule` 이 있고 중첩 규칙은 `StyleSheetBuilder` 가 평평한 규칙 목록으로 편다(`Parent`).

스크립트용 CSSOM(DOM-13)은 자바 모델 `css/cssom/*`(`StyleSheet`, `CssRule` 계열, `MediaList`, `CssRuleList`, `RuleEditor`)를 `js/dom/{CssomInterfaces,CssomMembers,StyleObject}` 와 `js/host/{CssomBindings,CssNamespaceApi,CssFontApi}` 로 노출한다. 시트 객체는 문서 CSS 엔진이 쓰는 것과 같고 변경은 스타일 재계산으로 통지된다(`StyleSheet.changed`). `StyleSheet.isQuirks()` 는 시트를 호환 모드로 파싱했는지 알려 `insertRule` 이 같은 방식으로 파싱하게 한다. 규칙과 한계는 `docs/progress/cssom.md`, `docs/progress/cssg.md`.

## Range 화면 좌표와 Geometry 인터페이스 (P20-03, 2026-11-17)

`render/TextSelections.rects(Range)` 가 CSSOM View 의 규칙으로 범위의 사각형(요소의 border box, 텍스트의 글자 사각형)을 계산하고 `js/host/RangeGeometry` 가 합집합 규칙으로 `getBoundingClientRect/getClientRects` 를 만든다. `dom/{DomRectReadOnly,DomRect,DomPointReadOnly,DomPoint,DomRectList}` 와 `js/dom/GeometryBindings` 가 Geometry 인터페이스를 노출한다. 문서 큐가 렌더링을 반영한 뒤 뷰포트 좌표로 바꾸는 호스트 훅은 `HostEnvironment.getRangeRects/getRangeText/getElementRects` 이다. 자세한 내용은 `docs/progress/edit.md`.

## 편집, 끌어다 놓기와 요소 인터페이스 보강 (P20-06, P20-01, P20-10, 2026-11-17)

DOM 수준 편집 엔진은 `dom/edit/{Editor,UndoManager,TextUnits,EditingHosts}`(contenteditable/designMode/`execCommand`, `beforeinput`/`input`, 되돌리기)이고 JS 연결은 `js/dom/EditBindings`. 화면 입력과의 연결은 `core/PageEditing`(편집 호스트 안의 키·자르기·붙이기·끌어 놓기 삽입·조합 제어기), `core/PageComposition`(입력기 리스너와 질의), `core/PageDragDrop`(끌어다 놓기 상태 기계, 컴포넌트 안에서 흉내), `core/ClipboardSource`(붙여넣기 읽기 훅)이다. `dom/{DragEvent,DataTransferItem,DataTransferItemList}` 와 확장된 `ClipboardData` 가 `DataTransfer` 전체를 맡는다. 계약 5.19.

P20-01 에서 더한 것의 소유 영역: `js/host/LegacyWebApi`(`window.event`, `external`, 창 막대, `elementsFromPoint`, 폐기된 document 멤버, 네임스페이스 조회, `crypto`), `js/host/MicrodataApi` 와 `dom/{Microdata,PropertiesCollection,PropertyNodeList}`(마이크로데이터), `js/host/MetaRefresh`(`meta http-equiv=refresh`), `js/builtins/ConsoleBuiltins`(Console Standard), `render/InlineSvg`(인라인 `<svg>` 그리기), `render/ImageMaps`(클라이언트 이미지 맵 히트 테스트), `dom/NamespaceLookup`, `layout/Replaced` 와 `layout/BoxBuilder` 의 `object` 대체 내용 배치·`details` 숨김.

## CSS-I, 캔버스 확장, 서버 전송 이벤트·포인터·페이지 수명의 구성 (P10-08, P20-05-b, P20-13, 2026-11-17)

- **CSS(CSS-I, `docs/progress/cssg.md` 19~29절)**: `css.cascade.PageStyles` 가 `@page` 쪽 문맥의 계단식을 계산한다(쪽 상자는 배치하지 않는다). 그리기 컨텍스트(`PaintContext.surface`)가 그려지는 이미지를 알며,
  `backdrop-filter` 가 있는 목록만 `PagePainter` 가 읽을 수 있는 영역 이미지에 그린다. `StyleResolver.restyleWithCustom` 이 애니메이션 값을 가진 사용자 정의 속성으로 요소의 스타일을 다시 계산한다
  (애니메이션 엔진은 등록된 속성에 `CUSTOM_BASE + 순번` id 를 붙여 속성 코드를 재사용한다). `LayoutEngine.applyTransforms` 는 `MotionPath.has` 이면 오프셋 변환을 `transform` 과 합성한다.
  `layout.FontFeatures` 가 글꼴 기능 태그(`font-variant-*`, `font-feature-settings`)를 해석한다. 새 시험은 `css.cascade.PageRuleTest`, `css.RegisteredPropertyAnimationTest`, `css.ScrollPropertiesTest`,
  `layout.{FontFeaturesTest,HangingPunctuationLayoutTest,InitialLetterLayoutTest,MotionPathLayoutTest}`, `render.{BackdropFilterPaintTest,ControlColorSchemePaintTest,MaskBorderPaintTest,MotionPathPaintTest,TextEmphasisPaintTest}`.
- **캔버스(`docs/progress/canvas.md`)**: `js.host.canvas.OffscreenBindings`/`OffscreenCanvasValue`(`OffscreenCanvas`, `transferControlToOffscreen`), `ImageBitmapValue`/`BitmapOps`/`CanvasPromises`(`ImageBitmap`, `createImageBitmap` 의 비동기 결과),
  `ContextSettings`(컨텍스트 생성 옵션)와 `js.host.web.BlobBytes`(Blob 의 바이트 접근), `CanvasGradient` 의 원뿔형(`createConicGradient`). 시험 `CanvasOffscreenTest`, `CanvasExtrasTest`.
  CANVAS-B: `dom.{DomMatrixReadOnly,DomMatrix,DomMatrixParser}`(행렬 계산과 CSS 변환 목록 문자열 해석)과 `js.dom.MatrixBindings`(`DOMMatrix`), `js.host.canvas.TextSpacing`(`letterSpacing`/`wordSpacing`), `render.CanvasFilters`(`ctx.filter`, 레이아웃 `FilterSpec` 재사용), `render.{CorsImageSource,ImageLookup}`, `js.host.CorsImageRequestSource`, `net.RequestPurpose.IMAGE_CORS`. 데이터 흐름: crossorigin `img` → `ImageLoads`(`CorsImageRequestSource`) → `PageResources`(항목 키 = URL + CORS 모드, `Origin` 헤더, 응답 검사) → 렌더러(`ImageLookup`)와 캔버스(`HostEnvironment.getLoadedImage(url, cors)`).
- **서버 전송 이벤트(`docs/progress/webapi2.md`)**: `js.host.web.EventSourceApi`/`EventStreamParser`, 요청 서비스 `js.host.StreamingWebRequestSource`(`core.PageWebRequests` 가 구현; 스트림은 메모리에 읽지 않고, 열린 스트림은
  페이지당 6개, 읽기 제한 5분), `net.ResourceRequest.{streaming, readTimeoutMs}`(streaming 요청은 자체 소켓 연결을 쓴다).
- **포인터 이벤트**: `dom.PointerEvent`, `core.PagePointers`(포인터 상태·캡처·호환 마우스 이벤트 억제), `js.host.{PointerApi,PointerService}`; `core.PageInput` 이 마우스를 포인터 이벤트로 먼저 보낸다.
- **기하 관찰자·성능·XPath(WEBAPI-B, `docs/progress/webapi2.md` 3~8절)**: `js.host.{RenderObservers,IntersectionObservers,ResizeObservers}`(레이아웃 게시를 트리거로 관찰 대상의 상자를 다시 재는 렌더링 단계: `core.PageView` → `HtmlPageDocument.layoutPublished()` → `PageRuntime.layoutPublished()` → `PageScripts.layoutPublished()`), `js.host.{PagePerformance,PageTimeline}`(User Timing, Navigation Timing; 페이지가 알 수 없는 네트워크 시각은 0, `HostEnvironment.getNavigationType()` 이 탐색 종류를 준다), `js.host.InterfaceNames`(`Navigator` 등 인터페이스 객체 이름), `js.host.XPathApi`와 패키지 `dom.xpath`(XPath 1.0 파서·평가기: `XPathExpression`, `XPathException`, `NamespaceResolver`).
- **페이지 수명 이벤트**: `js.host.PageLifecycle`(`pageshow`/`pagehide`/`beforeunload`/`unload`/`visibilitychange`, 창 `focus`/`blur`/`online`/`offline`), `dom.BeforeUnloadEvent`(`returnValue`, 확인 요청 판정),
  `dom.PageTransitionEvent`. `unload()` 는 떠나는 페이지에서 한 번만 실행된다. `core.PageRuntime` 이 호스트의 가시성·초점·온라인 변화와 이동·폐기 때의 `unload()` 호출을 연결하지만, 실제 컴포넌트 이동·`clean()`·`dispose()` 에서의 순서는 `core.PageLifecycleIntegrationTest`(9건)가 확인한다. `pageshow` 는 컴포넌트가 페이지를 확정하기 직전에 스크립트에 전달되므로, 사용자 코드가 `pageshow` 직후 곧바로 이동하면 그 페이지는 확정되지 못하고 취소된다.
- **CSS-J(`docs/progress/cssg.md` 30절)**: `render.BorderImageLayer`(아홉 조각 그리기; `mask-border` 의 `MaskBorderLayer` 를 일반화한 것으로 `border-image` 와 `mask-border` 가 함께 쓴다), `render.BackgroundLayers.pickImage`(`image-set()` 후보 선택), `layout.AnchorPositioning`(앵커 위치 지정의 기본 형태: `anchor-name`, `position-anchor`, `anchor()`, `anchor-size()`), `core.CssCursors`(CSS `cursor` 를 컴포넌트의 마우스 커서에 연결, `PageInput` 이 호출), `layout.GeneratedContent` 의 스타일 격리 카운터 스택(`contain: style`), `layout.FontResolver.restrict`(`font-synthesis-*` 로 합성 허용 여부 제한).
- **텍스트 트랙/WebVTT(WEBAPI-C, `docs/progress/webapi2.md` 10절)**: 패키지 `media.vtt`(WebVTT 파일 파서와 cue 텍스트 파서; DOM 과 무관한 순수 Java), `js.host.{TextTracks,TextTrackBindings,MediaTracks,TextTrackValue,TextTrackListValue,TextTrackCueValue,TextTrackCueListValue,VttRegionValue}`와 `dom.TrackEvent`. `js.host.MediaElements` 가 `TextTracks` 를 소유하고 재생 위치 변화와 `track` 변경을 알리면 규격의 time marches on 에 따라 `enter`/`exit`/`cuechange` 를 미디어 요소의 작업으로 올린다(마지막 실행 위치가 cue 시작과 같을 때 활성 cue 를 "놓친 cue" 에서 빼서 `enter` 가 두 번 나가지 않게 했다). 자막은 그리지 않는다(`showing` 은 `hidden` 과 같다).
- **이미지 방향·색 관리·필름 그레인(MEDIA-B, `docs/progress/media-matrix.md` 9절)**: `media.image.ExifOrientation`(JPEG/PNG `eXIf`/WebP `EXIF` 공통 EXIF 방향; `DecodedImage` 가 적용한 방향과 되돌린 이미지 `unoriented()` 를 가진다), CSS `image-orientation: from-image|none`(`Prop.IMAGE_ORIENTATION`, `layout.OrientedContent`가 `none` 일 때 저장된 크기를 쓰고 렌더러가 저장된 그대로 그린다), 패키지 `media.image.color`(ICC 행렬/TRC·회색조 프로필, PNG `gAMA`/`cHRM`/`cICP`/`iCCP`, JPEG APP2·WebP `ICCP`·BMP V4/V5·AVIF `colr` 를 디코딩 직후 한 번 sRGB 로 변환; sRGB 이면 건너뜀; LUT·CMYK 프로필은 sRGB 로 봄; PQ/HLG 는 기준 백색 203 cd/m² 로 맞추고 초과는 자름), `media.image.avif.Av1FilmGrain`(AV1 필름 그레인 합성, 규격 5.9.30/7.18.3)과 AVIF 중첩 오버레이(세 단계, 순환 거부).
- **CSS-K(`docs/progress/cssg.md` 31절)**: `layout.StickyConstraint`(`position: sticky` 의 제약 상자)와 `render.StickyGroup`(스크롤 위치에 따라 밀어 그리는 그룹), `render.TextClipBackground`(`background-clip: text`: 페이지 글자와 같은 텍스트 명령을 마스크로 합성), `BoxCommands.FixedLayer`(`background-attachment: fixed`), `layout.CjkLineBreak`(`line-break` 단계별 금칙), `layout.AnchorPositioning.Area`(`position-area`), `AnimationEngine.animatePseudo`(의사 요소 전환). `PagePainter.paint(..., viewX, viewY)` 가 뷰포트 위치를 따로 받아 fixed/sticky 가 스크롤해도 제자리에 있게 하고, `DisplayList.isScrollDependent()` 인 페이지는 스크롤할 때 전체를 다시 그린다(`SwingWebPanel.updateViewport`). 교차 오리진 웹 글꼴은 CORS 모드로 요청해 응답이 오리진을 허용할 때만 쓴다.
- **AV1 인터 프레임·초해상도·계층·애니메이션 AVIF(MEDIA-C, `docs/progress/media-matrix.md` 10절)**: `media.image.avif` 의 `Av1Decoder`(인스턴스 하나가 시퀀스를 디코딩: 참조 슬롯 8개 `Av1RefFrame`, 순서 힌트, `show_existing_frame`, CDF 저장/적재, 시간 단위 API `decodeTemporalUnit`, 동작 지점/공간 계층 선택), `Av1MvStack`(움직임 벡터 예측 7.10), `Av1InterPred`(이동 보상·복합·OBMC·인트라 블록 복사), `Av1Warp`(국소/전역 워프), `Av1WedgeMasks`, `Av1MotionField`(시간 움직임 벡터), `Av1Upscale`(초해상도 7.16), `Av1ToolStats`(시험이 쓰인 도구를 증명하는 카운터), `AvifInterTables`/`AvifInterCdfTables`(규격 표; 생성 스크립트 `browser/src/test/resources/media/avif/generation/`), `AvifContainer.Track`(moov/trak 샘플 표)와 `AvifDecoder.decodeSequence`(애니메이션 AVIF). 지원하지 않는 기능은 틀린 그림이 아니라 `UNSUPPORTED_FORMAT`/`CORRUPT` 로 실패한다.
- **CSS-L(`docs/progress/cssg.md` 32절)**: `layout.Resizing`/`core.PageResize`/`render.DisplayCommands.Resizer`(`resize` 핸들: 판정, 그리기, 끌어서 인라인 `width`/`height` 변경), `layout.ShapeImage`/`ShapeImageSource`(`shape-outside` 이미지의 알파 마스크 공급 계약; `render.ImageReplacedResolver` 가 CORS `anonymous` 로 가져온 첫 프레임으로 구현, `layout.FloatShapes` 가 `Area` 로 바꿈), `media.image.svg.SvgReference`/`render.SvgReferences`(CSS `filter`/`clip-path`/`mask-image` 의 `url(#id)`: 같은 문서의 SVG 정의를 SVG 이미지와 같은 `SvgFilters`/`SvgMasks` 로 상자에 적용), `render.VerticalTextCommands.EmphasisColumn`(세로쓰기 `text-emphasis`), `layout.MulticolLayouter` 의 clone 장식 계산(다단 조각의 `box-decoration-break: clone`), `css.values.CalcNode.Anchor`/`CssCalc.withAnchors`/`mapAnchors`(`calc()` 안 `anchor()`), `layout.AnchorPositioning.Lookup.defaultRect`/`inScope`/`hidden`(암묵 앵커, `anchor-scope`, `position-visibility`), `layout.WebFontSet.TimedLoader`/`core.PageResources.fontTimes`(`font-display` 기간: 요청 계층이 잰 시작·도착 시각), `WebFontSet.blockedUntil`/`takeBlockDeadline`, `LayoutBox.isFontBlocked`, `PagePresentation.scheduleBlockEnd`(P10-08-g: block 기간의 텍스트는 대체 글꼴 자리만 차지하고 그리지 않으며 기간 끝에 다시 렌더링), `media.font.{Brotli,BrotliData,BrotliException,Woff2}`(WOFF2: RFC 7932 Brotli 디코더와 WOFF2 컨테이너·변환된 `glyf`/`loca`/`hmtx` 복원 뒤 sfnt 재조립, `layout.WebFontSet` 이 `format('woff2')` 와 서명으로 사용), `css.cssom.PositionTryRule`, `layout.PositionTry`/`PositionTrySource`, `layout.AbsoluteLayouter` 의 시도 배치(`trial`/`fallback`/`applyVisibility`)(`@position-try`, `position-try-fallbacks`/`-order`, `position-try`, `anchor-center`). 새 CSS 속성 id 312~315(`position-try-fallbacks`, `position-try-order`, `position-visibility`, `anchor-scope`), 규칙 종류 `CssRule.POSITION_TRY_RULE`. WOFF2 시험 자료는 스크립트가 만든 합성 글꼴, Node.js 참조 Brotli 인코더의 스트림과 독립 인코더 fontTools 의 WOFF2 이며, JRE 의 Lucida 글꼴(Java 와 함께만 쓰도록 허락됨)로 만든 파일은 저장소에 두지 않는다(시험 때 변환: `browser/src/test/java/org/duckdns/hjow/swingwebpanel/media/font/Woff2Fonts.java`).

## Web Audio 엔진 (P25 A1, 2026-11-22)

- **패키지**: `media.webaudio`(순수 Java, `javax.swing`/DOM/JavaScript 와 무관한 그래프 엔진)와 하위 `media.webaudio.dsp`(FFT, 쌍이차/IIR 커널). JavaScript 바인딩(`js.host.webaudio`, C 단계)과 AudioWorklet(`js.worker` 구조 재사용, D 단계)은 이 위의 얇은 계층으로 둔다.
- **스레드 모델**: 제어 스레드(JavaScript 또는 호출자)가 `connect/disconnect/채널 속성/start/stop` 을 부르면 동기 구간에서 검증하고, 렌더링 스레드(오프라인에서는 호출 스레드)가 render quantum(128 프레임) 경계에서 **제어 메시지**를 적용한다. 단순 속성은 `volatile` 필드를 quantum 시작에 스냅샷으로 읽고, 자동화 타임라인은 짧은 `synchronized` 구간으로 공유한다.
- **렌더링 규칙**: 렌더링 중에는 새 배열을 만들지 않고 `AudioBus`/`float[]` 를 재사용한다(채널 수가 바뀔 때만 재할당). 시작한 원천과 꼬리 시간 안의 노드만 엔진이 붙잡고, 연결도 시작도 없는 노드는 처리하지 않는다. DelayNode 가 든 순환은 읽기/쓰기 두 단계로 나누고 그 밖의 순환은 무음 처리한다(Tarjan). 한도는 `WebAudioLimits` 의 이름 있는 상수(활성 노드 20000, 자동화 이벤트 65536 등).
- **채널 믹스**: 기존 `media.audio.ChannelMixer`(디코딩 결과를 스테레오로 줄이는 용도)는 재사용하지 않고 규격 4.4/4.5절 행렬을 `ChannelMixing` 에 구현했다.
- 도구 목록은 변경 없음(`tools/webfetch` 로 규격 조회). A2 에서 `tools/hrtfgen`(HRTF 자료 변환)이 추가될 예정이다. 상세 `docs/progress/webaudio.md`.
- **디코딩(P25-05, `media.webaudio.decode`)**: `AudioBufferDecoder`(바이트 → P18 디코더(`AudioDecoders.open`, 시그니처 판별) → float → 채널별 `Resampler`(Kaiser 창 sinc 다상, 정지 대역 100 dB) → `AudioBuffer`)와 `AudioDecodeService`(인스턴스마다 데몬 스레드 풀 `swingwebpanel-audio-decode-N`, 유휴 5초 뒤 종료, 정적 상태 없음, 콜백은 호출자가 준 `Executor`(문서 큐용), 취소/닫기/한도). 스레드가 하나이면 요청 순서, 둘 이상이면 순서 비보장(규격이 정하지 않음).
- **A2 노드와 JAR 리소스(P25-04)**: `media.webaudio` 에 `WaveShaperNode`(2x/4x 과표본화, Kaiser 반대역 필터), `ConvolverNode`(지연 없는 분할 FFT 합성곱), `DynamicsCompressorNode`(Chromium 설계를 옮김, BSD 고지 유지), `AnalyserNode`, `PannerNode`/`AudioListener`(equalpower, HRTF), `MediaElementAudioSourceNode`(순수 Java 쪽, `<audio>` 어댑터는 C 단계)가 추가됐다. 제품 JAR 에 **비자바 리소스 `media/webaudio/dsp/kemar-compact.hrtf`(약 381 KB)가 처음으로 들어간다**(Maven이 `browser/src/main/resources/`에서 복사). HRTF 자료를 못 읽으면 패너는 equalpower 로 되돌아간다.
- **Web Audio 의 JavaScript 바인딩(P25 C1)**: 패키지 `js.host.webaudio`(`WebAudioApi` 설치·정리, `ContextApi`/`NodeApi`/`ValueApi`/`BufferApi` 가 인터페이스 정의, `OfflineRender` 가 오프라인 렌더링). 실시간 컨텍스트는 `media.webaudio.RealtimeAudioContext` + 교체 가능한 `Driver`(기본 `ClockRenderDriver`: 벽시계 속도로 렌더링하고 출력은 버리므로 소리가 나지 않고 시계만 흐른다. 장치 드라이버는 C2). 엔진 스레드가 스크립트에 알리는 것은 `WebAudioApi.postTask` 를 통한 페이지 작업뿐이다. `AudioBuffer.getChannelData` 는 채널마다 기준 저장소 하나(건드리기 전 `float[]`, 건드린 뒤 `byte[]` 로 `Float32Array` 가 뒷받침)이고 노드는 acquire 스냅샷으로 가져간다.
- **실시간 출력과 `ScriptProcessorNode`(P25 C2a)**: `media.webaudio.output`(`AudioOutput`, `JavaSoundOutput`(16비트 반올림·클립, 장치가 요청 표본율/채널을 지원하지 않으면 `Resampler` 변환), `NullOutput`(장치 없음: 소리는 버리되 벽시계 속도), `OutputFactory`/`SystemOutputFactory`)와 `media.webaudio.OutputRenderDriver`(`RealtimeAudioContext.Driver` 구현: 스레드 `swingwebpanel-audio-render-N` 이 첫 `start` 에서 생겨 `close` 까지 유지, 컨텍스트가 도는 구간마다 출력을 열고 일시정지·닫기에서 해제하므로 일시정지 중에는 장치를 붙잡지 않음, 장치를 못 열면 그 구간은 `NullOutput`, `latencyHint` 별 청크/버퍼 크기와 끊김 횟수, 렌더링 스레드는 JavaScript 를 부르지 않음). `ScriptProcessorNode` 의 순수 Java 엔진 노드는 `2*bufferSize` 지연(Chromium 방식)이며 실시간은 응답이 늦으면 무음, 오프라인은 렌더링이 리스너를 기다린다(기본 최대 10초). 자원 소유 표의 `WebAudioApi` 행에는 "오디오 출력 라인(구간마다 열고 닫음, `ctx.close()` 로 끝남)"이 포함된다.
- **`<audio>` 요소 연결과 호스트 출력(P25 C2b)**: `media.webaudio.MediaElementTap` 이 P18 재생기(`media.playback.MediaPlayer`)의 출력 자리에 "그래프가 비우는 링 파이프"를 꽂는다(`MediaPlayer.reopenOutput()`; 기존 16비트 재생 경로는 그대로). 그 요소의 시계는 그래프가 되고(컨텍스트가 suspended 이면 요소가 멈춘다), 표본율이 다르면 `Resampler` 로 변환하며, 음량·음소거는 재생기가 곱한다. 의존 방향은 `media.webaudio` → `media.playback` 하나이다. **새 스레드는 없다**(재생 스레드 `swingwebpanel-audio` 와 렌더링 스레드 `swingwebpanel-audio-render-N` 이 파이프를 통해 만나며 링은 짧은 잠금으로 보호). 호스트는 `SwingWebPanel.createWebAudioOutputFactory()`(기본 Java Sound, null 이면 무음)로 Web Audio 출력을 바꾼다(`<audio>` 용 `createAudioOutputFactory()` 와는 독립, `docs/API_CONTRACT.md` 5.21). 자원 소유 표: `WebAudioApi` 행에 요소 탭(`ContextValue.releaseTaps`)과 "한도에서 포기되는 컨텍스트(suspended 이고 컨텍스트와 노드의 스크립트 객체가 모두 수거됨)"가 더해지고, `MediaElements` 의 요소별 출력 팩터리는 탭이 있으면 그래프의 파이프, 없으면 컴포넌트의 출력이다.
- **오디오 worklet(P25 D1)**: 패키지 `js.host.webaudio.worklet`(`WorkletScope` 가 컨텍스트마다 DOM 없는 별도 realm 과 worklet 스레드를 소유, `WorkletGlobals`, `ProcessorRunner`, `ModuleGraphFetcher` 등)와 새 공개 클래스 `js.worker.ChannelPorts`(`MessagePort` 설치와 두 문맥에 끝이 하나씩 속한 채널 만들기), 엔진 쪽 `media.webaudio.AudioWorkletNode`. 스레드: 컨텍스트마다 worklet 스레드 `swingwebpanel-audio-worklet-N`(데몬, 모듈 평가와 메시지 전달)과 realm 공정 잠금 하나. `process()` 는 오디오 컨텍스트의 렌더링 스레드가 부르며, 실시간은 `tryLock` 2 ms(못 쥐면 그 quantum 무음 + 콘솔 경고), 오프라인은 기다리면서 쌓인 메시지를 먼저 실행해 결과가 결정적이다. 페이지 스레드는 worklet realm 을 만지지 않는다. 모듈 그래프는 잠금 밖에서 먼저 가져온다. `addModule` 의 실패 의미는 HTML Standard 와 Chromium 156 을 따른다(구문 오류는 그 오류로 reject, 평가 중 예외는 resolve 하고 콘솔에 보고, 네트워크·스킴·오리진·MIME 실패만 `AbortError`). 자원 소유 표: `WorkletScope`(realm, worklet 스레드, 작업 큐) ← `WebAudioApi.unregister`/`dispose()` → `WorkletApi.contextClosed`/`dispose()` → `WorkletScope.dispose()`.
