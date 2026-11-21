package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutput;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.playback.MediaPlayer;
import org.duckdns.hjow.swingwebpanel.media.playback.RecordingListener;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the connection between the player of a media element and an audio graph (plan item P25-04, P25-07): the <code>MediaElementTap</code> that the player plays into and that the
 * node of the graph pulls from, and the request of the player to reopen its output. The player is the real {@link MediaPlayer} (it decodes real WAV data on its own thread), the graph side
 * is played by the test thread, which pulls render quanta of 128 frames as a rendering thread does. Shown: the audio arrives exactly (16 bit sample divided by 32768) and in order; the
 * position of the media is the number of frames that were pulled (nobody pulls: the element stands still); pause stops the pulls and resume goes on without a loss; a seek discards the old
 * audio; a media of another rate is converted by the streaming resampler (the length is exact, the sine is the same sine, the tail arrives when the player closes the pipe); audio that the
 * page may not read is delivered as silence at the normal pace; a released tap drains in real time and the element goes on to its end; and an output that was opened for the speakers is
 * replaced by the pipe in the middle of the playing without skipping or repeating a frame.
 * <p>
 * 미디어 요소의 재생기와 오디오 그래프의 연결 시험(계획 P25-04, P25-07): 재생기가 재생해 넣고 그래프의 노드가 가져가는 <code>MediaElementTap</code> 과, 재생기가 출력을 다시 열도록 하는
 * 요청. 재생기는 진짜 {@link MediaPlayer}(자기 스레드에서 진짜 WAV 데이터를 디코딩한다)이고 그래프 쪽은 렌더링 스레드처럼 128 프레임 render quantum 을 가져가는 시험 스레드가 맡는다.
 * 보이는 것: 오디오가 정확히(16비트 표본을 32768 로 나눈 값) 순서대로 도착한다. 미디어의 위치는 가져간 프레임 수이다(아무도 가져가지 않으면 요소는 멈춘다). 일시정지는 가져가기를 멈추고
 * 재개는 손실 없이 이어 간다. 탐색은 이전 오디오를 버린다. 다른 주파수의 미디어는 스트리밍 리샘플러가 바꾼다(길이가 정확하고 사인은 같은 사인이며 재생기가 파이프를 닫을 때 꼬리가 도착한다).
 * 페이지가 읽어서는 안 되는 오디오는 보통 속도로 무음이 전달된다. 놓은 탭은 실시간으로 비워지고 요소는 끝까지 계속 간다. 스피커용으로 연 출력은 재생 도중에 프레임을 건너뛰거나 반복하지
 * 않고 파이프로 바뀐다.
 */
public class MediaElementTapTest {

    /**
     * The number of frames of a render quantum.
     * <p>
     * render quantum 의 프레임 수.
     */
    private static final int QUANTUM = 128;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MediaElementTapTest() {
    }

    /**
     * Returns the sample of the test ramp: a deterministic 16 bit value that differs from frame to frame and from channel to channel.
     * <p>
     * 시험 램프의 표본을 반환한다: 프레임마다 그리고 채널마다 다른 결정적인 16비트 값.
     *
     * @param frame   the frame / 프레임
     * @param channel the channel / 채널
     * @return the sample / 표본
     */
    private static short ramp(long frame, int channel) {
        return (short) (((frame * 7 + channel * 5003) % 20000) - 10000);
    }

    /**
     * Makes the WAV file of the ramp.
     * <p>
     * 램프의 WAV 파일을 만든다.
     *
     * @param channels the number of channels / 채널 수
     * @param rate     the sample rate / 샘플링 주파수
     * @param frames   the number of frames / 프레임 수
     * @return the file / 파일
     */
    private static byte[] rampWav(int channels, int rate, int frames) {
        byte[] data = new byte[frames * channels * 2];
        // 프레임과 채널마다 표본을 리틀 엔디언으로 쓴다.
        for (int i = 0; i < frames; i++) {
            for (int c = 0; c < channels; c++) {
                short s = ramp(i, c);
                data[(i * channels + c) * 2] = (byte) s;
                data[(i * channels + c) * 2 + 1] = (byte) (s >> 8);
            }
        }
        return AudioKit.wav(1, channels, rate, 16, data, -1);
    }

