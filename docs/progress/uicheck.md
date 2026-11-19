# uicheck 진행 기록 (P09-08-b-2 실제 Swing 창 확인, P22-10 자동 검증과 실제 확인의 구분)

도구: `tools/uicheck`(`AGENTS.md`/`README.md`). 실제 Swing 창에 `SwingWebPanel` 을 띄우고 `java.awt.Robot` 으로 **실제 화면**을 캡처한다. 조정자(AI)가 캡처를 이미지로 직접 읽어 눈으로 확인했다.
환경: Windows 10 Pro(RDP 세션), JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로 도구·제품·예제를 컴파일하고 실행했다. 날짜 2026-11-18.

## 실행한 사례와 결과 (직접 실행, 스크린샷은 `docs/progress/uicheck/`)

| 사례 | 명령(요약) | 결과 |
| --- | --- | --- |
| 정적 예제 페이지 표시 | `--url classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html --shot first` | 제목, 한글/English 문단, 목록, 파란 단추 링크, 표(캡션과 경계선), 실제 세로 스크롤 막대가 보인다(`static-first.png`) |
| 스크롤과 대체 표시 | `--scroll 400 --shot scrolled` | 표, "실패한 이미지" 상자(미지원 URL 문구와 대체 문구 "요청한 이미지가 없습니다."), 스크롤 확인 문단이 보인다(`static-scrolled.png`). 처음에는 긴 URL 이 공백이 없어 상자 가장자리에서 잘렸으나(발견), `DisplayCommands.Placeholder.wrapLine` 으로 상자 폭에 맞게 줄바꿈하도록 고쳐 같은 창에서 URL 이 끝까지 보이는 것을 확인했다(`example-broken-image-wrapped.png`, 시험 `PlaceholderWrapTest` 4건, 렌더링 시험 240건 통과) |
| 합성 마우스 클릭 | `--click 137 326`(AWT 큐에 이벤트를 넣음) | `second.html` 로 이동(제목 "두 번째 페이지"와 "처음 페이지로 돌아가기" 단추가 보인다) |
| 실제 OS 마우스 클릭 | `--realmouse --click 137 326` | 같은 이동이 일어난다(Robot 이 실제 포인터를 움직여 눌렀고 끝나면 포인터를 되돌렸다) |
| 뒤로/앞으로/새로고침 | `--back --forward --refresh` | 이력 순서대로 `index.html` ↔ `second.html`, 새로고침 후 같은 페이지, 진행률 100, 상태 "Done" |
| **StaticPageExample 의 실제 창** | `--example StaticPageExample --realmouse --click 137 326 --press "<" --press ">" --press "R"` | 예제 창(제목 "SwingWebPanel", 도구 모음 `<` `>` `R`, 주소 입력란, 상태 "Done", 진행 막대): 실제 마우스 클릭으로 `second.html`, `<` 로 `index.html`, `>` 로 `second.html`, `R` 새로고침, 스크롤 막대 이동(`example-*.png`: 내용 영역만 담도록 도구를 고친 뒤 다시 찍은 것) |
| **실제 마우스 휠** | `--example StaticPageExample --wheel 5` / `--wheel -3` | 스크롤 위치 0 → 240(5칸), 240 → 96(되돌림 3칸): JScrollPane 의 휠 단위(칸당 48 px)로 움직인다 |
| **실제 다운로드 팝업** | `--window DownloadPopupExample --press "Slow 3 MB file" --dialogshot ...` | 실제 `JDialog` 가 뜬다: 파일/출처/저장 위치(사용자 다운로드 폴더)/크기, 진행 막대와 백분율(12%, 41%), "392.0 KB of 3.0 MB - 557.6 KB/s", 상태 줄("Downloading..."), `Save as...` 와 `Cancel` 단추. `Cancel` 을 누르면 결과 줄이 "Cancelled" 가 되고 단추가 `Save as...`(비활성)와 `Close` 로 바뀐다. **스크린샷에 사용자 계정 경로가 보여 저장소에는 넣지 않았다.** |
| 폐기 | `--dispose` | 예외 없이 끝나고 종료 코드 0(패널 `dispose()` 후 창 `dispose()`) |

