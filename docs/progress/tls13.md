# tls13 모듈 설계와 진행 기록 (P32)

Java 6·7용 TLS 1.3·1.2 자체 구현 모듈 `tls13` 의 범위, 구조, 결정 사항과 검증 결과를 기록한다.
계획은 `AGENTS.md` 의 P32, 공개 API 계약은 `docs/API_CONTRACT.md`, 기능 판정은 `docs/FEATURE_MATRIX.md` 를 따른다.

## 1. 범위와 결정 (P32-01, 2026-11-27 확정)

### 1.1 사용자 결정

| 항목 | 결정 |
| --- | --- |
| 구현 위치 | 엔진·브라우저에 내장하지 않는 별도 Maven 하위 모듈 |
| TLS 버전 | TLS 1.3 과 TLS 1.2 (클라이언트 전용) |
| 브라우저 연결 | SwingWebPanel 의 메서드로 선택적으로 불러온다(기본 꺼짐) |
| desktopapp | 실행 중인 Java 가 6·7 일 때만 모듈을 쓴다 |
| 루트 인증서 | 작업 시점의 최신 Mozilla CA 목록(CCADB 공개본, CDLA-Permissive-2.0). 자동 갱신 없음 |
| 라이선스 | 모듈은 프로젝트와 같은 Apache 2.0. MPL 2.0 인 NSS `certdata.txt` 는 쓰지 않는다 |

### 1.2 모듈

| 항목 | 결정 |
| --- | --- |
| 디렉터리 | 저장소 루트의 `tls13/` (부모 POM `<modules>` 에서 `engine` 뒤, `browser` 앞) |
| Maven 좌표 | `org.duckdns.hjow:swingwebpanel-tls13`, 판은 부모와 같다 |
| 산출물 | `tls13/target/swingwebpanel-tls13.jar` |
| 패키지 | `org.duckdns.hjow.swingwebpanel.tls13` (공개 타입), 하위 패키지 `crypto`, `x509`, `proto` (구현) |
| 의존성 | 실행·컴파일 의존성 없음(JRE 표준 API 만). 시험 범위에서만 엔진 모듈의 시험 지원 JAR(`testing.TestRunner`, `Check`)을 쓴다 |
| 경계 | 모듈의 클래스는 `engine`·`browser`·AWT/Swing 을 참조하지 않는다. 시험이 컴파일된 클래스의 상수 풀을 검사한다 |
| 제품 JAR | `swingwebpanel.jar`, `swingwebpanel-js.jar`, `swingwebpanel-browser.jar` 에 넣지 않는다. desktopapp 실행 JAR 에만 합친다 |

### 1.3 공개 진입점 (이름은 기본안)

| 타입 | 역할 |
| --- | --- |
| `Tls13SocketFactory` | `javax.net.ssl.SSLSocketFactory` 구현. `getInstance()`(기본 설정의 공유 인스턴스)와 `new Tls13SocketFactory(Tls13Config)` |
| `Tls13Config` | 신뢰 저장소(기본 루트 목록 또는 호스트의 `KeyStore`), 허용할 버전·스위트·그룹. 변경 불가능한 값 객체 |
| `Tls13Failures` | 예외가 "협상 실패"(공통 버전·스위트 없음)인지 알려 주는 정적 판정(`isNegotiationFailure(Throwable)`). 브라우저가 리플렉션으로 부른다 |

- JSSE 공급자(`SSLContextSpi`)로 등록하지 않는다. JVM 전역 상태를 바꾸지 않는다.
- 연결되지 않은 `createSocket()` 은 `SocketException`(원인 `UnsupportedOperationException`)을 던진다. JRE 6u45 의 `HttpsURLConnection` 은 이때 일반 소켓을 연결한 뒤
  `createSocket(socket, host, port, true)` 로 호스트 이름을 넘기므로 SNI 를 보낼 수 있다(P32-05-e 에서 7u80 도 확인한다).

### 1.4 프로토콜 범위

TLS 1.3 (RFC 8446):

