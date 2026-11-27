# desktopapp 설계와 진행 기록 (P31)

SwingWebPanel 로 만드는 Swing 웹 브라우저 서브 프로젝트 `desktopapp` 의 범위, 구조, SwingWebPanel 보완 목록을 기록한다.
계획은 `AGENTS.md` 의 P31, 공개 API 계약은 `docs/API_CONTRACT.md`, 기능 판정은 `docs/FEATURE_MATRIX.md` 를 따른다.

## 1. 범위 (P31-01, 2026-11-26 확정)

### 1.1 필수 기능 (사용자 지정)

| 기능 | 계획 항목 |
| --- | --- |
| 제목 표시줄, 주소 표시줄 | P31-03 |
| 뒤로 가기, 앞으로 가기, 홈, 멈춤(로딩 중지), 새로고침 버튼 | P31-03 |
| 상태 표시줄 | P31-03 |
| 탭 브라우징 | P31-04 |
| 메뉴 | P31-05 |
| 북마크 | P31-06 |
| 설정 화면(홈 화면, 기본 글꼴 등) | P31-07 |
| 팝업(`window.open` 의 창 객체로 제어, 기본 차단, 열 위치 설정) | P31-08 |
| 인쇄 | P31-10 |

### 1.2 함께 제공하는 기본 기능

새 창, 다운로드(기존 다운로드 팝업과 저장 폴더 설정), 확대·축소, 페이지 원본 보기, 페이지 내 찾기, 세션 방문 기록 목록,
키보드 단축키, 개인 정보 삭제, 콘솔 보기. 계획 항목은 P31-05, P31-08, P31-09 이다.

### 1.3 범위 밖

- 확장 프로그램, 동기화, 프로필, 시크릿 창, 암호 저장, 개발자 도구(콘솔 보기는 제공), 자동 완성 기록은 만들지 않는다.
- 주소 표시줄의 검색은 설정에 검색 주소 서식이 있을 때만 동작한다. 기본값은 비어 있다(사용자 입력을 외부 서비스로 보내는 기본값을 두지 않는다).
- 방문 기록은 창의 세션 이력만 보인다. 프로그램을 끝낸 뒤에도 남는 전체 방문 기록은 저장하지 않는다.

## 2. 모듈 (P31-01)

| 항목 | 결정 |
| --- | --- |
| 디렉터리 | 저장소 루트의 `desktopapp/` (부모 POM `<modules>` 에 추가, `examples` 뒤) |
| Maven 좌표 | `org.duckdns.hjow:swingwebpanel-desktopapp`, 판은 부모와 같다 |
| 패키지 | `org.duckdns.hjow.swingwebpanel.desktopapp` |
| 의존성 | 제품 모듈 `swingwebpanel` 하나(전이 의존성으로 `swingwebpanel-js`). 외부 라이브러리 없음 |
| 컴파일 | 부모 POM 의 JDK 6 `javac` 설정을 그대로 쓴다(`-source 1.6 -target 1.6`) |
| 실행 JAR | `desktopapp/target/swingwebpanel-desktopapp.jar`. 앱 클래스와 `swingwebpanel`·`swingwebpanel-js` 클래스를 합친 단일 JAR, `Main-Class` = `org.duckdns.hjow.swingwebpanel.desktopapp.Main`. JRE 6 에서 `java -jar` 로 실행한다 |
| 배포 | Maven Central 배포 대상이 아니다(`maven.deploy.skip`). 제품 JAR(`swingwebpanel.jar` 등)에는 앱 클래스를 넣지 않는다 |
| 스크립트 | `desktopapp/run.sh`(UTF-8), `desktopapp/run.bat`(MSWIN949): Maven 으로 빌드한 뒤 JDK 6 의 `java -jar` 로 실행한다 |
| 문서 | `desktopapp/README.md`(사용자용, 한국어), `desktopapp/AGENTS.md`(AI 용) |
| 시험 | `desktopapp/src/test/java`. 엔진 모듈의 시험 JAR 에 있는 자체 실행기(`testing.TestRunner`, `Check`)를 재사용하고 JDK 6 `java` 로 실행한다(브라우저 모듈과 같은 antrun 실행) |
| 앱 이름 | `SwingWebPanel Browser`(창 제목과 정보 대화 상자) |

경계 규칙: 앱은 `org.duckdns.hjow.swingwebpanel` 패키지의 공개 타입(`SwingWebPanel`, `ConsoleListener`/`ConsoleEvent`, `WebMessage*`, P31 에서 추가하는 공개 타입)과
그 보호 확장 지점만 쓴다. `core`, `js`, `layout`, `net` 같은 내부 패키지를 import 하지 않는다. 시험이 앱 소스의 import 를 검사한다.

## 3. 구조 (P31-01)

화면 부품과 모델/제어 계층을 나눈다. 모델/제어 계층은 Swing 창 없이(headless) 시험한다.

