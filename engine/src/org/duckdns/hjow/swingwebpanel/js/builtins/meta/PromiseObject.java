package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * A Promise instance (ECMA-262 6th edition 25.4): the internal slots [[PromiseState]], [[PromiseResult]],
 * [[PromiseFulfillReactions]], [[PromiseRejectReactions]] and [[PromiseIsHandled]] (the last one comes from the host rejection tracker
 * model of later editions and drives {@link PromiseRejectionTracker}). Only the promise built-ins change the state; hosts and tests read it
 * with the getters.
 * <p>
 * Promise 인스턴스(ECMA-262 6판 25.4): 내부 슬롯 [[PromiseState]], [[PromiseResult]], [[PromiseFulfillReactions]],
 * [[PromiseRejectReactions]], [[PromiseIsHandled]]([[PromiseIsHandled]] 는 후속 판의 호스트 거부 추적 모델에서 온 것으로
 * {@link PromiseRejectionTracker} 를 움직인다). promise 내장 객체만 상태를 바꾸며 호스트와 시험은 getter 로 읽는다.
 */
public final class PromiseObject extends JsObject {

    /**
     * State: not settled yet.
     * <p>
     * 상태: 아직 확정되지 않음.
     */
    public static final int PENDING = 0;

    /**
     * State: fulfilled with a value.
     * <p>
     * 상태: 값으로 이행됨.
     */
    public static final int FULFILLED = 1;

    /**
     * State: rejected with a reason.
     * <p>
     * 상태: 이유와 함께 거부됨.
     */
    public static final int REJECTED = 2;

    /**
     * The [[PromiseState]] slot.
     * <p>
     * [[PromiseState]] 슬롯.
     */
    private int state = PENDING;

    /**
     * The [[PromiseResult]] slot.
     * <p>
     * [[PromiseResult]] 슬롯.
     */
    private JsValue result = Js.UNDEFINED;

    /**
     * The [[PromiseFulfillReactions]] slot (null once settled).
     * <p>
     * [[PromiseFulfillReactions]] 슬롯(확정되면 null).
     */
    private List<PromiseReaction> fulfillReactions = new ArrayList<PromiseReaction>();

    /**
     * The [[PromiseRejectReactions]] slot (null once settled).
     * <p>
     * [[PromiseRejectReactions]] 슬롯(확정되면 null).
     */
    private List<PromiseReaction> rejectReactions = new ArrayList<PromiseReaction>();

    /**
     * True once a handler was added with then.
     * <p>
     * then 으로 처리기가 추가되면 true.
     */
    private boolean handled;

    /**
     * Creates a pending promise.
     * <p>
     * 대기 중인 promise 를 만든다.
     *
     * @param proto the prototype / 프로토타입
     */
    PromiseObject(JsObject proto) {
        super(proto);
    }

    /**
     * Returns the state: {@link #PENDING}, {@link #FULFILLED} or {@link #REJECTED}.
     * <p>
     * 상태를 반환한다: {@link #PENDING}, {@link #FULFILLED}, {@link #REJECTED}.
     *
     * @return the state / 상태
     */
    public int getState() {
        return state;
    }

    /**
     * Returns the fulfillment value or rejection reason (undefined while pending).
     * <p>
     * 이행 값 또는 거부 이유를 반환한다(대기 중에는 undefined).
     *
     * @return the result / 결과
     */
    public JsValue getResult() {
        return result;
    }

    /**
     * Tells whether a handler was ever added with then.
     * <p>
     * then 으로 처리기가 추가된 적이 있는지 알려 준다.
     *
     * @return true if handled / 처리기가 있으면 true
     */
    public boolean isHandled() {
        return handled;
    }

    /**
     * Marks the promise as handled.
     * <p>
     * promise 를 처리됨으로 표시한다.
     */
    void markHandled() {
        handled = true;
    }

    /**
     * Returns the reactions to run on fulfillment (only while pending).
     * <p>
     * 이행 시 실행할 반응 목록을 반환한다(대기 중에만).
     *
     * @return the list / 목록
     */
    List<PromiseReaction> fulfillReactions() {
        return fulfillReactions;
    }

    /**
     * Returns the reactions to run on rejection (only while pending).
     * <p>
     * 거부 시 실행할 반응 목록을 반환한다(대기 중에만).
     *
     * @return the list / 목록
     */
    List<PromiseReaction> rejectReactions() {
        return rejectReactions;
    }

    /**
     * Settles the promise and drops the reaction lists; the caller schedules the reactions.
     * <p>
     * promise 를 확정하고 반응 목록을 버린다. 반응의 예약은 호출자가 한다.
     *
     * @param newState FULFILLED or REJECTED / FULFILLED 또는 REJECTED
     * @param value    the value or reason / 값 또는 이유
     */
    void settle(int newState, JsValue value) {
        this.state = newState;
        this.result = value;
        this.fulfillReactions = null;
        this.rejectReactions = null;
    }
}
