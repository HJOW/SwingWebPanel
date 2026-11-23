package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FormHostAdapter;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the JavaScript side of the form controls (plan items P11-05 to P11-07 and P15-02): the interfaces and their prototype chains, the IDL
 * attributes that read and write the state of the controls instead of the content attributes, the collections, the validation API, the
 * <code>Option</code> constructor, the dates and files, the exceptions, and the submission through <code>form.submit()</code> and
 * <code>requestSubmit()</code> to the host of the document.
 * <p>
 * 폼 컨트롤의 JavaScript 쪽 시험(계획 P11-05~P11-07, P15-02): 인터페이스와 프로토타입 체인, 콘텐츠 속성이 아니라 컨트롤의 상태를 읽고 쓰는 IDL 속성, 컬렉션,
 * 검증 API, <code>Option</code> 생성자, 날짜와 파일, 예외, <code>form.submit()</code> 과 <code>requestSubmit()</code> 로 문서의 호스트에 이르는 제출.
 */
public class FormBindingTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormBindingTest() {
    }

    /**
     * The test page.
     * <p>
     * 시험 페이지.
     */
    private static final String PAGE = "<!doctype html><html><head><title>T</title></head><body>"
            + "<form id=f action='/save' method=post>"
            + "<input id=t name=q value='abc'><input id=cb type=checkbox name=c value=yes checked><input id=r1 type=radio name=g value=1><input id=r2 type=radio name=g value=2 checked>"
            + "<select id=s name=s><option id=o1 value=a>Alpha</option><option id=o2 value=b selected>Beta</option><option id=o3>  Gamma   x </option></select>"
            + "<textarea id=ta name=ta>\nhello\nworld</textarea><button id=go name=btn value=B>Go</button><input id=n type=number name=n min=0 max=10 step=2 value=4>"
            + "<fieldset id=fs><legend id=lg>l</legend></fieldset><output id=out name=out>7</output><progress id=pr value=3 max=10></progress>"
            + "<meter id=me value=2 min=0 max=4></meter></form>"
            + "<label id=lb for=t>Label</label><div id=d></div><datalist id=dl><option value=x></option><option value=y></option></datalist></body></html>";

    /**
     * A host that records the submissions.
     * <p>
     * 제출을 기록하는 호스트.
     */
    private static final class Host extends FormHostAdapter {

        /** The submissions. / 제출 정보. */
        final List<FormSubmission> submissions = new ArrayList<FormSubmission>();

        /** The reported invalid controls. / 보고된 올바르지 않은 컨트롤. */
        final List<String> reports = new ArrayList<String>();

        /**
         * Creates the host.
         * <p>
         * 호스트를 만든다.
         */
        Host() {
        }

        /**
         * Records a submission.
         * <p>
         * 제출 정보를 기록한다.
         *
         * @param submission the submission / 제출 정보
         * @param form       the form / 폼
         */
        public void submit(FormSubmission submission, Element form) {
            submissions.add(submission);
        }

        /**
         * Records a report.
         * <p>
         * 보고를 기록한다.
         *
         * @param control the control / 컨트롤
         * @param message the message / 메시지
         */
        public void reportInvalid(Element control, String message) {
            reports.add(control.getId() + ":" + message);
        }
    }

    /**
     * Creates the environment of the test page.
     * <p>
     * 시험 페이지의 환경을 만든다.
     *
     * @return the environment / 환경
     */
    private static DomTestEnv env() {
        return new DomTestEnv(PAGE);
    }

    /**
     * Returns the text of a byte array that holds ASCII.
     * <p>
     * ASCII 를 담은 바이트 배열의 문자열을 반환한다.
     *
     * @param b the bytes / 바이트
     * @return the text / 문자열
     */
    private static String ascii(byte[] b) {
        StringBuilder s = new StringBuilder(b.length);
        // 바이트를 문자로 옮긴다.
        for (int i = 0; i < b.length; i++) {
            s.append((char) (b[i] & 0x7F));
        }
        return s.toString();
    }

    /**
     * The interfaces: classes, prototype chains, constructor names and the types of elements that had no interface before.
     * <p>
     * 인터페이스: 클래스, 프로토타입 체인, 생성자 이름, 이전에는 인터페이스가 없던 요소의 종류.
     */
    public void testInterfaces() {
        DomTestEnv e = env();
        Check.equal("[object HTMLFormElement]", e.eval("Object.prototype.toString.call(document.getElementById('f'))"), "form");
        Check.equal("[object HTMLInputElement]", e.eval("Object.prototype.toString.call(document.getElementById('t'))"), "input");
        Check.equal("[object HTMLSelectElement]", e.eval("Object.prototype.toString.call(document.getElementById('s'))"), "select");
        Check.equal("[object HTMLOptionElement]", e.eval("Object.prototype.toString.call(document.getElementById('o1'))"), "option");
        Check.equal("[object HTMLTextAreaElement]", e.eval("Object.prototype.toString.call(document.getElementById('ta'))"), "textarea");
        Check.equal("[object HTMLButtonElement]", e.eval("Object.prototype.toString.call(document.getElementById('go'))"), "button");
        Check.equal("[object HTMLFieldSetElement]", e.eval("Object.prototype.toString.call(document.getElementById('fs'))"), "fieldset");
        Check.equal("[object HTMLLegendElement]", e.eval("Object.prototype.toString.call(document.getElementById('lg'))"), "legend");
        Check.equal("[object HTMLOutputElement]", e.eval("Object.prototype.toString.call(document.getElementById('out'))"), "output");
        Check.equal("[object HTMLProgressElement]", e.eval("Object.prototype.toString.call(document.getElementById('pr'))"), "progress");
        Check.equal("[object HTMLMeterElement]", e.eval("Object.prototype.toString.call(document.getElementById('me'))"), "meter");
        Check.equal("[object HTMLDataListElement]", e.eval("Object.prototype.toString.call(document.getElementById('dl'))"), "datalist");
        Check.equal("[object HTMLLabelElement]", e.eval("Object.prototype.toString.call(document.getElementById('lb'))"), "label");
        Check.equal("true", e.eval("document.getElementById('t') instanceof HTMLInputElement && document.getElementById('t') instanceof HTMLElement"),
                "prototype chain");
        Check.equal("true", e.eval("Object.getPrototypeOf(HTMLInputElement) === HTMLElement"), "interface object chain");
        Check.equal("HTMLInputElement", e.eval("document.createElement('input').constructor.name"), "createElement('input')");
        Check.equal("[object HTMLOptGroupElement]", e.eval("Object.prototype.toString.call(document.createElement('optgroup'))"), "optgroup");
        Check.equal("function", e.eval("typeof Option"), "Option is a function");
        Check.equal("true", e.eval("new Option() instanceof HTMLOptionElement"), "new Option() is an option");
        Check.equal("TypeError", e.eval("try { Option(); 'no' } catch (x) { x.name }"), "Option needs new");
        Check.equal("[object ValidityState]", e.eval("Object.prototype.toString.call(document.getElementById('t').validity)"), "validity");
    }

    /**
     * The value and checkedness IDL attributes follow the state of the control; the content attributes only provide defaults.
     * <p>
     * value 와 checkedness IDL 속성은 컨트롤의 상태를 따르고 콘텐츠 속성은 기본값만 제공한다.
     */
    public void testValueAndChecked() {
        DomTestEnv e = env();
        Check.equal("abc", e.eval("document.getElementById('t').value"), "value");
        Check.equal("abc", e.eval("document.getElementById('t').defaultValue"), "defaultValue");
        Check.equal("typed|abc|abc", e.eval("var t = document.getElementById('t'); t.value = 'typed'; [t.value, t.defaultValue, t.getAttribute('value')].join('|')"),
                "setting value changes the state, not the attribute");
        Check.equal("typed", e.eval("document.getElementById('t').setAttribute('value', 'zzz'); document.getElementById('t').value"), "dirty value ignores the attribute");
        Check.equal("zzz", e.eval("document.getElementById('t').defaultValue"), "defaultValue follows the attribute");
        Check.equal("true|true", e.eval("var c = document.getElementById('cb'); [c.checked, c.defaultChecked].join('|')"), "checked");
        Check.equal("false|true|checked", e.eval("c.checked = false; [c.checked, c.defaultChecked, c.getAttribute('checked') === '' ? 'checked' : 'none'].join('|')"),
                "checkedness is separate from the attribute");
        Check.equal("yes", e.eval("c.value"), "value of a checkbox");
        Check.equal("number", e.eval("document.getElementById('n').type"), "type");
        Check.equal("text", e.eval("var x = document.createElement('input'); x.setAttribute('type', 'BOGUS'); x.type"), "unknown type is text");
        Check.equal("hidden|hidden", e.eval("x.type = 'hidden'; [x.type, x.getAttribute('type')].join('|')"), "type setter");
        Check.equal("InvalidStateError", e.eval("var fi = document.createElement('input'); fi.type = 'file'; try { fi.value = 'x'; 'no' } catch (err) { err.name }"),
                "a file input cannot be given a value");
        Check.equal("", e.eval("fi.value = ''; fi.value"), "but can be cleared");
        Check.equal("TypeError", e.eval("try { Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').get.call(document.body); 'no' } catch (err) { err.name }"),
                "illegal invocation");
    }

    /**
     * The radio button group through script.
     * <p>
     * 스크립트로 다루는 라디오 버튼 그룹.
     */
    public void testRadioGroup() {
        DomTestEnv e = env();
        Check.equal("false|true", e.eval("var r1 = document.getElementById('r1'), r2 = document.getElementById('r2'); [r1.checked, r2.checked].join('|')"), "initial");
        Check.equal("true|false", e.eval("r1.checked = true; [r1.checked, r2.checked].join('|')"), "checking one unchecks the other");
        Check.equal("2", e.eval("String(document.getElementById('f').elements.g.length)"), "the radio node list");
        Check.equal("1", e.eval("document.getElementById('f').elements.g.value"), "RadioNodeList.value is the checked radio's value");
        Check.equal("2|false|true", e.eval("var g = document.getElementById('f').elements.g; g.value = '2'; [g.value, r1.checked, r2.checked].join('|')"), "setting value");
        Check.equal("[object RadioNodeList]", e.eval("Object.prototype.toString.call(g)"), "class");
        Check.equal("true", e.eval("g[0] === r1 && g[1] === r2 && g.item(1) === r2"), "indexed access");
    }

    /**
     * The <code>select</code> and <code>option</code> interfaces.
     * <p>
     * <code>select</code> 와 <code>option</code> 인터페이스.
     */
    public void testSelect() {
        DomTestEnv e = env();
        Check.equal("1|b|Beta", e.eval("var s = document.getElementById('s'); [s.selectedIndex, s.value, s.options[s.selectedIndex].text].join('|')"), "initial");
        Check.equal("3|select-one", e.eval("[s.length, s.type].join('|')"), "length and type");
        Check.equal("0|a", e.eval("s.selectedIndex = 0; [s.selectedIndex, s.value].join('|')"), "selectedIndex");
        Check.equal("2|Gamma x", e.eval("s.value = 'Gamma x'; [s.selectedIndex, s.value].join('|')"), "value of an option without a value attribute is its text");
        Check.equal("-1|", e.eval("s.value = 'none'; [s.selectedIndex, s.value].join('|')"), "unknown value selects nothing");
        Check.equal("true|false|a", e.eval("var o1 = document.getElementById('o1'); o1.selected = true; [o1.selected, o1.defaultSelected, o1.value].join('|')"), "option.selected");
        Check.equal("true", e.eval("s[0] === o1 && s.options[0] === o1 && s.item(1) === document.getElementById('o2') && s.namedItem('o3') === document.getElementById('o3')"),
                "indexed and named access");
        Check.equal("3", e.eval("String(s.options.length)"), "options.length");
        Check.equal("4|d|D", e.eval("var opt = new Option('D', 'd', false, true); s.add(opt); [s.length, s.value, opt.text].join('|')"), "add(new Option) selects it");
        Check.equal("5|0", e.eval("s.add(new Option('Z', 'z'), 0); [s.length, s.options[0].value === 'z' ? 0 : 1].join('|')"), "add before an index");
        Check.equal("true|6", e.eval("var ref = s.options[2]; s.add(new Option('Y', 'y'), ref); [s.options[2].value === 'y', s.length].join('|')"), "add before an element");
        Check.equal("5", e.eval("s.remove(0); String(s.length)"), "remove(index)");
        Check.equal("4", e.eval("s.options.remove(0); String(s.length)"), "options.remove");
        Check.equal("2", e.eval("s.options.length = 2; String(s.length)"), "options.length setter truncates");
        Check.equal("TypeError", e.eval("try { s.add(document.createElement('div')); 'no' } catch (x) { x.name }"), "add needs an option");
        Check.equal("selected|true|false", e.eval("var o = new Option('t', 'v', true); [o.getAttribute('selected') === '' ? 'selected' : 'no', o.selected, o.getAttribute('value') === 'v' ? false : true].join('|')"),
                "Option(text, value, defaultSelected)");
        Check.equal("true", e.eval("var m = document.createElement('select'); m.multiple = true; m.appendChild(new Option('1')); m.appendChild(new Option('2', '2', false, true)); m.appendChild(new Option('3', '3', false, true));"
                + " m.selectedOptions.length === 2 && m.selectedOptions[1].value === '3' && m.selectedIndex === 1"), "multiple select and selectedOptions");
        Check.equal("true|true", e.eval("var box = document.createElement('div'); var sel = document.createElement('select'); box.appendChild(sel); var attached = sel.parentNode === box; sel.remove(); [attached, sel.parentNode === null].join('|')"),
                "select.remove() without arguments removes the element itself");
    }

    /**
     * The <code>textarea</code> interface and the selection API.
     * <p>
     * <code>textarea</code> 인터페이스와 선택 API.
     */
    public void testTextArea() {
        DomTestEnv e = env();
        Check.equal("hello\nworld", e.eval("document.getElementById('ta').value"), "the first newline was dropped by the parser");
        Check.equal("hello\nworld|11", e.eval("var ta = document.getElementById('ta'); [ta.defaultValue, ta.textLength].join('|')"), "defaultValue and textLength");
        Check.equal("hello world|hello\nworld", e.eval("ta.value = 'hello world'; [ta.value, ta.defaultValue].join('|')"), "setting the value keeps the default");
        Check.equal("11|11", e.eval("[ta.selectionStart, ta.selectionEnd].join('|')"), "the cursor moves to the end when the value changes");
        Check.equal("0|5|forward", e.eval("ta.setSelectionRange(0, 5, 'forward'); [ta.selectionStart, ta.selectionEnd, ta.selectionDirection].join('|')"), "setSelectionRange");
        Check.equal("HELLO world", e.eval("ta.setRangeText('HELLO'); ta.value"), "setRangeText replaces the selection");
        Check.equal("HELLO there", e.eval("ta.setRangeText('there', 6, 11); ta.value"), "setRangeText with a range");
        Check.equal("textarea|2", e.eval("[ta.type, ta.rows].join('|')"), "type and rows");
        Check.equal("IndexSizeError", e.eval("try { ta.rows = 0; 'no' } catch (x) { x.name }"), "rows must be positive");
        Check.equal("null|null", e.eval("var t = document.getElementById('t'); t.type = 'number'; [String(t.selectionStart), String(t.selectionEnd)].join('|')"), "the selection API does not apply to number inputs");
        Check.equal("InvalidStateError", e.eval("try { t.setSelectionRange(0, 1); 'no' } catch (x) { x.name }"), "setSelectionRange throws for number inputs");
    }

    /**
     * The form interface: <code>elements</code>, named and indexed access, enumerated attributes, reset and the submission to the host.
     * <p>
     * form 인터페이스: <code>elements</code>, 이름과 위치 접근, 열거형 속성, 초기화, 호스트로의 제출.
     */
    public void testForm() {
        DomTestEnv e = env();
        Host host = new Host();
        e.document.setFormHost(host);
        Check.equal("[object HTMLFormControlsCollection]", e.eval("Object.prototype.toString.call(document.getElementById('f').elements)"), "elements");
        Check.equal("10", e.eval("var f = document.getElementById('f'); String(f.elements.length)"), "number of listed elements");
        Check.equal("10", e.eval("String(f.length)"), "form.length");
        Check.equal("true", e.eval("f.q === document.getElementById('t') && f.elements.q === f.q && f[0] === f.q && f.elements[0] === f.q && f.elements.namedItem('s') === document.getElementById('s')"),
                "named and indexed access");
        Check.equal("post|http://example.test/save|application/x-www-form-urlencoded", e.eval("[f.method, f.action, f.enctype].join('|')"), "enumerated attributes and the resolved action");
        Check.equal("get|multipart/form-data", e.eval("f.method = 'BOGUS'; f.enctype = 'MULTIPART/FORM-DATA'; [f.method, f.enctype].join('|')"), "invalid method falls back");
        Check.equal("multipart/form-data", e.eval("f.encoding"), "encoding is an alias of enctype");
        e.eval("f.method = 'post'; f.enctype = ''");
        Check.equal(0, host.submissions.size(), "nothing was submitted yet");
        e.eval("f.submit()");
        Check.equal(1, host.submissions.size(), "submit() reached the host");
        FormSubmission s = host.submissions.get(0);
        Check.equal("POST", s.getMethod(), "method");
        Check.equal("http://example.test/save", s.getUrl(), "url");
        Check.equal("q=abc&c=yes&g=2&s=b&ta=hello%0D%0Aworld&n=4", ascii(s.getBody()), "body");
        e.eval("document.getElementById('go').dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}))");
        Check.equal(2, host.submissions.size(), "a click on the submit button submits");
        Check.equal("q=abc&c=yes&g=2&s=b&ta=hello%0D%0Aworld&btn=B&n=4", ascii(host.submissions.get(1).getBody()), "the submitter is in the entry list");
        e.eval("f.addEventListener('submit', function (ev) { ev.preventDefault(); globalThis_submitter = ev.submitter && ev.submitter.id; });"
                + "f.requestSubmit(document.getElementById('go'))");
        Check.equal(2, host.submissions.size(), "a cancelled submit event stops it");
        Check.equal("go", e.eval("String(globalThis_submitter)"), "event.submitter");
        Check.equal("TypeError", e.eval("try { f.requestSubmit(document.getElementById('t')); 'no' } catch (x) { x.name }"), "requestSubmit needs a submit button");
        Check.equal("NotFoundError", e.eval("try { f.requestSubmit(document.createElement('button')); 'no' } catch (x) { x.name }"), "a submit button of another form");
        e.eval("f.q.value = 'changed'; f.c.checked = false; f.s.selectedIndex = 0; f.reset()");
        Check.equal("abc|true|1", e.eval("[f.q.value, f.c.checked, f.s.selectedIndex].join('|')"), "reset");
        Check.equal("true", e.eval("var resetSeen = false; f.addEventListener('reset', function (ev) { resetSeen = true; ev.preventDefault(); }); f.q.value = 'x'; f.reset(); resetSeen && f.q.value === 'x'"),
                "a cancelled reset event keeps the values");
    }

    /**
     * The constraint validation API of the controls and the form.
     * <p>
     * 컨트롤과 폼의 제약 검증 API.
     */
    public void testValidation() {
        DomTestEnv e = env();
        Host host = new Host();
        e.document.setFormHost(host);
        Check.equal("true|true|", e.eval("var t = document.getElementById('t'); [t.willValidate, t.validity.valid, t.validationMessage].join('|')"), "a valid control");
        Check.equal("true|false|Please fill out this field.", e.eval("t.required = true; t.value = ''; [t.validity.valueMissing, t.validity.valid, t.validationMessage].join('|')"), "valueMissing");
        Check.equal("false", e.eval("String(t.checkValidity())"), "checkValidity");
        Check.equal("true", e.eval("var log = []; t.addEventListener('invalid', function (ev) { log.push(ev.type + ':' + ev.bubbles + ':' + ev.cancelable); }); t.checkValidity(); log.join() === 'invalid:false:true'"),
                "the invalid event does not bubble and is cancelable");
        Check.equal("false", e.eval("var f = document.getElementById('f'); String(f.checkValidity())"), "the form is invalid");
        Check.equal("false", e.eval("String(f.reportValidity())"), "reportValidity");
        Check.equal(Arrays.asList("t:Please fill out this field."), host.reports, "the host got the first invalid control");
        Check.equal("true", e.eval("String(document.activeElement === t)"), "which got the focus");
        Check.equal("customError|oops", e.eval("t.required = false; t.setCustomValidity('oops'); [t.validity.customError ? 'customError' : 'none', t.validationMessage].join('|')"), "custom validity");
        Check.equal("true", e.eval("t.setCustomValidity(''); String(t.validity.valid)"), "cleared");
        Check.equal("rangeOverflow|stepMismatch", e.eval("var n = document.getElementById('n'); n.value = '11'; [n.validity.rangeOverflow ? 'rangeOverflow' : '-', n.validity.stepMismatch ? 'stepMismatch' : '-'].join('|')"),
                "range and step");
        Check.equal("true|false", e.eval("n.value = '3'; [n.validity.stepMismatch, n.validity.rangeUnderflow].join('|')"), "step mismatch only");
        Check.equal("false|false", e.eval("[document.getElementById('fs').willValidate, document.getElementById('out').willValidate].join('|')"), "fieldset and output are barred");
        Check.equal("typeMismatch", e.eval("var em = document.createElement('input'); em.type = 'email'; em.value = 'nope'; em.validity.typeMismatch ? 'typeMismatch' : 'ok'"), "email");
        Check.equal("patternMismatch", e.eval("em.type = 'text'; em.pattern = '[0-9]+'; em.value = 'x'; em.validity.patternMismatch ? 'patternMismatch' : 'ok'"), "pattern");
        Check.equal("true", e.eval("var vs = t.validity; t.setCustomValidity('z'); String(vs === t.validity && vs.customError)"), "the same live ValidityState object");
    }

    /**
     * Numbers and dates: <code>valueAsNumber</code>, <code>valueAsDate</code> as a <code>Date</code>, <code>stepUp</code> and <code>stepDown</code>.
     * <p>
     * 숫자와 날짜: <code>valueAsNumber</code>, <code>Date</code> 인 <code>valueAsDate</code>, <code>stepUp</code>, <code>stepDown</code>.
     */
    public void testNumbersAndDates() {
        DomTestEnv e = env();
        Check.equal("4|number", e.eval("var n = document.getElementById('n'); [n.valueAsNumber, typeof n.valueAsNumber].join('|')"), "valueAsNumber");
        Check.equal("6|0|10|2", e.eval("n.stepUp(); [n.value, n.min, n.max, n.step].join('|')"), "stepUp() steps by one step (2)");
        Check.equal("2", e.eval("n.stepDown(2); n.value"), "stepDown(2) with step 2 from 6 gives 2");
        Check.equal("8", e.eval("n.valueAsNumber = 8; n.value"), "set valueAsNumber");
        Check.equal("true", e.eval("n.value = ''; String(n.valueAsNumber !== n.valueAsNumber)"), "an empty value is NaN");
        Check.equal("true|1970-01-03|172800000", e.eval("var d = document.createElement('input'); d.type = 'date'; d.valueAsDate = new Date(Date.UTC(1970, 0, 3, 12, 0, 0));"
                + " [d.valueAsDate instanceof Date, d.value, d.valueAsNumber].join('|')"), "valueAsDate");
        Check.equal("true|1970-01-03", e.eval("[d.valueAsDate.getTime() === 172800000, d.value].join('|')"), "reading valueAsDate gives a Date");
        Check.equal("null|", e.eval("d.valueAsDate = null; [String(d.valueAsDate), d.value].join('|')"), "null clears");
        Check.equal("TypeError", e.eval("try { d.valueAsDate = 5; 'no' } catch (x) { x.name }"), "a number is not a Date");
        Check.equal("null", e.eval("String(document.getElementById('t').valueAsDate)"), "valueAsDate does not apply to text");
        Check.equal("InvalidStateError", e.eval("try { document.getElementById('t').stepUp(); 'no' } catch (x) { x.name }"), "stepUp does not apply to text");
        Check.equal("3|50", e.eval("var rg = document.createElement('input'); rg.type = 'range'; var a = rg.value; rg.value = '2.5'; [rg.value, a].join('|')"),
                "range value is sanitized: the default is 50, 2.5 is rounded to the nearest step 3 (the greater one when equally near)");
    }

    /**
     * Labels, <code>form</code>, <code>control</code>, fieldset, output, progress, meter and datalist.
     * <p>
     * 레이블, <code>form</code>, <code>control</code>, fieldset, output, progress, meter, datalist.
     */
    public void testOtherElements() {
        DomTestEnv e = env();
        Check.equal("true|1", e.eval("var lb = document.getElementById('lb'); var t = document.getElementById('t'); [lb.control === t, t.labels.length].join('|')"), "label.control and labels");
        Check.equal("true|true", e.eval("[t.labels[0] === lb, t.form === document.getElementById('f')].join('|')"), "labels[0] and form");
        Check.equal("true", e.eval("String(lb.form === document.getElementById('f'))"), "the form of a label is the form of its labeled control");
        Check.equal("fieldset|7|output", e.eval("[document.getElementById('fs').type, document.getElementById('out').value, document.getElementById('out').type].join('|')"), "fieldset and output");
        Check.equal("0", e.eval("String(document.getElementById('fs').elements.length)"), "fieldset.elements lists controls only (a legend is none)");
        Check.equal("3|10|0.3", e.eval("var p = document.getElementById('pr'); [p.value, p.max, p.position].join('|')"), "progress");
        Check.equal("2|0|4", e.eval("var me = document.getElementById('me'); [me.value, me.min, me.max].join('|')"), "meter");
        Check.equal("2", e.eval("String(document.getElementById('dl').options.length)"), "datalist.options");
        Check.equal("0", e.eval("String(p.labels.length)"), "progress.labels");
    }

    /**
     * Files: <code>input.files</code> is a <code>FileList</code> of <code>File</code> objects that the realm's factory creates.
     * <p>
     * 파일: <code>input.files</code> 는 realm 의 팩터리가 만드는 <code>File</code> 객체의 <code>FileList</code> 이다.
     */
    public void testFiles() {
        final DomTestEnv e = env();
        final List<FileEntry> made = new ArrayList<FileEntry>();
        class Factory extends JsObject implements FormBindings.FileFactory {
            /**
             * Creates the factory object.
             * <p>
             * 팩터리 객체를 만든다.
             */
            Factory() {
                super(e.realm.getObjectPrototype());
            }

            /**
             * Creates a plain object with the name and size of the file.
             * <p>
             * 파일의 이름과 크기를 가진 일반 객체를 만든다.
             *
             * @param entry the file / 파일
             * @return the object / 객체
             */
            public JsObject newFile(FileEntry entry) {
                made.add(entry);
                JsObject o = e.realm.newObject();
                o.defineData("name", Js.str(entry.getName()), true, true, true);
                o.defineData("size", Js.num(entry.getSize()), true, true, true);
                return o;
            }
        }
        e.realm.setIntrinsic(FormBindings.FILE_FACTORY_KEY, new Factory());
        e.eval("var fi = document.createElement('input'); fi.type = 'file'; fi.multiple = true; document.body.appendChild(fi);");
        Check.equal("[object FileList]|0", e.eval("[Object.prototype.toString.call(fi.files), fi.files.length].join('|')"), "an empty list");
        Check.equal("null", e.eval("String(document.getElementById('t').files)"), "files of a text input is null");
        HtmlInputElement in = (HtmlInputElement) e.document.getElementsByTagName("input").item(e.document.getElementsByTagName("input").getLength() - 1);
        in.setSelectedFiles(Arrays.asList(new FileEntry("a.txt", "text/plain", 1, new byte[] {1, 2, 3}), new FileEntry("b.txt", "", 2, new byte[0])));
        Check.equal("2|a.txt|3|b.txt|C:\\fakepath\\a.txt", e.eval("[fi.files.length, fi.files[0].name, fi.files[0].size, fi.files.item(1).name, fi.value].join('|')"), "files");
        Check.equal("true|true", e.eval("[fi.files === fi.files, fi.files[0] === fi.files[0]].join('|')"), "same objects");
        Check.equal(2, made.size(), "each file object was made once");
        Check.equal("null", e.eval("String(fi.files.item(5))"), "an index out of range");
        Check.equal("0", e.eval("fi.value = ''; String(fi.files.length)"), "clearing the value clears the files");
    }

    /**
     * Clicks from script run the activation behavior: a checkbox toggles, a label forwards the click, a listener sees the new state and can cancel it.
     * <p>
     * 스크립트의 click 은 활성화 동작을 실행한다: 체크박스가 토글되고, label 이 click 을 전달하고, 리스너는 새 상태를 보며 취소할 수 있다.
     */
    public void testClickActivation() {
        DomTestEnv e = env();
        Check.equal("false", e.eval("var cb = document.getElementById('cb'); cb.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true})); String(cb.checked)"),
                "a synthetic mouse click toggles the checkbox");
        Check.equal("false", e.eval("cb.dispatchEvent(new Event('click', {bubbles: true})); String(cb.checked)"), "a plain event does not");
        Check.equal("true", e.eval("var lb = document.getElementById('lb'); var t = document.getElementById('t'); t.type = 'checkbox'; t.checked = false;"
                + " lb.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true})); String(t.checked)"), "a click on a label toggles its control");
        Check.equal("true", e.eval("var seen = []; cb.addEventListener('click', function (ev) { seen.push(cb.checked); ev.preventDefault(); });"
                + " var before = cb.checked; cb.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true})); String(cb.checked === before && seen[0] === !before)"),
                "listeners see the toggled state, and preventDefault restores it");
    }
}
