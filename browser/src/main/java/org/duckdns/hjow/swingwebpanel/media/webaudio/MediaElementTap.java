package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutput;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.decode.Resampler;

/**
 * The connection between the player of a media element (an <code>audio</code> element of the page, see {@code MediaPlayer}) and a {@link MediaElementAudioSourceNode} of an audio graph
 * (plan item P25-04, P25-07): the player of the element plays <em>into</em> this object instead of into the speakers, and the node of the graph pulls what the player produced, one
 * render quantum at a time. The object plays both roles: it is the {@link AudioOutputFactory} of the player (it opens a {@linkplain Pipe pipe}, the "output" that the player writes
 * 16 bit PCM to) and the {@link MediaElementPcmSource} of the node (it converts the PCM of the pipe to floating point at the sample rate of the context and delivers it on request).
 * <ul>
 * <li><strong>The clock of the element is the graph.</strong> The player derives the position of the media from the frames that it wrote minus the frames that are still queued in
 * its output ({@code MediaPlayer} documents it); here the output is the pipe, which the graph drains at the pace of the rendering of the context. So the position, the
 * <code>timeupdate</code> events and the <code>ended</code> event of the element follow the audio that the graph has consumed, and the element stands still while the pipe is not drained: while the
 * context is suspended, or while the element is paused (the player stops the pipe). This is also what a browser does (the output of the element is taken by the graph, and the media
 * stalls when nobody takes it).</li>
 * <li><strong>Volume and muting</strong> are the business of the player: it multiplies the samples by the volume of the element (0 while it is muted) before it writes them, so the
 * <code>volume</code> and <code>muted</code> of the element also apply to the audio that enters the graph (a browser does the same: the volume of the element is applied to the audio that it
 * provides to the node).</li>
 * <li><strong>Sample rate.</strong> If the media was recorded at another rate than the context runs at, the pipe converts it with the streaming {@link Resampler} (one instance per
 * channel; it takes from the ring only as many frames as the quantum needs, but the filter looks about 65 source frames ahead, so the position of the media leads the audio that
 * the graph has delivered by about 1.5 ms, which is not noticeable; the tail of the converter is delivered when the player closes the pipe at the end of the media).</li>
 * <li><strong>Cross-origin media</strong> that the page may not read (fetched without the permission of the server): {@link #setOutputAllowed(boolean) the host says so}; the pipe is
 * then drained at the normal pace but the node receives silence, so the element is not stalled and the page learns nothing of the audio.</li>
 * <li><strong>After the node is released</strong> ({@link #release()}, called when the context is closed): the element stays bound to the node for good (a browser cannot restore the
 * normal output of an element either); the pipe then drains in real time by the wall clock and delivers nothing, so the element goes on playing, silently.</li>
 * </ul>
 * The methods of the player are called by its playback thread, the methods of the source by the rendering thread of the context, and {@link Pipe#getQueuedFrames()} by any thread; a lock that
 * is held only while a few thousand samples are copied makes it safe (the playback thread never holds it while it decodes or waits).
 * <p>
 * 미디어 요소(페이지의 <code>audio</code> 요소. {@code MediaPlayer} 참고)의 재생기와 오디오 그래프의 {@link MediaElementAudioSourceNode} 를 잇는 것(계획 P25-04, P25-07): 요소의 재생기는
 * 스피커 대신 이 객체<em>로</em> 재생하고, 그래프의 노드는 재생기가 만든 것을 render quantum 하나씩 가져간다. 객체는 두 역할을 한다: 재생기의 {@link AudioOutputFactory}(재생기가
 * 16비트 PCM 을 쓰는 "출력"인 {@linkplain Pipe 파이프}를 연다)이며 노드의 {@link MediaElementPcmSource}(파이프의 PCM 을 컨텍스트의 샘플링 주파수의 부동 소수점으로 바꿔 요청에 따라 준다).
 * <ul>
 * <li><strong>요소의 시계는 그래프이다.</strong> 재생기는 쓴 프레임에서 출력에 아직 큐에 있는 프레임을 뺀 값으로 미디어의 위치를 구한다({@code MediaPlayer} 에 적혀 있다). 여기서 출력은
 * 컨텍스트의 렌더링 속도로 그래프가 비우는 파이프이다. 그래서 요소의 위치, <code>timeupdate</code> 이벤트, <code>ended</code> 이벤트는 그래프가 소비한 오디오를 따르고, 파이프를 비우지
 * 않는 동안(컨텍스트가 일시정지된 동안, 또는 요소가 일시정지되어 재생기가 파이프를 멈춘 동안) 요소는 멈춰 있다. 브라우저도 마찬가지이다(요소의 출력을 그래프가 가져가며 아무도
 * 가져가지 않으면 미디어가 멈춘다).</li>
 * <li><strong>음량과 음소거</strong>는 재생기의 일이다: 재생기가 샘플을 쓰기 전에 요소의 음량(음소거이면 0)을 곱한다. 그래서 요소의 <code>volume</code> 과 <code>muted</code> 는 그래프로
 * 들어가는 오디오에도 적용된다(브라우저도 요소가 노드에 주는 오디오에 요소의 음량을 적용한다).</li>
 * <li><strong>샘플링 주파수.</strong> 미디어가 컨텍스트가 도는 것과 다른 주파수로 녹음되었으면 파이프가 스트리밍 {@link Resampler}(채널마다 인스턴스 하나)로 바꾼다. quantum 이 필요로 하는
 * 만큼만 링에서 가져오지만 필터는 원본 프레임을 약 65 개 앞서 보므로, 미디어의 위치는 그래프가 전달한 오디오보다 약 1.5 ms 앞선다(들리지 않는다). 변환기의 꼬리는 미디어가 끝나 재생기가
 * 파이프를 닫을 때 내보낸다.</li>
 * <li><strong>페이지가 읽을 수 없는 교차 오리진 미디어</strong>(서버의 허가 없이 가져온 것): {@link #setOutputAllowed(boolean) 호스트가 알린다}. 파이프는 그래도 보통 속도로
 * 비워지지만 노드는 무음을 받으므로 요소는 멈추지 않고 페이지는 오디오에 대해 아무것도 알 수 없다.</li>
 * <li><strong>노드를 놓은 뒤</strong>({@link #release()}. 컨텍스트가 닫힐 때 부른다): 요소는 영원히 노드에 묶인 채로 남는다(브라우저도 요소의 보통 출력을 되돌릴 수 없다). 파이프는 그때부터
 * 벽시계 속도로 비워지고 아무것도 주지 않으므로 요소는 소리 없이 계속 재생된다.</li>
 * </ul>
 * 재생기의 메서드는 재생 스레드가, 원천의 메서드는 컨텍스트의 렌더링 스레드가, {@link Pipe#getQueuedFrames()} 는 어느 스레드든 부른다. 몇천 개의 표본을 복사하는 동안에만 쥐는 잠금이
 * 안전하게 해 준다(재생 스레드는 디코딩하거나 기다리는 동안에는 쥐지 않는다).
 */
