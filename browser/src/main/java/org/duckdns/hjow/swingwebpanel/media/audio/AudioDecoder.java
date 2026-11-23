package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A decoder for one audio container/codec family (plan items P18-02, P18-07). Implementations are pure Java (Java 6) and use only the public
 * JRE API; they never start external programs and never call a native codec. The registry {@link AudioDecoders} picks a decoder from the file
 * signature (first bytes) and uses the MIME type only as a hint, so the same extension with a different real codec is handled correctly.
 * <p>
 * 오디오 컨테이너/코덱 계열 하나의 디코더(계획 P18-02, P18-07). 구현은 순수 Java(Java 6)이며 JRE 공개 API 만 쓰고 외부 프로그램을 실행하거나
 * 네이티브 코덱을 호출하지 않는다. 등록부 {@link AudioDecoders} 는 파일 시그니처(첫 바이트)로 디코더를 고르고 MIME 타입은 힌트로만 쓰므로
 * 확장자가 같아도 실제 코덱이 다른 경우를 올바르게 처리한다.
 */
public interface AudioDecoder {

    /**
     * Returns a short lower-case name of the format (for diagnostics), for example <code>wav</code>.
     * <p>
     * 형식의 짧은 소문자 이름(진단용)을 반환한다. 예: <code>wav</code>.
     *
     * @return format name / 형식 이름
     */
    String getFormatName();

    /**
     * Returns the MIME types handled (lower case, without parameters), for example <code>audio/wav</code>.
     * <p>
     * 처리하는 MIME 타입(소문자, 매개변수 없음)을 반환한다. 예: <code>audio/wav</code>.
     *
     * @return MIME types / MIME 타입
     */
    String[] getMimeTypes();

    /**
     * Tells whether the data looks like this format (file signature of the first bytes; the data array is complete, so containers whose
     * codec is only known from the headers may look a little further). No full parsing.
     * <p>
     * 데이터가 이 형식처럼 보이는지 알려 준다(첫 바이트의 파일 시그니처. 데이터 배열은 전체이므로 코덱을 헤더에서 알 수 있는 컨테이너는 조금 더 살펴볼
     * 수 있다). 전체 해석은 하지 않는다.
     *
     * @param data the whole encoded data / 인코딩된 전체 데이터
     * @return true if it matches / 맞으면 true
     */
    boolean accepts(byte[] data);

    /**
     * Opens the data for decoding. Headers are validated here (so unsupported or damaged files fail early) but samples are decoded only when
     * {@link PcmSource#read} is called.
     * <p>
     * 디코딩을 위해 데이터를 연다. 헤더는 여기서 검증하지만(지원하지 않거나 손상된 파일이 일찍 실패하도록) 샘플은
     * {@link PcmSource#read} 를 호출할 때 디코딩한다.
     *
     * @param data   the whole encoded data (kept by the source; not modified) / 인코딩된 전체 데이터(소스가 보관하며 수정하지 않음)
     * @param limits limits to respect / 지켜야 할 제한
     * @return a PCM source positioned at the start / 처음에 위치한 PCM 소스
     * @throws MediaException UNSUPPORTED_FORMAT for a codec variant that is not supported, CORRUPT for damaged headers, TOO_LARGE over a limit / 지원하지 않는 코덱 변형은 UNSUPPORTED_FORMAT, 헤더 손상은 CORRUPT, 제한 초과는 TOO_LARGE
     */
    PcmSource open(byte[] data, DecodeLimits limits) throws MediaException;
}
