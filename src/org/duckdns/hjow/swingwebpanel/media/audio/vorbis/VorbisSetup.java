package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * The configuration of a Vorbis stream from its identification and setup header packets (Vorbis I specification, sections 4.2.2 and
 * 4.2.4): channels, sample rate, the two block sizes, the codebooks, floors, residues, mappings and modes. The comment header is not
 * needed for decoding and is only checked for its type.
 * <p>
 * 식별 헤더 패킷과 설정 헤더 패킷에서 얻는 Vorbis 스트림의 설정(Vorbis I 규격 4.2.2, 4.2.4절): 채널 수, 샘플링 주파수, 두 블록 크기, 코드북, 플로어, 잔차,
 * 매핑, 모드. 주석 헤더는 디코딩에 필요 없으므로 유형만 확인한다.
 */
final class VorbisSetup {

    /**
     * A mapping: how channels are coupled and which floor and residue every channel uses.
     * <p>
     * 매핑: 채널을 어떻게 결합하고 채널마다 어떤 플로어와 잔차를 쓰는지.
     */
    static final class Mapping {
        /**
         * Magnitude channel of every coupling step.
         * <p>
         * 결합 단계마다의 크기(magnitude) 채널.
         */
        int[] magnitude;

        /**
         * Angle channel of every coupling step.
         * <p>
         * 결합 단계마다의 각도(angle) 채널.
         */
        int[] angle;

        /**
         * Submap of every channel.
         * <p>
         * 채널마다의 하위 맵.
         */
        int[] mux;

        /**
         * Floor of every submap.
         * <p>
         * 하위 맵마다의 플로어.
         */
        int[] submapFloor;

        /**
         * Residue of every submap.
         * <p>
         * 하위 맵마다의 잔차.
         */
        int[] submapResidue;
    }

    /**
     * Number of channels.
     * <p>
     * 채널 수.
     */
    final int channels;

    /**
     * Sample rate.
     * <p>
     * 샘플링 주파수.
     */
    final int sampleRate;

    /**
     * Short block size.
     * <p>
     * 짧은 블록 크기.
     */
    final int blockSize0;

    /**
     * Long block size.
     * <p>
     * 긴 블록 크기.
     */
    final int blockSize1;

    /**
     * The floors.
     * <p>
     * 플로어.
     */
    VorbisFloor[] floors;

    /**
     * The residues.
     * <p>
     * 잔차.
     */
    VorbisResidue[] residues;

    /**
     * The mappings.
     * <p>
     * 매핑.
     */
    Mapping[] mappings;

    /**
     * Block flag of every mode: true for the long block size.
     * <p>
     * 모드마다의 블록 플래그. 긴 블록 크기이면 true.
     */
    boolean[] modeLong;

    /**
     * Mapping number of every mode.
     * <p>
     * 모드마다의 매핑 번호.
     */
    int[] modeMapping;

