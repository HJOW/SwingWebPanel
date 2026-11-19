# 과거 HTML 표시 규칙 진행 기록

현재 작업 번호: P07-07-b-11 완료와 P07-07-c(LEG-07/12/13/14/15) 구현·검증 완료(2026-11-17, LEGACY 후속 작업자 worktree). 이전 기록(P07-07-b-3~b-10)은 아래에 그대로 보존한다.

## 인계 요약 (LEGACY 후속 작업자, 2026-11-17)

- 커밋 `20261117-330`~`339` (push 하지 않았다). 미검증 WIP(`wip/legacy-quirks-style-unverified`)는 `git cherry-pick -n` 으로 가져와 컴파일하고 새 시험으로 확인한 뒤 330 에 넣었다. 새 시험은 수정 전 소스(HEAD)로 먼저 돌려 실패를 확인했다(`build/base.sh` 로 수정 전 클래스를 앞에 둠): `QuirksStyleScriptTest` 4건 전부, `LegacyQuirksTest` 의 새 2건(clip, @supports)과 채우기 시험, `BrokenImageFallbackTest` 4건 전부, `LegacyScriptCompatTest` 3건(for/event, 타입 목록, 삽입 스크립트), `LegacyXhtmlPageTest` 2건(xml-stylesheet, document.write), `LegacyFormCompatTest` 3건(document/window 이름 접근). 이미지 맵과 meta refresh 는 새 기능이다. `LegacyLinkMetaTest`(옛 속성 반영은 시험을 쓰기 전에 구현을 넣었다), `LegacyLinkIntegrationTest`, `ImageButtonIntegrationTest`(이미 맞는 동작을 고정)는 수정 전 실패를 확인하지 않았다.
- 새 시험(모두 JDK 6u45, 직접 실행): `QuirksStyleScriptTest` 4, `LegacyQuirksTest` +3(clip, @supports, 채우기; 모두 16), `BrokenImageFallbackTest` 4, `ImageMapTest` 8, `ImageMapIntegrationTest` 5, `LegacyScriptCompatTest` 6, `LegacyXhtmlPageTest` 3, `XmlStylesheetInstructionTest` 2, `MetaRefreshTest` 2, `MetaRefreshIntegrationTest` 9, `LegacyFormCompatTest` 7, `LegacyLinkMetaTest` 2, `LegacyLinkIntegrationTest` 5, `ImageButtonIntegrationTest` 2. 합계 새 시험 62건.
- 시험 실행: 구현 중에는 `build/inc.sh`(증분 컴파일 + 시험 실행기; `build/` 는 git 무시)로 필요한 시험만, 단계가 끝날 때는 HEAD 스냅샷 전체 회귀(`scripts/snaptest.sh`). b-11 끝: **2967 통과 / 0 실패 / 13 건너뜀**(12 = 선택적 Opus 원본 벡터, 1 = 스냅샷에서 만들지 않은 제품 JAR 검사). 최종 전체 회귀 결과는 아래 "최종 회귀" 절.
- 미해결과 한계는 각 절의 "한계"에 적었다. Chromium 소스는 이번에도 조회하지 않았다(브라우저와의 차이는 모두 "미확인").

## 조정자 요청 (LEGACY 후속 작업자)

### AGENTS.md

- `P07-07-b-11` 체크 가능: 입력 목록(Quirks 3.5, 3.8, 3.9, 3.13 은 이전 작업자의 `LegacyQuirksBoxesTest` 4건, HTML5 2014 §10.4.2 이미지 대체 상자 `BrokenImageFallbackTest` 4건, `clip: rect()` quirk `LegacyQuirksTest.testClipRectQuirk` + `QuirksStyleScriptTest.testClipRect`, `element.style`/`rule.style`/`insertRule`/`@supports` quirk `QuirksStyleScriptTest` 4건 + `LegacyQuirksTest.testSupportsRuleInQuirksSheet`, html/body 채우기의 세로쓰기와 자손 재배치 `LegacyQuirksTest.testFillReachesDependantsAndVerticalModes`) 가 모두 시험으로 확인되었다. 한계는 아래 b-11 절.
- `P07-07-b` 상위: LEG-01/03~06/08/11 의 입력이 모두 처리되었다. 남은 한계(marquee 애니메이션 미구현, 호환 모드 값 quirk 의 제외 범위 `calc()`/단축 속성/`var()`/교차 출처 시트 예외, html 과 body 의 쓰기 방향이 다른 경우의 채우기)를 기능 표의 알려진 한계로 받아들이면 체크할 수 있다.
- `P07-07-c`: 이 작업의 범위(LEG-07, 12, 13, 14, 15)는 모두 구현·검증했다. LEG-02(프레임)는 P20-02(체크됨), LEG-09(플러그인 대체)는 P20-11(미완료)이 근거이므로 P07-07-c 상위 체크는 LEG-09 의 P20-11 근거가 생길 때 판단한다.

### FEATURE_MATRIX 문구 제안

- LEG-07: "부분 구현 → 검증 완료(클라이언트 쪽: `img[usemap]` 의 `map` 찾기(HTML5 2014 §2.4.9), `area` 의 rect/circle/poly/default 와 coords 해석, 트리 순서 첫 모양이 맨 위, href 없는 area 가 점을 잡음, 표시된 이미지의 CSS 픽셀 좌표(확대·변환 무관), 링크 이동·하이퍼링크 진입/이탈 통지, `HTMLMapElement.areas/images`; 서버 쪽: `ismap` 클릭이 `?x,y` 를 붙임). 구현 `render.ImageMaps`·`render.HitTester`·`core.PageInput`·`dom.ElementCollections`·`js.dom.MapBindings`. 시험 `ImageMapTest` 8, `ImageMapIntegrationTest` 5. 제약: `object` 가 이미지를 표현하는 경우의 usemap 과 이미지를 보여 주지 않는 사용자 에이전트용 area 링크 목록(§4.7.13.2 의 첫 방식)은 미구현, 키보드로 area 로 이동·활성화하는 경로는 코드만 고쳤고 시험은 없음(미확인), area 의 `cursor` 와 포커스 링 모양은 없음."
- LEG-12: "검증 완료(파서: `content.xhtml.*`(P17-05) 기존 시험; 페이지: XHTML 1.0 Strict/Transitional prolog·DOCTYPE, 항상 표준 모드, `xml:lang` 과 `:lang()`, DTD 없는 엔티티, 빈 요소 태그, CDATA 스크립트·스타일, `<?xml-stylesheet?>` 처리 명령(새), XML 문서의 `document.write/open/close` 가 `InvalidStateError`(새), body onload; `text/html` 로 제공한 XHTML 문법은 HTML 파서). 시험 `LegacyXhtmlPageTest` 3, `XmlStylesheetInstructionTest` 2. 제약: xml-stylesheet 의 `alternate=yes` 는 무시하고 `charset`·선호 스타일 시트 집합은 쓰지 않으며 PI 를 동적으로 삽입한 변경은 반영하지 않는다."
- LEG-13: "검증 완료(`<!-- -->` 로 감싼 스크립트와 스타일, CDATA 표시, `type`/`language` 판정(HTML5 2014 §4.11.1.1 의 MIME 목록 전체, 빈 type 은 JavaScript, type 이 있으면 language 무시), `for`/`event`(window + onload 만 실행), 삽입한 스크립트도 같은 규칙, `javascript:` 등 차단). 시험 `LegacyScriptCompatTest` 6. 제약: MIME 매개변수(`charset` 등)는 현행 표준과 브라우저처럼 무시한다(2014 REC 는 매개변수까지 비교하라고 함); `text/javascript;e4x=1` 미지원."
- LEG-14: "검증 완료(`input type` 의 대소문자 무시·옛 값·알 수 없는 값, `isindex` 폼, 폼 컨트롤을 `name` 과 `id` 로 찾기(`form.a`, `form.elements`), 이름이 같은 라디오 목록, 이미지 버튼 클릭 좌표 제출(`name.x`, `name.y`), `document.폼이름`·`document.imgname`·`window.id` 의 이름 있는 프로퍼티(HTML5 2014 §5.1.4, §5.3; 문서가 바뀌면 따라가고 여러 개이면 컬렉션)). 구현 `js.dom.DocumentNamedObject`, `js.host.WindowNames`, `js.JsRealm.NamedGlobals`, `dom.ElementCollections`. 시험 `LegacyFormCompatTest` 7, `ImageButtonIntegrationTest` 2. 제약: 이름은 내장 멤버와 전역 변수를 가리지 않는다(규격의 `[OverrideBuiltins]` 와 다르게 DOM clobbering 을 막음); window 이름 접근에 프레임 이름은 포함하지 않는다; **`input type=image` 의 그림을 읽거나 그리지 않는다(대체 텍스트 단추로 보임)**; `document.all`/`document.layers` 같은 비표준 IE/Netscape 컬렉션은 없음."
- LEG-15: "검증 완료(`<meta http-equiv=refresh content='N;url=...'>` 의 예약 이동(새; 규격 알고리즘, 문서가 완전히 로드된 뒤 N 초, 이력 항목 교체, 주소 없으면 다시 읽기, 첫 refresh 만, JavaScript 설정과 무관, 취소와 폐기, 프레임은 그 프레임만, 차단 스킴 무시, `allow-scripts` 없는 샌드박스 프레임은 안 함), `link`/`a`/`meta`/`script` 의 옛 속성 반영(`rev`, `charset`, `target`, `coords`, `name`, `shape`, `scheme`, `event`, `htmlFor`), `rel` 링크 타입(`relList`), `rev` 만 있는 link 는 스타일 시트가 아님, `<a name>` 앵커가 프래그먼트 대상(id 가 먼저, 이름은 대소문자 구분, 빈 앵커 포함), `<base target>` 기본 대상(링크 자신의 target 이 우선), `_blank`/이름 있는 대상은 P20-02 의 이동 정책). 구현 `core.MetaRefresh`, `core.HtmlPageDocument`, `core.ParsingPageLoader`, `js.dom.DomInterfaces`. 시험 `MetaRefreshTest` 2, `MetaRefreshIntegrationTest` 9, `LegacyLinkMetaTest` 2, `LegacyLinkIntegrationTest` 5. 제약: 사용자가 이동을 취소하는 화면은 없음; HTTP `Refresh:` 응답 헤더와 `http-equiv=set-cookie`/`default-style` 는 미구현."
- LEG-11: 위 "부분 구현" 문구의 마지막 줄을 "미구현: html 과 body 의 쓰기 방향이 달라 채우기를 적용하지 않는 경우, 값 quirk 는 `calc()`·단축 속성·`var()` 값과 교차 출처 시트 예외를 다루지 않음"으로 바꾸고, §10.4.2 호환 모드 이미지 대체 상자·`clip: rect()` quirk·`element.style` 스크립트 quirk·html/body 채우기의 세로쓰기와 자손 재배치를 구현 완료로 옮긴다. 구현 클래스에 `render.ImageReplacedResolver`·`layout.ReplacedFallback`(깨진 이미지 텍스트), `layout.BlockLayouter.quirkFillMin`, `js.dom.StyleObject`·`css.cssom.CssStyleDeclaration.setQuirks` 추가.
- ARCHITECTURE: 새 클래스 `layout/ReplacedFallback`, `render/ImageMaps`, `dom`의 `ElementCollections.documentNames/windowNames/mapAreas/mapImages/findMap`, `js/dom/{MapBindings,DocumentNamedObject}`, `js/host/WindowNames`, `js/JsRealm.NamedGlobals`(전역 객체가 `GlobalObject` 가 됨), `css/cssom/XmlStylesheetInstruction`, `core/MetaRefresh`.

## P07-07-b-11 구현과 검증 (2026-11-17)