| 항목 | 지원 |
| --- | --- |
| 암호 스위트 | `TLS_AES_128_GCM_SHA256`, `TLS_AES_256_GCM_SHA384`, `TLS_CHACHA20_POLY1305_SHA256` |
| 그룹 | `x25519`(첫 key_share), `secp256r1`, `secp384r1`(HelloRetryRequest 로 요청받으면 보냄) |
| CertificateVerify 서명 | `rsa_pss_rsae_sha256/384/512`, `ecdsa_secp256r1_sha256`, `ecdsa_secp384r1_sha384` |
| 확장 | `server_name`, `supported_versions`, `supported_groups`, `key_share`, `signature_algorithms`, `signature_algorithms_cert`, ALPN(`http/1.1`) |
| 처리 | HelloRetryRequest, 미들박스 호환 모드, KeyUpdate, NewSessionTicket(버림), 인증서 요청(빈 Certificate), 경보 |
| 하지 않음 | 세션 재개(PSK), 0-RTT, 클라이언트 인증서, 서버 역할, 하이브리드 양자 내성 그룹 |

TLS 1.2 (RFC 5246, RFC 9325 권고):

| 항목 | 지원 |
| --- | --- |
| 암호 스위트 | ECDHE_ECDSA/ECDHE_RSA × AES_128_GCM_SHA256, AES_256_GCM_SHA384, CHACHA20_POLY1305_SHA256 (여섯 개) |
| 그룹 | `x25519`, `secp256r1`, `secp384r1`, 점 형식은 비압축만 |
| ServerKeyExchange 서명 | `rsa_pkcs1_sha256/384/512`, `rsa_pss_rsae_sha256/384/512`, `ecdsa_secp256r1_sha256`, `ecdsa_secp384r1_sha384` |
| 확장 | `extended_master_secret`(항상 제안, 서버가 안 써도 연결), `renegotiation_info`(빈 값), `ec_point_formats` |
| 하지 않음 | 세션 재개, 재협상(HelloRequest 는 `no_renegotiation` 경고로 거절), 클라이언트 인증서, CBC·RC4·3DES, 정적 RSA, DHE |

- ClientHello 는 `supported_versions` 로 1.3 과 1.2 를 제안한다. 서버가 1.2 를 고르면 ServerHello random 의 다운그레이드 표식(RFC 8446 4.1.3)을 검사한다.
- 서버가 TLS 1.1 이하를 고르면 협상 실패로 끝낸다(브라우저가 JRE 기본 JSSE 로 한 번 다시 시도, P32-07).

### 1.5 남은 세부 사항의 결정 (이번에 확정)

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| Ed25519 | 구현하지 않는다(`ed25519` 서명을 제안하지 않음) | 계획에서 선택 사항이다. Mozilla 루트 목록에 Ed25519 루트가 없고 공개 CA 가 웹 서버용으로 발급하지 않는다 |
| P-521 | 인증서 서명 검증(`ecdsa_secp521r1_sha512`)만 지원한다. 키 교환 그룹으로는 제안하지 않는다 | 일부 CA 사슬에 P-521 서명이 있을 수 있다. 검증은 공개 값만 다루므로 일반 곡선 코드로 충분하다 |
| OCSP 스테이플링 | 요청하지 않는다(`status_request` 를 보내지 않음). 폐기 확인(OCSP/CRL)을 하지 않는다 | 계획의 기본안. 한계로 문서에 적는다 |
| SHA-1 서명 | 사슬의 인증서 서명과 핸드셰이크 서명 모두 거부한다. 신뢰 앵커(루트)의 자체 서명은 검증하지 않으므로 무관하다 | RFC 9155, 브라우저 정책 |
| RSA 키 길이 | 2048비트 미만의 RSA 키를 가진 인증서와 신뢰 앵커를 거부한다. EC 는 P-256/P-384/P-521 만 받는다 | CA/B Forum 기준 |
| 인증서 사슬 길이 | 서버가 보낸 인증서는 최대 10개, 구성한 경로는 최대 8단계 | 자원 고갈 방지 |
| 유효 기간 시계 | `System.currentTimeMillis()`. 설정으로 시험용 시계를 줄 수 있다 | 만료 사례 시험 |
| 레코드 크기 | 평문 최대 2^14, 보내는 레코드도 2^14 로 나눈다. `max_fragment_length`/`record_size_limit` 은 제안하지 않는다 | 단순성 |
| 난수 | `java.security.SecureRandom`(JRE 기본). 시험은 결정적 난수원을 주입한다 | RFC 8448 추적 재현 |
| SSLSession | 소켓마다 하나. 세션 캐시와 재개가 없으므로 `getSessionContext()` 는 null | 범위 |
| 닫기 | `close()` 는 `close_notify` 를 보내려 시도한 뒤(쓰기 잠금을 기다리지 않음) 아래 소켓을 닫는다. 잘린 연결(close_notify 없는 EOF)은 읽기에서 `SSLException` 이 아니라 EOF 로 돌려준다(HTTP 는 길이로 본문 끝을 판정한다) | `HttpsURLConnection` 과 실제 서버의 동작에 맞춤. 한계로 문서에 적는다 |

