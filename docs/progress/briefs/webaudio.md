# WEBAUDIO 작업자 브리프 (P25 Web Audio API)

먼저 `docs/progress/briefs/common.md`(공통 규칙)를 읽는다. 이 문서는 P25 전체 구조와 **이번에 맡는 단계**를 정한다. 단계는 한 번에 하나씩 맡긴다(조정자가 다음 브리프를 보낸다).

## 작업 위치
- 당신은 git worktree 에서 일한다(작업 디렉터리가 그 worktree 이다). 모든 명령은 그 디렉터리 안에서 실행한다. 메인 체크아웃의 파일은 읽기만 한다.
- 기준 브랜치는 `main` 이다(이 저장소에는 `master` 가 없다). commit 메시지 번호 대역은 **`20261122-100` 부터 1씩**(조정자는 001~099, 엔진 작업자는 200~299).
- 이 저장소는 아직 Maven 을 쓰지 않는다(P23 은 맨 마지막). 빌드/시험은 `build.sh`, `test.sh`, `scripts/pkgtest.sh`, `scripts/snaptest.sh` 로 JDK 6u45 에서 한다. 전체 빌드는 약 1~3분, 전체 회귀는 수십 분이므로 **소유 패키지의 시험만** 돌린다.

## P25 전체(AGENTS.md "P25. Web Audio API"를 그대로 읽는다)
요구: W3C Web Audio API 를 순수 Java 6 로 구현한다. 그래프 엔진은 **JavaScript 와 무관한 순수 Java 패키지**로 만들고, JavaScript 연결은 그 위의 얇은 바인딩으로 둔다.

| 영역 | 패키지(기본안) |
| --- | --- |
| 오디오 그래프·AudioParam·노드·오프라인 렌더링(순수 Java, DOM/JS 무관) | `org.duckdns.hjow.swingwebpanel.media.webaudio` (하위 `dsp`, `node` 등은 자유) |
| JavaScript 바인딩(`AudioContext` 등 전역 객체와 노드/AudioParam 래퍼) | `org.duckdns.hjow.swingwebpanel.js.host.webaudio` |
| AudioWorklet 실행 문맥 | `org.duckdns.hjow.swingwebpanel.js.worker` 구조를 재사용(`js/worker/**` 읽을 것) |

단계(P25-01~08 은 AGENTS.md 본문):
- **A1** (이번 단계): P25-01 판본 고정과 대상 목록, P25-02 그래프·렌더링 엔진, P25-03 AudioParam 자동화, 그리고 P25-04 의 1차 노드.
- **A2**: P25-04 의 나머지 노드(WaveShaper, Convolver, DynamicsCompressor, Analyser, Panner/AudioListener/HRTF, MediaElementAudioSource).
- **B**: P25-05 AudioBuffer·decodeAudioData·리샘플러.
- **C**: JavaScript 바인딩 전체와 P25-07 출력·수명(`AudioContext` 실시간 출력, `OfflineAudioContext`, 이동/clean/dispose 정리).
- **D**: P25-06 AudioWorklet, P25-08 검증과 문서.

## 이번 단계 A1 의 일

### 1. P25-01: 기준 판본과 대상 목록 (먼저)
- `tools/webfetch`(`tools/webfetch/AGENTS.md` 를 읽을 것. JRE 25 로 실행)로 W3C Web Audio API 의 **현재 판본**(1.0 REC 와 이후 판)을 조회하고, 판본·URL·조회 날짜를 `docs/progress/webaudio.md` 에 기록한다. 규격 본문이 길면 필요한 절만 `tools/` 로 내려받아 읽는다.
- 인터페이스별 대상 표(BaseAudioContext, AudioContext, OfflineAudioContext, AudioNode, AudioParam, AudioBuffer, 각 노드, AudioListener, PeriodicWave, AudioWorklet 등)를 `docs/progress/webaudio.md` 에 만든다. 각 행에 `구현 상태`(미구현/부분/검증 완료)를 둔다. Chromium 의 `third_party/blink/renderer/modules/webaudio` 를 참고 구현으로 기록한다(필요한 파일만 `tools/webfetch` 로 읽는다. 전체 clone 금지. 경로·조회 날짜 기록).
- 결정해서 근거와 함께 기록할 것: (a) `MediaStream` 계열(`getUserMedia`, `MediaStreamAudioSourceNode`/`DestinationNode`)의 대상 여부(기본안: 장치 입력이 없으므로 명시적 예외, 구조만 표시하고 `NotSupportedError`), (b) `MediaElementAudioSourceNode` 의 `<video>` 원천(비디오 예외에 따라 거부), (c) `ScriptProcessorNode`(폐기 예정)의 처리, (d) HRTF 임펄스 응답 데이터의 출처와 이용 조건(순수 Java 로 쓸 수 있는 데이터가 없으면 근거를 적고 equalpower 로 대체), (e) `sinkId`/`setSinkId` 와 자동 재생 정책(사용자 활성화) 여부.
- `docs/FEATURE_MATRIX.md` 는 조정자 몫이다. "조정자 요청"에 추가할 행 초안을 적는다.

