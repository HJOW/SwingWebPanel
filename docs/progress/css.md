# CSS 작업자 진행 기록 (P08)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/css/**`, `tests/src/org/duckdns/hjow/swingwebpanel/css/**`, `tests/resources/css/**`, `docs/progress/css.md`.
시험 실행: `scripts/pkgtest.sh css "css dom net" "css"`. 린트: `tools/sourcecheck/run.sh --only encoding,forbidden,javadoc,loops src/org/duckdns/hjow/swingwebpanel/css tests/src/org/duckdns/hjow/swingwebpanel/css`.

## 통합용 공개 API (레이아웃/렌더링/JS 작업자가 의존하는 계약)

모든 클래스는 Java 6, 외부 의존 없음. `css` 패키지는 `dom`(Document/Element/Node/DomMutationListener/SelectorEngine)과 `net.UrlUtil` 만 참조하며 `html` 패키지에는 의존하지 않는다.

### 가장 쉬운 진입점: `org.duckdns.hjow.swingwebpanel.css.CssEngine`

```java
CssEngine css = new CssEngine(document, styleSheetLoader /* null 가능 */, new MediaEnvironment(viewportWidth, viewportHeight));
css.loadStyleSheets();                       // 로딩 스레드에서: <style>, <link rel=stylesheet>, @import 수집 → 리졸버에 전달
ComputedStyle s = css.getComputedStyle(element);
css.refreshIfNeeded();                       // <style>/<link> 가 바뀌었으면 시트를 다시 모은다(DOM 변경 뒤 호출)
css.getResolver().setMediaEnvironment(env);  // 창 크기/인쇄 등이 바뀌면
css.getDiagnostics();                        // 버려지거나 적용되지 않은 선언/규칙의 진단
css.dispose();
```
`new CssEngine` 은 문서에 `CssSelectorEngine` 을 설치하여 `querySelector*/matches/closest` 가 동작하게 한다.

### StyleSheetLoader (호스트가 구현: P04 요청 계층 위에)

```java
public interface StyleSheetLoader { LoadedStyleSheet load(String absoluteUrl) throws IOException; }   // 텍스트 + 최종 URL
```
구현은 User-Agent/헤더/쿠키/스키마 제한/캐시를 공통 요청기로 처리하고 `StyleSheetDecoder.decode(bytes, httpCharset, referrerCharset)` 로 디코딩한다(BOM > HTTP charset > `@charset` > 참조 문서 > UTF-8).

### ComputedStyle (안정 계약)

`org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle` — 요소(또는 의사 요소) 하나의 계산 스타일. **불변**이며 요소끼리 공유될 수 있다. 값은 `Prop.XXX` 상수(`org.duckdns.hjow.swingwebpanel.css.props.Prop`, 176개)로 읽는다.

| 메서드 | 설명 |
| --- | --- |
| `CssValue get(int id)` / `get(String name)` | 계산 값(절대 null 아님). 상대 길이는 px 의 `CssLength`, 백분율은 `CssPercentage`(또는 백분율이 든 `CssCalc`), 키워드는 `CssKeyword`, 색은 `CssColor`(`currentcolor` 는 키워드로 남음), 목록은 `CssList`(쉼표 목록은 `isComma()`) |
| `double px(int id)` | 절대 길이(px). 키워드/백분율/calc 이면 NaN |
| `double resolve(int id, double percentBase)` | 길이/백분율/calc → px(실제 값). `auto` 같은 키워드이면 NaN |
| `double number(int id)` | 숫자 값(opacity, flex-grow, z-index, order, 단위 없는 line-height). 아니면 NaN |
| `String keyword(int id)` / `isKeyword(id, "x")` / `isAuto(id)` | 키워드 판정 |
| `int color(int id)` / `CssColor colorValue(int id)` | 색을 `0xAARRGGBB` 로. `currentcolor` 는 `color` 속성으로 해석. 색이 아니면 0 / null |
| `display()`, `position()`, `fontSizePx()`, `rootFontSizePx()`, `fontWeight()`(숫자), `lineHeightPx()`(normal 이면 NaN), `fontFamilies()` | 자주 쓰는 값 |
| `getCustomProperty("--x")`, `customPropertyNames()` | 사용자 정의 속성(원본 `CssRaw`) |
| `getPropertyValue(String name)` | `getComputedStyle(el).getPropertyValue(name)` — 직렬화한 값(색은 `rgb()`, 단축 속성은 가능하면 합쳐서) |
| `sameValues(ComputedStyle o)` | 값이 완전히 같은지(재배치/다시 그리기 생략 판단) |
| `static ComputedStyle initial()` | 선언 없는 스타일(시험/기본값용) |

