# sourcecheck - 소스 규칙 검사 도구

프로젝트 규칙을 자동으로 검사하는 도구입니다. 제품에 포함되지 않습니다.

검사 내용:

- 파일 인코딩: java/md/sh 는 UTF-8(BOM 없음), bat 는 MS949(UTF-8 한국어 혼입 금지, CRLF)
- 금지 API: `com.sun`/`sun` 패키지, JavaScript 엔진 연결(Rhino, Nashorn, `javax.script` 등), 외부 프로세스 실행, 네이티브 코드(JNI), Swing 내장 HTML 렌더러
- 네이티브/바이너리 파일(`.dll`, `.so`, `.c`, `.jar` 등)이 저장소에 들어갔는지
- Java 6 이후 구문/API(람다, 다이아몬드, try-with-resources 등) 사용 여부 (빠른 점검, 최종 증명은 JDK 6 컴파일)
- 모든 클래스/메서드/필드/상수의 영어+한국어 Javadoc
- 모든 반복문의 한국어 설명 주석
- 플랫폼 기본 인코딩에 기대는 파일 입출력

## 사용법

JDK 6 이 필요합니다(`C:\Program Files\Java\jdk1.6.0_45`, 다르면 `JAVA6_HOME` 지정).

```
tools\sourcecheck\build.bat
tools\sourcecheck\run.bat                   (기본 경로 전체 검사)
tools\sourcecheck\run.bat --only javadoc browser/src/main/java/org/duckdns/hjow/swingwebpanel/net
tools\sourcecheck\run.bat --selftest        (검사기 자체 시험)
```

위반은 `경로:줄: [규칙] 메시지` 로 출력되며 위반이 있으면 종료 코드 1 입니다.
정당한 예외 줄에는 `sourcecheck:allow` 를 주석으로 적습니다.

## 주의

이 개발 환경의 입력 도구는 `\uXXXX` 를 실제 문자로 바꿔 저장합니다. 보이지 않는 문자나 줄바꿈이 소스에 섞이지 않도록
특수 문자는 `(char) 0x2028` 형태로 쓰고, 문자열에 `\u0041` 텍스트 자체를 담아야 하면 `"\\u0041"` 로 씁니다.
