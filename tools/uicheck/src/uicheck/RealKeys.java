package uicheck;

import java.awt.Robot;
import java.awt.event.KeyEvent;

/**
 * Presses real keys of the operating system with a <code>java.awt.Robot</code> (the key events go through the window system and the input method
 * of the focused window, like a person's typing). It understands key specifications such as <code>TAB</code>, <code>shift+TAB</code>,
 * <code>ctrl+C</code> or <code>BACK_SPACE*3</code> (the names are the <code>KeyEvent.VK_</code> names without the prefix) and types plain ASCII text
 * on a US/Korean keyboard layout.
 * <p>
 * <code>java.awt.Robot</code> 으로 운영체제의 실제 키를 누른다(키 이벤트가 사람이 친 것처럼 창 시스템과 포커스를 가진 창의 입력기를 거친다).
 * <code>TAB</code>, <code>shift+TAB</code>, <code>ctrl+C</code>, <code>BACK_SPACE*3</code> 같은 키 지정(이름은 <code>KeyEvent.VK_</code> 에서 앞부분을 뺀 것)을
 * 이해하고, 미국/한국 자판 배열에서 ASCII 글을 친다.
 * <p>
 * Usage / 사용법: <code>RealKeys keys = new RealKeys(robot); keys.press("shift+TAB"); keys.type("gksrmf");</code>
 */
final class RealKeys {

    /** Pause between two key strokes in milliseconds. / 두 키 입력 사이의 쉼(밀리초). */
    private static final int STROKE_MS = 70;

    /** Unshifted punctuation on the keyboard. / 자판에서 Shift 없이 치는 문장 부호. */
    private static final String PLAIN = "`-=[]\\;',./ ";

    /** The shifted counterparts of {@link #PLAIN} (the space has none). / {@link #PLAIN} 에 Shift 를 누른 문자(공백은 없음). */
    private static final String SHIFTED = "~_+{}|:\"<>? ";

    /** The key codes of {@link #PLAIN}. / {@link #PLAIN} 의 키 코드. */
    private static final int[] PLAIN_KEYS = {KeyEvent.VK_BACK_QUOTE, KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS, KeyEvent.VK_OPEN_BRACKET,
        KeyEvent.VK_CLOSE_BRACKET, KeyEvent.VK_BACK_SLASH, KeyEvent.VK_SEMICOLON, KeyEvent.VK_QUOTE, KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD,
        KeyEvent.VK_SLASH, KeyEvent.VK_SPACE};

    /** Shifted digits, in the order of the digit keys 1 to 0. / 숫자 키 1~0 순서의 Shift 문자. */
    private static final String SHIFTED_DIGITS = "!@#$%^&*()";

    /** The robot / 로봇. */
    private final Robot robot;

    /**
     * Creates the helper for a robot.
     * <p>
     * 로봇 하나에 대한 도우미를 만든다.
     *
     * @param robot the robot / 로봇
     */
    RealKeys(Robot robot) {
        this.robot = robot;
    }

    /**
     * Presses a key specification: modifiers (<code>ctrl</code>, <code>shift</code>, <code>alt</code>) and a key name joined by <code>+</code>, and
     * an optional repeat count after <code>*</code>.
     * <p>
     * 키 지정을 누른다: 보조 키(<code>ctrl</code>, <code>shift</code>, <code>alt</code>)와 키 이름을 <code>+</code> 로 잇고, 반복 횟수를 <code>*</code> 뒤에 쓸 수 있다.
     *
     * @param spec the specification / 키 지정
     * @throws InterruptedException if interrupted / 중단된 경우
     */
    void press(String spec) throws InterruptedException {
        int times = 1;
        int star = spec.lastIndexOf('*');
        if (star > 0) {
            times = Integer.parseInt(spec.substring(star + 1));
            spec = spec.substring(0, star);
        }
        String[] parts = spec.split("\\+");
        int[] codes = new int[parts.length];
        // 각 부분을 키 코드로 바꾼다(앞쪽은 보조 키, 마지막은 누를 키).
        for (int i = 0; i < parts.length; i++) {
            codes[i] = codeOf(parts[i]);
        }
        // 정한 횟수만큼 보조 키를 누른 채 마지막 키를 누른다.
        for (int t = 0; t < times; t++) {
            stroke(codes);
        }
    }

