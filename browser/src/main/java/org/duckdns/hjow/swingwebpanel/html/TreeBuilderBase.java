package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The data structures and primitive operations of the tree construction stage of the HTML parser ("tree construction", 13.2.6):
 * the stack of open elements with its scope searches, the list of active formatting elements (Noah's Ark clause, reconstruction), the
 * "appropriate place for inserting a node" with foster parenting and template contents, the creation of elements from tokens (attribute
 * adjustment for SVG and MathML, template contents, script flags, form association), insertion of text (batched so that long text becomes
 * one text node), the adoption agency algorithm, "reset the insertion mode appropriately" and the notification of the
 * {@link ParserListener}. The insertion mode rules themselves are in the subclass {@link HtmlTreeBuilder}.
 * <p>
 * The stack stores, besides the elements, small integers for their tag ids ({@link HtmlTags}) and namespaces so that scope searches
 * never compare strings.
 * <p>
 * HTML 파서 트리 생성 단계("tree construction", 13.2.6)의 자료 구조와 기본 연산: 범위 검색이 있는 열린 요소 스택, 활성 서식 요소 목록(Noah's Ark 규칙,
 * 재구성), foster parenting 과 template contents 를 반영한 "appropriate place for inserting a node", 토큰으로부터 요소 만들기(SVG/MathML 속성 보정,
 * template contents, script 플래그, 폼 연결), 텍스트 삽입(긴 텍스트가 텍스트 노드 하나가 되도록 모아서 처리), adoption agency 알고리즘,
 * "reset the insertion mode appropriately", {@link ParserListener} 통지. 삽입 모드의 규칙 자체는 하위 클래스 {@link HtmlTreeBuilder} 에 있다.
 * <p>
 * 스택은 요소와 함께 태그 번호({@link HtmlTags})와 네임스페이스의 작은 정수도 저장하므로 범위 검색에서 문자열을 비교하지 않는다.
 */
abstract class TreeBuilderBase {

    /**
     * Insertion mode: initial.
     * <p>
     * 삽입 모드: initial.
     */
    static final int INITIAL = 0;

    /**
     * Insertion mode: before html.
     * <p>
     * 삽입 모드: before html.
     */
    static final int BEFORE_HTML = 1;

    /**
     * Insertion mode: before head.
     * <p>
     * 삽입 모드: before head.
     */
    static final int BEFORE_HEAD = 2;

    /**
     * Insertion mode: in head.
     * <p>
     * 삽입 모드: in head.
     */
    static final int IN_HEAD = 3;

    /**
     * Insertion mode: in head noscript.
     * <p>
     * 삽입 모드: in head noscript.
     */
    static final int IN_HEAD_NOSCRIPT = 4;

    /**
     * Insertion mode: after head.
     * <p>
     * 삽입 모드: after head.
     */
    static final int AFTER_HEAD = 5;

    /**
     * Insertion mode: in body.
     * <p>
     * 삽입 모드: in body.
     */
    static final int IN_BODY = 6;

    /**
     * Insertion mode: text.
     * <p>
     * 삽입 모드: text.
     */
    static final int TEXT = 7;

    /**
     * Insertion mode: in table.
     * <p>
     * 삽입 모드: in table.
     */
    static final int IN_TABLE = 8;

    /**
     * Insertion mode: in table text.
     * <p>
     * 삽입 모드: in table text.
     */
    static final int IN_TABLE_TEXT = 9;

    /**
     * Insertion mode: in caption.
     * <p>
     * 삽입 모드: in caption.
     */
    static final int IN_CAPTION = 10;

    /**
     * Insertion mode: in column group.
     * <p>
     * 삽입 모드: in column group.
     */
    static final int IN_COLUMN_GROUP = 11;

    /**
     * Insertion mode: in table body.
     * <p>
     * 삽입 모드: in table body.
     */
    static final int IN_TABLE_BODY = 12;

    /**
     * Insertion mode: in row.
     * <p>
     * 삽입 모드: in row.
     */
    static final int IN_ROW = 13;

    /**
     * Insertion mode: in cell.
     * <p>
     * 삽입 모드: in cell.
     */
    static final int IN_CELL = 14;

    /**
     * Insertion mode: in select.
     * <p>
     * 삽입 모드: in select.
     */
    static final int IN_SELECT = 15;

    /**
     * Insertion mode: in select in table.
     * <p>
     * 삽입 모드: in select in table.
     */
    static final int IN_SELECT_IN_TABLE = 16;

    /**
     * Insertion mode: in template.
     * <p>
     * 삽입 모드: in template.
     */
    static final int IN_TEMPLATE = 17;

    /**
     * Insertion mode: after body.
     * <p>
     * 삽입 모드: after body.
     */
    static final int AFTER_BODY = 18;

    /**
     * Insertion mode: in frameset.
     * <p>
     * 삽입 모드: in frameset.
     */
    static final int IN_FRAMESET = 19;

    /**
     * Insertion mode: after frameset.
     * <p>
     * 삽입 모드: after frameset.
     */
    static final int AFTER_FRAMESET = 20;

    /**
     * Insertion mode: after after body.
     * <p>
     * 삽입 모드: after after body.
     */
    static final int AFTER_AFTER_BODY = 21;

    /**
     * Insertion mode: after after frameset.
     * <p>
     * 삽입 모드: after after frameset.
     */
    static final int AFTER_AFTER_FRAMESET = 22;

    /**
     * Namespace code: HTML.
     * <p>
     * 네임스페이스 코드: HTML.
     */
    static final int NS_HTML = 0;

    /**
     * Namespace code: SVG.
     * <p>
     * 네임스페이스 코드: SVG.
     */
    static final int NS_SVG = 1;

    /**
     * Namespace code: MathML.
     * <p>
     * 네임스페이스 코드: MathML.
     */
    static final int NS_MATHML = 2;

    /**
     * Namespace URIs by namespace code.
     * <p>
     * 네임스페이스 코드별 URI.
     */
    static final String[] NS_URI = {Namespaces.HTML, Namespaces.SVG, Namespaces.MATHML};

    /**
     * Attribute adjustment kind: none (HTML element).
     * <p>
     * 속성 보정 종류: 없음(HTML 요소).
     */
    static final int ATTRS_HTML = 0;

    /**
     * Attribute adjustment kind: SVG attributes and foreign attributes.
     * <p>
     * 속성 보정 종류: SVG 속성과 외부 속성.
     */
    static final int ATTRS_SVG = 1;

    /**
     * Attribute adjustment kind: MathML attributes and foreign attributes.
     * <p>
     * 속성 보정 종류: MathML 속성과 외부 속성.
     */
    static final int ATTRS_MATHML = 2;

    /**
     * Scope kind: default scope.
     * <p>
     * 범위 종류: 기본 범위.
     */
    static final int SCOPE_DEFAULT = 0;

    /**
     * Scope kind: list item scope.
     * <p>
     * 범위 종류: list item 범위.
     */
    static final int SCOPE_LIST = 1;

    /**
     * Scope kind: button scope.
     * <p>
     * 범위 종류: button 범위.
     */
    static final int SCOPE_BUTTON = 2;

    /**
     * Scope kind: table scope.
     * <p>
     * 범위 종류: table 범위.
     */
    static final int SCOPE_TABLE = 3;

    /**
     * Scope kind: select scope.
     * <p>
     * 범위 종류: select 범위.
     */
    static final int SCOPE_SELECT = 4;

    /**
     * SVG attribute names (lower case) to their correct case.
     * <p>
     * SVG 속성 이름(소문자)에서 올바른 대소문자로의 표.
     */
    private static final Map<String, String> SVG_ATTRIBUTES = new HashMap<String, String>();

    /**
     * SVG element names (lower case) to their correct case.
     * <p>
     * SVG 요소 이름(소문자)에서 올바른 대소문자로의 표.
     */
    private static final Map<String, String> SVG_ELEMENTS = new HashMap<String, String>();

    /**
     * Foreign attribute names to {namespace, qualified name}.
     * <p>
     * 외부 속성 이름에서 {네임스페이스, 정규화된 이름}으로의 표.
     */
    private static final Map<String, String[]> FOREIGN_ATTRIBUTES = new HashMap<String, String[]>();

    static {
        String[] svgAttrs = {"attributeName", "attributeType", "baseFrequency", "baseProfile", "calcMode", "clipPathUnits", "diffuseConstant",
                "edgeMode", "filterUnits", "glyphRef", "gradientTransform", "gradientUnits", "kernelMatrix", "kernelUnitLength", "keyPoints",
                "keySplines", "keyTimes", "lengthAdjust", "limitingConeAngle", "markerHeight", "markerUnits", "markerWidth", "maskContentUnits",
                "maskUnits", "numOctaves", "pathLength", "patternContentUnits", "patternTransform", "patternUnits", "pointsAtX", "pointsAtY",
                "pointsAtZ", "preserveAlpha", "preserveAspectRatio", "primitiveUnits", "refX", "refY", "repeatCount", "repeatDur",
                "requiredExtensions", "requiredFeatures", "specularConstant", "specularExponent", "spreadMethod", "startOffset", "stdDeviation",
                "stitchTiles", "surfaceScale", "systemLanguage", "tableValues", "targetX", "targetY", "textLength", "viewBox", "viewTarget",
                "xChannelSelector", "yChannelSelector", "zoomAndPan"};
        // 소문자 이름을 올바른 대소문자 이름에 대응시킨다.
        for (int i = 0; i < svgAttrs.length; i++) {
            SVG_ATTRIBUTES.put(svgAttrs[i].toLowerCase(java.util.Locale.ENGLISH), svgAttrs[i]);
        }
        String[] svgElems = {"altGlyph", "altGlyphDef", "altGlyphItem", "animateColor", "animateMotion", "animateTransform", "clipPath", "feBlend",
                "feColorMatrix", "feComponentTransfer", "feComposite", "feConvolveMatrix", "feDiffuseLighting", "feDisplacementMap",
                "feDistantLight", "feDropShadow", "feFlood", "feFuncA", "feFuncB", "feFuncG", "feFuncR", "feGaussianBlur", "feImage", "feMerge",
                "feMergeNode", "feMorphology", "feOffset", "fePointLight", "feSpecularLighting", "feSpotLight", "feTile", "feTurbulence",
                "foreignObject", "glyphRef", "linearGradient", "radialGradient", "textPath"};
        // 소문자 요소 이름을 올바른 대소문자 이름에 대응시킨다.
        for (int i = 0; i < svgElems.length; i++) {
            SVG_ELEMENTS.put(svgElems[i].toLowerCase(java.util.Locale.ENGLISH), svgElems[i]);
        }
        String[] xlink = {"actuate", "arcrole", "href", "role", "show", "title", "type"};
        // xlink 속성은 XLink 네임스페이스로 보낸다.
        for (int i = 0; i < xlink.length; i++) {
            FOREIGN_ATTRIBUTES.put("xlink:" + xlink[i], new String[] {Namespaces.XLINK, "xlink:" + xlink[i]});
        }
        FOREIGN_ATTRIBUTES.put("xml:lang", new String[] {Namespaces.XML, "xml:lang"});
        FOREIGN_ATTRIBUTES.put("xml:space", new String[] {Namespaces.XML, "xml:space"});
        FOREIGN_ATTRIBUTES.put("xmlns", new String[] {Namespaces.XMLNS, "xmlns"});
        FOREIGN_ATTRIBUTES.put("xmlns:xlink", new String[] {Namespaces.XMLNS, "xmlns:xlink"});
    }

    /**
     * An entry of the list of active formatting elements: an element with the token that created it, or a marker (element is null).
     * <p>
     * 활성 서식 요소 목록의 항목: 요소와 그것을 만든 토큰, 또는 표시(marker, 요소가 null).
     */
    static final class FormatEntry {
        /**
         * The element, or null for a marker.
         * <p>
         * 요소. 표시이면 null.
         */
        Element element;

        /**
         * The token that created the element.
         * <p>
         * 요소를 만든 토큰.
         */
        final HtmlToken token;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param element element or null for a marker / 요소 또는 표시이면 null
         * @param token   creating token / 만든 토큰
         */
        FormatEntry(Element element, HtmlToken token) {
            this.element = element;
            this.token = token;
        }
    }

    /**
     * The document that nodes are created in.
     * <p>
     * 노드를 만드는 문서.
     */
    final Document doc;

    /**
     * The tokenizer.
     * <p>
     * 토큰화기.
     */
    final HtmlTokenizer tokenizer;

    /**
     * Error collector.
     * <p>
     * 오류 수집기.
     */
    final ParseErrorLog log;

    /**
     * Listener or null.
     * <p>
     * 리스너 또는 null.
     */
    final ParserListener listener;

    /**
     * Parser control handed to the listener.
     * <p>
     * 리스너에 건네는 파서 제어.
     */
    final ParserControl control;

    /**
     * The host that decides about encoding changes, or null.
     * <p>
     * 인코딩 변경을 결정하는 호스트 또는 null.
     */
    final ParserHost host;

    /**
     * True if the "parser scripting" flag is set: noscript content is raw text.
     * <p>
     * "parser scripting" 플래그가 켜져 있으면 true: noscript 내용이 원문 텍스트가 된다.
     */
    final boolean scripting;

    /**
     * True when the document is an iframe srcdoc document (no quirks mode detection).
     * <p>
     * 문서가 iframe srcdoc 문서이면 true(quirks 모드 판정을 하지 않는다).
     */
    boolean srcdoc;

    /**
     * The fragment context element, or null when a whole document is parsed.
     * <p>
     * 조각 파싱의 컨텍스트 요소. 문서 전체를 파싱하면 null.
     */
    Element contextElement;

    /**
     * Tag id of the context element.
     * <p>
     * 컨텍스트 요소의 태그 번호.
     */
    int contextId;

    /**
     * Namespace code of the context element.
     * <p>
     * 컨텍스트 요소의 네임스페이스 코드.
     */
    int contextNs;

    /**
     * True while parsing a fragment.
     * <p>
     * 조각을 파싱하는 동안 true.
     */
    boolean fragment;

    /**
     * Current insertion mode.
     * <p>
     * 현재 삽입 모드.
     */
    int mode = INITIAL;

    /**
     * The original insertion mode (for the text and table text modes).
     * <p>
     * 원래 삽입 모드(text 와 table text 모드용).
     */
    int originalMode = INITIAL;

    /**
     * The stack of template insertion modes.
     * <p>
     * template 삽입 모드 스택.
     */
    int[] templateModes = new int[8];

    /**
     * Number of entries of the template insertion mode stack.
     * <p>
     * template 삽입 모드 스택의 항목 수.
     */
    int templateCount;

    /**
     * The stack of open elements (index 0 is the root html element).
     * <p>
     * 열린 요소 스택(0번이 루트 html 요소).
     */
    Element[] stackEl = new Element[64];

    /**
     * Tag ids of the stack entries.
     * <p>
     * 스택 항목의 태그 번호.
     */
    int[] stackId = new int[64];

    /**
     * Namespace codes of the stack entries.
     * <p>
     * 스택 항목의 네임스페이스 코드.
     */
    int[] stackNs = new int[64];

    /**
     * Number of stack entries.
     * <p>
     * 스택 항목 수.
     */
    int stackSize;

    /**
     * The list of active formatting elements.
     * <p>
     * 활성 서식 요소 목록.
     */
    final List<FormatEntry> formatting = new ArrayList<FormatEntry>();

    /**
     * The head element pointer.
     * <p>
     * head 요소 포인터.
     */
    Element headPointer;

    /**
     * The form element pointer.
     * <p>
     * form 요소 포인터.
     */
    Element formPointer;

    /**
     * The frameset-ok flag.
     * <p>
     * frameset-ok 플래그.
     */
    boolean framesetOk = true;

    /**
     * The foster parenting flag.
     * <p>
     * foster parenting 플래그.
     */
    boolean fosterParenting;

    /**
     * True once a script was handed to the listener (the parse can no longer be restarted with another encoding).
     * <p>
     * 스크립트를 리스너에 넘긴 뒤에는 true(다른 인코딩으로 파싱을 다시 시작할 수 없다).
     */
    boolean scriptsDispatched;

    /**
     * True after "stop parsing".
     * <p>
     * "stop parsing" 이후 true.
     */
    boolean stopped;

    /**
     * Offset of the token being processed (the position of reported errors).
     * <p>
     * 처리 중인 토큰의 위치(보고되는 오류의 위치).
     */
    int tokenOffset;

    /**
     * Parent of the location computed by {@link #computeLocation(int)}.
     * <p>
     * {@link #computeLocation(int)} 이 계산한 위치의 부모.
     */
    Node locParent;

    /**
     * Child before which to insert at the computed location, or null to append.
     * <p>
     * 계산한 위치에서 그 앞에 삽입할 자식. 끝에 붙이려면 null.
     */
    Node locBefore;

    /**
     * Pending text that has not been put into the tree yet.
     * <p>
     * 아직 트리에 넣지 않은 대기 텍스트.
     */
    private final StringBuilder pendingText = new StringBuilder();

    /**
     * Parent of the pending text.
     * <p>
     * 대기 텍스트의 부모.
     */
    private Node pendingParent;

    /**
     * Child before which the pending text goes, or null.
     * <p>
     * 대기 텍스트가 그 앞에 들어갈 자식 또는 null.
     */
    private Node pendingBefore;

    /**
     * True once a <code>base</code> element with an <code>href</code> has been seen.
     * <p>
     * <code>href</code> 가 있는 <code>base</code> 요소를 이미 보았으면 true.
     */
    private boolean baseSeen;

    /**
     * True once the title of the document has been reported.
     * <p>
     * 문서 제목을 이미 보고했으면 true.
     */
    private boolean titleReported;

    /**
     * Creates the base of a tree builder.
     * <p>
     * 트리 생성기의 기반을 만든다.
     *
     * @param doc       document / 문서
     * @param tokenizer tokenizer / 토큰화기
     * @param log       error collector / 오류 수집기
     * @param listener  listener or null / 리스너 또는 null
     * @param control   parser control / 파서 제어
     * @param host      host or null / 호스트 또는 null
     * @param scripting parser scripting flag / parser scripting 플래그
     */
    TreeBuilderBase(Document doc, HtmlTokenizer tokenizer, ParseErrorLog log, ParserListener listener, ParserControl control, ParserHost host,
            boolean scripting) {
        this.doc = doc;
        this.tokenizer = tokenizer;
        this.log = log;
        this.listener = listener;
        this.control = control;
        this.host = host;
        this.scripting = scripting;
    }

    /**
     * Reports a parse error at the current token.
     * <p>
     * 현재 토큰 위치에 파싱 오류를 보고한다.
     *
     * @param code error code / 오류 코드
     */
    final void err(String code) {
        log.add(code, tokenOffset);
    }

    /**
     * Returns the tag id of a token (computed on first use).
     * <p>
     * 토큰의 태그 번호를 반환한다(처음 쓸 때 계산).
     *
     * @param t token / 토큰
     * @return id / 번호
     */
    final int tid(HtmlToken t) {
        if (t.tagId < 0) {
            t.tagId = HtmlTags.id(t.name);
        }
        return t.tagId;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 열린 요소 스택
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the current node (the bottommost element of the stack).
     * <p>
     * 현재 노드(스택의 맨 아래 요소)를 반환한다.
     *
     * @return element, or null if the stack is empty / 요소. 스택이 비었으면 null
     */
    final Element currentNode() {
        return stackSize == 0 ? null : stackEl[stackSize - 1];
    }

    /**
     * Tells whether the current node is an HTML element with a tag id.
     * <p>
     * 현재 노드가 해당 번호의 HTML 요소인지 알려 준다.
     *
     * @param id tag id / 태그 번호
     * @return true if so / 그러면 true
     */
    final boolean currentIs(int id) {
        return stackSize > 0 && stackNs[stackSize - 1] == NS_HTML && stackId[stackSize - 1] == id;
    }

    /**
     * Tells whether the stack entry at an index is an HTML element with a tag id.
     * <p>
     * 스택의 해당 위치 항목이 해당 번호의 HTML 요소인지 알려 준다.
     *
     * @param i  index / 위치
     * @param id tag id / 태그 번호
     * @return true if so / 그러면 true
     */
    final boolean isHtmlAt(int i, int id) {
        return stackNs[i] == NS_HTML && stackId[i] == id;
    }

    /**
     * Pushes an element onto the stack.
     * <p>
     * 요소를 스택에 넣는다.
     *
     * @param e  element / 요소
     * @param id tag id / 태그 번호
     * @param ns namespace code / 네임스페이스 코드
     */
    final void push(Element e, int id, int ns) {
        if (stackSize == stackEl.length) {
            int n = stackSize * 2;
            Element[] el = new Element[n];
            int[] ids = new int[n];
            int[] nss = new int[n];
            System.arraycopy(stackEl, 0, el, 0, stackSize);
            System.arraycopy(stackId, 0, ids, 0, stackSize);
            System.arraycopy(stackNs, 0, nss, 0, stackSize);
            stackEl = el;
            stackId = ids;
            stackNs = nss;
        }
        stackEl[stackSize] = e;
        stackId[stackSize] = id;
        stackNs[stackSize] = ns;
        stackSize++;
    }

    /**
     * Pops the current node ("finish parsing the children" is reported to the listener).
     * <p>
     * 현재 노드를 꺼낸다("finish parsing the children" 를 리스너에 알린다).
     *
     * @return the popped element / 꺼낸 요소
     */
    final Element pop() {
        stackSize--;
        Element e = stackEl[stackSize];
        int id = stackId[stackSize];
        int ns = stackNs[stackSize];
        stackEl[stackSize] = null;
        closed(e, id, ns);
        return e;
    }

    /**
     * Removes the stack entry at an index (the entries below move up).
     * <p>
     * 스택의 해당 위치 항목을 제거한다(아래 항목들이 올라온다).
     *
     * @param i index / 위치
     */
    final void removeAt(int i) {
        Element e = stackEl[i];
        int id = stackId[i];
        int ns = stackNs[i];
        // 제거한 자리를 아래 항목으로 메운다.
        for (int k = i; k < stackSize - 1; k++) {
            stackEl[k] = stackEl[k + 1];
            stackId[k] = stackId[k + 1];
            stackNs[k] = stackNs[k + 1];
        }
        stackSize--;
        stackEl[stackSize] = null;
        closed(e, id, ns);
    }

    /**
     * Inserts an entry into the stack at an index.
     * <p>
     * 스택의 해당 위치에 항목을 끼워 넣는다.
     *
     * @param i  index / 위치
     * @param e  element / 요소
     * @param id tag id / 태그 번호
     * @param ns namespace code / 네임스페이스 코드
     */
    final void insertAt(int i, Element e, int id, int ns) {
        push(e, id, ns);
        // 새 항목이 들어갈 자리를 비우도록 아래 항목을 한 칸씩 내린다.
        for (int k = stackSize - 1; k > i; k--) {
            stackEl[k] = stackEl[k - 1];
            stackId[k] = stackId[k - 1];
            stackNs[k] = stackNs[k - 1];
        }
        stackEl[i] = e;
        stackId[i] = id;
        stackNs[i] = ns;
    }

    /**
     * Returns the stack index of an element, searching from the bottom.
     * <p>
     * 요소의 스택 위치를 아래에서부터 찾아 반환한다.
     *
     * @param e element / 요소
     * @return index or -1 / 위치 또는 -1
     */
    final int indexOf(Element e) {
        // 스택을 아래에서 위로 훑어 같은 요소를 찾는다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (stackEl[i] == e) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Pops elements until an HTML element with a tag id has been popped.
     * <p>
     * 해당 번호의 HTML 요소가 꺼내질 때까지 요소를 꺼낸다.
     *
     * @param id tag id / 태그 번호
     */
    final void popUntil(int id) {
        // 해당 요소를 꺼낼 때까지 반복한다(스택 바닥을 넘지 않는다).
        while (stackSize > 0) {
            boolean hit = isHtmlAt(stackSize - 1, id);
            pop();
            if (hit) {
                return;
            }
        }
    }

    /**
     * Pops elements until a given element has been popped.
     * <p>
     * 주어진 요소가 꺼내질 때까지 요소를 꺼낸다.
     *
     * @param e element / 요소
     */
    final void popUntilElement(Element e) {
        // 해당 요소를 꺼낼 때까지 반복한다.
        while (stackSize > 0) {
            boolean hit = stackEl[stackSize - 1] == e;
            pop();
            if (hit) {
                return;
            }
        }
    }

    /**
     * Pops elements until one of the h1-h6 elements has been popped.
     * <p>
     * h1-h6 중 하나가 꺼내질 때까지 요소를 꺼낸다.
     */
    final void popUntilHeading() {
        // 제목 요소를 꺼낼 때까지 반복한다.
        while (stackSize > 0) {
            boolean hit = stackNs[stackSize - 1] == NS_HTML && stackId[stackSize - 1] >= HtmlTags.H1 && stackId[stackSize - 1] <= HtmlTags.H6;
            pop();
            if (hit) {
                return;
            }
        }
    }

    /**
     * Tells whether the stack has an HTML element with a tag id anywhere.
     * <p>
     * 스택 어디엔가 해당 번호의 HTML 요소가 있는지 알려 준다.
     *
     * @param id tag id / 태그 번호
     * @return true if present / 있으면 true
     */
    final boolean stackHas(int id) {
        // 스택 전체를 훑는다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (isHtmlAt(i, id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the index of the last HTML element with a tag id in the stack, or -1.
     * <p>
     * 스택에서 해당 번호의 마지막 HTML 요소의 위치를 반환한다. 없으면 -1.
     *
     * @param id tag id / 태그 번호
     * @return index / 위치
     */
    final int lastIndexOf(int id) {
        // 아래에서 위로 찾는다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (isHtmlAt(i, id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tells whether the stack entry at an index is in the "special" category.
     * <p>
     * 스택의 해당 위치 항목이 "special" 범주인지 알려 준다.
     *
     * @param i index / 위치
     * @return true if special / special 이면 true
     */
    final boolean isSpecialAt(int i) {
        int id = stackId[i];
        switch (stackNs[i]) {
        case NS_HTML:
            return HtmlTags.isSpecial(id);
        case NS_MATHML:
            return id == HtmlTags.MI || id == HtmlTags.MO || id == HtmlTags.MN || id == HtmlTags.MS || id == HtmlTags.MTEXT
                    || id == HtmlTags.ANNOTATION_XML;
        default:
            return id == HtmlTags.FOREIGNOBJECT || id == HtmlTags.DESC || id == HtmlTags.TITLE;
        }
    }

    /**
     * Tells whether the stack entry at an index ends a scope search of a kind.
     * <p>
     * 스택의 해당 위치 항목이 어떤 종류의 범위 검색을 끝내는지 알려 준다.
     *
     * @param i    index / 위치
     * @param kind scope kind / 범위 종류
     * @return true if it is a boundary / 경계이면 true
     */
    private boolean isBoundary(int i, int kind) {
        int ns = stackNs[i];
        int id = stackId[i];
        if (kind == SCOPE_TABLE) {
            return ns == NS_HTML && (id == HtmlTags.HTML || id == HtmlTags.TABLE || id == HtmlTags.TEMPLATE);
        }
        if (kind == SCOPE_SELECT) {
            return !(ns == NS_HTML && (id == HtmlTags.OPTGROUP || id == HtmlTags.OPTION));
        }
        if (ns == NS_HTML) {
            if (HtmlTags.isScopeBoundary(id)) {
                return true;
            }
            return (kind == SCOPE_LIST && (id == HtmlTags.OL || id == HtmlTags.UL)) || (kind == SCOPE_BUTTON && id == HtmlTags.BUTTON);
        }
        if (ns == NS_MATHML) {
            return id == HtmlTags.MI || id == HtmlTags.MO || id == HtmlTags.MN || id == HtmlTags.MS || id == HtmlTags.MTEXT
                    || id == HtmlTags.ANNOTATION_XML;
        }
        return id == HtmlTags.FOREIGNOBJECT || id == HtmlTags.DESC || id == HtmlTags.TITLE;
    }

    /**
     * Tells whether the stack has an HTML element with a tag id in a scope.
     * <p>
     * 스택이 해당 번호의 HTML 요소를 어떤 범위 안에 가지고 있는지 알려 준다.
     *
     * @param id   tag id / 태그 번호
     * @param kind scope kind / 범위 종류
     * @return true if in scope / 범위 안에 있으면 true
     */
    final boolean inScope(int id, int kind) {
        // 현재 노드에서 위로 올라가며 대상을 찾거나 범위 경계를 만날 때까지 본다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (isHtmlAt(i, id)) {
                return true;
            }
            if (isBoundary(i, kind)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Tells whether a given element is in the default scope.
     * <p>
     * 주어진 요소가 기본 범위 안에 있는지 알려 준다.
     *
     * @param e element / 요소
     * @return true if in scope / 범위 안에 있으면 true
     */
    final boolean elementInScope(Element e) {
        // 현재 노드에서 위로 올라가며 대상을 찾거나 범위 경계를 만날 때까지 본다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (stackEl[i] == e) {
                return true;
            }
            if (isBoundary(i, SCOPE_DEFAULT)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Tells whether the stack has one of the h1-h6 elements in the default scope.
     * <p>
     * 스택이 기본 범위 안에 h1-h6 중 하나를 가지고 있는지 알려 준다.
     *
     * @return true if so / 그러면 true
     */
    final boolean headingInScope() {
        // 현재 노드에서 위로 올라가며 제목 요소를 찾거나 범위 경계를 만날 때까지 본다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (stackNs[i] == NS_HTML && stackId[i] >= HtmlTags.H1 && stackId[i] <= HtmlTags.H6) {
                return true;
            }
            if (isBoundary(i, SCOPE_DEFAULT)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Generates implied end tags: pops dd, dt, li, optgroup, option, p, rb, rp, rt and rtc elements from the current node.
     * <p>
     * 암시적 종료 태그를 만든다: 현재 노드부터 dd, dt, li, optgroup, option, p, rb, rp, rt, rtc 요소를 꺼낸다.
     *
     * @param exceptName local name of elements that stay open, or null / 남겨 둘 요소의 로컬 이름. 없으면 null
     */
    final void generateImpliedEndTags(String exceptName) {
        // 현재 노드가 종료 태그를 생략할 수 있는 요소인 동안 꺼낸다.
        while (stackSize > 0 && stackNs[stackSize - 1] == NS_HTML && HtmlTags.isImpliedEnd(stackId[stackSize - 1])
                && !(exceptName != null && exceptName.equals(stackEl[stackSize - 1].getLocalName()))) {
            pop();
        }
    }

    /**
     * Generates all implied end tags thoroughly (also caption, colgroup, tbody, td, tfoot, th, thead, tr).
     * <p>
     * 모든 암시적 종료 태그를 철저히 만든다(caption, colgroup, tbody, td, tfoot, th, thead, tr 도 포함).
     */
    final void generateImpliedEndTagsThoroughly() {
        // 현재 노드가 종료 태그를 생략할 수 있는 요소인 동안 꺼낸다.
        while (stackSize > 0 && stackNs[stackSize - 1] == NS_HTML) {
            int id = stackId[stackSize - 1];
            if (!(HtmlTags.isImpliedEnd(id) || id == HtmlTags.CAPTION || id == HtmlTags.COLGROUP || id == HtmlTags.TBODY || id == HtmlTags.TD
                    || id == HtmlTags.TFOOT || id == HtmlTags.TH || id == HtmlTags.THEAD || id == HtmlTags.TR)) {
                break;
            }
            pop();
        }
    }

    /**
     * Called when an element leaves the stack: the listener learns that its children are complete.
     * <p>
     * 요소가 스택에서 나갈 때 호출된다: 리스너가 자식이 완성되었음을 알게 된다.
     *
     * @param e  element / 요소
     * @param id tag id / 태그 번호
     * @param ns namespace code / 네임스페이스 코드
     */
    private void closed(Element e, int id, int ns) {
        if (listener == null || fragment) {
            return;
        }
        listener.elementClosed(e);
        if (ns == NS_HTML && id == HtmlTags.STYLE) {
            listener.styleReady(e);
        } else if (ns == NS_HTML && id == HtmlTags.TITLE && !titleReported) {
            titleReported = true;
            listener.titleReady(e.getTextContent());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 삽입 위치와 노드 삽입
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether an element is an HTML <code>template</code>.
     * <p>
     * 요소가 HTML <code>template</code> 인지 알려 준다.
     *
     * @param n node / 노드
     * @return true if so / 그러면 true
     */
    static boolean isTemplate(Node n) {
        return n instanceof Element && Namespaces.HTML.equals(((Element) n).getNamespaceURI()) && "template".equals(((Element) n).getLocalName());
    }

    /**
     * Sets the computed location, redirecting into template contents.
     * <p>
     * 계산한 위치를 지정한다. template 이면 template contents 안으로 돌린다.
     *
     * @param parent parent / 부모
     * @param before child to insert before, or null / 그 앞에 삽입할 자식 또는 null
     */
    private void setLoc(Node parent, Node before) {
        if (isTemplate(parent)) {
            locParent = ParserNodeData.ensureTemplateContents((Element) parent);
            locBefore = null;
        } else {
            locParent = parent;
            locBefore = before;
        }
    }

    /**
     * Computes the "appropriate place for inserting a node" for the stack entry at an index; the result is in
     * {@link #locParent} and {@link #locBefore}.
     * <p>
     * 스택의 해당 위치 항목을 대상으로 "appropriate place for inserting a node" 를 계산한다. 결과는 {@link #locParent} 와 {@link #locBefore} 에 있다.
     *
     * @param idx stack index of the target element / 대상 요소의 스택 위치
     */
    final void computeLocation(int idx) {
        int id = stackId[idx];
        boolean tableRelated = stackNs[idx] == NS_HTML
                && (id == HtmlTags.TABLE || id == HtmlTags.TBODY || id == HtmlTags.TFOOT || id == HtmlTags.THEAD || id == HtmlTags.TR);
        if (fosterParenting && tableRelated) {
            int lastTemplate = lastIndexOf(HtmlTags.TEMPLATE);
            int lastTable = lastIndexOf(HtmlTags.TABLE);
            if (lastTemplate >= 0 && (lastTable < 0 || lastTemplate > lastTable)) {
                setLoc(stackEl[lastTemplate], null);
                return;
            }
            if (lastTable < 0) {
                setLoc(stackEl[0], null);
                return;
            }
            Node p = stackEl[lastTable].getParentNode();
            if (p != null) {
                setLoc(p, stackEl[lastTable]);
                return;
            }
            setLoc(stackEl[lastTable - 1], null);
            return;
        }
        setLoc(stackEl[idx], null);
    }

    /**
     * Inserts a node at the computed location, ignoring errors that the DOM reports (for example a second document element).
     * <p>
     * 계산한 위치에 노드를 삽입한다. DOM 이 보고하는 오류(예: 두 번째 문서 요소)는 무시한다.
     *
     * @param parent parent / 부모
     * @param before child to insert before or null / 그 앞에 삽입할 자식 또는 null
     * @param n      node / 노드
     */
    final void tryInsert(Node parent, Node before, Node n) {
        try {
            parent.insertBefore(n, before);
        } catch (DomException ex) {
            err("insertion-failed");
        }
    }

    /**
     * Puts the pending text into the tree.
     * <p>
     * 대기 중인 텍스트를 트리에 넣는다.
     */
    final void flushText() {
        if (pendingText.length() == 0) {
            return;
        }
        Node parent = pendingParent;
        Node before = pendingBefore;
        String data = pendingText.toString();
        pendingText.setLength(0);
        pendingParent = null;
        pendingBefore = null;
        if (before != null && before.getParentNode() != parent) {
            return;
        }
        Node prev = before != null ? before.getPreviousSibling() : parent.getLastChild();
        try {
            if (prev != null && prev.getNodeType() == Node.TEXT_NODE) {
                ((Text) prev).appendData(data);
            } else {
                parent.insertBefore(doc.createTextNode(data), before);
            }
        } catch (DomException ex) {
            err("insertion-failed");
        }
    }

    /**
     * Inserts characters at the appropriate place (the text is batched until something else is inserted).
     * <p>
     * 알맞은 위치에 문자를 삽입한다(다른 것이 삽입될 때까지 텍스트를 모아 둔다).
     *
     * @param s characters / 문자
     */
    final void insertChars(String s) {
        if (stackSize == 0) {
            return;
        }
        computeLocation(stackSize - 1);
        if (locParent.getNodeType() == Node.DOCUMENT_NODE) {
            return;
        }
        if (pendingText.length() > 0 && (pendingParent != locParent || pendingBefore != locBefore)) {
            flushText();
        }
        if (pendingText.length() == 0) {
            pendingParent = locParent;
            pendingBefore = locBefore;
        }
        pendingText.append(s);
    }

    /**
     * Inserts a comment at the appropriate place, or into a given parent.
     * <p>
     * 알맞은 위치에, 또는 주어진 부모에 주석을 삽입한다.
     *
     * @param data   comment text / 주석 텍스트
     * @param parent explicit parent, or null for the appropriate place / 지정한 부모. 알맞은 위치이면 null
     */
    final void insertComment(String data, Node parent) {
        flushText();
        if (parent != null) {
            tryInsert(parent, null, doc.createComment(data));
            return;
        }
        if (stackSize == 0) {
            tryInsert(doc, null, doc.createComment(data));
            return;
        }
        computeLocation(stackSize - 1);
        tryInsert(locParent, locBefore, doc.createComment(data));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 요소 만들기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Lower-cases an ASCII string.
     * <p>
     * ASCII 문자열을 소문자로 바꾼다.
     *
     * @param s text / 문자열
     * @return lower case text / 소문자 문자열
     */
    static String lower(String s) {
        return DocumentModes.lower(s);
    }

    /**
     * Returns the SVG element name with its correct case.
     * <p>
     * 올바른 대소문자의 SVG 요소 이름을 반환한다.
     *
     * @param lowerName lower case name / 소문자 이름
     * @return adjusted name (the same name if there is no adjustment) / 보정된 이름(보정이 없으면 같은 이름)
     */
    static String adjustSvgElementName(String lowerName) {
        String a = SVG_ELEMENTS.get(lowerName);
        return a == null ? lowerName : a;
    }

    /**
     * Sets the attributes of a token on an element, applying the SVG or MathML attribute adjustment and the foreign attribute
     * adjustment for foreign elements. Attributes that the DOM rejects (names that are not XML names) are dropped with an error.
     * <p>
     * 토큰의 속성을 요소에 지정한다. 외부 요소에는 SVG 또는 MathML 속성 보정과 foreign 속성 보정을 적용한다. 빈 이름만 거부한다(XML 이름이 아닌 이름은 받아들인다. 거부된
     * 속성은 오류를 보고하고 버린다).
     *
     * @param el   element / 요소
     * @param t    token / 토큰
     * @param kind attribute adjustment kind / 속성 보정 종류
     */
    final void applyAttributes(Element el, HtmlToken t, int kind) {
        int n = t.getAttributeCount();
        // 토큰의 속성을 차례로 요소에 옮긴다.
        for (int i = 0; i < n; i++) {
            String name = t.attrNames.get(i);
            String value = t.attrValues.get(i);
            try {
                if (kind == ATTRS_HTML) {
                    el.setAttributeUnchecked(name, value);
                    continue;
                }
                if (kind == ATTRS_SVG) {
                    String a = SVG_ATTRIBUTES.get(name);
                    if (a != null) {
                        name = a;
                    }
                } else if ("definitionurl".equals(name)) {
                    name = "definitionURL";
                }
                String[] f = FOREIGN_ATTRIBUTES.get(name);
                if (f != null) {
                    el.setAttributeNS(f[0], f[1], value);
                } else {
                    el.setAttributeUnchecked(name, value);
                }
            } catch (DomException ex) {
                err("invalid-attribute-name");
            }
        }
    }

    /**
     * Creates an element for a token without inserting it ("create an element for a token").
     * <p>
     * 삽입하지 않고 토큰으로 요소를 만든다("create an element for a token").
     *
     * @param localName local name of the element / 요소의 로컬 이름
     * @param t         token that supplies the attributes, or null / 속성을 제공하는 토큰. 없으면 null
     * @param ns        namespace code / 네임스페이스 코드
     * @param kind      attribute adjustment kind / 속성 보정 종류
     * @return the element / 요소
     */
    final Element createElement(String localName, HtmlToken t, int ns, int kind) {
        Element el = doc.createElementInternal(NS_URI[ns], null, localName);
        if (t != null && t.attrNames != null) {
            applyAttributes(el, t, kind);
        }
        if (ns == NS_HTML) {
            if ("template".equals(localName)) {
                ParserNodeData.ensureTemplateContents(el);
            } else if ("script".equals(localName)) {
                ParserNodeData.ScriptFlags f = ParserNodeData.scriptFlags(el);
                f.setParserInserted(true);
                f.setForceAsync(false);
                if (fragment) {
                    f.setAlreadyStarted(true);
                }
            }
        }
        return el;
    }

    /**
     * Creates an HTML element from the token of a formatting entry (adoption agency, reconstruction).
     * <p>
     * 서식 항목의 토큰으로 HTML 요소를 만든다(adoption agency, 재구성).
     *
     * @param e entry / 항목
     * @return the new element / 새 요소
     */
    final Element createFromEntry(FormatEntry e) {
        return createElement(e.token.name, e.token, NS_HTML, ATTRS_HTML);
    }

    /**
     * Creates an element for a token, inserts it at the appropriate place and pushes it onto the stack ("insert a foreign element"; for
     * the HTML namespace "insert an HTML element").
     * <p>
     * 토큰으로 요소를 만들어 알맞은 위치에 삽입하고 스택에 넣는다("insert a foreign element". HTML 네임스페이스이면 "insert an HTML element").
     *
     * @param t         token / 토큰
     * @param localName local name of the new element / 새 요소의 로컬 이름
     * @param ns        namespace code / 네임스페이스 코드
     * @param kind      attribute adjustment kind / 속성 보정 종류
     * @return the new element / 새 요소
     */
    final Element insertElement(HtmlToken t, String localName, int ns, int kind) {
        flushText();
        Element el = createElement(localName, t, ns, kind);
        computeLocation(stackSize - 1);
        tryInsert(locParent, locBefore, el);
        int id = ns == NS_HTML ? HtmlTags.id(localName) : HtmlTags.id(lower(localName));
        if (ns == NS_HTML) {
            associateForm(el, id);
        }
        push(el, id, ns);
        afterInsert(el, id, ns);
        return el;
    }

    /**
     * Inserts an HTML element for a token.
     * <p>
     * 토큰으로 HTML 요소를 삽입한다.
     *
     * @param t token / 토큰
     * @return the new element / 새 요소
     */
    final Element insertHtml(HtmlToken t) {
        return insertElement(t, t.name, NS_HTML, ATTRS_HTML);
    }

    /**
     * Inserts an HTML element for an attribute-less synthetic start tag.
     * <p>
     * 속성이 없는 가상의 시작 태그로 HTML 요소를 삽입한다.
     *
     * @param name tag name / 태그 이름
     * @return the new element / 새 요소
     */
    final Element insertHtml(String name) {
        return insertElement(HtmlToken.synthetic(HtmlToken.START_TAG, name, tokenOffset), name, NS_HTML, ATTRS_HTML);
    }

    /**
     * Associates a form-associated element with the form element pointer, as the standard describes for "create an element for the token".
     * <p>
     * 폼 연관 요소를 form 요소 포인터에 연결한다. "create an element for the token" 에 표준이 적은 대로이다.
     *
     * @param el element / 요소
     * @param id tag id / 태그 번호
     */
    private void associateForm(Element el, int id) {
        if (formPointer == null || fragment || (id != HtmlTags.BUTTON && id != HtmlTags.FIELDSET && id != HtmlTags.INPUT && id != HtmlTags.OBJECT
                && id != HtmlTags.SELECT && id != HtmlTags.TEXTAREA && id != HtmlTags.IMG && !"output".equals(el.getLocalName()))) {
            return;
        }
        if (stackHas(HtmlTags.TEMPLATE) || (id != HtmlTags.IMG && el.hasAttribute("form"))) {
            return;
        }
        if (locParent.getRootNode() == formPointer.getRootNode()) {
            ParserNodeData.setFormOwner(el, formPointer);
        }
    }

    /**
     * Hook after an element has been inserted and pushed: it tells the listener and handles the elements that the loading model cares
     * about (<code>base</code>, <code>meta</code>, <code>link</code>, <code>img</code>).
     * <p>
     * 요소를 삽입하고 스택에 넣은 뒤의 훅: 리스너에 알리고 로딩 모델이 관심을 갖는 요소(<code>base</code>, <code>meta</code>, <code>link</code>,
     * <code>img</code>)를 처리한다.
     *
     * @param el element / 요소
     * @param id tag id / 태그 번호
     * @param ns namespace code / 네임스페이스 코드
     */
    private void afterInsert(Element el, int id, int ns) {
        if (fragment) {
            return;
        }
        if (listener != null) {
            listener.elementInserted(el);
        }
        if (ns != NS_HTML) {
            return;
        }
        switch (id) {
        case HtmlTags.IMG:
            if (listener != null) {
                listener.imageFound(el);
            }
            break;
        case HtmlTags.LINK:
            String rel = el.getAttribute("rel");
            if (listener != null && rel != null && containsToken(rel, "stylesheet")) {
                listener.stylesheetFound(el);
            }
            break;
        case HtmlTags.BASE:
            handleBase(el);
            break;
        case HtmlTags.META:
            handleMeta(el);
            break;
        default:
            break;
        }
    }

    /**
     * Tells whether a space separated token list contains a token, ignoring ASCII case.
     * <p>
     * 공백으로 구분한 토큰 목록에 토큰이 있는지 ASCII 대소문자를 무시하고 알려 준다.
     *
     * @param list  token list / 토큰 목록
     * @param token lower case token / 소문자 토큰
     * @return true if present / 있으면 true
     */
    static boolean containsToken(String list, String token) {
        String[] parts = lower(list).split("[ \t\n\f\r]+");
        // 목록의 토큰을 하나씩 비교한다.
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a <code>base</code> element: the first one with an <code>href</code> fixes the document base URL.
     * <p>
     * <code>base</code> 요소를 처리한다: <code>href</code> 가 있는 첫 요소가 문서 기준 URL 을 정한다.
     *
     * @param el the element / 요소
     */
    private void handleBase(Element el) {
        String href = el.getAttribute("href");
        if (baseSeen || href == null) {
            return;
        }
        baseSeen = true;
        String resolved = null;
        try {
            resolved = UrlUtil.resolve(doc.getUrl(), href);
        } catch (IllegalArgumentException ex) {
            err("invalid-base-url");
        }
        if (resolved != null) {
            doc.setBaseUri(resolved);
            if (listener != null) {
                listener.baseHrefChanged(href, resolved);
            }
        }
    }

    /**
     * Handles a <code>meta</code> element: a declared encoding, or a refresh.
     * <p>
     * <code>meta</code> 요소를 처리한다: 선언된 인코딩 또는 새로고침.
     *
     * @param el the element / 요소
     */
    private void handleMeta(Element el) {
        String charset = el.getAttribute("charset");
        String httpEquiv = el.getAttribute("http-equiv");
        String content = el.getAttribute("content");
        String name = null;
        if (charset != null) {
            name = EncodingLabels.canonicalName(charset);
        } else if (httpEquiv != null && content != null && "content-type".equals(lower(httpEquiv))) {
            name = EncodingSniffer.extractFromMetaContent(content);
        }
        if (name != null) {
            if (listener != null) {
                listener.charsetDeclared(name);
            }
            if (host != null && host.metaCharset(name)) {
                stopped = true;
            }
        }
        if (listener != null && httpEquiv != null && content != null && "refresh".equals(lower(httpEquiv))) {
            listener.metaRefresh(content);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 활성 서식 요소 목록
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Inserts a marker at the end of the list of active formatting elements.
     * <p>
     * 활성 서식 요소 목록의 끝에 표시(marker)를 넣는다.
     */
    final void insertMarker() {
        formatting.add(new FormatEntry(null, null));
    }

    /**
     * Clears the list of active formatting elements up to the last marker.
     * <p>
     * 활성 서식 요소 목록을 마지막 표시까지 비운다.
     */
    final void clearFormattingToMarker() {
        // 끝에서부터 표시를 제거할 때까지 항목을 지운다.
        while (!formatting.isEmpty()) {
            FormatEntry e = formatting.remove(formatting.size() - 1);
            if (e.element == null) {
                return;
            }
        }
    }

    /**
     * Tells whether two tokens have the same name and the same attributes (in any order).
     * <p>
     * 두 토큰의 이름과 속성(순서 무관)이 같은지 알려 준다.
     *
     * @param a first token / 첫 토큰
     * @param b second token / 둘째 토큰
     * @return true if equal / 같으면 true
     */
    private static boolean sameTag(HtmlToken a, HtmlToken b) {
        if (!a.name.equals(b.name) || a.getAttributeCount() != b.getAttributeCount()) {
            return false;
        }
        // 한 토큰의 속성마다 다른 토큰에 같은 이름과 값이 있는지 확인한다.
        for (int i = 0; i < a.getAttributeCount(); i++) {
            String v = b.getAttribute(a.attrNames.get(i));
            if (v == null || !v.equals(a.attrValues.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pushes an element onto the list of active formatting elements, applying the Noah's Ark clause (at most three equal entries after
     * the last marker).
     * <p>
     * 활성 서식 요소 목록에 요소를 넣는다. Noah's Ark 규칙을 적용한다(마지막 표시 뒤에 같은 항목은 최대 세 개).
     *
     * @param el element / 요소
     * @param t  token that created it / 요소를 만든 토큰
     */
    final void pushFormatting(Element el, HtmlToken t) {
        int count = 0;
        int earliest = -1;
        // 마지막 표시 뒤의 항목 중 같은 태그를 세고 가장 오래된 것의 위치를 기억한다.
        for (int i = formatting.size() - 1; i >= 0; i--) {
            FormatEntry e = formatting.get(i);
            if (e.element == null) {
                break;
            }
            if (sameTag(e.token, t)) {
                count++;
                earliest = i;
            }
        }
        if (count >= 3) {
            formatting.remove(earliest);
        }
        formatting.add(new FormatEntry(el, t));
    }

    /**
     * Returns the entry of an element in the list of active formatting elements, or null.
     * <p>
     * 활성 서식 요소 목록에서 요소의 항목을 반환한다. 없으면 null.
     *
     * @param el element / 요소
     * @return entry or null / 항목 또는 null
     */
    final FormatEntry findFormatting(Element el) {
        // 목록을 끝에서부터 찾는다.
        for (int i = formatting.size() - 1; i >= 0; i--) {
            if (formatting.get(i).element == el) {
                return formatting.get(i);
            }
        }
        return null;
    }

    /**
     * Reconstructs the active formatting elements ("reconstruct the active formatting elements").
     * <p>
     * 활성 서식 요소를 재구성한다("reconstruct the active formatting elements").
     */
    final void reconstructFormatting() {
        int n = formatting.size();
        if (n == 0) {
            return;
        }
        FormatEntry last = formatting.get(n - 1);
        if (last.element == null || indexOf(last.element) >= 0) {
            return;
        }
        int i = n - 1;
        // 표시나 스택에 있는 항목을 만날 때까지 거슬러 올라간다.
        while (i > 0) {
            FormatEntry e = formatting.get(i - 1);
            if (e.element == null || indexOf(e.element) >= 0) {
                break;
            }
            i--;
        }
        // 거슬러 올라간 위치부터 끝까지 요소를 다시 만들어 삽입하고 항목을 바꾼다.
        for (; i < n; i++) {
            FormatEntry e = formatting.get(i);
            e.element = insertElement(e.token, e.token.name, NS_HTML, ATTRS_HTML);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // adoption agency
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles an end tag that matches no formatting element's rules ("any other end tag" of the "in body" insertion mode).
     * <p>
     * 서식 요소 규칙에 해당하지 않는 종료 태그를 처리한다("in body" 삽입 모드의 "any other end tag").
     *
     * @param t end tag token / 종료 태그 토큰
     */
    abstract void anyOtherEndTag(HtmlToken t);

    /**
     * The adoption agency algorithm.
     * <p>
     * adoption agency 알고리즘.
     *
     * @param t the token (its name is the subject) / 토큰(이름이 대상이다)
     */
    final void adoptionAgency(HtmlToken t) {
        String subject = t.name;
        int top = stackSize - 1;
        if (top >= 0 && stackNs[top] == NS_HTML && subject.equals(stackEl[top].getLocalName()) && findFormatting(stackEl[top]) == null) {
            pop();
            return;
        }
        // 바깥 반복은 최대 8 번이다.
        for (int outer = 0; outer < 8; outer++) {
            int fi = -1;
            // 마지막 표시 뒤에서 이름이 같은 서식 요소를 끝에서부터 찾는다.
            for (int i = formatting.size() - 1; i >= 0; i--) {
                FormatEntry e = formatting.get(i);
                if (e.element == null) {
                    break;
                }
                if (subject.equals(e.element.getLocalName())) {
                    fi = i;
                    break;
                }
            }
            if (fi < 0) {
                anyOtherEndTag(t);
                return;
            }
            FormatEntry fe = formatting.get(fi);
            Element formattingElement = fe.element;
            int si = indexOf(formattingElement);
            if (si < 0) {
                err("adoption-element-not-open");
                formatting.remove(fi);
                return;
            }
            if (!elementInScope(formattingElement)) {
                err("adoption-element-not-in-scope");
                return;
            }
            if (formattingElement != currentNode()) {
                err("adoption-element-not-current");
            }
            int fb = -1;
            // 서식 요소보다 아래에 있는 첫 번째 special 요소(furthest block)를 찾는다.
            for (int i = si + 1; i < stackSize; i++) {
                if (isSpecialAt(i)) {
                    fb = i;
                    break;
                }
            }
            if (fb < 0) {
                popUntilElement(formattingElement);
                formatting.remove(fe);
                return;
            }
            Element commonAncestor = stackEl[si - 1];
            int bookmark = fi;
            Element furthestBlock = stackEl[fb];
            Element lastNode = furthestBlock;
            int nodeIdx = fb;
            int inner = 0;
            // 안쪽 반복: furthest block 위의 요소들을 차례로 훑으며 서식 요소가 아니면 제거하고 맞으면 복제해 이어 붙인다.
            while (true) {
                inner++;
                nodeIdx--;
                Element node = stackEl[nodeIdx];
                if (node == formattingElement) {
                    break;
                }
                FormatEntry ne = findFormatting(node);
                if (inner > 3 && ne != null) {
                    int r = formatting.indexOf(ne);
                    formatting.remove(r);
                    if (r < bookmark) {
                        bookmark--;
                    }
                    ne = null;
                }
                if (ne == null) {
                    removeAt(nodeIdx);
                    continue;
                }
                Element fresh = createFromEntry(ne);
                ne.element = fresh;
                stackEl[nodeIdx] = fresh;
                if (lastNode == furthestBlock) {
                    bookmark = formatting.indexOf(ne) + 1;
                }
                fresh.appendChild(lastNode);
                lastNode = fresh;
            }
            // lastNode 를 commonAncestor 를 기준으로 한 알맞은 위치에 삽입한다.
            flushText();
            computeLocationFor(commonAncestor, si - 1);
            tryInsert(locParent, locBefore, lastNode);
            Element replacement = createFromEntry(fe);
            // furthest block 의 자식을 모두 새 요소로 옮기고 새 요소를 furthest block 의 자식으로 붙인다.
            while (furthestBlock.getFirstChild() != null) {
                replacement.appendChild(furthestBlock.getFirstChild());
            }
            furthestBlock.appendChild(replacement);
            int fIdx = formatting.indexOf(fe);
            formatting.remove(fIdx);
            if (fIdx < bookmark) {
                bookmark--;
            }
            fe.element = replacement;
            formatting.add(Math.min(bookmark, formatting.size()), fe);
            removeAt(indexOf(formattingElement));
            int fbIdx = indexOf(furthestBlock);
            insertAt(fbIdx + 1, replacement, HtmlTags.id(subject), NS_HTML);
        }
    }

    /**
     * Computes the appropriate place for inserting a node with an explicit target element that is on the stack.
     * <p>
     * 스택에 있는 명시적 대상 요소를 기준으로 알맞은 삽입 위치를 계산한다.
     *
     * @param target element / 요소
     * @param idx    its stack index (it may have shifted, so it is looked up again) / 스택 위치(밀렸을 수 있어 다시 찾는다)
     */
    private void computeLocationFor(Element target, int idx) {
        int i = indexOf(target);
        computeLocation(i >= 0 ? i : idx);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 삽입 모드 재설정
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the current template insertion mode (the top of the stack of template insertion modes).
     * <p>
     * 현재 template 삽입 모드(template 삽입 모드 스택의 맨 위)를 반환한다.
     *
     * @return the mode / 모드
     */
    final int currentTemplateMode() {
        return templateModes[templateCount - 1];
    }

    /**
     * Pushes a template insertion mode.
     * <p>
     * template 삽입 모드를 넣는다.
     *
     * @param m mode / 모드
     */
    final void pushTemplateMode(int m) {
        if (templateCount == templateModes.length) {
            int[] n = new int[templateCount * 2];
            System.arraycopy(templateModes, 0, n, 0, templateCount);
            templateModes = n;
        }
        templateModes[templateCount++] = m;
    }

    /**
     * "Reset the insertion mode appropriately".
     * <p>
     * 스택을 보고 삽입 모드를 다시 정한다("reset the insertion mode appropriately").
     */
    final void resetInsertionMode() {
        boolean last = false;
        // 스택을 아래에서 위로 훑으며 첫 번째로 모드를 정할 수 있는 요소를 찾는다.
        for (int i = stackSize - 1; i >= 0; i--) {
            int id = stackId[i];
            int ns = stackNs[i];
            if (i == 0) {
                last = true;
                if (fragment) {
                    id = contextId;
                    ns = contextNs;
                }
            }
            if (ns == NS_HTML) {
                switch (id) {
                case HtmlTags.SELECT:
                    mode = IN_SELECT;
                    if (!last) {
                        // 조상 중에 table 이 있으면 in select in table, template 을 만나면 in select 이다.
                        for (int a = i - 1; a >= 0; a--) {
                            if (isHtmlAt(a, HtmlTags.TEMPLATE)) {
                                break;
                            }
                            if (isHtmlAt(a, HtmlTags.TABLE)) {
                                mode = IN_SELECT_IN_TABLE;
                                break;
                            }
                        }
                    }
                    return;
                case HtmlTags.TD:
                case HtmlTags.TH:
                    if (!last) {
                        mode = IN_CELL;
                        return;
                    }
                    break;
                case HtmlTags.TR:
                    mode = IN_ROW;
                    return;
                case HtmlTags.TBODY:
                case HtmlTags.THEAD:
                case HtmlTags.TFOOT:
                    mode = IN_TABLE_BODY;
                    return;
                case HtmlTags.CAPTION:
                    mode = IN_CAPTION;
                    return;
                case HtmlTags.COLGROUP:
                    mode = IN_COLUMN_GROUP;
                    return;
                case HtmlTags.TABLE:
                    mode = IN_TABLE;
                    return;
                case HtmlTags.TEMPLATE:
                    mode = currentTemplateMode();
                    return;
                case HtmlTags.HEAD:
                    if (!last) {
                        mode = IN_HEAD;
                        return;
                    }
                    break;
                case HtmlTags.BODY:
                    mode = IN_BODY;
                    return;
                case HtmlTags.FRAMESET:
                    mode = IN_FRAMESET;
                    return;
                case HtmlTags.HTML:
                    mode = headPointer == null ? BEFORE_HEAD : AFTER_HEAD;
                    return;
                default:
                    break;
                }
            }
            if (last) {
                mode = IN_BODY;
                return;
            }
        }
        mode = IN_BODY;
    }
}
