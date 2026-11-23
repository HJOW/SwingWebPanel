package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.TextHitInfo;
import java.awt.geom.Rectangle2D;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.render.SelectionHighlight;

/**
 * Text input by an input method into an editing host of the page (plan item P20-06), for languages that assemble characters before they
 * are final, such as Korean (Hangul letters become a syllable) and Japanese. The component registers it as its input method listener. An
 * <code>InputMethodEvent</code> carries the text that the input method has committed and the text that it still composes; this class passes
 * both to the editing engine ({@link PageEditing#compose}), which shows the composed text in the DOM and sends the composition events and
 * the <code>insertCompositionText</code> input events. It also answers the questions of the input method about where the text is (the
 * caret of the page), so that the window of the candidates opens next to the caret. When no editing host has the focus, the component
 * switches the input methods off, so this listener is not called. Form controls have their own Swing editors with their own composition.
 * The listener runs on the event dispatch thread; the work goes to the document queue.
 * <p>
 * 입력기가 페이지의 편집 호스트에 하는 텍스트 입력(계획 P20-06). 글자를 확정하기 전에 조립하는 언어(한글은 음절로 조립되고, 일본어 등)를 위한 것이다. 컴포넌트가 이것을
 * 입력기 리스너로 등록한다. <code>InputMethodEvent</code> 는 입력기가 확정한 텍스트와 아직 조합 중인 텍스트를 나르며, 이 클래스는 둘을 편집 엔진
 * ({@link PageEditing#compose})에 넘긴다. 엔진은 조합 중인 텍스트를 DOM 에 보이고 조합 이벤트와 <code>insertCompositionText</code> 입력 이벤트를 보낸다. 또 텍스트가
 * 어디 있는지에 대한 입력기의 질문(페이지의 캐럿)에 답해, 후보 창이 캐럿 옆에 열리게 한다. 편집 호스트가 포커스를 가지지 않으면 컴포넌트가 입력기를 끄므로 이
 * 리스너는 불리지 않는다. 폼 컨트롤은 자기 조합을 가진 자기 Swing 편집기가 있다. 리스너는 이벤트 디스패치 스레드에서 실행되며 일은 문서 큐로 간다.
 */
public final class PageComposition implements InputMethodListener {

    /**
     * The Swing component that has the keyboard focus.
     * <p>
     * 키보드 포커스를 가진 Swing 컴포넌트.
     */
    private final JComponent component;

    /**
     * The view, for the zoom.
     * <p>
     * 확대 배율을 아는 뷰.
     */
    private final PageView view;

    /**
     * The keyboard controller, which finds the focused element on the document queue.
     * <p>
     * 문서 큐에서 포커스를 가진 요소를 찾는 키보드 제어기.
     */
    private final PageKeyboard keyboard;

    /**
     * The editing controller, which applies the text.
     * <p>
     * 텍스트를 적용하는 편집 제어기.
     */
    private final PageEditing editing;

    /**
     * The selection controller, which knows where the caret is.
     * <p>
     * 캐럿이 어디 있는지 아는 선택 제어기.
     */
    private final PageSelection selection;

    /**
     * The session, for the current page.
     * <p>
     * 현재 페이지를 아는 세션.
     */
    private final BrowserSession session;

    /**
     * What the input method may ask the component.
     * <p>
     * 입력기가 컴포넌트에 물을 수 있는 것.
     */
    private final InputMethodRequests requests = new Requests();

    /**
     * True while a composition is open (written on the event dispatch thread).
     * <p>
     * 조합이 열려 있는 동안 true(이벤트 디스패치 스레드에서 쓴다).
     */
    private boolean composing;

