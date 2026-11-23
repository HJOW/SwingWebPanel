package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

/**
 * The CRC-32 of Ogg pages: generator polynomial 0x04C11DB7, initial value 0, no reflection and no final xor, table driven.
 * <p>
 * Ogg 페이지의 CRC-32: 생성 다항식 0x04C11DB7, 초기값 0, 비트 반사 없음, 최종 xor 없음. 표를 이용한다.
 */
final class OggCrc {

    /**
     * The lookup table for one byte.
     * <p>
     * 바이트 하나용 조회 표.
     */
    private static final int[] TABLE = new int[256];

    static {
        // 바이트 값마다 8번 시프트하며 표를 만든다.
        for (int i = 0; i < 256; i++) {
            int r = i << 24;
            // 표 한 칸을 8비트 시프트로 계산한다.
            for (int k = 0; k < 8; k++) {
                r = (r & 0x80000000) != 0 ? (r << 1) ^ 0x04C11DB7 : r << 1;
            }
            TABLE[i] = r;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private OggCrc() {
    }

    /**
     * Continues a checksum over more bytes.
     * <p>
     * 바이트를 더해 체크섬을 이어서 계산한다.
     *
     * @param crc    checksum so far (0 at the start) / 지금까지의 체크섬(처음에는 0)
     * @param d      data / 데이터
     * @param offset first byte / 첫 바이트
     * @param length number of bytes / 바이트 수
     * @return the new checksum / 새 체크섬
     */
    static int update(int crc, byte[] d, int offset, int length) {
        int c = crc;
        // 바이트마다 표를 이용해 갱신한다.
        for (int i = 0; i < length; i++) {
            c = (c << 8) ^ TABLE[((c >>> 24) ^ d[offset + i]) & 0xFF];
        }
        return c;
    }
}
