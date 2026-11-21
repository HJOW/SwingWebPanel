# WEBAUDIO-C2b 작업 기록 (P25-04, P25-07: 장치 출력 연결, ScriptProcessorNode 바인딩, `<audio>` 요소의 그래프 연결, 수명)

이 문서는 WEBAUDIO-C2b 작업자의 기록이다. 작업 번호 대역은 `20261122-330` ~ `359` 이다(이번에 330~335 를 썼다). 앞 단계의 기록: `docs/progress/webaudio.md`(A1/A2, 엔진), `docs/progress/webaudio-b.md`(디코딩),
`docs/progress/webaudio-c.md`(C1: JavaScript 바인딩), `docs/progress/webaudio-c2a.md`(C2a: 소리 출력 드라이버와 `ScriptProcessorNode` 엔진 노드; 이 브랜치는 C2a 의 완료 커밋 b39e558 위에서 시작했다).
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `docs/API_CONTRACT.md`, `scripts/**`, 루트 `README.md`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

## 0. 범위

C1 이 JavaScript 쪽 `AudioContext`(시계만 흐르는 `ClockRenderDriver`)를 만들고 C2a 가 장치로 재생하는 구동기(`OutputRenderDriver`)와 `ScriptProcessorNode` 엔진 노드를 만들었다. 이 단계는 그것들을 **연결**한다.

1. 실시간 `AudioContext` 를 장치 출력 구동기로 바꾸고 옵션(`latencyHint`, `sampleRate`, `sinkId`)·`setSinkId`·`getOutputTimestamp` 를 규격대로 연결, 호스트가 출력을 바꾸는 방법(`createWebAudioOutputFactory`).
2. `ScriptProcessorNode`(`createScriptProcessor`, `onaudioprocess`/`addEventListener('audioprocess')`, `AudioProcessingEvent`)의 JavaScript 바인딩.
3. `createMediaElementSource(audioElement)` 와 `MediaElementAudioSourceNode` 생성자: P18 의 `<audio>` 재생기의 소리를 그래프로 돌리는 어댑터 `MediaElementTap`.
4. 수명(P21): 페이지 이동/`clean()`/`dispose()`/`setJavascriptEnabled(false)` 와 닫히는 컨텍스트에서 렌더링 스레드·출력·재생기·탭의 정리, 닿을 수 없게 된 멈춘 컨텍스트가 한도를 영구히 쥐지 않게 하기.

하지 않은 것: AudioWorklet(D 단계), Maven(P23), 실제 소리 장치와 실제 Swing 창에서의 확인(이 PC 에는 Java Sound 믹서가 없다. 7절).

## 1. 구현 파일

제품(`src/org/duckdns/hjow/swingwebpanel/`):

| 파일 | 변경 |
| --- | --- |
| `media/webaudio/MediaElementTap.java` | **새 파일.** `<audio>` 재생기와 그래프를 잇는다(2절). 재생기의 `AudioOutputFactory` 이면서 노드의 `MediaElementPcmSource` |
| `media/webaudio/OutputRenderDriver.java` | `restart()` 메서드 하나 추가(출력 전환, 3.3절) |
| `media/playback/MediaPlayer.java` | `reopenOutput()` 추가와 재생 루프의 `performReopen()`(2.4절). 기존 16비트 재생 경로는 그대로 |
| `js/host/MediaElements.java` | 요소별 출력 팩터리 `ElementOutputs`, `Media.tap`, `connectToGraph`, `mayReadAudio`(2.5절) |
| `js/host/PageScripts.java` | 공개 메서드 `connectMediaElement(Element, float)` 하나 |
| `js/host/HostEnvironment.java`, `core/PageRuntime.java`, `core/PageView.java` | `getWebAudioOutputFactory()` / `setWebAudioOutputFactory` 배선 |
| `SwingWebPanel.java` | `protected OutputFactory createWebAudioOutputFactory()` 추가(3.1절)와 첫 사용 때 한 번 묻는 위임 팩터리 |
| `js/host/webaudio/ContextApi.java` | `createRealtime`(출력 구동기, 옵션), `setSinkId`, `getOutputTimestamp`, `createScriptProcessor` 와 `ScriptProcessing`, `createMediaElementSource`, 닫을 때 탭 놓기 |
| `js/host/webaudio/NodeApi.java` | `ScriptProcessorNode`·`MediaElementAudioSourceNode` 인터페이스, 컨텍스트가 필요한 노드를 만드는 `ContextMaker`, 노드의 스크립트 객체 약한 추적 |
| `js/host/webaudio/{ContextValue,NodeValue,AudioBufferValue,Idl,WebAudioApi}.java` | 출력 구동기 접근, 탭 목록, 포기 판정(`isAbandoned`), 블록 버퍼를 엔진으로 되가져가는 `storeInto`/`detach`, 한도에서의 포기(`register`) |
| 문서 보정 | `AudioProcessingEvent`, `package-info` 의 낡은 설명 |

시험(`tests/src/org/duckdns/hjow/swingwebpanel/`): `media/webaudio/MediaElementTapTest`(10), `media/webaudio/OutputRenderDriverSwitchTest`(3), `js/host/webaudio/WebAudioOutputScriptTest`(11), `js/host/webaudio/MediaElementSourceScriptTest`(10),
`js/host/webaudio/WebAudioOutputComponentTest`(5, 실제 `SwingWebPanel` + `TestHttpServer`), 도우미 `js/host/webaudio/PacedDevice`(시험 스위트 아님: 모의 소리 장치). 기존 파일 수정: `js/host/PageFixture`(Env 에 `webAudioOutputs`, 기본 무음 출력),
C1 시험 세 곳의 기대값(`RealtimeContextScriptTest` 2건과 `WebAudioInterfaceTest.testExcludedNames`: 아래 6절).

## 2. 설계: `<audio>` 요소를 그래프로 (가장 큰 부분)

### 2.1 읽은 것

- P18 `MediaPlayer`: 재생 스레드가 2048 프레임 블록을 디코딩해 **`AudioOutput`(16비트 LE 인터리브 PCM, 비블록 `write`, `getQueuedFrames`, `start/stop/flush/close`)에 쓴다. 위치 = 쓴 프레임 − 출력 큐의 프레임**이므로 시계는 출력이 소비하는 속도이다. 음량/음소거는 재생기가 16비트 표본에 고정 소수점으로 미리 곱한다.
- A2 `MediaElementAudioSourceNode` + `MediaElementPcmSource`: 렌더링 스레드가 quantum 마다 `pull(float[][], frames)` 로 PCM 을 당겨 간다.

