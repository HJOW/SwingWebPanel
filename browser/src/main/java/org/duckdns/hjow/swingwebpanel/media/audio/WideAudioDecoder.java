package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * An {@link AudioDecoder} that can also deliver the channels and the precision of the file (plan item P25-05): in addition to
 * {@link #open(byte[], DecodeLimits)}, which gives 16 bit PCM of at most two channels for playing, {@link #openWide(byte[], DecodeLimits)} gives
 * a {@link FloatPcmSource}. A decoder that does not implement this interface is used through the 16 bit path by
 * {@link AudioDecoders#openWide}. Both methods accept and reject the same data.
 * <p>
 * 파일의 채널과 정밀도도 줄 수 있는 {@link AudioDecoder}(계획 P25-05): 재생용으로 채널이 최대 둘인 16비트 PCM 을 주는 {@link #open(byte[], DecodeLimits)} 에
 * 더해 {@link #openWide(byte[], DecodeLimits)} 는 {@link FloatPcmSource} 를 준다. 이 인터페이스를 구현하지 않은 디코더는 {@link AudioDecoders#openWide} 가
 * 16비트 경로로 쓴다. 두 메서드는 같은 데이터를 받아들이고 같은 데이터를 거부한다.
 */
public interface WideAudioDecoder extends AudioDecoder {

    /**
     * Opens the data for decoding with the channels and the precision of the file. Headers are validated here (so unsupported or damaged files
     * fail early) but samples are decoded only when {@link FloatPcmSource#read} is called.
     * <p>
     * 파일의 채널과 정밀도로 디코딩하려고 데이터를 연다. 헤더는 여기서 검증하지만(지원하지 않거나 손상된 파일이 일찍 실패하도록) 샘플은
     * {@link FloatPcmSource#read} 를 호출할 때 디코딩한다.
     *
     * @param data   the whole encoded data (kept by the source; not modified) / 인코딩된 전체 데이터(소스가 보관하며 수정하지 않음)
     * @param limits limits to respect / 지켜야 할 제한
     * @return a float source positioned at the start / 처음에 위치한 float 소스
     * @throws MediaException the same failures as {@link #open(byte[], DecodeLimits)} / {@link #open(byte[], DecodeLimits)} 와 같은 실패
     */
    FloatPcmSource openWide(byte[] data, DecodeLimits limits) throws MediaException;
}
