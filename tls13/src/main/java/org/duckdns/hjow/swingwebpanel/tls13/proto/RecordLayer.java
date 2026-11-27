package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Aead;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes;

/**
 * The record layer of TLS 1.3 (RFC 8446 section 5) and of the AEAD suites of TLS 1.2 (RFC 5246 section 6.2, RFC 5288, RFC 7905): it
 * splits outgoing data into records of at most 2^14 bytes, protects and unprotects records with the current keys, and enforces the
 * size limits. The same class serves both ends of a connection. Reading and writing may happen on two threads at the same time; each
 * direction is used by one thread at a time (the callers hold their own locks). Example:
 * <pre>
 * RecordLayer rec = new RecordLayer(in, out);
 * rec.write(RecordLayer.HANDSHAKE, clientHello, 0, clientHello.length);
 * RecordLayer.Record r = rec.read();              // r.type, r.data
 * rec.setReadProtection(suite, true, key, iv);    // from now on incoming records are decrypted
 * </pre>
 * <p>
 * TLS 1.3 의 레코드 계층(RFC 8446 5절)과 TLS 1.2 AEAD 스위트의 레코드 계층(RFC 5246 6.2절, RFC 5288, RFC 7905): 나가는 데이터를 최대 2^14
 * 바이트의 레코드로 나누고, 현재 키로 레코드를 보호·해제하며, 크기 한도를 강제한다. 같은 클래스가 연결의 양쪽 끝에 쓰인다. 읽기와 쓰기는 두 스레드에서
 * 동시에 할 수 있고, 방향마다 한 번에 한 스레드만 쓴다(호출자가 잠금을 가진다).
 */
public final class RecordLayer {

    /** Content type change_cipher_spec. / 내용 형식 change_cipher_spec. */
    public static final int CHANGE_CIPHER_SPEC = 20;

    /** Content type alert. / 내용 형식 alert. */
    public static final int ALERT = 21;

    /** Content type handshake. / 내용 형식 handshake. */
    public static final int HANDSHAKE = 22;

    /** Content type application_data. / 내용 형식 application_data. */
    public static final int APPLICATION_DATA = 23;

    /** The largest plaintext of a record. / 레코드 평문의 최대 크기. */
    public static final int MAX_PLAINTEXT = 16384;

    /**
     * One received record after decryption.
     * <p>
     * 복호화한 뒤의 받은 레코드 하나.
     */
    public static final class Record {

        /** The content type. / 내용 형식. */
        public final int type;

        /** The plaintext. / 평문. */
        public final byte[] data;

        /**
         * Creates a record.
         * <p>
         * 레코드를 만든다.
         *
         * @param type the content type / 내용 형식
         * @param data the plaintext / 평문
         */
        Record(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }

    /**
     * The keys of one direction.
     * <p>
     * 한 방향의 키.
     */
    private static final class Protection {

        /** The AEAD cipher of this direction. / 이 방향의 AEAD 암호. */
        private final Aead aead;

        /** The IV: 12 bytes, or the 4 byte salt of AES-GCM in TLS 1.2. / IV: 12바이트. TLS 1.2 의 AES-GCM 은 4바이트 salt. */
        private final byte[] iv;

        /** True for TLS 1.3 record protection. / TLS 1.3 레코드 보호이면 true. */
        private final boolean tls13;

        /** True if records carry an explicit nonce. / 레코드가 명시적 nonce 를 실으면 true. */
        private final boolean explicitNonce;

        /** The sequence number of the next record. / 다음 레코드의 순서 번호. */
        private long sequence;

        /**
         * Creates the keys of a direction.
         * <p>
         * 한 방향의 키를 만든다.
         *
         * @param suite the cipher suite / 암호 스위트
         * @param tls13 true for TLS 1.3 / TLS 1.3 이면 true
         * @param key   the key / 키
         * @param iv    the IV / IV
         */
        Protection(CipherSuite suite, boolean tls13, byte[] key, byte[] iv) {
            this.aead = suite.newAead(key);
            this.iv = iv.clone();
            this.tls13 = tls13;
            this.explicitNonce = !tls13 && suite.explicitNonce();
        }

