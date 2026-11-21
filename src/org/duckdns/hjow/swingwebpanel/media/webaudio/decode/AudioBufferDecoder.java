package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.util.concurrent.CancellationException;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioLimits;

/**
 * The decoding pipeline of <code>decodeAudioData</code> (plan item P25-05): encoded audio file data in, an {@link AudioBuffer} at the sample
 * rate of the context out. It reuses the audio decoders of the media layer through their <em>wide path</em>
 * ({@link AudioDecoders#openWide}), which choose the decoder from the <em>signature of the data</em> (the MIME type is only a hint, the specification
 * says the type is determined by sniffing), so every format the media elements play is decoded here too: WAV (PCM, float, mu-law, A-law, ADPCM),
 * AIFF/AIFF-C, AU, FLAC (native and in Ogg), MP3, AAC (ADTS and MP4), Ogg Vorbis, Opus and WebM audio.
 * <p>
 * <b>Channels and samples.</b> The buffer has <em>every channel of the file</em> (up to 32; a 5.1 file gives six channels) with the precision of the file: an N bit
 * integer sample is exactly <code>sample / 2^(N-1)</code>, floating point samples are kept, and a lossy codec delivers its own float output without the
 * rounding to 16 bits and without clipping (so a value can leave the range -1 to 1, as it can in a browser). The channel order is the canonical one of the Web
 * Audio speaker layouts (front left, front right, front centre, LFE, back left, back right, back centre, side left, side right; mono and stereo as they are),
 * which is also the order of FFmpeg and of the <code>WAVEFORMATEXTENSIBLE</code> mask: formats that code the channels in another order (Vorbis, Opus mapping
 * family 1 and AAC) are mapped to it (see {@link FloatPcmSource}). No mix is applied. Samples that are not finite numbers (what a decoder can produce from damaged data) are replaced by silence and reported in the warning.
 * <p>
 * <b>Resampling.</b> If the rate of the file differs from the wanted rate, every channel is converted with a {@link Resampler} while it is
 * decoded (block by block, so the whole file is never held in memory in float form before the conversion). The converted length is
 * <code>ceil(frames * wanted / rate)</code>.
 * <p>
 * <b>Errors and damaged data.</b> Data that cannot be decoded at all is an <code>EncodingError</code> ({@link AudioException.Kind#ENCODING}) with the cause attached
 * (unknown format, a header that is damaged or a format variant that is not supported, a file with no samples, a sample rate that cannot be converted, data over the
 * limits, no memory, more than 32 channels). <b>Data that is damaged or cut off after frames were decoded is not an error</b>: the frames before the damage
 * are the result and {@link DecodeResult#isTruncated()} tells that it is only a part (as in a browser, which plays what it can decode; the Web Audio API 1.1 draft
 * encourages a warning in that case, which {@link DecodeResult#getWarning()} and {@link DecodeWarningCallback} carry). A decoder that fails
 * before it delivered the first frame gives an <code>EncodingError</code>. A cancelled decoding ends with a {@link CancellationException} instead (no error: the result is
 * simply not wanted). Arguments that the caller controls (a null array, a sample rate outside 3000 to 768000 Hz) are errors of the caller.
 * <p>
 * <b>Limits.</b> The input is limited by {@link DecodeLimits} (64 MB by default), the output by {@link WebAudioLimits#MAX_BUFFER_SAMPLES} samples (all channels
 * together), the number of decoded input samples by {@link #MAX_INPUT_SAMPLES}. The class has no state: it can be used from any number of threads at once.
 * <p>
 * <code>decodeAudioData</code> 의 디코딩 파이프라인(계획 P25-05): 인코딩된 오디오 파일 데이터가 들어가고 컨텍스트의 표본율인 {@link AudioBuffer} 가 나온다.
 * 미디어 계층의 오디오 디코더를 <em>넓은 경로</em>({@link AudioDecoders#openWide})로 재사용하며, 그 디코더는 <em>데이터의 시그니처</em>로 디코더를 고른다(MIME 타입은
 * 힌트일 뿐이며 규격도 타입을 스니핑으로 정한다고 한다). 그래서 미디어 요소가 재생하는 모든 형식을 여기서도 디코딩한다: WAV(PCM, 부동 소수점, mu-law, A-law, ADPCM),
 * AIFF/AIFF-C, AU, FLAC(네이티브와 Ogg), MP3, AAC(ADTS와 MP4), Ogg Vorbis, Opus, WebM 오디오.
 * <p>
 * <b>채널과 표본.</b> 버퍼는 <em>파일의 모든 채널</em>(최대 32개. 5.1 파일은 채널 여섯 개)을 파일의 정밀도로 가진다. N비트 정수 표본은 정확히 <code>표본 / 2^(N-1)</code>,
 * 부동 소수점 표본은 그대로이고, 손실 코덱은 16비트 반올림과 클리핑 없이 자기 float 출력을 준다(그래서 값이 브라우저에서처럼 -1 ~ 1 범위를 벗어날 수 있다). 채널 순서는 Web Audio 스피커
 * 배치의 표준 순서(앞 왼쪽, 앞 오른쪽, 앞 중앙, LFE, 뒤 왼쪽, 뒤 오른쪽, 뒤 중앙, 옆 왼쪽, 옆 오른쪽. 모노와 스테레오는 그대로)이며 이는 FFmpeg 와 <code>WAVEFORMATEXTENSIBLE</code>
 * 마스크의 순서이기도 하다. 채널을 다른 순서로 부호화하는 형식(Vorbis, Opus 매핑 군 1, AAC)은 이 순서로 바꾼다({@link FloatPcmSource} 참조). 믹스는 하지 않는다. 유한하지 않은 수의 표본(디코더가 손상된 데이터에서 만들 수 있다)은 무음으로 바꾸고 경고에 알린다.
 * <p>
 * <b>리샘플링.</b> 파일의 표본율이 원하는 표본율과 다르면 디코딩하는 동안 채널마다 {@link Resampler} 로 변환한다(블록 단위이므로 변환 전의 float 형태 전체를
 * 메모리에 두지 않는다). 변환한 길이는 <code>ceil(프레임 수 * 원하는 표본율 / 파일 표본율)</code> 이다.
 * <p>
 * <b>오류와 손상된 데이터.</b> 전혀 디코딩할 수 없는 데이터는 원인을 붙인 <code>EncodingError</code>({@link AudioException.Kind#ENCODING})이다(알 수 없는 형식, 손상된 헤더나
 * 지원하지 않는 형식 변형, 표본이 없는 파일, 변환할 수 없는 표본율, 한도를 넘는 데이터, 메모리 부족, 32개보다 많은 채널). <b>프레임을 디코딩한 뒤에 손상되거나 잘린 데이터는 오류가
 * 아니다</b>: 손상 앞의 프레임이 결과이고 {@link DecodeResult#isTruncated()} 가 일부뿐임을 알려 준다(디코딩할 수 있는 것을 재생하는 브라우저처럼. Web Audio API 1.1 초안은 이때 경고를
 * 권장하며 {@link DecodeResult#getWarning()} 과 {@link DecodeWarningCallback} 이 그것을 전한다). 첫 프레임을 내기 전에 실패한 디코더는 <code>EncodingError</code> 이다. 취소된 디코딩은
 * 대신 {@link CancellationException} 으로 끝난다(오류가 아니다. 결과가 필요하지 않을 뿐이다). 호출한 쪽이 정하는 인수(null 배열, 3000 ~ 768000 Hz 밖의 표본율)는 호출한 쪽의 오류이다.
 * <p>
 * <b>한도.</b> 입력은 {@link DecodeLimits}(기본 64 MB)로, 출력은 {@link WebAudioLimits#MAX_BUFFER_SAMPLES} 표본(모든 채널 합)으로, 디코딩한 입력 표본 수는
 * {@link #MAX_INPUT_SAMPLES} 로 제한한다. 클래스에 상태가 없으므로 여러 스레드에서 동시에 써도 된다.
 */
