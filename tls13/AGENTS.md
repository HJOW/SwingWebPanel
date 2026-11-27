# tls13 (AI용 문서)

## 목적

Java 6·7 에서 쓰는 TLS 1.3·1.2 클라이언트를 순수 Java 로 구현한 선택 모듈이다(저장소 루트 `AGENTS.md` 의 P32).
제품 JAR(`swingwebpanel.jar` 등)에 들어가지 않으며, 엔진·브라우저 모듈과 서로 컴파일 의존성이 없다.
설계와 결정 기록은 `docs/progress/tls13.md` 에 있다. 작업 전에 그 문서와 루트 `AGENTS.md` 의 P32 를 읽는다.

## 규칙

- 루트 `AGENTS.md` 의 "주의사항"과 공통 작업 규칙을 그대로 따른다: Java 6 문법/API, 외부 라이브러리·네이티브 코드·`com.sun`/`sun` 금지,
  모든 클래스/메서드/필드에 영어·한국어 Javadoc, 반복문과 중요 분기에 한국어 주석, java/md/sh 는 UTF-8, bat 는 MSWIN949.
- **경계**: JRE 표준 공개 API 만 쓴다. `engine`·`browser` 의 클래스, AWT/Swing 을 참조하지 않는다. `ModuleBoundaryTest` 가 컴파일된 클래스의 상수 풀을 검사한다.
  시험 코드만 엔진 모듈의 시험 지원 JAR(`testing.TestRunner`, `Check`)을 쓴다.
- **JVM 전역 상태를 바꾸지 않는다**: `Security.addProvider`, `SSLContext.setDefault`, `HttpsURLConnection.setDefault...` 를 쓰지 않는다.
- **보안 원칙**: 인증서 검증과 호스트 이름 검증을 끄는 선택지를 만들지 않는다. TLS 1.0/1.1, CBC·RC4·3DES 스위트, 정적 RSA 키 교환, 재협상, 세션 재개를 구현하지 않는다.
- 비밀 값을 다루는 코드(`crypto` 의 AES, GHASH, ChaCha20, Poly1305, X25519, 태그 비교)에는 비밀에 따른 분기와 배열 인덱스를 넣지 않는다.
  P-256/P-384 ECDH 는 `BigInteger` 를 쓰므로 상수 시간이 아니다(문서화한 한계).
- 암호 기본 요소를 고치면 해당 표준 시험 벡터 시험을 JDK 6u45 에서 다시 통과시킨다.

## 구조

