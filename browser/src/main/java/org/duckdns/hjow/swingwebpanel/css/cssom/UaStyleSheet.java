package org.duckdns.hjow.swingwebpanel.css.cssom;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;

/**
 * The user agent default style sheet: the rendering defaults the HTML Standard prescribes for HTML elements (which elements are blocks, lists, tables,
 * what is hidden; margins and sizes of headings, lists and tables; colors of links; bold, italic, underline and monospace elements; the look of form
 * controls and fieldsets), with the additions of browsers where the standard is silent. A second small sheet holds the rules that only apply to
 * documents in quirks mode and a third one the rules of HTML documents only (forms in tables). All are written as Java string constants so that they travel with the class files (no resource files are needed) and are
 * parsed once, lazily. Type selectors only match HTML elements because the sheet declares the XHTML namespace as the default namespace.
 * <p>
 * 사용자 에이전트 기본 스타일 시트: HTML 표준이 HTML 요소에 정한 렌더링 기본값(어떤 요소가 블록, 목록, 표인지, 무엇이 숨겨지는지, 제목과 목록과 표의 여백과 크기, 링크 색,
 * 굵은/기울임/밑줄/고정폭 요소, 폼 컨트롤과 fieldset 의 모양)과, 표준이 말하지 않은 곳의 브라우저 보완. 작은 두 번째 시트에는 호환 모드 문서에만, 세 번째 시트에는
 * HTML 문서에만(표 안의 폼) 적용되는 규칙이 있다. 모두 Java 문자열 상수로 써서 클래스 파일과 함께 다니며(리소스 파일 불필요) 한 번만 지연 파싱한다. 시트가 XHTML 네임스페이스를 기본 네임스페이스로 선언하므로
 * 타입 선택자는 HTML 요소에만 일치한다.
 */
public final class UaStyleSheet {

