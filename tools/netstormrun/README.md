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

출력 디렉터리에는 일반 Swing 그리기를 거친 PNG가 들어갑니다. 콘솔과 타이밍 표는 표준 출력에 기록됩니다. 시작 화면에서는 도구가 클릭하고, 메인 메뉴를 5초간 유지하면 종료합니다.