    /**
     * Makes the WAV file of a mono sine of the amplitude 0.5 whose samples are 16 bit values.
     * <p>
     * 표본이 16비트 값인 진폭 0.5 모노 사인의 WAV 파일을 만든다.
     *
     * @param rate   the sample rate / 샘플링 주파수
     * @param hz     the frequency / 주파수
     * @param frames the number of frames / 프레임 수
     * @return the file / 파일
     */
    private static byte[] sineWav(int rate, double hz, int frames) {
        byte[] data = new byte[frames * 2];
        // 프레임마다 사인의 값을 16비트로 반올림해 쓴다.
        for (int i = 0; i < frames; i++) {
            short s = (short) Math.round(16384 * Math.sin(2 * Math.PI * hz * i / rate));
            data[i * 2] = (byte) s;
            data[i * 2 + 1] = (byte) (s >> 8);
        }
        return AudioKit.wav(1, 1, rate, 16, data, -1);
    }

    /**
     * Creates a loaded player that plays into a tap.
     * <p>
     * 탭으로 재생하는 로드된 재생기를 만든다.
     *
     * @param tap  the tap / 탭
     * @param l    the listener / 리스너
     * @param file the data / 데이터
     * @return the player / 재생기
     * @throws Exception on failure / 실패 시
     */
    private static MediaPlayer load(MediaElementTap tap, RecordingListener l, byte[] file) throws Exception {
        MediaPlayer p = new MediaPlayer(tap, l);
        p.load(file, "audio/wav");
        l.await("loaded", 1);
        return p;
    }

    /**
     * Pulls render quanta, as the rendering thread of a context does, until a number of frames arrived or the time is over; the frames of channel 0 and 1 are appended to the result.
     * <p>
     * 컨텍스트의 렌더링 스레드처럼 render quantum 을 가져간다. 정해진 수의 프레임이 도착하거나 시간이 다할 때까지 반복하며, 채널 0 과 1 의 프레임을 결과에 덧붙인다.
     *
     * @param tap      the tap / 탭
     * @param channels the number of channels of the media / 미디어의 채널 수
     * @param frames   the number of frames to wait for / 기다릴 프레임 수
     * @param millis   the longest time / 최대 시간
     * @return the frames, one array per channel / 프레임. 채널마다 배열 하나
     * @throws Exception on failure / 실패 시
     */
    private static float[][] pullFrames(MediaElementTap tap, int channels, int frames, long millis) throws Exception {
        float[][] out = new float[channels][frames];
        float[][] quantum = new float[channels][QUANTUM];
        int got = 0;
        long end = System.currentTimeMillis() + millis;
        // 원하는 프레임이 모일 때까지 한 quantum 씩 가져간다. 비어 있으면 짧게 쉰다.
        while (got < frames && System.currentTimeMillis() < end) {
            int n = tap.pull(quantum, Math.min(QUANTUM, frames - got));
            // 가져온 프레임을 결과에 옮긴다.
            for (int c = 0; c < channels; c++) {
                System.arraycopy(quantum[c], 0, out[c], got, n);
            }
            got += n;
            if (n == 0) {
                Thread.sleep(1);
            }
        }
        // 모자라게 가져왔으면 가져온 만큼만 돌려준다.
        if (got < frames) {
            for (int c = 0; c < channels; c++) {
                out[c] = Arrays.copyOf(out[c], got);
            }
        }
        return out;
    }

