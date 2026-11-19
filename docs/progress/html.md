# HTML 작업자 진행 기록 (P07)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/html/**`, `tests/src/org/duckdns/hjow/swingwebpanel/html/**`, `tests/resources/html/**`, `tools/entitygen/**`, `docs/progress/html.md`.
시험 실행: `scripts/pkgtest.sh html "html dom net" "html"`. 린트: `tools/sourcecheck/run.sh --only encoding,forbidden,javadoc,loops src/org/duckdns/hjow/swingwebpanel/html tests/src/org/duckdns/hjow/swingwebpanel/html tools/entitygen`.
규격 자료: `build/specs/html/` (gitignored; `tools/webfetch` 로 조회). 조회 기록은 아래 "규격 출처" 참조.

## 요약 (2026-11-16 기준)

- 구현: 인코딩 판정 + 입력 전처리 + 토큰화기(70 상태) + 문자 참조 표(생성) + 트리 생성기(삽입 모드 23개, 외부 콘텐츠, adoption agency, foster parenting,
  template, 조각 파싱) + 문서 모드 판정 + 직렬화/innerHTML 서비스 + 파서 리스너(자원 통지, document.write, 일시 정지, 인코딩 재시작).
- 시험: `scripts/pkgtest.sh html "html dom net" "html"` (JDK 6u45) → **html 시험 56건 통과**(+ 시험 실행기 자체 시험 7건), 실패 0.
  트리 생성 자료 사례 256건(`tests/resources/html/tree/*.dat`) + 토큰화기 사례 약 250개 단언 + 인코딩 + 오류 위치 + 무작위 입력 300×2 + 깊이 20000 / 1MB 입력.
- 한계(정직하게): html5lib-tests 같은 외부 적합성 자료는 받지 않았다(지시). 시험 자료는 규격 본문(2026-11-14 판)을 읽고 직접 쓴 것이다.
  그래서 "규격 전체 적합"을 주장하지 않는다. 검증된 것은 아래 사례 범위이다.

## 규격 출처

| 자료 | URL | 판본 / 조회 |
| --- | --- | --- |
| HTML 파싱 알고리즘 | https://html.spec.whatwg.org/multipage/parsing.html | HTML Living Standard, Last Updated 14 November 2026 (조회 2026-11-16, JRE 25) |
| 이름 있는 문자 참조 | https://html.spec.whatwg.org/entities.json | last-modified Wed, 12 Nov 2025 00:20:03 GMT, etag `6913d2b3-239e9`, 2231 항목 (조회 2026-11-16) |
| 인코딩 라벨 | https://encoding.spec.whatwg.org/encodings.json | last-modified Mon, 19 Oct 2026 23:30:10 GMT, etag `6ad6a802-22d1`, 40 인코딩 (조회 2026-11-16) |
| 기준 판본(P00-03) | https://www.w3.org/TR/2014/REC-html5-20141028/ | W3C HTML5 권고안 2014 |

참고 구현(Chromium)의 파일은 열람하지 않았다. 규격 본문만 따랐다(그래서 BSD 고지 대상 코드 차용 없음).

## 규격과 다르게 구현한 점 (Living Standard 14 Nov 2026 대비)

- 처리 명령 토큰화 상태(`<?target ... ?>`, 최신 초안이 추가)는 구현하지 않았다. 2014 권고안과 브라우저처럼 `<?` 는 bogus 주석이며 오류 코드는
  `unexpected-question-mark-instead-of-tag-name` 이다. 트리 생성기에는 "processing instruction token" 분기가 없다.
- `select` 는 고전적인 "in select"/"in select in table" 삽입 모드를 쓴다(최신 초안은 customizable select 를 위해 이 모드를 없앴다). 기본 범위 목록에도 `select` 가 없다.
- 파서 sanitizer, 사용자 정의 요소(custom element reactions), 선언적 shadow root, `template for` 패치, 추측 파싱은 구현하지 않았다.
- 직렬화에서 `pre`/`textarea`/`listing` 첫 자식이 LF 일 때 추가 LF 를 넣지 않는다(최신 표준과 브라우저가 같음).
- template contents 는 별도의 비활성 문서가 아니라 같은 문서에 속한 `DocumentFragment` 이다(`ParserNodeData.getTemplateContents`).
- 인코딩: 선언이 없고 전체가 멀티바이트가 있는 올바른 UTF-8 이면 UTF-8 로 추측한다(확장, 잠정적). 그렇지 않으면 windows-1252(또는 호출자가 준 대체 라벨).
- 속성 이름이 XML Name 이 아니면(`@click`, `"a"`) `dom.Element.setAttribute` 가 거부하므로 그 속성은 `invalid-attribute-name` 오류와 함께 버려진다(조정자 요청 1).
- 오류 코드: 토큰화 오류는 규격의 이름을 쓴다. 트리 생성 오류는 규격이 이름을 주지 않으므로 이 구현의 코드(`unexpected-start-tag`, `unexpected-end-tag`,
  `unexpected-open-element`, `missing-doctype` 등)를 쓴다.

