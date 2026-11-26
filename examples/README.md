# SwingWebPanel 예제

빌드는 Maven과 JDK 8 이상으로 실행합니다. 컴파일과 예제 실행에는 JDK 6u45를 사용합니다. 설치 경로가 다르면 `JAVA6_HOME`을 지정합니다.

- Windows: `examples\run.bat StaticPageExample`
- sh: `examples/run.sh StaticPageExample`
- 화면 없는 자동 점검: 위 명령 끝에 `--smoke`를 붙입니다. 클래스패스 HTML/상대 CSS 로딩, 원문, 배치 높이와 실제 그리기 픽셀을 확인합니다.
- 편집과 끌어다 놓기: `examples/run.sh EditingExample`. `contenteditable` 영역, 끌 수 있는 항목, 놓는 곳, 텍스트 필드가 있는 페이지를 엽니다. 실제 키보드와 한글 입력기, 실제 마우스 끌기, 시스템 클립보드(Ctrl+C/X/V)는 자동 시험으로 확인하지 못했으므로 이 예제로 사람이 확인합니다(`--smoke` 는 페이지가 읽히는지만 확인합니다).
- 콘솔 출력 받기: `examples/run.sh ConsoleExample`. 단추가 `console.log/warn/error`, 잡히지 않은 예외, 처리되지 않은 promise 거부를 만들고 등록한 `ConsoleListener` 가 받은 `ConsoleEvent` 를 아래 텍스트 영역에 나열합니다(`--smoke` 는 이벤트가 도착하는지 확인합니다).
- Java와 페이지 사이 메시지: `examples/run.sh MessageExample` 또는 `examples\run.bat MessageExample`. 클래스패스 페이지의 준비 메시지를 받은 뒤 Swing 입력값을 보내며, 페이지에서 보낸 메시지는 EDT의 라벨에 나타납니다. `--smoke`는 `WebMessageEvent.reply`와 페이지의 응답을 화면 없이 확인합니다.
- 독립 JavaScript 엔진은 `examples/run.sh EngineExample`로 실행합니다. 예제 클래스와 엔진 JAR를 클래스패스에 두고 `JsEngine`으로 스크립트 실행, Java 함수 주기, Map/List 읽기, promise 작업, 타이머, 오류 위치와 시간 제한을 보여 줍니다. `--smoke`는 결과를 확인하고 한 줄을 출력합니다.
- Java Scripting API(JSR 223)는 `examples/run.sh script.ScriptEngineExample`로 확인합니다. `ScriptEngineManager`로 고유한 이름 `swingwebpanel-js`의 엔진을 찾아 `eval`, `Bindings`, 컨텍스트의 라이터, `Compilable`, `Invocable`/`getInterface`, `ScriptException` 위치를 보여 줍니다. 하위 패키지 예제는 점으로 씁니다.
- 기본 API 창: `examples/run.sh BlankPageExample [시작 URL]`. `JScrollPane`, 상태/진행률 속성, 뒤/앞 이동, 새로고침과 창 종료 시 `dispose()`를 보여 줍니다. 시작 URL 없이 실행하면 시작 페이지의 단추가 `window.close()`를 부릅니다. 예제는 자기 페이지에 한해 `setRootWindowClosingEnabled(true)`로 이를 허용하고 `closeRootWindow()`를 재정의해 창만 닫습니다(기본 동작은 `System.exit(0)`).

정적 예제의 HTML/CSS는 Maven이 `examples/src/main/resources`에서 예제 클래스패스로 복사합니다. 예제 페이지는 로컬에서 열립니다.
누락된 이미지는 URL·alt 대체 표시를 확인하기 위한 자료입니다. 표의 열 폭/셀 병합 등 완전한 배치는 아직 P10-02 작업이며 이 예제로 전체 HTML/CSS 지원을 뜻하지 않습니다.
스크립트의 기본 실행·DOM 변경·포인터 클릭·계산 스타일/좌표 조회와 일반/defer 스크립트의 CSS 대기는 연결되어 있습니다. async/module 로딩 정책, 스크립트 이동, 키보드/폼/미디어 재생 등 남은 기능은 `AGENTS.md`와 `docs/FEATURE_MATRIX.md`를 참고합니다.
