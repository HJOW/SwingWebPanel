package org.duckdns.hjow.swingwebpanel.dom.html;

/**
 * The selection of a text control (HTML Standard 4.10.20 "APIs for the text control selections"): the start and end of the selected text
 * and its direction. The value of the control is owned by the element, so every method gets its current length. The positions are UTF-16 code
 * unit offsets.
 * <p>
 * 텍스트 컨트롤의 선택(HTML 표준 4.10.20 "APIs for the text control selections"): 선택한 글의 시작과 끝, 방향. 컨트롤의 값은 요소가 소유하므로 모든 메서드는 현재
 * 길이를 받는다. 위치는 UTF-16 코드 단위 오프셋이다.
 */
final class TextSelection {

    /** Selection start. / 선택 시작. */
    private int start;

    /** Selection end. / 선택 끝. */
    private int end;

    /** Selection direction: none, forward or backward. / 선택 방향: none, forward, backward. */
    private String direction = "none";

    /**
     * Returns the selection start, clamped to the value length.
     * <p>
     * 선택 시작을 값 길이에 맞춰 반환한다.
     *
     * @param length the length of the value / 값의 길이
     * @return the start / 시작
     */
    int getStart(int length) {
        return Math.min(start, length);
    }

    /**
     * Returns the selection end, clamped to the value length.
     * <p>
     * 선택 끝을 값 길이에 맞춰 반환한다.
     *
     * @param length the length of the value / 값의 길이
     * @return the end / 끝
     */
    int getEnd(int length) {
        return Math.min(end, length);
    }

    /**
     * Returns the selection direction.
     * <p>
     * 선택 방향을 반환한다.
     *
     * @return none, forward or backward / none, forward, backward
     */
    String getDirection() {
        return direction;
    }

    /**
     * Moves the cursor to the end of the text and unselects (the effect of changing the value from a script).
     * <p>
     * 커서를 글의 끝으로 옮기고 선택을 푼다(스크립트가 값을 바꿀 때의 효과).
     *
     * @param length the length of the value / 값의 길이
     */
    void moveToEnd(int length) {
        start = length;
        end = length;
        direction = "none";
    }

    /**
     * Moves the cursor to the beginning of the text and unselects.
     * <p>
     * 커서를 글의 처음으로 옮기고 선택을 푼다.
     */
    void moveToStart() {
        start = 0;
        end = 0;
        direction = "none";
    }

    /**
     * Sets the selection range ("set the selection range" with clamping and the direction rules).
     * <p>
     * 선택 범위를 지정한다(위치 보정과 방향 규칙을 적용한 "set the selection range").
     *
     * @param s         new start / 새 시작
     * @param e         new end / 새 끝
     * @param dir       direction or null for "none" / 방향. null 이면 "none"
     * @param length    the length of the value / 값의 길이
     * @return true if the selection changed / 선택이 바뀌었으면 true
     */
    boolean set(int s, int e, String dir, int length) {
        int ns = Math.max(0, Math.min(s, length));
        int ne = Math.max(0, Math.min(e, length));
        String nd = "forward".equals(dir) || "backward".equals(dir) ? dir : "none";
        if (ne < ns) {
            ns = ne;
        }
        boolean changed = ns != start || ne != end || !nd.equals(direction);
        start = ns;
        end = ne;
        direction = nd;
        return changed;
    }

    /**
     * Copies this selection to another object.
     * <p>
     * 이 선택을 다른 객체로 복사한다.
     *
     * @param to the target / 대상
     */
    void copyTo(TextSelection to) {
        to.start = start;
        to.end = end;
        to.direction = direction;
    }

    /**
     * Adjusts the selection after a text range was replaced ("preserve" select mode of <code>setRangeText</code>), or selects according to the mode.
     * <p>
     * 글의 구간을 바꾼 뒤 선택을 조정한다(<code>setRangeText</code> 의 "preserve" 모드). 모드에 따라 선택하기도 한다.
     *
     * @param mode        select, start, end or preserve / select, start, end, preserve
     * @param rangeStart  start of the replaced range / 바뀐 구간의 시작
     * @param rangeEnd    end of the replaced range (before the replacement) / 바뀐 구간의 끝(바꾸기 전)
     * @param newLength   length of the replacement / 바꾼 글의 길이
     * @param oldLength   the length of the value before the replacement / 바꾸기 전 값의 길이
     */
    void afterReplace(String mode, int rangeStart, int rangeEnd, int newLength, int oldLength) {
        int newEnd = rangeStart + newLength;
        if ("select".equals(mode)) {
            start = rangeStart;
            end = newEnd;
            direction = "none";
        } else if ("start".equals(mode)) {
            start = rangeStart;
            end = rangeStart;
            direction = "none";
        } else if ("end".equals(mode)) {
            start = newEnd;
            end = newEnd;
            direction = "none";
        } else {
            int delta = newLength - (rangeEnd - rangeStart);
            int s = Math.min(start, oldLength);
            int e = Math.min(end, oldLength);
            // 바뀐 구간 뒤에 있던 위치는 길이 차이만큼 밀고, 구간 안에 있던 위치는 구간 끝에 맞춘다.
            if (s > rangeEnd) {
                s += delta;
            } else if (s > rangeStart) {
                s = rangeStart;
            }
            if (e > rangeEnd) {
                e += delta;
            } else if (e > rangeStart) {
                e = newEnd;
            }
            start = s;
            end = e;
        }
    }
}
