package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.KeySchedule13;

/**
 * One TLS client connection over a pair of streams: it runs the handshake, then carries application data, answers the messages that
 * may follow a handshake (TLS 1.3: NewSessionTicket is ignored, KeyUpdate is processed; TLS 1.2: a HelloRequest is refused with a
 * <code>no_renegotiation</code> warning) and handles alerts and closure. Reading and writing may happen on different threads; the
 * handshake is run once by whichever thread needs it first. A local protocol error sends a fatal alert before the exception is thrown.
 * Example:
 * <pre>
 * Connection c = new Connection(in, out, parameters, "www.example.com");
 * c.handshake();
 * c.write(request, 0, request.length);
 * int n = c.read(buffer, 0, buffer.length);   // -1 after the close_notify of the server
 * c.close();
 * </pre>
 * <p>
 * 스트림 쌍 위의 TLS 클라이언트 연결 하나: 핸드셰이크를 수행한 뒤 응용 데이터를 나르고, 핸드셰이크 뒤에 올 수 있는 메시지에 답하며(TLS 1.3:
 * NewSessionTicket 은 무시, KeyUpdate 는 처리; TLS 1.2: HelloRequest 는 <code>no_renegotiation</code> 경고로 거절) 경보와 종료를 처리한다. 읽기와
 * 쓰기는 서로 다른 스레드에서 할 수 있고, 핸드셰이크는 먼저 필요로 한 스레드가 한 번 수행한다. 이쪽에서 발견한 프로토콜 오류는 예외를 던지기 전에
 * 치명적 경보를 보낸다.
 */
public final class Connection {

    /** The record layer. / 레코드 계층. */
    private final RecordLayer records;

    /** The handshake message reader and writer. / 핸드셰이크 메시지 읽기·쓰기 도구. */
    private final HandshakeIo io;

    /** The handshake. / 핸드셰이크. */
    private final ClientHandshake handshake;

    /** Serializes the handshake. / 핸드셰이크를 직렬화한다. */
    private final ReentrantLock handshakeLock = new ReentrantLock();

    /** Serializes reading. / 읽기를 직렬화한다. */
    private final ReentrantLock readLock = new ReentrantLock();

    /** Serializes writing. / 쓰기를 직렬화한다. */
    private final ReentrantLock writeLock = new ReentrantLock();

    /** True once the handshake succeeded. / 핸드셰이크가 성공했으면 true. */
    private volatile boolean established;

    /** The failure that ended the connection, or null. / 연결을 끝낸 실패. 없으면 null. */
    private volatile IOException failure;

    /** Application data received but not yet read. / 받았지만 아직 읽지 않은 응용 데이터. */
    private byte[] buffer = new byte[0];

    /** The read position in {@link #buffer}. / {@link #buffer} 의 읽기 위치. */
    private int bufferPos;

    /** True once the server closed its side (close_notify or end of stream). / 서버가 자기 쪽을 닫았으면 true(close_notify 또는 스트림 끝). */
    private volatile boolean inboundClosed;

    /** True once this side sent close_notify or a fatal alert. / 이쪽에서 close_notify 나 치명적 경보를 보냈으면 true. */
    private volatile boolean outboundClosed;

    /**
     * Creates a connection with a fresh handshake.
     * <p>
     * 새 핸드셰이크를 가진 연결을 만든다.
     *
     * @param in         the stream from the server / 서버에서 오는 스트림
     * @param out        the stream to the server / 서버로 가는 스트림
     * @param parameters the settings / 설정
     * @param host       the host that the client connects to / 클라이언트가 연결하는 호스트
     */
    public Connection(InputStream in, OutputStream out, Parameters parameters, String host) {
        this.records = new RecordLayer(in, out);
        this.io = new HandshakeIo(records);
        this.handshake = new ClientHandshake(records, io, parameters, host);
    }

