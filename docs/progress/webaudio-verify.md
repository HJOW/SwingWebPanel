# Web Audio API 최종 검증 정리 (P25-08, 2026-11-22)

P25(Web Audio API)의 검증 근거를 한곳에 모은 문서이다. 작업자별 상세 기록은 `webaudio.md`(A1/A2), `webaudio-b.md`(디코딩), `webaudio-c.md`(JavaScript 바인딩), `webaudio-c2a.md`(실시간 출력),
`webaudio-c2b.md`(출력 연결과 `<audio>`), `webaudio-d1.md`(AudioWorklet)이다. 기능별 상태는 `docs/FEATURE_MATRIX.md` 8.4절(WAU-01~12, 예외 EXC-14)이 정본이다.
모든 검증은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로 했다.

## 1. 검증의 층(자동 검증과 사람 확인을 구분한다)

| 층 | 무엇으로 | 결과 |
| --- | --- | --- |
| 순수 Java 엔진(노드, 자동화, 믹스, 렌더링 순서) | 닫힌 식이나 독립 계산으로 만든 기대값 PCM(`media.webaudio.*Test`) | 통과(아래 2절) |
| JavaScript 경로 | 실제 페이지에서 스크립트가 그래프를 만들고 `OfflineAudioContext` 로 렌더링해 결과를 같은 방식으로 비교(`js.host.webaudio.*`) | 통과 |
| **독립된 구현과의 비교** | 실제 Chromium 156 이 같은 스크립트를 렌더링한 출력 67개 시나리오와 표본 단위 비교(`ChromeReferenceTest`, 3절) | 통과, 설명되는 차이 6가지 |
| 디코딩 | P18 시험 자료 121개와 ffmpeg 참조 PCM(무손실은 비트 단위), 손상 사본 1089개(`media.webaudio.decode.*`, `media.audio.wide.*`) | 통과 |
| 실시간 출력의 스레드·수명·누수 | 가짜 출력과 가짜 `SourceDataLine`, 100개 컨텍스트 생성/닫기 뒤 스레드·열린 출력 0, 이동/`clean()`/`dispose()`/스크립트 끄기, 두 컴포넌트 동시 재생 | 통과 |
| 장치 없음 경로 | `NullOutput`(벽시계 속도), 출력을 열 수 없는 팩터리에서도 스크립트 예외 없음 | 통과 |
| **실제 소리 장치와 청취** | 이 PC 는 이 세션에서 Java Sound 믹서가 0개 | **확인하지 못했다(P22-10, P25-07: 사람이 소리 장치가 있는 PC 에서)** |

## 2. 노드와 기능별 시험 (최근 전체 회귀에서 통과한 수)

| 기능 | 시험 클래스(통과 수) |
| --- | --- |
| 그래프와 렌더링 순서, 순환, 믹스, 상태 | `GraphTest` 18, `ChannelMixingTest` 8, `EngineStateTest` 6, `RealtimeAudioContextTest` 7 |
| AudioParam 자동화(모든 메서드, a/k-rate, 오류) | `AudioParamTest` 22 |
| 원천 노드 | `AudioBufferSourceNodeTest` 14, `OscillatorNodeTest` 12, `PeriodicWaveTest` 3, `dsp.FftTest` 4 |
| 처리 노드 | `DelayNodeTest` 8, `BiquadFilterNodeTest` 10, `IIRFilterNodeTest` 6, `StereoPannerNodeTest` 5, `ChannelSplitMergeTest` 5, `WaveShaperNodeTest` 10(`dsp.HalfBandTest` 7), `ConvolverNodeTest` 8(`dsp.PartitionedConvolverTest` 6), `DynamicsCompressorNodeTest` 6(`dsp.DynamicsCompressorKernelTest` 7), `AnalyserNodeTest` 9 |
| 공간화 | `PannerNodeTest` 9, `SpatialTest` 6, `HrtfPannerNodeTest` 8, `dsp.HrtfDatabaseTest` 4, `dsp.HrtfSetTest` 6 |
| `<audio>` 연결, 스크립트 프로세서 | `MediaElementAudioSourceNodeTest` 6, `MediaElementTapTest` 10, `ScriptProcessorNodeTest` 14 |
| AudioWorklet | `AudioWorkletNodeTest` 13, `js.host.webaudio.worklet.*` 약 45건 |
| 버퍼와 디코딩 | `AudioBufferTest` 7, `decode.ResamplerTest` 17, `decode.AudioBufferDecoderTest` 20, `decode.AudioDecodeServiceTest` 19, `decode.TruncatedDataTest` 3, `media.audio.wide.*` |
| 실시간 출력 | `OutputRenderDriverTest` 15, `OutputRenderDriverSwitchTest` 3, `output.JavaSoundOutputTest` 10, `output.NullOutputTest` 6 |
| JavaScript 바인딩 | `WebAudioInterfaceTest` 7, `OfflineRenderingTest` 10, `OfflineContextTest` 6, `AudioBufferScriptTest` 5, `DecodeAudioDataTest` 6, `DecodeDamagedDataTest` 9, `RealtimeContextScriptTest` 8, `WebAudioOutputScriptTest` 11, `MediaElementSourceScriptTest` 10, `WebAudioLifecycleTest` 8, `WebAudioComponentTest` 5, `WebAudioOutputComponentTest` 5 |
| 독립 구현과의 비교 | `ChromeReferenceTest` 1(시나리오 67개) |