public final class MediaElementTap implements MediaElementPcmSource, AudioOutputFactory {

    /**
     * The scale of a 16 bit sample to the range of -1 to 1 (an exact power of two, so the conversion is exact).
     * <p>
     * 16비트 표본을 -1 ~ 1 범위로 바꾸는 배율(2 의 거듭제곱이므로 변환이 정확하다).
     */
    private static final float SCALE = 1.0f / 32768.0f;

    /**
     * The least number of frames that the pipe can hold, so that the player can write a block of its own at once.
     * <p>
     * 파이프가 담을 수 있는 최소 프레임 수. 재생기가 자기 블록 하나를 한 번에 쓸 수 있게 한다.
     */
    static final int MIN_CAPACITY_FRAMES = 4096;

    /**
     * The seconds of audio that the pipe holds at most (the player decodes ahead by as much).
     * <p>
     * 파이프가 담는 최대 오디오 시간(초). 재생기가 이만큼 앞서 디코딩한다.
     */
    static final double CAPACITY_SECONDS = 0.15;

    /**
     * The number of source frames that the resampling path takes from the pipe at a time.
     * <p>
     * 리샘플링 경로가 한 번에 파이프에서 가져오는 원본 프레임 수.
     */
    private static final int CHUNK_FRAMES = 512;

    /**
     * One output that the player writes to: a ring of 16 bit little endian interleaved PCM frames that the graph drains.
     * <p>
     * 재생기가 쓰는 출력 하나: 그래프가 비우는 16비트 리틀 엔디언 인터리브 PCM 프레임의 링.
     */
    public final class Pipe implements AudioOutput {

        /**
         * The sample rate of the media in Hz.
         * <p>
         * 미디어의 샘플링 주파수(Hz).
         */
        private final int rate;

        /**
         * The number of channels.
         * <p>
         * 채널 수.
         */
        private final int channels;

        /**
         * The bytes of one frame (two per channel).
         * <p>
         * 프레임 하나의 바이트 수(채널마다 둘).
         */
        private final int frameBytes;

        /**
         * The number of frames that the ring holds.
         * <p>
         * 링이 담는 프레임 수.
         */
        private final int capacity;

