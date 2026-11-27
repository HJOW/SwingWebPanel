# swingwebpanel-tls13

Java 6·7 에서 쓰는 TLS 1.3·1.2 클라이언트를 순수 Java 로 구현한 선택 모듈입니다. 외부 라이브러리와 네이티브 코드를 쓰지 않습니다.

**개발 중입니다.** 지금은 모듈 골격과 암호 기본 요소만 있으며 아직 TLS 연결에 쓸 수 없습니다. 진행 상황은 [docs/progress/tls13.md](../docs/progress/tls13.md)에 있습니다.

## 왜 필요한가

JRE 6 의 기본 TLS 구현은 TLS 1.0 까지만 지원하고 서버 이름(SNI)을 보내지 않아 요즘 HTTPS 사이트 대부분에 연결하지 못합니다. JRE 7 은 TLS 1.2 까지 지원합니다.
이 모듈은 그런 실행 환경에서 TLS 1.3 과 TLS 1.2 로 연결할 수 있게 합니다. Java 8 이상에서는 JRE 의 TLS 구현을 쓰는 것이 좋습니다.

## 주의

- 이 모듈은 표준 시험 벡터와 상호 운용 시험으로 검증하지만, 외부 감사를 받은 암호 라이브러리가 아닙니다.
- Java 에서는 상수 시간 실행을 보장할 수 없습니다.
- 인증서 폐기 확인(OCSP, CRL)을 하지 않습니다.
- 루트 인증서 목록은 모듈에 들어 있는 판을 쓰며 자동으로 갱신되지 않습니다.

## 빌드

저장소 루트에서 실행합니다. JDK 6 이 필요합니다(루트 README 참고).

```sh
mvn -B -pl tls13 -am "-Dtest.filter=tls13" verify
```

산출물은 `tls13/target/swingwebpanel-tls13.jar` 입니다.

## 라이선스

Apache License 2.0 (저장소 루트의 `LICENSE`).
