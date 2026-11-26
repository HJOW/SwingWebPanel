package org.duckdns.hjow.swingwebpanel.testing;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.SSLContext;

/**
 * Local HTTP/1.1 test server written on plain <code>java.net.ServerSocket</code> (HTTPS on <code>SSLServerSocket</code>
 * when constructed with an {@link SSLContext}); it never uses <code>com.sun.net.httpserver</code>.
 * It listens on 127.0.0.1 with an ephemeral port, serves routes registered by exact path (or prefix when the path ends
 * with <code>*</code>), records every request, and can simulate delays, redirects, chunked and compressed bodies,
 * truncated bodies and dropped connections. Every response carries <code>Connection: close</code>.
 * Call {@link #stop()} (usually in <code>tearDown</code>) to close sockets and join threads.
 * <p>
 * Use: <code>TestHttpServer s = new TestHttpServer(); s.route("/a", handler); s.start(); ... s.url("/a") ... s.stop();</code>
 * <p>
 * 순수 <code>java.net.ServerSocket</code> 으로 작성한 로컬 HTTP/1.1 시험 서버(생성 시 {@link SSLContext} 를 주면
 * <code>SSLServerSocket</code> 으로 HTTPS). <code>com.sun.net.httpserver</code> 는 쓰지 않는다.
 * 127.0.0.1 의 임시 포트에서 대기하며, 정확한 경로(또는 경로가 <code>*</code> 로 끝나면 접두사)로 등록한 경로를 응답하고,
 * 모든 요청을 기록하며, 지연/리디렉션/chunked 및 압축 본문/잘린 본문/연결 끊기를 흉내 낼 수 있다. 모든 응답에
 * <code>Connection: close</code> 를 붙인다. 소켓과 스레드를 정리하려면 {@link #stop()} 을 호출한다(보통 <code>tearDown</code>).
 * <p>
 * 사용: <code>TestHttpServer s = new TestHttpServer(); s.route("/a", handler); s.start(); ... s.url("/a") ... s.stop();</code>
 */
public class TestHttpServer {

    /**
     * Produces the response for a request.
     * <p>
     * 요청에 대한 응답을 만든다.
     */
    public interface Handler {
        /**
         * Handles one request.
         * <p>
         * 요청 하나를 처리한다.
         *
         * @param request the parsed request / 해석된 요청
         * @return the response to send / 보낼 응답
         * @throws Exception any failure; the server answers 500 / 모든 실패(서버는 500 으로 응답)
         */
        Response handle(Request request) throws Exception;
    }

    /**
     * Where a {@link Streamer} writes the pieces of a streamed body.
     * <p>
     * {@link Streamer} 가 스트림 본문의 조각을 쓰는 곳.
     */
    public interface Sink {
        /**
         * Writes a piece as UTF-8 and flushes it to the client at once.
         * <p>
         * 조각을 UTF-8 로 쓰고 곧바로 클라이언트에 flush 한다.
         *
         * @param text the text / 텍스트
         * @throws IOException if the client went away / 클라이언트가 떠났으면
         */
        void write(String text) throws IOException;

        /**
         * Writes a binary body fragment and flushes it. / 이진 본문 조각을 쓰고 전송한다.
         * @param bytes source bytes / 원본 바이트
         * @param offset first byte / 첫 바이트 위치
         * @param length number of bytes / 바이트 수
         * @throws IOException if writing fails / 쓰기에 실패하면
         */
        void write(byte[] bytes, int offset, int length) throws IOException;

        /**
         * Waits; returns false as soon as the server stops, so that a streamer ends with the test.
         * <p>
         * 기다린다. 서버가 멈추면 곧바로 false 를 반환하여 스트리머가 시험과 함께 끝나게 한다.
         *
         * @param ms the milliseconds / 밀리초
         * @return true if the full time passed and the server still runs / 시간이 다 지났고 서버가 계속 동작하면 true
         */
        boolean pause(long ms);

        /**
         * Tells whether the server still runs.
         * <p>
         * 서버가 아직 동작하는지 알려 준다.
         *
         * @return true while running / 동작 중이면 true
         */
        boolean isRunning();
    }

    /**
     * Produces a streamed body piece by piece (see {@link Response#streamer}).
     * <p>
     * 스트림 본문을 조각 단위로 만든다({@link Response#streamer} 참조).
     */
    public interface Streamer {
        /**
         * Writes the body.
         * <p>
         * 본문을 쓴다.
         *
         * @param sink where to write / 쓸 곳
         * @throws Exception any failure drops the connection / 어떤 실패든 연결을 끊는다
         */
        void produce(Sink sink) throws Exception;
    }

