package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.JsValue;

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
     * The handler: a callable, or undefined for the identity (fulfill) / thrower (reject) default.
     * <p>
     * 처리기: 호출 가능 값. undefined 이면 기본 동작(이행은 그대로 전달, 거부는 다시 던짐).
     */
    final JsValue handler;

    /**
     * Creates a reaction.
     * <p>
     * 반응을 만든다.
     *
     * @param capability derived promise capability or null / 파생 promise capability 또는 null
     * @param fulfill    true for fulfill / 이행이면 true
     * @param handler    handler or undefined / 처리기 또는 undefined
     */
    PromiseReaction(PromiseCapability capability, boolean fulfill, JsValue handler) {
        this.capability = capability;
        this.fulfill = fulfill;
        this.handler = handler;
    }
}