        /**
         * The ring of PCM bytes (guarded by the lock of the tap).
         * <p>
         * PCM 바이트의 링(탭의 잠금으로 보호).
         */
        private final byte[] ring;

        /**
         * The index of the first queued frame (guarded by the lock).
         * <p>
         * 큐의 첫 프레임 인덱스(잠금으로 보호).
         */
        private int readIndex;

        /**
         * The number of queued frames (guarded by the lock).
         * <p>
         * 큐에 있는 프레임 수(잠금으로 보호).
         */
        private int queued;

        /**
         * True while the player has started the output (guarded by the lock).
         * <p>
         * 재생기가 출력을 시작한 동안 true(잠금으로 보호).
         */
        private boolean started;

        /**
         * True once the player closed the output (guarded by the lock).
         * <p>
         * 재생기가 출력을 닫은 뒤 true(잠금으로 보호).
         */
        private boolean closed;

        /**
         * The number of flushes; the rendering thread resets its converters when it sees a new number (guarded by the lock).
         * <p>
         * 비움의 횟수. 렌더링 스레드는 새 번호를 보면 자기 변환기를 초기화한다(잠금으로 보호).
         */
        private int flushes;

        /**
         * The time of the last virtual drain in nanoseconds (guarded by the lock).
         * <p>
         * 마지막 가상 배출의 시각(나노초. 잠금으로 보호).
         */
        private long drainNanos;

        /**
         * The fraction of a frame that the virtual drain did not account for yet (guarded by the lock).
         * <p>
         * 가상 배출이 아직 반영하지 않은 프레임의 소수 부분(잠금으로 보호).
         */
        private double drainCarry;

        /**
         * The converters, one per channel, or null if the rate of the media equals the rate of the context (rendering thread).
         * <p>
         * 채널마다의 변환기. 미디어의 주파수가 컨텍스트의 주파수와 같으면 null(렌더링 스레드).
         */
        private final Resampler[] converters;

        /**
         * The source samples that were taken from the ring for the converters, one array per channel (rendering thread).
         * <p>
         * 변환기를 위해 링에서 가져온 원본 표본. 채널마다 배열 하나(렌더링 스레드).
         */
        private final float[][] input;

        /**
         * The work array that a converter writes to (rendering thread).
         * <p>
         * 변환기가 쓰는 작업 배열(렌더링 스레드).
         */
        private final float[] scratch;

        /**
         * True if the rate of the media cannot be converted to the rate of the context (the pipe is then drained silently by the wall clock).
         * <p>
         * 미디어의 주파수를 컨텍스트의 주파수로 바꿀 수 없으면 true(그러면 파이프를 벽시계로 소리 없이 비운다).
         */
        private final boolean unconvertible;

        /**
         * The converted samples that were not delivered yet, one array per channel (rendering thread).
         * <p>
         * 아직 전달하지 않은 변환된 표본. 채널마다 배열 하나(렌더링 스레드).
         */
        private float[][] converted;

        /**
         * The number of converted samples that wait (rendering thread).
         * <p>
         * 기다리는 변환된 표본의 수(렌더링 스레드).
         */
        private int convertedCount;

        /**
         * The number of flushes that the rendering thread has seen (rendering thread).
         * <p>
         * 렌더링 스레드가 본 비움의 횟수(렌더링 스레드).
         */
        private int seenFlushes;

        /**
         * True once the tail of the converters was delivered after the close (rendering thread).
         * <p>
         * 닫힌 뒤 변환기의 꼬리를 전달했으면 true(렌더링 스레드).
         */
        private boolean tailDone;

        /**
         * Creates the output of a media.
         * <p>
         * 미디어의 출력을 만든다.
         *
         * @param rate     the sample rate of the media / 미디어의 샘플링 주파수
         * @param channels the number of channels / 채널 수
         */
        Pipe(int rate, int channels) {
            this.rate = rate;
            this.channels = channels;
            this.frameBytes = channels * 2;
            this.capacity = Math.max(MIN_CAPACITY_FRAMES, (int) Math.ceil(rate * CAPACITY_SECONDS));
            this.ring = new byte[capacity * frameBytes];
            this.drainNanos = System.nanoTime();
            Resampler[] made = null;
            float[][] in = null;
            float[] tmp = null;
            boolean impossible = false;
            // 컨텍스트와 주파수가 다르면 채널마다 스트리밍 변환기를 만든다. 만들 수 없는 비율이면 변환 없이 무음 배출로 대신한다.
            if ((double) rate != (double) contextRate) {
                try {
                    made = new Resampler[channels];
                    // 채널마다 변환기를 하나씩 만든다.
                    for (int c = 0; c < channels; c++) {
                        made[c] = new Resampler(rate, contextRate);
                    }
                    in = new float[channels][CHUNK_FRAMES];
                    tmp = new float[made[0].maxOutputFrames(CHUNK_FRAMES) + 8];
                    converted = new float[channels][tmp.length * 2];
                } catch (IllegalArgumentException e) {
                    made = null;
                    in = null;
                    tmp = null;
                    impossible = true;
                }
            }
            this.converters = made;
            this.input = in;
            this.scratch = tmp;
            this.unconvertible = impossible;
        }