계산 규칙(요약): 상속 속성은 부모 계산 값을 물려받고 그 밖은 초기값. `font-size` 를 먼저 계산(키워드 9/10/13/16/18/24/32/48px, larger/smaller ×1.2, % 와 em 은 부모 기준), 나머지 상대 길이는 자신의 글꼴 크기 기준, `rem` 은 루트 글꼴 크기(루트 요소 자신의 `rem` 은 16px 기준). `font-weight` 는 숫자(bolder/lighter 는 표), `line-height` 의 %/길이는 px 로 숫자는 숫자로. 테두리/outline/column-rule 두께는 style 이 none/hidden 이면 0, 키워드 thin/medium/thick = 1/3/5px.
`float` 가 있거나 `position: absolute|fixed` 이거나 루트 요소이면 `display` 를 블록화(inline→block, inline-block→block, inline-table→table, inline-flex→flex, inline-grid→grid, table-*→block), 절대 배치면 `float: none`. `overflow-x/y` 한쪽이 visible/clip 이 아니면 다른 쪽 visible→auto, clip→hidden. `opacity` 는 0..1 로 제한하고 %는 숫자로.
값의 형태: `display` 등 키워드 속성은 `CssKeyword`; 길이 속성은 `CssLength`(px)/`CssPercentage`/`CssCalc`/`auto`; `font-family`/`transition-*`/`background-*`/`box-shadow` 등은 쉼표 `CssList`(층마다 원소 하나. 원소가 여러 구성 요소이면 공백 `CssList`); `transform` 은 `CssFunctionValue` 의 공백 목록(`translate(10px, 5px)` 의 인수는 `getArguments()`); 이미지는 `CssUrl`(`getResolvedUrl()` 은 스타일 시트 URL 기준 절대 URL) 또는 그라디언트 `CssFunctionValue`.

### StyleResolver (구현 완료)

```java
StyleResolver r = new StyleResolver(document, mediaEnvironment);   // 문서의 DOM 변경을 구독한다. 문서를 소유한 스레드에서만 쓴다(동기화 없음)
r.setStyleSheets(List<StyleSheet>);          // 작성자 스타일 시트(문서 순서). UA 기본 스타일(+호환 모드 추가분)은 자동 포함
r.setUserStyleSheets(List<StyleSheet>);      // 선택: 사용자 스타일 시트
r.setElementState(ElementState);             // :hover/:active/:focus/:checked 등. SimpleElementState 가 기본 구현
r.stateChanged(element);                     // 상태가 바뀐 뒤 호출: 상태를 조회한 스타일만 다시 계산
r.setPresentationalHints(h);                 // 기본 HtmlPresentationalHints(bgcolor, align, width, border, cellpadding, font ...)
r.setMediaEnvironment(env);                  // 값이 다르면 미디어 의존 규칙을 다시 평가하고 전체 무효화
ComputedStyle s  = r.getComputedStyle(element);         // 캐시. 깊이 5000 의 트리도 재귀 없이 계산
ComputedStyle ps = r.getPseudoStyle(element, "before"); // before/after/first-line/first-letter/marker/selection/placeholder/backdrop. 규칙이 없으면 null
r.addChangeListener(StyleChangeListener);    // stylesInvalidated(Element rootOrNull): 낡은 서브트리 루트(null 이면 문서 전체) — 레이아웃이 재배치를 예약
r.invalidate();                              // 전체 무효화(호환 모드가 바뀐 뒤, 시트의 disabled/media 를 바꾼 뒤)
r.consumeSheetSourceChange();                // <style>/<link> 가 바뀌었는지(읽으면 지워짐)
r.getStyleVersion();                         // 무효화마다 증가
r.dispose();
```
캐스케이드 순서(오름차순): UA 일반 < 사용자 일반 < 표현 힌트 < 작성자 일반(`style` 속성 최상위) < 작성자 `!important` < 사용자 `!important` < UA `!important`. 같은 층에서는 구체성, 그다음 소스 순서. `revert` 는 작성자 층 앞의 값으로 되돌린다.
무효화 범위: 속성 변경은 그 요소의 서브트리(스타일 시트가 형제 결합자를 쓰면 부모 서브트리, `:has()` 를 쓰면 문서 전체), 자식 목록 변경은 구조 선택자(`:first-child`, `:empty`, `+`, `:nth-*`)가 있을 때만 부모 서브트리, 동적 상태 변경은 상태를 조회한 요소만.

