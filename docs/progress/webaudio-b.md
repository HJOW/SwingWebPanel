# WEBAUDIO-B 작업 기록 (P25-05 AudioBuffer 와 decodeAudioData, 순수 Java 쪽)

이 문서는 WEBAUDIO-B 작업자의 기록이다. 작업 번호 대역은 `20261122-140` ~ `169` 이다(1단계에서 140~147, **B2 단계에서 148~155** 를 썼다). 앞 단계(A1)의 기록은 `docs/progress/webaudio.md` 이다.
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

**읽는 순서.** 1~9절은 1단계(커밋 140~147)의 기록이며, 그 뒤 B2 단계(커밋 148~154, 조정자의 승인으로 `media/audio/**` 디코더에 넓은 경로를 더하고 손상 정책을 관대하게 바꾼 일)가 바꾼 곳에는 "(B2 에서 바뀜: 10절)"로 표시하고 낡은 서술을 고쳤다. **현재 상태의 사실은 10절**과 고친 본문이다.

범위: P25-05 중 **JavaScript 와 무관한 순수 Java 쪽**. `decodeAudioData` 의 Promise/콜백 모양, 전달받은 `ArrayBuffer` 의 분리(detach), 결과를 문서 큐로 옮기는 일, `BaseAudioContext.decodeAudioData` 메서드 자체는 C 단계(JavaScript 바인딩)이다. 그래서 **P25-05 전체는 완료로 체크하지 않는 것을 권장한다**(아래 "체크박스 후보").

## 1. 읽은 규격

`tools/webfetch`(JRE 25 로 실행, 조회 2026-11-22)로 Web Audio API 1.0 권고안 https://www.w3.org/TR/2021/REC-webaudio-20210617/ (604341 바이트)를 다시 받아 다음 절을 읽었다. 1.1 작업 초안 https://www.w3.org/TR/2026/WD-webaudio-1.1-20260922/ 은 A1 이 받아 둔 본문에서 같은 절을 읽었다.

| 절 | 내용과 이 구현의 결정 |
| --- | --- |
| 1.1.2 `decodeAudioData` | 디코딩은 제어 스레드도 렌더링 스레드도 아닌 별도 "디코딩 스레드"에서 한다. 여러 디코딩 스레드가 병렬로 처리할 수 있고 결과 순서는 정하지 않는다. 형식은 MIME Sniffing 으로 판별한다(데이터 시그니처). 실패는 `EncodingError`. 성공하면 컨텍스트의 표본율과 다를 때 리샘플링한다. 결과는 "media element task"로 전달한다. → `AudioBufferDecoder`, `Resampler`, `AudioDecodeService` |
| 1.1.2 (1.1 WD) | 1.0 과 같고 추가가 둘 있다. (1) 미디어 바이트 스트림에 오디오 트랙이 여럿이면 **첫 트랙만** 디코딩한다. MP4(AAC)와 WebM 의 P18 디코더가 이미 `firstAudioTrack()` 을 쓴다(코드를 읽어 확인했고 트랙이 여럿인 시험 자료는 없다). (2) "손상됐지만 디코딩을 계속할 수 있으면 개발자 도구로 경고하라"는 비규범 주석. **(B2 에서 바뀜: 10절)** 이제 첫 프레임을 디코딩한 뒤의 손상은 부분 결과와 경고(`DecodeResult`/`DecodeWarningCallback`)로 끝난다. 1.0 REC 는 손상 시 `EncodingError` 라고만 하고 부분 결과를 말하지 않으므로 이는 1.1 WD 주석과 실제 브라우저의 동작에 맞춘 선택이다(3.4). |
| 1.1.2 `createBuffer` | 인수가 음수/0/공칭 범위 밖이면 `NotSupportedError`. 채널 32개 이상, 표본율 8000~96000 이상을 지원해야 한다(규격이 정한 최소 지원 범위). → A1 의 `WebAudioLimits`(표본율 3000~768000, 채널 32) |
| 1.4 `AudioBuffer` | 생성자 `NotSupportedError`(공칭 범위 밖). `getChannelData` 의 채널이 범위 밖이면 `IndexSizeError`. **`copyFromChannel`/`copyToChannel` 은 `max(0, min(N_b - k, N_f))` 프레임을 복사하고 `bufferOffset` 이 길이 이상이어도 오류가 아니다**(오류는 `channelNumber` 뿐). |

## 2. 구현 파일과 한 일

소스는 `src/org/duckdns/hjow/swingwebpanel/media/webaudio/` 아래이다. 시험은 `tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio/` 아래이다.

| 파일 | 역할 |
| --- | --- |
| `AudioBuffer.java`(변경) | `copyFromChannel/copyToChannel` 을 규격 1.4.3 의 식으로 바로잡음(2.1). 공개 정적 팩터리 `wrap(float[][], float)`(복사 없이 채널 배열을 가져감) 추가. 생성 검사를 한 메서드로 모음 |
| `decode/Resampler` | 표본율 변환기(Kaiser 창 sinc 다상 보간, 스트리밍, 채널 하나). 설계와 품질은 4절 |
| `decode/AudioBufferDecoder` | 파이프라인(**B2 에서 바뀜: 10절**): 바이트 → P18 `AudioDecoders.openWide`(시그니처로 디코더 선택) → 원래 채널과 float 정밀도의 블록(`FloatPcmSource`) → 유한하지 않은 표본을 0 으로 → 채널별 `Resampler` → `ChannelSink` → `AudioBuffer.wrap`. 첫 프레임 전의 실패는 원인이 붙은 `EncodingError`, 그 뒤의 손상은 부분 결과 + 경고, 취소는 `CancellationException`. `decodeResult(...)` 는 `DecodeResult`, `decode(...)` 는 버퍼만 반환한다. 시험용으로 소스를 직접 받는 `decodeSource` 가 있다(package-private) |
| `decode/DecodeResult`, `decode/DecodeWarningCallback`(B2) | 디코딩 결과(버퍼, 잘림 표시, 경고 문장, 원인)와, `onSuccess` 앞에 경고를 받는 `DecodeCallback` 의 하위 인터페이스 |
| `decode/ChannelSink` | 채널 하나의 출력 배열. 길이를 알면 그 크기 그대로(끝에서 복사 없음), 모르면 두 배씩 늘며 한도를 넘으면 `EncodingError` |
| `decode/DecodeCancellation` | 디코딩이 블록마다 확인하는 취소 플래그 인터페이스 |
| `decode/DecodeCallback`, `decode/DecodeRequest` | 결과 콜백(`onSuccess(AudioBuffer)`/`onError(AudioException)`)과 요청 핸들(`cancel()`, `isCancelled()`, `isDone()`) |
| `decode/AudioDecodeService` | 백그라운드 서비스. 자기 데몬 스레드에서 디코딩하고 호출한 쪽이 고른 `Executor` 에서 콜백. 취소, 닫기, 한도, 콜백 격리. 정적 상태 없음 |
| `decode/package-info` | 패키지 설명 |

시험(B2 뒤): `AudioBufferTest` 7, `decode/ResamplerTest` 17, `decode/AudioBufferDecoderTest` 20, `decode/AudioDecodeServiceTest` 19, `decode/TruncatedDataTest` 3, 도우미 `decode/AudioFiles`(스위트 아님). B2 의 넓은 경로 시험은 `tests/.../media/audio/wide/`(10.7). A1 의 `EngineStateTest` 의 어서션 하나를 바꾸었다(2.1).

### 2.1 `AudioBuffer` 결정

- `copyFromChannel/copyToChannel`: 규격 식대로 `bufferOffset >= length` 도 오류가 아니라 아무것도 복사하지 않는다(A1 의 기본형은 `IndexSizeError` 를 던졌다). A1 의 `EngineStateTest.testAudioBufferRules` 가 그 오류를 기대하고 있어서 **그 어서션 하나를 규격에 맞게 바꾸었다**(`offset at the length` → 아무것도 복사하지 않음). A1 의 나머지 어서션과 시그니처는 그대로이다.
- 정수 인수는 WebIDL `unsigned long` 값으로 본다. 음수인 Java 값은 2^31 이상인 값이다: 생성자와 `getChannelData` 의 채널에서는 범위 밖(`NotSupportedError`/`IndexSizeError`), `bufferOffset` 에서는 끝 뒤(복사 없음). JavaScript 계층은 `ToUint32` 한 값을 `(int)` 로 넘기면 된다.
- `wrap` 은 배열을 그대로 가져간다(디코딩 결과를 한 번 더 복사하지 않으려는 것). 채널 수 1~32, 길이가 모두 같고 1 이상, 표본 총수 `WebAudioLimits.MAX_BUFFER_SAMPLES` 이하, 표본율 3000~768000 을 생성자와 같은 규칙으로 검사한다.
- 규격의 "acquire the content"(재생 중 변경이 영향을 주는 시점)는 A1 의 5.3절 2번 그대로이다(재생 중인 소스 노드는 배열을 공유한다). 이번에 바꾸지 않았다.
- `AudioBufferOptions` 사전(`numberOfChannels` 기본 1, `length`/`sampleRate` 필수)의 기본값과 필수 검사(없으면 `TypeError`)는 JavaScript 계층의 일이다.

## 3. 디코딩 파이프라인의 결정

### 3.1 P18 디코더 재사용과 B2 이전의 한계(B2 에서 해소: 10절)

