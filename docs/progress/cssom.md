# CSSOM 작업(DOM-13): styleSheets, 규칙 API, 생성 스타일 시트, CSS 네임스페이스, document.fonts

작업 브랜치: `worktree-agent-a270495658554d6b1`(스냅샷 기준 `50e9109` 에서 갈라짐). 커밋 `20261117-110` ~ `20261117-117`(+ 이 문서의 커밋). push 하지 않았다.

## 조정자 요청

### FEATURE_MATRIX DOM-13 행 문구(제안)

`DOM-13 | CSSOM: getComputedStyle(의사 요소 인수 포함), element.style(CSSStyleDeclaration), document.styleSheets, StyleSheet/CSSStyleSheet(insertRule, deleteRule, addRule, removeRule, disabled, media), MediaList, CSSRuleList, CSSRule 계열(CSSStyleRule, CSSMediaRule, CSSSupportsRule, CSSImportRule, CSSFontFaceRule, CSSKeyframesRule/CSSKeyframeRule, CSSNamespaceRule, CSSLayerBlockRule/CSSLayerStatementRule, 그 밖의 종류는 일반 CSSRule), 생성 스타일 시트와 adoptedStyleSheets, CSS.supports/CSS.escape, document.fonts(FontFaceSet)/FontFace | css.cssom.*, js.dom.{CssomInterfaces,CssomMembers,StyleObject}, js.host.{CssomBindings,CssNamespaceApi,CssFontApi} | CssomSheetsTest, CssomRulesTest, CssomConstructableTest, CssomNamespaceTest, CssomFontsTest, CssomDeclarationTest, CssomIntegrationTest | 부분 구현(구현한 범위는 검증 완료) | 아래 "한계"`

규격 판본(조회 2026-11-17, `tools/webfetch`, JRE 25 로 실행): CSSOM — W3C Working Draft 2021-08-26(`https://www.w3.org/TR/2021/WD-cssom-1-20210826/`), CSS Conditional Rules 3 — CRD 2024-08-15(`CSS.supports`), CSS Font Loading 3 — WD 2023-04-06. 참고 구현으로 Chromium 의 동작은 기억에 의존했고 소스를 조회하지는 않았다(아래 "Chromium 과의 선택" 참조).

### 체크 후보

- 기능 표 DOM-13: 위 문구로 "부분 구현"(한계 목록을 둔 채). 모든 하위 요구(범위 1~6)는 구현했고 검증했으나 CSSOM 전체(CSSPageRule 의 `selectorText`/`style`, `CSSPropertyRule`, `CSSContainerRule` 전용 인터페이스, 생성 스타일 시트의 구성 문서 검사)는 아니다.
- `docs/progress/api-surface.md`: "CSSStyleSheet/StyleSheet/CSSRule/CSSRuleList, document.styleSheets, document.fonts, adoptedStyleSheets, CSSOM 규칙 API" 행과 Document 표의 `document.styleSheets` 행을 "구현함"(표 "이 세션에서 구현한 것" 으로 이동)으로, `CSS` 네임스페이스(`CSS.supports`, `CSS.escape`)를 추가.
- `FEATURE_MATRIX` CSS-45 행의 "DOM-13/14 와 연결" 비고: DOM-13 은 이 문서로 연결.
- AGENTS.md 체크 항목은 없다(DOM-13 은 P20-01 의 일부이고 P20-01 은 이 작업만으로 끝나지 않는다).

### 병합 주의(CSS-H 브랜치 `worktree-agent-a10ac11cd0afc4b47`와의 충돌 예상)

CSS-H 는 `css/cssom` 에 `ContainerCondition`, `CounterStyleRule`, `PropertyRule`, `ScopeSpec` 를 더하고 `CssRule`(상수 101~104), `GroupRule`(`container`/`scope` 필드와 팩터리), `StyleSheetBuilder`(at-규칙 해석)를 고쳤다. 이 작업은 같은 파일을 가산적으로 바꿨다.

