# uicheck

실제 Swing 창에 `SwingWebPanel` 을 띄우고, 정해 둔 순서(페이지 열기, 스크롤, 링크 클릭, 뒤로/앞으로/새로고침, 폐기)대로 실행하면서 창의 스크린샷을 저장하는 확인 도구입니다.
화면 밖에서 도는 자동 시험이 확인하지 못하는 부분(실제 창, 글꼴, 스크롤 막대, 실제 마우스 클릭)을 눈으로 확인할 때 씁니다.

## 사용 방법

1. 제품을 빌드합니다. (`./build.sh`, 또는 `BUILD_DIR=build/ui sh build.sh` 후 `SWP_CLASSES=build/ui/classes` 지정)
2. 도구를 빌드합니다: `tools/uicheck/build.sh` (Windows: `tools\uicheck\build.bat`)
3. 실행합니다: `tools/uicheck/run.sh --out build/uicheck --url classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html --shot first --scroll 400 --shot scrolled --dispose`
   (Windows: `tools\uicheck\run.bat ...`)

`--out` 디렉터리에 `first.png`, `scrolled.png` 같은 스크린샷이 저장됩니다. 실행하는 동안 창이 화면 맨 위에 잠시 나타납니다.

## 옵션과 단계

- `--out DIR` 스크린샷을 저장할 디렉터리(필수), `--size WxH` 창 내용 크기(기본 800x600)
- `--url 주소`, `--shot 이름`, `--scroll 위치`, `--click X Y`, `--back`, `--forward`, `--refresh`, `--wait 밀리초`, `--expect-title 글`, `--example 예제이름`(예제 창 사용, 먼저 `examples/run.sh StaticPageExample --smoke` 로 예제를 컴파일), `--press 단추글`, `--window 예제이름`(패널 없는 예제 창), `--dialogshot 이름`(팝업 대화상자 촬영), `--wheel 칸수`(실제 마우스 휠), `--dispose`
- `--realmouse` 클릭을 실제 마우스로 합니다(포인터는 클릭 뒤 원래 자리로 돌아옵니다). 주지 않으면 사용자의 마우스를 건드리지 않습니다.

종료 코드는 0(성공), 1(실패), 2(사용법 오류), 3(화면 없음)입니다.

## 실제 키보드, 한글 입력기, 클립보드, 폼, 다운로드 (2026-11-19 추가)

- `--serve` 내장 HTTP 서버(127.0.0.1, 빈 포트)를 켭니다. `tools/uicheck/pages` 의 확인용 페이지(`input.html`, `links.html`, `form.html`, `dl.html`)를 내주고,
  `/echo` 로 보낸 폼 요청을 그대로 보여 주며(표준 출력에도 `[echo]` 로 찍힘), `/download` 는 한글 이름·크기 불명·느린·끊기는 첨부 파일을 돌려줍니다.
  이후 `--url` 값의 `{base}` 가 서버 주소로 바뀝니다. 예: `--serve --url "{base}input.html"`
- `--key 지정` 실제 키를 누릅니다(`TAB`, `shift+TAB`, `ctrl+C`, `BACK_SPACE*3`, `ENTER` 등. 이름은 `KeyEvent.VK_` 뒤 부분).
- `--type 글` 실제 키로 ASCII 글을 칩니다. 한글 모드이면 시스템 입력기가 조합합니다(`gksrmf` → "한글").
- `--hangul on|off` 포커스를 가진 컴포넌트의 입력기를 한글/영문 모드로 바꿉니다(한/영 키 대신; JDK 6 의 Robot 은 그 키를 누를 수 없습니다).
- `--realclick X Y` 실제 마우스 클릭, `--hover X Y` 실제 포인터를 올려 둠, `--drag X1 Y1 X2 Y2` 실제 끌기, `--clickbutton 글` 어느 창이든(파일 선택기, 확인 창 포함) 글이 그렇게 시작하는 단추를 실제 마우스로 누름.
  실제 마우스를 쓴 단계가 있으면 도구가 끝날 때 포인터를 처음 자리로 되돌립니다.
- `--log` 페이지의 sessionStorage/localStorage 를 찍습니다(확인용 페이지가 이벤트 기록을 거기에 남깁니다). `--expect-storage 키 값` 값을 확인합니다.
- `--hyperlinks` 하이퍼링크 리스너를 달아 사건을 찍습니다. `--clipboard` 시스템 클립보드의 형식과 글을 찍습니다. `--focusinfo` 포커스를 가진 컴포넌트를 찍습니다. `--expect-file 경로` 파일이 있는지 확인합니다.
- 다운로드 기본 폴더가 사용자의 실제 Downloads 가 되지 않도록 `UICHECK_JVM_OPTS="-Duser.home=<빌드 아래 빈 폴더>"` 로 실행하는 것을 권합니다.

예(한글 입력): `tools/uicheck/run.sh --out build/uicheck --size 640x360 --serve --url "{base}input.html" --realclick 160 57 --hangul on --type gksrmf --key SPACE --log`

이 도구는 실제 마우스와 키보드를 움직이므로 실행하는 동안 컴퓨터를 만지지 마세요(키가 다른 창으로 갈 수 있습니다). 소리를 귀로 듣는 확인은 이 도구로 할 수 없습니다.