- 형식 판별은 P18 의 시그니처 판별(`AudioDecoders.openWide`, 1단계에서는 `open`)을 그대로 쓴다. MIME 타입은 힌트이며 틀려도 결과가 같다(`testTheMimeTypeIsOnlyAHint`: 모든 시험 자료를 올바른 힌트, 힌트 없음, 틀린 힌트로 디코딩해 같은 표본). 시험 자료 아홉 계열 전부(WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM)가 같은 경로이다.
- 1단계의 표본은 P18 의 `PcmSource`(16비트 인터리브, 채널 최대 둘)의 `표본 / 32768` 이었고 두 가지 한계가 있었다: (1) 채널이 둘보다 많은 파일을 디코더 안에서 `ChannelMixer` 로 스테레오로 다운믹스한다. (2) 24/32비트 정수, 부동 소수점, 손실 코덱의 float 를 디코더 안에서 16비트로 반올림한다. 둘 다 `media/audio/**` 의 디코더 출력 단계에 있어 1단계에서는 고치지 못했다.
- **B2 에서 해소**: 아홉 디코더 전부에 `FloatPcmSource` 를 내는 넓은 경로(`WideAudioDecoder.openWide`)를 더했고 `decodeAudioData` 의 파이프라인이 그것을 읽는다. 결과 버퍼는 **파일의 모든 채널을 표준 순서로, float 정밀도로** 가진다(10.2~10.5). 16비트 `PcmSource` 경로(재생)는 바뀌지 않았고 비트 단위로 같음을 확인했다(10.7). 1단계의 시험 `testMultichannelFilesAreMixedDownByTheMediaDecoders` 는 `testMultichannelFilesKeepEveryChannel` 로 바뀌었다.
- 파이프라인 자체는 소스가 주는 채널(최대 32)을 순서와 개수 그대로 유지한다(`testChannelsOfASourceAreKept`: 1, 2, 3, 4, 6, 8, 32 채널의 가짜 소스).

### 3.2 오류와 취소

- 디코딩을 시작할 수 없는 실패는 `EncodingError` 이며 `getCause()` 에 P18 의 `MediaException`(또는 디코더가 던진 런타임 예외)을 보존한다: 알 수 없는 형식, 지원하지 않는 변형(예: Yamaha ADPCM, WMA, MPEG Layer II, Apple Lossless 시험 자료 넷), 헤더 손상, **프레임을 하나도 디코딩하기 전의 손상**, 표본이 없는 파일, 변환할 수 없는 표본율, 한도 초과, 메모리 부족(`OutOfMemoryError` 를 이 좁은 자리에서만 잡는다), 채널 32개 초과. **(B2 에서 바뀜: 10절)** 1단계에서는 프레임을 디코딩한 뒤의 손상/잘림도 `EncodingError` 였으나 이제는 그 앞까지의 부분 결과와 경고이다(3.4).
- 디코더가 손상된 데이터 때문에 던지는 예상 밖의 런타임 예외(배열 인덱스 등)도 같은 규칙으로 처리한다: 첫 프레임 전이면 `EncodingError`, 이후면 부분 결과(`testFailuresInTheMiddleOfTheStream`, 가짜 소스가 읽는 도중 `ArrayIndexOutOfBoundsException` 을 던진다). 손상 사본 1089개(시험 자료 121개 x 손상 9종: 잘림, 가운데 바이트 변경, 0 블록, 앞부분 1/3/8 바이트 변경, 아무 곳 1/4/32 바이트 변경)를 디코딩한 결과는 **1단계(엄격)에서 703개 디코딩/386개 `EncodingError`, B2 의 관대한 정책에서 705개 디코딩(그중 2개는 잘림으로 표시)/384개 `EncodingError`** 이며 **그 밖의 예외는 두 경우 모두 0건**이다(`testDamagedFilesDecodeOrGiveAnEncodingErrorAndNothingElse`). 시험 자료가 0.1~0.4초 길이라 Ogg 가 한두 페이지뿐이어서 자르면 남는 것이 없는 사본이 대부분이라 2개만 늘었다. 긴 파일로는 3.4 의 표를 본다.
- 취소: `DecodeCancellation` 플래그 또는 스레드 인터럽트이며 블록(4096 프레임)마다 확인한다. 결과는 `CancellationException`(오류가 아니다). 5초 분량 파일에서 플래그가 선 뒤 질의 한두 번 안에 멈춘다(`testCancellation`).

### 3.3 한도(이름 있는 상수)

| 상수 | 값 | 뜻 |
| --- | --- | --- |
| `AudioBufferDecoder.BLOCK_FRAMES` | 4096 | 한 번에 디코딩하고 변환하는 프레임 수(취소 확인 간격) |
| `AudioBufferDecoder.MAX_INPUT_SAMPLES` | 4 x 2^25 | 변환 전 디코딩할 수 있는 표본 수(모든 채널 합). 표본율이 높은 파일을 허용되는 가장 큰 버퍼로 줄일 수 있게 한다 |
| 출력 한도 | `WebAudioLimits.MAX_BUFFER_SAMPLES`(2^25) | 모든 채널 합. 길이를 미리 알면 디코딩 전에 거부하고 모르면 자라는 도중에 거부한다(`ChannelSink`) |
| 입력 한도 | `DecodeLimits.DEFAULT` 64 MB | P18 의 한도를 그대로 쓴다 |
| `Resampler.MAX_RATIO` | 1024 | 두 표본율 비율의 한계(어느 방향이든). 넘으면 `EncodingError`(변환할 수 없는 표본율) |

한도 시험은 줄인 한도(`decode(..., DecodeLimits, long maxOutputSamples)` 의 인수)로 했다. **실제 64 MB 입력이나 2^25 표본 출력을 직접 만들어 보지는 않았다**(메모리 때문). 한도 검사 코드 경로는 줄인 한도로 같은 코드가 실행된다.

### 3.4 잘린 파일의 동작(확인한 사실과 선택. B2 에서 관대한 정책으로 바뀜: 10.6)

**현재의 정책(B2)**: 프레임을 하나 이상 디코딩한 뒤 손상이 드러나거나 파일이 잘려 있으면 **디코딩한 만큼을 성공으로 돌려주고** `DecodeResult.isTruncated()` 와 경고 문장으로 알린다. 프레임이 하나도 없거나(헤더 손상 포함), 지원하지 않는 변형이거나, 한도를 넘으면 `EncodingError` 이다. 형식별 결과 표는 10.6 이다. 아래는 1단계(엄격한 정책)에서 잘린 사본을 디코딩한 측정이며 정책이 바뀐 이유를 보여 주려고 남긴다(임시 측정 프로그램의 결과, 거부하는 4개를 뺀 시험 자료 117개, 60%/90%/99% 에서 자름):

| 형식 | 60% 에서 자름 | 90%, 99% 에서 자름 |
| --- | --- | --- |
| WAV, AIFF, AU | 모두 디코딩(남은 만큼) | 모두 디코딩 |
| MP3 | 15/16 디코딩 | 모두 디코딩 |
| AAC | 17/25 디코딩, 8 `EncodingError` | 같음 |
| WebM | 모두 디코딩 | 모두 디코딩 |
| FLAC | 0/15 디코딩 | 90%: 2/15, 99%: 3/15 디코딩 |
| Ogg Vorbis, Opus | 모두 `EncodingError` | 모두 `EncodingError` |

즉 1단계의 P18 디코더(16비트 경로를 `AudioBufferDecoder` 가 읽을 때)는 Ogg(Vorbis, Opus)와 FLAC 의 잘린 파일을 디코딩 실패로 보고했다. 그 시험 자료는 Ogg 가 한두 페이지뿐인 짧은 파일이라 자르면 첫 페이지도 남지 않는 경우가 많았다는 점에 유의한다(10.6 은 0.1초마다 페이지가 있는 2초 파일을 쓴다). 브리프가 "잘림 → EncodingError"라고 하여 1단계에서는 엄격하게 두었고, 조정자가 실제 브라우저의 동작에 맞는 관대한 정책(요청 5번)을 선택하여 B2 에서 바꾸었다.

## 4. 표본율 변환기(`Resampler`)

### 4.1 방식과 근거

- **윈도 sinc 다상 보간**: 출력 표본마다 그 위치 둘레의 입력 표본을 Kaiser 창을 씌운 sinc 저역 통과 커널로 가중해 더한다. 차단 주파수는 두 표본율 중 낮은 쪽 나이퀴스트의 95 %(전이 대역 90~100 %). 올릴 때는 이미지를, 내릴 때는 앨리어싱될 성분을 없앤다.
- 설계 정지 대역 감쇠 100 dB(디코더가 주는 16비트 표본의 해상도 -96 dB 보다 좋다). Kaiser 의 식 `N = (A - 7.95) / (14.36 * df)` 로 같은 표본율에서 **130 탭**이고 출력 표본율이 낮으면 커널을 그 비율만큼 늘린다(96000 에서 44100 은 280 탭, 768000 에서 3000 은 32822 탭). Kaiser 창을 고른 까닭: 정해진 감쇠를 가장 적은 탭으로 얻는 흔한 창이다.
- **위상 표**: 두 표본율이 정수이면 비율을 `up/down` 으로 약분하고 `up <= 1024` 이며 표가 24 MB 이하이면 **정확한 위상**(보간 오차 없음. 44100 에서 48000 은 160 위상, 8000 에서 48000 은 6, 22050 에서 48000 은 320)을 미리 계산한다. 아니면(`up` 이 크거나 표본율이 정수가 아님) **1024 행 표를 두 행 사이 선형 보간**한다(아주 넓은 커널은 행이 줄지만 보간 오차도 비율의 제곱으로 줄어든다). 각 위상은 DC 이득 정확히 1 이 되도록 정규화한다. 위치는 정수 표본율이면 정확한 정수 산술(`n * down / up`)이다.
- **길이**: `ceil(프레임 수 * 출력 표본율 / 입력 표본율)`. 근거: 출력 표본 `n` 의 시각은 `n / 출력 표본율` 이고 입력의 재생 시간 안에 드는 출력 표본을 모두 만든다. 재생 시간이 출력 표본 하나보다 작은 오차로 보존되고 입력 끝이 잘리지 않는다. 브리프가 `round` 를 예로 들었으나(식은 예시) `ceil` 을 골랐다. 두 식은 비율이 정수일 때(48000/8000 등)와 길이 x 비율이 정수일 때 같고, 그렇지 않을 때 최대 1 표본 차이이다. **Chrome 이 어느 식을 쓰는지는 확인하지 못했다**.
- 지연이 없다(필터가 출력 위치에 대칭이다). 첫 표본 앞과 마지막 표본 뒤의 입력은 0 으로 본다(양 끝은 유한한 신호의 필터 응답).
- **스트리밍**: 입력을 어떤 크기로 나누어 줘도 결과가 비트 단위로 같다(`testChunkingDoesNotChangeTheResult`). 커널 길이만큼의 창만 보관하므로 파일 전체를 float 로 메모리에 두지 않는다.