        /**
         * Builds the nonce of the current record: the IV XOR the sequence number (TLS 1.3 and ChaCha20), or salt and explicit part.
         * <p>
         * 현재 레코드의 nonce 를 만든다: IV 와 순서 번호의 XOR(TLS 1.3, ChaCha20), 또는 salt 와 명시적 부분.
         *
         * @param explicit the explicit part of eight bytes, or null / 8바이트 명시적 부분 또는 null
         * @return the 12 byte nonce / 12바이트 nonce
         */
        byte[] nonce(byte[] explicit) {
            byte[] n = new byte[12];
            if (explicitNonce) {
                System.arraycopy(iv, 0, n, 0, 4);
                System.arraycopy(explicit, 0, n, 4, 8);
                return n;
            }
            System.arraycopy(iv, 0, n, 0, 12);
            // 순서 번호를 빅 엔디언으로 nonce 의 뒤쪽 8바이트에 XOR 한다.
            for (int i = 0; i < 8; i++) {
                n[11 - i] ^= (byte) (sequence >>> (8 * i));
            }
            return n;
        }
    }

    /** The stream that records are read from. / 레코드를 읽는 스트림. */
    private final InputStream in;

    /** The stream that records are written to. / 레코드를 쓰는 스트림. */
    private final OutputStream out;

    /** The keys for reading, or null before any. / 읽기 키. 아직 없으면 null. */
    private Protection read;

    /** The keys for writing, or null before any. / 쓰기 키. 아직 없으면 null. */
    private Protection write;

    /** The version field of outgoing records: 0x0301 for the first ClientHello, 0x0303 afterwards. / 나가는 레코드의 버전 필드: 첫 ClientHello 는 0x0301, 그 뒤는 0x0303. */
    private int writeVersion = 0x0301;

    /** True while unprotected change_cipher_spec records are dropped (TLS 1.3 middlebox compatibility). / 보호되지 않은 change_cipher_spec 레코드를 버리는 동안 true(TLS 1.3 미들박스 호환). */
    private boolean dropPlainCcs;

    /** The header of the record that is being received. / 받는 중인 레코드의 헤더. */
    private final byte[] head = new byte[5];

    /** The body of the record that is being received, or null while the header is read. / 받는 중인 레코드의 본문. 헤더를 읽는 동안은 null. */
    private byte[] pendingBody;

    /** The number of bytes already received of the header or body in progress. / 진행 중인 헤더나 본문에서 이미 받은 바이트 수. */
    private int partial;

    /** True once a TLS 1.3 handshake is known, so that plaintext records other than the allowed ones are refused. / TLS 1.3 핸드셰이크임이 알려지면 true. */
    private boolean expectProtected;

