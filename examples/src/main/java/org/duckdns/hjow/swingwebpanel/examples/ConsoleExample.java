package org.duckdns.hjow.swingwebpanel.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

/**
 * Shows how a host application receives the output of the page's <code>console</code> object: a <code>SwingWebPanel</code> with a page
 * whose buttons call <code>console.log</code>, <code>console.warn</code>, <code>console.error</code>, throw an exception or reject a
 * promise, and a text area below that lists every {@link ConsoleEvent} the registered {@link ConsoleListener} gets. The listener is called
 * on the event dispatch thread, so it may touch Swing components directly. Closing the window calls {@link SwingWebPanel#dispose()},
 * which also removes the console listener. Run it with <code>examples/run.sh ConsoleExample</code>; with <code>--smoke</code> the page logs
 * by itself, and the window closes after both events arrive or a timeout, printing a one-line result.
 * <p>
 * 호스트 응용이 페이지의 <code>console</code> 객체 출력을 받는 방법을 보여 준다: 단추가 <code>console.log</code>, <code>console.warn</code>,
 * <code>console.error</code> 를 호출하거나 예외를 던지거나 promise 를 거부하는 페이지가 있는 <code>SwingWebPanel</code>, 그리고 등록한
 * {@link ConsoleListener} 가 받은 모든 {@link ConsoleEvent} 를 나열하는 아래쪽 텍스트 영역. 리스너는 이벤트 디스패치 스레드에서 호출되므로 Swing
 * 컴포넌트를 바로 만져도 된다. 창을 닫으면 {@link SwingWebPanel#dispose()} 를 호출하며 콘솔 리스너도 함께 제거된다.
 * <code>examples/run.sh ConsoleExample</code> 로 실행하며, <code>--smoke</code> 를 주면 페이지가 스스로 로그를 쓴다.
 * 두 이벤트를 받거나 제한 시간이 지나면 창을 닫고 한 줄 결과를 출력한다.
 */
public final class ConsoleExample {

    /**
     * The page: five buttons, each calls a console method or makes a problem that the browser reports.
     * <p>
     * 페이지: 단추 다섯 개. 각각 콘솔 메서드를 호출하거나 브라우저가 보고하는 문제를 만든다.
     */
    private static final String PAGE = "<h2>Console example</h2>"
            + "<p><button onclick=\"console.log('clicked %s, %d times', 'log', ++n)\">log</button> "
            + "<button onclick=\"console.warn('careful', {a: 1})\">warn</button> "
            + "<button onclick=\"console.error('failed:', new Error('boom').message)\">error</button> "
            + "<button onclick=\"throw new Error('uncaught from a button')\">throw</button> "
            + "<button onclick=\"Promise.reject(new Error('nobody handles this'))\">reject</button></p>"
            + "<script>var n = 0; console.info('page ready'); console.log('hello', [1, 2, 3]);</" + "script>";

    /**
     * Private constructor: this class only has a main method.
     * <p>
     * 비공개 생성자: 이 클래스에는 main 메서드만 있다.
     */
    private ConsoleExample() {
    }

    /**
     * Program entry point.
     * <p>
     * 프로그램 진입점.
     *
     * @param args optional <code>--smoke</code> / 선택적인 <code>--smoke</code>
     */
    public static void main(final String[] args) {
        boolean smoke = false;
        // 인수를 훑어 --smoke 를 찾는다.
        for (int i = 0; i < args.length; i++) {
            if ("--smoke".equals(args[i])) {
                smoke = true;
            }
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("headless environment: no window is shown");
            return;
        }
        final boolean smokeRun = smoke;
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Builds and shows the window on the event dispatch thread.
             * <p>
             * 이벤트 디스패치 스레드에서 창을 만들어 보여 준다.
             */
            public void run() {
                show(smokeRun);
            }
        });
    }

    /**
     * Builds and shows the window.
     * <p>
     * 창을 만들어 보여 준다.
     *
     * @param smoke whether to close automatically / 자동으로 닫을지 여부
     */
    private static void show(final boolean smoke) {
        final SwingWebPanel panel = new SwingWebPanel();
        final JTextArea log = new JTextArea(8, 60);
        log.setEditable(false);
        final int[] count = new int[1];

        // 콘솔 리스너를 등록한다. 호출은 이벤트 디스패치 스레드에서 오므로 텍스트 영역을 바로 갱신해도 된다.
        panel.addConsoleListener(new ConsoleListener() {
            /**
             * Appends one line per console message: serial number, level, who wrote it, where, and the message.
             * <p>
             * 콘솔 메시지마다 한 줄을 덧붙인다: 일련번호, 수준, 쓴 주체, 위치, 메시지.
             *
             * @param e the console message / 콘솔 메시지
             */
            public void consoleMessage(ConsoleEvent e) {
                count[0]++;
                String place = e.getLineNumber() > 0 ? " (" + e.getScriptUrl() + ":" + e.getLineNumber() + ")" : "";
                log.append("#" + e.getSequence() + " [" + e.getLevel() + "/" + e.getOrigin() + "] " + e.getMessage() + place + "\n");
                // 새 줄이 보이도록 맨 아래로 스크롤한다.
                log.setCaretPosition(log.getDocument().getLength());
            }
        });

        final JFrame frame = new JFrame("SwingWebPanel console");
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(panel), new JScrollPane(log));
        split.setResizeWeight(0.7);
        frame.add(split, BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            /**
             * Releases the component (this also removes the console listener) and closes the window.
             * <p>
             * 컴포넌트를 해제하고(콘솔 리스너도 함께 제거된다) 창을 닫는다.
             *
             * @param e window event / 창 이벤트
             */
            public void windowClosing(WindowEvent e) {
                panel.dispose();
                frame.dispose();
            }
        });
        frame.pack();
        frame.setVisible(true);
        panel.setContent(PAGE, "text/html");
        if (smoke) {
            final long deadline = System.nanoTime() + 20000000000L;
            Timer t = new Timer(100, new ActionListener() {
                /**
                 * Reports what the listener got and ends the smoke run.
                 * <p>
                 * 리스너가 받은 것을 보고하고 점검 실행을 끝낸다.
                 *
                 * @param e timer event / 타이머 이벤트
                 */
                public void actionPerformed(ActionEvent e) {
                    boolean ready = count[0] >= 2;
                    // 페이지와 리스너의 비동기 작업이 모두 끝날 때까지 기다린다.
                    if (!ready && System.nanoTime() - deadline < 0L) {
                        return;
                    }
                    ((Timer) e.getSource()).stop();
                    System.out.println("smoke " + (ready ? "OK" : "FAILED") + " consoleEvents=" + count[0]
                            + " listeners=" + panel.getConsoleListeners().length);
                    panel.dispose();
                    frame.dispose();
                    System.exit(ready ? 0 : 1);
                }
            });
            t.start();
        }
    }
}