### 값/속성/선택자 도우미

- `PropertyRegistry.getInstance()` — `byId/byName`, `parseDeclaration(name, tokens, env, out)`(결과 `OK/UNKNOWN/UNSUPPORTED/INVALID`), `longhandsOf(name)`, `names()`, `unsupportedNames()`, `aliasNames()`.
- `org.duckdns.hjow.swingwebpanel.css.values.*` — `CssLength`, `CssPercentage`, `CssNumber`, `CssColor`(`getArgb()`), `CssKeyword`, `CssList`, `CssFunctionValue`(transform, gradient, ...), `CssUrl`, `CssCalc.resolve(lc, base)`, `ValueGrammar`.
- 스타일 시트 모델: `StyleSheet.parse(text, url, origin, loader, diag)`, 규칙 `StyleRule/GroupRule(@media,@supports,@layer)/ImportRule/NamespaceRule/FontFaceRule(getFamily/getSources)/KeyframesRule(getKeyframes)/UnknownRule`, `CssStyleDeclaration`(CSSOM: `getPropertyValue/setProperty/removeProperty/getCssText`, `setChangeListener`), `SupportsCondition.evaluate/supports`(= `CSS.supports`), `MediaQueryList.parse(text).matches(env)`(= `matchMedia`).
- 선택자: `CssSelectorEngine`(`querySelector*/matches/closest`), `SelectorParser`, `ElementState`.

### 알려진 한계 (정직한 기록)

- 블록·함수의 중첩이 256단계를 넘으면 그 지점에서 텍스트가 끝난 것으로 보고(스택 보호), 선택자 인수의 중첩은 24단계까지 허용한다. 치환(`var()`)이 만드는 토큰은 값 하나당 10만 개로 제한한다.
- 계산 값까지 구현했으며 **레이아웃/렌더링은 P09/P10 소관**이다. "파싱만" 표시된 속성(아래 표)은 저장만 하고 렌더러가 쓰지 않는다.
- 논리 속성(`margin-inline-*` 등), 3D 변환(`perspective`, `translate3d()`), `clip-path`, `mask`, `@container`, `@property`, `@page` 는 지원하지 않는다(선언을 버리고 `unsupported` 진단 또는 `UnknownRule`).
- 캐스케이드 레이어(`@layer`)는 파싱하지만 순서는 적용하지 않는다(블록 안 규칙은 일반 규칙처럼 동작). CSS 중첩 규칙(`&`)은 미지원. `ex`/`ch` 는 글꼴 크기의 0.5배로 근사한다(글꼴 측정값은 P09-03).
- `@import` 는 깊이 8, 시트당 64개 제한, 순환은 건너뛴다. `<link>` 는 `rel` 에 `stylesheet` 가 있고 `alternate` 가 없는 것만 적용한다. `body` 의 `link/vlink/alink` 속성은 대응시키지 않는다.
- 색은 sRGB 8비트로 변환하며 색 영역 밖의 값은 채널별로 자른다. `color-mix()` 는 srgb/srgb-linear/oklab 만 지원한다.
- `:in-range/:out-of-range` 는 `ElementState` 가 알려 줄 때만, `:valid/:invalid` 는 required 만 속성으로 판정한다.

