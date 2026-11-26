package org.duckdns.hjow.swingwebpanel.download;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.FailureKind;
import org.duckdns.hjow.swingwebpanel.net.RequestPurpose;
import org.duckdns.hjow.swingwebpanel.net.ResourceRequest;
import org.duckdns.hjow.swingwebpanel.net.ResourceResponse;
import org.duckdns.hjow.swingwebpanel.net.ResponseHandler;

/**
 * One download in progress: it owns the transfer thread, the cancel token, the partial file and the popup of the download. The transfer
 * writes into a partial file next to the target, reports its progress to the popup (at most about ten times a second), and at the end
 * either moves the finished file to the target or deletes the partial file (cancel, failure). All popup calls happen on the event
 * dispatch thread; everything else on the transfer thread.
 * <p>
 * 진행 중인 다운로드 하나: 전송 스레드, 취소 토큰, 부분 파일, 팝업을 소유한다. 전송은 대상 옆의 부분 파일에 쓰고, 진행 상황을 팝업에 알리며
 * (초당 약 10번 이하), 끝나면 완성된 파일을 대상으로 옮기거나 부분 파일을 지운다(취소, 실패). 팝업 호출은 모두 이벤트 디스패치 스레드에서,
 * 나머지는 전송 스레드에서 일어난다.
 */
final class DownloadTask implements DownloadControl, Runnable {

    /** Shortest time between two progress reports in milliseconds. 진행 보고 사이의 최소 간격(밀리초). */
    private static final long REPORT_INTERVAL_MS = 100;

    /** The manager. 관리자. */
    private final SwingDownloadHandler manager;

    /** What is downloaded. 내려받는 대상. */
    private final DownloadRequest request;

    /** Cancel token of the transfer. 전송의 취소 토큰. */
    private final CancelToken token = new CancelToken();

    /** The target file (may be changed by the user). 대상 파일(사용자가 바꿀 수 있다). */
    private volatile File target;

    /** True once the download was cancelled. 다운로드가 취소되었으면 true. */
    private volatile boolean cancelled;

    /** The number of open choices of a new target (see {@link #holdForChoice(boolean)}); guarded by {@link #holdLock}. 열려 있는 새 대상 고르기의 수({@link #holdForChoice(boolean)} 참고). {@link #holdLock} 으로 보호한다. */
    private int holds;

    /** The lock and monitor of {@link #holds}. {@link #holds} 의 잠금과 모니터. */
    private final Object holdLock = new Object();

    /** The popup, or null before it exists and after it was closed. 팝업. 만들어지기 전이나 닫은 뒤에는 null. */
    private volatile DownloadHandle view;

    /** Total size in bytes or -1 (learned from the response). 전체 크기(바이트) 또는 -1(응답에서 알아낸다). */
    private volatile long total;

    /** Time of the last progress report. 마지막 진행 보고 시각. */
    private long lastReport;

    /** The partial file while data is written. 데이터를 쓰는 동안의 부분 파일. */
    private File partial;

    /** First I/O error of the handler, or null. 처리기에서 처음 일어난 I/O 오류. 없으면 null. */
    private volatile IOException ioError;

    /**
     * Creates a task.
     * <p>
     * 작업을 만든다.
     *
     * @param manager the manager / 관리자
     * @param request what to download / 내려받을 대상
     * @param target  the initial target file / 처음 대상 파일
     */
    DownloadTask(SwingDownloadHandler manager, DownloadRequest request, File target) {
        this.manager = manager;
        this.request = request;
        this.target = target.getAbsoluteFile();
        this.total = request.getLength();
    }

    /**
     * Returns what is downloaded.
     * <p>
     * 내려받는 대상을 반환한다.
     *
     * @return the request / 요청
     */
    DownloadRequest getRequest() {
        return request;
    }

    /**
     * Creates the popup on the event dispatch thread and then starts the transfer thread.
     * <p>
     * 이벤트 디스패치 스레드에서 팝업을 만든 뒤 전송 스레드를 시작한다.
     */
    void begin() {
        SwingDownloadHandler.later(new Runnable() {
            /**
             * Creates the popup and starts the thread.
             * <p>
             * 팝업을 만들고 스레드를 시작한다.
             */
            public void run() {
                DownloadHandle v = manager.makePopup(DownloadTask.this);
                if (cancelled) {
                    if (v != null) {
                        v.dispose();
                    }
                } else {
                    view = v;
                    if (v != null) {
                        v.setTarget(target);
                        v.setState(DownloadState.STARTING, "Connecting...");
                    }
                }
                manager.status("Download started: " + request.getFileName());
                Thread t = new Thread(DownloadTask.this, "swingwebpanel-download");
                t.setDaemon(true);
                t.start();
            }
        });
    }

