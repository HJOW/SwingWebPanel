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
| 2026-11-28 | P32-02 | 모듈 골격(`tls13/`, 경계 시험, 문서) |
| 2026-11-28 | P32-03 | 암호 기본 요소 a~f 와 대조 벡터 도구 `tools/tlsvectors`. 모듈 시험 40건 |
| 2026-11-28 | P32-04 | X.509 해석, 경로 검증, 호스트 이름 검증, 내장 루트 목록(4절). 모듈 시험 62건 |
| 2026-11-28 | P32-05-a·b·d·e | 핸드셰이크·레코드 계층·JSSE 소켓(5절). 모듈 시험 92건. P32-05-c(독립 구현의 TLS 1.2 서버 메시지)는 P32-06 의 상호 운용 서버와 함께 진행한다 |

## 4. 인증서 검증과 루트 목록 (P32-04)

### 4.1 내장 루트 목록

| 항목 | 값 |
| --- | --- |
| 원본 | CCADB "Mozilla Included CA Certificate Report" `https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV` |
| 조회 | 2026-11-28, `tools/webfetch`(Zulu 25), HTTP 200, 460,554 바이트, 응답의 Last-Modified 는 Fri, 27 Nov 2026 15:34:55 GMT |
| 원본 SHA-256 | `f069fdc3699ef77359b3d7bc8ded208e473234b59c61065e507fd7210ad591b9` |
| 변환 | `tools/cabundle`(JDK 6u45). 보고서 167행 가운데 `Trust Bits` 에 `Websites` 가 있는 루트 112개 |
| 자료 | `tls13/src/main/resources/org/duckdns/hjow/swingwebpanel/tls13/x509/mozilla-roots.txt`(163,261 바이트) |
| 불신 날짜가 있는 루트 | 3개: Izenpe.com(2026-04-15), Autoridad de Certificacion Firmaprofesional CIF A62634068(2026-08-06), emSign Root CA - G1(2026-11-16) |
| 적용 제약이 있는 루트 | 1개: TUBITAK Kamu SM SSL Kok Sertifikasi - Surum 1(`*.tr`) |
| 키 종류 | RSA 2048·4096비트, EC P-256·P-384·P-521 (모두 모듈이 지원) |
| 라이선스 | CDLA-Permissive-2.0. 문구와 CCADB 출처 고지는 `tls13/src/main/resources/META-INF/LICENSE-ccadb.txt`(문구 출처: SPDX license-list-data, 2026-11-28 조회) |
| 갱신 | **자동 갱신 없음.** 절차는 `tools/cabundle/AGENTS.md` |

같은 보고서로 두 번 변환해 같은 파일이 나옴을 확인했다. 보고서의 지문과 인증서의 SHA-256 이 다르면 도구가 오류로 끝난다.

### 4.2 검증 규칙의 요약

- 경로: 서버가 보낸 인증서(최대 10개)에서 깊이 우선으로 찾는다. 경로는 최대 8단계, 서명 검사는 탐색 한 번에 최대 48회.
- 이름: subjectAltName 만 쓴다(CN 무시). 와일드카드는 맨 왼쪽 레이블 전체, 레이블 하나에만 맞고 뒤에 레이블이 둘 이상 있어야 한다.
- 발급자 이름과 주체 이름은 **바이트 단위로** 비교한다(RFC 5280 의 문자열 정규화 비교는 하지 않는다).
- 키: RSA 는 2048비트 이상, EC 는 P-256/P-384/P-521. 서명: SHA-256/384/512 의 RSA PKCS #1 v1.5, RSA-PSS, ECDSA. SHA-1·MD5 는 거부.
- 루트 인증서 자체의 서명과 유효 기간은 검사하지 않는다(Java 의 PKIX 검증기와 같다).
- 서버 인증서 자체가 신뢰 저장소에 있으면(호스트가 넣은 자체 서명 인증서) 경로 없이 신뢰한다. 이름과 유효 기간은 그대로 검사한다.
- 정책 확장(certificatePolicies, policyConstraints, inhibitAnyPolicy, policyMappings)은 critical 이어도 받아들이고 강제하지 않는다. 그 밖의 모르는 critical 확장은 거부한다.

