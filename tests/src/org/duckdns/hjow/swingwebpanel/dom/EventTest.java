package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the DOM event model (plan items P06-06, P06-07, P06-09): phases and order, target and current target, cancellation and
 * propagation flags, listener registration rules, listener errors, re-entrancy and the document to window path.
 * <p>
 * DOM 이벤트 모델 시험(계획 P06-06, P06-07, P06-09): 단계와 순서, target 과 currentTarget, 취소와 전파 플래그, 리스너 등록 규칙, 리스너 오류, 재진입,
 * 문서에서 window 로 가는 경로.
 */
public class EventTest {

    /**
     * Document of the current case.
     * <p>
     * 현재 사례의 문서.
     */
    private Document doc;

    /**
     * Elements of the case: html > body > div > span.
     * <p>
     * 사례의 요소: html > body > div > span.
     */
    private Element html;

    /**
     * Body element.
     * <p>
     * body 요소.
     */
    private Element body;

    /**
     * Div element.
     * <p>
     * div 요소.
     */
    private Element div;

    /**
     * Span element (the usual event target).
     * <p>
     * span 요소(보통의 이벤트 대상).
     */
    private Element span;

    /**
     * Log of listener calls in order.
     * <p>
     * 리스너 호출의 순서 기록.
     */
    private List<String> log;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EventTest() {
    }

    /**
     * Builds the tree and the log before each case.
     * <p>
     * 사례마다 트리와 기록을 만든다.
     */
    public void setUp() {
        doc = Document.createHtmlDocument();
        html = doc.createElement("html");
        body = doc.createElement("body");
        div = doc.createElement("div");
        span = doc.createElement("span");
        doc.appendChild(html);
        html.appendChild(body);
        body.appendChild(div);
        div.appendChild(span);
        log = new ArrayList<String>();
    }

