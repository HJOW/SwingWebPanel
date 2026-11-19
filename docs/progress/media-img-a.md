# MEDIA-IMG-A 진행 기록 (PNG/APNG/GIF/JPEG/BMP/ICO 이미지 디코더와 등록부)

> **갱신(MEDIA-B, 2026-11-18):** 이 기록 뒤에 색 관리(PNG gAMA/cHRM/sRGB/iCCP/cICP, JPEG APP2 ICC, BMP V4/V5)와 PNG `eXIf` 방향이 구현되었다. 아래 "색 관리 없음", "색 프로필 미적용" 서술은 과거 상태이며 현재 상태는 `media-matrix.md` 9절을 본다.

담당 경로: `src/org/duckdns/hjow/swingwebpanel/media/*.java`(MediaTypes 등 새 클래스), `src/org/duckdns/hjow/swingwebpanel/media/image/*.java`(webp/avif/svg 하위 패키지 제외),
`tests/src/org/duckdns/hjow/swingwebpanel/media/image/*.java`(하위 패키지 제외), 이 문서.

## 현재 상태 (2026-11-16)

```text
현재 작업 번호: P18-01(이미지 부분), P18-02, P18-03 중 IMG-01~06 완료. 다음은 조정자의 P18-05(img 요소 연결)와 WebP/AVIF/SVG 작업자의 결과 통합
완료한 내용:
  - ImageDecoders: 시그니처 기반 형식 판별 + MIME 힌트(동률 해소용) + 제한/오류/취소 정리 + 선택적 디코더 리플렉션 + MIME/Accept 도우미
  - PngDecoder(PNG + APNG), GifDecoder, JpegDecoder, BmpDecoder, IcoDecoder(+ decodeAll), 공통 PixelOps, MediaTypes
  - MediaException.Kind.CANCELLED 추가(스레드 인터럽트 = 이동 취소, 인터럽트 플래그 유지), ImageDecoder.accepts 의 head 를 최대 1024바이트로 문서 정정
변경 파일: 아래 "파일" 절
검증 결과: JDK 6u45, 개인 빌드 스크립트(media + media/image 만 컴파일, build/pkg-mia): 75 passed / 0 failed / 0 skipped (기반 시설 7건 포함, 이미지 68건).
  sourcecheck(내 파일 전부) 0 위반.
미해결 사항: 아래 "알려진 한계" 참조
다음 작업 번호: P18-05 (img/picture/source 연결, 로딩 완료/실패 이벤트, alt 표시) — 조정자/렌더러 단계
```

## 파일

제품: `media/MediaTypes.java`, `media/MediaException.java`(CANCELLED 추가), `media/image/{ImageDecoders,PngDecoder,GifDecoder,JpegDecoder,BmpDecoder,IcoDecoder,PixelOps}.java`,
`media/image/ImageDecoder.java`(문서만 정정).
시험: `tests/src/org/duckdns/hjow/swingwebpanel/media/image/{PngDecoderTest,ApngDecoderTest,GifDecoderTest,JpegDecoderTest,BmpDecoderTest,IcoDecoderTest,ImageDecodersTest,ImageIoFactsTest}.java`,
시험 도우미(독립 인코더) `{PngBuilder,GifBuilder,BmpBuilder,Bytes,ImageAsserts}.java`.

commit: cca278f(PNG/APNG), 69727e5(GIF), 96e753c(BMP/ICO), a733758(JPEG), b1a6bff(등록부와 JRE 사실 시험), 그리고 이 문서와 시험 보강.

## 검증 요약 (클래스별 시험 수)

