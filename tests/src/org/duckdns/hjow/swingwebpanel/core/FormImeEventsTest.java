package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.duckdns.hjow.swingwebpanel.dom.CompositionEvent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.InputEvent;
import org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the events of the editor of a text control while an input method composes text, with the sequence of input method events that the real
 * Korean input method of Windows sent in the real-window check (<code>tools/uicheck</code>, <code>docs/progress/uicheck.md</code>): the
 * <code>compositionend</code> event carries the committed text, a syllable that is committed in the same event as the next one starts ends one
 * composition and starts another, the <code>input</code> and key events during the composition say <code>isComposing</code>, and the
 * <code>beforeinput</code> event comes before each edit and can cancel it (except composed text). These defects were found with the real input
 * method; this test reproduces them with hand-made events.
 * <p>
 * 입력기가 글을 조합하는 동안 텍스트 컨트롤 편집기의 이벤트를 시험한다. 실제 창 확인(<code>tools/uicheck</code>, <code>docs/progress/uicheck.md</code>)에서 Windows
 * 의 실제 한국어 입력기가 보낸 입력기 이벤트 순서를 쓴다: <code>compositionend</code> 는 확정된 글을 가지고, 한 이벤트에서 음절이 확정되며 다음 음절이 시작되면 조합
 * 하나가 끝나고 다른 조합이 시작되며, 조합 중의 <code>input</code> 과 키 이벤트는 <code>isComposing</code> 이고, <code>beforeinput</code> 은 편집마다 먼저 오며
 * 편집을 취소할 수 있다(조합 중인 글은 빼고). 이 결함들은 실제 입력기로 찾았고 이 시험은 손으로 만든 이벤트로 재현한다.
 */
public class FormImeEventsTest {

    /** A page with one text field. 텍스트 필드 하나가 있는 페이지. */
    private static final String PAGE = "<!doctype html><style>html,body{margin:0} input{font:12px monospace;margin:0;display:block}</style>"
            + "<input id=name value=''>";

    /** The Hangul syllable HAN. 한글 음절 '한'. */
    private static final String HAN = "한";

    /** The Hangul syllable GEUL. 한글 음절 '글'. */
    private static final String GEUL = "글";

    /** The Hangul jamo KIYEOK. 한글 자모 'ㄱ'. */
    private static final String KIYEOK = "ㄱ";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormImeEventsTest() {
    }

    /**
     * Opens the page, focuses the field and records the composition, input and key events with their details.
     * <p>
     * 페이지를 열고 필드에 포커스를 주며 조합, 입력, 키 이벤트를 자세한 내용과 함께 기록한다.
     *
     * @param f      the fixture / 고정 장치
     * @param events receives the records / 기록을 받는다
     * @throws Exception on failure / 실패하면
     */
    private static void prepare(final FormControlIntegrationTest.Fixture f, final List<String> events) throws Exception {
        f.open(PAGE);
        f.focus("name");
        f.onDocument(new Runnable() {
            public void run() {
                Document d = f.document();
                String[] kinds = {"compositionstart", "compositionupdate", "compositionend", "beforeinput", "input", "keyup"};
                // 관심 있는 이벤트를 모두 캡처 단계에서 자세히 기록한다.
                for (int i = 0; i < kinds.length; i++) {
                    d.addEventListener(kinds[i], new EventListener() {
                        public void handleEvent(Event event) {
                            events.add(describe(event));
                        }
                    }, true);
                }
            }
        });
    }

    /**
     * Describes an event: its type, its data or input type and its <code>isComposing</code> and <code>cancelable</code> flags.
     * <p>
     * 이벤트를 설명한다: 종류, 데이터나 입력 종류, <code>isComposing</code> 과 <code>cancelable</code>.
     *
     * @param event the event / 이벤트
     * @return the description / 설명
     */
    static String describe(Event event) {
        String type = event.getType();
        if (event instanceof CompositionEvent) {
            return type + "(" + ((CompositionEvent) event).getData() + ")";
        }
        if (event instanceof InputEvent) {
            InputEvent ie = (InputEvent) event;
            return type + "(" + ie.getInputType() + "," + ie.getData() + (ie.isComposing() ? ",composing" : "") + (event.isCancelable() ? ",cancelable" : "") + ")";
        }
        if (event instanceof KeyboardEvent) {
            return type + "(" + ((KeyboardEvent) event).getKey() + (((KeyboardEvent) event).isComposing() ? ",composing" : "") + ")";
        }
        return type;
    }

