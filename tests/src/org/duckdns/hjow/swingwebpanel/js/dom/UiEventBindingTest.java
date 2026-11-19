package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script-level tests of the UI event interfaces (plan item P11-01): <code>UIEvent</code>, <code>MouseEvent</code>,
 * <code>WheelEvent</code>, <code>KeyboardEvent</code>, <code>FocusEvent</code>, <code>InputEvent</code> and <code>CompositionEvent</code> as
 * script constructors with their option dictionaries, the interface chain, the members, <code>document.createEvent</code>, and the delivery
 * of events that the host builds in Java to the listeners of the page.
 * <p>
 * UI 이벤트 인터페이스의 스크립트 수준 시험(계획 P11-01): 스크립트 생성자와 그 옵션 사전으로 만드는 <code>UIEvent</code>,
 * <code>MouseEvent</code>, <code>WheelEvent</code>, <code>KeyboardEvent</code>, <code>FocusEvent</code>, <code>InputEvent</code>,
 * <code>CompositionEvent</code>, 인터페이스 사슬, 멤버, <code>document.createEvent</code>, 호스트가 Java 로 만든 이벤트가 페이지의 리스너에
 * 전달되는 것.
 */
public class UiEventBindingTest {

    /**
     * The page used by the cases.
     * <p>
     * 사례가 쓰는 페이지.
     */
    private static final String PAGE = "<!doctype html><html><body><div id=a><span id=b>x</span></div></body></html>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public UiEventBindingTest() {
    }

    /**
     * P11-01: the interfaces exist, extend each other as specified and are not callable without new.
     * <p>
     * P11-01: 인터페이스가 있고 명세대로 서로를 확장하며 new 없이는 호출할 수 없다.
     */
    public void testInterfaceChain() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("true", e.eval("new MouseEvent('click') instanceof UIEvent && new MouseEvent('click') instanceof Event"), "mouse chain");
        Check.equal("true", e.eval("new WheelEvent('wheel') instanceof MouseEvent && new KeyboardEvent('keydown') instanceof UIEvent"),
                "wheel and keyboard");
        Check.equal("true", e.eval("new FocusEvent('focus') instanceof UIEvent && new InputEvent('input') instanceof UIEvent && "
                + "new CompositionEvent('compositionend') instanceof UIEvent"), "focus, input, composition");
        Check.equal("[object MouseEvent]", e.eval("Object.prototype.toString.call(new MouseEvent('x'))"), "tag");
        Check.equal("throws", e.eval("(function () { try { MouseEvent('x'); return 'no'; } catch (err) { return 'throws'; } })()"), "needs new");
        Check.equal("throws", e.eval("(function () { try { new KeyboardEvent(); return 'no'; } catch (err) { return 'throws'; } })()"),
                "type is required");
        Check.equal("true", e.eval("Object.getPrototypeOf(MouseEvent) === UIEvent && Object.getPrototypeOf(UIEvent) === Event"),
                "constructor chain");
    }

    /**
     * P11-01: the option dictionaries fill the members and the modifier state is readable.
     * <p>
     * P11-01: 옵션 사전이 멤버를 채우고 보조 키 상태를 읽을 수 있다.
     */
    public void testConstructorOptions() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("5,6,7,8,5,6,1,3,true,false,true,false,2",
                e.eval("var m = new MouseEvent('click', { clientX: 5, clientY: 6, screenX: 7, screenY: 8, button: 1, buttons: 3, ctrlKey: true,"
                        + "altKey: true, detail: 2 }); [m.clientX, m.clientY, m.screenX, m.screenY, m.x, m.y, m.button, m.buttons, m.ctrlKey,"
                        + "m.shiftKey, m.altKey, m.metaKey, m.detail].join(',')"), "mouse options");
        Check.equal("true,true,false", e.eval("[m.getModifierState('Control'), m.getModifierState('Alt'), m.getModifierState('Shift')].join(',')"),
                "modifier state");
        Check.equal("a,KeyA,1,65,0,65,true,false", e.eval("var k = new KeyboardEvent('keydown', { key: 'a', code: 'KeyA', location: 1, keyCode: 65,"
                + "repeat: true }); [k.key, k.code, k.location, k.keyCode, k.charCode, k.which, k.repeat, k.isComposing].join(',')"), "keyboard");
        Check.equal("97,97", e.eval("var p = new KeyboardEvent('keypress', { key: 'a', charCode: 97, keyCode: 97 }); p.which + ',' + p.charCode"),
                "keypress which is the character code");
        Check.equal("1,2,3,1", e.eval("var w = new WheelEvent('wheel', { deltaX: 1, deltaY: 2, deltaZ: 3, deltaMode: 1 }); "
                + "[w.deltaX, w.deltaY, w.deltaZ, w.deltaMode].join(',')"), "wheel");
        Check.equal("true,1,2", e.eval("[WheelEvent.DOM_DELTA_PIXEL === 0, WheelEvent.DOM_DELTA_LINE, WheelEvent.DOM_DELTA_PAGE].join(',')"),
                "wheel constants");
        Check.equal("hello,insertText,false", e.eval("var i = new InputEvent('input', { data: 'hello', inputType: 'insertText' }); "
                + "[i.data, i.inputType, i.isComposing].join(',')"), "input");
        Check.equal("null", e.eval("String(new InputEvent('input').data)"), "input without data");
        Check.equal("한", e.eval("new CompositionEvent('compositionupdate', { data: '한' }).data"), "composition");
        Check.equal("true,false", e.eval("var ev = new MouseEvent('click', { bubbles: true, cancelable: true }); [ev.bubbles, ev.composed].join(',')"),
                "base options");
    }

    /**
     * P11-01: events reach listeners with the related target and bubble and cancel like other events.
     * <p>
     * P11-01: 이벤트는 상대 대상과 함께 리스너에 도달하고 다른 이벤트처럼 버블링과 취소가 된다.
     */
    public void testDispatchAndRelatedTarget() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("a:b|b:b", e.eval("var log = []; var a = document.getElementById('a'), b = document.getElementById('b');"
                + "a.addEventListener('mouseover', function (ev) { log.push('a:' + ev.relatedTarget.id); });"
                + "b.addEventListener('mouseover', function (ev) { log.push('b:' + ev.relatedTarget.id); });"
                + "b.dispatchEvent(new MouseEvent('mouseover', { bubbles: true, relatedTarget: b })); log.reverse().join('|')"), "bubbling");
        Check.equal("false", e.eval("var c = new MouseEvent('click', { cancelable: true }); b.addEventListener('click', function (ev) { "
                + "ev.preventDefault(); }); String(b.dispatchEvent(c))"), "cancelable events can be cancelled");
        Check.equal("focus", e.eval("var f; b.addEventListener('focus', function (ev) { f = ev.type + (ev instanceof FocusEvent ? '' : '!'); });"
                + "b.dispatchEvent(new FocusEvent('focus')); f"), "focus listener");
        Check.equal("null", e.eval("String(new FocusEvent('blur').relatedTarget)"), "related target defaults to null");
    }

    /**
     * P11-01: <code>document.createEvent</code> knows the UI event interfaces, and the event handler attributes receive them.
     * <p>
     * P11-01: <code>document.createEvent</code> 가 UI 이벤트 인터페이스를 알고 이벤트 핸들러 속성이 그것을 받는다.
     */
    public void testCreateEventAndHandlers() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("true,true", e.eval("var m = document.createEvent('MouseEvents'); m.initEvent('click', true, true); "
                + "[m instanceof MouseEvent, m.type === 'click'].join(',')"), "MouseEvents");
        Check.equal("true", e.eval("document.createEvent('KeyboardEvent') instanceof KeyboardEvent"), "KeyboardEvent");
        Check.equal("true", e.eval("document.createEvent('UIEvents') instanceof UIEvent && document.createEvent('FocusEvent') instanceof FocusEvent"),
                "UIEvents and FocusEvent");
        Check.equal("throws", e.eval("(function () { try { document.createEvent('NoSuchEvent'); return 'no'; } catch (err) { return 'throws'; } })()"),
                "unknown interface");
        Check.equal("k:Enter", e.eval("var got = ''; var el = document.getElementById('b'); el.onkeydown = function (ev) { got = 'k:' + ev.key; };"
                + "el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' })); got"), "onkeydown");
    }

    /**
     * P11-01: an event that the host builds in Java is seen by the page with the same members, and it is trusted only when the host says
     * so.
     * <p>
     * P11-01: 호스트가 Java 로 만든 이벤트를 페이지가 같은 멤버로 보며, 호스트가 신뢰한다고 할 때만 신뢰 이벤트이다.
     */
    public void testHostBuiltEvents() {
        DomTestEnv e = new DomTestEnv(PAGE);
        e.eval("var seen = ''; document.getElementById('b').addEventListener('mousedown', function (ev) { seen = [ev.type, ev.clientX, ev.clientY,"
                + "ev.pageX, ev.offsetX, ev.button, ev.buttons, ev.shiftKey, ev.isTrusted, ev instanceof MouseEvent].join(','); });"
                + "document.addEventListener('keydown', function (ev) { seen += '|' + [ev.key, ev.code, ev.keyCode, ev.ctrlKey, ev.isTrusted].join(','); });");
        Document d = e.document;
        Element b = d.getElementById("b");
        MouseEvent m = new MouseEvent("mousedown", true, true);
        m.setCoordinates(110, 120, 10, 20, 15, 25, 3, 4);
        m.setButtons(0, 1);
        m.setModifiers(false, true, false, false);
        b.dispatchTrustedEvent(m);
        KeyboardEvent k = new KeyboardEvent("keydown", true, true);
        k.setKey("x", "KeyX", 0, 88, 0);
        k.setModifiers(true, false, false, false);
        d.dispatchEvent(k);
        Check.equal("mousedown,10,20,15,3,0,1,true,true,true|x,KeyX,88,true,false", e.eval("seen"), "what the page saw");
    }
}