### 2.2 해법: 재생기의 출력 자리에 파이프를 꽂는다

`MediaPlayer` 를 거의 건드리지 않는 방법으로 **요소의 출력을 "그래프가 비우는 링 버퍼"로 바꾼다**. `MediaElementTap`(재생기의 `AudioOutputFactory` 이자 노드의 `MediaElementPcmSource`)이 열어 주는 `Pipe` 는 `AudioOutput` 이고, 렌더링 스레드가 `pull` 로 링에서 값을 읽는다.
그러면 규격과 브라우저가 요구하는 것이 구조에서 저절로 나온다.

- **요소의 시계가 그래프이다**: 위치(`currentTime`), `timeupdate`, `ended` 가 그래프가 소비한 프레임을 따른다(브리프의 지시 "재생 위치는 그래프가 당겨 간 프레임 수"). 컨텍스트가 `suspended` 이면 아무도 파이프를 비우지 않으므로 요소가 멈춘다(`paused` 는 false 로 남는다). 요소가 일시정지되면 재생기가 파이프를 `stop()` 하므로 `pull` 이 0 을 준다.
- **음량과 음소거**: 재생기가 샘플에 곱하므로 `volume`/`muted` 가 그래프로 들어가는 오디오에 적용된다. **Chromium 과 같다**: `third_party/blink/renderer/platform/media/web_audio_source_provider_impl.cc`(`WebAudioSourceProviderImpl::ProvideInput` 의 `bus_wrapper_->Scale(volume_)`, `SetVolume`; Gitiles `refs/heads/main`, 조회 2026-11-22, 본문 12537 바이트)가 요소의 유효 음량(음소거면 0)을 노드가 주는 오디오에 곱한다.
  같은 파일의 `ProvideInput` 은 **오염된(tainted) 오리진이면 `Zero()`**(2.5절), 클라이언트(그래프)가 끊어지면 무음, 그리고 `SetClient` 의 주석("It's not possible to resume an element after disconnection ... the only way to disconnect a client is internally at time of destruction")으로 요소가 노드에 영구히 묶이는 것을 확인했다(2.6절).
- **표본율**: 미디어와 컨텍스트의 표본율이 다르면 B 단계 `Resampler`(스트리밍, 채널마다 인스턴스)로 변환한다(`MediaElementAudioSourceNode` 자체의 선형 보간은 쓰지 않도록 탭이 컨텍스트 표본율을 보고한다). 시험이 **탭의 결과가 `Resampler.resample()` 가 신호 전체를 한꺼번에 변환한 결과와 비트 단위로 같음**을 확인한다(첫 프레임부터. 44100→48000, 48000→44100, 8000→12000/6000, 모노/스테레오). 길이가 정확하다(`ceil(frames*out/in)`).
  필터가 원본을 약 65 프레임 앞서 보므로 **요소의 위치가 그래프가 전달한 오디오보다 약 1.5 ms 앞선다**(링에서는 quantum 이 필요한 만큼만 가져간다). 끝에서는 재생기가 파이프를 닫을 때 변환기의 꼬리를 한 번 내보내므로 **미디어의 마지막 약 1.5 ms 가 나머지보다 몇 ms 늦게 도착한다**(그 사이는 무음. 한계 8절).
- **링 크기**: `max(4096 프레임, 0.15 초)`. 재생기는 그만큼 앞서 디코딩하고 가득 차면 4 ms 간격으로 기다린다(P18 의 기존 동작).
- **잠금**: 링 접근은 몇천 표본을 복사하는 동안만 쥐는 짧은 잠금이다(재생 스레드는 디코딩/대기 중에는 쥐지 않으므로 A2 의 "블록하지 않는 `pull`" 계약을 지킨다).

### 2.3 `MediaElementPcmSource` 계약과의 차이

A2 의 노드는 `isOutputAllowed()` 가 false 이면 `pull` 하지 않는다. 그러면 파이프가 비워지지 않아 **페이지가 읽을 수 없는 미디어의 요소가 멈춰 버린다**(브라우저는 멈추지 않는다). 그래서 탭의 `isOutputAllowed()` 는 언제나 true 이고 **탭이 스스로 무음을 전달한다**(같은 속도로 비우되 0 을 준다). 호스트는 `setOutputAllowed(boolean)` 으로 허용 여부를 알린다.

### 2.4 이미 출력을 연 재생기: `MediaPlayer.reopenOutput()`

스크립트가 요소를 **재생 도중이나 일시정지 중에** 연결할 수 있으므로 재생기가 스피커용 출력을 이미 쥐고 있을 수 있다(일시정지된 요소도 출력을 열어 둔다). 새 공개 메서드 `reopenOutput()` 은 재생 스레드에게 "열린 출력을 놓고 다음 쓰기 때 팩터리로 새 출력을 열라"고 요청한다.
재생 스레드(`performReopen`)는 **지금 들리는 프레임**(쓴 프레임 − 이전 출력의 큐)에서 디코더를 다시 맞추고 이전 출력을 닫는다: 이전 출력의 큐에 있던 소리는 새 출력을 위해 다시 디코딩하므로 건너뛰거나 반복하는 프레임이 없다. 리스너에는 아무것도 알리지 않는다(탐색이 아니다: `seeked` 가 없다).
`MediaElements` 의 요소별 팩터리(`ElementOutputs`)가 **재생기가 출력을 열 때마다** `Media.tap` 을 보고 탭의 파이프 또는 컴포넌트의 출력을 고른다. 시험: 엔진(`MediaElementTapTest.testReopenSwitchesFromTheSpeakersToThePipeWithoutAGap`: 재생 중/일시정지 중, 스피커가 재생한 프레임 + 파이프의 프레임이 파일 전체와 같음)과 JavaScript(`testElementThatPlaysToTheSpeakersIsSwitchedToTheGraph`).
**알려진 경쟁**: 노드는 만들어진 순간부터(`pin`) 렌더링 스레드가 quantum 마다 요소에서 가져가므로, 이미 재생 중인 요소에 대해 스크립트가 `createMediaElementSource` 를 부르고 `destination` 에 연결하기 전의 quantum(많아야 한두 개, 128 프레임 단위)은 사라진다(브라우저도 같다). 시험이 이 손실을 허용 범위(≤ 512 프레임, 128 의 배수)로 기록한다.

