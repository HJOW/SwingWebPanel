package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The switch of the output of a running context (plan item P25-07, the <code>setSinkId</code> of the script layer): <code>setOutputFactory</code> followed by <code>restart()</code> makes the
 * driver end its segment, release the output, and open the next output through the new factory at once, and the signal goes on at the same time; <code>restart()</code> does nothing while the
 * driver is paused or closed (the next start uses the new factory anyway) and never opens an output by itself; the silent factory plays nowhere and keeps the time running.
 * <p>
 * 실행 중인 컨텍스트의 출력 전환(계획 P25-07, 스크립트 계층의 <code>setSinkId</code>): <code>setOutputFactory</code> 와 <code>restart()</code> 를 차례로 부르면 드라이버가 구간을 끝내고 출력을
 * 놓고 새 팩터리로 곧바로 다음 출력을 열며 신호는 같은 시간에서 이어진다. <code>restart()</code> 는 드라이버가 일시정지나 닫힘이면 아무것도 하지 않고(다음 시작이 어차피 새 팩터리를 쓴다)
 * 스스로 출력을 열지 않는다. 무음 팩터리는 어디로도 재생하지 않으며 시간은 계속 흐르게 한다.
 */
public class OutputRenderDriverSwitchTest {

    /**
     * The sample rate of the contexts.
     * <p>
     * 컨텍스트의 샘플링 주파수.
     */
    private static final float RATE = 48000f;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OutputRenderDriverSwitchTest() {
    }

    /**
     * Creates a context that plays a constant 0.25 to the outputs of a factory, started.
     * <p>
     * 팩터리의 출력으로 상수 0.25 를 재생하는 컨텍스트를 만들고 시작한다.
     *
     * @param factory the factory / 팩터리
     * @return the context / 컨텍스트
     */
    private static RealtimeAudioContext start(ManualOutputFactory factory) {
        RealtimeAudioContext ctx = OutputRenderDriver.createContext(RATE, 0.01, factory, "swingwebpanel-audio-render-switch");
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(0.25f);
        c.connect(ctx.getDestination());
        c.start(0);
        ctx.resume();
        return ctx;
    }

    /**
     * Waits until an output holds a number of frames.
     * <p>
     * 출력이 정해진 수의 프레임을 가질 때까지 기다린다.
     *
     * @param out    the output / 출력
     * @param frames the number of frames / 프레임 수
     * @throws Exception on failure / 실패 시
     */
    private static void awaitFrames(ManualOutput out, long frames) throws Exception {
        long end = System.currentTimeMillis() + 10000;
        // 출력이 충분한 프레임을 받을 때까지 짧게 쉬며 기다린다.
        while (out.getWrittenFrames() < frames) {
            if (System.currentTimeMillis() > end) {
                Check.fail("the output got " + out.getWrittenFrames() + " of " + frames + " frames");
            }
            Thread.sleep(2);
        }
    }

