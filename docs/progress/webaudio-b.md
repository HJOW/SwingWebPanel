# WEBAUDIO-B 작업 기록 (P25-05 AudioBuffer 와 decodeAudioData, 순수 Java 쪽)

이 문서는 WEBAUDIO-B 작업자의 기록이다. 작업 번호 대역은 `20261122-140` ~ `169` 이다(이번에 140~146 과 이 문서의 commit 147 을 썼다). 앞 단계(A1)의 기록은 `docs/progress/webaudio.md` 이다.
조정자만 고치는 문서(`AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**`)에 대한 요청은 맨 아래 "조정자 요청"에 모았다.

범위: P25-05 중 **JavaScript 와 무관한 순수 Java 쪽**. `decodeAudioData` 의 Promise/콜백 모양, 전달받은 `ArrayBuffer` 의 분리(detach), 결과를 문서 큐로 옮기는 일, `BaseAudioContext.decodeAudioData` 메서드 자체는 C 단계(JavaScript 바인딩)이다. 그래서 **P25-05 전체는 완료로 체크하지 않는 것을 권장한다**(아래 "체크박스 후보").

## 1. 읽은 규격

`tools/webfetch`(JRE 25 로 실행, 조회 2026-11-22)로 Web Audio API 1.0 권고안 https://www.w3.org/TR/2021/REC-webaudio-20210617/ (604341 바이트)를 다시 받아 다음 절을 읽었다. 1.1 작업 초안 https://www.w3.org/TR/2026/WD-webaudio-1.1-20260922/ 은 A1 이 받아 둔 본문에서 같은 절을 읽었다.

| 절 | 내용과 이 구현의 결정 |
| --- | --- |
| 1.1.2 `decodeAudioData` | 디코딩은 제어 스레드도 렌더링 스레드도 아닌 별도 "디코딩 스레드"에서 한다. 여러 디코딩 스레드가 병렬로 처리할 수 있고 결과 순서는 정하지 않는다. 형식은 MIME Sniffing 으로 판별한다(데이터 시그니처). 실패는 `EncodingError`. 성공하면 컨텍스트의 표본율과 다를 때 리샘플링한다. 결과는 "media element task"로 전달한다. → `AudioBufferDecoder`, `Resampler`, `AudioDecodeService` |
| 1.1.2 (1.1 WD) | 1.0 과 같고 추가가 둘 있다. (1) 미디어 바이트 스트림에 오디오 트랙이 여럿이면 **첫 트랙만** 디코딩한다. MP4(AAC)와 WebM 의 P18 디코더가 이미 `firstAudioTrack()` 을 쓴다(코드를 읽어 확인했고 트랙이 여럿인 시험 자료는 없다). (2) "손상됐지만 디코딩을 계속할 수 있으면 개발자 도구로 경고하라"는 비규범 주석. 이 구현은 계속하지 않고 `EncodingError` 이다(아래 3.4). |
| 1.1.2 `createBuffer` | 인수가 음수/0/공칭 범위 밖이면 `NotSupportedError`. 채널 32개 이상, 표본율 8000~96000 이상을 지원해야 한다(규격이 정한 최소 지원 범위). → A1 의 `WebAudioLimits`(표본율 3000~768000, 채널 32) |
| 1.4 `AudioBuffer` | 생성자 `NotSupportedError`(공칭 범위 밖). `getChannelData` 의 채널이 범위 밖이면 `IndexSizeError`. **`copyFromChannel`/`copyToChannel` 은 `max(0, min(N_b - k, N_f))` 프레임을 복사하고 `bufferOffset` 이 길이 이상이어도 오류가 아니다**(오류는 `channelNumber` 뿐). |

## 2. 구현 파일과 한 일

소스는 `src/org/duckdns/hjow/swingwebpanel/media/webaudio/` 아래이다. 시험은 `tests/src/org/duckdns/hjow/swingwebpanel/media/webaudio/` 아래이다.

