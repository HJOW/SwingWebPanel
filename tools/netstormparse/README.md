# netstormparse

NetstormReborn의 번들과 ES 모듈 원본이 SwingWebPanel 자체 JavaScript 파서에 의해 구문 오류 없이 읽히는지 확인하는 개발 도구입니다.
게임 파일은 이 저장소로 복사하지 않으며, 지정한 외부 경로에서 직접 읽습니다.

## 준비

JDK 6이 필요하며 `JAVA6_HOME`으로 경로를 바꿀 수 있습니다. 먼저 엔진 JAR을 만듭니다.

```bat
build.bat
tools\netstormparse\build.bat
```

## 실행

```bat
tools\netstormparse\run.bat "H:\Workspace\git\NetstormReborn\webpj\www"
```

성공하면 번들 문자 수, inline module 수, 원본 모듈 수, static import 수와 오류 0을 출력합니다.
구문 오류 또는 import가 가리키는 파일이 없으면 종료 코드 1로 끝납니다. 입력 경로나 엔진 JAR가 없으면 종료 코드 2입니다.

위 명령의 마지막에 `--bundle-profile`을 붙이면 번들 하나의 파싱 시간, 최대 힙과 파싱 전후 힙 사용량을 출력합니다.
실제 Swing 페이지의 실행 시간과 화면은 [netstormrun](../netstormrun/README.md)에서 확인합니다.