## 상태 요약

| 계획 항목 | 상태 | 검증 |
| --- | --- | --- |
| P08-01 토큰화기/파서 | 완료 | CssSyntaxTest |
| P08-02 스타일 시트 소스(외부/style/style 속성/UA 기본 스타일/@import 순환·순서·제한) | 완료 | StyleSheetTest, CssEngineTest |
| P08-03 선택자(타입/id/클래스/속성/결합자/의사 클래스·요소) | 완료 | SelectorTest |
| P08-04 캐스케이드(출처/중요도/구체성/순서, 단축 속성 전개, inherit/initial/unset/revert) | 완료 | StyleResolverTest, PropertyTest, ComputedStyleTest |
| P08-05 값 형식(길이/백분율/색/문자열/URL/calc, 지정/계산/실제 값 구분) | 완료 | ColorTest, CalcTest, GrammarTest, ComputedStyleTest |
| P08-06 미디어 질의와 상태 의존 선택자 무효화 | 완료 | MediaQueryTest, StyleResolverTest |
| P08-07 속성 구현과 미지원 선언 진단 | 계산 값까지 완료(렌더링 적용은 P09/P10) | PropertyTest, StyleSheetTest |
| P08-08 검증 | 완료 | CssEngineTest 등 전체 |
| P08-09 선택자 엔진 + 계산 스타일 일치 검증 | 완료 | SelectorTest, CssEngineTest(35개 선택자를 두 경로로 대조) |

## 속성별 지원 표 (`PropertyReport` 로 생성, 2026-11-16)

"파싱+계산 값"은 선언을 검증하고 상속/초기값/상대 단위/계산 값 규칙까지 구현했다는 뜻이다(레이아웃 사용 여부는 P09/P10). "파싱만"은 저장만 한다.

