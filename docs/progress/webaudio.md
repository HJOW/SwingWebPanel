# WEBAUDIO 작업 기록 (P25 Web Audio API)

이 문서는 WEBAUDIO 작업자의 기록이다. 작업 번호 대역은 `20261122-100` 부터이다. 단계: **A1**(P25-01, P25-02, P25-03, P25-04 의 1차 노드)을 이번에 맡았다.
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

## 1. P25-01 기준 판본과 조회 기록

조회는 `tools/webfetch`(JDK 6 로 빌드, 조회는 JRE 25 로 실행: `WEBFETCH_JAVA_HOME`)로 했다. 조회 날짜는 모두 2026-11-22 이다.

| 자료 | URL | 판본 | 조회 결과 |
| --- | --- | --- | --- |
| Web Audio API 1.0 권고안(출발 기준) | https://www.w3.org/TR/2021/REC-webaudio-20210617/ | W3C Recommendation, 17 June 2021 | status 200, 604341 바이트, `last-modified` 2021-06-08 |
| Web Audio API 1.1 작업 초안(현재 판) | https://www.w3.org/TR/2026/WD-webaudio-1.1-20260922/ (최신 판 주소 https://www.w3.org/TR/webaudio-1.1/) | W3C Working Draft, 22 September 2026 | status 200, 501084 바이트, `last-modified` 2026-09-11 |

- 조회 시점에 `https://www.w3.org/TR/webaudio/` 는 위 1.1 작업 초안으로 연결되었다(1.0 권고안 본문은 자기 자신을 "Latest published version: /TR/webaudio/" 로 적는다). 따라서 **구현의 기능 기준은 1.0 권고안**이고, 1.1 에서 늘어난 항목(`renderSizeHint`/`renderQuantumSize`, `interrupted` 상태, `setSinkId`/`AudioSinkInfo`, `AudioPlaybackStats`, `onerror`, `AudioContext.onsinkchange`)은 1.1 작업 초안 본문을 따로 읽고 표에서 `1.1` 로 표시한다.
- 규격 본문은 `build` 와 무관한 임시 위치(세션 scratchpad)에 `--text` 로 내려받아 읽었다(저장소에 원문을 넣지 않는다). 이번 단계에서 읽은 절: BaseAudioContext, AudioContext(생성자·상태·`close`), AudioNode(연결/분리/채널 규칙/수명/꼬리 시간), AudioParam(전체), AudioScheduledSourceNode, AudioBufferSourceNode(재생 알고리즘), AudioDestinationNode, BiquadFilterNode(계수식), ChannelMerger/Splitter, ConstantSource, DelayNode, GainNode, IIRFilterNode, OscillatorNode/PeriodicWave(파형, 정규화, 계수), StereoPannerNode, OfflineAudioContext(요약), 2절 처리 모델(렌더링 알고리즘, 제어 메시지), 4절 채널 업/다운 믹스, 6절 패닝(스테레오 패너).
- 규격 자체의 불일치 한 가지: AudioDestinationNode 표는 `numberOfOutputs` 를 1 로 적지만(1.0, 1.1 모두) 실제 브라우저는 0 이다. 이 구현은 **0**(브라우저 호환, 출력에 연결할 곳이 없음)으로 하고 차이를 기능 표에 남긴다.
- 규격의 `createPeriodicWave`/`PeriodicWave` 길이 불일치 오류는 본문이 서로 다르다(`IndexSizeError` 와 `NotSupportedError`). Chromium 처럼 `IndexSizeError` 를 쓴다.

### 참고 구현(Chromium)

| 파일(`third_party/blink/renderer/` 아래) | 용도 |
| --- | --- |
| `platform/audio/biquad.cc` | 쌍이차 필터 계수식과 경계값(주파수 0/1, Q 0) 처리 |
| `modules/webaudio/periodic_wave.cc`, `periodic_wave_handler.cc` | 파형 표(옥타브당 3개 대역, 표 크기 2048/4096/16384), 정규화, 표 보간 |
| `modules/webaudio/oscillator_handler.cc` | 위상 누산기, 주파수 제한, 표 보간 |
| `platform/audio/audio_bus.cc`, `modules/webaudio/audio_node_input.cc` | 채널 믹스와 입력 채널 수 계산 |
| `modules/webaudio/delay_node.cc` | 지연 노드 속성 규칙 |