| 파일 | 역할 |
| --- | --- |
| `AudioBuffer.java`(변경) | `copyFromChannel/copyToChannel` 을 규격 1.4.3 의 식으로 바로잡음(2.1). 공개 정적 팩터리 `wrap(float[][], float)`(복사 없이 채널 배열을 가져감) 추가. 생성 검사를 한 메서드로 모음 |
| `decode/Resampler` | 표본율 변환기(Kaiser 창 sinc 다상 보간, 스트리밍, 채널 하나). 설계와 품질은 4절 |
| `decode/AudioBufferDecoder` | 파이프라인: 바이트 → P18 `AudioDecoders.open`(시그니처로 디코더 선택) → 16비트 PCM 블록 → float(`표본 / 32768`) → 채널별 `Resampler` → `ChannelSink` → `AudioBuffer.wrap`. 모든 실패는 원인이 붙은 `EncodingError`, 취소는 `CancellationException`. 시험용으로 소스를 직접 받는 `decodeSource` 가 있다(package-private) |
| `decode/ChannelSink` | 채널 하나의 출력 배열. 길이를 알면 그 크기 그대로(끝에서 복사 없음), 모르면 두 배씩 늘며 한도를 넘으면 `EncodingError` |
| `decode/DecodeCancellation` | 디코딩이 블록마다 확인하는 취소 플래그 인터페이스 |
| `decode/DecodeCallback`, `decode/DecodeRequest` | 결과 콜백(`onSuccess(AudioBuffer)`/`onError(AudioException)`)과 요청 핸들(`cancel()`, `isCancelled()`, `isDone()`) |
| `decode/AudioDecodeService` | 백그라운드 서비스. 자기 데몬 스레드에서 디코딩하고 호출한 쪽이 고른 `Executor` 에서 콜백. 취소, 닫기, 한도, 콜백 격리. 정적 상태 없음 |
| `decode/package-info` | 패키지 설명 |

시험: `AudioBufferTest` 7, `decode/ResamplerTest` 17, `decode/AudioBufferDecoderTest` 19, `decode/AudioDecodeServiceTest` 18, 도우미 `decode/AudioFiles`(스위트 아님). A1 의 `EngineStateTest` 의 어서션 하나를 바꾸었다(2.1).

### 2.1 `AudioBuffer` 결정

- `copyFromChannel/copyToChannel`: 규격 식대로 `bufferOffset >= length` 도 오류가 아니라 아무것도 복사하지 않는다(A1 의 기본형은 `IndexSizeError` 를 던졌다). A1 의 `EngineStateTest.testAudioBufferRules` 가 그 오류를 기대하고 있어서 **그 어서션 하나를 규격에 맞게 바꾸었다**(`offset at the length` → 아무것도 복사하지 않음). A1 의 나머지 어서션과 시그니처는 그대로이다.
- 정수 인수는 WebIDL `unsigned long` 값으로 본다. 음수인 Java 값은 2^31 이상인 값이다: 생성자와 `getChannelData` 의 채널에서는 범위 밖(`NotSupportedError`/`IndexSizeError`), `bufferOffset` 에서는 끝 뒤(복사 없음). JavaScript 계층은 `ToUint32` 한 값을 `(int)` 로 넘기면 된다.
- `wrap` 은 배열을 그대로 가져간다(디코딩 결과를 한 번 더 복사하지 않으려는 것). 채널 수 1~32, 길이가 모두 같고 1 이상, 표본 총수 `WebAudioLimits.MAX_BUFFER_SAMPLES` 이하, 표본율 3000~768000 을 생성자와 같은 규칙으로 검사한다.
- 규격의 "acquire the content"(재생 중 변경이 영향을 주는 시점)는 A1 의 5.3절 2번 그대로이다(재생 중인 소스 노드는 배열을 공유한다). 이번에 바꾸지 않았다.
- `AudioBufferOptions` 사전(`numberOfChannels` 기본 1, `length`/`sampleRate` 필수)의 기본값과 필수 검사(없으면 `TypeError`)는 JavaScript 계층의 일이다.

## 3. 디코딩 파이프라인의 결정

### 3.1 P18 디코더 재사용과 그 한계(중요)

