package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

/**
 * The conversion between the <code>float[]</code> samples of the engine and the little endian bytes that the <code>Float32Array</code> of a script shows (the typed arrays of the
 * script engine keep their elements in a <code>byte[]</code>). Both directions work in place on arrays that the caller owns, so a render quantum allocates nothing.
 * <p>
 * 엔진의 <code>float[]</code> 표본과 스크립트의 <code>Float32Array</code> 가 보여 주는 리틀 엔디언 바이트 사이의 변환(스크립트 엔진의 형식화 배열은 요소를 <code>byte[]</code> 에 둔다). 두
 * 방향 모두 호출자가 가진 배열에서 제자리로 동작하므로 render quantum 은 아무것도 할당하지 않는다.
 */
final class FloatBytes {

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private FloatBytes() {
    }

    /**
     * Writes samples as little endian bytes.
     * <p>
     * 표본을 리틀 엔디언 바이트로 쓴다.
     *
     * @param src   the samples / 표본
     * @param count the number of samples / 표본 수
     * @param dst   the bytes, at least four times as long as <code>count</code> / 바이트(<code>count</code> 의 네 배 이상)
     */
    static void write(float[] src, int count, byte[] dst) {
        // 표본마다 네 바이트를 낮은 자리부터 쓴다.
        for (int i = 0; i < count; i++) {
            int bits = Float.floatToRawIntBits(src[i]);
            int o = i << 2;
            dst[o] = (byte) bits;
            dst[o + 1] = (byte) (bits >> 8);
            dst[o + 2] = (byte) (bits >> 16);
            dst[o + 3] = (byte) (bits >> 24);
        }
    }

    /**
     * Reads little endian bytes as samples.
     * <p>
     * 리틀 엔디언 바이트를 표본으로 읽는다.
     *
     * @param src   the bytes / 바이트
     * @param count the number of samples to read / 읽을 표본 수
     * @param dst   the samples, at least <code>count</code> long / 표본(<code>count</code> 이상)
     */
    static void read(byte[] src, int count, float[] dst) {
        // 네 바이트마다 표본 하나를 읽는다.
        for (int i = 0; i < count; i++) {
            int o = i << 2;
            int bits = (src[o] & 0xFF) | ((src[o + 1] & 0xFF) << 8) | ((src[o + 2] & 0xFF) << 16) | ((src[o + 3] & 0xFF) << 24);
            dst[i] = Float.intBitsToFloat(bits);
        }
    }
}