### 4.2 측정한 품질(JDK 6u45, 시험이 출력하는 값)

`ResamplerTest` 의 출력(`sh scripts/pkgtest.sh wb ... --filter ...decode.ResamplerTest`)이다. 표본율 쌍은 15개(44100↔48000, 22050/8000/11025/16000/32000→48000, 96000→44100, 44100→22050/96000, 3000→768000, 768000→3000, 44101→48000, 44100.5→48000, 48000→44100.25).

| 항목 | 목표(설계) | 측정 | 시험의 판정 |
| --- | --- | --- | --- |
| 통과 대역 이득(5~90 % 나이퀴스트) | 리플 약 1e-4 dB | 최대 **9.3e-5 dB** | 0.001 dB 미만 |
| 전이 대역 | 95 % 에서 -6.02 dB | 95 %: -6.02 dB, 97.5 %: -25.7 ~ -26.0 dB(프로그램 측정) | 95 %: -6.0206 +- 0.01 dB, 97.5 %: -20 dB 미만 |
| 정지 대역(앨리어싱될 톤을 내릴 때) | 100 dB | 가장 약한 것 **-100.8 dB**(나이퀴스트 102 %) | -96 dB 미만 |
| 올릴 때 이미지 + 앨리어싱 + 반올림 | 100 dB | 가장 큰 잔차 **-104.7 dB** | -96 dB 미만 |
| 사인 재현(진폭 0.5) | 약 5e-6 | 최대 오차 **5.14e-6**(약 -99.8 dB) | 1e-5 미만 |
| 보간하는 표 대 정확한 위상 | 약 -120 dB | 최대 차이 **8.3e-7**(풀 스케일 잡음) | 1e-6 미만 |
| DC 이득 | 정확히 1 | 모든 위상 | 2e-7 이내(float) |

- 통과 대역 이득은 주파수를 아는 정현파의 **최소 제곱 적합**으로 잰다. 정수 주기가 아닌 구간에 단일 구간 푸리에(Goertzel)를 쓰면 스펙트럼 누설이 이득 오차를 -60 ~ -75 dB 수준으로 부풀려서 처음 측정이 틀렸다(0.001 dB 이 나오는 값이 실제로는 약 3e-5 dB).
- 속도(JDK 6u45 server VM, 다른 작업이 도는 4코어 PC, 참고용): 모노 60초 분량 44100→48000 은 0.46초(약 130배속), 96000→44100 은 0.9~1.3초(47~64배속), 8000→48000 은 0.45초, 44101→48000(보간 표)은 0.7초. 시험은 한도를 20초로 두어 제곱에 비례하는 구현만 거른다.
- **변이 시험**(손으로 한 군데를 바꿔 시험이 실패하는지 확인하고 되돌렸다): (A) 입력 위치를 1 표본 어긋나게 → 5건 실패(청크 분할, DC, 임펄스 대칭, 사인, 짧은 입력), (B) 정지 대역 설계를 70 dB 로 → 5건 실패, (C) 위상 정규화 제거 → DC 시험 1건 실패. 디코더 파이프라인에서도 (M1) `/ 32768f` 를 `/ 32767f` 로 → 5건, (M2) 채널 교환 → 5건, (M3) 변환기 꼬리 비우기 제거 → 7건이 실패했다. 서비스에서는 (S1) 취소된 요청의 콜백 차단 제거 → 처음에는 통과(콜백 참조를 null 로 만들어 NPE 로 막혀 같은 결과였다)해서 시험에 "콜백 실패 0건" 어서션을 더해 잡게 했고, (S2) 닫을 때 요청 취소 제거 → 2건, (S3) 대기 요청 취소 때 카운터 반환 제거 → 4건이 실패했다.

### 4.3 ffmpeg 와의 참고 비교(합격 기준이 아니다)

합격 기준은 4.2 의 해석적 기대값이다. 같은 입력을 ffmpeg(`N-125875-g5d4d3bdc61-20260731`, libsoxr 와 swresample 포함)로 바꾼 결과와도 비교했다. 입력은 `aevalsrc=0.5*sin(2*PI*1000*t)+0.2*sin(2*PI*15000*t)` 2초(`-f f32le`, 모노), 출력은 `aresample=<표본율>:resampler=soxr:precision=28`(soxr VHQ) 또는 기본 `aresample`(swr)이다. 비교는 양 끝 400 표본을 뺀 구간이다(임시 측정 프로그램, 참조 파일은 저장소에 넣지 않았다. 위 명령으로 다시 만들 수 있다).

| 변환 | 길이(이 구현 / ffmpeg) | 해석식 대비 최대 오차: 이 구현 | ffmpeg | 둘의 최대 차이 |
| --- | --- | --- | --- | --- |
| 44100→48000 대 soxr VHQ | 96000 / 96000 | 1.3e-6 (-114.5 dB) | 1.5e-6 (-113.4 dB) | 2.5e-6 (-108.9 dB) |
| 44100→48000 대 swr 기본 | 96000 / 96000 | 1.3e-6 (-114.5 dB) | 9.6e-6 (-97.2 dB) | 8.8e-6 (-98.1 dB) |
| 44100→22050 대 soxr(15 kHz 톤은 사라져야 함) | 44100 / 44100 | 3.3e-7 (-126.5 dB) | 2.5e-8 (-148.9 dB) | 3.3e-7 (-126.6 dB) |
| 96000→44100 대 soxr | 88200 / 88200 | 1.4e-6 (-114.2 dB) | 4.5e-7 (-123.8 dB) | 1.8e-6 (-112.0 dB) |

dB 는 합성 신호의 최대 진폭 0.7 에 대한 비이다. 결과: **길이가 ffmpeg 와 같고(`ceil` 식과 같은 길이) 시간 정렬이 같으며(둘의 차이가 -108 dB 이하이므로 어긋남이 없다) 정확도는 ffmpeg 의 soxr 최고 품질과 같은 수준, 기본 swr 보다 약 17 dB 좋다.** 이 비교는 소프트웨어 하나(ffmpeg)와의 비교일 뿐 다른 브라우저(Chromium 의 `SincResampler`)와의 비교가 아니다.

## 5. 비동기 서비스(`AudioDecodeService`)

### 5.1 스레드와 수명

- 인스턴스마다 `ThreadPoolExecutor`(스레드 `DEFAULT_THREADS` = `min(2, 프로세서 수)`개, 큐는 무제한, `allowCoreThreadTimeOut(true)`, 유휴 `IDLE_MILLIS` 5초)를 가진다. 스레드는 필요할 때 만들고 일이 없으면 스스로 끝나므로 **쓰지 않거나 닫는 것을 잊은 서비스도 스레드를 쥐지 않는다**(`testIdleThreadsEndWithoutClosing`).
- 스레드는 데몬이며 이름이 `swingwebpanel-audio-decode-N` 이고 우선순위가 보통보다 하나 낮다(디코딩이 사용자 인터페이스와 경쟁하지 않게). 정적 상태가 없다(정적 executor 나 컴포넌트 참조 없음). P21 점검 `StaticStateAuditTest`, `ThreadCreationAuditTest` 와 제품 JAR 내용 점검(`JarContentTest`)을 새로 빌드한 결과로 직접 실행해 통과했다(7절).
- 소유자(컨텍스트)가 닫히면 `close()` 를 부른다: 모든 요청을 **콜백 없이** 취소(실행 중인 디코딩은 인터럽트되어 다음 블록에서 끝남), 새 요청은 `InvalidStateError`, 풀 종료. 반복 호출과 콜백 안에서의 호출이 안전하다(`testCloseInsideACallback`).

### 5.2 순서, 콜백, 한도

