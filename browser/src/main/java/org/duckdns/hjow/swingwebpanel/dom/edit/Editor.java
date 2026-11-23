package org.duckdns.hjow.swingwebpanel.dom.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.CompositionEvent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.InputEvent;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.Selection;
import org.duckdns.hjow.swingwebpanel.dom.StaticRange;
import org.duckdns.hjow.swingwebpanel.dom.Text;

/**
 * The editing engine of one document (plan item P20-06): it changes the DOM the way the browsers do when the user types, deletes, breaks a
 * line, formats, cuts, pastes or drops in an editing host (<code>contenteditable</code> or design mode), and it is the implementation of
 * <code>document.execCommand()</code>. The selection of the document is the place where it works. Every operation sends the cancelable
 * <code>beforeinput</code> event to the editing host first (Input Events, with the <code>inputType</code>, the <code>data</code> and the
 * target ranges), does nothing when a listener cancels it, then changes the DOM as one step of the undo history and sends
 * <code>input</code>. The engine needs no layout: where it must know what is a block or whether white space is preserved it asks the
 * {@link Environment}, whose default decides by the names of the elements.
 * <p>
 * Inside this engine a position is a node and an offset, as in a range. Text is inserted into text nodes (white space is rebalanced with
 * no-break spaces so that every typed space stays visible, as the browsers do for <code>white-space: normal</code>), a deletion works on
 * character units, a line break inside a block is a <code>br</code>, a paragraph break splits the block (the new block is a copy of the
 * old one, a <code>div</code> after a heading, a new item in a list), an empty block keeps a placeholder <code>br</code>, and the end of
 * a block is joined to the next one when the deletion crosses it.
 * <p>
 * 한 문서의 편집 엔진(계획 P20-06): 편집 호스트(<code>contenteditable</code> 이나 디자인 모드) 안에서 사용자가 글자를 입력하고, 지우고, 줄을 바꾸고, 서식을 정하고,
 * 잘라내고, 붙여넣고, 놓을 때 브라우저가 하는 대로 DOM 을 바꾸며, <code>document.execCommand()</code> 의 구현이다. 문서의 선택이 일하는 자리이다. 모든
 * 작업은 먼저 취소할 수 있는 <code>beforeinput</code> 이벤트를 편집 호스트에 보내고(Input Events: <code>inputType</code>, <code>data</code>, 대상 범위),
 * 리스너가 취소하면 아무것도 하지 않으며, 아니면 DOM 을 되돌리기 이력의 한 단계로 바꾼 뒤 <code>input</code> 을 보낸다. 엔진은 레이아웃이 필요 없다.
 * 무엇이 블록인지, 공백이 보존되는지 알아야 할 때는 {@link Environment} 에 묻고, 기본 구현은 요소 이름으로 정한다.
 * <p>
 * 이 엔진 안에서 위치는 범위처럼 노드와 오프셋이다. 텍스트는 텍스트 노드에 넣고(입력한 공백이 모두 보이도록 NBSP 로 공백을 재조정한다. <code>white-space:
 * normal</code> 에서 브라우저가 하는 대로), 지우기는 글자 단위로 하며, 블록 안의 줄바꿈은 <code>br</code>, 문단 나누기는 블록을 쪼개고(새 블록은 옛 블록의 복사본이고
 * 제목 뒤에서는 <code>div</code>, 목록에서는 새 항목), 빈 블록은 자리표시 <code>br</code> 을 가지며, 지우기가 블록의 끝을 넘으면 다음 블록과 합친다.
 */
public final class Editor {

    /**
     * What the engine needs from the host: how events are sent, and the facts about the page that only the layout knows. The default
     * methods of {@link Editor} work without a host.
     * <p>
     * 엔진이 호스트에게 필요로 하는 것: 이벤트를 보내는 방법과 레이아웃만 아는 페이지의 사실. 호스트 없이도 {@link Editor} 의 기본값으로 동작한다.
     */
    public interface Environment {
        /**
         * Sends an event (a trusted one: the user agent sends it) and tells whether it was not cancelled.
         * <p>
         * 이벤트를 전달하고(신뢰 이벤트: 사용자 에이전트가 보낸다) 취소되지 않았는지 알려 준다.
         *
         * @param target the target / 대상
         * @param event  the event / 이벤트
         * @return false if a listener cancelled it / 리스너가 취소했으면 false
         */
        boolean dispatch(Node target, Event event);

        /**
         * Tells whether an element is block-level (the line breaks and the joining of lines work on blocks).
         * <p>
         * 요소가 블록 수준인지 알려 준다(줄바꿈과 줄 합치기는 블록 단위로 일한다).
         *
         * @param e the element / 요소
         * @return true for a block / 블록이면 true
         */
        boolean isBlock(Element e);

        /**
         * Tells whether the white space of a node is preserved (<code>white-space: pre</code> and its kin), so that typing a space inserts a
         * plain space and a line feed is a character.
         * <p>
         * 노드의 공백이 보존되는지(<code>white-space: pre</code> 계열) 알려 준다. 보존되면 공백 입력은 일반 공백을 넣고 줄바꿈은 문자이다.
         *
         * @param node the node / 노드
         * @return true if preserved / 보존되면 true
         */
        boolean preservesWhitespace(Node node);
    }

    /**
     * The names of the block-level elements that the default environment knows.
     * <p>
     * 기본 환경이 아는 블록 수준 요소의 이름.
     */
    private static final Set<String> BLOCKS = new HashSet<String>(Arrays.asList("address", "article", "aside", "blockquote", "body", "caption",
            "center", "dd", "details", "dir", "div", "dl", "dt", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5",
            "h6", "header", "hgroup", "hr", "html", "li", "main", "menu", "nav", "ol", "p", "pre", "section", "summary", "table", "tbody", "td",
            "tfoot", "th", "thead", "tr", "ul", "listing", "plaintext", "xmp"));

    /**
     * The elements that are one unit for editing (they have no editable text of the page).
     * <p>
     * 편집에서 한 단위인 요소(페이지의 편집 가능한 텍스트가 없다).
     */
    private static final Set<String> ATOMIC = new HashSet<String>(Arrays.asList("img", "input", "textarea", "select", "button", "video", "audio",
            "canvas", "iframe", "object", "embed", "svg", "math", "hr", "wbr", "meter", "progress"));

    /**
     * The elements that cannot be split by a paragraph break: table cells and captions keep their paragraphs inside.
     * <p>
     * 문단 나누기로 쪼갤 수 없는 요소: 표 셀과 캡션은 문단을 그 안에 둔다.
     */
    private static final Set<String> CONTAINERS = new HashSet<String>(Arrays.asList("td", "th", "caption", "body", "html", "details", "summary",
            "fieldset", "figure", "figcaption", "form", "table", "tbody", "thead", "tfoot", "tr"));

    /**
     * The commands of <code>execCommand</code> that change inline formatting, with the element that carries the format and the other
     * elements that mean the same.
     * <p>
     * 인라인 서식을 바꾸는 <code>execCommand</code> 명령과, 서식을 나타내는 요소 및 같은 뜻의 다른 요소.
     */
    private static final Map<String, String[]> FORMATS = new LinkedHashMap<String, String[]>();

    static {
        FORMATS.put("bold", new String[] {"b", "strong"});
        FORMATS.put("italic", new String[] {"i", "em"});
        FORMATS.put("underline", new String[] {"u"});
        FORMATS.put("strikethrough", new String[] {"s", "strike", "del"});
        FORMATS.put("subscript", new String[] {"sub"});
        FORMATS.put("superscript", new String[] {"sup"});
    }

    /**
     * The input types of the format commands (Input Events).
     * <p>
     * 서식 명령의 입력 종류(Input Events).
     */
    private static final Map<String, String> FORMAT_TYPES = new LinkedHashMap<String, String>();

    static {
        FORMAT_TYPES.put("bold", "formatBold");
        FORMAT_TYPES.put("italic", "formatItalic");
        FORMAT_TYPES.put("underline", "formatUnderline");
        FORMAT_TYPES.put("strikethrough", "formatStrikeThrough");
        FORMAT_TYPES.put("subscript", "formatSubscript");
        FORMAT_TYPES.put("superscript", "formatSuperscript");
    }

