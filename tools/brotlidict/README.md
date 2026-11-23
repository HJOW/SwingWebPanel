# brotlidict

Brotli 압축 데이터 형식(RFC 7932)의 텍스트에서 정적 사전(122,784 바이트), 단어 변환 121 개, 문맥 조회표를 읽어
SwingWebPanel 의 자바 소스 `BrotliData.java` 로 만들어 주는 개발 보조 도구입니다. 웹 글꼴 형식 WOFF2 를 읽는 Brotli 디코더가 이 표를 씁니다.
제품 기능이 아니며 제품 JAR 에는 포함되지 않습니다.

## 사용법

1. RFC 텍스트를 내려받습니다(`tools/webfetch` 도구, 출처 정보는 `.meta` 파일로 함께 저장).

```bash
WEBFETCH_JAVA_HOME="/c/Program Files/Java/zuu25" sh tools/webfetch/run.sh --out build/rfc7932.txt https://www.rfc-editor.org/rfc/rfc7932.txt
```

2. 빌드합니다. JDK 6 이 필요합니다(`JAVA6_HOME` 으로 경로를 바꿀 수 있습니다). Windows 에서는 `tools\brotlidict\build.bat` 을 사용합니다.

```bash
tools/brotlidict/build.sh
```

3. 실행합니다. 마지막 인수는 생성 파일을 쓸 디렉터리입니다.

```bash
tools/brotlidict/run.sh build/rfc7932.txt browser/src/main/java/org/duckdns/hjow/swingwebpanel/media/font
```

도구는 RFC 가 적어 둔 길이와 CRC-32(사전 `0x5136cb04`, 변환 `0x3d965f81`, 조회표 세 개)와 읽은 표를 비교하고, 하나라도 다르면 파일을 만들지 않고 오류로 끝납니다.
생성된 파일은 직접 고치지 않습니다.
