# WEBAUDIO-C 작업 기록 (P25 JavaScript 바인딩과 오프라인 렌더링/디코딩의 연결, C1 단계)

이 문서는 WEBAUDIO-C1 작업자의 기록이다. 작업 번호 대역은 `20261122-170` ~ `199` 이다(이번에 170~175 를 썼다. 175 는 B2 병합 뒤의 후속: 7절). 앞 단계의 기록: `docs/progress/webaudio.md`(A1/A2, 엔진), `docs/progress/webaudio-b.md`(디코딩 서비스).
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**`, 루트 `README.md`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

범위(C1): 순수 Java 엔진(`media.webaudio`) 위의 **JavaScript 바인딩**(`js.host.webaudio`)과 오프라인 렌더링/디코딩/컨텍스트 수명의 연결, 실시간 `AudioContext` 의 **JS 쪽 형태**(장치 출력과 `ScriptProcessorNode`, `<audio>` 어댑터는 C2). 기준 규격은 Web Audio API 1.0 권고안(2021-06-17, https://www.w3.org/TR/2021/REC-webaudio-20210617/ , `tools/webfetch` 로 2026-11-22 에 다시 내려받아 1.1(BaseAudioContext), 1.2(AudioContext), 1.3(OfflineAudioContext), 1.4(AudioBuffer, "acquire the content")를 읽었다).

## 1. 구현 파일

엔진 쪽 추가(`src/org/duckdns/hjow/swingwebpanel/media/webaudio/`): `RealtimeAudioContext`(실시간 컨텍스트: 상태, 지연, 드라이버 인터페이스 `Driver`/`Renderer`), `ClockRenderDriver`(장치 없이 벽시계 속도로 렌더링하는 드라이버), `AudioBufferSourceNode.acquireBuffer`(1 메서드 추가, 2.2절). 시험 `media/webaudio/RealtimeAudioContextTest` 7건.

JavaScript 쪽(`src/org/duckdns/hjow/swingwebpanel/js/host/webaudio/`, 새 패키지):

| 파일 | 역할 |
| --- | --- |
| `WebAudioApi` | 설치기와 소유자. 분류기 등록, 인터페이스 설치 순서, 함수/접근자 정의(엔진 예외를 DOMException/RangeError/TypeError 로 바꿈), 페이지 작업 전달(`postTask`), 이벤트 전달, 열린 컨텍스트 목록과 한도, 스레드 번호, `dispose()` |
| `WebAudioHostLimits` | 페이지 한도 상수(실시간 컨텍스트 6, 오프라인 컨텍스트 16, 오프라인 메모리 합 2^25 표본) |
| `Idl` | WebIDL 변환(`float`/`double`/`unsigned long`, 시퀀스, 사전, 열거형, 형식화 배열 읽기/쓰기) |
| `Members` | 엔진 클래스의 Java API 를 리플렉션으로 읽어 속성/연산을 짧은 명세(`a:fftSize=`, `p:gain`, `setValueAtTime`)에서 설치. 이름은 페이지를 만들 때 모두 검사한다 |
| `ContextValue`, `NodeValue`, `AudioBufferValue` | 스크립트 쪽 값: `EventTarget` 인 컨텍스트와 노드(엔진 객체와 스크립트 전용 상태), `AudioBuffer`(3절의 두 저장소) |
| `ContextApi` | `BaseAudioContext`, `AudioContext`, `OfflineAudioContext`, 이벤트 인터페이스, 팩터리 전부, `decodeAudioData`, 상태/프라미스/이벤트 규칙 |
| `NodeApi` | `AudioNode`, `AudioScheduledSourceNode`, `AudioDestinationNode` 와 노드 15종의 생성자/팩터리/옵션 사전, `connect`/`disconnect`, 버퍼·곡선·주기파·분석기 데이터·주파수 응답 |
| `ValueApi`, `BufferApi` | `AudioParam`, `AudioListener`, `PeriodicWave`, `AudioBuffer` |
| `OfflineRender` | 오프라인 렌더링 스레드(`swingwebpanel-audio-offline-N`), 일시 중단/재개/완료/취소 |
| `Promises`, `OfflineAudioCompletionEvent`, `AudioProcessingEvent` | 프라미스 도우미, 두 이벤트 클래스 |

기존 파일에 넣은 연결(조정자가 다른 작업자와 조율할 수 있도록 목록): `js/host/WindowGlobals.java` 3곳(필드 1개, `install()` 끝에 `WebAudioApi.install(page)` 1줄, `cancelRequests()` 에 `webAudio.dispose()` 4줄). 다른 기존 파일은 고치지 않았다. (후속 175 는 이 패키지의 `ContextApi.java` 한 파일만 고쳤다.)

시험(`tests/src/org/duckdns/hjow/swingwebpanel/js/host/webaudio/`): 도우미 `WebAudioPage`(스위트 아님), `WebAudioInterfaceTest` 7, `OfflineRenderingTest` 10, `OfflineContextTest` 6, `AudioBufferScriptTest` 5, `DecodeAudioDataTest` 6, `RealtimeContextScriptTest` 8, `WebAudioLifecycleTest` 8, `WebAudioComponentTest` 5(실제 `SwingWebPanel` + `TestHttpServer`), `DecodeDamagedDataTest` 9(후속 175, 7절) (합계 64건).

## 2. 설계 결정

### 2.1 바인딩 구조
- 스크립트 객체는 모두 `DomBridge` 의 래퍼(`HostObject`)이다. 컨텍스트와 노드는 `dom.EventTarget` 을 상속한 값 객체(`ContextValue`, `NodeValue`)가 엔진 객체를 쥐고, `AudioParam`/`AudioListener`/`PeriodicWave` 는 엔진 객체를 그대로 감싼다(그래서 같은 파라미터는 페이지가 붙잡고 있는 동안 같은 객체). 래퍼는 약하게 캐시되고 스크립트가 프로퍼티를 저장하면 고정된다(`DomBridge` 의 기존 규칙).
- 속성/연산의 대부분은 엔진의 공개 Java API 에서 따라 나온다(`Members`). 변환은 Java 형식을 따른다: `float`/`double` 은 유한해야 하고(WebIDL, 아니면 `TypeError`; `float` 넘침도 `TypeError`), `int` 는 `unsigned long`(2^31 이상은 가장 큰 `int` 로 제한해 엔진이 모두 범위 밖으로 거부), 엔진 열거형은 문자열(**속성 대입의 잘못된 이름은 무시, 인수와 사전 멤버는 `TypeError`** — WebIDL 규칙), `float[]`/`double[]` 은 시퀀스. 스크립트 전용 규칙이 있는 것만 손으로 썼다(버퍼, 곡선, 주기파, 분석기 데이터, 주파수 응답, `connect`/`disconnect`).
- 노드 옵션 사전: `AudioNodeOptions` 를 먼저, 그 다음 노드의 멤버를 적용한다(ConvolverNode 만 `disableNormalization` 을 `buffer` 보다 먼저: 정규화는 버퍼를 정할 때 적용되므로). undefined 는 건너뛴다. 팩터리(`createDelay(max)` 등)는 같은 코드를 쓴다.
- 이벤트 클래스는 `DomInterfaces` 의 분류기가 모르는 `Event` 하위 클래스를 모두 `Event` 로 부르므로, 이 API 가 만든 이벤트는 인터페이스의 프로토타입을 `bridge.wrap(e).setPrototypeOf(proto)` 로 미리 달아 전달한다(`MessageEvent` 의 생성자와 같은 방식). 그래서 기존 `DomInterfaces` 를 고치지 않았다.
- 비표준 이전 이름 `webkitAudioContext`/`webkitOfflineAudioContext` 를 같은 생성자로 둔다(오래된 페이지의 `window.AudioContext || window.webkitAudioContext`). 1.1 작업 초안의 `renderQuantumSize` 속성(값 128)도 둔다.
- 노출하지 않는 것: `MediaStream*` 인터페이스(EXC-14), `AudioWorklet`/`audioWorklet`(D), `ScriptProcessorNode`(C2). `createMediaStreamSource/TrackSource/Destination` 은 `NotSupportedError`. `createScriptProcessor`, `createMediaElementSource` 는 C2 가 구현하며 그때까지 `NotSupportedError`.

### 2.2 `AudioBuffer` 와 `getChannelData` (중심 설계)
문제: 엔진은 `float[]` 채널을 재생하고 스크립트는 `byte[]` 를 뒷받침 저장소로 쓰는 `Float32Array` 를 본다. `getChannelData(c)` 는 같은 배열 객체를 매번 돌려주고 그 내용이 버퍼의 내용이어야 한다.
해법(`AudioBufferValue`): **채널마다 기준 저장소가 하나**이다. 스크립트가 그 채널을 요청하기 전에는 `float[]`(디코딩 결과나 렌더링 결과를 재생만 하는 스크립트는 복사도 둘째 저장소도 필요 없다), 첫 `getChannelData(c)`/`copyToChannel(...c...)` 부터는 `byte[]` 이며 그 순간 `float[]` 를 버려 메모리를 두 배로 쓰지 않는다. `Float32Array` 는 채널마다 한 번 만들어 보관한다. 그 `ArrayBuffer` 를 스크립트가 다른 곳으로 옮겨(전달) 분리해도 버퍼는 깨지지 않는다(다음 호출이 같은 내용의 새 배열을 돌려준다).
**내용 가져오기(acquire)**: 노드는 `AudioBufferValue.acquire()` 로 엔진 `AudioBuffer` 스냅샷을 얻는다. 스크립트가 닿지 않은 채널은 공유하고(다시 쓰이지 않는다) 닿은 채널은 바이트에서 복사한다. 닿은 채널이 없으면 스냅샷을 기억해 여러 소스가 같은 버퍼를 재생해도 복사가 없다. 시점: 버퍼 원천은 `start()` 에서(이미 시작했다면 `buffer` 를 정할 때), 컨볼버는 `buffer` 를 정할 때. 그래서 재생을 시작한 뒤 스크립트가 배열을 바꿔도 그 소리에 영향이 없고, 같은 버퍼로 나중에 시작한 원천은 새 데이터를 재생한다(`AudioBufferScriptTest.testAcquireTheContent`).
REC 1.4.3 과의 **차이**: REC 는 가져오는 순간 앞서 `getChannelData()` 가 돌려준 배열을 분리(detach)하고 다음 호출이 새 배열을 준다고 하는데(Firefox 가 그렇게 한다), 이 구현은 분리하지 않는다(배열은 유효하고 쓸 수 있는 채로 남는다. Chromium 이 그렇게 한다). REC 를 따르는 스크립트는 배열의 동일성을 비교하지 않는 한 차이를 알 수 없고, Chromium 용으로 쓴 스크립트(분리된 배열에 계속 쓴다)도 동작한다. 기능 표 제약에 적어 달라고 요청한다.
엔진 쪽 보강: A1 의 `AudioBufferSourceNode.setBuffer` 는 한 번만 정하고 배열을 공유한다(A1 5.3-2). 스크립트의 "한 번만" 규칙(`[[buffer set]]`)은 스크립트 쪽 `NodeValue.bufferSet` 이 지키고, 엔진에는 **여러 번 부를 수 있는 `acquireBuffer(AudioBuffer)` 한 메서드**를 더했다(A1 파일 수정은 이 한 군데뿐). 시험 `RealtimeAudioContextTest.testAcquireBuffer`.

### 2.3 실시간 `AudioContext` 와 시간의 진행 (C2 가 바꿔 끼우는 지점)
- `RealtimeAudioContext`(엔진)는 스레드를 갖지 않는다. 상태(`suspended`/`running`/`closed`)와 `Driver` 를 가진다. `Driver.start(Renderer)`/`pause()`/`close()`; `Renderer` 는 `renderQuantum()`, 현재 프레임, 표본율, quantum 크기, 목적지 채널 수, 목적지의 마지막 quantum 오디오(`getOutputBus()`)를 준다. **C2 는 `ClockRenderDriver` 대신 `SourceDataLine` 에 쓰는 드라이버를 같은 `Driver` 로 구현해 `ContextApi.createRealtime` 의 `new ClockRenderDriver(...)` 한 줄만 바꾸면 된다.** 장치 지연은 `RealtimeAudioContext.reportOutputLatency` 로 알린다.
- **시간 진행 방식(장치 없음)**: `ClockRenderDriver` 가 데몬 스레드(`swingwebpanel-audio-render-N`)에서 **벽시계의 속도로 그래프를 렌더링하고 출력은 버린다**. `currentTime` 이 실시간으로 흐르고, 원천이 제때 시작/종료하고(`ended`), 분석기와(C2 의) 스크립트 프로세서가 신호를 본다. 렌더링은 벽시계가 만든 프레임보다 한 quantum 앞서고, 밀리면 연속 최대 64 quantum 을 따라잡는다. 일시 중단하면 시계의 기준을 다시 잡아 재개할 때 시간이 튀지 않는다.
- 상태 규칙: 새 컨텍스트는 **자동 재생 제한이 없다**(결정 e 확인: 호스트가 Swing 응용이므로 막을 이유가 없다) — 만들면 엔진이 바로 돌고, 보이는 `state` 는 첫 작업에서 `running` 이 되며 `statechange` 가 한 번 발생한다. 제어 호출(`resume`/`suspend`/`close`)은 엔진에 즉시 요청하고 페이지 작업을 쌓는다. 그 작업이 **보이는 상태를 그 순간 엔진의 상태로 맞추고, 바뀌었으면 `statechange` 를 먼저 발생시킨 뒤 프라미스를 resolve** 한다(규격의 "queue a media element task"; `testSuspendResumeClose`). 닫은 컨텍스트의 모든 제어 호출은 `InvalidStateError` 로 거부(close 는 호출 즉시 목록에서 빠져 한도를 돌려준다). 이미 그 상태인 호출은 이벤트 없이 resolve.
- 기본 표본율 48000(물어볼 장치가 없다. C2 가 장치 표본율을 알면 바꾼다), `latencyHint` 는 `interactive` 10 ms / `balanced` 20 ms / `playback` 100 ms 이고 숫자는 그 범위로 제한한 뒤 quantum(128)의 배수로 올림한 것이 `baseLatency`(최소 2 quantum), `outputLatency` 는 0. `getOutputTimestamp()` 는 첫 quantum 전에 0, 그 뒤 `{contextTime = currentTime - 지연, performanceTime = performance.now()}`.
- `sinkId`: 항상 `""`, `setSinkId("")` 와 `setSinkId({type:"none"})` 만 받고(`sinkId` 가 `{type:"none"}` 객체가 되며 바뀔 때 `sinkchange`) 다른 장치는 `NotSupportedError` 로 거부(결정 e). `{type:"none"}` 이 실제로 출력을 끄는 일은 C2 가 `ContextValue.sinkNone` 으로 한다.

### 2.4 컨텍스트 수와 메모리 한도 (`WebAudioHostLimits`)
- 실시간 컨텍스트: 페이지당 열려 있는(`close()` 하지 않은) 것 **6 개**(`NotSupportedError`). 근거: 실시간 컨텍스트는 렌더링 스레드와(C2 부터) 오디오 라인을 가지는 귀한 자원이고, 컨텍스트 수를 제한했던 브라우저(Chromium 의 하드웨어 컨텍스트)도 6 을 택했으며, 소리마다 컨텍스트를 만들고 닫지 않는 페이지의 누수를 스레드가 늘어나는 대신 개발자가 보는 오류로 바꾼다. 규격이 구현이 정하는 최대 수를 허용한다(1.1.7).
- 오프라인 컨텍스트: 렌더링을 끝내지 않은 것 16 개, 그리고 **엔진이 만들 때 결과 전체를 할당하므로** 합계 길이x채널이 2^25 표본(`WebAudioLimits.MAX_BUFFER_SAMPLES`, 134 MB)을 넘지 않게 예산을 둔다. 렌더링이 끝나거나 취소되면 몫을 돌려준다. **스크립트가 버린 컨텍스트도 렌더링이 끝나거나 페이지가 끝날 때까지 몫을 쥔다**(GC 의 시점에 기대지 않는다. 한계로 적는다).
- 엔진의 노드/버퍼 한도(`WebAudioLimits`)는 엔진이 그대로 적용하고(활성 노드 20000 등) 디코딩 대기 요청 64 개/256 MB 는 `AudioDecodeService` 의 한도이며 초과하면 `decodeAudioData` 의 프라미스가 `NotSupportedError` 로 거부된다.

### 2.5 `decodeAudioData`
`ContextValue` 마다 `AudioDecodeService` 하나(처음 부를 때 만든다). 스크립트 단계: 호출 자체의 실패(받는 이가 컨텍스트가 아님, 인수 누락/형식, 콜백이 함수가 아님)는 **reject 된 프라미스**(WebIDL 의 프라미스 반환 연산 규칙; `TypeError`), 닫힌 컨텍스트는 `InvalidStateError`, 이미 분리된 `ArrayBuffer` 는 `DataCloneError`(오류 콜백도 자기 작업에서 부른다), 그 밖은 `ArrayBuffer` 를 분리하고 바이트를 서비스에 넘긴다(복사 없음). 서비스가 다른 스레드에서 디코딩하고 컨텍스트 표본율로 리샘플링한 결과를 `Executor`(= `api.postTask`)를 통해 **페이지 작업으로** 전달하고, 그 작업에서 프라미스를 resolve 하고 성공 콜백을 부른다(성공 콜백이 프라미스 반응보다 먼저: 반응은 작업 뒤 마이크로태스크). 실패는 `EncodingError`(`DOMException`)로 reject 하고 오류 콜백에 같은 객체를 준다. 콜백이 던진 예외는 페이지에 보고하며 프라미스를 방해하지 않는다. 컨텍스트를 `close()` 하거나 페이지가 끝나면 서비스를 닫는다(대기 요청은 콜백 없이 취소).
**잘리거나 손상된 파일(후속 175, B2 병합 뒤)**: 서비스는 디코딩한 만큼의 버퍼로 `onSuccess` 를 부르며, 파일이 약속한 길이보다 짧거나 디코더가 손상을 알렸으면 그 직전에 같은 페이지 작업에서 `DecodeWarningCallback.onWarning(String)` 을 부른다. 바인딩은 이 경고를 **페이지 콘솔에 `warn` 수준으로** 보고한다(문구 `decodeAudioData: ` + 디코더의 경고, 예: `...cut off: 9860 of the 16000 frames that the file announces were decoded`; 컴포넌트에서는 `ConsoleEvent` 의 `Level.WARN`, 문서 URL 은 페이지, 리스너는 EDT). 1.1.2 가 권고하는 "손상됐지만 디코딩을 계속했을 때 개발자에게 경고" 이다. 프라미스와 성공 콜백은 평소처럼 resolve/호출된다(부분 결과도 성공). 프레임이 하나도 없으면(머리 안에서 자른 파일, 데이터 청크가 없는 파일) 경고 없이 `EncodingError` 로 reject 한다.

### 2.6 `OfflineAudioContext`
생성자 두 형식(사전: `numberOfChannels` 기본 1, `length`/`sampleRate` 필수; 인수 셋). `startRendering()` 은 스레드 `swingwebpanel-audio-offline-N`(데몬, 우선순위 보통-1)을 시작하고, 스레드는 quantum 을 연달아 렌더링하다가 일시 중단 지점(`suspend(time)` 의 quantum 올림 프레임)에서 페이지에 알리고 `resume()` 을 기다린다. 그 동안 스크립트가 그래프를 바꾼다(제어 메시지는 다음 quantum 시작에 적용되므로 새 값은 중단한 quantum 의 첫 프레임부터 들린다: `testSuspendAndChangeTheGraph`). 상태: `suspended` → (`startRendering` 호출 즉시) `running` → (일시 중단 때) `suspended` → `closed`(끝). 끝나면 페이지 작업 하나에서 `closed` 로 바꾸고 `statechange` 를 발생시키고 프라미스를 resolve 하며, **별도 작업**에서 `complete`(`OfflineAudioCompletionEvent`, `renderedBuffer` 는 프라미스가 준 객체)를 발생시킨다. 순서는 `statechange(running)`, `statechange(closed)`, 프라미스 반응, `complete`, `oncomplete`(`testStatesAndOrder`). `suspend` 오류: 양자화한 프레임이 음수이거나 현재 이전/같음이거나 길이 이상이거나 이미 예약됨 → `InvalidStateError`(reject). 렌더링 스레드는 스크립트를 부르지 않고 `postTask` 로만 알린다. 페이지가 끝나면 `cancel()`(플래그 + 인터럽트)으로 아무에게도 알리지 않고 끝낸다.

### 2.7 스레드와 정리(P21 연결)
- 스크립트 realm 은 페이지 스레드의 것이다. 엔진 스레드(실시간 렌더링, 오프라인 렌더링, 디코딩)가 만든 모든 것(`ended`, `statechange`, 완료, 디코딩 결과)은 `WebAudioApi.postTask` → 컴포넌트의 `Scheduler` → `PageScripts.runTask` 로만 스크립트에 닿는다(`testResultsAreTasksOfThePage`, `testEndedIsDeliveredByATask`: 페이지가 작업을 실행하지 않는 동안에는 오지 않는다). 폐기 뒤에 도착한 것은 버려진다.
- 정리: 페이지가 끝나거나 스크립트가 꺼지면 `WindowGlobals.cancelRequests()` → `WebAudioApi.dispose()`(어느 스레드에서나, 페이지 잠금 없이): 열린 실시간 컨텍스트를 닫고(렌더링 스레드 종료), 오프라인 렌더링을 취소하고, 디코딩 서비스를 닫고, 이후의 전달을 막는다. 시험으로 확인: 렌더링/오프라인/디코딩 스레드가 폐기 뒤 끝남, 20 페이지를 만들고 폐기해도 스레드가 남지 않음, 진행 중 오프라인 렌더링과 대기 중 디코딩이 폐기로 취소되고 이후 결과가 오지 않음, 스크립트를 끈 페이지는 아무것도 만들지 않음(`WebAudioLifecycleTest`).
- 정적 가변 상태 없음(스레드 번호는 페이지의 `WebAudioApi` 인스턴스 필드), 스레드 이름 접두사 `swingwebpanel-audio-render-`/`-offline-`/`-decode-`, 데몬.

## 3. 검증 (JDK 6u45, 이 PC, 실제로 실행한 것)

| 명령 | 결과 |
| --- | --- |
| `sh scripts/pkgtest.sh wac "js media/webaudio" "js/host/webaudio" --filter org.duckdns.hjow.swingwebpanel.js.host.webaudio` | 페이지 시험 49건 통과/0 실패(약 13 초). 컴포넌트 시험을 더하기 전의 값 |
| `sh build.sh` | OK. **엔진/브라우저 모듈 경계를 확인**한다: 브라우저 소스(`src`)가 컴파일된 엔진 클래스에만 의존해 컴파일된다(`js.host.webaudio` 와 `media.webaudio` 는 브라우저 모듈) |
| 시험 컴파일(`build/classes` 기준, `-sourcepath tests/src:engine/tests/src`) 뒤 `TestRunner --scan ... --filter org.duckdns.hjow.swingwebpanel.js.host.webaudio` | **55 통과 / 0 실패 / 0 건너뜀**(13 초; 마지막에 `testUnhandledRejectionIsReported` 를 더해 수명 시험 클래스만 다시 돌려 8건 통과): `WebAudioInterfaceTest` 7, `OfflineRenderingTest` 10, `OfflineContextTest` 6, `AudioBufferScriptTest` 5, `DecodeAudioDataTest` 6, `RealtimeContextScriptTest` 8, `WebAudioLifecycleTest` 8, `WebAudioComponentTest` 5 |
| 같은 방식으로 `--filter org.duckdns.hjow.swingwebpanel.media.webaudio` | **281 통과 / 0 실패 / 0 건너뜀**(42 초): A1 121 + A2 92 + B 61 + `RealtimeAudioContextTest` 7(이번에 추가) |
| `StaticStateAuditTest`, `ThreadCreationAuditTest`(`core.`), `JarContentTest`(`testing.selftest`), `-Dswp.jar=build/swingwebpanel.jar` 등 | 3건 통과. `ThreadCreationAuditTest` 가 `ClockRenderDriver` 에 스레드 이름 접두사 리터럴이 없다고 한 번 잡아 고쳤다(`THREAD_NAME_PREFIX`) |
| `sh tools/sourcecheck/run.sh src engine/src tests` / `docs tools` | **0 위반**(2708개 / 158개 파일: 인코딩, 금지 API, 영/한 Javadoc, 반복문 한국어 주석) |
| 손 변이 시험(5가지, 아래) | 모두 해당 시험이 실패했고 원래 코드로 되돌렸다 |
| 회귀(`WindowGlobals` 3 곳을 고쳤으므로 그 영향 범위만; 전체 `test.sh` 는 조정자가 돌린다) | `--filter org.duckdns.hjow.swingwebpanel.js.host.` **512 통과 / 0 실패 / 0 건너뜀**(40 초), `core.PageRelease` 1, `core.LifecycleAudit` 4, `core.PageRuntime` 13, `core.StaticState` 1, `core.ThreadCreation` 1, `testing.selftest` 4, `SwingWebPanelTest` 11 모두 통과. 전체 회귀는 돌리지 않았다 |

**시험의 성격**: 기대값은 시험이 닫힌 식이나 독립 계산으로 만든다. 사인 `0.5 sin(2 pi 441 n / 44100)`(오차 1e-5 미만), 선형 램프 `n / 2205`와 목표 접근/값 곡선의 식, 정확한 프레임 수만큼 지연된 임펄스(2000 프레임), 재생 속도 2 에서 `2n`, 스플리터/머저의 채널 배치, 스테레오 패너의 `cos`/`sin` 이득, 쌍이차 저역 통과의 차단 주파수 이득 `10^(Q/20)` 과 그 필터를 거친 사인의 정상 상태 진폭, 손으로 적은 합성곱 `[0.5, 0.25, -0.125]`, 웨이브셰이퍼 곡선, LFO 가 파라미터를 변조한 값 `0.5 + 0.25 sin(...)`, 일시 중단 뒤 바꾼 이득이 정확히 다음 quantum 의 첫 프레임(896, 1664)부터 들림, WAV 파일(스크립트가 만든 PCM16)을 디코딩한 값이 `정수/32768`(오차 1e-9 미만), 8000 Hz 파일을 16000 Hz 컨텍스트에서 디코딩한 사인(오차 5e-4 미만). 컴포넌트 시험은 실제 HTTP 서버가 준 WAV 를 페이지가 `fetch` → `decodeAudioData` → 게인 → `OfflineAudioContext` 로 렌더링해 표본이 `0.25 sin(...)` 와 같음을 확인한다. 실시간 시험은 벽시계 속도를 여유 있는 범위로만 확인한다(4절).
**손 변이 시험**(한 군데를 바꿔 시험이 실패하는지 확인하고 되돌림): (1) `AudioBufferValue` 가 낡은 스냅샷을 돌려줌 → `testAcquireTheContent` 실패(두 번째 소스가 새 데이터를 못 들음), (2) `decodeAudioData` 가 `ArrayBuffer` 를 분리하지 않음 → `testPromiseForm` 실패, (3) 오프라인 일시 중단이 상태를 `suspended` 로 바꾸지 않음 → `testAnalyserDuringSuspension` 실패, (4) 페이지 폐기가 오프라인 렌더링을 취소하지 않음 → `testDisposeCancelsAnOfflineRendering` 실패(스레드가 5 초 뒤에도 살아 있음), (5) 속성에 대입한 잘못된 열거형 이름이 `TypeError` 가 됨 → `testWebIdlConversions` 실패.
**시험이 잡은 구현의 문제**: 처음 돌린 시험에서 구현이 틀렸던 곳은 없고 시험의 기대값/한도 계산이 틀렸던 곳을 고쳤다(표본율 최솟값 3000, 합성곱의 부동소수점 잡음 1e-9, `getFrequencyResponse` 가 나이퀴스트 위 주파수에 `NaN`). 구현을 바꾸게 한 것은 감사 시험이 찾은 스레드 이름 한 곳과 `AudioContext` 를 7 개 만드는 시험 자신의 한도 위반뿐이다.

## 4. 확인하지 못한 것과 한계

확인하지 못한 것:
- **실제 소리 출력**(C2)과 실제 스피커 청취. 이 단계의 실시간 컨텍스트는 장치를 쓰지 않는다.
- JRE 6u45 이외의 JRE(JDK 7/8/25 는 이 PC 에 있으나 돌리지 않았다), 다른 OS/다른 코어 수에서의 실시간 시계의 정확도. 실시간 시험은 여유를 크게 둔 범위(벽시계의 0.7~1.2 배 + 50 ms)로만 확인했다.
- 실제 `SwingWebPanel` 창에 올린 상태(화면 표시)와 사람의 입력 연결(`AudioContext` 를 사용자 제스처로 시작하는 페이지). 컴포넌트 시험은 오프스크린 컴포넌트와 실제 HTTP 서버를 쓴다.
- 오프라인 컨텍스트 하나가 2^25 표본(134 MB)인 최대 크기의 실제 렌더링(예산 시험은 길이 2^23 x 채널 4 의 컨텍스트를 만들기만 한다).
- 다른 브라우저와의 동작 비교(Chromium 의 `decodeAudioData` 결과 길이와 리샘플러, `suspend` 의 세부).

구현의 한계(기능 표 제약에 옮길 것):
1. **acquire 시 이전 `getChannelData()` 배열을 분리하지 않는다**(2.2절, REC 1.4.3 과 다름. Chromium 과 같다). 스크립트가 `getChannelData` 의 `ArrayBuffer` 를 다른 곳으로 전달(transfer)하면 그 배열은 분리되지만 버퍼는 깨지지 않는다(다음 호출이 새 배열을 준다).
2. 버퍼를 한 채널이라도 `getChannelData()`/`copyToChannel()` 로 건드리면 acquire 가 그 채널을 매번 복사한다(소스 노드를 시작할 때마다 O(길이)). 건드리지 않은 버퍼는 복사가 없다. 건드리지 않은 채널과 건드린 채널을 섞은 버퍼는 건드린 채널만 복사한다.
3. 컨텍스트는 페이지가 끝나거나 `close()`/렌더링 완료까지 목록에 남는다(GC 의 시점에 기대지 않는다). 실시간 컨텍스트는 6 개, 오프라인은 16 개/메모리 2^25 표본이다. 닫지 않고 버린 실시간 컨텍스트는 자기 스레드를 페이지가 끝날 때까지 쥔다. 오프라인 컨텍스트는 만들 때 결과 전체를 할당한다(엔진 A1 설계).
4. 기본 표본율 48000 고정(장치를 묻지 않는다). `outputLatency` 는 0. `getOutputTimestamp().performanceTime` 은 호출 시점의 `performance.now()` 이다(실제 출력 시점의 추정이 아니다).
5. `sinkId` 는 기본 장치만(결정 e). `{type:"none"}` 은 상태만 바꾸고 이 단계에서 달라지는 소리는 없다(원래 소리가 없다). 1.1 작업 초안의 `onerror`, `AudioPlaybackStats`, `AudioContextOptions.renderSizeHint` 는 구현하지 않았다. `renderQuantumSize` 속성은 있다(값 128).
6. `decodeAudioData` 의 채널/정밀도 한계는 **B2 병합으로 해소되었다**(후속 175 에서 JS 경로로 확인, 7절): 채널은 파일의 수(최대 32, 33 은 `EncodingError`)이고 24비트 정수와 float 는 정확한 값으로 디코딩되며(1 을 넘는 float 도 깎지 않음), 잘린/손상된 파일은 디코딩한 만큼 성공하고 경고가 콘솔에 닿는다. **남은 한계(B2 의 정책이 그대로 JS 에 보인다)**: 경고는 디코더가 잘림을 알아볼 때만 온다 — 머리가 길이를 알리는 FLAC 네이티브, 그리고 손상을 감지하는 Vorbis/Ogg FLAC/FLAC 의 손상. 잘린 WAV, 잘린 Ogg Vorbis/Opus, MP3, ADTS AAC 는 짧은 정상 파일과 구별되지 않아 **경고 없이 더 짧게** resolve 한다(7.3절의 표). Opus 의 float 정밀도는 libopus 와 약 0.3 LSB 다르다(B2 10.5절). 채널 순서는 B2 10.4절의 결정(WAVEFORMATEXTENSIBLE 마스크 순서)을 따른다.
7. `OfflineAudioContext.suspend(time)` 은 quantum 경계로 올림한다(규격). 같은 quantum 안의 두 번째 예약은 `InvalidStateError`. `resume()` 이 일시 중단이 아닌 때(렌더링 중)에 불리면 바로 resolve 하고 아무것도 하지 않는다.
8. 스크립트가 `AudioListener` 의 파라미터를 노드 출력으로 구동하면 한 quantum 늦는다(엔진 A1 한계 5.3-4).
9. `PannerNode.panningModel = "HRTF"` 를 처음 정하면 페이지 스레드가 HRTF 자료를 읽는다(약 60 ms, A2 6.4절 8).
10. 컨트롤러 이벤트 `statechange` 의 `target`/`currentTarget` 은 컨텍스트, `ended` 의 것은 노드이다(규격 대로). 이벤트 객체는 버블링/취소 불가이다.
11. `MediaStream*` 인터페이스는 이름이 없고(EXC-14), `createMediaElementSource`/`createScriptProcessor`/`audioWorklet` 은 C2/D 에서 온다.

## 5. C2 를 위한 API 메모

- **실시간 컨텍스트를 바꿔 끼우는 지점**: `js/host/webaudio/ContextApi.createRealtime` 의 `new RealtimeAudioContext(sampleRate, latency, new ClockRenderDriver("swingwebpanel-audio-render-" + api.nextRenderSerial()))` 한 줄. C2 는 `media.webaudio` 에 `SourceDataLine` 드라이버(`RealtimeAudioContext.Driver` 구현)를 만들고, 장치를 못 열면 `ClockRenderDriver` 로 대신한다. 드라이버는 `Renderer.renderQuantum()` 을 부르고 `Renderer.getOutputBus()`(목적지 채널 수만큼의 채널, 마지막 quantum)를 장치 형식으로 바꿔 쓴다. 장치 표본율을 알면 `ContextApi.createRealtime` 의 기본값(`RealtimeAudioContext.DEFAULT_SAMPLE_RATE`)만 바꾸면 된다.
- 상태 규칙은 엔진 상태를 따라간다: `RealtimeAudioContext.resume()/suspend()/close()` 가 엔진 상태(`ContextState`)를 바꾸고 `ContextApi.syncState` 가 페이지 작업에서 보이는 상태와 `statechange` 를 맞춘다. 드라이버가 장치 실패 등으로 스스로 상태를 바꿔야 하면 `RealtimeAudioContext` 에 상태 변경 알림(리스너)을 더하고 `api.postTask(syncState)` 를 부르면 된다(지금은 스크립트의 제어 호출만 상태를 바꾼다).
- **페이지로 알리는 방법**: 렌더링 스레드에서 스크립트를 부르지 말고 `BaseAudioContext.TaskSink`(= `WebAudioApi.sink()` → `postTask`)를 쓴다. `ScriptProcessorNode` 의 `audioprocess` 는 `AudioProcessingEvent`(이미 정의, 생성자와 `playbackTime`/`inputBuffer`/`outputBuffer`) 를 `api.adopt(event, processingProto)`(`ContextApi.processingProto`, 지금은 private)로 프로토타입을 달아 `api.fire(nodeValue, event)` 로 전달하고, 핸들러가 채운 `outputBuffer`(`AudioBufferValue`)를 `acquire()` 로 가져가면 된다(1.4.3: "AudioProcessingEvent 의 dispatch 가 끝나면 outputBuffer 를 acquire"). `EventHandlers.define(proto, NodeValue.class, {"audioprocess"})` 로 `onaudioprocess` 를 만든다. 새 노드 형식은 `NodeApi.define(name, parent, engineClass, maker, 속성 명세, 연산 명세, 옵션 명세)` 한 번 호출이다(`NodeApi.installTypes` 참고).
- **`createMediaElementSource`**: `ContextApi.installRealtime` 에서 지금 `NotSupportedError` 를 던지는 메서드를 구현한다. `NodeApi.wrap(c, 엔진노드)` 로 `NodeValue` 를 만들고 `MediaElementAudioSourceNode` 인터페이스(`NodeApi.define`)와 `mediaElement` 속성을 더한다. `<video>` 는 `NotSupportedError`(결정 b). 요소를 이 컨텍스트가 닫힐 때(`WebAudioApi.dispose()`/`close()`) 푸는 일은 `ContextValue` 에 해제 목록을 더하면 된다.
- 정리: 새 자원(오디오 라인, 요소 연결)은 `WebAudioApi.dispose()`(어느 스레드에서나, 잠금 없이, 반복 가능)와 `ContextApi.control(…, 2)`(close)에서 닫는다. 새 스레드는 이름 접두사 `swingwebpanel-audio-` 와 `setDaemon(true)`(`ThreadCreationAuditTest`), 정적 가변 상태 금지(`StaticStateAuditTest`).
- 시험 도우미: `WebAudioPage`(`PageFixture` 래퍼: `run`, `number`, `errorOf`, `pump`, `waitFor`)와 `WebAudioComponentTest`(실제 `SwingWebPanel` + `TestHttpServer`)를 확장하면 된다.

## 6. 체크박스 후보 (체크는 조정자 몫)

- **P25-05**: 이 단계로 항목의 요구 중 **JavaScript 노출까지 끝났다**: `createBuffer`/`AudioBuffer` 생성자와 `getChannelData`/`copyFromChannel`/`copyToChannel`(1.4), 자체 디코더를 재사용하는 `decodeAudioData`(Promise 와 콜백 형식, `ArrayBuffer` 분리, `DataCloneError`/`EncodingError`, 컨텍스트 표본율로의 리샘플링, 문서 큐로 전달, 취소), acquire 규칙. B 단계가 남긴 "채널 3개 이상과 24비트 이상 정밀도"는 B2 가 병합되었고 후속 175 가 JS 경로(`DecodeDamagedDataTest`)로 확인했으므로(5.1 은 6채널, 24비트 FLAC 7.1 은 8채널과 24비트 정밀도, 합성 24비트/float WAV 는 정확한 값, 32채널까지, 잘린/손상 파일의 경고) **체크 가능**하다고 본다. 판단은 조정자에게 맡긴다.
- **P25-07**: **미완료로 유지**(C2): 실시간 출력(`SourceDataLine`), `latencyHint` 의 버퍼 크기, 장치 지연, 출력 장치/`sinkId`, 여러 컨텍스트의 라인 공유, `<audio>` 연결, 이동/clean/dispose 때 오디오 라인 정리는 C2. 이 단계에서 끝난 것: `OfflineAudioContext`(`startRendering`/`complete`/`suspend`/`resume`), 컨텍스트 수/버퍼 한도, 상태 전이와 `statechange`, 이동/clean/dispose/스크립트 끔 때의 컨텍스트·렌더링 스레드·디코딩 정리(P21 연결).
- **P25-02**: C1 로 "렌더링은 별도 오디오 스레드에서"(실시간 `AudioContext` 의 스레드 `swingwebpanel-audio-render-N` 와 오프라인 스레드 `swingwebpanel-audio-offline-N`)가 충족되었다(시계 드라이버). 장치로 내보내는 것은 C2 의 P25-07 이다. P25-02 를 체크할지는 조정자의 판단이다.
- P25-06(AudioWorklet), P25-08: 시작하지 않음.

## 7. 후속 175: B2 병합 뒤의 `decodeAudioData` 보강 (잘린/손상 파일의 경고, 다채널과 정밀도)

조정자가 이 브랜치를 B2/A2 가 병합된 `main`(2d69db1) 위로 rebase 했다(HEAD e14f0b8). B2 가 `AudioBufferDecoder.decodeResult`/`DecodeResult`/`DecodeWarningCallback` 과 넓은 디코딩 경로(원래 채널, float 정밀도)를 더했으므로 `js/host/webaudio` 쪽을 따라 맞췄다. 고친 제품 파일은 `js/host/webaudio/ContextApi.java` 한 파일이다(익명 `DecodeCallback` → `DecodeWarningCallback`, `onWarning` 이 `realm.getConsole().message("warn", "decodeAudioData: " + 경고)`).

### 7.1 시험 (`tests/src/org/duckdns/hjow/swingwebpanel/js/host/webaudio/DecodeDamagedDataTest`, 9건)

입력은 `tests/resources/media/audio`(실제 인코더가 만든 파일, 바이트는 16진수 문자열로 페이지에 넘겨 `decodeAudioData`)와 스크립트가 표본 단위로 만든 확장형 WAV 이다. 공통 도우미 `decodeAndCompare` 는 같은 바이트를 엔진의 `AudioBufferDecoder.decodeResult` 로도 디코딩해 (a) 프라미스가 resolve 되고 (b) 성공 콜백이 불리고 (c) 길이/채널 수/채널별 체크섬이 엔진 결과와 같고 (d) **콘솔 경고가 있는 것은 엔진이 잘림으로 표시한 결과일 때와 같고 문구가 엔진의 것과 같음**을 확인한다.

| 시험 | 확인하는 것 |
| --- | --- |
| `testCutFlacResolvesWithAWarning` | 70 % 에서 자른 FLAC(9860/16000 프레임): resolve, 길이가 줄어듦, 콘솔에 `warn` 한 건이 **성공 콜백의 `log` 보다 먼저**, 전체 파일은 경고 없음 |
| `testCutFilesOfEveryFormatResolveShorter` | 6 형식(Vorbis, Opus, FLAC, Ogg FLAC, MP3, AAC)을 70 % 에서 자른 사본이 전부 전체보다 짧게 resolve, 경고는 엔진이 잘림으로 표시한 FLAC 한 건뿐 |
| `testDamagedBlockInTheMiddle` | 가운데 300 바이트를 0 으로 만든 6 사본: 엔진 결과와 같음, Vorbis(83520/88200)는 경고가 있음 |
| `testCutWavResolves` | 데이터 청크가 선언보다 짧은 WAV: resolve, 약 절반의 프레임 |
| `testNoFrameIsAnEncodingError` | 머리 안(40 바이트)에서 자른 6 사본, 머리만 있는 WAV, 0 으로만 된 500 바이트: 전부 프라미스와 오류 콜백이 `EncodingError`(`DOMException`), 성공 콜백 없음, 경고 없음 |
| `testFiveOneFiles` | 5.1 WAV 는 6채널 500프레임이고 스크립트가 파일의 데이터 청크에서 읽은 표본과 **비트 단위로 같음**. 5.1 FLAC 은 채널마다 모노 참조 디코딩과 같음(차이 0). Opus 5.1(2880프레임), Vorbis 5.1(2048프레임), AAC 6채널(3072프레임)은 채널 수·프레임 수가 인코더가 쓴 것이고 채널마다 자기 참조 채널과 최대 차이 2e-4 이하 |
| `testTwentyFourBitFlacKeepsPrecision` | 24비트 7.1 FLAC: 8채널 300프레임, 16비트 참조에서 반 단계(16비트) 이내이며 16비트 간격의 배수가 아닌 표본이 100개를 넘음(16비트 경로였다면 반올림되어 없을 값) |
| `testExactValuesOfSyntheticFiles` | 스크립트가 만든 24비트 6채널 WAV(극단값 8388607/-8388608 포함)는 `정수/2^23` 과 정확히 같음. 32비트 float 6채널은 1 을 넘는 값까지 그대로(`!==` 비교). 32채널 WAV 는 디코딩되고 값이 채널 자리에 있음, 33채널은 `EncodingError` |
| `testCutFlacWarnsThroughTheComponent` | 실제 `SwingWebPanel` + `TestHttpServer`: 페이지가 잘린 FLAC 을 `fetch` 해 디코딩하고, 컴포넌트의 `ConsoleListener` 가 **EDT 에서** `Level.WARN` 이벤트(문구 `decodeAudioData: ` + 엔진의 경고, 문서 URL 은 페이지)를 받은 뒤 성공 콜백의 `LOG` 이벤트를 받음 |

### 7.2 검증 (JDK 6u45, 실제로 실행한 것)

| 명령 | 결과 |
| --- | --- |
| `sh build.sh` | OK(제품 JAR 세 개, 엔진/브라우저 경계 통과) |
| 시험 컴파일(`-sourcepath tests/src:engine/tests/src`) 뒤 `TestRunner --scan build/wa-test --filter org.duckdns.hjow.swingwebpanel.js.host.webaudio.DecodeDamagedDataTest` | **9 통과 / 0 실패 / 0 건너뜀**(약 8 초) |
| 같은 방식으로 `--filter ...js.host.webaudio`(기존 55건 + 새 9건) | **64 통과 / 0 실패 / 0 건너뜀**(20 초) |
| `--filter ...media.webaudio`(엔진, rebase 뒤 B2/A2 의 시험 포함) | **286 통과 / 0 실패 / 0 건너뜀**(49 초) |
| `StaticStateAuditTest`, `ThreadCreationAuditTest`, `JarContentTest` | 3건 통과 |
| `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/js/host/webaudio tests/src/org/duckdns/hjow/swingwebpanel/js/host/webaudio` | 0 위반(26개 파일) |
| **손 변이 시험**: `onWarning` 이 콘솔에 아무것도 쓰지 않게 바꾼 `ContextApi` 를 따로 컴파일해 클래스패스 맨 앞에 두고 새 클래스 실행 | 9건 중 **4건 실패**(`testCutFlacResolvesWithAWarning`, `testCutFilesOfEveryFormatResolveShorter`, `testDamagedBlockInTheMiddle`, `testCutFlacWarnsThroughTheComponent`: "the warning and the line of the success callback ... expected 2 but was 1"). 나머지 5건(경고와 무관한 다채널/정밀도/오류)은 통과 |

전체 회귀(`test.sh`)는 실행하지 않았다.

### 7.3 발견한 것: 어떤 사본에서 경고가 오는가 (엔진 `decodeResult` 를 직접 찍은 값, JS 시험이 이와 같음을 확인)

| 파일(전체 프레임) | 70 % 에서 자름 | 가운데 300 바이트 손상 |
| --- | --- | --- |
| Vorbis (88200) | 50752 프레임, 경고 없음 | 83520 프레임, **경고**(`cut off: 83520 of the 88200`) |
| Opus (96000) | 66888, 경고 없음 | 91848, 경고 없음(손상을 가림) |
| FLAC 네이티브 (16000) | 9860, **경고**(`cut off: 9860 of the 16000`) | 5950, **경고**(`damaged after 5950 frames ... negative FLAC LPC shift`) |
| Ogg FLAC (16000) | 11050, 경고 없음 | 15150, **경고** |
| MP3 (88200) | 60552, 경고 없음 | 87048, 경고 없음 |
| AAC (90112) | 64512, 경고 없음 | 89088, 경고 없음 |
| WAV (1500) | 절반으로 자르면 740 프레임, 경고 없음 | |

즉 조정자가 요청한 "(c) 경고가 콘솔 이벤트로 도착"은 **디코더가 잘림이나 손상을 알아보는 경우**(FLAC 의 잘림과 손상, Vorbis/Ogg FLAC 의 손상)에 성립한다. 잘린 WAV 와 잘린 Ogg Vorbis/Opus/MP3/AAC 는 B2 의 정책상 "짧은 정상 파일"로 보여 resolve 만 하고 경고가 없다(B2 10.6절이 같은 표를 적었다). 이를 바꾸려면 디코더 쪽(B2 영역: 예를 들어 WAV 데이터 청크 선언 크기와 실제 크기의 차이를 잘림으로 표시)에서 해야 하며 이 후속의 범위가 아니다.

### 7.4 해소된 한계와 남은 것

해소: 4절 6번(채널/정밀도). 남은 것: 7.3 의 경고 범위, Opus 의 정밀도(B2 10.5), 채널 33 이상은 `EncodingError`, 실제 브라우저와의 비교는 하지 않았다. 시험 중 알게 된 페이지 한도: 렌더링하지 않은 오프라인 컨텍스트는 페이지당 16 개이므로 디코딩 시험은 표본율마다 컨텍스트 하나를 재사용한다(2.4절).

## 조정자 요청

1. **FEATURE_MATRIX 행 초안**(WAU-01/07/08 갱신 + 새 행). WAU-02~06c 의 "엔진 검증 완료(JS 노출 C)" 는 "검증 완료(JS 경로 포함, C1)" 로 올려도 된다고 본다(각 노드의 JavaScript 경로 시험: `WebAudioInterfaceTest`, `OfflineRenderingTest`):

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-08a | JavaScript 바인딩: 전역 인터페이스 27개(`BaseAudioContext`, `AudioContext`, `OfflineAudioContext`, `AudioNode`, `AudioScheduledSourceNode`, `AudioDestinationNode` 와 노드 15종, `AudioParam`, `AudioBuffer`, `AudioListener`, `PeriodicWave`, `OfflineAudioCompletionEvent`, `AudioProcessingEvent`), 생성자와 `createX` 팩터리, 옵션 사전, `connect`/`disconnect` 전 형식, WebIDL 변환과 오류 이름, `webkitAudioContext` 별칭 | Web Audio API 1.0 REC 1.1~1.32 | `js.host.webaudio.{WebAudioApi,Members,Idl,ContextApi,NodeApi,ValueApi,BufferApi}` | `WebAudioInterfaceTest` 7, `OfflineRenderingTest` 10 | 검증 완료(`MediaElementAudioSourceNode`, `ScriptProcessorNode`, `AudioWorklet` 제외) | `MediaStream*` 미노출(EXC-14), `createMediaElementSource`/`createScriptProcessor` 는 C2 까지 `NotSupportedError`, `audioWorklet` 없음 |
| WAU-08b | `OfflineAudioContext`: 생성자 두 형식, `startRendering`(전용 스레드), `suspend(time)`/`resume`(그래프 변경), `complete`/`statechange` 순서, `length`/`oncomplete`, 페이지 한도 | 1.3 | `js.host.webaudio.{ContextApi,OfflineRender,WebAudioHostLimits}` | `OfflineContextTest` 6, `OfflineRenderingTest`, `WebAudioComponentTest` | 검증 완료 | 컨텍스트 16개/메모리 2^25 표본(최대 134 MB), 버린 컨텍스트도 렌더링 종료/페이지 종료까지 몫 유지 |
| WAU-08c | 실시간 `AudioContext` 의 JS 쪽: 상태/`statechange`, `resume`/`suspend`/`close`, 시간, 지연, `getOutputTimestamp`, `sinkId`/`setSinkId`/`sinkchange`, 6개 한도, 시계 드라이버(소리 없이 벽시계 속도로 렌더링) | 1.2, 1.1 WD | `media.webaudio.{RealtimeAudioContext,ClockRenderDriver}`, `js.host.webaudio.ContextApi` | `RealtimeAudioContextTest` 7, `RealtimeContextScriptTest` 8, `WebAudioComponentTest` | 부분 구현(장치 출력은 C2) | 자동 재생 제한 없음(결정 e), 표본율 48000 고정, `outputLatency` 0, `sinkId` 는 기본 장치 또는 none |
| WAU-07 | `AudioBuffer`, `getChannelData`(같은 `Float32Array`, 내용이 버퍼의 내용), `copyFrom/ToChannel`, acquire 규칙, `decodeAudioData`(프라미스/콜백, 분리, `DataCloneError`/`EncodingError`, 리샘플링, 문서 큐 전달) | 1.4, 1.1.2 | `js.host.webaudio.{AudioBufferValue,BufferApi,ContextApi}` | `AudioBufferScriptTest` 5, `DecodeAudioDataTest` 6, `DecodeDamagedDataTest` 9, `WebAudioComponentTest` | 검증 완료(JS 경로 포함. 채널 최대 32, 24비트/float 정밀도, 잘린/손상 파일은 디코딩한 만큼 resolve 하고 콘솔에 `warn`) | acquire 때 이전 배열을 분리하지 않음(Chromium 과 같고 REC 1.4.3 과 다름). 경고는 디코더가 잘림을 알아볼 때만(FLAC 네이티브의 잘림, Vorbis/Ogg FLAC/FLAC 의 손상): 잘린 WAV/Ogg Vorbis/Opus/MP3/AAC 는 경고 없이 짧게 resolve |
| WAU-10 | 수명과 정리: 페이지 이동/`clean()`/`dispose()`/스크립트 끔 때 컨텍스트, 렌더링 스레드, 디코딩 서비스를 닫고 늦은 결과를 버림 | P21 | `js.host.webaudio.WebAudioApi.dispose`, `js.host.WindowGlobals` | `WebAudioLifecycleTest` 8, `WebAudioComponentTest` 5 | 검증 완료(오디오 라인은 C2) | 스레드 이름 `swingwebpanel-audio-{render,offline,decode}-N` |

2. **ARCHITECTURE.md**: 패키지 표에 `js.host.webaudio`(Web Audio 의 JavaScript 바인딩)를 더하고, 스레드 모델에 "실시간 컨텍스트: `RealtimeAudioContext` + `ClockRenderDriver`(스레드 `swingwebpanel-audio-render-N`, 벽시계 속도, 출력 버림. C2 가 장치 드라이버로 대체)", "오프라인 렌더링: `OfflineRender`(스레드 `swingwebpanel-audio-offline-N`)", "엔진 스레드가 알리는 것은 `WebAudioApi.postTask` 로만 페이지의 작업이 된다" 를 적고, "자원 소유 목록과 정리 담당(P21-01)" 표에 한 행을 더해 달라: `WebAudioApi`(실시간/오프라인 컨텍스트, 렌더링 스레드, 디코딩 서비스) ← 정리 담당 `WindowGlobals.cancelRequests()` → `WebAudioApi.dispose()`.
3. **AGENTS.md 체크박스**: 6절 참고(P25-05 는 B2 뒤 체크 가능, P25-07 은 C2 뒤, P25-02 는 조정자 판단).
4. **README(루트) 사용 설명**: `AudioContext` 가 소리 없이 시계만 도는 단계임(C2 가 장치를 더한다)을 쓸 때 적어 달라. 이 단계에서 README 사용법을 바꿀 새 공개 Java API 는 없다(스크립트 API 만 늘었다).
5. **`docs/progress/media-play.md` "미확인" 표**: C2 와 별개로, 이 단계의 실시간 컨텍스트는 소리를 내지 않으므로 청취 항목은 없다. C2 가 추가할 항목은 그때 요청한다.
6. **`scripts/**`**: 변경 요청 없음. 다만 `scripts/pkgtest.sh` 로 JavaScript 호스트 시험을 돌릴 때는 소스 패키지에 `js`(엔진 포함)를 써야 한다(`"js/host/webaudio media/webaudio"` 만 쓰면 엔진의 내장 객체 일부가 컴파일되지 않아 `%Promise%` 가 비는 오류가 난다. `javac -sourcepath` 가 리플렉션으로만 쓰이는 클래스를 컴파일하지 않기 때문이다): `sh scripts/pkgtest.sh wac "js media/webaudio" "js/host/webaudio" --filter org.duckdns.hjow.swingwebpanel.js.host.webaudio`.
7. **B2 병합 뒤의 보강: 후속 175 로 완료**(7절): `ContextApi.decodeAudioData` 가 `DecodeWarningCallback` 을 써서 부분 결과의 경고를 페이지 콘솔(`warn`)로 보고한다. 이 요청은 더 필요 없다.
8. **다른 작업자와의 조율**: 이 작업이 건드린 기존 파일은 `js/host/WindowGlobals.java`(3곳)와 `media/webaudio/AudioBufferSourceNode.java`(메서드 1개 추가)뿐이다. B2 는 `media/audio/**` 와 `media/webaudio/decode/AudioBufferDecoder.java`, `docs/progress/webaudio-b.md` 를 고치므로 겹치지 않는다. 전체 회귀(`test.sh`)는 실행하지 않았다.