| 속성 | 종류 | 상속 | 초기값 | 상태 |
| --- | --- | --- | --- | --- |
| `display` | 개별 | 아니오 | `inline` | 파싱+계산 값 |
| `position` | 개별 | 아니오 | `static` | 파싱+계산 값 |
| `top` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `right` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `bottom` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `left` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `float` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `clear` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `z-index` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `overflow-x` | 개별 | 아니오 | `visible` | 파싱+계산 값 |
| `overflow-y` | 개별 | 아니오 | `visible` | 파싱+계산 값 |
| `overflow-wrap` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `visibility` | 개별 | 예 | `visible` | 파싱+계산 값 |
| `opacity` | 개별 | 아니오 | `1` | 파싱+계산 값 |
| `box-sizing` | 개별 | 아니오 | `content-box` | 파싱+계산 값 |
| `width` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `height` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `min-width` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `min-height` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `max-width` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `max-height` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `clip` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `margin-top` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `margin-right` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `margin-bottom` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `margin-left` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `padding-top` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `padding-right` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `padding-bottom` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `padding-left` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `border-top-width` | 개별 | 아니오 | `medium` | 파싱+계산 값 |
| `border-right-width` | 개별 | 아니오 | `medium` | 파싱+계산 값 |
| `border-bottom-width` | 개별 | 아니오 | `medium` | 파싱+계산 값 |
| `border-left-width` | 개별 | 아니오 | `medium` | 파싱+계산 값 |
| `border-top-style` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `border-right-style` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `border-bottom-style` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `border-left-style` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `border-top-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `border-right-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `border-bottom-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `border-left-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `border-top-left-radius` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `border-top-right-radius` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `border-bottom-right-radius` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `border-bottom-left-radius` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `border-collapse` | 개별 | 예 | `separate` | 파싱+계산 값 |
| `border-spacing` | 개별 | 예 | `0px` | 파싱+계산 값 |
| `border-image-source` | 개별 | 아니오 | `none` | 파싱만(렌더링 영향 없음) |
| `border-image-slice` | 개별 | 아니오 | `100%` | 파싱만(렌더링 영향 없음) |
| `border-image-width` | 개별 | 아니오 | `1` | 파싱만(렌더링 영향 없음) |
| `border-image-outset` | 개별 | 아니오 | `0px` | 파싱만(렌더링 영향 없음) |
| `border-image-repeat` | 개별 | 아니오 | `stretch` | 파싱만(렌더링 영향 없음) |
| `outline-width` | 개별 | 아니오 | `medium` | 파싱+계산 값 |
| `outline-style` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `outline-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `outline-offset` | 개별 | 아니오 | `0px` | 파싱+계산 값 |
| `box-shadow` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `background-color` | 개별 | 아니오 | `transparent` | 파싱+계산 값 |
| `background-image` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `background-repeat` | 개별 | 아니오 | `repeat` | 파싱+계산 값 |
| `background-attachment` | 개별 | 아니오 | `scroll` | 파싱+계산 값 |
| `background-position` | 개별 | 아니오 | `0% 0%` | 파싱+계산 값 |
| `background-size` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `background-origin` | 개별 | 아니오 | `padding-box` | 파싱+계산 값 |
| `background-clip` | 개별 | 아니오 | `border-box` | 파싱+계산 값 |
| `color` | 개별 | 예 | `black` | 파싱+계산 값 |
| `font-family` | 개별 | 예 | `serif` | 파싱+계산 값 |
| `font-size` | 개별 | 예 | `medium` | 파싱+계산 값 |
| `font-style` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `font-variant` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `font-weight` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `font-stretch` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `line-height` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `font-size-adjust` | 개별 | 예 | `none` | 파싱만(렌더링 영향 없음) |
| `font-kerning` | 개별 | 예 | `auto` | 파싱만(렌더링 영향 없음) |
| `font-feature-settings` | 개별 | 예 | `normal` | 파싱만(렌더링 영향 없음) |
| `text-align` | 개별 | 예 | `start` | 파싱+계산 값 |
| `text-align-last` | 개별 | 예 | `auto` | 파싱+계산 값 |
| `text-indent` | 개별 | 예 | `0px` | 파싱+계산 값 |
| `text-transform` | 개별 | 예 | `none` | 파싱+계산 값 |
| `text-decoration-line` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `text-decoration-style` | 개별 | 아니오 | `solid` | 파싱+계산 값 |
| `text-decoration-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `text-decoration-thickness` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `text-underline-offset` | 개별 | 예 | `auto` | 파싱만(렌더링 영향 없음) |
| `text-overflow` | 개별 | 아니오 | `clip` | 파싱+계산 값 |
| `text-shadow` | 개별 | 예 | `none` | 파싱+계산 값 |
| `letter-spacing` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `word-spacing` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `white-space` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `word-break` | 개별 | 예 | `normal` | 파싱+계산 값 |
| `line-break` | 개별 | 예 | `auto` | 파싱만(렌더링 영향 없음) |
| `hyphens` | 개별 | 예 | `manual` | 파싱+계산 값 |
| `tab-size` | 개별 | 예 | `8` | 파싱+계산 값 |
| `vertical-align` | 개별 | 아니오 | `baseline` | 파싱+계산 값 |
| `direction` | 개별 | 예 | `ltr` | 파싱+계산 값 |
| `unicode-bidi` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `writing-mode` | 개별 | 예 | `horizontal-tb` | 파싱+계산 값 |
| `text-orientation` | 개별 | 예 | `mixed` | 파싱만(렌더링 영향 없음) |
| `orphans` | 개별 | 예 | `2` | 파싱만(렌더링 영향 없음) |
| `widows` | 개별 | 예 | `2` | 파싱만(렌더링 영향 없음) |
| `list-style-type` | 개별 | 예 | `disc` | 파싱+계산 값 |
| `list-style-position` | 개별 | 예 | `outside` | 파싱+계산 값 |
| `list-style-image` | 개별 | 예 | `none` | 파싱+계산 값 |
| `counter-reset` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `counter-increment` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `counter-set` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `quotes` | 개별 | 예 | `auto` | 파싱+계산 값 |
| `content` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `table-layout` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `caption-side` | 개별 | 예 | `top` | 파싱+계산 값 |
| `empty-cells` | 개별 | 예 | `show` | 파싱+계산 값 |
| `flex-grow` | 개별 | 아니오 | `0` | 파싱+계산 값 |
| `flex-shrink` | 개별 | 아니오 | `1` | 파싱+계산 값 |
| `flex-basis` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `flex-direction` | 개별 | 아니오 | `row` | 파싱+계산 값 |
| `flex-wrap` | 개별 | 아니오 | `nowrap` | 파싱+계산 값 |
| `order` | 개별 | 아니오 | `0` | 파싱+계산 값 |
| `justify-content` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `align-items` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `align-self` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `align-content` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `justify-items` | 개별 | 아니오 | `legacy` | 파싱만(렌더링 영향 없음) |
| `justify-self` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `row-gap` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `column-gap` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `column-count` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `column-width` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `column-rule-width` | 개별 | 아니오 | `medium` | 파싱+계산 값 |
| `column-rule-style` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `column-rule-color` | 개별 | 아니오 | `currentcolor` | 파싱+계산 값 |
| `column-span` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `column-fill` | 개별 | 아니오 | `balance` | 파싱+계산 값 |
| `grid-template-columns` | 개별 | 아니오 | `none` | 파싱만(렌더링 영향 없음) |
| `grid-template-rows` | 개별 | 아니오 | `none` | 파싱만(렌더링 영향 없음) |
| `grid-template-areas` | 개별 | 아니오 | `none` | 파싱만(렌더링 영향 없음) |
| `grid-auto-flow` | 개별 | 아니오 | `row` | 파싱만(렌더링 영향 없음) |
| `grid-auto-columns` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `grid-auto-rows` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `grid-row-start` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `grid-row-end` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `grid-column-start` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `grid-column-end` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `transform` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `transform-origin` | 개별 | 아니오 | `50% 50%` | 파싱+계산 값 |
| `transition-property` | 개별 | 아니오 | `all` | 파싱+계산 값 |
| `transition-duration` | 개별 | 아니오 | `0s` | 파싱+계산 값 |
| `transition-timing-function` | 개별 | 아니오 | `ease` | 파싱+계산 값 |
| `transition-delay` | 개별 | 아니오 | `0s` | 파싱+계산 값 |
| `animation-name` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `animation-duration` | 개별 | 아니오 | `0s` | 파싱+계산 값 |
| `animation-timing-function` | 개별 | 아니오 | `ease` | 파싱+계산 값 |
| `animation-delay` | 개별 | 아니오 | `0s` | 파싱+계산 값 |
| `animation-iteration-count` | 개별 | 아니오 | `1` | 파싱+계산 값 |
| `animation-direction` | 개별 | 아니오 | `normal` | 파싱+계산 값 |
| `animation-fill-mode` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `animation-play-state` | 개별 | 아니오 | `running` | 파싱+계산 값 |
| `cursor` | 개별 | 예 | `auto` | 파싱+계산 값 |
| `pointer-events` | 개별 | 예 | `auto` | 파싱+계산 값 |
| `user-select` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `resize` | 개별 | 아니오 | `none` | 파싱+계산 값 |
| `appearance` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `accent-color` | 개별 | 예 | `auto` | 파싱만(렌더링 영향 없음) |
| `caret-color` | 개별 | 예 | `auto` | 파싱+계산 값 |
| `scroll-behavior` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `object-fit` | 개별 | 아니오 | `fill` | 파싱+계산 값 |
| `object-position` | 개별 | 아니오 | `50% 50%` | 파싱+계산 값 |
| `image-rendering` | 개별 | 예 | `auto` | 파싱+계산 값 |
| `filter` | 개별 | 아니오 | `none` | 파싱만(렌더링 영향 없음) |
| `mix-blend-mode` | 개별 | 아니오 | `normal` | 파싱만(렌더링 영향 없음) |
| `isolation` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `will-change` | 개별 | 아니오 | `auto` | 파싱만(렌더링 영향 없음) |
| `contain` | 개별 | 아니오 | `none` | 파싱만(렌더링 영향 없음) |
| `text-rendering` | 개별 | 예 | `auto` | 파싱만(렌더링 영향 없음) |
| `aspect-ratio` | 개별 | 아니오 | `auto` | 파싱+계산 값 |
| `animation` | 단축(8개 개별 속성) | - | - | 전개 구현 |
| `background` | 단축(8개 개별 속성) | - | - | 전개 구현 |
| `border` | 단축(17개 개별 속성) | - | - | 전개 구현 |
| `border-bottom` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `border-color` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `border-left` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `border-radius` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `border-right` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `border-style` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `border-top` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `border-width` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `column-rule` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `columns` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `flex` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `flex-flow` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `font` | 단축(10개 개별 속성) | - | - | 전개 구현 |
| `gap` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `inset` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `list-style` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `margin` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `outline` | 단축(3개 개별 속성) | - | - | 전개 구현 |
| `overflow` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `padding` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `place-content` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `place-items` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `place-self` | 단축(2개 개별 속성) | - | - | 전개 구현 |
| `text-decoration` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `transition` | 단축(4개 개별 속성) | - | - | 전개 구현 |
| `all` | 단축(CSS 전역 키워드만) | - | - | 전개 구현 |

