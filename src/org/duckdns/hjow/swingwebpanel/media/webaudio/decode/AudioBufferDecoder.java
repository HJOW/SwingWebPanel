package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.util.concurrent.CancellationException;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioLimits;

/**
 * The decoding pipeline of <code>decodeAudioData</code> (plan item P25-05): encoded audio file data in, an {@link AudioBuffer} at the sample
 * rate of the context out. It reuses the audio decoders of the media layer ({@link AudioDecoders}), which choose the decoder from the
 * <em>signature of the data</em> (the MIME type is only a hint, the specification says the type is determined by sniffing), so
 * every format the media elements play is decoded here too: WAV (PCM, float, mu-law, A-law, ADPCM), AIFF/AIFF-C, AU, FLAC (native and in
 * Ogg), MP3, AAC (ADTS and MP4), Ogg Vorbis, Opus and WebM audio.
 * <p>
 * <b>Samples.</b> The decoders deliver 16 bit PCM; each sample becomes <code>sample / 32768</code> (exact in a float), so the samples of a lossless
 * 16 bit file are exactly the ones in the file. The channels of the decoder are kept as they are (no mix is applied here): a mono file gives a
 * one channel buffer and a stereo file a two channel buffer with the left channel first. <b>Limit:</b> the audio decoders mix sources
 * of more than two channels down to stereo and round 24 bit, 32 bit and floating point samples to 16 bits <em>inside</em> the decoder, so a
 * 5.1 file gives a stereo buffer and a 24 bit file a buffer with 16 bit resolution; delivering the original channels and float precision
 * needs a decoder interface that does not exist yet (see <code>docs/progress/webaudio-b.md</code>).
 * <p>
 * <b>Resampling.</b> If the rate of the file differs from the wanted rate, every channel is converted with a {@link Resampler} while it is
 * decoded (block by block, so the whole file is never held in memory in float form before the conversion). The converted length is
 * <code>ceil(frames * wanted / rate)</code>.
 * <p>
 * <b>Errors.</b> Every failure of the data is an <code>EncodingError</code> ({@link AudioException.Kind#ENCODING}) with the cause attached
 * (unknown format, damaged or truncated data, a format variant that is not supported, a file with no samples, a sample rate that cannot be
 * converted, data over the limits, no memory): the specification does not distinguish them, and no partial result is returned. A cancelled decoding
 * ends with a {@link CancellationException} instead (no error: the result is simply not wanted). Arguments that the caller controls
 * (a null array, a sample rate outside 3000 to 768000 Hz) are errors of the caller.
 * <p>
 * <b>Limits.</b> The input is limited by {@link DecodeLimits} (64 MB by default), the output by
 * {@link WebAudioLimits#MAX_BUFFER_SAMPLES} samples (all channels together), the number of decoded input samples by
 * {@link #MAX_INPUT_SAMPLES}. The class has no state: it can be used from any number of threads at once.
 * <p>
 * <code>decodeAudioData</code> 의 디코딩 파이프라인(계획 P25-05): 인코딩된 오디오 파일 데이터가 들어가고 컨텍스트의 표본율인 {@link AudioBuffer} 가 나온다.
 * 미디어 계층의 오디오 디코더({@link AudioDecoders})를 재사용하며, 그 디코더는 <em>데이터의 시그니처</em>로 디코더를 고른다(MIME 타입은 힌트일 뿐이며
 * 규격도 타입을 스니핑으로 정한다고 한다). 그래서 미디어 요소가 재생하는 모든 형식을 여기서도 디코딩한다: WAV(PCM, 부동 소수점, mu-law, A-law, ADPCM),
 * AIFF/AIFF-C, AU, FLAC(네이티브와 Ogg), MP3, AAC(ADTS와 MP4), Ogg Vorbis, Opus, WebM 오디오.
 * <p>
 * <b>표본.</b> 디코더는 16비트 PCM 을 주며 표본마다 <code>표본 / 32768</code>(float 로 정확하다)이 되므로 무손실 16비트 파일의 표본은 파일에 있는 값
 * 그대로이다. 디코더의 채널은 그대로 둔다(여기서는 믹스하지 않는다). 모노 파일은 채널이 하나인 버퍼가, 스테레오 파일은 왼쪽 채널이 앞인 채널 둘의 버퍼가
 * 된다. <b>한계:</b> 오디오 디코더는 채널이 둘보다 많은 소스를 스테레오로 다운믹스하고 24비트, 32비트, 부동 소수점 표본을 디코더 <em>안에서</em> 16비트로
 * 반올림하므로 5.1 파일은 스테레오 버퍼가, 24비트 파일은 16비트 해상도의 버퍼가 된다. 원래의 채널과 float 정밀도를 주려면 아직 없는 디코더 인터페이스가
 * 필요하다(<code>docs/progress/webaudio-b.md</code> 참조).
 * <p>
 * <b>리샘플링.</b> 파일의 표본율이 원하는 표본율과 다르면 디코딩하는 동안 채널마다 {@link Resampler} 로 변환한다(블록 단위이므로 변환 전의 float 형태 전체를
 * 메모리에 두지 않는다). 변환한 길이는 <code>ceil(프레임 수 * 원하는 표본율 / 파일 표본율)</code> 이다.
 * <p>
 * <b>오류.</b> 데이터의 모든 실패는 원인을 붙인 <code>EncodingError</code>({@link AudioException.Kind#ENCODING})이다(알 수 없는 형식, 손상되거나 잘린 데이터,
 * 지원하지 않는 형식 변형, 표본이 없는 파일, 변환할 수 없는 표본율, 한도를 넘는 데이터, 메모리 부족). 규격은 이들을 구분하지 않으며 부분 결과는 돌려주지
 * 않는다. 취소된 디코딩은 대신 {@link CancellationException} 으로 끝난다(오류가 아니다. 결과가 필요하지 않을 뿐이다). 호출한 쪽이 정하는 인수(null 배열,
 * 3000 ~ 768000 Hz 밖의 표본율)는 호출한 쪽의 오류이다.
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
     * Decodes audio file data into a buffer at a sample rate, with the default limits.
     * <p>
     * 오디오 파일 데이터를 어떤 표본율의 버퍼로 디코딩한다. 기본 한도를 쓴다.
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
        return decode(data, mimeHint, sampleRate, cancel, DecodeLimits.DEFAULT, WebAudioLimits.MAX_BUFFER_SAMPLES);
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
        PcmSource source;
        try {
            source = AudioDecoders.open(data, mimeHint, lim);
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
     * source becomes an <code>EncodingError</code> (see the class description); the tests give it sources of their own.
     * <p>
     * 열린 소스를 끝까지 읽어 변환하고 버퍼를 만들며 무슨 일이 있어도 소스를 닫는다. 소스의 모든 실패는 <code>EncodingError</code> 가 된다(클래스 설명
     * 참조). 시험이 자기 소스를 준다.
     *
     * @param source     the source of 16 bit PCM; closed by this method / 16비트 PCM 소스. 이 메서드가 닫는다
     * @param sampleRate the wanted sample rate, already checked / 원하는 표본율. 이미 검사했다
     * @param cancel     the cancellation flag or null / 취소 플래그 또는 null
     * @param maxSamples the limit of the output in samples of all channels / 출력의 한도(모든 채널의 표본 수)
     * @return the buffer / 버퍼
     */
    static AudioBuffer decodeSource(PcmSource source, float sampleRate, DecodeCancellation cancel, long maxSamples) {
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
     * @return the buffer / 버퍼
     * @throws MediaException if the data is damaged / 데이터가 손상되었으면
     */
    private static AudioBuffer decodeFrom(PcmSource source, float sampleRate, DecodeCancellation cancel, long maxSamples) throws MediaException {
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
        byte[] pcm = new byte[BLOCK_FRAMES * channels * 2];
        float[][] chunk = new float[channels][BLOCK_FRAMES];
        float[][] converted = converters == null ? null : new float[channels][converters[0].maxOutputFrames(BLOCK_FRAMES)];
        long inputFrames = 0;
        int emptyReads = 0;
        // 블록 단위로 디코딩하고 float 로 바꾸어 (필요하면) 변환한 뒤 싱크에 모은다.
        for (;;) {
            checkCancelled(cancel);
            int n = source.read(pcm, 0, pcm.length);
            if (n < 0) {
                break;
            }
            int count = n / (channels * 2);
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
            // 인터리브된 16비트 표본을 채널별 float(표본 / 32768)로 나눈다.
            for (int f = 0; f < count; f++) {
                for (int c = 0; c < channels; c++) {
                    int p = (f * channels + c) * 2;
                    chunk[c][f] = ((short) ((pcm[p] & 0xFF) | (pcm[p + 1] << 8))) / 32768f;
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
        return AudioBuffer.wrap(result, sampleRate);
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
