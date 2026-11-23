package org.duckdns.hjow.swingwebpanel.download;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.core.DownloadHandler;
import org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException;
import org.duckdns.hjow.swingwebpanel.core.NavigationRequest;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;

/**
 * The download manager of a browser component (plan item P11-09, API contract section 7): it receives the navigations that must be
 * downloaded, shows one non-modal popup per download, transfers the data on a background thread through the component's request layer
 * (user agent, extra headers, cookies, the SSL extension point and the scheme restrictions of the {@link ResourceFetcher}) and saves it
 * as a file. Several downloads can run at the same time.
 * <p>
 * How a transfer goes: the data is written to a partial file <code>name.part-xxxxxxxx</code> in the directory of the target (by default
 * the Downloads folder of the user, or the home directory) and renamed when it is complete; an existing target is replaced only after the
 * user confirmed it in the popup, and replacing never loses either file. A cancelled or failed download deletes its partial file; a
 * completed file is never deleted, not even by {@link #cancelAll()} or {@link #dispose()}.
 * <p>
 * Hosts and tests replace the popup by overriding {@link #createDownloadPopup}. In a headless environment (no screen) a download is
 * cancelled at once and the reason is reported through the {@link DownloadStatusSink}.
 * <p>
 * Usage (inside the component): <code>new SwingDownloadHandler(this, fetcher, cookies, sink)</code> and install it with the session's
 * <code>setDownloadHandler</code>; call {@link #dispose()} when the component is disposed and {@link #cancelAll()} on <code>clean()</code>.
 * <p>
 * 브라우저 컴포넌트의 다운로드 관리자(계획 P11-09, API 계약 7절): 내려받아야 하는 이동을 받아 다운로드마다 모달이 아닌 팝업을 하나씩 보여 주고,
 * 컴포넌트의 요청 계층(User-Agent, 추가 헤더, 쿠키, SSL 확장 지점, {@link ResourceFetcher} 의 스킴 제한)으로 백그라운드 스레드에서 데이터를
 * 전송하여 파일로 저장한다. 여러 다운로드를 동시에 처리할 수 있다.
 * <p>
 * 전송 방식: 데이터는 대상이 있는 디렉터리(기본은 사용자의 Downloads 폴더, 없으면 홈 디렉터리)의 부분 파일 <code>name.part-xxxxxxxx</code> 에
 * 쓰고 끝나면 이름을 바꾼다. 기존 대상은 팝업에서 사용자가 확인한 뒤에만 교체하며 교체 중에도 어느 쪽 파일도 잃지 않는다. 취소되거나 실패한
 * 다운로드는 부분 파일을 지우고, 완료된 파일은 {@link #cancelAll()} 이나 {@link #dispose()} 로도 지우지 않는다.
 * <p>
 * 호스트와 시험은 {@link #createDownloadPopup} 을 재정의하여 팝업을 바꾼다. 헤드리스 환경(화면 없음)에서는 다운로드를 즉시 취소하고 이유를
 * {@link DownloadStatusSink} 로 알린다.
 * <p>
 * 사용법(컴포넌트 안에서): <code>new SwingDownloadHandler(this, fetcher, cookies, sink)</code> 를 만들어 세션의 <code>setDownloadHandler</code> 로
 * 설치하고, 컴포넌트가 폐기될 때 {@link #dispose()}, <code>clean()</code> 때 {@link #cancelAll()} 을 호출한다.
 */
public class SwingDownloadHandler implements DownloadHandler {

    /** The component whose window is the parent of the popups. 창이 팝업의 부모가 되는 컴포넌트. */
    private final Component owner;

    /** The request layer of the component. 컴포넌트의 요청 계층. */
    private final ResourceFetcher fetcher;

    /** The cookies of the component. 컴포넌트의 쿠키. */
    private final CookieStore cookies;

    /** Receiver of status messages or null. 상태 메시지를 받는 곳 또는 null. */
    private final DownloadStatusSink sink;

    /** Downloads that have not finished. 아직 끝나지 않은 다운로드. */
    private final Set<DownloadTask> active = Collections.synchronizedSet(new HashSet<DownloadTask>());

    /** Guards waiting for the end of all downloads. 모든 다운로드가 끝나기를 기다리는 데 쓰는 잠금. */
    private final Object idleLock = new Object();

