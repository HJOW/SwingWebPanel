package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.KeyEvent;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the <code>code</code> (the physical key) and <code>isComposing</code> of keyboard events (UI Events KeyboardEvent code Values, W3C Working Draft,
 * Last-Modified 21 April 2025, fetched 2026-11-18; UI Events; see <code>docs/progress/webapi2.md</code>): the table that turns the AWT key codes into the names
 * of the specification, and, with a real component and real AWT key events, the events that the page hears: <code>key</code>, <code>code</code>,
 * <code>location</code> and <code>isComposing</code>, which is true while an input method composes text.
 * <p>
 * 키보드 이벤트의 <code>code</code>(물리적 키)와 <code>isComposing</code> 을 시험한다(UI Events KeyboardEvent code Values W3C Working Draft, Last-Modified 2025-04-21,
 * 2026-11-18 조회, UI Events, <code>docs/progress/webapi2.md</code> 참조): AWT 키 코드를 규격의 이름으로 바꾸는 표와, 실제 컴포넌트와 실제 AWT 키 이벤트로 페이지가
 * 듣는 이벤트의 <code>key</code>, <code>code</code>, <code>location</code>, <code>isComposing</code>(입력기가 텍스트를 조합하는 동안 true).
 */
public class KeyboardCodeTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public KeyboardCodeTest() {
    }

    /**
     * Returns the names that the table gives, for key codes and locations written as <code>vk/location</code> pairs.
     * <p>
     * <code>vk/위치</code> 쌍으로 쓴 키 코드와 위치에 표가 주는 이름을 반환한다.
     *
     * @param keys the pairs / 쌍
     * @return the names joined with commas / 쉼표로 이은 이름
     */
    private static String codes(int[][] keys) {
        StringBuilder out = new StringBuilder();
        // 키마다 표에서 이름을 찾아 이어 붙인다.
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(KeyNames.code(keys[i][0], keys[i][1]));
        }
        return out.toString();
    }

    /**
     * The table of codes: letters, digits, the function keys (also F13 to F24), the keys of the main block, the arrows and the keys of the control pad, the modifier
     * keys with their side, the numpad and the keys that have no name.
     * <p>
     * 코드 표: 글자, 숫자, 기능 키(F13~F24 포함), 본체 키, 화살표와 제어 패드 키, 위치에 따른 보조 키, 숫자 패드, 이름이 없는 키.
     */
    public void testCodeTable() {
        Check.equal("KeyA,KeyZ,Digit0,Digit9", codes(new int[][] { { KeyEvent.VK_A, 0 }, { KeyEvent.VK_Z, 0 }, { KeyEvent.VK_0, 0 }, { KeyEvent.VK_9, 0 } }), "letters and digits");
        Check.equal("F1,F12,F13,F24", codes(new int[][] { { KeyEvent.VK_F1, 0 }, { KeyEvent.VK_F12, 0 }, { KeyEvent.VK_F13, 0 }, { KeyEvent.VK_F24, 0 } }), "function keys");
        Check.equal("Enter,Tab,Backspace,Delete,Escape,Space,CapsLock,ContextMenu,PrintScreen,ScrollLock,Pause,Insert", codes(new int[][] { { KeyEvent.VK_ENTER, 0 }, { KeyEvent.VK_TAB, 0 },
            { KeyEvent.VK_BACK_SPACE, 0 }, { KeyEvent.VK_DELETE, 0 }, { KeyEvent.VK_ESCAPE, 0 }, { KeyEvent.VK_SPACE, 0 }, { KeyEvent.VK_CAPS_LOCK, 0 },
            { KeyEvent.VK_CONTEXT_MENU, 0 }, { KeyEvent.VK_PRINTSCREEN, 0 }, { KeyEvent.VK_SCROLL_LOCK, 0 }, { KeyEvent.VK_PAUSE, 0 }, { KeyEvent.VK_INSERT, 0 } }), "named keys");
        Check.equal("ArrowLeft,ArrowRight,ArrowUp,ArrowDown,Home,End,PageUp,PageDown", codes(new int[][] { { KeyEvent.VK_LEFT, 0 }, { KeyEvent.VK_RIGHT, 0 }, { KeyEvent.VK_UP, 0 },
            { KeyEvent.VK_DOWN, 0 }, { KeyEvent.VK_HOME, 0 }, { KeyEvent.VK_END, 0 }, { KeyEvent.VK_PAGE_UP, 0 }, { KeyEvent.VK_PAGE_DOWN, 0 } }), "arrows and the control pad");
        Check.equal("ShiftLeft,ShiftRight,ControlLeft,ControlRight,AltLeft,AltRight,AltRight,MetaLeft", codes(new int[][] { { KeyEvent.VK_SHIFT, 1 }, { KeyEvent.VK_SHIFT, 2 },
            { KeyEvent.VK_CONTROL, 1 }, { KeyEvent.VK_CONTROL, 2 }, { KeyEvent.VK_ALT, 1 }, { KeyEvent.VK_ALT, 2 }, { KeyEvent.VK_ALT_GRAPH, 0 }, { KeyEvent.VK_META, 1 } }),
                "the modifier keys follow their side; AltGraph is the right Alt key");
        Check.equal("Numpad0,Numpad9,NumpadAdd,NumpadSubtract,NumpadMultiply,NumpadDivide,NumpadDecimal,NumpadEnter,NumpadComma,NumLock", codes(new int[][] { { KeyEvent.VK_NUMPAD0, 3 },
            { KeyEvent.VK_NUMPAD9, 3 }, { KeyEvent.VK_ADD, 3 }, { KeyEvent.VK_SUBTRACT, 3 }, { KeyEvent.VK_MULTIPLY, 3 }, { KeyEvent.VK_DIVIDE, 3 }, { KeyEvent.VK_DECIMAL, 3 },
            { KeyEvent.VK_ENTER, 3 }, { KeyEvent.VK_SEPARATOR, 3 }, { KeyEvent.VK_NUM_LOCK, 0 } }), "the numpad");
        Check.equal("Comma,Period,Slash,Semicolon,Equal,Minus,Backquote,BracketLeft,BracketRight,Backslash,Quote", codes(new int[][] { { KeyEvent.VK_COMMA, 0 }, { KeyEvent.VK_PERIOD, 0 },
            { KeyEvent.VK_SLASH, 0 }, { KeyEvent.VK_SEMICOLON, 0 }, { KeyEvent.VK_EQUALS, 0 }, { KeyEvent.VK_MINUS, 0 }, { KeyEvent.VK_BACK_QUOTE, 0 }, { KeyEvent.VK_OPEN_BRACKET, 0 },
            { KeyEvent.VK_CLOSE_BRACKET, 0 }, { KeyEvent.VK_BACK_SLASH, 0 }, { KeyEvent.VK_QUOTE, 0 } }), "punctuation");
        Check.equal(",", codes(new int[][] { { KeyEvent.VK_UNDEFINED, 0 }, { KeyEvent.VK_KANJI, 0 } }), "a key that has no name has the empty code");
    }

    /**
     * The page hears <code>key</code>, <code>code</code>, <code>isComposing</code> and <code>location</code> of real key events: false while no input method
     * composes, true for the keys that arrive while it composes, false again once it committed.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageHearsCodeAndIsComposing() throws Exception {
        EditingFixture.Panel panel = EditingFixture.create(400, 300);
        try {
            EditingFixture.open(panel, EditingFixture.page("document.addEventListener('keydown', function (e) { put('keys', (sessionStorage.getItem('keys') || '') + "
                    + "e.key + ':' + e.code + ':' + e.isComposing + ':' + e.location + '|'); }, true);"));
            EditingFixture.keyEvent(panel, KeyEvent.KEY_PRESSED, KeyEvent.VK_A, 'a', 0);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_RELEASED, KeyEvent.VK_A, 'a', 0);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n', 0);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_RELEASED, KeyEvent.VK_ENTER, '\n', 0);
            EditingFixture.awaitContains(panel, "keys", "Enter:Enter:false:0|");
            Check.equal("a:KeyA:false:0|Enter:Enter:false:0|", EditingFixture.stored(panel, "keys"), "plain keys: the code is the physical key and no composition is going on");
            // 입력기가 조합을 시작하면 그동안 도착하는 키 이벤트는 isComposing 이다.
            EditingFixture.inputMethod(panel, "", "ㅇ");
            EditingFixture.keyEvent(panel, KeyEvent.KEY_PRESSED, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, 0);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_RELEASED, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, 0);
            EditingFixture.awaitContains(panel, "keys", "ArrowLeft:ArrowLeft:true:0|");
            // 조합이 확정되면 다시 false 이다.
            EditingFixture.inputMethod(panel, "아", "");
            EditingFixture.keyEvent(panel, KeyEvent.KEY_PRESSED, KeyEvent.VK_B, 'b', 0);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_RELEASED, KeyEvent.VK_B, 'b', 0);
            EditingFixture.awaitContains(panel, "keys", "b:KeyB:false:0|");
        } finally {
            EditingFixture.close(panel);
        }
    }
}
