package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A stream of decoded audio with the <em>channels and the precision of the file</em>: 32 bit floating point samples in the range -1 to 1
 * (a sample of an N bit integer file is exactly <code>sample / 2^(N-1)</code>, a lossy codec delivers its own float output without the
 * rounding to 16 bits and without the clipping), one array per channel, produced on demand so that a long recording is never decoded
 * into memory as a whole (plan item P25-05, the wide path of the decoders). It is the counterpart of {@link PcmSource}, which delivers 16 bit
 * interleaved PCM with at most two channels (decoders mix more channels down to stereo) for playing; both come from the same decoders and
 * the {@link PcmSource} path is unchanged. Not thread-safe: one thread reads at a time.
 * <p>
 * <b>Channel order.</b> The channels are in the order of the Microsoft <code>WAVEFORMATEXTENSIBLE</code> mask, which is also the native order of
 * FFmpeg and the order that the speaker layouts of the Web Audio API use: front left, front right, front centre, LFE, back left, back right,
 * back centre, side left, side right. The coding order of a format is mapped to it where the format defines the speakers (the order of Vorbis,
 * Opus mapping family 1 and AAC differs: Vorbis puts the centre before the right channel, AAC begins with the centre); where the format does not
 * say (more than 8 channels, unknown configurations) the order of the file is kept. Mono and stereo are never reordered.
 * <p>
 * 파일의 <em>채널과 정밀도</em>를 가진 디코딩된 오디오 스트림: [-1, 1] 범위의 32비트 부동 소수점 표본이며(N비트 정수 파일의 표본은 정확히
 * <code>표본 / 2^(N-1)</code>, 손실 코덱은 16비트 반올림과 클리핑 없이 자기 float 출력을 준다) 채널마다 배열 하나이다. 요청할 때 만들어 내므로 긴
 * 녹음을 통째로 메모리에 디코딩하지 않는다(계획 P25-05, 디코더의 넓은 경로). 재생용인 {@link PcmSource}(16비트 인터리브 PCM, 채널은 최대 둘이며 디코더가
 * 더 많은 채널을 스테레오로 다운믹스)의 짝이다. 둘은 같은 디코더에서 나오며 {@link PcmSource} 경로는 바뀌지 않는다. 스레드 안전하지 않다. 한 번에 한
 * 스레드만 읽는다.
 * <p>
 * <b>채널 순서.</b> 채널은 Microsoft <code>WAVEFORMATEXTENSIBLE</code> 마스크의 순서이며 이는 FFmpeg 의 고유 순서이자 Web Audio API 의 스피커 배치가
 * 쓰는 순서이기도 하다: 앞 왼쪽, 앞 오른쪽, 앞 중앙, LFE, 뒤 왼쪽, 뒤 오른쪽, 뒤 중앙, 옆 왼쪽, 옆 오른쪽. 형식이 스피커를 정의하는 곳에서는 그 형식의 부호화
 * 순서를 이 순서로 바꾼다(Vorbis, Opus 매핑 군 1, AAC 의 순서는 다르다: Vorbis 는 중앙이 오른쪽보다 앞서고 AAC 는 중앙으로 시작한다). 형식이 정하지 않은
 * 곳(채널이 8개보다 많거나 알 수 없는 구성)에서는 파일의 순서를 유지한다. 모노와 스테레오는 바꾸지 않는다.
 */
public interface FloatPcmSource {

    /**
     * Returns the sample rate in frames per second.
     * <p>
     * 샘플링 주파수(초당 프레임 수)를 반환한다.
     *
     * @return sample rate / 샘플링 주파수
     */
    int getSampleRate();

    /**
     * Returns the number of channels of the file (at least 1; the order is described in the class description).
     * <p>
     * 파일의 채널 수(1 이상. 순서는 클래스 설명에 있다)를 반환한다.
     *
     * @return channels / 채널 수
     */
    int getChannels();

    /**
     * Returns the total number of frames or -1 if it is not known in advance.
     * <p>
     * 전체 프레임 수를 반환하며 미리 알 수 없으면 -1 이다.
     *
     * @return frames or -1 / 프레임 수 또는 -1
     */
    long getFrameCount();

    /**
     * Reads decoded frames into one array per channel. If the data turns out to be damaged while decoding, the frames decoded before the damage
     * are still delivered by the reads that precede it, and the failure is reported by the read that follows (and every later one).
     * <p>
     * 디코딩된 프레임을 채널마다 하나의 배열로 읽는다. 디코딩 중 데이터가 손상된 것으로 드러나면 손상 앞까지 디코딩한 프레임은 앞선 읽기가 전달하고 실패는 그
     * 다음 읽기(와 이후의 모든 읽기)가 보고한다.
     *
     * @param planar one array per channel, at least {@link #getChannels()} of them, each with room for <code>frames</code> samples from <code>offset</code> / 채널마다 배열 하나(최소
     *               {@link #getChannels()} 개). 각각 <code>offset</code> 부터 <code>frames</code> 개의 표본을 담을 수 있어야 한다
     * @param offset index of the first sample to write in every array / 모든 배열에서 처음 쓸 표본의 인덱스
     * @param frames maximum number of frames, at least 1 / 최대 프레임 수(1 이상)
     * @return the number of frames written (at least 1), or -1 at the end / 쓴 프레임 수(1 이상). 끝이면 -1
     * @throws MediaException CORRUPT if the data is damaged, CANCELLED if the thread was interrupted or the source was closed / 데이터가 손상되었으면 CORRUPT, 스레드가
     *                        인터럽트되었거나 소스를 닫았으면 CANCELLED
     */
    int read(float[][] planar, int offset, int frames) throws MediaException;

    /**
     * Releases resources held by the decoder (idempotent). After closing, reads fail.
     * <p>
     * 디코더가 쥐고 있는 자원을 해제한다(여러 번 호출해도 안전). 닫은 뒤 읽기는 실패한다.
     */
    void close();
}