- 리비전: `chromium/chromium` GitHub 미러의 `main` 최신 커밋 `8d101571406d3ef5bda7446b34568192ad99b3b5`(작성 2026-11-21 15:48 UTC), 조회 2026-11-22. 파일은 `raw.githubusercontent.com/chromium/chromium/<커밋>/third_party/blink/renderer/<경로>` 로 읽었다(전체 clone 금지).
- 이 리비전에서 `audio_param_timeline.cc` 는 위 두 위치에 없었다(404, 디렉터리 목록에도 없음). 자동화 타임라인은 규격 본문(1.6절)의 식을 직접 구현한다.
- Chromium 의 코드를 옮겨 적지 않았다(구조와 경계값 처리만 참고했고 식은 규격 본문에서 구현). 따라서 BSD 고지를 파일에 넣지 않는다. 경계값 처리를 참고한 파일: `dsp/BiquadKernel.java`, 표 구성 참고: `PeriodicWave.java`.

## 2. 인터페이스별 대상 표

단계 표기: A1(이번), A2(나머지 노드), B(`AudioBuffer`/디코딩), C(JavaScript 바인딩과 출력·수명), D(AudioWorklet과 최종 검증). 상태는 구현 정도이다. `엔진 검증 완료` 는 **순수 Java 엔진 수준에서** 해석적 기대값 시험이 통과했다는 뜻이며 JavaScript 에는 아직 노출되지 않았다(C 단계). 기능 표의 `검증 완료` 로 올리는 것은 C/D 에서 JavaScript 경로까지 확인한 뒤이다.

| 인터페이스 | 단계 | 구현 상태 | 비고 |
| --- | --- | --- | --- |
| BaseAudioContext(`destination`, `sampleRate`, `currentTime`, `state`, `createXxx` 팩터리, `renderQuantumSize`) | A1 | 부분 구현(엔진 검증 완료) | `listener` 는 A2, `audioWorklet` 은 D, `decodeAudioData` 는 B, `EventTarget`/`onstatechange` 는 C |
| OfflineAudioContext(`startRendering`) | A1 | 부분 구현(엔진 검증 완료) | 동기 렌더링과 `renderNextQuantum` 단계 진행. `suspend/resume`, `oncomplete`, Promise 는 C |
| AudioContext(실시간), `baseLatency/outputLatency/getOutputTimestamp/resume/suspend/close` | C | 미구현 | Java Sound 출력, 자동 재생 정책은 4.e |
| AudioBuffer | A1 기본형, B 완성 | 부분 구현 | 노드가 쓸 만큼의 순수 Java 자료형(채널 배열, 길이, 표본율, `copyFromChannel/copyToChannel`) |
| AudioNode(`connect`/`disconnect` 전 형태, `channelCount/channelCountMode/channelInterpretation`) | A1 | 엔진 검증 완료 | 제어 메시지 모델. `EventTarget` 상속은 C |
| AudioParam(자동화 전부, a-rate/k-rate) | A1 | 엔진 검증 완료 | `AudioParamTimeline` |
| AudioScheduledSourceNode(`start/stop/onended`) | A1 | 엔진 검증 완료 | `onended` 는 Runnable 리스너(이벤트 객체는 C) |
| AudioDestinationNode | A1 | 엔진 검증 완료 | `numberOfOutputs` 0 |
| AudioBufferSourceNode | A1 | 엔진 검증 완료 | 한계는 5.3절 |
| OscillatorNode / PeriodicWave | A1 | 엔진 검증 완료 | 대역 제한 표 |
| ConstantSourceNode | A1 | 엔진 검증 완료 | |
| GainNode | A1 | 엔진 검증 완료 | |
| DelayNode | A1 | 엔진 검증 완료 | 순환 허용 |
| BiquadFilterNode(`getFrequencyResponse`) | A1 | 엔진 검증 완료 | |
| IIRFilterNode(`getFrequencyResponse`) | A1 | 엔진 검증 완료 | |
| StereoPannerNode | A1 | 엔진 검증 완료 | |
| ChannelSplitterNode / ChannelMergerNode | A1 | 엔진 검증 완료 | |
| WaveShaperNode(oversample) | A2 | 미구현 | |
| ConvolverNode(normalize, 분할 FFT) | A2 | 미구현 | |
| DynamicsCompressorNode | A2 | 미구현 | |
| AnalyserNode | A2 | 미구현 | `dsp.Fft` 는 A1 에 있다 |
| PannerNode / AudioListener | A2 | 미구현 | equalpower + HRTF(4.d) |
| MediaElementAudioSourceNode | A2 | 미구현 | `<audio>` 만(4.b) |
| MediaStreamAudioSourceNode / MediaStreamAudioDestinationNode / MediaStreamTrackAudioSourceNode | C | 명시적 예외 | 4.a |
| ScriptProcessorNode / AudioProcessingEvent(폐기 예정) | C | 미구현(결정은 4.c) | |
| AudioWorklet / AudioWorkletGlobalScope / AudioWorkletNode / AudioWorkletProcessor / AudioParamDescriptor | D | 미구현 | `js/worker/**` 재사용 |
| OfflineAudioCompletionEvent | C | 미구현 | |
| `renderSizeHint`, `renderQuantumSize`(1.1) | A1(값), C(옵션) | 부분 구현 | 엔진은 크기를 매개변수로 받는다(기본 128) |
| `setSinkId` / `sinkId` / `AudioSinkInfo` / `onsinkchange`(1.1) | C | 미구현 | 4.e |
| `AudioPlaybackStats`(1.1) | C | 미구현 | 구조만(값은 0 이거나 `NotSupportedError`) |