- `CssRule`: 이쪽은 필드(`parentRule`, `parentSheet`), `getCssomType()`, `attach/detach/changed`, `getParentRule/getParentStyleSheet`, 빈 `setCssText` 를 더했다. 상수 블록 근처는 CSS-H 가 상수를 더하는 곳과 가깝다 → 둘 다 남긴다.
- `GroupRule`: 이쪽은 `rules` 를 변경 가능 목록(`rulesView` 가 읽기 전용 뷰)으로 바꾸고 `media` 를 비 final 로 했으며 `insertRule/deleteRule/getCssRules/getMediaList/getConditionText/getName/getNameList/attach/detach` 를 더했다. CSS-H 의 `container`/`scope` 필드와 팩터리는 그대로 두면 된다. 병합 뒤 할 일: **`CssRule.getCssomType()` 는 100 이상을 0 으로 돌려주므로** `COUNTER_STYLE_RULE`(104)만 11 로 보고하도록 고치고(`CSSCounterStyleRule`), `CssomInterfaces.classify` 에서 `CONTAINER_RULE` → `CSSContainerRule`(부모 `CSSConditionRule`), `SCOPE_RULE` → `CSSScopeRule`, `PropertyRule` → `CSSPropertyRule` 로 분류하는 인터페이스를 더한다(지금은 GroupRule 이면 `CSSGroupingRule`, 그 밖의 새 규칙은 일반 `CSSRule` 로 노출된다).
- `StyleSheetBuilder`: 이쪽은 생성자(네임스페이스 맵을 받는 것, 편집용 `forEditing`), `parseOne`, `resolveImports`, `buildKeyframe` 추출, `loadImport` 의 href/loader 설정, `buildNamespace` 의 `editing` 분기를 바꿨다. `buildAtRule` 본문은 건드리지 않았다.
- `StyleResolver`: `getPseudoComputedStyle` 하나만 더했다.

## 구현한 것(범위별)

### 1. `document.styleSheets`, StyleSheet/CSSStyleSheet, MediaList, `insertRule`/`deleteRule`

- `css/cssom/StyleSheet` 가 곧 스크립트의 `CSSStyleSheet` 이며 스타일 리졸버가 읽는 객체와 같다. 규칙 목록은 변경 가능하고(`getRules()` 는 따라가는 읽기 전용 뷰) `href`, `ownerNode`, `parentStyleSheet`, `ownerRule`, `title`, `media`(`MediaList`), `disabled` 를 가진다.
- `insertRule(text, index)`: CSSOM 6.4.1 "insert a CSS rule" 알고리즘(`RuleEditor`): 위치 → `IndexSizeError`, 규칙 정확히 하나로 파싱(아니면 `SyntaxError`, 알 수 없는 at-규칙과 올바르지 않은 선택자 포함), 위치 제약 → `HierarchyRequestError`(`@import` 앞에 서문 규칙만, 그룹 안의 `@import`/`@namespace`), `@namespace` 가 다른 규칙이 있는 시트에 → `InvalidStateError`. 생성 시트에 `@import` → `SyntaxError`. `deleteRule` 은 `IndexSizeError`/`InvalidStateError`(`@namespace`). 옛 `addRule(selector, style, index=끝)`(-1 반환), `removeRule(index=0)`, `rules` 별칭.
- `document.styleSheets`(`StyleSheetList`): 엔진이 문서 순서로 모은 시트(`<style>`, 로딩이 끝난 `<link rel=stylesheet>`; 대체 스타일 시트와 아직 오지 않은 링크는 없음, UA 시트 없음). `style.sheet`, `link.sheet`, 두 요소의 `disabled`(시트의 플래그를 읽고 쓰며 시트가 없을 때만 속성).
- **시트 동일성**: `DocumentStyleSheets.collect(doc, loader, diag, previous)` 가 텍스트(`<style>`)나 주소(`<link>`)가 같으면 이전 시트 객체를 재사용한다. 그래서 다른 `<style>` 이 추가되거나 늦은 스타일 시트가 도착해도(`PagePresentation.sheetsChanged` 로 매번 다시 모은다) 스크립트가 붙잡은 시트와 그 안에 넣은 규칙이 유지된다. 재사용한 시트는 파싱 때 읽지 못한 `@import`(리소스가 아직 오는 중)를 `resolvePendingImports` 로 제자리에서 채운다. 소유 요소의 `media/title/disabled` 속성은 속성이 바뀐 때만 시트에 적용한다(스크립트가 바꾼 미디어와 비활성 플래그를 지우지 않는다). 텍스트가 바뀐 `<style>` 은 새 시트 객체를 받는다.
- 변경 통지: 시트와 규칙의 변경은 `StyleSheet.changed()` → `CssEngine` 의 시트 리스너 → `StyleResolver.invalidate()`(캐시와 규칙 색인 폐기) → `CssEngine.setChangeListener` → `PagePresentation.invalidate()`(레이아웃 무효화와 뷰 재그림). 엔진은 쓰는 시트에만 리스너를 걸고 빠진 시트와 `dispose()` 시 끊는다.
- `MediaList`(`css/cssom/MediaList`): `mediaText`(대입 가능, `null`→""), `length`, `item`, `appendMedium`(중복 무시, 질의가 아니면 무시), `deleteMedium`(없으면 `NotFoundError`, 질의가 아니면 무시), 문자열화. 빈 목록은 `mediaText` ""이고 길이 0이지만 모든 환경에 일치한다(`MediaQueryList.isEmptyList`). `sheet.media = 'print'` 는 `mediaText` 로 전달(`[PutForwards]`).
- **교차 오리진**: 문서와 다른 오리진에서 읽은 시트(`href` 가 있고 `Origin.isSameOrigin` 이 아님)의 `cssRules`, `rules`, `insertRule`, `deleteRule`, `addRule`, `removeRule` 은 `SecurityError`(CSSOM 6.2 의 origin-clean flag). 스타일 적용에는 영향이 없고 `href`, `disabled` 등은 읽을 수 있다. `about:text`(불투명 오리진) 문서는 `href` 가 있는 시트를 읽을 수 없을 것이다(코드로 추정, 미확인).

