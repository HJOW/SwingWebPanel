package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * <code>window.performance</code> (User Timing Level 3, W3C Working Draft, and Navigation Timing Level 2, both fetched 2026-11-17; see
 * <code>docs/progress/webapi2.md</code>): the interface <code>Performance</code> with <code>now()</code>, <code>timeOrigin</code>, <code>mark()</code>,
 * <code>measure()</code>, <code>clearMarks()</code>, <code>clearMeasures()</code>, <code>getEntries()</code>, <code>getEntriesByType()</code>,
 * <code>getEntriesByName()</code> and <code>toJSON()</code>; the entry interfaces <code>PerformanceEntry</code>, <code>PerformanceMark</code>,
 * <code>PerformanceMeasure</code> and <code>PerformanceNavigationTiming</code>; and the legacy <code>performance.timing</code> (<code>PerformanceTiming</code>) and
 * <code>performance.navigation</code> (<code>PerformanceNavigation</code>) of Navigation Timing Level 1. The time origin is the moment when the page host started
 * (the scheduler clock, milliseconds since the Unix epoch); the stages of the document (<code>domInteractive</code>, the <code>DOMContentLoaded</code> and
 * <code>load</code> events, <code>domComplete</code>) are the times at which the page host really passed them ({@link PageTimeline}). What the page cannot know is
 * <em>not invented</em>: the network times of the navigation (redirects, DNS, connection, request, response), the transfer sizes and the unload events of the
 * previous document are 0, <code>redirectCount</code> is 0, and the legacy <code>navigationStart</code> and <code>domLoading</code> are the time origin. Differences
 * from a browser: <code>PerformanceNavigationTiming</code> derives from <code>PerformanceEntry</code> only (not from <code>PerformanceResourceTiming</code>),
 * <code>Performance</code> is not an <code>EventTarget</code>, there is no <code>PerformanceObserver</code>, and a mark's <code>detail</code> is copied with
 * <code>structuredClone</code> when it is an object. Every method runs on the thread that owns the page's scripts.
 * <p>
 * <code>window.performance</code>(User Timing Level 3 W3C Working Draft 와 Navigation Timing Level 2, 모두 2026-11-17 조회, <code>docs/progress/webapi2.md</code> 참조):
 * <code>now()</code>, <code>timeOrigin</code>, <code>mark()</code>, <code>measure()</code>, <code>clearMarks()</code>, <code>clearMeasures()</code>,
 * <code>getEntries()</code>, <code>getEntriesByType()</code>, <code>getEntriesByName()</code>, <code>toJSON()</code> 이 있는 <code>Performance</code> 인터페이스,
 * 항목 인터페이스 <code>PerformanceEntry</code>, <code>PerformanceMark</code>, <code>PerformanceMeasure</code>, <code>PerformanceNavigationTiming</code>, 그리고
 * Navigation Timing Level 1 의 옛 <code>performance.timing</code>(<code>PerformanceTiming</code>)과 <code>performance.navigation</code>(<code>PerformanceNavigation</code>).
 * 시간 원점은 페이지 호스트가 시작한 순간(스케줄러 시계, 유닉스 에포크 이후 밀리초)이고, 문서의 단계(<code>domInteractive</code>, <code>DOMContentLoaded</code> 와
 * <code>load</code> 이벤트, <code>domComplete</code>)는 페이지 호스트가 실제로 지난 시각이다({@link PageTimeline}). 페이지가 알 수 없는 것은 <em>지어내지 않는다</em>:
 * 이동의 네트워크 시각(리디렉션, DNS, 연결, 요청, 응답), 전송 크기, 이전 문서의 unload 이벤트는 0 이고 <code>redirectCount</code> 는 0 이며 옛
 * <code>navigationStart</code> 와 <code>domLoading</code> 은 시간 원점이다. 브라우저와의 차이: <code>PerformanceNavigationTiming</code> 은
 * <code>PerformanceResourceTiming</code> 이 아니라 <code>PerformanceEntry</code> 에서만 파생되고, <code>Performance</code> 는 <code>EventTarget</code> 이 아니며,
 * <code>PerformanceObserver</code> 가 없고, 마크의 <code>detail</code> 은 객체이면 <code>structuredClone</code> 으로 복사한다. 모든 메서드는 페이지 스크립트를 소유한
 * 스레드에서 실행된다.
 */
final class PagePerformance {