작업자들은 구현의 한 군데를 바꿔 시험이 실패하는지 확인하는 손 변이 시험을 노드/구성요소마다 했다(A1 3건, A2 7건, B 9건, B2 7건, C1 5건, C2a 6건, C2b 8건, D1 7건 이상). 일부 변이는 처음에 통과해서 시험을 보강했다.

## 3. Chromium 과의 비교 결과 (이번 최종 검증에서 새로 한 것)

- 참조: 이 PC 의 내장 브라우저(Chrome 156.0.8078.100)로 `tests/resources/webaudio/chrome/scenarios.js` 의 67개 시나리오를 `OfflineAudioContext` 로 렌더링해 `ref/*.json` 에 저장(만드는 방법은 같은 디렉터리의 `README.md`). 시험은 저장된 파일만 읽고 브라우저를 실행하지 않는다.
- **일치**: 약 60개 시나리오가 float 정밀도(최대 절대 오차 대부분 2e-5 이하, 발진기 표와 지연 보간은 1e-4 이하)로 같고, 채널 믹스 8가지, IIR 필터, 계단 자동화, 스플리터/머저, 컨볼버 비정규화는 **비트 단위로 같다**. 컴프레서는 1e-5 로 같다(Chromium 의 설계를 옮긴 것).
- **설명되는 차이 6가지**(시험이 두 쪽의 값을 모두 단언한다): ① 웨이브셰이퍼 과표본화 필터의 지연(규격이 구현에 맡김; 지연을 맞추면 파형은 2e-5 이내로 같음), ② 나이퀴스트 가까이 발진기 기본파(Chromium 이 크게 줄임, 이 엔진은 규격대로 유지), ③ 순환 안의 지연(이 엔진은 규격대로 128 프레임 하한, Chromium 은 되먹임 경로에 한 quantum 을 더함), ④ 버퍼 원천 `offset` 의 소수 프레임(Chromium 은 버림, 이 엔진은 보간), ⑤ 프레임 사이의 시작 시각(Chromium 은 올림, 이 엔진은 정확한 시각), ⑥ 컨볼버 정규화 상수(규격 본문 0.00125 대 Chromium 10^(-58/20)).
- 이 비교로 엔진의 결함은 발견되지 않았고, A1 이 "측정하지 않았다"고 적은 발진기 나이퀴스트 근처 동작을 실제로 측정했다(②).
- 비교하지 않은 것: HRTF 패너(Chromium 과 자료가 다름), AnalyserNode, 디코딩(ffmpeg 로 따로 검증함).
- 같은 방법으로 AudioWorklet 의 `addModule` 실패 의미를 Chromium 에서 직접 확인했다(구문 오류는 그 오류로 reject, 평가 중 예외는 resolve, 네트워크 실패만 `AbortError`). 이 엔진은 그에 맞췄다.

## 4. 알려진 한계 (자세한 것은 각 기록과 FEATURE_MATRIX 의 제약 열)

- 실제 소리 장치 출력과 청취 확인 없음(가짜 장치로 PCM·상태·수명 검증). `media-play.md` 의 "미확인" 표에 사람이 확인할 항목이 있다.
- `crossorigin` 속성이 있는 교차 오리진 미디어를 `createMediaElementSource` 로 연결하면 CORS 요청을 구현하지 않아 그래프에서 무음이다.
- 인터프리터 비용: 단순한 AudioWorklet 프로세서 일곱 개 안팎이 48 kHz 실시간의 한계이고 바쁘면 그 quantum 은 무음(콘솔 경고).
- `MediaStream` 계열(마이크 등)은 명시적 예외(EXC-14).
- `unhandledrejection` 이벤트와 같은 브라우저 부가 이벤트는 이 단계의 범위 밖이다.

## 5. 재현 명령

- 전체 회귀: `BUILD_DIR=build sh test.sh`(엔진 단계 + 브라우저 단계, JDK 6u45).
- Web Audio 만: `sh scripts/pkgtest.sh wa "js dom html css net storage core content layout render download media @root" "js/host/webaudio media/webaudio" --filter org.duckdns.hjow.swingwebpanel` (컴포넌트 시험은 소스 패키지에 위 전체 목록이 필요하다).
