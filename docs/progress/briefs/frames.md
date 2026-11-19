# FRAMES 작업자 재개 브리프 (P20-02 iframe/srcdoc/frame/frameset 브라우징 문맥)

당신은 SwingWebPanel 프로젝트의 FRAMES 작업자로, **이전 작업자가 중단한 작업을 이어서** 한다. 이전 작업자의 코드는 컴파일·시험이 확인되지 않은 부분이 있으니 그대로 신뢰하지 말고 직접 컴파일·시험으로 검증한다.

## 작업 위치(중요)
- 작업 디렉터리(git worktree): `H:\Workspace\git\SwingWebPanel\.claude\worktrees\agent-a19ec237e7b4417f7`, 브랜치 `worktree-agent-a19ec237e7b4417f7`.
  **모든 명령은 `cd /h/Workspace/git/SwingWebPanel/.claude/worktrees/agent-a19ec237e7b4417f7 && ...` 로 시작한다.** 메인 체크아웃(`H:\Workspace\git\SwingWebPanel`)의 파일은 읽기만 하고 수정/커밋하지 않는다.
- 이 브랜치는 옛 master(`20261117-060` 직후, 커밋 601/602 포함) 위에 있다. master 는 인수인계 커밋 061 하나만 앞서 있고 코드 변경은 없다. 시작할 때 `git rebase master` 가 필요하면 미커밋 변경을 먼저 WIP 커밋(`git commit -m "..."`)한다. (stash 금지: 다른 worktree 와 공유됨)
- 현재 미커밋 변경(15개 파일): `core/{FrameContext,FrameSet,HtmlPageDocument,PageRuntime,TopContext}`, `js/dom/DomInterfaces`, `js/host/{BrowsingContext,CrossOriginWindow,FrameApi,HostEnvironment,MessagingApi,PageScripts,WindowGlobals}`, 시험 `tests/src/org/duckdns/hjow/swingwebpanel/core/frames/{FrameNavigationTest,FrameWindowTest}`.
  이것은 단계 (c)(창 관계와 같은 오리진 접근)의 진행 중 코드다. 컴파일부터 확인한다.

## 읽을 것
`docs/progress/frames.md`(설계, 이력 규칙, 한도, 단계 (a)(b) 기록과 "하위 항목" 목록)를 **먼저 끝까지** 읽는다. 그리고 공통 규칙(아래).

## 해야 할 일 (단계 순서)
- (c) `window.parent/top/frames/length/frameElement/name/self`, `iframe.contentWindow/contentDocument`, 같은 오리진 접근, 다른 오리진 창 프록시(`postMessage`/`location` 쓰기/`closed`/`frames`/`length` 외 접근 차단, SecurityError DOMException)
- (d) target 이동 규칙(이미 링크/폼 target 은 구현됨)을 JS `window.open`/`location` 쓰기와 `history` 에 맞춰 정리, 이력 규칙 문서대로(프레임 이동은 컴포넌트 이력에 항목을 더하지 않음)
- (e) sandbox(`allow-scripts/forms/same-origin/popups/top-navigation/pointer-lock`)와 `srcdoc` 오리진, 누적 규칙
- (f) 과거 `frameset`/`frame`/`noframes` (rows/cols 크기 목록, 중첩, `frameborder`/`marginwidth`/`scrolling`/`noresize` 등), `frame.contentWindow`
- (g) 수명: 프레임 있는 페이지를 반복 이동/`clean()`/`dispose()` 했을 때 스레드·타이머·요청·참조가 남지 않음(WeakReference 시험), 프레임 문서의 스크립트 취소, 재귀/깊이 한도, 느린 응답 취소. 프레임 안 폼 컨트롤 편집 오버레이 위치(`ControlOverlays`)가 최상위 좌표만 아는 한계도 확인·처리 또는 문서화.
- 각 단계를 끝낼 때마다 `docs/progress/frames.md` 에 기록(변경 파일, 실제 실행한 시험 명령과 결과 수, 미해결 사항)하고 commit 한다.
- 끝나면(또는 한도에 닿으면) `docs/progress/frames.md` 맨 위에 "조정자 요청"(ARCHITECTURE 의 프레임 행, FEATURE_MATRIX 의 HEI/LEG 행, AGENTS.md 체크 후보: P20-02, P15-04-b-2 의 프레임 부분, P21-03/P22-09 의 프레임 부분)을 정리한다.

## 소유 경로
`src/org/duckdns/hjow/swingwebpanel/core/{Frame*,TopContext}.java`, `core/frames/**`, `render/Frame*.java`, `js/host/{BrowsingContext,CrossOriginWindow,FrameApi}.java`, 해당 시험 `tests/src/org/duckdns/hjow/swingwebpanel/core/frames/**` 와 프레임 관련 `js/host` 시험, `docs/progress/frames.md`.
다른 파일은 가산적 최소 수정만 한다(`core/{HtmlPageDocument,PageRuntime,PagePresentation,PageInput,...}`, `js/host/{HostEnvironment,PageScripts,WindowGlobals,MessagingApi}`, `js/dom/DomInterfaces`, `dom/Document`). WORKER 작업자가 `js/host/{MessagingApi,WindowGlobals}.java`, `js/dom/DomInterfaces.java` 도 가산적으로 고치고 있으므로 이 파일들은 **작은 가산 변경만** 하고 자주 commit 해서 충돌을 줄인다.
커밋 번호 대역: `20261117-601` 부터(602 까지 사용됨, 다음은 603).

<공통 규칙은 별도로 붙임>
