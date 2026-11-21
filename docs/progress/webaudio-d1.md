# WEBAUDIO-D1 작업 기록 (P25-06 AudioWorklet)

이 문서는 WEBAUDIO-D1 작업자의 기록이다. 작업 번호 대역은 `20261122-360` ~ `389` 이다(이번에 360~365 를 썼다). 앞 단계의 기록: `docs/progress/webaudio.md`(A1/A2, 엔진), `docs/progress/webaudio-b.md`(디코딩), `docs/progress/webaudio-c.md`(C1, JavaScript 바인딩), `docs/progress/webaudio-c2a.md`(C2a, 소리 출력과 `ScriptProcessorNode` 엔진), 그리고 Worker 구조 `docs/progress/worker.md`.
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**`, 루트 `README.md`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

범위(D1): P25-06 `audioWorklet.addModule`(공통 요청 계층과 모듈 로더), `AudioWorkletGlobalScope`(`registerProcessor`, `currentFrame`/`currentTime`/`sampleRate`), `AudioWorkletNode`/`AudioWorkletProcessor`(`process`, `parameterDescriptors`, `port`), 그리고 그것이 도는 스레드·시간 예산·수명. 실시간 출력 장치 연결과 `<audio>` 어댑터(C2b), 최종 검증과 문서(D2)는 하지 않았다.

## 1. 규격 판본과 조회 기록

조회는 `tools/webfetch`(JDK 6 으로 빌드, 조회는 JRE 25: `WEBFETCH_JAVA_HOME`)로 했다. 조회 날짜는 모두 2026-11-22 이다.

| 자료 | URL | 판본 | 읽은 부분 |
| --- | --- | --- | --- |
| Web Audio API 1.0 권고안 | https://www.w3.org/TR/2021/REC-webaudio-20210617/ | W3C Recommendation, 17 June 2021 (본문 604341 바이트, `last-modified` 2021-06-08) | 1.32 The AudioWorklet Interface 전체(1.32.1 개념, 1.32.2 `AudioWorkletGlobalScope`, 1.32.3 `AudioWorkletNode`와 옵션, 1.32.4 `AudioWorkletProcessor`와 `process` 인수, 1.32.5), 1.5.3 노드 수명, 2.4(렌더링 알고리즘의 AudioWorkletNode 단계) |
| HTML Standard(Living) "Worklets" 장 | https://html.spec.whatwg.org/multipage/worklets.html | WHATWG Living Standard, `last-modified` 2026-11-20 (본문 24222 바이트) | `Worklet.addModule()` 알고리즘, added modules 목록, 모듈 응답 맵, 수명 |

- `https://www.w3.org/TR/worklets-1/` 은 조회 시점에 HTML Standard 의 목차(`https://html.spec.whatwg.org/multipage/`)로 연결되었다(Worklets 가 HTML Standard 로 이동했다). 그래서 위 장을 직접 읽었다.
- **`addModule()` 실패의 종류(HTML Standard 와 실제 Chromium 에 맞춤. 20261122-367 에서 조정자 결정으로 고침)**: 처음에는 브리프의 지시("실패는 모두 `AbortError`")대로 만들었으나, 조정자가 이 PC 의 실제 Chromium 156(내장 브라우저, 2026-11-22)에서 `audioWorklet.addModule(blobURL)` 를 직접 실행해 확인한 결과와 HTML Standard 의 알고리즘이 같아서 그쪽으로 바꿨다. 동작: (1) **가져오기 실패**(404, 허용되지 않는 스킴, 교차 오리진 CORS 거부, 모듈 MIME 타입 오류, 가져올 수 없는 import)는 `AbortError`(`DOMException`); (2) **구문 오류**와 **링크 실패**(import 한 이름을 모듈이 export 하지 않음)와 해석할 수 없는 지정자는 "error to rethrow" 이므로 **그 오류 자체**(`DOMException` 이 아닌 `SyntaxError`/`TypeError` 오류 객체)로 reject; (3) **모듈이 실행 중 던지는 예외**(`throw`, 모듈 안의 `registerProcessor` 중복 이름과 생성자 아님 같은 오류 포함)는 **reject 하지 않는다**: 페이지 콘솔에 처리되지 않은 예외(`Uncaught ...`)로 보고하고 프라미스는 resolve 한다(예외 전에 등록한 이름은 등록된 채로 남는다). 모듈은 평가에 이르렀으면(실행 중 던졌어도) 평가한 것으로 쳐서 다시 추가해도 다시 실행하지 않고, 구문 오류/링크 실패/가져오기 실패는 기억하지 않아 다시 추가하면 다시 시도한다(규격의 module map 은 실패도 기억한다).
- 주소를 해석할 수 없으면 `SyntaxError` **`DOMException`**(규격과 같다. (2)의 오류 객체와 다르다), 인수가 없거나 `credentials` 가 잘못되면 거부된 프라미스의 `TypeError`(WebIDL 의 프라미스 반환 연산 규칙).
- **확인한 것과 하지 못한 것**: Chromium 156 에서 확인한 것은 정상 모듈 resolve, 구문 오류의 `SyntaxError` reject, 평가 중 예외 resolve(콘솔 보고), 모듈 안의 `registerProcessor` 중복 이름/`{}` 인수 resolve, 404 `AbortError` 이다(조정자의 실행 결과이며 이 작업자가 직접 실행한 것이 아니다). **링크 실패(없는 export 의 import)와 해석할 수 없는 지정자(`import 'bare'`)를 `SyntaxError`/`TypeError` 로 reject 하는 것은 HTML Standard("error to rethrow")를 따른 것이고 Chromium 에서 확인하지 않았다.** 링크 실패는 엔진이 던진 `SyntaxError`(메시지가 `The requested module '...'` 로 시작)로 식별한다(`WorkletScope.isLinkError`). Chromium 의 `audio_worklet*.cc` 는 이번에도 읽지 않았다.

## 2. 구현 파일

엔진(순수 Java, `src/org/duckdns/hjow/swingwebpanel/media/webaudio/`):

