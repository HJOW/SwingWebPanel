package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.duckdns.hjow.swingwebpanel.media.webaudio.decode.Resampler;

/**
 * The output on a Java Sound {@link SourceDataLine} of the default mixer (public API <code>javax.sound.sampled</code> only), playing 16-bit
 * signed little-endian PCM. The line is opened with a buffer of the capacity that the context asks for, and the {@link #write} method hands
 * the audio to the line in pieces that fit the free room of the line buffer, so it never blocks inside the Java Sound call: when the buffer
 * is full it sleeps for about the time in which the device plays one piece, and a {@link #close()} or a stop at that point is noticed within
 * a few milliseconds. The sample of a float is rounded to the nearest 16-bit value (an error of at most 1/65536 of the full scale) and
 * clipped; no dither is added.
 * <p>
 * If the device does not play the sample rate of the context, the output tries other rates in the order 48000, 44100, 96000, 32000, 24000,
 * 22050, 16000, 12000, 11025, 8000 Hz and converts the audio with the streaming {@link Resampler} (a windowed sinc of 100 dB stop band
 * attenuation) before it is written; the queue is still reported in frames of the context rate. A device that cannot play the number of
 * channels is tried with 2 and then 1 channel, and {@link #getChannels()} reports what was opened. Because the sample rate of the context
 * never depends on the device, a page sees the same <code>sampleRate</code> on every computer.
 * <p>
 * 기본 믹서의 Java Sound {@link SourceDataLine} 위의 출력(공개 API <code>javax.sound.sampled</code> 만 사용)이며 16비트 부호 있는 리틀 엔디언 PCM 을
 * 재생한다. 라인은 컨텍스트가 요청한 용량의 버퍼로 열리고, {@link #write} 는 라인 버퍼의 빈 자리에 들어가는 조각으로 오디오를 라인에 넘기므로 Java Sound
 * 호출 안에서 절대 블록하지 않는다: 버퍼가 가득 차면 장치가 한 조각을 재생하는 시간만큼 잠자고, 그 사이의 {@link #close()} 나 정지는 몇 밀리초 안에
 * 알아챈다. float 샘플은 가장 가까운 16비트 값으로 반올림되고(오차는 풀 스케일의 1/65536 이하) 잘리며 디더는 더하지 않는다.
 * <p>
 * 장치가 컨텍스트의 샘플링 주파수를 재생하지 못하면 출력은 48000, 44100, 96000, 32000, 24000, 22050, 16000, 12000, 11025, 8000 Hz 순서로 다른
 * 주파수를 시도하고 쓰기 전에 스트리밍 {@link Resampler}(저지 대역 감쇠 100 dB 의 창 sinc)로 오디오를 변환한다. 큐는 여전히 컨텍스트 주파수의
 * 프레임으로 보고한다. 장치가 채널 수를 재생하지 못하면 2채널, 1채널을 시도하며 {@link #getChannels()} 는 연 값을 알린다. 컨텍스트의 샘플링 주파수는
 * 장치에 의존하지 않으므로 페이지는 어떤 컴퓨터에서나 같은 <code>sampleRate</code> 를 본다.
 */
public final class JavaSoundOutput implements AudioOutput {

    /**
     * The access to the lines of the sound system; a test replaces it with a fake device, and {@link #system()} gives the real one.
     * <p>
     * 소리 시스템의 라인에 대한 접근. 시험은 가짜 장치로 바꾸고 {@link #system()} 은 실제 접근을 준다.
     */
    public interface Lines {

        /**
         * Tells whether the sound system can play a format.
         * <p>
         * 소리 시스템이 형식을 재생할 수 있는지 알려 준다.
         *
         * @param format the format / 형식
         * @return true if a line for the format exists / 형식을 위한 라인이 있으면 true
         */
        boolean isSupported(AudioFormat format);

        /**
         * Opens a line for a format with a buffer of a size.
         * <p>
         * 버퍼 크기를 정해 형식을 위한 라인을 연다.
         *
         * @param format      the format / 형식
         * @param bufferBytes the size of the line buffer in bytes / 라인 버퍼 크기(바이트)
         * @return the open line, stopped / 열린 라인(멈춤)
         * @throws LineUnavailableException if the line cannot be opened / 라인을 열 수 없으면
         */
        SourceDataLine open(AudioFormat format, int bufferBytes) throws LineUnavailableException;
    }

