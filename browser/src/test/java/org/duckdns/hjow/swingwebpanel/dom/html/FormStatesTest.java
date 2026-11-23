package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.NodeList;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the form related pseudo-classes that follow the live state of the controls (plan items P11-05 and P11-07, HTML Standard 4.16.3):
 * <code>:checked</code>, <code>:disabled</code>, <code>:enabled</code>, <code>:indeterminate</code>, <code>:default</code>, <code>:required</code>,
 * <code>:optional</code>, <code>:read-write</code>, <code>:read-only</code>, <code>:placeholder-shown</code>, <code>:valid</code>, <code>:invalid</code> and,
 * through {@link FormStates}, <code>:in-range</code> and <code>:out-of-range</code>. The selector engine of the document is the real one.
 * <p>
 * 컨트롤의 실제 상태를 따르는 폼 관련 의사 클래스 시험(계획 P11-05, P11-07, HTML 표준 4.16.3): <code>:checked</code>, <code>:disabled</code>, <code>:enabled</code>,
 * <code>:indeterminate</code>, <code>:default</code>, <code>:required</code>, <code>:optional</code>, <code>:read-write</code>, <code>:read-only</code>,
 * <code>:placeholder-shown</code>, <code>:valid</code>, <code>:invalid</code>, 그리고 {@link FormStates} 를 통한 <code>:in-range</code>, <code>:out-of-range</code>.
 * 문서의 선택자 엔진은 실제 엔진이다.
 */
