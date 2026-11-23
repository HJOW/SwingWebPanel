package org.duckdns.hjow.swingwebpanel.dom.html;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>input</code> element (HTML Standard 4.10.5), the most varied form control: its <code>type</code> attribute selects one of 22
 * states ({@link InputKinds}), and the state decides the value mode, the value sanitization, the constraints and the activation behavior.
 * What this class keeps besides the attributes is the state that the standard defines for the control: the value (and the "dirty value flag"
 * that stops the <code>value</code> attribute from changing it), the checkedness (and its dirty flag), the indeterminateness, the list of
 * selected files, the selection of text controls and the "bad input" report of the host. Every IDL attribute and method works on that
 * state, never on the content attribute alone.
 * <p>
 * <code>input</code> 요소(HTML 표준 4.10.5): 가장 다양한 폼 컨트롤. <code>type</code> 속성이 22개 상태({@link InputKinds}) 중 하나를 고르고, 상태가 value 모드, 값 정리,
 * 제약, 활성화 동작을 정한다. 이 클래스가 속성 외에 가지는 것은 표준이 컨트롤에 정의한 상태이다: 값(과 <code>value</code> 속성이 값을 바꾸지 못하게 하는 "dirty value
 * flag"), checkedness(와 그 dirty 플래그), indeterminateness, 선택한 파일 목록, 텍스트 컨트롤의 선택, 호스트의 "bad input" 보고. 모든 IDL 속성과 메서드는
 * 콘텐츠 속성만이 아니라 이 상태에 대해 동작한다.
 */
public final class HtmlInputElement extends HtmlListedElement {

    /** The state of the type attribute. / type 속성의 상태. */
    private int kind = InputKinds.TEXT;

    /** The value in the value mode (valid while the dirty value flag is set). / value 모드의 값(dirty value flag 가 켜져 있을 때 유효). */
    private String value = "";

    /** The dirty value flag: set once the value was changed through the IDL attribute or by the user. / dirty value flag: IDL 속성이나 사용자가 값을 바꾸면 켜진다. */
    private boolean dirtyValue;

    /** True if the value was last changed by a user edit. / 값을 마지막으로 바꾼 것이 사용자 편집이면 true. */
    private boolean userEdited;

    /** The checkedness (whether a checkbox or radio button is checked). / checkedness(체크박스나 라디오 버튼이 체크되어 있는지). */
    private boolean checked;

    /** The dirty checkedness flag: set once the checkedness was changed by script or the user. / dirty checkedness flag: 스크립트나 사용자가 checkedness 를 바꾸면 켜진다. */
    private boolean dirtyChecked;

    /** The indeterminateness of a checkbox (a mixed state shown by the UI). / 체크박스의 indeterminateness(UI 가 보여 주는 혼합 상태). */
    private boolean indeterminate;

    /** True if the host reported text that cannot be converted to a value. / 호스트가 값으로 바꿀 수 없는 글을 보고했으면 true. */
    private boolean badInput;

    /** The selection of a text control. / 텍스트 컨트롤의 선택. */
    private final TextSelection selection = new TextSelection();

    /** The selected files of a file upload control. / 파일 업로드 컨트롤에서 선택한 파일. */
    private List<FileEntry> files = Collections.emptyList();

    /** The cached files list object. / 캐시한 files 목록 객체. */
    private FileList fileList;

    /** The selected coordinate of an image button (x). / 이미지 버튼의 선택 좌표(x). */
    private int imageX;

    /** The selected coordinate of an image button (y). / 이미지 버튼의 선택 좌표(y). */
    private int imageY;

    /** The state saved by the legacy-pre-activation behavior: checkedness, indeterminateness and the checked radio button of the group. / legacy-pre-activation 동작이 저장한 상태: checkedness, indeterminateness, 그룹에서 체크되어 있던 라디오 버튼. */
    private Object[] preActivation;

    /**
     * Creates an input.
     * <p>
     * input 을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlInputElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Copies the whole state to a clone ("the cloning steps": value, dirty value flag, checkedness, dirty checkedness flag), and also the
     * indeterminateness, the selection and the selected files.
     * <p>
     * 전체 상태를 복제본에 복사한다("cloning steps": 값, dirty value flag, checkedness, dirty checkedness flag). indeterminateness, 선택, 선택한 파일도 복사한다.
     *
     * @param copy the clone / 복제본
     */
    protected void copyStateTo(Element copy) {
        super.copyStateTo(copy);
        HtmlInputElement c = (HtmlInputElement) copy;
        c.kind = kind;
        c.value = value;
        c.dirtyValue = dirtyValue;
        c.userEdited = userEdited;
        c.checked = checked;
        c.dirtyChecked = dirtyChecked;
        c.indeterminate = indeterminate;
        c.files = files;
        selection.copyTo(c.selection);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 종류(type)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the state of the type attribute (an {@link InputKinds} constant).
     * <p>
     * type 속성의 상태를 반환한다({@link InputKinds} 상수).
     *
     * @return the state / 상태
     */
    int kind() {
        return kind;
    }

    /**
     * Returns the <code>type</code> IDL attribute: the keyword of the current state (<code>text</code> for a missing or unknown value).
     * <p>
     * <code>type</code> IDL 속성을 반환한다: 현재 상태의 키워드(없거나 모르는 값이면 <code>text</code>).
     *
     * @return the type / 종류
     */
    public String getType() {
        return InputKinds.nameOf(kind);
    }

    /**
     * Sets the <code>type</code> attribute.
     * <p>
     * <code>type</code> 속성을 지정한다.
     *
     * @param value the new type / 새 종류
     */
    public void setType(String value) {
        setAttribute("type", value);
    }

    /**
     * Reacts to the attributes that define the state of the control.
     * <p>
     * 컨트롤의 상태를 정의하는 속성에 반응한다.
     *
     * @param local     attribute local name / 속성 로컬 이름
     * @param namespace attribute namespace / 속성 네임스페이스
     * @param oldValue  previous value or null / 이전 값 또는 null
     * @param newValue  new value or null / 새 값 또는 null
     */
    protected void attributeChanged(String local, String namespace, String oldValue, String newValue) {
        if (namespace != null) {
            return;
        }
        if ("type".equals(local)) {
            typeChanged(InputKinds.kindOf(newValue));
        } else if ("checked".equals(local)) {
            if (!dirtyChecked) {
                checked = newValue != null;
                if (checked && kind == InputKinds.RADIO) {
                    uncheckOthers();
                }
                stateChanged();
            }
        } else if ("name".equals(local)) {
            if (kind == InputKinds.RADIO && checked) {
                uncheckOthers();
            }
            stateChanged();
        } else if ("value".equals(local)) {
            if (InputKinds.valueMode(kind) == InputKinds.MODE_VALUE && !dirtyValue) {
                selection.moveToEnd(getValue().length());
            }
            stateChanged();
        } else if (("min".equals(local) || "max".equals(local) || "step".equals(local)) && kind == InputKinds.RANGE && dirtyValue) {
            // 범위 입력의 값은 min, max, step 이 바뀌면 다시 정리해야 한다.
            value = sanitize(value);
            stateChanged();
        } else if ("multiple".equals(local) && kind == InputKinds.EMAIL && dirtyValue) {
            value = sanitize(value);
            stateChanged();
        }
    }

    /**
     * Runs the steps that the standard requires when the state of the type attribute changes (value mode transitions, sanitization,
     * radio group, selection and files).
     * <p>
     * type 속성의 상태가 바뀔 때 표준이 요구하는 단계(값 모드 전환, 값 정리, 라디오 그룹, 선택, 파일)를 실행한다.
     *
     * @param newKind the new state / 새 상태
     */
    private void typeChanged(int newKind) {
        int oldKind = kind;
        if (oldKind == newKind) {
            return;
        }
        int oldMode = InputKinds.valueMode(oldKind);
        int newMode = InputKinds.valueMode(newKind);
        String oldValue = getValue();
        boolean wasSelectable = InputKinds.selectionApplies(oldKind);
        kind = newKind;
        if (oldMode == InputKinds.MODE_VALUE && oldValue.length() > 0 && (newMode == InputKinds.MODE_DEFAULT || newMode == InputKinds.MODE_DEFAULT_ON)) {
            // value 모드에서 default 모드로 가면 값을 value 속성에 저장한다.
            setAttribute("value", oldValue);
        } else if (oldMode != InputKinds.MODE_VALUE && newMode == InputKinds.MODE_VALUE) {
            // 다른 모드에서 value 모드로 오면 value 속성의 값을 쓰고 dirty 를 끈다.
            dirtyValue = false;
            userEdited = false;
        } else if (oldMode != InputKinds.MODE_FILENAME && newMode == InputKinds.MODE_FILENAME) {
            files = Collections.emptyList();
            fileList = null;
        }
        if (oldMode == InputKinds.MODE_FILENAME && newMode != InputKinds.MODE_FILENAME) {
            files = Collections.emptyList();
            fileList = null;
        }
        if (newMode == InputKinds.MODE_VALUE && dirtyValue) {
            value = sanitize(value);
        }
        badInput = false;
        if (!wasSelectable && InputKinds.selectionApplies(newKind)) {
            selection.moveToStart();
        }
        if (newKind == InputKinds.RADIO && checked) {
            uncheckOthers();
        }
        stateChanged();
    }

    /**
     * Forgets the owner that the parser assigned and applies the radio button group rule to an inserted checked radio button.
     * <p>
     * 파서가 지정한 소유자를 잊고, 삽입된 체크된 라디오 버튼에 라디오 그룹 규칙을 적용한다.
     */
    void insertedIntoTree() {
        super.insertedIntoTree();
        if (kind == InputKinds.RADIO && checked) {
            uncheckOthers();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 값
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>value</code> IDL attribute in the way of the current value mode: the value, the <code>value</code> attribute (with the
     * default <code>on</code> for checkboxes and radio buttons), or <code>C:\fakepath\</code> and the name of the first selected file.
     * <p>
     * 현재 value 모드에 맞게 <code>value</code> IDL 속성을 반환한다: 값, <code>value</code> 속성(체크박스와 라디오는 기본 <code>on</code>), 또는
     * <code>C:\fakepath\</code> 와 첫 선택 파일의 이름.
     *
     * @return the value / 값
     */
    public String getValue() {
        switch (InputKinds.valueMode(kind)) {
        case InputKinds.MODE_VALUE:
            return dirtyValue ? value : sanitize(attr("value"));
        case InputKinds.MODE_DEFAULT:
            return attr("value");
        case InputKinds.MODE_DEFAULT_ON:
            String v = getAttribute("value");
            return v == null ? "on" : v;
        default:
            return files.isEmpty() ? "" : "C:\\fakepath\\" + files.get(0).getName();
        }
    }

    /**
     * Sets <code>value</code> in the way of the current value mode. In the value mode the value is sanitized and becomes dirty (the cursor
     * moves to the end if the value changed); in the default modes the <code>value</code> attribute is set; a file upload control accepts only the empty
     * string (which empties the selection of files).
     * <p>
     * 현재 value 모드에 맞게 <code>value</code> 를 지정한다. value 모드에서는 값을 정리하고 dirty 로 만든다(값이 바뀌었으면 커서가 끝으로 간다). default 모드에서는
     * <code>value</code> 속성을 지정한다. 파일 업로드 컨트롤은 빈 문자열만 받으며 그러면 파일 선택이 비워진다.
     *
     * @param v the new value / 새 값
     * @throws DomException InvalidStateError for a file upload control and a non-empty value / 파일 업로드 컨트롤에 빈 문자열이 아닌 값이면 InvalidStateError
     */
    public void setValue(String v) {
        String nv = v == null ? "" : v;
        switch (InputKinds.valueMode(kind)) {
        case InputKinds.MODE_VALUE:
            String old = getValue();
            value = sanitize(nv);
            dirtyValue = true;
            userEdited = false;
            badInput = false;
            if (!value.equals(old)) {
                selection.moveToEnd(value.length());
            }
            stateChanged();
            break;
        case InputKinds.MODE_DEFAULT:
        case InputKinds.MODE_DEFAULT_ON:
            setAttribute("value", nv);
            break;
        default:
            if (nv.length() != 0) {
                throw DomException.invalidState("a file upload control cannot be given a value other than the empty string");
            }
            files = Collections.emptyList();
            fileList = null;
            stateChanged();
            break;
        }
    }

    /**
     * Sets the value as a result of an edit by the user (typing, pasting, a picker). Text that cannot be converted to a value in the
     * number and date states (the visible text is not a valid number or date) makes the value empty and sets the "bad input" flag. The host
     * fires the <code>input</code> event afterwards with {@link HtmlControl#fireInputEvent(String, String)}.
     * <p>
     * 사용자의 편집(입력, 붙여넣기, 선택기) 결과로 값을 지정한다. number 와 날짜 상태에서 값으로 바꿀 수 없는 글(보이는 글이 올바른 숫자나 날짜가 아님)은 값을 비우고
     * "bad input" 플래그를 켠다. 호스트는 이어서 {@link HtmlControl#fireInputEvent(String, String)} 로 <code>input</code> 이벤트를 발생시킨다.
     *
     * @param text the text that the user produced / 사용자가 만든 글
     */
    public void setValueFromUser(String text) {
        if (InputKinds.valueMode(kind) != InputKinds.MODE_VALUE) {
            return;
        }
        String t = text == null ? "" : text;
        String sanitized = sanitize(t);
        boolean parsed = InputKinds.isNumeric(kind) || kind == InputKinds.COLOR;
        badInput = parsed && t.length() > 0 && sanitized.length() == 0 && kind != InputKinds.RANGE && kind != InputKinds.COLOR;
        value = sanitized;
        dirtyValue = true;
        userEdited = true;
        stateChanged();
    }

    /**
     * Tells the control that the host shows text that cannot be converted to a value (or that it no longer does).
     * <p>
     * 호스트가 값으로 바꿀 수 없는 글을 보여 주고 있음(또는 더는 아님)을 컨트롤에 알린다.
     *
     * @param bad true if the visible text is not a valid value / 보이는 글이 올바른 값이 아니면 true
     */
    public void setBadInput(boolean bad) {
        badInput = bad;
        stateChanged();
    }

    /**
     * Returns the <code>defaultValue</code> IDL attribute: the <code>value</code> attribute.
     * <p>
     * <code>defaultValue</code> IDL 속성을 반환한다: <code>value</code> 속성.
     *
     * @return the attribute value, empty if absent / 속성 값. 없으면 빈 문자열
     */
    public String getDefaultValue() {
        return attr("value");
    }

    /**
     * Sets the <code>value</code> attribute.
     * <p>
     * <code>value</code> 속성을 지정한다.
     *
     * @param v the new default value / 새 기본값
     */
    public void setDefaultValue(String v) {
        setAttribute("value", v);
    }

    /**
     * The value sanitization algorithm of the current state.
     * <p>
     * 현재 상태의 값 정리 알고리즘.
     *
     * @param v the value / 값
     * @return the sanitized value / 정리한 값
     */
    private String sanitize(String v) {
        switch (kind) {
        case InputKinds.TEXT:
        case InputKinds.SEARCH:
        case InputKinds.TEL:
        case InputKinds.PASSWORD:
            return HtmlText.stripNewlines(v);
        case InputKinds.URL:
            return HtmlText.strip(HtmlText.stripNewlines(v));
        case InputKinds.EMAIL:
            if (hasAttribute("multiple")) {
                String[] parts = HtmlText.stripNewlines(v).split(",", -1);
                StringBuilder b = new StringBuilder();
                // 쉼표로 나눈 각 조각의 앞뒤 공백을 지우고 쉼표 하나로 다시 잇는다.
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        b.append(',');
                    }
                    b.append(HtmlText.strip(parts[i]));
                }
                return b.toString();
            }
            return HtmlText.strip(HtmlText.stripNewlines(v));
        case InputKinds.DATE:
            return DateTimeSyntax.parseDate(v) != null ? v : "";
        case InputKinds.MONTH:
            return DateTimeSyntax.parseMonth(v) != null ? v : "";
        case InputKinds.WEEK:
            return DateTimeSyntax.parseWeek(v) != null ? v : "";
        case InputKinds.TIME:
            return DateTimeSyntax.parseTime(v) != null ? v : "";
        case InputKinds.DATETIME_LOCAL:
            Long l = DateTimeSyntax.parseLocal(v);
            return l == null ? "" : DateTimeSyntax.formatLocal(l.longValue());
        case InputKinds.NUMBER:
            return NumberSyntax.parse(v) != null ? v : "";
        case InputKinds.RANGE:
            return sanitizeRange(v);
        case InputKinds.COLOR:
            return isSimpleColor(v) ? HtmlText.lower(v) : "#000000";
        default:
            return v;
        }
    }

    /**
     * Tells whether a string is a valid simple color: <code>#</code> and six hexadecimal digits.
     * <p>
     * 문자열이 올바른 simple color 인지 알려 준다: <code>#</code> 와 16진수 여섯 자리.
     *
     * @param v the text / 문자열
     * @return true if valid / 올바르면 true
     */
    static boolean isSimpleColor(String v) {
        if (v.length() != 7 || v.charAt(0) != '#') {
            return false;
        }
        // 나머지 여섯 문자가 모두 16진수인지 확인한다.
        for (int i = 1; i < 7; i++) {
            char c = v.charAt(i);
            if (!(HtmlText.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The value sanitization algorithm of the range state: an invalid value becomes the default (the middle of the range), then the value is
     * limited to the range and moved onto the nearest step.
     * <p>
     * range 상태의 값 정리 알고리즘: 올바르지 않은 값은 기본값(범위의 중간)이 되고, 그다음 값을 범위로 제한하고 가장 가까운 step 으로 옮긴다.
     *
     * @param v the value / 값
     * @return the sanitized value / 정리한 값
     */
    private String sanitizeRange(String v) {
        BigDecimal min = minimum();
        BigDecimal max = maximum();
        boolean reversed = max.compareTo(min) < 0;
        BigDecimal n = NumberSyntax.parse(v);
        if (n == null) {
            n = reversed ? min : min.add(max.subtract(min).divide(BigDecimal.valueOf(2)));
        }
        if (n.compareTo(min) < 0) {
            n = min;
        } else if (!reversed && n.compareTo(max) > 0) {
            n = max;
        }
        BigDecimal step = allowedStep();
        if (step != null) {
            BigDecimal base = stepBase();
            if (n.subtract(base).remainder(step).signum() != 0) {
                n = nearestStep(n, base, step, min, reversed ? null : max);
            }
        }
        return NumberSyntax.format(n);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 수치 상태: min, max, step
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the minimum: the <code>min</code> attribute converted to a number, otherwise the default minimum of the state, otherwise null.
     * <p>
     * 최솟값을 반환한다: 숫자로 바꾼 <code>min</code> 속성, 없으면 상태의 기본 최솟값, 없으면 null.
     *
     * @return the minimum or null / 최솟값 또는 null
     */
    BigDecimal minimum() {
        BigDecimal m = InputKinds.toNumber(kind, getAttribute("min"));
        if (m != null) {
            return m;
        }
        return kind == InputKinds.RANGE ? BigDecimal.ZERO : null;
    }

    /**
     * Returns the maximum: the <code>max</code> attribute converted to a number, otherwise the default maximum of the state, otherwise null.
     * <p>
     * 최댓값을 반환한다: 숫자로 바꾼 <code>max</code> 속성, 없으면 상태의 기본 최댓값, 없으면 null.
     *
     * @return the maximum or null / 최댓값 또는 null
     */
    BigDecimal maximum() {
        BigDecimal m = InputKinds.toNumber(kind, getAttribute("max"));
        if (m != null) {
            return m;
        }
        return kind == InputKinds.RANGE ? BigDecimal.valueOf(100) : null;
    }

    /**
     * Returns the allowed value step, or null if the <code>step</code> attribute is <code>any</code> or the state has no step.
     * <p>
     * 허용 값 step 을 반환한다. <code>step</code> 속성이 <code>any</code> 이거나 상태에 step 이 없으면 null.
     *
     * @return the step or null / step 또는 null
     */
    BigDecimal allowedStep() {
        if (!InputKinds.isNumeric(kind)) {
            return null;
        }
        BigDecimal scale = InputKinds.stepScale(kind);
        BigDecimal dflt = InputKinds.defaultStep(kind).multiply(scale);
        String s = getAttribute("step");
        if (s == null) {
            return dflt;
        }
        if ("any".equals(HtmlText.lower(s))) {
            return null;
        }
        BigDecimal p = NumberSyntax.parse(s);
        return p == null || p.signum() <= 0 ? dflt : p.multiply(scale);
    }

    /**
     * Returns the step base: the <code>min</code> attribute, else the <code>value</code> attribute, else the default step base, else zero.
     * <p>
     * step 기준값을 반환한다: <code>min</code> 속성, 없으면 <code>value</code> 속성, 없으면 기본 step 기준값, 없으면 0.
     *
     * @return the step base / step 기준값
     */
    BigDecimal stepBase() {
        BigDecimal m = InputKinds.toNumber(kind, getAttribute("min"));
        if (m != null) {
            return m;
        }
        BigDecimal v = InputKinds.toNumber(kind, getAttribute("value"));
        if (v != null) {
            return v;
        }
        BigDecimal d = InputKinds.defaultStepBase(kind);
        return d != null ? d : BigDecimal.ZERO;
    }

    /**
     * Returns the largest value on a step that is not above a limit.
     * <p>
     * 한계 이하의 step 위 값 중 가장 큰 것을 반환한다.
     *
     * @param n    the limit / 한계
     * @param base the step base / step 기준값
     * @param step the step / step
     * @return the aligned value / step 위의 값
     */
    private static BigDecimal floorStep(BigDecimal n, BigDecimal base, BigDecimal step) {
        BigDecimal k = n.subtract(base).divide(step, 0, RoundingMode.FLOOR);
        return base.add(k.multiply(step));
    }

    /**
     * Returns the smallest value on a step that is not below a limit.
     * <p>
     * 한계 이상의 step 위 값 중 가장 작은 것을 반환한다.
     *
     * @param n    the limit / 한계
     * @param base the step base / step 기준값
     * @param step the step / step
     * @return the aligned value / step 위의 값
     */
    private static BigDecimal ceilStep(BigDecimal n, BigDecimal base, BigDecimal step) {
        BigDecimal k = n.subtract(base).divide(step, 0, RoundingMode.CEILING);
        return base.add(k.multiply(step));
    }

    /**
     * Returns the value on a step that is nearest to a number (the greater one if two are equally near) and inside the limits.
     * <p>
     * 숫자에 가장 가까운 step 위의 값(같은 거리이면 큰 쪽)을 한계 안에서 반환한다.
     *
     * @param n    the number / 수
     * @param base the step base / step 기준값
     * @param step the step / step
     * @param min  the minimum or null / 최솟값 또는 null
     * @param max  the maximum or null / 최댓값 또는 null
     * @return the aligned value, or n if no value fits / step 위의 값. 맞는 값이 없으면 n
     */
    private static BigDecimal nearestStep(BigDecimal n, BigDecimal base, BigDecimal step, BigDecimal min, BigDecimal max) {
        BigDecimal lo = floorStep(n, base, step);
        BigDecimal hi = lo.add(step);
        BigDecimal cand = hi.subtract(n).compareTo(n.subtract(lo)) <= 0 ? hi : lo;
        if (min != null && cand.compareTo(min) < 0) {
            cand = ceilStep(min, base, step);
        }
        if (max != null && cand.compareTo(max) > 0) {
            cand = floorStep(max, base, step);
        }
        if ((min != null && cand.compareTo(min) < 0) || (max != null && cand.compareTo(max) > 0)) {
            return n;
        }
        return cand;
    }

    /**
     * Returns the <code>valueAsNumber</code> IDL attribute.
     * <p>
     * <code>valueAsNumber</code> IDL 속성을 반환한다.
     *
     * @return the number, or NaN if it does not apply or the value is not a number / 수. 적용되지 않거나 값이 수가 아니면 NaN
     */
    public double getValueAsNumber() {
        if (!InputKinds.isNumeric(kind)) {
            return Double.NaN;
        }
        BigDecimal n = InputKinds.toNumber(kind, getValue());
        return n == null ? Double.NaN : n.doubleValue();
    }

    /**
     * Sets <code>valueAsNumber</code>.
     * <p>
     * <code>valueAsNumber</code> 를 지정한다.
     *
     * @param d the number; NaN clears the value / 수. NaN 이면 값을 비운다
     * @throws DomException InvalidStateError if the state is neither numeric nor date-based / 상태가 수치도 날짜도 아니면 InvalidStateError
     */
    public void setValueAsNumber(double d) {
        if (!InputKinds.isNumeric(kind)) {
            throw DomException.invalidState("valueAsNumber does not apply to this type of input");
        }
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            setValue("");
        } else if (kind == InputKinds.NUMBER || kind == InputKinds.RANGE) {
            setValue(NumberSyntax.format(d));
        } else {
            setValue(InputKinds.fromNumber(kind, BigDecimal.valueOf((long) Math.floor(d))));
        }
    }

    /**
     * Returns the <code>valueAsDate</code> IDL attribute as milliseconds since the epoch.
     * <p>
     * <code>valueAsDate</code> IDL 속성을 에포크 이후 밀리초로 반환한다.
     *
     * @return the time value, or null if it does not apply or the value is not a date / 시간값. 적용되지 않거나 값이 날짜가 아니면 null
     */
    public Double getValueAsDateMillis() {
        if (!InputKinds.dateApplies(kind)) {
            return null;
        }
        BigDecimal n = InputKinds.toNumber(kind, getValue());
        if (n == null) {
            return null;
        }
        if (kind == InputKinds.MONTH) {
            long months = n.longValue();
            long y = 1970 + DateTimeSyntax.floorDiv(months, 12);
            int m = (int) DateTimeSyntax.floorMod(months, 12) + 1;
            return Double.valueOf((double) (DateTimeSyntax.daysFromCivil(y, m, 1) * DateTimeSyntax.MS_PER_DAY));
        }
        return Double.valueOf(n.doubleValue());
    }

    /**
     * Sets <code>valueAsDate</code> from a time value in milliseconds since the epoch.
     * <p>
     * 에포크 이후 밀리초의 시간값으로 <code>valueAsDate</code> 를 지정한다.
     *
     * @param ms the time value, or null or NaN to clear the value / 시간값. null 이나 NaN 이면 값을 비운다
     * @throws DomException InvalidStateError if the state is not date-based / 날짜 기반 상태가 아니면 InvalidStateError
     */
    public void setValueAsDateMillis(Double ms) {
        if (!InputKinds.dateApplies(kind)) {
            throw DomException.invalidState("valueAsDate does not apply to this type of input");
        }
        if (ms == null || Double.isNaN(ms.doubleValue()) || Double.isInfinite(ms.doubleValue())) {
            setValue("");
            return;
        }
        long t = (long) Math.floor(ms.doubleValue());
        switch (kind) {
        case InputKinds.DATE:
            setValue(DateTimeSyntax.formatDate(t));
            break;
        case InputKinds.MONTH:
            long[] c = DateTimeSyntax.civilFromDays(DateTimeSyntax.floorDiv(t, DateTimeSyntax.MS_PER_DAY));
            setValue(DateTimeSyntax.formatMonth((c[0] - 1970) * 12 + (c[1] - 1)));
            break;
        case InputKinds.WEEK:
            setValue(DateTimeSyntax.formatWeek(t));
            break;
        default:
            setValue(DateTimeSyntax.formatTime(t));
            break;
        }
    }

    /**
     * Implements <code>stepUp(n)</code>.
     * <p>
     * <code>stepUp(n)</code> 을 구현한다.
     *
     * @param n the number of steps / step 수
     * @throws DomException InvalidStateError if the method does not apply or there is no allowed step / 적용되지 않거나 허용 step 이 없으면 InvalidStateError
     */
    public void stepUp(int n) {
        step(n, false);
    }

    /**
     * Implements <code>stepDown(n)</code>.
     * <p>
     * <code>stepDown(n)</code> 을 구현한다.
     *
     * @param n the number of steps / step 수
     * @throws DomException InvalidStateError if the method does not apply or there is no allowed step / 적용되지 않거나 허용 step 이 없으면 InvalidStateError
     */
    public void stepDown(int n) {
        step(n, true);
    }

    /**
     * The algorithm of <code>stepUp</code> and <code>stepDown</code> (HTML Standard 4.10.5.4).
     * <p>
     * <code>stepUp</code>, <code>stepDown</code> 의 알고리즘(HTML 표준 4.10.5.4).
     *
     * @param n    the number of steps / step 수
     * @param down true for stepDown / stepDown 이면 true
     */
    private void step(int n, boolean down) {
        if (!InputKinds.isNumeric(kind)) {
            throw DomException.invalidState("stepUp and stepDown do not apply to this type of input");
        }
        BigDecimal step = allowedStep();
        if (step == null) {
            throw DomException.invalidState("the step attribute is any");
        }
        BigDecimal min = minimum();
        BigDecimal max = maximum();
        if (min != null && max != null && min.compareTo(max) > 0) {
            return;
        }
        BigDecimal base = stepBase();
        if (min != null && max != null && ceilStep(min, base, step).compareTo(max) > 0) {
            return;
        }
        BigDecimal v = InputKinds.toNumber(kind, getValue());
        BigDecimal value = v == null ? BigDecimal.ZERO : v;
        BigDecimal before = value;
        if (value.subtract(base).remainder(step).signum() != 0) {
            value = down ? floorStep(value, base, step) : ceilStep(value, base, step);
        } else {
            BigDecimal delta = step.multiply(BigDecimal.valueOf(n));
            value = down ? value.subtract(delta) : value.add(delta);
            if (min != null && value.compareTo(min) < 0) {
                value = ceilStep(min, base, step);
            }
            if (max != null && value.compareTo(max) > 0) {
                value = floorStep(max, base, step);
            }
        }
        if ((down && value.compareTo(before) > 0) || (!down && value.compareTo(before) < 0)) {
            return;
        }
        setValue(InputKinds.fromNumber(kind, value));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // checkedness, 라디오 그룹
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>checked</code> IDL attribute: the checkedness.
     * <p>
     * <code>checked</code> IDL 속성을 반환한다: checkedness.
     *
     * @return true if checked / 체크되었으면 true
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Sets <code>checked</code>: the checkedness changes and becomes dirty (the <code>checked</code> attribute no longer changes it); a checked
     * radio button unchecks the others of its group.
     * <p>
     * <code>checked</code> 를 지정한다: checkedness 가 바뀌고 dirty 가 된다(<code>checked</code> 속성이 더는 바꾸지 못한다). 체크된 라디오 버튼은 그룹의 다른 버튼의
     * 체크를 푼다.
     *
     * @param value the new checkedness / 새 checkedness
     */
    public void setChecked(boolean value) {
        checked = value;
        dirtyChecked = true;
        if (checked && kind == InputKinds.RADIO) {
            uncheckOthers();
        }
        stateChanged();
    }

    /**
     * Returns the <code>defaultChecked</code> IDL attribute: whether the <code>checked</code> attribute is present.
     * <p>
     * <code>defaultChecked</code> IDL 속성을 반환한다: <code>checked</code> 속성이 있는지.
     *
     * @return true if the attribute exists / 속성이 있으면 true
     */
    public boolean isDefaultChecked() {
        return hasAttribute("checked");
    }

    /**
     * Sets or removes the <code>checked</code> attribute.
     * <p>
     * <code>checked</code> 속성을 지정하거나 제거한다.
     *
     * @param value true to add the attribute / 속성을 추가하려면 true
     */
    public void setDefaultChecked(boolean value) {
        if (value) {
            setAttribute("checked", "");
        } else {
            removeAttribute("checked");
        }
    }

    /**
     * Returns the <code>indeterminate</code> IDL attribute.
     * <p>
     * <code>indeterminate</code> IDL 속성을 반환한다.
     *
     * @return the indeterminateness / indeterminateness
     */
    public boolean isIndeterminate() {
        return indeterminate;
    }

    /**
     * Sets <code>indeterminate</code>.
     * <p>
     * <code>indeterminate</code> 를 지정한다.
     *
     * @param value the new indeterminateness / 새 indeterminateness
     */
    public void setIndeterminate(boolean value) {
        indeterminate = value;
        stateChanged();
    }

    /**
     * Returns the members of the radio button group of this element (including itself): the radio buttons with the same non-empty name and
     * the same form owner (or without a form owner) in the same tree, in tree order. An element without a name is alone in its group.
     * <p>
     * 이 요소의 라디오 버튼 그룹의 구성원(자신 포함)을 반환한다: 같은 트리에서 이름이 같고(비어 있지 않음) 폼 소유자가 같은(또는 없는) 라디오 버튼, 트리 순서.
     * 이름이 없는 요소는 그룹에 혼자 있다.
     *
     * @return the group members / 그룹 구성원
     */
    List<HtmlInputElement> radioGroup() {
        List<HtmlInputElement> out = new ArrayList<HtmlInputElement>();
        String name = getAttribute("name");
        if (kind != InputKinds.RADIO || name == null || name.length() == 0) {
            out.add(this);
            return out;
        }
        HtmlFormElement form = getForm();
        Node root = getRootNode();
        // 같은 트리의 모든 노드를 보며 이름과 폼 소유자가 같은 라디오 버튼을 모은다.
        for (Node n = Node.nextInTree(root, root); n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof HtmlInputElement) {
                HtmlInputElement i = (HtmlInputElement) n;
                if (i.kind == InputKinds.RADIO && name.equalsIgnoreCase(i.getAttribute("name")) && i.getForm() == form) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    /**
     * Returns the members of the radio button group of this element (including itself) for the host: the arrow keys move between them.
     * <p>
     * 호스트를 위해 이 요소의 라디오 버튼 그룹의 구성원(자신 포함)을 반환한다: 화살표 키가 그 사이를 옮겨 다닌다.
     *
     * @return the group members, in tree order / 그룹 구성원. 트리 순서
     */
    public List<HtmlInputElement> getRadioGroup() {
        return radioGroup();
    }

    /**
     * Unchecks the other checked radio buttons of the group (the dirty checkedness flags are not touched).
     * <p>
     * 그룹의 다른 체크된 라디오 버튼의 체크를 푼다(dirty checkedness 플래그는 건드리지 않는다).
     */
    private void uncheckOthers() {
        List<HtmlInputElement> g = radioGroup();
        // 그룹의 다른 구성원 중 체크된 것을 푼다.
        for (int i = 0; i < g.size(); i++) {
            HtmlInputElement o = g.get(i);
            if (o != this && o.checked) {
                o.checked = false;
                o.stateChanged();
            }
        }
    }

    /**
     * Tells whether any radio button of the group is checked.
     * <p>
     * 그룹의 라디오 버튼 중 체크된 것이 있는지 알려 준다.
     *
     * @return true if one is checked / 하나라도 체크되었으면 true
     */
    boolean groupHasChecked() {
        List<HtmlInputElement> g = radioGroup();
        // 그룹을 훑으며 체크된 구성원을 찾는다.
        for (int i = 0; i < g.size(); i++) {
            if (g.get(i).checked) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 파일
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>files</code> IDL attribute: the same {@link FileList} object until the selection changes.
     * <p>
     * <code>files</code> IDL 속성을 반환한다: 선택이 바뀌기 전까지 같은 {@link FileList} 객체.
     *
     * @return the list, or null if this is not a file upload control / 목록. 파일 업로드 컨트롤이 아니면 null
     */
    public FileList getFiles() {
        if (kind != InputKinds.FILE) {
            return null;
        }
        if (fileList == null) {
            fileList = new FileList(files);
        }
        return fileList;
    }

    /**
     * Returns the selected files.
     * <p>
     * 선택한 파일을 반환한다.
     *
     * @return the unmodifiable list of selected files / 선택한 파일의 수정할 수 없는 목록
     */
    public List<FileEntry> getSelectedFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Replaces the selected files with files that the user chose (the host calls this after its file chooser). Without the
     * <code>multiple</code> attribute only the first file is kept. The host fires the <code>input</code> and <code>change</code> events afterwards.
     * <p>
     * 선택한 파일을 사용자가 고른 파일로 바꾼다(호스트가 파일 선택기 뒤에 부른다). <code>multiple</code> 속성이 없으면 첫 파일만 유지한다. 호스트는 이어서
     * <code>input</code>, <code>change</code> 이벤트를 발생시킨다.
     *
     * @param chosen the chosen files / 고른 파일
     */
    public void setSelectedFiles(List<FileEntry> chosen) {
        if (kind != InputKinds.FILE) {
            return;
        }
        List<FileEntry> l = new ArrayList<FileEntry>();
        // 고른 파일을 복사하되 multiple 이 없으면 첫 파일만 남긴다.
        for (int i = 0; i < chosen.size() && (i == 0 || hasAttribute("multiple")); i++) {
            l.add(chosen.get(i));
        }
        files = Collections.unmodifiableList(l);
        fileList = null;
        stateChanged();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 선택 API
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>selectionStart</code> IDL attribute.
     * <p>
     * <code>selectionStart</code> IDL 속성을 반환한다.
     *
     * @return the start, or null if the selection APIs do not apply to this type / 시작. 이 종류에 선택 API 가 적용되지 않으면 null
     */
    public Integer getSelectionStart() {
        return InputKinds.selectionApplies(kind) ? Integer.valueOf(selection.getStart(getValue().length())) : null;
    }

    /**
     * Returns the <code>selectionEnd</code> IDL attribute.
     * <p>
     * <code>selectionEnd</code> IDL 속성을 반환한다.
     *
     * @return the end, or null if the selection APIs do not apply to this type / 끝. 이 종류에 선택 API 가 적용되지 않으면 null
     */
    public Integer getSelectionEnd() {
        return InputKinds.selectionApplies(kind) ? Integer.valueOf(selection.getEnd(getValue().length())) : null;
    }

    /**
     * Returns the <code>selectionDirection</code> IDL attribute.
     * <p>
     * <code>selectionDirection</code> IDL 속성을 반환한다.
     *
     * @return forward, backward or none, or null if the selection APIs do not apply / forward, backward, none. 선택 API 가 적용되지 않으면 null
     */
    public String getSelectionDirection() {
        return InputKinds.selectionApplies(kind) ? selection.getDirection() : null;
    }

    /**
     * Checks that the selection APIs apply to the current type.
     * <p>
     * 현재 종류에 선택 API 가 적용되는지 확인한다.
     *
     * @throws DomException InvalidStateError if they do not apply / 적용되지 않으면 InvalidStateError
     */
    private void requireSelectable() {
        if (!InputKinds.selectionApplies(kind)) {
            throw DomException.invalidState("the selection APIs do not apply to input type " + getType());
        }
    }

    /**
     * Sets <code>selectionStart</code>.
     * <p>
     * <code>selectionStart</code> 를 지정한다.
     *
     * @param v the new start (null means 0) / 새 시작(null 은 0)
     * @throws DomException InvalidStateError if the APIs do not apply / API 가 적용되지 않으면 InvalidStateError
     */
    public void setSelectionStart(Integer v) {
        requireSelectable();
        int s = v == null ? 0 : v.intValue();
        setSelectionRange(s, Math.max(s, selection.getEnd(getValue().length())), selection.getDirection());
    }

    /**
     * Sets <code>selectionEnd</code>.
     * <p>
     * <code>selectionEnd</code> 를 지정한다.
     *
     * @param v the new end (null means 0) / 새 끝(null 은 0)
     * @throws DomException InvalidStateError if the APIs do not apply / API 가 적용되지 않으면 InvalidStateError
     */
    public void setSelectionEnd(Integer v) {
        requireSelectable();
        setSelectionRange(selection.getStart(getValue().length()), v == null ? 0 : v.intValue(), selection.getDirection());
    }

    /**
     * Sets <code>selectionDirection</code>.
     * <p>
     * <code>selectionDirection</code> 을 지정한다.
     *
     * @param v forward, backward or none / forward, backward, none
     * @throws DomException InvalidStateError if the APIs do not apply / API 가 적용되지 않으면 InvalidStateError
     */
    public void setSelectionDirection(String v) {
        requireSelectable();
        setSelectionRange(selection.getStart(getValue().length()), selection.getEnd(getValue().length()), v);
    }

    /**
     * Implements <code>setSelectionRange(start, end, direction)</code>.
     * <p>
     * <code>setSelectionRange(start, end, direction)</code> 를 구현한다.
     *
     * @param start     the start / 시작
     * @param end       the end / 끝
     * @param direction the direction or null / 방향 또는 null
     * @throws DomException InvalidStateError if the APIs do not apply / API 가 적용되지 않으면 InvalidStateError
     */
    public void setSelectionRange(int start, int end, String direction) {
        requireSelectable();
        if (selection.set(start, end, direction, getValue().length())) {
            fireEvent("select", true, false);
        }
        stateChanged();
    }

    /**
     * Records the selection that the user made in the editor of the host (the caret moved, text was selected). Unlike
     * {@link #setSelectionRange(int, int, String)} it does not tell the host that the state changed (the host made the change itself), and a
     * <code>select</code> event is fired only if some text is selected afterwards. Types without a selection ignore the call.
     * <p>
     * 사용자가 호스트의 편집기에서 만든 선택(캐럿 이동, 글 선택)을 기록한다. {@link #setSelectionRange(int, int, String)} 와 달리 호스트에 상태가 바뀌었다고
     * 알리지 않으며(호스트가 직접 바꾼 것), 선택된 글이 있을 때만 <code>select</code> 이벤트를 발생시킨다. 선택이 없는 종류는 호출을 무시한다.
     *
     * @param start     the start / 시작
     * @param end       the end / 끝
     * @param direction forward, backward or none / forward, backward, none
     */
    public void setSelectionFromUser(int start, int end, String direction) {
        if (!InputKinds.selectionApplies(kind)) {
            return;
        }
        if (selection.set(start, end, direction, getValue().length()) && start != end) {
            fireEvent("select", true, false);
        }
    }

    /**
     * Implements <code>select()</code>: selects all the text of the control (nothing happens for types that it does not apply to).
     * <p>
     * <code>select()</code> 를 구현한다: 컨트롤의 글 전체를 선택한다(적용되지 않는 종류에서는 아무 일도 없다).
     */
    public void select() {
        if (!InputKinds.selectApplies(kind)) {
            return;
        }
        if (InputKinds.selectionApplies(kind)) {
            setSelectionRange(0, getValue().length(), "none");
        } else {
            fireEvent("select", true, false);
        }
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
     * @throws DomException InvalidStateError if the APIs do not apply, IndexSizeError if start is after end / API 가 적용되지 않으면 InvalidStateError, start 가 end 보다 뒤이면 IndexSizeError
     */
    public void setRangeText(String replacement, int start, int end, String mode) {
        requireSelectable();
        if (start > end) {
            throw DomException.indexSize("the start of the range is after its end");
        }
        String v = getValue();
        int s = Math.min(start, v.length());
        int e = Math.min(end, v.length());
        String r = replacement == null ? "" : replacement;
        value = sanitize(v.substring(0, s) + r + v.substring(e));
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
     * @throws DomException InvalidStateError if the APIs do not apply / API 가 적용되지 않으면 InvalidStateError
     */
    public void setRangeText(String replacement) {
        requireSelectable();
        setRangeText(replacement, selection.getStart(getValue().length()), selection.getEnd(getValue().length()), "preserve");
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 속성 기반 정보와 분류
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the maximum allowed value length (the <code>maxlength</code> attribute), or -1.
     * <p>
     * 허용되는 최대 값 길이(<code>maxlength</code> 속성)를 반환한다. 없으면 -1.
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
     * @param v the maximum length, not negative / 최대 길이(음수 불가)
     * @throws DomException IndexSizeError if negative / 음수이면 IndexSizeError
     */
    public void setMaxLength(int v) {
        if (v < 0) {
            throw DomException.indexSize("maxLength must not be negative");
        }
        setAttribute("maxlength", Integer.toString(v));
    }

    /**
     * Returns the minimum required value length (the <code>minlength</code> attribute), or -1.
     * <p>
     * 요구되는 최소 값 길이(<code>minlength</code> 속성)를 반환한다. 없으면 -1.
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
     * @param v the minimum length, not negative / 최소 길이(음수 불가)
     * @throws DomException IndexSizeError if negative / 음수이면 IndexSizeError
     */
    public void setMinLength(int v) {
        if (v < 0) {
            throw DomException.indexSize("minLength must not be negative");
        }
        setAttribute("minlength", Integer.toString(v));
    }

    /**
     * Returns the <code>size</code> IDL attribute: the attribute if it is a positive number, otherwise 20.
     * <p>
     * <code>size</code> IDL 속성을 반환한다: 속성이 양수이면 그 값, 아니면 20.
     *
     * @return the size / 크기
     */
    public int getSize() {
        int v = HtmlText.parseNonNegative(getAttribute("size"));
        return v > 0 ? v : 20;
    }

    /**
     * Sets <code>size</code>.
     * <p>
     * <code>size</code> 를 지정한다.
     *
     * @param v the new size, greater than zero / 새 크기(0 보다 커야 함)
     * @throws DomException IndexSizeError if it is zero / 0 이면 IndexSizeError
     */
    public void setSize(int v) {
        if (v <= 0) {
            throw DomException.indexSize("size must be greater than zero");
        }
        setAttribute("size", Integer.toString(v));
    }

    /**
     * Returns the <code>list</code> IDL attribute: the <code>datalist</code> that the <code>list</code> attribute names.
     * <p>
     * <code>list</code> IDL 속성을 반환한다: <code>list</code> 속성이 가리키는 <code>datalist</code>.
     *
     * @return the datalist or null / datalist 또는 null
     */
    public Element getList() {
        String id = getAttribute("list");
        if (id == null || !(InputKinds.isNumeric(kind) || InputKinds.isTextLike(kind) || kind == InputKinds.COLOR)) {
            return null;
        }
        Element e = HtmlControl.findById(this, id);
        return e instanceof HtmlDataListElement ? e : null;
    }

    /**
     * Tells whether this is a submit button (the Submit Button or Image Button state).
     * <p>
     * 제출 버튼(Submit Button 또는 Image Button 상태)인지 알려 준다.
     *
     * @return true for submit buttons / 제출 버튼이면 true
     */
    public boolean isSubmitButton() {
        return kind == InputKinds.SUBMIT || kind == InputKinds.IMAGE;
    }

    /**
     * Tells whether the control is a text field that blocks implicit submission when it is the only one in its form (the text, date and
     * number states).
     * <p>
     * 폼에 하나뿐일 때 암묵적 제출을 막는 텍스트 필드인지(텍스트, 날짜, 숫자 상태) 알려 준다.
     *
     * @return true for these states / 이 상태이면 true
     */
    boolean blocksImplicitSubmission() {
        return kind <= InputKinds.NUMBER;
    }

    /**
     * An input except the hidden state is labelable.
     * <p>
     * hidden 상태가 아닌 input 은 라벨을 붙일 수 있다.
     *
     * @return true unless hidden / hidden 이 아니면 true
     */
    public boolean isLabelable() {
        return kind != InputKinds.HIDDEN;
    }

    /**
     * An input takes part in form submission.
     * <p>
     * input 은 폼 제출에 참여한다.
     *
     * @return true / true
     */
    boolean isSubmittable() {
        return true;
    }

    /**
     * An input is resettable.
     * <p>
     * input 은 초기화할 수 있다.
     *
     * @return true / true
     */
    boolean isResettable() {
        return true;
    }

    /**
     * Image buttons are not in <code>form.elements</code>.
     * <p>
     * 이미지 버튼은 <code>form.elements</code> 에 오르지 않는다.
     *
     * @return false for the Image Button state / Image Button 상태이면 false
     */
    boolean isInFormElements() {
        return kind != InputKinds.IMAGE;
    }

    /**
     * Visible inputs take part in the default focus order.
     * <p>
     * 보이는 input 은 기본 포커스 순서에 참여한다.
     *
     * @return 0, or -1 for hidden inputs / 0. hidden 이면 -1
     */
    int defaultTabIndex() {
        return kind == InputKinds.HIDDEN ? -1 : 0;
    }

    /**
     * An input is mutable unless it is disabled or (where the attribute applies) read-only.
     * <p>
     * input 은 비활성이거나 (속성이 적용되는 경우) 읽기 전용이 아니면 mutable 이다.
     *
     * @return true if mutable / mutable 이면 true
     */
    boolean isMutable() {
        return !isDisabled() && !(InputKinds.readonlyApplies(kind) && hasAttribute("readonly"));
    }

    /**
     * Tells whether the placeholder is displayed: the <code>placeholder</code> attribute applies, it is not empty and the value is empty.
     * <p>
     * placeholder 가 표시되는지 알려 준다: <code>placeholder</code> 속성이 적용되고 비어 있지 않으며 값이 비어 있다.
     *
     * @return true if the placeholder is shown / placeholder 가 표시되면 true
     */
    public boolean isPlaceholderShown() {
        return InputKinds.placeholderApplies(kind) && attr("placeholder").length() > 0 && getValue().length() == 0;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 초기화
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The reset algorithm: the dirty flags are cleared, the value and the checkedness come from the attributes again, and the selected files are emptied.
     * <p>
     * 초기화 알고리즘: dirty 플래그를 끄고, 값과 checkedness 를 다시 속성에서 가져오며, 선택한 파일을 비운다.
     */
    void resetControl() {
        String old = getValue();
        dirtyValue = false;
        userEdited = false;
        dirtyChecked = false;
        badInput = false;
        checked = hasAttribute("checked");
        files = Collections.emptyList();
        fileList = null;
        if (!getValue().equals(old)) {
            selection.moveToEnd(getValue().length());
        }
        if (checked && kind == InputKinds.RADIO) {
            uncheckOthers();
        }
        stateChanged();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 활성화 동작
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Checkboxes, radio buttons, buttons and file upload controls have an activation behavior unless they are disabled.
     * <p>
     * 체크박스, 라디오 버튼, 버튼, 파일 업로드 컨트롤은 비활성이 아니면 활성화 동작이 있다.
     *
     * @return true if the control has one / 활성화 동작이 있으면 true
     */
    boolean hasActivationBehavior() {
        if (isDisabled()) {
            return false;
        }
        return kind == InputKinds.CHECKBOX || kind == InputKinds.RADIO || kind == InputKinds.SUBMIT || kind == InputKinds.IMAGE
                || kind == InputKinds.RESET || kind == InputKinds.FILE;
    }

    /**
     * The legacy-pre-activation behavior of a click: a checkbox toggles (and loses its indeterminateness) and a radio button becomes
     * checked before the event is dispatched, so that listeners see the new state; {@link #cancelActivation()} undoes it.
     * <p>
     * 클릭의 legacy-pre-activation 동작: 이벤트를 전달하기 전에 체크박스는 토글(indeterminate 도 해제)되고 라디오 버튼은 체크되어 리스너가 새 상태를 본다.
     * {@link #cancelActivation()} 이 되돌린다.
     */
    void preActivate() {
        preActivation = null;
        if (kind == InputKinds.CHECKBOX) {
            preActivation = new Object[] {Boolean.valueOf(checked), Boolean.valueOf(indeterminate), null};
            checked = !checked;
            indeterminate = false;
            dirtyChecked = true;
        } else if (kind == InputKinds.RADIO) {
            HtmlInputElement prev = null;
            List<HtmlInputElement> g = radioGroup();
            // 그룹에서 지금 체크되어 있는 구성원을 기억한다.
            for (int i = 0; i < g.size(); i++) {
                if (g.get(i).checked) {
                    prev = g.get(i);
                }
            }
            preActivation = new Object[] {Boolean.valueOf(checked), null, prev};
            checked = true;
            dirtyChecked = true;
            uncheckOthers();
        }
        if (preActivation != null) {
            stateChanged();
        }
    }

    /**
     * The legacy-canceled-activation behavior: restores the state that {@link #preActivate()} changed.
     * <p>
     * legacy-canceled-activation 동작: {@link #preActivate()} 가 바꾼 상태를 되돌린다.
     */
    void cancelActivation() {
        Object[] s = preActivation;
        preActivation = null;
        if (s == null) {
            return;
        }
        if (kind == InputKinds.CHECKBOX) {
            checked = ((Boolean) s[0]).booleanValue();
            indeterminate = ((Boolean) s[1]).booleanValue();
        } else if (kind == InputKinds.RADIO) {
            HtmlInputElement prev = (HtmlInputElement) s[2];
            if (prev != null && prev.radioGroup().contains(this)) {
                prev.checked = true;
                this.checked = this == prev;
            } else {
                this.checked = false;
            }
        }
        stateChanged();
    }

    /**
     * The activation behavior of a click that was not cancelled: a checkbox or radio button fires <code>input</code> and <code>change</code>
     * (a radio button only if it was not checked before), a submit or image button submits the form, a reset button resets it, and a file
     * upload control asks the host for files.
     * <p>
     * 취소되지 않은 클릭의 활성화 동작: 체크박스와 라디오 버튼은 <code>input</code> 과 <code>change</code> 를 발생시키고(라디오 버튼은 이전에 체크되어 있지 않았을 때만),
     * submit 이나 image 버튼은 폼을 제출하고, reset 버튼은 폼을 초기화하고, 파일 업로드 컨트롤은 호스트에 파일을 요청한다.
     *
     * @param click the click event / click 이벤트
     */
    void activate(Event click) {
        Object[] s = preActivation;
        preActivation = null;
        if (kind == InputKinds.CHECKBOX || kind == InputKinds.RADIO) {
            boolean changed = kind == InputKinds.CHECKBOX || s == null || !((Boolean) s[0]).booleanValue();
            if (isConnected() && changed) {
                fireEvent("input", true, false);
                fireChangeEvent();
            }
        } else if (kind == InputKinds.SUBMIT || kind == InputKinds.IMAGE) {
            if (kind == InputKinds.IMAGE && click instanceof MouseEvent) {
                MouseEvent m = (MouseEvent) click;
                imageX = Math.max(0, (int) Math.round(m.getOffsetX()));
                imageY = Math.max(0, (int) Math.round(m.getOffsetY()));
            }
            HtmlFormElement form = getForm();
            if (form != null) {
                form.submitFrom(this, false);
            }
        } else if (kind == InputKinds.RESET) {
            HtmlFormElement form = getForm();
            if (form != null) {
                form.reset();
            }
        } else if (kind == InputKinds.FILE) {
            FormHost h = host();
            FileEntry[] chosen = null;
            boolean asked = false;
            // 스크립트가 만든(신뢰되지 않은) click 은 사용자의 동작이 아니므로 파일 선택기를 열지 않는다.
            if (h != null && isMutable() && click.isTrusted()) {
                asked = true;
                try {
                    chosen = h.chooseFiles(this, hasAttribute("multiple"), attr("accept"));
                } catch (RuntimeException e) {
                    // 호스트의 오류는 선택 취소로 본다.
                    System.err.println("FormHost.chooseFiles failed: " + e);
                }
            }
            if (chosen == null || chosen.length == 0) {
                // 선택기를 열었는데 아무것도 고르지 않았으면 cancel 이벤트를 알린다.
                if (asked) {
                    fireEvent("cancel", true, false);
                }
            } else {
                List<FileEntry> l = new ArrayList<FileEntry>();
                // 호스트가 준 배열을 목록으로 옮긴다.
                for (int i = 0; i < chosen.length; i++) {
                    l.add(chosen[i]);
                }
                setSelectedFiles(l);
                fireEvent("input", true, false);
                fireChangeEvent();
            }
        }
    }

    /**
     * Returns the selected coordinate of an image button (the position of the last click on it, relative to the image).
     * <p>
     * 이미지 버튼의 선택 좌표(그 위의 마지막 클릭 위치, 이미지 기준)를 반환한다.
     *
     * @return {x, y} / {x, y}
     */
    int[] selectedCoordinate() {
        return new int[] {imageX, imageY};
    }

    /**
     * Sets the selected coordinate of an image button (the host calls it for clicks that it handles itself).
     * <p>
     * 이미지 버튼의 선택 좌표를 지정한다(호스트가 직접 처리하는 클릭에 대해 부른다).
     *
     * @param x x coordinate / x 좌표
     * @param y y coordinate / y 좌표
     */
    public void setSelectedCoordinate(int x, int y) {
        imageX = Math.max(0, x);
        imageY = Math.max(0, y);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 제약 검증
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Hidden, reset and plain buttons and read-only controls are barred from constraint validation.
     * <p>
     * hidden, reset, 일반 버튼과 읽기 전용 컨트롤은 제약 검증에서 제외된다.
     *
     * @return true if barred / 제외되면 true
     */
    boolean isBarredBySelf() {
        return kind == InputKinds.HIDDEN || kind == InputKinds.RESET || kind == InputKinds.BUTTON
                || (InputKinds.readonlyApplies(kind) && hasAttribute("readonly"));
    }

    /**
     * Tells whether a string is a valid email address (the "valid email address" rule of the HTML Standard).
     * <p>
     * 문자열이 올바른 이메일 주소인지 알려 준다(HTML 표준의 "valid email address" 규칙).
     *
     * @param s the text / 문자열
     * @return true if valid / 올바르면 true
     */
    static boolean isEmailAddress(String s) {
        int at = s.indexOf('@');
        if (at <= 0 || at != s.lastIndexOf('@') || at == s.length() - 1) {
            return false;
        }
        // 로컬 부분의 모든 문자가 허용된 문자인지 확인한다.
        for (int i = 0; i < at; i++) {
            char c = s.charAt(i);
            if (!(HtmlText.isDigit(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || ".!#$%&'*+/=?^_`{|}~-".indexOf(c) >= 0)) {
                return false;
            }
        }
        String[] labels = s.substring(at + 1).split("\\.", -1);
        // 도메인의 각 레이블이 영숫자로 시작하고 끝나며 63자 이하인지 확인한다.
        for (int i = 0; i < labels.length; i++) {
            String l = labels[i];
            if (l.length() == 0 || l.length() > 63 || l.charAt(0) == '-' || l.charAt(l.length() - 1) == '-') {
                return false;
            }
            // 레이블의 문자가 영숫자와 하이픈뿐인지 확인한다.
            for (int k = 0; k < l.length(); k++) {
                char c = l.charAt(k);
                if (!(HtmlText.isDigit(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-')) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tells whether a string is a valid absolute URL: it has a scheme, and the schemes with a network location need a host.
     * <p>
     * 문자열이 올바른 절대 URL 인지 알려 준다: 스킴이 있어야 하고, 네트워크 위치가 있는 스킴은 호스트가 있어야 한다.
     *
     * @param s the text / 문자열
     * @return true if valid / 올바르면 true
     */
    static boolean isAbsoluteUrl(String s) {
        int colon = s.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        // 스킴의 첫 문자는 영문자, 나머지는 영숫자와 + - . 이어야 한다.
        for (int i = 0; i < colon; i++) {
            char c = s.charAt(i);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            if (i == 0 ? !letter : !(letter || HtmlText.isDigit(c) || c == '+' || c == '-' || c == '.')) {
                return false;
            }
        }
        String scheme = HtmlText.lower(s.substring(0, colon));
        boolean network = "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme) || "ws".equals(scheme) || "wss".equals(scheme);
        if (!network) {
            return true;
        }
        String rest = s.substring(colon + 1);
        // 네트워크 스킴은 // 로 시작하는 권한부의 호스트가 비어 있지 않아야 한다.
        while (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        int end = rest.length();
        // 권한부가 끝나는 첫 구분 문자의 위치를 찾는다.
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        String authority = rest.substring(0, end);
        int atSign = authority.lastIndexOf('@');
        String host = atSign >= 0 ? authority.substring(atSign + 1) : authority;
        int portColon = host.lastIndexOf(':');
        if (portColon >= 0 && host.indexOf(']') < portColon) {
            host = host.substring(0, portColon);
        }
        return host.length() > 0 && host.indexOf(' ') < 0;
    }

    /**
     * Computes the violated constraints of the input in its current state.
     * <p>
     * input 의 현재 상태에서 어긴 제약을 계산한다.
     *
     * @return the flags / 플래그
     */
    int computeFlags() {
        int f = 0;
        if (badInput) {
            f |= ValidityState.BAD_INPUT;
        }
        // 필수 값이 없는지 확인한다.
        if (hasAttribute("required") && InputKinds.requiredApplies(kind) && isMutable()) {
            if (kind == InputKinds.CHECKBOX) {
                if (!checked) {
                    f |= ValidityState.VALUE_MISSING;
                }
            } else if (kind == InputKinds.RADIO) {
                if (!groupHasChecked()) {
                    f |= ValidityState.VALUE_MISSING;
                }
            } else if (kind == InputKinds.FILE) {
                if (files.isEmpty()) {
                    f |= ValidityState.VALUE_MISSING;
                }
            } else if (getValue().length() == 0) {
                f |= ValidityState.VALUE_MISSING;
            }
        }
        if (InputKinds.valueMode(kind) != InputKinds.MODE_VALUE) {
            return f;
        }
        String v = getValue();
        if (v.length() == 0) {
            return f;
        }
        // 값의 구문(이메일, URL)과 pattern 을 검사한다.
        if (kind == InputKinds.EMAIL || kind == InputKinds.URL) {
            f |= syntaxFlags(v);
        }
        if (InputKinds.patternApplies(kind)) {
            f |= patternFlags(v);
            int max = getMaxLength();
            int min = getMinLength();
            if (dirtyValue && userEdited) {
                if (max >= 0 && v.length() > max) {
                    f |= ValidityState.TOO_LONG;
                }
                if (min >= 0 && v.length() < min) {
                    f |= ValidityState.TOO_SHORT;
                }
            }
        }
        if (InputKinds.isNumeric(kind)) {
            f |= rangeFlags(v);
        }
        return f;
    }

    /**
     * Checks the syntax of an email or URL value.
     * <p>
     * 이메일이나 URL 값의 구문을 검사한다.
     *
     * @param v the value / 값
     * @return TYPE_MISMATCH or 0 / TYPE_MISMATCH 또는 0
     */
    private int syntaxFlags(String v) {
        if (kind == InputKinds.URL) {
            return isAbsoluteUrl(v) ? 0 : ValidityState.TYPE_MISMATCH;
        }
        if (!hasAttribute("multiple")) {
            return isEmailAddress(v) ? 0 : ValidityState.TYPE_MISMATCH;
        }
        String[] parts = v.split(",", -1);
        // 쉼표로 나눈 주소가 모두 올바른지 확인한다.
        for (int i = 0; i < parts.length; i++) {
            if (!isEmailAddress(parts[i])) {
                return ValidityState.TYPE_MISMATCH;
            }
        }
        return 0;
    }

    /**
     * Checks the value against the <code>pattern</code> attribute.
     * <p>
     * 값을 <code>pattern</code> 속성에 대해 검사한다.
     *
     * @param v the value / 값
     * @return PATTERN_MISMATCH or 0 / PATTERN_MISMATCH 또는 0
     */
    private int patternFlags(String v) {
        String p = getAttribute("pattern");
        if (p == null || p.length() == 0) {
            return 0;
        }
        if (kind == InputKinds.EMAIL && hasAttribute("multiple")) {
            String[] parts = v.split(",", -1);
            // 쉼표로 나눈 각 값이 패턴과 맞는지 확인한다.
            for (int i = 0; i < parts.length; i++) {
                if (Boolean.FALSE.equals(PatternMatcher.matches(p, parts[i]))) {
                    return ValidityState.PATTERN_MISMATCH;
                }
            }
            return 0;
        }
        return Boolean.FALSE.equals(PatternMatcher.matches(p, v)) ? ValidityState.PATTERN_MISMATCH : 0;
    }

    /**
     * Checks the value of a numeric state against the minimum, the maximum and the step.
     * <p>
     * 수치 상태의 값을 최솟값, 최댓값, step 에 대해 검사한다.
     *
     * @param v the value / 값
     * @return the underflow, overflow and step mismatch flags / underflow, overflow, step mismatch 플래그
     */
    private int rangeFlags(String v) {
        BigDecimal n = InputKinds.toNumber(kind, v);
        if (n == null) {
            return 0;
        }
        int f = 0;
        BigDecimal min = minimum();
        BigDecimal max = maximum();
        boolean reversed = InputKinds.isPeriodic(kind) && min != null && max != null && max.compareTo(min) < 0;
        if (reversed) {
            if (n.compareTo(max) > 0 && n.compareTo(min) < 0) {
                f |= ValidityState.RANGE_UNDERFLOW | ValidityState.RANGE_OVERFLOW;
            }
        } else {
            if (min != null && n.compareTo(min) < 0) {
                f |= ValidityState.RANGE_UNDERFLOW;
            }
            if (max != null && n.compareTo(max) > 0) {
                f |= ValidityState.RANGE_OVERFLOW;
            }
        }
        BigDecimal step = allowedStep();
        if (step != null && n.subtract(stepBase()).remainder(step).signum() != 0) {
            f |= ValidityState.STEP_MISMATCH;
        }
        return f;
    }

    /**
     * Tells whether the input has range limitations: it is in a numeric state and has a minimum or a maximum (the range state always has).
     * <p>
     * input 에 범위 제한이 있는지 알려 준다: 수치 상태이고 최솟값이나 최댓값이 있다(range 상태는 언제나 있다).
     *
     * @return true if there are range limitations / 범위 제한이 있으면 true
     */
    boolean hasRangeLimits() {
        return InputKinds.isNumeric(kind) && (minimum() != null || maximum() != null);
    }

    /**
     * Tells whether the value is below the minimum or above the maximum (the value of the pseudo-class <code>:out-of-range</code>).
     * <p>
     * 값이 최솟값보다 작거나 최댓값보다 큰지 알려 준다(<code>:out-of-range</code> 의사 클래스의 값).
     *
     * @return true if out of range / 범위를 벗어나면 true
     */
    boolean isOutOfRange() {
        String v = getValue();
        if (!InputKinds.isNumeric(kind) || v.length() == 0) {
            return false;
        }
        return (rangeFlags(v) & (ValidityState.RANGE_UNDERFLOW | ValidityState.RANGE_OVERFLOW)) != 0;
    }

    /**
     * Returns the English message of the first violated constraint.
     * <p>
     * 처음 어긴 제약의 영어 메시지를 반환한다.
     *
     * @param f the flags / 플래그
     * @return the message / 메시지
     */
    String messageFor(int f) {
        if ((f & ValidityState.VALUE_MISSING) != 0) {
            if (kind == InputKinds.CHECKBOX) {
                return "Please check this box if you want to proceed.";
            }
            if (kind == InputKinds.RADIO) {
                return "Please select one of these options.";
            }
            return kind == InputKinds.FILE ? "Please select a file." : "Please fill out this field.";
        }
        if ((f & ValidityState.TYPE_MISMATCH) != 0) {
            return kind == InputKinds.EMAIL ? "Please enter an email address." : "Please enter a URL.";
        }
        if ((f & ValidityState.BAD_INPUT) != 0) {
            return kind == InputKinds.NUMBER || kind == InputKinds.RANGE ? "Please enter a number." : "Please enter a valid value.";
        }
        if ((f & ValidityState.TOO_LONG) != 0) {
            return "Please shorten this text to " + getMaxLength() + " characters or less (you are currently using " + getValue().length()
                    + " characters).";
        }
        if ((f & ValidityState.TOO_SHORT) != 0) {
            return "Please lengthen this text to " + getMinLength() + " characters or more (you are currently using " + getValue().length()
                    + " characters).";
        }
        if ((f & ValidityState.RANGE_UNDERFLOW) != 0) {
            return "Value must be greater than or equal to " + describe(minimum()) + ".";
        }
        if ((f & ValidityState.RANGE_OVERFLOW) != 0) {
            return "Value must be less than or equal to " + describe(maximum()) + ".";
        }
        if ((f & ValidityState.STEP_MISMATCH) != 0) {
            BigDecimal n = InputKinds.toNumber(kind, getValue());
            BigDecimal step = allowedStep();
            if (n != null && step != null) {
                BigDecimal base = stepBase();
                BigDecimal lo = floorStep(n, base, step);
                return "Please enter a valid value. The two nearest valid values are " + describe(lo) + " and " + describe(lo.add(step)) + ".";
            }
        }
        return super.messageFor(f);
    }

    /**
     * Writes a number of the current state the way the value is written.
     * <p>
     * 현재 상태의 수를 값을 쓰는 방식대로 쓴다.
     *
     * @param n the number / 수
     * @return the text / 문자열
     */
    private String describe(BigDecimal n) {
        return n == null ? "" : InputKinds.fromNumber(kind, n);
    }
}