## 통합용 공개 API (Public API for integration)

패키지 `org.duckdns.hjow.swingwebpanel.html`:

| 클래스 | 용도 |
| --- | --- |
| `EncodingSniffer` | `decode(byte[] bytes, String httpCharset)` / `decode(bytes, httpCharset, fallbackLabel)` → `DecodedText`. `core.TextDecoder` 를 대체한다. `charsetOfContentType(String contentType)` 가 Content-Type 헤더에서 charset 값을 뽑는다. `prescan`, `bomEncoding`, `extractFromMetaContent` 도 공개. |
| `DecodedText` | `getText()`(수신 원문, 줄바꿈/공백 그대로, BOM 제외 — `getContent()` 용), `getRawBytes()`, `getEncoding()`(WHATWG 이름), `isCertain()`, `getSource()`, `redecode(name)`, `DecodedText.ofString(text)`(setContent 용). |
| `InputPreprocessor` | `normalize(text)`(CRLF/CR → LF 복사본), `scanErrors`. 파서가 내부에서 쓴다. |
| `HtmlParser` | `parseDocument(String)`, `parseDocument(String source, String url, ParserListener l, boolean scripting)`, `parseDocument(byte[] bytes, String httpCharset, String url)`, `parseFragment(Element context, String markup[, boolean scripting])`. 인스턴스: `new HtmlParser(Document, String)` / `new HtmlParser(Document, DecodedText)`, `setListener`, `setScripting`, `setSrcdoc`, `setErrorLimit`, `run()`, `resume()`, `isPaused()`, `isFinished()`, `getErrors()`, `getErrorCodes()`, `getErrorCount()`, `getDecodedText()`; `implements ParserControl`. |
| `ParserListener` / `ParserListenerAdapter` / `ParserControl` | 콜백: `parseError`, `documentModeDetermined`, `elementInserted`, `elementClosed`, `scriptReady(Element, ParserControl)`, `styleReady`, `stylesheetFound`, `imageFound`, `baseHrefChanged`, `metaRefresh`, `titleReady`, `charsetDeclared`. 제어: `insertAtInsertionPoint(String)`(document.write), `pause()`, `isFragment()`, `getDocument()`. |
| `HtmlMarkupSupport` | `dom.MarkupSupport` 구현. `document.setMarkupSupport(new HtmlMarkupSupport())` 로 설치하면 `Element.getInnerHtml/setInnerHtml/getOuterHtml/setOuterHtml/insertAdjacentHtml` 가 동작한다. 직렬화는 반복 순회(깊이 제한 없음). |
| `ParserNodeData` | 보조 표: `getTemplateContents(Element)`, `ensureTemplateContents`, `getFormOwner(Element)`, `scriptFlags(Element)`(parser-inserted / already started / force async). |
| `DocumentModes` | `fromDoctype(name, publicId, systemId, forceQuirks)` → `Document.NO_QUIRKS_MODE/LIMITED_QUIRKS_MODE/QUIRKS_MODE`. |
| `HtmlTags`, `HtmlToken`, `HtmlTokenizer`, `ParseError`, `ParseErrorLog`, `NamedCharacterReferences`, `EncodingLabels` | 하위 구성요소(시험과 도구가 직접 사용 가능). |

통합 순서(조정자):
1. 응답 본문 바이트 → `EncodingSniffer.decode(bytes, EncodingSniffer.charsetOfContentType(contentTypeHeader))` → `DecodedText t`.
   `getContent()` 에는 `t.getText()`(수신 원문)를 보관한다(파서가 바꾸지 않는다).
2. `Document doc = Document.createHtmlDocument(); doc.setUrl(finalUrl); doc.setMarkupSupport(new HtmlMarkupSupport());`
3. `HtmlParser p = new HtmlParser(doc, t); p.setListener(...); p.setScripting(javascriptEnabled); p.run();`
   - 늦은 `<meta charset>` 가 잠정적 인코딩과 다르면 파서가 같은 `Document` 를 비우고 처음부터 다시 파싱한다(스크립트가 아직 안 돌았을 때만). 결과의 인코딩은 `doc.getCharacterSet()` / `p.getDecodedText()`.
   - 스크립트: 리스너의 `scriptReady(script, control)` 에서 인라인이면 실행하고 `control.insertAtInsertionPoint(text)` 로 document.write 를 처리한다. 외부 스크립트는 `control.pause()` 후 로딩이 끝나면 `p.resume()`.
   - 스타일/이미지/기준 URL/meta refresh/제목은 리스너로 온다. `<base href>` 는 파서가 `Document.setBaseUri` 에 이미 반영한다.
