# MEDIA-OPUS 진행 기록 (Opus 디코더, Ogg Opus, WebM A_OPUS)

> **갱신(MEDIA-D, 2026-11-18)**: 이 문서가 "시험용 인코더로만 검증"이라고 적은 한계는 `media-matrix.md` 11절에서 실제 인코더(ffmpeg 의 libmp3lame, 기본 AAC, libvorbis, libopus, flac)가 만든 파일과 ffmpeg 의 참조 PCM 으로 다시 확인했다. 그 과정에서 찾아 고친 결함과 남은 한계는 11.4~11.6절을 따른다.

담당 경로: `src/org/duckdns/hjow/swingwebpanel/media/audio/opus/**`, `tests/src/org/duckdns/hjow/swingwebpanel/media/audio/opus/**`, `tests/resources/media/opus/**`, 이 문서.
(`AudioDecoders`, `ogg/**`, `webm/**` 에는 Opus 를 연결하는 최소한의 추가만 한다.)
시험 실행: `sh scripts/pkgtest.sh opus "media/audio" "media/audio/opus" --filter org.duckdns.hjow.swingwebpanel.media.audio.opus` (JDK 6u45).
전체 RFC 6716 벡터까지 확인하려면 원본 파일 디렉터리를 `SWP_OPUS_VECTORS` 환경 변수로 준다(아래 "검증" 참조).

## 현재 상태 (요약)

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| 범위 디코더(RFC 6716 4.1), 패킷 계층(TOC, 묶음 코드 0~3, 자체 구분 프레이밍) | 구현·검증 | `OpusRangeDecoderTest`, `OpusPacketTest` |
| CELT(PVQ, 에너지, 할당, 역 MDCT, 후처리 필터, 반복 방지 잡음, 디엠퍼시스, 모노/스테레오 변환, 손실 은닉(잡음 대체)) | 구현·검증 | RFC 6716 벡터 12개 |
| SILK(LTP, LSF, 스테레오 예측, 리샘플러, PLC, CNG, 중복 프레임) | 구현·검증 | RFC 6716 벡터 12개 |
| 하이브리드, CELT/SILK 전환(중복 프레임, 교차 페이드) | 구현·검증 | 벡터 5, 6, 8, 9, 10, 12 |
| Ogg Opus(RFC 7845), WebM `A_OPUS` | 아래 진행 기록 참조 | |

## 작업 기록 (최신이 위)

### 작업 2~4: Ogg Opus, WebM `A_OPUS`, RFC 8251 수정, 견고성 (커밋 502~504, 조정자가 2026-11-17 병합)

작업자가 문서에 남기지 못하고 중단되어 조정자가 커밋과 시험 코드를 읽고 정리했다(작업자의 한계 서술은 아래 "한계"를 따른다).
- **Ogg Opus(RFC 7845)** `OpusHead`(헤더 해석·오류), `OpusInOgg`(OpusHead/OpusTags 와 오디오 패킷, 사전 건너뛰기, 출력 이득, 끝 자르기, 체인된 스트림, 페이지에 걸친 패킷),
  `OpusMultistream`(채널 매핑 0/1: 스테레오, 5.1 다중 스트림, 무음/중복 채널), `OpusSource`(탐색, 취소, 크기 제한), `OpusDecoder`(레지스트리 진입점, 시그니처 판별).
  시험 `OpusDecoderTest` 16건: 스테레오+사전 건너뛰기, 시그니처 판별, 끝 자르기, 출력 이득, 모노, 5.1 다중 스트림, 채널 매핑의 무음/중복, 체인(스테레오/모노 섞임), 탐색, 손상 패킷, 헤더 오류, 잘림·손상 파일, 취소, 크기 제한, 페이지에 걸친 패킷.
