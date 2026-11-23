package org.duckdns.hjow.swingwebpanel.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers shared by the tests of the core package.
 * <p>
 * core 패키지 시험들이 공유하는 도우미.
 */
public final class TestSupport {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TestSupport() {
    }

    /**
     * Waits until everything that was already queued on the event dispatch thread has run (twice, because handlers may queue more).
     * <p>
     * 이벤트 디스패치 스레드에 이미 들어 있는 작업이 모두 실행될 때까지 기다린다(처리기가 작업을 더 넣을 수 있어 두 번 한다).
     */
    public static void flushEdt() {
        try {
            // 처리기가 작업을 더 넣을 수 있으므로 두 번 비운다.
            for (int i = 0; i < 2; i++) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    /**
                     * Marker that does nothing.
                     * <p>
                     * 아무것도 하지 않는 표식.
                     */
                    public void run() {
                    }
                });
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits until the document thread is idle and all EDT notifications were delivered.
     * <p>
     * 문서 스레드가 놀고 모든 EDT 통지가 전달될 때까지 기다린다.
     *
     * @param s session / 세션
     */
    public static void settle(BrowserSession s) {
        Check.isTrue(s.awaitIdle(10000), "document thread must become idle");
        flushEdt();
    }


    /**
     * Waits until the confirmed page has the given URL (for situations where an unrelated load is still blocked).
     * <p>
     * 확정된 페이지가 주어진 URL 이 될 때까지 기다린다(관계없는 읽기가 아직 막혀 있는 상황용).
     *
     * @param s   session / 세션
     * @param url expected URL / 기대하는 URL
     */
    public static void waitForPage(BrowserSession s, String url) {
        long end = System.currentTimeMillis() + 5000;
        // 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(s.getPage()) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Check.equal(url, s.getPage(), "page must be confirmed");
        flushEdt();
    }
    /**
     * A listener that records every notification as text and remembers whether all of them ran on the EDT.
     * <p>
     * 모든 통지를 문자열로 기록하고 전부 EDT 에서 실행되었는지 기억하는 리스너.
     */
    public static final class Recorder implements SessionListener {
        /**
         * Recorded notifications.
         * <p>
         * 기록된 통지.
         */
        public final List<String> events = Collections.synchronizedList(new ArrayList<String>());

        /**
         * True while every call so far happened on the EDT.
         * <p>
         * 지금까지의 모든 호출이 EDT 에서 일어났으면 true.
         */
        public volatile boolean allOnEdt = true;

        /**
         * Notes the thread of a call.
         * <p>
         * 호출한 스레드를 기록한다.
         */
        private void thread() {
            if (!SwingUtilities.isEventDispatchThread()) {
                allOnEdt = false;
            }
        }

        /**
         * Records a confirmed page.
         * <p>
         * 확정된 페이지를 기록한다.
         *
         * @param oldUrl   previous URL / 이전 URL
         * @param snapshot new page / 새 페이지
         */
        public void pageConfirmed(String oldUrl, PageSnapshot snapshot) {
            thread();
            events.add("confirmed " + snapshot.getUrl());
        }

        /**
         * Records a navigation inside the page and the scroll target that came with it.
         * <p>
         * 페이지 안의 이동과 함께 온 스크롤 대상을 기록한다.
         *
         * @param oldUrl   previous URL / 이전 URL
         * @param snapshot the page / 페이지
         * @param target   the target box or null / 대상 상자. 없으면 null
         */
        public void fragmentChanged(String oldUrl, PageSnapshot snapshot, double[] target) {
            thread();
            events.add("fragment " + snapshot.getUrl() + (target == null ? " no-target" : " y=" + Math.round(target[1])));
        }

        /**
         * Records a scroll request of a script.
         * <p>
         * 스크립트의 스크롤 요청을 기록한다.
         *
         * @param x the x / x
         * @param y the y / y
         */
        public void scrollRequested(double x, double y) {
            thread();
            events.add("scroll " + Math.round(x) + "," + Math.round(y));
        }

        /**
         * Records a URL change.
         * <p>
         * URL 변경을 기록한다.
         *
         * @param oldUrl previous URL / 이전 URL
         * @param newUrl new URL / 새 URL
         */
        public void pageChanged(String oldUrl, String newUrl) {
            thread();
            events.add("page " + oldUrl + " -> " + newUrl);
        }

        /**
         * Records a title change.
         * <p>
         * 제목 변경을 기록한다.
         *
         * @param oldTitle previous title / 이전 제목
         * @param newTitle new title / 새 제목
         */
        public void titleChanged(String oldTitle, String newTitle) {
            thread();
            events.add("title " + newTitle);
        }

        /**
         * Records a status text change.
         * <p>
         * 상태 메시지 변경을 기록한다.
         *
         * @param oldText previous text / 이전 메시지
         * @param newText new text / 새 메시지
         */
        public void statusTextChanged(String oldText, String newText) {
            thread();
            events.add("status " + newText);
        }

        /**
         * Records a progress change.
         * <p>
         * 진행률 변경을 기록한다.
         *
         * @param oldValue previous progress / 이전 진행률
         * @param newValue new progress / 새 진행률
         */
        public void progressChanged(int oldValue, int newValue) {
            thread();
            events.add("progress " + newValue);
        }
    }
}
