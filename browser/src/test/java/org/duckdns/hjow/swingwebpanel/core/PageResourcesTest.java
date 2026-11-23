package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.render.ImageResource;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Resource lifecycle tests: failed/damaged/blocked images are terminal, and cancelled pages publish no late callback.
 * <p>리소스 수명 시험: 실패·손상·차단 이미지는 최종 상태가 되며 취소한 페이지는 늦은 콜백을 게시하지 않는다.
 */
public class PageResourcesTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public PageResourcesTest() {
    }

    /**
     * Waits for an image's terminal state without interpreting the resource bytes in the test.
     * <p>시험에서 리소스 바이트를 해석하지 않고 이미지의 최종 상태를 기다린다.
     * @param resources page resources / 페이지 리소스
     * @param url image URL / 이미지 URL
     * @return terminal state / 최종 상태
     * @throws Exception when interrupted / 인터럽트 시
     */
    private static ImageResource awaitImage(PageResources resources, String url) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        ImageResource state = resources.getImage(url);
        // 완료 또는 실패 상태가 게시될 때까지 짧게 기다린다.
        while (state.getState() == ImageResource.State.PENDING && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
            state = resources.getImage(url);
        }
        Check.isTrue(state.getState() != ImageResource.State.PENDING, "terminal image state");
        return state;
    }

    /**
     * Failed URLs, corrupt files and forbidden schemes finish once without retry loops or false success.
     * <p>실패 주소·손상 파일·금지 스키마가 재시도 반복이나 거짓 성공 없이 한 번에 끝난다.
     * @throws Exception on failure / 실패 시
     */
    public void testFailedDamagedAndBlockedImages() throws Exception {
        TestHttpServer server = new TestHttpServer();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        PageResources resources = null;
        try {
            server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/plain", "missing"));
            server.routeFixed("/broken", new TestHttpServer.Response(200).bytes("image/png", new byte[] {1, 2, 3}));
            server.start();
            resources = new PageResources(new ResourceFetcher(null, FetchLimits.DEFAULT, null), new CookieStore(),
                    PageSnapshot.loaded(server.url("/page"), "", "text/html", server.url("/page"), 200), new PageRequestContext(null),
                    workers, new PageResources.Listener() {
                        /** Accepts completion without touching a DOM. / DOM을 건드리지 않고 완료를 받는다.
                         * @param stylesheet stylesheet flag / 시트 표시 */
                        public void resourceChanged(boolean stylesheet) {
                        }
                    });
            Check.equal(ImageResource.State.FAILED, awaitImage(resources, server.url("/missing")).getState(), "HTTP failure");
            Check.equal(ImageResource.State.FAILED, awaitImage(resources, server.url("/broken")).getState(), "damaged image");
            Check.equal(ImageResource.State.FAILED, awaitImage(resources, "file:///blocked.png").getState(), "blocked scheme");
            Check.equal(ImageResource.State.FAILED, awaitImage(resources, "classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html").getState(),
                    "remote page cannot read classpath");
            resources.getImage(server.url("/missing"));
            Check.equal(1, server.count("/missing"), "failed state not fetched again");
            Check.equal(2, server.requests().size(), "blocked requests never reach network");
        } finally {
            if (resources != null) {
                resources.close();
            }
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            server.stop();
        }
    }

    /**
     * Closing a page interrupts outstanding requests and suppresses completion even if the server responds afterwards.
     * <p>페이지를 닫으면 진행 요청을 중단하고 서버가 나중에 응답하더라도 완료 통지를 막는다.
     * @throws Exception on failure / 실패 시
     */
    public void testCancelledPageDoesNotPublish() throws Exception {
        TestHttpServer server = new TestHttpServer();
        ExecutorService workers = Executors.newSingleThreadExecutor();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger callbacks = new AtomicInteger();
        PageResources resources = null;
        try {
            server.route("/slow", new TestHttpServer.Handler() {
                /** Delays a response until the page is closed. / 페이지를 닫을 때까지 응답을 지연한다.
                 * @param request incoming request / 받은 요청
                 * @return damaged image response / 손상 이미지 응답
                 * @throws Exception when interrupted / 인터럽트 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(10, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("image/png", new byte[] {1, 2, 3});
                }
            });
            server.start();
            resources = new PageResources(new ResourceFetcher(null, FetchLimits.DEFAULT, null), new CookieStore(),
                    PageSnapshot.loaded(server.url("/page"), "", "text/html", server.url("/page"), 200), new PageRequestContext(null),
                    workers, new PageResources.Listener() {
                        /** Records any incorrectly delivered completion. / 잘못 전달된 완료를 기록한다.
                         * @param stylesheet stylesheet flag / 시트 표시 */
                        public void resourceChanged(boolean stylesheet) {
                            callbacks.incrementAndGet();
                        }
                    });
            resources.getImage(server.url("/slow"));
            Check.isTrue(entered.await(5, TimeUnit.SECONDS), "request started");
            resources.close();
            release.countDown();
            workers.shutdown();
            Check.isTrue(workers.awaitTermination(10, TimeUnit.SECONDS), "cancelled worker terminates");
            Check.equal(0, callbacks.get(), "no late completion");
            Check.equal(ImageResource.State.FAILED, resources.getImage(server.url("/slow")).getState(), "closed page cannot restart a request");
        } finally {
            release.countDown();
            if (resources != null) {
                resources.close();
            }
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            server.stop();
        }
    }

    /**
     * Creates the resources of a page that is served by a test server, without a DOM and without a listener action.
     * <p>시험 서버가 제공하는 페이지의 리소스를 DOM 과 리스너 동작 없이 만든다.
     * @param server the server / 서버
     * @param workers the executor / 실행기
     * @return the resources / 리소스
     */
    private static PageResources newResources(TestHttpServer server, ExecutorService workers) {
        return new PageResources(new ResourceFetcher(null, FetchLimits.DEFAULT, null), new CookieStore(),
                PageSnapshot.loaded(server.url("/page"), "", "text/html", server.url("/page"), 200), new PageRequestContext(null), workers,
                new PageResources.Listener() {
                    /** Accepts completion without touching a DOM. / DOM 을 건드리지 않고 완료를 받는다.
                     * @param stylesheet stylesheet flag / 시트 표시 */
                    public void resourceChanged(boolean stylesheet) {
                    }
                });
    }

    /**
     * The activity counters (P19-08): a resource counts as started once however often it is asked for, a failed or damaged one counts as
     * ended like a good one, and the page waits only for the ones that did not end.
     * <p>활동 카운터(P19-08): 리소스는 몇 번을 요청해도 한 번 시작한 것으로 세고, 실패하거나 손상된 것도 정상인 것처럼 끝난 것으로 세며, 페이지는 끝나지
     * 않은 것만 기다린다.
     * @throws Exception on failure / 실패 시
     */
    public void testActivityCountsFailuresAsEnded() throws Exception {
        TestHttpServer server = new TestHttpServer();
        ExecutorService workers = Executors.newFixedThreadPool(3);
        final CountDownLatch release = new CountDownLatch(1);
        PageResources resources = null;
        try {
            server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/plain", "missing"));
            server.routeFixed("/broken", new TestHttpServer.Response(200).bytes("image/png", new byte[] {1, 2, 3}));
            server.route("/slow", new TestHttpServer.Handler() {
                /** Holds the response until the test opens the gate. / 시험이 문을 열 때까지 응답을 붙잡는다.
                 * @param request incoming request / 받은 요청
                 * @return damaged image response / 손상 이미지 응답
                 * @throws Exception when interrupted / 인터럽트 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(20, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("image/png", new byte[] {1, 2, 3});
                }
            });
            server.start();
            resources = newResources(server, workers);
            Check.isFalse(resources.hasPending(), "nothing was asked for yet");
            Check.equal(0, resources.activity()[1], "nothing started");
            resources.getImage(server.url("/slow"));
            resources.getImage(server.url("/slow"));
            Check.equal(ImageResource.State.FAILED, awaitImage(resources, server.url("/missing")).getState(), "HTTP failure");
            Check.equal(ImageResource.State.FAILED, awaitImage(resources, server.url("/broken")).getState(), "damaged image");
            long end = System.currentTimeMillis() + 10000;
            // 두 리소스의 끝남이 카운터에 반영될 때까지 짧게 기다린다.
            while (resources.activity()[0] < 2 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(2, resources.activity()[0], "the failed and the damaged image ended");
            Check.equal(3, resources.activity()[1], "three distinct resources were started (the second ask joins the first)");
            Check.isTrue(resources.hasPending(), "the held image is still loading");
            release.countDown();
            awaitImage(resources, server.url("/slow"));
            end = System.currentTimeMillis() + 10000;
            // 마지막 리소스의 끝남이 카운터에 반영될 때까지 짧게 기다린다.
            while (resources.hasPending() && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.isFalse(resources.hasPending(), "everything ended");
            Check.equal(3, resources.activity()[0], "all three ended");
        } finally {
            release.countDown();
            if (resources != null) {
                resources.close();
            }
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            server.stop();
        }
    }

    /**
     * The page limit of distinct resources (P19-05): the ones over the limit fail at once, are not started and not counted, and closing a
     * page ends what was still loading.
     * <p>페이지의 서로 다른 리소스 수 제한(P19-05): 제한을 넘는 것은 곧바로 실패하고 시작하지도 세지도 않으며, 페이지를 닫으면 아직 로딩 중이던 것이 끝난
     * 것으로 센다.
     * @throws Exception on failure / 실패 시
     */
    public void testEntryLimitAndClose() throws Exception {
        TestHttpServer server = new TestHttpServer();
        ExecutorService workers = Executors.newFixedThreadPool(4);
        final CountDownLatch release = new CountDownLatch(1);
        PageResources resources = null;
        try {
            server.route("/m", new TestHttpServer.Handler() {
                /** Holds every response until the test opens the gate. / 시험이 문을 열 때까지 모든 응답을 붙잡는다.
                 * @param request incoming request / 받은 요청
                 * @return not found / 없음
                 * @throws Exception when interrupted / 인터럽트 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(20, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(404).text("text/plain", "no");
                }
            });
            server.start();
            resources = newResources(server, workers);
            ImageResource last = null;
            // 한도(256)보다 많은 서로 다른 주소를 요청한다.
            for (int i = 0; i < 300; i++) {
                last = resources.getImage(server.url("/m?n=" + i));
            }
            Check.equal(ImageResource.State.FAILED, last.getState(), "the resource over the limit fails at once");
            Check.equal(256, resources.activity()[1], "only the resources within the limit were started");
            Check.isTrue(resources.hasPending(), "they are still loading");
            resources.close();
            Check.isFalse(resources.hasPending(), "closing the page ends what was loading");
            Check.equal(256, resources.activity()[0], "and counts it as ended");
        } finally {
            release.countDown();
            if (resources != null) {
                resources.close();
            }
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            server.stop();
        }
    }
}
