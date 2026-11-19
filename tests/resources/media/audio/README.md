# 오디오 시험 자료 / Audio test inputs

이 디렉터리의 파일은 `tests/src/org/duckdns/hjow/swingwebpanel/media/audio/reference` 의 시험(`ReferenceDecodeTest`)이 쓴다(계획 P18-11). 제품 디코더를 **실제 인코더가 만든 파일**과
**ffmpeg 자신의 디코더가 만든 참조 PCM** 으로 교차 검증하려고 만든 것이다. 모두 합성 신호(사인 스윕, 잡음 버스트, 무음, 풀 스케일 근처의 큰 소리, 아주 작은 소리,
클릭)로 만들었으므로 저작권이 있는 음원은 없다.

The files here are used by `ReferenceDecodeTest` (plan item P18-11): they cross-check the product's decoders with **files that real encoders wrote** and with
**reference PCM from ffmpeg's own decoders**. All signals are synthetic (sweeps, noise bursts, silence, near full scale tones, a very quiet tone, clicks), so no
copyrighted audio is in the repository.

## 구성 / Layout

| 경로 | 내용 |
| --- | --- |
| `index.txt` | 파일마다 한 줄: 이름, 경로, MIME, 그룹, 종류(exact/lossy), 샘플링 주파수, 채널 수, 채널 역할, ffmpeg 의 프레임 수, 제품이 내야 할 프레임 수(`expected`), 참조, 설명(코딩 도구). 필드 설명은 스크립트 머리말. |
| `mp3/`, `aac/`, `vorbis/`, `opus/`, `flac/`, `wav/`, `aiff/`, `au/`, `webm/` | 인코더가 쓴 파일. |
| `ref/*.flac` | 손실 코덱 파일의 참조 PCM(ffmpeg 의 16비트 디코딩 결과)을 FLAC 으로 무손실 압축한 것. 채널이 셋 이상이면 채널마다 한 파일(`name.0.flac` …). |
| `generation/make_audio_fixtures.py` | 위 파일과 색인을 만든 스크립트(제품/시험 실행에는 필요 없음). |

- `exact` 파일(PCM, 부동 소수점, mu-law, A-law, ADPCM, FLAC)은 ffmpeg 가 디코딩한 16비트 PCM 과 **비트 단위로** 같아야 한다(참조 = md5).
- `lossy` 파일(MP3, AAC, Vorbis, Opus)은 ffmpeg 의 출력과 허용 오차 안에서 같아야 하고 길이(프레임 수)가 `expected` 와 같아야 한다. 허용 오차의 근거는 `docs/progress/media-matrix.md` 11절.
- 24/32비트 정수 샘플(WAV, AIFF, AU, FLAC)은 ffmpeg 가 16비트로 줄일 때 버림(truncation)을 하지만 제품은 반올림하므로, 참조는 ffmpeg 의 32비트 디코딩에서 같은 반올림으로 계산했다.
- Opus 참조는 libopus 의 부동 소수점 API(`-request_sample_fmt flt`)로 디코딩했다. 16비트 API 는 큰 소리를 부드럽게 깎는 소프트 클리핑을 하기 때문이다.
- 채널이 셋 이상인 손실 파일은 소리를 6 dB 낮춰 만들었다(참조는 채널마다 16비트로 잘려 있고 제품은 자르지 않은 채널을 섞기 때문).
- 참조 FLAC 은 제품의 FLAC 디코더로 읽는다. 이 디코더는 같은 시험의 `exact` FLAC 파일로 ffmpeg 와 비트 단위로 같음을 먼저 확인한다.

## 다시 만들기 / Regenerating

```text
python generation/make_audio_fixtures.py <ffmpeg 실행 파일> <이 디렉터리> [이름,이름,...]
```

필요한 것: Python 3 과 libmp3lame, 기본 `aac`, libvorbis, libopus, `flac`, PCM/ADPCM 인코더가 들어 있는 ffmpeg(만든 환경: `ffmpeg N-125875-g5d4d3bdc61-20260731`,
2026-11-18). 이름을 주면 그 파일만 다시 만들고 색인의 나머지 줄은 유지한다. 신호는 결정적이다(선형 합동 생성기). 다만 인코더가 Ogg 시리얼 번호나 시각 같은 값을 파일에 넣으므로(libvorbis, libopus, WAV `bext`) 다시 만들면 그 파일들은 바이트 단위로 같지는 않다. 재생성하면 `index.txt` 와 `ref/` 의 참조도 같이 바뀌므로 파일과 참조를 함께 커밋해야 한다.