| 패키지 | 내용 |
| --- | --- |
| `desktopapp` | `Main`(진입점), `BrowserApp`(프로세스 전체: 설정·북마크 모델, 열린 창 목록, 종료) |
| `desktopapp.model` | `Settings`, `SettingsStore`, `BookmarkNode`, `BookmarkModel`, `BookmarkStore`, `AddressInput`(주소 입력 정규화), `ClosedTabs` |
| `desktopapp.ui` | `BrowserWindow`(JFrame), `BrowserTab`(탭 하나 = SwingWebPanel 하나), `TabStrip`, `NavigationBar`, `StatusBar`, `Menus`, `SettingsDialog`, `BookmarkDialog`, `FindBar`, `ConsoleWindow`, `SourceWindow`, `PrintPreview`, `PopupWindow`(JDialog) |

- 탭 하나가 SwingWebPanel 하나를 가진다. 창의 도구 모음·주소 표시줄·상태 표시줄·제목은 활성 탭의 상태에 연결한다.
- 모든 Swing 조작은 EDT 에서 한다. SwingWebPanel 의 공개 메서드는 어느 스레드에서나 부를 수 있지만(P01-04) 앱은 EDT 에서만 부른다.
- 설정과 북마크 모델은 프로세스에 하나이고 모든 창이 공유한다. 변경은 리스너로 모든 창에 알린다.

## 4. 사용자 데이터 (P31-01)

| 항목 | 결정 |
| --- | --- |
| 위치 | `${user.home}/.swingwebpanel/desktopapp/`. 시스템 속성 `swingwebpanel.desktopapp.home` 으로 바꿀 수 있다(시험과 휴대용 실행) |
| 설정 | `settings.properties`. `java.util.Properties` 의 텍스트 형식을 UTF-8 `Reader`/`Writer` 로 읽고 쓴다(Java 6 API) |
| 북마크 | `bookmarks.txt`. UTF-8, 한 줄에 한 항목(종류, 깊이, 이름, 주소를 탭으로 구분하고 탭·줄바꿈·역슬래시를 이스케이프) |
| 창 상태 | `settings.properties` 안의 `window.*` 키(위치, 크기, 최대화). "지난번 창과 탭 복원"용 주소 목록은 `session.txt` |
| 쓰기 | 같은 디렉터리의 임시 파일에 쓴 뒤 교체한다. 교체에 실패하면 원본을 남긴다 |
| 읽기 실패 | 파일이 없으면 기본값. 해석할 수 없으면 손상된 파일을 `이름.broken` 으로 보존하고 기본값으로 시작한다 |
| 금지 | Java 객체 직렬화를 쓰지 않는다. 브라우저 캐시와 localStorage 의 위치(P16)는 바꾸지 않는다 |

## 5. SwingWebPanel 보완 목록 (P31-01, 2026-11-26 확인과 결정)

"기존 계약과 기본 동작을 바꾸지 않는다"가 원칙이다. 새 공개 메서드는 `docs/API_CONTRACT.md` 의 확장 메서드 절과 기능 표에 행을 추가한다.

| 후보 | 현재 상태(코드 확인) | 결정 | 계획 항목 |
| --- | --- | --- | --- |
| (a) 기본 글꼴 설정 | `layout.FontResolver.javaName` 이 `serif`/`sans-serif`/`monospace` 를 Java 논리 글꼴에 정적으로 대응시킨다. `font-size: medium` 은 고정 크기이다. 웹 글꼴은 `LayoutEngine` 이 배치 동안 스레드 지역 값으로 넘긴다 | **추가**. 컴포넌트별 글꼴 설정 API. 웹 글꼴과 같은 방식(배치 동안의 스레드 지역 값)으로 전달해 정적 공유 상태를 만들지 않는다 | P31-07 |
| (b) 새 창 요청과 팝업 | `js.host.FrameApi` 의 `window.open` 은 새 창이 필요하면 null. `core.FrameNavigator` 는 링크·폼의 `_blank` 를 현재 문맥에서 연다. 문서 스레드는 컴포넌트마다 하나(`core.NavigationController`) | **추가**. 새 창 요청 확장 지점, 창 관계(opener), 팝업 차단 설정, 기본 팝업 창(JDialog) | P31-08 |
| (c) 로딩 중 여부 | 전용 메서드는 없다. `getProgress()` 가 로딩 중에는 0~99 또는 -1, 끝나면 100 이다(계약 5.11). `stop()` 이 있다 | **추가하지 않음**. 앱은 `getProgress() != 100` 과 `"progress"` 속성 알림으로 판정한다 | P31-03 |
| (d) 페이지 내 찾기 | 없다. 텍스트 선택과 하이라이트 그리기(P11-08, P10-08-h)는 있다 | **추가**. 찾기 API | P31-09 |
| (e) 편집 명령 | 키보드(Ctrl+A/C, 편집 영역의 Ctrl+X/V)로만 동작한다(`core.PageKeyboard`, `core.PageSelection`). 호스트가 부를 공개 메서드는 없다 | **추가**. 메뉴가 부를 `copy`/`cut`/`paste`/`selectAll` 공개 메서드. 키보드와 같은 경로를 쓴다 | P31-05 |
| (f) 쿠키 삭제 | `clean()` 은 계약상 쿠키를 유지한다(5.13). 지우는 메서드가 없다 | **추가**. 쿠키만 지우는 메서드(공유 저장소 객체의 `clear()`). (g)와 함께 만든다 | P31-04, P31-09 |
| (g) 탭·창 사이 쿠키 공유 | 쿠키 저장소는 컴포넌트마다 따로이다(`SwingWebPanel` 의 `cookies` 필드를 필드 초기화에서 만든다). `core.BrowserSession` 은 저장소를 받는 생성자가 있지만 `dispose()` 가 저장소를 비운다 | **추가**. 루트 패키지의 공개 타입(가칭 `CookieJar`)으로 쿠키 저장소를 나타내고, 그것을 받는 `SwingWebPanel` 생성자를 둔다. 같은 객체를 받은 컴포넌트들이 쿠키를 공유한다. 공유 저장소는 한 컴포넌트의 `dispose()` 로 비워지지 않는다. 기본값(컴포넌트별 저장소)과 `getCookies()`·`clean()` 계약은 그대로 둔다. HTTP 캐시는 공유하지 않는다(컴포넌트별 캐시 계약 유지) | P31-04 |
| (h) 인쇄 | 실제 인쇄와 쪽 나눔 배치가 없다. `window.print()` 는 빈 함수, `beforeprint`/`afterprint` 는 처리기 속성만 있다. `@page` 는 CSSOM 으로만 해석한다 | **추가**. 인쇄 API(`Printable`/`Pageable`), 인쇄 이벤트, `window.print()` 확장 지점 | P31-10 |

