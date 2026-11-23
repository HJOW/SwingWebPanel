package org.duckdns.hjow.swingwebpanel.media.playback;

/**
 * One open audio output for 16-bit signed little-endian interleaved PCM of a fixed sample rate and channel count (plan item P18-08). The
 * player writes decoded blocks to it and derives the playback position from the number of frames that are queued and not rendered yet.
 * The default implementation is a Java Sound line ({@link JavaSoundOutputFactory}); a host or a test can supply another one (for example a
 * recorder) through {@link AudioOutputFactory}. All methods are called by the playback thread of one player, except {@link #getQueuedFrames()}
 * (called by any thread that asks for the position) and {@link #close()} (the playback thread, at the end); implementations must make these
 * safe. An output is created in the stopped state: written data waits in its buffer until {@link #start()}.
 * <p>
 * 샘플링 주파수와 채널 수가 정해진 16비트 부호 있는 리틀 엔디언 인터리브 PCM 을 위한 열린 오디오 출력 하나(계획 P18-08). 재생기는 디코딩한 블록을
 * 여기에 쓰고, 큐에 있지만 아직 재생되지 않은 프레임 수로 재생 위치를 구한다. 기본 구현은 Java Sound 라인({@link JavaSoundOutputFactory})이며
 * 호스트나 시험은 {@link AudioOutputFactory} 로 다른 출력(예: 기록기)을 줄 수 있다. 모든 메서드는 재생기 하나의 재생 스레드가 부르며, 예외는
 * {@link #getQueuedFrames()}(위치를 묻는 모든 스레드)와 {@link #close()}(끝낼 때 재생 스레드)이고 구현은 이들이 안전하도록 해야 한다. 출력은 멈춘
 * 상태로 만들어지며 쓴 데이터는 {@link #start()} 까지 버퍼에서 기다린다.
 */
public interface AudioOutput {

    /**
     * Writes as many whole frames as fit into the buffer now and returns at once; it never blocks. A return value of 0 means that the buffer
     * is full (the caller waits and tries again).
     * <p>
     * 지금 버퍼에 들어가는 만큼의 완전한 프레임을 쓰고 곧바로 돌아온다. 절대 블록하지 않는다. 반환값 0 은 버퍼가 가득 찼다는 뜻이다(호출자가
     * 기다렸다가 다시 시도한다).
     *
     * @param data   the PCM bytes / PCM 바이트
     * @param offset the start offset / 시작 위치
     * @param length the number of bytes offered (a multiple of the frame size) / 제안하는 바이트 수(프레임 크기의 배수)
     * @return the number of bytes taken (a multiple of the frame size) / 받아들인 바이트 수(프레임 크기의 배수)
     */
    int write(byte[] data, int offset, int length);

    /**
     * Returns the number of frames that were written and not rendered yet (also the frames of a stopped output). It is 0 when the buffer is
     * empty, which is how the player knows that the last sound has been heard. Callable from any thread.
     * <p>
     * 쓰였지만 아직 재생되지 않은 프레임 수를 반환한다(멈춘 출력의 프레임도 포함). 버퍼가 비면 0 이며 재생기는 이것으로 마지막 소리까지 들렸음을
     * 안다. 어느 스레드에서나 부를 수 있다.
     *
     * @return the queued frames / 큐에 있는 프레임 수
     */
    long getQueuedFrames();

    /**
     * Starts or resumes rendering of the queued data.
     * <p>
     * 큐에 있는 데이터의 재생을 시작하거나 재개한다.
     */
    void start();

    /**
     * Pauses rendering; the queued data stays and continues at {@link #start()}.
     * <p>
     * 재생을 멈춘다. 큐에 있는 데이터는 남고 {@link #start()} 에서 이어진다.
     */
    void stop();

    /**
     * Discards the queued data (a seek); the output keeps its started or stopped state.
     * <p>
     * 큐에 있는 데이터를 버린다(탐색). 출력은 시작/정지 상태를 유지한다.
     */
    void flush();

    /**
     * Releases the output; it is not used again. Repeated calls are harmless.
     * <p>
     * 출력을 해제한다. 다시 쓰지 않는다. 여러 번 불러도 무해하다.
     */
    void close();
}
