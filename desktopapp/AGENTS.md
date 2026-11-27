# desktopapp (AI용 문서)

## 목적

SwingWebPanel 로 만든 Swing 웹 브라우저 응용 프로그램이다(저장소 루트 `AGENTS.md` 의 P31). 제품 JAR(`swingwebpanel.jar` 등)에는 들어가지 않는 별도 Maven 모듈이다.
설계와 결정 기록은 `docs/progress/desktopapp.md` 에 있다. 작업 전에 그 문서와 루트 `AGENTS.md` 의 P31 을 읽는다.

## 규칙

- 루트 `AGENTS.md` 의 "주의사항"과 공통 작업 규칙을 그대로 따른다: Java 6 문법/API, 외부 라이브러리·네이티브 코드·`com.sun`/`sun` 금지,
  모든 클래스/메서드/필드에 영어·한국어 Javadoc, 반복문과 중요 분기에 한국어 주석, java/md/sh 는 UTF-8, bat 는 MSWIN949.
- **경계**: `org.duckdns.hjow.swingwebpanel` 루트 패키지의 공개 타입과 `SwingWebPanel` 의 보호 확장 지점만 쓴다.
  `core`, `js`, `layout`, `net` 등 내부 패키지를 import 하지 않는다. `AppBoundaryTest` 가 소스를 검사한다.
- SwingWebPanel 에 기능이 필요하면 브라우저 모듈을 고치고, 새 공개 메서드를 `docs/API_CONTRACT.md` 의 확장 메서드 절과 `docs/FEATURE_MATRIX.md` 에 기록한다.
- 화면 부품(`ui`)과 모델/제어 계층(`model`)을 나눈다. 모델/제어 계층은 창 없이 시험할 수 있어야 한다.
- 모든 Swing 조작은 EDT 에서 한다.

## 구조

| 경로 | 내용 |
| --- | --- |
| `pom.xml` | 모듈 POM. 부모의 JDK 6 컴파일 설정을 쓴다. `package` 에서 실행 JAR 을 만들고 `verify` 에서 JDK 6 으로 `--smoke` 실행 |
| `src/assembly/app.xml` | 앱 + `swingwebpanel` + `swingwebpanel-js` 클래스를 합친 실행 JAR 의 구성 |
| `src/main/java/.../desktopapp/Main.java` | 진입점. `[주소]` 또는 `--smoke` |
| `src/main/java/.../desktopapp/BrowserApp.java` | 프로세스 전체 상태(설정, 열린 창, 마지막 창이 닫힐 때의 종료) |
| `src/main/java/.../desktopapp/Texts.java` | UI 글의 언어 선택. 글은 쓰는 곳에 `Texts.of("English", "한국어")` 로 함께 적는다 |
| `src/main/java/.../desktopapp/model/` | `AddressInput`(주소 입력 정규화), `Settings`·`SettingsStore`·`SessionStore`(설정과 세션), `BookmarkModel`·`BookmarkStore`·`BookmarkHtml`(북마크), `ConsoleLog`(탭의 콘솔 메시지), `PageFiles`, `ZoomSteps`, `TextFiles` |
| `src/main/java/.../desktopapp/ui/BrowserMenu.java`, `Commands.java` | 메뉴 막대와 명령 이름. 모든 명령은 `BrowserWindow.perform(이름)` 으로 실행한다 |
| `src/main/java/.../desktopapp/ui/SettingsDialog.java`, `BookmarkDialog.java` | 설정 대화 상자(모달), 북마크 관리 창(비모달) |
| `src/main/java/.../desktopapp/ui/BrowserWindow.java` | 브라우저 창(JFrame). `NavigationBar.Actions` 와 `BrowserTab.Listener` 를 구현한다. 탭 목록(`JTabbedPane`)과 닫은 탭 목록을 가진다 |
| `src/main/java/.../desktopapp/ui/TabHeader.java` | 탭 줄의 탭 머리(제목, 읽는 중 표시, 닫기 단추) |
| `src/main/java/.../desktopapp/ui/BrowserTab.java` | 페이지 하나(SwingWebPanel + JScrollPane)와 그 상태(읽는 중인 주소, 알림 문구, 마우스 아래 링크, 차단된 팝업) |
| `src/main/java/.../desktopapp/ui/TabPanel.java` | 탭의 컴포넌트(`SwingWebPanel` 하위 클래스): 다운로드 폴더, 페이지가 여는 창·차단 알림·스크립트의 닫기를 탭에 넘긴다. 페이지가 연 창의 컴포넌트도 이 클래스이다(`createPopupPanel`) |
| `src/main/java/.../desktopapp/ui/PopupWindow.java` | 스크립트가 연 창을 보이는 팝업 창(브라우저 창이 소유한 비모달 `JDialog`) |
| `src/main/java/.../desktopapp/ui/FindBar.java` | 찾기 막대(상태 표시줄 위). 입력하는 즉시 `BrowserWindow.find` 를 거쳐 활성 탭의 `SwingWebPanel.find` 를 부른다 |
| `src/main/java/.../desktopapp/ui/SourceWindow.java`, `ConsoleWindow.java`, `HistoryDialog.java` | 페이지 원본(평문, 읽기 전용), 활성 탭의 콘솔 메시지(`model.ConsoleLog`), 활성 탭의 세션 이력을 보이는 보조 창. 모두 브라우저 창이 소유한 비모달 `JDialog` |
| `src/main/java/.../desktopapp/model/PaperFormat.java`, `PrintDocument.java` | 페이지 설정의 용지를 설정 글로 읽고 쓰기, 인쇄물의 쪽 여백에 머리글·바닥글을 더하는 `Pageable` |
| `src/main/java/.../desktopapp/ui/PrintPreview.java` | 인쇄 미리 보기(쪽 넘기기, 크기, 인쇄 단추). 쪽은 인쇄 작업이 받는 것과 같은 `Pageable` 이 그린다 |
| `src/main/java/.../desktopapp/ui/NavigationBar.java`, `StatusBar.java`, `Icons.java` | 탐색 도구 모음(버튼, 주소 표시줄, 이력 메뉴), 상태 표시줄, Java2D 로 그린 아이콘 |
| `src/test/java/.../desktopapp/ui/TestPages.java` | 창 시험 도우미: 임시 파일 페이지, 루프백 HTTP 서버, EDT 조건 대기 |
| `src/test/java/...` | 자체 실행기(`testing.TestRunner`, 엔진 모듈의 시험 JAR)로 실행하는 시험 |
| `run.sh`, `run.bat` | 빌드 후 JDK 6 의 `java -jar` 로 실행 |

