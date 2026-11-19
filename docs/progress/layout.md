# LAYOUT-A 진행 기록 (P09-01, P09-02, P09-03)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/layout/**`, `tests/src/org/duckdns/hjow/swingwebpanel/layout/**`, 이 파일.
시험 실행: `scripts/pkgtest.sh lay "layout css html dom core net storage content media @root" "layout"` (JDK 6u45). 린트: `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/layout tests/src/org/duckdns/hjow/swingwebpanel/layout`.

## 통합용 공개 API (렌더러/조정자가 의존하는 계약)

```java
Document doc = ...;                       // core.HtmlPageDocument.getDocument() 등
CssEngine css = new CssEngine(doc, loader, new MediaEnvironment(w, h));  css.loadStyleSheets();
LayoutResult r = LayoutEngine.layout(doc, new CssStyleSource(css), replacedResolverOrNull, viewportWidth, viewportHeight);
r.getRoot(); r.getContentWidth(); r.getContentHeight();      // preferredSize 용 문서 크기(스크롤 영역)
r.getBoxes(domNode); r.boxAt(x, y); r.getBoxCount();
```

- **전체 재배치**만 한다(`LayoutEngine.layout` 은 호출마다 처음부터 새 `LayoutResult`). 너비가 바뀌면 새 너비로 다시 호출한다. 재배치 시점은 호출자(문서 스레드)가 정한다: 뷰포트 너비/DOM/스타일/치환 콘텐츠가 바뀌었을 때만, 레이아웃 콜백 안에서는 하지 않는다(반복 없음).
- **스레드**: 호출은 DOM 과 스타일을 읽으므로 문서 소유 스레드에서 한다(호출자를 막음). 내부 재귀가 깊으므로 **64 MiB 스택의 보조 스레드**에서 계산하고 보조 스레드는 문서에 쓰지 않는다. 결과(`LayoutResult`/`LayoutBox`)는 반환 뒤 바뀌지 않으므로 EDT 에서 읽어도 된다. 실행 중 취소는 불가.
- **좌표**: 모든 값은 CSS 픽셀의 **문서 좌표**(원점 = 페이지 왼쪽 위, 스크롤은 렌더러 몫). `LayoutBox.getX/Y/Width/Height` 는 border box, margin/border/padding 은 사용 값(`getMarginTop()` 등)이라 padding box/content box/margin box 는 더해서 얻는다. 기준선(`getBaseline()`)은 절대 y.
- **상자 트리**(`LayoutBox.Type`): `BLOCK`(요소, 목록 항목, 루트), `ANONYMOUS_BLOCK`(블록 사이의 인라인 콘텐츠), `INLINE_BLOCK`, `REPLACED`(블록/인라인 수준은 `isInlineLevel()`), `LINE`(줄 상자, 너비 = 블록 content 너비), `INLINE`(인라인 요소의 줄별 조각: 배경/border/padding 영역), `TEXT`(줄 위의 텍스트 조각), `MARKER`(바깥쪽 목록 표식).
  자식 순서가 그리기 순서(상자 → 자식). 줄의 자식은 `INLINE` 조각들이 먼저, 그다음 `TEXT`/원자적 상자가 **시각 순서**로 온다.
- **텍스트 조각**: `getText()`(공백 접힘/`text-transform` 적용된 그려지는 문자열, 논리 순서), `getSourceStart/End()`(원본 Text 노드 데이터 안의 범위), `getFont()`/`getAscent()`/`getDescent()`, `getBidiLevel()`(홀수 = 오른쪽에서 왼쪽), `getTextDecoration()`(`DECORATION_*` 비트), `getLetterSpacing()`, `getNode()`(Text 노드 또는 생성 콘텐츠의 요소), `getStyle()`(소유 요소의 스타일 → 색 등).
  `text-align: justify` 줄은 낱말마다 조각이 따로이고 공백은 간격이다.
- **의사 요소**: `LayoutBox.getPseudo()` 가 `before`/`after`. **치환 요소**: `ReplacedResolver.getReplacedContent(element)` → `ReplacedContent` 의 자연 크기. 없으면 HTML `width/height` 속성, 그다음 기본 크기(video/canvas/iframe/embed/object/svg 300×150, audio 300×54, input 150×21, checkbox 13×13, select 120×21, textarea cols/rows).
- **어댑터**: `CssStyleSource` 가 `css.CssEngine` 을 `StyleSource` 로 연결한다(계산 스타일 + `::before/::after`).
- **자르기**: `LayoutBox.isClipsContent()`(overflow ≠ visible)이면 렌더러가 padding box 로 클립한다. `LayoutResult.boxAt` 은 클립 조상의 border box 밖에서는 자손을 찾지 않는다.

## 구현한 것

| 항목 | 내용 | 시험 |
| --- | --- | --- |
| P09-01 레이아웃 트리 | DOM 과 분리, `display:none` 제외, `display:contents`, 익명 블록, 인라인 항목 펼치기, 인라인 안의 블록으로 인한 분할, 치환 요소, `::before/::after`, 목록 표식(`ol start/reversed`, `li value`, disc/circle/square/decimal/decimal-leading-zero/alpha/roman/greek, inside/outside), 깊이 제한 512 | BlockLayoutTest, ListAndGeneratedContentTest |
| P09-02 정상 흐름 | 사용 너비/가로 margin(auto, %, min/max, `box-sizing`, 중앙 정렬, 제약 초과), 높이(지정, %, min/max), margin 합치기(형제, 부모-첫/마지막 자식, 빈 상자 관통, 음수 margin, BFC 루트 차단), shrink-to-fit, 치환 요소 크기(비율), `position:relative` | BlockLayoutTest |
| P09-03 텍스트 | 글꼴 선택/대체(Dialog 로 한글 대체), 측정, 공백 접기(`white-space` 6가지, 탭 정지), 줄바꿈(`BreakIterator`, `overflow-wrap`, `word-break` break-all/keep-all/break-word), `text-align`(justify 포함), `text-indent`, `line-height`, `vertical-align`(baseline/sub/super/top/bottom/middle/text-top/text-bottom/길이/%), `letter/word-spacing`, `text-transform`, `text-decoration` 정보, 한글/서로게이트/양방향(java.text.Bidi) | InlineLayoutTest |
| 견고성 | 빈/특이 문서, 깊은 중첩 5000단 → 512 에서 자름, 요소 1만 개, 긴 텍스트 재배치, 결과 독립성 | LayoutRobustnessTest |

