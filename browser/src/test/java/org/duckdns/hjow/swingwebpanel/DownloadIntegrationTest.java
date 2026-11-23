package org.duckdns.hjow.swingwebpanel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.download.DownloadControl;
import org.duckdns.hjow.swingwebpanel.download.DownloadHandle;
import org.duckdns.hjow.swingwebpanel.download.DownloadRequest;
import org.duckdns.hjow.swingwebpanel.download.DownloadState;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The download popup of the component (plan items P01-18 and P11-09 at the component level): a response that is an attachment or cannot be
 * shown opens a popup through the overridable <code>createDownloadPopup</code>, the file is saved in the directory that the host chose,
 * the page, the history, the source and the progress do not change, and <code>clean</code> cancels a transfer that is still running.
 * <p>
 * 컴포넌트의 다운로드 팝업(컴포넌트 수준의 계획 P01-18, P11-09): 응답이 첨부 파일이거나 보여 줄 수 없으면 재정의 가능한
 * <code>createDownloadPopup</code> 으로 팝업이 열리고, 파일은 호스트가 고른 디렉터리에 저장되며, 페이지, 이력, 원문, 진행률은 바뀌지 않고,
 * <code>clean</code> 은 아직 진행 중인 전송을 취소한다.
 */
public class DownloadIntegrationTest {

    /**
     * A popup that records what the manager tells it.
     * <p>
     * 관리자가 알려 주는 것을 기록하는 팝업.
     */
    private static final class RecordingPopup implements DownloadHandle {
        /**
         * The request that the popup belongs to.
         * <p>
         * 팝업이 속한 요청.
         */
        final DownloadRequest request;

        /**
         * The control for cancelling.
         * <p>
         * 취소를 위한 제어 객체.
         */
        final DownloadControl control;

        /**
         * The last state.
         * <p>
         * 마지막 상태.
         */
        volatile DownloadState state;

        /**
         * The last target shown.
         * <p>
         * 마지막으로 보여 준 대상.
         */
        volatile File target;

        /**
         * True after the popup was closed by the manager.
         * <p>
         * 관리자가 팝업을 닫은 뒤 true.
         */
        volatile boolean closed;

        /**
         * Counts down when the transfer reached a final state.
         * <p>
         * 전송이 최종 상태에 이르면 줄어든다.
         */
        final CountDownLatch finished = new CountDownLatch(1);

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param request the request / 요청
         * @param control the control / 제어 객체
         */
        RecordingPopup(DownloadRequest request, DownloadControl control) {
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
        }

        /**
         * Ignores progress.
         * <p>
         * 진행을 무시한다.
         *
         * @param done  bytes done / 받은 바이트
         * @param total the total / 전체
         */
        public void setProgress(long done, long total) {
        }

        /**
         * Records the state and releases the waiting test at a final state.
         * <p>
         * 상태를 기록하고 최종 상태이면 기다리는 시험을 풀어 준다.
         *
         * @param s the state / 상태
         * @param m the message / 메시지
         */
        public void setState(DownloadState s, String m) {
            state = s;
            if (s == DownloadState.COMPLETED || s == DownloadState.FAILED || s == DownloadState.CANCELLED) {
                finished.countDown();
            }
        }

        /**
         * Keeps the target.
         * <p>
         * 대상을 유지한다.
         *
         * @param current the current target / 현재 대상
         * @return the same target / 같은 대상
         */
        public File chooseTarget(File current) {
            return current;
        }

        /**
         * Agrees to replace a file.
         * <p>
         * 파일 교체에 동의한다.
         *
         * @param existing the existing file / 이미 있는 파일
         * @return true / true
         */
        public boolean confirmOverwrite(File existing) {
            return true;
        }

        /**
         * Records that the popup was closed (what a cancelling <code>clean</code> does) and releases the waiting test.
         * <p>
         * 팝업이 닫혔음을 기록하고(취소하는 <code>clean</code> 이 하는 일) 기다리는 시험을 풀어 준다.
         */
        public void dispose() {
            closed = true;
            finished.countDown();
        }
    }

    /**
     * A component whose popups and download directory are under the control of the test.
     * <p>
     * 팝업과 다운로드 디렉터리를 시험이 제어하는 컴포넌트.
     */
    private static final class TestPanel extends SwingWebPanel {
        /**
         * The popups that were created.
         * <p>
         * 만들어진 팝업.
         */
        final List<RecordingPopup> popups = Collections.synchronizedList(new ArrayList<RecordingPopup>());

        /**
         * The directory for downloads.
         * <p>
         * 다운로드 디렉터리.
         */
        final File directory;

        /**
         * Creates the component.
         * <p>
         * 컴포넌트를 만든다.
         *
         * @param directory the download directory / 다운로드 디렉터리
         */
        TestPanel(File directory) {
            this.directory = directory;
        }

