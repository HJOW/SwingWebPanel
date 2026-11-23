package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the silent output (plan item P25-07): the virtual device plays at the pace of the clock (a clock that the test controls), the
 * queue drains to 0 when the writer is too slow and the device then waits for new data, stop keeps the queue and flush discards it, and a
 * write waits while the output is full: at least as long as the audio lasts, which is what makes the context time run at real speed (a
 * loose lower bound with the real clock, because only the lower bound does not depend on the load of the machine), and a stop or a close
 * wakes a waiting writer.
 * <p>
 * 무음 출력 시험(계획 P25-07): 가상 장치는 시계의 속도로 재생하고(시험이 제어하는 시계), 쓰는 쪽이 너무 느리면 큐가 0 으로 비워지고 장치는 새 데이터를 기다리며,
 * 정지는 큐를 유지하고 비움은 큐를 버리며, 출력이 가득 차 있으면 쓰기가 기다린다: 적어도 오디오가 지속되는 시간만큼 기다리며 이것이 컨텍스트 시간을 실제
 * 속도로 흐르게 한다(실제 시계로는 느슨한 아래쪽 한계만 본다. 아래쪽 한계만이 컴퓨터의 부하에 의존하지 않기 때문이다). 정지나 닫기는 기다리는 쓰는 쪽을 깨운다.
 */
public class NullOutputTest {

    /**
     * A clock that only moves when the test says so.
     * <p>
     * 시험이 시킬 때만 움직이는 시계.
     */
    private static final class TestClock implements NullOutput.Clock {

        /**
         * The reading in nanoseconds.
         * <p>
         * 값(나노초).
         */
        private long now = 5000000000L;

        /**
         * Returns the reading.
         * <p>
         * 값을 반환한다.
         *
         * @return the reading / 값
         */
        public long nanoTime() {
            return now;
        }

        /**
         * Moves the clock forward.
         * <p>
         * 시계를 앞으로 보낸다.
         *
         * @param millis the milliseconds / 밀리초
         */
        void advance(long millis) {
            now += millis * 1000000L;
        }
    }

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public NullOutputTest() {
    }

    /**
     * Creates silent audio of a number of frames.
     * <p>
     * 일정 프레임의 무음 오디오를 만든다.
     *
     * @param frames the frames / 프레임 수
     * @return the audio / 오디오
     */
    private static float[][] audio(int frames) {
        return new float[2][frames];
    }

    /**
     * At 1000 Hz one millisecond is one frame: the started device plays one frame per millisecond, an idle device plays nothing, and a
     * device that ran dry does not count the time that it was dry, so frames that arrive later are played from their arrival.
     * <p>
     * 1000 Hz 에서는 1 밀리초가 1 프레임이다: 시작된 장치는 밀리초마다 프레임 하나를 재생하고, 쉬는 장치는 아무것도 재생하지 않으며, 비워진 장치는 빈 시간을
     * 세지 않으므로 나중에 도착한 프레임은 도착한 때부터 재생된다.
     */
    public void testQueueDrainsAtTheClockSpeed() {
        TestClock clock = new TestClock();
        NullOutput out = new NullOutput(1000, 2, 1000, clock);
        out.write(audio(600), 600);
        Check.equal(600L, out.getQueuedFrames(), "queued before the start");
        clock.advance(500);
        Check.equal(600L, out.getQueuedFrames(), "a stopped device plays nothing");
        out.start();
        clock.advance(200);
        Check.equal(400L, out.getQueuedFrames(), "200 ms played");
        clock.advance(100);
        Check.equal(300L, out.getQueuedFrames(), "100 ms more");
        out.write(audio(500), 500);
        Check.equal(800L, out.getQueuedFrames(), "500 frames added");
        clock.advance(1000);
        Check.equal(0L, out.getQueuedFrames(), "everything was played, the queue is empty (an underrun)");
        clock.advance(5000);
        Check.equal(0L, out.getQueuedFrames(), "an empty device stays empty");
        out.write(audio(300), 300);
        Check.equal(300L, out.getQueuedFrames(), "300 frames arrive");
        clock.advance(100);
        Check.equal(200L, out.getQueuedFrames(), "the time while the device was dry is not played");
    }

