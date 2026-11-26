package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollection;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.MediaRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.UnscriptedMedia;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;

/**
 * Connects the audio of a script-disabled document to its view and document queue.
 * <p>
 * 스크립트가 비활성인 문서의 오디오를 그 뷰와 문서 큐에 연결한다.
 */
final class PageAudio extends HostEnvironment implements MediaRequestSource {

    /** The visual and resource state of the page. / 페이지의 시각·리소스 상태. */
    private final PagePresentation presentation;
    /** The component providing audio output and viewport dimensions. / 오디오 출력과 뷰포트 크기를 제공하는 컴포넌트. */
    private final PageView view;
    /** Owning frame, or null on a top-level page. / 소유 프레임. 최상위 문서이면 null. */
    private final FrameContext frame;
    /** The automatic-features permission for this document, fixed when it is created. / 이 문서를 만들 때 고정하는 자동 기능 권한. */
    private final boolean autoplayAllowed;
    /** Document-scoped callbacks. / 문서 수명에 귀속된 콜백. */
    private final DocumentScheduler scheduler;
    /** Parser-created audio elements to discover on confirmation. / 페이지 확정 때 살펴볼 파서 생성 오디오 요소. */
    private final ElementCollection elements;
    /** Shared media state machine without a script realm. / 스크립트 렐름을 이용하지 않는 공통 미디어 상태 기계. */
    final UnscriptedMedia media;

    /**
     * Prepares media without starting requests on the parsing thread.
     * <p>
     * 파서 스레드에서 요청을 시작하지 않고 미디어를 준비한다.
     * @param session the owning component session / 소유 컴포넌트 세션
     * @param view the component view / 컴포넌트 뷰
     * @param presentation the confirmed page's resources / 확정될 페이지의 리소스
     * @param frame the owning frame or null / 소유 프레임 또는 null
     * @param elements the parsed audio elements / 파싱한 오디오 요소
     */
    PageAudio(BrowserSession session, PageView view, PagePresentation presentation, FrameContext frame, ElementCollection elements) {
        this.view = view;
        this.frame = frame;
        autoplayAllowed = frame == null || frame.flags().allowsScripts();
        this.presentation = presentation;
        this.elements = elements;
        scheduler = new DocumentScheduler(session);
        media = new UnscriptedMedia(this);
        presentation.setMediaControlSource(media);
    }

    /**
     * Starts media only for the confirmed document. / 확정된 문서의 미디어만 시작한다.
     * @param snapshot the owning confirmed snapshot / 소유 확정 스냅샷
     * @return true while the snapshot is active / 스냅샷이 활성 상태이면 true
     */
    boolean activate(PageSnapshot snapshot) {
        if (!scheduler.activate(snapshot)) {
            return false;
        }
        media.start(elements);
        return true;
    }

    /** Releases document callbacks and media. / 문서 콜백과 미디어를 해제한다. */
    void close() {
        scheduler.close();
        media.close();
    }

    /** Returns the scheduler for this document. / 이 문서의 스케줄러를 반환한다. */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /** Returns the component output factory. / 컴포넌트의 출력 팩터리를 반환한다. */
    public AudioOutputFactory getAudioOutputFactory() {
        return view.getAudioOutputFactory();
    }

    /** Checks the frame's sandboxed automatic-features flag. / 프레임의 자동 기능 샌드박스 제한을 확인한다. */
    public boolean allowsMediaAutoplay() {
        return autoplayAllowed;
    }

    /** Returns the media request service. / 미디어 요청 서비스를 반환한다. */
    public MediaRequestSource getMediaRequestSource() {
        return this;
    }

    /**
     * Uses the common page request layer. / 공통 페이지 요청 계층을 사용한다.
     * @param absoluteUrl the resource address / 리소스 주소
     * @param callback the result receiver / 결과를 받을 객체
     * @return the cancellation handle / 취소 핸들
     */
    public Request request(String absoluteUrl, Callback callback) {
        return presentation.requestMedia(absoluteUrl, callback);
    }

    /** Returns the width in CSS pixels. / CSS 픽셀 단위 너비를 반환한다. */
    public int getViewportWidth() {
        return frame == null ? view.viewportWidth() : frame.viewportWidth();
    }

    /** Returns the height in CSS pixels. / CSS 픽셀 단위 높이를 반환한다. */
    public int getViewportHeight() {
        return frame == null ? view.viewportHeight() : frame.viewportHeight();
    }

    /**
     * Reports a result to the renderer. / 리소스 결과를 렌더러에 보고한다.
     * @param absoluteUrl the resource address / 리소스 주소
     * @param error the failure reason or null / 실패 이유 또는 null
     */
    public void mediaStateChanged(String absoluteUrl, String error) {
        presentation.mediaState(absoluteUrl, error);
    }

    /**
     * Redraws the media controls. / 미디어 컨트롤을 다시 그린다.
     * @param target the audio element that changed / 상태가 바뀐 오디오 요소
     */
    public void mediaControlsChanged(Element target) {
        presentation.canvasChanged();
    }

    /** Requests a new report when media stops delaying the load. / 미디어의 로드 지연이 끝나면 다시 보고하도록 요청한다. */
    public void pageLoaded() {
        presentation.loadChanged();
    }
}