## 3. 결정 사항 (a~e)

- **(a) MediaStream 계열**(`getUserMedia`, `MediaStreamAudioSourceNode`, `MediaStreamTrackAudioSourceNode`, `MediaStreamAudioDestinationNode`): 명시적 예외로 한다. 이 프로젝트에는 `MediaStream`/장치 입력이 없고 마이크 같은 장치 접근은 프로젝트 범위 밖이다. 해당 팩터리(`createMediaStreamSource`, `createMediaStreamDestination`, `createMediaStreamTrackSource`)는 있되 `NotSupportedError` 를 던지고, `MediaStream*` 생성자 이름은 전역에 노출하지 않는다(구조만 표시). 기능 표에 예외 행을 추가해야 한다(조정자 요청 참고).
- **(b) `MediaElementAudioSourceNode` 의 `<video>` 원천**: 비디오 예외에 따라 거부한다(`createMediaElementSource(<video>)` 는 `NotSupportedError`). `<audio>` 는 P18 의 재생기 출력을 그래프에 연결한다(A2). 교차 오리진 원천은 규격대로 무음이다.
- **(c) `ScriptProcessorNode`(폐기 예정)**: 레거시 페이지 호환을 위해 **C 단계에서 구현하기로 결정**했다(`bufferSize` 프레임을 모아 문서 큐로 `audioprocess` 이벤트를 전달하고 결과를 출력으로 돌려주는 지연 있는 노드, 입력 채널/출력 채널 수 1~32). A1 에서는 만들지 않는다. 그때까지 `createScriptProcessor` 는 `NotSupportedError`. AudioWorklet(D)이 같은 문서 큐 경로를 쓴다.
- **(d) HRTF 임펄스 응답 데이터**: 후보는 MIT Media Lab 의 KEMAR 측정 자료이다. 확인한 이용 조건(2026-11-22): `https://sound.media.mit.edu/resources/KEMAR/README` 가 "This data is Copyright 1994 by the MIT Media Laboratory. It is provided free with no restrictions on use, provided the authors are cited when the data is used in any research or commercial application." 라고 적는다(최종 수정 2000-07-14). 즉 **저자 표기를 조건으로 자유롭게 쓸 수 있다**. 압축 자료(`compact.zip`, 226 KB, 710개 위치, 44.1 kHz)는 A2 에서 `tools/` 도구로 내려받아 순수 Java 로 읽는 자원 파일로 바꾸고(원본 URL·판본·조회 날짜 기록, 저자 표기는 README 와 해당 파일의 Javadoc 에), 다른 표본율은 순수 Java 리샘플러로 맞춘다. 데이터를 쓸 수 없는 경우의 대체는 구면 머리 모델(우드워스 ITD + 머리 그림자 1차 필터)로 만든 합성 HRIR 이며 이 경우 근거를 적는다. **A1 은 PannerNode 를 만들지 않는다.**
- **(e) `sinkId`/`setSinkId` 와 자동 재생 정책**: 기본안은 `sinkId` 는 항상 `""`(기본 장치)이고 `setSinkId` 는 `""` 와 `{type:'none'}` 만 받아들이며 다른 값은 `NotSupportedError`(Java Sound 로 장치 이름을 고르는 것은 지원 범위 밖이므로 기능 표에 제약으로 적는다). 자동 재생 정책: 사용자 활성화(sticky activation)를 요구하지 않는다(규격이 "may disallow"). 컴포넌트는 호스트 Swing 앱이므로 막을 이유가 없고, 막으면 서버가 만든 데모가 소리가 나지 않는다. `AudioContext` 는 만들어지면 `running` 으로 시작하고 장치가 없으면 소리 없이 시계만 진행한다(C 단계에서 구현·검증). 이 결정은 C 단계에서 다시 확인한다.

## 4. A1 설계 요약 (구현 파일은 5.1절)