    /**
     * The attributes of <code>PerformanceTiming</code> whose names cannot be used for a mark (User Timing, "mark": a name of a read only attribute of
     * <code>PerformanceTiming</code> is a SyntaxError), with the stage each stands for; the names that are not stages stand for the time origin or 0.
     * <p>
     * 마크 이름으로 쓸 수 없는 <code>PerformanceTiming</code> 속성의 이름(User Timing 의 "mark": <code>PerformanceTiming</code> 의 읽기 전용 속성 이름은 SyntaxError).
     * 이름마다 대응하는 단계가 있고 단계가 아닌 이름은 시간 원점이나 0 에 대응한다.
     */
    private static final String[] TIMING_ATTRIBUTES = { "navigationStart", "unloadEventStart", "unloadEventEnd", "redirectStart", "redirectEnd", "fetchStart",
        "domainLookupStart", "domainLookupEnd", "connectStart", "connectEnd", "secureConnectionStart", "requestStart", "responseStart", "responseEnd",
        "domLoading", "domInteractive", "domContentLoadedEventStart", "domContentLoadedEventEnd", "domComplete", "loadEventStart", "loadEventEnd" };

    /**
     * The attributes of <code>PerformanceNavigationTiming</code> that are times in milliseconds relative to the time origin (the stage or 0 for those that the page
     * cannot know), in the order of the interface.
     * <p>
     * 시간 원점에 상대적인 밀리초 시각인 <code>PerformanceNavigationTiming</code> 의 속성(페이지가 알 수 없는 것은 단계나 0). 인터페이스 순서.
     */
    private static final String[] NAVIGATION_TIMES = { "redirectStart", "redirectEnd", "fetchStart", "domainLookupStart", "domainLookupEnd", "connectStart",
        "connectEnd", "secureConnectionStart", "requestStart", "responseStart", "responseEnd", "unloadEventStart", "unloadEventEnd", "domInteractive",
        "domContentLoadedEventStart", "domContentLoadedEventEnd", "domComplete", "loadEventStart", "loadEventEnd" };

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The marks and measures in the order in which they were created.
     * <p>
     * 마크와 측정을 만들어진 순서대로 담는다.
     */
    private final List<Entry> buffer = new ArrayList<Entry>();

    /**
     * The prototypes of the entry interfaces by name.
     * <p>
     * 이름별 항목 인터페이스의 프로토타입.
     */
    private JsObject markProto;

    /**
     * The prototype of <code>PerformanceMeasure</code>.
     * <p>
     * <code>PerformanceMeasure</code> 의 프로토타입.
     */
    private JsObject measureProto;

    /**
     * The one <code>PerformanceNavigationTiming</code> object of the document.
     * <p>
     * 문서의 <code>PerformanceNavigationTiming</code> 객체 하나.
     */
    private JsObject navigationEntry;

    /**
     * The legacy <code>performance.timing</code> object.
     * <p>
     * 옛 <code>performance.timing</code> 객체.
     */
    private JsObject legacyTiming;

    /**
     * The legacy <code>performance.navigation</code> object.
     * <p>
     * 옛 <code>performance.navigation</code> 객체.
     */
    private JsObject legacyNavigation;

    /**
     * A mark or a measure of the buffer.
     * <p>
     * 버퍼의 마크나 측정.
     */
    private static final class Entry extends JsObject {
        /**
         * The name.
         * <p>
         * 이름.
         */
        final String name;

        /**
         * The entry type, <code>mark</code> or <code>measure</code>.
         * <p>
         * 항목 종류. <code>mark</code> 또는 <code>measure</code>.
         */
        final String type;

        /**
         * The start time in milliseconds after the time origin.
         * <p>
         * 시간 원점 이후 밀리초의 시작 시각.
         */
        final double startTime;

        /**
         * The duration in milliseconds (0 for a mark).
         * <p>
         * 밀리초 단위 지속 시간(마크는 0).
         */
        final double duration;

        /**
         * The detail value of the mark or the measure (the copy of the <code>detail</code> option, or <code>null</code>).
         * <p>
         * 마크나 측정의 detail 값(<code>detail</code> 옵션의 복사본. 없으면 <code>null</code>).
         */
        final JsValue detail;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param proto     the prototype / 프로토타입
         * @param name      the name / 이름
         * @param type      the type / 종류
         * @param startTime the start time / 시작 시각
         * @param duration  the duration / 지속 시간
         * @param detail    the detail / detail
         */
        Entry(JsObject proto, String name, String type, double startTime, double duration, JsValue detail) {
            super(proto);
            this.name = name;
            this.type = type;
            this.startTime = startTime;
            this.duration = duration;
            this.detail = detail;
        }
    }

    /**
     * An object whose attributes are computed from the state of the page when they are read (the navigation entry and the legacy objects).
     * <p>
     * 속성을 읽을 때 페이지의 상태에서 계산하는 객체(탐색 항목과 옛 객체).
     */
    private static final class LiveObject extends JsObject {
        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        LiveObject(JsObject proto) {
            super(proto);
        }
    }

