# MEDIA-SVG-AVIF 진행 기록 (SVG 디코더 P18-04, AVIF 디코더 P18-03)

> **갱신(MEDIA-B, 2026-11-18):** 이 기록 뒤에 AVIF 필름 그레인, 중첩 오버레이, `colr`(nclx/prof) 색 변환이 구현되었다. 아래 해당 서술은 과거 상태이며 남은 변형(인터 프레임, 인트라 블록 복사, 초해상도, 계층 이미지)은 `media-matrix.md` 7, 9절을 본다.

담당 경로: `src/org/duckdns/hjow/swingwebpanel/media/image/svg/**`, `src/org/duckdns/hjow/swingwebpanel/media/image/avif/**`, `tests/src/org/duckdns/hjow/swingwebpanel/media/image/{svg,avif}/**`,
`tests/resources/media/{svg,avif}/**`, 이 문서. 등록부 `ImageDecoders` 는 `org.duckdns.hjow.swingwebpanel.media.image.svg.SvgDecoder`,
`org.duckdns.hjow.swingwebpanel.media.image.avif.AvifDecoder` 를 리플렉션으로 찾으므로 다른 연결은 필요 없다.
시험 실행(JDK 6u45): `scripts/pkgtest.sh svg "media" "media/image/svg"`, `scripts/pkgtest.sh avif "media" "media/image/avif"`.
소스 검사: `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/media/image/svg src/org/duckdns/hjow/swingwebpanel/media/image/avif tests/src/org/duckdns/hjow/swingwebpanel/media/image/svg tests/src/org/duckdns/hjow/swingwebpanel/media/image/avif` → 0건.

## 현재 상태 (2026-11-16)

```text
현재 작업 번호: P18-04(SVG) 완료 후보, P18-03 의 AVIF 부분 완료 후보 (아래 "체크 후보" 와 "알려진 한계" 참조)
완료한 내용:
  - SVG: 자체 XML 파서, CSS(스타일시트/선택자/연쇄/미디어 질의), 스타일 계산, 경로/변환/색/길이 해석기, Java2D 렌더러(도형, 선, 그라디언트, 패턴, 클립, 마스크,
    use/symbol/nested svg/switch, image(data:), marker, text, filter, 혼합 모드), 임의 크기 그리기 API, Chrome 156 기준 비교 사례 151개.
  - AVIF: 자체 AV1 인트라 프레임 디코더(OBU, 시퀀스/프레임 헤더, 심볼 디코더, 분할/모드 정보/팔레트/변환 크기/계수/예측/역양자화(양자화 행렬 포함)/역변환,
    블록 경계 제거 필터, CDEF, 루프 복원), HEIF 컨테이너(항목, 속성, 참조, 격자, 보조 알파, clap/irot/imir), YUV→RGB 변환.
변경 파일: 아래 "파일" 절
검증 결과: 아래 "검증 요약"
미해결 사항: 아래 "알려진 한계", "조정 요청"
다음 작업 번호: (이 영역 자체의 후속) AVIF 인트라 블록 복사/필름 그레인/초해상도, AVIF 이미지 시퀀스(인터 프레임) — 선행 필요 없음, 별도 작업
```

## 파일

### SVG (`media/image/svg`)
제품: `{SvgDecoder,SvgImage,SvgRenderer,SvgXmlParser,SvgElement,SvgCss,SvgStyle,SvgPaint,SvgColors,SvgLength,SvgScanner,SvgTransforms,SvgPath,SvgBounds,SvgMasks,SvgPaintServers,SvgMarkers,SvgText,SvgFilters,SvgTurbulence}.java`
(공개 클래스는 `SvgDecoder` 와 `SvgImage`, 나머지는 패키지 비공개).
시험: `tests/src/org/duckdns/hjow/swingwebpanel/media/image/svg/{SvgReferenceTest,SvgDecoderTest,SvgValueParsersTest,SvgXmlParserTest,SvgCssTest}.java`, 도우미 `SvgTestKit.java`.
자료: `tests/resources/media/svg/{cases,refs,index.txt,generation}` (Chrome 기준 사례와 만든 방법).

### AVIF (`media/image/avif`)
제품(공개 클래스는 `AvifDecoder` 하나, 나머지는 패키지 비공개):

