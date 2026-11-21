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

단계 표기: A1(이번), A2(나머지 노드), B(`AudioBuffer`/디코딩), C(JavaScript 바인딩과 출력·수명), D(AudioWorklet과 최종 검증). 상태는 구현 정도이며 `검증 완료` 는 해당 단계의 시험이 끝난 뒤에만 쓴다.

| 인터페이스 | 단계 | 구현 상태 | 비고 |
| --- | --- | --- | --- |
| BaseAudioContext(`destination`, `sampleRate`, `currentTime`, `state`, `createXxx` 팩터리, `renderQuantumSize`) | A1 | 구현(순수 Java) | `listener` 는 A2, `audioWorklet` 은 D, `decodeAudioData` 는 B |
| OfflineAudioContext(`startRendering`) | A1 | 구현(동기 렌더링) | `suspend/resume`, `oncomplete` 는 C |
| AudioContext(실시간), `baseLatency/outputLatency/getOutputTimestamp/resume/suspend/close` | C | 미구현 | Java Sound 출력, 자동 재생 정책은 4.e |
| AudioBuffer | A1 기본형, B 완성 | 부분 구현 | 노드가 쓸 만큼의 순수 Java 자료형(채널 배열, 길이, 표본율, `copyFromChannel/copyToChannel`) |
| AudioNode(`connect`/`disconnect` 전 형태, `channelCount/channelCountMode/channelInterpretation`) | A1 | 구현 | 제어 메시지 모델 |
| AudioParam(자동화 전부, a-rate/k-rate) | A1 | 구현 | `AudioParamTimeline` |
| AudioScheduledSourceNode(`start/stop/onended`) | A1 | 구현 | |
| AudioDestinationNode | A1 | 구현 | `numberOfOutputs` 0 |
| AudioBufferSourceNode | A1 | 구현 | |
| OscillatorNode / PeriodicWave | A1 | 구현 | 대역 제한 표 |
| ConstantSourceNode | A1 | 구현 | |
| GainNode | A1 | 구현 | |
| DelayNode | A1 | 구현 | 순환 허용 |
| BiquadFilterNode(`getFrequencyResponse`) | A1 | 구현 | |
| IIRFilterNode(`getFrequencyResponse`) | A1 | 구현 | |
| StereoPannerNode | A1 | 구현 | |
| ChannelSplitterNode / ChannelMergerNode | A1 | 구현 | |
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

## 4. A1 설계 요약 (구현 파일은 6절)

- 패키지 `org.duckdns.hjow.swingwebpanel.media.webaudio`(순수 Java, `javax.swing`/DOM/JavaScript 무관), 하위 `dsp`(FFT, 쌍이차 커널, IIR 커널).
- **스레드 모델**: 제어 스레드(JavaScript 또는 호출자)가 `connect/disconnect/채널 속성/start/stop` 을 부르면 동기 구간에서 검증하고, 렌더링 스레드가 render quantum 경계에서 **제어 메시지**를 적용한다. 단순 속성(발진기 type, loop 등)은 `volatile` 필드를 quantum 시작에 스냅샷으로 읽는다. 자동화 타임라인은 짧은 `synchronized` 구간으로 공유한다(한 quantum 의 값 계산은 한 번의 호출).
- **그래프 수명**: 엔진은 시작된 `AudioScheduledSourceNode` 와 활성/꼬리 시간 안의 노드만 강하게 붙잡고(`pinned`), 목적지에서 앞으로 도달 가능한 연결을 따라 처리 대상을 정한다. 연결도 시작도 안 된 노드는 처리하지 않는다(`AudioParam.value` 가 시간에 따라 변하지 않음: 규격 3절의 "해제 가능한 노드"에 해당).
- **순환**: 강연결 요소(Tarjan)를 찾아 DelayNode 가 든 순환은 읽기/쓰기 두 단계로 나누고(`delayTime` 은 한 render quantum 이상으로 제한), DelayNode 없는 순환은 해당 노드를 무음 처리한다.
- **채널 믹스**: 기존 `media/audio/ChannelMixer` 는 디코딩된 다채널을 스테레오로 줄이는 용도(LFE 버림, 풀스케일 보정)라 Web Audio 4절의 행렬(mono/stereo/quad/5.1 사이의 정확한 계수)과 다르다. 재사용하지 않고 `ChannelMixing` 에 규격 표를 그대로 구현한다.
- **GC 부담**: 렌더링 중 `AudioBus` 와 `float[]` 를 재사용하고 quantum 마다 새 배열을 만들지 않는다(채널 수가 바뀔 때만 재할당).

## 5. 구현 파일 / 검증 / 한계 / 다음 단계

(구현과 시험을 마친 뒤 아래 절을 채운다. 이 절이 비어 있으면 아직 작업 중이다.)

## 조정자 요청

(작업 중에 채운다.)