| 파일 | 역할 |
| --- | --- |
| `AudioWorkletNode` | 엔진 노드. 입력/출력 수(0~32, 둘 다 0 은 `NotSupportedError`), `outputChannelCount` 규칙(1.32.3.3.2), 이름 있는 파라미터(`ParamDescriptor`), `Processor`/`ProcessorFactory`/`ErrorListener` 인터페이스, active source flag 와 컨텍스트에 붙잡히기, 건너뜀(`SKIPPED`), 실패 뒤 영구 무음, 제어 메시지로 지연 생성(준비 안 되면 다음 quantum 에 다시, 최대 2000번) |
| `WorkletProcessorException` | `processorerror` 가 싣는 메시지/파일/줄/열을 순수 값으로 담는 예외(렌더링 쪽 realm 을 제어 스레드가 만지지 않게 한다) |

JavaScript 쪽(`src/org/duckdns/hjow/swingwebpanel/js/host/webaudio/`):

| 파일 | 역할 |
| --- | --- |
| `WorkletApi` | 페이지 쪽: `BaseAudioContext.audioWorklet`, `Worklet`/`AudioWorklet`(`addModule`), `AudioWorkletNode`(생성자, `parameters`, `port`, `onprocessorerror`), `AudioParamMap`(maplike), 컨텍스트별 스코프 수명(`contextClosed`, `dispose`) |
| `worklet/WorkletScope` | `AudioWorkletGlobalScope` 와 그 안에서 실행되는 모든 것: realm, 공정 잠금, worklet 스레드, 작업 큐, 모듈 로드, 프로세서 생성, `process` 호출, 한도, 폐기 |
| `worklet/WorkletGlobals` | 전역 범위 설치: `registerProcessor`(규격 단계별 오류), `currentFrame`/`currentTime`/`sampleRate`, `AudioWorkletProcessor` 생성자와 `port` |
| `worklet/ProcessorRunner` | `process(inputs, outputs, parameters)` 호출: 엔진 오디오 ↔ `Float32Array`(`byte[]` 저장소 재사용), 얼린 배열과 `parameters` 객체, 전달되어 분리된 배열의 재생성 |
| `worklet/ModuleGraphFetcher` | 모듈 그래프를 realm 잠금 **밖에서** 선취득(진입 모듈 가져오기 → 엔진 파서로 import 찾기 → 반복) |
| `worklet/ProcessorObject`, `FloatBytes`, `WorkletHost`, `WorkletLimits`, `package-info` | 프로세서 인스턴스(`port` 를 가진 일반 객체), float↔바이트 변환, 페이지 연결 인터페이스, 이름 있는 한도 상수 |

기존 파일에 한 연결(조정자가 C2b 와 조율할 수 있도록 목록): `js/host/webaudio/WebAudioApi.java` **4곳**(필드 1개, `installAll()` 끝의 `worklets = new WorkletApi(this, nodes); worklets.install(g);` 2줄, `unregister()` 의 `worklets.contextClosed(c)` 호출, `dispose()` 의 `worklets.dispose()` 호출). 새 공개 파일 `js/worker/ChannelPorts.java`(`MessagePort` 설치와 두 문맥에 끝이 하나씩 속한 채널 만들기; `PortApi`/`PortCore` 가 package-private 이라 같은 패키지에 새 파일로 열었다. 기존 파일은 고치지 않았다). 시험 `tests/.../js/host/webaudio/WebAudioInterfaceTest.java` 의 두 단언(`testInterfaceMembers` 의 표에 `audioWorklet` 과 새 인터페이스 4행, `testExcludedNames` 에서 worklet 이름과 `audioWorklet` 의 "없어야 함" 제거)을 이 기능에 맞게 고쳤다(C2b 가 같은 단언을 `ScriptProcessorNode` 때문에 고칠 것이므로 병합 때 충돌할 수 있다).

시험:

| 파일 | 건수 | 확인하는 것 |
| --- | --- | --- |
| `media/webaudio/AudioWorkletNodeTest` | 13 | 엔진 노드를 Java 프로세서로: 이득, 사인 생성기, active source flag, 입력 null, 파라미터(a/k-rate, 램프), 채널 구성, 검증, 실패와 통지, 건너뜀, 지연 생성, 파라미터 구동 |
| `js/host/webaudio/worklet/AudioWorkletRenderTest` | 3 | 이득, 사인(`currentFrame`/`sampleRate`), bypass(규격 예제) 의 렌더링 PCM 이 닫힌 식 |
| `.../AudioWorkletParametersTest` | 5 | a-rate/k-rate 램프의 프레임별 값, `AudioParamMap` 인터페이스 전체, `parameterData`, 명목 범위, 출력 연결로 파라미터 구동 |
| `.../AudioWorkletModulesTest` | 8 | `addModule` 의 해결 시점, 실패의 종류(가져오기 실패 `AbortError`, 구문 오류/링크 실패는 그 `SyntaxError`/`TypeError` 오류 객체로 reject, 실행 중 예외는 resolve + 콘솔 보고, 닫힌 컨텍스트 `InvalidStateError`), 한 번만 평가, 상대 import/순환/절대 주소, `registerProcessor` 의 오류 전부(15개 호출), 생성자 오류와 속성, 전역 범위의 구성 |
| `.../AudioWorkletMessagingTest` | 7 | 양방향 메시지, 구조화 복제(Map/Date/객체), 전달한 `ArrayBuffer`, 순서(200건), 오프라인 렌더링 전에 보낸 메시지가 첫 quantum 전에 전달, 닫힌 포트, `start()` 규칙, 옵션 객체 |
| `.../AudioWorkletFailureTest` | 6 | `process`/생성자의 예외 → `processorerror`(메시지, 파일, 줄)와 영구 무음과 이웃 무영향, `process` 없음/삭제, 무한 반복의 중단, 반환 값과 마이크로태스크 순서, 메시지 핸들러의 예외 |
| `.../AudioWorkletChannelsTest` | 4 | 입력 둘 출력 둘, 채널 수(입력이 따라가는 것, `explicit`, 연결 없음, `outputChannelCount`), 출력이 매번 0 으로 채워짐, 얼린 배열과 재사용과 전달 뒤 재생성 |
| `.../AudioWorkletRealtimeTest` | 6 | 실시간: 분석기가 사인을 봄, 프레임 계수가 128 의 양의 배수로 늘고 건너뜀은 드물며(25 % 이하) 시계를 느슨하게 따라감, 느린 `process` 의 시간 한도, **바쁜 worklet 이 렌더링 스레드를 붙잡지 않음**, 오프라인은 바쁜 worklet 을 기다림, 비용 측정(단일 프로세서: 느슨한 하한 0.1 배, 가장 빠른 것 최대 3번), 여러 프로세서의 비용(출력만). 몹시 바쁜 컴퓨터에서는 두 비용 시험이 이유를 밝히고 건너뜀. 벽시계 단언의 완화는 4.2절 |
| `.../AudioWorkletLifecycleTest` | 6 | 스레드 시작/종료(close, 오프라인 완료, 페이지 폐기), 페이지 20개 반복, 폐기된 페이지에는 아무것도 안 닿음, **모듈을 가져오는 중 페이지 교체**(요청 취소) |
| `.../AudioWorkletComponentTest` | 3 | 실제 `SwingWebPanel` + 실제 HTTP 서버: User-Agent/추가 헤더/쿠키, 리디렉션 뒤 상대 import, MIME(`text/plain` 거부), CORS, 콘솔이 EDT 로, 페이지 교체/clean/JS 끔/dispose 때 스레드 종료 |