    /**
     * Stop keeps the queue (the clock does not play it), start continues, and flush discards it.
     * <p>
     * 정지는 큐를 유지하고(시계가 재생하지 않는다), 시작은 이어 가고, 비움은 큐를 버린다.
     */
    public void testStopKeepsTheQueueAndFlushDiscardsIt() {
        TestClock clock = new TestClock();
        NullOutput out = new NullOutput(1000, 2, 1000, clock);
        out.write(audio(500), 500);
        out.start();
        clock.advance(100);
        Check.equal(400L, out.getQueuedFrames(), "100 ms played");
        out.stop();
        clock.advance(1000);
        Check.equal(400L, out.getQueuedFrames(), "a stopped device keeps the queue");
        out.start();
        clock.advance(100);
        Check.equal(300L, out.getQueuedFrames(), "playing continues");
        out.flush();
        Check.equal(0L, out.getQueuedFrames(), "flushed");
        out.write(audio(100), 100);
        Check.equal(100L, out.getQueuedFrames(), "writing after a flush");
        Check.isFalse(out.isAudible(), "a silent output is not audible");
        Check.equal(1000L, out.getSampleRate(), "sample rate");
        Check.equal(2L, out.getChannels(), "channels");
        Check.equal(1000L, out.getBufferFrames(), "capacity");
        Check.contains(out.getDescription(), "silent", "description");
    }

    /**
     * A stopped output that is full cannot be emptied by anything, so the rest of a write is dropped at once instead of waiting for ever.
     * <p>
     * 멈춘 채 가득 찬 출력은 아무것도 비울 수 없으므로 쓰기의 나머지는 영원히 기다리지 않고 곧바로 버려진다.
     */
    public void testStoppedAndFullOutputDropsTheRest() {
        TestClock clock = new TestClock();
        NullOutput out = new NullOutput(1000, 2, 1000, clock);
        out.write(audio(1500), 1500);
        Check.equal(1000L, out.getQueuedFrames(), "only the capacity was accepted");
    }

    /**
     * With the real clock a started output takes at least the duration of the audio (minus its own buffer) to accept it: 8 blocks of
     * 100 ms into a buffer of 100 ms take at least 700 ms (0.6 s is required, so that a coarse timer does not fail the test). Only a lower
     * bound is checked because the upper bound depends on the load of the machine.
     * <p>
     * 실제 시계로는 시작된 출력이 오디오를 받는 데 적어도 오디오의 길이(출력 자신의 버퍼를 뺀)만큼 걸린다: 100 ms 버퍼에 100 ms 분량 8개를 쓰면 적어도 700 ms 가 걸린다
     * (거친 타이머가 시험을 실패시키지 않도록 0.6 초를 요구한다). 위쪽 한계는 컴퓨터의 부하에 의존하므로 아래쪽 한계만 확인한다.
     */
    public void testWriteWaitsForTheDevice() {
        NullOutput out = new NullOutput(8000, 2, 800);
        out.start();
        long begin = System.nanoTime();
        // 100 ms 분량을 여덟 번 쓴다. 가득 차면 장치가 재생하는 시간만큼 기다린다.
        for (int i = 0; i < 8; i++) {
            out.write(audio(800), 800);
        }
        double seconds = (System.nanoTime() - begin) / 1e9;
        out.close();
        Check.isTrue(seconds >= 0.6, "8 x 100 ms of audio were accepted in " + seconds + " s");
        Check.isTrue(seconds < 30.0, "the writes did not wait for ever: " + seconds + " s");
    }

    /**
     * A writer that waits for room is woken by a close and by a stop, which is how a context ends its rendering thread at once.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCloseAndStopWakeAWriter() throws Exception {
        // 닫기와 정지 두 방법으로 기다리는 쓰는 쪽이 깨어나는지 확인한다.
        for (int round = 0; round < 2; round++) {
            final NullOutput out = new NullOutput(100, 2, 100);
            out.start();
            final boolean[] finished = new boolean[1];
            Thread writer = new Thread(new Runnable() {
                public void run() {
                    // 1000 프레임은 100 Hz 에서 10 초가 걸린다. 깨우지 않으면 끝나지 않는다.
                    out.write(audio(1000), 1000);
                    finished[0] = true;
                }
            });
            writer.setDaemon(true);
            writer.start();
            Thread.sleep(150);
            Check.isTrue(writer.isAlive(), "the writer waits (round " + round + ")");
            if (round == 0) {
                out.close();
            } else {
                out.stop();
            }
            writer.join(3000);
            Check.isFalse(writer.isAlive(), "the writer was woken (round " + round + ")");
            Check.isTrue(finished[0], "the write returned (round " + round + ")");
            out.close();
        }
    }

    /**
     * The constructor rejects values that are not positive.
     * <p>
     * 생성자는 양수가 아닌 값을 거부한다.
     */
    public void testRejectsBadArguments() {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                new NullOutput(0, 2, 100);
            }
        }, "rate 0");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                new NullOutput(8000, 0, 100);
            }
        }, "0 channels");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                new NullOutput(8000, 2, 0);
            }
        }, "buffer 0");
    }
}