        /**
         * Creates a recording popup instead of a dialog.
         * <p>
         * 대화상자 대신 기록하는 팝업을 만든다.
         *
         * @param request what is downloaded / 내려받는 대상
         * @param control the control / 제어 객체
         * @return the popup / 팝업
         */
        protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
            RecordingPopup popup = new RecordingPopup(request, control);
            popups.add(popup);
            return popup;
        }

        /**
         * Returns the directory that the test made.
         * <p>
         * 시험이 만든 디렉터리를 반환한다.
         *
         * @return the directory / 디렉터리
         */
        protected File getDownloadDirectory() {
            return directory;
        }
    }

    /**
     * Servers started by a case, stopped in tearDown.
     * <p>
     * 사례에서 시작한 서버. tearDown 에서 중지한다.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Panels created by a case, disposed in tearDown.
     * <p>
     * 사례에서 만든 패널. tearDown 에서 폐기한다.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DownloadIntegrationTest() {
    }

    /**
     * Releases everything the case created.
     * <p>
     * 사례가 만든 것을 모두 해제한다.
     */
    public void tearDown() {
        // 만든 패널과 서버를 모두 정리한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        // 시작한 서버를 모두 중지한다.
        for (TestHttpServer s : servers) {
            s.stop();
        }
    }

    /**
     * Creates a directory for one case.
     * <p>
     * 사례 하나를 위한 디렉터리를 만든다.
     *
     * @return the new directory / 새 디렉터리
     * @throws IOException if it cannot be created / 만들 수 없으면
     */
    private static File tempDirectory() throws IOException {
        File dir = File.createTempFile("swp-download-test", "");
        if (!dir.delete() || !dir.mkdir()) {
            throw new IOException("cannot create " + dir);
        }
        return dir;
    }

    /**
     * Deletes a directory with its files.
     * <p>
     * 디렉터리와 그 안의 파일을 지운다.
     *
     * @param dir the directory / 디렉터리
     */
    private static void delete(File dir) {
        File[] files = dir.listFiles();
        // 안의 파일을 먼저 지운다.
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        dir.delete();
    }

    /**
     * Waits until the load finished (progress 100 and no "Loading" status).
     * <p>
     * 로딩이 끝날 때까지(진행률 100, "Loading" 상태 아님) 기다린다.
     *
     * @param p the panel / 패널
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void settle(SwingWebPanel p) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 상태가 끝난 것으로 보일 때까지 짧게 쉬며 기다린다.
        while ((p.getProgress() != 100 || p.getStatusText().startsWith("Loading")) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(50);
    }

    /**
     * P01-18, P11-09: an attachment and a response that cannot be shown each open a popup, the files are saved with the right content in
     * the directory of the host, and the page, the address, the history, the source and the progress stay as they were.
     * <p>
     * P01-18, P11-09: 첨부 파일과 보여 줄 수 없는 응답은 각각 팝업을 열고, 파일은 호스트의 디렉터리에 올바른 내용으로 저장되며, 페이지, 주소, 이력,
     * 원문, 진행률은 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDownloadsOpenPopupsAndLeaveThePageAlone() throws Exception {
        File dir = tempDirectory();
        try {
            TestHttpServer s = new TestHttpServer();
            s.start();
            servers.add(s);
            s.routeFixed("/page", TestHttpServer.Response.html("<p>page</p>"));
            s.routeFixed("/report.html", new TestHttpServer.Response(200).text("text/html", "<p>report</p>")
                    .header("Content-Disposition", "attachment; filename=\"report.html\""));
            byte[] data = new byte[5000];
            // 내용이 위치마다 다르도록 채운다.
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i * 7);
            }
            s.routeFixed("/archive.bin", new TestHttpServer.Response(200).bytes("application/octet-stream", data));
            TestPanel p = new TestPanel(dir);
            panels.add(p);
            p.setPage(s.url("/page"));
            settle(p);
            String history = p.getHistories().toString();
            p.setPage(s.url("/report.html"));
            settle(p);
            p.setPage(s.url("/archive.bin"));
            settle(p);
            long end = System.currentTimeMillis() + 30000;
            // 두 팝업이 모두 열릴 때까지 기다린다.
            while (p.popups.size() < 2 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(2, p.popups.size(), "one popup per download");
            // 팝업마다 전송이 끝나기를 기다린다.
            for (RecordingPopup popup : new ArrayList<RecordingPopup>(p.popups)) {
                Check.isTrue(popup.finished.await(30, TimeUnit.SECONDS), "the transfer of " + popup.request.getFileName() + " ended");
                Check.equal(DownloadState.COMPLETED, popup.state, "done: " + popup.request.getFileName());
            }
            Check.equal(s.url("/page"), p.getPage(), "the page address did not change");
            Check.equal("<p>page</p>", p.getContent(), "the source did not change");
            Check.equal(history, p.getHistories().toString(), "the history did not change");
            Check.equal(100, p.getProgress(), "the progress did not change");
            Check.equal("<p>report</p>", new String(read(new File(dir, "report.html")), "UTF-8"), "the attachment was saved");
            byte[] saved = read(new File(dir, "archive.bin"));
            Check.equal(data.length, saved.length, "the binary file has its size");
            // 저장된 파일의 모든 바이트가 보낸 것과 같은지 확인한다.
            for (int i = 0; i < data.length; i++) {
                if (saved[i] != data[i]) {
                    Check.fail("byte " + i + " differs");
                }
            }
            File[] left = dir.listFiles();
            Check.equal(2, left == null ? -1 : left.length, "no partial file is left behind");
        } finally {
            delete(dir);
        }
    }

    /**
     * P01-18: <code>clean</code> cancels a transfer that is still running; no file is left in the directory.
     * <p>
     * P01-18: <code>clean</code> 은 아직 진행 중인 전송을 취소한다. 디렉터리에 파일이 남지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanCancelsARunningDownload() throws Exception {
        File dir = tempDirectory();
        try {
            TestHttpServer s = new TestHttpServer();
            s.start();
            servers.add(s);
            final CountDownLatch entered = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            s.route("/slow.bin", new TestHttpServer.Handler() {
                /**
                 * Holds the response until the test lets go.
                 * <p>
                 * 시험이 놓아 줄 때까지 응답을 붙잡는다.
                 *
                 * @param request the request / 요청
                 * @return the file / 파일
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(20, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("application/octet-stream", new byte[1000]);
                }
            });
            TestPanel p = new TestPanel(dir);
            panels.add(p);
            p.setPage(s.url("/slow.bin"));
            Check.isTrue(entered.await(30, TimeUnit.SECONDS), "the transfer started");
            long end = System.currentTimeMillis() + 30000;
            // 팝업이 열릴 때까지 기다린다.
            while (p.popups.isEmpty() && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(1, p.popups.size(), "the popup opened");
            p.clean();
            Check.isTrue(p.popups.get(0).finished.await(30, TimeUnit.SECONDS), "the popup was closed by clean");
            Check.isTrue(p.popups.get(0).closed, "clean closes the popup of a running download");
            release.countDown();
            Thread.sleep(300);
            File[] left = dir.listFiles();
            Check.equal(0, left == null ? -1 : left.length, "nothing was saved");
        } finally {
            delete(dir);
        }
    }

    /**
     * P21-03: <code>dispose</code> cancels a transfer that is still running and closes its popup; nothing is saved and the next
     * <code>dispose</code> call is harmless.
     * <p>
     * P21-03: <code>dispose</code> 는 아직 진행 중인 전송을 취소하고 팝업을 닫는다. 아무것도 저장되지 않으며 다시 부르는 <code>dispose</code> 는 해롭지 않다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposeCancelsARunningDownload() throws Exception {
        File dir = tempDirectory();
        try {
            TestHttpServer s = new TestHttpServer();
            s.start();
            servers.add(s);
            final CountDownLatch entered = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            s.route("/slow.bin", new TestHttpServer.Handler() {
                /**
                 * Holds the response until the test lets go.
                 * <p>
                 * 시험이 놓아 줄 때까지 응답을 붙잡는다.
                 *
                 * @param request the request / 요청
                 * @return the file / 파일
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(20, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("application/octet-stream", new byte[1000]);
                }
            });
            TestPanel p = new TestPanel(dir);
            panels.add(p);
            p.setPage(s.url("/slow.bin"));
            Check.isTrue(entered.await(30, TimeUnit.SECONDS), "the transfer started");
            long end = System.currentTimeMillis() + 30000;
            // 팝업이 열릴 때까지 기다린다.
            while (p.popups.isEmpty() && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(1, p.popups.size(), "the popup opened");
            p.dispose();
            Check.isTrue(p.popups.get(0).finished.await(30, TimeUnit.SECONDS), "the popup was closed by dispose");
            Check.isTrue(p.popups.get(0).closed, "dispose closes the popup of a running download");
            p.dispose();
            release.countDown();
            Thread.sleep(300);
            File[] left = dir.listFiles();
            Check.equal(0, left == null ? -1 : left.length, "nothing was saved");
        } finally {
            delete(dir);
        }
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f the file / 파일
     * @return its bytes / 바이트
     * @throws IOException if it cannot be read / 읽을 수 없으면
     */
    private static byte[] read(File f) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            // 파일 끝까지 읽어 모은다.
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
