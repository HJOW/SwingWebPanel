package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the media element source node (Web Audio API 1.22, plan item P25-04) with a fake supplier that plays a sine: the samples of
 * the media reach the destination unchanged at equal sample rates, a paused media and a media without audio give silence, a short
 * delivery is padded with silence, media from another origin without permission gives silence and is not pulled, the channels follow
 * the media, resampling with a linear interpolation follows the closed form of a slow sine (and is exact for a ratio of 2), the
 * context keeps the node processed until it is closed, and the errors.
 * <p>
 * 미디어 요소 소스 노드 시험(Web Audio API 1.22, 계획 P25-04). 사인을 재생하는 가짜 공급자를 쓴다: 샘플링 주파수가 같으면 미디어의 샘플이 목적지에 그대로
 * 닿고, 일시 정지한 미디어와 오디오가 없는 미디어는 무음이며, 적게 준 프레임은 무음으로 채워지고, 서버의 허가 없는 다른 오리진의 미디어는 무음이며 가져오지도
 * 않고, 채널은 미디어를 따르며, 선형 보간 리샘플링은 느린 사인의 닫힌 식을 따르고(비율 2 에서는 정확), 컨텍스트는 닫을 때까지 노드를 처리하며, 오류를 확인한다.
 */
public class MediaElementAudioSourceNodeTest {

    /**
     * The sample rate of the contexts of the tests.
     * <p>
     * 시험 컨텍스트의 샘플링 주파수.
     */
    private static final float RATE = 8000f;

    /**
     * A fake media element: a sine of a frequency per channel that plays when it is not paused.
     * <p>
     * 가짜 미디어 요소: 일시 정지가 아니면 재생되는, 채널마다 주파수가 있는 사인.
     */
    private static final class FakeMedia implements MediaElementPcmSource {

        /**
         * The number of channels.
         * <p>
         * 채널 수.
         */
        int channels = 1;

        /**
         * The sample rate of the media.
         * <p>
         * 미디어의 샘플링 주파수.
         */
        float rate = RATE;

        /**
         * Whether the audio may be heard.
         * <p>
         * 오디오를 들려줘도 되는지.
         */
        boolean allowed = true;

        /**
         * Whether the media is paused (no frames are delivered).
         * <p>
         * 일시 정지인지(프레임을 주지 않는다).
         */
        boolean paused;

        /**
         * The largest number of frames that one call delivers.
         * <p>
         * 호출 한 번이 주는 최대 프레임 수.
         */
        int limit = Integer.MAX_VALUE;

        /**
         * The number of frames that were delivered so far.
         * <p>
         * 지금까지 준 프레임 수.
         */
        long position;

        /**
         * The number of calls of pull.
         * <p>
         * pull 호출 횟수.
         */
        int pulls;

        /**
         * The frequency of channel 0 in Hz (channel 1 has twice that).
         * <p>
         * 채널 0 의 주파수(Hz). 채널 1 은 두 배.
         */
        double hz = 100.0;

        /**
         * Returns the number of channels.
         * <p>
         * 채널 수를 반환한다.
         *
         * @return the channels / 채널 수
         */
        public int getChannelCount() {
            return channels;
        }

        /**
         * Returns the sample rate of the media.
         * <p>
         * 미디어의 샘플링 주파수를 반환한다.
         *
         * @return the rate / 샘플링 주파수
         */
        public float getSampleRate() {
            return rate;
        }

        /**
         * Tells whether the audio may be heard.
         * <p>
         * 오디오를 들려줘도 되는지 알린다.
         *
         * @return the permission / 허가
         */
        public boolean isOutputAllowed() {
            return allowed;
        }

        /**
         * Delivers the next frames of the sine.
         * <p>
         * 사인의 다음 프레임을 준다.
         *
         * @param dest   the destination / 대상
         * @param frames the wanted frames / 원하는 프레임 수
         * @return the delivered frames / 준 프레임 수
         */
        public int pull(float[][] dest, int frames) {
            pulls++;
            if (paused) {
                return 0;
            }
            int n = Math.min(frames, limit);
            // 프레임마다 채널별 사인을 계산해 넣는다.
            for (int i = 0; i < n; i++) {
                // 채널마다 자기 주파수의 사인을 넣는다.
                for (int c = 0; c < channels; c++) {
                    dest[c][i] = (float) Math.sin(2 * Math.PI * hz * (c + 1) * (position + i) / rate);
                }
            }
            position += n;
            return n;
        }
    }

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public MediaElementAudioSourceNodeTest() {
    }

