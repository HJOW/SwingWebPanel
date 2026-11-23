package org.duckdns.hjow.swingwebpanel.dom.html;

import java.math.BigDecimal;

/**
 * The table of the states of the <code>type</code> attribute of an <code>input</code> element (HTML Standard 4.10.5.1): the state numbers,
 * the keyword of each state, the mode of the <code>value</code> IDL attribute, which attributes and APIs apply to which state, and the
 * numeric view of the date/time/number states (convert a string to a number and back, step scale, default step, default step base).
 * An unknown or missing type is the Text state.
 * <p>
 * <code>input</code> 요소의 <code>type</code> 속성 상태 표(HTML 표준 4.10.5.1): 상태 번호, 각 상태의 키워드, <code>value</code> IDL 속성의 모드, 어떤 속성과
 * API 가 어떤 상태에 적용되는지, 날짜/시간/숫자 상태의 수치 표현(문자열을 수로 또는 그 반대로 바꾸기, step 배율, 기본 step, 기본 step 기준값). 알 수 없거나
 * 없는 type 은 Text 상태이다.
 */
final class InputKinds {

    /** Text state. / Text 상태. */
    static final int TEXT = 0;
    /** Search state. / Search 상태. */
    static final int SEARCH = 1;
    /** Telephone state. / Telephone 상태. */
    static final int TEL = 2;
    /** URL state. / URL 상태. */
    static final int URL = 3;
    /** Email state. / Email 상태. */
    static final int EMAIL = 4;
    /** Password state. / Password 상태. */
    static final int PASSWORD = 5;
    /** Date state. / Date 상태. */
    static final int DATE = 6;
    /** Month state. / Month 상태. */
    static final int MONTH = 7;
    /** Week state. / Week 상태. */
    static final int WEEK = 8;
    /** Time state. / Time 상태. */
    static final int TIME = 9;
    /** Local Date and Time state. / Local Date and Time 상태. */
    static final int DATETIME_LOCAL = 10;
    /** Number state. / Number 상태. */
    static final int NUMBER = 11;
    /** Range state. / Range 상태. */
    static final int RANGE = 12;
    /** Color state. / Color 상태. */
    static final int COLOR = 13;
    /** Checkbox state. / Checkbox 상태. */
    static final int CHECKBOX = 14;
    /** Radio Button state. / Radio Button 상태. */
    static final int RADIO = 15;
    /** File Upload state. / File Upload 상태. */
    static final int FILE = 16;
    /** Submit Button state. / Submit Button 상태. */
    static final int SUBMIT = 17;
    /** Image Button state. / Image Button 상태. */
    static final int IMAGE = 18;
    /** Reset Button state. / Reset Button 상태. */
    static final int RESET = 19;
    /** Button state. / Button 상태. */
    static final int BUTTON = 20;
    /** Hidden state. / Hidden 상태. */
    static final int HIDDEN = 21;

    /** The mode "value" of the value IDL attribute. / value IDL 속성의 "value" 모드. */
    static final int MODE_VALUE = 0;
    /** The mode "default". / "default" 모드. */
    static final int MODE_DEFAULT = 1;
    /** The mode "default/on". / "default/on" 모드. */
    static final int MODE_DEFAULT_ON = 2;
    /** The mode "filename". / "filename" 모드. */
    static final int MODE_FILENAME = 3;

    /** The keyword of every state, indexed by state number. / 상태 번호별 키워드. */
    private static final String[] NAMES = {"text", "search", "tel", "url", "email", "password", "date", "month", "week", "time",
        "datetime-local", "number", "range", "color", "checkbox", "radio", "file", "submit", "image", "reset", "button", "hidden"};

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private InputKinds() {
    }

