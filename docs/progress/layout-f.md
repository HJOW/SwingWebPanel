# LAYOUT-F 진행 기록 (P10-04-f ::first-line, P10-05 @font-face, P10-06 transform, P10-07 transition/animation, P10-08 clip-path)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/{layout,render,css}/**`, `core/{PagePresentation,PageResources}.java`(애니메이션 틱과 웹 글꼴 서비스의 최소 연결), 이들의 시험, 이 파일.
브랜치: 격리 worktree(`worktree-agent-a279d9b13b9f1a150`). 커밋 번호는 `20261117-401` 부터.
시험 실행(격리, JDK 6u45): `sh scripts/pkgtest.sh layoutf "css html dom layout render core net storage content js media download @root" "layout render css core" --filter swingwebpanel`.
린트: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/layout src/org/duckdns/hjow/swingwebpanel/render src/org/duckdns/hjow/swingwebpanel/css src/org/duckdns/hjow/swingwebpanel/core tests/src/org/duckdns/hjow/swingwebpanel/layout tests/src/org/duckdns/hjow/swingwebpanel/render tests/src/org/duckdns/hjow/swingwebpanel/css tests/src/org/duckdns/hjow/swingwebpanel/core`.

**규격 조회에 관한 정직한 기록:** 이 작업은 사용 한도로 두 번 끊겼고 `tools/webfetch` 로 규격 본문을 다시 조회하지 못했다. 구현은 기능 표(FEATURE_MATRIX CSS-13/24/25/26/27/28/35/42)에 고정된 판본과 규격 알고리즘에 따랐다. 판본/장 번호는 각 클래스 Javadoc 에 적었으며 "조회 날짜"는 적지 않았다. 규격 본문 대조(특히 CSS Transitions 1 의 되돌림 단축 계수, Fonts 3 5.2 의 굵기 매칭 세부, Masking 1 5 절)는 후속 확인 과제이다.

## 작업 기록

### 2026-11-17 (2차) P10-05 @font-face, P10-07 transition/animation, P10-08 clip-path

**현재 작업 번호:** P10-05, P10-07, P10-08(부분)
**완료한 내용**
- **P10-07 transition/animation** — `css/anim/{AnimationEngine,Easing,Interpolator,AnimationClock,CssAnimationEvent}`, `css/cascade/StyleAnimator`, `StyleResolver`(`setAnimator`, `invalidateElement`, `computeDeclared`), `core/PagePresentation`(틱 + 16 ms 프레임 타이머).
  엔진은 `StyleResolver` 의 훅이다: 스타일을 계산할 때 이전 스타일과 비교해 바뀐 속성마다 전환을 시작하고(`transition-property` 의 `all`/이름/단축 이름, 목록 반복), `animation-name` 의 애니메이션을 시작하고(`@keyframes` 이름 조회, 마지막 규칙이 이김), 시계 시각의 값(전환이 애니메이션보다 우선)을 덮은 스타일을 돌려준다. 자손은 그 스타일을 상속한다.
  `tick()` 이 시간을 진행해 끝난 것을 정리하고, 값이 움직이는 요소의 캐시한 스타일을 버리고, 이벤트를 순서대로 발생시킨다: `transitionrun/start/end/cancel`, `animationstart/iteration/end/cancel`(`CssAnimationEvent`, 버블링, `elapsedTime` 초). 시계는 `AnimationClock`(시험은 손으로 정하는 시계). 완화: `linear/ease/ease-in/ease-out/ease-in-out/step-start/step-end/steps(n, jump-*)/cubic-bezier()`(넘침 포함), `linear(...)` 함수는 선형으로 취급. 보간: 숫자·길이·백분율·색(미리 곱한 알파)·`transform`(함수별, 안 되면 2D 분해 행렬)·같은 길이의 목록·`visibility`. 애니메이션: 키프레임 다중 선택자, 없는 from/to 는 기저 값, 키프레임 안의 `animation-timing-function`, `animation-play-state`(진행도 유지), direction 4 종, fill-mode 4 종, 소수/`infinite` 반복.
  JS 비활성은 이 엔진과 무관하게 동작한다(`setJavascriptEnabled(false)` 에서도 CSS 애니메이션이 계속된다는 통합 시험). 페이지 교체/폐기(`retire`)는 `AnimationEngine.dispose()` + 프레임 타이머 `cancel()`(스레드 종료를 시험으로 확인).
- **P10-05 @font-face** — `layout/{WebFontSource,WebFontSet,Woff}`, `FontResolver`(스레드 로컬 웹 글꼴 조회), `LayoutEngine.layout(..., WebFontSource)`, `PageRenderer.render(..., WebFontSource)`, `core/PageResources.fontBytes`(공통 요청 계층: 같은 캐시/헤더/쿠키/스킴 제한/한도 8 MiB, 목적 `RequestPurpose.FONT`), `PagePresentation`(규칙 수집, 읽힌 뒤 재배치).
  대상 형식: **TrueType**(`0x00010000`/`true`), **WOFF 1.0**(순수 Java `Inflater` 로 sfnt 복원: 헤더/디렉터리/순서/크기/길이 검증), **OpenType-CFF(`OTTO`)** 는 `Font.createFont` 를 시도하고 JRE 가 읽지 못하면(아래 "JDK 6u45 확인" 참조) 그 소스를 실패로 보고 다음 소스를 시도. **WOFF2, Embedded OpenType, SVG 글꼴, Type 1 은 미지원**(`format()` 힌트가 그것이면 요청 없이 건너뛰고, 서명이 WOFF2 이면 실패). `local()` 소스(설치된 패밀리), 여러 `src` 의 순차 시도, 실패하면 다음 후보/대체 글꼴. 로딩 중에는 다음 패밀리를 쓰고(swap) 도착하면 재배치. `font-weight`(범위 포함)/`font-style` 매칭(CSS Fonts 3 5.2 의 방향 규칙), 요청한 굵게/기울임이 면에 없으면 JRE 합성, `unicode-range`(텍스트 세그먼트 전체가 범위 안일 때만 그 면 사용). 페이지마다 최대 32 면.
  **교차 오리진 글꼴은 허용하지 않는다**(CORS 허용이 필요한데 P20-08-c 가 소켓 클라이언트로 `Origin` 을 보낼 수 있게 되기 전의 한계. 요청 자체를 하지 않는다. 클래스패스 페이지는 클래스패스 글꼴만).
- **개별 변환 속성 `translate`/`rotate`/`scale`**(CSS Transforms 2, 2D 형태만) — `Prop.TRANSLATE/ROTATE/SCALE`(179~181), `Transforms.individual`(행렬 = translate · rotate · scale · transform, 변환 원점 둘레), `Css.hasTransform` 에 포함(쌓임 맥락과 포함 블록), `Interpolator` 가 각도를 보간해 전환/애니메이션도 가능. `rotate: z 30deg` 는 2D 회전, 다른 축과 값 세 개 형태는 문법에서 무효. 시험: `TransformPaintTest.testIndividualTransformProperties`.
- **P10-08(부분) clip-path** — `layout/ClipPaths`, `css/props`(`clip-path` 속성 추가: `Prop.CLIP_PATH`=178, 값은 `none | [ <generic-function> | <ident> ]+`), `PaintWalker`/`DisplayListBuilder`/`PaintSequence`(그리기와 히트가 같은 모양). 기본 도형 `inset()`(+`round` 반지름 하나), `circle()`, `ellipse()`, `polygon()`(`nonzero/evenodd`), 기준 상자 `border-box`(기본)/`padding-box`/`content-box`/`margin-box`. `clip-path` 가 있으면 쌓임 맥락(opacity 처럼). 변환과 함께 쓰면 변환 그룹 안에서 잘린다.
- **P10-04-f / P10-06** 는 1 차 커밋 `20261117-401`(아래).

**변경 파일(2 차)**: `src/org/duckdns/hjow/swingwebpanel/css/anim/{AnimationEngine,AnimationClock,CssAnimationEvent,Easing,Interpolator}.java`, `css/cascade/{StyleAnimator,StyleResolver}.java`, `css/props/{Prop,PropertyTable,PropertyRegistry}.java`, `layout/{WebFontSource,WebFontSet,Woff,FontResolver,LayoutEngine,ClipPaths,PaintWalker,PaintSequence,BoxBuilder}.java`, `render/{PageRenderer,DisplayListBuilder}.java`, `core/{PagePresentation,PageResources}.java`, 시험 `css/AnimationTest`, `layout/WebFontTest`, `render/ClipPathPaintTest`, `core/{AnimationIntegrationTest,WebFontIntegrationTest}`.

**검증 결과(JDK 6u45)**: 아래 "최종 전체 시험" 절 참조(rebase 뒤 전체 실행 결과).

**JDK 6u45 확인 사항**: JRE 6 `lib/fonts` 의 `LucidaSansRegular.ttf`/`LucidaSansDemiBold.ttf` 로 TrueType 왕복(원본 ↔ WOFF ↔ 복원)이 같은 글꼴로 읽힘을 시험(`WebFontTest`). **OpenType-CFF 가 JDK 6u45 에서 읽히는지는 CFF 시험 글꼴이 없어 시험하지 못했다**(미확인으로 기록. 읽히지 않으면 소스 실패 → 다음 소스로 넘어가는 경로는 `garbage.bin` 사례로 시험).

**미해결 사항 / 한계**
- transitions: 되돌림 단축 계수 없음, `@starting-style` 없음(처음 계산한 스타일에는 전환이 없다), 의사 요소 스타일은 애니메이션하지 않는다, 서로 다른 함수의 `transform` 목록 보간에서 백분율은 크기 0 기준, 보간 못 하는 키프레임 속성은 중간에서 바뀐다(이산). 레이아웃에 영향이 없는 속성(opacity/transform)도 프레임마다 **전체 재배치**(`PageRenderer.render`)를 한다: 다시 그리기만 하는 최적화는 하지 않았다(성능 한계. 작은 문서에서는 문제없음, 큰 문서의 애니메이션은 느릴 수 있음).
- `TransitionEvent`/`AnimationEvent` 의 `propertyName`/`animationName`/`elapsedTime` 를 JS 에 노출하는 연결은 이 작업 범위(`js/**`) 밖이다 → 조정자 요청. DOM 이벤트로는 발생하므로 `addEventListener` 로 받는 것까지는 통합 시험이 확인했다(`e.type`).
- `@font-face`: WOFF2/EOT/SVG 글꼴/Type 1/교차 오리진/`font-display` 의 block/fallback/optional 의 구분/`font-variation-settings`/`size-adjust` 등 descriptor 는 미지원. `unicode-range` 는 세그먼트 단위 한계. JS `FontFace`/`document.fonts` 는 구현하지 않았다(미구현 목록). `font-display` 는 항상 swap 처럼 동작.
- clip-path: `url(#id)`(SVG clipPath), `path()`, `rect()`, `xywh()`, `shape()`, 상자 키워드만 있는 값, `inset()` 의 모서리별 반지름은 미지원(아무것도 자르지 않는다). `mask`, `filter`, `mix-blend-mode`, `backdrop-filter`, `shape-outside`, `scroll-snap`, `will-change`, `contain` 등은 이 작업에서 구현하지 못했다(아래 P10-08 목록).

**다음 작업 번호**: P10-08(남은 모듈 속성), P10-05 의 OpenType-CFF/WOFF2 확인.

**조정자 요청**
1. FEATURE_MATRIX 문구 제안(행별):
   - CSS-13: "`@font-face`: TrueType, WOFF 1.0(순수 Java 복원), `local()`, 순차 src, unicode-range(세그먼트 단위), weight/style 매칭. OpenType-CFF 는 `Font.createFont` 시도(JDK 6u45 미확인), WOFF2/EOT/SVG/Type1/교차 오리진 미지원. font-display 는 swap 처럼. 웹 글꼴 `FontFace`/`document.fonts`(CSS-43) 미구현" (`layout.WebFontSet/WebFontSource/Woff`, 시험 `WebFontTest` 5, `WebFontIntegrationTest` 2).
   - CSS-24/25: "`transform`/`transform-origin` 2D(matrix, translate(X/Y), scale(X/Y), rotate, skew(X/Y)와 목록, 3D 이름 중 2D 와 같은 것(translate3d(x,y,0), translateZ(0), scale3d(x,y,1), scaleZ(1), rotateZ, rotate3d(0,0,1,a), 3D 항목이 항등인 matrix3d)은 2D 로 투영, 그 밖의 3D 함수(rotateX/Y, perspective(), 0 이 아닌 z)는 선언 무효. `perspective`/`transform-style`/`backface-visibility` 는 효과 없음(EXC-03)). 그리기/히트(역변환)/getBoundingClientRect/스크롤 영역/쌓임 맥락/fixed·absolute 포함 블록 검증됨(`TransformPaintTest` 8)". 구현: `css.anim.Transforms`, `layout.LayoutBox.transform`, `render.TransformGroup`.
   - CSS-26/27/28: "전환/애니메이션/완화 부분 구현 → 검증(수동 시계 `AnimationTest` 9, 컴포넌트 `AnimationIntegrationTest` 2)" 와 위 한계.
   - CSS-42: "`::first-line` 구현(첫 줄 탐색 후 스타일 적용, `FirstLineLayoutTest` 7, `FirstLinePaintTest` 1, `LateBackgroundIntegrationTest` 2). 한계: 세로쓰기 컨테이너 제외, 고유 크기(min/max-content) 측정은 무시, 컨테이너 안에서 시작하는 float 는 탐색에 반영 안 됨." `::selection`/`::placeholder` 는 여전히 미구현.
   - CSS-35: "`clip-path` 기본 도형(inset/circle/ellipse/polygon) 구현, `mask` 미구현"(`ClipPathPaintTest` 3).
   - EXC-03 문구: 현재 "2D 투영 가능한 경우만" 과 일치하게 구현됨(위).
2. `docs/ARCHITECTURE.md`: 캐시 계층 표에 "웹 글꼴 바이트: 페이지 소유(`PageResources` 항목, 한도 8 MiB/파일, 32 면), 디코딩한 `Font` 는 `WebFontSet`(페이지 소유, 페이지 교체/폐기 때 함께 해제). JRE 6 `Font.createFont` 는 임시 파일을 만들고 글꼴이 수거되거나 JVM 이 끝날 때 지운다" 및 "애니메이션: `PagePresentation` 이 `AnimationEngine`(문서 스레드)과 프레임 타이머(데몬 스레드 하나, 애니메이션이 있을 때만, retire 때 cancel)를 소유".
3. `js/dom`/`js/host` 작업자: `CssAnimationEvent`(`getPropertyName/getAnimationName/getElapsedTime/getPseudoElement`)를 `TransitionEvent`/`AnimationEvent` 객체로 노출, 전역 생성자 `TransitionEvent`/`AnimationEvent`, `ontransitionend` 등 이벤트 핸들러 프로퍼티.

**체크박스 후보**(구현과 검증이 모두 끝난 것)
- P10-04-f(`::first-line` 좌표/화소 시험 + `TestHttpServer` 컴포넌트 시험 통과) → P10-04 상위 항목도 하위 a~f 모두 완료이므로 체크 가능.
- P10-06(transform 2D 구현 + 개별 속성 translate/rotate/scale + 역변환 히트 + 3D 예외 기록) → 체크 가능.
- P10-07(수동 시계 시험 + 컴포넌트 시험 + 폐기 시 정리) → 체크 가능(한계는 위에 기록).
- P10-05: 대상 형식 정의와 TrueType/WOFF 구현·시험 완료, WOFF2/OTTO(JDK 6 미확인)/교차 오리진은 한계로 기록. 계획 문장("JRE 가 읽을 수 없는 웹 글꼴 형식은 필요한 자체 처리를 추적")은 WOFF 처리와 미지원 목록으로 충족했으므로 체크 후보. 단 WOFF2(Brotli)는 자체 디코더가 필요한 형식이므로 "추적" 항목으로 남는다.
- P10-08: **체크하지 않는다**(아래 목록의 미구현 속성이 남음).

## P10-08 CSS3 모듈별 점검(미구현/부분 구현 목록, 2026-11-17)

`docs/progress/css.md` 의 속성 표와 `PropertyTable` 플래그(P = 파싱만), `PropertyRegistry` 의 미지원 이름 목록을 대조한 결과. "효과 없어도 되는 것"은 제외 사유를 적었다.

| 모듈 | 속성/기능 | 상태 | 비고 |
| --- | --- | --- | --- |
| Masking 1 | `clip-path` 기본 도형 | **이번 작업에서 구현** | url()/path()/rect()/xywh()/shape() 미지원 |
| Masking 1 | `mask`, `mask-image/size/position/repeat/mode/composite`, `mask-border`, `clip-rule` | 미구현(미지원 이름) | 래스터 마스크 합성이 필요 |
| Filter Effects 1 | `filter`(함수 목록), `backdrop-filter` | 파싱만(`filter`)/미지원 이름 | 색 행렬 + 블러는 구현 가능(오프스크린 그룹, opacity 그룹과 같은 구조) → 후속 |
| Compositing 1 | `mix-blend-mode`, `isolation`, `background-blend-mode` | 파싱만 | 그룹을 `Composite` 로 합성하는 구조 필요 → 후속 |
| Shapes 1 | `shape-outside`, `shape-margin`, `shape-image-threshold` | 미구현 | float 줄 배치에 영향(레이아웃) |
| Transforms 2 | `translate`, `rotate`, `scale` 개별 속성 | **구현됨**(2D 형태) | `Transforms.individual` |
| Transforms 2 | `perspective`, `transform-style`, `backface-visibility`, 3D 함수 | **명시적 예외 EXC-03** | 미구현과 구분 |
| Scroll Snap 1 | `scroll-snap-type/align/stop`, `scroll-padding`, `scroll-margin` | 미지원 이름 | 스크롤 후 위치 보정 필요 |
| UI 3/4 | `cursor`(파싱+계산 값, 효과 없음), `resize`, `appearance`, `accent-color`, `caret-color`, `user-select`, `scroll-behavior` | 부분(계산 값만) | `cursor` → Swing 커서 연결은 P11 소관, 효과 없어도 되는 것으로 제외 |
| Containment 1 | `contain`, `content-visibility` | 파싱만 | 최적화 힌트. 효과 없어도 되는 것으로 제외 |
| Will Change 1 | `will-change` | 파싱만 | 효과 없어도 되는 것으로 제외 |
| Images 3 | `object-fit`, `object-position` | **구현됨**(P10-04 이전, 확인: `ImagePaintTest`) | `image-set()`, `cross-fade()`, `element()` 미구현 |
| Logical Properties 1 | `margin-inline-*`, `padding-block-*`, `inset-*`, `border-*-*` 논리 | 미구현(CSS-37) | 속성 표/단축 속성과 쓰기 모드 매핑 필요 |
| Fragmentation 3 | `break-before/after/inside`, `box-decoration-break` | 미구현(CSS-39) | multicol 과 연계 |
| Text 3/4 | `text-wrap`, `hyphens`, `text-indent each-line`, `text-justify` | 부분 | 점검 필요(미확인) |
| Fonts 4 | `font-variation-settings`, `font-optical-sizing`, `size-adjust`, `font-palette` | 미구현 | JRE 6 Java2D 로 불가능한 부분 포함 |
| Pseudo 4 | `::selection`, `::placeholder`, `::backdrop`, `::file-selector-button` | 미구현 | `::first-line` 은 이번에 구현 |
| Cascade 5 | `@layer` 순서 적용, `@container`, `@property`, `@scope`, CSS nesting `&` | 미구현 | CSS-41 |
| Counter Styles 3 | `@counter-style`, `symbols()` | 미구현 | |
| Conditional 3/4 | `@supports` 의 `selector()`, `@media` 의 일부 4 단계 기능 | 부분 | |

따라서 P10-08 "기능 표의 CSS3 모듈별 미구현 속성/선택자/규칙을 다시 확인하고 각각 구현/검증" 은 **완료하지 못했다**(이번 작업에서 새로 구현한 것: transform, transition/animation, @font-face, `::first-line`, `clip-path`). 위 표의 "후속" 항목 중 `filter`, `mix-blend-mode` 가 다음 후보이다.

### 2026-11-17 (1차) P10-04-f ::first-line, P10-06 transform — 커밋 `20261117-401`

**완료한 내용**
- **::first-line** — `layout/FirstLine`(새), `InlineLayouter`(탐색 + 항목 분할), `InlineItem`, `BoxBuilder`, `css/cascade/ComputedStyle.overlay/with`. 첫 줄이 어디서 끝나는지는 스타일에 달려 있으므로(큰 글꼴은 줄을 일찍 나눈다) 먼저 모든 텍스트에 첫 줄 스타일을 입힌 항목으로 상자를 만들지 않는 탐색(dry + probe) 을 돌려 첫 줄의 끝(항목 번호, 원문 오프셋, 강제 줄바꿈 여부)을 얻고, 그 앞은 첫 줄 스타일, 그 뒤는 원래 스타일인 항목에 부드러운 줄바꿈(`InlineItem.soft`)을 넣어 실제 배치한다. 부드러운 줄바꿈은 줄바꿈된 줄처럼 끝나므로 `text-align: justify` 가 첫 줄에 유지된다. 배경은 모든 항목을 감싸는 `first-line` 인라인 조각(OPEN/CLOSE 의 가장자리 표시로 첫 줄에서만 사용됨)으로 그린다. 상속한 값만 덮어쓴다(`overlay`: 요소의 값과 같은 속성만 `::first-line` 값으로 교체. 스스로 속성을 정한 `<b style=color>` 는 유지). 블록이 블록 자식으로 시작하면 첫 자식으로 전파(`FirstLine.propagate`).
- **transform** — `css/anim/Transforms`(함수 → `AffineTransform`, origin, 보간), `PropertyTable`(`transform-function` 에 2D 와 같은 3D 이름 추가), `LayoutBox.transform/getTransform/getFullTransform`, `LayoutEngine.applyTransforms`(배치 뒤 최종 기하로 계산), `PaintWalker`(변환 그룹이 opacity/clip 을 감쌈), `render/TransformGroup` + `PagePainter.paintTransformGroup`(회전/기울임은 안티앨리어싱), `PaintSequence`(변환 사슬의 역변환으로 히트 테스트, 자르기도 자기 변환 공간에서 비교), `LayoutResult`(변환된 상자가 스크롤 영역에 기여), `HitTester.getClientRects`(변환한 경계 상자), `BoxBuilder`(변환된 상자는 쌓임 맥락이며 절대 배치/fixed 자손의 포함 블록, `tfCb`; `LayoutBox.isFixedPosition()` 은 변환된 조상이 없을 때만 true).
**검증 결과**: `FirstLineLayoutTest` 7, `FirstLinePaintTest` 1, `LateBackgroundIntegrationTest` 2(배경 이미지가 늦게 도착한 뒤 레이어가 나타남 + 컴포넌트를 통한 `::first-line`), `TransformPaintTest` 8 통과, 같은 시점 layout/render/css 전체 534 통과 0 실패 1 건너뜀(JarContentTest: 제품 JAR 없음).
**미해결**: `::first-line` 한계는 `FirstLine` Javadoc; transform: 폼 컨트롤 Swing 편집기(ControlSnapshot)의 위치는 변환을 반영하지 않는다(변환된 폼 컨트롤은 그려지는 자리와 편집기 자리가 다를 수 있음 → P11 후속).

## 최종 전체 시험(master 8c1cc51 위로 rebase 한 뒤, JDK 6u45, `sh test.sh`)

- 1차(커밋 402 까지): **2377 통과 / 1 실패 / 0 건너뜀**. 실패는 master 가 새로 추가한 `StaticStateAuditTest.testEveryStaticStateHolderIsReviewed` 가 `FontResolver#WEB`(웹 글꼴 `ThreadLocal`)을 검토 목록에 없다고 지적한 것 → 커밋 403 에서 사유와 함께 검토 목록에 추가(다른 작업자의 시험 파일을 한 줄 고쳤음. 조정자 확인 필요).
- 2차(개별 변환 속성 추가 뒤): 2378 통과 / 1 실패. 실패 `PageResourcesTest.testEntryLimitAndClose`("and counts it as ended" 기대 256, 실제 257)는 이 작업과 무관한 **간헐 경쟁**이다: `close()` 가 끝난 수를 시작한 수로 맞춘 직후 인터럽트된 작업자 스레드의 `finally { finished++ }` 가 시험의 단언보다 먼저 실행되면 257 이상이 된다. 같은 시험을 단독으로 6 번 돌리면 6 번 통과하고, 클래스 단위로 3 번 돌리면 1 번 통과/2 번 실패였다. 내가 바꾼 `PageResources` 의 부분(웹 글꼴 항목과 목적)은 이 카운터 경로를 건드리지 않는다(diff 로 확인). 조정자에게 원인(시험이 비동기 종료와 경쟁함)을 알리는 요청.
- 3차(최종 커밋 `20261117-404` 기준, 같은 명령): **2379 통과 / 0 실패 / 0 건너뜀**(646 초). 위 간헐 실패는 이 실행에서 나타나지 않았다.