- 패키지 `org.duckdns.hjow.swingwebpanel.media.webaudio`(순수 Java, `javax.swing`/DOM/JavaScript 무관), 하위 `dsp`(FFT, 쌍이차 커널, IIR 커널).
- **스레드 모델**: 제어 스레드(JavaScript 또는 호출자)가 `connect/disconnect/채널 속성/start/stop` 을 부르면 동기 구간에서 검증하고, 렌더링 스레드가 render quantum 경계에서 **제어 메시지**를 적용한다. 단순 속성(발진기 type, loop 등)은 `volatile` 필드를 quantum 시작에 스냅샷으로 읽는다. 자동화 타임라인은 짧은 `synchronized` 구간으로 공유한다(한 quantum 의 값 계산은 한 번의 호출).
- **그래프 수명**: 엔진은 시작된 `AudioScheduledSourceNode` 와 활성/꼬리 시간 안의 노드만 강하게 붙잡고(`pinned`), 목적지에서 앞으로 도달 가능한 연결을 따라 처리 대상을 정한다. 연결도 시작도 안 된 노드는 처리하지 않는다(`AudioParam.value` 가 시간에 따라 변하지 않음: 규격 3절의 "해제 가능한 노드"에 해당).
- **순환**: 강연결 요소(Tarjan)를 찾아 DelayNode 가 든 순환은 읽기/쓰기 두 단계로 나누고(`delayTime` 은 한 render quantum 이상으로 제한), DelayNode 없는 순환은 해당 노드를 무음 처리한다.
- **채널 믹스**: 기존 `media/audio/ChannelMixer` 는 디코딩된 다채널을 스테레오로 줄이는 용도(LFE 버림, 풀스케일 보정)라 Web Audio 4절의 행렬(mono/stereo/quad/5.1 사이의 정확한 계수)과 다르다. 재사용하지 않고 `ChannelMixing` 에 규격 표를 그대로 구현한다.
- **GC 부담**: 렌더링 중 `AudioBus` 와 `float[]` 를 재사용하고 quantum 마다 새 배열을 만들지 않는다(채널 수가 바뀔 때만 재할당).

## 5. 구현 파일 / 검증 / 한계 / 다음 단계

### 5.1 구현 파일(`src/org/duckdns/hjow/swingwebpanel/media/webaudio/`)

| 파일 | 역할 |
| --- | --- |
| `package-info`, `dsp/package-info` | 패키지 설명 |
| `AudioException` | 오류 종류(`IndexSizeError`, `NotSupportedError`, `InvalidStateError`, `InvalidAccessError`, `EncodingError`, 언어의 `RangeError`/`TypeError`). JavaScript 계층이 `Kind` 로 예외를 고른다 |
| `ChannelCountMode`, `ChannelInterpretation`, `AutomationRate`, `ContextState`, `OscillatorType`, `BiquadFilterType` | 열거형(스크립트 이름 변환 포함) |
| `WebAudioLimits` | 이름 있는 제한 상수(quantum 128, 채널 32, 표본율 3000~768000, 버퍼 `1<<25` 표본, 활성 노드 20000, 자동화 이벤트 65536, 지연 180초 미만, IIR 계수 20, 주기파 16384, 꼬리 30초) |
| `AudioBus`, `ChannelMixing` | 채널 묶음과 규격 4.4/4.5절의 업/다운믹스(스피커 12쌍 행렬 + discrete) |
| `AudioBuffer` | 노드가 쓰는 순수 Java 오디오 자료(채널 배열, 길이, 표본율, `copyFromChannel/copyToChannel`) |
| `AudioParamTimeline`, `AudioParam` | 자동화 타임라인(모든 메서드, 암묵 이벤트, 접기)과 파라미터(a/k-rate, 입력 합산, 명목 범위) |
| `AudioNode`, `AudioNodeInput`, `AudioNodeOutput` | 노드 기반 클래스(연결 전 형태, 채널 속성, 제어 메시지, 렌더링 단계) |
| `RenderGraph` | 렌더링 순서(Tarjan, 순환의 지연 노드 분할, 순환 무음) |
| `BaseAudioContext`, `OfflineAudioContext` | 컨텍스트(제어 메시지 큐, 렌더링 루프, 붙잡기/놓기, 팩터리), 오프라인 렌더링 |
| `AudioScheduledSourceNode`, `AudioBufferSourceNode`, `OscillatorNode`, `PeriodicWave`, `ConstantSourceNode` | 예약 원천 노드와 주기파(대역 제한 표) |
| `GainNode`, `DelayNode`, `BiquadFilterNode`, `IIRFilterNode`, `StereoPannerNode`, `ChannelSplitterNode`, `ChannelMergerNode`, `AudioDestinationNode` | 처리 노드와 목적지 |
| `dsp/Fft`, `dsp/BiquadCoefficients`, `dsp/BiquadFilter`, `dsp/IirFilter` | FFT(복소 radix-2), 쌍이차 계수/커널, IIR 커널(응답, 꼬리 추정) |

시험(`tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio/`): `ChannelMixingTest` 8, `AudioParamTest` 22, `GraphTest` 18, `OscillatorNodeTest` 12, `PeriodicWaveTest` 3, `AudioBufferSourceNodeTest` 14, `DelayNodeTest` 8, `BiquadFilterNodeTest` 10, `IIRFilterNodeTest` 6, `StereoPannerNodeTest` 5, `ChannelSplitMergeTest` 5, `EngineStateTest` 6, `dsp/FftTest` 4(합계 121건), 도우미 `WebAudioTestKit`(스위트 아님).

