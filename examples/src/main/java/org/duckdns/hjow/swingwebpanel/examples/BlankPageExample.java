package org.duckdns.hjow.swingwebpanel.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

/**
 * Minimal browser window: a <code>SwingWebPanel</code> wrapped in a <code>JScrollPane</code>, an address field, back/forward/refresh
 * buttons, a progress bar and a status line. It shows how the component is used and how it is released: closing the window calls
 * {@link SwingWebPanel#dispose()}. Run it with <code>examples/run.sh BlankPageExample</code>; with the argument <code>--smoke</code>
 * the window closes after the page finishes loading or a timeout, and the program prints a one-line result.
 * <p>
 * 최소한의 브라우저 창: <code>JScrollPane</code> 으로 감싼 <code>SwingWebPanel</code>, 주소 입력란, 뒤로/앞으로/새로고침 버튼,
 * 진행 막대, 상태 줄. 컴포넌트를 쓰는 방법과 해제하는 방법을 보여 준다: 창을 닫으면 {@link SwingWebPanel#dispose()} 를 호출한다.
 * <code>examples/run.sh BlankPageExample</code> 로 실행하며, <code>--smoke</code> 인수를 주면 페이지 로드 완료 또는 제한 시간 뒤
 * 창을 닫고 한 줄 결과를 출력한다.
 */
public final class BlankPageExample {

    /**
     * Private constructor: this class only has a main method.
     * <p>
     * 비공개 생성자: 이 클래스에는 main 메서드만 있다.
     */
    private BlankPageExample() {
    }

    /**
     * Program entry point.
     * <p>
     * 프로그램 진입점.
     *
     * @param args optional <code>--smoke</code> and an optional start address / 선택적인 <code>--smoke</code> 와 선택적인 시작 주소
     */
    public static void main(final String[] args) {
        boolean smoke = false;
        String start = null;
        // 인수를 훑어 --smoke 와 시작 주소를 찾는다.
        for (int i = 0; i < args.length; i++) {
            if ("--smoke".equals(args[i])) {
                smoke = true;
            } else {
                start = args[i];
            }
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("headless environment: no window is shown");
            return;
        }
        final boolean smokeRun = smoke;
        final String startAddress = start;
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Builds and shows the window on the event dispatch thread.
             * <p>
             * 이벤트 디스패치 스레드에서 창을 만들어 보여 준다.
             */
            public void run() {
                show(smokeRun, startAddress);
            }
        });
    }

    /**
     * Builds and shows the window.
     * <p>
     * 창을 만들어 보여 준다.
     *
     * @param smoke whether to close automatically / 자동으로 닫을지 여부
     * @param start start address or null for a small test page / 시작 주소. 작은 시험 페이지이면 null
     */
    private static void show(final boolean smoke, String start) {
        final SwingWebPanel panel = new SwingWebPanel();
        final JFrame frame = new JFrame("SwingWebPanel");
        final JTextField address = new JTextField("about:blank");
        final JLabel status = new JLabel(" ");
        final JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(100);

        final ActionListener go = new ActionListener() {
            /**
             * Loads the address typed into the field.
             * <p>
             * 입력란에 쓴 주소를 읽는다.
             *
             * @param e event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.setPage(address.getText().trim());
                } catch (IllegalArgumentException ex) {
                    status.setText("Invalid address: " + ex.getMessage());
                }
            }
        };
        address.addActionListener(go);
        JButton back = new JButton("<");
        back.addActionListener(new ActionListener() {
            /**
             * Goes back one entry.
             * <p>
             * 한 칸 뒤로 간다.
             *
             * @param e event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                panel.back();
            }
        });
        JButton forward = new JButton(">");
        forward.addActionListener(new ActionListener() {
            /**
             * Goes forward one entry.
             * <p>
             * 한 칸 앞으로 간다.
             *
             * @param e event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                panel.forward();
            }
        });
        JButton refresh = new JButton("R");
        refresh.addActionListener(new ActionListener() {
            /**
             * Reloads the page.
             * <p>
             * 페이지를 다시 읽는다.
             *
             * @param e event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                panel.refresh();
            }
        });
        JPanel buttons = new JPanel();
        buttons.add(back);
        buttons.add(forward);
        buttons.add(refresh);
        JPanel top = new JPanel(new BorderLayout());
        top.add(buttons, BorderLayout.WEST);
        top.add(address, BorderLayout.CENTER);

        // 컴포넌트의 속성 변경을 받아 주소란, 상태 줄, 진행 막대를 갱신한다.
        panel.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * Mirrors the component state into the window.
             * <p>
             * 컴포넌트 상태를 창에 반영한다.
             *
             * @param e property change / 속성 변경
             */
            public void propertyChange(PropertyChangeEvent e) {
                String n = e.getPropertyName();
                if (SwingWebPanel.PROPERTY_PAGE.equals(n)) {
                    address.setText(String.valueOf(e.getNewValue()));
                } else if (SwingWebPanel.PROPERTY_STATUS_TEXT.equals(n)) {
                    status.setText(String.valueOf(e.getNewValue()));
                } else if (SwingWebPanel.PROPERTY_PROGRESS.equals(n)) {
                    int v = ((Integer) e.getNewValue()).intValue();
                    bar.setIndeterminate(v < 0);
                    bar.setValue(Math.max(v, 0));
                }
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(bar, BorderLayout.EAST);
        frame.add(top, BorderLayout.NORTH);
        frame.add(new JScrollPane(panel), BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            /**
             * Releases the component and closes the window.
             * <p>
             * 컴포넌트를 해제하고 창을 닫는다.
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
        if (start != null) {
            address.setText(start);
            go.actionPerformed(null);
        } else {
            panel.setContent("<h1>Hello</h1>", "text/html");
        }
        if (smoke) {
            final long deadline = System.nanoTime() + 20000000000L;
            final boolean blankTarget = "about:blank".equals(start);
            Timer t = new Timer(100, new ActionListener() {
                /**
                 * Reports the state and ends the smoke run.
                 * <p>
                 * 상태를 보고하고 점검 실행을 끝낸다.
                 *
                 * @param e timer event / 타이머 이벤트
                 */
                public void actionPerformed(ActionEvent e) {
                    String page = panel.getPage();
                    boolean ready = panel.getProgress() == 100 && (blankTarget || !"about:blank".equals(page));
                    // 초기 빈 페이지를 결과로 보고하지 않고 실제 이동 또는 제한 시간을 기다린다.
                    if (!ready && System.nanoTime() - deadline < 0L) {
                        return;
                    }
                    ((Timer) e.getSource()).stop();
                    System.out.println("smoke " + (ready ? "OK" : "FAILED") + " page=" + page + " status=" + panel.getStatusText()
                            + " progress=" + panel.getProgress() + " histories=" + panel.getHistories());
                    panel.dispose();
                    frame.dispose();
                    System.exit(ready ? 0 : 1);
                }
            });
            t.start();
        }
    }
}