P30 에서 이미 추가한 것: `setRootWindowClosingEnabled`/`isRootWindowClosingEnabled`, `closeRootWindow()`(계약 5.23). 앱은 `closeRootWindow()` 를 재정의하여 탭만 닫는다(P31-08).

P30 구현 중 확인한 사실(P31-08 설계에 영향): 파싱 중에 실행되는 인라인 스크립트는 문서 스레드가 아니라 로더 스레드에서 페이지 잠금을 쥐고 실행된다.
확정 전 페이지의 타이머는 `core.DocumentScheduler` 가 확정 뒤로 미룬다. 따라서 "DOM 과 스크립트는 문서 스레드만 건드린다"가 아니라
"한 페이지의 스크립트 작업은 그 페이지의 잠금으로 직렬화되고, 확정된 페이지의 작업은 문서 스레드에서 실행된다"가 정확하다.
P31-08-a(이벤트 루프 공유)와 P31-08-b(팝업의 동기 생성)는 이 구조를 전제로 다시 확인한다.

## 6. 진행 기록

| 날짜 | 항목 | 내용 |
| --- | --- | --- |
| 2026-11-26 | P31-01 | 범위, 모듈, 구조, 사용자 데이터, SwingWebPanel 보완 목록을 확정했다(이 문서 1~5절) |
| 2026-11-26 | P31-02 | 모듈 골격, 실행 JAR, 시험 실행 연결. JDK 6u45 에서 앱 시험 3건과 `java -jar ... --smoke` 통과 |
| 2026-11-26 | P31-03 | 주 창, 탐색 도구 모음, 주소 표시줄, 상태 표시줄. 앱 시험 11건 통과. 실제 창 화면 캡처로 배치 확인(JRE 6u45, Windows 10) |
| 2026-11-27 | P31-04-a | 탭 브라우징(탭 머리, 새 탭, 닫기, 다시 열기, 전환, 활성 탭 연결, 수명, 닫기 확인). 보완 후보 (f)(g)를 `CookieJar`·`clearCookies()` 로 추가했고(계약 5.24), 목록에 없던 `getTitle()` 의 살아 있는 문서 제목(계약 5.25)을 추가했다. 앱 시험 15건 통과. 링크를 새 탭에서 열기(P31-04-b)는 P31-08 과 함께 한다 |
| 2026-11-27 | P31-05-a | 보완 후보 (e): `copy()`/`cut()`/`paste()`/`selectAll()` 과 명령 키 소비(계약 5.26). 집중 시험 5건 통과. 전체 회귀: 엔진 892 통과, 브라우저 3,845 통과·1 실패(P22-16-c4 의 기존 실패)·12 건너뜀 |
| 2026-11-27 | P31-07 | 설정: 항목(`model.Settings`), 파일(`SettingsStore`), 세션(`SessionStore`), 대화 상자(`ui.SettingsDialog`), 탭별 적용, 시작 페이지와 세션 복원, 창 위치 기억. 보완 후보 (a)를 컴포넌트별 기본 글꼴 API 로 추가(계약 5.27). 브라우저 시험 5건, 앱 시험 35건 통과 |
| 2026-11-27 | P31-06 | 북마크: 모델(`BookmarkNode`, `BookmarkModel`), 파일(`BookmarkStore`, `TextFiles`), Netscape HTML 가져오기·내보내기(`BookmarkHtml`), 북마크 메뉴, 관리 창(`ui.BookmarkDialog`). 앱 시험 27건 통과 |
| 2026-11-27 | P31-08, P31-04-b | 보완 후보 (b): 페이지가 여는 창과 팝업 차단(계약 5.28). 앱: `ui.TabPanel`, `ui.PopupWindow`, 팝업·링크의 열 위치, 차단 알림과 단추, 링크의 새 탭·새 창(Ctrl·Shift·가운데 단추·문맥 메뉴), 탭을 닫을 때 `disposeKeepingStorage()`. 브라우저 시험 12건, 앱 시험 39건 통과 |
| 2026-11-27 | P31-05-b | 메뉴 막대(`ui.BrowserMenu`), 명령(`ui.Commands`, `BrowserWindow.perform`), 가속 키와 메뉴 항목 없는 키, 페이지 저장(`model.PageFiles`), 확대 단계(`model.ZoomSteps`), 새 창, 끝내기, 정보. 앱 시험 21건 통과 |

