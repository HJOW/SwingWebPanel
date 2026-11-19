# LEGACY 작업자 브리프 (P07-07-b/c: 과거 HTML 요소·표현 속성·문서 모드의 화면 규칙)

## 작업 위치
새 git worktree(격리)에서 작업한다(`pwd` 로 확인). master 에서 갈라진 브랜치 `worktree-agent-<id>`.

## 목표
AGENTS.md `P07-07-b`(LEG-01/03~06/08/11 의 UA 스타일·표현 속성·목록·문서 모드별 화면 규칙)를 끝내고, 가능하면 `P07-07-c`(LEG-02/07/09/12~15)에서 프레임/플러그인을 제외한 나머지(이미지 맵 LEG-07, XHTML LEG-12, 스크립트/스타일 호환 LEG-13, 폼 호환 LEG-14, 링크/메타 호환 LEG-15)를 점검·구현·검증한다. 프레임(LEG-02)은 FRAMES 작업자의 일이고 플러그인 대체(LEG-09)는 P20-11 이므로 건드리지 않는다.
이미 구현된 것: `css/cascade/HtmlPresentationalHints.java`(740줄: body 의 link/vlink/alink/margin*, align, valign, width/height, hspace/vspace, border, bgcolor, 표 속성 등 일부), `css/cssom/UaStyleSheet.java`, `html/DocumentModes.java`(+ `DocumentModesTest`, `LegacyHtmlTest`, `TreeConstructionTest`). 이 코드와 `docs/FEATURE_MATRIX.md` 의 LEG-01~15 행(현재 대부분 "미구현"으로 적혀 있으나 코드는 일부 있음 → 실제 상태를 조사해 갱신 문구를 제안)을 출발점으로 한다.

## 기준과 방법
- 기준 규격: **HTML5 W3C REC 2014-10-28 §10 "Rendering"**(10.3.x "Non-conforming features" 의 표현 속성 표와 UA 스타일시트 10.3.x, 10.4 "Replaced elements" 등)과 `docs/FEATURE_MATRIX.md` 의 "이전 HTML 요소/속성 목록" 절(P00-03). 조회: `tools/webfetch`(`tools/webfetch/AGENTS.md`; HTTPS 는 JDK 6 으로 불가하므로 기본 `java`(JDK 25)로 실행). URL/판본/조회일을 `docs/progress/legacy.md` 에 기록한다. 문서 모드(quirks/limited-quirks)의 차이는 WHATWG Quirks Mode Standard 를 보조 기준으로 쓰되 판본/조회일을 적는다.
- **먼저 점검표를 만든다**: REC §10 의 표현 속성 규칙(요소별 속성 → CSS 매핑)을 하나씩 열거해 `docs/progress/legacy.md` 에 표로 적고(요소, 속성, 규격 규칙, 현재 구현 여부, 시험 이름), 미구현을 순서대로 구현한다. 구현마다 계산 스타일과 상자/픽셀(`layout`/`render` 시험) 검증을 둔다. 규칙 구현은 `HtmlPresentationalHints` 와 `UaStyleSheet` 에 가산한다(작성자 CSS 가 항상 이긴다는 우선순위 규칙 시험 포함).
- 반드시 포함: `font`(color/face/size 의 규격 해석 알고리즘), `basefont`, `center`, `big/small/tt/strike/s/u/nobr/blink/marquee`(표시만; marquee 애니메이션은 선택), `dir/menu`, `ol/ul` 의 `type/compact/start`, `li value/type`, `align/valign/width/height/hspace/vspace/border/bgcolor/nowrap/clear/noshade/size`, 표의 `cellpadding/cellspacing/border/frame/rules/summary/char/charoff/axis/headers/background`, `plaintext/listing/xmp/isindex`, `<body background/text/link/vlink/alink/bgcolor/marginwidth...>`(일부는 이미 구현), 문서 모드별 차이(quirks 의 표 안 글꼴 상속, 이미지 아래 간격 `vertical-align` 처리, `body` 높이 채우기, 줄 높이 처리, 숫자 단위 없는 길이, `hashless color`, `<table>` 안 상속 등 — 규격에 있는 차이만).
- JavaScript 없이 HTML/CSS 만으로 검증할 수 있게 하고, 실제 `SwingWebPanel` 오프스크린 `paint` 로 화소 확인을 몇 개 둔다.

## 소유 경로
`src/org/duckdns/hjow/swingwebpanel/css/cascade/HtmlPresentationalHints.java`, `css/cssom/UaStyleSheet.java`, `html/DocumentModes.java` 와 필요한 `layout/**`·`render/**` 의 가산 변경, 시험(`tests/src/org/duckdns/hjow/swingwebpanel/css/**`, `html/**`, `layout/**`, `render/**`), `docs/progress/legacy.md`. CSS-G 작업자가 `css/**`·`layout/**`·`render/**` 를 동시에 고치고 있으므로(필터/마스크/논리 속성 등) 같은 파일은 작은 가산 변경만 하고 자주 commit 한다. `core/**`, `js/**` 는 건드리지 않는다(필요하면 "조정자 요청").
커밋 번호 대역: `20261117-A01` 형식은 쓸 수 없으므로 **`20261117-301` 부터**(3자리 숫자).

## 기록
`docs/progress/legacy.md`: 점검표, 구현 내역, 한계, 시험 명령과 실제 결과 수, 조정자 요청(FEATURE_MATRIX LEG-* 행 문구, AGENTS.md P07-07-b/c 체크 가능 여부).
