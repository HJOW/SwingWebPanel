package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;

/**
 * Creates the source of a WAV, AIFF or AU file in the form that was asked for (plan item P25-05): the decoders of the family parse the headers
 * once and call these functions at the places where they create a source, so the 16 bit path ({@link RawPcmSource}, {@link AdpcmSource}) and
 * the wide path ({@link RawFloatSource}, {@link AdpcmFloatSource}) accept and reject exactly the same files. The result is a
 * {@link org.duckdns.hjow.swingwebpanel.media.audio.PcmSource} for the 16 bit path and a
 * {@link org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource} for the wide one; the caller casts it to the type it asked for.
 * <p>
 * WAV, AIFF, AU 파일의 소스를 요청한 형태로 만든다(계획 P25-05). 이 계열의 디코더는 헤더를 한 번만 해석하고 소스를 만드는 자리에서 이 함수를 부르므로 16비트 경로
 * ({@link RawPcmSource}, {@link AdpcmSource})와 넓은 경로({@link RawFloatSource}, {@link AdpcmFloatSource})가 정확히 같은 파일을 받아들이고 거부한다. 결과는 16비트 경로에서는
 * {@link org.duckdns.hjow.swingwebpanel.media.audio.PcmSource}, 넓은 경로에서는 {@link org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource} 이며 호출한 쪽이 요청한 형으로 바꾼다.
 */
final class PcmSources {

    /**
     * Private constructor: static functions only.
     * <p>
     * 비공개 생성자: 정적 함수만 있다.
     */
    private PcmSources() {
    }

    /**
     * Creates a source that owns an encoded PCM input. / 인코딩된 PCM 입력을 소유하는 소스를 만든다.
     * @param wide true for float samples / 부동 소수점 표본이면 true
     * @param data the input to own / 소유할 입력
     * @param start first sample position / 첫 표본 위치
     * @param end exclusive end of the samples / 표본 끝 위치. 미포함
     * @param format sample encoding / 표본 인코딩
     * @param channels input channels / 입력 채널 수
     * @param sampleRate frames per second / 초당 프레임 수
     * @param mixer optional stereo mixer / 선택적 스테레오 믹서
     * @return the PCM source of the requested precision / 요청 정밀도의 PCM 소스
     */
    static Object raw(boolean wide, ByteSource data, int start, int end, SampleFormat format, int channels, int sampleRate, ChannelMixer mixer) {
        if (wide) {
            return new RawFloatSource(data, start, end, format, channels, sampleRate);
        }
        return new RawPcmSource(data, start, end, format, channels, sampleRate, mixer);
    }

    /**
     * Creates a source that owns an encoded ADPCM input. / 인코딩된 ADPCM 입력을 소유하는 소스를 만든다.
     * @param wide true for float samples / 부동 소수점 표본이면 true
     * @param data the input to own / 소유할 입력
     * @param start first block position / 첫 블록 위치
     * @param end exclusive end of the blocks / 블록 끝 위치. 미포함
     * @param kind ADPCM algorithm / ADPCM 알고리즘
     * @param channels input channels / 입력 채널 수
     * @param sampleRate frames per second / 초당 프레임 수
     * @param blockAlign bytes per block / 블록당 바이트 수
     * @param samplesPerBlock frames per block / 블록당 프레임 수
     * @param coefs Microsoft ADPCM coefficients or null / Microsoft ADPCM 계수 또는 null
     * @param mixer optional stereo mixer / 선택적 스테레오 믹서
     * @return the PCM source of the requested precision / 요청 정밀도의 PCM 소스
     */
    static Object adpcm(boolean wide, ByteSource data, int start, int end, int kind, int channels, int sampleRate, int blockAlign, int samplesPerBlock,
            int[] coefs, ChannelMixer mixer) {
        AdpcmSource s = new AdpcmSource(data, start, end, kind, channels, sampleRate, blockAlign, samplesPerBlock, coefs, mixer);
        return wide ? new AdpcmFloatSource(s) : s;
    }

    /**
     * Reads one encoded block and reports media input failures. / 인코딩 블록 하나를 읽고 미디어 입력 실패를 보고한다.
     * @param source the encoded input / 인코딩된 입력
     * @param offset first source byte / 원천의 시작 바이트 위치
     * @param buffer destination block / 대상 블록
     * @param count number of input bytes / 입력 바이트 수
     * @throws MediaException when the input cannot be read / 입력을 읽을 수 없으면
     */
    static void readBlock(ByteSource source, long offset, byte[] buffer, int count) throws MediaException {
        try {
            Bytes.readFully(source, offset, buffer, 0, count);
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        }
    }
}
