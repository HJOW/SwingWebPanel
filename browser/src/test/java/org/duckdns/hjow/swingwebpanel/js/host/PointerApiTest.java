package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the script side of pointer events against the W3C Pointer Events Level 3 text that was fetched on 2026-11-17 (see
 * <code>docs/progress/webapi2.md</code>): the <code>PointerEvent</code> interface and constructor, the pointer capture methods of <code>Element</code> with a
 * fake {@link PointerService}. The event order and the capture behaviour on real input are in <code>org.duckdns.hjow.swingwebpanel.core.PointerEventIntegrationTest</code>.
 * <p>
 * 포인터 이벤트의 스크립트 쪽을 2026-11-17 에 조회한 W3C Pointer Events Level 3 본문으로 시험한다(<code>docs/progress/webapi2.md</code> 참조):
 * <code>PointerEvent</code> 인터페이스와 생성자, 가짜 {@link PointerService} 로 <code>Element</code> 의 포인터 캡처 메서드. 실제 입력의 이벤트 순서와 캡처 동작은
 * <code>org.duckdns.hjow.swingwebpanel.core.PointerEventIntegrationTest</code> 에 있다.
 */
public class PointerApiTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public PointerApiTest() {
    }

    /**
     * The interface object, the prototype chain and the defaults of a constructed event.
     * <p>
     * 인터페이스 객체, 프로토타입 체인, 생성한 이벤트의 기본값.
     */
    public void testInterfaceAndDefaults() {
        PageFixture f = new PageFixture().load("<div id=a></div>");
        Check.equal("function|true|true|true|[object PointerEvent]", f.scripts.evaluate("[typeof PointerEvent, PointerEvent.prototype instanceof MouseEvent, "
                + "PointerEvent.prototype instanceof UIEvent, new PointerEvent('pointerdown') instanceof Event, "
                + "Object.prototype.toString.call(new PointerEvent('pointerdown'))].join('|')", "t"), "interface, chain, tag");
        Check.equal("0|1|1|0|0|0|0|0|1.5707963267948966|0||false", f.scripts.evaluate("var e = new PointerEvent('pointerdown'); [e.pointerId, e.width, "
                + "e.height, e.pressure, e.tangentialPressure, e.tiltX, e.tiltY, e.twist, e.altitudeAngle, e.azimuthAngle, e.pointerType, e.isPrimary].join('|')",
                "t"), "the defaults of PointerEventInit");
        Check.equal("false|false|false|false", f.scripts.evaluate("[e.bubbles, e.cancelable, e.isTrusted, e.composed].join('|')", "t"), "event defaults");
        Check.equal("0|0|0|0|null", f.scripts.evaluate("[e.clientX, e.clientY, e.button, e.buttons, String(e.relatedTarget)].join('|')", "t"), "the mouse defaults");
        Check.equal("0|0", f.scripts.evaluate("[e.getCoalescedEvents().length, e.getPredictedEvents().length].join('|')", "t"), "no coalesced and no predicted events");
        Check.equal("TypeError", f.scripts.evaluate("try { new PointerEvent(); 'no' } catch (x) { x.name }", "t"), "the type is required");
        Check.equal("TypeError", f.scripts.evaluate("try { PointerEvent('pointerdown'); 'no' } catch (x) { x.name }", "t"), "new is required");
    }

    /**
     * The members of the init dictionary reach the event.
     * <p>
     * 초기화 사전의 멤버가 이벤트에 닿는다.
     */
    public void testInitDictionary() {
        PageFixture f = new PageFixture().load("<div id=a></div>");
        Check.equal("7|12|14.5|0.75|-0.5|30|-20|90|0.5|1.25|pen|true", f.scripts.evaluate("var e = new PointerEvent('pointermove', {pointerId: 7, width: 12, "
                + "height: 14.5, pressure: 0.75, tangentialPressure: -0.5, tiltX: 30, tiltY: -20, twist: 90, altitudeAngle: 0.5, azimuthAngle: 1.25, "
                + "pointerType: 'pen', isPrimary: true, clientX: 5, clientY: 6, button: 2, buttons: 2, ctrlKey: true, bubbles: true, cancelable: true}); "
                + "[e.pointerId, e.width, e.height, e.pressure, e.tangentialPressure, e.tiltX, e.tiltY, e.twist, e.altitudeAngle, e.azimuthAngle, e.pointerType, "
                + "e.isPrimary].join('|')", "t"), "pointer members");
        Check.equal("5|6|2|2|true|true|true", f.scripts.evaluate("[e.clientX, e.clientY, e.button, e.buttons, e.ctrlKey, e.bubbles, e.cancelable].join('|')", "t"),
                "mouse and event members");
        Check.equal("2|1|true", f.scripts.evaluate("var a = new PointerEvent('pointermove', {pointerId: 1}), b = new PointerEvent('pointermove', {pointerId: 2}); "
                + "var c = new PointerEvent('pointermove', {coalescedEvents: [a, b], predictedEvents: [b]}); "
                + "var list = c.getCoalescedEvents(); [list.length, c.getPredictedEvents().length, list[0] === a && list[1] === b].join('|')", "t"),
                "coalesced and predicted events");
        Check.equal("TypeError", f.scripts.evaluate("try { new PointerEvent('pointermove', {coalescedEvents: [{}]}); 'no' } catch (x) { x.name }", "t"),
                "an item that is not a PointerEvent");
        Check.equal("true", f.scripts.evaluate("var c1 = new PointerEvent('pointermove', {coalescedEvents: [a]}); c1.getCoalescedEvents().push(1); "
                + "String(c1.getCoalescedEvents().length === 1)", "t"), "the lists are copies");
    }

    /**
     * A constructed event is dispatched like any other event and reaches handlers by attribute and by listener.
     * <p>
     * 생성한 이벤트는 다른 이벤트와 같이 전달되며 속성 핸들러와 리스너에 닿는다.
     */
    public void testDispatchAndHandlers() {
        PageFixture f = new PageFixture().load("<div id=a></div>");
        Check.equal("true|true|true|true", f.scripts.evaluate("var a = document.getElementById('a'); [a.onpointerdown === null, a.onpointerup === null, "
                + "a.ongotpointercapture === null, a.onlostpointercapture === null].join('|')", "t"), "the handler attributes");
        Check.equal("pointerdown:7:mouse|up:7|false", f.scripts.evaluate("var log = []; a.onpointerdown = function (e) { log.push(e.type + ':' + e.pointerId + ':' + "
                + "e.pointerType); }; a.addEventListener('pointerup', function (e) { log.push('up:' + e.pointerId); });"
                + "a.dispatchEvent(new PointerEvent('pointerdown', {pointerId: 7, pointerType: 'mouse'})); "
                + "var r = a.dispatchEvent(new PointerEvent('pointerup', {pointerId: 7, cancelable: true})); log.join('|') + '|' + (!r)", "t"),
                "handlers by attribute and by listener");
    }

    /**
     * Without a pointer service the page has no active pointer.
     * <p>
     * 포인터 서비스가 없으면 페이지에는 활성 포인터가 없다.
     */
    public void testCaptureMethodsWithoutAPointer() {
        PageFixture f = new PageFixture().load("<div id=a></div>");
        Check.equal("function|function|function", f.scripts.evaluate("var a = document.getElementById('a'); [typeof a.setPointerCapture, "
                + "typeof a.releasePointerCapture, typeof a.hasPointerCapture].join('|')", "t"), "the methods exist on every element");
        Check.equal("NotFoundError", f.scripts.evaluate("try { a.setPointerCapture(1); 'no' } catch (x) { x.name }", "t"), "no active pointer: set");
        Check.equal("NotFoundError", f.scripts.evaluate("try { a.releasePointerCapture(1); 'no' } catch (x) { x.name }", "t"), "no active pointer: release");
        Check.equal("false", f.scripts.evaluate("String(a.hasPointerCapture(1))", "t"), "hasPointerCapture only answers false");
        Check.equal("TypeError", f.scripts.evaluate("try { a.setPointerCapture(); 'no' } catch (x) { x.name }", "t"), "the id is required");
    }

    /**
     * A fake service that records what the script asks for.
     * <p>
     * 스크립트가 요청하는 것을 기록하는 가짜 서비스.
     */
    private static final class Recorder implements PointerService {
        /**
         * What was asked.
         * <p>
         * 요청된 것.
         */
        final List<String> calls = new ArrayList<String>();

        /**
         * The element that holds the pending capture.
         * <p>
         * 대기 중인 캡처를 가진 요소.
         */
        Element pending;

        /**
         * Pointer 1 is active, others are not.
         * <p>
         * 포인터 1 만 활성이다.
         *
         * @param pointerId the id / ID
         * @return true for 1 / 1 이면 true
         */
        public boolean isActive(int pointerId) {
            return pointerId == 1;
        }

        /**
         * Records the capture.
         * <p>
         * 캡처를 기록한다.
         *
         * @param pointerId the id / ID
         * @param element   the element / 요소
         */
        public void setCapture(int pointerId, Element element) {
            calls.add("set:" + pointerId + ":" + element.getAttribute("id"));
            pending = element;
        }

        /**
         * Tells whether the element holds the capture.
         * <p>
         * 요소가 캡처를 가졌는지 알려 준다.
         *
         * @param pointerId the id / ID
         * @param element   the element / 요소
         * @return the answer / 답
         */
        public boolean hasCapture(int pointerId, Element element) {
            return pending == element;
        }

        /**
         * Records the release.
         * <p>
         * 해제를 기록한다.
         *
         * @param pointerId the id / ID
         * @param element   the element / 요소
         */
        public void releaseCapture(int pointerId, Element element) {
            calls.add("release:" + pointerId + ":" + element.getAttribute("id"));
            if (pending == element) {
                pending = null;
            }
        }
    }

    /**
     * The checks of the three methods: <code>NotFoundError</code> for an id that is not active, <code>InvalidStateError</code> for an element that is not connected, the
     * state handed to the service.
     * <p>
     * 세 메서드의 검사: 활성이 아닌 ID 는 <code>NotFoundError</code>, 연결되지 않은 요소는 <code>InvalidStateError</code>, 서비스에 넘기는 상태.
     */
    public void testCaptureMethodsWithAPointer() {
        PageFixture f = new PageFixture();
        Recorder rec = new Recorder();
        f.env.pointers = rec;
        f.load("<div id=a></div><div id=b></div>");
        Check.equal("false", f.scripts.evaluate("var a = document.getElementById('a'), b = document.getElementById('b'); String(a.hasPointerCapture(1))", "t"),
                "no capture yet");
        f.scripts.evaluate("a.setPointerCapture(1)", "t");
        Check.equal("true|false", f.scripts.evaluate("[a.hasPointerCapture(1), b.hasPointerCapture(1)].join('|')", "t"),
                "hasPointerCapture is true at once, before any event announces the capture");
        Check.equal("NotFoundError", f.scripts.evaluate("try { a.setPointerCapture(2); 'no' } catch (x) { x.name }", "t"), "an id that is not active");
        Check.equal("NotFoundError", f.scripts.evaluate("try { b.releasePointerCapture(9); 'no' } catch (x) { x.name }", "t"), "release with an id that is not active");
        Check.equal("InvalidStateError", f.scripts.evaluate("var loose = document.createElement('div'); try { loose.setPointerCapture(1); 'no' } catch (x) { x.name }", "t"),
                "an element that is not connected");
        f.scripts.evaluate("b.releasePointerCapture(1)", "t");
        Check.equal("true", f.scripts.evaluate("String(a.hasPointerCapture(1))", "t"), "releasing an element that has no capture changes nothing");
        f.scripts.evaluate("a.releasePointerCapture(1)", "t");
        Check.equal("false", f.scripts.evaluate("String(a.hasPointerCapture(1))", "t"), "released");
        Check.equal("[set:1:a, release:1:b, release:1:a]", rec.calls.toString(), "what reached the service (the failures never did)");
    }
}
