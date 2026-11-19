# CANVAS-A 진행 기록 (P20-05 Canvas 2D, P00-08 의 2D 캔버스/WebGL·WebGPU 예외)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/js/host/canvas/**`(신규 패키지), `src/org/duckdns/hjow/swingwebpanel/js/host/CanvasHostImpl.java`(신규, 페이지와 캔버스 패키지의 어댑터),
`src/org/duckdns/hjow/swingwebpanel/render/{CanvasSource,CanvasSizes}.java`(신규), 기존 공유 파일의 최소 변경(`js/host/{HostEnvironment,PageScripts,ElementExtras,ImageLoads}.java`,
`core/{PageRuntime,PagePresentation,PageResources}.java`, `render/{DisplayListBuilder,ImageReplacedResolver,ImageResource}.java`),
시험 `tests/src/org/duckdns/hjow/swingwebpanel/js/host/canvas/**`, `tests/src/org/duckdns/hjow/swingwebpanel/core/{CanvasIntegrationTest,CanvasRepaintTest}.java`, `tests/.../js/host/{PageFixture,ElementExtrasTest}.java`(수정), 이 문서.
작업 브랜치: `worktree-agent-a15487a775bef4101`(별도 worktree). 커밋 번호는 `20261117-201`부터 썼다.
시험 실행: 빠른 반복은 `build/cvc.sh`/`build/cvt.sh`(작업자 로컬 스크립트, `build/` 는 커밋 대상이 아님. JDK 6u45 `javac -sourcepath` 로 컴파일하고 `org.duckdns.hjow.swingwebpanel.testing.TestRunner <스위트>` 실행),
전체 회귀는 `sh scripts/pkgtest.sh canvasfull2 "js dom html css net storage core content layout render download media @root" "js core render @root layout content download dom html css net storage media" --filter org.duckdns.hjow.swingwebpanel.`
(자기 worktree 이므로 다른 작업자의 미완성 코드와 이미 격리되어 있다). 모든 시험은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로만 실행했다.
린트: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/js/host/canvas tests/src/org/duckdns/hjow/swingwebpanel/js/host/canvas ...` 0 위반(마지막 실행 기록은 아래 검증 결과).

참고 규격(조회 도구 `tools/webfetch`, 새 JRE = Zulu 25.28+85 로 실행):
- WHATWG HTML Living Standard, 4.12.5 The canvas element — `https://html.spec.whatwg.org/multipage/canvas.html`(조회 2026-11-17, `Last-Modified: Mon, 16 Nov 2026 15:32:22 GMT`,
  `ETag "6afb2206-c07ec"`, `--text` 평문 232,133 바이트; 저장 `build/specs/whatwg-canvas.txt`, 커밋 대상 아님). 본문과 **대조해 확인한 항목**: `drawImage` 의 음수 크기(규격은 "이미지는 원래 방향으로 처리한다"이므로 뒤집지 않고 사각형만 정규화한다.
  처음에는 거울 반전으로 구현했다가 이 문장을 읽고 고쳤다), 그림자 알고리즘(σ = shadowBlur/2, 색은 그림자 색, 알파는 그림자 색 알파의 곱, copy 에서는 그림자가 가려짐, 그림자의 합성 연산자), 그리기 모델(전역 알파 곱 → 그림자 → 원본의 합성 순서), `fillText` 의 maxWidth 규칙(0 이하와 NaN 은 아무것도 그리지 않음),
  `globalAlpha`/`globalCompositeOperation`/`shadowBlur` 의 잘못된 값 무시, `putImageData` 의 변경 사각형 자르기, 크기 속성의 기본값 300/150.
  **본문과 줄 단위로 대조하지 않은 부분**: 나머지 알고리즘(경로 만들기의 하위 경로 규칙, `arcTo`/`arc`/`ellipse`/`roundRect` 의 수식, 그라디언트 정지점·두 원 방사형 규칙, 패턴, `getImageData`/`createImageData`/`ImageData` 생성자의 오류 순서, TextMetrics 의 기준선 값)는 규격 알고리즘에 대한 지식으로 구현했고 시험 기대값도 같은 이해에서 만들었다.
  규격이 정하지 않은 값(글꼴 대체, 글자 모양, 안티앨리어싱, `hanging`/`ideographic` 기준선 비율)은 Chrome 의 값에 가깝게 정했으나 Chrome 과 화소를 비교하지 않았다.
- W3C HTML5 Recommendation 2014-10-28, 4.11.4 The canvas element(`https://www.w3.org/TR/2014/REC-html5-20141028/scripting-1.html`, 조회 2026-11-16, 이전 작업자의 저장 파일 `build/html5-scripting-20141028.txt`): 요소의 크기 속성, 대체 콘텐츠, 보안 절만 참고했다.
- 합성 연산자 공식(W3C Compositing and Blending Level 1)과 SVG 1.1 부록 F.6 의 호 변환(`Path2D` 문자열)은 **규격을 조회하지 않고** 알려진 공식으로 구현했다. 합성은 불투명 화소 조합으로만 값을 확인했고, 반투명 화소의 값은 다른 구현과 비교하지 않았다.

## 작업 기록

### 2026-11-17 P20-05 Canvas 2D 와 HTMLCanvasElement (P00-08 의 2D 지원, WebGL/WebGPU 는 null)

현재 작업 번호: P20-05(구현과 자동 검증 완료, 실제 Swing 창 확인은 하지 않음. 체크박스 후보는 아래).

완료한 내용:

1. **`HTMLCanvasElement`**(`js/host/canvas/CanvasBindings`): `width`/`height`(속성 반영, 기본 300×150, 두 속성을 따로 기본값으로 되돌림, 음이 아닌 정수 해석 규칙, 2147483647 초과는 기본값; 값을 지정하면 같은 값이라도 `setAttribute` 로 지정해도 비트맵을 투명하게 새로 만들고 컨텍스트 상태를 초기화하되 컨텍스트 객체는 그대로),
   `getContext('2d' [, {alpha:false}])`(같은 컨텍스트를 돌려주고 `canvas` 로 요소를 안다; `alpha:false` 는 불투명 검정으로 시작하고 읽을 때 검정 위에 합침; 다른 옵션은 무시),
   **`webgl`/`webgl2`/`experimental-webgl`/`webgpu`/`bitmaprenderer`/모르는 형식은 `null`**, `WebGLRenderingContext`/`WebGL2RenderingContext`/`GPU`/`navigator.gpu` 는 정의하지 않음(자동 시험과 실제 컴포넌트 시험으로 확인),
   `toDataURL(type, quality)`(PNG 기본, `image/jpeg`(투명 화소는 검정, 품질 0..1, 기본 0.92), 그 밖의 형식은 PNG, 면적이 없는 캔버스는 `"data:,"`), `toBlob(cb, type, quality)`(지금 인코딩하고 다음 작업에서 `Blob` 으로 콜백, 면적이 없으면 null; `Blob` 은 페이지의 전역 `Blob` 생성자로 만든다),
   오염된 캔버스의 `getImageData`/`toDataURL`/`toBlob` 은 `SecurityError`. 레이아웃은 **치환 요소**이며 자연 크기는 속성(`render/CanvasSizes`, `ImageReplacedResolver`), CSS `width`/`height` 로 늘고 줄며, 안의 대체 콘텐츠는 그리지 않는다.