    /**
     * A parsed HTTP request as received by the server.
     * <p>
     * 서버가 받은 해석된 HTTP 요청.
     */
    public static final class Request {
        /**
         * Request method such as GET or POST.
         * <p>
         * GET, POST 같은 요청 메서드.
         */
        public final String method;
        /**
         * Raw request target (path and query) as sent on the request line.
         * <p>
         * 요청 줄에 적힌 원래의 요청 대상(경로와 질의).
         */
        public final String target;
        /**
         * Path part of the target (without query).
         * <p>
         * 요청 대상의 경로 부분(질의 제외).
         */
        public final String path;
        /**
         * Query part of the target without '?', or null.
         * <p>
         * '?' 를 뺀 질의 부분, 없으면 null.
         */
        public final String query;
        /**
         * HTTP version text such as HTTP/1.1.
         * <p>
         * HTTP/1.1 같은 버전 문자열.
         */
        public final String version;
        /**
         * Headers by lower-case name (repeated headers joined with ", ").
         * <p>
         * 소문자 이름별 헤더(반복된 헤더는 ", " 로 이음).
         */
        public final Map<String, String> headers;
        /**
         * Headers in arrival order as name/value pairs with the original name case.
         * <p>
         * 도착 순서의 헤더(이름은 원래 대소문자 유지, [이름, 값] 쌍).
         */
        public final List<String[]> headerList;
        /**
         * Decoded request body (empty if none).
         * <p>
         * 해독된 요청 본문(없으면 빈 배열).
         */
        public final byte[] body;
        /**
         * Arrival time in milliseconds.
         * <p>
         * 도착 시각(밀리초).
         */
        public final long time;

        /**
         * Creates a request record.
         * <p>
         * 요청 기록을 만든다.
         *
         * @param method     method / 메서드
         * @param target     raw target / 원래 대상
         * @param version    version text / 버전 문자열
         * @param headerList headers in order / 순서대로의 헤더
         * @param body       body bytes / 본문 바이트
         */
        Request(String method, String target, String version, List<String[]> headerList, byte[] body) {
            this.method = method;
            this.target = target;
            int q = target.indexOf('?');
            this.path = q < 0 ? target : target.substring(0, q);
            this.query = q < 0 ? null : target.substring(q + 1);
            this.version = version;
            this.headerList = headerList;
            Map<String, String> map = new LinkedHashMap<String, String>();
            // 같은 이름의 헤더는 ", " 로 합쳐 소문자 이름으로 보관한다.
            for (int i = 0; i < headerList.size(); i++) {
                String key = headerList.get(i)[0].toLowerCase();
                String old = map.get(key);
                map.put(key, old == null ? headerList.get(i)[1] : old + ", " + headerList.get(i)[1]);
            }
            this.headers = map;
            this.body = body;
            this.time = System.currentTimeMillis();
        }

        /**
         * Returns a header value by case-insensitive name.
         * <p>
         * 대소문자 구분 없이 이름으로 헤더 값을 돌려준다.
         *
         * @param name header name / 헤더 이름
         * @return value or null / 값 또는 null
         */
        public String header(String name) {
            return headers.get(name.toLowerCase());
        }

