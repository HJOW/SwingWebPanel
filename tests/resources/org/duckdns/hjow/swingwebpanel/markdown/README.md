# Markdown 시험 자료 (Markdown test data)

| 파일 | 출처 URL | 판본 | 조회일 | 용도 |
| --- | --- | --- | --- | --- |
| `commonmark-0.31.2-spec.json` | https://spec.commonmark.org/0.31.2/spec.json | CommonMark 0.31.2 (last-modified 2025-06-01) | 2026-11-16 | 규격 예제 652개. `CommonMarkSpecTest` 가 전부 실행한다 |
| `gfm-0.29-spec.txt` | https://raw.githubusercontent.com/github/cmark-gfm/master/test/spec.txt | GitHub Flavored Markdown 0.29 (2019-04-06) | 2026-11-16 | GFM 확장 예제(표, 취소선, 자동 링크, 태그 필터). `CommonMarkSpecTest.testGfmExtensionExamples` 가 읽는다 |

조회는 `tools/webfetch`(JRE Zulu 25)로 했다. 이 자료는 텍스트 규격 자료이며 CC-BY-SA 4.0(GFM)/BSD-2(CommonMark) 라이선스이다.
