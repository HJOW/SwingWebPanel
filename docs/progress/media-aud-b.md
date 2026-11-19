# MEDIA-AUD-B 진행 기록 (MP3, AAC/MP4, Opus, WebM)

> **갱신(MEDIA-D, 2026-11-18)**: 이 문서가 "시험용 인코더로만 검증"이라고 적은 한계는 `media-matrix.md` 11절에서 실제 인코더(ffmpeg 의 libmp3lame, 기본 AAC, libvorbis, libopus, flac)가 만든 파일과 ffmpeg 의 참조 PCM 으로 다시 확인했다. 그 과정에서 찾아 고친 결함과 남은 한계는 11.4~11.6절을 따른다.

담당 경로: `src/org/duckdns/hjow/swingwebpanel/media/audio/{mp3,aac,opus,webm,mp4}/**`, `tests/src/org/duckdns/hjow/swingwebpanel/media/audio/{mp3,aac,opus,webm,mp4}/**`, 이 문서.
시험 실행: `scripts/pkgtest.sh mab "media/audio/mp3 media/audio/aac media/audio/mp4 ..." "media/audio/mp3 media/audio/aac media/audio/mp4 ..."` (JDK 6u45).
레지스트리(AUD-A 의 `AudioDecoders`)는 `org.duckdns.hjow.swingwebpanel.media.audio.{mp3.Mp3Decoder, aac.AacDecoder, opus.OpusDecoder, webm.WebmDecoder}` 를 리플렉션으로 찾으며
`getFormatName()` 은 각각 `mp3`, `aac`, `opus`, `webm` 이다.

## 현재 상태

| 항목 | 상태 | 시험 |
| --- | --- | --- |
| MP3 (AUD-04) | 구현·검증 완료(시험용 인코더 기준) | `Mp3DecoderTest` 13건(자체 시험 7건 별도) |
| MP4 역다중화(AUD-05 의 컨테이너) | 구현·검증 완료 | `Mp4FileTest` 4건 |
| AAC (AUD-05) | 구현·검증 완료(시험용 인코더 기준). HE-AAC 는 SBR 없이 핵심만 | `AacDecoderTest` 13건 |
| WebM/Matroska 역다중화 + Vorbis 트랙 (AUD-09 일부) | 구현·검증 완료(시험용 생성기 기준) | `WebmDecoderTest` 5건 |
| Opus (AUD-08) | **미구현**(시작하지 않음). `OpusDecoder` 클래스가 없으므로 레지스트리는 Opus 를 인식하지 않는다 | - |
| WebM 의 Opus 트랙 (AUD-09 나머지) | **미구현**: `A_OPUS` 트랙은 `UNSUPPORTED_FORMAT` | - |

## 출처와 라이선스 기록

- MP3: Huffman 트리(`Mp3Tables.HUFFMAN_TREE`)와 합성 윈도 D[i] 는 `github.com/hajimehoshi/go-mp3`(Apache License 2.0, Copyright 2017 Hajime Hoshi, PDMP3 파생) 의
  `internal/huffman/huffman.go`, `internal/frame/frame.go` 에서 **스크립트로 추출**했다(조회일 2026-11-16, `tools/webfetch`; 추출 명령은 이 문서 하단). 스케일팩터 대역 폭은 `minimp3`(CC0)에서 가져왔다.
  구조(부가 정보, 스케일팩터, 정지/시작 윈도, 스테레오 처리)는 go-mp3 와 minimp3 를 참고해 표준 알고리즘으로 다시 작성했다.
- AAC: Huffman 코드북(`AacCodebooks`)과 스케일팩터 대역 오프셋(`AacBands`)은 `github.com/DV8FromTheWorld/JAADec`(JAAD, 퍼블릭 도메인 선언) 에서 스크립트로 추출했다(조회일 2026-11-16).
  디코더 구조(`AacFrameDecoder`)는 JAAD 의 ICStream/ICSInfo/FilterBank 를 참고해 다시 작성했다. IMDCT(`AacImdct`)는 같은 저장소의 `vorbis.Imdct`(AUD-A, 패키지 비공개) 알고리즘을 옮겨 적었다.
- 코드를 JRE 6 에서 컴파일/실행해 검증했다(JDK 6u45).

## 정책/결정

