package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredCloneException;

/**
 * The shared steps of posting a message between script contexts: reading the transfer list (ArrayBuffers and <code>MessagePort</code>
 * objects), serializing the message with the structured clone algorithm in the realm of the sender (so a failure is a
 * <code>DataCloneError</code> thrown at the caller of <code>postMessage</code>), and, on the receiving side, turning the serialized message
 * into a <code>MessageEvent</code> of the realm of the receiver, with the ports that came with it. A message never carries a script object
 * from one realm to another: only the immutable serialized tree crosses, and a transferred port crosses as the end of its channel.
 * <p>
 * 스크립트 문맥 사이에서 메시지를 보내는 공통 단계: 전달 목록(ArrayBuffer 와 <code>MessagePort</code> 객체) 읽기, 보내는 쪽 realm 에서 구조화 복제
 * 알고리즘으로 메시지 직렬화(실패는 <code>postMessage</code> 호출자에게 던져지는 <code>DataCloneError</code>), 받는 쪽에서는 직렬화된 메시지를 함께 온 포트와
 * 더불어 받는 쪽 realm 의 <code>MessageEvent</code> 로 바꾸기. 메시지는 스크립트 객체를 한 realm 에서 다른 realm 으로 나르지 않는다. 불변의 직렬화된 트리만
 * 건너가고, 전달된 포트는 채널의 끝으로 건너간다.
 */
final class Messages {

    /**
     * Private constructor: static helpers.
     * <p>
     * 비공개 생성자: 정적 도우미.
     */
    private Messages() {
    }

    /**
     * Creates the <code>DataCloneError</code> exception of a context.
     * <p>
     * 문맥의 <code>DataCloneError</code> 예외를 만든다.
     *
     * @param ctx     the sending context / 보내는 문맥
     * @param message what failed / 실패한 것
     * @return the exception to throw / 던질 예외
     */
    static JsException cloneError(ScriptContext ctx, String message) {
        return ctx.getBridge().domError(new DomException("DataCloneError", DomException.DATA_CLONE_ERR, message));
    }

    /**
     * Reads the transfer list of a <code>postMessage</code> call: the array-like argument, or the <code>transfer</code> member of an options
     * object. ArrayBuffers and <code>MessagePort</code> objects can be transferred; a duplicate entry or another object is a
     * <code>DataCloneError</code>.
     * <p>
     * <code>postMessage</code> 호출의 전달 목록을 읽는다: 배열 형태 인수 또는 옵션 객체의 <code>transfer</code> 멤버. ArrayBuffer 와
     * <code>MessagePort</code> 객체를 전달할 수 있으며 중복 항목이나 다른 객체는 <code>DataCloneError</code> 이다.
     *
     * @param ctx the sending context / 보내는 문맥
     * @param arg the argument (may be undefined) / 인수(undefined 일 수 있다)
     * @return the objects to transfer, null for none / 전달할 객체. 없으면 null
     */
    static List<JsObject> transferList(ScriptContext ctx, JsValue arg) {
        JsValue list = arg;
        if (list.isObject() && !((JsObject) list).isArray()) {
            list = ((JsObject) list).get("transfer");
        }
        if (list.isNullOrUndefined()) {
            return null;
        }
        if (!list.isObject()) {
            throw JsException.typeError("The transfer list is not an object");
        }
        JsValue[] items = Ops.createListFromArrayLike(list);
        List<JsObject> out = new ArrayList<JsObject>();
        IdentityHashMap<JsObject, Boolean> seen = new IdentityHashMap<JsObject, Boolean>();
        // 전달 목록의 항목은 중복 없는 ArrayBuffer 이거나 MessagePort 여야 한다.
        for (int i = 0; i < items.length; i++) {
            boolean port = DomBridge.targetOf(items[i], PortTarget.class) != null;
            if (!items[i].isObject() || !(port || items[i] instanceof ArrayBufferObject)) {
                throw cloneError(ctx, "Value not transferable.");
            }
            JsObject o = (JsObject) items[i];
            if (seen.containsKey(o)) {
                throw cloneError(ctx, "Transfer list contains duplicate entries.");
            }
            seen.put(o, Boolean.TRUE);
            out.add(o);
        }
        return out;
    }