2. **`CanvasRenderingContext2D`**(`Context2D`, `Context2DBindings`; 순수 Java2D `BufferedImage` `TYPE_INT_ARGB`(알파를 곱하지 않은 화소), GPU/네이티브 없음, JVM 전역 Java2D 속성 설정 없음):
   상태 `save/restore/reset`(깊이 10000), 변환 `scale/rotate/translate/transform/setTransform(숫자 6개 또는 a~f 객체)/resetTransform/getTransform`(DOMMatrix 가 없어 a~f, m11 등을 가진 평범한 객체),
   경로 `beginPath/closePath/moveTo/lineTo/quadraticCurveTo/bezierCurveTo/arcTo/arc/ellipse/rect/roundRect`(점은 추가할 때의 행렬로 변환해 장치 좌표로 보관; 호는 3차 베지어, 사분원 이하로 쪼갬), `fill/stroke/clip`(`nonzero`/`evenodd`),
   `fillRect/strokeRect/clearRect`, `fillStyle/strokeStyle`(CSS 색 문자열 — 프로젝트의 `CssColor` 파서 재사용, `currentcolor` 는 요소의 색 —, `CanvasGradient`, `CanvasPattern`; 읽을 때 `#rrggbb` 또는 `rgba(r, g, b, a)`),
   `createLinearGradient/createRadialGradient/addColorStop`(RGBA 비곱셈 보간, 같은 오프셋은 날카로운 경계, 두 원 방사형은 규격의 "가장 큰 ω" 규칙), `createPattern`(`repeat/repeat-x/repeat-y/no-repeat`, 생성 시점의 복사본, 쌍선형/최근접 샘플링),
   선 `lineWidth/lineCap/lineJoin/miterLimit/setLineDash/getLineDash/lineDashOffset`(선은 사용자 좌표에서 계산해 균일하지 않은 확대에서도 맞음), 그림자 `shadowColor/Blur/OffsetX/OffsetY`(가우시안, 오프셋은 장치 화소),
   `globalAlpha`, **`globalCompositeOperation` 26종 전부**(`source-over`/`in`/`out`/`atop`, `destination-over`/`in`/`out`/`atop`, `lighter`, `copy`, `xor`, 분리 가능한 블렌드 11종, 분리할 수 없는 4종 `hue/saturation/color/luminosity`; 원본이 투명한 곳도 바꾸는 `source-in/out`, `destination-in/atop`, `copy` 는 클립된 캔버스 전체에 적용),
   문자 `font`(CSS 단축 구문 해석, 단위 px/pt/pc/in/cm/mm/q/em/rem/ex/%, 크기 키워드; 직렬화) `textAlign/textBaseline/direction`, `fillText/strokeText`(maxWidth), `measureText`(`TextMetrics` 12개 값),
   이미지 `drawImage`(3·5·9 인수, `img`·다른 `canvas`·자기 자신·`video`(그릴 프레임이 없어 아무것도 안 그림), 원본/대상 사각형 자르기, 음수 크기는 정규화), 화소 `createImageData/getImageData/putImageData/ImageData`(생성자 두 형태; `data` 는 `Uint8ClampedArray` 이고 바이트를 공유; `putImageData` 는 행렬·클립·알파·그림자·연산자를 무시하고 변경 사각형 지원),
   `isPointInPath/isPointInStroke`, `imageSmoothingEnabled/Quality`(꺼 두면 최근접, `high` 는 쌍삼차), `getContextAttributes`, `isContextLost`. 잘못된 인수는 규격대로 처리한다(NaN/무한대는 조용히 무시, 음수 반지름은 `IndexSizeError`, 필수 인수 부족은 `TypeError`, 알 수 없는 열거값과 범위 밖 값은 무시).
3. **`Path2D`**(`Path2DValue`, `Path2DBindings`, `PathData`): 생성자(빈 경로, 복사, SVG 경로 데이터 `M m L l H h V v C c S s Q q T t A a Z z` — 암묵적 반복, 붙여 쓴 숫자와 호 플래그, 첫 오류에서 멈추고 그 앞을 남김), `addPath(path, matrix)`(자기 자신도 가능), 경로 메서드 전부(`roundRect` 포함),
   그리고 컨텍스트의 `fill/stroke/clip/isPointInPath/isPointInStroke` 의 경로 인수(사용하는 때의 행렬이 경로에 적용됨).
4. **오염 규칙(origin-clean)**(`CanvasHostImpl`, `ImageResource` 의 출처 정보, `PageResources`): 다른 오리진 이미지를 `drawImage` 하면 캔버스가 오염되어 `getImageData`/`toDataURL`/`toBlob` 이 `SecurityError`. 같은 오리진(최종 URL 기준)은 깨끗하다.
   `crossorigin` 속성이 있고 응답의 `Access-Control-Allow-Origin`(`use-credentials` 이면 `Access-Control-Allow-Credentials: true` 도)이 문서 오리진을 허용하면(P04-11 `OriginPolicy.corsAllows`) 깨끗하다. `crossorigin` 이 없으면 응답에 허용 헤더가 있어도 오염된다.
   오염은 다른 캔버스로 `drawImage` 하거나 오염된 이미지/캔버스로 만든 `CanvasPattern` 으로 **칠할 때** 전달되고, 크기 속성을 다시 지정하면 지워진다. 이미지 표시가 된다는 이유로 본문을 스크립트에 노출하지 않는다.
5. **화면 연결**: 렌더러는 캔버스 비트맵의 **불변 스냅샷**(`DecodedImage`, 복사본)을 `DisplayCommands.Image` 로 그린다(`render/CanvasSource` 를 `PageResources` 가 구현, `DisplayListBuilder.paintReplaced` 의 canvas 분기). 스냅샷은 마지막 스냅샷 뒤 그림이 바뀌었을 때만 복사하고(바뀌지 않으면 같은 객체),
   변경 알림은 스냅샷마다 한 번(`CanvasSurface.markChanged` → `HostEnvironment.canvasChanged` → `PageRuntime` → `PagePresentation.canvasChanged`)이며, 이 알림은 **레이아웃을 무효화하지 않고** 보관한 레이아웃에서 디스플레이 리스트만 다시 만든다(`PagePresentation.render` 의 `paintDirty` 경로, `PageRenderer.fromLayout`).
   그래서 `requestAnimationFrame` 애니메이션이 프레임마다 페이지를 다시 배치하지 않는다. 그리는 동안 문서 스레드가 같은 비트맵을 바꿔도 EDT 는 복사본을 그린다. 확대/CSS 크기는 `Image` 명령이 상자 크기로 쌍선형 확대한다. 히트 테스트와 마우스 이벤트(`offsetX/offsetY` 포함)는 일반 치환 요소와 같다.
