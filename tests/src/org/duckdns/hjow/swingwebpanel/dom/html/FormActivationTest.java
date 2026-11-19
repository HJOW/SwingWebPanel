package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the activation behavior of clicks (plan items P11-05 to P11-07, DOM Standard 2.9 and HTML Standard 4.10): the legacy pre-activation of
 * checkboxes and radio buttons that listeners see and that a cancelled click undoes, the events that follow, labels, buttons that contain other
 * elements, disabled controls, synthetic clicks, file upload controls, image buttons, reset buttons and implicit submission.
 * <p>
 * click 의 활성화 동작 시험(계획 P11-05~P11-07, DOM 표준 2.9, HTML 표준 4.10): 리스너가 보고 취소된 click 이 되돌리는 체크박스와 라디오 버튼의 legacy
 * pre-activation, 이어지는 이벤트, label, 다른 요소를 품은 버튼, 비활성 컨트롤, 합성 click, 파일 업로드 컨트롤, 이미지 버튼, reset 버튼, 암묵적 제출.
 */
public class FormActivationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormActivationTest() {
    }

    /**
     * Creates a click mouse event (bubbling, cancelable).
     * <p>
     * click 마우스 이벤트(버블링, 취소 가능)를 만든다.
     *
     * @return the event / 이벤트
     */
    private static MouseEvent click() {
        return new MouseEvent("click", true, true);
    }

    /**
     * Dispatches a trusted click at the element with the id.
     * <p>
     * id 가 같은 요소에 신뢰된 click 을 전달한다.
     *
     * @param d  the document / 문서
     * @param id the id / id
     * @return false if the click was cancelled / click 이 취소되었으면 false
     */
    private static boolean trustedClick(Document d, String id) {
        return d.getElementById(id).dispatchTrustedEvent(click());
    }

    /**
     * Listens to the event types at the document (capture) and logs <code>type@id</code>.
     * <p>
     * 문서에서 이벤트 종류를 (캡처로) 듣고 <code>종류@id</code> 를 기록한다.
     *
     * @param d     the document / 문서
     * @param log   the log / 기록
     * @param types the event types / 이벤트 종류
     */
    private static void watch(Document d, List<String> log, String... types) {
        // 종류마다 문서에 기록용 리스너를 등록한다.
        for (int i = 0; i < types.length; i++) {
            d.addEventListener(types[i], FormTestSupport.logger(log), true);
        }
    }

    /**
     * A click on a checkbox toggles it before the listeners run, then <code>input</code> and <code>change</code> follow; a cancelled click restores it.
     * <p>
     * 체크박스를 click 하면 리스너가 실행되기 전에 토글되고 <code>input</code>, <code>change</code> 가 이어진다. 취소된 click 은 되돌린다.
     */
    public void testCheckboxActivation() {
        final Document d = FormTestSupport.page("<input id=c type=checkbox>");
        final HtmlInputElement c = FormTestSupport.input(d, "c");
        final List<String> log = new ArrayList<String>();
        watch(d, log, "click", "input", "change");
        final List<Boolean> seen = new ArrayList<Boolean>();
        c.addEventListener("click", new EventListener() {
            /**
             * Records the checkedness that the listener sees.
             * <p>
             * 리스너가 보는 checkedness 를 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                seen.add(Boolean.valueOf(c.isChecked()));
            }
        }, false);
        Check.isTrue(trustedClick(d, "c"), "not cancelled");
        Check.isTrue(c.isChecked(), "toggled");
        Check.equal(Arrays.asList(Boolean.TRUE), seen, "the listener already sees the new state");
        Check.equal(Arrays.asList("click@c", "input@c", "change@c"), log, "events in order");
        log.clear();
        trustedClick(d, "c");
        Check.isFalse(c.isChecked(), "toggled back");
        c.addEventListener("click", new EventListener() {
            /**
             * Cancels the click.
             * <p>
             * click 을 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                e.preventDefault();
            }
        }, false);
        log.clear();
        Check.isFalse(trustedClick(d, "c"), "cancelled");
        Check.isFalse(c.isChecked(), "a cancelled click restores the state");
        Check.equal(Arrays.asList("click@c"), log, "and no input or change event");
    }

    /**
     * A click on a checkbox clears the indeterminate state; a cancelled click restores it.
     * <p>
     * 체크박스를 click 하면 indeterminate 가 해제된다. 취소된 click 은 되살린다.
     */
    public void testIndeterminate() {
        Document d = FormTestSupport.page("<input id=c type=checkbox>");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        c.setIndeterminate(true);
        trustedClick(d, "c");
        Check.isFalse(c.isIndeterminate(), "cleared by the click");
        Check.isTrue(c.isChecked(), "checked");
        c.setIndeterminate(true);
        EventListener cancel = new EventListener() {
            /**
             * Cancels the click.
             * <p>
             * click 을 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                e.preventDefault();
            }
        };
        c.addEventListener("click", cancel, false);
        trustedClick(d, "c");
        Check.isTrue(c.isIndeterminate(), "restored by the cancelled click");
        Check.isTrue(c.isChecked(), "and the checkedness too");
    }

    /**
     * A click on a radio button checks it and unchecks the others of the group; <code>change</code> fires only if it was not checked; a cancelled click restores
     * the group.
     * <p>
     * 라디오 버튼을 click 하면 체크되고 그룹의 다른 버튼이 해제된다. <code>change</code> 는 이전에 체크되어 있지 않았을 때만 발생한다. 취소된 click 은 그룹을 되돌린다.
     */
    public void testRadioActivation() {
        Document d = FormTestSupport.page("<form><input id=a type=radio name=g checked><input id=b type=radio name=g><input id=c type=radio name=g></form>");
        List<String> log = new ArrayList<String>();
        watch(d, log, "change", "input");
        trustedClick(d, "b");
        Check.isTrue(FormTestSupport.input(d, "b").isChecked(), "b checked");
        Check.isFalse(FormTestSupport.input(d, "a").isChecked(), "a unchecked");
        Check.equal(Arrays.asList("input@b", "change@b"), log, "events");
        log.clear();
        trustedClick(d, "b");
        Check.equal(0, log.size(), "clicking the checked radio again fires nothing");
        d.getElementById("c").addEventListener("click", new EventListener() {
            /**
             * Cancels the click.
             * <p>
             * click 을 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                e.preventDefault();
            }
        }, false);
        trustedClick(d, "c");
        Check.isTrue(FormTestSupport.input(d, "b").isChecked(), "a cancelled click restores the previously checked radio");
        Check.isFalse(FormTestSupport.input(d, "c").isChecked(), "c is not checked");
    }

    /**
     * A click on a label clicks its labeled control; a click on the control inside the label does not toggle it twice; interactive content in a label
     * keeps its own behavior.
     * <p>
     * label 을 click 하면 labeled control 에 click 이 간다. label 안의 컨트롤을 click 해도 두 번 토글되지 않는다. label 안의 상호작용 콘텐츠는 자신의 동작을 유지한다.
     */
    public void testLabelActivation() {
        Document d = FormTestSupport.page("<label id=l1 for=c1>one <b id=bold>bold</b></label><input id=c1 type=checkbox>"
                + "<label id=l2>two <input id=c2 type=checkbox> <a id=link href=#x>link</a> <button id=bt type=button>b</button></label>"
                + "<label id=l3 for=dis>three</label><input id=dis type=checkbox disabled><label id=l4 for=missing>four</label>");
        List<String> log = new ArrayList<String>();
        watch(d, log, "click", "change");
        trustedClick(d, "l1");
        Check.isTrue(FormTestSupport.input(d, "c1").isChecked(), "the labeled control was clicked");
        Check.equal(Arrays.asList("click@l1", "click@c1", "change@c1"), log, "the label click is followed by a click at the control");
        log.clear();
        trustedClick(d, "bold");
        Check.isFalse(FormTestSupport.input(d, "c1").isChecked(), "a click on a child of the label activates the control (toggled back)");
        Check.equal(Arrays.asList("click@bold", "click@c1", "change@c1"), log, "events");
        log.clear();
        trustedClick(d, "c2");
        Check.isTrue(FormTestSupport.input(d, "c2").isChecked(), "a click on the control inside the label toggles it once");
        Check.equal(Arrays.asList("click@c2", "change@c2"), log, "no second click");
        log.clear();
        trustedClick(d, "l2");
        Check.isFalse(FormTestSupport.input(d, "c2").isChecked(), "a click on the label text toggles the first labelable descendant");
        log.clear();
        trustedClick(d, "link");
        trustedClick(d, "bt");
        Check.isFalse(FormTestSupport.input(d, "c2").isChecked(), "interactive content inside the label is not forwarded to the control");
        log.clear();
        trustedClick(d, "l3");
        Check.isFalse(FormTestSupport.input(d, "dis").isChecked(), "a disabled control does not toggle");
        Check.equal(Arrays.asList("click@l3"), log, "and gets no click");
        trustedClick(d, "l4");
    }

    /**
     * A click on an element inside a button reaches the activation behavior of the button; disabled controls get no click.
     * <p>
     * 버튼 안의 요소를 click 하면 버튼의 활성화 동작에 닿는다. 비활성 컨트롤은 click 을 받지 않는다.
     */
    public void testButtonActivation() {
        Document d = FormTestSupport.page("<form id=f action=/a><input name=x value=1><button id=b name=btn value=B><span id=inner>go</span></button>"
                + "<button id=dis name=d disabled><span id=in2>x</span></button><button id=plain type=button>p</button><button id=rs type=reset>r</button></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        List<String> log = new ArrayList<String>();
        watch(d, log, "click", "submit");
        trustedClick(d, "inner");
        Check.equal(1, host.submissions.size(), "a click on the span inside the button submits the form");
        Check.equal("http://example.test/a?x=1&btn=B", host.submissions.get(0).getUrl(), "with the button as the submitter");
        Check.equal(Arrays.asList("click@inner", "submit@f"), log, "click then submit");
        log.clear();
        Check.isTrue(trustedClick(d, "dis"), "a click at a disabled button is not dispatched");
        Check.equal(0, log.size(), "no listener ran");
        trustedClick(d, "in2");
        Check.equal(1, host.submissions.size(), "a disabled button does not submit, also not through its child");
        trustedClick(d, "plain");
        Check.equal(1, host.submissions.size(), "a plain button does nothing");
    }

    /**
     * Only mouse event clicks are activation events; a plain <code>Event</code> named click is not; a script's click event toggles a checkbox as in browsers.
     * <p>
     * 마우스 이벤트인 click 만 활성화 이벤트이다. 이름이 click 인 일반 <code>Event</code> 는 아니다. 스크립트의 click 이벤트는 브라우저처럼 체크박스를 토글한다.
     */
    public void testSyntheticClicks() {
        Document d = FormTestSupport.page("<input id=c type=checkbox><form id=f action=/a><input id=s type=submit></form>");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        c.dispatchEvent(new Event("click", true, true));
        Check.isFalse(c.isChecked(), "a plain event named click does nothing");
        c.dispatchEvent(click());
        Check.isTrue(c.isChecked(), "an untrusted mouse click toggles");
        d.getElementById("s").dispatchEvent(click());
        Check.equal(1, host.submissions.size(), "an untrusted mouse click on a submit button submits");
        Check.isTrue(FormActivation.click(c), "click() returns true");
        Check.isFalse(c.isChecked(), "click() toggles");
        FormActivation.click(c, false);
        Check.isTrue(c.isChecked(), "click() with an untrusted event too");
        c.setAttribute("disabled", "");
        FormActivation.click(c);
        Check.isTrue(c.isChecked(), "click() does nothing for a disabled control");
    }

    /**
     * A file upload control asks the host only for a click of the user; the chosen files are selected and <code>input</code> and <code>change</code> follow; a cancelled
     * chooser fires <code>cancel</code>.
     * <p>
     * 파일 업로드 컨트롤은 사용자의 click 에만 호스트에 파일을 요청한다. 고른 파일이 선택되고 <code>input</code>, <code>change</code> 가 이어진다. 취소한 선택기는
     * <code>cancel</code> 을 발생시킨다.
     */
    public void testFileInputActivation() {
        Document d = FormTestSupport.page("<input id=f type=file accept='.txt' multiple><input id=g type=file>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        List<String> log = new ArrayList<String>();
        watch(d, log, "input", "change", "cancel");
        host.files = null;
        trustedClick(d, "f");
        Check.equal(1, host.chooserCalls, "the chooser was opened");
        Check.equal(Arrays.asList("cancel@f"), log, "a cancelled chooser fires cancel");
        log.clear();
        host.files = new FileEntry[] {new FileEntry("a.txt", "text/plain", 1, new byte[] {1, 2}), new FileEntry("b.txt", "text/plain", 2, new byte[0])};
        trustedClick(d, "f");
        Check.equal(2, FormTestSupport.input(d, "f").getFiles().getLength(), "two files with multiple");
        Check.equal(Arrays.asList("input@f", "change@f"), log, "events");
        Check.equal(2L, FormTestSupport.input(d, "f").getFiles().item(0).getSize(), "size");
        trustedClick(d, "g");
        Check.equal(1, FormTestSupport.input(d, "g").getFiles().getLength(), "without multiple only the first file is kept");
        d.getElementById("f").dispatchEvent(click());
        Check.equal(3, host.chooserCalls, "an untrusted click does not open the chooser");
        FormTestSupport.input(d, "f").setValue("");
        Check.equal(0, FormTestSupport.input(d, "f").getFiles().getLength(), "setting the value to the empty string clears the files");
    }

    /**
     * An image button remembers the position of the click.
     * <p>
     * 이미지 버튼은 click 위치를 기억한다.
     */
    public void testImageButtonClick() {
        Document d = FormTestSupport.page("<form id=f action=/a><input id=im type=image name=pic src=x.png></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        MouseEvent m = click();
        m.setCoordinates(0, 0, 0, 0, 0, 0, 12.4, 7.6);
        d.getElementById("im").dispatchTrustedEvent(m);
        Check.equal(1, host.submissions.size(), "submitted");
        Check.equal("http://example.test/a?pic.x=12&pic.y=8", host.submissions.get(0).getUrl(), "the offset of the click, rounded");
    }

    /**
     * Reset buttons reset the form through the <code>reset</code> event; submit buttons outside a form do nothing.
     * <p>
     * reset 버튼은 <code>reset</code> 이벤트를 거쳐 폼을 초기화한다. 폼 밖의 submit 버튼은 아무 일도 하지 않는다.
     */
    public void testResetButtonClick() {
        Document d = FormTestSupport.page("<form id=f><input id=t value=a><input id=r type=reset></form><input id=orphan type=submit><button id=ob></button>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        List<String> log = new ArrayList<String>();
        watch(d, log, "reset");
        FormTestSupport.input(d, "t").setValue("changed");
        trustedClick(d, "r");
        Check.equal("a", FormTestSupport.input(d, "t").getValue(), "reset");
        Check.equal(Arrays.asList("reset@f"), log, "reset event");
        trustedClick(d, "orphan");
        trustedClick(d, "ob");
        Check.equal(0, host.submissions.size(), "no form, no submission");
    }

    /**
     * Implicit submission: Enter in a text field clicks the default button if there is one, otherwise submits when at most one field blocks it.
     * <p>
     * 암묵적 제출: 텍스트 필드에서 Enter 를 누르면 기본 버튼이 있으면 그 버튼을 click 하고, 없으면 막는 필드가 하나 이하일 때 제출한다.
     */
    public void testImplicitSubmission() {
        Document d = FormTestSupport.page("<form id=a action=/a><input name=x value=1><input type=submit name=go value=Go><input type=submit name=other></form>"
                + "<form id=b action=/b><input name=x value=1></form>"
                + "<form id=c action=/c><input name=x value=1><input name=y value=2></form>"
                + "<form id=e action=/e><input name=x value=1><input name=y value=2><input type=submit name=s value=S disabled></form>"
                + "<form id=g action=/g><input name=x value=1><input type=checkbox name=c checked></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        FormTestSupport.form(d, "a").implicitSubmit();
        Check.equal("http://example.test/a?x=1&go=Go", host.submissions.get(0).getUrl(), "the default button is the submitter");
        FormTestSupport.form(d, "b").implicitSubmit();
        Check.equal("http://example.test/b?x=1", host.submissions.get(1).getUrl(), "one text field and no button");
        FormTestSupport.form(d, "c").implicitSubmit();
        Check.equal(2, host.submissions.size(), "two text fields and no button: no implicit submission");
        FormTestSupport.form(d, "e").implicitSubmit();
        Check.equal(2, host.submissions.size(), "a disabled default button: nothing happens");
        FormTestSupport.form(d, "g").implicitSubmit();
        Check.equal(3, host.submissions.size(), "a checkbox does not block implicit submission");
        Check.equal("http://example.test/g?x=1&c=on", host.submissions.get(2).getUrl(), "with the checkbox");
    }

    /**
     * The state change notifications reach the host.
     * <p>
     * 상태 변경 통지가 호스트에 닿는다.
     */
    public void testHostIsNotified() {
        Document d = FormTestSupport.page("<input id=t><input id=c type=checkbox>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        int before = host.stateChanges;
        FormTestSupport.input(d, "t").setValue("x");
        Check.isTrue(host.stateChanges > before, "a value change is reported");
        before = host.stateChanges;
        trustedClick(d, "c");
        Check.isTrue(host.stateChanges > before, "a toggle is reported");
    }
}