public final class AudioBufferDecoder {

    /**
     * The number of frames that are decoded and converted at a time.
     * <p>
     * 한 번에 디코딩하고 변환하는 프레임 수.
     */
    public static final int BLOCK_FRAMES = 4096;

    /**
     * The largest number of samples (all channels together) that may be decoded from the file, before the conversion: four times the size
     * limit of a buffer, which lets a file at a high sample rate be reduced to a buffer at the largest allowed size.
     * <p>
     * 변환 전에 파일에서 디코딩할 수 있는 표본 수(모든 채널 합)의 최댓값: 버퍼 크기 한도의 4배. 표본율이 높은 파일을 허용되는 가장 큰 크기의 버퍼로 줄일 수
     * 있게 한다.
     */
    public static final long MAX_INPUT_SAMPLES = 4L * WebAudioLimits.MAX_BUFFER_SAMPLES;

    /**
     * The number of reads in a row that may deliver no frame before the decoder is taken to be stuck.
     * <p>
     * 프레임을 하나도 주지 않는 읽기가 연속으로 몇 번까지 허용되는지(넘으면 디코더가 멈춘 것으로 본다).
     */
    private static final int MAX_EMPTY_READS = 16;

    /**
     * The capacity of a channel array when the length of the file is not known in advance.
     * <p>
     * 파일의 길이를 미리 모를 때 채널 배열의 처음 용량.
     */
    private static final int UNKNOWN_LENGTH_CAPACITY = 1 << 16;