    /**
     * Renders a media element source into a stereo destination.
     * <p>
     * 미디어 요소 소스를 스테레오 목적지로 렌더링한다.
     *
     * @param media  the media / 미디어
     * @param frames the length / 길이
     * @return the rendered audio / 렌더링한 오디오
     */
    private static AudioBuffer render(FakeMedia media, int frames) {
        OfflineAudioContext ctx = new OfflineAudioContext(2, frames, RATE);
        MediaElementAudioSourceNode n = ctx.createMediaElementSource(media);
        n.connect(ctx.getDestination());
        return ctx.startRendering();
    }

    /**
     * At equal sample rates the samples of the media reach the destination unchanged (a mono media is up-mixed to both channels), and
     * the context pulled exactly as many frames as it rendered.
     * <p>
     * 샘플링 주파수가 같으면 미디어의 샘플이 목적지에 그대로 닿고(모노 미디어는 양쪽 채널로 업믹스), 컨텍스트는 렌더링한 만큼의 프레임을 정확히 가져온다.
     */
    public void testSamplesReachTheDestination() {
        FakeMedia m = new FakeMedia();
        AudioBuffer out = render(m, 1024);
        // 프레임마다 닫힌 식의 사인과 같은지 본다.
        for (int i = 0; i < 1024; i++) {
            double e = Math.sin(2 * Math.PI * 100.0 * i / RATE);
            Check.near(e, out.getChannelData(0)[i], 1e-6, "left " + i);
            Check.near(e, out.getChannelData(1)[i], 1e-6, "right " + i);
        }
        Check.equal(1024L, m.position, "the frames that were pulled");
        Check.equal(8L, (long) m.pulls, "one pull per quantum");
    }

    /**
     * A stereo media keeps its channels (the second channel has twice the frequency here).
     * <p>
     * 스테레오 미디어는 채널을 유지한다(여기서는 둘째 채널의 주파수가 두 배).
     */
    public void testChannelsFollowTheMedia() {
        FakeMedia m = new FakeMedia();
        m.channels = 2;
        AudioBuffer out = render(m, 512);
        // 두 채널이 각자의 주파수의 사인과 같은지 본다.
        for (int i = 0; i < 512; i++) {
            Check.near(Math.sin(2 * Math.PI * 100.0 * i / RATE), out.getChannelData(0)[i], 1e-6, "channel 0 " + i);
            Check.near(Math.sin(2 * Math.PI * 200.0 * i / RATE), out.getChannelData(1)[i], 1e-6, "channel 1 " + i);
        }
    }

    /**
     * A paused media and a media without audio give silence; a short delivery is padded with silence (the quantum that gets 40 frames
     * has silence from frame 40), and when the media resumes the sine continues.
     * <p>
     * 일시 정지한 미디어와 오디오가 없는 미디어는 무음이다. 적게 준 프레임은 무음으로 채워지고(40 프레임을 받은 quantum 은 프레임 40 부터 무음), 미디어가
     * 다시 재생되면 사인이 이어진다.
     */
    public void testPausedNoAudioAndShortDelivery() {
        FakeMedia paused = new FakeMedia();
        paused.paused = true;
        WebAudioTestKit.assertSilent("paused", render(paused, 512).getChannelData(0), 0, 512);
        FakeMedia none = new FakeMedia();
        none.channels = 0;
        WebAudioTestKit.assertSilent("no audio", render(none, 512).getChannelData(0), 0, 512);
        FakeMedia shortMedia = new FakeMedia();
        shortMedia.limit = 40;
        AudioBuffer out = render(shortMedia, 384);
        float[] o = out.getChannelData(0);
        // 각 quantum 의 앞 40 프레임은 사인이고 나머지는 무음이다.
        for (int q = 0; q < 3; q++) {
            for (int i = 0; i < 128; i++) {
                double e = i < 40 ? Math.sin(2 * Math.PI * 100.0 * (q * 40 + i) / RATE) : 0.0;
                Check.near(e, o[q * 128 + i], 1e-6, "quantum " + q + " frame " + i);
            }
        }
    }

    /**
     * Media from another origin without the permission of the server gives silence, and the node does not even pull it (the audio is
     * not consumed); allowing it later lets the sound through.
     * <p>
     * 서버의 허가 없는 다른 오리진의 미디어는 무음이고 노드는 가져가지도 않는다(오디오를 소비하지 않는다). 나중에 허용하면 소리가 지나간다.
     */
    public void testCrossOriginIsSilent() {
        FakeMedia m = new FakeMedia();
        m.allowed = false;
        OfflineAudioContext ctx = new OfflineAudioContext(2, 1024, RATE);
        MediaElementAudioSourceNode n = ctx.createMediaElementSource(m);
        n.connect(ctx.getDestination());
        // 4 quantum 은 허용되지 않은 상태로 렌더링한다.
        for (int q = 0; q < 4; q++) {
            ctx.renderNextQuantum();
        }
        Check.equal(0L, (long) m.pulls, "nothing was pulled");
        m.allowed = true;
        // 나머지 quantum 을 끝까지 렌더링한다.
        while (!ctx.isComplete()) {
            ctx.renderNextQuantum();
        }
        float[] o = ctx.finishRendering().getChannelData(0);
        WebAudioTestKit.assertSilent("before the permission", o, 0, 512);
        Check.near(Math.sin(2 * Math.PI * 100.0 * 3 / RATE), o[515], 1e-6, "after the permission the sine plays from its start");
    }

