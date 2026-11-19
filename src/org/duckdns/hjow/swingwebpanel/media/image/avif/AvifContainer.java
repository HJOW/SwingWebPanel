package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A reader of the HEIF / ISO base media file format structure of an AVIF file (plan item P18-03; ISO/IEC 23008-12 and the AV1 Image File
 * Format): it finds the primary item, the item locations (iloc), the item infos (iinf), the item references (iref) and the item properties
 * (iprp: ispe, colr, pixi, av1C, auxC, irot, imir, clap, ...) and returns the bytes of an item. It does not decode any image data.
 * <p>
 * AVIF 파일의 HEIF / ISO 기본 미디어 파일 형식 구조 읽기(계획 P18-03; ISO/IEC 23008-12 와 AV1 이미지 파일 형식): 기본 항목, 항목 위치(iloc), 항목
 * 정보(iinf), 항목 참조(iref), 항목 속성(iprp: ispe, colr, pixi, av1C, auxC, irot, imir, clap 등)을 찾고 항목의 바이트를 돌려준다. 이미지 데이터는
 * 디코딩하지 않는다.
 */
final class AvifContainer {

    /**
     * An item of the file.
     * <p>
     * 파일의 항목 하나.
     */
    static final class Item {

        /**
         * The item identifier.
         * <p>
         * 항목 식별자.
         */
        int id;

        /**
         * The four character item type (for example <code>av01</code> or <code>grid</code>).
         * <p>
         * 네 글자 항목 종류(예: <code>av01</code> 또는 <code>grid</code>).
         */
        String type = "";

        /**
         * The construction method of the location: 0 file offset, 1 data of the item data box, 2 other item.
         * <p>
         * 위치의 구성 방법: 0 파일 오프셋, 1 항목 데이터 상자의 데이터, 2 다른 항목.
         */
        int constructionMethod;

        /**
         * The start of each extent of the item.
         * <p>
         * 항목의 각 범위의 시작.
         */
        final List<Long> extentOffsets = new ArrayList<Long>();

        /**
         * The length of each extent of the item (-1 for the rest of the file).
         * <p>
         * 항목의 각 범위의 길이(-1 은 파일의 나머지).
         */
        final List<Long> extentLengths = new ArrayList<Long>();

        /**
         * The property associations in the order of the file: pairs of property index and essential flag.
         * <p>
         * 파일 순서대로의 속성 연결: 속성 색인과 필수 플래그의 쌍.
         */
        final List<int[]> properties = new ArrayList<int[]>();
    }

    /**
     * A property box of the item property container.
     * <p>
     * 항목 속성 컨테이너의 속성 상자.
     */
    static final class Property {

        /**
         * The four character box type.
         * <p>
         * 네 글자 상자 종류.
         */
        String type = "";

        /**
         * The offset of the payload in the file.
         * <p>
         * 페이로드의 파일 내 오프셋.
         */
        int offset;

        /**
         * The length of the payload.
         * <p>
         * 페이로드의 길이.
         */
        int length;
    }

    /**
     * A track of the movie box that holds an image sequence (animated AVIF) or an auxiliary sequence (alpha): the samples of the track,
     * each of them a temporal unit of AV1 data, with their position in the file and their duration.
     * <p>
     * 이미지 시퀀스(애니메이션 AVIF) 또는 보조 시퀀스(알파)를 담은 무비 상자의 트랙: 트랙의 샘플(각각 AV1 데이터의 시간 단위), 파일 안의 위치와 지속 시간.
     */
    static final class Track {

        /**
         * The track identifier.
         * <p>
         * 트랙 식별자.
         */
        int trackId;

        /**
         * The handler type of the media (<code>pict</code>, <code>vide</code> or <code>auxv</code>).
         * <p>
         * 미디어의 핸들러 종류(<code>pict</code>, <code>vide</code>, <code>auxv</code>).
         */
        String handler = "";

        /**
         * The number of time units per second of the media.
         * <p>
         * 미디어의 초당 시간 단위 수.
         */
        long timescale;

        /**
         * The track that this track is an auxiliary of (the track reference <code>auxl</code>), or -1.
         * <p>
         * 이 트랙이 보조인 트랙(트랙 참조 <code>auxl</code>), 없으면 -1.
         */
        int auxFor = -1;

        /**
         * True if the sample entry says that the track holds the alpha channel (<code>auxi</code> box).
         * <p>
         * 샘플 항목이 트랙이 알파 채널을 담는다고 하면(<code>auxi</code> 상자) true.
         */
        boolean alpha;

        /**
         * The track that this track names with the track reference <code>prem</code> (its alpha is premultiplied into the colour of that
         * track), or -1.
         * <p>
         * 이 트랙이 트랙 참조 <code>prem</code> 으로 가리키는 트랙(그 알파가 이 트랙의 색에 미리 곱해져 있음), 없으면 -1.
         */
        int premRef = -1;

        /**
         * The duration of the first edit of the edit list in time units of the movie (0 if there is no edit list).
         * <p>
         * 편집 목록의 첫 편집의 지속 시간(무비의 시간 단위, 편집 목록이 없으면 0).
         */
        long editDuration;

        /**
         * True if the edit list asks to repeat the sequence (the repeat flag of the edit list box).
         * <p>
         * 편집 목록이 시퀀스 반복을 요청하면(편집 목록 상자의 반복 플래그) true.
         */
        boolean repeat;