4. `setContent("text/html")` 도 같은 경로: `DecodedText.ofString(content)` 또는 `new HtmlParser(doc, content)`.
5. `iframe srcdoc` 는 `p.setSrcdoc(true)`.

## 요소 이름 인식과 동작 구현 상태 (P07-09 기록, P07-07 옛 요소 표)

파서는 **모든 태그 이름**을 DOM 요소로 만든다(알 수 없는 이름은 일반 요소, 속성 보존). 아래는 파서가 특별히 다루는 이름이다. 화면 표시 규칙(표현 속성의
CSS 매핑, 기본 스타일)은 P08-02(사용자 에이전트 스타일)와 P09 의 몫이며 여기서는 "DOM 에 속성과 요소가 손실 없이 들어온다"까지만 검증했다.

| 분류 | 요소 | 파서 동작 | 시험 |
| --- | --- | --- | --- |
| void | `area base basefont bgsound br col embed frame hr img input keygen link meta param source track wbr` | 삽입 즉시 스택에서 꺼냄(자식 없음), `<br/>` 등 자기 닫기는 승인 | basics, special, LegacyHtmlTest |
| 원문 텍스트(RAWTEXT) | `style xmp iframe noembed noframes noscript(스크립트 켜짐)` | 토큰화기 RAWTEXT, 문자 참조 없음 | body, special |
| RCDATA | `title textarea` | 문자 참조만 해석 | basics |
| script | `script` | script data 상태(이스케이프/이중 이스케이프), 플래그 부착, `scriptReady` 통지 | tokenizer, special, HtmlParserTest |
| 평문 | `plaintext` | PLAINTEXT 상태(끝까지 문자) | body |
| 서식(adoption agency) | `a b big code em font i nobr s small strike strong tt u` | 활성 서식 목록, Noah's Ark(3), 재구성 | body, tables |
| 경계(marker) | `applet marquee object td th caption template` | 활성 서식 목록 표시, 범위 경계 | body, tables, special |
| 표 | `table caption colgroup col tbody thead tfoot tr td th` | 암시적 tbody/tr/colgroup, foster parenting | tables |
| 프레임 | `frameset frame noframes` | in frameset / after frameset 모드, body 가 없는 문서 | special, LegacyHtmlTest |
| 폼 | `form input select option optgroup button textarea fieldset` | form 포인터, 폼 소유자 기록, in select 모드 | tables, special, HtmlParserTest |
| 루비 | `ruby rb rt rp rtc` | 암시적 종료 | body |
| 외부 콘텐츠 | `svg math` 와 그 자손 | SVG/MathML 네임스페이스, 태그/속성 이름 대소문자 복원, 통합 지점, 이탈 | foreign |
| 옛 요소 | `acronym big blink center dir font isindex layer marquee menu multicol nextid nobr spacer strike tt u` 등 | 일반 요소로 보존(속성 포함) | LegacyHtmlTest |
| 옛 DOCTYPE | HTML 2.0/3.2/4.0/4.01/XHTML 1.0 | 표준/제한 호환/호환 모드 판정 | DocumentModesTest, LegacyHtmlTest |

## 조정자 요청

1. **`dom.Element` 에 이름 검증 없는 속성 추가 API** (예: `setAttributeUnchecked(String name, String value)` 또는 `Attr` 생성 시 이름 검사 생략).
   파서는 `@click`, `:x`, `a"b` 같은 XML Name 이 아닌 속성 이름도 만들어야 하는데(Vue 등 프레임워크가 쓴다) 지금은 `invalid-attribute-name` 으로 버린다.
2. **template `cloneNode`**: `Node.cloneNode(deep)` 가 `ParserNodeData.getTemplateContents(template)` 의 내용을 복제하지 않는다(DOM 쪽 보조 표 연동 필요).
   `Element.copyStateTo` 를 확장해 복제 요소에 `ParserNodeData.setTemplateContents(copy, 복제한 조각)` 를 호출하거나, DOM 에 template contents 필드를 두는 것을 권장한다.
3. `core.TextDecoder` 를 `html.EncodingSniffer`/`DecodedText` 로 교체(위 통합 순서 1).
4. 네트워크 계층(P04 `NetworkPageLoader`)에서 HTML 응답을 `HtmlParser` 로 연결하고 `ParserListener` 로 CSS/이미지/스크립트 요청을 시작한다(P08-02, P15).

## 체크박스 후보

구현과 검증이 끝난 항목(근거: 위 시험, 범위 한계는 "요약" 참조):

