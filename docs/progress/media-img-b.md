# MEDIA-IMG-B 진행 기록 (WebP 디코더)

> **갱신(MEDIA-B, 2026-11-18):** 이 기록 뒤에 WebP 의 `EXIF` 방향과 `ICCP` 프로파일이 구현되었다. 아래 "적용하지 않는다" 서술은 과거 상태이며 현재 상태는 `media-matrix.md` 9절을 본다.

담당 경로: `src/org/duckdns/hjow/swingwebpanel/media/image/webp/**`, `tests/src/org/duckdns/hjow/swingwebpanel/media/image/webp/**`, `tests/resources/org/duckdns/hjow/swingwebpanel/testdata/webp/**`, 이 문서.
시험 실행: `scripts/pkgtest.sh webp "media/image" "media/image/webp"` (JDK 6u45). 등록부 `ImageDecoders` 는 `org.duckdns.hjow.swingwebpanel.media.image.webp.WebpDecoder` 를
리플렉션으로 찾으므로 다른 연결은 필요 없다(`getFormatName()` = `webp`, MIME `image/webp`).

## 현재 상태 (2026-11-16)

```text
현재 작업 번호: P18-03 중 IMG-08(WebP) 구현과 검증 완료. P18-03 전체는 SVG(P18-04)와 AVIF 가 끝나야 체크할 수 있다
완료한 내용:
  - 중단되어 있던 작업(커밋되지 않은 VP8 파일 7개, 시험 없음)을 이어받아 WebP 전체를 완성했다.
  - WebpDecoder(RIFF 컨테이너, 단순/확장 파일, ALPH, ANIM/ANMF 애니메이션), Vp8Decoder(VP8 키 프레임: 이어받은 코드를 시험으로 검증),
    Vp8lDecoder(VP8L 무손실: 변환 4종, 접두 부호, LZ77+2차원 거리표, 색 캐시, 메타 접두 부호), WebpAlpha(ALPH: 무압축/무손실, 필터 4종)
변경 파일: 아래 "파일" 절
검증 결과: JDK 6u45, 위 명령: 24 passed / 0 failed(기반 시설 7건 포함, WebP 17건). media/image 전체 92 passed / 0 failed. sourcecheck 0 위반.
  libwebp 1.6.0 이 만든 92개 파일의 모든 프레임이 libwebp 의 디코딩 결과와 바이트 단위로 같고, Chrome 의 WebP 인코더가 만든 11개도 같다.
미해결 사항: 아래 "알려진 한계"
다음 작업 번호: P18-04(SVG), AVIF, P18-05(img 요소 연결)
```

## 파일

제품: `media/image/webp/{WebpDecoder,WebpRaster,WebpAlpha,Vp8lDecoder,Vp8lBits,Vp8lHuffman,Vp8Decoder,Vp8BoolDecoder,Vp8Frame,Vp8Predict,Vp8LoopFilter,Vp8CoeffTables,Vp8ModeTables}.java`
(공개 클래스는 `WebpDecoder` 하나이고 나머지는 패키지 비공개).
시험: `tests/src/org/duckdns/hjow/swingwebpanel/media/image/webp/{WebpDecoderTest,WebpLibwebpTest,WebpRobustnessTest,WebpFixtures,WebpBuilder}.java`.
자료: `tests/resources/org/duckdns/hjow/swingwebpanel/testdata/webp/chrome-fixtures*.txt`(Chrome 이 만든 11개, 텍스트), `.../webp/libwebp/*.webp` + `manifest.txt`(libwebp 로 만든 92개),
`.../webp/libwebp/generate_fixtures.py`(만든 방법을 기록한 스크립트. 빌드와 시험이 실행하지 않는다).

## 구현 요약

- **컨테이너(`WebpDecoder`)**: RIFF 크기가 실제 데이터보다 크면 CORRUPT(잘린 파일). 단순 파일(`VP8 `/`VP8L` 하나)과 `VP8X` 확장 파일. 확장 파일의 정지 영상은 `ALPH` 다음 `VP8 `/`VP8L`,
  캔버스 크기와 비트스트림 크기가 다르면 CORRUPT. ICCP/EXIF/XMP/알 수 없는 청크는 건너뛴다. 청크는 순차로 훑는다(청크 수만큼 객체를 만들지 않는다).