### 5.2 검증(실제로 실행한 것, JDK 6u45)

- `sh scripts/pkgtest.sh wa "media/webaudio" "media/webaudio" --filter org.duckdns.hjow.swingwebpanel.media.webaudio` → **121 통과 / 0 실패 / 0 건너뜀**(1.3~2 초). 같은 시험을 여러 번 돌려도 같다(동시성 시험 `EngineStateTest.testControlThreadRacesTheRenderingThread` 를 11번 반복 실행해 모두 통과).
- `sh build.sh` → 제품 전체 컴파일과 `build/swingwebpanel.jar` 생성 성공(`javac 1.6.0_45`, JAR 에 `media/webaudio/` 클래스 50개, 시험 클래스 없음). `StaticStateAuditTest`(정적 필드 점검)와 `JarContentTest`(제품 JAR 내용) 통과. **전체 회귀(`test.sh`)는 실행하지 않았다**(조정자 몫, 공용 4코어 PC 의 부하 때문).
- `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/media/webaudio tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio docs/progress/webaudio.md` → 0 위반(52개 파일: 인코딩, 금지 API, 영/한 Javadoc, 반복문 한국어 주석).
- **시험의 성격**: 기대값은 모두 시험이 닫힌 식이나 독립 계산으로 만든다(구현 코드를 복사하지 않았다). 예: 사인파 `sin(2 pi f n / sr)`, 선형/지수 램프·목표 접근·값 곡선의 식, 버퍼 램프의 선형 보간, 지연의 정확한 프레임 수, 쌍이차 필터의 응답(중심 주파수 이득 `10^(Q/20)`, 대역 통과 1, 노치 0, 전역 통과 1·위상 pi, 피킹 `A^2`, 선반 `A^2`/1/`A`)과 규격의 계수식으로 시험이 직접 푼 차분 방정식, IIR 1극 필터의 응답과 위상, 스테레오 패너의 이득식, 믹스 행렬 12쌍, 피드백 에코 열 `y[200k] = 0.5^(k-1)`, 기본 파형의 급수 합(정규화 계수를 시험이 따로 계산). 허용 오차: 닫힌 식 비교 `1e-6`(float 출력 정밀도), 표 보간이 있는 사인 `3e-6`(표 2048점의 선형 보간 오차 `(2 pi / 2048)^2 / 8 = 1.2e-6`), 사각/톱니 파형은 불연속점에서 10 프레임 떨어진 곳에서 `0.06`(Gibbs 리플), 삼각파 `0.02`(부분음 약 29개로 줄일 때의 꼭짓점 오차 약 `0.013`).
- 시험이 구현의 결함을 잡는지 **변이 시험**을 손으로 3가지 해 보았다(5.1→2 믹스 가중치 변경, 패너 오른쪽 이득의 `sin` 을 `cos` 로, 지연 읽기 위치를 1 프레임 어긋나게): 모두 해당 시험이 실패했고 원래 코드로 되돌렸다.
- 속도(임시 측정, 프로젝트 파일 아님): 20개 목소리(톱니파 → 주파수가 램프하는 쌍이차 필터 → 패너) + 피드백 지연, 44.1 kHz 스테레오, JDK 6u45 에서 20초 분량 렌더링이 약 3초(6배속 이상, 최적화 전 2.3배속). 다른 작업자가 같은 PC 를 쓰는 상태에서 잰 값이라 참고용이다. 실시간 출력의 지연/끊김은 C 단계에서 장치로 확인한다.
- **확인하지 못한 것**: 실제 소리 출력(C), 다른 브라우저로 만든 참조 PCM 과의 비교(필요하면 ffmpeg 의 `biquad`/`aeval` 로 D 단계에서), JRE 6 이외의 JRE 에서의 실행, 매우 긴 렌더링(수 분)에서의 메모리.

### 5.3 한계와 규격/참조 구현과의 차이(기능 표에 옮길 것)

