package org.duckdns.hjow.swingwebpanel.download;

import java.io.File;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Smoke test of the real download popup: it can be created, updated with every state and disposed without an exception. It needs a
 * screen and is skipped in a headless environment; how the dialog really looks and behaves is checked by hand with the example
 * <code>org.duckdns.hjow.swingwebpanel.examples.DownloadPopupExample</code>.
 * <p>
 * 실제 다운로드 팝업의 동작 확인 시험: 만들고, 모든 상태로 갱신하고, 예외 없이 해제할 수 있다. 화면이 필요하며 헤드리스 환경에서는 건너뛴다.
 * 대화상자의 실제 모양과 동작은 예제 <code>org.duckdns.hjow.swingwebpanel.examples.DownloadPopupExample</code> 로 직접 확인한다.
 */
public class SwingDownloadDialogTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SwingDownloadDialogTest() {
    }

    /**
     * Number formatting of the popup.
     * <p>
     * 팝업의 숫자 표기.
     */
    public void testSizeFormat() {
        Check.equal("0 B", SwingDownloadDialog.format(0), "zero");
        Check.equal("1023 B", SwingDownloadDialog.format(1023), "bytes");
        Check.equal("1.0 KB", SwingDownloadDialog.format(1024), "kilobyte");
        Check.equal("1.5 MB", SwingDownloadDialog.format(1572864), "megabytes");
        Check.equal("2.0 GB", SwingDownloadDialog.format(2L * 1024 * 1024 * 1024), "gigabytes");
    }

    /**
     * Drives the real dialog through its life.
     * <p>
     * 실제 대화상자를 생애 전체에 걸쳐 움직여 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRealDialogLifecycle() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            Check.skip("no display: the real dialog cannot be created in a headless environment");
        }
        final DownloadRequest req = new DownloadRequest("http://example.test/dir/a%20file.bin", null, "application/octet-stream", null, 2048, null, null);
        final boolean[] cancelled = new boolean[1];
        final File[] target = { new File("a file.bin").getAbsoluteFile() };
        final DownloadControl control = new DownloadControl() {
            /**
             * Records the cancellation.
             * <p>
             * 취소를 기록한다.
             */
            public void cancel() {
                cancelled[0] = true;
            }

            /**
             * Records the new target.
             * <p>
             * 새 대상을 기록한다.
             *
             * @param t the target / 대상
             */
            public void changeTarget(File t) {
                target[0] = t;
            }

            /**
             * Returns the target.
             * <p>
             * 대상을 반환한다.
             *
             * @return the target / 대상
             */
            public File getTarget() {
                return target[0];
            }

            /**
             * Accepts the hold while the user chooses (nothing to wait for here).
             * <p>
             * 사용자가 고르는 동안의 붙잡기를 받는다(여기서는 기다릴 것이 없다).
             *
             * @param holding true while choosing / 고르는 동안 true
             */
            public void holdForChoice(boolean holding) {
            }
        };
        final Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates and updates the dialog.
             * <p>
             * 대화상자를 만들고 갱신한다.
             */
            public void run() {
                try {
                    SwingDownloadDialog d = new SwingDownloadDialog(null, req, control);
                    d.setTarget(target[0]);
                    d.setState(DownloadState.STARTING, "Connecting...");
                    d.setProgress(0, 2048);
                    d.setProgress(1024, 2048);
                    d.setProgress(1500, -1);
                    d.setState(DownloadState.RUNNING, "Downloading...");
                    d.setState(DownloadState.COMPLETED, "Saved to " + target[0]);
                    d.dispose();
                    d.dispose();
                } catch (RuntimeException e) {
                    failure[0] = e;
                }
            }
        });
        Check.isNull(failure[0], "the dialog works: " + failure[0]);
        Check.isFalse(cancelled[0], "nothing was cancelled");
    }

    /**
     * The size row of the dialog says "unknown" while the size is not known and shows the size as soon as a progress report knows it (found
     * when the real popup was looked at: the row kept "unknown" while the bar already showed "of 3.0 MB").
     * <p>
     * 대화상자의 크기 줄은 크기를 모르는 동안 "unknown" 이고 진행 보고가 크기를 알려 주면 곧바로 그 크기를 보여 준다(실제 팝업을 보다가 발견: 막대는 이미
     * "of 3.0 MB" 를 보이는데 크기 줄은 "unknown" 으로 남아 있었다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizeRowFollowsTheProgress() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            Check.skip("no display: the real dialog cannot be created in a headless environment");
        }
        final DownloadRequest req = new DownloadRequest("http://example.test/dir/sized.bin", null, "application/octet-stream", null, -1, null, null);
        final String[] texts = new String[2];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the dialog and reads the size row before and after a progress report.
             * <p>
             * 대화상자를 만들고 진행 보고 전후의 크기 줄을 읽는다.
             */
            public void run() {
                SwingDownloadDialog d = new SwingDownloadDialog(null, req, null);
                java.awt.Component row = findLabelAfter(d.getContentPane(), "Size:");
                texts[0] = ((javax.swing.JLabel) row).getText();
                d.setProgress(100, 4096);
                texts[1] = ((javax.swing.JLabel) row).getText();
                d.dispose();
            }
        });
        Check.equal("unknown", texts[0], "the size is not known at first");
        Check.isTrue(texts[1].indexOf("KB") >= 0, "the size row shows the total once it is known: " + texts[1]);
    }

    /**
     * Checks that the bar of a download of unknown size shows no percentage (the real popup showed "0%" on the moving bar).
     * <p>
     * 크기를 모르는 다운로드의 막대가 백분율을 보이지 않는지 확인한다(실제 팝업은 움직이는 막대에 "0%" 를 보였다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnknownSizeBarHasNoPercentage() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            Check.skip("no display: the real dialog cannot be created in a headless environment");
        }
        final DownloadRequest req = new DownloadRequest("http://example.test/dir/stream.bin", null, "application/octet-stream", null, -1, null, null);
        final String[] text = new String[1];
        final boolean[] indeterminate = new boolean[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the dialog, reports progress of unknown total and reads the bar.
             * <p>
             * 대화상자를 만들고 전체를 모르는 진행을 알린 뒤 막대를 읽는다.
             */
            public void run() {
                SwingDownloadDialog d = new SwingDownloadDialog(null, req, null);
                d.setProgress(32768, -1);
                javax.swing.JProgressBar bar = findBar(d.getContentPane());
                text[0] = bar.getString();
                indeterminate[0] = bar.isIndeterminate();
                d.dispose();
            }
        });
        Check.isTrue(indeterminate[0], "the bar moves without an end");
        Check.equal("", text[0], "and shows no percentage");
    }

    /**
     * Finds the progress bar in a container tree.
     * <p>
     * 컨테이너 트리에서 진행 막대를 찾는다.
     *
     * @param parent the container to search / 찾을 컨테이너
     * @return the bar or null / 막대. 없으면 null
     */
    private static javax.swing.JProgressBar findBar(java.awt.Container parent) {
        java.awt.Component[] children = parent.getComponents();
        // 자식을 차례로 보면서 진행 막대를 찾고 컨테이너면 안쪽을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof javax.swing.JProgressBar) {
                return (javax.swing.JProgressBar) children[i];
            }
            if (children[i] instanceof java.awt.Container) {
                javax.swing.JProgressBar inner = findBar((java.awt.Container) children[i]);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * Finds the label that follows the caption label with a text in the same container (the value of a captioned row).
     * <p>
     * 같은 컨테이너에서 주어진 글의 제목 레이블 바로 다음에 오는 레이블(제목이 있는 줄의 값)을 찾는다.
     *
     * @param parent  the container to search / 찾을 컨테이너
     * @param caption the caption text / 제목 글
     * @return the value label or null / 값 레이블. 없으면 null
     */
    private static java.awt.Component findLabelAfter(java.awt.Container parent, String caption) {
        java.awt.Component[] children = parent.getComponents();
        // 자식을 차례로 보면서 제목을 찾으면 그 다음 구성요소를, 컨테이너이면 안쪽을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof javax.swing.JLabel && caption.equals(((javax.swing.JLabel) children[i]).getText()) && i + 1 < children.length) {
                return children[i + 1];
            }
            if (children[i] instanceof java.awt.Container) {
                java.awt.Component inner = findLabelAfter((java.awt.Container) children[i], caption);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }
}
