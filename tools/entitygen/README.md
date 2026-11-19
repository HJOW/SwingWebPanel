# entitygen

HTML 표준의 이름 있는 문자 참조 표(`entities.json`)와 Encoding 표준의 인코딩 라벨 표(`encodings.json`)에서
SwingWebPanel 의 자바 소스 두 개(`NamedCharacterData.java`, `EncodingLabelData.java`)를 만들어 주는 개발 보조 도구입니다.
제품 기능이 아니며 제품 JAR 에는 포함되지 않습니다.

## 사용법

1. 입력 파일을 내려받습니다(예: `tools/webfetch` 도구, 출처 정보는 `.meta` 파일로 함께 저장).
2. 빌드합니다. JDK 6 이 필요합니다(`JAVA6_HOME` 으로 경로를 바꿀 수 있습니다).

```bash
tools/entitygen/build.sh
```

Windows 에서는 `tools\entitygen\build.bat` 을 사용합니다.

3. 실행합니다. 마지막 인수는 생성 파일을 쓸 디렉터리입니다.

```bash
tools/entitygen/run.sh build/specs/html/entities.json build/specs/html/encodings.json src/org/duckdns/hjow/swingwebpanel/html
```

생성된 파일 머리의 Javadoc 에 입력의 URL, 조회 날짜, 판본(Last-Modified, ETag)이 기록됩니다. 규격이 바뀌면 입력을 다시 받아 같은 명령을 다시 실행하세요.
생성 파일은 직접 고치지 않습니다.

## 출력 형식

- `NamedCharacterData.CHUNKS`: 공백으로 구분한 `이름:코드포인트,...` 항목들(2231 개). 이름은 `&` 를 뺀 것이며 세미콜론이 있는 이름과 없는 옛 이름이 모두 들어 있습니다.
- `EncodingLabelData.CHUNKS`: `|` 로 구분한 `표준이름=라벨,라벨,...` 항목들(40 개).

## 종료 코드

0 성공, 1 사용법 오류, 2 출력 디렉터리 오류.
