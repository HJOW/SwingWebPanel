package org.duckdns.hjow.swingwebpanel.core;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardEvent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.Selection;
import org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts;
import org.duckdns.hjow.swingwebpanel.dom.edit.Editor;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * Editing by the user in the rendered page (plan item P20-06): what the keys do inside an editing host (an element with
 * <code>contenteditable</code>, or a document in design mode). Typed characters, Enter, Shift+Enter, Backspace, Delete (with Ctrl: by word),
 * the shortcuts Ctrl+B, Ctrl+I, Ctrl+U, Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z, Ctrl+A, Ctrl+X and Ctrl+V, and the caret keys (arrows, Home and End,
 * with Ctrl by word or to the edge of the host) become operations of the {@link Editor} of the document. The editor does the work on the DOM
 * and sends <code>beforeinput</code> and <code>input</code>; this class only decides what a key means, joins the editor with the layout (the
 * blocks and the preserved white space come from the computed style, the caret moves along the lines of the page) and with the clipboard
 * (<code>cut</code> and <code>paste</code> events). All work happens on the document queue; the clipboard is read on the event dispatch
 * thread before the paste is queued.
 * <p>
 * 렌더링된 페이지에서 사용자의 편집(계획 P20-06): 편집 호스트(<code>contenteditable</code> 요소나 디자인 모드 문서) 안에서 키가 하는 일. 입력한 글자, Enter,
 * Shift+Enter, Backspace, Delete(Ctrl 이면 낱말 단위), 단축키 Ctrl+B, Ctrl+I, Ctrl+U, Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z, Ctrl+A, Ctrl+X, Ctrl+V, 캐럿 키(화살표,
 * Home, End. Ctrl 이면 낱말 단위나 호스트의 가장자리)가 문서의 {@link Editor} 의 작업이 된다. 편집기가 DOM 을 바꾸고 <code>beforeinput</code> 과
 * <code>input</code> 을 보낸다. 이 클래스는 키의 뜻을 정하고, 편집기를 레이아웃(블록과 보존되는 공백은 계산 스타일에서, 캐럿은 페이지의 줄을 따라 움직인다)과
 * 클립보드(<code>cut</code>, <code>paste</code> 이벤트)에 잇는다. 모든 일은 문서 큐에서 일어나며, 클립보드는 붙여넣기를 큐에 넣기 전에 이벤트 디스패치
 * 스레드에서 읽는다.
 */
public final class PageEditing {

    /**
     * The session that owns the document queue.
     * <p>
     * 문서 큐를 소유한 세션.
     */
    private final BrowserSession session;

    /**
     * The view that knows the rendering of the page.
     * <p>
     * 페이지의 렌더링을 아는 뷰.
     */
    private final PageView view;

    /**
     * The selection controller (caret movement, copy and cut).
     * <p>
     * 선택 제어기(캐럿 이동, 복사, 잘라내기).
     */
    private final PageSelection selection;

    /**
     * Where pasted data comes from, or null for none.
     * <p>
     * 붙여넣는 데이터의 출처. 없으면 null.
     */
    private volatile ClipboardSource source;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * Creates the controller of a page component.
     * <p>
     * 페이지 컴포넌트의 제어기를 만든다.
     *
     * @param session   the session / 세션
     * @param view      the view / 뷰
     * @param selection the selection controller / 선택 제어기
     */
    public PageEditing(BrowserSession session, PageView view, PageSelection selection) {
        this.session = session;
        this.view = view;
        this.selection = selection;
    }

    /**
     * Sets where pasted data comes from (null: paste events carry no data).
     * <p>
     * 붙여넣는 데이터의 출처를 정한다(null 이면 붙여넣기 이벤트에 데이터가 없다).
     *
     * @param clipboard the source or null / 출처. 없으면 null
     */
    public void setClipboardSource(ClipboardSource clipboard) {
        this.source = clipboard;
    }