| 파일 | 역할 |
| --- | --- |
| `AvifDecoder` | `ImageDecoder` 구현. 컨테이너 해석 → 기본 항목(av01 또는 grid) 디코딩 → 알파 → 색 변환 → clap/irot/imir 적용. |
| `AvifContainer` | ISO BMFF/HEIF 상자 읽기: ftyp, meta(pitm, iloc, iinf, iref, iprp/ipco/ipma, idat), moov 유무. 항목 바이트 조립. |
| `AvifColor` | YUV(또는 단색)→8비트 ARGB: 행렬 계수(항등, 709, 601 계열, 240M, 2020 NCL, YCgCo), 전체/제한 범위, 8/10/12비트, 이중 선형 색차 보간, 알파(미리 곱한 알파 포함). |
| `Av1Decoder` | OBU 열 해석, 첫 키/인트라 전용 프레임의 타일 그룹 디코딩, 후처리 필터 호출, 지원하지 않는 도구는 `UNSUPPORTED_FORMAT`. |
| `Av1Sequence`, `Av1FrameHeader`, `Av1BitReader` | 시퀀스/프레임 헤더(양자화, 분할, 타일, 델타 q/lf, 루프 필터, CDEF, LR, 양자화 행렬 수준). |
| `Av1SymbolDecoder`, `Av1Cdfs` | 범위 디코더(적응형 CDF)와 CDF 집합(양자화 문맥별 계수 표 선택, 복사). |
| `Av1TileDecoder`, `Av1CoeffReader` | 분할 트리, 블록 모드 정보(인트라 프레임), 팔레트, 필터 인트라, CfL, 변환 크기, 계수 읽기와 문맥, 복원(역양자화+역변환). |
| `Av1IntraPred`, `Av1InvTransform` | 인트라 예측(DC/방향/스무스/Paeth/재귀 필터, 가장자리 필터/업샘플), 역 DCT/ADST/항등/WHT(규격과 같은 정수 연산). |
| `Av1LoopFilter`, `Av1Cdef`, `Av1LoopRestoration` | 블록 경계 제거, CDEF, 루프 복원(위너, 자기 유도). |
| `Av1Frame`, `Av1Picture` | 프레임 상태와 디코딩 결과(평면 + 색 설명). |
| `AvifTables`, `AvifCdfTables`, `AvifQmTable`, `AvifTableData` | 규격에서 생성한 상수 표(생성 스크립트와 입력 판본: `tests/resources/media/avif/generation`). |

시험: `tests/src/org/duckdns/hjow/swingwebpanel/media/image/avif/{Av1StreamTest,Av1ComponentsTest,AvifDecoderTest}.java`, 도우미 `AvifTestKit.java`(HEIF 작성기 포함).
자료: `tests/resources/media/avif/{av1,avif,generation,README.md}` (합계 227 KB; 만든 방법과 도구 버전은 README 참조).

## 검증 요약

### SVG
- `scripts/pkgtest.sh svg "media" "media/image/svg"` (JDK 6u45): 63건 통과, 실패 0 (마지막 전체 실행 기준, 아래 "알려진 문제: JDK 6 글꼴 네이티브 충돌" 참조).
- Chrome 156 이 같은 문서를 `img` + canvas 로 그린 픽셀과 사례 151개를 비교한다(평균 오차 대부분 3 미만, 40 넘게 다른 픽셀 비율 대부분 2% 미만. 글꼴 래스터화
  때문에 문자 사례는 3~5%, 패턴 변환 사례 약 4.3%, feTurbulence 평균 약 13으로 한계를 사례별로 `index.txt` 에 기록).
- 보안/한계: 외부 접근 금지(스크립트, 외부 이미지, 외부 엔티티, `use` 외부 참조), 엔티티/요소/깊이/방문 수 한계, 중첩 SVG 이미지 깊이, 취소, 악의적 문서 시험 포함.

### AVIF
명령: `scripts/pkgtest.sh avif "media" "media/image/avif"` → 이 영역 시험 27건 + 실행기 자체 시험 7건 = 34건 통과, 실패 0, 건너뜀 0.

1. **AV1 디코딩은 libdav1d 와 비트 단위로 같다.** `av1/index.txt` 의 스트림 25개(+ 거부 확인용 2개)는 libaom(여러 옵션), rav1e, SVT-AV1 이 만들었고, 디코딩한 평면의 md5 가
   ffmpeg 의 libdav1d 출력과 같다(`Av1StreamTest.testBitExactAgainstDav1d`). 포함하는 도구: 4:0:0/4:2:0/4:2:2/4:4:4, 8/10/12비트, 무손실, 홀수/1x1 크기, 타일 2x2(타일 그룹 둘로 나눈 것 포함),
   128x128 슈퍼블록, 팔레트(휘도/색차), 필터 인트라, CfL, 모든 크기 변환, 변환 집합 축소, 양자화 행렬, 분할(rav1e), 델타 q/델타 lf, 가장자리 필터 끄기, 블록 경계 제거,
   CDEF, 위너/자기 유도/전환 복원. `testFixturesCoverTheTools` 가 색인에서 도구 사용을 확인한다.
   (개발 중에는 이 밖에 1000x700, 4000x3000, 515x300 등의 여러 옵션 스트림 수십 개도 같은 방식으로 비교했으며 모두 같았다. 4000x3000 은 -Xmx512m 에서 3.5초.)
