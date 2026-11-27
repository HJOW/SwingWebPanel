package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;

/**
 * Reads and writes handshake messages on top of the record layer and keeps the transcript, the concatenation of all handshake messages
 * that the Finished values and the key schedule hash (RFC 8446 section 4.4.1, RFC 5246 section 7.4.9). A handshake message may be split
 * over several records or share a record with others; this class reassembles them. Alerts that arrive instead of a handshake message
 * become exceptions. Example:
 * <pre>
 * io.send(HandshakeIo.CLIENT_HELLO, body);            // adds header, writes, appends to the transcript
 * HandshakeIo.Message m = io.receive();               // m.type, m.body; already in the transcript
 * byte[] hash = io.transcriptHash(HashAlg.SHA256);
 * </pre>
 * <p>
 * 레코드 계층 위에서 핸드셰이크 메시지를 읽고 쓰며 전사(모든 핸드셰이크 메시지를 이어 붙인 것. Finished 값과 키 스케줄이 그 해시를 쓴다. RFC 8446
 * 4.4.1절, RFC 5246 7.4.9절)를 보관한다. 핸드셰이크 메시지는 여러 레코드에 나뉘거나 한 레코드에 여럿이 들어올 수 있으며 이 클래스가 다시 조립한다.
 * 핸드셰이크 메시지 대신 온 경보는 예외가 된다.
 */
public final class HandshakeIo {

    /** Handshake type hello_request (TLS 1.2). / 핸드셰이크 형식 hello_request(TLS 1.2). */
    public static final int HELLO_REQUEST = 0;

    /** Handshake type client_hello. / 핸드셰이크 형식 client_hello. */
    public static final int CLIENT_HELLO = 1;

    /** Handshake type server_hello. / 핸드셰이크 형식 server_hello. */
    public static final int SERVER_HELLO = 2;

    /** Handshake type new_session_ticket. / 핸드셰이크 형식 new_session_ticket. */
    public static final int NEW_SESSION_TICKET = 4;

    /** Handshake type encrypted_extensions (TLS 1.3). / 핸드셰이크 형식 encrypted_extensions(TLS 1.3). */
    public static final int ENCRYPTED_EXTENSIONS = 8;

    /** Handshake type certificate. / 핸드셰이크 형식 certificate. */
    public static final int CERTIFICATE = 11;

    /** Handshake type server_key_exchange (TLS 1.2). / 핸드셰이크 형식 server_key_exchange(TLS 1.2). */
    public static final int SERVER_KEY_EXCHANGE = 12;

    /** Handshake type certificate_request. / 핸드셰이크 형식 certificate_request. */
    public static final int CERTIFICATE_REQUEST = 13;

    /** Handshake type server_hello_done (TLS 1.2). / 핸드셰이크 형식 server_hello_done(TLS 1.2). */
    public static final int SERVER_HELLO_DONE = 14;

    /** Handshake type certificate_verify. / 핸드셰이크 형식 certificate_verify. */
    public static final int CERTIFICATE_VERIFY = 15;

    /** Handshake type client_key_exchange (TLS 1.2). / 핸드셰이크 형식 client_key_exchange(TLS 1.2). */
    public static final int CLIENT_KEY_EXCHANGE = 16;

    /** Handshake type finished. / 핸드셰이크 형식 finished. */
    public static final int FINISHED = 20;

    /** Handshake type key_update (TLS 1.3). / 핸드셰이크 형식 key_update(TLS 1.3). */
    public static final int KEY_UPDATE = 24;

    /** The largest handshake message accepted. / 받아들이는 핸드셰이크 메시지의 최대 크기. */
    public static final int MAX_MESSAGE = 256 * 1024;

    /**
     * One handshake message.
     * <p>
     * 핸드셰이크 메시지 하나.
     */
    public static final class Message {

        /** The handshake type. / 핸드셰이크 형식. */
        public final int type;

        /** The body without the four byte header. / 4바이트 헤더를 뺀 본문. */
        public final byte[] body;

        /**
         * Creates a message.
         * <p>
         * 메시지를 만든다.
         *
         * @param type the handshake type / 핸드셰이크 형식
         * @param body the body / 본문
         */
        Message(int type, byte[] body) {
            this.type = type;
            this.body = body;
        }
    }

    /** The record layer. / 레코드 계층. */
    private final RecordLayer records;

    /** Handshake bytes received but not yet returned as a message. / 받았지만 아직 메시지로 돌려주지 않은 핸드셰이크 바이트. */
    private byte[] pending = new byte[0];

    /** The transcript. / 전사. */
    private ByteArrayOutputStream transcript = new ByteArrayOutputStream();

    /** True while a negotiation failure is still possible: no ServerHello was accepted yet. / 협상 실패가 아직 가능한 동안 true: ServerHello 를 아직 받아들이지 않았다. */
    private boolean negotiating = true;

