package org.duckdns.hjow.swingwebpanel.core;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.dom.html.ControlDisplay;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;

/**
 * What the user's mouse and keyboard do to the controls that have no text to edit, written against the DOM only (the document thread runs it):
 * choosing options of a select element, moving the value of a slider, moving along the radio buttons of a group. Every change that the user makes
 * fires the events that a browser fires: <code>input</code> and, once the choice is done, <code>change</code>.
 * <p>
 * 편집할 글이 없는 컨트롤에 사용자의 마우스와 키보드가 하는 일을 DOM 만 가지고 쓴 것(문서 스레드가 실행한다): select 의 option 고르기, 슬라이더 값 움직이기,
 * 그룹의 라디오 버튼 사이 옮겨 다니기. 사용자가 하는 변경은 모두 브라우저가 발생시키는 이벤트를 발생시킨다: <code>input</code>, 선택이 끝나면 <code>change</code>.
 */
final class ControlActions {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ControlActions() {
    }

    /**
     * Chooses an option as the user does: in a drop-down or a single-choice list it becomes the only selected option, in a multiple-choice list
     * it replaces the selection or (with <code>toggle</code>) is added or removed. Disabled options cannot be chosen.
     * <p>
     * 사용자가 하듯이 option 을 고른다: 드롭다운이나 단일 선택 목록에서는 하나뿐인 선택된 option 이 되고, 다중 선택 목록에서는 선택을 대체하거나
     * (<code>toggle</code> 이면) 더하거나 뺀다. 비활성 option 은 고를 수 없다.
     *
     * @param select the select element / select 요소
     * @param option the option that was chosen / 고른 option
     * @param toggle true to toggle the option of a multiple-choice list / 다중 선택 목록에서 option 을 토글하려면 true
     * @return true if the selection changed (then <code>input</code> and <code>change</code> were fired) / 선택이 바뀌었으면(그러면 <code>input</code> 과
     *         <code>change</code> 가 발생했다) true
     */
    static boolean choose(HtmlSelectElement select, HtmlOptionElement option, boolean toggle) {
        if (option.isDisabled() || ControlDisplay.isDisabled(select)) {
            return false;
        }
        List<HtmlOptionElement> all = select.getOptionElements();
        boolean[] before = selection(all);
        if (select.isMultiple() && toggle) {
            option.setSelected(!option.isSelected());
        } else {
            // 고른 option 이외의 선택을 풀고 고른 option 을 선택한다.
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i) != option && all.get(i).isSelected()) {
                    all.get(i).setSelected(false);
                }
            }
            option.setSelected(true);
        }
        return announce(select, before, all);
    }

    /**
     * Chooses the options from one option to another (the shift key in a multiple-choice list); the enabled options in between are selected
     * and the others are not.
     * <p>
     * option 하나에서 다른 option 까지 고른다(다중 선택 목록에서 Shift 키). 그 사이의 활성 option 이 선택되고 나머지는 선택이 풀린다.
     *
     * @param select the select element / select 요소
     * @param from   the anchor option / 기준 option
     * @param to     the option that was chosen / 고른 option
     * @return true if the selection changed / 선택이 바뀌었으면 true
     */
    static boolean chooseRange(HtmlSelectElement select, HtmlOptionElement from, HtmlOptionElement to) {
        if (ControlDisplay.isDisabled(select)) {
            return false;
        }
        List<HtmlOptionElement> all = select.getOptionElements();
        boolean[] before = selection(all);
        int a = all.indexOf(from);
        int b = all.indexOf(to);
        if (a < 0 || b < 0) {
            return false;
        }
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        // 모든 option 을 보며 범위 안의 활성 option 만 선택한다.
        for (int i = 0; i < all.size(); i++) {
            all.get(i).setSelected(i >= lo && i <= hi && !all.get(i).isDisabled());
        }
        return announce(select, before, all);
    }

    /**
     * Moves the selection of a select element by a number of enabled options (the arrow keys), or to its first or last enabled option
     * (<code>Integer.MIN_VALUE</code> or <code>Integer.MAX_VALUE</code>: the Home and End keys).
     * <p>
     * select 의 선택을 활성 option 몇 개만큼 옮긴다(화살표 키). 또는 첫 번째나 마지막 활성 option 으로 옮긴다(<code>Integer.MIN_VALUE</code>,
     * <code>Integer.MAX_VALUE</code>: Home, End 키).
     *
     * @param select the select element / select 요소
     * @param delta  the number of options, negative for backward / option 수. 뒤로는 음수
     * @return the option that is selected now, or null if there is none / 지금 선택된 option. 없으면 null
     */
    static HtmlOptionElement move(HtmlSelectElement select, int delta) {
        List<HtmlOptionElement> all = select.getOptionElements();
        int current = -1;
        // 선택된 마지막 option 의 위치를 찾는다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isSelected()) {
                current = i;
            }
        }
        int target = current;
        if (delta == Integer.MIN_VALUE || delta == Integer.MAX_VALUE) {
            target = delta < 0 ? nextEnabled(all, -1, 1) : nextEnabled(all, all.size(), -1);
        } else {
            int step = delta < 0 ? -1 : 1;
            int left = Math.abs(delta);
            // 활성 option 을 delta 개 지날 때까지 한 칸씩 옮긴다.
            while (left > 0) {
                int next = nextEnabled(all, target, step);
                if (next < 0) {
                    break;
                }
                target = next;
                left--;
            }
        }
        if (target < 0 || target >= all.size() || target == current) {
            return null;
        }
        HtmlOptionElement option = all.get(target);
        choose(select, option, false);
        return option;
    }

    /**
     * Finds the next enabled option from a position in a direction.
     * <p>
     * 위치에서 한 방향으로 다음 활성 option 을 찾는다.
     *
     * @param all  the options / option 들
     * @param from the position to start after / 이 위치 다음부터 찾는다
     * @param step 1 or -1 / 1 또는 -1
     * @return the position, or -1 if there is none / 위치. 없으면 -1
     */
    private static int nextEnabled(List<HtmlOptionElement> all, int from, int step) {
        // 한 방향으로 한 칸씩 가며 처음 만나는 활성 option 을 찾는다.
        for (int i = from + step; i >= 0 && i < all.size(); i += step) {
            if (!all.get(i).isDisabled()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Selects the first enabled option, after the selected one, whose label begins with the typed text (type-ahead find).
     * <p>
     * 선택된 option 다음부터 라벨이 입력한 글로 시작하는 첫 활성 option 을 고른다(입력으로 찾기).
     *
     * @param select the select element / select 요소
     * @param typed  the characters typed in a row / 연달아 입력한 글자
     * @return the chosen option or null / 고른 option 또는 null
     */
    static HtmlOptionElement find(HtmlSelectElement select, String typed) {
        List<HtmlOptionElement> all = select.getOptionElements();
        if (all.isEmpty() || typed.length() == 0) {
            return null;
        }
        int current = -1;
        // 선택된 option 의 위치를 찾는다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isSelected()) {
                current = i;
            }
        }
        String lower = typed.toLowerCase();
        boolean repeat = typed.length() > 1 && allSame(typed);
        // 같은 글자를 반복해서 치면 다음 option 부터 그 글자로, 여러 글자를 이어 치면 현재 option 부터 그 글자들로 찾는다.
        int start = typed.length() == 1 || repeat ? current + 1 : Math.max(current, 0);
        if (repeat) {
            lower = lower.substring(0, 1);
        }
        // 시작 위치부터 한 바퀴 돌며 라벨이 맞는 활성 option 을 찾는다.
        for (int n = 0; n < all.size(); n++) {
            HtmlOptionElement o = all.get((start + n + all.size()) % all.size());
            if (!o.isDisabled() && o.getLabel().trim().toLowerCase().startsWith(lower)) {
                choose(select, o, false);
                return o;
            }
        }
        return null;
    }

    /**
     * Tells whether every character of a string is the same.
     * <p>
     * 문자열의 모든 글자가 같은지 알려 준다.
     *
     * @param s the string / 문자열
     * @return true if all the same / 모두 같으면 true
     */
    private static boolean allSame(String s) {
        // 첫 글자와 다른 글자가 있는지 찾는다.
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != s.charAt(0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns which options are selected.
     * <p>
     * 어떤 option 이 선택되었는지 반환한다.
     *
     * @param all the options / option 들
     * @return the selectedness of every option / 모든 option 의 선택 상태
     */
    private static boolean[] selection(List<HtmlOptionElement> all) {
        boolean[] s = new boolean[all.size()];
        // option 마다 선택 상태를 기록한다.
        for (int i = 0; i < s.length; i++) {
            s[i] = all.get(i).isSelected();
        }
        return s;
    }

    /**
     * Fires <code>input</code> and <code>change</code> if the selection is not the one before.
     * <p>
     * 선택이 이전과 다르면 <code>input</code> 과 <code>change</code> 를 발생시킨다.
     *
     * @param select the select element / select 요소
     * @param before the selection before / 이전 선택
     * @param all    the options / option 들
     * @return true if the selection changed / 선택이 바뀌었으면 true
     */
    private static boolean announce(HtmlSelectElement select, boolean[] before, List<HtmlOptionElement> all) {
        boolean[] now = selection(all);
        // 이전과 달라진 option 이 하나라도 있는지 본다.
        for (int i = 0; i < now.length; i++) {
            if (i >= before.length || now[i] != before[i]) {
                select.fireInputEvent(null, null);
                select.fireChangeEvent();
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the value of a slider as a key does: the arrows by one step, Page Up and Page Down by ten steps, Home and End to the ends.
     * <code>input</code> and <code>change</code> follow when the value changed.
     * <p>
     * 키가 하는 대로 슬라이더의 값을 옮긴다: 화살표는 한 단계, Page Up 과 Page Down 은 열 단계, Home 과 End 는 양 끝으로. 값이 바뀌면 <code>input</code> 과
     * <code>change</code> 가 이어서 발생한다.
     *
     * @param range the slider / 슬라이더
     * @param key   the <code>key</code> value of the key event / 키 이벤트의 <code>key</code> 값
     * @return true if the key was a slider key / 슬라이더 키였으면 true
     */
    static boolean slide(HtmlInputElement range, String key) {
        String before = range.getValue();
        try {
            if ("ArrowRight".equals(key) || "ArrowUp".equals(key)) {
                range.stepUp(1);
            } else if ("ArrowLeft".equals(key) || "ArrowDown".equals(key)) {
                range.stepDown(1);
            } else if ("PageUp".equals(key)) {
                range.stepUp(10);
            } else if ("PageDown".equals(key)) {
                range.stepDown(10);
            } else if ("Home".equals(key)) {
                range.stepDown(Integer.MAX_VALUE / 2);
            } else if ("End".equals(key)) {
                range.stepUp(Integer.MAX_VALUE / 2);
            } else {
                return false;
            }
        } catch (RuntimeException e) {
            // 단계 계산이 불가능한 값(범위 오류 등)에서는 키를 무시한다.
            return true;
        }
        if (!range.getValue().equals(before)) {
            range.fireInputEvent(null, null);
            range.fireChangeEvent();
        }
        return true;
    }

    /**
     * Sets the value of a slider from a position on its track (a mouse press or drag), as a sanitized value of the control, and fires
     * <code>input</code> when it changed.
     * <p>
     * 슬라이더 트랙의 한 위치(마우스 누름이나 끌기)로 슬라이더의 값을 컨트롤의 정리된 값으로 정하고, 바뀌었으면 <code>input</code> 을 발생시킨다.
     *
     * @param range the slider / 슬라이더
     * @param ratio the position between 0 and 1 / 0 과 1 사이의 위치
     * @return true if the value changed / 값이 바뀌었으면 true
     */
    static boolean slideTo(HtmlInputElement range, double ratio) {
        double min = number(range.getAttribute("min"), 0);
        double max = number(range.getAttribute("max"), 100);
        double clamped = Math.max(0, Math.min(1, ratio));
        double v = min + clamped * (Math.max(min, max) - min);
        String before = range.getValue();
        range.setValueFromUser(new java.math.BigDecimal(v).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        if (range.getValue().equals(before)) {
            return false;
        }
        range.fireInputEvent(null, null);
        return true;
    }

    /**
     * Parses a number attribute.
     * <p>
     * 숫자 속성을 해석한다.
     *
     * @param s        the attribute value or null / 속성 값 또는 null
     * @param fallback the value if it is missing or not a number / 없거나 숫자가 아닐 때 값
     * @return the number / 숫자
     */
    private static double number(String s, double fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            double d = Double.parseDouble(s.trim());
            return Double.isNaN(d) || Double.isInfinite(d) ? fallback : d;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Moves from a radio button to the next or previous enabled one of its group (the arrow keys): the new one gets the focus and is checked as
     * if it were clicked.
     * <p>
     * 라디오 단추에서 같은 그룹의 다음 또는 이전 활성 단추로 옮긴다(화살표 키): 새 단추가 포커스를 받고 눌린 것처럼 체크된다.
     *
     * @param radio the radio button that has the focus / 포커스를 가진 라디오 단추
     * @param delta 1 for forward, -1 for backward / 앞으로는 1, 뒤로는 -1
     * @return the new radio button, or null if the group has no other enabled one / 새 단추. 그룹에 다른 활성 단추가 없으면 null
     */
    static HtmlInputElement moveRadio(HtmlInputElement radio, int delta) {
        List<HtmlInputElement> group = radio.getRadioGroup();
        int at = group.indexOf(radio);
        // 그룹을 delta 방향으로 한 바퀴 돌며 처음 만나는 활성 단추를 찾는다.
        for (int n = 1; n < group.size(); n++) {
            HtmlInputElement next = group.get(((at + delta * n) % group.size() + group.size()) % group.size());
            if (!ControlDisplay.isDisabled(next)) {
                FocusManager.focus(next, next.getOwnerDocument().isWindowFocused());
                next.dispatchTrustedEvent(new MouseEvent("click", true, true));
                return next;
            }
        }
        return null;
    }
}
