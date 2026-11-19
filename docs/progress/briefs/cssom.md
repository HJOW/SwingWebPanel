# CSSOM 작업자 브리프 (DOM-13: 스크립트용 CSSOM — styleSheets, 규칙 API, CSS 네임스페이스)

## 작업 위치
새 git worktree(격리)에서 작업한다(`pwd` 로 확인). master 에서 갈라진 브랜치 `worktree-agent-<id>`.

## 목표
스크립트가 스타일시트와 규칙을 읽고 고칠 수 있게 한다(`docs/FEATURE_MATRIX.md` DOM-13 은 "미구현"이었고 `getComputedStyle`/`element.style` 만 구현됨, `docs/progress/api-surface.md` 참조). 자바 쪽 모델 `src/org/duckdns/hjow/swingwebpanel/css/cssom/*`(`StyleSheet`, `CssRule`, `StyleRule`, `GroupRule`, `ImportRule`, `FontFaceRule`, `KeyframesRule`, `NamespaceRule`, `CssStyleDeclaration`, `DocumentStyleSheets`, `StyleSheetBuilder`)는 이미 있고 스크립트 연결이 없다. 이 모델은 페이지의 CSS 엔진(`css/CssEngine`, `core/PagePresentation`)이 쓰는 것과 같은 객체여야 하며, 규칙을 고치면 스타일/레이아웃이 다시 계산되어야 한다(변경 통지는 `PagePresentation.sheetsChanged` / `CssEngine.refreshIfNeeded` 계열을 먼저 읽고 파악한다).

범위(우선순위 순서, 각각 끝낼 때마다 commit):
1. `document.styleSheets`(`StyleSheetList`, 문서 순서, `<style>`/`<link rel=stylesheet>`/`@import` 의 시트, 사용자 에이전트 시트는 제외), `StyleSheet`/`CSSStyleSheet` 의 `type`, `href`, `ownerNode`, `parentStyleSheet`, `title`, `media`(`MediaList`: `mediaText`, `length`, `item`, `appendMedium`, `deleteMedium`), `disabled`(바꾸면 스타일 재계산), `cssRules`/`rules`, `insertRule(rule, index)`(문법 오류 `SyntaxError`, 위치 `IndexSizeError`, `@import` 위치 `HierarchyRequestError`), `deleteRule`, `addRule/removeRule`(옛 API). `<style>` 요소의 `sheet`, `<link>` 의 `sheet`.
2. 규칙 객체: `CSSRule`(`type`, `cssText`, `parentRule`, `parentStyleSheet`), `CSSStyleRule`(`selectorText` 읽기/쓰기, `style` 은 **살아 있는** `CSSStyleDeclaration` — `element.style` 에 쓰인 `js.dom.StyleObject` 를 재사용·일반화), `CSSImportRule`(`href`, `media`, `styleSheet`), `CSSMediaRule`/`CSSSupportsRule`(`conditionText`/`media`, `cssRules`, `insertRule/deleteRule`), `CSSFontFaceRule`, `CSSKeyframesRule`(`name`, `cssRules`, `appendRule/deleteRule/findRule`)와 `CSSKeyframeRule`, `CSSNamespaceRule`. 지원하지 않는 규칙은 `CSSRule.type` 만 가진 일반 객체. 수정 뒤 `getComputedStyle`, 레이아웃, 화면이 갱신되는 통합 시험 포함.
3. 생성 가능한 스타일시트: `new CSSStyleSheet({media, disabled})`, `replace()`(Promise), `replaceSync()`, `document.adoptedStyleSheets`(배열 호환 객체; 문서에 적용되어야 함).
4. `CSS` 네임스페이스: `CSS.supports(property, value)` 와 `CSS.supports(conditionText)`(`@supports` 평가기 `SupportsCondition` 재사용), `CSS.escape(ident)`.
5. `document.fonts`(`FontFaceSet` 의 `ready`, `check`, `load`, `status`, `size`, `forEach`)와 `FontFace` 최소(`@font-face` 구현은 `css`/`render` 의 웹 글꼴 로더와 연결, 어려우면 `ready` 가 이행하는 최소 + 한계 기록).
6. `getComputedStyle(el, pseudo)` 의 의사 요소 인수와 `CSSStyleDeclaration` 의 `getPropertyValue/setProperty/removeProperty/getPropertyPriority/cssText/length/item` 가 규격대로인지 점검(이미 구현된 부분은 시험 보강만).

## 기준 규격과 조회
CSSOM(W3C CSS Object Model, `https://www.w3.org/TR/cssom-1/`)과 CSS Conditional 3 의 `CSS.supports`, CSS Font Loading 3. 조회는 `tools/webfetch`(`tools/webfetch/AGENTS.md`; HTTPS 는 JDK 6 으로 불가하므로 기본 `java`(JDK 25)로 실행). 판본/조회일을 `docs/progress/cssom.md` 에 적는다.

## 연결 방식(기존 코드 규칙)
JavaScript 연결은 `js/dom` 의 `DomBridge.defineInterface(이름, 부모, 자바 클래스, 멤버 명세)` 와 `MemberInstaller` 명세(`a:속성`, `a:속성=` 쓰기 가능, `m:메서드(타입,...)`, `r:` 반영), 분류기 `bridge.addClassifier`, 생성자 `bridge.setConstructor` 를 쓴다(`js/host/MediaQueries.java`, `ElementStates.java`, `js/dom/HtmlMoreInterfaces.java`, `TableBindings.java` 가 최근 예). 페이지 환경이 필요한 것(스타일 재계산 요청, 문서 큐)은 `js/host/HostEnvironment` 훅에 가산하고 `core/PageRuntime`/`PagePresentation` 이 구현한다(작은 가산 변경만; FRAMES 작업자가 `core/{PageRuntime,PagePresentation,HtmlPageDocument}` 를 동시에 고친다). 스크립트에 자바 객체/리플렉션을 노출하지 않는다.

## 소유 경로
`src/org/duckdns/hjow/swingwebpanel/js/dom/Cssom*.java`(새), `js/host/Cssom*`/`CssNamespace*`(새), `css/cssom/**`(모델 확장은 가산), 해당 시험(`tests/src/org/duckdns/hjow/swingwebpanel/js/dom/Cssom*Test`, `js/host/Cssom*Test`, `css/**`), `docs/progress/cssom.md`. `js/host/{HostEnvironment,WindowGlobals}`, `js/dom/DomInterfaces`, `core/**` 는 가산 변경만 하고 자주 commit.
커밋 번호 대역: `20261117-A` 형식은 쓸 수 없다 → **`20261117-501` 은 OPUS 가 썼으므로 `20261117-110` 부터**(3자리 숫자).

<공통 규칙은 별도 파일>
