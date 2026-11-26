# SwingWebPanel Browser (desktopapp)

SwingWebPanel 컴포넌트로 만든 Swing 웹 브라우저입니다. 외부 라이브러리 없이 Java 6 이상에서 동작합니다.
이 프로그램은 SwingWebPanel 의 공개 API 만 사용하므로, SwingWebPanel 을 자기 프로그램에 넣어 쓰는 방법의 예이기도 합니다.

> 개발 중입니다. 지금은 창 하나에 탐색 도구 모음과 상태 표시줄이 있습니다. 탭, 메뉴, 북마크, 설정 화면, 인쇄 등은 `AGENTS.md`(저장소 루트)의 P31 계획에 따라 추가됩니다.
> 현재 상태는 아래 "지금 되는 것"을 보세요.

## 요구 사항

- 실행: Java 6 이상(JRE 6u45 에서 확인).
- 빌드: Maven 3.8.8 이상과 JDK 8 이상(Maven 실행용), JDK 6(컴파일과 시험 실행용, `JAVA6_HOME` 으로 경로 지정).

## 빌드와 실행

저장소 루트에서 실행합니다.

```sh
desktopapp/run.sh                       # 빌드한 뒤 빈 페이지로 시작
desktopapp/run.sh https://example.com/  # 시작 주소 지정
```

Windows 에서는 `desktopapp\run.bat` 을 같은 방식으로 씁니다.

직접 빌드하고 실행하려면 다음과 같이 합니다.

```sh
mvn -B -pl desktopapp -am -Dmaven.test.skip=true package
java -jar desktopapp/target/swingwebpanel-desktopapp.jar [시작 주소]
```

`swingwebpanel-desktopapp.jar` 는 앱과 SwingWebPanel 을 합친 단일 실행 JAR 입니다. 다른 JAR 없이 이 파일 하나로 실행됩니다.

## 지금 되는 것

- 브라우저 창 하나에 페이지를 보여 줍니다(스크롤 포함). 시작 주소를 명령행 인수로 줄 수 있습니다.
- 제목 표시줄에 페이지 제목(없으면 주소)과 프로그램 이름을 보입니다.
- 탐색 도구 모음: 뒤로, 앞으로, 새로고침, 멈춤, 홈 버튼.
  - 뒤로·앞으로 버튼을 오른쪽 클릭하면 그 방향의 방문 기록이 나옵니다.
  - Shift 를 누른 채 새로고침을 누르면 캐시 없이 다시 읽습니다.
  - 멈춤 버튼은 페이지를 읽는 동안에만 켜집니다.
- 주소 표시줄: 주소를 입력하고 Enter 를 누르면 이동하고, Esc 를 누르면 원래 주소로 돌아갑니다.
  - `example.com` 처럼 스킴 없이 입력하면 `https://` 를 붙입니다(보안 연결이 실패하면 HTTP 로 다시 시도합니다).
  - `C:\폴더\파일.html` 같은 파일 경로를 입력하면 그 파일을 엽니다.
  - 입력 중인 글은 페이지가 바뀌어도 지워지지 않습니다.
- 상태 표시줄: 페이지의 상태 문구(링크 위에 마우스를 올리면 그 주소), 읽는 동안의 진행 막대, 확대 비율.
- 창을 닫으면 그 창의 페이지를 정리하고, 마지막 창을 닫으면 프로그램이 끝납니다.
- 화면의 글은 JVM 의 기본 언어가 한국어이면 한국어로, 아니면 영어로 보입니다(`-Dswingwebpanel.desktopapp.lang=ko` 또는 `en` 으로 지정 가능).

## 사용자 데이터

설정과 북마크는 `사용자 홈/.swingwebpanel/desktopapp/` 아래에 UTF-8 텍스트 파일로 저장할 예정입니다(P31-06, P31-07).
JVM 옵션 `-Dswingwebpanel.desktopapp.home=폴더` 로 위치를 바꿀 수 있습니다.

## 점검용 옵션

`java -jar swingwebpanel-desktopapp.jar --smoke` 는 창을 보이지 않고 페이지 하나를 읽은 뒤 `desktopapp smoke ok` 를 출력하고 종료 코드 0 으로 끝납니다.
빌드(`mvn verify`)가 패키징한 JAR 을 JDK 6 에서 이 옵션으로 실행해 봅니다.