### 2. P25-02: 그래프와 렌더링 엔진 (순수 Java)
- render quantum 128 프레임, 모든 형태의 `connect`/`disconnect`(노드→노드, 노드→AudioParam, 특정 출력/입력 지정), `channelCount`/`channelCountMode`/`channelInterpretation`(speakers/discrete)에 따른 업믹스/다운믹스(기존 `media/audio/ChannelMixer` 를 읽고 재사용 여부 결정), 순환 규칙(DelayNode 가 있는 순환만 허용, 아니면 무음 처리), 입력이 끊긴 노드의 꼬리 시간과 수명, `currentTime`/`sampleRate`/`baseLatency`/`outputLatency`.
- 렌더링은 **별도 렌더링 스레드(또는 오프라인에서는 호출 스레드)** 에서 하며, 제어 메시지 모델(생성/연결/속성 변경을 렌더링 스레드가 render quantum 경계에서 적용)로 JavaScript 스레드와 상태를 주고받는다. 이번 단계는 **스레드와 무관한 코어**(렌더링 한 quantum 을 진행하는 메서드)와 **오프라인 렌더링**을 먼저 완성한다. 실시간 출력(`SourceDataLine`)은 C 단계다.
- 렌더링 중 버퍼를 재사용하고 객체를 새로 만들지 않는다(GC 부담). 노드 수/버퍼 크기 상한을 상수로 둔다.

### 3. P25-03: AudioParam 자동화
`value`, `setValueAtTime`, `linearRampToValueAtTime`, `exponentialRampToValueAtTime`, `setTargetAtTime`, `setValueCurveAtTime`, `cancelScheduledValues`, `cancelAndHoldAtTime`, a-rate/k-rate 와 `automationRate`, 명목 범위 제한(clamp), 이벤트 겹침 등의 오류 규칙(`NotSupportedError`, `RangeError`, `InvalidStateError`), 규격 알고리즘(sample-accurate 계산식)대로. 규격의 표본값 예를 시험에 쓴다.

### 4. P25-04 의 1차 노드
`AudioDestinationNode`, `AudioBufferSourceNode`(loop/loopStart/loopEnd, playbackRate, detune, start/stop 오프셋과 grain), `OscillatorNode` + `PeriodicWave`(대역 제한 파형 표, `setPeriodicWave`, 규격의 정규화), `ConstantSourceNode`, `GainNode`, `DelayNode`(최대 지연, 연속 지연 시간 변화), `BiquadFilterNode`(8가지 유형, `getFrequencyResponse`, 규격의 계수식), `IIRFilterNode`(`getFrequencyResponse`), `StereoPannerNode`, `ChannelSplitterNode`, `ChannelMergerNode`. (`AudioBuffer` 자료형은 B 단계에서 `decodeAudioData` 와 함께 마무리한다. 이번에는 노드가 쓸 만큼의 순수 Java `AudioBuffer`(채널 배열, 길이, 표본율)를 만든다.)

## 검증(이번 단계)
- 시험은 `tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio/**` 에 둔다(외부 시험 라이브러리 없음, `testing.Check`/`TestRunner` 규칙. 기존 시험의 구조를 따른다).
- **합성 신호의 해석적 기대값**으로 검증한다: 오실레이터 사인파의 표본값, GainNode 의 이득, 선형/지수 램프의 표본값, DelayNode 의 정확한 지연 표본 수, BiquadFilter 의 주파수 응답이 규격의 해석식과 같은지, 믹싱 규칙(mono→stereo 등)의 표. 허용 오차는 근거와 함께 적는다.
- 오프라인 렌더링 결과를 독립적으로 계산한 값과 비교한다(구현 코드를 시험에 복사해 쓰지 않는다).
- 다른 브라우저/도구(예: Node 의 오디오 라이브러리, ffmpeg 필터)로 만든 참조 출력을 쓰면 `tools/` 도구로 만들고 판본과 조건을 기록한다. 이 PC 에는 ffmpeg 이 있다(`libavfilter` 의 `aeval`/`biquad` 등). 없어도 해석적 기대값만으로 이번 단계는 완료할 수 있다.

## 이번 단계 완료 보고에 넣을 것
`docs/progress/webaudio.md` 에: 기준 판본 표, 결정 사항(a~e), 구현 파일 목록, 실행한 시험 명령과 실제 결과(통과/실패/건너뜀 수), 아직 안 한 것·한계, 다음 단계(A2)를 위한 API 메모(노드 기본 클래스, 파라미터 접근 방법, 렌더링 진입점), 체크박스 후보.