### 2.5 교차 오리진

`MediaElements.mayReadAudio`: 리소스의 `currentSrc` 가 **요소의 문서와 같은 오리진**일 때만 그래프에서 들린다(`net.Origin`, `classpath://` 끼리는 같은 오리진). 다른 오리진이면 `MediaElementTap.setOutputAllowed(false)` 로 **무음이 전달되고 요소는 멈추지 않는다**(시험 `testMediaOfAnotherOriginIsSilentInTheGraph`). 새 리소스를 로드할 때마다 다시 정한다.
**`crossorigin` 속성이 있는 요소의 CORS 요청은 구현하지 않았다**: P18 의 미디어 요청(`PageResources.requestMedia`)은 CORS 모드가 없어(`media-play.md` 한계 5 의 "`crossOrigin` 의 CORS 효과 없음") 서버가 허가했는지 알 수 없다. 그래서 `crossorigin="anonymous"` 로 다른 오리진을 읽고 서버가 허가한 미디어도 **그래프에서는 무음**이다(규격은 들려야 한다). 구현하려면 `RequestPurpose` 에 `AUDIO_CORS` 를 더하고(`HttpFetcher.acceptFor`, `OriginPolicy.requiresCors`), `MediaRequestSource` 에 CORS 모드를 받는 요청을 더하고, `PageResources.requestMedia` 가 `corsGranted` 로 응답을 검사해 결과를 `MediaElements`/탭으로 알려야 한다(파일 6개 안팎, P18 영역). 또한 같은 오리진 URL 이 **다른 오리진으로 리다이렉트**되어도 같은 오리진으로 취급한다(최종 URL 을 `MediaRequestSource.Callback` 이 주지 않는다).

### 2.6 컨텍스트가 닫힐 때

규격은 요소를 되돌리는 방법을 두지 않고 Chromium 도 되돌리지 않는다(2.2절). 그래서 **컨텍스트를 닫으면 요소는 영구히 자기 노드에 묶인 채 소리 없이 보통 속도로 끝까지 계속 재생된다**: `ContextValue.releaseTaps()` → `MediaElementTap.release()` 가 파이프를 **벽시계로 비우게** 한다(재생기가 링을 채우는 대로 `getQueuedFrames` 가 실시간으로 줄어듦. 별도 스레드 없음). 요소는 자기 직접 출력을 다시 열지 않는다(시험 `testClosedContextLetsTheElementPlayOnSilently`: 0.5 초 파일이 약 0.5 초 만에 `ended`, 스피커 출력 열기 0 회).

### 2.7 오류

`createMediaElementSource`/생성자: 같은 요소에 두 번째 노드(같은 컨텍스트든 다른 컨텍스트든, 첫 컨텍스트가 닫혔든)는 `InvalidStateError`(Chromium 의 "HTMLMediaElement already connected previously to a different MediaElementSourceNode"), `<video>` 는 요소를 건드리지 않고 `NotSupportedError`(결정 b), 미디어 요소가 아닌 것·`null`·인수 없음·옵션 없음·`mediaElement` 없음·잘못된 컨텍스트는 `TypeError`, `OfflineAudioContext` 에는 `createMediaElementSource` 가 없고 생성자에 오프라인 컨텍스트를 주면 `TypeError`, 닫힌 컨텍스트는 `InvalidStateError`, 다른 문서(프레임)의 요소는 `NotSupportedError`.

## 3. 설계: 출력, 옵션, `ScriptProcessorNode`, 수명

### 3.1 호스트가 출력을 바꾸는 방법 (브리프 2번의 결정)

**새 protected 메서드 `SwingWebPanel.createWebAudioOutputFactory()`**(반환 `media.webaudio.output.OutputFactory`, 기본 `new SystemOutputFactory()` = Java Sound 기본 믹서, `null` 이면 모든 컨텍스트가 소리 없이 돈다)로 정했다. `<audio>` 용 `createAudioOutputFactory()` 를 어댑터로 재사용하지 **않은** 이유:

- 두 출력은 성격이 다르다. Web Audio 의 `AudioOutput` 은 **부동 소수점 평면 오디오를 받고 `write` 가 장치에 자리가 생길 때까지 블록**하며(렌더링 속도를 장치에 맞추는 메커니즘), P18 의 것은 16비트 인터리브를 받고 **절대 블록하지 않는다**. 어댑터는 비블록 쓰기를 폴링으로 감싸고 float→16비트(정밀도 손실)·버퍼 크기 제어 포기를 감수해야 한다.
- 호스트가 `<audio>` 는 소리를 내고 Web Audio 는 끄거나(또는 그 반대) 독립적으로 고를 수 있다. 모든 소리를 돌리려는 호스트는 두 메서드를 모두 재정의한다(Javadoc 에 적었다).
- 경로: `SwingWebPanel` → `PageView.setWebAudioOutputFactory` → `PageRuntime.getWebAudioOutputFactory()` → `HostEnvironment.getWebAudioOutputFactory()`(공개 추상 클래스의 새 메서드, 기본 null = 시스템 출력) → `ContextApi.createRealtime` 이 `OutputRenderDriver.createContext(sampleRate, latency, factory, name)` 에 넘긴다.
  위임 팩터리는 첫 사용 때(첫 실시간 컨텍스트의 렌더링 스레드에서) 한 번만 재정의 메서드를 부르고, 팩터리가 null 이면 `IOException` 을 던져 드라이버가 소리 없는 출력으로 대신하게 한다. **장치가 없는 환경에서도 스크립트는 예외 없이 동작한다**(이 PC 에서 기본 `SwingWebPanel` 로 확인: `WebAudioOutputComponentTest.testDefaultOutputLetsAContextRunWithoutAScriptError`).
- 시험이 가짜 출력을 넣는 방법: 컴포넌트는 `createWebAudioOutputFactory()` 재정의, 페이지 시험 도구는 `PageFixture.Env.webAudioOutputs`(기본은 `OutputRenderDriver.silentOutputs()` 이므로 시험이 소리를 내거나 장치를 열지 않는다).

### 3.2 옵션과 속성

