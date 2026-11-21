package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The helper of the audio worklet tests (not a suite): a page that is loaded the way the component loads it ({@link PageFixture}, with a manual clock and a task queue that the
 * test runs), the modules of the worklet as texts under their addresses, and the means to run scripts, to read the error that a statement throws, and to wait for what the other threads
 * produce (the worklet thread, an offline rendering) while the test thread runs the tasks of the page, as the document thread of the component does.
 * <p>
 * 오디오 worklet 시험의 도우미(스위트가 아님): 컴포넌트가 로드하는 방식으로 로드한 페이지({@link PageFixture}. 수동 시계와 시험이 실행하는 작업 큐를 가진다), 주소별 텍스트로 둔 worklet 의 모듈, 그리고
 * 스크립트를 실행하고, 문장이 던지는 오류를 읽고, 다른 스레드(worklet 스레드, 오프라인 렌더링)가 만드는 것을 기다리는 수단. 기다리는 동안 컴포넌트의 문서 스레드처럼 시험 스레드가 페이지의 작업을
 * 실행한다.
 */
final class WorkletPage {

    /**
     * The address of the page; modules are under the same directory.
     * <p>
     * 페이지의 주소. 모듈은 같은 디렉터리에 있다.
     */
    static final String DIR = "http://example.test/dir/";

    /**
     * The fixture of the page.
     * <p>
     * 페이지의 시험 도구.
     */
    final PageFixture fixture = new PageFixture();

    /**
     * What the consoles of the page and of the worklet received, as <code>level: text</code>; a list that the worklet thread, the rendering threads and the test thread may use at once.
     * <p>
     * 페이지와 worklet 의 콘솔이 받은 것(<code>수준: 문자열</code>). worklet 스레드, 렌더링 스레드, 시험 스레드가 동시에 쓸 수 있는 목록.
     */
    private final List<String> lines = Collections.synchronizedList(new ArrayList<String>());

    /**
     * Loads an empty page.
     * <p>
     * 빈 페이지를 로드한다.
     */
    WorkletPage() {
        fixture.load(DIR + "page.html", "<!doctype html><body><p>worklet</p></body>");
        fixture.scripts.getRealm().setConsole(new ConsoleHook() {
            /**
             * Keeps the message.
             * <p>
             * 메시지를 보관한다.
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                lines.add(level + ": " + text);
            }
        });
    }

    /**
     * Returns a copy of what the consoles received so far.
     * <p>
     * 지금까지 콘솔이 받은 것의 복사본을 반환한다.
     *
     * @return the lines / 줄
     */
    List<String> console() {
        synchronized (lines) {
            return new ArrayList<String>(lines);
        }
    }

    /**
     * Puts the text of a module under an address in the directory of the page.
     * <p>
     * 페이지의 디렉터리 안 주소에 모듈의 텍스트를 둔다.
     *
     * @param file the file name / 파일 이름
     * @param text the text / 텍스트
     */
    void module(String file, String text) {
        fixture.env.scripts.put(DIR + file, text);
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
        Check.notNull(result, "the script threw: " + source + " " + console());
        return result;
    }

    /**
     * Evaluates a script that must not throw and returns its completion value as a number.
     * <p>
     * 던지지 않아야 하는 스크립트를 실행해 완료 값을 수로 반환한다.
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
     * Runs the tasks of the page for a time.
     * <p>
     * 일정 시간 동안 페이지의 작업을 실행한다.
     *
     * @param ms the time in milliseconds / 시간(밀리초)
     */
    void pumpFor(long ms) {
        long end = System.currentTimeMillis() + ms;
        // 정해진 시간 동안 작업을 실행하며 짧게 쉰다.
        while (System.currentTimeMillis() < end) {
            pump();
            sleep(2);
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
                Check.fail("timed out: " + message + " " + condition + " " + console());
            }
            sleep(2);
        }
    }

    /**
     * Sleeps a little.
     * <p>
     * 잠깐 잔다.
     *
     * @param ms the time / 시간
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Check.fail("interrupted");
        }
    }
}
