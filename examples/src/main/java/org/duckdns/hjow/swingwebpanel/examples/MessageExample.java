package org.duckdns.hjow.swingwebpanel.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.WebMessageListener;

/**
 * Demonstrates host messages and replies with a local classpath document.
 * Run with <code>--smoke</code> to check the round trip without displaying a window.
 * <p>
 * 로컬 클래스패스 문서에서 호스트 메시지와 응답을 보여 준다.
 * <code>--smoke</code>로 창을 띄우지 않고 왕복을 확인할 수 있다.
 */
public final class MessageExample {

    /** Local example page. / 로컬 예제 페이지. */
    private static final String PAGE = "classpath://org/duckdns/hjow/swingwebpanel/examples/pages/messages.html";

    /** Exact origin of the local document. / 로컬 문서의 정확한 오리진. */
    private static final String ORIGIN = "classpath://";

    /** Runs through the static entry point. / 정적 진입점으로 실행한다. */
    private MessageExample() {
    }

    /**
     * Starts the interactive example or the round-trip check.
     * <p>
     * 대화형 예제 또는 왕복 점검을 시작한다.
     *
     * @param args optional <code>--smoke</code> / 선택 인수 <code>--smoke</code>
     * @throws Exception if the smoke check cannot run / 왕복 점검을 실행할 수 없으면
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--smoke".equals(args[0])) {
            smoke();
        } else if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Use --smoke to check messaging without a display.");
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Creates the example window on the EDT. / EDT에서 예제 창을 만든다. */
                public void run() {
                    show();
                }
            });
        }
    }

    /**
     * Validates the example document's origin, sender kind and message shape.
     * <p>
     * 예제 문서의 오리진, 송신 종류 및 메시지 구조를 검증한다.
     *
     * @param event received event / 수신 이벤트
     * @return message fields when accepted, or null / 받아들인 메시지 필드 또는 null
     */
    private static Map<?, ?> trustedData(WebMessageEvent event) {
        if (!ORIGIN.equals(event.getOrigin()) || !PAGE.equals(event.getDocumentUrl())
                || event.getSourceKind() != WebMessageEvent.SourceKind.DOCUMENT || !(event.getData() instanceof Map<?, ?>)) {
            return null;
        }
        return (Map<?, ?>) event.getData();
    }

    /** Builds the interactive Java host interface. / 대화형 Java 호스트 화면을 만든다. */
    private static void show() {
        final SwingWebPanel panel = new SwingWebPanel();
        final JFrame frame = new JFrame("Java and page messages");
        final JTextField text = new JTextField("Hello from Java");
        final JButton send = new JButton("Send to page");
        final JLabel status = new JLabel("Waiting for the page");
        send.setEnabled(false);
        final ActionListener transmit = new ActionListener() {
            /** Sends the field value to the exact example origin. / 입력값을 정확한 예제 오리진으로 보낸다. */
            public void actionPerformed(ActionEvent e) {
                if (send.isEnabled()) {
                    panel.postMessage(Collections.singletonMap("text", text.getText()), ORIGIN);
                }
            }
        };
        send.addActionListener(transmit);
        text.addActionListener(transmit);
        panel.addWebMessageListener(new WebMessageListener() {
            /** Updates the Swing controls on the EDT. / EDT에서 Swing 컨트롤을 갱신한다. */
            public void messageReceived(WebMessageEvent event) {
                Map<?, ?> fields = trustedData(event);
                if (fields == null) {
                    return;
                }
                if ("ready".equals(fields.get("type"))) {
                    send.setEnabled(true);
                    status.setText("Page ready");
                } else if (fields.get("text") instanceof String) {
                    if ("echo".equals(fields.get("type"))) {
                        status.setText("Page received " + fields.get("text"));
                    } else if ("note".equals(fields.get("type"))) {
                        status.setText(String.valueOf(fields.get("text")));
                    }
                }
            }
        });
        JPanel controls = new JPanel(new BorderLayout(8, 4));
        controls.add(text, BorderLayout.CENTER);
        controls.add(send, BorderLayout.EAST);
        controls.add(status, BorderLayout.SOUTH);
        frame.add(new JScrollPane(panel), BorderLayout.CENTER);
        frame.add(controls, BorderLayout.SOUTH);
        frame.setPreferredSize(new Dimension(560, 300));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            /** Releases the browser when the example closes. / 예제를 닫을 때 브라우저를 해제한다. */
            public void windowClosed(WindowEvent e) {
                panel.dispose();
            }
        });
        frame.pack();
        frame.setVisible(true);
        panel.setPage(PAGE);
    }

    /**
     * Replies to readiness from the EDT and waits on the launching thread for the page echo.
     * <p>
     * EDT에서 준비 메시지에 답하고 시작 스레드에서 페이지의 응답을 기다린다.
     *
     * @throws Exception on setup or waiting failure / 환경 구성 또는 대기 실패 시
     */
    private static void smoke() throws Exception {
        final SwingWebPanel[] created = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the browser on the EDT. / EDT에서 브라우저를 만든다. */
            public void run() {
                created[0] = new SwingWebPanel();
            }
        });
        SwingWebPanel panel = created[0];
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<String> echo = new AtomicReference<String>();
        try {
            panel.addWebMessageListener(new WebMessageListener() {
                /** Sends the challenge and checks the matching reply. / 확인값을 보내고 대응하는 응답을 받는다. */
                public void messageReceived(WebMessageEvent event) {
                    Map<?, ?> fields = trustedData(event);
                    if (fields == null || !SwingUtilities.isEventDispatchThread()) {
                        return;
                    }
                    if ("ready".equals(fields.get("type"))) {
                        event.reply(Collections.singletonMap("text", "message smoke"));
                    } else if ("echo".equals(fields.get("type")) && fields.get("text") instanceof String) {
                        echo.set((String) fields.get("text"));
                        done.countDown();
                    }
                }
            });
            panel.setPage(PAGE);
            if (!done.await(20, TimeUnit.SECONDS) || !"message smoke".equals(echo.get())) {
                throw new IllegalStateException("The classpath message round trip did not complete");
            }
            System.out.println("MessageExample smoke OK: " + echo.get());
        } finally {
            panel.dispose();
        }
    }
}
