# CSS-G 진행 기록 (P10-08: CSS3 모듈별 미구현 속성/선택자/규칙 구현과 검증)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/css/**`, `layout/**`, `render/**` 중 이 기능과 직접 관련된 파일, 해당 시험, 이 파일.
브랜치: 격리 worktree(`worktree-agent-a178810631d7b7f41`). 커밋 번호는 `20261117-801` 부터.
출발점: `docs/progress/layout-f.md` 의 "P10-08 CSS3 모듈별 점검" 표.

시험 실행(JDK 6u45): 이 worktree 는 다른 작업자와 격리되어 있으므로 `scripts/pkgtest.sh` 를 직접 쓴다.
`sh scripts/pkgtest.sh cg "css html dom layout render media net storage content core js download @root" "css layout render" --filter org.duckdns.hjow.swingwebpanel.render`
(처음 한 번 전체 컴파일에 몇 분 걸린다. 이후에는 `build/pkg-cg/{classes,test-classes}` 에 바뀐 파일만 javac 로 다시 컴파일해 `org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan ... --filter <이름>` 으로 실행했다.
`Prop.COUNT` 같은 상수가 바뀌면 그 상수를 쓰는 모든 클래스를 다시 컴파일해야 한다: `Prop.COUNT` 를 쓰는 파일 목록은 `grep -rl "Prop.COUNT" src tests`).
린트: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/layout src/org/duckdns/hjow/swingwebpanel/render src/org/duckdns/hjow/swingwebpanel/css tests/src/org/duckdns/hjow/swingwebpanel/layout tests/src/org/duckdns/hjow/swingwebpanel/render tests/src/org/duckdns/hjow/swingwebpanel/css`.

**최신(2026-11-18): P10-08 최종 대조와 CSS-J 구현(`border-image`, `font-synthesis`, `text-justify: inter-character`, `contain: style`)과 명시적 예외(EXC-09~13)는 이 파일 맨 아래 §30 에 있다. 아래 CSS-I/CSS-H 요약의 "남은 미구현 목록"은 §30.3 의 표로 대체되었다.**

## CSS-I 후속 작업 요약과 조정자 요청 (커밋 `20261117-880` ~ `20261117-89x`, 브랜치 `worktree-agent-a48b89d2ebc757f3a`)

이전 CSS-I 가 중단된 뒤 이어서 `@page`(§19), `backdrop-filter`(§20), 글꼴 기능(§21), `text-emphasis`(§22), 어두운 폼 컨트롤(§23), scroll 속성 시험과 논리 scroll-margin/padding(§24), 등록된 사용자 정의 속성의 전환/애니메이션(§25), Motion Path(§26), `hanging-punctuation`(§27), `mask-border`(§28), `initial-letter`(§29)를 구현했다. 미검증 WIP 브랜치 `wip/cssi-page-unverified` 는 `PageRule`/`MarginRule`/빌더/CSSOM 만 컴파일·시험을 거쳐 가져왔고(나머지는 모듈별로 다시 구현), 그 브랜치는 그대로 두었다(삭제는 조정자 몫).
**조정자 요청**
1. FEATURE_MATRIX 문구 제안(행 번호는 현재 표 기준): CSS-34(Filter Effects) "`backdrop-filter` 구현(Filter Effects 2 ED 2026-10-02: 쌓임 맥락/포함 블록, border box 와 둥근 모서리로 자름, 흐림의 mirror 가장자리; 시험 `BackdropFilterPaintTest` 8건). 한계: 변환된 요소에서 역변환 버퍼를 쓰지 않음, 요소 `opacity` 가 필터링한 배경에 곱해지지 않음, 애니메이션 보간 없음"; CSS-13(Fonts) "`font-variant` 단축 속성(여섯 개별 속성, 섞인 순서)과 `font-variant-caps/ligatures/numeric/east-asian/position/alternates`, `font-feature-settings` 의 규격 우선순위 해석(`FontFeatures`), 화면 효과는 작은/petite 대문자(`smcp c2sc pcap c2pc`), 위·아래첨자(`sups subs`), 전각(`fwid`), `font-synthesis` 허용 여부에 한정(시험 `FontFeaturesTest` 8건). 합자/숫자 모양/대체 형태/글리프 변형/커닝은 Java2D(JRE 6) 한계로 해석만 하고 효과 없음"; CSS-15(Text Decoration) "`text-emphasis-style/color/position` 과 `text-emphasis` 구현(Text Decoration 4 WD 2022-05-04: 모양 5종×채움 2, 문자열, 점이 붙는 글자 규칙, 줄 높이에 점의 공간, 글자 가운데에 그림; 시험 `TextEmphasisPaintTest` 8건). 한계: 세로쓰기 텍스트의 점은 그리지 않음"; CSS-14(Text) "`hanging-punctuation` 구현(`first` 의 여는 부호가 첫 줄 왼쪽 밖, `force-end`/`allow-end` 의 마침표·쉼표와 `last` 의 닫는 부호가 줄 맞춤에서 세어지지 않음; 시험 `HangingPunctuationLayoutTest` 4건). 한계: 정렬/양쪽 맞춤은 매달린 글자를 줄 폭에 포함해 계산(`force-end` 의 오른쪽 정렬 시 밖으로 밀리는 동작 없음), 세로쓰기와 rtl 은 처리하지 않음"; CSS-42(Pseudo-Elements) "`initial-letter` 구현(`::first-letter { initial-letter: size [sink|drop|raise] }` 를 size 줄 크기의 왼쪽 float 드롭 캡으로: 글꼴 크기=((size-1)*줄 높이+문단 대문자 높이)/대문자 비율, 상자 높이=sink 줄, 대문자 위쪽을 첫 줄에 정렬, sink 줄이 옆으로 흐름; 시험 `InitialLetterLayoutTest` 4건). 한계: 인라인 요소나 `::first-letter` 이외에는 적용하지 않음, 세로쓰기 없음, 대문자 높이는 H 글리프로 잼, 기준선 정렬은 줄 높이가 정해진 문단에서만 정밀"; CSS-35(Masking) "`mask-border` 구현(`mask-border-source/slice/width/outset/repeat/mode` 와 `mask` 처럼 `mask-border` 단축 속성: 이미지나 그라디언트를 아홉 조각으로 잘라 border 이미지 영역에 그리고 stretch/repeat/round/space, fill, 알파/휘도 모드, `mask-image` 위에 더해짐; 시험 `MaskBorderPaintTest` 7건). 한계: 같은 아홉 조각 방식인 `border-image` 는 여전히 미구현, 이미지가 늦게 도착하면 도착 뒤 다시 그림, `mask-border-outset` 으로 넓어진 영역 밖의 내용은 없으므로 그리기 범위는 넓히지 않음"; CSS-41(Color Adjustment) "어두운 구성표의 폼 컨트롤 그리기 구현(체크박스/라디오/화살표/스크롤 막대/진행 막대/미터/슬라이더/파일 단추/색 선택 가장자리; 시험 `ControlColorSchemePaintTest` 5건)"; CSS-40(Scroll Snap/Scrollbars) "계산 값·계단식·단축 속성·논리 `scroll-margin/padding-*`(block/inline)·상속 시험 `ScrollPropertiesTest` 8건. 스냅/스크롤바 그리기는 엔진이 스크롤을 소유하지 않아 구현하지 않음(계산 값만)"; CSS-07 또는 CSS-26/27(Transitions/Animations) "등록된 사용자 정의 속성(`@property`)의 형 있는 문법 값은 `transition` 과 `@keyframes` 로 보간(`var()` 로 쓰는 속성과 상속 자손 반영, 이벤트의 propertyName 은 `--x`; 시험 `RegisteredPropertyAnimationTest` 7건). 한계: `<transform-list>`/`<image>`/섞인 형은 이산"; CSS-44(Motion Path 등) "Motion Path Level 1 구현(`offset-path`: `path()`/`circle()`/`ellipse()`/`inset()`/`rect()`/`xywh()`/`polygon()`/`ray()`, `offset-distance`(닫힌 경로 나머지, 열린 경로 한정), `offset-position/anchor/rotate`, `offset` 단축 속성, 변환 순서 합성; 시험 `MotionPathLayoutTest` 8건, `MotionPathPaintTest` 2건). 한계: `url()` 도형 참조, `inset(... round)` 의 시작점 규칙, `coord-box` 선택은 근사. Speech/View Transitions 는 구현하지 않음(출력 매체가 없고 화면 효과가 별도 규격에 걸쳐 있음)"; Paged Media(CSS-44 아래 또는 새 행) "`@page`(쪽 선택자, 열여섯 여백 규칙, `size/page-orientation/marks/bleed/page`, CSSOM `CSSPageRule/CSSMarginRule`, 쪽 문맥 계단식과 계산된 쪽 상자 `PageStyles`; 시험 `PageRuleTest` 8건, `CssomModernRulesTest.testPageRule`). 한계: 인쇄가 없으므로 쪽 상자/여백 상자/쪽 나눔은 배치하지 않음".
2. AGENTS.md P10-08 은 **체크하지 않는다**. 남은 미구현 목록(모두 구현하지 못한 것이며 숨기지 않는다): `anchor-positioning`(`anchor-name`, `position-anchor`, `anchor()`, `position-try-*` 전부 미구현), `contain: style` 과 `content-visibility: auto` 의 화면 밖 생략, `hyphens: auto` 사전과 `text-justify: inter-character`, 세로쓰기 텍스트의 `text-emphasis`, 합자/숫자 모양 등 OpenType 효과(Java2D 한계), `font-synthesis-weight/style` 의 굵게/기울임 합성 연결, 스냅/스크롤바 효과(엔진이 스크롤을 소유하지 않음), `view-transition`(구현하지 않음: 요소 스냅샷 교차 전환은 화면 합성 계층과 문서 전환 모델이 필요하고 이 컴포넌트의 이동은 비동기 페이지 교체라 대응하는 시점이 없음), Speech(구현하지 않음: 출력 장치 없음), `border-image`(Backgrounds 3 의 미구현 부분).
3. `docs/ARCHITECTURE.md` 에 추가할 것: 그리기 컨텍스트(`PaintContext.surface`)가 그려지는 이미지를 알며 `backdrop-filter` 가 있는 목록만 `PagePainter` 가 읽을 수 있는 영역 이미지에 그린다; `StyleResolver.restyleWithCustom` 이 애니메이션 값을 가진 사용자 정의 속성으로 요소의 스타일을 다시 계산한다(애니메이션 엔진은 등록된 속성에 `CUSTOM_BASE + 순번` id 를 붙여 속성 코드를 재사용); `LayoutEngine.applyTransforms` 가 `MotionPath.has` 이면 오프셋 변환을 `transform` 과 합성한다; `css.cascade.PageStyles` 가 쪽 문맥의 계단식을 계산한다; `layout.FontFeatures` 가 글꼴 기능 태그를 해석한다.
4. 시험 결과는 이 파일 맨 아래 "시험 실행 결과"에 적었다(JDK 6u45).

## CSS-H 작업 요약과 조정자 요청 (커밋 `20261117-820` ~ `828`)

CSS-G 의 후속(CSS-H)이 §9 이후를 구현했다: 중첩(§9), `@container`·containment·`@scope`·스크롤/텍스트/글꼴 속성 파싱(§10~§12), `@property`(§13), `@counter-style`(§14), `text-overflow`/`line-clamp`(§15), 소프트 하이픈·`text-wrap: balance`(§16), 색 구성표·small-caps·font-size-adjust(§17), 컨테이너 상대 길이·`line-break: anywhere`·`text-justify: none`(§18).
**조정자 요청**
1. FEATURE_MATRIX 문구 제안: CSS-05(Cascade) "`@layer` 순서(Level 5), `@scope`(Level 6: 범위 루트·한계·근접도), `@property` 등록 속성(Properties and Values API 1: 문법 검사, 상속, 정규 계산 값), CSS Nesting 1(중첩 규칙, `&`, 앞 결합자, 조건 그룹 중첩) — 시험 `CascadeLayersTest` `CssNestingTest` `CssScopeTest` `RegisteredPropertyTest`. 한계: 등록된 속성의 전환/애니메이션 없음, `@scope` 의 전문 없는 루트는 문서 루트, 중첩의 CSSOM 은 평평한 규칙 목록"; CSS-30(Conditional) "`@container` 크기 질의/`style()` 질의와 컨테이너 상대 길이(`cq*`), `@supports font-format()/at-rule()`"; CSS-23(Lists/Counter Styles) "`@counter-style`(6 시스템과 서술자), `symbols()`, 문자열 표식, 미리 정의된 스타일 확대(`CounterStylesTest`)"; CSS-38(Containment) "`contain`(size/inline-size/layout/paint/strict/content), `content-visibility: hidden`, `container-type`, `contain-intrinsic-*` — 부분 구현(style 격리 없음, auto 의 화면 밖 생략 없음)"; CSS-40(Scroll Snap/Scrollbars) "계산 값만(엔진이 스크롤바와 스냅을 그리지 않음)"; CSS-14(Text) "`hyphens`(소프트 하이픈), `text-wrap-mode/style: balance`, `line-break: anywhere`, `text-justify: none`, `line-clamp` 구현. 자동 하이픈 사전과 `text-justify: inter-character` 없음"; CSS-32(UI) "`text-overflow`(줄임표/문자열)·`line-clamp` 구현(이전에는 파싱만이었는데 효과 있음으로 표시되어 있었음)"; Fonts(CSS-13/16) "`font-variant: small-caps`, `font-size-adjust` 구현, 나머지 `font-variant-*`/`font-feature-settings` 는 계산 값만"; Color Adjust(`color-scheme`, `light-dark()`, 시스템 색의 어두운 값, 어두운 캔버스)는 폼 컨트롤 제외 구현.
2. AGENTS.md P10-08 은 **체크하지 않는다**. 남은 미구현/한계: 등록된 사용자 정의 속성의 전환·애니메이션(`AnimationEngine` 연동 필요), `contain: style` 과 `content-visibility: auto` 의 생략, `scrollbar-*`·`scroll-snap-*` 의 효과(엔진에 스크롤바가 없음), `text-emphasis`/`hanging-punctuation`/`initial-letter`/`hyphens: auto` 사전, `font-variant` 단축 속성 형태와 OpenType 기능, 어두운 구성표의 폼 컨트롤 그리기, `@starting-style`, `@page`, `backdrop-filter`, `mask-border`, `view-transition`, Motion Path, `anchor-positioning` 등 CSS-G 표의 나머지.
3. `docs/ARCHITECTURE.md` 에 추가할 것: 레이아웃은 `ContainerSource` 를 구현한 스타일 공급자(`CssStyleSource`)에게 컨테이너 크기를 알리고 `usesContainerQueries()` 이면 최대 4 번 다시 배치한다(`LayoutEngine.layoutWithContainers`); `CounterStyleSource` 로 문서의 `@counter-style` 을 얻는다; `LengthContext` 에 색 구성표와 컨테이너 크기가 실린다.
4. 시험 실행 시 알린 점: 전체 시험 중 `js.host.web.XhrTest.testTimeout` 은 CPU 경합 중에 한 번 실패했고 단독으로는 통과한다(시간 민감).

## 조회한 규격 (CSS-H, 도구 `tools/webfetch`, 실행 JRE: Zulu 25, 조회일 2026-11-17)

| 규격 | URL | 판본(본문 첫 줄) |
| --- | --- | --- |
| CSS Nesting Module | https://www.w3.org/TR/css-nesting-1/ | W3C Working Draft, 22 January 2026 |
| CSS Conditional Rules Module Level 5 | https://www.w3.org/TR/css-conditional-5/ | W3C Working Draft, 30 October 2025 |
| CSS Cascading and Inheritance Level 6 | https://www.w3.org/TR/css-cascade-6/ | W3C Working Draft, 6 September 2024 |
| CSS Counter Styles Level 3 | https://www.w3.org/TR/css-counter-styles-3/ | W3C Candidate Recommendation Snapshot, 27 July 2021 |
| CSS Properties and Values API Level 1 | https://www.w3.org/TR/css-properties-values-api-1/ | W3C Working Draft, 26 March 2024 |
| CSS Containment Module Level 3 / Level 2 | https://www.w3.org/TR/css-contain-3/, css-contain-2/ | WD 18 August 2022 / WD 17 September 2022 |
| CSS Overflow Module Level 4 | https://www.w3.org/TR/css-overflow-4/ | W3C Working Draft, 21 March 2023 |
| CSS Color Adjustment Module Level 1 | https://www.w3.org/TR/css-color-adjust-1/ | W3C Candidate Recommendation Snapshot, 16 December 2025 |
| CSS Text 3 / Text 4, Fonts 4, Color 5, UI 3 | https://www.w3.org/TR/css-text-3/, css-text-4/, css-fonts-4/, css-color-5/, css-ui-3/ | 조회함(본문 첫 줄에 날짜가 없는 판은 기록하지 못함) |

규격 본문을 줄 단위로 대조한 것은 컨테이너 선택(Conditional 5 §5), 컨테이너 상대 길이(§4.4 끝), 범위 근접도(Cascade 6 §2), 중첩의 `&` 와 `@scope`(Nesting §3.3: `@scope` 안에서 `&` 는 `:where(:scope)` 이다 → 구현을 고쳤음), `:is()` 구체성뿐이다. 나머지 알고리즘은 규격 지식에 따랐고 본문 전체와 줄 단위로 대조하지는 않았다(미확인).

## 조회한 규격 (CSS-G, 도구 `tools/webfetch`, 실행 JRE: Zulu 25, 조회일 2026-11-17)

| 규격 | URL | 판본 |
| --- | --- | --- |
| Filter Effects Module Level 1 | https://www.w3.org/TR/filter-effects-1/ | W3C Working Draft 2018-12-18 (페이지의 Last-Modified: 2018-12-14) |
| Compositing and Blending Level 1 | https://www.w3.org/TR/compositing-1/ | W3C Candidate Recommendation Draft 2024-03-21 |

## 1. Filter Effects 1 (`filter`)

| 속성/기능 | 파싱 | 계산 | 배치/그리기 | 시험 |
| --- | --- | --- | --- | --- |
| `filter`(`blur()` `brightness()` `contrast()` `drop-shadow()` `grayscale()` `hue-rotate()` `invert()` `opacity()` `saturate()` `sepia()`) | `PropertyTable` 의 `filter-function` 문법(인수 생략, 음수 거부, 백분율, 각도 단위), 알 수 없는 함수는 선언 무효 | 길이는 px 로 계산(기존 `StyleComputer.map`), `layout.FilterSpec.of` 가 양을 한정(grayscale/invert/opacity/sepia 최대 1) | `PaintWalker` → `Visitor.beginEffects/endEffects` → `render.EffectGroup` → `PagePainter.paintEffectGroup`(버퍼에 그린 뒤 `FilterRenderer.apply`) | `FilterPaintTest` 10건 |
| 쌓임 맥락과 포함 블록 | — | — | `BoxBuilder.initPosition`(쌓임 맥락), `fillPositioned`/표(절대/fixed 자손의 포함 블록: `FilterSpec.has`) | `testStackingContextAndContainingBlock` |
| 히트 테스트 | — | — | 영향 없음(`PaintSequence.beginEffects` 는 무시. 규격 5.1 절) | `testBlur`, `testDropShadow` |
| 그려지는 범위 | — | — | `FilterSpec.extent()`(blur 는 3σ+2 px, drop-shadow 는 오프셋 포함)만큼 `DisplayListBuilder.subtreeBounds` 와 효과 그룹 영역을 넓힘, 보이는 영역 밖에서 번져 오는 내용도 그림(`paintEffectGroup` 의 `vis`) | `testBlur`, `testHiddenAndOverflow` |

구현 요점: 색 함수는 sRGB 의 미리 곱하지 않은 색에서 규격 13.1 절의 행렬/전달 함수로 계산하고 결과를 0~1 로 한정한다(`FilterRenderer.colorFunction`). 흐림은 σ 가 기기 픽셀 2 미만이면 분리 가능한 참 가우스 합성곱, 2 이상이면 규격 `feGaussianBlur` 의 상자 흐림 세 번(d 가 홀수/짝수 경우의 상자 크기 포함)이다. `drop-shadow()` 는 입력 알파를 흐리고 옮겨 그림자 색을 입혀 이미지 아래에 합성한다. 시험 값: 빨강에 `grayscale(1)` → 0x363636, `invert(1)` → 0x00FFFF, `hue-rotate(120deg)` → 0x007100 등 규격 행렬 값과 일치.
한계: `url()`(SVG 필터 참조)가 든 `filter` 는 존재하지 않는 참조로 보고 사슬 전체를 무시한다(규격 5.1 절의 동작). `backdrop-filter` 는 구현하지 않았다(미지원 이름으로 남김). `filter` 의 애니메이션 보간은 구현하지 않았다. `drop-shadow` 오프셋은 기기 픽셀로 반올림한다(소수 오프셋은 정확하지 않다). 회전/기울임 변환 아래에서 흐림은 x/y 배율을 따로 곱한다(등방성 가정).

## 2. Compositing 1 (`mix-blend-mode`, `isolation`, `background-blend-mode`)

| 속성 | 파싱 | 계산/배치 | 그리기 | 시험 |
| --- | --- | --- | --- | --- |
| `mix-blend-mode`(17 모드: normal, multiply, screen, overlay, darken, lighten, color-dodge, color-burn, hard-light, soft-light, difference, exclusion, hue, saturation, color, luminosity, plus-lighter) | `PropertyTable` 의 `blend-mode` 문법(알 수 없는 값은 선언 무효) | 혼합 모드가 있는 상자는 쌓임 맥락(`BoxBuilder.initPosition`) | `PaintWalker.context` 가 불투명도 그룹(`beginOpacity`)을 열고 `DisplayListBuilder` 가 그룹에 혼합 모드를 실음 → `PagePainter.paintOpacityGroup` 이 `BlendModes.composite` 로 그룹 버퍼를 그림 | `BlendModeTest`(`testMixBlendMode`, `testBlendWithOpacity`, `testStackingContextAndHit`) |
| `isolation` | 문법 `auto \| isolate` | `isolate` 는 쌓임 맥락 | 쌓임 맥락마다 격리: 안에 혼합하는 상자가 있는 비루트 쌓임 맥락은 자신의 버퍼 그룹으로 그림(`PaintWalker.markIsolation`) | `testIsolation`(isolate, z-index:0, opacity, 쌓임 맥락이 아닌 부모) |
| `background-blend-mode` | 새 속성(`Prop.BACKGROUND_BLEND_MODE`=182, 목록, 레이어 수만큼 되풀이) | — | 혼합하는 레이어가 있으면 `render.BlendedBackground`: 배경색과 레이어를 자신의 버퍼에 아래부터 자기 모드로 합성 | `testBackgroundBlendMode`(레이어 둘, 목록 되풀이) |

혼합 식: 규격 9~10 절의 식을 `BlendModes.blend` 로 구현(분리 가능 11 모드, 분리 불가능 4 모드(Lum/ClipColor/SetLum/Sat/SetSat), `plus-lighter` 는 미리 곱한 합의 한정). 알파는 `Cr = (1 - as/ar)·Cb + as/ar·((1 - ab)·Cs + ab·B(Cb, Cs))`. 손으로 계산한 값으로 시험(`testSeparableFormulas`, `testNonSeparableFormulas`, `testAlpha`).
한계: 혼합은 `Composite` 로 픽셀마다 `ColorModel.getRGB` 를 쓰므로 큰 영역에서는 느리다(소프트웨어 합성). 루트 요소의 `mix-blend-mode` 와 캔버스(루트/body 배경)의 `background-blend-mode` 는 구현하지 않았다(루트는 그룹이 아니고 캔버스 배경은 혼합 없이 그린다). `plus-lighter` 는 Compositing 2 의 의미를 근사한다. `background-blend-mode` 는 `background-attachment:fixed/local`, `background-clip:text` 를 보통 배경처럼(기존 한계) 다룬다.

## 3. Masking 1 (`mask-*`, `mask`, `clip-path` 의 나머지 도형, `clip-rule`)

규격: https://www.w3.org/TR/css-masking-1/ (W3C Candidate Recommendation Draft 2021-08-05, 조회 2026-11-17).

| 속성 | 파싱 | 그리기 | 시험 |
| --- | --- | --- | --- |
| `mask-image`(`none`, 이미지, 그라디언트; 쉼표 목록) | `PropertyTable`(`Prop.MASK_IMAGE`=183) | `layout.Effects.hasMask` → 쌓임 맥락 + 효과 그룹, `render.MaskLayers`(레이어 타일은 `BackgroundLayers.build` 를 마스크 속성 id 로 재사용, 레이어마다 알파/휘도 마스크 값, 아래부터 `mask-composite` 로 합성, 결과를 내용 알파에 곱함) | `MaskPaintTest` 8건 |
| `mask-mode`(alpha, luminance, match-source=alpha) | `Prop.MASK_MODE` | 휘도 = 0.2125 R + 0.7154 G + 0.0721 B(미리 곱하지 않은 색) × 알파 | `testMaskMode` |
| `mask-size/position/repeat`(repeat, no-repeat, space, round, 1~4 값 위치) | `Prop.MASK_SIZE/POSITION/REPEAT` | 배경 레이어와 같은 코드 | `testSizeRepeatPosition` |
| `mask-origin`(기본 border-box), `mask-clip`(기본 border-box, `no-clip`) | `Prop.MASK_ORIGIN/CLIP` | `BackgroundLayers.edge`: fill-box=content, stroke/view-box=border, margin-box≈border(근사) | `testOriginAndClip` |
| `mask-composite`(add/subtract/intersect/exclude) | `Prop.MASK_COMPOSITE` | Porter-Duff source over/out/in/xor, 맨 아래 레이어의 연산자는 무시, `none`/없는 레이어=투명한 검정 | `testComposite` |
| `mask` 단축 속성 | `Shorthands.Mask`(층 안의 부분을 순서 없이, 상자 하나=origin+clip, 둘=origin,clip, `no-clip`, 쓰지 않은 부분 초기화) | — | `testShorthand` |
| `-webkit-mask`, `-webkit-mask-image/size/position/repeat/origin/clip` | `PropertyRegistry` 별칭 | — | — |
| `clip-path: path()/rect()/xywh()` | `ClipPaths`(`path()` 는 `media.image.svg.PathShapes` 로 SVG 경로 데이터를 읽고 기준 상자의 왼쪽 위에 놓음, `nonzero`/`evenodd`) | `PaintWalker`/`DisplayListBuilder` 기존 clip-path 경로 | `ClipPathShapesTest` 4건 |
| `clip-path: inset(... round ...)`의 모서리별/축별 반지름 | `ClipPaths.withRadii`(`border-radius` 와 같은 1~4 값, `/` 세로 반지름, 겹치면 비례 축소) | 타원 모서리 사각형 | `testRadii` |
| `clip-path: <기준 상자>` 만 | `ClipPaths.shapeOf`(상자 가장자리) | — | `testBoxOnly` |
| `clip-rule` | 속성 표(`Prop.CLIP_RULE`, 파싱만) | HTML 요소에는 효과 없음(SVG `clipPath` 안의 도형에만 의미가 있다) | — |

한계: `mask-border*` 와 SVG `mask` 요소를 가리키는 `url(#id)` 참조는 구현하지 않았다(미지원 이름/값으로 남김). 이미지가 아직 도착하지 않았거나 실패한 마스크 레이어는 규격대로 투명한 검정이어서 상자가 그려지지 않는다(이미지 도착 뒤 다시 그린다). 마스크는 히트 테스트에 영향이 없다. `path()` 의 경로 데이터가 틀리면 SVG 파서가 읽을 수 있는 앞부분까지 쓰고, 그리는 구간이 하나도 없을 때만 무효로 본다(규격은 경로 전체가 올바르지 않으면 무효). `margin-box` 는 `border-box` 로 근사. `mask-type` 은 파싱하지 않는다.

## 4. CSS Logical Properties and Values Level 1

규격: https://www.w3.org/TR/css-logical-1/ (조회 2026-11-17; 매핑 규칙은 2 절). 구현: `css.props.LogicalProps`(매핑), `StyleResolver.compute`(계단식 단계에서 옮겨 씀), `StyleComputer.modes`/`VarResolver.resolvePending`(var()), `ComputedStyle`(논리 이름 질의), `Shorthands`(단축 속성).

| 속성 | id 범위 | 매핑 | 시험 |
| --- | --- | --- | --- |
| `margin-block-start/end`, `margin-inline-start/end`, `padding-*`, `inset-*`, `border-*-width/style/color`(block/inline start/end) | `Prop.MARGIN_BLOCK_START`(192) ~ `BORDER_INLINE_END_COLOR`(215) | 요소의 `writing-mode` 와 `direction` 에 따라 물리 변(위/오른쪽/아래/왼쪽)으로: horizontal-tb 는 block-start=위, inline-start=왼쪽(rtl 이면 오른쪽), vertical-rl/sideways-rl 은 block-start=오른쪽, vertical-lr/sideways-lr 은 block-start=왼쪽, inline-start=위쪽(sideways-lr 은 아래쪽, rtl 은 반대) | `testMappingTable`(10 모드×4 변), `testHorizontalBoxes`, `testVerticalModes`, `testInset` |
| `border-start-start-radius`, `border-start-end-radius`, `border-end-start-radius`, `border-end-end-radius` | 216~219 | 블록 변과 인라인 변의 교차 모서리 | `testRadiiAndComputedValues` |
| `inline-size`, `block-size`, `min-/max-inline-size`, `min-/max-block-size` | 220~225 | 가로쓰기 inline=width/block=height, 세로쓰기는 반대 | `testVerticalModes` |
| 단축 속성 `margin-block/inline`, `padding-block/inline`, `inset-block/inline`, `border-block/inline-width/style/color`, `border-block/inline-start/end`, `border-block`, `border-inline` | `Shorthands`(`Pair`, `AnyOrder` 재사용) | 값 1~2 개(start, end) | `testHorizontalBoxes`, `testCascadeOrder`, `PropertyTest` |

계단식: 논리 선언은 `StyleResolver.compute` 가 이기는 `writing-mode`/`direction` 선언을 먼저 찾아(`StyleComputer.modes`: 명시값, `initial`, 선언 없음/상속/`var()` 는 부모 값) 물리 개별 속성에 쓰므로 같은 변의 논리/물리 선언은 계단식 순서(출처, 구체성, 소스 순서, `!important`, `style` 속성)대로 겨루고 나중 것이 이긴다. 레이아웃 코드는 물리 속성만 안다. `var()` 값은 `VarResolver.resolvePending` 이 선언한 논리 id 를 같은 매핑으로 찾는다. 논리 이름의 `getComputedStyle().getPropertyValue()` 와 `ComputedStyle.get(name)` 은 그 요소의 매핑으로 물리 값을 돌려준다. `getComputedStyle` 의 직렬화와 단축 속성의 직렬화(`Pair`)도 같다.
한계: `inherit` 는 부모의 논리 값이 아니라 같은 물리 변의 값을 물려받는다(부모와 자식의 쓰기 모드가 다르면 규격과 다르다). 같은 규칙 안에서 같은 논리 속성을 두 번 선언하면 선언 맵의 삽입 순서 때문에 물리 선언과의 순서가 첫 위치 기준이 된다(`margin-inline-start:1px; margin-left:2px; margin-inline-start:3px` 의 결과는 `margin-left` 가 이긴다). 이것은 기존 `CssStyleDeclaration` 의 중복 선언 처리(같은 이름은 첫 위치 유지)에서 오며, 같은 이름만 중복되는 보통 경우에는 영향이 없다. `border-image-*`, `overflow-block/inline`, `scroll-margin/padding-*` 의 논리 이름, `text-align: start/end` 이외의 논리 값(`float: inline-start` 는 기존 구현), `contain-intrinsic-*` 는 다루지 않았다. rtl 컨테이너 안에서 over-constrained 블록을 오른쪽에 붙이는 CSS 2.1 10.3.3 의 규칙은 기존 레이아웃 엔진에 없다(논리 속성과 무관한 기존 한계; 논리 margin 의 계산 값만 시험했다).

## 5. CSS Fragmentation Module Level 3 (`break-before`, `break-after`, `break-inside`, `box-decoration-break`)

규격: https://www.w3.org/TR/css-break-3/ (조회 2026-11-17, 3 절 "Controlling Breaks"). 구현: `PropertyTable`(`Prop.BREAK_BEFORE/AFTER/INSIDE`=226~228, `BOX_DECORATION_BREAK`=229(파싱만)), `PropertyRegistry` 별칭(`page-break-before/after/inside`, `-webkit-column-break-before/after/inside`, `-webkit-box-decoration-break`), `MulticolLayouter.collect`(끊을 수 있는 자리의 종류 0 허용/1 피함/2 강제)와 `fill`(강제 나눔을 먼저 처리).

| 속성 | 동작(다단 컨테이너 안) | 시험 |
| --- | --- | --- |
| `break-before`/`break-after`: `always`, `all`, `column`, `page`, `left`, `right`, `recto`, `verso` | 두 형제 사이의 강제 열 나눔(`page` 계열도 열을 나눈다: 모든 분할 문맥을 가로지름). 컨테이너 맨 앞/맨 뒤에서는 나누지 않음. 첫/마지막 정상 흐름 자식의 값은 부모에게 전파(깊이 8) | `FragmentationBreakTest`(`testBreakBeforeColumn`, `testBreakAfterAndSeveral`, `testStartValuesAndAliases`, `testPropagation`) |
| `break-before`/`break-after`: `avoid`, `avoid-column` | 그 자리에서 나누는 것을 피함(다른 자리에서 나눌 수 있으면 거기서, 피할 곳뿐이면 어쩔 수 없이 거기서) | `testAvoidBetweenBoxes` |
| `break-inside: avoid`, `avoid-column` | 그 상자 안(후손 포함)의 끊을 수 있는 자리를 모두 "피함"으로 표시 → 상자가 다음 열에 들어가면 통째로 옮김, 열보다 크면 어쩔 수 없이 나뉨 | `testBreakInsideAvoid` |
| `avoid-page`, `avoid-region`, `region` | 열 나눔에는 영향 없음(파싱은 함) | `testStartValuesAndAliases`, `testParsing` |
| 예전 이름 `page-break-*`, `-webkit-column-break-*` | break-* 의 별칭 | `testStartValuesAndAliases` |
| `box-decoration-break` | **파싱만**(`slice`, `clone`). 줄/열에서 나뉜 상자의 조각이 border/padding 를 어떻게 그릴지는 구현하지 않았다. 인라인 상자에서는 조각 너비를 줄바꿈 계산이 알아야 하고 블록 조각은 높이가 바뀌므로 다음 작업으로 남긴다 | `testParsing` |

기존 `orphans`/`widows` 처리(줄 사이 끊기)는 그대로이며 forced break 는 그 제한을 넘는다. 한계: 쪽 나눔(paged media)은 없으므로 `page`/`left`/`right`/`recto`/`verso` 는 열 나눔으로만 동작하고 `avoid-page` 는 무시한다. 플렉스/그리드 항목 사이와 표 행 사이의 나눔 값은 해석하지 않는다(`MulticolLayouter.breakable` 이 나누지 않는 상자).

## 6. CSS Shapes Module Level 1 (`shape-outside`, `shape-margin`, `shape-image-threshold`)

규격: https://www.w3.org/TR/css-shapes-1/ (W3C Candidate Recommendation Draft 2025-06-12, 조회 2026-11-17). 구현: `PropertyTable`(`Prop.SHAPE_OUTSIDE`=230, `SHAPE_MARGIN`=231, `SHAPE_IMAGE_THRESHOLD`=232(파싱만)), `layout.FloatShapes`(float 영역 = 기본 도형을 기준 상자(기본 margin-box) 기준으로 만들어 `shape-margin` 만큼 넓히고 margin box 로 자름), `ClipPaths.shapeFor`(clip-path 와 같은 도형 읽기 재사용), `FloatContext`(`Item.area`, `leftEdgeShaped`/`rightEdgeShaped`), `InlineLayouter`(`refreshBounds` 가 줄의 너비를 구할 때 모양을 쓰고 `placeLaidOut` 이 float 영역을 문맥에 등록).

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| `circle()`, `ellipse()`, `polygon()`, `inset()`(`round` 포함), `rect()`, `xywh()`, `path()` | float 옆의 줄이 띠(줄 높이)마다 float 영역이 닿는 가장 오른쪽(오른쪽 float 는 가장 왼쪽) 점까지 비켜 선다. 영역이 띠에 닿지 않으면 줄은 줄어들지 않는다 | `ShapeOutsideLayoutTest.testCircle`, `testOtherShapes`, `testRightFloat` |
| 기준 상자(`margin-box`(기본), `border-box`, `padding-box`, `content-box`) | 상자만 쓰면 그 상자의 사각형 | `testOtherShapes` |
| `shape-margin`(길이, 백분율=포함 블록 너비) | 영역을 바깥으로 넓힘(굵은 둥근 윤곽선을 합침). 결과는 float 의 margin box 에서 잘림 | `testShapeMargin` |
| 쓸 수 없는 값(`none`, `url()`, 그라디언트, 틀린 도형) | margin box 를 감쌈 | `testUnusableValues` |
| 블록 서식 문맥을 만드는 상자와 다른 float | margin box 를 씀(규격: 모양은 인라인 내용에만 영향) | `testBfcBoxUsesMarginBox` |

한계: 이미지 모양(`url()`, 그라디언트, `shape-image-threshold`)은 구현하지 않았다. 줄의 띠는 기존 엔진이 쓰는 근사(`strutH`: 빈 줄의 높이)이다 — 글꼴이 큰 줄은 실제 줄 높이보다 좁은 띠로 판정한다. `border-box` 로 지정한 모양에 `border-radius` 의 둥근 모서리를 반영하지 않는다. 줄이 들어갈 자리가 없으면 기존처럼 float 의 아래쪽 가장자리로 내려간다(모양 안에서 필요한 만큼만 내려가지 않음).

## 7. Pseudo-Elements 4 / Basic UI 4 (`::placeholder`, `::selection`, `caret-color`, `accent-color`)

| 속성 | 구현 | 시험 |
| --- | --- | --- |
| `::placeholder` | 이미 `StyleResolver.getPseudoStyle(e, "placeholder")` 가 계산하던 스타일을 `BoxBuilder` 가 `input`/`textarea` 의 `LayoutBox.placeholderStyle` 로 보관하고 `ControlPainter.snapshot` 이 그 `color` 를 `ControlSnapshot.placeholderColor` 로 옮겨 텍스트 필드와 텍스트 영역의 placeholder 를 그 색(알파 포함)으로 그린다. 기본값은 UA 스타일시트 `::placeholder { color: #757575 }`(이전 상수와 같은 색) | `PseudoUiPaintTest.testPlaceholderColor` |
| `accent-color`(`P` 플래그 해제) | `ControlSnapshot.accent`(0 이면 auto) → 체크박스 칠함/테두리, 라디오 점, `progress` 채움, `range` 슬라이더가 `ControlPainter.accentOf` 의 색을 쓴다. 상속 속성 | `testAccentColor` |
| `caret-color` | 계산 값만(`auto`/색, `currentcolor` 는 `color`). 캐럿을 그리는 일은 텍스트 선택/캐럿 작업(P11-08, TEXTSEL)의 몫이다 | `testComputedValues` |
| `::selection` | 계산 스타일만(`getPseudoStyle(e, "selection")`: `color`, `background-color`). 선택 영역을 그리는 일은 P11-08 의 몫이다 | `testComputedValues` |

한계: `::placeholder` 는 색만 반영한다(글꼴/`font-style`/`opacity` 등은 입력 컨트롤의 글꼴을 따른다). `::file-selector-button`, `::backdrop` 은 계산하지 않는다. `accent-color` 는 `appearance` 와 상관없이 단순 색 대체이며 대비 색 자동 선택(체크 표시 색)은 하지 않는다.

## 8. Cascade 5 / Conditional (`@layer`, `@supports selector()`; `@container`, `@property`, nesting, `@scope`는 아래 "남은 항목")

규격: https://www.w3.org/TR/css-cascade-5/ 6 절 (규칙 알고리즘은 기능 표의 판본과 규격 지식에 따랐고 이번 세션에는 조회하지 않았다 → 후속 확인).

| 기능 | 구현 | 시험 |
| --- | --- | --- |
| `@layer name { ... }`, `@layer a, b;`, 익명 `@layer { ... }`, 중첩/점 이름(`a.b`) | `StyleSheetBuilder`(블록 없는 `@layer` 는 `GroupRule.layerStatement`), `RuleIndex.addLayer/registerLayer/layerRank`(처음 나온 순서, 하위 레이어는 레이어 자신의 규칙보다 앞), `Entry.layer` | `CascadeLayersTest.testLayerOrder`, `testNestedAndAnonymous` |
| 레이어 없는 규칙이 이긴다(구체성 무관), `style` 속성도 레이어 없음 | `StyleResolver.App.layer`(비교 순서: 층(origin/중요도), **레이어**, 구체성, 소스 순서) | `testUnlayeredBeatsLayered` |
| `!important` 의 레이어 순서 뒤집기(앞선 레이어가 이기고 레이어 없는 중요 선언이 가장 약함) | 중요 선언의 레이어 키 = `MAX_VALUE - 순위` | `testImportantIsReversed` |
| `@import url(x) layer(name)`, `layer`(익명) | `ImportRule.layer`, `RuleIndex.addRules` 가 가져온 시트를 그 레이어에 넣음 | `testImportIntoLayer` |
| `@supports selector(...)`, `not`/`and`/`or` | 이미 있던 `SupportsCondition`(파싱되는 선택자면 참)을 시험으로 확인 | `testSupportsSelector` |

한계: `revert-layer` 는 `revert` 처럼(이전 층의 값으로) 동작한다(레이어 단위가 아님). 레이어 안의 `@media`/`@supports` 는 지원한다. CSSOM 의 `CSSLayerBlockRule`/`CSSLayerStatementRule` 객체 모델과 `getComputedStyle` 외 JS API 는 연결하지 않았다.

## 9. CSS Nesting Module (CSS-H, 커밋 번호 820 대역)

규격: https://www.w3.org/TR/css-nesting-1/ (조회 2026-11-17은 아래 "조회한 규격(CSS-H)" 표). 구현: `css.syntax.CssParser.parseBlockContents`(선언, 중첩 한정 규칙, 중첩 at-규칙을 소스 순서로 읽음; 최상위 `{}` 블록이 다른 값 옆에 있으면 선언이 아니라 규칙이고 `--x: {}` 는 사용자 정의 속성 선언), `css.cssom.StyleSheetBuilder`(`buildStyleRules`/`buildContents`/`nestPrelude`/`substitute`/`selfReference`/`children`).

| 기능 | 구현 | 시험 |
| --- | --- | --- |
| `&`(중첩 선택자) | 선택자 값에서 `&` 를 `:is(부모 선택자)` 로 바꾼 뒤 기존 선택자 파서로 파싱(함수 인수 안의 `&` 도 바뀜). 시트 최상위의 `&` 는 `:scope`. 부모의 구체성은 `:is()` 규칙(가장 높은 항목)으로 자동 | `CssNestingTest.testBasicNesting`, `testSpecificityOfTheParent`, `testRecovery` |
| `&` 가 없는 중첩 선택자 | 앞에 `:is(부모) ` 를 붙여 자손으로 만듦 | `testBasicNesting` |
| 앞 결합자(`> .b`, `+ .b`, `~ .b`) | `:is(부모) > .b` 등 | `testRelativeSelectors` |
| 쉼표 목록(부모와 중첩 양쪽) | 중첩 선택자는 쉼표마다 따로 해석, 부모는 `:is(a, b)` | `testSelectorLists` |
| 선언과 중첩 규칙의 순서 | 중첩 규칙 앞의 선언은 부모 규칙, 뒤의 선언은 같은 선택자를 가진 새 규칙(소스 순서로 캐스케이드에 참가, 구체성은 부모와 같음) | `testSourceOrder` |
| 중첩된 `@media`/`@supports`/`@layer` | 그룹 규칙 안에서 선언은 부모 선택자의 규칙이 되고 안의 중첩 규칙도 부모 기준으로 해석 | `testNestedGroups` |
| 올바르지 않은 중첩 규칙 | 그 규칙만 버리고 부모는 유지 | `testRecovery` |

한계: CSSOM 은 평평한 규칙 목록이며 `CSSStyleRule.cssRules`/`CSSNestedDeclarations` 객체 모델과 규칙의 `selectorText` 는 `:is()` 로 펴진 형태이다(작성한 그대로가 아님). `&` 뒤에 타입 선택자(`&div`)는 규격처럼 무효이다. 중첩된 `@font-face`/`@keyframes` 는 규격에서 무효이지만 그대로 받아들인다. 의사 요소가 있는 부모(`::before { & b {} }`)는 `:is()` 안의 의사 요소가 무효라 규칙이 버려진다.

## 10. Container Queries(`@container`, `container-*`)와 Containment(`contain`, `content-visibility`)

규격: https://www.w3.org/TR/css-conditional-5/ 4 절, https://www.w3.org/TR/css-contain-3/, https://www.w3.org/TR/css-contain-2/ (판본은 위 "조회한 규격(CSS-H)" 표. 조회로 확인한 것: 질의 컨테이너 선택 규칙(5 절: 질의가 요구하는 모든 특성에 유효한 가장 가까운 조상 컨테이너, 이름 필터, 알 수 없는 특성이면 컨테이너 미선택), 컨테이너 상대 길이는 축마다 그 축을 받는 가장 가까운 컨테이너, `none and not or` 제외). 구현: `css.cssom.ContainerCondition`(전문 파싱·평가), `GroupRule.container`, `RuleIndex`(`Entry.containers`, `usesContainers`), `StyleResolver.containersMatch/updateContainerSizes`, `layout.ContainerSource`+`CssStyleSource`, `LayoutEngine.layoutWithContainers/measureContainers`, `layout.Containment`(격리 비트), `BoxBuilder`/`BlockLayouter`/`Css.clips` 의 훅.

| 기능 | 구현 | 시험 |
| --- | --- | --- |
| `container-type: normal|size|inline-size`, `container-name`(여러 이름), `container` 단축 | 속성 233~234, `Shorthands.Container` | `ContainmentPropertiesTest.testContainer` |
| `@container [name] <condition>` | 조건 = 크기 특성(`min-/max-width|height`, 범위 문법 `(w > 1px)`/`(1px <= w <= 2px)`, `inline-size`/`block-size`(쓰기 모드에 따라 물리로 바꿔 평가), `aspect-ratio`, `orientation`), `not`/`and`/`or`(섞으면 무효), 중첩 괄호, `style(--x: v)`/`style(--x)`, 알 수 없는 것은 unknown(거짓). 크기 특성은 미디어 질의 평가기에 컨테이너 content box 를 뷰포트로 준 환경으로 평가 | `ContainerQueryLayoutTest.testMinWidthOfTheContainer`, `testConditionSyntax`, `testStyleQueries` |
| 질의 컨테이너 찾기 | 요소의 엄격한 조상(의사 요소는 원본 요소 자신도) 중 가장 가까운, 이름이 맞고 `container-type` 이 있으며 질의가 요구하는 축을 격리하는 것(`inline-size` 컨테이너는 높이 질의 불가 → 다음 조상). `style()` 만 있는 질의는 이름이 맞는 가장 가까운 조상(이름이 없으면 부모) | `testNamedContainers`, `testAxes`, `testContentBoxAndViewport` |
| 레이아웃과의 순환 | 스타일은 컨테이너 크기에 달려 있고 크기는 레이아웃이 안다: `LayoutEngine` 이 배치 뒤 `ContainerSource.updateContainerSizes` 로 컨테이너의 content box 를 알리고, 기록과 다르면 그 서브트리의 캐시 스타일을 (리스너 통지 없이) 버린 뒤 다시 배치(최대 4 번 추가). 첫 배치는 이전 레이아웃에서 기록한 크기를 쓰므로 보통 한 번으로 끝난다 | `testNestedContainersSettle` |
| 크기 격리(`contain: size|inline-size|strict`, `container-type`, `content-visibility: hidden`) | `Containment`: shrink-to-fit 의 고유 너비 = `contain-intrinsic-width`(없으면 0), 자동 높이 = `contain-intrinsic-height`(없으면 0). 내용은 그대로 배치되어 넘침 | `testInlineSizeContainmentOfShrinkToFit`, `testSizeContainmentHeight` |
| 레이아웃/그리기 격리(`contain: layout|paint|content|strict`, `container-type`, `content-visibility: auto|hidden`) | 블록 서식 문맥(float 포함), 쌓임 맥락, 절대·fixed 자손의 포함 블록, 그리기 격리는 내용 자르기(`Css.clips`) | `testLayoutContainment` |
| `content-visibility: hidden` | 내용과 `::before/::after` 의 상자를 만들지 않음(`BoxBuilder.fill/fillFlex`). `auto` 는 보이는 것과 같음 | `testContentVisibility` |
| `contain-intrinsic-width/height`, `contain-intrinsic-size` | 계산 값과 크기 격리의 고유 크기 | `testContainer`, `testSizeContainmentHeight` |

한계: 컨테이너 안의 `em`/`rem` 은 컨테이너 글꼴이 아니라 미디어 질의 기본(16px)을 쓴다. 컨테이너 질의 단위 `cqw/cqh/cqi/cqb/cqmin/cqmax` 는 구현하지 않았다. `contain: style`(카운터 범위), `content-visibility: auto` 의 화면 밖 렌더링 생략, `contain-intrinsic-inline-size/block-size`, `contain-intrinsic-size` 의 `auto` 가 기억하는 마지막 크기, `scroll-state()` 질의, 표 상자(TABLE)가 컨테이너일 때의 정확한 content box(표 래퍼 근사)는 미구현. 컨테이너 크기가 서로 순환(규칙이 크기를 바꿔 조건을 뒤집음)하면 4 번 뒤에 멈춘 마지막 배치를 쓴다. `container-name` 에 `none` 이 다른 이름과 섞여도 받아들인다(`<custom-ident>` 문법이 `none` 을 거르지 않음).

## 11. `@scope`(CSS Cascading and Inheritance Level 6, https://www.w3.org/TR/css-cascade-6/ 2 절)

구현: `css.cssom.ScopeSpec`(전문 파싱, `findRoot`/`distance`, `MatchContext.ScopeTest` 로 `:scope` 의 의미를 바꿈), `GroupRule.scope`, `RuleIndex.Entry.scopes/scopeRelative`, `StyleResolver.compute`(범위 검사와 근접도), 정렬 비교기에 `proximity`(구체성 다음, 소스 순서 앞).

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| `@scope (start)` | 시작 선택자와 일치하는 요소가 범위 루트. 안의 규칙은 루트의 자손에만 적용(루트 자신은 제외). 선택자가 `:scope`/`&` 를 쓰면 루트 자신도 일치 가능(@scope 안의 `&` 는 구체성 0 의 `:where(:scope)`, 시트 최상위의 `&` 는 `:scope`) | `CssScopeTest.testBelowTheRoot`, `testScopeSelector` |
| `to (end)` | 끝 선택자와 일치하는 요소와 그 자손은 그 루트의 범위가 아님. 끝 선택자의 `:scope` 는 그 루트 | `testScopeLimit` |
| 범위 근접도 | 구체성이 같으면 루트가 요소에 더 가까운 규칙이 이기고, 범위 규칙이 범위 밖 규칙을 이긴다 | `testProximity` |
| 전문 없는 `@scope { }` | 루트 요소(`html`)를 루트로 삼음 | `testImplicitRootAndNesting` |
| 중첩된 `@scope`, `@media` 안/밖 | 안쪽 루트는 바깥 루트의 아래에 있어야 함 | `testImplicitRootAndNesting` |
| 올바르지 않은 전문 | 규칙만 버림(진단 INVALID) | `testRecovery` |

한계: 전문 없는 `@scope` 의 루트는 규격과 달리 `<style>` 요소의 부모가 아니라 문서 루트이다. 스타일 규칙 안에 중첩된 `@scope` 의 루트는 부모 규칙이 아니라 선언 그룹으로만 처리한다. `@scope` 안의 `:scope` 는 시작 선택자와 일치하는 모든 요소이다(여러 루트 중 어느 것인지는 가리지 않음).

## 12. 파싱과 계산 값만 있는 속성(CSS-H, 속성 233~276)

`PropertyTable` 에 추가(`P` 플래그는 렌더링 효과 없음): `container-type`/`container-name`/`content-visibility`/`contain-intrinsic-width|height`(효과 있음, §10), `scroll-snap-type|align|stop`, `scroll-padding-*`, `scroll-margin-*`, `overscroll-behavior-x|y`, `scrollbar-width|color|gutter`, `touch-action`, `color-scheme`, `forced-color-adjust`, `print-color-adjust`, `text-wrap-mode`(효과는 아래), `text-wrap-style`, `hanging-punctuation`, `hyphenate-character`, `text-justify`, `text-emphasis-style|color|position`, `text-underline-position`, `text-decoration-skip-ink`, `font-variant-ligatures|numeric|east-asian|position|alternates`, `font-synthesis`, `font-optical-sizing`, `font-variation-settings`, `font-language-override`. 단축 속성(`Shorthands`): `container`, `contain-intrinsic-size`, `scroll-padding`, `scroll-margin`, `overscroll-behavior`, `text-wrap`, `text-emphasis`. `PropertyRegistry` 의 미지원 목록에서 뺐다(`@supports (prop: value)`, `getComputedStyle`, 계단식이 그 속성을 알게 됨). 시험: `ContainmentPropertiesTest` 4건. 이 속성들 중 렌더링에 효과가 있는 것은 각 모듈 절에 적었고 나머지는 계산 값만 있다(한계).

## 13. `@property` — 등록된 사용자 정의 속성(CSS Properties and Values API 1, https://www.w3.org/TR/css-properties-values-api-1/ 3 절)

구현: `css.cssom.PropertyRule`(서술자 `syntax`/`inherits`/`initial-value` 검증: 이름은 `--` 로 시작, 문법은 허용한 자료형(`<length> <number> <percentage> <length-percentage> <color> <image> <url> <integer> <angle> <time> <resolution> <transform-function> <transform-list> <custom-ident> <string>`)과 키워드의 `|` `+` `#` 조합 또는 `*`), `StyleSheetBuilder.buildProperty`, `css.cascade.RegisteredProperty`(등록; 초기값이 맥락에 의존하면(`em`, `vw` 등) 등록하지 않음; 계산), `RuleIndex.registrations()`(나중 등록이 이김, 조건 그룹 안의 것은 그 그룹이 적용될 때만), `StyleComputer.compute(..., registered)`(글꼴 크기를 알기 위해 등록 없이 한 번 계산한 뒤 등록된 값을 계산하고 다시 계산).

| 동작 | 구현 | 시험 |
| --- | --- | --- |
| 선언 없는 속성의 값 = 초기값, `inherits: false` 이면 부모 값을 받지 않음 | `RegisteredProperty.apply` | `RegisteredPropertyTest.testInitialValueAndInheritance` |
| 문법에 맞지 않는 값은 계산 시점에 무효 → `unset`(상속이면 상속값, 아니면 초기값) | `ValueGrammar.match` 로 검사 | `testSyntaxCheck` |
| 계산 값 = 자료형의 정규 형태(`2em` → `40px`, `calc()` 단순화, 색), 자손은 계산된 값을 상속 | `StyleComputer.map` 후 직렬화 | `testComputedValue` |
| `var(--x)` 로 쓰면 계산된 형 있는 값이 치환됨 | 형 있는 사용자 정의 속성 맵으로 `resolvePending` | `testComputedValue`, `testColorsAndUniversalSyntax` |
| `inherit`/`initial`/`unset` | 전역 키워드 처리 | `testSyntaxCheck` |
| 올바르지 않은 등록(서술자 누락, 맥락 의존 초기값, 틀린 문법/자료형, `--` 없는 이름)은 무시 | `PropertyRule.create`, `RegisteredProperty.of` | `testInvalidRegistrations` |

한계: **등록된 속성의 전환과 애니메이션(`@keyframes` 안의 `--x`, `transition: --x`)은 구현하지 않았다**(값이 이산적으로 바뀐다). `CSS.registerProperty()` 와 `CSS.supports` 의 연결은 JS 쪽 작업이다. `<transform-list>` 와 `<image>` 의 정규 형태는 일반 값의 직렬화를 따른다. 계산한 값의 `getPropertyValue` 직렬화는 길이를 `px` 로, 숫자를 최소 자릿수로 쓴다.

## 14. Counter Styles 3(`@counter-style`, `symbols()`, 문자열 표식)

규격: https://www.w3.org/TR/css-counter-styles-3/. 구현: `css.cssom.CounterStyleRule`(이름 검증: `none`/CSS 전역 키워드/예약 이름(`decimal` `disc` `circle` `square` `disclosure-open` `disclosure-closed`)은 정의 불가, 서술자 보관), `RuleIndex.counterStyleRules()`(같은 이름은 나중 정의), `layout.CounterStyleSource`+`CssStyleSource`, `layout.CounterStyle`(시스템별 알고리즘), `layout.CounterStyles`(미리 정의된 스타일, 규칙 변환·검사, `symbols()`, 대체 사슬 최대 16), `ListMarkers`(옛 정적 메서드는 얇은 층으로 남김), `BoxBuilder`(목록 표식), `GeneratedContent`(`counter()`/`counters()`).

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| 미리 정의된 스타일 | decimal, decimal-leading-zero, lower/upper-roman(1~3999, 밖은 decimal), lower/upper-alpha·latin, lower-greek, disc, circle, square, disclosure-open/closed, cjk-decimal, 인도·아랍·태국·티베트·크메르·라오·미얀마 숫자 18종, hiragana, katakana, hiragana-iroha, katakana-iroha, korean-hangul-formal, korean-hanja-informal(±9999). 스타일마다 접미사(". ", " ", "、", ", ") | `CounterStylesTest.testPredefinedStyles` |
| 시스템 | `cyclic`, `fixed [n]`, `symbolic`, `alphabetic`(일대일 진법), `numeric`(위치 진법), `additive`(탐욕적 합, 0 은 가중치 0 기호), `extends <name>`(시스템과 쓰지 않은 서술자를 가져옴) | `testSystems`, `testDescriptors` |
| 서술자 | `negative`(1~2 기호), `prefix`, `suffix`, `range`(쉼표 구간들은 하나의 [최소, 최대] 로 근사, `infinite`, `auto`), `pad`(음수 부호를 포함한 길이 기준), `fallback`, 시스템별 자동 범위(alphabetic·symbolic 은 1 이상, additive 는 0 이상) | `testDescriptors` |
| 유효성 | cyclic/fixed/symbolic 은 기호 1 개 이상, alphabetic/numeric 은 2 개 이상, additive 는 `additive-symbols`(가중치 감소), extends 대상이 없거나 순환이면 정의를 무시(이름은 미리 정의된 스타일 또는 decimal 이 됨) | `testDescriptors` |
| `list-style-type: "문자열"` | 문자열이 표식 그 자체(접미사 없음) | `testSymbolsStringsAndCounters` |
| `list-style-type: symbols(...)`, `counter(n, symbols(...))` | 익명 스타일. 종류 생략 시 symbolic, 접미사 " " | 같은 시험 |
| `counter()`/`counters()` 의 스타일 인수 | `@counter-style` 이름, 미리 정의된 이름, `symbols()`. 접두사/접미사 없이 표현만 | 같은 시험 |

한계: `speak-as`, 이미지 기호(`symbols` 의 `<image>`), 이름 있는 `@counter-style` 의 `system: fixed` 의 음수 처리 세부, `@font-feature-values`, `list-style-type` 의 `<counter-style-name>` 에 대한 CSSOM(`CSS.counterStyles`)은 구현하지 않았다. `disclosure-closed` 는 오른쪽에서 왼쪽 방향에서도 `▸` 이다(규격은 방향에 따라 `◂`). 한자/한글 숫자는 10000 이상에서 `cjk-decimal` 로 대체된다. `armenian`, `georgian`, `hebrew`, `japanese-*`, `simp-chinese-*`, `trad-chinese-*`, `ethiopic-numeric`, `cjk-heavenly-stem` 등 나머지 미리 정의된 스타일은 아직 없다(decimal 로 표시됨). 문자열 키워드는 소문자 비교로 미리 정의된 이름을 찾고, 저자 정의 이름은 대소문자를 구분한다.

## 15. `text-overflow: ellipsis`와 `line-clamp`(CSS UI 3 text-overflow, CSS Overflow 4 line-clamp)

이전에는 `text-overflow` 가 플래그 없는 속성(효과 있음)으로 표시되어 있었지만 레이아웃/그리기 어디에도 구현이 없었다(`grep ellipsis` 로 확인) — 이번에 구현했다. 규격: https://www.w3.org/TR/css-ui-3/#text-overflow, https://www.w3.org/TR/css-overflow-4/#line-clamp. 구현: `layout.TextOverflow`(완성된 줄 상자 위의 후처리), `BlockLayouter.finish` 의 호출, `Prop.LINE_CLAMP`=277(`line-clamp: none | <integer>`, `-webkit-line-clamp` 별칭).

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| `text-overflow: ellipsis` | `overflow` 가 visible 이 아닌(또는 paint containment 가 있는) 블록 컨테이너의 줄이 content box 를 넘으면, 줄임표(U+2026)가 content box 안에서 끝나도록 텍스트를 글자(서로게이트 쌍은 하나)마다 줄이고(뒤의 공백은 버림) 뒤의 조각을 지운 뒤 마지막 텍스트의 스타일로 줄임표 조각을 더한다. 줄에 맞으면 그대로 | `TextOverflowLayoutTest.testEllipsisOfOverflowingLine`, `testNoEllipsisWhenItIsNotNeeded` |
| `text-overflow: "문자열"`, 값 둘(`clip "~"`) | 문자열이 줄임표를 대체. 값이 둘이면 둘째(줄 끝) | `testStringAndAtomicBoxes` |
| 원자적 상자(inline-block 등) | 들어가지 않으면 통째로 지움(자르지 않음) | `testStringAndAtomicBoxes` |
| `line-clamp: N`, `-webkit-line-clamp: N` | 줄 상자를 N 개만 남기고 블록 높이를 N 번째 줄의 아래로 줄이며, 지운 줄이 있으면 마지막 줄 끝에 줄임표를 붙인다(내용이 다 들어가면 안 붙임). `display: -webkit-box` 의 익명 항목에서도 동작 | `testLineClamp` |
| 그리기 | 자른 페이지의 화소가 "남은 텍스트 + 줄임표" 로 된 페이지와 같고, 자른 줄은 그려지지 않음 | `TextOverflowPaintTest` |

한계: 왼쪽에서 오른쪽(`direction: ltr`)만 다룬다(`rtl` 은 자르지 않음). 줄임표는 글꼴 자간(`letter-spacing`)과 단어 간격을 정확히 반영하지 않은 `advance` 합으로 자르며(`justify` 줄은 정렬 전의 폭), 세로쓰기 줄은 처리하지 않는다. `line-clamp` 는 블록 컨테이너의 직접 인라인 콘텐츠의 줄만 센다(중첩된 블록 자식에 걸친 N 줄 제한과 `line-clamp` 의 블록 줄임표 값은 없다). `-webkit-box-orient` 는 여전히 미지원 이름이다(없어도 동작). `text-overflow` 의 줄임표 위로 마우스를 올리는 상호작용(툴팁)은 없다.

## 16. 소프트 하이픈(`hyphens`)과 `text-wrap: balance`(CSS Text 3 5 절, CSS Text 4 text-wrap-style)

규격: https://www.w3.org/TR/css-text-3/#hyphenation, https://www.w3.org/TR/css-text-4/#text-wrap-style (판본은 위 "조회한 규격(CSS-H)" 표). 구현: `layout.InlineLayouter`(`Seg.shy`, `Flow.text` 의 소프트 하이픈 처리, `hyphenWidth`/`hyphenateEnd`, `balanced`/`balancedWidth`/`layout`), `Prop.TEXT_WRAP_STYLE`(이제 `P` 플래그 없음).

JDK 6u45 Java2D 는 U+00AD 를 **보이는 하이픈 글리프로 그리고 너비도 가진다**(`FontProbe` 로 확인: monospaced 20px 에서 U+00AD 너비 20, 잉크 6 화소)는 것을 발견했다 — 이전 구현은 모든 `&shy;` 를 항상 하이픈으로 보여 주었다. 이제:

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| `&shy;`(U+00AD) | 배치하는 텍스트에서는 폭 없는 공백(U+200B)으로 바꾸어 줄바꿈 기회로 두고(그리지 않음, 너비 0) 원문 위치는 보존. 줄이 거기서 나뉘면 줄 끝의 그 자리가 `-` 가 되고 조각이 하이픈 너비만큼 커진다. 하이픈이 들어갈 자리가 있는지도 줄바꿈 판단에 넣는다(안 들어가면 더 앞선 기회에서 나눔) | `HyphensAndBalanceLayoutTest.testBreakAtSoftHyphen` |
| 줄 중간의 `&shy;`, `white-space: nowrap`/`pre` | 보이지 않고 너비 0 | `testInvisibleWithoutBreak` |
| `hyphens: none` | `&shy;` 를 없앤다(줄바꿈 기회도 없음) | `testInvisibleWithoutBreak` |
| `hyphens: manual`(기본)/`auto` | `auto` 는 사전이 없어 `manual` 과 같다(소프트 하이픈에서만 나눔) | — |
| `text-wrap: balance`(`text-wrap-style: balance` 와 `wrap` 모드) | 가로쓰기 왼쪽→오른쪽 컨테이너의 문단(줄 2~6 개)을 먼저 전체 너비로 재어 줄 수 n 을 얻고, 줄 수를 n 으로 유지하는 가장 좁은 너비를 이분 탐색(12 번)해 그 너비로 배치한 뒤 줄을 `text-align`(left/center/right) 에 따라 컨테이너 안에서 옮긴다. float 가 있는 문맥, 첫 줄(`::first-line`)이 있는 문단, `pre*`, rtl 에서는 하지 않음 | `testBalancedLines`, `testBalancedLinesStayAligned` |
| `text-wrap: stable`/`pretty` | 균형 없음(계산 값만) | `testBalancedLines` |

한계: 하이픈 문자는 항상 U+002D 이다(규격은 글꼴이 있으면 U+2010). 소프트 하이픈이 세그먼트(요소) 경계의 맨 끝에 있으면 줄바꿈 기회가 되지 않는다. `hyphens: auto` 의 자동 음절 나눔(사전)은 없다. `hyphenate-character`, `hyphenate-limit-*` 는 계산 값만. balance 는 `justify` 와 `text-align-last`, `white-space: pre-line` 은 고려하지 않으며, 균형 너비는 줄 수만 보고 줄 길이 분산을 최소화하지는 않는다(Chromium 도 같은 단순한 방식이다).

## 17. 색 구성표(`color-scheme`, `light-dark()`, 시스템 색)와 글꼴 변형(`font-variant: small-caps`, `font-size-adjust`)

규격: https://www.w3.org/TR/css-color-adjust-1/, https://www.w3.org/TR/css-color-5/ (light-dark), https://www.w3.org/TR/css-fonts-3/#small-caps, https://www.w3.org/TR/css-fonts-4/#font-size-adjust-prop (판본은 위 "조회한 규격(CSS-H)" 표). 구현: `css.values.CssColor`(`lightDark`, `resolve(dark)`, 시스템 색은 밝은/어두운 값을 함께 가짐), `NamedColors.lookupSystemDark`, `LengthContext.withPrefersDark/withDark`, `StyleComputer.usesDark`/`computeCore`/`map`, `ComputedStyle.usesDarkScheme()`, `StyleResolver`(환경의 `prefers-color-scheme` 를 계산에 전달), `render.DisplayListBuilder.paintCanvasBackground`(어두운 루트의 캔버스), `layout.InlineLayouter`(`small-caps`), `layout.FontResolver.adjusted`(`font-size-adjust`).

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| 요소의 색 구성표 | `color-scheme` 에 `dark` 만 있으면 어두움, `light dark` 이면 환경 선호(`MediaEnvironment.getColorScheme()`)를 따름, `normal`/`light`/그 밖은 밝음. 상속 속성이므로 서브트리에 적용되고 `light` 로 되돌릴 수 있음 | `ColorSchemeTest.testLightByDefault`, `testDark`, `testPreferenceAndOverride` |
| 시스템 색 | `Canvas`(#121212) `CanvasText`(#fff) `LinkText` `VisitedText` `ActiveText` `ButtonFace` `ButtonText` `Field` `FieldText` `Highlight` `HighlightText` `Mark` `MarkText` `GrayText` 등이 어두운 값을 가짐(Chromium 의 어두운 값). 계산 시점에 요소의 구성표로 결정 | `testDark` |
| `light-dark(a, b)` | `CssColor.lightDark` 로 둘을 보관하고 계산 시점에 요소의 구성표로 고름(`currentcolor` 가 끼면 밝은 값) | `testLightByDefault`, `testDark` |
| 루트 요소의 기본 글자색, 캔버스 | 어두운 구성표의 루트는 `color` 선언이 없으면 CanvasText(밝음). 루트와 body 에 배경이 없으면 캔버스를 Canvas(어두운 값)로 칠함. 선언한 배경이 우선 | `testDark`, `ColorSchemePaintTest` |
| `font-variant: small-caps`, `all-small-caps` | 소문자(all- 은 모든 글자)를 대문자로 바꿔 글꼴 크기의 80 % 로 그림(종류가 바뀔 때 세그먼트를 나눔, 공백과 문장 부호는 보통 크기). 원문 위치는 유지 | `FontVariantLayoutTest.testSmallCaps` |
| `font-size-adjust: <number>` | `x` 글리프의 높이로 잰 글꼴의 aspect 로 크기를 `크기 × 값 ÷ aspect` 로 조정(줄 높이 기준 크기는 명목 값 유지, 최대 4 배) | `testFontSizeAdjust` |

한계: **폼 컨트롤(`ControlPainter`)과 스크롤 영역의 그리기는 구성표를 따르지 않는다**(고정 밝은 색; 어두운 구성표에서 컨트롤의 배경/글자색은 스타일이 `buttonface`/`field` 등 시스템 색을 쓰는 부분만 어둡게 됨). UA 스타일시트의 링크 색은 시스템 색이 아니라 고정 값이면 바뀌지 않는다. `<meta name=color-scheme>` 와 `only` 키워드, 어두운 구성표의 `forced-colors` 는 구현하지 않았다. `font-variant` 는 여전히 캡 값만 받는 개별 속성이다(`font-variant: common-ligatures` 같은 단축 속성 형태는 무효이며, `font-variant-caps` 이름도 없다). `font-variant-ligatures/numeric/east-asian/position/alternates`, `font-feature-settings`, `font-kerning`, `font-variation-settings`, `font-optical-sizing`, `font-synthesis`, `font-display` 는 파싱과 계산 값만 있고 Java2D(JRE 6)가 OpenType 기능을 줄 수 없어 화면 효과가 없다. `small-caps` 는 글꼴에 진짜 작은 대문자가 있어도 항상 합성한다.

## 18. 컨테이너 상대 길이(`cqw` `cqh` `cqi` `cqb` `cqmin` `cqmax`)와 텍스트 보완(`line-break: anywhere`, `text-justify: none`)

규격: https://www.w3.org/TR/css-conditional-5/ (4.4 Container Relative Lengths), https://www.w3.org/TR/css-text-4/#line-break-property, #text-justify-property (판본은 위 "조회한 규격(CSS-H)" 표). 구현: `css.values.CssUnits`(`CONTAINER_LENGTH` 종류와 6 개 단위, `calc()` 안에서도), `LengthContext`(`withContainer`, `toPx`), `StyleResolver.containerLengthBase`(요소의 가장 가까운 조상 컨테이너를 축마다 찾아 이미 잰 크기를 줌), `RuleIndex`(`container-type` 를 선언하는 규칙이 있으면 `usesContainers` 로 레이아웃이 컨테이너를 잼), `InlineLayouter`.

| 기능 | 동작 | 시험 |
| --- | --- | --- |
| `cqw`/`cqi` | 가장 가까운 `size`/`inline-size` 컨테이너(세로쓰기이면 `size` 만) content box 너비의 1 % | `ContainerQueryLayoutTest.testContainerLengths` |
| `cqh`/`cqb` | 가장 가까운 `size` 컨테이너(세로쓰기의 `inline-size` 도) content box 높이의 1 %. `inline-size` 컨테이너는 건너뜀 | 같은 시험 |
| `cqmin`/`cqmax` | 두 값 중 작은/큰 쪽 | 같은 시험 |
| 컨테이너가 없는 축 | 뷰포트 크기의 1 %(작은 뷰포트와 같음) | 같은 시험 |
| 컨테이너 크기가 바뀔 때 | 컨테이너 질의와 같은 방식으로 (`@container` 규칙이 없어도) 레이아웃 뒤 크기를 알려 서브트리 스타일을 다시 계산 | 같은 시험(뷰포트 400 사례) |
| `line-break: anywhere` | 모든 글자 사이가 줄바꿈 기회, 긴 단어도 나눔(`word-break: break-all` + `overflow-wrap: anywhere`) | `TextWrapLayoutTest.testLineBreakAnywhereAndTextJustify` |
| `text-justify: none` | `text-align: justify` 의 양쪽 정렬을 끔 | 같은 시험 |

한계: 컨테이너 상대 길이는 인라인 `style` 속성만으로 컨테이너를 선언한 문서(규칙 없음)에서는 첫 배치가 뷰포트 단위로 대체한 값을 쓰며 레이아웃 크기 알림도 하지 않는다(`usesContainers` 가 스타일 시트 규칙으로만 켜진다). `line-break: loose/normal/strict`(CJK 소형 가나 규칙)와 `text-justify: inter-character/distribute`(글자 사이 간격 분배)는 계산 값만 있다. `text-wrap-style: pretty`, `hanging-punctuation`, `hyphenate-limit-*`, `text-emphasis-*`, `text-underline-position`, `text-decoration-skip-ink`, `initial-letter`, `scroll-snap-*`, `scroll-padding/margin`, `overscroll-behavior`, `scrollbar-*`(엔진이 스크롤바를 그리지 않음)는 파싱·계산 값만 있다.

## 19. CSS Paged Media 3 (`@page`, `size`, `page`, `marks`, `bleed`, `page-orientation`) — CSS-I 후속, 커밋 880

규격: https://www.w3.org/TR/css-page-3/ (W3C Working Draft, 14 September 2023, 조회 2026-11-17, 본문 4.2 쪽 선택자, 4.4 쪽 문맥의 계단식, 6 쪽 속성, 7 쪽 크기와 표시를 줄 단위로 읽고 구현). 이전 CSS-I 가 중단될 때 남긴 미검증 WIP(`wip/cssi-page-unverified`)를 컴파일·시험으로 확인한 뒤 통과한 부분만 가져왔다: `PageRule`/`MarginRule`/`StyleSheetBuilder.buildPage`/`CssomInterfaces` 는 가져오고, WIP 의 선택자 구체성(참/거짓)은 규격 4.4 의 "같은 의사 클래스를 되풀이하면 구체성이 올라간다"에 맞지 않아 개수 세기로 고쳤다. WIP 의 나머지 속성(backdrop-filter 등)은 각 모듈 시험과 함께 다시 만들었다.

| 기능 | 구현 | 시험 |
| --- | --- | --- |
| `@page <쪽 선택자 목록> { ... }` | `css.cssom.PageRule`(쪽 이름 + `:left :right :first :blank` 의 쉼표 목록, 빈 목록은 모든 쪽, 틀린 목록/모르는 의사 클래스/공백이 낀 의사 클래스는 규칙을 버림, 블록 없는 규칙 버림), `StyleSheetBuilder.buildPage` | `PageRuleTest.testSelectorParsing` |
| 열여섯 쪽 여백 규칙 `@top-left-corner ... @right-bottom` | `css.cssom.MarginRule`(선언 보관, 알 수 없는 at-규칙/중첩 스타일 규칙은 무시) | `testMarginRules` |
| CSSOM | `CSSPageRule`(`selectorText` 읽기/쓰기, `style`, `cssRules`), `CSSMarginRule`(`name`, `style`), 종류 6/9 | `testCssomModel`, `CssomModernRulesTest.testPageRule` |
| 쪽 문맥의 계단식 | `css.cascade.PageStyles.resolve(규칙, 쪽 이름, 쪽 번호, 빈 쪽, 오른쪽)`: 일치하는 규칙을 구체성(이름 > `:first/:blank` 개수 > `:left/:right` 개수) 순으로 적용하고 같으면 소스 순서, `!important` 는 모두를 이김. `StyleResolver.getPageRules()` 가 환경에 적용되는 시트(미디어 그룹 포함)의 `@page` 를 모음 | `testCascadeInThePageContext` |
| `size` | `auto`(이 엔진은 A4 세로), `<length>{1,2}`(하나면 정사각형), 용지 이름 10종(`a5 a4 a3 b5 b4 jis-b5 jis-b4 letter legal ledger`), `portrait/landscape`(이름이나 기본 용지의 방향을 돌림), 음수/섞인 값/두 방향은 선언 무효 → 초기값 | `testPageSize` |
| 쪽 여백 `margin` | 쪽 크기 기준 백분율(좌우는 너비, 위아래는 높이: 규격 예 `10%`), `calc()`, `em` 은 쪽 문맥 글꼴(16px). 쪽 영역 = 쪽 상자 − 여백 | `testMargins` |
| `marks`, `bleed`, `page-orientation`, `page` | `marks: none | crop || cross`(정규 순서 직렬화), `bleed: auto` 는 `crop` 이 있으면 6pt(8px), 아니면 0, `page-orientation`(계산 값 그대로), 요소의 `page`(`auto | <custom-ident>`, 상속 안 함, 쪽 이름으로 쓰임) | `testMarksBleedAndOrientation`, `testPageProperty` |

한계: **인쇄가 없으므로 쪽 상자, 여백 상자, 쪽 나눔은 배치하지 않는다**(빌더가 `UNSUPPORTED "@page"` 진단을 남긴다). 쪽 여백의 UA 기본값은 0(인쇄할 수 없는 영역이 없음). `page-orientation` 은 회전하지 않고 계산 값만 있다. `page` 속성의 "사용 값"(쪽 나눔에서 요소 이름을 쪽에 전파)은 구현하지 않았다. 쪽 문맥의 상속(`@page { color }` 가 여백 상자에 상속)은 계산하지 않는다. 규격의 쪽 영역 예시는 양쪽 여백을 한쪽씩만 뺀 값(189mm x 267.3mm)을 쓰지만 양쪽을 모두 빼면 168mm x 237.6mm 이며 시험은 후자를 확인한다.

## 20. Filter Effects 2 (`backdrop-filter`)

규격: https://drafts.csswg.org/filter-effects-2/ (Editor's Draft, 2 October 2026, 조회 2026-11-17, 2 절 "Filtering and Clipping"을 읽고 구현). 구현: `layout.FilterSpec.ofBackdrop/hasBackdrop`(함수 목록은 `filter` 와 같은 코드), `PropertyTable`(`Prop.BACKDROP_FILTER`=283, 별칭 `-webkit-backdrop-filter`), `Effects.makesStackingContext`/`BoxBuilder`(쌓임 맥락과 fixed/absolute 자손의 포함 블록), `PaintWalker.context`(효과 그룹 생성), `render.EffectGroup`(배경 필터와 border box 모양), `PaintContext.surface`(그려지는 이미지), `PagePainter`(배경 필터가 있는 목록만 읽을 수 있는 이미지에 그림), `render.BackdropFilters`.

| 동작 | 구현 | 시험 |
| --- | --- | --- |
| 상자 뒤에 이미 그려진 내용을 필터링 | 가장 가까운 버퍼 그룹(백드롭 루트에 해당: 페이지 버퍼, 불투명도/필터/마스크 그룹의 버퍼)에서 border box 영역을 읽어 `FilterRenderer` 로 필터를 적용하고 `Src` 합성으로 그 자리를 대체한 뒤 요소를 그 위에 그림 | `BackdropFilterPaintTest.testInvertInsideTheBox`, `testElementContentIsPaintedOnTop` |
| 영역 = border box(`border-radius` 포함) | `RoundedBox.ofBorderBox(...).toShape()` 으로 자름. 자식의 크기는 영역에 영향 없음 | `testRoundedCorners` |
| 흐림의 가장자리 모드 `mirror` | 흐림이 닿는 거리만큼 상자 안쪽 내용을 거울처럼 이어 붙인 입력을 필터에 줌(가장자리가 투명해지며 옅어지지 않음) | `testBlurAndMirroredEdge` |
| 쌓임 맥락 + fixed/absolute 포함 블록 | `filter` 와 같은 규칙 | `testStackingContextAndContainingBlock` |
| 스크롤/클립 | 클립 사각형과 스크롤 오프셋에서도 같은 화소 | `testPaintWithClipAndScroll` |
| `url()` 이나 모르는 함수 | `filter` 와 같이 사슬을 무시 | `testParsingAndAlias` |

한계: 변환된(회전/기울임) 요소는 변환된 모양 안을 자르되 필터는 그 경계 사각형을 입력으로 쓰므로 규격이 말하는 "역변환한 버퍼"가 아니다. 요소의 `opacity` 는 필터링한 배경에 곱해지지 않는다(규격은 곱함). 배경 필터가 없는 페이지는 이전과 같은 경로이다. `backdrop-filter` 의 애니메이션 보간은 없다. 히트 테스트에는 영향이 없다.

## 21. CSS Fonts 4 글꼴 변형과 OpenType 기능 (`font-variant` 단축 속성, `font-variant-*`, `font-feature-settings`, `font-synthesis`)

규격: https://www.w3.org/TR/css-fonts-4/ (W3C Working Draft, 12 November 2026, 조회 2026-11-17, 6.12 `font-variant`, 우선순위(feature precedence), `font-synthesis-*` 를 읽음). 구현: `Prop.FONT_VARIANT_CAPS`(=이전의 `FONT_VARIANT` id 71, 이름만 `font-variant-caps` 로 바뀜), `Shorthands.FontVariant`(여섯 개별 속성의 단축 속성), `font` 단축 속성이 나머지 변형/`font-feature-settings` 를 초기화, `layout.FontFeatures`, `InlineLayouter`(`text`/`emit`/`buildLine`).

| 기능 | 구현 | 시험 |
| --- | --- | --- |
| `font-variant` 단축 속성 | 키워드를 속한 개별 속성(`caps ligatures numeric east-asian position alternates`)으로 나누고(서로 떨어져 있어도 됨, 순서 자유) 개별 속성 문법으로 확인. `normal` 은 모두 초기화, `none` 은 `font-variant-ligatures: none`, 쓰지 않은 개별 속성은 초기값, 틀린 키워드/같은 개별 속성의 두 값은 무효 | `FontFeaturesTest.testShorthand` |
| `font-variant-alternates` | `stylistic() styleset() character-variant() swash() ornaments() annotation()` 와 `historical-forms` 문법 | `testShorthand` |
| `font` 단축 속성 | `font-variant-caps` 를 정하고 나머지 변형과 `font-feature-settings` 는 초기화(`normal`/`small-caps` 만 허용) | `testFontShorthand`, `PropertyTest.testFont` |
| 기능 태그와 우선순위 | `FontFeatures.of(style)`: `font-variant-*` 와 `font-kerning` 이 태그(`liga clig dlig hlig calt lnum onum pnum tnum frac afrc ordn zero jp78.. fwid pwid ruby subs sups smcp c2sc pcap c2pc unic titl hist kern`)가 되고 `font-feature-settings`(태그 문자열 + 정수/`on`/`off`, 나중 항목이 앞을 대체)가 그 뒤에 적용되어 이김 | `testTags`, `testFeatureSettings` |
| **화면 효과(이 엔진이 직접 재현하는 것)** | `smcp/c2sc` 와 `pcap/c2pc`: 소문자(c2 는 모두)를 80 % 크기 대문자로(petite 도 같은 합성), `sups/subs`: 글자를 70 % 로 줄이고 기준선에서 글꼴 크기의 1/3 올리거나 1/5 내림, `fwid`: ASCII 를 전각(U+FF01~FF5E)으로, `font-synthesis`(`none` 또는 `small-caps`/`position` 목록)가 합성을 허용할 때만 | `testCapsEffects`, `testPosition`, `testFullWidth`, `testSynthesisPermission` |

한계: Java2D(JRE 6)는 글꼴의 OpenType 기능을 켜고 끌 수 없으므로 **합자(`liga` 등), 숫자 모양(`tnum`/`onum`/`zero`...), 대체 형태(`hist`/`ss01`/`salt`), 동아시아 글리프 변형(`jp78`...), `font-kerning`, `font-variation-settings`, `font-optical-sizing` 은 해석만 하고 화면에는 효과가 없다**(`unicase`/`titling-caps` 도 효과 없음). 합성한 작은 대문자와 위/아래첨자는 글꼴의 진짜 글리프가 아니라 크기만 줄인 것이다. 위첨자/아래첨자는 줄 높이에 영향을 주지 않고(기준선만 옮김) 줄 위쪽 밖으로 나갈 수 있다. `font-variant-east-asian: full-width` 는 코드 포인트를 바꾸므로 `getContent()` 에는 영향이 없으나 텍스트 선택/복사에는 전각 문자로 나온다. `font-synthesis-weight/style` 은 굵게/기울임 합성에 연결하지 않았다. `@font-feature-values` 는 미구현.

## 22. CSS Text Decoration 4 강조점 (`text-emphasis-style`, `-color`, `-position`, `text-emphasis`)

규격: https://www.w3.org/TR/css-text-decor-4/ (W3C Working Draft, 4 May 2022, 조회 2026-11-17, 3.1~3.4 절을 읽고 구현). 구현: `layout.TextEmphasis`(모양/채움/문자열/색/위치와 점이 붙는 글자 판정), `InlineLayouter`(`Seg.emUp/emDown` 과 줄 높이), `render.EmphasisMarks`(+ `DisplayCommand.Type.EMPHASIS`), `DisplayListBuilder.paintText`. 세 속성의 "파싱만" 플래그를 풀었다.

| 동작 | 구현 | 시험 |
| --- | --- | --- |
| 모양: `dot circle double-circle triangle sesame` × `filled/open`, `<string>`(첫 글자 단위) | `filled`/`open` 만 쓰면 규격 판본(2022)대로 `circle`(이전 판은 dot), `filled` 가 기본 채움, 두 채움은 무효 | `TextEmphasisPaintTest.testMarkOfTheStyle` |
| 점이 붙는 글자 | 글자 단위마다 하나. 구분자(Z*), 제어/서식/미할당(Cc Cf Cn), 문장 부호(P*; 단 `# % & @ § ¶ ‰ ‱ ٪ ؉ ؊ ⁊ ⁋ ⁓ 〽` 는 붙음)에는 안 붙음 | `testWhichCharactersAreMarked` |
| 색, 위치 | `text-emphasis-color`(기본 글자 색), `text-emphasis-position: over|under [left|right]`(가로쓰기는 over/under 만 의미), 단축 속성은 위치를 건드리지 않음 | `testShorthandColorAndPosition` |
| 크기, 상속 | 글꼴 크기의 50 %. 상속 속성이며 자식이 `none` 으로 끌 수 있음 | `testSizeAndInheritance` |
| 줄 높이 | ruby 텍스트처럼 점의 높이를 텍스트 상자의 위(over)나 아래(under)에 더해 줄 상자를 키움(기준선 정렬 상자) | `testRoomOfTheMarks` |
| 그리기 | 글자의 advance 합(자간/단어 간격 포함)으로 글자 가운데에 점을 그림. RTL 조각은 좌우를 뒤집음 | `testMarksOverTheText`, `testMarksUnderTheText`, `testNoMarks` |

한계: **세로쓰기(회전한 텍스트)의 강조점은 그리지 않는다**(`text-emphasis-position` 의 left/right 는 계산 값만). 점은 글꼴의 글리프가 아니라 Java2D 가 그리는 문자이므로 글꼴에 없으면 대체 글꼴을 쓴다. 밑줄/윗줄과 점이 겹칠 때의 위치 조정, ruby 와 점의 충돌 처리, `text-emphasis-skip`, 점 글꼴로 Kenten 같은 전용 글꼴 선택은 없다. 결합 문자열은 코드 포인트 단위로 점을 붙인다(그래핌 클러스터 단위가 아님).

## 23. 어두운 색 구성표의 폼 컨트롤 (CSS Color Adjustment 1, §17 의 "폼 컨트롤 제외" 한계 해소)

구현: `render.ControlSnapshot.dark`(스냅샷이 컨트롤 스타일의 `usesDarkScheme()` 를 보관), `render.ControlPainter.Palette`(밝은/어두운 두 팔레트 21 색: 강조색과 비활성 강조색, 상자 가장자리와 채움과 체크 표시, 드롭다운 화살표, 스크롤 막대의 트랙과 손잡이, 파일 단추의 채움과 글자, 진행 막대 트랙과 줄무늬와 윤곽, 미터 트랙, 슬라이더 트랙, 색 선택 단추 가장자리, 리스트 선택, 비활성 글자). 컨트롤의 글자색/배경/테두리는 이전처럼 스타일에서 오므로 UA 스타일시트가 시스템 색(`field`, `buttonface` ...)을 쓰는 부분은 이미 어두웠고, 이번에는 그리는 쪽이 직접 칠하는 부분을 구성표에 따르게 했다. 어두운 값은 Chromium 의 어두운 모양(강조색 #99C8FF, 가장자리 #858585, 채움 #3B3B3B, 트랙 #2B2B2B/#3B3B3B)을 참고한 근사값이다(규격은 구체적인 색을 정하지 않는다). `accent-color` 로 지정한 색은 두 구성표에서 그대로 쓴다. 시험 `ControlColorSchemePaintTest` 5건(스냅샷의 구성표, 체크박스/라디오 화소, 진행 막대·미터·슬라이더 트랙, 드롭다운 화살표와 파일 단추, 리스트 박스 스크롤 막대, 밝은 페이지가 이전과 같은 색).

한계: `color-scheme` 이 `light dark` 이면 환경 선호(`MediaEnvironment.getColorScheme()`)를 따른다(§17). 컨트롤의 포커스 링, `<input type=date>` 같은 달력 팝업, 네이티브 `<dialog>`/`<details>` 표식의 어두운 색은 다루지 않았다. 텍스트 필드의 캐럿 색은 글자 색을 따른다.

## 24. CSS Scroll Snap 1 / Overscroll / Scrollbars 1 의 계산 값 시험과 논리 `scroll-margin`/`scroll-padding`

규격: https://www.w3.org/TR/css-scroll-snap-1/, https://www.w3.org/TR/css-scrollbars-1/ (§12 의 판본 표). 엔진에는 자체 스크롤 막대와 스냅이 없으므로(호스트의 `JScrollPane` 이 페이지를 스크롤한다) **계산 값과 계단식, 문법 검사, 단축 속성, 상속만** 구현하고 시험했다. 이번에 더한 것: 논리 `scroll-margin-block-start/-end`, `-inline-start/-end` 와 `scroll-padding-*` 8 개 개별 속성(`Prop` 296~303)과 단축 속성 `scroll-margin-block|inline`, `scroll-padding-block|inline`. `LogicalProps` 에 둘째 범위(`SCROLL_FIRST`~`SCROLL_LAST`)를 두어 `writing-mode` 와 `direction` 에 따라 물리 변(`scroll-margin-top` 등)으로 옮겨 쓰며, 계단식에서 물리 선언과 순서대로 겨룬다(§4 와 같은 방식). 시험 `ScrollPropertiesTest` 8건: `scroll-snap-type` 11개 올바른 값과 6개 틀린 값, `scroll-snap-align` 한 개/두 개, `scroll-snap-stop`, `scroll-padding`(길이/백분율/auto, 음수 거부), `scroll-margin`(음수 허용, 백분율/auto 거부), 단축 속성 1~4 값, 논리 속성의 가로/rtl/세로쓰기 대응과 물리 속성과의 순서, `overscroll-behavior` 한 개/두 개, `scrollbar-width/color/gutter`(`scrollbar-color` 만 상속), `touch-action` 조합, `@supports` 인식, `em` 이 px 로 계산됨.

한계: 스냅 위치 계산, 스크롤바 그리기, `overscroll-behavior` 의 스크롤 연쇄 차단, `scrollbar-width: none` 으로 스크롤바 숨기기, `scrollbar-gutter` 의 공간 예약, `touch-action` 의 제스처 제한은 없다(엔진이 스크롤을 소유하지 않음). `overscroll-behavior-block/inline` 논리 이름은 구현하지 않았다.

## 25. 등록된 사용자 정의 속성의 전환과 애니메이션 (§13 의 한계 해소; CSS Properties and Values API 1)

구현: `StyleResolver.registeredPropertyNames/typedCustomValue/computeRegisteredValue/restyleWithCustom`(등록된 형 있는 속성의 값을 얻고, 애니메이션 값을 선언보다 이기게 넣어 스타일을 다시 계산한다), `RegisteredProperty.typed/isTyped`(문법으로 형 있는 값으로 파싱; `<number>` 의 `1` 은 정수가 아니라 실수로 다뤄 보간이 반올림되지 않게 함), `css.anim.AnimationEngine`(등록된 속성 이름에 속성 id 가 아닌 번호 `CUSTOM_BASE + 순번`을 붙여 전환/키프레임 코드를 그대로 지나가게 함: `startTransitions` 가 이름별로 이전/이후 형 있는 값을 비교, `buildFrames` 가 키프레임의 `--x:` 선언을 문법에 맞춰 계산, `collectCustom`/`progressOf` 가 어느 시각의 값을 모아 `animate` 가 `restyleWithCustom` 으로 `var()` 로 쓰는 속성을 다시 계산).

| 동작 | 시험 `RegisteredPropertyAnimationTest` |
| --- | --- |
| `transition: --w 1s linear`: 등록된 `<length>` 가 계산 값 사이를 달리고, `width: var(--w)` 가 따라가며, 이벤트(`transitionrun/start/end`)의 `propertyName` 이 `--w` | `testLengthTransitionReachesVar` |
| 상속하는 속성은 자손이 그 순간의 값으로 `var()` 를 풀고, `transition: all` 이 등록된 속성도 덮음 | `testInheritedAndAll` |
| `<number>`, `<color>`(알파 곱 보간), `<percentage>`, `<integer>`(반올림) | `testOtherTypes` |
| 보편 문법 `*`, 등록되지 않은 속성, 섞을 수 없는 형(`<length> | <percentage>` 사이)은 보간 없이 즉시(또는 이산) 바뀜 | `testNotInterpolated` |
| `@keyframes` 안의 `--x`: 키프레임 3 개, 없는 `from`(기저 값), `forwards` 채움, 채움 없으면 기저 값으로 복귀, `var()` 와 자손 | `testKeyframes` |
| 키프레임의 문법에 맞지 않는 값과 등록되지 않은 속성은 무시 | `testInvalidKeyframeValues` |
| 전환 중단(현재 값에서 다시 시작, `transitioncancel`), 엔진 폐기 | `testInterruptionAndDispose` |

한계: 값이 `<transform-list>`/`<image>` 이면 보간하지 않고 이산이다(보간 함수는 속성 id 가 없어 `transform` 전용 경로를 쓰지 못함). `<length-percentage>` 에서 길이와 백분율이 섞이면 이산이다(calc 로 섞지 않음). 이름의 대소문자는 CSS 키워드 정규화를 따른다(`--X` 같은 대문자 이름은 확인하지 않음). 의사 요소의 사용자 정의 속성은 애니메이션하지 않는다(기존 한계와 같음). 등록된 속성이 `inherits: true` 여도 자손이 이미 계산된 뒤에 부모가 움직이면 `tick()` 이 부모를 다시 계산해 자손도 새 상속 값을 받는다(시험으로 확인).

## 26. CSS Motion Path 1 (`offset-path`, `offset-distance`, `offset-position`, `offset-anchor`, `offset-rotate`, `offset`)

규격: https://www.w3.org/TR/motion-1/ (W3C Working Draft, 5 November 2024, 조회 2026-11-17, 2 절 전체와 3 절 "동등한 경로"를 읽고 구현). 구현: `layout.MotionPath`(경로 만들기, 사용 거리, 점과 방향, 오프셋 변환, 합성), `ClipPaths.shapeFor(box, style, ...)` 오버로드(기본 도형 해석 재사용), `Css.hasTransform`(오프셋 경로가 있으면 변환된 상자: 쌓임 맥락과 fixed/absolute 자손의 포함 블록), `LayoutEngine.applyTransforms`(배치 뒤 `MotionPath.boxTransform`), `Shorthands.Offset`, `Prop` 291~295(`P` 플래그를 풀었음), `offset-path` 문법을 `none | [ <generic-function> || <상자 키워드> ] | <url>` 로 좁힘.

| 동작 | 구현 | 시험 |
| --- | --- | --- |
| `path()`, `circle()`, `ellipse()`, `inset()`/`rect()`/`xywh()`, `polygon()`, 상자 키워드만 | 도형의 동등한 경로(3 절): 원/타원은 가장 오른쪽 점에서 시계 방향, 직사각형은 윗변 왼쪽 끝에서 시계 방향, 다각형/`path()` 는 자신의 점. 기준 상자 = 포함 블록을 만드는 상자의 border box. 곡선은 0.1px 오차로 폄 | `MotionPathLayoutTest.testBasicShapes`, `testPathPlacementAndRotation` |
| `ray(<각도> <크기>? contain? at <위치>?)` | 방위각(0도 위, 시계 방향), `closest-side/closest-corner/farthest-side/farthest-corner/sides`, 위치 없으면 오프셋 위치 → 없으면 포함 블록의 가운데, `contain` 없는 ray 는 거리 제한 없음 | `testRay` |
| `offset-distance` 의 실제 값(2.2.1) | 길이 또는 경로 길이의 백분율. 닫힌 경로는 길이로 나눈 나머지(음수 포함), 열린 경로는 0~길이로 한정 | `testUsedDistance` |
| `offset-position`(2.3) | `auto` 는 상자 자신의 왼쪽 위(그러면 `path()` 가 거기서 시작), `<position>` 은 포함 블록 안의 점(`path()` 의 원점, `at` 없는 원/타원의 중심, ray 의 시작), `normal` 은 없음(`path()` 는 포함 블록의 왼쪽 위가 원점) | `testOffsetPosition` |
| `offset-anchor`(2.4) | `auto` 는 `transform-origin`, 위치는 상자 안의 점 | `testAnchor` |
| `offset-rotate`(2.5) | `auto`(경로의 접선 방향), `reverse`(+180도), `<각도>`, `auto <각도>`; 회전은 놓인 앵커 둘레 | `testPathPlacementAndRotation`, `testRay` |
| 변환 합성(2.7과 CSS Transforms 2) | 개별 속성(`translate rotate scale`) → 오프셋 변환 → `transform` 순서(각각 `transform-origin` 둘레, 개별 속성이 가장 바깥) | `testComposition` |
| `offset` 단축 속성(2.6) | `[ <position>? [ <path> [ <distance> || <rotate> ]? ]? ]! [ / <anchor> ]?` | `testProperties` |
| 그리기와 입력 | 상자는 경로가 놓은 자리와 회전으로 그려지고 그 자리에서 클릭이 찾으며 레이아웃의 자리와 뒤따르는 상자는 그대로 | `MotionPathPaintTest` 2건 |

한계: `<url>`(SVG 도형 참조)은 읽을 수 없어 오프셋 변환이 없다(규격은 `path("m 0 0")` 처럼 동작). `<coord-box>` 키워드는 기준 상자 크기를 `border-box` 로 근사하고(`margin-box` 등을 고르지 않음, 키워드만 있는 경로는 `ClipPaths` 의 상자 해석을 따름), `inset()`/`rect()` 의 `round` 는 둥근 모서리 시작점 규칙(3 절)을 따르지 않는다. 경로를 따라 움직이는 애니메이션(`@keyframes` 의 `offset-distance`)은 일반 속성 보간으로 되지만 `offset-path` 자체의 보간(도형 사이)은 이산이다. `path()` 의 원점 규칙은 규격 본문이 정하지 않아 `offset-position` 이 `normal` 이면 포함 블록의 왼쪽 위로 했다(문서에 명시). 연속한 하위 경로는 이음새 없이 이어 붙여 길이를 더한다. `offset-path` 가 있는 상자의 `position:sticky` 와 표시 방식(`display:inline`)의 상자는 변환되지 않는다(`Css.hasTransform` 을 쓰는 기존 규칙과 같음).

## 27. `hanging-punctuation` (CSS Text 3, https://www.w3.org/TR/css-text-3/#hanging-punctuation-property, CRD 2026-08-14, 조회 2026-11-17)

구현: `layout.InlineLayouter.Flow`(`openingHang`, `closingHang`, `isStopOrComma`, `endsText`, `hangs`, `baseBounds`/`refreshBounds` 의 첫 줄 보정), `Prop.HANGING_PUNCTUATION` 의 "파싱만" 플래그를 풂.

| 키워드 | 동작 | 시험 `HangingPunctuationLayoutTest` |
| --- | --- | --- |
| `first` | 첫 줄 맨 앞의 여는 부호(Ps, Pi, Pf, ASCII 따옴표, 표의 문자 공백 U+3000)가 줄 왼쪽 가장자리 밖에 매달린다: 첫 줄의 왼쪽을 그 글자의 advance 만큼 옮기고 너비를 그만큼 늘리므로 한 글자 모자란 낱말이 첫 줄에 들어간다. 뒷줄은 그대로 | `testFirst` |
| `force-end`, `allow-end` | 낱말(단위)이 줄 끝에서 마침표/쉼표(`, . ، ۔ 、 。 ， ． ﹐ ﹑ ﹒ ｡ ､`)로 끝나면 그 글자 하나는 줄에 맞출 때 세지 않는다(줄이 상자보다 그 글자만큼 길어질 수 있다) | `testEnd` |
| `last` | 텍스트의 마지막 단위가 닫는 부호(Pe, Pf, Pi, ASCII 따옴표)로 끝나면 같은 방식으로 세지 않는다(마지막 단위가 아니면 안 매달림) | `testLast` |
| 상속 | 상속 속성이며 자식이 `none` 으로 끌 수 있음 | `testInheritance` |

한계: 매달린 글자도 줄 안의 조각으로 그려지므로 `text-align: right/center/justify` 는 매달린 글자를 줄 폭에 포함해 계산한다(규격은 정렬과 양쪽 맞춤에서 빼며 `force-end` 의 양쪽 맞춤 때 줄 밖으로 밀림). `allow-end` 와 `force-end` 는 구분하지 않고 둘 다 "맞지 않을 때만" 효과를 낸다(왼쪽 정렬에서는 눈에 보이는 차이가 없음). 세로쓰기와 rtl 은 다루지 않았다. 매다는 글자는 한 줄에 한 글자뿐이다.

## 28. `mask-border` (CSS Masking 1, https://www.w3.org/TR/css-masking-1/#the-mask-border; 아홉 조각 규칙은 CSS Backgrounds 3 의 `border-image` 6.1~6.5)

§3 의 한계("`mask-border*` 는 구현하지 않았다")를 해소했다. 구현: `render.BorderImageLayer`(마스크 레이어; CSS-J 가 `border-image` 를 위해 일반화하며 `MaskBorderLayer` 에서 이름을 바꿨다. §30.2), `MaskLayers.of`(`mask-image` 레이어 위에 맨 위 레이어로 더함, 합성 연산자 add), `Effects.hasMask`(`mask-border-source` 가 none 이 아니면 마스크가 있음), `BackgroundLayers.decodedImage/gradient`(원본 해석), `Shorthands.MaskBorder`(`mask-border` 단축 속성), `Prop` 285~290(파싱만이던 속성), `PropertyRegistry`(미지원 이름에서 뺌).

| 동작 | 구현 | 시험 `MaskBorderPaintTest` |
| --- | --- | --- |
| `mask-border-source` | `none`, 이미지 URL(디코딩된 첫 프레임), 그라디언트(border 이미지 영역 크기로 그린 이미지; 이때 slice 의 숫자는 영역의 화소) | `testRingAndFill`, `testImageSource` |
| `mask-border-slice` | 1~4 값(숫자=원본 화소, 백분율=원본 크기, 이미지보다 크지 않게 한정), `fill` 이면 가운데 조각도 그림 | `testRingAndFill`, `testImageSource` |
| `mask-border-width` | 길이, 백분율(영역 기준), 숫자(border 너비의 배수), `auto`(조각 크기); 반대쪽 합이 영역을 넘으면 같은 비율로 줄임 | `testWidthAndOutset` |
| `mask-border-outset` | 길이 또는 숫자(border 너비의 배수); border box 를 그만큼 넓힌 영역에 그림 | `testWidthAndOutset` |
| `mask-border-repeat` | 가로 가장자리와 세로 가장자리의 `stretch`/`repeat`(가운데 맞춤, 끝은 영역이 자름)/`round`(타일 수를 정수로 맞춰 크기 조정)/`space`(정수 개 사이에 틈, 한 개도 못 들어가면 안 그림) | `testRepeatModes` |
| `mask-border-mode` | `alpha`(기본), `luminance` | `testLuminanceMode` |
| `mask-image` 와 결합 | mask-border 가 맨 위 레이어(add) | `testWithMaskImage` |
| `mask-border` 단축 속성 | `source || slice [ / width? [ / outset ]? ]? || repeat || mode`(순서 자유, 쓰지 않은 개별 속성은 초기화, 빈 너비 칸 허용) | `testProperties` |

한계: ~~`border-image` 자체는 구현하지 않았다~~ → `border-image` 는 CSS-J 가 같은 코드로 구현했다(§30.2; 아래 `space` 반복은 규격 5.6 절에 맞춰 앞·사이·뒤 간격을 같게 고쳤다). 영역 크기에 맞춰 그린 그라디언트 이미지는 매 그리기마다 다시 만든다(큰 영역에서는 느리다; 16 M 화소 초과는 그리지 않음). 쓸 수 없는 원본(아직 도착하지 않았거나 없는 이미지)은 `none` 처럼 마스크가 없는 것으로 그려지며(`mask-image` 가 없으면 내용이 그대로 보임) (시험 `testUnavailableSource`). 이미지가 도착한 뒤 다시 그리는 것은 `mask-image` 와 같은 이미지 로딩 경로를 쓰지만 `mask-border` 로 직접 시험하지는 못했다(미확인). `mask-border-source` 의 SVG `mask` 요소 참조(`url(#id)`)와 `image-set()` 은 쓸 수 없는 원본으로 보아 마스크를 만들지 않는다. 마스크는 히트 테스트에 영향이 없다.

## 29. `initial-letter` (CSS Inline Layout 3, https://www.w3.org/TR/css-inline-3/#initial-letter-styling)

구현: `layout.InitialLetter.adjust`(`BoxBuilder.splitFirstLetter` 가 `::first-letter` 스타일에 적용), `Prop.INITIAL_LETTER`(`normal | <number [0,inf]> [ <integer> | drop | raise ]?`; 효과가 있으므로 플래그 없음). `::first-letter { initial-letter: size sink }` 의 첫 글자를 이미 있는 float 드롭 캡 경로로 보내되 스타일을 다음과 같이 바꾼다: `float: left`, `display: block`, 글꼴 크기 = `((size − 1) × 문단 줄 높이 + 문단의 대문자 높이) ÷ 글자 글꼴의 대문자 비율`(대문자 높이는 `H` 글리프의 높이), `line-height` = `sink × 문단 줄 높이`(상자 높이가 sink 줄), 위쪽 margin = 첫 줄의 대문자 위쪽 위치 − 상자 안의 글자의 대문자 위쪽 위치(음수 가능)와 그 반대 값의 아래쪽 margin(float 가 차지하는 높이가 정확히 sink 줄). `sink` 는 생략하면 `floor(size)`, `drop` 도 그 값, `raise` 는 1, 정수는 그 값. 작성자가 이미 float 나 `none` 이 아닌 `float` 를 준 글자는 건드리지 않는다.

| 동작 | 시험 `InitialLetterLayoutTest` |
| --- | --- |
| `initial-letter: 3` → 글자 상자가 float 이고 높이 3 줄(90px), 첫 세 줄은 글자 오른쪽에서 시작하고 넷째 줄은 문단 왼쪽에서 시작 | `testThreeLines` |
| 글자의 대문자 높이 = 두 줄 + 문단 대문자 높이(±1.5px), 글자와 첫 줄의 대문자 위쪽이 같은 선(±1.5px), 글자의 기준선이 셋째 줄의 기준선(±1.5px) | `testSizeAndAlignment` |
| `3 2`(옆으로 흐르는 줄이 둘), `3 raise`(한 줄), `2.5`(두 줄), `2 drop` | `testSink` |
| `normal`, 1 보다 작은 크기, 올바르지 않은 값은 그대로; 작성자의 `float: left; font-size: 300%` 는 그대로 존중 | `testNotChanged` |

한계: `::first-letter` 에만 적용한다(규격은 인라인 요소 등에도 쓸 수 있음). 세로쓰기와 `initial-letter-align`/`initial-letter-wrap` 은 없다. 대문자 높이는 `H` 글리프로 근사하고, 글자 상자 안의 위치는 반 줄간격과 ascent 로 계산하므로 줄 높이를 정한 문단에서 정밀하고 `line-height: normal` 이면 ascent+descent+leading 으로 근사한다. 첫 글자가 `ComputedStyle.with` 로 바뀐 스타일을 쓰므로 `getComputedStyle(el, '::first-letter')` 는 원래 값(`float: none`)을 보여 준다.

## 시험 실행 결과

- 2026-11-17 `FilterPaintTest` 10 통과(JDK 6u45), `org.duckdns.hjow.swingwebpanel.css` 148 / `layout` 268 / `render`(필터 추가 전 시점) 144 통과, 실패 0.

## 30. P10-08 최종 대조와 CSS-J 구현 (CSS-J, 2026-11-18, 커밋 번호 `20261117-900` 부터)

이 절은 P10-08("기능 표의 CSS3 모듈별 미구현 속성/선택자/규칙을 다시 확인하고 각각 구현/검증")의 마지막 대조이다. 위 §1~§29 와 `docs/progress/layout-f.md` 의 표, `docs/FEATURE_MATRIX.md` 6절의 `부분 구현`/`예외` 행과 그 "제약"을 한 표로 모았고, 각 항목을 **구현**(이번 작업이나 앞선 작업), **명시적 예외**(근거와 함께; 새 예외 ID 는 기능 표 3절의 EXC-09~13), **후속**(구현할 수 있으나 이번에 하지 못함; 미구현으로 남는다) 중 하나로 정했다. 후속이 하나라도 남으므로 이 절은 **P10-08 을 체크하지 않는다**는 결론이다(30.6 의 "체크 후보 판단").

### 30.1 조회한 규격 (도구 `tools/webfetch`, 실행 JRE: Zulu 25, 조회일 2026-11-18)

| 규격 | URL | 판본(`Last-Modified`) | 읽은 곳 |
| --- | --- | --- | --- |
| CSS Backgrounds and Borders Level 3 | https://www.w3.org/TR/css-backgrounds-3/ | 기능 표 CSS-11 의 CRD 2024-03-11(Mon, 11 Mar 2024) | 5 절 Border Images 전체(5.1~5.8), 4.3 Corner Clipping, 3.10 border 단축 속성의 `border-image` 초기화 |
| CSS Containment Module Level 2 | https://www.w3.org/TR/css-contain-2/ | WD 2022-09-17(Sat, 17 Sep 2022) | 3.3 Style Containment(카운터·따옴표 범위, 예제), 4 `content-visibility`(`auto` 가 "사용자에게 관련 있음"인 조건) |
| CSS Fonts Level 4 | https://www.w3.org/TR/css-fonts-4/ | Thu, 12 Nov 2026 | 2.8 `font-synthesis-weight/style/small-caps/position`과 `font-synthesis` 단축 속성의 값 대응 |
| CSS Text Level 3 | https://www.w3.org/TR/css-text-3/ | Fri, 14 Aug 2026 | 6.4 `text-justify`(`auto`/`none`/`inter-word`/`inter-character`, `distribute` 는 옛 값) |

본문을 모두 대조하지는 않았다(읽은 곳만 줄 단위로 대조). 나머지 알고리즘은 이전 작업의 판본과 규격 지식에 따랐다.

### 30.2 이번에 구현한 것

| 기능 | 구현 | 시험 |
| --- | --- | --- |
| **`border-image`**(`-source/-slice/-width/-outset/-repeat`, 단축 속성) | `render.BorderImageLayer`(이전 `MaskBorderLayer` 를 일반화: `mask-border` 와 같은 아홉 조각 코드를 두 속성 묶음 `MASK`/`BORDER` 로 씀), `DisplayListBuilder.decorate`(원본을 쓸 수 있으면 border 스타일 대신 이 레이어를 그리고, 아니면 border 스타일; `border-collapse: collapse` 표에는 적용하지 않음), `DisplayListBuilder.grow`(outset 만큼 경계 확장), `css.props.Shorthands.MaskBorder`(두 단축 속성의 공용 클래스), `PropertyTable`/`PropertyRegistry`(파싱만 표시와 미지원 이름 제거). 규격 5.6 절을 지켜 다시 썼다: 모서리는 영역 크기, 위아래/좌우 가장자리는 영역 두께에 맞추고 길이는 같은 비율, 가운데는 위·왼쪽 가장자리(없으면 아래·오른쪽, 그것도 없으면 1)의 배율; `repeat` 는 첫 타일을 영역 가운데에 놓고(개수는 홀수), `round` 는 개수를 정수로 맞춘 뒤 타일 크기를 다시 정하고, `space` 는 정수 개의 타일을 놓고 남는 공간을 **앞·사이·뒤에 똑같이** 나눔(이전 `mask-border` 구현은 사이에만 나눴다: 규격 5.6 절 본문 "the gap before the first tile, the gap after the last tile, and the gaps between tiles are equalized" 에 맞춰 고쳤고 `MaskBorderPaintTest.testRepeatModes` 의 기대 위치를 바꿨다); 조각은 자기만의 부분 이미지(`getSubimage`)로 그려 이웃 화소와 섞이지 않음; 클립은 `g.setClip` 이 아니라 `g.clip` 으로 더해 조상의 `overflow` 클립이 유지됨; `border-image-width: auto` 는 래스터 원본이면 조각 크기, 그라디언트 원본이면 border 너비(5.3 절); `border-radius` 는 이미지를 자르지 않음(4.3 절); 늦게 도착하는 이미지는 레이아웃/디스플레이 리스트를 다시 만들 때 나타남 | `BorderImagePaintTest` 12(border 대체, 가운데·fill, 조각이 섞이지 않음, 원본 실패 때 border 스타일, width/outset/겹침 축소, slice 값, 반복 방식 네 가지와 너무 큰 타일, 세로 반복과 가운데 타일 배율, 그라디언트 원본, `border-radius` 와 무관함, 조상 클립, 속성과 단축 속성), `BorderImageIntegrationTest` 2(실제 HTTP: 늦게 도착한 이미지가 border 스타일을 대체, 404 이면 border 스타일 유지), `MaskBorderPaintTest` 8 |
| **`font-synthesis`**(단축 속성)와 **`font-synthesis-weight/style/small-caps/position`** | `Prop`(`FONT_SYNTHESIS` → `FONT_SYNTHESIS_WEIGHT`(273), 새 `FONT_SYNTHESIS_STYLE/SMALL_CAPS/POSITION`(304~306), `COUNT` 306), `PropertyTable`, `Shorthands.FontSynthesis`(`none` 혼자이면 넷 모두 none, 나열한 종류는 auto 나머지는 none, 되풀이·모르는 이름은 무효; 규격 2.8.5 의 값 대응), `FontFeatures.mayRealize`(개별 속성을 읽음), `FontResolver.resolve/restrict`(굵게·기울임 비트가 JRE 의 합성으로 만들어지게 되면 뺌: 진짜 면이 있는지는 `Font.getFontName()` 이 보통 면과 다른 이름을 내는지로 알아본다 — JDK 6u45 에서 Arial/Tahoma/Dialog 는 진짜 면, Lucida Console/Impact/Gulim 은 합성임을 확인(`FontProbe`)), `WebFontSource.find(..., synthBold, synthItalic)`/`WebFontSet`(웹 글꼴 면에 굵게/기울임이 없을 때 합성 여부). 합성 금지가 없으면 이전과 같다 | `FontSynthesisTest` 3(웹 글꼴 면, 설치된 패밀리, 레이아웃의 텍스트 상자 글꼴), `ContainmentPropertiesTest`(단축 속성 값 대응) |
| **`text-justify: inter-character`/`distribute`** | `InlineLayouter.buildLine/placeChars/nextCluster/isCjk`: 양쪽 정렬된 줄의 남는 너비를 글자(자소 클러스터: 결합 부호, 서로게이트 쌍, 접합자 열, 한글 자모 열) 뒤마다 똑같이 나눔(줄의 마지막 글자 뒤는 제외, 공백도 글자). `auto` 는 공백이 없는 줄에서 한중일 글자가 둘 이상이면 같은 방식(규격 6.4.3 의 `auto` 예: 낱말 구분자와 한중일 글자 사이를 늘림). `inter-word`/`none` 은 이전과 같다 | `TextJustifyCharacterTest` 4 |
| **`contain: style`**(카운터 범위) | `GeneratedContent.enter/exit/find`: 스타일 격리 요소의 `::before`·자손·`::after` 의 `counter-increment`/`counter-set` 은 그 하위 트리로 한정된다: 안에서 처음 쓰면 바깥에 같은 이름이 있어도 0 에서 시작하는 새 카운터가 만들어지고 요소가 끝나면 사라지며 바깥 카운터를 바꾸지 않는다(`counter()`/`counters()` 는 바깥 카운터도 본다. 규격 3.3 의 예 `1 1.2`). `contain: style/content/strict`, `content-visibility: auto/hidden`, `container-type` 이 모두 스타일 격리이다(`Containment.flags`). **함께 고친 기존 오류**: 의사 요소 자신의 `counter-increment` 가 자신의 `content` 를 평가한 **뒤에** 적용되던 것을 앞으로 바꿨다(`BoxBuilder.pseudoContent/flexPseudoContent`: `p::before{counter-increment:n;content:counter(n)}` 이 이전에는 0 을 보였다) | `StyleContainmentCounterTest` 5(규격 예 `1 1.2`, 증가가 바깥으로 새지 않음, 안쪽 카운터는 0 에서 시작, 의사 요소 카운터 순서, 격리 없는 경우 회귀) |
| **`content-visibility: auto`** | 구현 변경 없음, **결정을 기록**: 규격은 `auto` 요소가 "사용자에게 관련 있으면"(화면 안이거나 사용자 정의 마진 안, 포커스·선택) 내용을 그대로 배치하고 그리며, 레이아웃·스타일·그리기 격리는 관련 여부와 무관하게 유지된다고 정한다(Contain 2 4 절). 이 엔진은 문서 전체를 한 번에 배치하고 레이아웃은 스크롤 위치를 모르므로(`PageView` 는 호스트 `JScrollPane` 이 스크롤을 소유) **사용자 정의 마진을 문서 전체로 정해** `auto` 는 항상 "관련 있음"이다: 세 격리는 `Containment.flags` 가 켜고(위 `contain: style` 포함) 내용은 건너뛰지 않는다. 화면 밖 그림은 그리기 명령의 교차 검사(`PagePainter`)로 이미 생략된다. 건너뜀 상태에서만 의미가 있는 `contain-intrinsic-size` 의 `auto` 기억과 `contentvisibilityautostatechange` 이벤트는 해당 없음 | 기존 `ContainerQueryLayoutTest.testLayoutContainment`, `StyleContainmentCounterTest` |
| **`image-set()`**(`background-image`, `border-image-source`, `mask-border-source` 등 `<image>` 자리) | `render.BackgroundLayers.pickImage`: 후보의 해상도(`x`, `dppx`, `dpi`, `dpcm`; 없으면 1x) 중 장치 화소 비율 1 보다 작지 않은 것 가운데 가장 작은 것, 없으면 가장 큰 것을 고르고(`type()` 은 무시) 이미지/문자열 이름을 `url()` 로 풀며, 고른 해상도로 이미지의 고유 크기를 나눈다(`tileSize`: 2x 이미지는 CSS 픽셀로 절반). 쓸 수 없는 후보를 고르면 아무것도 그리지 않고 다른 후보로 대신하지도 않는다(선택은 해상도로만) | `ImageSetPaintTest` 4(선택 규칙, 해상도 단위와 문자열 이름, 고유 크기, `border-image`, 없는 후보) |
| **`cursor`**(컴포넌트의 마우스 커서) | `core.CssCursors`(키워드 → `Cursor` 대응; 값 끝의 키워드가 정하고 `auto` 는 기존 동작: 링크 위는 손, 선택할 수 있는 텍스트 위는 텍스트 커서), `core.PageInput.requestHover/applyHover`(맞은 상자의 `cursor` 를 읽어 커서가 바뀔 때만 `setCursor`). `cursor: default` 는 링크의 손을 덮고 상속한다. `cursor: none` 은 투명 커서(헤드리스 환경에서 만들 수 없으면 기본 화살표) | `CssCursorIntegrationTest` 3(키워드 대응, 스타일의 키워드와 상속, 실제 컴포넌트에서 마우스 이동에 따른 커서) |

### 30.3 모듈별 대조 표

"확인"은 이 작업에서 코드나 시험으로 직접 확인했는지(`확인`) 아니면 앞선 문서의 기재를 그대로 옮겼는지(`기재`)를 뜻한다.

| 모듈(기능 표 ID) | 구현 안 된 속성/규칙 | 구현 가능 여부(Java2D·JRE 6·엔진 구조) | 결정 | 확인 | 근거 |
| --- | --- | --- | --- | --- | --- |
| Backgrounds 3 (CSS-11) | `border-image` | 가능 | **구현함**(CSS-J) | 확인 | 30.2 |
| Backgrounds 3 (CSS-11) | `background-attachment: fixed/local`(scroll 처럼 그림), `background-clip: text`(그리지 않음), 둥근 모서리의 히트 테스트 | 가능(`fixed` 는 `PaintContext` 의 스크롤 값으로, `text` 는 글리프 윤곽 `TextLayout.getOutline` 으로) | 후속 | 기재 | 기능 표 CSS-11 |
| Images 3/4 (CSS-12) | `cross-fade()`, `element()`, `image()` | `cross-fade` 가능, `element()` 는 요소를 이미지로 그려야 해 큼 | 후속 | 확인 | `render.BackgroundLayers` |
| Images 4 (CSS-12) | `image-set()` | 가능 | **구현함**(CSS-J) | 확인 | 30.2 |
| Fonts 3/4 (CSS-13) | 합자·숫자 모양·대체 형태·동아시아 형태·`font-kerning` 등 OpenType 기능, `font-variation-settings`/`font-optical-sizing`/`font-palette`, `font-feature-settings` 의 효과 | **불가**: 아래 30.4 의 실험 | **명시적 예외 EXC-11** | 확인 | 30.4 |
| Fonts 3/4 (CSS-13) | `font-synthesis-weight/style` 의 굵게/기울임 합성 연결 | 가능 | **구현함**(CSS-J) | 확인 | 30.2 |
| Fonts 3/4 (CSS-13) | WOFF2(Brotli 디코더), EOT, SVG 글꼴, Type 1, 교차 오리진 글꼴(CORS), `font-display` block/fallback/optional 구분, `size-adjust` 등 서술자, `@font-feature-values` | WOFF2 는 자체 Brotli 디코더(큼), 교차 오리진은 P20-08-c 이후 가능, `font-display` 는 작음 | 후속(WOFF2 와 CORS 글꼴이 우선) | 기재 | 기능 표 CSS-13 |
| Text 3/4 (CSS-14) | `hyphens: auto` 의 자동 음절 나눔 | 규격이 허용하는 "언어 자원 없음"으로 처리 | **명시적 예외 EXC-12**(소프트 하이픈 `&shy;` 와 `manual` 은 구현·시험: `HyphensAndBalanceLayoutTest.testBreakAtSoftHyphen`) | 확인 | 30.4 |
| Text 3/4 (CSS-14) | `text-justify: inter-character` | 가능 | **구현함**(CSS-J) | 확인 | 30.2 |
| Text 3/4 (CSS-14) | `line-break: loose/normal/strict` 의 CJK 소형 가나·부호 규칙, `text-wrap: pretty`, `hyphenate-limit-*`, `text-autospace`, `text-spacing-trim`, `hanging-punctuation` 의 정렬·세로쓰기 한계 | 가능(규칙 표가 필요) | 후속 | 기재 | cssg §16·§18·§27 |
| Text Decoration 3/4 (CSS-15) | 세로쓰기 텍스트의 `text-emphasis` | 가능하나 세로 줄 안의 점 공간 확보, 눕힌/똑바로 선 글자별 위치가 필요해 큼 | 후속 | 확인 | `render.EmphasisMarks`, `layout.TextEmphasis`(세로는 그리지 않음) |
| Text Decoration (CSS-15) | `text-decoration-skip-ink`, `text-underline-position`, `text-decoration-thickness` 의 일부 | 가능(글리프 윤곽) | 후속(계산 값만) | 기재 | cssg §12 |
| Writing Modes (CSS-16) | 세로 글리프 형태(`vert`), 인라인 요소의 `writing-mode`, 세로 루트의 오른쪽 붙임 등 | `vert` 는 GSUB 라 불가(EXC-11), 나머지는 가능 | `vert` 는 **EXC-11**, 나머지 후속 | 기재 | 기능 표 CSS-16 |
| Positioned Layout (CSS-01/18) | `position: sticky` 의 스크롤 추종(relative 처럼 배치), 인라인 요소가 포함 블록인 경우, RTL 절대 배치 | sticky 는 가능(그리기 시점의 변환과 같은 변환의 히트 테스트), 나머지는 레이아웃 확장 | 후속 | `sticky` 확인(`LayoutEngine` Javadoc), 나머지 기재 | 기능 표 CSS-18 |
| 스크롤 컨테이너 전반 | `overflow: auto/scroll` 요소 안의 자체 스크롤(자르기만 함: `Css.clips`), `scroll-snap-*`, `scrollbar-*`, `overscroll-behavior`, `scroll-behavior: smooth`, `scroll-state()` 질의, 스크롤 구동 애니메이션(`animation-timeline`), scroll anchoring | 엔진에 스크롤 컨테이너가 없다(스크롤은 호스트 `JScrollPane` 이 소유) | **명시적 예외 EXC-13**(계산 값만) | 확인 | 30.4 |
| Flexbox/Grid/Multicol/Align (CSS-19~22) | `visibility: collapse` 의 접힘, subgrid/masonry(Grid 2), 그리드 `baseline` 정렬, `calc()` 트랙 크기, 다단의 표/플렉스 분할 | 개별 판단 필요 | 후속(subgrid/masonry 는 기능 표 CSS-21 이 이미 Level 2 미지원으로 기록) | 기재 | 기능 표 CSS-19~22 |
| Transforms 2 (CSS-25) | `perspective`, `transform-style`, `backface-visibility`, 3D 함수 | 상단 요구로 불가 | 명시적 예외 EXC-03(기존) | 확인 | 기능 표 3절 |
| Transitions/Animations (CSS-26/27) | `@starting-style`, 되돌림 단축 계수, 의사 요소의 전환·애니메이션, `animation-timeline`, 레이아웃 무관 속성의 부분 다시 그리기 | `@starting-style`·의사 요소는 가능, `animation-timeline` 은 EXC-13 | 후속(`animation-timeline` 은 EXC-13) | 기재 | cssg 21 이전·layout-f |
| UI (CSS-32) | `cursor` 의 Swing 커서 연결 | 가능 | **구현함**(CSS-J: 키워드를 `Cursor` 의 미리 정의된 커서에 대응; `url()` 이미지 커서는 쓰지 않음, Java 6 에 없는 커서(`help`, `not-allowed`, `zoom-in` 등)는 기본 화살표) | 확인 | 30.2 |
| UI (CSS-32) | `resize` 핸들, `appearance`(계산 값만), `user-select` 외 | `resize` 는 핸들 그림과 드래그가 필요 | 후속 | 기재 | 기능 표 CSS-32 |
| Compositing/Filter/Masking/Shapes (CSS-33~36) | `filter: url(#id)`, `clip-path: url()/shape()`, `mask-image: url(#id)`(SVG 요소 참조), `shape-outside` 의 이미지 모양과 `shape-image-threshold`, `backdrop-filter` 의 한계(변환, opacity, 보간) | 가능(인라인 SVG 요소 해석과 알파 임계값) | 후속 | 기재 | 기능 표 CSS-34~36 |
| Logical (CSS-37) | 부모와 쓰기 모드가 다를 때 `inherit` 의 논리 변 | 가능 | 후속(작음) | 기재 | cssg §4 |
| Containment (CSS-38) | `contain: style` | 가능 | **구현함**(CSS-J) | 확인 | 30.2 |
| Containment (CSS-38) | `content-visibility: auto` 의 화면 밖 건너뜀 | 레이아웃이 스크롤을 모름 | **결정**: 규격이 허용하는 사용자 정의 마진을 문서 전체로 둠(항상 관련 있음) | 확인 | 30.2 |
| Containment (CSS-38) | `contain-intrinsic-inline-size/block-size`, `scroll-state()` 질의 | 앞의 것은 가능(작음), 뒤의 것은 EXC-13 | 후속 / EXC-13 | 기재 | cssg §10 |
| Fragmentation (CSS-39) | `box-decoration-break`(파싱만), 쪽 나눔(`page`/`left`/`right`/`recto`/`verso` 의 인쇄 효과) | `box-decoration-break` 는 가능(인라인 조각의 border 를 줄마다), 인쇄 매체는 없음 | `box-decoration-break` 후속, 쪽 나눔은 **불가: 인쇄 매체 없음**(`@page` 도 쪽 상자를 배치하지 않음; 기능 표 CSS-44) | 기재 | cssg §5·§19 |
| Pseudo-Elements 4 (CSS-42) | `::file-selector-button`, `::backdrop`, `::highlight()`, `::target-text`, `::spelling-error`, `::grammar-error`, `::cue`, `::slotted`/`::part`(Shadow DOM 없음: HTML5 2014 이후 규격으로 기능 표 확장 목록) | 일부 가능 | 후속(`::slotted/::part` 는 Shadow DOM 이 있어야 하므로 범위 밖) | 기재 | cssg §7 |
| Font Loading/Will Change (CSS-43) | `will-change` 의 효과 | 효과 없음이 규격상 올바름(최적화 힌트) | 구현 대상 아님(계산 값) | 기재 | 기능 표 CSS-43 |
| View Transitions (CSS-44) | `view-transition-name`, `::view-transition-*`, `document.startViewTransition`, `@view-transition` | 이전/이후 상태를 합성 계층에 잡아 의사 요소 트리로 애니메이션하는 구조가 필요(이 엔진은 페이지를 디스플레이 리스트 하나로 컴포넌트에 그리고, 이동은 비동기 페이지 교체라 이전 문서의 스냅샷을 쥐고 있지 않음) | **명시적 예외 EXC-10**(`document.startViewTransition` 은 정의하지 않아 기능 검사가 거짓이 됨: `grep` 으로 확인) | 확인 | 30.4 |
| Speech (CSS-44) | `speak`, `voice-*`, `pause-*`, `rest-*`, `cue-*`, `@media speech` | 음성 합성 공개 API 가 JRE 6 에 없음 | **명시적 예외 EXC-09** | 확인 | 30.4 |
| Motion Path/Web Animations (CSS-44) | Motion Path `url()` 도형, `inset(... round)` 의 시작점 규칙, `coord-box`, `element.animate()`/`getAnimations()` | JS API 는 가능 | 후속(JS 연결) | 기재 | cssg §26 |
| Anchor Positioning 1 (CSS-46, 신규) | `anchor-name`, `position-anchor`, `anchor()`, `anchor-size()` 의 기본 형태 | 가능 | **구현함**(CSS-J, 30.5) | 확인 | 30.5 |
| Anchor Positioning 1 (CSS-46) | `position-area`, `position-try-*`/`@position-try`, `position-visibility`, `anchor-scope`, `anchor-center`, `calc()` 안의 `anchor()`, 암묵 앵커, 인라인 앵커, 상대 위치·스크롤 보정 | 가능(`position-area` 는 격자 계산, `position-try` 는 넘침 검사와 재배치) | 후속 | 확인 | 30.5 |
| CSSOM View (CSS-45) | `scrollTo({behavior})` 의 부드러운 이동 등 | EXC-13 | EXC-13 | 기재 | 기능 표 CSS-45 |
| 나머지 행(CSS-02~10, 17, 23, 28~31, 41) | 파싱·캐스케이드·계산 값은 P08 에서 검증, 화면 효과는 P09/P10 | — | 기능 표가 `부분 구현`인 것은 위 항목의 한계를 합친 것(CSS-23 의 표 행 카운터와 중첩 따옴표 한계는 후속, CSS-41 의 `revert-layer` 는 레이어 단위가 아님·`<meta name=color-scheme>` 미지원은 후속) | 기재 | 기능 표 |

### 30.4 명시적 예외(기능 표 3절 EXC-09~13)와 실험

EXC-01~08 은 요구사항 원문에 근거한 예외이고 **EXC-09~13 은 구현 구조/JRE 6 의 한계에 근거한 예외**이다(요구사항 때문이 아님). 예외 모듈의 선언은 구문과 계산 값은 있으나(있는 이름만) 화면 효과가 없다.

| ID | 대상 | 근거(확인한 것) | 처리 |
| --- | --- | --- | --- |
| EXC-09 | CSS Speech Level 1 | JRE 6 의 공개 API 에 음성 합성이 없다(`javax.speech` 는 JRE 에 포함되지 않음). 이 컴포넌트에는 소리 전용 렌더링 대상이 없다 | 속성 이름을 알아보지 못해 선언은 무시된다. `@media speech` 는 맞지 않는다 |
| EXC-10 | CSS View Transitions Level 1 | 합성 계층(이전/이후 스냅샷을 잡아 의사 요소 트리로 애니메이션)과 문서 전환 모델이 없다: 페이지는 한 디스플레이 리스트로 호스트 컴포넌트에 그려지고 이동은 비동기 페이지 교체이다. `grep -rn "startViewTransition\|view-transition" src` 결과 없음 | `document.startViewTransition` 정의 안 함(기능 검사가 거짓이므로 페이지가 일반 갱신으로 되돌아간다), `view-transition-name` 은 알 수 없는 속성 |
| EXC-11 | OpenType 기능과 가변 글꼴(합자 `liga/clig/dlig`, 숫자 `tnum/onum/lnum/zero`, 대체 `hist/salt/ssNN/swsh`, 동아시아 `jp78~`·`vert`, 커닝 제어, `font-variation-settings`, `font-optical-sizing`, `font-palette`, `font-feature-settings` 의 효과) | JRE 6u45(Windows)에서 `Font.deriveFont(Map)` 의 `TextAttribute.KERNING_ON`/`LIGATURES_ON` 을 시험했다(`build/FontProbe3.java`, 글꼴 10 종): **커닝은 옛 `kern` 표가 있는 글꼴(Verdana, Palatino Linotype)에서만 너비가 바뀌고 Consolas 는 너비 0 을 돌려주며, 합자는 `fi fl ffi` 의 글리프 수가 하나도 바뀌지 않았다**(Palatino Linotype 만 너비가 108.20 에서 105.31 로 달라졌으나 글리프 수는 그대로여서 합자 치환으로 확인하지 못했다). GSUB/GPOS 기능을 켜고 끄는 공개 API 가 없고 가변 글꼴 축 API 도 없다 | 해석한 기능 태그는 `FontFeatures` 가 보관하고, 이 엔진이 직접 재현하는 것만 효과가 있다(작은/petite 대문자, 위/아래첨자, 전각 형태, 합성 굵게·기울임) |
| EXC-12 | `hyphens: auto`(자동 음절 나눔) | 언어별 하이픈 패턴(Liang 사전)이 필요한데 번들할 자료와 `lang` 별 자원이 없다. 규격은 언어에 맞는 자원이 없으면 자동 하이픈을 넣지 않는 것을 허용한다(Text 3 하이픈 절) | `auto` 는 `manual` 처럼 동작한다: 소프트 하이픈(U+00AD)과 `hyphens: none` 은 구현(cssg §16) |
| EXC-13 | 스크롤을 소유해야 하는 기능(`scroll-snap-*`, `scrollbar-width/color/gutter`, `overscroll-behavior`, `scroll-behavior: smooth`, `scroll-state()` 질의, `animation-timeline` 의 scroll()/view(), scroll anchoring, 요소 안의 `overflow: auto/scroll` 스크롤) | 스크롤바와 스크롤 위치는 호스트 `JScrollPane` 이 소유하며 이 컴포넌트에는 자체 스크롤 컨테이너가 없다(`layout.Css.clips`: `overflow` 가 `visible` 이 아니면 자르기만 한다). 호스트의 스크롤바를 컴포넌트 안에서 꾸밀 수 없다 | 계산 값만 있다(`ScrollPropertiesTest`) |

### 30.5 CSS Anchor Positioning Level 1(`anchor-name`, `position-anchor`, `anchor()`, `anchor-size()`)의 기본 형태

규격: https://www.w3.org/TR/css-anchor-position-1/ (W3C Working Draft, `Last-Modified: Fri, 27 Mar 2026 22:59:30 GMT`, 조회 2026-11-18, 도구 `tools/webfetch`, 읽은 곳: 2.1~2.5 앵커와 대상 앵커 요소 찾기, 3.2 `anchor()`와 3.2.1 풀리는 조건, 5.1 `anchor-size()` 일부). 기능 표에 새 행 CSS-46 을 더했다.

구현: `layout.AnchorPositioning`, `layout.AbsoluteLayouter.layoutList`(절대 배치 상자마다 배치 직전에 `a.style` 을 앵커 함수가 길이로 바뀐 스타일로 교체하면 나머지 배치 알고리즘은 그대로이다), `css.values.Terminals`(문법 항목 `<anchor-function>`: `anchor()`/`anchor-size()` 함수만 받는다), `css.props.PropertyTable`/`Prop`(`anchor-name`(307), `position-anchor`(308), 오프셋 속성 8 개와 `<size>`/`<max-size>` 문법에 `<anchor-function>` 추가, `COUNT` 308). 논리 오프셋(`inset-block-start` 등)은 계단식 단계에서 물리 속성으로 옮겨지므로 함께 동작한다.

| 기능 | 동작 | 시험 `AnchorPositioningLayoutTest`(9) |
| --- | --- | --- |
| `anchor-name`(목록) | 그 요소의 주 상자(블록, 인라인 블록, 치환, 표 등 상자; 인라인 비치환 요소와 의사 요소는 제외)가 앵커 | `testTopAndLeft` |
| `position-anchor: --이름` | 이름 없는 `anchor()`/`anchor-size()` 의 기본 앵커. `normal`/`none`/`auto` 는 기본 앵커 없음(암묵 앵커 미구현) | `testTopAndLeft`, `testUnresolvableFunctions` |
| `anchor()` in `top/right/bottom/left` | 포함 블록(padding box)의 그 변을 앵커 상자의 변에 맞추는 오프셋: 변 `top/right/bottom/left`(다른 축의 변은 풀 수 없음), `center`, 백분율, `inside`/`outside`(속성의 같은/반대쪽 변), `start`/`end`/`self-start`/`self-end`(가로쓰기·왼쪽에서 오른쪽에서만) | `testTopAndLeft`, `testBottomAndRight`, `testPercentagesAndKeywords` |
| `anchor-size()` | 앵커 상자의 `width`/`height`/`inline`/`block`(`self-` 포함)을 길이로: `width`, `height`, `min-*`, `max-*` 와 오프셋 | `testAnchorSize` |
| 풀 수 없을 때(앵커 없음, 다른 축의 변, 기본 앵커 없음) | 대체값(길이나 백분율)으로 계산되고, 없으면 `auto`(최대 크기는 `none`), 곧 정적 위치 | `testUnresolvableFunctions` |
| 대상 앵커 요소(2.3) | 이름이 같은 것 중 배치 상자의 가장 가까운 조상(포함 블록 자신까지), 없으면 포함 블록 하위 트리(초기 포함 블록이면 문서 전체)에서 트리 순서의 마지막 것. 배치 상자 자신과 그 안, 아직 배치되지 않은 배치 상자(소스 순서에서 자신과 그 뒤)와 그 안은 앵커가 될 수 없고, 흐름 안의 앵커는 트리에서 상자 뒤에 있어도 된다 | `testTargetAnchorElement`, `testInitialContainingBlock` |
| 속성 문법 | `anchor-name: none \| <dashed-ident>#`, `position-anchor: normal \| none \| auto \| <dashed-ident>`, 오프셋·크기 속성은 `anchor()`/`anchor-size()` 만 함수로 받고 다른 함수와 다른 속성의 `anchor()` 는 무효 | `testProperties` |

**구현하지 않은 것(숨기지 않는다)**: `position-area`, `position-try-*`/`@position-try`/`position-visibility`(넘침 관리와 대체 위치), `anchor-scope`, `anchor-center`, `calc()`/`min()`/`max()` 안의 `anchor()`, `position-anchor: auto`/`match-parent`(암묵 앵커), 인라인 상자 앵커, 배치 상자의 포함 블록 하위 트리 밖의 앵커(포함 블록이 초기 블록이면 문서 전체이므로 그 경우는 된다), `position: relative` 오프셋과 `transform` 으로 옮겨진 앵커의 보정, 스크롤에 따른 보정(3.3: 이 엔진은 스크롤 위치를 레이아웃이 모른다 — EXC-13), 계산 값의 해석(`getComputedStyle().top` 은 함수 그대로이고 픽셀로 풀린 값이 아니다), 앵커 이동에 따른 전환/애니메이션. `anchor()` 의 `em` 단위 대체값은 풀지 않는다(대체값은 px/% 만 확인했다).


### 30.6 체크 후보 판단(P10-08)

**P10-08 은 체크하지 않는다**(체크 후보 아님). "모든 CSS 모듈이 구현 또는 근거 있는 명시적 예외로 확정되었는가"에 대한 답은 아니오이다:

- **확정된 것**: 30.3 표의 "구현함"(이번 작업: `border-image`, `font-synthesis-weight/style`, `text-justify: inter-character`(와 `auto` 의 한중일 글자), `contain: style`, `image-set()`, `cursor`, Anchor Positioning 기본 형태; 앞선 작업: §1~§29)과 "명시적 예외"(EXC-09~13 과 기존 EXC-03; `content-visibility: auto` 는 규격이 허용하는 사용자 정의 마진을 문서 전체로 정한 결정).
- **여전히 후속(구현할 수 있으나 하지 못한 것; 미구현으로 남는다)**: `position: sticky` 의 스크롤 추종, `background-attachment: fixed/local`, `background-clip: text`, `cross-fade()`/`element()`, WOFF2·CORS 글꼴·`font-display` 구분, `line-break: loose/normal/strict` 의 CJK 규칙·`text-wrap: pretty`, 세로쓰기 텍스트의 `text-emphasis`, `text-decoration-skip-ink`, `box-decoration-break`, `@starting-style`·의사 요소 전환, `resize` 핸들, `filter/clip-path/mask` 의 SVG 요소 참조(`url(#id)`), `shape-outside` 이미지 모양, `::file-selector-button`/`::backdrop`/`::highlight()` 등, `@font-feature-values`, Anchor Positioning 의 `position-area`·`position-try-*`·`anchor-scope`, `contain-intrinsic-inline-size/block-size`, Motion Path 의 일부와 `element.animate()`.
- 따라서 P10 단계 전체(P10-08 에 달려 있음)도 체크 후보가 아니다. 위 후속 중 우선순위가 높은 것은 `sticky`, `background-clip: text`, `line-break: strict` 와 CJK 정렬, `box-decoration-break`, `position-area` 순이다(사용 빈도와 구현 난이도 기준의 추정이다).

### 30.7 변경 파일, 시험 결과, 한계, 조정자 요청(CSS-J, 커밋 `20261117-900` ~ `20261117-909`)

**변경 파일**
- 제품: `render/{BorderImageLayer(MaskBorderLayer 의 이름을 바꾸고 일반화), MaskLayers, DisplayListBuilder, BackgroundLayers}`, `css/props/{Prop, PropertyTable, PropertyRegistry, Shorthands}`, `css/values/Terminals`, `layout/{FontFeatures, FontResolver, WebFontSource, WebFontSet, InlineLayouter, GeneratedContent, BoxBuilder, AbsoluteLayouter, AnchorPositioning(새)}`, `core/{CssCursors(새), PageInput}`.
- 시험(새 42 건): `render/{BorderImagePaintTest 12, ImageSetPaintTest 4}`, `core/{BorderImageIntegrationTest 2, CssCursorIntegrationTest 3}`, `layout/{FontSynthesisTest 3, TextJustifyCharacterTest 4, StyleContainmentCounterTest 5, AnchorPositioningLayoutTest 9}`; 고친 시험: `render/MaskBorderPaintTest.testRepeatModes`(`space` 의 기대 위치를 규격 5.6 절에 맞춤), `css/ContainmentPropertiesTest`(`font-synthesis` 가 단축 속성이 됨).
- 문서: 이 파일, `docs/FEATURE_MATRIX.md`(CSS-11/12/13/14/23/32/38/40/44 행 보강, CSS-46 행과 EXC-09~13 행 신설, 3절 머리글).

**시험 실행 결과**(모두 JDK 6u45, 이 PC)
- 단계별(작업 트리의 증분 컴파일): `render` 252, `css` 313, `layout` 384, `core` 373, `org.duckdns.hjow.swingwebpanel.ContentIntegrationTest` 8, `SwingRenderingTest` 10 — 모두 0 실패. (`js` 접두사 전체 실행은 JRE 6u45 의 `fontmanager.dll` 접근 위반으로 JVM 이 죽었다: 크래시 스택은 `js.host.canvas.CanvasText.layout`(`Context2D.measureText`) → `Font.layoutGlyphVector` 로 CANVAS-B 의 영역이며 이 작업의 변경과 무관하다. 이 작업이 `js` 에 영향을 줄 수 있는 `js/dom/StyleObject`(`Prop.COUNT` 사용)는 아래 전체 회귀가 확인한다.)
- **전체 회귀**(`build/fullrun.sh`: HEAD 스냅샷에서 `test.sh`): `=== 3345 passed, 0 failed, 12 skipped (600909 ms) ===`(건너뜀 12 는 선택적 Opus 원본 벡터), 이어서 `[OK] runner exit-code self-check passed`, 종료 코드 0. 이 작업 전 master 의 통합 전체가 3303 통과였고 새 시험 42 건이 더해진 값과 같다. 소스 규칙 점검 `sh tools/sourcecheck/run.sh` 는 2157 파일 0 위반이다(제품 JAR 은 JDK 6u45 로 만들어졌고 `JarContentTest` 도 통과)

**남은 한계(숨기지 않는다)**
- `border-image`: 합쳐진 border 표는 대상 아님(규격도 정의하지 않음), SVG 원본의 slice 는 디코딩한 래스터 크기 기준(벡터 좌표 기준의 시험 없음), 히트 테스트와 스크롤 영역에 outset 이 기여하지 않음(규격 5.4 가 그렇게 정함). `mask-border` 의 `space` 는 규격 5.6 절의 같은 간격 규칙으로 바뀌었다(동작 변경).
- `font-synthesis`: 설치된 패밀리의 진짜 면 판정은 `Font.getFontName()` 비교라는 경험적 방법이다(JDK 6u45 의 Windows 에서 Arial/Tahoma/Dialog 는 진짜, Lucida Console/Impact/Gulim 은 합성으로 확인했고 다른 글꼴과 다른 OS 는 확인하지 못했다). 웹 글꼴은 서술자의 굵기/스타일로 판정해 정확하다.
- `text-justify`: 줄에 공백이 없을 때만 `auto` 가 한중일 글자 사이를 늘인다(공백이 있는 한국어 줄은 공백만 늘임). 양방향 줄과 세로쓰기 줄은 이전과 같다(양방향은 정렬 안 함).
- `contain: style`: 따옴표 깊이는 이 엔진에 중첩이 없어 해당 없음, 암묵 `list-item` 카운터는 격리하지 않음(`list-item` 번호는 별도 번호 매기기이다).
- `image-set()`: 장치 화소 비율은 항상 1(HiDPI 연동 없음), `list-style-image`/`content` 의 이미지에는 연결하지 않음.
- `cursor`: `url()` 이미지 커서를 쓰지 않으며 Java 6 에 같은 커서가 없는 키워드는 기본 화살표, `cursor: none` 은 헤드리스 환경에서 기본 화살표이다. 실제 마우스로 커서 모양을 눈으로 확인하지는 못했다(시험은 `Component.getCursor()` 의 종류를 읽는다).
- Anchor Positioning: 30.5 의 "구현하지 않은 것".
- 규격 본문을 줄 단위로 모두 대조하지는 못했다(30.1 에 읽은 곳을 적었다).

**조정자 요청**
1. `docs/FEATURE_MATRIX.md` 13절의 축별 개수표(CSS 표시 14/29/2/0)를 갱신해 주세요: 새 행 CSS-46(부분 구현), 명시적 예외 EXC-09~13(예외 열; 요구사항이 아니라 구조/JRE 6 한계에 근거함을 3절 머리글과 각 행에 적었다)과 CSS-11/12/32 의 변화를 반영한다.
2. `docs/ARCHITECTURE.md`: `render.BorderImageLayer`(border-image 와 mask-border 가 공유하는 아홉 조각 레이어), `render.BackgroundLayers.pickImage`(image-set), `layout.AnchorPositioning`(절대 배치 직전에 anchor 함수를 길이로 바꾼 스타일로 교체), `core.CssCursors`(CSS cursor → Swing 커서), `layout.GeneratedContent` 의 스타일 격리 스택, `layout.FontResolver.restrict`(font-synthesis)를 새 절/표에 추가해 주세요.
3. AGENTS.md P10-08 과 P10 은 체크하지 않는다(30.6). P10-08 의 남은 후속을 새 하위 항목(예: `P10-08-a` `sticky`, `-b` `background-clip: text`, `-c` `position-area` 등)으로 나누어 추적하는 것을 권한다.
4. 기존 `StaticStateAuditTest`(정적 상태 점검)에는 이 작업이 새 정적 상태를 만들지 않았다(`CssCursors` 는 정적 상태 없음, `core`/`layout`/`render` 의 새 클래스도 마찬가지).
5. `docs/progress/cssg.md` §3 의 "mask-border 미구현" 문구와 §28 의 "border-image 미구현"은 이 절로 대체된다(§28 에 표시를 남겼다).

## 31. P10-08 후속 마무리 (CSS-K, 2026-11-18, 커밋 번호 `20261117-920` 부터)

§30.6 의 "후속"(구현할 수 있으나 하지 못한 것) 중 우선순위 순으로 구현했다. 규격은 `tools/webfetch`(실행 JRE: Zulu 25, 조회일 2026-11-18)로 읽었다: CSS Text Module Level 3 https://www.w3.org/TR/css-text-3/ (`Last-Modified: Fri, 14 Aug 2026`; 5.3 `line-break` 의 엄격도 규칙 전체), CSS Anchor Positioning Level 1 https://www.w3.org/TR/css-anchor-position-1/ (`Fri, 27 Mar 2026`; 3.1 `position-area`와 4.1 기본 정렬 전체). 나머지(CSS Positioned Layout 3 3.4 sticky, Backgrounds 3 3.9, Text Decoration 4, Transitions 2, Fragmentation 3 5.3, CSS Fonts 4 4.3)는 규격 지식에 따랐고 본문을 줄 단위로 대조하지는 않았다.

### 31.1 항목별 상태

| 항목 | 상태 | 구현 | 시험(JDK 6u45) |
| --- | --- | --- | --- |
| `position: sticky` | **구현** | `layout.StickyConstraint`(제약: 마진 상자, 포함 블록 콘텐츠 상자, 오프셋. 시작 가장자리가 이김), `LayoutEngine.applySticky`(배치 뒤 제약 계산), `BoxBuilder`(sticky 는 쌓임 맥락), `PaintWalker`/`PaintSequence`/`DisplayListBuilder`(sticky 그룹 `StickyGroup`; 히트 테스트가 같은 이동량을 씀), `render.PagePainter.paintStickyGroup`(그릴 때 뷰포트 사각형으로 이동량 계산), `PaintContext.viewport`, `DisplayList` 의 뷰포트 크기 | `StickyPaintTest` 10(머리글 따라 붙음, 포함 블록 끝에서 멈춤, 아래쪽 오프셋, 가로, 따라가는 절대 배치 자손, 호스트가 이미 스크롤한 그래픽스, 히트 테스트, 움직이지 않는 경우, 축 알고리즘), `StickyIntegrationTest` 1(실제 컴포넌트 + `JScrollPane` + `JViewport` 스크롤) |
| 실제 컴포넌트의 `position: fixed` 고정 | **결함을 고침**(새 발견) | `PageView.paint` 가 JScrollPane 이 이미 스크롤한 그래픽스에 그리면서 스크롤을 0 으로 알려 fixed 상자가 페이지와 함께 스크롤되던 것을 고쳤다: `PagePainter.paint(..., viewX, viewY)` 로 뷰포트 위치를 따로 전달. `SwingWebPanel.updateViewport` 는 fixed/sticky 가 있는 페이지(`DisplayList.isScrollDependent()`)가 스크롤되면 전체를 다시 그린다(JViewport 의 픽셀 복사 스크롤이 남기는 자국 방지) | `StickyIntegrationTest`(fixed 파란 상자가 두 번 스크롤해도 같은 자리), `StickyPaintTest.testFixedBoxWithHostScroll` |
| `background-attachment: fixed` | **구현** | `BackgroundLayers.tiles`(타일 기하를 정적 함수로 뽑음), `BoxCommands.FixedLayer`(그릴 때 `ctx.viewport` 로 계산; 뷰포트를 모르면 `scroll` 처럼), 루트/body 의 캔버스 배경에도 적용 | `BackgroundAttachmentPaintTest` 3 |
| `background-attachment: local` | **구현**(엔진에 스크롤하는 상자가 없으므로 정적 범위) | 내용을 자르는 상자는 `DisplayListBuilder.contentExtent` 로 넘치는 콘텐츠까지 늘린 padding box 를 위치 기준으로 쓴다 | `testLocalBackgroundCoversTheScrollableArea` |
| `background-clip: text`, `-webkit-background-clip: text` | **구현** | `render.TextClipBackground`(배경 버퍼와 글자 마스크 버퍼를 `DstIn` 합성; 글자 마스크는 `DisplayListBuilder.textMask` 가 페이지 글자와 같은 명령을 불투명한 색으로 만듦: 글리프, 간격, 장식이 같음), 인라인 요소의 글자는 `LayoutBox.isTextOfInline` 으로 찾음, 배경색도 마지막 레이어가 text 이면 글자로 자름 | `testBackgroundClippedToTextOfABlock`, `testGradientTextOfAnInlineElement`(좌우 그라디언트 글자) |
| `line-break: loose/normal/strict/auto`의 CJK 줄바꿈 | **구현** | `layout.CjkLineBreak`(규격 5.3 이 요구하는 규칙: normal/auto 는 U+301C, U+30A0 앞 허용, strict 는 금지, loose 는 작은 가나, 장음 부호, 반복 부호, IN, 가운뎃점 문장 부호, 접미사 앞, 접두사 뒤, 한자 뒤 하이픈 앞 허용; 여는 괄호 뒤는 어느 단계도 금지). **JRE 6u45 의 `BreakIterator.getLineInstance` 가 작은 가나와 닫는 괄호 앞을 이미 금지함을 실험으로 확인했고(그래서 JRE 기본이 strict 에 해당) 그 위에 단계별 차이를 얹었다** | `LineBreakStrictnessLayoutTest` 3(작은 가나, 물결 대시와 장음 부호, 괄호) |
| `text-wrap: pretty` | **구현**(근사) | `InlineLayouter.prettyShrink`: 마지막 줄이 너비의 25% 보다 짧으면 끝에서 둘째 줄만 최대 40% 까지 좁혀(`Flow.shrinkLine/shrinkTo`) 줄 수를 늘리지 않고 단어를 내림. `stable` 은 normal 과 같고 `balance` 는 기존 | `testPrettyAvoidsAnOrphan` |
| `text-decoration-skip-ink` | **구현** | `TextRenderer.skipInk`(글리프 윤곽 `GlyphVector.getOutline` 을 선 두께만큼 넓힌 영역을 선 띠에서 빼고 클립으로 그림; 복합 문자 체계는 윤곽을 만들지 않음) | `TextDecorationInkPaintTest.testDescendersInterruptTheUnderline` |
| `text-underline-offset`, `text-underline-position: under` | **구현** | `DecorationExtras`(길이/백분율 거리, descent 아래) | `testUnderlineOffset`, `testUnderlinePositionUnder` |
| `box-decoration-break: slice`/`clone` | **구현**(인라인 조각) | `InlineLayouter.Flow.placeLtr`/`clonesEdges`: clone 이면 이어지는 조각이 시작 가장자리를, 줄 끝에서 열린 요소가 끝 가장자리를 가진다. slice 는 기존 | `BoxDecorationBreakLayoutTest` 3. **남음**: 다단 조각(블록)의 clone, 오른쪽에서 왼쪽 줄, 줄바꿈이 가장자리 너비를 확보하지 않음 |
| `@starting-style` | **이미 구현**(커밋 `20261117-871`, CSS-ANIM; §30 의 후속 표기는 낡았다) | `css.cascade.RuleIndex`, `AnimationEngine` | `StartingStyleTest` |
| `transition-behavior: allow-discrete`, `display`/`content-visibility` 의 이산 전환 | **구현** | `Prop.TRANSITION_BEHAVIOR`(309), `transition` 단축 속성이 `allow-discrete` 를 받음(`Shorthands.Transition`), `AnimationEngine`(보간할 수 없는 값은 절반에서 바뀌고, display/content-visibility 는 `none` 이 아닌 값을 전환 내내 유지해 퇴장 애니메이션이 가능) | `DiscreteTransitionTest` 5 |
| 의사 요소 전환(`::before`/`::after` 등) | **구현**(전환만; 키프레임 애니메이션은 아님) | `StyleAnimator.animatePseudo`, `StyleResolver.getPseudoStyle`, `AnimationEngine.animatePseudo`(의사 요소별 상태), 이벤트의 `pseudoElement` | `PseudoElementTransitionTest` 3 |
| `position-area`(Anchor Positioning 1) | **구현**(기본 형태) | `Prop.POSITION_AREA`(310), `AnchorPositioning.Area/area/tracksOf`(3x3 격자, 단일 키워드 규칙, 모호한 키워드 배정, 영역별 기본 정렬, 영역을 넘으면 포함 블록 안으로 되돌림), `AbsoluteLayouter.layoutList` 연결 | `PositionAreaLayoutTest` 5 |
| 교차 오리진 웹 글꼴(CORS) | **구현** | `PageResources.fetch`: 글꼴을 CORS 모드로 가져온다(교차 오리진이면 `Origin`, 쿠키는 같은 오리진에만; 응답이 문서의 오리진을 허용해야 사용). P20-08-c 의 소켓 클라이언트가 `Origin` 을 보낼 수 있게 했으므로 "교차 오리진 글꼴 미지원" 한계가 해소됨 | `WebFontIntegrationTest` 3(허용 없음 = 요청하지만 쓰지 않음, 허용 = 사용) |
| 세로쓰기 텍스트의 `text-emphasis` | **남음**(이번에 구현하지 않음) | 설계 메모: `render.EmphasisMarks` 가 가로 줄만 그린다. 세로쓰기는 줄 상자의 점 공간 확보(오른쪽, 쓰기 모드별)와 눕힌/똑바로 선 글자마다 점 위치를 정해야 하며 `layout.TextEmphasis` 의 점 공간이 줄 높이에만 더해진다 | — |
| `resize` 핸들 | **남음**(이번에 구현하지 않음) | 설계 메모: ① `LayoutResult` 에 `resize != none` 이고 내용을 자르는 상자 목록을 캐시, ② `DisplayListBuilder.decorate` 가 padding box 오른쪽 아래에 15px 핸들을 그림, ③ `PageInput.sendMouse`(mousedown) 에서 핸들을 누르면 끌기 상태를 시작하고(DOM mousedown 은 보내지 않음) `mouseDragged` 가 인라인 `style` 의 `width`/`height` 를 바꾼다(`horizontal`/`vertical`/`both`/`block`/`inline` 제한), ④ 호버 커서는 `se-resize`. 프레임·끌어다 놓기·포인터 캡처와 얽힌 입력 경로라 별도 항목으로 시험과 함께 하는 것이 안전하다 | — |
| `shape-outside` 이미지 모양 + `shape-image-threshold` | **남음**(이번에 구현하지 않음) | 설계 메모: `layout.FloatShapes.areaOf` 는 이미지(`url()`)를 `none` 으로 다룬다. 이미지를 모양으로 쓰려면 ① 스타일시트 수집 단계가 `shape-outside` 의 `url()` 도 리소스로 요청하고, ② 레이아웃이 디코딩된 알파(`ReplacedResolver` 같은 조회)에 접근하고, ③ 알파 > 임계값인 행을 `java.awt.geom.Area` 로 이어(큰 이미지는 축소) 모양으로 쓴다 | — |
| `filter`/`clip-path`/`mask` 의 SVG 요소 참조(`url(#id)`) | **남음**(조사하지 않음: 미확인) | 인라인 `<svg>` 요소 모델(`render.InlineSvg`)과 `media.image.svg` 의 필터 프리미티브/`clipPath`/`mask` 지원 범위를 확인하지 못했다 | — |
| Anchor Positioning 의 `anchor()` 의 `calc()` 안 사용, 암묵 앵커, `position-try-*`, `anchor-scope` | **남음** | §30.5 | — |
| WOFF2 | **남음**(이번에 구현하지 않음) | 필요한 설계: ① 순수 Java Brotli 디코더(RFC 7932). 정적 사전 122,784 바이트가 필요하며 RFC 부록의 16진 덤프를 `tools/` 의 변환 도구로 읽어 리소스 파일로 만들어 JAR 에 넣는다(네이티브/외부 JAR 없음), ② WOFF2 컨테이너(`woff2` 헤더, 테이블 디렉터리의 UIntBase128, 변환된 `glyf`/`loca`/`hmtx` 의 복원: 삼중 인코딩된 점 좌표, 복합 글리프, 명령 스트림), ③ 복원한 sfnt 를 기존 `layout.Woff`/`WebFontSource` 경로(`Font.createFont`)에 연결. 크기가 커서 단독 작업 항목으로 나누는 것이 맞다 | — |
| `font-display` 의 block/fallback/optional 구분 | **남음** | 지금은 항상 swap 처럼 다음 패밀리를 먼저 쓴다. **시도하고 되돌렸다**: optional(첫 요청에서 바로 없으면 이번 페이지에서 쓰지 않음)을 `WebFontSet` 에 구현하면, 글꼴을 페이지마다 비동기로 가져와 첫 조회는 캐시된 글꼴이라도 항상 "아직 없음"이므로 optional 글꼴을 절대 쓰지 못하게 된다(브라우저는 약 100 ms 의 유예 안에 도착하면 쓴다). 유예 시간(첫 조회에서 짧게 기다리거나 도착 시각을 재는 방식)이 있어야 의미가 있어 이번에는 두지 않았다. block(보이지 않는 텍스트를 먼저 그림)도 글자를 그리지 않는 상태가 레이아웃에 없어 하지 않았다 | — |

### 31.2 변경 파일

- 제품: `layout/{StickyConstraint(새), CjkLineBreak(새), LayoutBox, LayoutEngine, LayoutResult, BoxBuilder, PaintWalker, PaintSequence, InlineLayouter, AnchorPositioning, AbsoluteLayouter}`, `render/{StickyGroup(새), TextClipBackground(새), DisplayCommand, DisplayList, DisplayListBuilder, PagePainter, PaintContext, RenderedPage, BackgroundLayers, BoxCommands, TextRenderer, DisplayCommands}`, `core/{PageView, PageResources}`, `SwingWebPanel`, `css/anim/{AnimationEngine, CssAnimationEvent, CssTransitionEvent}`, `css/cascade/{StyleAnimator, StyleResolver}`, `css/props/{Prop(309 `transition-behavior`, 310 `position-area`, COUNT 310), PropertyTable, Shorthands}`.
- 시험(새 클래스 11개): `render/{StickyPaintTest 10, BackgroundAttachmentPaintTest 6, TextDecorationInkPaintTest 3}`, `core/StickyIntegrationTest 1`, `layout/{LineBreakStrictnessLayoutTest 4, PositionAreaLayoutTest 5, BoxDecorationBreakLayoutTest 3}`, `css/{DiscreteTransitionTest 5, PseudoElementTransitionTest 3}`; 고친 시험: `core/WebFontIntegrationTest`(교차 오리진 글꼴이 이제 요청된다; 허용되는 경우의 시험 추가 → 3건).
- 문서: 이 파일, `docs/FEATURE_MATRIX.md`(CSS-11/13/14/15/18/26/39/46 행에 CSS-K 결과 추가, CSS-18 의 "sticky 미구현", CSS-26 의 `@starting-style`/의사 요소 한계, CSS-46 의 `position-area` 한계 삭제).

### 31.3 시험 실행 결과(모두 JDK 6u45, 이 PC)

- 항목마다 `scripts/pkgtest.sh` 로 새 시험을 돌려 통과를 확인했다(위 건수). sticky 를 넣은 직후 `layout render core @root` 전체(1146 통과 0 실패 1 건너뜀: JAR 시험은 JAR 을 만들지 않은 실행이라 건너뜀)를 돌렸고, `css core` 접두사 `org.duckdns.hjow.swingwebpanel.css`(321 통과 0 실패)로 전환 변경을 확인했다.
- **전체 회귀**(`BUILD_DIR=build/full sh test.sh`, 작업 트리 마지막 커밋 `20261117-927` 기준): `=== 3479 passed, 1 failed, 12 skipped (608869 ms) ===`(건너뜀 12 는 선택적 Opus 원본 벡터). 실패 1건은 `MixedDocumentsIntegrationTest.testXhtmlWithSvg`(`getContent()` 가 XHTML 원문과 다름)인데, 같은 실행 중에 제품과 `tools/uicheck` 를 다시 빌드하고 실제 창을 띄워 부하가 컸던 상황의 시간 의존 실패로 보이며, 단독 재실행(`scripts/pkgtest.sh ... --filter org.duckdns.hjow.swingwebpanel.MixedDocuments`)에서는 6건 모두 통과했다(원인을 더 파지는 않았다: CSS 와 무관한 XHTML 원문 시험이다). 이 작업 전 master 의 통합 전체는 3439 통과였고 새 시험 41건이 더해진 값과 일치한다.
- 소스 규칙 점검 `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel tests/src/org/duckdns/hjow/swingwebpanel`: 1585 파일 0 위반.
- 실제 Swing 창에서 눈으로 확인한 것은 31.7 에 있다(sticky, fixed, 글자로 자른 그라디언트, 끊기는 밑줄). 나머지(`position-area`, 줄바꿈 엄격도, `box-decoration-break`, 전환)는 화면 밖 이미지의 화소나 레이아웃 좌표 시험만 있고 실제 창 확인은 하지 않았다.

### 31.4 남은 한계(숨기지 않는다)

- sticky: `overflow` 상자(스크롤하지 않음, EXC-13) 안과 인라인 상자는 움직이지 않는다. `getBoundingClientRect`, 텍스트 선택 강조, 이미지 맵 좌표는 옮기기 전 위치이다. 변환된 조상 안에서는 변환된 좌표계의 뷰포트로 계산한다. 퍼센트 오프셋은 뷰포트 기준이다. `JViewport` 의 기본 스크롤(픽셀 복사) 뒤에 전체를 다시 그리는 방식이므로 sticky/fixed 가 있는 큰 페이지는 스크롤마다 전체를 그린다.
- `background-attachment: local` 은 스크롤하는 상자가 엔진에 없어 정적이다. `fixed` 는 `position: fixed`/sticky/변환 안에서 좌표계 가정이 근사이다. `background-clip: text` 는 캔버스(루트/body)에는 적용하지 않고, `-webkit-text-fill-color` 가 없어 `color: transparent` 로만 글자를 숨길 수 있다.
- `line-break`: 언어(`lang`)가 아닌 글자 종류로 일본어/중국어를 판단한다. `pretty` 는 끝에서 둘째 줄만 조정하는 근사이다.
- `text-decoration-skip-ink`: 복합 문자 체계와 400 자를 넘는 조각, 세로쓰기에는 건너뛰지 않는다.
- `position-area`: 논리 키워드는 가로쓰기·왼쪽에서 오른쪽만. `anchor-center` 값과 안전/불안전 정렬은 없다.
- `box-decoration-break: clone`: 줄바꿈이 복제한 가장자리의 너비를 확보하지 않아 줄이 그만큼 넓어질 수 있고, 다단 조각과 오른쪽에서 왼쪽 줄은 미구현이다.
- 교차 오리진 글꼴: JVM 프록시가 적용되면 JRE 연결을 써서 `Origin` 이 빠진다(`docs/progress/net.md` 의 알려진 한계와 같다).

### 31.5 P10-08 체크 후보 판단

**P10-08 은 체크하지 않는다**(체크 후보 아님). 모든 CSS 후속이 구현이거나 근거 있는 예외로 확정되지 않았다. 이번에 구현으로 닫은 것: sticky, `background-attachment: fixed/local`, `background-clip: text`, `box-decoration-break`(인라인), `line-break` 엄격도, `text-wrap: pretty`, `text-decoration-skip-ink`(+`text-underline-offset/position`), `transition-behavior: allow-discrete`, 의사 요소 전환, `position-area`, 교차 오리진 웹 글꼴. **여전히 후속으로 남은 것**(구현할 수 있으나 하지 못했다; 근거 있는 예외가 아니다): 세로쓰기 `text-emphasis`, `resize` 핸들, `shape-outside` 이미지 모양, `filter/clip-path/mask` 의 SVG 요소 참조(조사하지 못함), `anchor()` 의 `calc()` 안 사용·암묵 앵커·`position-try-*`, WOFF2(Brotli), `font-display`, 다단 조각의 `box-decoration-break: clone`, `cross-fade()`/`element()`, `::file-selector-button`/`::backdrop`/`::highlight()`, `@font-feature-values`, `contain-intrinsic-*`, Motion Path 의 일부와 `element.animate()`. 이 중 구조적으로 불가능한 것은 없고(스크롤 컨테이너 소유 같은 EXC-13 은 §30.4 에 이미 있다) 모두 크기와 시간 때문에 남았다.

### 31.6 조정자 요청

1. `docs/FEATURE_MATRIX.md` 13절의 축별 개수표(CSS 표시)를 갱신해 주세요: 이번에 CSS-11/13/14/15/18/26/39/46 의 한계가 줄었다(행은 아직 `부분 구현`).
2. `docs/ARCHITECTURE.md`: `layout.StickyConstraint`/`render.StickyGroup`(sticky 그룹과 그릴 때 이동량), `PagePainter.paint(..., viewX, viewY)`/`PaintContext.viewport`(호스트가 스크롤한 그래픽스에서 fixed/sticky 의 기준), `render.TextClipBackground`(`background-clip: text`), `BoxCommands.FixedLayer`, `layout.CjkLineBreak`, `layout.AnchorPositioning.Area`, `css.anim.AnimationEngine.animatePseudo`(의사 요소 전환)를 새 절/표에 추가해 주세요.
3. **동작 변경 알림(PROGRESS 에 기록 권장)**: ① 실제 `JScrollPane` 안의 컴포넌트에서 `position: fixed` 상자가 이제 스크롤해도 제자리에 있다(이전에는 페이지와 함께 스크롤됨; 화면 밖 이미지 시험은 스크롤을 직접 줘서 통과했었다). ② `position: sticky` 상자는 쌓임 맥락이 되었다. ③ 밑줄은 기본(`text-decoration-skip-ink: auto`)으로 내려긋는 획을 피해 끊긴다(브라우저와 같음). ④ 교차 오리진 웹 글꼴이 요청된다.
4. AGENTS.md P10-08 은 체크하지 않는다. 남은 후속을 `P10-08-a` `resize` 핸들, `-b` `shape-outside` 이미지, `-c` SVG 참조, `-d` 세로 `text-emphasis`, `-e` WOFF2, `-f` `position-try` 등으로 나누어 추적하는 것을 권한다.
5. 31.7 의 실제 창 확인(sticky, fixed, 글자로 자른 그라디언트, 끊기는 밑줄)을 P22-10 의 렌더링 확인 목록에 더해 주세요(스크린샷은 `docs/progress/uicheck/cssk-*.png`).

### 31.7 실제 Swing 창에서 눈으로 확인한 것(`tools/uicheck`, JDK 6u45, Windows 10, 2026-11-18)

`docs/progress/uicheck/cssk-page.html`(클래스패스의 `uik.html`)을 실제 `JFrame` + `JScrollPane` + `SwingWebPanel` 창(내용 600x400)에 열어 스크린샷을 찍고 읽었다: 명령은 `SWP_CLASSES=build/ui/classes sh tools/uicheck/run.sh --out <디렉터리> --size 600x400 --url classpath://uik.html --shot top --scroll 400 --shot scrolled --dispose`.
- `cssk-top.png`: 글자로 자른 그라디언트(`Gradient Text`가 빨강에서 파랑으로 번짐, 글자 자체는 `color: transparent`), 밑줄(`gjpqy underline`의 빨간 밑줄이 g j p q y 의 내려긋는 획 둘레에서 끊김), 노란 sticky 머리글이 제자리, 오른쪽 아래의 fixed 초록 상자.
- `cssk-scrolled.png`(세로 스크롤 400): sticky 머리글이 창 위쪽에 붙어 있고, fixed 상자는 같은 자리(오른쪽 아래)에 있으며, 뒤의 그라디언트 배경만 스크롤되었다.
확인하지 못한 것: 실제 마우스 휠/키보드 스크롤 중의 모습(스크롤 막대 이동만 확인), 한글 IME, 작은 가나 줄바꿈의 실제 글꼴 모양(자동 시험이 좌표를 확인한다).

## 32. P10-08-d 남은 후속 (CSS-L, 2026-11-18, 커밋 번호 `20261117-940` 부터)

§31.5 의 "여전히 후속" 중 `resize` 핸들, `shape-outside` 이미지 모양, SVG 요소 참조(`filter`/`clip-path`/`mask`), 세로쓰기 `text-emphasis`, 다단 `box-decoration-break: clone`, `anchor()` 의 `calc()` 안 사용과 암묵 앵커, `font-display` 의 기간 모델을 구현했다. 조정자의 추가 요청(2026-11-18)으로 **WOFF2(Brotli 디코더와 WOFF2 컨테이너, 32.5)** 와 **`position-try-*` 일체(`@position-try`, `position-try-fallbacks`/`-order`, `position-try`, `position-visibility`, `anchor-scope`, `anchor-center`, 32.6)** 도 구현했다. `font-display` 의 block 기간 보이지 않는 텍스트만 하지 못했다(32.4). **새 CSS 속성 id(조정자 재배정 대상)**: `position-try-fallbacks` 312, `position-try-order` 313, `position-visibility` 314, `anchor-scope` 315(`Prop.COUNT` 311 → 315; `PropertyTable.DEFS` 의 끝에 이 순서로 덧붙였고 `image-orientation`(311) 다음이다. 병합에서 다른 작업자가 같은 끝에 속성을 더했다면 번호와 `COUNT` 를 이어서 다시 매겨야 한다). 새 CSS 규칙 종류 상수 `CssRule.POSITION_TRY_RULE` 106(CSSOM `type` 은 0). 새 이름 있는 값 정의 `area-value`, `try-tactic`(`PropertyTable.PRODUCTIONS`; `position-area` 의 값 정의를 `area-value` 로 옮겼다: 문법은 같다). 문법이 바뀐 속성: `align-self`/`justify-self`/`align-items`/`justify-items`(`anchor-center` 추가). 문법만 바뀐 속성: `position-anchor`(`match-parent` 추가), `clip-path`(`<url>` 추가), `shape-image-threshold`(`<number> | <percentage>`, "파싱만" 표시 제거).

규격 조회(도구 `tools/webfetch`, 실행 JRE: Zulu 25, 조회일 2026-11-18): CSS Shapes Module Level 1 https://www.w3.org/TR/css-shapes-1/ (`Last-Modified: Thu, 05 Jun 2025`; 4 절 Shapes from Image, 6.2 `shape-image-threshold`), CSS Anchor Positioning Level 1 https://www.w3.org/TR/css-anchor-position-1/ (2.4 `position-anchor`, 2.4.1 암묵 앵커), RFC 7932 https://www.rfc-editor.org/rfc/rfc7932.txt (WOFF2 설계용 구조 확인: 부록 A 가 정적 사전 122,784 바이트를 16진 덤프와 CRC-32 `0x5136cb04` 로 싣고 부록 B 가 변환 목록을 싣는다). 나머지(CSS UI 4 resize, Filter Effects 1, Masking 1, Fragmentation 3, Text Decoration 4, Fonts 4 4.8)는 규격 지식에 따랐고 본문을 줄 단위로 대조하지는 않았다.

### 32.1 항목별 상태

| 항목 | 상태 | 구현 | 시험(JDK 6u45) |
| --- | --- | --- | --- |
| `resize`(none/both/horizontal/vertical/block/inline) | **구현** | `layout.Resizing`(방향 판정: 블록 컨테이너이고 `overflow` 가 hidden/scroll/auto, `block`/`inline` 은 쓰기 모드를 따름; 15px 핸들 사각형: padding box 오른쪽 아래, padding box 가 더 작으면 없음; 변환을 거꾸로 적용한 히트 판정; `box-sizing` 에 따른 값 보정), `layout.PaintWalker.Visitor.resizer`(내용을 자르는 padding box 자르기가 끝난 뒤 내용 위에 그림), `render.DisplayCommands.Resizer`(밝은 줄과 어두운 줄의 대각선 둘), `core.PageResize`(누름: 핸들이면 DOM `mousedown`/`mouseup` 을 보내지 않음, 끌기: 포인터 이동만큼 border box 크기를 인라인 `style` 의 `width`/`height` 로 씀, 놓기), `core.PageInput`(끌기를 언제나 문서 큐에 넘겨 누름 뒤에 순서대로 처리; 핸들 위 `se-resize`/`e-resize`/`s-resize` 커서) | `ResizingLayoutTest` 6, `ResizeIntegrationTest` 7(실제 AWT 이벤트와 컴포넌트의 그리기: 핸들 그림, 조건이 없으면 핸들 없음, 끌어 크기 바뀜과 재배치, 방향 제한, content-box, 핸들 누름의 `mousedown` 억제, 커서) |
| `shape-outside` 이미지 모양, `shape-image-threshold` | **구현** | `layout.FloatShapes`(이미지/`image-set()`/그라디언트 값 판정, 알파가 기준보다 큰 표본의 사각형을 합쳐 `Area` 로: 이미지는 float 의 콘텐츠 상자에 맞게 늘림(4 절), 축마다 최대 512 표본, 같은 구간이 이어진 행은 합침, 기준을 넘는 픽셀이 없으면 빈 영역), `layout.ShapeImage`/`ShapeImageSource`(레이아웃이 보는 알파 마스크의 공급 계약), `render.ImageReplacedResolver`(CORS `anonymous` 로 가져온 첫 프레임의 알파, 그라디언트는 `GradientSpec.createPaint` 로 그려 알파만), `layout.BoxBuilder`(이미지 모양인 float 에 공급자를 연결), `css.cascade.StyleComputer`(`shape-image-threshold` 를 0~1 숫자로) | `ShapeImageLayoutTest` 10(가짜 마스크: 불투명한 반쪽, 늘림, 기준값 숫자/백분율/1, 빈 영역, 쓸 수 없는 이미지, `shape-margin`, 오른쪽 float, 그라디언트와 그 크기, 공급자 없음, `image-set()` 선택), `ShapeImageIntegrationTest` 2(실제 HTTP: 가져오는 동안 margin box 이다가 도착하면 이미지 알파를 따름 · 허락 없는 다른 오리진 이미지는 쓰지 않고 `Access-Control-Allow-Origin` 이 있으면 씀) |
| `filter: url(#id)`, `clip-path: url(#id)`, `mask-image: url(#id)` (같은 문서의 SVG 요소 참조) | **구현**(래스터) | `media.image.svg.SvgReference`(공개 파사드: 정의가 든 `svg` 를 한 번 해석, 보조 `rect` 를 border box 로 맞추고 SVG 이미지와 같은 `SvgFilters.apply`/`SvgMasks.applyClipPath`/`applyMask` 를 호출; `filterReach`, `maskCoverage`), `SvgFilters.userRegion`(필터 영역 계산을 함수로 뺌), `SvgImage.parseDefinitions`(크기가 0 인 숨겨진 `svg` 도 정의로 읽음), `render.SvgReferences`(DOM 에서 요소 종류·SVG 네임스페이스를 확인하고 가장 바깥 `svg` 를 직렬화해 보관), `layout.FilterSpec`(`REFERENCE` 함수, `resolve`, `extent`), `ClipPaths.reference`, `render.EffectGroup`/`PagePainter`(순서: 필터, 클립 경로, 마스크)/`FilterRenderer`/`MaskLayers`(마스크 레이어를 `maskCoverage` 로)/`BackgroundLayers.fragmentReference`(`#id` 를 이미지로 가져오지 않음)/`CanvasFilters`(캔버스는 참조를 해석하지 않으므로 거르지 않는 값) | `SvgReferencePaintTest` 12(`feColorMatrix`, `feOffset` 과 필터 영역, `userSpaceOnUse` 영역, 흐림이 영역 끝까지 닿음, 함수와 섞은 사슬 순서, 깨진 참조 다섯 가지와 쌓임 맥락, 사용자 공간/경계 상자 단위의 클립 경로, `mask` 요소와 `maskContentUnits`, 없는 마스크는 상자를 가림, 셋을 함께, `display:none` 인 `svg` 의 정의), `FilterPaintTest.testInvalidAndUrl`(옛 "url() 은 무시"를 새 계약으로 고침) |
| 세로쓰기 텍스트의 `text-emphasis` | **구현**(`vertical-rl`/`vertical-lr`) | `layout.TextEmphasis`(세로 조판 판정, `over`/`under` 대신 `right`/`left`, 기본 모양 sesame), `render.VerticalTextCommands.EmphasisColumn`(똑바로 선 글자는 칸의 가운데, 눕힌 글자는 advance 의 가운데, 조합 텍스트는 상자 가운데; 글리프 상자 오른쪽/왼쪽의 확보된 공간에 똑바로 그림), `render.DisplayListBuilder`. 줄이 그쪽으로 커지는 것은 기존 `emUp`/`emDown` 계산을 그대로 씀 | `VerticalEmphasisPaintTest` 5(오른쪽 칸마다 점, 왼쪽, 줄 폭 증가, 기본 sesame 과 `vertical-lr`, 눕힌 글자와 공백) |
| 다단 조각의 `box-decoration-break: clone` | **구현** | `layout.MulticolLayouter`(끊는 자리마다 clone 조상의 위·아래 장식 합을 구간 계산에 넣음 → 조각의 내용과 아래쪽 장식이 열에 들어가는 곳에서 자름, 이어지는 조각은 위쪽 border/padding 을 되풀이하며 내용이 그 아래에서 시작, 끊긴 조각은 아래쪽 장식, 중첩 지원) | `MulticolCloneLayoutTest` 5(slice 는 장식이 바깥 가장자리만, clone 은 모든 조각에 두 장식·높이 90/90/70·내용 시작 15px, 조각이 열 높이를 넘지 않음, 나뉘지 않는 상자는 그대로, 중첩) |
| `anchor()`/`anchor-size()` 의 `calc()`/`min()`/`max()`/`clamp()` 안 사용 | **구현** | `css.values.CalcNode.Anchor`(잎: 값 없음, 레이아웃 의존이라 접히지 않음), `CssCalc.withAnchors`/`hasAnchor`/`AnchorResolver`, `layout.AnchorPositioning.evaluateCalc`(함수마다 길이/대체값, 풀 수 없고 대체값 없으면 속성은 `auto`(최대 크기는 `none`)) | `AnchorCalcImplicitLayoutTest` 6 중 3(calc 의 네 속성, `max`/`clamp`/`min` 과 이름 생략, 대체값과 `auto`) |
| 암묵 앵커(`position-anchor: auto`, `position-area` 가 있는 `normal`, `match-parent`) | **구현**(의사 요소의 원래 요소만) | `layout.AnchorPositioning.Lookup.defaultRect`/`implicitRect`/`principalBoxOf`(절대 배치 상자는 포함 블록으로 옮겨져 있어 트리 조상이 아니므로 DOM 으로 주 상자를 트리에서 찾음), `PropertyTable`(`match-parent`) | 위 시험의 나머지 3(`::after` 의 `anchor(bottom)`, `normal` 은 `position-area` 에 따라 none/auto 이고 영역 아래에 가운데 정렬, `match-parent`) |
| `font-display` 의 기간 모델(`optional`, `fallback`, `swap`, `auto`/`block` 의 swap 기간) | **부분 구현**(block 기간의 보이지 않는 텍스트 제외) | `core.PageResources`(`Entry.fetchStart/fetchDone` 와 `fontTimes`: 요청을 시작하고 바이트가 도착한 시각을 요청 계층이 잼), `core.PagePresentation`(`WebFontSet.TimedLoader`), `layout.WebFontSet`(`late`: `optional` 은 block 100ms/swap 0, `fallback` 은 block 100ms/swap 3s, 나머지는 swap 무한; 늦게 도착한 글꼴은 `FAILED`("font-display: ...") 로 두어 이 페이지에서 대체 글꼴이 유지됨) | `FontDisplayLateTest` 4(경계값 포함), `FontDisplayIntegrationTest` 4(지연 700ms 의 `optional` 은 끝까지 대체 글꼴, 곧바로 오면 씀, `swap`/`auto`/`fallback` 은 늦어도 씀; 세 번 반복해 안정 확인) |
| WOFF2(`@font-face` 의 `format('woff2')`) | **구현**(실제 브라우저가 만든 WOFF2 로는 확인하지 못함: 32.5) | `media.font.Brotli`(RFC 7932 디코더: 비트 읽기, 단순/복잡 접두사 코드, 블록 종류와 전환, 문맥 모드와 문맥 지도(RLE, 역 MTF), 삽입·복사 명령, 거리 링 버퍼, 정적 사전과 변환 121개, 압축 안 한 블록, 출력 한도), `media.font.BrotliData`(생성: `tools/brotlidict` 가 RFC 부록 A/B 의 텍스트에서 사전 122,784바이트, 변환 표, 문맥 조회표 셋을 읽고 RFC 가 적은 CRC-32 와 대조한 뒤 쓴다), `media.font.Woff2`(헤더, 테이블 디렉터리, `UIntBase128`/`255UInt16`, 파일 구조 검사, 변환된 `glyf`/`loca`/`hmtx` 복원, 체크섬 재계산, sfnt 재조립), `layout.WebFontSet`(`woff2` 힌트와 서명), `css.cssom.SupportsCondition`(`font-format(woff2)`) | `BrotliTest` 7, `Woff2Test` 15, `WebFontTest.testWoff2SourceIsDecoded`, `WebFontIntegrationTest.testWoff2FontIsUsedByThePage`, `SupportsConditionTest`(32.3) |
| `@position-try`, `position-try-fallbacks`/`-order`/`position-try`, `position-visibility`, `anchor-scope`, `anchor-center` | **구현**(한계: 32.4, 32.6) | `css.cssom.PositionTryRule`(CSSOM 포함), `css.props`(속성 id 312~315, 단축 속성 `PositionTry`), `layout.PositionTry`(대안 읽기, 전술, 순열, 위치 영역 뒤집기, 정렬 뒤집기), `layout.AbsoluteLayouter`(시도 배치 `trial`, 넘침 판정, `fallback`, 가시성), `layout.AnchorPositioning`(`anchor-center`, 앵커 범위, 앵커 상자 가림 판정) | `PositionTryLayoutTest` 12, `PositionTryPropertiesTest` 6, `CssomModernRulesTest.testPositionTryRule` |

### 32.2 변경 파일

- 제품(새 클래스): `layout/{Resizing, ShapeImage, ShapeImageSource}`, `core/PageResize`, `media/image/svg/SvgReference`, `render/SvgReferences`.
- 제품(고침): `layout/{FloatShapes, LayoutBox, BoxBuilder, PaintWalker, PaintSequence, FilterSpec, ClipPaths, TextEmphasis, MulticolLayouter, AnchorPositioning, WebFontSet}`, `render/{DisplayCommand, DisplayCommands, DisplayListBuilder, ImageReplacedResolver, EffectGroup, PagePainter, FilterRenderer, MaskLayers, BackgroundLayers, CanvasFilters, VerticalTextCommands}`, `media/image/svg/{SvgFilters, SvgImage}`, `css/values/{CalcNode, CssCalc}`, `css/props/PropertyTable`, `css/cascade/StyleComputer`, `core/{PageInput, PageResources, PagePresentation}`.
- 시험(새 클래스 11): `layout/{ResizingLayoutTest 6, ShapeImageLayoutTest 10, MulticolCloneLayoutTest 5, AnchorCalcImplicitLayoutTest 6, FontDisplayLateTest 4}`, `render/{SvgReferencePaintTest 12, VerticalEmphasisPaintTest 5}`, `core/{ResizeIntegrationTest 7, ShapeImageIntegrationTest 2, FontDisplayIntegrationTest 4}`; 고친 시험: `render/FilterPaintTest.testInvalidAndUrl`(url 참조의 새 계약). 새 시험은 모두 61건.
- WOFF2(커밋 948~950): 제품(새 클래스) `media/font/{Brotli, BrotliData(도구가 생성), BrotliException, Woff2}`; 제품(고침) `layout/{WebFontSet, Woff}`, `css/cssom/SupportsCondition`; 도구 `tools/brotlidict`(Java 6: `src/brotlidict/BrotliDict.java`, build/run 의 sh 와 bat, `AGENTS.md`, `README.md`); 시험 `media/font/{BrotliTest 7, Woff2Test 15, Woff2Builder, Woff2Fonts}`(고친 시험: `layout/WebFontTest`(+`testWoff2SourceIsDecoded`), `core/WebFontIntegrationTest`(+`testWoff2FontIsUsedByThePage`), `css/SupportsConditionTest`); 시험 자료 `tests/resources/org/duckdns/hjow/swingwebpanel/media/font/{brotli/(참조 인코더가 만든 스트림 23개와 그 입력; 실제 글꼴 없음), woff2/(스크립트가 만든 합성 글꼴 `synthetic.ttf` 와 그것으로 만든 WOFF2 세 개), generation/{make-brotli-vectors.js, make-woff2-vectors.js}}`; `.gitattributes`(이진 시험 자료의 줄 끝 변환 방지).
- `position-try-*`(커밋 951): 제품(새 클래스) `css/cssom/PositionTryRule`, `layout/{PositionTry, PositionTrySource}`; 제품(고침) `css/props/{Prop, PropertyTable, Shorthands}`, `css/cssom/{CssRule, CssStyleDeclaration(restrictTo), StyleSheetBuilder}`, `css/cascade/{RuleIndex, StyleResolver}`, `css/values/{CalcNode, CssCalc}`(`mapAnchors`), `js/dom/CssomInterfaces`, `layout/{AbsoluteLayouter, AnchorPositioning, BlockLayouter, CssStyleSource, FlexLayouter, LayoutBox, LayoutEngine}`; 시험(새) `layout/PositionTryLayoutTest 12`, `css/PositionTryPropertiesTest 6`; 고친 시험 `js/host/CssomModernRulesTest`(+`testPositionTryRule`).
- 문서: 이 파일, `docs/FEATURE_MATRIX.md`(CSS-13, 15, 32, 34, 35, 36, 39, 46 행에 CSS-L 결과 추가).

### 32.3 시험 실행 결과(모두 JDK 6u45, 이 PC)

- 항목마다 새 시험을 돌려 통과를 확인했다. 중간 점검으로 `layout`(423), `render`(292), `css`(321), `core`(401, resize 직후) 접두사 전체를 여러 번 돌렸고 모두 0 실패였다.
- **전체 회귀**(`BUILD_DIR=build/full sh test.sh`, 작업 트리 마지막 코드 커밋 `20261117-946` 기준): 첫 실행은 `js.host.canvas`(`Context2D.measureText` → `Font.layoutGlyphVector`)에서 JRE 6u45 의 `fontmanager.dll` 접근 위반으로 JVM 이 죽어 요약 줄이 없다(그 전까지 1941 건 PASS, FAIL 0; 이 작업과 무관한 기존 결함). 마지막 결과는 이 절 끝의 "최종 전체 회귀"에 적는다.
- WOFF2 와 `position-try-*` 의 항목별 시험(JDK 6u45): `BrotliTest` 7 통과, `Woff2Test` 14 통과(원본 JRE 글꼴 비교는 건너뛰지 않고 실제로 수행: 실행 JRE 의 `lib/fonts` 에 같은 CRC-32 의 원본이 있었다), `WebFontTest` 6, `WebFontIntegrationTest` 4, `SupportsConditionTest` 3, `CssomFontsTest` 4, `PositionTryLayoutTest` 12, `PositionTryPropertiesTest` 6, `CssomModernRulesTest` 7 모두 통과. 패키지 단위 확인: `layout` 440, `css` 327, `js.host` 438 통과(0 실패). Woff2 디코더의 삼중 식 하나를 일부러 틀리게 고쳐 시험이 4건 실패로 잡는 것을 확인한 뒤 되돌렸다(변이 시험 한 번).
- 소스 규칙 점검 `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel tests/src/org/duckdns/hjow/swingwebpanel`: 1619 파일(src, tests, 이 문서와 기능 표 포함) 0 위반(2026-11-18, 마지막 코드 커밋 전).
- 시험을 돌리는 동안 JRE 6u45 의 `fontmanager.dll` 접근 위반으로 JVM 이 한 번 죽었다(`js.host.canvas` 접두사 시험 중, 이 작업의 변경과 무관하게 §30.7 에 적힌 같은 결함): 같은 시험을 다시 돌리면 통과했다(73건).
- 실제 Swing 창에서 눈으로 확인한 것은 없다: 위 모두 화면 밖 이미지의 화소/좌표 시험과 합성 AWT 이벤트이다(`resize` 의 실제 OS 마우스 끌기와 핸들 모양은 `tools/uicheck` 의 `--realmouse` 로 사람이나 후속 작업이 확인해야 한다).

### 32.4 남은 한계(숨기지 않는다)

- `resize`: `textarea` 에 UA 기본 `resize: both` 를 주지 않았다(기존 화소 시험과 편집기 겹침 때문; 요소의 `resize` 는 작성자가 쓰면 동작), 이미지/`iframe` 의 `resize` 없음, 페이지 확대 중과 `iframe` 안의 끌기는 시험하지 않음, 핸들 그림은 방향과 상관없이 같은 대각선 둘.
- `shape-outside`: `attr(src url)` 미지원, 표본 해상도(512) 안에서만 모양을 따름, 큰 float 에서 줄마다 Area 교차를 하므로 아주 복잡한 모양은 느릴 수 있음(시험 없음), 이미지 도착 전에는 margin box 이다가 도착하면 다시 배치(한 번 더 배치).
- SVG 요소 참조: 외부 파일(`url(file.svg#id)`), `backdrop-filter: url()`, 캔버스 `filter = "url(#id)"` 는 지원하지 않음(캔버스는 거르지 않는 값으로 처리), 클립 경로는 래스터로만 자르므로 클릭(히트 테스트)은 잘리지 않음, 요소가 `transform` 안에 있을 때의 사용자 공간은 변환된 좌표계(합성 변환을 그대로 쓴 것으로 시험은 없음), 매 그리기마다 같은 `SvgReference` 를 잠그고(`synchronized`) 보조 요소를 바꾸므로 한 문서의 필터 요소가 많으면 그리기가 직렬화됨.
- 세로쓰기 `text-emphasis`: `sideways-lr`/`sideways-rl` 에는 그리지 않음, 점은 가로 글리프 그대로, 오른쪽에서 왼쪽 줄(양방향)의 눕힌 글자는 순서를 뒤집지 않음.
- 다단 `clone`: 절대 배치 자식은 되풀이한 위쪽 장식을 반영하지 않음, 표/플렉스/그리드 컨테이너는 나뉘지 않으므로 clone 이 의미 없음(기존 한계), 인라인 clone 의 줄바꿈이 가장자리 너비를 확보하지 않는 한계(§31.4)는 그대로.
- `calc()` 의 anchor: 위치/크기 속성이 아닌 곳에 쓴 `anchor()` 가 든 `calc()` 는 파싱되고 값이 0 이 됨(규격은 계산 시점에 무효), 암묵 앵커는 의사 요소의 원래 요소뿐(popover 호출자 없음).
- `font-display`: block 기간(`auto`/`block` 의 3 초, `fallback`/`optional` 의 100ms)에 글자를 보이지 않게 그리는 상태가 레이아웃/그리기에 없어 글꼴이 오는 동안은 모든 값에서 다음 패밀리를 보여 줌. `local()` 소스는 즉시이므로 늦지 않음. 시각은 `System.currentTimeMillis()`(시스템 시계) 기준이어서 시계가 점프하면 한 번 틀릴 수 있음.
- WOFF2: 글꼴 컬렉션(`ttcf`)은 거부한다. CFF(`OTTO`) flavor 는 복원하지만 JDK 6u45 의 `Font.createFont` 가 CFF 를 거부할 수 있다(기존 한계). 확장 메타데이터와 비공개 데이터는 위치와 패딩만 검사하고 풀거나 해석하지 않는다. 풀린 테이블 합과 다시 만든 sfnt 는 16 MiB 로 제한, 테이블 수는 1024 로 제한. 점이 없는 윤곽선과 16비트 범위를 벗어나는 점 좌표는(드물지만 일부 디코더가 받아들일 수 있는 입력) 거부한다. `glyf` 변환 테이블은 스트림을 다 쓰지 않거나 끝에 남는 바이트가 있어도 받아들인다(규격이 금지하지 않음). Brotli 디코더는 스트림을 통째로 메모리에 푼다(출력 한도 안에서). **검증의 한계**: 이 PC 에는 브라우저나 다른 독립 인코더가 만든 WOFF2 파일이 없었다. 시험 파일은 같은 규격 본문(W3C WOFF2 REC, 최종 수정 2024-08-05)으로 쓴 인코더 둘(JS 스크립트: 실제 JRE 글꼴을 Node.js 의 Brotli 로 압축, Java 생성기: 합성 글꼴과 잘못된 파일, Brotli 저장 블록)이 만들었으므로 규격을 같게 오독했다면 둘 다 틀려 드러나지 않을 수 있다. 완화한 것: 삼중 표를 인코더는 규격 표의 행(바이트 수, 비트, 더하는 양, 부호)으로, 디코더는 닫힌 식으로 구현해 서로 다른 형태로 적었고(표 읽기에서 옮겨 적은 오류는 128 행 전부를 값의 최소/최대/중간으로 확인), 실제 JRE 글꼴 둘을 원본과 테이블별, 글리프별, Java2D 윤곽선·진행 폭으로 비교하며, Brotli 부분은 참조 인코더(Node.js)의 스트림으로 따로 확인했다. 브라우저가 만든 WOFF2 로의 확인은 필요한 환경(허락받은 작은 `.woff2`)이 생기면 하나만 더 `Woff2Test` 에 넣으면 된다.
- `position-try-*`: 규격이 기억하는 "마지막으로 성공한 대안"(6.5.1.1)은 없다(레이아웃을 매번 처음부터 하므로 맞는 첫 대안이 매번 이긴다; 한 문서의 요소 크기가 바뀌어 결과가 달라질 수 있는 경우 규격 구현은 이전 대안을 유지하려 한다). 대안은 최대 16개(규격 최소 5). 전술의 물리 변은 포함 블록의 쓰기 모드와 방향으로 정하나, 세로쓰기 프레임 안의 배치 결과는 시험하지 않았다(순열은 `testTacticPermutations` 로 확인). `anchor()` 의 논리 변(`start`/`end`)은 반대편으로 옮겨지면 서로 바꾸는 근사. 값 안의 `calc()` 는 안의 anchor 함수의 변만 고치고 식의 모양은 그대로(규격과 같다). 문서의 `<dashed-ident>` 와 전술을 함께 쓴 항목은 이름의 규칙을 적용한 뒤 전술로 뒤집는다. `@position-try` 의 값에서 `var()` 는 해석하지 않는다. 넘침 판정은 margin box 와 inset-modified containing block 이며(허용 오차 0.05px) 자손의 넘침과 스크롤 보정은 보지 않는다. `position-visibility: anchors-visible`(초기값)은 앵커를 가진 상자에만 작동하고 스크롤 위치를 알 수 없어 스크롤하지 않은 기하로 자름을 판정한다(스크롤해도 다시 판정하지 않으므로, 스크롤 컨테이너 안에서 처음에 앵커가 잘려 숨겨진 상자는 스크롤로 앵커가 보이게 되어도 다음 레이아웃까지 숨겨져 있다). `visibility: hidden` 인 앵커는 숨겨진 앵커로 세지 않고 `position-visibility` 로 숨겨진 앵커만 센다. `anchors-valid` 는 대체값 없는 `anchor()`/`anchor-size()` 의 앵커가 없을 때만 숨긴다(규격이 정의를 열어 둠: "기본 앵커를 포함할지"). 숨김은 상자와 모든 자손의 계산된 `visibility` 를 `hidden` 으로 고쳐 쓰는 것이며(레이아웃 상자의 스타일만 고쳐 쓰므로 스크립트의 `getComputedStyle` 에는 반영되지 않는다), 숨겨진 상자는 히트 테스트 대상이 아니다. `anchor-scope` 는 `all` 과 이름 목록을 양방향으로 적용하되 그림자 DOM 은 없다. `anchor-center` 는 `justify-self`/`align-self`/`place-self` 만 읽고(`*-items` 로 정해지는 `auto` 는 읽지 않음) 기본 앵커가 없으면 상자를 오프셋과 정적 위치에 둔다(규격은 `center`); 플렉스/그리드에서는 `center` 로 해석한다.

### 32.5 WOFF2 (구현함: 커밋 948~950)

**순서와 결과.** (1) `tools/brotlidict`(Java 6) 가 RFC 7932 텍스트(`tools/webfetch` 로 조회: `https://www.rfc-editor.org/rfc/rfc7932.txt`, 조회일 2026-11-18, etag `W/"1d5372459b2a6c5336defa4ec6da5e72"`, 389,979 바이트)에서 정적 사전 122,784바이트(부록 A), 변환 121개(부록 B), 문맥 조회표 셋(7.1 절)을 읽고 RFC 가 적은 길이와 CRC-32(`0x5136cb04`, `0x3d965f81`, `0x8e91efb7`, `0xd01a32f4`, `0x0dd7a0d6`)와 모두 대조한 뒤 `BrotliData.java`(Base64 상수)를 쓴다(하나라도 다르면 파일을 만들지 않고 실패; 이 PC 에서 모두 일치). (2) `media.font.Brotli` 가 RFC 7932 디코더(약 1,100줄)이며 참조 인코더(Node.js v26.13.0 `zlib.brotliCompressSync`)가 한 번 만든 스트림 23개(32.10 참고)(빈 입력, 아주 짧은 입력, 영어 텍스트 품질 0/5/9/11 와 윈도 10~24, 무작위 바이트, 반복, 16비트 표본, UTF-8, 90,000 글자 긴 텍스트)와 실제 TrueType 글꼴(75,144바이트, 글꼴 모드)을 **바이트 단위로** 복원한다(`BrotliTest`: 스트림이 정적 사전 참조 728개, 서로 다른 변환 45개, 블록 종류 13개, 리터럴 트리 39개, 문맥 모드 마스크 13, NPOSTFIX/NDIRECT 블록, 압축 안 한 블록 3개를 덮는 것도 확인; 잘린 스트림, 비트 변조, 출력 한도, 쓰레기 입력은 `BrotliException` 으로만 끝남). (3) `media.font.Woff2`(W3C WOFF2 REC https://www.w3.org/TR/WOFF2/, 최종 수정 2024-08-05, 조회일 2026-11-18)가 컨테이너와 변환 해제를 한다: 헤더, 알려진 태그 63개와 `UIntBase128`/`255UInt16`, 변환 버전(`glyf`/`loca` 0 과 3, `hmtx` 0 과 1, 그 밖은 거부), 파일 구조(길이 필드, 압축 블록 → 메타데이터 → 비공개 데이터의 4바이트 경계와 0 패딩, 불필요한 데이터 거부), Brotli 로 푼 크기와 디렉터리 길이 합의 일치, 변환된 `glyf`(스트림 일곱 개, 삼중 좌표 128 값, 점 플래그의 곡선 위/밖, 합성 글리프의 구성 요소(단어 인수, 세 종류의 크기 조정, MORE, 명령 플래그), 명령 스트림, `bboxBitmap`(빈 글리프에 상자가 있으면 거부, 합성 글리프에 상자가 없으면 거부, 단순 글리프의 상자는 없으면 점에서 계산), 겹침 비트맵), `loca`(짧은/긴 형식, 4바이트 정렬, 짧은 형식의 범위 초과 거부), `hmtx`(생략된 왼쪽 여백을 글리프 상자의 xMin 에서 복원), 모든 테이블의 체크섬과 `head` 의 `checkSumAdjustment`(`0xB1B0AFBA` 에서 뺀 값) 재계산, 태그 순 sfnt 재조립. `layout.WebFontSet` 이 `format('woff2')` 소스를 받아 서명으로 알아보고 sfnt 로 복원해 `Font.createFont` 로 읽으며(손상된 WOFF2 는 실패한 소스로 세어 다음 소스를 시도), `font-format(woff2)` 는 참이 된다.

**검증과 그 한계(정직하게).** 이 PC 에는 브라우저가 만든 WOFF2 가 없다(조정자 확인). 그래서 시험용 인코더 둘을 같은 규격 본문으로 직접 썼다: ① `tests/resources/.../generation/make-woff2-vectors.js`(Node.js): 스크립트가 직접 만드는 합성 TrueType 글꼴(`synthetic.ttf`, 글리프 240개: 단순 192, 합성 33, 빈 것 15, 명시한 상자 56, 삼중 행 125개 사용, 짧은 `loca`, `hmtx` 변환 플래그 1)을 WOFF2 로 만든다(`glyf`/`loca`/`hmtx` 변환 포함 그리고 변환 없음 그리고 확장 메타데이터와 비공개 데이터가 붙은 것. 처음에는 JRE 의 Lucida 글꼴로 만들었으나 라이선스 때문에 바꿨다: 32.10); ② `Woff2Builder`(Java, 시험 코드): 합성 글꼴, 잘못된 파일(Brotli 저장 블록으로 감싸 단일 바이트를 바꿀 수 있게), 그리고 시험 때 실행 중인 JRE 의 실제 글꼴(Lucida Bright Demibold, Lucida Typewriter Bold, Lucida Sans Regular)을 바꾼 WOFF2(압축하지 않은 Brotli 블록; 파일은 저장소에 두지 않음, `Woff2Fonts`). 디코더 시험 `Woff2Test` 15건: 합성 글꼴(참조 Brotli 압축)과 실제 JRE 글꼴(시험 때 변환)의 복원 결과를 원본과 **테이블별**(`glyf`/`loca`/`head` 제외 바이트 같음, `head` 는 `checkSumAdjustment` 와 규격이 정한 플래그 11 번 비트만 다름), **글리프별**(점, 곡선 위/밖, 명령, 합성 구성 요소, 상자), Java2D(글리프 코드, 진행 폭, 400자 가까운 윤곽선의 경로 선분이 같음)로 비교, 모든 테이블의 체크섬과 전체 체크섬이 맞는지, 128 개 삼중 행 전부(가장 작은/가장 큰/중간 값), 255UInt16 의 세 형태(점 수 300/600/800, 명령 길이 300/600/1000), 합성 글리프 세 종류의 크기 조정과 명령, 짧은/긴 `loca`, `hmtx` 플래그 1/2/3 과 긴 지표 수가 글리프 수보다 작은 경우, 알려진 태그를 명시한 태그로 쓴 것, flavor `OTTO`/`true`, 규격이 디코더가 거부하라고 한 위반 40가지가 넘는 경우와 모든 길이의 잘림과 무작위 바이트 변조 4,000번과 전 바이트 반전(모두 `IOException` 으로만 끝남). **여전히 드러나지 않을 수 있는 것**: 인코더와 디코더를 같은 사람이 같은 본문으로 썼으므로 규격의 같은 오독은 둘 다에 있다(완화: 삼중 표는 인코더가 표의 행으로, 디코더가 닫힌 식으로 달리 적었고, 원본 글꼴과 비교하며, 디코더의 삼중 식을 일부러 고쳐 시험이 실패하는 것을 한 번 확인했다).

### 32.6 `position-try-*` (구현함: 커밋 951)

규격: CSS Anchor Positioning Level 1(https://www.w3.org/TR/css-anchor-position-1/, `Last-Modified: Fri, 27 Mar 2026`, 조회일 2026-11-18)의 2.2 `anchor-scope`, 4.1/4.2 `anchor-center`, 6 절 전체(6.1 `position-try-fallbacks`, 6.2 `position-try-order`, 6.3 `position-try`, 6.4 `@position-try`, 6.5 적용 알고리즘과 6.5.2 전술, 6.6 `position-visibility`), 8.1 `CSSPositionTryRule`.

- **`@position-try`**: `css.cssom.PositionTryRule`(이름은 `--`로 시작하는 식별자만, 받는 속성은 8.1 의 서술자 목록과 같은 이름의 개별 속성: 오프셋, 마진, 크기, 자기 정렬, `position-anchor`, `position-area`; `CssStyleDeclaration.restrictTo` 가 선언 단위로 걸러내고 `!important` 와 사용자 정의 속성은 무효; JS 에서 `style.setProperty` 도 같다). 같은 이름은 나중 규칙이 이긴다(`RuleIndex.positionTryRules`). 레이아웃은 `PositionTrySource.positionTryOption` 으로 규칙의 선언을 요소 기준으로 계산한 값을 받는다(논리 속성은 쓰기 모드의 물리 속성으로).
- **대안 읽기**: `PositionTry.options` 가 `position-try-fallbacks` 를 항목별로 읽는다(이름, 전술 한 개 이상, 이름+전술, `position-area` 값). 전술 `flip-block`/`flip-inline` 은 포함 블록의 쓰기 모드에 따라 `flip-y`/`flip-x` 로, `flip-start` 는 블록 시작 변과 인라인 시작 변(방향 `rtl` 과 `sideways-lr` 포함)을 맞바꾸는 대각선 뒤집기로 바뀐다(`PositionTry.permutation`). 뒤집기는 6.5.2 의 순서를 따른다: 오프셋과 마진은 반대편(또는 대각선 맞은편) 변의 값과 맞바꾸고, `anchor()` 의 물리 변은 순열을 따르고 `start`/`end` 와 백분율은 반대편으로 옮겨지면 맞바뀌거나 100% 에서 빼며(`calc()` 안의 anchor 함수도 `CssCalc.mapAnchors` 로 같이), `anchor-size()` 와 너비/높이는 축이 바뀌면 맞바뀌고, 자기 정렬은 같은 축을 뒤집으면 `start`/`end` 와 `left`/`right` 가 바뀌고 축이 바뀌면 `align-self` 와 `justify-self` 가 맞바뀌며, `position-area` 의 트랙은 변의 순열로 옮겨진다(`PositionTry.flipArea`).
- **적용**(`AbsoluteLayouter`): 상자를 기본 스타일로 배치하고(`trial`) margin box 가 inset-modified containing block(포함 블록의 padding box 또는 위치 영역에서 `auto` 가 아닌 오프셋만큼 줄인 사각형)에 완전히 들어가지 않으면(허용 오차 0.05px) 대안을 순서대로 같은 방법으로 배치해 넘치지 않는(그리고 포함 블록 크기가 음수로 고쳐지지 않은) 첫 대안을 쓴다. 이름 없는 규칙이거나 규칙이 없는 이름의 항목은 건너뛴다. `position-try-order` 는 기본 스타일을 포함한 대안마다 포함 블록만 정해(배치 없이) 그 너비/높이(`most-block-size`/`most-inline-size` 는 쓰기 모드에 따라)로 안정 정렬한다. 모든 대안이 넘치면 기본 스타일을 다시 배치해 상태를 되돌린다. `position-try` 단축 속성(`Shorthands.PositionTry`)은 순서와 대안을 함께 정하고 직렬화한다.
- **`position-visibility`**(`AbsoluteLayouter.applyVisibility`): `no-overflow`(대안을 쓴 뒤에도 넘치면 숨김), `anchors-valid`(대체값 없는 `anchor()`/`anchor-size()` 의 앵커가 없으면 숨김), `anchors-visible`(초기값: 기본 앵커가 `position-visibility` 로 숨겨졌거나 앵커와 포함 블록 사이의 상자가 넘침을 잘라 앵커가 그 padding box 밖에 완전히 있으면 숨김. 앵커를 가진 상자에만 검사), `always`. 숨김은 `LayoutBox.positionHidden` 과 상자와 모든 자손의 `visibility` 를 `hidden` 으로 고쳐 쓰는 것이다(자손이 `visible` 로 되돌릴 수 없음).
- **`anchor-scope`**(`AnchorPositioning.Lookup.inScope`): 앵커 이름을 가진 상자나 그 조상 중 이름을 제한한 요소(`all` 이나 이름 목록)가 있으면 배치 상자가 그 서브트리 안에 있어야 하고, 배치 상자나 그 조상 중 이름을 제한한 요소가 있으면 앵커가 그 서브트리 안에 있어야 한다.
- **`anchor-center`**(`AnchorPositioning.withAnchorCenter`): `justify-self`/`align-self`/`place-self` 가 `anchor-center` 이고 기본 앵커가 있으면 그 축에서 앵커 가운데에 놓는다(auto 오프셋과 마진은 0, 포함 블록 안으로 되돌림). 위치 영역과 함께면 그 영역의 해당 축 정렬만 바뀐다. 한 축만 요구하면 다른 축은 평소대로 배치한다(`Area.active`). 플렉스/그리드에서는 `center` 로 해석한다(`FlexLayouter.words`).
- 시험: `PositionTryLayoutTest` 12(예상 좌표는 규격 문장과 3x3 격자에서 손으로 계산: 뒤집기별 위치, `flip-start` 가 상자의 너비와 높이도 맞바꿈, 인셋과 `anchor()` 와 `calc()` 와 마진과 백분율, 이름 있는 규칙과 이름+전술, 나중 규칙, 없는 이름, 위치 영역 항목, 맞는 것이 없을 때 기본 스타일 유지, 순서와 단축 속성, 가시성 네 값, 앵커 범위, `anchor-center`, 순열과 트랙 뒤집기 단위 시험), `PositionTryPropertiesTest` 6(문법의 유효/무효, 단축 속성, `@position-try` 의 걸러내기), `CssomModernRulesTest.testPositionTryRule`.

### 32.7 체크 후보 판단(P10-08-d, P10-08)

**P10-08-d 는 체크하지 않는다**(체크 후보 아님). 이 항목이 모았던 것 중 WOFF2 와 `position-try-*` 계열은 이번에 구현으로 닫았으나 `font-display` 의 block 기간의 보이지 않는 텍스트(글자를 그리지 않는 상태와 기간 만료 때의 다시 그리기 타이머)가 남았고, 모든 CSS 모듈이 구현이거나 근거 있는 예외로 확정되었는가에 대한 답은 아직 아니오이다. 이번에 구현으로 닫은 것: `resize`, `shape-outside` 이미지 모양과 `shape-image-threshold`, SVG 요소 참조 세 가지, 세로쓰기 `text-emphasis`, 다단 `box-decoration-break: clone`, `calc()` 안 anchor, 암묵 앵커, `font-display` 기간, WOFF2(Brotli 포함), `@position-try`/`position-try-fallbacks`/`position-try-order`/`position-try`/`position-visibility`/`anchor-scope`/`anchor-center`. 따라서 **P10-08 상위 항목도 체크하지 않는다**. 구조적으로 불가능해 새 명시적 예외(EXC-14 이상)로 정한 것은 없다(남은 항목은 모두 구현할 수 있다). §31.5 의 목록 중 이번에도 건드리지 않은 것: `cross-fade()`/`element()`, `::file-selector-button`/`::backdrop`/`::highlight()`, `@font-feature-values`, `contain-intrinsic-*`, Motion Path 의 일부와 `element.animate()`.

### 32.8 조정자 요청

1. `docs/FEATURE_MATRIX.md` 13절의 축별 개수표를 갱신해 주세요(CSS-13/15/32/34/35/36/39/46 의 한계가 줄었다; 행은 아직 `부분 구현`).
2. `docs/ARCHITECTURE.md`: `layout.Resizing`/`core.PageResize`/`render.DisplayCommands.Resizer`(resize), `layout.ShapeImage`·`ShapeImageSource`(shape-outside 이미지의 알파 마스크 공급 계약; `render.ImageReplacedResolver` 구현), `media.image.svg.SvgReference`/`render.SvgReferences`(CSS 의 SVG 요소 참조), `render.VerticalTextCommands.EmphasisColumn`, `layout.MulticolLayouter` 의 clone 장식 계산, `css.values.CalcNode.Anchor`/`CssCalc.withAnchors`/`CssCalc.mapAnchors`, `layout.AnchorPositioning.Lookup.defaultRect`/`inScope`/`hidden`, `layout.WebFontSet.TimedLoader`/`core.PageResources.fontTimes`(font-display), **`media.font.Brotli`/`BrotliData`/`BrotliException`/`Woff2`(WOFF2), `tools/brotlidict`(도구 목록에 한 줄: RFC 7932 에서 Brotli 사전과 표를 만드는 Java 6 도구), `css.cssom.PositionTryRule`, `layout.PositionTry`/`PositionTrySource`/`AbsoluteLayouter` 의 시도 배치(`trial`/`fallback`/`applyVisibility`)**를 새 절/표에 추가해 주세요.
3. AGENTS.md P10-08-d 는 체크하지 않는다(32.7). 남은 일을 새 하위 항목으로 나누는 것을 권한다: `P10-08-g` `font-display` block 기간의 보이지 않는 텍스트(글자를 그리지 않는 상태와 기간 만료 때의 다시 그리기 타이머). WOFF2(`P10-08-e`)와 `position-try-*`(`P10-08-f`)는 이번에 구현했으므로 체크 후보로 판단해 주세요: 근거는 32.3(시험 통과), 32.4 와 32.5(**브라우저가 만든 WOFF2 로는 확인하지 못했다**는 한계), 32.6.
4. 동작 변경 알림(PROGRESS 에 기록 권장): ① `filter: url(#id)`/`clip-path: url(#id)`/`mask-image: url(#id)` 가 이제 같은 문서의 SVG 요소를 적용한다(없는 `mask` 참조는 상자를 가림: 이전에도 같음; 없는 `filter` 참조는 필터 없음이지만 쌓임 맥락). 이전의 "`url()` 하나라도 있으면 `FilterSpec.of` 가 null" 이 바뀌어 `FilterPaintTest.testInvalidAndUrl` 을 고쳤다. ② `font-display: optional`/`fallback` 의 글꼴이 늦게 오면 쓰지 않는다(이전에는 항상 쓰고 다시 배치). ③ `overflow` 가 hidden/scroll/auto 이고 작성자가 `resize` 를 쓴 블록은 오른쪽 아래에 핸들을 그린다. ④ **`position-visibility` 의 초기값이 `anchors-visible` 이므로 기본 앵커(`position-anchor` 이름, `position-area`)를 쓰는 절대 배치 상자는 그 앵커가 중간 상자의 넘침 자르기 밖에 완전히 있으면 이제 숨겨진다**(이전에는 보였다). ⑤ `@font-face` 의 `format('woff2')` 소스를 이제 요청하고 읽는다(이전에는 요청 없이 건너뜀); `font-format(woff2)` 는 참이다. ⑥ `align-self`/`justify-self` 등에 `anchor-center` 를 쓸 수 있고(플렉스/그리드에서는 `center`), 새 속성 `position-try-fallbacks`/`position-try-order`/`position-visibility`/`anchor-scope` 와 단축 속성 `position-try` 가 파싱된다.
5. `StaticStateAuditTest`: 이 작업은 새 정적 상태를 만들지 않았다(`SvgReference` 와 `SvgReferences` 는 인스턴스 상태이고 `DisplayListBuilder` 가 만든 시점마다 가진다; `PositionTryRule.Accepted.IDS` 는 변하지 않는 숫자 집합 한 개를 처음 쓸 때 만드는 `final` 정적 필드이며 컴포넌트를 참조하지 않는다. 전체 회귀가 처음에 이것을 비-final 정적 캐시로 두었다고 `StaticStateAuditTest` 로 잡아냈고 고쳤다).
6. 실제 창 확인 목록(P22-10)에 `resize` 핸들과 끌기(실제 OS 마우스), 세로쓰기 강조점 모양, `filter: url(#id)` 의 모습을 더해 주세요. 이번 작업도 같이: WOFF2 글꼴로 꾸민 페이지의 글자 모양(실제 창), `position-try-fallbacks` 로 뒤집힌 팝오버의 위치, `position-visibility` 로 숨겨진 상자 — 모두 화면 밖 화소/좌표 시험만 했고 실제 Swing 창에서 눈으로 확인하지 않았다.
7. `.gitattributes`: 이진 시험 자료(`*.raw`, `*.br`, `*.woff2`, `*.ttf`)의 줄 끝 변환을 막는 줄을 더했다(`autocrlf=true` 환경에서 시험 자료가 깨지는 것을 막음). 병합 때 다른 작업자가 같은 파일을 고쳤다면 합쳐 주세요.
8. 새 이진 시험 자료: `tests/resources/org/duckdns/hjow/swingwebpanel/media/font/brotli/`(Node.js 가 만든 `.br` 와 그 입력 `.raw`, 실제 글꼴 없음)와 `woff2/`(스크립트가 만든 합성 글꼴 `synthetic.ttf` 와 그 WOFF2 세 개, 합 약 110KB). **해결함(조정자, 2026-11-19, 32.10)**: 처음에 넣은 Lucida 글꼴의 WOFF2 네 개와 Brotli 스트림 하나는 저장소와 이 브랜치의 이력에서 지웠다.

### 32.9 최종 전체 회귀

모두 JDK 6u45(`scripts/jdk6.sh`), 이 PC, `BUILD_DIR=build/full sh test.sh`(백그라운드 실행, 로그 `build/fullrun-l.txt`, `build/fullrun-l2.txt`)이다.

| 실행 | 기준 | 결과 |
| --- | --- | --- |
| 1차 | 커밋 `20261117-951`(WOFF2 와 `position-try-*` 구현 직후) | **3625 통과 / 2 실패 / 14 건너뜀**(778 초). 실패 ①: `core.StaticStateAuditTest.testEveryStaticStateHolderIsReviewed`: `PositionTryRule#acceptedIds`(비-final 정적 캐시)를 검토하지 않았다고 잡았다 → 실제 결함이 아니라 설계 지침 위반으로 보고 `final` 홀더로 고쳤다(커밋 952). 실패 ②: `core.FontDisplayIntegrationTest.testOptionalFontThatArrivesAtOnceIsUsed` 60 초 시간 초과: 이 시험은 700KB 글꼴이 100ms 안에 도착해야 하는데 같은 시점에 PC 가 바빴다(같은 클래스를 바로 다시 돌렸을 때도 한 번 실패했고, 부하가 줄어든 뒤 단독 1회와 클래스 2회 모두 통과했다). 시간에 민감한 시험이며 이 작업이 바꾼 경로는 아니다(`optional` 판정은 요청 시작과 도착 시각의 차이만 본다). |
| 2차 | 커밋 `20261117-952`(고침 반영, 코드의 마지막 변경) | **3626 통과 / 1 실패 / 14 건너뜀**(690 초). 실패: `js.host.canvas.CanvasTextStyleTest.testKerning`("with a font that has kerning pairs ... Tahoma: none 350.57, normal 463.77"): 설치된 Tahoma 의 캔버스 글자 너비 측정이며 이 작업이 건드리지 않은 영역이다. 같은 클래스를 단독으로 다시 돌리면 통과한다(4건 통과). 이 PC 의 JRE 6u45 글꼴 관리자에서 캔버스 글자 측정이 불안정한 것은 §30.7 과 이 절 앞부분에 적은 기존 결함(`fontmanager.dll` 접근 위반)과 같은 계열로 보이나 원인은 확인하지 못했다. |

건너뜀 14건은 모두 이유를 적은 `Check.skip`(선택적 Opus 원본 시험 벡터 12건: 환경 변수로 디렉터리를 줘야 하며 이 PC 에 없음, 이 PC 에 Java Sound 믹서가 없어서 건너뛰는 실제 오디오 라인 시험 2건)이며 이 작업이 만든 것은 없다(`Woff2Test` 는 원본 JRE 글꼴이 있어 건너뛰지 않았다). **마지막 전체 회귀가 완전히 깨끗하지는 않다**: 2차에 실패한 시험 1건은 단독 재실행에서 통과했으나, 같은 실행에서의 통과가 아니므로 "전부 통과"라고 쓰지 않는다. JVM 이 죽는 접근 위반(`hs_err_pid*.log`)은 두 실행 모두 없었다.

### 32.10 Lucida 글꼴 파생 자료 제거(조정자, 2026-11-19)

- 이유: JRE 6 의 `lib/fonts` 에 든 Lucida 글꼴(Bigelow & Holmes)은 Java 와 함께 쓰도록만 허락된 것이어서, 그것을 WOFF2/Brotli 로 바꾼 파일을 저장소에 두지 않는다. 처음 넣은 `woff2/LucidaBrightDemiBold-{xf,null,meta}.woff2`, `woff2/LucidaTypewriterBold-xf.woff2`, `brotli/font-LucidaBrightDemiBold.{br,txt}` 를 지우고 **이 브랜치의 이력에서도 지웠다**(`git filter-branch --index-filter`, 미푸시 브랜치이므로 가능).
- 바꾼 시험 자료: `woff2/` 는 `generation/make-woff2-vectors.js` 가 직접 만드는 합성 글꼴 `synthetic.ttf`(32,748바이트)와 그것을 참조 Brotli 인코더로 압축한 `synthetic-{xf,null,meta}.woff2`, `brotli/` 는 실제 글꼴 대신 직접 만든 150,000바이트 이진 입력 `structured.raw` 를 글꼴 모드로 압축한 스트림 둘(`structured-2-q11-w22.br`, `structured-2-q5-w16.br`). 두 스크립트를 다시 실행하면 저장소의 파일과 바이트 단위로 같은 파일이 나오는 것을 확인했다(Node.js v26.13.0).
- 바꾼 시험: `Woff2Test.testRealFontsDecodeToTheOriginal` 를 둘로 나눴다. `testReferenceFilesDecodeToTheSyntheticFont`(참조 Brotli 로 압축한 합성 글꼴 WOFF2 세 개를 원본과 테이블별·글리프별로 비교; 합성 글꼴은 쓸 수 있는 `cmap` 이 없어 Java2D 윤곽선 비교는 하지 않음)와 `testJreFontsDecodeToTheOriginal`(실행 중인 JRE 의 `LucidaBrightDemiBold.ttf`, `LucidaTypewriterBold.ttf`, `LucidaSansRegular.ttf` 를 시험 때 `Woff2Builder` 로 변환(압축하지 않은 Brotli 블록)해 원본과 테이블별·글리프별·Java2D 윤곽선과 진행 폭으로 비교, 셋 다 없으면 `Check.skip`). `BrotliTest.testTrueTypeFont` 는 `testLargeBinaryInput` 으로, `WebFontTest.testWoff2SourceIsDecoded` 와 `WebFontIntegrationTest.testWoff2FontIsUsedByThePage` 는 JRE 글꼴을 시험 때 WOFF2 로 바꿔 쓰도록(없으면 건너뜀) 바꿨다. 도우미 `tests/src/org/duckdns/hjow/swingwebpanel/media/font/Woff2Fonts.java`.
- 잃은 것(정직하게): 실제 글꼴 크기의 테이블 데이터를 **참조 Brotli 인코더가 압축한** WOFF2 는 이제 없다(실제 글꼴은 압축하지 않은 Brotli 블록으로만 디코더를 거친다). 압축된 Brotli 의 실제 크기 입력은 `structured.raw`(150,000바이트, 두 품질)와 기존 긴 텍스트가, 압축된 WOFF2 테이블 데이터는 합성 글꼴이 맡는다. 32.3 과 32.5 의 앞선 시험 수와 "실제 JRE 글꼴 둘" 표현은 이 변경 전의 기록이다.

### 32.11 병합 뒤 전체 회귀(조정자, 2026-11-19)

Lucida 파생 자료 제거(커밋 `20261119-001`)와 master 병합(`20261119-002`) 뒤 그 worktree 에서 `BUILD_DIR=build/coord sh test.sh`(JDK 6u45): **3644 통과 / 0 실패 / 14 건너뜀**(742 초). 건너뜀은 선택적 Opus 원본 벡터 12건과 Java Sound 믹서가 필요한 실제 오디오 라인 시험 2건이다. master 를 이 상태로 빨리 감기했다.

### 32.12 독립 인코더(fontTools)의 WOFF2 (조정자, 2026-11-19)

32.5 의 "인코더와 디코더를 같은 사람이 썼다"는 한계를 줄이려고, 널리 쓰이는 글꼴 도구 fontTools 4.68.0(`fontTools.ttLib.woff2`, brotli 1.2.1, Python 3.13.1; 내려받아 이 PC 의 임시 가상 환경에서만 실행, 제품·시험 실행에는 필요 없음)로 합성 글꼴 `synthetic.ttf` 를 세 가지로 인코딩했다: `glyf`/`loca` 와 `hmtx` 변환, `glyf`/`loca` 만 변환, 변환 없음(`tests/resources/org/duckdns/hjow/swingwebpanel/media/font/woff2/synthetic-fonttools-{xf,glyf,null}.woff2`, 생성 `generation/make-woff2-fonttools.py`, 다시 실행하면 같은 바이트). 그 인코더는 삼중 인코딩, 플래그 반복, 경계 상자 비트맵, Brotli 설정을 스스로 고른다. 새 시험 `Woff2Test.testFontToolsFilesDecodeToTheSyntheticFont` 는 세 파일이 합성 글꼴로 풀리는지(다른 테이블은 바이트 단위, 글리프는 점·플래그·명령·합성 기록·상자 단위) 확인하며 JDK 6u45 에서 통과했다. 차이 하나: fontTools 는 합성 글꼴 `head` 의 작은 임의 시각 값을 유닉스 시각으로 읽어 바꿔 쓰므로 이 파일들에서는 `created`/`modified` 를 비교하지 않는다. 브라우저가 만든 WOFF2 는 여전히 없다.

## 33. P10-08-g `font-display` 의 block 기간 (조정자, 2026-11-19)

32.4 에 남았던 "block 기간의 보이지 않는 텍스트"를 구현했다. 기준: CSS Fonts Module Level 4 4.8 절(`font-display`; https://www.w3.org/TR/css-fonts-4/, 기능 표 CSS-13 의 판본): "If the font face is not loaded, any element attempting to use it must render an invisible fallback font face"(block 기간), swap 기간에는 대체 글꼴을 보인다.

### 33.1 동작

| `font-display` | block 기간 | 기간 중 | 기간 뒤(글꼴 아직 없음) |
| --- | --- | --- | --- |
| `auto`, `block`, 모르는 값 | 3초(`WebFontSet.LONG_BLOCK_MS`; 규격 권장, `auto` 는 주요 브라우저처럼 `block`) | 대체 글꼴의 자리를 차지하고 그리지 않음 | 대체 글꼴로 그림, 글꼴이 오면 바꿈 |
| `fallback` | 100ms(`SHORT_BLOCK_MS`) | 위와 같음 | 3초 swap 기간 안에 오면 바꿈(32절의 기간 모델) |
| `optional` | 100ms | 위와 같음 | 바꾸지 않음(32절) |
| `swap` | 없음 | 곧바로 대체 글꼴로 그림 | 오면 바꿈 |

- block 기간은 요청 계층이 잰 글꼴 요청 시작(`PageResources.fontTimes` 의 시작 시각)부터이다. 시각을 모르는 로더(`TimedLoader` 가 아닌 것)는 막지 않는다(이전 동작).
- `font-family` 목록에서 처음 만난 대기 중인 웹 패밀리가 block 기간이면 그 뒤에서 고른 글꼴은 보이지 않는다. 그보다 앞에 쓸 수 있는 패밀리가 있으면 그것을 보이게 쓴다(웹 패밀리까지 가지 않음).
- 보이지 않는 텍스트는 글자, 밑줄 등 장식, 강조점을 그리지 않는다(Chromium 이 보이지 않는 대체 글꼴의 텍스트 조각 그리기를 통째로 건너뛰는 것과 같은 선택). 배치·선택·히트 테스트·스크립트의 크기 조회는 대체 글꼴 그대로이다.
- 기간이 끝나면 페이지가 스스로 다시 렌더링한다: 렌더링마다 `WebFontSet.takeBlockDeadline()` 이 가장 이른 끝을 알려 주고 `PagePresentation.scheduleBlockEnd` 가 애니메이션 프레임 타이머(데몬 `java.util.Timer`, 페이지 정리 때 취소)로 그 시각에 무효화한다. 한 번에 가장 이른 끝 하나만 기다린다.

### 33.2 변경 파일

- 제품: `layout/{WebFontSource(blockedUntil), WebFontSet(LONG_BLOCK_MS, blockedUntil, takeBlockDeadline, blockPeriod), FontResolver(resolve 의 block 출력 인수), InlineLayouter(Seg.invisible), LayoutBox(fontBlocked, isFontBlocked)}`, `render/DisplayListBuilder`(막힌 조각은 그리지 않음), `core/PagePresentation`(scheduleBlockEnd).
- 시험(새): `render/FontBlockPaintTest` 5(기간 값, 자리 유지와 화소 0, 기간 뒤 그림·`fallback` 의 짧은 기간, `swap`·시각 없는 로더·앞의 패밀리, 선언되지 않았거나 실패한 패밀리), `core/FontBlockIntegrationTest` 3(실제 컴포넌트와 HTTP: 서버가 글꼴을 붙잡은 동안 처음과 요청 2초 뒤 화소 0, 기간이 끝나면 다른 일 없이 대체 글꼴로 나타남, `swap` 은 곧바로, 기간 중 `dispose`).
- 확인: 다시 렌더링을 예약하지 않게 바꾼 `PagePresentation` 으로는 통합 시험이 "after the block period the text is painted" 에서 실패한다(시험이 예약을 실제로 확인함).

### 33.3 한계

- 목록 표식(`::marker`, `list-style`), `text-overflow` 의 줄임표, 폼 컨트롤 글자, `initial-letter` 는 글꼴을 따로 고르므로 block 기간에도 보인다.
- 시간은 시스템 시계(`System.currentTimeMillis()`)이다(32.4 와 같음). 통합 시험은 아주 바쁜 컴퓨터에서 첫 그림이 기간 뒤에 나오면 보이지 않음의 판정을 건너뛰고 그 사실을 표준 오류에 적는다(나타남은 언제나 확인).
- 실제 Swing 창에서 눈으로 확인하지 않았다(화면 밖 그림과 컴포넌트 `paint` 만).

## 34. P10-08-h 의 CSS 부분 (CSS-M, 2026-11-19, 커밋 `20261119-101` 부터)

P10-08-h 중 Web Animations(`element.animate()`, 작업자 WEBANIM)를 뺀 나머지를 구현했다: CSS Images 4 의 `cross-fade()`·`image()`·`element()`, `::file-selector-button`, 최상위 계층과 `::backdrop`, `::highlight()` 와 CSS Custom Highlight API 1, `@font-feature-values` 와 `font-variant-alternates` 의 글리프 효과, `contain-intrinsic-*` 의 남은 부분, Motion Path 1 의 남은 부분. 구조적으로 불가능해 새 명시적 예외로 올릴 항목은 없다(34.5 의 한계는 모두 구현할 수 있는 후속이다).

### 34.1 조회한 규격 (도구 `tools/webfetch`, 실행 JRE: Zulu 25, 조회일 2026-11-19)

| 규격 | URL | 판본(`Last-Modified`) | 읽은 곳 |
| --- | --- | --- | --- |
| CSS Images Level 4 | https://www.w3.org/TR/css-images-4/ | Wed, 24 Sep 2025 | 2.5 `image()`(대체 색, 조각, 단색, 방향), 2.6 `cross-fade()`(문법, 정규화, 2.6.1 크기, 2.6.2 그리기, 2.6.3), 2.7 `element()`(그려지는 요소, 그림 원본, 2.7.3 순환) |
| CSS Custom Highlight API Level 1 | https://www.w3.org/TR/css-highlight-api-1/ | Wed, 15 Dec 2021 | 3.1~3.2(Highlight, HighlightRegistry), 4.1~4.2.6(선택자, 계단식, 그리기, 우선순위), 5.1~5.2(다시 그리기, 정적 범위) |
| CSS Pseudo-Elements Level 4 | https://www.w3.org/TR/css-pseudo-4/ | Fri, 27 Jun 2025 | 3.2(하이라이트에 적용되는 속성), 3.5(하이라이트 상속), 3.6(그리기), 5.1 `::file-selector-button` |
| CSS Positioned Layout Level 4 | https://www.w3.org/TR/css-position-4/ | Tue, 07 Oct 2025 | 3.1 최상위 계층 스타일, 3.2 `::backdrop`(UA `position: fixed; inset: 0`), 3.3 |
| HTML Living Standard 렌더링 | https://html.spec.whatwg.org/multipage/rendering.html | Thu, 19 Nov 2026 | `dialog`, `dialog:modal`, `dialog::backdrop`(`rgba(0,0,0,0.1)`) UA 규칙 |
| CSS Fonts Level 4 | https://www.w3.org/TR/css-fonts-4/ | Thu, 12 Nov 2026 | 4.9.1(`font-display` 서술자), 6.8 대응 표, 6.9.1~6.9.2(`@font-feature-values` 문법, 오류 처리, 여러 값), 6.12, 9.1(`CSSFontFeatureValuesRule`, 맵, 직렬화) |
| CSS Box Sizing Level 4 | https://www.w3.org/TR/css-sizing-4/ | Fri, 04 Sep 2026 | 5.2 `contain-intrinsic-*`(값, 논리 속성, 단축 속성), 5.2.1 마지막으로 기억한 크기 |
| CSS Motion Path Level 1 | https://www.w3.org/TR/motion-1/ | Thu, 31 Oct 2024 | 2.1(`<coord-box>`, `<url>`, 상자만이면 `inset(0 round X)`), 2.1.3, 3(동등한 경로) |

### 34.2 항목별 상태

| 항목 | 상태 | 구현 | 시험(JDK 6u45) |
| --- | --- | --- | --- |
| `cross-fade()` | 구현 | 문법(`css/values/Terminals`: 이미지나 색 하나와 0~100% 백분율 많아야 하나, 틀리면 선언 무효), 정규화(빠진 백분율은 남은 몫을 나눔, 합 100% 초과는 줄임, 미만은 투명 나머지), 고유 크기는 고유 크기 있는 이미지 입력의 가중 평균(색과 크기 없는 입력 제외), 화소는 프리멀티플라이드 sRGB 가중 평균(`render/GeneratedImages.CrossFadePicture`), 중첩, 옛 `-webkit-cross-fade(A, B, p)` | `ImageFunctionsPaintTest` 9 중 testSyntax, testColorsAreAveraged, testNaturalSizeOfImages, testRepeatedTiles |
| `image()` | 구현 | 색만이면 고유 크기 없는 단색, URL 이 잘못되었거나 불러오는 중이면 대체 색, 색이 없으면 잘못된 이미지, `#xywh=[pixel:|percent:]x,y,w,h` 조각(모르는 조각은 잘못된 이미지), 메타데이터 방향 항상 적용, `ltr`/`rtl` 이 상자의 방향과 다르면 가로로 뒤집음 | testImageFunction |
| `element()` | 구현 | 그려지는 요소는 자손과 함께 쌓임 맥락으로 그리고(자신의 변환·고정 배치·sticky 무시, 조상 변환은 훑지 않음) 장식된 경계 상자(조각의 border box + `border-image-outset`)가 고유 크기, 인라인 요소는 조각의 장식과 그 텍스트, 그려지지 않는 `img`(이미지 도착)와 `canvas` 는 그림 원본, 없는 요소·그 밖의 요소·순환(참조자나 그리고 있는 그림의 노드의 조상이거나 자신)은 잘못된 이미지, 중첩 깊이 8(`DisplayListBuilder.elementPicture`, `PaintWalker.walkAsImage`) | testElementImage, testElementInvalidAndPaintSources |
| `<image>` 를 받는 곳 | 구현(일부) | `background-image`, `mask-image`(배경 레이어), `border-image-source`, `mask-border-source`(아홉 조각), **`list-style-image`**(새로 구현: 바깥쪽 표식, 모든 `<image>`, 고유 크기가 없으면 1em 정사각형, 아래쪽이 기준선, `list-style-type: none` 이어도 보임, 없는 이미지는 `list-style-type`) | testBorderImageAndMask, testListStyleImage |
| `::file-selector-button` | 구현 | `input[type=file]` 단추를 그 스타일로 그림. 작성자가 **선언한** 속성만 쓰고(`StyleResolver.getPseudoDeclared`) 나머지는 그리는 쪽의 모양: 배경, 글자색, 위쪽 border 너비·스타일·색, 왼쪽 위 반지름, 왼쪽·오른쪽 padding, `margin-inline-end`, 글꼴 크기·굵기, `display: none`, `visibility` (`render/ControlPainter.fileButtonLook`) | `PseudoElementsMorePaintTest` 3 중 testFileSelectorButton |
| 최상위 계층과 `::backdrop` | 구현 | `showModal()` 이 `Document.addToTopLayer`, `close()` 가 빼기, `:modal`(열린 대화상자 + 최상위 계층), WHATWG UA 규칙(`dialog` 절대 배치·가운데·border·padding, `dialog:modal` 고정 배치, `::backdrop` 고정·inset 0, `dialog::backdrop` 배경), 최상위 계층의 요소와 `::backdrop` 을 루트의 끝에 쌓임 맥락으로 만들고 가장 높은 z 로 그림, 표시 안 되는 조상 안이면 그리지 않음, 절대 배치의 `width/height: fit-content|min-content|max-content` | testModalDialogAndBackdrop, testStylesAndNonModalDialog, `ModalDialogTest` 1 |
| `::highlight()` 와 Highlight API | 구현 | `Highlight`(setlike, `priority`, `type`), `CSS.highlights`(maplike), `::highlight(이름)` 선택자(이름은 대소문자 구분), 하이라이트 계단식(정하지 않은 하이라이트 속성은 상속 속성이 아니어도 부모의 하이라이트에서, 루트의 `color` 는 `currentcolor`; `StyleResolver.getHighlightStyle`), 텍스트 조각마다 배경을 깔고 덮는 가장 위 층의 색과 장식으로 다시 그림(우선순위, 같으면 등록 순서, `currentcolor` 는 아래 층 색), 접힌 범위·다른 문서·올바르지 않은 정적 범위 무시, 한 하이라이트의 겹치는 범위는 한 영역, 등록부·하이라이트·등록된 살아 있는 범위의 변경마다 다시 그림 | `HighlightPaintTest` 5, `HighlightApiTest` 4 |
| `@font-feature-values` | 구현 | 전문은 이름 있는 패밀리(일반 패밀리면 규칙 무효), 일곱 기능 값 블록과 `font-display` 서술자, 6.9.1 의 오류 처리(모르는 at-규칙·종류에 맞지 않는 선언 무시, 같은 블록 합치기, 이름은 대소문자 구분), CSSOM `CSSFontFeatureValuesRule`(`type` 14, `fontFamily`, 일곱 맵)과 `CSSFontFeatureValuesMap`(maplike, `set` 검사), 9.1 직렬화, `font-display` 서술자는 서술자가 없는 그 패밀리의 `@font-face` 면의 기본 표시 정책(4.9.1; `WebFontSet.displayOf`) | `FontFeatureValuesCssomTest` 3, `FontFeatureValuesLayoutTest` 의 `defaultDisplay` 확인 |
| `font-variant-alternates` 함수 값 | 구현 | 이름을 그 패밀리의 `@font-feature-values` 에서 찾아 `salt`·`ss01~99`·`cv01~99`·`swsh`+`cswh`·`ornm`·`nalt`·`hist` 로(`FontFeatures.glyphFeatures`), `font-feature-settings` 의 같은 태그가 이김. **글리프 효과**: 웹 글꼴의 `GSUB` 단일 치환(종류 1, 형식 1·2), 대체 치환(종류 3, 기능 값 = 대체 번호), 확장(종류 7)을 직접 읽어 `cmap` 이 대체 글리프를 가리키는 글꼴 사본을 만들어 쓴다(`layout/GlyphSubstitution`, `WebFontSet.withAlternates`). 재기와 그리기가 모두 대체 글리프 | `FontFeatureValuesLayoutTest` 2(합성 글꼴 `SyntheticGsubFont`) |
| `contain-intrinsic-*` 의 남은 부분 | 구현 | 단축 속성 `contain-intrinsic-size` 를 `[ auto? [ none | <length> ] ]{1,2}` 로 고침(이전 `Pair` 는 `auto 100px` 를 읽지 못했다), 논리 개별 속성 `contain-intrinsic-inline-size`/`-block-size`(속성 id 316, 317, 쓰기 모드에 따라 너비/높이), 마지막으로 기억한 크기(배치 뒤 `auto` 가 있고 크기 격리가 없는 요소의 안쪽 크기를 문서에 약하게 기억, `auto` 가 없어지면 잊음, 내용을 건너뛰는 상자(`content-visibility: hidden`)가 그 크기를 씀) | `ContainIntrinsicSizeTest` 3 |
| Motion Path 1 의 남은 부분 | 구현 | `<coord-box>` 기준 상자(포함 블록의 content/padding/border/margin box, `fill-box` 는 content, `stroke-box`·`view-box` 는 border; `ray()` 와 위치에도), 상자만이면 포함 블록의 `border-radius`(안쪽 상자는 줄이고 margin box 는 늘림)로 `inset(0 round X)` 이며 윗변 모서리 뒤 왼쪽 끝에서 시계 방향, `url(#id)` 는 문서의 SVG 도형(`path rect circle ellipse line polyline polygon`)의 동등한 경로(좌표 상자 왼쪽 위가 원점, 백분율은 좌표 상자 기준), 도형이 아니면 `path("m 0 0")`(점 하나), 문법에 `url() coord-box` 를 허용, `offset-path` 보간(같은 종류·구조의 도형 함수, 명령이 같은 `path()`, `ray()`; 그 밖은 이산; `css/values/ShapeInterpolation`) | `MotionPathMoreLayoutTest` 4, 기존 `MotionPathLayoutTest` 1건의 기대를 규격대로 고침 |

### 34.3 변경 파일

- 제품(새): `render/GeneratedImages`, `render/HighlightOverlay`, `dom/Highlight`, `dom/HighlightRegistry`, `js/host/HighlightApi`, `css/cssom/FontFeatureValuesRule`, `js/dom/FontFeatureValuesBindings`, `layout/GlyphSubstitution`, `layout/SvgShapePaths`, `layout/MarkerImageSource`, `css/values/ShapeInterpolation`.
- 제품(가산 변경): `css/values/Terminals`(이미지 함수 문법), `render/{BackgroundLayers, BoxCommands, BorderImageLayer, DisplayListBuilder, PagePainter(paintCommands), ControlPainter, ControlSnapshot, ImageReplacedResolver}`, `layout/{PaintWalker(walkAsImage, 최상위 계층 z), BoxBuilder(최상위 계층, 파일 단추 스타일, 표식 이미지), BlockLayouter(표식 이미지, 기억한 크기), AbsoluteLayouter(fit-content), LayoutBox, LayoutResult(스타일 공급자, backdrop 색인 제외), LayoutEngine, StyleSource, CssStyleSource, Containment, FontFeatures, FontResolver, WebFontSet, WebFontSource, MotionPath}`, `css/cascade/{StyleResolver(getHighlightStyle, getPseudoDeclared, getFontFeatureValues), RuleIndex}`, `css/selector/{SelectorParser(::highlight()), PseudoClasses(:modal 45), SelectorMatcher, HtmlStates}`, `css/cssom/{CssRule(107, CSSOM 14), StyleSheetBuilder, UaStyleSheet(dialog 규칙)}`, `css/props/{Prop(316, 317), PropertyTable, LogicalProps, Shorthands(ContainIntrinsicSize)}`, `css/anim/Interpolator`(한 줄: `offset-path` 를 `ShapeInterpolation` 으로), `dom/Document`(하이라이트 등록부, 최상위 계층, 기억한 크기), `js/host/{CssNamespaceApi, PageScripts(onTaskEnd), ElementStates(showModal/close 의 최상위 계층)}`, `js/dom/CssomInterfaces`.
- 시험(새): `render/ImageFunctionsPaintTest` 9, `render/HighlightPaintTest` 5, `render/PseudoElementsMorePaintTest` 3, `js/host/HighlightApiTest` 4, `js/host/ModalDialogTest` 1, `js/host/FontFeatureValuesCssomTest` 3, `layout/FontFeatureValuesLayoutTest` 2(+ 합성 글꼴 만드는 `layout/SyntheticGsubFont`), `layout/ContainIntrinsicSizeTest` 3, `layout/MotionPathMoreLayoutTest` 4. 고친 시험: `layout/MotionPathLayoutTest.testComposition` 의 "url() 은 변환 없음" 기대(규격은 `path("m 0 0")`).
- 수정 전 소스에서의 실패: 위 새 시험은 모두 이번 기능을 직접 확인한다(예: 이전 `BackgroundLayers` 는 `cross-fade()` 를 그리지 않았고, `::highlight()` 는 선택자 오류, `@font-feature-values` 는 `UnknownRule`, `contain-intrinsic-size: auto 100px` 는 무효였다). 수정 전 소스로 다시 돌려 실패를 따로 확인하지는 않았다.

### 34.4 실행한 명령과 결과

- 항목별: `sh scripts/pkgtest.sh cssm "css layout render media js dom html net core content storage download @root" "<시험 패키지>" --filter <이름>` 과 같은 클래스로 `TestRunner --filter` 직접 실행. 결과: `ImageFunctionsPaintTest` 9/9, `HighlightPaintTest` 5/5, `HighlightApiTest` 4/4, `PseudoElementsMorePaintTest` 3/3, `ModalDialogTest` 1/1, `FontFeatureValues*` 5/5, `ContainIntrinsicSizeTest` 3/3, `MotionPath*` 14/14.
- 중간 회귀(커밋 101~103 뒤): `render css layout js/host` 패키지를 돌린 실행은 675 통과/0 실패까지 간 뒤 `js.host.canvas.CanvasTextStyleTest` 에서 알려진 JRE 6 `fontmanager.dll` 접근 위반으로 JVM 이 죽었다(render·css·layout 은 그 전에 모두 통과). `js.host` 만 따로 다시 돌려 443 통과/0 실패.
- 커밋 107(`@font-feature-values` 의 `font-display` 연결)은 전체 회귀가 시작된 뒤의 변경이라 `FontFeatureValuesLayoutTest` 2/2, `WebFontTest` 6/6, `FontBlockPaintTest` 5/5, `FontDisplay*` 4/4 만 따로 돌렸다.
- 소스 규칙: `sh tools/sourcecheck/run.sh <변경 경로>` 0 위반.
- 전체 회귀: 34.7 에 적는다.

### 34.5 한계 (숨기지 않음; 모두 후속으로 구현할 수 있다)

- **이미지 함수**: `content: <image>`(생성 콘텐츠의 이미지 자체가 이 엔진에 없다)와 안쪽(`inside`) 목록 표식의 이미지는 아직 없다(텍스트 표식을 씀). `shape-outside` 는 `cross-fade()`/`image()`/`element()` 를 이미지 모양으로 쓰지 않는다. `CSS.elementSources`(2.7.2)는 없다. `element()` 는 문서 전체를 다시 배치하지 않고 이미 배치된 상자를 그리므로 그 요소의 그림이 바뀌면(DOM 변경) 다음 렌더링에서 반영된다; `list-style-image: element()` 는 배치 전에 크기를 알 수 없어 텍스트 표식을 쓴다. `cross-fade()` 래스터는 CSS 픽셀 해상도(확대 시 늘림)이며 타일 하나가 4M 화소를 넘으면 줄여 만든다. 계산 값은 쓴 그대로이다(2.6.3 의 단순화·빠진 백분율 채우기를 계산 값에 반영하지 않음). SVG 그림 원본(`pattern`, 그라디언트 요소)은 `element()` 에서 잘못된 이미지이다.
- **`::file-selector-button`**: 단추 라벨은 고정 "Choose File" 이며 `content` 는 적용하지 않는다. 그 밖의 속성(그림자, 아래·옆 border 의 다른 값, `font-family`)은 그리지 않는다.
- **최상위 계층**: 모달 대화상자만 들어간다(Fullscreen API, popover 없음). 문서의 나머지를 비활성(inert)으로 만들지 않으며 Esc 로 닫는 동작도 없다. 최상위 계층의 요소를 작성자가 `position: static` 으로 바꾸면 규격처럼 absolute 로 계산하지 않고 흐름에 놓는다. 문서를 떠난 대화상자는 다음 렌더링에서 계층에서 빠진다(제거 단계의 즉시 처리 아님).
- **하이라이트**: 세로쓰기(회전) 텍스트, 목록 표식, 치환 요소(이미지)에는 그리지 않는다. `text-shadow` 는 엔진이 텍스트 그림자를 그리지 않아 적용되지 않는다. 장식은 위 층의 선이 아래 층의 선을 대신한다(규격은 층마다 그림). `getComputedStyle(el, '::highlight(x)')` 는 지원하지 않는다. `highlightsFromPoint` 같은 이후 판의 API 와 하이라이트 이벤트는 없다. 범위가 바뀐 것은 작업이 끝날 때 알아챈다(같은 작업 안의 중간 상태는 그리지 않음).
- **`@font-feature-values` 글리프 효과**: 웹 글꼴(`@font-face`)에만 효과가 있다(JRE 6 은 시스템 글꼴의 바이트를 공개 API 로 주지 않음 — 기능 표 EXC-11 과 같은 이유). 문맥 치환(종류 5, 6), 합자(종류 4), 위치 조정(GPOS)은 적용하지 않으며 스크립트별 언어 체계 대신 `DFLT`, 없으면 `latn` 의 기본 언어 체계를 쓴다. 같은 문자의 모든 출현이 같은 글리프가 된다(cmap 재작성이므로). 브라우저나 실제 글꼴로는 확인하지 못했고 합성 글꼴로만 확인했다.
- **`contain-intrinsic-*`**: `content-visibility: auto` 는 이 엔진에서 늘 "관련 있음"(30.2)이라 기억한 크기를 쓰는 경우는 `content-visibility: hidden` 뿐이다. 기억은 배치 직후에 하며 크기 관찰자의 전달 시점과 똑같지는 않다.
- **Motion Path**: `url()` 은 같은 문서의 SVG 도형만이며 도형의 `transform` 속성은 적용하지 않는다. `path()` 보간은 명령 글자가 같아야 하고 절대/상대 명령을 서로 바꾸지 않는다. 길이와 백분율이 섞인 도형은 보간하지 않는다(calc 로 섞지 않음).
- 실제 Swing 창에서는 확인하지 않았다(모두 화면 밖 그림과 배치·스크립트 시험).

### 34.6 조정자 요청

1. FEATURE_MATRIX: CSS-12 의 "한계: `cross-fade()`/`element()`/`image()` 미구현" 을 34.2 의 구현으로 바꾸고 34.5 의 이미지 함수 한계를 적는다(`list-style-image` 는 이제 구현, `content` 이미지는 남음). CSS-42 에 `::file-selector-button`, `::backdrop`(최상위 계층, `:modal`), `::highlight()` 와 Custom Highlight API 를 구현으로 더한다. CSS-13 에 `@font-feature-values` 와 웹 글꼴의 대체 글리프(EXC-11 중 대체 형태는 웹 글꼴에 한해 해소; 합자·숫자 모양·커닝은 그대로 EXC-11)를 적는다. CSS-38 에 `contain-intrinsic-*` 논리 속성·단축 속성 `auto`·마지막으로 기억한 크기를 더한다. CSS-44 의 Motion Path 한계(`url()`, `coord-box`, `round` 시작점)를 해소로 바꾸고 보간을 더한다. 13절 집계에 반영.
2. ARCHITECTURE: `render.GeneratedImages`(이미지 함수 그림), `render.HighlightOverlay` + `dom.Highlight/HighlightRegistry` + `js.host.HighlightApi`, `layout.GlyphSubstitution`(GSUB → cmap 재작성), `layout.SvgShapePaths`, `css.values.ShapeInterpolation`, `css.cssom.FontFeatureValuesRule`, `Document` 의 최상위 계층·하이라이트 등록부·기억한 크기를 적는다.
3. 병합: `Prop.COUNT` 는 317 이다(WEBANIM 의 330 이상 대역과 합칠 때 큰 값으로). `PropertyTable` 의 행 순서가 id 를 정하므로 316, 317 행 뒤에 WEBANIM 의 행이 오도록 맞춘다. `css/anim/Interpolator` 에 한 줄(`offset-path`)을 더했다. `js/host/PageScripts` 에 `onTaskEnd` 와 검사점 끝의 호출을 더했다. `dom/Document` 에 세 가산 필드를 더했다.
4. P10-08-h 와 P10-08 의 체크 후보 판단: 34.7.

### 34.7 전체 회귀와 체크 후보 판단

- 전체 회귀(커밋 106 까지의 소스, JDK 6u45): `BUILD_DIR=build/full sh test.sh > build/fullrun-m.txt 2>&1` → `=== 3703 passed, 1 failed, 12 skipped (1034362 ms) ===`. 건너뜀 12 건은 모두 선택적 Opus 원본 벡터(`SWP_OPUS_VECTORS` 없음)이다. 실패 1 건은 `css.PositionTryPropertiesTest.testPropertyIds` 의 "the number of properties (expected 315 but was 317)": 속성 수를 315 로 못박은 시험이라 이번에 속성 316, 317 을 더한 것의 직접적인 결과이다(간헐 실패가 아님). 그 단정을 `Prop.COUNT >= Prop.ANCHOR_SCOPE` 로 바꾸고 새 id 316, 317 의 확인은 `ContainIntrinsicSizeTest` 에 두었다. 같은 빌드로 두 시험 클래스만 다시 돌려 6/6, 3/3 통과(커밋 108). `core.StaticStateAuditTest` 는 통과(새 정적 상태 없음).
- 커밋 107 의 변경은 34.4 처럼 관련 시험만 따로 돌렸다.
- **P10-08-h 체크 후보 판단**: 이 작업이 맡은 CSS 부분(이미지 함수, 세 의사 요소와 Highlight API, `@font-feature-values`, `contain-intrinsic-*`, Motion Path)은 규격 판본과 대조해 구현하고 시험으로 확인했다. P10-08-h 의 남은 항목은 WEBANIM 의 `element.animate()` 뿐이므로, 그것이 끝나고 조정자가 34.5 의 한계를 "부분 구현의 한계"로 받아들이면 P10-08-h 는 체크 후보이다. 34.5 에는 구조적으로 불가능한 것이 없어 새 명시적 예외(EXC-14 이상)를 제안하지 않는다. 34.5 중 P10-08-h 의 목록 밖이던 기존 공백(`content: <image>`, 안쪽 표식의 이미지, `CSS.elementSources`)은 별도 후속 항목으로 두기를 권한다.
- **P10-08 상위 항목**: 이 판단은 조정자의 몫이다. CSS 쪽에서 남은 것은 34.5 의 한계(모두 구현 가능한 후속)이며 그중 무엇을 P10-08 을 막는 미구현으로 볼지에 따라 정해진다.
