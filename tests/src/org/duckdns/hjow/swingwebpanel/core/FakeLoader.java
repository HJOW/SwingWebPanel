package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A scriptable {@link PageLoader} for tests. Every URL loads immediately into a page whose source is
 * <code>"content of " + url</code>, unless a gate, a redirect or a failure was registered for it. Gated URLs block until
 * {@link #release(String)} is called; whether a gated load notices cancellation can be chosen, so both the cooperative path
 * (the loader stops) and the stale-completion path (the loader finishes late) can be tested.
 * <p>
 * 시험용으로 동작을 지정할 수 있는 {@link PageLoader}. 게이트, 리디렉션, 실패를 등록하지 않은 URL 은 원문이
 * <code>"content of " + url</code> 인 페이지로 즉시 읽힌다. 게이트가 걸린 URL 은 {@link #release(String)} 가 호출될 때까지 막히며,
 * 막힌 로딩이 취소를 알아차릴지 선택할 수 있어 협조적 경로(로더가 멈춤)와 오래된 완료 경로(로더가 늦게 끝남)를 모두 시험할 수 있다.
 */
public class FakeLoader implements PageLoader {

    /**
     * Counts the documents of this loader that were disposed.
     * <p>
     * 이 로더가 만든 문서 중 해제된 문서의 수를 센다.
     */
    public final AtomicInteger disposed = new AtomicInteger();

    /**
     * Requests in the order they reached the loader.
     * <p>
     * 로더에 도착한 순서대로의 요청.
     */
    public final List<NavigationRequest> requests = Collections.synchronizedList(new ArrayList<NavigationRequest>());

    /**
     * Gates by URL.
     * <p>
     * URL 별 게이트.
     */
    private final Map<String, CountDownLatch> gates = Collections.synchronizedMap(new HashMap<String, CountDownLatch>());

    /**
     * Redirect targets by URL.
     * <p>
     * URL 별 리디렉션 대상.
     */
    private final Map<String, String> redirects = Collections.synchronizedMap(new HashMap<String, String>());

    /**
     * Failures to throw by URL.
     * <p>
     * URL 별로 던질 실패.
     */
    private final Map<String, RuntimeException> failures = Collections.synchronizedMap(new HashMap<String, RuntimeException>());

    /**
     * Whether gated loads ignore cancellation (to simulate a loader that finishes late).
     * <p>
     * 게이트가 걸린 로딩이 취소를 무시할지 여부(늦게 끝나는 로더를 흉내 냄).
     */
    public volatile boolean ignoreCancel;

    /**
     * Creates a loader.
     * <p>
     * 로더를 만든다.
     */
    public FakeLoader() {
    }

    /**
     * A document that counts its disposal.
     * <p>
     * 해제 횟수를 세는 문서.
     */
    public static final class CountingDocument implements PageDocument {
        /**
         * Counter of the owning loader.
         * <p>
         * 이 문서를 만든 로더의 카운터.
         */
        private final AtomicInteger owner;

        /**
         * Creates a document that counts into the given counter.
         * <p>
         * 주어진 카운터에 세는 문서를 만든다.
         *
         * @param owner counter of the loader / 로더의 카운터
         */
        public CountingDocument(AtomicInteger owner) {
            this.owner = owner;
        }

        /**
         * Number of dispose calls on this document.
         * <p>
         * 이 문서에 대한 dispose 호출 횟수.
         */
        public final AtomicInteger disposeCalls = new AtomicInteger();

        /**
         * Counts the call.
         * <p>
         * 호출을 센다.
         */
        public void dispose() {
            disposeCalls.incrementAndGet();
            owner.incrementAndGet();
        }
    }

    /**
     * Makes a URL block until released.
     * <p>
     * URL 이 해제될 때까지 막히게 한다.
     *
     * @param url URL to gate / 막을 URL
     */
    public void gate(String url) {
        gates.put(url, new CountDownLatch(1));
    }

    /**
     * Lets a gated URL continue.
     * <p>
     * 막힌 URL 을 계속 진행시킨다.
     *
     * @param url gated URL / 막힌 URL
     */
    public void release(String url) {
        CountDownLatch g = gates.get(url);
        if (g != null) {
            g.countDown();
        }
    }

    /**
     * Makes a URL answer with the page of another URL.
     * <p>
     * URL 이 다른 URL 의 페이지로 응답하게 한다.
     *
     * @param from requested URL / 요청 URL
     * @param to   final URL / 최종 URL
     */
    public void redirect(String from, String to) {
        redirects.put(from, to);
    }

    /**
     * Makes a URL fail with an exception.
     * <p>
     * URL 이 예외로 실패하게 한다.
     *
     * @param url URL / URL
     * @param e   exception to throw / 던질 예외
     */
    public void fail(String url, RuntimeException e) {
        failures.put(url, e);
    }

    /**
     * Returns the number of requests seen so far.
     * <p>
     * 지금까지 받은 요청 수를 반환한다.
     *
     * @return request count / 요청 수
     */
    public int requestCount() {
        return requests.size();
    }

    /**
     * Loads a page according to the registered script.
     * <p>
     * 등록된 지시에 따라 페이지를 읽는다.
     *
     * @param r request / 요청
     * @return the page / 페이지
     * @throws Exception on cancellation or scripted failure / 취소나 지시된 실패
     */
    public PageSnapshot load(NavigationRequest r) throws Exception {
        requests.add(r);
        String url = r.getUrl();
        CountDownLatch g = gates.get(url);
        if (g != null) {
            // 게이트가 열릴 때까지 짧게 기다리며, 설정에 따라 취소를 확인한다.
            while (!g.await(10, TimeUnit.MILLISECONDS)) {
                if (!ignoreCancel) {
                    r.getCancelToken().throwIfCancelled();
                }
            }
        }
        RuntimeException failure = failures.get(url);
        if (failure != null) {
            throw failure;
        }
        if ("about:blank".equals(url)) {
            return PageSnapshot.blank();
        }
        if ("about:text".equals(url)) {
            return PageSnapshot.text(r.getContent(), r.getMimeType());
        }
        String finalUrl = redirects.containsKey(url) ? redirects.get(url) : url;
        return PageSnapshot.loaded(finalUrl, "content of " + finalUrl, "text/html", finalUrl, 200).withDocument(new CountingDocument(disposed));
    }
}