    /**
     * A running context that is told to switch to a new factory releases the old output at once and plays to the new one from the frame at which it stopped; the old output is not written
     * to any more and the new one starts with the signal.
     * <p>
     * 새 팩터리로 바꾸라는 말을 들은 실행 중인 컨텍스트는 이전 출력을 곧바로 놓고 멈춘 프레임에서 새 출력으로 재생한다. 이전 출력에는 더 쓰지 않고 새 출력은 신호로 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRestartSwitchesTheRunningOutput() throws Exception {
        ManualOutputFactory first = new ManualOutputFactory();
        first.setAutoConsume(true);
        ManualOutputFactory second = new ManualOutputFactory();
        second.setAutoConsume(true);
        RealtimeAudioContext ctx = start(first);
        OutputRenderDriver driver = (OutputRenderDriver) ctx.getDriver();
        try {
            ManualOutput old = first.waitFor(1, 10000);
            awaitFrames(old, 1000);
            driver.setOutputFactory(second);
            Check.equal(1, first.count(), "setting the factory alone does not touch the running segment");
            driver.restart();
            ManualOutput next = second.waitFor(1, 10000);
            Check.notNull(next, "the new factory served the next segment");
            awaitFrames(next, 1000);
            long end = System.currentTimeMillis() + 5000;
            // 이전 출력이 닫힐 때까지 기다린다.
            while (!old.isClosed() && System.currentTimeMillis() < end) {
                Thread.sleep(2);
            }
            Check.isTrue(old.isClosed(), "the old output was released");
            long frozen = old.getWrittenFrames();
            Thread.sleep(100);
            Check.equal(frozen, old.getWrittenFrames(), "and is not written to any more");
            Check.equal(0.25f, next.copyChannel(0)[0], "the signal is there from the first frame of the new output");
            Check.equal(1, first.count(), "the old factory was asked once");
            Check.equal(2, driver.getSegmentCount(), "two segments");
        } finally {
            ctx.close();
        }
    }

    /**
     * <code>restart()</code> does nothing while the driver is paused (the next start uses the factory that was set), before the first start and after the close: no output is opened by it.
     * <p>
     * <code>restart()</code> 는 드라이버가 일시정지된 동안, 첫 시작 전에, 닫은 뒤에는 아무것도 하지 않는다. 그것이 출력을 여는 일은 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRestartDoesNothingWhenThereIsNothingToSwitch() throws Exception {
        ManualOutputFactory first = new ManualOutputFactory();
        first.setAutoConsume(true);
        ManualOutputFactory second = new ManualOutputFactory();
        second.setAutoConsume(true);
        RealtimeAudioContext ctx = OutputRenderDriver.createContext(RATE, 0.01, first, "swingwebpanel-audio-render-switch");
        OutputRenderDriver driver = (OutputRenderDriver) ctx.getDriver();
        driver.restart();
        Thread.sleep(50);
        Check.equal(0, first.count(), "before the first start nothing is opened");
        ctx.resume();
        ManualOutput one = first.waitFor(1, 10000);
        awaitFrames(one, 500);
        ctx.suspend();
        driver.setOutputFactory(second);
        driver.restart();
        Thread.sleep(100);
        Check.equal(0, second.count(), "a paused driver opens no output on restart");
        ctx.resume();
        ManualOutput two = second.waitFor(1, 10000);
        Check.notNull(two, "the next start uses the factory that was set while the driver was paused");
        awaitFrames(two, 500);
        ctx.close();
        int count = second.count();
        driver.restart();
        Thread.sleep(50);
        Check.equal(count, second.count(), "a closed driver opens no output on restart");
    }

    /**
     * The silent factory plays nowhere: a context that uses it keeps its time running at real speed, no sound device is asked for, and the factory that was replaced is not asked again.
     * <p>
     * 무음 팩터리는 어디로도 재생하지 않는다: 그것을 쓰는 컨텍스트는 시간을 실제 속도로 흐르게 하고 소리 장치를 요청하지 않으며 바뀐 팩터리에는 다시 묻지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSilentFactoryKeepsTheTimeRunning() throws Exception {
        ManualOutputFactory device = new ManualOutputFactory();
        device.setAutoConsume(true);
        RealtimeAudioContext ctx = start(device);
        OutputRenderDriver driver = (OutputRenderDriver) ctx.getDriver();
        try {
            awaitFrames(device.waitFor(1, 10000), 500);
            driver.setOutputFactory(OutputRenderDriver.silentOutputs());
            driver.restart();
            long t0 = System.nanoTime();
            double time0 = ctx.getCurrentTime();
            Thread.sleep(400);
            double advanced = ctx.getCurrentTime() - time0;
            double wall = (System.nanoTime() - t0) / 1e9;
            Check.isTrue(advanced > 0.25 && advanced < wall + 0.15, "the time runs at about real speed without an output: " + advanced + " s in " + wall + " s");
            Check.equal(1, device.count(), "the replaced factory was not asked again");
            Check.isFalse(driver.isOutputAudible(), "nothing is audible");
        } finally {
            ctx.close();
        }
    }
}
