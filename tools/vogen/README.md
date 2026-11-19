# vogen

유니코드의 `VerticalOrientation.txt`(UAX #50, `Vertical_Orientation` 속성)에서 SwingWebPanel 의 자바 소스 `VerticalOrientationData.java` 를 만들어 주는
개발 보조 도구입니다. 세로쓰기에서 `text-orientation: mixed` 가 글자를 똑바로 세울지 눕힐지 정하는 표입니다.
제품 기능이 아니며 제품 JAR 에는 포함되지 않습니다.

## 사용법

1. 입력 파일을 내려받습니다(예: `tools/webfetch` 도구, 출처 정보는 `.meta` 파일로 함께 저장). JDK 6 은 HTTPS 를 읽지 못하므로 새 JRE 를 지정합니다.

```bash
WEBFETCH_JAVA_HOME="/c/Program Files/Java/zuu25" tools/webfetch/run.sh --out build/vo.txt \
    https://www.unicode.org/Public/UCD/latest/ucd/VerticalOrientation.txt 2> build/vo.txt.meta
```

2. 빌드합니다. JDK 6 이 필요합니다(`JAVA6_HOME` 으로 경로를 바꿀 수 있습니다).

```bash
tools/vogen/build.sh
```

Windows 에서는 `tools\vogen\build.bat` 을 사용합니다.

3. 실행합니다. 마지막 인수는 생성 파일을 쓸 디렉터리입니다.

```bash
tools/vogen/run.sh build/vo.txt src/org/duckdns/hjow/swingwebpanel/layout
```

생성된 파일 머리의 Javadoc 에 입력의 URL, 조회 날짜, 판본(Last-Modified, ETag)이 기록됩니다. 규격이 바뀌면 입력을 다시 받아 같은 명령을 다시 실행하세요.
생성 파일은 직접 고치지 않습니다.

## 출력 형식

- `VerticalOrientationData.CHUNKS`: 공백으로 구분한 16 진수 `시작-끝` 쌍. 똑바로 서는(`U`, `Tu`, `Tr`) 코드 포인트의 연속 구간이며 나머지(`R`)는 눕힙니다.
  파일에 적힌 배정된 코드 포인트 외에, 머리말이 정한 일부 블록의 미배정 코드 포인트도 똑바로 서는 것으로 계산합니다.

## 종료 코드

0 성공, 1 사용법 오류, 2 출력 디렉터리 오류.