## 빌드와 시험 명령 (저장소 루트에서, Maven 은 JDK 8 이상으로 실행)

| 목적 | 명령 |
| --- | --- |
| 앱 시험만 | `mvn -B -o -pl desktopapp -am "-Dtest.filter=desktopapp" test` |
| 실행 JAR 과 smoke 실행 | `mvn -B -o -pl desktopapp -am verify` (앞 모듈의 시험도 실행된다. 건너뛰려면 `-DskipTests` 는 smoke 도 건너뛴다) |
| 시험 없이 실행 JAR 만 | `mvn -B -o -pl desktopapp -am -Dmaven.test.skip=true package` |
| 실행 | `desktopapp/run.sh [주소]`, 또는 `java -jar desktopapp/target/swingwebpanel-desktopapp.jar [주소]` |

- 산출물: `desktopapp/target/swingwebpanel-desktopapp.jar`(실행 JAR), `desktopapp/target/swingwebpanel-desktopapp-<판>.jar`(앱 클래스만).
- 종료 코드(`--smoke`): 0 성공, 1 실패.
- 창 시험은 버튼을 `doClick()` 으로 누르고 창을 보이지 않게 만든다. 실제 마우스·키보드 확인은 `tools/uicheck` 로 따로 한다(P31-11, 기록 `docs/progress/uicheck.md` 2026-11-27 절).
- 창에 텍스트 필드를 더하면 `BrowserTab.releaseCommandKeys(필드)` 를 부른다. Swing 텍스트 필드가 편집에 쓰는 Ctrl+H 가 브라우저의 방문 기록 명령을 가로채지 않게 한다(`PageToolsTest.testHistoryKeyInTextFields`).
- 실제 창 확인에서 "인쇄..."와 "페이지 설정..."(운영 체제의 대화 상자)은 실행하지 않는다. 메뉴 항목은 방향키 횟수가 아니라 글이 일치하는 `--press` 로 누른다.
- 프레임이 필요한 시험은 headless JVM 에서 `Check.skip` 으로 건너뛴다. 건너뛴 경우 이유를 `docs/PROGRESS.md` 에 기록한다.
- 사용자 데이터 위치는 시스템 속성 `swingwebpanel.desktopapp.home` 으로 바꾼다. 시험과 smoke 실행은 `target` 아래를 쓴다.
- **인쇄 시험은 프린터에 닿지 않는다**: 프린터에 닿는 코드는 `BrowserApp.Printing` 의 기본 구현(`PrinterJob`) 하나뿐이고, 창 시험(`WindowCase`)은 그것을 `FakePrinting` 으로 바꾼다.
  새 시험도 `PrinterJob` 을 직접 쓰지 말고 쪽을 `BufferedImage` 에 그려 확인한다. 실제 출력 확인이 꼭 필요하면 PDF 프린터로만 한다(`docs/progress/uicheck.md`).

## 실제로 실행해 본 JRE

- JDK 6u45(`1.6.0_45`, Windows 10 과 Windows 11): 시험과 `java -jar ... --smoke`, 실제 창 확인(`tools/uicheck`).
- JDK 8u202(Windows 11): `java -jar ... --smoke`.
