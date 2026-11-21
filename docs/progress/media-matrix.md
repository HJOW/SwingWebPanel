# MEDIA-MATRIX: 이미지·오디오 형식 대응표와 점검 (P18-01, P18-02, P18-03, P18-10, P18-11)

**MEDIA-D (2026-11-18 작업, 2026-11-19 조정자 마무리): 11절.** 요약: (1) 오디오 디코더를 이 PC 의 ffmpeg 로 만든 **실제 인코더 파일 117개**(libmp3lame, ffmpeg 기본 AAC, libvorbis, libopus, flac, PCM/ADPCM)와 ffmpeg 의 참조 PCM 으로 교차 검증했다
(`ReferenceDecodeTest` 15건 통과, 무손실은 비트 단위, 손실은 ISO/IEC 11172-4 허용 오차 안; 거부해야 하는 형식 4개는 `UNSUPPORTED_FORMAT`). (2) 실제 파일에서만 드러난 결함과 빠진 기능 12가지를 고쳤다(11.4절). (3) P18-11 의 기록 작업은 끝났으나
P18 의 마지막 항목이라 P18-08(소리를 귀로 확인, 사람 필요)이 닫힐 때까지 체크하지 않는다(11.7절).

**조정자 요청 (MEDIA-C, 2026-11-18): 10.8절을 읽을 것.** 요약: (1) AVIF 의 남은 변형(인트라 블록 복사, 초해상도, 인터 프레임 전체, 애니메이션 AVIF, 계층 이미지)을 모두 구현했고 libdav1d 와 비트 단위로 비교했다.
(2) **P18-03 은 체크 후보**(조건과 근거: 10.6절, 최종 판단은 조정자), P18-11 은 오디오 쪽 미확인 때문에 체크하지 않는다. (3) `media.image.avif` 가 `Av1*` 24개 클래스로 늘었다(`docs/ARCHITECTURE.md` 의 설명 갱신 요청: 10.8절).

**조정자 요청 (MEDIA-B, 2026-11-18): 9.7절을 읽을 것.** 요약: (1) `PropertyTable`/`Prop` 끝에 `image-orientation` 을 더했고 CSS-J 와 충돌하여 조정자가 id 를 309 로 다시 매겼다(처리됨), (2) P18-03/P18-11 은 체크하지 말 것(AVIF 인터 프레임·인트라 블록 복사·초해상도·계층 이미지가 남음: 7절, 9.5절),
(3) 이 PC 에 ffmpeg(libaom/libdav1d/libzimg)가 있어 AV1 남은 변형은 기준 스트림과 비교하며 이어서 할 수 있다.

작성: MEDIA 작업자(2026-11-17). 이 문서는 형식마다 "무엇을 어떤 시험으로 어디까지 확인했는지"를 한 곳에 모은 것이다. 모든 시험은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로 직접
실행한 것만 "통과"로 적었고, 돌리지 못한 것과 사람이 확인해야 하는 것은 "미확인"으로 적었다. 형식별 세부(구현 정책, 알려진 한계)는 각 영역 문서에 있다:
`media-img-a.md`(PNG/APNG/GIF/JPEG/BMP/ICO), `media-img-b.md`(WebP), `media-svg-avif.md`(SVG/AVIF), `media-aud-a.md`(WAV/AIFF/AU/FLAC/Ogg/Vorbis),
`media-aud-b.md`(MP3/AAC/MP4/WebM), `media-opus.md`(Opus), `media-play.md`(재생 엔진과 `audio` 요소).

## 1. JRE 6u45 가 직접 읽는 형식 (P18-02, 실측)

`build/probe/JreProbe`(저장소에 넣지 않는 일회용 프로그램)를 JDK 6u45 로 실행한 결과(2026-11-17, `java.version=1.6.0_45`, Sun Microsystems Inc., Windows 10):

```text
ImageIO reader format names: [BMP, GIF, JPEG, JPG, PNG, WBMP, bmp, gif, jpeg, jpg, png, wbmp]
ImageIO reader suffixes:     [bmp, gif, jpeg, jpg, png, wbmp]
ImageIO reader MIME types:   [image/bmp, image/gif, image/jpeg, image/png, image/vnd.wap.wbmp, image/x-png]
Java Sound audio file (writer) types: WAVE(wav) AU(au) AIFF(aif)
Java Sound mixers: 3  (DirectSound Playback 장치 둘과 Port Mixer)
Encodings convertible from PCM_SIGNED: [ALAW, PCM_FLOAT, PCM_SIGNED, PCM_UNSIGNED, ULAW]
```

- **ImageIO 는 BMP/GIF/JPEG/PNG/WBMP 만 읽는다.** WebP, AVIF, SVG, ICO/CUR 은 리더가 없고 APNG 는 기본 이미지만, GIF 는 합성되지 않은 프레임만 준다(`ImageIoFactsTest`, `media-img-a.md`).
  그래서 **이미지 디코더는 모두 자체 구현**이고, JPEG 만 샘플 디코딩(허프만/IDCT)을 ImageIO 의 `readRaster`(공개 API)에 맡긴다(헤더·마커·EXIF·색 규칙·CMYK 는 자체).
- **Java Sound 는 오디오 "읽기" 에 쓰지 않는다.** JRE 6 의 Java Sound 는 WAV IMA/MS ADPCM, WAV EXTENSIBLE, AU float, AIFC `sowt`/`twos`/`fl32` 를 읽지 못하고 스트리밍 WAV 헤더를 빈 데이터로 읽으며,
  MP3/AAC/Ogg/FLAC/Opus 는 아예 없다(`media-aud-a.md` 실측). 그래서 **모든 오디오 디코더는 자체 구현**이고 Java Sound 는 **출력**(`SourceDataLine`)에만 쓴다(`JavaSoundOutputFactory`).
- 자체 디코더가 필요한 것과 JRE 가 읽는 것의 구분은 아래 표의 "JRE 6 이 직접" 열이다. "예" 라도 자체 디코더를 쓰는 형식이 있다(PNG/GIF/BMP 는 ImageIO 를 시험의 교차 확인에만 쓴다).
- **이 PC 의 Java Sound 믹서 수**: 위 실행에서 3개였다(이전 문서의 "믹서 0개" 와 다르다. 오디오 장치의 상태에 따라 달라지는 값으로 보인다). 믹서가 있을 때만 실제 장치 시험이 돌고, 없으면
  `JavaSoundDeviceTest` 가 이유를 대고 건너뛴다.

## 2. 이미지 형식 (P18-01, P18-03)

| ID | 형식 (MIME) | 구현 | JRE 6 이 직접 | 시험 (JDK 6u45, 클래스: 건수) | 변형 독립 검증 | 알려진 차이 |
| --- | --- | --- | --- | --- | --- | --- |
| IMG-01 | PNG (`image/png`) | `PngDecoder` | 예(자체 디코더 사용, ImageIO 는 교차 확인) | `PngDecoderTest` 14 | 색 유형 5 × 비트 심도 15조합 × 크기 6 × 인터레이스, tRNS, 16비트, 필터 0~4, CRC, 잘림, 크기 폭탄 | 16비트는 상위 바이트, 룩업 테이블 ICC 는 sRGB 로 간주(색 관리와 방향은 9절) |
| IMG-02 | APNG (`image/apng`) | `PngDecoder` | 기본 이미지만 | `ApngDecoderTest` 10 | 처리/혼합 방법, 기본 이미지 안/밖, 지연·반복, 순서 오류 시 정지 대체 | |
| IMG-03 | GIF (`image/gif`) | `GifDecoder` | 프레임을 합성하지 않음 | `GifDecoderTest` 10 | LZW 9~12비트, 투명, 처리 방법 1·2·3, 인터레이스, 지연/반복 | |
| IMG-04 | JPEG (`image/jpeg`) | `JpegDecoder` | 원시 샘플만(`readRaster`) | `JpegDecoderTest` 10 | baseline/progressive, 회색, CMYK/YCCK, EXIF 방향 1~8, 잘림 | 산술/무손실/12비트/성분 2개는 `UNSUPPORTED_FORMAT`, CMYK 는 ICC 를 쓰지 않음 |
| IMG-05 | BMP (`image/bmp`) | `BmpDecoder` | 예(자체 디코더 사용) | `BmpDecoderTest` 8 | 1/4/8/16/24/32비트, RLE4/8, V4/V5, 알파 | BI_JPEG/BI_PNG/RLE24 미지원 |
| IMG-06 | ICO/CUR (`image/x-icon`) | `IcoDecoder` | 없음(WBMP 리더가 헤더를 가로챔) | `IcoDecoderTest` 5 | 32비트 알파, AND 마스크, 여러 크기, 내장 PNG | |
| IMG-07 | SVG (`image/svg+xml`) | `svg.SvgDecoder` | 없음 | `SvgDecoderTest` 19, `SvgReferenceTest` 12(Chrome 156 비교 151 사례), `SvgCssTest` 8, `SvgValueParsersTest` 10, `SvgXmlParserTest` 7 | 도형/경로/변환/그라디언트/문자/필터/`use` | CSS·SMIL 애니메이션 미실행, 외부 리소스 읽지 않음 |
| IMG-08 | WebP (`image/webp`) | `webp.WebpDecoder` | 없음 | `WebpDecoderTest` 2(Chrome 파일 11개), `WebpLibwebpTest` 5(libwebp 92개 프레임 바이트 비교), `WebpRobustnessTest` 10 | VP8 손실, VP8L 무손실, ALPH 알파, ANIM/ANMF 애니메이션 | XMP 는 건너뜀(EXIF 방향과 ICCP 프로파일은 적용) |
| IMG-09 | AVIF (`image/avif`) | `avif.AvifDecoder` | 없음 | `AvifDecoderTest` 13, `Av1StreamTest` 10(libdav1d 와 비트 단위 비교 77개 스트림), `Av1SequenceTest` 3(인터 프레임 시퀀스 39개, 321프레임), `AvifSequenceTest` 7, `AvifLayerTest` 5, `Av1ComponentsTest` 6, `AvifOverlayTest` 6, `AvifColorTest` 2 | 정지 영상(AV1 인트라), 알파, 격자, 오버레이(중첩 포함), 필름 그레인, 인트라 블록 복사, 초해상도, **애니메이션(인터 프레임)**, **계층 이미지(`a1op`/`lsel`)**, irot/imir/clap, 8/10/12비트, 4:0:0~4:4:4 | `enable_dual_filter=1` 스트림과 계층 간 예측이 있는 실제 공간 계층 스트림은 libdav1d 와 비교하지 못함, 프레임 크기가 바뀌는 시퀀스는 기본 항목만 표시, 행렬 계수 10~14 는 `UNSUPPORTED_FORMAT`: 10절 |
| (공통) | 형식 판별과 제한 | `ImageDecoders` | | `ImageDecodersTest` 8, `ImageIoFactsTest` 3, `MediaFailureKindsTest`(아래 3절) | 시그니처가 형식을 정함(MIME 은 동률 해소용 힌트) | |

