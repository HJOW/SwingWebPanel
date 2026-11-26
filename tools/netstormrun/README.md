# NetStorm 실행 계측

외부 NetStorm 웹 샘플의 번들 또는 ES 모듈 진입점을 SwingWebPanel로 실행합니다. 게임 적재 진행, 프레임 콜백 시간, 다운로드한 자산 크기와 콘솔을 기록합니다. 외부 샘플은 원래 위치에서 읽습니다.

먼저 제품을 빌드한 뒤 JDK 6으로 이 도구를 컴파일합니다.

```powershell
tools\netstormrun\build.bat
tools\netstormrun\run.bat "H:\Workspace\git\NetstormReborn\webpj\www" index build\netstorm6 90
$env:NETSTORM_JAVA_HOME = 'C:\Program Files\Java\jdk1.7.0_80'
tools\netstormrun\run.bat "H:\Workspace\git\NetstormReborn\webpj\www" debug build\netstorm7 120
```

인수는 샘플의 `www` 디렉터리, 진입점, 출력 디렉터리, 최대 실행 시간(초) 순서입니다. `index`는 번들, `debug`는 ES 모듈 진입점입니다. 유닉스 계열에서는 `build.sh`와 `run.sh`를 사용합니다.

마지막에 스크립트 작업의 시간 한도(밀리초)를 선택적으로 붙일 수 있습니다. 예를 들어 `120 60000`은 최대 실행 시간 120초, 해당 진입점의 작업 한도 60초입니다.

실제 Swing 창을 확인하려면 그 뒤에 `--window`를 붙입니다. 이 모드는 데스크톱에서 시작 및 환영 버튼을 직접 클릭하고 다음 대화가 표시됨을 확인합니다.

출력 디렉터리에는 시작과 마지막 상태의 PNG가 저장됩니다. 창 모드는 처음 환영 화면도 캡처합니다. 콘솔, 타이밍, Java Sound 믹서 수와 게임이 보고한 음악·효과음 상태는 표준 출력에 기록됩니다. 메인 메뉴를 5초간 유지해야 성공으로 기록합니다. 스피커에서 들리는 소리는 실제 장치에서 확인해야 합니다.