### 4.3 한계

- 폐기 확인(OCSP, CRL, OneCRL)을 하지 않는다. OCSP 스테이플링을 요청하지 않으며 must-staple 확장을 강제하지 않는다.
- 인증서 투명성(SCT)을 확인하지 않는다.
- 와일드카드가 공개 접미사(`*.co.uk` 등)에 걸리는지는 확인하지 않는다(레이블 수만 본다).
- 이름 제약은 DNS 이름, IP 주소, 디렉터리 이름(RDN 접두사의 바이트 비교)만 적용한다.

### 4.4 실제 사이트 사슬 (자동 시험 자료)

`tools/tlsvectors` 의 `ChainCapture`(Zulu 25 의 JSSE)로 2026-11-28 에 14개 호스트의 사슬을 기록했다(`tls13/src/test/resources/real-chains.txt`):
www.google.com, github.com, www.naver.com, letsencrypt.org, www.cloudflare.com, example.com, www.wikipedia.org, www.microsoft.com, www.amazon.com,
www.daum.net, www.rfc-editor.org, www.mozilla.org, www.apple.com, www.kakao.com. 시험 `x509.TrustStoreTest.testRealServerChains` 가 JDK 6u45 에서
기록한 시각으로 내장 루트에 대해 모두 검증하고(RSA 와 EC 서버 인증서 모두 포함), 다른 호스트 이름(NAME_MISMATCH), 3년 뒤(EXPIRED), 다른 신뢰 저장소(UNTRUSTED),
서명 한 비트 변조(BAD_SIGNATURE)를 거부함을 확인한다.

## 5. 핸드셰이크, 레코드 계층, JSSE 소켓 (P32-05)

### 5.1 구조

| 클래스 | 하는 일 |
| --- | --- |
| `Tls13SocketFactory` | 공개 진입점. 연결된 소켓 위에 TLS 를 얹는다. 인수 없는 `createSocket()` 은 원인이 `UnsupportedOperationException` 인 `SocketException` 으로 거절한다. 그러면 JRE 의 `HttpsURLConnection` 이 일반 소켓을 직접 연결하고 호스트 이름과 함께 `createSocket(Socket, host, port, autoClose)` 를 부르므로 서버 이름(SNI)을 보낼 수 있다 |
| `Tls13Config` | 신뢰 저장소, 프로토콜 버전, 암호 스위트, 그룹. 변경할 수 없는 객체이다. 인증서 검사나 호스트 이름 검사를 끄는 항목은 없다 |
| `Tls13Exception` | 실패의 종류(`NEGOTIATION`, `CERTIFICATE`, `PROTOCOL`, `PEER_ALERT`)와 경보 코드 |
| `Tls13Failures` | `isNegotiationFailure(Throwable)`. JRE 형식만 받는 static 메서드라서 모듈에 컴파일 의존성이 없는 브라우저가 리플렉션으로 부른다(P32-07) |
| `proto.ClientHandshake` | ClientHello 하나로 TLS 1.3 과 1.2 를 제안하고 서버가 고른 버전의 핸드셰이크를 한다 |
| `proto.RecordLayer` | 두 버전의 레코드 보호. 소켓 제한 시간으로 끊긴 읽기를 바이트를 잃지 않고 이어 간다 |
| `proto.Connection` | 핸드셰이크 뒤의 응용 데이터, 경보, KeyUpdate, NewSessionTicket(버림), TLS 1.2 HelloRequest(경고 `no_renegotiation` 으로 거절) |

### 5.2 협상 실패의 판정

다른 TLS 구현(JRE)으로 한 번 더 시도해도 되는 실패는 `Kind.NEGOTIATION` 뿐이다.

