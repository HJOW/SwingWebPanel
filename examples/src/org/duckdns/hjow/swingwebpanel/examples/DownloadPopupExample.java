package org.duckdns.hjow.swingwebpanel.examples;

import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.download.DownloadControl;
import org.duckdns.hjow.swingwebpanel.download.DownloadHandle;
import org.duckdns.hjow.swingwebpanel.download.DownloadRequest;
import org.duckdns.hjow.swingwebpanel.download.DownloadState;
import org.duckdns.hjow.swingwebpanel.download.DownloadStatusSink;
import org.duckdns.hjow.swingwebpanel.download.SwingDownloadHandler;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;

/**
 * Manual check of the download popup: it starts a small web server on the local machine that serves a slow 3 MB file, a file of unknown
 * size and a missing file, and shows a window with one button for each. Click a button to see the real popup (progress bar, Save as...
 * with a file chooser, Cancel, the replace confirmation when the file already exists); click several buttons to run downloads at the same
 * time. Run it with <code>examples/run.sh DownloadPopupExample</code>. With the argument <code>--smoke</code> no window is shown: the
 * program downloads the file into a temporary directory through a popup that only records what happens, prints one line and exits with
 * 0 on success (used for automatic checks, also in a headless environment).
 * <p>
 * 다운로드 팝업의 수동 확인용 예제: 이 컴퓨터에서 느리게 보내는 3 MB 파일, 크기를 알 수 없는 파일, 없는 파일을 제공하는 작은 웹 서버를 시작하고
 * 각각의 버튼이 있는 창을 보여 준다. 버튼을 누르면 실제 팝업(진행 막대, 파일 선택기가 뜨는 다른 이름으로 저장..., 취소, 파일이 이미 있을 때의
 * 교체 확인)을 볼 수 있고, 버튼을 여러 번 누르면 다운로드가 동시에 실행된다. <code>examples/run.sh DownloadPopupExample</code> 로 실행한다.
 * <code>--smoke</code> 인수를 주면 창을 보여 주지 않고 상태만 기록하는 팝업으로 임시 디렉터리에 파일을 내려받은 뒤 한 줄을 출력하고 성공하면 0 으로
 * 끝난다(자동 확인용이며 헤드리스 환경에서도 동작한다).
 */
public final class DownloadPopupExample {

    /** Size of the slow file in bytes. 느린 파일의 크기(바이트). */
    private static final int SLOW_SIZE = 3 * 1024 * 1024;

    /** Private constructor: this class only has a main method. 비공개 생성자: 이 클래스에는 main 메서드만 있다. */
    private DownloadPopupExample() {
    }

