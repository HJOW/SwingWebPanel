# tlsvectors

TLS 모듈(`tls13`)의 암호 구현을 검증할 대조용 시험 벡터를 만드는 개발 도구입니다.
새 버전 Java 에 들어 있는 표준 암호 구현으로 값을 계산해 파일로 저장하고, TLS 모듈의 시험이 그 파일을 읽어 자신의 계산 결과와 비교합니다.

## 무엇을 만드나

AES-GCM, ChaCha20-Poly1305, X25519, NIST 곡선(P-256, P-384, P-521)의 ECDH 와 ECDSA, RSA 서명(PKCS #1 v1.5, PSS)의 입력과 기대 결과입니다.

## 사용법

저장소 루트에서 실행합니다. 빌드에는 JDK 6, 실행에는 Java 11 이상이 필요합니다.

```bat
tools\tlsvectors\build.bat
set TLSVECTORS_JAVA_HOME=C:\Program Files\Java\zuu25
tools\tlsvectors\run.bat tls13\src\test\resources\jca-vectors.txt
```

```sh
tools/tlsvectors/build.sh
TLSVECTORS_JAVA_HOME=/path/to/jdk tools/tlsvectors/run.sh tls13/src/test/resources/jca-vectors.txt
```

만든 파일의 첫 줄들에 계산에 쓴 Java 버전이 기록됩니다. 다시 실행하면 키 쌍과 서명은 새로 만들어지므로 파일 내용이 달라지지만 모두 올바른 벡터입니다.

## 실제 사이트의 인증서 사슬 기록

인증서 검증 시험에 쓰는 실제 HTTPS 사이트의 인증서 사슬도 이 도구로 기록합니다. 지정한 사이트에 접속해 서버가 보낸 인증서를 파일에 적습니다.

```bat
"%TLSVECTORS_JAVA_HOME%\bin\java" -cp tools\tlsvectors\build\classes tlsvectors.ChainCapture tls13\src\test\resources\real-chains.txt www.google.com github.com
```

## 주의

- 벡터 생성과 인증서 사슬 기록에는 최신 Java를 사용합니다.
- 이 도구는 제품에 포함되지 않습니다.

## 실제 HTTPS 연결 확인

벡터 생성과 별도로 Java 6·7에서 선택 TLS 모듈로 직접 서버에 연결해 볼 수 있습니다. 모듈과 도구를 먼저 빌드합니다.

```bat
"%JAVA6_HOME%\bin\java" -cp "tools\tlsvectors\build\classes;tls13\target\swingwebpanel-tls13.jar" tlsvectors.LiveProbe https://example.com/ https://expired.badssl.com/
```

각 URL의 TLS 버전, 암호 스위트, HTTP 상태 또는 인증서 거부 등의 실패 이유를 출력합니다. 연결 확인에는 HEAD를 사용합니다.
