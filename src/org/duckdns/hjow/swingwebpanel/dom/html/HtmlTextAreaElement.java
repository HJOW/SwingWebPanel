package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;

/**
 * The <code>textarea</code> element (HTML Standard 4.10.11): a multi-line plain text control. Its state is the "raw value", the "dirty value flag"
 * (once the value was set, the child text no longer defines it), the "last changed by a user edit" flag that <code>maxlength</code> and
 * <code>minlength</code> checks need, and the selection. The "API value" that scripts read and that is submitted is the raw value with every line
 * break normalized to LF (submission turns them into CRLF); the default value is the text of its child text nodes.
 * <p>
 * <code>textarea</code> 요소(HTML 표준 4.10.11): 여러 줄 일반 텍스트 컨트롤. 상태는 "raw value", "dirty value flag"(값이 한 번 지정되면 자식 글이 더는 값을 정의하지
 * 않는다), <code>maxlength</code>/<code>minlength</code> 검사에 필요한 "사용자 편집으로 마지막에 바뀜" 플래그, 선택이다. 스크립트가 읽고 제출되는 "API value" 는
 * 모든 줄 바꿈을 LF 로 정규화한 raw value 이다(제출할 때 CRLF 가 된다). 기본값은 자식 텍스트 노드의 글이다.
 */
public final class HtmlTextAreaElement extends HtmlListedElement {

    /** The raw value (valid only while the dirty value flag is set). / raw value(dirty value flag 가 켜져 있을 때만 유효). */
    private String rawValue = "";

    /** The dirty value flag: set once the value was changed through the IDL attribute or by the user. / dirty value flag: IDL 속성이나 사용자가 값을 바꾸면 켜진다. */
    private boolean dirtyValue;

    /** True if the value was last changed by a user edit. / 값을 마지막으로 바꾼 것이 사용자 편집이면 true. */
    private boolean userEdited;

    /** The selection. / 선택. */
    private final TextSelection selection = new TextSelection();

    /**
     * Creates a textarea.
     * <p>
     * textarea 를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlTextAreaElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Copies the value, its flags and the selection to a clone.
     * <p>
     * 값, 값의 플래그, 선택을 복제본에 복사한다.
     *
     * @param copy the clone / 복제본
     */
    protected void copyStateTo(Element copy) {
        super.copyStateTo(copy);
        HtmlTextAreaElement t = (HtmlTextAreaElement) copy;
        t.rawValue = rawValue;
        t.dirtyValue = dirtyValue;
        t.userEdited = userEdited;
        selection.copyTo(t.selection);
    }

    /**
     * A textarea is labelable.
     * <p>
     * textarea 는 라벨을 붙일 수 있다.
     *
     * @return true / true
     */
    public boolean isLabelable() {
        return true;
    }

    /**
     * A textarea takes part in form submission.
     * <p>
     * textarea 는 폼 제출에 참여한다.
     *
     * @return true / true
     */
    boolean isSubmittable() {
        return true;
    }

    /**
     * A textarea is resettable.
     * <p>
     * textarea 는 초기화할 수 있다.
     *
     * @return true / true
     */
    boolean isResettable() {
        return true;
    }

    /**
     * A textarea takes part in the default focus order.
     * <p>
     * textarea 는 기본 포커스 순서에 참여한다.
     *
     * @return 0 / 0
     */
    int defaultTabIndex() {
        return 0;
    }

    /**
     * A textarea is mutable unless it is disabled or read-only.
     * <p>
     * textarea 는 비활성이거나 읽기 전용이 아니면 mutable 이다.
     *
     * @return true if mutable / mutable 이면 true
     */
    boolean isMutable() {
        return !isDisabled() && !hasAttribute("readonly");
    }

    /**
     * A read-only textarea is barred from constraint validation.
     * <p>
     * 읽기 전용 textarea 는 제약 검증에서 제외된다.
     *
     * @return true if read-only / 읽기 전용이면 true
     */
    boolean isBarredBySelf() {
        return hasAttribute("readonly");
    }

    /**
     * Returns the <code>type</code> IDL attribute: <code>textarea</code>.
     * <p>
     * <code>type</code> IDL 속성을 반환한다: <code>textarea</code>.
     *
     * @return "textarea" / "textarea"
     */
    public String getType() {
        return "textarea";
    }

