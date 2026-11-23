package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the sample conversions: G.711 tables against the reference encoders, float and integer narrowing, and the channel mixer.
 * <p>
 * 샘플 변환 시험: 기준 인코더와 대조한 G.711 표, float 와 정수 줄이기, 채널 믹서.
 */
public class SampleConversionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SampleConversionTest() {
    }

    /**
     * Known G.711 values and the round trip of the independent reference encoders over all 256 codes.
     * <p>
     * 알려진 G.711 값과 독립적인 기준 인코더로의 256개 코드 전체 왕복.
     */
    public void testG711() {
        Check.equal(0, PcmConverter.ulaw(0xFF), "mu-law 0xFF is zero");
        Check.equal(0, PcmConverter.ulaw(0x7F), "mu-law 0x7F is negative zero");
        Check.equal(-32124, PcmConverter.ulaw(0x00), "mu-law 0x00 is the most negative value");
        Check.equal(32124, PcmConverter.ulaw(0x80), "mu-law 0x80 is the most positive value");
        Check.equal(8, PcmConverter.alaw(0xD5), "A-law 0xD5");
        Check.equal(-8, PcmConverter.alaw(0x55), "A-law 0x55");
        Check.equal(32256, PcmConverter.alaw(0xAA), "A-law 0xAA is the most positive value");
        Check.equal(-32256, PcmConverter.alaw(0x2A), "A-law 0x2A is the most negative value");
        // 모든 코드를 디코딩한 값이 기준 인코더로 같은 코드로 돌아오는지 확인한다.
        for (int code = 0; code < 256; code++) {
            int expectedU = code == 0x7F ? 0xFF : code;
            Check.equal(expectedU, AudioKit.ulawEncode(PcmConverter.ulaw(code)), "mu-law round trip of " + code);
            Check.equal(code, AudioKit.alawEncode(PcmConverter.alaw(code)), "A-law round trip of " + code);
        }
        // 임의의 선형 값은 양자화 오차 안에서 돌아온다.
        for (int v = -32768; v < 32768; v += 97) {
            int u = PcmConverter.ulaw(AudioKit.ulawEncode(v));
            int a = PcmConverter.alaw(AudioKit.alawEncode(v));
            int tol = Math.abs(v) / 16 + 40;
            Check.isTrue(Math.abs(u - v) <= tol, "mu-law approximates " + v + " got " + u);
            Check.isTrue(Math.abs(a - v) <= tol, "A-law approximates " + v + " got " + a);
        }
    }

    /**
     * Float clipping rounds half up, clips and maps NaN to silence; integer narrowing shifts, rounds and clips.
     * <p>
     * float 변환은 .5 를 올리고 자르며 NaN 은 무음으로 한다. 정수 줄이기는 밀고 반올림하고 자른다.
     */
    public void testNarrowing() {
        Check.equal(0, PcmConverter.clip16(0f), "zero");
        Check.equal(16384, PcmConverter.clip16(0.5f), "half scale");
        Check.equal(-16384, PcmConverter.clip16(-0.5f), "negative half scale");
        Check.equal(32767, PcmConverter.clip16(1.0f), "full scale clips to 32767");
        Check.equal(-32768, PcmConverter.clip16(-1.0f), "negative full scale");
        Check.equal(32767, PcmConverter.clip16(100f), "overload");
        Check.equal(-32768, PcmConverter.clip16(-100f), "negative overload");
        Check.equal(0, PcmConverter.clip16(Float.NaN), "NaN");
        Check.equal(1, PcmConverter.clip16(0.5f / 32768f), "half a step rounds up");
        Check.equal(0, PcmConverter.clip16(0.49f / 32768f), "just below half a step");
        Check.equal(100 << 8, PcmConverter.narrow(100, 8), "8 bits are shifted up");
        Check.equal(-5 << 12, PcmConverter.narrow(-5, 4), "4 bits are shifted up");
        Check.equal(100, PcmConverter.narrow(100 * 256 + 127, 24), "24 bits round down below half");
        Check.equal(101, PcmConverter.narrow(100 * 256 + 128, 24), "24 bits round up at half");
        Check.equal(-100, PcmConverter.narrow(-100 * 256 + 127, 24), "negative below half");
        Check.equal(32767, PcmConverter.narrow(8388607, 24), "24-bit maximum clips");
        Check.equal(-32768, PcmConverter.narrow(-8388608, 24), "24-bit minimum");
        Check.equal(32767, PcmConverter.narrow(0x7FFFFFFFL, 32), "32-bit maximum clips");
        Check.equal(-32768, PcmConverter.narrow(0x80000000L - 0x100000000L, 32), "32-bit minimum");
    }

    /**
     * The mixer keeps stereo sides, distributes centre and surround channels and never exceeds full scale.
     * <p>
     * 믹서는 스테레오의 양쪽을 유지하고 중앙과 서라운드 채널을 나누어 넣으며 풀 스케일을 넘지 않는다.
     */
    public void testChannelMixer() {
        float[] out = new float[2];
        ChannelMixer m51 = ChannelMixer.forChannels(6);
        float[] frame = { 1f, 0, 0, 0, 0, 0 };
        m51.mix(frame, out);
        Check.isTrue(out[0] > 0.4f && out[1] == 0f, "front left only reaches the left side");
        float[] full = { 1f, 1f, 1f, 1f, 1f, 1f };
        m51.mix(full, out);
        Check.isTrue(out[0] <= 1.0001f && out[0] >= 0.8f && Math.abs(out[0] - out[1]) < 1e-6f, "full scale stays at full scale: " + out[0]);
        float[] lfe = { 0, 0, 0, 1f, 0, 0 };
        m51.mix(lfe, out);
        Check.equal(0f, out[0] + out[1], "the LFE channel is dropped");
        ChannelMixer m3 = ChannelMixer.forChannels(3);
        m3.mix(new float[] { 0f, 0f, 1f }, out);
        Check.isTrue(Math.abs(out[0] - out[1]) < 1e-6f && out[0] > 0.4f, "centre of a 3-channel layout reaches both sides");
        ChannelMixer m9 = ChannelMixer.forChannels(9);
        m9.mix(new float[] { 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f }, out);
        Check.isTrue(out[0] > 0.9f && out[1] == 0f, "more than eight channels alternate left and right");
        ChannelMixer back = ChannelMixer.forRoles(new int[] { ChannelMixer.BC });
        back.mix(new float[] { 1f }, out);
        Check.isTrue(Math.abs(out[0] - out[1]) < 1e-6f && out[0] > 0.9f, "a single back centre channel is normalized to full scale");
        ChannelMixer masked = ChannelMixer.forWavMask(3, 0x7);
        masked.mix(new float[] { 0f, 0f, 1f }, out);
        Check.isTrue(out[0] > 0.4f && Math.abs(out[0] - out[1]) < 1e-6f, "mask FL FR FC");
        ChannelMixer wrongMask = ChannelMixer.forWavMask(2, 0x7);
        Check.equal(2, wrongMask.getInputChannels(), "a mask with a different bit count falls back to the default layout");
    }
}