(도우미 `WorkletPage` 는 스위트가 아니다.)

## 3. 설계 결정

### 3.1 스레드: realm 은 한 번에 한 스레드, 규격대로 렌더링 스레드에서 `process` 를 실행

- worklet realm 하나(컨텍스트마다)는 **공정한 `ReentrantLock`**(`WorkletScope.lock`) 하나로 직렬화한다. 이 realm 을 쓰는 스레드는 셋이다: (1) **worklet 스레드**(`swingwebpanel-audio-worklet-N`, 데몬, 첫 `addModule` 에 시작): 모듈 평가와 realm 작업(포트로 오는 메시지 전달); (2) **오디오 컨텍스트의 렌더링 스레드**: 프로세서 생성(제어 메시지)과 quantum 마다 `process` 호출; (3) 그 밖은 아무도 없다. 페이지 스레드는 realm 을 절대 만지지 않고 작업 큐와 일반 데이터(이름, 설명, 직렬화된 메시지)로만 주고받는다. 렌더링 스레드는 페이지 쪽(문서 큐)을 기다리지 않으므로 교착이 없다(렌더링 스레드가 기다릴 수 있는 것은 realm 잠금뿐이고, 그 잠금을 쥔 worklet 스레드는 렌더링 쪽 잠금을 필요로 하지 않는다).
- 모듈 **가져오기는 잠금 밖에서** 한다(`ModuleGraphFetcher`): 진입 모듈을 요청 계층으로 가져와 엔진의 파서(`Parser.parseModule`, realm 이 필요 없다)로 import 를 찾고 그래프가 완성될 때까지 되풀이한 뒤에야 잠금을 쥐고 파싱/링크/평가한다. 그래서 느린 네트워크가 렌더링 스레드를 막지 않는다(시험: 6초 걸리는 모듈, 페이지 교체 479 ms 만에 스레드 종료).
- **오프라인은 결정적**: 오프라인 컨텍스트의 렌더링 스레드는 `lockInterruptibly()` 로 기다리고(기한 없음, 취소는 인터럽트), quantum 을 처리하기 전에 **쌓인 realm 작업(메시지)을 직접 실행**한다. 그래서 `startRendering()` 전에 보낸 메시지는 첫 quantum 전에 항상 전달된다. 프로세서는 규격대로 첫 quantum 시작의 제어 메시지에서 만들어진다(렌더링 중이 아닌 컨텍스트에서는 만들어지지 않으므로 대화는 실시간 컨텍스트에서 시험했다).
- **실시간은 기다리지 않는다**(시간 예산): 렌더링 스레드는 `tryLock(2 ms)` 만 시도하고, 그래도 worklet 스레드가 쥐고 있으면(모듈 평가, 긴 메시지 핸들러) 그 노드는 그 quantum 에 **무음**(`SKIPPED`)이다. 같은 quantum 의 다른 노드는 시도조차 하지 않는다(`skipFrame`)고 해서 렌더링 스레드가 잃는 시간이 quantum 마다 2 ms 한 번이다. 건너뛴 quantum 과 render quantum 보다 오래 걸린 호출은 세어서 **페이지 콘솔에 초당 한 번까지 `warn` 으로** 알린다("could not keep up with the clock"; 멀쩡한 worklet 은 보고하지 않는다).
- **무한 루프 방어**: `process` 한 번은 `ExecutionLimits` 로 **200만 단계**(`WorkletLimits.PROCESS_STEP_LIMIT`)와, 실시간에서는 **100 ms**(`REALTIME_PROCESS_TIME_LIMIT_MILLIS`)의 한도가 있다. 넘으면 던진 것처럼 실패한다(`processorerror`, 영구 무음). 오프라인은 시간 한도 없이 단계 한도만이다(결정성). 메시지 핸들러/모듈/생성자는 페이지의 작업 한도(`env.getTaskTimeLimitMillis()`)와 기본 단계 한도.
- **폐기**: `dispose()` 는 어느 스레드에서나 기다리지 않고: 폐기 표시, 큐 비우기, 실행 중인 스크립트 취소(`limits.cancel()`), worklet 스레드 인터럽트. 그 뒤 `process` 는 `SKIPPED`(무음)를 돌려주고 늦게 도착한 것은 페이지 쪽 `postTask` 가 버린다. 취소로 중단된 것은 실패가 아니다(`processorerror` 없음).

### 3.2 배열 재사용과 GC 부담

규격 1.32.4.3.1 은 "호출 사이에 배열을 재사용하고, 토폴로지가 바뀌거나 배열이 전달되면 새로 만든다"고 한다. `ProcessorRunner` 는 입력/출력 채널마다, 파라미터마다 `Float32Array` 하나와 `byte[]` 하나를 두고 제자리에서 채운다. 바깥 배열(`inputs`, `outputs`, 그 요소)과 `parameters` 객체는 `Object.freeze`(realm 을 만들 때 잡아 둔 것) 한 `FrozenArray` 이므로 토폴로지(채널 수, 파라미터 배열 길이)가 바뀔 때와 스크립트가 배열의 버퍼를 전달(`postMessage` 의 전달 목록)해 분리했을 때만 다시 만든다. 출력의 `byte[]` 는 호출마다 0 으로 채운다. quantum 마다의 할당은 호출 인수 배열(`JsValue[3]`)과 스크립트가 만드는 것뿐이다. 값이 일정한 a-rate 블록과 k-rate 는 길이 1 배열(규격이 허용), 아니면 프레임마다.

