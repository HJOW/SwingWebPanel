package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentType;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The tree construction stage of the HTML parser: the 23 insertion modes of the HTML Standard ("initial" to "after after frameset"),
 * the tree construction dispatcher that decides between the HTML insertion mode rules and the rules for foreign content (SVG and
 * MathML), the fragment parsing set-up and "stop parsing". It builds the DOM of <code>org.duckdns.hjow.swingwebpanel.dom</code>, recovers from misnested
 * and incomplete markup exactly as the standard describes (including the adoption agency algorithm, foster parenting, template
 * insertion modes and the legacy "in select" handling) and tells the {@link ParserListener} about scripts, style sheets, images and
 * other resources the document refers to.
 * <p>
 * Differences from the newest draft of the standard are listed in <code>docs/progress/html.md</code>: sanitizer hooks, custom elements,
 * declarative shadow roots and speculative parsing are not implemented, and <code>select</code> keeps its classic insertion modes.
 * <p>
 * HTML 파서의 트리 생성 단계: HTML 표준의 23 개 삽입 모드("initial" 부터 "after after frameset" 까지), HTML 삽입 모드 규칙과 외부 콘텐츠(SVG, MathML)
 * 규칙 중 무엇을 쓸지 정하는 트리 생성 디스패처, 조각 파싱 준비, "stop parsing". <code>org.duckdns.hjow.swingwebpanel.dom</code> 의 DOM 을 만들고, 잘못 중첩되거나 불완전한
 * 마크업을 표준이 정한 대로 복구하며(adoption agency 알고리즘, foster parenting, template 삽입 모드, 옛 "in select" 처리 포함) 스크립트, 스타일시트, 이미지
 * 등 문서가 참조하는 자원을 {@link ParserListener} 에 알린다.
 * <p>
 * 표준 최신 초안과의 차이는 <code>docs/progress/html.md</code> 에 있다: sanitizer 훅, 사용자 정의 요소, 선언적 shadow root, 추측 파싱은 구현하지 않았고
 * <code>select</code> 는 고전적인 삽입 모드를 유지한다.
 */
final class HtmlTreeBuilder extends TreeBuilderBase {

    /**
     * Character class: NUL.
     * <p>
     * 문자 부류: NUL.
     */
    private static final int CLS_NUL = 0;

    /**
     * Character class: ASCII white space.
     * <p>
     * 문자 부류: ASCII 공백.
     */
    private static final int CLS_WS = 1;

    /**
     * Character class: any other character.
     * <p>
     * 문자 부류: 그 밖의 문자.
     */
    private static final int CLS_OTHER = 2;

    /**
     * True if the next character token must lose a leading line feed (after pre, listing and textarea start tags).
     * <p>
     * 다음 문자 토큰의 맨 앞 줄바꿈을 버려야 하면 true(pre, listing, textarea 시작 태그 뒤).
     */
    private boolean skipLf;

    /**
     * The pending table character tokens of the "in table text" insertion mode.
     * <p>
     * "in table text" 삽입 모드의 pending table character tokens.
     */
    private final StringBuilder pendingTable = new StringBuilder();

    /**
     * Creates a tree builder.
     * <p>
     * 트리 생성기를 만든다.
     *
     * @param doc       document / 문서
     * @param tokenizer tokenizer / 토큰화기
     * @param log       error collector / 오류 수집기
     * @param listener  listener or null / 리스너 또는 null
     * @param control   parser control / 파서 제어
     * @param host      host or null / 호스트 또는 null
     * @param scripting parser scripting flag / parser scripting 플래그
     */
    HtmlTreeBuilder(Document doc, HtmlTokenizer tokenizer, ParseErrorLog log, ParserListener listener, ParserControl control, ParserHost host,
            boolean scripting) {
        super(doc, tokenizer, log, listener, control, host, scripting);
    }

    /**
     * Sets up the fragment case ("HTML fragment parsing algorithm"): the root <code>html</code> element, the template insertion mode, the
     * form element pointer and the insertion mode that depends on the context element. The caller sets the tokenizer state.
     * <p>
     * 조각 파싱을 준비한다("HTML fragment parsing algorithm"): 루트 <code>html</code> 요소, template 삽입 모드, form 요소 포인터, 컨텍스트 요소에 따른
     * 삽입 모드. 토큰화기 상태는 호출자가 지정한다.
     *
     * @param context the context element / 컨텍스트 요소
     * @return the root element that receives the parsed nodes / 파싱된 노드를 받는 루트 요소
     */
    Element startFragment(Element context) {
        fragment = true;
        contextElement = context;
        contextNs = nsOf(context);
        contextId = HtmlTags.id(lower(context.getLocalName()));
        Element root = doc.createElementInternal(NS_URI[NS_HTML], null, "html");
        push(root, HtmlTags.HTML, NS_HTML);
        if (contextNs == NS_HTML && contextId == HtmlTags.TEMPLATE) {
            pushTemplateMode(IN_TEMPLATE);
        }
        resetInsertionMode();
        // 컨텍스트 요소부터 위로 올라가며 가장 가까운 form 을 form 요소 포인터로 삼는다.
        for (Node n = context; n != null; n = n.getParentNode()) {
            if (n instanceof Element && NS_URI[NS_HTML].equals(((Element) n).getNamespaceURI()) && "form".equals(((Element) n).getLocalName())) {
                formPointer = (Element) n;
                break;
            }
        }
        return root;
    }

    /**
     * Returns the namespace code of an element.
     * <p>
     * 요소의 네임스페이스 코드를 반환한다.
     *
     * @param e element / 요소
     * @return NS_HTML, NS_SVG or NS_MATHML (other namespaces count as HTML) / 네임스페이스 코드(다른 네임스페이스는 HTML 로 본다)
     */
    private static int nsOf(Element e) {
        String ns = e.getNamespaceURI();
        if (NS_URI[NS_SVG].equals(ns)) {
            return NS_SVG;
        }
        return NS_URI[NS_MATHML].equals(ns) ? NS_MATHML : NS_HTML;
    }

    /**
     * Returns the adjusted current node's namespace code and id as a pair, or null when no element is open.
     * <p>
     * 조정된 현재 노드의 네임스페이스 코드와 번호를 반환한다. 열린 요소가 없으면 null.
     *
     * @return the "adjusted current node" element / 조정된 현재 노드 요소
     */
    private Element adjustedCurrentNode() {
        if (stackSize == 0) {
            return null;
        }
        return fragment && stackSize == 1 ? contextElement : stackEl[stackSize - 1];
    }