- P07-01 (EncodingSniffer/DecodedText/InputPreprocessor, 우선순위와 meta 재디코딩, 원문 보존) — 통합 교체는 조정자 몫
- P07-02 (HtmlToken 종류/위치, 속성/자기 닫기/중복/문자 참조)
- P07-03 (HtmlTokenizer 70 상태 상태 기계)
- P07-04 (이름 있는/숫자 문자 참조, 속성 규칙, `tools/entitygen` 생성 표와 출처 기록)
- P07-05 (삽입 모드 23개, 열린 요소 스택, adoption agency, 표 foster parenting 등)
- P07-06 (template, SVG/MathML 외부 콘텐츠, 네임스페이스 전환, 조각 파싱)
- P07-07 (DOCTYPE 문서 모드 3종, 옛 요소/속성 보존 표) — 표현 속성의 화면 규칙은 P08/P09
- P07-08 (script/style/link/img/base/meta/title 통지, document.write 삽입 지점, 일시 정지/재개, JavaScript 비활성(scripting=false)에서도 파싱 정상) — async/defer 의 실제 실행 순서 모델은 P15
- P07-09 (고정 기대 트리 256건 + 토큰화/인코딩/직렬화/무작위/거대 입력)

P07 단계 전체(P07-09 의 "P07을 완료 처리")는 위 한계(외부 적합성 자료 없음)를 감안해 조정자가 판단한다.

## 작업 기록

(작업 단위마다 맨 위에 추가한다.)

### 2026-11-16 P07-05 ~ P07-09 트리 생성기, 파서 파사드, 직렬화, 시험

현재 작업 번호: P07-05, P07-06, P07-07, P07-08, P07-09 (구현·시험 완료), 다음은 조정자의 통합(위 "조정자 요청")
완료한 내용:
- `HtmlTags`(태그 번호 123개와 special/formatting/범위 표), `DocumentModes`, `ParserNodeData`, `TreeBuilderBase`(스택, 서식 목록, 삽입 위치, 요소 생성, adoption agency,
  삽입 모드 재설정), `HtmlTreeBuilder`(23 삽입 모드, 외부 콘텐츠, 디스패처), `HtmlParser`(파사드, 재시작, document.write, 일시 정지), `HtmlMarkupSupport`(반복 직렬화, innerHTML).
- 시험: `TreeConstructionTest`(7개 .dat, 256 사례), `HtmlParserTest`(13), `HtmlMarkupSupportTest`(6), `LegacyHtmlTest`(7), `DocumentModesTest`(2), `TreeDump`(html5lib 표기 덤프).
변경 파일: `src/org/duckdns/hjow/swingwebpanel/html/**`, `tests/src/org/duckdns/hjow/swingwebpanel/html/**`, `tests/resources/html/tree/*.dat`, 이 문서.
검증 결과: `scripts/pkgtest.sh html "html dom net" "html"` (JDK 6u45, Windows) → 56 + 7(실행기 자체 시험) 통과, 실패 0. sourcecheck 위반 0.
검증 중 고친 것: 문서 모드 없는 파싱에서 `meta`/`base` 처리가 리스너 유무에 묶여 있던 오류, 시험 기대값 오류 6건(규격을 다시 읽고 구현이 맞음을 확인).
미해결 사항: 외부 적합성 자료 미사용(위 요약). 트리 생성 오류 코드는 규격 이름이 없는 것이 많다. `select` 의 최신 초안 동작은 의도적 미구현.
다음 작업 번호: 통합(P08-02 의 style 처리, P09 의 DOM 사용, P15 의 script 실행 연결).
조정자 요청: 위 "조정자 요청" 참조.

### 2026-11-16 P07-01~P07-04 인코딩 판정, 입력 전처리, 토큰화기, 문자 참조, entitygen

현재 작업 번호: P07-01, P07-02, P07-03, P07-04 (구현·시험 완료)
완료한 내용:
- `tools/entitygen`: entities.json/encodings.json → `NamedCharacterData`/`EncodingLabelData`(청크 문자열 상수) 생성기(Java 6, 자체 `MiniJson`).
- `EncodingLabels`(라벨→표준 이름 40 인코딩, 표준 이름→JRE charset 후보, windows-1252/x-user-defined/replacement 자체 디코더),
  `EncodingSniffer`(BOM → HTTP charset → meta 사전 검사(1024 바이트) → 유효한 UTF-8 추측(확장) → 대체 인코딩), `DecodedText`, `InputPreprocessor`.
- `HtmlTokenizer`: 70 개 상태와 문자 참조(최장 일치, 옛 이름, 속성 값 안의 역사적 규칙, C1 치환표), `insertText`(document.write).
변경 파일: `tools/entitygen/**`, `src/org/duckdns/hjow/swingwebpanel/html/**`, `tests/src/org/duckdns/hjow/swingwebpanel/html/{EncodingSnifferTest,HtmlTokenizerTest}.java`.
검증 결과: EncodingSnifferTest 11건, HtmlTokenizerTest 10건 통과.
