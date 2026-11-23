package org.duckdns.hjow.swingwebpanel.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A tiny HTTP server for the download tests that holds a response open: it sends the head and the first part of the body, then waits
 * until the test releases it or the client closes the connection. This makes "cancel in the middle of a transfer" deterministic, which
 * the general test server cannot do. Only a single request per connection is served and the request is not interpreted.
 * <p>
 * 다운로드 시험을 위한 작은 HTTP 서버로 응답을 열어 둔 채 붙잡는다: 응답 머리와 본문의 앞부분을 보낸 뒤 시험이 풀어 주거나 클라이언트가 연결을
 * 닫을 때까지 기다린다. 일반 시험 서버가 하지 못하는 "전송 도중 취소"를 결정적으로 시험할 수 있다. 연결마다 요청 하나만 처리하며 요청을
 * 해석하지 않는다.
 */
final class HoldingServer {

    /** The listening socket. 듣는 소켓. */
    private final ServerSocket server;

    /** Total body size announced in Content-Length. Content-Length 로 알리는 전체 본문 크기. */
    private final int totalSize;

    /** Bytes sent before the server starts holding. 붙잡기 전에 보내는 바이트 수. */
    private final int firstPart;

    /** Opened when the test releases the held responses. 시험이 붙잡은 응답을 풀어 줄 때 열린다. */
    private final CountDownLatch release = new CountDownLatch(1);

    /** Number of accepted connections. 받아들인 연결 수. */
    private final AtomicInteger connections = new AtomicInteger();

    /** Number of clients that closed while held. 붙잡혀 있는 동안 연결을 닫은 클라이언트 수. */
    private final AtomicInteger clientsClosed = new AtomicInteger();

    /** Number of responses that were completely sent. 끝까지 보낸 응답 수. */
    private final AtomicInteger completed = new AtomicInteger();

    /** True after {@link #stop()}. {@link #stop()} 뒤에는 true. */
    private volatile boolean stopped;

    /**
     * Starts a server on a free loopback port.
     * <p>
     * 비어 있는 루프백 포트에서 서버를 시작한다.
     *
     * @param totalSize total body size / 전체 본문 크기
     * @param firstPart bytes sent before holding / 붙잡기 전에 보내는 바이트 수
     * @throws IOException if the socket cannot be opened / 소켓을 열 수 없으면
     */
    HoldingServer(int totalSize, int firstPart) throws IOException {
        this.totalSize = totalSize;
        this.firstPart = Math.min(firstPart, totalSize);
        this.server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        Thread t = new Thread(new Runnable() {
            /**
             * Accepts connections until the server is stopped.
             * <p>
             * 서버가 멈출 때까지 연결을 받는다.
             */
            public void run() {
                // 서버가 닫힐 때까지 연결을 받아 각각을 별도 스레드에서 처리한다.
                while (!stopped) {
                    try {
                        final Socket s = server.accept();
                        connections.incrementAndGet();
                        Thread h = new Thread(new Runnable() {
                            /**
                             * Serves one connection.
                             * <p>
                             * 연결 하나를 처리한다.
                             */
                            public void run() {
                                serve(s);
                            }
                        }, "holding-server-conn");
                        h.setDaemon(true);
                        h.start();
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }, "holding-server");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the address of a path on this server.
     * <p>
     * 이 서버에서 경로의 주소를 반환한다.
     *
     * @param path the path starting with a slash / 슬래시로 시작하는 경로
     * @return the URL / URL
     */
    String url(String path) {
        return "http://127.0.0.1:" + server.getLocalPort() + path;
    }

    /**
     * Lets the held responses continue.
     * <p>
     * 붙잡은 응답이 계속되게 한다.
     */
    void release() {
        release.countDown();
    }

    /**
     * Returns the number of connections accepted so far.
     * <p>
     * 지금까지 받아들인 연결 수를 반환한다.
     *
     * @return count / 개수
     */
    int getConnections() {
        return connections.get();
    }

    /**
     * Returns the number of clients that closed the connection while it was held.
     * <p>
     * 붙잡혀 있는 동안 연결을 닫은 클라이언트 수를 반환한다.
     *
     * @return count / 개수
     */
    int getClientsClosed() {
        return clientsClosed.get();
    }

    /**
     * Returns the number of responses that were sent completely.
     * <p>
     * 끝까지 보낸 응답 수를 반환한다.
     *
     * @return count / 개수
     */
    int getCompleted() {
        return completed.get();
    }

    /**
     * Stops the server and frees held responses.
     * <p>
     * 서버를 멈추고 붙잡은 응답을 푼다.
     */
    void stop() {
        stopped = true;
        release.countDown();
        try {
            server.close();
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Serves a request: reads the head, sends the first part, holds, then sends the rest.
     * <p>
     * 요청을 처리한다: 머리를 읽고, 앞부분을 보내고, 붙잡은 뒤, 나머지를 보낸다.
     *
     * @param s the connection / 연결
     */
    private void serve(Socket s) {
        try {
            s.setSoTimeout(100);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            int state = 0;
            // 빈 줄(CRLF CRLF)이 나올 때까지 요청 머리를 읽는다.
            while (state < 4) {
                int c = in.read();
                if (c < 0) {
                    return;
                }
                if (c == '\r' && (state == 0 || state == 2)) {
                    state++;
                } else if (c == '\n' && (state == 1 || state == 3)) {
                    state++;
                } else {
                    state = c == '\r' ? 1 : 0;
                }
            }
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + totalSize
                    + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
            out.write(new byte[firstPart]);
            out.flush();
            // 풀어 줄 때까지 기다리며 클라이언트가 연결을 닫는지 살핀다.
            while (release.getCount() > 0) {
                try {
                    if (in.read() < 0) {
                        clientsClosed.incrementAndGet();
                        return;
                    }
                } catch (SocketTimeoutException e) {
                    release.await(10, TimeUnit.MILLISECONDS);
                }
            }
            out.write(new byte[totalSize - firstPart]);
            out.flush();
            completed.incrementAndGet();
        } catch (IOException e) {
            clientsClosed.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                // 닫기 실패는 무시한다.
                return;
            }
        }
    }
}