개별 속성 176개(파싱+계산 값 142, 파싱만 34), 단축 속성 29개.

별칭(접두사 붙은 이름 등): 221개. 예: `-moz-align-content`→`align-content` `-moz-align-items`→`align-items` `-moz-align-self`→`align-self` `-moz-animation`→`animation` `-moz-animation-delay`→`animation-delay` `-moz-animation-direction`→`animation-direction` 

알아보지만 지원하지 않는 이름(선언을 버리고 `unsupported` 진단): 163개 — `-moz-osx-font-smoothing`, `-moz-text-size-adjust`, `-ms-overflow-style`, `-ms-text-size-adjust`, `-webkit-box-flex`, `-webkit-box-orient`, `-webkit-font-smoothing`, `-webkit-line-clamp`, `-webkit-overflow-scrolling`, `-webkit-print-color-adjust`, `-webkit-tap-highlight-color`, `-webkit-text-size-adjust`, `alignment-baseline`, `backdrop-filter`, `backface-visibility`, `baseline-shift`, `block-size`, `border-block`, `border-block-color`, `border-block-end`, `border-block-start`, `border-block-style`, `border-block-width`, `border-end-end-radius`, `border-end-start-radius`, `border-image`, `border-inline`, `border-inline-color`, `border-inline-end`, `border-inline-start`, `border-inline-style`, `border-inline-width`, `border-start-end-radius`, `border-start-start-radius`, `break-after`, `break-before`, `break-inside`, `clip-path`, `clip-rule`, `color-interpolation`, `color-interpolation-filters`, `color-scheme`, `contain-intrinsic-size`, `container`, `container-name`, `container-type`, `content-visibility`, `cx`, `cy`, `d`, `dominant-baseline`, `fill`, `fill-opacity`, `fill-rule`, `flood-color`, `flood-opacity`, `font-language-override`, `font-optical-sizing`, `font-synthesis`, `font-variant-east-asian`, `font-variant-ligatures`, `font-variant-numeric`, `font-variant-position`, `font-variation-settings`, `forced-color-adjust`, `grid`, `grid-area`, `grid-auto-position`, `grid-column`, `grid-row`, `grid-template`, `hanging-punctuation`, `hyphenate-character`, `initial-letter`, `inline-size`, `inset-block`, `inset-block-end`, `inset-block-start`, `inset-inline`, `inset-inline-end`, `inset-inline-start`, `lighting-color`, `line-clamp`, `margin-block`, `margin-block-end`, `margin-block-start`, `margin-inline`, `margin-inline-end`, `margin-inline-start`, `marker`, `marker-end`, `marker-mid`, `marker-start`, `mask`, `mask-image`, `mask-position`, `mask-repeat`, `mask-size`, `mask-type`, `max-block-size`, `max-inline-size`, `min-block-size`, `min-inline-size`, `offset`, `offset-path`, `overscroll-behavior`, `overscroll-behavior-x`, `overscroll-behavior-y`, `padding-block`, `padding-block-end`, `padding-block-start`, `padding-inline`, `padding-inline-end`, `padding-inline-start`, `page-break-after`, `page-break-before`, `page-break-inside`, `paint-order`, `perspective`, `perspective-origin`, `print-color-adjust`, `r`, `rotate`, `ruby-align`, `ruby-position`, `rx`, `ry`, `scale`, `scroll-margin`, `scroll-padding`, `scroll-snap-align`, `scroll-snap-type`, `scrollbar-color`, `scrollbar-gutter`, `scrollbar-width`, `shape-image-threshold`, `shape-margin`, `shape-outside`, `shape-rendering`, `stop-color`, `stop-opacity`, `stroke`, `stroke-dasharray`, `stroke-dashoffset`, `stroke-linecap`, `stroke-linejoin`, `stroke-opacity`, `stroke-width`, `text-anchor`, `text-combine-upright`, `text-emphasis`, `text-emphasis-color`, `text-emphasis-style`, `text-justify`, `text-size-adjust`, `text-wrap`, `touch-action`, `transform-style`, `translate`, `vector-effect`, `x`, `y`, `zoom`

