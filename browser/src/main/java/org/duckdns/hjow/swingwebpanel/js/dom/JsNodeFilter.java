package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.NodeFilter;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Adapts the <code>NodeFilter</code> callback of a script to the Java {@link NodeFilter}: a function is called with the node as its only
 * argument, any other object must have an <code>acceptNode</code> method that is called with the object as <code>this</code> (the rules of a
 * WebIDL callback interface). The result is converted to an unsigned short, like WebIDL does. An exception of the script goes through to the
 * traversal call of the caller.
 * <p>
 * 스크립트의 <code>NodeFilter</code> 콜백을 Java {@link NodeFilter} 에 맞춘다: 함수는 노드를 유일한 인수로 호출하고, 그 밖의 객체는
 * <code>acceptNode</code> 메서드가 있어야 하며 그 객체를 <code>this</code> 로 호출한다(WebIDL 콜백 인터페이스의 규칙). 결과는 WebIDL 처럼 unsigned
 * short 로 바꾼다. 스크립트의 예외는 호출자의 탐색 호출까지 전달된다.
 */
public final class JsNodeFilter implements NodeFilter {

    /**
     * The bridge that wraps the nodes for the script.
     * <p>
     * 노드를 스크립트용으로 감싸는 브리지.
     */
    private final DomBridge bridge;

    /**
     * The function or object of the script.
     * <p>
     * 스크립트의 함수 또는 객체.
     */
    private final JsObject callback;

    /**
     * Creates the adapter.
     * <p>
     * 어댑터를 만든다.
     *
     * @param bridge   the bridge / 브리지
     * @param callback the function or object with <code>acceptNode</code> / 함수 또는 <code>acceptNode</code> 를 가진 객체
     */
    public JsNodeFilter(DomBridge bridge, JsObject callback) {
        this.bridge = bridge;
        this.callback = callback;
    }

    /**
     * Returns the function or object of the script (what <code>filter</code> returns to the script).
     * <p>
     * 스크립트의 함수 또는 객체를 반환한다(<code>filter</code> 가 스크립트에 돌려주는 것).
     *
     * @return the callback / 콜백
     */
    public JsObject getCallback() {
        return callback;
    }

    /**
     * Calls the script.
     * <p>
     * 스크립트를 호출한다.
     *
     * @param node the node / 노드
     * @return the result as an unsigned short / unsigned short 로 바꾼 결과
     */
    public short acceptNode(Node node) {
        JsValue fn = callback;
        JsValue self = Js.UNDEFINED;
        // 함수가 아니면 acceptNode 메서드를 찾아 그 객체를 this 로 호출한다.
        if (!callback.isCallable()) {
            fn = callback.get("acceptNode");
            self = callback;
            if (!fn.isCallable()) {
                throw JsException.typeError("the node filter has no acceptNode function");
            }
        }
        JsValue result = Ops.call(fn, self, new JsValue[] { bridge.wrap(node) });
        return (short) Ops.toUint16(result);
    }

    /**
     * Converts the argument of <code>createNodeIterator</code> or <code>createTreeWalker</code>: undefined and null mean no filter, an
     * object becomes an adapter, anything else is a TypeError.
     * <p>
     * <code>createNodeIterator</code>, <code>createTreeWalker</code> 의 인수를 바꾼다: undefined 와 null 은 필터 없음, 객체는 어댑터, 그 밖은 TypeError.
     *
     * @param bridge the bridge / 브리지
     * @param v      the argument / 인수
     * @return the filter or null / 필터 또는 null
     */
    public static NodeFilter of(DomBridge bridge, JsValue v) {
        if (v.isNullOrUndefined()) {
            return null;
        }
        if (!v.isObject()) {
            throw JsException.typeError("the node filter is not an object");
        }
        return new JsNodeFilter(bridge, (JsObject) v);
    }
}
