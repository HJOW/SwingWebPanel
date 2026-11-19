# WEBANIM 진행 기록 (P10-08-h 중 `element.animate()` — Web Animations Level 1)

작업자: WEBANIM (격리 worktree 브랜치 `worktree-agent-abb75335020f73767`, master `e39aa97` 에서 갈라짐). 커밋 번호 대역은 `20261119-201` 부터.
같은 시기에 작업자 CSS-M 이 P10-08-h 의 나머지 CSS 항목(`cross-fade()`/`element()`, `::file-selector-button` 등)을 맡았다. 이 문서는 Web Animations 만 다룬다.

## 1. 조회한 규격

| 규격 | URL | 판본 | 조회 |
| --- | --- | --- | --- |
| Web Animations (Level 1) | https://www.w3.org/TR/web-animations-1/ → 판본 URL https://www.w3.org/TR/2023/WD-web-animations-1-20230605/ | W3C Working Draft **5 June 2023** (`Last-Modified: Mon, 05 Jun 2023 02:28:05 GMT`) | 2026-11-19, `tools/webfetch`(JRE 25, `WEBFETCH_JAVA_HOME=C:/Program Files/Java/zuu25`) |

- 참고한 다른 규격(조회하지 않고 기존 기록을 따름): CSS Easing Level 1 의 계단 함수 before 플래그(3.1 절), CSS Transitions/Animations Level 2 의 합성 순서(전환 < CSS 애니메이션 < 그 밖의 애니메이션).
- 구현 판단의 근거로 쓴 절: 4.3(타임라인, "update animations and send events"), 4.4(애니메이션의 모든 절차), 4.5~4.11(시간 계산), 5.3(키프레임 효과와 계산 키프레임,
  5.3.2 의 글꼴 크기 순서 의존 예), 5.4(효과 스택과 합성), 5.5(교체된 애니메이션 제거), 6.2~6.12(프로그래밍 인터페이스, 6.6.3 키프레임 인수 처리).

## 2. 구조

모델(스크립트와 무관, `src/org/duckdns/hjow/swingwebpanel/css/anim/**`):

- `EffectTiming` — 시간 속성과 시간 계산(단계, 활성 시간, 전체/단순 반복 진행도, 현재 반복, 방향, 완화와 before 플래그), 시간 사전 검사(`validate`), `<easing-function>` 해석.
- `WebAnimation`(`EventTarget`) — `Animation` 의 상태 기계: 시작/유지 시간, 재생 속도와 대기 재생 속도, 대기 play/pause 작업, 끝남 상태와 끝 알림 단계, 취소, 뒤집기, 교체 상태.
- `KeyframeEffectModel` — 대상(요소와 의사 요소), 키프레임(지정 값과 해석한 선언), 계산 키프레임(대상 기준 계산, 글꼴 크기 우선), 효과 값(속성별 키프레임, 암묵 0/1 키프레임, 구간, 키프레임 완화, 보간과 합성), 속성 이름 변환(IDL ↔ CSS).
- `Compositor` — `add`/`accumulate`(숫자, 길이, 백분율, 각도, 색, transform 목록, 같은 길이의 목록).
- `WebTimeline` — `DocumentTimeline`(원점 시간). `WebAnimationSet` — 문서 등록부(합성 순서, 문서 시간, 대기 애니메이션 이벤트 큐, update 절차, 교체 제거, 스타일 적용, `commitStyles`, 멈춤/폐기).
- `WebAnimationHost`(프라미스, 마이크로태스크, 프레임, 스타일 변경 알림), `AnimationPlaybackEvent`, `AnimationException`.
- `AnimationEngine` 변경: `WebAnimationSet` 을 소유하고 `animate()`/`animatePseudo()` 의 맨 위에서 스크립트 애니메이션을 적용한다. 효과 스택의 순서를 규격대로 **CSS 전환 → CSS 애니메이션 → 스크립트 애니메이션**으로 바꿨다(이전에는 전환이 CSS 애니메이션 위였다. 같은 속성에 둘이 함께 걸리는 경우만 결과가 달라지며, 기존 시험에는 그런 사례가 없었다. 새 사례는 `WebAnimationStyleTest.testCompositeOrderWithCss`).

