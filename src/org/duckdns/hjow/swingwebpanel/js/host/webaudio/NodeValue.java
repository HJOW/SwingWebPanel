package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioNode;

/**
 * The script side of an <code>AudioNode</code> (Web Audio API 1.5): an <code>EventTarget</code> (the <code>ended</code> event of a source node and the events
 * of the nodes that the scripting stage adds are dispatched at it) that holds the engine node and the little state that only the script interface has: the
 * buffer object that the script assigned (a buffer source and a convolver return <em>that</em> object from their <code>buffer</code> attribute, although the engine
 * plays a snapshot), the curve array of a wave shaper, and the flags of the "set once" rules. The wrapper of the interface is created for this object by the
 * DOM bridge, so the same node always appears as the same script object while the page holds it.
 * <p>
 * <code>AudioNode</code> 의 스크립트 쪽(Web Audio API 1.5): <code>EventTarget</code>(원천 노드의 <code>ended</code> 이벤트와 스크립팅 단계가 더하는 노드의 이벤트가 이것에
 * 전달된다)이며 엔진 노드와 스크립트 인터페이스에만 있는 약간의 상태를 가진다: 스크립트가 대입한 버퍼 객체(버퍼 원천과 컨볼버는 엔진이 스냅샷을 재생하더라도
 * <code>buffer</code> 속성에서 <em>그</em> 객체를 반환한다), 웨이브셰이퍼의 곡선 배열, "한 번만 정한다" 규칙의 표시. 인터페이스의 래퍼는 DOM 브리지가 이 객체를
 * 위해 만들므로 같은 노드는 페이지가 붙잡고 있는 동안 언제나 같은 스크립트 객체로 보인다.
 */
final class NodeValue extends EventTarget {

    /**
     * The context that the node belongs to.
     * <p>
     * 노드가 속한 컨텍스트.
     */
    private final ContextValue context;

    /**
     * The engine node.
     * <p>
     * 엔진 노드.
     */
    private final AudioNode node;

    /**
     * The buffer that the script assigned to <code>buffer</code> (a buffer source, a convolver), or null.
     * <p>
     * 스크립트가 <code>buffer</code> 에 대입한 버퍼(버퍼 원천, 컨볼버). 없으면 null.
     */
    AudioBufferValue assignedBuffer;

    /**
     * True after a buffer source was assigned a buffer that is not null (a buffer can be assigned once, the rule <code>[[buffer set]]</code>).
     * <p>
     * 버퍼 원천에 null 이 아닌 버퍼를 대입한 뒤 true(버퍼는 한 번만 대입할 수 있다. <code>[[buffer set]]</code> 규칙).
     */
    boolean bufferSet;

    /**
     * The array that the script assigned to the <code>curve</code> of a wave shaper, or null.
     * <p>
     * 스크립트가 웨이브셰이퍼의 <code>curve</code> 에 대입한 배열. 없으면 null.
     */
    JsObject assignedCurve;

    /**
     * The media element of a <code>MediaElementAudioSourceNode</code> (the script object that was passed to <code>createMediaElementSource</code>), or null; the node holds the element,
     * as the <code>mediaElement</code> attribute shows it.
     * <p>
     * <code>MediaElementAudioSourceNode</code> 의 미디어 요소(<code>createMediaElementSource</code> 에 넘긴 스크립트 객체). 없으면 null. <code>mediaElement</code> 속성이 보여 주듯
     * 노드가 요소를 붙잡는다.
     */
    JsObject mediaElement;

    /**
     * Creates the value of a node.
     * <p>
     * 노드의 값을 만든다.
     *
     * @param context the context / 컨텍스트
     * @param node    the engine node / 엔진 노드
     */
    NodeValue(ContextValue context, AudioNode node) {
        this.context = context;
        this.node = node;
    }

    /**
     * Returns the context of the node.
     * <p>
     * 노드의 컨텍스트를 반환한다.
     *
     * @return the context / 컨텍스트
     */
    ContextValue context() {
        return context;
    }

    /**
     * Returns the engine node.
     * <p>
     * 엔진 노드를 반환한다.
     *
     * @return the node / 노드
     */
    AudioNode node() {
        return node;
    }
}