## 관찰한 것 (숨기지 않는다)

- **다운로드 팝업의 "Size:" 줄이 응답을 받은 뒤에도 `unknown` 으로 남는 결함**을 실제 팝업에서 발견해 고쳤다(`SwingDownloadDialog` 가 진행 보고에서 전체 크기를 알면 크기 줄도 갱신; 새 시험 `SwingDownloadDialogTest.testSizeRowFollowsTheProgress` 는 수정 전 소스에서 실패하는 것을 확인했다).

- 긴 URL 의 대체 상자 문구가 잘리던 결함은 이 확인에서 발견해 같은 날 고쳤다(위 표).
- 예제 창이 막 나타난 직후 **포인터를 올리자마자 누르는 첫 Robot 클릭**이 링크를 이동시키지 못한 사례가 한 번 있었다(포인터를 올려 400 ms 기다린 뒤 누르면 항상 이동). 합성 이벤트로는 같은 연속 동작이 항상 동작했다. 제품 쪽 경쟁(이동 이벤트와 누름의 처리 순서)인지 AWT/Robot 의 활성화 지연인지 **원인을 확인하지 못했다**.
- 한 번의 실행에 창이 약 5~20 초 화면 맨 위에 나타난다. 잠긴 세션이면 검은 화면이 찍힌다.

## 자동 검증과 실제 확인의 구분 (P22-10 의 기록)

| 층 | 자동 검증(직접 실행해 통과) | 실제 Swing/OS/장치에서 확인한 것 | 실제로 확인하지 못한 것 |
| --- | --- | --- | --- |
| 렌더링 | 화면 밖 `BufferedImage` 로 좌표·화소 시험(`SwingRenderingTest` 외 다수) | **이 PC 의 실제 창**(위 표): 글꼴(한글/English), 상자, 목록, 표, 단추, 스크롤 막대, 대체 문구 | 다른 OS/글꼴/DPI/배율, GPU 합성 환경 |
| 마우스 입력 | 실제 AWT `MouseEvent` 를 컴포넌트에 보내는 시험(`DomMouseEventTest`, `PageInputTest`, `PointerEventIntegrationTest`) | **실제 OS 마우스**(Robot): 링크 클릭 이동, 스크롤 막대, **휠 스크롤** | 끌기 선택, 우클릭 메뉴, 터치패드/터치 |
| 키보드 입력 | 합성 `KeyEvent` 시험(`KeyboardFocusTest`, `KeyboardCodeTest`) | 도구 모음 단추를 `doClick()` 으로 눌렀다(실제 키 입력은 아님) | 실제 키 입력(Tab 이동, 입력란 타이핑), **한글 IME**(조합/삭제/포커스 이동), 서식 있는 클립보드 |
| 오디오 | 가짜 출력 PCM 일치 | 이 PC 의 믹서(DirectSound 3개)로 실제 라인을 열어 **무음**을 쓰고 위치 시계가 끝에 닿는 것(`JavaSoundDeviceTest`, JRE 6u45) | **소리를 귀로 확인**, 음량/음소거, 속도 변환 음질, 여러 소리의 혼합 |
| 다운로드 팝업 | 대체한 protected 메서드로 흐름 시험(`DownloadIntegrationTest`), 실제 `JDialog` 생성/갱신(`SwingDownloadDialogTest`) | **실제 팝업을 띄워 눈으로 확인**(진행 중, 취소 후 상태) | `JFileChooser`(`Save as...`)와 덮어쓰기 확인 창의 실제 조작 |
| 창/수명 | `LifecycleTest`, `SwingWebPanelTest`(dispose 후 리스너 제거 등) | 실제 창에서 `dispose()` 후 예외 없이 종료 | 장시간 사용, 여러 창 |

