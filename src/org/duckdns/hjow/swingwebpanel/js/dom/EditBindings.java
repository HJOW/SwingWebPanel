package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.StaticRange;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The JavaScript interfaces that editing adds (plan item P20-06): <code>StaticRange</code> (the target ranges of
 * <code>InputEvent.getTargetRanges()</code>; the constructor takes <code>startContainer</code>, <code>startOffset</code>,
 * <code>endContainer</code> and <code>endOffset</code>), and the helpers that read the <code>dataTransfer</code> and
 * <code>targetRanges</code> of an <code>InputEvent</code> init dictionary.
 * <p>
 * 편집이 더하는 JavaScript 인터페이스(계획 P20-06): <code>StaticRange</code>(<code>InputEvent.getTargetRanges()</code> 의 대상 범위. 생성자는
 * <code>startContainer</code>, <code>startOffset</code>, <code>endContainer</code>, <code>endOffset</code> 을 받는다)와 <code>InputEvent</code> 초기화 사전의
 * <code>dataTransfer</code>, <code>targetRanges</code> 를 읽는 도우미.
 */
public final class EditBindings {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EditBindings() {
    }

    /**
     * Installs <code>StaticRange</code> and the classifier that maps the Java class to it.
     * <p>
     * <code>StaticRange</code> 와 Java 클래스를 그것에 대응시키는 분류기를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a static range.
             * <p>
             * 정적 범위의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return o instanceof StaticRange ? "StaticRange" : null;
            }
        });
        final JsRealm realm = bridge.getRealm();
        final DomBridge.InterfaceInfo info = bridge.defineInterface("StaticRange", null, StaticRange.class,
                new String[] { "a:startContainer", "a:startOffset", "a:endContainer", "a:endOffset", "a:collapsed" });
        bridge.setConstructor("StaticRange", new NativeFunction(realm, "StaticRange", 1, new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'StaticRange': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a static range from the four members of the dictionary; the containers must be nodes that are not document types or
             * attributes.
             * <p>
             * 사전의 네 멤버로 정적 범위를 만든다. 컨테이너는 문서 유형이나 속성이 아닌 노드여야 한다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue init = Js.arg(args, 0);
                if (!init.isObject()) {
                    throw JsException.typeError("Failed to construct 'StaticRange': 1 argument required, but only 0 present.");
                }
                JsObject d = (JsObject) init;
                Node sc = container(d, "startContainer");
                Node ec = container(d, "endContainer");
                if (d.get("startOffset").isUndefined() || d.get("endOffset").isUndefined()) {
                    throw JsException.typeError("Failed to construct 'StaticRange': The required member startOffset is undefined.");
                }
                if (sc.getNodeType() == Node.DOCUMENT_TYPE_NODE || ec.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
                    throw bridge.domError(new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "the container is a document type"));
                }
                StaticRange sr = new StaticRange(sc, (int) Ops.toNumber(d.get("startOffset")), ec, (int) Ops.toNumber(d.get("endOffset")));
                HostObject w = bridge.wrap(sr);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Reads a node member of a dictionary (a required member).
     * <p>
     * 사전의 노드 멤버를 읽는다(필수 멤버).
     *
     * @param d    the dictionary / 사전
     * @param name the member / 멤버
     * @return the node / 노드
     */
    private static Node container(JsObject d, String name) {
        Node n = (Node) DomBridge.targetOf(d.get(name), Node.class);
        if (n == null) {
            throw JsException.typeError("Failed to construct 'StaticRange': The required member " + name + " is undefined or not a Node.");
        }
        return n;
    }

    /**
     * Reads the <code>targetRanges</code> member of an <code>InputEventInit</code> dictionary: an array of static ranges (other items are
     * skipped).
     * <p>
     * <code>InputEventInit</code> 사전의 <code>targetRanges</code> 멤버를 읽는다: 정적 범위의 배열(다른 항목은 건너뛴다).
     *
     * @param options the dictionary or any value / 사전 또는 아무 값
     * @return the ranges / 범위
     */
    static List<StaticRange> readTargetRanges(JsValue options) {
        List<StaticRange> out = new ArrayList<StaticRange>();
        if (!options.isObject()) {
            return out;
        }
        JsValue v = ((JsObject) options).get("targetRanges");
        if (!v.isObject()) {
            return out;
        }
        JsObject array = (JsObject) v;
        long length = (long) Ops.toNumber(array.get("length"));
        // 배열의 항목을 하나씩 읽어 정적 범위만 모은다.
        for (long i = 0; i < length && i < 1000; i++) {
            StaticRange sr = (StaticRange) DomBridge.targetOf(array.get(String.valueOf(i)), StaticRange.class);
            if (sr != null) {
                out.add(sr);
            }
        }
        return out;
    }

    /**
     * Reads the <code>dataTransfer</code> member of an <code>InputEventInit</code> dictionary.
     * <p>
     * <code>InputEventInit</code> 사전의 <code>dataTransfer</code> 멤버를 읽는다.
     *
     * @param options the dictionary or any value / 사전 또는 아무 값
     * @return the data or null / 데이터. 없으면 null
     */
    static ClipboardData readTransfer(JsValue options) {
        if (!options.isObject()) {
            return null;
        }
        return (ClipboardData) DomBridge.targetOf(((JsObject) options).get("dataTransfer"), ClipboardData.class);
    }
}