    /**
     * Creates a connection around a prepared handshake (tests that replay recorded traces).
     * <p>
     * 준비된 핸드셰이크를 감싼 연결을 만든다(기록된 추적을 재현하는 시험).
     *
     * @param records   the record layer / 레코드 계층
     * @param io        the handshake message reader and writer / 핸드셰이크 메시지 읽기·쓰기 도구
     * @param handshake the handshake / 핸드셰이크
     */
    public Connection(RecordLayer records, HandshakeIo io, ClientHandshake handshake) {
        this.records = records;
        this.io = io;
        this.handshake = handshake;
    }

    /**
     * Runs the handshake if it has not run yet. If it failed before, the same failure is thrown again.
     * <p>
     * 아직 수행하지 않았으면 핸드셰이크를 수행한다. 앞서 실패했으면 같은 실패를 다시 던진다.
     *
     * @throws IOException    on a read or write failure / 읽기·쓰기 실패 시
     * @throws Tls13Exception if the handshake fails / 핸드셰이크가 실패하면
     */
    public void handshake() throws IOException {
        if (established) {
            return;
        }
        handshakeLock.lock();
        try {
            if (established) {
                return;
            }
            if (failure != null) {
                throw failure;
            }
            try {
                handshake.run();
                records.setDropPlainChangeCipherSpec(false);
                established = true;
            } catch (IOException e) {
                fail(e);
                throw e;
            } catch (RuntimeException e) {
                // 방어: 해석기의 검사를 빠져나간 입력도 프로토콜 오류로 끝낸다.
                IOException wrapped = Tls13Exception.protocol(Tls13Exception.INTERNAL_ERROR, "unexpected " + e);
                wrapped.initCause(e);
                fail(wrapped);
                throw wrapped;
            }
        } finally {
            handshakeLock.unlock();
        }
    }

    /**
     * Records a failure and, if this side detected it, tells the server with a fatal alert.
     * <p>
     * 실패를 기록하고, 이쪽에서 발견한 것이면 치명적 경보로 서버에 알린다.
     *
     * @param e the failure / 실패
     */
    private void fail(IOException e) {
        if (failure == null) {
            failure = e;
        }
        if (e instanceof Tls13Exception && !((Tls13Exception) e).received()) {
            sendAlert(2, ((Tls13Exception) e).alert());
        }
        inboundClosed = true;
    }