스크립트 연결(`src/org/duckdns/hjow/swingwebpanel/js/host/anim/**`): `WebAnimationsApi`(인터페이스 설치, `WebAnimationHost` 구현, 프레임 훅), `KeyframeArguments`(키프레임 인수와 시간 사전, 결과 객체), `AnimationPage`(페이지 쪽 계약).
페이지 쪽 가산 변경: `js/host/AnimationHostImpl`(새 파일), `Timers`(프레임 훅: Web Animations 단계를 같은 시각의 `requestAnimationFrame` 콜백 바로 전에 실행), `WindowGlobals`(설치, 스크립트 중지 시 멈춤),
`HostEnvironment.animationsChanged()`(기본 구현은 `canvasChanged(null)` 로 새 렌더링 요청), `js/dom/DomInterfaces`(이벤트 분류 한 줄), `css/cascade/StyleResolver.getAnimator()`.

시계와 스레드: 문서 타임라인의 시간은 `performance.now()`·`requestAnimationFrame` 과 같은 스케줄러 시계(페이지 시작 기준 밀리초)이다. 애니메이션이 프레임을 기다리는 동안 문서 시간은 마지막 프레임의 시간에 머물고
(규격 4.3.2: 프레임 사이에 바뀌지 않음), 아무것도 기다리지 않으면 시계를 따른다. 모든 모델 조작은 문서 스레드에서 한다. 스크립트가 멈출 때(JavaScript 끄기, 이동, 폐기)는 어느 스레드에서든 `detach()` 가
volatile 표시만 세우고, 문서 스레드의 다음 렌더링 틱(`AnimationEngine.tick` → `flushStopped`)이 애니메이션을 놓고 대상 스타일을 다시 계산한다.

## 3. 인터페이스·멤버별 상태

| 인터페이스 / 멤버 | 상태 | 비고 |
| --- | --- | --- |
| `Animation` 생성자 `(effect?, timeline?)` | 구현 | 타임라인 인수가 없으면(undefined) 기본 문서 타임라인, null 이면 타임라인 없음 |
| `id`, `effect`, `timeline`, `startTime`, `currentTime`, `playbackRate` | 구현 | 4.4.1~4.4.5, 4.4.15.1. `double?` 인수의 비유한 값은 TypeError |
| `playState`, `replaceState`, `pending` | 구현 | 4.4.17, 5.5.1 |
| `ready`, `finished` | 구현 | realm 의 `%Promise%`. ready 는 다음 애니메이션 프레임에서(그 프레임 시각을 준비 시간으로) resolve. 취소 시 `AbortError` 로 reject |
| `onfinish`, `oncancel`, `onremove` | 구현 | 이벤트는 문서의 대기 애니메이션 이벤트 큐를 거쳐 다음 프레임에서 예정 시각·합성 순서로 정렬해 전달 |
| `play()`, `pause()`, `reverse()`, `finish()`, `cancel()`, `updatePlaybackRate()`, `persist()` | 구현 | 4.4.8~4.4.16. `finish()` 는 끝 알림을 즉시 실행 |
| `commitStyles()` | 구현(제약) | 대상 효과 스택의 그 애니메이션까지의 값을 인라인 style 에 씀. 의사 요소는 NoModificationAllowedError, 렌더링되지 않는 대상은 InvalidStateError. 사용자 정의 속성은 쓰지 않음 |
| `AnimationEffect`: `getTiming()`, `getComputedTiming()`, `updateTiming()` | 구현 | 6.5. `fill: auto` → `none`, `duration: auto` → 0 |
| `KeyframeEffect` 두 생성자, `target`, `pseudoElement`, `composite`, `getKeyframes()`, `setKeyframes()` | 구현 | 6.6. `pseudoElement` 는 `::before`/`::after`/`::marker`(옛 한 콜론 형식 정규화) |
| `KeyframeEffect.iterationComposite` (Level 2) | 기록만 | 값을 보관·반환하지만 반복 누적은 적용하지 않음 |
| 키프레임 인수(배열 형식, 속성별 형식, 이터러블) | 구현 | 6.6.3: 읽기 순서, 오프셋 검사, 완화(쓰이지 않은 것 포함) 검사, 잘못된 값 버리기, IDL 이름만 받음(`cssFloat`, `cssOffset`, `--x`) |
| `composite: replace/add/accumulate`(효과와 키프레임별) | 구현(제약) | 결합할 수 없는 값(키워드, 길이와 백분율의 혼합 등)은 replace. transform 의 accumulate 는 같은 함수 목록일 때 인수 합(배율은 a+b−1), 아니면 연결(행렬 누적은 하지 않음) |
| `AnimationTimeline.currentTime`, `DocumentTimeline({originTime})` | 구현 | 다른 문서(예: `DOMParser`)의 `timeline` 은 비활성(currentTime null) |
| `AnimationPlaybackEvent` 생성자, `currentTime`, `timelineTime` | 구현 | |
| `Element.animate()`, `Element.getAnimations({subtree})`, `Document.getAnimations()`, `document.timeline` | 구현 | `getAnimations` 는 관련(relevant) 애니메이션만, 합성 순서 |
| 교체된 애니메이션 자동 제거, `remove` 이벤트 | 구현 | 5.5.2 |
| `CSSAnimation`(`animationName`)/`CSSTransition`(`transitionProperty`) 노출 | **미구현** | 아래 6절 |