## 7. 구현하면서 정한 것

### 7.1 주소 표시줄 (P31-03)

- 주소 표시줄이 보이는 주소는 탭이 정한다(`BrowserTab.getAddressText()`): 사용자가 연 주소가 아직 도착하지 않았으면 읽는 중인 그 주소, 아니면 보이는 페이지의 주소.
  읽는 중인 주소는 페이지가 확정되거나(`page` 속성) 진행률이 100 이 되면(완료 또는 멈춤) 지운다.
- 사용자가 글을 고친 뒤에는(`edited`) 페이지가 바뀌어도 덮어쓰지 않는다. Enter 로 열기에 성공하거나 Esc 로 되돌리거나 다른 탭으로 바꾸면 풀린다.
- 열 수 없는 글(검색 서식이 없는데 공백이 있는 글, 컴포넌트가 거부한 주소)은 주소 표시줄에 그대로 두고 상태 표시줄에 알림을 보인다.
  알림은 페이지가 바뀌거나 페이지가 새 상태 문구를 내면 사라진다.
- 파일 경로는 `java.net.URI` 로 직접 주소를 만든다. `File.toURI()` 는 파일 시스템을 조회해서 UNC 경로(`\\서버\공유`)에서 2초 이상 EDT 를 막았다(시험에서 확인).
- 컴포넌트는 `file:/C:/...` 를 `file:///C:/...` 로 정규화해 알려 준다. 앱도 같은 형태로 만든다.

### 7.2 UI 글의 언어 (P31-03)

- 글은 쓰는 곳에 영어와 한국어를 함께 적고 `Texts.of(영어, 한국어)` 가 고른다. JVM 기본 로캘이 한국어이면 한국어, 아니면 영어이다.
  시스템 속성 `swingwebpanel.desktopapp.lang` 으로 바꿀 수 있다. 시험은 `en` 으로 고정한다.
- 아이콘은 Java2D 로 그린다(`ui.Icons`). 이미지 파일과 특정 글꼴에 기대지 않는다.

### 7.3 탭 (P31-04-a)

- 탭 하나가 `BrowserTab`(SwingWebPanel 과 그 `JScrollPane`) 하나이다. 창의 도구 모음, 주소 표시줄, 상태 표시줄, 제목은 활성 탭만 따르고, 배경 탭의 변경은 탭 머리(제목, 읽는 중 표시)에만 반영한다.
- 탭 머리의 제목은 24자에서 자르고 전체 제목은 도구 설명으로 보인다. 읽는 중인 탭의 제목은 기울인 글꼴이다.
- 다른 탭으로 바꾸면 주소 표시줄에서 편집하던 글은 버린다.
- 닫은 탭은 주소만 기억한다(창마다 20개, 빈 페이지는 제외). 다시 열면 이력과 폼 내용은 돌아오지 않는다.
- 응용 프로그램의 모든 탭과 창이 `CookieJar` 하나를 쓴다. sessionStorage 와 이력은 탭마다 따로이다.
- 배경 탭의 페이지는 계속 실행되고 `document.visibilityState` 는 `hidden` 이 된다(컴포넌트의 `isShowing()` 연결).
- 대화 상자는 `BrowserApp.Prompts` 를 거친다. 시험은 이것을 바꿔 대화 상자 없이 답한다.

### 7.4 메뉴와 단축키 (P31-05, 2026-11-27 구현)

