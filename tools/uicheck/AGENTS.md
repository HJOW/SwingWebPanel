# uicheck (AI용 문서)

## 목적

실제 Swing 창(`JFrame` + `JScrollPane` + `SwingWebPanel`)을 화면에 띄우고, 명령행에 적은 단계를 차례로 실행하면서 `java.awt.Robot` 으로 **실제 화면의 스크린샷**을 저장하는 개발 보조 도구이다.
화면 밖(오프스크린) 시험이 닿지 못하는 층(무거운 창, 플랫폼 글꼴, 실제 스크롤 막대, OS 마우스 입력)을 확인해 `docs/PROGRESS.md` P22-10 의 "자동 검증과 실제 확인의 구분"과
P09-08-b-2 같은 항목의 근거를 만든다. 제품 코드가 아니며 제품 JAR/시험은 이 도구에 의존하지 않는다(이 도구가 제품 클래스를 쓴다).
도구 출력(스크린샷)은 보조 자료이며 제품 기능 시험을 대신하지 않는다. 스크린샷은 AI 가 이미지로 직접 읽어(`Read` 도구) 눈으로 확인한다.

## 빌드와 실행

- 먼저 제품을 `./build.sh` 또는 `build.bat`로 빌드한다. 기본 제품 클래스패스는 `browser/target/swingwebpanel.jar`이다.
  `SWP_CLASSES`에 저장소 루트 기준의 다른 JAR 또는 클래스 디렉터리를 지정할 수 있다.
- 빌드: `tools/uicheck/build.sh` 또는 `tools\uicheck\build.bat` (JDK 6, `JAVA6_HOME`, 못 찾으면 종료 코드 1, 제품 클래스가 없으면 2)
- 실행: `tools/uicheck/run.sh [옵션] [단계...]` 또는 `tools\uicheck\run.bat ...` — 저장소 루트를 기준으로 실행한다. 클래스패스에 제품 JAR, `examples/target/classes`와 `examples/src/main/resources`를 둔다
  (그래서 `classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html` 같은 예제 페이지를 열 수 있다).

## 인수

`--out DIR`(필수, 스크린샷 디렉터리), `--size WxH`(내용 크기, 기본 800x600), `--realmouse`(클릭에 Robot 으로 실제 OS 마우스를 쓴다; 끝나면 포인터를 원래 위치로 되돌린다.
기본은 AWT 이벤트 큐에 마우스 이벤트를 넣어 사용자의 실제 포인터를 건드리지 않는다), 그리고 단계(앞에서부터 차례로):

| 단계 | 동작 |
| --- | --- |
| `--url U` | `setPage(U)` 를 부르고 진행률이 100 으로 400 ms 머물 때까지 기다린다(최대 30 초) |
| `--shot NAME` | 창의 스크린샷을 `DIR/NAME.png` 로 저장하고 `[shot] 경로 크기 page=... progress=... status=...` 를 출력한다 |
| `--scroll Y` | 세로 스크롤 막대를 `Y` 로 옮긴다 |
| `--click X Y` | 보이는 영역(viewport) 기준 좌표를 클릭한다(눌림/뗌/클릭) 뒤 이동이 끝나기를 기다린다 |
| `--back`, `--forward`, `--refresh` | 같은 이름의 `SwingWebPanel` 메서드를 부르고 기다린다 |
| `--wait MS` | 쉰다 |
| `--expect-title TEXT` | 수신 원문에 `<title>` 과 `TEXT` 가 있는지 확인한다(없으면 실패 계수 증가) |
| `--example NAME` | `org.duckdns.hjow.swingwebpanel.examples.NAME` 의 `main`을 실행해 그 창을 쓴다. 예제 클래스는 Maven이 `examples/target/classes`에 컴파일한다. |
| `--press TEXT` | 열려 있는 모든 창(팝업 대화상자 포함)에서 단추(글이 `TEXT` 인 `AbstractButton`)를 `doClick()` 으로 누르고 기다린다(예: `--press "<"`) |
| `--window NAME` | `--example` 와 같지만 브라우저 패널이 없는 예제(예: `DownloadPopupExample`)의 첫 보이는 JFrame 을 창으로 쓴다 |
| `--dialogshot NAME` | 보이는 첫 JDialog(예: 다운로드 팝업)의 내용 영역을 `DIR/NAME.png` 로 저장한다(최대 10 초 기다림) |
| `--wheel N` | Robot 으로 **실제 OS 마우스 휠**을 `N` 칸(양수 = 아래) 돌리고 `[wheel] notches=N scroll 전 -> 후` 를 출력한다(포인터는 되돌린다) |
| `--dispose` | 패널과 창을 폐기한다 |
| `--serve` | 내장 HTTP/1.0 서버(`EchoServer`, 127.0.0.1 의 빈 포트)를 켜고 `[serve] 주소` 를 출력한다. `tools/uicheck/pages/*.html` 을 내주고, `/echo...` 는 요청(메서드, 대상, Content-Type, UTF-8 본문)을 `[echo]` 줄로 출력하며 같은 내용을 페이지로 돌려주고, `/download?name=&size=&unknown=1&slow=1&fail=1` 은 첨부 파일(한글 `filename*`, 길이 없음, 약 4초, 절반 뒤 끊김)을 돌려준다. 이후 `--url` 값의 `{base}` 가 서버 주소로 바뀐다 |
| `--key SPEC` | Robot 으로 **실제 OS 키**를 누른다(`TAB`, `shift+TAB`, `ctrl+C`, `BACK_SPACE*2` 등; 이름은 `KeyEvent.VK_` 뒤). 키는 포커스를 가진 창(파일 선택기 등 대화상자 포함)으로 간다 |
| `--type TEXT` | ASCII 글을 실제 키로 친다(미국/한국 자판 배열). 입력기가 한글 모드이면 **Windows 한국어 입력기가 조합**한다(`gksrmf` → 한글) |
| `--hangul on\|off` | 포커스 소유자의 `InputContext` 를 한국어로 고르고 `setCharacterSubsets(HANGUL_SYLLABLES 또는 BASIC_LATIN)` 로 한글/영문 모드를 바꾼다(한/영 키 대신: JDK 6 Robot 은 `VK_KANA` 를 `Invalid key code` 로 거부). 페이지 패널(contenteditable)에서는 `off` 가 듣지 않았다(아래 제약) |
| `--realclick X Y` | `--realmouse` 없이도 그 클릭만 실제 마우스로 한다 |
| `--mousedown X Y`, `--mouseup` | 실제 왼쪽 단추를 누른 채 두었다가 뗀다(`:active` 확인) |
| `--hover X Y` | 실제 포인터를 보이는 영역의 점에 올려 둔다(호버 스타일, 링크 진입) |
| `--drag X1 Y1 X2 Y2` | 실제 왼쪽 단추로 12 단계에 걸쳐 끈다(텍스트 선택) |
| `--clickbutton PREFIX` | 보이는 모든 창에서 글이 `PREFIX` 로 시작하는 단추를 찾아(최대 10초) 실제 마우스로 누른다(예: `Save as`, `예`, `Close`) |
| `--log` | 패널의 sessionStorage/localStorage 항목을 `[storage]` 로 출력한다(확인용 페이지가 이벤트 기록을 남긴다) |
| `--expect-storage KEY VALUE` | 저장소 항목 값을 확인한다(다르면 실패 계수 증가) |
| `--hyperlinks` | 출력하는 `HyperlinkListener` 를 등록한다(`[hyperlink] TYPE url= desc= edt=`) |
| `--clipboard` | 시스템 클립보드의 형식, 글, HTML 을 출력한다 |
| `--focusinfo` | 포커스 소유자 컴포넌트 클래스를 출력한다 |
| `--expect-file PATH` | 파일이 있고 비어 있지 않은지 확인하고 크기와 첫 26 바이트를 출력한다 |
| (서버 경로) `/tone.wav?sec=&hz=&amp=` | `--serve` 의 서버가 22050 Hz 16비트 모노 사인파 WAV 를 만든다(오디오 장치 확인용; `pages/audio.html`, `audio2.html`) |