## 4. 시험과 결과

모두 JDK 6u45 로 실행했다(아래 "실제 결과"는 직접 실행한 출력이다).

| 시험 클래스 | 건수 | 내용 |
| --- | --- | --- |
| `css.anim.EffectTimingTest` | 6 | 반복 경계(끝점 제외), 채움과 지연, 음의 끝 지연, 반복 시작과 방향, 0 지속 시간의 끝없는 반복(현재 반복 Infinity), 계단 완화의 before 플래그, 시간 사전 검사 |
| `css.anim.WebAnimationModelTest` | 9 | 대기 play 와 다음 프레임의 준비 시간, pause/resume, `finish()` 의 동기 알림, cancel 의 reject 와 이벤트, reverse·playbackRate·updatePlaybackRate, 이동(현재/시작 시간), 교체 제거와 persist, 이벤트 정렬과 놓아 주기, 문서 시간(프레임 사이 고정, 뒤로 가지 않음, 멈춤) |
| `css.anim.WebAnimationStyleTest` | 10 | 실제 리졸버로 계산 스타일: 보간, 암묵 키프레임과 오프셋, 5.3.2 예(15px/225px), add/accumulate, 이산 값, 사용자 정의 속성(등록된 `<length>` 보간과 `var()`, 미등록은 절반에서 바뀜), 채움·일시정지·취소, CSS 와의 합성 순서, 의사 요소 대상, commitStyles |
| `js.host.anim.WebAnimationsApiTest` | 10 | 스크립트: animate 반환값과 ready, getComputedStyle, 키프레임 두 형식과 getKeyframes 정규화, TypeError/DOMException 23종, 시간 API, 프라미스와 이벤트 순서, 생성자와 타임라인(rAF 와 같은 시각), getAnimations, 교체와 commitStyles, 스크립트 중지 |
| `core.WebAnimationsIntegrationTest` | 3 | 실제 컴포넌트와 HTTP: 스스로 그려지는 사라지기와 finished/finish, 타이머의 `cancel()` 과 hashchange 핸들러의 `finish()`(fill forwards)가 화면을 바꿈, 이동과 JavaScript 끄기 뒤 이전 애니메이션이 남지 않음(이전 페이지의 이벤트 없음) |

실행 명령(패키지 시험):

```text
sh scripts/pkgtest.sh webanim "css dom html" "css/anim" --filter org.duckdns.hjow.swingwebpanel.css.anim
sh scripts/pkgtest.sh webanim "css js dom html layout render net core content storage download media @root" "css/anim js/host/anim core" --filter WebAnimation
sh tools/sourcecheck/run.sh <변경 경로>
```

실제 결과(2026-11-19, JDK 6u45):

