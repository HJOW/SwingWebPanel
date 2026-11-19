package org.duckdns.hjow.swingwebpanel.js.host;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.MediaQueryListEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomInterfaces;

/**
 * <code>window.matchMedia(query)</code> and <code>MediaQueryList</code> (CSSOM View Module, section 4.2; the media query grammar and its
 * evaluation are those of the CSS engine, {@link org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList}). A list reads <code>media</code>, the
 * <code>matches</code> of the query against the current viewport, and is an <code>EventTarget</code> with <code>onchange</code>,
 * <code>addEventListener("change", ...)</code> and the legacy <code>addListener</code>/<code>removeListener</code>. When the viewport of the
 * component changes ({@link #viewportChanged()}, called on the document queue) every list that is still reachable is evaluated again and a
 * <code>change</code> event (<code>MediaQueryListEvent</code> with <code>media</code> and <code>matches</code>) is fired at the lists whose
 * result changed. The lists are kept by weak references, so a list that a script dropped does not stay alive.
 * <p>
 * <code>window.matchMedia(query)</code> 와 <code>MediaQueryList</code>(CSSOM View Module 4.2절. 미디어 질의 문법과 평가는 CSS 엔진의
 * {@link org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList} 것을 쓴다). 목록은 <code>media</code> 와 현재 뷰포트에 대한 질의의 <code>matches</code> 를 읽고,
 * <code>onchange</code>, <code>addEventListener("change", ...)</code>, 옛 <code>addListener</code>/<code>removeListener</code> 가 있는
 * <code>EventTarget</code> 이다. 컴포넌트의 뷰포트가 바뀌면({@link #viewportChanged()}, 문서 큐에서 호출) 아직 접근할 수 있는 모든 목록을 다시
 * 평가하여 결과가 바뀐 목록에 <code>change</code> 이벤트(<code>media</code>, <code>matches</code> 가 있는 <code>MediaQueryListEvent</code>)를
 * 발생시킨다. 목록은 약한 참조로 보관하므로 스크립트가 버린 목록은 살아 있지 않는다.
 */
final class MediaQueries {

    /**
     * A media query list object: the query text and the result that was last reported.
     * <p>
     * 미디어 질의 목록 객체: 질의 문자열과 마지막으로 알린 결과.
     */
    public static final class Mql extends EventTarget {
        /**
         * The query text as the script passed it.
         * <p>
         * 스크립트가 넘긴 질의 문자열.
         */
        private final String media;

        /**
         * The result that the last evaluation reported (the base of the <code>change</code> decision).
         * <p>
         * 마지막 평가가 알린 결과(<code>change</code> 판단의 기준).
         */
        private boolean last;

        /**
         * Creates a list.
         * <p>
         * 목록을 만든다.
         *
         * @param media the query text / 질의 문자열
         * @param last  the result now / 지금의 결과
         */
        Mql(String media, boolean last) {
            this.media = media;
            this.last = last;
        }

        /**
         * Returns the query text (<code>MediaQueryList.media</code>).
         * <p>
         * 질의 문자열을 반환한다(<code>MediaQueryList.media</code>).
         *
         * @return the query / 질의
         */
        public String getMedia() {
            return media;
        }
    }

    /**
     * The page whose viewport is queried.
     * <p>
     * 뷰포트를 질의하는 페이지.
     */
    private final PageScripts page;

