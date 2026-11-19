package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

/**
 * A Vorbis floor: the spectral envelope of one channel of one packet. Decoding reads the floor data from the packet and renders the
 * envelope curve into an array of <code>half</code> values (half of the block size).
 * <p>
 * Vorbis 플로어: 패킷 하나의 채널 하나의 스펙트럼 포락선. 디코딩은 패킷에서 플로어 데이터를 읽고 포락선 곡선을 <code>half</code>(블록 크기의 절반)개의
 * 값으로 만든다.
 */
interface VorbisFloor {

    /**
     * Decodes the floor of a channel.
     * <p>
     * 채널의 플로어를 디코딩한다.
     *
     * @param br   packet bits / 패킷 비트
     * @param out  receives the curve, the first <code>half</code> values / 곡선을 받는다. 앞쪽 <code>half</code> 개
     * @param half half of the block size / 블록 크기의 절반
     * @return true if the channel has a floor in this packet (the curve was written), false if the channel is unused (silent) / 이 패킷에서 채널에 플로어가 있으면(곡선을 썼다) true, 쓰지 않는 채널(무음)이면 false
     */
    boolean decode(VorbisBitReader br, float[] out, int half);
}