2. **libavif 와의 색 변환 비교.** Pillow(libavif 1.4.2)가 쓴 AVIF 7개를 libavif 자신이 디코딩한 RGBA 와 비교: 평균 차이 < 0.7, 최대 차이 ≤ 6, 2 초과 차이 0.3% 미만
   (실측 평균 0.0~0.28, 최대 1~4). 4:2:0/4:2:2/4:4:4/4:0:0, 항등 행렬(무손실), 알파 포함 (`testLibavifFilesMatchLibavif`).
3. **컨테이너 기능**(작성기로 실제 AV1 페이로드를 감싼 파일): 항목 데이터 상자와 다중 범위, irot(90/180/270), imir(두 축), clap(중심/오프셋/범위 밖 오류), 속성 순서,
   2x2 격자(YUV 영역에서 합성, 출력 크기로 자르기, 타일 수/형식 불일치/범위 부족 오류), 보조 알파(크기 불일치 오류), 미리 곱한 알파, colr nclx 우선, 필수 속성 모르면 거부,
   이미지 시퀀스/오버레이 거부, ispe 가 AV1 프레임보다 작으면 자르고 크면 오류. imir/irot 방향은 ffmpeg 의 해석과 일치함을 확인(imir 축 0=위아래, 1=좌우; irot=반시계).
4. **견고성.** 손상/잘린 입력 24,000회 퍼징(16개 파일 x 1500회, 비트 뒤집기와 바이트 덮어쓰기)에서 `MediaException` 이외의 예외 0, 가장 느린 경우 105 ms.
   퍼징에서 발견한 오류 하나(4:2:2 에서 색차 크기가 없는 블록 크기 → 배열 범위 오류)를 `CORRUPT` 로 고쳤다. 제한(`DecodeLimits` 픽셀/바이트)과 취소(`Thread.interrupt`)도 시험한다.
5. 소스 검사 0건(영어/한국어 Javadoc, 한국어 반복문 주석, 금지 API 없음). Java 6 문법과 API 만 사용(JDK 6u45 로 컴파일/실행).

## 알려진 한계

### SVG
- CSS 애니메이션/SMIL 은 실행하지 않는다(정적 첫 상태). `textLength`, 세로 쓰기, SVG 글꼴 미지원. 조명 필터 기본 요소(feDiffuseLighting 등)는 건너뛰므로 그것만 가진 필터는 요소를 숨긴다.
  비분리 혼합 모드(hue/saturation/color/luminosity)는 normal 처럼 처리. 회전한 요소의 필터는 배율만 반영. 외부 리소스는 어떤 경우에도 읽지 않는다(`data:` 이미지만 허용).
- 문자 사례는 Chrome 이 쓴 Windows 글꼴(Arial 등)이 필요하며 없으면 이유와 함께 건너뛴다.

### AVIF (`UNSUPPORTED_FORMAT` 으로 거부하는 것 — 잘못된 그림을 만들지 않는다)
- **인터 프레임**: AVIF 이미지 시퀀스(`avis`, 애니메이션)는 거부한다(첫 프레임만 보여 주는 대체도 하지 않았다). 정지 영상 항목에는 영향이 없다.
- **인트라 블록 복사**(`allow_intrabc`, 화면 캡처용 libaom 옵션), **초해상도**(`use_superres`), **필름 그레인**(`apply_grain`)이 켜진 프레임.
- **오버레이**(`iovl`), 계층 이미지(a1op 의 작동 지점이 0 이 아니거나 lsel), 알 수 없는 필수 속성, 상수 휘도 행렬 계수(3, 10~14).
- 색: ICC 프로파일(`rICC`/`prof`)과 색 원색/전달 함수(HDR PQ/HLG 포함)는 반영하지 않고 샘플을 sRGB 로 본다(브라우저가 프로파일 없는 이미지를 다루는 방식). 색차 위치는
  중앙(이중 선형)으로 가정. 깊이/기타 보조 이미지는 무시.