    /**
     * Sends an alert if the write lock is free; never blocks on a writer that is stuck.
     * <p>
     * 쓰기 잠금이 비어 있으면 경보를 보낸다. 멈춰 있는 쓰기 스레드를 기다리지 않는다.
     *
     * @param level       1 for a warning, 2 for a fatal alert / 경고는 1, 치명적 경보는 2
     * @param description the alert description / 경보 설명
     */
    private void sendAlert(int level, int description) {
        if (outboundClosed || !writeLock.tryLock()) {
            return;
        }
        try {
            if (level == 2 || description == Tls13Exception.CLOSE_NOTIFY) {
                outboundClosed = true;
            }
            records.write(RecordLayer.ALERT, new byte[] { (byte) level, (byte) description }, 0, 2);
        } catch (IOException e) {
            // 경보를 보내지 못해도 원래의 실패가 이미 보고된다.
            outboundClosed = true;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Reads application data.
     * <p>
     * 응용 데이터를 읽는다.
     *
     * @param b   the destination / 대상
     * @param off the start / 시작
     * @param len the largest number of bytes / 최대 바이트 수
     * @return the number of bytes read, or -1 after the server closed the connection / 읽은 바이트 수. 서버가 연결을 닫은 뒤에는 -1
     * @throws IOException    on a read failure / 읽기 실패 시
     * @throws Tls13Exception on a fatal alert or a protocol error / 치명적 경보 또는 프로토콜 오류
     */
    public int read(byte[] b, int off, int len) throws IOException {
        handshake();
        if (len == 0) {
            return 0;
        }
        readLock.lock();
        try {
            // 응용 데이터가 생기거나 연결이 끝날 때까지 레코드를 읽는다.
            while (bufferPos >= buffer.length) {
                if (failure != null) {
                    throw failure;
                }
                if (inboundClosed) {
                    return -1;
                }
                try {
                    RecordLayer.Record r = records.read();
                    if (r == null) {
                        // close_notify 없이 끝난 연결도 스트림 끝으로 돌려준다(HTTP 는 길이로 본문 끝을 판정한다).
                        inboundClosed = true;
                        return -1;
                    }
                    process(r);
                } catch (Tls13Exception e) {
                    fail(e);
                    throw e;
                }
            }
            int n = Math.min(len, buffer.length - bufferPos);
            System.arraycopy(buffer, bufferPos, b, off, n);
            bufferPos += n;
            return n;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the number of application data bytes that can be read without waiting for the network.
     * <p>
     * 네트워크를 기다리지 않고 읽을 수 있는 응용 데이터 바이트 수를 반환한다.
     *
     * @return the count / 개수
     */
    public int available() {
        return established ? Math.max(0, buffer.length - bufferPos) : 0;
    }

    /**
     * Handles one record that arrived after the handshake.
     * <p>
     * 핸드셰이크 뒤에 온 레코드 하나를 처리한다.
     *
     * @param r the record / 레코드
     * @throws IOException on a failure / 실패 시
     */
    private void process(RecordLayer.Record r) throws IOException {
        if (r.type == RecordLayer.APPLICATION_DATA) {
            buffer = r.data;
            bufferPos = 0;
        } else if (r.type == RecordLayer.ALERT) {
            if (r.data.length != 2) {
                throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "malformed alert");
            }
            int description = r.data[1] & 0xff;
            if (description == Tls13Exception.CLOSE_NOTIFY) {
                inboundClosed = true;
                sendAlert(1, Tls13Exception.CLOSE_NOTIFY);
            } else if (description == Tls13Exception.USER_CANCELED || (!handshake.isTls13() && r.data[0] == 1)) {
                // 경고 수준의 경보는 무시한다(TLS 1.3 에서는 user_canceled 만 경고이다).
                return;
            } else {
                inboundClosed = true;
                throw new Tls13Exception(Tls13Exception.Kind.PEER_ALERT, description, true, "the server sent the alert " + Tls13Exception.alertName(description));
            }
        } else if (r.type == RecordLayer.HANDSHAKE) {
            io.feed(r.data);
            HandshakeIo.Message m;
            // 레코드에 든 핸드셰이크 메시지를 차례로 처리한다.
            while ((m = io.take()) != null) {
                postHandshake(m);
            }
        } else {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "record of type " + r.type + " after the handshake");
        }
    }

    /**
     * Handles a handshake message that arrived after the handshake.
     * <p>
     * 핸드셰이크 뒤에 온 핸드셰이크 메시지를 처리한다.
     *
     * @param m the message / 메시지
     * @throws IOException on a failure / 실패 시
     */
    private void postHandshake(HandshakeIo.Message m) throws IOException {
        if (!handshake.isTls13()) {
            if (m.type == HandshakeIo.HELLO_REQUEST && m.body.length == 0) {
                // 재협상은 하지 않는다. 경고로 거절하고 연결은 유지한다(RFC 5246 7.2.2절).
                sendAlert(1, Tls13Exception.NO_RENEGOTIATION);
                return;
            }
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "handshake message " + m.type + " after the handshake");
        }
        if (m.type == HandshakeIo.NEW_SESSION_TICKET) {
            // 세션 재개를 하지 않으므로 티켓은 버린다.
            return;
        }
        if (m.type != HandshakeIo.KEY_UPDATE || m.body.length != 1 || (m.body[0] != 0 && m.body[0] != 1)) {
            throw Tls13Exception.protocol(m.type == HandshakeIo.KEY_UPDATE ? Tls13Exception.ILLEGAL_PARAMETER : Tls13Exception.UNEXPECTED_MESSAGE,
                    "handshake message " + m.type + " after the handshake");
        }
        // KeyUpdate: 서버의 다음 트래픽 비밀로 읽기 키를 바꾼다. 키는 메시지 경계에서만 바뀐다.
        io.expectBoundary();
        CipherSuite s = handshake.suite();
        HashAlg hash = s.hash();
        byte[] next = KeySchedule13.nextTrafficSecret(hash, handshake.serverApplicationSecret());
        handshake.setApplicationSecret(false, next);
        records.setReadProtection(s, true, KeySchedule13.trafficKey(hash, next, s.keyLength()), KeySchedule13.trafficIv(hash, next));
        if (m.body[0] == 1) {
            // update_requested: 현재 키로 KeyUpdate(update_not_requested)를 보낸 뒤 쓰기 키를 바꾼다.
            writeLock.lock();
            try {
                byte[] reply = HandshakeIo.encode(HandshakeIo.KEY_UPDATE, new byte[] { 0 });
                records.write(RecordLayer.HANDSHAKE, reply, 0, reply.length);
                byte[] mine = KeySchedule13.nextTrafficSecret(hash, handshake.clientApplicationSecret());
                handshake.setApplicationSecret(true, mine);
                records.setWriteProtection(s, true, KeySchedule13.trafficKey(hash, mine, s.keyLength()), KeySchedule13.trafficIv(hash, mine));
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * Writes application data.
     * <p>
     * 응용 데이터를 쓴다.
     *
     * @param b   the data / 데이터
     * @param off the start / 시작
     * @param len the length / 길이
     * @throws IOException if the connection failed or was closed, or on a write failure / 연결이 실패했거나 닫혔거나 쓰기에 실패하면
     */
    public void write(byte[] b, int off, int len) throws IOException {
        handshake();
        if (len == 0) {
            return;
        }
        writeLock.lock();
        try {
            if (failure != null) {
                throw failure;
            }
            if (outboundClosed) {
                throw new IOException("the TLS connection is closed");
            }
            records.write(RecordLayer.APPLICATION_DATA, b, off, len);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sends close_notify if that is possible without waiting. The caller closes the streams afterwards.
     * <p>
     * 기다리지 않고 가능하면 close_notify 를 보낸다. 스트림은 호출자가 그 뒤에 닫는다.
     */
    public void close() {
        if (established && failure == null) {
            sendAlert(1, Tls13Exception.CLOSE_NOTIFY);
        }
        outboundClosed = true;
        inboundClosed = true;
    }

    /**
     * Tells if the handshake succeeded.
     * <p>
     * 핸드셰이크가 성공했는지 알려 준다.
     *
     * @return true if established / 성립했으면 true
     */
    public boolean isEstablished() {
        return established;
    }

    /**
     * Returns the negotiated cipher suite.
     * <p>
     * 협상한 암호 스위트를 반환한다.
     *
     * @return the suite, or null before the handshake / 스위트. 핸드셰이크 전이면 null
     */
    public CipherSuite suite() {
        return handshake.suite();
    }

    /**
     * Returns the JSSE name of the negotiated protocol.
     * <p>
     * 협상한 프로토콜의 JSSE 이름을 반환한다.
     *
     * @return <code>TLSv1.3</code> or <code>TLSv1.2</code>
     */
    public String protocol() {
        return handshake.isTls13() ? "TLSv1.3" : "TLSv1.2";
    }

    /**
     * Returns the certificates as the server sent them.
     * <p>
     * 서버가 보낸 그대로의 인증서들을 반환한다.
     *
     * @return the DER certificates, the server certificate first / DER 인증서들. 서버 인증서가 먼저
     */
    public List<byte[]> peerChain() {
        return handshake.peerChain();
    }

    /**
     * Returns the session identifier that the client sent.
     * <p>
     * 클라이언트가 보낸 세션 식별자를 반환한다.
     *
     * @return the identifier / 식별자
     */
    public byte[] sessionId() {
        return handshake.sessionId();
    }

    /**
     * Wipes the keys of the connection.
     * <p>
     * 연결의 키를 지운다.
     */
    public void destroy() {
        records.destroy();
    }
}