    /**
     * Creates the performance object of a page.
     * <p>
     * 페이지의 performance 객체를 만든다.
     *
     * @param page the page / 페이지
     */
    PagePerformance(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the time in milliseconds after the time origin.
     * <p>
     * 시간 원점 이후의 시간(밀리초)을 반환한다.
     *
     * @return the time / 시간
     */
    private double now() {
        Scheduler sch = page.getEnvironment().getScheduler();
        long t = sch == null ? System.currentTimeMillis() : sch.now();
        return t - page.startTime();
    }

    /**
     * Returns the time of a stage in milliseconds after the time origin, or 0 if it was not reached.
     * <p>
     * 단계의 시각을 시간 원점 이후 밀리초로 반환한다. 이르지 못했으면 0.
     *
     * @param stage the stage of {@link PageTimeline} / {@link PageTimeline} 의 단계
     * @return the time / 시간
     */
    private double stage(int stage) {
        long t = page.getTimeline().get(stage);
        return t == 0 ? 0 : t - page.startTime();
    }

    /**
     * Returns the time of a stage as milliseconds since the Unix epoch (the legacy attributes), or 0 if it was not reached.
     * <p>
     * 단계의 시각을 유닉스 에포크 이후 밀리초로 반환한다(옛 속성). 이르지 못했으면 0.
     *
     * @param stage the stage / 단계
     * @return the time / 시간
     */
    private double epoch(int stage) {
        return page.getTimeline().get(stage);
    }

    /**
     * Returns the value of a named time attribute relative to the time origin (the names of <code>PerformanceTiming</code> and of the times of
     * <code>PerformanceNavigationTiming</code>); 0 for the network times that the page cannot know.
     * <p>
     * 이름 있는 시각 속성의 값을 시간 원점 기준으로 반환한다(<code>PerformanceTiming</code> 과 <code>PerformanceNavigationTiming</code> 시각의 이름). 페이지가 알 수 없는 네트워크
     * 시각은 0 이다.
     *
     * @param name the attribute name / 속성 이름
     * @return the time in milliseconds after the time origin / 시간 원점 이후 밀리초
     */
    private double relative(String name) {
        if (name.equals("domInteractive")) {
            return stage(PageTimeline.DOM_INTERACTIVE);
        } else if (name.equals("domContentLoadedEventStart")) {
            return stage(PageTimeline.DCL_START);
        } else if (name.equals("domContentLoadedEventEnd")) {
            return stage(PageTimeline.DCL_END);
        } else if (name.equals("domComplete")) {
            return stage(PageTimeline.DOM_COMPLETE);
        } else if (name.equals("loadEventStart")) {
            return stage(PageTimeline.LOAD_START);
        } else if (name.equals("loadEventEnd")) {
            return stage(PageTimeline.LOAD_END);
        }
        return 0;
    }

    /**
     * Installs <code>window.performance</code> and the interfaces on the global object.
     * <p>
     * 전역 객체에 <code>window.performance</code> 와 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        JsObject entryProto = interfaceObject(g, "PerformanceEntry", null, false);
        getter(entryProto, "name", new NativeFunction.Call() {
            /**
             * Returns the name.
             * <p>
             * 이름을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the name / 이름
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(nameOf(self));
            }
        });
        getter(entryProto, "entryType", new NativeFunction.Call() {
            /**
             * Returns the entry type.
             * <p>
             * 항목 종류를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the type / 종류
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(self instanceof Entry ? ((Entry) self).type : "navigation");
            }
        });
        getter(entryProto, "startTime", new NativeFunction.Call() {
            /**
             * Returns the start time.
             * <p>
             * 시작 시각을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time / 시각
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(self instanceof Entry ? ((Entry) self).startTime : 0);
            }
        });
        getter(entryProto, "duration", new NativeFunction.Call() {
            /**
             * Returns the duration (the navigation entry lasts until the end of the load event).
             * <p>
             * 지속 시간을 반환한다(탐색 항목은 load 이벤트가 끝날 때까지 지속된다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the duration / 지속 시간
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(self instanceof Entry ? ((Entry) self).duration : relative("loadEventEnd"));
            }
        });
        realm.method(entryProto, "toJSON", 0, new NativeFunction.Call() {
            /**
             * Returns a plain object with the attributes of the entry.
             * <p>
             * 항목의 속성을 가진 보통 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return entryJson(self);
            }
        });
        markProto = interfaceObject(g, "PerformanceMark", entryProto, true);
        getter(markProto, "detail", new NativeFunction.Call() {
            /**
             * Returns the detail.
             * <p>
             * detail 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the detail / detail
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return detailOf(self);
            }
        });
        measureProto = interfaceObject(g, "PerformanceMeasure", entryProto, false);
        getter(measureProto, "detail", new NativeFunction.Call() {
            /**
             * Returns the detail.
             * <p>
             * detail 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the detail / detail
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return detailOf(self);
            }
        });
        JsObject navProto = interfaceObject(g, "PerformanceNavigationTiming", entryProto, false);
        // 시각 속성마다 읽을 때 단계에서 계산하는 게터를 정의한다.
        for (int i = 0; i < NAVIGATION_TIMES.length; i++) {
            final String name = NAVIGATION_TIMES[i];
            getter(navProto, name, new NativeFunction.Call() {
                /**
                 * Returns the time.
                 * <p>
                 * 시각을 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the time / 시각
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.num(relative(name));
                }
            });
        }
        getter(navProto, "initiatorType", constant(Js.str("navigation")));
        getter(navProto, "nextHopProtocol", constant(Js.str("")));
        getter(navProto, "redirectCount", constant(Js.num(0)));
        getter(navProto, "transferSize", constant(Js.num(0)));
        getter(navProto, "encodedBodySize", constant(Js.num(0)));
        getter(navProto, "decodedBodySize", constant(Js.num(0)));
        getter(navProto, "type", new NativeFunction.Call() {
            /**
             * Returns <code>navigate</code>, <code>reload</code> or <code>back_forward</code> from the kind of navigation that made the page.
             * <p>
             * 페이지를 만든 이동의 종류에 따라 <code>navigate</code>, <code>reload</code>, <code>back_forward</code> 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the type / 종류
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(page.getEnvironment().getNavigationType());
            }
        });
        navigationEntry = new LiveObject(navProto);
        JsObject timingProto = interfaceObject(g, "PerformanceTiming", null, false);
        // 옛 속성마다 에포크 밀리초를 반환하는 게터를 정의한다.
        for (int i = 0; i < TIMING_ATTRIBUTES.length; i++) {
            final String name = TIMING_ATTRIBUTES[i];
            getter(timingProto, name, new NativeFunction.Call() {
                /**
                 * Returns the time in milliseconds since the Unix epoch.
                 * <p>
                 * 유닉스 에포크 이후 밀리초 시각을 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the time / 시각
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.num(legacy(name));
                }
            });
        }
        legacyTiming = new LiveObject(timingProto);
        JsObject navigationProto = interfaceObject(g, "PerformanceNavigation", null, false);
        JsObject navigationCtor = (JsObject) navigationProto.get("constructor");
        String[] constantNames = { "TYPE_NAVIGATE", "TYPE_RELOAD", "TYPE_BACK_FORWARD", "TYPE_RESERVED" };
        int[] constantValues = { 0, 1, 2, 255 };
        // 상수는 인터페이스 객체와 프로토타입에 모두 있다(WebIDL).
        for (int i = 0; i < constantNames.length; i++) {
            navigationProto.defineData(constantNames[i], Js.num(constantValues[i]), false, true, false);
            navigationCtor.defineData(constantNames[i], Js.num(constantValues[i]), false, true, false);
        }
        getter(navigationProto, "type", new NativeFunction.Call() {
            /**
             * Returns 0 for a navigation, 1 for a reload and 2 for a move in the history.
             * <p>
             * 이동이면 0, 새로고침이면 1, 이력 이동이면 2 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the type / 종류
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String t = page.getEnvironment().getNavigationType();
                return Js.num("reload".equals(t) ? 1 : "back_forward".equals(t) ? 2 : 0);
            }
        });
        getter(navigationProto, "redirectCount", constant(Js.num(0)));
        legacyNavigation = new LiveObject(navigationProto);
        installPerformance(g);
    }

    /**
     * Returns a getter that returns a constant.
     * <p>
     * 상수를 반환하는 게터를 반환한다.
     *
     * @param value the value / 값
     * @return the function body / 함수 본문
     */
    private static NativeFunction.Call constant(final JsValue value) {
        return new NativeFunction.Call() {
            /**
             * Returns the constant.
             * <p>
             * 상수를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return value;
            }
        };
    }

    /**
     * Returns the value of a legacy <code>PerformanceTiming</code> attribute in milliseconds since the Unix epoch.
     * <p>
     * 옛 <code>PerformanceTiming</code> 속성의 값을 유닉스 에포크 이후 밀리초로 반환한다.
     *
     * @param name the attribute / 속성
     * @return the time / 시각
     */
    private double legacy(String name) {
        if (name.equals("navigationStart") || name.equals("domLoading")) {
            return page.startTime();
        } else if (name.equals("domInteractive")) {
            return epoch(PageTimeline.DOM_INTERACTIVE);
        } else if (name.equals("domContentLoadedEventStart")) {
            return epoch(PageTimeline.DCL_START);
        } else if (name.equals("domContentLoadedEventEnd")) {
            return epoch(PageTimeline.DCL_END);
        } else if (name.equals("domComplete")) {
            return epoch(PageTimeline.DOM_COMPLETE);
        } else if (name.equals("loadEventStart")) {
            return epoch(PageTimeline.LOAD_START);
        } else if (name.equals("loadEventEnd")) {
            return epoch(PageTimeline.LOAD_END);
        }
        return 0;
    }

    /**
     * Returns the name of an entry (the address of the document for the navigation entry).
     * <p>
     * 항목의 이름을 반환한다(탐색 항목은 문서의 주소).
     *
     * @param self the entry / 항목
     * @return the name / 이름
     */
    private String nameOf(JsValue self) {
        if (self instanceof Entry) {
            return ((Entry) self).name;
        } else if (self instanceof LiveObject) {
            return page.getDocument().getUrl();
        }
        throw JsException.typeError("Illegal invocation");
    }

    /**
     * Returns the detail of a mark or a measure (<code>null</code> if it has none).
     * <p>
     * 마크나 측정의 detail 을 반환한다(없으면 <code>null</code>).
     *
     * @param self the entry / 항목
     * @return the detail / detail
     */
    private static JsValue detailOf(JsValue self) {
        if (self instanceof Entry) {
            return ((Entry) self).detail;
        }
        throw JsException.typeError("Illegal invocation");
    }

    /**
     * Defines a read-only accessor on a prototype.
     * <p>
     * 프로토타입에 읽기 전용 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the attribute / 속성
     * @param get   the getter / 게터
     */
    private void getter(JsObject proto, String name, NativeFunction.Call get) {
        proto.defineAccessor(name, page.getRealm().newFunction("get " + name, 0, get), null, true, true);
    }

    /**
     * Creates an interface object and its prototype, puts it on the global object and returns the prototype; the constructor throws <code>Illegal constructor</code>
     * unless the page may construct the interface.
     * <p>
     * 인터페이스 객체와 프로토타입을 만들어 전역 객체에 두고 프로토타입을 반환한다. 페이지가 만들 수 있는 인터페이스가 아니면 생성자는 <code>Illegal constructor</code> 를 던진다.
     *
     * @param g           the global object / 전역 객체
     * @param name        the interface name / 인터페이스 이름
     * @param parent      the prototype of the parent interface, or null / 부모 인터페이스의 프로토타입. 없으면 null
     * @param constructible true if the page may construct it (<code>PerformanceMark</code>) / 페이지가 만들 수 있으면(<code>PerformanceMark</code>) true
     * @return the prototype / 프로토타입
     */
    private JsObject interfaceObject(JsObject g, final String name, JsObject parent, final boolean constructible) {
        JsRealm realm = page.getRealm();
        final JsObject proto = parent == null ? realm.newObject() : new JsObject(parent);
        NativeFunction ctor = new NativeFunction(realm, name, constructible ? 1 : 0, new NativeFunction.Call() {
            /**
             * An interface object cannot be called.
             * <p>
             * 인터페이스 객체는 함수로 부를 수 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError(constructible ? "Failed to construct '" + name + "': Please use the 'new' operator, this DOM object constructor cannot be called as a function."
                        : "Illegal constructor");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a mark (the only interface that the page may construct) or throws <code>Illegal constructor</code>.
             * <p>
             * 마크를 만든다(페이지가 만들 수 있는 유일한 인터페이스). 그 밖에는 <code>Illegal constructor</code> 를 던진다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the object / 객체
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (!constructible) {
                    throw JsException.typeError("Illegal constructor");
                }
                return createMark(Js.arg(args, 0), Js.arg(args, 1), Ops.getPrototypeFromConstructor(r, newTarget, markProto));
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(Js.str(name), false, false, true));
        g.defineBuiltin(name, ctor);
        return proto;
    }

    /**
     * Installs <code>Performance</code> and the <code>window.performance</code> object.
     * <p>
     * <code>Performance</code> 와 <code>window.performance</code> 객체를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installPerformance(JsObject g) {
        JsRealm realm = page.getRealm();
        JsObject proto = interfaceObject(g, "Performance", null, false);
        realm.method(proto, "now", 0, new NativeFunction.Call() {
            /**
             * Returns the milliseconds since the time origin.
             * <p>
             * 시간 원점 이후의 밀리초를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time / 시간
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(now());
            }
        });
        getter(proto, "timeOrigin", new NativeFunction.Call() {
            /**
             * Returns the time origin in milliseconds since the Unix epoch.
             * <p>
             * 유닉스 에포크 이후 밀리초의 시간 원점을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time / 시각
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(page.startTime());
            }
        });
        getter(proto, "timing", constantObject("timing"));
        getter(proto, "navigation", constantObject("navigation"));
        realm.method(proto, "mark", 1, new NativeFunction.Call() {
            /**
             * Creates a mark and adds it to the buffer.
             * <p>
             * 마크를 만들어 버퍼에 더한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the mark / 마크
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'mark' on 'Performance': 1 argument required, but only 0 present.");
                }
                Entry mark = createMark(args[0], Js.arg(args, 1), markProto);
                buffer.add(mark);
                return mark;
            }
        });
        realm.method(proto, "measure", 1, new NativeFunction.Call() {
            /**
             * Creates a measure between two points and adds it to the buffer.
             * <p>
             * 두 지점 사이의 측정을 만들어 버퍼에 더한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the measure / 측정
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'measure' on 'Performance': 1 argument required, but only 0 present.");
                }
                Entry measure = createMeasure(Ops.toString(args[0]), Js.arg(args, 1), Js.arg(args, 2));
                buffer.add(measure);
                return measure;
            }
        });
        realm.method(proto, "clearMarks", 0, clear("mark"));
        realm.method(proto, "clearMeasures", 0, clear("measure"));
        realm.method(proto, "getEntries", 0, new NativeFunction.Call() {
            /**
             * Returns all entries ordered by start time.
             * <p>
             * 모든 항목을 시작 시각 순서로 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the array / 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return entries(null, null);
            }
        });
        realm.method(proto, "getEntriesByType", 1, new NativeFunction.Call() {
            /**
             * Returns the entries of a type.
             * <p>
             * 종류별 항목을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the array / 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'getEntriesByType' on 'Performance': 1 argument required, but only 0 present.");
                }
                return entries(null, Ops.toString(args[0]));
            }
        });
        realm.method(proto, "getEntriesByName", 1, new NativeFunction.Call() {
            /**
             * Returns the entries with a name, and a type if one is given.
             * <p>
             * 이름이 같은 항목을 반환한다. 종류를 주면 그 종류만.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the array / 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'getEntriesByName' on 'Performance': 1 argument required, but only 0 present.");
                }
                JsValue type = Js.arg(args, 1);
                return entries(Ops.toString(args[0]), type.isUndefined() ? null : Ops.toString(type));
            }
        });
        realm.method(proto, "toJSON", 0, new NativeFunction.Call() {
            /**
             * Returns an object with <code>timeOrigin</code> (and the legacy objects).
             * <p>
             * <code>timeOrigin</code> 과 옛 객체를 가진 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsObject o = r.newObject();
                o.createDataProperty("timeOrigin", Js.num(page.startTime()));
                return o;
            }
        });
        g.defineData("performance", new JsObject(proto), true, true, true);
    }

    /**
     * Returns a getter that returns the legacy object <code>timing</code> or <code>navigation</code>.
     * <p>
     * 옛 객체 <code>timing</code> 또는 <code>navigation</code> 을 반환하는 게터를 반환한다.
     *
     * @param which <code>timing</code> or <code>navigation</code> / <code>timing</code> 또는 <code>navigation</code>
     * @return the function body / 함수 본문
     */
    private NativeFunction.Call constantObject(final String which) {
        return new NativeFunction.Call() {
            /**
             * Returns the object.
             * <p>
             * 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return "timing".equals(which) ? legacyTiming : legacyNavigation;
            }
        };
    }

    /**
     * Returns the body of <code>clearMarks</code> or <code>clearMeasures</code>.
     * <p>
     * <code>clearMarks</code> 또는 <code>clearMeasures</code> 의 본문을 반환한다.
     *
     * @param type the entry type to clear / 지울 항목 종류
     * @return the function body / 함수 본문
     */
    private NativeFunction.Call clear(final String type) {
        return new NativeFunction.Call() {
            /**
             * Removes the entries of the type (only those with the given name if there is one).
             * <p>
             * 종류의 항목을 지운다(이름을 주면 그 이름만).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue name = Js.arg(args, 0);
                String n = name.isUndefined() ? null : Ops.toString(name);
                // 버퍼를 거꾸로 훑어 종류와 이름이 맞는 항목을 지운다.
                for (int i = buffer.size() - 1; i >= 0; i--) {
                    Entry e = buffer.get(i);
                    if (e.type.equals(type) && (n == null || e.name.equals(n))) {
                        buffer.remove(i);
                    }
                }
                return Js.UNDEFINED;
            }
        };
    }

    /**
     * Returns the entries of the timeline as a script array, ordered by start time (stable), filtered by name and type.
     * <p>
     * 타임라인의 항목을 이름과 종류로 걸러 시작 시각 순서(안정 정렬)의 스크립트 배열로 반환한다.
     *
     * @param name the name, or null for all / 이름. 모두이면 null
     * @param type the type, or null for all / 종류. 모두이면 null
     * @return the array / 배열
     */
    private JsValue entries(String name, String type) {
        List<JsObject> all = new ArrayList<JsObject>();
        // 탐색 항목은 시작 시각이 0 이므로 맨 앞에 둔다.
        if ((type == null || type.equals("navigation")) && (name == null || name.equals(nameOf(navigationEntry)))) {
            all.add(navigationEntry);
        }
        // 버퍼의 마크와 측정 중 조건에 맞는 것을 더한다.
        for (Entry e : buffer) {
            if ((type == null || e.type.equals(type)) && (name == null || e.name.equals(name))) {
                all.add(e);
            }
        }
        Collections.sort(all, new Comparator<JsObject>() {
            /**
             * Orders by start time; entries that start together keep their order.
             * <p>
             * 시작 시각으로 정렬한다. 같은 시각에 시작하면 순서를 유지한다.
             *
             * @param a one entry / 항목 하나
             * @param b the other entry / 다른 항목
             * @return the order / 순서
             */
            public int compare(JsObject a, JsObject b) {
                double x = a instanceof Entry ? ((Entry) a).startTime : 0;
                double y = b instanceof Entry ? ((Entry) b).startTime : 0;
                return x < y ? -1 : (x > y ? 1 : 0);
            }
        });
        return page.getRealm().newArray(new ArrayList<JsValue>(all));
    }

    /**
     * Builds the plain object that <code>toJSON</code> of an entry returns.
     * <p>
     * 항목의 <code>toJSON</code> 이 반환하는 보통 객체를 만든다.
     *
     * @param self the entry / 항목
     * @return the object / 객체
     */
    private JsValue entryJson(JsValue self) {
        JsObject o = page.getRealm().newObject();
        o.createDataProperty("name", Js.str(nameOf(self)));
        if (self instanceof Entry) {
            Entry e = (Entry) self;
            o.createDataProperty("entryType", Js.str(e.type));
            o.createDataProperty("startTime", Js.num(e.startTime));
            o.createDataProperty("duration", Js.num(e.duration));
            o.createDataProperty("detail", e.detail);
        } else {
            o.createDataProperty("entryType", Js.str("navigation"));
            o.createDataProperty("startTime", Js.num(0));
            o.createDataProperty("duration", Js.num(relative("loadEventEnd")));
            // 탐색 항목의 시각 속성을 모두 담는다.
            for (int i = 0; i < NAVIGATION_TIMES.length; i++) {
                o.createDataProperty(NAVIGATION_TIMES[i], Js.num(relative(NAVIGATION_TIMES[i])));
            }
            o.createDataProperty("type", Js.str(page.getEnvironment().getNavigationType()));
            o.createDataProperty("redirectCount", Js.num(0));
        }
        return o;
    }

    /**
     * Tells whether a name is an attribute of <code>PerformanceTiming</code>.
     * <p>
     * 이름이 <code>PerformanceTiming</code> 의 속성인지 알려 준다.
     *
     * @param name the name / 이름
     * @return true if it is / 그렇다면 true
     */
    private static boolean isTimingAttribute(String name) {
        // 속성 이름 목록에서 같은 이름을 찾는다.
        for (int i = 0; i < TIMING_ATTRIBUTES.length; i++) {
            if (TIMING_ATTRIBUTES[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a mark (User Timing, the <code>PerformanceMark</code> constructor and <code>mark()</code>): the name, the optional <code>startTime</code> (not negative)
     * and the optional <code>detail</code>.
     * <p>
     * 마크를 만든다(User Timing 의 <code>PerformanceMark</code> 생성자와 <code>mark()</code>): 이름, 선택적 <code>startTime</code>(음수 아님), 선택적 <code>detail</code>.
     *
     * @param nameValue the name value / 이름 값
     * @param options   the options value / 옵션 값
     * @param proto     the prototype / 프로토타입
     * @return the mark / 마크
     */
    private Entry createMark(JsValue nameValue, JsValue options, JsObject proto) {
        String name = Ops.toString(nameValue);
        // 읽기 전용 PerformanceTiming 속성의 이름은 마크 이름으로 쓸 수 없다.
        if (isTimingAttribute(name)) {
            throw page.getBridge().domError(DomException.syntax("Failed to execute 'mark' on 'Performance': '" + name + "' is part of the PerformanceTiming interface, and cannot be used as a mark name."));
        }
        double start = now();
        JsValue detail = Js.NULL;
        if (options.isObject()) {
            JsObject o = (JsObject) options;
            JsValue st = o.get("startTime");
            if (!st.isUndefined()) {
                start = RenderObservers.finite(st, "startTime");
                if (start < 0) {
                    throw JsException.typeError("Failed to construct 'PerformanceMark': 'startTime' cannot be negative.");
                }
            }
            JsValue d = o.get("detail");
            if (!d.isUndefined() && !d.isNull()) {
                detail = copy(d);
            }
        }
        return new Entry(proto, name, "mark", start, 0, detail);
    }

    /**
     * Copies a detail value with <code>structuredClone</code> when it is an object (a value that cannot be cloned throws, as the specification says).
     * <p>
     * detail 값이 객체이면 <code>structuredClone</code> 으로 복사한다(복제할 수 없는 값은 규격대로 오류를 던진다).
     *
     * @param value the value / 값
     * @return the copy / 복사본
     */
    private JsValue copy(JsValue value) {
        if (!value.isObject()) {
            return value;
        }
        JsValue clone = page.getRealm().getGlobalObject().get("structuredClone");
        return clone.isCallable() ? Ops.call(clone, Js.UNDEFINED, new JsValue[] { value }) : value;
    }

    /**
     * Converts a position of <code>measure()</code> (a mark name or a time) to a time after the time origin.
     * <p>
     * <code>measure()</code> 의 지점(마크 이름이나 시각)을 시간 원점 이후의 시각으로 바꾼다.
     *
     * @param v the value / 값
     * @return the time / 시각
     */
    private double position(JsValue v) {
        if (v instanceof org.duckdns.hjow.swingwebpanel.js.JsNumber) {
            return RenderObservers.finite(v, "time");
        }
        String name = Ops.toString(v);
        // 가장 최근의 같은 이름 마크의 시작 시각을 쓴다.
        for (int i = buffer.size() - 1; i >= 0; i--) {
            Entry e = buffer.get(i);
            if (e.type.equals("mark") && e.name.equals(name)) {
                return e.startTime;
            }
        }
        // PerformanceTiming 속성 이름은 그 시각에서 navigationStart 를 뺀 값이다.
        if (isTimingAttribute(name)) {
            return legacy(name) == 0 ? 0 : legacy(name) - page.startTime();
        }
        throw page.getBridge().domError(DomException.syntax("Failed to execute 'measure' on 'Performance': The mark '" + name + "' does not exist."));
    }

    /**
     * Creates a measure (User Timing, <code>measure(name, startOrMeasureOptions, endMark)</code>).
     * <p>
     * 측정을 만든다(User Timing 의 <code>measure(name, startOrMeasureOptions, endMark)</code>).
     *
     * @param name    the name / 이름
     * @param second  the second argument: a start mark name or an options object / 두 번째 인수: 시작 마크 이름이나 옵션 객체
     * @param endMark the third argument / 세 번째 인수
     * @return the measure / 측정
     */
    private Entry createMeasure(String name, JsValue second, JsValue endMark) {
        double end;
        double start;
        JsValue detail = Js.NULL;
        JsValue startArg = Js.UNDEFINED;
        JsValue endArg = endMark;
        JsValue durationArg = Js.UNDEFINED;
        // 두 번째 인수가 옵션 사전이면 start, end, duration, detail 을 읽는다. 문자열이면 시작 마크 이름이다.
        if (second.isObject()) {
            JsObject o = (JsObject) second;
            if (!endMark.isUndefined()) {
                throw JsException.typeError("Failed to execute 'measure' on 'Performance': The measure options cannot be combined with an end mark.");
            }
            startArg = o.get("start");
            endArg = o.get("end");
            durationArg = o.get("duration");
            JsValue d = o.get("detail");
            if (!d.isUndefined() && !d.isNull()) {
                detail = copy(d);
            }
            if (!durationArg.isUndefined() && !startArg.isUndefined() && !endArg.isUndefined()) {
                throw JsException.typeError("Failed to execute 'measure' on 'Performance': If a non-empty PerformanceMeasureOptions object was passed, "
                        + "it must not have all of its 'start', 'duration', and 'end' properties defined");
            }
        } else if (!second.isUndefined()) {
            startArg = second;
        }
        // 끝 시각: end 가 있으면 그것, start 와 duration 이 있으면 합, 아니면 지금.
        if (!endArg.isUndefined()) {
            end = position(endArg);
        } else if (!durationArg.isUndefined() && !startArg.isUndefined()) {
            end = position(startArg) + RenderObservers.finite(durationArg, "duration");
        } else {
            end = now();
        }
        // 시작 시각: start 가 있으면 그것, duration 과 end 가 있으면 차, 아니면 0.
        if (!startArg.isUndefined()) {
            start = position(startArg);
        } else if (!durationArg.isUndefined() && !endArg.isUndefined()) {
            start = end - RenderObservers.finite(durationArg, "duration");
        } else {
            start = 0;
        }
        return new Entry(measureProto, name, "measure", start, end - start, detail);
    }
}