이미지 요소 쪽(P18-05/06): `ImageLoadIntegrationTest` 2, `MediaFallbackIntegrationTest`(표시 실패 시 미지원 URL + alt), `ImageSelection`/`picture`/`srcset` 시험은 `docs/progress/media-img-b.md` 와 렌더 시험에 있다.

## 3. 오디오 형식 (P18-01, P18-07)

| ID | 형식 (MIME) | 구현 | JRE 6 Java Sound 가 읽는가 | 시험 (클래스: 건수) | 변형 | 알려진 차이 |
| --- | --- | --- | --- | --- | --- | --- |
| AUD-01 | WAV (`audio/wav`, `x-wav`, `wave`) | `pcm.WavDecoder` | PCM/μ-law/A-law 만(ADPCM, EXTENSIBLE 못 읽음) | `WavDecoderTest` 15, `AdpcmTest` 6, `SampleConversionTest` 3, **ffmpeg 파일 18개 비트 단위(11절)** | PCM 8/16/24/32, float 32/64, μ-law/A-law, IMA/MS ADPCM, EXTENSIBLE, 스트리밍 크기, **RF64/BW64** | |
| AUD-02 | AIFF/AIFC (`audio/aiff`) | `pcm.AiffDecoder`, `pcm.AdpcmSource` | PCM 만 | `AiffAuDecoderTest` 7, **ffmpeg 파일 12개 비트 단위(11절)** | PCM 8~32, `sowt`/`twos`/`fl32`/`fl64`/`ulaw`/`alaw`/`raw `/`in24`/`in32`, **`ima4`** | 그 밖의 AIFF-C 압축 미지원 |
| AUD-03 | AU/SND (`audio/basic`) | `pcm.AuDecoder` | PCM/μ-law/A-law | (위와 같은 클래스), **ffmpeg 파일 8개 비트 단위(11절)** | PCM, μ-law, A-law, float | ADPCM/DSP 미지원 |
| AUD-04 | MP3 (`audio/mpeg`) | `mp3.Mp3Decoder` | 없음 | `Mp3DecoderTest` 13, **libmp3lame 파일 15개를 ffmpeg 와 비교(11절)** | MPEG-1/2/2.5 Layer III, ID3v1/v2, VBR(Xing), LAME 갭리스 | 인텐시티 스테레오·혼합 블록은 실제 파일로 확인 못 함. Layer I/II 는 `UNSUPPORTED_FORMAT` |
| AUD-05 | AAC (`audio/aac`, `audio/mp4`, `audio/x-m4a`) | `aac.AacDecoder`, `mp4.*` | 없음 | `AacDecoderTest` 13, `Mp4FileTest` 4, **ffmpeg AAC 파일 25개를 ffmpeg 와 비교(11절)** | ADTS, MP4/M4A(조각 포함), AAC-LC, 인덱스 0~12, 구성 1~7/11/12/14 | HE-AAC 는 SBR/PS 없이 핵심만(실제 파일 없음). Main 예측/LTP 미지원 |
| AUD-06 | Ogg Vorbis (`audio/ogg`) | `vorbis.VorbisDecoder`, `ogg.*` | 없음 | `VorbisDecoderTest` 8, `VorbisFloorTest` 2, `OggReaderTest` 8, **libvorbis 파일 9개 + WebM 1개를 ffmpeg 와 비교(11절)** | Vorbis I 전체, 체인/다중화, 탐색, 다채널 | 체인 파일은 첫 고리만. floor 0 은 실제 파일 없음 |
| AUD-07 | FLAC (`audio/flac`) | `flac.FlacDecoder` | 없음 | `FlacDecoderTest` 11, **ffmpeg flac 파일 15개 비트 단위(11절)** | 모든 서브프레임, 4~32비트, Ogg FLAC | CRC 오류는 엄격하게 `CORRUPT` |
| AUD-08 | Opus (`audio/ogg; codecs=opus`) | `opus.OpusDecoder` | 없음 | `OpusVectorTest` 28(RFC 6716 벡터 12 + RFC 8251 3 발췌), `OpusDecoderTest` 16, `OpusPacketTest` 5, `OpusRangeDecoderTest` 6, `CeltPvqTest` 4, `OpusRobustnessTest` 5, **libopus 파일 12개 + WebM 1개를 libopus/ffmpeg 와 비교(11절)** | CELT/SILK/하이브리드, Ogg Opus, 매핑 군 1(5.1) | 원본 벡터 12건은 `SWP_OPUS_VECTORS` 가 있을 때만(건너뜀). 패킷 손실 은닉은 libopus 와 비교 못 함 |
| AUD-09 | WebM 오디오 (`audio/webm`) | `webm.WebmDecoder` | 없음 | `WebmDecoderTest` 5, `OpusWebmTest` 6, **ffmpeg WebM 파일 2개(Vorbis, Opus)를 ffmpeg 와 비교(11절)** | Matroska + Vorbis/Opus, DiscardPadding | 브라우저가 만든 WebM 은 미확인(ffmpeg 먹서 파일뿐) |
| (공통) | 형식 판별과 블록 PCM | `AudioDecoders`, `BlockPcmSource` | | `AudioDecodersTest` 5, `BlockPcmSourceTest` 4, `MediaFailureKindsTest`(아래) | 시그니처가 형식을 정함, `canPlayType` | |

재생(AUD-10, WEB-09): `MediaPlayerTest` 20(가짜 출력), `RateConverterTest` 9, `MediaElementsTest`(요소 상태 기계), `AudioIntegrationTest` 6, `AudioControlsIntegrationTest`, `AudioRobustnessIntegrationTest`, `JavaSoundDeviceTest`(실제 장치, 무음).

## 4. 실패 종류 (P18-01): `MediaException.Kind`

종류: `ACCESS_FAILED`(접근 실패: 네트워크/장치), `UNSUPPORTED_FORMAT`(형식 미지원), `CORRUPT`(손상), `TOO_LARGE`(제한 초과), `CANCELLED`(이동 취소). 접근 실패는 네트워크 계층(`MediaRequestSource` 의 오류 문자열 →
`error` 코드 4)과 소리 출력(`JavaSoundOutputFactory`, 장치 없음/형식 미지원 → `ACCESS_FAILED` → `error` 코드 3)에서 나오고, 나머지는 디코더가 낸다.

`MediaFailureKindsTest`(새로 추가, 형식마다 같은 입력 구성)가 형식마다 실제로 나오는 종류를 고정한다(선언된 MIME 이 있든 없든 같다):

| 입력 | 결과 |
| --- | --- |
| 시그니처 + 엉터리 데이터: PNG, GIF, JPEG, ICO, WebP, AVIF, SVG, WAV, AIFF, AU, FLAC | `CORRUPT`(시그니처가 형식을 정하므로 형식은 알고 내용이 손상) |
| 시그니처 + 엉터리 데이터: BMP, Ogg, MP3(ID3 와 프레임 동기), MP4/AAC, AAC ADTS, WebM | `UNSUPPORTED_FORMAT`(BMP 는 두 글자뿐인 시그니처, 나머지는 안쪽 코덱/프레임 구조가 확인되어야 디코더가 파일을 받아들이므로 엉터리 앞부분은 "그 형식이 아님") |
| HTML 같은 글(서버의 오류 페이지)을 이미지/오디오로 | 모든 선언 타입에서 `UNSUPPORTED_FORMAT` |
| 빈 파일, null | `CORRUPT` |
| `DecodeLimits` 의 바이트 제한 초과 | `TOO_LARGE`(이미지·오디오 모두, 디코더가 보기 전) |

페이지에서 리소스를 로드할 때는 이 차이가 모두 같은 `error` 이벤트(코드 4)로 끝난다. 종류가 의미를 갖는 곳은 오류 메시지와, 재생이 시작된 뒤에 나타나는 손상(`CORRUPT` → 코드 3 `MEDIA_ERR_DECODE`)이다.
**알려진 비일관성**: 앞의 둘째 줄의 형식은 앞부분이 손상되면 "손상" 이 아니라 "형식 아님" 으로 보고된다(예: ID3 태그만 있고 MPEG 프레임이 없는 MP3). 코드(3 대신 4)와 메시지가 조금 부정확해지는 정도라 고치지 않았다.