    /**
     * The real sound system: the default mixer of {@link AudioSystem}.
     * <p>
     * 실제 소리 시스템: {@link AudioSystem} 의 기본 믹서.
     */
    private static final class SystemLines implements Lines {

        /**
         * Asks the sound system; a machine without a mixer answers false.
         * <p>
         * 소리 시스템에 묻는다. 믹서가 없는 컴퓨터는 false 로 답한다.
         *
         * @param format the format / 형식
         * @return true if a line exists / 라인이 있으면 true
         */
        public boolean isSupported(AudioFormat format) {
            return AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format));
        }

        /**
         * Opens a line of the default mixer.
         * <p>
         * 기본 믹서의 라인을 연다.
         *
         * @param format      the format / 형식
         * @param bufferBytes the line buffer size / 라인 버퍼 크기
         * @return the open line / 열린 라인
         * @throws LineUnavailableException if the device is busy or missing / 장치가 사용 중이거나 없으면
         */
        public SourceDataLine open(AudioFormat format, int bufferBytes) throws LineUnavailableException {
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
            line.open(format, bufferBytes);
            return line;
        }
    }

    /**
     * The device rates that are tried after the rate of the context.
     * <p>
     * 컨텍스트의 주파수 다음에 시도하는 장치 주파수.
     */
    private static final int[] FALLBACK_RATES = {48000, 44100, 96000, 32000, 24000, 22050, 16000, 12000, 11025, 8000};

    /**
     * The longest sleep in milliseconds while the line buffer is full.
     * <p>
     * 라인 버퍼가 가득 찼을 때 잠자는 가장 긴 시간(밀리초).
     */
    private static final int MAX_SLEEP_MILLIS = 20;

    /**
     * The open line.
     * <p>
     * 열린 라인.
     */
    private final SourceDataLine line;

    /**
     * The sample rate of the data that {@link #write} accepts.
     * <p>
     * {@link #write} 가 받는 데이터의 샘플링 주파수.
     */
    private final int contextRate;

    /**
     * The sample rate of the line.
     * <p>
     * 라인의 샘플링 주파수.
     */
    private final int deviceRate;

    /**
     * The number of channels of the line.
     * <p>
     * 라인의 채널 수.
     */
    private final int channels;

    /**
     * The bytes of one frame (16 bits per channel).
     * <p>
     * 프레임 하나의 바이트 수(채널당 16비트).
     */
    private final int frameBytes;

    /**
     * The size of the line buffer in bytes.
     * <p>
     * 라인 버퍼의 크기(바이트).
     */
    private final int bufferBytes;

    /**
     * One converter per channel, or null if the device plays the rate of the context.
     * <p>
     * 채널마다 변환기 하나. 장치가 컨텍스트의 주파수를 재생하면 null.
     */
    private final Resampler[] resamplers;

    /**
     * The converted audio of the last write (only with converters).
     * <p>
     * 마지막 쓰기에서 변환한 오디오(변환기가 있을 때만).
     */
    private float[][] converted;

    /**
     * The 16-bit interleaved bytes of the last write.
     * <p>
     * 마지막 쓰기의 16비트 인터리브 바이트.
     */
    private byte[] pcm = new byte[0];

    /**
     * True while the line plays.
     * <p>
     * 라인이 재생하는 동안 true.
     */
    private volatile boolean started;

    /**
     * True after {@link #close()}.
     * <p>
     * {@link #close()} 뒤에 true.
     */
    private volatile boolean closed;

    /**
     * Wraps an open line.
     * <p>
     * 열린 라인을 감싼다.
     *
     * @param line        the open line / 열린 라인
     * @param contextRate the sample rate of the data in Hz / 데이터의 샘플링 주파수(Hz)
     * @param deviceRate  the sample rate of the line in Hz / 라인의 샘플링 주파수(Hz)
     * @param channels    the number of channels of the line / 라인의 채널 수
     */
    private JavaSoundOutput(SourceDataLine line, int contextRate, int deviceRate, int channels) {
        this.line = line;
        this.contextRate = contextRate;
        this.deviceRate = deviceRate;
        this.channels = channels;
        this.frameBytes = channels * 2;
        this.bufferBytes = line.getBufferSize();
        // 장치 주파수가 다르면 채널마다 변환기를 만든다.
        if (contextRate != deviceRate) {
            this.resamplers = new Resampler[channels];
            // 채널마다 변환기 하나를 만든다.
            for (int c = 0; c < channels; c++) {
                resamplers[c] = new Resampler(contextRate, deviceRate);
            }
        } else {
            this.resamplers = null;
        }
    }

    /**
     * Returns the access to the real sound system (the default mixer of Java Sound).
     * <p>
     * 실제 소리 시스템(Java Sound 의 기본 믹서)에 대한 접근을 반환한다.
     *
     * @return the access / 접근
     */
    public static Lines system() {
        return new SystemLines();
    }

    /**
     * Opens a line for the data, trying the rates and channel counts described in the class comment.
     * <p>
     * 클래스 설명의 주파수와 채널 수를 시도하며 데이터를 위한 라인을 연다.
     *
     * @param lines        the sound system / 소리 시스템
     * @param sampleRate   the sample rate of the data in Hz / 데이터의 샘플링 주파수(Hz)
     * @param channels     the number of channels that the caller would like / 호출자가 원하는 채널 수
     * @param bufferFrames the capacity of the buffer in frames of the data rate / 버퍼의 용량(데이터 주파수의 프레임)
     * @return the output, stopped / 멈춘 상태의 출력
     * @throws IOException if no line can be opened / 라인을 열 수 없으면
     */
    public static JavaSoundOutput open(Lines lines, int sampleRate, int channels, int bufferFrames) throws IOException {
        if (sampleRate < 1 || channels < 1 || bufferFrames < 1) {
            throw new IllegalArgumentException("sampleRate=" + sampleRate + " channels=" + channels + " bufferFrames=" + bufferFrames);
        }
        ArrayList<Integer> rates = new ArrayList<Integer>();
        rates.add(Integer.valueOf(sampleRate));
        // 컨텍스트의 주파수 다음에 대체 주파수를 중복 없이 시도 목록에 넣는다.
        for (int i = 0; i < FALLBACK_RATES.length; i++) {
            Integer r = Integer.valueOf(FALLBACK_RATES[i]);
            if (!rates.contains(r)) {
                rates.add(r);
            }
        }
        ArrayList<Integer> counts = new ArrayList<Integer>();
        counts.add(Integer.valueOf(channels));
        // 요청한 채널 수 다음에 2채널과 1채널을 시도한다.
        if (!counts.contains(Integer.valueOf(2))) {
            counts.add(Integer.valueOf(2));
        }
        if (!counts.contains(Integer.valueOf(1))) {
            counts.add(Integer.valueOf(1));
        }
        String lastProblem = "no format is supported";
        // 주파수를 바깥 반복으로, 채널 수를 안쪽 반복으로 시도해 처음 열리는 라인을 쓴다.
        for (int ri = 0; ri < rates.size(); ri++) {
            int rate = rates.get(ri).intValue();
            // 채널 수 후보를 하나씩 시도한다.
            for (int ci = 0; ci < counts.size(); ci++) {
                int ch = counts.get(ci).intValue();
                AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
                if (!lines.isSupported(format)) {
                    continue;
                }
                int deviceFrames = (int) Math.max(1L, ((long) bufferFrames * rate + sampleRate - 1) / sampleRate);
                try {
                    SourceDataLine line = lines.open(format, deviceFrames * ch * 2);
                    return new JavaSoundOutput(line, sampleRate, rate, ch);
                } catch (LineUnavailableException e) {
                    lastProblem = "the audio output device is not available: " + e.getMessage();
                } catch (IllegalArgumentException e) {
                    lastProblem = "no audio output device: " + e.getMessage();
                } catch (SecurityException e) {
                    lastProblem = "access to the audio device is denied: " + e.getMessage();
                }
            }
        }
        throw new IOException("no audio output device plays " + sampleRate + " Hz, " + channels + " channel(s): " + lastProblem);
    }

    /**
     * Returns the sample rate of the data.
     * <p>
     * 데이터의 샘플링 주파수를 반환한다.
     *
     * @return the sample rate in Hz / 샘플링 주파수(Hz)
     */
    public int getSampleRate() {
        return contextRate;
    }

    /**
     * Returns the number of channels of the line.
     * <p>
     * 라인의 채널 수를 반환한다.
     *
     * @return the number of channels / 채널 수
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Returns the sample rate of the line, which differs from {@link #getSampleRate()} when the device did not play the rate of the data.
     * <p>
     * 라인의 샘플링 주파수를 반환한다. 장치가 데이터의 주파수를 재생하지 못했으면 {@link #getSampleRate()} 와 다르다.
     *
     * @return the device rate in Hz / 장치 주파수(Hz)
     */
    public int getDeviceSampleRate() {
        return deviceRate;
    }

    /**
     * Returns the delay that the conversion of the sample rate adds, in frames of the data rate: the converter is a linear phase filter, so
     * the first frames that it can compute need that many frames of input, and everything that is written comes out that much later (0
     * if the device plays the rate of the data). For 44100 Hz to 48000 Hz it is about 64 frames, 1.5 ms.
     * <p>
     * 표본율 변환이 더하는 지연을 데이터 주파수의 프레임으로 반환한다: 변환기는 선형 위상 필터이므로 처음 계산할 수 있는 프레임에 그만큼의 입력 프레임이 필요하고,
     * 쓴 모든 것이 그만큼 늦게 나온다(장치가 데이터의 주파수를 재생하면 0). 44100 Hz 에서 48000 Hz 로는 약 64 프레임, 1.5 ms 이다.
     *
     * @return the delay in frames / 지연(프레임)
     */
    public int getConversionDelayFrames() {
        return resamplers == null ? 0 : (resamplers[0].getTaps() + 1) / 2;
    }

    /**
     * Returns the capacity of the line buffer in frames of the data rate.
     * <p>
     * 라인 버퍼의 용량을 데이터 주파수의 프레임으로 반환한다.
     *
     * @return the capacity in frames / 용량(프레임)
     */
    public int getBufferFrames() {
        return (int) ((long) (bufferBytes / frameBytes) * contextRate / deviceRate);
    }

    /**
     * Converts a float sample to a 16-bit value: rounded to the nearest, clipped to the 16-bit range, and 0 for a NaN.
     * <p>
     * float 샘플을 16비트 값으로 바꾼다: 가장 가까운 값으로 반올림하고 16비트 범위로 자르며 NaN 은 0 이다.
     *
     * @param v the sample / 샘플
     * @return the value from -32768 to 32767 / -32768 ~ 32767 의 값
     */
    static int toPcm16(float v) {
        // NaN 은 무음으로 재생한다.
        if (v != v) {
            return 0;
        }
        int s = (int) Math.floor(v * 32768.0 + 0.5);
        // 16비트 범위를 벗어나면 자른다.
        if (s > 32767) {
            return 32767;
        }
        if (s < -32768) {
            return -32768;
        }
        return s;
    }

    /**
     * Writes planar float audio as 16-bit little-endian interleaved bytes.
     * <p>
     * 평면 float 오디오를 16비트 리틀 엔디언 인터리브 바이트로 쓴다.
     *
     * @param src      the channels / 채널
     * @param frames   the number of frames / 프레임 수
     * @param channels the number of channels / 채널 수
     * @param out      the bytes, at least <code>frames * channels * 2</code> long / 바이트(최소 <code>frames * channels * 2</code> 길이)
     */
    static void encode16(float[][] src, int frames, int channels, byte[] out) {
        int p = 0;
        // 프레임마다 채널 순서대로 두 바이트씩 쓴다.
        for (int i = 0; i < frames; i++) {
            for (int c = 0; c < channels; c++) {
                int s = toPcm16(src[c][i]);
                out[p++] = (byte) s;
                out[p++] = (byte) (s >> 8);
            }
        }
    }

    /**
     * Queues the audio into the line, converting the rate if needed and waiting while the line buffer is full and the line plays.
     * <p>
     * 오디오를 라인에 넣는다. 필요하면 주파수를 변환하고, 라인 버퍼가 가득 차 있고 라인이 재생 중이면 기다린다.
     *
     * @param data   the audio, one array per channel / 오디오(채널마다 하나의 배열)
     * @param frames the number of frames / 프레임 수
     */
    public void write(float[][] data, int frames) {
        if (closed || frames <= 0) {
            return;
        }
        float[][] source = data;
        int n = frames;
        // 장치 주파수가 다르면 채널마다 변환해 같은 수의 프레임을 얻는다.
        if (resamplers != null) {
            int capacity = resamplers[0].maxOutputFrames(frames);
            if (converted == null || converted[0].length < capacity) {
                converted = new float[channels][capacity];
            }
            // 채널마다 변환해 같은 수의 프레임을 얻는다.
            for (int c = 0; c < channels; c++) {
                n = resamplers[c].process(data[c], 0, frames, converted[c], 0);
            }
            source = converted;
        }
        if (n <= 0) {
            return;
        }
        if (pcm.length < n * frameBytes) {
            pcm = new byte[n * frameBytes];
        }
        encode16(source, n, channels, pcm);
        int offset = 0;
        // 라인 버퍼의 빈 자리만큼씩 넘기고, 가득 차면 장치가 재생할 시간만큼 기다린다.
        while (offset < n && !closed) {
            int free;
            try {
                free = line.available() / frameBytes;
            } catch (RuntimeException e) {
                // 다른 스레드가 라인을 닫았으면 더 쓸 수 없다.
                return;
            }
            if (free > 0) {
                int m = Math.min(free, n - offset);
                line.write(pcm, offset * frameBytes, m * frameBytes);
                offset += m;
                continue;
            }
            // 멈춘 채 가득 찼으면 자리를 만들 것이 없으므로 나머지를 버린다.
            if (!started) {
                return;
            }
            long millis = Math.max(1L, Math.min(MAX_SLEEP_MILLIS, (long) Math.ceil(Math.min(n - offset, 256) * 1000.0 / deviceRate)));
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Returns the frames in the line buffer, in frames of the data rate.
     * <p>
     * 라인 버퍼에 있는 프레임 수를 데이터 주파수의 프레임으로 반환한다.
     *
     * @return the queued frames / 큐에 있는 프레임 수
     */
    public long getQueuedFrames() {
        if (closed) {
            return 0;
        }
        int queuedBytes;
        try {
            queuedBytes = bufferBytes - line.available();
        } catch (RuntimeException e) {
            return 0;
        }
        if (queuedBytes <= 0) {
            return 0;
        }
        return (long) (queuedBytes / frameBytes) * contextRate / deviceRate;
    }

    /**
     * Starts the line.
     * <p>
     * 라인을 시작한다.
     */
    public void start() {
        if (!closed) {
            started = true;
            line.start();
        }
    }

    /**
     * Stops the line; the queue stays.
     * <p>
     * 라인을 멈춘다. 큐는 남는다.
     */
    public void stop() {
        if (!closed) {
            started = false;
            line.stop();
        }
    }

    /**
     * Discards the queue and the memory of the converters.
     * <p>
     * 큐와 변환기의 기억을 버린다.
     */
    public void flush() {
        if (closed) {
            return;
        }
        line.flush();
        // 변환기가 있으면 이전 신호의 기억을 지운다.
        if (resamplers != null) {
            for (int c = 0; c < resamplers.length; c++) {
                resamplers[c].reset();
            }
        }
    }

    /**
     * Stops, discards and closes the line.
     * <p>
     * 라인을 멈추고 비우고 닫는다.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        started = false;
        try {
            line.stop();
            line.flush();
        } finally {
            line.close();
        }
    }

    /**
     * Returns the format of the line.
     * <p>
     * 라인의 형식을 반환한다.
     *
     * @return the description / 설명
     */
    public String getDescription() {
        return "Java Sound " + deviceRate + " Hz " + channels + " ch" + (deviceRate != contextRate ? " (converted from " + contextRate + " Hz)" : "");
    }

    /**
     * Returns true: a Java Sound line plays the audio.
     * <p>
     * true 를 반환한다: Java Sound 라인이 오디오를 재생한다.
     *
     * @return true
     */
    public boolean isAudible() {
        return true;
    }
}
