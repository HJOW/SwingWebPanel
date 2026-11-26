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