- 형식 판별은 P18 의 시그니처 판별(`AudioDecoders.open`)을 그대로 쓴다. MIME 타입은 힌트이며 틀려도 결과가 같다(`testTheMimeTypeIsOnlyAHint`: 모든 시험 자료를 올바른 힌트, 힌트 없음, 틀린 힌트로 디코딩해 같은 표본). 시험 자료 아홉 계열 전부(WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM)가 같은 경로이다.
- 표본: P18 의 `PcmSource` 는 16비트 인터리브 PCM 이다. 표본마다 `표본 / 32768` 이 되며(float 로 정확) 다른 처리는 하지 않는다.
- **한계 1 (채널)**: P18 디코더는 **디코더 안에서** 채널이 둘보다 많은 소스를 `ChannelMixer` 로 스테레오로 다운믹스한다(`BlockPcmSource`, `RawPcmSource`, `FlacPcmSource`, `AdpcmSource`, `AacFrameDecoder`, `OpusSource`, `VorbisSource` 가 모두 같은 구조). 그래서 5.1 파일은 **스테레오 버퍼**가 된다. Web Audio 는 파일의 채널을 그대로 버퍼에 담는다(브리프의 "채널 수는 디코더가 주는 대로 보존한다"). 파이프라인 자체는 소스가 주는 채널(최대 32)을 순서와 개수 그대로 유지하며(`testChannelsOfASourceAreKept`: 1, 2, 3, 4, 6, 8, 32 채널의 가짜 소스), 한계는 디코더 쪽에 있다. `testMultichannelFilesAreMixedDownByTheMediaDecoders` 가 현재 한계를 확인하는 시험이며 디코더가 바뀌는 날 의도적으로 고쳐야 한다.
- **한계 2 (정밀도)**: 24/32비트 정수와 부동 소수점 표본(WAV, AIFF, AU, FLAC)과 손실 코덱의 내부 float 가 디코더 **안에서** 16비트로 반올림된다. 그래서 결과의 해상도는 16비트(약 -98 dB)이다. 실제 브라우저는 float 정밀도를 준다.
- 두 한계는 모두 **`media/audio/**` 의 디코더 출력 단계**에 있고 이 작업의 소유 경로 밖이어서 고치지 않았다. 제안은 조정자 요청 4번(B2)이다.

### 3.2 오류와 취소

- 데이터의 모든 실패는 `EncodingError` 이며 `getCause()` 에 P18 의 `MediaException`(또는 디코더가 던진 런타임 예외)을 보존한다: 알 수 없는 형식, 지원하지 않는 변형(예: Yamaha ADPCM, WMA, MPEG Layer II, Apple Lossless 시험 자료 넷), 손상/잘림, 표본이 없는 파일, 변환할 수 없는 표본율, 한도 초과, 메모리 부족(`OutOfMemoryError` 를 이 좁은 자리에서만 잡는다). 규격이 구분하지 않으며 부분 결과는 돌려주지 않는다.
- 디코더가 손상된 데이터 때문에 던지는 예상 밖의 런타임 예외(배열 인덱스 등)도 `EncodingError` 로 바뀐다(`testFailuresInTheMiddleOfTheStream`, 가짜 소스가 읽는 도중 `ArrayIndexOutOfBoundsException` 을 던진다). 손상 사본 1089개(시험 자료 121개 x 손상 9종: 잘림, 가운데 바이트 변경, 0 블록, 앞부분 1/3/8 바이트 변경, 아무 곳 1/4/32 바이트 변경)를 디코딩해 **703개는 유효한 버퍼로 디코딩되고 386개는 `EncodingError`** 이며 **그 밖의 예외는 0건**이다(`testDamagedFilesDecodeOrGiveAnEncodingErrorAndNothingElse`).
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

### 3.4 잘린 파일의 동작(확인한 사실과 선택)

잘린 사본을 디코딩하면 형식마다 다르다(임시 측정 프로그램의 결과, 거부하는 4개를 뺀 시험 자료 117개, 60%/90%/99% 에서 자름):