규격(판본·조회일 고정): HTML5 REC 2014-10-28 [§10.4.2 Images](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#images)(조회 `tools/webfetch`, JRE Zulu 25, `Last-Modified: Fri, 24 Oct 2014 16:15:24 GMT`, 2026-11-17), WHATWG [Quirks Mode Standard](https://quirks.spec.whatwg.org/) 3.6, 3.7(`Last-Modified: Mon, 19 Oct 2026 11:56:37 GMT`), [CSS Values 4 부록 C](https://drafts.csswg.org/css-values-4/#quirky-length)와 [CSS Color 4 부록 B](https://drafts.csswg.org/css-color-4/#quirky-color)(`Last-Modified: Mon, 16 Nov 2026 22:56:27 GMT`). 앞의 4개 규칙(3.5, 3.8, 3.9, 3.13)은 이전 작업자(커밋 320)가 `LegacyQuirksBoxesTest` 4건(`testPercentageHeight`, `testCellImagesHaveNoWrapOpportunity`, `testNowrapCellMinimumWidth`, `testTableCellHeightBoxSizing`)으로 구현했고 이 작업에서 전체 회귀에 포함해 확인했다.

| 규칙 | 구현(커밋) | 시험 | 한계 |
| --- | --- | --- | --- |
| 값 quirk(단위 없는 길이, # 없는 색)를 `element.style`, `rule.style`, `insertRule`, `cssText`, `setProperty` 에 적용 | 330: `CssStyleDeclaration.setQuirks`(블록이 소유자의 모드를 기억), `StyleSheetBuilder`, `js.dom.StyleObject`(호환 모드 문서의 `style`) | `QuirksStyleScriptTest.testElementStyleLengths/testElementStyleColors/testRuleStyleInsertRuleAndSupports`(수정 전 4건 모두 실패) | `calc()`·`rgb()` 안, `border`·`background`·`font` 같은 단축 속성, `var()` 치환 값, 교차 출처 시트의 예외(웹 표준은 시트의 출처를 구분)는 구현하지 않았다 |
| `@supports` 규칙 안에서는 호환 모드 시트의 quirky 값이 올바르고 `CSS.supports()` 에서는 아님 | 330: `SupportsCondition.evaluate(.., quirks)` | `LegacyQuirksTest.testSupportsRuleInQuirksSheet`, `QuirksStyleScriptTest.testRuleStyleInsertRuleAndSupports`(`CSS.supports('width','10')` 는 호환 모드에서도 false) | — |
| `clip: rect()` 의 인수가 quirky length(값 quirk 의 유일한 함수 예외) | 330: `QuirkyValues.rectOf` | `LegacyQuirksTest.testClipRectQuirk`, `QuirksStyleScriptTest.testClipRect` | 숫자만 px 로 바꾸며 `rect()` 의 인수가 4개가 아니면 선언을 무시한다 |
| html 요소가 뷰포트를, body 가 html 을 채움: **배치 뒤 후처리를 배치 중 최소 블록 크기로 바꿈** | 331: `LayoutBox.quirkFill`, `LayoutEngine.markQuirkFill`, `BlockLayouter.minHeight/quirkFillMin` | `LegacyQuirksTest.testFillReachesDependantsAndVerticalModes`(수정 전 첫 단언에서 실패: 위치 지정된 자손이 29px 에 있음), 기존 `testHtmlAndBodyFillTheViewport` | 세로쓰기(html 이 `vertical-rl`)에서 블록 흐름 방향(너비)을 채운다. html 과 body 의 프레임이 다르면(`writing-mode` 가 서로 다름) body 채우기를 적용하지 않는다(표준이 열어 둔 경우). html 의 블록 크기가 auto 가 아니면 body 는 그 지정 크기를 채운다. 직교 흐름 루트인 body 의 채우기는 검증하지 않았다 |
| 깨졌고 크기가 없는 `img` 를 대체 텍스트를 내용으로 하는 비치환 인라인 요소로 배치(HTML5 2014 §10.4.2); 크기(width, height 둘 다)가 있으면 치환 상자를 유지하고 그리는 쪽이 같은 텍스트를 채움 | 332: `layout.ReplacedFallback`(새 인터페이스), `render.ImageReplacedResolver.getFallbackText`, `BoxBuilder.brokenImageText` | `BrokenImageFallbackTest` 4건(호환·표준 모드 모두; alt 가 있으면 `alt [지원하지 않는 URL (Unsupported URL): 주소]`, alt 가 없거나 빈 값이면 안내만, 소스가 없으면 alt 만, 읽는 중·읽음·요청 전은 치환 상자, 링크 안의 텍스트가 링크) | **규격과 다른 점**: 규격은 크기가 있어도 표준 모드에서 alt 가 있으면(그리고 곧 나타날 것이 아니면) 비치환 인라인 텍스트로 보이라고 하지만, 이 프로젝트는 이미지 자리에 미지원 URL 을 보여야 하므로(P18-06) 크기가 있으면 모드와 무관하게 상자를 유지한다. `width` 와 `height` 중 하나만 있으면 인라인 텍스트로 보고 그 크기는 무시한다. `display` 가 인라인이 아니거나 float/절대 배치이면 이전처럼 크기 0 치환 상자이다 |

## P07-07-c 구현과 검증 (2026-11-17)

### LEG-07 이미지 맵(커밋 333)

규격: HTML5 REC 2014-10-28 [§4.7.11 map, §4.7.12 area, §4.7.13 Image maps](https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html#image-maps), [§2.4.4.5 정수 목록, §2.4.9 해시 이름 참조](https://www.w3.org/TR/2014/REC-html5-20141028/infrastructure.html), [§4.5.1 a 의 활성화 동작(서버 쪽 이미지 맵)](https://www.w3.org/TR/2014/REC-html5-20141028/text-level-semantics.html)(조회 2026-11-17, 같은 도구와 JRE).

- 규격의 오탈자 하나를 현행 의미로 읽었다: 직사각형의 첫 숫자가 셋째보다 "작으면" 서로 바꾼다고 되어 있으나 정상 입력을 뒤집는 오류이므로 "크면" 바꾼다(결과적으로 두 모서리의 순서와 무관).
- 구현: `render.ImageMaps`(map 찾기, area 목록, 모양 판정, 정수 목록 해석, 점 좌표 변환, 서버 쪽 접미사), `render.HitTester`(이미지 위의 점이 area 에 속하면 요소와 링크가 그 area), `core.PageInput`(클릭 후 접미사 붙임, 키보드 Enter 가 area 링크도 활성화), `dom.ElementCollections`(`findMap`, `mapAreas`, `mapImages`), `js.dom.MapBindings`(`HTMLMapElement.areas/images`).
- 결과: 트리 순서의 첫 area 가 맨 위, `href` 없는 area(죽은 영역)는 점을 잡아 이미지나 이미지를 감싼 링크로 가지 않고, 모든 모양 밖의 점은 이미지와 감싼 링크로 간다. 좌표는 표시된 이미지의 content box 기준 CSS 픽셀이며 변환(`transform`)·fixed·스크롤의 영향을 받지 않는다. 클릭은 area 에서 `click` 이 발생(버블)하고 그 링크로 이동하며 `target`/`download` 도 area 의 것을 쓴다. 호버는 `ENTERED/EXITED` 하이퍼링크 이벤트. `ismap`: `a[href]` 안의 `img[ismap]` 클릭은 `href?x,y`(이미지 border 왼쪽·위쪽 가장자리 기준 정수, 음수는 0, 프래그먼트 앞)로 이동.
- 시험: `ImageMapTest` 8(map 찾기, 정수 목록, 모양 24개 단언, 최상위 모양, 이미지에 남는 점, 변환·border, 서버 쪽 접미사, area 목록) + `ImageMapIntegrationTest` 5(실제 컴포넌트·HTTP: 클릭·죽은 영역, 감싼 링크, `ismap` 요청 쿼리, 호버 이벤트, 스크립트가 읽는 `areas`/`images`).
- 한계: `object` 요소가 이미지를 표현하는 경우의 `usemap` 은 처리하지 않는다(`img` 만). 이미지를 보여 주지 않는 사용자 에이전트용 area 링크 목록(§4.7.13.2 의 첫 방식)은 없다. 키보드 Enter 로 area 를 활성화하는 경로는 코드만 고쳤고 시험이 없다(미확인). area 의 `cursor` 와 포커스 링은 그려지지 않는다.

### LEG-13 스크립트와 스타일 호환(커밋 334)

규격: HTML5 REC 2014-10-28 [§4.11.1.1 Scripting languages와 script 준비 알고리즘](https://www.w3.org/TR/2014/REC-html5-20141028/scripting-1.html)(조회 2026-11-17).

- 고친 것: `PageScripts.isJavaScriptType`(스크립트 블록 타입: type 이 비면 JavaScript, 비어 있지 않은 type 이 있으면 language 무시, 없으면 `text/` + language, 목록은 규격의 16개 MIME 전체: `application/x-ecmascript` 와 `text/javascript1.0`~`1.5` 가 빠져 있었다), `for`/`event` 속성(둘 다 있으면 `window` + `onload`(`onload()`)일 때만 실행; 파서가 넣은 스크립트와 동적 스크립트 모두; 거부하면 "already started" 표시).
- 이미 맞았던 것을 시험으로 고정: `<!-- ... //-->` 로 감싼 스크립트(렉서의 HTML 유사 주석, ECMA-262 부록 B), `//<![CDATA[`, `n --> 3`(연산자), 스타일의 `<!-- -->`(CSS 의 CDO/CDC), 스타일 `type`/`media`, `javascript:`·`vbscript:`·`data:`·`file:`·`mailto:`·`about:text`·`blob:` 이동 차단(`SchemePolicy`).
- 시험: `LegacyScriptCompatTest` 6(실행해야 하는 type/language 조합과 실행하지 않는 조합, for/event 13개 조합, 삽입한 스크립트 7개, 적용하는 스타일 7개와 적용하지 않는 4개).
- 한계: 2014 REC 는 MIME 매개변수까지 비교하라고 하지만(`type="text/javascript; charset=utf-8"` 은 JavaScript 가 아님) 현행 표준과 브라우저처럼 매개변수를 무시한다(시험이 이를 고정). `text/javascript;e4x=1` 은 지원하지 않는다.

### LEG-12 XHTML 호환(커밋 335, 336)

- 파서(`content.xhtml.*`)와 페이지 흐름은 P17-05 에서 이미 구현되었다(`XhtmlParserTest`, `ContentIntegrationTest`). 이 작업은 XHTML 1.0 문서를 실제 컴포넌트로 점검해 두 가지가 비어 있음을 찾아 고쳤다: (1) `<?xml-stylesheet type="text/css" href="..."?>` 처리 명령이 스타일 시트를 연결하지 않았다 → `css.cssom.XmlStylesheetInstruction`(의사 속성 해석: 따옴표 둘, 엔티티, 문자 참조, 첫 이름이 이김)과 `DocumentStyleSheets.fromInstruction`(문서 직계 자식의 PI 만, `type=text/css`, `alternate=yes` 는 제외, `media`·`title`), (2) XML 문서에서 `document.write/writeln/open/close` 가 던지지 않았다 → `WindowGlobals.requireHtmlDocument`(`InvalidStateError`).
- 시험: `LegacyXhtmlPageTest` 3(XHTML 1.0 Transitional prolog+DOCTYPE+`xml:lang`+`&nbsp;`+CDATA+body onload+`compatMode`/`namespaceURI`/`contentType`/`tagName` 대소문자, 외부 CSS/JS 와 빈 요소 태그 `<script src/>`, `text/html` 로 제공한 XHTML 문법은 HTML 파서의 결과) + `XmlStylesheetInstructionTest` 2.
- 한계: xml-stylesheet 의 `charset`, 선호 스타일 시트 집합(`title` 과 `alternate="no"` 의 조합), PI 를 스크립트가 동적으로 넣거나 지우는 변경은 반영하지 않는다(DocumentStyleSheets 가 다시 모일 때만).

### LEG-14 폼 호환(커밋 338, 339)

규격: HTML5 REC 2014-10-28 [§5.1.4 Named access on the Window object, §5.3 Named access on the Document object(dom.html)](https://www.w3.org/TR/2014/REC-html5-20141028/dom.html), §4.10.7.1.19 Image Button state.

- 이미 맞았던 것을 시험으로 고정: `input type` 상태(대소문자 무시, 공백은 제거하지 않음, 알 수 없는 값·`datetime` 은 text), `form.a`·`form.elements.a` 가 name 과 id 로 컨트롤을 찾음(둘을 가진 컨트롤 포함), 이름이 같은 라디오는 `RadioNodeList`, `isindex` 폼(b-9), 이미지 버튼의 좌표 제출(`FormSubmissionTest`; 이 작업에서 실제 클릭 통합 시험 추가).
- 새로 구현: `document.myform`/`document.logo` 처럼 **이름 있는 프로퍼티**. Document: `applet/embed/form/img/object` 의 `name`, `applet/object` 의 `id`, 이름도 있는 `img` 의 `id`(object 안의 embed/object 는 제외) — `js.dom.DocumentNamedObject`(HTMLDocument 래퍼). Window: 모든 HTML 요소의 `id`, `a/applet/area/embed/form/frameset/img/object` 의 `name` — `js.host.WindowNames` + `JsRealm.NamedGlobals`(전역 객체가 `GlobalObject` 가 되어 `hasProperty`/`get` 이 비어 있으면 호스트에 묻는다). 요소가 하나이면 요소, 여럿이면 라이브 컬렉션. 문서의 변경 번호마다 이름 색인을 한 번 만든다.
- 규격과 다른 점: 규격은 Document 의 이름이 내장 멤버를 가린다(`[OverrideBuiltins]`)고 하지만 가리지 않는다(`<form name=cookie>` 가 `document.cookie` 를 바꾸지 못함: DOM clobbering 방지). Window 의 이름은 전역 변수·함수·내장보다 뒤이고, 스크립트가 `var d` 를 선언하면 자신의 프로퍼티를 얻는다. window 이름 접근에 프레임의 이름은 포함하지 않는다.
- 시험: `LegacyFormCompatTest` 7(Document 폼 컬렉션, Document 이름 프로퍼티, 라이브와 가리지 않음, 컨트롤 name/id, window 이름 접근, input type, isindex) + `ImageButtonIntegrationTest` 2. JS 전체(`org.duckdns.hjow.swingwebpanel.js`) 943건 통과(전역 객체 변경 후).
- 한계: **`input type=image` 의 `src` 그림을 읽지도 그리지도 않는다**(`ImageLoads` 가 `img` 만 추적하고 `ControlPainter` 가 이미지 버튼을 대체 텍스트 단추로 그린다). 좌표 제출은 클릭의 `offsetX/offsetY` 를 쓴다. `document.all`, `document.layers`, `window.event` 같은 IE/Netscape 비표준은 없다.

### LEG-15 링크/메타 호환(커밋 337, 339)

규격: HTML5 REC 2014-10-28 [§4.2.5.3 Pragma directives(Refresh state)](https://www.w3.org/TR/2014/REC-html5-20141028/document-metadata.html), §4.2.3 base, §4.2.4 link, §4.5.1 a, §4.11.1 script(조회 2026-11-17).

- `meta http-equiv=refresh`: 파서가 `metaRefresh(content)` 를 이미 알렸으나 아무 데서도 쓰지 않았다. 새로 `core.MetaRefresh`(규격 알고리즘을 단계대로 해석: 초, 소수 무시, `;`/`,`, `url=`, 따옴표, 공백·탭·줄바꿈 제거), `ParsingPageLoader`(첫 번째 올바른 refresh 만 기억), `HtmlPageDocument`(페이지가 확정되면 `javax.swing.Timer` 로 문서 로드 완료를 기다린 뒤 N 초 후 `NavigationPolicy` 로 허용된 주소로 **이력 항목을 바꾸며** 이동. 주소가 없으면 현재 주소를 다시 읽음; 프레임이면 그 프레임만; `allow-scripts` 없는 샌드박스 프레임은 안 함; 문서 폐기·페이지 교체 시 타이머 중지; JavaScript 설정과 무관).
- 옛 속성 반영: `link.rev/charset/target`, `a.charset/coords/name/rev/shape`, `meta.scheme`, `script.event/htmlFor`(규격의 "폐기된 기능"은 반영만 하고 다른 효과가 없다). `a.text`, `relList` 는 이미 있었다(시험으로 확인).
- 이미 맞았던 것을 시험으로 고정: `<a name>` 이 프래그먼트 대상(id 가 먼저, 이름은 대소문자 구분, 빈 앵커 포함), `<base target>` 이 링크의 기본 대상이고 링크 자신의 target 이 우선(`_blank`/모르는 이름은 P20-02 의 정책: 현재 문맥 재사용), `rev=stylesheet` 는 스타일 시트가 아님.
- 시험: `MetaRefreshTest` 2(26개 값 + 9개 무효), `MetaRefreshIntegrationTest` 9(이력 교체·첫 refresh·JS 끔·주소 없는 반복 로드와 중단·취소/폐기·초 기다림·차단 스킴과 base 상대 주소·`setContent`·프레임/샌드박스), `LegacyLinkMetaTest` 2, `LegacyLinkIntegrationTest` 5.
- 한계: 이동 전에 사용자가 취소하는 화면은 없다. HTTP `Refresh:` 응답 헤더, `http-equiv=set-cookie`, `default-style` 은 구현하지 않았다. `time` 은 int 최댓값에서 포화되며 아주 큰 값은 사실상 이동하지 않는다.


## 최종 회귀 (2026-11-17, HEAD `41dc361` 스냅샷)

- `sh scripts/snaptest.sh c1full "docs" "<모든 제품 패키지>" "<모든 시험 패키지>"` (JDK 6u45, 다른 작업자 둘과 CPU 를 나눔): **3016 통과 / 2 실패 / 13 건너뜀**(1,056,979 ms; 건너뜀 13 = 선택적 Opus 원본 벡터 12 + 스냅샷에서 만들지 않은 제품 JAR 검사 1).
- 실패 2건은 조정자 문서의 "시간 민감 시험" 목록에 있는 것이다: `FrameLifecycleTest.testCleanAndDisposeStopTheScriptsOfAllFrames`(clean 뒤 프레임 tick 5회 더 셈), `WorkerLifecycleTest.testTerminateStopsAnEndlessLoopAndSilencesTheWorker`(60 초 시간 초과). 같은 클래스를 단독 재실행(`FrameLifecycleTest` 6건, `WorkerLifecycleTest` 12건)하면 **18 통과 / 0 실패**이다. 합치면 3018 통과 = b-11 끝의 2967 + 새 시험 51건(`ImageMapTest` 8, `ImageMapIntegrationTest` 5, `LegacyScriptCompatTest` 6, `LegacyXhtmlPageTest` 3, `XmlStylesheetInstructionTest` 2, `MetaRefreshTest` 2, `MetaRefreshIntegrationTest` 9, `LegacyFormCompatTest` 7, `LegacyLinkMetaTest` 2, `LegacyLinkIntegrationTest` 5, `ImageButtonIntegrationTest` 2)과 일치한다. 이 두 실패가 이번 변경 때문이 아니라는 증거는 단독 재실행 통과와 같은 파일을 이전 실행(2967 / 0 실패)에서 통과했다는 것이며, 부하가 큰 실행에서 간헐 실패한다는 기존 기록(PROGRESS)과 같다.
- 중간 확인: `org.duckdns.hjow.swingwebpanel.layout` 339, `org.duckdns.hjow.swingwebpanel.css` 288, `org.duckdns.hjow.swingwebpanel.render` 187(채우기 quirk 변경 뒤), `org.duckdns.hjow.swingwebpanel.js` 943(전역 객체와 Document 래퍼 변경 뒤), 모두 0 실패.
- 소스 규칙: `tools/sourcecheck` 변경한 모든 Java 42개 파일 0 위반, `git diff --check` 깨끗. 제품 클래스 이름에 `Test` 를 넣지 않았다.
- 제품 JAR: `BUILD_DIR=build/jarchk sh build.sh`(javac 1.6.0_45)로 만든 `swingwebpanel.jar` 에 대해 `JarContentTest` 1건 통과(제품 파일만 들어 있음). 스냅샷 전체 회귀가 건너뛴 그 1건이다.
- 확인하지 못한 것: 실제 Swing 창에서의 사람 눈 확인(이미지 맵 포인터 모양·호버, meta refresh 의 체감 시간), 키보드로 area 를 활성화하는 경로, Chromium 과의 동작 비교.

---

## 이전 인계 요약 (LEGACY 작업자, 2026-11-17: b-7~b-10)

- 이 작업자는 커밋 301~304 로 P07-07-b-7, b-8, b-9, b-10 을 구현·검증했다(브랜치 `worktree-agent-af20b1997a4228269`, master 의 `9dd77dd` 에서 갈라짐). push 하지 않았다.
- 새 시험(모두 JDK 6u45 에서 직접 실행): `LegacyTablesTest` 21, `LegacyEmbeddedTest` 17, `LegacyElementsTest` 12, `LegacyQuirksTest` 13, `SwingRenderingTest.testLegacyTableAttributesPaint` 1. 구현 변경마다 새 시험을 수정 전 제품 클래스로 먼저 돌려 실패를 확인했다(각 절의 시험 표).
- 전체 회귀: b-7 은 2607 통과 / 3 실패(원인과 해소는 b-7 절), b-8·b-9 는 2639 통과 / 0 실패 / 12 건너뜀, b-10 은 JDK 6 의 `fontmanager.dll` 충돌로 한 번 중단되어 세 번에 나누어 합쳐 2652 통과 / 0 실패(b-10 절의 시험 표).
- 미해결: (1) `P07-07-b` 상위 항목 판단(위 b-10 요청), (2) 호환 모드 규칙 중 Quirks Mode Standard 3.5, 3.8, 3.9, 3.13 과 HTML5 2014 §10.4.2 의 이미지 대체 상자, (3) marquee 애니메이션, (4) 규격과 다르게 남긴 점(b-7 의 숨김 표 부품 `display:none`, b-8 의 `hr overflow:hidden`·fieldset padding 의 Chromium 비교 미확인, b-9 의 isindex 확장과 현행 WHATWG 의 차이), (5) Chromium 소스는 한 번도 조회하지 않았다(모든 "브라우저와 다를 수 있음"은 미확인).
- 다음 작업 번호: 조정자가 b-7~b-10 체크와 상위 b 판단을 마친 뒤 `P07-07-c`(프레임은 FRAMES, 플러그인 대체는 P20-11 이므로 제외하고 LEG-07 이미지 맵, LEG-12 XHTML, LEG-13 스크립트/스타일 호환, LEG-14 폼 호환, LEG-15 링크/메타 호환). 호환 모드 미구현 규칙을 `P07-07-b-11` 로 쪼갤지는 조정자 결정이다.

## 조정자 요청 (LEGACY 작업자)

### P07-07-b-10 (문서 모드별 화면 차이, LEG-11)

- AGENTS.md: `P07-07-b-10` 체크 가능(근거: 아래 "P07-07-b-10 구현과 검증"의 `LegacyQuirksTest` 13건, 전체 회귀는 아래 시험 표). **상위 `P07-07-b` 는 조정자가 판단한다**: b-1~b-10 의 입력은 모두 처리했으나 (1) LEG-01 의 marquee 애니메이션은 미구현(정적 상자만), (2) 호환 모드 규칙 중 Quirks 3.5(백분율 높이), 3.8(셀 안 이미지 줄바꿈), 3.9(nowrap 셀 최소 너비), 3.13(셀 높이 box-sizing)과 HTML5 2014 §10.4.2 의 이미지 대체 상자는 미구현으로 기록했다. 이를 FEATURE_MATRIX 의 알려진 한계로 받아들이면 상위 항목을 체크할 수 있고, 아니면 별도 하위 항목(`P07-07-b-11` 등)으로 쪼갠다. 프레임/플러그인 대체 등 `P07-07-c` 는 이 작업 범위가 아니다.
- FEATURE_MATRIX LEG-11 문구 제안: "부분 구현(검증 완료: DOCTYPE 판정, form margin, 표 글꼴 초기화, 여백 병합 quirk, 단위 없는 길이, # 없는 16진수 색, html/body 의 뷰포트 채우기(가로쓰기), 줄 높이 quirk(이미지 아래 간격), collapsing table, 표의 body 색 상속, `:hover/:active` quirk. 미구현: Quirks Mode Standard 3.5, 3.8, 3.9, 3.13, HTML5 2014 §10.4.2 호환 모드 이미지 대체 상자)". 기준 판본: HTML5 REC 2014-10-28 §10, Quirks Mode Standard(Living, 2026-10-19), CSS Values 4 부록 C·CSS Color 4 부록 B(2026-11-16 초안). 구현 클래스에 `css/props/QuirkyValues`, `css/cascade/QuirkHints` 추가, `layout/LayoutEngine.fillViewport`.
- ARCHITECTURE: `css/props/QuirkyValues`, `css/cascade/{LegacyAttributes,TableHints,QuirkHints}` 가 새로 생겼고 `ParseEnv` 에 호환 모드 플래그가 있다.

### P07-07-b-9 (plaintext/listing/xmp/isindex, marquee, basefont, LEG-08 및 LEG-01 의 남은 부분)

- AGENTS.md: `P07-07-b-9` 체크 가능(근거: 아래 "P07-07-b-9 구현과 검증"의 `LegacyElementsTest` 12건, `special.dat` 트리 구성 사례, 전체 회귀 2639 통과 / 0 실패 / 12 건너뜀).
- FEATURE_MATRIX 문구 제안: LEG-08(plaintext/listing/xmp/isindex): "검증 완료(HTML5 2014 §8.2.5.4.7 의 파서 규칙 — isindex 는 form/hr/label/input 으로 확장 —, §10.3.3 의 UA 스타일, `pre[wrap]`). 제약: 2016년 이후 WHATWG 는 isindex 확장을 지웠으므로 현행 브라우저와 다르다(기준 판본은 2014); 안내 문구는 영어 고정". LEG-01 의 marquee: "부분 구현(정적 상자: inline-block, overflow 항상 hidden, bgcolor/width/height/hspace/vspace, 세로 방향 자연 높이 200px; 애니메이션/behavior/scrollamount/scrolldelay/loop/이벤트 미구현)", basefont: "검증 완료(숨김, 영향 없음; HTML 4/IE 의 base font 효과는 의도적으로 구현하지 않음)". 숨김 요소 목록에 `source`, `track` 과 `embed[hidden]` 규칙이 추가되었다.

### P07-07-b-8 (hr/치환 요소/br/fieldset 속성, LEG-05)

- AGENTS.md: `P07-07-b-8` 체크 가능(근거: 아래 "P07-07-b-8 구현과 검증"의 `LegacyEmbeddedTest` 17건과 전체 회귀 2639 통과 / 0 실패 / 12 건너뜀(b-9 와 함께 실행)).
- FEATURE_MATRIX LEG-05 문구 제안: "검증 완료(HTML5 2014 §10.3.12 hr, §10.3.13 fieldset/legend, §10.4.3 삽입 요소·이미지의 align/hspace/vspace/border/width/height/frameborder, §10.3.4 br clear, 문단·제목 align). 제약: `vertical-align:-webkit-baseline-middle` 은 엔진 확장 키워드; `absbottom` 등 규격 밖 브라우저 확장은 없음; fieldset 의 padding 은 규격 값(현행 브라우저와 다를 수 있음, 미확인); `hr` 의 `overflow:hidden` 은 규격 밖 브라우저 규칙". 구현 클래스에 `css/cascade/LegacyAttributes` 추가, 검증 사례에 `LegacyEmbeddedTest` 17건.

### P07-07-b-7 (표 표현 속성, LEG-06)

- AGENTS.md: `P07-07-b-7` 체크 가능(근거: 아래 "P07-07-b-7 구현과 검증"의 시험과 전체 회귀).
- FEATURE_MATRIX LEG-06 문구 제안: "부분 구현 → 검증 완료(HTML5 2014 §10.3.9 의 표 규칙: border/frame/rules/cellpadding/cellspacing/hspace/vspace/width/height/bgcolor/background/bordercolor/align/valign/nowrap/col·colgroup width/tr height/caption align/th 정렬/표 안 폼 숨김/호환 모드 표 글꼴 초기화·nowrap 예외). 제약: 숨겨진 표 부품(`[hidden]`)은 규격의 `visibility:collapse` 가 아니라 `display:none`(현행 WHATWG·브라우저 동작); `col`/`colgroup` 의 `span`·`rowspan`·`colspan` 은 P10-02 의 표 모델이 처리; `char`/`charoff`/`summary`/`axis`/`abbr`/`headers`/`scope` 는 렌더링 효과 없음(규격에도 없음); 호환 모드 여백 병합 규칙(§10.3.10)은 P07-07-b-10". 구현 클래스에 `css/cascade/TableHints`, `LegacyAttributes` 추가, 검증 사례에 `LegacyTablesTest` 21건·`SwingRenderingTest.testLegacyTableAttributesPaint`.
- ARCHITECTURE: `css/cascade` 에 `LegacyAttributes`(속성 해석·선언 출력 공용 도우미)와 `TableHints`(표 힌트)가 생겼고 `UaStyleSheet` 에 HTML 문서 전용 시트(`htmlOnly()`)가 추가되었다.

## 기준 자료

- HTML5 W3C REC 2014-10-28 [§10 Rendering](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html), 특히 §10.3.4의 font color/face/size.
- 같은 판본 [§2.4.6 Colors](https://www.w3.org/TR/2014/REC-html5-20141028/infrastructure.html#rules-for-parsing-a-legacy-color-value).
- 조회: 2026-11-17, `tools/webfetch`(Java 6으로 작성된 기존 도구), 실행 JRE Zulu 25. 두 문서 모두 HTTP 200. 도구용 새 JRE 사용은 제품 JRE 6 검증과 구분한다.

## 점검표

| LEG/작업 | 요소·속성 | 규칙과 기대 결과 | 현재 상태·검증 |
| --- | --- | --- | --- |
| LEG-01 / P07-07-b-3 | font color | §2.4.6: ASCII 공백, CSS3 이름, #rgb, 128자 제한, 성분 분할·절삭. 빈 원문/transparent만 실패하며 한 자리 성분도 색이 된다 | 완료: `LegacyFontHintsTest.testShortColorsAndEmptyInput/testColorWhitespaceAndKeywordEdition/testColorUnicodeAndLengthBoundary` |
| LEG-01 / P07-07-b-3 | font face | §10.3.4: font-family 값으로 해석. CSS 값 파싱은 다른 힌트의 선언에 영향을 주지 않아야 한다 | 완료: `testFaceValueIsolation`(인용된 구분자·목록·닫히지 않은 주석·추가 선언 거부) |
| LEG-01 / P07-07-b-3 | font size | §10.3.4: 앞 ASCII 공백 → 선택적 부호 → 연속 숫자 → 상대값 기준 3 → 1~7 범위 → 키워드. 부호 뒤 공백은 실패 | 완료: `testAbsoluteFontSizes/testRelativeSizesAndInvalidPrefixes/testInheritanceMutationAndAuthorCascade/testLayoutAndPixels`, `SwingRenderingTest.testLegacyFontHintsWithoutScript` |
| LEG-01 / P07-07-b-4 | big/small/tt/strike/s/u/blink | §10.3.4의 상대 크기·고정폭·장식. blink는 정책대로 정적 텍스트 | `LegacyUaStylesTest.testPhrasingDefaultsAndAuthorOverrides/testFontGeometryAndMutation/testDecorationPixelsAndStaticBlink` 통과 |
| LEG-01 / P07-07-b-4 | center, basefont | §10.3.1/3의 basefont 숨김 및 center 텍스트 정렬 힌트 | `testCenterHintCascade/testCenterGeometryAndPixels/testBasefontHiddenDoesNotChangeFontSize` 통과. basefont 의 HTML 4/IE 효과는 구현하지 않기로 했다(P07-07-b-9: `LegacyElementsTest.testBasefontIsHiddenWithoutEffect`) |
| LEG-01 / P07-07-b-5 | center, div/caption/표 부품 align의 자손 정렬 | §10.2 "align descendants": 두 margin 이 auto 가 아니고 제약이 넘쳐 오른쪽 margin 이 더 크게 강제되며 자신은 적용 가능한 align 속성이 없는 자손, 가장 깊은 정렬 요소가 이김 | 완료: `AlignDescendantsLayoutTest` 16건, `SwingRenderingTest.testAlignDescendantsPaintsAlignedBlocks` |
| LEG-01 / P07-07-b-4 | nobr/wbr | §10.3.4의 nobr nowrap 및 nobr wbr normal | `testNobrExplicitOpportunity/testWbrStyleMutationAndVisibility/testWbrOutsideNobrAndPlainNoWrap/testWbrEdgesAndIntrinsicSize/testWbrBeforeAtomicAndEmptyInline/testWbrWithFirstLine/testWbrKeepsFollowingNoWrapChunkIntact` 통과 |
| LEG-01 / P07-07-b-9 | marquee | §10.5.10과 정적 표시 정책 | 부분 완료: 정적 상자·표현 속성·클리핑은 `LegacyElementsTest.testMarqueeStyle/testMarqueePixels`; 애니메이션은 미구현(정책상 선택) |
| LEG-03 / P07-07-b-6 | ol/ul/li type, ol start/reversed, li value, dir/menu | §10.3.8의 type 힌트(ol 1/a/A/i/I 대소문자 구분, ul disc/circle/square ASCII 대소문자 무시, li 는 부모와 무관하게 둘 다)와 §2.4.4.1 정수 해석 규칙 | 완료: `LegacyListsTest` 6건. compact 는 §10 에 렌더링 규칙이 없어 효과 없음 |
| LEG-04 | body link/vlink/alink, margin* | §10.3.2의 색·속성 우선순위 | P07-07-b-1/2 검증 완료. 나머지 body 속성은 후속 대조 |
| LEG-06 / P07-07-b-7 | 표 속성 전체(§10.3.9) | 요소별 표현 힌트와 UA 규칙 | 완료: `LegacyTablesTest` 21건(아래 "P07-07-b-7 구현과 검증"의 점검표) |
| LEG-05 / P07-07-b-8 | hr/img/object/embed/iframe/br/p·h/legend/fieldset 정렬·크기 속성 | §10.3.12, 10.3.13, 10.4.3 | 완료: `LegacyEmbeddedTest` 17건 |
| LEG-08 / P07-07-b-9 | plaintext/listing/xmp/isindex | UA 표시 및 파서·폼 처리 | 완료: `LegacyElementsTest` 12건, `special.dat` |
| LEG-11 / P07-07-b-10 | quirks/limited-quirks/no-quirks | 문서 모드별 화면 차이 | 부분 완료: `LegacyQuirksTest` 13건(미구현 호환 모드 규칙은 "P07-07-b-10 구현과 검증"에 기록) |
| LEG-11 / P07-07-b-11 | 호환 모드의 남은 규칙 | Quirks 3.5/3.8/3.9/3.13, §10.4.2, `clip` quirk, 스크립트 style quirk, html/body 채우기 | 완료(한계는 "P07-07-b-11 구현과 검증"): `LegacyQuirksBoxesTest` 4, `BrokenImageFallbackTest` 4, `QuirksStyleScriptTest` 4, `LegacyQuirksTest` 16 |
| LEG-07 / P07-07-c | map/area/usemap/ismap | 클라이언트·서버 쪽 이미지 맵 | 완료: `ImageMapTest` 8, `ImageMapIntegrationTest` 5 |
| LEG-12 / P07-07-c | XHTML 1.0 | prolog, DOCTYPE, xml:lang, 엔티티, xml-stylesheet, document.write | 완료: `LegacyXhtmlPageTest` 3, `XmlStylesheetInstructionTest` 2 |
| LEG-13 / P07-07-c | 스크립트/스타일 호환 | `<!-- -->`, type, language, for/event | 완료: `LegacyScriptCompatTest` 6 |
| LEG-14 / P07-07-c | 폼 호환 | input type, name/id 접근, document/window 이름 접근, 이미지 버튼 | 완료(`input type=image` 그림 로드·그리기 제외): `LegacyFormCompatTest` 7, `ImageButtonIntegrationTest` 2 |
| LEG-15 / P07-07-c | 링크/메타 호환 | meta refresh, 옛 속성, a name, base target, rel | 완료: `MetaRefreshTest` 2, `MetaRefreshIntegrationTest` 9, `LegacyLinkMetaTest` 2, `LegacyLinkIntegrationTest` 5 |

P07-07-b 및 P07-07 전체는 미완료로 유지한다. 프레임 등 P07-07-c의 기본 동작은 이 작업의 완료 조건에 포함하지 않는다.

## P07-07-b-10 구현과 검증 (2026-11-17)

규격(판본과 조회일을 고정한다): (1) HTML5 REC 2014-10-28 [§10 Rendering](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html) 의 "In quirks mode ..." 규칙(§10.3.3 `form`, §10.3.9 표, §10.3.10 여백 병합 quirk, §10.3.11 폼 컨트롤, §10.4.2 이미지) — 조회 `tools/webfetch`, 실행 JRE Zulu 25, `Last-Modified: Fri, 24 Oct 2014 16:15:24 GMT`, 2026-11-17. (2) WHATWG [Quirks Mode Standard](https://quirks.spec.whatwg.org/) — Living Standard, `Last-Modified: Mon, 19 Oct 2026 11:56:37 GMT`, 조회 2026-11-17. (3) [CSS Values 4 부록 C Quirky Lengths](https://drafts.csswg.org/css-values-4/#quirky-length), [CSS Color 4 부록 B Quirky Hex Colors](https://drafts.csswg.org/css-color-4/#quirky-color) — 편집자 초안, 각각 `Last-Modified: Mon, 16 Nov 2026 22:56:27/28 GMT`, 조회 2026-11-17. 문서 모드 판정은 HTML5 2014 §8.2.5.4.1(DOCTYPE 의 public/system 식별자 표)이며 P07-07-a(`html/DocumentModes`, `DocumentModesTest`)에서 검증이 끝났다. Chromium 소스는 조회하지 않았다(미확인).

### 점검표

| 규칙 | 규격 | 이전 상태 | 구현 | 시험(`LegacyQuirksTest` 13건 외) |
| --- | --- | --- | --- | --- |
| DOCTYPE → quirks / limited-quirks / no-quirks | HTML5 2014 §8.2.5.4.1 | 구현됨 | 변경 없음 | `testDocumentModes`(8개 DOCTYPE), `DocumentModesTest` |
| `form {margin-bottom:1em}` | §10.3.3 | 없음 | UA 호환 모드 시트 | `testFormMargin` |
| 표의 글꼴/줄 높이/공백/정렬 초기화 | §10.3.9 | 구현됨 | 변경 없음 | `LegacyTablesTest.testQuirksTableFontReset`(b-7) |
| `td[nowrap]` + 길이 `width` → `white-space:normal` | §10.3.9 | 없음 | b-7 `TableHints.cell` | `LegacyTablesTest.testNowrap`(b-7) |
| 여백 병합 quirk(body/td/th 의 첫·끝 요소 margin 제거) | §10.3.10 | 없음 | `QuirkHints`(표현 힌트로 출력) + `StyleResolver.quirkStructureChanged`(DOM 변경 시 캐시 무효화) | `testMarginCollapsingQuirks`, `testMarginQuirksFollowMutation` |
| `input:not([type=image]), textarea {box-sizing:border-box}` | §10.3.11 | 구현됨 | 변경 없음 | `testControlBoxSizing` |
| `img[align=left/right] {margin 3px}` | §10.4.2 | 구현됨 | 변경 없음 | `LegacyEmbeddedTest.testQuirksImageAlignMargins`(b-8) |
| 이미지가 표시되지 않을 때 alt 텍스트/대체 상자(호환 모드에서는 크기를 가진 대체 상자) | §10.4.2 | 이미지 대체 표시(P18-06)가 모드와 무관하게 동작 | **미구현**: 호환 모드 전용 규칙은 구현하지 않음 | — |
| 단위 없는 길이(`<quirky-length>`: 규격 목록 33개 중 `clip` 을 뺀 32개 속성) | Quirks 3.2, Values 4 부록 C | 없음 | `QuirkyValues`, `ParseEnv.isQuirks`(문서 시트·`style` 속성·`@import`), 호환 모드에서만 | `testUnitlessLengths` |
| # 없는 16진수 색(`<quirky-color>`: color, background-color, border-*-color) | Quirks 3.1, Color 4 부록 B | 없음 | `QuirkyValues` | `testHashlessColors` |
| 줄 높이 quirk: 텍스트 없는 인라인 상자는 줄 높이 0(호환·제한적 호환 모드) | Quirks 3.3 | 없음 | `InlineLayouter`(`withoutText`) | `testLineHeightQuirks` |
| 블록의 줄 높이 무시(strut 없음) = 이미지 아래 간격 제거(호환·제한적 호환 모드) | Quirks 3.4 | 없음 | `InlineLayouter`: 블록 자신의 텍스트가 없는 줄은 strut 를 빼고, 아무 기여도 없으면 strut 로 돌아감 | `testLineHeightQuirks` |
| 백분율 높이 계산 quirk(높이가 auto 인 조상을 건너뜀) | Quirks 3.5 | 없음 | **미구현** | — |
| html 이 뷰포트를 채우고 body 가 html 을 채움(호환 모드) | Quirks 3.6, 3.7 | 없음 | `LayoutEngine.fillViewport`(가로쓰기, 후처리) | `testHtmlAndBodyFillTheViewport` |
| 표 셀 너비 계산 quirk(auto 너비 셀 안 img 는 앞뒤에 줄바꿈 기회 없음) | Quirks 3.8 | 없음 | **미구현** | — |
| 표 셀 nowrap 최소 너비 quirk | Quirks 3.9 | 없음 | **미구현**: HTML5 2014 의 `td[nowrap][width] {white-space:normal}`(b-7)이 있으므로 현행 표준의 방식은 따르지 않는다 | — |
| collapsing table quirk(행 그룹/캡션/열 없는 표: 높이 0, border none) | Quirks 3.10 | 없음 | `BoxBuilder.isCollapsingTable` | `testCollapsingTable` |
| 텍스트 장식이 표 안으로 전파되지 않음 | Quirks 3.11 | 엔진이 모든 모드에서 표 안으로 전파하지 않음 | 변경 없음(호환 모드에서 만족) | `testDecorationDoesNotPropagateIntoTables`(호환 모드만 고정). 표준 모드에서도 전파하지 않는 것은 CSS 2.1 과 다른 별도 한계 |
| 표가 body 의 색을 상속(`quirk-inherit`) | Quirks 3.12 | 없음 | `StyleResolver.compute`/`bodyElement` | `testTableColorFromBody` |
| 표 셀 높이의 box-sizing | Quirks 3.13 | 없음 | **미구현** | — |
| `:active`/`:hover` 만으로 된 선택자는 링크에만 일치 | Quirks 4.1 | 없음 | `SelectorMatcher.matchCompound`/`MatchContext.isNested` | `testHoverActiveQuirk` |
| id/class 선택자의 대소문자 무시 | HTML/Selectors(호환 모드) | 구현됨 | 변경 없음 | `SelectorTest`(기존) |

### 구현 요약과 한계

- **변경 파일**: `css/values/ParseEnv`(호환 모드 플래그), `css/props/QuirkyValues`(새), `css/props/PropertyRegistry`, `css/cssom/{StyleSheet,StyleSheetBuilder,DocumentStyleSheets,CssStyleDeclaration,UaStyleSheet}`, `css/cascade/{QuirkHints(새),HtmlPresentationalHints,StyleResolver}`, `css/selector/{MatchContext,SelectorMatcher}`, `layout/{LayoutEngine,BlockLayouter,InlineLayouter,BoxBuilder}`, 시험 `tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/LegacyQuirksTest.java`, `LegacyTablesTest.java`(표가 body 색을 받으므로 한 줄 수정), `core/StaticStateAuditTest`는 b-7 에서 수정했다.
- **값 quirk 는 구문 분석 단계에서 적용**한다: 호환 모드 문서의 `<style>`, `<link rel=stylesheet>`, `@import` 한 시트, `style` 속성을 `ParseEnv.isQuirks()` 로 파싱하고 `QuirkyValues.rewrite` 가 해당 속성(정해진 목록)의 최상위 구성 요소만 일반 차원/해시 토큰으로 바꾼 뒤 평소의 문법이 판정한다. `calc()` 같은 함수 안, `border`·`background`·`font` 같은 단축 속성, `clip: rect()` 의 예외, 스크립트의 `element.style` 설정, `var()` 치환 값, 교차 출처 시트의 예외(웹 표준은 시트의 출처를 구분한다)는 구현하지 않았다.
- **줄 높이 quirk**는 `LayoutEngine` 이 `BlockLayouter.lineQuirks`(호환 또는 제한적 호환 모드)를 켜면 `InlineLayouter.buildLine` 이 적용한다. 세로쓰기에서는 적용하지 않는다. 줄에 블록 자신의 텍스트가 없고 다른 기여가 있을 때만 strut 를 뺀다(빈 줄과 `<br>` 만 있는 줄은 이전처럼 strut 높이).
- **html/body 채우기**는 배치 뒤 후처리로 상자 높이만 늘린다: 늘어난 높이를 기준으로 배치되는 자손(백분율 높이, `bottom` 이 body 에 붙은 절대 배치 등)은 다시 배치하지 않는다. 세로쓰기는 다루지 않는다.
- **여백 병합 quirk** 는 규격이 "사용자 에이전트 시트 규칙"이라고 하는 것을 표현 힌트 단계(UA 시트 위, 작성자 규칙 아래)로 낸다(사용자 시트의 일반 규칙보다 위): 작성자 CSS 는 이긴다. DOM 변경 시 body/td/th 의 자식과 요소 자신의 캐시만 버린다.
- 호환 모드에서 `table` 이 body 의 색을 받는 것은 `StyleResolver.compute` 의 특례이다: 표 자신이 `color` 를 정하면(`initial` 을 포함하지 않음) 그 값을 쓴다.
- **미구현으로 남긴 호환 모드 규칙**: Quirks 3.5(백분율 높이), 3.8(셀 안 이미지 줄바꿈), 3.9(nowrap 셀 최소 너비), 3.13(셀 높이 box-sizing), HTML5 2014 §10.4.2 의 호환 모드 이미지 대체 상자. 이들은 P07-07-b-10 의 입력 목록(form margin, 표 글꼴, 단위 없는 길이, 해시 없는 색, body 높이, 이미지 아래 간격)에 없으며 상위 항목 완료 전에 별도 하위 항목으로 처리하거나 한계로 기능 표에 적어야 한다.
- 문서 모드는 DOCTYPE 로 정해지며 `setContent(..., "text/html")` 의 원문에 DOCTYPE 이 없으면 호환 모드가 된다(웹 문서와 같다): 예제와 시험 페이지에 `<!doctype html>` 이 필요하다.

### 시험

| 명령/범위 | 결과 | 비고 |
| --- | --- | --- |
| 새 시험 `LegacyQuirksTest` 를 수정 전(b-9 commit) 제품 클래스와 실행(`build/head2.sh`) | 2 통과 / 10 실패 | `build/quirks-before.log`: form margin, 여백 quirk, 단위 없는 길이, 해시 없는 색, hover quirk, 표 색, 뷰포트 채우기, 줄 높이, collapsing table 재현; 모드 판정과 장식 전파는 이미 맞아 통과 |
| 수정 후 `LegacyQuirksTest` | 13 통과 / 0 실패 | `build/quirks-3.log`(`testControlBoxSizing` 포함) |
| `org.duckdns.hjow.swingwebpanel.css` 전체 / `org.duckdns.hjow.swingwebpanel.layout` 전체 | 231 통과 / 290 통과, 실패 0 (`:has()` 인수를 중첩으로 표시한 마지막 수정 뒤 css 만 다시: 232 통과) | `build/css-reg3.log`, `build/layout-reg3.log`, `build/css-reg4.log` |
| `BUILD_DIR=build/full3 ./test.sh` | **중단**: 2394 통과 / 0 실패 뒤 `SvgReferenceTest` 도중 JDK 6 의 `fontmanager.dll` 에서 `EXCEPTION_ACCESS_VIOLATION`(JRE 내부 글꼴 배치 `sun.font.SunLayoutEngine.nativeLayout`, SVG 텍스트 시험)으로 JVM 이 종료됨 | `build/full-b10.log`. 이전 두 번의 전체 실행(b-7, b-9)에서는 같은 시험이 통과했고 이 변경(css/layout)은 그 경로와 무관하다. 비결정적인 JRE 6 충돌로 보이며 원인은 확인하지 못했다 |
| 위 실행에서 나타나지 않은 시험 클래스 41개 + `SvgReferenceTest` 단독 재실행 | 359 통과 + 12 통과, 실패 0, 충돌 없음 | `build/full-b10-rest.log`(`build/rest.sh`), `build/svgref.log`. 세 로그를 합친 고유 통과 시험 **2652**(= b-9 의 2639 + 새 13), 실패 0, 건너뜀은 b-9 와 같은 선택적 Opus 원본 벡터 12건 |
| `tools/sourcecheck` (css, layout, 시험) | 0 위반 | |

## P07-07-b-9 구현과 검증 (2026-11-17)

규격: HTML5 REC 2014-10-28 [§8.2.5.4.7 The "in body" insertion mode](https://www.w3.org/TR/2014/REC-html5-20141028/syntax.html#parsing-main-inbody)(isindex, plaintext, listing, xmp: 조회 `tools/webfetch` 2026-11-17, `Last-Modified: Fri, 24 Oct 2014 16:15:24 GMT`; 2014 판본에는 isindex 의 form/hr/label/input 확장이 **있다**), [§10.3.1 Hidden elements](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#hidden-elements), §10.3.3(listing, plaintext, pre, xmp, `pre[wrap]`), §10.5.10 The marquee element. 보조: WHATWG HTML Living Standard [15.5.13 The marquee element](https://html.spec.whatwg.org/multipage/rendering.html#the-marquee-element-2)(조회 2026-11-17, `Last-Modified: Mon, 16 Nov 2026 15:32:23 GMT`)의 `marquee { display:inline-block; text-align:initial; overflow:hidden !important }` 는 2014 판본에 없는 display 규칙을 보충하는 데 썼다. Chromium 소스는 조회하지 않았다(미확인).

### 점검표

| 요소·규칙 | 이전 상태 | 구현 | 시험(`LegacyElementsTest` 12건) |
| --- | --- | --- | --- |
| `plaintext`: 열린 p 를 닫고 토큰화기를 PLAINTEXT 로, 나머지 입력이 하나의 텍스트(종료 태그 없음) | 파서에 이미 구현 | 변경 없음, 시험 추가 | `testPlaintextTakesTheRestOfTheInput`, `testRawElementsLayout` |
| `listing`: pre 처럼(맨 앞 LF 하나 버림, 안의 마크업 해석) | 이미 구현 | 변경 없음, 시험 추가 | `testListingLikePre` |
| `xmp`: RAWTEXT, 열린 p 를 닫음 | 이미 구현 | 변경 없음, 시험 추가 | `testXmpIsRawText` |
| `listing, plaintext, pre, xmp` UA: block, monospace, `white-space:pre`, margin 1em 0 | 이미 구현 | 변경 없음, 시험 추가 | 위 세 시험 |
| `isindex`: p 를 닫고, 열린 form 이 없을 때 `form[action] > hr + label(prompt + input[name=isindex, 나머지 속성]) + hr`, 열린 form 안에서는 무시 | **없음**(일반 요소로 남음) | `HtmlTreeBuilder.isindex` | `testIsindexExpands`, `testIsindexAttributes`, `testIsindexInsideForm`; 트리 구성 자료 `special.dat` |
| `pre[wrap] {white-space:pre-wrap}` | 없음 | `HtmlPresentationalHints` (pre 만) | `testPreWrap` |
| `[hidden], ..., source, track { display:none }`, `embed[hidden] {display:inline; height:0; width:0}` | `source`, `track`, `embed[hidden]` 없음 | UA 시트 | `testHiddenElements` |
| `basefont`: 숨김, 뒤따르는 글 크기·`font size=+1` 기준에 영향 없음(HTML5 2014 에는 HTML 4 의 base font 효과가 없다) | 이미 숨김(`display:none`) | 변경 없음, 시험 추가. HTML 4/IE 의 효과(`<basefont size>` 가 이후 글을 키움)는 구현하지 않는다 | `testBasefontIsHiddenWithoutEffect` |
| `marquee` UA: inline-block, `text-align:initial`, overflow 항상 hidden(작성자 CSS 무시) | 일반 인라인 요소 | UA 시트(`!important`) | `testMarqueeStyle`, `testMarqueePixels` |
| `marquee` 힌트: bgcolor, width, height, hspace/vspace margin, direction=up/down 이면 자연 높이 200px(height 속성이 덮어씀) | 없음 | `HtmlPresentationalHints.marquee` | `testMarqueeStyle`, `testMarqueePixels` |

### 구현 요약과 한계

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/html/HtmlTreeBuilder.java`(`isindex` 확장, 기본 안내 문구 상수), `css/cascade/HtmlPresentationalHints.java`(`marquee`, `pre wrap`), `css/cssom/UaStyleSheet.java`(hidden 목록, `embed[hidden]`, `marquee`), 시험 `tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/LegacyElementsTest.java`, `tests/src/org/duckdns/hjow/swingwebpanel/html/LegacyHtmlTest.java`(`isindex` 를 일반 폐기 요소 목록에서 뺌), 자료 `tests/resources/html/tree/special.dat`(옛 사례 `<isindex><menuitem>` 는 현행 WHATWG 의 결과를 담았으므로 `<blink><menuitem>` 으로 바꾸고 HTML5 2014 의 isindex 확장 사례를 추가).
- **marquee 는 정적 상자이다**(프로젝트 정책): 애니메이션, `behavior`, `scrollamount`, `scrolldelay`, `loop`, `truespeed`, `start()`/`stop()`, `bounce/finish/start` 이벤트는 구현하지 않았다. 내용은 애니메이션 전 위치(시작 쪽에 붙은 일반 흐름)로 보이고 넘치는 부분은 잘린다. 가로 marquee 의 너비는 `width` 가 없으면 shrink-to-fit 이다(규격은 너비를 말하지 않으며, 브라우저는 가로 marquee 를 줄 전체 너비로 늘릴 수 있다: 미확인).
- `isindex` 는 HTML5 2014 의 형태로 확장한다. 2016년 이후 WHATWG 는 이 규칙을 지워 `isindex` 를 알 수 없는 요소로 두므로 현행 브라우저와 다르다(이 프로젝트의 기준 판본은 2014 이다). 안내 문구는 영어 기본 문장이며 현지화하지 않는다.
- HTML 4 `basefont` 의 효과는 구현하지 않는다: HTML5 2014 §10.3.1 이 숨기며 현행 브라우저도 숨긴다.

### 시험

| 명령/범위 | 결과 | 비고 |
| --- | --- | --- |
| 새 시험 `LegacyElementsTest` 를 수정 전(b-8 commit) 제품 클래스와 실행 | 5 통과 / 7 실패 | `build/elem-before.log`: isindex 3건, hidden 요소, marquee 2건, pre wrap 재현; plaintext/listing/xmp/basefont 는 이미 맞아 통과 |
| 수정 후 `LegacyElementsTest` | 12 통과 / 0 실패 | `build/elem-1.log` |
| `org.duckdns.hjow.swingwebpanel.html` 전체(60+1건) | 처음 1건 실패(`special.dat` 의 옛 isindex 사례) → 자료 수정 후 `TreeConstructionTest` 7건 통과 | `build/html-reg1.log`, `build/html-reg2.log` |
| `BUILD_DIR=build/full2 ./test.sh`(b-8 과 b-9 를 합친 전체 컴파일·회귀·실행기 종료 코드) | **2639 통과 / 0 실패 / 12 건너뜀**(351981 ms; 건너뜀 12건은 선택적 Opus 원본 벡터) | `build/full-b9.log`. b-7 전체 실행의 실패 3건 해소: `StaticStateAuditTest` 는 등록 수정으로 통과, worker 2건은 부하가 적은 이번 실행에서 통과(앞선 실패는 부하 의존으로 보임) |
| `tools/sourcecheck` (css, html, 시험, 자료) | 0 위반 | |

## P07-07-b-8 구현과 검증 (2026-11-17)

규격: HTML5 REC 2014-10-28 [§10.3.12 The hr element](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#the-hr-element-0), [§10.3.13 The fieldset and legend elements](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#the-fieldset-and-legend-elements), [§10.4.1~10.4.3 Replaced elements](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#replaced-elements), §10.3.3~10.3.4(`br clear`, 문단/제목 `align`)(조회 `tools/webfetch`, 실행 JRE Zulu 25, 2026-11-17, 판본 `Last-Modified: Fri, 24 Oct 2014 16:15:24 GMT`). Chromium 소스는 조회하지 않았다(미확인).

### 점검표

| 규칙 | 이전 상태 | 구현 | 시험(`LegacyEmbeddedTest`) |
| --- | --- | --- | --- |
| `hr[color], hr[noshade] {border-style:solid}` | `noshade` 만(1px 고정 두께), `color` 는 `border-color` 만 | 속성 존재만 보고 `border-style:solid` | `testRuleDefaultsAndSolid` |
| `hr` + color/noshade + `size`: 네 border 두께 = size/2 | 없음 | `border-width: size/2` | `testRuleSize` |
| `hr` + `size`, color/noshade 없음: size=1 → `border-bottom-width:0`, size>1 → `height: size-2` | size>1 높이만(color/noshade 와 무관), size=1 없음 | 규격대로 | `testRuleSize`, `testRulePixels` |
| `hr color` → `color`(글자색) | `border-color` | `color` | `testRuleDefaultsAndSolid` |
| `hr width`, `hr align`(left/right/center → margin) | 있음(값을 다듬어 비교) | 값 전체 비교(공백 불허) | `testRuleWidthAndAlign` |
| `iframe[frameborder=0], [frameborder=no i] {border:none}` | `0` 만 | `0`, `no`(대소문자 무시) | `testFrameBorder` |
| `align` → float(left/right), `vertical-align`(top/baseline/texttop/absmiddle·abscenter→middle/bottom), center·middle → 기준선에 세로 가운데 | `texttop` 없음, `center` 없음, `middle` 를 CSS `middle` 로 처리 | 새 키워드 `-webkit-baseline-middle`(엔진 확장)와 `InlineLayouter.alignment` | `testReplacedAlignment`, `testBaselineMiddleGeometry` |
| `hspace`/`vspace` → 좌우/상하 margin(차원 값: 길이 또는 %) | 정수 px 만, video/canvas 에도 적용 | applet/embed/iframe/img/object/input[type=image] 만, 차원 값 | `testReplacedSpacing` |
| `img/object/input[image] border` > 0 → 네 변 두께 + `solid` | `border>=0`, input 없음 | `border > 0`, input 포함 | `testReplacedBorder` |
| `width`/`height`(applet, embed, iframe, object, video, img, input[image]) | input[image] 없음 | input[image] 추가(canvas 는 프로젝트 확장으로 유지) | `testReplacedDimensions` |
| 호환 모드 `img[align=left/right i]` margin 3px | UA 시트에 있음 | 변경 없음, 시험 추가(hspace 가 이김) | `testQuirksImageAlignMargins` |
| `br[clear=left/right/all/both i]` | 값을 다듬어 비교, 줄바꿈이 float 를 비우지 않음 | 값 전체 비교, 강제 줄바꿈 뒤 줄이 해당 float 아래에서 시작(`InlineLayouter`/`BlockLayouter.clearSidesOf`) | `testBreakClear`, `testBreakClearLayout` |
| `p/h1~h6 align` left/right/center/justify(middle 은 효과 없음) | middle 이 p 에서 가운데로 처리됨 | b-7 의 `LegacyAttributes.textAlign` | `testParagraphAndHeadingAlignment` |
| fieldset UA: 좌우 margin 2px, `border:groove 2px ThreeDFace`, padding `0.35em 0.625em 0.75em`; legend 좌우 padding 2px | padding 이 `0.35em 0.75em 0.625em`, border 색 없음(검정) | 규격 값, `threedface` | `testFieldsetDefaults` |
| fieldset 은 새 블록 서식 문맥, legend 는 위쪽 border 에 걸치고 `align`(left/right/center)로 위치, 없으면 left(rtl 이면 right) | 구현됨(값을 다듬어 비교) | 값 전체 비교만 수정 | `testLegendAlignment`, `testFieldsetContainsFloatsAndBreaksBorder` |

### 구현 요약과 한계

- `HtmlPresentationalHints`: `hr`, `replaced`(embed·img·object·applet·iframe·input[image]·video·canvas), `br clear` 를 `LegacyAttributes` 공용 도우미로 다시 썼고 사용하지 않게 된 도우미(`add`, `attr`, `intValue`, `color`, `dimension`, `background`)를 지웠다. `input[type=image i]` 도 치환 요소 힌트를 받는다.
- `PropertyTable`/`InlineLayouter.alignment`: `vertical-align: -webkit-baseline-middle`(상자의 세로 가운데가 부모의 기준선) 추가. HTML5 2014 §10.4.3 의 "center/middle: 요소의 세로 가운데를 부모 기준선에 맞춘다"를 CSS `middle`(x 높이 절반 위)과 구별하기 위해서다. 작성자 CSS 에서도 쓸 수 있는 비표준 키워드이다.
- `BoxBuilder`/`InlineLayouter`/`BlockLayouter`: `br` 의 계산 스타일을 강제 줄바꿈 항목에 남겨, `clear` 가 있으면 다음 줄이 해당 쪽 float 의 아래에서 시작한다(CSS 2.1 의 인라인 `clear` 설명; 부드러운 줄바꿈에는 적용하지 않음).
- `UaStyleSheet`: fieldset 규칙을 규격 값으로 바꿨다. 규격과 다른 채 남긴 것: `hr {overflow:hidden}`(브라우저의 규칙으로 float 옆에서 블록 서식 문맥이 된다).
- 한계(기록): `absbottom`(브라우저 확장)은 규격에 없어 효과가 없다. `align` 값 비교는 규격대로 공백을 허용하지 않는다. 규격의 fieldset 안 padding(`0.625em` 좌우, `0.75em` 아래)은 현행 브라우저와 다를 수 있다(Chromium 확인은 미확인). `applet` 은 플러그인 대체 항목(P20-11)에서 다룬다. 이미지 맵 등 `usemap` 은 P07-07-c 이다.

### 시험

JDK 6u45, 이 worktree 에서 직접 실행했다.

| 명령/범위 | 결과 | 비고 |
| --- | --- | --- |
| 새 시험 `LegacyEmbeddedTest` 를 수정 전 제품과 실행 | 4 통과 / 11 실패 | `build/emb-before.log`: hr 실선·size, 두께, frameborder=no, align 키워드, hspace/vspace, border=0, br clear 값/배치, fieldset 기본값 등 재현 |
| 수정 후 `LegacyEmbeddedTest` | 17 통과 / 0 실패 | `build/emb-after3.log` |
| `org.duckdns.hjow.swingwebpanel.css.cascade.Legacy*`(모든 이전 시험) | 68 통과 / 0 실패 | `build/legacy-all1.log` |
| `org.duckdns.hjow.swingwebpanel.css` 전체 / `org.duckdns.hjow.swingwebpanel.layout` 전체 | 207 통과 / 290 통과, 실패 0 | `build/css-reg2.log`, `build/layout-reg2.log` |

전체 `./test.sh` 는 b-9 이후에 한 번에 돌린다(아래 b-9 절 참고).

## P07-07-b-7 구현과 검증 (2026-11-17)

규격: HTML5 REC 2014-10-28 [§10.3.9 Tables](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#tables-0)(조회 `tools/webfetch`, 실행 JRE Zulu 25, HTTP 200, `Last-Modified: Fri, 24 Oct 2014 16:15:24 GMT`, 조회일 2026-11-17), §2.4.4.1 음이 아닌 정수와 §2.4.4.4 차원 값의 해석 규칙. Chromium 소스는 이번 작업에서 조회하지 않았다(미확인).

### 점검표(§10.3.9)

| 규칙 | 이전 상태 | 구현 | 시험(`LegacyTablesTest` 외) |
| --- | --- | --- | --- |
| `table, td, th {border-color:gray}`, `thead,tbody,tfoot,tr {border-color:inherit}` | 없음(표에 `border>0` 일 때만 힌트가 gray 를 줌) | UA 시트 | `testBorderColors` |
| `table[rules|frame] {border-color:black}` 과 `rules` 표의 셀 black | 없음 | UA 시트(`UaStyleSheet.tableRuleColors`, 자식 결합자 목록) | `testBorderColors` |
| `table[border]` 폭/`outset`, 0 과 같으면 무시, 오류는 1px | `border>0` 만 처리, `<table border>` 는 효과 없음(오류) | `TableHints.table` | `testBorderAttribute`(빈 값, 0, -1, -0, " +2x", abc) |
| 셀 `border-width:1px; border-style:inset` (표의 `border`가 0 이 아닐 때) | 가장 가까운 조상 표를 봄(중첩 표도 바깥 표 힌트를 받음) | `LegacyAttributes.tableOfCell`(구조 `table > [thead｜tbody｜tfoot >] tr > td`) | `testBorderHintsStayWithTheirTable` |
| `bordercolor` | `border>0` 일 때만, 기본 gray 를 줌 | 단독으로 네 변 색 | `testBorderColors` |
| `frame` 9개 값 | 없음 | `TableHints.table`(9 값 표, ASCII 대소문자 무시) | `testFrameAttribute`, `testFrameWithBorderWidth`, `testFrameAndRulesPixels` |
| `rules` 5개 값(표 `hidden`+`collapse`, 셀 선, 열 그룹/행 그룹/행 선) | 없음 | `TableHints.table/cell/row/section/column` | `testRulesAttribute`, `testBorderAndRulesTogether`, `testFrameAndRulesPixels`, `SwingRenderingTest.testLegacyTableAttributesPaint` |
| `cellspacing`, `cellpadding` | `[0-9]` 앞쪽 숫자만(부호 ` +2` 거부) | 음이 아닌 정수 규칙(`LegacyAttributes.nonNegativeInteger`) | `testCellPaddingAndSpacing`, `testGeometry` |
| `hspace`/`vspace` of table | 없음 | 차원 값(길이 또는 %)으로 좌우/상하 margin | `testSizeMarginAndColorHints`, `testGeometry` |
| `width`/`height` of table, td, th, col | 있음 | 그대로(차원 값 해석을 ASCII 공백으로 교정) | `testSizeMarginAndColorHints` |
| `tr height` | 없음 | `TableHints.row` | `testSizeMarginAndColorHints`, `testGeometry` |
| `bgcolor`/`background` of thead, tbody, tfoot, tr | `bgcolor` 만 | 둘 다 | `testSizeMarginAndColorHints` |
| `table align`: left/right 는 float, center 는 auto margin | `middle` 도 가운데로 처리(규격에 없음) | center 만(대소문자 무시, 공백 불허) | `testAlignmentHints` |
| thead/tbody/tfoot/tr/td/th `align`: left/right/justify/center/middle/absmiddle | `middle`, `absmiddle` 이 효과 없음 | `LegacyAttributes.textAlign` | `testAlignmentHints` |
| `caption`: 기본 가운데, `align=left/right/justify`, `align=bottom` → `caption-side:bottom` | UA 의 `caption{text-align:center}`, `bottom` 없음 | 힌트(`TableHints.caption`): 사용자 CSS 보다 우선하는 표현 힌트 | `testAlignmentHints` |
| `th` 정렬: 부모의 `text-align` 이 초기값일 때만 center | `th{text-align:center}` 로 부모의 `text-align:right` 를 덮어씀 | UA `th{text-align:-internal-center}` + `StyleComputer` 처리 | `testHeaderCellTextAlign` |
| `valign` 네 값 | 있음(공백 허용) | 공백 불허, 대소문자 무시 | `testVerticalAlignHints` |
| `td[nowrap], th[nowrap]` 과 호환 모드의 길이 `width` 예외 | 예외 없음 | `TableHints.cell`(문서 모드 확인) | `testNowrap` |
| 호환 모드 표의 글꼴/줄 높이/공백/정렬 초기화 | 있음(`QUIRKS_TEXT`) | 변경 없음, 시험 추가 | `testQuirksTableFontReset` |
| HTML 문서에서 표/행 그룹/행의 자식 `form` 은 `display:none`(CSS 와 무관) | 없음 | UA `!important` 규칙, HTML 문서 전용 시트(`UaStyleSheet.htmlOnly`) | `testFormsInTablesAreHidden` |
| `col`/`colgroup` `span`, `colgroup` 폭 | 배치 엔진이 처리(P10-02) | `colgroup width`(HTML 4.01) 확장 유지 | `testGeometry`, `testColumnGroupSpan` |
| 작성자 CSS 가 모든 힌트보다 우선, 속성 변경 시 재계산 | — | 변경 없음 | `testAuthorStylesOverrideTableHints`, `testAttributeMutation` |

### 구현 요약

- `css/cascade/LegacyAttributes`(새): "음이 아닌 정수"(앞 ASCII 공백, 부호, 숫자, 뒤 글자 무시, 오류/0 이 아닌 음수 거부), "차원 값", 열거형 속성의 키워드 맞춤(값 전체, 다듬지 않음, ASCII 대소문자 무시: 규격의 `[attr=value i]` 와 같다), 선언 출력, `table > tr > td` 구조로 표를 찾는 도우미. 이전에 `HtmlPresentationalHints` 안에 있던 중복 도우미를 옮겼다.
- `css/cascade/TableHints`(새): 위 표의 힌트. `HtmlPresentationalHints.hintsFor` 는 표 부품(td/th/tr/thead/tbody/tfoot/caption/colgroup)이 자신의 속성이 없어도 표의 힌트를 받도록 속성 검사를 건너뛴다.
- `UaStyleSheet`: border 색 규칙, `th` 정렬 규칙(`-internal-center`, 새 텍스트 정렬 키워드), 표 안 폼 규칙(HTML 문서 전용 시트). `StyleResolver.ensureIndex` 가 HTML 문서에 그 시트를 더한다. `caption{text-align:center}` UA 규칙은 표현 힌트로 옮겼다.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/css/cascade/{LegacyAttributes,TableHints,HtmlPresentationalHints,StyleComputer,StyleResolver}.java`, `css/cssom/UaStyleSheet.java`, `css/props/PropertyTable.java`, 시험 `tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/{LegacyFixture,LegacyTablesTest,StyleResolverTest}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/SwingRenderingTest.java`.

### 규격과 다른 점(기록)

- 숨겨진 표 부품(`tr[hidden]` 등): 2014 규격은 `display` 를 표 값으로 유지하고 `visibility:collapse` 를 주지만, 이 엔진의 UA 시트는 일반 `[hidden]{display:none}` 을 그대로 쓴다(현행 WHATWG HTML 과 브라우저의 동작). 행/열 접기(`visibility:collapse` 의 공간 제거)는 배치 엔진에 없다.
- `colgroup width` 는 2014 렌더링 절에 없고 HTML 4.01 에 있는 속성이므로 "이전 HTML 지원" 목표로 남겼다.
- 속성 값 공백: `align`, `valign`, `rules`, `frame` 등은 규격의 `[attr=value i]` 선택자처럼 값 전체를 비교하므로 `align=" center"` 는 효과가 없다(이전 구현은 앞뒤 공백을 잘랐다). 브라우저 중 일부는 `valign` 에서 공백을 허용할 수 있으나 이 구현은 규격을 따른다(Chromium 확인은 미확인).
- 레이아웃 층(`layout/AlignDescendants`)의 `align` 속성 판정은 b-5 의 구현대로 공백을 자르고 `table` 의 `middle` 을 "적용 가능한 align"으로 본다(자손 정렬 판정 전용; 표 자체의 힌트와 다름).

### 시험

JDK 6u45. 아래 숫자는 이 worktree 에서 직접 실행한 결과이다.

| 명령/범위 | 결과 | 비고 |
| --- | --- | --- |
| 새 시험 `LegacyTablesTest` 를 수정 전 제품 클래스와 실행 | 4 통과 / 15 실패 | `build/tbl-before.log`: border 빈 값, frame/rules, 표 border 색, `tr height`, hspace, nowrap 호환 예외, 폼 숨김, `th` 정렬 등 재현 |
| 수정 후 `LegacyTablesTest` | 21 통과 / 0 실패 | `build/tbl-after2.log` |
| `SwingRenderingTest.testLegacyTableAttributesPaint` | 1 통과 | 실제 `SwingWebPanel.paint`, JavaScript 비활성, 원문 보존 |
| `org.duckdns.hjow.swingwebpanel.css` 전체 | 188 통과 / 0 실패 | 기존 `StyleResolverTest` 가 지운 `dimensionValue` 를 쓰므로 `LegacyAttributes.dimension` 으로 고침 |
| `org.duckdns.hjow.swingwebpanel.layout` 전체 | 290 통과 / 0 실패 | `build/layout-reg1.log` |
| `BUILD_DIR=build/full ./test.sh` (전체 컴파일·회귀·실행기 종료 코드, 다른 작업자와 CPU 공유로 916초) | **2607 통과 / 3 실패 / 12 건너뜀**(건너뜀은 선택적 Opus 원본 벡터) | `build/full-b7.log`. 실패 3건: (1) `StaticStateAuditTest`: 새 정적 필드 `UaStyleSheet#htmlOnly` 를 REVIEWED 에 등록하지 않았음 → 등록으로 수정(이 실행 뒤라 아직 전체 재실행으로 확인하지 못함; 다음 항목의 전체 회귀에서 확인). (2) `WorkerLifecycleTest.testTerminateStopsAnEndlessLoopAndSilencesTheWorker`(60초 시간 초과) (3) `WorkerPortTest.testTargetOriginFilterOfWindowPostMessage`(오리진이 `""` 대신 `undefined`): 두 worker 시험은 같은 `build/full` 클래스로 단독 재실행하면 12건/5건 모두 통과하므로 다른 작업자와 CPU 를 나누는 부하 중의 시간 의존 실패로 보이며 이 작업의 변경(css/cascade)과는 무관하다(원인은 확인하지 못했다) |
| `tools/sourcecheck` (css, 시험 css) | 0 위반 | |

## P07-07-b-6 구현과 검증 (2026-11-17)

규격: HTML5 REC 2014-10-28 [§4.5.5 ol / §4.5.7 li의 ordinal value](https://www.w3.org/TR/2014/REC-html5-20141028/grouping-content.html), [§10.3.8 Lists](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html)(조회 `tools/webfetch`, 실행 JRE Zulu 25, HTTP 200, 조회일 2026-11-17), §2.4.4.1 정수 해석 규칙.

발견한 차이와 수정:

- `HtmlPresentationalHints.listType`: 이전에는 `li[type]` 를 부모가 `ol` 일 때만 번호 값(1/a/A/i/I), 아니면 기호 값(disc/circle/square)으로 해석해 `ul > li[type=a]`, `ol > li[type=square]` 가 무시되었고 속성 앞뒤 공백을 잘라 `type=" a"` 도 받았다. 규격의 선택자(`ol[type=1], li[type=1]`, `ul[type=disc i], li[type=disc i]`)대로 `ol` 은 번호 값만, `ul` 은 기호 값만, `li` 는 부모와 무관하게 둘 다 받으며 공백을 자르지 않는다. 번호 값은 대소문자를 구분하고 기호 값은 ASCII 대소문자를 구분하지 않는다.
- `BoxBuilder.parseInt`: 이전에는 `Integer.parseInt(trim)` 이어서 `start="+3"`, `start=" 4x"`, `value="7px"` 가 기본값으로 떨어졌다(Java 6 은 `+` 부호도 받지 못한다). "정수 해석 규칙"(앞쪽 ASCII 공백 → 선택적 부호 → 숫자, 뒤 글자 무시, 숫자 없음/int 범위 초과는 기본값)으로 바꿨다. `reversed` 의 기본 시작값(li 자식 수)과 `value` 후 이어지는 번호는 기존 구현이 맞았다.

시험 `LegacyListsTest`(6건): `ol type` 10개 값, `ul type` 9개 값, li 의 부모 독립(ul/ol/menu/dir 조합과 ol type 덮어쓰기), 작성자 CSS 우선순위(힌트 > UA 시트, 작성자 규칙 > 힌트), `start`/`value` 정수 해석 경계, `reversed`(start 해석 불가 시 li 수, value 로 재시작, type 힌트와 함께). 수정 전 소스(HEAD)로 같은 시험을 돌리면 6건 중 4건이 실패함을 확인했다(`ul > li[type=a]` 표식, `type='a '`, `start='+3'`, `reversed start=' 10x'`).

기준과 다른 점: HTML5 2014 는 `li value` 를 부모 `ol` 에 대해서만 처리하고 그 밖의 목록에서는 효과가 없다고 하지만, 이 구현은 모든 부모의 `li value` 를 따른다(현행 WHATWG HTML 과 브라우저의 동작). 숫자 표식을 쓰는 `ul` 에서만 보이는 차이이며 시험은 `ol` 만 확인한다. `compact` 속성은 규격 §10 에 렌더링 규칙이 없어 효과가 없다.

시험 실행: `scripts/pkgtest.sh lists "layout html css dom" "layout" --filter org.duckdns.hjow.swingwebpanel.layout.LegacyLists`(JDK 6u45) 6 통과 / 0 실패. 전체 `./test.sh`: 2543 íµê³¼ / 0 ì¤í¨ / 12 ê±´ëë(494585 ms, ì´ì  2537 ìì ì ìí 6ê±´ ì¦ê°).

## P07-07-b-5 구현과 검증 (2026-11-17)

규격: HTML5 REC 2014-10-28 [§10.2](https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html)의 "align descendants" 정의(조회 `tools/webfetch`, 실행 JRE Zulu 25, HTTP 200, `Last-Modified: Fri, 24 Oct 2014 16:15:24 GMT`, 조회일 2026-11-17), §10.3.3(center, div align), §10.3.9(caption, thead, tbody, tfoot, tr, td, th의 align).

정의의 요건(자손을 정렬하는 조건): 두 가로 margin 이 모두 `auto` 가 아니고, 제약이 넘치며(CSS 2.1 10.3.3) 한쪽 margin 의 사용 값이 더 크게 강제되고, 자신은 적용 가능한 align 속성이 없는 자손만 정렬한다. 여러 요소가 정렬하면 가장 깊게 중첩된 요소가 이긴다. 정렬된 상자는 사용 margin 으로 위치를 정한다.

구현:

- `layout/AlignDescendants`: 정렬 요소 판정(`center`는 항상 가운데, `div`/표 부품은 align 값 center·middle 가운데, left·justify 왼쪽, right 오른쪽, `caption`은 left/right/justify가 아니면 가운데)과 조상 사슬에서 가장 가까운 정렬 요소 찾기. 자손 자신에게 적용 가능한 align 속성(정렬 요소, p/h1~h6, table, hr의 규칙이 있는 값)이 있으면 정렬하지 않는다. 생성 콘텐츠 블록은 원래 요소부터 찾는다. `text-align`과 독립이다.
- `BlockLayouter.resolveMargins`: auto margin 이 없고 오른쪽 margin 이 더 크게 강제되면(남는 공간 > 0) 가운데는 margin 상자를 가운데에 두고(왼쪽·오른쪽 margin 에 남는 공간의 절반씩을 더함, 다른 브라우저와 같음), 오른쪽은 남는 공간을 왼쪽 margin 으로 옮긴다. 왼쪽은 그대로(오른쪽 margin 이 강제됨). 블록 상자, 치환 블록(`display:block` 이미지), 표(`TableLayouter` 도 같은 경로), `max-width` 로 줄어든 너비에 적용된다. 포함 블록보다 넓은 상자(남는 공간 음수)는 움직이지 않는다.
- `BlockLayouter.layoutBlockLevel`: 사용 margin 이 정해진 뒤에 안쪽 float 문맥의 x(`nextX`)를 갱신한다. 이전에는 호출자가 계산 margin 으로 정한 값이 그대로 남아, auto margin 으로 가운데 간 블록 안의 float 가 바깥 float 옆으로 밀렸다(시험 `testFloatInsideAlignedBlock`이 이전 동작에서 210 대 150 으로 실패하는 것을 확인했다).

Chromium 과의 차이(기능 표 기록, **Chromium 소스는 이번 작업에서 조회하지 않았다 — 미확인**): 알려진 구현 방식(`-webkit-center` 를 상속되는 `text-align` 값으로 처리)이라면 상속 사슬 중간의 작성자 `text-align` 이 정렬을 취소하고 자손의 align 속성은 부모 정렬을 막지 않을 것이다. 이 구현은 규격 문구를 따라 `text-align`과 독립이며 자손의 적용 가능한 align 속성이 있으면 정렬하지 않는다. 글쓰기 방향은 고려하지 않는다(엔진이 오른쪽 margin 을 강제하는 왼쪽에서 오른쪽 가정).

시험:

| 명령/범위 | 결과 | 비고 |
| --- | --- | --- |
| `scripts/pkgtest.sh align "layout html css dom" "layout" --filter org.duckdns.hjow.swingwebpanel.layout.AlignDescendants` | 16 통과 / 0 실패 (중간에 blockquote 기본 margin 40px 를 놓친 시험 오류 1건을 고침) | JDK 6u45 |
| float 시험에서 `nextX` 갱신 한 줄을 끄고 실행 | 실패(기대 150, 실제 210) | 시험이 변경을 지키는지 확인 |
| `./test.sh`(전체 컴파일·회귀·실행기 종료 코드 점검) | **2537 통과 / 0 실패 / 12 건너뜀**(674864 ms; 동시 작업 때문에 이전 실행보다 느림). 이전 2520 에서 새 시험 17건 증가. 건너뜀 12건은 선택적 Opus 원본 벡터 | `build/align-full.log` |

한계: 정렬 요소와 자손의 판정은 레이아웃 시점에 DOM 속성을 읽는다(속성 변경은 문서 재배치가 일어나는 경로에서 반영). `marquee`, `basefont` 과거 판본 효과는 P07-07-b-9. `table[align]`/`hr[align]`/`p[align]` 은 자손 정렬을 하지 않고 텍스트 정렬/auto margin 힌트만 가진다.

## P07-07-b-4 구현과 검증 (2026-11-17)

규격: 위 `tools/webfetch`로 조회한 HTML5 REC 2014-10-28의 §10.3.1 Hidden elements, §10.3.3 Flow content, §10.3.4 Phrasing content를 재사용했다(조회일 2026-11-17). 새로운 규격 판본이나 Chromium 코드는 도입하지 않았다.

완료한 구현:

- `HtmlPresentationalHints`: 속성이 없는 center에도 `text-align:center`를 작성자 표현 힌트로 제공한다. 기존 `-webkit-center` UA 규칙은 계산만 되고 실제 줄 정렬이 되지 않았으며, 일반 사용자 CSS가 이기던 출처 문제도 있었다. 일반 작성자 CSS·중요 사용자 CSS는 힌트를 덮어쓴다.
- `UaStyleSheet`: §10.3.4의 `blink{text-decoration:blink}`, `nobr wbr{white-space:normal}` 추가. blink의 그림은 프로젝트 정책대로 정적이며 추가 타이머가 없다.
- `BoxBuilder`/`InlineItem`/`InlineLayouter`: wbr를 부모 스타일의 U+200B 글리프 대신 자체 계산 스타일을 가진 `OPPORTUNITY`로 전달한다. 필요한 경우에만 다음 내용 앞에서 줄을 나누며, nowrap인 다음 텍스트·원자 상자에도 적용한다. 빈 인라인 가장자리는 기회를 소모하지 않는다. 기회만 있는 문서는 빈 줄을 만들지 않는다. CSS로 nowrap 또는 display:none을 지정하면 기회를 억제한다. wbr의 뒤쪽 nowrap 텍스트는 `overflow-wrap:break-word`여도 내부를 나누지 않는다.
- `LegacyUaStylesTest` 13건: 위 점검표의 스타일·힌트 출처·변경·상자·픽셀·내재 너비·첫 줄 스타일 검증. 기존 big/small/tt/strike/s/u/basefont 동작도 계산 값만 확인하지 않고 글꼴·기하·화면을 대조했다.
- `SwingRenderingTest.testLegacyUaAlignmentAndWrappingWithoutScript`: 실제 `SwingWebPanel.paint`에서 JavaScript 비활성, center의 빈 양쪽 가장자리, wbr 뒤 원자 상자의 다음 줄 배치, 수신 원문 보존 확인.

검증 환경: JDK/JRE 6u45, Windows. 입력 및 기대 결과는 새 시험 클래스의 각 메서드에 고정했다.

| 명령/범위 | 결과 | 로그 |
| --- | --- | --- |
| 최초 새 시험 8건을 수정 전 제품과 실행 | 1 통과 / 7 실패: center 위치·힌트 출처, blink 값, wbr 스타일·줄바꿈·빈 줄 문제 재현 | `build/ua-before.log` |
| 추가 경계 시험 후 빈 인라인/원자 상자 조합 | 11 통과 / 1 실패 → 기회 소모 시점 수정 | `build/ua-after.log`(중간 결과) |
| 추가 nowrap/overflow-wrap 조합 | 0 통과 / 1 실패 → nowrap 덩어리 내부의 비상 줄바꿈 억제 | `build/ua-nowrap-before.log` |
| 최종 집중 시험: 새 UA 13건 + 기존·신규 Swing Legacy 2건 | 15 통과 / 0 실패 / 0 건너뜀 | `build/ua-focused.log` |
| `test.bat`(전체 제품·시험 컴파일, 제품 JAR, 전체 회귀, 실행기 실패 종료 코드) | **2520 통과 / 0 실패 / 12 건너뜀**(296510 ms), JAR 생성·내용 검사 및 실패 스위트 종료 코드 1 확인 통과 | `build/ua-full.log` |
| `bash tools/sourcecheck/run.sh --max 40` | 1849개 파일 / 0 위반 | `build/ua-sourcecheck.log` |

첫 전체 실행은 nowrap/overflow-wrap 경계 수정 전에 중단했고 `build/ua-full-superseded.log`로 보존했다. 위 수치는 최종 코드로 다시 실행한 전체 회귀이며 중단된 실행은 합산하지 않는다. 건너뜀 12건은 선택적 Opus 원본 벡터 두 판본의 파일이 없는 사례로 각각 이유가 기록되어 있다(`SWP_OPUS_VECTORS`, `SWP_OPUS_VECTORS_8251`). 제품 JAR 검사는 이번 전체 실행에서 통과했다.

최종 소스에서 반복할 수 있는 검증 명령(PowerShell, 저장소 루트):

```powershell
.\test.bat
& 'C:\Program Files\Java\jdk1.6.0_45\bin\java.exe' -cp 'build/classes;build/test-classes' org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.css.cascade.LegacyUaStylesTest org.duckdns.hjow.swingwebpanel.SwingRenderingTest --filter Legacy
& 'C:\Program Files\Git\bin\bash.exe' tools/sourcecheck/run.sh --max 40
git diff --check
```

미해결 사항: 이번 하위 작업의 알려진 실패는 없다. center의 자손 블록 정렬(P07-07-b-5), marquee 정적 상자·속성, basefont의 과거 판본 효과 및 LEG-03~06/08/11은 미완료다. 실제 창의 입력·글꼴 모양은 이 오프스크린 검증으로 대체하지 않는다. 다음 작업 번호는 P07-07-b-5이다. P07-07-b-4만 완료로 표시하고 상위 항목은 미완료로 유지했다.

변경 파일: `AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, 이 문서, `css/cascade/HtmlPresentationalHints.java`, `css/cssom/UaStyleSheet.java`, `layout/{BoxBuilder,InlineItem,InlineLayouter}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/LegacyUaStylesTest.java`, `tests/src/org/duckdns/hjow/swingwebpanel/SwingRenderingTest.java`.

## P07-07-b-3 검증 결과

환경: Windows, JDK/JRE `1.6.0_45`(`C:\Program Files\Java\jdk1.6.0_45`)로 컴파일·실행. 새 JRE는 규격 조회에만 사용했다.

1. 수정 전 HEAD `2730b6f` 전체 회귀:
   `bash scripts/snaptest.sh resume1117 "src tests" "js dom html css net storage core content layout render download media @root" "js dom html css net storage core content layout render download media testing @root"`
   → **2496 통과 / 0 실패 / 13 건너뜀**(440833 ms). `build/resume1117.log`.
   건너뜀: 선택적 Opus 원본 벡터 12건(두 판본의 환경 변수 미설정), 스냅샷에서 빌드하지 않은 제품 JAR 검사 1건. 수정 후 전체 재실행 결과로 표시하지 않는다.
2. 새 `LegacyFontHintsTest`를 수정 전 제품 클래스와 실행 → **0 통과 / 8 실패**. 색의 `StringIndexOutOfBoundsException`, 긴 선행 0, ASCII 공백, 인용된 글꼴 이름 문제 재현. `build/font-before.log`.
3. 수정 제품 클래스 및 두 시험 클래스를 `javac -encoding UTF-8 -source 1.6 -target 1.6`으로 별도 `build/font-after`에 컴파일. 기반 클래스/시험 자원은 위 스냅샷의 것을 사용하고 수정 클래스를 클래스패스 맨 앞에 둠:
   - `LegacyFontHintsTest` → **8 통과 / 0 실패 / 0 건너뜀**. `build/font-after.log`.
   - CSS·레이아웃·렌더링의 모든 `*Test.class` 및 새 시험 → **558 통과 / 0 실패 / 0 건너뜀**(71128 ms). `build/font-regression.log`.
   - `SwingRenderingTest --filter testLegacyFontHintsWithoutScript` → **1 통과 / 0 실패**. 실제 `SwingWebPanel.paint`를 EDT에서 호출, JavaScript 비활성, 색·48×24 상자 경계·원문 보존 확인. `build/font-swing.log`.
4. `bash tools/sourcecheck/run.sh --max 40` → **1848개 파일 / 위반 0**. `git diff --check` 통과.

수정 후 검증 명령(PowerShell, 저장소 루트):

```powershell
$baseline = 'build/snap-resume1117/build/pkg-resume1117'
New-Item -ItemType Directory -Force build/font-after | Out-Null
& 'C:\Program Files\Java\jdk1.6.0_45\bin\javac.exe' -encoding UTF-8 -source 1.6 -target 1.6 -cp "$baseline/classes;$baseline/test-classes" -d build/font-after src/org/duckdns/hjow/swingwebpanel/css/cascade/HtmlPresentationalHints.java tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/LegacyFontHintsTest.java tests/src/org/duckdns/hjow/swingwebpanel/SwingRenderingTest.java
$fontClassPath = "build/font-after;$baseline/classes;$baseline/test-classes"
& 'C:\Program Files\Java\jdk1.6.0_45\bin\java.exe' -cp $fontClassPath org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.css.cascade.LegacyFontHintsTest
& 'C:\Program Files\Java\jdk1.6.0_45\bin\java.exe' -cp $fontClassPath org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.SwingRenderingTest --filter testLegacyFontHintsWithoutScript
$testClasses = @(Get-ChildItem -Path "$baseline/test-classes/org/duckdns/hjow/swingwebpanel/css","$baseline/test-classes/org/duckdns/hjow/swingwebpanel/layout","$baseline/test-classes/org/duckdns/hjow/swingwebpanel/render" -Recurse -Filter '*Test.class' | ForEach-Object { $_.FullName.Substring((Resolve-Path "$baseline/test-classes").Path.Length + 1).Replace('\','.').Replace('.class','') })
$testClasses += 'org.duckdns.hjow.swingwebpanel.css.cascade.LegacyFontHintsTest'
& 'C:\Program Files\Java\jdk1.6.0_45\bin\java.exe' -cp $fontClassPath org.duckdns.hjow.swingwebpanel.testing.TestRunner @testClasses
```

생성물과 로그는 git 무시 대상이다. 화면 밖 그리기 검증이며 실제 창의 사용자 입력·글꼴 모양 검증으로 기록하지 않는다.

## P07-07-b-3 변경 파일 및 인계 (이전 기록)

- `src/org/duckdns/hjow/swingwebpanel/css/cascade/HtmlPresentationalHints.java`: 공용 과거 색 해석 수정(본문·표 등 같은 도우미를 쓰는 속성에도 적용), font face의 단일 CSS 값 파싱·직렬화, 크기의 포화 숫자 해석. 기존 공개 API 변화 없음.
- `tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/LegacyFontHintsTest.java`: 새 회귀 8건.
- `tests/src/org/duckdns/hjow/swingwebpanel/SwingRenderingTest.java`: 실제 Swing 경로 회귀 1건.
- `AGENTS.md`, `docs/FEATURE_MATRIX.md`, `docs/PROGRESS.md`, 이 문서: 하위 작업·근거·범위 기록.

미해결 사항: 이 작업 범위의 알려진 실패는 없다. P07-07-b 상위의 다른 LEG 항목과 P07-07-c는 미완료다. basefont의 역사적 효과와 HTML5 2014의 숨김·무효 동작 차이는 후속에서 판본별로 기록해야 한다.

다음 작업 번호: P07-07-b의 남은 LEG-01 UA 규칙을 다음 하위 항목으로 분할하여 검증한다. 원래 진행 기록에 있는 프레임·워커·CSS 작업 트리 3개는 변경·병합하지 않았다.