- 명령: 메뉴 항목, 키 바인딩, 시험이 모두 `BrowserWindow.perform(명령 이름)` 하나로 명령을 실행한다. 이름은 `ui.Commands` 의 상수이고 메뉴 항목의 컴포넌트 이름으로도 쓴다.
- 메뉴와 가속 키(기능이 이미 있는 것만. 나머지는 각 기능의 계획 항목이 더한다)
  - 파일: 새 탭 Ctrl+T, 새 창 Ctrl+N, 위치 열기 Ctrl+L, 파일 열기 Ctrl+O, 페이지 저장 Ctrl+S, 탭 닫기 Ctrl+W, 창 닫기 Ctrl+Shift+W, 끝내기
  - 편집: 잘라내기 Ctrl+X, 복사 Ctrl+C, 붙여넣기 Ctrl+V, 모두 선택 Ctrl+A
  - 보기: 도구 모음, 상태 표시줄(체크 항목), 확대 Ctrl+=, 축소 Ctrl+-, 원래 크기 Ctrl+0
  - 이동: 뒤로 Alt+←, 앞으로 Alt+→, 홈 Alt+Home, 새로고침 F5, 캐시 무시 새로고침 Ctrl+F5, 멈춤 Esc
  - 탭: 다음 탭 Ctrl+Tab, 이전 탭 Ctrl+Shift+Tab, 닫은 탭 다시 열기 Ctrl+Shift+T
  - 도움말: 정보(앱, SwingWebPanel, Java 의 판. JAR 매니페스트의 `Implementation-Version`)
- 메뉴 항목이 없는 추가 키(루트 창의 `WHEN_IN_FOCUSED_WINDOW` 바인딩): Ctrl+숫자패드 +/-/0, Ctrl+R, Ctrl+Shift+R, Ctrl+PageDown/PageUp, Ctrl+F4, F6, Alt+D, Ctrl+1~8(그 번호의 탭), Ctrl+9(마지막 탭).
- 키 충돌 규칙
  1. 페이지가 스스로 처리하는 명령 키는 컴포넌트가 소비한다(계약 5.26). 메뉴 가속 키는 실행되지 않는다.
  2. 주소 표시줄과 폼 컨트롤 같은 텍스트 필드는 자기 편집 키를 먼저 처리한다.
  3. 그 밖의 가속 키는 브라우저가 실행한다. 페이지는 `keydown` 을 보지만 막을 수 없다.
  4. Tab 과 Shift+Tab 은 포커스 이동이다(페이지에 포커스가 있으면 페이지 안에서 이동). Ctrl+Tab 은 탭 전환이므로 창의 포커스 이동 키에서 뺀다(`setFocusTraversalKeys`).
  5. 탭의 `JScrollPane` 이 가진 Ctrl+PageUp/PageDown(가로 쪽 넘김) 바인딩은 끄고 탭 전환에 쓴다.
  6. 입력기가 조합 중인 키는 입력기가 소비한다.
- 편집 명령의 대상: 창 자체의 텍스트 필드(주소 표시줄)에 포커스가 있으면 그 필드, 아니면 활성 탭의 `SwingWebPanel.copy()` 등.
- 페이지 저장: `getContent()` 원문을 UTF-8 로 쓴다. HTML/XHTML 은 원문의 문자셋 선언보다 우선하도록 바이트 순서 표시를 먼저 쓴다. 제안 이름은 주소 경로의 마지막 부분이고 확장자가 없으면 MIME 에 따라 붙인다.
- 파일 선택과 알림은 `BrowserApp.Prompts` 에 `chooseFile`, `inform` 을 더해 시험이 대신 답하게 한다.
- 시험 방법: 가속 키는 보이는 창이 있어야 실행된다(`KeyboardManager` 가 `isShowing()` 을 본다). 또 Swing 은 포커스를 받을 수 있는 창에만 창 전체의 키 바인딩을 등록한다
  (`KeyboardManager.getTopAncestor` 가 `isFocusableWindow()` 를 본다). 그래서 시험은 사용자의 포커스를 빼앗지 않도록 포커스를 받지 않는 상태로 창을 화면 밖에 보인 뒤,
  상태를 되돌리고 메뉴 막대를 다시 달아 등록을 새로 하게 한다(`WindowCase.showOffScreen`). 키 입력은 컴포넌트의 키 리스너 호출과 `SwingUtilities.processKeyBindings` 로 흉내 낸다.
- 메뉴 항목의 활성 상태는 메뉴가 열릴 때(`menuSelected`) 맞추고 닫힐 때 모두 활성으로 되돌린다. 비활성 항목의 가속 키는 실행되지 않으므로, 닫힌 메뉴에 낡은 비활성 상태가 남으면 키가 막힌다.
- `JMenuBar` 를 상속한 클래스에 `update(Object)` 라는 메서드를 두면 `update(null)` 호출이 `JComponent.update(Graphics)` 로 풀린다. 메서드 이름은 `refreshItems` 이다.
- 도구 모음과 상태 표시줄을 보일지는 설정(`view.toolbar`, `view.statusBar`)이므로 모든 창에 함께 적용된다. 새 탭과 "원래 크기" 명령의 확대 비율도 설정(`zoom.default`)이다.
- 확대 키(Ctrl 과 +, -, 0)는 페이지에 포커스가 있으면 컴포넌트가 처리하고(계약 5.26) 주소 표시줄에 포커스가 있으면 메뉴 가속 키가 처리한다. 두 경로가 같은 단계(`ZoomSteps`)를 쓴다.
  다만 컴포넌트의 Ctrl+0 은 100% 로, 메뉴의 "원래 크기"는 설정의 기본 확대 비율로 되돌린다.

### 7.5 북마크 (P31-06, 2026-11-27)

