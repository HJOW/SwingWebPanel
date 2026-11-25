package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the nonblocking cancellation/cleanup split needed by Swing lifecycle calls (P15-08-b-1).
 * <p>Swing 수명 호출에 필요한 비동기 중단·해제 분리 시험.
 */
public class PageDisposalTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public PageDisposalTest() {
    }

    /**
     * A cancellation request completes while a page task still owns the lock; cleanup can follow after that task leaves.
     * <p>페이지 작업이 잠금을 소유한 동안에도 중단 요청은 완료되며 작업 종료 뒤 전체 해제가 가능하다.
     * @throws Exception on failure / 실패 시
     */
    public void testRequestDoesNotWaitForPageLock() throws Exception {
        final PageFixture fixture = new PageFixture().load("<body>before</body>");
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch requested = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Thread owner = new Thread(new Runnable() {
            /** Holds the page lock as an in-flight host task. / 진행 중인 호스트 작업으로 페이지 잠금을 소유한다. */
            public void run() {
                try {
                    fixture.scripts.runTask(new Runnable() {
                        /** Waits while holding script ownership. / 스크립트 소유권을 가진 채 대기한다. */
                        public void run() {
                            locked.countDown();
                            try {
                                release.await(15, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                } catch (Throwable failure) {
                    error.compareAndSet(null, failure);
                }
            }
        }, "disposal-test-owner");
        owner.setDaemon(true);
        Thread requester = new Thread(new Runnable() {
            /** Requests cancellation independently of the owning thread. / 소유 스레드와 독립적으로 중단을 요청한다. */
            public void run() {
                try {
                    fixture.scripts.requestDisposal();
                } catch (Throwable failure) {
                    error.compareAndSet(null, failure);
                } finally {
                    requested.countDown();
                }
            }
        }, "disposal-test-requester");
        requester.setDaemon(true);
        try {
            owner.start();
            Check.isTrue(locked.await(5, TimeUnit.SECONDS), "page lock held");
            requester.start();
            Check.isTrue(requested.await(5, TimeUnit.SECONDS), "request completes before page lock is released");
            Check.equal(1, release.getCount(), "owner still blocked at request completion");
            Check.isTrue(fixture.scripts.isDisposed(), "disposal visible immediately");
        } finally {
            release.countDown();
            owner.join(5000);
            requester.join(5000);
            fixture.scripts.dispose();
        }
        Check.isTrue(!owner.isAlive() && !requester.isAlive(), "test threads terminate");
        Check.isNull(error.get(), "no worker exception");
    }

    /**
     * Already queued callbacks and later evaluation/event entry points stop immediately, even before full cleanup takes place.
     * <p>전체 해제 전에도 이미 대기한 콜백과 후속 평가·이벤트 진입점이 즉시 중지된다.
     */
    public void testRequestSuppressesCallbacksBeforeCleanup() {
        PageFixture fixture = new PageFixture().load("<body onclick=\"console.log('click')\">before<script>"
                + "setTimeout(function(){console.log('timer')},1); requestAnimationFrame(function(){console.log('frame')});</script></body>");
        try {
            fixture.scripts.requestDisposal();
            fixture.scripts.requestDisposal();
            fixture.advance(100);
            fixture.scripts.dispatchEvent(fixture.document.getElementsByTagName("body").item(0), new Event("click", true, true));
            Check.equal("", fixture.log(), "no queued timer/frame/event script");
            Check.isNull(fixture.scripts.evaluate("console.log('eval')", "after-close"), "new evaluation suppressed");
        } finally {
            fixture.scripts.dispose();
            fixture.scripts.dispose();
        }
    }

    /** Direct DOM delivery after a disposal request skips every JS listener while preserving Java listeners. / 폐기 요청 뒤 직접 DOM 전달은 모든 JS 리스너를 건너뛰고 Java 리스너를 유지한다. */
    public void testDirectTrustedInputBeforeCleanup() {
        PageFixture fixture = new PageFixture().load("<body onclick=\"console.log('inline')\">before<script>"
                + "document.body.addEventListener('click',()=>console.log('function'));"
                + "document.body.addEventListener('click',{handleEvent:()=>console.log('object')});</script></body>");
        final int[] javaCalls = new int[1];
        Element body = (Element) fixture.document.getElementsByTagName("body").item(0);
        body.addEventListener("click", new EventListener() {
            /** Records trusted Java input without entering the realm. / 영역에 진입하지 않고 신뢰 Java 입력을 기록한다.
             * @param event input event / 입력 이벤트 */
            public void handleEvent(Event event) {
                Check.isTrue(event.isTrusted(), "trust preserved after cancellation");
                javaCalls[0]++;
            }
        }, false);
        try {
            fixture.scripts.requestDisposal();
            body.dispatchTrustedEvent(new Event("click", true, true));
            fixture.scripts.dispatchTrustedEvent(body, new Event("click", true, true));
            Check.equal(2, javaCalls[0], "Java listeners survive immediate script gate");
            Check.equal("", fixture.log(), "inline/function/object callbacks gated before dispose");
        } finally {
            fixture.scripts.dispose();
        }
    }

    /**
     * A Promise continuation suspended at await never mutates the old document after a disposal request.
     * <p>
     * await 에서 중단된 Promise 이어짐은 폐기 요청 뒤 오래된 문서를 바꾸지 않는다.
     */
    public void testAwaitContinuationStopsAtDisposal() {
        final PageFixture fixture = new PageFixture().load("<body></body>");
        fixture.scripts.runTask(new Runnable() {
            /**
             * Starts an async continuation and cancels the page before its microtask checkpoint.
             * <p>
             * async 이어짐을 시작하고 마이크로태스크 검사점 전에 페이지를 중단한다.
             */
            public void run() {
                fixture.scripts.getRealm().evaluateScript(
                        "globalThis.late='clean'; (async function(){ await 0; document.body.setAttribute('late','ran'); late='ran'; })();",
                        "await-disposal.js");
                fixture.scripts.requestDisposal();
            }
        });
        try {
            Check.isFalse(fixture.document.getElementsByTagName("body").item(0).hasAttribute("late"),
                    "await did not mutate the disposed page");
            Check.equal("clean", fixture.scripts.getRealm().getGlobalObject().get("late").toString(),
                    "await continuation did not update the old global");
        } finally {
            fixture.scripts.dispose();
        }
    }
}
