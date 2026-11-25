package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

/**
 * A PromiseReaction record (ECMA-262 6th edition 25.4.1.2): what to do with the result of a promise for one registration made with
 * then: the capability of the derived promise, the kind (fulfill or reject) and the handler.
 * <p>
 * PromiseReaction 기록(ECMA-262 6판 25.4.1.2): then 으로 한 등록 하나에 대해 promise 의 결과로 할 일. 파생 promise 의 capability,
 * 종류(이행 또는 거부), 처리기.
 */
final class PromiseReaction {

    /**
     * Capability of the derived promise, or null if there is none.
     * <p>
     * 파생 promise 의 capability. 없으면 null.
     */
    final PromiseCapability capability;

    /**
     * True for a fulfill reaction, false for a reject reaction.
     * <p>
     * 이행 반응이면 true, 거부 반응이면 false.
     */
    final boolean fulfill;

    /**
     * The job callback, or null for the identity (fulfill) / thrower (reject) default.
     * <p>
     * 작업 콜백. null이면 기본 동작(이행은 그대로 전달, 거부는 다시 던짐).
     */
    final PromiseJobCallback handler;

    /**
     * Creates a reaction.
     * <p>
     * 반응을 만든다.
     *
     * @param capability derived promise capability or null / 파생 promise capability 또는 null
     * @param fulfill    true for fulfill / 이행이면 true
     * @param handler    handler or null / 처리기 또는 null
     */
    PromiseReaction(PromiseCapability capability, boolean fulfill, PromiseJobCallback handler) {
        this.capability = capability;
        this.fulfill = fulfill;
        this.handler = handler;
    }
}