1. `AudioDestinationNode.numberOfOutputs` 는 0(규격 표는 1, 브라우저는 0).
2. `AudioBufferSourceNode` 는 `start()` 때 내용을 복사(acquire)하지 않고 채널 배열을 공유한다. 재생 중에 배열을 바꾸면 그 소리가 들린다. 소수 프레임 루프 끝점의 이음매는 이웃 프레임 사이 보간의 근사이다(끝점이 정수 프레임이면 정확하다).
3. 연결도 시작도 되지 않은(어떤 시작한 원천에서도 도달할 수 없는) 노드는 처리하지 않는다. 그런 노드의 `AudioParam.value` 는 시간에 따라 변하지 않는다(규격은 "계속 처리"라고 하지만 JavaScript 가 쥔 노드를 엔진이 알 수 없으므로 참조 구현과 같은 쪽을 택했다).
4. 리스너(A2)의 파라미터는 `registerContextParam` 으로 노드보다 먼저 계산하므로 **노드 출력으로 구동할 수 없다**(입력이 한 quantum 늦다). 규격은 순서 목록 안에서 계산한다.
5. `cancelScheduledValues` 는 시간 이후의 이벤트와 그 시간을 포함하는 값 곡선만 지운다(참조 구현과 같음). 이미 시작한 `setTargetAtTime` 은 취소하지 않는다. 규격의 "활성 자동화도 취소" 문장이 이를 뜻하는지 모호하다.
6. 발진기: 파형 표 선택은 참조 구현과 같되(옥타브당 3개, 크기 2048/4096/16384) 기본파가 남아 있는 마지막 표까지만 고르도록 제한해서 나이퀴스트 주파수 근처(마지막 1/3 옥타브)에서도 사인파의 진폭이 유지된다(참조 구현의 표 선택 코드를 그대로 따라 계산하면 이 구간에서 기본파의 진폭이 나이퀴스트 주파수의 0 까지 줄어든다. 실제 Chromium 으로 소리를 재 보지는 않았고 코드만 읽었다). 정확한 시작 시간이 프레임 사이에 있으면 규격 문장("노드의 정확한 시작 시간에서 위상 0")대로 부분 프레임 위상을 반영한다(`OscillatorNodeTest.testStartAndStopTimes`; 참조 구현이 어떻게 하는지는 읽지 않았다). 표의 정규화 최댓값은 표 점(2048/4096/16384)에서 잰다.
7. 꼬리 시간: 쌍이차 필터는 현재 계수의 극 반지름에서(16비트 문턱 `1/32768` 까지 줄어드는 프레임 수 + 2), IIR 필터는 노드를 만들 때 임펄스 응답을 시뮬레이션해서(4096 프레임 연속으로 문턱 아래이면 끝) 추정한다. 둘 다 30초로 제한한다. 지연 노드는 `maxDelayTime + 1 quantum`.
8. 지연 노드의 채널 수 변화: 입력 채널이 늘면 지연선의 내용을 즉시 새 배치로 섞고, 줄면 넓은 입력이 지연선에서 빠진 뒤(`maxDelayTime` 후)에 줄인다.
9. 순환 안의 지연 노드: `delayTime` 하한 1 quantum, 읽기/쓰기 두 단계, 출력이 `2^-126` 이상일 때만 활성. 지연 노드 없는 순환은 해당 노드 전부 무음(오류 없음).
10. 한 컨텍스트가 동시에 처리하는 노드 20000개 상한(넘는 노드는 무음), 파라미터당 자동화 이벤트 65536개(넘으면 `NotSupportedError`), 버퍼/지연선 `1<<25` 표본(`NotSupportedError`).
11. `AudioException` 의 `TYPE` 은 유한하지 않은 숫자(WebIDL `float`/`double` 변환 실패)이다. 실제 JavaScript 값 변환(문자열→숫자)은 C 단계의 바인딩이 한다.
12. 인터페이스 이름·열거형 이름은 스크립트 이름과 `getScriptName/fromScriptName` 로 연결한다. `EventTarget`(이벤트 객체, `statechange`, `ended` 이벤트 객체)은 C 단계의 바인딩이 `TaskSink`/`setEndedListener` 위에 만든다.

### 5.4 다음 단계(A2) API 메모

