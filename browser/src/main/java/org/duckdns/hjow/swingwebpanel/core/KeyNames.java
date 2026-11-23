package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.KeyEvent;

/**
 * The names that DOM keyboard events use for the keys of AWT: the <code>key</code> value, the physical <code>code</code>, the legacy
 * <code>keyCode</code> and the location. Only keys whose names are the same on every keyboard layout are named; a printable key is
 * identified by the character that it produces.
 * <p>
 * DOM 키보드 이벤트가 AWT 키에 쓰는 이름: <code>key</code> 값, 물리적 <code>code</code>, 예전 <code>keyCode</code>, 위치. 키보드 배열과 상관없이 이름이
 * 같은 키만 이름을 붙이고, 글자가 나오는 키는 그 글자로 식별한다.
 */
final class KeyNames {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private KeyNames() {
    }

    /**
     * Returns the DOM <code>key</code> value of a key.
     * <p>
     * 키의 DOM <code>key</code> 값을 반환한다.
     *
     * @param vk the AWT virtual key code / AWT 가상 키 코드
     * @param ch the character that the key produced, or <code>KeyEvent.CHAR_UNDEFINED</code> / 키가 만든 문자. 없으면 <code>KeyEvent.CHAR_UNDEFINED</code>
     * @return the value / 값
     */
    static String key(int vk, char ch) {
        switch (vk) {
        case KeyEvent.VK_ENTER:
            return "Enter";
        case KeyEvent.VK_TAB:
            return "Tab";
        case KeyEvent.VK_BACK_SPACE:
            return "Backspace";
        case KeyEvent.VK_DELETE:
            return "Delete";
        case KeyEvent.VK_ESCAPE:
            return "Escape";
        case KeyEvent.VK_LEFT:
            return "ArrowLeft";
        case KeyEvent.VK_RIGHT:
            return "ArrowRight";
        case KeyEvent.VK_UP:
            return "ArrowUp";
        case KeyEvent.VK_DOWN:
            return "ArrowDown";
        case KeyEvent.VK_HOME:
            return "Home";
        case KeyEvent.VK_END:
            return "End";
        case KeyEvent.VK_PAGE_UP:
            return "PageUp";
        case KeyEvent.VK_PAGE_DOWN:
            return "PageDown";
        case KeyEvent.VK_INSERT:
            return "Insert";
        case KeyEvent.VK_SHIFT:
            return "Shift";
        case KeyEvent.VK_CONTROL:
            return "Control";
        case KeyEvent.VK_ALT:
            return "Alt";
        case KeyEvent.VK_META:
        case KeyEvent.VK_WINDOWS:
            return "Meta";
        case KeyEvent.VK_CAPS_LOCK:
            return "CapsLock";
        case KeyEvent.VK_NUM_LOCK:
            return "NumLock";
        case KeyEvent.VK_SCROLL_LOCK:
            return "ScrollLock";
        case KeyEvent.VK_PAUSE:
            return "Pause";
        case KeyEvent.VK_PRINTSCREEN:
            return "PrintScreen";
        case KeyEvent.VK_CONTEXT_MENU:
            return "ContextMenu";
        case KeyEvent.VK_SPACE:
            return " ";
        default:
            break;
        }
        if (vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F12) {
            return "F" + (vk - KeyEvent.VK_F1 + 1);
        }
        if (ch != KeyEvent.CHAR_UNDEFINED && ch >= 32 && ch != 127) {
            return String.valueOf(ch);
        }
        return "Unidentified";
    }