    /**
     * Creates a listener that appends a label (with the phase) to the log.
     * <p>
     * 라벨(과 단계)을 기록에 덧붙이는 리스너를 만든다.
     *
     * @param label label / 라벨
     * @return the listener / 리스너
     */
    private EventListener logger(final String label) {
        return new EventListener() {
            /**
             * Logs the call.
             * <p>
             * 호출을 기록한다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add(label + "@" + e.getEventPhase());
            }
        };
    }

    /**
     * P06-06: capture from the outside in, then the target, then bubbling from the inside out; capture listeners of the target run
     * before its bubble listeners.
     * <p>
     * P06-06: 바깥에서 안으로 캡처, 그다음 대상, 그다음 안에서 바깥으로 버블. 대상의 캡처 리스너가 버블 리스너보다 먼저 실행된다.
     */
    public void testPhasesAndOrder() {
        doc.addEventListener("x", logger("doc-b"), false);
        doc.addEventListener("x", logger("doc-c"), true);
        body.addEventListener("x", logger("body-b"), false);
        body.addEventListener("x", logger("body-c"), true);
        span.addEventListener("x", logger("span-b"), false);
        span.addEventListener("x", logger("span-c"), true);
        boolean result = span.dispatchEvent(new Event("x", true, true));
        Check.isTrue(result, "dispatchEvent returns true when not cancelled");
        Check.equal(Arrays.asList("doc-c@1", "body-c@1", "span-c@2", "span-b@2", "body-b@3", "doc-b@3"), log, "listener order and phases");
    }

    /**
     * P06-06: an event that does not bubble skips the bubble phase but still runs capture and target listeners.
     * <p>
     * P06-06: 버블하지 않는 이벤트는 버블 단계를 건너뛰지만 캡처와 대상 리스너는 실행한다.
     */
    public void testNonBubblingEvent() {
        body.addEventListener("x", logger("body-b"), false);
        body.addEventListener("x", logger("body-c"), true);
        span.addEventListener("x", logger("span-b"), false);
        span.dispatchEvent(new Event("x", false, false));
        Check.equal(Arrays.asList("body-c@1", "span-b@2"), log, "no bubble phase");
    }

    /**
     * P06-06: target, currentTarget, phase and composedPath during and after the dispatch.
     * <p>
     * P06-06: 전달 중과 전달 뒤의 target, currentTarget, 단계, composedPath.
     */
    public void testTargetsAndPath() {
        final Event e = new Event("x", true, false);
        final List<String> seen = new ArrayList<String>();
        div.addEventListener("x", new EventListener() {
            /**
             * Records the targets seen by the listener.
             * <p>
             * 리스너가 본 대상을 기록한다.
             *
             * @param ev event / 이벤트
             */
            public void handleEvent(Event ev) {
                seen.add(((Element) ev.getTarget()).getLocalName() + "/" + ((Element) ev.getCurrentTarget()).getLocalName());
                // 이벤트 경로의 각 대상을 이름으로 기록한다.
                for (EventTarget t : ev.composedPath()) {
                    seen.add(t instanceof Element ? ((Element) t).getLocalName() : "doc");
                }
            }
        });
        Check.isNull(e.getTarget(), "no target before dispatch");
        span.dispatchEvent(e);
        Check.equal(Arrays.asList("span/div", "span", "div", "body", "html", "doc"), seen, "target, current target and path");
        Check.isNull(e.getCurrentTarget(), "no current target after dispatch");
        Check.equal(Event.NONE, e.getEventPhase(), "phase after dispatch");
        Check.same(span, e.getTarget(), "the target stays");
        Check.isTrue(e.composedPath().isEmpty(), "the path is empty after dispatch");
    }

    /**
     * P06-06: preventDefault works only for cancelable events; passive listeners cannot cancel.
     * <p>
     * P06-06: preventDefault 는 취소 가능한 이벤트에서만 작동하고 passive 리스너는 취소할 수 없다.
     */
    public void testCancellation() {
        EventListener cancel = new EventListener() {
            /**
             * Cancels the event.
             * <p>
             * 이벤트를 취소한다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                e.preventDefault();
            }
        };
        span.addEventListener("c", cancel);
        Check.isFalse(span.dispatchEvent(new Event("c", false, true)), "a cancelable event can be cancelled");
        Event notCancelable = new Event("c", false, false);
        Check.isTrue(span.dispatchEvent(notCancelable), "a non-cancelable event cannot be cancelled");
        Check.isFalse(notCancelable.isDefaultPrevented(), "and reports no cancellation");
        span.removeEventListener("c", cancel, false);
        span.addEventListener("c", cancel, false, false, true);
        Event e = new Event("c", false, true);
        Check.isTrue(span.dispatchEvent(e), "a passive listener cannot cancel");
        Check.isFalse(e.isDefaultPrevented(), "the event is not cancelled");
    }

    /**
     * P06-06: stopPropagation stops later targets but not the rest of the current target; stopImmediatePropagation stops both.
     * <p>
     * P06-06: stopPropagation 은 이후 대상을 막지만 현재 대상의 나머지는 막지 않고, stopImmediatePropagation 은 둘 다 막는다.
     */
    public void testStopPropagation() {
        div.addEventListener("x", new EventListener() {
            /**
             * Logs and stops the propagation.
             * <p>
             * 기록하고 전파를 멈춘다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add("div1");
                e.stopPropagation();
            }
        });
        div.addEventListener("x", logger("div2"));
        body.addEventListener("x", logger("body"));
        span.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("div1", "div2@3"), log, "div2 still runs, body does not");
        log.clear();
        div.removeAllEventListeners();
        div.addEventListener("x", new EventListener() {
            /**
             * Logs and stops the propagation immediately.
             * <p>
             * 기록하고 즉시 전파를 멈춘다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add("first");
                e.stopImmediatePropagation();
            }
        });
        div.addEventListener("x", logger("second"));
        span.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("first"), log, "stopImmediatePropagation skips the remaining listeners and later targets");
        log.clear();
        Event reused = new Event("x", true, false);
        span.dispatchEvent(reused);
        log.clear();
        span.dispatchEvent(reused);
        Check.equal(Arrays.asList("first"), log, "the propagation flags are reset after a dispatch, so an event object can be dispatched again");
    }

    /**
     * P06-07: duplicate registrations are ignored; capture flag matters for removal; once listeners run once.
     * <p>
     * P06-07: 중복 등록은 무시되고, 제거에는 캡처 플래그가 중요하며, once 리스너는 한 번만 실행된다.
     */
    public void testRegistrationRules() {
        EventListener l = logger("l");
        span.addEventListener("x", l, false);
        span.addEventListener("x", l, false);
        span.addEventListener("x", l, true);
        span.dispatchEvent(new Event("x"));
        Check.equal(Arrays.asList("l@2", "l@2"), log, "the same listener with the same capture flag is registered once (twice with both flags)");
        log.clear();
        span.removeEventListener("x", l, true);
        span.dispatchEvent(new Event("x"));
        Check.equal(Arrays.asList("l@2"), log, "removing with the capture flag removes only that registration");
        span.removeEventListener("x", l, true);
        span.removeEventListener("x", l, false);
        span.removeEventListener("never", l, false);
        span.addEventListener("x", null, false);
        Check.isFalse(span.hasEventListeners("x"), "nothing is left");
        log.clear();
        span.addEventListener("o", logger("once"), false, true, false);
        span.dispatchEvent(new Event("o"));
        span.dispatchEvent(new Event("o"));
        Check.equal(Arrays.asList("once@2"), log, "a once listener runs only once");
        Check.equal(0, span.getEventListenerCount(), "and is removed");
    }

    /**
     * P06-07: listeners removed during a dispatch are skipped, listeners added during a dispatch to the same target do not run in it.
     * <p>
     * P06-07: 전달 중 제거된 리스너는 건너뛰고, 전달 중 같은 대상에 추가된 리스너는 그 전달에서 실행되지 않는다.
     */
    public void testListenerChangesDuringDispatch() {
        final EventListener second = logger("second");
        final EventListener late = logger("late");
        span.addEventListener("x", new EventListener() {
            /**
             * Removes the second listener and adds a late one.
             * <p>
             * 두 번째 리스너를 제거하고 늦은 리스너를 추가한다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add("first");
                span.removeEventListener("x", second, false);
                span.addEventListener("x", late, false);
                body.addEventListener("x", logger("body-added"), false);
            }
        });
        span.addEventListener("x", second, false);
        span.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("first", "body-added@3"), log, "removed listeners are skipped, added ones of the same target wait, later targets see new ones");
        log.clear();
        span.dispatchEvent(new Event("x", false, false));
        Check.equal(Arrays.asList("first", "late@2"), log, "the added listener runs in the next dispatch");
    }

    /**
     * P06-07: a failing listener is reported and the other listeners still run.
     * <p>
     * P06-07: 실패한 리스너는 보고되고 다른 리스너는 계속 실행된다.
     */
    public void testListenerErrorsAreIsolated() {
        final List<Throwable> errors = new ArrayList<Throwable>();
        doc.setListenerErrorHandler(new ListenerErrorHandler() {
            /**
             * Collects the failure.
             * <p>
             * 실패를 모은다.
             *
             * @param target  target / 대상
             * @param event   event / 이벤트
             * @param failure failure / 실패
             */
            public void listenerFailed(EventTarget target, Event event, Throwable failure) {
                errors.add(failure);
            }
        });
        span.addEventListener("x", new EventListener() {
            /**
             * Fails.
             * <p>
             * 실패한다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                throw new IllegalStateException("expected test failure");
            }
        });
        span.addEventListener("x", logger("after"));
        body.addEventListener("x", logger("body"));
        span.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("after@2", "body@3"), log, "other listeners and targets still run");
        Check.equal(1, errors.size(), "the failure was reported");
    }

    /**
     * P06-06: re-dispatching an event during its own dispatch is an InvalidStateError; afterwards the event can be dispatched again.
     * <p>
     * P06-06: 이벤트를 전달 중에 다시 전달하면 InvalidStateError 이고, 끝난 뒤에는 다시 전달할 수 있다.
     */
    public void testReentrancyAndRedispatch() {
        final Event e = new Event("x", false, false);
        final List<String> caught = new ArrayList<String>();
        span.addEventListener("x", new EventListener() {
            /**
             * Tries to dispatch the same event again.
             * <p>
             * 같은 이벤트를 다시 전달하려고 한다.
             *
             * @param ev event / 이벤트
             */
            public void handleEvent(Event ev) {
                try {
                    span.dispatchEvent(ev);
                } catch (DomException ex) {
                    caught.add(ex.getName());
                }
            }
        });
        span.dispatchEvent(e);
        Check.equal(Arrays.asList("InvalidStateError"), caught, "re-entrant dispatch is rejected");
        span.dispatchEvent(e);
        Check.equal(2, caught.size(), "the event can be dispatched again after the dispatch ended");
    }

    /**
     * P06-06: the trust flag, custom event payloads and event creation by interface name.
     * <p>
     * P06-06: 신뢰 플래그, 사용자 정의 이벤트의 값, 인터페이스 이름으로 이벤트 만들기.
     */
    public void testTrustCustomEventAndCreateEvent() {
        final List<Boolean> trusted = new ArrayList<Boolean>();
        span.addEventListener("t", new EventListener() {
            /**
             * Records whether the event is trusted.
             * <p>
             * 이벤트가 신뢰되는지 기록한다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                trusted.add(Boolean.valueOf(e.isTrusted()));
            }
        });
        span.dispatchEvent(new Event("t"));
        span.dispatchTrustedEvent(new Event("t"));
        Check.equal(Arrays.asList(Boolean.FALSE, Boolean.TRUE), trusted, "script events are untrusted, user agent events are trusted");
        CustomEvent ce = new CustomEvent("c", true, false, "payload");
        Check.equal("payload", ce.getDetail(), "custom event detail");
        Event created = doc.createEvent("HTMLEvents");
        created.initEvent("made", true, true);
        Check.equal("made", created.getType(), "createEvent + initEvent");
        Check.isTrue(created.isBubbles() && created.isCancelable(), "flags set by initEvent");
        Check.isTrue(doc.createEvent("customevent") instanceof CustomEvent, "createEvent CustomEvent");
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Creates an unknown event interface.
             * <p>
             * 알 수 없는 이벤트 인터페이스를 만든다.
             */
            public void run() {
                doc.createEvent("FooEvent");
            }
        }, "unknown event interface");
    }

    /**
     * P06-06: events go from the document to its window, except load; detached subtrees stop at their root.
     * <p>
     * P06-06: 이벤트는 문서에서 window 로 가지만 load 는 예외이고, 분리된 하위 트리는 자신의 루트에서 멈춘다.
     */
    public void testDocumentToWindowPathAndDetachedTrees() {
        EventTarget window = new EventTarget() {
        };
        window.addEventListener("x", logger("window"));
        window.addEventListener("load", logger("window-load"));
        doc.setEventWindow(window);
        span.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("window@3"), log, "the window is the last target of the path");
        log.clear();
        span.dispatchEvent(new Event("load", true, false));
        Check.isTrue(log.isEmpty(), "load does not propagate from the document to the window");
        Element loose = doc.createElement("p");
        Element inner = doc.createElement("i");
        loose.appendChild(inner);
        loose.addEventListener("x", logger("loose"));
        inner.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("loose@3"), log, "a detached subtree has its own path that ends at its root");
    }

    /**
     * P06-06: the event path is computed when the dispatch starts, so tree changes during the dispatch do not change the targets.
     * <p>
     * P06-06: 이벤트 경로는 전달이 시작될 때 계산되므로 전달 중 트리 변경이 대상을 바꾸지 않는다.
     */
    public void testPathIsFixedAtDispatchStart() {
        div.addEventListener("x", new EventListener() {
            /**
             * Moves the div out of the tree while the event is travelling.
             * <p>
             * 이벤트가 이동 중일 때 div 를 트리에서 뺀다.
             *
             * @param e event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add("div");
                div.remove();
            }
        });
        body.addEventListener("x", logger("body"));
        span.dispatchEvent(new Event("x", true, false));
        Check.equal(Arrays.asList("div", "body@3"), log, "body still gets the event although the div left the tree");
    }
}