실제 마우스 단계(`--realclick`/`--hover`/`--drag`/`--clickbutton`/`--wheel`)는 처음 포인터 위치를 기억해 끝날 때 되돌린다. 표준 출력은 플랫폼 기본 인코딩(이 PC 는 MS949)이므로 `iconv -f CP949 -t UTF-8` 로 읽는다.
다운로드 기본 폴더와 `JFileChooser` 가 사용자의 실제 폴더를 쓰지 않도록 `UICHECK_JVM_OPTS="-Duser.home=<build 아래 빈 폴더>"` 로 실행한다(단, Windows 의 `JFileChooser` 는 `user.home` 과 무관하게 실제 문서 폴더에서 열린다: 그 스크린샷은 저장소에 넣지 않는다).

종료 코드: 0 성공, 1 단계/기대 실패 또는 예외, 2 사용법 오류, 3 화면 없음(headless). 스크린샷은 창의 **내용 영역만**(루트 패널; 제목 줄과 눈에 보이지 않는 창 테두리 제외) 담는다. 주변에 있는 다른 창의 화면 조각이 사진에 섞여 개인 정보가 저장소에 들어가는 것을 막기 위해서이다(전체 화면 캡처는 하지 않는다).
`--click X Y` 의 좌표는 스크롤 창의 보이는 영역(viewport) 기준이다(스크린샷 좌표에서 도구 모음 등 위쪽 구성요소의 높이를 뺀 값).

## 제약

- Windows RDP/콘솔 세션에서 실제 화면이 켜져 있어야 한다(잠긴 세션은 검은 화면이 찍힌다). 창은 약 5~20 초 동안 화면 맨 위에 나타난다.
- 실제 키·마우스를 움직이므로 실행 중에 사람이 컴퓨터를 쓰면 키가 다른 창으로 갈 수 있다. 한글 모드는 이 PC 의 입력 언어가 한국어(Microsoft IME, `0412`)여서 가능했다.
- `--hangul off` 는 폼 편집기(`JTextComponent`)에서는 듣지만 페이지 패널 자체(contenteditable)에서는 영문으로 돌아가지 않았다(원인 미확인; 실제 사용자는 한/영 키를 쓴다).
- 오디오를 **귀로 듣는** 확인은 이 도구가 하지 못한다. 클립보드는 다른 프로세스(PowerShell)로 넣고 읽어 교차 확인한다(`docs/progress/uicheck.md`).
- 도구는 Java 6 문법/API 로 작성했고(`java.awt.Robot`, `MouseInfo`, `java.awt.im.InputContext`, `java.net.ServerSocket`, `java.awt.datatransfer` 만 쓴다) JDK 6u45 로 컴파일·실행했다.

## 실행해 본 JRE

JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`), Windows 10 Pro(RDP 세션, 입력 언어 한국어). 사례와 결과는 `docs/progress/uicheck.md` 에 있다(2026-11-18 창·마우스, 2026-11-19 키보드·한글 IME·클립보드·폼·다운로드).