    /**
     * Private constructor: static functions only.
     * <p>
     * 비공개 생성자: 정적 함수만 있다.
     */
    private AudioBufferDecoder() {
    }

    /**
     * Decodes audio file data into a buffer at a sample rate, with the default limits (see {@link #decodeResult} for the information about damaged
     * data that this method does not return).
     * <p>
     * 오디오 파일 데이터를 어떤 표본율의 버퍼로 디코딩한다. 기본 한도를 쓴다(손상된 데이터에 대한 정보는 이 메서드가 돌려주지 않으며 {@link #decodeResult} 를 참조).
     *
     * @param data       the whole encoded file / 인코딩된 파일 전체
     * @param mimeHint   the MIME type of the data or null; only a hint / 데이터의 MIME 타입 또는 null. 힌트일 뿐이다
     * @param sampleRate the sample rate of the result, from 3000 to 768000 Hz / 결과의 표본율(3000 ~ 768000 Hz)
     * @param cancel     a flag that tells that the result is no longer wanted, or null / 결과가 더는 필요하지 않음을 알리는 플래그. null 가능
     * @return the buffer, with the channels of the file and <code>ceil(frames * sampleRate / rateOfTheFile)</code> frames / 파일의 채널을 가지며 프레임 수가
     *         <code>ceil(프레임 수 * sampleRate / 파일의 표본율)</code> 인 버퍼
     * @throws AudioException        <code>EncodingError</code> if the data cannot be decoded; <code>NotSupportedError</code> for a sample
     *                               rate outside the range / 데이터를 디코딩할 수 없으면 <code>EncodingError</code>. 범위 밖의 표본율이면 <code>NotSupportedError</code>
     * @throws CancellationException if the decoding was cancelled / 디코딩이 취소되었으면
     * @throws IllegalArgumentException if <code>data</code> is null / <code>data</code> 가 null 이면
     */
    public static AudioBuffer decode(byte[] data, String mimeHint, float sampleRate, DecodeCancellation cancel) {
        return decodeResult(data, mimeHint, sampleRate, cancel).getBuffer();
    }

    /**
     * Decodes audio file data into a buffer at a sample rate, with explicit limits (used by tests that need small ones).
     * <p>
     * 오디오 파일 데이터를 어떤 표본율의 버퍼로 디코딩한다. 한도를 직접 정한다(작은 한도가 필요한 시험이 쓴다).
     *
     * @param data             the whole encoded file / 인코딩된 파일 전체
     * @param mimeHint         the MIME type of the data or null; only a hint / 데이터의 MIME 타입 또는 null. 힌트일 뿐이다
     * @param sampleRate       the sample rate of the result, from 3000 to 768000 Hz / 결과의 표본율(3000 ~ 768000 Hz)
     * @param cancel           a flag that tells that the result is no longer wanted, or null / 결과가 더는 필요하지 않음을 알리는 플래그. null 가능
     * @param limits           the limits of the media decoders, null for the defaults / 미디어 디코더의 한도. null 이면 기본값
     * @param maxOutputSamples the most samples (all channels together) the buffer may hold, at most
     *                         {@link WebAudioLimits#MAX_BUFFER_SAMPLES} / 버퍼가 담을 수 있는 최대 표본 수(모든 채널 합). {@link WebAudioLimits#MAX_BUFFER_SAMPLES} 이하
     * @return the buffer / 버퍼
     * @throws AudioException        <code>EncodingError</code> if the data cannot be decoded; <code>NotSupportedError</code> for a sample
     *                               rate outside the range / 데이터를 디코딩할 수 없으면 <code>EncodingError</code>. 범위 밖의 표본율이면 <code>NotSupportedError</code>
     * @throws CancellationException if the decoding was cancelled / 디코딩이 취소되었으면
     * @throws IllegalArgumentException if <code>data</code> is null or the limit is not positive / <code>data</code> 가 null 이거나 한도가 양수가 아니면
     */
    public static AudioBuffer decode(byte[] data, String mimeHint, float sampleRate, DecodeCancellation cancel, DecodeLimits limits, long maxOutputSamples) {
        return decodeResult(data, mimeHint, sampleRate, cancel, limits, maxOutputSamples).getBuffer();
    }