- 모델: 루트 폴더 하나 아래의 트리이다. 항목(`BookmarkNode`)은 모델(`BookmarkModel`)만 바꾸고, 바꿀 때마다 리스너에 알린다. 응용 프로그램(`BrowserApp`)이 리스너로 매번 파일에 저장한다.
- 파일 `bookmarks.txt`: 첫 줄 `swingwebpanel-bookmarks 1`, 그 뒤 한 줄에 항목 하나. 필드는 탭으로 구분한다: 종류(`F` 폴더, `B` 북마크), 깊이(루트의 항목이 0), 이름, 북마크이면 주소.
  필드 안의 탭·줄 바꿈·캐리지 리턴·역슬래시는 `\t`, `\n`, `\r`, `\\` 로 적는다. 폴더의 항목은 그 폴더 줄 바로 뒤에 한 단계 깊게 온다.
- 읽기: 파일이 없으면 빈 북마크. 머리 줄이 다르거나, 모르는 종류·필드 수·깊이·이스케이프가 있으면 손상된 것으로 보고 `bookmarks.txt.broken` 으로 보존한 뒤 빈 북마크로 시작한다.
  이 사실은 `BrowserApp.takeStorageProblem()` 으로 한 번 알린다.
- 쓰기(`TextFiles.write`): 같은 디렉터리의 `이름.tmp` 에 쓴 뒤 이름을 바꾼다. 이름 바꾸기가 기존 파일을 덮어쓰지 못하면(Windows) 기존 파일을 `이름.old` 로 옮겼다가 교체하고, 실패하면 되돌린다.
- 추가: Ctrl+D 는 활성 탭의 제목과 주소를 루트 끝에 더한다. 같은 주소가 이미 있으면 먼저 묻는다. 빈 페이지는 더하지 않는다. Ctrl+Shift+D 는 폴더 이름을 물어 창의 모든 탭(빈 페이지 제외)을 새 폴더에 더한다.
- 북마크 메뉴: 열릴 때마다 지금의 북마크로 다시 채운다. 폴더는 하위 메뉴이고 맨 위에 "모두 탭으로 열기"가 있다(10개를 넘으면 먼저 묻는다. 하위 폴더는 열지 않는다).
  항목을 그냥 누르면 활성 탭, Ctrl 을 누르고 누르면 새 탭, Shift 를 누르고 누르면 새 창에서 연다.
- 관리 창(`ui.BookmarkDialog`, Ctrl+Shift+B): 창마다 하나, 창을 소유자로 하는 비모달 대화 상자이다. 트리는 모델의 항목을 그대로 노드로 쓴다(`TreeModel` 구현).
  모델이 바뀌면 열려 있던 폴더와 선택을 유지한 채 트리를 다시 그린다. 이름·주소 필드와 "변경 저장", 새 폴더, 삭제(항목이 든 폴더는 먼저 묻는다), 위로, 아래로, 폴더로 옮기기(목록에서 고른다), 열기 세 가지, 가져오기, 내보내기.
- 가져오기·내보내기(`BookmarkHtml`): Netscape 북마크 파일 형식이다. 가져오기는 파일을 텍스트로만 읽어 `H3`(폴더), `HREF` 가 있는 `A`(북마크), `DL`(목록의 시작과 끝)만 처리한다. 주석은 건너뛰고,
  이름 안의 태그는 버리며, 문자 참조는 `&amp; &lt; &gt; &quot; &apos;` 와 숫자 참조만 푼다. 파일을 페이지로 보이지 않으므로 그 안의 스크립트는 실행되지 않는다.
  가져온 항목은 `Imported from 파일이름` 폴더에 넣어 기존 북마크와 섞이지 않게 한다. 내보내기는 이름과 주소의 `& < > " '` 를 이스케이프하고 줄 바꿈을 공백으로 적는다.
- 시험은 사용자 데이터 디렉터리를 사례마다 임시 디렉터리로 돌린다(`WindowCase`, `BrowserAppTest`). Maven 의 시험 실행도 `-Dswingwebpanel.desktopapp.home=target/test-home` 으로 실제 사용자 데이터를 건드리지 않는다.

### 7.6 설정 (P31-07, 2026-11-27)

- 설정 값은 모두 문자열이고(`model.Settings`) 쓰는 곳에서 형식을 맞춘다: `getBoolean`, `getInt(키, 최소, 최대)`, `getChoice(키, 허용 값)`. 이해할 수 없는 값은 그 키의 기본값으로 읽는다.
- 파일 `settings.properties`: `java.util.Properties` 형식의 UTF-8 텍스트. 첫 줄 뒤에 `settings.version=1` 이 오고 키마다 한 줄을 정해진 순서로 쓴다. ASCII 밖의 문자는 그대로 쓰고 역슬래시·줄 바꿈·탭·맨 앞 공백만 이스케이프한다.
  판 줄이 없거나 다른 판이거나 속성 형식으로 읽을 수 없으면 `settings.properties.broken` 으로 보존하고 기본값으로 시작한다. 모르는 키는 무시한다.