        /**
         * Returns the sample rate of the media that this output plays.
         * <p>
         * 이 출력이 재생하는 미디어의 샘플링 주파수를 반환한다.
         *
         * @return the sample rate in Hz / 샘플링 주파수(Hz)
         */
        public int getSampleRate() {
            return rate;
        }

        /**
         * Returns the number of channels of the media.
         * <p>
         * 미디어의 채널 수를 반환한다.
         *
         * @return the number of channels / 채널 수
         */
        public int getChannels() {
            return channels;
        }

        /**
         * Writes as many whole frames as fit into the ring and returns at once; 0 means that the ring is full (the player waits and tries again).
         * <p>
         * 링에 들어가는 만큼의 완전한 프레임을 쓰고 곧바로 돌아온다. 0 은 링이 가득 찼다는 뜻이다(재생기가 기다렸다가 다시 시도한다).
         *
         * @param data   the PCM bytes / PCM 바이트
         * @param offset the start offset / 시작 위치
         * @param length the number of bytes offered / 제안하는 바이트 수
         * @return the number of bytes taken / 받아들인 바이트 수
         */
        public int write(byte[] data, int offset, int length) {
            synchronized (lock) {
                if (closed) {
                    return 0;
                }
                drainVirtually();
                int frames = Math.min(length / frameBytes, capacity - queued);
                if (frames <= 0) {
                    return 0;
                }
                int at = (readIndex + queued) % capacity;
                int first = Math.min(frames, capacity - at);
                System.arraycopy(data, offset, ring, at * frameBytes, first * frameBytes);
                // 링의 끝을 넘는 나머지는 앞에서부터 이어 쓴다.
                if (first < frames) {
                    System.arraycopy(data, offset + first * frameBytes, ring, 0, (frames - first) * frameBytes);
                }
                queued += frames;
                written += frames;
                return frames * frameBytes;
            }
        }

        /**
         * Returns the number of frames that were written and not consumed yet (the player derives the position of the media from it).
         * <p>
         * 쓰였지만 아직 소비되지 않은 프레임 수를 반환한다(재생기가 이것으로 미디어의 위치를 구한다).
         *
         * @return the queued frames / 큐에 있는 프레임 수
         */
        public long getQueuedFrames() {
            synchronized (lock) {
                drainVirtually();
                return queued;
            }
        }

        /**
         * Lets the graph take the queued frames (the element plays).
         * <p>
         * 그래프가 큐의 프레임을 가져가게 한다(요소가 재생한다).
         */
        public void start() {
            synchronized (lock) {
                if (!closed) {
                    drainNanos = System.nanoTime();
                    started = true;
                }
            }
        }

        /**
         * Stops the graph from taking frames (the element is paused); the queued frames stay.
         * <p>
         * 그래프가 프레임을 가져가지 못하게 한다(요소가 일시정지). 큐의 프레임은 남는다.
         */
        public void stop() {
            synchronized (lock) {
                drainVirtually();
                started = false;
            }
        }

        /**
         * Discards the queued frames (a seek).
         * <p>
         * 큐의 프레임을 버린다(탐색).
         */
        public void flush() {
            synchronized (lock) {
                queued = 0;
                readIndex = 0;
                flushes++;
            }
        }

        /**
         * Closes the output: nothing is accepted any more, the queued frames are dropped and the tail of the converters is delivered once.
         * <p>
         * 출력을 닫는다: 더는 받지 않고, 큐의 프레임은 버려지며, 변환기의 꼬리는 한 번 전달한다.
         */
        public void close() {
            synchronized (lock) {
                closed = true;
                started = false;
                queued = 0;
                readIndex = 0;
            }
        }

