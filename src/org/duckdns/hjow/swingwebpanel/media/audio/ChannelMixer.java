package org.duckdns.hjow.swingwebpanel.media.audio;

/**
 * Mixes the channels of a multi-channel layout down to stereo (plan item P18-07). Every input channel has a role; front left/right keep
 * their side, front/back centre go to both sides at -3 dB, left/right surround and back channels go to their side at -3 dB, and the low
 * frequency channel is dropped. The weights of each side are then scaled by one common factor so that full-scale input can never exceed
 * full scale at the output. Layouts with one or two channels are not mixed (mono stays mono).
 * <p>
 * 다채널 배치를 스테레오로 다운믹스한다(계획 P18-07). 입력 채널마다 역할이 있다: 앞 왼쪽/오른쪽은 자기 쪽을 유지하고, 앞/뒤 중앙은 -3 dB 로 양쪽에
 * 들어가며, 왼쪽/오른쪽 서라운드와 뒤 채널은 -3 dB 로 자기 쪽에 들어가고, 저주파 채널은 버린다. 각 쪽의 가중치는 공통 계수로 나누어 풀 스케일 입력이
 * 출력에서 풀 스케일을 넘지 않게 한다. 채널이 하나나 둘인 배치는 섞지 않는다(모노는 모노로 남는다).
 */
public final class ChannelMixer {

    /**
     * Role: front left.
     * <p>
     * 역할: 앞 왼쪽.
     */
    public static final int FL = 0;

    /**
     * Role: front right.
     * <p>
     * 역할: 앞 오른쪽.
     */
    public static final int FR = 1;

    /**
     * Role: front centre.
     * <p>
     * 역할: 앞 중앙.
     */
    public static final int FC = 2;

    /**
     * Role: low frequency effects (dropped by the mix).
     * <p>
     * 역할: 저주파 효과(믹스에서 버린다).
     */
    public static final int LFE = 3;

    /**
     * Role: back left.
     * <p>
     * 역할: 뒤 왼쪽.
     */
    public static final int BL = 4;

    /**
     * Role: back right.
     * <p>
     * 역할: 뒤 오른쪽.
     */
    public static final int BR = 5;

    /**
     * Role: back centre.
     * <p>
     * 역할: 뒤 중앙.
     */
    public static final int BC = 6;

    /**
     * Role: side left.
     * <p>
     * 역할: 옆 왼쪽.
     */
    public static final int SL = 7;

    /**
     * Role: side right.
     * <p>
     * 역할: 옆 오른쪽.
     */
    public static final int SR = 8;

    /**
     * Role: not used in the mix.
     * <p>
     * 역할: 믹스에 쓰지 않음.
     */
    public static final int NONE = 9;

    /**
     * Weight of a -3 dB channel.
     * <p>
     * -3 dB 채널의 가중치.
     */
    private static final float HALF_POWER = 0.70710678f;

    /**
     * Weight of every input channel in the left output.
     * <p>
     * 입력 채널마다의 왼쪽 출력 가중치.
     */
    private final float[] left;

    /**
     * Weight of every input channel in the right output.
     * <p>
     * 입력 채널마다의 오른쪽 출력 가중치.
     */
    private final float[] right;

    /**
     * Creates a mixer for channel roles.
     * <p>
     * 채널 역할로 믹서를 만든다.
     *
     * @param roles role of every input channel (values of the role constants) / 입력 채널마다의 역할(역할 상수 값)
     */
    private ChannelMixer(int[] roles) {
        left = new float[roles.length];
        right = new float[roles.length];
        float sumL = 0;
        float sumR = 0;
        // 역할마다 왼쪽/오른쪽 가중치를 정하고 각 쪽의 합을 구한다.
        for (int i = 0; i < roles.length; i++) {
            switch (roles[i]) {
            case FL:
                left[i] = 1f;
                break;
            case FR:
                right[i] = 1f;
                break;
            case FC:
            case BC:
                left[i] = HALF_POWER;
                right[i] = HALF_POWER;
                break;
            case BL:
            case SL:
                left[i] = HALF_POWER;
                break;
            case BR:
            case SR:
                right[i] = HALF_POWER;
                break;
            default:
                break;
            }
            sumL += left[i];
            sumR += right[i];
        }
        float scale = 1f / Math.max(Math.max(sumL, sumR), 1e-6f);
        // 두 쪽이 같은 계수로 나뉘도록 공통 계수를 곱한다.
        for (int i = 0; i < roles.length; i++) {
            left[i] *= scale;
            right[i] *= scale;
        }
    }