| 형식 | 60% 에서 자름 | 90%, 99% 에서 자름 |
| --- | --- | --- |
| WAV, AIFF, AU | 모두 디코딩(남은 만큼) | 모두 디코딩 |
| MP3 | 15/16 디코딩 | 모두 디코딩 |
| AAC | 17/25 디코딩, 8 `EncodingError` | 같음 |
| WebM | 모두 디코딩 | 모두 디코딩 |
| FLAC | 0/15 디코딩 | 90%: 2/15, 99%: 3/15 디코딩 |
| Ogg Vorbis, Opus | 모두 `EncodingError` | 모두 `EncodingError` |

즉 P18 디코더는 Ogg(Vorbis, Opus)와 FLAC 의 잘린 파일을 디코딩 실패로 보고한다. 브리프가 "잘림 → EncodingError"라고 했고 1.0 REC 도 실패하면 `EncodingError` 이므로 **엄격하게 두었다**(부분 결과 없음). 실제 브라우저는 보통 디코딩한 만큼을 돌려주므로 호환성 면에서는 관대한 쪽이 낫다. 정책 선택은 조정자 요청 5번이다.

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
- `DecodeRequest.cancel()`: 대기 중인 요청은 큐에서 빼고 카운터를 되돌리며, 실행 중인 요청은 인터럽트한다. **`true` 를 반환한 뒤에는 그 요청의 콜백이 시작되지 않는다**(결과가 이미 실행기에서 기다리고 있어도). 콜백이 이미 시작했으면 `false`. 무작위 취소 150개 시험(`testRandomCancellationKeepsTheBooks`)이 `true` 인 요청은 콜백 0회, 나머지는 정확히 1회, 끝난 뒤 서비스의 대기/활성 요청 0, 닫은 뒤 스레드 0 을 확인한다.
- 한도: `MAX_PENDING_REQUESTS` 64(디코딩이 끝나지 않은 요청), `MAX_PENDING_BYTES` 256 MB(그 데이터의 합). 넘으면 `decode()` 가 `NotSupportedError` 를 **동기로** 던진다(JavaScript 계층이 그것으로 Promise 를 거부). 서비스는 `byte[]` 를 넘겨받고(복사하지 않음) 디코딩이 끝나면 참조를 놓는다.
- 인수: `data`/`callback` 이 null 이면 `IllegalArgumentException`, 표본율이 3000~768000 밖이면 `NotSupportedError`. **빈 데이터는 동기 오류가 아니라 비동기 `EncodingError`**(규격: 디코딩 실패는 Promise 거부).

## 6. 확인하지 못한 것 / 한계

- 24/32비트·float 정밀도와 채널이 셋 이상인 파일의 원래 채널(3.1절, P18 디코더의 한계).
- 잘린 Ogg/FLAC 파일은 `EncodingError`(3.4절). 실제 브라우저와 다르다.
- 실제 64 MB 입력, 2^25 표본 출력, 수 분짜리 실제 파일의 메모리/시간: 직접 하지 않았다(줄인 한도로 같은 코드 경로를 시험).
- 트랙이 여럿인 MP4/WebM 에서 첫 트랙만 디코딩하는지: 코드를 읽어 확인했을 뿐 시험 자료가 없다.
- Chromium 의 `decodeAudioData` 결과 길이(`ceil` 인지)와 리샘플러 방식은 확인하지 못했다.
- JRE 6u45 이외의 JRE(JDK 7/8/25 가 이 PC 에 있다)에서는 돌리지 않았다.
- 실시간 `AudioContext`, JavaScript 노출, Promise/ArrayBuffer 분리/문서 큐 전달은 이번 범위 밖(C).

