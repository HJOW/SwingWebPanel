# WORKER 진행 기록 (P20-09-b: Worker / SharedWorker / BroadcastChannel / MessagePort 전달)

작업자: WORKER (격리 worktree 브랜치 `worktree-agent-abcaff908d567c37a`). 항목은 맨 위가 최신이다. 커밋 번호 대역은 `20261117-701` 부터.

## 조정자 요청 (요약. 최종 상태는 맨 아래 "현재 상태" 참조)

작업 중이다. 아래는 현재까지의 잠정 문구이며, 작업을 마칠 때 다시 정리한다.

- AGENTS.md 체크 후보: **아직 없음**(P20-09-b 는 SharedWorker 와 MessagePort 전달이 끝나야 체크 후보가 된다).

## 규격과 조회 기록

- HTML Standard (WHATWG) "Web workers" — https://html.spec.whatwg.org/multipage/workers.html, `tools/webfetch`(JRE 25)로 2026-11-17 조회,
  Last-Modified: Mon, 16 Nov 2026 15:32:23 GMT. (W3C Web Workers CR 2012 는 별도 규격이며 이 구현은 위 HTML Standard 의 현행 문구를 따른다.)
- HTML Standard (WHATWG) "Web messaging" — https://html.spec.whatwg.org/multipage/web-messaging.html, 같은 방법으로 2026-11-17 조회, 같은 Last-Modified.
  (window.postMessage 의 targetOrigin, 채널 메시징, MessagePort 전달 규칙, Broadcasting to other browsing contexts.)
- HTML5 2014 REC 에는 이 장들이 없다(별도 REC `webmessaging` 2015-05-19 는 DOMAPI-B 가 이미 기록했다).

## 이전 작업자의 초기 구현을 이어받은 기록

- 이전 작업자가 중단한 미커밋 구현(`js/worker/**` 약 3,500줄, `dom/ErrorEvent`, `js/host/web/{WebApis,XhrApi}` 의 `WebApis.Host` 분리,
  `js/dom/DomInterfaces.installEventCore`, `MessagingApi.installMessageEvent` 공개, `WindowGlobals` 의 `WorkerApi` 설치와 종료)를 그대로 신뢰하지 않고 컴파일부터 확인했다.
- 20261117-701: 컴파일되는 상태로 보존. 20261117-702: **시험 22건 모두 통과**(처음에는 부분 컴파일 때문에 `String.prototype` 의 선택적 내장이 빠져 7건이
  실패해 보였으나, 이는 시험 환경 문제였다 — 아래 "시험 실행 방법" 참조 — 그 밖에 시험 1건의 잘못된 가정(`document.body` 는 `<head>` 의 스크립트 시점에 null)을 고쳤다).

## 시험 실행 방법 (중요)

`scripts/pkgtest.sh worker "js dom @root core" "js/worker"` 처럼 **부분 패키지만** 컴파일하면 `StandardBuiltins.installAll` 이 `Class.forName` 으로 찾는
선택적 내장 설치기(`js/builtins/stringarray`, `datajson` 등)가 컴파일되지 않아 `'abc'.indexOf` 가 없는 realm 이 만들어지고 `e.data.split is not a function` 같은
엉뚱한 실패가 난다. 따라서 이 작업의 시험은 **js 전체와 의존 패키지를 함께 컴파일**한다:

```text
sh scripts/pkgtest.sh worker "js dom html css net storage core content layout render download media @root" "js/worker" --filter org.duckdns.hjow.swingwebpanel.js.worker
```

## 변경 파일 (이 브랜치)

- `src/org/duckdns/hjow/swingwebpanel/js/worker/**`(WorkerApi, WorkerHandle, WorkerScope, WorkerGlobals, WorkerGlobal, WorkerTimers, WorkerWebHost, WorkerLimits, WorkerParent,
  Messages, ScriptContext, PageContext, ScriptUtil, BroadcastApi, BroadcastChannelTarget, BroadcastHub, package-info)
- `src/org/duckdns/hjow/swingwebpanel/dom/ErrorEvent.java`
- 가산 수정: `js/dom/DomInterfaces`(`installEventCore`, `ErrorEvent`), `js/host/MessagingApi`(`installMessageEvent` 공개), `js/host/WindowGlobals`(WorkerApi 설치/종료),
  `js/host/web/{WebApis,XhrApi}`(`WebApis.Host` 로 페이지/worker 문맥 분리)
- 시험: `tests/src/org/duckdns/hjow/swingwebpanel/js/worker/{WorkerErrorTest,WorkerLifecycleTest,WorkerMessagingTest,WorkerGlobalScopeTest,WorkerTestSupport}`