시험: 44건 통과(JDK 6u45, 기반 시설 7건 포함). 시험은 실제 `HtmlParser` + `CssEngine`(UA 시트 포함)으로 문서를 만들고 `font: 10px monospace` 로 기대 너비를 글꼴이 알려 주는 advance 로 계산한다.

## 알려진 한계와 결정

1. **JRE 6u45 의 네이티브 충돌**: 이 환경(Windows, JRE 6u45)에서 `Font.getStringBounds`/`TextLayout` 이 **히브리어/아랍어 같은 복합 문자 체계**에서 `fontmanager.dll` 접근 위반으로 JVM 전체를 죽이거나 너비 0 을 돌려준다(`FontResolver.isComplex` 범위). 레이아웃은 그런 텍스트를 글자별 advance 합(`FontMetrics.charWidth`, 모양 변환/커닝 없음)으로 잰다. **렌더러가 같은 텍스트를 `drawString`/`TextLayout` 으로 그리면 같은 충돌이 날 수 있으니 복합 텍스트는 글자(코드 포인트) 단위로 `drawChars` 하거나 먼저 환경 검증이 필요하다**(P09-04 담당에게 전달).
2. `float`, 절대/고정 배치, `position:sticky`, 표, flexbox, grid, 다단은 구현하지 않았다(P10): 해당 상자는 정상 흐름의 보통 블록(표 셀 등도 블록)으로 배치된다. 확장 지점은 `BlockLayouter.finish`(상자별 내용 배치)와 `BoxBuilder`(display 분류).
3. 인라인 요소의 `position:relative` 는 적용하지 않는다(블록, inline-block, 치환 요소만).
4. 양방향 재배열이 필요한 줄에서는 인라인 요소의 가로 margin/border/padding 을 적용하지 않는다.
5. `vertical-align` 은 자기 범위만 쓴다(하위 트리 범위 합산 없음). `top`/`bottom` 정렬 상자의 자손은 부모의 이동량을 따르지 않는다.
6. 인라인 요소는 자신의 주 글꼴 메트릭으로 줄 높이에 기여하고, 대체 글꼴은 조각 상자의 높이만 바꾼다.
7. 인라인 요소 안의 블록은 CSS 2.1 처럼 인라인을 둘로 나누지만 양쪽 조각의 가장자리(border/padding)는 시작/끝에만 준다(`box-decoration-break: slice`).
8. `font-variant: small-caps`, `text-justify`, `hyphens`, `text-overflow`, `writing-mode` 세로쓰기, `ruby` 는 미구현(무시).
9. `content` 는 문자열, `attr()`, `open-quote/close-quote` 만 처리한다(counter(), url() 미지원).
10. 한국어 줄바꿈 기회는 JRE `BreakIterator` 가 정한다(한글 음절 사이에서도 끊김, `word-break: keep-all` 로 억제).

## CSS 작업자에게 필요한 것 (조정자 전달)

- 현재 계약(`ComputedStyle`, `StyleResolver.getComputedStyle/getPseudoStyle`, UA 시트)으로 충분하다. 추가로 있으면 좋은 것: `display: list-item` 의 `::marker` 스타일(`getPseudoStyle(e, "marker")`)을 쓸 수 있게 하는 것(지금은 항목 자신의 스타일로 표식을 그림).

## 작업 기록

### 2026-11-16 P09-01 ~ P09-03 레이아웃 엔진 구현과 시험
완료한 내용: 위 표. 변경 파일: `src/org/duckdns/hjow/swingwebpanel/layout/*`(LayoutEngine, LayoutResult, LayoutBox, StyleSource, CssStyleSource, ReplacedContent, ReplacedResolver, BoxBuilder, BlockLayouter, InlineLayouter, InlineItem, Margin, FontResolver, TextMeasure, Css, Replaced, ListMarkers), `tests/src/org/duckdns/hjow/swingwebpanel/layout/*`(LayoutTestSupport, BlockLayoutTest, InlineLayoutTest, ListAndGeneratedContentTest, LayoutRobustnessTest).
검증 결과: `scripts/pkgtest.sh lay "layout css html dom core net storage content media @root" "layout"` → 44 passed / 0 failed (JDK 6u45). sourcecheck 0 위반.
체크박스 후보: P09-01, P09-02, P09-03 (HTML-04/05/06/07, CSS 상자/텍스트 모듈의 레이아웃 부분). P09-09 의 레이아웃 절반(좌표/크기 검증)도 통과했고, 그리기 절반은 렌더러 작업자 몫이다.
미해결/다음: 렌더러(P09-04~06)가 이 트리를 그린다. P09-07(preferredSize/재배치 루프 방지)과 P09-08(통합)은 조정자가 `SwingWebPanel` 에 연결한다. P10 에서 float/position/table/flex 를 추가한다.