        /**
         * The number of samples.
         * <p>
         * 샘플 수.
         */
        int sampleCount;

        /**
         * The position of each sample in the file.
         * <p>
         * 각 샘플의 파일 내 위치.
         */
        long[] sampleOffsets = new long[0];

        /**
         * The size of each sample in bytes.
         * <p>
         * 각 샘플의 바이트 크기.
         */
        int[] sampleSizes = new int[0];

        /**
         * The duration of each sample in time units of the media.
         * <p>
         * 각 샘플의 지속 시간(미디어 시간 단위).
         */
        long[] sampleDurations = new long[0];

        /**
         * A synthetic item that holds the properties of the sample entry (<code>av1C</code>, <code>colr</code>, ...), so that they can be read like
         * the properties of an item.
         * <p>
         * 샘플 항목의 속성(<code>av1C</code>, <code>colr</code> 등)을 담은 가상 항목. 항목의 속성처럼 읽을 수 있게 한다.
         */
        final Item entry = new Item();
    }

    /**
     * A reference of the item reference box.
     * <p>
     * 항목 참조 상자의 참조 하나.
     */
    static final class Reference {

        /**
         * The reference type.
         * <p>
         * 참조 종류.
         */
        String type = "";

        /**
         * The referring item.
         * <p>
         * 참조하는 항목.
         */
        int from;

        /**
         * The referenced items.
         * <p>
         * 참조되는 항목.
         */
        int[] to = new int[0];
    }

    /**
     * The file data.
     * <p>
     * 파일 데이터.
     */
    final byte[] data;

    /**
     * The items by identifier in the order of the file.
     * <p>
     * 파일 순서의 항목(식별자별).
     */
    final Map<Integer, Item> items = new LinkedHashMap<Integer, Item>();

    /**
     * The properties of the item property container (the first one has the index 1).
     * <p>
     * 항목 속성 컨테이너의 속성(첫째가 색인 1).
     */
    final List<Property> properties = new ArrayList<Property>();

    /**
     * The item references.
     * <p>
     * 항목 참조.
     */
    final List<Reference> references = new ArrayList<Reference>();

    /**
     * The primary item or -1.
     * <p>
     * 기본 항목 또는 -1.
     */
    int primaryItem = -1;

    /**
     * True if the file has a movie box (an image sequence).
     * <p>
     * 파일에 무비 상자가 있으면(이미지 시퀀스) true.
     */
    boolean hasMovie;

    /**
     * The number of time units per second of the movie (the movie header box), 0 if there is none.
     * <p>
     * 무비의 초당 시간 단위 수(무비 헤더 상자), 없으면 0.
     */
    long movieTimescale;

    /**
     * The tracks of the movie box that have an AV1 sample entry (empty if there is no movie box).
     * <p>
     * AV1 샘플 항목을 가진 무비 상자의 트랙(무비 상자가 없으면 비어 있다).
     */
    final List<Track> tracks = new ArrayList<Track>();

    /**
     * The brands of the file type box (major brand first).
     * <p>
     * 파일 종류 상자의 브랜드(주 브랜드가 먼저).
     */
    final List<String> brands = new ArrayList<String>();

    /**
     * The offset of the item data box payload or -1.
     * <p>
     * 항목 데이터 상자 페이로드의 오프셋 또는 -1.
     */
    private int idatOffset = -1;

    /**
     * The length of the item data box payload.
     * <p>
     * 항목 데이터 상자 페이로드의 길이.
     */
    private int idatLength;

    /**
     * Creates a reader over the file data.
     * <p>
     * 파일 데이터에 대한 읽기를 만든다.
     *
     * @param data the file / 파일
     */
    private AvifContainer(byte[] data) {
        this.data = data;
    }

