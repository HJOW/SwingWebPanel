package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the state that the form controls keep besides their attributes (plan item P11-05, HTML Standard 4.10): the element classes of the
 * factory, the value modes and the dirty value flag, the value sanitization of every input type, checkedness and the radio button groups,
 * indeterminateness, textarea and output values, the form owner and <code>form.elements</code>, labels and disabled fieldsets, cloning and the reset
 * algorithm. The documents are produced by the real HTML parser.
 * <p>
 * 폼 컨트롤이 속성 외에 가지는 상태의 시험(계획 P11-05, HTML 표준 4.10): 팩터리의 요소 클래스, value 모드와 dirty value flag, 모든 input 종류의 값 정리,
 * checkedness 와 라디오 버튼 그룹, indeterminateness, textarea 와 output 의 값, 폼 소유자와 <code>form.elements</code>, label 과 비활성 fieldset, 복제와 초기화 알고리즘.
 * 문서는 실제 HTML 파서가 만든다.
 */
public class FormControlStateTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormControlStateTest() {
    }

    /**
     * The parser and <code>createElement</code> create the specialized classes for the HTML namespace; other elements and the same names in
     * other namespaces or in an XML document stay generic.
     * <p>
     * 파서와 <code>createElement</code> 는 HTML 네임스페이스에 대해 특화된 클래스를 만든다. 다른 요소와 다른 네임스페이스나 XML 문서의 같은 이름은 일반 요소로 남는다.
     */
    public void testFactoryClasses() {
        Document d = FormTestSupport.page("<form id=f><input id=i><button id=b></button><select id=s><optgroup id=g><option id=o></option></optgroup></select>"
                + "<textarea id=t></textarea><label id=l></label><fieldset id=fs><legend id=lg></legend></fieldset><output id=out></output>"
                + "<progress id=p></progress><meter id=m></meter><datalist id=dl></datalist><div id=div></div></form>");
        Check.isTrue(d.getElementById("f") instanceof HtmlFormElement, "form");
        Check.isTrue(d.getElementById("i") instanceof HtmlInputElement, "input");
        Check.isTrue(d.getElementById("b") instanceof HtmlButtonElement, "button");
        Check.isTrue(d.getElementById("s") instanceof HtmlSelectElement, "select");
        Check.isTrue(d.getElementById("g") instanceof HtmlOptGroupElement, "optgroup");
        Check.isTrue(d.getElementById("o") instanceof HtmlOptionElement, "option");
        Check.isTrue(d.getElementById("t") instanceof HtmlTextAreaElement, "textarea");
        Check.isTrue(d.getElementById("l") instanceof HtmlLabelElement, "label");
        Check.isTrue(d.getElementById("fs") instanceof HtmlFieldSetElement, "fieldset");
        Check.isTrue(d.getElementById("lg") instanceof HtmlLegendElement, "legend");
        Check.isTrue(d.getElementById("out") instanceof HtmlOutputElement, "output");
        Check.isTrue(d.getElementById("p") instanceof HtmlProgressElement, "progress");
        Check.isTrue(d.getElementById("m") instanceof HtmlMeterElement, "meter");
        Check.isTrue(d.getElementById("dl") instanceof HtmlDataListElement, "datalist");
        Check.isTrue(d.getElementById("div") instanceof HtmlGenericElement, "div is generic");
        Check.isTrue(d.createElement("input") instanceof HtmlInputElement, "createElement creates an input");
        Check.isTrue(d.createElementNS("http://www.w3.org/2000/svg", "input") instanceof HtmlGenericElement, "an svg input is generic");
        Check.isTrue(d.createElementNS(null, "input") instanceof HtmlGenericElement, "an input without namespace is generic");
        Document x = Document.createXmlDocument("application/xhtml+xml");
        Check.isTrue(x.createElementNS("http://www.w3.org/1999/xhtml", "INPUT") instanceof HtmlGenericElement, "upper case name in XML is generic");
        Check.isTrue(x.createElementNS("http://www.w3.org/1999/xhtml", "input") instanceof HtmlInputElement, "lower case name in XML");
    }

    /**
     * The value mode "value": the value attribute is the default until the dirty value flag is set by the IDL attribute or by the user.
     * <p>
     * value 모드 "value": IDL 속성이나 사용자가 dirty value flag 를 켜기 전까지 value 속성이 기본값이다.
     */
    public void testValueAndDirtyFlag() {
        Document d = FormTestSupport.page("<input id=a value='x'><input id=b><textarea id=t>q</textarea>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        Check.equal("x", a.getValue(), "initial value is the attribute");
        a.setAttribute("value", "y");
        Check.equal("y", a.getValue(), "a clean value follows the attribute");
        a.setValue("z");
        Check.equal("z", a.getValue(), "set value");
        a.setAttribute("value", "w");
        Check.equal("z", a.getValue(), "a dirty value ignores the attribute");
        Check.equal("w", a.getDefaultValue(), "defaultValue reflects the attribute");
        a.removeAttribute("value");
        Check.equal("z", a.getValue(), "still dirty");
        HtmlInputElement b = FormTestSupport.input(d, "b");
        Check.equal("", b.getValue(), "no attribute, empty value");
        Check.equal("text", b.getType(), "default type");
        b.setValueFromUser("typed");
        Check.equal("typed", b.getValue(), "user edit");
        b.setAttribute("value", "ignored");
        Check.equal("typed", b.getValue(), "the user edit made it dirty");
    }

    /**
     * The value sanitization algorithm of the text states, url and email.
     * <p>
     * 텍스트 상태, url, email 의 값 정리 알고리즘.
     */
    public void testSanitizationOfTextStates() {
        Document d = FormTestSupport.page("<input id=t><input id=u type=url><input id=e type=email><input id=m type=email multiple><input id=p type=password>");
        HtmlInputElement t = FormTestSupport.input(d, "t");
        t.setValue("a\r\nb\nc\rd");
        Check.equal("abcd", t.getValue(), "newlines are stripped from a text value");
        t.setValue("  keep spaces  ");
        Check.equal("  keep spaces  ", t.getValue(), "text keeps leading and trailing spaces");
        HtmlInputElement u = FormTestSupport.input(d, "u");
        u.setValue("  http://x.test/ \n");
        Check.equal("http://x.test/", u.getValue(), "url is stripped");
        HtmlInputElement e = FormTestSupport.input(d, "e");
        e.setValue(" a@b.test ");
        Check.equal("a@b.test", e.getValue(), "email is stripped");
        HtmlInputElement m = FormTestSupport.input(d, "m");
        m.setValue(" a@b.test , c@d.test ,e@f.test");
        Check.equal("a@b.test,c@d.test,e@f.test", m.getValue(), "multiple emails are stripped one by one");
        HtmlInputElement p = FormTestSupport.input(d, "p");
        p.setValue("pw\nx");
        Check.equal("pwx", p.getValue(), "password");
    }

    /**
     * The value sanitization algorithm of number, color, date and the other numeric states: an invalid string becomes the empty string
     * (color: black).
     * <p>
     * number, color, date 와 그 밖의 수치 상태의 값 정리 알고리즘: 올바르지 않은 문자열은 빈 문자열이 된다(color 는 검정).
     */
    public void testSanitizationOfNumericStates() {
        Document d = FormTestSupport.page("<input id=n type=number><input id=c type=color><input id=dt type=date><input id=mo type=month><input id=w type=week>"
                + "<input id=tm type=time><input id=l type=datetime-local>");
        HtmlInputElement n = FormTestSupport.input(d, "n");
        String[] valid = {"5", "-5", "5.5", ".5", "1e3", "1E-3", "-0.5e+2", "0"};
        // 올바른 부동 소수점 수는 그대로 값이 된다.
        for (int i = 0; i < valid.length; i++) {
            n.setValue(valid[i]);
            Check.equal(valid[i], n.getValue(), "valid number " + valid[i]);
        }
        String[] invalid = {"abc", "5.", "+5", " 5", "5 ", "1e", "--1", "1,5", "0x10", "NaN", "Infinity"};
        // 올바르지 않은 문자열은 빈 문자열이 된다.
        for (int i = 0; i < invalid.length; i++) {
            n.setValue(invalid[i]);
            Check.equal("", n.getValue(), "invalid number '" + invalid[i] + "'");
        }
        HtmlInputElement c = FormTestSupport.input(d, "c");
        Check.equal("#000000", c.getValue(), "default color");
        c.setValue("#ABCdef");
        Check.equal("#abcdef", c.getValue(), "color is lower-cased");
        c.setValue("red");
        Check.equal("#000000", c.getValue(), "a color name is not a simple color");
        c.setValue("#abc");
        Check.equal("#000000", c.getValue(), "a short color is not a simple color");
        HtmlInputElement dt = FormTestSupport.input(d, "dt");
        dt.setValue("2020-02-29");
        Check.equal("2020-02-29", dt.getValue(), "leap day");
        dt.setValue("2019-02-29");
        Check.equal("", dt.getValue(), "not a leap year");
        dt.setValue("2020-13-01");
        Check.equal("", dt.getValue(), "month 13");
        dt.setValue("2020-2-9");
        Check.equal("", dt.getValue(), "missing zeros");
        HtmlInputElement mo = FormTestSupport.input(d, "mo");
        mo.setValue("2021-07");
        Check.equal("2021-07", mo.getValue(), "month");
        mo.setValue("2021-00");
        Check.equal("", mo.getValue(), "month 0");
        HtmlInputElement w = FormTestSupport.input(d, "w");
        w.setValue("2020-W53");
        Check.equal("2020-W53", w.getValue(), "2020 has 53 weeks");
        w.setValue("2021-W53");
        Check.equal("", w.getValue(), "2021 has 52 weeks");
        HtmlInputElement tm = FormTestSupport.input(d, "tm");
        tm.setValue("23:59");
        Check.equal("23:59", tm.getValue(), "time");
        tm.setValue("23:59:59.123");
        Check.equal("23:59:59.123", tm.getValue(), "time with milliseconds");
        tm.setValue("24:00");
        Check.equal("", tm.getValue(), "hour 24");
        HtmlInputElement l = FormTestSupport.input(d, "l");
        l.setValue("2020-02-29T12:30");
        Check.equal("2020-02-29T12:30", l.getValue(), "local date and time");
        l.setValue("2020-02-29 12:30");
        Check.equal("2020-02-29T12:30", l.getValue(), "a space separator is normalized to T");
        l.setValue("2020-02-29T12:30:00");
        Check.equal("2020-02-29T12:30", l.getValue(), "zero seconds are dropped by normalization");
    }

    /**
     * The range state: the value is clamped to the range and moved to the nearest step; without a value it is the middle of the range.
     * <p>
     * range 상태: 값은 범위로 제한되고 가장 가까운 step 으로 옮겨진다. 값이 없으면 범위의 중간이다.
     */
    public void testRangeSanitization() {
        Document d = FormTestSupport.page("<input id=r type=range><input id=r2 type=range min=0 max=10 step=3><input id=r3 type=range min=10 max=0>");
        HtmlInputElement r = FormTestSupport.input(d, "r");
        Check.equal("50", r.getValue(), "default range value is 50");
        r.setValue("150");
        Check.equal("100", r.getValue(), "clamped to the maximum");
        r.setValue("-3");
        Check.equal("0", r.getValue(), "clamped to the minimum");
        r.setValue("2.4");
        Check.equal("2", r.getValue(), "moved to the nearest integer step");
        r.setValue("junk");
        Check.equal("50", r.getValue(), "junk becomes the default");
        HtmlInputElement r2 = FormTestSupport.input(d, "r2");
        Check.equal("6", r2.getValue(), "the middle of 0..10 is 5, which is rounded to the nearest step (0, 3, 6, 9): 6");
        r2.setValue("100");
        Check.equal("9", r2.getValue(), "the largest step not above the maximum");
        HtmlInputElement r3 = FormTestSupport.input(d, "r3");
        Check.equal("10", r3.getValue(), "a reversed range uses the minimum");
    }

    /**
     * <code>valueAsNumber</code>, <code>valueAsDate</code>, <code>stepUp</code> and <code>stepDown</code> of the numeric and date states.
     * <p>
     * 수치와 날짜 상태의 <code>valueAsNumber</code>, <code>valueAsDate</code>, <code>stepUp</code>, <code>stepDown</code>.
     */
    public void testNumericApi() {
        Document d = FormTestSupport.page("<input id=n type=number value=5 min=0 max=10 step=2><input id=dt type=date value='1970-01-02'>"
                + "<input id=t type=text value=5><input id=tm type=time value='01:00'><input id=mo type=month value='1970-03'>");
        HtmlInputElement n = FormTestSupport.input(d, "n");
        Check.near(5.0, n.getValueAsNumber(), 0, "valueAsNumber");
        n.setValueAsNumber(8);
        Check.equal("8", n.getValue(), "set valueAsNumber");
        n.setValueAsNumber(Double.NaN);
        Check.equal("", n.getValue(), "NaN clears");
        Check.isTrue(Double.isNaN(n.getValueAsNumber()), "empty value is NaN");
        n.setValue("4");
        n.stepUp(1);
        Check.equal("6", n.getValue(), "stepUp from 4 by step 2 with base 5? the step base is min=0 so 4 is aligned: 6");
        n.stepUp(10);
        Check.equal("10", n.getValue(), "stepUp stops at the maximum");
        n.stepDown(2);
        Check.equal("6", n.getValue(), "stepDown by two steps");
        n.setValue("5");
        n.stepUp(1);
        Check.equal("6", n.getValue(), "a misaligned value goes to the next step");
        HtmlInputElement dt = FormTestSupport.input(d, "dt");
        Check.near(86400000.0, dt.getValueAsNumber(), 0, "date as number is in milliseconds");
        Check.equal(Double.valueOf(86400000.0), dt.getValueAsDateMillis(), "valueAsDate");
        dt.setValueAsDateMillis(Double.valueOf(3 * 86400000.0 + 5));
        Check.equal("1970-01-04", dt.getValue(), "set valueAsDate truncates to the day");
        dt.stepUp(1);
        Check.equal("1970-01-05", dt.getValue(), "a date steps by days");
        HtmlInputElement tm = FormTestSupport.input(d, "tm");
        Check.near(3600000.0, tm.getValueAsNumber(), 0, "time as number");
        tm.stepUp(1);
        Check.equal("01:01", tm.getValue(), "a time steps by minutes");
        HtmlInputElement mo = FormTestSupport.input(d, "mo");
        Check.near(2.0, mo.getValueAsNumber(), 0, "month as number counts months since 1970-01");
        final HtmlInputElement t = FormTestSupport.input(d, "t");
        Check.isTrue(Double.isNaN(t.getValueAsNumber()), "valueAsNumber does not apply to text");
        Check.isNull(t.getValueAsDateMillis(), "valueAsDate does not apply to text");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Calls stepUp on a text input.
             * <p>
             * text input 에 stepUp 을 부른다.
             */
            public void run() {
                t.stepUp(1);
            }
        }, "stepUp does not apply to text");
    }

    /**
     * Checkbox and radio value mode "default/on", the dirty checkedness flag and the reset algorithm.
     * <p>
     * 체크박스와 라디오의 value 모드 "default/on", dirty checkedness flag, 초기화 알고리즘.
     */
    public void testCheckedness() {
        Document d = FormTestSupport.page("<form id=f><input id=c type=checkbox checked><input id=c2 type=checkbox value=yes><input id=r type=radio></form>");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        Check.isTrue(c.isChecked(), "checked attribute checks it");
        Check.equal("on", c.getValue(), "default value of a checkbox is on");
        Check.equal("yes", FormTestSupport.input(d, "c2").getValue(), "value attribute");
        Check.equal("on", FormTestSupport.input(d, "r").getValue(), "default value of a radio is on");
        c.removeAttribute("checked");
        Check.isFalse(c.isChecked(), "a clean checkedness follows the attribute");
        c.setAttribute("checked", "");
        Check.isTrue(c.isChecked(), "and follows it again");
        c.setChecked(false);
        c.setAttribute("checked", "");
        Check.isFalse(c.isChecked(), "a dirty checkedness ignores the attribute");
        Check.isTrue(c.isDefaultChecked(), "defaultChecked reflects the attribute");
        FormTestSupport.form(d, "f").reset();
        Check.isTrue(c.isChecked(), "reset restores the checked attribute");
        c.setChecked(false);
        c.setDefaultChecked(false);
        c.setDefaultChecked(true);
        Check.isFalse(c.isChecked(), "still dirty");
        c.setIndeterminate(true);
        Check.isTrue(c.isIndeterminate(), "indeterminate");
        c.setValue("custom");
        Check.equal("custom", c.getAttribute("value"), "setValue in default/on mode sets the attribute");
        Check.equal("custom", c.getValue(), "and reads it back");
    }

    /**
     * Radio button groups: same name (compatibility caseless), same form owner, same tree; checking one unchecks the others.
     * <p>
     * 라디오 버튼 그룹: 같은 이름(compatibility caseless), 같은 폼 소유자, 같은 트리. 하나를 체크하면 나머지가 해제된다.
     */
    public void testRadioGroups() {
        Document d = FormTestSupport.page("<form id=f1><input id=a type=radio name=g checked><input id=b type=radio name=g><input id=c type=radio name=h></form>"
                + "<form id=f2><input id=d type=radio name=g checked></form>"
                + "<input id=e type=radio name=g checked><input id=f type=radio name=G><input id=n1 type=radio checked><input id=n2 type=radio checked>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        HtmlInputElement b = FormTestSupport.input(d, "b");
        HtmlInputElement dd = FormTestSupport.input(d, "d");
        HtmlInputElement e = FormTestSupport.input(d, "e");
        HtmlInputElement f = FormTestSupport.input(d, "f");
        Check.isTrue(a.isChecked() && dd.isChecked() && e.isChecked(), "different forms and no form are different groups");
        Check.isTrue(FormTestSupport.input(d, "n1").isChecked() && FormTestSupport.input(d, "n2").isChecked(), "radios without a name are not grouped");
        b.setChecked(true);
        Check.isTrue(b.isChecked(), "b is checked");
        Check.isFalse(a.isChecked(), "a was unchecked");
        Check.isTrue(dd.isChecked(), "the other form is not affected");
        Check.isTrue(e.isChecked(), "e is still checked: another form owner and no other checked radio in its group yet");
        f.setChecked(true);
        Check.isFalse(e.isChecked(), "name=G is in the group of name=g (compatibility caseless) of the form owner null");
        Check.isTrue(f.isChecked(), "f is checked");
        a.setAttribute("name", "h");
        FormTestSupport.input(d, "c").setChecked(true);
        Check.isFalse(a.isChecked(), "a moved to the group h and was unchecked by c");
        Check.isTrue(b.isChecked(), "b keeps its checkedness");
        a.setChecked(true);
        Check.isFalse(FormTestSupport.input(d, "c").isChecked(), "checking a unchecks c");
    }

    /**
     * A checked radio button that is inserted into a document unchecks the others of its group; its form owner follows the tree.
     * <p>
     * 문서에 삽입된 체크된 라디오 버튼은 그룹의 다른 버튼을 해제한다. 폼 소유자는 트리를 따른다.
     */
    public void testRadioInsertion() {
        Document d = FormTestSupport.page("<form id=f><input id=a type=radio name=g checked></form>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        HtmlInputElement n = (HtmlInputElement) d.createElement("input");
        n.setAttribute("type", "radio");
        n.setAttribute("name", "g");
        n.setAttribute("checked", "");
        Check.isTrue(n.isChecked(), "a new checked radio is checked");
        FormTestSupport.form(d, "f").appendChild(n);
        Check.isTrue(n.isChecked(), "the inserted radio stays checked");
        Check.isFalse(a.isChecked(), "and the radio of the group was unchecked");
        Check.same(FormTestSupport.form(d, "f"), n.getForm(), "form owner is the ancestor");
    }

    /**
     * The form owner: the nearest form ancestor, the <code>form</code> attribute (by id, only when connected), and the owner that the parser assigned
     * to elements that the tree builder moved out of the form.
     * <p>
     * 폼 소유자: 가장 가까운 form 조상, <code>form</code> 속성(id, 연결되어 있을 때만), 트리 빌더가 폼 밖으로 옮긴 요소에 파서가 지정한 소유자.
     */
    public void testFormOwner() {
        Document d = FormTestSupport.page("<form id=f1><input id=a><div><input id=b></div></form><form id=f2></form><input id=c form=f2><input id=d form=nothing>"
                + "<form id=f3><table><tr><input id=e></tr></table><input id=g></form>");
        Check.same(FormTestSupport.form(d, "f1"), FormTestSupport.input(d, "a").getForm(), "child");
        Check.same(FormTestSupport.form(d, "f1"), FormTestSupport.input(d, "b").getForm(), "descendant");
        Check.same(FormTestSupport.form(d, "f2"), FormTestSupport.input(d, "c").getForm(), "form attribute");
        Check.isNull(FormTestSupport.input(d, "d").getForm(), "form attribute naming nothing");
        // 파서가 foster parenting 으로 테이블 밖에 둔 요소도 폼 소유자는 f3 이다.
        Check.same(FormTestSupport.form(d, "f3"), FormTestSupport.input(d, "e").getForm(), "foster parented input keeps the form owner");
        Check.same(FormTestSupport.form(d, "f3"), FormTestSupport.input(d, "g").getForm(), "after the table");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        a.setAttribute("form", "f2");
        Check.same(FormTestSupport.form(d, "f2"), a.getForm(), "the attribute wins over the ancestor");
        a.setAttribute("form", "f2 f1");
        Check.isNull(a.getForm(), "ids are matched as a whole");
        HtmlInputElement detached = (HtmlInputElement) d.createElement("input");
        detached.setAttribute("form", "f1");
        Check.isNull(detached.getForm(), "a disconnected element has no form by attribute");
    }

    /**
     * <code>form.elements</code>: the listed elements of the form in tree order (no image buttons), live, with named access and radio node lists.
     * <p>
     * <code>form.elements</code>: 폼의 목록 요소를 트리 순서로(이미지 버튼 제외), 라이브로, 이름 접근과 라디오 노드 목록과 함께.
     */
    public void testFormElements() {
        Document d = FormTestSupport.page("<form id=f><input id=a name=n type=text><input id=b name=n type=radio value=1 checked><input id=c name=n type=radio value=2>"
                + "<input id=im type=image name=pic><select id=s name=s></select><textarea id=t></textarea><button id=bt>x</button><fieldset id=fs></fieldset>"
                + "<output id=o></output><input id=h type=hidden name=hh><div><input id=in2></div></form><input id=out form=f>");
        HtmlFormElement f = FormTestSupport.form(d, "f");
        FormControlsCollection els = f.getElements();
        String ids = "";
        // 컬렉션을 앞에서부터 읽어 id 를 이어 붙인다.
        for (int i = 0; i < els.getLength(); i++) {
            ids += els.item(i).getId() + ",";
        }
        Check.equal("a,b,c,s,t,bt,fs,o,h,in2,out,", ids, "listed elements except the image button, in tree order");
        Check.equal(els.getLength(), f.getLength(), "form.length");
        Check.same(els, f.getElements(), "the same collection every time");
        Object named = els.namedItem("n");
        Check.isTrue(named instanceof RadioNodeList, "several controls with one name give a RadioNodeList");
        Check.equal("1", ((RadioNodeList) named).getValue(), "the value of the checked radio");
        ((RadioNodeList) named).setValue("2");
        Check.isTrue(FormTestSupport.input(d, "c").isChecked(), "setting the value checks the radio with that value");
        Check.same(d.getElementById("s"), els.namedItem("s"), "a single control by name");
        Check.same(d.getElementById("s"), els.namedItem("s"), "and by id");
        Check.isNull(els.namedItem("pic"), "image buttons are not in elements");
        Object byForm = f.getNamedProperty("pic");
        Check.same(d.getElementById("im"), byForm, "but form[name] finds the image button");
        int before = els.getLength();
        f.appendChild(d.createElement("input"));
        Check.equal(before + 1, els.getLength(), "the collection is live");
    }

    /**
     * Labels: the <code>for</code> attribute, the first labelable descendant, <code>labels</code> lists, labelable elements.
     * <p>
     * label: <code>for</code> 속성, 첫 labelable 후손, <code>labels</code> 목록, labelable 요소.
     */
    public void testLabels() {
        Document d = FormTestSupport.page("<label id=l1 for=a>1</label><input id=a><label id=l2><span>x</span><input id=b><input id=c></label>"
                + "<label id=l3 for=h>3</label><input id=h type=hidden><label id=l4 for=nothing>4</label><label id=l5 for=div>5</label><div id=div></div>"
                + "<label id=l6 for=a>6</label><label id=l7><div><button id=bt>b</button></div></label>");
        Check.same(d.getElementById("a"), ((HtmlLabelElement) d.getElementById("l1")).getControl(), "for attribute");
        Check.same(d.getElementById("b"), ((HtmlLabelElement) d.getElementById("l2")).getControl(), "first labelable descendant");
        Check.isNull(((HtmlLabelElement) d.getElementById("l3")).getControl(), "a hidden input is not labelable");
        Check.isNull(((HtmlLabelElement) d.getElementById("l4")).getControl(), "for names nothing");
        Check.isNull(((HtmlLabelElement) d.getElementById("l5")).getControl(), "a div is not labelable");
        Check.same(d.getElementById("bt"), ((HtmlLabelElement) d.getElementById("l7")).getControl(), "a button is labelable");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        FormElementList labels = a.getLabels();
        Check.equal(2, labels.getLength(), "labels of a");
        Check.same(d.getElementById("l1"), labels.item(0), "first label in tree order");
        Check.same(d.getElementById("l6"), labels.item(1), "second label");
        Check.equal(1, FormTestSupport.input(d, "b").getLabels().getLength(), "b has the label l2");
        Check.equal(0, FormTestSupport.input(d, "c").getLabels().getLength(), "c is not the labeled control of l2");
        Check.isNull(FormTestSupport.input(d, "h").getLabels(), "hidden inputs have no labels list");
        d.getElementById("l6").remove();
        Check.equal(1, labels.getLength(), "labels is live");
    }

    /**
     * A disabled fieldset disables its descendants except those inside its first legend child; disabled affects the form control state.
     * <p>
     * 비활성 fieldset 은 첫 legend 자식 안의 것을 뺀 후손을 비활성으로 만든다.
     */
    public void testDisabledFieldset() {
        Document d = FormTestSupport.page("<fieldset id=fs disabled><legend><input id=a></legend><legend><input id=b></legend><input id=c><div><select id=s></select></div>"
                + "<fieldset><input id=n></fieldset></fieldset><input id=out><button id=bt disabled></button><fieldset id=fs2 disabled><fieldset><legend><input id=m></legend></fieldset></fieldset>");
        Check.isFalse(FormTestSupport.input(d, "a").isDisabled(), "inside the first legend");
        Check.isTrue(FormTestSupport.input(d, "b").isDisabled(), "inside the second legend");
        Check.isTrue(FormTestSupport.input(d, "c").isDisabled(), "a plain descendant");
        Check.isTrue(FormTestSupport.select(d, "s").isDisabled(), "deep descendant");
        Check.isTrue(FormTestSupport.input(d, "n").isDisabled(), "nested fieldset");
        Check.isFalse(FormTestSupport.input(d, "out").isDisabled(), "outside");
        Check.isTrue(((HtmlButtonElement) d.getElementById("bt")).isDisabled(), "disabled attribute");
        Check.isTrue(FormTestSupport.input(d, "m").isDisabled(), "a legend of an inner fieldset is not the first legend of the disabled one");
        d.getElementById("fs").removeAttribute("disabled");
        Check.isFalse(FormTestSupport.input(d, "c").isDisabled(), "enabling the fieldset enables its controls");
        Check.same(d.getElementById("c"), ((HtmlFieldSetElement) d.getElementById("fs")).getElements().item(2), "fieldset.elements contains the descendants in tree order");
    }

    /**
     * <code>textarea</code>: the default value is the child text (the parser drops a first newline), the value normalizes newlines, a dirty value
     * ignores later changes of the child text, reset restores it.
     * <p>
     * <code>textarea</code>: 기본값은 자식 텍스트(파서가 첫 줄 바꿈을 버린다), 값은 줄 바꿈을 정규화하고, dirty 값은 자식 텍스트의 이후 변경을 무시하며, 초기화가 되살린다.
     */
    public void testTextarea() {
        Document d = FormTestSupport.page("<form id=f><textarea id=a>\nfirst\nsecond</textarea><textarea id=b>x</textarea></form>");
        HtmlTextAreaElement a = FormTestSupport.textarea(d, "a");
        Check.equal("first\nsecond", a.getValue(), "the first newline was dropped by the parser");
        Check.equal("first\nsecond", a.getDefaultValue(), "default value is the child text");
        a.setValue("p\r\nq\rr");
        Check.equal("p\nq\nr", a.getValue(), "the API value uses LF");
        Check.equal("p\r\nq\rr", a.getRawValue(), "the raw value is kept as it was set");
        a.setDefaultValue("changed");
        Check.equal("p\nq\nr", a.getValue(), "dirty: the child text does not change the value");
        Check.equal("changed", a.getTextContent(), "defaultValue is the child text");
        FormTestSupport.form(d, "f").reset();
        Check.equal("changed", a.getValue(), "reset restores the default value");
        HtmlTextAreaElement b = FormTestSupport.textarea(d, "b");
        b.setDefaultValue("y");
        Check.equal("y", b.getValue(), "a clean textarea follows the child text");
        Check.equal("textarea", b.getType(), "type");
        Check.equal(1, b.getTextLength(), "textLength");
    }

    /**
     * <code>output</code>: the value is the text content; the default value is restored by reset.
     * <p>
     * <code>output</code>: 값은 텍스트 콘텐츠이고 기본값은 초기화로 되살린다.
     */
    public void testOutput() {
        Document d = FormTestSupport.page("<form id=f><output id=o name=res for='a b'>7</output></form>");
        HtmlOutputElement o = (HtmlOutputElement) d.getElementById("o");
        Check.equal("7", o.getValue(), "value is the text content");
        Check.equal("7", o.getDefaultValue(), "default value");
        o.setValue("9");
        Check.equal("9", o.getValue(), "set value");
        Check.equal("9", o.getTextContent(), "the text content changed");
        o.setDefaultValue("8");
        Check.equal("9", o.getValue(), "the value was set so the default does not change it");
        FormTestSupport.form(d, "f").reset();
        Check.equal("8", o.getValue(), "reset restores the default value");
        Check.equal("output", o.getType(), "type");
        Check.equal("a b", o.getHtmlFor(), "htmlFor");
    }

    /**
     * Cloning copies the value, the dirty flags and the checkedness ("cloning steps").
     * <p>
     * 복제는 값, dirty 플래그, checkedness 를 복사한다("cloning steps").
     */
    public void testCloning() {
        Document d = FormTestSupport.page("<input id=a value=x><input id=c type=checkbox><textarea id=t>q</textarea>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        a.setValue("typed");
        HtmlInputElement a2 = (HtmlInputElement) a.cloneNode(true);
        Check.equal("typed", a2.getValue(), "the value is cloned");
        a2.setAttribute("value", "other");
        Check.equal("typed", a2.getValue(), "the dirty flag is cloned");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        c.setChecked(true);
        HtmlInputElement c2 = (HtmlInputElement) c.cloneNode(false);
        Check.isTrue(c2.isChecked(), "checkedness is cloned");
        HtmlTextAreaElement t = FormTestSupport.textarea(d, "t");
        t.setValue("edited");
        HtmlTextAreaElement t2 = (HtmlTextAreaElement) t.cloneNode(true);
        Check.equal("edited", t2.getValue(), "the textarea value is cloned");
        Check.equal("q", t2.getDefaultValue(), "the default value is the child text");
    }

    /**
     * Changing the <code>type</code> attribute moves between value modes and sanitizes the value.
     * <p>
     * <code>type</code> 속성을 바꾸면 value 모드가 바뀌고 값이 정리된다.
     */
    public void testTypeChange() {
        Document d = FormTestSupport.page("<input id=a value='12x'>");
        HtmlInputElement a = FormTestSupport.input(d, "a");
        Check.equal("12x", a.getValue(), "text");
        a.setAttribute("type", "number");
        Check.equal("", a.getValue(), "number sanitizes the invalid value");
        a.setAttribute("type", "bogus");
        Check.equal("text", a.getType(), "an unknown type is text");
        a.setValue("typed");
        a.setAttribute("type", "hidden");
        Check.equal("typed", a.getValue(), "to the default mode: the value is stored in the attribute");
        Check.equal("typed", a.getAttribute("value"), "attribute");
        a.setAttribute("type", "TEXT");
        Check.equal("text", a.getType(), "type is case-insensitive");
        a.setAttribute("type", "file");
        Check.equal("", a.getValue(), "file mode");
        a.setAttribute("type", "checkbox");
        Check.equal("typed", a.getValue(), "back to default/on");
    }

    /**
     * A document that is traversed and modified keeps the listed elements consistent when nodes move between forms.
     * <p>
     * 노드가 폼 사이를 옮겨 다녀도 목록 요소가 일관된다.
     */
    public void testMovingBetweenForms() {
        Document d = FormTestSupport.page("<form id=f1><input id=a name=x></form><form id=f2></form>");
        HtmlFormElement f1 = FormTestSupport.form(d, "f1");
        HtmlFormElement f2 = FormTestSupport.form(d, "f2");
        Element a = d.getElementById("a");
        Check.equal(1, f1.getElements().getLength(), "f1 has a");
        Node moved = f2.appendChild(a);
        Check.same(a, moved, "appendChild returns the node");
        Check.equal(0, f1.getElements().getLength(), "f1 lost it");
        Check.equal(1, f2.getElements().getLength(), "f2 has it");
        Check.same(f2, ((HtmlInputElement) a).getForm(), "form owner changed");
    }
}