`new AudioContext({latencyHint, sampleRate, sinkId})`: `latencyHint` 는 `interactive`/`balanced`/`playback` 또는 초 단위 수(C1 의 `resolveLatency`: 10/20/100 ms, 범위로 제한), `sampleRate` 는 3000~768000(밖이면 `NotSupportedError`, 이제 장치 표본율과 무관하게 컨텍스트가 그 표본율로 돈다. 장치가 지원하지 않으면 `JavaSoundOutput` 이 변환하고 지연이 약 1.5 ms 더해진다), `sinkId` 는 `""`(호스트가 고른 출력) 또는 `{type:"none"}`(무음 출력, 팩터리에 묻지 않는다)이고 그 밖의 장치는 `NotSupportedError`.
`outputLatency` 는 드라이버가 쓸 때마다 보고하는 출력 큐의 시간(interactive 약 50 ms), `baseLatency` 는 C1 의 값. `getOutputTimestamp()` 는 드라이버의 `getOutputTimestamp()`(지금 들리는 컨텍스트 시간)와 `performance.now() − (nanoTime − 타임스탬프 시각)`(0 미만이면 0); 첫 quantum 전에는 0.

### 3.3 `setSinkId`와 `sinkchange`

`setSinkId(id)` 는 새 팩터리(`""` → 호스트의 것, `{type:"none"}` → `silentOutputs()`)를 드라이버에 알리고 **싱크가 바뀌었고 컨텍스트가 실행 중이면 `OutputRenderDriver.restart()`(새 메서드) 로 구간을 곧바로 새 출력으로 바꾼다**: 이전 출력은 놓이고(그 큐의 오디오, 많아야 버퍼 하나 분량인 50~80 ms 는 버려진다) 컨텍스트 시간은 렌더링한 프레임에서 이어진다. 멈춘 컨텍스트에서는 다음 `resume()` 이 새 팩터리를 쓴다. 그 뒤 페이지 작업에서 `sinkId` 를 바꾸고 `sinkchange` 를 발생시키므로 **이벤트는 전환이 적용된 뒤에 온다**(C2a 문서 9절이 걱정한 "다음 구간부터 적용되는데 이벤트가 먼저 오는" 문제가 없다). 같은 싱크로의 호출은 이벤트가 없다. 시험 `testSinkNoneAndBack`.

### 3.4 `ScriptProcessorNode`

`createScriptProcessor(bufferSize = 0, in = 2, out = 2)`: 잘못된 값은 `IndexSizeError`(엔진), `channelCount`/`channelCountMode` 변경은 `InvalidStateError`, 인터페이스는 생성자가 없다(`new ScriptProcessorNode()` 는 `TypeError`), `bufferSize` 읽기 전용, `onaudioprocess` 속성과 `addEventListener`. `AudioProcessingEvent` 의 `inputBuffer`/`outputBuffer` 는 **블록의 버퍼 위에 만든** `AudioBuffer` 이다:
스크립트가 `getChannelData` 로 닿은 채널만 자기 바이트를 얻고(C1 의 두 저장소 설계), 이벤트가 끝나면 **`storeInto`** 가 핸들러가 쓴 바이트를 엔진 블록의 `float[]` 로 되가져가고(`outputBuffer` 의 1.4.3 acquire) **`detach`** 가 두 버퍼의 남은 공유 배열을 복사본으로 바꾼다(엔진이 블록을 재활용해도 스크립트가 간직한 버퍼가 바뀌지 않는다. 시험이 확인).
`audioprocess` 리스너(속성 포함)가 없으면 이벤트도 버퍼도 만들지 않고 출력은 무음이다. 입력 채널이 0 이면 `inputBuffer` 는 **null** 이다(규격은 non-null `AudioBuffer`; 0 채널 `AudioBuffer` 는 만들 수 없다). 지연은 정확히 `2 * bufferSize`(C2a 3절: 시험이 임펄스가 프레임 100 → 612 로 나옴과 `playbackTime = (kB + 2B)/sampleRate` 를 닫힌 식으로 확인).
오프라인에서는 렌더링 스레드가 리스너를 기다린다(기본 최대 10 초): 페이지가 끝나면 `WebAudioApi.dispose()` 의 취소(인터럽트)가 대기를 끝낸다.

### 3.5 수명(P21)

- 페이지가 끝나면(이동/`clean()`/`dispose()`/스크립트 끄기: `WindowGlobals.cancelRequests()` → `WebAudioApi.dispose()`, 새 코드는 없다) 열린 컨텍스트가 닫혀 **렌더링 스레드가 끝나고 출력이 닫히며**, 요소는 `MediaElements.dispose()`/`stopSoundNow()` 로 재생기가 닫힌다. 실제 컴포넌트로 4 경로를 확인한다(`WebAudioOutputComponentTest.testPageEndsTheContextsThePlayersAndTheOutputs`: 렌더링 스레드 수 복귀, 재생 스레드 수 복귀, 출력 `isClosed`).
- `close()` 한 컨텍스트: 탭 놓기(2.6), 렌더링 스레드·출력 정리. 반복 생성·폐기(JS 30회 + 컴포넌트 12개)에서 스레드가 남지 않는다.
- **한도(6개)를 영구히 쥐는 문제의 개선**: 규격과 브라우저는 **실행 중인 컨텍스트는 닫힐 때까지 살려 둔다**(소리와 이벤트를 만들 수 있다). 그래서 `running` 은 계속 붙잡는다. 대신 **`suspended` 이면서 어떤 스크립트도 더는 닿을 수 없는 컨텍스트**는 새 컨텍스트가 자리가 필요할 때(`WebAudioApi.register` 가 한도에 닿았을 때) **포기하고 닫는다**(렌더링 스레드와 출력이 끝난다).
  "닿을 수 없음"의 판정: 컨텍스트의 스크립트 객체와 **그 컨텍스트의 모든 노드의 스크립트 객체**가 약한 참조로 모두 수거되었는가(노드의 `context` 속성이 컨텍스트에 이르는 길을 주므로 노드 객체도 확인해야 한다. `ContextValue.trackNodeWrapper`/`isUnreachable`). GC 시점에는 의존하지 않고 한도에 닿았을 때만 본다.
  시험 `testSuspendedContextsThatNobodyCanReachDoNotHoldTheLimit`(GC 를 반복 요청): 포기된 4개의 스레드가 끝나고 스크립트가 아직 닿는 멈춘 컨텍스트와 실행 중인 컨텍스트는 유지됨. **변이 시험**: `isAbandoned()` 가 항상 false 를 돌려주면 이 시험이 실패한다.
  한계: 실행 중인 컨텍스트를 닫지 않고 버리는 스크립트(소리마다 컨텍스트 하나)는 브라우저처럼 여전히 한도에 닿는다. 오프라인 컨텍스트와 `AudioParam`/`AudioListener` 객체는 추적하지 않는다.

