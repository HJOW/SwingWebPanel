package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.io.IOException;

/**
 * The default {@link OutputFactory}: it opens a {@link JavaSoundOutput} on the default mixer of Java Sound. On a machine without a sound
 * device it fails with an {@link IOException}, and the driver then plays into a {@link NullOutput} instead (it never refuses to run).
 * <p>
 * 기본 {@link OutputFactory}: Java Sound 의 기본 믹서에 {@link JavaSoundOutput} 을 연다. 소리 장치가 없는 컴퓨터에서는 {@link IOException} 으로
 * 실패하며, 드라이버는 그때 {@link NullOutput} 으로 재생한다(실행을 거부하지 않는다).
 */
public final class SystemOutputFactory implements OutputFactory {

    /**
     * Creates the factory.
     * <p>
     * 팩터리를 만든다.
     */
    public SystemOutputFactory() {
    }

    /**
     * Opens a Java Sound output.
     * <p>
     * Java Sound 출력을 연다.
     *
     * @param sampleRate   the sample rate of the data in Hz / 데이터의 샘플링 주파수(Hz)
     * @param channels     the number of channels wanted / 원하는 채널 수
     * @param bufferFrames the capacity of the queue in frames / 큐의 용량(프레임)
     * @return the output, stopped / 멈춘 상태의 출력
     * @throws IOException if no device can be opened / 장치를 열 수 없으면
     */
    public AudioOutput open(int sampleRate, int channels, int bufferFrames) throws IOException {
        return JavaSoundOutput.open(JavaSoundOutput.system(), sampleRate, channels, bufferFrames);
    }
}
