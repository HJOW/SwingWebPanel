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
| `src/main/java/.../desktopapp/model/` | `AddressInput`(주소 입력 정규화), `Settings`(설정 값과 리스너) |
| `src/main/java/.../desktopapp/ui/BrowserWindow.java` | 브라우저 창(JFrame). `NavigationBar.Actions` 와 `BrowserTab.Listener` 를 구현한다. 탭 목록(`JTabbedPane`)과 닫은 탭 목록을 가진다 |
| `src/main/java/.../desktopapp/ui/TabHeader.java` | 탭 줄의 탭 머리(제목, 읽는 중 표시, 닫기 단추) |
| `src/main/java/.../desktopapp/ui/BrowserTab.java` | 페이지 하나(SwingWebPanel + JScrollPane)와 그 상태(읽는 중인 주소, 알림 문구) |
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
- 창 시험은 버튼을 `doClick()` 으로 누르고 창을 보이지 않게 만든다. 실제 마우스·키보드 확인은 `tools/uicheck` 로 따로 한다(P31-11).
- 프레임이 필요한 시험은 headless JVM 에서 `Check.skip` 으로 건너뛴다. 건너뛴 경우 이유를 `docs/PROGRESS.md` 에 기록한다.
- 사용자 데이터 위치는 시스템 속성 `swingwebpanel.desktopapp.home` 으로 바꾼다. 시험과 smoke 실행은 `target` 아래를 쓴다.

## 실제로 실행해 본 JRE

- JDK 6u45(`1.6.0_45`, Windows 10): 시험과 `java -jar ... --smoke`.