    /**
     * Stops all work (the component was disposed).
     * <p>
     * 모든 일을 멈춘다(컴포넌트가 폐기되었다).
     */
    public void dispose() {
        disposed = true;
        source = null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 클립보드 읽기(이벤트 디스패치 스레드)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads the clipboard into the data of a <code>paste</code> event (event dispatch thread): <code>text/plain</code> and
     * <code>text/html</code> if the clipboard has them. A clipboard that cannot be used gives empty data and a status message.
     * <p>
     * 클립보드를 <code>paste</code> 이벤트의 데이터로 읽는다(이벤트 디스패치 스레드): 클립보드에 있으면 <code>text/plain</code> 과 <code>text/html</code>.
     * 클립보드를 쓸 수 없으면 빈 데이터를 만들고 상태 메시지를 보인다.
     *
     * @return the data, never null / 데이터. null 이 아니다
     */
    ClipboardData readClipboard() {
        ClipboardData data = new ClipboardData();
        ClipboardSource from = source;
        if (from == null || disposed) {
            return data;
        }
        try {
            Transferable t = from.read();
            if (t != null) {
                fill(data, t);
            }
        } catch (Exception e) {
            // 헤드리스 환경이나 접근 거부 등으로 읽을 수 없으면 빈 데이터로 두고 상태 메시지만 보인다.
            session.setStatusMessage("Paste failed: the clipboard cannot be used");
        } catch (java.awt.AWTError e) {
            session.setStatusMessage("Paste failed: the clipboard cannot be used");
        }
        return data;
    }

    /**
     * Copies the text and the markup of a transferable into clipboard data.
     * <p>
     * 전달 객체의 텍스트와 마크업을 클립보드 데이터에 옮긴다.
     *
     * @param data the data to fill / 채울 데이터
     * @param t    the transferable / 전달 객체
     * @throws Exception if the transferable fails / 전달 객체가 실패하면
     */
    private static void fill(ClipboardData data, Transferable t) throws Exception {
        if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            Object text = t.getTransferData(DataFlavor.stringFlavor);
            if (text instanceof String) {
                data.put("text/plain", (String) text);
            }
        }
        DataFlavor[] flavors = t.getTransferDataFlavors();
        // 형식 목록에서 문자열로 읽을 수 있는 text/html 을 찾는다.
        for (int i = 0; flavors != null && i < flavors.length; i++) {
            DataFlavor f = flavors[i];
            if ("text".equals(f.getPrimaryType()) && "html".equals(f.getSubType()) && f.getRepresentationClass() == String.class) {
                Object html = t.getTransferData(f);
                if (html instanceof String) {
                    data.put("text/html", fragmentOf((String) html));
                    break;
                }
            }
        }
    }