## 7. 실행한 검증(JDK 6u45, 이 PC)

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
    req = decoder.decode(bytes, /* mimeHint */ null, (float) context.getSampleRate(), new DecodeCallback() {
        public void onSuccess(AudioBuffer b) { /* promise resolve(wrapper(b)); successCallback 호출 */ }
        public void onError(AudioException e) { /* promise reject(DOMException "EncodingError"); errorCallback 호출 */ }
    }, documentQueueExecutor);                                   // 콜백을 문서 큐로 옮기는 Executor
} catch (AudioException e) {                                     // InvalidStateError(서비스가 닫힘) 또는 NotSupportedError(한도)
    /* promise reject(e) */
}
```

- `AudioDecodeService.decode` 는 어느 스레드에서나 부를 수 있다. `AudioBufferDecoder.decode(...)` 는 정적이고 상태가 없으며 호출 스레드에서 동기로 돈다(시험이나 즉시 필요한 경우. 렌더링/제어 스레드에서는 쓰지 말 것).
- **취소된 요청(`cancel()`, `close()`)은 콜백이 오지 않으므로 JavaScript 계층의 promise 는 스스로 정산하거나 버려야 한다**(규격의 `[[pending promises]]` 처리).
- `AudioBuffer`(디코딩 결과)는 스레드 안전하지 않다. 결과는 `Executor` 가 옮겨 준 스레드에서만 만지면 된다.
- 리샘플러는 다른 곳에서도 쓸 수 있다(예: HRTF 응답을 컨텍스트 표본율로): `Resampler.resample(float[] in, double inRate, double outRate)` 가 `ceil` 길이의 배열을 돌려주고, 스트리밍은 `new Resampler(in, out)`, `process(...)`, `finish(...)`, `maxOutputFrames(n)`, `maxFinishFrames()`, `outputLength(n)` 이다. 채널마다 인스턴스를 쓴다.

## 9. 체크박스 후보(체크는 조정자 몫)

- **P25-05: 미완료로 유지 권장.** 끝난 부분(순수 Java): `AudioBuffer` 의 생성 규칙/`getChannelData`/`copyFromChannel`/`copyToChannel`(규격 1.4), 자체 디코더를 재사용하는 `decodeAudioData` 파이프라인(전 형식, 시그니처 판별, `EncodingError` 매핑), 컨텍스트 표본율로의 리샘플링과 방식/품질 근거 기록(4절), 백그라운드 디코딩과 취소/닫기/한도/스레드 수명. 남은 부분: `createBuffer`/`AudioBuffer` 생성자/`decodeAudioData` 의 **JavaScript 노출**(Promise 와 콜백 형식, `ArrayBuffer` 분리, 문서 큐 전달) = C 단계, 그리고 3.1절의 채널/정밀도 한계(B2 를 승인하면 해소).
- P25-06~08: 시작하지 않음.

## 조정자 요청

1. **FEATURE_MATRIX 의 WAU-07 행 교체 초안**(기존 "(B) / 부분 구현 / A1 은 노드가 쓰는 자료형만"을 대체):

| 기능 ID | 요구사항 | 규격/판본 | 구현 클래스 | 검증 사례 | 지원 상태 | 제약 |
| --- | --- | --- | --- | --- | --- | --- |
| WAU-07 | `AudioBuffer`(생성 규칙, `getChannelData`, `copyFromChannel`/`copyToChannel`), `decodeAudioData` 의 순수 Java 부분: 자체 디코더 재사용 파이프라인(시그니처 판별, `EncodingError`), 컨텍스트 표본율로의 리샘플링(Kaiser 창 sinc 다상, 정지 대역 100 dB), 백그라운드 디코딩 서비스(취소, 닫기, 한도) | Web Audio API 1.0 REC(2021-06-17) 1.1.2, 1.4. 1.1 WD(2026-09-22)의 첫 트랙만 디코딩 | `media.webaudio.AudioBuffer`, `media.webaudio.decode.{AudioBufferDecoder, Resampler, AudioDecodeService, DecodeCallback, DecodeRequest, DecodeCancellation, ChannelSink}` | `AudioBufferTest` 7, `ResamplerTest` 17, `AudioBufferDecoderTest` 19(P18 시험 자료 121개 전부, 손상 사본 1089개), `AudioDecodeServiceTest` 18 | 부분 구현(순수 Java 검증 완료, JavaScript 노출은 C) | 채널 3개 이상은 P18 디코더가 스테레오로 다운믹스, 24비트 이상과 float 는 16비트로 반올림, 잘린 Ogg(Vorbis/Opus)/FLAC 은 `EncodingError`(실제 브라우저는 부분 결과). 출력 길이는 `ceil` |

2. **ARCHITECTURE.md**: 패키지 표에 `media.webaudio.decode`(디코딩 파이프라인, 표본율 변환기, 백그라운드 서비스)를 추가하고 스레드 모델에 "`AudioDecodeService`: 인스턴스마다 데몬 스레드 풀(이름 `swingwebpanel-audio-decode-N`, 유휴 5초 뒤 종료, 정적 상태 없음), 콜백은 호출자가 준 `Executor`" 한 줄을 넣는다. 도구 목록은 바뀌지 않았다(`tools/webfetch`, `tools/sourcecheck` 만 사용).
3. **A1 파일을 한 군데 고쳤다**: `tests/.../media/webaudio/EngineStateTest.java` 의 어서션 하나(`copyFromChannel` 에서 `bufferOffset` 이 길이와 같을 때 `IndexSizeError` 기대)를 규격 1.4.3 에 맞게 "아무것도 복사하지 않음"으로 바꾸었다(2.1절). A1 의 `AudioBuffer` 구현도 같은 이유로 바뀌었다. A2 가 이 파일을 건드리지 않기로 되어 있어 충돌은 없을 것이다.
4. **(승인 필요) B2: P18 디코더에 원래 채널과 float 정밀도를 내는 선택적 경로 추가.** 3.1절의 두 한계는 `media/audio/**` 에 있다. 제안: `media.audio` 에 `FloatPcmSource`(`getSampleRate/getChannels/getFrameCount`, `int read(float[][] planar, int offset, int frames)`, `close`)와 선택적 `AudioDecoder` 확장(`openWide(byte[], DecodeLimits)`)을 두고, 각 디코더(`RawPcmSource`/WAV/AIFF/AU, `AdpcmSource`, `FlacPcmSource`, `VorbisSource`, `Mp3Source`, `AacFrameDecoder`, `OpusSource`, WebM 의 래퍼)가 믹서와 16비트 변환 직전의 float 를 그대로 내보내게 한다(기존 `PcmSource` 경로는 그대로). `AudioDecoders.openWide` 는 넓은 경로가 없는 디코더를 16비트 경로로 감싼다. 이 작업의 파이프라인은 `AudioBufferDecoder.decodeFrom` 한 곳만 `FloatPcmSource` 를 읽도록 바꾸면 된다(바이트 → float 변환 루프를 대체). 검증은 기존 시험 자료로 가능하다: 무손실 ≤2채널은 새 경로를 16비트로 줄인 것이 기존 경로와 비트 단위로 같아야 하고, 채널이 3개 이상인 파일은 `ref/*.N.flac`(채널마다 ffmpeg 의 참조)과 비교한다. 규모는 파일 약 9개에 각 수십 줄과 시험이며 한 작업자가 한 세션에서 할 수 있다고 본다(실제 해 보지는 않은 추정). 이번 작업의 소유 경로 밖이어서 하지 않았다.
5. **(결정 필요) 잘린 파일 정책**(3.4절): 현재 엄격(`EncodingError`, 브리프와 1.0 REC 에 충실). 대안은 "디코딩한 만큼 돌려준다"이다(1.1 WD 의 비규범 주석과 실제 브라우저의 동작에 가깝다). 구현은 `AudioBufferDecoder.decodeFrom` 에서 `MediaException.Kind.CORRUPT` 를 받았을 때 이미 모은 프레임이 있으면 거기까지로 끝내는 한 군데 변경과 시험 갱신이다. 정하면 알려 달라.
6. `docs/PROGRESS.md` 에 이 단계의 결과(위 7절의 명령과 결과, 한계 6절)를 옮겨 달라. 이 단계는 `scripts/**` 변경 요청이 없다. 감사 시험을 `sh scripts/pkgtest.sh` 로 돌리면 제품 전체를 소스패스로 컴파일하므로 느려서, 이번에는 `sh build.sh` 뒤 `build/classes` 에 대해 `javac` 와 `TestRunner --scan` 으로 따로 돌렸다(7절). 이 방식을 스크립트로 두고 싶으면 알려 달라.