| 상황 | 종류 |
| --- | --- |
| 서버가 ClientHello 에 `handshake_failure`, `protocol_version`, `insufficient_security` 경보로 답함 | NEGOTIATION |
| 서버가 TLS 1.1 이하의 ServerHello 로 답함(모르는 확장이 있어도 버전을 먼저 본다) | NEGOTIATION |
| 서버가 모듈이 제안하지 않은 버전(예: TLS 1.3 전용 설정에 TLS 1.2)을 고름 | NEGOTIATION |
| 인증서 사슬이나 호스트 이름 거부 | CERTIFICATE |
| 틀린 서명·Finished·레코드 태그, 다운그레이드 표식, 제안하지 않은 스위트·그룹·확장, 세션 재개 시도, 순서가 틀린 메시지 | PROTOCOL |
| ServerHello 를 받아들인 뒤의 치명적 경보, 그 밖의 경보 | PEER_ALERT |
| 연결이 끊김(`EOFException`), 시간 초과 | 모듈의 예외가 아니다 |

### 5.3 구현하면서 정한 것

- ClientHello 의 세션 식별자는 항상 난수 32바이트이다(미들박스 호환 모드). TLS 1.2 서버가 그 값을 되돌려 보내면 세션 재개 시도로 보고 거절한다.
- TLS 1.3 에서 서버의 `change_cipher_spec` 은 핸드셰이크 동안만 버린다. 핸드셰이크 뒤에 오면 `unexpected_message` 이다.
- 키는 핸드셰이크 메시지의 경계에서만 바꾼다. 메시지가 키 변경에 걸쳐 있으면 `unexpected_message` 이다.
- TLS 1.3 레코드는 바깥 길이(2^14 + 256), 내부 평문 길이(2^14 + 1), 내용 길이(2^14)를 각각 검사한다.
- TLS 1.2 AES-GCM 의 명시적 nonce 로는 순서 번호를 쓴다(같은 키에서 되풀이되지 않는다). 받을 때는 레코드에 실린 값을 쓴다.
- `close_notify` 없이 레코드 사이에서 닫힌 연결은 스트림 끝으로 돌려준다(HTTP 는 길이로 본문 끝을 판정한다). 레코드 도중에 끊기면 `EOFException` 이다.
- 핸드셰이크가 실패하면 그 실패를 기억하고 이후의 읽기·쓰기에서 같은 예외를 다시 던진다. 핸드셰이크 도중의 소켓 제한 시간도 연결을 끝낸다. 핸드셰이크 뒤의 읽기 제한 시간은 연결을 끝내지 않는다.
- `close()` 는 쓰기 잠금을 기다리지 않는다. 다른 스레드가 쓰는 중이면 `close_notify` 를 보내지 않고 아래 소켓을 닫는다.
- `createSocket(InetAddress, ...)` 는 주소 문자열을 호스트로 쓴다(역방향 조회는 위조될 수 있다). 그 경우 인증서에 그 IP 주소가 있어야 하고 서버 이름은 보내지 않는다.
- `SSLSession.getPeerCertificateChain()`(옛 `javax.security.cert`)은 그 구현이 없는 JRE(15 이상)에서 `SSLPeerUnverifiedException` 이다. `getPeerCertificates()` 는 모든 JRE 에서 동작한다.

### 5.4 자동 시험

| 시험 | 내용 |
| --- | --- |
| `proto.Rfc8448TraceTest` 4건 | RFC 8448 의 3절(단순 1-RTT), 5절(HelloRetryRequest), 7절(호환 모드)을 재현한다. 추적의 개인 키와 ClientHello 를 주고 서버의 레코드를 먹이면 클라이언트가 쓰는 모든 레코드(Finished, 응용 데이터, 경보, `change_cipher_spec`)가 추적과 바이트 단위로 같다. 6절(인증서 요청)은 빈 Certificate 로 답하므로 추적과 같을 수 없어, 클라이언트의 비행을 추적의 키로 복호화해 확인한다 |
| `proto.HandshakeTest` 6건 | 시험 서버와의 완전한 핸드셰이크: TLS 1.3 스위트 3종, 그룹 3종(HelloRetryRequest 포함), RSA·ECDSA(P-256, P-384) 인증서, TLS 1.2 스위트 6종 × 그룹 × 서명 방식 × 확장 마스터 비밀 사용·미사용, 인증서 요청, 한 버전 전용 설정, 서버가 본 ClientHello 의 내용 |
| `proto.HandshakeFailureTest` 6건 | 5.2 절의 판정과, 결함마다 클라이언트가 보내는 경보 |
| `proto.RecordProtocolTest` 5건 | 1바이트·3바이트 레코드로 조각난 핸드셰이크, TLS 1.3 패딩과 빈 레코드, 너무 큰·손상된·보호되지 않은 레코드, 양방향 KeyUpdate, NewSessionTicket, HelloRequest, 경보, 끊긴 연결 |
| `proto.SocketFactoryTest` 9건 | 소켓 팩터리와 `SSLSocket`·`SSLSession`, 읽기 제한 시간 뒤 이어 읽기, 다른 스레드의 `close`, 동시 읽기·쓰기(각 방향 300 KiB), `HttpsURLConnection` 에 설치(서버가 받은 서버 이름으로 SNI 전송을 확인) |