6. **수명/자원(P21 연결)**: 비트맵과 컨텍스트는 페이지 소유(`PageScripts.dispose` → `CanvasBindings.dispose` 가 모든 표면을 해제)이며 페이지 교체/`clean()`/`dispose()` 에서 사라진다. 요소는 약한 참조로만 잡는다(`WeakHashMap` 값이 키를 참조하지 않음). 페이지를 폐기하고 버리면 표면과 스냅샷이 수거됨을 시험으로 확인했다.
   **한도**(`CanvasLimits`, 문서화): 한 변 ≤ 16384, 화소 수 ≤ 16,777,216(4096×4096; 화소당 4바이트이므로 비트맵 64 MiB, 스냅샷 하나에 같은 양). 넘으면 비트맵을 할당하지 않는다: `getContext` 는 동작하고 그리기는 조용히 무시되며 `getImageData` 는 `InvalidStateError`, `toDataURL` 은 `"data:,"`, 레이아웃은 속성의 크기를 그대로 쓴다.
   그림자는 버퍼가 4M 칸을 넘거나 번짐 계산량이 4억 곱셈-덧셈을 넘으면 그리지 않는다(스크립트가 호출 하나로 문서 스레드를 오래 붙잡지 못하게). `ImageData` 는 16M 화소까지, `save()` 중첩은 10000 까지.

변경 파일:
- 신규 소스(`src/org/duckdns/hjow/swingwebpanel/js/host/canvas/`): `package-info`, `CanvasLimits`, `CanvasBitmap`, `CanvasSurface`, `Context2D`, `DrawState`, `PathBuilder`, `PathData`, `Path2DValue`, `Composites`, `ShadowBlur`, `CanvasGradient`, `CanvasPattern`, `CanvasImage`, `CanvasColors`, `CanvasFont`, `CanvasText`,
  `TextMetricsValue`, `ImageDataValue`, `CanvasEncoder`, `CanvasHost`, `CanvasBindings`, `Context2DBindings`, `Path2DBindings`; `src/org/duckdns/hjow/swingwebpanel/js/host/CanvasHostImpl.java`; `src/org/duckdns/hjow/swingwebpanel/render/{CanvasSource,CanvasSizes}.java`.
- 수정 소스(최소 변경): `js/host/HostEnvironment`(`getLoadedImage`, `canvasChanged` 기본 구현 2개), `js/host/PageScripts`(`canvases` 필드, `canvasSource()`, `dispose` 에서 해제), `js/host/ElementExtras`(옛 `getContext` null 스텁을 `page.canvases().install(g)` 로 교체), `js/host/ImageLoads`(`isBroken`),
  `core/PageRuntime`(두 훅과 `setCanvasSource` 연결), `core/PagePresentation`(`paintDirty` 경로, `canvasChanged`, `peekImage`, `setCanvasSource`), `core/PageResources`(`CanvasSource` 구현, `peekImage`, 이미지의 최종 URL/CORS 헤더 보관), `render/DisplayListBuilder`(canvas 분기 8줄), `render/ImageReplacedResolver`(canvas 자연 크기), `render/ImageResource`(출처 정보 필드).
- 시험: `tests/src/org/duckdns/hjow/swingwebpanel/js/host/canvas/{CanvasBasicsTest,CanvasPathsTest,CanvasPaintTest,CanvasPixelsTest,CanvasPath2DTest}.java`, `tests/.../core/{CanvasIntegrationTest,CanvasRepaintTest}.java`, 수정: `tests/.../js/host/PageFixture.java`(`Env.loadedImages`, `canvasChanges`, `getLoadedImage`/`canvasChanged` 재정의,
  `ManualScheduler` 의 대기열 동기화는 내가 먼저 고쳤으나 master 의 같은 수정(`e7f67ea`)과 겹쳐 rebase 때 master 의 것을 썼다: 전체 회귀에서 한 번 본 간헐 실패 `MediaElementsTest.testNewLoadCancelsTheOldOne`(부하 중 `post` 와 `advance` 가 `PriorityQueue` 를 동시에 건드려 NPE)의 원인이다), `ElementExtrasTest`(옛 "2D 는 null" 기대값을 갱신).

검증 결과(JDK 6u45, 실제 실행):
- 캔버스 시험 55건 통과/실패 0: `CanvasBasicsTest` 9, `CanvasPathsTest` 11, `CanvasPaintTest` 11, `CanvasPixelsTest` 10, `CanvasPath2DTest` 4, `core/CanvasIntegrationTest` 9(실제 `SwingWebPanel` + `TestHttpServer`, 화면 밖 `paint` 로 읽은 화소), `core/CanvasRepaintTest` 1.
  화소는 안티앨리어싱이 값을 바꾸지 않는 정수 사각형·축 정렬 도형에서 `getImageData` 로 확인하고, 곡선은 포함/제외 영역(`isPointInPath`/`isPointInStroke`, 안쪽·바깥 화소)과 반투명 가장자리 범위로 확인했다. 합성 26종은 불투명 두 색의 왼쪽(원본 없음)·오른쪽(원본 있음) 화소의 기대값 표로 확인했다.
  파일은 `ImageIO` 로 다시 디코딩해 크기와 화소를 확인했다(PNG 화소 정확 일치, JPEG 는 근사). 실제 컴포넌트 시험: 그린 화소가 컴포넌트에 나타남(+`getContent()` 원문 불변), CSS 확대/배경/대체 콘텐츠 미표시, 애니메이션 40프레임, 크기 변경 후 재배치, 클릭(`clientX/Y`, `offsetX/Y`), WebGL/WebGPU 없음,
  실제 HTTP 응답(같은 오리진/다른 오리진/CORS 허용/허용 없는 `crossorigin`)의 오염 규칙, 페이지 교체/`clean()`/`dispose()`, 한도를 넘는 캔버스.
