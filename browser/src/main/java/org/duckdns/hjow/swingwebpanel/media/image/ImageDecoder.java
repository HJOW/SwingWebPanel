package org.duckdns.hjow.swingwebpanel.media.image;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A decoder for one image format (plan items P18-02, P18-03). Implementations are pure Java (Java 6) and use only the public JRE API; they
 * never start external programs. The registry {@link ImageDecoders} picks a decoder by looking at the first bytes of the data (file
 * signature) and uses the MIME type only as a hint, so a wrong extension or MIME type does not decide the format.
 * <p>
 * 이미지 형식 하나의 디코더(계획 P18-02, P18-03). 구현은 순수 Java(Java 6)이며 JRE 공개 API 만 쓰고 외부 프로그램을 실행하지 않는다.
 * 등록부 {@link ImageDecoders} 는 데이터의 첫 바이트(파일 시그니처)로 디코더를 고르고 MIME 타입은 힌트로만 쓰므로 잘못된 확장자나 MIME 타입이
 * 형식을 결정하지 못한다.
 */
public interface ImageDecoder {

    /**
     * Returns a short lower-case name of the format (for diagnostics), for example <code>png</code>.
     * <p>
     * 형식의 짧은 소문자 이름(진단용)을 반환한다. 예: <code>png</code>.
     *
     * @return format name / 형식 이름
     */
    String getFormatName();

    /**
     * Returns the MIME types of the format (lower case), for example <code>image/png</code>.
     * <p>
     * 형식의 MIME 타입(소문자)을 반환한다. 예: <code>image/png</code>.
     *
     * @return MIME types / MIME 타입
     */
    String[] getMimeTypes();

    /**
     * Tells whether the first bytes of the data look like this format (file signature check only; no full parsing).
     * <p>
     * 데이터의 첫 바이트가 이 형식처럼 보이는지 알려 준다(파일 시그니처 확인만 하며 전체 해석은 하지 않는다).
     *
     * @param head the first bytes of the data (the registry passes up to 1024 bytes; fewer if the data is shorter) / 데이터의 첫 바이트(등록부는 최대 1024바이트를 넘기며 데이터가 짧으면 그보다 적음)
     * @return true if the signature matches / 시그니처가 맞으면 true
     */
    boolean accepts(byte[] head);

    /**
     * Decodes the whole image.
     * <p>
     * 이미지 전체를 디코딩한다.
     *
     * @param data   the encoded data / 인코딩된 데이터
     * @param limits limits to respect / 지켜야 할 제한
     * @return the decoded image with at least one frame / 프레임이 하나 이상인 디코딩된 이미지
     * @throws MediaException CORRUPT for damaged data, TOO_LARGE over a limit, UNSUPPORTED_FORMAT for a variant of the format that is not supported / 손상은 CORRUPT, 제한 초과는 TOO_LARGE, 지원하지 않는 변형은 UNSUPPORTED_FORMAT
     */
    DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException;
}
