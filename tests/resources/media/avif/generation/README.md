# AVIF 시험 자료와 표 생성 스크립트 / Generation scripts of the AVIF tests and tables

이 디렉터리의 스크립트는 제품 코드(`src/org/duckdns/hjow/swingwebpanel/media/image/avif`)의 상수 표와 시험 자료를 만드는 데 쓴 것이다.
제품 빌드와 시험 실행에는 필요하지 않다(생성물이 저장소에 들어 있다). 규격이나 시험 자료를 다시 만들 때만 쓴다.

The scripts in this directory produced the constant tables of the product code and the test inputs. They are not needed to build or to run the
tests (the generated files are committed); use them only to regenerate tables or fixtures.

## 상수 표 / Constant tables

| 스크립트 | 역할 |
| --- | --- |
| `spec2txt.py <av1-spec.html> <av1-spec.txt>` | AV1 규격 HTML 을 줄 단위 텍스트로 바꾼다. |
| `extract_tables.py <av1-spec.txt> <tables.json> [표 이름...]` | 텍스트에서 `Name[..] = { ... }` 꼴의 표를 읽어 JSON 으로 저장한다. |
| `gen_java_tables.py <tables.json> <출력 디렉터리>` | `AvifTables.java`, `AvifCdfTables.java` 를 만든다. |
| `gen_qm.py <tables.json> <AvifQmTable.java>` | 양자화 행렬 표 `AvifQmTable.java` 를 만든다. |

- 규격 출처: AV1 Bitstream & Decoding Process Specification,
  <https://aomediacodec.github.io/av1-spec/av1-spec.html>, 마지막 수정 2023-05-25 12:27 PT, 조회일 2026-11-16.
  (JDK 6 은 현대 HTTPS 에 접속하지 못하므로 `tools/webfetch` 를 더 새 JRE 로 실행해 내려받았다.)
- 생성한 파일은 손으로 고치지 않는다. 같은 입력으로 다시 만들면 저장소의 파일과 바이트 단위로 같아야 한다.

## 시험 자료 / Test inputs

시험 자료 생성 절차는 `tests/resources/media/avif/README.md` 에 있다.
