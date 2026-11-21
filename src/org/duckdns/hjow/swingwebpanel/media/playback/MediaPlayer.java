package org.duckdns.hjow.swingwebpanel.media.playback;

import java.lang.ref.WeakReference;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;

/**
 * The player of one audio resource (plan items P18-08, P18-10): it keeps the encoded data of the resource in memory (within the limits of
 * {@link DecodeLimits}), decodes it block by block on its own daemon thread and writes every block to an {@link AudioOutput}. The decoded
 * PCM is never held as a whole, so a long recording costs the memory of its compressed data only. The class knows nothing about the DOM: the
 * media element creates one player per loaded resource, calls {@link #play()}, {@link #pause()}, {@link #seek(double)} and the setters from
 * any thread, reads {@link #getPosition()} when a script asks for <code>currentTime</code>, and receives the facts through a
 * {@link PlaybackListener}.
 * <p>
 * The clock is the sound itself: the position is the source frame after the last frame handed to the output minus the frames that are still
 * queued in the output, so it stands still while paused, follows the real device and needs no timer. Pausing stops the output and
 * seeking flushes it, both before the call returns; the volume is applied to the samples in software (a gain of the 16-bit values), so it
 * is the same on every device and exact in tests. The playback rate may be a quarter to four times (see {@link #setRate(double)} and
 * {@link RateConverter}); the position clock follows it.
 * <p>
 * A player is used once: {@link #load(byte[], String)} is called a single time, and {@link #close()} releases the thread, the decoder and the
 * output (also when the owner is garbage collected: the listener is held weakly, so an abandoned player ends itself). All outputs are closed on
 * the playback thread before it ends, and {@link #close()} waits a short time for that.
 * <p>
 * 오디오 리소스 하나의 재생기(계획 P18-08, P18-10): 리소스의 인코딩된 데이터를 메모리에 두고({@link DecodeLimits} 한도 안에서) 자신의 데몬 스레드에서
 * 블록 단위로 디코딩하여 블록마다 {@link AudioOutput} 에 쓴다. 디코딩된 PCM 을 통째로 두지 않으므로 긴 녹음도 압축된 데이터 크기만큼의 메모리만
 * 든다. 이 클래스는 DOM 을 모른다. 미디어 요소가 로드한 리소스마다 재생기 하나를 만들고, 어느 스레드에서나 {@link #play()}, {@link #pause()},
 * {@link #seek(double)} 과 설정 메서드를 부르며, 스크립트가 <code>currentTime</code> 을 물으면 {@link #getPosition()} 을 읽고, 사실은
 * {@link PlaybackListener} 로 받는다.
 * <p>
 * 시계는 소리 자체이다: 위치는 출력에 넘긴 마지막 프레임 다음의 소스 프레임에서 출력에 아직 큐에 있는 프레임을 뺀 값이므로 일시정지 중에는 멈춰 있고,
 * 실제 장치를 따라가며, 타이머가 필요 없다. 일시정지는 출력을 멈추고 탐색은 출력을 비우며 둘 다 호출이 돌아오기 전에 끝난다. 음량은 샘플에
 * 소프트웨어로(16비트 값에 이득) 적용하므로 장치와 상관없이 같고 시험에서 정확하다. 재생 속도는 4분의 1배에서 4배까지 가능하며
 * ({@link #setRate(double)} 와 {@link RateConverter} 참고) 위치 시계가 이를 따른다.
 * <p>
 * 재생기는 한 번만 쓴다: {@link #load(byte[], String)} 는 한 번 부르고, {@link #close()} 는 스레드, 디코더, 출력을 해제한다(소유자가 가비지 컬렉션되는
 * 경우에도: 리스너를 약하게 잡으므로 버려진 재생기는 스스로 끝난다). 모든 출력은 재생 스레드가 끝나기 전에 그 스레드에서 닫으며
 * {@link #close()} 는 그것을 잠깐 기다린다.
 */
public final class MediaPlayer {

    /**
     * The size of the decoded block in frames (about 46 ms at 44.1 kHz); also the unit of the memory used for decoded sound.
     * <p>
     * 디코딩 블록의 크기(프레임 수; 44.1 kHz 에서 약 46 ms). 디코딩된 소리에 쓰는 메모리의 단위이기도 하다.
     */
    static final int BLOCK_FRAMES = 2048;

    /**
     * How long the playback thread waits for room in the output before it looks again, in milliseconds.
     * <p>
     * 재생 스레드가 출력에 빈 곳이 생기기를 기다렸다가 다시 보는 간격(밀리초).
     */
    private static final long POLL_MILLIS = 4;

    /**
     * The longest time between two {@link PlaybackListener#timeUpdate()} reports while playing, in nanoseconds (200 ms, so at least 4 Hz).
     * <p>
     * 재생 중 {@link PlaybackListener#timeUpdate()} 보고 사이의 최대 간격(나노초; 200 ms 이므로 4 Hz 이상).
     */
    private static final long TIME_UPDATE_NANOS = 200L * 1000 * 1000;

    /**
     * The longest time that {@link #close()} waits for the playback thread, in milliseconds.
     * <p>
     * {@link #close()} 가 재생 스레드를 기다리는 최대 시간(밀리초).
     */
    private static final long CLOSE_WAIT_MILLIS = 2000;

    /**
     * How long an idle (paused or ended) playback thread sleeps before it checks that the owner of the player still exists, in milliseconds.
     * <p>
     * 쉬고 있는(일시정지나 끝난) 재생 스레드가 재생기의 소유자가 아직 있는지 확인하기 전까지 자는 시간(밀리초).
     */
    private static final long IDLE_CHECK_MILLIS = 1000;

    /**
     * The gain of the full volume in fixed point (1.0 is 32768).
     * <p>
     * 최대 음량의 이득(고정 소수점; 1.0 이 32768).
     */
    private static final int UNITY_GAIN = 32768;

    /**
     * The value that a failed read returns inside the playback loop (the failure was reported already).
     * <p>
     * 재생 루프 안에서 실패한 읽기가 반환하는 값(실패는 이미 알렸다).
     */
    private static final int READ_FAILED = -2;

    /** Report kind: loaded. / 보고 종류: 로드됨. */
    private static final int N_LOADED = 0;

    /** Report kind: load failed. / 보고 종류: 로드 실패. */
    private static final int N_LOAD_FAILED = 1;

    /** Report kind: seek finished. / 보고 종류: 탐색 끝. */
    private static final int N_SEEKED = 2;

    /** Report kind: wrapped around. / 보고 종류: 반복으로 되돌아감. */
    private static final int N_LOOPED = 3;

    /** Report kind: ended. / 보고 종류: 끝. */
    private static final int N_ENDED = 4;

    /** Report kind: duration known. / 보고 종류: 길이를 알게 됨. */
    private static final int N_DURATION = 5;

    /** Report kind: time update. / 보고 종류: 시간 갱신. */
    private static final int N_TIME = 6;

    /** Report kind: playback failed. / 보고 종류: 재생 실패. */
    private static final int N_FAILED = 7;

    /** Report kind: the sound began. / 보고 종류: 소리가 시작됨. */
    private static final int N_STARTED = 8;