    /**
     * Parses the identification header.
     * <p>
     * 식별 헤더를 해석한다.
     *
     * @param id the identification packet / 식별 패킷
     * @throws MediaException CORRUPT for an invalid header, UNSUPPORTED_FORMAT for another Vorbis version / 올바르지 않은 헤더는 CORRUPT, 다른 Vorbis 버전은 UNSUPPORTED_FORMAT
     */
    VorbisSetup(byte[] id) throws MediaException {
        if (id.length < 30 || id[0] != 1 || !Bytes.matches(id, 1, "vorbis")) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis identification header");
        }
        if (Bytes.le32(id, 7) != 0) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported Vorbis version");
        }
        channels = id[11] & 0xFF;
        long rate = Bytes.le32(id, 12);
        int sizes = id[28] & 0xFF;
        int b0 = sizes & 15;
        int b1 = sizes >> 4;
        if (channels < 1 || rate < 1 || rate > 1000000 || b0 < 6 || b0 > 13 || b1 < 6 || b1 > 13 || b0 > b1 || (id[29] & 1) == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis identification values");
        }
        sampleRate = (int) rate;
        blockSize0 = 1 << b0;
        blockSize1 = 1 << b1;
    }

    /**
     * Parses the setup header.
     * <p>
     * 설정 헤더를 해석한다.
     *
     * @param setup the setup packet / 설정 패킷
     * @throws MediaException CORRUPT for an invalid header / 올바르지 않은 헤더이면 CORRUPT
     */
    void parseSetup(byte[] setup) throws MediaException {
        if (setup.length < 8 || setup[0] != 5 || !Bytes.matches(setup, 1, "vorbis")) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis setup header");
        }
        VorbisBitReader br = new VorbisBitReader(setup);
        br.read(8);
        br.read(24);
        br.read(24);
        int bookCount = br.read(8) + 1;
        VorbisCodebook[] books = new VorbisCodebook[bookCount];
        // 코드북을 하나씩 읽는다.
        for (int i = 0; i < bookCount; i++) {
            books[i] = new VorbisCodebook(br);
        }
        int times = br.read(6) + 1;
        // 시간 영역 변환 항목은 모두 0 이어야 한다(쓰이지 않는다).
        for (int i = 0; i < times; i++) {
            if (br.read(16) != 0) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis time domain transform");
            }
        }
        int floorCount = br.read(6) + 1;
        floors = new VorbisFloor[floorCount];
        // 플로어를 유형에 따라 읽는다.
        for (int i = 0; i < floorCount; i++) {
            int type = br.read(16);
            if (type == 0) {
                floors[i] = new VorbisFloor0(br, books);
            } else if (type == 1) {
                floors[i] = new VorbisFloor1(br, books);
            } else {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis floor type");
            }
        }
        int residueCount = br.read(6) + 1;
        residues = new VorbisResidue[residueCount];
        // 잔차를 유형에 따라 읽는다.
        for (int i = 0; i < residueCount; i++) {
            int type = br.read(16);
            if (type > 2) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis residue type");
            }
            residues[i] = new VorbisResidue(br, books, type);
        }
        int mappingCount = br.read(6) + 1;
        mappings = new Mapping[mappingCount];
        // 매핑을 하나씩 읽는다.
        for (int i = 0; i < mappingCount; i++) {
            mappings[i] = parseMapping(br, floorCount, residueCount);
        }
        int modeCount = br.read(6) + 1;
        modeLong = new boolean[modeCount];
        modeMapping = new int[modeCount];
        // 모드를 하나씩 읽는다.
        for (int i = 0; i < modeCount; i++) {
            modeLong[i] = br.read(1) != 0;
            int window = br.read(16);
            int transform = br.read(16);
            modeMapping[i] = br.read(8);
            if (window != 0 || transform != 0 || modeMapping[i] >= mappingCount) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis mode");
            }
        }
        if (br.read(1) == 0 || br.isEndOfPacket()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the Vorbis setup header is cut off or lacks the framing bit");
        }
    }

    /**
     * Parses one mapping.
     * <p>
     * 매핑 하나를 해석한다.
     *
     * @param br           packet bits / 패킷 비트
     * @param floorCount   number of floors / 플로어 수
     * @param residueCount number of residues / 잔차 수
     * @return the mapping / 매핑
     * @throws MediaException CORRUPT for an invalid mapping / 올바르지 않은 매핑이면 CORRUPT
     */
    private Mapping parseMapping(VorbisBitReader br, int floorCount, int residueCount) throws MediaException {
        if (br.read(16) != 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis mapping type");
        }
        Mapping m = new Mapping();
        int submaps = br.read(1) != 0 ? br.read(4) + 1 : 1;
        int steps = br.read(1) != 0 ? br.read(8) + 1 : 0;
        m.magnitude = new int[steps];
        m.angle = new int[steps];
        int bits = VorbisBitReader.ilog(channels - 1);
        // 결합 단계마다 크기 채널과 각도 채널을 읽는다.
        for (int i = 0; i < steps; i++) {
            m.magnitude[i] = br.read(bits);
            m.angle[i] = br.read(bits);
            if (m.magnitude[i] == m.angle[i] || m.magnitude[i] >= channels || m.angle[i] >= channels) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis channel coupling");
            }
        }
        if (br.read(2) != 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "reserved Vorbis mapping bits are not zero");
        }
        m.mux = new int[channels];
        if (submaps > 1) {
            // 채널마다 속한 하위 맵을 읽는다.
            for (int c = 0; c < channels; c++) {
                m.mux[c] = br.read(4);
                if (m.mux[c] >= submaps) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis submap number");
                }
            }
        }
        m.submapFloor = new int[submaps];
        m.submapResidue = new int[submaps];
        // 하위 맵마다 플로어와 잔차 번호를 읽는다.
        for (int s = 0; s < submaps; s++) {
            br.read(8);
            m.submapFloor[s] = br.read(8);
            m.submapResidue[s] = br.read(8);
            if (m.submapFloor[s] >= floorCount || m.submapResidue[s] >= residueCount) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis submap configuration");
            }
        }
        return m;
    }
}