위 표의 "확인하지 못한 것"은 `docs/PROGRESS.md` 의 "사람이 확인" 목록(한글 IME, 소리 청취, 실제 키보드/클립보드/팝업)과 같다. 환경이 없으므로 P22-10/P11-02/P11-10/P18-08 은 미완료로 둔다.

## 조정자 요청

- ARCHITECTURE 도구 목록(6절)에 `tools/uicheck` 를 추가했다. 새 작업이 렌더러/입력을 크게 바꾸면 `tools/uicheck/run.sh --example StaticPageExample ...` 를 다시 돌려 스크린샷을 확인하는 것을 권한다.
- 후속 후보: 첫 클릭 경쟁의 원인 조사.

## 2026-11-19 실제 키보드, 한글 IME, 링크, 폼, 파일 선택기, 다운로드 팝업, 클립보드, 오디오 장치 (조정자)

환경: Windows 10 Pro(RDP 세션, 입력 언어 한국어 Microsoft IME `0412`), JDK 6u45 로 제품(`BUILD_DIR=build/ui`)·도구를 빌드하고 실행. 도구를 넓혔다(`tools/uicheck`: `--serve`, `--key`, `--type`, `--hangul`, `--realclick`, `--hover`, `--drag`, `--clickbutton`, `--log`, `--hyperlinks`, `--clipboard`, `--focusinfo`, `--expect-file`; 확인용 페이지 `tools/uicheck/pages/*.html`). 다운로드 기본 폴더는 `UICHECK_JVM_OPTS=-Duser.home=<build/uicheck-home>` 로 저장소의 빌드 폴더로 돌렸다.

### 한글 IME 를 자동으로 실제 입력하는 방법(새로 알게 된 것)
JDK 6 의 `Robot.keyPress(KeyEvent.VK_KANA)`(한/영 키)는 `IllegalArgumentException: Invalid key code` 로 거부된다. 공개 API `InputContext.selectInputMethod(Locale.KOREA)` + `setCharacterSubsets({Character.UnicodeBlock.HANGUL_SYLLABLES})` 가 Windows 한국어 입력기를 한글 모드로 바꾸고, 그 뒤 Robot 의 실제 키(`gksrmf`)를 **Windows 입력기가 조합**한다(실험: `JTextField` 에 `ㅎ→하→한→확정, ㄱ→그→글→확정` 의 `InputMethodEvent` 가 와서 "한글"). 모드 바꾸기만 API 로 하고 조합은 실제 OS 입력기가 하므로 손으로 만든 `InputMethodEvent` 시험과 층이 다르다.

