package org.duckdns.hjow.swingwebpanel.css.props;

/**
 * The data of the property table: for every supported longhand property its name, whether it is inherited, its initial value, the value
 * definition (grammar) and a flags string, and the named value definitions the grammars refer to. The order of the rows defines the property ids
 * of {@link Prop}; adding a row means regenerating that class (see the generation note in <code>docs/progress/css.md</code>; a test checks
 * that both agree).
 * <p>
 * Flags: <code>P</code> the property is parsed and stored but has no effect on rendering in this engine (parsed-only).
 * <p>
 * 속성 표의 데이터: 지원하는 모든 개별 속성의 이름, 상속 여부, 초기값, 값 정의(문법), 플래그 문자열, 그리고 문법이 참조하는 이름 있는 값 정의. 행의 순서가
 * {@link Prop} 의 속성 id 를 정한다. 행을 더하면 그 클래스를 다시 생성해야 한다(<code>docs/progress/css.md</code> 의 생성 메모 참조. 시험이 둘이 일치하는지 확인한다).
 * <p>
 * 플래그: <code>P</code> 속성은 파싱하고 저장하지만 이 엔진의 렌더링에는 영향이 없다(파싱만).
 */
final class PropertyTable {

    /**
     * Private constructor: static data class.
     * <p>
     * 비공개 생성자: 정적 데이터 클래스이다.
     */
    private PropertyTable() {
    }

    /** Named value definitions: {name, grammar}. / 이름 있는 값 정의: {이름, 문법}. */
    static final String[][] PRODUCTIONS = {
        {"line-width", "thin | medium | thick | <length [0,inf]>"},
        {"line-style", "none | hidden | dotted | dashed | solid | double | groove | ridge | inset | outset"},
        {"lp-nn", "<length-percentage [0,inf]>"},
        {"size", "auto | <length-percentage [0,inf]> | min-content | max-content | fit-content | fit-content(<length-percentage [0,inf]>) | stretch"
            + " | -webkit-fill-available | -moz-available | <anchor-function>"},
        {"max-size", "none | <length-percentage [0,inf]> | min-content | max-content | fit-content | fit-content(<length-percentage [0,inf]>) | stretch"
            + " | -webkit-fill-available | -moz-available | <anchor-function>"},
        {"box", "border-box | padding-box | content-box"},
        {"position", "[ left | center | right | top | bottom | <length-percentage> ]{1,4}"},
        {"repeat-style", "repeat-x | repeat-y | [ repeat | space | round | no-repeat ]{1,2}"},
        {"bg-size", "[ <length-percentage [0,inf]> | auto ]{1,2} | cover | contain"},
        {"bg-image", "none | <image>"},
        {"shadow", "<color>? && [ <length>{2,4} ] && inset?"},
        {"text-shadow-item", "<color>? && <length>{2,3}"},
        {"easing", "ease | linear | ease-in | ease-out | ease-in-out | step-start | step-end"
            + " | steps(<integer [1,inf]> [, <ident>]?) | cubic-bezier(<number [0,1]>, <number>, <number [0,1]>, <number>)"
            + " | linear(<any>#)"},
        {"transform-function", "matrix(<number>#{6}) | translate(<length-percentage> [, <length-percentage>]?) | translateX(<length-percentage>)"
            + " | translateY(<length-percentage>) | scale(<number> [, <number>]?) | scaleX(<number>) | scaleY(<number>)"
            + " | rotate(<angle> | <zero>) | skew([<angle> | <zero>] [, [<angle> | <zero>]]?) | skewX(<angle> | <zero>) | skewY(<angle> | <zero>)"
            // 3D 함수 중 인수가 정확히 2D 가 되는 것만 받는다(EXC-03): z 이동 0, z 배율 1, z 축 회전, 단위 행렬의 3D 항목.
            + " | translate3d(<length-percentage>, <length-percentage>, <length [0,0]>) | translateZ(<length [0,0]>)"
            + " | scale3d(<number>, <number>, <number [1,1]>) | scaleZ(<number [1,1]>) | rotateZ(<angle> | <zero>)"
            + " | rotate3d(<number [0,0]>, <number [0,0]>, <number [1,1]>, <angle> | <zero>)"
            + " | matrix3d(<number>, <number>, <number [0,0]>, <number [0,0]>, <number>, <number>, <number [0,0]>, <number [0,0]>,"
            + " <number [0,0]>, <number [0,0]>, <number [1,1]>, <number [0,0]>, <number>, <number>, <number [0,0]>, <number [1,1]>)"},
        // CSS Anchor Positioning 1: position-area 의 값(키워드 하나나 둘)과 position-try-fallbacks 의 자동 변형(6.1 절)
        {"area-value", "[ left | center | right | span-left | span-right | x-start | x-end | span-x-start | span-x-end | self-x-start | self-x-end"
            + " | span-self-x-start | span-self-x-end | span-all | top | bottom | span-top | span-bottom | y-start | y-end | span-y-start | span-y-end"
            + " | self-y-start | self-y-end | span-self-y-start | span-self-y-end | block-start | block-end | span-block-start | span-block-end"
            + " | inline-start | inline-end | span-inline-start | span-inline-end | self-block-start | self-block-end | span-self-block-start"
            + " | span-self-block-end | self-inline-start | self-inline-end | span-self-inline-start | span-self-inline-end | start | end | span-start"
            + " | span-end | self-start | self-end | span-self-start | span-self-end ]{1,2}"},
        {"try-tactic", "flip-block || flip-inline || flip-start || flip-x || flip-y"},
        {"align-position", "[ safe | unsafe ]? [ center | start | end | self-start | self-end | flex-start | flex-end | left | right ]"},
        {"content-distribution", "space-between | space-around | space-evenly | stretch"},
        {"overflow-keyword", "visible | hidden | clip | scroll | auto"},
        {"generic-family", "serif | sans-serif | monospace | cursive | fantasy | system-ui | ui-serif | ui-sans-serif | ui-monospace | ui-rounded | math"
            + " | emoji | fangsong"},
        {"blend-mode", "normal | multiply | screen | overlay | darken | lighten | color-dodge | color-burn | hard-light | soft-light | difference"
            + " | exclusion | hue | saturation | color | luminosity | plus-lighter"},
        {"number-percentage", "<number [0,inf]> | <percentage [0,inf]>"},
        {"filter-function", "blur(<length [0,inf]>?) | brightness(<number-percentage>?) | contrast(<number-percentage>?)"
            + " | drop-shadow(<color>? && <length>{2,3}) | grayscale(<number-percentage>?) | hue-rotate([ <angle> | <zero> ]?)"
            + " | invert(<number-percentage>?) | opacity(<number-percentage>?) | saturate(<number-percentage>?) | sepia(<number-percentage>?)"},
        {"cursor-keyword", "auto | default | none | context-menu | help | pointer | progress | wait | cell | crosshair | text | vertical-text | alias"
            + " | copy | move | no-drop | not-allowed | grab | grabbing | e-resize | n-resize | ne-resize | nw-resize | s-resize | se-resize"
            + " | sw-resize | w-resize | ew-resize | ns-resize | nesw-resize | nwse-resize | col-resize | row-resize | all-scroll | zoom-in | zoom-out"},
    };