- `css/anim` 패키지 시험: 24 통과 / 0 실패(모델 커밋 `3b30cd2` 시점). 이후 `WebAnimationStyleTest.testCustomProperties` 추가 뒤 그 클래스 10/10 통과.
- 모델·스크립트·통합 묶음(`--filter WebAnimation`): 30 통과 / 1 실패. 실패는 `WebAnimationsIntegrationTest.testCancelAndFinishChangeThePicture` 가 1.2초 간격 두 타이머 사이의 빨간 구간을
  부하 속에서 놓친 것(첫 페이지의 클래스 적재로 렌더링이 늦음)이었고, 두 번째 단계를 hashchange 로 바꿔 결정적으로 만든 뒤 그 클래스 단독 3/3 통과. 디버그 프로그램으로 실제 컴포넌트의
  화면 색 변화를 시간별로 확인했다(파랑 → 취소 뒤 빨강).
- **전체 회귀**(`BUILD_DIR=build/full sh test.sh`, 커밋 `7d2d595` 의 트리): `=== 3705 passed, 2 failed, 12 skipped (1299845 ms) ===`. 새 시험 37건 모두 통과.
  실패 2건은 `render.FontBlockPaintTest.testBlockPeriods`, `testTextIsPaintedAfterTheBlockPeriod`(웹 글꼴 block 기간의 시간 민감 시험, 애니메이션 코드와 무관)이며
  같은 클래스 단독 재실행에서 5/5 통과했다 → 부하에 따른 간헐 실패로 판단. 건너뜀 12건은 기존의 선택적 시험(환경 의존)이다.
- 전체 회귀 뒤의 작은 변경(사용자 정의 속성의 합성 순서를 전환 → CSS 애니메이션 순으로 맞춤, 커밋 204)은 `css` 시험 `--filter Animation` 으로 다시 확인했다: `=== 38 passed, 0 failed, 0 skipped ===`(AnimationTest 9, DiscreteTransitionTest 2, PropertyTest 1, RegisteredPropertyAnimationTest 7, WebAnimationModelTest 9, WebAnimationStyleTest 10).
- 소스 규칙: `sh tools/sourcecheck/run.sh` 로 변경한 모든 경로 0 위반.

## 5. 숨기지 않는 한계

- CSS 애니메이션과 전환은 `getAnimations()` 에 나오지 않고 `CSSAnimation`/`CSSTransition` 객체가 없다(6절).
- 애니메이션 값은 캐스케이드의 animation 출처가 아니라 계산 스타일 위에 덮인다: 작성자의 `!important` 선언도 애니메이션이 이긴다(CSS 애니메이션 엔진의 기존 한계와 같음).
- 계산 키프레임은 대상의 글꼴 크기·루트 글꼴 크기·색·쓰기 모드·방향이 바뀔 때만 다시 계산한다(규격은 매번 "살아 있는" 계산). `var()` 가 든 키프레임 값은 선언 시점 해석을 따른다.
- 합성: `add`/`accumulate` 는 2절의 값 종류만. 사용자 정의 속성은 등록된 형 있는 속성이면 보간, 아니면 절반에서 바뀌며 합성 연산은 적용하지 않는다.
- `[[PromiseIsHandled]]` 를 설정하지 않는다(호스트에 거부 추적이 없어 관찰되지 않는다).
- `iterationComposite`(Level 2)는 보관만 한다. 그룹 효과, 스크롤 타임라인은 대상 밖(Level 2 이후).
- 끝없는 실행 애니메이션은 약 16ms 마다 프레임과 재배치를 일으킨다(기존 CSS 애니메이션과 같은 비용 모델).
- 스크립트를 끄거나 이동하면 스크립트 애니메이션은 이벤트 없이 끝난다(효과도 사라진다). 브라우저에는 대응하는 동작이 없어 프로젝트 정책으로 정했다.
- **실제 Swing 창에서 눈으로 확인하지 않았다**: 통합 시험은 화면 밖 `paint` 로 픽셀을 읽는다.

## 6. CSSAnimation / CSSTransition 을 구현하지 않은 이유

