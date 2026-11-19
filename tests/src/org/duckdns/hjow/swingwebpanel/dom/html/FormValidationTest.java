package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the constraint validation of the form controls (plan item P11-07, HTML Standard 4.10.20): which controls are validated, every kind of
 * validity state, <code>checkValidity</code> and <code>reportValidity</code> with the <code>invalid</code> event, the custom validity message and
 * the interactive validation that a submission performs.
 * <p>
 * 폼 컨트롤의 제약 검증 시험(계획 P11-07, HTML 표준 4.10.20): 어떤 컨트롤이 검증되는지, 모든 종류의 validity 상태, <code>invalid</code> 이벤트를 동반하는
 * <code>checkValidity</code>, <code>reportValidity</code>, 사용자 지정 검증 메시지, 제출이 수행하는 대화형 검증.
 */
public class FormValidationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormValidationTest() {
    }

    /**
     * Returns the validity flags of a control as a short string of the names of the set flags, for compact comparison.
     * <p>
     * 컨트롤의 validity 플래그를 설정된 플래그 이름의 짧은 문자열로 반환한다. 비교를 간결하게 하기 위함이다.
     *
     * @param c the control / 컨트롤
     * @return the names, empty when valid / 이름. 올바르면 빈 문자열
     */
    private static String flags(HtmlListedElement c) {
        ValidityState v = c.getValidity();
        StringBuilder b = new StringBuilder();
        if (v.isValueMissing()) {
            b.append("valueMissing ");
        }
        if (v.isTypeMismatch()) {
            b.append("typeMismatch ");
        }
        if (v.isPatternMismatch()) {
            b.append("patternMismatch ");
        }
        if (v.isTooLong()) {
            b.append("tooLong ");
        }
        if (v.isTooShort()) {
            b.append("tooShort ");
        }
        if (v.isRangeUnderflow()) {
            b.append("rangeUnderflow ");
        }
        if (v.isRangeOverflow()) {
            b.append("rangeOverflow ");
        }
        if (v.isStepMismatch()) {
            b.append("stepMismatch ");
        }
        if (v.isBadInput()) {
            b.append("badInput ");
        }
        if (v.isCustomError()) {
            b.append("customError ");
        }
        return b.toString().trim();
    }

    /**
     * Required text controls: the value is missing while it is empty; a valid control reports valid.
     * <p>
     * 필수 텍스트 컨트롤: 값이 비어 있는 동안 값이 없는 것으로 본다. 올바른 컨트롤은 valid 이다.
     */
    public void testRequiredText() {
        Document d = FormTestSupport.page("<input id=a required><input id=b required value=x><textarea id=t required></textarea><input id=n type=number required>"
                + "<input id=p required type=password><input id=u type=hidden required>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        Check.equal("valueMissing", flags(a), "empty required input");
        Check.isFalse(a.getValidity().isValid(), "not valid");
        Check.isTrue(a.willValidate(), "willValidate");
        Check.isFalse(a.checkValidity(), "checkValidity");
        a.setValue("x");
        Check.equal("", flags(a), "filled");
        Check.isTrue(a.getValidity().isValid(), "valid");
        Check.equal("", flags(FormTestSupport.input(d, "b")), "value attribute");
        Check.equal("valueMissing", flags(FormTestSupport.textarea(d, "t")), "textarea");
        FormTestSupport.textarea(d, "t").setValue("text");
        Check.equal("", flags(FormTestSupport.textarea(d, "t")), "textarea filled");
        Check.equal("valueMissing", flags(FormTestSupport.input(d, "n")), "number");
        Check.equal("valueMissing", flags(FormTestSupport.input(d, "p")), "password");
        Check.isFalse(FormTestSupport.input(d, "u").willValidate(), "hidden inputs are barred");
        Check.equal("", flags(FormTestSupport.input(d, "u")) + FormTestSupport.input(d, "u").getValidationMessage(), "hidden: no message");
    }

    /**
     * Required checkbox, radio group, file upload and select (with the placeholder label option).
     * <p>
     * 필수 체크박스, 라디오 그룹, 파일 업로드, select(placeholder label option 포함).
     */
    public void testRequiredOtherControls() {
        Document d = FormTestSupport.page("<form><input id=c type=checkbox required><input id=r1 type=radio name=g required><input id=r2 type=radio name=g>"
                + "<input id=f type=file required>"
                + "<select id=s1 required><option value=''>Choose</option><option value=1>one</option></select>"
                + "<select id=s2 required><option value=0>zero</option><option value=1>one</option></select>"
                + "<select id=s3 required size=3><option value=''>Choose</option><option value=1>one</option></select>"
                + "<select id=s4 required multiple><option value=''>Choose</option><option value=1>one</option></select>"
                + "<select id=s5 required><optgroup label=g><option value=''>Choose</option></optgroup><option value=1 selected>one</option></select></form>");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        Check.equal("valueMissing", flags(c), "unchecked required checkbox");
        c.setChecked(true);
        Check.equal("", flags(c), "checked");
        HtmlInputElement r1 = FormTestSupport.input(d, "r1");
        HtmlInputElement r2 = FormTestSupport.input(d, "r2");
        Check.equal("valueMissing", flags(r1), "required radio group without a checked radio");
        Check.equal("", flags(r2), "the member that is not required is valid by itself");
        r2.setChecked(true);
        Check.equal("", flags(r1), "one radio of the group is checked");
        HtmlInputElement f = FormTestSupport.input(d, "f");
        Check.equal("valueMissing", flags(f), "no file selected");
        f.setSelectedFiles(Arrays.asList(new FileEntry("a.txt", "text/plain", 0, new byte[] {1})));
        Check.equal("", flags(f), "a file is selected");
        Check.equal("valueMissing", flags(FormTestSupport.select(d, "s1")), "the placeholder label option is selected: value is missing");
        FormTestSupport.select(d, "s1").setSelectedIndex(1);
        Check.equal("", flags(FormTestSupport.select(d, "s1")), "another option");
        Check.equal("", flags(FormTestSupport.select(d, "s2")), "the first option is a real value");
        Check.equal("valueMissing", flags(FormTestSupport.select(d, "s3")), "display size 3: nothing is selected, so the value is missing");
        Check.equal("valueMissing", flags(FormTestSupport.select(d, "s4")), "a multiple select without a selected option is missing");
        Check.equal("", flags(FormTestSupport.select(d, "s5")), "the option in an optgroup is not the placeholder label option and another one is selected");
    }

    /**
     * Elements that are barred from constraint validation.
     * <p>
     * 제약 검증에서 제외되는 요소.
     */
    public void testBarredElements() {
        Document d = FormTestSupport.page("<form><input id=ro required readonly><input id=dis required disabled><input id=btn type=button required><input id=sub type=submit>"
                + "<input id=rst type=reset><input id=img type=image><button id=b type=button>x</button><button id=bs>y</button><fieldset id=fs></fieldset>"
                + "<output id=o></output><datalist><input id=inl required></datalist><fieldset disabled><input id=fsd required></fieldset>"
                + "<input id=cb type=checkbox required readonly><select id=sel required disabled></select><textarea id=tro required readonly></textarea></form>");
        String[] barred = {"ro", "dis", "btn", "rst", "b", "fs", "o", "inl", "fsd", "sel", "tro"};
        // 제외되는 요소는 willValidate 가 false 이고 validity 가 올바르며 검증 메시지가 없다.
        for (int i = 0; i < barred.length; i++) {
            HtmlListedElement e = (HtmlListedElement) d.getElementById(barred[i]);
            Check.isFalse(e.willValidate(), barred[i] + " is barred");
            Check.isTrue(e.checkValidity(), barred[i] + " checkValidity is true");
            Check.equal("", e.getValidationMessage(), barred[i] + " has no message");
        }
        Check.isTrue(FormTestSupport.input(d, "sub").willValidate(), "a submit button is validated");
        Check.isTrue(((HtmlListedElement) d.getElementById("bs")).willValidate(), "a submit button element is validated");
        Check.isTrue(FormTestSupport.input(d, "img").willValidate(), "an image button is validated");
        Check.isTrue(FormTestSupport.input(d, "cb").willValidate(), "readonly does not apply to checkboxes");
        Check.equal("valueMissing", flags(FormTestSupport.input(d, "cb")), "so a required checkbox is missing");
        FormTestSupport.input(d, "ro").setCustomValidity("bad");
        Check.isTrue(FormTestSupport.input(d, "ro").checkValidity(), "a custom error of a barred control is ignored");
    }

    /**
     * Pattern: the whole value must match the regular expression; an invalid pattern is ignored; the title is part of the message.
     * <p>
     * pattern: 값 전체가 정규식과 맞아야 한다. 올바르지 않은 pattern 은 무시된다. title 은 메시지의 일부이다.
     */
    public void testPattern() {
        Document d = FormTestSupport.page("<input id=a pattern='[0-9]{3}-[0-9]{4}' title='Format: 123-4567' value='123-4567'><input id=b pattern='[a-z]+'>"
                + "<input id=c pattern='(' value='x'><input id=e type=email pattern='.+@example\\.com' value='u@example.com'><input id=n type=number pattern='x' value=5>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        Check.equal("", flags(a), "matches");
        a.setValue("123-45678");
        Check.equal("patternMismatch", flags(a), "the whole value must match");
        Check.equal("Please match the requested format. Format: 123-4567", a.getValidationMessage(), "the title is appended to the message");
        HtmlInputElement b = FormTestSupport.input(d, "b");
        Check.equal("", flags(b), "an empty value is not checked against the pattern");
        b.setValue("abC");
        Check.equal("patternMismatch", flags(b), "case matters");
        b.setValue("abc");
        Check.equal("", flags(b), "abc");
        Check.equal("", flags(FormTestSupport.input(d, "c")), "an invalid pattern is ignored");
        Check.equal("", flags(FormTestSupport.input(d, "e")), "email with a pattern");
        Check.equal("", flags(FormTestSupport.input(d, "n")), "pattern does not apply to number");
        b.setAttribute("pattern", "a|b");
        b.setValue("ab");
        Check.equal("patternMismatch", flags(b), "the pattern is wrapped in a group: ^(?:a|b)$ does not match ab");
    }

    /**
     * Type mismatch of the email and url states, with the multiple attribute of email.
     * <p>
     * email 과 url 상태의 type mismatch. email 의 multiple 속성 포함.
     */
    public void testTypeMismatch() {
        Document d = FormTestSupport.page("<input id=e type=email><input id=m type=email multiple><input id=u type=url>");
        HtmlInputElement e = FormTestSupport.input(d, "e");
        String[] good = {"a@b", "user.name+tag@example.co.kr", "a@b-c.d", "!#$%&'*+/=?^_`{|}~-@x"};
        // 올바른 이메일 주소는 type mismatch 가 없다.
        for (int i = 0; i < good.length; i++) {
            e.setValue(good[i]);
            Check.equal("", flags(e), "valid email " + good[i]);
        }
        String[] bad = {"a", "a@", "@b", "a@@b", "a b@c", "a@b..c", "a@-b", "a@b-", "a@b_c", "a@.b", "한글@b"};
        // 올바르지 않은 이메일 주소는 type mismatch 이다.
        for (int i = 0; i < bad.length; i++) {
            e.setValue(bad[i]);
            Check.equal("typeMismatch", flags(e), "invalid email " + bad[i]);
        }
        e.setValue("");
        Check.equal("", flags(e), "empty is fine");
        HtmlInputElement m = FormTestSupport.input(d, "m");
        m.setValue("a@b, c@d");
        Check.equal("", flags(m), "two addresses");
        m.setValue("a@b,c");
        Check.equal("typeMismatch", flags(m), "one of them is bad");
        HtmlInputElement u = FormTestSupport.input(d, "u");
        String[] goodUrl = {"http://example.com/", "https://example.com:8080/a?b#c", "ftp://host/x", "mailto:a@b", "x-custom:anything", "file:///c:/x", "javascript:alert(1)"};
        // 절대 URL 은 type mismatch 가 없다.
        for (int i = 0; i < goodUrl.length; i++) {
            u.setValue(goodUrl[i]);
            Check.equal("", flags(u), "valid url " + goodUrl[i]);
        }
        String[] badUrl = {"example.com", "/relative/path", "//example.com", "http://", "http:// x.com", ":x", "1http://x", "http:///"};
        // 상대 URL 이나 호스트 없는 URL 은 type mismatch 이다.
        for (int i = 0; i < badUrl.length; i++) {
            u.setValue(badUrl[i]);
            Check.equal("typeMismatch", flags(u), "invalid url " + badUrl[i]);
        }
    }

    /**
     * Length limits are checked only after the user edited the value (a script's value is never too long or too short).
     * <p>
     * 길이 제한은 사용자가 값을 편집한 뒤에만 검사한다(스크립트가 넣은 값은 너무 길거나 짧지 않다).
     */
    public void testLengthLimits() {
        Document d = FormTestSupport.page("<input id=a maxlength=3 minlength=2 value=abcdef><textarea id=t maxlength=4 minlength=3></textarea>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        Check.equal("", flags(a), "the default value is not checked");
        a.setValue("abcdef");
        Check.equal("", flags(a), "a value set by script is not checked");
        a.setValueFromUser("abcd");
        Check.equal("tooLong", flags(a), "an edit by the user that is too long");
        a.setValueFromUser("a");
        Check.equal("tooShort", flags(a), "an edit by the user that is too short");
        a.setValueFromUser("ab");
        Check.equal("", flags(a), "within the limits");
        a.setValueFromUser("");
        Check.equal("", flags(a), "an empty value is never too short");
        Check.equal(3, a.getMaxLength(), "maxLength");
        Check.equal(2, a.getMinLength(), "minLength");
        HtmlTextAreaElement t = FormTestSupport.textarea(d, "t");
        t.setValueFromUser("abcde");
        Check.equal("tooLong", flags(t), "textarea too long");
        t.setValueFromUser("ab");
        Check.equal("tooShort", flags(t), "textarea too short");
        t.setValue("abcdef");
        Check.equal("", flags(t), "set by script");
        a.setValueFromUser("\ud83d\ude00\ud83d\ude00");
        Check.equal("tooLong", flags(a), "the length counts UTF-16 code units, so two emoji are 4 units");
    }

    /**
     * Range and step of number, range, date, month, week, time and local date and time.
     * <p>
     * number, range, date, month, week, time, datetime-local 의 범위와 step.
     */
    public void testRangeAndStep() {
        Document d = FormTestSupport.page("<input id=n type=number min=0 max=10 step=2>"
                + "<input id=n2 type=number step=0.1>"
                + "<input id=n3 type=number step=any value=0.123456>"
                + "<input id=n4 type=number min=1 step=3>"
                + "<input id=d type=date min=2020-01-10 max=2020-01-20>"
                + "<input id=m type=month min=2020-02 max=2020-04>"
                + "<input id=w type=week min=2020-W10 max=2020-W20>"
                + "<input id=t type=time min=09:00 max=17:00 step=900>"
                + "<input id=t2 type=time min=22:00 max=02:00>"
                + "<input id=l type=datetime-local min=2020-01-01T00:00 max=2020-12-31T23:59>"
                + "<input id=r type=range>");
        HtmlInputElement n = FormTestSupport.input(d, "n");
        n.setValue("4");
        Check.equal("", flags(n), "4 is on a step");
        n.setValue("3");
        Check.equal("stepMismatch", flags(n), "3 is not on a step of 2 from 0");
        n.setValue("-2");
        Check.equal("rangeUnderflow", flags(n), "below the minimum");
        n.setValue("12");
        Check.equal("rangeOverflow", flags(n), "above the maximum");
        n.setValue("11");
        Check.equal("rangeOverflow stepMismatch", flags(n), "both");
        HtmlInputElement n2 = FormTestSupport.input(d, "n2");
        n2.setValue("0.3");
        Check.equal("", flags(n2), "decimal steps are exact (BigDecimal): 0.3 is 3 steps of 0.1");
        n2.setValue("0.35");
        Check.equal("stepMismatch", flags(n2), "0.35");
        Check.equal("", flags(FormTestSupport.input(d, "n3")), "step=any never mismatches");
        HtmlInputElement n4 = FormTestSupport.input(d, "n4");
        n4.setValue("4");
        Check.equal("", flags(n4), "the step base is the minimum: 1, 4, 7");
        n4.setValue("5");
        Check.equal("stepMismatch", flags(n4), "5 is not on a step from the minimum");
        HtmlInputElement dt = FormTestSupport.input(d, "d");
        dt.setValue("2020-01-09");
        Check.equal("rangeUnderflow", flags(dt), "date underflow");
        dt.setValue("2020-01-21");
        Check.equal("rangeOverflow", flags(dt), "date overflow");
        dt.setValue("2020-01-15");
        Check.equal("", flags(dt), "date in range");
        HtmlInputElement mo = FormTestSupport.input(d, "m");
        mo.setValue("2020-01");
        Check.equal("rangeUnderflow", flags(mo), "month underflow");
        mo.setValue("2020-03");
        Check.equal("", flags(mo), "month in range");
        HtmlInputElement w = FormTestSupport.input(d, "w");
        w.setValue("2020-W21");
        Check.equal("rangeOverflow", flags(w), "week overflow");
        w.setValue("2020-W10");
        Check.equal("", flags(w), "week at the minimum");
        HtmlInputElement t = FormTestSupport.input(d, "t");
        t.setValue("08:59");
        Check.equal("rangeUnderflow stepMismatch", flags(t), "time underflow; the step is 900 s from the minimum 09:00");
        t.setValue("09:15");
        Check.equal("", flags(t), "time on a step");
        t.setValue("17:01");
        Check.equal("rangeOverflow stepMismatch", flags(t), "time overflow");
        HtmlInputElement t2 = FormTestSupport.input(d, "t2");
        t2.setValue("23:00");
        Check.equal("", flags(t2), "a reversed time range wraps around midnight: 23:00 is in 22:00..02:00");
        t2.setValue("01:00");
        Check.equal("", flags(t2), "01:00 too");
        t2.setValue("12:00");
        Check.equal("rangeUnderflow rangeOverflow", flags(t2), "12:00 is outside the wrapping range");
        HtmlInputElement l = FormTestSupport.input(d, "l");
        l.setValue("2019-12-31T23:59");
        Check.equal("rangeUnderflow", flags(l), "local date and time underflow");
        l.setValue("2020-06-01T12:00");
        Check.equal("", flags(l), "in range");
        Check.equal("", flags(FormTestSupport.input(d, "r")), "a range control is always valid");
    }

    /**
     * Bad input: the host reports text that is not a valid number or date.
     * <p>
     * bad input: 호스트가 올바른 숫자나 날짜가 아닌 글을 보고한다.
     */
    public void testBadInput() {
        Document d = FormTestSupport.page("<input id=n type=number><input id=dt type=date><input id=t>");
        HtmlInputElement n = FormTestSupport.input(d, "n");
        n.setValueFromUser("12abc");
        Check.equal("", n.getValue(), "an invalid number from the user is the empty value");
        Check.equal("badInput", flags(n), "and bad input");
        n.setValueFromUser("12");
        Check.equal("12", n.getValue(), "a valid number");
        Check.equal("", flags(n), "no bad input any more");
        n.setBadInput(true);
        Check.equal("badInput", flags(n), "the host reports unparsable text");
        n.setValue("3");
        Check.equal("", flags(n), "setting the value from script clears it");
        HtmlInputElement dt = FormTestSupport.input(d, "dt");
        dt.setValueFromUser("2020-13-45");
        Check.equal("badInput", flags(dt), "bad date");
        Check.equal("Please enter a valid value.", dt.getValidationMessage(), "the message of a bad date");
        HtmlInputElement t = FormTestSupport.input(d, "t");
        t.setValueFromUser("anything");
        Check.equal("", flags(t), "text never has bad input");
    }

    /**
     * Custom validity: a message makes the control invalid and is the validation message; an empty string clears it.
     * <p>
     * 사용자 지정 검증: 메시지가 있으면 컨트롤이 올바르지 않고 그것이 검증 메시지이다. 빈 문자열이면 해제된다.
     */
    public void testCustomValidity() {
        Document d = FormTestSupport.page("<input id=a><select id=s></select><textarea id=t></textarea><button id=b></button><fieldset id=f></fieldset><output id=o></output>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        a.setCustomValidity("Not this one");
        Check.equal("customError", flags(a), "custom error");
        Check.equal("Not this one", a.getValidationMessage(), "message");
        Check.isFalse(a.checkValidity(), "checkValidity");
        a.setCustomValidity("");
        Check.equal("", flags(a), "cleared");
        Check.equal("", a.getValidationMessage(), "no message");
        FormTestSupport.select(d, "s").setCustomValidity("x");
        Check.isFalse(FormTestSupport.select(d, "s").checkValidity(), "select");
        FormTestSupport.textarea(d, "t").setCustomValidity("x");
        Check.isFalse(FormTestSupport.textarea(d, "t").checkValidity(), "textarea");
        ((HtmlButtonElement) d.getElementById("b")).setCustomValidity("x");
        Check.isFalse(((HtmlButtonElement) d.getElementById("b")).checkValidity(), "a submit button");
        ((HtmlFieldSetElement) d.getElementById("f")).setCustomValidity("x");
        Check.isTrue(((HtmlFieldSetElement) d.getElementById("f")).checkValidity(), "a fieldset is barred");
        a.setCustomValidity("one");
        a.setAttribute("required", "");
        Check.equal("valueMissing customError", flags(a), "several flags at once");
        Check.equal("one", a.getValidationMessage(), "the custom message wins");
    }

    /**
     * <code>checkValidity</code> fires a cancelable <code>invalid</code> event that does not bubble; <code>reportValidity</code> focuses the first control
     * whose event was not cancelled and tells the host.
     * <p>
     * <code>checkValidity</code> 는 버블링하지 않는 취소 가능한 <code>invalid</code> 이벤트를 발생시킨다. <code>reportValidity</code> 는 이벤트가 취소되지 않은 첫
     * 컨트롤에 포커스를 주고 호스트에 알린다.
     */
    public void testInvalidEventAndReport() {
        Document d = FormTestSupport.page("<form id=f><input id=a required><input id=b required><input id=c></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        final List<String> log = new ArrayList<String>();
        final List<Boolean> flags = new ArrayList<Boolean>();
        EventListener rec = new EventListener() {
            /**
             * Records the event and its flags.
             * <p>
             * 이벤트와 그 플래그를 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add(e.getType() + "@" + ((org.duckdns.hjow.swingwebpanel.dom.Element) e.getTarget()).getId());
                flags.add(Boolean.valueOf(e.isBubbles()));
                flags.add(Boolean.valueOf(e.isCancelable()));
            }
        };
        d.getElementById("a").addEventListener("invalid", rec, false);
        d.getElementById("b").addEventListener("invalid", rec, false);
        d.getElementById("c").addEventListener("invalid", rec, false);
        d.getDocumentElement().addEventListener("invalid", rec, false);
        HtmlInputElement a = FormTestSupport.input(d, "a");
        Check.isFalse(a.checkValidity(), "checkValidity of an invalid control");
        Check.equal(Arrays.asList("invalid@a"), log, "the event is fired at the control and does not bubble to the root element");
        Check.equal(Arrays.asList(Boolean.FALSE, Boolean.TRUE), flags, "not bubbling, cancelable");
        Check.equal(0, host.reports.size(), "checkValidity does not report");
        log.clear();
        Check.isTrue(FormTestSupport.input(d, "c").checkValidity(), "a valid control fires no event");
        Check.equal(0, log.size(), "no event");
        HtmlFormElement f = FormTestSupport.form(d, "f");
        Check.isFalse(f.checkValidity(), "the form is invalid");
        Check.equal(Arrays.asList("invalid@a", "invalid@b"), log, "an event at every invalid control in tree order");
        Check.equal(0, host.reports.size(), "no report");
        log.clear();
        Check.isFalse(f.reportValidity(), "reportValidity of the form");
        Check.equal(Arrays.asList("invalid@a", "invalid@b"), log, "events");
        Check.equal(Arrays.asList("a: Please fill out this field."), host.reports, "only the first invalid control is reported");
        Check.same(a, FocusManager.getFocused(d), "and focused");
        // 첫 컨트롤의 invalid 이벤트를 취소하면 보고 대상은 다음 컨트롤이 된다.
        d.getElementById("a").addEventListener("invalid", new EventListener() {
            /**
             * Cancels the event.
             * <p>
             * 이벤트를 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                e.preventDefault();
            }
        }, false);
        host.reports.clear();
        Check.isFalse(f.reportValidity(), "again");
        Check.equal(Arrays.asList("b: Please fill out this field."), host.reports, "the first control whose event was not cancelled is reported");
        host.reports.clear();
        Check.isFalse(FormTestSupport.input(d, "b").reportValidity(), "reportValidity of a single control");
        Check.equal(Arrays.asList("b: Please fill out this field."), host.reports, "reported");
        host.reports.clear();
        Check.isFalse(a.reportValidity(), "reportValidity returns false although the event was cancelled");
        Check.equal(0, host.reports.size(), "a cancelled invalid event suppresses the report");
    }

    /**
     * A submission validates the form interactively unless <code>novalidate</code> or <code>formnovalidate</code> turns it off; an invalid form is not submitted.
     * <p>
     * 제출은 <code>novalidate</code>/<code>formnovalidate</code> 가 끄지 않는 한 폼을 대화형으로 검증한다. 올바르지 않은 폼은 제출되지 않는다.
     */
    public void testSubmissionValidates() {
        Document d = FormTestSupport.page("<form id=f action=/s><input id=a name=a required><button id=go>go</button><button id=skip formnovalidate>skip</button>"
                + "<button id=other formaction=/o>other</button></form><form id=g novalidate action=/s><input name=a required><button id=g1>go</button></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        final List<String> log = new ArrayList<String>();
        d.addEventListener("submit", FormTestSupport.logger(log), true);
        HtmlFormElement f = FormTestSupport.form(d, "f");
        f.requestSubmit((org.duckdns.hjow.swingwebpanel.dom.Element) d.getElementById("go"));
        Check.equal(0, host.submissions.size(), "an invalid form is not submitted");
        Check.equal(0, log.size(), "and no submit event is fired");
        Check.equal(1, host.reports.size(), "the problem is reported");
        f.requestSubmit((org.duckdns.hjow.swingwebpanel.dom.Element) d.getElementById("skip"));
        Check.equal(1, host.submissions.size(), "formnovalidate skips the validation");
        Check.equal(Arrays.asList("submit@f"), log, "the submit event was fired");
        FormTestSupport.form(d, "g").requestSubmit(null);
        Check.equal(2, host.submissions.size(), "novalidate on the form");
        f.submit();
        Check.equal(3, host.submissions.size(), "submit() never validates");
        Check.equal(Arrays.asList("submit@f", "submit@g"), log, "and fires no submit event");
        FormTestSupport.input(d, "a").setValue("v");
        f.requestSubmit(null);
        Check.equal(4, host.submissions.size(), "a valid form is submitted");
    }

    /**
     * The validity state is live and its object is the same each time.
     * <p>
     * validity 상태는 라이브이며 객체는 매번 같다.
     */
    public void testValidityObjectIsLive() {
        Document d = FormTestSupport.page("<input id=a>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        ValidityState v = a.getValidity();
        Check.same(v, a.getValidity(), "same object");
        Check.isTrue(v.isValid(), "valid");
        a.setAttribute("required", "");
        Check.isTrue(v.isValueMissing(), "the earlier object follows the state");
        Check.isFalse(v.isValid(), "invalid now");
        a.setAttribute("type", "email");
        a.setValue("x");
        Check.isTrue(v.isTypeMismatch(), "type mismatch after changing the type");
    }

    /**
     * The default messages of the validity states.
     * <p>
     * validity 상태의 기본 메시지.
     */
    public void testValidationMessages() {
        Document d = FormTestSupport.page("<input id=a required><input id=b type=email value=x><input id=c type=number min=1 max=5 step=2><input id=d type=checkbox required>"
                + "<input id=e type=radio name=r required><input id=f type=url value=x><input id=g type=file required><input id=h type=number min=2>");
        Check.equal("Please fill out this field.", FormTestSupport.input(d, "a").getValidationMessage(), "required");
        Check.equal("Please enter an email address.", FormTestSupport.input(d, "b").getValidationMessage(), "email");
        Check.equal("Please check this box if you want to proceed.", FormTestSupport.input(d, "d").getValidationMessage(), "checkbox");
        Check.equal("Please select one of these options.", FormTestSupport.input(d, "e").getValidationMessage(), "radio");
        Check.equal("Please enter a URL.", FormTestSupport.input(d, "f").getValidationMessage(), "url");
        Check.equal("Please select a file.", FormTestSupport.input(d, "g").getValidationMessage(), "file");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        c.setValue("0");
        Check.equal("Value must be greater than or equal to 1.", c.getValidationMessage(), "underflow");
        c.setValue("6");
        Check.equal("Value must be less than or equal to 5.", c.getValidationMessage(), "overflow");
        c.setValue("2");
        Check.equal("Please enter a valid value. The two nearest valid values are 1 and 3.", c.getValidationMessage(), "step mismatch");
        FormTestSupport.input(d, "h").setValue("1");
        Check.equal("Value must be greater than or equal to 2.", FormTestSupport.input(d, "h").getValidationMessage(), "underflow, default step 1");
    }
}
