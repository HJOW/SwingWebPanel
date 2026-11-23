package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredCloneException;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The <code>history</code> object of a page (HTML5 2014 REC 5.5.2, the <code>History</code> interface): <code>length</code>,
 * <code>state</code>, <code>go</code>, <code>back</code>, <code>forward</code>, <code>pushState</code>, <code>replaceState</code> and the
 * <code>scrollRestoration</code> attribute of the living standard. <code>pushState</code> and <code>replaceState</code> make a structured
 * clone of the state object (a value that cannot be cloned raises a <code>DataCloneError</code>), accept a URL of the same document origin only
 * (otherwise a <code>SecurityError</code>), change the document URL and <code>history.state</code> at once, and ask the component to give the
 * history entry of the page the new URL and state; nothing is requested from the network and no <code>popstate</code> or
 * <code>hashchange</code> event is fired for such a call. The state of the current entry is also what the component restores when the entry
 * is visited again ({@link PageScripts#historyTraversed}).
 * <p>
 * 페이지의 <code>history</code> 객체(HTML5 2014 REC 5.5.2, <code>History</code> 인터페이스): <code>length</code>, <code>state</code>,
 * <code>go</code>, <code>back</code>, <code>forward</code>, <code>pushState</code>, <code>replaceState</code> 와 현행 표준의
 * <code>scrollRestoration</code> 속성. <code>pushState</code> 와 <code>replaceState</code> 는 상태 객체를 구조화 복제하고(복제할 수 없는 값은
 * <code>DataCloneError</code>), 같은 문서 오리진의 URL만 받으며(아니면 <code>SecurityError</code>), 문서 URL과 <code>history.state</code> 를 곧바로
 * 바꾸고, 컴포넌트에 페이지의 이력 항목이 새 URL과 상태를 갖도록 요청한다. 네트워크 요청은 없으며 이런 호출에는 <code>popstate</code> 와
 * <code>hashchange</code> 이벤트가 발생하지 않는다. 현재 항목의 상태는 그 항목을 다시 방문할 때 컴포넌트가 복원하는 값이기도 하다
 * ({@link PageScripts#historyTraversed}).
 */
final class HistoryApi {

    /**
     * The largest estimated size of one state object in bytes (the state stays in the history entry as long as the entry lives).
     * <p>
     * 상태 객체 하나의 최대 추정 크기(바이트). 상태는 항목이 살아 있는 동안 이력 항목에 남는다.
     */
    static final long MAX_STATE_BYTES = 8L * 1024L * 1024L;

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The serialized state of the current entry, or null.
     * <p>
     * 현재 항목의 직렬화된 상태. 없으면 null.
     */
    private ClonedValue state;

    /**
     * The value that <code>history.state</code> returns (made from {@link #state} when first read, so that the same object is returned each
     * time), or null while not yet made.
     * <p>
     * <code>history.state</code> 가 반환하는 값({@link #state} 로 처음 읽을 때 만들어 매번 같은 객체를 돌려준다). 아직 만들지 않았으면 null.
     */
    private JsValue stateValue;

    /**
     * The value of <code>history.scrollRestoration</code>.
     * <p>
     * <code>history.scrollRestoration</code> 의 값.
     */
    private String scrollRestoration = "auto";

    /**
     * Creates the object.
     * <p>
     * 객체를 만든다.
     *
     * @param page the page / 페이지
     */
    HistoryApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Makes another serialized value the state of the current entry (a traversal to another entry, or the first state of a document that was
     * loaded again for an entry).
     * <p>
     * 다른 직렬화된 값을 현재 항목의 상태로 삼는다(다른 항목으로의 이동, 또는 항목을 위해 다시 읽은 문서의 첫 상태).
     *
     * @param value the serialized state or null / 직렬화된 상태 또는 null
     */
    void setState(ClonedValue value) {
        this.state = value;
        this.stateValue = null;
    }

    /**
     * Returns the value of <code>history.state</code>: null without a state, otherwise the copy that was made when the state was first read.
     * Call on the thread that owns the realm.
     * <p>
     * <code>history.state</code> 의 값을 반환한다: 상태가 없으면 null, 있으면 처음 읽을 때 만든 복사본. realm 을 소유한 스레드에서 호출해야 한다.
     *
     * @return the state value / 상태 값
     */
    JsValue stateValue() {
        if (stateValue == null) {
            stateValue = state == null ? Js.NULL : StructuredClone.deserialize(page.getRealm(), state);
        }
        return stateValue;
    }

    /**
     * Defines a method of the history object.
     * <p>
     * history 객체의 메서드를 정의한다.
     *
     * @param h      the history object / history 객체
     * @param name   the name / 이름
     * @param length the declared length / 선언된 길이
     * @param call   the behaviour / 동작
     */
    private void method(JsObject h, String name, int length, NativeFunction.Call call) {
        h.defineData(name, page.getRealm().newFunction(name, length, call), true, true, true);
    }

    /**
     * Installs <code>history</code> on the global object.
     * <p>
     * 전역 객체에 <code>history</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        final HostEnvironment env = page.getEnvironment();
        JsObject h = realm.newObject();
        h.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("History"), false, false, true);
        h.defineAccessor("length", realm.newFunction("get length", 0, new NativeFunction.Call() {
            /**
             * Returns the number of entries of the session history.
             * <p>
             * 세션 이력의 항목 수를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the number / 개수
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getHistoryLength());
            }
        }), null, true, true);
        h.defineAccessor("state", realm.newFunction("get state", 0, new NativeFunction.Call() {
            /**
             * Returns the state object of the current entry.
             * <p>
             * 현재 항목의 상태 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return stateValue();
            }
        }), null, true, true);
        h.defineAccessor("scrollRestoration", realm.newFunction("get scrollRestoration", 0, new NativeFunction.Call() {
            /**
             * Returns <code>auto</code> or <code>manual</code>.
             * <p>
             * <code>auto</code> 또는 <code>manual</code> 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the mode / 방식
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(scrollRestoration);
            }
        }), realm.newFunction("set scrollRestoration", 1, new NativeFunction.Call() {
            /**
             * Sets the mode; a value that is not one of the two is ignored.
             * <p>
             * 방식을 지정한다. 둘 중 하나가 아닌 값은 무시한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String v = Ops.toString(Js.arg(args, 0));
                if ("auto".equals(v) || "manual".equals(v)) {
                    scrollRestoration = v;
                }
                return Js.UNDEFINED;
            }
        }), true, true);
        method(h, "back", 0, new NativeFunction.Call() {
            /**
             * Moves one entry back.
             * <p>
             * 한 칸 뒤로 간다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                env.historyGo(-1);
                return Js.UNDEFINED;
            }
        });
        method(h, "forward", 0, new NativeFunction.Call() {
            /**
             * Moves one entry forward.
             * <p>
             * 한 칸 앞으로 간다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                env.historyGo(1);
                return Js.UNDEFINED;
            }
        });
        method(h, "go", 0, new NativeFunction.Call() {
            /**
             * Moves a number of entries (0 or no argument reloads the page).
             * <p>
             * 여러 칸 이동한다(0 이나 인수가 없으면 페이지를 새로고침한다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                double d = Ops.toInteger(Js.arg(args, 0));
                int n = (int) Math.max(-1000000000.0, Math.min(1000000000.0, d));
                if (n == 0) {
                    env.reload();
                } else {
                    env.historyGo(n);
                }
                return Js.UNDEFINED;
            }
        });
        method(h, "pushState", 2, new NativeFunction.Call() {
            /**
             * Adds an entry with a state object.
             * <p>
             * 상태 객체가 있는 항목을 추가한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                update(args, false);
                return Js.UNDEFINED;
            }
        });
        method(h, "replaceState", 2, new NativeFunction.Call() {
            /**
             * Gives the current entry a new state object.
             * <p>
             * 현재 항목에 새 상태 객체를 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                update(args, true);
                return Js.UNDEFINED;
            }
        });
        g.defineData("history", h, true, true, true);
    }

    /**
     * The algorithm of <code>pushState</code> and <code>replaceState</code> (HTML5 2014 REC 5.5.2): clone the state, check the URL, change
     * the document URL and <code>history.state</code>, and ask the component to change the history entry.
     * <p>
     * <code>pushState</code>, <code>replaceState</code> 의 알고리즘(HTML5 2014 REC 5.5.2): 상태를 복제하고, URL을 검사하고, 문서 URL과
     * <code>history.state</code> 를 바꾸고, 컴포넌트에 이력 항목을 바꾸라고 요청한다.
     *
     * @param args    the arguments data, title and optional url / 인수 data, title, 선택적 url
     * @param replace true for <code>replaceState</code> / <code>replaceState</code> 이면 true
     */
    private void update(JsValue[] args, boolean replace) {
        String method = replace ? "replaceState" : "pushState";
        if (args.length < 2) {
            throw JsException.typeError("Failed to execute '" + method + "' on 'History': 2 arguments required, but only " + args.length
                    + " present.");
        }
        // WebIDL 인수 변환: title 은 문자열로 바꾸되 쓰이지 않고, url 은 undefined 와 null 이면 없는 것이다.
        Ops.toString(args[1]);
        JsValue u = Js.arg(args, 2);
        String urlText = u.isNullOrUndefined() ? null : Ops.toString(u);
        ClonedValue cloned;
        try {
            cloned = StructuredClone.serialize(page.getRealm(), args[0], MAX_STATE_BYTES, null);
        } catch (StructuredCloneException e) {
            throw page.getBridge().domError(new DomException(e.getName(), DomException.DATA_CLONE_ERR, e.getMessage()));
        }
        Document doc = page.getDocument();
        String target = urlText == null ? doc.getUrl() : checkedUrl(doc, urlText);
        doc.setUrl(target);
        this.state = cloned;
        this.stateValue = null;
        page.getEnvironment().updateHistoryState(cloned, target, replace);
    }

    /**
     * Resolves the URL argument against the document base URL and checks that only its path, query and fragment differ from the document URL
     * (scheme, user information, host and port must be equal), as the HTML Standard requires.
     * <p>
     * URL 인수를 문서 기준 URL로 해석하고, 문서 URL과 경로, 질의, 프래그먼트만 다른지(스킴, 사용자 정보, 호스트, 포트는 같아야 한다) HTML 표준대로 검사한다.
     *
     * @param doc  the document / 문서
     * @param text the URL argument / URL 인수
     * @return the canonical absolute URL / 정규화한 절대 URL
     * @throws JsException a <code>SecurityError</code> DOMException / <code>SecurityError</code> DOMException
     */
    private String checkedUrl(Document doc, String text) {
        UrlUtil.Parts wanted;
        UrlUtil.Parts current;
        String absolute;
        try {
            absolute = UrlUtil.resolve(doc.getBaseUri(), text);
            wanted = UrlUtil.parse(absolute);
            current = UrlUtil.parse(doc.getUrl());
        } catch (RuntimeException e) {
            throw page.getBridge().domError(DomException.security("Failed to parse the URL '" + text + "'."));
        }
        boolean same = equal(wanted.scheme, current.scheme) && equal(wanted.userInfo, current.userInfo) && equal(wanted.host, current.host)
                && UrlUtil.effectivePort(wanted) == UrlUtil.effectivePort(current);
        if (!same) {
            throw page.getBridge().domError(DomException.security("A history state object with URL '" + absolute
                    + "' cannot be created in a document with URL '" + doc.getUrl() + "'."));
        }
        String normalized = UrlUtil.normalize(absolute);
        return normalized == null ? absolute : normalized;
    }

    /**
     * Compares two strings that may be null.
     * <p>
     * null 일 수 있는 두 문자열을 비교한다.
     *
     * @param a the first / 첫째
     * @param b the second / 둘째
     * @return true if equal / 같으면 true
     */
    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
