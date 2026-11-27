# tlsvectors (AI용 문서)

## 목적

TLS 모듈(`tls13`, 루트 `AGENTS.md` 의 P32-03)의 암호 기본 요소를 **독립 구현과 대조**하기 위한 시험 벡터를 만든다.
더 새 JRE 의 표준 암호 공급자(JCA/JCE)로 계산하며, TLS 모듈의 코드는 쓰지 않는다. 제품 기능이 아니고 제품 JAR·시험 클래스는 이 도구에 의존하지 않는다.
생성한 파일 `tls13/src/test/resources/jca-vectors.txt` 를 저장소에 커밋해 두고, 시험 `crypto.JcaCrossCheckTest` 가 JDK 6 에서 읽는다.

## 빌드와 실행

- 소스: `tools/tlsvectors/src/tlsvectors/TlsVectors.java` (Java 6 문법, 외부 라이브러리 없음)
- 빌드: `tools/tlsvectors/build.sh` 또는 `tools\tlsvectors\build.bat` (JDK 6, `JAVA6_HOME`. 못 찾으면 종료 코드 1)
- 실행: `tools/tlsvectors/run.sh 출력파일` 또는 `tools\tlsvectors\run.bat 출력파일`
  - 실행 JRE 는 **`TLSVECTORS_JAVA_HOME`** 으로 지정한다(Java 11 이상). 지정하지 않으면 종료 코드 1.
  - JRE 6·7·8u202 에는 필요한 알고리즘(AES-GCM, ChaCha20-Poly1305, X25519, RSASSA-PSS 등)이 없어 종료 코드 2 로 끝난다.
- 보통의 사용: `tools\tlsvectors\run.bat tls13\src\test\resources\jca-vectors.txt`

## 인증서 사슬 기록 (ChainCapture)

같은 도구의 둘째 진입점 `tlsvectors.ChainCapture` 는 실제 HTTPS 서버가 내놓는 인증서 사슬을 기록한다(P32-04 의 경로 검증 시험 입력).
실행 중인 JRE 의 TLS 구현으로 연결하므로(그 구현이 사슬도 검증한다) 새 JRE 로 실행한다. 스크립트는 없고 직접 실행한다:

`"%TLSVECTORS_JAVA_HOME%\bin\java" -cp tools\tlsvectors\build\classes tlsvectors.ChainCapture tls13\src\test\resources\real-chains.txt www.google.com github.com ...`

- 출력: 한 줄에 `chain 호스트 기록시각(1970년부터의 밀리초) 프로토콜 인증서들(Base64 DER, 서버 인증서가 먼저)`. 머리 줄에 JRE 판.
- 종료 코드: 0 모든 호스트 기록 / 1 사용법 오류 / 2 닿지 못한 호스트가 있음.
- 인증서는 만료되므로 시험(`x509.TrustStoreTest`)은 기록한 시각으로 검증한다. 2026-11-28 에 14개 호스트를 기록했다(Zulu 25, 모두 TLSv1.3).
- 외부 서버에 접속하는 도구이다. 호스트 이름 외에 보내는 정보는 없다.

## 종료 코드

0 기록함 / 1 사용법 오류(인수, 실행 JRE 미지정) / 2 실행 중인 JRE 에 알고리즘이 없음

## 출력 형식

UTF-8 텍스트, 한 줄에 벡터 하나, 낱말은 공백 하나로 나눈다. `#` 로 시작하는 머리 줄에 생성한 JRE 판이 있다. 빈 값은 `-` 로 쓴다.

| 첫 낱말 | 나머지 낱말 |
| --- | --- |
| `gcm` | 키(16 또는 32바이트) nonce aad 평문 봉인결과(암호문+태그). 평문 길이 0~1000 |
| `chacha` | 키 nonce aad 평문 봉인결과 |
| `x25519` | 개인키 공개키 상대공개키 공유비밀 |
| `ecdh` | 곡선(`P-256`/`P-384`/`P-521`) 개인스칼라 상대X 상대Y 공유비밀 |
| `ecdsa` | 곡선 해시 Qx Qy 메시지 r s |
| `rsa-pkcs1` | 해시 n e 메시지 서명. 법은 2048, 2049, 3072, 4096비트 |
| `rsa-pss` | 해시 salt길이 n e 메시지 서명. salt 길이는 해시 길이, 0, 20 |

수(`n`, `e`, 좌표, `r`, `s`)는 앞의 0 을 뺀 16진수, 바이트 열은 소문자 16진수이다.

## 제약

- 대칭 암호의 입력(키, nonce, 메시지)은 고정 시드의 `java.util.Random` 으로 만들어 실행마다 같다. 키 쌍과 서명은 JRE 의 난수를 쓰므로 실행마다 달라진다(모두 유효하다).
- `GCMParameterSpec` 과 `Cipher.updateAAD` 는 Java 7 의 API 라서 Java 6 소스에서 리플렉션으로 부른다.
- TLS 1.2 PRF 와 HKDF 는 JRE 의 공개 API 가 없어 만들지 않는다(RFC 벡터와 상호 운용 시험으로 확인한다).
- 실제로 실행해 본 JRE: 빌드는 JDK 6u45, 실행은 Zulu OpenJDK 25(Windows 10). JDK 6u45 로 실행하면 종료 코드 2 임을 확인했다.