public class FormStatesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormStatesTest() {
    }

    /**
     * The test page.
     * <p>
     * 시험 페이지.
     */
    private static final String PAGE = "<form id=f><input id=a type=checkbox checked><input id=b type=checkbox><input id=r1 type=radio name=g><input id=r2 type=radio name=g>"
            + "<input id=t required><input id=u><textarea id=ta required></textarea><input id=ro readonly><input id=dis disabled>"
            + "<select id=s><option id=o1>1</option><option id=o2 selected>2</option></select>"
            + "<input id=n type=number min=1 max=5 value=9><input id=n2 type=number min=1 max=5 value=3><input id=n3 type=number>"
            + "<input id=ph placeholder=x><input id=sub type=submit><input type=submit id=sub2><progress id=p></progress><progress id=p2 value=1></progress>"
            + "<fieldset id=fs disabled><input id=infs></fieldset><input id=e type=email value=bad></form>";

    /**
     * Returns the ids of the elements that match a selector, separated by commas.
     * <p>
     * 선택자와 일치하는 요소의 id 를 쉼표로 구분해 반환한다.
     *
     * @param d        the document / 문서
     * @param selector the selector / 선택자
     * @return the ids / id
     */
    private static String ids(Document d, String selector) {
        NodeList l = d.querySelectorAll(selector);
        StringBuilder b = new StringBuilder();
        // 일치한 요소의 id 를 이어 붙인다.
        for (int i = 0; i < l.getLength(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(((Element) l.item(i)).getId());
        }
        return b.toString();
    }

    /**
     * Creates the page with the selector engine.
     * <p>
     * 선택자 엔진이 있는 페이지를 만든다.
     *
     * @return the document / 문서
     */
    private static Document page() {
        Document d = FormTestSupport.parse("<!DOCTYPE html><html><body>" + PAGE + "</body></html>");
        CssSelectorEngine.install(d);
        return d;
    }

    /**
     * <code>:checked</code>, <code>:indeterminate</code> and <code>:default</code> follow checkedness, indeterminateness and selectedness, not attributes.
     * <p>
     * <code>:checked</code>, <code>:indeterminate</code>, <code>:default</code> 는 속성이 아니라 checkedness, indeterminateness, selectedness 를 따른다.
     */
    public void testCheckedIndeterminateDefault() {
        Document d = page();
        Check.equal("a,o2", ids(d, ":checked"), "initial :checked");
        FormTestSupport.input(d, "b").setChecked(true);
        FormTestSupport.input(d, "a").setChecked(false);
        FormTestSupport.select(d, "s").setSelectedIndex(0);
        Check.equal("b,o1", ids(d, ":checked"), "after script changes");
        Check.equal("a,o2,sub", ids(d, ":default"), ":default is independent of the live state: the attributes and the default button");
        Check.equal("r1,r2,p", ids(d, ":indeterminate"), "a radio group without a checked radio, a progress without value");
        FormTestSupport.input(d, "r1").setChecked(true);
        FormTestSupport.input(d, "b").setIndeterminate(true);
        Check.equal("b,p", ids(d, ":indeterminate"), "a checked radio group is not indeterminate; the checkbox with indeterminateness is");
    }

    /**
     * <code>:disabled</code> and <code>:enabled</code> include the effect of a disabled fieldset.
     * <p>
     * <code>:disabled</code>, <code>:enabled</code> 는 비활성 fieldset 의 영향을 포함한다.
     */
    public void testDisabledEnabled() {
        Document d = page();
        Check.equal("dis,fs,infs", ids(d, ":disabled"), "disabled");
        Check.isTrue(ids(d, ":enabled").indexOf("dis") < 0 && ids(d, ":enabled").indexOf("infs") < 0, "not enabled: " + ids(d, ":enabled"));
        Check.isTrue(("," + ids(d, ":enabled") + ",").indexOf(",t,") >= 0 && ("," + ids(d, ":enabled") + ",").indexOf(",s,") >= 0
                && ("," + ids(d, ":enabled") + ",").indexOf(",o1,") >= 0, "enabled: " + ids(d, ":enabled"));
        d.getElementById("fs").removeAttribute("disabled");
        Check.equal("dis", ids(d, ":disabled"), "after enabling the fieldset");
        Check.equal("", ids(d, "p:enabled, p:disabled"), "a progress element is neither");
    }

    /**
     * <code>:required</code>, <code>:optional</code>, <code>:read-write</code> and <code>:read-only</code> follow the kind and the state of the controls.
     * <p>
     * <code>:required</code>, <code>:optional</code>, <code>:read-write</code>, <code>:read-only</code> 은 컨트롤의 종류와 상태를 따른다.
     */
    public void testRequiredAndReadWrite() {
        Document d = page();
        Check.equal("t,ta", ids(d, ":required"), "required");
        Check.equal("a,b,r1,r2,u,ro,dis,s,n,n2,n3,ph,infs,e", ids(d, ":optional"), "optional: the controls to which required applies and that do not have it");
        String rw = "," + ids(d, ":read-write") + ",";
        Check.isTrue(rw.indexOf(",t,") >= 0 && rw.indexOf(",u,") >= 0 && rw.indexOf(",ta,") >= 0 && rw.indexOf(",n,") >= 0 && rw.indexOf(",e,") >= 0, "text controls are read-write: " + rw);
        Check.isTrue(rw.indexOf(",ro,") < 0 && rw.indexOf(",dis,") < 0 && rw.indexOf(",a,") < 0 && rw.indexOf(",s,") < 0, "readonly, disabled, checkbox, select are not: " + rw);
        String ro = "," + ids(d, ":read-only") + ",";
        Check.isTrue(ro.indexOf(",ro,") >= 0 && ro.indexOf(",dis,") >= 0 && ro.indexOf(",a,") >= 0 && ro.indexOf(",t,") < 0, ":read-only is the negation: " + ro);
        d.getElementById("ro").removeAttribute("readonly");
        Check.isTrue(("," + ids(d, ":read-write") + ",").indexOf(",ro,") >= 0, "readonly removed");
    }

    /**
     * <code>:placeholder-shown</code> follows the value, not the attribute.
     * <p>
     * <code>:placeholder-shown</code> 은 속성이 아니라 값을 따른다.
     */
    public void testPlaceholderShown() {
        Document d = page();
        Check.equal("ph", ids(d, ":placeholder-shown"), "empty value");
        FormTestSupport.input(d, "ph").setValue("typed");
        Check.equal("", ids(d, ":placeholder-shown"), "with a value");
        FormTestSupport.input(d, "ph").setValue("");
        Check.equal("ph", ids(d, ":placeholder-shown"), "emptied by script");
    }

    /**
     * <code>:valid</code> and <code>:invalid</code> follow the validity of the live state; forms and fieldsets aggregate; barred controls are neither.
     * <p>
     * <code>:valid</code>, <code>:invalid</code> 는 실제 상태의 유효성을 따른다. form 과 fieldset 은 합산하고, 제외된 컨트롤은 둘 다 아니다.
     */
    public void testValidInvalid() {
        Document d = page();
        String invalid = "," + ids(d, ":invalid") + ",";
        Check.isTrue(invalid.indexOf(",t,") >= 0 && invalid.indexOf(",ta,") >= 0 && invalid.indexOf(",n,") >= 0 && invalid.indexOf(",e,") >= 0 && invalid.indexOf(",f,") >= 0,
                "invalid controls and the form: " + invalid);
        Check.isTrue(invalid.indexOf(",u,") < 0 && invalid.indexOf(",n2,") < 0 && invalid.indexOf(",ro,") < 0 && invalid.indexOf(",fs,") < 0, "valid ones are not invalid: " + invalid);
        String valid = "," + ids(d, ":valid") + ",";
        Check.isTrue(valid.indexOf(",u,") >= 0 && valid.indexOf(",n2,") >= 0 && valid.indexOf(",fs,") >= 0 && valid.indexOf(",sub,") >= 0, "valid: " + valid);
        Check.isTrue(valid.indexOf(",ro,") < 0 && valid.indexOf(",dis,") < 0 && valid.indexOf(",p,") < 0, "barred controls and other elements are neither valid nor invalid: " + valid);
        FormTestSupport.input(d, "t").setValue("x");
        FormTestSupport.textarea(d, "ta").setValue("y");
        FormTestSupport.input(d, "n").setValue("2");
        FormTestSupport.input(d, "e").setValue("a@b.test");
        Check.equal("", ids(d, ":invalid"), "everything is valid now, also the form");
        Check.isTrue(("," + ids(d, ":valid") + ",").indexOf(",f,") >= 0, "the form is valid");
        FormTestSupport.input(d, "u").setCustomValidity("no");
        Check.equal("f,u", ids(d, ":invalid"), "a custom error makes the control and the form invalid");
    }

    /**
     * <code>:in-range</code> and <code>:out-of-range</code> (the answers of {@link FormStates}).
     * <p>
     * <code>:in-range</code>, <code>:out-of-range</code>({@link FormStates} 의 답).
     */
    public void testRange() {
        Document d = page();
        Check.equal(Boolean.TRUE, FormStates.outOfRange(d.getElementById("n")), "9 is above the maximum 5");
        Check.equal(Boolean.FALSE, FormStates.inRange(d.getElementById("n")), "so it is not in range");
        Check.equal(Boolean.TRUE, FormStates.inRange(d.getElementById("n2")), "3 is in range");
        Check.equal(Boolean.FALSE, FormStates.outOfRange(d.getElementById("n2")), "not out of range");
        Check.equal(Boolean.FALSE, FormStates.inRange(d.getElementById("n3")), "no range limitations: neither");
        Check.equal(Boolean.FALSE, FormStates.outOfRange(d.getElementById("n3")), "no range limitations: neither");
        Check.equal(Boolean.FALSE, FormStates.inRange(d.getElementById("t")), "text controls have no range");
        Check.isNull(FormStates.inRange(d.getDocumentElement()), "an element of another layer is not decided");
        FormTestSupport.input(d, "n").setValue("5");
        Check.equal(Boolean.TRUE, FormStates.inRange(d.getElementById("n")), "5 is the maximum: in range");
        FormTestSupport.input(d, "n").setValue("");
        Check.equal(Boolean.TRUE, FormStates.inRange(d.getElementById("n")), "an empty value is in range when there are limits");
    }
}