## 설계 (이전 작업자 코드를 읽고 확인한 것)

- 전용 worker 하나 = `WorkerScope` 하나 = 자기 `JsRealm` + 자기 `DomBridge`(이벤트 인터페이스만: `DomInterfaces.installEventCore`) + **자기 데몬 스레드**
  (`swingwebpanel-worker-N`, 단일 스레드 `ScheduledThreadPoolExecutor`). realm 은 자기 스레드만 만지며 모든 작업(메시지, 타이머, 요청 결과)이 그 스레드에 줄 서서
  실행되고 마이크로태스크 검사점이 뒤따른다. 페이지 스레드와 worker 스레드 사이에는 **불변의 직렬화 트리(`ClonedValue`)와 문자열/숫자만** 건너간다.
- 스레드를 쓰는 이유: 무한 반복하는 worker 가 UI 와 다른 worker 를 막지 않아야 한다(공유 단일 큐로는 불가능). 대신 한도 `WorkerLimits.MAX_WORKERS_PER_PAGE`(16)로
  스레드 폭주를 막고, 페이지 종료/이동/`clean()`/`dispose()`/JavaScript 끄기에서 `WorkerApi.terminateAll()` 이 기다리지 않고 모두 종료한다(스크립트 취소 + 요청 취소 +
  `executor.shutdownNow()`). 스레드 종료/누수는 `WorkerLifecycleTest` 가 확인한다.
- 페이지 쪽 `Worker` 객체 = `WorkerHandle`(EventTarget). worker 에서 오는 모든 호출은 worker 스레드에 도착해 페이지의 `ScriptContext.post` 로 페이지 작업이 되어 전달된다.
- 요청 API(`fetch`, `XMLHttpRequest` 등)는 페이지와 같은 코드(`js/host/web/**`)와 같은 컴포넌트 요청 서비스(`env.getWebRequestSource()`)를 쓰므로 User-Agent, 추가
  헤더, 쿠키, SSL 확장 지점, 스킴 제한, CORS 가 그대로 적용된다(`WorkerWebHost`).

## 현재 상태 (2026-11-17 조정자가 정리)

작업자의 커밋 701~710 을 master 위로 rebase 하고(충돌 1건: `WindowGlobals` 의 필드 두 개를 모두 남김) master 의 `WebSocketApi`(새로 추가됨)가 `WebApis.page()` 를 쓰던 것을 `WebApis.Host` 구조(페이지와 worker 공용)에 맞게 고쳐(711) 컴파일했다. 병합 뒤 전체 `./test.sh` 를 JDK 6u45 로 돌려 **2588 통과 / 0 실패 / 12 건너뜀**(master 2543 + worker 45건, 534277 ms)이다. 건너뜀 12건은 선택적 Opus 원본 벡터 파일 부재.

- [x] 컴파일 확인, 기존 시험 22건 통과, 전역 범위 시험 4건 추가(`WorkerGlobalScopeTest`)
- [x] BroadcastChannel(`WorkerBroadcastTest` 3), 요청 계층(`WorkerRequestTest` 3: 헤더·쿠키·CORS·취소)
- [x] MessageChannel/MessagePort 를 worker 에 설치하고 MessagePort 전달(`WorkerPortTest` 5)
- [x] SharedWorker(컴포넌트별 `WorkerRegistry`, `connect` 이벤트, 소유자 수명, `WorkerSharedTest` 6)
- [x] `window.postMessage` targetOrigin 과 `ports`(`WorkerMessagingTest` 6)
- [x] 리스너 안의 무한 반복 중단을 worker 오류로 보고, 한도 중단이 EDT 를 막지 않음(`WorkerErrorTest` 6, `WorkerLifecycleTest` 12)
- [x] 소스 규칙 점검 0 위반(`tools/sourcecheck`, 35 파일)
- [x] AGENTS.md P20-09-b, P20-09 체크, FEATURE_MATRIX WEB-08 갱신(조정자)

알려진 한계: 이 구현은 worker 마다 데몬 스레드 하나(최대 `WorkerLimits.MAX_WORKERS_PER_PAGE`=16)를 쓴다. 모듈 worker(`type: 'module'`)는 지원하지 않고 `error` 이벤트로 거부한다(`WorkerApi` 설명). worker 안의 `WebSocket`, `IndexedDB`, `Cache`, `Atomics`/`SharedArrayBuffer` 공유 메모리는 `js/worker` 에 구현이 없다(소스 검색으로 확인했고 시험 근거도 없으므로 미구현으로 본다). Chromium 소스와의 비교는 하지 않았다.