    /** The default style sheet text for all documents. / 모든 문서의 기본 스타일 시트 문자열. */
    static final String TEXT = "@namespace url(http://www.w3.org/1999/xhtml);\n"
            + "html, body { display: block }\n"
            + "[hidden], area, base, basefont, datalist, head, link, meta, noembed, noframes, param, rp, script, source, style, template, track, title"
            + " { display: none }\n"
            + "embed[hidden] { display: inline; height: 0; width: 0 }\n"
            + "input[type=hidden i] { display: none !important }\n"
            + "@media (scripting: enabled) { audio:not([controls]) { display: none !important } }\n"
            + "dialog:not([open]) { display: none }\n"
            + "dialog { position: absolute; inset-inline-start: 0; inset-inline-end: 0; width: fit-content; height: fit-content; margin: auto; border: solid;"
            + " padding: 1em; background-color: Canvas; color: CanvasText }\n"
            + "dialog:modal { position: fixed; overflow: auto; inset-block: 0; max-width: calc(100% - 6px - 2em); max-height: calc(100% - 6px - 2em) }\n"
            + "::backdrop { position: fixed; inset: 0 }\n"
            + "dialog::backdrop { background: rgba(0,0,0,0.1) }\n"
            + "address, blockquote, center, dialog, div, figure, figcaption, footer, form, header, hr, legend, listing, main, p, plaintext, pre, search, xmp,"
            + " article, aside, h1, h2, h3, h4, h5, h6, hgroup, nav, section, dir, dd, dl, dt, menu, ol, ul, details, summary, fieldset, optgroup { display: block }\n"
            + "li { display: list-item }\n"
            + "marquee { display: inline-block; text-align: initial; overflow: hidden !important }\n"
            + "slot { display: contents }\n"
            + "table { display: table } caption { display: table-caption } colgroup { display: table-column-group } col { display: table-column }\n"
            + "thead { display: table-header-group } tbody { display: table-row-group } tfoot { display: table-footer-group }\n"
            + "tr { display: table-row } td, th { display: table-cell }\n"
            + "input, select, button, textarea, progress, meter { display: inline-block }\n"
            + "ruby { display: ruby } rt { display: ruby-text }\n"
            + "body { margin: 8px }\n"
            + "p, dl { margin-top: 1em; margin-bottom: 1em }\n"
            + "blockquote, figure { margin: 1em 40px }\n"
            + "listing, plaintext, pre, xmp { margin: 1em 0 }\n"
            + "address, i, cite, em, var, dfn { font-style: italic }\n"
            + "h1 { margin: 0.67em 0; font-size: 2em } h2 { margin: 0.83em 0; font-size: 1.5em } h3 { margin: 1em 0; font-size: 1.17em }\n"
            + "h4 { margin: 1.33em 0; font-size: 1em } h5 { margin: 1.67em 0; font-size: 0.83em } h6 { margin: 2.33em 0; font-size: 0.67em }\n"
            + "h1, h2, h3, h4, h5, h6, th { font-weight: bold } b, strong { font-weight: bolder }\n"
            + "dd { margin-left: 40px }\n"
            + "dir, menu, ol, ul { margin: 1em 0; padding-left: 40px }\n"
            + ":is(dir, dl, menu, ol, ul) :is(dir, dl, menu, ol, ul) { margin-top: 0; margin-bottom: 0 }\n"
            + "dir, menu, ul { list-style-type: disc } ol { list-style-type: decimal }\n"
            + ":is(dir, dl, menu, ol, ul) :is(dir, menu, ul) { list-style-type: circle }\n"
            + ":is(dir, menu, ol, ul) :is(dir, menu, ol, ul) :is(dir, menu, ul) { list-style-type: square }\n"
            + "table { box-sizing: border-box; border-spacing: 2px; border-collapse: separate; text-indent: initial }\n"
            + "td, th { padding: 1px }\n"
            + "th { text-align: -internal-center }\n"
            + "thead, tbody, tfoot, table > tr { vertical-align: middle }\n"
            + "tr, td, th { vertical-align: inherit }\n"
            + "table, td, th { border-color: gray } thead, tbody, tfoot, tr { border-color: inherit }\n"
            + tableRuleColors() + " { border-color: black }\n"
            + "hr { color: gray; border-style: inset; border-width: 1px; margin: 0.5em auto; overflow: hidden }\n"
            + ":any-link { color: #0000ee; text-decoration: underline; cursor: pointer }\n"
            + ":visited { color: #551a8b }\n"
            + ":focus-visible { outline: auto 1px }\n"
            + "u, ins { text-decoration: underline } s, strike, del { text-decoration: line-through } blink { text-decoration: blink }\n"
            + "small { font-size: smaller } big { font-size: larger }\n"
            + "sub { vertical-align: sub; font-size: smaller } sup { vertical-align: super; font-size: smaller }\n"
            + "code, kbd, samp, tt, pre, listing, xmp, plaintext { font-family: monospace }\n"
            + "pre, listing, xmp, plaintext { white-space: pre }\n"
            + "nobr { white-space: nowrap } nobr wbr { white-space: normal }\n"
            + "mark { background-color: yellow; color: black }\n"
            + "q::before { content: open-quote } q::after { content: close-quote }\n"
            + "abbr[title], acronym[title] { text-decoration: underline dotted }\n"
            + "bdi, output { unicode-bidi: isolate } bdo { unicode-bidi: bidi-override }\n"
            + "[dir=ltr i] { direction: ltr } [dir=rtl i] { direction: rtl }\n"
            + "video { object-fit: contain }\n"
            + "iframe { border: 2px inset }\n"
            + "frameset { display: grid; width: 100vw; height: 100vh; margin: 0; padding: 0; border: 0; background-color: gray; box-sizing: border-box;"
            + " grid-template-columns: minmax(0,1fr); grid-template-rows: minmax(0,1fr); grid-auto-rows: 0px; grid-auto-columns: 0px }\n"
            + "frameset > frameset { width: 100%; height: 100%; min-width: 0; min-height: 0 }\n"
            + "frame { display: block; width: 100%; height: 100%; min-width: 0; min-height: 0; margin: 0; padding: 0; border: 0 solid gray; box-sizing: border-box; background-color: white }\n"
            + "fieldset { margin-left: 2px; margin-right: 2px; padding: 0.35em 0.625em 0.75em; border: 2px groove threedface }\n"
            + "legend { padding: 0 2px }\n"
            + "input, textarea, select, button { margin: 0; font-size: 13.3333px; font-family: sans-serif; letter-spacing: normal; word-spacing: normal;"
            + " text-transform: none; text-indent: 0; text-shadow: none; text-align: start; color: fieldtext; line-height: normal }\n"
            + "input, textarea, select { background-color: field; border: 2px inset #767676; padding: 1px 2px }\n"
            + "button, input[type=button i], input[type=submit i], input[type=reset i] { background-color: buttonface; color: buttontext;"
            + " border: 2px outset buttonborder; padding: 1px 6px; text-align: center }\n"
            + "textarea { white-space: pre-wrap; overflow: auto; font-family: monospace; padding: 2px; border: 1px solid #767676 }\n"
            + "select { border: 1px solid #767676; border-radius: 0; padding: 0 }\n"
            + "input[type=radio i], input[type=checkbox i], input[type=reset i], input[type=button i], input[type=submit i], select, button {"
            + " box-sizing: border-box }\n"
            + "input:disabled, textarea:disabled, select:disabled { color: graytext; background-color: rgba(239, 239, 239, 0.3) }\n"
            + "button:disabled, input[type=button i]:disabled, input[type=submit i]:disabled, input[type=reset i]:disabled { background-color: buttonface; color: graytext }\n"
            + "input[type=checkbox i], input[type=radio i] { padding: 0; border: none; background-color: transparent; margin: 3px 3px 3px 4px;"
            + " box-sizing: border-box }\n"
            + "input[type=range i] { padding: 0; border: none; background-color: transparent; margin: 2px }\n"
            + "input[type=color i] { width: 50px; height: 27px; box-sizing: border-box; padding: 1px 2px; border: 1px solid buttonborder;"
            + " background-color: buttonface }\n"
            + "input[type=file i] { padding: 0; border: none; background-color: transparent; color: inherit }\n"
            + "input[type=image i] { padding: 0; border: none; background-color: transparent }\n"
            + "progress, meter { vertical-align: -0.2em; border: none; padding: 0; background-color: transparent }\n"
            + "::placeholder { color: #757575 }\n"
            + "details > summary:first-of-type { display: list-item; list-style-type: disclosure-closed; list-style-position: inside }\n"
            + "details[open] > summary:first-of-type { list-style-type: disclosure-open }\n";

