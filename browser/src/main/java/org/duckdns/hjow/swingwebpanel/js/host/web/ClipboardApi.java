package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The secure Window clipboard interface for plain text. An input task grants writing, and an explicit host paste grants reading.
 * <p>
 * 보안 Window의 일반 텍스트 클립보드 인터페이스. 입력 작업에서 쓰기를 허용하며 호스트의 명시적인 붙여넣기 입력에서 읽기를 허용한다.
 */
public final class ClipboardApi {
    /** The event target associated with the document's navigator. / 문서의 navigator에 속한 이벤트 대상. */
    private static final class ClipboardTarget extends EventTarget {
        /** Creates the navigator's clipboard target. / navigator의 클립보드 대상을 만든다. */
        ClipboardTarget() {
        }
    }

    /** The owning page. / 소유 페이지. */
    private final PageScripts page;
    /** Realm in which the clipboard promises settle. / 클립보드 프라미스를 확정하는 렐름. */
    private final JsRealm realm;
    /** Object exposed as navigator.clipboard. / navigator.clipboard로 노출하는 객체. */
    private final JsValue clipboard;

    /**
     * Installs the Clipboard interface and creates one instance for the page.
     * <p>
     * Clipboard 인터페이스를 설치하고 페이지 인스턴스 하나를 만든다.
     *
     * @param page secure page / 보안 페이지
     */
    private ClipboardApi(PageScripts page) {
        this.page = page;
        realm = page.getRealm();
        DomBridge bridge = page.getBridge();
        bridge.addClassifier(new DomBridge.Classifier() {
            /** Selects the Clipboard interface for its event target. / 이벤트 대상의 Clipboard 인터페이스를 선택한다. */
            public String classify(Object target) {
                return target instanceof ClipboardTarget ? "Clipboard" : null;
            }
        });
        JsObject prototype = bridge.defineInterface("Clipboard", "EventTarget", ClipboardTarget.class, new String[0]).getPrototype();
        Natives.method(realm, prototype, "readText", 0, this, "readText");
        Natives.method(realm, prototype, "writeText", 1, this, "writeText");
        clipboard = bridge.wrap(new ClipboardTarget());
    }

    /**
     * Exposes the clipboard on a navigator in a secure context.
     * <p>
     * 보안 컨텍스트의 navigator에 클립보드를 노출한다.
     *
     * @param page secure page / 보안 페이지
     * @param navigator the page navigator / 페이지 navigator
     */
    public static void install(PageScripts page, JsObject navigator) {
        ClipboardApi api = new ClipboardApi(page);
        navigator.defineData("clipboard", api.clipboard, false, true, true);
    }

    /**
     * Implements clipboard.readText for the current paste action.
     * <p>
     * 현재 붙여넣기 동작에서 clipboard.readText를 구현한다.
     *
     * @param self receiver / 수신 객체
     * @param args unused arguments / 사용하지 않는 인수
     * @return a text promise / 텍스트 프라미스
     */
    JsValue readText(JsValue self, JsValue[] args) {
        return self == clipboard ? request(null) : Promises.rejectedTypeError(realm, "Invalid Clipboard receiver");
    }

    /**
     * Implements clipboard.writeText for the current trusted input.
     * <p>
     * 현재 신뢰 입력에서 clipboard.writeText를 구현한다.
     *
     * @param self receiver / 수신 객체
     * @param args text argument / 텍스트 인수
     * @return a completion promise / 완료 프라미스
     */
    JsValue writeText(JsValue self, JsValue[] args) {
        if (self != clipboard) {
            return Promises.rejectedTypeError(realm, "Invalid Clipboard receiver");
        }
        try {
            Natives.requireArgs(args, 1, "writeText", "Clipboard");
            return request(Ops.toString(args[0]));
        } catch (JsException e) {
            return Promises.rejected(realm, e.getValue(realm));
        }
    }

    /**
     * Submits a plain text operation to the host if the current input and containing origins allow it.
     * <p>
     * 현재 입력과 상위 오리진이 허용하면 일반 텍스트 동작을 호스트에 요청한다.
     *
     * @param write text to write, or null to read / 쓸 텍스트. 읽으려면 null
     * @return a completion promise / 완료 프라미스
     */
    private JsValue request(final String write) {
        if (!page.hasClipboardActivation(write == null) || !sameOriginAncestors()) {
            return Promises.rejected(realm, notAllowed("Clipboard access requires a trusted input in this document"));
        }
        final Promises.Deferred pending = Promises.create(realm);
        page.getEnvironment().requestClipboardText(write, new HostEnvironment.ClipboardResult() {
            /** Settles the operation in the caller's realm. / 호출자 렐름에서 동작을 확정한다. */
            public void complete(String text, boolean unavailable) {
                if (unavailable) {
                    pending.reject(notAllowed("The host clipboard is unavailable"));
                } else {
                    pending.resolve(write == null ? Js.str(text) : Js.UNDEFINED);
                }
            }
        });
        return pending.promise;
    }

    /**
     * Checks clipboard access under the default same-origin ancestor policy.
     * <p>
     * 기본 동일 오리진 상위 문서 정책으로 클립보드 접근을 검사한다.
     *
     * @return true for top-level and same-origin embedded documents / 최상위·동일 오리진 하위 문서이면 true
     */
    private boolean sameOriginAncestors() {
        Origin origin = Origin.of(page.getDocument().getOriginUrl());
        BrowsingContext context = page.getEnvironment().getBrowsingContext();
        // 교차 오리진 프레임에는 현재 컴포넌트의 클립보드 권한을 위임하지 않는다.
        for (BrowsingContext parent = context == null ? null : context.getParent(); parent != null; parent = parent.getParent()) {
            if (!origin.isSameOrigin(Origin.of(parent.getOriginUrl()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the denial exception in the owning realm.
     * <p>
     * 소유 렐름에서 거부 예외를 만든다.
     *
     * @param message denial reason / 거부 이유
     * @return a NotAllowedError DOMException / NotAllowedError DOMException
     */
    private JsValue notAllowed(String message) {
        return page.getBridge().domError(new DomException("NotAllowedError", (short) 0, message)).getValue(realm);
    }
}