    /**
     * True if an event emptied the composed text and applying it waits one turn of the event queue for a commit that may follow (written on
     * the event dispatch thread). The Korean input method of Windows commits a syllable with the Space key in two events, an empty
     * composition and then the committed text; applying them together ends the composition once with the syllable as data.
     * <p>
     * 이벤트가 조합 중인 텍스트를 비웠고 그 적용이 뒤따를 수 있는 확정을 이벤트 큐 한 바퀴 동안 기다리면 true(이벤트 디스패치 스레드에서 쓴다). Windows 의 한국어
     * 입력기는 Space 키로 음절을 확정할 때 빈 조합과 확정된 텍스트를 두 이벤트로 보낸다. 함께 적용하면 조합이 음절을 data 로 한 번 끝난다.
     */
    private boolean emptyPending;

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
     * @param component the component / 컴포넌트
     * @param session   the session / 세션
     * @param view      the view / 뷰
     * @param keyboard  the keyboard controller / 키보드 제어기
     * @param editing   the editing controller / 편집 제어기
     * @param selection the selection controller / 선택 제어기
     */
    public PageComposition(JComponent component, BrowserSession session, PageView view, PageKeyboard keyboard, PageEditing editing,
            PageSelection selection) {
        this.component = component;
        this.session = session;
        this.view = view;
        this.keyboard = keyboard;
        this.editing = editing;
        this.selection = selection;
    }

    /**
     * Returns the answers that the input method needs (for <code>getInputMethodRequests()</code> of the component).
     * <p>
     * 입력기가 필요로 하는 답을 반환한다(컴포넌트의 <code>getInputMethodRequests()</code> 용).
     *
     * @return the requests object / 질의 객체
     */
    public InputMethodRequests getRequests() {
        return requests;
    }

    /**
     * Stops the input method work (the component was disposed).
     * <p>
     * 입력기 일을 멈춘다(컴포넌트가 폐기되었다).
     */
    public void dispose() {
        disposed = true;
    }

    /**
     * Tells the input method to finish the composition that runs (it commits the composed text): called before a mouse press moves the
     * caret, so that the text stays where it was composed.
     * <p>
     * 입력기에 진행 중인 조합을 끝내라고 알린다(조합 중인 텍스트를 확정한다). 마우스 누름이 캐럿을 옮기기 전에 불러, 텍스트가 조합하던 자리에 남게 한다.
     */
    public void finish() {
        if (composing && component.getInputContext() != null) {
            component.getInputContext().endComposition();
        }
    }

    /**
     * The text of the input method changed: the committed text and the composed text go to the editing engine on the document queue.
     * <p>
     * 입력기의 텍스트가 바뀌었다: 확정된 텍스트와 조합 중인 텍스트가 문서 큐에서 편집 엔진으로 간다.
     *
     * @param event the event / 이벤트
     */
    public void inputMethodTextChanged(InputMethodEvent event) {
        event.consume();
        if (disposed) {
            return;
        }
        AttributedCharacterIterator it = event.getText();
        int total = it == null ? 0 : it.getEndIndex() - it.getBeginIndex();
        int count = Math.min(event.getCommittedCharacterCount(), total);
        StringBuilder done = new StringBuilder();
        StringBuilder going = new StringBuilder();
        // 앞의 확정된 글자와 뒤의 조합 중인 글자를 나누어 모은다.
        for (int i = 0; i < total; i++) {
            it.setIndex(it.getBeginIndex() + i);
            (i < count ? done : going).append(it.current());
        }
        boolean was = composing;
        boolean pending = emptyPending;
        emptyPending = false;
        composing = going.length() > 0;
        // 조합 중인 동안 도착하는 키 이벤트가 isComposing 을 알리게 한다.
        keyboard.setComposing(composing);
        // 확정 없이 조합을 비우는 이벤트는 한 바퀴 미뤄, 바로 뒤에 확정 이벤트가 오면 그것만 적용한다(조합이 확정된 글로 한 번 끝나게).
        if (was && !pending && done.length() == 0 && going.length() == 0) {
            emptyPending = true;
            SwingUtilities.invokeLater(new Runnable() {
                /** Applies the emptied composition if no commit came. / 확정이 오지 않았으면 비운 조합을 적용한다. */
                public void run() {
                    if (emptyPending && !disposed) {
                        emptyPending = false;
                        apply("", "");
                    }
                }
            });
            return;
        }
        apply(done.toString(), going.toString());
    }

    /**
     * Gives committed and composed text to the editing engine on the document queue, for the element that has the focus.
     * <p>
     * 확정된 텍스트와 조합 중인 텍스트를 문서 큐에서 포커스를 가진 요소의 편집 엔진에 준다.
     *
     * @param committed the committed text / 확정된 텍스트
     * @param composed  the composed text / 조합 중인 텍스트
     */
    private void apply(final String committed, final String composed) {
        final PageSnapshot snapshot = session.getSnapshot();
        keyboard.queueForFocused(snapshot, new PageKeyboard.FocusedTask() {
            /** Applies the text on the document queue. / 문서 큐에서 텍스트를 적용한다. */
            public void run(PageSnapshot page, Document doc, Element target) {
                editing.compose(page, doc, target, committed, composed);
            }
        });
    }