### 1.6 암호 구현 방침

| 기본 요소 | 구현 | 부채널 방침 |
| --- | --- | --- |
| SHA-256/384/512, HMAC | JRE 의 `MessageDigest`, `Mac` | JRE 구현 |
| HKDF, TLS 1.3 키 스케줄, TLS 1.2 PRF | 모듈(HMAC 위) | 해당 없음 |
| AES-128/256 | 모듈, 비트슬라이스(표 조회 없음, 4블록 병렬) | 비밀에 따른 분기·배열 인덱스 없음 |
| GCM(GHASH) | 모듈, 표 없는 캐리리스 곱(정수 곱의 구멍 기법) | 비밀에 따른 분기·배열 인덱스 없음 |
| ChaCha20-Poly1305 | 모듈(26비트 림 Poly1305) | 비밀에 따른 분기·배열 인덱스 없음 |
| X25519 | 모듈(16림 `long`, 몽고메리 사다리, 조건부 교환) | 비밀에 따른 분기·배열 인덱스 없음 |
| P-256/P-384 ECDH | 모듈, `java.math.BigInteger` 위의 몽고메리 사다리 | **상수 시간이 아니다**(BigInteger 연산 시간이 값에 따라 다르다). 키는 연결마다 새로 만들어 한 번만 쓰므로 반복 측정이 어렵다. `x25519` 를 먼저 제안한다 |
| ECDSA·RSA 서명 검증 | 모듈, `BigInteger` | 공개 값만 다룬다 |
| 태그·Finished 비교 | 모듈, 전체 길이를 OR 로 누적 | 길이만 드러난다 |

Java 에서는 JIT 과 GC 때문에 상수 시간 실행을 보장할 수 없다. 위 표는 소스 수준의 방침이다. 다 쓴 키 자료 배열은 0 으로 지운다(GC 가 옮긴 사본은 지울 수 없다).

### 1.7 성능 목표

| 항목 | 목표 (JRE 6u45, 이 PC) |
| --- | --- |
| 첫 핸드셰이크(로컬 서버, x25519 + RSA 2048 사슬) | 1초 이내(JIT 예열 전 포함) |
| 예열 뒤 핸드셰이크 | 150 ms 이내 |
| 수신 처리량(AES-128-GCM, 예열 뒤) | 3 MB/s 이상 |

측정 결과는 P32-06 에서 이 문서에 기록한다.

## 2. 검증 자료의 출처

| 자료 | 출처 | 쓰는 곳 |
| --- | --- | --- |
| HKDF 벡터 | RFC 5869 부록 A | P32-03-a |
| TLS 1.3 키 스케줄과 핸드셰이크 추적 | RFC 8448 | P32-03-a, P32-05-a |
| AES 벡터 | FIPS 197 부록 C | P32-03-b |
| GCM 벡터 | McGrew-Viega "The Galois/Counter Mode of Operation (GCM)" 부록 B(시험 사례 1~4, 13~16), NIST SP 800-38D 와 같은 알고리즘 | P32-03-b |
| ChaCha20, Poly1305, AEAD 벡터 | RFC 8439 2장과 부록 A | P32-03-c |
| X25519 벡터 | RFC 7748 5.2, 6.1 | P32-03-d |
| P-256/P-384 벡터 | RFC 6979 부록 A.2.5, A.2.6(ECDSA), RFC 5903 8장(ECDH) | P32-03-e |
| RSA 서명 | 더 새 JRE 의 JCA 로 만든 대조 자료(생성 도구와 JRE 판 기록), RFC 8017 의 부호화 규칙 | P32-03-f |

계획(P32-03)의 "NIST CAVP 벡터" 는 저장소에 큰 자료 파일을 두지 않기 위해 같은 알고리즘의 RFC·원 논문 벡터와 JCA 대조 자료로 대신한다.
대조 자료는 `tools/tlsvectors`(P32-03 에서 만든다)가 더 새 JRE 의 표준 공급자로 생성하며, 생성한 JRE 판을 자료 머리에 적는다.

## 3. 진행 기록

| 날짜 | 항목 | 내용 |
| --- | --- | --- |
| 2026-11-27 | P32-01 | 이 문서의 1~2절을 확정했다 |