시험 서버(`proto.ScriptedServer`)는 모듈의 암호 기본 요소로 만든 시험 코드이다. 레코드는 자체 코드로 쓰고(패딩, 분할, 손상, TLS 1.2 의 임의 명시적 nonce) 클라이언트의 레코드는 모듈의 레코드 계층으로 읽는다.
따라서 이 시험들은 상태 기계와 오류 처리의 검증이고, 다른 구현과의 일치는 RFC 8448 재현(TLS 1.3)과 P32-06 의 JSSE 서버(TLS 1.3·1.2)로 확인한다.

RFC 8448 재현에서 ClientHello 는 추적의 것을 그대로 보낸다(추적의 클라이언트가 보내는 확장이 모듈과 다르다). 모듈이 만드는 ClientHello 는 시험 서버가 해석해 내용을 확인하고(`HandshakeTest`), P32-06 에서 JSSE 서버가 받아들이는지 본다.

실행한 JRE: JDK 6u45 에서 모듈 시험 92건 통과. `proto` 시험 30건은 JDK 7u80, 8u202, Zulu 25 에서도 통과했다(JDK 6 으로 컴파일한 클래스).

## 6. 상호 운용·실환경·성능 (P32-05-c, P32-06, 2026-11-28)

### 6.1 자동 검증

`JsseFixture`가 `SWP_TLS_SERVER_JAVA_HOME`의 Java 11 이상으로 별도 JVM을 실행한다. 이번 서버는 Zulu OpenJDK 25였다.
`JsseServer`는 표준 JSSE와 JCA만 쓰며 시험용 루프백 주소로 제한한다. 알고리즘 설정은 자식 JVM에만 적용한다.
클라이언트는 인증서 시험용 루트를 사용하며 상호 운용 행렬에서는 고정 난수로 ClientHello를 구성한다.

| 대상 | 확인 |
| --- | --- |
| TLS 1.3 | AES-128/256-GCM, ChaCha20-Poly1305, X25519·P-256·P-384, 두 NIST 곡선의 HelloRetryRequest, RSA·ECDSA 인증서, 클라이언트 인증서 요청에 빈 응답, JSSE가 보낸 KeyUpdate로 수신 키 변경 |
| TLS 1.2 | ECDHE_RSA·ECDHE_ECDSA × AES-128/256-GCM·ChaCha20-Poly1305 여섯 스위트, X25519·P-256·P-384, EMS 사용·미사용, 독립 JSSE 서명·Finished와 4 MiB 응답 |
| 실패 | TLS 1.1 전용·TLS 1.2 CBC 전용 서버의 협상 실패, 만료·호스트 이름 불일치 인증서의 거부, 예고한 4 MiB보다 짧게 끝난 응답 |
| 상태 기계 | `RecordProtocolTest`의 HelloRequest 거절과 `HandshakeFailureTest`의 세션 재개 시도 거부 |

`mvn -B -o -pl tls13 -am "-Dtest.filter=tls13" verify`로 JDK 6u45에서 97건이 통과했다. 같은 JDK 6 클래스의 전체 모듈 시험을 7u80·8u202로 실행해 각 97건이 통과했다.
`SWP_TLS_SERVER_JAVA_HOME`을 지정하지 않거나 해당 실행 파일이 없으면 독립 JSSE를 쓰는 다섯 시험이 이유와 함께 건너뛴다.