    /**
     * Returns the DOM <code>code</code> (the physical key) of a key.
     * <p>
     * 키의 DOM <code>code</code>(물리적 키)를 반환한다.
     *
     * @param vk       the AWT virtual key code / AWT 가상 키 코드
     * @param location the DOM key location (0 standard, 1 left, 2 right, 3 numpad) / DOM 키 위치(0 표준, 1 왼쪽, 2 오른쪽, 3 숫자 패드)
     * @return the code, or "" if the key has no standard name / 코드. 표준 이름이 없으면 ""
     */
    static String code(int vk, int location) {
        if (vk >= KeyEvent.VK_A && vk <= KeyEvent.VK_Z) {
            return "Key" + (char) vk;
        }
        if (vk >= KeyEvent.VK_0 && vk <= KeyEvent.VK_9) {
            return "Digit" + (char) vk;
        }
        if (vk >= KeyEvent.VK_NUMPAD0 && vk <= KeyEvent.VK_NUMPAD9) {
            return "Numpad" + (vk - KeyEvent.VK_NUMPAD0);
        }
        if (vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F12) {
            return "F" + (vk - KeyEvent.VK_F1 + 1);
        }
        // F13 부터 F24 까지는 AWT 에서 다른 범위의 값이다.
        if (vk >= KeyEvent.VK_F13 && vk <= KeyEvent.VK_F24) {
            return "F" + (vk - KeyEvent.VK_F13 + 13);
        }
        String side = location == 2 ? "Right" : "Left";
        switch (vk) {
        case KeyEvent.VK_ADD:
            return "NumpadAdd";
        case KeyEvent.VK_SUBTRACT:
            return "NumpadSubtract";
        case KeyEvent.VK_MULTIPLY:
            return "NumpadMultiply";
        case KeyEvent.VK_DIVIDE:
            return "NumpadDivide";
        case KeyEvent.VK_DECIMAL:
            return "NumpadDecimal";
        case KeyEvent.VK_SEPARATOR:
            return "NumpadComma";
        case KeyEvent.VK_NUM_LOCK:
            return "NumLock";
        case KeyEvent.VK_SCROLL_LOCK:
            return "ScrollLock";
        case KeyEvent.VK_PRINTSCREEN:
            return "PrintScreen";
        case KeyEvent.VK_PAUSE:
            return "Pause";
        case KeyEvent.VK_CONTEXT_MENU:
            return "ContextMenu";
        case KeyEvent.VK_ALT_GRAPH:
            return "AltRight";
        case KeyEvent.VK_ENTER:
            return location == 3 ? "NumpadEnter" : "Enter";
        case KeyEvent.VK_TAB:
            return "Tab";
        case KeyEvent.VK_BACK_SPACE:
            return "Backspace";
        case KeyEvent.VK_DELETE:
            return "Delete";
        case KeyEvent.VK_ESCAPE:
            return "Escape";
        case KeyEvent.VK_SPACE:
            return "Space";
        case KeyEvent.VK_LEFT:
            return "ArrowLeft";
        case KeyEvent.VK_RIGHT:
            return "ArrowRight";
        case KeyEvent.VK_UP:
            return "ArrowUp";
        case KeyEvent.VK_DOWN:
            return "ArrowDown";
        case KeyEvent.VK_HOME:
            return "Home";
        case KeyEvent.VK_END:
            return "End";
        case KeyEvent.VK_PAGE_UP:
            return "PageUp";
        case KeyEvent.VK_PAGE_DOWN:
            return "PageDown";
        case KeyEvent.VK_INSERT:
            return "Insert";
        case KeyEvent.VK_SHIFT:
            return "Shift" + side;
        case KeyEvent.VK_CONTROL:
            return "Control" + side;
        case KeyEvent.VK_ALT:
            return "Alt" + side;
        case KeyEvent.VK_META:
        case KeyEvent.VK_WINDOWS:
            return "Meta" + side;
        case KeyEvent.VK_CAPS_LOCK:
            return "CapsLock";
        case KeyEvent.VK_COMMA:
            return "Comma";
        case KeyEvent.VK_PERIOD:
            return "Period";
        case KeyEvent.VK_SLASH:
            return "Slash";
        case KeyEvent.VK_SEMICOLON:
            return "Semicolon";
        case KeyEvent.VK_EQUALS:
            return "Equal";
        case KeyEvent.VK_MINUS:
            return "Minus";
        case KeyEvent.VK_BACK_QUOTE:
            return "Backquote";
        case KeyEvent.VK_OPEN_BRACKET:
            return "BracketLeft";
        case KeyEvent.VK_CLOSE_BRACKET:
            return "BracketRight";
        case KeyEvent.VK_BACK_SLASH:
            return "Backslash";
        case KeyEvent.VK_QUOTE:
            return "Quote";
        default:
            return "";
        }
    }

    /**
     * Returns the legacy <code>keyCode</code> of a key: the value that browsers report, which differs from the AWT code for some keys.
     * <p>
     * 키의 예전 <code>keyCode</code> 를 반환한다. 브라우저가 알리는 값이며 일부 키는 AWT 코드와 다르다.
     *
     * @param vk the AWT virtual key code / AWT 가상 키 코드
     * @return the legacy code / 예전 코드
     */
    static int legacyKeyCode(int vk) {
        switch (vk) {
        case KeyEvent.VK_ENTER:
            return 13;
        case KeyEvent.VK_DELETE:
            return 46;
        case KeyEvent.VK_INSERT:
            return 45;
        case KeyEvent.VK_META:
        case KeyEvent.VK_WINDOWS:
            return 91;
        case KeyEvent.VK_COMMA:
            return 188;
        case KeyEvent.VK_PERIOD:
            return 190;
        case KeyEvent.VK_SLASH:
            return 191;
        case KeyEvent.VK_SEMICOLON:
            return 186;
        case KeyEvent.VK_EQUALS:
            return 187;
        case KeyEvent.VK_MINUS:
            return 189;
        case KeyEvent.VK_OPEN_BRACKET:
            return 219;
        case KeyEvent.VK_BACK_SLASH:
            return 220;
        case KeyEvent.VK_CLOSE_BRACKET:
            return 221;
        default:
            return vk > 255 ? 0 : vk;
        }
    }

    /**
     * Converts an AWT key location to the DOM location (0 standard, 1 left, 2 right, 3 numpad).
     * <p>
     * AWT 키 위치를 DOM 위치(0 표준, 1 왼쪽, 2 오른쪽, 3 숫자 패드)로 바꾼다.
     *
     * @param awtLocation the AWT location / AWT 위치
     * @return the DOM location / DOM 위치
     */
    static int location(int awtLocation) {
        switch (awtLocation) {
        case KeyEvent.KEY_LOCATION_LEFT:
            return 1;
        case KeyEvent.KEY_LOCATION_RIGHT:
            return 2;
        case KeyEvent.KEY_LOCATION_NUMPAD:
            return 3;
        default:
            return 0;
        }
    }
}
