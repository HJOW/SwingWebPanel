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
| `src/main/java/.../tls13/crypto/` | 암호 기본 요소(바이트 도우미, 해시·HMAC·HKDF·PRF, AES-GCM, ChaCha20-Poly1305, X25519, NIST 곡선, RSA 서명 검증) |
| `src/test/java/.../tls13/` | 자체 실행기(`testing.TestRunner`)로 실행하는 시험. `Hex` 는 시험 벡터용 16진수 도우미 |

## 빌드와 시험 명령 (저장소 루트에서, Maven 은 JDK 8 이상으로 실행)

| 목적 | 명령 |
| --- | --- |
| 모듈 시험만 | `mvn -B -o -pl tls13 -am "-Dtest.filter=tls13" verify` |
| 시험 없이 JAR 만 | `mvn -B -o -pl tls13 -am -Dmaven.test.skip=true package` |

- 시험 실행기는 이름이 `*Test` 인 클래스의 `test*` 메서드를 실행한다. 종료 코드: 0 모두 통과, 1 실패 있음.
- 시험은 모듈 디렉터리(`tls13/`)를 작업 디렉터리로 실행된다.

## 실제로 실행해 본 JRE

- JDK 6u45(`1.6.0_45`, Windows 10): 컴파일과 시험.
