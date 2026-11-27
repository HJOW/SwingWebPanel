# swingwebpanel-tls13

Java 6·7 에서 쓰는 TLS 1.3·1.2 클라이언트를 순수 Java 로 구현한 선택 모듈입니다. 외부 라이브러리와 네이티브 코드를 쓰지 않습니다.

모듈은 JSSE의 `SSLSocketFactory` 인터페이스로 연결됩니다. 검증 결과와 지원 암호 조합은 설계 문서를 참고하세요. [TLS 설계와 검증 기록](../docs/progress/tls13.md)

## 왜 필요한가

JRE 6 의 기본 TLS 구현은 TLS 1.0 까지만 지원하고 서버 이름(SNI)을 보내지 않아 요즘 HTTPS 사이트 대부분에 연결하지 못합니다. JRE 7 은 TLS 1.2 까지 지원합니다.
이 모듈은 그런 실행 환경에서 TLS 1.3 과 TLS 1.2 로 연결할 수 있게 합니다. Java 8 이상에서는 JRE 의 TLS 구현을 쓰는 것이 좋습니다.

## 사용법

`swingwebpanel-tls13.jar`를 클래스패스에 넣고 `SwingWebPanel.setTls13ModuleEnabled(true)`를 호출합니다. 일반 Java 프로그램에서도 연결마다 팩터리를 설치할 수 있습니다.

```java
HttpsURLConnection connection = (HttpsURLConnection) new URL("https://example.com/").openConnection();
connection.setSSLSocketFactory(Tls13SocketFactory.getInstance());
try {
    System.out.println(connection.getResponseCode());
} finally {
    connection.disconnect();
}
```

별도 신뢰 저장소를 쓰려면 `new Tls13SocketFactory(Tls13Config.defaults().withKeyStore(myKeyStore))`로 해당 연결의 팩터리를 지정합니다.

## 주의

- 이 모듈은 표준 시험 벡터와 독립 JSSE 상호 운용 시험으로 검증했습니다. 외부 보안 감사는 받지 않았습니다.
- P-256과 P-384 키 교환은 상수 시간이 아닙니다. Java의 JIT와 GC도 상수 시간 실행을 보장하지 않습니다.
- 인증서 폐기(OCSP, CRL, OneCRL)를 확인하지 않습니다.
- 내장 Mozilla 루트는 2026-11-28에 CCADB에서 받은 목록입니다. 자동 갱신되지 않으므로 이후 신뢰 변경이 반영되려면 새 배포본이 필요합니다.
- 세션 재개, 재협상과 클라이언트 인증서는 지원하지 않습니다. TLS 1.1 이하나 CBC만 허용하는 서버의 협상은 실패합니다.

## 빌드

저장소 루트에서 실행합니다. JDK 6 이 필요합니다(루트 README 참고).

```sh
mvn -B -pl tls13 -am "-Dtest.filter=tls13" verify
```

산출물은 `tls13/target/swingwebpanel-tls13.jar` 입니다.
독립 서버와 상호 운용 시험을 실행하려면 `SWP_TLS_SERVER_JAVA_HOME`에 Java 11 이상을 지정합니다. 검증에는 Zulu 25를 사용했습니다.

## 라이선스

Apache License 2.0 (저장소 루트의 `LICENSE`).
CCADB에서 받은 Mozilla 루트 자료는 CDLA-Permissive-2.0으로 배포합니다. [CCADB 고지](src/main/resources/META-INF/LICENSE-ccadb.txt)
