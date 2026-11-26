package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A decoder for an array or a seekable byte source. On a successful open the returned PCM source owns the encoded input.
 * <p>
 * 배열이나 탐색 가능한 바이트 원천을 받는 디코더. 열기에 성공하면 반환한 PCM 소스가 인코딩 입력을 소유한다.
 */
public abstract class ByteAudioDecoder implements WideAudioDecoder {

    /**
     * Checks an encoded array without copying it. / 인코딩 배열을 복사하지 않고 판별한다.
     * @param data the encoded bytes or null / 인코딩 바이트 또는 null
     * @return true when the signature matches / 시그니처가 일치하면 true
     */
    public final boolean accepts(byte[] data) {
        return data != null && acceptsSource(new ArrayByteSource(data));
    }

    /**
     * Checks the signature of a byte source without taking its ownership. / 바이트 원천을 소유하지 않고 시그니처를 확인한다.
     * @param data the encoded input / 인코딩 입력
     * @return true when the signature matches / 시그니처가 일치하면 true
     */
    public abstract boolean acceptsSource(ByteSource data);

    /**
     * Opens an encoded array as 16-bit PCM. / 인코딩 배열을 16비트 PCM으로 연다.
     * @param data the encoded bytes / 인코딩 바이트
     * @param limits resource limits / 리소스 제한
     * @return the PCM source / PCM 소스
     * @throws MediaException if the media cannot be opened / 미디어를 열 수 없으면
     */
    public final PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        return openSource(new ArrayByteSource(data), limits);
    }

    /**
     * Opens an encoded array as full-width float PCM. / 인코딩 배열을 전체 정밀도의 float PCM으로 연다.
     * @param data the encoded bytes / 인코딩 바이트
     * @param limits resource limits / 리소스 제한
     * @return the float PCM source / float PCM 소스
     * @throws MediaException if the media cannot be opened / 미디어를 열 수 없으면
     */
    public final FloatPcmSource openWide(byte[] data, DecodeLimits limits) throws MediaException {
        return openWideSource(new ArrayByteSource(data), limits);
    }

    /**
     * Opens 16-bit PCM and transfers input ownership on success. / 16비트 PCM을 열고 성공할 때 입력 소유권을 옮긴다.
     * @param data the encoded input / 인코딩 입력
     * @param limits resource limits / 리소스 제한
     * @return the PCM source / PCM 소스
     * @throws MediaException if the media cannot be opened / 미디어를 열 수 없으면
     */
    public final PcmSource openSource(ByteSource data, DecodeLimits limits) throws MediaException {
        return (PcmSource) open(data, limits, false);
    }

    /**
     * Opens float PCM and transfers input ownership on success. / float PCM을 열고 성공할 때 입력 소유권을 옮긴다.
     * @param data the encoded input / 인코딩 입력
     * @param limits resource limits / 리소스 제한
     * @return the float PCM source / float PCM 소스
     * @throws MediaException if the media cannot be opened / 미디어를 열 수 없으면
     */
    public final FloatPcmSource openWideSource(ByteSource data, DecodeLimits limits) throws MediaException {
        return (FloatPcmSource) open(data, limits, true);
    }

    /**
     * Checks the common container-offset limit and translates failed input reads. / 공통 컨테이너 위치 한도를 확인하고 입력 읽기 오류를 변환한다.
     * @param data the encoded input / 인코딩 입력
     * @param limits resource limits / 리소스 제한
     * @param wide true for float PCM / float PCM이면 true
     * @return the source of the requested precision / 요청한 정밀도의 소스
     * @throws MediaException if the media cannot be opened / 미디어를 열 수 없으면
     */
    private Object open(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException {
        try {
            if (data.length() > Integer.MAX_VALUE) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "encoded audio exceeds the container-offset limit");
            }
            return create(data, limits, wide);
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        }
    }

    /**
     * Parses format-specific data and builds the corresponding decoder. / 형식별 데이터를 읽고 해당 디코더를 만든다.
     * @param data the encoded input / 인코딩 입력
     * @param limits resource limits / 리소스 제한
     * @param wide true for float PCM / float PCM이면 true
     * @return a {@link PcmSource} or a {@link FloatPcmSource} / PcmSource 또는 FloatPcmSource
     * @throws MediaException if the format cannot be opened / 형식을 열 수 없으면
     */
    protected abstract Object create(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException;
}
