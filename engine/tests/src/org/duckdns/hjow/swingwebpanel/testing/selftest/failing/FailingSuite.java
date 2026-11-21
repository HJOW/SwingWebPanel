package org.duckdns.hjow.swingwebpanel.testing.selftest.failing;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * A suite that fails on purpose. It is NOT discovered by <code>--scan</code> (its name does not end with Test/Tests);
 * <code>test.sh</code> and <code>test.bat</code> run it explicitly to prove that the runner exits with code 1 on a failure.
 * <p>
 * 일부러 실패하는 스위트. 이름이 Test/Tests 로 끝나지 않아 <code>--scan</code> 으로는 발견되지 않는다.
 * <code>test.sh</code>/<code>test.bat</code> 가 이 클래스를 직접 지정해 실행하여 실행기가 실패 시 종료 코드 1 을 내는지 확인한다.
 */
public class FailingSuite {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FailingSuite() {
    }

    /**
     * A passing case, to prove that passing cases are counted next to the failure.
     * <p>
     * 통과하는 사례. 실패와 함께 통과도 집계되는지 확인한다.
     */
    public void testPasses() {
        Check.equal(2, 1 + 1, "arithmetic");
    }

    /**
     * The deliberate failure.
     * <p>
     * 의도적인 실패.
     */
    public void testFailsOnPurpose() {
        Check.equal(1, 2, "this failure is expected by the self-check");
    }
}