    /**
     * Resampling with a linear interpolation: a sine of 100 Hz of a media of 4000 Hz in a context of 8000 Hz (ratio 0.5) follows the
     * closed form within the error of the linear interpolation (at most <code>(2 pi f / sourceRate)^2 / 8</code> of the
     * amplitude: 0.0031 for the media of 4000 Hz); a media of 16000 Hz (ratio 2) is exact because the output frames fall on source frames; a ratio of 1.5 is checked
     * over many quanta, which verifies the carry-over of the fractional position.
     * <p>
     * 선형 보간 리샘플링: 8000 Hz 컨텍스트에서 4000 Hz 미디어의 100 Hz 사인(비율 0.5)은 선형 보간 오차(진폭의 최대 <code>(2 pi f / 원본 속도)^2 / 8</code>, 4000 Hz 미디어는 0.0031) 안에서 닫힌 식을 따른다. 16000 Hz 미디어(비율 2)는 출력 프레임이 원본 프레임에 놓이므로 정확하다. 비율 1.5 는 많은 quantum 에 걸쳐 확인하며
     * 소수 위치의 이월을 검증한다.
     */
    public void testResampling() {
        float[] rates = {4000f, 16000f, 12000f};
        // 허용 오차: 선형 보간의 최대 오차 (2 pi f / 원본 속도)^2 / 8 (4000 Hz 는 0.0031, 12000 Hz 는 0.00034)보다 조금 크게 잡는다.
        double[] tolerance = {0.0035, 1e-6, 0.0005};
        // 미디어 주파수마다 닫힌 식의 사인과 비교한다.
        for (int r = 0; r < rates.length; r++) {
            FakeMedia m = new FakeMedia();
            m.rate = rates[r];
            AudioBuffer out = render(m, 4096);
            // 필터 이력과 무관한 프레임 4 이후를 비교한다(첫 프레임은 위상 0).
            for (int i = 0; i < 4096; i++) {
                double e = Math.sin(2 * Math.PI * 100.0 * i / RATE);
                Check.near(e, out.getChannelData(0)[i], tolerance[r], "media rate " + rates[r] + " frame " + i);
            }
        }
    }

    /**
     * The context keeps the node processed from its creation (the count of pinned nodes is 1), and {@link
     * MediaElementAudioSourceNode#close()} releases it: the output is silent and the pin is gone. The supplier is required.
     * <p>
     * 컨텍스트는 노드를 만든 때부터 처리 대상으로 붙잡고(붙잡은 노드 수 1) {@link MediaElementAudioSourceNode#close()} 가 놓아 준다. 출력은 무음이고 붙잡음이
     * 사라진다. 공급자는 필수이다.
     */
    public void testCloseAndErrors() {
        FakeMedia m = new FakeMedia();
        OfflineAudioContext ctx = new OfflineAudioContext(2, 1024, RATE);
        MediaElementAudioSourceNode n = ctx.createMediaElementSource(m);
        Check.same(m, n.getMediaElementSource(), "the supplier");
        Check.equal(0L, (long) n.getNumberOfInputs(), "no inputs");
        Check.equal(1L, (long) n.getNumberOfOutputs(), "one output");
        n.connect(ctx.getDestination());
        ctx.renderNextQuantum();
        Check.equal(1L, (long) ctx.getPinnedNodeCount(), "the context keeps the node processed");
        n.close();
        ctx.renderNextQuantum();
        Check.equal(0L, (long) ctx.getPinnedNodeCount(), "close releases the node");
        long pulls = m.pulls;
        // 닫은 뒤 나머지 quantum 을 끝까지 렌더링한다.
        while (!ctx.isComplete()) {
            ctx.renderNextQuantum();
        }
        float[] o = ctx.finishRendering().getChannelData(0);
        WebAudioTestKit.assertSilent("after close", o, 256, 1024);
        Check.equal(pulls, (long) m.pulls, "a closed node pulls nothing");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                new OfflineAudioContext(1, 128, RATE).createMediaElementSource(null);
            }
        }, "a null supplier");
    }
}