    /**
     * Decodes audio file data into a buffer at a sample rate, with the default limits, and tells whether the data was decoded completely.
     * <p>
     * 오디오 파일 데이터를 어떤 표본율의 버퍼로 디코딩하고 데이터를 완전히 디코딩했는지 알려 준다. 기본 한도를 쓴다.
     *
     * @param data       the whole encoded file / 인코딩된 파일 전체
     * @param mimeHint   the MIME type of the data or null; only a hint / 데이터의 MIME 타입 또는 null. 힌트일 뿐이다
     * @param sampleRate the sample rate of the result, from 3000 to 768000 Hz / 결과의 표본율(3000 ~ 768000 Hz)
     * @param cancel     a flag that tells that the result is no longer wanted, or null / 결과가 더는 필요하지 않음을 알리는 플래그. null 가능
     * @return the buffer and the warning for damaged or cut-off data / 버퍼와, 손상되었거나 잘린 데이터에 대한 경고
     * @throws AudioException        <code>EncodingError</code> if the data cannot be decoded; <code>NotSupportedError</code> for a sample
     *                               rate outside the range / 데이터를 디코딩할 수 없으면 <code>EncodingError</code>. 범위 밖의 표본율이면 <code>NotSupportedError</code>
     * @throws CancellationException if the decoding was cancelled / 디코딩이 취소되었으면
     * @throws IllegalArgumentException if <code>data</code> is null / <code>data</code> 가 null 이면
     */
    public static DecodeResult decodeResult(byte[] data, String mimeHint, float sampleRate, DecodeCancellation cancel) {
        return decodeResult(data, mimeHint, sampleRate, cancel, DecodeLimits.DEFAULT, WebAudioLimits.MAX_BUFFER_SAMPLES);
    }

