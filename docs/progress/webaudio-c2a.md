# WEBAUDIO-C2a 작업 기록 (P25-07: 소리 출력 드라이버와 ScriptProcessorNode 엔진 노드)

이 문서는 WEBAUDIO-C2a 작업자의 기록이다. 작업 번호 대역은 `20261122-300` ~ `329` 이다(이번에 300~303 을 썼다). 앞 단계의 기록: `docs/progress/webaudio.md`(A1/A2, 엔진), `docs/progress/webaudio-b.md`(디코딩), C1 의 기록 `docs/progress/webaudio-c.md`(C1 브랜치 `worktree-agent-a481f49edd6577c57` 에 있다).
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**`, 루트 `README.md`, `docs/progress/media-play.md`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

## 0. 범위와 바뀐 지시

처음 지시(순수 Java `RealtimeAudioContext` 를 새로 만들기)를 받고 만들던 중, 조정자가 **C1 이 이미 `RealtimeAudioContext`(교체 가능한 `Driver`/`Renderer` 인터페이스)와 `ClockRenderDriver` 를 만들었다**고 알려 와서 내 `RealtimeAudioContext` 와 `LatencyHint` 를 **버렸다**(어디에도 commit 하지 않았다). 지시대로 C1 의 소스(`git show worktree-agent-a481f49edd6577c57:...`)를 이 worktree 에 **임시로 가져와** 컴파일하며 작업했고 그 두 파일은 commit 에 넣지 않았다(작업 끝에 지웠다, 7절). 이 단계가 만든 것:

1. `media/webaudio/output/**`: 소리 출력 추상 `AudioOutput`, 구현 `JavaSoundOutput`(Java Sound `SourceDataLine`)과 `NullOutput`(장치 없음, 벽시계 속도), 팩터리 `OutputFactory`/`SystemOutputFactory`.
2. `media/webaudio/OutputRenderDriver`: C1 의 `RealtimeAudioContext.Driver` 를 **장치로 재생하도록 구현**한 드라이버(렌더링 스레드 `swingwebpanel-audio-render-N`, 끊김 정책, `latencyHint` 별 버퍼, 장치 쪽 지연 보고, 장치 없음 대체).
3. `media/webaudio/ScriptProcessorNode`(순수 Java 엔진 노드, 결정 c)와 `BaseAudioContext.createScriptProcessor`.
4. 시험 `OutputRenderDriverTest`, `ScriptProcessorNodeTest`, `output/{NullOutputTest,JavaSoundOutputTest}` 와 시험 도우미(`ManualOutput`, `ManualOutputFactory`, `FakeLine`, `FakeLines`).

C1 이 이미 한 일(상태 전이, `suspend/resume/close` 의 규칙과 오류, `currentTime`, 6 개 한도, `statechange`)은 다시 만들지 않았고 시험도 겹치지 않게 **장치 드라이버 쪽 동작**(PCM 정확성, 끊김, 지연, 스레드 수명, 장치 없음, 멈춘 장치)에 집중했다.

## 1. 구현 파일

`src/org/duckdns/hjow/swingwebpanel/media/webaudio/` (모두 새 파일, 수정한 기존 파일은 `BaseAudioContext` 하나):

| 파일 | 역할 |
| --- | --- |
| `output/AudioOutput` | 열린 소리 출력 하나: float 평면 오디오를 받는 큐(`write` 가 가득 차면 장치가 재생할 때까지 기다림 = 렌더링 속도 맞춤), `getQueuedFrames`, `start/stop/flush/close`. **`stop()` 은 어느 스레드에서나 부를 수 있고 기다리는 `write` 를 곧바로 돌려보낸다**(제어 쪽이 멈춘 장치 위의 일시정지/닫기를 풀 수 있게) |
| `output/OutputFactory`, `output/SystemOutputFactory` | 출력을 여는 팩터리(드라이버의 렌더링 스레드가 부름, 호스트/시험이 바꿀 수 있음). 기본은 Java Sound |
| `output/JavaSoundOutput` | `SourceDataLine` 출력. float → 16비트 LE(가장 가까운 값으로 반올림, 자르기, NaN 무음), 라인 버퍼의 빈 자리만큼 쓰는 비블록 쓰기 + 가득 차면 잠자기, 장치가 요청한 표본율/채널을 지원하지 않으면 대체 주파수·채널 수를 시도하고 `media.webaudio.decode.Resampler` 로 변환, `Lines` 인터페이스로 소리 시스템을 주입(시험은 가짜 장치) |
| `output/NullOutput` | 장치 없음: 오디오는 버리고 가상 장치가 벽시계 속도로 큐를 비움. 가득 차면 `write` 가 기다림, 느리면 큐가 0 이 되고(언더런) 그때부터의 시간은 세지 않음 |
| `OutputRenderDriver` | `RealtimeAudioContext.Driver` 구현(2절) |
| `ScriptProcessorNode` | 엔진 노드(3절) |
| `BaseAudioContext`(수정) | `createScriptProcessor(bufferSize, numberOfInputChannels, numberOfOutputChannels)` 메서드 하나를 `createPeriodicWave` 와 `createBuffer` 사이에 추가(C1 은 이 파일을 건드리지 않았다) |

시험(`tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio/`): `OutputRenderDriverTest` 15, `ScriptProcessorNodeTest` 14, `output/NullOutputTest` 6, `output/JavaSoundOutputTest` 10(합계 45건), 도우미 `output/{ManualOutput,ManualOutputFactory,FakeLine,FakeLines}`(스위트 아님).

## 2. `OutputRenderDriver` 의 설계와 결정

- **스레드와 구간**: 스레드 하나(`swingwebpanel-audio-render-N`, 데몬, 우선순위 보통+3)가 **첫 `start` 에 생기고 `close()` 까지 산다**(C1 의 `ClockRenderDriver` 와 같은 수명). 컨텍스트가 도는 **구간(segment)** 마다 출력을 열고 청크를 렌더링하다가 `pause()`/`close()` 에서 출력을 해제한다. 그래서 **일시정지된 컨텍스트는 소리 장치를 붙잡지 않고**, 일시정지는 곧바로 조용해진다(큐의 오디오는 버려진다). 컨텍스트 시간은 렌더링한 프레임에서 이어진다(C1 규약). 구간은 `epoch` 번호로 구분한다: 일시정지 직후 곧바로 재개해도 이미 끊긴 구간은 반드시 끝나고 새 구간이 새 출력을 연다(두 구간이 동시에 렌더러를 부르는 일은 없다).
- **지연 설계(`latencyHint` → 크기)**: 컨텍스트를 만들 때의 지연(C1 의 `resolveLatency`: 10/20/100 ms)이 두 크기를 정한다. **청크** = 지연(quantum 수)의 절반, 1~16 quantum. **버퍼** = max(50 ms, 지연의 4배)를 청크의 정수 배(최소 둘)로 올림. 버퍼를 지연 그 자체로 하지 않은 이유: Java Sound 라인은 수 밀리초의 버퍼에서 믿을 수 없다. 상수 `MIN_BUFFER_SECONDS`, `BUFFER_PER_LATENCY`, `MAX_CHUNK_QUANTA` 로 조정할 수 있다. 실제 측정값(JDK 6u45, 이 PC):

  | 표본율 | 지연 힌트 | 청크 | 버퍼 | 보고한 `outputLatency`(가득 찼을 때) |
  | --- | --- | --- | --- | --- |
  | 44100 | interactive 10 ms | 128 프레임(2.9 ms) | 2304 프레임(52 ms) | 52.2 ms |
  | 44100 | balanced 20 ms | 384(8.7 ms) | 3840(87 ms) | 87.1 ms |
  | 44100 | playback 100 ms | 2048(46 ms) | 18432(418 ms) | 418 ms |
  | 48000 | interactive | 128(2.7 ms) | 2432(51 ms) | 50.7 ms |
  | 48000 | balanced | 384(8 ms) | 3840(80 ms) | 80 ms |
  | 48000 | playback | 2048(43 ms) | 20480(427 ms) | 427 ms |

  `playback` 의 버퍼는 400 ms 대로 길다(끊김 없음을 위한 값). 길면 `BUFFER_PER_LATENCY` 를 줄이면 된다. `baseLatency` 는 C1 이 정한 값(quantum 올림, 최소 2 quantum)을 그대로 쓰고, 드라이버는 **쓸 때마다 큐의 오디오 시간을 `reportOutputLatency` 로 알린다**(가득 찬 장치에서 `outputLatency` = 버퍼) 그리고 구간이 끝나면 0 으로 되돌린다.
- **끊김(underrun) 정책**: 렌더링이 실시간보다 느려 장치가 비면(쓰기 직전에 큐가 0) 횟수를 센다(`getUnderrunCount()`). 그러면 스레드는 버퍼가 다시 찰 때까지 쉬지 않고 렌더링해 쓴다. **오디오를 버리거나 반복하지 않으며 지연이 커지지 않는다**: 소리의 끊김이 부족했던 시간만큼 이어지고, 렌더링한 프레임은 모두 출력에 쓰인다(`testNothingIsLostWhenTheDevicePlaysAtOnce`: 닫은 뒤 출력이 받은 프레임 == 컨텍스트가 렌더링한 프레임). 소리를 시작하기 전에 **버퍼를 미리 채운다**(prefill: 한 청크가 더 들어가지 않을 때까지 렌더링한 뒤 `start()`) 그래서 처음 소리의 끊김이 없다.
- **장치 없음/열기 실패**: 구간마다 `OutputFactory.open` 을 시도하고 `IOException`/`RuntimeException`/표본율이 다른 출력이면 그 구간은 **`NullOutput`** 으로 재생한다(`getOutputProblem()` 에 이유, `isOutputAudible()` false). 시간은 실제 속도로 가고(`NullOutput` 이 쓰기를 늦춘다) 그래프는 동작한다. **다음 구간은 장치를 다시 시도하므로 나중에 나타난 장치를 쓴다**(`testDeviceThatAppearsLaterIsUsedByTheNextSegment`). 지시는 "`ClockRenderDriver` 로 대체하는 팩터리"였지만 같은 역할을 **같은 코드 경로**로 하도록 드라이버 안의 `NullOutput` 으로 했다(실제 장치와 같은 prefill/버퍼 앞섬이 있어 `currentTime` 이 장치 유무에 관계없이 같은 모양으로 흐르고 구간마다 장치를 다시 시도할 수 있다). 장치 없이 쓰고 싶은 호스트를 위해 `OutputRenderDriver.silentOutputs()` 와 `setOutputFactory(...)`(다음 구간부터 적용)를 두었다 — `setSinkId({type:"none"})` 에 쓰면 된다.
- **멈춘 장치**: 장치가 멈춰 `write` 가 기다리는 동안에도 `pause()`/`close()` 는 `AudioOutput.stop()` 으로 기다림을 풀어 몇 밀리초 안에 돌아온다(`testStalledDeviceDoesNotHoldAPauseOrAClose`).
- **표본율**: 컨텍스트의 표본율은 장치와 **무관**하다(C1 의 기본 48000). 장치가 그 표본율을 재생하지 못하면 `JavaSoundOutput` 이 48000, 44100, 96000, 32000, 24000, 22050, 16000, 12000, 11025, 8000 Hz 순으로 시도해 `Resampler`(B 단계, 저지 대역 100 dB)로 변환한다. 변환하면 **약 64 프레임(44100→48000 에서 1.5 ms)의 지연이 더해진다**(선형 위상 필터, `getConversionDelayFrames()`). 채널은 요청(2)이 안 되면 2, 1 을 시도한다. 컨텍스트 목적지가 출력 채널과 다르면 스피커 규칙(`ChannelMixing`)으로 섞는다(6채널 → 모노/6채널 장치 시험).
- **수명(누수 방지) 정책**: 렌더링 스레드는 **살아 있는 동안 렌더러(컨텍스트)를 붙잡는다**. 일시정지 중에는 출력도 장치도 없고 스레드는 대기만 한다. 규격처럼 **실행 중인 컨텍스트는 소유자(페이지)가 닫아야 한다**: 페이지 계층(`WebAudioApi.dispose()`)이 열린 컨텍스트를 `close()` 하는 C1 의 정리가 그 몫이다. GC 에서 컨텍스트와 스레드를 풀려고 약한 참조를 쓰지는 않았다(이유: 실행 중인 컨텍스트가 JS 가 놓았다고 소리를 멈추면 규격과 다르고, C1 이 열린 컨텍스트를 페이지의 목록에 강하게 둔다). 100 개 컨텍스트를 만들고 닫으면 스레드와 출력이 남지 않음을 시험했다.
- **스레드 규약**: `start/pause/close` 는 곧바로 돌아오며(C1 의 컨텍스트가 직렬화해서 부른다) 플래그와 알림, 출력 `stop()` 만 한다. 렌더러와 출력을 부르는 스레드는 렌더링 스레드뿐이고 스크립트는 부르지 않는다. 렌더러/출력이 예외를 던지면 `getRenderFailure()` 에 기록하고 **다음 `start` 까지 멈춘다**(끝없이 실패를 되풀이하지 않도록). 이때 C1 컨텍스트의 `state` 는 `running` 으로 남는다(C1 의 한계 그대로, C2b 가 필요하면 상태 변경 알림을 더한다).
- **`getOutputTimestamp()`**: 마지막 쓰기의 큐와 그 뒤 지난 시간으로 지금 들리는 컨텍스트 시간을 추정한다(렌더링한 시간을 넘지 않음). 재생 중이 아니면 렌더링한 시간. `System.nanoTime()` 기준 값을 함께 준다(C2b 가 `performance.now()` 기준으로 바꾼다).

## 3. `ScriptProcessorNode` 의 설계와 결정(결정 c)

규격(1.0 REC 1.29/1.12, 2026-11-22 `tools/webfetch` 로 다시 읽음, https://www.w3.org/TR/2021/REC-webaudio-20210617/)과 Chromium 의 동작(`script_processor_handler.cc` 의 이중 버퍼링)을 따랐다.

- **지연은 정확히 두 블록(`2 * bufferSize` 프레임)이다. 브리프의 "지연 = bufferSize" 와 다르다.** 근거: 블록 k 의 입력은 블록 끝에 완성되고, 이벤트는 그 순간 요청된다. 한 블록 지연이라면 다음 quantum 을 렌더링하기 *전에* 리스너가 응답해야 하는데, 작업 싱크(페이지의 이벤트 루프)의 다른 스레드에서 도는 리스너는 그럴 수 없다(렌더링 스레드는 장치 버퍼보다 앞서 가므로 응답 시간이 수 마이크로초가 된다). 그래서 Chromium 처럼 **리스너에게 블록 하나의 시간을 응답 시간으로 주고 출력은 블록 k+2 동안 재생**한다. `playbackTime` = 출력 버퍼가 재생되기 시작하는 컨텍스트 시간 `(k*B + 2*B) / sampleRate`. 처음 두 블록의 출력은 무음. 오프라인과 실시간이 같은 모델이다(결과가 같다).
- **실시간**: 리스너가 늦으면(출력이 필요한 경계에 응답이 없으면) 그 블록의 출력은 **무음**이고 블록을 **포기**한다(늦은 결과는 버려지고, 아직 시작하지 않은 작업은 리스너를 부르지 않는다). `getLateBlockCount()`. **오프라인**: 실시간을 지킬 필요가 없으므로 렌더링이 리스너를 기다린다(최대 `RESPONSE_TIMEOUT_MILLIS` 10 초, `setResponseTimeout` 으로 조정, 넘으면 늦은 블록). 기다림은 리스너를 실행하는 스레드에서 일어나면 안 된다 — C1 의 오프라인 렌더링 스레드(`swingwebpanel-audio-offline-N`)는 페이지 스레드가 아니므로 괜찮다.
- **리스너 규약**: `ScriptProcessorNode.ProcessListener.process(inputBuffer, outputBuffer, playbackTime)`. 입력은 블록의 **복사본**(호출 동안만 유효, 입력 채널 0 이면 null), 출력은 **처음에 무음**(출력 채널 0 이면 null)이고 **리스너가 돌아올 때 출력 버퍼에 남은 것이 재생**된다(별도의 `provide` 호출은 없다: 호출이 끝나는 것이 제공이다). 작업 싱크(`setTaskSink`)가 있으면 제어 쪽에서, 없으면 렌더링 스레드에서 실행된다(`testWithoutASinkTheListenerRunsOnTheRenderingThreadOnly`: 일시정지/재개를 80 번 빠르게 해도 동시에 둘이 실행되지 않고 렌더링 스레드 하나만 부른다). 리스너의 `RuntimeException` 은 가두고 `getLastListenerError()` 에 남긴다(출력은 리스너가 남긴 대로). 리스너가 없으면 이벤트도 없고 출력은 무음.
- **활성**: 입력이나 출력 연결이 하나라도 있는 동안 처리한다(규격: "actively processing"). 입력 없는 생성기와 출력 없는 분석기가 동작한다. 연결을 모두 끊으면 진행 중인 블록과 대기 중인 출력을 **잊는다**(다시 연결하면 처음 두 블록이 다시 무음). 노드는 만든 뒤 컨텍스트가 붙잡는다(`pin`, 입력 없는 생성기를 처리하려면 필요).
- **채널과 오류**: `numberOfInputChannels`/`numberOfOutputChannels` 0~32(둘 다 0 은 안 됨), `bufferSize` 0(= 2048, 구현이 고름) 또는 256~16384 의 2 의 거듭제곱 — 범위 밖은 모두 `IndexSizeError`(규격 본문과 Chromium 이 같음). `channelCount`(= 입력 채널 수, 입력 채널 0 이면 1 로 보고)와 `channelCountMode`(`explicit`)는 바꿀 수 없다: 규격은 `NotSupportedError` 라고 하지만(그 자체가 규격 이슈 2153/2154 로 "브라우저와 다름"이 표시됨) 이 프로젝트의 다른 노드(`ChannelSplitterNode` 등)와 브라우저처럼 **`InvalidStateError`** 를 던진다. `channelInterpretation` 은 바꿀 수 있다.
- **메모리**: 블록(입력+출력 버퍼) 셋을 노드와 함께 만들고(렌더링 중 할당 없음) 리스너가 느릴 때 최대 8 개까지 늘린다. 모두 쓰고 있으면 그 블록의 입력을 버리고(`getDroppedBlockCount()`) 이벤트를 요청하지 않는다. 작업이 나중에라도 실행되면 블록이 재활용된다(`testStuckListenerLimitsTheBlocks`). 블록은 렌더링 쪽과 리스너 양쪽이 놓아야 풀로 돌아간다(참조 수).

## 4. 검증 (JDK 6u45, 이 PC, 실제로 실행한 것)

- `sh scripts/pkgtest.sh c2a "media/webaudio" "media/webaudio" --filter org.duckdns.hjow.swingwebpanel.media.webaudio.output` → **16 통과 / 0 실패**. 같은 방식으로 `...webaudio.OutputRenderDriverTest` → **15 통과**, `...webaudio.ScriptProcessorNodeTest` → **14 통과**(합계 새 시험 45건).
- **안정성**: 같은 시험을 5 번 반복(`TestRunner` 직접 실행) 모두 통과, 드라이버 시험을 **4 개 프로세스로 동시에**(4코어 PC 의 부하) 돌려도 모두 통과(시간 시험은 아래쪽 한계 위주의 넉넉한 여유).
- **변이 시험**(구현을 한 곳 고쳐 시험이 실패하는지 확인하고 `git checkout` 으로 복구, 6 가지): (1) `playbackTime` 의 `2B` → `1B`: 3건 실패, (2) 늦은 블록을 포기하지 않음: 2건, (3) 일시정지가 기다리는 쓰기를 깨우지 않음(`wakeWriter` 제거): 5건 실패(스레드 51개가 남음), (4) 16비트 반올림 → 버림: `JavaSoundOutputTest` 2건과 드라이버 16비트 경로 시험 실패, (5) `NullOutput` 이 빈 뒤 기준을 다시 잡지 않음: 1건, (6) 일시정지가 구간을 끝내지 않음(`epoch` 무시): 1건. 모두 원래 코드로 복구했다(`git status` 로 확인).
- `sh build.sh` → **성공**(제품 전체 컴파일, `build/swingwebpanel*.jar`). `StaticStateAuditTest`, `ThreadCreationAuditTest`, `JarContentTest` 를 그 결과 위에서 실행 → **3 통과**(정적 가변 상태 없음, 새 스레드는 데몬 + `swingwebpanel-` 이름).
- `sh tools/sourcecheck/run.sh <새 경로들>` → **0 위반**.
- **`media/webaudio` 전체**(`--filter org.duckdns.hjow.swingwebpanel.media.webaudio`, A1/A2/B/내 시험): `pkgtest.sh c2a "media/webaudio" "media/webaudio"` 로는 **317 통과 / 6 실패**인데, 실패 6건은 모두 `decode/AudioBufferDecoderTest`/`TruncatedDataTest` 의 "unrecognized audio format"(MP3/AAC 등)으로 **내 변경과 무관한 pkgtest 의 한계**다: 선택 디코더가 리플렉션으로만 쓰여서 `javac -sourcepath` 가 컴파일하지 않는다(C1 도 같은 현상을 기록함). 소스 패키지에 `media` 전체를 주면(`pkgtest.sh c2b "media" "media/webaudio" --filter ...webaudio.decode`) **59 통과 / 0 실패**.
- **측정**(JDK 6u45, 임시 프로그램, 프로젝트 파일 아님): 2절 표의 크기·지연 값. 렌더링 비용(5초 분량 오프라인, 44100 Hz 스테레오, 톱니파→저역 통과→패너 목소리): 10개 12.8배속, 20개 7.6배속, 40개 3.4배속, 80개 1.9배속, 160개 1.0배속 — 목소리 100개 안팎부터 이 PC 에서 실시간을 맞추기 어렵다(공용 PC 의 값이라 참고용).
- **확인하지 못한 것**(8절에 정리): 실제 소리 장치, 실제 스피커로 들리는지, JS 쪽 연결, 전체 회귀.

## 5. 설계상 확인한 규격/참조와의 차이

1. `ScriptProcessorNode` 의 지연이 2 블록이다(3절). `channelCount` 변경 오류는 `InvalidStateError`(규격 본문은 `NotSupportedError`).
2. 일시정지는 큐에 남은 소리를 **버린다**(규격은 하드웨어를 멈추는 것이라고만 한다). 재개하면 렌더링한 프레임부터 이어지므로 일시정지 직전 장치 버퍼에 있던 최대 버퍼 시간(interactive 52 ms)의 소리는 다시 들리지 않는다.
3. `outputLatency` 는 장치 버퍼에 쌓인 오디오(우리가 쓴 것) 시간이다. Java Sound 는 장치/드라이버 안쪽의 지연을 알려 주지 않으므로 그만큼은 포함하지 않는다.
4. 컨텍스트의 표본율이 장치와 무관하다(Chromium 은 장치의 표본율을 쓴다). 장치가 재생하지 못하면 변환 지연이 더해진다.

## 6. 한계

- 실제 소리 장치에서 확인하지 못했다(이 PC 의 Java Sound 믹서는 JDK 6/8/25 모두 0 개). `JavaSoundOutput` 은 가짜 `SourceDataLine`(`FakeLine`)으로만 시험했다: 특히 **`SourceDataLine.available()` 의 실제 값, `line.write` 가 정말 블록하지 않는지, 일부 장치의 버퍼 크기 조정**은 실제 장치에서 확인이 필요하다(P22-10).
- 6 채널 이상의 장치 출력은 구조만 있다(요청은 2 채널, 기본 믹서가 지원하지 않으면 2→1 로 대체).
- 하나의 `AudioOutput` 은 컨텍스트마다 따로 연다(여러 컨텍스트/컴포넌트가 각자 라인을 연다). 같은 장치를 섞어 쓰는 믹서는 Java Sound 의 몫이다(장치가 중복 열기를 허용하지 않으면 두 번째부터 `NullOutput` 이 된다, 시험 못함).
- 드라이버 실패 때 컨텍스트 `state` 가 `running` 으로 남는다.
- `ScriptProcessorNode`: 응답이 한 블록 안에 오지 않는 리스너는 실시간에서 무음이다(작은 `bufferSize` 는 페이지 이벤트 루프가 바쁘면 끊긴다. Chromium 도 같음). 블록당 입력 복사 1 회와 출력 복사 1 회.

## 7. 임시로 가져온 C1 소스

C1 의 `RealtimeAudioContext.java`, `ClockRenderDriver.java` 를 `git show` 로 이 worktree 에 가져와 컴파일/시험했다(**commit 하지 않았고 작업 끝에 지웠다**). 내 commit 은 새 파일 + `BaseAudioContext` 한 메서드뿐이므로 조정자가 C1 을 병합한 뒤 내 브랜치를 main 위로 rebase 하면 충돌이 없다(C1 은 `BaseAudioContext` 를 건드리지 않았다). 내 시험이 C1 의 `RealtimeAudioContext` 를 쓰므로 **C1 병합 전의 main 에서는 컴파일되지 않는다**(`OutputRenderDriver` 가 `RealtimeAudioContext.Driver` 를 구현하므로 소스도 마찬가지).

## 8. 확인하지 못한 것과 사람이 확인할 것

- 실제 스피커로 소리가 나오는지(오실레이터 소리, 일시정지가 곧바로 조용한지, 끊김 없는지, `latencyHint` 별 체감 지연, 44100 ↔ 48000 변환의 소리 품질, 한 페이지에서 두 컨텍스트가 동시에 나는지, 장치 사용 중일 때 무음 대체).
- `ScriptProcessorNode` 가 JS 핸들러로 실제 페이지에서 동작하는지(C2b).
- 전체 회귀(`test.sh`)는 돌리지 않았다(조정자 몫).

## 9. C2b 를 위한 API 메모

- **컨텍스트 만들기**: `ContextApi.createRealtime` 의 `new RealtimeAudioContext(sampleRate, latency, new ClockRenderDriver(name))` 한 줄을 `OutputRenderDriver.createContext(sampleRate, latency, outputFactory, name)` 로 바꾼다(드라이버를 만들고 컨텍스트에 `attach` 해서 `outputLatency` 가 닿는다. 직접 하려면 `new OutputRenderDriver(name, factory, latency)` → `new RealtimeAudioContext(rate, latency, driver)` → `driver.attach(ctx)`). 이름은 그대로 `"swingwebpanel-audio-render-" + api.nextRenderSerial()`. `outputFactory` 가 null 이면 `SystemOutputFactory`(Java Sound). 호스트가 소리를 다른 곳으로 보내게 하려면 P18 의 `createAudioOutputFactory()` 처럼 `SwingWebPanel` 에 보호 메서드(예: `createWebAudioOutputFactory()`)를 두고 그 결과를 여기에 넘기면 된다(`OutputFactory` 는 `AudioOutput` 을 돌려주고, 시험용 `ManualOutputFactory` 와 같은 모양).
- **상태 전이**: C1 의 `resume()/suspend()/close()` 가 `driver.start/pause/close` 를 곧바로 부르고, 드라이버는 즉시 돌아온다(장치 열기와 prefill 은 렌더링 스레드가 한다). 새 컨텍스트가 `running` 이 되는 시점(C1 이 `statechange` 한 번)은 바뀌지 않는다. 장치를 열 수 없어도 `running` 이다(소리 없이 시계만 흐름). 장치 상태가 궁금하면 `((OutputRenderDriver) ctx.getDriver()).isOutputAudible()`, `getOutputProblem()`, `getUnderrunCount()`.
- **`getOutputTimestamp()`**: `driver.getOutputTimestamp()` 의 `getContextTime()` 이 `contextTime`, `performanceTime` 은 `performance.now() - (System.nanoTime() - ts.getNanoTime()) / 1e6`. 드라이버가 `OutputRenderDriver` 가 아니면(`ClockRenderDriver`) C1 의 추정을 쓴다. `outputLatency`/`baseLatency` 는 컨텍스트가 이미 보고한다.
- **`sinkId`**: `setSinkId({type:"none"})` → `driver.setOutputFactory(OutputRenderDriver.silentOutputs())`, `setSinkId("")` → `driver.setOutputFactory(null)`(또는 호스트 팩터리). 이미 실행 중인 구간은 자기 출력을 유지하므로 **새 팩터리는 컨텍스트가 일시정지되었다가 재개될 때(다음 구간부터) 적용된다**. 곧바로 적용하려면 컨텍스트를 `suspend()` 했다가 `resume()` 해야 하며 이는 스크립트에 `statechange` 두 번으로 보인다(그래서 바로 적용을 요구하지 않는 한 다음 `resume` 에 맡기는 것을 권한다).
- **`ScriptProcessorNode` 연결**: `ctx.createScriptProcessor(bufferSize, in, out)`(WebIDL 기본값 `bufferSize` 0, in 2, out 2; 오류는 `AudioException`, `IndexSizeError`). `NodeApi.define("ScriptProcessorNode", AudioNode 프로토, ScriptProcessorNode.class, ...)` 와 읽기 전용 `bufferSize`, `onaudioprocess`(`EventHandlers.define`). `onaudioprocess` 나 `addEventListener("audioprocess")` 가 생기면 `node.setProcessListener(l)`, 모두 없어지면 `setProcessListener(null)`(그러면 이벤트도 없고 출력은 무음). `l.process(inputBuffer, outputBuffer, playbackTime)` 은 **페이지 작업**으로 불린다(`ctx.setTaskSink(api.sink())` 가 이미 돼 있어야 함 — C1 이 하고 있다) 그 안에서: ① 엔진의 `AudioBuffer`(float)에서 JS `AudioBuffer` 래퍼(`AudioBufferValue`, 입력은 float → 채널별 저장소로 복사, 출력은 0)를 만들고 ② `AudioProcessingEvent`(이미 정의됨)를 `api.fire(nodeValue, event)` 로 전달하고 ③ 핸들러가 돌아오면 출력 래퍼의 채널 데이터를 **엔진의 `outputBuffer.getChannelData(c)` 로 되복사**한다(1.4.3 의 acquire). 엔진의 블록 버퍼는 리스너가 돌아온 뒤 재활용되므로 래퍼가 그것을 붙잡으면 안 된다(래퍼를 분리/무효화). JS 예외는 리스너 안에서 보고하고 삼켜도 된다(엔진은 `RuntimeException` 만 가둔다). 입력 채널 0 이면 `inputBuffer` 는 null(이벤트의 `inputBuffer` 는 채널 0 인 `AudioBuffer` 가 없으므로 null 로 노출하거나 빈 버퍼), 출력 채널 0 이면 `outputBuffer` null.
- **오프라인에서**: 오프라인 렌더링 스레드가 블록마다 페이지 작업이 리스너를 마칠 때까지 기다린다(기본 최대 10 초). 페이지 스레드가 오프라인 렌더링을 직접 돌리면 안 된다(교착). C1 의 `OfflineRender` 는 별도 스레드이므로 괜찮다. 페이지가 끝나 작업이 버려지면 대기는 제한 시간(또는 `setResponseTimeout`)까지 걸리므로, 페이지 정리(`cancel`)에서 대기를 빨리 끝내려면 필요에 따라 `setResponseTimeout(1)` 을 호출하거나 노드를 비활성으로 두면 된다.
- **정리**: `ScriptProcessorNode` 는 따로 자원이 없다(스레드 없음). 컨텍스트를 `close()` 하면 렌더링이 끝나므로 블록은 노드와 함께 수거된다. 오디오 라인은 드라이버가 구간마다 열고 닫으므로 `WebAudioApi.dispose()` → `ctx.close()` 로 충분하다(스레드가 끝나고 출력이 닫힘을 시험함).

## 10. 체크박스 후보 (체크는 조정자 몫)

- **P25-07**: 이 단계로 항목의 요구 중 **실시간 `AudioContext` 의 Java Sound 출력(`SourceDataLine`), 장치 없음/열 수 없음 때의 상태와 시간 진행(`NullOutput`), `latencyHint` 와 버퍼 크기, 장치 쪽 지연(`outputLatency`/`getOutputTimestamp`), 끊김 정책, 일시정지/재개/닫기 때의 라인과 스레드 정리**가 끝났다(자동 검증). 남은 것: **JS 연결(`createRealtime` 의 한 줄과 `sinkId` none 연결)**, `ScriptProcessorNode` 의 JS 바인딩, `<audio>` 요소 연결(`MediaElementAudioSourceNode`, C2b), 이동/clean/dispose 도중의 연결 시험, **실제 장치에서의 청취 확인(P22-10)**. 따라서 **C2b 와 사람의 확인 뒤에 체크하는 것을 권장**한다.
- **P25-04**: `ScriptProcessorNode`(결정 c)의 엔진 쪽이 끝났다. 항목의 나머지(`MediaElementAudioSourceNode` 의 `<audio>` 연결)는 C2b.
- **P25-02**: C1 의 기록대로 "렌더링은 별도 오디오 스레드에서"가 충족된다(이제 장치로 내보내는 스레드이기도 하다). 체크 여부는 조정자 판단.

## 조정자 요청

1. **FEATURE_MATRIX 행 초안**(WAU-08c 갱신 + 새 행):

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-08c(갱신) | 실시간 `AudioContext`: 상태/`statechange`, `resume/suspend/close`, 시간, 지연, `getOutputTimestamp`, `sinkId`, 6개 한도. **장치 출력**(아래 WAU-12) | 1.2, 1.1 WD | `media.webaudio.{RealtimeAudioContext,ClockRenderDriver,OutputRenderDriver}`, `js.host.webaudio.ContextApi` | C1 의 시험 + `OutputRenderDriverTest` 15 | 부분 구현(JS 연결은 C2b, 청취 확인 못함) | 자동 재생 제한 없음(결정 e), 표본율 48000 고정(장치가 못 하면 변환, 1.5 ms 지연), 일시정지는 큐의 소리를 버림 |
| WAU-11 | `ScriptProcessorNode`(폐기 예정)/`AudioProcessingEvent` 의 엔진: 2 블록 지연(`playbackTime` 은 출력 재생 시각), 입출력 채널 0~32, 오프라인은 리스너를 기다림, 실시간은 늦으면 무음, 블록 풀(최대 8) | 1.0 REC 1.29, 1.12 | `media.webaudio.ScriptProcessorNode`, `BaseAudioContext.createScriptProcessor` | `ScriptProcessorNodeTest` 14 | 엔진 검증 완료(JS 노출 C2b) | 지연 `2*bufferSize`(Chromium 과 같음), `channelCount` 변경 오류는 `InvalidStateError`(규격은 `NotSupportedError`), `bufferSize` 0 은 2048 |
| WAU-12 | 소리 출력: Java Sound `SourceDataLine`(16비트, 반올림), 장치가 요청한 표본율/채널을 지원하지 않으면 대체와 `Resampler` 변환, 장치 없음/열기 실패는 `NullOutput`(시간은 실제 속도), prefill, 끊김 횟수, 멈춘 장치가 일시정지/닫기를 붙잡지 않음 | P25-07 | `media.webaudio.output.*`, `media.webaudio.OutputRenderDriver` | `JavaSoundOutputTest` 10, `NullOutputTest` 6, `OutputRenderDriverTest` 15 | 부분 구현(가짜 장치로 검증, 실제 장치와 청취는 확인 못함) | 6 채널 이상 장치 구조만, 장치 내부 지연은 알 수 없음, 컨텍스트마다 라인 하나 |

2. **`docs/progress/media-play.md` "미확인" 표**: 다음 항목을 추가해 주세요(사람이 소리 장치가 있는 PC 에서): Web Audio 실시간 출력(`OutputRenderDriver`)으로 오실레이터/버퍼 소리가 들리는지, 일시정지가 곧바로 조용한지, 끊김 없는지, `latencyHint` 3 종의 체감 지연, 장치가 44100 Hz 를 지원하지 않을 때 변환된 소리의 품질, 두 컨텍스트 동시 재생, 장치 사용 중일 때 무음 대체, `setSinkId({type:"none"})` 후의 무음.
3. **ARCHITECTURE.md**: 패키지 표에 `media.webaudio.output`(소리 출력), 스레드 모델의 "실시간 컨텍스트" 항목을 "`RealtimeAudioContext` + `OutputRenderDriver`(스레드 `swingwebpanel-audio-render-N`, 장치로 재생, 장치 없으면 `NullOutput` 으로 벽시계 속도, 구간마다 장치 열기/닫기)"로 보강, "자원 소유 목록과 정리 담당(P21-01)" 표의 `WebAudioApi` 행에 "오디오 출력 라인(`OutputRenderDriver` 구간마다 열고 닫음, `ctx.close()` 로 끝남)"을 더해 주세요. 새 스레드 이름은 기존 `swingwebpanel-audio-render-` 그대로입니다.
4. **`scripts/pkgtest.sh`**: 소스 패키지에 `media/webaudio` 만 주면 선택 디코더(MP3/AAC 등, 리플렉션으로만 참조)가 컴파일되지 않아 `decode` 시험이 실패합니다(내 변경과 무관). `media/webaudio` 시험을 돌릴 때는 `"media"` 를 소스 패키지로 주는 것을 문서(PROGRESS 의 도구 메모)에 적어 주세요.
5. **README(루트)**: 사용 설명에 "`AudioContext` 는 Java Sound 장치가 있으면 소리가 나고, 없으면 소리 없이 시계만 흐른다. `ScriptProcessorNode` 는 폐기 예정 API 의 호환 구현(지연 2 블록)" 정도를 쓸 때 적어 주세요(새 공개 Java API: `media.webaudio.OutputRenderDriver`, `media.webaudio.output.*`, `ScriptProcessorNode`; `SwingWebPanel` 의 공개 API 는 변하지 않았다).
6. **AGENTS.md 체크박스**: 10절 참고.