    /**
     * The caret of the input method moved inside the composed text (the page shows the caret at the end of the composed text).
     * <p>
     * 입력기의 캐럿이 조합 중인 텍스트 안에서 움직였다(페이지는 캐럿을 조합 중인 텍스트의 끝에 보인다).
     *
     * @param event the event / 이벤트
     */
    public void caretPositionChanged(InputMethodEvent event) {
        event.consume();
    }

    /**
     * What the input method may ask: where the text is, and what text there is (the page offers none to the input method, which composes
     * without the surrounding text).
     * <p>
     * 입력기가 물을 수 있는 것: 텍스트가 어디 있는지, 어떤 텍스트가 있는지(페이지는 입력기에 주변 텍스트를 주지 않으며 입력기는 주변 텍스트 없이 조합한다).
     */
    private final class Requests implements InputMethodRequests {

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         */
        Requests() {
        }

        /**
         * Returns the place of the caret on the screen, where the window of the input method opens.
         * <p>
         * 입력기의 창이 열리는 곳인 캐럿의 화면 위치를 반환한다.
         *
         * @param offset the offset in the text (ignored) / 텍스트 안의 오프셋(무시한다)
         * @return the rectangle in screen coordinates / 화면 좌표의 사각형
         */
        public Rectangle getTextLocation(TextHitInfo offset) {
            Point origin;
            try {
                origin = component.getLocationOnScreen();
            } catch (java.awt.IllegalComponentStateException e) {
                // 화면에 보이지 않는 컴포넌트는 화면 위치가 없으므로 원점을 쓴다.
                origin = new Point(0, 0);
            }
            SelectionHighlight caret = selection.getCaret();
            double zoom = view.getZoom();
            if (caret == null) {
                return new Rectangle(origin.x, origin.y, 0, 16);
            }
            Rectangle2D bar = caret.getBounds();
            return new Rectangle(origin.x + (int) Math.round(bar.getX() * zoom), origin.y + (int) Math.round(bar.getY() * zoom), 1,
                    Math.max(1, (int) Math.round(bar.getHeight() * zoom)));
        }

        /**
         * There is no text under a point that the page could report.
         * <p>
         * 페이지가 알릴 수 있는 점 아래의 텍스트는 없다.
         *
         * @param x the x on the screen / 화면의 x
         * @param y the y on the screen / 화면의 y
         * @return null / null
         */
        public TextHitInfo getLocationOffset(int x, int y) {
            return null;
        }

        /**
         * Returns the insert position in the surrounding text (the page gives no surrounding text, so it is 0).
         * <p>
         * 주변 텍스트 안의 삽입 위치를 반환한다(페이지는 주변 텍스트를 주지 않으므로 0).
         *
         * @return 0 / 0
         */
        public int getInsertPositionOffset() {
            return 0;
        }

        /**
         * Returns no committed text.
         * <p>
         * 확정된 텍스트가 없다고 답한다.
         *
         * @param beginIndex the first index / 첫 인덱스
         * @param endIndex   the end index / 끝 인덱스
         * @param attributes the attributes that are wanted / 원하는 속성
         * @return an empty iterator / 빈 반복자
         */
        public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
            return new AttributedString("").getIterator();
        }

        /**
         * Returns the length of the committed text (0).
         * <p>
         * 확정된 텍스트의 길이를 반환한다(0).
         *
         * @return 0 / 0
         */
        public int getCommittedTextLength() {
            return 0;
        }

        /**
         * The page cannot take back committed text.
         * <p>
         * 페이지는 확정한 텍스트를 되돌릴 수 없다.
         *
         * @param attributes the attributes that are wanted / 원하는 속성
         * @return null / null
         */
        public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
            return null;
        }

        /**
         * The page offers no selected text to the input method.
         * <p>
         * 페이지는 입력기에 선택된 텍스트를 주지 않는다.
         *
         * @param attributes the attributes that are wanted / 원하는 속성
         * @return an empty iterator / 빈 반복자
         */
        public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
            return new AttributedString("").getIterator();
        }
    }
}
