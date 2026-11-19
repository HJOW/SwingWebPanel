package org.duckdns.hjow.swingwebpanel.media.playback;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Receives what a {@link MediaPlayer} reports (plan items P18-08, P18-05-c). Every method is called on the playback thread of the player,
 * outside its internal locks, so an implementation must only record the fact or queue work for the owner of the page and return quickly.
 * Nothing is reported after {@link MediaPlayer#close()} returned. The player holds its listener weakly: the owner keeps the listener
 * reachable for as long as it wants reports.
 * <p>
 * {@link MediaPlayer} 가 알리는 것을 받는다(계획 P18-08, P18-05-c). 모든 메서드는 재생기의 재생 스레드에서 내부 잠금 밖에서 호출되므로, 구현은 사실을
 * 기록하거나 페이지 소유자에게 작업을 올리기만 하고 빨리 돌아와야 한다. {@link MediaPlayer#close()} 가 돌아온 뒤에는 아무것도 알리지 않는다.
 * 재생기는 리스너를 약하게 참조하므로, 보고를 원하는 동안은 소유자가 리스너를 도달 가능하게 유지해야 한다.
 */
public interface PlaybackListener {

    /**
     * The headers were read and the media can be played: the duration and the format are known.
     * <p>
     * 헤더를 읽었고 미디어를 재생할 수 있다: 길이와 형식을 안다.
     *
     * @param info the facts about the media / 미디어에 대한 정보
     */
    void loaded(MediaInfo info);

    /**
     * The data cannot be played (unsupported format, damaged headers, too large). Only reported once, instead of {@link #loaded(MediaInfo)}.
     * <p>
     * 데이터를 재생할 수 없다(지원하지 않는 형식, 손상된 헤더, 너무 큼). {@link #loaded(MediaInfo)} 대신 한 번만 알린다.
     *
     * @param error the reason / 이유
     */
    void loadFailed(MediaException error);

    /**
     * The sound began: the output was opened and accepted the first data after a play request (also a resume after a pause, and the restart
     * after the end). Reported once per start, so a page can tell that <code>play()</code> really worked.
     * <p>
     * 소리가 시작되었다: 재생 요청(일시정지 뒤의 재개와 끝난 뒤의 재시작 포함)에 이어 출력이 열리고 첫 데이터를 받아들였다. 시작마다 한 번 알리므로
     * 페이지는 <code>play()</code> 가 정말 동작했음을 알 수 있다.
     */
    void started();

    /**
     * A seek ended: the next sound comes from the new position.
     * <p>
     * 탐색이 끝났다: 다음 소리는 새 위치에서 나온다.
     */
    void seeked();

    /**
     * The playback wrapped around to the start because of the loop flag.
     * <p>
     * 반복 플래그 때문에 재생이 처음으로 되돌아갔다.
     */
    void looped();

    /**
     * The end was reached and the last sound has been rendered (not reported while looping).
     * <p>
     * 끝에 닿았고 마지막 소리까지 재생되었다(반복 중에는 알리지 않는다).
     */
    void ended();

    /**
     * The length of the media became known only now (a source that could not tell its length in advance reached its end).
     * <p>
     * 미디어의 길이를 지금에서야 알게 되었다(길이를 미리 말하지 못하던 소스가 끝에 닿았다).
     *
     * @param seconds the duration / 길이(초)
     */
    void durationKnown(double seconds);

    /**
     * Playback is going on: a regular report (at least four times a second) that the position moved.
     * <p>
     * 재생이 진행 중이다: 위치가 움직였다는 정기 보고(초당 4번 이상).
     */
    void timeUpdate();

    /**
     * Playback failed and stopped: the data turned out to be damaged, or the output device cannot be opened. The player stays loaded; a
     * later {@link MediaPlayer#play()} tries again.
     * <p>
     * 재생이 실패하여 멈췄다: 데이터가 손상된 것으로 드러났거나 출력 장치를 열 수 없다. 재생기는 로드된 채로 있으며 이후의
     * {@link MediaPlayer#play()} 가 다시 시도한다.
     *
     * @param error the reason (ACCESS_FAILED for the device, CORRUPT for damaged data) / 이유(장치는 ACCESS_FAILED, 손상된 데이터는 CORRUPT)
     */
    void failed(MediaException error);
}
