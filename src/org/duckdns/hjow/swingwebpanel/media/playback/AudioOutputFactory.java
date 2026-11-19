package org.duckdns.hjow.swingwebpanel.media.playback;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Opens audio outputs for the players of a component (plan item P18-08). The component creates one factory through its overridable
 * {@code createAudioOutputFactory()} method, so a host can route the sound elsewhere and a test can record the PCM that would be played.
 * <p>
 * 컴포넌트의 재생기를 위해 오디오 출력을 연다(계획 P18-08). 컴포넌트는 재정의할 수 있는 {@code createAudioOutputFactory()} 메서드로 팩터리 하나를
 * 만들므로, 호스트는 소리를 다른 곳으로 보낼 수 있고 시험은 재생되었을 PCM 을 기록할 수 있다.
 */
public interface AudioOutputFactory {

    /**
     * Opens an output for 16-bit signed little-endian interleaved PCM. Called by the playback thread when playback starts for the first
     * time (and again after the output was closed at the end of the media).
     * <p>
     * 16비트 부호 있는 리틀 엔디언 인터리브 PCM 을 위한 출력을 연다. 재생이 처음 시작될 때(그리고 미디어 끝에서 출력을 닫은 뒤 다시) 재생 스레드가
     * 부른다.
     *
     * @param sampleRate the frames per second / 초당 프레임 수
     * @param channels   1 or 2 / 1 또는 2
     * @return the output, stopped / 멈춘 상태의 출력
     * @throws MediaException ACCESS_FAILED if there is no output device or it cannot be opened (the element then reports an error) / 출력 장치가
     *                        없거나 열 수 없으면 ACCESS_FAILED(요소는 오류를 알린다)
     */
    AudioOutput open(int sampleRate, int channels) throws MediaException;
}