        /**
         * Removes the frames that a wall clock would have played from a pipe whose node was released (nobody pulls it any more), so that the element goes on.
         * Call it with the lock held.
         * <p>
         * 노드를 놓은 파이프(아무도 더는 가져가지 않는다)에서 벽시계가 재생했을 프레임을 빼서 요소가 계속 진행하게 한다. 잠금을 쥐고 부른다.
         */
        private void drainVirtually() {
            long now = System.nanoTime();
            // 놓은 파이프나 변환할 수 없는 파이프만 벽시계로 비운다. 그래프가 가져가는 파이프는 그래프가 비운다.
            if ((released || unconvertible) && started && queued > 0) {
                double due = (now - drainNanos) * 1e-9 * rate + drainCarry;
                int take = (int) Math.min(queued, Math.floor(due));
                drainCarry = due - Math.floor(due);
                // 못 비운 만큼은 다음에 비운다(링이 비었으면 남은 소수 부분도 버린다).
                if (take > 0) {
                    readIndex = (readIndex + take) % capacity;
                    queued -= take;
                }
                if (queued == 0) {
                    drainCarry = 0;
                }
            } else {
                drainCarry = 0;
            }
            drainNanos = now;
        }

        /**
         * Delivers the next frames to the node as floating point at the rate of the context.
         * <p>
         * 컨텍스트의 샘플링 주파수의 부동 소수점으로 다음 프레임을 노드에 전달한다.
         *
         * @param dest   the arrays to fill, one per channel / 채울 배열. 채널마다 하나
         * @param frames the number of frames that are wanted / 원하는 프레임 수
         * @return the number of frames delivered / 전달한 프레임 수
         */
        int pull(float[][] dest, int frames) {
            // 노드를 놓았거나 변환할 수 없는 파이프는 벽시계로 비워지므로 아무것도 주지 않는다.
            if (released || unconvertible) {
                return 0;
            }
            int n;
            if (converters == null) {
                n = pullDirect(dest, frames);
            } else {
                n = pullConverted(dest, frames);
            }
            // 페이지가 읽을 수 없는 오디오는 같은 속도로 비우되 무음을 전달한다.
            if (!outputAllowed) {
                // 채널마다 앞쪽 n 프레임을 0 으로 만든다.
                for (int c = 0; c < dest.length; c++) {
                    Arrays.fill(dest[c], 0, n, 0f);
                }
            }
            pulledFrames += n;
            return n;
        }

        /**
         * Delivers frames at the same sample rate: converts the 16 bit samples of the ring.
         * <p>
         * 같은 샘플링 주파수로 프레임을 전달한다: 링의 16비트 표본을 바꾼다.
         *
         * @param dest   the arrays to fill / 채울 배열
         * @param frames the number of frames that are wanted / 원하는 프레임 수
         * @return the number of frames delivered / 전달한 프레임 수
         */
        private int pullDirect(float[][] dest, int frames) {
            synchronized (lock) {
                if (!started || closed) {
                    return 0;
                }
                int n = Math.min(frames, queued);
                // 표본마다 링에서 읽어 부동 소수점으로 바꾼다.
                convertFrames(dest, 0, n);
                readIndex = (readIndex + n) % capacity;
                queued -= n;
                return n;
            }
        }

        /**
         * Converts frames of the ring into the arrays, from the first queued frame on (the lock is held; the ring is not changed).
         * <p>
         * 링의 프레임을 큐의 첫 프레임부터 배열로 바꾼다(잠금을 쥔 채. 링은 바꾸지 않는다).
         *
         * @param dest   the arrays, one per channel / 배열. 채널마다 하나
         * @param offset the index of the first element to write / 쓸 첫 요소의 인덱스
         * @param n      the number of frames / 프레임 수
         */
        private void convertFrames(float[][] dest, int offset, int n) {
            int width = Math.min(dest.length, channels);
            int index = readIndex;
            // 프레임마다 채널의 표본을 읽는다.
            for (int i = 0; i < n; i++) {
                int base = index * frameBytes;
                // 프레임의 채널마다 16비트 표본을 읽어 부동 소수점으로 바꾼다.
                for (int c = 0; c < width; c++) {
                    int o = base + c * 2;
                    dest[c][offset + i] = ((short) ((ring[o] & 0xff) | (ring[o + 1] << 8))) * SCALE;
                }
                index++;
                if (index == capacity) {
                    index = 0;
                }
            }
            // 파이프의 채널보다 많은 배열은 무음으로 둔다.
            for (int c = width; c < dest.length; c++) {
                Arrays.fill(dest[c], offset, offset + n, 0f);
            }
        }