    /**
     * Sends an input method event to the editor, as the input method of the system does.
     * <p>
     * 시스템 입력기처럼 편집기에 입력기 이벤트를 보낸다.
     *
     * @param f         the fixture / 고정 장치
     * @param e         the editor / 편집기
     * @param text      the whole text of the event / 이벤트의 전체 글
     * @param committed the number of committed characters at its start / 앞쪽의 확정된 문자 수
     * @throws Exception on failure / 실패하면
     */
    private static void ime(FormControlIntegrationTest.Fixture f, final JTextComponent e, final String text, final int committed) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                e.dispatchEvent(new InputMethodEvent(e, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new AttributedString(text).getIterator(), committed,
                        null, null));
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
    }

    /**
     * Returns the records whose type is one of the given types.
     * <p>
     * 종류가 주어진 것 중 하나인 기록을 반환한다.
     *
     * @param events the records / 기록
     * @param prefix the type prefix, for example <code>composition</code> / 종류 접두사(예: <code>composition</code>)
     * @return the matching records / 맞는 기록
     */
    private static List<String> only(List<String> events, String prefix) {
        List<String> out = new ArrayList<String>();
        synchronized (events) {
            // 접두사가 맞는 기록만 차례대로 모은다.
            for (String s : events) {
                if (s.startsWith(prefix)) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /**
     * Checks the composition events of the sequence that the real Korean input method sent for "han-geul": each syllable is one composition and
     * <code>compositionend</code> carries the committed syllable (it was empty before the fix).
     * <p>
     * 실제 한국어 입력기가 "한글" 에 보낸 순서의 조합 이벤트를 확인한다: 음절마다 조합 하나이고 <code>compositionend</code> 는 확정된 음절을 가진다(고치기 전에는 비어 있었다).
     *
     * @throws Exception on failure / 실패하면
     */
    public void testCompositionEndCarriesTheCommittedSyllable() throws Exception {
        FormControlIntegrationTest.Fixture f = new FormControlIntegrationTest.Fixture();
        List<String> events = Collections.synchronizedList(new ArrayList<String>());
        prepare(f, events);
        JTextComponent e = f.editor("name");
        // 실제 입력기가 보낸 순서: ㅎ, 하, 한, 한 확정, ㄱ, 그, 글, 글 확정.
        ime(f, e, "ㅎ", 0);
        ime(f, e, "하", 0);
        ime(f, e, HAN, 0);
        ime(f, e, HAN, 1);
        ime(f, e, KIYEOK, 0);
        ime(f, e, "그", 0);
        ime(f, e, GEUL, 0);
        ime(f, e, GEUL, 1);
        List<String> comp = only(events, "composition");
        List<String> expected = new ArrayList<String>();
        expected.add("compositionstart()");
        expected.add("compositionupdate(ㅎ)");
        expected.add("compositionupdate(하)");
        expected.add("compositionupdate(" + HAN + ")");
        expected.add("compositionend(" + HAN + ")");
        expected.add("compositionstart()");
        expected.add("compositionupdate(" + KIYEOK + ")");
        expected.add("compositionupdate(그)");
        expected.add("compositionupdate(" + GEUL + ")");
        expected.add("compositionend(" + GEUL + ")");
        Check.equal(expected, comp, "the composition events of two syllables");
        Check.equal(HAN + GEUL, ((HtmlInputElement) f.el("name")).getValue(), "the value is the committed text");
    }

    /**
     * Checks an input method that commits a syllable and starts the next one in the same event: the composition ends with the committed syllable
     * and a new one starts with the rest.
     * <p>
     * 한 이벤트에서 음절을 확정하고 다음 음절을 시작하는 입력기를 확인한다: 조합이 확정된 음절로 끝나고 나머지로 새 조합이 시작된다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testSyllableRolloverInOneEvent() throws Exception {
        FormControlIntegrationTest.Fixture f = new FormControlIntegrationTest.Fixture();
        List<String> events = Collections.synchronizedList(new ArrayList<String>());
        prepare(f, events);
        JTextComponent e = f.editor("name");
        ime(f, e, HAN, 0);
        ime(f, e, HAN + KIYEOK, 1);
        List<String> comp = only(events, "composition");
        Check.equal("[compositionstart(), compositionupdate(" + HAN + "), compositionend(" + HAN + "), compositionstart(), compositionupdate(" + KIYEOK + ")]",
                comp.toString(), "one event that commits and starts");
        ime(f, e, "", 0);
        comp = only(events, "composition");
        Check.equal("compositionend()", comp.get(comp.size() - 1), "a cancelled composition ends with empty data");
        Check.equal(HAN, ((HtmlInputElement) f.el("name")).getValue(), "the cancelled syllable is gone");
    }

    /**
     * Checks the commit of a syllable with the Space key as the real Korean input method of Windows sends it: an event that empties the composed
     * text and, right after it in the event queue, an event with the committed syllable. The composition ends once, with the syllable as data
     * (before the fix the end had empty data).
     * <p>
     * Windows 의 실제 한국어 입력기가 보내는 대로 Space 키로 음절을 확정하는 경우를 확인한다: 조합 중인 글을 비우는 이벤트와 이벤트 큐에서 바로 뒤의 확정된 음절
     * 이벤트. 조합은 음절을 data 로 가지고 한 번 끝난다(고치기 전에는 끝의 data 가 비어 있었다).
     *
     * @throws Exception on failure / 실패하면
     */
    public void testSpaceCommitInTwoEvents() throws Exception {
        FormControlIntegrationTest.Fixture f = new FormControlIntegrationTest.Fixture();
        List<String> events = Collections.synchronizedList(new ArrayList<String>());
        prepare(f, events);
        final JTextComponent e = f.editor("name");
        ime(f, e, GEUL, 0);
        // 두 이벤트가 이벤트 큐에 이어서 있는 것처럼 한 EDT 작업에서 차례로 보낸다.
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                e.dispatchEvent(new InputMethodEvent(e, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new AttributedString("").getIterator(), 0, null, null));
                e.dispatchEvent(new InputMethodEvent(e, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new AttributedString(GEUL).getIterator(), 1, null, null));
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        Check.equal("[compositionstart(), compositionupdate(" + GEUL + "), compositionend(" + GEUL + ")]", only(events, "composition").toString(),
                "the end of the composition carries the syllable");
        Check.equal(GEUL, ((HtmlInputElement) f.el("name")).getValue(), "the value is the syllable");
    }

    /**
     * Checks that the <code>input</code> and <code>beforeinput</code> events of composed text say <code>isComposing</code> (the
     * <code>beforeinput</code> one cannot be cancelled) and that a key that is released during the composition says <code>isComposing</code> too.
     * <p>
     * 조합 중인 글의 <code>input</code> 과 <code>beforeinput</code> 이벤트가 <code>isComposing</code> 인지(<code>beforeinput</code> 은 취소할 수 없음),
     * 조합 중에 뗀 키도 <code>isComposing</code> 인지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testComposedTextEventsSayComposing() throws Exception {
        final FormControlIntegrationTest.Fixture f = new FormControlIntegrationTest.Fixture();
        List<String> events = Collections.synchronizedList(new ArrayList<String>());
        prepare(f, events);
        final JTextComponent e = f.editor("name");
        ime(f, e, HAN, 0);
        Check.equal("[beforeinput(insertCompositionText," + HAN + ",composing)]", only(events, "beforeinput").toString(), "beforeinput of composed text");
        Check.equal("[input(insertCompositionText," + HAN + ",composing)]", only(events, "input").toString(), "input of composed text");
        SwingUtilities.invokeAndWait(new Runnable() {
            // 시험 환경에는 AWT 포커스가 없어 키 이벤트를 편집기에 보내면 버려지므로 편집기의 키 리스너(페이지 키보드)를 직접 부른다.
            public void run() {
                f.keyboard.keyReleased(new KeyEvent(e, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_S, 's'));
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        Check.equal("[keyup(s,composing)]", only(events, "keyup").toString(), "a key released while composing");
        ime(f, e, HAN, 1);
        SwingUtilities.invokeAndWait(new Runnable() {
            // 시험 환경에는 AWT 포커스가 없어 키 이벤트를 편집기에 보내면 버려지므로 편집기의 키 리스너(페이지 키보드)를 직접 부른다.
            public void run() {
                f.keyboard.keyReleased(new KeyEvent(e, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_S, 's'));
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        Check.equal("keyup(s)", only(events, "keyup").get(1), "a key released after the commit");
    }

    /**
     * Checks that a typed edit fires a cancelable <code>beforeinput</code> before <code>input</code>, and that a cancelled one leaves the DOM value
     * as it was and puts that value back into the editor.
     * <p>
     * 입력한 편집이 <code>input</code> 앞에 취소할 수 있는 <code>beforeinput</code> 을 발생시키는지, 취소된 편집은 DOM 값을 그대로 두고 그 값을 편집기에 되돌리는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testBeforeInputComesFirstAndCanCancel() throws Exception {
        final FormControlIntegrationTest.Fixture f = new FormControlIntegrationTest.Fixture();
        List<String> events = Collections.synchronizedList(new ArrayList<String>());
        prepare(f, events);
        f.onDocument(new Runnable() {
            public void run() {
                f.el("name").addEventListener("beforeinput", new EventListener() {
                    public void handleEvent(Event event) {
                        // 'X' 를 넣는 편집만 막는다.
                        if ("X".equals(((InputEvent) event).getData())) {
                            event.preventDefault();
                        }
                    }
                }, false);
            }
        });
        JTextComponent e = f.editor("name");
        FormControlIntegrationTest.type(f, e, 0, "a");
        List<String> edits = new ArrayList<String>(only(events, "beforeinput"));
        edits.addAll(only(events, "input("));
        Check.equal("[beforeinput(insertText,a,cancelable), input(insertText,a)]", edits.toString(), "beforeinput and input of a typed letter");
        Check.isTrue(events.indexOf("beforeinput(insertText,a,cancelable)") < events.indexOf("input(insertText,a)"), "beforeinput comes first");
        events.clear();
        FormControlIntegrationTest.type(f, e, 1, "X");
        TestSupport.flushEdt();
        Check.equal("a", ((HtmlInputElement) f.el("name")).getValue(), "the cancelled edit did not reach the DOM");
        Check.equal("a", e.getText(), "the editor shows the DOM value again");
        Check.equal("[beforeinput(insertText,X,cancelable)]", only(events, "beforeinput").toString(), "the cancelled beforeinput");
        Check.equal(0, only(events, "input(").size(), "no input event for a cancelled edit");
        FormControlIntegrationTest.type(f, e, 1, "b");
        Check.equal("ab", ((HtmlInputElement) f.el("name")).getValue(), "the next edit goes through");
    }
}