    /**
     * Decodes audio file data into a buffer at a sample rate, with explicit limits, and tells whether the data was decoded completely.
     * <p>
     * 오디오 파일 데이터를 어떤 표본율의 버퍼로 디코딩하고 데이터를 완전히 디코딩했는지 알려 준다. 한도를 직접 정한다.
     *
     * @param data             the whole encoded file / 인코딩된 파일 전체
     * @param mimeHint         the MIME type of the data or null; only a hint / 데이터의 MIME 타입 또는 null. 힌트일 뿐이다
     * @param sampleRate       the sample rate of the result, from 3000 to 768000 Hz / 결과의 표본율(3000 ~ 768000 Hz)
     * @param cancel           a flag that tells that the result is no longer wanted, or null / 결과가 더는 필요하지 않음을 알리는 플래그. null 가능
     * @param limits           the limits of the media decoders, null for the defaults / 미디어 디코더의 한도. null 이면 기본값
     * @param maxOutputSamples the most samples (all channels together) the buffer may hold, at most
     *                         {@link WebAudioLimits#MAX_BUFFER_SAMPLES} / 버퍼가 담을 수 있는 최대 표본 수(모든 채널 합). {@link WebAudioLimits#MAX_BUFFER_SAMPLES} 이하
     * @return the buffer and the warning for damaged or cut-off data / 버퍼와, 손상되었거나 잘린 데이터에 대한 경고
     * @throws AudioException        <code>EncodingError</code> if the data cannot be decoded; <code>NotSupportedError</code> for a sample
     *                               rate outside the range / 데이터를 디코딩할 수 없으면 <code>EncodingError</code>. 범위 밖의 표본율이면 <code>NotSupportedError</code>
     * @throws CancellationException if the decoding was cancelled / 디코딩이 취소되었으면
     * @throws IllegalArgumentException if <code>data</code> is null or the limit is not positive / <code>data</code> 가 null 이거나 한도가 양수가 아니면
     */
    public static DecodeResult decodeResult(byte[] data, String mimeHint, float sampleRate, DecodeCancellation cancel, DecodeLimits limits, long maxOutputSamples) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (maxOutputSamples <= 0) {
            throw new IllegalArgumentException("maxOutputSamples must be positive");
        }
        if (!(sampleRate >= WebAudioLimits.MIN_SAMPLE_RATE && sampleRate <= WebAudioLimits.MAX_SAMPLE_RATE)) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "sampleRate " + sampleRate + " is not in " + WebAudioLimits.MIN_SAMPLE_RATE + ".."
                    + WebAudioLimits.MAX_SAMPLE_RATE);
        }
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        long maxSamples = Math.min(maxOutputSamples, (long) WebAudioLimits.MAX_BUFFER_SAMPLES);
        checkCancelled(cancel);
        FloatPcmSource source;
        try {
            source = AudioDecoders.openWide(data, mimeHint, lim);
        } catch (MediaException e) {
            throw failure(e);
        } catch (RuntimeException e) {
            throw encodingError("the audio data is damaged: " + e, e);
        } catch (OutOfMemoryError e) {
            throw encodingError("not enough memory to decode the audio data", e);
        }
        return decodeSource(source, sampleRate, cancel, maxSamples);
    }

    /**
     * Reads an opened source to its end, converts it and builds the buffer, and closes the source whatever happens. Every failure of the
     * source before its first frame becomes an <code>EncodingError</code> (see the class description); the tests give it sources of their own.
     * <p>
     * 열린 소스를 끝까지 읽어 변환하고 버퍼를 만들며 무슨 일이 있어도 소스를 닫는다. 첫 프레임 전의 소스의 모든 실패는 <code>EncodingError</code> 가 된다(클래스 설명
     * 참조). 시험이 자기 소스를 준다.
     *
     * @param source     the source of float samples; closed by this method / float 표본 소스. 이 메서드가 닫는다
     * @param sampleRate the wanted sample rate, already checked / 원하는 표본율. 이미 검사했다
     * @param cancel     the cancellation flag or null / 취소 플래그 또는 null
     * @param maxSamples the limit of the output in samples of all channels / 출력의 한도(모든 채널의 표본 수)
     * @return the buffer and the warning / 버퍼와 경고
     */
    static DecodeResult decodeSource(FloatPcmSource source, float sampleRate, DecodeCancellation cancel, long maxSamples) {
        try {
            return decodeFrom(source, sampleRate, cancel, maxSamples);
        } catch (MediaException e) {
            throw failure(e);
        } catch (OutOfMemoryError e) {
            throw encodingError("not enough memory to decode the audio data", e);
        } catch (AudioException e) {
            throw e;
        } catch (CancellationException e) {
            throw e;
        } catch (RuntimeException e) {
            // 손상된 데이터 때문에 디코더가 던진 예상 밖의 런타임 오류도 디코딩 실패이다.
            throw encodingError("the audio data is damaged: " + e, e);
        } finally {
            source.close();
        }
    }

    /**
     * Turns a failure of the media layer into the exception to throw: a cancellation for a decoder that ended as cancelled, an
     * <code>EncodingError</code> for every other failure.
     * <p>
     * 미디어 계층의 실패를 던질 예외로 바꾼다: 취소로 끝낸 디코더는 취소, 그 밖의 모든 실패는 <code>EncodingError</code>.
     *
     * @param e the exception of the media layer / 미디어 계층의 예외
     * @return the exception to throw / 던질 예외
     */
    private static RuntimeException failure(MediaException e) {
        // 디코더가 취소로 끝났으면 오류가 아니라 취소이다.
        if (e.getKind() == MediaException.Kind.CANCELLED) {
            return cancelled(e);
        }
        return encodingError(describe(e), e);
    }

    /**
     * Reads the whole source, converts it and builds the buffer.
     * <p>
     * 소스를 끝까지 읽어 변환하고 버퍼를 만든다.
     *
     * @param source     the opened source / 열린 소스
     * @param sampleRate the wanted sample rate / 원하는 표본율
     * @param cancel     the cancellation flag or null / 취소 플래그 또는 null
     * @param maxSamples the limit of the output in samples of all channels / 출력의 한도(모든 채널의 표본 수)
     * @return the buffer and the warning / 버퍼와 경고
     * @throws MediaException if the data is damaged before the first frame / 첫 프레임 전에 데이터가 손상되었으면
     */
    private static DecodeResult decodeFrom(FloatPcmSource source, float sampleRate, DecodeCancellation cancel, long maxSamples) throws MediaException {
        int fileRate = source.getSampleRate();
        int channels = source.getChannels();
        long frames = source.getFrameCount();
        if (fileRate < 1 || channels < 1 || channels > WebAudioLimits.MAX_CHANNELS) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported format: " + channels + " channels at " + fileRate + " Hz");
        }
        Resampler[] converters = null;
        // 파일의 표본율이 원하는 값과 다르면 채널마다 변환기를 만든다.
        if (fileRate != (double) sampleRate) {
            converters = new Resampler[channels];
            try {
                // 채널마다 변환기를 하나씩 만든다.
                for (int c = 0; c < channels; c++) {
                    converters[c] = new Resampler(fileRate, sampleRate);
                }
            } catch (IllegalArgumentException e) {
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "a sample rate of " + fileRate + " Hz cannot be converted to " + sampleRate + " Hz");
            }
        }
        long maxFrames = maxSamples / channels;
        if (maxFrames < 1) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "the limit is smaller than one frame");
        }
        long expected = -1;
        // 길이를 미리 알면 한도를 먼저 확인하고 그만큼의 배열을 한 번에 만든다.
        if (frames >= 0) {
            if (frames > MAX_INPUT_SAMPLES / channels) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "the audio data has " + frames + " frames, more than the limit");
            }
            expected = converters == null ? frames : converters[0].outputLength(frames);
            if (expected > maxFrames) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "the decoded audio would have " + expected + " frames, more than the limit of " + maxFrames);
            }
        }
        ChannelSink[] sinks = new ChannelSink[channels];
        // 채널마다 출력을 모을 싱크를 만든다.
        for (int c = 0; c < channels; c++) {
            sinks[c] = new ChannelSink(expected >= 0 ? (int) expected : UNKNOWN_LENGTH_CAPACITY, (int) maxFrames);
        }
        float[][] chunk = new float[channels][BLOCK_FRAMES];
        float[][] converted = converters == null ? null : new float[channels][converters[0].maxOutputFrames(BLOCK_FRAMES)];
        long inputFrames = 0;
        int emptyReads = 0;
        long nonFinite = 0;
        MediaException damage = null;
        // 블록 단위로 디코딩하고 (필요하면) 변환한 뒤 싱크에 모은다.
        for (;;) {
            checkCancelled(cancel);
            int count;
            try {
                count = source.read(chunk, 0, BLOCK_FRAMES);
            } catch (MediaException e) {
                // 프레임을 낸 뒤에 손상이 드러나면 그 앞까지를 결과로 삼는다(첫 프레임 전이면 디코딩 실패이다).
                if (e.getKind() == MediaException.Kind.CORRUPT && inputFrames > 0) {
                    damage = e;
                    break;
                }
                throw e;
            } catch (RuntimeException e) {
                // 디코더가 검사하지 않은 손상 때문에 생긴 예상 밖의 런타임 오류도 같게 취급한다.
                if (inputFrames > 0) {
                    damage = new MediaException(MediaException.Kind.CORRUPT, "damaged audio data: " + e, e);
                    break;
                }
                throw e;
            }
            if (count < 0) {
                break;
            }
            // 한 프레임도 없는 읽기가 계속되면 디코더가 멈춘 것이다.
            if (count == 0) {
                if (++emptyReads > MAX_EMPTY_READS) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the decoder delivered no samples");
                }
                continue;
            }
            emptyReads = 0;
            inputFrames += count;
            if (inputFrames > MAX_INPUT_SAMPLES / channels) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "the audio data has more than the limit of " + MAX_INPUT_SAMPLES + " samples");
            }
            // 유한하지 않은 표본(손상된 데이터에 대한 디코더의 반응)은 필터와 그래프를 오염시키지 않도록 무음으로 바꾼다.
            for (int c = 0; c < channels; c++) {
                float[] ch = chunk[c];
                // 프레임마다 처리한다.
                for (int i = 0; i < count; i++) {
                    if (ch[i] - ch[i] != 0f) {
                        ch[i] = 0f;
                        nonFinite++;
                    }
                }
            }
            // 채널마다 변환하고(필요하면) 싱크에 덧붙인다.
            for (int c = 0; c < channels; c++) {
                if (converters == null) {
                    sinks[c].append(chunk[c], count);
                } else {
                    int w = converters[c].process(chunk[c], 0, count, converted[c], 0);
                    sinks[c].append(converted[c], w);
                }
            }
        }
        // 변환기의 꼬리를 비워 마지막 출력 표본까지 모은다.
        if (converters != null) {
            float[] tail = new float[converters[0].maxFinishFrames()];
            for (int c = 0; c < channels; c++) {
                int w;
                // 꼬리가 다 나올 때까지 되풀이한다.
                do {
                    checkCancelled(cancel);
                    w = converters[c].finish(tail, 0);
                    sinks[c].append(tail, w);
                } while (w > 0);
            }
        }
        int length = sinks[0].length();
        if (length == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the audio data has no samples");
        }
        float[][] result = new float[channels][];
        // 채널마다 정확한 길이의 배열을 얻는다. 길이는 모두 같아야 한다.
        for (int c = 0; c < channels; c++) {
            if (sinks[c].length() != length) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the channels have different lengths");
            }
            result[c] = sinks[c].finish();
        }
        String warning = null;
        boolean truncated = false;
        if (damage != null) {
            truncated = true;
            warning = "the audio data is damaged after " + inputFrames + " frames; only the part before the damage was decoded (" + damage.getMessage() + ")";
        } else if (frames >= 0 && inputFrames < frames) {
            truncated = true;
            warning = "the audio data is cut off: " + inputFrames + " of the " + frames + " frames that the file announces were decoded";
        }
        // 유한하지 않은 표본을 바꾸었으면 그것도 경고에 덧붙인다.
        if (nonFinite > 0) {
            String extra = nonFinite + " samples that were not finite numbers were replaced by silence";
            warning = warning == null ? "the audio data is damaged: " + extra : warning + "; " + extra;
        }
        return new DecodeResult(AudioBuffer.wrap(result, sampleRate), truncated, warning, damage);
    }

    /**
     * Throws the exception of a cancelled decoding if the flag is set or the thread was interrupted.
     * <p>
     * 플래그가 서 있거나 스레드가 인터럽트되었으면 취소된 디코딩의 예외를 던진다.
     *
     * @param cancel the flag or null / 플래그 또는 null
     */
    private static void checkCancelled(DecodeCancellation cancel) {
        if ((cancel != null && cancel.isCancelled()) || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("the decoding was cancelled");
        }
    }

    /**
     * Builds the exception of a decoding that a decoder ended as cancelled.
     * <p>
     * 디코더가 취소로 끝낸 디코딩의 예외를 만든다.
     *
     * @param cause the exception of the decoder / 디코더의 예외
     * @return the exception to throw / 던질 예외
     */
    private static CancellationException cancelled(MediaException cause) {
        CancellationException e = new CancellationException("the decoding was cancelled");
        e.initCause(cause);
        return e;
    }

    /**
     * Describes a failure of the media layer.
     * <p>
     * 미디어 계층의 실패를 설명한다.
     *
     * @param e the exception / 예외
     * @return the message / 메시지
     */
    private static String describe(MediaException e) {
        switch (e.getKind()) {
        case UNSUPPORTED_FORMAT:
            return "the audio format is not supported: " + e.getMessage();
        case TOO_LARGE:
            return "the audio data is too large: " + e.getMessage();
        case ACCESS_FAILED:
            return "the audio data could not be read: " + e.getMessage();
        default:
            return "the audio data is damaged: " + e.getMessage();
        }
    }

    /**
     * Creates an <code>EncodingError</code> with a cause.
     * <p>
     * 원인을 붙인 <code>EncodingError</code> 를 만든다.
     *
     * @param message the message / 메시지
     * @param cause   the cause / 원인
     * @return the exception / 예외
     */
    static AudioException encodingError(String message, Throwable cause) {
        AudioException e = new AudioException(AudioException.Kind.ENCODING, message);
        e.initCause(cause);
        return e;
    }
}