## 작업 기록

### 2026-11-16 P08-01 ~ P08-09
- P08-01 `css.syntax`: 토큰화기/파서(스펙의 오류 복구, 20만 겹 중첩도 반복형으로 처리), CssSyntaxTest 17건.
- P08-03/09 `css.selector`: 선택자 파서/매처/엔진(`:has`, An+B, HTML 대소문자 규칙, 네임스페이스, 작업량 한도), SelectorTest 18건.
- P08-05/07 `css.values`, `css.props`: 값 형식(색 11종 표기, calc, 문법 엔진), 속성 표 176개, 단축 속성 29개, 별칭 221개, 지원하지 않는 이름 163개, ColorTest 7 + CalcTest 7 + GrammarTest 10 + PropertyTest 12 + CssFuzzTest 3(무작위 입력 4000개, 변형 시트 600개, 적대적 중첩/크기).
- P08-04/06 `css.cascade`, `css.media`, `css.cssom`: ComputedStyle/StyleComputer/VarResolver(var() 치환, 순환, 폴백), StyleResolver(캐스케이드, 의사 요소, 무효화), 미디어 질의, 스타일 시트 모델/UA 기본 스타일/로더 계약, ComputedStyleTest 9 + StyleResolverTest 14 + MediaQueryTest 9 + StyleSheetTest 14 + CssEngineTest 3.
검증 결과: `scripts/pkgtest.sh css "css dom net" "css"` (JDK 6u45, 병렬 작업 중 CPU 포화 환경) → **131건 통과, 실패 0, 건너뜀 0**. sourcecheck(encoding, forbidden, javadoc, loops) 위반 0.
미해결 사항: 위 "알려진 한계" 참조. 속성 표의 `Prop` 클래스는 `tests/resources/css/generate-prop.sh` 로 `PropertyTable` 에서 생성한다(PropertyTest 가 일치를 확인).
다음 작업 번호: 이 작업자의 범위(P08)는 끝났다. 이후는 P09(레이아웃)가 `ComputedStyle` 을 소비하고, P11 이 `ElementState`/`stateChanged` 를 연결하며, P15 가 `CssStyleDeclaration`(`element.style`), `getComputedStyle`, `matchMedia`, `CSS.supports` 를 JS 에 연결한다.
조정자 요청: 없음. (UA 기본 스타일을 Java 문자열 상수로 두었으므로 리소스 복사 변경이 필요 없다. 호스트는 `StyleSheetLoader` 구현과 `CssEngine.loadStyleSheets()` 호출, `ElementState` 연결만 하면 된다.)

체크박스 후보: P08-01, P08-02, P08-03, P08-04, P08-05, P08-06, P08-07, P08-08, P08-09 (그리고 단계 완료 항목 P08-09 로 P08 전체)
단, P08-07 은 "계산 값까지"이며 실제 화면 적용은 P09/P10 에서 다시 확인해야 한다(FEATURE_MATRIX 의 상태는 "부분 구현"으로 두는 것을 권한다).