- 출력은 16비트 부호 있는 리틀 엔디언 인터리브 PCM, 모노 또는 스테레오(3채널 이상은 스테레오로 섞음).
- MP3 는 Layer III 만 지원한다(Layer I/II 는 인식하되 `UNSUPPORTED_FORMAT`; 브라우저도 MP3 를 Layer III 로만 재생). 자유 형식 스트림은 미지원. LAME 태그가 있으면 인코더 지연+디코더 지연(529)과 끝 패딩을 잘라 정확한 길이를 낸다.
- AAC: ADTS 와 MP4/M4A 지원, ADIF 는 미지원(브라우저가 재생하지 않음). Main 프로파일 예측, LTP, 이득 제어, 결합 채널, 960선 프레임은 `UNSUPPORTED_FORMAT`/프레임 무음. HE-AAC(SBR/PS)는 AAC-LC 핵심만 해독하므로 핵심 샘플링 주파수(예: 24 kHz)로 대역이 줄어든 채 재생된다(FEATURE_MATRIX 의 "HE-AAC 는 확장" 정책).
- 해독할 수 없는 프레임(손상, 미지원 도구)은 무음으로 대체하고 계속한다. 스레드가 인터럽트되면 `MediaException(CANCELLED)` 를 던지고 인터럽트 플래그는 유지한다.
- 탐색: MP3 는 8 프레임 앞에서, AAC 는 1 블록 앞에서 해독을 시작해 필터/겹침/비트 저장소 상태를 복원한다. 시험에서 탐색 뒤 샘플이 연속 해독과 비트 단위로 같음을 확인했다.

## 검증 방식과 한계(정직한 기록)

- 실제 MP3/AAC 파일을 시험에 쓰지 못했다(바이너리를 내려받지 않는다는 규칙). 대신 **시험용 인코더**를 표준의 분석 알고리즘으로 따로 작성했다.
  - `Mp3TestEncoder`: 다상 분석(C = D/32), 정방향 MDCT(긴/시작/짧은/정지 윈도), 얼라이어스 감소, 전체 이득/스케일팩터 양자화, 허프만 부호화(디코더의 트리를 따라가 부호 표를 만듦), 부가 정보, 비트 저장소(main_data_begin), MS 스테레오, MPEG-1/2/2.5.
    디코더와 코드를 공유하는 것은 허프만 트리 표와 합성 윈도 표뿐이다(분석 필터의 완전 복원 성질로 합성 윈도 표를 함께 확인: 분석+합성 지연 1057 샘플에서 SNR 50 dB 이상).
  - `AacTestEncoder`: 정방향 MDCT(ISO 14496-3 의 계수 2 포함, 사인/KBD 윈도, 긴/시작/짧은(그룹)/정지), 스케일팩터, 구간 데이터, 코드북 11(이스케이프), M/S, TNS 분석 필터, 펄스 데이터, ADTS.
    디코더와 공유하는 것은 허프만 코드 표뿐이다.
  - `Mp4TestBuilder`: ISO 14496-12 에서 직접 쓴 MP4/M4A(편집 목록, 청크당 여러 샘플, 조각화) 생성기.
- 이 방식은 "인코더와 디코더가 같은 오해를 하지 않는다"는 한도 안에서의 검증이다. 실제 인코더(LAME, FDK/Nero/ffmpeg AAC)가 만든 스트림으로의 확인은 하지 못했다. 특히 다음은 시험용 인코더로 만들지 않았거나 만든 스트림에서만 확인했다:
  - MP3: 인텐시티 스테레오, 혼합 블록, 비트 저장소 중 일부 경계, 자유 형식, Layer I/II, count1 영역(시험용 인코더는 count1 을 쓰지 않고 큰 값 쌍으로 부호화) — 디코딩 경로는 있으나 시험용 스트림으로 확인하지 않았다.
  - AAC: 인텐시티 스테레오, PNS(잡음 대체)는 디코딩 경로만 있고 시험용 스트림으로 확인하지 않았다. 코드북 1~10 은 시험용 인코더가 쓰지 않는다(코드북 11 만) — 코드북 표의 완전성은 Huffman 트리 구성이 모든 행에 대해 충돌 없이 성공하는 것으로만 확인한다.
- 이전 판단: 시험 시간은 큰 편이다(AAC 약 60초, 시험용 인코더의 정의식 MDCT 때문).

## 추출 명령(참고)