### 사례와 결과 (모두 직접 실행)
| 사례 | 결과 |
| --- | --- |
| 폼 입력란의 한글 조합(`input.html`: 클릭 → 한글 모드 → `gksrmf` Space `dlqfur` Backspace → Tab → `dkssud` Enter `gktpdy`) | 값 "한글 입려"(조합 중 Backspace 가 자모 `ㄱ` 만 지움), 텍스트 영역 "안녕\n하세" 계열, 조합 중 글자에 점선 밑줄과 포커스 테두리(`ime2.png`). **결함 4개를 찾아 고쳤다**(커밋 008): `compositionend` 의 `data` 가 비어 있음, 조합 중 `input`/`keyup` 의 `isComposing` 이 false, 폼 컨트롤에 `beforeinput` 없음, Space 확정이 빈 조합과 확정 두 이벤트로 와서 끝의 data 가 빔. 고친 뒤 같은 실제 입력에서 `compositionend a data=한`, `keyup ... composing=true`, `beforeinput insertCompositionText ... composing=true` 를 확인 |
| contenteditable 의 한글 조합(같은 입력) | 내용 "한글 입려 ". Space 확정이 빈 조합 하나를 더 만들던 결함을 고쳤다(커밋 010) |
| `:active` 와 포커스(`input.html` 의 단추, `--mousedown`/`--mouseup`) | 실제 단추를 누르고 있는 동안 `button:active` 초록 배경과 `:focus` 테두리(`2026-11-19-active-held.png`), 떼면 초록이 사라지고 포커스 테두리만 남음(`2026-11-19-active-released.png`), `click` 은 `isTrusted=true` |
| 링크 호버·진입·이탈·클릭·취소·차단·키보드(`links.html`, `--hyperlinks`) | 실제 포인터를 올리면 `:hover` 노란 배경(`links-hover.png`)과 EDT 의 `ENTERED`, 상태 줄에 주소, 벗어나면 `EXITED`. DOM 순서 `mouseover mousedown focus mouseup click`. `preventDefault` 링크는 이동 없음(계약대로 호스트는 `ACTIVATED` 를 받음), `mailto:` 는 요청 없이 차단, 일반 링크는 `second.html` 로 이동. 실제 Tab/Shift+Tab 으로 링크 사이 포커스 이동(`:focus` 빨간 테두리, `links-focus.png`), Enter 가 `click` 과 이동을 일으킴 |
| 폼 전송(`form.html`) | 한글 모드로 `rjator`(검색) 입력 후 실제 Enter: 암묵적 제출이 기본 단추를 제출자로 넣어 `POST /echo/post`, 본문 `q=%EA%B2%80%EC%83%89&hidden=%EC%88%A8%EA%B9%80+%EA%B0%92&agree=yes&pick=second&send=%EB%B3%B4%EB%82%B4%EA%B8%B0`(내장 서버가 받은 그대로) |
| 파일 선택(`<input type=file>`) | 실제 마우스로 "Choose File" → **실제 `JFileChooser`**(한국어 UI)에 경로를 실제 키로 입력하고 Enter → `change`(파일 1개 28바이트) → 실제 클릭으로 multipart 제출, 서버가 받은 본문에 `filename="upload.txt"`, `Content-Type: text/plain`, UTF-8 한글 내용. 선택기는 `user.home` 과 무관하게 실제 "문서" 폴더에서 열려 사용자 폴더 이름이 보이므로 그 스크린샷은 저장소에 넣지 않았다 |
| 다운로드 팝업(`dl.html`) | 한글 파일 이름(`filename*`) "한글 파일.txt" 표시와 저장, 크기 불명 응답(불확정 막대, 50000 바이트 저장), 끊긴 응답("Download failed: received 30000 of 60000 bytes", 부분 파일 없음), 같은 이름이 있으면 덮어쓰기 확인 창("예(Y)" 를 실제 클릭 → 교체), **실제 `Save as...`→`JFileChooser` 에서 다른 경로 선택**. 결함 3개를 찾아 고쳤다(커밋 009): ① 선택기가 열린 동안 전송이 끝나면 고른 경로를 무시하고 옛 경로에 저장하면서 팝업에는 새 경로를 보임(`DownloadControl.holdForChoice`; 고친 뒤 같은 실제 조작으로 고른 파일에 30000 바이트 저장, `dl-ko-done2.png`), ② 크기 불명 막대에 "0%", ③ 실패 때 낡은 진행(13%) 표시. 다운로드마다 요청이 두 번 가는 것(이동 응답을 버리고 다시 요청)은 API_CONTRACT 5.8 의 설계대로이다 |
| 클립보드: 페이지에서 복사 | 실제 끌기로 "복사할 굵은 글 끝" 선택 → 실제 Ctrl+C → `copy` 이벤트(대상 body, 선택 글). **다른 프로세스(PowerShell, .NET `Clipboard`)** 가 읽은 OS 클립보드: `UnicodeText` = "복사할 굵은 글 끝", `HTML Format`(CF_HTML 머리 `Version:0.9`, `StartFragment` 위치, UTF-8 조각 `<b>…</b>`; .NET 의 문자열 해석이 UTF-8 일부를 깨뜨리지만 구조와 굵게 표시는 확인) |
| 클립보드: 밖에서 붙여넣기 | PowerShell 이 UTF-8 CF_HTML(`<p>밖에서 <i>기울인</i> <b>굵은</b> 붙임</p>`)과 글을 넣고 실제 Ctrl+V: contenteditable 에 서식 그대로(`html-ed`), 텍스트 필드에는 글. 필드의 `paste` 이벤트가 `text/plain` 만 보이던 결함을 고쳤다(커밋 011, 이제 `text/html` 조각도). 참고: Windows PowerShell 5.1 의 `Set-Clipboard -AsHtml` 은 CF_HTML 을 UTF-8 이 아닌 ANSI 로 써서 한글이 깨진다(제품 결함 아님, 규격에 맞는 CF_HTML 로 다시 확인) |
| 오디오가 실제 장치에 나오는지(`audio.html`, `audio2.html`) | 귀 대신 **OS 오디오 엔진의 피크 미터**(Windows Core Audio `IAudioMeterInformation`, PowerShell 에서 읽음; 저장소 밖 측정 수단이며 제품·도구 코드가 아니다)로 쟀다. 대조: Windows 자체 `SoundPlayer` 로 진폭 0.1 의 440 Hz WAV 를 틀면 끝점 피크 0.1000. 패널의 `<audio>`(내장 서버의 진폭 0.1 사인파): 음량 1 → **0.1001**, 음량 0.5 → **0.0500**, 음소거 → 0, 음소거 해제 → 0.1001, 일시정지 → 0. 두 요소 동시 재생(440 Hz + 660 Hz, 각 0.1): **0.1906**(두 사인 합의 이론 최댓값)이 14.03 초(음원 길이) 동안. 즉 디코딩한 PCM 이 실제 출력 장치에 정확한 크기로 섞여 나간다 |
| 오디오 시작 지연 | 15초 쉬고 시작하면 첫 소리까지 `SoundPlayer` 4.4초, JDK 6 `SourceDataLine`(500 ms 버퍼) 4.3초(JVM 시작 포함), 150 ms 버퍼·2048 프레임 쓰기 6.2초, 패널 5.5~8.1초(전체 회귀가 같이 돌던 상태). 약 4.3초는 RDP 오디오 끝점이 깨어나는 이 환경의 지연이고 작은 버퍼가 약 2초를 더한다. 장치 소리가 나기 전에 `playing` 이 오는 것은 규격과 맞지만 `play()` 의 Promise 가 첫 소리 때까지 해결되지 않는 결함(규격은 `playing` 과 함께 해결)을 찾아 MEDIA-E 에 넘겼다 |

