package org.duckdns.hjow.swingwebpanel.testing.selftest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.duckdns.hjow.swingwebpanel.testing.AssertionFailure;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.SkipException;
import org.duckdns.hjow.swingwebpanel.testing.TestRunner;

/**
 * Tests of the test infrastructure itself: the {@link Check} helpers and the exit code of {@link TestRunner}.
 * <p>
 * 시험 기반 시설 자체의 시험: {@link Check} 도우미와 {@link TestRunner} 의 종료 코드.
 */
public class RunnerSelfTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RunnerSelfTest() {
    }

    /**
     * Checks that equal values and arrays pass and different ones throw {@link AssertionFailure}.
     * <p>
     * 같은 값/배열은 통과하고 다른 값은 {@link AssertionFailure} 를 던지는지 확인한다.
     */
    public void testEqualAndArrays() {
        Check.equal("a", "a", "strings");
        Check.equal(new byte[] { 1, 2 }, new byte[] { 1, 2 }, "byte arrays compare by content");
        Check.equal(null, null, "nulls");
        Check.throwsException(AssertionFailure.class, new Block() {
            /**
             * Runs a failing comparison.
             * <p>
             * 실패하는 비교를 실행한다.
             */
            public void run() {
                Check.equal("a", "b", "must fail");
            }
        }, "different strings must fail");
    }

    /**
     * Checks that assertion helpers work even though Java <code>assert</code> may be disabled.
     * <p>
     * Java <code>assert</code> 가 꺼져 있어도 검사 도우미가 동작하는지 확인한다.
     */
    public void testChecksDoNotDependOnAssertKeyword() {
        Check.throwsException(AssertionFailure.class, new Block() {
            /**
             * Runs a failing isTrue check.
             * <p>
             * 실패하는 isTrue 검사를 실행한다.
             */
            public void run() {
                Check.isTrue(false, "explicit failure");
            }
        }, "isTrue(false) must throw regardless of -ea");
    }

    /**
     * Checks that a skip without a reason is itself a failure.
     * <p>
     * 이유 없는 건너뜀은 그 자체로 실패인지 확인한다.
     */
    public void testSkipRequiresReason() {
        Check.throwsException(AssertionFailure.class, new Block() {
            /**
             * Calls skip with an empty reason.
             * <p>
             * 빈 이유로 skip 을 호출한다.
             */
            public void run() {
                Check.skip("  ");
            }
        }, "skip without reason must fail");
        Check.throwsException(SkipException.class, new Block() {
            /**
             * Calls skip with a reason.
             * <p>
             * 이유를 주고 skip 을 호출한다.
             */
            public void run() {
                Check.skip("environment missing");
            }
        }, "skip with reason must signal a skip");
    }

    /**
     * Runs the runner on the deliberately failing suite and checks exit code 1 and the report.
     * <p>
     * 의도적으로 실패하는 스위트로 실행기를 돌려 종료 코드 1 과 보고서를 확인한다.
     */
    public void testRunnerReportsFailureWithExitCodeOne() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        int code = TestRunner.run(new String[] { "org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite" }, ps);
        ps.flush();
        String report = bos.toString();
        Check.equal(TestRunner.EXIT_FAILED, code, "exit code for a failing suite");
        Check.contains(report, "FAIL org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite.testFailsOnPurpose", "failure line");
        Check.contains(report, "1 passed, 1 failed, 0 skipped", "summary counts");
    }

    /**
     * Runs the runner on this passing suite filtered to one case and checks exit code 0.
     * <p>
     * 이 통과 스위트를 사례 하나로 필터링해 실행하고 종료 코드 0 을 확인한다.
     */
    public void testRunnerExitCodeZeroOnSuccess() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        int code = TestRunner.run(new String[] { "--filter", "testEqualAndArrays",
                "org.duckdns.hjow.swingwebpanel.testing.selftest.RunnerSelfTest" }, ps);
        ps.flush();
        Check.equal(TestRunner.EXIT_OK, code, "exit code for a passing suite");
        Check.contains(bos.toString(), "1 passed, 0 failed", "summary counts");
    }

    /**
     * Checks that an unknown suite class is reported as a failure, not silently ignored.
     * <p>
     * 알 수 없는 스위트 클래스가 조용히 무시되지 않고 실패로 보고되는지 확인한다.
     */
    public void testMissingSuiteIsFailure() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int code = TestRunner.run(new String[] { "no.such.Suite" }, new PrintStream(bos));
        Check.equal(TestRunner.EXIT_FAILED, code, "missing suite must fail the run");
    }
}