### 3.3 전역 범위의 구성

`JsRealm.create()` 의 ES2015 내장 객체 + `console`(→ 페이지 콘솔; 위치 있는 `ConsoleRecord` 를 `PageScripts.workerConsole` 로 보내므로 출처는 `WORKER` 로 보인다) + 채널 메시징(`MessagePort`, `MessageEvent`; `MessageChannel` 은 규격대로 지운다) + `registerProcessor`, `AudioWorkletProcessor`, `currentFrame`/`currentTime`/`sampleRate`. window, document, 타이머, `fetch` 등은 없다(시험으로 확인). Worker 의 `WorkerScope`/`WorkerGlobals` 는 **재사용하지 않았다**: 전용 스레드 + 클래식 스크립트 + `self`/`importScripts`/타이머/요청 API 가 있는 구조라 worklet(모듈 전용, 타이머/요청 없음, 렌더링 스레드가 실행)과 맞지 않고, 대신 그 아래 층(`js.worker.PortApi/PortCore/PortTarget` 의 메시지 채널, 구조화 복제 `StructuredClone`, `ScriptContext`)을 새 공개 진입점 `ChannelPorts` 로 열어 재사용했다. 페이지 쪽 포트는 같은 `PortTarget` 이다(`node.port instanceof MessagePort`).

### 3.4 그 밖의 결정

- 프로세서 생성자가 받는 옵션: 규격대로 옵션 사전을 구조화 복제해 worklet realm 에서 역직렬화한 객체(`numberOfInputs`, `numberOfOutputs`는 항상, 나머지는 준 것만). 직렬화할 수 없으면 생성자에서 `DataCloneError`.
- `AudioWorkletProcessor` 생성자는 노드 생성 중(pending processor construction data)에만 동작하며 아니면 `TypeError`. 생성자가 `AudioWorkletProcessor` 인스턴스를 만들지 않으면 실패로 친다.
- `registerProcessor` 이름은 `addModule` 이 resolve 되기 전에 페이지 작업으로 페이지에 알려진다(규격 1.32.1 의 "채워진 뒤에 resolve"; 페이지 작업은 순서를 지키므로 이름 공개 작업이 resolve 작업보다 앞선다).
- 컨텍스트가 닫히거나(`close()`) 오프라인 렌더링이 끝나면(성공/실패) 스코프를 폐기한다(`WebAudioApi.unregister` → `WorkletApi.contextClosed`): 스레드가 끝나고 프로세서는 무음이다.
- 스코프 수 상한은 컨텍스트 수 상한(C1: 실시간 6, 오프라인 16)에 묶인다. 스레드 이름 접두사 `swingwebpanel-audio-worklet-`(데몬), 정적 가변 상태 없음.
- `SecureContext`(규격은 `[SecureContext]`)는 강제하지 않는다. 호스트가 Swing 응용이고 C1 의 자동 재생 결정과 같은 이유다.
- `addModule` 의 `credentials: "omit"` 은 요청 계층에 "쿠키를 전혀 보내지 않는" 모드가 없어 `same-origin` 과 같다(`include` 만 모든 오리진에 쿠키).

## 4. 검증 (JDK 6u45, 이 PC, 실제로 실행한 것)

| 명령 | 결과 |
| --- | --- |
| `sh scripts/pkgtest.sh wd1 "media" "media/webaudio" --filter org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNodeTest` | 13 통과 / 0 실패 |
| `sh scripts/pkgtest.sh wd1 "js dom html css net storage core content layout render download media @root" "js/host/webaudio" --filter org.duckdns.hjow.swingwebpanel.js.host.webaudio` | **137 통과 / 0 실패 / 0 건너뜀**(20261122-367 시점, 약 59 초; coordinator 의 rebase 로 C2b 시험이 더해진 뒤): C1/C2b 의 90건 + worklet 47건. 이후(20261122-368, 4.2절) worklet 시험이 48건이 되어 `...js.host.webaudio.worklet` 은 **48 통과 / 0 실패 / 0 건너뜀**(약 32 초). 같은 명령을 다른 작업자가 컴퓨터를 쓰는 동안 돌렸을 때 C1 의 시간 민감한 시험 `WebAudioLifecycleTest`(`testResultsAreTasksOfThePage`, `testConsoleAndErrorsFromHandlers`, `testDisposeCancelsAnOfflineRendering`)가 번갈아 한두 건 실패했다가 다시 돌리면 통과했다(worklet 과 무관한 타이밍 시험, 부하 때문으로 보인다. 원인을 파고들지는 않았다) |
| `media.webaudio` 전체(엔진 A1/A2/B/C2a + 새 13건, pkgtest 컴파일 뒤 `TestRunner --scan ... --filter org.duckdns.hjow.swingwebpanel.media.webaudio`) | 344 통과 / 0 실패 / 0 건너뜀 |
| `sh build.sh` | OK(엔진/브라우저 경계 검사 통과) |
| `StaticStateAuditTest`, `ThreadCreationAuditTest`, `JarContentTest`, `BrowserJarsTest`(빌드한 JAR 위에서) | 각각 통과 |
| `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/js/host/webaudio tests/src/org/duckdns/hjow/swingwebpanel/js/host/webaudio ...` | 0 위반 |

전체 회귀(`test.sh`)는 실행하지 않았다(조정자 몫).

**시험의 성격**: 오디오 기대값은 시험이 닫힌 식으로 만든다: 상수에 이득(0.5 × 0.25 = 0.125), 사인 `sin(2 pi 250 n / 8000)`(오차 1e-6, `float` 저장), 선형 램프 `4 n / 512`(1e-5), k-rate 는 quantum 첫 프레임의 램프 값, 두 입력의 합 1 과 차 0.5, 결정적 오프라인 위상(`sin(2 pi 1000 n / 8000)` 4096 표본, 오차 1e-4). 메시지/오류/수명은 페이지에서 관찰되는 값(콘솔 줄, 이벤트의 `message`/`filename`/`lineno`, 스레드 수)이다.