- **노드 만들기**: `AudioNode`(public 추상, 훅은 package-private)를 상속하고 같은 패키지에 둔다. 생성자는 `super(context, 입력 수, 출력 수, 기본 channelCount, 모드, 해석)` 를 부르고, 파라미터는 `registerParam(new AudioParam(context, this, 이름, 기본값, 최솟값, 최댓값, AutomationRate, 속도 고정 여부))` 로 등록한다(등록한 파라미터는 `process` 전에 자동 계산된다).
- **훅**: `abstract void process(int frames)`(활성일 때만 불린다. `inputBus(i)` 를 읽고 `outputBus(j)` 에 채널 수를 정해 쓴다), `long tailFrames()`(꼬리 시간. 0 보다 크면 엔진이 활성인 동안 노드를 붙잡는다), `void resetState()`(활성이었다가 비활성이 될 때 내부 상태 지우기), `checkChannelCount/Mode/Interpretation`(제약, 예외를 던진다), `isSource()`/`sourceActive()`(원천), `afterRender()`.
- **파라미터 읽기**: `param.isConstantBlock()` 이면 `param.valueAt(0)` 한 값, 아니면 `param.getBlockValues()[i]`(a-rate). k-rate 는 항상 상수 블록. `computeValues` 는 노드가 처리되기 전에 엔진이 부른다.
- **상수 크기**: `getContext().getRenderQuantumSize()`(기본 128), `getSampleRate()`, 한도는 `WebAudioLimits`. 렌더링 중 새 배열을 만들지 말고 노드 필드에 미리 할당한다(채널 수가 바뀔 때만 다시 할당).
- **컨텍스트**: 렌더링 진입점은 package-private `BaseAudioContext.renderQuantum()` 이다(한 번에 한 스레드). 실시간 컨텍스트(C)는 같은 패키지의 하위 클래스로 만들어 자기 스레드에서 이것을 부르고 `destination.getRenderedBus()` 를 장치로 보낸다. 오프라인은 `OfflineAudioContext.renderNextQuantum()/startRendering()`. 컨텍스트가 가진 파라미터는 `registerContextParam`(리스너용). 렌더링 스레드가 제어 쪽으로 보내는 일은 `postTask`(`TaskSink`). 공유 기본 파형은 `getBasicWave`.
- **PannerNode/AudioListener**: 리스너를 `BaseAudioContext` 에 두고 위치·방향 9개 파라미터를 `registerContextParam` 으로 등록한다. 패너는 리스너 파라미터의 `valueAt()` 를 읽는다. HRTF 는 4.d 의 계획대로 KEMAR 자료를 `tools/` 도구로 가져와 자원으로 둔다.
- **AnalyserNode**: `dsp.Fft`(복소 radix-2, 정규화 없음)를 쓴다. 제어 스레드가 읽는 `getFloatFrequencyData` 등은 렌더링 스레드가 쓰는 링 버퍼에서 `synchronized` 스냅샷으로 가져온다(`AudioParamTimeline` 과 같은 방식).
- **ConvolverNode**: `tailFrames()` 는 임펄스 응답 길이. 분할 FFT 는 `dsp.Fft` 위에. **DynamicsCompressorNode**: 규격의 6 ms 선행 지연이 있으므로 `tailFrames()` 에 반영하고 `attack/release/knee/ratio/threshold` 는 `rateLocked=true`(k-rate 고정)로 등록한다. **WaveShaperNode**: 곡선 보간과 oversample 은 `dsp` 에 별도 클래스로.

### 5.5 체크박스 후보(체크는 조정자 몫)

- **P25-01**: 완료 후보. 판본 표(5절의 1), 인터페이스별 대상 표(2절), 결정 a~e(3절), 조회 기록과 Chromium 참고 파일·리비전이 `docs/progress/webaudio.md` 에 있다. FEATURE_MATRIX 행 초안은 아래 "조정자 요청".
- **P25-02**: **미완료로 유지 권장**. 그래프, 믹스, 순환, 꼬리/수명, 제어 메시지, 오프라인 렌더링과 `currentTime/sampleRate/baseLatency/outputLatency` 접근자는 끝났지만, 항목의 "렌더링은 별도 오디오 스레드에서"(실시간 `AudioContext` 의 스레드)는 C 단계이다.
- **P25-03**: 완료 후보(모든 메서드, 오류 규칙, a-rate/k-rate, 명목 범위, 22건).
- **P25-04**: **미완료로 유지**. 1차 노드(목적지, 버퍼 원천, 발진기+주기파, 상수 원천, 게인, 지연, 쌍이차, IIR, 스테레오 패너, 스플리터, 머저)만 끝났다. WaveShaper, Convolver, DynamicsCompressor, Analyser, Panner/Listener(HRTF), MediaElementAudioSource 는 A2.
- P25-05~P25-08: 시작하지 않음.

## 조정자 요청