- **애니메이션**: 투명한 캔버스에 프레임을 합성해 프레임마다 캔버스 전체를 `DecodedFrame` 으로 낸다. 합성은 libwebp 의 `WebPAnimDecoder` 와 같다:
  키 프레임 판정(첫 프레임, 합성 없는 전체 프레임, 배경 지우기 뒤), 이전 프레임 사각형 지우기, 알파가 일부인 픽셀의 `BlendPixelNonPremult` 정수 연산(방금 지운 사각형 안은 복사).
  `ANIM` 의 배경색은 무시한다(브라우저와 같음). 지연 10 ms 이하는 100 ms(다른 애니메이션 형식과 같은 정책). 프레임 수/총 픽셀은 `DecodeLimits` 로 제한한다.
- **VP8 손실**: RFC 6386 의 키 프레임. 불리언 디코더, 세그먼트, 루프 필터(일반/단순), 토큰 파티션, 인트라 예측, 역DCT/WHT. YUV→RGB 는 libwebp 와 같은 6비트 고정소수점 BT.601,
  색차는 "fancy" 업샘플링(9-3-3-1). 표(`Vp8CoeffTables`, `Vp8ModeTables`)는 RFC 6386(https://www.rfc-editor.org/rfc/rfc6386.txt, 조회일 2026-11-16)에서 스크립트로 추출해 원소 수로 확인했다.
- **VP8L 무손실**: WebP Lossless Bitstream Specification. 비트 리더(LSB 우선), 접두 부호(단순형/일반형, 길이 부호 반복 16/17/18, `max_symbol`), 8비트 빠른 표 + 정규 부호 느린 경로,
  불완전/초과 구독 부호 거부, 기호 하나뿐인 부호는 0비트, 변환 4종(예측 14모드, 색 교차, 녹색 빼기, 색 인덱싱과 픽셀 묶음 1/2/4비트), LZ77 + 120개 거리표, 색 캐시, 메타 접두 부호
  (쓰이지 않는 집합은 검증만 하고 표를 만들지 않음, 표 메모리 8 Mi int 상한).
- **ALPH**: 무압축 또는 헤더 없는 VP8L(녹색 채널이 알파), 필터 없음/가로/세로/기울기. 전처리(레벨 축소) 힌트는 디코딩에 무관하다.
- **제한과 취소**: 크기는 아무것도 할당하기 전에 `DecodeLimits.checkImageSize`(VP8/VP8L 헤더와 VP8X 캔버스 모두), 애니메이션은 프레임 수와 총 픽셀, 스레드 인터럽트는 CANCELLED(플래그 유지).

## 검증 요약

| 시험 | 수 | 내용 |
| --- | --- | --- |
| WebpDecoderTest | 2 | Chrome(libwebp)이 인코딩한 11개 파일(손실 7: 사진 3종·잡음·단색·1x1·알파, 무손실 4)의 모든 행 해시가 Chrome 이 디코딩한 값(WebGL 로 알파를 곱하지 않고 읽음)과 같다 |
| WebpLibwebpTest | 5 | libwebp 1.6.0(Pillow)이 만든 파일 84개 + ffmpeg 프리셋으로 만든 8개의 모든 프레임이 libwebp 디코딩 결과와 바이트 단위로 같다. 범위는 아래 |
| WebpRobustnessTest | 10 | 모든 길이에서 자른 파일, 컨테이너를 고쳐 비트스트림만 자른 파일, 무작위 손상 1600회(누수 없음), 쓰레기/빈 데이터/알 수 없는 첫 청크, 크기 폭탄(VP8X, VP8, VP8L 헤더), 프레임 수/총 픽셀 제한, 애니메이션 구조 오류 6종, 등록부 연동(MIME 힌트 무관), 취소, 비교가 차이를 실제로 찾는지 대조 |

`WebpLibwebpTest` 가 다루는 범위(파일 이름 접두사): `lossy_photo*`(품질 5~100, method 0/4/6, 1x1·1x3·3x1·2x2·15·16·17·31x5·5x31·33x17·97x65·130x40·200x120 등), `lossy_flat/stripes/noise/pattern`,
`lossy_ffmpeg_*`(프리셋 picture/photo/drawing/icon/text → 루프 필터 선명도 0/3/4/6, 필터 끔), `lossy_alpha*`(알파 품질 20/50/100, 무압축/압축, 필터 0~3, 노이즈 알파, 1x1),
`lossless_photo*`(method 0~6, 품질 0/50/100, 1x1·1x9·9x1·2x3·17x17·33x5), `lossless_palette_*`(2~256색: 픽셀 묶음 8/4/2/1개와 묶음 없음, 폭이 묶음 단위의 배수가 아닌 경우),
`lossless_noise/pattern/stripes/mixed`(예측 모드 0~13 전부, 색 교차, 녹색 빼기, 색 캐시와 적중, 메타 접두 부호 여러 집합, 120개 거리표와 120 초과 거리), `lossless_alpha*`,
`container_*`(단순 파일, VP8X + ICCP/EXIF/XMP/알 수 없는 청크, 홀수 길이 청크 패딩), `anim_*`(libwebp 애니메이션 인코더 8종, 손으로 조립한 파일 6종: 오프셋, 배경 지우기/유지 × 합성/덮어쓰기 조합,
알파가 일부인 프레임, 손실+ALPH 프레임, 안의 알 수 없는 청크, 1x1 프레임, 지연 0/5/10 ms).

기능 범위를 계측한 결과(시험 파일이 실제로 지난 경로): VP8L 변환 0~3 전부, 예측 모드 0~13 전부, 팔레트 비트 0~3, 색 캐시 사용과 적중, 메타 접두 부호(집합 2 이상), 단순 부호 1/2 기호,
일반 부호 + 반복 16/17/18 + `max_symbol`, 거리표/120 초과; VP8 서브블록 모드 10종, 휘도 모드 5종, 색차 모드 4종, 토큰 범주 3~6, 일반 루프 필터(선명도 0/3/4/6), 세그먼트 유/무;
ALPH 무압축·무손실 필터 0~3.

JRE 6 사실: `ImageIO` 에는 WebP 리더가 없다(`readers for webp: false`, 시험 `ImageIoFactsTest`가 출력). 따라서 자체 디코더가 필요하다.

## 알려진 한계 (정직한 기록)

- **실제 인코더 출력으로 검증하지 못한 VP8 기능**: 건너뜀 플래그(`mb_no_coeff_skip`)와 확률, 단순 루프 필터, 루프 필터 변화량(`mode_ref_lf_delta`), 토큰 파티션 2개 이상, 양자화 변화량 중 일부.
  libwebp 인코더(Pillow, ffmpeg 프리셋, Chrome)는 이 기능들을 쓴 스트림을 만들지 않았다(계측으로 확인). 이 경로들은 **실행 비교가 없고**, 코드를 libwebp 의 동작
  (`ParseSegmentHeader`, `ParseFilterHeader`, `VP8ParseQuant`, `PrecomputeFilterStrengths`, `VP8DecodeMB` 의 건너뜀 처리, `DoFilter2`/`SimpleHFilter16(i)` 의 임계값과 순서)과
  대조해 읽는 것으로만 확인했다(헤더 읽기 순서, 건너뛴 매크로블록의 문맥 초기화, 안쪽 경계 판정, 단순 필터의 `2*level+ilevel`/`+4` 한계, 파티션 크기 표 모두 일치).
  대조에서 찾은 알려진 차이 두 가지(둘 다 libwebp 인코더가 만들지 않는 스트림에서만 드러남): ① 세그먼트로 조정한 필터 수준을 델타를 더하기 전에 0..63 으로 자른다(RFC 의 참조 디코더 방식).
  libwebp 는 자르지 않는다. ② 16x16 예측 매크로블록의 Y2 블록이 토큰은 있으나 값이 모두 0 이면 안쪽 경계를 필터링한다(libwebp 는 건너뜀).
- 색 프로파일(ICCP)은 적용하지 않는다(sRGB 로 간주). EXIF 방향은 적용하지 않는다(WebP 에서는 브라우저마다 다름). XMP/EXIF 는 읽지 않는다.
- 불완전한(잘린) 파일은 점진 표시 없이 CORRUPT 이다(libwebp 의 증분 디코더 동작은 구현하지 않음).
- `VP8L` 헤더의 알파 비트는 합성 판정(키 프레임)에만 쓰고 픽셀의 알파 값은 디코딩된 그대로 낸다(libwebp 와 같음).
- 한 영상의 접두 부호 표 메모리는 8 Mi int(32 MiB)까지이고 이를 넘으면 CORRUPT 이다. 큰 영상(6400만 픽셀 제한 안)은 `int[]` 한두 벌을 더 쓴다.
- 애니메이션 프레임은 프레임마다 캔버스 전체를 `int[]` 로 가지므로 메모리는 `프레임 수 × 캔버스` 이다(`DecodeLimits.maxTotalPixels` 로 제한).
- Chromium/Blink 의 애니메이션 합성은 Skia(알파 미리 곱하기) 때문에 알파가 일부인 픽셀에서 libwebp 와 1 정도 다를 수 있다. 이 프로젝트는 libwebp(`WebPAnimDecoder`)를 기준으로 삼았다.

## 출처와 라이선스 기록

- VP8: RFC 6386, 위 URL, 조회일 2026-11-16. VP8L: "WebP Lossless Bitstream Specification"(https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification), WebP Container Specification.
  디코더는 규격에서 다시 작성했고 외부 코드를 복사하지 않았다.
- `WebpDecoder` 의 애니메이션 합성 산식(`BlendPixelNonPremult` 의 정수 연산, 키 프레임 판정)은 libwebp 의 `src/demux/anim_decode.c`(BSD-3-Clause, Copyright Google Inc.)의 동작을 따라 다시 썼다.
  구조를 옮겨 적을 정도의 차용은 아니지만 출처를 남긴다. 참조 구현 비교에만 libwebp(Pillow 12.3 에 포함된 1.6.0)를 사용했고 제품 코드는 libwebp 를 실행하거나 연결하지 않는다.
- 시험 자료는 Chrome 156(내장 브라우저), Pillow 12.3.0 + libwebp 1.6.0, ffmpeg(libwebp 인코더)로 생성했다. 스크립트 `generate_fixtures.py`: `python generate_fixtures.py <출력 디렉터리>`
  (환경 변수 `FFMPEG` 로 ffmpeg 실행 파일을 주면 프리셋 파일이 추가된다). 이 스크립트는 문서일 뿐이며 시험과 빌드가 실행하지 않는다.

## 이어받은 작업의 상태 (기록)

이 문서가 만들어지기 전의 상태: 커밋되지 않은 `media/image/webp/Vp8*.java` 7개 파일(약 2700줄, 2026-11-16 12:06~12:13 작성)만 있었고 시험, 진행 기록, `WebpDecoder` 가 없었다.
이어받은 파일은 시험으로 모든 경로를 확인한 결과 수정이 필요하지 않았고, 반복문 한국어 주석 14곳만 보강했다.

## 체크박스 후보

- IMG-08(WebP) 구현과 검증 완료(위 한계 전제). P18-03 은 SVG(IMG-07)와 AVIF(IMG-09) 후에 체크한다. P18-02 의 "JRE 가 제공하지 않는 형식은 자체 디코더" 항목에 WebP 는 해당(해결).

## 조정자 요청

- `docs/FEATURE_MATRIX.md` IMG-08 행을 위 상태로 갱신(이 커밋에서 함). `docs/PROGRESS.md` 에 반영(이 커밋에서 함).
