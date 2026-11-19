# vogen (AI용 문서)

## 목적

유니코드 데이터 파일 `VerticalOrientation.txt`(UAX #50 의 `Vertical_Orientation` 속성)에서 제품 소스의 표를 만들어 내는 개발 보조 도구이다.
제품 기능이 아니며 제품 코드(`src`)와 제품 JAR은 이 도구의 클래스에 의존하지 않는다.
계획 항목: P10-03-e(`text-orientation: mixed` 가 글자마다 눕힐지 똑바로 세울지 정하는 표를 규격 자료에서 생성하고 도구를 `tools/`에 둔다).

## 입력과 출력

- 입력: `VerticalOrientation.txt` — `https://www.unicode.org/Public/UCD/latest/ucd/VerticalOrientation.txt`.
  각 줄은 `코드포인트[..코드포인트] ; 값 # 설명` 이다. 값은 `U`, `R`, `Tu`, `Tr`. 파일에 없는 코드 포인트는 `R` 이다.
  머리말은 일부 블록(CJK, 한글, 사설 영역, 보조 한자 평면 등)의 **미배정** 코드 포인트가 `U` 가 기본값이라고 적는다.
  이 도구는 그 목록을 `VoGen.UNASSIGNED_UPRIGHT` 에 복사해 두고 적용한다(파일 판본이 바뀌면 머리말과 대조해 갱신한다).
- 입력 옆에 webfetch 도구가 쓴 `<입력>.meta` 파일이 있으면 `source-url`, `retrieved`, `last-modified`, `etag` 줄을 생성 소스의 Javadoc 에 기록한다.
  (webfetch 의 stderr 출력을 `VerticalOrientation.txt.meta` 로 저장해 둔다.)
- 출력(`출력디렉터리`): `VerticalOrientationData.java` — `org.duckdns.hjow.swingwebpanel.layout` 패키지의 패키지 비공개 클래스. `CHUNKS` 문자열 배열이며
  항목은 공백으로 구분한 16 진수 `시작-끝` 쌍이다. 똑바로 서는 값(`U`, `Tu`, `Tr`)의 코드 포인트만 연속 구간으로 합쳐 낸다(나머지는 `R`: 눕힘).
  CSS Writing Modes Level 3 5.1.2 절이 `U`, `Tu`, `Tr` 을 똑바로, `R` 을 눕혀서 쓰라고 한다. 한 상수의 길이는 6000 글자 이하로 나눈다.
  제품 빌드(`scripts/pkgtest.sh`)는 자바가 아닌 자원을 복사하지 않으므로 표를 자원이 아니라 자바 소스로 만든다.

## 빌드와 실행

- 소스: `tools/vogen/src/vogen/VoGen.java` (Java 6 문법, 외부 라이브러리 없음)
- 빌드: `tools/vogen/build.sh` 또는 `tools\vogen\build.bat` (JDK 6 필요. `JAVA6_HOME`, 기본 `C:\Program Files\Java\jdk1.6.0_45`. 못 찾으면 종료 코드 1)
- 실행: `tools/vogen/run.sh VerticalOrientation.txt src/org/duckdns/hjow/swingwebpanel/layout` 또는 `tools\vogen\run.bat ...`. 실행 JRE 는 `JAVA6_HOME`(없으면 기본 JDK 6 경로).
- 종료 코드: 0 성공 / 1 사용법 오류 / 2 출력 디렉터리 오류. 입력 파일이 잘못되면 예외와 함께 0 이 아닌 코드로 끝난다.
- 입력 내려받기: `WEBFETCH_JAVA_HOME=<새 JRE> tools/webfetch/run.sh --out build/vo.txt <URL> 2> build/vo.txt.meta` (JDK 6u45 는 HTTPS 를 읽지 못하므로 새 JRE 를 쓴다).

## 제약

- 생성 파일은 직접 고치지 않는다. 판본이 바뀌면 입력을 다시 받아(`tools/webfetch`) 이 도구를 다시 실행한다.
- 이 개발 환경의 Bash 입력은 `\\` 를 `\` 로 바꾸므로 소스를 고칠 때 Write/Edit 도구를 쓴다.
- 도구는 `Tr` 과 `Tu` 를 `U` 와 구별하지 않고 모두 "똑바로"로 낸다. 세로 글리프 대체 자형(vert 기능)이 없는 글꼴을 위한 합성은 하지 않는다.

## 마지막으로 생성한 판본

- `VerticalOrientation-18.0.0.txt`: source-url `https://www.unicode.org/Public/UCD/latest/ucd/VerticalOrientation.txt`, retrieved 2026-11-17,
  last-modified Tue, 01 Sep 2026 20:29:02 GMT, etag `W/"2ee40-65a71c4edff80-gzip"`, 데이터 줄 2499개, 93 구간, 332,725 코드 포인트.
- 실제로 실행해 본 JRE: JDK 6u45 `1.6.0_45` (sh). bat 은 같은 방식이나 이 환경에서는 실행해 보지 못했다.
