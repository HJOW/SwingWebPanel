package org.duckdns.hjow.swingwebpanel.download;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of the download manager (plan item P11-09, API contract section 7) with a replaced popup and real local servers: full and
 * unknown-size downloads, request headers, failures, cancellation in the middle of a transfer, the confirmation before replacing a
 * file, several downloads at once, <code>cancelAll</code>, changing the target, the headless refusal and the partial file cleanup.
 * The real dialog is checked separately in {@link SwingDownloadDialogTest} and by the manual example.
 * <p>
 * 팝업을 바꾼 다운로드 관리자와 실제 로컬 서버로 하는 시험(계획 P11-09, API 계약 7절): 전체 다운로드, 크기를 모르는 다운로드, 요청 헤더, 실패,
 * 전송 도중 취소, 파일을 교체하기 전의 확인, 여러 다운로드의 동시 처리, <code>cancelAll</code>, 대상 변경, 헤드리스 거부, 부분 파일 정리. 실제
 * 대화상자는 {@link SwingDownloadDialogTest} 와 수동 예제로 따로 확인한다.
 */
public class DownloadManagerTest {

    /** Longest time to wait for a download in the tests. 시험에서 다운로드를 기다리는 최대 시간. */
    private static final long WAIT_MS = 30000;

    /** A popup that records everything and answers questions as configured. 모든 것을 기록하고 설정대로 질문에 답하는 팝업. */
    static final class FakeHandle implements DownloadHandle {
        /** What happened, in order. 일어난 일(순서대로). */
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());

        /** The control the manager gave. 관리자가 준 제어 객체. */
        final DownloadControl control;

        /** The request. 요청. */
        final DownloadRequest request;

        /** The last state. 마지막 상태. */
        volatile DownloadState state;

        /** The last result line. 마지막 결과 줄. */
        volatile String message = "";

        /** The last reported progress. 마지막으로 보고된 진행 상황. */
        volatile long done = -1;

        /** The total of the last progress report. 마지막 진행 보고의 전체 크기. */
        volatile long total = -2;

        /** The last target shown. 마지막으로 보여 준 대상. */
        volatile File target;

        /** True after dispose. dispose 뒤에는 true. */
        volatile boolean disposed;

        /** Answer to the overwrite question. 덮어쓰기 질문의 답. */
        volatile boolean confirm;

        /** Answer to the choose-target question. 대상 선택 질문의 답. */
        volatile File chosen;

        /** Called once on the first progress report. 첫 진행 보고에서 한 번 호출된다. */
        volatile Runnable onFirstProgress;

        /** Whether the first progress was seen. 첫 진행을 보았는지 여부. */
        private boolean seenProgress;

        /**
         * Creates a handle.
         * <p>
         * 핸들을 만든다.
         *
         * @param request the request / 요청
         * @param control the control / 제어 객체
         */
        FakeHandle(DownloadRequest request, DownloadControl control) {
            this.request = request;
            this.control = control;
        }

        /**
         * Records the target.
         * <p>
         * 대상을 기록한다.
         *
         * @param t the target / 대상
         */
        public void setTarget(File t) {
            target = t;
            events.add("target");
        }

        /**
         * Records the progress and runs the first-progress hook.
         * <p>
         * 진행을 기록하고 첫 진행 훅을 실행한다.
         *
         * @param d bytes done / 받은 바이트
         * @param t total / 전체
         */
        public void setProgress(long d, long t) {
            done = d;
            total = t;
            events.add("progress");
            if (!seenProgress) {
                seenProgress = true;
                Runnable r = onFirstProgress;
                if (r != null) {
                    r.run();
                }
            }
        }

        /**
         * Records the state.
         * <p>
         * 상태를 기록한다.
         *
         * @param s the state / 상태
         * @param m the message / 메시지
         */
        public void setState(DownloadState s, String m) {
            state = s;
            message = m;
            events.add("state:" + s);
        }

        /**
         * Answers the choose-target question.
         * <p>
         * 대상 선택 질문에 답한다.
         *
         * @param current the current target / 현재 대상
         * @return the configured answer / 설정한 답
         */
        public File chooseTarget(File current) {
            events.add("choose");
            return chosen;
        }

