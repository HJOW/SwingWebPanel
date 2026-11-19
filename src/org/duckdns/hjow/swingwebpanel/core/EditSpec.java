package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;

/**
 * Everything that the Swing editor of a text control needs to start: the element, the geometry and style of the control as it was drawn, the
 * text and the selection of the DOM, the flags of the control and the listener that receives the edits. The form host makes it on the document
 * thread and the overlay manager reads it on the EDT; it never changes.
 * <p>
 * 텍스트 컨트롤의 Swing 편집기가 시작하는 데 필요한 모든 것: 요소, 그려진 컨트롤의 위치와 스타일, DOM 의 글과 선택, 컨트롤의 플래그, 편집을 받는 리스너.
 * 폼 호스트가 문서 스레드에서 만들고 오버레이 관리자가 EDT 에서 읽으며, 바뀌지 않는다.
 */
final class EditSpec {

    /**
     * Receives what the user does in an editor. All methods are called on the EDT.
     * <p>
     * 사용자가 편집기에서 하는 일을 받는다. 모든 메서드는 EDT 에서 호출된다.
     */
    interface Listener {

        /**
         * The text changed (one or several edits that were merged).
         * <p>
         * 글이 바뀌었다(합쳐진 편집 하나 또는 여럿).
         *
         * @param text      the whole text of the editor / 편집기의 전체 글
         * @param inputType the <code>inputType</code> of the <code>input</code> event / <code>input</code> 이벤트의 <code>inputType</code>
         * @param data      the inserted text or null / 삽입한 글 또는 null
         */
        void textEdited(String text, String inputType, String data);

        /**
         * The caret moved or the selection changed.
         * <p>
         * 캐럿이 움직였거나 선택이 바뀌었다.
         *
         * @param start    the start of the selection (the caret if empty) / 선택 시작(비어 있으면 캐럿)
         * @param end      the end of the selection / 선택 끝
         * @param backward true if the caret is at the start of the selection / 캐럿이 선택의 시작에 있으면 true
         */
        void selectionEdited(int start, int end, boolean backward);

        /**
         * The input method started, updated or ended a composition.
         * <p>
         * 입력기가 조합을 시작, 갱신, 종료했다.
         *
         * @param type the DOM event type: compositionstart, compositionupdate or compositionend / DOM 이벤트 종류
         * @param data the composed text / 조합 중인 글
         */
        void composition(String type, String data);

        /**
         * The user cut, copied or pasted in the editor: the DOM event is sent to the control and the answer comes back to the callback (on
         * the document thread; the callback goes to the EDT itself).
         * <p>
         * 사용자가 편집기에서 잘라내기, 복사, 붙여넣기를 했다: DOM 이벤트가 컨트롤에 가고 답이 콜백으로 돌아온다(문서 스레드에서. 콜백이 스스로 EDT 로 간다).
         *
         * @param type     the event type: <code>cut</code>, <code>copy</code> or <code>paste</code> / 이벤트 종류
         * @param plain    the selected text (cut, copy) or the text of the clipboard (paste) / 선택한 글(잘라내기, 복사) 또는 클립보드의 글(붙여넣기)
         * @param html     the HTML of the clipboard for a paste (its fragment), or null / 붙여넣기의 클립보드 HTML(그 조각) 또는 null
         * @param callback receives the answer / 답을 받는다
         */
        void clipboard(String type, String plain, String html, ClipboardCallback callback);

        /**
         * Tells whether the page of the control is still the page that the component shows.
         * <p>
         * 컨트롤의 페이지가 아직 컴포넌트가 보여 주는 페이지인지 알려 준다.
         *
         * @return true if current / 현재 페이지이면 true
         */
        boolean isCurrent();
    }

    /**
     * What the editor does when the DOM event of a cut, a copy or a paste is over.
     * <p>
     * 잘라내기, 복사, 붙여넣기의 DOM 이벤트가 끝났을 때 편집기가 하는 일.
     */
    interface ClipboardCallback {
        /**
         * The event was handled (it is called on the document thread).
         * <p>
         * 이벤트를 처리했다(문서 스레드에서 호출된다).
         *
         * @param cancelled true if a listener cancelled the event (the default action does not happen) / 리스너가 이벤트를 취소했으면 true(기본 동작을 하지 않는다)
         * @param data      the data of the event: a script may have put its own data on it / 이벤트의 데이터. 스크립트가 자기 데이터를 넣었을 수 있다
         */
        void done(boolean cancelled, org.duckdns.hjow.swingwebpanel.dom.ClipboardData data);
    }

    /** The element. 요소. */
    final Element element;

    /** The drawn control, for the geometry, the font and the colors. 그려진 컨트롤(위치, 글꼴, 색). */
    final ControlSnapshot control;

    /** True for a password field. 암호 필드이면 true. */
    final boolean password;

    /** True for a text area. 텍스트 영역이면 true. */
    final boolean multiline;

    /** True if the text cannot be edited. 글을 편집할 수 없으면 true. */
    final boolean readOnly;

    /** True if the control is disabled. 컨트롤이 비활성이면 true. */
    final boolean disabled;

    /** The <code>maxlength</code> or -1. <code>maxlength</code> 또는 -1. */
    final int maxLength;

    /** The wrap attribute of a text area is off. 텍스트 영역의 줄 바꿈이 꺼져 있다. */
    final boolean noWrap;

    /** The text of the control. 컨트롤의 글. */
    final String text;

    /** The selection start and end in the text. 글에서 선택의 시작과 끝. */
    final int selectionStart, selectionEnd;

    /** True if the selection is backward. 선택이 뒤쪽 방향이면 true. */
    final boolean backward;

    /** The receiver of the edits. 편집을 받는 쪽. */
    final Listener listener;

    /**
     * Creates the specification.
     * <p>
     * 명세를 만든다.
     *
     * @param element  the element / 요소
     * @param control  the drawn control / 그려진 컨트롤
     * @param password true for a password field / 암호 필드이면 true
     * @param readOnly true if read-only / 읽기 전용이면 true
     * @param disabled true if disabled / 비활성이면 true
     * @param maxLength the maxlength or -1 / maxlength 또는 -1
     * @param noWrap   true if the wrap attribute is off / 줄 바꿈이 꺼져 있으면 true
     * @param text     the text / 글
     * @param start    the selection start / 선택 시작
     * @param end      the selection end / 선택 끝
     * @param backward true for a backward selection / 뒤쪽 방향 선택이면 true
     * @param listener the receiver of the edits / 편집을 받는 쪽
     */
    EditSpec(Element element, ControlSnapshot control, boolean password, boolean readOnly, boolean disabled, int maxLength, boolean noWrap,
            String text, int start, int end, boolean backward, Listener listener) {
        this.element = element;
        this.control = control;
        this.password = password;
        this.multiline = control.getKind() == ControlSnapshot.Kind.TEXT_AREA;
        this.readOnly = readOnly;
        this.disabled = disabled;
        this.maxLength = maxLength;
        this.noWrap = noWrap;
        this.text = text;
        this.selectionStart = start;
        this.selectionEnd = end;
        this.backward = backward;
        this.listener = listener;
    }
}