### 2. 규칙 객체

- `CSSRule`(`type`, `cssText`(대입은 무시), `parentRule`, `parentStyleSheet`, 상수), `CSSStyleRule`(`selectorText` 읽기/쓰기: 올바르지 않으면 무시, 시트의 네임스페이스를 안다; `style` 은 **살아 있는** 선언 블록), `CSSGroupingRule`(`cssRules`, `insertRule`, `deleteRule`), `CSSConditionRule`(`conditionText`), `CSSMediaRule`(`media`), `CSSSupportsRule`, `CSSLayerBlockRule`(`name`), `CSSLayerStatementRule`(`nameList`), `CSSImportRule`(`href`(절대 URL), `media`, `styleSheet`, `layerName`), `CSSFontFaceRule`(`style`: 속성이기도 한 디스크립터만, 읽기 전용에 가까움), `CSSKeyframesRule`(`name` 쓰기, `cssRules`, `length`, `appendRule`, `deleteRule`, `findRule`), `CSSKeyframeRule`(`keyText` 읽기/쓰기, `style`), `CSSNamespaceRule`(`namespaceURI`, `prefix`). `@page`, `@counter-style` 등은 일반 `CSSRule`(`@page`=6, `@counter-style`=11, `@font-feature-values`=14, 그 밖은 0).
- `rule.style`(`CSSStyleDeclaration`)은 `js.dom.StyleObject` 를 일반화한 래퍼(규칙의 `CssStyleDeclaration` 을 제자리에서 편집)이며 `parentRule` 을 가진다. `rule.style = 'color: red'` 는 `cssText` 로 전달. 규칙의 선언 블록은 변경 리스너로 시트에 알리고 상대 `url()` 은 시트의 URL 을 기준으로 해석한다.
- `CSSRuleList` 는 살아 있는 읽기 전용 목록이며 같은 소유자에게 같은 객체이다. 지운 규칙은 시트에서 떨어져(`parentStyleSheet` null) 그 `style` 을 바꿔도 페이지가 바뀌지 않는다.

### 3. 생성 가능한 스타일 시트

- `new CSSStyleSheet({media, disabled, baseURL})`(`media` 는 문자열이나 `MediaList`, 잘못된 `baseURL` → `NotAllowedError`), `replaceSync(text)`, `replace(text)`(이미 이행된 Promise; 생성하지 않은 시트는 `NotAllowedError`), `@import` 는 무시한다. 생성한 시트는 `ownerNode`/`href`/`parentStyleSheet` 가 null.
- `document.adoptedStyleSheets`: 변경하면 바로 페이지에 알리는 보통 배열(`JsArray` 하위 클래스, `push/pop/splice/대입` 모두). 엔진이 문서의 시트 다음에 배열 순서로 적용한다(`CssEngine.setAdoptedStyleSheets`; 글꼴 규칙과 `@keyframes` 도 채택한 시트에서 읽는다). 대입은 `CSSStyleSheet` 가 아닌 값 → `TypeError`, 생성하지 않은 시트 → `NotAllowedError`(규격). 비활성 채택 시트는 적용되지 않는다.