### 4.1 변이 시험(손으로, 고치고 시험이 실패하는지 확인하고 되돌림)

| 변이 | 실패한 시험 |
| --- | --- |
| 엔진: `activeSource = result == ACTIVE` → `true` | `AudioWorkletNodeTest.testActiveSourceFlag` |
| 엔진: 실패한 quantum 의 출력을 무음으로 되돌리지 않음 | `AudioWorkletNodeTest.testFailureEndsTheProcessor` |
| `ProcessorRunner`: 전달되어 분리된 배열을 다시 만들지 않음 | `AudioWorkletChannelsTest.testTheFrozenArraysAndTheirReuse` |
| `ProcessorRunner`: 출력을 호출마다 0 으로 채우지 않음 | `AudioWorkletChannelsTest.testOutputsAreZeroFilledForEveryCall` |
| `registerProcessor`: 이름 중복 검사 제거 | `AudioWorkletModulesTest.testRegisterProcessorErrors` |
| `WorkletScope.evaluate`: 모듈 평가 중 예외를 resolve 하지 않고 reject(`AbortError`) | `AudioWorkletModulesTest.testAnExceptionWhileTheModuleRunsResolves`(20261122-367) |
| 모듈 import 를 리디렉션 뒤 최종 주소가 아니라 처음 주소로 해석 | `AudioWorkletComponentTest.testAModuleThroughTheRequestLayer` |
| 실시간에서 `tryLock(2 ms)` 대신 `lock()`(블로킹) | `AudioWorkletRealtimeTest.testABusyWorkletDoesNotHoldTheRenderingThread`("skipped for 1.0 quanta", 150 기대) |
| 비용 측정의 하한 `MINIMUM_TIMES_REAL_TIME` 0.1 → 50(하한이 살아 있는지) | `AudioWorkletRealtimeTest.testTheCostOfAProcessor`("renders at 0.1 x real time or faster ...: 32.9 x", 20261122-368) |
| 비용 측정의 게인 0.5 → 0.4(잰 렌더링이 일을 했는지 보는 마지막 표본 검사) | `AudioWorkletRealtimeTest.testTheCostOfAProcessor`("the last sample of the rendering that was measured (expected 0.5 ... was 0.4)", 20261122-368) |

### 4.2 벽시계에 달린 시험 단언의 완화 (20261122-368, 조정자 요청)

**계기**: 조정자의 전체 회귀(50f399c)에서 `AudioWorkletRealtimeTest.testTheCostOfAProcessor` 한 건이 실패했다("one simple processor (gain) renders at least as fast as real time: 0.8 x"). 이 PC 에 다른 작업이 돌아 부하가 큰 때였고, 이 시험은 게인 프로세서의 10 초 렌더링이 실시간의 1배 이상이어야 한다고 벽시계로 단언했다(한가할 때 6~8배).

**선택과 근거** (조정자가 제시한 세 가지 중):
- **채택: 느슨한 하한 + 가장 빠른 값**. 단언은 실시간의 **0.1 배**(`MINIMUM_TIMES_REAL_TIME`)뿐이고(10 초 분량을 100 초 안에 렌더링), 단일 프로세서는 최대 3번(`MAXIMUM_ATTEMPTS`) 재서 가장 빠른 값을 쓴다(실시간에 이르면 바로 멈추고, 20 초의 예산(`RETRY_BUDGET_MILLIS`)이 다하면 되풀이하지 않는다). 속도는 계속 출력한다. 0.1 배는 멈춤이나 한 자릿수 이상의 퇴보를 잡지만 바쁜 컴퓨터는 잡지 않는다. 한가한 컴퓨터의 측정이 6~12배(빈 프로세서는 90~2000배)이므로 하한은 그 60 분의 1 이다. 하한이 살아 있음은 변이(0.1 → 50)로 확인했다(4.1절).
- **채택하지 않음: `ThreadMXBean.getCurrentThreadCpuTime()`**. 렌더링은 시험이 소유하지 않은 엔진의 스레드(오프라인 렌더링 스레드)에서 돌아 시험 스레드의 CPU 시간으로는 잴 수 없고, 그 스레드를 찾아 재려면 스레드 이름에 기대야 하며 스레드 CPU 시간의 지원은 JVM 선택 사항이다.
- **추가로, 잰 렌더링이 일을 했는지** 마지막 표본으로 확인한다(게인은 0.5 × 노드 수, 빈 프로세서는 0). 아무것도 하지 않은 렌더링이 빠르다고 측정되지 않게 한다(변이 0.5 → 0.4 로 확인, 4.1절).
- **너무 바쁜 컴퓨터에서는 건너뛴다**(이유를 밝힌 `Check.skip`, 실패가 아니다): 시험 실행기의 사례별 제한 시간은 60 초인데 부하가 심하면 렌더링 하나가 그 시간을 넘긴다. 빈 프로세서가 실시간의 5배(`MINIMUM_EMPTY_TIMES_REAL_TIME`; 한가할 때 90배 이상)보다 느리면 `testTheCostOfAProcessor` 는 나머지를 재지 않고 건너뛰며, 새 시험 `testTheCostOfSeveralProcessors`(게인 4개와 8개, 속도는 출력만)는 빈 프로세서가 5배보다, 또는 게인 하나가 2배(`MINIMUM_TIMES_FOR_SEVERAL`)보다 느리면 건너뛴다. 여러 프로세서의 측정을 별도 시험으로 뗀 것은 그 측정이 한가한 컴퓨터에서도 15 초 걸려 단언이 있는 시험의 제한 시간을 먹기 때문이다.