- 응용 프로그램이 설정의 리스너로 변경마다 저장한다. 창은 설정의 리스너로 막대 보이기를 적용하고 탭마다 `BrowserTab.applySettings()` 를 부른다.
- 탭에 적용하는 것: JavaScript 사용, 스크립트의 탭 닫기 허용, User-Agent(빈 값이면 컴포넌트의 원래 값), 기본 글꼴 네 종류와 두 크기, 기본 확대 비율. 달라진 것만 바꾼다.
  기본 확대 비율이 바뀌면 이전 기본값을 그대로 쓰던 탭만 따라가고, 사용자가 확대 비율을 바꾼 탭은 그대로 둔다. 다운로드 폴더는 탭의 컴포넌트가 `getDownloadDirectory()` 재정의로 설정을 읽는다(없거나 없는 폴더이면 컴포넌트의 기본값).
- 응용 프로그램의 기본값: 고정폭 크기 13(브라우저와 같게. 컴포넌트의 내장 값은 0 = 주변 글의 크기), 팝업 차단 켬, 팝업은 팝업 창, 새 창 요청 링크는 새 탭, 시작은 홈 페이지, 머리글·바닥글 인쇄 켬, 배경 인쇄 끔.
- 설정 대화 상자(`ui.SettingsDialog`): 모달, 구역 다섯 개(일반, 탭과 창, 글꼴, 콘텐츠, 인쇄). 컨트롤마다 설정 키 하나를 텍스트 값으로 읽고 쓰는 `Field` 로 묶는다. 확인은 모든 값을 `Settings.setAll` 로 한 번에 적는다(리스너는 한 번만 불린다).
  취소와 닫기는 아무것도 바꾸지 않는다. "기본값으로"는 컨트롤에만 기본값을 보인다. 글꼴 목록은 `(기본값)` 과 설치된 패밀리이고, 설정에 있지만 설치되지 않은 패밀리는 목록에 더해 값을 잃지 않게 한다. 미리보기는 표준 글꼴과 고정폭 글꼴을 고른 크기로 보인다.
- 스크립트의 탭 닫기: 탭의 컴포넌트가 `closeRootWindow()` 를 재정의해 그 탭만 닫는다(`BrowserTab.Listener.tabCloseRequested` → `BrowserWindow.closeTab`). 마지막 탭이면 창이 닫히고, 마지막 창이면 사용자가 닫을 때와 같이 프로그램이 끝난다.
- 시작(`BrowserApp.start(주소)`): 명령행 주소가 있으면 그것, 없으면 설정 `startup.page`(홈/빈 페이지/지난번의 창과 탭). 복원할 세션이 없으면 홈 페이지이다.
- 세션 `session.txt`: `끝내기` 명령은 닫기 전에 열린 모든 창의 탭 주소를 기억하고, 마지막 창을 따로 닫을 때는 그 창의 탭이 세션이 된다. 빈 페이지 탭은 빼고 주소만 기억한다(이력과 폼 내용은 복원하지 않는다). 최대 200개 탭.
- 창 위치: 창을 닫을 때 최대화되지 않았을 때의 경계와 최대화 여부를 설정(`window.*`)에 적는다. 다음 창은 그 크기와 위치로 열리고(제목 줄이 어느 화면에도 걸치지 않으면 위치는 플랫폼에 맡긴다) 추가 창은 30픽셀씩 비켜 둔다.
- 컴포넌트의 기본 글꼴 구현(계약 5.27): 일반 이름의 패밀리는 페이지의 `WebFontSet` 에 두어 `FontResolver` 가 배치 동안 읽는다(정적 상태 없음). 기본 크기는 `StyleResolver.setDefaultFontSize` 가 기준 컨텍스트의 글꼴 크기로 넘기고
  `StyleComputer` 가 루트의 시작 크기와 크기 키워드에 쓴다. 표준 글꼴과 고정폭 크기는 사용자 출처 스타일 시트(`FontDefaults.userStyleSheet()`)이다. `PageView` 가 자기가 만든 표현(프레임 포함)에 값을 전달한다.

### 7.7 새 탭·새 창·팝업 (P31-08, P31-04-b, 2026-11-27)

SwingWebPanel 쪽(계약 5.28):

- 문서 스레드는 `core.DocumentLoop`(참조 수를 세는 단일 스레드 실행기)로 옮겼다. `NavigationController` 는 자기 루프를 만들거나 받은 루프를 쓴다.
  닫힌 제어기의 대기 작업은 실행 직전에 걸러 버리고(`closed`), 마지막 사용자가 떠날 때만 스레드를 끝낸다.
- **계획의 기본안과 다르게** 세션을 먼저 만들고 Swing 부품을 나중에 붙이지 않는다. `SwingWebPanel` 의 생성자가 세션을 만들기 때문에,
  `window.open` 은 스크립트의 스레드에서 `createPopupPanel(PopupSeed)` 로 컴포넌트 전체를 만들고(화면에 붙기 전의 Swing 컴포넌트는 어느 스레드에서 만들어도 된다)
  보이는 일만 `invokeLater` 로 EDT 에 넘긴다. 호스트가 자기 하위 클래스를 쓰려면 이 팩터리를 재정의한다(앱의 `TabPanel`).