- 메모리: 프레임 평면(short)을 필터 단계마다 복사하므로 약 3배(예: 4000x3000 4:2:0 약 200 MB). `OutOfMemoryError` 는 등록부가 `TOO_LARGE` 로 바꾼다.
- 이 구현은 규격(2023-05-25판)의 정수 연산을 그대로 따른 것이며 SIMD/멀티스레드 최적화는 없다(속도: 1000x700 약 0.5~0.8초, 4000x3000 약 3.5초, JDK 6, 한 스레드).

### 알려진 문제: JDK 6 글꼴 네이티브 충돌 (SVG 문자 렌더링)
- JDK 6u45(Windows)의 `fontmanager.dll` 이 `sun.font.SunLayoutEngine.nativeLayout` 에서 `EXCEPTION_ACCESS_VIOLATION` 으로 JVM 을 종료시키는 경우가 있었다. 개발 중 세 번 관찰했으며
  (두 번은 `font-size="0"` 문자 → 이후 크기 0 이하는 그리지 않도록 고침), 마지막은 `SvgDecoderTest.testTextInk`(전체 스위트 실행 중에만, 단독 실행은 통과)에서 발생했다.
  원인은 JDK 6 글꼴 엔진(`nativeLayout` 이 잘못된 주소를 읽음)으로 보이며 제품 코드로 완전히 막을 수 없다. 재현은 불안정하다: 같은 코드로 SVG 스위트 전체를 연속 실행했을 때
  충돌 없이 63건이 통과한 실행이 여러 번이고(`--filter testTextInk` 단독 3회도 통과), 충돌은 다른 작업자의 JVM 이 동시에 돌던 시점에 한 번 났다. 충돌 시 JVM 이 통째로 종료되므로
  JRE 6 에서 SVG 문자를 쓰는 응용은 이 위험을 알아야 한다. 완화책(후속 검토): `TextLayout` 대신 `Font.createGlyphVector` 로 복잡하지 않은 문자열만 배치해 `GlyphLayout` 네이티브 경로를
  피하는 방법(자획 모양과 폭은 같지만 합자/복합 문자 조판을 잃는다). 효과를 검증하지 못해 적용하지 않았다.

## 체크 후보

- **P18-04 (SVG)**: 구현과 Chrome 기준 비교로 검증됨. 위 "알려진 한계"(애니메이션 미실행, 조명 필터 등)를 기능 표에 제약으로 적고 체크해도 되며, 보수적으로는
  "SVG 정적 렌더링 지원, 애니메이션/조명 필터 제외" 로 적는 것을 권한다. JDK 6 글꼴 충돌 문제를 인지한 상태에서 체크할 것.
- **P18-03 의 AVIF 부분**: AV1 정지 프레임(키/인트라 전용) + HEIF 정지 이미지(격자, 알파, 변환 속성)는 구현과 외부 기준(libdav1d 비트 단위, libavif)으로 검증됨.
  그러나 P18-03 전체(다른 형식 포함)는 다른 담당 영역이므로 이 문서는 AVIF 하위 항목만 체크 후보로 제시한다. 애니메이션 AVIF(이미지 시퀀스), 인트라 블록 복사,
  필름 그레인, 초해상도는 **미구현**(위 한계)이므로 "AVIF 모든 변형" 이 필요한 판정에서는 미완료로 남겨야 한다.

## 조정 요청 (코디네이터에게)

1. 커밋 번호 `20261116-137` 이 다른 작업자의 커밋(P20-08-b)과 중복됐다(동시 커밋 경합). 이력을 고치지 않았으며 필요하면 PROGRESS 에서 구분해 적는다.
2. FEATURE_MATRIX IMG-06(AVIF)/IMG-07(SVG) 행에 위 "알려진 한계"를 제약으로, 시험 파일을 검증 사례로 옮겨 주세요(이 영역 밖 문서라 수정하지 않았다).
3. 생성 스크립트(`tests/resources/media/{svg,avif}/generation`)는 도구 규칙(`tools/`)이 아니라 시험 자료 옆에 두었다(제품/도구에 포함되지 않으며 Python 3 가 필요).
   규칙상 `tools/` 에 Java 6 도구로 옮겨야 한다면 별도 작업이 필요하다.
4. AV1 규격 표는 2023-05-25판 HTML(`https://aomediacodec.github.io/av1-spec/av1-spec.html`)에서 생성했다. JDK 6 은 현대 HTTPS 에 접속하지 못해 `tools/webfetch` 를 더 새 JRE 로
   실행해 받았다(P00 의 JRE 6 TLS 사실과 일치).
5. JDK 6 글꼴 네이티브 충돌 시 저장소 루트에 `hs_err_pid*.log` 가 생긴다(`.gitignore` 의 `*.log` 로 제외됨). 시험 실행 전후에 지워도 된다.