    /**
     * The longhand properties: {name, "i" if inherited, initial value text, value definition, flags}. The order is the order of the ids in
     * {@link Prop}.
     * <p>
     * 개별 속성: {이름, 상속이면 "i", 초기값 문자열, 값 정의, 플래그}. 순서가 {@link Prop} 의 id 순서이다.
     */
    static final String[][] DEFS = {
        // 표시와 배치
        {"display", "", "inline", "none | contents | block | inline | inline-block | list-item | inline-list-item | table | inline-table"
            + " | table-row-group | table-header-group | table-footer-group | table-row | table-column-group | table-column | table-cell"
            + " | table-caption | flex | inline-flex | grid | inline-grid | flow-root | ruby | ruby-base | ruby-text | ruby-base-container"
            + " | ruby-text-container | run-in | -webkit-box | -webkit-inline-box | -webkit-flex | -webkit-inline-flex", ""},
        {"position", "", "static", "static | relative | absolute | fixed | sticky | -webkit-sticky", ""},
        {"top", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"right", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"bottom", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"left", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"float", "", "none", "left | right | none | inline-start | inline-end", ""},
        {"clear", "", "none", "none | left | right | both | inline-start | inline-end", ""},
        {"z-index", "", "auto", "auto | <integer>", ""},
        {"overflow-x", "", "visible", "<overflow-keyword>", ""},
        {"overflow-y", "", "visible", "<overflow-keyword>", ""},
        {"overflow-wrap", "i", "normal", "normal | break-word | anywhere", ""},
        {"visibility", "i", "visible", "visible | hidden | collapse", ""},
        {"opacity", "", "1", "<number> | <percentage>", ""},
        {"box-sizing", "", "content-box", "content-box | border-box", ""},
        {"width", "", "auto", "<size>", ""},
        {"height", "", "auto", "<size>", ""},
        {"min-width", "", "auto", "<size>", ""},
        {"min-height", "", "auto", "<size>", ""},
        {"max-width", "", "none", "<max-size>", ""},
        {"max-height", "", "none", "<max-size>", ""},
        {"clip", "", "auto", "auto | rect([ <length> | auto ]#{4}) | rect([ <length> | auto ]{4})", ""},
        // 여백
        {"margin-top", "", "0", "<length-percentage> | auto", ""},
        {"margin-right", "", "0", "<length-percentage> | auto", ""},
        {"margin-bottom", "", "0", "<length-percentage> | auto", ""},
        {"margin-left", "", "0", "<length-percentage> | auto", ""},
        {"padding-top", "", "0", "<lp-nn>", ""},
        {"padding-right", "", "0", "<lp-nn>", ""},
        {"padding-bottom", "", "0", "<lp-nn>", ""},
        {"padding-left", "", "0", "<lp-nn>", ""},
        // 테두리
        {"border-top-width", "", "medium", "<line-width>", ""},
        {"border-right-width", "", "medium", "<line-width>", ""},
        {"border-bottom-width", "", "medium", "<line-width>", ""},
        {"border-left-width", "", "medium", "<line-width>", ""},
        {"border-top-style", "", "none", "<line-style>", ""},
        {"border-right-style", "", "none", "<line-style>", ""},
        {"border-bottom-style", "", "none", "<line-style>", ""},
        {"border-left-style", "", "none", "<line-style>", ""},
        {"border-top-color", "", "currentcolor", "<color>", ""},
        {"border-right-color", "", "currentcolor", "<color>", ""},
        {"border-bottom-color", "", "currentcolor", "<color>", ""},
        {"border-left-color", "", "currentcolor", "<color>", ""},
        {"border-top-left-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-top-right-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-bottom-right-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-bottom-left-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-collapse", "i", "separate", "separate | collapse", ""},
        {"border-spacing", "i", "0", "<length [0,inf]>{1,2}", ""},
        {"border-image-source", "", "none", "<bg-image>", ""},
        {"border-image-slice", "", "100%", "[ <number [0,inf]> | <percentage [0,inf]> ]{1,4} && fill?", ""},
        {"border-image-width", "", "1", "[ <length-percentage [0,inf]> | <number [0,inf]> | auto ]{1,4}", ""},
        {"border-image-outset", "", "0", "[ <length [0,inf]> | <number [0,inf]> ]{1,4}", ""},
        {"border-image-repeat", "", "stretch", "[ stretch | repeat | round | space ]{1,2}", ""},
        {"outline-width", "", "medium", "<line-width>", ""},
        {"outline-style", "", "none", "auto | none | dotted | dashed | solid | double | groove | ridge | inset | outset", ""},
        {"outline-color", "", "currentcolor", "<color> | invert", ""},
        {"outline-offset", "", "0", "<length>", ""},
        {"box-shadow", "", "none", "none | <shadow>#", ""},
        // 배경
        {"background-color", "", "transparent", "<color>", ""},
        {"background-image", "", "none", "<bg-image>#", ""},
        {"background-repeat", "", "repeat", "<repeat-style>#", ""},
        {"background-attachment", "", "scroll", "[ scroll | fixed | local ]#", ""},
        {"background-position", "", "0% 0%", "<position>#", ""},
        {"background-size", "", "auto", "<bg-size>#", ""},
        {"background-origin", "", "padding-box", "<box>#", ""},
        {"background-clip", "", "border-box", "[ <box> | text | border-area ]#", ""},
        // 색과 글꼴
        {"color", "i", "black", "<color>", ""},
        {"font-family", "i", "serif", "[ <string> | <custom-ident>+ ]#", ""},
        {"font-size", "i", "medium", "xx-small | x-small | small | medium | large | x-large | xx-large | xxx-large | larger | smaller"
            + " | <length-percentage [0,inf]>", ""},
        {"font-style", "i", "normal", "normal | italic | oblique [ <angle> ]?", ""},
        {"font-variant-caps", "i", "normal", "normal | small-caps | all-small-caps | petite-caps | all-petite-caps | unicase | titling-caps", ""},
        {"font-weight", "i", "normal", "normal | bold | bolder | lighter | <number [1,1000]>", ""},
        {"font-stretch", "i", "normal", "normal | ultra-condensed | extra-condensed | condensed | semi-condensed | semi-expanded | expanded"
            + " | extra-expanded | ultra-expanded | <percentage [0,inf]>", ""},
        {"line-height", "i", "normal", "normal | <number [0,inf]> | <length-percentage [0,inf]>", ""},
        {"font-size-adjust", "i", "none", "none | <number [0,inf]>", ""},
        {"font-kerning", "i", "auto", "auto | normal | none", "P"},
        {"font-feature-settings", "i", "normal", "normal | [ <string> [ <integer [0,inf]> | on | off ]? ]#", "P"},
        // 글
        {"text-align", "i", "start", "start | end | left | right | center | justify | match-parent | justify-all | -webkit-left | -webkit-right"
            + " | -webkit-center | -internal-center", ""},
        {"text-align-last", "i", "auto", "auto | start | end | left | right | center | justify", ""},
        {"text-indent", "i", "0", "<length-percentage> && hanging? && each-line?", ""},
        {"text-transform", "i", "none", "none | [ capitalize | uppercase | lowercase ] || full-width || full-size-kana", ""},
        {"text-decoration-line", "", "none", "none | [ underline || overline || line-through || blink ] | spelling-error | grammar-error", ""},
        {"text-decoration-style", "", "solid", "solid | double | dotted | dashed | wavy", ""},
        {"text-decoration-color", "", "currentcolor", "<color>", ""},
        {"text-decoration-thickness", "", "auto", "auto | from-font | <length-percentage>", ""},
        {"text-underline-offset", "i", "auto", "auto | <length-percentage>", ""},
        {"text-overflow", "", "clip", "[ clip | ellipsis | <string> ]{1,2}", ""},
        {"text-shadow", "i", "none", "none | <text-shadow-item>#", ""},
        {"letter-spacing", "i", "normal", "normal | <length>", ""},
        {"word-spacing", "i", "normal", "normal | <length-percentage>", ""},
        {"white-space", "i", "normal", "normal | pre | nowrap | pre-wrap | pre-line | break-spaces", ""},
        {"word-break", "i", "normal", "normal | break-all | keep-all | break-word", ""},
        {"line-break", "i", "auto", "auto | loose | normal | strict | anywhere", ""},
        {"hyphens", "i", "manual", "none | manual | auto", ""},
        {"tab-size", "i", "8", "<number [0,inf]> | <length [0,inf]>", ""},
        {"vertical-align", "", "baseline", "baseline | sub | super | text-top | text-bottom | middle | top | bottom | -webkit-baseline-middle | <length-percentage>", ""},
        {"direction", "i", "ltr", "ltr | rtl", ""},
        {"unicode-bidi", "", "normal", "normal | embed | bidi-override | isolate | isolate-override | plaintext", ""},
        {"writing-mode", "i", "horizontal-tb", "horizontal-tb | vertical-rl | vertical-lr | sideways-rl | sideways-lr", ""},
        {"text-orientation", "i", "mixed", "mixed | upright | sideways", ""},
        {"orphans", "i", "2", "<integer [1,inf]>", "P"},
        {"widows", "i", "2", "<integer [1,inf]>", "P"},
        // 목록, 내용, 표
        {"list-style-type", "i", "disc", "none | <string> | <custom-ident> | symbols(<any>+)", ""},
        {"list-style-position", "i", "outside", "inside | outside", ""},
        {"list-style-image", "i", "none", "<bg-image>", ""},
        {"counter-reset", "", "none", "none | [ <custom-ident> <integer>? ]+", ""},
        {"counter-increment", "", "none", "none | [ <custom-ident> <integer>? ]+", ""},
        {"counter-set", "", "none", "none | [ <custom-ident> <integer>? ]+", ""},
        {"quotes", "i", "auto", "auto | none | [ <string> <string> ]+", ""},
        {"content", "", "normal", "normal | none | [ <string> | <image> | <generic-function> | open-quote | close-quote | no-open-quote"
            + " | no-close-quote ]+", ""},
        {"table-layout", "", "auto", "auto | fixed", ""},
        {"caption-side", "i", "top", "top | bottom", ""},
        {"empty-cells", "i", "show", "show | hide", ""},
        // 유연 상자와 정렬
        {"flex-grow", "", "0", "<number [0,inf]>", ""},
        {"flex-shrink", "", "1", "<number [0,inf]>", ""},
        {"flex-basis", "", "auto", "content | <size>", ""},
        {"flex-direction", "", "row", "row | row-reverse | column | column-reverse", ""},
        {"flex-wrap", "", "nowrap", "nowrap | wrap | wrap-reverse", ""},
        {"order", "", "0", "<integer>", ""},
        {"justify-content", "", "normal", "normal | left | right | <content-distribution> | <align-position>", ""},
        {"align-items", "", "normal", "normal | stretch | baseline | first baseline | last baseline | <align-position> | anchor-center", ""},
        {"align-self", "", "auto", "auto | normal | stretch | baseline | first baseline | last baseline | <align-position> | anchor-center", ""},
        {"align-content", "", "normal", "normal | baseline | first baseline | last baseline | <content-distribution> | <align-position>", ""},
        {"justify-items", "", "legacy", "normal | stretch | legacy | baseline | first baseline | last baseline | <align-position> | anchor-center", "P"},
        {"justify-self", "", "auto", "auto | normal | stretch | baseline | first baseline | last baseline | <align-position> | anchor-center", "P"},
        {"row-gap", "", "normal", "normal | <lp-nn>", ""},
        {"column-gap", "", "normal", "normal | <lp-nn>", ""},
        // 다단
        {"column-count", "", "auto", "auto | <integer [1,inf]>", ""},
        {"column-width", "", "auto", "auto | <length [0,inf]>", ""},
        {"column-rule-width", "", "medium", "<line-width>", ""},
        {"column-rule-style", "", "none", "<line-style>", ""},
        {"column-rule-color", "", "currentcolor", "<color>", ""},
        {"column-span", "", "none", "none | all", ""},
        {"column-fill", "", "balance", "auto | balance | balance-all", ""},
        // 격자(파싱만)
        {"grid-template-columns", "", "none", "none | <any>+", "P"},
        {"grid-template-rows", "", "none", "none | <any>+", "P"},
        {"grid-template-areas", "", "none", "none | <string>+", "P"},
        {"grid-auto-flow", "", "row", "[ row | column ] || dense", "P"},
        {"grid-auto-columns", "", "auto", "<any>+", "P"},
        {"grid-auto-rows", "", "auto", "<any>+", "P"},
        {"grid-row-start", "", "auto", "auto | <any>+", "P"},
        {"grid-row-end", "", "auto", "auto | <any>+", "P"},
        {"grid-column-start", "", "auto", "auto | <any>+", "P"},
        {"grid-column-end", "", "auto", "auto | <any>+", "P"},
        // 변형, 전환, 애니메이션
        {"transform", "", "none", "none | <transform-function>+", ""},
        {"transform-origin", "", "50% 50%", "[ <length-percentage> | left | center | right | top | bottom ]{1,3}", ""},
        {"transition-property", "", "all", "none | [ all | <custom-ident> ]#", ""},
        {"transition-duration", "", "0s", "<time [0,inf]>#", ""},
        {"transition-timing-function", "", "ease", "<easing>#", ""},
        {"transition-delay", "", "0s", "<time>#", ""},
        {"animation-name", "", "none", "[ none | <custom-ident> | <string> ]#", ""},
        {"animation-duration", "", "0s", "<time [0,inf]>#", ""},
        {"animation-timing-function", "", "ease", "<easing>#", ""},
        {"animation-delay", "", "0s", "<time>#", ""},
        {"animation-iteration-count", "", "1", "[ infinite | <number [0,inf]> ]#", ""},
        {"animation-direction", "", "normal", "[ normal | reverse | alternate | alternate-reverse ]#", ""},
        {"animation-fill-mode", "", "none", "[ none | forwards | backwards | both ]#", ""},
        {"animation-play-state", "", "running", "[ running | paused ]#", ""},
        // 사용자 인터페이스와 기타
        {"cursor", "i", "auto", "[ [ <url> [ <number> <number> ]? , ]* <cursor-keyword> ]", ""},
        {"pointer-events", "i", "auto", "auto | none | visible | visiblepainted | visiblefill | visiblestroke | painted | fill | stroke | all", ""},
        {"user-select", "", "auto", "auto | text | none | contain | all", ""},
        {"resize", "", "none", "none | both | horizontal | vertical | block | inline", ""},
        {"appearance", "", "auto", "none | auto | <ident>", "P"},
        {"accent-color", "i", "auto", "auto | <color>", ""},
        {"caret-color", "i", "auto", "auto | <color>", ""},
        {"scroll-behavior", "", "auto", "auto | smooth", "P"},
        {"object-fit", "", "fill", "fill | contain | cover | none | scale-down", ""},
        {"object-position", "", "50% 50%", "<position>", ""},
        {"image-rendering", "i", "auto", "auto | crisp-edges | pixelated | smooth | high-quality | -webkit-optimize-contrast", ""},
        {"filter", "", "none", "none | [ <filter-function> | <url> ]+", ""},
        {"mix-blend-mode", "", "normal", "<blend-mode>", ""},
        {"isolation", "", "auto", "auto | isolate", ""},
        {"will-change", "", "auto", "auto | <ident>#", "P"},
        {"contain", "", "none", "none | strict | content | [ size || layout || style || paint || inline-size ]", ""},
        {"text-rendering", "i", "auto", "auto | optimizespeed | optimizelegibility | geometricprecision", "P"},
        {"aspect-ratio", "", "auto", "auto || [ <number [0,inf]> [ / <number [0,inf]> ]? ]", ""},
        {"text-combine-upright", "i", "none", "none | all | digits <integer [2,4]>?", ""},
        {"clip-path", "", "none", "none | [ <generic-function> | <ident> | <url> ]+", ""},
        {"translate", "", "none", "none | <length-percentage> [ <length-percentage> <length [0,0]>? ]?", ""},
        {"rotate", "", "none", "none | <angle> | <zero> | z <angle> | z <zero>", ""},
        {"scale", "", "none", "none | [ <number> | <percentage> ]{1,2}", ""},
        // 합성과 마스킹(CSS-G)
        {"background-blend-mode", "", "normal", "<blend-mode>#", ""},
        {"mask-image", "", "none", "<bg-image>#", ""},
        {"mask-mode", "", "match-source", "[ alpha | luminance | match-source ]#", ""},
        {"mask-repeat", "", "repeat", "<repeat-style>#", ""},
        {"mask-position", "", "0% 0%", "<position>#", ""},
        {"mask-clip", "", "border-box", "[ border-box | padding-box | content-box | margin-box | fill-box | stroke-box | view-box | no-clip ]#", ""},
        {"mask-origin", "", "border-box", "[ border-box | padding-box | content-box | margin-box | fill-box | stroke-box | view-box ]#", ""},
        {"mask-size", "", "auto", "<bg-size>#", ""},
        {"mask-composite", "", "add", "[ add | subtract | intersect | exclude ]#", ""},
        {"clip-rule", "i", "nonzero", "nonzero | evenodd", "P"},
        // 논리 속성(CSS Logical Properties 1): 계단식에서 쓰기 모드와 방향에 따라 물리 속성으로 옮겨 쓴다(css.props.LogicalProps)
        {"margin-block-start", "", "0", "<length-percentage> | auto", ""},
        {"margin-block-end", "", "0", "<length-percentage> | auto", ""},
        {"margin-inline-start", "", "0", "<length-percentage> | auto", ""},
        {"margin-inline-end", "", "0", "<length-percentage> | auto", ""},
        {"padding-block-start", "", "0", "<lp-nn>", ""},
        {"padding-block-end", "", "0", "<lp-nn>", ""},
        {"padding-inline-start", "", "0", "<lp-nn>", ""},
        {"padding-inline-end", "", "0", "<lp-nn>", ""},
        {"inset-block-start", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"inset-block-end", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"inset-inline-start", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"inset-inline-end", "", "auto", "auto | <length-percentage> | <anchor-function>", ""},
        {"border-block-start-width", "", "medium", "<line-width>", ""},
        {"border-block-end-width", "", "medium", "<line-width>", ""},
        {"border-inline-start-width", "", "medium", "<line-width>", ""},
        {"border-inline-end-width", "", "medium", "<line-width>", ""},
        {"border-block-start-style", "", "none", "<line-style>", ""},
        {"border-block-end-style", "", "none", "<line-style>", ""},
        {"border-inline-start-style", "", "none", "<line-style>", ""},
        {"border-inline-end-style", "", "none", "<line-style>", ""},
        {"border-block-start-color", "", "currentcolor", "<color>", ""},
        {"border-block-end-color", "", "currentcolor", "<color>", ""},
        {"border-inline-start-color", "", "currentcolor", "<color>", ""},
        {"border-inline-end-color", "", "currentcolor", "<color>", ""},
        {"border-start-start-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-start-end-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-end-start-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"border-end-end-radius", "", "0", "<lp-nn>{1,2}", ""},
        {"inline-size", "", "auto", "<size>", ""},
        {"block-size", "", "auto", "<size>", ""},
        {"min-inline-size", "", "auto", "<size>", ""},
        {"min-block-size", "", "auto", "<size>", ""},
        {"max-inline-size", "", "none", "<max-size>", ""},
        {"max-block-size", "", "none", "<max-size>", ""},
        // 분할(CSS Fragmentation 3)
        {"break-before", "", "auto", "auto | avoid | always | all | avoid-page | page | left | right | recto | verso | avoid-column | column | avoid-region | region", ""},
        {"break-after", "", "auto", "auto | avoid | always | all | avoid-page | page | left | right | recto | verso | avoid-column | column | avoid-region | region", ""},
        {"break-inside", "", "auto", "auto | avoid | avoid-page | avoid-column | avoid-region", ""},
        {"box-decoration-break", "", "slice", "slice | clone", ""},
        // 모양(CSS Shapes 1): float 를 감싸는 줄이 따르는 모양
        {"shape-outside", "", "none", "none | [ <generic-function> | <ident> ]+ | <url>", ""},
        {"shape-margin", "", "0", "<length-percentage [0,inf]>", ""},
        {"shape-image-threshold", "", "0", "<number> | <percentage>", ""},
        // 컨테이너 질의와 격리(CSS Containment 3, CSS Conditional 5): 컨테이너의 크기가 @container 의 조건이 된다
        {"container-type", "", "normal", "normal | size | inline-size", ""},
        {"container-name", "", "none", "none | <custom-ident>+", ""},
        {"content-visibility", "", "visible", "visible | auto | hidden", ""},
        {"contain-intrinsic-width", "", "none", "none | <length [0,inf]> | auto [ none | <length [0,inf]> ]", ""},
        {"contain-intrinsic-height", "", "none", "none | <length [0,inf]> | auto [ none | <length [0,inf]> ]", ""},
        // 스크롤 스냅과 스크롤바(CSS Scroll Snap 1, CSS Overscroll 1, CSS Scrollbars 1): 계산 값만 있다
        {"scroll-snap-type", "", "none", "none | [ x | y | block | inline | both ] [ mandatory | proximity ]?", "P"},
        {"scroll-snap-align", "", "none", "[ none | start | end | center ]{1,2}", "P"},
        {"scroll-snap-stop", "", "normal", "normal | always", "P"},
        {"scroll-padding-top", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-padding-right", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-padding-bottom", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-padding-left", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-margin-top", "", "0", "<length>", "P"},
        {"scroll-margin-right", "", "0", "<length>", "P"},
        {"scroll-margin-bottom", "", "0", "<length>", "P"},
        {"scroll-margin-left", "", "0", "<length>", "P"},
        {"overscroll-behavior-x", "", "auto", "contain | none | auto", "P"},
        {"overscroll-behavior-y", "", "auto", "contain | none | auto", "P"},
        {"scrollbar-width", "", "auto", "auto | thin | none", "P"},
        {"scrollbar-color", "i", "auto", "auto | <color>{2}", "P"},
        {"scrollbar-gutter", "", "auto", "auto | stable && both-edges?", "P"},
        {"touch-action", "", "auto", "auto | none | manipulation | [ pan-x | pan-left | pan-right ] || [ pan-y | pan-up | pan-down ] || pinch-zoom", "P"},
        // 색 구성표(CSS Color Adjust 1)
        {"color-scheme", "i", "normal", "normal | [ light | dark | <custom-ident> ]+ && only?", ""},
        {"forced-color-adjust", "i", "auto", "auto | none | preserve-parsed-font-color", "P"},
        {"print-color-adjust", "i", "economy", "economy | exact", "P"},
        // 텍스트(CSS Text 4, CSS Text Decoration 4)
        {"text-wrap-mode", "i", "wrap", "wrap | nowrap", ""},
        {"text-wrap-style", "i", "auto", "auto | balance | stable | pretty", ""},
        {"hanging-punctuation", "i", "none", "none | [ first || [ force-end | allow-end ] || last ]", ""},
        {"hyphenate-character", "i", "auto", "auto | <string>", "P"},
        {"text-justify", "i", "auto", "auto | none | inter-word | inter-character | distribute", ""},
        {"text-emphasis-style", "i", "none", "none | [ [ filled | open ] || [ dot | circle | double-circle | triangle | sesame ] ] | <string>", ""},
        {"text-emphasis-color", "i", "currentcolor", "<color>", ""},
        {"text-emphasis-position", "i", "over right", "[ over | under ] && [ right | left ]?", ""},
        {"text-underline-position", "i", "auto", "auto | [ from-font | under ] || [ left | right ]", ""},
        {"text-decoration-skip-ink", "i", "auto", "auto | none | all", ""},
        // 글꼴(CSS Fonts 4)
        {"font-variant-ligatures", "i", "normal", "normal | none | [ common-ligatures | no-common-ligatures ] || [ discretionary-ligatures | no-discretionary-ligatures ]"
            + " || [ historical-ligatures | no-historical-ligatures ] || [ contextual | no-contextual ]", "P"},
        {"font-variant-numeric", "i", "normal", "normal | [ lining-nums | oldstyle-nums ] || [ proportional-nums | tabular-nums ]"
            + " || [ diagonal-fractions | stacked-fractions ] || ordinal || slashed-zero", "P"},
        {"font-variant-east-asian", "i", "normal", "normal | [ jis78 | jis83 | jis90 | jis04 | simplified | traditional ] || [ full-width | proportional-width ] || ruby", "P"},
        {"font-variant-position", "i", "normal", "normal | sub | super", "P"},
        {"font-variant-alternates", "i", "normal", "normal | [ stylistic(<custom-ident>) || historical-forms || styleset(<custom-ident>#) || character-variant(<custom-ident>#)"
            + " || swash(<custom-ident>) || ornaments(<custom-ident>) || annotation(<custom-ident>) ]", "P"},
        {"font-synthesis-weight", "i", "auto", "auto | none", ""},
        {"font-optical-sizing", "i", "auto", "auto | none", "P"},
        {"font-variation-settings", "i", "normal", "normal | [ <string> <number> ]#", "P"},
        {"font-language-override", "i", "normal", "normal | <string>", "P"},
        // 줄 수 제한(CSS Overflow 4 line-clamp 와 -webkit-line-clamp): 블록 컨테이너의 줄을 N 줄로 자르고 마지막 줄 끝에 줄임표를 붙인다
        {"line-clamp", "", "none", "none | <integer [1,inf]>", ""},
        // 쪽 서식(CSS Paged Media 3): @page 서술자와 page 속성. 인쇄가 없으므로 파싱과 계산 값만 있다
        {"size", "", "auto", "auto | <length [0,inf]>{1,2} | [ a5 | a4 | a3 | b5 | b4 | jis-b5 | jis-b4 | letter | legal | ledger ] || [ portrait | landscape ]", "P"},
        {"page-orientation", "", "upright", "upright | rotate-left | rotate-right", "P"},
        {"marks", "", "none", "none | [ crop || cross ]", "P"},
        {"bleed", "", "auto", "auto | <length>", "P"},
        {"page", "", "auto", "auto | <custom-ident>", "P"},
        // 배경 필터(Filter Effects 2 backdrop-filter): 요소 뒤에 이미 그려진 내용에 필터를 적용한다
        {"backdrop-filter", "", "none", "none | [ <filter-function> | <url> ]+", ""},
        // 처음 글자 키우기(CSS Inline Layout 3 initial-letter)
        {"initial-letter", "", "normal", "normal | <number [0,inf]> [ <integer [1,inf]> | drop | raise ]?", ""},
        // 마스크 테두리(CSS Masking 1 mask-border): border-image 와 같은 아홉 조각으로 마스크를 만든다
        {"mask-border-source", "", "none", "<bg-image>", ""},
        {"mask-border-mode", "", "alpha", "luminance | alpha", ""},
        {"mask-border-slice", "", "0", "[ <number [0,inf]> | <percentage [0,inf]> ]{1,4} && fill?", ""},
        {"mask-border-width", "", "auto", "[ <length-percentage [0,inf]> | <number [0,inf]> | auto ]{1,4}", ""},
        {"mask-border-outset", "", "0", "[ <length [0,inf]> | <number [0,inf]> ]{1,4}", ""},
        {"mask-border-repeat", "", "stretch", "[ stretch | repeat | round | space ]{1,2}", ""},
        // 모션 경로(CSS Motion Path 1): 요소를 경로를 따라 놓는다
        {"offset-path", "", "none", "none | [ [ <generic-function> | <url> ] || [ content-box | padding-box | border-box | margin-box | fill-box | stroke-box | view-box ] ]", ""},
        {"offset-distance", "", "0", "<length-percentage>", ""},
        {"offset-rotate", "", "auto", "[ auto | reverse ] || <angle>", ""},
        {"offset-anchor", "", "auto", "auto | <position>", ""},
        {"offset-position", "", "normal", "normal | auto | <position>", ""},
        // 논리 scroll-margin/scroll-padding(CSS Scroll Snap 1): 쓰기 모드와 방향에 따라 물리 변으로 옮겨 쓴다(LogicalProps)
        {"scroll-margin-block-start", "", "0", "<length>", "P"},
        {"scroll-margin-block-end", "", "0", "<length>", "P"},
        {"scroll-margin-inline-start", "", "0", "<length>", "P"},
        {"scroll-margin-inline-end", "", "0", "<length>", "P"},
        {"scroll-padding-block-start", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-padding-block-end", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-padding-inline-start", "", "auto", "auto | <lp-nn>", "P"},
        {"scroll-padding-inline-end", "", "auto", "auto | <lp-nn>", "P"},
        // font-synthesis 의 나머지 개별 속성(CSS Fonts 4: font-synthesis 는 이 네 개별 속성의 단축 속성이다)
        {"font-synthesis-style", "i", "auto", "auto | none", ""},
        {"font-synthesis-small-caps", "i", "auto", "auto | none", ""},
        {"font-synthesis-position", "i", "auto", "auto | none", ""},
        // 앵커 위치(CSS Anchor Positioning 1): anchor-name 이 앵커를 만들고 position-anchor 가 기본 앵커를 정한다. anchor()/anchor-size() 는 오프셋과 크기 속성이 받는다
        {"anchor-name", "", "none", "none | <dashed-ident>#", ""},
        {"position-anchor", "", "normal", "normal | none | auto | match-parent | <dashed-ident>", ""},
        // 보간할 수 없는(이산) 속성도 전환할지 정한다(CSS Transitions 2): allow-discrete 이면 display 와 같은 속성도 전환한다.
        {"transition-behavior", "", "normal", "[ normal | allow-discrete ]#", ""},
        // 앵커 상자와 포함 블록이 만드는 3x3 격자의 영역에 배치한다(CSS Anchor Positioning 1 3.1 절). 값은 한두 개의 키워드이다.
        {"position-area", "", "none", "none | <area-value>", ""},
        // 이미지 방향(CSS Images 3 5.1절): from-image 는 EXIF 방향 등을 적용하고 none 은 저장된 그대로 보여 준다(각도/flip 값은 선택 사항이며 폐기 예정이라 받지 않는다)
        {"image-orientation", "i", "from-image", "from-image | none", ""},
        // 위치 대안과 조건부 숨김(CSS Anchor Positioning 1 6 절): 넘치면 시도할 대안 목록, 대안의 정렬 순서, 넘침/앵커 상태에 따른 숨김, 앵커 이름의 범위
        {"position-try-fallbacks", "", "none", "none | [ [ <dashed-ident> || <try-tactic> ] | <area-value> ]#", ""},
        {"position-try-order", "", "normal", "normal | most-width | most-height | most-block-size | most-inline-size", ""},
        {"position-visibility", "", "anchors-visible", "always | [ anchors-valid || anchors-visible || no-overflow ]", ""},
        {"anchor-scope", "", "none", "none | all | <dashed-ident>#", ""},
        // 논리 contain-intrinsic 크기(CSS Box Sizing 4 5.2 절, id 316~317): 쓰기 모드에 따라 contain-intrinsic-width/height 에 쓴다
        {"contain-intrinsic-inline-size", "", "none", "none | <length [0,inf]> | auto [ none | <length [0,inf]> ]", ""},
        {"contain-intrinsic-block-size", "", "none", "none | <length [0,inf]> | auto [ none | <length [0,inf]> ]", ""},
    };
}
