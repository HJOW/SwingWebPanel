package org.duckdns.hjow.swingwebpanel;

import javax.swing.SwingUtilities;

/**
 * Checks the default {@link SwingWebPanel#closeRootWindow()} in a JVM of its own (case C-ROOTCLOSE-8, plan item P30-03): a page calls
 * <code>window.close()</code>, and the process must end with exit code 0 because the component called <code>System.exit(0)</code>. If the
 * program is still running after the wait, the process ends with exit code 3. The Maven build runs it twice: with the setting on (expected
 * code 0) and, as the control, with the setting off (argument <code>off</code>, expected code 3).
 * <p>
 * 기본 {@link SwingWebPanel#closeRootWindow()} 를 별도 JVM 에서 확인한다(사례 C-ROOTCLOSE-8, 계획 P30-03): 페이지가 <code>window.close()</code>
 * 를 부르고, 컴포넌트가 <code>System.exit(0)</code> 을 불렀으므로 프로세스가 종료 코드 0 으로 끝나야 한다. 기다린 뒤에도 프로그램이 실행 중이면
 * 종료 코드 3 으로 끝난다. Maven 빌드가 두 번 실행한다: 설정을 켠 경우(기대 코드 0)와, 대조로 설정을 끈 경우(인수 <code>off</code>, 기대 코드 3).
 */
public final class RootWindowClosingExitMain {

    /** Exit code when the page did not end the program. / 페이지가 프로그램을 끝내지 않았을 때의 종료 코드. */
    private static final int STILL_RUNNING = 3;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private RootWindowClosingExitMain() {
    }

    /**
     * Shows a page that closes its window and waits for the end of the program.
     * <p>
     * 자기 창을 닫는 페이지를 보여 주고 프로그램이 끝나기를 기다린다.
     *
     * @param args <code>off</code> to leave the setting off (the control run) / 설정을 끈 채로 두려면 <code>off</code>(대조 실행)
     * @throws Exception on failure / 실패 시
     */
    public static void main(String[] args) throws Exception {
        final boolean allow = args.length == 0 || !"off".equals(args[0]);
        final SwingWebPanel[] made = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component on the EDT. / EDT 에서 컴포넌트를 만든다. */
            public void run() {
                made[0] = new SwingWebPanel();
                made[0].setSize(200, 100);
            }
        });
        SwingWebPanel panel = made[0];
        panel.setRootWindowClosingEnabled(allow);
        panel.setContent("<!doctype html><title>close</title><script>window.close();</" + "script>", "text/html");
        // 허용한 경우 여기서 기다리는 동안 프로세스가 끝난다. 대조 실행은 짧게만 기다린다.
        Thread.sleep(allow ? 30000 : 4000);
        Runtime.getRuntime().halt(STILL_RUNNING);
    }
}