    /**
     * Opens the outputs; null means that there is no sound device (playing then fails).
     * <p>
     * 출력을 연다. null 이면 소리 장치가 없다는 뜻이며 재생은 실패한다.
     */
    private final AudioOutputFactory outputs;

    /**
     * The receiver of the reports, held weakly.
     * <p>
     * 보고를 받는 쪽(약한 참조).
     */
    private final WeakReference<PlaybackListener> listener;

    /**
     * The limits for the decoders.
     * <p>
     * 디코더에 적용하는 제한.
     */
    private final DecodeLimits limits;

    /**
     * Guards the commands (play, seek, close) and wakes the playback thread.
     * <p>
     * 명령(재생, 탐색, 닫기)을 보호하고 재생 스레드를 깨운다.
     */
    private final Object lock = new Object();

    /**
     * Guards the output reference and the accepted frame count, so that the position is read consistently.
     * <p>
     * 출력 참조와 받아들인 프레임 수를 보호하여 위치를 일관되게 읽게 한다.
     */
    private final Object positionLock = new Object();

    /**
     * The encoded data (until the player ends).
     * <p>
     * 인코딩된 데이터(재생기가 끝날 때까지).
     */
    private byte[] data;

    /**
     * The MIME type that the server declared, only a hint.
     * <p>
     * 서버가 선언한 MIME 타입. 힌트일 뿐이다.
     */
    private String mime;

    /**
     * The playback thread, null before {@link #load(byte[], String)}.
     * <p>
     * 재생 스레드. {@link #load(byte[], String)} 전에는 null.
     */
    private Thread thread;

    /**
     * True once {@link #close()} was called or the owner disappeared; guarded by {@link #lock}.
     * <p>
     * {@link #close()} 가 불렸거나 소유자가 사라졌으면 true. {@link #lock} 으로 보호한다.
     */
    private volatile boolean closed;

    /**
     * True while playback is wanted (play was called and not paused, and the end was not reached); guarded by {@link #lock}.
     * <p>
     * 재생이 필요한 동안 true(play 가 불렸고 일시정지되지 않았으며 끝에 닿지 않음). {@link #lock} 으로 보호한다.
     */
    private boolean wantPlay;

    /**
     * The frame that a seek asked for, -1 if none; written under {@link #lock}.
     * <p>
     * 탐색이 요청한 프레임. 없으면 -1. {@link #lock} 아래에서 쓴다.
     */
    private volatile long seekRequest = -1;

    /**
     * True while the playback thread is asked to reopen the output (see {@link #reopenOutput()}); written under {@link #lock}.
     * <p>
     * 재생 스레드가 출력을 다시 열도록 요청받은 동안 true({@link #reopenOutput()} 참고). {@link #lock} 아래에서 쓴다.
     */
    private boolean reopenRequested;

    /**
     * True once the start of the sound was reported for the current play request; guarded by {@link #lock}.
     * <p>
     * 현재의 재생 요청에 대해 소리의 시작을 이미 알렸으면 true. {@link #lock} 으로 보호한다.
     */
    private boolean startReported;

    /**
     * True while the output is started; guarded by {@link #lock}.
     * <p>
     * 출력이 시작된 동안 true. {@link #lock} 으로 보호한다.
     */
    private boolean outputStarted;

    /**
     * The facts about the media, null until loaded.
     * <p>
     * 미디어에 대한 정보. 로드되기 전에는 null.
     */
    private volatile MediaInfo info;

    /**
     * True if the end of the media was reached and rendered (cleared by a seek).
     * <p>
     * 미디어의 끝에 닿아 재생이 끝났으면 true(탐색하면 지운다).
     */
    private volatile boolean ended;

    /**
     * True to start again from the beginning at the end.
     * <p>
     * 끝에서 처음부터 다시 시작하려면 true.
     */
    private volatile boolean loop;

    /**
     * The volume as a gain in fixed point, 0 to {@link #UNITY_GAIN}.
     * <p>
     * 이득으로 나타낸 음량(고정 소수점; 0 에서 {@link #UNITY_GAIN}).
     */
    private volatile int volumeGain = UNITY_GAIN;

    /**
     * True if the sound is muted.
     * <p>
     * 소리가 꺼져 있으면 true.
     */
    private volatile boolean muted;

    /**
     * The completed ranges of the played time.
     * <p>
     * 재생이 끝난 시간 범위.
     */
    private volatile MediaRanges played = MediaRanges.EMPTY;

    /**
     * The start of the range that is being played now, NaN if the playback is not running.
     * <p>
     * 지금 재생 중인 범위의 시작. 재생 중이 아니면 NaN.
     */
    private volatile double runStart = Double.NaN;

    /**
     * The source frame after the last frame that the output accepted (fractional when the playback rate is not 1.0, because an output
     * frame then stands for as many source frames as the rate); guarded by {@link #positionLock}.
     * <p>
     * 출력이 받아들인 마지막 프레임 다음의 소스 프레임(재생 속도가 1.0 이 아니면 출력 프레임 하나가 속도만큼의 소스 프레임을 뜻하므로 소수를 포함).
     * {@link #positionLock} 으로 보호한다.
     */
    private double acceptedFrames;

    /**
     * The playback rate (1.0 is the normal speed), from {@link RateConverter#MIN_RATE} to {@link RateConverter#MAX_RATE}.
     * <p>
     * 재생 속도(1.0 이 보통 속도). {@link RateConverter#MIN_RATE} 에서 {@link RateConverter#MAX_RATE} 까지.
     */
    private volatile double rate = 1.0;

    /**
     * True to keep the pitch when the rate is not 1.0 (the default), false to change it with the speed.
     * <p>
     * 속도가 1.0 이 아닐 때 음높이를 유지하려면(기본) true, 속도와 함께 바꾸려면 false.
     */
    private volatile boolean preservePitch = true;

    /**
     * How many source frames one output frame of the block that is written now stands for (1.0 without a converter); written by the playback
     * thread, read by the position clock.
     * <p>
     * 지금 쓰고 있는 블록의 출력 프레임 하나가 몇 개의 소스 프레임에 해당하는지(변환기가 없으면 1.0). 재생 스레드가 쓰고 위치 시계가 읽는다.
     */
    private volatile double frameRatio = 1.0;

    /**
     * The speed converter, null while the rate is 1.0 (only the playback thread uses it).
     * <p>
     * 속도 변환기. 속도가 1.0 인 동안은 null(재생 스레드만 쓴다).
     */
    private RateConverter converter;

    /**
     * The buffer that holds the converted sound (only used by the playback thread).
     * <p>
     * 변환된 소리를 담는 버퍼(재생 스레드만 쓴다).
     */
    private byte[] convBlock;

    /**
     * The buffer that {@link #blockOffset} and {@link #blockLength} describe: {@link #block} or {@link #convBlock}.
     * <p>
     * {@link #blockOffset} 과 {@link #blockLength} 가 설명하는 버퍼: {@link #block} 또는 {@link #convBlock}.
     */
    private byte[] pending;

    /**
     * The last position that was reported, in frames; it keeps the position from running backwards by a little when the output
     * reports its queue late; guarded by {@link #positionLock}.
     * <p>
     * 마지막으로 보고한 위치(프레임). 출력이 대기 프레임 수를 늦게 알려 줄 때 위치가 조금 뒤로 가지 않게 한다. {@link #positionLock} 으로 보호한다.
     */
    private double lastFrame;