        /**
         * Returns the body decoded as UTF-8.
         * <p>
         * 본문을 UTF-8 로 해독해 돌려준다.
         *
         * @return body text / 본문 문자열
         */
        public String bodyText() {
            try {
                return new String(body, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * A response description with switches for failure simulation.
     * <p>
     * 실패 흉내용 스위치를 가진 응답 기술.
     */
    public static final class Response {
        /**
         * Status code.
         * <p>
         * 상태 코드.
         */
        public int status;
        /**
         * Reason phrase (null = standard text).
         * <p>
         * 이유 문구(null 이면 표준 문구).
         */
        public String reason;
        /**
         * Response headers as name/value pairs.
         * <p>
         * 응답 헤더([이름, 값] 쌍).
         */
        public final List<String[]> headers = new ArrayList<String[]>();
        /**
         * Response body bytes.
         * <p>
         * 응답 본문 바이트.
         */
        public byte[] body = new byte[0];
        /**
         * Delay before anything is written, in milliseconds.
         * <p>
         * 아무것도 쓰기 전에 기다릴 시간(밀리초).
         */
        public long delayMs;
        /**
         * Send the body with chunked transfer encoding.
         * <p>
         * 본문을 chunked 전송 인코딩으로 보낸다.
         */
        public boolean chunked;
        /**
         * Chunk size in bytes when chunked.
         * <p>
         * chunked 일 때 조각 크기(바이트).
         */
        public int chunkSize = 5;
        /**
         * When chunked, close the connection after the first chunk without the terminating chunk.
         * <p>
         * chunked 일 때 첫 조각 뒤에 종료 조각 없이 연결을 닫는다.
         */
        public boolean abortChunked;
        /**
         * Declare the full Content-Length but send only this many body bytes, then close (-1 = off).
         * <p>
         * Content-Length 는 전체로 선언하되 본문을 이만큼만 보내고 닫는다(-1 이면 끔).
         */
        public int truncateBodyAt = -1;
        /**
         * Close the connection without sending anything.
         * <p>
         * 아무것도 보내지 않고 연결을 닫는다.
         */
        public boolean dropWithoutResponse;
        /**
         * Omit Content-Length and Transfer-Encoding so the body ends when the connection closes.
         * <p>
         * Content-Length 와 Transfer-Encoding 을 생략해 연결이 닫힐 때 본문이 끝나게 한다.
         */
        public boolean noLength;
        /**
         * Produces the body piece by piece (chunked, each piece flushed at once) so that a test controls the timing of a stream such as an
         * event stream; null for an ordinary response. When it returns the body ends normally, when it throws the connection is dropped
         * without the terminating chunk.
         * <p>
         * 본문을 조각 단위로 만든다(chunked, 조각마다 곧바로 flush). 이벤트 스트림 같은 스트림의 타이밍을 시험이 정할 수 있다. 보통 응답이면 null. 반환하면
         * 본문이 정상적으로 끝나고, 예외를 던지면 종료 조각 없이 연결을 끊는다.
         */
        public Streamer streamer;

        /** Declared size of a streamed body, or -1 for chunked transfer. / 스트림 본문의 선언 크기. chunked이면 -1. */
        private long streamedLength = -1;

        /**
         * Creates a response with a status code.
         * <p>
         * 상태 코드를 가진 응답을 만든다.
         *
         * @param status status code / 상태 코드
         */
        public Response(int status) {
            this.status = status;
        }

        /**
         * Adds a header (fluent).
         * <p>
         * 헤더를 추가한다(연쇄 호출용).
         *
         * @param name  header name / 헤더 이름
         * @param value header value / 헤더 값
         * @return this / 자기 자신
         */
        public Response header(String name, String value) {
            headers.add(new String[] { name, value });
            return this;
        }

        /**
         * Sets a UTF-8 text body and Content-Type (fluent).
         * <p>
         * UTF-8 문자열 본문과 Content-Type 을 설정한다(연쇄 호출용).
         *
         * @param contentType Content-Type value / Content-Type 값
         * @param text        body text / 본문 문자열
         * @return this / 자기 자신
         */
        public Response text(String contentType, String text) {
            try {
                this.body = text.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            return header("Content-Type", contentType);
        }

        /**
         * Sets a binary body and Content-Type (fluent).
         * <p>
         * 바이너리 본문과 Content-Type 을 설정한다(연쇄 호출용).
         *
         * @param contentType Content-Type value / Content-Type 값
         * @param data        body bytes / 본문 바이트
         * @return this / 자기 자신
         */
        public Response bytes(String contentType, byte[] data) {
            this.body = data;
            return header("Content-Type", contentType);
        }

        /**
         * Uses chunked transfer encoding (fluent).
         * <p>
         * chunked 전송 인코딩을 사용한다(연쇄 호출용).
         *
         * @param size chunk size / 조각 크기
         * @return this / 자기 자신
         */
        public Response chunked(int size) {
            this.chunked = true;
            this.chunkSize = size;
            return this;
        }

        /**
         * Produces the body piece by piece with the streamer (fluent; see {@link #streamer}).
         * <p>
         * 스트리머로 본문을 조각 단위로 만든다(연쇄 호출용. {@link #streamer} 참조).
         *
         * @param s the streamer / 스트리머
         * @return this / 자기 자신
         */
        public Response streamedBy(Streamer s) {
            return streamedBy(s, -1);
        }

        /**
         * Produces a streamed body with a declared length. / 길이를 선언하는 스트림 본문을 만든다.
         * @param s the streamer / 스트리머
         * @param length declared length, -1 for chunked / 선언 길이. chunked이면 -1
         * @return this response / 이 응답
         */
        public Response streamedBy(Streamer s, long length) {
            this.streamer = s;
            this.streamedLength = length;
            return this;
        }

        /**
         * Delays the response (fluent).
         * <p>
         * 응답을 지연시킨다(연쇄 호출용).
         *
         * @param ms delay in milliseconds / 지연(밀리초)
         * @return this / 자기 자신
         */
        public Response delay(long ms) {
            this.delayMs = ms;
            return this;
        }

        /**
         * Truncates the body on the wire (fluent).
         * <p>
         * 전송 중에 본문을 자른다(연쇄 호출용).
         *
         * @param bytes number of body bytes actually sent / 실제로 보낼 본문 바이트 수
         * @return this / 자기 자신
         */
        public Response truncateAt(int bytes) {
            this.truncateBodyAt = bytes;
            return this;
        }

        /**
         * Compresses the current body with gzip and sets Content-Encoding (fluent).
         * <p>
         * 현재 본문을 gzip 으로 압축하고 Content-Encoding 을 설정한다(연쇄 호출용).
         *
         * @return this / 자기 자신
         */
        public Response gzip() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                GZIPOutputStream gz = new GZIPOutputStream(bos);
                gz.write(body);
                gz.close();
                this.body = bos.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return header("Content-Encoding", "gzip");
        }

        /**
         * Compresses the current body as deflate and sets Content-Encoding (fluent).
         * <p>
         * 현재 본문을 deflate 로 압축하고 Content-Encoding 을 설정한다(연쇄 호출용).
         *
         * @param zlibWrapped true for the zlib format (what HTTP specifies), false for raw deflate / zlib 형식(HTTP 규격)이면 true, raw deflate 면 false
         * @return this / 자기 자신
         */
        public Response deflate(boolean zlibWrapped) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                java.util.zip.Deflater def = new java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, !zlibWrapped);
                DeflaterOutputStream out = new DeflaterOutputStream(bos, def);
                out.write(body);
                out.close();
                def.end();
                this.body = bos.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return header("Content-Encoding", "deflate");
        }

        /**
         * Creates a 200 response with a text body.
         * <p>
         * 문자열 본문을 가진 200 응답을 만든다.
         *
         * @param contentType Content-Type value / Content-Type 값
         * @param text        body text / 본문 문자열
         * @return the response / 응답
         */
        public static Response ok(String contentType, String text) {
            return new Response(200).text(contentType, text);
        }

        /**
         * Creates an HTML 200 response (UTF-8).
         * <p>
         * HTML 200 응답(UTF-8)을 만든다.
         *
         * @param html HTML text / HTML 문자열
         * @return the response / 응답
         */
        public static Response html(String html) {
            return ok("text/html; charset=UTF-8", html);
        }

        /**
         * Creates a redirect response.
         * <p>
         * 리디렉션 응답을 만든다.
         *
         * @param status   3xx status / 3xx 상태 코드
         * @param location Location header value / Location 헤더 값
         * @return the response / 응답
         */
        public static Response redirect(int status, String location) {
            return new Response(status).header("Location", location).text("text/plain; charset=UTF-8", "redirect");
        }
    }

    /**
     * Maximum request line or header line length accepted.
     * <p>
     * 허용하는 요청 줄/헤더 줄의 최대 길이.
     */
    private static final int MAX_LINE = 16384;

    /**
     * TLS context for HTTPS mode, or null for plain HTTP.
     * <p>
     * HTTPS 모드의 TLS 컨텍스트, 평문 HTTP 면 null.
     */
    private final SSLContext sslContext;
    /**
     * Registered routes in registration order.
     * <p>
     * 등록 순서대로의 경로 목록.
     */
    private final Map<String, Handler> routes = new LinkedHashMap<String, Handler>();
    /**
     * Request log.
     * <p>
     * 요청 기록.
     */
    private final List<Request> log = new ArrayList<Request>();
    /**
     * Sockets that are open now, closed by {@link #stop()}.
     * <p>
     * 지금 열려 있는 소켓. {@link #stop()} 이 닫는다.
     */
    private final List<Socket> openSockets = new ArrayList<Socket>();
    /**
     * Connection threads started so far.
     * <p>
     * 지금까지 시작한 연결 스레드.
     */
    private final List<Thread> workers = new ArrayList<Thread>();
    /**
     * Listening socket.
     * <p>
     * 대기 소켓.
     */
    private ServerSocket server;
    /**
     * Accept loop thread.
     * <p>
     * 접속 수락 반복 스레드.
     */
    private Thread acceptThread;
    /**
     * Whether the server runs.
     * <p>
     * 서버가 동작 중인지 여부.
     */
    private volatile boolean running;
    /**
     * Number of connections that failed before a request was read (for example TLS handshake errors).
     * <p>
     * 요청을 읽기 전에 실패한 연결 수(예: TLS 핸드셰이크 오류).
     */
    private volatile int failedConnections;

    /**
     * Creates a plain HTTP server.
     * <p>
     * 평문 HTTP 서버를 만든다.
     */
    public TestHttpServer() {
        this(null);
    }

    /**
     * Creates a server; with a non-null context it speaks HTTPS.
     * <p>
     * 서버를 만든다. 컨텍스트가 null 이 아니면 HTTPS 로 동작한다.
     *
     * @param sslContext server TLS context or null / 서버 TLS 컨텍스트 또는 null
     */
    public TestHttpServer(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * Registers a handler for an exact path, or a prefix when the path ends with '*'.
     * <p>
     * 정확한 경로(경로가 '*' 로 끝나면 접두사)에 처리기를 등록한다.
     *
     * @param path    path pattern / 경로 패턴
     * @param handler handler / 처리기
     */
    public synchronized void route(String path, Handler handler) {
        routes.put(path, handler);
    }

    /**
     * Registers a fixed response for a path; a fresh copy of the headers is sent each time.
     * <p>
     * 경로에 고정 응답을 등록한다. 헤더는 매번 새로 복사해 보낸다.
     *
     * @param path     path pattern / 경로 패턴
     * @param response response template / 응답 견본
     */
    public void routeFixed(String path, final Response response) {
        route(path, new Handler() {
            /**
             * Returns the fixed response.
             * <p>
             * 고정 응답을 돌려준다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public Response handle(Request request) {
                return response;
            }
        });
    }

    /**
     * Starts listening on 127.0.0.1 with an ephemeral port.
     * <p>
     * 127.0.0.1 의 임시 포트에서 대기를 시작한다.
     *
     * @throws IOException when the socket cannot be opened / 소켓을 열 수 없을 때
     */
    public synchronized void start() throws IOException {
        InetAddress loop = InetAddress.getByName("127.0.0.1");
        if (sslContext != null) {
            server = sslContext.getServerSocketFactory().createServerSocket(0, 50, loop);
        } else {
            server = new ServerSocket(0, 50, loop);
        }
        running = true;
        acceptThread = new Thread(new Runnable() {
            /**
             * Accepts connections until stopped.
             * <p>
             * 중지될 때까지 접속을 수락한다.
             */
            public void run() {
                acceptLoop();
            }
        }, "TestHttpServer-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Returns the listening port.
     * <p>
     * 대기 포트를 돌려준다.
     *
     * @return port number / 포트 번호
     */
    public int getPort() {
        return server.getLocalPort();
    }

    /**
     * Builds an absolute URL for a path. HTTPS servers use the host name localhost (needed by the certificate).
     * <p>
     * 경로의 절대 URL 을 만든다. HTTPS 서버는 인증서에 맞도록 호스트 이름 localhost 를 쓴다.
     *
     * @param path path starting with '/' / '/' 로 시작하는 경로
     * @return absolute URL / 절대 URL
     */
    public String url(String path) {
        return (sslContext != null ? "https" : "http") + "://localhost:" + getPort() + path;
    }

    /**
     * Returns a copy of the request log.
     * <p>
     * 요청 기록의 복사본을 돌려준다.
     *
     * @return requests in arrival order / 도착 순서의 요청
     */
    public synchronized List<Request> requests() {
        return new ArrayList<Request>(log);
    }

    /**
     * Counts logged requests for a path (exact match on the path part).
     * <p>
     * 경로(경로 부분이 정확히 일치)의 기록된 요청 수를 센다.
     *
     * @param path path / 경로
     * @return count / 개수
     */
    public synchronized int count(String path) {
        int n = 0;
        // 기록을 모두 훑어 경로가 같은 요청만 센다.
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).path.equals(path)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the total number of logged requests.
     * <p>
     * 기록된 전체 요청 수를 돌려준다.
     *
     * @return count / 개수
     */
    public synchronized int totalRequests() {
        return log.size();
    }

    /**
     * Returns the last logged request for a path, or null.
     * <p>
     * 경로의 마지막 기록 요청을 돌려준다. 없으면 null.
     *
     * @param path path / 경로
     * @return request or null / 요청 또는 null
     */
    public synchronized Request last(String path) {
        // 뒤에서부터 찾아 가장 최근 요청을 돌려준다.
        for (int i = log.size() - 1; i >= 0; i--) {
            if (log.get(i).path.equals(path)) {
                return log.get(i);
            }
        }
        return null;
    }

    /**
     * Returns the number of connections that failed before a request was read.
     * <p>
     * 요청을 읽기 전에 실패한 연결 수를 돌려준다.
     *
     * @return count / 개수
     */
    public int getFailedConnections() {
        return failedConnections;
    }

    /**
     * Returns the number of connections that are open now (accepted and not yet closed by the server).
     * <p>
     * 지금 열려 있는(수락했고 서버가 아직 닫지 않은) 연결 수를 돌려준다.
     *
     * @return count / 개수
     */
    public synchronized int getOpenConnections() {
        return openSockets.size();
    }

    /**
     * Waits until at least n requests were logged.
     * <p>
     * 요청이 n 개 이상 기록될 때까지 기다린다.
     *
     * @param n         wanted count / 원하는 개수
     * @param timeoutMs maximum wait / 최대 대기(밀리초)
     * @return true if reached / 도달했으면 true
     * @throws InterruptedException when interrupted / 인터럽트될 때
     */
    public boolean awaitRequests(int n, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        // 짧게 쉬면서 목표 개수에 도달할 때까지 확인한다.
        while (System.currentTimeMillis() < end) {
            if (totalRequests() >= n) {
                return true;
            }
            Thread.sleep(10);
        }
        return totalRequests() >= n;
    }

    /**
     * Stops the server: closes the listening socket and all connections and joins the threads.
     * <p>
     * 서버를 멈춘다. 대기 소켓과 모든 연결을 닫고 스레드를 합류시킨다.
     */
    public void stop() {
        List<Socket> sockets;
        List<Thread> threads;
        synchronized (this) {
            running = false;
            sockets = new ArrayList<Socket>(openSockets);
            threads = new ArrayList<Thread>(workers);
        }
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            // 이미 닫힌 소켓은 무시한다.
        }
        // 열린 연결은 모두 강제로 닫아 읽기 대기를 풀어 준다.
        for (int i = 0; i < sockets.size(); i++) {
            try {
                sockets.get(i).close();
            } catch (IOException e) {
                // 무시한다.
            }
        }
        try {
            if (acceptThread != null) {
                acceptThread.join(3000);
            }
            // 모든 연결 스레드가 끝나기를 차례로 기다린다.
            for (int i = 0; i < threads.size(); i++) {
                threads.get(i).join(3000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Accept loop body.
     * <p>
     * 접속 수락 반복의 본문.
     */
    private void acceptLoop() {
        // 서버가 동작하는 동안 접속마다 처리 스레드를 만든다.
        while (running) {
            final Socket socket;
            try {
                socket = server.accept();
            } catch (IOException e) {
                return;
            }
            Thread t = new Thread(new Runnable() {
                /**
                 * Serves one connection.
                 * <p>
                 * 연결 하나를 처리한다.
                 */
                public void run() {
                    serve(socket);
                }
            }, "TestHttpServer-conn");
            t.setDaemon(true);
            synchronized (this) {
                openSockets.add(socket);
                workers.add(t);
            }
            t.start();
        }
    }

    /**
     * Reads one request from a socket, answers it and closes the socket.
     * <p>
     * 소켓에서 요청 하나를 읽어 응답하고 소켓을 닫는다.
     *
     * @param socket client socket / 클라이언트 소켓
     */
    private void serve(Socket socket) {
        try {
            socket.setSoTimeout(30000);
            InputStream in = new BufferedInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            // 평문 서버가 TLS 핸드셰이크(0x16)를 받으면 곧바로 400 을 돌려 클라이언트가 오래 기다리지 않게 한다.
            if (sslContext == null) {
                in.mark(1);
                int first = in.read();
                in.reset();
                if (first == 0x16) {
                    out.write("HTTP/1.1 400 Bad Request\r\nConnection: close\r\nContent-Length: 0\r\n\r\n".getBytes("ISO-8859-1"));
                    out.flush();
                    return;
                }
            }
            Request req = readRequest(in);
            if (req == null) {
                failedConnections++;
                return;
            }
            synchronized (this) {
                log.add(req);
            }
            Response resp = dispatch(req);
            writeResponse(out, req, resp);
        } catch (IOException e) {
            failedConnections++;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // 무시한다.
            }
            synchronized (this) {
                openSockets.remove(socket);
            }
        }
    }

    /**
     * Finds the handler for a request and runs it.
     * <p>
     * 요청에 맞는 처리기를 찾아 실행한다.
     *
     * @param req request / 요청
     * @return response / 응답
     */
    private Response dispatch(Request req) {
        Handler handler = null;
        synchronized (this) {
            handler = routes.get(req.path);
            // 정확한 경로가 없으면 '*' 로 끝나는 접두사 경로에서 가장 먼저 맞는 것을 찾는다.
            if (handler == null) {
                for (Map.Entry<String, Handler> e : routes.entrySet()) {
                    String k = e.getKey();
                    if (k.endsWith("*") && req.path.startsWith(k.substring(0, k.length() - 1))) {
                        handler = e.getValue();
                        break;
                    }
                }
            }
        }
        if (handler == null) {
            return new Response(404).text("text/plain; charset=UTF-8", "not found: " + req.path);
        }
        try {
            Response r = handler.handle(req);
            return r == null ? new Response(204) : r;
        } catch (Exception e) {
            return new Response(500).text("text/plain; charset=UTF-8", "handler failure: " + e);
        }
    }

    /**
     * Reads a CRLF (or LF) terminated line in ISO-8859-1.
     * <p>
     * CRLF(또는 LF)로 끝나는 한 줄을 ISO-8859-1 로 읽는다.
     *
     * @param in input / 입력
     * @return the line without terminator, or null at end of stream / 종결자를 뺀 줄, 스트림 끝이면 null
     * @throws IOException on read failure or an over-long line / 읽기 실패 또는 너무 긴 줄
     */
    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        // 줄 끝(LF)을 만날 때까지 한 글자씩 읽는다.
        while ((c = in.read()) >= 0) {
            if (c == '\n') {
                int len = sb.length();
                if (len > 0 && sb.charAt(len - 1) == '\r') {
                    sb.setLength(len - 1);
                }
                return sb.toString();
            }
            sb.append((char) c);
            if (sb.length() > MAX_LINE) {
                throw new IOException("line too long");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Parses a request (line, headers and body).
     * <p>
     * 요청(요청 줄, 헤더, 본문)을 해석한다.
     *
     * @param in input / 입력
     * @return request or null when the connection closed before a request line / 요청 줄 전에 연결이 닫히면 null
     * @throws IOException on malformed input / 잘못된 입력
     */
    private Request readRequest(InputStream in) throws IOException {
        String line = readLine(in);
        if (line == null || line.length() == 0) {
            return null;
        }
        String[] parts = line.split(" ", 3);
        if (parts.length < 3) {
            throw new IOException("bad request line: " + line);
        }
        List<String[]> headers = new ArrayList<String[]>();
        // 빈 줄이 나올 때까지 헤더 줄을 읽는다.
        while (true) {
            String h = readLine(in);
            if (h == null) {
                throw new IOException("eof in headers");
            }
            if (h.length() == 0) {
                break;
            }
            int colon = h.indexOf(':');
            if (colon > 0) {
                headers.add(new String[] { h.substring(0, colon).trim(), h.substring(colon + 1).trim() });
            }
        }
        byte[] body = new byte[0];
        String te = null;
        String cl = null;
        // 헤더를 훑으며 본문 길이를 정하는 두 헤더를 찾는다.
        for (int i = 0; i < headers.size(); i++) {
            String n = headers.get(i)[0];
            if ("transfer-encoding".equalsIgnoreCase(n)) {
                te = headers.get(i)[1];
            } else if ("content-length".equalsIgnoreCase(n)) {
                cl = headers.get(i)[1];
            }
        }
        if (te != null && te.toLowerCase().indexOf("chunked") >= 0) {
            body = readChunkedBody(in);
        } else if (cl != null) {
            int len = Integer.parseInt(cl.trim());
            body = new byte[len];
            int off = 0;
            // 선언된 길이만큼 본문을 모두 읽는다.
            while (off < len) {
                int n = in.read(body, off, len - off);
                if (n < 0) {
                    throw new IOException("eof in body");
                }
                off += n;
            }
        }
        return new Request(parts[0], parts[1], parts[2], headers, body);
    }

    /**
     * Reads a chunked request body.
     * <p>
     * chunked 요청 본문을 읽는다.
     *
     * @param in input / 입력
     * @return decoded body / 해독된 본문
     * @throws IOException on malformed input / 잘못된 입력
     */
    private static byte[] readChunkedBody(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 크기 0 인 조각이 나올 때까지 조각을 읽는다.
        while (true) {
            String sizeLine = readLine(in);
            if (sizeLine == null) {
                throw new IOException("eof in chunked body");
            }
            int semi = sizeLine.indexOf(';');
            if (semi >= 0) {
                sizeLine = sizeLine.substring(0, semi);
            }
            int size = Integer.parseInt(sizeLine.trim(), 16);
            if (size == 0) {
                // 트레일러가 있으면 빈 줄까지 읽어 버린다.
                String t;
                while ((t = readLine(in)) != null && t.length() > 0) {
                    // 트레일러 줄은 무시한다.
                }
                break;
            }
            byte[] chunk = new byte[size];
            int off = 0;
            // 조각 크기만큼 모두 읽을 때까지 반복한다.
            while (off < size) {
                int n = in.read(chunk, off, size - off);
                if (n < 0) {
                    throw new IOException("eof in chunk");
                }
                off += n;
            }
            bos.write(chunk);
            readLine(in);
        }
        return bos.toByteArray();
    }

    /**
     * Writes a response, applying the simulation switches.
     * <p>
     * 응답을 쓴다. 흉내 스위치를 적용한다.
     *
     * @param out  output / 출력
     * @param req  the request (HEAD gets no body) / 요청(HEAD 에는 본문을 보내지 않음)
     * @param resp response / 응답
     * @throws IOException on write failure / 쓰기 실패 시
     */
    private void writeResponse(OutputStream out, Request req, Response resp) throws IOException {
        // 지연이 지정되면 쓰기 전에 조금씩 쉬며 서버 중지를 확인한다.
        long end = System.currentTimeMillis() + resp.delayMs;
        while (running && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(Math.min(20, Math.max(1, end - System.currentTimeMillis())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (resp.dropWithoutResponse) {
            return;
        }
        boolean head = "HEAD".equals(req.method);
        StringBuilder sb = new StringBuilder();
        String reason = resp.reason != null ? resp.reason : reasonOf(resp.status);
        sb.append("HTTP/1.1 ").append(resp.status).append(' ').append(reason).append("\r\n");
        // 응답 헤더를 그대로 쓴다.
        for (int i = 0; i < resp.headers.size(); i++) {
            sb.append(resp.headers.get(i)[0]).append(": ").append(resp.headers.get(i)[1]).append("\r\n");
        }
        sb.append("Connection: close\r\n");
        if (resp.streamer != null && (!head || resp.streamedLength >= 0)) {
            boolean chunked = resp.streamedLength < 0;
            if (chunked) {
                sb.append("Transfer-Encoding: chunked\r\n");
            } else {
                sb.append("Content-Length: ").append(resp.streamedLength).append("\r\n");
            }
            sb.append("\r\n");
            out.write(sb.toString().getBytes("UTF-8"));
            out.flush();
            if (!head) {
                writeStreamed(out, resp.streamer, chunked);
            }
            return;
        }
        if (resp.chunked && !head) {
            sb.append("Transfer-Encoding: chunked\r\n");
        } else if (!resp.noLength) {
            sb.append("Content-Length: ").append(resp.body.length).append("\r\n");
        }
        sb.append("\r\n");
        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
        if (head) {
            return;
        }
        if (resp.chunked) {
            int size = Math.max(1, resp.chunkSize);
            int off = 0;
            // 본문을 일정 크기의 조각으로 나누어 보낸다.
            while (off < resp.body.length) {
                int n = Math.min(size, resp.body.length - off);
                out.write((Integer.toHexString(n) + "\r\n").getBytes("ISO-8859-1"));
                out.write(resp.body, off, n);
                out.write("\r\n".getBytes("ISO-8859-1"));
                out.flush();
                off += n;
                if (resp.abortChunked) {
                    return;
                }
            }
            out.write("0\r\n\r\n".getBytes("ISO-8859-1"));
            out.flush();
        } else {
            int n = resp.truncateBodyAt >= 0 ? Math.min(resp.truncateBodyAt, resp.body.length) : resp.body.length;
            out.write(resp.body, 0, n);
            out.flush();
        }
    }

    /**
     * Runs a streamer against the output: each written piece becomes one flushed chunk; a normal return ends the body with the terminating chunk,
     * an exception drops the connection without it.
     * <p>
     * 스트리머를 출력에 대해 실행한다. 쓴 조각마다 flush 된 chunk 하나가 되고, 정상 반환하면 종료 조각으로 본문을 끝내며, 예외가 나면 종료 조각 없이 연결을 끊는다.
     *
     * @param out      output / 출력
     * @param streamer the streamer / 스트리머
     * @param chunked whether to encode chunks / chunked 인코딩 여부
     * @throws IOException on write failure / 쓰기 실패 시
     */
    private void writeStreamed(final OutputStream out, Streamer streamer, final boolean chunked) throws IOException {
        Sink sink = new Sink() {
            /**
             * Writes one chunk.
             * <p>
             * chunk 하나를 쓴다.
             *
             * @param text the text / 텍스트
             * @throws IOException if the client went away / 클라이언트가 떠났으면
             */
            public void write(String text) throws IOException {
                byte[] data = text.getBytes("UTF-8");
                write(data, 0, data.length);
            }

            /** Writes and flushes a nonempty binary fragment. / 비어 있지 않은 이진 조각을 쓰고 전송한다. */
            public void write(byte[] data, int offset, int length) throws IOException {
                if (length == 0) {
                    return;
                }
                if (chunked) {
                    out.write((Integer.toHexString(length) + "\r\n").getBytes("ISO-8859-1"));
                }
                out.write(data, offset, length);
                if (chunked) {
                    out.write("\r\n".getBytes("ISO-8859-1"));
                }
                out.flush();
            }

            /**
             * Waits in small steps so that stopping the server ends the wait.
             * <p>
             * 서버를 멈추면 기다림이 끝나도록 짧게 나누어 기다린다.
             *
             * @param ms the milliseconds / 밀리초
             * @return true if the full time passed / 시간이 다 지났으면 true
             */
            public boolean pause(long ms) {
                long end = System.currentTimeMillis() + ms;
                // 목표 시간까지 짧게 쉬며 서버가 아직 동작하는지 확인한다.
                while (running && System.currentTimeMillis() < end) {
                    try {
                        Thread.sleep(Math.min(10, Math.max(1, end - System.currentTimeMillis())));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                return running;
            }

            /**
             * Tells whether the server runs.
             * <p>
             * 서버가 동작하는지 알려 준다.
             *
             * @return true while running / 동작 중이면 true
             */
            public boolean isRunning() {
                return running;
            }
        };
        try {
            streamer.produce(sink);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("streamer failed: " + e);
        }
        if (chunked) {
            out.write("0\r\n\r\n".getBytes("ISO-8859-1"));
        }
        out.flush();
    }

    /**
     * Standard reason phrase for common status codes.
     * <p>
     * 흔한 상태 코드의 표준 이유 문구.
     *
     * @param status status code / 상태 코드
     * @return reason phrase / 이유 문구
     */
    private static String reasonOf(int status) {
        switch (status) {
        case 200: return "OK";
        case 201: return "Created";
        case 204: return "No Content";
        case 301: return "Moved Permanently";
        case 302: return "Found";
        case 303: return "See Other";
        case 304: return "Not Modified";
        case 307: return "Temporary Redirect";
        case 308: return "Permanent Redirect";
        case 400: return "Bad Request";
        case 401: return "Unauthorized";
        case 403: return "Forbidden";
        case 404: return "Not Found";
        case 500: return "Internal Server Error";
        case 503: return "Service Unavailable";
        default: return "Status " + status;
        }
    }
}