    /**
     * The default environment: events go to the target as trusted events, blocks and preserved white space are decided by element names.
     * <p>
     * 기본 환경: 이벤트는 신뢰 이벤트로 대상에 가고, 블록과 보존되는 공백은 요소 이름으로 정한다.
     */
    private static final Environment DEFAULT_ENVIRONMENT = new Environment() {
        /**
         * Sends the event as a trusted event.
         * <p>
         * 이벤트를 신뢰 이벤트로 보낸다.
         *
         * @param target the target / 대상
         * @param event  the event / 이벤트
         * @return false if cancelled / 취소되었으면 false
         */
        public boolean dispatch(Node target, Event event) {
            return target.dispatchTrustedEvent(event);
        }

        /**
         * Decides by the name of the element.
         * <p>
         * 요소의 이름으로 정한다.
         *
         * @param e the element / 요소
         * @return true for a block / 블록이면 true
         */
        public boolean isBlock(Element e) {
            return BLOCKS.contains(e.getLocalName());
        }

        /**
         * White space is preserved inside <code>pre</code>, <code>listing</code>, <code>plaintext</code>, <code>xmp</code> and
         * <code>textarea</code>, or where an element says <code>white-space: pre</code>, <code>pre-wrap</code> or
         * <code>break-spaces</code> in its <code>style</code> attribute.
         * <p>
         * 공백은 <code>pre</code>, <code>listing</code>, <code>plaintext</code>, <code>xmp</code>, <code>textarea</code> 안이나 요소의
         * <code>style</code> 속성이 <code>white-space: pre</code>, <code>pre-wrap</code>, <code>break-spaces</code> 인 곳에서 보존된다.
         *
         * @param node the node / 노드
         * @return true if preserved / 보존되면 true
         */
        public boolean preservesWhitespace(Node node) {
            // 위로 올라가며 이름이나 style 속성으로 공백을 보존하는 요소를 찾는다.
            for (Node n = node; n != null; n = n.getParentNode()) {
                if (!(n instanceof Element)) {
                    continue;
                }
                Element e = (Element) n;
                String name = e.getLocalName();
                if ("pre".equals(name) || "listing".equals(name) || "plaintext".equals(name) || "xmp".equals(name) || "textarea".equals(name)) {
                    return true;
                }
                String style = e.getAttribute("style");
                if (style != null) {
                    String s = style.toLowerCase(java.util.Locale.ENGLISH).replace(" ", "");
                    if (s.indexOf("white-space:pre") >= 0 || s.indexOf("white-space:break-spaces") >= 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    /**
     * A position: a node and an offset in it.
     * <p>
     * 위치: 노드와 그 안의 오프셋.
     */
    private static final class Pos {
        /** The node. 노드. */
        final Node node;
        /** The offset. 오프셋. */
        final int offset;

        /**
         * Creates a position.
         * <p>
         * 위치를 만든다.
         *
         * @param node   the node / 노드
         * @param offset the offset / 오프셋
         */
        Pos(Node node, int offset) {
            this.node = node;
            this.offset = offset;
        }
    }

    /**
     * One editing operation that runs inside {@link #perform}.
     * <p>
     * {@link #perform} 안에서 실행되는 편집 작업 하나.
     */
    private interface Operation {
        /**
         * Changes the DOM.
         * <p>
         * DOM 을 바꾼다.
         *
         * @param host the editing host / 편집 호스트
         */
        void run(Element host);
    }

    /**
     * The document.
     * <p>
     * 문서.
     */
    private final Document doc;

    /**
     * The undo history.
     * <p>
     * 되돌리기 이력.
     */
    private final UndoManager undo;

    /**
     * The environment.
     * <p>
     * 환경.
     */
    private Environment env = DEFAULT_ENVIRONMENT;

    /**
     * The element that a paragraph break creates after a heading or in the host (<code>defaultParagraphSeparator</code>).
     * <p>
     * 제목 뒤나 호스트 안에서 문단 나누기가 만드는 요소(<code>defaultParagraphSeparator</code>).
     */
    private String paragraphSeparator = "div";

    /**
     * The range of the text that an input method is composing (a live range, so that changes of the tree move it), or null when no
     * composition runs.
     * <p>
     * 입력기가 조합 중인 텍스트의 범위(트리의 변경을 따라 움직이는 살아 있는 범위). 진행 중인 조합이 없으면 null.
     */
    private Range composition;

    /**
     * True from the start of a composition until its first text was put into the DOM (the selection that the composition replaces is
     * still in place).
     * <p>
     * 조합이 시작한 뒤 첫 텍스트가 DOM 에 들어가기까지 true(조합이 대신할 선택이 아직 그대로 있다).
     */
    private boolean compositionFresh;

    /**
     * The formats that the user switched with a collapsed caret, which apply to the next typed text: command name to on or off.
     * <p>
     * 접힌 캐럿에서 사용자가 바꾼 서식이며 다음에 입력할 텍스트에 적용된다: 명령 이름에서 켬/끔으로.
     */
    private final Map<String, Boolean> pending = new LinkedHashMap<String, Boolean>();

    /**
     * The caret node where {@link #pending} was set, so that it is dropped when the caret moved.
     * <p>
     * {@link #pending} 을 정한 곳의 캐럿 노드. 캐럿이 옮겨 가면 버리기 위해 둔다.
     */
    private Node pendingNode;

    /**
     * The caret offset where {@link #pending} was set.
     * <p>
     * {@link #pending} 을 정한 곳의 캐럿 오프셋.
     */
    private int pendingOffset;

    /**
     * Creates the editor of a document (use {@link Document#getEditor()}).
     * <p>
     * 문서의 편집기를 만든다({@link Document#getEditor()} 를 쓴다).
     *
     * @param doc the document / 문서
     */
    public Editor(Document doc) {
        this.doc = doc;
        this.undo = new UndoManager(doc);
    }

    /**
     * Sets the environment (null restores the default).
     * <p>
     * 환경을 정한다(null 이면 기본값).
     *
     * @param environment the environment or null / 환경. 없으면 null
     */
    public void setEnvironment(Environment environment) {
        this.env = environment == null ? DEFAULT_ENVIRONMENT : environment;
    }

    /**
     * Returns the undo history.
     * <p>
     * 되돌리기 이력을 반환한다.
     *
     * @return the history / 이력
     */
    public UndoManager getUndoManager() {
        return undo;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 선택과 편집 호스트
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the editing host of the selection: the one host that holds both ends of the selection, or null if there is no selection or it is
     * not inside one editing host.
     * <p>
     * 선택의 편집 호스트를 반환한다: 선택의 양 끝을 모두 가진 편집 호스트 하나. 선택이 없거나 한 편집 호스트 안이 아니면 null.
     *
     * @return the host or null / 호스트. 없으면 null
     */
    public Element getSelectionHost() {
        Selection sel = doc.getSelection();
        if (sel.getRangeCount() == 0) {
            return null;
        }
        Range r = sel.getRangeAt(0);
        Element a = EditingHosts.hostOf(r.getStartContainer());
        Element b = EditingHosts.hostOf(r.getEndContainer());
        return a != null && a == b && a.isConnected() ? a : null;
    }

    /**
     * Tells whether the selection is inside an editing host, so that editing keys and commands act on it.
     * <p>
     * 선택이 편집 호스트 안이어서 편집 키와 명령이 거기에 작용하는지 알려 준다.
     *
     * @return true if editing applies / 편집이 적용되면 true
     */
    public boolean isEditing() {
        return getSelectionHost() != null;
    }

    /**
     * Returns the position of the focus end of the selection.
     * <p>
     * 선택의 focus 쪽 끝의 위치를 반환한다.
     *
     * @return the position / 위치
     */
    private Pos caret() {
        Selection s = doc.getSelection();
        return new Pos(s.getFocusNode(), s.getFocusOffset());
    }

    /**
     * Puts the collapsed selection at a position.
     * <p>
     * 접힌 선택을 위치에 둔다.
     *
     * @param p the position / 위치
     */
    private void setCaret(Pos p) {
        doc.getSelection().collapse(p.node, p.offset);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 작은 도우미
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the index of a node among the children of its parent.
     * <p>
     * 노드의 부모 안에서의 위치를 반환한다.
     *
     * @param n the node / 노드
     * @return the index / 위치
     */
    private static int indexOf(Node n) {
        int i = 0;
        // 앞 형제를 세어 위치를 구한다.
        for (Node s = n.getPreviousSibling(); s != null; s = s.getPreviousSibling()) {
            i++;
        }
        return i;
    }

    /**
     * Returns the child of a node at an index, or null.
     * <p>
     * 노드의 한 위치의 자식을 반환한다. 없으면 null.
     *
     * @param parent the node / 노드
     * @param index  the index / 위치
     * @return the child or null / 자식. 없으면 null
     */
    private static Node childAt(Node parent, int index) {
        if (index < 0) {
            return null;
        }
        Node c = parent.getFirstChild();
        // 앞에서부터 위치만큼 형제를 건너뛴다.
        for (int i = 0; i < index && c != null; i++) {
            c = c.getNextSibling();
        }
        return c;
    }

    /**
     * Returns the number of children of a node.
     * <p>
     * 노드의 자식 수를 반환한다.
     *
     * @param n the node / 노드
     * @return the number / 수
     */
    private static int childCount(Node n) {
        int c = 0;
        // 자식을 세어 수를 구한다.
        for (Node k = n.getFirstChild(); k != null; k = k.getNextSibling()) {
            c++;
        }
        return c;
    }

    /**
     * Tells whether a node is an element with a local name.
     * <p>
     * 노드가 어떤 로컬 이름의 요소인지 알려 준다.
     *
     * @param n    the node / 노드
     * @param name the name / 이름
     * @return true if it is / 맞으면 true
     */
    private static boolean isNamed(Node n, String name) {
        return n instanceof Element && name.equals(((Element) n).getLocalName());
    }

    /**
     * Tells whether an element is block-level.
     * <p>
     * 요소가 블록 수준인지 알려 준다.
     *
     * @param n the node / 노드
     * @return true for a block element / 블록 요소이면 true
     */
    private boolean isBlock(Node n) {
        return n instanceof Element && env.isBlock((Element) n);
    }

    /**
     * Tells whether an element is one unit for editing (an image, a control ...).
     * <p>
     * 요소가 편집에서 한 단위(이미지, 컨트롤 등)인지 알려 준다.
     *
     * @param n the node / 노드
     * @return true for an atomic element / 한 단위 요소이면 true
     */
    private static boolean isAtomic(Node n) {
        return n instanceof Element && ATOMIC.contains(((Element) n).getLocalName());
    }

    /**
     * Tells whether a node is a leaf for deleting and moving: a text node with data, a <code>br</code>, or an atomic element.
     * <p>
     * 노드가 지우기와 이동의 잎인지 알려 준다: 데이터가 있는 텍스트 노드, <code>br</code>, 한 단위 요소.
     *
     * @param n the node / 노드
     * @return true for a leaf / 잎이면 true
     */
    private static boolean isLeaf(Node n) {
        if (n instanceof Text) {
            return ((Text) n).getLength() > 0;
        }
        return isNamed(n, "br") || isAtomic(n);
    }

    /**
     * Returns the nearest block of a node, or the editing host when no block is between them.
     * <p>
     * 노드의 가장 가까운 블록을 반환한다. 사이에 블록이 없으면 편집 호스트.
     *
     * @param n    the node / 노드
     * @param host the editing host / 편집 호스트
     * @return the block / 블록
     */
    private Element blockOf(Node n, Element host) {
        // 위로 올라가며 호스트에 닿기 전의 첫 블록 요소를 찾는다.
        for (Node c = n; c != null && c != host; c = c.getParentNode()) {
            if (isBlock(c)) {
                return (Element) c;
            }
        }
        return host;
    }

    /**
     * Tells whether a node shows something to the user (text that is not only collapsible white space, or an atomic element); a
     * <code>br</code> alone is not content.
     * <p>
     * 노드가 사용자에게 무언가를 보이는지 알려 준다(접히는 공백만이 아닌 텍스트, 또는 한 단위 요소). <code>br</code> 하나는 내용이 아니다.
     *
     * @param n the node / 노드
     * @return true if visible content / 보이는 내용이면 true
     */
    private static boolean hasVisibleContent(Node n) {
        if (n instanceof Text) {
            String d = ((Text) n).getData();
            // 텍스트의 문자 중 접히지 않는 것이 하나라도 있으면 보이는 내용이다.
            for (int i = 0; i < d.length(); i++) {
                if (!TextUnits.isCollapsible(d.charAt(i))) {
                    return true;
                }
            }
            return false;
        }
        if (isAtomic(n)) {
            return true;
        }
        // 자식 중 보이는 내용이 있는지 본다.
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (hasVisibleContent(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the last leaf inside a node in tree order, or null.
     * <p>
     * 노드 안의 트리 순서로 마지막 잎을 반환한다. 없으면 null.
     *
     * @param n the node / 노드
     * @return the leaf or null / 잎. 없으면 null
     */
    private static Node lastLeaf(Node n) {
        if (isLeaf(n)) {
            return n;
        }
        if (n instanceof Text || isAtomic(n)) {
            return null;
        }
        // 마지막 자식부터 거꾸로 잎을 찾는다.
        for (Node c = n.getLastChild(); c != null; c = c.getPreviousSibling()) {
            Node leaf = lastLeaf(c);
            if (leaf != null) {
                return leaf;
            }
        }
        return null;
    }

    /**
     * Returns the first leaf inside a node in tree order, or null.
     * <p>
     * 노드 안의 트리 순서로 첫 잎을 반환한다. 없으면 null.
     *
     * @param n the node / 노드
     * @return the leaf or null / 잎. 없으면 null
     */
    private static Node firstLeaf(Node n) {
        if (isLeaf(n)) {
            return n;
        }
        if (n instanceof Text || isAtomic(n)) {
            return null;
        }
        // 첫 자식부터 잎을 찾는다.
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            Node leaf = firstLeaf(c);
            if (leaf != null) {
                return leaf;
            }
        }
        return null;
    }

    /**
     * Returns the leaf that is closest before a node (not inside it) inside the editing host.
     * <p>
     * 편집 호스트 안에서 노드 앞(노드 안이 아님)의 가장 가까운 잎을 반환한다.
     *
     * @param from the node / 노드
     * @param host the editing host / 편집 호스트
     * @return the leaf or null / 잎. 없으면 null
     */
    private static Node leafBefore(Node from, Element host) {
        // 앞 형제의 마지막 잎을 찾고, 없으면 부모로 올라가 같은 일을 한다.
        for (Node c = from; c != null && c != host; c = c.getParentNode()) {
            for (Node p = c.getPreviousSibling(); p != null; p = p.getPreviousSibling()) {
                Node leaf = lastLeaf(p);
                if (leaf != null) {
                    return leaf;
                }
            }
        }
        return null;
    }

    /**
     * Returns the leaf that is closest after a node (not inside it) inside the editing host.
     * <p>
     * 편집 호스트 안에서 노드 뒤(노드 안이 아님)의 가장 가까운 잎을 반환한다.
     *
     * @param from the node / 노드
     * @param host the editing host / 편집 호스트
     * @return the leaf or null / 잎. 없으면 null
     */
    private static Node leafAfter(Node from, Element host) {
        // 뒤 형제의 첫 잎을 찾고, 없으면 부모로 올라가 같은 일을 한다.
        for (Node c = from; c != null && c != host; c = c.getParentNode()) {
            for (Node n = c.getNextSibling(); n != null; n = n.getNextSibling()) {
                Node leaf = firstLeaf(n);
                if (leaf != null) {
                    return leaf;
                }
            }
        }
        return null;
    }

    /**
     * Returns the leaf at or before a position (the closest one that ends at or before it).
     * <p>
     * 위치에서 끝나거나 그 앞에 있는 가장 가까운 잎을 반환한다.
     *
     * @param p    the position / 위치
     * @param host the editing host / 편집 호스트
     * @return the leaf or null / 잎. 없으면 null
     */
    private static Node leafAtOrBefore(Pos p, Element host) {
        if (p.node instanceof Text) {
            return leafBefore(p.node, host);
        }
        // 요소 안의 위치이면 위치 앞의 자식들에서 마지막 잎을 찾고, 없으면 요소 앞으로 간다.
        for (int i = p.offset - 1; i >= 0; i--) {
            Node c = childAt(p.node, i);
            Node leaf = c == null ? null : lastLeaf(c);
            if (leaf != null) {
                return leaf;
            }
        }
        return p.node == host ? null : leafBefore(p.node, host);
    }

    /**
     * Returns the leaf at or after a position.
     * <p>
     * 위치에서 시작하거나 그 뒤에 있는 가장 가까운 잎을 반환한다.
     *
     * @param p    the position / 위치
     * @param host the editing host / 편집 호스트
     * @return the leaf or null / 잎. 없으면 null
     */
    private static Node leafAtOrAfter(Pos p, Element host) {
        if (p.node instanceof Text) {
            return leafAfter(p.node, host);
        }
        int n = childCount(p.node);
        // 요소 안의 위치이면 위치 뒤의 자식들에서 첫 잎을 찾고, 없으면 요소 뒤로 간다.
        for (int i = p.offset; i < n; i++) {
            Node c = childAt(p.node, i);
            Node leaf = c == null ? null : firstLeaf(c);
            if (leaf != null) {
                return leaf;
            }
        }
        return p.node == host ? null : leafAfter(p.node, host);
    }

    /**
     * Tells whether something is shown on the same line before a text node (text or an atomic element in the same block, up to a
     * <code>br</code>); the leading white space of a line is not collapsed away when nothing precedes it.
     * <p>
     * 텍스트 노드 앞의 같은 줄에 보이는 것이 있는지 알려 준다(같은 블록의 텍스트나 한 단위 요소, <code>br</code> 까지). 앞에 아무것도 없는 줄의 시작 공백은
     * 접혀 사라지지 않는다.
     *
     * @param t    the text node / 텍스트 노드
     * @param host the editing host / 편집 호스트
     * @return true if something precedes / 앞에 있으면 true
     */
    private boolean contentBefore(Text t, Element host) {
        Node leaf = leafBefore(t, host);
        if (leaf == null || blockOf(leaf, host) != blockOf(t, host) || isNamed(leaf, "br")) {
            return false;
        }
        return hasVisibleContent(leaf);
    }

    /**
     * Tells whether something is shown on the same line after a text node.
     * <p>
     * 텍스트 노드 뒤의 같은 줄에 보이는 것이 있는지 알려 준다.
     *
     * @param t    the text node / 텍스트 노드
     * @param host the editing host / 편집 호스트
     * @return true if something follows / 뒤에 있으면 true
     */
    private boolean contentAfter(Text t, Element host) {
        Node leaf = leafAfter(t, host);
        if (leaf == null || blockOf(leaf, host) != blockOf(t, host) || isNamed(leaf, "br")) {
            return false;
        }
        return hasVisibleContent(leaf);
    }

    /**
     * Returns the position at the start of a node's content: inside the first text when the content starts with one.
     * <p>
     * 노드 내용의 시작 위치를 반환한다. 내용이 텍스트로 시작하면 그 텍스트 안.
     *
     * @param n the node / 노드
     * @return the position / 위치
     */
    private static Pos startOf(Node n) {
        Node c = n;
        // 첫 자식이 인라인 요소이면 그 안으로 들어간다.
        while (c.getFirstChild() instanceof Element && !isNamed(c.getFirstChild(), "br") && !isAtomic(c.getFirstChild())) {
            c = c.getFirstChild();
        }
        Node first = c.getFirstChild();
        return first instanceof Text ? new Pos(first, 0) : new Pos(c, 0);
    }

    /**
     * Returns the position at the end of a node's content: inside the last text when the content ends with one.
     * <p>
     * 노드 내용의 끝 위치를 반환한다. 내용이 텍스트로 끝나면 그 텍스트 안.
     *
     * @param n the node / 노드
     * @return the position / 위치
     */
    private static Pos endOf(Node n) {
        Node c = n;
        // 마지막 자식이 인라인 요소이면 그 안으로 들어간다.
        while (c.getLastChild() instanceof Element && !isNamed(c.getLastChild(), "br") && !isAtomic(c.getLastChild())) {
            c = c.getLastChild();
        }
        Node last = c.getLastChild();
        return last instanceof Text ? new Pos(last, ((Text) last).getLength()) : new Pos(c, childCount(c));
    }

    /**
     * Puts a placeholder <code>br</code> into a block that shows nothing, so that the block keeps its height and has a place for the caret.
     * <p>
     * 아무것도 보이지 않는 블록에 자리표시 <code>br</code> 을 넣어, 블록이 높이를 유지하고 캐럿 자리를 갖게 한다.
     *
     * @param block the block / 블록
     */
    private void ensurePlaceholder(Element block) {
        if (!hasVisibleContent(block) && !hasBr(block)) {
            block.appendChild(doc.createElement("br"));
        }
    }

    /**
     * Tells whether a node has a <code>br</code> child.
     * <p>
     * 노드가 <code>br</code> 자식을 가졌는지 알려 준다.
     *
     * @param n the node / 노드
     * @return true if it has / 있으면 true
     */
    private static boolean hasBr(Node n) {
        // 자식 중 br 이 있는지 본다.
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (isNamed(c, "br")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the placeholder <code>br</code> children of a block that shows nothing (it is about to receive content).
     * <p>
     * 아무것도 보이지 않는 블록의 자리표시 <code>br</code> 자식을 제거한다(내용을 받으려는 참이다).
     *
     * @param block the block / 블록
     */
    private void stripPlaceholders(Element block) {
        if (hasVisibleContent(block)) {
            return;
        }
        Node c = block.getFirstChild();
        // 자식 중 br 을 모두 제거한다.
        while (c != null) {
            Node next = c.getNextSibling();
            if (isNamed(c, "br")) {
                block.removeChild(c);
            }
            c = next;
        }
    }

    /**
     * Removes a node and the ancestors that it leaves empty, up to (not including) the editing host.
     * <p>
     * 노드와, 그 때문에 비게 된 조상을 편집 호스트 앞까지 제거한다.
     *
     * @param n    the node / 노드
     * @param host the editing host / 편집 호스트
     */
    private static void removeWithEmptyAncestors(Node n, Element host) {
        Node c = n;
        // 노드를 제거하고, 부모가 비면 부모도 제거하기를 호스트 앞까지 되풀이한다.
        while (c != null && c != host && c.getParentNode() != null) {
            Node parent = c.getParentNode();
            parent.removeChild(c);
            if (parent == host || parent.getFirstChild() != null) {
                break;
            }
            c = parent;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 공백 재조정
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Rebalances the white space of a text node so that every space that the user typed stays visible: a run of spaces becomes
     * no-break spaces and a normal space at its end (the line can still wrap there), and a run at the start or end of a line becomes
     * no-break spaces only, because HTML collapses the other kinds away. Text with preserved white space is left alone. The length of the
     * data does not change, so the selection stays valid.
     * <p>
     * 입력한 공백이 모두 보이도록 텍스트 노드의 공백을 재조정한다: 공백 연속은 NBSP 와 끝의 일반 공백 하나가 되고(그곳에서 줄을 바꿀 수 있다), 줄의 시작이나 끝의
     * 연속은 NBSP 만 된다. HTML 이 다른 모양은 접어 없애기 때문이다. 공백이 보존되는 텍스트는 건드리지 않는다. 데이터 길이는 변하지 않으므로 선택은 유효하다.
     *
     * @param t    the text node / 텍스트 노드
     * @param host the editing host / 편집 호스트
     */
    private void rebalance(Text t, Element host) {
        if (!t.isConnected() || env.preservesWhitespace(t)) {
            return;
        }
        String d = t.getData();
        boolean before = contentBefore(t, host);
        boolean after = contentAfter(t, host);
        int i = 0;
        // 공백 연속(접히는 공백과 NBSP)을 하나씩 찾아 규칙대로 다시 쓴다.
        while (i < d.length()) {
            char c = d.charAt(i);
            if (!TextUnits.isCollapsible(c) && c != '\u00A0') {
                i++;
                continue;
            }
            int j = i;
            // 연속의 끝을 찾는다.
            while (j < d.length() && (TextUnits.isCollapsible(d.charAt(j)) || d.charAt(j) == '\u00A0')) {
                j++;
            }
            boolean leading = i == 0 && !before;
            boolean trailing = j == d.length() && !after;
            // 연속의 문자마다 원하는 문자와 다르면 한 글자씩 바꾼다(길이가 같아 선택의 오프셋은 유지된다).
            for (int k = i; k < j; k++) {
                char want = leading || trailing || k < j - 1 ? '\u00A0' : ' ';
                if (d.charAt(k) != want) {
                    t.replaceData(k, 1, String.valueOf(want));
                }
            }
            i = j;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 입력 이벤트와 작업 실행
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the target ranges of an operation that changes what the selection covers.
     * <p>
     * 선택이 덮는 것을 바꾸는 작업의 대상 범위를 반환한다.
     *
     * @return the ranges / 범위
     */
    private List<StaticRange> selectionRanges() {
        List<StaticRange> out = new ArrayList<StaticRange>();
        Selection sel = doc.getSelection();
        if (sel.getRangeCount() > 0) {
            Range r = sel.getRangeAt(0);
            out.add(new StaticRange(r.getStartContainer(), r.getStartOffset(), r.getEndContainer(), r.getEndOffset()));
        }
        return out;
    }

    /**
     * Returns a static range of the content between two positions.
     * <p>
     * 두 위치 사이의 내용의 정적 범위를 반환한다.
     *
     * @param a the first position / 첫 위치
     * @param b the second position / 둘째 위치
     * @return the range / 범위
     */
    private static StaticRange between(Pos a, Pos b) {
        return new StaticRange(a.node, a.offset, b.node, b.offset);
    }

    /**
     * Runs an editing operation with the events and the undo step around it: <code>beforeinput</code> (if a listener cancels it nothing
     * happens), the change of the DOM as one step of the history, then <code>input</code>.
     * <p>
     * 이벤트와 되돌리기 단계로 둘러싸 편집 작업을 실행한다: <code>beforeinput</code>(리스너가 취소하면 아무 일도 없다), DOM 변경(이력의 한 단계), 그리고
     * <code>input</code>.
     *
     * @param inputType the input type / 입력 종류
     * @param data      the <code>data</code> of the events, or null / 이벤트의 <code>data</code>. 없으면 null
     * @param transfer  the <code>dataTransfer</code> of the events, or null / 이벤트의 <code>dataTransfer</code>. 없으면 null
     * @param ranges    the target ranges of <code>beforeinput</code> / <code>beforeinput</code> 의 대상 범위
     * @param mergeKey  the key that merges consecutive steps, or null / 이어지는 단계를 합치는 키. 없으면 null
     * @param op        the operation / 작업
     * @return true if the operation ran / 작업을 했으면 true
     */
    private boolean perform(String inputType, String data, ClipboardData transfer, List<StaticRange> ranges, String mergeKey, Operation op) {
        Element host = getSelectionHost();
        if (host == null) {
            return false;
        }
        // 조합 텍스트 입력(insertCompositionText)의 beforeinput 은 취소할 수 없고 isComposing 이다(Input Events).
        boolean composing = "insertCompositionText".equals(inputType);
        InputEvent before = new InputEvent("beforeinput", true, !composing);
        before.setInput(data, inputType, composing);
        before.setDataTransfer(transfer);
        before.setTargetRanges(ranges);
        // 리스너가 취소하면 아무것도 바꾸지 않는다.
        if (!env.dispatch(host, before)) {
            return false;
        }
        // 리스너가 선택이나 문서를 바꾸었을 수 있으므로 호스트를 다시 확인한다.
        host = getSelectionHost();
        if (host == null) {
            return false;
        }
        boolean opened = undo.begin();
        try {
            op.run(host);
        } finally {
            if (opened) {
                undo.end(mergeKey);
            }
        }
        if (host.isConnected()) {
            InputEvent after = new InputEvent("input", true, false);
            after.setInput(data, inputType, composing);
            after.setDataTransfer(transfer);
            env.dispatch(host, after);
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 텍스트 입력
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Types text at the caret (<code>insertText</code>): the selected content is replaced; a line feed in the text becomes a line break.
     * <p>
     * 캐럿에 텍스트를 입력한다(<code>insertText</code>): 선택된 내용은 바뀌고, 텍스트의 줄바꿈은 줄 바꿈이 된다.
     *
     * @param text the text / 텍스트
     * @return true if the text was inserted (false: no editable selection, empty text, or a listener cancelled) / 넣었으면 true(편집 가능한 선택이 없거나 빈
     *         텍스트이거나 리스너가 취소하면 false)
     */
    public boolean insertText(final String text) {
        return insertText(text, "insertText");
    }

    /**
     * Inserts text for an input type (<code>insertText</code>, <code>insertFromDrop</code> ...).
     * <p>
     * 입력 종류에 맞게 텍스트를 넣는다(<code>insertText</code>, <code>insertFromDrop</code> 등).
     *
     * @param text      the text / 텍스트
     * @param inputType the input type / 입력 종류
     * @return true if inserted / 넣었으면 true
     */
    private boolean insertText(final String text, String inputType) {
        if (text == null || text.length() == 0 || !isEditing()) {
            return false;
        }
        boolean typing = "insertText".equals(inputType) && text.indexOf('\n') < 0 && !isEditingRangeNonCollapsed();
        return perform(inputType, text, null, selectionRanges(), typing ? "typing" : null, new Operation() {
            /**
             * Inserts the text.
             * <p>
             * 텍스트를 넣는다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                doInsertText(host, text);
            }
        });
    }

    /**
     * Tells whether the selection covers content (typing then replaces it, which does not merge with earlier typing).
     * <p>
     * 선택이 내용을 덮는지 알려 준다(그러면 입력은 그것을 바꾸며 앞의 입력과 합쳐지지 않는다).
     *
     * @return true if not collapsed / 접혀 있지 않으면 true
     */
    private boolean isEditingRangeNonCollapsed() {
        Selection s = doc.getSelection();
        return s.getRangeCount() > 0 && !s.isCollapsed();
    }

    /**
     * Inserts text at the caret after deleting the selection, handles line feeds, and applies the formats that the user switched.
     * <p>
     * 선택을 지운 뒤 캐럿에 텍스트를 넣고, 줄바꿈을 처리하며, 사용자가 바꾼 서식을 적용한다.
     *
     * @param host the editing host / 편집 호스트
     * @param text the text / 텍스트
     */
    private void doInsertText(Element host, String text) {
        deleteSelectionIfAny(host);
        Pos p = caret();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Pos typedStart = null;
        Pos typedEnd = null;
        // 줄마다 텍스트를 넣고 줄 사이에는 줄 바꿈(보존되는 공백에서는 줄바꿈 문자)을 넣는다.
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                p = lineBreakAt(p, host);
            }
            if (lines[i].length() > 0) {
                p = insertPlain(p, lines[i], host);
                if (typedStart == null) {
                    typedStart = new Pos(p.node, p.offset - lines[i].length());
                }
                typedEnd = p;
            }
        }
        setCaret(p);
        // 접힌 캐럿에서 바꾼 서식(굵게 등)은 방금 넣은 텍스트에 적용한다.
        if (typedStart != null && typedEnd != null && typedStart.node == typedEnd.node && !pending.isEmpty() && pendingNode == typedStart.node
                && pendingOffset == typedStart.offset) {
            applyPending((Text) typedStart.node, typedStart.offset, typedEnd.offset, host);
        } else {
            pending.clear();
        }
    }

    /**
     * Inserts plain text into the text node at a position (a text node is made if the position is not in one) and returns the position
     * after it.
     * <p>
     * 위치의 텍스트 노드에 일반 텍스트를 넣고(위치가 텍스트 안이 아니면 텍스트 노드를 만든다) 그 뒤의 위치를 반환한다.
     *
     * @param p    the position / 위치
     * @param text the text without line feeds / 줄바꿈 없는 텍스트
     * @param host the editing host / 편집 호스트
     * @return the position after the text / 텍스트 뒤의 위치
     */
    private Pos insertPlain(Pos p, String text, Element host) {
        Text t;
        int off;
        boolean created = false;
        if (p.node instanceof Text) {
            t = (Text) p.node;
            off = p.offset;
        } else {
            Node before = childAt(p.node, p.offset - 1);
            Node after = childAt(p.node, p.offset);
            // 위치 앞이나 뒤의 텍스트 노드를 쓰고, 없으면 새 텍스트 노드를 만든다.
            if (before instanceof Text) {
                t = (Text) before;
                off = t.getLength();
            } else if (after instanceof Text) {
                t = (Text) after;
                off = 0;
            } else {
                t = doc.createTextNode("");
                p.node.insertBefore(t, after);
                off = 0;
                created = true;
            }
        }
        t.insertData(off, text);
        // 줄의 처음(앞이 없거나 br)에 새 텍스트를 만들었고 바로 뒤가 블록의 마지막 br(자리표시)이면, 그 줄에 내용이 생겼으므로 자리표시를 지운다.
        if (created && isNamed(t.getNextSibling(), "br") && t.getNextSibling().getNextSibling() == null
                && (t.getPreviousSibling() == null || isNamed(t.getPreviousSibling(), "br"))) {
            t.getParentNode().removeChild(t.getNextSibling());
        }
        rebalance(t, host);
        return new Pos(t, off + text.length());
    }

    /**
     * Deletes the content of the selection if it is not collapsed.
     * <p>
     * 선택이 접혀 있지 않으면 그 내용을 지운다.
     *
     * @param host the editing host / 편집 호스트
     */
    private void deleteSelectionIfAny(Element host) {
        Selection s = doc.getSelection();
        if (s.getRangeCount() > 0 && !s.isCollapsed()) {
            deleteSelectionContents(host);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 지우기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Deletes the content of the selection (the live range of the selection): the range content goes, empty inline elements that it leaves
     * are removed, the two blocks at its ends are joined when it crossed a block boundary, and an emptied block gets its placeholder. The
     * caret is left at the place of the deletion.
     * <p>
     * 선택(선택의 살아 있는 범위)의 내용을 지운다: 범위 내용이 사라지고, 그것이 남기는 빈 인라인 요소는 제거되며, 범위가 블록 경계를 넘었으면 양 끝의 두 블록을 합치고,
     * 비게 된 블록에는 자리표시를 넣는다. 캐럿은 지운 자리에 남는다.
     *
     * @param host the editing host / 편집 호스트
     */
    private void deleteSelectionContents(Element host) {
        Selection sel = doc.getSelection();
        if (sel.getRangeCount() == 0 || sel.isCollapsed()) {
            return;
        }
        Range r = sel.getRangeAt(0);
        Element startBlock = blockOf(r.getStartContainer(), host);
        Element endBlock = blockOf(r.getEndContainer(), host);
        r.deleteContents();
        Pos p = cleanup(new Pos(r.getStartContainer(), r.getStartOffset()), host);
        // 범위가 블록 경계를 넘었으면 양 끝의 블록을 합친다.
        if (startBlock != endBlock && startBlock != host && endBlock != host && startBlock.isConnected() && endBlock.isConnected()
                && !startBlock.contains(endBlock) && !endBlock.contains(startBlock)) {
            p = mergeBlocks(startBlock, endBlock, host, p);
        } else if (startBlock != host) {
            ensurePlaceholder(startBlock);
        }
        setCaret(settle(p));
    }

    /**
     * Removes the empty text nodes and the empty inline elements that a deletion left around a position, going up from it; the position
     * moves with them.
     * <p>
     * 지우기가 위치 둘레에 남긴 빈 텍스트 노드와 빈 인라인 요소를 위로 올라가며 제거한다. 위치는 그것들과 함께 옮겨진다.
     *
     * @param p    the position / 위치
     * @param host the editing host / 편집 호스트
     * @return the position after the clean-up / 정리한 뒤의 위치
     */
    private Pos cleanup(Pos p, Element host) {
        Node n = p.node;
        int o = p.offset;
        // 비게 된 텍스트 노드나 인라인 요소는 제거하고 위치를 그 자리(부모 안)로 올린다.
        while (n != null && n != host) {
            boolean emptyText = n instanceof Text && ((Text) n).getLength() == 0;
            boolean emptyInline = n instanceof Element && n.getFirstChild() == null && !isBlock(n) && !isAtomic(n) && !isNamed(n, "br");
            if (!emptyText && !emptyInline) {
                break;
            }
            Node parent = n.getParentNode();
            if (parent == null) {
                break;
            }
            int idx = indexOf(n);
            parent.removeChild(n);
            n = parent;
            o = idx;
        }
        return new Pos(n, o);
    }

    /**
     * Moves the content of the block <code>src</code> to the end of the block <code>dest</code> and removes the emptied block (and the
     * ancestors that it leaves empty); the placeholders that would show as an extra line are removed. Returns the position where the two
     * contents meet.
     * <p>
     * 블록 <code>src</code> 의 내용을 블록 <code>dest</code> 의 끝으로 옮기고 비게 된 블록(과 그 때문에 빈 조상)을 제거한다. 추가 줄로 보일 자리표시는 제거한다.
     * 두 내용이 만나는 위치를 반환한다.
     *
     * @param dest the block that keeps the content / 내용을 가지는 블록
     * @param src  the block that is emptied / 비워지는 블록
     * @param host the editing host / 편집 호스트
     * @param join the position that the caret had, which is used when the dest block shows nothing / 캐럿이 있던 위치. dest 가 비어 있으면 쓴다
     * @return the position of the join / 합쳐진 위치
     */
    private Pos mergeBlocks(Element dest, Element src, Element host, Pos join) {
        stripPlaceholders(dest);
        Pos at = hasVisibleContent(dest) ? endOf(dest) : new Pos(dest, 0);
        if (hasVisibleContent(dest) || hasVisibleContent(src)) {
            stripPlaceholders(src);
        }
        // src 의 자식을 모두 dest 의 끝으로 옮긴다.
        Node child = src.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            dest.appendChild(child);
            child = next;
        }
        removeWithEmptyAncestors(src, host);
        ensurePlaceholder(dest);
        return hasVisibleContent(dest) || at.node != dest ? at : startOf(dest);
    }

    /**
     * Returns a good caret position for a position: a position in a text node when one is next to it.
     * <p>
     * 위치에 대해 좋은 캐럿 위치를 반환한다. 텍스트 노드가 옆에 있으면 그 안의 위치.
     *
     * @param p the position / 위치
     * @return the position / 위치
     */
    private static Pos settle(Pos p) {
        if (p.node instanceof Text) {
            return p;
        }
        Node before = childAt(p.node, p.offset - 1);
        Node after = childAt(p.node, p.offset);
        if (before instanceof Text) {
            return new Pos(before, ((Text) before).getLength());
        }
        return after instanceof Text ? new Pos(after, 0) : p;
    }

    /**
     * Deletes one character (or word) backward or forward from the caret (<code>deleteContentBackward</code>,
     * <code>deleteContentForward</code>, <code>deleteWordBackward</code>, <code>deleteWordForward</code>); a selection is deleted whole.
     * <p>
     * 캐럿에서 뒤나 앞으로 글자 하나(또는 낱말 하나)를 지운다(<code>deleteContentBackward</code>, <code>deleteContentForward</code>,
     * <code>deleteWordBackward</code>, <code>deleteWordForward</code>). 선택은 통째로 지운다.
     *
     * @param backward true for Backspace, false for Delete / Backspace 이면 true, Delete 이면 false
     * @param word     true to delete a word / 낱말이면 true
     * @return true if something was deleted / 지웠으면 true
     */
    public boolean delete(final boolean backward, final boolean word) {
        Element host = getSelectionHost();
        if (host == null) {
            return false;
        }
        Selection sel = doc.getSelection();
        String type;
        if (!sel.isCollapsed()) {
            type = backward ? "deleteContentBackward" : "deleteContentForward";
        } else {
            type = word ? (backward ? "deleteWordBackward" : "deleteWordForward") : (backward ? "deleteContentBackward" : "deleteContentForward");
            // 지울 것이 없는 자리(호스트의 시작에서 Backspace 등)에서는 이벤트도 보내지 않는다.
            if (!canDeleteUnit(host, backward)) {
                return false;
            }
        }
        List<StaticRange> ranges = selectionRanges();
        StaticRange unit = sel.isCollapsed() ? unitRange(backward, word) : null;
        // 접힌 캐럿이 텍스트 안에서 글자(낱말)를 지우면 지워질 범위를 대상 범위로 알린다.
        if (unit != null) {
            ranges = new ArrayList<StaticRange>();
            ranges.add(unit);
        }
        return perform(type, null, null, ranges, "delete", new Operation() {
            /**
             * Deletes.
             * <p>
             * 지운다.
             *
             * @param h the editing host / 편집 호스트
             */
            public void run(Element h) {
                doDelete(h, backward, word);
            }
        });
    }

    /**
     * Returns the range that a deletion of one unit inside the text node at the caret removes, or null when the unit is not inside that
     * text node.
     * <p>
     * 캐럿의 텍스트 노드 안에서 한 단위를 지울 때 지워지는 범위를 반환한다. 그 단위가 그 텍스트 노드 안이 아니면 null.
     *
     * @param backward true for backward / 뒤로이면 true
     * @param word     true for a word / 낱말이면 true
     * @return the range or null / 범위. 없으면 null
     */
    private StaticRange unitRange(boolean backward, boolean word) {
        Pos p = caret();
        if (!(p.node instanceof Text)) {
            return null;
        }
        String d = ((Text) p.node).getData();
        if (backward && p.offset > 0) {
            return new StaticRange(p.node, word ? TextUnits.wordBefore(d, p.offset) : TextUnits.previous(d, p.offset), p.node, p.offset);
        }
        if (!backward && p.offset < d.length()) {
            return new StaticRange(p.node, p.offset, p.node, word ? TextUnits.wordAfter(d, p.offset) : TextUnits.next(d, p.offset));
        }
        return null;
    }

    /**
     * Tells whether there is anything to delete next to the collapsed caret.
     * <p>
     * 접힌 캐럿 옆에 지울 것이 있는지 알려 준다.
     *
     * @param host     the editing host / 편집 호스트
     * @param backward true to look before the caret / 캐럿 앞을 보려면 true
     * @return true if there is / 있으면 true
     */
    private boolean canDeleteUnit(Element host, boolean backward) {
        Pos p = caret();
        if (p.node instanceof Text) {
            Text t = (Text) p.node;
            if (backward ? p.offset > 0 : p.offset < t.getLength()) {
                return true;
            }
        }
        Node leaf = backward ? leafAtOrBefore(p, host) : leafAtOrAfter(p, host);
        return leaf != null;
    }

    /**
     * Deletes the selection or one unit next to the caret.
     * <p>
     * 선택이나 캐럿 옆의 한 단위를 지운다.
     *
     * @param host     the editing host / 편집 호스트
     * @param backward true to delete backward / 뒤로 지우면 true
     * @param word     true to delete a word / 낱말이면 true
     */
    private void doDelete(Element host, boolean backward, boolean word) {
        Selection sel = doc.getSelection();
        if (!sel.isCollapsed()) {
            deleteSelectionContents(host);
            return;
        }
        Pos p = caret();
        // 1. 캐럿이 텍스트 안이고 그 방향에 글자가 남아 있으면 글자(낱말) 하나를 지운다.
        if (p.node instanceof Text) {
            Text t = (Text) p.node;
            String d = t.getData();
            if (backward && p.offset > 0) {
                int from = word ? TextUnits.wordBefore(d, p.offset) : TextUnits.previous(d, p.offset);
                afterTextDelete(t, from, p.offset - from, host);
                return;
            }
            if (!backward && p.offset < d.length()) {
                int to = word ? TextUnits.wordAfter(d, p.offset) : TextUnits.next(d, p.offset);
                afterTextDelete(t, p.offset, to - p.offset, host);
                return;
            }
        }
        Node leaf = backward ? leafAtOrBefore(p, host) : leafAtOrAfter(p, host);
        if (leaf == null) {
            return;
        }
        Element block = blockOf(p.node, host);
        // 2. 같은 블록의 이웃 잎: 텍스트이면 끝(처음) 글자를, br 이나 한 단위 요소이면 그것을 지운다.
        if (blockOf(leaf, host) == block) {
            if (leaf instanceof Text) {
                Text t = (Text) leaf;
                String d = t.getData();
                if (backward) {
                    int from = word ? TextUnits.wordBefore(d, d.length()) : TextUnits.previous(d, d.length());
                    afterTextDelete(t, from, d.length() - from, host);
                } else {
                    int to = word ? TextUnits.wordAfter(d, 0) : TextUnits.next(d, 0);
                    afterTextDelete(t, 0, to, host);
                }
            } else {
                Node parent = leaf.getParentNode();
                int idx = indexOf(leaf);
                parent.removeChild(leaf);
                Pos q = cleanup(new Pos(parent, idx), host);
                ensurePlaceholderOf(q, host);
                setCaret(settle(q));
            }
            return;
        }
        // 3. 블록 경계: 이웃 잎이 있는 블록을 이 블록과 합친다.
        Element other = blockOf(leaf, host);
        Pos q;
        if (backward) {
            q = joinBackward(block, other, host);
        } else {
            q = joinForward(block, other, host);
        }
        if (q != null) {
            setCaret(settle(q));
        }
    }

    /**
     * Puts a placeholder into the block of a position if it shows nothing now.
     * <p>
     * 위치의 블록이 지금 아무것도 보이지 않으면 자리표시를 넣는다.
     *
     * @param p    the position / 위치
     * @param host the editing host / 편집 호스트
     */
    private void ensurePlaceholderOf(Pos p, Element host) {
        Element b = blockOf(p.node, host);
        if (b != host) {
            ensurePlaceholder(b);
        }
    }

    /**
     * Deletes characters of a text node and puts the caret at the place of the deletion; an emptied text node is removed.
     * <p>
     * 텍스트 노드의 글자를 지우고 캐럿을 지운 자리에 둔다. 비게 된 텍스트 노드는 제거한다.
     *
     * @param t     the text node / 텍스트 노드
     * @param from  the offset where the deletion starts / 지우기가 시작하는 오프셋
     * @param count the number of characters / 글자 수
     * @param host  the editing host / 편집 호스트
     */
    private void afterTextDelete(Text t, int from, int count, Element host) {
        t.deleteData(from, count);
        Pos p = cleanup(new Pos(t, from), host);
        if (p.node instanceof Text) {
            rebalance((Text) p.node, host);
        }
        ensurePlaceholderOf(p, host);
        setCaret(settle(p));
    }

    /**
     * Joins the block of the caret to the block before it (Backspace at the start of a block). Returns the new caret position, or null if
     * nothing could be joined.
     * <p>
     * 캐럿이 있는 블록을 그 앞의 블록과 합친다(블록의 시작에서 Backspace). 새 캐럿 위치를 반환한다. 합칠 수 없으면 null.
     *
     * @param block the block of the caret / 캐럿의 블록
     * @param prev  the block that holds the leaf before it / 그 앞의 잎을 가진 블록
     * @param host  the editing host / 편집 호스트
     * @return the position or null / 위치. 없으면 null
     */
    private Pos joinBackward(Element block, Element prev, Element host) {
        if (block == host) {
            // 호스트 바로 아래의 인라인 줄: 앞의 블록의 끝으로 줄의 노드들을 옮긴다.
            return prev == host ? null : moveRunInto(prev, caret(), host);
        }
        if (prev == host) {
            // 앞이 호스트 바로 아래의 인라인 줄: 이 블록의 자식을 그 줄 뒤(블록 자리)로 풀어 넣는다.
            return unwrapInto(host, block, startOf(block));
        }
        if (block.contains(prev) || prev.contains(block)) {
            return null;
        }
        return mergeBlocks(prev, block, host, startOf(block));
    }

    /**
     * Joins the block after the caret to the block of the caret (Delete at the end of a block).
     * <p>
     * 캐럿 뒤의 블록을 캐럿의 블록과 합친다(블록의 끝에서 Delete).
     *
     * @param block the block of the caret / 캐럿의 블록
     * @param next  the block that holds the leaf after it / 그 뒤의 잎을 가진 블록
     * @param host  the editing host / 편집 호스트
     * @return the position or null / 위치. 없으면 null
     */
    private Pos joinForward(Element block, Element next, Element host) {
        if (block == host) {
            // 호스트 바로 아래의 인라인 줄 뒤의 블록: 그 블록을 풀어 줄에 잇는다.
            return next == host ? null : unwrapInto(host, next, caret());
        }
        if (next == host) {
            // 뒤가 호스트 바로 아래의 인라인 줄: 그 줄의 노드들을 이 블록의 끝으로 옮긴다.
            return moveRunInto(block, endOf(block), host, true);
        }
        if (block.contains(next) || next.contains(block)) {
            return null;
        }
        return mergeBlocks(block, next, host, endOf(block));
    }

    /**
     * Moves the run of inline nodes that holds a position, directly inside the host, to the end of a block that precedes it.
     * <p>
     * 호스트 바로 아래의 인라인 노드 줄(위치를 가진 줄)을 그 앞의 블록의 끝으로 옮긴다.
     *
     * @param dest the block that receives the run (the block before the run is used) / 줄을 받는 블록(줄 앞의 블록을 쓴다)
     * @param at   the position inside the run / 줄 안의 위치
     * @param host the editing host / 편집 호스트
     * @return the position of the join / 합쳐진 위치
     */
    private Pos moveRunInto(Element dest, Pos at, Element host) {
        return moveRunInto(dest, at, host, false);
    }

    /**
     * Moves the run of inline nodes of the host that follows a block (or holds a position) into a block.
     * <p>
     * 호스트의 인라인 노드 줄(블록 뒤에 오거나 위치를 가진 줄)을 블록 안으로 옮긴다.
     *
     * @param dest  the block that receives the run / 줄을 받는 블록
     * @param at    the position inside the run, or the end of the block when <code>after</code> / 줄 안의 위치. <code>after</code> 이면 블록의 끝
     * @param host  the editing host / 편집 호스트
     * @param after true if the run follows the block / 줄이 블록 뒤에 있으면 true
     * @return the position of the join / 합쳐진 위치
     */
    private Pos moveRunInto(Element dest, Pos at, Element host, boolean after) {
        Node top;
        if (after) {
            top = dest;
            // 호스트의 직계 자식에 닿을 때까지 올라간다.
            while (top.getParentNode() != host && top.getParentNode() != null) {
                top = top.getParentNode();
            }
            top = top.getNextSibling();
        } else {
            top = at.node;
            // 호스트의 직계 자식에 닿을 때까지 올라간다.
            while (top != null && top.getParentNode() != host) {
                top = top.getParentNode();
            }
            // 줄의 맨 앞 노드까지 거슬러 올라간다.
            while (top != null && top.getPreviousSibling() != null && !isBlock(top.getPreviousSibling())) {
                top = top.getPreviousSibling();
            }
        }
        if (top == null) {
            return null;
        }
        Element target = dest;
        // 목적 블록 안의 가장 깊은 마지막 블록으로 들어간다.
        for (Node c = target.getLastChild(); c != null; c = target.getLastChild()) {
            if (isBlock(c)) {
                target = (Element) c;
            } else {
                break;
            }
        }
        stripPlaceholders(target);
        Pos join = hasVisibleContent(target) ? endOf(target) : new Pos(target, 0);
        // 줄의 인라인 노드를 블록이 나올 때까지 목적 블록의 끝으로 옮긴다.
        Node n = top;
        while (n != null && !isBlock(n)) {
            Node next = n.getNextSibling();
            target.appendChild(n);
            n = next;
        }
        ensurePlaceholder(target);
        return join;
    }

    /**
     * Unwraps the children of a block into the host at the place of the block, so that they continue the inline run before it (or after
     * it).
     * <p>
     * 블록의 자식을 블록 자리의 호스트 안으로 풀어 넣어, 그 앞(또는 뒤)의 인라인 줄에 이어지게 한다.
     *
     * @param host  the editing host / 편집 호스트
     * @param block the block to unwrap (a descendant of the host) / 풀 블록(호스트의 후손)
     * @param at    the position that the caret gets when the block is unwrapped / 블록을 풀었을 때 캐럿이 갖는 위치
     * @return the position of the join / 합쳐진 위치
     */
    private Pos unwrapInto(Element host, Element block, Pos at) {
        Node top = block;
        // 호스트의 직계 자식에 닿을 때까지 올라간다.
        while (top.getParentNode() != host && top.getParentNode() != null) {
            top = top.getParentNode();
        }
        stripPlaceholders(block);
        Node prev = top.getPreviousSibling();
        Pos join = prev != null && !isBlock(prev) && hasVisibleContent(prev) ? endOf(prev) : at;
        // 블록의 자식을 블록 앞으로 하나씩 옮긴 뒤 빈 블록을 제거한다.
        Node child = block.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            top.getParentNode().insertBefore(child, top);
            child = next;
        }
        removeWithEmptyAncestors(block, host);
        return join;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 줄과 문단 나누기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Splits the ancestors of a position, from the node of the position up to a limit element, so that the position becomes a boundary
     * between two children of the limit; the part after the position goes into copies of the split elements. Returns the index in the limit
     * element where the part after the position starts. Nothing is copied when the position is at the start or the end of an element.
     * <p>
     * 위치의 조상을 위치의 노드에서 한계 요소까지 쪼개어, 위치가 한계 요소의 두 자식 사이의 경계가 되게 한다. 위치 뒤의 부분은 쪼갠 요소의 복사본에 들어간다.
     * 위치 뒤의 부분이 시작하는 한계 요소 안의 위치를 반환한다. 위치가 요소의 처음이나 끝이면 복사하지 않는다.
     *
     * @param limit the element that is not split / 쪼개지 않는 요소
     * @param p     the position inside the limit element / 한계 요소 안의 위치
     * @return the index in the limit element / 한계 요소 안의 위치
     */
    private int splitUntil(Element limit, Pos p) {
        Node node = p.node;
        int offset = p.offset;
        // 텍스트 안이면 텍스트를 쪼개거나(안쪽이면) 부모 안의 위치로 바꾼다.
        if (node instanceof Text) {
            Text t = (Text) node;
            Node parent = t.getParentNode();
            int idx = indexOf(t);
            if (offset <= 0) {
                node = parent;
                offset = idx;
            } else if (offset >= t.getLength()) {
                node = parent;
                offset = idx + 1;
            } else {
                Text tail = t.splitText(offset);
                node = parent;
                offset = indexOf(tail);
            }
        }
        // 한계 요소에 닿을 때까지 한 단계씩 올라가며 안쪽이면 요소를 복사해 뒷부분을 옮긴다.
        while (node != limit && node.getParentNode() != null) {
            Element cur = (Element) node;
            Node parent = cur.getParentNode();
            int idx = indexOf(cur);
            if (offset <= 0) {
                node = parent;
                offset = idx;
            } else if (offset >= childCount(cur)) {
                node = parent;
                offset = idx + 1;
            } else {
                Element clone = (Element) cur.cloneNode(false);
                if (clone.hasAttribute("id")) {
                    clone.removeAttribute("id");
                }
                Node c = childAt(cur, offset);
                // 위치 뒤의 자식을 모두 복사본으로 옮긴다.
                while (c != null) {
                    Node next = c.getNextSibling();
                    clone.appendChild(c);
                    c = next;
                }
                parent.insertBefore(clone, cur.getNextSibling());
                node = parent;
                offset = indexOf(clone);
            }
        }
        return offset;
    }

    /**
     * Inserts a line break at a position (<code>br</code>, or a line feed where white space is preserved) and returns the position after it.
     * A break at the end of a block gets one more <code>br</code> after it, because a single trailing <code>br</code> shows no new line.
     * <p>
     * 위치에 줄 바꿈(<code>br</code>, 공백이 보존되는 곳에서는 줄바꿈 문자)을 넣고 그 뒤의 위치를 반환한다. 블록 끝의 줄 바꿈은 뒤에 <code>br</code> 을 하나 더
     * 가진다. 끝의 <code>br</code> 하나는 새 줄을 보이지 않기 때문이다.
     *
     * @param p    the position / 위치
     * @param host the editing host / 편집 호스트
     * @return the position after the break / 줄 바꿈 뒤의 위치
     */
    private Pos lineBreakAt(Pos p, Element host) {
        boolean plain = EditingHosts.isPlainTextOnly(host);
        if (env.preservesWhitespace(p.node) || plain) {
            Pos q = insertPlain(p, "\n", host);
            Text t = (Text) q.node;
            // 줄의 끝이면 뒤따르는 줄바꿈이 새 줄을 보이도록 줄바꿈 문자를 하나 더 둔다.
            if (q.offset == t.getLength() && !contentAfter(t, host)) {
                t.appendData("\n");
            }
            return q;
        }
        int idx = splitUntil(blockOrInline(p, host), p);
        Element container = blockOrInline(p, host);
        Element br = doc.createElement("br");
        container.insertBefore(br, childAt(container, idx));
        Pos after = new Pos(container, idx + 1);
        // 블록의 끝(뒤에 보이는 내용이 없음)이면 br 을 하나 더 두어 새 줄이 보이게 한다.
        Node tailLeaf = leafAtOrAfter(after, host);
        if (tailLeaf == null || isNamed(tailLeaf, "br") && !hasVisibleContentAfter(br, host) || blockOf(tailLeaf, host) != blockOf(br, host)) {
            container.insertBefore(doc.createElement("br"), childAt(container, idx + 1));
        }
        return settle(after);
    }

    /**
     * Tells whether a node is followed by content that shows in the same block.
     * <p>
     * 노드 뒤에 같은 블록에서 보이는 내용이 이어지는지 알려 준다.
     *
     * @param n    the node / 노드
     * @param host the editing host / 편집 호스트
     * @return true if content follows / 내용이 뒤따르면 true
     */
    private boolean hasVisibleContentAfter(Node n, Element host) {
        Element block = blockOf(n, host);
        Node leaf = leafAfter(n, host);
        // 뒤의 잎을 따라가며 같은 블록에서 보이는 내용(br 은 제외)을 찾는다.
        while (leaf != null && blockOf(leaf, host) == block) {
            if (!isNamed(leaf, "br") && hasVisibleContent(leaf)) {
                return true;
            }
            leaf = leafAfter(leaf, host);
        }
        return false;
    }

    /**
     * Returns the element that a position's line break goes into: the nearest inline-or-block parent of the position (the position's own
     * element if it is an element, the parent of a text node).
     * <p>
     * 위치의 줄 바꿈이 들어갈 요소를 반환한다: 위치의 요소(텍스트 노드이면 그 부모).
     *
     * @param p    the position / 위치
     * @param host the editing host / 편집 호스트
     * @return the element / 요소
     */
    private Element blockOrInline(Pos p, Element host) {
        Node n = p.node instanceof Text ? p.node.getParentNode() : p.node;
        return n instanceof Element ? (Element) n : host;
    }

    /**
     * Inserts a line break at the selection (<code>insertLineBreak</code>, Shift+Enter).
     * <p>
     * 선택에 줄 바꿈을 넣는다(<code>insertLineBreak</code>, Shift+Enter).
     *
     * @return true if inserted / 넣었으면 true
     */
    public boolean insertLineBreak() {
        return perform("insertLineBreak", null, null, selectionRanges(), null, new Operation() {
            /**
             * Inserts the break.
             * <p>
             * 줄 바꿈을 넣는다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                deleteSelectionIfAny(host);
                setCaret(lineBreakAt(caret(), host));
            }
        });
    }

    /**
     * Breaks the paragraph at the selection (<code>insertParagraph</code>, Enter): the block is split in two (the new block is a copy of the
     * old one, after a heading a <code>div</code>); in an empty list item the list ends; where white space is preserved a line feed is
     * inserted.
     * <p>
     * 선택에서 문단을 나눈다(<code>insertParagraph</code>, Enter): 블록이 둘로 갈라진다(새 블록은 옛 블록의 복사본이고 제목 뒤에서는 <code>div</code>). 빈 목록 항목에서는
     * 목록이 끝나고, 공백이 보존되는 곳에서는 줄바꿈 문자를 넣는다.
     *
     * @return true if done / 했으면 true
     */
    public boolean insertParagraph() {
        return perform("insertParagraph", null, null, selectionRanges(), null, new Operation() {
            /**
             * Breaks the paragraph.
             * <p>
             * 문단을 나눈다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                deleteSelectionIfAny(host);
                doInsertParagraph(host);
            }
        });
    }

    /**
     * Splits the block of the caret.
     * <p>
     * 캐럿의 블록을 나눈다.
     *
     * @param host the editing host / 편집 호스트
     */
    private void doInsertParagraph(Element host) {
        Pos p = caret();
        Element block = blockOf(p.node, host);
        if (EditingHosts.isPlainTextOnly(host) || env.preservesWhitespace(p.node)) {
            setCaret(lineBreakAt(p, host));
            return;
        }
        String name = block.getLocalName();
        // 호스트 바로 아래나 쪼갤 수 없는 블록(표 셀 등) 안: 뒤쪽 내용을 새 기본 단락 요소로 감싼다.
        if (block == host || CONTAINERS.contains(name)) {
            int idx = splitUntil(block, p);
            Element div = doc.createElement(paragraphSeparator);
            Node c = childAt(block, idx);
            // 위치 뒤의 노드를 모두 새 단락으로 옮긴다.
            while (c != null) {
                Node next = c.getNextSibling();
                div.appendChild(c);
                c = next;
            }
            ensurePlaceholder(div);
            // 앞부분이 비어 있으면 그것도 빈 줄로 보이도록 단락으로 감싼다.
            if (!hasVisibleContent(block) && !hasBr(block) && block.getFirstChild() == null) {
                Element head = doc.createElement(paragraphSeparator);
                head.appendChild(doc.createElement("br"));
                block.appendChild(head);
            }
            block.appendChild(div);
            setCaret(startOf(div));
            return;
        }
        // 빈 목록 항목에서 Enter 는 목록을 끝낸다.
        if ("li".equals(name) && !hasVisibleContent(block) && isList(block.getParentNode())) {
            exitList(block, host);
            return;
        }
        int idx = splitUntil(block, p);
        boolean tailEmpty = true;
        // 뒤쪽에 보이는 내용이 있는지 본다.
        for (Node c = childAt(block, idx); c != null; c = c.getNextSibling()) {
            if (hasVisibleContent(c)) {
                tailEmpty = false;
            }
        }
        boolean heading = name.length() == 2 && name.charAt(0) == 'h' && Character.isDigit(name.charAt(1));
        Element fresh;
        // 제목의 끝에서는 일반 단락이 생기고, 그 밖에는 같은 요소의 복사본이 생긴다.
        if (heading && tailEmpty) {
            fresh = doc.createElement(paragraphSeparator);
        } else {
            fresh = (Element) block.cloneNode(false);
            if (fresh.hasAttribute("id")) {
                fresh.removeAttribute("id");
            }
        }
        Node c = childAt(block, idx);
        // 위치 뒤의 노드를 새 블록으로 옮긴다.
        while (c != null) {
            Node next = c.getNextSibling();
            fresh.appendChild(c);
            c = next;
        }
        ensurePlaceholder(block);
        ensurePlaceholder(fresh);
        block.getParentNode().insertBefore(fresh, block.getNextSibling());
        setCaret(startOf(fresh));
    }

    /**
     * Tells whether a node is a list.
     * <p>
     * 노드가 목록인지 알려 준다.
     *
     * @param n the node / 노드
     * @return true for <code>ul</code> and <code>ol</code> / <code>ul</code>, <code>ol</code> 이면 true
     */
    private static boolean isList(Node n) {
        return isNamed(n, "ul") || isNamed(n, "ol");
    }

    /**
     * Ends a list at an empty item: the item is replaced by a new paragraph after the list; the items after it go into a new list after the
     * paragraph.
     * <p>
     * 빈 항목에서 목록을 끝낸다: 항목은 목록 뒤의 새 단락이 되고, 그 뒤의 항목은 단락 뒤의 새 목록에 들어간다.
     *
     * @param item the empty item / 빈 항목
     * @param host the editing host / 편집 호스트
     */
    private void exitList(Element item, Element host) {
        Element list = (Element) item.getParentNode();
        Node parent = list.getParentNode();
        Element div = doc.createElement(isBlock(parent) && parent != host ? "li".equals(((Element) parent).getLocalName()) ? "div" : paragraphSeparator
                : paragraphSeparator);
        div.appendChild(doc.createElement("br"));
        Node after = list.getNextSibling();
        // 항목 뒤의 항목이 있으면 같은 종류의 새 목록으로 옮긴다.
        if (item.getNextSibling() != null) {
            Element rest = (Element) list.cloneNode(false);
            Node c = item.getNextSibling();
            // 항목 뒤의 형제를 새 목록으로 옮긴다.
            while (c != null) {
                Node next = c.getNextSibling();
                rest.appendChild(c);
                c = next;
            }
            parent.insertBefore(rest, after);
            after = rest;
        }
        parent.insertBefore(div, after);
        list.removeChild(item);
        if (list.getFirstChild() == null) {
            parent.removeChild(list);
        }
        setCaret(new Pos(div, 0));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 서식
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the names of the elements that carry a format command, or null if it is not a format command.
     * <p>
     * 서식 명령을 나타내는 요소의 이름들을 반환한다. 서식 명령이 아니면 null.
     *
     * @param command the command name in lower case / 소문자 명령 이름
     * @return the element names or null / 요소 이름. 없으면 null
     */
    private static String[] formatNames(String command) {
        return FORMATS.get(command);
    }

    /**
     * Tells whether the text is inside an element of a format (between it and the host).
     * <p>
     * 텍스트가 서식 요소 안(그것과 호스트 사이)에 있는지 알려 준다.
     *
     * @param t     the node / 노드
     * @param names the element names / 요소 이름
     * @param host  the editing host / 편집 호스트
     * @return the format element, or null / 서식 요소. 없으면 null
     */
    private static Element formatAncestor(Node t, String[] names, Element host) {
        // 위로 올라가며 서식 요소를 찾는다.
        for (Node c = t.getParentNode(); c != null && c != host; c = c.getParentNode()) {
            if (c instanceof Element) {
                String n = ((Element) c).getLocalName();
                // 서식 요소 이름 목록에서 같은 이름을 찾는다.
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(n)) {
                        return (Element) c;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Toggles an inline format of the selection (<code>formatBold</code>, <code>formatItalic</code> ...): a selection that is all inside the
     * format loses it, otherwise the format is applied to all the text. With a collapsed caret the format is remembered and applies to the
     * text typed next.
     * <p>
     * 선택의 인라인 서식을 켜고 끈다(<code>formatBold</code>, <code>formatItalic</code> 등): 선택이 모두 서식 안이면 서식이 없어지고, 아니면 모든 텍스트에
     * 서식이 적용된다. 접힌 캐럿에서는 서식을 기억해 두었다가 다음에 입력하는 텍스트에 적용한다.
     *
     * @param command the command: <code>bold</code>, <code>italic</code>, <code>underline</code>, <code>strikeThrough</code>,
     *                <code>subscript</code>, <code>superscript</code> / 명령
     * @return true if done / 했으면 true
     */
    public boolean toggleFormat(String command) {
        final String key = command == null ? "" : command.toLowerCase(java.util.Locale.ENGLISH);
        final String[] names = formatNames(key);
        Element host = getSelectionHost();
        if (names == null || host == null) {
            return false;
        }
        Selection sel = doc.getSelection();
        // 접힌 캐럿이면 다음 입력에 적용할 서식으로 기억하고 끝낸다(문서는 바뀌지 않는다).
        if (sel.isCollapsed()) {
            boolean now = queryFormat(key, host);
            Boolean pendingNow = pendingNode == sel.getFocusNode() && pendingOffset == sel.getFocusOffset() ? pending.get(key) : null;
            if (pendingNode != sel.getFocusNode() || pendingOffset != sel.getFocusOffset()) {
                pending.clear();
            }
            pendingNode = sel.getFocusNode();
            pendingOffset = sel.getFocusOffset();
            pending.put(key, Boolean.valueOf(pendingNow != null ? !pendingNow.booleanValue() : !now));
            return true;
        }
        return perform(FORMAT_TYPES.get(key), null, null, selectionRanges(), null, new Operation() {
            /**
             * Formats the text of the selection.
             * <p>
             * 선택의 텍스트에 서식을 적용한다.
             *
             * @param h the editing host / 편집 호스트
             */
            public void run(Element h) {
                formatSelection(key, names, h);
            }
        });
    }

    /**
     * Tells whether a format applies to the selection: all its text (or the text at the caret) is inside the format.
     * <p>
     * 서식이 선택에 적용되는지 알려 준다: 선택의 모든 텍스트(또는 캐럿 위치의 텍스트)가 서식 안에 있다.
     *
     * @param command the command name in lower case / 소문자 명령 이름
     * @param host    the editing host / 편집 호스트
     * @return true if the format applies / 서식이 적용되면 true
     */
    private boolean queryFormat(String command, Element host) {
        String[] names = formatNames(command);
        Selection sel = doc.getSelection();
        if (names == null || sel.getRangeCount() == 0) {
            return false;
        }
        Range r = sel.getRangeAt(0);
        if (r.isCollapsed()) {
            Node n = r.getStartContainer();
            Boolean p = pendingNode == n && pendingOffset == r.getStartOffset() ? pending.get(command) : null;
            if (p != null) {
                return p.booleanValue();
            }
            // 캐럿의 노드(텍스트이면 그 텍스트, 요소이면 그 요소 자신)가 서식 안에 있는지 본다.
            return formatAncestor(n, names, host) != null || (n instanceof Element && isFormatElement((Element) n, names));
        }
        List<Text> texts = textsOf(r);
        if (texts.isEmpty()) {
            return false;
        }
        // 선택의 텍스트가 모두 서식 안에 있어야 참이다.
        for (int i = 0; i < texts.size(); i++) {
            if (formatAncestor(texts.get(i), names, host) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether an element is one of the format elements.
     * <p>
     * 요소가 서식 요소 중 하나인지 알려 준다.
     *
     * @param e     the element / 요소
     * @param names the names / 이름
     * @return true if it is / 맞으면 true
     */
    private static boolean isFormatElement(Element e, String[] names) {
        // 서식 요소 이름 목록에서 같은 이름을 찾는다.
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(e.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the non-empty text nodes that a range covers (the parts at its ends are included whole after the ends were split by
     * {@link #splitEnds}).
     * <p>
     * 범위가 덮는 비어 있지 않은 텍스트 노드를 반환한다(양 끝의 부분은 {@link #splitEnds} 로 쪼갠 뒤 통째로 들어간다).
     *
     * @param r the range / 범위
     * @return the text nodes in tree order / 트리 순서의 텍스트 노드
     */
    private List<Text> textsOf(Range r) {
        List<Text> out = new ArrayList<Text>();
        Node root = r.getCommonAncestorContainer();
        // 공통 조상 아래를 트리 순서로 훑으며 범위에 닿는 비어 있지 않은 텍스트를 모은다.
        for (Node n = root; n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof Text && ((Text) n).getLength() > 0 && r.intersectsNode(n)) {
                Text t = (Text) n;
                int from = n == r.getStartContainer() ? r.getStartOffset() : 0;
                int to = n == r.getEndContainer() ? r.getEndOffset() : t.getLength();
                if (from < to) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    /**
     * Splits the text nodes at the two ends of the selection range so that the range covers whole text nodes only. The range stays on the
     * same content (live ranges follow a split).
     * <p>
     * 선택 범위의 양 끝의 텍스트 노드를 쪼개 범위가 텍스트 노드 전체만 덮게 한다. 범위는 같은 내용에 머문다(살아 있는 범위는 쪼갬을 따라간다).
     *
     * @param r the range / 범위
     */
    private void splitEnds(Range r) {
        Node end = r.getEndContainer();
        int endOffset = r.getEndOffset();
        // 끝이 텍스트 안쪽이면 끝에서 쪼개 앞부분만 범위에 남긴다.
        if (end instanceof Text && endOffset > 0 && endOffset < ((Text) end).getLength()) {
            ((Text) end).splitText(endOffset);
        }
        Node start = r.getStartContainer();
        int startOffset = r.getStartOffset();
        // 시작이 텍스트 안쪽이면 시작에서 쪼개 뒷부분만 범위에 남긴다.
        if (start instanceof Text && startOffset > 0 && startOffset < ((Text) start).getLength()) {
            Text tail = ((Text) start).splitText(startOffset);
            r.setStart(tail, 0);
        }
    }

    /**
     * Applies or removes a format on the text of the selection range.
     * <p>
     * 선택 범위의 텍스트에 서식을 적용하거나 없앤다.
     *
     * @param key   the command name / 명령 이름
     * @param names the element names of the format / 서식의 요소 이름
     * @param host  the editing host / 편집 호스트
     */
    private void formatSelection(String key, String[] names, Element host) {
        Selection sel = doc.getSelection();
        Range r = sel.getRangeAt(0);
        splitEnds(r);
        List<Text> texts = textsOf(r);
        if (texts.isEmpty()) {
            return;
        }
        boolean all = true;
        // 모든 텍스트가 이미 서식 안이면 서식을 없애고, 아니면 모두에 적용한다.
        for (int i = 0; i < texts.size() && all; i++) {
            all = formatAncestor(texts.get(i), names, host) != null;
        }
        Text first = texts.get(0);
        Text last = texts.get(texts.size() - 1);
        // 선택 범위의 텍스트 노드를 차례로 처리한다.
        for (int i = 0; i < texts.size(); i++) {
            Text t = texts.get(i);
            if (all) {
                removeFormat(t, names, host);
            } else if (formatAncestor(t, names, host) == null) {
                wrapFormat(t, names[0]);
            }
        }
        // 서식을 적용한 텍스트마다 이웃한 같은 서식 요소와 합친다.
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).isConnected()) {
                mergeAdjacent(texts.get(i), names, host);
            }
        }
        sel.setBaseAndExtent(first, 0, last, last.getLength());
    }

    /**
     * Wraps a text node in a format element.
     * <p>
     * 텍스트 노드를 서식 요소로 감싼다.
     *
     * @param t   the text node / 텍스트 노드
     * @param tag the element name / 요소 이름
     */
    private void wrapFormat(Text t, String tag) {
        Element w = doc.createElement(tag);
        t.getParentNode().insertBefore(w, t);
        w.appendChild(t);
    }

    /**
     * Takes a text node out of the format elements around it: the elements are split around the text and the copy that holds only the text
     * is unwrapped.
     * <p>
     * 텍스트 노드를 둘러싼 서식 요소에서 꺼낸다: 요소를 텍스트 둘레에서 쪼개고 텍스트만 가진 복사본을 푼다.
     *
     * @param t     the text node / 텍스트 노드
     * @param names the element names of the format / 서식의 요소 이름
     * @param host  the editing host / 편집 호스트
     */
    private void removeFormat(Text t, String[] names, Element host) {
        Element f = formatAncestor(t, names, host);
        // 서식 요소가 여러 겹이어도 하나씩 풀어 모두 없앤다.
        while (f != null) {
            Element parent = (Element) f.getParentNode();
            splitUntil(parent, new Pos(t.getParentNode(), indexOf(t) + 1));
            splitUntil(parent, new Pos(t.getParentNode(), indexOf(t)));
            Node holder = t;
            // 텍스트를 품은, 부모 바로 아래의 요소를 찾는다.
            while (holder.getParentNode() != parent) {
                holder = holder.getParentNode();
            }
            Element unit = (Element) holder;
            Node c = unit.getFirstChild();
            boolean isFormat = isFormatElement(unit, names);
            // 서식 요소 자신이면 그 자식을 부모로 풀어 놓고 그 요소를 제거한다.
            if (isFormat) {
                while (c != null) {
                    Node next = c.getNextSibling();
                    parent.insertBefore(c, unit);
                    c = next;
                }
                parent.removeChild(unit);
            } else {
                // 서식 요소 안쪽의 다른 인라인 요소이면 그 안에서 한 겹 더 찾는다.
                Element inner = formatAncestor(t, names, parent);
                if (inner == null) {
                    break;
                }
            }
            f = formatAncestor(t, names, host);
        }
    }

    /**
     * Merges the format element that holds a text node with the same kind of element next to it, so that <code>&lt;b&gt;a&lt;/b&gt;&lt;b&gt;b&lt;/b&gt;</code>
     * becomes one element.
     * <p>
     * 텍스트 노드를 품은 서식 요소를 이웃한 같은 종류의 요소와 합쳐, <code>&lt;b&gt;a&lt;/b&gt;&lt;b&gt;b&lt;/b&gt;</code> 가 요소 하나가 되게 한다.
     *
     * @param t     the text node / 텍스트 노드
     * @param names the element names of the format / 서식의 요소 이름
     * @param host  the editing host / 편집 호스트
     */
    private void mergeAdjacent(Text t, String[] names, Element host) {
        Element f = formatAncestor(t, names, host);
        if (f == null) {
            return;
        }
        // 앞뒤 이웃이 같은 이름의 속성 없는 서식 요소이면 내용을 한쪽으로 합친다.
        Node prev = f.getPreviousSibling();
        if (prev instanceof Element && sameFormat((Element) prev, f)) {
            Node c = f.getFirstChild();
            // 서식 요소의 자식을 앞 형제 서식 요소로 옮겨 합친다.
            while (c != null) {
                Node next = c.getNextSibling();
                prev.appendChild(c);
                c = next;
            }
            f.getParentNode().removeChild(f);
            f = (Element) prev;
        }
        Node next = f.getNextSibling();
        if (next instanceof Element && sameFormat((Element) next, f)) {
            Node c = next.getFirstChild();
            // 다음 형제 서식 요소의 자식을 이 요소로 옮겨 합친다.
            while (c != null) {
                Node n2 = c.getNextSibling();
                f.appendChild(c);
                c = n2;
            }
            f.getParentNode().removeChild(next);
        }
    }

    /**
     * Tells whether two elements have the same name and neither has attributes.
     * <p>
     * 두 요소가 이름이 같고 둘 다 속성이 없는지 알려 준다.
     *
     * @param a the first element / 첫 요소
     * @param b the second element / 둘째 요소
     * @return true if they can be merged / 합칠 수 있으면 true
     */
    private static boolean sameFormat(Element a, Element b) {
        return a.getLocalName().equals(b.getLocalName()) && !a.hasAttributes() && !b.hasAttributes();
    }

    /**
     * Applies the formats that were switched at a collapsed caret to the text that was just typed (the part of a text node).
     * <p>
     * 접힌 캐럿에서 바꾼 서식을 방금 입력한 텍스트(텍스트 노드의 한 부분)에 적용한다.
     *
     * @param t    the text node that holds the typed text / 입력한 텍스트를 가진 텍스트 노드
     * @param from the start of the typed text / 입력한 텍스트의 시작
     * @param to   the end of the typed text / 입력한 텍스트의 끝
     * @param host the editing host / 편집 호스트
     */
    private void applyPending(Text t, int from, int to, Element host) {
        Map<String, Boolean> todo = new LinkedHashMap<String, Boolean>(pending);
        pending.clear();
        // 입력한 부분만 따로 떼어 서식을 적용한다.
        Text mid = t;
        if (to < t.getLength()) {
            t.splitText(to);
        }
        if (from > 0) {
            mid = t.splitText(from);
        }
        // 보류 중인 서식을 하나씩 적용한다.
        for (Map.Entry<String, Boolean> e : todo.entrySet()) {
            String[] names = formatNames(e.getKey());
            if (names == null) {
                continue;
            }
            boolean has = formatAncestor(mid, names, host) != null;
            if (e.getValue().booleanValue() && !has) {
                wrapFormat(mid, names[0]);
            } else if (!e.getValue().booleanValue() && has) {
                removeFormat(mid, names, host);
            }
        }
        setCaret(new Pos(mid, mid.getLength()));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 입력기의 조합 입력
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether an input method is composing text now (between <code>compositionstart</code> and <code>compositionend</code>).
     * <p>
     * 입력기가 지금 텍스트를 조합하고 있는지(<code>compositionstart</code> 와 <code>compositionend</code> 사이) 알려 준다.
     *
     * @return true while composing / 조합 중이면 true
     */
    public boolean isComposing() {
        return composition != null;
    }

    /**
     * Applies what an input method reports (Java: an <code>InputMethodEvent</code>): the text that it has committed and the text that it is
     * still composing. The composed text is shown in the DOM at the caret as it is composed (each change is an <code>insertCompositionText</code>
     * with <code>compositionupdate</code> and <code>isComposing</code>); the committed text replaces it and ends the composition
     * (<code>compositionend</code>); the text that is still composed after the committed part starts the next composition (as when Hangul
     * letters finish a syllable and start the next). Both empty ends a running composition without text (the user cancelled it). A
     * composition is one step of the undo history.
     * <p>
     * 입력기가 알리는 것(Java: <code>InputMethodEvent</code>)을 적용한다: 확정한 텍스트와 아직 조합 중인 텍스트. 조합 중인 텍스트는 조합되는 대로 캐럿 자리의 DOM 에
     * 보인다(변경마다 <code>compositionupdate</code> 와 <code>isComposing</code> 이 있는 <code>insertCompositionText</code>). 확정된 텍스트는 그것을 바꾸고 조합을
     * 끝낸다(<code>compositionend</code>). 확정된 부분 뒤에 아직 조합 중인 텍스트는 다음 조합을 시작한다(한글이 한 음절을 마치고 다음 음절을 시작할 때처럼). 둘 다
     * 비면 진행 중인 조합을 텍스트 없이 끝낸다(사용자가 취소했다). 조합 하나는 되돌리기 이력의 한 단계이다.
     *
     * @param committed the text that the input method committed / 입력기가 확정한 텍스트
     * @param composing the text that is still composed / 아직 조합 중인 텍스트
     * @return true if the DOM was changed or a composition ended / DOM 이 바뀌었거나 조합이 끝났으면 true
     */
    public boolean compose(String committed, String composing) {
        String done = committed == null ? "" : committed;
        String going = composing == null ? "" : composing;
        // 선택이 편집 호스트 밖으로 나가 조합 자리를 잃었으면 진행 중인 조합은 잊는다.
        if (composition != null && getSelectionHost() == null) {
            composition = null;
            compositionFresh = false;
        }
        if (composition == null && (done.length() + going.length() == 0 || !isEditing())) {
            return false;
        }
        boolean changed;
        if (done.length() > 0) {
            // 확정된 텍스트가 조합을 대신하고 조합이 끝난다. 아직 조합 중인 텍스트가 있으면 새 조합이 이어진다.
            changed = replaceComposition(done);
            endComposition(done);
            if (going.length() > 0) {
                changed |= replaceComposition(going);
            }
        } else if (going.length() > 0) {
            changed = replaceComposition(going);
        } else {
            // 조합 중인 텍스트가 없으면 조합이 취소된 것이다.
            changed = replaceComposition("");
            endComposition("");
        }
        return changed;
    }

    /**
     * Forgets a running composition without any event (the page was replaced or the component was disposed).
     * <p>
     * 진행 중인 조합을 이벤트 없이 잊는다(페이지가 교체되었거나 컴포넌트가 폐기되었다).
     */
    public void abandonComposition() {
        composition = null;
        compositionFresh = false;
    }

    /**
     * Replaces the composed text with a new text: starts the composition (<code>compositionstart</code>) when none runs, then sends
     * <code>beforeinput</code>, <code>compositionupdate</code>, changes the DOM and sends <code>input</code>.
     * <p>
     * 조합 중인 텍스트를 새 텍스트로 바꾼다: 진행 중인 조합이 없으면 조합을 시작하고(<code>compositionstart</code>), <code>beforeinput</code>,
     * <code>compositionupdate</code> 를 보낸 뒤 DOM 을 바꾸고 <code>input</code> 을 보낸다.
     *
     * @param text the new text of the composition / 조합의 새 텍스트
     * @return true if the DOM was changed / DOM 이 바뀌었으면 true
     */
    private boolean replaceComposition(final String text) {
        Element host = getSelectionHost();
        boolean starting = composition == null;
        if (starting) {
            if (host == null) {
                return false;
            }
            undo.breakMerging();
            CompositionEvent start = new CompositionEvent("compositionstart", true, false);
            start.setData("");
            env.dispatch(host, start);
            // 리스너가 선택을 옮겼을 수 있으므로 다시 확인한다.
            if (getSelectionHost() == null) {
                return false;
            }
            composition = doc.createRange();
            compositionFresh = true;
        }
        List<StaticRange> ranges = compositionFresh ? selectionRanges() : new ArrayList<StaticRange>();
        // 이미 시작한 조합이면 현재 조합 텍스트가 대상 범위이다.
        if (!compositionFresh) {
            ranges.add(new StaticRange(composition.getStartContainer(), composition.getStartOffset(), composition.getEndContainer(),
                    composition.getEndOffset()));
        }
        boolean ran = perform("insertCompositionText", text, null, ranges, "composition", new Operation() {
            /**
             * Shows the new composed text.
             * <p>
             * 새 조합 텍스트를 보인다.
             *
             * @param h the editing host / 편집 호스트
             */
            public void run(Element h) {
                CompositionEvent update = new CompositionEvent("compositionupdate", true, false);
                update.setData(text);
                env.dispatch(h, update);
                doReplaceComposition(h, text);
            }
        });
        if (!ran && starting) {
            composition = null;
            compositionFresh = false;
        }
        return ran;
    }

    /**
     * Puts the composed text into the DOM at the place of the composition (the first time: at the caret, in place of the selection), and
     * leaves the caret at the end of the composed text.
     * <p>
     * 조합 텍스트를 조합 자리의 DOM 에 넣고(처음에는 캐럿 자리에, 선택을 대신해서) 캐럿을 조합 텍스트의 끝에 둔다.
     *
     * @param host the editing host / 편집 호스트
     * @param text the composed text / 조합 텍스트
     */
    private void doReplaceComposition(Element host, String text) {
        Pos at;
        if (compositionFresh) {
            deleteSelectionIfAny(host);
            at = caret();
            compositionFresh = false;
        } else {
            // 이전 조합 텍스트를 지우고 그 자리에 새 텍스트를 넣는다.
            composition.deleteContents();
            at = new Pos(composition.getStartContainer(), composition.getStartOffset());
        }
        if (text.length() == 0) {
            // 취소: 비게 된 텍스트 노드는 정리하고 블록이 비었으면 자리표시를 둔다.
            Pos p = cleanup(at, host);
            composition.setStart(p.node, p.offset);
            composition.setEnd(p.node, p.offset);
            Element block = blockOf(p.node, host);
            if (block != host) {
                ensurePlaceholder(block);
            }
            setCaret(settle(p));
            return;
        }
        Pos end = insertPlain(at, text, host);
        composition.setStart(end.node, end.offset - text.length());
        composition.setEnd(end.node, end.offset);
        setCaret(end);
        pending.clear();
    }

    /**
     * Ends the composition: sends <code>compositionend</code> with the final text and closes the undo step of the composition.
     * <p>
     * 조합을 끝낸다: 최종 텍스트로 <code>compositionend</code> 를 보내고 조합의 되돌리기 단계를 닫는다.
     *
     * @param data the final text (empty for a cancelled composition) / 최종 텍스트(취소된 조합이면 빈 문자열)
     */
    private void endComposition(String data) {
        Element host = getSelectionHost();
        composition = null;
        compositionFresh = false;
        undo.breakMerging();
        if (host != null) {
            CompositionEvent end = new CompositionEvent("compositionend", true, false);
            end.setData(data);
            env.dispatch(host, end);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 붙여넣기, 놓기, HTML 삽입
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Pastes data into the selection (<code>insertFromPaste</code>): HTML is inserted as sanitized markup (unless the host is
     * plaintext-only), otherwise the plain text; the selection is replaced.
     * <p>
     * 데이터를 선택에 붙여넣는다(<code>insertFromPaste</code>): HTML 은 정리한 마크업으로 넣고(호스트가 plaintext-only 이면 제외) 아니면 일반 텍스트를 넣으며,
     * 선택은 바뀐다.
     *
     * @param data the data of the clipboard / 클립보드의 데이터
     * @return true if inserted / 넣었으면 true
     */
    public boolean insertFromPaste(ClipboardData data) {
        return insertTransfer(data, "insertFromPaste");
    }

    /**
     * Inserts dropped data at the selection (<code>insertFromDrop</code>); see {@link #insertFromPaste}.
     * <p>
     * 놓은 데이터를 선택에 넣는다(<code>insertFromDrop</code>). {@link #insertFromPaste} 참조.
     *
     * @param data the dragged data / 끌어 온 데이터
     * @return true if inserted / 넣었으면 true
     */
    public boolean insertFromDrop(ClipboardData data) {
        return insertTransfer(data, "insertFromDrop");
    }

    /**
     * Inserts the data of a transfer.
     * <p>
     * 데이터 전달 객체의 데이터를 넣는다.
     *
     * @param data      the data / 데이터
     * @param inputType <code>insertFromPaste</code> or <code>insertFromDrop</code> / 입력 종류
     * @return true if inserted / 넣었으면 true
     */
    private boolean insertTransfer(final ClipboardData data, String inputType) {
        if (data == null || !isEditing()) {
            return false;
        }
        final String html = data.getData("text/html");
        final String plain = data.getData("text/plain");
        if (html.length() == 0 && plain.length() == 0) {
            return false;
        }
        return perform(inputType, null, data, selectionRanges(), null, new Operation() {
            /**
             * Inserts the data.
             * <p>
             * 데이터를 넣는다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                deleteSelectionIfAny(host);
                if (html.length() > 0 && !EditingHosts.isPlainTextOnly(host)) {
                    insertFragmentAt(host, sanitized(html, host));
                } else {
                    doInsertText(host, plain.length() > 0 ? plain : html);
                }
            }
        });
    }

    /**
     * Inserts HTML at the selection (<code>execCommand("insertHTML")</code>; <code>insertFromPaste</code> for pasted markup). The markup is
     * parsed in the context of the editing host.
     * <p>
     * 선택에 HTML 을 넣는다(<code>execCommand("insertHTML")</code>). 마크업은 편집 호스트를 문맥으로 해석한다.
     *
     * @param html the markup / 마크업
     * @return true if inserted / 넣었으면 true
     */
    public boolean insertHtml(final String html) {
        if (html == null || !isEditing() || EditingHosts.isPlainTextOnly(getSelectionHost())) {
            return false;
        }
        return perform("insertText", html, null, selectionRanges(), null, new Operation() {
            /**
             * Inserts the markup.
             * <p>
             * 마크업을 넣는다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                deleteSelectionIfAny(host);
                insertFragmentAt(host, parseFragment(html, host));
            }
        });
    }

    /**
     * Parses markup in the context of the editing host.
     * <p>
     * 마크업을 편집 호스트 문맥으로 해석한다.
     *
     * @param html the markup / 마크업
     * @param host the editing host / 편집 호스트
     * @return the fragment / 조각
     */
    private DocumentFragment parseFragment(String html, Element host) {
        Range r = doc.createRange();
        r.selectNodeContents(host);
        return r.createContextualFragment(html);
    }

    /**
     * Parses pasted markup and removes what a paste must not bring in: scripts, styles, frames, objects, comments, event handler
     * attributes and <code>javascript:</code> addresses.
     * <p>
     * 붙여넣은 마크업을 해석하고 붙여넣기가 가져오면 안 되는 것(스크립트, 스타일, 프레임, 객체, 주석, 이벤트 핸들러 속성, <code>javascript:</code> 주소)을 제거한다.
     *
     * @param html the markup / 마크업
     * @param host the editing host / 편집 호스트
     * @return the cleaned fragment / 정리한 조각
     */
    private DocumentFragment sanitized(String html, Element host) {
        DocumentFragment f = parseFragment(html, host);
        sanitize(f);
        return f;
    }

    /**
     * Removes the unsafe parts of a subtree.
     * <p>
     * 하위 트리의 안전하지 않은 부분을 제거한다.
     *
     * @param parent the root of the subtree / 하위 트리의 루트
     */
    private static void sanitize(Node parent) {
        Node c = parent.getFirstChild();
        // 자식을 하나씩 보며 위험한 노드는 제거하고 요소는 속성을 정리한 뒤 안으로 들어간다.
        while (c != null) {
            Node next = c.getNextSibling();
            if (c.getNodeType() == Node.COMMENT_NODE || c.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                parent.removeChild(c);
            } else if (c instanceof Element) {
                Element e = (Element) c;
                String name = e.getLocalName();
                if ("script".equals(name) || "style".equals(name) || "iframe".equals(name) || "object".equals(name) || "embed".equals(name)
                        || "meta".equals(name) || "link".equals(name) || "base".equals(name) || "frame".equals(name) || "frameset".equals(name)
                        || "template".equals(name) || "noscript".equals(name) || "title".equals(name) || "head".equals(name)) {
                    parent.removeChild(c);
                } else {
                    cleanAttributes(e);
                    sanitize(e);
                }
            }
            c = next;
        }
    }

    /**
     * Removes the event handler attributes and the scripting addresses of an element.
     * <p>
     * 요소의 이벤트 핸들러 속성과 스크립트 주소를 제거한다.
     *
     * @param e the element / 요소
     */
    private static void cleanAttributes(Element e) {
        List<String> remove = new ArrayList<String>();
        List<String> names = e.getAttributeNames();
        // 속성을 하나씩 보며 on 으로 시작하는 것과 javascript: 로 시작하는 주소를 모은다.
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i).toLowerCase(java.util.Locale.ENGLISH);
            String v = e.getAttribute(names.get(i));
            String lower = v == null ? "" : v.trim().toLowerCase(java.util.Locale.ENGLISH).replace("\t", "").replace("\n", "").replace("\r", "");
            if (n.startsWith("on") || (("href".equals(n) || "src".equals(n) || "action".equals(n) || "formaction".equals(n) || "xlink:href".equals(n))
                    && (lower.startsWith("javascript:") || lower.startsWith("vbscript:") || lower.startsWith("data:text/html")))) {
                remove.add(names.get(i));
            }
        }
        // 모은 속성을 제거한다.
        for (int i = 0; i < remove.size(); i++) {
            e.removeAttribute(remove.get(i));
        }
    }

    /**
     * Inserts a fragment at the caret and puts the caret after it. An inline fragment goes into the text at the caret; a fragment that holds
     * blocks splits the block of the caret around it.
     * <p>
     * 조각을 캐럿에 넣고 캐럿을 그 뒤에 둔다. 인라인 조각은 캐럿의 텍스트 안에 들어가고, 블록을 가진 조각은 캐럿의 블록을 그 둘레에서 쪼갠다.
     *
     * @param host     the editing host / 편집 호스트
     * @param fragment the fragment / 조각
     */
    private void insertFragmentAt(Element host, DocumentFragment fragment) {
        if (fragment.getFirstChild() == null) {
            return;
        }
        Pos p = caret();
        boolean blocks = false;
        // 조각 안에 블록이 있는지 본다.
        for (Node c = fragment.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (isBlock(c)) {
                blocks = true;
            }
        }
        Node last = fragment.getLastChild();
        Element container = blockOrInline(p, host);
        // 블록을 가진 조각은 캐럿의 블록을 둘레에서 쪼개고 그 사이에 넣는다.
        if (blocks && container != host && !CONTAINERS.contains(container.getLocalName()) && isBlock(container)) {
            int idx = splitUntil(container, p);
            container.insertBefore(fragment, childAt(container, idx));
        } else {
            int idx = splitUntil(container, p);
            container.insertBefore(fragment, childAt(container, idx));
        }
        Pos end;
        if (last == null) {
            end = p;
        } else {
            end = last instanceof Text ? new Pos(last, ((Text) last).getLength()) : endOf(last);
            if (!(end.node instanceof Text) && last.getParentNode() != null) {
                end = new Pos(last.getParentNode(), indexOf(last) + 1);
            }
        }
        setCaret(settle(end));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 잘라내기와 선택 지우기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Deletes the selection as the second half of a cut (<code>deleteByCut</code>).
     * <p>
     * 선택을 잘라내기의 후반으로 지운다(<code>deleteByCut</code>).
     *
     * @return true if deleted / 지웠으면 true
     */
    public boolean deleteByCut() {
        return deleteSelectionAs("deleteByCut");
    }

    /**
     * Deletes the selection because it was dragged away (<code>deleteByDrag</code>).
     * <p>
     * 선택이 끌려 나가 지운다(<code>deleteByDrag</code>).
     *
     * @return true if deleted / 지웠으면 true
     */
    public boolean deleteByDrag() {
        return deleteSelectionAs("deleteByDrag");
    }

    /**
     * Deletes the content of the selection with an input type.
     * <p>
     * 입력 종류를 정해 선택의 내용을 지운다.
     *
     * @param inputType the input type / 입력 종류
     * @return true if deleted / 지웠으면 true
     */
    private boolean deleteSelectionAs(String inputType) {
        Selection sel = doc.getSelection();
        if (sel.getRangeCount() == 0 || sel.isCollapsed()) {
            return false;
        }
        return perform(inputType, null, null, selectionRanges(), null, new Operation() {
            /**
             * Deletes the selection.
             * <p>
             * 선택을 지운다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                deleteSelectionContents(host);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 되돌리기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Undoes the last editing step (<code>historyUndo</code>).
     * <p>
     * 마지막 편집 단계를 되돌린다(<code>historyUndo</code>).
     *
     * @return true if a step was undone / 되돌렸으면 true
     */
    public boolean undo() {
        return history("historyUndo", true);
    }

    /**
     * Redoes the step that was undone last (<code>historyRedo</code>).
     * <p>
     * 마지막으로 되돌린 단계를 다시 한다(<code>historyRedo</code>).
     *
     * @return true if a step was redone / 다시 했으면 true
     */
    public boolean redo() {
        return history("historyRedo", false);
    }

    /**
     * Sends the events of an undo or a redo around the replay of a step.
     * <p>
     * 단계를 되돌리거나 다시 하는 일의 둘레에 이벤트를 보낸다.
     *
     * @param inputType <code>historyUndo</code> or <code>historyRedo</code> / 입력 종류
     * @param undoing   true for undo / 되돌리기이면 true
     * @return true if a step was replayed / 단계를 되돌렸으면 true
     */
    private boolean history(String inputType, boolean undoing) {
        Element host = getSelectionHost();
        if (host == null && doc.getSelection().getRangeCount() > 0) {
            return false;
        }
        if (undoing ? !undo.canUndo() : !undo.canRedo()) {
            return false;
        }
        if (host != null) {
            InputEvent before = new InputEvent("beforeinput", true, true);
            before.setInput(null, inputType, false);
            if (!env.dispatch(host, before)) {
                return false;
            }
        }
        boolean done = undoing ? undo.undo() : undo.redo();
        pending.clear();
        Element after = getSelectionHost();
        if (done && after != null) {
            InputEvent input = new InputEvent("input", true, false);
            input.setInput(null, inputType, false);
            env.dispatch(after, input);
        }
        return done;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // execCommand
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether a command name is one that {@link #execCommand} implements.
     * <p>
     * 명령 이름이 {@link #execCommand} 가 구현하는 것인지 알려 준다.
     *
     * @param name the command name, in any letter case / 명령 이름(대소문자 무관)
     * @return true if supported / 지원하면 true
     */
    public boolean isCommandSupported(String name) {
        String c = lower(name);
        return FORMATS.containsKey(c) || Arrays.asList("inserttext", "inserthtml", "insertparagraph", "insertlinebreak", "delete", "forwarddelete",
                "selectall", "undo", "redo", "removeformat", "createlink", "unlink", "formatblock", "insertorderedlist", "insertunorderedlist",
                "inserthorizontalrule", "insertimage", "defaultparagraphseparator", "stylewithcss", "usecss").contains(c);
    }

    /**
     * Tells whether a command can run now (the selection is in an editing host, and for undo and redo there is a step).
     * <p>
     * 명령이 지금 실행될 수 있는지 알려 준다(선택이 편집 호스트 안이고, 되돌리기와 다시 하기는 단계가 있다).
     *
     * @param name the command name / 명령 이름
     * @return true if enabled / 실행할 수 있으면 true
     */
    public boolean isCommandEnabled(String name) {
        String c = lower(name);
        if (!isCommandSupported(c)) {
            return false;
        }
        if ("selectall".equals(c) || "defaultparagraphseparator".equals(c) || "stylewithcss".equals(c) || "usecss".equals(c)) {
            return true;
        }
        if ("undo".equals(c)) {
            return undo.canUndo();
        }
        if ("redo".equals(c)) {
            return undo.canRedo();
        }
        return isEditing();
    }

    /**
     * Returns the state of a command for the selection (<code>queryCommandState</code>): whether a format applies.
     * <p>
     * 선택에 대한 명령의 상태를 반환한다(<code>queryCommandState</code>): 서식이 적용되는지.
     *
     * @param name the command name / 명령 이름
     * @return true if the command is in effect / 명령이 효력 중이면 true
     */
    public boolean queryCommandState(String name) {
        String c = lower(name);
        Element host = getSelectionHost();
        if (host == null) {
            return false;
        }
        if (FORMATS.containsKey(c)) {
            return queryFormat(c, host);
        }
        if ("insertorderedlist".equals(c) || "insertunorderedlist".equals(c)) {
            Node n = doc.getSelection().getFocusNode();
            // 위로 올라가며 목록을 찾는다.
            for (Node a = n; a != null && a != host; a = a.getParentNode()) {
                if (isNamed(a, "insertorderedlist".equals(c) ? "ol" : "ul")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the value of a command for the selection (<code>queryCommandValue</code>): the block that holds the caret for
     * <code>formatBlock</code>, the paragraph separator for <code>defaultParagraphSeparator</code>, else an empty string.
     * <p>
     * 선택에 대한 명령의 값을 반환한다(<code>queryCommandValue</code>): <code>formatBlock</code> 은 캐럿을 가진 블록, <code>defaultParagraphSeparator</code> 는
     * 문단 구분 요소, 나머지는 빈 문자열.
     *
     * @param name the command name / 명령 이름
     * @return the value / 값
     */
    public String queryCommandValue(String name) {
        String c = lower(name);
        if ("defaultparagraphseparator".equals(c)) {
            return paragraphSeparator;
        }
        Element host = getSelectionHost();
        if ("formatblock".equals(c) && host != null) {
            return blockOf(doc.getSelection().getFocusNode(), host).getLocalName();
        }
        return "";
    }

    /**
     * Lower-cases a command name.
     * <p>
     * 명령 이름을 소문자로 바꾼다.
     *
     * @param name the name / 이름
     * @return the lower-case name / 소문자 이름
     */
    private static String lower(String name) {
        return name == null ? "" : name.toLowerCase(java.util.Locale.ENGLISH);
    }

    /**
     * Runs an editing command (<code>document.execCommand(name, showUI, value)</code>); it is false when the command is not supported, is
     * not enabled, or was cancelled.
     * <p>
     * 편집 명령을 실행한다(<code>document.execCommand(name, showUI, value)</code>). 지원하지 않거나 실행할 수 없거나 취소되면 false.
     *
     * @param name  the command name, in any letter case / 명령 이름(대소문자 무관)
     * @param value the value argument, or null / 값 인수. 없으면 null
     * @return true if the command ran / 명령을 실행했으면 true
     */
    public boolean execCommand(String name, String value) {
        String c = lower(name);
        if (!isCommandEnabled(c)) {
            return false;
        }
        if (FORMATS.containsKey(c)) {
            return toggleFormat(c);
        }
        if ("inserttext".equals(c)) {
            return insertText(value);
        }
        if ("inserthtml".equals(c)) {
            return insertHtml(value);
        }
        if ("insertparagraph".equals(c)) {
            return insertParagraph();
        }
        if ("insertlinebreak".equals(c)) {
            return insertLineBreak();
        }
        if ("delete".equals(c)) {
            return delete(true, false);
        }
        if ("forwarddelete".equals(c)) {
            return delete(false, false);
        }
        if ("selectall".equals(c)) {
            return selectAll();
        }
        if ("undo".equals(c)) {
            return undo();
        }
        if ("redo".equals(c)) {
            return redo();
        }
        if ("defaultparagraphseparator".equals(c)) {
            String v = lower(value);
            if ("div".equals(v) || "p".equals(v)) {
                paragraphSeparator = v;
                return true;
            }
            return false;
        }
        if ("stylewithcss".equals(c) || "usecss".equals(c)) {
            return true;
        }
        return execBlockCommand(c, value);
    }

    /**
     * Runs the commands that change the structure of the blocks.
     * <p>
     * 블록의 구조를 바꾸는 명령을 실행한다.
     *
     * @param c     the command name in lower case / 소문자 명령 이름
     * @param value the value argument / 값 인수
     * @return true if the command ran / 명령을 실행했으면 true
     */
    private boolean execBlockCommand(String c, final String value) {
        if ("removeformat".equals(c)) {
            return perform("formatRemove", null, null, selectionRanges(), null, new Operation() {
                /**
                 * Removes the inline formats.
                 * <p>
                 * 인라인 서식을 제거한다.
                 *
                 * @param host the editing host / 편집 호스트
                 */
                public void run(Element host) {
                    Range r = doc.getSelection().getRangeAt(0);
                    splitEnds(r);
                    List<Text> texts = textsOf(r);
                    // 텍스트마다 모든 서식 요소에서 꺼낸다.
                    for (int i = 0; i < texts.size(); i++) {
                        for (String[] names : FORMATS.values()) {
                            removeFormat(texts.get(i), names, host);
                        }
                    }
                    if (!texts.isEmpty()) {
                        doc.getSelection().setBaseAndExtent(texts.get(0), 0, texts.get(texts.size() - 1), texts.get(texts.size() - 1).getLength());
                    }
                }
            });
        }
        if ("createlink".equals(c) && value != null && value.length() > 0) {
            return perform("insertLink", value, null, selectionRanges(), null, new Operation() {
                /**
                 * Wraps the selected text in a link.
                 * <p>
                 * 선택한 텍스트를 링크로 감싼다.
                 *
                 * @param host the editing host / 편집 호스트
                 */
                public void run(Element host) {
                    Range r = doc.getSelection().getRangeAt(0);
                    if (r.isCollapsed()) {
                        doInsertText(host, value);
                        r = doc.createRange();
                        Pos p = caret();
                        r.setStart(p.node, Math.max(0, p.offset - value.length()));
                        r.setEnd(p.node, p.offset);
                    }
                    splitEnds(r);
                    List<Text> texts = textsOf(r);
                    // 텍스트마다 링크 요소로 감싼다(이미 링크 안이면 주소만 바꾼다).
                    for (int i = 0; i < texts.size(); i++) {
                        Element a = formatAncestor(texts.get(i), new String[] {"a"}, host);
                        if (a == null) {
                            a = doc.createElement("a");
                            texts.get(i).getParentNode().insertBefore(a, texts.get(i));
                            a.appendChild(texts.get(i));
                        }
                        a.setAttribute("href", value);
                    }
                    // 링크를 만든 뒤에도 같은 텍스트가 선택된 채로 둔다.
                    if (!texts.isEmpty()) {
                        doc.getSelection().setBaseAndExtent(texts.get(0), 0, texts.get(texts.size() - 1), texts.get(texts.size() - 1).getLength());
                    }
                }
            });
        }
        if ("unlink".equals(c)) {
            return perform("insertLink", null, null, selectionRanges(), null, new Operation() {
                /**
                 * Takes the selected text out of its links.
                 * <p>
                 * 선택한 텍스트를 링크에서 꺼낸다.
                 *
                 * @param host the editing host / 편집 호스트
                 */
                public void run(Element host) {
                    Range r = doc.getSelection().getRangeAt(0);
                    splitEnds(r);
                    List<Text> texts = textsOf(r);
                    // 텍스트마다 링크 요소에서 꺼낸다.
                    for (int i = 0; i < texts.size(); i++) {
                        removeFormat(texts.get(i), new String[] {"a"}, host);
                    }
                }
            });
        }
        if ("formatblock".equals(c) && value != null) {
            final String tag = value.trim().toLowerCase(java.util.Locale.ENGLISH).replace("<", "").replace(">", "");
            if (!BLOCKS.contains(tag) || "html".equals(tag) || "body".equals(tag) || "li".equals(tag) || "td".equals(tag) || "th".equals(tag)) {
                return false;
            }
            return perform("insertParagraph", null, null, selectionRanges(), null, new Operation() {
                /**
                 * Renames the block of the caret.
                 * <p>
                 * 캐럿의 블록의 이름을 바꾼다.
                 *
                 * @param host the editing host / 편집 호스트
                 */
                public void run(Element host) {
                    Object[] saved = saveSelection();
                    Pos p = new Pos(doc.getSelection().getFocusNode(), doc.getSelection().getFocusOffset());
                    Element block = blockOf(p.node, host);
                    // 호스트 바로 아래의 줄이면 새 블록으로 감싸고, 블록이면 이름이 다른 요소로 바꾼다.
                    if (block == host) {
                        Element wrap = doc.createElement(tag);
                        Node top = p.node;
                        // 호스트의 직계 자식에 닿을 때까지 올라간다.
                        while (top.getParentNode() != host) {
                            top = top.getParentNode();
                        }
                        Node first = top;
                        // 같은 줄을 이루는 앞쪽 인라인 형제를 거슬러 올라간다.
                        while (first.getPreviousSibling() != null && !isBlock(first.getPreviousSibling())) {
                            first = first.getPreviousSibling();
                        }
                        host.insertBefore(wrap, first);
                        Node n = first;
                        // 같은 줄의 인라인 형제를 새 블록으로 옮긴다.
                        while (n != null && !isBlock(n) && n != wrap) {
                            Node next = n.getNextSibling();
                            wrap.appendChild(n);
                            n = next;
                        }
                    } else {
                        Element renamed = doc.createElement(tag);
                        Node child = block.getFirstChild();
                        // 옛 블록의 자식을 새 요소로 옮긴다.
                        while (child != null) {
                            Node next = child.getNextSibling();
                            renamed.appendChild(child);
                            child = next;
                        }
                        block.getParentNode().replaceChild(renamed, block);
                    }
                    restoreSelection(saved);
                }
            });
        }
        if ("insertorderedlist".equals(c) || "insertunorderedlist".equals(c)) {
            final String listTag = "insertorderedlist".equals(c) ? "ol" : "ul";
            return perform(listTag.equals("ol") ? "insertOrderedList" : "insertUnorderedList", null, null, selectionRanges(), null, new Operation() {
                /**
                 * Turns the block of the caret into a list item, or takes it out of the list.
                 * <p>
                 * 캐럿의 블록을 목록 항목으로 바꾸거나 목록에서 꺼낸다.
                 *
                 * @param host the editing host / 편집 호스트
                 */
                public void run(Element host) {
                    toggleList(listTag, host);
                }
            });
        }
        if ("inserthorizontalrule".equals(c)) {
            return perform("insertHorizontalRule", null, null, selectionRanges(), null, new Operation() {
                /**
                 * Inserts a rule after the paragraph break.
                 * <p>
                 * 문단 나누기 뒤에 가로줄을 넣는다.
                 *
                 * @param host the editing host / 편집 호스트
                 */
                public void run(Element host) {
                    deleteSelectionIfAny(host);
                    Pos p = caret();
                    Element block = blockOf(p.node, host);
                    Element rule = doc.createElement("hr");
                    if (block == host) {
                        int idx = splitUntil(host, p);
                        host.insertBefore(rule, childAt(host, idx));
                    } else {
                        int idx = splitUntil(block, p);
                        Node after = block.getNextSibling();
                        Element tail = (Element) block.cloneNode(false);
                        Node n = childAt(block, idx);
                        // 나누는 위치 뒤의 자식을 새 블록으로 옮긴다.
                        while (n != null) {
                            Node next = n.getNextSibling();
                            tail.appendChild(n);
                            n = next;
                        }
                        ensurePlaceholder(tail);
                        block.getParentNode().insertBefore(rule, after);
                        block.getParentNode().insertBefore(tail, after);
                        setCaret(startOf(tail));
                        return;
                    }
                    setCaret(new Pos(rule.getParentNode(), indexOf(rule) + 1));
                }
            });
        }
        if ("insertimage".equals(c) && value != null && value.length() > 0) {
            return insertHtmlElement("img", "src", value);
        }
        return false;
    }

    /**
     * Remembers the four numbers of the selection so that {@link #restoreSelection} can put it back after nodes were moved (moving a node
     * takes the boundary points that are inside it out to its old parent).
     * <p>
     * 선택의 네 값을 기억해, 노드를 옮긴 뒤 {@link #restoreSelection} 이 되돌려 놓을 수 있게 한다(노드를 옮기면 그 안에 있던 경계점이 옛 부모로 나온다).
     *
     * @return the anchor node, anchor offset, focus node, focus offset as an array of objects / 앵커 노드, 앵커 오프셋, focus 노드, focus 오프셋
     */
    private Object[] saveSelection() {
        Selection s = doc.getSelection();
        if (s.getRangeCount() == 0) {
            return null;
        }
        return new Object[] {s.getAnchorNode(), Integer.valueOf(s.getAnchorOffset()), s.getFocusNode(), Integer.valueOf(s.getFocusOffset())};
    }

    /**
     * Puts a selection that {@link #saveSelection} remembered back, if its nodes are still in the document.
     * <p>
     * {@link #saveSelection} 이 기억한 선택을, 그 노드가 아직 문서에 있으면 되돌려 놓는다.
     *
     * @param saved the remembered selection / 기억한 선택
     */
    private void restoreSelection(Object[] saved) {
        if (saved == null) {
            return;
        }
        Node a = (Node) saved[0];
        Node f = (Node) saved[2];
        if (a.isConnected() && f.isConnected()) {
            doc.getSelection().setBaseAndExtent(a, ((Integer) saved[1]).intValue(), f, ((Integer) saved[3]).intValue());
        }
    }

    /**
     * Inserts an element with one attribute at the selection.
     * <p>
     * 속성 하나를 가진 요소를 선택에 넣는다.
     *
     * @param tag   the element name / 요소 이름
     * @param attr  the attribute name / 속성 이름
     * @param value the attribute value / 속성 값
     * @return true if inserted / 넣었으면 true
     */
    private boolean insertHtmlElement(final String tag, final String attr, final String value) {
        return perform("insertText", null, null, selectionRanges(), null, new Operation() {
            /**
             * Inserts the element.
             * <p>
             * 요소를 넣는다.
             *
             * @param host the editing host / 편집 호스트
             */
            public void run(Element host) {
                deleteSelectionIfAny(host);
                Element e = doc.createElement(tag);
                e.setAttribute(attr, value);
                DocumentFragment f = doc.createDocumentFragment();
                f.appendChild(e);
                insertFragmentAt(host, f);
            }
        });
    }

    /**
     * Turns the block of the caret into an item of a list of a kind, or takes it out when it is one already.
     * <p>
     * 캐럿의 블록을 어떤 종류의 목록 항목으로 바꾸고, 이미 그러면 목록에서 꺼낸다.
     *
     * @param listTag <code>ul</code> or <code>ol</code> / <code>ul</code> 또는 <code>ol</code>
     * @param host    the editing host / 편집 호스트
     */
    private void toggleList(String listTag, Element host) {
        Object[] saved = saveSelection();
        toggleListMoving(listTag, host);
        restoreSelection(saved);
    }

    /**
     * Does the work of {@link #toggleList}: the nodes are moved, so the caller restores the selection.
     * <p>
     * {@link #toggleList} 의 일을 한다. 노드를 옮기므로 호출자가 선택을 되돌린다.
     *
     * @param listTag <code>ul</code> or <code>ol</code> / <code>ul</code> 또는 <code>ol</code>
     * @param host    the editing host / 편집 호스트
     */
    private void toggleListMoving(String listTag, Element host) {
        Pos p = caret();
        Element block = blockOf(p.node, host);
        Element item = block;
        // 이미 목록 항목이면 목록에서 꺼내 일반 단락으로 만든다(다른 종류의 목록이면 종류를 바꾼다).
        if (isNamed(block, "li") && isList(block.getParentNode())) {
            Element list = (Element) block.getParentNode();
            if (listTag.equals(list.getLocalName())) {
                Element div = doc.createElement(paragraphSeparator);
                Node child = block.getFirstChild();
                // 블록의 자식을 새 div 로 옮긴다.
                while (child != null) {
                    Node next = child.getNextSibling();
                    div.appendChild(child);
                    child = next;
                }
                ensurePlaceholder(div);
                Node after = list.getNextSibling();
                if (block.getNextSibling() != null) {
                    Element rest = (Element) list.cloneNode(false);
                    Node c = block.getNextSibling();
                    // 항목 뒤의 형제를 새 목록으로 옮긴다.
                    while (c != null) {
                        Node next = c.getNextSibling();
                        rest.appendChild(c);
                        c = next;
                    }
                    list.getParentNode().insertBefore(rest, after);
                    after = rest;
                }
                list.getParentNode().insertBefore(div, after);
                list.removeChild(block);
                if (list.getFirstChild() == null) {
                    list.getParentNode().removeChild(list);
                }
                setCaret(startOf(div));
            } else {
                Element renamed = doc.createElement(listTag);
                Node child = list.getFirstChild();
                // 목록의 자식을 이름을 바꾼 목록으로 옮긴다.
                while (child != null) {
                    Node next = child.getNextSibling();
                    renamed.appendChild(child);
                    child = next;
                }
                list.getParentNode().replaceChild(renamed, list);
            }
            return;
        }
        Element list = doc.createElement(listTag);
        Element li = doc.createElement("li");
        // 호스트 바로 아래의 줄은 줄 전체를 항목으로 감싸고, 블록이면 블록의 내용을 항목으로 옮긴다.
        if (block == host) {
            Node top = p.node;
            // 호스트의 직계 자식에 닿을 때까지 올라간다.
            while (top.getParentNode() != host) {
                top = top.getParentNode();
            }
            Node first = top;
            // 같은 줄을 이루는 앞쪽 인라인 형제를 거슬러 올라간다.
            while (first.getPreviousSibling() != null && !isBlock(first.getPreviousSibling())) {
                first = first.getPreviousSibling();
            }
            host.insertBefore(list, first);
            list.appendChild(li);
            Node n = first;
            // 같은 줄의 인라인 형제를 새 항목으로 옮긴다.
            while (n != null && n != list && !isBlock(n)) {
                Node next = n.getNextSibling();
                li.appendChild(n);
                n = next;
            }
        } else {
            Node child = item.getFirstChild();
            // 블록의 자식을 새 항목으로 옮긴다.
            while (child != null) {
                Node next = child.getNextSibling();
                li.appendChild(child);
                child = next;
            }
            list.appendChild(li);
            item.getParentNode().replaceChild(list, item);
        }
        ensurePlaceholder(li);
        setCaret(startOf(li));
    }

    /**
     * Selects all the content of the editing host that holds the selection (Select All inside an editing host); the body when the
     * selection is not in one.
     * <p>
     * 선택을 가진 편집 호스트의 모든 내용을 선택한다(편집 호스트 안의 전체 선택). 선택이 호스트 안이 아니면 body.
     *
     * @return true if something was selected / 선택했으면 true
     */
    public boolean selectAll() {
        Element host = getSelectionHost();
        return selectAllIn(host);
    }

    /**
     * Returns the environment that works without a host (events are sent to the target as trusted events; blocks and preserved white space
     * are decided by element names). A host that wraps the engine uses it as the base of its own environment.
     * <p>
     * 호스트 없이 동작하는 환경을 반환한다(이벤트는 신뢰 이벤트로 대상에 가고, 블록과 보존되는 공백은 요소 이름으로 정한다). 엔진을 감싸는 호스트가 자기 환경의
     * 바탕으로 쓴다.
     *
     * @return the default environment / 기본 환경
     */
    public static Environment defaultEnvironment() {
        return DEFAULT_ENVIRONMENT;
    }

    /**
     * Puts the caret (a collapsed selection) at the start or at the end of the content of an editing host, as when the user moves the focus
     * into the host with the keyboard or presses Ctrl+Home or Ctrl+End.
     * <p>
     * 캐럿(접힌 선택)을 편집 호스트 내용의 처음이나 끝에 둔다. 사용자가 키보드로 호스트에 포커스를 옮기거나 Ctrl+Home, Ctrl+End 를 누를 때와 같다.
     *
     * @param host the editing host / 편집 호스트
     * @param end  true for the end, false for the start / 끝이면 true, 처음이면 false
     */
    public void placeCaret(Element host, boolean end) {
        Pos p = end ? endOf(host) : startOf(host);
        doc.getSelection().collapse(p.node, p.offset);
    }

    /**
     * Selects all the content of an editing host (the body when the host is null).
     * <p>
     * 편집 호스트의 모든 내용을 선택한다(호스트가 null 이면 body).
     *
     * @param host the editing host or null / 편집 호스트. 없으면 null
     * @return true if something was selected / 선택했으면 true
     */
    private boolean selectAllIn(Element host) {
        Node root = host;
        if (root == null) {
            org.duckdns.hjow.swingwebpanel.dom.ElementCollection bodies = doc.getElementsByTagName("body");
            root = bodies.getLength() > 0 ? bodies.item(0) : doc.getDocumentElement();
        }
        if (root == null) {
            return false;
        }
        doc.getSelection().selectAllChildren(root);
        return true;
    }
}
