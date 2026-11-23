package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A stream of decoded audio: 16-bit signed little-endian interleaved PCM that is produced on demand, so a long recording is never decoded into
 * memory as a whole (plan item P18-08). The playback code reads blocks and writes them to a Java Sound line. Not thread-safe: one
 * thread reads and seeks at a time.
 * <p>
 * 디코딩된 오디오의 스트림: 16비트 부호 있는 리틀 엔디언 인터리브 PCM 을 요청할 때 만들어 내므로 긴 녹음을 통째로 메모리에 디코딩하지 않는다
 * (계획 P18-08). 재생 코드는 블록을 읽어 Java Sound 라인에 쓴다. 스레드 안전하지 않으며 한 번에 한 스레드만 읽고 탐색한다.
 */
public interface PcmSource {

    /**
     * Returns the sample rate in frames per second.
     * <p>
     * 샘플링 주파수(초당 프레임 수)를 반환한다.
     *
     * @return sample rate / 샘플링 주파수
     */
    int getSampleRate();

    /**
     * Returns the number of channels (1 or 2; sources with more channels are mixed down to stereo by the decoder).
     * <p>
     * 채널 수(1 또는 2)를 반환한다. 채널이 더 많은 소스는 디코더가 스테레오로 다운믹스한다.
     *
     * @return channels / 채널 수
     */
    int getChannels();

    /**
     * Returns the total number of frames (one sample per channel) or -1 if it is not known in advance.
     * <p>
     * 전체 프레임 수(채널마다 샘플 하나)를 반환하며 미리 알 수 없으면 -1 이다.
     *
     * @return frames or -1 / 프레임 수 또는 -1
     */
    long getFrameCount();

    /**
     * Reads decoded PCM bytes (16-bit signed little-endian, interleaved). Blocks of whole frames are returned.
     * <p>
     * 디코딩된 PCM 바이트(16비트 부호 있는 리틀 엔디언, 인터리브)를 읽는다. 프레임 단위로 완전한 블록을 반환한다.
     *
     * @param buffer buffer to fill / 채울 버퍼
     * @param offset start offset / 시작 위치
     * @param length maximum number of bytes (at least one frame) / 최대 바이트 수(최소 한 프레임)
     * @return bytes read (a multiple of the frame size), or -1 at the end / 읽은 바이트 수(프레임 크기의 배수). 끝이면 -1
     * @throws MediaException CORRUPT if the data turns out to be damaged while decoding / 디코딩 중 데이터가 손상된 것으로 드러나면 CORRUPT
     */
    int read(byte[] buffer, int offset, int length) throws MediaException;

    /**
     * Tells whether {@link #seekToFrame(long)} is supported.
     * <p>
     * {@link #seekToFrame(long)} 를 지원하는지 알려 준다.
     *
     * @return true if seekable / 탐색할 수 있으면 true
     */
    boolean isSeekable();

    /**
     * Moves to a frame position; the next read starts there (or at the nearest decodable position before it, then skips to it).
     * <p>
     * 프레임 위치로 이동한다. 다음 읽기가 거기서 시작한다(디코딩 가능한 가장 가까운 앞 위치에서 시작해 건너뛴다).
     *
     * @param frame frame index, 0 or more; positions past the end move to the end / 프레임 위치. 0 이상이며 끝을 넘으면 끝으로 이동
     * @throws MediaException if the source is not seekable or the data is damaged / 탐색할 수 없거나 데이터가 손상되었을 때
     */
    void seekToFrame(long frame) throws MediaException;

    /**
     * Releases resources held by the decoder (idempotent). After closing, reads fail.
     * <p>
     * 디코더가 쥐고 있는 자원을 해제한다(여러 번 호출해도 안전). 닫은 뒤 읽기는 실패한다.
     */
    void close();
}
