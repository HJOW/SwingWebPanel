package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollection;
import org.duckdns.hjow.swingwebpanel.render.MediaControlSource;
import org.duckdns.hjow.swingwebpanel.render.MediaControlState;

/**
 * Runs HTML audio through the common media state machine without a script realm.
 * <p>
 * 스크립트 렐름 없이 공통 미디어 상태 기계로 HTML 오디오를 실행한다.
 */
public final class UnscriptedMedia implements MediaControlSource {

    /** Document services. / 문서 서비스. */
    private final HostEnvironment host;
    /** Completion queue for media tasks. / 미디어 작업의 완료 큐. */
    private final ScriptLoads loads = new ScriptLoads();
    /** Audio states and player ownership. / 오디오 상태와 재생기 소유권. */
    private final MediaElements media;
    /** Whether a queue drain is already scheduled. / 완료 큐를 비우는 작업이 이미 예약됐는지 여부. */
    private final AtomicBoolean drainPosted = new AtomicBoolean();
    /** Lifecycle flag shared with the fetch and playback threads. / 가져오기·재생 스레드에 공유하는 수명 상태. */
    private volatile boolean closed;

    /**
     * Creates media using the document's scheduler and resource services.
     * <p>
     * 문서의 스케줄러와 리소스 서비스로 미디어를 만든다.
     * @param host the media services / 미디어 서비스
     */
    public UnscriptedMedia(HostEnvironment host) {
        this.host = host;
        media = new MediaElements(host, loads);
        loads.setNotifier(new Runnable() {
            /** Schedules delivery from whichever thread completed a task. / 작업을 완료한 스레드에서 전달을 예약한다. */
            public void run() {
                requestDrain();
            }
        });
    }

    /**
     * Starts selection for parser-created audio on the confirmed document's thread.
     * <p>
     * 확정된 문서 스레드에서 파서가 만든 오디오의 리소스 선택을 시작한다.
     * @param elements the audio elements in document order / 문서 순서의 오디오 요소
     */
    public void start(ElementCollection elements) {
        synchronized (media) {
            if (closed) {
                return;
            }
            int count = elements.getLength();
            // 마크업에서 얻은 오디오의 리소스를 문서 순서대로 선택한다.
            for (int i = 0; i < count; i++) {
                media.insertedMedia(elements.item(i));
            }
        }
    }

    /**
     * Applies a media control on the document thread. / 문서 스레드에서 미디어 컨트롤을 적용한다.
     * @param target the audio element / 오디오 요소
     * @param part the control part / 컨트롤 부분
     * @param fraction the seek or volume fraction / 탐색 또는 음량 비율
     */
    public void control(Element target, int part, double fraction) {
        synchronized (media) {
            if (!closed) {
                media.control(target, part, fraction);
            }
        }
    }

    /** Returns the current controls state. / 현재 컨트롤 상태를 반환한다. */
    public MediaControlState getMediaControls(Element target) {
        synchronized (media) {
            return media.getMediaControls(target);
        }
    }

    /** Returns whether the document has finished waiting for audio. / 문서의 오디오 기다림이 끝났는지 반환한다. */
    public boolean isLoadComplete() {
        return closed || !media.isDelayingLoad();
    }

    /** Requests a single pending delivery on the document queue. / 문서 큐에 전달 작업을 하나만 예약한다. */
    private void requestDrain() {
        if (closed || !drainPosted.compareAndSet(false, true)) {
            return;
        }
        try {
            host.getScheduler().post(new Runnable() {
                /** Runs the ready tasks on the document thread. / 문서 스레드에서 준비된 작업을 실행한다. */
                public void run() {
                    drain();
                }
            }, 0);
        } catch (RejectedExecutionException e) {
            drainPosted.set(false);
        }
    }

    /** Delivers ready tasks and reports a finished load. / 준비된 작업을 전달하고 로딩 완료를 보고한다. */
    private void drain() {
        drainPosted.set(false);
        synchronized (media) {
            boolean waiting = media.isDelayingLoad();
            ScriptLoads.Load next;
            // 요청 완료, 재생기 상태, DOM 이벤트를 같은 문서 큐에서 순서대로 처리한다.
            while (!closed && (next = loads.pollReady()) != null) {
                media.deliver((MediaElements.Task) next.tag);
            }
            if (!closed && waiting && !media.isDelayingLoad()) {
                host.pageLoaded();
            }
        }
    }

    /** Cancels requests and closes outputs once, including on a component cleanup. / 컴포넌트 정리 등에서 요청과 출력을 한 번 해제한다. */
    public void close() {
        synchronized (media) {
            if (!closed) {
                closed = true;
                loads.close();
                media.dispose();
            }
        }
    }
}