    /**
     * Returns the default mixer for a number of channels: the usual speaker order of WAV, FLAC and Vorbis (3: L R C; 4: L R BL BR;
     * 5: L R C BL BR; 6: L R C LFE BL BR; 7: L R C LFE BC SL SR; 8: L R C LFE BL BR SL SR), and alternating left/right for more channels.
     * <p>
     * 채널 수의 기본 믹서를 반환한다: WAV, FLAC, Vorbis 의 일반적인 스피커 순서(3: L R C, 4: L R BL BR, 5: L R C BL BR, 6: L R C LFE BL BR,
     * 7: L R C LFE BC SL SR, 8: L R C LFE BL BR SL SR)이며 채널이 더 많으면 왼쪽/오른쪽을 번갈아 배정한다.
     *
     * @param channels number of input channels (at least 1) / 입력 채널 수(1 이상)
     * @return mixer / 믹서
     */
    public static ChannelMixer forChannels(int channels) {
        int[] roles = new int[channels];
        switch (channels) {
        case 1:
            roles[0] = FC;
            break;
        case 2:
            roles[0] = FL;
            roles[1] = FR;
            break;
        case 3:
            set(roles, FL, FR, FC);
            break;
        case 4:
            set(roles, FL, FR, BL, BR);
            break;
        case 5:
            set(roles, FL, FR, FC, BL, BR);
            break;
        case 6:
            set(roles, FL, FR, FC, LFE, BL, BR);
            break;
        case 7:
            set(roles, FL, FR, FC, LFE, BC, SL, SR);
            break;
        case 8:
            set(roles, FL, FR, FC, LFE, BL, BR, SL, SR);
            break;
        default:
            // 채널이 아홉 개 이상이면 왼쪽과 오른쪽을 번갈아 배정한다.
            for (int i = 0; i < channels; i++) {
                roles[i] = (i & 1) == 0 ? FL : FR;
            }
            break;
        }
        return new ChannelMixer(roles);
    }

    /**
     * Returns a mixer for the speaker mask of a WAVE_FORMAT_EXTENSIBLE header: the set bits, from the lowest, name the roles in channel
     * order (bit 0 front left, 1 front right, 2 front centre, 3 LFE, 4 back left, 5 back right, 8 back centre, 9 side left, 10 side right;
     * other speakers are not mixed). If the number of set bits differs from the channel count the default layout is used.
     * <p>
     * WAVE_FORMAT_EXTENSIBLE 헤더의 스피커 마스크용 믹서를 반환한다: 설정된 비트가 낮은 쪽부터 채널 순서대로 역할을 나타낸다(비트 0 앞 왼쪽, 1 앞 오른쪽,
     * 2 앞 중앙, 3 LFE, 4 뒤 왼쪽, 5 뒤 오른쪽, 8 뒤 중앙, 9 옆 왼쪽, 10 옆 오른쪽, 다른 스피커는 섞지 않음). 설정된 비트 수가 채널 수와 다르면 기본
     * 배치를 쓴다.
     *
     * @param channels number of input channels / 입력 채널 수
     * @param mask     channel mask / 채널 마스크
     * @return mixer / 믹서
     */
    public static ChannelMixer forWavMask(int channels, long mask) {
        if (Long.bitCount(mask & 0xFFFFFFFFL) != channels) {
            return forChannels(channels);
        }
        int[] roles = new int[channels];
        int idx = 0;
        // 마스크의 켜진 비트를 낮은 쪽부터 채널 순서에 대응시킨다.
        for (int bit = 0; bit < 32 && idx < channels; bit++) {
            if ((mask & (1L << bit)) != 0) {
                roles[idx++] = roleOfBit(bit);
            }
        }
        return new ChannelMixer(roles);
    }