| 시험 | 수 | 내용 |
| --- | --- | --- |
| PngDecoderTest | 14 | 색 유형 5종 × 비트 심도 15조합 × 크기 6종 × 인터레이스 유무 정확 비교, 필터 0~4 개별, tRNS(회색/RGB/팔레트, 8/16비트), 팔레트 밖 인덱스, IDAT 분할(1바이트 청크 포함), 색 관리 청크 무시, CRC(필수 → CORRUPT, 보조 → 건너뜀), 잘림/IEND 없음, 잘못된 헤더 10종, 100000×100000/2147483647² 주장 → TOO_LARGE(즉시), ImageIO 가 쓴 PNG 교차 비교, 취소, 변형 300×3 |
| ApngDecoderTest | 10 | SOURCE/OVER, BACKGROUND/PREVIOUS(첫 프레임 PREVIOUS = BACKGROUND), 기본 이미지가 애니메이션 안/밖, 지연 환산(분모 0, ≤10 ms → 100 ms), 재생 횟수, 한 프레임 = 정지, 인터레이스 프레임, 잘못된 순서 번호/캔버스 밖/잘린 fdAT → 기본 이미지로 대체, 기본 이미지 손상 → CORRUPT, 프레임/픽셀 제한 → TOO_LARGE, 늦은 acTL 무시 |
| GifDecoderTest | 10 | LZW 코드 크기 9~12비트 성장과 표 초기화(임의 데이터 6종, JRE GIF 리더로 교차 확인), ImageIO 가 쓴 GIF, 투명/지역 색 표/인터레이스 8종 높이, 처리 방법 1·2·3, 지연, 반복 횟수, 캔버스 크기 규칙, 잘림, 잘못된 LZW 코드 크기, 65535² → TOO_LARGE, 프레임 제한, 변형 800회, 취소 |
| JpegDecoderTest | 10 | baseline/progressive 구역 색, 회색, CMYK(표지 없음/Adobe/YCCK), 3성분 색 규칙(JFIF/Adobe 0·1/성분 id RGB), EXIF 방향 1~8 × II/MM, `PixelOps.orient` 정확 비교, 깨진 EXIF 무시, 잘림(부분 디코딩/헤더 CORRUPT), 미지원 방식(산술/무손실/차분/12비트/2성분), 크기 0/65535², 변형 300회, 취소 |
| BmpDecoderTest | 8 | 24비트 아래→위/위→아래/코어/V4/V5 헤더, 1/4/8비트 팔레트(코어 헤더, 색 수 부족), 32비트(알파, 알파 전부 0 → 불투명, 비트 마스크, V4 알파 마스크, BI_ALPHABITFIELDS), 16비트 5-5-5/5-6-5, RLE8/RLE4(반복·절대·이동·EOL·일찍 끝남), ImageIO 가 쓴 BMP, 잘못된 필드 11종과 큰 크기, 변형 1200회 |
| IcoDecoderTest | 5 | 32비트 알파(마스크 무시), AND 마스크(1/8/24/32비트), 여러 크기에서 가장 큰 것 선택 + `decodeAll` + 내장 PNG + CUR, 손상된 항목 건너뜀/전부 손상 → CORRUPT, 큰 크기 → TOO_LARGE, 변형 400회 |
| ImageDecodersTest | 8 | 시그니처가 형식을 정함(PNG 바이트 + `image/jpeg` 힌트 등 7가지 힌트 × 5형식), 빈/null/HTML/zip/바이트 제한 오류, MIME 도우미/Accept, 등록·동률 해소·RuntimeException → CORRUPT·디코더가 제한을 어겨도 TOO_LARGE, 8스레드 동시 디코딩, 취소, 선택적 디코더, 큰 이미지 3000×2000(159 ms)과 6000×3000 디코딩 도중 인터럽트 → CANCELLED |
| ImageIoFactsTest | 3 | JRE ImageIO 사실 출력(`[JRE fact]`)과 JpegDecoder 가 기대는 능력 단언 |

## JRE 6u45 에서 확인한 사실 (`ImageIoFactsTest` 출력, 실행 JDK 6u45 64비트 HotSpot, 2026-11-16)

- ImageIO 리더 형식: `bmp, gif, jpeg(jpg), png, wbmp` / MIME `image/bmp, image/gif, image/jpeg, image/png, image/x-png, image/vnd.wap.wbmp` / 라이터: 같은 6종(GIF 라이터 있음).
  **WebP, AVIF, SVG, ICO/CUR, APNG 애니메이션 리더는 없다** → 자체 디코더 필요(IMG-02, IMG-06 은 이 문서의 디코더로 해결, IMG-07~09 는 다른 작업자).
- JPEG: `readRaster` 가 baseline(3밴드), progressive(3밴드), 회색(1밴드), 성분 4개(4밴드)의 **원시 성분 값**(색 변환 없음)을 준다. 성분 4개 파일에 `ImageIO.read` 도 성공하지만 색 규칙이 불명확해 쓰지 않는다.
  65535×65535 헤더는 리더가 `IIOException: Maximum supported image dimension is 65500 pixels` 로 거부한다(우리는 그 전에 DecodeLimits 로 거부). 잘린 파일은 예외 없이 앞부분만 디코딩한다.
