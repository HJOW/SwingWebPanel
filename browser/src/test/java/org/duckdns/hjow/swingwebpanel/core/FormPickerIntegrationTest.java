package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.core.FormControlIntegrationTest.Condition;
import org.duckdns.hjow.swingwebpanel.core.FormControlIntegrationTest.Fixture;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Integration tests of the controls that the mouse and the keyboard choose in (plan items P11-05 and P11-10): the drop-down list of a select
 * element, a list box, a slider, a color well and the radio buttons of a group, through the real input classes of the page, the document thread
 * and the Swing list that opens under a drop-down. The events that a browser fires (<code>input</code>, <code>change</code>) are counted.
 * <p>
 * 마우스와 키보드로 고르는 컨트롤의 통합 시험(계획 P11-05, P11-10): select 의 드롭다운 목록, 리스트 박스, 슬라이더, 색 선택 단추, 그룹의 라디오 단추를 페이지의 실제
 * 입력 클래스, 문서 스레드, 드롭다운 아래에 열리는 Swing 목록으로 확인한다. 브라우저가 발생시키는 이벤트(<code>input</code>, <code>change</code>)를 센다.
 */
public class FormPickerIntegrationTest {

    /** A page with controls that are chosen in. 고르는 컨트롤이 있는 페이지. */
    private static final String PAGE = "<!doctype html><style>html,body{margin:0} select,input{font:12px monospace;margin:0;display:block}"
            + "#rg{width:100px}</style><form>"
            + "<select id=sel><option>one</option><option selected>two</option><option disabled>three</option><option>four</option></select>"
            + "<select id=lb size=3 multiple><option>a</option><option>b</option><option>c</option><option>d</option></select>"
            + "<input id=rg type=range min=0 max=100 value=0>"
            + "<input id=col type=color value='#000000'>"
            + "<input id=r1 type=radio name=r checked><input id=r2 type=radio name=r><input id=r3 type=radio name=r disabled><input id=r4 type=radio name=r>"
            + "</form>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormPickerIntegrationTest() {
    }

    /**
     * Sends a trusted click with coordinates to an element on the document thread (as the page input does for a real click).
     * <p>
     * 실제 클릭을 페이지 입력이 하듯이 문서 스레드에서 요소에 좌표가 있는 신뢰된 클릭을 보낸다.
     *
     * @param f     the fixture / 고정 설정
     * @param id    the id of the element / 요소의 id
     * @param x     the page x / 페이지 x
     * @param y     the page y / 페이지 y
     * @param ctrl  the Ctrl key / Ctrl 키
     * @param shift the Shift key / Shift 키
     * @throws Exception on failure / 실패하면
     */
    private static void click(final Fixture f, final String id, final double x, final double y, final boolean ctrl, final boolean shift) throws Exception {
        f.onDocument(new Runnable() {
            public void run() {
                org.duckdns.hjow.swingwebpanel.dom.MouseEvent e = new org.duckdns.hjow.swingwebpanel.dom.MouseEvent("click", true, true);
                e.setCoordinates(x, y, x, y, x, y, 0, 0);
                e.setModifiers(ctrl, shift, false, false);
                f.el(id).dispatchTrustedEvent(e);
            }
        });
    }

    /**
     * Checks that a click on a drop-down opens its list under the control, that the list shows the options, and that picking a row selects the
     * option and fires <code>input</code> and <code>change</code>.
     * <p>
     * 드롭다운을 누르면 컨트롤 아래에 목록이 열리고 option 이 보이며, 행을 고르면 option 이 선택되고 <code>input</code> 과 <code>change</code> 가 발생하는지
     * 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testDropdownOpensAndPicks() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        Check.isNull(f.overlays.getPicker(), "no list before the click");
        ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("sel"));
        click(f, "sel", c.getX() + 5, c.getY() + 5, false, false);
        final JList list = f.overlays.getPicker();
        Check.notNull(list, "the list is open");
        Check.equal(4, list.getModel().getSize(), "four rows");
        Check.equal(1, list.getSelectedIndex(), "the selected option is highlighted");
        double[] border = c.getBorderBox();
        Check.equal((int) Math.round(border[0]), list.getParent().getParent().getX(), "the list is under the control (x)");
        Check.equal((int) Math.round(border[1] + border[3]), list.getParent().getParent().getY(), "the list is under the control (y)");
        f.forgetEvents();
        release(list, 3);
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        Check.isNull(f.overlays.getPicker(), "the list closes after a choice");
        Check.equal("four", ((HtmlSelectElement) f.el("sel")).getValue(), "the chosen option is selected");
        Check.equal(1, f.count("input:sel"), "one input event");
        Check.equal(1, f.count("change:sel"), "one change event");
    }

    /**
     * Sends a mouse release to a row of a list.
     * <p>
     * 목록의 행에 마우스 떼기를 보낸다.
     *
     * @param list the list / 목록
     * @param row  the row / 행
     * @throws Exception on failure / 실패하면
     */
    private static void release(final JList list, final int row) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Rectangle b = list.getCellBounds(row, row);
                Point p = new Point(b.x + 5, b.y + b.height / 2);
                list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, p.x, p.y, 1, false,
                        MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Checks that a disabled option cannot be picked and that a second click on the drop-down closes the list without a change.
     * <p>
     * 비활성 option 을 고를 수 없고, 드롭다운을 다시 누르면 변경 없이 목록이 닫히는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testDisabledOptionAndSecondClick() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("sel"));
        click(f, "sel", c.getX() + 5, c.getY() + 5, false, false);
        release(f.overlays.getPicker(), 2);
        TestSupport.settle(f.session);
        Check.notNull(f.overlays.getPicker(), "a disabled row cannot be picked: the list stays open");
        Check.equal("two", ((HtmlSelectElement) f.el("sel")).getValue(), "and the selection did not change");
        click(f, "sel", c.getX() + 5, c.getY() + 5, false, false);
        TestSupport.flushEdt();
        Check.isNull(f.overlays.getPicker(), "a click on the control closes the list");
        Check.equal(0, f.count("change:sel"), "without a change event");
    }

    /**
     * Checks the keys of a closed drop-down: the arrows move the selection over the options that are enabled, a letter finds an option, and
     * Enter opens the list.
     * <p>
     * 닫힌 드롭다운의 키를 확인한다: 화살표는 활성 option 위로 선택을 옮기고, 글자는 option 을 찾고, Enter 는 목록을 연다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testDropdownKeys() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("sel");
        f.forgetEvents();
        key(f, "ArrowDown");
        Check.equal("four", ((HtmlSelectElement) f.el("sel")).getValue(), "the arrow skips the disabled option");
        Check.equal(1, f.count("change:sel"), "and fires change");
        key(f, "ArrowUp");
        Check.equal("two", ((HtmlSelectElement) f.el("sel")).getValue(), "back up");
        key(f, "Home");
        Check.equal("one", ((HtmlSelectElement) f.el("sel")).getValue(), "Home");
        key(f, "End");
        Check.equal("four", ((HtmlSelectElement) f.el("sel")).getValue(), "End");
        key(f, "o");
        Check.equal("one", ((HtmlSelectElement) f.el("sel")).getValue(), "a letter finds the option that begins with it");
        key(f, "Enter");
        TestSupport.flushEdt();
        Check.notNull(f.overlays.getPicker(), "Enter opens the list");
    }

    /**
     * Sends a key to the page keyboard of the focused control.
     * <p>
     * 포커스를 가진 컨트롤에 대한 페이지 키보드에 키를 보낸다.
     *
     * @param f   the fixture / 고정 설정
     * @param key the <code>key</code> value / <code>key</code> 값
     * @throws Exception on failure / 실패하면
     */
    private static void key(final Fixture f, String key) throws Exception {
        final int vk;
        char ch = KeyEvent.CHAR_UNDEFINED;
        if ("ArrowDown".equals(key)) {
            vk = KeyEvent.VK_DOWN;
        } else if ("ArrowUp".equals(key)) {
            vk = KeyEvent.VK_UP;
        } else if ("ArrowLeft".equals(key)) {
            vk = KeyEvent.VK_LEFT;
        } else if ("ArrowRight".equals(key)) {
            vk = KeyEvent.VK_RIGHT;
        } else if ("Home".equals(key)) {
            vk = KeyEvent.VK_HOME;
        } else if ("End".equals(key)) {
            vk = KeyEvent.VK_END;
        } else if ("Enter".equals(key)) {
            vk = KeyEvent.VK_ENTER;
            ch = '\n';
        } else {
            vk = Character.toUpperCase(key.charAt(0));
            ch = key.charAt(0);
        }
        final char typed = ch;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // AWT 는 포커스가 없는 컴포넌트의 키 이벤트를 돌려보내므로 페이지 키보드를 직접 부른다.
                f.keyboard.keyPressed(new KeyEvent(f.component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, vk, typed));
                f.keyboard.keyReleased(new KeyEvent(f.component, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, vk, typed));
            }
        });
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
    }

    /**
     * Checks a list box: a click chooses a row, Ctrl toggles, Shift extends the range, and every change fires <code>change</code>.
     * <p>
     * 리스트 박스를 확인한다: 클릭은 행을 고르고, Ctrl 은 토글하고, Shift 는 범위를 넓히며, 변경마다 <code>change</code> 가 발생한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testListBoxMouse() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("lb"));
        double row = c.getLineHeight();
        final HtmlSelectElement lb = (HtmlSelectElement) f.el("lb");
        f.forgetEvents();
        click(f, "lb", c.getX() + 5, c.getY() + row * 0.5, false, false);
        Check.equal("a", selected(lb), "a click chooses the row");
        click(f, "lb", c.getX() + 5, c.getY() + row * 2.5, true, false);
        Check.equal("a,c", selected(lb), "Ctrl adds a row");
        click(f, "lb", c.getX() + 5, c.getY() + row * 2.5, true, false);
        Check.equal("a", selected(lb), "Ctrl on a selected row removes it");
        click(f, "lb", c.getX() + 5, c.getY() + row * 0.5, false, false);
        Check.equal("a", selected(lb), "a plain click on the only selected row changes nothing");
        click(f, "lb", c.getX() + 5, c.getY() + row * 2.5, false, true);
        Check.equal("a,b,c", selected(lb), "Shift extends from the last chosen row");
        click(f, "lb", c.getX() + 5, c.getY() + row * 1.5, false, false);
        Check.equal("b", selected(lb), "a plain click replaces the selection");
        Check.equal(5, f.count("change:lb"), "every change fired change, and none for a click that changed nothing");
    }

    /**
     * Returns the labels of the selected options of a list.
     * <p>
     * 목록에서 선택된 option 의 라벨을 반환한다.
     *
     * @param s the select element / select 요소
     * @return the labels, comma separated / 쉼표로 구분한 라벨
     */
    private static String selected(HtmlSelectElement s) {
        StringBuilder b = new StringBuilder();
        // option 을 순서대로 보며 선택된 것의 라벨을 모은다.
        for (HtmlOptionElement o : s.getOptionElements()) {
            if (o.isSelected()) {
                b.append(b.length() == 0 ? "" : ",").append(o.getLabel());
            }
        }
        return b.toString();
    }

    /**
     * Checks a slider: a press moves the thumb to the mouse, the drag follows it, the release fires <code>change</code>, and the arrow keys
     * step the value.
     * <p>
     * 슬라이더를 확인한다: 누르면 손잡이가 마우스로 옮겨지고, 끌기가 따라가며, 떼면 <code>change</code> 가 발생하고, 화살표 키는 값을 한 단계씩 바꾼다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testSlider() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        final ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("rg"));
        final HtmlInputElement rg = (HtmlInputElement) f.el("rg");
        f.forgetEvents();
        f.onDocument(new Runnable() {
            public void run() {
                f.el("rg").dispatchTrustedEvent(mouse("mousedown", c.getX() + c.getWidth() / 2, c.getY() + 3, 1));
            }
        });
        Check.isTrue(Math.abs(Double.parseDouble(rg.getValue()) - 50) <= 1, "a press in the middle gives about 50, was " + rg.getValue());
        f.onDocument(new Runnable() {
            public void run() {
                f.el("rg").dispatchTrustedEvent(mouse("mousemove", c.getX() + c.getWidth() - 1, c.getY() + 3, 1));
            }
        });
        Check.equal("100", rg.getValue(), "the drag follows the mouse to the end");
        f.onDocument(new Runnable() {
            public void run() {
                f.el("rg").dispatchTrustedEvent(mouse("mouseup", c.getX() + c.getWidth() - 1, c.getY() + 3, 0));
            }
        });
        Check.equal(1, f.count("change:rg"), "the release fires change");
        Check.isTrue(f.count("input:rg") >= 2, "and the move fired input events");
        f.focus("rg");
        key(f, "ArrowLeft");
        Check.equal("99", rg.getValue(), "an arrow key steps by one");
        key(f, "Home");
        Check.equal("0", rg.getValue(), "Home goes to the minimum");
    }

    /**
     * Makes a mouse event of the page input kind.
     * <p>
     * 페이지 입력이 만드는 종류의 마우스 이벤트를 만든다.
     *
     * @param type    the event type / 이벤트 종류
     * @param x       the page x / 페이지 x
     * @param y       the page y / 페이지 y
     * @param buttons the buttons that are down / 눌려 있는 단추
     * @return the event / 이벤트
     */
    private static org.duckdns.hjow.swingwebpanel.dom.MouseEvent mouse(String type, double x, double y, int buttons) {
        org.duckdns.hjow.swingwebpanel.dom.MouseEvent e = new org.duckdns.hjow.swingwebpanel.dom.MouseEvent(type, true, true);
        e.setCoordinates(x, y, x, y, x, y, 0, 0);
        e.setButtons(0, buttons);
        return e;
    }

    /**
     * Checks a color well: the color that the chooser gives becomes the value and fires <code>input</code> and <code>change</code>, and a
     * cancelled chooser changes nothing.
     * <p>
     * 색 선택 단추를 확인한다: 선택기가 준 색이 값이 되어 <code>input</code> 과 <code>change</code> 가 발생하고, 취소하면 아무것도 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testColorWell() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("col"));
        f.forgetEvents();
        f.pickedColor = null;
        click(f, "col", c.getX() + 5, c.getY() + 5, false, false);
        Check.equal("#000000", ((HtmlInputElement) f.el("col")).getValue(), "a cancelled chooser changes nothing");
        f.pickedColor = Integer.valueOf(0xFF8800);
        click(f, "col", c.getX() + 5, c.getY() + 5, false, false);
        Check.equal("#ff8800", ((HtmlInputElement) f.el("col")).getValue(), "the chosen color is the value");
        Check.equal(1, f.count("change:col"), "change was fired");
        until(f, "the swatch is drawn with the new color", new Condition() {
            public boolean holds() {
                return f.view.getRenderedPage().getControl(f.el("col")).getSwatch() == 0xFFFF8800;
            }
        });
    }

    /**
     * Waits for a condition.
     * <p>
     * 조건을 기다린다.
     *
     * @param f    the fixture / 고정 설정
     * @param what what is awaited / 기다리는 것
     * @param c    the condition / 조건
     * @throws Exception on failure / 실패하면
     */
    private static void until(Fixture f, String what, Condition c) throws Exception {
        FormControlIntegrationTest.until(what, c);
    }

    /**
     * Checks the arrow keys on radio buttons: they move to the next enabled button of the group, check it and fire <code>change</code>; the
     * disabled button is skipped and the group wraps around.
     * <p>
     * 라디오 단추의 화살표 키를 확인한다: 그룹의 다음 활성 단추로 옮기고, 체크하고, <code>change</code> 를 발생시킨다. 비활성 단추는 건너뛰고 그룹은 한 바퀴 돈다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testRadioArrowKeys() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("r1");
        f.forgetEvents();
        key(f, "ArrowDown");
        Check.isTrue(((HtmlInputElement) f.el("r2")).isChecked(), "the next radio button is checked");
        Check.isFalse(((HtmlInputElement) f.el("r1")).isChecked(), "and the first one is not");
        Check.equal(1, f.count("change:r2"), "change was fired on the new button");
        key(f, "ArrowDown");
        Check.isTrue(((HtmlInputElement) f.el("r4")).isChecked(), "the disabled button is skipped");
        key(f, "ArrowDown");
        Check.isTrue(((HtmlInputElement) f.el("r1")).isChecked(), "the group wraps around");
        key(f, "ArrowUp");
        Check.isTrue(((HtmlInputElement) f.el("r4")).isChecked(), "ArrowUp goes backwards");
        Check.equal(r4Focus(f), "r4", "and the focus follows");
    }

    /**
     * Returns the id of the element that has the DOM focus.
     * <p>
     * DOM 포커스를 가진 요소의 id 를 반환한다.
     *
     * @param f the fixture / 고정 설정
     * @return the id / id
     */
    private static String r4Focus(Fixture f) {
        Element e = org.duckdns.hjow.swingwebpanel.dom.FocusManager.getFocused(f.document());
        return e == null ? null : e.getAttribute("id");
    }
}