    /**
     * Program entry point.
     * <p>
     * 프로그램 진입점.
     *
     * @param args optional <code>--smoke</code> / 선택적인 <code>--smoke</code>
     * @throws Exception on failure / 실패 시
     */
    public static void main(String[] args) throws Exception {
        boolean smoke = args.length > 0 && "--smoke".equals(args[0]);
        final ServerSocket server = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        final String base = "http://127.0.0.1:" + server.getLocalPort();
        startServer(server, smoke ? 0 : 50);
        final ResourceFetcher fetcher = new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT,
                DownloadPopupExample.class.getClassLoader());
        final CookieStore cookies = new CookieStore();
        if (smoke) {
            System.exit(smoke(base, fetcher, cookies) ? 0 : 1);
        }
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Shows the window.
             * <p>
             * 창을 보여 준다.
             */
            public void run() {
                showWindow(base, fetcher, cookies);
            }
        });
    }

    /**
     * Runs the automatic check without a window.
     * <p>
     * 창 없이 자동 확인을 실행한다.
     *
     * @param base    the address of the local server / 로컬 서버의 주소
     * @param fetcher the request layer / 요청 계층
     * @param cookies the cookies / 쿠키
     * @return true if the download worked / 다운로드가 동작했으면 true
     * @throws Exception on failure / 실패 시
     */
    private static boolean smoke(String base, ResourceFetcher fetcher, CookieStore cookies) throws Exception {
        final File dir = File.createTempFile("swp-dl-example-", ".d");
        dir.delete();
        dir.mkdirs();
        final StringBuilder log = new StringBuilder();
        SwingDownloadHandler handler = new SwingDownloadHandler(null, fetcher, cookies, new DownloadStatusSink() {
            /**
             * Appends the message to the log.
             * <p>
             * 메시지를 기록에 덧붙인다.
             *
             * @param message the message / 메시지
             */
            public void downloadStatus(String message) {
                synchronized (log) {
                    log.append(message).append('\n');
                }
            }
        }) {
            /**
             * Uses a popup that shows nothing.
             * <p>
             * 아무것도 보여 주지 않는 팝업을 쓴다.
             *
             * @param request the request / 요청
             * @param control the control / 제어 객체
             * @return the quiet popup / 조용한 팝업
             */
            protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
                return new QuietPopup();
            }

            /**
             * Saves into the temporary directory.
             * <p>
             * 임시 디렉터리에 저장한다.
             *
             * @return the directory / 디렉터리
             */
            protected File getDefaultDirectory() {
                return dir;
            }

            /**
             * Pretends that a screen exists so that the transfer runs.
             * <p>
             * 전송이 일어나도록 화면이 있는 것처럼 행동한다.
             *
             * @return false / false
             */
            protected boolean isHeadless() {
                return false;
            }
        };
        handler.start(new DownloadRequest(base + "/sample.bin", null, "application/octet-stream", null, -1, null, null));
        boolean ended = handler.awaitIdle(60000);
        File saved = new File(dir, "sample.bin");
        boolean ok = ended && saved.isFile() && saved.length() == SLOW_SIZE;
        System.out.println("DownloadPopupExample smoke: " + (ok ? "OK" : "FAILED") + " (" + saved + ", " + saved.length() + " bytes)");
        saved.delete();
        dir.delete();
        handler.dispose();
        return ok;
    }

    /** A popup that ignores everything (for the smoke test). 모든 것을 무시하는 팝업(동작 확인용). */
    private static final class QuietPopup implements DownloadHandle {
        /** Ignores the target. 대상을 무시한다. @param target ignored / 무시 */
        public void setTarget(File target) {
            return;
        }

        /** Ignores the progress. 진행을 무시한다. @param done ignored / 무시 @param total ignored / 무시 */
        public void setProgress(long done, long total) {
            return;
        }

        /** Ignores the state. 상태를 무시한다. @param state ignored / 무시 @param message ignored / 무시 */
        public void setState(DownloadState state, String message) {
            return;
        }

        /** Gives up choosing. 선택을 포기한다. @param current ignored / 무시 @return null / null */
        public File chooseTarget(File current) {
            return null;
        }

        /** Agrees to replace. 교체에 동의한다. @param existing ignored / 무시 @return true / true */
        public boolean confirmOverwrite(File existing) {
            return true;
        }

        /** Does nothing. 아무것도 하지 않는다. */
        public void dispose() {
            return;
        }
    }

    /**
     * Shows the window with the download buttons.
     * <p>
     * 다운로드 버튼이 있는 창을 보여 준다.
     *
     * @param base    the address of the local server / 로컬 서버의 주소
     * @param fetcher the request layer / 요청 계층
     * @param cookies the cookies / 쿠키
     */
    private static void showWindow(final String base, ResourceFetcher fetcher, CookieStore cookies) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("This example needs a screen. Use --smoke for the automatic check.");
            System.exit(0);
        }
        final JFrame frame = new JFrame("Download popup example");
        final JLabel status = new JLabel("Click a button to download. Files go to your Downloads folder.");
        final SwingDownloadHandler handler = new SwingDownloadHandler(frame.getContentPane(), fetcher, cookies, new DownloadStatusSink() {
            /**
             * Shows the message in the status line.
             * <p>
             * 메시지를 상태 줄에 보여 준다.
             *
             * @param message the message / 메시지
             */
            public void downloadStatus(final String message) {
                SwingUtilities.invokeLater(new Runnable() {
                    /**
                     * Sets the text.
                     * <p>
                     * 텍스트를 지정한다.
                     */
                    public void run() {
                        status.setText(message);
                    }
                });
            }
        });
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(button("Slow 3 MB file", handler, base + "/sample.bin"));
        buttons.add(button("Unknown size", handler, base + "/unknown.bin"));
        buttons.add(button("Missing file (404)", handler, base + "/missing.bin"));
        buttons.add(button("Korean name", handler, base + "/korean.bin"));
        frame.getContentPane().add(buttons, java.awt.BorderLayout.CENTER);
        frame.getContentPane().add(status, java.awt.BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            /**
             * Cancels the downloads and exits: this is how a host releases the manager.
             * <p>
             * 다운로드를 취소하고 끝낸다: 호스트가 관리자를 해제하는 방법이다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosing(java.awt.event.WindowEvent e) {
                handler.dispose();
                frame.dispose();
                System.exit(0);
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Creates a button that starts a download.
     * <p>
     * 다운로드를 시작하는 버튼을 만든다.
     *
     * @param text    the caption / 제목
     * @param handler the download manager / 다운로드 관리자
     * @param url     the address to download / 내려받을 주소
     * @return the button / 버튼
     */
    private static JButton button(String text, final SwingDownloadHandler handler, final String url) {
        JButton b = new JButton(text);
        b.addActionListener(new ActionListener() {
            /**
             * Starts the download.
             * <p>
             * 다운로드를 시작한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                handler.start(new DownloadRequest(url, null, "application/octet-stream", null, -1, "DownloadPopupExample/1.0", null));
            }
        });
        return b;
    }

    /**
     * Starts the local server on its own thread; every connection is served on a thread of its own.
     * <p>
     * 로컬 서버를 별도 스레드에서 시작한다. 연결마다 별도 스레드가 처리한다.
     *
     * @param server    the listening socket / 듣는 소켓
     * @param delayStep milliseconds to wait after every 32 KB block (0 = full speed) / 32 KB 블록마다 기다리는 시간(밀리초, 0 이면 최고 속도)
     */
    private static void startServer(final ServerSocket server, final int delayStep) {
        Thread t = new Thread(new Runnable() {
            /**
             * Accepts connections.
             * <p>
             * 연결을 받는다.
             */
            public void run() {
                // 연결을 하나씩 받아 각각을 별도 스레드에서 처리한다.
                while (true) {
                    try {
                        final Socket s = server.accept();
                        Thread h = new Thread(new Runnable() {
                            /**
                             * Serves one connection.
                             * <p>
                             * 연결 하나를 처리한다.
                             */
                            public void run() {
                                serve(s, delayStep);
                            }
                        }, "example-server-conn");
                        h.setDaemon(true);
                        h.start();
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }, "example-server");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Serves one request.
     * <p>
     * 요청 하나를 처리한다.
     *
     * @param s         the connection / 연결
     * @param delayStep milliseconds to wait after every block / 블록마다 기다리는 시간(밀리초)
     */
    private static void serve(Socket s, int delayStep) {
        try {
            InputStream in = s.getInputStream();
            StringBuilder head = new StringBuilder();
            int c;
            // 요청 머리를 빈 줄까지 읽는다.
            while ((c = in.read()) >= 0) {
                head.append((char) c);
                if (head.length() >= 4 && head.substring(head.length() - 4).equals("\r\n\r\n")) {
                    break;
                }
            }
            String line = head.toString().split("\r\n")[0];
            String path = line.split(" ").length > 1 ? line.split(" ")[1] : "/";
            OutputStream out = s.getOutputStream();
            if (path.equals("/missing.bin")) {
                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes("US-ASCII"));
            } else {
                boolean unknown = path.equals("/unknown.bin");
                String name = path.equals("/korean.bin") ? "attachment; filename*=UTF-8''%ED%95%9C%EA%B8%80.bin" : "attachment";
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: " + name + "\r\n"
                        + (unknown ? "" : "Content-Length: " + SLOW_SIZE + "\r\n") + "Connection: close\r\n\r\n").getBytes("US-ASCII"));
                byte[] block = new byte[32 * 1024];
                int sent = 0;
                // 3 MB 를 32 KB 블록으로 나누어 보내며 블록 사이에 잠시 쉰다.
                while (sent < SLOW_SIZE) {
                    out.write(block);
                    out.flush();
                    sent += block.length;
                    if (delayStep > 0) {
                        Thread.sleep(delayStep);
                    }
                }
            }
            out.flush();
        } catch (IOException e) {
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                return;
            }
        }
    }
}