## 4. 검증 (JDK 6u45, 이 PC, 실제로 실행한 것)

검증 환경: JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`), `-source 1.6 -target 1.6`. 이 PC 의 Java Sound 믹서는 0 개이며(`JavaSoundDeviceTest` 2건이 건너뜀), 따라서 **실제 장치 경로는 가짜 장치로만** 시험했다.
시험 장치: `ManualOutputFactory`(기록하는 가짜 출력) + `PacedDevice`(벽시계 속도로 소비하는 모의 장치. 소비하지 않는 가짜 출력은 컨텍스트를 CPU 속도로 달리게 하므로 필요했다) — 시험이 만든 값은 모두 닫힌 식이나 독립 계산(`Resampler.resample`, 파일의 16비트 표본/32768)이다.

| 명령(작업 트리 커밋 `20261122-334` 기준) | 결과 |
| --- | --- |
| `sh build.sh`(`BUILD_DIR=build/full`: 엔진 모듈을 먼저 컴파일하고 브라우저 모듈을 컴파일된 엔진 클래스에만 의존해 컴파일 + JAR 3개) | **성공**(`[OK] build/full/swingwebpanel.jar (engine + browser) ... created (javac 1.6.0_45)`) |
| 시험 컴파일: `tests/src` 전체(+ `engine/tests/src` 의 TestRunner/Check, `-sourcepath`)를 `build/full/classes` 에 대해 `-source 1.6 -target 1.6` 로 컴파일 | 성공 |
| `TestRunner --scan ... --filter org.duckdns.hjow.swingwebpanel.js.host.webaudio` | **90 통과 / 0 실패 / 0 건너뜀**(C1 의 기존 64건 + 새 26건) |
| `--filter ...media.webaudio`(엔진 전체: A1/A2/B/C1/C2a/새 `MediaElementTapTest` 10) | **341 통과 / 0 실패 / 0 건너뜀** |
| `--filter ...media.webaudio.OutputRenderDriverSwitchTest`(위 실행 뒤에 추가한 새 시험 3건, 따로 실행) | **3 통과 / 0 실패** |
| `--filter ...media.playback`(P18 재생기. 회귀) | **30 통과 / 0 실패 / 2 건너뜀**(이 PC 에 Java Sound 믹서가 없음: `JavaSoundDeviceTest` 2건) |
| `--filter ...js.host.`(`MediaElementsTest` 등 JavaScript 호스트 전체, 위 90건 포함) | **548 통과 / 0 실패 / 0 건너뜀** |
| `--filter ...core.Audio`(`AudioIntegrationTest`, `AudioControlsIntegrationTest`, `AudioRobustnessIntegrationTest`: 실제 컴포넌트 + HTTP + 가짜 출력의 P18 `<audio>` 시험. 회귀) | **12 통과 / 0 실패 / 0 건너뜀** |
| `--filter ...core.StaticState`, `...core.ThreadCreation`(정적 가변 상태 없음, 새 스레드는 데몬 + `swingwebpanel-` 이름) | 각 **1 통과** |
| `--filter ...testing.selftest`(제품 JAR 내용 `JarContentTest` 포함), `...SwingWebPanelTest`(11), `...core.PageRuntime`(13), `...core.Lifecycle`, `...core.PageRelease` | **4 / 11 / 13 / 11 / 1 통과, 0 실패** |
| 위 시험을 5~6 번 되풀이(`MediaElementSourceScriptTest` 를 연속 실행) | 모두 통과(처음에는 마지막 quantum 의 기록 경쟁으로 간헐 실패가 있어 `stable()` 도우미와 손실 허용 범위로 고침) |
| `sh tools/sourcecheck/run.sh <변경한 제품/시험 경로 175개 파일과 이 문서>` | **0 위반** |

시험이 확인하는 것(요약):

- `WebAudioOutputScriptTest`(11): 오실레이터→게인이 출력에 **`0.5 sin(2π·441·n/48000)` 의 닫힌 식**(오차 < 1e-4, 시작 전 정확히 무음, 두 채널 같음), `suspend()` 가 출력을 놓고 조용(쓴 프레임 불변, 시간 정지)·`resume()` 이 같은 시간에서 새 출력으로 이어 감·`close()` 가 스레드와 출력 정리·상태 이벤트 순서,
  `sampleRate`/`latencyHint` 옵션이 출력의 표본율과 버퍼(interactive 약 50 ms, playback > 0.3 s)와 `outputLatency` 에 반영, `getOutputTimestamp()`, `sinkId:{type:'none'}` 옵션과 `setSinkId`(none/기본)가 출력을 놓거나 새 출력을 열고 `sinkchange` 가 전환 뒤에 한 번,
  장치가 없을 때(`open` 이 `IOException`) 컨텍스트가 예외 없이 벽시계 속도로 돎, **`ScriptProcessorNode`**: 인터페이스와 오류 전부, 오프라인에서 **정확히 두 블록 지연**(프레임 100/300/1000 의 임펄스가 612/812/1512 에서 두 배 값으로 나옴)과 `playbackTime`, 간직한 출력 버퍼의 독립, 던지는 핸들러, 입력 채널 0 의 생성기(카운터 `1, 2, 3 ...` 이 512 프레임 뒤에서 틈 없이)와 핸들러 없는 노드의 무음, 실시간 컨텍스트에서 4096 프레임 블록의 카운터가 틈 없이,
  그리고 한도 개선 시험.
- `MediaElementSourceScriptTest`(10): 파일의 모든 표본이 **`16비트 표본/32768 × 게인` 과 정확히 같게 한 번씩 순서대로**, 요소 자신의 출력이 열리지 않음, 그래프가 시계(0.1875 초 파일이 컨텍스트 시간으로 0.1875~0.7 초), 일시정지/탐색/음량(고정 소수점 이득)/음소거/반복이 그래프를 거쳐 동작, 44100 Hz 파일이 `Resampler` 결과와 비트 단위로 같음,
  스피커로 재생하던 요소의 전환, 오류 종류(두 번째 연결, 다른 컨텍스트, 비디오, 아닌 것, 오프라인, 닫힌 컨텍스트, 생성자 형식), 다른 오리진 무음, 컨텍스트가 닫힌 뒤의 소리 없는 계속 재생, 30개 컨텍스트·페이지 폐기 뒤 스레드/출력 정리, 일시정지된 컨텍스트가 요소를 멈춤.
- `WebAudioOutputComponentTest`(5, 실제 `SwingWebPanel` + `TestHttpServer`): 진짜 HTTP 의 WAV 가 호스트의 출력에서 같은 값, 이동/`clean()`/스크립트 끄기/`dispose()` 4 경로에서 렌더링 스레드·재생 스레드·출력 정리, 두 컴포넌트가 각자의 출력으로, 12개 컴포넌트 폐기 뒤 남은 스레드 없음, 재정의 없는 기본 컴포넌트에서 스크립트 오류·콘솔 오류 없음.
- `MediaElementTapTest`(10)·`OutputRenderDriverSwitchTest`(3): 2절과 3.3절의 엔진 수준 확인(정확한 순서와 값, 위치=가져간 프레임, 일시정지/재개/탐색, 변환 길이와 비트 단위 동일, 읽기 금지 무음, 놓은 뒤 실시간 배출, 재생기 출력 다시 열기).

## 5. 변이 시험 (구현의 한 군데를 바꿔 시험이 실패하는지 확인하고 복구)

| 변이 | 실패한 시험 |
| --- | --- |
| `MediaElements.ElementOutputs` 가 탭을 무시 | `MediaElementSourceScriptTest` 9건 중 7건 |
| `mayReadAudio` 가 항상 true | `testMediaOfAnotherOriginIsSilentInTheGraph`(들리면 안 되는 6000 프레임이 들림) |
| `ScriptProcessing` 이 출력 버퍼를 엔진으로 되가져가지 않음(`storeInto` 제거) | 스크립트 프로세서 시험 3건 |
| `OutputRenderDriver.restart()` 가 아무것도 하지 않음 | `testSinkNoneAndBack`(이전 출력이 놓이지 않음) |
| `MediaElementTap.release()` 가 아무것도 하지 않음 | `testClosedContextLetsTheElementPlayOnSilently`(요소가 끝나지 않음) |
| `WebAudioApi.dispose()` 가 실시간 컨텍스트를 닫지 않음 | `WebAudioOutputComponentTest` 2건(렌더링 스레드가 남음) |
| `ContextValue.isAbandoned()` 가 항상 false | `testSuspendedContextsThatNobodyCanReachDoNotHoldTheLimit` |
| 처음 구현의 버그(시험이 잡음) | `MediaPlayer.performReopen` 이 초 단위 위치를 프레임으로 착각해 0 프레임으로 되감음 → `MediaElementTapTest` 의 재개 시험이 실패해 고침 |

## 6. 기존 시험의 기대값을 바꾼 곳 (의도한 동작 변경)

- `RealtimeContextScriptTest.testStateAndDefaults`: 장치가 없어도(무음 출력) `outputLatency` 가 0 이 아니라 출력 큐의 시간(약 0.05 초)이다. 기대를 `0 <= outputLatency < 0.2` 로 바꿨다.
- `RealtimeContextScriptTest.testOutputTimestamp`: `contextTime` 이 `currentTime` **보다 작음** 이 아니라 **이하이고 0.5 초 이내**(출력이 비어 있으면 같을 수 있다).
- `WebAudioInterfaceTest.testExcludedNames`: `ScriptProcessorNode`·`MediaElementAudioSourceNode` 가 이제 있다(`createScriptProcessor` 는 오류 없음, `createMediaElementSource(document.body)` 는 `TypeError`).
- `PageFixture.Env`: `webAudioOutputs`(기본 무음 출력)와 `getWebAudioOutputFactory()` 추가.

## 7. 확인하지 못한 것

- **실제 소리**: 이 PC 에는 Java Sound 믹서가 없다. 오실레이터/버퍼/`<audio>` 가 그래프를 거쳐 실제 스피커로 들리는지, 일시정지/`suspend()` 가 곧바로 조용한지, 끊김이 없는지, 변환된 소리의 품질, 요소가 그래프로 바뀌는 순간의 소리, 두 컨텍스트 동시 재생, 장치 사용 중일 때 무음 대체, `setSinkId({type:"none"})` 뒤의 무음: **모두 미확인**(`media-play.md` "미확인" 표에 항목 추가 요청: 조정자 요청 6).
- **실제 Swing 창**과 실제 마우스·키로 시작하는 페이지(사용자 제스처): 실제 `SwingWebPanel` 인스턴스(오프스크린)로만 확인했다.
- JRE 6 이외의 JRE, 다른 코어 수에서의 실시간 시계의 정확도(시험은 여유를 둔 범위만 확인).
- 전체 회귀(`test.sh`)는 돌리지 않았다(조정자 몫). 4절의 범위만 돌렸다.
- `crossorigin` 속성이 있는 교차 오리진 미디어(2.5절).

## 8. 한계

1. 일시정지(`suspend()`)와 출력 전환은 **렌더링 스레드가 그래프에서 가져왔지만 아직 장치에 넘기지 못한 오디오**(한 청크: interactive 약 3 ms, `playback` 약 46 ms)를 잃는다. 그래프에 연결된 요소에서는 그만큼의 미디어 프레임이 요소 위치는 지나갔지만 들리지 않는다(시험 `testSuspendedContextStallsTheElementAndResumeContinues` 가 ≤ 256 프레임 손실을 허용 범위로 기록).
2. 표본율을 변환할 때 **미디어의 마지막 약 1.5 ms 가 몇 ms 늦게** 도착하고 요소의 위치가 그래프가 전달한 것보다 약 1.5 ms 앞선다(2.2절).
3. 노드를 만든 뒤 `destination` 에 연결하기 전에 렌더링 스레드가 가져간 quantum 은 사라진다(2.4절, 브라우저와 같다).
4. 요소의 소리는 16비트(재생기의 PCM)이다. 그래프에는 `s/32768` 로 들어간다(그 이상의 정밀도는 P18 재생기가 16비트로 내보내므로 없다).
5. `crossorigin` CORS 미구현(2.5절), 리다이렉트 뒤의 오리진 미확인, 요소는 **같은 문서의** 요소여야 한다.
6. `ScriptProcessorNode` 의 `inputBuffer` 는 입력 채널이 0 이면 null 이다.
7. `setSinkId` 로 실행 중인 컨텍스트를 바꾸면 버퍼 하나(50~80 ms)의 오디오를 잃는다.
8. 실행 중인 컨텍스트를 닫지 않고 버리면 한도를 쥔다(3.5절).
9. 이미 재생 중인 요소를 연결하면 `reopenOutput` 으로 이전 출력의 큐(최대 약 150 ms)를 다시 디코딩한다(시간 비용만 있고 소리의 손실은 없다).

## 9. 체크박스 후보 (체크는 조정자 몫)

- **P25-04**: `MediaElementAudioSourceNode`(`<audio>` 연결, 교차 오리진 무음, `<video>` 거부)와 `ScriptProcessorNode`(결정 c)의 **JavaScript 노출과 자동 검증이 끝났다**. 남은 것: 소리를 귀로 확인(P22-10), `crossorigin` CORS(2.5절). 따라서 항목의 "나머지 노드" 요구는 충족하나 **청취 확인 뒤에 체크하는 것을 권장**한다.
- **P25-07**: 실시간 `AudioContext` 의 Java Sound 출력 연결(`createRealtime` + 호스트 팩터리), 옵션 `latencyHint`/`sampleRate`/`sinkId`, `setSinkId`(none 포함)와 `sinkchange`, `getOutputTimestamp`/`outputLatency`/`baseLatency`, 장치 없음에서의 상태와 시간, 여러 컨텍스트·여러 컴포넌트의 동시 출력(각자 출력), 이동/clean/dispose/스크립트 끄기 때의 렌더링 스레드·오디오 라인·재생기·탭 정리(P21), 한도와 포기 정책 — **자동 검증 범위에서 모두 끝났다**.
  남은 것: **실제 장치에서의 청취 확인**(사람, P22-10). 자동 검증과 실제 확인을 구분하기 위해 체크는 청취 확인 뒤를 권장한다.
- P25-02: 변경 없음(C1/C2a 로 충족).

## 조정자 요청

1. **FEATURE_MATRIX 행 초안**(WAU-06c, 08c, 11, 12 갱신과 새 행):

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-06c | `MediaElementAudioSourceNode`: `createMediaElementSource(audio)` / 생성자 `{mediaElement}`, `audio` 요소의 소리를 그래프로(요소 시계=그래프, 음량·음소거 적용), 표본율 변환(`Resampler`), 같은 오리진만 들림(다른 오리진은 무음이나 요소는 진행), 컨텍스트가 닫히면 영구히 묶인 채 소리 없이 계속 | 1.22 | `media.webaudio.{MediaElementAudioSourceNode,MediaElementTap}`, `media.playback.MediaPlayer.reopenOutput`, `js.host.{MediaElements,PageScripts}`, `js.host.webaudio.NodeApi` | `MediaElementTapTest` 10, `MediaElementSourceScriptTest` 10, `WebAudioOutputComponentTest` 5 | 검증 완료(자동. 청취 미확인) | `<video>`·미디어 요소가 아닌 것·같은 요소의 두 번째 노드·오프라인 컨텍스트·닫힌 컨텍스트·다른 문서의 요소는 오류. `crossorigin` CORS 미구현(그 경우 그래프에서 무음), 리다이렉트 뒤 오리진 미확인, 요소는 16비트, 표본율 변환 시 위치가 약 1.5 ms 앞서고 마지막 약 1.5 ms 가 몇 ms 늦음, 노드를 `destination` 에 연결하기 전의 quantum 은 사라짐 |
| WAU-08c(갱신) | 실시간 `AudioContext`: 장치 출력(호스트 팩터리), 옵션 `latencyHint`/`sampleRate`/`sinkId`, `setSinkId`(기본/none, 실행 중이면 즉시 적용)와 `sinkchange`, `outputLatency`/`getOutputTimestamp`, 장치 없음 | 1.2, 1.1 WD | `media.webaudio.{RealtimeAudioContext,OutputRenderDriver}`, `js.host.webaudio.ContextApi` | `WebAudioOutputScriptTest` 11, `OutputRenderDriverSwitchTest` 3, `OutputRenderDriverTest` 15, `RealtimeContextScriptTest` 8, `WebAudioOutputComponentTest` 5 | 검증 완료(자동. 청취 미확인) | 자동 재생 제한 없음(결정 e), `setSinkId` 로 실행 중인 컨텍스트를 바꾸면 버퍼 하나(50~80 ms)의 오디오를 잃음, 일시정지는 한 청크(3 ms~46 ms)를 잃음 |
| WAU-11(갱신) | `ScriptProcessorNode`(폐기 예정)/`AudioProcessingEvent`: `createScriptProcessor`, `onaudioprocess`/`addEventListener`, 정확히 `2*bufferSize` 지연, 핸들러가 쓴 출력 버퍼를 블록으로 | 1.0 REC 1.29, 1.12 | `media.webaudio.ScriptProcessorNode`, `js.host.webaudio.{ContextApi,NodeApi,AudioBufferValue}` | `ScriptProcessorNodeTest` 14, `WebAudioOutputScriptTest`(스크립트 프로세서 5건) | 검증 완료(자동) | 입력 채널이 0 이면 `inputBuffer` 는 null(규격은 non-null), 실시간에서 응답이 한 블록 안에 오지 않으면 그 블록은 무음 |
| WAU-12(갱신) | 소리 출력: Java Sound(`SourceDataLine`)와 호스트 팩터리 `SwingWebPanel.createWebAudioOutputFactory()`(null 이면 무음), 장치 없음/열기 실패는 무음 출력으로 시간만 진행, 구간마다 열고 닫음, 컨텍스트마다 출력 하나(두 컴포넌트·두 컨텍스트 각자) | P25-07 | `media.webaudio.output.*`, `media.webaudio.OutputRenderDriver`, `SwingWebPanel`, `core.{PageView,PageRuntime}`, `js.host.HostEnvironment` | `JavaSoundOutputTest` 10, `NullOutputTest` 6, `OutputRenderDriverTest` 15, `WebAudioOutputComponentTest` 5 | 부분 구현(가짜 장치로 검증, **실제 장치와 청취는 확인 못함**) | 6 채널 이상 장치 구조만, 장치 내부 지연은 알 수 없음 |
| WAU-10(갱신) | 수명: 이동/`clean()`/`dispose()`/스크립트 끄기 때 컨텍스트·렌더링 스레드·출력·재생기·탭 정리, **닿을 수 없는 멈춘 컨텍스트는 한도에서 풀림** | P21 | `js.host.webaudio.{WebAudioApi,ContextValue}`, `js.host.WindowGlobals` | `WebAudioLifecycleTest` 8, `WebAudioOutputComponentTest` 5, `WebAudioOutputScriptTest.testSuspendedContextsThatNobodyCanReachDoNotHoldTheLimit` | 검증 완료(오디오 라인은 가짜 출력으로) | 실행 중인 컨텍스트를 닫지 않고 버리면 한도를 쥠(브라우저와 같음) |

2. **API_CONTRACT.md**(새 절 5.21 "확장 메서드: Web Audio 출력(P25-07)", AGENTS.md 의 공개 메서드 목록은 그대로): `protected org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory SwingWebPanel.createWebAudioOutputFactory()` —
   실시간 `AudioContext` 의 출력을 여는 팩터리를 만든다. 기본 `new SystemOutputFactory()`(Java Sound 기본 믹서. 장치가 없거나 열 수 없으면 컨텍스트는 소리 없이 실제 속도로 돌고 스크립트 예외는 없다). `null` 이면 모든 컨텍스트가 소리 없이 돈다. 첫 실시간 컨텍스트가 돌기 시작할 때 **한 번** 호출한다(그 컨텍스트의 렌더링 스레드에서).
   팩터리의 `open(sampleRate, channels, bufferFrames)` 는 컨텍스트의 **구간(`resume()` 에서 다음 `suspend()` 까지)마다** 그 컨텍스트의 렌더링 스레드에서 불리며 `IOException`/`RuntimeException` 은 "장치 없음"으로 처리된다. 반환하는 `AudioOutput.write` 는 장치에 자리가 생길 때까지 블록해도 되고 `stop()` 이 불리면 곧바로 돌아와야 한다.
   `<audio>` 요소의 `createAudioOutputFactory()` 와는 **독립**이다(모든 소리를 돌리려면 둘 다 재정의). 공개 변화: `js.host.HostEnvironment.getWebAudioOutputFactory()`(공개 추상 클래스의 새 메서드, 기본 null), `js.host.PageScripts.connectMediaElement(Element, float)`, `media.playback.MediaPlayer.reopenOutput()`, `media.webaudio.MediaElementTap`, `media.webaudio.OutputRenderDriver.restart()`.
   시험 행 후보: `C-WEBAUDIO-OUT-1~8`(출력 연결, 옵션, sink, 장치 없음, 스크립트 프로세서, 요소 연결, 수명, 한도).
3. **README(루트) 사용 설명**: "`AudioContext` 는 Java Sound 기본 믹서로 소리를 내고, 장치가 없으면 소리 없이 시간만 흐른다(예외 없음). 호스트는 `createWebAudioOutputFactory()` 를 재정의해 소리를 다른 곳으로 보내거나(`<audio>` 요소는 `createAudioOutputFactory()` 가 따로 있다) `null` 을 반환해 끈다.
   `audio` 요소를 `createMediaElementSource` 로 연결하면 요소의 소리가 그래프를 거쳐 컨텍스트의 출력으로 나간다(같은 오리진만. 비디오는 지원하지 않음). `ScriptProcessorNode` 는 폐기 예정 API 의 호환 구현(지연 2 블록)."
4. **ARCHITECTURE.md**: ① 패키지 의존: `media.webaudio` → `media.playback`(탭이 `AudioOutput`/`AudioOutputFactory` 를 구현. 반대 방향은 없음). ② "자원 소유 목록과 정리 담당(P21-01)" 표: `WebAudioApi` 행에 "오디오 출력 라인(`OutputRenderDriver` 가 구간마다 열고 닫음), 요소 탭(`ContextValue.releaseTaps`), 한도에서 포기되는 컨텍스트" 를 더하고,
   `MediaElements` 행에 "요소별 출력 팩터리: `Media.tap` 이 있으면 그래프의 파이프, 없으면 컴포넌트의 출력" 을 더한다. ③ 스레드 모델: **새 스레드 없음**(재생 스레드 `swingwebpanel-audio` 와 렌더링 스레드 `swingwebpanel-audio-render-N` 이 파이프를 통해 만난다. 파이프의 링은 짧은 잠금으로 보호).
5. **`docs/progress/media-play.md` "미확인" 표**: 다음을 사람이 소리 장치가 있는 PC 에서 확인하도록 더해 주세요: ① 오실레이터/`AudioBufferSourceNode` 소리가 실제로 들리는지, `suspend()` 가 곧바로 조용한지, 끊김 없는지, `latencyHint` 3종의 체감 지연,
   ② `<audio>` + `createMediaElementSource` + 게인으로 음량이 실제로 바뀌는지와 요소의 일시정지/탐색/반복이 소리에 맞는지, 재생 중인 요소를 연결하는 순간의 소리, 44100 Hz 파일이 48000 Hz 컨텍스트에서 변환된 소리의 품질, ③ 두 컨텍스트·두 컴포넌트 동시 재생과 한쪽 `dispose()`, ④ 장치 사용 중일 때 무음 대체, `setSinkId({type:"none"})` 뒤의 무음, ⑤ 실제 Swing 창에서 사용자 제스처로 시작하는 페이지.
6. **`scripts/pkgtest.sh` 메모**: 컴포넌트 시험(`WebAudioOutputComponentTest` 등)을 `pkgtest.sh` 로 돌리려면 소스 패키지에 `js media core @root dom html css layout render net storage content download` 가 모두 필요하다(`"js media"` 만으로는 컴포넌트 클래스가 컴파일되지 않는다). 저는 `build.sh` 뒤 증분 컴파일을 썼다.
7. **AGENTS.md 체크박스**: 9절 참고(P25-04, P25-07 은 청취 확인 뒤 권장).
8. **다른 작업자와의 조율**: 이 작업이 건드린 기존 파일은 `SwingWebPanel.java`(메서드 하나와 위임 팩터리 한 블록), `core/{PageView,PageRuntime}.java`(접근자), `js/host/{HostEnvironment,MediaElements,PageScripts}.java`, `media/playback/MediaPlayer.java`(`reopenOutput`), `media/webaudio/OutputRenderDriver.java`(`restart`), `tests/.../js/host/PageFixture.java`(Env 필드 하나)와 C1 시험 세 곳이다.
   모두 추가 위주이며 P18 의 기존 시험(`media.playback` 30, `core.Audio` 12, `js.host.` 548)이 그대로 통과한다.
9. **C1 요청 중 미뤄진 것**: C1 문서 2.1 의 "`createMediaElementSource`/`createScriptProcessor` 는 C2 가 구현" 은 이 작업으로 끝났다(WAU-08a 제약의 "C2 까지 NotSupportedError" 를 지워도 된다).