```text
go-mp3:  huffmanTable = internal/huffman/huffman.go 24..302 행(0x.... 값), D 표 = internal/frame/frame.go 의 synthDtbl(512 값; 값*65536 을 반올림해 정수 표로 저장)
JAAD:    huffman/Codebooks.java 의 HCB1..HCB11, HCB_SF 를 awk 로 메서드(hcbN())로 변환, syntax/ScaleFactorBands.java 의 SWB_OFFSET_* 를 awk 로 추출
```

## 체크박스 후보

- AUD-04(MP3): 위 한계를 전제로 구현·검증 완료. AUD-05(AAC): ADTS/MP4 구현·검증 완료(HE-AAC 확장은 SBR 제외).
  P18-07 은 AUD-04~09 가 모두 끝나야 체크할 수 있다.

## 중단 시점의 상태와 재개 방법 (2026-11-16, 사용자 중단 요청)

- 마지막 전체 실행: `scripts/pkgtest.sh mab "media/audio/mp3 media/audio/aac media/audio/mp4 media/audio/webm media/audio/vorbis media/audio/ogg" "media/audio/mp3 media/audio/aac media/audio/mp4 media/audio/webm"` → **42 passed / 0 failed / 0 skipped**(자체 시험 7건 포함: MP3 13, AAC 13, MP4 4, WebM 5). sourcecheck 내 경로 0 위반.
- 구현·검증됨: MP3(Layer III), AAC-LC(ADTS, MP4/M4A, HE-AAC 는 핵심만), MP4 역다중화(조각화 포함), WebM/Matroska 역다중화(레이싱 3종, 크기 모름 세그먼트/클러스터, BlockGroup)와 Vorbis 트랙(메모리 Ogg 다리: `VorbisInOgg`, 블록 크기로 그래뉼을 정확히 계산, 모드 표는 설정 헤더 뒤에서부터 탐색).
- **WebM 의 Vorbis 길이**: WebM 에는 Ogg 의 마지막 그래뉼 자르기 정보가 없어 마지막 블록 경계까지(최대 한 블록) 더 길 수 있다(시험이 확인한 한계). **갱신(MEDIA-D)**: 블록에 `DiscardPadding` 이 있으면(ffmpeg 의 Matroska 먹서가 Vorbis 에도 쓴다) 그 값으로 끝을 자른다(`media-matrix.md` 11.4절 8번).
- 미구현: **Opus 전체**(Ogg Opus, WebM/MP4 의 Opus). 소스도 시험도 없다(`media/audio/opus` 디렉터리 없음).
- 재개 순서(권장):
  1. libopus(BSD-3, `github.com/xiph/opus`)의 `celt/` 와 `silk/` 소스를 `tools/webfetch` 로 읽는다(RFC 6716 이 본문이며 표는 `silk/tables_*.c`, `celt/modes.c`, `celt/static_modes_float.h`). 표는 이번 MP3/AAC 처럼 awk 로 Java 배열로 추출한다(손으로 옮기지 않는다).
  2. `opus.OpusRangeDecoder`(RFC 6716 4.1) → `opus.CeltDecoder`(대역 에너지, 할당, PVQ, 정규화 / IMDCT / 포스트필터) → `opus.OpusPacket`(TOC, 프레임 수, 코드 0~3) → `opus.OpusDecoder`(Ogg Opus: `OggReader`/`OggPacketStream` 사용, 사전 건너뛰기, 출력 이득, 48 kHz) 순으로 구현하고 `BlockPcmSource` 를 상속한다. SILK/하이브리드는 그 뒤.
  3. 시험은 `OggMux`(AUD-A 시험 도구)로 Ogg Opus 를 만들고, CELT 부분집합을 쓰는 시험용 인코더(범위 인코더 + Laplace/PVQ 부호화)로 만든다. 실제 Opus 파일이 없으므로 한계를 이 문서에 계속 기록한다.
  4. `WebmDecoder.open` 에 `A_OPUS` 분기를 추가한다(CodecPrivate 의 OpusHead, CodecDelay=사전 건너뛰기, SeekPreRoll 80 ms). `webm/WebmFile.Track` 은 이미 이 정보를 제공한다.
  5. 레지스트리는 클래스 이름(`org.duckdns.hjow.swingwebpanel.media.audio.opus.OpusDecoder`)만 찾으므로 다른 연결은 필요 없다. `getFormatName()` 은 `opus`.

## 조정자 요청

- 없음.