CSS 엔진(`AnimationEngine` 의 `Anim`/`Transition`)은 벽시계(`AnimationClock`)로 직접 진행하고 프라미스·재생 이벤트·대기 작업·유지 시간 같은 Web Animations 상태가 없다.
`getAnimations()` 가 CSS 애니메이션을 돌려주고 `pause()`/`currentTime`/`cancel()` 이 실제 CSS 애니메이션에 반영되려면 두 모델을 하나로 합치거나(CSS 애니메이션을 `WebAnimation` 으로 표현)
API 조작이 `animation-play-state` 를 덮어쓰는 규칙(CSS Animations 2 의 "play state 재정의")과 시간 변환을 갖춘 대리 객체가 필요하다. 어느 쪽이든 기존 CSS 애니메이션 시험 전체에 영향을 주는 큰 변경이라
이번 작업에서는 하지 않았다. 지금은 `getAnimations()` 가 스크립트 애니메이션만 돌려준다.

## 7. 조정자 요청

1. **FEATURE_MATRIX** — CSS-44 의 `element.animate`/`getAnimations` 도 없다 문구와 496행(`element.animate` 대상 목록 밖), 553행(`element.animate` 없음)을 고치고 새 행을 둔다(제안 ID `CSS-WA` 또는 `WEB-WA`):
   `| Web Animations Level 1 | web-animations-1 — W3C WD 2023-06-05 | css.anim.{EffectTiming,WebAnimation,KeyframeEffectModel,WebAnimationSet,WebTimeline,Compositor,AnimationPlaybackEvent}, js.host.anim.{WebAnimationsApi,KeyframeArguments,AnimationPage}, js.host.AnimationHostImpl | css.anim.{EffectTimingTest 6,WebAnimationModelTest 9,WebAnimationStyleTest 10}, js.host.anim.WebAnimationsApiTest 10, core.WebAnimationsIntegrationTest 3 | 검증 완료(제약 있음) | Animation/KeyframeEffect/AnimationEffect/AnimationTimeline/DocumentTimeline/AnimationPlaybackEvent 전부, element.animate/getAnimations, document.getAnimations/timeline, 키프레임 두 형식, 합성 replace/add/accumulate, 교체 제거, commitStyles. 한계: CSSAnimation/CSSTransition 미노출(getAnimations 는 스크립트 애니메이션만), iterationComposite(Level 2)는 보관만, !important 를 이김, 계산 키프레임 캐시(webanim.md 5절) |`
2. **CSS-26/27** 에 한 문장 추가: "효과 스택 합성 순서를 Web Animations 5.4.2/CSS Transitions 2 대로 바꿈: 같은 속성에서 CSS 애니메이션이 실행 중인 전환을 이긴다(이전에는 전환이 이겼다). 스크립트 애니메이션(element.animate)은 그 위. 시험 `WebAnimationStyleTest.testCompositeOrderWithCss`."
3. **ARCHITECTURE** `css.anim` 설명에 추가: "Web Animations 모델(WebAnimation 상태 기계, KeyframeEffectModel, WebAnimationSet 문서 등록부; AnimationEngine 이 소유하며 효과 스택 맨 위에 적용)", `js.host.anim` 패키지(스크립트 연결) 신설, 그리고 "Web Animations 의 update 단계는 Timers 의 애니메이션 프레임에서 rAF 콜백 직전에 같은 시각으로 실행; 스크립트 중지는 WebAnimationSet.detach(어느 스레드) → AnimationEngine.tick 의 flushStopped(문서 스레드)".
4. **공유 파일 가산 변경 알림**: `HostEnvironment.animationsChanged()`(기본 `canvasChanged(null)`), `StyleResolver.getAnimator()`, `js/dom/DomInterfaces` 분류 한 줄, `Timers`(scheduleFrame/setAnimationHook, frame 의 훅 호출), `WindowGlobals`(설치·중지). core 파일은 고치지 않았다.
5. **P10-08-h 체크 후보 판단**: Web Animations(`element.animate()`) 부분은 이 문서 3절의 미구현 1건(CSSAnimation/CSSTransition 노출)을 빼고 구현·검증했다. P10-08-h 전체 체크는 CSS-M 의 결과와 함께 조정자가 판단하며, CSSAnimation/CSSTransition 노출은 기능 표에 한계로 적거나 후속 항목으로 두기를 제안한다.