    /** The rules that apply to documents in quirks mode only. / 호환 모드 문서에만 적용되는 규칙. */
    static final String QUIRKS_TEXT = "@namespace url(http://www.w3.org/1999/xhtml);\n"
            + "form { margin-bottom: 1em }\n"
            + "input:not([type=image i]), textarea { box-sizing: border-box }\n"
            + "table { font-size: medium; font-weight: normal; font-style: normal; font-variant: normal; line-height: normal; white-space: normal;"
            + " text-align: start }\n"
            + "img[align=left i] { margin-right: 3px } img[align=right i] { margin-left: 3px }\n";

    /**
     * The rules that apply to HTML documents only (not to XML documents that use the XHTML namespace): the forms that are children of a
     * table, a row group or a row are not displayed, irrespective of the CSS rules of the page (HTML5 2014 section 10.3.9).
     * <p>
     * HTML 문서에만 적용되는 규칙(XHTML 네임스페이스를 쓰는 XML 문서에는 적용하지 않음): 표, 행 그룹, 행의 자식인 폼은 페이지의 CSS 와 상관없이 표시하지 않는다(HTML5 2014 §10.3.9).
     */
    static final String HTML_ONLY_TEXT = "@namespace url(http://www.w3.org/1999/xhtml);\n"
            + "table > form, thead > form, tbody > form, tfoot > form, tr > form { display: none !important }\n";

    /** The parsed standards-mode sheet. / 파싱한 표준 모드 시트. */
    private static StyleSheet standards;

    /** The parsed quirks-mode additions. / 파싱한 호환 모드 추가분. */
    private static StyleSheet quirks;

    /** The parsed rules of HTML documents. / 파싱한 HTML 문서 전용 규칙. */
    private static StyleSheet htmlOnly;