    /**
     * The open output, null if there is none; guarded by {@link #positionLock} for writes.
     * <p>
     * 열린 출력. 없으면 null. 쓰기는 {@link #positionLock} 으로 보호한다.
     */
    private AudioOutput output;

    /**
     * The decoder (only used by the playback thread).
     * <p>
     * 디코더(재생 스레드만 쓴다).
     */
    private PcmSource source;

    /**
     * The decoded block that is being written (only used by the playback thread).
     * <p>
     * 쓰는 중인 디코딩된 블록(재생 스레드만 쓴다).
     */
    private byte[] block;

    /**
     * The number of valid bytes of the block.
     * <p>
     * 블록의 유효한 바이트 수.
     */
    private int blockLength;

    /**
     * The bytes of the block that were taken by the output.
     * <p>
     * 출력이 받아 간 블록의 바이트 수.
     */
    private int blockOffset;

    /**
     * True if the decoder reached its end.
     * <p>
     * 디코더가 끝에 닿았으면 true.
     */
    private boolean eof;

    /**
     * The time of the last time update report in nanoseconds.
     * <p>
     * 마지막 시간 갱신 보고의 시각(나노초).
     */
    private long lastTimeUpdate;

    /**
     * Creates a player with the default limits.
     * <p>
     * 기본 제한으로 재생기를 만든다.
     *
     * @param outputs  opens the outputs, or null if there is no sound device / 출력을 여는 팩터리. 소리 장치가 없으면 null
     * @param listener receives the reports; held weakly, so the owner keeps it / 보고를 받는 쪽. 약하게 잡으므로 소유자가 유지한다
     */
    public MediaPlayer(AudioOutputFactory outputs, PlaybackListener listener) {
        this(outputs, listener, DecodeLimits.DEFAULT);
    }