    /**
     * Returns the namespace code of the adjusted current node.
     * <p>
     * 조정된 현재 노드의 네임스페이스 코드를 반환한다.
     *
     * @return namespace code / 네임스페이스 코드
     */
    int adjustedNs() {
        return fragment && stackSize == 1 ? contextNs : (stackSize == 0 ? NS_HTML : stackNs[stackSize - 1]);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 디스패처
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Processes one token from the tokenizer.
     * <p>
     * 토큰화기가 낸 토큰 하나를 처리한다.
     *
     * @param t the token / 토큰
     */
    void process(HtmlToken t) {
        if (stopped) {
            return;
        }
        tokenOffset = t.offset;
        if (t.type == HtmlToken.CHARACTER) {
            String data = t.data;
            if (skipLf) {
                skipLf = false;
                if (data.charAt(0) == '\n') {
                    data = data.substring(1);
                    if (data.length() == 0) {
                        return;
                    }
                }
            }
            characters(data);
            return;
        }
        skipLf = false;
        flushText();
        dispatch(t);
        if (t.type == HtmlToken.START_TAG && t.selfClosing && !t.selfClosingAcked) {
            err("non-void-html-element-start-tag-with-trailing-solidus");
        }
    }

    /**
     * Splits character data into runs of one class (NUL, white space, other) and handles each run.
     * <p>
     * 문자 데이터를 부류(NUL, 공백, 그 밖)가 같은 구간으로 나누어 구간마다 처리한다.
     *
     * @param data characters / 문자
     */
    private void characters(String data) {
        int n = data.length();
        int i = 0;
        // 같은 부류가 이어지는 구간을 찾아 하나씩 처리한다.
        while (i < n && !stopped) {
            int cls = classOf(data.charAt(i));
            int j = i + 1;
            // 같은 부류가 이어지는 동안 구간의 끝을 늘린다.
            while (j < n && classOf(data.charAt(j)) == cls) {
                j++;
            }
            charsDispatch(i == 0 && j == n ? data : data.substring(i, j), cls);
            i = j;
        }
    }

    /**
     * Returns the class of a character.
     * <p>
     * 문자의 부류를 반환한다.
     *
     * @param c character / 문자
     * @return one of the CLS constants / CLS 상수 중 하나
     */
    private static int classOf(char c) {
        if (c == 0) {
            return CLS_NUL;
        }
        return c == ' ' || c == '\n' || c == '\t' || c == '\f' || c == '\r' ? CLS_WS : CLS_OTHER;
    }

    /**
     * Tells whether the element is an HTML integration point.
     * <p>
     * 요소가 HTML 통합 지점인지 알려 준다.
     *
     * @param node element / 요소
     * @param id   tag id / 태그 번호
     * @param ns   namespace code / 네임스페이스 코드
     * @return true if so / 그러면 true
     */
    private static boolean isHtmlIntegrationPoint(Element node, int id, int ns) {
        if (ns == NS_SVG) {
            return id == HtmlTags.FOREIGNOBJECT || id == HtmlTags.DESC || id == HtmlTags.TITLE;
        }
        if (ns == NS_MATHML && id == HtmlTags.ANNOTATION_XML) {
            String enc = node.getAttribute("encoding");
            return enc != null && ("text/html".equals(lower(enc)) || "application/xhtml+xml".equals(lower(enc)));
        }
        return false;
    }

    /**
     * The decision of the tree construction dispatcher: true if the token must be handled by the rules for foreign content.
     * <p>
     * 트리 생성 디스패처의 판단: 토큰을 외부 콘텐츠 규칙으로 처리해야 하면 true.
     *
     * @param type token type / 토큰 종류
     * @param tid  tag id of a start tag / 시작 태그의 태그 번호
     * @return true for foreign content rules / 외부 콘텐츠 규칙이면 true
     */
    private boolean useForeign(int type, int tid) {
        if (stackSize == 0 || type == HtmlToken.EOF) {
            return false;
        }
        Element node = adjustedCurrentNode();
        int ns = adjustedNs();
        if (ns == NS_HTML) {
            return false;
        }
        int id = fragment && stackSize == 1 ? contextId : stackId[stackSize - 1];
        if (ns == NS_MATHML && (id == HtmlTags.MI || id == HtmlTags.MO || id == HtmlTags.MN || id == HtmlTags.MS || id == HtmlTags.MTEXT)) {
            if (type == HtmlToken.START_TAG && tid != HtmlTags.MGLYPH && tid != HtmlTags.MALIGNMARK) {
                return false;
            }
            if (type == HtmlToken.CHARACTER) {
                return false;
            }
        }
        if (ns == NS_MATHML && id == HtmlTags.ANNOTATION_XML && type == HtmlToken.START_TAG && tid == HtmlTags.SVG) {
            return false;
        }
        return !(isHtmlIntegrationPoint(node, id, ns) && (type == HtmlToken.START_TAG || type == HtmlToken.CHARACTER));
    }

    /**
     * Runs the tree construction dispatcher for a token that is not a character.
     * <p>
     * 문자가 아닌 토큰에 대해 트리 생성 디스패처를 실행한다.
     *
     * @param t token / 토큰
     */
    private void dispatch(HtmlToken t) {
        if (stopped) {
            return;
        }
        if (useForeign(t.type, t.type == HtmlToken.START_TAG ? tid(t) : 0)) {
            foreignToken(t);
        } else {
            htmlToken(t);
        }
    }

    /**
     * Runs the tree construction dispatcher for a run of characters.
     * <p>
     * 문자 구간에 대해 트리 생성 디스패처를 실행한다.
     *
     * @param s   characters / 문자
     * @param cls character class / 문자 부류
     */
    private void charsDispatch(String s, int cls) {
        if (stopped) {
            return;
        }
        if (useForeign(HtmlToken.CHARACTER, 0)) {
            foreignChars(s, cls);
        } else {
            htmlChars(s, cls);
        }
    }

    /**
     * Handles a non-character token with the rules of the current insertion mode.
     * <p>
     * 문자가 아닌 토큰을 현재 삽입 모드의 규칙으로 처리한다.
     *
     * @param t token / 토큰
     */
    private void htmlToken(HtmlToken t) {
        switch (mode) {
        case INITIAL:
            initial(t);
            break;
        case BEFORE_HTML:
            beforeHtml(t);
            break;
        case BEFORE_HEAD:
            beforeHead(t);
            break;
        case IN_HEAD:
            inHead(t);
            break;
        case IN_HEAD_NOSCRIPT:
            inHeadNoscript(t);
            break;
        case AFTER_HEAD:
            afterHead(t);
            break;
        case IN_BODY:
            inBody(t);
            break;
        case TEXT:
            text(t);
            break;
        case IN_TABLE:
            inTable(t);
            break;
        case IN_TABLE_TEXT:
            flushTableText();
            mode = originalMode;
            dispatch(t);
            break;
        case IN_CAPTION:
            inCaption(t);
            break;
        case IN_COLUMN_GROUP:
            inColumnGroup(t);
            break;
        case IN_TABLE_BODY:
            inTableBody(t);
            break;
        case IN_ROW:
            inRow(t);
            break;
        case IN_CELL:
            inCell(t);
            break;
        case IN_SELECT:
            inSelect(t);
            break;
        case IN_SELECT_IN_TABLE:
            inSelectInTable(t);
            break;
        case IN_TEMPLATE:
            inTemplate(t);
            break;
        case AFTER_BODY:
            afterBody(t);
            break;
        case IN_FRAMESET:
            inFrameset(t);
            break;
        case AFTER_FRAMESET:
            afterFrameset(t);
            break;
        case AFTER_AFTER_BODY:
            afterAfterBody(t);
            break;
        default:
            afterAfterFrameset(t);
            break;
        }
    }

    /**
     * Handles characters with the rules of the current insertion mode.
     * <p>
     * 문자를 현재 삽입 모드의 규칙으로 처리한다.
     *
     * @param s   characters / 문자
     * @param cls character class / 문자 부류
     */
    private void htmlChars(String s, int cls) {
        switch (mode) {
        case INITIAL:
            if (cls != CLS_WS) {
                missingDoctype();
                mode = BEFORE_HTML;
                charsDispatch(s, cls);
            }
            break;
        case BEFORE_HTML:
            if (cls != CLS_WS) {
                createRootHtml(null);
                mode = BEFORE_HEAD;
                charsDispatch(s, cls);
            }
            break;
        case BEFORE_HEAD:
            if (cls != CLS_WS) {
                headPointer = insertHtml("head");
                mode = IN_HEAD;
                charsDispatch(s, cls);
            }
            break;
        case IN_HEAD:
            if (cls == CLS_WS) {
                insertChars(s);
            } else {
                pop();
                mode = AFTER_HEAD;
                charsDispatch(s, cls);
            }
            break;
        case IN_HEAD_NOSCRIPT:
            if (cls == CLS_WS) {
                insertChars(s);
            } else {
                err("unexpected-character");
                pop();
                mode = IN_HEAD;
                charsDispatch(s, cls);
            }
            break;
        case AFTER_HEAD:
            if (cls == CLS_WS) {
                insertChars(s);
            } else {
                insertHtml("body");
                framesetOk = true;
                mode = IN_BODY;
                charsDispatch(s, cls);
            }
            break;
        case TEXT:
            insertChars(s);
            break;
        case IN_TABLE:
            tableChars(s, cls);
            break;
        case IN_TABLE_TEXT:
            if (cls == CLS_NUL) {
                err("unexpected-null-character");
            } else {
                pendingTable.append(s);
            }
            break;
        case IN_COLUMN_GROUP:
            if (cls == CLS_WS) {
                insertChars(s);
            } else if (!currentIs(HtmlTags.COLGROUP)) {
                err("unexpected-character");
            } else {
                pop();
                mode = IN_TABLE;
                charsDispatch(s, cls);
            }
            break;
        case IN_TABLE_BODY:
        case IN_ROW:
            tableChars(s, cls);
            break;
        case IN_SELECT:
        case IN_SELECT_IN_TABLE:
            if (cls == CLS_NUL) {
                err("unexpected-null-character");
            } else {
                insertChars(s);
            }
            break;
        case AFTER_BODY:
        case AFTER_AFTER_BODY:
            if (cls == CLS_WS) {
                bodyChars(s, cls);
            } else {
                err("unexpected-character");
                mode = IN_BODY;
                charsDispatch(s, cls);
            }
            break;
        case IN_FRAMESET:
        case AFTER_FRAMESET:
            if (cls == CLS_WS) {
                insertChars(s);
            } else {
                err("unexpected-character");
            }
            break;
        case AFTER_AFTER_FRAMESET:
            if (cls == CLS_WS) {
                bodyChars(s, cls);
            } else {
                err("unexpected-character");
            }
            break;
        default:
            // IN_BODY, IN_CAPTION, IN_CELL, IN_TEMPLATE
            bodyChars(s, cls);
            break;
        }
    }

    /**
     * Characters with the rules of the "in body" insertion mode.
     * <p>
     * "in body" 삽입 모드 규칙의 문자 처리.
     *
     * @param s   characters / 문자
     * @param cls character class / 문자 부류
     */
    private void bodyChars(String s, int cls) {
        if (cls == CLS_NUL) {
            err("unexpected-null-character");
            return;
        }
        reconstructFormatting();
        insertChars(s);
        if (cls == CLS_OTHER) {
            framesetOk = false;
        }
    }

    /**
     * Characters of the table modes (the "in table" insertion mode rules).
     * <p>
     * 표 관련 모드의 문자 처리("in table" 삽입 모드 규칙).
     *
     * @param s   characters / 문자
     * @param cls character class / 문자 부류
     */
    private void tableChars(String s, int cls) {
        if (currentIs(HtmlTags.TABLE) || currentIs(HtmlTags.TBODY) || currentIs(HtmlTags.TEMPLATE) || currentIs(HtmlTags.TFOOT)
                || currentIs(HtmlTags.THEAD) || currentIs(HtmlTags.TR)) {
            pendingTable.setLength(0);
            originalMode = mode;
            mode = IN_TABLE_TEXT;
            charsDispatch(s, cls);
            return;
        }
        err("unexpected-character-in-table");
        fosterParenting = true;
        bodyChars(s, cls);
        fosterParenting = false;
    }

    /**
     * Ends the "in table text" insertion mode: white space is inserted as it is, any other text is foster parented.
     * <p>
     * "in table text" 삽입 모드를 마친다: 공백뿐이면 그대로 삽입하고 아니면 foster parenting 으로 처리한다.
     */
    private void flushTableText() {
        String p = pendingTable.toString();
        pendingTable.setLength(0);
        if (p.length() == 0) {
            return;
        }
        boolean allWs = true;
        // 공백이 아닌 문자가 있는지 확인한다.
        for (int i = 0; i < p.length(); i++) {
            if (classOf(p.charAt(i)) != CLS_WS) {
                allWs = false;
                break;
            }
        }
        if (allWs) {
            insertChars(p);
            return;
        }
        err("unexpected-character-in-table");
        fosterParenting = true;
        int i = 0;
        // 부류가 같은 구간마다 "in body" 규칙으로 foster parenting 하며 처리한다.
        while (i < p.length()) {
            int cls = classOf(p.charAt(i));
            int j = i + 1;
            // 같은 부류가 이어지는 동안 구간의 끝을 늘린다.
            while (j < p.length() && classOf(p.charAt(j)) == cls) {
                j++;
            }
            bodyChars(p.substring(i, j), cls);
            i = j;
        }
        fosterParenting = false;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 공통 도우미
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Insert a comment from a comment token at the appropriate place.
     * <p>
     * 주석 토큰의 주석을 알맞은 위치에 삽입한다.
     *
     * @param t comment token / 주석 토큰
     */
    private void comment(HtmlToken t) {
        insertComment(t.data, null);
    }

    /**
     * Acknowledges the self-closing flag of a start tag.
     * <p>
     * 시작 태그의 self-closing 플래그를 처리했다고 표시한다.
     *
     * @param t start tag / 시작 태그
     */
    private static void ack(HtmlToken t) {
        t.selfClosingAcked = true;
    }

    /**
     * Inserts a void HTML element: insert, pop and acknowledge.
     * <p>
     * void HTML 요소를 삽입한다: 삽입하고 꺼내고 플래그를 처리했다고 표시한다.
     *
     * @param t start tag / 시작 태그
     */
    private void insertVoid(HtmlToken t) {
        insertHtml(t);
        pop();
        ack(t);
    }

    /**
     * Records the "missing DOCTYPE" case: quirks mode unless the document is an iframe srcdoc document.
     * <p>
     * DOCTYPE 이 없는 경우를 기록한다: iframe srcdoc 문서가 아니면 quirks 모드.
     */
    private void missingDoctype() {
        if (!srcdoc) {
            err("missing-doctype");
            doc.setCompatibilityMode(Document.QUIRKS_MODE);
        }
        if (listener != null) {
            listener.documentModeDetermined(doc.getCompatibilityMode());
        }
    }

    /**
     * Creates the root <code>html</code> element, appends it to the document and pushes it.
     * <p>
     * 루트 <code>html</code> 요소를 만들어 문서에 붙이고 스택에 넣는다.
     *
     * @param t start tag token, or null for an html element without attributes / 시작 태그 토큰. 속성이 없는 html 요소이면 null
     */
    private void createRootHtml(HtmlToken t) {
        Element el = createElement("html", t, NS_HTML, ATTRS_HTML);
        tryInsert(doc, null, el);
        push(el, HtmlTags.HTML, NS_HTML);
        if (listener != null) {
            listener.elementInserted(el);
        }
    }

    /**
     * "Close a p element".
     * <p>
     * p 요소를 닫는다("close a p element").
     */
    private void closeP() {
        generateImpliedEndTags("p");
        if (!currentIs(HtmlTags.P)) {
            err("unexpected-open-element");
        }
        popUntil(HtmlTags.P);
    }

    /**
     * Closes a p element if the stack has one in button scope.
     * <p>
     * 스택에 button 범위의 p 요소가 있으면 닫는다.
     */
    private void closePIfInButtonScope() {
        if (inScope(HtmlTags.P, SCOPE_BUTTON)) {
            closeP();
        }
    }

    /**
     * Follows the generic raw text or RCDATA element parsing algorithm.
     * <p>
     * generic raw text 또는 RCDATA 요소 파싱 알고리즘을 따른다.
     *
     * @param t     start tag / 시작 태그
     * @param state tokenizer state (RAWTEXT or RCDATA) / 토큰화기 상태(RAWTEXT 또는 RCDATA)
     */
    private void genericText(HtmlToken t, int state) {
        insertHtml(t);
        tokenizer.setState(state);
        originalMode = mode;
        mode = TEXT;
    }

    /**
     * "Stop parsing": the stack is emptied and the parse ends.
     * <p>
     * "Stop parsing": 스택을 비우고 파싱을 끝낸다.
     */
    void stopParsing() {
        flushText();
        // 열려 있는 모든 요소를 꺼낸다.
        while (stackSize > 0) {
            pop();
        }
        stopped = true;
    }

    /**
     * Reports a parse error if the stack has an element that must not be open at the end of the body or the document.
     * <p>
     * body 나 문서의 끝에서 열려 있으면 안 되는 요소가 스택에 있으면 파싱 오류를 보고한다.
     */
    private void checkOpenAtEnd() {
        // 스택의 모든 요소가 닫지 않아도 되는 요소인지 확인한다.
        for (int i = 0; i < stackSize; i++) {
            if (!(stackNs[i] == NS_HTML && HtmlTags.isOpenAllowedAtEnd(stackId[i]))) {
                err("unexpected-open-element-at-end");
                return;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // initial, before html, before head
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The "initial" insertion mode.
     * <p>
     * "initial" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void initial(HtmlToken t) {
        if (t.type == HtmlToken.COMMENT) {
            insertComment(t.data, doc);
        } else if (t.type == HtmlToken.DOCTYPE) {
            if (!"html".equals(t.name) || t.publicId != null || (t.systemId != null && !"about:legacy-compat".equals(t.systemId))) {
                err("unexpected-doctype");
            }
            DocumentType dt = doc.createDocumentType(t.name == null ? "" : t.name, t.publicId == null ? "" : t.publicId,
                    t.systemId == null ? "" : t.systemId);
            if (doc.getDoctype() == null && doc.getDocumentElement() == null) {
                tryInsert(doc, null, dt);
            }
            if (!srcdoc) {
                doc.setCompatibilityMode(DocumentModes.fromDoctype(t.name, t.publicId, t.systemId, t.forceQuirks));
            }
            if (listener != null) {
                listener.documentModeDetermined(doc.getCompatibilityMode());
            }
            mode = BEFORE_HTML;
        } else {
            missingDoctype();
            mode = BEFORE_HTML;
            dispatch(t);
        }
    }

    /**
     * The "before html" insertion mode.
     * <p>
     * "before html" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void beforeHtml(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
        } else if (t.type == HtmlToken.COMMENT) {
            insertComment(t.data, doc);
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML) {
            createRootHtml(t);
            mode = BEFORE_HEAD;
        } else if (t.type == HtmlToken.END_TAG && id != HtmlTags.HEAD && id != HtmlTags.BODY && id != HtmlTags.HTML && id != HtmlTags.BR) {
            err("unexpected-end-tag");
        } else {
            createRootHtml(null);
            mode = BEFORE_HEAD;
            dispatch(t);
        }
    }

    /**
     * The "before head" insertion mode.
     * <p>
     * "before head" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void beforeHead(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
        } else if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML) {
            inBody(t);
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.HEAD) {
            headPointer = insertHtml(t);
            mode = IN_HEAD;
        } else if (t.type == HtmlToken.END_TAG && id != HtmlTags.HEAD && id != HtmlTags.BODY && id != HtmlTags.HTML && id != HtmlTags.BR) {
            err("unexpected-end-tag");
        } else {
            headPointer = insertHtml("head");
            mode = IN_HEAD;
            dispatch(t);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // in head, in head noscript, after head
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The "in head" insertion mode.
     * <p>
     * "in head" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inHead(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
            return;
        }
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.HTML:
                inBody(t);
                return;
            case HtmlTags.BASE:
            case HtmlTags.BASEFONT:
            case HtmlTags.BGSOUND:
            case HtmlTags.LINK:
            case HtmlTags.META:
                insertVoid(t);
                return;
            case HtmlTags.TITLE:
                genericText(t, HtmlTokenizer.RCDATA);
                return;
            case HtmlTags.NOSCRIPT:
                if (scripting) {
                    genericText(t, HtmlTokenizer.RAWTEXT);
                } else {
                    insertHtml(t);
                    mode = IN_HEAD_NOSCRIPT;
                }
                return;
            case HtmlTags.NOFRAMES:
            case HtmlTags.STYLE:
                genericText(t, HtmlTokenizer.RAWTEXT);
                return;
            case HtmlTags.SCRIPT:
                insertHtml(t);
                tokenizer.setState(HtmlTokenizer.SCRIPT_DATA);
                originalMode = mode;
                mode = TEXT;
                return;
            case HtmlTags.TEMPLATE:
                insertMarker();
                framesetOk = false;
                mode = IN_TEMPLATE;
                pushTemplateMode(IN_TEMPLATE);
                insertHtml(t);
                return;
            case HtmlTags.HEAD:
                err("unexpected-start-tag");
                return;
            default:
                break;
            }
        } else if (t.type == HtmlToken.END_TAG) {
            if (id == HtmlTags.HEAD) {
                pop();
                mode = AFTER_HEAD;
                return;
            }
            if (id == HtmlTags.TEMPLATE) {
                templateEnd();
                return;
            }
            if (id != HtmlTags.BODY && id != HtmlTags.HTML && id != HtmlTags.BR) {
                err("unexpected-end-tag");
                return;
            }
        }
        pop();
        mode = AFTER_HEAD;
        dispatch(t);
    }

    /**
     * The end tag of a template element (shared by the modes that process it "using the rules for in head").
     * <p>
     * template 요소의 종료 태그(in head 규칙으로 처리하는 모드들이 공유한다).
     */
    private void templateEnd() {
        if (!stackHas(HtmlTags.TEMPLATE)) {
            err("unexpected-end-tag");
            return;
        }
        generateImpliedEndTagsThoroughly();
        if (!currentIs(HtmlTags.TEMPLATE)) {
            err("unexpected-open-element");
        }
        popUntil(HtmlTags.TEMPLATE);
        clearFormattingToMarker();
        templateCount--;
        resetInsertionMode();
    }

    /**
     * The "in head noscript" insertion mode.
     * <p>
     * "in head noscript" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inHeadNoscript(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.COMMENT) {
            inHead(t);
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            if (id == HtmlTags.HTML) {
                inBody(t);
                return;
            }
            if (id == HtmlTags.BASEFONT || id == HtmlTags.BGSOUND || id == HtmlTags.LINK || id == HtmlTags.META || id == HtmlTags.NOFRAMES
                    || id == HtmlTags.STYLE) {
                inHead(t);
                return;
            }
            if (id == HtmlTags.HEAD || id == HtmlTags.NOSCRIPT) {
                err("unexpected-start-tag");
                return;
            }
        } else if (t.type == HtmlToken.END_TAG) {
            if (id == HtmlTags.NOSCRIPT) {
                pop();
                mode = IN_HEAD;
                return;
            }
            if (id != HtmlTags.BR) {
                err("unexpected-end-tag");
                return;
            }
        }
        err("unexpected-token-in-noscript");
        pop();
        mode = IN_HEAD;
        dispatch(t);
    }

    /**
     * The "after head" insertion mode.
     * <p>
     * "after head" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void afterHead(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
            return;
        }
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.HTML:
                inBody(t);
                return;
            case HtmlTags.BODY:
                insertHtml(t);
                framesetOk = false;
                mode = IN_BODY;
                return;
            case HtmlTags.FRAMESET:
                insertHtml(t);
                mode = IN_FRAMESET;
                return;
            case HtmlTags.BASE:
            case HtmlTags.BASEFONT:
            case HtmlTags.BGSOUND:
            case HtmlTags.LINK:
            case HtmlTags.META:
            case HtmlTags.NOFRAMES:
            case HtmlTags.SCRIPT:
            case HtmlTags.STYLE:
            case HtmlTags.TEMPLATE:
            case HtmlTags.TITLE:
                err("unexpected-start-tag");
                // head 요소를 잠시 스택에 되돌려 놓고 in head 규칙으로 처리한 뒤 다시 뺀다.
                push(headPointer, HtmlTags.HEAD, NS_HTML);
                inHead(t);
                int hi = indexOf(headPointer);
                if (hi >= 0) {
                    removeAt(hi);
                }
                return;
            case HtmlTags.HEAD:
                err("unexpected-start-tag");
                return;
            default:
                break;
            }
        } else if (t.type == HtmlToken.END_TAG) {
            if (id == HtmlTags.TEMPLATE) {
                inHead(t);
                return;
            }
            if (id != HtmlTags.BODY && id != HtmlTags.HTML && id != HtmlTags.BR) {
                err("unexpected-end-tag");
                return;
            }
        }
        insertHtml("body");
        framesetOk = true;
        mode = IN_BODY;
        dispatch(t);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // in body
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Adds the attributes of a token that the element does not have yet (the second <code>html</code> or <code>body</code> tag).
     * <p>
     * 토큰의 속성 중 요소에 아직 없는 것을 더한다(두 번째 <code>html</code> 또는 <code>body</code> 태그).
     *
     * @param el element / 요소
     * @param t  token / 토큰
     */
    private void mergeAttributes(Element el, HtmlToken t) {
        // 토큰의 속성마다 요소에 같은 이름이 없을 때만 추가한다.
        for (int i = 0; i < t.getAttributeCount(); i++) {
            String name = t.attrNames.get(i);
            if (!el.hasAttribute(name)) {
                try {
                    el.setAttributeUnchecked(name, t.attrValues.get(i));
                } catch (org.duckdns.hjow.swingwebpanel.dom.DomException ex) {
                    err("invalid-attribute-name");
                }
            }
        }
    }

    /**
     * The "in body" insertion mode.
     * <p>
     * "in body" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inBody(HtmlToken t) {
        switch (t.type) {
        case HtmlToken.COMMENT:
            comment(t);
            break;
        case HtmlToken.DOCTYPE:
            err("unexpected-doctype");
            break;
        case HtmlToken.START_TAG:
            inBodyStart(t);
            break;
        case HtmlToken.END_TAG:
            inBodyEnd(t);
            break;
        default:
            inBodyEof(t);
            break;
        }
    }

    /**
     * End of the file in the "in body" insertion mode (also used by the modes that defer to it).
     * <p>
     * "in body" 삽입 모드에서의 파일 끝(그것에 위임하는 모드들도 쓴다).
     *
     * @param t EOF token / EOF 토큰
     */
    private void inBodyEof(HtmlToken t) {
        if (templateCount > 0) {
            inTemplate(t);
            return;
        }
        checkOpenAtEnd();
        stopParsing();
    }

    /**
     * Start tags of the "in body" insertion mode.
     * <p>
     * "in body" 삽입 모드의 시작 태그.
     *
     * @param t start tag / 시작 태그
     */
    private void inBodyStart(HtmlToken t) {
        int id = tid(t);
        switch (id) {
        case HtmlTags.HTML:
            err("unexpected-start-tag");
            if (!stackHas(HtmlTags.TEMPLATE)) {
                mergeAttributes(stackEl[0], t);
            }
            return;
        case HtmlTags.BASE:
        case HtmlTags.BASEFONT:
        case HtmlTags.BGSOUND:
        case HtmlTags.LINK:
        case HtmlTags.META:
        case HtmlTags.NOFRAMES:
        case HtmlTags.SCRIPT:
        case HtmlTags.STYLE:
        case HtmlTags.TEMPLATE:
        case HtmlTags.TITLE:
            inHead(t);
            return;
        case HtmlTags.BODY:
            err("unexpected-start-tag");
            if (stackSize == 1 || !isHtmlAt(1, HtmlTags.BODY) || stackHas(HtmlTags.TEMPLATE)) {
                return;
            }
            framesetOk = false;
            mergeAttributes(stackEl[1], t);
            return;
        case HtmlTags.FRAMESET:
            err("unexpected-start-tag");
            if (stackSize == 1 || !isHtmlAt(1, HtmlTags.BODY) || !framesetOk) {
                return;
            }
            Node bodyParent = stackEl[1].getParentNode();
            if (bodyParent != null) {
                bodyParent.removeChild(stackEl[1]);
            }
            // 루트 html 요소만 남을 때까지 꺼낸다.
            while (stackSize > 1) {
                pop();
            }
            insertHtml(t);
            mode = IN_FRAMESET;
            return;
        case HtmlTags.ADDRESS:
        case HtmlTags.ARTICLE:
        case HtmlTags.ASIDE:
        case HtmlTags.BLOCKQUOTE:
        case HtmlTags.CENTER:
        case HtmlTags.DETAILS:
        case HtmlTags.DIALOG:
        case HtmlTags.DIR:
        case HtmlTags.DIV:
        case HtmlTags.DL:
        case HtmlTags.FIELDSET:
        case HtmlTags.FIGCAPTION:
        case HtmlTags.FIGURE:
        case HtmlTags.FOOTER:
        case HtmlTags.HEADER:
        case HtmlTags.HGROUP:
        case HtmlTags.MAIN:
        case HtmlTags.MENU:
        case HtmlTags.NAV:
        case HtmlTags.OL:
        case HtmlTags.P:
        case HtmlTags.SEARCH:
        case HtmlTags.SECTION:
        case HtmlTags.SUMMARY:
        case HtmlTags.UL:
            closePIfInButtonScope();
            insertHtml(t);
            return;
        case HtmlTags.H1:
        case HtmlTags.H2:
        case HtmlTags.H3:
        case HtmlTags.H4:
        case HtmlTags.H5:
        case HtmlTags.H6:
            closePIfInButtonScope();
            if (stackNs[stackSize - 1] == NS_HTML && stackId[stackSize - 1] >= HtmlTags.H1 && stackId[stackSize - 1] <= HtmlTags.H6) {
                err("nested-heading");
                pop();
            }
            insertHtml(t);
            return;
        case HtmlTags.PRE:
        case HtmlTags.LISTING:
            closePIfInButtonScope();
            insertHtml(t);
            skipLf = true;
            framesetOk = false;
            return;
        case HtmlTags.FORM:
            if (formPointer != null && !stackHas(HtmlTags.TEMPLATE)) {
                err("nested-form");
                return;
            }
            closePIfInButtonScope();
            Element form = insertHtml(t);
            if (!stackHas(HtmlTags.TEMPLATE)) {
                formPointer = form;
            }
            return;
        case HtmlTags.LI:
            listItemStart(t, true);
            return;
        case HtmlTags.DD:
        case HtmlTags.DT:
            listItemStart(t, false);
            return;
        case HtmlTags.PLAINTEXT:
            closePIfInButtonScope();
            insertHtml(t);
            tokenizer.setState(HtmlTokenizer.PLAINTEXT);
            return;
        case HtmlTags.BUTTON:
            if (inScope(HtmlTags.BUTTON, SCOPE_DEFAULT)) {
                err("nested-button");
                generateImpliedEndTags(null);
                popUntil(HtmlTags.BUTTON);
            }
            reconstructFormatting();
            insertHtml(t);
            framesetOk = false;
            return;
        case HtmlTags.A:
            Element existing = null;
            // 마지막 표시 뒤에 a 요소가 이미 있는지 찾는다.
            for (int i = formatting.size() - 1; i >= 0; i--) {
                FormatEntry e = formatting.get(i);
                if (e.element == null) {
                    break;
                }
                if (isHtmlName(e.element, "a")) {
                    existing = e.element;
                    break;
                }
            }
            if (existing != null) {
                err("nested-anchor");
                adoptionAgency(t);
                FormatEntry fe = findFormatting(existing);
                if (fe != null) {
                    formatting.remove(fe);
                }
                int si = indexOf(existing);
                if (si >= 0) {
                    removeAt(si);
                }
            }
            reconstructFormatting();
            pushFormatting(insertHtml(t), t);
            return;
        case HtmlTags.B:
        case HtmlTags.BIG:
        case HtmlTags.CODE:
        case HtmlTags.EM:
        case HtmlTags.FONT:
        case HtmlTags.I:
        case HtmlTags.S:
        case HtmlTags.SMALL:
        case HtmlTags.STRIKE:
        case HtmlTags.STRONG:
        case HtmlTags.TT:
        case HtmlTags.U:
            reconstructFormatting();
            pushFormatting(insertHtml(t), t);
            return;
        case HtmlTags.NOBR:
            reconstructFormatting();
            if (inScope(HtmlTags.NOBR, SCOPE_DEFAULT)) {
                err("nested-nobr");
                adoptionAgency(t);
                reconstructFormatting();
            }
            pushFormatting(insertHtml(t), t);
            return;
        case HtmlTags.APPLET:
        case HtmlTags.MARQUEE:
        case HtmlTags.OBJECT:
            reconstructFormatting();
            insertHtml(t);
            insertMarker();
            framesetOk = false;
            return;
        case HtmlTags.TABLE:
            if (doc.getCompatibilityMode() != Document.QUIRKS_MODE) {
                closePIfInButtonScope();
            }
            insertHtml(t);
            framesetOk = false;
            mode = IN_TABLE;
            return;
        case HtmlTags.AREA:
        case HtmlTags.BR:
        case HtmlTags.EMBED:
        case HtmlTags.IMG:
        case HtmlTags.KEYGEN:
        case HtmlTags.WBR:
            reconstructFormatting();
            insertVoid(t);
            framesetOk = false;
            return;
        case HtmlTags.INPUT:
            reconstructFormatting();
            insertVoid(t);
            String type = t.getAttribute("type");
            if (type == null || !"hidden".equals(lower(type))) {
                framesetOk = false;
            }
            return;
        case HtmlTags.PARAM:
        case HtmlTags.SOURCE:
        case HtmlTags.TRACK:
            insertVoid(t);
            return;
        case HtmlTags.HR:
            closePIfInButtonScope();
            insertVoid(t);
            framesetOk = false;
            return;
        case HtmlTags.IMAGE:
            err("image-start-tag");
            inBodyStart(t.renamed("img"));
            t.selfClosingAcked = true;
            return;
        case HtmlTags.TEXTAREA:
            insertHtml(t);
            skipLf = true;
            tokenizer.setState(HtmlTokenizer.RCDATA);
            originalMode = mode;
            framesetOk = false;
            mode = TEXT;
            return;
        case HtmlTags.XMP:
            closePIfInButtonScope();
            reconstructFormatting();
            framesetOk = false;
            genericText(t, HtmlTokenizer.RAWTEXT);
            return;
        case HtmlTags.IFRAME:
            framesetOk = false;
            genericText(t, HtmlTokenizer.RAWTEXT);
            return;
        case HtmlTags.NOEMBED:
            genericText(t, HtmlTokenizer.RAWTEXT);
            return;
        case HtmlTags.NOSCRIPT:
            if (scripting) {
                genericText(t, HtmlTokenizer.RAWTEXT);
            } else {
                reconstructFormatting();
                insertHtml(t);
            }
            return;
        case HtmlTags.SELECT:
            reconstructFormatting();
            insertHtml(t);
            framesetOk = false;
            mode = mode == IN_TABLE || mode == IN_CAPTION || mode == IN_TABLE_BODY || mode == IN_ROW || mode == IN_CELL ? IN_SELECT_IN_TABLE
                    : IN_SELECT;
            return;
        case HtmlTags.OPTGROUP:
        case HtmlTags.OPTION:
            if (currentIs(HtmlTags.OPTION)) {
                pop();
            }
            reconstructFormatting();
            insertHtml(t);
            return;
        case HtmlTags.RB:
        case HtmlTags.RTC:
            if (inScope(HtmlTags.RUBY, SCOPE_DEFAULT)) {
                generateImpliedEndTags(null);
                if (!currentIs(HtmlTags.RUBY)) {
                    err("unexpected-open-element");
                }
            }
            insertHtml(t);
            return;
        case HtmlTags.RP:
        case HtmlTags.RT:
            if (inScope(HtmlTags.RUBY, SCOPE_DEFAULT)) {
                generateImpliedEndTags("rtc");
                if (!currentIs(HtmlTags.RTC) && !currentIs(HtmlTags.RUBY)) {
                    err("unexpected-open-element");
                }
            }
            insertHtml(t);
            return;
        case HtmlTags.MATH:
            reconstructFormatting();
            insertElement(t, "math", NS_MATHML, ATTRS_MATHML);
            if (t.selfClosing) {
                pop();
                ack(t);
            }
            return;
        case HtmlTags.SVG:
            reconstructFormatting();
            insertElement(t, "svg", NS_SVG, ATTRS_SVG);
            if (t.selfClosing) {
                pop();
                ack(t);
            }
            return;
        case HtmlTags.CAPTION:
        case HtmlTags.COL:
        case HtmlTags.COLGROUP:
        case HtmlTags.FRAME:
        case HtmlTags.HEAD:
        case HtmlTags.TBODY:
        case HtmlTags.TD:
        case HtmlTags.TFOOT:
        case HtmlTags.TH:
        case HtmlTags.THEAD:
        case HtmlTags.TR:
            err("unexpected-start-tag");
            return;
        default:
            // isindex 는 HTML5 2014 의 "in body" 규칙이 form, hr, label, input 으로 펼친다.
            if ("isindex".equals(t.name)) {
                isindex(t);
                return;
            }
            reconstructFormatting();
            insertHtml(t);
            return;
        }
    }

    /**
     * The default prompt of an expanded <code>isindex</code> element (HTML5 2014 section 8.2.5.4.7: the characters together with the
     * input element express "This is a searchable index. Enter search keywords: ").
     * <p>
     * 펼친 <code>isindex</code> 요소의 기본 안내 문구(HTML5 2014 §8.2.5.4.7: 글자와 input 요소가 함께 "This is a searchable index. Enter search keywords: " 를 나타낸다).
     */
    private static final String ISINDEX_PROMPT = "This is a searchable index. Enter search keywords: ";

    /**
     * Handles the obsolete <code>isindex</code> start tag in the "in body" insertion mode (HTML5 2014 section 8.2.5.4.7): unless a form
     * is open it becomes <code>&lt;form [action]&gt;&lt;hr&gt;&lt;label&gt;prompt&lt;input name=isindex ...&gt;&lt;/label&gt;&lt;hr&gt;&lt;/form&gt;</code>,
     * where the input takes all attributes of the token except <code>name</code>, <code>action</code> and <code>prompt</code> and the
     * prompt is the <code>prompt</code> attribute or the default sentence.
     * <p>
     * 폐기된 <code>isindex</code> 시작 태그를 "in body" 삽입 모드에서 처리한다(HTML5 2014 §8.2.5.4.7): 열린 form 이 없으면
     * <code>&lt;form [action]&gt;&lt;hr&gt;&lt;label&gt;안내 문구&lt;input name=isindex ...&gt;&lt;/label&gt;&lt;hr&gt;&lt;/form&gt;</code> 으로 바뀐다.
     * input 은 <code>name</code>, <code>action</code>, <code>prompt</code> 를 뺀 토큰의 모든 속성을 받고 안내 문구는 <code>prompt</code> 속성 또는 기본 문장이다.
     *
     * @param t the <code>isindex</code> start tag / <code>isindex</code> 시작 태그
     */
    private void isindex(HtmlToken t) {
        err("isindex");
        // 열린 form 이 있으면(템플릿 밖) 토큰을 무시한다.
        if (formPointer != null && !stackHas(HtmlTags.TEMPLATE)) {
            return;
        }
        ack(t);
        framesetOk = false;
        closePIfInButtonScope();
        Element form = insertHtml("form");
        if (!stackHas(HtmlTags.TEMPLATE)) {
            formPointer = form;
        }
        String action = t.getAttribute("action");
        if (action != null) {
            form.setAttribute("action", action);
        }
        insertHtml("hr");
        pop();
        reconstructFormatting();
        insertHtml("label");
        String prompt = t.getAttribute("prompt");
        insertChars(prompt != null ? prompt : ISINDEX_PROMPT);
        HtmlToken input = HtmlToken.synthetic(HtmlToken.START_TAG, "input", t.getOffset());
        // 토큰의 속성 중 name, action, prompt 를 뺀 나머지를 input 이 이어받는다.
        for (int i = 0; i < t.getAttributeCount(); i++) {
            String name = t.getAttributeName(i);
            if (!"name".equals(name) && !"action".equals(name) && !"prompt".equals(name)) {
                input.addAttribute(name, t.getAttributeValue(i));
            }
        }
        input.addAttribute("name", "isindex");
        insertHtml(input);
        pop();
        pop();
        insertHtml("hr");
        pop();
        pop();
        if (!stackHas(HtmlTags.TEMPLATE)) {
            formPointer = null;
        }
    }

    /**
     * Tells whether an element is an HTML element with a local name.
     * <p>
     * 요소가 해당 로컬 이름의 HTML 요소인지 알려 준다.
     *
     * @param e    element / 요소
     * @param name local name / 로컬 이름
     * @return true if so / 그러면 true
     */
    private static boolean isHtmlName(Element e, String name) {
        return NS_URI[NS_HTML].equals(e.getNamespaceURI()) && name.equals(e.getLocalName());
    }

    /**
     * Handles the start tags li, dd and dt: closes the previous item of the same list.
     * <p>
     * li, dd, dt 시작 태그를 처리한다: 같은 목록의 이전 항목을 닫는다.
     *
     * @param t  start tag / 시작 태그
     * @param li true for li, false for dd and dt / li 이면 true, dd 와 dt 이면 false
     */
    private void listItemStart(HtmlToken t, boolean li) {
        framesetOk = false;
        // 스택을 아래에서 위로 훑으며 같은 목록의 항목을 찾거나 special 요소(address, div, p 제외)를 만날 때까지 본다.
        for (int i = stackSize - 1; i >= 0; i--) {
            boolean match = li ? isHtmlAt(i, HtmlTags.LI) : (isHtmlAt(i, HtmlTags.DD) || isHtmlAt(i, HtmlTags.DT));
            if (match) {
                String name = stackEl[i].getLocalName();
                generateImpliedEndTags(name);
                if (stackEl[stackSize - 1] != stackEl[i]) {
                    err("unexpected-open-element");
                }
                popUntilElement(stackEl[i]);
                break;
            }
            if (isSpecialAt(i) && !(stackNs[i] == NS_HTML
                    && (stackId[i] == HtmlTags.ADDRESS || stackId[i] == HtmlTags.DIV || stackId[i] == HtmlTags.P))) {
                break;
            }
        }
        closePIfInButtonScope();
        insertHtml(t);
    }

    /**
     * End tags of the "in body" insertion mode.
     * <p>
     * "in body" 삽입 모드의 종료 태그.
     *
     * @param t end tag / 종료 태그
     */
    private void inBodyEnd(HtmlToken t) {
        int id = tid(t);
        switch (id) {
        case HtmlTags.TEMPLATE:
            inHead(t);
            return;
        case HtmlTags.BODY:
            if (!inScope(HtmlTags.BODY, SCOPE_DEFAULT)) {
                err("unexpected-end-tag");
                return;
            }
            checkOpenAtEnd();
            mode = AFTER_BODY;
            return;
        case HtmlTags.HTML:
            if (!inScope(HtmlTags.BODY, SCOPE_DEFAULT)) {
                err("unexpected-end-tag");
                return;
            }
            checkOpenAtEnd();
            mode = AFTER_BODY;
            dispatch(t);
            return;
        case HtmlTags.ADDRESS:
        case HtmlTags.ARTICLE:
        case HtmlTags.ASIDE:
        case HtmlTags.BLOCKQUOTE:
        case HtmlTags.BUTTON:
        case HtmlTags.CENTER:
        case HtmlTags.DETAILS:
        case HtmlTags.DIALOG:
        case HtmlTags.DIR:
        case HtmlTags.DIV:
        case HtmlTags.DL:
        case HtmlTags.FIELDSET:
        case HtmlTags.FIGCAPTION:
        case HtmlTags.FIGURE:
        case HtmlTags.FOOTER:
        case HtmlTags.HEADER:
        case HtmlTags.HGROUP:
        case HtmlTags.LISTING:
        case HtmlTags.MAIN:
        case HtmlTags.MENU:
        case HtmlTags.NAV:
        case HtmlTags.OL:
        case HtmlTags.PRE:
        case HtmlTags.SEARCH:
        case HtmlTags.SECTION:
        case HtmlTags.SUMMARY:
        case HtmlTags.UL:
            if (!inScope(id, SCOPE_DEFAULT)) {
                err("unexpected-end-tag");
                return;
            }
            generateImpliedEndTags(null);
            if (!currentIs(id)) {
                err("unexpected-open-element");
            }
            popUntil(id);
            return;
        case HtmlTags.FORM:
            if (!stackHas(HtmlTags.TEMPLATE)) {
                Element node = formPointer;
                formPointer = null;
                int ni = node == null ? -1 : indexOf(node);
                if (node == null || ni < 0 || !elementInScope(node)) {
                    err("unexpected-end-tag");
                    return;
                }
                generateImpliedEndTags(null);
                if (currentNode() != node) {
                    err("unexpected-open-element");
                }
                removeAt(indexOf(node));
            } else {
                if (!inScope(HtmlTags.FORM, SCOPE_DEFAULT)) {
                    err("unexpected-end-tag");
                    return;
                }
                generateImpliedEndTags(null);
                if (!currentIs(HtmlTags.FORM)) {
                    err("unexpected-open-element");
                }
                popUntil(HtmlTags.FORM);
            }
            return;
        case HtmlTags.P:
            if (!inScope(HtmlTags.P, SCOPE_BUTTON)) {
                err("unexpected-end-tag");
                insertHtml("p");
            }
            closeP();
            return;
        case HtmlTags.LI:
            if (!inScope(HtmlTags.LI, SCOPE_LIST)) {
                err("unexpected-end-tag");
                return;
            }
            generateImpliedEndTags("li");
            if (!currentIs(HtmlTags.LI)) {
                err("unexpected-open-element");
            }
            popUntil(HtmlTags.LI);
            return;
        case HtmlTags.DD:
        case HtmlTags.DT:
            if (!inScope(id, SCOPE_DEFAULT)) {
                err("unexpected-end-tag");
                return;
            }
            generateImpliedEndTags(t.name);
            if (!currentIs(id)) {
                err("unexpected-open-element");
            }
            popUntil(id);
            return;
        case HtmlTags.H1:
        case HtmlTags.H2:
        case HtmlTags.H3:
        case HtmlTags.H4:
        case HtmlTags.H5:
        case HtmlTags.H6:
            if (!headingInScope()) {
                err("unexpected-end-tag");
                return;
            }
            generateImpliedEndTags(null);
            if (!currentIs(id)) {
                err("unexpected-open-element");
            }
            popUntilHeading();
            return;
        case HtmlTags.A:
        case HtmlTags.B:
        case HtmlTags.BIG:
        case HtmlTags.CODE:
        case HtmlTags.EM:
        case HtmlTags.FONT:
        case HtmlTags.I:
        case HtmlTags.NOBR:
        case HtmlTags.S:
        case HtmlTags.SMALL:
        case HtmlTags.STRIKE:
        case HtmlTags.STRONG:
        case HtmlTags.TT:
        case HtmlTags.U:
            adoptionAgency(t);
            return;
        case HtmlTags.APPLET:
        case HtmlTags.MARQUEE:
        case HtmlTags.OBJECT:
            if (!inScope(id, SCOPE_DEFAULT)) {
                err("unexpected-end-tag");
                return;
            }
            generateImpliedEndTags(null);
            if (!currentIs(id)) {
                err("unexpected-open-element");
            }
            popUntil(id);
            clearFormattingToMarker();
            return;
        case HtmlTags.BR:
            err("end-tag-br");
            inBodyStart(HtmlToken.synthetic(HtmlToken.START_TAG, "br", t.offset));
            return;
        default:
            anyOtherEndTag(t);
            return;
        }
    }

    /**
     * "Any other end tag" of the "in body" insertion mode.
     * <p>
     * "in body" 삽입 모드의 "any other end tag".
     *
     * @param t end tag / 종료 태그
     */
    void anyOtherEndTag(HtmlToken t) {
        // 스택을 아래에서 위로 훑으며 이름이 같은 HTML 요소를 찾거나 special 요소를 만날 때까지 본다.
        for (int i = stackSize - 1; i >= 0; i--) {
            if (stackNs[i] == NS_HTML && t.name.equals(stackEl[i].getLocalName())) {
                generateImpliedEndTags(t.name);
                if (i != stackSize - 1) {
                    err("unexpected-open-element");
                }
                popUntilElement(stackEl[i]);
                return;
            }
            if (isSpecialAt(i)) {
                err("unexpected-end-tag");
                return;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // text
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The "text" insertion mode (the content of script, style, title, textarea and so on has been tokenized as characters).
     * <p>
     * "text" 삽입 모드(script, style, title, textarea 등의 내용은 문자로 토큰화되었다).
     *
     * @param t token / 토큰
     */
    private void text(HtmlToken t) {
        if (t.type == HtmlToken.EOF) {
            err("unexpected-eof-in-text");
            if (currentIs(HtmlTags.SCRIPT)) {
                ParserNodeData.scriptFlags(currentNode()).setAlreadyStarted(true);
            }
            pop();
            mode = originalMode;
            dispatch(t);
        } else if (t.type == HtmlToken.END_TAG && tid(t) == HtmlTags.SCRIPT) {
            Element script = pop();
            mode = originalMode;
            if (!fragment && listener != null) {
                scriptsDispatched = true;
                listener.scriptReady(script, control);
            }
        } else if (t.type == HtmlToken.END_TAG) {
            pop();
            mode = originalMode;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 표 관련 모드
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * "Clear the stack back to a table context".
     * <p>
     * 현재 노드가 table, template, html 이 될 때까지 스택을 비운다.
     */
    private void clearToTableContext() {
        // 현재 노드가 table, template, html 중 하나가 될 때까지 꺼낸다.
        while (stackSize > 1 && !(currentIs(HtmlTags.TABLE) || currentIs(HtmlTags.TEMPLATE) || currentIs(HtmlTags.HTML))) {
            pop();
        }
    }

    /**
     * "Clear the stack back to a table body context".
     * <p>
     * 현재 노드가 tbody, tfoot, thead, template, html 이 될 때까지 스택을 비운다.
     */
    private void clearToTableBodyContext() {
        // 현재 노드가 tbody, tfoot, thead, template, html 중 하나가 될 때까지 꺼낸다.
        while (stackSize > 1 && !(currentIs(HtmlTags.TBODY) || currentIs(HtmlTags.TFOOT) || currentIs(HtmlTags.THEAD)
                || currentIs(HtmlTags.TEMPLATE) || currentIs(HtmlTags.HTML))) {
            pop();
        }
    }

    /**
     * "Clear the stack back to a table row context".
     * <p>
     * 현재 노드가 tr, template, html 이 될 때까지 스택을 비운다.
     */
    private void clearToTableRowContext() {
        // 현재 노드가 tr, template, html 중 하나가 될 때까지 꺼낸다.
        while (stackSize > 1 && !(currentIs(HtmlTags.TR) || currentIs(HtmlTags.TEMPLATE) || currentIs(HtmlTags.HTML))) {
            pop();
        }
    }

    /**
     * "Anything else" of the "in table" insertion mode: the token is processed by the "in body" rules with foster parenting.
     * <p>
     * "in table" 삽입 모드의 "anything else": 토큰을 foster parenting 을 켜고 "in body" 규칙으로 처리한다.
     *
     * @param t token / 토큰
     */
    private void tableAnythingElse(HtmlToken t) {
        err("unexpected-token-in-table");
        fosterParenting = true;
        inBody(t);
        fosterParenting = false;
    }

    /**
     * The "in table" insertion mode.
     * <p>
     * "in table" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inTable(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
            return;
        }
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.EOF) {
            inBody(t);
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.CAPTION:
                clearToTableContext();
                insertMarker();
                insertHtml(t);
                mode = IN_CAPTION;
                return;
            case HtmlTags.COLGROUP:
                clearToTableContext();
                insertHtml(t);
                mode = IN_COLUMN_GROUP;
                return;
            case HtmlTags.COL:
                clearToTableContext();
                insertHtml("colgroup");
                mode = IN_COLUMN_GROUP;
                dispatch(t);
                return;
            case HtmlTags.TBODY:
            case HtmlTags.TFOOT:
            case HtmlTags.THEAD:
                clearToTableContext();
                insertHtml(t);
                mode = IN_TABLE_BODY;
                return;
            case HtmlTags.TD:
            case HtmlTags.TH:
            case HtmlTags.TR:
                clearToTableContext();
                insertHtml("tbody");
                mode = IN_TABLE_BODY;
                dispatch(t);
                return;
            case HtmlTags.TABLE:
                err("unexpected-start-tag");
                if (!inScope(HtmlTags.TABLE, SCOPE_TABLE)) {
                    return;
                }
                popUntil(HtmlTags.TABLE);
                resetInsertionMode();
                dispatch(t);
                return;
            case HtmlTags.STYLE:
            case HtmlTags.SCRIPT:
            case HtmlTags.TEMPLATE:
                inHead(t);
                return;
            case HtmlTags.INPUT:
                String type = t.getAttribute("type");
                if (type != null && "hidden".equals(lower(type))) {
                    err("unexpected-hidden-input-in-table");
                    insertVoid(t);
                    return;
                }
                break;
            case HtmlTags.FORM:
                err("unexpected-form-in-table");
                if (formPointer != null && !stackHas(HtmlTags.TEMPLATE)) {
                    return;
                }
                Element f = insertHtml(t);
                if (!stackHas(HtmlTags.TEMPLATE)) {
                    formPointer = f;
                }
                pop();
                return;
            default:
                break;
            }
        } else {
            switch (id) {
            case HtmlTags.TABLE:
                if (!inScope(HtmlTags.TABLE, SCOPE_TABLE)) {
                    err("unexpected-end-tag");
                    return;
                }
                popUntil(HtmlTags.TABLE);
                resetInsertionMode();
                return;
            case HtmlTags.BODY:
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.HTML:
            case HtmlTags.TBODY:
            case HtmlTags.TD:
            case HtmlTags.TFOOT:
            case HtmlTags.TH:
            case HtmlTags.THEAD:
            case HtmlTags.TR:
                err("unexpected-end-tag");
                return;
            case HtmlTags.TEMPLATE:
                inHead(t);
                return;
            default:
                break;
            }
        }
        tableAnythingElse(t);
    }

    /**
     * The "in caption" insertion mode.
     * <p>
     * "in caption" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inCaption(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        boolean closeCaption = false;
        if (t.type == HtmlToken.END_TAG && id == HtmlTags.CAPTION) {
            if (!inScope(HtmlTags.CAPTION, SCOPE_TABLE)) {
                err("unexpected-end-tag");
                return;
            }
            closeCaption = true;
        } else if ((t.type == HtmlToken.START_TAG && (id == HtmlTags.CAPTION || id == HtmlTags.COL || id == HtmlTags.COLGROUP
                || id == HtmlTags.TBODY || id == HtmlTags.TD || id == HtmlTags.TFOOT || id == HtmlTags.TH || id == HtmlTags.THEAD
                || id == HtmlTags.TR)) || (t.type == HtmlToken.END_TAG && id == HtmlTags.TABLE)) {
            if (!inScope(HtmlTags.CAPTION, SCOPE_TABLE)) {
                err("unexpected-token");
                return;
            }
            closeCaption = true;
        } else if (t.type == HtmlToken.END_TAG && (id == HtmlTags.BODY || id == HtmlTags.COL || id == HtmlTags.COLGROUP || id == HtmlTags.HTML
                || id == HtmlTags.TBODY || id == HtmlTags.TD || id == HtmlTags.TFOOT || id == HtmlTags.TH || id == HtmlTags.THEAD
                || id == HtmlTags.TR)) {
            err("unexpected-end-tag");
            return;
        }
        if (!closeCaption) {
            inBody(t);
            return;
        }
        generateImpliedEndTags(null);
        if (!currentIs(HtmlTags.CAPTION)) {
            err("unexpected-open-element");
        }
        popUntil(HtmlTags.CAPTION);
        clearFormattingToMarker();
        mode = IN_TABLE;
        if (!(t.type == HtmlToken.END_TAG && id == HtmlTags.CAPTION)) {
            dispatch(t);
        }
    }

    /**
     * The "in column group" insertion mode.
     * <p>
     * "in column group" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inColumnGroup(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
            return;
        }
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.EOF) {
            inBody(t);
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            if (id == HtmlTags.HTML) {
                inBody(t);
                return;
            }
            if (id == HtmlTags.COL) {
                insertVoid(t);
                return;
            }
            if (id == HtmlTags.TEMPLATE) {
                inHead(t);
                return;
            }
        } else {
            if (id == HtmlTags.COLGROUP) {
                if (!currentIs(HtmlTags.COLGROUP)) {
                    err("unexpected-end-tag");
                    return;
                }
                pop();
                mode = IN_TABLE;
                return;
            }
            if (id == HtmlTags.COL) {
                err("unexpected-end-tag");
                return;
            }
            if (id == HtmlTags.TEMPLATE) {
                inHead(t);
                return;
            }
        }
        if (!currentIs(HtmlTags.COLGROUP)) {
            err("unexpected-token");
            return;
        }
        pop();
        mode = IN_TABLE;
        dispatch(t);
    }

    /**
     * The "in table body" insertion mode.
     * <p>
     * "in table body" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inTableBody(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.TR:
                clearToTableBodyContext();
                insertHtml(t);
                mode = IN_ROW;
                return;
            case HtmlTags.TH:
            case HtmlTags.TD:
                err("unexpected-cell-in-table-body");
                clearToTableBodyContext();
                insertHtml("tr");
                mode = IN_ROW;
                dispatch(t);
                return;
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.TBODY:
            case HtmlTags.TFOOT:
            case HtmlTags.THEAD:
                closeTableBody(t);
                return;
            default:
                break;
            }
        } else if (t.type == HtmlToken.END_TAG) {
            switch (id) {
            case HtmlTags.TBODY:
            case HtmlTags.TFOOT:
            case HtmlTags.THEAD:
                if (!inScope(id, SCOPE_TABLE)) {
                    err("unexpected-end-tag");
                    return;
                }
                clearToTableBodyContext();
                pop();
                mode = IN_TABLE;
                return;
            case HtmlTags.TABLE:
                closeTableBody(t);
                return;
            case HtmlTags.BODY:
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.HTML:
            case HtmlTags.TD:
            case HtmlTags.TH:
            case HtmlTags.TR:
                err("unexpected-end-tag");
                return;
            default:
                break;
            }
        }
        inTable(t);
    }

    /**
     * Closes the table body section for a token that ends it and reprocesses the token.
     * <p>
     * 표 본문 구역을 끝내는 토큰에 대해 그 구역을 닫고 토큰을 다시 처리한다.
     *
     * @param t token / 토큰
     */
    private void closeTableBody(HtmlToken t) {
        if (!inScope(HtmlTags.TBODY, SCOPE_TABLE) && !inScope(HtmlTags.THEAD, SCOPE_TABLE) && !inScope(HtmlTags.TFOOT, SCOPE_TABLE)) {
            err("unexpected-token");
            return;
        }
        clearToTableBodyContext();
        pop();
        mode = IN_TABLE;
        dispatch(t);
    }

    /**
     * The "in row" insertion mode.
     * <p>
     * "in row" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inRow(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.TH:
            case HtmlTags.TD:
                clearToTableRowContext();
                insertHtml(t);
                mode = IN_CELL;
                insertMarker();
                return;
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.TBODY:
            case HtmlTags.TFOOT:
            case HtmlTags.THEAD:
            case HtmlTags.TR:
                closeRow(t);
                return;
            default:
                break;
            }
        } else if (t.type == HtmlToken.END_TAG) {
            switch (id) {
            case HtmlTags.TR:
                if (!inScope(HtmlTags.TR, SCOPE_TABLE)) {
                    err("unexpected-end-tag");
                    return;
                }
                clearToTableRowContext();
                pop();
                mode = IN_TABLE_BODY;
                return;
            case HtmlTags.TABLE:
                closeRow(t);
                return;
            case HtmlTags.TBODY:
            case HtmlTags.TFOOT:
            case HtmlTags.THEAD:
                if (!inScope(id, SCOPE_TABLE) || !inScope(HtmlTags.TR, SCOPE_TABLE)) {
                    err("unexpected-end-tag");
                    return;
                }
                clearToTableRowContext();
                pop();
                mode = IN_TABLE_BODY;
                dispatch(t);
                return;
            case HtmlTags.BODY:
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.HTML:
            case HtmlTags.TD:
            case HtmlTags.TH:
                err("unexpected-end-tag");
                return;
            default:
                break;
            }
        }
        inTable(t);
    }

    /**
     * Closes the row for a token that ends it and reprocesses the token.
     * <p>
     * 행을 끝내는 토큰에 대해 행을 닫고 토큰을 다시 처리한다.
     *
     * @param t token / 토큰
     */
    private void closeRow(HtmlToken t) {
        if (!inScope(HtmlTags.TR, SCOPE_TABLE)) {
            err("unexpected-token");
            return;
        }
        clearToTableRowContext();
        pop();
        mode = IN_TABLE_BODY;
        dispatch(t);
    }

    /**
     * "Close the cell".
     * <p>
     * 셀을 닫는다("close the cell").
     */
    private void closeCell() {
        generateImpliedEndTags(null);
        if (!currentIs(HtmlTags.TD) && !currentIs(HtmlTags.TH)) {
            err("unexpected-open-element");
        }
        // td 나 th 가 꺼내질 때까지 꺼낸다.
        while (stackSize > 0) {
            boolean hit = isHtmlAt(stackSize - 1, HtmlTags.TD) || isHtmlAt(stackSize - 1, HtmlTags.TH);
            pop();
            if (hit) {
                break;
            }
        }
        clearFormattingToMarker();
        mode = IN_ROW;
    }

    /**
     * The "in cell" insertion mode.
     * <p>
     * "in cell" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inCell(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.END_TAG) {
            switch (id) {
            case HtmlTags.TD:
            case HtmlTags.TH:
                if (!inScope(id, SCOPE_TABLE)) {
                    err("unexpected-end-tag");
                    return;
                }
                generateImpliedEndTags(null);
                if (!currentIs(id)) {
                    err("unexpected-open-element");
                }
                popUntil(id);
                clearFormattingToMarker();
                mode = IN_ROW;
                return;
            case HtmlTags.BODY:
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.HTML:
                err("unexpected-end-tag");
                return;
            case HtmlTags.TABLE:
            case HtmlTags.TBODY:
            case HtmlTags.TFOOT:
            case HtmlTags.THEAD:
            case HtmlTags.TR:
                if (!inScope(id, SCOPE_TABLE)) {
                    err("unexpected-end-tag");
                    return;
                }
                closeCell();
                dispatch(t);
                return;
            default:
                break;
            }
        } else if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.CAPTION:
            case HtmlTags.COL:
            case HtmlTags.COLGROUP:
            case HtmlTags.TBODY:
            case HtmlTags.TD:
            case HtmlTags.TFOOT:
            case HtmlTags.TH:
            case HtmlTags.THEAD:
            case HtmlTags.TR:
                if (!inScope(HtmlTags.TD, SCOPE_TABLE) && !inScope(HtmlTags.TH, SCOPE_TABLE)) {
                    err("unexpected-start-tag");
                    return;
                }
                closeCell();
                dispatch(t);
                return;
            default:
                break;
            }
        }
        inBody(t);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // select
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Pops elements until a select element has been popped and resets the insertion mode.
     * <p>
     * select 요소가 꺼내질 때까지 요소를 꺼내고 삽입 모드를 재설정한다.
     */
    private void closeSelect() {
        popUntil(HtmlTags.SELECT);
        resetInsertionMode();
    }

    /**
     * The "in select" insertion mode.
     * <p>
     * "in select" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inSelect(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
            return;
        }
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.EOF) {
            inBody(t);
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            switch (id) {
            case HtmlTags.HTML:
                inBody(t);
                return;
            case HtmlTags.OPTION:
                if (currentIs(HtmlTags.OPTION)) {
                    pop();
                }
                insertHtml(t);
                return;
            case HtmlTags.OPTGROUP:
                if (currentIs(HtmlTags.OPTION)) {
                    pop();
                }
                if (currentIs(HtmlTags.OPTGROUP)) {
                    pop();
                }
                insertHtml(t);
                return;
            case HtmlTags.HR:
                if (currentIs(HtmlTags.OPTION)) {
                    pop();
                }
                if (currentIs(HtmlTags.OPTGROUP)) {
                    pop();
                }
                insertVoid(t);
                return;
            case HtmlTags.SELECT:
                err("unexpected-select-in-select");
                if (inScope(HtmlTags.SELECT, SCOPE_SELECT)) {
                    closeSelect();
                }
                return;
            case HtmlTags.INPUT:
            case HtmlTags.KEYGEN:
            case HtmlTags.TEXTAREA:
                err("unexpected-start-tag-in-select");
                if (!inScope(HtmlTags.SELECT, SCOPE_SELECT)) {
                    return;
                }
                closeSelect();
                dispatch(t);
                return;
            case HtmlTags.SCRIPT:
            case HtmlTags.TEMPLATE:
                inHead(t);
                return;
            default:
                err("unexpected-start-tag-in-select");
                return;
            }
        }
        switch (id) {
        case HtmlTags.OPTGROUP:
            if (currentIs(HtmlTags.OPTION) && stackSize >= 2 && isHtmlAt(stackSize - 2, HtmlTags.OPTGROUP)) {
                pop();
            }
            if (currentIs(HtmlTags.OPTGROUP)) {
                pop();
            } else {
                err("unexpected-end-tag");
            }
            return;
        case HtmlTags.OPTION:
            if (currentIs(HtmlTags.OPTION)) {
                pop();
            } else {
                err("unexpected-end-tag");
            }
            return;
        case HtmlTags.SELECT:
            if (!inScope(HtmlTags.SELECT, SCOPE_SELECT)) {
                err("unexpected-end-tag");
                return;
            }
            closeSelect();
            return;
        case HtmlTags.TEMPLATE:
            inHead(t);
            return;
        default:
            err("unexpected-end-tag-in-select");
            return;
        }
    }

    /**
     * The "in select in table" insertion mode.
     * <p>
     * "in select in table" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inSelectInTable(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        boolean tableTag = id == HtmlTags.CAPTION || id == HtmlTags.TABLE || id == HtmlTags.TBODY || id == HtmlTags.TFOOT
                || id == HtmlTags.THEAD || id == HtmlTags.TR || id == HtmlTags.TD || id == HtmlTags.TH;
        if (t.type == HtmlToken.START_TAG && tableTag) {
            err("unexpected-table-tag-in-select");
            closeSelect();
            dispatch(t);
        } else if (t.type == HtmlToken.END_TAG && tableTag) {
            err("unexpected-table-end-tag-in-select");
            if (!inScope(id, SCOPE_TABLE)) {
                return;
            }
            closeSelect();
            dispatch(t);
        } else {
            inSelect(t);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // template, after body, frameset
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Replaces the current template insertion mode and switches to the same mode, then reprocesses the token.
     * <p>
     * 현재 template 삽입 모드를 바꾸고 같은 모드로 전환한 뒤 토큰을 다시 처리한다.
     *
     * @param newMode new mode / 새 모드
     * @param t       token / 토큰
     */
    private void templateSwitch(int newMode, HtmlToken t) {
        templateCount--;
        pushTemplateMode(newMode);
        mode = newMode;
        dispatch(t);
    }

    /**
     * The "in template" insertion mode.
     * <p>
     * "in template" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inTemplate(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT || t.type == HtmlToken.DOCTYPE) {
            inBody(t);
            return;
        }
        if (t.type == HtmlToken.EOF) {
            if (!stackHas(HtmlTags.TEMPLATE)) {
                stopParsing();
                return;
            }
            err("unexpected-eof-in-template");
            popUntil(HtmlTags.TEMPLATE);
            clearFormattingToMarker();
            templateCount--;
            resetInsertionMode();
            dispatch(t);
            return;
        }
        if (t.type == HtmlToken.END_TAG) {
            if (id == HtmlTags.TEMPLATE) {
                inHead(t);
            } else {
                err("unexpected-end-tag");
            }
            return;
        }
        switch (id) {
        case HtmlTags.BASE:
        case HtmlTags.BASEFONT:
        case HtmlTags.BGSOUND:
        case HtmlTags.LINK:
        case HtmlTags.META:
        case HtmlTags.NOFRAMES:
        case HtmlTags.SCRIPT:
        case HtmlTags.STYLE:
        case HtmlTags.TEMPLATE:
        case HtmlTags.TITLE:
            inHead(t);
            return;
        case HtmlTags.CAPTION:
        case HtmlTags.COLGROUP:
        case HtmlTags.TBODY:
        case HtmlTags.TFOOT:
        case HtmlTags.THEAD:
            templateSwitch(IN_TABLE, t);
            return;
        case HtmlTags.COL:
            templateSwitch(IN_COLUMN_GROUP, t);
            return;
        case HtmlTags.TR:
            templateSwitch(IN_TABLE_BODY, t);
            return;
        case HtmlTags.TD:
        case HtmlTags.TH:
            templateSwitch(IN_ROW, t);
            return;
        default:
            templateSwitch(IN_BODY, t);
            return;
        }
    }

    /**
     * The "after body" insertion mode.
     * <p>
     * "after body" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void afterBody(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            insertComment(t.data, stackEl[0]);
        } else if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML) {
            inBody(t);
        } else if (t.type == HtmlToken.END_TAG && id == HtmlTags.HTML) {
            if (fragment) {
                err("unexpected-end-tag");
            } else {
                mode = AFTER_AFTER_BODY;
            }
        } else if (t.type == HtmlToken.EOF) {
            stopParsing();
        } else {
            err("unexpected-token-after-body");
            mode = IN_BODY;
            dispatch(t);
        }
    }

    /**
     * The "in frameset" insertion mode.
     * <p>
     * "in frameset" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void inFrameset(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
        } else if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML) {
            inBody(t);
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.FRAMESET) {
            insertHtml(t);
        } else if (t.type == HtmlToken.END_TAG && id == HtmlTags.FRAMESET) {
            if (stackSize == 1) {
                err("unexpected-end-tag");
                return;
            }
            pop();
            if (!fragment && !currentIs(HtmlTags.FRAMESET)) {
                mode = AFTER_FRAMESET;
            }
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.FRAME) {
            insertVoid(t);
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.NOFRAMES) {
            inHead(t);
        } else if (t.type == HtmlToken.EOF) {
            if (stackSize != 1) {
                err("unexpected-eof");
            }
            stopParsing();
        } else {
            err("unexpected-token-in-frameset");
        }
    }

    /**
     * The "after frameset" insertion mode.
     * <p>
     * "after frameset" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void afterFrameset(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG || t.type == HtmlToken.END_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
        } else if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML) {
            inBody(t);
        } else if (t.type == HtmlToken.END_TAG && id == HtmlTags.HTML) {
            mode = AFTER_AFTER_FRAMESET;
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.NOFRAMES) {
            inHead(t);
        } else if (t.type == HtmlToken.EOF) {
            stopParsing();
        } else {
            err("unexpected-token-after-frameset");
        }
    }

    /**
     * The "after after body" insertion mode.
     * <p>
     * "after after body" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void afterAfterBody(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            insertComment(t.data, doc);
        } else if (t.type == HtmlToken.DOCTYPE || (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML)) {
            inBody(t);
        } else if (t.type == HtmlToken.EOF) {
            stopParsing();
        } else {
            err("unexpected-token-after-after-body");
            mode = IN_BODY;
            dispatch(t);
        }
    }

    /**
     * The "after after frameset" insertion mode.
     * <p>
     * "after after frameset" 삽입 모드.
     *
     * @param t token / 토큰
     */
    private void afterAfterFrameset(HtmlToken t) {
        int id = t.type == HtmlToken.START_TAG ? tid(t) : 0;
        if (t.type == HtmlToken.COMMENT) {
            insertComment(t.data, doc);
        } else if (t.type == HtmlToken.DOCTYPE || (t.type == HtmlToken.START_TAG && id == HtmlTags.HTML)) {
            inBody(t);
        } else if (t.type == HtmlToken.EOF) {
            stopParsing();
        } else if (t.type == HtmlToken.START_TAG && id == HtmlTags.NOFRAMES) {
            inHead(t);
        } else {
            err("unexpected-token-after-after-frameset");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 외부 콘텐츠(SVG, MathML)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Characters in foreign content.
     * <p>
     * 외부 콘텐츠의 문자 처리.
     *
     * @param s   characters / 문자
     * @param cls character class / 문자 부류
     */
    private void foreignChars(String s, int cls) {
        if (cls == CLS_NUL) {
            err("unexpected-null-character");
            StringBuilder sb = new StringBuilder();
            // NUL 문자 하나하나를 U+FFFD 로 바꾼다.
            for (int i = 0; i < s.length(); i++) {
                sb.append((char) 0xFFFD);
            }
            insertChars(sb.toString());
            return;
        }
        insertChars(s);
        if (cls == CLS_OTHER) {
            framesetOk = false;
        }
    }

    /**
     * Tells whether a start tag breaks out of foreign content (the HTML elements that cannot be children of SVG or MathML).
     * <p>
     * 시작 태그가 외부 콘텐츠를 벗어나게 하는지(SVG 나 MathML 의 자식이 될 수 없는 HTML 요소인지) 알려 준다.
     *
     * @param t start tag / 시작 태그
     * @return true if it breaks out / 벗어나게 하면 true
     */
    private boolean breaksOut(HtmlToken t) {
        switch (tid(t)) {
        case HtmlTags.B:
        case HtmlTags.BIG:
        case HtmlTags.BLOCKQUOTE:
        case HtmlTags.BODY:
        case HtmlTags.BR:
        case HtmlTags.CENTER:
        case HtmlTags.CODE:
        case HtmlTags.DD:
        case HtmlTags.DIV:
        case HtmlTags.DL:
        case HtmlTags.DT:
        case HtmlTags.EM:
        case HtmlTags.EMBED:
        case HtmlTags.H1:
        case HtmlTags.H2:
        case HtmlTags.H3:
        case HtmlTags.H4:
        case HtmlTags.H5:
        case HtmlTags.H6:
        case HtmlTags.HEAD:
        case HtmlTags.HR:
        case HtmlTags.I:
        case HtmlTags.IMG:
        case HtmlTags.LI:
        case HtmlTags.LISTING:
        case HtmlTags.MENU:
        case HtmlTags.META:
        case HtmlTags.NOBR:
        case HtmlTags.OL:
        case HtmlTags.P:
        case HtmlTags.PRE:
        case HtmlTags.RUBY:
        case HtmlTags.S:
        case HtmlTags.SMALL:
        case HtmlTags.SPAN:
        case HtmlTags.STRONG:
        case HtmlTags.STRIKE:
        case HtmlTags.SUB:
        case HtmlTags.SUP:
        case HtmlTags.TABLE:
        case HtmlTags.TT:
        case HtmlTags.U:
        case HtmlTags.UL:
        case HtmlTags.VAR:
            return true;
        case HtmlTags.FONT:
            return t.getAttribute("color") != null || t.getAttribute("face") != null || t.getAttribute("size") != null;
        default:
            return false;
        }
    }

    /**
     * Tokens that are not characters, in foreign content.
     * <p>
     * 외부 콘텐츠의 문자가 아닌 토큰 처리.
     *
     * @param t token / 토큰
     */
    private void foreignToken(HtmlToken t) {
        if (t.type == HtmlToken.COMMENT) {
            comment(t);
            return;
        }
        if (t.type == HtmlToken.DOCTYPE) {
            err("unexpected-doctype");
            return;
        }
        if (t.type == HtmlToken.START_TAG) {
            if (breaksOut(t)) {
                err("unexpected-html-element-in-foreign-content");
                // 현재 노드가 MathML 텍스트 통합 지점, HTML 통합 지점, HTML 요소가 될 때까지 꺼내고 HTML 규칙으로 다시 처리한다.
                while (stackSize > 1 && !(stackNs[stackSize - 1] == NS_HTML || isMathTextIntegration(stackSize - 1)
                        || isHtmlIntegrationPoint(stackEl[stackSize - 1], stackId[stackSize - 1], stackNs[stackSize - 1]))) {
                    pop();
                }
                htmlToken(t);
                return;
            }
            int ns = adjustedNs();
            String name = t.name;
            int kind = ns == NS_SVG ? ATTRS_SVG : ATTRS_MATHML;
            if (ns == NS_SVG) {
                name = adjustSvgElementName(name);
            }
            Element el = insertElement(t, name, ns, kind);
            if (t.selfClosing) {
                if ("script".equals(t.name) && ns == NS_SVG) {
                    ack(t);
                    foreignScriptEnd(el);
                } else {
                    pop();
                    ack(t);
                }
            }
            return;
        }
        if (t.type == HtmlToken.END_TAG) {
            if ("br".equals(t.name) || "p".equals(t.name)) {
                err("unexpected-html-end-tag-in-foreign-content");
                // 현재 노드가 MathML 텍스트 통합 지점, HTML 통합 지점, HTML 요소가 될 때까지 꺼내고 HTML 규칙으로 다시 처리한다.
                while (stackSize > 1 && !(stackNs[stackSize - 1] == NS_HTML || isMathTextIntegration(stackSize - 1)
                        || isHtmlIntegrationPoint(stackEl[stackSize - 1], stackId[stackSize - 1], stackNs[stackSize - 1]))) {
                    pop();
                }
                htmlToken(t);
                return;
            }
            if ("script".equals(t.name) && stackSize > 0 && stackNs[stackSize - 1] == NS_SVG && stackId[stackSize - 1] == HtmlTags.SCRIPT) {
                foreignScriptEnd(currentNode());
                return;
            }
            int i = stackSize - 1;
            if (!lower(stackEl[i].getLocalName()).equals(t.name)) {
                err("unexpected-end-tag");
            }
            // 스택을 위로 올라가며 같은 이름의 요소를 찾고, HTML 요소를 만나면 HTML 규칙으로 처리한다.
            while (true) {
                if (i == 0) {
                    return;
                }
                if (lower(stackEl[i].getLocalName()).equals(t.name)) {
                    popUntilElement(stackEl[i]);
                    return;
                }
                i--;
                if (stackNs[i] == NS_HTML) {
                    htmlToken(t);
                    return;
                }
            }
        }
    }

    /**
     * Tells whether the stack entry at an index is a MathML text integration point.
     * <p>
     * 스택의 해당 위치 항목이 MathML 텍스트 통합 지점인지 알려 준다.
     *
     * @param i index / 위치
     * @return true if so / 그러면 true
     */
    private boolean isMathTextIntegration(int i) {
        int id = stackId[i];
        return stackNs[i] == NS_MATHML && (id == HtmlTags.MI || id == HtmlTags.MO || id == HtmlTags.MN || id == HtmlTags.MS || id == HtmlTags.MTEXT);
    }

    /**
     * The end of an SVG <code>script</code> element: it is popped and the listener may run it.
     * <p>
     * SVG <code>script</code> 요소의 끝: 꺼내고 리스너가 실행할 수 있게 한다.
     *
     * @param script the script element / script 요소
     */
    private void foreignScriptEnd(Element script) {
        pop();
        if (!fragment && listener != null) {
            scriptsDispatched = true;
            listener.scriptReady(script, control);
        }
    }
}