### 자동 검증과 실제 확인의 구분 (P22-10 표 갱신: 위 2026-11-18 표의 "확인하지 못한 것" 중 이번에 실제로 확인한 것)
| 층 | 이번에 실제 OS/장치에서 확인한 것 | 여전히 확인하지 못한 것 |
| --- | --- | --- |
| 키보드 | 실제 키(Robot `SendInput`)로 Tab/Shift+Tab 포커스 이동, 입력란·텍스트 영역·contenteditable 입력, Enter 제출, Backspace | 다른 자판 배열, 접근성 도구 |
| 한글 IME | Windows 한국어 입력기의 실제 조합(음절 넘김, 조합 중 Backspace, Space/Tab/Enter 확정, 포커스 이동) | 한/영 키 자체(모드는 `InputContext` 로 바꿈), 한자 변환 후보 창, 다른 IME(일본어/중국어) |
| 마우스 | 호버, 링크 클릭, 끌기 선택, 대화상자 단추 | 우클릭 메뉴, 터치 |
| 클립보드 | OS 클립보드를 다른 프로세스에서 넣고 읽기(글, CF_HTML) | 다른 앱(Word 등)과의 서식 왕복 |
| 파일 대화상자 | 실제 `JFileChooser` 로 업로드 파일 선택, 다운로드 `Save as`, 덮어쓰기 확인 창 | 선택기의 취소 단추(자동 시험만) |
| 오디오 | OS 오디오 엔진 피크 미터로 출력 크기·음량·음소거·일시정지·혼합 | **사람의 귀로 듣는 확인**(음색·잡음의 주관적 품질), 속도 변환 음질 |

스크린샷(사용자 정보가 없는 것만): `docs/progress/uicheck/2026-11-19-*.png`.