- 린트: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/js/host/canvas src/org/duckdns/hjow/swingwebpanel/render/Canvas*.java ... tests/src/org/duckdns/hjow/swingwebpanel/js/host tests/src/org/duckdns/hjow/swingwebpanel/core/Canvas*.java` → 0 위반(소유 파일과 수정한 공유 파일 전부).
- 전체 회귀(`sh scripts/pkgtest.sh canvasfull4 "js dom html css net storage core content layout render download media @root" "js core render @root layout content download dom html css net storage media" --filter org.duckdns.hjow.swingwebpanel.`,
  master `8a094b9`(`20261117-103`, LAYOUT-E 병합 포함) 위로 rebase 한 마지막 코드 커밋 `20261117-207` 상태): **2223 통과 / 실패 0 / 건너뜀 2**(건너뜀: `HttpFetcherTest.testRefererOriginAndCacheHeaders` — JRE 6 `HttpURLConnection` 이 `Origin` 을 보내지 못함,
  `JarContentTest.testProductJarContainsOnlyProductFiles` — 제품 JAR 을 만들지 않음; 둘 다 기존 사유). 이보다 앞선 전체 회귀(master `de5971c` 기준 2088 통과/실패 1, `ebb566e` 기준 2155 통과/실패 0, `599dd83` 기준 2159 통과/실패 0)에서 실패는 아래 간헐 실패 1건뿐이었다.
- 기록할 간헐 실패: 첫 전체 회귀에서 `MediaElementsTest.testNewLoadCancelsTheOldOne` NPE 1건(위 `ManualScheduler` 경쟁, 단독 3회 재실행은 통과. 조정자가 master 에 같은 수정을 넣었으므로 rebase 때 master 의 `ManualScheduler` 를 그대로 쓰고 내 `Env` 추가분만 얹었다).

미해결 사항(한계. 숨기지 않고 기록):
1. **실제 Swing 창에서 확인하지 않았다.** 그림은 컴포넌트를 화면 밖 `BufferedImage` 에 `paint` 해서 읽은 화소로만 확인했다(P22-10 의 "자동 검증"과 "실제 확인" 구분). 한 번 일회용 프로그램(저장하지 않음)으로 데모 페이지(그라디언트 띠와 한글을 포함한 글자, 그림자 있는 원, 방사형 그라디언트, 둥근 꺾임의 꺾은선, 점선 베지어, 회전+`destination-out`, `Path2D` 문자열, `roundRect` 클립, `strokeText`)를
   화면 밖에 그려 PNG 로 저장하고 눈으로 확인했으며 의도한 모양이었다. 이것도 실제 창이 아니라 같은 화면 밖 그리기이다.
2. **구현하지 않은 API**(프로토타입에 정의하지 않음): `OffscreenCanvas`, `ImageBitmap`/`createImageBitmap`, `ImageBitmapRenderingContext`(`bitmaprenderer` 는 null), `transferControlToOffscreen`, `captureStream`, `createConicGradient`, `drawFocusIfNeeded`, `scrollPathIntoView`, 히트 영역(`addHitRegion`),
   `filter`, `fontKerning`, `fontStretch`, `fontVariantCaps`, `letterSpacing`, `wordSpacing`, `textRendering`, `lang`, 컬러 스페이스 옵션(`colorSpace`, `display-p3`), `willReadFrequently`/`desynchronized`(무시), `contextlost`/`contextrestored` 이벤트, `DOMMatrix`(`getTransform` 은 a~f/m11.. 를 가진 평범한 객체),
   `drawImage` 의 `SVGImageElement`/`OffscreenCanvas`/`ImageBitmap`/`VideoFrame` 소스(`img` 로 쓴 SVG 이미지는 됨).
3. 그리기 차이: 클립 마스크는 안티앨리어싱하지만 `drawImage` 의 가장자리는 Java2D 의 `clip` 이라 안티앨리어싱이 없다(축 정렬·정수 위치에서는 정확). 이미지의 `imageSmoothingQuality` 는 `low`/`medium` 이 쌍선형, `high` 가 쌍삼차이다. 애니메이션 이미지는 첫 프레임만 그린다. `srcset` 의 밀도는 `drawImage` 의 자연 크기에 반영하지 않는다(픽셀 크기를 쓴다).
   글자는 `GlyphVector` 윤곽을 채우므로 힌팅/LCD 가 없고(Chrome 과 다른 모양), 복합 문자 체계(아랍/인도계 등)는 JRE 의 `layoutGlyphVector` 에 맡긴다. `direction="inherit"` 는 요소의 `dir`/`direction` 을 읽지 않고 ltr 로 본다. 글꼴은 목록에서 텍스트를 모두 표시할 수 있는 첫 패밀리(없으면 Dialog)를 쓴다.
   `hanging`/`ideographic` 기준선과 em 높이는 비율로 근사한다.
4. 반투명 화소의 합성 값(예: `multiply` 로 반투명 위에 반투명)은 공식대로 계산하지만 다른 구현과 비교하지 않았다. 불투명 캔버스(`alpha:false`)는 읽을 때(스냅샷, `getImageData`, 내보내기) 검정 위에 합치는 방식이라, 그 사이의 중간 상태의 합성 연산은 투명도가 있는 비트맵에서 일어난다.
5. 오염 판정: `Origin` 요청 헤더를 보낼 수 없는 JRE 6 의 제약(P20-08-c)으로, `Origin` 이 있어야 `Access-Control-Allow-Origin` 을 내는 서버의 이미지는 `crossorigin` 이 있어도 깨끗해지지 못한다. 규격은 `crossorigin` 이미지가 허용을 받지 못하면 로드 자체를 실패시키지만 이 구현은 이미지를 보여 주고 캔버스를 오염시킨다(더 안전한 쪽).
   오리진이 불투명한 문서(`setContent` 로 만든 `about:text` 와 `about:blank`)는 모든 이미지를 외부 것으로 보므로 그런 문서에서 이미지를 그린 캔버스는 읽을 수 없다. 같은 이유로 그런 문서에는 `sessionStorage` 도 없다(기존 동작).
6. 성능: 그림이 바뀐 프레임마다 비트맵 복사(`w*h*4` 바이트)와 디스플레이 리스트 재구성이 든다(레이아웃은 아님). 큰 캔버스의 `Composites.apply` 는 화소마다 배열을 만드는 일반 경로라 `source-over` 외 연산을 캔버스 전체에 쓰면 느리다(4096×4096 에서 호출당 수 초). 한도 안에서 UI 는 문서 스레드가 아니라서 멈추지 않지만 스크립트 한 작업이 길어진다.
7. `document.getElementById('c').getContext('2d')` 를 `<canvas>` 가 문서에 붙기 전에 불러도 동작한다. 서로 다른 문서의 요소를 `adoptNode` 하는 경우는 시험하지 않았다.

다음 작업 번호: P20-05 의 한계 항목(위 2번)은 필요하면 하위 항목(`P20-05-b` OffscreenCanvas/ImageBitmap, `P20-05-c` 텍스트 확장 속성과 `filter`)으로 나눠 추가한다. 이 작업의 후속으로는 실제 Swing 창 확인(P22-10), P21-01 의 소유 목록 표에 "캔버스 표면(`CanvasBindings`) ← `PageScripts.dispose`" 행 추가.

조정자 요청:
- **FEATURE_MATRIX**(제안 문구; 표의 열은 기능 ID · 요구사항 · 규격 · 구현 클래스 · 검증 사례 · 지원 상태 · 제약):
  - `WEB-05`(Canvas 2D): 규격 열에 "WHATWG HTML LS 4.12.5(조회 2026-11-17), HTML5 2014 REC 4.11.4"; 구현 `js.host.canvas.{CanvasBindings,Context2D,Context2DBindings,CanvasSurface,CanvasBitmap,Path2DBindings,PathData}`, `render.{CanvasSource,CanvasSizes}`, `core.{PageResources,PagePresentation}`;
    검증 `CanvasBasicsTest`/`CanvasPathsTest`/`CanvasPaintTest`/`CanvasPixelsTest`/`CanvasPath2DTest`(JS 시험 45건), `core/CanvasIntegrationTest`(9)/`CanvasRepaintTest`(1);
    지원 상태 **"부분 구현"**(상태·변환·경로·`Path2D`·색/그라디언트/패턴·선·그림자·합성 26종·문자·`drawImage`·화소·`toDataURL/toBlob`·오염 규칙은 화면 밖 화소로 검증 완료. `OffscreenCanvas`/`ImageBitmap`/`filter`/`letterSpacing` 등과 실제 창 확인은 미구현·미확인);
    제약 "구현하지 않은 API 목록은 `docs/progress/canvas.md` 미해결 2번, 한도 한 변 16384·16M 화소, 합성의 반투명 값은 다른 구현과 비교하지 않음".
  - `HEI-07`: `HTMLCanvasElement` 를 "부분 구현(`width/height/getContext/toDataURL/toBlob` 검증 완료, `captureStream/transferControlToOffscreen` 없음)"으로, `HTMLTemplateElement`/`Dialog`/`Details` 는 기존 상태 유지.
  - `HTML-12`(스크립팅 `canvas` 부분)와 `HTML-08`(임베디드 `canvas`): `canvas` 요소는 치환 요소로 배치/그림/이벤트 검증 완료, 대체 자식 콘텐츠는 그리지 않음.
  - `EXC-02`: "검증 완료" — `getContext('webgl'/'webgl2'/'experimental-webgl'/'webgpu')` 는 `null`, `WebGLRenderingContext`/`WebGL2RenderingContext`/`GPU`/`navigator.gpu` 미노출(`CanvasBasicsTest.testOnlyTwoDimensionalContext`, `CanvasIntegrationTest.testNoThreeDimensionalContext`).
  - 새 행 제안: `WEB-05a Path2D`(부분 구현 → SVG 경로 문자열 포함 검증 완료, `CanvasPath2DTest`), `WEB-05b ImageData`(검증 완료: `CanvasPixelsTest`), `WEB-05c OffscreenCanvas/ImageBitmap`(미구현).
- **ARCHITECTURE.md**: 패키지 표에 `js.host.canvas`(캔버스 비트맵·2D 엔진·스크립트 연결)와 `render.CanvasSource`(렌더러가 읽는 캔버스 그림) 추가, 데이터 흐름 한 줄: "스크립트(문서 스레드) → `Context2D` → `CanvasBitmap`(int[] ARGB) → `CanvasSurface.snapshot()`(변경이 있을 때만 복사) → `CanvasBindings`(=`CanvasSource`) → `PageResources.getCanvasImage` → `DisplayListBuilder`(치환 요소 canvas)".
  캐시 계층 표의 "배치 캐시" 행에 "캔버스 그림만 바뀌면 배치를 재사용하고 디스플레이 리스트만 다시 만든다(`PagePresentation.paintDirty`)" 추가. 한도 상수는 `CanvasLimits`.
- **P21-01 소유 목록**: "캔버스 비트맵/컨텍스트/그라디언트/패턴 — 페이지 소유 — `PageScripts.dispose()` → `CanvasBindings.dispose()`(표면 해제, 문서 리스너 제거). 요소 약한 참조. 시험 `CanvasPixelsTest.testCollectedWithThePage`".
- **AGENTS.md 의 `P20-05`**: 아래 "체크박스 후보" 참고.

체크박스 후보:
- **P20-05 — 후보로 제안한다(조정자 판단)**: 항목 본문의 요구(상태/변환/경로/색/선/그리기/문자/이미지/픽셀 API 를 자체 객체와 Java2D 로 구현, 이미지의 교차 오리진 상태와 픽셀 조회 제한, 크기 변경 시 상태 초기화, TypedArray 와의 연결 검증, `getContext('2d')` 지원과 WebGL/WebGPU 요청의 null, `navigator.gpu`/WebGL 인터페이스 미노출)은
  구현하고 JRE 6 에서 검증했다. 다만 (a) 실제 Swing 창 확인이 없고(P22-10 에서 따로 기록), (b) 위 2번의 미구현 API 가 남아 있다. 이를 하위 항목으로 나눠 체크하려면 `P20-05-a`(본문의 요구 전부: 구현·검증 완료)와 `P20-05-b`(OffscreenCanvas/ImageBitmap/filter/텍스트 확장 속성: 미구현)로 쪼갠 뒤 a 만 체크하는 것을 권한다.
- **P00-08 의 2D 캔버스/WebGL·WebGPU 동작 정책**(컨텍스트 요청의 null 반환, 관련 전역 객체 미노출)은 구현 근거가 생겼다(위 `EXC-02`).
- P09-07/P10 계열 항목은 건드리지 않았다. 레이아웃 변경은 `ImageReplacedResolver` 의 canvas 분기뿐이며 `layout/**` 는 수정하지 않았다.


---

# CANVAS-B 진행 기록 (P20-05-b 나머지)

작업 브랜치: `worktree-agent-ab7a4859c17b89489`(master `2331776` 위). 커밋 번호는 `20261117-730`부터 썼다.
시험 실행: `sh scripts/pkgtest.sh <이름> "js dom html css net storage core content layout render download media @root" "js core render @root layout content download dom html css net storage" --filter org.duckdns.hjow.swingwebpanel.js.host.canvas.`
(전체 패키지 목록을 쓴다: 패키지를 줄여 부분 컴파일하면 전역 생성자가 없어 JS 시험이 "(no value) is not a constructor" 로 실패한다). 반복 작업은 로컬 증분 스크립트 `build/inc.sh`(커밋 대상 아님)로 변경 파일만 다시 컴파일했다.
모든 시험은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로만 실행했다.

## 앞선 CANVAS 작업자의 기록 누락 보충

master 에 이미 있던 `20261117-720`(OffscreenCanvas, ImageBitmap/`createImageBitmap`, `transferControlToOffscreen`, 컨텍스트 설정), `721`→`723`(`createConicGradient`)은 이 문서에 기록이 없었다.
이 작업에서 **내가 구현하지 않았고 코드를 줄 단위로 다시 검토하지도 않았다**. 확인한 것은 그 시험(`CanvasOffscreenTest` 12건, `CanvasExtrasTest.testConicGradient`)이 JDK 6 에서 통과한다는 것뿐이다.
구성(커밋 메시지와 파일 목록 기준): `js/host/canvas/{OffscreenBindings,OffscreenCanvasValue,ImageBitmapValue,BitmapOps,CanvasPromises,ContextSettings}`, `CanvasGradient` 의 원뿔형, `js/host/web/BlobBytes`.

## 규격 조회(도구 `tools/webfetch`, 새 JRE = Zulu 25.28+85 로 실행; 본문 저장은 `build/specs/*`, 커밋 대상 아님)

- **WHATWG HTML Living Standard, 4.12.5 The canvas element** `https://html.spec.whatwg.org/multipage/canvas.html` — 조회 2026-11-17, `Last-Modified: Tue, 17 Nov 2026 11:40:10 GMT`, `ETag "6afc3d1a-c07ec"`, `--text` 평문 232,133 바이트.
  본문과 대조해 구현한 부분: 4.12.5.1.5 텍스트 스타일(`letterSpacing`/`wordSpacing` 는 CSS `<length>` 로 해석하고 해석 실패는 무시, getter 는 직렬화된 형태, `fontKerning`/`fontStretch`/`fontVariantCaps`/`textRendering` 은 열거값만 받고 나머지는 무시, `lang` 은 주어진 문자열 그대로),
  4.12.5.1.20 필터(`filter` 는 `none` 이거나 `<filter-value-list>`, 해석 실패와 CSS 전역 키워드는 무시하고 getter 는 **설정한 그대로의 문자열**, 상대 길이는 설정할 때의 요소 글꼴 크기 기준, 필터 좌표는 변환의 영향을 받지 않음), 4.12.5.1.22 그리기 모델(전역 알파를 곱한 이미지 A 가 필터의 입력, 그림자는 필터 출력 B 에서),
  4.12.5.1.14 포커스 링(`drawFocusIfNeeded`: 요소가 포커스를 가졌고 캔버스 요소의 자손일 때만, 링은 변환과 클립에는 따르고 그림자·전역 알파·합성·스타일에는 따르지 않음), IDL 의 `CanvasUserInterface` 는 `OffscreenCanvasRenderingContext2D` 에 포함되지 않음(그래서 offscreen 컨텍스트에는 `drawFocusIfNeeded` 가 없다).
  IDL 의 멤버 이름을 구현과 기계적으로 대조했다(스크립트로 추출): 빠진 것은 `drawFocusIfNeeded` 하나였고 이제 구현했다.
- **W3C Geometry Interfaces Module Level 1**(`https://www.w3.org/TR/geometry-1/`, 본문 "W3C Candidate Recommendation Draft, 4 December 2025", `Last-Modified: Tue, 02 Dec 2025 16:11:17 GMT`, 99,027 바이트) — 조회 2026-11-17. 6절(DOMMatrix 인터페이스, 6.1 사전 검증과 보정, 6.2 문자열 해석, 6.3 만들기, 6.4 속성, 6.5/6.6 변환 메서드)을 줄 단위로 읽고 구현했다. 2절의 `DOMPointReadOnly.matrixTransform()` 도.
- **캔버스 히트 영역**: W3C HTML Canvas 2D Context Recommendation(`https://www.w3.org/TR/2015/REC-2dcontext-20151119/`, 조회 2026-11-18, `Last-Modified: Mon, 02 Oct 2017 10:54:24 GMT`)에는 `addHitRegion(HitRegionOptions)`/`removeHitRegion(id)`/`clearHitRegions()` 와 `MouseEvent.region` 이 있다(본문에서 7곳 확인).
  그러나 현행 WHATWG HTML LS 의 캔버스 장(위 URL, 232,133 바이트 평문)에는 `addHitRegion`, `removeHitRegion`, `clearHitRegions`, `scrollPathIntoView`, "hit region" 이 **한 번도 나오지 않는다**(검색 0건). `https://www.w3.org/TR/2dcontext2/` 는 WHATWG HTML 표준(`https://html.spec.whatwg.org/multipage/`)으로 넘어간다(조회 2026-11-18, 최종 URL 로 확인).
  → 현행 규격에서 삭제된 기능으로 보고 **구현하지 않았다**(정의하지 않음: `typeof ctx.addHitRegion === 'undefined'` 를 시험 `CanvasExtrasTest.testContextLossAndRemovedMembers` 가 확인). 브라우저들이 언제 빼 갔는지는 조회하지 않았다(미확인).

## 한 일(항목별 커밋)

1. **`DOMMatrix` / `DOMMatrixReadOnly`**(커밋 730). `dom/{DomMatrixReadOnly,DomMatrix,DomMatrixParser}`(4x4 열 우선 저장 배열 16개 + `is2D`; 2D 행렬끼리는 a~f 만으로 곱해서 무한대와 0 의 곱이 NaN 을 만들지 않음; 3D 값은 저장·곱셈·역행렬만 하고 캔버스는 2D 부분만 씀),
   `js/dom/MatrixBindings`(`DomInterfaces` 가 `GeometryBindings` 다음에 설치). 생성자(인수 없음 / CSS 변환 목록 문자열 / 숫자 6개 또는 16개 시퀀스), `fromMatrix`/`fromFloat32Array`/`fromFloat64Array`, 속성 `a`~`f`·`m11`~`m44`·`is2D`·`isIdentity`(읽기 전용은 `DOMMatrixReadOnly`, 설정자는 `DOMMatrix` 의 프로토타입에서 `is2D` 규칙 적용),
   새 행렬을 돌려주는 `translate/scale/scaleNonUniform/scale3d/rotate/rotateFromVector/rotateAxisAngle/skewX/skewY/multiply/flipX/flipY/inverse`, `transformPoint`(+`DOMPointReadOnly.matrixTransform`), `toFloat32Array/toFloat64Array`, 문자열 변환(`matrix(...)`/`matrix3d(...)`, NaN 이나 무한대가 있으면 `InvalidStateError`), `toJSON`,
   `DOMMatrix` 의 `multiplySelf/preMultiplySelf/translateSelf/scaleSelf/scale3dSelf/rotateSelf/rotateFromVectorSelf/rotateAxisAngleSelf/skewXSelf/skewYSelf/invertSelf/setMatrixValue`, 옛 이름 `SVGMatrix`·`WebKitCSSMatrix`(= `DOMMatrix`). 사전 인수(`DOMMatrixInit`, 다른 행렬도 가능)는 6.1 절대로 검증한다(`a` 와 `m11` 이 다르면 `TypeError`, `is2D:true` 와 3D 멤버 충돌 `TypeError`).
   문자열 해석은 CSS `transform` 문법(`matrix matrix3d translate(X/Y/Z/3d) scale(X/Y/Z/3d) rotate(X/Y/Z/3d) skew(X/Y) perspective`; 길이는 `px cm mm Q in pt pc` 와 단위 없는 0 만, 각도는 `deg grad rad turn` 과 단위 없는 0 만; `em`, `%`, `calc()` 는 규격대로 `SyntaxError`).
   캔버스 연결: `getTransform()` 은 현재 변환의 **복사본인** 진짜 `DOMMatrix`, `setTransform(DOMMatrix | DOMMatrix2DInit | 숫자 6개)`(a 와 m11 충돌은 `TypeError`, 유한하지 않은 값은 무시), `Path2D.addPath(path, matrix)`, `CanvasPattern.setTransform(matrix)`(새로 구현: 패턴 공간에서 칠할 때의 변환보다 먼저 적용, 뒤집을 수 없거나 NaN 이면 칠하지 않음).
2. **텍스트 확장 속성**(커밋 731). `letterSpacing`·`wordSpacing`(`TextSpacing`: `px pt pc in cm mm Q em ex ch rem` 과 단위 없는 0; 마지막 글리프 뒤에도 더해져 `measureText().width` 에 포함; 낱말 간격은 스페이스와 NBSP 뒤; `em` 은 **글자를 그릴 때** 현재 글꼴 기준),
   `fontKerning`(`normal` 이면 글꼴의 커닝 속성을 켜고 `layoutGlyphVector` 로 배치: 이 PC 의 Tahoma 는 `AVAVAVToTo` 60px 가 350.57 → 321.74 로 좁아졌으나 Arial/Times New Roman/`sans-serif`(=Arial) 는 Java2D 를 거치면 커닝 쌍이 없어 변화 없음; `auto` 와 `none` 은 커닝 없음), `fontStretch`·`fontVariantCaps`·`textRendering`·`lang` 은 **상태만 보관**(효과 없음), 모두 `save/restore/reset` 에 포함.
   **JRE 6u45 의 JVM 종료 버그를 피했다**: `Font.layoutGlyphVector`(와 `TextLayout`)에 히브리어를 주면 이 PC 의 JRE 6u45 가 `fontmanager.dll` 접근 위반으로 **JVM 전체가 죽는다**(Dialog 글꼴, 작은 `Glyph.java` 로 단독 재현; 아랍어·태국어·데바나가리·한글·라틴은 정상). 렌더러의 `TextRenderer` 가 이미 같은 이유로 `createGlyphVector` 만 쓰는데 캔버스의 `CanvasText.layout` 만 복합 문자에 `layoutGlyphVector` 를 써서
   캔버스에 히브리어를 그리면 같은 충돌이 날 수 있었다. 히브리어(U+0590~U+05FF, U+FB1D~U+FB4F)가 든 텍스트는 이제 `createGlyphVector`(오른쪽에서 왼쪽이면 문자열을 뒤집어 시각 순서)로 만든다(모양 변환 없음).
3. **`ctx.filter`**(커밋 732). 기존 레이아웃의 필터 구현을 재사용했다: `layout/FilterSpec.of(CssValue, color, LengthContext)`(공개 오버로드 추가, 상대 길이는 `LengthContext` 로 환산; 기존 `of(ComputedStyle)` 는 같은 코드를 부름), `render/CanvasFilters`(새 공개 문: `parse`/`margin`/`apply`; 화소 코드는 패키지 전용 `FilterRenderer`).
   `PropertyRegistry.parse("filter", ...)` 의 문법을 통과해야 올바른 값(전역 키워드와 `var()` 는 거부; `url()` 만 있거나 섞인 목록은 올바르지만 **아무것도 거르지 않는다**: 규격의 "문서가 로드되지 않은 SVG 필터" 처럼 필터 없이 그림). `Context2D.render` 의 레이어 경로에서 전역 알파를 곱한 뒤 필터를 적용하고 그림자·합성은 그 출력으로 한다.
   레이어는 번짐 거리만큼 넓힌다(캔버스 밖으로 번진 부분은 버림). `clearRect`/`putImageData` 는 필터를 거치지 않는다. 함수는 `blur brightness contrast drop-shadow grayscale hue-rotate invert opacity saturate sepia`.
4. **`crossorigin` 이미지 요청의 `Origin` 과 CORS 검사**(커밋 733). `RequestPurpose.IMAGE_CORS` 추가(`OriginPolicy.requiresCors` 에 포함 → 교차 오리진이면 `Origin` 을 싣고 소켓 기반 연결(P20-08-c)로 보냄), `anonymous` 는 쿠키를 같은 오리진에만, `use-credentials` 는 모든 오리진에 보내고 `Access-Control-Allow-Credentials: true` 와 정확한 오리진 필요.
   `PageResources` 의 이미지 항목을 (URL, CORS 모드)별로 나눠서 같은 주소의 일반 `<img>` 와 `crossorigin` `<img>` 가 서로 다른 가져오기를 한다(렌더러도 `render/ImageLookup`·`CorsImageSource` 로 요소에 맞는 항목을 그리므로 CORS 이미지를 두 번 가져오지 않음; 시험이 요청 수 1 을 확인). 응답이 문서 오리진을 허용하지 않으면 이미지는 **로드되지 않고 `error` 이벤트**가 나며 캔버스에는 그려지지 않는다(규격대로: 이전 구현은 이미지를 보여 주고 캔버스만 오염시켰다).
   오리진이 없는 페이지(`setContent`)는 `Origin: null` 을 싣고 `*`(자격 증명 없이)나 `null` 이라는 답으로 허용된다(`OriginPolicy.corsAllowsDocument`). 허용된 이미지는 `ImageResource` 의 CORS 헤더로 캔버스가 깨끗하다. `crossorigin` 값은 대소문자 무시로 `use-credentials` 이고 그 밖(빈 값, 모르는 값)은 `anonymous`.
   스크립트 쪽 로더는 `js/host/CorsImageRequestSource`(선택 인터페이스; 구현하지 않는 호스트는 CORS 없이 로드)와 `HostEnvironment.getLoadedImage(url, cors)` 로 연결했다. 기존 시험 `CanvasIntegrationTest.testTaintWithRealResponses` 의 "허락 없는 crossorigin" 기대를 **오염에서 `error` 로 고쳤다**(규격이 맞음).
5. **현행 규격 점검과 `drawFocusIfNeeded`**(커밋 734). 위 대조로 `drawFocusIfNeeded(element)`/`(path, element)` 를 구현했다(`Context2D.drawFocusRing`: 1 화소 어두운 링 `rgb(16,16,16)` + 양쪽 1 화소 흰 후광, 변환과 클립만 적용; 요소가 `FocusManager` 의 포커스이고 캔버스 요소의 자손일 때만; `OffscreenCanvasRenderingContext2D` 에는 정의하지 않음).
   `oncontextlost`/`oncontextrestored` 핸들러 속성을 전역 핸들러 목록(`js/dom/EventHandlers.GLOBAL_EVENTS`)에 더했다(HTML LS 의 GlobalEventHandlers). 이 소프트웨어 캔버스는 컨텍스트를 잃지 않으므로 `isContextLost()` 는 항상 false 이고 이벤트는 발생하지 않는다. `getContextAttributes()` 는 규격의 키 네 개(`alpha`, `colorSpace`, `desynchronized`, `willReadFrequently`)를 시험으로 확인했다. `scrollPathIntoView` 는 현행 규격에 없어 정의하지 않았다.

## 변경 파일

- 신규 소스: `dom/{DomMatrixReadOnly,DomMatrix,DomMatrixParser}`, `js/dom/MatrixBindings`, `js/host/canvas/TextSpacing`, `js/host/CorsImageRequestSource`, `render/{CanvasFilters,CorsImageSource,ImageLookup}`.
- 수정 소스: `js/dom/{DomInterfaces,EventHandlers}`, `js/host/canvas/{CanvasBindings,CanvasPattern,CanvasText,Context2D,Context2DBindings,DrawState,Path2DBindings}`, `layout/FilterSpec`, `net/{RequestPurpose,OriginPolicy,HttpFetcher}`,
  `core/{PageResources,PagePresentation,PageRuntime}`, `js/host/{HostEnvironment,ImageLoads,CanvasHostImpl}`, `render/{DisplayListBuilder,ImageReplacedResolver}`.
- 시험(신규): `js/host/canvas/{DomMatrixTest(9),CanvasMatrixTest(4),CanvasTextStyleTest(4),CanvasFilterTest(5)}`, `core/CrossOriginImageIntegrationTest(5)`; 수정: `js/host/canvas/CanvasExtrasTest`(+2), `core/CanvasIntegrationTest`(기대 1곳과 스크립트의 깨진 이미지 처리).

## 검증 결과(JDK 6u45)

- 캔버스 시험 전체(`--filter org.duckdns.hjow.swingwebpanel.js.host.canvas.`): **82 통과 / 실패 0**(`js.host.canvas.` 패키지: 기존 58 + 이번 24; 마지막 코드 커밋 `735` 상태, 증분 컴파일).
- `org.duckdns.hjow.swingwebpanel.net.`: 140 통과/0 실패, `org.duckdns.hjow.swingwebpanel.js.host.`: 377 통과/0 실패(커밋 733 시점, 증분 컴파일), 레이아웃 필터 시험(`render.FilterPaintTest` 10, `BackdropFilterPaintTest` 8, `MaskPaintTest` 8) 통과(커밋 732 시점).
- 소스 규칙 점검: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/js tests/src/org/duckdns/hjow/swingwebpanel/js/host/canvas src/org/duckdns/hjow/swingwebpanel/net src/org/duckdns/hjow/swingwebpanel/render src/org/duckdns/hjow/swingwebpanel/core` → 0 위반. 기본 경로 전체(`sh tools/sourcecheck/run.sh`): 0 위반(2147 파일, 커밋 735 시점).
- 전체 회귀(`BUILD_DIR=build/full sh test.sh`, 마지막 코드 커밋 `20261117-734` 상태): **3302 통과 / 실패 0 / 건너뜀 12**(건너뜀 12 = 선택적 Opus 원본 벡터; 553 초; 커밋 `734` 상태. 그 뒤 커밋 `735`(`ImageLoads` 가 `crossorigin` 변경에 반응) 는 `ImageLoads`·`ImageLoadIntegration`·`CrossOriginImage`·캔버스 시험만 다시 돌렸다).

## 확인하지 못한 것·한계(숨기지 않고 기록)

1. **실제 Swing 창에서 확인하지 않았다**(P22-10 의 "자동 검증"과 "실제 확인" 구분). 모든 그림은 화면 밖 화소와 `getImageData` 로 확인했다. 필터·텍스트 간격·포커스 링의 실제 모양(안티앨리어싱, 두께)을 눈으로 보지 않았다.
2. **DOMMatrix**: 3D 값은 저장·곱셈·역행렬만 한다(3D 그리기 없음, 캔버스에는 2D 부분 a~f 만 쓴다). `rotateZ()` 는 모듈 규격대로 3D 함수로 세어 `is2D=false`(숫자는 같음). 구조화 복제(`structuredClone`/`postMessage`)로 `DOMMatrix`/`DOMPoint`/`DOMRect` 를 복제하는 것은 **구현하지 않았다**(`js/host/structured` 에 분류가 없음). Worker 의 전역에는 `DOMMatrix` 가 없다(현재 Worker 에 캔버스도 없음: `CanvasBindings` 는 페이지에만 설치).
   `new DOMMatrix(string)` 의 "Window 가 아닌 전역이면 `TypeError`" 단계는 해당 전역이 없어 시험하지 않았다. 문자열의 `calc()`/상대 단위는 `SyntaxError`(규격과 같음)이나, 규격이 허용하는 모든 CSS 변환 함수 문법의 변형(예: 쉼표 없는 공백 구분)은 거부한다(CSS Transforms 규격도 쉼표를 요구).
3. **텍스트 속성**: `fontStretch`·`fontVariantCaps`·`textRendering`·`lang` 은 효과 없음(상태만). `fontKerning` 효과는 글꼴에 달려 있다(위 Tahoma 만 확인). `letterSpacing` 은 아랍 문자처럼 이어 쓰는 글자에서 CSS 와 달리 글리프 사이를 그대로 벌린다(연결을 끊지 않으려는 처리 없음). 히브리어는 모양 변환 없이 일대일 글리프이며 결합 부호(니쿠드)의 위치는 글꼴에 맡긴다.
4. **필터**: `url()` 참조는 아무것도 거르지 않는다(SVG 필터 DOM 이 없음). 필터 입력은 그려진 이미지(캔버스 안으로 자른 영역)라서 캔버스 밖에 그려진 부분이 번져 들어오는 것은 반영하지 않는다. 큰 영역(수백만 화소)의 `blur` 는 문서 스레드의 한 작업을 길게 만들 수 있다(별도 제한 없음; 레이어 상한은 `CanvasLimits` 의 화소 수). `drop-shadow()` 의 기본 색은 캔버스 요소의 계산된 `color` 다.
   공유 파일 `layout/FilterSpec` 의 공개 오버로드 하나를 더했다(기존 호출은 같은 코드 경로).
5. **`crossorigin` 이미지**: JVM 프록시(`ProxySelector`)가 적용되면 소켓 클라이언트가 프록시를 쓰지 못해 JRE 연결을 쓰고 `Origin` 헤더가 사라진다(`docs/progress/net.md` 의 기존 한계). 그러면 `Origin` 이 있어야 허용하는 서버의 이미지는 `error` 가 된다. 이미지 요청에는 사전 요청(preflight)이 필요 없다(단순 GET 이고 페이지가 쓴 헤더 없음). 배경 이미지(`background-image`)와 `<link rel=stylesheet crossorigin>`, 웹 글꼴의 `crossorigin` 은 이번에 건드리지 않았다(글꼴은 이전처럼 같은 오리진만).
   요소의 `crossorigin` 속성을 로드 뒤에 바꾸면 이미지를 CORS 모드로 다시 가져온다(`ImageLoads` 가 `crossorigin` 속성 변경에 반응; 커밋 735, 시험 `testAttributeChangeFetchesAgain`).
6. 이번 작업의 전체 시험에서 간헐 실패가 있었다면 아래 "전체 회귀" 줄에 적었다. 시간 민감 시험은 컴퓨터가 바쁠 때 실패할 수 있어 단독 재실행으로 판단한다.

## 조정자 요청

- **FEATURE_MATRIX**: `WEB-05`(캔버스), `WEB-05c`(Offscreen), 새 행 `WEB-05d`(DOMMatrix)·`WEB-05e`(`ctx.filter`)·`WEB-05f`(텍스트 확장 속성)·`WEB-05g`(`crossorigin` 이미지)·`WEB-05h`(`drawFocusIfNeeded`와 컨텍스트 손실 멤버), `EXC-08`(캔버스 히트 영역과 `scrollPathIntoView`), `WEB-12`·`CSS-45`·"Canvas 2D(REC 4.12)" 행과 13절의 행 ID 목록을 이 작업자가 고쳤다(시험 이름과 건수, 한계는 위와 같음).
- **ARCHITECTURE.md**: "캔버스" 항(444행 근처)에 다음을 더해 달라: `dom.{DomMatrixReadOnly,DomMatrix,DomMatrixParser}`(행렬 계산과 CSS 변환 목록 문자열 해석), `js.dom.MatrixBindings`, `js.host.canvas.TextSpacing`, `render.{CanvasFilters,CorsImageSource,ImageLookup}`, `js.host.CorsImageRequestSource`, `net.RequestPurpose.IMAGE_CORS`; 데이터 흐름 한 줄: "crossorigin `img` → `ImageLoads`(`CorsImageRequestSource`) → `PageResources`(항목 키 = URL + CORS 모드, `Origin` 헤더, 응답 검사) → 렌더러(`ImageLookup`)와 캔버스(`HostEnvironment.getLoadedImage(url, cors)`)".
- **P21-01 소유 목록**: 새 자원은 없다(`DOMMatrix` 는 순수 값 객체).
- **API_CONTRACT/PROGRESS**: 변경 없음. 이 작업자는 `AGENTS.md`, `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`, `docs/API_CONTRACT.md`, `scripts/**` 를 고치지 않았다.

## 체크박스 후보

- **P20-05-b**: 본문(AGENTS.md)이 적은 대상 중 `OffscreenCanvas`/`ImageBitmap`(720), `createConicGradient`(723), `DOMMatrix`, `filter`, 텍스트 확장 속성(`letterSpacing wordSpacing fontKerning fontStretch fontVariantCaps textRendering`), `crossorigin` 이미지의 `Origin`(`P20-08-c` 의 소켓 연결), 히트 영역(규격에서 삭제됨을 근거와 함께 기록)을 모두 처리했다.
  남은 것은 위 "한계"(효과 없는 상태만의 속성, `DOMMatrix` 구조화 복제, 실제 창 확인)뿐이므로 **체크 후보로 제안한다**(조정자 판단). 실제 창 확인은 P22-10 에서 따로 기록한다.
