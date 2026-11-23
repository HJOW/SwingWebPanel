package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The listener behind an event handler IDL attribute such as <code>onclick</code> or an event handler content attribute: it is registered
 * once, at the first assignment, and later assignments only replace the callback, so the handler keeps its position in the listener list.
 * A callback that returns <code>false</code> cancels the event. The callback may also be compiled lazily from the attribute text.
 * <p>
 * <code>onclick</code> 같은 이벤트 핸들러 IDL 속성이나 이벤트 핸들러 콘텐츠 속성 뒤의 리스너. 첫 대입 때 한 번 등록하며 이후 대입은 콜백만
 * 바꾸므로 핸들러가 리스너 목록에서 자기 위치를 유지한다. <code>false</code> 를 반환하는 콜백은 이벤트를 취소한다. 콜백은 속성 텍스트에서
 * 필요할 때 컴파일할 수도 있다.
 */
public final class HandlerListener implements EventListener {

    /**
     * Compiles the callback of an event handler content attribute on first use.
     * <p>
     * 이벤트 핸들러 콘텐츠 속성의 콜백을 처음 쓸 때 컴파일한다.
     */
    public interface Compiler {
        /**
         * Compiles the handler.
         * <p>
         * 핸들러를 컴파일한다.
         *
         * @return the function, or null if the source is invalid (the error was reported) / 함수. 소스가 올바르지 않으면 null(오류는 보고됨)
         */
        JsObject compile();
    }

    /**
     * The bridge of the page.
     * <p>
     * 페이지의 브리지.
     */
    private final DomBridge bridge;

    /**
     * The current callback, or null if none.
     * <p>
     * 현재 콜백. 없으면 null.
     */
    private JsObject callback;

    /**
     * The pending compiler of a content attribute, or null.
     * <p>
     * 아직 컴파일하지 않은 콘텐츠 속성의 컴파일러. 없으면 null.
     */
    private Compiler compiler;

    /**
     * Creates a handler with no callback.
     * <p>
     * 콜백이 없는 핸들러를 만든다.
     *
     * @param bridge the bridge / 브리지
     */
    public HandlerListener(DomBridge bridge) {
        this.bridge = bridge;
    }

    /**
     * Sets the callback (null clears the handler).
     * <p>
     * 콜백을 지정한다(null 이면 핸들러를 지운다).
     *
     * @param cb the callback or null / 콜백 또는 null
     */
    public void setCallback(JsObject cb) {
        this.callback = cb;
        this.compiler = null;
    }

    /**
     * Sets a compiler that produces the callback when the first event arrives (event handler content attribute).
     * <p>
     * 첫 이벤트가 올 때 콜백을 만드는 컴파일러를 지정한다(이벤트 핸들러 콘텐츠 속성).
     *
     * @param c the compiler / 컴파일러
     */
    public void setCompiler(Compiler c) {
        this.callback = null;
        this.compiler = c;
    }

    /**
     * Returns the current callback; a pending compiler is run first.
     * <p>
     * 현재 콜백을 반환한다. 대기 중인 컴파일러가 있으면 먼저 실행한다.
     *
     * @return the callback or null / 콜백 또는 null
     */
    public JsObject getCallback() {
        if (compiler != null) {
            Compiler c = compiler;
            compiler = null;
            callback = c.compile();
        }
        return callback;
    }

    /**
     * Calls the callback; a result of <code>false</code> cancels the event.
     * <p>
     * 콜백을 부른다. 결과가 <code>false</code> 이면 이벤트를 취소한다.
     *
     * @param event the event / 이벤트
     */
    public void handleEvent(Event event) {
        if (!bridge.isEventExecutionEnabled()) {
            return;
        }
        JsObject cb = getCallback();
        if (cb == null || !cb.isCallable()) {
            return;
        }
        JsValue previousEvent = null;
        try {
            JsValue self = event.getCurrentTarget() == null ? Js.UNDEFINED : bridge.toJs(event.getCurrentTarget());
            JsValue arg = bridge.toJs(event);
            // 핸들러가 도는 동안 window.event 는 이 이벤트이다(중첩 전달을 위해 이전 값을 보관한다).
            previousEvent = bridge.enterEvent(arg);
            JsValue result = Ops.call(cb, self, new JsValue[] { arg });
            if (event instanceof org.duckdns.hjow.swingwebpanel.dom.BeforeUnloadEvent) {
                // onbeforeunload 는 null/undefined 가 아닌 반환 값을 확인 요청으로 본다: 이벤트를 취소하고, returnValue 가 비어 있으면 그 문자열을 returnValue 로 정한다.
                if (!result.isNullOrUndefined()) {
                    event.preventDefault();
                    org.duckdns.hjow.swingwebpanel.dom.BeforeUnloadEvent unload = (org.duckdns.hjow.swingwebpanel.dom.BeforeUnloadEvent) event;
                    if (unload.getReturnValue().length() == 0) {
                        unload.setReturnValue(Ops.toString(result));
                    }
                }
            } else if (result == Js.FALSE) {
                event.preventDefault();
            }
        } catch (JsException e) {
            bridge.reportError(e);
        } finally {
            if (previousEvent != null) {
                bridge.leaveEvent(previousEvent);
            }
        }
    }
}