요소 쪽 대응: 가져오기 실패/지원하지 않는 형식/손상 → `error` 이벤트와 `MediaError.code`(오디오 4 = `MEDIA_ERR_SRC_NOT_SUPPORTED`, 재생 중 손상과 장치 없음 3 = `MEDIA_ERR_DECODE`), 이미지 → `error` 이벤트와 대체 표시(미지원 URL + alt).

## 5. 변형별 독립 검증 (P18-03)

| 변형 | 형식 | 근거 |
| --- | --- | --- |
| 애니메이션 | APNG, GIF, WebP(ANIM), **AVIF(`avis`)** | `ApngDecoderTest`, `GifDecoderTest`, `WebpLibwebpTest`(프레임 바이트 비교), `AvifSequenceTest`(프레임 픽셀이 libdav1d 와 비트 단위로 같은 원본 시퀀스와 같음, 지속 시간·반복 횟수는 컨테이너에서 읽은 값). SVG 애니메이션 미실행 |
| 투명도 | PNG(tRNS, 알파 채널), GIF(투명 인덱스), WebP(ALPH, VP8L), AVIF(보조 이미지), BMP(32비트), ICO(알파/AND 마스크) | 각 디코더 시험 |
| 진행형 JPEG | JPEG | `JpegDecoderTest` (baseline/progressive 구역 색 비교) |
| 방향 정보 | JPEG(EXIF 1~8), **PNG(`eXIf`), WebP(`EXIF`)**, AVIF(irot/imir/clap) | 공통 `ExifOrientation`: `ExifOrientationTest` 8(바이트 순서 두 가지, `Exif\0\0` 머리말, 손상, 정의대로의 여덟 방향, APNG 모든 프레임), `WebpOrientationTest` 4(손실/무손실/애니메이션, EXIF 가 이미지 앞뒤), `JpegDecoderTest`, `AvifDecoderTest`. CSS `image-orientation: from-image`(기본)/`none`: `ImageOrientationPaintTest` 5(img 자연 크기와 화소, 상속, 배경 이미지, 각도/flip 값 거부) |
| 색 처리(YUV/CMYK) | JPEG CMYK/YCCK, AVIF 행렬/범위/비트 깊이, WebP YUV→RGB(libwebp 와 바이트 일치) | 각 시험 |
| 색 관리(ICC, gamma, CICP) | PNG(`cICP`>`iCCP`>`sRGB`>`gAMA`+`cHRM`), JPEG(APP2 ICC, 여러 조각), WebP(`ICCP`), BMP(V4 보정된 RGB, V5 내장 프로파일), AVIF(`colr` nclx/prof/rICC) | `color.ColorProfileTest` 12(독립 기준: CSS Color 4 의 행렬 분수, ICC 프로파일 직접 작성, **ffmpeg libzimg 대조**), `ColorImageTest` 9, `WebpColorTest` 3, `AvifColorTest` 2. 결정과 한계는 9절 |
| HDR 전달 특성 | PQ(SMPTE ST 2084), HLG(ITU-R BT.2100) | CICP 가 PQ/HLG 이면 HDR 기준 백색 203 cd/m2 를 표준 동적 범위의 흰색으로 맞추고 더 밝은 값은 자른다(`ColorProfileTest.testCodePoints`). 9절 |
| 필름 그레인(AV1) | AVIF | `Av1StreamTest.testBitExactAgainstDav1d`: libaom 시험 벡터 16개와 홀수 크기, 4:4:4, 4:2:2, 10/12비트, 회색, 큰 이미지 등 24개 스트림이 libdav1d 와 비트 단위로 같음 |

## 6. 이번 작업(P18-08/10/11) 시험 결과

(아래 결과는 JDK 6u45 로 직접 실행한 것이다. 실행 환경: Windows 10, 4코어, 다른 작업자와 CPU 공유. 세부는 `media-play.md` 의 "2026-11-17 2차 MEDIA 작업 기록".)

| 시험 | 결과 |
| --- | --- |
| `org.duckdns.hjow.swingwebpanel.media.playback`(`MediaPlayerTest` 20, `RateConverterTest` 9, `JavaSoundDeviceTest` 3) | 32 통과, 0 실패, 0 건너뜀(실제 라인 시험도 실행됨) |
| `MediaElementsTest` | 23 통과 |
| `MediaControlsTest` | 6 통과 |
| `AudioControlsIntegrationTest`(합성 AWT 마우스) | 1 통과 |
| `AudioIntegrationTest` | 6 통과 |
| `AudioRobustnessIntegrationTest` | 5 통과 |
| `MediaFailureKindsTest` | 5 통과 |

`MediaFailureKindsTest` 를 처음 돌렸을 때 BMP, Ogg, MP3, MP4, AAC ADTS, WebM 의 "시그니처 + 엉터리" 가 `CORRUPT` 가 아니라 `UNSUPPORTED_FORMAT` 이었다. 코드를 바꾸지 않고 형식별 실제 종류를 표(4절)에 고정했다(이유와 영향은 4절).

## 7. AVIF 의 남은 변형: 설계와 범위 (P18-03)

**갱신(MEDIA-C, 2026-11-18): 이 절이 "남은 변형"으로 적었던 네 가지(초해상도, 인트라 블록 복사, 계층 이미지, 인터 프레임)와 그에 딸린 애니메이션 AVIF 를 모두 구현했다. 설계·근거·검증은 10절.**
이 절의 아래 내용은 MEDIA-B 가 적은 당시의 계획과 방법이며 기준 도구 정보는 지금도 맞다.

**구현한 것(MEDIA-B 시점):** 오버레이(`iovl`), **중첩 오버레이**(세 단계까지, 순환은 거부: `AvifOverlayTest` 6), **필름 그레인 합성**(`Av1FilmGrain`, 규격 5.9.30 / 7.18.3. 정수 연산이 규격 그대로라 libdav1d 와 비트 단위로 같다).

**기준 도구는 이 PC 에 있다**(이전 기록의 "ffmpeg 없음" 은 틀렸다): `ffmpeg N-125875-g5d4d3bdc61-20260731`(`C:\Users\BUZZ\AppData\Local\Microsoft\WinGet\Packages\yt-dlp.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-N-125875-g5d4d3bdc61-win64-gpl\bin`)에
인코더 libaom-av1, librav1e, libsvtav1, 디코더 libdav1d, 필터 zscale(libzimg)이 들어 있다. 새 스트림은 `tests/resources/media/avif/generation/` 의 `make_*_fixtures.py` 로 만들고 libdav1d 로 디코딩한 평면의 md5 를
색인(`av1/index.txt`, `seq/index.txt`, `layers/index.txt`)에 적어 시험으로 확인한다.

## 8. 미확인과 사람이 필요한 것 (P18-10, P22-10)

- **소리를 귀로 확인하지 못했다.** `JavaSoundDeviceTest` 는 실제 Java Sound 라인을 열고 **무음**을 써서 위치 시계가 장치를 따라 끝에 닿는 것까지 확인하지만, 들리는지는 확인할 수 없다.
- 실제 마우스로 `controls` 막대를 눌러 본 확인은 사람이 해야 한다(`AudioControlsIntegrationTest` 는 합성한 AWT 마우스 이벤트로 컴포넌트에 보낸다).
- 실제 libvorbis/lame/AAC 인코더가 만든 파일과의 상호 운용은 **11절에서 확인했다**(ffmpeg 의 libmp3lame, 기본 AAC 인코더, libvorbis, libopus 와 ffmpeg 의 디코더 출력). 남은 미확인은 11.6절.

## 9. MEDIA-B 작업 기록 (2026-11-18, P18-03 의 남은 변형)

작업자 MEDIA-B. 모든 시험은 JDK 6u45 로 직접 실행했다. 커밋 `20261117-540`~`543`(번호는 지시된 대역).

### 9.1 한 일