        /**
         * Answers the overwrite question.
         * <p>
         * 덮어쓰기 질문에 답한다.
         *
         * @param existing the existing file / 기존 파일
         * @return the configured answer / 설정한 답
         */
        public boolean confirmOverwrite(File existing) {
            events.add("confirm");
            return confirm;
        }

        /**
         * Records the disposal.
         * <p>
         * 해제를 기록한다.
         */
        public void dispose() {
            disposed = true;
            events.add("dispose");
        }
    }

    /** The manager under test with a replaced popup and directory. 팝업과 디렉터리를 바꾼 시험 대상 관리자. */
    private class TestManager extends SwingDownloadHandler {
        /** Popups created, in order. 만든 팝업(순서대로). */
        final List<FakeHandle> handles = Collections.synchronizedList(new ArrayList<FakeHandle>());

        /** Hook that configures new popups. 새 팝업을 설정하는 훅. */
        volatile Configurer configurer;

        /** Pretend to be headless. 헤드리스인 척한다. */
        volatile boolean headless;

        /**
         * Creates the manager.
         * <p>
         * 관리자를 만든다.
         */
        TestManager() {
            super(null, new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, DownloadManagerTest.class.getClassLoader()),
                    cookies, new DownloadStatusSink() {
                        /**
                         * Records the message.
                         * <p>
                         * 메시지를 기록한다.
                         *
                         * @param message the message / 메시지
                         */
                        public void downloadStatus(String message) {
                            messages.add(message);
                        }
                    });
        }

        /**
         * Creates a recording popup.
         * <p>
         * 기록하는 팝업을 만든다.
         *
         * @param request the request / 요청
         * @param control the control / 제어 객체
         * @return the popup / 팝업
         */
        protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
            FakeHandle h = new FakeHandle(request, control);
            Configurer c = configurer;
            if (c != null) {
                c.configure(h);
            }
            handles.add(h);
            return h;
        }

        /**
         * Returns the directory of the case.
         * <p>
         * 사례의 디렉터리를 반환한다.
         *
         * @return directory / 디렉터리
         */
        protected File getDefaultDirectory() {
            return downloadDir;
        }

        /**
         * Returns the configured headless flag.
         * <p>
         * 설정한 헤드리스 값을 반환한다.
         *
         * @return the flag / 값
         */
        protected boolean isHeadless() {
            return headless;
        }
    }

    /** Configures a new popup. 새 팝업을 설정한다. */
    private interface Configurer {
        /**
         * Configures a popup.
         * <p>
         * 팝업을 설정한다.
         *
         * @param h the popup / 팝업
         */
        void configure(FakeHandle h);
    }

    /** Directory in which the downloads of the case are saved. 사례의 다운로드를 저장하는 디렉터리. */
    private File downloadDir;

    /** Status messages of the manager. 관리자의 상태 메시지. */
    private final List<String> messages = Collections.synchronizedList(new ArrayList<String>());

    /** The cookies of the manager. 관리자의 쿠키. */
    private CookieStore cookies;

    /** The manager of the case. 사례의 관리자. */
    private TestManager manager;

    /** The general test server. 일반 시험 서버. */
    private TestHttpServer http;

    /** The holding server or null. 붙잡는 서버. 없으면 null. */
    private HoldingServer holding;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DownloadManagerTest() {
    }

    /**
     * Prepares the directory, the servers and the manager of a case.
     * <p>
     * 사례의 디렉터리, 서버, 관리자를 준비한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        downloadDir = File.createTempFile("swp-dl-", ".d");
        downloadDir.delete();
        downloadDir.mkdirs();
        cookies = new CookieStore();
        manager = new TestManager();
        http = new TestHttpServer();
        http.start();
    }

    /**
     * Stops everything and removes the directory.
     * <p>
     * 모든 것을 멈추고 디렉터리를 지운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void tearDown() throws Exception {
        manager.dispose();
        manager.awaitIdle(10000);
        http.stop();
        if (holding != null) {
            holding.stop();
        }
        deleteTree(downloadDir);
    }

    /**
     * Deletes a directory tree.
     * <p>
     * 디렉터리 트리를 지운다.
     *
     * @param f the file or directory / 파일 또는 디렉터리
     */
    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            // 하위 항목을 먼저 지운다.
            for (int i = 0; i < children.length; i++) {
                deleteTree(children[i]);
            }
        }
        f.delete();
    }

    /**
     * Waits for all queued event dispatch thread work.
     * <p>
     * 쌓인 이벤트 디스패치 스레드 작업이 모두 끝나기를 기다린다.
     *
     * @throws Exception on failure / 실패 시
     */
    private static void flushEdt() throws Exception {
        // 처리기가 작업을 더 넣을 수 있으므로 두 번 비운다.
        for (int i = 0; i < 2; i++) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Marker that does nothing.
                 * <p>
                 * 아무것도 하지 않는 표식.
                 */
                public void run() {
                    return;
                }
            });
        }
    }

    /**
     * Starts a download and waits until it has ended.
     * <p>
     * 다운로드를 시작하고 끝날 때까지 기다린다.
     *
     * @param req the request / 요청
     * @return the popup of the download / 다운로드의 팝업
     * @throws Exception on failure / 실패 시
     */
    private FakeHandle run(DownloadRequest req) throws Exception {
        int before = manager.handles.size();
        Check.notNull(manager.start(req), "the download was accepted");
        Check.isTrue(manager.awaitIdle(WAIT_MS), "the download ended in time");
        flushEdt();
        Check.equal(before + 1, manager.handles.size(), "one popup was created");
        return manager.handles.get(before);
    }

    /**
     * Creates a request for a path of the test server.
     * <p>
     * 시험 서버의 경로에 대한 요청을 만든다.
     *
     * @param path the path / 경로
     * @param cd   Content-Disposition or null / Content-Disposition 또는 null
     * @return the request / 요청
     */
    private DownloadRequest request(String path, String cd) {
        return new DownloadRequest(http.url(path), null, "application/octet-stream", cd, -1, "dl-test/1", null);
    }

    /**
     * Makes a body of a given size with a recognizable pattern.
     * <p>
     * 알아볼 수 있는 패턴으로 주어진 크기의 본문을 만든다.
     *
     * @param n the size / 크기
     * @return the bytes / 바이트
     */
    private static byte[] body(int n) {
        byte[] b = new byte[n];
        // 위치에 따라 달라지는 값을 채운다.
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (i * 31 + i / 251);
        }
        return b;
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f the file / 파일
     * @return the bytes / 바이트
     * @throws Exception on failure / 실패 시
     */
    private static byte[] readBytes(File f) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Checks that the download directory holds exactly the given files (no partial or helper files).
     * <p>
     * 다운로드 디렉터리에 주어진 파일들만 있는지(부분 파일이나 보조 파일이 없는지) 확인한다.
     *
     * @param names the expected names / 기대하는 이름
     */
    private void onlyFiles(String... names) {
        List<String> have = new ArrayList<String>(java.util.Arrays.asList(downloadDir.list()));
        Collections.sort(have);
        List<String> want = new ArrayList<String>(java.util.Arrays.asList(names));
        Collections.sort(want);
        Check.equal(want.toString(), have.toString(), "files in the download directory");
    }

    /**
     * A known-size download: the file has the exact content, the popup saw the states in order and the progress ended at the total.
     * <p>
     * 크기를 아는 다운로드: 파일의 내용이 정확하고 팝업은 상태를 순서대로 보았으며 진행이 전체에서 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFullDownload() throws Exception {
        byte[] data = body(300000);
        http.routeFixed("/file", new TestHttpServer.Response(200).bytes("application/octet-stream", data));
        FakeHandle h = run(request("/file", "attachment; filename=\"data.bin\""));
        Check.equal(DownloadState.COMPLETED, h.state, "state");
        File f = new File(downloadDir, "data.bin");
        Check.isTrue(java.util.Arrays.equals(data, readBytes(f)), "content");
        Check.equal(f.getAbsoluteFile(), h.target.getAbsoluteFile(), "the popup shows the target");
        Check.equal(300000L, h.done, "progress ends at the total");
        Check.equal(300000L, h.total, "total is known");
        Check.isTrue(h.message.startsWith("Saved to "), "result line: " + h.message);
        Check.isTrue(h.events.indexOf("state:STARTING") < h.events.indexOf("state:RUNNING")
                && h.events.indexOf("state:RUNNING") < h.events.indexOf("state:COMPLETED"), "states in order: " + h.events);
        Check.isFalse(h.disposed, "a finished popup stays open for the user");
        onlyFiles("data.bin");
        Check.isTrue(messages.get(0).startsWith("Download started: data.bin"), "status: " + messages);
        Check.isTrue(messages.get(messages.size() - 1).startsWith("Download completed: "), "status: " + messages);
    }

    /**
     * A download of unknown size (the body ends when the connection closes) reports an indeterminate total.
     * <p>
     * 크기를 모르는 다운로드(연결이 닫힐 때 본문이 끝남)는 불확정 전체 크기를 보고한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnknownSize() throws Exception {
        byte[] data = body(50000);
        TestHttpServer.Response r = new TestHttpServer.Response(200).bytes("application/octet-stream", data);
        r.noLength = true;
        http.routeFixed("/unknown", r);
        FakeHandle h = run(request("/unknown", null));
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        Check.isTrue(java.util.Arrays.equals(data, readBytes(new File(downloadDir, "unknown"))), "content, named after the URL");
        Check.equal(-1L, h.total, "the total is unknown");
        Check.equal(50000L, h.done, "all bytes were counted");
    }

    /**
     * An empty body gives an empty file.
     * <p>
     * 빈 본문은 빈 파일이 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEmptyFile() throws Exception {
        http.routeFixed("/empty", new TestHttpServer.Response(200).bytes("application/octet-stream", new byte[0]));
        FakeHandle h = run(request("/empty", null));
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        Check.equal(0L, new File(downloadDir, "empty").length(), "empty file");
    }

    /**
     * The user agent and the extra headers of the component are sent, and cookies of the response go to the cookie store.
     * <p>
     * 컴포넌트의 User-Agent 와 추가 헤더가 전송되고 응답의 쿠키는 쿠키 저장소로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRequestLayerIsUsed() throws Exception {
        http.routeFixed("/h", new TestHttpServer.Response(200).bytes("application/octet-stream", body(10)).header("Set-Cookie", "k=v; Path=/"));
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("X-Download-Test", "yes");
        RequestHeaders rh = new RequestHeaders();
        rh.addAll(extra);
        DownloadRequest req = new DownloadRequest(http.url("/h"), null, "", null, -1, "my-agent/9", rh);
        FakeHandle h = run(req);
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        TestHttpServer.Request seen = http.last("/h");
        Check.equal("my-agent/9", seen.header("User-Agent"), "user agent");
        Check.equal("yes", seen.header("X-Download-Test"), "extra header");
        Check.contains(cookies.getCookies(), "k=v", "cookie of the response is stored");
        run(request("/h", null));
        Check.contains(http.last("/h").header("Cookie") == null ? "" : http.last("/h").header("Cookie"), "k=v", "the stored cookie is sent next time");
    }

    /**
     * A server error does not produce a file and the result line names the status.
     * <p>
     * 서버 오류는 파일을 만들지 않으며 결과 줄에 상태가 나온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpErrors() throws Exception {
        http.routeFixed("/e500", new TestHttpServer.Response(500).text("text/html", "<html>broken</html>"));
        FakeHandle h = run(request("/e500", null));
        Check.equal(DownloadState.FAILED, h.state, "state");
        Check.contains(h.message, "500", "the status is named: " + h.message);
        h = run(request("/missing", null));
        Check.equal(DownloadState.FAILED, h.state, "404 state");
        Check.contains(h.message, "404", "the status is named: " + h.message);
        onlyFiles();
        Check.isTrue(messages.get(messages.size() - 1).startsWith("Download failed: "), "status: " + messages);
    }

    /**
     * A response that ends early (declared length not reached) and a refused connection fail and leave no partial file.
     * <p>
     * 일찍 끝난 응답(선언한 길이에 못 미침)과 거부된 연결은 실패하며 부분 파일을 남기지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenTransfers() throws Exception {
        http.routeFixed("/cut", new TestHttpServer.Response(200).bytes("application/octet-stream", body(5000)).truncateAt(1000));
        FakeHandle h = run(request("/cut", null));
        Check.equal(DownloadState.FAILED, h.state, "truncated: " + h.message);
        onlyFiles();
        http.routeFixed("/drop", new TestHttpServer.Response(200));
        TestHttpServer dead = new TestHttpServer();
        dead.start();
        String url = dead.url("/x");
        dead.stop();
        h = run(new DownloadRequest(url, null, "", null, -1, null, null));
        Check.equal(DownloadState.FAILED, h.state, "connection refused: " + h.message);
        onlyFiles();
    }

    /**
     * A blocked scheme is refused by the request layer (nothing is written).
     * <p>
     * 차단된 스킴은 요청 계층이 거부한다(아무것도 쓰지 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedScheme() throws Exception {
        FakeHandle h = run(new DownloadRequest("file:///etc/passwd", null, "", null, -1, null, null));
        Check.equal(DownloadState.FAILED, h.state, "file: is not allowed: " + h.message);
        onlyFiles();
    }

    /**
     * Cancelling in the middle of a transfer (the server holds the connection) ends the download, closes the connection and deletes the
     * partial file.
     * <p>
     * 전송 도중(서버가 연결을 붙잡고 있음) 취소하면 다운로드가 끝나고 연결이 닫히며 부분 파일이 지워진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelInTheMiddle() throws Exception {
        holding = new HoldingServer(1000000, 200000);
        manager.configurer = new Configurer() {
            /**
             * Cancels at the first progress report.
             * <p>
             * 첫 진행 보고에서 취소한다.
             *
             * @param h the popup / 팝업
             */
            public void configure(final FakeHandle h) {
                h.onFirstProgress = new Runnable() {
                    /**
                     * Cancels through the control.
                     * <p>
                     * 제어 객체로 취소한다.
                     */
                    public void run() {
                        h.control.cancel();
                    }
                };
            }
        };
        FakeHandle h = run(new DownloadRequest(holding.url("/slow.bin"), null, "", null, -1, null, null));
        Check.equal(DownloadState.CANCELLED, h.state, "state: " + h.message);
        onlyFiles();
        long end = System.currentTimeMillis() + 10000;
        // 서버가 연결이 닫힌 것을 알아차릴 때까지 기다린다.
        while (holding.getClientsClosed() == 0 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.equal(1, holding.getClientsClosed(), "the connection was closed");
        Check.equal(0, holding.getCompleted(), "the response was not completed");
        Check.isTrue(messages.get(messages.size() - 1).startsWith("Download cancelled: "), "status: " + messages);
    }

    /**
     * Cancelling the manager (clean or dispose of the component) cancels all running downloads, closes their popups and deletes their
     * partial files, but never deletes a file that was already completed.
     * <p>
     * 관리자를 취소하면(컴포넌트의 clean 이나 dispose) 실행 중인 모든 다운로드가 취소되고 팝업이 닫히며 부분 파일이 지워지지만 이미 완료된 파일은
     * 지우지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelAll() throws Exception {
        http.routeFixed("/done", new TestHttpServer.Response(200).bytes("application/octet-stream", body(1000)));
        FakeHandle done = run(request("/done", null));
        Check.equal(DownloadState.COMPLETED, done.state, "a finished download");
        holding = new HoldingServer(500000, 100000);
        Check.notNull(manager.start(new DownloadRequest(holding.url("/a.bin"), null, "", null, -1, null, null)), "first started");
        Check.notNull(manager.start(new DownloadRequest(holding.url("/b.bin"), null, "", null, -1, null, null)), "second started");
        long end = System.currentTimeMillis() + 20000;
        // 두 다운로드가 모두 연결되어 붙잡힐 때까지 기다린다.
        while (holding.getConnections() < 2 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.equal(2, holding.getConnections(), "both transfers are in progress");
        Check.equal(2, manager.getActiveCount(), "two active downloads");
        manager.cancelAll();
        Check.isTrue(manager.awaitIdle(WAIT_MS), "all transfers ended");
        flushEdt();
        Check.isTrue(manager.handles.get(1).disposed && manager.handles.get(2).disposed, "the popups were closed");
        Check.isFalse(done.disposed, "popups of finished downloads are left to the user");
        onlyFiles("done");
        Check.equal(1000, readBytes(new File(downloadDir, "done")).length, "the completed file is intact");
    }

    /**
     * After dispose the manager accepts nothing; in a headless environment a download is refused with a status message and no popup.
     * <p>
     * dispose 뒤에는 관리자가 아무것도 받지 않는다. 헤드리스 환경에서는 다운로드를 상태 메시지와 함께 거부하며 팝업도 만들지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeadlessAndDispose() throws Exception {
        http.routeFixed("/x", new TestHttpServer.Response(200).bytes("application/octet-stream", body(10)));
        manager.headless = true;
        Check.isNull(manager.start(request("/x", null)), "refused when headless");
        flushEdt();
        Check.equal(0, manager.handles.size(), "no popup");
        Check.equal(0, http.count("/x"), "nothing was requested");
        Check.isTrue(messages.get(0).startsWith("Download cancelled"), "status: " + messages);
        onlyFiles();
        manager.headless = false;
        manager.dispose();
        Check.isNull(manager.start(request("/x", null)), "refused after dispose");
        Check.equal(0, http.count("/x"), "still nothing requested");
    }

    /**
     * An existing target is replaced only after the user confirmed; a refusal leads to the choose-target question.
     * <p>
     * 기존 대상은 사용자가 확인한 뒤에만 교체한다. 거부하면 대상 선택 질문으로 이어진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOverwriteConfirmation() throws Exception {
        http.routeFixed("/o", new TestHttpServer.Response(200).bytes("application/octet-stream", "NEW".getBytes("UTF-8")));
        final File existing = new File(downloadDir, "o");
        SafeFilesTest.write(existing, "OLD");
        manager.configurer = new Configurer() {
            /**
             * Answers yes.
             * <p>
             * 예로 답한다.
             *
             * @param h the popup / 팝업
             */
            public void configure(FakeHandle h) {
                h.confirm = true;
            }
        };
        FakeHandle h = run(request("/o", null));
        Check.equal(DownloadState.COMPLETED, h.state, "confirmed: " + h.message);
        Check.isTrue(h.events.contains("confirm"), "the user was asked");
        Check.equal("NEW", SafeFilesTest.read(existing), "replaced");
        onlyFiles("o");
        SafeFilesTest.write(existing, "OLD");
        final File other = new File(downloadDir, "other.txt");
        manager.configurer = new Configurer() {
            /**
             * Answers no and chooses another file.
             * <p>
             * 아니오로 답하고 다른 파일을 고른다.
             *
             * @param f the popup / 팝업
             */
            public void configure(FakeHandle f) {
                f.confirm = false;
                f.chosen = other;
            }
        };
        h = run(request("/o", null));
        Check.equal(DownloadState.COMPLETED, h.state, "saved elsewhere: " + h.message);
        Check.equal("OLD", SafeFilesTest.read(existing), "the existing file is untouched");
        Check.equal("NEW", SafeFilesTest.read(other), "saved under the chosen name");
        Check.equal(other.getAbsoluteFile(), h.target.getAbsoluteFile(), "the popup shows the new target");
        onlyFiles("o", "other.txt");
        manager.configurer = new Configurer() {
            /**
             * Answers no and gives up.
             * <p>
             * 아니오로 답하고 포기한다.
             *
             * @param f the popup / 팝업
             */
            public void configure(FakeHandle f) {
                f.confirm = false;
                f.chosen = null;
            }
        };
        h = run(request("/o", null));
        Check.equal(DownloadState.CANCELLED, h.state, "given up: " + h.message);
        Check.equal("OLD", SafeFilesTest.read(existing), "the existing file is still untouched");
        onlyFiles("o", "other.txt");
    }

    /**
     * Several downloads run at the same time, each with its own popup, and all files are complete.
     * <p>
     * 여러 다운로드가 동시에 실행되며 각각 자신의 팝업이 있고 모든 파일이 완전하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConcurrentDownloads() throws Exception {
        int n = 8;
        byte[][] data = new byte[n][];
        // 크기가 서로 다른 파일을 경로마다 준비하고 모두 시작한다.
        for (int i = 0; i < n; i++) {
            data[i] = body(20000 + i * 7919);
            http.routeFixed("/c" + i, new TestHttpServer.Response(200).bytes("application/octet-stream", data[i]));
            Check.notNull(manager.start(request("/c" + i, null)), "started " + i);
        }
        Check.isTrue(manager.awaitIdle(WAIT_MS), "all ended");
        flushEdt();
        Check.equal(n, manager.handles.size(), "one popup per download");
        // 모든 파일의 내용을 확인한다.
        for (int i = 0; i < n; i++) {
            Check.isTrue(java.util.Arrays.equals(data[i], readBytes(new File(downloadDir, "c" + i))), "content of file " + i);
        }
        // 모든 팝업의 상태를 확인한다.
        for (int i = 0; i < n; i++) {
            Check.equal(DownloadState.COMPLETED, manager.handles.get(i).state, "state of popup " + i);
        }
        Check.equal(0, manager.getActiveCount(), "nothing is active any more");
    }

    /**
     * The popup can change the target while the transfer runs; the file ends up there and nothing stays at the old place.
     * <p>
     * 전송 중에 팝업이 대상을 바꿀 수 있다. 파일은 새 곳에 저장되고 옛 곳에는 아무것도 남지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChangeTargetDuringTransfer() throws Exception {
        holding = new HoldingServer(400000, 100000);
        final File elsewhere = new File(new File(downloadDir, "sub"), "moved.bin");
        manager.configurer = new Configurer() {
            /**
             * Changes the target at the first progress report and then lets the server continue.
             * <p>
             * 첫 진행 보고에서 대상을 바꾼 뒤 서버가 계속하게 한다.
             *
             * @param h the popup / 팝업
             */
            public void configure(final FakeHandle h) {
                h.onFirstProgress = new Runnable() {
                    /**
                     * Changes the target and releases the server.
                     * <p>
                     * 대상을 바꾸고 서버를 푼다.
                     */
                    public void run() {
                        h.control.changeTarget(elsewhere);
                        holding.release();
                    }
                };
            }
        };
        FakeHandle h = run(new DownloadRequest(holding.url("/slow.bin"), null, "", null, -1, null, null));
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        Check.equal(400000, readBytes(elsewhere).length, "saved at the new place");
        Check.isFalse(new File(downloadDir, "slow.bin").exists(), "nothing at the old place");
        String[] top = downloadDir.list();
        Check.equal("[sub]", java.util.Arrays.asList(top).toString(), "no partial file is left in the old directory");
        Check.equal(elsewhere.getAbsoluteFile(), h.target.getAbsoluteFile(), "the popup shows the new target");
    }

    /**
     * The user opens the file chooser of the popup, the transfer finishes while it is open, and the user then picks another file: the
     * download waits for the choice and the file ends up at the chosen place (found in the real-window check: the file used to stay at the
     * old place while the popup showed the new one).
     * <p>
     * 사용자가 팝업의 파일 선택기를 열고, 열려 있는 동안 전송이 끝나며, 그 뒤 사용자가 다른 파일을 고른다: 다운로드는 선택을 기다리고 파일은 고른 곳에 저장된다
     * (실제 창 확인에서 찾음: 팝업은 새 곳을 보이는데 파일은 옛 곳에 남았었다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChoiceWhileTheTransferFinishes() throws Exception {
        http.routeFixed("/quick.bin", new TestHttpServer.Response(200).bytes("application/octet-stream", body(5000)));
        final File chosen = new File(new File(downloadDir, "picked"), "chosen.bin");
        final boolean[] beforeChoice = new boolean[1];
        manager.configurer = new Configurer() {
            /**
             * Opens the "chooser" at the first progress report and picks the file a while after the transfer ended.
             * <p>
             * 첫 진행 보고에서 "선택기"를 열고 전송이 끝나고 한참 뒤에 파일을 고른다.
             *
             * @param h the popup / 팝업
             */
            public void configure(final FakeHandle h) {
                h.control.holdForChoice(true);
                Thread chooser = new Thread(new Runnable() {
                    /**
                     * Waits, checks that nothing was saved yet, then chooses.
                     * <p>
                     * 기다린 뒤 아직 아무것도 저장되지 않았는지 보고 고른다.
                     */
                    public void run() {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            return;
                        }
                        beforeChoice[0] = new File(downloadDir, "quick.bin").exists();
                        h.control.changeTarget(chosen);
                        h.control.holdForChoice(false);
                    }
                }, "test-chooser");
                chooser.setDaemon(true);
                chooser.start();
            }
        };
        FakeHandle h = run(new DownloadRequest(http.url("/quick.bin"), null, "", null, -1, null, null));
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        Check.isFalse(beforeChoice[0], "nothing was saved at the old place while the user was choosing");
        Check.equal(5000, readBytes(chosen).length, "saved at the chosen place");
        Check.isFalse(new File(downloadDir, "quick.bin").exists(), "nothing at the old place");
        Check.isTrue(h.message.indexOf("chosen.bin") >= 0, "the result names the chosen file: " + h.message);
    }

    /**
     * A missing target directory is created, and a target directory that cannot be created fails the download without a file.
     * <p>
     * 없는 대상 디렉터리는 만들어지고, 만들 수 없는 대상 디렉터리는 파일 없이 다운로드를 실패시킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTargetDirectory() throws Exception {
        http.routeFixed("/t", new TestHttpServer.Response(200).bytes("application/octet-stream", body(100)));
        final File newDir = new File(downloadDir, "created/later");
        TestManager m2 = new TestManager() {
            /**
             * Returns a directory that does not exist yet.
             * <p>
             * 아직 없는 디렉터리를 반환한다.
             *
             * @return directory / 디렉터리
             */
            protected File getDefaultDirectory() {
                return newDir;
            }
        };
        Check.notNull(m2.start(request("/t", null)), "started");
        Check.isTrue(m2.awaitIdle(WAIT_MS), "ended");
        Check.equal(100, readBytes(new File(newDir, "t")).length, "the directory was created");
        final File blocker = new File(downloadDir, "blocker");
        SafeFilesTest.write(blocker, "I am a file");
        TestManager m3 = new TestManager() {
            /**
             * Returns a path below a regular file.
             * <p>
             * 일반 파일 아래의 경로를 반환한다.
             *
             * @return directory / 디렉터리
             */
            protected File getDefaultDirectory() {
                return new File(blocker, "inside");
            }
        };
        Check.notNull(m3.start(request("/t", null)), "started");
        Check.isTrue(m3.awaitIdle(WAIT_MS), "ended");
        flushEdt();
        Check.equal(DownloadState.FAILED, m3.handles.get(0).state, "failed: " + m3.handles.get(0).message);
        Check.equal(1, new File(downloadDir, "blocker").length() > 0 ? 1 : 0, "the blocking file is untouched");
        m2.dispose();
        m3.dispose();
    }

    /**
     * Korean names from Content-Disposition are used for the file, and a malicious name cannot leave the download directory.
     * <p>
     * Content-Disposition 의 한글 이름은 파일 이름으로 쓰이며 악의적인 이름은 다운로드 디렉터리를 벗어날 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileNamesAreApplied() throws Exception {
        http.routeFixed("/k", new TestHttpServer.Response(200).bytes("application/octet-stream", body(10)));
        FakeHandle h = run(request("/k", "attachment; filename*=UTF-8''%ED%95%9C%EA%B8%80.bin"));
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        Check.isTrue(new File(downloadDir, "한글.bin").isFile(), "Korean file name");
        h = run(request("/k", "attachment; filename=\"..\\..\\evil.bin\""));
        Check.equal(DownloadState.COMPLETED, h.state, "state: " + h.message);
        Check.isTrue(new File(downloadDir, "evil.bin").isFile(), "the directory part was removed");
        Check.isFalse(new File(downloadDir.getParentFile(), "evil.bin").exists(), "nothing outside of the download directory");
    }

    /**
     * The manager is also usable through the download handler interface of the component core.
     * <p>
     * 관리자는 컴포넌트 코어의 다운로드 처리기 인터페이스로도 쓸 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHandlerInterface() throws Exception {
        http.routeFixed("/hi", new TestHttpServer.Response(200).bytes("application/octet-stream", body(64)));
        org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException ex = new org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException(http.url("/hi"), http.url("/hi"),
                "application/octet-stream", "attachment; filename=\"viaCore.bin\"", 64, org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException.Reason.ATTACHMENT);
        org.duckdns.hjow.swingwebpanel.core.DownloadHandler handler = manager;
        handler.downloadRequired(null, ex);
        Check.isTrue(manager.awaitIdle(WAIT_MS), "ended");
        flushEdt();
        Check.equal(64, readBytes(new File(downloadDir, "viaCore.bin")).length, "downloaded through the core interface");
    }
}