- PNG: 16비트 회색 쓰기/읽기, Adam7 읽기, 2비트 팔레트 읽기 가능. **APNG 는 기본 이미지만** 읽는다(애니메이션 무시). 우리 PNG 디코더는 ImageIO 를 쓰지 않는다(ImageIO 는 시험 교차 확인에만 사용).
- GIF: 리더가 프레임을 서브 이미지로만 주고 합성하지 않는다(2프레임 파일 → 2 images) → 합성/처리 방법/투명도는 자체 구현. LZW 도 자체 구현(ImageIO 는 교차 확인에만 사용).
- BMP: RLE8 도 읽는다(우리는 자체 디코더).
- **ICO 파일 앞 4바이트(`00 00 01 00`)를 ImageIO 의 WBMP 리더가 자기 형식으로 착각**해 `ImageIO.read` 가 `IllegalArgumentException: Empty region!` 을 던진다 → 형식 판별을 ImageIO 에 맡기면 안 되고 우리 시그니처 판별(`ImageDecoders`)을 써야 한다.
- 위 시험은 JDK 6u45 에서만 실행했다. 더 새 JRE 는 이 단계에서 검증하지 않았다.

## 결정 사항과 가정 (문서화 요구)

- **색 관리 없음**: PNG gAMA/cHRM/sRGB/iCCP, JPEG ICC 프로필, BMP V4/V5 프로필은 모두 무시하고 sRGB 로 본다. (Chromium 은 ICC 를 적용하므로 색이 약간 다를 수 있다. 기능 표 "남은 호환성 차이"에 기록할 것.)
- PNG 16비트 샘플은 **상위 바이트만** 쓴다. 팔레트 밖 인덱스는 불투명 검정. 필수 청크 CRC 오류 → CORRUPT, 보조 청크 CRC 오류 → 그 청크만 건너뜀. IEND 없음은 허용.
- **APNG**: 기본 이미지는 IDAT 앞에 fcTL 이 있을 때만 애니메이션의 첫 프레임. 순서 번호/프레임 위치/프레임 데이터가 일관되지 않으면 **정지 기본 이미지로 대체**(APNG 규격의 권고). 프레임 수/픽셀 제한 초과와 취소는 대체하지 않고 오류로 보고.
- **지연**: GIF/APNG 모두 10 ms 이하(0 포함)는 100 ms 로 바꾼다(브라우저 관례). 반복: GIF Netscape N → N+1 번 재생, 확장 없음 → 1번, 0 → 무한(`loopCount` 0). APNG `num_plays` 그대로.
- **GIF 캔버스**: 논리 화면 크기, 첫 프레임이 더 크거나 화면 크기가 0 이면 첫 프레임 범위로 넓힘, 이후 프레임은 잘림. "배경으로 복원" 은 투명으로 지움. 팔레트 밖 인덱스는 투명. 프레임 중간에서 끝난 파일은 그 프레임을 버리고 앞 프레임만 반환(하나도 없으면 CORRUPT). 데이터가 일찍 끝난 프레임(종료 코드)은 디코딩된 픽셀 유지.
- **JPEG**: 헤더/마커/EXIF/색 규칙은 자체, 샘플 디코딩(허프만/IDCT/업샘플)만 JRE ImageIO 의 `readRaster`(공개 API)를 사용한다. 3성분: Adobe 변환 0 = RGB, JFIF = YCbCr, 성분 id R·G·B = RGB, 그 밖 = YCbCr.
  4성분: Adobe 표지 있음 → 반전된 CMYK(변환 2 = YCCK), 없음 → 일반 잉크량. 잘린 파일은 디코딩된 부분만(나머지 회색, 브라우저와 같음). 산술 부호화, 무손실, 계층(차분), 12비트, 성분 2개는 UNSUPPORTED_FORMAT.
- **BMP**: 32비트 알파는 사용하되 알파가 전부 0 이면 불투명(Chromium 과 같은 방향). RLE 가 건너뛴 픽셀은 투명. BI_JPEG/BI_PNG/Huffman 1D/RLE24 → UNSUPPORTED_FORMAT. OS/2 v2 16바이트 헤더는 미지원.
- **ICO/CUR**: 가장 큰 이미지를 반환(면적 기준, 동률은 앞선 항목), `IcoDecoder.decodeAll` 로 전부 얻을 수 있음. 32비트 + 알파가 있으면 AND 마스크 무시, 알파가 전부 0 이면 마스크 적용. 디렉터리의 가로/세로 값은 쓰지 않고 이미지 자체의 헤더를 쓴다.
- **안전**: 모든 디코더가 크기를 `DecodeLimits.checkImageSize` 로 확인한 뒤에만 배열을 할당한다(큰 크기 주장 → TOO_LARGE 가 즉시, 메모리 사용 없음). `RuntimeException` → CORRUPT, `OutOfMemoryError` → TOO_LARGE 로 경계에서 변환.
  행/프레임/LZW 코드 단위로 `Thread.currentThread().isInterrupted()` 를 확인해 인터럽트 시 `MediaException.Kind.CANCELLED` 로 중단하고 플래그를 유지한다. ImageIO 가 도는 JPEG 샘플 디코딩 구간은 중단되지 않는다(디코딩 전후와 색 변환 중에만 확인).
