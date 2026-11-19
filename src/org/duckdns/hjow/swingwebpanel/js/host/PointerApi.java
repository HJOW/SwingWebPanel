package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * The pointer capture methods of <code>Element</code> (W3C Pointer Events Level 3, sections 5 and 9): <code>setPointerCapture(pointerId)</code>,
 * <code>releasePointerCapture(pointerId)</code> and <code>hasPointerCapture(pointerId)</code>. They check their arguments the way the
 * specification says and hand the state to the {@link PointerService} of the host: a pointer id that is not an active pointer is a
 * <code>NotFoundError</code>, an element that is not connected is an <code>InvalidStateError</code> (for <code>setPointerCapture</code> only), a pointer without a
 * button down silently keeps its state. A host without a service has no active pointer. The events that announce the capture
 * (<code>gotpointercapture</code>, <code>lostpointercapture</code>) are fired by the component when it delivers the next pointer event.
 * <p>
 * <code>Element</code> 의 포인터 캡처 메서드(W3C Pointer Events Level 3, 5절과 9절): <code>setPointerCapture(pointerId)</code>,
 * <code>releasePointerCapture(pointerId)</code>, <code>hasPointerCapture(pointerId)</code>. 인수를 규격대로 검사하고 상태는 호스트의 {@link PointerService} 에
 * 넘긴다: 활성 포인터가 아닌 ID 는 <code>NotFoundError</code>, 연결되지 않은 요소는 <code>InvalidStateError</code>(<code>setPointerCapture</code> 만), 눌린 버튼이 없는
 * 포인터는 상태를 조용히 유지한다. 서비스가 없는 호스트에는 활성 포인터가 없다. 캡처를 알리는 이벤트(<code>gotpointercapture</code>,
 * <code>lostpointercapture</code>)는 컴포넌트가 다음 포인터 이벤트를 전달할 때 발생시킨다.
 */
final class PointerApi {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    PointerApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Installs the three methods on the prototype of <code>Element</code>.
     * <p>
     * <code>Element</code> 프로토타입에 세 메서드를 설치한다.
     */
    void install() {
        JsObject proto = page.getBridge().getInterface("Element").getPrototype();
        method(proto, "setPointerCapture", new NativeFunction.Call() {
            /**
             * Sets the pending capture of a pointer to the element.
             * <p>
             * 포인터의 대기 중인 캡처를 요소로 정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                int id = pointerId(args, "setPointerCapture");
                PointerService service = activeService(id);
                if (!e.isConnected()) {
                    throw error(DomException.invalidState("Failed to execute 'setPointerCapture' on 'Element': The element is not connected."));
                }
                service.setCapture(id, e);
                return Js.UNDEFINED;
            }
        });
        method(proto, "releasePointerCapture", new NativeFunction.Call() {
            /**
             * Clears the pending capture of a pointer if the element has it.
             * <p>
             * 요소가 가졌으면 포인터의 대기 중인 캡처를 지운다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                int id = pointerId(args, "releasePointerCapture");
                PointerService service = activeService(id);
                service.releaseCapture(id, e);
                return Js.UNDEFINED;
            }
        });
        method(proto, "hasPointerCapture", new NativeFunction.Call() {
            /**
             * Tells whether the element has the pending capture of a pointer (an id that is not active is simply false).
             * <p>
             * 요소가 포인터의 대기 중인 캡처를 가졌는지 알려 준다(활성이 아닌 ID 는 그냥 false).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the answer / 답
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                int id = pointerId(args, "hasPointerCapture");
                PointerService service = page.getEnvironment().getPointerService();
                return Js.bool(service != null && service.hasCapture(id, e));
            }
        });
    }

    /**
     * Defines a method of one argument on a prototype.
     * <p>
     * 프로토타입에 인수 하나인 메서드를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the method name / 메서드 이름
     * @param body  the body / 본문
     */
    private void method(JsObject proto, String name, NativeFunction.Call body) {
        proto.defineBuiltin(name, page.getRealm().newFunction(name, 1, body));
    }

    /**
     * Returns the element that a method was called on or throws the standard TypeError.
     * <p>
     * 메서드를 부른 요소를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element element(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Converts the first argument to a <code>long</code> pointer id (WebIDL: a missing argument is a <code>TypeError</code>).
     * <p>
     * 첫 인수를 <code>long</code> 포인터 ID 로 바꾼다(WebIDL: 없는 인수는 <code>TypeError</code>).
     *
     * @param args   the arguments / 인수
     * @param method the method name for the message / 메시지용 메서드 이름
     * @return the id / ID
     */
    private static int pointerId(JsValue[] args, String method) {
        if (args.length < 1) {
            throw JsException.typeError("Failed to execute '" + method + "' on 'Element': 1 argument required, but only 0 present.");
        }
        double d = Ops.toNumber(args[0]);
        return Double.isNaN(d) || Double.isInfinite(d) ? 0 : (int) d;
    }

    /**
     * Returns the service after checking that the pointer is active: no service or an id that is not active is a <code>NotFoundError</code>.
     * <p>
     * 포인터가 활성인지 확인한 뒤 서비스를 반환한다. 서비스가 없거나 활성이 아닌 ID 는 <code>NotFoundError</code> 이다.
     *
     * @param id the pointer id / 포인터 ID
     * @return the service / 서비스
     */
    private PointerService activeService(int id) {
        PointerService service = page.getEnvironment().getPointerService();
        if (service == null || !service.isActive(id)) {
            throw error(DomException.notFound("No active pointer with the given id is found."));
        }
        return service;
    }

    /**
     * Turns a DOM exception of Java code into the exception of the script.
     * <p>
     * Java 코드의 DOM 예외를 스크립트 예외로 바꾼다.
     *
     * @param ex the exception / 예외
     * @return the script exception / 스크립트 예외
     */
    private JsException error(DomException ex) {
        return page.getBridge().domError(ex);
    }
}
