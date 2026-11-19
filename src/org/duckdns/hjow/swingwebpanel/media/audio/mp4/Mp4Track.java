package org.duckdns.hjow.swingwebpanel.media.audio.mp4;

/**
 * One track of an MP4 / ISO base media file: its sample entry (codec, channels, sampling frequency, codec configuration) and the sample
 * table (position, size and duration of every sample), with the edit list start offset. Filled by {@link Mp4File}.
 * <p>
 * MP4 / ISO 기본 미디어 파일의 트랙 하나: 샘플 엔트리(코덱, 채널, 샘플링 주파수, 코덱 구성)와 샘플 표(샘플마다 위치, 크기, 길이), 편집 목록의
 * 시작 오프셋. {@link Mp4File} 이 채운다.
 */
public final class Mp4Track {

    /** Track id. / 트랙 id. */
    int id;
    /** Handler type, for example soun. / 핸들러 종류. 예: soun. */
    String handler = "";
    /** Four character code of the sample entry, for example mp4a or Opus. / 샘플 엔트리의 4글자 코드. 예: mp4a, Opus. */
    String format = "";
    /** Object type indication of the elementary stream descriptor (0x40 AAC, 0x6B MPEG-1 audio). / 기본 스트림 서술자의 객체 종류 표시. */
    int objectType;
    /** Decoder specific configuration (AudioSpecificConfig for AAC, the dOps box for Opus). / 디코더 고유 구성. */
    byte[] config = new byte[0];
    /** Channel count of the sample entry. / 샘플 엔트리의 채널 수. */
    int channels;
    /** Sampling frequency of the sample entry. / 샘플 엔트리의 샘플링 주파수. */
    int sampleRate;
    /** Media time scale (ticks per second). / 미디어 시간 단위(초당 틱). */
    int timescale = 1;
    /** Media duration in time scale units (0 if unknown). / 미디어 길이(시간 단위, 모르면 0). */
    long duration;
    /** Offset of every sample in the file. / 샘플마다 파일 안의 위치. */
    long[] offsets = new long[0];
    /** Size of every sample. / 샘플마다 크기. */
    int[] sizes = new int[0];
    /** Duration of every sample in time scale units. / 샘플마다 길이(시간 단위). */
    int[] durations = new int[0];
    /** Start of the edit list (media time of the first edit, in time scale units; 0 if none). / 편집 목록의 시작(첫 편집의 미디어 시간). */
    long editStart;
    /** Duration of the edit in movie time scale units (0 if none). / 편집의 길이(영상 시간 단위, 없으면 0). */
    long editDuration;
    /** Movie time scale used by the edit list. / 편집 목록이 쓰는 영상 시간 단위. */
    int movieTimescale = 1;

    /**
     * Returns the track id. / 트랙 id 를 반환한다.
     *
     * @return id / id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the handler type. / 핸들러 종류를 반환한다.
     *
     * @return handler / 핸들러
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Returns the sample entry format. / 샘플 엔트리 형식을 반환한다.
     *
     * @return format code / 형식 코드
     */
    public String getFormat() {
        return format;
    }

    /**
     * Returns the object type indication. / 객체 종류 표시를 반환한다.
     *
     * @return object type / 객체 종류
     */
    public int getObjectType() {
        return objectType;
    }

    /**
     * Returns the codec configuration bytes. / 코덱 구성 바이트를 반환한다.
     *
     * @return configuration / 구성
     */
    public byte[] getConfig() {
        return config;
    }

    /**
     * Returns the channel count of the sample entry. / 샘플 엔트리의 채널 수를 반환한다.
     *
     * @return channels / 채널 수
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Returns the sampling frequency of the sample entry. / 샘플 엔트리의 샘플링 주파수를 반환한다.
     *
     * @return rate / 주파수
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the media time scale. / 미디어 시간 단위를 반환한다.
     *
     * @return ticks per second / 초당 틱
     */
    public int getTimescale() {
        return timescale;
    }

    /**
     * Returns the media duration in time scale units. / 미디어 길이(시간 단위)를 반환한다.
     *
     * @return duration / 길이
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Returns the number of samples. / 샘플 수를 반환한다.
     *
     * @return count / 개수
     */
    public int getSampleCount() {
        return sizes.length;
    }

    /**
     * Returns the file offset of a sample. / 샘플의 파일 위치를 반환한다.
     *
     * @param i sample index / 샘플 번호
     * @return offset / 위치
     */
    public long getOffset(int i) {
        return offsets[i];
    }

    /**
     * Returns the size of a sample. / 샘플의 크기를 반환한다.
     *
     * @param i sample index / 샘플 번호
     * @return size in bytes / 바이트 수
     */
    public int getSize(int i) {
        return sizes[i];
    }

    /**
     * Returns the duration of a sample in time scale units. / 샘플의 길이(시간 단위)를 반환한다.
     *
     * @param i sample index / 샘플 번호
     * @return duration / 길이
     */
    public int getDuration(int i) {
        return durations[i];
    }

    /**
     * Returns the media time where the edit list starts. / 편집 목록이 시작하는 미디어 시간을 반환한다.
     *
     * @return time scale units / 시간 단위
     */
    public long getEditStart() {
        return editStart;
    }

    /**
     * Returns the duration of the edit in media time scale units, or 0 if there is no edit list.
     * <p>
     * 편집의 길이(미디어 시간 단위)를 반환하며 편집 목록이 없으면 0 이다.
     *
     * @return duration / 길이
     */
    public long getEditDurationInMediaUnits() {
        return movieTimescale <= 0 ? 0 : editDuration * timescale / movieTimescale;
    }
}