        /**
         * Delivers frames after a sample rate conversion: takes source frames as long as the converted ones do not suffice, keeps what is left over for the next call.
         * <p>
         * 샘플링 주파수 변환 뒤의 프레임을 전달한다: 변환된 프레임이 모자라는 동안 원본 프레임을 가져오고 남은 것은 다음 호출을 위해 둔다.
         *
         * @param dest   the arrays to fill / 채울 배열
         * @param frames the number of frames that are wanted / 원하는 프레임 수
         * @return the number of frames delivered / 전달한 프레임 수
         */
        private int pullConverted(float[][] dest, int frames) {
            boolean finished;
            synchronized (lock) {
                finished = closed;
            }
            // 탐색으로 큐를 비웠으면 변환기와 쌓아 둔 변환 결과를 버리고 새로 시작한다.
            discardIfFlushed();
            // 원하는 프레임이 모일 때까지 원본을 가져와 변환한다. 필요한 만큼만 가져와야 링의 프레임이 그래프가 소비한 만큼만 줄어 요소의 위치가 앞서가지 않는다.
            while (convertedCount < frames) {
                int wanted = (int) Math.ceil((frames - convertedCount) * (double) rate / contextRate) + 2;
                int got = takeSource(Math.min(CHUNK_FRAMES, wanted));
                if (got == 0) {
                    break;
                }
                convertSource(got);
            }
            // 가져와 변환하는 사이에 탐색이 큐를 비웠다면 방금 변환한 것은 낡은 소리이므로 전달하지 않는다.
            if (discardIfFlushed()) {
                return 0;
            }
            // 파이프가 닫혔고 원본이 바닥났으면 변환기의 꼬리를 한 번 내보낸다.
            if (finished && convertedCount < frames && !tailDone) {
                tailDone = true;
                // 꼬리를 모두 내보낼 때까지 반복한다.
                for (;;) {
                    int m = 0;
                    // 채널마다 변환기의 꼬리를 받아 덧붙인다.
                    for (int c = 0; c < channels; c++) {
                        m = converters[c].finish(scratch, 0);
                        append(c, m);
                    }
                    if (m == 0) {
                        break;
                    }
                    convertedCount += m;
                }
            }
            int n = Math.min(frames, convertedCount);
            int width = Math.min(dest.length, channels);
            // 변환된 프레임의 앞부분을 전달하고 나머지를 앞으로 당긴다.
            for (int c = 0; c < width; c++) {
                System.arraycopy(converted[c], 0, dest[c], 0, n);
                System.arraycopy(converted[c], n, converted[c], 0, convertedCount - n);
            }
            // 파이프의 채널보다 많은 배열은 무음으로 둔다.
            for (int c = width; c < dest.length; c++) {
                Arrays.fill(dest[c], 0, n, 0f);
            }
            convertedCount -= n;
            return n;
        }

        /**
         * Forgets the converters and the waiting converted samples if the queue was flushed since the last time (a seek): what they hold belongs to the old position.
         * <p>
         * 지난번 이후 큐가 비워졌으면(탐색) 변환기와 기다리는 변환된 표본을 잊는다. 그것들은 이전 위치의 것이다.
         *
         * @return true if something was forgotten / 잊은 것이 있으면 true
         */
        private boolean discardIfFlushed() {
            int seen;
            synchronized (lock) {
                seen = flushes;
            }
            if (seen == seenFlushes) {
                return false;
            }
            seenFlushes = seen;
            convertedCount = 0;
            tailDone = false;
            // 채널마다 변환기를 처음 상태로 되돌린다.
            for (int c = 0; c < channels; c++) {
                converters[c].reset();
            }
            return true;
        }

        /**
         * Takes up to a number of source frames (at most {@link #CHUNK_FRAMES}) from the ring into the input arrays as floating point.
         * <p>
         * 링에서 최대 정해진 수(많아야 {@link #CHUNK_FRAMES})의 원본 프레임을 부동 소수점으로 입력 배열에 가져온다.
         *
         * @param most the largest number of frames to take / 가져올 최대 프레임 수
         * @return the number of frames taken / 가져온 프레임 수
         */
        private int takeSource(int most) {
            synchronized (lock) {
                if (!started || closed || queued == 0) {
                    return 0;
                }
                int n = Math.min(most, queued);
                convertFrames(input, 0, n);
                readIndex = (readIndex + n) % capacity;
                queued -= n;
                return n;
            }
        }

        /**
         * Converts source frames of the input arrays and appends the result to the waiting converted samples.
         * <p>
         * 입력 배열의 원본 프레임을 변환해 기다리는 변환된 표본 뒤에 덧붙인다.
         *
         * @param n the number of source frames / 원본 프레임 수
         */
        private void convertSource(int n) {
            int m = 0;
            // 채널마다 변환하고 결과를 덧붙인다(채널의 결과 수는 같다).
            for (int c = 0; c < channels; c++) {
                m = converters[c].process(input[c], 0, n, scratch, 0);
                append(c, m);
            }
            convertedCount += m;
        }