- 팝업은 여는 컴포넌트의 정책을 따른다: 재정의하지 않은 `configureSsl`, `getDownloadDirectory`, 스크립트 시간 한도, 오디오 팩터리, 새 창 확장 지점은 `creator` 에 위임한다.
- 관계는 문서 스레드에서 부른 `window.open` 에만 맺는다. 파싱 중인 스크립트는 로더 스레드에서 실행되므로(5절의 P30 확인 사실) 그 호출은 관계없는 창을 열고 null 을 준다.
- 팝업 차단은 `window.open` 안에서 판정한다. 링크·폼의 새 창은 호스트가 받을 때만 생기므로, 스크립트가 제출한 폼의 차단은 `WindowRequest.getPanel()` 에서 판정한다
  (새 창을 다루지 않는 호스트의 기존 동작, 곧 제자리 제출을 바꾸지 않기 위해서이다).
- `dispose()` 는 계약상 localStorage 를 비운다. 탭·팝업을 하나씩 닫는 호스트를 위해 `disposeKeepingStorage()` 를 추가했다. P31-04-a 의 탭 닫기가
  `dispose()` 를 써서 다른 탭의 localStorage 까지 지우던 것을 이번에 고쳤다(`ui.PopupsTest.testClosingATabKeepsLocalStorage`).
- 샌드박스 상속은 구현하지 않았다. 최상위 창이 샌드박스를 지니려면 스크립트 허용·불투명 오리진·폼·다운로드·최상위 이동 판정 열 군데를 고쳐야 한다.
  대신 안전한 쪽으로, 샌드박스를 벗어나도 되는 문서(`allow-popups-to-escape-sandbox`)만 새 창을 열게 했다. 그 밖의 샌드박스 문서는 전과 같이 동작한다.
- COOP: 응답을 받은 뒤 문서를 만들기 전에 `BrowserSession.applyOpenerPolicy` 가 그룹 전환을 판정한다. 그룹을 떠난 창은 다른 그룹의 스크립트에게 닫힌 창이다(`TopContext.isClosed()` 가 호출한 쪽의 그룹을 본다).
- 읽는 중인 창으로 보낸 `postMessage` 는 설치된(확정 전) 페이지가 받는다(`BrowsingContext.getMessageScripts()`). P27 의 호스트 메시지와 같은 대상 선택이다.

앱 쪽:

- 탭의 컴포넌트를 `ui.TabPanel`(최상위 클래스)로 뺐다. 페이지가 연 창의 컴포넌트는 탭보다 먼저 만들어지므로 탭은 나중에 묶는다(`bind`). `BrowserTab(app, panel)` 이 그런 컴포넌트를 받는다.
- 스크립트가 연 창은 설정 `popups.place` 에 따라 `ui.PopupWindow`(기본), 새 `BrowserWindow`, 새 탭(활성)에 보인다. 어디에 보이든 opener 관계와 `window.close()` 는 같다.
- `ui.PopupWindow`: 여는 브라우저 창이 소유한 비모달 `JDialog`. 제목, 읽기 전용 주소, 페이지, 상태 표시줄만 있다. 요청한 크기는 페이지 영역의 크기이다.
  브라우저 창을 닫으면 함께 닫힌다(실제 브라우저의 팝업과 다르다). 팝업의 페이지가 요청한 창과 문맥 메뉴는 소유한 브라우저 창이 처리한다.
- 새 창을 요청하는 링크·폼은 설정 `links.place` 에 따라 새 탭(활성) 또는 새 창. 사용자가 고른 것: Ctrl+클릭·가운데 단추는 새 탭(설정 `tabs.selectNew` 에 따라 활성), Shift+클릭은 새 창.
- 차단: 탭이 마지막으로 차단된 주소를 기억하고 상태 표시줄이 "팝업을 차단했습니다: 주소"와 "이 팝업 열기" 단추를 보인다. 페이지가 바뀌면 사라진다.
  단추로 연 창은 관계없는 창이다. 주소 없는 팝업(`window.open()` 뒤 `document.write`)은 다시 열 것이 없어 알림만 보인다.
- 문맥 메뉴: 링크 위에서는 새 탭·새 창에서 열기, 주소 복사. 이어서 뒤로·앞으로·새로고침·복사·모두 선택. 링크는 `HyperlinkListener` 의 ENTERED/EXITED 로 안다.
  페이지가 `contextmenu` 이벤트를 취소해도 메뉴가 보인다(컴포넌트가 그 결과를 알려 주지 않는다. 한계).
- 스크립트의 닫기: 호스트가 만든 탭은 설정(스크립트의 본 창 닫기 허용)에 따라 `closeRootWindow()`, 페이지가 연 탭·팝업은 `closeOpenedWindow()` 로 오며 둘 다 그 탭(팝업)만 닫는다.
  창의 마지막 탭이면 창이 닫히고, 마지막 창이면 프로그램이 끝난다.