    /**
     * Parses the structure of an AVIF file.
     * <p>
     * AVIF 파일의 구조를 해석한다.
     *
     * @param data the file / 파일
     * @return the container / 컨테이너
     * @throws MediaException CORRUPT for a damaged structure, UNSUPPORTED_FORMAT if the file is not an AVIF file / 구조가 손상되면 CORRUPT, AVIF 파일이 아니면 UNSUPPORTED_FORMAT
     */
    static AvifContainer parse(byte[] data) throws MediaException {
        AvifContainer c = new AvifContainer(data);
        int pos = 0;
        boolean sawFtyp = false;
        // 최상위 상자를 차례로 읽는다.
        while (pos + 8 <= data.length) {
            long size = c.u32(pos);
            String type = c.fourcc(pos + 4);
            int header = 8;
            if (size == 1) {
                size = c.u64(pos + 8);
                header = 16;
            } else if (size == 0) {
                size = data.length - pos;
            }
            if (size < header || pos + size > data.length) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid box size of " + type);
            }
            int payload = pos + header;
            int end = (int) (pos + size);
            if (type.equals("ftyp")) {
                c.parseFtyp(payload, end);
                sawFtyp = true;
            } else if (type.equals("meta")) {
                c.parseMeta(payload, end);
            } else if (type.equals("moov")) {
                c.hasMovie = true;
                c.parseMoov(payload, end);
            }
            pos = end;
        }
        if (!sawFtyp) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: the file type box is missing");
        }
        return c;
    }

    /**
     * Reads an unsigned 8 bit value.
     * <p>
     * 부호 없는 8비트 값을 읽는다.
     *
     * @param p the position / 위치
     * @return the value / 값
     * @throws MediaException CORRUPT if the position is outside the file / 위치가 파일 밖이면 CORRUPT
     */
    private int u8(int p) throws MediaException {
        if (p < 0 || p >= data.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated data");
        }
        return data[p] & 0xFF;
    }

    /**
     * Reads an unsigned big endian 16 bit value.
     * <p>
     * 부호 없는 빅 엔디언 16비트 값을 읽는다.
     *
     * @param p the position / 위치
     * @return the value / 값
     * @throws MediaException CORRUPT if the position is outside the file / 위치가 파일 밖이면 CORRUPT
     */
    private int u16(int p) throws MediaException {
        return (u8(p) << 8) | u8(p + 1);
    }

    /**
     * Reads an unsigned big endian 32 bit value.
     * <p>
     * 부호 없는 빅 엔디언 32비트 값을 읽는다.
     *
     * @param p the position / 위치
     * @return the value / 값
     * @throws MediaException CORRUPT if the position is outside the file / 위치가 파일 밖이면 CORRUPT
     */
    private long u32(int p) throws MediaException {
        return ((long) u16(p) << 16) | u16(p + 2);
    }

    /**
     * Reads an unsigned big endian 64 bit value (values above 2^62 are rejected).
     * <p>
     * 부호 없는 빅 엔디언 64비트 값을 읽는다(2^62 를 넘는 값은 거부한다).
     *
     * @param p the position / 위치
     * @return the value / 값
     * @throws MediaException CORRUPT if the position is outside the file or the value is too large / 위치가 파일 밖이거나 값이 너무 크면 CORRUPT
     */
    private long u64(int p) throws MediaException {
        long hi = u32(p);
        long lo = u32(p + 4);
        if (hi > 0x3FFFFFFFL) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: 64 bit value is too large");
        }
        return (hi << 32) | lo;
    }

    /**
     * Reads a value of 0, 4 or 8 bytes (sizes used by the location box).
     * <p>
     * 0, 4 또는 8바이트 값을 읽는다(위치 상자에서 쓰는 크기).
     *
     * @param p    the position / 위치
     * @param size the size in bytes (0, 4 or 8) / 바이트 단위 크기(0, 4 또는 8)
     * @return the value / 값
     * @throws MediaException CORRUPT for an invalid size / 크기가 잘못되었으면 CORRUPT
     */
    private long uN(int p, int size) throws MediaException {
        if (size == 0) {
            return 0;
        }
        if (size == 4) {
            return u32(p);
        }
        if (size == 8) {
            return u64(p);
        }
        throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid field size " + size);
    }

    /**
     * Reads a four character code.
     * <p>
     * 네 글자 코드를 읽는다.
     *
     * @param p the position / 위치
     * @return the code / 코드
     * @throws MediaException CORRUPT if the position is outside the file / 위치가 파일 밖이면 CORRUPT
     */
    private String fourcc(int p) throws MediaException {
        char[] ch = new char[4];
        // 네 바이트를 문자로 옮긴다.
        for (int i = 0; i < 4; i++) {
            ch[i] = (char) u8(p + i);
        }
        return new String(ch);
    }

    /**
     * Reads the file type box.
     * <p>
     * 파일 종류 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for a truncated box / 상자가 잘렸으면 CORRUPT
     */
    private void parseFtyp(int p, int end) throws MediaException {
        brands.add(fourcc(p));
        // 부 버전(4바이트) 다음의 호환 브랜드를 모두 읽는다.
        for (int q = p + 8; q + 4 <= end; q += 4) {
            brands.add(fourcc(q));
        }
    }

    /**
     * Reads the meta box.
     * <p>
     * meta 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseMeta(int p, int end) throws MediaException {
        int pos = p + 4;
        // 전체 상자의 버전과 플래그 다음의 하위 상자를 읽는다.
        while (pos + 8 <= end) {
            long size = u32(pos);
            String type = fourcc(pos + 4);
            int header = 8;
            if (size == 1) {
                size = u64(pos + 8);
                header = 16;
            } else if (size == 0) {
                size = end - pos;
            }
            if (size < header || pos + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid size of the box " + type);
            }
            int payload = pos + header;
            int boxEnd = (int) (pos + size);
            if (type.equals("pitm")) {
                int version = u8(payload);
                primaryItem = version == 0 ? u16(payload + 4) : (int) u32(payload + 4);
            } else if (type.equals("iloc")) {
                parseIloc(payload, boxEnd);
            } else if (type.equals("iinf")) {
                parseIinf(payload, boxEnd);
            } else if (type.equals("iref")) {
                parseIref(payload, boxEnd);
            } else if (type.equals("iprp")) {
                parseIprp(payload, boxEnd);
            } else if (type.equals("idat")) {
                idatOffset = payload;
                idatLength = boxEnd - payload;
            }
            pos = boxEnd;
        }
    }

    /**
     * Returns the end of the box that starts at a position (and checks it against the end of the parent).
     * <p>
     * 위치에서 시작하는 상자의 끝을 반환한다(부모의 끝과 대조한다).
     *
     * @param pos the start of the box / 상자의 시작
     * @param end the end of the parent / 부모의 끝
     * @return the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for an invalid size / 크기가 잘못되었으면 CORRUPT
     */
    private int boxEnd(int pos, int end) throws MediaException {
        long size = u32(pos);
        if (size == 0) {
            return end;
        }
        if (size < 8 || pos + size > end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid box size of " + fourcc(pos + 4));
        }
        return (int) (pos + size);
    }

    /**
     * Reads the movie box: the tracks that hold AV1 samples.
     * <p>
     * 무비 상자를 읽는다: AV1 샘플을 담은 트랙.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseMoov(int p, int end) throws MediaException {
        int pos = p;
        // 무비 상자의 하위 상자 중 트랙을 읽는다.
        while (pos + 8 <= end) {
            int boxEnd = boxEnd(pos, end);
            if (fourcc(pos + 4).equals("mvhd")) {
                int version = u8(pos + 8);
                movieTimescale = u32(pos + 8 + (version == 1 ? 20 : 12));
            } else if (fourcc(pos + 4).equals("trak")) {
                Track t = new Track();
                if (parseTrak(t, pos + 8, boxEnd) && t.sampleCount > 0) {
                    tracks.add(t);
                }
            }
            pos = boxEnd;
        }
    }

    /**
     * Reads a track box.
     * <p>
     * 트랙 상자를 읽는다.
     *
     * @param t   the track to fill / 채울 트랙
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @return true if the track has an AV1 sample entry / 트랙이 AV1 샘플 항목을 가졌으면 true
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private boolean parseTrak(Track t, int p, int end) throws MediaException {
        int pos = p;
        boolean av1 = false;
        // 트랙의 하위 상자(tkhd, edts, tref, mdia)를 읽는다.
        while (pos + 8 <= end) {
            int boxEnd = boxEnd(pos, end);
            String type = fourcc(pos + 4);
            int payload = pos + 8;
            if (type.equals("tkhd")) {
                int version = u8(payload);
                t.trackId = (int) u32(payload + (version == 1 ? 20 : 12));
            } else if (type.equals("edts")) {
                int q = payload;
                // 편집 목록 상자의 반복 플래그를 찾는다.
                while (q + 8 <= boxEnd) {
                    int qe = boxEnd(q, boxEnd);
                    if (fourcc(q + 4).equals("elst")) {
                        t.repeat = (u32(q + 8) & 1) != 0;
                        // 첫 편집의 지속 시간(무비 시간 단위)을 읽는다.
                        if (u32(q + 12) >= 1) {
                            t.editDuration = u8(q + 8) == 1 ? u64(q + 16) : u32(q + 16);
                        }
                    }
                    q = qe;
                }
            } else if (type.equals("tref")) {
                int q = payload;
                // 트랙 참조 중 auxl(이 트랙이 보조인 트랙)을 찾는다.
                while (q + 8 <= boxEnd) {
                    int qe = boxEnd(q, boxEnd);
                    if (fourcc(q + 4).equals("auxl") && q + 12 <= qe) {
                        t.auxFor = (int) u32(q + 8);
                    } else if (fourcc(q + 4).equals("prem") && q + 12 <= qe) {
                        t.premRef = (int) u32(q + 8);
                    }
                    q = qe;
                }
            } else if (type.equals("mdia")) {
                av1 = parseMdia(t, payload, boxEnd);
            }
            pos = boxEnd;
        }
        return av1;
    }

    /**
     * Reads a media box.
     * <p>
     * 미디어 상자를 읽는다.
     *
     * @param t   the track / 트랙
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @return true if the track has an AV1 sample entry / 트랙이 AV1 샘플 항목을 가졌으면 true
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private boolean parseMdia(Track t, int p, int end) throws MediaException {
        int pos = p;
        boolean av1 = false;
        // 미디어 헤더(mdhd), 핸들러(hdlr), 미디어 정보(minf)를 읽는다.
        while (pos + 8 <= end) {
            int boxEnd = boxEnd(pos, end);
            String type = fourcc(pos + 4);
            int payload = pos + 8;
            if (type.equals("mdhd")) {
                int version = u8(payload);
                t.timescale = u32(payload + (version == 1 ? 20 : 12));
            } else if (type.equals("hdlr")) {
                t.handler = fourcc(payload + 8);
            } else if (type.equals("minf")) {
                int q = payload;
                // 미디어 정보의 샘플 표(stbl)를 찾는다.
                while (q + 8 <= boxEnd) {
                    int qe = boxEnd(q, boxEnd);
                    if (fourcc(q + 4).equals("stbl")) {
                        av1 = parseStbl(t, q + 8, qe);
                    }
                    q = qe;
                }
            }
            pos = boxEnd;
        }
        return av1;
    }

    /**
     * Reads a sample table box: the sample entry, the durations, the sizes and the positions of the samples.
     * <p>
     * 샘플 표 상자를 읽는다: 샘플 항목, 샘플의 지속 시간, 크기, 위치.
     *
     * @param t   the track / 트랙
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @return true if the track has an AV1 sample entry / 트랙이 AV1 샘플 항목을 가졌으면 true
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private boolean parseStbl(Track t, int p, int end) throws MediaException {
        int pos = p;
        boolean av1 = false;
        int stszBox = -1;
        int sttsBox = -1;
        int stscBox = -1;
        int chunkBox = -1;
        boolean chunk64 = false;
        int stszEnd = 0;
        int sttsEnd = 0;
        int stscEnd = 0;
        int chunkEnd = 0;
        // 샘플 표의 하위 상자 위치를 모은다.
        while (pos + 8 <= end) {
            int boxEnd = boxEnd(pos, end);
            String type = fourcc(pos + 4);
            int payload = pos + 8;
            if (type.equals("stsd")) {
                // 첫 샘플 항목이 av01 이면 하위 상자(av1C, colr, auxi 등)를 속성으로 등록한다.
                int entry = payload + 8;
                if (entry + 8 <= boxEnd && fourcc(entry + 4).equals("av01")) {
                    av1 = true;
                    int entryEnd = boxEnd(entry, boxEnd);
                    int q = entry + 8 + 78;
                    // 샘플 항목의 하위 상자를 속성으로 등록한다.
                    while (q + 8 <= entryEnd) {
                        int qe = boxEnd(q, entryEnd);
                        Property pr = new Property();
                        pr.type = fourcc(q + 4);
                        pr.offset = q + 8;
                        pr.length = qe - q - 8;
                        properties.add(pr);
                        t.entry.properties.add(new int[] { properties.size(), 0 });
                        if (pr.type.equals("auxi") && pr.length > 4) {
                            StringBuilder sb = new StringBuilder();
                            // 보조 종류 문자열에 알파가 들어 있는지 확인한다.
                            for (int i = 4; i < pr.length; i++) {
                                int b = u8(pr.offset + i);
                                if (b == 0) {
                                    break;
                                }
                                sb.append((char) b);
                            }
                            t.alpha = sb.toString().toLowerCase().indexOf("alpha") >= 0;
                        }
                        q = qe;
                    }
                }
            } else if (type.equals("stts")) {
                sttsBox = payload;
                sttsEnd = boxEnd;
            } else if (type.equals("stsc")) {
                stscBox = payload;
                stscEnd = boxEnd;
            } else if (type.equals("stsz")) {
                stszBox = payload;
                stszEnd = boxEnd;
            } else if (type.equals("stco") || type.equals("co64")) {
                chunkBox = payload;
                chunkEnd = boxEnd;
                chunk64 = type.equals("co64");
            }
            pos = boxEnd;
        }
        if (!av1 || stszBox < 0 || sttsBox < 0 || stscBox < 0 || chunkBox < 0) {
            return false;
        }
        readSampleTables(t, stszBox, stszEnd, sttsBox, sttsEnd, stscBox, stscEnd, chunkBox, chunkEnd, chunk64);
        return true;
    }

    /**
     * The largest number of samples of a track that is read.
     * <p>
     * 읽는 트랙의 최대 샘플 수.
     */
    private static final int MAX_SAMPLES = 1 << 20;

    /**
     * Expands the sample tables of a track into the size, the position and the duration of every sample.
     * <p>
     * 트랙의 샘플 표를 샘플마다 크기, 위치, 지속 시간으로 펼친다.
     *
     * @param t        the track / 트랙
     * @param stsz     the payload of the sample size box / 샘플 크기 상자의 페이로드
     * @param stszEnd  the end of that box / 그 상자의 끝
     * @param stts     the payload of the time to sample box / 시간-샘플 상자의 페이로드
     * @param sttsEnd  the end of that box / 그 상자의 끝
     * @param stsc     the payload of the sample to chunk box / 샘플-청크 상자의 페이로드
     * @param stscEnd  the end of that box / 그 상자의 끝
     * @param chunk    the payload of the chunk offset box / 청크 오프셋 상자의 페이로드
     * @param chunkEnd the end of that box / 그 상자의 끝
     * @param chunk64  true if the chunk offsets have 64 bits / 청크 오프셋이 64비트이면 true
     * @throws MediaException CORRUPT for inconsistent tables / 표가 서로 맞지 않으면 CORRUPT
     */
    private void readSampleTables(Track t, int stsz, int stszEnd, int stts, int sttsEnd, int stsc, int stscEnd, int chunk, int chunkEnd,
            boolean chunk64) throws MediaException {
        long uniform = u32(stsz + 4);
        long count = u32(stsz + 8);
        if (count > MAX_SAMPLES) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "AVIF: too many samples in the image sequence");
        }
        if (uniform == 0 && stsz + 12 + count * 4 > stszEnd) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated sample size box");
        }
        int n = (int) count;
        t.sampleCount = n;
        t.sampleSizes = new int[n];
        // 샘플 크기를 읽는다(모두 같으면 한 값).
        for (int i = 0; i < n; i++) {
            long sz = uniform != 0 ? uniform : u32(stsz + 12 + i * 4);
            if (sz > data.length) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: a sample is larger than the file");
            }
            t.sampleSizes[i] = (int) sz;
        }
        t.sampleDurations = new long[n];
        long entries = u32(stts + 4);
        if (stts + 8 + entries * 8 > sttsEnd) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated time to sample box");
        }
        int s = 0;
        // 시간-샘플 표의 (개수, 지속 시간) 쌍을 샘플마다 펼친다.
        for (long e = 0; e < entries && s < n; e++) {
            long cnt = u32(stts + 8 + (int) e * 8);
            long delta = u32(stts + 12 + (int) e * 8);
            // 항목의 개수만큼 같은 지속 시간을 채운다.
            for (long k = 0; k < cnt && s < n; k++) {
                t.sampleDurations[s++] = delta;
            }
        }
        long chunkCount = u32(chunk + 4);
        int width = chunk64 ? 8 : 4;
        if (chunk + 8 + chunkCount * width > chunkEnd) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated chunk offset box");
        }
        long stscEntries = u32(stsc + 4);
        if (stsc + 8 + stscEntries * 12 > stscEnd) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated sample to chunk box");
        }
        t.sampleOffsets = new long[n];
        int sample = 0;
        // 청크마다 청크에 속한 샘플의 위치를 크기를 더해 가며 구한다.
        for (long c = 1; c <= chunkCount && sample < n; c++) {
            long perChunk = 0;
            // 이 청크에 해당하는 샘플-청크 항목(첫 청크 번호가 c 이하인 마지막 항목)을 찾는다.
            for (long e = 0; e < stscEntries; e++) {
                long first = u32(stsc + 8 + (int) e * 12);
                if (first <= c) {
                    perChunk = u32(stsc + 12 + (int) e * 12);
                }
            }
            long off = chunk64 ? u64(chunk + 8 + (int) (c - 1) * 8) : u32(chunk + 8 + (int) (c - 1) * 4);
            // 청크 안의 샘플을 차례로 배치한다.
            for (long k = 0; k < perChunk && sample < n; k++) {
                t.sampleOffsets[sample] = off;
                off += t.sampleSizes[sample];
                sample++;
            }
        }
        if (sample < n) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the chunks do not hold all samples");
        }
        // 모든 샘플이 파일 안에 있는지 확인한다.
        for (int i = 0; i < n; i++) {
            if (t.sampleOffsets[i] < 0 || t.sampleOffsets[i] + t.sampleSizes[i] > data.length) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: a sample lies outside of the file");
            }
        }
    }

    /**
     * Tells if the file type box lists a brand.
     * <p>
     * 파일 종류 상자에 브랜드가 있는지 알려 준다.
     *
     * @param brand the four character brand / 네 글자 브랜드
     * @return true if the brand is the major brand or a compatible brand / 주 브랜드이거나 호환 브랜드이면 true
     */
    boolean hasBrand(String brand) {
        return brands.contains(brand);
    }

    /**
     * Finds the track that holds the colour images of an image sequence: the first track with an AV1 sample entry that is not an auxiliary
     * track.
     * <p>
     * 이미지 시퀀스의 색 이미지를 담은 트랙을 찾는다: AV1 샘플 항목이 있고 보조 트랙이 아닌 첫 트랙.
     *
     * @return the track or null / 트랙 또는 null
     */
    Track colorTrack() {
        // 보조 트랙(auxl 참조나 auxv 핸들러)이 아닌 첫 AV1 트랙을 찾는다.
        for (Track t : tracks) {
            if (t.auxFor < 0 && !t.alpha && !t.handler.equals("auxv")) {
                return t;
            }
        }
        return null;
    }

    /**
     * Finds the alpha track of a colour track: the auxiliary track that refers to it.
     * <p>
     * 색 트랙의 알파 트랙을 찾는다: 그 트랙을 참조하는 보조 트랙.
     *
     * @param color the colour track / 색 트랙
     * @return the alpha track or null / 알파 트랙 또는 null
     */
    Track alphaTrack(Track color) {
        // 색 트랙을 auxl 로 참조하는 보조 트랙 중 알파 종류를 찾는다.
        for (Track t : tracks) {
            if (t != color && t.auxFor == color.trackId && (t.alpha || t.handler.equals("auxv"))) {
                return t;
            }
        }
        return null;
    }

    /**
     * Gets or creates the item with an identifier.
     * <p>
     * 식별자의 항목을 가져오거나 만든다.
     *
     * @param id the identifier / 식별자
     * @return the item / 항목
     */
    private Item item(int id) {
        Item it = items.get(Integer.valueOf(id));
        if (it == null) {
            it = new Item();
            it.id = id;
            items.put(Integer.valueOf(id), it);
        }
        return it;
    }

    /**
     * Reads the item location box.
     * <p>
     * 항목 위치 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseIloc(int p, int end) throws MediaException {
        int version = u8(p);
        int pos = p + 4;
        int b = u8(pos++);
        int offsetSize = b >> 4;
        int lengthSize = b & 15;
        b = u8(pos++);
        int baseOffsetSize = b >> 4;
        int indexSize = version >= 1 ? (b & 15) : 0;
        long count;
        if (version < 2) {
            count = u16(pos);
            pos += 2;
        } else {
            count = u32(pos);
            pos += 4;
        }
        // 항목마다 위치 정보를 읽는다.
        for (long n = 0; n < count; n++) {
            if (pos > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated item location box");
            }
            int id;
            if (version < 2) {
                id = u16(pos);
                pos += 2;
            } else {
                id = (int) u32(pos);
                pos += 4;
            }
            int method = 0;
            if (version >= 1) {
                method = u16(pos) & 15;
                pos += 2;
            }
            pos += 2;
            long base = uN(pos, baseOffsetSize);
            pos += baseOffsetSize;
            int extents = u16(pos);
            pos += 2;
            Item it = item(id);
            it.constructionMethod = method;
            // 항목의 범위를 모두 읽는다.
            for (int e = 0; e < extents; e++) {
                if (version >= 1 && indexSize > 0) {
                    pos += indexSize;
                }
                long off = uN(pos, offsetSize);
                pos += offsetSize;
                long len = lengthSize == 0 ? -1 : uN(pos, lengthSize);
                pos += lengthSize;
                it.extentOffsets.add(Long.valueOf(base + off));
                it.extentLengths.add(Long.valueOf(len));
            }
        }
    }

    /**
     * Reads the item information box.
     * <p>
     * 항목 정보 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseIinf(int p, int end) throws MediaException {
        int version = u8(p);
        int pos = p + 4;
        long count;
        if (version == 0) {
            count = u16(pos);
            pos += 2;
        } else {
            count = u32(pos);
            pos += 4;
        }
        // 항목 정보 항목(infe)을 차례로 읽는다.
        for (long n = 0; n < count && pos + 8 <= end; n++) {
            long size = u32(pos);
            String type = fourcc(pos + 4);
            if (size < 8 || pos + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid item info entry");
            }
            int payload = pos + 8;
            int boxEnd = (int) (pos + size);
            if (type.equals("infe")) {
                int v = u8(payload);
                int q = payload + 4;
                if (v >= 2) {
                    int id;
                    if (v == 2) {
                        id = u16(q);
                        q += 2;
                    } else {
                        id = (int) u32(q);
                        q += 4;
                    }
                    q += 2;
                    item(id).type = fourcc(q);
                }
            }
            pos = boxEnd;
        }
    }

    /**
     * Reads the item reference box.
     * <p>
     * 항목 참조 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseIref(int p, int end) throws MediaException {
        int version = u8(p);
        int pos = p + 4;
        // 참조 종류별 상자를 차례로 읽는다.
        while (pos + 8 <= end) {
            long size = u32(pos);
            String type = fourcc(pos + 4);
            if (size < 8 || pos + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid item reference box");
            }
            int q = pos + 8;
            Reference r = new Reference();
            r.type = type;
            if (version == 0) {
                r.from = u16(q);
                q += 2;
            } else {
                r.from = (int) u32(q);
                q += 4;
            }
            int n = u16(q);
            q += 2;
            r.to = new int[n];
            // 참조되는 항목 식별자를 모두 읽는다.
            for (int i = 0; i < n; i++) {
                if (version == 0) {
                    r.to[i] = u16(q);
                    q += 2;
                } else {
                    r.to[i] = (int) u32(q);
                    q += 4;
                }
            }
            references.add(r);
            pos = (int) (pos + size);
        }
    }

    /**
     * Reads the item properties box.
     * <p>
     * 항목 속성 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseIprp(int p, int end) throws MediaException {
        int pos = p;
        // 속성 컨테이너(ipco)와 연결(ipma) 상자를 읽는다.
        while (pos + 8 <= end) {
            long size = u32(pos);
            String type = fourcc(pos + 4);
            if (size < 8 || pos + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid item properties box");
            }
            int payload = pos + 8;
            int boxEnd = (int) (pos + size);
            if (type.equals("ipco")) {
                int q = payload;
                // 속성 상자를 차례로 읽는다. 첫 번째가 색인 1 이다.
                while (q + 8 <= boxEnd) {
                    long psize = u32(q);
                    if (psize < 8 || q + psize > boxEnd) {
                        throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid property box");
                    }
                    Property pr = new Property();
                    pr.type = fourcc(q + 4);
                    pr.offset = q + 8;
                    pr.length = (int) (psize - 8);
                    properties.add(pr);
                    q = (int) (q + psize);
                }
            } else if (type.equals("ipma")) {
                parseIpma(payload, boxEnd);
            }
            pos = boxEnd;
        }
    }

    /**
     * Reads the item property association box.
     * <p>
     * 항목 속성 연결 상자를 읽는다.
     *
     * @param p   the start of the payload / 페이로드 시작
     * @param end the end of the box / 상자의 끝
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void parseIpma(int p, int end) throws MediaException {
        int version = u8(p);
        int flags = u16(p + 2);
        int pos = p + 4;
        long count = u32(pos);
        pos += 4;
        // 항목마다 연결된 속성 색인을 읽는다.
        for (long n = 0; n < count; n++) {
            if (pos >= end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated property association box");
            }
            int id;
            if (version < 1) {
                id = u16(pos);
                pos += 2;
            } else {
                id = (int) u32(pos);
                pos += 4;
            }
            int assoc = u8(pos++);
            Item it = item(id);
            // 항목에 연결된 속성 색인을 하나씩 읽는다.
            for (int a = 0; a < assoc; a++) {
                int v;
                if ((flags & 1) != 0) {
                    v = u16(pos);
                    pos += 2;
                    it.properties.add(new int[] { v & 0x7FFF, (v >> 15) & 1 });
                } else {
                    v = u8(pos++);
                    it.properties.add(new int[] { v & 0x7F, (v >> 7) & 1 });
                }
            }
        }
    }

    /**
     * Finds the first property of a type that is associated with an item.
     * <p>
     * 항목에 연결된 종류의 첫 번째 속성을 찾는다.
     *
     * @param it   the item / 항목
     * @param type the four character property type / 네 글자 속성 종류
     * @return the property or null / 속성 또는 null
     */
    Property property(Item it, String type) {
        // 항목에 연결된 속성 중 종류가 같은 첫 속성을 찾는다.
        for (int[] a : it.properties) {
            if (a[0] >= 1 && a[0] <= properties.size()) {
                Property pr = properties.get(a[0] - 1);
                if (pr.type.equals(type)) {
                    return pr;
                }
            }
        }
        return null;
    }

    /**
     * Returns the identifiers that an item refers to with a reference type.
     * <p>
     * 항목이 어떤 참조 종류로 참조하는 식별자를 반환한다.
     *
     * @param type the reference type / 참조 종류
     * @param from the referring item / 참조하는 항목
     * @return the referenced items in order / 순서대로의 참조 대상 항목
     */
    int[] referencedBy(String type, int from) {
        List<Integer> out = new ArrayList<Integer>();
        // 종류와 출발 항목이 맞는 참조의 대상을 모은다.
        for (Reference r : references) {
            if (r.type.equals(type) && r.from == from) {
                // 참조 대상 식별자를 목록에 담는다.
                for (int i = 0; i < r.to.length; i++) {
                    out.add(Integer.valueOf(r.to[i]));
                }
            }
        }
        int[] a = new int[out.size()];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i).intValue();
        }
        return a;
    }

    /**
     * Returns the items that refer to an item with a reference type.
     * <p>
     * 항목을 어떤 참조 종류로 참조하는 항목을 반환한다.
     *
     * @param type the reference type / 참조 종류
     * @param to   the referenced item / 참조되는 항목
     * @return the referring items / 참조하는 항목
     */
    int[] referencing(String type, int to) {
        List<Integer> out = new ArrayList<Integer>();
        // 종류가 맞고 대상 목록에 항목이 들어 있는 참조의 출발 항목을 모은다.
        for (Reference r : references) {
            if (!r.type.equals(type)) {
                continue;
            }
            // 대상 목록에 항목이 들어 있는지 확인한다.
            for (int i = 0; i < r.to.length; i++) {
                if (r.to[i] == to) {
                    out.add(Integer.valueOf(r.from));
                }
            }
        }
        int[] a = new int[out.size()];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i).intValue();
        }
        return a;
    }

    /**
     * Reads a property payload as an unsigned 32 bit value at an offset of the payload.
     * <p>
     * 속성 페이로드의 오프셋에서 부호 없는 32비트 값을 읽는다.
     *
     * @param pr  the property / 속성
     * @param off the offset in the payload / 페이로드 안의 오프셋
     * @return the value / 값
     * @throws MediaException CORRUPT if the property is too short / 속성이 너무 짧으면 CORRUPT
     */
    long propU32(Property pr, int off) throws MediaException {
        if (off + 4 > pr.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated " + pr.type + " property");
        }
        return u32(pr.offset + off);
    }

    /**
     * Reads a property payload as an unsigned 16 bit value at an offset of the payload.
     * <p>
     * 속성 페이로드의 오프셋에서 부호 없는 16비트 값을 읽는다.
     *
     * @param pr  the property / 속성
     * @param off the offset in the payload / 페이로드 안의 오프셋
     * @return the value / 값
     * @throws MediaException CORRUPT if the property is too short / 속성이 너무 짧으면 CORRUPT
     */
    int propU16(Property pr, int off) throws MediaException {
        if (off + 2 > pr.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated " + pr.type + " property");
        }
        return u16(pr.offset + off);
    }

    /**
     * Reads a property payload as an unsigned 8 bit value at an offset of the payload.
     * <p>
     * 속성 페이로드의 오프셋에서 부호 없는 8비트 값을 읽는다.
     *
     * @param pr  the property / 속성
     * @param off the offset in the payload / 페이로드 안의 오프셋
     * @return the value / 값
     * @throws MediaException CORRUPT if the property is too short / 속성이 너무 짧으면 CORRUPT
     */
    int propU8(Property pr, int off) throws MediaException {
        if (off + 1 > pr.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated " + pr.type + " property");
        }
        return u8(pr.offset + off);
    }

    /**
     * Returns the bytes of an item (all extents joined).
     * <p>
     * 항목의 바이트(모든 범위를 이은 것)를 반환한다.
     *
     * @param it       the item / 항목
     * @param maxBytes the largest accepted size / 허용하는 최대 크기
     * @return the data / 데이터
     * @throws MediaException CORRUPT if a location is outside the file, UNSUPPORTED_FORMAT for other construction methods, TOO_LARGE for a large item / 위치가 파일 밖이면 CORRUPT, 다른 구성 방법은 UNSUPPORTED_FORMAT, 큰 항목은 TOO_LARGE
     */
    byte[] itemData(Item it, long maxBytes) throws MediaException {
        if (it.extentOffsets.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: item " + it.id + " has no data");
        }
        if (it.constructionMethod != 0 && it.constructionMethod != 1) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: unsupported item construction method " + it.constructionMethod);
        }
        long total = 0;
        long[] starts = new long[it.extentOffsets.size()];
        long[] lens = new long[starts.length];
        // 범위마다 파일 안의 시작 위치와 길이를 확정하고 전체 크기를 구한다.
        for (int i = 0; i < starts.length; i++) {
            long off = it.extentOffsets.get(i).longValue();
            long len = it.extentLengths.get(i).longValue();
            long limit;
            if (it.constructionMethod == 1) {
                if (idatOffset < 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the item data box is missing");
                }
                off += idatOffset;
                limit = (long) idatOffset + idatLength;
            } else {
                limit = data.length;
            }
            if (len < 0) {
                len = limit - off;
            }
            if (off < 0 || len < 0 || off + len > limit) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: an item extent lies outside the file");
            }
            starts[i] = off;
            lens[i] = len;
            total += len;
        }
        if (total > maxBytes) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "AVIF: item " + it.id + " is too large");
        }
        byte[] out = new byte[(int) total];
        int w = 0;
        // 범위를 순서대로 이어 붙인다.
        for (int i = 0; i < starts.length; i++) {
            System.arraycopy(data, (int) starts[i], out, w, (int) lens[i]);
            w += (int) lens[i];
        }
        return out;
    }
}