    /**
     * Creates a player.
     * <p>
     * 재생기를 만든다.
     *
     * @param outputs  opens the outputs, or null if there is no sound device / 출력을 여는 팩터리. 소리 장치가 없으면 null
     * @param listener receives the reports; held weakly, so the owner keeps it / 보고를 받는 쪽. 약하게 잡으므로 소유자가 유지한다
     * @param limits   the limits for the decoders / 디코더에 적용하는 제한
     */
    public MediaPlayer(AudioOutputFactory outputs, PlaybackListener listener, DecodeLimits limits) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.outputs = outputs;
        this.listener = new WeakReference<PlaybackListener>(listener);
        this.limits = limits == null ? DecodeLimits.DEFAULT : limits;
    }

    /**
     * Starts opening the data on the playback thread; the result arrives as {@link PlaybackListener#loaded(MediaInfo)} or
     * {@link PlaybackListener#loadFailed(MediaException)}. Callable once.
     * <p>
     * 재생 스레드에서 데이터를 열기 시작한다. 결과는 {@link PlaybackListener#loaded(MediaInfo)} 또는
     * {@link PlaybackListener#loadFailed(MediaException)} 로 온다. 한 번만 부를 수 있다.
     *
     * @param encoded the whole encoded resource (kept, not modified) / 인코딩된 리소스 전체(보관하며 수정하지 않는다)
     * @param type    the MIME type the server declared, may be null / 서버가 선언한 MIME 타입. null 가능
     * @throws IllegalStateException if called twice or after close / 두 번 부르거나 닫은 뒤에 부르면
     */
    public void load(byte[] encoded, String type) {
        Thread t;
        synchronized (lock) {
            if (closed || thread != null) {
                throw new IllegalStateException("the player was already loaded or closed");
            }
            this.data = encoded;
            this.mime = type;
            t = new Thread(new Runnable() {
                /**
                 * Runs the playback thread.
                 * <p>
                 * 재생 스레드를 실행한다.
                 */
                public void run() {
                    runThread();
                }
            }, "swingwebpanel-audio");
            t.setDaemon(true);
            thread = t;
        }
        t.start();
    }

    /**
     * Asks to play from the current position (resumes after a pause, starts again from the beginning after the end). The output is opened
     * on the playback thread; if that fails, {@link PlaybackListener#failed(MediaException)} is reported. Returns at once.
     * <p>
     * 현재 위치에서 재생하도록 요청한다(일시정지 뒤에는 재개하고, 끝난 뒤에는 처음부터 다시 시작한다). 출력은 재생 스레드에서 열며 실패하면
     * {@link PlaybackListener#failed(MediaException)} 를 알린다. 곧바로 돌아온다.
     */
    public void play() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            // 끝난 뒤의 재생은 처음부터 다시 시작한다.
            if (info != null && seekRequest < 0 && isEnded()) {
                seekRequest = 0;
                ended = false;
            }
            if (!wantPlay) {
                startReported = false;
            }
            wantPlay = true;
            lock.notifyAll();
        }
    }

    /**
     * Pauses: the output stops before the call returns, so the position stands still. Harmless if not playing.
     * <p>
     * 일시정지한다: 호출이 돌아오기 전에 출력이 멈추므로 위치가 멈춘다. 재생 중이 아니어도 무해하다.
     */
    public void pause() {
        synchronized (lock) {
            wantPlay = false;
            startReported = false;
            stopOutputLocked();
            lock.notifyAll();
        }
    }

    /**
     * Moves the position. The position reads as the new value at once; the sound continues from there as soon as the playback thread flushed the
     * output and moved the decoder, which is reported as {@link PlaybackListener#seeked()}. Ignored before the media is loaded. A position
     * past the end moves to the end, a negative one to the start.
     * <p>
     * 위치를 옮긴다. 위치는 곧바로 새 값으로 읽히고, 재생 스레드가 출력을 비우고 디코더를 옮기는 대로 거기서 소리가 이어지며
     * {@link PlaybackListener#seeked()} 로 알린다. 미디어가 로드되기 전에는 무시한다. 끝을 넘는 위치는 끝으로, 음수는 처음으로 옮긴다.
     *
     * @param seconds the new position in seconds / 새 위치(초)
     */
    public void seek(double seconds) {
        MediaInfo i = info;
        if (i == null || Double.isNaN(seconds)) {
            return;
        }
        long frame = seconds <= 0 ? 0 : (long) Math.min(Math.round(seconds * i.getSampleRate()), Long.MAX_VALUE / 2);
        if (i.getFrameCount() >= 0 && frame > i.getFrameCount()) {
            frame = i.getFrameCount();
        }
        synchronized (lock) {
            if (closed) {
                return;
            }
            seekRequest = frame;
            ended = false;
            lock.notifyAll();
        }
    }

    /**
     * Asks the player to give up the output that is open and to open a new one through its factory at the next write, continuing at the position that is heard now (the sound that
     * was queued in the old output is dropped and decoded again for the new one, so nothing is skipped and nothing is repeated). It is for a factory that decided to serve another
     * destination since the output was opened: the host of the Web Audio API routes the sound of an element into the graph of an audio context when a script connects the
     * element, and a paused element still holds the output that it opened for the speakers. Nothing is reported to the listener (this is not a seek). It has no effect if no output is
     * open (the next one is opened through the factory anyway); it is done by the playback thread, and the call returns at once.
     * <p>
     * 열려 있는 출력을 놓고 다음 쓰기 때 팩터리를 통해 새 출력을 열도록 요청한다. 지금 들리는 위치에서 이어 간다(이전 출력에 큐에 있던 소리는 버리고 새 출력을 위해 다시 디코딩하므로
     * 건너뛰는 것도 반복되는 것도 없다). 출력을 연 뒤 팩터리가 다른 곳을 맡기로 한 경우를 위한 것이다: Web Audio API 의 호스트는 스크립트가 요소를 연결하면 요소의 소리를 오디오 컨텍스트의
     * 그래프로 돌리는데, 일시정지된 요소도 스피커용으로 열어 둔 출력을 아직 쥐고 있다. 리스너에는 아무것도 알리지 않는다(탐색이 아니다). 열린 출력이 없으면 효과가 없다(다음 출력은
     * 어차피 팩터리를 통해 연다). 재생 스레드가 하며 호출은 곧바로 돌아온다.
     */
    public void reopenOutput() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            reopenRequested = true;
            lock.notifyAll();
        }
    }

    /**
     * Sets the loop flag: at the end the playback starts again from the beginning without a gap.
     * <p>
     * 반복 플래그를 정한다: 끝에서 틈 없이 처음부터 다시 재생한다.
     *
     * @param value true to loop / 반복하려면 true
     */
    public void setLoop(boolean value) {
        loop = value;
    }

    /**
     * Sets the volume as a factor between 0 and 1 (values outside are limited). The samples are multiplied in software; sound that is already
     * in the output buffer (about 150 ms) is not changed.
     * <p>
     * 음량을 0 과 1 사이의 배율로 정한다(범위 밖의 값은 제한). 샘플에 소프트웨어로 곱하며 이미 출력 버퍼에 있는 소리(약 150 ms)는 바뀌지 않는다.
     *
     * @param value the volume / 음량
     */
    public void setVolume(double value) {
        double v = Double.isNaN(value) ? 1 : Math.max(0, Math.min(1, value));
        volumeGain = (int) Math.round(v * UNITY_GAIN);
    }

    /**
     * Mutes or unmutes the sound without changing the volume.
     * <p>
     * 음량을 바꾸지 않고 소리를 끄거나 켠다.
     *
     * @param value true to mute / 끄려면 true
     */
    public void setMuted(boolean value) {
        muted = value;
    }

    /**
     * Sets the playback rate: 1.0 is the normal speed, 2.0 twice as fast. From a quarter to four times the speed is supported (see
     * {@link RateConverter}); the pitch stays the same unless {@link #setPreservesPitch(boolean)} says otherwise. It applies to the sound that
     * is decoded from now on (the 150 ms in the output buffer keep their speed), and the position clock follows the speed. A value that is not
     * supported (zero, negative, outside the range, not a number) is refused, returning false and keeping the old rate.
     * <p>
     * 재생 속도를 정한다: 1.0 이 보통 속도이고 2.0 은 두 배 빠르다. 4분의 1배에서 4배까지 지원하며({@link RateConverter} 참고) 음높이는
     * {@link #setPreservesPitch(boolean)} 가 달리 정하지 않는 한 그대로이다. 지금부터 디코딩하는 소리에 적용되고(출력 버퍼에 있는 150 ms 는 속도를
     * 유지한다) 위치 시계는 속도를 따른다. 지원하지 않는 값(0, 음수, 범위 밖, 숫자 아님)은 거절하며 false 를 반환하고 이전 속도를 유지한다.
     *
     * @param value the rate / 속도
     * @return true if the rate is supported and was set / 지원하는 속도라서 정했으면 true
     */
    public boolean setRate(double value) {
        if (!RateConverter.isSupported(value)) {
            return false;
        }
        rate = value;
        return true;
    }

    /**
     * Tells whether a playback rate can be played: from a quarter to four times the normal speed.
     * <p>
     * 재생 속도를 재생할 수 있는지 알려 준다: 보통 속도의 4분의 1배에서 4배까지.
     *
     * @param value the rate / 속도
     * @return true if {@link #setRate(double)} accepts it / {@link #setRate(double)} 가 받아들이면 true
     */
    public static boolean isRateSupported(double value) {
        return RateConverter.isSupported(value);
    }

    /**
     * Returns the playback rate.
     * <p>
     * 재생 속도를 반환한다.
     *
     * @return the rate / 속도
     */
    public double getRate() {
        return rate;
    }

    /**
     * Chooses whether the pitch stays the same when the speed changes (the default) or follows the speed like a record that turns faster.
     * <p>
     * 속도가 바뀔 때 음높이를 그대로 둘지(기본), 아니면 레코드를 빨리 돌릴 때처럼 속도를 따르게 할지 정한다.
     *
     * @param value true to keep the pitch / 음높이를 유지하려면 true
     */
    public void setPreservesPitch(boolean value) {
        preservePitch = value;
    }

    /**
     * Returns the current playback position in seconds: the new position right after a seek, the end after the end was reached, 0 before
     * the media is loaded.
     * <p>
     * 현재 재생 위치(초)를 반환한다: 탐색 직후에는 새 위치, 끝에 닿은 뒤에는 끝, 미디어가 로드되기 전에는 0.
     *
     * @return the position / 위치
     */
    public double getPosition() {
        MediaInfo i = info;
        if (i == null) {
            return 0;
        }
        long pending = seekRequest;
        if (pending >= 0) {
            return (double) pending / i.getSampleRate();
        }
        return currentFrame(i) / i.getSampleRate();
    }

    /**
     * Returns the duration in seconds: NaN before the media is loaded, positive infinity while the length is not known.
     * <p>
     * 길이(초)를 반환한다: 미디어가 로드되기 전에는 NaN, 길이를 모르는 동안은 양의 무한대.
     *
     * @return the duration / 길이
     */
    public double getDuration() {
        MediaInfo i = info;
        return i == null ? Double.NaN : i.getDuration();
    }

    /**
     * Returns the facts about the media, null until the media is loaded.
     * <p>
     * 미디어에 대한 정보를 반환한다. 로드되기 전에는 null.
     *
     * @return the facts or null / 정보 또는 null
     */
    public MediaInfo getInfo() {
        return info;
    }

    /**
     * Tells whether the playback reached the end and rendered the last sound, or the position was moved to the end of a known length.
     * <p>
     * 재생이 끝에 닿아 마지막 소리까지 재생되었는지, 또는 위치가 알려진 길이의 끝으로 옮겨졌는지 알려 준다.
     *
     * @return true at the end / 끝에 있으면 true
     */
    public boolean isEnded() {
        if (ended) {
            return true;
        }
        MediaInfo i = info;
        return i != null && i.getFrameCount() >= 0 && seekRequest < 0 && currentFrame(i) >= i.getFrameCount();
    }

    /**
     * Tells whether the player was closed (by {@link #close()}, because its owner disappeared, or because its thread ended).
     * <p>
     * 재생기가 닫혔는지(<code>{@link #close()}</code>, 소유자가 사라짐, 또는 스레드가 끝남) 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Tells whether playback is wanted right now (play was called, no pause, the end not reached).
     * <p>
     * 지금 재생이 필요한지(play 가 불렸고 일시정지도 끝도 아닌지) 알려 준다.
     *
     * @return true while playing / 재생 중이면 true
     */
    public boolean isPlaying() {
        synchronized (lock) {
            return wantPlay && !closed;
        }
    }

    /**
     * Returns the ranges that were played: a snapshot including the part that is playing now.
     * <p>
     * 재생한 범위를 반환한다: 지금 재생 중인 부분을 포함한 스냅샷.
     *
     * @return the ranges / 범위
     */
    public MediaRanges getPlayed() {
        MediaRanges p = played;
        double start = runStart;
        MediaInfo i = info;
        // 재생 중이면 지금 위치까지의 범위를 더한다.
        if (!Double.isNaN(start) && i != null) {
            p = p.add(start, livePosition());
        }
        return p;
    }

    /**
     * Returns the time ranges that can be sought to: the whole duration for a seekable source of known length, nothing otherwise.
     * <p>
     * 탐색할 수 있는 시간 범위를 반환한다: 길이를 아는 탐색 가능한 소스는 전체 길이, 그 밖에는 없음.
     *
     * @return the ranges / 범위
     */
    public MediaRanges getSeekable() {
        MediaInfo i = info;
        if (i == null || !i.isSeekable() || i.getFrameCount() < 0) {
            return MediaRanges.EMPTY;
        }
        return MediaRanges.of(0, i.getDuration());
    }

    /**
     * Stops everything: the playback thread, the decoder and the output. Safe to call repeatedly and from any thread; after it returned (the wait
     * for the thread is limited to two seconds) nothing is reported any more and the output is closed.
     * <p>
     * 모든 것을 멈춘다: 재생 스레드, 디코더, 출력. 여러 번, 어느 스레드에서나 안전하게 부를 수 있다. 돌아온 뒤에는(스레드를 기다리는 시간은 2초로
     * 제한) 더 이상 아무것도 알리지 않으며 출력은 닫혀 있다.
     */
    public void close() {
        Thread t;
        synchronized (lock) {
            closed = true;
            wantPlay = false;
            t = thread;
            lock.notifyAll();
        }
        if (t == null || t == Thread.currentThread()) {
            return;
        }
        // 재생 스레드가 디코딩 중이어도 빨리 멈추도록 인터럽트하고 끝나기를 잠깐 기다린다.
        t.interrupt();
        try {
            t.join(CLOSE_WAIT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The body of the playback thread: opens the data, reports it, and plays on demand until the player is closed.
     * <p>
     * 재생 스레드의 본문: 데이터를 열고 알린 뒤 재생기가 닫힐 때까지 요청에 따라 재생한다.
     */
    private void runThread() {
        try {
            PcmSource opened;
            try {
                opened = AudioDecoders.open(data, mime, limits);
            } catch (MediaException e) {
                if (e.getKind() != MediaException.Kind.CANCELLED) {
                    report(N_LOAD_FAILED, e, 0);
                }
                return;
            }
            source = opened;
            block = new byte[BLOCK_FRAMES * opened.getChannels() * 2];
            pending = block;
            MediaInfo i = new MediaInfo(opened.getSampleRate(), opened.getChannels(), opened.getFrameCount(), opened.isSeekable());
            info = i;
            // 리스너에 대한 지역 변수가 재생 루프 동안 스택에 남지 않도록 알림은 별도 메서드에서 한다.
            if (!report(N_LOADED, i, 0)) {
                return;
            }
            playLoop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            finish();
        }
    }

    /**
     * Releases everything the thread owns when it ends.
     * <p>
     * 스레드가 끝날 때 스레드가 가진 모든 것을 해제한다.
     */
    private void finish() {
        synchronized (lock) {
            closed = true;
            wantPlay = false;
        }
        closeOutput();
        PcmSource s = source;
        source = null;
        if (s != null) {
            s.close();
        }
        data = null;
        block = null;
        pending = null;
        converter = null;
        convBlock = null;
    }

    /**
     * The main loop: wait for work, then seek, or decode one block and hand it to the output.
     * <p>
     * 주 루프: 일을 기다렸다가 탐색하거나, 블록 하나를 디코딩하여 출력에 넘긴다.
     *
     * @throws InterruptedException if the thread is interrupted (the player is being closed) / 스레드가 인터럽트되면(재생기를 닫는 중)
     */
    private void playLoop() throws InterruptedException {
        // 닫힐 때까지 요청(탐색, 재생)을 처리하며 반복한다.
        while (true) {
            long seek;
            boolean play;
            boolean reopen;
            synchronized (lock) {
                if (closed) {
                    return;
                }
                seek = seekRequest;
                play = wantPlay;
                // 대기 중인 탐색이 없을 때만 출력 다시 열기를 처리한다(탐색이 먼저 끝나야 위치가 정해진다).
                reopen = reopenRequested && seek < 0;
                if (reopen) {
                    reopenRequested = false;
                }
            }
            if (seek >= 0) {
                performSeek(seek);
                continue;
            }
            if (reopen) {
                performReopen();
                continue;
            }
            if (!play) {
                // 일시정지 상태: 지금까지 재생한 범위를 닫고 요청이 올 때까지 쉰다.
                endRun();
                synchronized (lock) {
                    // 요청이 없으면 잠깐 쉰다. 깨어날 때마다 소유자가 아직 있는지 확인하므로 버려진 재생기는 스스로 끝난다.
                    if (!closed && seekRequest < 0 && !wantPlay) {
                        lock.wait(IDLE_CHECK_MILLIS);
                    }
                }
                if (target() == null) {
                    return;
                }
                continue;
            }
            if (!ensureOutput()) {
                continue;
            }
            startOutput();
            if (blockOffset >= blockLength) {
                // 다음 블록을 만든다(필요하면 디코딩하고 속도를 변환한다). 모두 끝났으면 끝을 처리한다.
                int filled = fillBlock();
                if (filled < 0) {
                    continue;
                }
                if (filled == 0) {
                    handleEnd();
                    continue;
                }
            }
            int taken = writeBlock();
            if (taken > 0) {
                reportStart();
            }
            noteProgress();
            if (taken == 0) {
                waitForRoom();
            }
        }
    }

    /**
     * Prepares the next block to write: it decodes source blocks and, while the playback rate is not 1.0, passes them through the
     * {@link RateConverter} until the converter has sound to give. The gain of the volume is applied to the result.
     * <p>
     * 쓸 다음 블록을 준비한다: 소스 블록을 디코딩하고, 재생 속도가 1.0 이 아닌 동안에는 {@link RateConverter} 가 줄 소리가 생길 때까지 거기에
     * 통과시킨다. 결과에 음량 이득을 적용한다.
     *
     * @return 1 if a block is ready, 0 if the data and the converter are exhausted, -1 if a failure was reported / 블록이 준비되면 1, 데이터와
     *         변환기가 모두 소진되었으면 0, 실패를 알렸으면 -1
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private int fillBlock() throws InterruptedException {
        // 변환된 소리가 나오거나 데이터가 소진될 때까지 소스 블록을 읽어 넣는다.
        while (true) {
            // 속도가 1.0 이 아니면 변환기를 쓰기 시작한다(블록 경계에서만 시작하므로 쓰던 소리는 끊기지 않는다).
            if (converter == null && rate != 1.0 && !eof) {
                converter = new RateConverter(source.getSampleRate(), source.getChannels());
                convBlock = new byte[block.length * 2];
            }
            RateConverter conv = converter;
            if (conv != null) {
                conv.setRate(rate, preservePitch);
                int m = conv.read(convBlock, 0, convBlock.length);
                if (m > 0) {
                    pending = convBlock;
                    blockLength = m;
                    blockOffset = 0;
                    frameRatio = rate;
                    applyGain(convBlock, m);
                    return 1;
                }
                if (eof) {
                    return 0;
                }
            } else if (eof) {
                return 0;
            }
            int n = readBlock();
            if (n == READ_FAILED) {
                return -1;
            }
            if (n < 0) {
                eof = true;
                if (conv != null) {
                    conv.finish();
                }
                continue;
            }
            if (conv != null) {
                conv.push(block, 0, n);
                continue;
            }
            pending = block;
            blockLength = n;
            blockOffset = 0;
            frameRatio = 1.0;
            applyGain(block, n);
            return 1;
        }
    }

    /**
     * Forgets the sound that the converter holds after a seek or a jump back to the start; the converter is dropped while the rate is 1.0.
     * <p>
     * 탐색이나 처음으로 되돌아간 뒤 변환기가 가진 소리를 잊는다. 속도가 1.0 인 동안에는 변환기를 버린다.
     */
    private void resetConverter() {
        // 속도가 1.0 이면 변환기를 버리고, 아니면 상태만 지워 새 위치에서 다시 시작하게 한다.
        if (converter != null) {
            if (rate == 1.0) {
                converter = null;
                convBlock = null;
            } else {
                converter.reset();
            }
        }
        pending = block;
        frameRatio = 1.0;
    }

    /**
     * Reads the next decoded block; a failure is reported and ends the playback.
     * <p>
     * 다음 디코딩된 블록을 읽는다. 실패는 알리고 재생을 끝낸다.
     *
     * @return the bytes read, -1 at the end of the data, or {@link #READ_FAILED} / 읽은 바이트 수, 데이터의 끝이면 -1, 또는 {@link #READ_FAILED}
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private int readBlock() throws InterruptedException {
        try {
            return source.read(block, 0, block.length);
        } catch (MediaException e) {
            if (e.getKind() == MediaException.Kind.CANCELLED) {
                throw new InterruptedException("decoding was cancelled");
            }
            failPlayback(e);
            return READ_FAILED;
        } catch (RuntimeException e) {
            failPlayback(new MediaException(MediaException.Kind.CORRUPT, "the decoder failed: " + e, e));
            return READ_FAILED;
        }
    }

    /**
     * Handles the end of the data: starts over when looping, otherwise waits until the output rendered the last sound and reports the end.
     * <p>
     * 데이터의 끝을 처리한다: 반복이면 처음부터 다시 시작하고, 아니면 출력이 마지막 소리를 재생할 때까지 기다린 뒤 끝을 알린다.
     *
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private void handleEnd() throws InterruptedException {
        MediaInfo i = info;
        long total;
        synchronized (positionLock) {
            total = Math.round(acceptedFrames);
        }
        // 길이를 미리 몰랐던 소스는 끝에 닿았을 때 비로소 길이를 안다.
        if (i.getFrameCount() < 0) {
            info = i = new MediaInfo(i.getSampleRate(), i.getChannels(), total, i.isSeekable());
            report(N_DURATION, null, i.getDuration());
        }
        // 소리가 없는 미디어(길이 0)는 반복해도 의미가 없으므로 바쁜 반복을 막기 위해 끝으로 처리한다.
        if (loop && total > 0) {
            restart();
            return;
        }
        AudioOutput o = output;
        // 마지막 소리가 재생될 때까지(출력 큐가 빌 때까지) 기다린다.
        if (o != null && o.getQueuedFrames() > 0) {
            waitForRoom();
            return;
        }
        endRun();
        synchronized (lock) {
            if (seekRequest >= 0 || !wantPlay) {
                return;
            }
            wantPlay = false;
            startReported = false;
            ended = true;
        }
        // 변환된 소리의 끝은 소스 프레임 수와 조금 다를 수 있으므로 끝난 위치는 정확히 길이로 맞춘다.
        synchronized (positionLock) {
            if (i.getFrameCount() >= 0) {
                acceptedFrames = i.getFrameCount();
            }
        }
        closeOutput();
        report(N_ENDED, null, 0);
    }

    /**
     * Starts the media again from its beginning for the loop flag, without flushing the output (no gap).
     * <p>
     * 반복 플래그에 따라 출력을 비우지 않고(틈 없이) 미디어를 처음부터 다시 시작한다.
     *
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private void restart() throws InterruptedException {
        try {
            rewindSource();
        } catch (MediaException e) {
            if (e.getKind() == MediaException.Kind.CANCELLED) {
                throw new InterruptedException("decoding was cancelled");
            }
            failPlayback(e);
            return;
        } catch (RuntimeException e) {
            failPlayback(new MediaException(MediaException.Kind.CORRUPT, "the decoder failed: " + e, e));
            return;
        }
        blockLength = 0;
        blockOffset = 0;
        eof = false;
        resetConverter();
        synchronized (positionLock) {
            acceptedFrames = 0;
        }
        // 반복으로 되돌아간 구간은 새 재생 구간이다.
        endRun();
        report(N_LOOPED, null, 0);
    }

    /**
     * Moves the decoder to the beginning, by seeking or, if it cannot seek, by opening the data again.
     * <p>
     * 디코더를 처음으로 옮긴다. 탐색할 수 없으면 데이터를 다시 연다.
     *
     * @throws MediaException if the decoder fails / 디코더가 실패하면
     */
    private void rewindSource() throws MediaException {
        if (source.isSeekable()) {
            source.seekToFrame(0);
        } else {
            source.close();
            source = AudioDecoders.open(data, mime, limits);
        }
    }

    /**
     * Executes a seek on the playback thread: flushes the output, moves the decoder and reports it.
     * <p>
     * 재생 스레드에서 탐색을 실행한다: 출력을 비우고 디코더를 옮기고 알린다.
     *
     * @param frame the requested frame / 요청된 프레임
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private void performSeek(long frame) throws InterruptedException {
        endRun();
        try {
            AudioOutput o = output;
            if (o != null) {
                o.flush();
            }
            // 탐색할 수 있는 소스는 바로 옮기고, 아닌 소스는 처음부터 다시 열어 읽으며 버린다.
            if (source.isSeekable()) {
                source.seekToFrame(frame);
            } else {
                rewindSource();
                skipFrames(frame);
            }
        } catch (MediaException e) {
            if (e.getKind() == MediaException.Kind.CANCELLED) {
                throw new InterruptedException("decoding was cancelled");
            }
            synchronized (lock) {
                if (seekRequest == frame) {
                    seekRequest = -1;
                }
            }
            failPlayback(e);
            return;
        } catch (RuntimeException e) {
            synchronized (lock) {
                if (seekRequest == frame) {
                    seekRequest = -1;
                }
            }
            failPlayback(new MediaException(MediaException.Kind.CORRUPT, "the decoder failed: " + e, e));
            return;
        }
        blockLength = 0;
        blockOffset = 0;
        eof = false;
        resetConverter();
        synchronized (positionLock) {
            acceptedFrames = frame;
            lastFrame = frame;
        }
        boolean superseded;
        synchronized (lock) {
            // 탐색 도중 새 요청이 왔으면 그 요청이 이어서 처리된다.
            superseded = seekRequest != frame;
            if (!superseded) {
                seekRequest = -1;
            }
        }
        if (!superseded) {
            report(N_SEEKED, null, 0);
        }
    }

    /**
     * Gives up the open output and moves the decoder back to the position that is heard now, so that the next write opens a new output through the factory and continues
     * there (see {@link #reopenOutput()}). The sound that was queued in the old output is decoded again; the played ranges and the position do not change.
     * <p>
     * 열린 출력을 놓고 디코더를 지금 들리는 위치로 되돌려, 다음 쓰기가 팩터리를 통해 새 출력을 열고 거기서 이어 가게 한다({@link #reopenOutput()} 참고). 이전 출력에 큐에 있던 소리는
     * 다시 디코딩한다. 재생한 범위와 위치는 바뀌지 않는다.
     *
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private void performReopen() throws InterruptedException {
        AudioOutput old;
        synchronized (positionLock) {
            old = output;
        }
        MediaInfo i = info;
        // 열린 출력이 없으면 다음에 열 때 팩터리가 새로 정하므로 할 일이 없다. 로드 전이어도 마찬가지이다.
        if (old == null || source == null || i == null) {
            return;
        }
        // 지금 들리는 프레임(쓴 프레임에서 큐에 남은 프레임을 뺀 값)에서 이어 간다.
        long frame = Math.round(currentFrame(i));
        endRun();
        closeOutput();
        try {
            // 탐색할 수 있는 소스는 바로 옮기고, 아닌 소스는 처음부터 다시 열어 읽으며 버린다.
            if (source.isSeekable()) {
                source.seekToFrame(frame);
            } else {
                rewindSource();
                skipFrames(frame);
            }
        } catch (MediaException e) {
            if (e.getKind() == MediaException.Kind.CANCELLED) {
                throw new InterruptedException("decoding was cancelled");
            }
            failPlayback(e);
            return;
        } catch (RuntimeException e) {
            failPlayback(new MediaException(MediaException.Kind.CORRUPT, "the decoder failed: " + e, e));
            return;
        }
        blockLength = 0;
        blockOffset = 0;
        eof = false;
        resetConverter();
        synchronized (positionLock) {
            acceptedFrames = frame;
            lastFrame = frame;
        }
    }

    /**
     * Reads and discards frames from the start of a source that cannot seek.
     * <p>
     * 탐색할 수 없는 소스의 처음에서부터 프레임을 읽어 버린다.
     *
     * @param frames the number of frames to skip / 건너뛸 프레임 수
     * @throws MediaException if the decoder fails / 디코더가 실패하면
     */
    private void skipFrames(long frames) throws MediaException {
        int frameBytes = source.getChannels() * 2;
        long left = frames;
        // 건너뛸 프레임을 다 읽을 때까지 블록 단위로 읽어 버린다.
        while (left > 0 && !closed) {
            int n = source.read(block, 0, (int) Math.min(block.length, left * frameBytes));
            if (n <= 0) {
                return;
            }
            left -= n / frameBytes;
        }
    }

    /**
     * Opens the output if there is none; a failure ends the playback and is reported.
     * <p>
     * 출력이 없으면 연다. 실패하면 재생을 끝내고 알린다.
     *
     * @return true if an output is open / 출력이 열려 있으면 true
     */
    private boolean ensureOutput() {
        if (output != null) {
            return true;
        }
        MediaInfo i = info;
        try {
            if (outputs == null) {
                throw new MediaException(MediaException.Kind.ACCESS_FAILED, "this component has no audio output");
            }
            AudioOutput opened = outputs.open(i.getSampleRate(), i.getChannels());
            if (opened == null) {
                throw new MediaException(MediaException.Kind.ACCESS_FAILED, "the audio output could not be opened");
            }
            synchronized (positionLock) {
                output = opened;
            }
            return true;
        } catch (MediaException e) {
            failPlayback(e);
            return false;
        } catch (RuntimeException e) {
            failPlayback(new MediaException(MediaException.Kind.ACCESS_FAILED, "the audio output failed: " + e, e));
            return false;
        }
    }

    /**
     * Starts the output unless it runs already or playback was paused in the meantime.
     * <p>
     * 출력이 이미 동작 중이거나 그 사이 일시정지되었으면 빼고 출력을 시작한다.
     */
    private void startOutput() {
        synchronized (lock) {
            AudioOutput o = output;
            if (wantPlay && !outputStarted && o != null && !closed) {
                o.start();
                outputStarted = true;
            }
        }
    }

    /**
     * Stops the output if it runs; the caller holds {@link #lock}.
     * <p>
     * 출력이 동작 중이면 멈춘다. 호출자가 {@link #lock} 을 쥐고 있다.
     */
    private void stopOutputLocked() {
        AudioOutput o = output;
        if (outputStarted && o != null) {
            outputStarted = false;
            o.stop();
        }
    }

    /**
     * Offers the rest of the block to the output.
     * <p>
     * 블록의 나머지를 출력에 제안한다.
     *
     * @return the bytes that the output took / 출력이 받아 간 바이트 수
     */
    private int writeBlock() {
        int taken;
        synchronized (positionLock) {
            AudioOutput o = output;
            if (o == null) {
                return 0;
            }
            taken = o.write(pending, blockOffset, blockLength - blockOffset);
            if (taken > 0) {
                blockOffset += taken;
                // 출력 프레임 하나는 속도만큼의 소스 프레임에 해당한다.
                acceptedFrames += (taken / (source.getChannels() * 2)) * frameRatio;
            }
        }
        return taken;
    }

    /**
     * Reports that the sound began, once per play request.
     * <p>
     * 재생 요청마다 한 번, 소리가 시작되었음을 알린다.
     */
    private void reportStart() {
        synchronized (lock) {
            if (startReported || !wantPlay) {
                return;
            }
            startReported = true;
        }
        report(N_STARTED, null, 0);
    }

    /**
     * Updates the played range and reports the time at least four times a second.
     * <p>
     * 재생한 범위를 갱신하고 초당 4번 이상 시간을 알린다.
     */
    private void noteProgress() {
        if (Double.isNaN(runStart)) {
            runStart = livePosition();
        }
        long now = System.nanoTime();
        // 마지막 보고 뒤 일정 시간이 지났으면 위치가 움직였음을 알린다.
        if (now - lastTimeUpdate >= TIME_UPDATE_NANOS) {
            lastTimeUpdate = now;
            report(N_TIME, null, 0);
        }
    }

    /**
     * Closes the current range of played time (the playback stopped or jumped).
     * <p>
     * 지금까지 재생한 시간 범위를 닫는다(재생이 멈췄거나 건너뛰었다).
     */
    private void endRun() {
        double start = runStart;
        if (!Double.isNaN(start)) {
            played = played.add(start, livePosition());
            runStart = Double.NaN;
        }
    }

    /**
     * Returns the position of the sound that is rendered now, ignoring a seek that was asked for but not executed yet.
     * <p>
     * 지금 재생 중인 소리의 위치를 반환한다. 요청했지만 아직 실행하지 않은 탐색은 무시한다.
     *
     * @return the position in seconds / 위치(초)
     */
    private double livePosition() {
        MediaInfo i = info;
        return i == null ? 0 : currentFrame(i) / i.getSampleRate();
    }

    /**
     * Waits a little for room in the output or for a command.
     * <p>
     * 출력에 빈 곳이 생기거나 명령이 올 때까지 잠깐 기다린다.
     *
     * @throws InterruptedException if the player is being closed / 재생기를 닫는 중이면
     */
    private void waitForRoom() throws InterruptedException {
        synchronized (lock) {
            if (!closed && seekRequest < 0 && wantPlay) {
                lock.wait(POLL_MILLIS);
            }
        }
    }

    /**
     * Ends the playback because of an error: stops and closes the output, reports the error and waits for the next command.
     * <p>
     * 오류 때문에 재생을 끝낸다: 출력을 멈추고 닫으며 오류를 알리고 다음 명령을 기다린다.
     *
     * @param error the reason / 이유
     */
    private void failPlayback(MediaException error) {
        endRun();
        synchronized (lock) {
            wantPlay = false;
            startReported = false;
            outputStarted = false;
        }
        closeOutput();
        blockLength = 0;
        blockOffset = 0;
        resetConverter();
        report(N_FAILED, error, 0);
    }

    /**
     * Closes the output if there is one (on the playback thread; the output is closed exactly once).
     * <p>
     * 출력이 있으면 닫는다(재생 스레드에서; 출력은 정확히 한 번 닫는다).
     */
    private void closeOutput() {
        AudioOutput o;
        synchronized (positionLock) {
            o = output;
            output = null;
        }
        synchronized (lock) {
            outputStarted = false;
        }
        if (o != null) {
            try {
                o.close();
            } catch (RuntimeException e) {
                // 출력을 닫다가 난 오류는 더 할 수 있는 일이 없으므로 무시한다.
                return;
            }
        }
    }

    /**
     * Computes the frame that is being rendered now.
     * <p>
     * 지금 재생 중인 프레임을 계산한다.
     *
     * @param i the facts about the media / 미디어 정보
     * @return the frame position (fractional) / 프레임 위치(소수 포함)
     */
    private double currentFrame(MediaInfo i) {
        long total = i.getFrameCount();
        synchronized (positionLock) {
            double frame = acceptedFrames;
            AudioOutput o = output;
            if (o != null) {
                // 대기 중인 출력 프레임은 속도만큼의 소스 프레임에 해당한다.
                frame -= o.getQueuedFrames() * frameRatio;
            }
            // 반복으로 되돌아간 직후에는 이전 바퀴의 끝부분 소리가 아직 큐에 있으므로 길이만큼 되감아 센다.
            if (frame < 0) {
                frame = total >= 0 && loop ? frame + total : 0;
            }
            if (total >= 0 && frame > total) {
                frame = total;
            }
            frame = Math.max(0, frame);
            // 장치가 알려 주는 대기 프레임 수는 갱신이 늦을 수 있으므로 위치가 조금 뒤로 가는 것은 막는다. 반복으로 되돌아가는 큰 후퇴는 그대로 둔다.
            double tolerance = total >= 0 ? Math.min(i.getSampleRate() / 2.0, total / 2.0) : i.getSampleRate() / 2.0;
            if (frame < lastFrame && lastFrame - frame < tolerance) {
                frame = lastFrame;
            } else {
                lastFrame = frame;
            }
            return frame;
        }
    }

    /**
     * Scales the 16-bit little-endian samples of a block by the volume.
     * <p>
     * 블록의 16비트 리틀 엔디언 샘플에 음량을 곱한다.
     *
     * @param b      the block / 블록
     * @param length the valid bytes / 유효한 바이트 수
     */
    private void applyGain(byte[] b, int length) {
        int gain = muted ? 0 : volumeGain;
        if (gain == UNITY_GAIN) {
            return;
        }
        // 샘플마다 고정 소수점 이득을 곱해 다시 쓴다.
        for (int p = 0; p + 1 < length; p += 2) {
            int sample = (short) ((b[p] & 0xff) | (b[p + 1] << 8));
            int scaled = (sample * gain) >> 15;
            b[p] = (byte) scaled;
            b[p + 1] = (byte) (scaled >> 8);
        }
    }

    /**
     * Returns the listener, or null if the player was closed or the owner disappeared (the player then closes itself).
     * <p>
     * 리스너를 반환한다. 재생기가 닫혔거나 소유자가 사라졌으면 null 이며 그 경우 재생기는 스스로 닫힌다.
     *
     * @return the listener or null / 리스너 또는 null
     */
    private PlaybackListener target() {
        if (closed) {
            return null;
        }
        PlaybackListener l = listener.get();
        if (l == null) {
            synchronized (lock) {
                closed = true;
                wantPlay = false;
                lock.notifyAll();
            }
        }
        return l;
    }


    /**
     * Reports one fact to the listener on the playback thread. The reference to the listener lives only inside this call, so a thread that
     * plays for a long time does not keep the owner of the player alive. A failing listener cannot stop the playback.
     * <p>
     * 사실 하나를 재생 스레드에서 리스너에 알린다. 리스너에 대한 참조는 이 호출 안에서만 살아 있으므로, 오래 재생하는 스레드가 재생기의 소유자를
     * 붙잡지 않는다. 리스너가 실패해도 재생은 멈추지 않는다.
     *
     * @param kind   one of the <code>N_</code> constants / <code>N_</code> 상수 중 하나
     * @param arg    the {@link MediaInfo} or {@link MediaException} of the report, or null / 보고의 {@link MediaInfo} 또는 {@link MediaException}. 없으면 null
     * @param number the duration of a duration report / 길이 보고의 길이
     * @return false if nobody is listening any more (the player closes itself) / 듣는 쪽이 더 없으면 false(재생기는 스스로 닫힌다)
     */
    private boolean report(int kind, Object arg, double number) {
        PlaybackListener l = target();
        if (l == null) {
            return false;
        }
        try {
            // 보고의 종류에 맞는 리스너 메서드를 부른다.
            switch (kind) {
            case N_LOADED:
                l.loaded((MediaInfo) arg);
                break;
            case N_LOAD_FAILED:
                l.loadFailed((MediaException) arg);
                break;
            case N_SEEKED:
                l.seeked();
                break;
            case N_LOOPED:
                l.looped();
                break;
            case N_ENDED:
                l.ended();
                break;
            case N_DURATION:
                l.durationKnown(number);
                break;
            case N_TIME:
                l.timeUpdate();
                break;
            case N_STARTED:
                l.started();
                break;
            default:
                l.failed((MediaException) arg);
                break;
            }
        } catch (RuntimeException e) {
            // 리스너의 오류는 재생을 멈추게 하지 않는다.
            return true;
        }
        return true;
    }
}
