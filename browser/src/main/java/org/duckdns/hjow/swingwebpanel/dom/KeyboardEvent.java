package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A keyboard event (UI Events): the value of the key (<code>key</code>), the physical key (<code>code</code>), where on the keyboard it
 * is, whether it repeats, and the legacy <code>keyCode</code>, <code>charCode</code> and <code>which</code> that old scripts read.
 * <p>
 * 키보드 이벤트(UI Events): 키의 값(<code>key</code>), 물리적 키(<code>code</code>), 키보드의 위치, 반복 여부, 오래된 스크립트가 읽는 예전
 * <code>keyCode</code>, <code>charCode</code>, <code>which</code>.
 */
public class KeyboardEvent extends UiEvent {

    /**
     * Location: the standard part of the keyboard.
     * <p>
     * 위치: 키보드의 표준 부분.
     */
    public static final int DOM_KEY_LOCATION_STANDARD = 0;

    /**
     * Location: the left key of a pair (Shift, Control ...).
     * <p>
     * 위치: 쌍을 이루는 키(Shift, Control 등)의 왼쪽.
     */
    public static final int DOM_KEY_LOCATION_LEFT = 1;

    /**
     * Location: the right key of a pair.
     * <p>
     * 위치: 쌍을 이루는 키의 오른쪽.
     */
    public static final int DOM_KEY_LOCATION_RIGHT = 2;

    /**
     * Location: the numeric keypad.
     * <p>
     * 위치: 숫자 키패드.
     */
    public static final int DOM_KEY_LOCATION_NUMPAD = 3;

    /**
     * The value of the key such as <code>a</code>, <code>Enter</code>, <code>ArrowLeft</code>.
     * <p>
     * <code>a</code>, <code>Enter</code>, <code>ArrowLeft</code> 같은 키의 값.
     */
    private String key = "";

    /**
     * The physical key such as <code>KeyA</code>, <code>Enter</code>.
     * <p>
     * <code>KeyA</code>, <code>Enter</code> 같은 물리적 키.
     */
    private String code = "";

    /**
     * The location of the key.
     * <p>
     * 키의 위치.
     */
    private int location;

    /**
     * True while the key is held down and repeats.
     * <p>
     * 키를 누르고 있어 반복되는 중이면 true.
     */
    private boolean repeat;

    /**
     * True while an input method composes text.
     * <p>
     * 입력기가 텍스트를 조합하는 중이면 true.
     */
    private boolean composing;

    /**
     * The legacy key code.
     * <p>
     * 예전 키 코드.
     */
    private int keyCode;

    /**
     * The legacy character code (only for <code>keypress</code>).
     * <p>
     * 예전 문자 코드(<code>keypress</code> 에만 있다).
     */
    private int charCode;

    /**
     * Creates a keyboard event.
     * <p>
     * 키보드 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public KeyboardEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets what identifies the key.
     * <p>
     * 키를 식별하는 값을 지정한다.
     *
     * @param keyValue     the key value / 키 값
     * @param codeValue    the physical key / 물리적 키
     * @param keyLocation  the location / 위치
     * @param legacyKey    the legacy key code / 예전 키 코드
     * @param legacyChar   the legacy character code / 예전 문자 코드
     */
    public void setKey(String keyValue, String codeValue, int keyLocation, int legacyKey, int legacyChar) {
        this.key = keyValue == null ? "" : keyValue;
        this.code = codeValue == null ? "" : codeValue;
        this.location = keyLocation;
        this.keyCode = legacyKey;
        this.charCode = legacyChar;
    }

    /**
     * Sets the repeat and composing flags.
     * <p>
     * 반복과 조합 표시를 지정한다.
     *
     * @param repeating whether the key repeats / 키가 반복되는지
     * @param isComposing whether an input method composes / 입력기가 조합 중인지
     */
    public void setStates(boolean repeating, boolean isComposing) {
        this.repeat = repeating;
        this.composing = isComposing;
    }

    /**
     * Returns the key value.
     * <p>
     * 키 값을 반환한다.
     *
     * @return the key / 키
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the physical key.
     * <p>
     * 물리적 키를 반환한다.
     *
     * @return the code / 코드
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the location of the key.
     * <p>
     * 키의 위치를 반환한다.
     *
     * @return the location / 위치
     */
    public int getLocation() {
        return location;
    }

    /**
     * Tells whether the key repeats.
     * <p>
     * 키가 반복되는지 알려 준다.
     *
     * @return true if repeating / 반복이면 true
     */
    public boolean isRepeat() {
        return repeat;
    }

    /**
     * Tells whether an input method composes text.
     * <p>
     * 입력기가 텍스트를 조합 중인지 알려 준다.
     *
     * @return true if composing / 조합 중이면 true
     */
    public boolean isComposing() {
        return composing;
    }

    /**
     * Returns the legacy key code.
     * <p>
     * 예전 키 코드를 반환한다.
     *
     * @return the code / 코드
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Returns the legacy character code.
     * <p>
     * 예전 문자 코드를 반환한다.
     *
     * @return the code / 코드
     */
    public int getCharCode() {
        return charCode;
    }

    /**
     * Returns the legacy <code>which</code>: the character code for <code>keypress</code>, the key code otherwise.
     * <p>
     * 예전 <code>which</code> 를 반환한다. <code>keypress</code> 는 문자 코드, 그 밖에는 키 코드이다.
     *
     * @return the value / 값
     */
    public int getWhich() {
        return "keypress".equals(getType()) ? charCode : keyCode;
    }
}
