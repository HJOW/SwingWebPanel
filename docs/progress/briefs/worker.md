# WORKER 작업자 재개 브리프 (P20-09-b Worker/SharedWorker/BroadcastChannel)

당신은 SwingWebPanel 프로젝트의 WORKER 작업자로, **이전 작업자가 중단한 초기 구현을 이어서** 한다. 이전 작업자의 코드는 컴파일·시험이 확인되지 않았다. 그대로 신뢰하지 말고 컴파일부터 직접 확인한다.

## 작업 위치(중요)
- 작업 디렉터리(git worktree): `H:\Workspace\git\SwingWebPanel\.claude\worktrees\agent-abcaff908d567c37a`, 브랜치 `worktree-agent-abcaff908d567c37a`(master `20261117-060` 직후와 같은 커밋 위, 이 브랜치의 커밋은 아직 없음).
  **모든 명령은 `cd /h/Workspace/git/SwingWebPanel/.claude/worktrees/agent-abcaff908d567c37a && ...` 로 시작한다.** 메인 체크아웃의 파일은 읽기만 한다.
- 시작 시 `git rebase master` 가능(master 는 문서 커밋 061 만 앞섬). 미커밋 변경이 있으므로 먼저 의미 있는 첫 커밋(컴파일되는 상태가 되면)을 만든 뒤 rebase 한다. stash 금지(다른 worktree 와 공유됨).
- 미커밋 변경: `dom/ErrorEvent`(신규), `js/worker/**`(신규 15개 파일 약 3,500줄: `WorkerScope`, `WorkerGlobals`, `WorkerHandle`, `WorkerApi`, `WorkerTimers`, `BroadcastHub`, `Messages` 등), `js/dom/DomInterfaces`, `js/host/{MessagingApi,WindowGlobals}`, `js/host/web/{WebApis,XhrApi}` 수정, 시험 `tests/src/org/duckdns/hjow/swingwebpanel/js/worker/{WorkerErrorTest,WorkerLifecycleTest,WorkerMessagingTest,WorkerTestSupport}`.
  `docs/progress/worker.md` 는 아직 없다. 코드를 읽어 설계를 파악하고 이 문서를 만든다(설계, 한도, 한계, 검증).

## 요구(AGENTS.md P20-09 / P20-09-b)
"대상 규격의 Worker/message 관련 기능은 **별도 실행 문맥과 제한된 객체 집합**으로 구현한다. 메시지의 구조화 복사/전달, 큐, 오류, 종료를 처리하고 Worker에서 DOM을 직접 변경하지 못하게 한다."
- P20-09-b: `Worker`/`SharedWorker` 별도 실행 문맥(DOM 없는 제한된 전역: `self`, `postMessage`, `onmessage`, `importScripts`, `close`, `location`, `navigator`, 타이머, `fetch`/`XMLHttpRequest`(공통 요청 계층 경유), `structuredClone`), `BroadcastChannel`, `MessagePort` 의 전달(transfer), `targetOrigin` 필터.
  입력: 워커 스크립트와 메시지 왕복·오류·종료; 기대: 별도 realm 의 직렬 실행, DOM 접근 불가, 종료 뒤 콜백 없음; 구현 위치: `js/worker/**`.
- 이미 있는 것(재사용): `js/host/structured/**`(구조화 복제), `js/host/MessagingApi`(`postMessage`/`MessageChannel`/`MessagePort`), 공통 요청 서비스 `core/PageWebRequests` 와 `js/host/web/**`(XHR/fetch), `js/host` 의 `Scheduler`/타이머, `js/JsRealm`.
- 규칙: 워커 스크립트 요청은 User-Agent/추가 헤더/쿠키/SSL 확장 지점/스키마 제한을 거치고 같은 오리진만(규격: 다른 오리진 워커 스크립트는 SecurityError). 워커는 페이지 이동/`clean()`/`dispose()` 때 종료되고 그 뒤 콜백은 실행되지 않는다. 워커마다 Java 스레드를 만들지 말고 가능하면 공유 실행기/문서 큐와 같은 소유 스레드 규칙을 지킨다(직렬 실행, 무한 반복은 실행 제한으로 종료). 스레드를 쓰면 종료·누수 시험이 필요하다.
- 조회할 규격: HTML Standard "Web workers"(WHATWG, 조회일 기록), HTML5 2014 REC "Web Workers" 는 별도 규격(W3C Web Workers CR 2012)이다. 판본과 조회일을 `docs/progress/worker.md` 에 적는다. 조회는 `tools/webfetch`(JDK 25 로 실행해야 HTTPS 가능; `tools/webfetch/AGENTS.md` 참조).

## 해야 할 일
1. 컴파일 확인 → 이전 시험(`js.worker` 4개 시험 파일)을 돌려 현재 상태 파악 → 고장 수정 → 첫 커밋.
2. 위 요구를 항목별로 점검표로 만들어 구현하고 시험을 보강한다(왕복/큐 순서/오류 전파(`ErrorEvent`, `onerror`)/`close()`/`terminate()`/`importScripts`/타이머 정리/DOM 접근 불가(`document`, `window` 가 정의되지 않음)/교차 오리진 거부/transfer 후 원본 분리/BroadcastChannel 같은 오리진 전달/컴포넌트 `clean`·`dispose` 후 종료/무한 루프 워커가 UI 와 다른 워커를 막지 않음).
3. `SharedWorker` 와 `MessagePort` transfer, `targetOrigin` 필터까지 한다. 어려우면 한계를 정직하게 문서화하고 남긴다.
4. 끝나면 `docs/progress/worker.md` 맨 위에 "조정자 요청"(FEATURE_MATRIX/ARCHITECTURE 행 문구, AGENTS.md 체크 후보 P20-09-b/P20-09)을 정리한다.

## 소유 경로
`src/org/duckdns/hjow/swingwebpanel/js/worker/**`, `src/org/duckdns/hjow/swingwebpanel/dom/ErrorEvent.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/worker/**`, `docs/progress/worker.md`. 다른 파일(`js/host/{MessagingApi,WindowGlobals}`, `js/host/web/{WebApis,XhrApi}`, `js/dom/DomInterfaces`)은 **가산적 최소 수정만** 한다. FRAMES 작업자도 `MessagingApi`/`WindowGlobals`/`DomInterfaces` 를 가산적으로 고치고 있으니 작은 변경을 자주 commit 한다.
커밋 번호 대역: `20261117-701` 부터.

<공통 규칙은 별도로 붙임>