    /**
     * The audio of a mono and a stereo media arrives exactly and in order, whatever the way that it is cut into render quanta; the player reports the end when the graph consumed the last
     * frame, and the tap tells the channel count of the media.
     * <p>
     * 모노와 스테레오 미디어의 오디오는 render quantum 으로 어떻게 나뉘든 정확히 순서대로 도착한다. 재생기는 그래프가 마지막 프레임을 소비했을 때 끝을 알리고, 탭은 미디어의 채널 수를 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAudioArrivesExactlyAndInOrder() throws Exception {
        // 모노와 스테레오를 차례로 확인한다.
        for (int channels = 1; channels <= 2; channels++) {
            MediaElementTap tap = new MediaElementTap(8000f);
            Check.equal(0, tap.getChannelCount(), "before the player opened its output there is no audio");
            Check.equal(0, tap.pull(new float[1][QUANTUM], QUANTUM), "nothing to pull before the player opened its output");
            RecordingListener l = new RecordingListener();
            MediaPlayer p = load(tap, l, rampWav(channels, 8000, 5000));
            p.play();
            float[][] got = pullFrames(tap, channels, 5000, 20000);
            Check.equal(5000, got[0].length, "all frames arrived (" + channels + " channels)");
            Check.equal(channels, tap.getChannelCount(), "the tap tells the channel count");
            // 모든 표본이 16비트 값을 32768 로 나눈 값과 정확히 같아야 한다.
            for (int i = 0; i < 5000; i++) {
                for (int c = 0; c < channels; c++) {
                    if (got[c][i] != ramp(i, c) / 32768f) {
                        Check.fail("frame " + i + " channel " + c + ": " + got[c][i] + " instead of " + ramp(i, c) / 32768f);
                    }
                }
            }
            l.await("ended", 1);
            Check.equal(1, l.count("ended"), "the end is reported once, after the graph consumed the last frame");
            Check.equal(5000L, tap.getPulledFrames(), "the tap delivered every frame once");
            Check.equal(5000L, tap.getWrittenFrames(), "and the player wrote every frame once");
            p.close();
        }
    }

    /**
     * The position of the media is the number of frames that the graph pulled: nobody pulls, the element stands still (the ring fills up, then the player waits); every pull moves it by
     * exactly the frames that were pulled.
     * <p>
     * 미디어의 위치는 그래프가 가져간 프레임 수이다: 아무도 가져가지 않으면 요소는 멈춘다(링이 차고 재생기가 기다린다). 가져갈 때마다 가져간 프레임만큼 정확히 움직인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPositionIsWhatTheGraphPulled() throws Exception {
        MediaElementTap tap = new MediaElementTap(8000f);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = load(tap, l, rampWav(1, 8000, 40000));
        p.play();
        Thread.sleep(400);
        Check.near(0.0, p.getPosition(), 1e-9, "nobody pulled: the position did not move although the player is playing");
        Check.isTrue(tap.getWrittenFrames() >= MediaElementTap.MIN_CAPACITY_FRAMES, "the player filled the ring and then waited: " + tap.getWrittenFrames());
        Check.isTrue(tap.getWrittenFrames() <= 4 * MediaElementTap.MIN_CAPACITY_FRAMES, "the player did not run away with the file: " + tap.getWrittenFrames());
        float[][] first = pullFrames(tap, 1, 1000, 10000);
        Check.equal(1000, first[0].length, "1000 frames were pulled");
        Check.near(1000 / 8000.0, p.getPosition(), 1e-9, "the position is the number of frames that were pulled");
        float[][] more = pullFrames(tap, 1, 2500, 10000);
        Check.equal(2500, more[0].length, "2500 more frames were pulled");
        Check.near(3500 / 8000.0, p.getPosition(), 1e-9, "and the position moved by the same amount");
        Check.equal(ramp(1000, 0) / 32768f, more[0][0], "the second pull goes on where the first one stopped");
        p.close();
    }

    /**
     * Pausing the element stops the pulls at once (the queued frames stay) and resuming goes on without losing or repeating a frame.
     * <p>
     * 요소를 일시정지하면 가져가기가 즉시 멈추고(큐의 프레임은 남는다) 재개하면 프레임을 잃거나 반복하지 않고 이어 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPauseStopsThePullsAndResumeContinues() throws Exception {
        MediaElementTap tap = new MediaElementTap(8000f);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = load(tap, l, rampWav(1, 8000, 20000));
        p.play();
        float[][] a = pullFrames(tap, 1, 1500, 10000);
        p.pause();
        float[][] quantum = new float[1][QUANTUM];
        Check.equal(0, tap.pull(quantum, QUANTUM), "a paused element delivers nothing although the ring still holds audio");
        Thread.sleep(100);
        Check.equal(0, tap.pull(quantum, QUANTUM), "still nothing");
        Check.near(1500 / 8000.0, p.getPosition(), 1e-9, "the position stands still");
        p.play();
        float[][] b = pullFrames(tap, 1, 2000, 10000);
        Check.equal(2000, b[0].length, "the element plays on");
        // 일시정지 전후의 프레임이 끊기거나 반복되지 않고 이어져야 한다.
        for (int i = 0; i < 2000; i++) {
            if (b[0][i] != ramp(1500 + i, 0) / 32768f) {
                Check.fail("after the pause frame " + i + " is " + b[0][i] + " but frame " + (1500 + i) + " of the media is " + ramp(1500 + i, 0) / 32768f);
            }
        }
        Check.equal(1500, a[0].length, "before the pause 1500 frames");
        p.close();
    }

    /**
     * A seek discards the audio of the old position (the ring is flushed) and the next pulls deliver the audio of the new position from its first frame.
     * <p>
     * 탐색은 이전 위치의 오디오를 버리고(링을 비운다) 다음 가져가기는 새 위치의 오디오를 첫 프레임부터 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeekDiscardsTheOldAudio() throws Exception {
        MediaElementTap tap = new MediaElementTap(8000f);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = load(tap, l, rampWav(1, 8000, 30000));
        p.play();
        pullFrames(tap, 1, 800, 10000);
        p.seek(2.0);
        l.await("seeked", 1);
        float[][] after = pullFrames(tap, 1, 300, 10000);
        Check.equal(300, after[0].length, "audio goes on after the seek");
        // 탐색한 위치(16000 프레임)부터의 표본이어야 한다.
        for (int i = 0; i < 300; i++) {
            if (after[0][i] != ramp(16000 + i, 0) / 32768f) {
                Check.fail("after the seek frame " + i + " is " + after[0][i] + " but frame " + (16000 + i) + " was expected: " + ramp(16000 + i, 0) / 32768f);
            }
        }
        Check.near((16000 + 300) / 8000.0, p.getPosition(), 1e-9, "the position follows the pulls from the new position");
        p.close();
    }

    /**
     * A media of another sample rate is converted: 8000 Hz to 12000 Hz and 8000 Hz to 6000 Hz. The number of frames is the exact length of the conversion (the tail of the converter
     * arrives when the player closes the pipe at the end), the sine is the same sine at the new rate inside the signal (the filter is better than 0.001 there), and the position of
     * the media stays in the units of the media.
     * <p>
     * 다른 샘플링 주파수의 미디어는 변환된다: 8000 Hz 에서 12000 Hz, 8000 Hz 에서 6000 Hz. 프레임 수는 변환의 정확한 길이이고(변환기의 꼬리는 재생기가 끝에서 파이프를 닫을 때 도착한다),
     * 신호 안에서 사인은 새 주파수의 같은 사인이다(필터가 거기서 0.001 보다 좋다). 미디어의 위치는 미디어의 단위로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnotherSampleRateIsConverted() throws Exception {
        int[] contextRates = {12000, 6000};
        // 올리는 변환과 내리는 변환을 차례로 확인한다.
        for (int k = 0; k < contextRates.length; k++) {
            int rate = contextRates[k];
            MediaElementTap tap = new MediaElementTap(rate);
            RecordingListener l = new RecordingListener();
            MediaPlayer p = load(tap, l, sineWav(8000, 400, 8000));
            p.play();
            long expected = (8000L * rate + 7999) / 8000;
            float[][] got = pullFrames(tap, 1, (int) expected, 20000);
            Check.equal(expected, got[0].length, "the frames that the conversion owes up to now (" + rate + " Hz)");
            l.await("ended", 1);
            // 끝에서 재생기가 파이프를 닫은 뒤 변환기의 꼬리가 남아 있으면 마저 가져온다.
            float[][] tail = pullFrames(tap, 1, 4000, 300);
            Check.equal(0, tail[0].length, "no frame beyond the exact length of the conversion");
            Check.equal(expected, tap.getPulledFrames(), "the delivered frames are the exact length of the conversion at " + rate + " Hz");
            // 신호의 안쪽(양 끝의 필터 응답을 지난 부분)에서 사인과 같아야 한다.
            double worst = 0;
            for (int i = 400; i < got[0].length - 400; i++) {
                double want = 0.5 * Math.sin(2 * Math.PI * 400 * i / rate);
                worst = Math.max(worst, Math.abs(got[0][i] - want));
            }
            Check.isTrue(worst < 1e-3, "the converted sine is the sine at " + rate + " Hz: the largest error is " + worst);
            Check.near(1.0, p.getPosition(), 1e-9, "the position of the media is in its own units: the media is one second long");
            p.close();
        }
    }

    /**
     * The conversions between the two common rates (44100 Hz and 48000 Hz, both ways, mono and stereo) deliver the exact length, which is the length of half a second at the new rate, and the
     * position of the media is half a second at the end.
     * <p>
     * 흔한 두 주파수 사이의 변환(44100 Hz 와 48000 Hz, 양방향, 모노와 스테레오)은 새 주파수의 0.5 초 길이인 정확한 길이를 전달하고, 끝에서 미디어의 위치는 0.5 초이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTheCommonRatesDeliverTheExactLength() throws Exception {
        int[][] pairs = {{44100, 48000}, {48000, 44100}};
        // 두 방향의 변환과 모노, 스테레오를 차례로 확인한다.
        for (int k = 0; k < pairs.length; k++) {
            for (int channels = 1; channels <= 2; channels++) {
                int mediaRate = pairs[k][0];
                int contextRate = pairs[k][1];
                MediaElementTap tap = new MediaElementTap(contextRate);
                RecordingListener l = new RecordingListener();
                MediaPlayer p = load(tap, l, rampWav(channels, mediaRate, mediaRate / 2));
                p.play();
                long expected = (long) contextRate / 2;
                float[][] got = pullFrames(tap, channels, (int) expected, 10000);
                Check.equal(expected, got[0].length, "half a second from " + mediaRate + " Hz to " + contextRate + " Hz, " + channels + " channels");
                l.await("ended", 1);
                float[][] late = pullFrames(tap, channels, 200, 300);
                Check.equal(0, late[0].length, "and not a frame more");
                Check.near(0.5, p.getPosition(), 1e-9, "the position of the media is half a second at the end");
                p.close();
            }
        }
    }

    /**
     * The converted audio is the very result of the resampler of the decoding path, from the first frame to the last (the converter is streaming, so the way that the quanta cut the
     * signal does not matter: the result is bit for bit the same as the conversion of the whole signal at once).
     * <p>
     * 변환된 오디오는 디코딩 경로의 리샘플러의 결과 그 자체이다(첫 프레임부터 마지막 프레임까지). 변환기는 스트리밍이므로 quantum 이 신호를 어떻게 나누든 상관없다: 결과는 신호 전체를 한꺼번에
     * 변환한 것과 비트 단위로 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConvertedAudioIsTheResamplerResultFromTheFirstFrame() throws Exception {
        int frames = 5000;
        float[] input = new float[frames];
        // 입력은 16비트 표본을 32768 로 나눈 값이다.
        for (int i = 0; i < frames; i++) {
            input[i] = (short) Math.round(16384 * Math.sin(2 * Math.PI * 440 * i / 44100.0)) / 32768f;
        }
        float[] want = org.duckdns.hjow.swingwebpanel.media.webaudio.decode.Resampler.resample(input, 44100, 48000);
        MediaElementTap tap = new MediaElementTap(48000f);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = load(tap, l, sineWav(44100, 440, frames));
        p.play();
        float[][] got = pullFrames(tap, 1, want.length, 10000);
        Check.equal(want.length, got[0].length, "the length of the conversion");
        // 변환 결과가 참조 결과와 모든 프레임에서 같아야 한다.
        for (int i = 0; i < want.length; i++) {
            if (got[0][i] != want[i]) {
                Check.fail("frame " + i + " of the converted audio is " + got[0][i] + " but the resampler gives " + want[i]);
            }
        }
        p.close();
    }

    /**
     * Audio that the page may not read is consumed at the normal pace and delivered as silence: the element is not stalled (its position and its end follow the pulls) and the page
     * learns nothing of the samples.
     * <p>
     * 페이지가 읽어서는 안 되는 오디오는 보통 속도로 소비되고 무음으로 전달된다: 요소는 멈추지 않으며(위치와 끝이 가져가기를 따른다) 페이지는 표본에 대해 아무것도 알 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAudioThatThePageMayNotReadIsSilent() throws Exception {
        MediaElementTap tap = new MediaElementTap(8000f);
        tap.setOutputAllowed(false);
        Check.isFalse(tap.isOutputAllowedForPage(), "the host forbids the page to read the audio");
        RecordingListener l = new RecordingListener();
        MediaPlayer p = load(tap, l, rampWav(1, 8000, 3000));
        p.play();
        float[][] got = pullFrames(tap, 1, 3000, 10000);
        Check.equal(3000, got[0].length, "the frames are consumed at the normal pace");
        // 모든 표본이 정확히 0 이어야 한다.
        for (int i = 0; i < 3000; i++) {
            if (got[0][i] != 0f) {
                Check.fail("frame " + i + " is not silent: " + got[0][i]);
            }
        }
        l.await("ended", 1);
        Check.near(3000 / 8000.0, p.getPosition(), 1e-9, "the element reached its end");
        p.close();
    }

    /**
     * A released tap (its context was closed) delivers nothing but the element is not stalled: the pipe drains by the wall clock, so the position moves in real time and the element ends.
     * <p>
     * 놓은 탭(컨텍스트가 닫혔다)은 아무것도 주지 않지만 요소는 멈추지 않는다: 파이프는 벽시계로 비워지므로 위치가 실시간으로 움직이고 요소는 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReleasedTapDrainsInRealTime() throws Exception {
        MediaElementTap tap = new MediaElementTap(8000f);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = load(tap, l, rampWav(1, 8000, 8000));
        tap.release();
        Check.isTrue(tap.isReleased(), "released");
        long t0 = System.nanoTime();
        p.play();
        l.await("ended", 1);
        long millis = (System.nanoTime() - t0) / 1000000L;
        Check.isTrue(millis >= 700, "the one second long element took " + millis + " ms: it plays in real time (the first 0.15 s fit in the ring)");
        Check.isTrue(millis < 4000, "and it ends in about a second: " + millis + " ms");
        Check.equal(0, tap.pull(new float[1][QUANTUM], QUANTUM), "a released tap delivers nothing");
        Check.equal(0L, tap.getPulledFrames(), "and nothing was delivered");
        p.close();
    }

    /**
     * A player that has an output open for the speakers is asked to reopen it: the sound that was queued in the old output is decoded again for the new one, so that the frames that the
     * speakers played plus the frames that the pipe delivers are the media from its first frame to its end, without a skipped or a repeated frame. The old output is closed (once), also
     * when the player is paused at that moment, and the next output is opened through the factory.
     * <p>
     * 스피커용으로 출력을 열어 둔 재생기는 그것을 다시 열도록 요청받는다: 이전 출력에 큐에 있던 소리를 새 출력을 위해 다시 디코딩하므로, 스피커가 재생한 프레임과 파이프가 전달하는 프레임을
     * 이으면 건너뛰거나 반복한 프레임 없이 미디어의 첫 프레임부터 끝까지이다. 이전 출력은 (한 번) 닫히며, 그 순간 재생기가 일시정지 상태여도 그렇고, 다음 출력은 팩터리를 통해 연다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReopenSwitchesFromTheSpeakersToThePipeWithoutAGap() throws Exception {
        // 재생 중일 때(0)와 일시정지 중일 때(1)의 전환을 차례로 확인한다.
        for (int round = 0; round < 2; round++) {
            final MediaElementTap tap = new MediaElementTap(8000f);
            final FakeOutputs speakers = new FakeOutputs(2000);
            final boolean[] toPipe = new boolean[1];
            AudioOutputFactory factory = new AudioOutputFactory() {
                /**
                 * Opens the pipe once the test switched, the speakers before.
                 * <p>
                 * 시험이 바꾼 뒤에는 파이프를, 그 전에는 스피커를 연다.
                 *
                 * @param sampleRate the sample rate / 샘플링 주파수
                 * @param channels   the channels / 채널 수
                 * @return the output / 출력
                 * @throws MediaException on failure / 실패 시
                 */
                public AudioOutput open(int sampleRate, int channels) throws MediaException {
                    return toPipe[0] ? tap.open(sampleRate, channels) : speakers.open(sampleRate, channels);
                }
            };
            RecordingListener l = new RecordingListener();
            MediaPlayer p = new MediaPlayer(factory, l);
            p.load(rampWav(1, 8000, 9000), "audio/wav");
            l.await("loaded", 1);
            p.play();
            FakeOutputs.Out old = speakers.await(0);
            long played = 0;
            long end = System.currentTimeMillis() + 10000;
            // 스피커가 1000 프레임을 재생하는 척하고, 큐에 프레임이 남아 있는 상태를 만든다.
            while (played < 1000 && System.currentTimeMillis() < end) {
                played += old.consume(1000 - played);
                Thread.sleep(2);
            }
            // 재생기가 큐를 다시 채울 때까지 기다린다.
            while (old.getQueuedFrames() < 1500 && System.currentTimeMillis() < end) {
                Thread.sleep(2);
            }
            Check.isTrue(old.getQueuedFrames() > 0, "the speakers hold audio that was not played yet");
            if (round == 1) {
                p.pause();
            }
            toPipe[0] = true;
            p.reopenOutput();
            if (round == 1) {
                // 일시정지 중에도 이전 출력은 닫혀야 한다.
                long until = System.currentTimeMillis() + 10000;
                while (old.closeCalls() == 0 && System.currentTimeMillis() < until) {
                    Thread.sleep(2);
                }
                Check.equal(1, old.closeCalls(), "the output of the speakers was closed although the player is paused");
                Check.equal(0, tap.getOpenCount(), "the pipe is opened at the next play");
                p.play();
            }
            float[][] got = pullFrames(tap, 1, 9000 - (int) played, 20000);
            Check.equal(9000 - (int) played, got[0].length, "the pipe delivers the rest of the media (round " + round + ")");
            // 스피커가 재생한 프레임 + 파이프의 프레임이 미디어의 처음부터 끝까지여야 한다.
            for (int i = 0; i < got[0].length; i++) {
                if (got[0][i] != ramp(played + i, 0) / 32768f) {
                    Check.fail("round " + round + ": the frame after the " + played + " that the speakers played is wrong at " + i + ": " + got[0][i] + " instead of "
                            + ramp(played + i, 0) / 32768f);
                }
            }
            byte[] heard = old.written();
            // 스피커가 실제로 재생한 앞부분이 미디어의 처음 프레임들이어야 한다.
            for (int i = 0; i < played; i++) {
                short s = (short) ((heard[i * 2] & 0xff) | (heard[i * 2 + 1] << 8));
                if (s != ramp(i, 0)) {
                    Check.fail("the speakers played frame " + i + " wrongly");
                }
            }
            l.await("ended", 1);
            Check.equal(1, old.closeCalls(), "the output of the speakers was closed exactly once");
            Check.equal(1, tap.getOpenCount(), "exactly one pipe was opened");
            p.close();
        }
    }
}