    /**
     * Creates the handshake reader and writer.
     * <p>
     * 핸드셰이크 읽기·쓰기 도구를 만든다.
     *
     * @param records the record layer / 레코드 계층
     */
    public HandshakeIo(RecordLayer records) {
        this.records = records;
    }

    /**
     * Marks the negotiation as finished: later alerts of the peer are no longer "negotiation failures".
     * <p>
     * 협상이 끝났음을 표시한다: 이후 상대의 경보는 더 이상 "협상 실패"가 아니다.
     */
    public void negotiated() {
        negotiating = false;
    }

    /**
     * Turns a received alert record into an exception.
     * <p>
     * 받은 경보 레코드를 예외로 바꾼다.
     *
     * @param data        the alert record / 경보 레코드
     * @param negotiating true if no ServerHello was accepted yet / 아직 ServerHello 를 받아들이지 않았으면 true
     * @return the exception / 예외
     */
    public static Tls13Exception alert(byte[] data, boolean negotiating) {
        if (data.length != 2) {
            return Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "malformed alert");
        }
        int description = data[1] & 0xff;
        boolean refusal = description == Tls13Exception.HANDSHAKE_FAILURE || description == Tls13Exception.PROTOCOL_VERSION
                || description == Tls13Exception.INSUFFICIENT_SECURITY;
        Tls13Exception.Kind kind = negotiating && refusal ? Tls13Exception.Kind.NEGOTIATION : Tls13Exception.Kind.PEER_ALERT;
        return new Tls13Exception(kind, description, true, "the server sent the alert " + Tls13Exception.alertName(description));
    }

    /**
     * Returns the next handshake message and appends it to the transcript.
     * <p>
     * 다음 핸드셰이크 메시지를 반환하고 전사에 덧붙인다.
     *
     * @return the message / 메시지
     * @throws IOException    on a read failure or if the connection ends / 읽기 실패 또는 연결이 끝나면
     * @throws Tls13Exception on an alert, a record of another type or a malformed message / 경보, 다른 형식의 레코드, 잘못된 메시지
     */
    public Message receive() throws IOException {
        Message m = receiveRaw();
        transcript.write(header(m.type, m.body.length), 0, 4);
        transcript.write(m.body, 0, m.body.length);
        return m;
    }

    /**
     * Returns the next handshake message without touching the transcript (messages after the handshake).
     * <p>
     * 전사를 건드리지 않고 다음 핸드셰이크 메시지를 반환한다(핸드셰이크 뒤의 메시지).
     *
     * @return the message / 메시지
     * @throws IOException    on a read failure or if the connection ends / 읽기 실패 또는 연결이 끝나면
     * @throws Tls13Exception on an alert, a record of another type or a malformed message / 경보, 다른 형식의 레코드, 잘못된 메시지
     */
    public Message receiveRaw() throws IOException {
        // 메시지 하나가 다 모일 때까지 핸드셰이크 레코드를 읽는다.
        while (true) {
            Message m = take();
            if (m != null) {
                return m;
            }
            RecordLayer.Record r = records.read();
            if (r == null) {
                throw new EOFException("the server closed the connection during the handshake");
            }
            if (r.type == RecordLayer.ALERT) {
                throw alert(r.data, negotiating);
            }
            if (r.type != RecordLayer.HANDSHAKE) {
                throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "record of type " + r.type + " during the handshake");
            }
            feed(r.data);
        }
    }

    /**
     * Adds the content of a handshake record to the reassembly buffer.
     * <p>
     * 핸드셰이크 레코드의 내용을 재조립 버퍼에 더한다.
     *
     * @param data the record content / 레코드 내용
     * @throws Tls13Exception if the record is empty / 레코드가 비었으면
     */
    public void feed(byte[] data) throws Tls13Exception {
        if (data.length == 0) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "empty handshake record");
        }
        byte[] joined = new byte[pending.length + data.length];
        System.arraycopy(pending, 0, joined, 0, pending.length);
        System.arraycopy(data, 0, joined, pending.length, data.length);
        pending = joined;
    }

    /**
     * Takes a complete message out of the reassembly buffer.
     * <p>
     * 재조립 버퍼에서 완성된 메시지를 꺼낸다.
     *
     * @return the message, or null if more bytes are needed / 메시지. 바이트가 더 필요하면 null
     * @throws Tls13Exception if the message is too large / 메시지가 너무 크면
     */
    public Message take() throws Tls13Exception {
        if (pending.length < 4) {
            return null;
        }
        int length = ((pending[1] & 0xff) << 16) | ((pending[2] & 0xff) << 8) | (pending[3] & 0xff);
        if (length > MAX_MESSAGE) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "handshake message of " + length + " bytes");
        }
        if (pending.length < 4 + length) {
            return null;
        }
        byte[] body = new byte[length];
        System.arraycopy(pending, 4, body, 0, length);
        Message m = new Message(pending[0] & 0xff, body);
        byte[] rest = new byte[pending.length - 4 - length];
        System.arraycopy(pending, 4 + length, rest, 0, rest.length);
        pending = rest;
        return m;
    }

    /**
     * Checks that no partial or extra handshake bytes are buffered. Keys may change only at a message boundary (RFC 8446 section 5.1).
     * <p>
     * 덜 온 핸드셰이크 바이트나 여분의 바이트가 버퍼에 없는지 확인한다. 키는 메시지 경계에서만 바뀔 수 있다(RFC 8446 5.1절).
     *
     * @throws Tls13Exception unexpected_message if bytes are buffered / 바이트가 남아 있으면 unexpected_message
     */
    public void expectBoundary() throws Tls13Exception {
        if (pending.length != 0) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "handshake data across a key change");
        }
    }

    /**
     * Builds the four byte handshake header.
     * <p>
     * 4바이트 핸드셰이크 헤더를 만든다.
     *
     * @param type   the handshake type / 핸드셰이크 형식
     * @param length the body length / 본문 길이
     * @return the header / 헤더
     */
    private static byte[] header(int type, int length) {
        return new byte[] { (byte) type, (byte) (length >> 16), (byte) (length >> 8), (byte) length };
    }

    /**
     * Sends a handshake message and appends it to the transcript.
     * <p>
     * 핸드셰이크 메시지를 보내고 전사에 덧붙인다.
     *
     * @param type the handshake type / 핸드셰이크 형식
     * @param body the body / 본문
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    public void send(int type, byte[] body) throws IOException {
        sendEncoded(encode(type, body), true);
    }

    /**
     * Encodes a handshake message with its header.
     * <p>
     * 핸드셰이크 메시지를 헤더와 함께 부호화한다.
     *
     * @param type the handshake type / 핸드셰이크 형식
     * @param body the body / 본문
     * @return the encoded message / 부호화한 메시지
     */
    public static byte[] encode(int type, byte[] body) {
        byte[] out = new byte[4 + body.length];
        System.arraycopy(header(type, body.length), 0, out, 0, 4);
        System.arraycopy(body, 0, out, 4, body.length);
        return out;
    }

    /**
     * Sends an already encoded handshake message.
     * <p>
     * 이미 부호화한 핸드셰이크 메시지를 보낸다.
     *
     * @param message the message with its header / 헤더가 붙은 메시지
     * @param record  true to append it to the transcript / 전사에 덧붙이려면 true
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    public void sendEncoded(byte[] message, boolean record) throws IOException {
        records.write(RecordLayer.HANDSHAKE, message, 0, message.length);
        if (record) {
            transcript.write(message, 0, message.length);
        }
    }

    /**
     * Returns the hash of the transcript so far.
     * <p>
     * 지금까지의 전사의 해시를 반환한다.
     *
     * @param hash the hash function of the cipher suite / 암호 스위트의 해시 함수
     * @return the hash / 해시
     */
    public byte[] transcriptHash(HashAlg hash) {
        return hash.digest(transcript.toByteArray());
    }

    /**
     * Replaces the transcript (one ClientHello) by the synthetic <code>message_hash</code> message, as RFC 8446 section 4.4.1 requires
     * after a HelloRetryRequest.
     * <p>
     * 전사(ClientHello 하나)를 합성 <code>message_hash</code> 메시지로 바꾼다. RFC 8446 4.4.1절이 HelloRetryRequest 뒤에 요구하는 것이다.
     *
     * @param hash the hash function of the cipher suite / 암호 스위트의 해시 함수
     */
    public void replaceByMessageHash(HashAlg hash) {
        byte[] digest = transcriptHash(hash);
        transcript = new ByteArrayOutputStream();
        transcript.write(header(254, digest.length), 0, 4);
        transcript.write(digest, 0, digest.length);
    }

    /**
     * Removes the last message from the transcript and returns the transcript before it (used to insert the HelloRetryRequest handling
     * after the message was already received).
     * <p>
     * 전사에서 마지막 메시지를 떼고 그 앞까지의 전사를 반환한다(메시지를 이미 받은 뒤에 HelloRetryRequest 처리를 끼워 넣을 때 쓴다).
     *
     * @param last the message to remove / 뗄 메시지
     */
    public void removeLast(Message last) {
        byte[] all = transcript.toByteArray();
        transcript = new ByteArrayOutputStream();
        transcript.write(all, 0, all.length - 4 - last.body.length);
    }

    /**
     * Appends an encoded message to the transcript without sending it.
     * <p>
     * 부호화한 메시지를 보내지 않고 전사에 덧붙인다.
     *
     * @param message the message with its header / 헤더가 붙은 메시지
     */
    public void appendToTranscript(byte[] message) {
        transcript.write(message, 0, message.length);
    }
}
