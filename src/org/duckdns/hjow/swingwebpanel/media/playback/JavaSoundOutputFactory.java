package org.duckdns.hjow.swingwebpanel.media.playback;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The standard output: a {@link SourceDataLine} of the default mixer of Java Sound (public API {@code javax.sound.sampled} only). The line
 * buffer holds about 150 ms, so a pause or a seek reacts at once (the queue is stopped or flushed) and a change of the volume is heard within
 * that time. When the machine has no audio device, or the device cannot play the format, {@link #open(int, int)} fails with
 * {@link MediaException.Kind#ACCESS_FAILED}.
 * <p>
 * 표준 출력: Java Sound 의 기본 믹서의 {@link SourceDataLine}(공개 API {@code javax.sound.sampled} 만 사용). 라인 버퍼는 약 150 ms 를 담으므로 일시정지와
 * 탐색은 곧바로 반응하고(큐를 멈추거나 비움) 음량 변경은 그 시간 안에 들린다. 컴퓨터에 오디오 장치가 없거나 장치가 그 형식을 재생할 수 없으면
 * {@link #open(int, int)} 는 {@link MediaException.Kind#ACCESS_FAILED} 로 실패한다.
 */
public final class JavaSoundOutputFactory implements AudioOutputFactory {

    /**
     * The line buffer length in hundredths of a second.
     * <p>
     * 라인 버퍼 길이(100분의 1초 단위).
     */
    private static final int BUFFER_HUNDREDTHS = 15;

    /**
     * Creates the factory.
     * <p>
     * 팩터리를 만든다.
     */
    public JavaSoundOutputFactory() {
    }

    /**
     * Opens a line for the format.
     * <p>
     * 형식에 맞는 라인을 연다.
     *
     * @param sampleRate the frames per second / 초당 프레임 수
     * @param channels   1 or 2 / 1 또는 2
     * @return the output, stopped / 멈춘 상태의 출력
     * @throws MediaException ACCESS_FAILED if no line can be opened / 라인을 열 수 없으면 ACCESS_FAILED
     */
    public AudioOutput open(int sampleRate, int channels) throws MediaException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, channels, channels * 2, sampleRate, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            // 지원하는 라인이 없으면(오디오 장치 없음) 곧바로 실패로 알린다.
            if (!AudioSystem.isLineSupported(info)) {
                throw new MediaException(MediaException.Kind.ACCESS_FAILED, "no audio output device supports " + sampleRate + " Hz, " + channels
                        + " channel(s)");
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            int frameBytes = channels * 2;
            int bufferBytes = Math.max(frameBytes * 256, (int) ((long) sampleRate * BUFFER_HUNDREDTHS / 100) * frameBytes);
            line.open(format, bufferBytes);
            return new LineOutput(line, frameBytes);
        } catch (LineUnavailableException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "the audio output device is not available: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "no audio output device: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "access to the audio device is denied: " + e.getMessage(), e);
        }
    }

    /**
     * The output on a Java Sound line.
     * <p>
     * Java Sound 라인 위의 출력.
     */
    private static final class LineOutput implements AudioOutput {

        /**
         * The open line.
         * <p>
         * 열린 라인.
         */
        private final SourceDataLine line;

        /**
         * The bytes of one frame.
         * <p>
         * 프레임 하나의 바이트 수.
         */
        private final int frameBytes;

        /**
         * The line buffer size in bytes.
         * <p>
         * 라인 버퍼 크기(바이트).
         */
        private final int bufferBytes;

        /**
         * Wraps an open line.
         * <p>
         * 열린 라인을 감싼다.
         *
         * @param line       the line / 라인
         * @param frameBytes the bytes of one frame / 프레임 하나의 바이트 수
         */
        LineOutput(SourceDataLine line, int frameBytes) {
            this.line = line;
            this.frameBytes = frameBytes;
            this.bufferBytes = line.getBufferSize();
        }

        /**
         * Writes what fits into the free part of the line buffer.
         * <p>
         * 라인 버퍼의 빈 부분에 들어가는 만큼 쓴다.
         *
         * @param data   the bytes / 바이트
         * @param offset the start / 시작
         * @param length the bytes offered / 제안한 바이트 수
         * @return the bytes taken / 받아들인 바이트 수
         */
        public int write(byte[] data, int offset, int length) {
            int room = line.available();
            int n = Math.min(length, room);
            n -= n % frameBytes;
            return n <= 0 ? 0 : line.write(data, offset, n);
        }

        /**
         * Returns the frames in the line buffer.
         * <p>
         * 라인 버퍼에 있는 프레임 수를 반환한다.
         *
         * @return the frames / 프레임 수
         */
        public long getQueuedFrames() {
            int queued = bufferBytes - line.available();
            return queued <= 0 ? 0 : queued / frameBytes;
        }

        /**
         * Starts the line.
         * <p>
         * 라인을 시작한다.
         */
        public void start() {
            line.start();
        }

        /**
         * Stops the line; the queue stays.
         * <p>
         * 라인을 멈춘다. 큐는 남는다.
         */
        public void stop() {
            line.stop();
        }

        /**
         * Discards the queue.
         * <p>
         * 큐를 버린다.
         */
        public void flush() {
            line.flush();
        }

        /**
         * Stops, discards and closes the line.
         * <p>
         * 라인을 멈추고 비우고 닫는다.
         */
        public void close() {
            try {
                line.stop();
                line.flush();
            } finally {
                line.close();
            }
        }
    }
}