    /**
     * Creates a record layer over two streams.
     * <p>
     * 두 스트림 위에 레코드 계층을 만든다.
     *
     * @param in  the incoming stream / 들어오는 스트림
     * @param out the outgoing stream / 나가는 스트림
     */
    public RecordLayer(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Installs keys for incoming records; the sequence number starts at zero.
     * <p>
     * 들어오는 레코드의 키를 설치한다. 순서 번호는 0 부터 시작한다.
     *
     * @param suite the cipher suite / 암호 스위트
     * @param tls13 true for TLS 1.3 record protection / TLS 1.3 레코드 보호이면 true
     * @param key   the key / 키
     * @param iv    the IV or salt / IV 또는 salt
     */
    public void setReadProtection(CipherSuite suite, boolean tls13, byte[] key, byte[] iv) {
        Protection old = read;
        read = new Protection(suite, tls13, key, iv);
        expectProtected = true;
        if (old != null) {
            old.aead.destroy();
        }
    }

    /**
     * Installs keys for outgoing records; the sequence number starts at zero.
     * <p>
     * 나가는 레코드의 키를 설치한다. 순서 번호는 0 부터 시작한다.
     *
     * @param suite the cipher suite / 암호 스위트
     * @param tls13 true for TLS 1.3 record protection / TLS 1.3 레코드 보호이면 true
     * @param key   the key / 키
     * @param iv    the IV or salt / IV 또는 salt
     */
    public void setWriteProtection(CipherSuite suite, boolean tls13, byte[] key, byte[] iv) {
        Protection old = write;
        write = new Protection(suite, tls13, key, iv);
        if (old != null) {
            old.aead.destroy();
        }
    }

    /**
     * Sets whether unprotected change_cipher_spec records are silently dropped (during a TLS 1.3 handshake).
     * <p>
     * 보호되지 않은 change_cipher_spec 레코드를 조용히 버릴지 정한다(TLS 1.3 핸드셰이크 동안).
     *
     * @param drop true to drop them / 버리려면 true
     */
    public void setDropPlainChangeCipherSpec(boolean drop) {
        dropPlainCcs = drop;
    }

    /**
     * Makes later outgoing records carry the version 0x0303 (after the first ClientHello).
     * <p>
     * 이후 나가는 레코드가 버전 0x0303 을 싣게 한다(첫 ClientHello 뒤).
     */
    public void useRecordVersion12() {
        writeVersion = 0x0303;
    }

    /**
     * Reads more bytes of the record header or body that is being received. The progress is kept in fields, so a read that is
     * interrupted by a socket timeout can be continued by the next call without losing bytes.
     * <p>
     * 받는 중인 레코드 헤더나 본문의 바이트를 더 읽는다. 진행 상태를 필드에 두므로, 소켓 제한 시간으로 중단된 읽기를 다음 호출이 바이트를 잃지 않고
     * 이어 갈 수 있다.
     *
     * @param buf    the destination / 대상
     * @param n      the number of bytes wanted in total / 모두 합해 원하는 바이트 수
     * @param atEdge true if an end of stream before the first byte is a clean end / 첫 바이트 전의 스트림 끝이 정상 종료이면 true
     * @return false if the stream ended cleanly before the first byte / 첫 바이트 전에 스트림이 깨끗하게 끝났으면 false
     * @throws IOException on a read failure or an end of stream inside the data / 읽기 실패, 또는 데이터 도중에 스트림이 끝나면
     */
    private boolean readFully(byte[] buf, int n, boolean atEdge) throws IOException {
        // 요청한 만큼 찰 때까지 읽는다.
        while (partial < n) {
            int r = in.read(buf, partial, n - partial);
            if (r < 0) {
                if (partial == 0 && atEdge) {
                    return false;
                }
                throw new EOFException("the connection ended inside a TLS record");
            }
            partial += r;
        }
        partial = 0;
        return true;
    }
    /**
     * Reads the next record and removes its protection.
     * <p>
     * 다음 레코드를 읽고 보호를 푼다.
     *
     * @return the record, or null if the peer closed the connection between records / 레코드. 상대가 레코드 사이에서 연결을 닫았으면 null
     * @throws IOException    on a read failure / 읽기 실패 시
     * @throws Tls13Exception if the record is malformed, too large or fails authentication / 레코드가 잘못되었거나 너무 크거나 인증에 실패하면
     */
    public Record read() throws IOException {
        // 버려야 하는 change_cipher_spec 을 건너뛰기 위해 되풀이한다.
        while (true) {
            if (pendingBody == null) {
                if (!readFully(head, 5, true)) {
                    return null;
                }
            }
            int type = this.head[0] & 0xff;
            int length = ((this.head[3] & 0xff) << 8) | (this.head[4] & 0xff);
            if (type < CHANGE_CIPHER_SPEC || type > APPLICATION_DATA || (this.head[1] & 0xff) != 3) {
                throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "the peer did not send a TLS record (type " + type + ")");
            }
            int limit = read == null ? MAX_PLAINTEXT : read.tls13 ? MAX_PLAINTEXT + 256 : MAX_PLAINTEXT + 2048;
            if (length > limit) {
                throw Tls13Exception.protocol(Tls13Exception.RECORD_OVERFLOW, "record of " + length + " bytes");
            }
            if (pendingBody == null) {
                pendingBody = new byte[length];
            }
            readFully(pendingBody, length, false);
            byte[] body = pendingBody;
            pendingBody = null;
            byte[] head = this.head.clone();
            if (type == CHANGE_CIPHER_SPEC && dropPlainCcs) {
                // TLS 1.3 미들박스 호환: 내용이 01 한 바이트인 보호되지 않은 레코드만 버린다.
                if (length != 1 || body[0] != 1) {
                    throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "bad change_cipher_spec");
                }
                continue;
            }
            if (read == null) {
                if (expectProtected) {
                    throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "unprotected record after the keys were installed");
                }
                return new Record(type, body);
            }
            return read.tls13 ? open13(type, head, body) : open12(type, body);
        }
    }

    /**
     * Removes TLS 1.3 protection: the outer type is application_data, the real type is the last non-zero byte of the plaintext.
     * <p>
     * TLS 1.3 보호를 푼다: 바깥 형식은 application_data 이고 실제 형식은 평문의 마지막 0 아닌 바이트이다.
     *
     * @param type the outer type / 바깥 형식
     * @param head the five header bytes (the associated data) / 헤더 5바이트(연관 데이터)
     * @param body the ciphertext with the tag / 태그가 붙은 암호문
     * @return the record / 레코드
     * @throws Tls13Exception if the record is not valid / 레코드가 유효하지 않으면
     */
    private Record open13(int type, byte[] head, byte[] body) throws Tls13Exception {
        if (type != APPLICATION_DATA) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "unprotected record of type " + type + " in TLS 1.3");
        }
        if (body.length < Aead.TAG_LENGTH + 1) {
            throw Tls13Exception.protocol(Tls13Exception.BAD_RECORD_MAC, "record too short");
        }
        byte[] plain = new byte[body.length - Aead.TAG_LENGTH];
        if (!read.aead.open(read.nonce(null), head, body, 0, body.length, plain, 0)) {
            throw Tls13Exception.protocol(Tls13Exception.BAD_RECORD_MAC, "record authentication failed");
        }
        read.sequence++;
        if (plain.length > MAX_PLAINTEXT + 1) {
            // 내용, 형식 바이트, 패딩을 합친 TLSInnerPlaintext 는 2^14 + 1 바이트를 넘을 수 없다(RFC 8446 5.4절).
            throw Tls13Exception.protocol(Tls13Exception.RECORD_OVERFLOW, "inner plaintext of " + plain.length + " bytes");
        }
        int end = plain.length;
        // 뒤쪽의 0 패딩을 걷어 낸다.
        while (end > 0 && plain[end - 1] == 0) {
            end--;
        }
        if (end == 0) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "record without a content type");
        }
        int inner = plain[end - 1] & 0xff;
        if (end - 1 > MAX_PLAINTEXT) {
            throw Tls13Exception.protocol(Tls13Exception.RECORD_OVERFLOW, "plaintext of " + (end - 1) + " bytes");
        }
        if (inner < ALERT || inner > APPLICATION_DATA) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "protected record of type " + inner);
        }
        byte[] data = Bytes.copy(plain, 0, end - 1);
        Bytes.wipe(plain);
        return new Record(inner, data);
    }

    /**
     * Builds the associated data of a TLS 1.2 AEAD record: sequence number, type, version and plaintext length.
     * <p>
     * TLS 1.2 AEAD 레코드의 연관 데이터를 만든다: 순서 번호, 형식, 버전, 평문 길이.
     *
     * @param sequence the sequence number / 순서 번호
     * @param type     the content type / 내용 형식
     * @param length   the plaintext length / 평문 길이
     * @return 13 bytes / 13바이트
     */
    private static byte[] aad12(long sequence, int type, int length) {
        byte[] aad = new byte[13];
        Bytes.putBe64(sequence, aad, 0);
        aad[8] = (byte) type;
        aad[9] = 3;
        aad[10] = 3;
        aad[11] = (byte) (length >> 8);
        aad[12] = (byte) length;
        return aad;
    }

    /**
     * Removes TLS 1.2 AEAD protection.
     * <p>
     * TLS 1.2 AEAD 보호를 푼다.
     *
     * @param type the content type / 내용 형식
     * @param body the record body / 레코드 본문
     * @return the record / 레코드
     * @throws Tls13Exception if the record is not valid / 레코드가 유효하지 않으면
     */
    private Record open12(int type, byte[] body) throws Tls13Exception {
        int skip = read.explicitNonce ? 8 : 0;
        if (body.length < skip + Aead.TAG_LENGTH) {
            throw Tls13Exception.protocol(Tls13Exception.BAD_RECORD_MAC, "record too short");
        }
        int n = body.length - skip - Aead.TAG_LENGTH;
        if (n > MAX_PLAINTEXT) {
            throw Tls13Exception.protocol(Tls13Exception.RECORD_OVERFLOW, "plaintext of " + n + " bytes");
        }
        byte[] explicit = read.explicitNonce ? Bytes.copy(body, 0, 8) : null;
        byte[] plain = new byte[n];
        if (!read.aead.open(read.nonce(explicit), aad12(read.sequence, type, n), body, skip, body.length - skip, plain, 0)) {
            throw Tls13Exception.protocol(Tls13Exception.BAD_RECORD_MAC, "record authentication failed");
        }
        read.sequence++;
        return new Record(type, plain);
    }

    /**
     * Writes data as one or more records of a content type.
     * <p>
     * 데이터를 그 내용 형식의 레코드 하나 이상으로 쓴다.
     *
     * @param type the content type / 내용 형식
     * @param data the data / 데이터
     * @param off  the start / 시작
     * @param len  the length; zero writes one empty record / 길이. 0 이면 빈 레코드 하나를 쓴다
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    public void write(int type, byte[] data, int off, int len) throws IOException {
        int done = 0;
        // 평문을 최대 2^14 바이트씩 나눠 레코드로 보낸다.
        do {
            int n = Math.min(MAX_PLAINTEXT, len - done);
            writeOne(type, data, off + done, n);
            done += n;
        } while (done < len);
        out.flush();
    }

    /**
     * Writes one record.
     * <p>
     * 레코드 하나를 쓴다.
     *
     * @param type the content type / 내용 형식
     * @param data the data / 데이터
     * @param off  the start / 시작
     * @param n    the length, at most 2^14 / 길이. 2^14 이하
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    private void writeOne(int type, byte[] data, int off, int n) throws IOException {
        byte[] record;
        if (write == null) {
            record = new byte[5 + n];
            record[0] = (byte) type;
            record[1] = (byte) (writeVersion >> 8);
            record[2] = (byte) writeVersion;
            System.arraycopy(data, off, record, 5, n);
        } else if (write.tls13) {
            // TLSInnerPlaintext = 내용 | 실제 형식. 패딩은 넣지 않는다.
            int inner = n + 1;
            record = new byte[5 + inner + Aead.TAG_LENGTH];
            record[0] = (byte) APPLICATION_DATA;
            record[1] = 3;
            record[2] = 3;
            record[3] = (byte) ((inner + Aead.TAG_LENGTH) >> 8);
            record[4] = (byte) (inner + Aead.TAG_LENGTH);
            byte[] plain = new byte[inner];
            System.arraycopy(data, off, plain, 0, n);
            plain[n] = (byte) type;
            write.aead.seal(write.nonce(null), Bytes.copy(record, 0, 5), plain, 0, inner, record, 5);
            Bytes.wipe(plain);
            write.sequence++;
            out.write(record);
            return;
        } else {
            int skip = write.explicitNonce ? 8 : 0;
            record = new byte[5 + skip + n + Aead.TAG_LENGTH];
            record[0] = (byte) type;
            record[1] = 3;
            record[2] = 3;
            byte[] explicit = null;
            if (write.explicitNonce) {
                // 명시적 nonce 로 순서 번호를 쓴다(같은 키에서 되풀이되지 않는다).
                explicit = new byte[8];
                Bytes.putBe64(write.sequence, explicit, 0);
                System.arraycopy(explicit, 0, record, 5, 8);
            }
            write.aead.seal(write.nonce(explicit), aad12(write.sequence, type, n), data, off, n, record, 5 + skip);
            write.sequence++;
        }
        int bodyLength = record.length - 5;
        record[3] = (byte) (bodyLength >> 8);
        record[4] = (byte) bodyLength;
        out.write(record);
    }

    /**
     * Wipes the keys of both directions.
     * <p>
     * 양방향의 키를 지운다.
     */
    public void destroy() {
        if (read != null) {
            read.aead.destroy();
        }
        if (write != null) {
            write.aead.destroy();
        }
    }
}