    /**
     * Private constructor: static factory class.
     * <p>
     * 비공개 생성자: 정적 팩토리 클래스이다.
     */
    private UaStyleSheet() {
    }

    /**
     * Builds the selector list of HTML5 2014 section 10.3.9 that gives black borders: the tables with a <code>rules</code> or
     * <code>frame</code> attribute and the cells of the tables with a <code>rules</code> attribute (written out as the section does, with
     * child combinators, so a nested table is not matched through its parent).
     * <p>
     * HTML5 2014 §10.3.9 의 검은 border 를 주는 선택자 목록을 만든다: <code>rules</code> 나 <code>frame</code> 속성이 있는 표와 <code>rules</code> 속성이 있는 표의 셀
     * (규격처럼 자식 결합자로 풀어 써서 중첩된 표가 부모를 통해 일치하지 않는다).
     *
     * @return the selector list without a block / 블록 없는 선택자 목록
     */
    private static String tableRuleColors() {
        String[] rules = {"none", "groups", "rows", "cols", "all"};
        String[] frames = {"void", "above", "below", "hsides", "lhs", "rhs", "vsides", "box", "border"};
        String[] paths = {"", "thead > ", "tbody > ", "tfoot > "};
        StringBuilder sb = new StringBuilder();
        // rules 값마다 표 자체의 선택자를 쓴다.
        for (int i = 0; i < rules.length; i++) {
            sb.append("table[rules=").append(rules[i]).append(" i], ");
        }
        // frame 값마다 표 자체의 선택자를 쓴다.
        for (int i = 0; i < frames.length; i++) {
            sb.append("table[frame=").append(frames[i]).append(" i], ");
        }
        // rules 값마다 직접 행 또는 행 그룹 안의 행의 셀 선택자를 쓴다.
        for (int i = 0; i < rules.length; i++) {
            // 표 바로 아래와 세 가지 행 그룹 아래의 행을 차례로 쓴다.
            for (int p = 0; p < paths.length; p++) {
                sb.append("table[rules=").append(rules[i]).append(" i] > ").append(paths[p]).append("tr > td, ");
                sb.append("table[rules=").append(rules[i]).append(" i] > ").append(paths[p]).append("tr > th");
                if (i < rules.length - 1 || p < paths.length - 1) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns the text of the user agent style sheet that applies to every HTML document.
     * <p>
     * 모든 HTML 문서에 적용되는 사용자 에이전트 스타일 시트의 문자열을 반환한다.
     *
     * @return the CSS text / CSS 문자열
     */
    public static String standardsText() {
        return TEXT;
    }

    /**
     * Returns the user agent style sheet that applies to every HTML document.
     * <p>
     * 모든 HTML 문서에 적용되는 사용자 에이전트 스타일 시트를 반환한다.
     *
     * @return the shared sheet / 공유 시트
     */
    public static synchronized StyleSheet standards() {
        if (standards == null) {
            standards = StyleSheet.parse(TEXT, null, StyleSheet.ORIGIN_USER_AGENT, null, new CssDiagnostics());
        }
        return standards;
    }

    /**
     * Returns the additional user agent rules for documents in quirks mode.
     * <p>
     * 호환 모드 문서를 위한 추가 사용자 에이전트 규칙을 반환한다.
     *
     * @return the shared sheet / 공유 시트
     */
    public static synchronized StyleSheet quirks() {
        if (quirks == null) {
            quirks = StyleSheet.parse(QUIRKS_TEXT, null, StyleSheet.ORIGIN_USER_AGENT, null, new CssDiagnostics());
        }
        return quirks;
    }

    /**
     * Returns the additional user agent rules for HTML documents (as opposed to XML documents).
     * <p>
     * HTML 문서(XML 문서가 아닌)를 위한 추가 사용자 에이전트 규칙을 반환한다.
     *
     * @return the shared sheet / 공유 시트
     */
    public static synchronized StyleSheet htmlOnly() {
        if (htmlOnly == null) {
            htmlOnly = StyleSheet.parse(HTML_ONLY_TEXT, null, StyleSheet.ORIGIN_USER_AGENT, null, new CssDiagnostics());
        }
        return htmlOnly;
    }
}