    /**
     * Cancels the transfer; the transfer thread deletes the partial file.
     * <p>
     * 전송을 취소한다. 전송 스레드가 부분 파일을 지운다.
     */
    public void cancel() {
        cancelled = true;
        token.cancel();
        // 대상 고르기를 기다리던 전송도 깨운다.
        synchronized (holdLock) {
            holdLock.notifyAll();
        }
    }

    /**
     * Holds the end of the transfer while the user chooses a new target (<code>true</code>) or releases it (<code>false</code>); see
     * {@link DownloadControl#holdForChoice(boolean)}.
     * <p>
     * 사용자가 새 대상을 고르는 동안 전송의 끝을 붙잡거나(<code>true</code>) 놓는다(<code>false</code>). {@link DownloadControl#holdForChoice(boolean)} 참고.
     *
     * @param holding true while the user chooses / 사용자가 고르는 동안 true
     */
    public void holdForChoice(boolean holding) {
        synchronized (holdLock) {
            holds = Math.max(0, holds + (holding ? 1 : -1));
            holdLock.notifyAll();
        }
    }

    /**
     * Waits until no choice of a new target is open or the download is cancelled.
     * <p>
     * 열려 있는 새 대상 고르기가 없어지거나 다운로드가 취소될 때까지 기다린다.
     *
     * @throws InterruptedException if the thread is interrupted / 스레드가 중단되면
     */
    private void awaitChoice() throws InterruptedException {
        synchronized (holdLock) {
            // 사용자가 고르기를 마치거나 취소할 때까지 기다린다(놓친 알림에 대비해 주기적으로 다시 본다).
            while (holds > 0 && !cancelled) {
                holdLock.wait(500);
            }
        }
    }

    /**
     * Changes the target file.
     * <p>
     * 대상 파일을 바꾼다.
     *
     * @param newTarget the new target / 새 대상
     */
    public void changeTarget(File newTarget) {
        if (newTarget == null) {
            return;
        }
        final File t = newTarget.getAbsoluteFile();
        target = t;
        post(new Runnable() {
            /**
             * Shows the new target.
             * <p>
             * 새 대상을 보여 준다.
             */
            public void run() {
                DownloadHandle v = view;
                if (v != null) {
                    v.setTarget(t);
                }
            }
        });
    }

    /**
     * Returns the current target file.
     * <p>
     * 현재 대상 파일을 반환한다.
     *
     * @return target / 대상
     */
    public File getTarget() {
        return target;
    }

    /**
     * Closes the popup (event dispatch thread or any thread).
     * <p>
     * 팝업을 닫는다(이벤트 디스패치 스레드 또는 어느 스레드나).
     */
    void closeView() {
        final DownloadHandle v = view;
        view = null;
        if (v != null) {
            SwingDownloadHandler.later(new Runnable() {
                /**
                 * Disposes the popup.
                 * <p>
                 * 팝업을 해제한다.
                 */
                public void run() {
                    v.dispose();
                }
            });
        }
    }

    /**
     * Runs a popup update on the event dispatch thread if the popup still exists.
     * <p>
     * 팝업이 아직 있으면 팝업 갱신을 이벤트 디스패치 스레드에서 실행한다.
     *
     * @param r the update / 갱신
     */
    private void post(Runnable r) {
        if (view != null) {
            SwingDownloadHandler.later(r);
        }
    }

    /**
     * Shows a state.
     * <p>
     * 상태를 보여 준다.
     *
     * @param state   the state / 상태
     * @param message the result line / 결과 줄
     */
    private void showState(final DownloadState state, final String message) {
        post(new Runnable() {
            /**
             * Updates the popup.
             * <p>
             * 팝업을 갱신한다.
             */
            public void run() {
                DownloadHandle v = view;
                if (v != null) {
                    v.setState(state, message);
                }
            }
        });
    }