    /**
     * Returns the fragment of a clipboard HTML text: the part between the <code>StartFragment</code> and <code>EndFragment</code> comments
     * that some platforms add, else the whole text.
     * <p>
     * 클립보드 HTML 텍스트의 조각을 반환한다: 일부 플랫폼이 더하는 <code>StartFragment</code>, <code>EndFragment</code> 주석 사이, 없으면 전체.
     *
     * @param html the text / 텍스트
     * @return the fragment / 조각
     */
    static String fragmentOf(String html) {
        int start = html.indexOf("<!--StartFragment-->");
        int end = html.indexOf("<!--EndFragment-->");
        if (start >= 0 && end > start) {
            return html.substring(start + "<!--StartFragment-->".length(), end);
        }
        return html;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 환경: 이벤트 전달과 레이아웃 지식
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The environment of the editor for one page: events go through the script host of the document that owns the target (so that
     * microtasks and observers run after them), blocks and preserved white space come from the computed style of the layout where there is
     * a box, and from the names of the elements otherwise.
     * <p>
     * 한 페이지에 대한 편집기의 환경: 이벤트는 대상을 소유한 문서의 스크립트 호스트를 거쳐 간다(이후에 마이크로태스크와 관찰자가 실행된다). 블록과 보존되는 공백은
     * 상자가 있으면 레이아웃의 계산 스타일에서, 없으면 요소 이름에서 정한다.
     */
    private final class PageEnvironment implements Editor.Environment {

        /**
         * The page that the environment belongs to.
         * <p>
         * 환경이 속한 페이지.
         */
        private final PageSnapshot snapshot;

        /**
         * Creates the environment.
         * <p>
         * 환경을 만든다.
         *
         * @param snapshot the page / 페이지
         */
        PageEnvironment(PageSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        /**
         * Sends the event through the script host of the document of the target.
         * <p>
         * 대상의 문서의 스크립트 호스트로 이벤트를 보낸다.
         *
         * @param target the target / 대상
         * @param event  the event / 이벤트
         * @return false if cancelled / 취소되었으면 false
         */
        public boolean dispatch(Node target, Event event) {
            HtmlPageDocument top = holder(snapshot);
            if (top == null) {
                return target.dispatchTrustedEvent(event);
            }
            HtmlPageDocument owner = top.ownerOf(target);
            return (owner == null ? top : owner).dispatchInput(target, event);
        }

        /**
         * A box with a block-level <code>display</code> makes a block; without a box the names decide.
         * <p>
         * 블록 수준 <code>display</code> 의 상자는 블록이다. 상자가 없으면 이름으로 정한다.
         *
         * @param e the element / 요소
         * @return true for a block / 블록이면 true
         */
        public boolean isBlock(Element e) {
            ComputedStyle style = styleOf(e);
            if (style == null) {
                return Editor.defaultEnvironment().isBlock(e);
            }
            String display = style.display();
            return "block".equals(display) || "list-item".equals(display) || "flex".equals(display) || "grid".equals(display)
                    || "flow-root".equals(display) || display.startsWith("table");
        }

        /**
         * White space is preserved where the computed <code>white-space</code> of the closest element with a box keeps spaces.
         * <p>
         * 상자가 있는 가장 가까운 요소의 계산된 <code>white-space</code> 가 공백을 보존하면 보존된다.
         *
         * @param node the node / 노드
         * @return true if preserved / 보존되면 true
         */
        public boolean preservesWhitespace(Node node) {
            // 노드에서 위로 올라가며 상자가 있는 첫 요소의 계산 스타일을 본다.
            for (Node n = node; n != null; n = n.getParentNode()) {
                ComputedStyle style = n instanceof Element ? styleOf((Element) n) : null;
                if (style != null) {
                    String ws = style.keyword(Prop.WHITE_SPACE);
                    return "pre".equals(ws) || "pre-wrap".equals(ws) || "break-spaces".equals(ws);
                }
            }
            return Editor.defaultEnvironment().preservesWhitespace(node);
        }

        /**
         * Returns the computed style of the first box of an element in the current rendering, or null.
         * <p>
         * 현재 렌더링에서 요소의 첫 상자의 계산 스타일을 반환한다. 없으면 null.
         *
         * @param e the element / 요소
         * @return the style or null / 스타일. 없으면 null
         */
        private ComputedStyle styleOf(Element e) {
            RenderedPage page = view.getRenderedPage();
            if (page == null) {
                return null;
            }
            List<LayoutBox> boxes = page.getLayout().getBoxes(e);
            return boxes.isEmpty() ? null : boxes.get(0).getStyle();
        }
    }

    /**
     * Returns the document holder of a page, or null if it is not an HTML page.
     * <p>
     * 페이지의 문서 보관 객체를 반환한다. HTML 페이지가 아니면 null.
     *
     * @param snapshot the page / 페이지
     * @return the holder or null / 보관 객체. 없으면 null
     */
    private static HtmlPageDocument holder(PageSnapshot snapshot) {
        return snapshot != null && snapshot.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) snapshot.getDocument() : null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 키 처리(문서 큐)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the editor of a document ready for an operation inside a host: the environment of the page is installed, and the caret is put
     * at the start of the host when the selection is not inside it (the focus came with the Tab key).
     * <p>
     * 호스트 안의 작업을 위해 준비된 문서의 편집기를 반환한다: 페이지의 환경을 설치하고, 선택이 호스트 안이 아니면(Tab 키로 포커스가 왔다) 캐럿을 호스트의
     * 처음에 둔다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param host     the editing host / 편집 호스트
     * @return the editor / 편집기
     */
    private Editor prepare(PageSnapshot snapshot, Document doc, Element host) {
        Editor editor = doc.getEditor();
        editor.setEnvironment(new PageEnvironment(snapshot));
        Selection sel = doc.getSelection();
        // 선택이 없거나 다른 곳에 있으면 호스트의 처음에 캐럿을 둔다.
        if (sel.getRangeCount() == 0 || editor.getSelectionHost() != host) {
            editor.placeCaret(host, false);
        }
        return editor;
    }

    /**
     * Does the default action of a key that is pressed (<code>keydown</code> that no listener cancelled) inside an editing host.
     * <p>
     * 편집 호스트 안에서 눌린 키(어떤 리스너도 취소하지 않은 <code>keydown</code>)의 기본 동작을 한다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param target   the element that had the event: the focused element, else the body / 이벤트를 받은 요소: 포커스를 가진 요소, 없으면 body
     * @param key      the DOM key value / DOM key 값
     * @param keyCode  the legacy key code (the AWT virtual key: the letter of a Ctrl shortcut, whatever character the platform reports for the key press) / 예전 키 코드(AWT 가상 키. Ctrl 단축키의 글자. 플랫폼이 키 누름에 보고하는 문자와 무관하다)
     * @param ctrl     true if Ctrl is down / Ctrl 이 눌려 있으면 true
     * @param shift    true if Shift is down / Shift 가 눌려 있으면 true
     * @param alt      true if Alt is down / Alt 가 눌려 있으면 true
     * @param meta     true if Meta is down / Meta 가 눌려 있으면 true
     * @return true if the key belongs to editing and was handled / 키가 편집의 것이어서 처리했으면 true
     */
    boolean keyDown(PageSnapshot snapshot, Document doc, Element target, String key, int keyCode, boolean ctrl, boolean shift, boolean alt, boolean meta) {
        Element host = disposed ? null : EditingHosts.hostOf(target);
        if (host == null || alt || meta) {
            return false;
        }
        String k = key == null ? "" : key;
        // Ctrl 이 없는 키: 지우기와 캐럿 이동(Shift 가 있는 캐럿 키는 선택 확장이라 PageKeyboard 가 맡는다).
        if (!ctrl) {
            if ("Backspace".equals(k) || "Delete".equals(k)) {
                if (shift && "Delete".equals(k)) {
                    return cut(snapshot, doc, host);
                }
                prepare(snapshot, doc, host).delete("Backspace".equals(k), false);
                selection.requestReveal();
                return true;
            }
            if (!shift && isCaretKey(k)) {
                prepare(snapshot, doc, host);
                selection.moveByKey(snapshot, k, false);
                return true;
            }
            return false;
        }
        // Ctrl 이 있는 키: 서식, 되돌리기, 전체 선택, 잘라내기, 낱말 단위 지우기와 이동.
        Editor editor = null;
        boolean done = true;
        if (keyCode >= 'A' && keyCode <= 'Z') {
            char c = Character.toLowerCase((char) keyCode);
            if (c == 'b' || c == 'i' || c == 'u') {
                editor = prepare(snapshot, doc, host);
                editor.toggleFormat(c == 'b' ? "bold" : c == 'i' ? "italic" : "underline");
            } else if (c == 'z') {
                editor = prepare(snapshot, doc, host);
                if (shift) {
                    editor.redo();
                } else {
                    editor.undo();
                }
            } else if (c == 'y') {
                editor = prepare(snapshot, doc, host);
                editor.redo();
            } else if (c == 'a') {
                editor = prepare(snapshot, doc, host);
                editor.selectAll();
            } else if (c == 'x') {
                return cut(snapshot, doc, host);
            } else {
                done = false;
            }
        } else if ("Backspace".equals(k) || "Delete".equals(k)) {
            editor = prepare(snapshot, doc, host);
            editor.delete("Backspace".equals(k), true);
        } else if (!shift && ("Home".equals(k) || "End".equals(k))) {
            // Ctrl+Home, Ctrl+End 는 호스트의 처음과 끝으로 간다.
            prepare(snapshot, doc, host).placeCaret(host, "End".equals(k));
        } else if (!shift && isCaretKey(k)) {
            prepare(snapshot, doc, host);
            selection.moveByKey(snapshot, k, true);
            return true;
        } else {
            done = false;
        }
        if (done) {
            selection.requestReveal();
        }
        return done;
    }

    /**
     * Tells whether a DOM key value moves the caret (the arrow keys, Home and End).
     * <p>
     * DOM key 값이 캐럿을 움직이는지 알려 준다(화살표 키, Home, End).
     *
     * @param key the key / 키
     * @return true for a caret key / 캐럿 키이면 true
     */
    static boolean isCaretKey(String key) {
        return "ArrowLeft".equals(key) || "ArrowRight".equals(key) || "ArrowUp".equals(key) || "ArrowDown".equals(key) || "Home".equals(key)
                || "End".equals(key);
    }

    /**
     * Does the default action of a typed character (<code>keypress</code> that no listener cancelled) inside an editing host: the character
     * is inserted, Enter breaks the paragraph and Shift+Enter inserts a line break.
     * <p>
     * 편집 호스트 안에서 입력한 글자(어떤 리스너도 취소하지 않은 <code>keypress</code>)의 기본 동작을 한다: 글자를 넣고, Enter 는 문단을 나누며
     * Shift+Enter 는 줄바꿈을 넣는다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param target   the element that had the event / 이벤트를 받은 요소
     * @param typed    the DOM key value of the typed character ("Enter" for the Enter key) / 입력한 글자의 DOM key 값(Enter 키는 "Enter")
     * @param shift    true if Shift is down / Shift 가 눌려 있으면 true
     * @return true if the character belongs to editing and was handled / 글자가 편집의 것이어서 처리했으면 true
     */
    boolean keyPress(PageSnapshot snapshot, Document doc, Element target, String typed, boolean shift) {
        Element host = disposed ? null : EditingHosts.hostOf(target);
        if (host == null || typed == null || typed.length() == 0) {
            return false;
        }
        Editor editor = prepare(snapshot, doc, host);
        if ("Enter".equals(typed)) {
            if (shift) {
                editor.insertLineBreak();
            } else {
                editor.insertParagraph();
            }
        } else {
            editor.insertText(typed);
        }
        selection.requestReveal();
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 잘라내기와 붙여넣기(문서 큐)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Cuts the selection of an editing host: <code>cut</code> is sent, and the selection goes to the clipboard and is removed unless a
     * listener cancelled the event.
     * <p>
     * 편집 호스트의 선택을 잘라낸다: <code>cut</code> 이 전달되고, 리스너가 취소하지 않았으면 선택이 클립보드로 가고 지워진다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param host     the editing host / 편집 호스트
     * @return true (the key was handled) / true(키를 처리했다)
     */
    private boolean cut(PageSnapshot snapshot, Document doc, Element host) {
        final Editor editor = prepare(snapshot, doc, host);
        selection.cut(snapshot, new Runnable() {
            /** Removes the selected content as the input type deleteByCut. / 선택한 내용을 입력 종류 deleteByCut 으로 지운다. */
            public void run() {
                editor.deleteByCut();
            }
        });
        selection.requestReveal();
        return true;
    }

    /**
     * Pastes data (Ctrl+V, Shift+Insert): the cancelable <code>paste</code> event is sent to the focused element (the body if none has
     * the focus) with the data in its <code>clipboardData</code>; unless a listener cancelled it, the data is inserted into the editing
     * host at the selection (an event in a place that cannot be edited changes nothing).
     * <p>
     * 붙여넣는다(Ctrl+V, Shift+Insert): 취소할 수 있는 <code>paste</code> 이벤트를 포커스를 가진 요소(없으면 body)에 데이터를 <code>clipboardData</code> 에 담아
     * 보낸다. 리스너가 취소하지 않았으면 데이터를 편집 호스트의 선택 자리에 넣는다(편집할 수 없는 곳에서는 이벤트만 가고 아무것도 바뀌지 않는다).
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param target   the element that had the event: the focused element, else the body / 이벤트를 받은 요소: 포커스를 가진 요소, 없으면 body
     * @param data     the clipboard data read on the event dispatch thread / 이벤트 디스패치 스레드에서 읽은 클립보드 데이터
     * @return true if the event was sent / 이벤트를 보냈으면 true
     */
    boolean paste(PageSnapshot snapshot, Document doc, Element target, ClipboardData data) {
        HtmlPageDocument top = holder(snapshot);
        if (disposed || top == null || target == null) {
            return false;
        }
        HtmlPageDocument owner = top.ownerOf(target);
        // paste 이벤트의 clipboardData 는 읽기 전용이다(스크립트는 읽을 수만 있고 데이터를 바꾸거나 더할 수 없다).
        data.setMode(ClipboardData.READ_ONLY);
        boolean notCancelled = (owner == null ? top : owner).dispatchInput(target, new ClipboardEvent("paste", true, true, data));
        Element host = EditingHosts.hostOf(target);
        if (notCancelled && host != null && session.getSnapshot() == snapshot) {
            Editor editor = prepare(snapshot, doc, host);
            editor.insertFromPaste(data);
            selection.requestReveal();
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 끌어다 놓기: 놓은 텍스트 넣기(문서 큐)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Does the default action of a <code>drop</code> that no listener cancelled on an editable place: the dropped data is inserted at the
     * position under the pointer (<code>insertFromDrop</code>) and then selected. When the drag moves a selection out of an editing host,
     * that selection is deleted first (<code>deleteByDrag</code>); a drop inside the dragged selection itself changes nothing. The drop
     * point is a live range, so the deletion does not displace it.
     * <p>
     * 편집 가능한 곳에서 리스너가 취소하지 않은 <code>drop</code> 의 기본 동작을 한다: 놓은 데이터를 포인터 아래 위치에 넣고(<code>insertFromDrop</code>) 선택한다.
     * 끌기가 편집 호스트에서 선택을 옮기는 것이면 그 선택을 먼저 지운다(<code>deleteByDrag</code>). 끌고 있는 선택 안에 놓으면 아무것도 바뀌지 않는다. 놓는 자리는
     * 살아 있는 범위이므로 지우기가 그것을 옮기지 않는다.
     *
     * @param snapshot   the page / 페이지
     * @param doc        the document / 문서
     * @param target     the element that got the drop / drop 을 받은 요소
     * @param node       the node of the drop position / 놓는 위치의 노드
     * @param offset     the offset of the drop position / 놓는 위치의 오프셋
     * @param data       the dropped data (readable) / 놓은 데이터(읽을 수 있는 것)
     * @param moveSource the selection that the drag moves (it is deleted), or null to copy / 끌기가 옮기는 선택(지운다). 복사이면 null
     * @return true if the data was inserted / 데이터를 넣었으면 true
     */
    boolean drop(PageSnapshot snapshot, Document doc, Element target, Node node, int offset, ClipboardData data, Range moveSource) {
        Element host = disposed ? null : EditingHosts.hostOf(target);
        if (host == null || node == null || session.getSnapshot() != snapshot) {
            return false;
        }
        Editor editor = prepare(snapshot, doc, host);
        Selection sel = doc.getSelection();
        Range point = doc.createRange();
        point.setStart(node, offset);
        point.setEnd(node, offset);
        // 끌고 있는 선택 안에 놓으면 아무것도 하지 않는다.
        if (moveSource != null && !moveSource.isCollapsed() && moveSource.isPointInRange(node, offset)) {
            return false;
        }
        // 선택을 옮기는 끌기이면 원래 선택을 지우고(deleteByDrag) 놓는 자리는 살아 있는 범위로 따라간다.
        if (moveSource != null && !moveSource.isCollapsed()) {
            sel.setBaseAndExtent(moveSource.getStartContainer(), moveSource.getStartOffset(), moveSource.getEndContainer(), moveSource.getEndOffset());
            editor.deleteByDrag();
        }
        sel.collapse(point.getStartContainer(), point.getStartOffset());
        boolean inserted = editor.insertFromDrop(data);
        // 넣은 텍스트를 선택한다.
        if (inserted && sel.getRangeCount() > 0 && sel.getFocusNode() != null) {
            sel.setBaseAndExtent(point.getStartContainer(), point.getStartOffset(), sel.getFocusNode(), sel.getFocusOffset());
        }
        selection.requestReveal();
        return inserted;
    }

    /**
     * Deletes the selection that a drag moved to a place that is not an editing host (the default action of <code>dragend</code>): the
     * source selection must be inside one editing host.
     * <p>
     * 끌기가 편집 호스트가 아닌 곳으로 옮긴 선택을 지운다(<code>dragend</code> 의 기본 동작). 원래 선택은 하나의 편집 호스트 안에 있어야 한다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param source   the selection that was dragged / 끌린 선택
     * @return true if deleted / 지웠으면 true
     */
    boolean deleteDragged(PageSnapshot snapshot, Document doc, Range source) {
        Element host = disposed ? null : EditingHosts.hostOf(source.getStartContainer());
        if (host == null || source.isCollapsed() || session.getSnapshot() != snapshot) {
            return false;
        }
        Editor editor = prepare(snapshot, doc, host);
        doc.getSelection().setBaseAndExtent(source.getStartContainer(), source.getStartOffset(), source.getEndContainer(), source.getEndOffset());
        return editor.deleteByDrag();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 입력기의 조합 입력(문서 큐)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Applies the text of an input method to the editing host that has the keyboard focus (a Hangul composition, for example): the text that
     * is still composed is shown in the DOM as it is composed, with <code>compositionstart</code>, <code>compositionupdate</code>,
     * <code>beforeinput</code> and <code>input</code> of the input type <code>insertCompositionText</code>, and the committed text ends the
     * composition with <code>compositionend</code> (see {@link Editor#compose}).
     * <p>
     * 입력기의 텍스트를 키보드 포커스를 가진 편집 호스트에 적용한다(예: 한글 조합). 아직 조합 중인 텍스트는 조합되는 대로 DOM 에 보이며
     * <code>compositionstart</code>, <code>compositionupdate</code>, 입력 종류 <code>insertCompositionText</code> 의 <code>beforeinput</code> 과 <code>input</code> 이
     * 따르고, 확정된 텍스트는 <code>compositionend</code> 로 조합을 끝낸다({@link Editor#compose} 참조).
     *
     * @param snapshot  the page / 페이지
     * @param doc       the document / 문서
     * @param target    the focused element, else the body / 포커스를 가진 요소. 없으면 body
     * @param committed the committed text / 확정된 텍스트
     * @param composing the text that is still composed / 아직 조합 중인 텍스트
     * @return true if the text went into an editing host / 텍스트가 편집 호스트에 들어갔으면 true
     */
    boolean compose(PageSnapshot snapshot, Document doc, Element target, String committed, String composing) {
        Element host = disposed ? null : EditingHosts.hostOf(target);
        if (host == null) {
            return false;
        }
        Editor editor = prepare(snapshot, doc, host);
        boolean done = editor.compose(committed, composing);
        selection.requestReveal();
        return done;
    }
}
