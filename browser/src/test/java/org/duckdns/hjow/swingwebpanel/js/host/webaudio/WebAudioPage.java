package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The helper of the Web Audio page tests (not a suite): a page that is loaded the way the component loads it ({@link PageFixture}, with a manual clock and a
 * task queue that the test runs), plus the means to run scripts, to read the error that a statement throws and to wait for what other threads produce (an
 * offline rendering, a decoded buffer, a real-time context) while the test thread runs the tasks of the page, as the document thread of the component does.
 * <p>
 * Web Audio 페이지 시험의 도우미(스위트가 아님): 컴포넌트가 로드하는 방식으로 로드한 페이지({@link PageFixture}. 수동 시계와 시험이 실행하는 작업 큐를 가진다)와, 스크립트를
 * 실행하고, 문장이 던지는 오류를 읽고, 다른 스레드가 만드는 것(오프라인 렌더링, 디코딩된 버퍼, 실시간 컨텍스트)을 기다리는 수단. 기다리는 동안 컴포넌트의 문서 스레드처럼 시험
 * 스레드가 페이지의 작업을 실행한다.
 */
final class WebAudioPage {

    /**
     * The fixture of the page.
     * <p>
     * 페이지의 시험 도구.
     */
    final PageFixture fixture = new PageFixture();

    /**
     * Loads an empty page with the default address.
     * <p>
     * 기본 주소의 빈 페이지를 로드한다.
     */
    WebAudioPage() {
        this(false);
    }

    /** Loads the page with the specified isolation capability. / 지정한 격리 권한으로 페이지를 로드한다. */
    WebAudioPage(boolean isolated) {
        fixture.env.crossOriginIsolated = isolated;
        fixture.load("http://example.test/dir/page.html", "<!doctype html><body><p>audio</p></body>");
    }

    /**
     * Evaluates a script that must not throw and returns its completion value as text.
     * <p>
     * 던지지 않아야 하는 스크립트를 실행해 완료 값을 문자열로 반환한다.
     *
     * @param source the script / 스크립트
     * @return the text / 문자열
     */
    String run(String source) {
        String result = fixture.scripts.evaluate(source, "test");
        Check.notNull(result, "the script threw: " + source + " " + fixture.console);
        return result;
    }

    /**
     * Evaluates a script that must not throw and returns its completion value as a number.
     * <p>
     * 던지지 않아야 하는 스크립트를 실행해 완료 값을 숫자로 반환한다.
     *
     * @param source the script / 스크립트
     * @return the number / 수
     */
    double number(String source) {
        return Double.parseDouble(run(source));
    }

    /**
     * Returns the name of the error that a statement throws, or <code>no error</code>.
     * <p>
     * 문장이 던지는 오류의 이름을 반환한다. 던지지 않으면 <code>no error</code>.
     *
     * @param statement the statement / 문장
     * @return the error name / 오류 이름
     */
    String errorOf(String statement) {
        return run("(function () { try { " + statement + "; return 'no error'; } catch (e) { return e.name; } })()");
    }

    /**
     * Runs the tasks that are due.
     * <p>
     * 실행할 때가 된 작업을 실행한다.
     */
    void pump() {
        fixture.advance(0);
    }

    /**
     * Runs the tasks of the page for a time (to give other threads time to deliver what they produce, or to show that nothing comes).
     * <p>
     * 페이지의 작업을 일정 시간 동안 실행한다(다른 스레드가 만든 것을 전달할 시간을 주거나, 아무것도 오지 않음을 보이기 위해).
     *
     * @param ms the time in milliseconds / 시간(밀리초)
     */
    void pumpFor(long ms) {
        long end = System.currentTimeMillis() + ms;
        // 정해진 시간 동안 작업을 실행하며 짧게 쉰다.
        while (System.currentTimeMillis() < end) {
            pump();
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Check.fail("interrupted");
            }
        }
        pump();
    }

    /**
     * Waits until a script expression is true, running the tasks of the page meanwhile.
     * <p>
     * 스크립트 식이 참이 될 때까지 기다린다. 그동안 페이지의 작업을 실행한다.
     *
     * @param condition the expression / 식
     * @param timeoutMs the longest time to wait / 기다리는 최대 시간
     * @param message   the message of the failure / 실패 메시지
     */
    void waitFor(String condition, long timeoutMs, String message) {
        long end = System.currentTimeMillis() + timeoutMs;
        // 식이 참이 될 때까지(최대 timeoutMs) 작업을 실행하며 짧게 쉰다.
        for (;;) {
            pump();
            if ("true".equals(run("String(!!(" + condition + "))"))) {
                return;
            }
            if (System.currentTimeMillis() > end) {
                Check.fail("timed out: " + message + " " + condition + " " + fixture.console);
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Check.fail("interrupted");
            }
        }
    }
}
