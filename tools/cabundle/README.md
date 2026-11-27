# cabundle

TLS 모듈(`tls13`)에 넣는 루트 인증서 목록을 만드는 개발 도구입니다.

Mozilla 가 웹 사이트용으로 신뢰하는 루트 인증서 목록을 Common CA Database(CCADB)가 공개하는 보고서에서 가져와, 모듈이 읽는 형식의 파일로 바꿉니다.

## 알아 둘 점

- **목록은 자동으로 갱신되지 않습니다.** 새 목록이 필요하면 아래 절차를 직접 다시 실행해야 합니다.
- CCADB 자료는 CDLA-Permissive-2.0 라이선스입니다. 라이선스 문구와 출처 표시는 모듈의 `META-INF/LICENSE-ccadb.txt` 에 들어 있습니다.
- 이 도구는 제품에 포함되지 않습니다.

## 사용법

저장소 루트에서 실행합니다. JDK 6 이 필요하고, 보고서를 받을 때는 HTTPS 접속을 위해 새 버전 Java 가 필요합니다.

```bat
set WEBFETCH_JAVA_HOME=C:\Program Files\Java\zuu25
tools\webfetch\run.bat --out build\ccadb.csv https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV

tools\cabundle\build.bat
tools\cabundle\run.bat build\ccadb.csv tls13\src\main\resources\org\duckdns\hjow\swingwebpanel\tls13\x509\mozilla-roots.txt https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV 2026-11-28
```

마지막 인수는 보고서를 받은 날짜입니다. 실행이 끝나면 루트 수가 출력됩니다. 그 뒤 `tls13` 모듈의 시험을 실행해 확인합니다.

같은 보고서 파일로 다시 실행하면 항상 같은 결과 파일이 나옵니다.