1. **EXIF 방향의 PNG(`eXIf`)·WebP(`EXIF`) 적용**(540): JPEG 의 EXIF 읽기를 공통 `media.image.ExifOrientation` 으로 일반화했다(두 가지 바이트 순서, TIFF 숫자 42 확인, `Exif\0\0` 머리말 허용, 손상은 1).
   `DecodedImage` 가 적용한 방향(`getAppliedOrientation`)과 되돌린 이미지(`unoriented()`, 한 번 만들어 보관)를 가진다. CSS `image-orientation: from-image | none` 속성을 새로 등록했고(`Prop.IMAGE_ORIENTATION = 304`, 상속)
   `none` 이면 `img` 의 자연 크기(`layout.OrientedContent`)와 그림, CSS 배경 이미지가 저장된 그대로의 모양을 쓴다. 규격: CSS Images 3 5.1절(<https://www.w3.org/TR/css-images-3/>, 편집본 2023-12-18, 조회 2026-11-18)의
   `[ <angle> || flip ]` 값은 선택 사항이고 폐기 예정이라 받지 않는다(Chromium 도 두 키워드만 지원). AVIF 의 `irot`/`imir` 은 컨테이너 규칙이라 `none` 의 영향을 받지 않는다.
2. **색 관리**(541): 새 패키지 `media.image.color`(`ColorProfile`, `ColorMath`, `Transfer`, `IccProfile`). 입력: ICC 프로파일(행렬/TRC `RGB `와 회색조 `kTRC`, 곡선 `curv`(항등/u8Fixed8 감마/표)와 `para` 0~4),
   PNG `cICP`>`iCCP`>`sRGB`>`gAMA`+`cHRM`(PNG 3판 우선순위, 쓸 수 없는 청크는 다음으로), JPEG APP2(여러 조각, 순서 번호 검사), WebP `ICCP`, BMP V4 보정된 RGB 와 V5 내장 프로파일, AVIF `colr`(`nclx`, `prof`, `rICC`; 프로파일 우선).
   변환 = 8비트 채널의 256항목 선형화 표 → 3x3 행렬(Bradford 적응으로 D65, sRGB 행렬의 역) → 16384 항목 부호화 표(sRGB OETF). 12MP 임의 픽셀에서 약 0.22초(JDK 6u45). sRGB 와 같은 프로파일(모든 채널이 한 단계 이내)은 `ColorProfile.SRGB` 이며
   건너뛴다(기존 시험의 화소가 바뀌지 않았다).
3. **AVIF 중첩 오버레이**(542): 세 단계까지(순환은 `UNSUPPORTED_FORMAT`).
4. **AV1 필름 그레인**(543): `Av1FilmGrain`(규격 5.9.30 매개변수, 7.18.3 합성). 출력 화면에만 더하고 참조 프레임은 그대로다.

### 9.2 결정과 근거

- **BT.709(1), SMPTE 170M(6), BT.2020(14, 15) 전달 특성은 sRGB 곡선으로 본다.** 규격 그대로(BT.709 OETF 의 역)가 아니라 Chromium 의 선택이다: `ui/gfx/color_space.cc` 의 `GetTransferFunction` 주석("use the same transfer function as sRGB, which will
  more closely match other media players")와 코드(조회 2026-11-18, <https://chromium.googlesource.com/chromium/src/+/refs/heads/main/ui/gfx/color_space.cc>). libzimg(ffmpeg `zscale`)는 같은 코드 값을 BT.1886 감마 2.4 처럼 변환하므로
  이 한 코드 값에서만 그 라이브러리와 다르다(원색 변환은 일치).
- **HDR(PQ/HLG)**: SDR 로 내려 보여 준다. PQ 는 절대 휘도(최대 10000 cd/m2)를 HDR 기준 백색 203 cd/m2(ITU-R BT.2408, Chromium 의 `ColorSpace::kDefaultSDRWhiteLevel = 203`)로 나누어 1.0 을 SDR 흰색으로 하고, HLG 는 장면 광에 BT.2100 의 시스템 감마
  1.2(공칭 최고 1000 cd/m2: 휘도^0.2 x 1000)를 곱한 뒤 같은 나눗셈을 한다. 203 cd/m2 를 넘는 하이라이트는 **단순히 자른다**(톤 매핑 곡선 없음). 이유: 모든 HDR 이미지가 sRGB 로 잘못 보이는 일을 없애는 것이 목적이고, 이 PC 에는 톤 매핑 기준 구현이 없어
  어떤 곡선도 기준과 비교할 수 없다. 기준 백색(PQ 약 58 %, HLG 75 %)이 흰색으로 나오는 것만 시험한다.
- **프로파일 읽기**: 행렬/TRC 와 회색조만. 룩업 테이블 프로파일(`A2B0` 계열), CMYK, Lab 연결 공간, 장치 연결, 추상, 이름 있는 색 프로파일은 `null`(= 샘플을 sRGB 로 본다). `wtpt`/`chad` 는 쓰지 않는다(행렬 프로파일의 색소는 정의상 D50 으로 적응되어 있고 skcms 도 같다).
  sRGB 영역 밖의 성분은 자른다(상대 색도 측정).
- **gAMA**: 1/감마가 2.2 에 0.1 이내이고 원색이 sRGB(또는 cHRM 없음)이면 sRGB 로 보고 변환하지 않는다(많은 도구가 sRGB 이미지에 gAMA 45455 를 붙인다). 아니면 순수 거듭제곱 곡선이다. `cHRM` 만 있으면 sRGB 곡선과 그 원색.
- **ImageIO 와 ICC**: ImageIO 의 JPEG 리더는 `readRaster` 에서도 어긋난 APP2 ICC 조각(빠진 조각, 개수 불일치)에서 실패한다. 브라우저는 그런 그림도 보여 주므로 ICC 세그먼트를 읽기 전에 빼고(`Info.withoutIcc`) 프로파일은 직접 읽는다.

### 9.3 검증 (JDK 6u45, `sh scripts/pkgtest.sh mb4 "media" "media" --filter <접두사>`, 렌더 시험은 `pkgtest` 의 render/layout/css 포함 판)

| 시험 | 건수 | 결과 |
| --- | --- | --- |
| `ExifOrientationTest` | 8 | 통과 |
| `WebpOrientationTest` | 4 | 통과 |
| `ImageOrientationPaintTest`(render) | 5 | 통과 |
| `color.ColorProfileTest`(ICC 직접 작성 프로파일, CSS Color 4 행렬 분수, **ffmpeg libzimg 대조값 6가지 원색 조합 x 7색**) | 12 | 통과 |
| `ColorImageTest`(PNG 우선순위/폴백/회색조/팔레트/APNG, JPEG APP2 조각, BMP V4/V5) | 9 | 통과 |
| `WebpColorTest` | 3 | 통과 |
| `AvifColorTest` | 2 | 통과 |
| `AvifOverlayTest` | 6(새 1) | 통과 |
| `Av1StreamTest` | 9 | 통과: 비트 단위 시험이 인트라 25 + 필름 그레인 24 = 49개 스트림을 libdav1d md5 와 대조 |
| 기존 `media.image.*` 시험 전체(avif 36, webp 24, Png 14, Jpeg 10, Apng 10, Bmp 8, Gif 10, Ico 5, ImageDecoders 8 등) | 211 | 통과(`svg` 56 은 아래 JRE 결함 때문에 따로 실행: 통과) |
| 기존 css/layout/render/media 전체(첫 커밋 직후) | 1311 | 통과, 0 실패, 13 건너뜀(Opus 벡터 12 + 제품 JAR 1) |

- **JRE 6u45 간헐 결함**: `media.image.svg` 시험을 반복 실행하면 `fontmanager.dll` 의 `EXCEPTION_ACCESS_VIOLATION` 으로 JVM 이 가끔 죽는다(이번 작업의 코드와 무관: SVG 코드를 바꾸지 않았고 같은 시험이 단독으로 통과한다). 인수인계에 적힌 알려진 결함과 같다.
- libzimg 대조: `ffmpeg ... color=c=...,format=gbrp,setparams=...color_primaries=P:color_trc=iec61966-2-1` 다음 `zscale=p=bt709:t=iec61966-2-1:m=gbr:r=pc` 로 변환한 값을 `ColorProfileTest.testMatchesLibzimg` 에 적었다(P3 D65, BT.2020, DCI-P3, BT.470BG, SMPTE 170M 원색과 선형 광의 BT.709 원색: 모두 ±1 이내).
- 필름 그레인 기준: `generation/make_grain_fixtures.py`(ffmpeg libaom 인코더 `film-grain-test=N`, libdav1d 디코더). 그레인을 끈 디코딩(`-export_side_data film_grain`)의 md5 와 다름을 확인해 기준 md5 가 그레인이 적용된 것임을 확인했다.

### 9.4 한계와 미확인

- **색 관리의 실제 브라우저 대비**: 위 libzimg 대조와 정의 대조뿐이다. Chromium 으로 같은 파일을 열어 화소를 비교하지는 못했다(전달 특성 1 의 선택은 Chromium 소스 근거).
- **HDR 톤 매핑**: 단순 클리핑(9.2). 실제 HDR 이미지(PQ/HLG) 파일로 눈으로 확인하지 못했다(합성 값 시험뿐).
- ICC 룩업 테이블 프로파일, CMYK ICC 는 변환하지 않는다. 16비트 PNG 는 8비트로 줄인 뒤 변환한다(정밀도 손실).
- JS 의 `img.naturalWidth/Height` 는 `image-orientation: none` 이어도 방향을 적용한 크기를 준다(CSS 레이아웃/그림은 따른다). `mask-border-source` 이미지는 `image-orientation` 을 따르지 않는다.
- 초해상도, 인트라 블록 복사, 계층 이미지, 인터 프레임(7절)은 구현하지 못했다. **→ 이후 MEDIA-C 가 모두 구현했다(10절).**

### 9.5 P18-03 / P18-11 체크 후보 판단

**(MEDIA-B 시점의 판단. MEDIA-C 로 상황이 바뀌었다: 10.6절.)** **체크하지 않는 것이 맞다(닫을 수 없다).** P18-03 은 "대상 표의 형식과 변형을 각각 구현"이어야 하는데 AVIF 의 인터 프레임(애니메이션 AVIF), 인트라 블록 복사, 초해상도, 계층 이미지가 구현되지 않았고 명시적 예외로 근거를 댈 수도 없다
(AVIF 애니메이션은 HTML 이 요구하는 이미지 형식의 정상 기능). 이번 작업으로 닫힌 변형: 방향 정보(JPEG/PNG/WebP/AVIF 전부 + CSS `image-orientation`), 색 관리(행렬/TRC·회색조 ICC, gamma, CICP, 보정된 BMP, HDR 의 SDR 표시),
AVIF 필름 그레인·중첩 오버레이. 남은 것과 필요한 일은 7절. P18-11(형식별 입력 자료/기대 값/실패 사례 기록, 비디오 외 미구현 없음)은 같은 이유로 닫을 수 없다.

### 9.6 변경 파일 (경로는 저장소 루트 기준)

- 제품: `media/image/{ExifOrientation,DecodedImage,PixelOps,PngDecoder,JpegDecoder,BmpDecoder}.java`, `media/image/webp/WebpDecoder.java`, `media/image/avif/{AvifDecoder,AvifColor,Av1Picture,Av1FilmGrain,Av1FrameHeader,Av1Decoder}.java`,
  새 패키지 `media/image/color/{ColorProfile,ColorMath,Transfer,IccProfile}.java`, `layout/{OrientedContent,BoxBuilder}.java`, `render/{ImageReplacedResolver,DisplayListBuilder,BackgroundLayers}.java`,
  `css/props/{Prop,PropertyTable}.java`(속성 `image-orientation` 추가, id 304 = 표의 마지막 행).
- 시험: `media/image/{ExifBlocks,ExifOrientationTest,ColorImageTest}.java`, `media/image/color/{IccBuilder,ColorProfileTest}.java`, `media/image/webp/{WebpOrientationTest,WebpColorTest}.java`,
  `media/image/avif/{AvifColorTest,AvifOverlayTest,Av1StreamTest}.java`, `render/ImageOrientationPaintTest.java`, 자료 `tests/resources/media/avif/av1/{grain_*.obu,index.txt}`, 생성 스크립트 `tests/resources/media/avif/generation/make_grain_fixtures.py`.
- 문서: `docs/progress/media-matrix.md`(이 절), `docs/FEATURE_MATRIX.md`(IMG-01/02/04/05/08/09).

### 9.7 조정자 요청

- `css/props/PropertyTable.java` 와 `Prop.java` 의 맨 끝에 `image-orientation`(id 304)을 더했다. 다른 작업자(CSS-J 등)도 표의 끝에 행을 더했다면 병합 충돌이 나며 해결은 행 순서를 정해 `Prop` 상수 번호를 다시 매기는 것이다
  (`tests/resources/css/generate-prop.sh` 가 `Prop.java` 를 다시 만들지만 손으로 단 javadoc 이 사라지므로 상수 번호만 고치는 것을 권한다; `PropertyTest` 가 일치를 확인한다).
- `docs/PROGRESS.md`/`AGENTS.md` 는 고치지 않았다. P18-03/P18-11 은 체크하지 말 것(9.5). AGENTS.md 의 P18-03 설명에 "AVIF 인터 프레임/인트라 블록 복사/초해상도/계층 이미지 남음"을 적어 두는 것을 권한다.
- 이 PC 에 ffmpeg(libaom/librav1e/libsvtav1/libdav1d/libzimg)가 있으므로 7절의 1~2번(초해상도, 인트라 블록 복사)은 기준 스트림과 비교하며 이어서 할 수 있다(시험 스트림 `aom_intrabc.obu` 가 이미 있다). 인수인계 문서의 "기준 디코더 없음" 서술을 고쳐 주기 바란다.

## 10. MEDIA-C 작업 기록 (2026-11-18, P18-03 의 남은 AVIF 변형 전부)

작업자 MEDIA-C(격리 worktree `agent-ad9dd37e3dbecb211`). 커밋 `20261117-550`~`558`(지시된 번호 대역). 모든 시험은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로
`sh scripts/pkgtest.sh mc "media" "media" --filter org.duckdns.hjow.swingwebpanel.media.image.avif` 처럼 직접 실행했다.

### 10.1 한 일 (작은 것부터, 커밋 순)

| 커밋 | 내용 |
| --- | --- |
| 550 | **인트라 블록 복사**(`allow_intrabc`): 움직임 벡터 예측 7.10.2(`Av1MvStack`), 정수 벡터 읽기와 유효성, 현재 프레임 복사 예측(`Av1InterPred`, 색차는 이중 선형), 인터 변환 트리, 이 모드에서 루프 필터/CDEF/루프 복원 끔. libdav1d 와 비트 단위로 같은 스트림 12개 |
| 551 | **초해상도**(`use_superres`): 규격 7.16 의 8탭 업스케일(`Av1Upscale`, 표 `UPSCALE_FILTER`는 규격에서 추출)과 루프 복원 열 위치 환산. SVT-AV1 이 만든 분모 9~16 스트림 16개가 일치 |
| 552 | **다중 프레임 기반**: 비압축 헤더 전체(참조 프레임 선택, 순서 힌트, 전역 움직임, 건너뛰기 모드, 분할 상태 갱신, 필름 그레인 상속), 참조 슬롯 8개(`Av1RefFrame`)와 `show_existing_frame`, CDF 저장/적재, 시간 단위별 디코딩 API |
| 553 | 인터 1단계: 단일 참조 이동 보상 시퀀스 |
| 554 | **인터 프레임 전체**: 단일/복합 참조, 이동 보상(`Av1InterPred`, 크기가 다른 참조), OBMC, 국소 워프(`Av1Warp`, 7.11.3.5/6/8)와 전역 워프, 인터인트라, 쐐기·차이 가중·거리 가중 복합(`Av1WedgeMasks`), 시간 움직임 벡터(`Av1MotionField`, 7.9/7.19), 건너뛰기 모드, 세그먼트 예측, 인터 변환 크기와 집합, 인터 프레임 루프 필터 규칙, 필름 그레인 상속. 시퀀스 39개 |
| 555 | 필름 그레인 크기 조절 함수 점이 증가하지 않는 손상 입력에서의 0 으로 나누기를 막는 검증과 손상 시퀀스 시험 |
| 556 | **애니메이션 AVIF**: `AvifContainer.Track`(moov/trak/mdia/stbl: stsd 의 av01 샘플 항목과 `av1C`/`colr`/`auxi`, stts, stsc, stsz, stco/co64, edts/elst, tref `auxl`/`prem`, mvhd/mdhd 시간 단위)과 `AvifDecoder.decodeSequence` |
| 557 | **계층 이미지**: `a1op`(동작 지점), `lsel`(공간 계층)을 `Av1Decoder.decodeLayer` 로 연결 |
| 558 | 문서(이 절)와 낡은 클래스 설명 정리 |

### 10.2 설계와 결정(근거)

- **규격 판본**: AV1 비트스트림 규격 <https://aomediacodec.github.io/av1-spec/>(2023-07-14 판, 조회 2026-11-18, `tools/webfetch`)의 5(구문), 6(의미), 7(복호 과정), 특히 7.9 움직임장, 7.10 움직임 벡터 예측, 7.11.3 인터 예측, 7.16 업스케일, 7.20/7.21 참조 프레임
  갱신/적재. 표(CDF 기본값, 필터 계수, 워프 필터, 마스크)는 규격 텍스트에서 `generation/gen_inter_tables.py`, `extract_tables.py` 로 추출했다. AVIF 규격 <https://aomediacodec.github.io/av1-avif/>(`Last-Modified: Mon, 08 Dec 2025`, 조회 2026-11-18)의
  2.3(계층 이미지: `a1op`, `lsel`, `a1lx`), 3(이미지 시퀀스), 4.1(보조 시퀀스). 구조(참조 슬롯, 상태 저장/적재, 움직임장)는 규격 기준이며 libdav1d 소스를 옮기지 않았다(비교는 디코딩 결과로만 했다).
- **디코더 구조**: `Av1Decoder` 는 인스턴스 하나가 시퀀스를 디코딩한다(참조 슬롯 8개, 순서 힌트 이어받기). 정지 이미지는 처음 표시되는 화면(계층이면 선택한 계층)에서 멈추고, 시퀀스는 시간 단위(`decodeTemporalUnit`)마다 호출한다.
  지원하지 않는 기능은 틀린 그림이 아니라 `UNSUPPORTED_FORMAT`/`CORRUPT` 로 실패한다(예: 필수 속성의 모르는 종류, 참조 프레임 형식/크기 비율 위반).
- **출력 규칙(공간 계층)**: AV1 규격 7.18 과 AVIF 2.3.1: 동작 지점에서 가장 높은 공간 계층의 화면. `lsel`(0~3)이 있으면 그 계층의 출력 화면, 0xFFFF 는 가장 높은 계층(점진적 표시는 하지 않는다), 없는 계층/없는 동작 지점/계층 번호 4 이상은 `CORRUPT`.
  동작 지점의 OBU 확장 헤더 `OperatingPointIdc` 로 다른 계층의 OBU 를 버린다. 시퀀스의 샘플(시간 단위)은 마지막 화면(가장 높은 계층)을 쓴다.
- **애니메이션 선택 규칙**: `avis` 브랜드이거나 기본 항목이 없으면 색 트랙(핸들러가 `auxv` 가 아니고 `auxl` 참조가 없는 첫 AV1 트랙)을 애니메이션으로 디코딩한다. 시퀀스가 `UNSUPPORTED_FORMAT` 으로 실패하고 기본 항목이 있으면 기본 항목을 정지 이미지로 보여 준다
  (프레임 크기가 바뀌는 시퀀스가 그 경우). 알파 트랙은 색 트랙을 `auxl` 로 참조하는 트랙이며 색 트랙의 `prem` 참조가 있으면 미리 곱한 알파다.
- **지속 시간·반복**: 프레임 지연 = 샘플 지속 시간(트랙 시간 단위)을 밀리초로 반올림, 0 이면 100 ms(렌더러가 10 ms 이하를 100 ms 로 보므로 같은 결과). 재생 횟수: 편집 목록(`elst`)의 반복 플래그가 있으면 무한(0), 아니면 첫 편집의 지속 시간을 샘플 합계로 나눈 값(최소 1).
  `media_time` 이 0 이 아닌 편집과 빈 편집(앞 지연)은 무시한다.
- **한도**: 트랙의 샘플 수 최대 2^20, `DecodeLimits` 의 프레임 수와 전체 픽셀 한도 적용, 샘플이 파일 밖이면 `CORRUPT`.

### 10.3 검증: libdav1d 와의 비트 단위 비교

방법: ffmpeg 의 libaom/librav1e/libsvtav1 로 `.obu` 를 만들고 libdav1d 가 디코딩한 프레임별 평면의 md5(`-noautoscale -f rawvideo`, 크기는 ffprobe)를 색인에 적어, 제품 디코더의 모든 프레임이 같은지 시험한다. 색인 `seq/index.txt` 의 39개 시퀀스, 321프레임.
어떤 도구가 실제로 쓰였는지는 `Av1ToolStats` 카운터로 증명한다(비트 단위로 같아도 도구가 한 번도 나오지 않았다면 근거가 약하므로 `Av1SequenceTest.testSequencesCoverTheTools` 가 확인). 39개 시퀀스 전체 합계(일회용 도구로 센 값):

| 도구 | 사용 횟수 | | 도구 | 사용 횟수 |
| --- | --- | --- | --- | --- |
| OBMC | 3506 | | 건너뛰기 모드 | 544 |
| 국소 워프 | 468 | | 크기가 다른 참조 | 1570 |
| 전역 워프 | 123 | | 시간 움직임 벡터 후보 | 51079 |
| 인터인트라 / 쐐기 인터인트라 | 55 / 61 | | 인터 프레임 안의 인트라 블록 | 3778 |
| 쐐기 복합 | 129 | | 세그먼트 | 718 |
| 차이 가중 복합 | 34 | | 복합 블록 전체 | 1509 |
| 거리 가중 복합 | 19 | | 평균 복합 | 1327 |
| **이중 필터(`enable_dual_filter`)** | **0** | | | |

시퀀스 구성: 기본/큰 움직임 두 가지 입력, 다중 참조와 altref(숨은 프레임과 `show_existing_frame`), 키 프레임 여러 개, OBMC/워프/전역 움직임/인터인트라/마스크 복합/거리 복합, 해상도 변경(`resize`), 초해상도(`superres`) 시퀀스, 타일, 128x128 슈퍼블록,
4:2:0/4:2:2/4:4:4/단색, 8/10/12비트, 홀수 크기, 무손실, 화면 콘텐츠, 오류 복원, 적응 양자화/델타 Q, 필름 그레인, rav1e/SVT-AV1 이 만든 스트림.

| 시험(`--filter org.duckdns.hjow.swingwebpanel.media.image.avif`, 2026-11-18 JDK 6u45) | 건수 | 결과 |
| --- | --- | --- |
| `Av1StreamTest`(정지 77개 스트림 비트 단위, 인트라 블록 복사·초해상도 도구 확인, 손상/잘린 입력) | 10 | 통과 |
| `Av1SequenceTest`(시퀀스 39개 비트 단위, 도구 사용 확인, 손상 시퀀스 560회) | 3 | 통과 |
| `AvifSequenceTest`(애니메이션 파일 11개: 프레임 픽셀, 지속 시간, 재생 횟수, 알파 트랙, 크기 바뀌는 시퀀스의 기본 항목 대체, 기본 항목 없음, 한도/취소, 손상 샘플 표 3가지, 잘라낸/바이트를 바꾼 파일 수백 건) | 7 | 통과 |
| `AvifLayerTest`(동작 지점·계층별 화면이 libdav1d 와 같음, 속성 조합 11개 파일, 잘못된 선택, 계층이 있는 시퀀스) | 5 | 통과 |
| 같은 접두사의 기존 시험(`AvifDecoderTest` 13, `Av1ComponentsTest` 6, `AvifOverlayTest` 6, `AvifColorTest` 2) | 27 | 통과 |

손상 입력 퍼징: `Av1Decoder.decodeAll` 에 시퀀스 바이트를 임의로(비트 뒤집기, 바이트 바꾸기, 헤더 부근 손상) 바꾼 입력 12,800건을 일회용 도구(저장소 밖)로 돌렸다(디코딩 성공 10,490 + `MediaException` 2,310). 이 퍼징이 `MediaException` 이외로 던지는 곳을
하나 찾았고(필름 그레인 크기 조절 함수 점이 증가하지 않을 때의 0 으로 나누기: 555 에서 수정) 수정 뒤에는 0건이다. 시험에는 같은 방식의 작은 판이 들어 있다(`Av1SequenceTest.testDamagedSequences` 560회).

### 10.4 시험 자료와 생성 방법 (모두 `tests/resources/media/avif/`, 생성 스크립트는 `generation/`)

| 경로 | 내용 | 스크립트 |
| --- | --- | --- |
| `av1/ibc_*.obu`, `sr_*.obu` | 인트라 블록 복사 12개(libaom 화면 콘텐츠, 8/10/12비트, 4:2:0/단색/무손실), 초해상도 16개(SVT-AV1) | `make_still_fixtures.py` |
| `seq/*.obu` + `seq/index.txt` | 인터 프레임 시퀀스 39개 + 프레임별 크기와 md5 | `make_seq_fixtures.py` |
| `avis/*.avif` + `avis/index.txt` | 위 시퀀스를 ffmpeg AVIF 먹서로 감싼 애니메이션 11개(알파 트랙, 편집 목록 반복 횟수 1·3, `meta` 상자 없음, 크기 바뀌는 시퀀스) | `make_avis_fixtures.py` |
| `layers/*.obu`, `layers/*.avif` + `layers/index.txt` | 두 공간 계층 스트림 2개와 `a1op`/`lsel` 조합 AVIF 11개, 계층 시퀀스 `avis_layers.avif` | `make_layer_fixtures.py` |

스크립트는 Python 이며 시험 자료 생성용이다(제품/시험 실행에는 필요 없음). 규격 표 생성기 `gen_inter_tables.py`/`extract_tables.py` 는 규격 URL 판본을 위 10.2 에 적었다.

### 10.5 한계와 미확인 (정직한 기록)

1. **이중 필터(`enable_dual_filter=1`)**: 구현했지만 비트 단위로 확인하지 못했다. 이 PC 의 libaom(ffmpeg 래퍼)은 `enable_dual_filter=1` 헤더를 쓰는 스트림을 만들지 않았고(`Av1ToolStats.DUAL_FILTER` 가 모든 시퀀스에서 0),
   시퀀스 헤더 한 비트를 뒤집은 스트림은 libdav1d 가 거부하거나 효과가 없었다. 읽기 구문은 규격 5.11.23 과 CDF 문맥(`interpFilterContext`) 그대로이며 단일 필터 모드는 확인했다.
2. **거리 가중 복합(`enable_jnt_comp`)**은 libaom 이 `-cpu-used 0` 에서만 만든다(`seq_jnt`: 19회). 다른 설정에서는 확인한 스트림이 없다.
3. **계층 이미지**: 이 PC 의 인코더 중 공간 계층을 만드는 것이 없어(ffmpeg 의 libaom 래퍼에 SVC 옵션 없음), `seq_keyframes` 의 키 프레임 두 장을 계층 0/1 로 조립하고 시퀀스 헤더에 동작 지점 둘(0x301, 0x101)을 쓴 합성 스트림으로 검증했다
   (`make_layer_fixtures.py`). libdav1d 가 이 스트림에서 `-oppoint 0/1`, `-alllayers 0/1` 로 낸 화면과 같다. **계층 간 예측(계층 1 이 계층 0 을 참조)과 계층마다 프레임 크기가 다른 실제 스트림은 확인하지 못했다**(참조 크기 조정 자체는 `seq_resize` 로 확인).
4. **애니메이션 AVIF 의 컨테이너**: 시험 파일은 ffmpeg 의 AVIF 먹서가 만든 것과 그 바이트 수정뿐이다. libavif(`avifenc`)나 브라우저가 만든 파일은 이 PC 에 없어 상호 운용은 확인하지 못했다(정지 이미지는 Pillow/libavif 파일 7개로 비교했다). 한 트랙에 샘플 항목이 둘 이상이거나
   트랙 헤더의 변환 행렬(회전)이 있는 파일, `media_time` 이 0 이 아닌 편집은 처리하지 않는다(무시).
5. **프레임 크기가 바뀌는 시퀀스**는 애니메이션으로 만들지 않는다(`UNSUPPORTED_FORMAT`; 기본 항목이 있으면 그것을 정지 이미지로, 없으면 실패). 브라우저는 같은 파일을 대개 첫 프레임 크기로 보여 주지만 규격(MIAF)은 이를 금지하므로 필요성이 낮다고 보았다.
6. 행렬 계수 3, 10(BT.2020 상수 휘도), 11, 12, 13, 14(ICtCp)는 변환하지 않는다(`UNSUPPORTED_FORMAT`). 이는 이전부터의 제한이며 이번에 바꾸지 않았다.
7. `large_scale_tile`(타일 목록 OBU)은 AVIF 에 쓰이지 않아 구현하지 않았고 OBU 를 무시한다.
8. **성능**: 큰 애니메이션의 디코딩 시간은 측정하지 않았다(시험 파일 11개 전체를 디코딩하는 데 약 0.5초; 최대 320x192). `DecodeLimits` 의 프레임 수·전체 픽셀 한도(기본 4096프레임, 2.56억 픽셀)와 취소(인터럽트: OBU 와 타일의 슈퍼블록마다 확인)를 지킨다.
9. 색 변환의 정확성은 정지 이미지 쪽 검증(Pillow/libavif 비교, libzimg 비교)을 따르며, 애니메이션 프레임은 같은 함수(`AvifColor.toArgb`, `ColorProfile`)를 쓴다. 애니메이션 프레임의 ARGB 를 외부 디코더 출력과 직접 비교하지는 않았다(YUV 평면은 비트 단위로 비교).

### 10.6 P18-03 / P18-11 체크 후보 판단

- **P18-03**: AVIF 쪽 남은 변형(인터 프레임, 인트라 블록 복사, 초해상도, 계층 이미지)이 모두 구현되고 검증되어(위 10.3) 9.5절이 막았던 이유가 사라졌다. **체크 후보로 판단한다.** 단, 조정자가 알아야 할 조건: ① 10.5 의 1(이중 필터), 3(실제 공간 계층 스트림), 4(다른 도구가 만든 애니메이션 파일)는
  확인하지 못했다. 이것을 "변형 구현"의 일부로 볼지는 조정자의 판단이다. ② 이미지 형식 쪽 다른 알려진 제한(JPEG 산술/무손실/12비트, BMP BI_JPEG/BI_PNG/RLE24, ICC 룩업 테이블 프로파일 등)은 9.5절에서도 닫는 이유로 삼지 않았으므로 같은 입장을 유지했다.
  내가 체크하지는 않았다(지시대로).
- **P18-11**: 체크하지 않는다. 이미지 쪽 대상 표에는 미구현이 없지만(위) 오디오 쪽은 P18-08 이 미체크이고(재생 장치 확인) AUD-04/05/06/09 가 시험용 인코더 기준으로만 검증되어 있다(8절). 비디오 예외를 제외한 대상 표 전체를 판정하는 것은 MEDIA 오디오 담당의 몫이다.

### 10.7 변경 파일 (경로는 저장소 루트 기준)

- 제품(`src/org/duckdns/hjow/swingwebpanel/media/image/avif/`): `Av1Decoder`(다시 작성: 인스턴스형, 참조 슬롯, 시간 단위 API, 동작 지점/계층), `Av1FrameHeader`, `Av1TileDecoder`(인터 블록 전부), `Av1CoeffReader`, `Av1Cdfs`, `Av1Frame`, `Av1LoopFilter`, `Av1Sequence`, `Av1FilmGrain`,
  `AvifContainer`(moov/trak), `AvifDecoder`(시퀀스, 계층), 새 클래스 `Av1MvStack`, `Av1InterPred`, `Av1Upscale`, `Av1RefFrame`, `Av1Warp`, `Av1WedgeMasks`, `Av1MotionField`, `Av1ToolStats`, `AvifInterTables`, `AvifInterCdfTables`.
- 시험(`tests/src/org/duckdns/hjow/swingwebpanel/media/image/avif/`): `Av1StreamTest`, `Av1SequenceTest`(새), `AvifSequenceTest`(새), `AvifLayerTest`(새). 자료와 스크립트는 10.4.
- 문서: `docs/progress/media-matrix.md`(이 절과 2·5·7·9절 표시), `docs/FEATURE_MATRIX.md`(IMG-09), `tests/resources/media/avif/README.md`.

### 10.8 조정자 요청

- `docs/PROGRESS.md`, `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/API_CONTRACT.md`, `scripts/**` 는 지시대로 고치지 않았다. 반영을 요청한다: ① `AGENTS.md` P18-03 설명의 "AVIF 인터 프레임/인트라 블록 복사/초해상도/계층 이미지 남음"(9.7절 권고)을 "구현·검증 완료(10절), 한계 10.5"로,
  ② `docs/ARCHITECTURE.md` 의 `media.image.avif` 클래스 목록에 위 새 클래스 추가, ③ 10.6 의 P18-03 체크 여부 결정.
- 병합할 때 충돌 가능성이 있는 곳: `docs/progress/media-matrix.md`(표 2·5절과 끝에 10절), `docs/FEATURE_MATRIX.md` IMG-09 행. 제품/시험 소스는 `media.image.avif` 안에서만 바꿨다(다른 패키지와 겹치지 않음).
- 전체 회귀: 10.9 참조.

## 11. MEDIA-D 작업 기록 (2026-11-18 작업자, 2026-11-19 조정자 마무리, P18-11 의 오디오 쪽)

목적: 오디오 디코더(WAV/AIFF/AU/MP3/AAC/Vorbis/Opus/FLAC/WebM)는 규격에서 직접 쓴 시험용 인코더로만 검증되어 있었다(3절 표의 옛 "시험용 인코더 기준"). 같은 사람이 인코더와 디코더를 쓰면 규격의 같은 오독이 둘 다에 들어가므로, **실제 인코더가 쓴 파일**과 **그 파일을 ffmpeg 의 디코더가 푼 참조 PCM** 으로 교차 검증했다.

### 11.1 한 일 (커밋 순)

| 커밋 | 내용 |
| --- | --- |
| 560 | 시험 자료 생성 스크립트 `tests/resources/media/audio/generation/make_audio_fixtures.py`(합성 신호, ffmpeg 로 인코딩과 참조 디코딩) |
| 561 | MP3 MPEG-2.5 8 kHz 짧은 블록의 영역 0 경계를 대역 폭에서 계산(11.4 의 1) |
| 562 | AAC 샘플링 주파수 인덱스 12, 채널 구성 11/12/14, 다채널 섞기 정책 통일, 탐색 때 PNS 잡음 발생기 재시작(11.4 의 2~5) |
| 563 | Vorbis/Opus 다채널 섞기의 채널 순서, WebM Vorbis 의 `DiscardPadding`(11.4 의 6~8) |
| 564 | WAV RF64/BW64, AIFF-C `ima4`(11.4 의 9~10) |
| 565 | `media.audio.reference.ReferenceDecodeTest`/`ReferenceFixture`/`ReferenceCompare` 와 시험 자료 |
| 566 | AAC 손상 블록이 범위 밖 값을 만들면 그 블록만 무음(11.4 의 11) |
| 567~568 | 손상 사본 시험, 시험 자료 추가, 거부해야 하는 형식 4개(Yamaha ADPCM WAV, WMA2 WAV, MPEG-1 Layer II, ALAC M4A), 생성기로 전체 재생성 |
| 568 WIP → 조정자 | MP3 탐색의 비트 저장소 사전 해독(11.4 의 12)과 그것을 드러내는 시험 자료 `mp3_v2_44100_st_gap`, `WavDecoder`/`AiffDecoder` 설명 갱신. 작업자가 중단된 상태에서 조정자가 시험의 위치 계산 결함(짧은 자료에서 탐색 위치가 끝을 넘어 `NegativeArraySizeException`)을 고치고 검증했다 |

### 11.2 시험 자료와 참조

- 만든 환경: Windows 10, `ffmpeg N-125875-g5d4d3bdc61-20260731`(libmp3lame, 기본 `aac`, libvorbis, libopus, `flac`, PCM/ADPCM 인코더), Python 3, 2026-11-18. 신호는 모두 합성(사인 스윕, 잡음 버스트, 무음, 풀 스케일 근처, 아주 작은 소리, 클릭; 선형 합동 생성기로 결정적)이라 저작권 있는 음원은 없다. 자세한 것은 `tests/resources/media/audio/README.md`.
- 색인 `tests/resources/media/audio/index.txt` 121줄: 비교 대상 117개(WAV 18, AIFF/AIFC 12, AU 8, FLAC 15 은 `exact`; MP3 16, AAC 25, Ogg Vorbis 9, Ogg Opus 12, WebM 2(Vorbis, Opus) 는 `lossy`)와 거부해야 하는 4개.
- 참조: `exact` 는 ffmpeg 가 푼 16비트 PCM 의 md5(24/32비트 정수는 제품과 같은 반올림으로 계산), `lossy` 는 ffmpeg 의 16비트 디코딩을 FLAC 으로 무손실 압축한 `ref/*.flac`(채널이 셋 이상이면 채널마다). Opus 참조는 libopus 의 부동 소수점 API 출력(16비트 API 의 소프트 클리핑을 피함). 참조 FLAC 은 제품의 FLAC 디코더로 읽으며, 그 디코더는 같은 시험의 `exact` FLAC 15개로 먼저 ffmpeg 와 비트 단위로 같음을 확인한다.
- 길이: 제품은 컨테이너가 알려 주는 대로 디코더 지연과 패딩을 지운 길이(`expected`, 갭리스)를 내야 한다. ffmpeg 의 길이가 다른 경우(예: Vorbis 의 마지막 그래뉼, ID3 가 있는 MP3)는 공통 구간만 비교하되 길이는 `expected` 와 같아야 한다.

### 11.3 허용 오차와 근거

| 대상 | 최대 차이 | 차이의 RMS | 근거 |
| --- | --- | --- | --- |
| `exact`(PCM/float/μ-law/A-law/ADPCM/FLAC, 2채널 이하) | 0 | 0 | md5 일치 |
| `exact` 다채널(16비트) / 24비트 다채널 | 1 LSB | 0.05 / 0.45 | 제품은 반올림 전 채널에서 섞고 참조는 채널마다 16비트로 반올림된 것에서 섞는다 |
| MP3, AAC, Vorbis(2채널 이하) | 2 LSB | 0.2887(= 1/√12) | ISO/IEC 11172-4 2.6.3 "full accuracy"(RMS < 2^-15/√12, 최대 ≤ 2^-14; 인용 https://www.underbit.com/resources/mpeg/audio/compliance, 2026-11-18 조회). AAC/Vorbis 는 구할 수 있던 자료에 같은 기준이 없어 MP3 의 기준을 같이 쓴다 |
| Opus(2채널 이하) | 1 LSB | 0.32 | libopus 와 같은 부동 소수점 디코딩이므로 연산 순서의 반올림 차이뿐 |
| 손실 다채널(섞은 결과) | 2 LSB | 0.6 | 위 다채널과 같은 반올림 누적(최대 8채널) |

측정값(JDK 6u45, 2026-11-19 조정자 실행): 모든 `exact` 파일 비트 단위 일치(24비트 7.1 FLAC 은 섞은 결과 최대 1, RMS 0.31). MP3 16개 최대 1, RMS 0.055~0.096. AAC 21개(2채널 이하) 최대 1, RMS 0.014~0.034, 다채널 4개 RMS 0.36~0.41. Vorbis 6개(2채널 이하) 최대 1, RMS 0.021~0.035, 다채널 3개 RMS 0.33~0.40. Opus 11개(2채널 이하) 최대 1, RMS 0~0.25(SILK 전용 다섯 개는 완전히 같음), 5.1 은 RMS 0.36. WebM 두 개 최대 1. 모두 길이가 `expected` 와 같고 시간 이동이 없다.

### 11.4 실제 파일에서 찾아 고친 결함과 빠진 기능

1. MP3 MPEG-2.5 8 kHz 짧은 블록: 영역 0 의 경계를 36선으로 고정하던 것을 대역 폭에서 계산(72선)하도록 고침(libmp3lame 파일에서 발견).
2. AAC 샘플링 주파수 인덱스 12(7350 Hz) 지원.
3. AAC 채널 구성 11/12/14 지원.
4. 다채널 섞기를 공통 믹서 정책으로 통일(4채널 이상에서 뒤 중앙 -3 dB 등).
5. AAC 탐색 때 PNS(지각 잡음 대체) 잡음 발생기를 다시 시작(처음으로 탐색하면 새 디코더와 같은 결과).
6. Vorbis 다채널(매핑 군 1)의 섞기를 Vorbis 채널 순서로 고침.
7. Opus 매핑 군 1 의 섞기를 Vorbis 채널 순서로 고침.
8. WebM 의 Vorbis 블록에 `DiscardPadding` 이 있으면 그 값으로 끝을 자름(ffmpeg 의 Matroska 먹서가 씀).
9. WAV 의 64비트 형태 RF64/BW64(`ds64` 청크) 지원.
10. AIFF-C `ima4`(QuickTime IMA ADPCM, 64프레임마다 채널별 34바이트) 지원.
11. AAC 손상 블록이 배열 범위 밖 값을 만들면 예외 대신 그 블록만 무음(손상 사본 시험에서 발견).
12. MP3 탐색: 목표 앞 두 프레임을 해독하려면 그 앞 프레임들의 주 데이터로 비트 저장소(최대 511바이트)가 채워져야 하는데, 작은 VBR 프레임(104바이트)이 이어지면 고정 8프레임 사전 해독으로는 모자랐다. `Mp3Source.prerollStart` 가 주 데이터가 충분히 모일 때까지(최대 64프레임) 시작점을 앞으로 옮긴다. 조정자 확인: 고치기 전 `Mp3Source` 로는 `mp3_v2_44100_st_gap` 의 23152 프레임 탐색 결과가 순차 디코딩과 달랐고(SNR 12.8 dB), 고친 뒤 같다.

### 11.5 시험과 결과

- `media.audio.reference.ReferenceDecodeTest` 15건: 형식별 비교 9건(`testWav/Aiff/Au/Flac/Mp3/Aac/Vorbis/Opus/WebmMatchesFfmpeg`), `testDecoderIsChosenBySignature`(MIME 힌트가 아닌 시그니처), `testSeekMatchesSequentialDecoding`(파일마다 여덟 위치로 탐색한 뒤 끝까지 순차 결과와 같음; AAC 는 중간 탐색에서 PNS 잡음 대역만 달라도 됨 — SNR 25 dB 이상), `testDamagedRealFilesFailCleanly`·`testDamagedHeadersFailCleanly`(자르기, 헤더·임의 바이트 변경, 0 블록: 디코딩되거나 `MediaException` 으로만 끝나고 제한 시간 안), `testRefusedFormatsAreReportedAsUnsupported`, `testIndexIsComplete`.
- JDK 6u45, 2026-11-19 조정자 실행: `scripts/pkgtest.sh md ... --filter org.duckdns.hjow.swingwebpanel.media.audio` 에서 176 통과/1 실패/12 건너뜀(실패는 위 11.1 의 시험 위치 계산 결함, 건너뜀은 선택적 Opus 원본 벡터 12건) → 고친 뒤 `ReferenceDecodeTest` 15 통과/0 실패. 통합 전체 회귀는 병합 때 PROGRESS 에 적는다.

### 11.6 확인하지 못한 것

- MP3: libmp3lame 은 인텐시티 스테레오와 혼합 블록을 쓰지 않아 실제 파일로 확인하지 못했다(16개 모두 `intensity_frames=0`, `mixed_granules=0`). 자유 형식 스트림과 Layer I/II 는 미지원(Layer II 는 `UNSUPPORTED_FORMAT` 로 확인).
- AAC: HE-AAC(SBR/PS)는 실제 파일이 없다(제품은 핵심 대역만). Main 예측/LTP 는 미지원.
- Vorbis: libvorbis 는 floor 1 만 쓰므로 floor 0 은 실제 파일로 확인하지 못했다.
- Opus: 패킷 손실 은닉은 libopus 와 비교하지 못했다.
- WebM: ffmpeg 의 먹서 파일뿐이고 브라우저가 만든 WebM 은 없다.
- AU 의 ADPCM(G.721/G.723)과 DSP 인코딩은 미지원 그대로.
- **소리를 귀로 확인하지 못했다**(P18-08, P22-10; 8절).

### 11.7 P18-11 판단

P18-11 이 요구하는 "형식별 입력 자료, 기대 픽셀 또는 PCM, 상태 변화와 실패 사례의 기록"은 이미지(5·9·10절)와 오디오(3절과 이 절)에 모두 있고, 비디오 예외를 뺀 대상 표에 미구현 행은 없다(남은 것은 위 11.6 과 10.5 의 명시한 한계). 그러나 P18-11 은 P18 단계의 마지막 항목이므로 계획의 규칙("단계의 마지막 완료 항목은 그 단계의 모든 작업과 검증이 끝나야 체크한다")에 따라 **P18-08(오디오 재생: 소리를 귀로 확인해야 함, 사람 필요)이 닫힐 때까지 체크하지 않는다**.

### 11.8 변경 파일

- 제품(`src/org/duckdns/hjow/swingwebpanel/media/audio/`): `ChannelMixer`, `aac/{AacBands, AacConfig, AacDecoder, AacFrameDecoder, AacSource}`, `mp3/{Mp3FrameDecoder, Mp3Source}`, `opus/OpusSource`, `vorbis/VorbisSource`, `webm/VorbisInOgg`, `pcm/{AdpcmSource, AiffDecoder, WavDecoder}`.
- 시험: `tests/src/org/duckdns/hjow/swingwebpanel/media/audio/reference/{ReferenceDecodeTest, ReferenceFixture, ReferenceCompare}`(새), 고친 시험 `opus/OpusDecoderTest`, `pcm/AiffAuDecoderTest`, `vorbis/VorbisDecoderTest`, 시험 자료 `tests/resources/media/audio/**`.
- 문서: 이 절, 3절 표, `docs/FEATURE_MATRIX.md` AUD-01~09, `docs/progress/{media-aud-a, media-aud-b, media-opus}.md` 의 갱신 표시.

## 12. 넓은 디코딩 경로 `openWide` (P25-05 B2, 2026-11-22)

Web Audio 의 `decodeAudioData` 를 위해 아홉 디코더(WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM)가 모두 `WideAudioDecoder.openWide` 를 구현한다. 이 경로는 **파일의 모든 채널을 표준 순서로, float 정밀도로** 내보내는 `FloatPcmSource` 를 돌려준다(클리핑 없음, 1 을 넘을 수 있음). `AudioDecoders.openWide` 는 넓은 경로가 없는 디코더를 16비트 `PcmFloatAdapter` 로 감싼다.
- **재생용 16비트 `PcmSource` 경로는 바뀌지 않았다.** 위 3절의 "3채널 이상은 `ChannelMixer` 로 스테레오로 다운믹스, 24비트 이상과 float 는 16비트로 반올림"은 **이 재생 경로에 한정된 서술**이다. 두 경로는 같은 디코딩 코어를 공유하며 2채널 이하에서는 넓은 경로를 16비트로 줄인 결과가 재생 경로와 비트 단위로 같다(시험 자료의 2채널 이하 파일 전부, 손실 코덱 포함).
- **채널 순서**: WAVEFORMATEXTENSIBLE/FFmpeg/Web Audio 4.2절 순서(FL FR FC LFE BL BR BC SL SR). Vorbis, Opus 매핑 군 1, AAC 는 이 순서로 바꾸고 WAV/FLAC/AIFF/AU 는 파일 순서를 그대로 둔다(표와 근거: `docs/progress/webaudio-b.md` 10.4).
- **정밀도(ffmpeg float 참조 대비 rms 개선)**: MP3 20~42배, AAC 260~420배, Vorbis 137배, Opus 1.03~1.08배(이 구현의 Opus 디코더가 libopus 와 약 0.3 LSB 다르고 원인은 규명하지 않음). 정수 표본은 `표본/2^(비트-1)` 과 비트 단위로 같다(FLAC 4~32비트).
- **다채널 검증**: 다채널 시험 자료 14개를 ffmpeg 의 채널별 참조와 비교해 정확한 형식은 최대 차이 0, 손실 코덱은 최대 0~1 LSB(rms 0.18 LSB 이하).
- **손상 정책**: 첫 프레임 뒤의 손상/잘림은 디코딩한 만큼 성공으로 돌려주고(`DecodeResult.isTruncated()`, 경고) 프레임이 하나도 없거나 헤더 손상, 지원하지 않는 변형, 한도 초과, 채널 32개 초과는 `EncodingError`. 표시 없이 짧게 디코딩되는 잘림이 있다(MP3, ADTS AAC, Ogg Vorbis/Opus, Ogg FLAC; `truncated` 표시를 시험한 것은 FLAC 네이티브뿐).
- 시험 `media.audio.wide.*`(5개 클래스, 13건)와 `media.webaudio.decode.*`, 시험 자료 `tests/resources/media/audio/{wide,trunc,generation}`(352,232 바이트, `generation/*.sh` 로 재생성, ffmpeg `N-125875-g5d4d3bdc61-20260731`). 상세와 한계는 `docs/progress/webaudio-b.md` 10절.