    /**
     * Returns the state of a <code>type</code> attribute value: an ASCII case-insensitive match of a keyword, otherwise Text.
     * <p>
     * <code>type</code> 속성 값의 상태를 반환한다: 키워드와 ASCII 대소문자 무시 일치, 아니면 Text.
     *
     * @param type the attribute value or null / 속성 값 또는 null
     * @return the state number / 상태 번호
     */
    static int kindOf(String type) {
        if (type == null) {
            return TEXT;
        }
        String t = HtmlText.lower(type);
        // 키워드 표에서 같은 것을 찾는다.
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(t)) {
                return i;
            }
        }
        return TEXT;
    }

    /**
     * Returns the keyword of a state.
     * <p>
     * 상태의 키워드를 반환한다.
     *
     * @param kind the state / 상태
     * @return the keyword / 키워드
     */
    static String nameOf(int kind) {
        return NAMES[kind];
    }

    /**
     * Returns the mode of the <code>value</code> IDL attribute in a state.
     * <p>
     * 상태에서 <code>value</code> IDL 속성의 모드를 반환한다.
     *
     * @param kind the state / 상태
     * @return one of the MODE constants / MODE 상수 중 하나
     */
    static int valueMode(int kind) {
        switch (kind) {
        case HIDDEN:
        case SUBMIT:
        case IMAGE:
        case RESET:
        case BUTTON:
            return MODE_DEFAULT;
        case CHECKBOX:
        case RADIO:
            return MODE_DEFAULT_ON;
        case FILE:
            return MODE_FILENAME;
        default:
            return MODE_VALUE;
        }
    }

    /**
     * Tells whether the state is a text-like state whose value is typed text (text, search, telephone, URL, email, password).
     * <p>
     * 상태가 입력한 글을 값으로 갖는 글 형태인지(text, search, tel, url, email, password) 알려 준다.
     *
     * @param kind the state / 상태
     * @return true for these states / 이 상태이면 true
     */
    static boolean isTextLike(int kind) {
        return kind <= PASSWORD;
    }

    /**
     * Tells whether the <code>min</code>, <code>max</code> and <code>step</code> attributes and the numeric APIs apply (date, month, week, time, local date and time, number, range).
     * <p>
     * <code>min</code>, <code>max</code>, <code>step</code> 속성과 수치 API 가 적용되는지(date, month, week, time, datetime-local, number, range) 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if they apply / 적용되면 true
     */
    static boolean isNumeric(int kind) {
        return kind >= DATE && kind <= RANGE;
    }

    /**
     * Tells whether the <code>readonly</code> attribute applies.
     * <p>
     * <code>readonly</code> 속성이 적용되는지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if it applies / 적용되면 true
     */
    static boolean readonlyApplies(int kind) {
        return kind <= NUMBER;
    }

    /**
     * Tells whether the <code>required</code> attribute applies.
     * <p>
     * <code>required</code> 속성이 적용되는지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if it applies / 적용되면 true
     */
    static boolean requiredApplies(int kind) {
        return kind <= NUMBER || kind == CHECKBOX || kind == RADIO || kind == FILE;
    }

    /**
     * Tells whether the <code>placeholder</code> attribute applies.
     * <p>
     * <code>placeholder</code> 속성이 적용되는지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if it applies / 적용되면 true
     */
    static boolean placeholderApplies(int kind) {
        return kind <= PASSWORD || kind == NUMBER;
    }

    /**
     * Tells whether the <code>pattern</code>, <code>minlength</code>, <code>maxlength</code> and <code>size</code> attributes apply.
     * <p>
     * <code>pattern</code>, <code>minlength</code>, <code>maxlength</code>, <code>size</code> 속성이 적용되는지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if they apply / 적용되면 true
     */
    static boolean patternApplies(int kind) {
        return kind <= PASSWORD;
    }

    /**
     * Tells whether the selection APIs (<code>selectionStart</code>, <code>setSelectionRange</code>, <code>setRangeText</code>) apply.
     * <p>
     * 선택 API(<code>selectionStart</code>, <code>setSelectionRange</code>, <code>setRangeText</code>)가 적용되는지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if they apply / 적용되면 true
     */
    static boolean selectionApplies(int kind) {
        return kind <= PASSWORD && kind != EMAIL;
    }

    /**
     * Tells whether <code>select()</code> applies.
     * <p>
     * <code>select()</code> 가 적용되는지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if it applies / 적용되면 true
     */
    static boolean selectApplies(int kind) {
        return kind <= NUMBER || kind == COLOR || kind == FILE;
    }

    /**
     * Tells whether the control is a button state (submit, image, reset, button).
     * <p>
     * 컨트롤이 버튼 상태(submit, image, reset, button)인지 알려 준다.
     *
     * @param kind the state / 상태
     * @return true for buttons / 버튼이면 true
     */
    static boolean isButton(int kind) {
        return kind >= SUBMIT && kind <= BUTTON;
    }

    /**
     * Tells whether the <code>valueAsDate</code> API applies (date, month, week, time).
     * <p>
     * <code>valueAsDate</code> API 가 적용되는지(date, month, week, time) 알려 준다.
     *
     * @param kind the state / 상태
     * @return true if it applies / 적용되면 true
     */
    static boolean dateApplies(int kind) {
        return kind >= DATE && kind <= TIME;
    }

    /**
     * Tells whether the state has a periodic domain (the time state, where 23:00 to 01:00 is a valid range).
     * <p>
     * 상태가 주기적 정의역을 갖는지(time 상태: 23:00~01:00 도 올바른 범위) 알려 준다.
     *
     * @param kind the state / 상태
     * @return true for time / time 이면 true
     */
    static boolean isPeriodic(int kind) {
        return kind == TIME;
    }

    /**
     * Converts a string to a number ("the algorithm to convert a string to a number") of a numeric state.
     * <p>
     * 수치 상태의 "문자열을 수로 바꾸는 알고리즘"을 적용한다.
     *
     * @param kind the state / 상태
     * @param s    the text, may be null / 문자열(null 가능)
     * @return the number, or null on error / 수. 오류이면 null
     */
    static BigDecimal toNumber(int kind, String s) {
        if (s == null) {
            return null;
        }
        Long v;
        switch (kind) {
        case NUMBER:
        case RANGE:
            return NumberSyntax.parse(s);
        case DATE:
            v = DateTimeSyntax.parseDate(s);
            break;
        case MONTH:
            v = DateTimeSyntax.parseMonth(s);
            break;
        case WEEK:
            v = DateTimeSyntax.parseWeek(s);
            break;
        case TIME:
            v = DateTimeSyntax.parseTime(s);
            break;
        case DATETIME_LOCAL:
            v = DateTimeSyntax.parseLocal(s);
            break;
        default:
            return null;
        }
        return v == null ? null : BigDecimal.valueOf(v.longValue());
    }

    /**
     * Converts a number to a string ("the algorithm to convert a number to a string") of a numeric state.
     * <p>
     * 수치 상태의 "수를 문자열로 바꾸는 알고리즘"을 적용한다.
     *
     * @param kind the state / 상태
     * @param n    the number / 수
     * @return the text / 문자열
     */
    static String fromNumber(int kind, BigDecimal n) {
        long l;
        switch (kind) {
        case DATE:
            l = n.longValue();
            return DateTimeSyntax.formatDate(l);
        case MONTH:
            l = n.longValue();
            return DateTimeSyntax.formatMonth(l);
        case WEEK:
            l = n.longValue();
            return DateTimeSyntax.formatWeek(l);
        case TIME:
            l = n.longValue();
            return DateTimeSyntax.formatTime(l);
        case DATETIME_LOCAL:
            l = n.longValue();
            return DateTimeSyntax.formatLocal(l);
        default:
            return NumberSyntax.format(n);
        }
    }

    /**
     * Returns the step scale factor of a numeric state.
     * <p>
     * 수치 상태의 step 배율을 반환한다.
     *
     * @param kind the state / 상태
     * @return the factor / 배율
     */
    static BigDecimal stepScale(int kind) {
        switch (kind) {
        case DATE:
            return BigDecimal.valueOf(DateTimeSyntax.MS_PER_DAY);
        case WEEK:
            return BigDecimal.valueOf(DateTimeSyntax.MS_PER_WEEK);
        case TIME:
        case DATETIME_LOCAL:
            return BigDecimal.valueOf(1000L);
        default:
            return BigDecimal.ONE;
        }
    }

    /**
     * Returns the default step of a numeric state (before the scale factor is applied).
     * <p>
     * 수치 상태의 기본 step 을 반환한다(배율을 곱하기 전).
     *
     * @param kind the state / 상태
     * @return the default step / 기본 step
     */
    static BigDecimal defaultStep(int kind) {
        return kind == TIME || kind == DATETIME_LOCAL ? BigDecimal.valueOf(60L) : BigDecimal.ONE;
    }

    /**
     * Returns the default step base of a state, or null if the state defines none (then it is zero).
     * <p>
     * 상태의 기본 step 기준값을 반환한다. 정의하지 않으면 null(그때는 0).
     *
     * @param kind the state / 상태
     * @return the default step base or null / 기본 step 기준값 또는 null
     */
    static BigDecimal defaultStepBase(int kind) {
        return kind == WEEK ? BigDecimal.valueOf(-259200000L) : null;
    }
}