    /**
     * Returns the <code>defaultValue</code> IDL attribute: the concatenated data of the child text nodes.
     * <p>
     * <code>defaultValue</code> IDL 속성을 반환한다: 자식 텍스트 노드 데이터를 이은 것.
     *
     * @return the default value / 기본값
     */
    public String getDefaultValue() {
        StringBuilder b = new StringBuilder();
        // 자식 중 텍스트 노드의 데이터만 이어 붙인다.
        for (Node c = getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Text) {
                b.append(((Text) c).getData());
            }
        }
        return b.toString();
    }

    /**
     * Sets <code>defaultValue</code>: replaces the children with that text.
     * <p>
     * <code>defaultValue</code> 를 지정한다: 자식을 그 글로 바꾼다.
     *
     * @param value the new default value / 새 기본값
     */
    public void setDefaultValue(String value) {
        setTextContent(value);
        stateChanged();
    }

    /**
     * Returns the raw value: the value that was set, or the default value if none was set.
     * <p>
     * raw value 를 반환한다: 지정된 값, 지정된 적이 없으면 기본값.
     *
     * @return the raw value / raw value
     */
    String getRawValue() {
        return dirtyValue ? rawValue : getDefaultValue();
    }

    /**
     * Returns the form submission value: the raw value with every line break normalized to CRLF and, if the <code>wrap</code> attribute is in the
     * Hard state, lines broken at spaces (or after <code>cols</code> characters if a word is longer) so that no line has more than
     * <code>cols</code> characters (the wrapping algorithm is up to the user agent, HTML Standard 4.10.11 "textarea wrapping transformation").
     * <p>
     * 폼 제출 값을 반환한다: 모든 줄 바꿈을 CRLF 로 정규화한 raw value. <code>wrap</code> 속성이 Hard 상태이면 한 줄이 <code>cols</code> 문자를 넘지 않도록 공백에서
     * (단어가 더 길면 <code>cols</code> 문자 뒤에서) 줄을 나눈다(줄 나누기 알고리즘은 사용자 에이전트의 몫이다).
     *
     * @return the submission value / 제출 값
     */
    String getSubmissionValue() {
        String crlf = HtmlText.toCrlf(getRawValue());
        if (!"hard".equals(getWrap())) {
            return crlf;
        }
        int cols = getCols();
        String[] lines = crlf.split("\r\n", -1);
        StringBuilder out = new StringBuilder(crlf.length() + 16);
        // 줄마다 cols 를 넘으면 마지막 공백(없으면 cols 위치)에서 나눈다.
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append("\r\n");
            }
            String line = lines[i];
            // 남은 줄이 cols 를 넘는 동안 앞부분을 잘라 낸다.
            while (line.length() > cols) {
                int cut = line.lastIndexOf(' ', cols);
                int skip = 1;
                if (cut <= 0) {
                    cut = cols;
                    skip = 0;
                }
                out.append(line.substring(0, cut)).append("\r\n");
                line = line.substring(cut + skip);
            }
            out.append(line);
        }
        return out.toString();
    }

    /**
     * Returns the <code>value</code> IDL attribute: the API value, the raw value with CRLF and CR normalized to LF.
     * <p>
     * <code>value</code> IDL 속성을 반환한다: API value, 즉 CRLF 와 CR 을 LF 로 정규화한 raw value.
     *
     * @return the value / 값
     */
    public String getValue() {
        return HtmlText.normalizeNewlines(getRawValue());
    }

    /**
     * Sets <code>value</code> from a script: the value becomes dirty, and if the API value changed the cursor moves to the end.
     * <p>
     * 스크립트에서 <code>value</code> 를 지정한다: 값이 dirty 가 되고 API value 가 바뀌었으면 커서가 끝으로 간다.
     *
     * @param value the new value / 새 값
     */
    public void setValue(String value) {
        String old = getValue();
        rawValue = value == null ? "" : value;
        dirtyValue = true;
        userEdited = false;
        String now = getValue();
        if (!now.equals(old)) {
            selection.moveToEnd(now.length());
        }
        stateChanged();
    }

    /**
     * Sets the value as a result of an edit by the user (the host calls this when the user typed, pasted or deleted text). The value becomes
     * dirty and "changed by the user", which enables the <code>maxlength</code> and <code>minlength</code> checks. The host fires the
     * <code>input</code> event afterwards with {@link HtmlControl#fireInputEvent(String, String)}.
     * <p>
     * 사용자의 편집 결과로 값을 지정한다(사용자가 글을 입력, 붙여넣기, 삭제했을 때 호스트가 부른다). 값은 dirty 가 되고 "사용자가 바꿈"이 되어
     * <code>maxlength</code>, <code>minlength</code> 검사가 켜진다. 호스트는 이어서 {@link HtmlControl#fireInputEvent(String, String)} 로
     * <code>input</code> 이벤트를 발생시킨다.
     *
     * @param value the new value / 새 값
     */
    public void setValueFromUser(String value) {
        rawValue = value == null ? "" : value;
        dirtyValue = true;
        userEdited = true;
        stateChanged();
    }

    /**
     * Returns the <code>textLength</code> IDL attribute: the length of the API value in UTF-16 code units.
     * <p>
     * <code>textLength</code> IDL 속성을 반환한다: API value 의 UTF-16 코드 단위 길이.
     *
     * @return the length / 길이
     */
    public int getTextLength() {
        return getValue().length();
    }

    /**
     * The reset algorithm: the value becomes the default value again.
     * <p>
     * 초기화 알고리즘: 값이 다시 기본값이 된다.
     */
    void resetControl() {
        String old = getValue();
        dirtyValue = false;
        userEdited = false;
        rawValue = "";
        String now = getValue();
        if (!now.equals(old)) {
            selection.moveToEnd(now.length());
        }
        stateChanged();
    }

    /**
     * Returns the <code>rows</code> IDL attribute: the attribute if it is a positive number, otherwise 2.
     * <p>
     * <code>rows</code> IDL 속성을 반환한다: 속성이 양수이면 그 값, 아니면 2.
     *
     * @return the rows / 행 수
     */
    public int getRows() {
        int v = HtmlText.parseNonNegative(getAttribute("rows"));
        return v > 0 ? v : 2;
    }

    /**
     * Sets <code>rows</code>.
     * <p>
     * <code>rows</code> 를 지정한다.
     *
     * @param value the new row count, greater than zero / 새 행 수(0 보다 커야 함)
     * @throws DomException IndexSizeError if it is zero / 0 이면 IndexSizeError
     */
    public void setRows(int value) {
        if (value <= 0) {
            throw DomException.indexSize("rows must be greater than zero");
        }
        setAttribute("rows", Integer.toString(value));
    }

    /**
     * Returns the <code>cols</code> IDL attribute: the attribute if it is a positive number, otherwise 20.
     * <p>
     * <code>cols</code> IDL 속성을 반환한다: 속성이 양수이면 그 값, 아니면 20.
     *
     * @return the columns / 열 수
     */
    public int getCols() {
        int v = HtmlText.parseNonNegative(getAttribute("cols"));
        return v > 0 ? v : 20;
    }

    /**
     * Sets <code>cols</code>.
     * <p>
     * <code>cols</code> 를 지정한다.
     *
     * @param value the new column count, greater than zero / 새 열 수(0 보다 커야 함)
     * @throws DomException IndexSizeError if it is zero / 0 이면 IndexSizeError
     */
    public void setCols(int value) {
        if (value <= 0) {
            throw DomException.indexSize("cols must be greater than zero");
        }
        setAttribute("cols", Integer.toString(value));
    }

    /**
     * Returns the <code>wrap</code> IDL attribute: <code>hard</code> or <code>soft</code>.
     * <p>
     * <code>wrap</code> IDL 속성을 반환한다: <code>hard</code> 또는 <code>soft</code>.
     *
     * @return the wrap state / wrap 상태
     */
    public String getWrap() {
        return "hard".equals(HtmlText.lower(attr("wrap"))) ? "hard" : "soft";
    }

    /**
     * Returns the maximum allowed value length, or -1 if there is none.
     * <p>
     * 허용되는 최대 값 길이를 반환한다. 없으면 -1.
     *
     * @return the maximum length / 최대 길이
     */
    public int getMaxLength() {
        return HtmlText.parseNonNegative(getAttribute("maxlength"));
    }

    /**
     * Sets <code>maxLength</code>.
     * <p>
     * <code>maxLength</code> 를 지정한다.
     *
     * @param value the maximum length, not negative / 최대 길이(음수 불가)
     * @throws DomException IndexSizeError if negative / 음수이면 IndexSizeError
     */
    public void setMaxLength(int value) {
        if (value < 0) {
            throw DomException.indexSize("maxLength must not be negative");
        }
        setAttribute("maxlength", Integer.toString(value));
    }

    /**
     * Returns the minimum required value length, or -1 if there is none.
     * <p>
     * 요구되는 최소 값 길이를 반환한다. 없으면 -1.
     *
     * @return the minimum length / 최소 길이
     */
    public int getMinLength() {
        return HtmlText.parseNonNegative(getAttribute("minlength"));
    }

    /**
     * Sets <code>minLength</code>.
     * <p>
     * <code>minLength</code> 를 지정한다.
     *
     * @param value the minimum length, not negative / 최소 길이(음수 불가)
     * @throws DomException IndexSizeError if negative / 음수이면 IndexSizeError
     */
    public void setMinLength(int value) {
        if (value < 0) {
            throw DomException.indexSize("minLength must not be negative");
        }
        setAttribute("minlength", Integer.toString(value));
    }

    /**
     * Tells whether the placeholder is displayed: a non-empty <code>placeholder</code> and an empty value.
     * <p>
     * placeholder 가 표시되는지 알려 준다: 비어 있지 않은 <code>placeholder</code> 와 빈 값.
     *
     * @return true if the placeholder is shown / placeholder 가 표시되면 true
     */
    public boolean isPlaceholderShown() {
        return attr("placeholder").length() > 0 && getValue().length() == 0;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 선택 API
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>selectionStart</code> IDL attribute.
     * <p>
     * <code>selectionStart</code> IDL 속성을 반환한다.
     *
     * @return the selection start / 선택 시작
     */
    public int getSelectionStart() {
        return selection.getStart(getValue().length());
    }

    /**
     * Returns the <code>selectionEnd</code> IDL attribute.
     * <p>
     * <code>selectionEnd</code> IDL 속성을 반환한다.
     *
     * @return the selection end / 선택 끝
     */
    public int getSelectionEnd() {
        return selection.getEnd(getValue().length());
    }

    /**
     * Returns the <code>selectionDirection</code> IDL attribute.
     * <p>
     * <code>selectionDirection</code> IDL 속성을 반환한다.
     *
     * @return forward, backward or none / forward, backward, none
     */
    public String getSelectionDirection() {
        return selection.getDirection();
    }

    /**
     * Sets <code>selectionStart</code> (the end moves along if it would be before the start).
     * <p>
     * <code>selectionStart</code> 를 지정한다(끝이 시작보다 앞서면 함께 움직인다).
     *
     * @param value the new start / 새 시작
     */
    public void setSelectionStart(int value) {
        setSelectionRange(value, Math.max(value, getSelectionEnd()), getSelectionDirection());
    }

    /**
     * Sets <code>selectionEnd</code>.
     * <p>
     * <code>selectionEnd</code> 를 지정한다.
     *
     * @param value the new end / 새 끝
     */
    public void setSelectionEnd(int value) {
        setSelectionRange(getSelectionStart(), value, getSelectionDirection());
    }

    /**
     * Sets <code>selectionDirection</code>.
     * <p>
     * <code>selectionDirection</code> 를 지정한다.
     *
     * @param value forward, backward or none / forward, backward, none
     */
    public void setSelectionDirection(String value) {
        setSelectionRange(getSelectionStart(), getSelectionEnd(), value);
    }

    /**
     * Implements <code>setSelectionRange(start, end, direction)</code>; a changed selection fires a <code>select</code> event.
     * <p>
     * <code>setSelectionRange(start, end, direction)</code> 를 구현한다. 선택이 바뀌면 <code>select</code> 이벤트가 발생한다.
     *
     * @param start     the start / 시작
     * @param end       the end / 끝
     * @param direction the direction or null / 방향 또는 null
     */
    public void setSelectionRange(int start, int end, String direction) {
        if (selection.set(start, end, direction, getValue().length())) {
            fireEvent("select", true, false);
        }
        stateChanged();
    }

    /**
     * Records the selection that the user made in the editor of the host (the caret moved, text was selected). Unlike
     * {@link #setSelectionRange(int, int, String)} it does not tell the host that the state changed (the host made the change itself), and a
     * <code>select</code> event is fired only if some text is selected afterwards.
     * <p>
     * 사용자가 호스트의 편집기에서 만든 선택(캐럿 이동, 글 선택)을 기록한다. {@link #setSelectionRange(int, int, String)} 와 달리 호스트에 상태가 바뀌었다고
     * 알리지 않으며(호스트가 직접 바꾼 것), 선택된 글이 있을 때만 <code>select</code> 이벤트를 발생시킨다.
     *
     * @param start     the start / 시작
     * @param end       the end / 끝
     * @param direction forward, backward or none / forward, backward, none
     */
    public void setSelectionFromUser(int start, int end, String direction) {
        if (selection.set(start, end, direction, getValue().length()) && start != end) {
            fireEvent("select", true, false);
        }
    }

    /**
     * Implements <code>select()</code>: selects all the text.
     * <p>
     * <code>select()</code> 를 구현한다: 글 전체를 선택한다.
     */
    public void select() {
        int len = getValue().length();
        setSelectionRange(0, len, "none");
    }

    /**
     * Implements <code>setRangeText(replacement, start, end, selectMode)</code>.
     * <p>
     * <code>setRangeText(replacement, start, end, selectMode)</code> 를 구현한다.
     *
     * @param replacement the new text / 새 글
     * @param start       the start of the range / 구간 시작
     * @param end         the end of the range / 구간 끝
     * @param mode        select, start, end or preserve (null means preserve) / select, start, end, preserve(null 은 preserve)
     * @throws DomException IndexSizeError if start is after end / start 가 end 보다 뒤이면 IndexSizeError
     */
    public void setRangeText(String replacement, int start, int end, String mode) {
        if (start > end) {
            throw DomException.indexSize("the start of the range is after its end");
        }
        String v = getValue();
        int s = Math.min(start, v.length());
        int e = Math.min(end, v.length());
        String r = replacement == null ? "" : replacement;
        String nv = v.substring(0, s) + r + v.substring(e);
        rawValue = nv;
        dirtyValue = true;
        userEdited = false;
        selection.afterReplace(mode == null ? "preserve" : mode, s, e, r.length(), v.length());
        stateChanged();
    }

    /**
     * Implements <code>setRangeText(replacement)</code>: replaces the selected text.
     * <p>
     * <code>setRangeText(replacement)</code> 를 구현한다: 선택한 글을 바꾼다.
     *
     * @param replacement the new text / 새 글
     */
    public void setRangeText(String replacement) {
        setRangeText(replacement, getSelectionStart(), getSelectionEnd(), "preserve");
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 제약 검증
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Computes the violated constraints: <code>valueMissing</code>, <code>tooLong</code> and <code>tooShort</code>.
     * <p>
     * 어긴 제약을 계산한다: <code>valueMissing</code>, <code>tooLong</code>, <code>tooShort</code>.
     *
     * @return the flags / 플래그
     */
    int computeFlags() {
        int f = 0;
        String v = getValue();
        if (hasAttribute("required") && isMutable() && v.length() == 0) {
            f |= ValidityState.VALUE_MISSING;
        }
        if (dirtyValue && userEdited && v.length() > 0) {
            int max = getMaxLength();
            int min = getMinLength();
            if (max >= 0 && v.length() > max) {
                f |= ValidityState.TOO_LONG;
            }
            if (min >= 0 && v.length() < min) {
                f |= ValidityState.TOO_SHORT;
            }
        }
        return f;
    }

    /**
     * Returns the message of a violated textarea constraint.
     * <p>
     * 어긴 textarea 제약의 메시지를 반환한다.
     *
     * @param f the flags / 플래그
     * @return the message / 메시지
     */
    String messageFor(int f) {
        if ((f & ValidityState.TOO_LONG) != 0) {
            return "Please shorten this text to " + getMaxLength() + " characters or less (you are currently using " + getTextLength()
                    + " characters).";
        }
        if ((f & ValidityState.TOO_SHORT) != 0) {
            return "Please lengthen this text to " + getMinLength() + " characters or more (you are currently using " + getTextLength()
                    + " characters).";
        }
        return super.messageFor(f);
    }
}