    /** True after {@link #dispose()}: no new download is accepted. dispose 뒤에는 true: 새 다운로드를 받지 않는다. */
    private volatile boolean disposed;

    /**
     * Creates a download manager.
     * <p>
     * 다운로드 관리자를 만든다.
     *
     * @param owner   the browser component; its window becomes the parent of the popups (may be null) / 브라우저 컴포넌트. 그 창이 팝업의 부모가 된다(null 가능)
     * @param fetcher the request layer of the component (SSL hook, limits, scheme policy), not null / 컴포넌트의 요청 계층(SSL 훅, 제한, 스킴 정책). null 불가
     * @param cookies the cookie store of the component, not null / 컴포넌트의 쿠키 저장소. null 불가
     * @param sink    receiver of short status messages, may be null / 짧은 상태 메시지를 받는 곳. null 가능
     */
    public SwingDownloadHandler(Component owner, ResourceFetcher fetcher, CookieStore cookies, DownloadStatusSink sink) {
        if (fetcher == null || cookies == null) {
            throw new IllegalArgumentException("fetcher and cookies must not be null");
        }
        this.owner = owner;
        this.fetcher = fetcher;
        this.cookies = cookies;
        this.sink = sink;
    }

    /**
     * Starts the download of a navigation that turned out to be a file (called on a background thread by the session).
     * <p>
     * 파일로 판명된 이동의 다운로드를 시작한다(세션이 백그라운드 스레드에서 호출한다).
     *
     * @param request  the navigation request that found the download / 다운로드를 발견한 이동 요청
     * @param download description of the response / 응답의 설명
     */
    public void downloadRequired(NavigationRequest request, DownloadRequiredException download) {
        start(DownloadRequest.of(request, download));
    }

    /**
     * Starts a download. In a headless environment or after {@link #dispose()} nothing is transferred.
     * <p>
     * 다운로드를 시작한다. 헤드리스 환경이거나 {@link #dispose()} 뒤에는 아무것도 전송하지 않는다.
     *
     * @param request the download / 다운로드
     * @return the control of the started download, or null if it was refused / 시작한 다운로드의 제어 객체. 거부했으면 null
     */
    public DownloadControl start(DownloadRequest request) {
        if (disposed) {
            return null;
        }
        if (isHeadless()) {
            status("Download cancelled (no display available): " + request.getFileName());
            return null;
        }
        DownloadTask task = new DownloadTask(this, request, new File(getDefaultDirectory(), request.getFileName()));
        active.add(task);
        task.begin();
        return task;
    }