    /**
     * Returns a mixer for the channel order of Vorbis I (section 4.3.9 of the specification), which Opus uses for its channel mapping family 1: it
     * differs from the order of WAV and FLAC for three channels and more (centre before right: 3 = L C R, 5 = L C R BL BR, 6 = L C R BL BR LFE,
     * 7 = L C R SL SR BC LFE, 8 = L C R SL SR BL BR LFE). Up to two channels and four channels are the same as in {@link #forChannels(int)}.
     * <p>
     * Vorbis I 의 채널 순서(규격 4.3.9절)로 믹서를 만든다. Opus 의 채널 매핑 계열 1 이 이 순서를 쓴다. 채널이 셋 이상일 때 WAV 와 FLAC 의 순서와
     * 다르다(오른쪽보다 중앙이 먼저: 3 = L C R, 5 = L C R BL BR, 6 = L C R BL BR LFE, 7 = L C R SL SR BC LFE, 8 = L C R SL SR BL BR LFE).
     * 채널이 둘 이하이거나 넷이면 {@link #forChannels(int)} 와 같다.
     *
     * @param channels number of channels / 채널 수
     * @return mixer / 믹서
     */
    public static ChannelMixer forVorbisOrder(int channels) {
        int[] roles = new int[channels];
        switch (channels) {
        case 3:
            set(roles, FL, FC, FR);
            break;
        case 5:
            set(roles, FL, FC, FR, BL, BR);
            break;
        case 6:
            set(roles, FL, FC, FR, BL, BR, LFE);
            break;
        case 7:
            set(roles, FL, FC, FR, SL, SR, BC, LFE);
            break;
        case 8:
            set(roles, FL, FC, FR, SL, SR, BL, BR, LFE);
            break;
        default:
            return forChannels(channels);
        }
        return new ChannelMixer(roles);
    }

    /**
     * Returns a mixer for explicit roles.
     * <p>
     * 명시한 역할로 믹서를 만든다.
     *
     * @param roles role of every input channel / 입력 채널마다의 역할
     * @return mixer / 믹서
     */
    public static ChannelMixer forRoles(int[] roles) {
        return new ChannelMixer(roles.clone());
    }

    /**
     * Fills the first roles of an array.
     * <p>
     * 배열의 앞쪽 역할을 채운다.
     *
     * @param roles array / 배열
     * @param v     roles in order / 순서대로의 역할
     */
    private static void set(int[] roles, int... v) {
        System.arraycopy(v, 0, roles, 0, v.length);
    }

    /**
     * Maps a bit of the WAVE speaker mask to a role.
     * <p>
     * WAVE 스피커 마스크의 비트를 역할로 바꾼다.
     *
     * @param bit bit number / 비트 번호
     * @return role / 역할
     */
    private static int roleOfBit(int bit) {
        switch (bit) {
        case 0:
            return FL;
        case 1:
            return FR;
        case 2:
            return FC;
        case 3:
            return LFE;
        case 4:
            return BL;
        case 5:
            return BR;
        case 8:
            return BC;
        case 9:
            return SL;
        case 10:
            return SR;
        default:
            return NONE;
        }
    }

    /**
     * Returns the number of input channels.
     * <p>
     * 입력 채널 수를 반환한다.
     *
     * @return channels / 채널 수
     */
    public int getInputChannels() {
        return left.length;
    }

    /**
     * Mixes one frame.
     * <p>
     * 프레임 하나를 섞는다.
     *
     * @param frame the samples of one frame, one per input channel (full scale 1.0) / 입력 채널마다 하나인 프레임의 샘플(풀 스케일 1.0)
     * @param out   two floats that receive left and right / 왼쪽과 오른쪽을 받는 float 두 개
     */
    public void mix(float[] frame, float[] out) {
        float l = 0;
        float r = 0;
        // 채널마다 가중치를 곱해 더한다.
        for (int i = 0; i < left.length; i++) {
            l += frame[i] * left[i];
            r += frame[i] * right[i];
        }
        out[0] = l;
        out[1] = r;
    }
}
