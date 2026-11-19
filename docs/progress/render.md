# RENDER-A 진행 기록 (P09-04, P09-05, P09-06, P09-09 그리기 절반)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/render/**`, `tests/src/org/duckdns/hjow/swingwebpanel/render/**`, 이 파일.
시험 실행: `scripts/pkgtest.sh render "render layout css html dom core net storage content media @root" "render"` (JDK 6u45). 린트: `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/render tests/src/org/duckdns/hjow/swingwebpanel/render`.

## 통합용 공개 API

```java
RenderedPage page = PageRenderer.render(doc, cssEngine, imageSource /* null 가능 */, viewportW, viewportH); // 문서 스레드에서
// EDT: paintComponent(Graphics g) { page.paint((Graphics2D) g, g.getClipBounds(), scrollX, scrollY); }   // 100 %
//      page.paint(g2, clip, scrollX, scrollY, scale, animationTimeMillis);                               // 확대/애니메이션
// EDT: HitResult h = page.hitTest(mouseX, mouseY, scrollX, scrollY, scale);   h.getElement(), h.getHref(), h.getTextNode(), h.getTextOffset()
// preferredSize = new Dimension(page.getPreferredWidth(), page.getPreferredHeight());  (확대하면 × scale)
```

- 좌표 변환은 하나뿐이다: `screen = (document − scroll) × scale` (`PagePainter` 와 `HitTester.toDocumentX/Y`·`toScreenX/Y` 가 같은 식).
- 스레드: `PageRenderer.render` 는 DOM/스타일을 읽으므로 문서 스레드에서 부른다. `RenderedPage`/`DisplayList`/`LayoutResult` 는 불변이라 EDT 가 읽어도 된다. 새 페이지 객체가 옛것을 대체하며 옛것은 그리기가 쓰는 동안 유효하다.
- `ImageSource`(호스트 구현): `getImage(url)`/`getMedia(url)` 가 `ImageResource`(LOADED+이미지/FAILED+메시지/PENDING) 를 돌려준다. null 은 진행 중. 리소스 상태가 바뀌면 호스트가 `render` 를 다시 부른다. `ImageReplacedResolver` 가 읽은 `img` 의 자연 크기를 레이아웃에 준다.

## 구현한 것

| 파일 | 내용 |
| --- | --- |
| `DisplayListBuilder` | `LayoutResult` → `DisplayList`. 캔버스 배경(루트, 없으면 body, 캔버스 전체), 단위(inline-block, overflow≠visible, opacity<1)별 블록 배경/경계선 → 인라인 콘텐츠(CSS 2.1 부록 E 의 정상 흐름 부분집합), 텍스트, 표식, 치환 콘텐츠(img/audio/video), 배경 이미지(레이어/반복/위치/크기), outline. float/위치 지정/z-index 는 P10-01(연결 지점 `emitUnit`/`inlineContent`) |
| `DisplayCommand(s)` | FILL_RECT, BACKGROUND_IMAGE, BORDER, TEXT, IMAGE, PLACEHOLDER, MARKER_SHAPE, CLIP/OPACITY 그룹(시작 명령이 끝 명령 위치를 가짐). 모든 명령이 문서 좌표 경계를 알아 부분 다시 그리기가 건너뛴다 |
| `BorderPainter` | 변마다 너비/스타일/색, 미터 조인 사다리꼴(solid/dashed/dotted/double/groove/ridge/inset/outset) |
| `TextRenderer` | 안전한 텍스트 경로(아래), 글자/단어 간격, 장식선(underline/overline/line-through, solid/double/dotted/dashed/wavy) |
| `PagePainter` | 상태 저장/복구(변환, 클립, 색, 페인트, 스트로크, 글꼴, 합성, 모든 렌더링 힌트), 클립 밖 명령/그룹 건너뛰기, 안티앨리어싱 끔(도형)/켬(텍스트), 불투명도 그룹은 화면 밖 이미지에 그려 한 번 합성 |
| `HitTester`/`HitResult` | 점 → 가장 깊은 상자/요소/가장 가까운 `a[href]`·`area[href]`(절대 URL)/텍스트 노드와 캐럿 오프셋, `getBoundingBox`/`getClientRects`, 화면↔문서 변환 |
| `RenderedPage`/`PageRenderer`/`ImageReplacedResolver` | 불변 스냅샷과 편의 진입점 |

## 복합 문자 그리기 결정 (JRE 6u45 / Windows 실험, 2026-11-16)

최소 재현(별도 프로세스, `Font("Dialog",PLAIN,20)`, 헤드리스 BufferedImage, 문자열은 코드 포인트로 생성):