- **순서**: 스레드가 하나이면 요청 순서대로 디코딩하고 알린다(`testOneThreadKeepsTheOrder`: 긴 요청과 짧은 요청을 섞어도). 둘 이상이면 병렬이며 알리는 순서는 정하지 않는다. 근거: 규격이 "여러 디코딩 스레드가 병렬로 처리할 수 있다"고 하며 순서를 정하지 않고, 짧은 소리가 긴 음악을 기다릴 이유가 없다. 순서가 필요한 호출자는 `new AudioDecodeService(1)` 을 쓴다.
- 콜백은 정확히 한 번(취소되었으면 한 번도 안), 요청과 함께 준 `Executor` 에서(없으면 디코딩 스레드에서) 실행된다. 문서 큐로 옮기는 일은 호출자가 그 큐의 `Executor` 를 주어 한다(C). 실행기가 거부하면(예: 큐가 닫힘) 콜백은 버리고 요청은 끝난 것으로 한다. 콜백이 던진 예외는 버리고 `getCallbackFailureCount()` 로만 센다(다른 요청과 스레드에 영향 없음).
- **경고(B2)**: 디코딩이 부분 결과로 끝났으면 서비스는 콜백이 `DecodeWarningCallback` 일 때 같은 `Executor` 에서 `onWarning(String)` 을 **`onSuccess` 직전에** 부른다(같은 실행기 작업 안에서 순서대로). 일반 `DecodeCallback` 은 변하지 않으며 경고를 받지 않는다. `testTheWarningOfACutOffFileComesBeforeSuccess`.
- `DecodeRequest.cancel()`: 대기 중인 요청은 큐에서 빼고 카운터를 되돌리며, 실행 중인 요청은 인터럽트한다. **`true` 를 반환한 뒤에는 그 요청의 콜백이 시작되지 않는다**(결과가 이미 실행기에서 기다리고 있어도). 콜백이 이미 시작했으면 `false`. 무작위 취소 150개 시험(`testRandomCancellationKeepsTheBooks`)이 `true` 인 요청은 콜백 0회, 나머지는 정확히 1회, 끝난 뒤 서비스의 대기/활성 요청 0, 닫은 뒤 스레드 0 을 확인한다.
- 한도: `MAX_PENDING_REQUESTS` 64(디코딩이 끝나지 않은 요청), `MAX_PENDING_BYTES` 256 MB(그 데이터의 합). 넘으면 `decode()` 가 `NotSupportedError` 를 **동기로** 던진다(JavaScript 계층이 그것으로 Promise 를 거부). 서비스는 `byte[]` 를 넘겨받고(복사하지 않음) 디코딩이 끝나면 참조를 놓는다.
- 인수: `data`/`callback` 이 null 이면 `IllegalArgumentException`, 표본율이 3000~768000 밖이면 `NotSupportedError`. **빈 데이터는 동기 오류가 아니라 비동기 `EncodingError`**(규격: 디코딩 실패는 Promise 거부).

## 6. 확인하지 못한 것 / 한계 (B2 뒤의 최종 목록은 10.9)

- ~~24/32비트·float 정밀도와 채널이 셋 이상인 파일의 원래 채널~~ → B2 에서 해소(10절).
- ~~잘린 Ogg/FLAC 파일은 `EncodingError`~~ → B2 에서 부분 결과 + 경고로 바뀜(10.6).
- 실제 64 MB 입력, 2^25 표본 출력, 수 분짜리 실제 파일의 메모리/시간: 1단계에서는 직접 하지 않았다(줄인 한도로 같은 코드 경로를 시험). B2 에서 230초 24비트 스테레오 WAV(58 MB)로 작은 힙에서 디코딩해 보았다(10.8).
- 트랙이 여럿인 MP4/WebM 에서 첫 트랙만 디코딩하는지: 코드를 읽어 확인했을 뿐 시험 자료가 없다.
- Chromium 의 `decodeAudioData` 결과 길이(`ceil` 인지)와 리샘플러 방식은 확인하지 못했다.
- JRE 6u45 이외의 JRE(JDK 7/8/25 가 이 PC 에 있다)에서는 돌리지 않았다.
- 실시간 `AudioContext`, JavaScript 노출, Promise/ArrayBuffer 분리/문서 큐 전달은 이번 범위 밖(C).

## 7. 실행한 검증(JDK 6u45, 이 PC) — 1단계 시점의 결과. B2 뒤의 결과는 10.7

| 명령 | 결과 |
| --- | --- |
| `sh scripts/pkgtest.sh wb "media/webaudio media/audio" "media/webaudio media/audio/reference media/audio/testkit" --filter org.duckdns.hjow.swingwebpanel.media.webaudio` | **182 통과 / 0 실패 / 0 건너뜀**(34초). A1 의 121건 + `AudioBufferTest` 7 + `ResamplerTest` 17 + `AudioBufferDecoderTest` 19 + `AudioDecodeServiceTest` 18. `AudioDecodeServiceTest` 는 컴파일 한 번에 6번 되풀이 실행해도 모두 통과(2.1초 안팎) |
| `sh build.sh` | 제품 전체 컴파일과 JAR 3개 생성 성공(`javac 1.6.0_45`), `media/webaudio/decode` 클래스 12개가 JAR 에 들어감 |
| `build/classes` 에 대해 `StaticStateAuditTest`, `ThreadCreationAuditTest`, `JarContentTest` 를 `TestRunner --scan` 으로 직접 실행 | **3 통과 / 0 실패** (새 항목을 `StaticStateAuditTest` 에 추가할 필요 없었다) |
| `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/media/webaudio tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio/decode` | **0 위반**(인코딩, 금지 API, 영/한 Javadoc, 반복문 한국어 주석) |

**전체 회귀(`test.sh`)는 실행하지 않았다**(조정자 몫). `EngineStateTest` 의 어서션 하나와 `AudioBuffer` 복사 규칙이 바뀌었으나 `media/webaudio` 밖에서 `AudioBuffer` 를 쓰는 코드는 없다(`grep`: `BaseAudioContext.createBuffer`, `OfflineAudioContext` 의 `new AudioBuffer` 뿐).

## 8. C 단계(JavaScript 바인딩)를 위한 API 메모

```java
// 컨텍스트(소유자)마다 서비스 하나. 컨텍스트의 close()/페이지 이탈/컴포넌트 dispose 에서 service.close().
AudioDecodeService decoder = new AudioDecodeService();            // 순서가 필요하면 new AudioDecodeService(1)

// decodeAudioData(audioData, success, error) 의 제어 스레드 단계:
//   (JS) 문서가 fully active 가 아니면 InvalidStateError 로 거부된 promise 를 반환
//   (JS) audioData 가 이미 분리되었으면 DataCloneError 로 거부
//   (JS) byte[] bytes = ArrayBuffer 의 내용(복사해서 넘기고 ArrayBuffer 는 분리); bytes 는 서비스가 소유하므로 이후 건드리지 않는다
DecodeRequest req;
try {
    req = decoder.decode(bytes, /* mimeHint */ null, (float) context.getSampleRate(), new DecodeWarningCallback() {
        public void onWarning(String message) { /* (B2) 부분 결과일 때만, onSuccess 직전. 개발자 콘솔 경고: console.warn 으로 보내면 된다(P24 콘솔 이벤트) */ }
        public void onSuccess(AudioBuffer b) { /* promise resolve(wrapper(b)); successCallback 호출 */ }
        public void onError(AudioException e) { /* promise reject(DOMException "EncodingError"); errorCallback 호출 */ }
    }, documentQueueExecutor);                                   // 콜백을 문서 큐로 옮기는 Executor. 경고가 필요 없으면 DecodeCallback 으로 충분
} catch (AudioException e) {                                     // InvalidStateError(서비스가 닫힘) 또는 NotSupportedError(한도)
    /* promise reject(e) */
}
```

- `AudioDecodeService.decode` 는 어느 스레드에서나 부를 수 있다. `AudioBufferDecoder.decode(...)` 는 정적이고 상태가 없으며 호출 스레드에서 동기로 돈다(시험이나 즉시 필요한 경우. 렌더링/제어 스레드에서는 쓰지 말 것). 같은 일을 `DecodeResult`(버퍼, `isTruncated()`, `getWarning()`, `getCause()`)로 돌려주는 `decodeResult(...)` 도 있다.
- **(B2) 결과의 모양**: `AudioBuffer` 는 파일의 **모든 채널**(1~32)을 가진다. 채널 순서는 표준 순서(앞 왼쪽, 앞 오른쪽, 앞 중앙, LFE, 뒤 왼쪽, 뒤 오른쪽, 뒤 중앙, 옆 왼쪽, 옆 오른쪽. Web Audio 4.2절)이며 5.1 파일은 `[FL, FR, FC, LFE, BL, BR]` 이다(WAV/FLAC/FFmpeg 순서. 10.4). 값은 float 이다(24비트 정수는 `표본/2^23` 으로 정확하고 손실 코덱은 16비트 반올림 없는 자기 float 이며, 클리핑하지 않으므로 1을 넘는 값이 있을 수 있다). 표본율은 컨텍스트의 것이다. **C 단계는 채널 수에 대해 아무 가정도 하지 않아야 한다**(예: `createBuffer` 와 노드 그래프의 `channelCount` 규칙은 A1 이 32 채널까지 다룬다).
- **(B2) 잘림 알림**: Promise 는 부분 결과로도 resolve 하며(규격의 `EncodingError` 가 아니다), 경고를 개발자 콘솔에 남기는 것이 1.1 WD 의 권고이다. 호출자가 구분해야 하면 `DecodeWarningCallback`.
- **취소된 요청(`cancel()`, `close()`)은 콜백이 오지 않으므로 JavaScript 계층의 promise 는 스스로 정산하거나 버려야 한다**(규격의 `[[pending promises]]` 처리).
- `AudioBuffer`(디코딩 결과)는 스레드 안전하지 않다. 결과는 `Executor` 가 옮겨 준 스레드에서만 만지면 된다.
- 리샘플러는 다른 곳에서도 쓸 수 있다(예: HRTF 응답을 컨텍스트 표본율로): `Resampler.resample(float[] in, double inRate, double outRate)` 가 `ceil` 길이의 배열을 돌려주고, 스트리밍은 `new Resampler(in, out)`, `process(...)`, `finish(...)`, `maxOutputFrames(n)`, `maxFinishFrames()`, `outputLength(n)` 이다. 채널마다 인스턴스를 쓴다.