| 경로 | 내용 |
| --- | --- |
| `pom.xml` | 모듈 POM. 부모의 JDK 6 컴파일 설정을 쓴다. 산출물 `target/swingwebpanel-tls13.jar` |
| `src/main/java/.../tls13/` | 공개 진입점: `Tls13SocketFactory`(`SSLSocketFactory`), `Tls13Config`(설정. 검증을 끄는 항목은 없다), `Tls13Exception`(실패의 종류 `Kind` 와 경보), `Tls13Failures`(리플렉션으로 부를 수 있는 실패 판정). `TlsSocket`·`TlsSession` 은 패키지 전용 JSSE 구현 |
| `src/main/java/.../tls13/proto/` | 프로토콜(모듈 내부용. 공개 계약이 아니다): `RecordLayer`(두 버전의 레코드 보호), `HandshakeIo`(메시지 재조립과 전사), `ClientHello`, `ServerHello`, `ClientHandshake`(TLS 1.3·1.2 상태 기계), `Connection`(핸드셰이크 뒤의 읽기·쓰기·경보·KeyUpdate), `CipherSuite`, `Parameters`, `ByteReader`·`ByteWriter` |
| `src/main/java/.../tls13/x509/` | 인증서: `Der`(엄격한 DER), `X509Cert`, `SubjectKey`, `SignatureAlgorithm`, `NameConstraints`, `HostNames`, `TrustRoot`, `TrustStore`, `PathValidator`, `X509Exception`(거부 이유) |
| `src/main/resources/.../x509/mozilla-roots.txt` | 내장 루트 목록. `tools/cabundle` 이 CCADB 보고서로 만든다. **손으로 고치지 않는다. 자동 갱신도 없다** |
| `src/main/resources/META-INF/LICENSE-ccadb.txt` | 루트 목록의 출처 고지와 CDLA-Permissive-2.0 문구. JAR 에 함께 들어가야 한다 |
| `src/main/java/.../tls13/crypto/` | 암호 기본 요소: `Bytes`, `HashAlg`(해시·HMAC), `Hkdf`, `KeySchedule13`, `Prf12`, `Aead`·`Aes`·`Ghash`·`AesGcm`, `ChaCha20Poly1305`·`Poly1305`, `X25519`, `EcCurve`·`Ecdh`·`Ecdsa`, `RsaVerify` |
| `src/test/java/.../tls13/` | 자체 실행기(`testing.TestRunner`)로 실행하는 시험. `Hex`(16진수), `Rfc8448`(RFC 8448 추적 읽기), `Vectors`(줄 단위 벡터 자료 읽기), `DerWriter`·`TestKey`·`CertBuilder`(JRE 6 에서 시험용 키와 인증서를 만들고 서명한다)는 도우미. `proto/ScriptedServer`(결함을 주입할 수 있는 시험용 TLS 1.3·1.2 서버. 루프백 소켓, 자체 스레드)와 `proto/TlsFixture`(시험용 루트·서버 인증서·클라이언트 연결)는 연결 시험의 도우미 |
| `src/test/resources/` | `rfc8448.txt`(RFC 원문), `ec-vectors.txt`(RFC 6979·5903 에서 뽑은 값), `jca-vectors.txt`(`tools/tlsvectors` 가 더 새 JRE 로 만든 대조 벡터. 다시 만들면 내용이 달라진다), `real-chains.txt`(`tools/tlsvectors` 의 `ChainCapture` 가 기록한 실제 서버의 인증서 사슬. 기록한 시각으로 검증한다) |

## 빌드와 시험 명령 (저장소 루트에서, Maven 은 JDK 8 이상으로 실행)

| 목적 | 명령 |
| --- | --- |
| 모듈 시험만 | `mvn -B -o -pl tls13 -am "-Dtest.filter=tls13" verify` |
| 시험 없이 JAR 만 | `mvn -B -o -pl tls13 -am -Dmaven.test.skip=true package` |

- 시험 실행기는 이름이 `*Test` 인 클래스의 `test*` 메서드를 실행한다. 종료 코드: 0 모두 통과, 1 실패 있음.
- 시험은 모듈 디렉터리(`tls13/`)를 작업 디렉터리로 실행된다.
- `JsseInteropTest`는 `SWP_TLS_SERVER_JAVA_HOME`에 Java 11 이상(검증판 Zulu 25)을 지정하면 독립 루프백 JSSE 서버를 실행한다. 경로를 지정하지 않거나 실행 파일이 없으면 이유를 출력하고 해당 시험을 건너뛴다.
- 실환경 확인에는 `tools/tlsvectors`의 `LiveProbe`를 쓴다. 자동 회귀 시험은 공개 사이트를 호출하지 않는다.
- `swingwebpanel-tls13-tests.jar`는 desktopapp의 브라우저 연동 시험에 루프백 서버를 제공한다. 실행 JAR에는 제품 클래스와 루트 목록 및 라이선스만 포함한다.

## 실제로 실행해 본 JRE

- JDK 6u45(`1.6.0_45`, Windows 10): 컴파일과 시험.
- JDK 7u80, JDK 8u202, Zulu 25: JDK 6 으로 컴파일한 클래스로 `proto` 시험(핸드셰이크, 레코드, 소켓 팩터리, `HttpsURLConnection` 연동)을 실행했다.
- JDK 6u45·7u80·8u202: Zulu 25의 기본 JSSE 서버와의 상호 운용을 포함한 전체 모듈 회귀 97건을 실행했다.
