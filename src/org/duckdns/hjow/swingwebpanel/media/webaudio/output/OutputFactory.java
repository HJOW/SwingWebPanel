package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.io.IOException;

/**
 * Opens the sound output of a real-time Web Audio context (plan item P25-07). The host application can supply another factory to route
 * the sound elsewhere (a different device, a recorder), and a test supplies one that records the audio that would be played. The default
 * is {@link SystemOutputFactory}. {@link #open} is called by the rendering thread of the driver (never by the thread of the script), so
 * opening a slow device does not block a page.
 * <p>
 * 실시간 Web Audio 컨텍스트의 소리 출력을 연다(계획 P25-07). 호스트 애플리케이션은 소리를 다른 곳(다른 장치, 기록기)으로 보내려고 다른 팩터리를 줄 수
 * 있고, 시험은 재생되었을 오디오를 기록하는 팩터리를 준다. 기본은 {@link SystemOutputFactory} 이다. {@link #open} 은 드라이버의 렌더링 스레드가
 * 부르므로(스크립트의 스레드는 부르지 않는다) 느린 장치를 여는 일이 페이지를 막지 않는다.
 */
public interface OutputFactory {

    /**
     * Opens an output that accepts planar float audio. The returned output reports the channel count it really opened
     * ({@link AudioOutput#getChannels()}), which can be less than asked for.
     * <p>
     * 평면 float 오디오를 받는 출력을 연다. 반환한 출력은 실제로 연 채널 수({@link AudioOutput#getChannels()})를 알리며 요청한 수보다 적을 수 있다.
     *
     * @param sampleRate   the sample rate of the data in Hz / 데이터의 샘플링 주파수(Hz)
     * @param channels     the number of channels that the context would like to write / 컨텍스트가 쓰고 싶은 채널 수
     * @param bufferFrames the capacity of the queue in frames that decides the latency / 지연을 정하는 큐의 용량(프레임)
     * @return the output, stopped / 멈춘 상태의 출력
     * @throws IOException if there is no output device or it cannot be opened (the driver then falls back to the silent output) / 출력 장치가 없거나
     *                     열 수 없으면(드라이버는 그때 무음 출력으로 대신한다)
     */
    AudioOutput open(int sampleRate, int channels, int bufferFrames) throws IOException;
}