## 9. 체크박스 후보(체크는 조정자 몫)

- **P25-05: 미완료로 유지 권장.** 끝난 부분(순수 Java): `AudioBuffer` 의 생성 규칙/`getChannelData`/`copyFromChannel`/`copyToChannel`(규격 1.4), 자체 디코더를 재사용하는 `decodeAudioData` 파이프라인(전 형식, 시그니처 판별, `EncodingError` 매핑), 컨텍스트 표본율로의 리샘플링과 방식/품질 근거 기록(4절), 백그라운드 디코딩과 취소/닫기/한도/스레드 수명. 남은 부분: `createBuffer`/`AudioBuffer` 생성자/`decodeAudioData` 의 **JavaScript 노출**(Promise 와 콜백 형식, `ArrayBuffer` 분리, 문서 큐 전달) = C 단계. (1단계의 채널/정밀도 한계는 B2 로 해소되었고 손상 정책도 관대하게 바뀌었다: 10절.)
- P25-06~08: 시작하지 않음.

## 10. B2 단계: 넓은 디코딩 경로와 관대한 손상 정책 (커밋 148~154, 이 문서는 155)

조정자가 승인한 두 가지: (1) P18 디코더에 **원래 채널과 float 정밀도**를 내는 선택적 경로를 더하고 `decodeAudioData` 가 그것을 쓰게 한다(재생용 16비트 `PcmSource` 경로는 비트 단위로 그대로). (2) 잘린/손상된 파일은 **디코딩한 만큼을 성공으로** 돌려주고 호출자에게 알린다(`onSuccess(AudioBuffer)` 의 모양은 바꾸지 않는다).

### 10.1 커밋

| 번호 | 해시 | 내용 |
| --- | --- | --- |
| 148 | `232de7e` | 기반(`FloatPcmSource`, `WideAudioDecoder`, `BlockFloatSource`, `PcmFloatAdapter`, `AudioDecoders.openWide`, `ChannelMixer.canonicalOrder`)과 PCM 계열(WAV/AIFF/AU, G.711, IMA/MS ADPCM) |
| 149 | `6f278ce` | FLAC(네이티브와 Ogg FLAC, 전체 비트 깊이, 모든 채널)와 ffmpeg 채널별 참조 시험 |
| 150 | `bd62164` | Vorbis, Opus, WebM(Vorbis/Opus 트랙) |
| 151 | `3bf9788` | MP3 |
| 152 | `2641977` | AAC(ADTS, M4A) |
| 153 | `6aa5513` | 손실 코덱의 float 정밀도 시험(ffmpeg float 참조 10개) |
| 154 | `8010fad` | `decodeAudioData` 파이프라인을 넓은 경로로 전환, 관대한 손상 정책, `DecodeResult`/`DecodeWarningCallback`, 잘림 시험 자료 |
| 155 | 이 문서 | |

### 10.2 새 API (`media.audio`와 `media.webaudio.decode`)

| 이름 | 역할 |
| --- | --- |
| `FloatPcmSource` | 인터페이스. `getSampleRate()`, `getChannels()`(파일의 채널 수), `getFrameCount()`(-1 은 미상), `int read(float[][] planar, int offset, int frames)`(채널마다 배열 하나. 쓴 프레임 수, 끝이면 -1), `close()`. 손상이 드러나면 손상 앞까지의 프레임은 앞선 읽기가 전달하고 실패(`MediaException.Kind.CORRUPT`)는 그다음 읽기가 보고한다(`BlockPcmSource` 와 같은 의미) |
| `WideAudioDecoder extends AudioDecoder` | `FloatPcmSource openWide(byte[], DecodeLimits)`. **아홉 디코더 전부**(Wav, Aiff, Au, Flac, Mp3, Aac, Vorbis, Opus, Webm)가 구현한다. `open` 과 `openWide` 는 같은 데이터를 받아들이고 거부한다 |
| `AudioDecoders.openWide(data, mimeHint, limits)` | `open` 과 같은 선택(`choose`: 시그니처, MIME 는 힌트). 넓은 경로가 없는 디코더는 `PcmFloatAdapter`(16비트 경로를 감쌈, 채널 최대 둘) |
| `BlockFloatSource` | 블록 디코더를 `FloatPcmSource` 로 만드는 추상 기반. `fill(float[][])` 가 블록 하나를 채운다. 요청 크기와 블록 경계가 무관하다 |
| `PcmFloatAdapter` | 16비트 `PcmSource` 를 `표본/32768` 의 float 로 감싼다(폴백) |
| `ChannelMixer.canonicalOrder()` | 역할(FL FR FC LFE BL BR BC SL SR)로 정렬한 입력 채널 순서. 역할을 모르거나 겹치면 파일 순서를 유지한다(10.4) |
| `RawFloatSource`, `AdpcmFloatSource`, `PcmSources`(`pcm`), `FlacFloatSource`, `VorbisFloatSource`, `OpusFloatSource`, `Mp3FloatSource`, `AacFloatSource` | 형식별 넓은 경로. 모두 **16비트 경로와 같은 디코딩 코어를 쓴다**: 기존 소스에 걸이(hook) 메서드를 더해(`FlacPcmSource.nextFrame()`, `VorbisSource.nextBlock()`/`blockOutput()`, `OpusSource.nextBlock()`/`blockFrom()`, `Mp3Source.decodeBlock()`/`sampleOf()`, `AacFrameDecoder.decodeWide`, `AdpcmSource.decodeNextBlock()`, `RawPcmSource.toFloat`) 믹서와 16비트 변환 직전의 값을 꺼낸다. 그래서 두 경로가 같은 스트림을 받아들이고 서로 비트 단위로 일치한다 |
| `DecodeResult`(`decode`) | `getBuffer()`, `isTruncated()`, `getWarning()`(없으면 null), `getCause()`(손상을 알린 예외 또는 null) |
| `DecodeWarningCallback extends DecodeCallback` | `onWarning(String)`. 서비스가 `onSuccess` 직전에 같은 실행기 작업에서 부른다(부분 결과일 때만) |
| `AudioBufferDecoder.decodeResult(...)` | `DecodeResult` 를 반환(인수는 `decode` 와 같다). `decode(...)` 는 버퍼만 반환 |

### 10.3 표본 값(정밀도)

| 형식 | 값 | 확인 |
| --- | --- | --- |
| WAV/AIFF/AU 정수 8/16/24/32비트, FLAC 4~32비트 | `(float) (표본 / 2^(비트-1))`. 24비트 이하는 정확하고 32비트는 몫에 가장 가까운 float(`2147483647 / 2^31` 은 1.0f) | `WidePcmTest`, `WideFlacTest`: 정답을 시험이 직접 만들고 비트 단위로 같음(극단값 포함, 채널 1/2/3/6/8, 리틀/빅 엔디언, 모든 읽기 크기) |
| WAV/AIFF/AU float 32/64비트 | 파일의 값(64비트는 float 로 반올림) | `WidePcmTest.testFloatWav` 등 |
| G.711 µ-law/A-law | 디코딩한 선형 16비트 값 / 32768 | `testCompandedSamples`(끝점 값) |
| IMA/MS ADPCM | 디코더의 16비트 값 / 32768(ADPCM 자체가 16비트 출력) | `WideEqualsNarrowTest` 의 `wav_ima_*`, `wav_ms_*` 시험 자료(16비트 경로와 비트 단위로 같음). 전용 정답 시험은 없다 |
| MP3, AAC, Vorbis, Opus | 디코더의 float(16비트 반올림도 클리핑도 없다) | 10.5 |

### 10.4 채널 순서 조사와 결정

**결정**: `AudioBuffer` 의 채널은 **WAVEFORMATEXTENSIBLE 마스크 순서**(= FFmpeg 의 고유 순서 = Web Audio 4.2절의 스피커 순서)로 둔다: 앞 왼쪽, 앞 오른쪽, 앞 중앙, LFE, 뒤 왼쪽, 뒤 오른쪽, (앞 왼쪽/오른쪽 중앙), 뒤 중앙, 옆 왼쪽, 옆 오른쪽. 형식이 스피커를 정의하는 곳은 그 순서로 바꾸고, 정의하지 않는 곳(AIFF, AU, 마스크 없는 WAV 의 6채널 이상, 채널 9개 이상, 알 수 없는 AAC 구성, Opus 매핑 군 2/3/255)은 파일의 순서를 유지한다. 모노와 스테레오는 바꾸지 않는다.