### 6.2 서명·신뢰와 실제 HTTPS 응답

`tools/tlsvectors`의 `LiveProbe`를 JRE 6u45와 7u80에서 수동 실행했다. 날짜는 2026-11-28 KST이며, HTTPS HEAD의 첫 응답을 확인했다.
두 JRE에서 4.4절의 14개 호스트 모두 TLS 1.3으로 접속했다. Google, GitHub, Naver, Let's Encrypt, Cloudflare, Example Domain, Wikipedia, Microsoft, Amazon, RFC Editor, Mozilla, Apple은 200을 반환했다.
Daum은 404, Kakao는 302를 반환했다. 두 상태 모두 인증서 검증과 TLS 협상이 끝난 후 받은 HTTP 응답이다.
`https://tls-v1-2.badssl.com:1012/`는 두 JRE 모두 TLS 1.2의 ECDHE_RSA AES-256-GCM SHA-384로 200을 반환했다.
`expired.badssl.com`은 EXPIRED, `wrong.host.badssl.com`은 NAME_MISMATCH, `self-signed.badssl.com`과 `untrusted-root.badssl.com`은 UNTRUSTED로 각각 거부했다.

### 6.3 성능

Zulu 25 루프백 서버의 X25519·RSA 2048·AES-128-GCM 구성에서 `TestRunner --filter testBenchmark org.duckdns.hjow.swingwebpanel.tls13.proto.JsseInteropTest`만 각각 새 JVM에서 실행했다.
첫 연결은 TCP 연결 이후 난수와 TLS 상태를 준비하는 시점부터 측정했다. 예열 후 핸드셰이크는 다섯 번의 중앙값, 처리량은 해당 다섯 번의 4 MiB 응답 중 최저값이다.

| 클라이언트 | 첫 핸드셰이크 | 예열 뒤 | 최저 수신 처리량 |
| --- | ---: | ---: | ---: |
| JRE 6u45 | 480.0 ms | 17.3 ms | 5.50 MB/s |
| JRE 7u80 | 300.0 ms | 15.9 ms | 6.80 MB/s |

두 실행 환경 모두 1.7절의 목표(1초, 150 ms, 3 MB/s)를 충족했다.

## 7. SwingWebPanel 연동 (P32-07, 2026-11-28)

선택 API는 계약 5.31에 있다. 브라우저는 모듈 클래스를 리플렉션으로 찾아 표준 `SSLSocketFactory`만 연결에 설치한다.
기본값은 JRE 경로다. 호스트가 보호 훅을 재정의하면 실제 설치한 팩터리가 연결을 처리한다.
주 문서, 프레임, 하위 리소스, Fetch/XHR, EventSource와 다운로드의 호출점을 따라가서 같은 `ResourceFetcher`를 사용하는지 확인했다.
팝업의 SSL 훅은 여는 패널에 위임되며 여는 패널을 폐기한 뒤에도 열린 팝업은 마지막 SSL 정책으로 탐색한다.

JRE 6u45에서 모듈 없는 브라우저 자체 시험 1건과 `OptionalTlsBrowserTest` 5건을 검증했다.
Zulu 25 루프백 서버와 HTTPS 탐색, 두 오리진 간 TLS CORS 사전 요청·본 요청 및 팝업을 시험했다.
CBC 전용 서버에는 HTTPS 모듈·HTTPS JSSE·HTTP 순으로 연결했다. 시험 루트를 내장 Mozilla 목록에서 찾을 수 없는 TLS 1.3 서버에는 HTTPS 모듈·HTTP 순으로 연결했다.
속성 알림 순서, 값이 같은 요청의 알림 억제와 EDT 실행, 기본 꺼짐, `clean()` 후 값 유지, 폐기 후 변경 거부와 GC를 확인했다.

## 8. 데스크톱 앱 (P32-08)