**같은 방식으로 완화한 다른 시험(`AudioWorklet*` 시험을 훑은 결과)**:
- `AudioWorkletRealtimeTest.testTheAudioOfAProcessorInARealTimeContext`: 프레임 계수가 "정확히 128 씩" 늘어야 한다는 단언은 worklet 스레드가 realm 의 잠금을 쥔 채 스케줄에서 밀리면(렌더링 스레드는 2 ms 만 기다리고 포기한다) 부하에서 깨질 수 있다. 이제 모든 차이가 128 의 양의 배수여야 하고(결정적), 정확히 128 이 아닌 호출은 **25 % 이하**(`MAXIMUM_SKIPPED_SHARE`)여야 한다. 시계 검사의 아래쪽은 0.4 배에서 0.1 배로 낮췄다(대기 시간에는 폴링의 지연도 들어 있다). 위쪽(`2 t + 0.5 s`, 실시간보다 훨씬 빠르게 렌더링하지 않음)은 부하에 흔들리지 않아 그대로이다.
- `testASlowProcessorIsStopped`: 300 ms 고정 창 뒤에 `currentTime` 이 0.1 초 앞섰는지 보던 것을, 앞서기를 최대 30 초까지 기다리는 방식으로 바꿨다.
- `testABusyWorkletDoesNotHoldTheRenderingThread`: 건너뛴 quantum 수의 위쪽 한도를 400(약 1 초)에서 2000(약 5 초)으로 올렸다("다시 재생한다"가 요지이고, 멈춘 realm 은 어차피 `waitFor` 에서 걸린다). 아래쪽 한도 40(기대 150의 27 %)은 그대로이다.
- `AudioWorkletComponentTest.testTheThreadsEndWithThePage`: 400 ms 뒤에 `rt.currentTime > 0.05` 이던 것을 `> 0`(시간이 흘렀다)으로 낮췄다.
- 그 밖의 `Date.now()`/`currentTimeMillis` 사용은 제한 시간(기다리는 상한)이거나 "시간 안에 끝났다"를 넉넉히 보는 것(`< 30000`, `< 12000`)이거나 출력뿐이다. "닫힌 컨텍스트의 프로세서는 더 이상 불리지 않는다" 같은 부재 확인은 부하가 클수록 통과하기 쉬우므로 그대로 두었다.

**부하에서의 확인** (JDK 6u45, 4코어 PC 에 CPU 를 먹는 perl 8개를 동시에 돌림. 아주 극단적인 부하이다):
- 중간 단계의 코드(하한 0.1 과 3번 재기만 하고 건너뛰기는 없던 것)는 이 부하에서 비용 시험이 60 초 제한 시간으로 실패했다(`TIMEOUT`). 완화 전의 원래 코드(하한 1.0 배)를 이 부하에서 돌려 보지는 않았다(조정자의 실패가 그 코드의 같은 종류의 실패이다).
- 완화 후 `AudioWorkletRealtimeTest` 만 돌리면 **4 통과 / 0 실패 / 2 건너뜀**(두 비용 시험이 "너무 바쁜 컴퓨터" 이유로 건너뜀. 빈 프로세서가 0.9~1.8 배였음). 실시간 시험은 모두 통과했고 `testABusyWorkletDoesNotHoldTheRenderingThread` 가 38~55 초 걸렸다(60 초 제한에 가깝다; 한가하면 3 초). worklet 시험 전체를 같은 부하에서 한 번 돌렸을 때(비용 시험을 건너뛰게 고치기 전 코드)는 46 통과 / 1 실패(비용 시험의 제한 시간) / 1 건너뜀이었고, 비용 시험 말고는 전부 통과했다.
- 한가한 컴퓨터에서는 `AudioWorkletRealtimeTest` 6 통과 / 0 실패 / 0 건너뜀, worklet 시험 전체 48 통과 / 0 실패 / 0 건너뜀.

**관찰(이 작업의 범위 밖, 원인을 파지 않음)**: 이 극단적인 부하에서는 worklet 이 없는 내장 노드(오실레이터 + 게인)만의 오프라인 10 초 렌더링도 4.8~16 초가 걸렸다(한가할 때 28~50 ms, 100~300배 느려짐). 곧 이 느려짐은 worklet 이 아니라 오프라인 렌더링 경로(C1/C2) 또는 시험 도우미(`WorkletPage` 가 페이지 작업을 2 ms 간격으로 폴링해 돌린다)가 코어 수보다 많은 프로세스와 경쟁할 때 스레드가 넘어갈 때마다 스케줄러 한 칸(약 15 ms)을 기다리기 때문으로 보이며, 이 구현의 렌더링 경로에 quantum 마다 스레드를 넘기는 곳이 있는지는 조정자가 볼 만하다. worklet 호출 경로 자체(`WorkletScope.runProcessor`)에는 오프라인에서 quantum 마다 스레드를 넘기는 곳이 없다(렌더링 스레드가 잠금을 쥐고 직접 실행한다).

## 5. 측정 (JDK 6u45, 공용 4코어 PC, 다른 작업자가 같이 돌 수 있는 상태라 참고용)

**엔진만(브라우저 없이 `JsRealm` 에서 `process` 흉내를 직접 호출, 워밍업 뒤)**: 빈 `process` 5.8 us, 128 프레임 이득 루프 280 us, 사인(`Math.sin`+위상) 248 us, 백색 잡음(`Math.random`) 195 us.

**프로세서 호출 전체(오프라인 10초 @ 48 kHz = 3750 quantum, `AudioWorkletRealtimeTest.testTheCostOfAProcessor` 와 게인 4개/8개는 `testTheCostOfSeveralProcessors`; 아래 표는 20261122-368 이전에 잰 값이고 이후 몇 번 다시 잰 값도 같은 범위였다: 게인 6.0~8.2배, 사인 8.6~9.7배, 잡음 9.8~12.3배, 게인 4개 2.0~2.2배, 8개 1.0배)**:

| 프로세서 | 10 초 렌더링 시간 | 실시간 대비 | quantum 당 노드 하나 |
| --- | --- | --- | --- |
| 빈 `process`(고정 비용: 배열, 변환, 호출) | 81 ms | 123.5 배속 | 22 us |
| 게인(상수 입력, 128 프레임 루프) 1개 | 1695 ms | 5.9 배속 | 452 us(워밍업 포함) |
| 사인 생성기 1개 | 1045 ms | 9.6 배속 | 279 us |
| 백색 잡음 1개 | 1127 ms | 8.9 배속 | 301 us |
| 게인 4개 | 4735 ms | 2.1 배속 | 316 us |
| 게인 8개 | 9488 ms | 1.1 배속 | 316 us |