    /**
     * Shows the progress, but not more often than the report interval unless forced.
     * <p>
     * 진행 상황을 보여 준다. 강제하지 않으면 보고 간격보다 자주 보여 주지 않는다.
     *
     * @param done  bytes received / 받은 바이트
     * @param force true to report now / 지금 보고하려면 true
     */
    private void showProgress(final long done, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastReport < REPORT_INTERVAL_MS) {
            return;
        }
        lastReport = now;
        long t = total;
        final long shownTotal = t >= 0 && done > t ? -1 : t;
        post(new Runnable() {
            /**
             * Updates the progress bar.
             * <p>
             * 진행 막대를 갱신한다.
             */
            public void run() {
                DownloadHandle v = view;
                if (v != null) {
                    v.setProgress(done, shownTotal);
                }
            }
        });
    }

    /**
     * Output stream that counts the bytes, reports progress and stops when the download is cancelled.
     * <p>
     * 바이트를 세고 진행 상황을 알리며 다운로드가 취소되면 멈추는 출력 스트림.
     */
    private final class Sink extends FilterOutputStream {
        /** Bytes written so far. 지금까지 쓴 바이트. */
        private long written;

        /**
         * Creates the stream.
         * <p>
         * 스트림을 만든다.
         *
         * @param out the file stream / 파일 스트림
         */
        Sink(OutputStream out) {
            super(out);
        }

        /**
         * Writes a byte.
         * <p>
         * 바이트 하나를 쓴다.
         *
         * @param b the byte / 바이트
         * @throws IOException on failure or cancellation / 실패나 취소 시
         */
        public void write(int b) throws IOException {
            check();
            out.write(b);
            written++;
            showProgress(written, false);
        }

        /**
         * Writes a block of bytes.
         * <p>
         * 바이트 블록을 쓴다.
         *
         * @param b   the buffer / 버퍼
         * @param off start / 시작
         * @param len length / 길이
         * @throws IOException on failure or cancellation / 실패나 취소 시
         */
        public void write(byte[] b, int off, int len) throws IOException {
            check();
            out.write(b, off, len);
            written += len;
            showProgress(written, false);
        }

        /**
         * Fails when the download was cancelled.
         * <p>
         * 다운로드가 취소되었으면 실패한다.
         *
         * @throws IOException if cancelled / 취소되었으면
         */
        private void check() throws IOException {
            if (cancelled) {
                throw new IOException("cancelled");
            }
        }

        /**
         * Returns the bytes written.
         * <p>
         * 쓴 바이트 수를 반환한다.
         *
         * @return bytes / 바이트 수
         */
        long getWritten() {
            return written;
        }
    }

    /**
     * The transfer thread: fetches the data into the partial file and finishes the download.
     * <p>
     * 전송 스레드: 데이터를 부분 파일로 가져오고 다운로드를 마무리한다.
     */
    public void run() {
        DownloadState end = DownloadState.FAILED;
        String message = "Download failed";
        final Sink[] sinkHolder = new Sink[1];
        try {
            ResourceRequest rr = new ResourceRequest(request.getUrl()).setUserAgent(request.getUserAgent())
                    .setExtraHeaders(request.getHeaders()).setCookies(manager.getCookies()).setPurpose(RequestPurpose.DOWNLOAD)
                    .setBlobUrlEntry(request.getBlobUrlEntry())
                    .setCancelToken(token);
            // 폼 제출의 응답을 저장하는 경우에만(사용자가 동의한 뒤) 같은 데이터를 다시 보낸다.
            if (request.getPostBody() != null) {
                rr.setMethod("POST").setBody(request.getPostBody(), request.getPostContentType());
                if (request.getPostReferrer() != null) {
                    rr.setReferrer(request.getPostReferrer());
                }
            }
            ResourceResponse response = manager.getFetcher().fetch(rr, new ResponseHandler() {
                /**
                 * Opens the partial file for a successful response; error responses are read into memory and discarded.
                 * <p>
                 * 성공한 응답이면 부분 파일을 열고 오류 응답은 메모리로 읽어 버린다.
                 *
                 * @param head the response head / 응답 머리
                 * @return the stream or null / 스트림 또는 null
                 * @throws IOException if the file cannot be created / 파일을 만들 수 없으면
                 */
                public OutputStream onResponse(ResourceResponse head) throws IOException {
                    if (head.isError() || cancelled) {
                        return null;
                    }
                    if (head.getDeclaredLength() >= 0) {
                        total = head.getDeclaredLength();
                    }
                    try {
                        File t = target;
                        partial = SafeFiles.newPartFile(t.getParentFile(), t.getName());
                        sinkHolder[0] = new Sink(new BufferedOutputStream(new FileOutputStream(partial), 64 * 1024));
                    } catch (IOException e) {
                        ioError = e;
                        throw e;
                    }
                    showState(DownloadState.RUNNING, "Downloading...");
                    return sinkHolder[0];
                }
            });
            closeQuietly(sinkHolder[0]);
            if (cancelled || token.isCancelled() || response.getFailureKind() == FailureKind.CANCELLED) {
                end = DownloadState.CANCELLED;
                message = "Cancelled";
            } else if (ioError != null) {
                message = "Cannot write the file: " + ioError.getMessage();
            } else if (response.getFailureKind() != FailureKind.NONE) {
                message = "Download failed: " + response.getFailureMessage();
            } else if (response.isHttpError()) {
                message = "The server answered " + response.getStatus() + " " + (response.getReason() == null ? "" : response.getReason());
            } else if (partial == null || sinkHolder[0] == null) {
                message = "The server sent no data";
            } else {
                showProgress(sinkHolder[0].getWritten(), true);
                // 사용자가 새 대상을 고르고 있으면 그 선택이 적용되도록 옮기기 전에 기다린다.
                awaitChoice();
                File saved = cancelled ? null : moveToTarget();
                if (saved == null) {
                    end = DownloadState.CANCELLED;
                    message = "Cancelled (the file already exists)";
                } else {
                    end = DownloadState.COMPLETED;
                    message = "Saved to " + saved.getPath();
                }
            }
        } catch (InterruptedException e) {
            // 대상 고르기를 기다리다 중단되면 실패로 끝내고 중단 표시를 되살린다.
            Thread.currentThread().interrupt();
            message = "Download failed: interrupted";
        } catch (IOException e) {
            message = "Download failed: " + e.getMessage();
        } catch (RuntimeException e) {
            message = "Download failed: " + e;
        } finally {
            closeQuietly(sinkHolder[0]);
            if (end != DownloadState.COMPLETED && partial != null) {
                partial.delete();
            }
            if (cancelled && end != DownloadState.COMPLETED) {
                end = DownloadState.CANCELLED;
                message = "Cancelled";
            }
            // 실패나 취소로 끝나도 마지막으로 받은 양을 보인다(진행 보고는 간격을 두므로 마지막 값이 빠질 수 있다).
            if (end != DownloadState.COMPLETED && sinkHolder[0] != null) {
                showProgress(sinkHolder[0].getWritten(), true);
            }
            showState(end, message);
            String report;
            if (end == DownloadState.COMPLETED) {
                report = "Download completed: " + message.substring("Saved to ".length());
            } else if (end == DownloadState.CANCELLED) {
                report = "Download cancelled: " + request.getFileName();
            } else {
                report = "Download failed: " + request.getFileName() + " (" + message + ")";
            }
            manager.status(report);
            manager.finished(this);
        }
    }

    /**
     * Moves the finished partial file to the target, asking for confirmation before an existing file is replaced.
     * <p>
     * 완성된 부분 파일을 대상으로 옮기며 기존 파일을 교체하기 전에 확인을 받는다.
     *
     * @return the file that was written, or null if the user refused to replace and gave up / 쓴 파일. 사용자가 교체를 거부하고 포기했으면 null
     * @throws IOException if the file cannot be moved / 파일을 옮길 수 없으면
     */
    private File moveToTarget() throws IOException {
        File t = target;
        // 대상이 이미 있으면 교체를 확인받고, 거부하면 다른 대상을 고르게 한다.
        while (t.exists()) {
            final File existing = t;
            final boolean[] answer = new boolean[1];
            final DownloadHandle v = view;
            if (v == null || cancelled) {
                return null;
            }
            boolean ran = SwingDownloadHandler.andWait(new Runnable() {
                /**
                 * Asks the user.
                 * <p>
                 * 사용자에게 묻는다.
                 */
                public void run() {
                    answer[0] = v.confirmOverwrite(existing);
                }
            });
            if (!ran) {
                return null;
            }
            if (answer[0]) {
                break;
            }
            final File[] chosen = new File[1];
            SwingDownloadHandler.andWait(new Runnable() {
                /**
                 * Lets the user choose another file.
                 * <p>
                 * 사용자가 다른 파일을 고르게 한다.
                 */
                public void run() {
                    chosen[0] = v.chooseTarget(existing);
                }
            });
            if (chosen[0] == null) {
                return null;
            }
            t = chosen[0].getAbsoluteFile();
            changeTarget(t);
        }
        SafeFiles.moveReplacing(partial, t);
        partial = null;
        return t;
    }

    /**
     * Closes a stream and ignores errors.
     * <p>
     * 스트림을 닫고 오류는 무시한다.
     *
     * @param s the stream or null / 스트림 또는 null
     */
    private static void closeQuietly(OutputStream s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                // 닫기 실패는 이미 결과에 반영되었으므로 무시한다.
                return;
            }
        }
    }
}
