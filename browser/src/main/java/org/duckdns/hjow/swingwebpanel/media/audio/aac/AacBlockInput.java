package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * Reads indexed raw AAC blocks into a reusable buffer. The returned bit reader is valid until the next block is read.
 * <p>
 * 색인된 AAC 원시 블록을 재사용 버퍼로 읽는다. 반환한 비트 판독기는 다음 블록을 읽기 전까지 유효하다.
 */
final class AacBlockInput {

    /** Encoded input owned by the PCM source after open. / 열기에 성공하면 PCM 소스가 소유하는 인코딩 입력. */
    private final ByteSource data;
    /** Offset of each raw data block. / 원시 데이터 블록별 위치. */
    private final int[] offsets;
    /** Encoded length of each raw data block. / 원시 데이터 블록별 인코딩 길이. */
    private final int[] lengths;
    /** Reused encoded block storage. / 인코딩 블록을 보관하는 재사용 버퍼. */
    private byte[] buffer = new byte[0];

    /**
     * Takes the block index and keeps the encoded input. / 블록 색인을 받으며 인코딩 입력을 보관한다.
     * @param data encoded input / 인코딩 입력
     * @param offsets block offsets / 블록 위치
     * @param lengths block lengths / 블록 길이
     */
    AacBlockInput(ByteSource data, int[] offsets, int[] lengths) {
        this.data = data;
        this.offsets = offsets;
        this.lengths = lengths;
    }

    /** Returns the number of blocks. / 블록 수를 반환한다. */
    int size() {
        return offsets.length;
    }

    /**
     * Reads one indexed block. / 색인의 블록 하나를 읽는다.
     * @param index index of the block / 블록의 색인
     * @return bit reader for the bytes read / 읽은 바이트의 비트 판독기
     * @throws MediaException if the input cannot be read / 입력을 읽을 수 없으면
     */
    AacBits read(int index) throws MediaException {
        int length = lengths[index];
        if (buffer.length < length) {
            buffer = new byte[length];
        }
        try {
            Bytes.readFully(data, offsets[index], buffer, 0, length);
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        }
        return new AacBits(buffer, 0, length);
    }

    /** Closes the input when the PCM source is released. / PCM 소스가 해제되면 입력을 닫는다. */
    void close() {
        data.close();
    }
}