해석: quantum 하나는 48 kHz 에서 2667 us 이다. **단순한 프로세서 하나는 quantum 의 약 10~17 %**, 이 PC 에서 게인 같은 단순 루프 **일곱 개 안팎이 실시간 한계**이다. 비용의 거의 전부는 `Float32Array` 요소 읽기/쓰기와 루프를 도는 인터프리터이고(프레임당 약 2.5 us), 이 구현이 더하는 고정 비용(배열 채우기, float↔바이트 변환, 호출, 마이크로태스크 검사점)은 호출당 약 22 us(0.8 %)이다. 그래서 복잡한 프로세서(수백 프레임당 수십 문장)는 실시간으로 재생하기 어렵고, 그 경우 렌더링 쪽은 장치 버퍼가 비는 것(C2a 의 끊김 정책)과 위 `warn` 으로 보인다. 프로세서가 `SharedArrayBuffer`/WASM 을 쓰는 최적화는 이 엔진에 없다.

## 6. 확인하지 못한 것과 한계

확인하지 못한 것:
- **실제 소리**(장치와 청취): 실시간 시험은 이 저장소의 C1 시계 드라이버(`ClockRenderDriver`, 소리 없이 벽시계 속도로 렌더링)와 분석기로 했다. C2a 의 `OutputRenderDriver`(장치)와 함께 돌려 보지 않았다(C2b 가 병합되지 않은 `main` 에서 작업했다). 워크릿 프로세서가 장치 출력의 끊김 정책 안에서 어떻게 들리는지는 P22-10(사람)에서 확인해야 한다.
- 실제 Chromium/Firefox 와의 비교. 규격 본문과 Chromium 의 알려진 동작만 근거로 했다.
- JRE 6u45 이외의 JRE. 매우 긴 실시간 재생(수 분)의 메모리.
- 프로세서 포트로 `MessagePort` 를 전달(transfer)하는 사용. 구조는 `ChannelPorts` 를 통해 같은 코드지만 시험하지 않았다.
- 소리 장치가 있는 컴퓨터에서 `latencyHint`/버퍼 크기에 따른 worklet 의 끊김 빈도.

한계와 규격/참조와의 차이(기능 표 제약에 옮길 것):
1. `addModule` 실패의 종류는 1절대로 HTML Standard 와 Chromium 에 맞췄다. 남은 차이: 실패한 모듈 가져오기를 기억하지 않는다(규격의 module map 은 기억한다); 링크 실패의 식별이 엔진의 메시지 접두사에 기댄다.
2. 실패한 모듈 가져오기를 캐시하지 않는다(규격의 module map 은 실패도 기억한다): 다시 호출하면 다시 시도한다. 같은 URL 은 realm 의 모듈 맵 덕에 한 번만 평가한다.
3. `credentials: "omit"` = `same-origin`. `[SecureContext]` 미강제.
4. worklet 전역에 `Event`/`EventTarget` 등 DOM 이벤트 핵심 인터페이스가 함께 보인다(규격은 `AudioWorkletGlobalScope` 에 노출하지 않는다). `window`, `document`, 타이머, 요청 API 는 없다.
5. worklet 안의 처리되지 않은 promise 거부는 보고하지 않는다(페이지 realm 은 `Uncaught (in promise)` 로 보고한다).
6. 실시간에서 realm 이 바쁘면 그 quantum 은 무음이다(규격은 렌더링 스레드가 스크립트를 항상 실행한다고 가정하므로 이 구현의 소리가 더 끊긴다). 건너뜀은 `warn` 으로 알린다.
7. 컨텍스트마다 worklet 스레드 하나(컨텍스트 수 상한 안에서). 한 realm 의 프로세서는 렌더링 스레드에서 순서대로 실행된다.
8. 프로세서가 한 번 실패하면 영구 무음이다(규격과 같다). 복구하려면 노드를 다시 만들어야 한다.
9. `processorerror` 의 `ErrorEvent.error` 는 `null` 이다(규격은 message/filename/lineno/colno 만 정한다).
10. `AudioWorkletNode` 의 프로세서는 컨텍스트가 렌더링하기 전에는 만들어지지 않는다(규격과 같다). 렌더링하지 않는 오프라인 컨텍스트에서는 `port` 메시지가 쌓이기만 한다.

## 7. 체크박스 후보 (체크는 조정자 몫)

- **P25-06**: 항목의 요구(`audioWorklet.addModule`(공통 요청 계층, 모듈 로더), `AudioWorkletGlobalScope`(`registerProcessor`, `currentFrame`/`currentTime`/`sampleRate`), `AudioWorkletNode`/`AudioWorkletProcessor`(`process`, `parameterDescriptors`, `port`), DOM 없는 별도 realm(P20-09 구조 재사용 = 메시지 채널 계층), 렌더링 스레드에서 JavaScript 실행의 한도와 무음 처리 규칙)은 **엔진 수준과 JavaScript 경로 모두 자동 검증으로 끝났다**(59건). 항목 문장이 "렌더링 스레드에서 JavaScript 를 실행하므로 실행 제한/중단과 처리 지연 시 무음 처리 규칙을 정한다"를 포함하며 그것도 구현·검증했다(3.1절). **체크 가능**하다고 본다. 단 실제 장치 출력에서의 청취는 사람 확인(P22-10)이다. P25 전체는 C2b, D2 가 남아 있다.