    /**
     * Cancels every running download and closes its popup. Finished downloads and their files are not touched. The partial files are
     * deleted by the transfer threads; use {@link #awaitIdle} to wait for that.
     * <p>
     * 실행 중인 모든 다운로드를 취소하고 팝업을 닫는다. 끝난 다운로드와 그 파일은 건드리지 않는다. 부분 파일은 전송 스레드가 지우며,
     * 기다리려면 {@link #awaitIdle} 을 쓴다.
     */
    public void cancelAll() {
        List<DownloadTask> copy;
        synchronized (active) {
            copy = new ArrayList<DownloadTask>(active);
        }
        // 실행 중인 다운로드마다 취소하고 팝업을 닫는다.
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).cancel();
            copy.get(i).closeView();
        }
    }

    /**
     * Cancels everything like {@link #cancelAll()} and refuses new downloads from now on.
     * <p>
     * {@link #cancelAll()} 처럼 모두 취소하고 이후의 새 다운로드를 거부한다.
     */
    public void dispose() {
        disposed = true;
        cancelAll();
    }

    /**
     * Returns the number of downloads that have not finished (including those that are still cancelling).
     * <p>
     * 아직 끝나지 않은 다운로드의 수를 반환한다(취소를 마무리하는 중인 것 포함).
     *
     * @return count / 개수
     */
    public int getActiveCount() {
        return active.size();
    }

    /**
     * Waits until all downloads have ended (their partial files are then deleted or renamed).
     * <p>
     * 모든 다운로드가 끝날 때까지 기다린다(그때 부분 파일은 지워지거나 이름이 바뀌어 있다).
     *
     * @param timeoutMillis longest time to wait / 최대 대기 시간
     * @return true if no download is active any more / 더 이상 활성 다운로드가 없으면 true
     */
    public boolean awaitIdle(long timeoutMillis) {
        long end = System.currentTimeMillis() + timeoutMillis;
        synchronized (idleLock) {
            // 활성 다운로드가 없어지거나 시간이 다할 때까지 기다린다.
            while (!active.isEmpty()) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) {
                    return false;
                }
                try {
                    idleLock.wait(Math.min(left, 100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates the popup of a download. Called on the event dispatch thread. Hosts and tests override it to show their own popup or to
     * observe the download; the default creates a {@link SwingDownloadDialog} whose parent is the window of the owner component.
     * <p>
     * 다운로드의 팝업을 만든다. 이벤트 디스패치 스레드에서 호출된다. 호스트와 시험은 이것을 재정의하여 자신의 팝업을 보여 주거나 다운로드를
     * 관찰한다. 기본 구현은 부모가 소유 컴포넌트의 창인 {@link SwingDownloadDialog} 를 만든다.
     *
     * @param request what is downloaded / 내려받는 대상
     * @param control how the popup can cancel the download or change its target / 팝업이 다운로드를 취소하거나 대상을 바꾸는 방법
     * @return the popup / 팝업
     */
    protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
        Window parent = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        return new SwingDownloadDialog(parent, request, control);
    }

    /**
     * Tells whether the environment has no screen (downloads are then cancelled). Tests override it.
     * <p>
     * 환경에 화면이 없는지 알려 준다(이때 다운로드는 취소된다). 시험이 재정의한다.
     *
     * @return true if headless / 헤드리스이면 true
     */
    protected boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    /**
     * Returns the directory in which downloads are saved by default: the Downloads folder of the user if it exists, else the home
     * directory.
     * <p>
     * 다운로드를 기본으로 저장하는 디렉터리를 반환한다: 사용자의 Downloads 폴더가 있으면 그것, 없으면 홈 디렉터리.
     *
     * @return directory / 디렉터리
     */
    protected File getDefaultDirectory() {
        File home = new File(System.getProperty("user.home", "."));
        File downloads = new File(home, "Downloads");
        return downloads.isDirectory() ? downloads : home;
    }

    /**
     * Reports a short status message to the sink.
     * <p>
     * 짧은 상태 메시지를 싱크에 보고한다.
     *
     * @param message the message / 메시지
     */
    void status(String message) {
        if (sink != null) {
            try {
                sink.downloadStatus(message);
            } catch (RuntimeException e) {
                Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, e);
            }
        }
    }

    /**
     * Returns the request layer.
     * <p>
     * 요청 계층을 반환한다.
     *
     * @return the fetcher / 가져오기 객체
     */
    ResourceFetcher getFetcher() {
        return fetcher;
    }

    /**
     * Returns the cookie store.
     * <p>
     * 쿠키 저장소를 반환한다.
     *
     * @return the cookies / 쿠키
     */
    CookieStore getCookies() {
        return cookies;
    }

    /**
     * Creates the popup of a task (event dispatch thread).
     * <p>
     * 작업의 팝업을 만든다(이벤트 디스패치 스레드).
     *
     * @param task the task / 작업
     * @return the popup or null if it could not be created / 팝업. 만들 수 없었으면 null
     */
    DownloadHandle makePopup(DownloadTask task) {
        try {
            return createDownloadPopup(task.getRequest(), task);
        } catch (RuntimeException e) {
            status("Download popup failed: " + e);
            return null;
        }
    }

    /**
     * Called by a task when it has ended.
     * <p>
     * 작업이 끝났을 때 작업이 호출한다.
     *
     * @param task the task / 작업
     */
    void finished(DownloadTask task) {
        active.remove(task);
        synchronized (idleLock) {
            idleLock.notifyAll();
        }
    }

    /**
     * Runs code on the event dispatch thread later.
     * <p>
     * 코드를 나중에 이벤트 디스패치 스레드에서 실행한다.
     *
     * @param r the code / 코드
     */
    static void later(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    /**
     * Runs code on the event dispatch thread and waits for it.
     * <p>
     * 코드를 이벤트 디스패치 스레드에서 실행하고 끝나기를 기다린다.
     *
     * @param r the code / 코드
     * @return false if the wait was interrupted or the code failed / 대기가 중단되었거나 코드가 실패했으면 false
     */
    static boolean andWait(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
            return true;
        }
        try {
            SwingUtilities.invokeAndWait(r);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }
}