1. **FEATURE_MATRIX 행 초안**(새 절 "Web Audio (P25)" 로, 기존 EXC-07 의 "`<audio>` 외 미디어 캡처/EME/WebRTC 전역 객체 미노출"과 일관됨):

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-01 | 컨텍스트와 렌더링 루프: `BaseAudioContext`, `OfflineAudioContext`(제어 메시지, render quantum 128, 순서/순환, 꼬리 시간과 수명) | Web Audio API 1.0 REC(2021-06-17) + 1.1 WD(2026-09-22) 2절, 3절 | `media.webaudio.BaseAudioContext`, `OfflineAudioContext`, `RenderGraph`, `AudioNode*` | `GraphTest` 18, `EngineStateTest` 6 | 부분 구현 | 실시간 `AudioContext`/장치 출력, JavaScript 바인딩, `suspend/resume`, Promise/이벤트는 C. 연결도 시작도 없는 노드는 처리하지 않음(파라미터 값이 시간에 따라 변하지 않음) |
| WAU-02 | 채널 믹스: `channelCount/Mode/Interpretation`, 4.4/4.5절 행렬 | Web Audio API 4절 | `ChannelMixing`, `AudioBus`, `AudioNodeInput` | `ChannelMixingTest` 8, `GraphTest` | 엔진 검증 완료(JS 노출 C) | 스피커 배치는 1/2/4/6 채널 |
| WAU-03 | `AudioParam` 자동화 전부(a/k-rate, 명목 범위, 입력 합산) | Web Audio API 1.6 | `AudioParam`, `AudioParamTimeline` | `AudioParamTest` 22, `GraphTest` | 엔진 검증 완료(JS 노출 C) | 이벤트 65536 상한, 오래된 이벤트는 접힘, `cancelScheduledValues` 는 시작한 목표 이벤트를 취소하지 않음 |
| WAU-04 | 원천 노드: `AudioBufferSourceNode`, `OscillatorNode`/`PeriodicWave`, `ConstantSourceNode`, `AudioDestinationNode` | 1.7, 1.9, 1.16, 1.26, 1.28, 1.10 | `media.webaudio.*SourceNode`, `OscillatorNode`, `PeriodicWave`, `AudioDestinationNode` | `AudioBufferSourceNodeTest` 14, `OscillatorNodeTest` 12, `PeriodicWaveTest` 3 | 엔진 검증 완료(JS 노출 C) | 5.3절 1, 2, 6 (destination 출력 0, 버퍼 공유, 소수 루프 근사, 발진기 표) |
| WAU-05 | 처리 노드: `GainNode`, `DelayNode`(순환), `BiquadFilterNode`, `IIRFilterNode`, `StereoPannerNode`, `ChannelSplitterNode`, `ChannelMergerNode` | 1.20, 1.18, 1.13, 1.21, 1.30, 1.15, 1.14 | `media.webaudio.*Node`, `dsp.*` | `DelayNodeTest` 8, `BiquadFilterNodeTest` 10, `IIRFilterNodeTest` 6, `StereoPannerNodeTest` 5, `ChannelSplitMergeTest` 5 | 엔진 검증 완료(JS 노출 C) | 꼬리 시간은 추정(30초 상한) |
| WAU-06 | 나머지 노드: `WaveShaperNode`, `ConvolverNode`, `DynamicsCompressorNode`, `AnalyserNode`, `PannerNode`/`AudioListener`(HRTF), `MediaElementAudioSourceNode`(`<audio>` 만) | 1.31, 1.17, 1.19, 1.8, 1.27, 1.11, 1.22 | (A2) | | 미구현 | HRTF 는 KEMAR 자료(저자 표기 조건), `<video>` 원천은 `NotSupportedError` |
| WAU-07 | `AudioBuffer`, `decodeAudioData`(자체 디코더 재사용, 리샘플러) | 1.4, 1.1.2 | (B) | | 부분 구현 | A1 은 노드가 쓰는 자료형만 |
| WAU-08 | 실시간 `AudioContext`(Java Sound 출력, 상태/`resume`/`suspend`/`close`), `OfflineAudioContext` 의 Promise/`oncomplete`/`suspend`, JavaScript 바인딩(`js.host.webaudio`), `ScriptProcessorNode` | 1.2, 1.3, 1.29 | (C) | | 미구현 | 자동 재생 정책 없음, `sinkId` 는 기본 장치만 |
| WAU-09 | `AudioWorklet`(`addModule`, `AudioWorkletNode`/`Processor`) | 1.32 | (D, `js.worker` 재사용) | | 미구현 | |
| EXC-14 | `MediaStream` 계열 노드(`createMediaStreamSource/Destination/TrackSource`), `getUserMedia` | 장치 입력 없음(EXC-07 과 같은 범위) | `createMediaStream*` 은 `NotSupportedError`, 전역 `MediaStream*` 미노출(C) | | 명시적 예외 | 마이크/캡처 불가 |

2. **ARCHITECTURE.md**: 패키지 표에 `media.webaudio`(순수 Java 엔진)와 `media.webaudio.dsp` 를 추가하고, 스레드 모델(제어 메시지 + 렌더링 스레드, 타임라인은 `synchronized` 공유)과 "렌더링 중 새 배열을 만들지 않는다" 규칙을 적는다. 도구 목록은 바뀌지 않았다(`tools/webfetch` 만 사용, 조회는 JRE 25).
3. **AGENTS.md 의 P25 체크박스**: 5.5절 참고(P25-01, P25-03 후보). 다음 브리프는 A2.
4. `scripts/**` 변경 요청은 없다. `tools/sourcecheck` 는 이 패키지에서 위반 0 이다. 다른 작업자(엔진 분리 P26)와의 충돌: 이 작업은 새 패키지 `media/webaudio` 와 그 시험, 이 문서만 추가했고 기존 파일을 고치지 않았다.
5. 전체 회귀(`test.sh`) 실행을 부탁한다(이 작업의 변경이 기존 시험에 영향을 주지 않아야 하며, `StaticStateAuditTest`/`JarContentTest` 는 직접 통과를 확인했다).