근거: Web Audio 1.0 REC 4.2절은 "Implementations MUST present the channels provided in the order defined below, skipping over those channels not present" 라고 하고 순서를 `0 FRONT_LEFT, 1 FRONT_RIGHT, 2 FRONT_CENTER, 3 LOW_FREQUENCY, 4 BACK_LEFT, 5 BACK_RIGHT, ...(6 FRONT_LEFT_OF_CENTER, 7 FRONT_RIGHT_OF_CENTER, 8 BACK_CENTER, 9 SIDE_LEFT, 10 SIDE_RIGHT, ...)` 로 정한다(이 세션에서 https://www.w3.org/TR/2021/REC-webaudio-20210617/ 본문을 받아 읽음). 같은 순서이므로 노드 그래프의 `channelInterpretation: "speakers"` 업/다운믹스(A1 의 `ChannelMixing`)가 그대로 맞는다. 다른 형식 규격(아래 표의 "부호화 순서")은 **이번에 다시 내려받아 읽지 않았고** 구현이 따르는 알려진 정의와 코드의 역할 표(`ChannelMixer.forVorbisOrder`, `AacFrameDecoder.rolesOf`)를 따랐으며, 결정적인 확인은 **ffmpeg 가 채널별로 만든 참조와의 비교 시험**이다.

| 형식 | 부호화 순서 | 처리 |
| --- | --- | --- |
| WAV | `WAVEFORMATEXTENSIBLE` 마스크 순서(마스크가 없는 6/8채널도 같다고 본다) | 그대로 |
| AIFF, AU | 스피커를 정의하지 않음 | 파일의 순서(ffmpeg 도 그대로 둔다. 5.1 AIFF 가 ffmpeg 참조와 같음) |
| FLAC | 1~8채널을 마스크 순서로 정의 | 그대로 |
| Vorbis(WebM 안의 Vorbis 포함) | 3ch: FL FC FR. 5.0: FL FC FR BL BR. 5.1: FL FC FR BL BR LFE. 6.1: FL FC FR SL SR BC LFE. 7.1: FL FC FR SL SR BL BR LFE | 역할로 정렬. 예: 5.1 → FL FR FC LFE BL BR, 7.1 → FL FR FC LFE BL BR SL SR |
| Opus 매핑 군 1 | Vorbis 와 같은 순서(1~8채널) | Vorbis 와 같게 정렬. 군 0(모노/스테레오), 2/3(앰비소닉), 255(개별 채널)는 스트림의 순서. 연결된(chained) 스트림이 서로 다른 배치이면 `PcmFloatAdapter`(16비트 경로)로 폴백 |
| AAC | 채널 구성 3: C L R. 4: C L R BC. 5: C L R BL BR. 6: C L R BL BR LFE. 7/12: C L R SL SR BL BR LFE. 11: C L R SL SR BC LFE | 역할로 정렬. 그 밖의 구성은 요소 순서를 유지 |
| MP3 | 채널 최대 둘(MPEG 1/2 Layer III) | 해당 없음 |

`ffmpeg` 참조와의 채널별 비교(`WideReferenceTest`, 14개 다채널 시험 자료: WAV 3/5.1/7.1ch, AIFF 5.1, FLAC 5.1/7.1(24비트), Vorbis 3/5.1/7.1, Opus 5.1, AAC 3/4/6/8ch): **정확한 형식은 최대 차이 0, 손실 코덱은 표본 단위(16비트 LSB)로 최대 0~1, rms 0.18 LSB 이하**이다. 채널 순서를 항등으로 바꾸는 변이를 넣으면(Vorbis, AAC, Opus 각각) 시험이 실패한다(10.7). **검증하지 못한 순서**: Vorbis/Opus 4ch(quad)와 5.0, 6.1(7채널)은 시험 자료가 없고 역할 표만 따랐으며 AAC 구성 4/11/12 중 구성 4 만 참조(4ch)가 있다.

### 10.5 정밀도 측정(손실 코덱)

`WideAccuracyTest`: 같은 파일을 ffmpeg 가 float 로 디코딩한 참조(`tests/resources/media/audio/wide/*.f32`, 1500 프레임씩 10개, `generation/make_wide_refs.sh`)와 비교한다. 16비트 경로는 `표본/32768`(반올림된 값)이다.

| 파일 | 넓은 경로 rms / 최대 | 16비트 경로 rms | 개선 배수 |
| --- | --- | --- | --- |
| MP3 CBR 128k 44100 | 2.108e-07 / 8.345e-07 | 8.782e-06 | 41.7배 |
| MP3 MPEG-2 32000 모노 | 4.204e-07 / 3.010e-06 | 8.693e-06 | 20.7배 |
| AAC ADTS 44100 스테레오 | 2.119e-08 / 8.941e-08 | 8.829e-06 | 416.6배 |
| AAC M4A 48000 모노 | 3.398e-08 / 1.192e-07 | 8.875e-06 | 261.2배 |
| Vorbis q6 WebM | 6.405e-08 / 2.086e-07 | 8.790e-06 | 137.2배 |
| Opus 128k 스테레오 | 9.435e-06 / 2.122e-05 | 9.715e-06 | **1.03배** |
| Opus VoIP 40k 모노 | 9.715e-06 / 2.193e-05 | 1.045e-05 | **1.08배** |
| AAC 6채널 | 1.050e-08 / 5.960e-08 | | |
| AAC 8채널 | 1.403e-08 / 1.192e-07 | | |
| Opus 5.1 | 8.668e-06 / 2.018e-05 | | |

해석:
- **넓은 경로를 16비트로 줄이면 16비트 경로와 비트 단위로 같다**(≤2채널 전 시험 자료, 손실 코덱 포함. `WideEqualsNarrowTest`). 따라서 넓은 경로의 float 와 16비트 경로의 표본의 차이는 반올림 한 단계 이내(1/65536)이며 클리핑 구간 밖이 아니면 1/32768 이내이다.
- MP3/AAC/Vorbis 는 float 경로가 ffmpeg 의 float 에 16비트 경로보다 **20~420배** 가깝다(양자화 잡음이 없어진다).
- **Opus 는 개선이 거의 없다(1.03~1.08배)**: 이 구현의 Opus 디코더가 libopus 의 float 출력과 약 0.3 LSB(rms 약 9e-6, 최대 2.2e-5) 다르기 때문이다. P18 의 Opus 디코더 자체의 한계이며(RFC 6716 의 허용 오차 안에서의 차이로 보이나 이 작업에서 그 원인을 규명하지는 않았다) B2 가 바꾸지 않았다.
- 모든 최대 차이는 2.2e-5(약 0.7 LSB) 미만이다.

### 10.6 관대한 손상 정책과 잘림 시험

**정책**(`AudioBufferDecoder`): 읽기 도중 `CORRUPT`(또는 디코더의 런타임 예외)가 나오면 **이미 디코딩한 프레임이 하나 이상일 때** 그 앞까지를 결과로 삼고 `DecodeResult.truncated` 로 표시하며 경고를 만든다. 프레임이 하나도 없으면(헤더 손상 포함) `EncodingError`. 파일이 약속한 길이(`getFrameCount()`)보다 적은 프레임을 주고 끝난 경우도 잘림이다(경고 `...cut off: N of the M frames...`). 디코더가 만든 유한하지 않은 표본(NaN/Infinity)은 0 으로 바꾸고 경고에 개수를 적는다(필터와 그래프가 오염되지 않게 하려는 선택이며 규격에는 없다). 지원하지 않는 변형, 표본이 없는 파일, 한도 초과, 채널 32개 초과는 그대로 `EncodingError` 이다.

**어느 잘림이 표시되는가**: 디코더가 손상을 감지하거나 파일이 약속한 길이(`getFrameCount()`)보다 적게 주고 끝날 때만 `truncated` 가 선다. 시험에서 확인한 것은 FLAC 네이티브(`STREAMINFO` 의 전체 표본 수)이다. 그 밖의 형식은 자른 뒤의 파일이 그냥 짧은 정상 파일과 구별되지 않는 경우가 많아 **표시 없이 짧게 디코딩**된다: MP3, ADTS AAC, Ogg Vorbis/Opus, Ogg FLAC(전체 길이를 알리지 않음)은 위 표에서 그렇고, WAV 는 코드를 읽어 확인했다(`WavDecoder` 가 `data` 청크의 선언 크기가 파일보다 크면 남은 길이로 줄인다. 시험으로 잘린 WAV 의 표시 여부를 확인하지는 않았다). AIFF/AU 는 확인하지 않았다. 시험이 그 차이를 그대로 적는다.

2초(`trunc/long_*`, Ogg 는 0.1초마다 페이지) 파일을 70/85/95/99 % 위치에서 자른 결과(`TruncatedDataTest`가 출력, 디코딩한 프레임 수 / `T`는 `truncated` 표시):

| 형식(전체 프레임) | 70 % | 85 % | 95 % | 99 % |
| --- | --- | --- | --- | --- |
| Vorbis (88200) | 50752 | 71232 | 81472 | 86592 |
| Opus (96000) | 66888 | 81288 | 90888 | 95688 |
| FLAC 네이티브 (16000) | 9860 T | 12920 T | 14960 T | 15810 T |
| Ogg FLAC (16000) | 11050 | 12750 | 14450 | 15300 |
| MP3 (88200) | 60552 | 74376 | 83592 | 87048 |
| AAC (90112) | 64512 | 77824 | 87040 | 89088 |

가운데 300바이트 블록을 손상시킨 사본(`testADamagedBlockInTheMiddle`):

| 파일 | 디코딩한 프레임 / 전체 | 표시 | 첫 차이가 나는 프레임 |
| --- | --- | --- | --- |
| Vorbis | 83520 / 88200 | T | 25152 |
| Opus | 91848 / 96000 | | 47688 |
| FLAC 네이티브 | 5950 / 16000 | T | 5950 |
| Ogg FLAC | 15150 / 16000 | T | 7650 |
| MP3 | 87048 / 88200 | | 43824 |
| AAC | 89088 / 90112 | | 46080 |

시험이 확인하는 것: 잘린 사본의 앞부분 표본이 온전한 파일의 앞부분과 같고(손상 사본에서는 손상 앞부분이 같고 첫 차이가 손상 위치 이후), 디코딩한 프레임 수가 잘라낸 위치에 따라 증가(단조)하며, FLAC 네이티브 잘림은 표시되고, 헤더 안쪽을 자르면 여전히 `EncodingError`(`testCutInsideTheHeadersIsStillAnError`). 서비스는 부분 결과에서 `onWarning` 을 `onSuccess` 앞에 부른다.

짧은 시험 자료 117개(0.1~0.4초) x 손상 9종 = 1089개 사본: 705개 디코딩(2개 잘림 표시), 384개 `EncodingError`, 그 밖의 예외 0건. 이 자료는 Ogg 가 한두 페이지라 대부분 남는 것이 없다(그래서 긴 자료를 따로 만들었다).

### 10.7 실행한 검증(JDK 6u45, 이 PC, `JAVA6_HOME` 기본값)

| 명령 | 결과 |
| --- | --- |
| `sh scripts/pkgtest.sh ma "media" "media" --filter org.duckdns.hjow.swingwebpanel.media` (media 전체, 마지막 소스 상태) | **681 통과 / 0 실패 / 14 건너뜀**(207초). 건너뜀 14건은 내가 건드리지 않은 시험들(`OpusVectorTest`, `Woff2Test`, `ImageDecodersTest`, `SvgReferenceTest`, `JavaSoundDeviceTest`, `MediaPlayerTest` 가 선택적 자료/장치에 따라 건너뛴다)이며 이 PC 에서의 개별 이유는 확인하지 않았다. 재생용 16비트 경로의 기존 P18 시험(ffmpeg 참조와의 비트 단위 비교 `ReferenceDecodeTest` 포함)이 그대로 통과한다 |
| `... --filter org.duckdns.hjow.swingwebpanel.media.webaudio` | **187 통과 / 0 실패**(32초): A1/A2 의 노드 시험 + `AudioBufferTest` 7, `ResamplerTest` 17, `AudioBufferDecoderTest` 20, `AudioDecodeServiceTest` 19, `TruncatedDataTest` 3 |
| `... --filter org.duckdns.hjow.swingwebpanel.media.audio.wide` | **13 통과 / 0 실패**(2초): `WidePcmTest` 6, `WideFlacTest` 3, `WideEqualsNarrowTest` 2, `WideReferenceTest` 1, `WideAccuracyTest` 1 |
| `sh build.sh` | 성공(`javac 1.6.0_45`), JAR 3개 |
| `build/classes` 에 대해 `StaticStateAuditTest`, `ThreadCreationAuditTest`, `JarContentTest` (`TestRunner --scan`) | **3 통과 / 0 실패** |
| `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/media tests/src/org/duckdns/hjow/swingwebpanel/media` | **0 위반**(373 파일) |

중간 상태(디코더마다 커밋하기 전)에서도 `media/audio` 와 `media/webaudio` 시험을 돌려 통과를 확인했다. 당시 출력에서 옮긴 값(범위가 실행마다 달라 서로 비교할 수 없다): 첫 실행 181 통과/12 건너뜀, PCM 뒤 189, FLAC 뒤 675 통과, 디코더가 모두 들어가고 파이프라인을 바꾸기 전의 media 전체 676 통과/0 실패/14 건너뜀(205초).

**변이 시험**(손으로 한 군데를 바꾸어 시험이 실패하는지 확인하고 되돌렸다. 되돌린 뒤 `git status` 로 의도하지 않은 변경이 없음을 확인):

| 변이 | 실패한 시험 |
| --- | --- |
| M1 Vorbis 채널 순서를 항등으로 | `WideReferenceTest` 1건 실패(14개 문제) |
| M2 AAC 채널 순서를 항등으로 | `WideReferenceTest` 1건 실패(20개 문제) |
| M3 FLAC 나누는 수를 `2^bps` 로 | `WideFlacTest` 3건 전부 실패 |
| M4 Opus 매핑 군 1 의 순서 변환 제거 | `WideReferenceTest` 1건 실패(5개 문제) |
| M5 관대한 정책을 끔(손상은 다시 오류) | `AudioBufferDecoderTest.testFailuresInTheMiddleOfTheStream`, `TruncatedDataTest.testADamagedBlockInTheMiddle` 실패 |
| M6 유한하지 않은 표본을 바꾸지 않음 | `AudioBufferDecoderTest.testNonFiniteSamplesAreReplacedBySilence`, `testDamagedFilesDecodeOrGiveAnEncodingErrorAndNothingElse`(9개 사본이 다른 결과) 실패 |
| M7 약속한 길이보다 짧을 때의 표시를 끔 | `AudioBufferDecoderTest.testSourcesOfUnknownOrWrongLength`, `AudioDecodeServiceTest.testTheWarningOfACutOffFileComesBeforeSuccess`, `TruncatedDataTest.testCutOffFilesGiveTheirFirstFrames` 실패 |

M6 과 M7 은 같은 소스 파일을 바꾸므로 실행이 겹치지 않았는지 확인했다(두 결과의 실패 집합이 각자의 변이에만 해당하고 파일은 원래대로 복구됨). 1단계에서 하나 남은 사실: M5 에서 `TruncatedDataTest.testCutOffFilesGiveTheirFirstFrames` 는 통과한다(그 시험의 자른 사본은 `CORRUPT` 가 아니라 길이 부족으로 끝나기 때문).

### 10.8 블록 스트리밍과 메모리

넓은 경로는 파일 전체를 float 로 메모리에 두지 않는다: 소스는 블록(형식별로 FLAC 프레임, Vorbis/Opus 패킷, MP3/AAC 프레임, 비압축 PCM 은 2048 프레임, ADPCM 은 ADPCM 블록) 하나를 `BlockFloatSource` 의 블록 버퍼에 디코딩하고, 파이프라인은 4096 프레임 덩어리로 변환기와 `ChannelSink` 에 넘긴다. 읽기 크기와 블록 경계가 결과에 영향을 주지 않는지는 `WidePcmTest.testReadSizesDoNotMatter` 와 `WideFlacTest.testBlockSizesAndStrategies` 가 확인한다.

직접 측정(이 PC, JDK 6u45, 저장소에 넣지 않은 일회용 프로그램): **230초 44.1 kHz 24비트 스테레오 WAV(입력 58 MB, 출력 float 77 MB, 약 1014만 프레임)를 `AudioBufferDecoder.decodeResult` 로 디코딩**했다. `-Xmx170m`(실제 최대 힙 162 MB), `-Xmx200m`, `-Xmx300m` 에서 성공했고(238~250 ms) `-Xmx140m`, `-Xmx150m` 에서는 출력 버퍼 할당(`ChannelSink`)에서 메모리 부족(`EncodingError` 로 보고)이 났다. 입력 58 MB + 출력 77 MB = 135 MB 에 대해 162 MB 의 힙으로 충분했으므로 파일 전체 크기의 중간 float 사본(그것이 있다면 77 MB 가 더 필요)은 없다. 한계: 이것은 한 번의 측정이며 힙의 세대 배치에 따라 임계값이 달라지고, 이 크기보다 큰 입력(64 MB 한도 근처)과 다른 형식으로는 하지 않았다.

### 10.9 확인하지 못한 것 / 한계 (B2 뒤의 최종 목록)

- 형식 규격(Vorbis I, RFC 7845, FLAC, ISO/IEC 14496-3, WAVEFORMATEXTENSIBLE)은 이번에 다시 읽지 않았다(10.4). Web Audio 4.2절만 읽었다.
- 시험 자료가 없는 채널 배치: Vorbis/Opus 4ch·5.0·6.1, AAC 구성 11/12, 채널이 둘보다 많은 ADPCM(시험 자료는 모노/스테레오뿐). WAV 는 마스크를 읽어 순서를 바꾸지 않고 파일 순서(= 마스크 비트 순서)를 그대로 쓴다. 마스크가 없는 6/8채널 파일이 실제로 5.1/7.1 순서인지는 규약일 뿐 파일마다 확인할 수 없다.
- 트랙이 여럿인 MP4/WebM 에서 첫 트랙만 디코딩하는지: 코드를 읽어 확인했을 뿐 시험 자료가 없다(1단계와 같음).
- Opus 의 float 정밀도 개선이 거의 없다(10.5, 디코더의 libopus 와의 0.3 LSB 차이).
- 실제 64 MB 입력과 2^25 표본 출력에 가까운 크기는 하지 않았다(10.8의 측정이 가장 큰 것). 다른 형식의 장시간 파일의 시간/메모리도 하지 않았다.
- Chromium 이 같은 잘림/손상 파일을 부분 결과로 디코딩하는지, 어떤 채널 순서와 길이(`ceil`)를 쓰는지는 확인하지 못했다(관대한 정책은 1.1 WD 의 비규범 주석과 조정자의 결정에 근거한다).
- 표시 없이 짧게 디코딩되는 잘림(MP3, ADTS AAC, Ogg, 코드상 WAV)을 호출자가 구별할 수 없다(10.6). 개발자 도구 경고가 서지 않는다.
- 규격(1.0 REC)은 손상 시 `EncodingError` 이다. 부분 결과 정책은 규격에서의 이탈이며 C 단계가 Promise 를 resolve 한다.
- JRE 6u45 이외의 JRE(JDK 7/8/25)에서는 돌리지 않았다. 실제 오디오 장치로 듣는 확인은 이 단계 범위 밖이다(디코딩은 순수 PCM 값 비교).

## 조정자 요청

0. **(B2 로 해소됨) 아래 4번과 5번은 조정자의 승인과 결정에 따라 B2(커밋 148~154)로 처리했다.** 4번의 제안과 달리 파이프라인의 변경은 `AudioBufferDecoder.decodeFrom` 한 곳이 아니라 `decodeResult` 의 반환 모양(`DecodeResult`)과 서비스의 `onWarning` 도 포함한다.

1. **FEATURE_MATRIX 의 WAU-07 행 교체 초안(B2 반영본)** — 기존 행("(B) / 부분 구현 / A1 은 노드가 쓰는 자료형만")과 1단계 초안을 대체한다:

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-07 | `AudioBuffer`(생성 규칙, `getChannelData`, `copyFromChannel`/`copyToChannel`), `decodeAudioData` 의 순수 Java 부분: 자체 디코더를 **파일의 모든 채널을 표준 순서로, float 정밀도로** 읽는 파이프라인(시그니처 판별, 첫 프레임 전의 실패는 `EncodingError`), 손상/잘림은 디코딩한 만큼의 부분 결과와 경고(`DecodeResult`, `DecodeWarningCallback`), 컨텍스트 표본율로의 리샘플링(Kaiser 창 sinc 다상, 정지 대역 100 dB), 백그라운드 디코딩 서비스(취소, 닫기, 한도) | Web Audio API 1.0 REC(2021-06-17) 1.1.2, 1.4, 4.2(채널 순서). 1.1 WD(2026-09-22)의 첫 트랙만 디코딩과 손상 경고 주석 | `media.webaudio.AudioBuffer`, `media.webaudio.decode.{AudioBufferDecoder, DecodeResult, DecodeWarningCallback, Resampler, AudioDecodeService, DecodeCallback, DecodeRequest, DecodeCancellation, ChannelSink}`, `media.audio.{FloatPcmSource, WideAudioDecoder, BlockFloatSource, PcmFloatAdapter, AudioDecoders.openWide, ChannelMixer.canonicalOrder}`와 형식별 `*FloatSource` | `AudioBufferTest` 7, `ResamplerTest` 17, `AudioBufferDecoderTest` 20(P18 시험 자료 전부, 손상 사본 1089개), `AudioDecodeServiceTest` 19, `TruncatedDataTest` 3, `audio.wide.{WidePcmTest 6, WideFlacTest 3, WideEqualsNarrowTest 2, WideReferenceTest 1(ffmpeg 채널별 참조 14개), WideAccuracyTest 1(ffmpeg float 참조 10개)}` | 부분 구현(순수 Java 검증 완료, JavaScript 노출은 C) | 규격(1.0 REC)은 손상 시 `EncodingError` 이나 이 구현은 첫 프레임 뒤의 손상을 부분 결과로 돌려준다(1.1 WD 주석, 실제 브라우저 동작에 맞춤). 표시 없이 짧게 디코딩되는 잘림이 있다(MP3, ADTS AAC, Ogg, 코드상 WAV). 채널 순서는 WAV/FFmpeg/Web Audio 4.2 순서이며 Vorbis 4ch/5.0/6.1, Opus 매핑 군 2/3/255, AAC 구성 11/12 는 참조 시험이 없다. Opus 의 float 개선이 작다(디코더가 libopus 와 0.3 LSB 다름). 출력 길이는 `ceil`. Chromium 과의 비교는 하지 않았다 |

2. **ARCHITECTURE.md**: 패키지 표에 `media.webaudio.decode`(디코딩 파이프라인, 표본율 변환기, 백그라운드 서비스)를 추가하고 스레드 모델에 "`AudioDecodeService`: 인스턴스마다 데몬 스레드 풀(이름 `swingwebpanel-audio-decode-N`, 유휴 5초 뒤 종료, 정적 상태 없음), 콜백은 호출자가 준 `Executor`" 한 줄을 넣는다. 도구 목록은 바뀌지 않았다(`tools/webfetch`, `tools/sourcecheck` 만 사용).
3. **A1 파일을 한 군데 고쳤다**: `tests/.../media/webaudio/EngineStateTest.java` 의 어서션 하나(`copyFromChannel` 에서 `bufferOffset` 이 길이와 같을 때 `IndexSizeError` 기대)를 규격 1.4.3 에 맞게 "아무것도 복사하지 않음"으로 바꾸었다(2.1절). A1 의 `AudioBuffer` 구현도 같은 이유로 바뀌었다. A2 가 이 파일을 건드리지 않기로 되어 있어 충돌은 없을 것이다.
4. **(승인됨, 처리 완료: B2, 10절) P18 디코더에 원래 채널과 float 정밀도를 내는 선택적 경로 추가.**(1단계의 요청문 그대로 남긴다) 3.1절의 두 한계는 `media/audio/**` 에 있다. 제안: `media.audio` 에 `FloatPcmSource`(`getSampleRate/getChannels/getFrameCount`, `int read(float[][] planar, int offset, int frames)`, `close`)와 선택적 `AudioDecoder` 확장(`openWide(byte[], DecodeLimits)`)을 두고, 각 디코더(`RawPcmSource`/WAV/AIFF/AU, `AdpcmSource`, `FlacPcmSource`, `VorbisSource`, `Mp3Source`, `AacFrameDecoder`, `OpusSource`, WebM 의 래퍼)가 믹서와 16비트 변환 직전의 float 를 그대로 내보내게 한다(기존 `PcmSource` 경로는 그대로). `AudioDecoders.openWide` 는 넓은 경로가 없는 디코더를 16비트 경로로 감싼다. 이 작업의 파이프라인은 `AudioBufferDecoder.decodeFrom` 한 곳만 `FloatPcmSource` 를 읽도록 바꾸면 된다(바이트 → float 변환 루프를 대체). 검증은 기존 시험 자료로 가능하다: 무손실 ≤2채널은 새 경로를 16비트로 줄인 것이 기존 경로와 비트 단위로 같아야 하고, 채널이 3개 이상인 파일은 `ref/*.N.flac`(채널마다 ffmpeg 의 참조)과 비교한다. 규모는 파일 약 9개에 각 수십 줄과 시험이며 한 작업자가 한 세션에서 할 수 있다고 본다(실제 해 보지는 않은 추정). 이번 작업의 소유 경로 밖이어서 하지 않았다.
5. **(결정됨: 관대한 정책, 처리 완료: B2, 10.6) 잘린 파일 정책**(3.4절. 1단계의 요청문 그대로 남긴다): 현재 엄격(`EncodingError`, 브리프와 1.0 REC 에 충실). 대안은 "디코딩한 만큼 돌려준다"이다(1.1 WD 의 비규범 주석과 실제 브라우저의 동작에 가깝다). 구현은 `AudioBufferDecoder.decodeFrom` 에서 `MediaException.Kind.CORRUPT` 를 받았을 때 이미 모은 프레임이 있으면 거기까지로 끝내는 한 군데 변경과 시험 갱신이다. 정하면 알려 달라.
6. `docs/PROGRESS.md` 에 이 단계의 결과(**B2 뒤의 10.7절의 명령과 결과, 한계 10.9절**; 1단계는 7절과 6절)를 옮겨 달라. 이 단계는 `scripts/**` 변경 요청이 없다. 감사 시험을 `sh scripts/pkgtest.sh` 로 돌리면 제품 전체를 소스패스로 컴파일하므로 느려서, 이번에는 `sh build.sh` 뒤 `build/classes` 에 대해 `javac` 와 `TestRunner --scan` 으로 따로 돌렸다(7절). 이 방식을 스크립트로 두고 싶으면 알려 달라.
7. **`docs/progress/media-matrix.md` 의 오디오 절을 갱신해 달라**(`media/audio/**` 의 문서이나 조정자 소유라 직접 고치지 않았다). 넣을 내용: (a) 아홉 디코더 전부 `openWide` 로 **파일의 모든 채널(표준 순서)과 float 정밀도**를 내는 넓은 경로가 있고(재생용 16비트 `PcmSource` 경로는 변하지 않음, 둘은 같은 디코딩 코어를 공유하며 <=2채널에서 비트 단위로 같음), 10.3 의 값 정의와 10.4 의 채널 순서 표를 근거로 한다. (b) 손실 코덱의 float 정밀도(10.5 의 표: MP3 20~42배, AAC 260~420배, Vorbis 137배, Opus 1.03~1.08배) 와 ffmpeg 채널별 참조 비교(최대 0~1 LSB). (c) 오디오 디코더 항목의 손상/잘림 설명(10.6: 첫 프레임 뒤의 손상은 부분 결과로 이어 가는 것이 `decodeAudioData` 의 정책이며, 형식별로 어느 잘림이 표시되는지). (d) `docs/progress/media-matrix.md` 에 "다채널 오디오는 스테레오로 다운믹스한다"고 쓰인 곳이 있다면 재생(`PcmSource`) 경로에 한정된 설명임을 밝힌다.
8. **`scripts/**` 변경 요청은 이번에도 없다.** `StaticStateAuditTest`/`ThreadCreationAuditTest` 는 B2 의 새 클래스(`media.audio` 의 float 소스들, `media.webaudio.decode.DecodeResult` 등)에 새 항목 없이 통과했다(정적 상태가 없다). `AGENTS.md` 의 "도구/경로" 규칙 대상인 새 도구는 만들지 않았다. 새 시험 자료 `tests/resources/media/audio/wide/*.f32`(10개)와 `trunc/*`(6개)는 합쳐 352,232 바이트이며 `generation/make_wide_refs.sh`, `make_trunc_fixtures.sh` 로 다시 만들 수 있다(ffmpeg `N-125875-g5d4d3bdc61-20260731`).
9. **A1/A2 에게 알릴 것**: `AudioBuffer.wrap(float[][], float)` 와 `AudioBufferDecoder.decodeResult` 가 공개 메서드이다. 디코딩 결과의 채널은 최대 32개이며 float 값이 1을 넘을 수 있다(클리핑 없음). 노드 그래프의 `AudioBufferSourceNode` 는 이를 그대로 쓰면 된다.