`DesktopTls`가 `java.specification.version`의 정확한 값 1.6과 1.7에서만 모듈을 활성화한다.
`TabPanel`의 일반 창·탭 생성과 팝업 생성이 같은 정책을 쓴다. 초기화에 실패하면 콘솔에 한 번 알린다.
도움말의 정보 창은 활성 패널의 TLS 설정을 표시한다. Java 8u202를 포함한 Java 8 이상은 JRE의 TLS를 사용한다.
Java 6·7의 실제 창에서 같은 HTTPS 페이지 두 곳을 열었다. [실제 창 검증](uicheck.md)

최종 `swingwebpanel-desktopapp.jar`로 `java -jar ... --smoke`를 실행했다.

| JRE | 보고된 TLS | 종료 코드 |
| --- | --- | ---: |
| 6u45 | module | 0 |
| 7u80 | module | 0 |
| 8u202 | JRE | 0 |
| Zulu 25 | JRE | 0 |

## 9. 전체 회귀와 배포 점검 (P32-09)

Zulu 25로 Maven을 실행하고 JDK 6u45로 빌드·시험한 `mvn -B -o verify`의 결과다. 엔진 892건과 TLS 97건을 통과했다.
브라우저는 3,872건 통과, 3건 실패, 12건 건너뜀이었다. 12건은 별도 Opus 전체 벡터가 없어서 건너뛰었다.
브라우저 단계에서 Maven이 중단되어 `mvn -B -o -pl desktopapp -am "-Dtest.filter=desktopapp" verify`로 앱 전체를 별도 실행했다. JRE 6u45에서 64건을 통과했고 실패나 건너뜀이 없었다.

다음 세 실패는 P22-16-c4에 기록된 Web Audio 재생 시각 문제다. 변경 전 커밋 `7377b35`의 `SwingWebPanel`과 `HttpFetcher`를 JDK 6으로 다시 컴파일해 동일한 브라우저 시험에 적용했을 때 각 실패를 재현했다.

- `WebAudioComponentTest.testNavigationEndsTheContexts`
- `WebAudioOutputComponentTest.testDefaultOutputLetsAContextRunWithoutAScriptError`
- `OutputRenderDriverTest.testNoDeviceIsSilentButTheTimeRunsAtRealSpeed`

최종 브라우저 JAR에 대해 Java 6u45로 HTTPS·소켓·CORS·프로토콜·모듈 부재 시험 47건을 다시 실행해 모두 통과했다. 다운로드 팝업 시험은 실제 화면을 사용할 수 있는 JVM에서 실행했다.
JRE 7u80·8u202에서 독립 JSSE 서버를 사용한 최종 TLS 모듈 전체 시험도 각각 97건을 통과했다. `tools/sourcecheck/run.bat`는 4,793개 파일에서 위반 0건을 보고했다.

선택 TLS JAR과 데스크톱 실행 JAR은 각각 TLS 구현 클래스 52개, 내장 루트, `META-INF/LICENSE-ccadb.txt`를 담았다. 두 JAR의 루트와 고지를 저장소 원본과 SHA-256으로 대조했다.

| 리소스 | SHA-256 |
| --- | --- |
| `mozilla-roots.txt` | `8a44ce3c3b61a5ae7fd88b84be171f14ba248527f9fd90d59e008ceffa315649` |
| `META-INF/LICENSE-ccadb.txt` | `f7a3e83c2646e7966544df58d488d7d23cd05e5049b82f5a30692830443c3ddb` |

고지 파일에 CCADB 출처와 CDLA-Permissive-2.0 전문이 들어 있다. 엔진, 브라우저, 합본 제품 JAR은 TLS 클래스와 두 자료를 포함하지 않는다. 배포 JAR에 NSS `certdata.txt`나 시험 서버 클래스가 들어 있지 않음을 확인했다.
내장 자료의 원본 리비전(2026-11-28 조회일과 CSV SHA-256)은 4.2절에 기록돼 있다. 자동 갱신, OCSP·CRL·OneCRL, 세션 재개와 P-256/P-384 상수 시간의 한계는 1절과 모듈 README를 따른다.
