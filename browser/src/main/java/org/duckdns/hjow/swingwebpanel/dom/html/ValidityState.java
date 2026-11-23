package org.duckdns.hjow.swingwebpanel.dom.html;

/**
 * The <code>ValidityState</code> of a form control (HTML Standard 4.10.21.3): which constraints the control currently violates. The object is
 * a live view: every getter asks the control, so the same object can be kept and read again after the control changed.
 * <p>
 * 폼 컨트롤의 <code>ValidityState</code>(HTML 표준 4.10.21.3): 컨트롤이 지금 어떤 제약을 어기고 있는지. 이 객체는 라이브 뷰이다. 모든 getter 가 컨트롤에
 * 물어보므로 같은 객체를 두고 컨트롤이 바뀐 뒤 다시 읽어도 된다.
 */
public final class ValidityState {

    /** Flag: a required value is missing. / 플래그: 필수 값이 없다. */
    static final int VALUE_MISSING = 1;
    /** Flag: the value does not have the required type syntax (email, url). / 플래그: 값이 요구하는 구문(email, url)이 아니다. */
    static final int TYPE_MISMATCH = 2;
    /** Flag: the value does not match the pattern. / 플래그: 값이 pattern 과 맞지 않는다. */
    static final int PATTERN_MISMATCH = 4;
    /** Flag: the value is longer than maxlength. / 플래그: 값이 maxlength 보다 길다. */
    static final int TOO_LONG = 8;
    /** Flag: the value is shorter than minlength. / 플래그: 값이 minlength 보다 짧다. */
    static final int TOO_SHORT = 16;
    /** Flag: the value is below the minimum. / 플래그: 값이 최솟값보다 작다. */
    static final int RANGE_UNDERFLOW = 32;
    /** Flag: the value is above the maximum. / 플래그: 값이 최댓값보다 크다. */
    static final int RANGE_OVERFLOW = 64;
    /** Flag: the value does not fit the step. / 플래그: 값이 step 에 맞지 않는다. */
    static final int STEP_MISMATCH = 128;
    /** Flag: the user typed something that cannot be converted to a value. / 플래그: 사용자가 값으로 바꿀 수 없는 것을 입력했다. */
    static final int BAD_INPUT = 256;
    /** Flag: a custom error was set. / 플래그: 사용자 정의 오류가 지정되었다. */
    static final int CUSTOM_ERROR = 512;

    /** The control. / 컨트롤. */
    private final HtmlListedElement control;

    /**
     * Creates the view of a control.
     * <p>
     * 컨트롤의 뷰를 만든다.
     *
     * @param control the control / 컨트롤
     */
    ValidityState(HtmlListedElement control) {
        this.control = control;
    }

    /**
     * Tells whether a flag is set.
     * <p>
     * 플래그가 설정되었는지 알려 준다.
     *
     * @param flag the flag / 플래그
     * @return true if set / 설정되었으면 true
     */
    private boolean has(int flag) {
        return (control.validityFlags() & flag) != 0;
    }

    /**
     * Returns <code>valueMissing</code>: a required control has no value.
     * <p>
     * <code>valueMissing</code> 을 반환한다: 필수 컨트롤에 값이 없다.
     *
     * @return the flag / 플래그
     */
    public boolean isValueMissing() {
        return has(VALUE_MISSING);
    }

    /**
     * Returns <code>typeMismatch</code>: the value is not a valid email address or URL.
     * <p>
     * <code>typeMismatch</code> 를 반환한다: 값이 올바른 이메일 주소나 URL 이 아니다.
     *
     * @return the flag / 플래그
     */
    public boolean isTypeMismatch() {
        return has(TYPE_MISMATCH);
    }

    /**
     * Returns <code>patternMismatch</code>: the value does not match the <code>pattern</code> attribute.
     * <p>
     * <code>patternMismatch</code> 를 반환한다: 값이 <code>pattern</code> 속성과 맞지 않는다.
     *
     * @return the flag / 플래그
     */
    public boolean isPatternMismatch() {
        return has(PATTERN_MISMATCH);
    }

    /**
     * Returns <code>tooLong</code>: the value is longer than <code>maxlength</code> (only after the user edited it).
     * <p>
     * <code>tooLong</code> 을 반환한다: 값이 <code>maxlength</code> 보다 길다(사용자가 편집한 뒤에만).
     *
     * @return the flag / 플래그
     */
    public boolean isTooLong() {
        return has(TOO_LONG);
    }

    /**
     * Returns <code>tooShort</code>: the value is shorter than <code>minlength</code> (only after the user edited it).
     * <p>
     * <code>tooShort</code> 를 반환한다: 값이 <code>minlength</code> 보다 짧다(사용자가 편집한 뒤에만).
     *
     * @return the flag / 플래그
     */
    public boolean isTooShort() {
        return has(TOO_SHORT);
    }

    /**
     * Returns <code>rangeUnderflow</code>: the value is below <code>min</code>.
     * <p>
     * <code>rangeUnderflow</code> 를 반환한다: 값이 <code>min</code> 보다 작다.
     *
     * @return the flag / 플래그
     */
    public boolean isRangeUnderflow() {
        return has(RANGE_UNDERFLOW);
    }

    /**
     * Returns <code>rangeOverflow</code>: the value is above <code>max</code>.
     * <p>
     * <code>rangeOverflow</code> 를 반환한다: 값이 <code>max</code> 보다 크다.
     *
     * @return the flag / 플래그
     */
    public boolean isRangeOverflow() {
        return has(RANGE_OVERFLOW);
    }

    /**
     * Returns <code>stepMismatch</code>: the value is not on a step.
     * <p>
     * <code>stepMismatch</code> 를 반환한다: 값이 step 위에 있지 않다.
     *
     * @return the flag / 플래그
     */
    public boolean isStepMismatch() {
        return has(STEP_MISMATCH);
    }

    /**
     * Returns <code>badInput</code>: the user typed text that cannot be converted to a value.
     * <p>
     * <code>badInput</code> 을 반환한다: 사용자가 값으로 바꿀 수 없는 글을 입력했다.
     *
     * @return the flag / 플래그
     */
    public boolean isBadInput() {
        return has(BAD_INPUT);
    }

    /**
     * Returns <code>customError</code>: <code>setCustomValidity</code> set a non-empty message.
     * <p>
     * <code>customError</code> 를 반환한다: <code>setCustomValidity</code> 가 빈 문자열이 아닌 메시지를 지정했다.
     *
     * @return the flag / 플래그
     */
    public boolean isCustomError() {
        return has(CUSTOM_ERROR);
    }

    /**
     * Returns <code>valid</code>: no flag is set.
     * <p>
     * <code>valid</code> 를 반환한다: 어떤 플래그도 설정되지 않았다.
     *
     * @return true if no constraint is violated / 어긴 제약이 없으면 true
     */
    public boolean isValid() {
        return control.validityFlags() == 0;
    }
}