        /**
         * Appends converted samples of the work array to the waiting ones of a channel, growing the array if needed.
         * <p>
         * 작업 배열의 변환된 표본을 채널의 기다리는 표본 뒤에 덧붙인다. 모자라면 배열을 키운다.
         *
         * @param c the channel / 채널
         * @param m the number of samples / 표본 수
         */
        private void append(int c, int m) {
            if (m <= 0) {
                return;
            }
            // 배열이 모자라면 두 배로 키운다.
            if (convertedCount + m > converted[c].length) {
                converted[c] = Arrays.copyOf(converted[c], Math.max(converted[c].length * 2, convertedCount + m));
            }
            System.arraycopy(scratch, 0, converted[c], convertedCount, m);
        }
    }

    /**
     * The sample rate of the context in Hz.
     * <p>
     * 컨텍스트의 샘플링 주파수(Hz).
     */
    private final float contextRate;

    /**
     * Guards the rings of the pipes.
     * <p>
     * 파이프의 링을 보호한다.
     */
    private final Object lock = new Object();

    /**
     * The pipe that the player writes to now (the last one that was opened).
     * <p>
     * 재생기가 지금 쓰는 파이프(마지막으로 연 것).
     */
    private volatile Pipe current;

    /**
     * The number of channels of the last pipe, which the node keeps reporting after the pipe was closed.
     * <p>
     * 마지막 파이프의 채널 수. 파이프를 닫은 뒤에도 노드에 계속 알린다.
     */
    private volatile int lastChannels;

    /**
     * False if the audio must not reach the page (cross-origin media without the permission of the server).
     * <p>
     * 오디오가 페이지에 닿으면 안 되면 false(서버의 허가 없는 교차 오리진 미디어).
     */
    private volatile boolean outputAllowed = true;

    /**
     * True once the node was released (guarded by the lock when it changes).
     * <p>
     * 노드를 놓은 뒤 true(바뀔 때 잠금으로 보호).
     */
    private volatile boolean released;

    /**
     * The number of frames that were written to the pipes (guarded by the lock).
     * <p>
     * 파이프에 쓴 프레임 수(잠금으로 보호).
     */
    private long written;

    /**
     * The number of frames that were delivered to the node (the rendering thread writes it).
     * <p>
     * 노드에 전달한 프레임 수(렌더링 스레드가 쓴다).
     */
    private volatile long pulledFrames;

    /**
     * The number of pipes that the player has opened.
     * <p>
     * 재생기가 연 파이프의 수.
     */
    private volatile int opened;

    /**
     * Creates the tap of an element for a context.
     * <p>
     * 컨텍스트를 위한 요소의 탭을 만든다.
     *
     * @param contextSampleRate the sample rate of the context in Hz / 컨텍스트의 샘플링 주파수(Hz)
     */
    public MediaElementTap(float contextSampleRate) {
        if (!(contextSampleRate > 0)) {
            throw new IllegalArgumentException("contextSampleRate " + contextSampleRate);
        }
        this.contextRate = contextSampleRate;
    }