- **WebM `A_OPUS`**: `WebmDecoder`/`WebmFile` 에 `A_OPUS` 분기(CodecPrivate 의 OpusHead, CodecDelay, DiscardPadding, SeekPreRoll). 시험 `OpusWebmTest` 6건(Opus 트랙, CodecPrivate 없음, DiscardPadding, 블록 배치, 탐색, 레지스트리/오류).
- **RFC 8251**(하이브리드 접기, 모노 위상 반전 무시, 패딩 넘침 방지): `CeltBands`/`CeltDecoder`/`OpusFrameDecoder`/`SilkNlsf` 수정. 벡터 05/06/12 의 RFC 8251 참조 출력 발췌본과 비교한다(`testRfc8251Vector05/06/12`, 접기 스위치의 효과 `testFoldingSwitchChangesHybridOutput`).
- **견고성** `OpusRobustnessTest` 5건: 무작위 패킷, 상수 페이로드, 잘림·손실 패킷, 스트림 전환, 거대한 패딩 주장.
- 시험 벡터 시험은 두 벡터씩 6개 + RFC 8251 6개로 나뉜 `testCompleteVectors…IfAvailable`(원본 전체 `SWP_OPUS_VECTORS` 가 있을 때만)과 발췌본 시험으로 구성된다. 발췌본 시험은 제한 시간 안에 끝난다.
- **조정자 검증(2026-11-17, JDK 6u45, `scripts/snaptest.sh`)**: master(`20261117-061`) 위로 rebase 한 뒤 전체 회귀 **2470 통과 / 실패 0 / 건너뜀 13**(건너뜀: 원본 벡터가 있을 때만 도는 12개 + 제품 JAR 시험 1개).
  `tools/sourcecheck` 0 위반. 병합: master 로 fast-forward(`cc7ace4`). 원본 벡터 전체 시험(`SWP_OPUS_VECTORS`)은 조정자가 다시 돌리지 않았다(작업 1 의 작업자 실측만 있음).

### 작업 1: Opus 코어 디코더 (P18-07 Opus 트랙)

- 완료한 내용: RFC 6716 부록 A 참조 디코더(float 구성)의 순수 Java 이식.
  - `OpusRangeDecoder`(엔트로피 디코더), `OpusPacket`(TOC/프레임 묶음), `OpusFrameDecoder`(opus_decoder.c: 모드 선택, 중복 프레임, 전환, 손실),
    `Celt*`(CELT: `CeltDecoder`, `CeltBands`, `CeltRate`, `CeltEnergy`, `CeltPvq`, `CeltMdct`, `CeltMath`, `CeltTables`),
    `Silk*`(SILK: `SilkDecoder`, `SilkChannel`, `SilkNlsf`, `SilkResampler`, `SilkMath`, `SilkTables`, `SilkNlsfTables`).
  - 출력 주파수는 48 kHz 고정(SILK 내부 8/12/16 kHz 를 `SilkResampler` 로 48 kHz 로 올림). 디코더 채널은 1 또는 2.
- **검증 결과(실측)**: RFC 6716 A.4 의 시험 벡터 testvector01..12 (`.bit`/`.dec` 24개의 SHA1 이 RFC 목록과 모두 일치함을 확인) 전체를
  `opus_demo -d 48000 2` 와 같이 스테레오 48 kHz 로 디코딩했다(JRE 6u45, 스크래치 구동 프로그램).
  - 패킷 약 21,000개 전부에서 **최종 범위 디코더 상태가 인코더 값과 일치**(불일치 0): 모든 엔트로피 부호화 기호가 정확히 디코딩됨.
  - 출력 PCM 은 참조 `.dec` 와 SILK 전용 벡터(2, 3, 4, 12)는 **비트 단위로 같고**, CELT/하이브리드 벡터는 최대 1 LSB 차이(부동소수점 반올림),
    신호 대 오차비 100.6~109.0 dB.