- `ImageDecoders.decode` 는 각 디코더 결과도 한 번 더 `DecodeLimits` 로 확인한다(다른 작업자의 디코더가 제한을 어겨도 TOO_LARGE).

## 알려진 한계 (미구현이 아니라 의도한 제약 또는 후속 과제)

- 이미지 요소 연결(P18-05), 로딩 실패 시 alt/URL 표시(P18-06), `ACCESS_FAILED` 발생(네트워크 계층) 은 이 작업의 범위 밖이다. `MediaException.Kind.ACCESS_FAILED` 는 로더 쪽에서 쓴다.
- JPEG 샘플 디코딩이 JRE 내부(ImageIO)에 의존하므로 JRE 가 못 읽는 변형(산술 부호화, 12비트, 무손실)은 지원하지 않는다. 자체 JPEG 디코더로 바꾸려면 별도 과제(허프만/IDCT/progressive 스캔 구현)가 필요하다.
- 색 프로필 미적용(위 결정 사항). PNG/JPEG 모두 10비트 이상 HDR, CICP 는 없다.
- 시험 입력은 시험 코드가 만든 파일과 ImageIO 가 쓴 파일뿐이다. 실제 사진/아이콘 모음 같은 외부 시료는 쓰지 않았다(내려받지 않음).
- JDK 6u45 에서만 실행했다. 더 새 JRE 의 ImageIO 동작 차이는 확인하지 않았다.

## 조정자 요청

- FEATURE_MATRIX §8.1: IMG-01~IMG-06 의 "JRE 6 기본 제공" 열을 위 사실로 정정(IMG-02 APNG: 기본 이미지만, IMG-03 GIF: 프레임 서브 이미지만, IMG-04 JPEG: 원시 샘플만 신뢰, IMG-05 BMP: RLE 도 읽음, IMG-06 ICO: 없음 + WBMP 리더 착각) 하고
  상태를 "검증 완료"로, 남은 차이(색 관리 없음, 지연 규칙, JPEG 미지원 변형)를 제약 열에 기록.
- 렌더러/로더 연결 시 `ImageDecoders.getAcceptHeader()` 를 `Accept` 요청 헤더에, `decode(data, contentType, limits)` 를 이미지 데이터 처리에 쓰고, `MediaException.Kind` 로 접근 실패/형식 미지원/손상(CORRUPT, TOO_LARGE)을 구분해 대체 표시를 고를 것.
- 선택적 디코더(WebP/AVIF/SVG)는 클래스 이름 `org.duckdns.hjow.swingwebpanel.media.image.{webp.WebpDecoder, avif.AvifDecoder, svg.SvgDecoder}` 로 리플렉션 등록된다(공개 무인자 생성자, `ImageDecoder` 구현 필요). 그 작업자가 이름을 바꾸면 `ImageDecoders.OPTIONAL_DECODERS` 도 바꿔야 한다.
- `MediaException.Kind.CANCELLED` 와 `ImageDecoder.accepts` 의 1024바이트 head 문서 변경은 공유 파일에 대한 추가적 변경이다.

## 체크박스 후보

- 기능 표: **IMG-01, IMG-02, IMG-03, IMG-04, IMG-05, IMG-06** (구현 + JDK 6u45 시험 완료. IMG-07 SVG, IMG-08 WebP, IMG-09 AVIF 는 다른 작업자).
- AGENTS.md: **P18-02 와 P18-03 은 이미지 중 PNG/APNG/GIF/JPEG/BMP/ICO 부분만 완료**(전체 항목은 WebP/AVIF/SVG, Java Sound 오디오가 끝나야 체크). **P18-01 은 이미지 판별/디코딩 부분만 완료**(오디오와 표시/재생 상태 분리가 남음). 지금 체크할 AGENTS.md 항목은 없다.
