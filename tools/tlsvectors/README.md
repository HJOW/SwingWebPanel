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

## 주의

- Java 6, 7 과 오래된 Java 8 에는 필요한 알고리즘이 없어 실행할 수 없습니다(오류 메시지와 함께 끝납니다).
- 이 도구는 제품에 포함되지 않습니다.
