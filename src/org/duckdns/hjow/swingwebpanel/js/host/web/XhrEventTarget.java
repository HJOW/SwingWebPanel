package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;

/**
 * The Java side of <code>XMLHttpRequestEventTarget</code>: the common base of an <code>XMLHttpRequest</code> and its
 * <code>XMLHttpRequestUpload</code> object. It adds nothing to {@link EventTarget} except the shared type that the
 * <code>on...</code> event handler properties (<code>onload</code>, <code>onerror</code> ...) are defined for.
 * <p>
 * <code>XMLHttpRequestEventTarget</code> 의 Java 쪽: <code>XMLHttpRequest</code> 와 그 <code>XMLHttpRequestUpload</code> 객체의 공통 기반. <code>on...</code>
 * 이벤트 핸들러 프로퍼티(<code>onload</code>, <code>onerror</code> 등)를 정의하는 공유 형식 말고는 {@link EventTarget} 에 더하는 것이 없다.
 */
abstract class XhrEventTarget extends EventTarget {

    /**
     * Creates the target.
     * <p>
     * 대상을 만든다.
     */
    XhrEventTarget() {
        super();
    }
}