    /**
     * The lists that were created (weakly).
     * <p>
     * 만들어진 목록(약한 참조).
     */
    private final List<WeakReference<Mql>> lists = new ArrayList<WeakReference<Mql>>();

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    MediaQueries(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the list of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 목록을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the list / 목록
     */
    private static Mql receiver(JsValue self) {
        Mql m = (Mql) DomBridge.targetOf(self, Mql.class);
        if (m == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return m;
    }

    /**
     * Evaluates a query against the current viewport.
     * <p>
     * 질의를 현재 뷰포트에 대해 평가한다.
     *
     * @param query the query text / 질의 문자열
     * @return true if it matches / 일치하면 true
     */
    private boolean evaluate(String query) {
        return page.getEnvironment().matchesMedia(query);
    }

    /**
     * Installs <code>window.matchMedia</code> and the interfaces.
     * <p>
     * <code>window.matchMedia</code> 와 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a media query list.
             * <p>
             * 미디어 질의 목록의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return o instanceof Mql ? "MediaQueryList" : null;
            }
        });
        bridge.defineInterface("MediaQueryListEvent", "Event", MediaQueryListEvent.class, new String[] { "a:media", "a:matches" });
        DomBridge.InterfaceInfo info = bridge.defineInterface("MediaQueryList", "EventTarget", Mql.class, new String[] { "a:media" });
        bridge.getEventHandlers().define(info.getPrototype(), Mql.class, new String[] { "change" });
        info.getPrototype().defineAccessor("matches", realm.newFunction("get matches", 0, new NativeFunction.Call() {
            /**
             * Evaluates the query now (the value is live).
             * <p>
             * 질의를 지금 평가한다(값은 실시간이다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the result / 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(evaluate(receiver(self).media));
            }
        }), null, true, true);
        info.getPrototype().defineBuiltin("addListener", realm.newFunction("addListener", 1, new NativeFunction.Call() {
            /**
             * The legacy form of <code>addEventListener("change", callback)</code>.
             * <p>
             * <code>addEventListener("change", callback)</code> 의 옛 형태.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomInterfaces.addListener(bridge, receiver(self), new JsValue[] { Js.str("change"), Js.arg(args, 0) });
                return Js.UNDEFINED;
            }
        }));
        info.getPrototype().defineBuiltin("removeListener", realm.newFunction("removeListener", 1, new NativeFunction.Call() {
            /**
             * The legacy form of <code>removeEventListener("change", callback)</code>.
             * <p>
             * <code>removeEventListener("change", callback)</code> 의 옛 형태.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomInterfaces.removeListener(bridge, receiver(self), new JsValue[] { Js.str("change"), Js.arg(args, 0) });
                return Js.UNDEFINED;
            }
        }));
        g.defineBuiltin("matchMedia", realm.newFunction("matchMedia", 1, new NativeFunction.Call() {
            /**
             * Creates a media query list for a query.
             * <p>
             * 질의의 미디어 질의 목록을 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (the query) / 인수(질의)
             * @return the list / 목록
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'matchMedia' on 'Window': 1 argument required, but only 0 present.");
                }
                String query = org.duckdns.hjow.swingwebpanel.js.Ops.toString(args[0]);
                Mql m = new Mql(query, evaluate(query));
                synchronized (lists) {
                    // 사라진 목록의 참조를 걷어 내고 새 목록을 기록한다.
                    for (int i = lists.size() - 1; i >= 0; i--) {
                        if (lists.get(i).get() == null) {
                            lists.remove(i);
                        }
                    }
                    lists.add(new WeakReference<Mql>(m));
                }
                return bridge.toJs(m);
            }
        }));
    }

    /**
     * Evaluates every live list again and fires <code>change</code> at the lists whose result changed. Call it on the document queue after
     * the viewport changed.
     * <p>
     * 아직 살아 있는 모든 목록을 다시 평가하고 결과가 바뀐 목록에 <code>change</code> 를 발생시킨다. 뷰포트가 바뀐 뒤 문서 큐에서 호출한다.
     */
    void viewportChanged() {
        List<Mql> snapshot = new ArrayList<Mql>();
        synchronized (lists) {
            // 아직 수거되지 않은 목록만 모은다.
            for (int i = 0; i < lists.size(); i++) {
                Mql m = lists.get(i).get();
                if (m != null) {
                    snapshot.add(m);
                }
            }
        }
        // 목록마다 결과를 다시 평가해 바뀐 것에만 이벤트를 보낸다.
        for (int i = 0; i < snapshot.size(); i++) {
            Mql m = snapshot.get(i);
            boolean now = evaluate(m.media);
            if (now != m.last) {
                m.last = now;
                page.dispatchEvent(m, new MediaQueryListEvent("change", m.media, now));
            }
        }
    }
}