    /**
     * Opens the output of the player: a new pipe replaces the previous one, which the player closed or will close.
     * <p>
     * 재생기의 출력을 연다: 새 파이프가 이전 파이프를 대신한다. 이전 파이프는 재생기가 닫았거나 곧 닫는다.
     *
     * @param sampleRate the sample rate of the media / 미디어의 샘플링 주파수
     * @param channels   the number of channels of the media / 미디어의 채널 수
     * @return the pipe / 파이프
     * @throws MediaException <code>ACCESS_FAILED</code> if the format is not one that a graph can take / 그래프가 받을 수 없는 형식이면 <code>ACCESS_FAILED</code>
     */
    public AudioOutput open(int sampleRate, int channels) throws MediaException {
        if (sampleRate <= 0 || channels < 1 || channels > WebAudioLimits.MAX_CHANNELS) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "the audio graph cannot take " + channels + " channels at " + sampleRate + " Hz");
        }
        Pipe p = new Pipe(sampleRate, channels);
        Pipe old = current;
        current = p;
        lastChannels = channels;
        opened++;
        // 이전 파이프가 아직 열려 있으면 닫는다(재생기가 이미 닫았다면 아무 일도 없다).
        if (old != null) {
            old.close();
        }
        return p;
    }

    /**
     * Tells the node how many channels the media has (0 before the player opened its output: nothing to play yet).
     * <p>
     * 노드에 미디어의 채널 수를 알려 준다(재생기가 출력을 열기 전에는 0. 아직 재생할 것이 없다).
     *
     * @return the number of channels / 채널 수
     */
    public int getChannelCount() {
        return lastChannels;
    }

    /**
     * Returns the sample rate of what {@link #pull} delivers: the rate of the context (the pipe converts).
     * <p>
     * {@link #pull} 이 주는 것의 샘플링 주파수를 반환한다: 컨텍스트의 주파수(파이프가 바꾼다).
     *
     * @return the sample rate / 샘플링 주파수
     */
    public float getSampleRate() {
        return contextRate;
    }

    /**
     * Tells that the audio may pass: always true, because the tap itself delivers silence for audio that the page may not read (the node would not pull then, and the element would
     * stall).
     * <p>
     * 오디오가 지나가도 된다고 알린다: 언제나 true 이다. 페이지가 읽으면 안 되는 오디오에는 탭이 직접 무음을 전달하기 때문이다(그렇게 하지 않으면 노드가 가져가지 않아 요소가 멈춘다).
     *
     * @return true / true
     */
    public boolean isOutputAllowed() {
        return true;
    }

    /**
     * Tells the tap whether the audio that the element plays may reach the page: false for media from another origin that was fetched without the permission of the server. The host
     * calls it whenever the element starts to load a resource.
     * <p>
     * 요소가 재생하는 오디오가 페이지에 닿아도 되는지 탭에 알린다: 서버의 허가 없이 가져온 다른 오리진의 미디어이면 false. 호스트는 요소가 리소스를 로드하기 시작할 때마다 부른다.
     *
     * @param allowed true if the audio may reach the page / 오디오가 페이지에 닿아도 되면 true
     */
    public void setOutputAllowed(boolean allowed) {
        this.outputAllowed = allowed;
    }

    /**
     * Tells whether the audio reaches the page.
     * <p>
     * 오디오가 페이지에 닿는지 알려 준다.
     *
     * @return true if it does / 닿으면 true
     */
    public boolean isOutputAllowedForPage() {
        return outputAllowed;
    }

    /**
     * Pulls the next samples of the playing media for the node: from the pipe that the player writes to, at the rate of the context. It returns 0 while the element is paused, the
     * pipe is empty, no pipe is open, or the tap was released.
     * <p>
     * 노드를 위해 재생 중인 미디어의 다음 표본을 가져온다: 재생기가 쓰는 파이프에서 컨텍스트의 주파수로. 요소가 일시정지되었거나 파이프가 비었거나 열린 파이프가 없거나 탭을 놓았으면 0 을
     * 반환한다.
     *
     * @param dest   the arrays to fill, one per channel / 채울 배열. 채널마다 하나
     * @param frames the number of frames that are wanted / 원하는 프레임 수
     * @return the number of frames delivered / 전달한 프레임 수
     */
    public int pull(float[][] dest, int frames) {
        Pipe p = current;
        if (p == null || released) {
            return 0;
        }
        return p.pull(dest, frames);
    }

    /**
     * Releases the node's hold: the pipe is drained in real time from now on and nothing is delivered any more (the element goes on playing silently). Calling it again has no effect.
     * <p>
     * 노드의 붙잡음을 놓는다: 파이프는 이제부터 실시간으로 비워지고 더는 아무것도 전달하지 않는다(요소는 소리 없이 계속 재생된다). 다시 불러도 효과가 없다.
     */
    public void release() {
        synchronized (lock) {
            released = true;
            Pipe p = current;
            // 지금까지 그래프가 비운 시각부터 벽시계로 비우도록 기준을 다시 잡는다.
            if (p != null) {
                p.drainNanos = System.nanoTime();
                p.drainCarry = 0;
            }
        }
    }

    /**
     * Tells whether the node was released.
     * <p>
     * 노드를 놓았는지 알려 준다.
     *
     * @return true after {@link #release()} / {@link #release()} 뒤에는 true
     */
    public boolean isReleased() {
        return released;
    }

    /**
     * Returns the number of frames that were delivered to the node (at the sample rate of the context), for tests and diagnostics.
     * <p>
     * 노드에 전달한 프레임 수(컨텍스트의 샘플링 주파수 기준)를 반환한다. 시험과 진단용이다.
     *
     * @return the number of frames / 프레임 수
     */
    public long getPulledFrames() {
        return pulledFrames;
    }

    /**
     * Returns the number of frames that the player wrote (at the sample rate of the media), for tests and diagnostics.
     * <p>
     * 재생기가 쓴 프레임 수(미디어의 샘플링 주파수 기준)를 반환한다. 시험과 진단용이다.
     *
     * @return the number of frames / 프레임 수
     */
    public long getWrittenFrames() {
        synchronized (lock) {
            return written;
        }
    }

    /**
     * Returns the number of pipes that the player has opened.
     * <p>
     * 재생기가 연 파이프의 수를 반환한다.
     *
     * @return the number of pipes / 파이프 수
     */
    public int getOpenCount() {
        return opened;
    }
}