| 방법 | 히브리어 שלום | 아랍어 | 태국어 | 데바나가리 | 한글 | e+결합 악센트 |
| --- | --- | --- | --- | --- | --- | --- |
| `drawString(전체)` | **아무것도 안 그림(ink 0)** | 그림 | 그림 | 그림 | 그림 | 그림 |
| 글자별 `drawString` | 그림 | 그림 | 그림 | 그림(결합 안 됨) | 그림 | 그림 |
| `TextLayout.draw` | **JVM 종료(rc=1)** | 그림 | 그림 | 그림 | 그림 | 그림 |
| `createGlyphVector`+`drawGlyphVector` | 그림 | 그림 | 그림 | 그림(결합됨) | 그림 | 그림 |
| `getStringBounds`/`FontMetrics.stringWidth/charWidth`/`canDisplayUpTo` | 이 실험에서는 모두 동작(레이아웃 작업자는 다른 환경에서 충돌을 관찰) | | | | | |

결정: `TextRenderer.isComplex`(히브리어~라오어 블록 0590–0EFF, 미얀마, 크메르, 양방향 제어, 아랍 표시 형태)에 드는 텍스트는 **`TextLayout` 을 쓰지 않고** `Font.createGlyphVector` + `drawGlyphVector` 로 그린다. 오른쪽에서 왼쪽 조각(홀수 bidi 수준)은 코드 포인트 단위로 뒤집어 시각 순서로 그린다(모양 변환 없음: 아랍 문자는 독립형, 인도계 결합 불완전, 거울 문자 미적용). 나머지(한글, 라틴, 결합 문자, 서로게이트 쌍)는 `drawString`; 글자/단어 간격이 있으면 클러스터(기본 문자+결합 문자/서로게이트 쌍)마다 `drawString`.

## 알려진 한계

- 모서리 둥글기(`border-radius`), 그라디언트, 그림자, `text-shadow`, `object-fit`, 다중 배경 `background-clip: text` 는 P10-04.
- float/절대/고정 배치/z-index 쌓임 맥락(P10-01): 단위는 인라인 단계에서 그리므로 형제의 블록 배경과 섞이지 않는다(부록 E 와 다른 점).
- `audio` 는 `controls` 가 있으면 최소 컨트롤 막대만 그린다(실제 컨트롤은 P18-08), `canvas`/`iframe`/폼 컨트롤 그리기는 P20/P11.
- 텍스트 오프셋(`HitResult.getTextOffset`)은 복합 문자에서는 폭 비례 근사, 그 밖에는 클러스터 경계의 advance 로 정한다.

## 작업 기록

### 2026-11-16 P09-04 ~ P09-06 디스플레이 리스트, 페인터, 히트 테스트
완료한 내용: 위 구현 표 전부. 시험은 실제 `HtmlParser` + `CssEngine` + `LayoutEngine` 으로 페이지를 만들고 헤드리스 `BufferedImage` 의 픽셀을 확인한다(전체 화면 비교 없음).
변경 파일: `src/org/duckdns/hjow/swingwebpanel/render/*`(19개), `tests/src/org/duckdns/hjow/swingwebpanel/render/{RenderTestSupport,PaintTest,TextPaintTest,HitTestTest,ImagePaintTest}.java`, 이 문서. layout 패키지는 수정하지 않았다.
검증 결과: `scripts/pkgtest.sh render "render layout css html dom core net storage content media @root" "render"` (JDK 6u45) → 37 passed / 0 failed(기반 시설 7건 포함, render 30건). sourcecheck 0 위반.
  시험 범위: 배경/경계선(변별 너비·색·미터 모서리, dashed/double/groove), 캔버스 배경(루트 > body), overflow 클립, 불투명도 그룹(자식 겹침이 어두워지지 않음), 부분 다시 그리기 = 전체 그리기(클립 안 픽셀 동일, 밖은 그대로), Graphics2D 상태 복원(변환/클립/색/스트로크/글꼴/합성/힌트),
  스크롤+배율 매핑, 그룹 중첩, visibility:hidden, 텍스트 잉크 위치(상자 밖에 없음), 장식선 행, 목록 표식(disc/circle/square 모양, 번호 텍스트), 글자 간격, 한글/결합 문자/서로게이트 쌍, 히브리어·아랍어·태국어·데바나가리·RTL 문단(크래시 없음),
  히트 테스트(중첩 블록, 링크의 절대/상대 URL, 중첩 인라인, 텍스트 오프셋 2.4→2/2.6→3, 스크롤/배율, 클라이언트 사각형, 경계 상자, 문서 밖), 이미지(자연 크기/속성 크기/애니메이션 프레임 선택/한 번 재생), 대체 그림(지원하지 않는 URL + alt 를 텍스트로, 비디오 안내, 오디오 컨트롤/실패), 배경 이미지 반복.
미해결 사항: 위 "알려진 한계". 실제 화면(JFrame)에서의 그리기와 입력은 호스트 통합 후 수동 확인(이 환경에서는 헤드리스만 검증).
체크박스 후보: P09-04, P09-05, P09-06. P09-09 의 그리기 절반(레이아웃 절반은 LAYOUT-A). P09-07(preferredSize/재배치)과 P09-08(통합)은 조정자가 `SwingWebPanel` 에 연결한 뒤.
다음 작업 번호: 조정자의 SwingWebPanel 통합(P09-07/08), 이후 P10.