    /**
     * Types a text of ASCII characters with real key strokes (letters, digits, space and the punctuation of the keyboard).
     * <p>
     * ASCII 문자(글자, 숫자, 공백, 자판의 문장 부호)로 된 글을 실제 키 입력으로 친다.
     *
     * @param text the text / 글
     * @throws InterruptedException if interrupted / 중단된 경우
     */
    void type(String text) throws InterruptedException {
        // 문자마다 키 코드와 Shift 여부를 정해 누른다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int plain = PLAIN.indexOf(c);
            int shifted = SHIFTED.indexOf(c);
            int digit = SHIFTED_DIGITS.indexOf(c);
            if (c >= 'a' && c <= 'z') {
                stroke(new int[] {KeyEvent.VK_A + (c - 'a')});
            } else if (c >= 'A' && c <= 'Z') {
                stroke(new int[] {KeyEvent.VK_SHIFT, KeyEvent.VK_A + (c - 'A')});
            } else if (c >= '0' && c <= '9') {
                stroke(new int[] {KeyEvent.VK_0 + (c - '0')});
            } else if (plain >= 0) {
                stroke(new int[] {PLAIN_KEYS[plain]});
            } else if (shifted >= 0 && c != ' ') {
                stroke(new int[] {KeyEvent.VK_SHIFT, PLAIN_KEYS[shifted]});
            } else if (digit >= 0) {
                // Shift 숫자 문자는 1~9, 0 순서이다.
                stroke(new int[] {KeyEvent.VK_SHIFT, digit == 9 ? KeyEvent.VK_0 : KeyEvent.VK_1 + digit});
            } else {
                throw new IllegalArgumentException("cannot type the character U+" + Integer.toHexString(c));
            }
        }
    }

    /**
     * Presses the keys in order, releases them in the reverse order and waits a little.
     * <p>
     * 키를 차례로 누르고 거꾸로 뗀 뒤 잠깐 기다린다.
     *
     * @param codes the key codes; all but the last are held / 키 코드. 마지막을 뺀 나머지는 누른 채 둔다
     * @throws InterruptedException if interrupted / 중단된 경우
     */
    private void stroke(int[] codes) throws InterruptedException {
        // 앞에서부터 누른다.
        for (int i = 0; i < codes.length; i++) {
            robot.keyPress(codes[i]);
        }
        // 뒤에서부터 뗀다.
        for (int i = codes.length - 1; i >= 0; i--) {
            robot.keyRelease(codes[i]);
        }
        robot.waitForIdle();
        Thread.sleep(STROKE_MS);
    }

    /**
     * Returns the key code of a modifier or key name.
     * <p>
     * 보조 키 또는 키 이름의 키 코드를 반환한다.
     *
     * @param name the name (<code>ctrl</code>, <code>shift</code>, <code>alt</code> or a <code>VK_</code> name without the prefix) / 이름
     * @return the key code / 키 코드
     */
    static int codeOf(String name) {
        String n = name.trim();
        // 보조 키 이름은 따로 받는다.
        if ("ctrl".equalsIgnoreCase(n) || "control".equalsIgnoreCase(n)) {
            return KeyEvent.VK_CONTROL;
        }
        if ("shift".equalsIgnoreCase(n)) {
            return KeyEvent.VK_SHIFT;
        }
        if ("alt".equalsIgnoreCase(n)) {
            return KeyEvent.VK_ALT;
        }
        try {
            return KeyEvent.class.getField("VK_" + n.toUpperCase()).getInt(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("unknown key name " + name);
        }
    }
}