### 4. `CSS` 네임스페이스

- `CSS.supports(property, value)`(속성 이름 대소문자 무시, 사용자 정의 속성은 항상 참, 빈 값/공백은 거짓, 이름의 공백은 처리하지 않음), `CSS.supports(conditionText)`(`@supports` 평가기 `SupportsCondition` 재사용, 먼저 그대로 평가하고 아니면 괄호로 감싸 평가), `CSS.escape(ident)`(`CssEscape.ident`). `[object CSS]`.

### 5. `document.fonts`, `FontFace`

- `FontFaceSet`(`size`, `status`="loaded", `ready`(이행된 Promise), `add/delete/has/clear`, `check(font, text)`, `load(font, text)`, `forEach`, `values/keys/entries/@@iterator`, `onloading` 등 핸들러 프로퍼티)와 `FontFace`(`family`, `style`, `weight`, `stretch`, `unicodeRange`, `variant`, `featureSettings`, `display`, `status`, `loaded`, `load()`). 집합은 스타일 시트의 `@font-face` 규칙(규칙에 연결된 `FontFace`)과 스크립트가 더한 글꼴 면을 나열한다.
- 스크립트가 더한 글꼴 면(문자열 `src`)은 **숨은 생성 시트의 `@font-face` 규칙**으로 바뀌어 페이지가 채택한다(`document.styleSheets`/`adoptedStyleSheets` 에는 보이지 않음). 그래서 스타일 시트의 글꼴과 같은 로더(페이지의 요청 계층, TrueType/WOFF, `WebFontIntegrationTest` 가 검증한 경로)가 읽고 도착하면 다시 배치한다. 규칙을 만들 수 없는 출처(`url()`/`local()` 이 아닌 문자열)는 `status` "error" 와 `SyntaxError` 로 reject 된 `loaded`.

### 6. `getComputedStyle(el, pseudo)`, `CSSStyleDeclaration` 점검

- `getComputedStyle` 의 두 번째 인수: `::before`, `::after`, `::first-line`, `::first-letter`, `::marker`, `::selection`, `::placeholder`, `::backdrop`, `::file-selector-button` 과 옛 한 콜론 네 가지. 규칙이 가리키지 않는 의사 요소는 요소에서 상속한 값과 초기값(`StyleResolver.getPseudoComputedStyle`). 없음/null/빈 문자열/콜론 없는 이름은 요소 자신, 올바르지 않은 콜론 문자열은 빈 선언.
- 점검에서 고친 것: 계산 스타일은 읽기 전용(`setProperty`/`removeProperty`/`cssText`/프로퍼티 대입 → `NoModificationAllowedError`; 기존 시험 `ElementExtrasTest.testComputedStyle` 의 기대를 이에 맞게 바꿨다), 계산 스타일의 `length`/`item`(값이 있는 개별 속성과 사용자 정의 속성), 사용자 정의 속성 이름의 대소문자 구분, `setProperty` 의 `null`/없는 `value`·`priority` 인수와 인수 1개 → `TypeError`, `setProperty` 가 속성을 그 자리에서 갱신하고 중요도가 바뀌면 이전 선언을 대체(이전 구현은 일반 선언 맵과 중요 선언 맵을 따로 둔 채 새 선언을 한쪽에만 넣어 `!important` 선언이 남을 수 있었다. 코드를 읽고 고쳤으며 수정 전 소스에서 시험을 돌려 보지는 않았다).

## 한계(정직하게)