    /**
     * Serializes a message with a transfer list that was read already, in the realm of the sender; a failure is a
     * <code>DataCloneError</code>. The buffers and ports of the transfer list are given up on success.
     * <p>
     * 이미 읽은 전달 목록으로 보내는 쪽 realm 에서 메시지를 직렬화한다. 실패는 <code>DataCloneError</code> 이다. 성공하면 전달 목록의 버퍼와 포트를 내놓는다.
     *
     * @param ctx      the sending context / 보내는 문맥
     * @param message  the message / 메시지
     * @param transfer the transfer list from {@link #transferList(ScriptContext, JsValue)}, or null / {@link #transferList(ScriptContext, JsValue)} 의 전달 목록. 없으면 null
     * @return the serialized message / 직렬화된 메시지
     */
    static ClonedValue serializeWith(ScriptContext ctx, JsValue message, List<JsObject> transfer) {
        try {
            return StructuredClone.serializeForMessaging(ctx.getRealm(), message, StructuredClone.DEFAULT_MAX_BYTES, transfer, new PortTransfer(ctx));
        } catch (StructuredCloneException e) {
            throw cloneError(ctx, e.getMessage());
        }
    }

    /**
     * Serializes a message in the realm of the sender; a failure is a <code>DataCloneError</code>. The buffers and ports of the transfer list
     * are given up on success.
     * <p>
     * 보내는 쪽 realm 에서 메시지를 직렬화한다. 실패는 <code>DataCloneError</code> 이다. 성공하면 전달 목록의 버퍼와 포트를 내놓는다.
     *
     * @param ctx         the sending context / 보내는 문맥
     * @param message     the message / 메시지
     * @param transferArg the transfer argument / 전달 인수
     * @return the serialized message / 직렬화된 메시지
     */
    static ClonedValue serialize(ScriptContext ctx, JsValue message, JsValue transferArg) {
        return serializeWith(ctx, message, transferList(ctx, transferArg));
    }

    /**
     * Creates the value of a serialized message in the realm of a context, together with the ports that it carries (call it inside a task of
     * that context).
     * <p>
     * 직렬화된 메시지의 값을, 함께 온 포트와 더불어 문맥의 realm 에 만든다(그 문맥의 작업 안에서 부른다).
     *
     * @param ctx   the receiving context / 받는 문맥
     * @param data  the serialized message / 직렬화된 메시지
     * @param ports a list that receives the ports in the order of the transfer list, or null / 포트를 전달 목록 순서로 받는 목록. 없으면 null
     * @return the value / 값
     */
    static JsValue deserialize(ScriptContext ctx, ClonedValue data, List<JsObject> ports) {
        return StructuredClone.deserializeForMessaging(ctx.getRealm(), data, new PortTransfer(ctx), ports);
    }

    /**
     * Creates a message event or a messageerror when the receiving realm cannot reconstitute its data.
     * Call this inside a task of the receiving context.
     * <p>
     * message 이벤트를 만들거나 수신 realm이 데이터를 복원할 수 없으면 messageerror를 만든다.
     * 수신 문맥의 작업 안에서 호출한다.
     *
     * @param ctx    the receiving context / 받는 문맥
     * @param data   the serialized message / 직렬화된 메시지
     * @param origin the origin of the sender, empty if there is none / 보낸 쪽의 오리진. 없으면 빈 문자열
     * @param source the source object of the event, or null / 이벤트의 소스 객체. 없으면 null
     * @return the event for the receiver / 수신자에게 전달할 이벤트
     */
    static MessageEvent event(ScriptContext ctx, ClonedValue data, String origin, Object source) {
        List<JsObject> ports = new ArrayList<JsObject>();
        try {
            JsValue value = deserialize(ctx, data, ports);
            MessageEvent event = new MessageEvent("message", false, false);
            event.setContent(value, origin, "", source, ports);
            return event;
        } catch (StructuredCloneException e) {
            // 역직렬화를 거부한 메시지의 데이터와 전달 포트는 수신자에 노출하지 않는다.
            MessageEvent event = new MessageEvent("messageerror", false, false);
            event.setContent(Js.NULL, origin, "", source, null);
            return event;
        }
    }

    /**
     * Creates the <code>message</code> event of a received message in the realm of the receiver (call it inside a task of the receiver).
     * <p>
     * 받은 메시지의 <code>message</code> 이벤트를 받는 쪽 realm 에 만든다(받는 쪽의 작업 안에서 부른다).
     *
     * @param ctx    the receiving context / 받는 문맥
     * @param data   the serialized message / 직렬화된 메시지
     * @param origin the origin of the sender, empty if there is none / 보낸 쪽의 오리진. 없으면 빈 문자열
     * @return the event / 이벤트
     */
    static MessageEvent event(ScriptContext ctx, ClonedValue data, String origin) {
        return event(ctx, data, origin, null);
    }
}