- 이 디코더가 구현한 것은 RFC 6716 의 참조 디코더 float 구성이다. 정확한 비트 일치는 요구되지 않으며(RFC 6716 6장: `opus_compare` 품질 지표) 이 구현은
  그보다 엄격한 기준(최대 1 LSB)을 만족한다. RFC 8251 의 새 벡터(`opus_testvectors-rfc8251.tar.gz`)는 내려받아 두었으나 이 단계에서는 사용하지 않았다.

## 출처와 라이선스 기록

- RFC 6716 (Valin, Vos, Terriberry, 2012-09), 부록 A "Reference Implementation": `https://www.rfc-editor.org/rfc/rfc6716.txt` 를 `tools/webfetch`(JRE 25 로 실행,
  HTTPS 는 JRE 6 TLS 한계 때문)로 내려받았다(조회일 2026-11-17, 977,743 바이트). 부록 A.3 의 base64 소스 묶음을 추출했다
  (`cat rfc6716.txt | grep '^   ###' | sed -e 's/...###//' | base64 --decode > opus-rfc6716.tar.gz`, SHA1 86a927223e73d2476646a1b933fcd3fffb6ecc8c 가 RFC 의 값과 일치).
  이식은 이 소스(celt/*.c, silk/*.c, src/opus_decoder.c, src/opus_compare.c)의 구조와 알고리즘을 Java 로 옮긴 것이므로 BSD 3조항 라이선스의 고지를 보존한다:
  > Copyright (c) 2001-2011 Xiph.Org, Skype Limited, Octasic, Jean-Marc Valin, Timothy B. Terriberry, CSIRO, Gregory Maxwell, Mark Borgerding, Erik de Castro Lopo.
  > Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: Redistributions of
  > source code must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in binary form must reproduce the above
  > copyright notice ... Neither the name of Internet Society, IETF or IETF Trust, nor the names of specific contributors, may be used to endorse or promote
  > products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" ...
  (전체 문구는 RFC 소스 묶음의 `COPYING` 과 RFC 6716 의 라이선스 절.) 이 라이선스를 따르는 Java 파일: `src/org/duckdns/hjow/swingwebpanel/media/audio/opus/` 아래의 모든 파일
  (`OpusRangeDecoder`, `OpusPacket`, `OpusFrameDecoder`, `Celt*`, `Silk*`), 테스트 `OpusRangeEncoder`, `OpusCompare`.
- 표 `CeltTables`, `SilkTables`, `SilkNlsfTables` 는 위 소스의 `celt/static_modes_float.h`, `celt/modes.c`, `silk/tables_*.c`, `silk/pitch_est_tables.c`,
  `silk/table_LSF_cos.c` 에서 **스크립트로 추출**했다(손으로 옮기지 않음). 추출 스크립트는 작업 중 임시 디렉터리의 Python 파일이었으며
  (`tools/` 에 두지 않음: 도구는 Java 6 이어야 한다는 규칙 때문. 조정자가 필요하면 Java 도구로 재작성 요청 가능), 방법은 다음과 같다:
  C 소스에서 `static const ... NAME[...] = { ... };` 를 정규식으로 잘라 숫자를 평가해 Java `int[]`/`float[]` 로 쓴다. 에너지 확률 표 `E_PROB_MODEL`
  (`CeltEnergy`)은 `celt/quant_bands.c` 에서 읽어 옮긴 뒤 같은 정규식으로 추출한 숫자와 대조해 336개가 모두 같음을 확인했다.
- 시험 벡터: `https://opus-codec.org/static/testvectors/opus_testvectors.tar.gz` (39,001,148 바이트, 조회일 2026-11-17, RFC 6716 의 벡터).
  `tests/resources/media/opus/vectors/` 에는 벡터마다 **패킷 앞부분 `.bit` 와 참조 출력 일부 구간 `.dec.gz`**(gzip, 구간 시작/길이는 `vectors.properties`)만
  넣었다(합계 약 2.0 MB). 원본에서 자른 발췌본이며 내용은 바꾸지 않았다. 라이선스: opus-codec.org 의 시험 벡터 페이지는 코드의 BSD 라이선스를 따로 명시하지
  않고 사이트 전체를 CC BY 3.0 으로 표시한다. 벡터 파일 자체의 재배포 조건은 명시된 것이 없어 확인하지 못했다. **조정자 판단 필요**: 재배포가 부적절하면
  `tests/resources/media/opus/vectors/` 를 지우면 `OpusVectorTest` 의 벡터 12개가 실패하므로(자료 없음) 그때는 시험을 `SWP_OPUS_VECTORS` 전용으로 바꾸어야 한다.

## 검증 방식과 한계(정직한 기록)

- `OpusVectorTest`: 저장소의 발췌본에서 패킷별 최종 범위(전부 일치해야 함), 참조 출력 구간과의 최대 샘플 차이(≤ 2 허용, 실측 ≤ 1), 신호 대 오차비(≥ 80 dB),
  `opus_compare` 품질(≥ 99 %, RFC 의 합격선은 0 %)을 확인한다. `testCompleteVectorsIfAvailable` 은 `SWP_OPUS_VECTORS` 디렉터리(원본 24개 파일)가 있을 때
  전체 벡터로 같은 검사를 하며 없으면 이유와 함께 건너뛴다.
- **실제 libopus 가 만든 일반 Opus 파일(.opus)은 확인하지 않았다**(바이너리를 내려받지 않음). 시험 벡터는 디코더의 모든 측면을 건드리도록 만든 합성 비트스트림이다.
- 구현하지 않았거나 참조와 다른 점: CELT 의 피치 기반 손실 은닉(`celt_decode_lost` 의 loss_count < 5 경로)은 이식하지 않고 잡음 대체 경로로 모든 손실을 은닉한다.
  (참조 디코더는 이 경로에서 이력 버퍼를 밀지 않는 결함이 있어 밀도록 고쳤다.) 시험 벡터에는 손실 패킷이 없으므로 손실 은닉 경로는 비트 단위 검증을 하지 못했다.
  비트스트림 디코딩에는 영향이 없다. FEC(LBRR) 복구 디코딩과 디코더 API 샘플링 주파수 8/12/16/24 kHz 출력은 구현하지 않았다(48 kHz 고정).

## 조정자 요청

- 없음(아직). 컨테이너 연결 뒤에 FEATURE_MATRIX AUD-08/AUD-09 문구 제안을 추가한다.

## 2026-11-26 원본 벡터 재검증

JRE 25에서 `tools/webfetch`로 원본을 다시 받았다. 두 서버 응답의 Last-Modified는 2024-04-12였다. 압축 아카이브와 푼 벡터는 로컬 `build`에 두었다.

| 벡터 | 원본 | SHA-256 |
| --- | --- | --- |
| RFC 6716 | [공식 아카이브](https://opus-codec.org/static/testvectors/opus_testvectors.tar.gz), 39,001,148바이트 | `94ac78ca4f74c4e43bc9fe4ec1ad0aa36f38ab90f45b0727c40dd1e96096e767` |
| RFC 8251 | [공식 아카이브](https://opus-codec.org/static/testvectors/opus_testvectors-rfc8251.tar.gz), 74,624,664바이트 | `6b26a22f9ba87b2b836906a9bb7afec5f8e54d49553b1200382520ee6fedfa55` |

`SWP_OPUS_VECTORS`를 `build/opus_testvectors`로, `SWP_OPUS_VECTORS_8251`을 `build/opus_newvectors`로 지정해 JRE 6u45에서 패키징된 JAR의 `OpusVectorTest`를 실행했다. 원본 벡터를 쓰는 12건을 포함해 28건이 통과했다(실패 0, 건너뜀 0).

## 체크박스 후보

- 없음(Ogg Opus / WebM 연결과 검증이 끝나면 P18-07 의 Opus 부분).