## 8. FEATURE_MATRIX WAU-09 갱신 초안

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-09a | `AudioWorkletNode` 엔진 노드(입력/출력 수, `outputChannelCount` 규칙, 이름 있는 파라미터, active source flag, 실패 뒤 영구 무음, 지연 생성, 건너뜀) | Web Audio API 1.0 REC 1.32.3, 1.5.3 | `media.webaudio.{AudioWorkletNode,WorkletProcessorException}` | `AudioWorkletNodeTest` 13 | 검증 완료(엔진) | 프로세서는 렌더링 스레드에서 호출(`Processor` 가 공급) |
| WAU-09b | `AudioWorklet.addModule`(공통 요청 계층, ES2015 모듈 그래프 선취득, 한 번만 평가; 실패: 가져오기 실패 `AbortError`, 구문 오류/링크 실패는 그 `SyntaxError` 로 reject, 실행 중 예외는 resolve 하고 콘솔 보고), `AudioWorkletGlobalScope`(`registerProcessor`, `currentFrame`/`currentTime`/`sampleRate`, `console`→페이지 콘솔), `AudioWorkletNode`/`AudioWorkletProcessor`(`process`, `parameterDescriptors`, `AudioParamMap`, `port`, `onprocessorerror`), 렌더링 스레드 실행과 한도 | 1.32, HTML Standard Worklets(2026-11-20), Chromium 156 에서 `addModule` 오류 동작 확인(2026-11-22, 조정자) | `js.host.webaudio.{WorkletApi,worklet.*}`, `js.worker.ChannelPorts` | `js/host/webaudio/worklet/` 시험 47건 | 검증 완료(자동. 장치 청취는 P22-10) | 6절 1~10(링크 실패의 `SyntaxError` reject 는 Chromium 미확인, 실패한 가져오기를 기억하지 않음, 실시간에서 realm 이 바쁘면 그 quantum 무음, `credentials: omit`=`same-origin`, `SecureContext` 미강제, 전역에 DOM 이벤트 인터페이스, worklet 의 미처리 거부 미보고) |

## 9. 다음 단계(D2)를 위한 메모

- 실시간 연결: C2b 가 `ContextApi.createRealtime` 를 `OutputRenderDriver.createContext(...)` 로 바꾸면 worklet 은 그 렌더링 스레드(`swingwebpanel-audio-render-N`)에서 `process` 를 호출한다. 이 코드는 어느 스레드에서 불리든 같게 동작한다(`WorkletScope.runProcessor`). 장치 드라이버는 렌더링을 장치 버퍼보다 앞서 하므로 `REALTIME_LOCK_WAIT_MILLIS`(2 ms)와 `REALTIME_PROCESS_TIME_LIMIT_MILLIS`(100 ms)가 장치의 지연(`interactive` 약 52 ms 버퍼)과 어떻게 맞물리는지 장치가 있는 PC 에서 확인하면 좋다.
- 한도와 대기 시간은 `WorkletLimits` 의 상수이다.
- 시험 도우미: `WorkletPage`(`PageFixture` + 스레드 안전한 콘솔 목록 + 모듈 텍스트 등록)를 쓰면 같은 방식의 시험을 더할 수 있다. 실시간 대화는 `new AudioContext()` 로(오프라인은 렌더링할 때만 프로세서가 생긴다).
- 시험을 `scripts/pkgtest.sh` 로 돌릴 때 소스 패키지는 `"js dom html css net storage core content layout render download media @root"`(컴포넌트 시험이 `SwingWebPanel` 을 쓰므로)를 준다. `"js media"` 만으로도 컴포넌트 시험을 뺀 나머지는 컴파일/실행된다.

## 조정자 요청

1. **AGENTS.md 체크박스**: 7절(P25-06 체크 가능).
2. **FEATURE_MATRIX**: 8절의 WAU-09a/b 로 WAU-09 행을 대체해 주세요. 3절 명시적 예외 표는 바뀌지 않았습니다.
3. **ARCHITECTURE.md**: 패키지 표에 `js.host.webaudio.worklet`(오디오 worklet 의 realm 과 스레드)와 새 공개 클래스 `js.worker.ChannelPorts`; 스레드 모델에 "오디오 worklet: 컨텍스트마다 worklet 스레드(`swingwebpanel-audio-worklet-N`, 데몬, 모듈 평가와 realm 작업)와 realm 공정 잠금. `process()` 는 오디오 컨텍스트의 렌더링 스레드가 호출(실시간은 `tryLock` 2 ms, 못 쥐면 그 quantum 무음 + 콘솔 경고, 오프라인은 기다리며 쌓인 메시지를 먼저 실행)"; "자원 소유 목록과 정리 담당(P21-01)" 표에 한 행: `WorkletScope`(realm, worklet 스레드, 작업 큐) ← 정리 담당 `WebAudioApi.unregister`/`dispose()` → `WorkletApi.contextClosed`/`dispose()` → `WorkletScope.dispose()`.
4. **README(루트)**: 사용 설명에 "`audioWorklet.addModule()` 로 `AudioWorkletProcessor` 모듈을 불러와 `AudioWorkletNode` 로 쓸 수 있다. 프로세서는 렌더링 스레드에서 도는 인터프리터 코드이므로 단순한 프로세서 일곱 개 안팎이 48 kHz 실시간 한계이고 스크립트가 바쁘면 그 quantum 은 무음이다(콘솔에 경고)"를 적어 주세요. 새 공개 Java API 는 없다(`SwingWebPanel` 의 공개 API 는 변하지 않았다).
5. **기존 파일 변경 목록(C2b 와의 병합 조율)**: `js/host/webaudio/WebAudioApi.java` 4곳(2절), `tests/.../js/host/webaudio/WebAudioInterfaceTest.java` 두 단언과 표 4행(2절). C2b 가 같은 파일을 고치면 충돌은 작은 줄 단위이다. `js/worker/ChannelPorts.java` 는 새 파일이다.
6. **규격 판단**: 처리 완료. 조정자가 결정한 대로 HTML Standard/Chromium 에 맞췄다(1절, 20261122-367). 링크 실패와 해석할 수 없는 지정자의 reject 는 규격만 따랐고 Chromium 에서 확인하지 못했다.
7. **`scripts/**`**: 변경 요청 없음. `scripts/pkgtest.sh` 설명의 시험 실행 예에 위 9절의 소스 패키지 목록을 메모해 주면 좋다.
8. **다음 브리프**: C2b(실시간 장치 연결, `ScriptProcessorNode` 바인딩, `<audio>` 어댑터)가 끝나면 D2(P25-08 최종 검증과 문서). worklet 이 장치 출력과 함께 도는 확인(P22-10, 사람)을 `docs/progress/media-play.md` "미확인" 표에 더해 주세요: 오디오 worklet 프로세서(사인, 이득)가 실제 스피커로 들리는지, 프로세서가 바쁠 때 끊김이 어떻게 들리는지, 7 개 안팎의 프로세서에서 끊김 없이 도는지.
