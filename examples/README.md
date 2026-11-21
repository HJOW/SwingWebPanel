# SwingWebPanel 예제

JDK 6u45가 필요합니다. 다른 경로를 사용하면 `JAVA6_HOME`을 지정합니다.

- Windows: `examples\run.bat StaticPageExample`
- sh: `examples/run.sh StaticPageExample`
- 화면 없는 자동 점검: 위 명령 끝에 `--smoke`를 붙입니다. 클래스패스 HTML/상대 CSS 로딩, 원문, 배치 높이와 실제 그리기 픽셀을 확인합니다.
- 편집과 끌어다 놓기: `examples/run.sh EditingExample`. `contenteditable` 영역, 끌 수 있는 항목, 놓는 곳, 텍스트 필드가 있는 페이지를 엽니다. 실제 키보드와 한글 입력기, 실제 마우스 끌기, 시스템 클립보드(Ctrl+C/X/V)는 자동 시험으로 확인하지 못했으므로 이 예제로 사람이 확인합니다(`--smoke` 는 페이지가 읽히는지만 확인합니다).
- 콘솔 출력 받기: `examples/run.sh ConsoleExample`. 단추가 `console.log/warn/error`, 잡히지 않은 예외, 처리되지 않은 promise 거부를 만들고 등록한 `ConsoleListener` 가 받은 `ConsoleEvent` 를 아래 텍스트 영역에 나열합니다(`--smoke` 는 이벤트가 도착하는지 확인합니다).
- 독립 JavaScript 엔진: `SWP_CLASSES=build/engine-classes examples/run.sh EngineExample`(Windows: `set SWP_CLASSES=build\engine-classes` 후 `examples\run.bat EngineExample`). 브라우저 컴포넌트나 AWT 없이 `JsEngine` 으로 스크립트 실행, Java 함수 주기, 살아 있는 Map/List 읽기, 함수 호출, promise 작업, 직접 만든 타이머, 오류 위치, 시간 제한을 보여 줍니다. `--smoke` 는 결과를 확인하고 한 줄을 출력합니다. 엔진 예제는 엔진 클래스만으로 컴파일됩니다.
- Java Scripting API(JSR 223): `SWP_CLASSES=build/engine-classes examples/run.sh script.ScriptEngineExample`. `ScriptEngineManager` 로 **고유한 이름** `swingwebpanel-js` 의 엔진을 찾아 `eval`, `Bindings`, 컨텍스트의 라이터, `Compilable`, `Invocable`/`getInterface`, `ScriptException` 위치를 보여 줍니다(하위 패키지 예제는 점으로 씁니다).
- 기본 API 창: `examples/run.sh BlankPageExample [시작 URL]`. `JScrollPane`, 상태/진행률 속성, 뒤/앞 이동, 새로고침과 창 종료 시 `dispose()`를 보여 줍니다.

정적 예제의 HTML/CSS는 `examples/resources`에서 예제 클래스패스로 복사합니다. 인터넷 연결이나 외부 라이브러리는 필요 없습니다.
누락된 이미지는 URL·alt 대체 표시를 확인하기 위한 자료입니다. 표의 열 폭/셀 병합 등 완전한 배치는 아직 P10-02 작업이며 이 예제로 전체 HTML/CSS 지원을 뜻하지 않습니다.
스크립트의 기본 실행·DOM 변경·포인터 클릭·계산 스타일/좌표 조회와 일반/defer 스크립트의 CSS 대기는 연결되어 있습니다. async/module 로딩 정책, 스크립트 이동, 키보드/폼/미디어 재생 등 남은 기능은 `AGENTS.md`와 `docs/FEATURE_MATRIX.md`를 참고합니다.
