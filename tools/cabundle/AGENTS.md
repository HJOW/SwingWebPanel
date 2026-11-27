# cabundle (AI용 문서)

## 목적

TLS 모듈(`tls13`, 루트 `AGENTS.md` 의 P32-04-c)에 넣는 **루트 인증서 목록 자료**를 만든다.
입력은 Common CA Database(CCADB)의 "Mozilla Included CA Certificate Report"(CSV), 출력은 모듈 자료
`tls13/src/main/resources/org/duckdns/hjow/swingwebpanel/tls13/x509/mozilla-roots.txt` 이다. 제품 기능이 아니고 제품 JAR·시험 클래스는 이 도구에 의존하지 않는다.

**자동 갱신은 없다**(2026-11-27 사용자 결정): 모듈은 실행 중에도 빌드 중에도 목록을 내려받지 않는다. 목록을 바꾸려면 아래 절차를 사람이 다시 실행해 결과를 커밋한다.

## 라이선스 (2026-11-27 확인)

- CCADB 자료는 CDLA-Permissive-2.0 이다(`https://www.ccadb.org/rootstores/usage#ccadb-data-usage-terms`). 공유할 때 라이선스 문구를 함께 내고 CCADB 출처를 표시한다.
  문구와 출처 고지는 `tls13/src/main/resources/META-INF/LICENSE-ccadb.txt` 에 있고, 생성한 자료의 머리 줄에도 적힌다.
- 같은 목록의 NSS `certdata.txt` 는 MPL 2.0 이므로 **입력으로 쓰지 않는다**. TLS 모듈은 Apache 2.0 을 유지한다.

## 갱신 절차 (저장소 루트에서)

1. 보고서를 받는다(HTTPS 이므로 새 JRE 로 `tools/webfetch` 를 실행한다). 표준 오류의 `# retrieved` 날짜를 3단계에 쓴다.
   `tools\webfetch\run.bat --out build\ccadb.csv https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV`
2. 빌드: `tools\cabundle\build.bat` (또는 `tools/cabundle/build.sh`. JDK 6, `JAVA6_HOME`)
3. 변환: `tools\cabundle\run.bat build\ccadb.csv tls13\src\main\resources\org\duckdns\hjow\swingwebpanel\tls13\x509\mozilla-roots.txt https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV 2026-11-28`
4. `tls13` 의 시험을 실행한다(`x509.TrustStoreTest`). 실제 사이트 사슬 자료(`real-chains.txt`)가 오래되어 사슬의 루트가 목록에서 빠졌으면
   `tools/tlsvectors` 의 `ChainCapture` 로 다시 기록한다.
5. `docs/progress/tls13.md` 에 원본 조회 날짜, 원본의 SHA-256, 루트 수를 적고 커밋한다.

## 입력/출력 형식

- 입력: CCADB 보고서 CSV(UTF-8). 쓰는 열: `Common Name or Certificate Name`, `SHA-256 Fingerprint`, `Trust Bits`,
  `Distrust for TLS After Date`(`YYYY.MM.DD`), `Mozilla Applied Constraints`, `PEM Info`.
- 선택: `Trust Bits` 에 `Websites` 가 있는 행만 쓴다. 인증서의 SHA-256 이 보고서의 지문과 다르면 오류로 끝난다.
- 출력: UTF-8 텍스트. `#` 머리 줄(`# source:`, `# retrieved:`, `# source-sha256:`, `# roots:`, 라이선스 고지)과 루트마다 탭으로 나눈 한 줄
  `root`, 불신 날짜(`yyyy-mm-dd` 또는 `-`), 허용 DNS 하위 트리(쉼표 구분 또는 `-`), 이름, 인증서(Base64 DER). 지문 순으로 정렬하므로 같은 입력은 같은 출력을 낸다.
- `Mozilla Applied Constraints` 는 `*.tr` 같은 DNS 하위 트리만 안다. `No name constraints ...` 로 시작하는 글은 제약 없음으로 본다. 모르는 형식이면 오류로 끝나므로 사람이 확인한다.

## 종료 코드

0 기록함 / 1 사용법 오류 / 2 보고서를 읽을 수 없거나 형식이 예상과 다름

## 제약

- Java 6 문법/API 만 사용. 실제로 실행해 본 JRE: JDK 6u45 `1.6.0_45`(Windows 10, bat). sh 스크립트는 실행해 보지 않았다.
- Mozilla 가 루트 목록 밖에서 적용하는 신뢰 변경 가운데 불신 날짜와 적용 제약만 옮긴다. OneCRL(중간 인증서 폐기 목록)과 EV 정책은 옮기지 않는다.
- 2026-11-28 실행 결과: 보고서 167행 가운데 루트 112개, 불신 날짜 3개, 적용 제약 1개(`*.tr`).