- `CSSPageRule`(`selectorText`, `style`), `CSSPropertyRule`, `CSSContainerRule`, `CSSScopeRule`, `CSSCounterStyleRule` 의 전용 인터페이스는 없다(일반 `CSSRule`, 그룹이면 `CSSGroupingRule`). `CSSFontFaceRule.style` 의 변경은 규칙을 바꾸지 않는다. `CSSKeyframesRule`/`CSSKeyframeRule` 의 변경은 다음 애니메이션 시작부터 반영될 수 있다(실행 중인 애니메이션은 이전 키프레임으로 계속될 수 있음; 미확인).
- 생성 시트의 "생성한 문서" 검사 없음(다른 문서의 시트도 채택할 수 있다). `replace()` 는 동기로 처리하고 "disallow modification flag" 가 없다. 생성 시트의 `title` 은 null(규격은 빈 문자열, Chromium 은 null)이고 `href` 는 null(규격 문장은 문서 URL 이나 Chromium 은 null). 채택 배열에 잘못된 값을 `push` 하면 즉시 오류가 아니라 무시된다(대입은 규격대로 오류).
- 교차 오리진 시트: CORS 로 허용하는 경우(`crossorigin` 속성)는 없다 → 항상 읽을 수 없다. 읽기 거부는 `StyleSheet`/규칙 접근만 막고 `@import` 로 가져온 시트의 `href` 가 같은 오리진인지는 그 시트의 `href` 로 따로 판단한다.
- `FontFace.load()` 는 바이트를 기다리지 않고 곧바로 `loaded`(상태 확인 없음); 스타일 시트의 글꼴 면은 "loaded" 로 센다; `loading`/`loadingdone`/`loadingerror` 이벤트는 발생하지 않는다; `ArrayBuffer` 출처는 받아들이되 적용하지 않는다; `unicodeRange`/`variant`/`featureSettings` 는 규칙에 쓰기만 하고 검사하지 않는다; `check()` 의 `text` 인수를 쓰지 않는다.
- `getComputedStyle` 의 올바르지 않은 의사 요소 문자열: 규격 WD(2021)는 `TypeError` 이나 Chromium 은 던지지 않고 빈 선언을 준다 → Chromium 동작을 선택했다(기록 의무). 계산 스타일의 `cssText` 는 ""(규격과 Chromium 은 전체 직렬화).
- 요소 인라인 `style` 의 `setProperty` 는 `style` 속성을 다시 파싱해 쓰므로 선언 순서가 직렬화 규칙(일반 선언 먼저, 그 다음 `!important`)을 따른다. 규칙의 선언 블록은 제자리 갱신이다.
- 검증하지 못한 것: 실제 Swing 창(Computer Use)에서 눈으로 본 확인은 하지 않았다(화면 밖 `BufferedImage` 로 그린 화소만 확인). `WebFontIntegrationTest` 와 같은 실제 글꼴 파일로 스크립트 `FontFace` 가 화소를 바꾸는 시험은 만들지 않았다(규칙이 엔진에 닿는 것까지만 `CssomFontsTest` 가 확인).

## Chromium 과의 선택(기록)

- 위 두 가지(의사 요소 인수, 생성 시트의 `title`/`href`)는 Chromium 동작을 따랐다. 나머지는 CSSOM WD 2021-08-26 의 알고리즘을 따랐고, `cssRules` 읽기는 교차 오리진에서 `SecurityError`. Chromium 소스 파일은 조회하지 않았다.

## 변경 파일

- 모델(`src/org/duckdns/hjow/swingwebpanel/css`): `cssom/{CssRule, StyleSheet, StyleRule, GroupRule, ImportRule, KeyframesRule, FontFaceRule, UnknownRule, CssStyleDeclaration, StyleSheetBuilder, DocumentStyleSheets}`(수정), `cssom/{CssRuleList, MediaList, RuleEditor}`(신규), `media/MediaQueryList`(빈 목록 표시와 질의 문자열), `CssEngine`(시트 재사용·변경 통지·채택 시트), `cascade/StyleResolver`(`getPseudoComputedStyle`).
- 스크립트(`src/org/duckdns/hjow/swingwebpanel/js`): `dom/{CssomInterfaces, CssomMembers}`(신규), `dom/StyleObject`(규칙 선언 블록, 계산 스타일 읽기 전용), `host/{CssomBindings, CssNamespaceApi, CssFontApi}`(신규), `host/{ElementExtras, HostEnvironment, WindowGlobals}`(가산 수정: `getComputedStyle` 의사 요소와 선언 메서드 인수, `getCssEngine()` 훅, 설치 한 줄).
- 코어(`src/org/duckdns/hjow/swingwebpanel/core`): `PagePresentation`(`cssEngine()`, 엔진 변경 리스너), `PageRuntime`(`getCssEngine()`) — 가산 수정.
- 시험(`tests/src/org/duckdns/hjow/swingwebpanel`): `js/host/{CssomSheetsTest 8, CssomRulesTest 7, CssomConstructableTest 3, CssomNamespaceTest 4, CssomFontsTest 4, CssomDeclarationTest 4}`, `core/CssomIntegrationTest 4`(총 34건), `js/host/PageFixture`(`cssEngine`/`getCssEngine`), `js/host/ElementExtrasTest`(계산 스타일 읽기 전용 기대).

## 검증 결과

(아래 "시험 실행" 절에서 채운다.)
