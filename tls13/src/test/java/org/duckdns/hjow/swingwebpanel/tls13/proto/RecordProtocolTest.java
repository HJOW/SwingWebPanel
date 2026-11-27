package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;

/**
 * The record layer and the messages after the handshake (plan item P32-05-b), against the scripted server: handshake messages that
 * are fragmented into tiny records or joined into one, TLS 1.3 record padding and empty records, records that are too large, damaged
 * or unprotected, KeyUpdate in both directions, NewSessionTicket, the HelloRequest of TLS 1.2, alerts, close_notify and connections
 * that end without it.
 * <p>
 * 시험 서버를 상대로 본 레코드 계층과 핸드셰이크 뒤의 메시지(계획 P32-05-b): 아주 작은 레코드로 조각나거나 하나로 합쳐진 핸드셰이크 메시지, TLS 1.3
 * 레코드 패딩과 빈 레코드, 너무 크거나 손상되었거나 보호되지 않은 레코드, 양방향 KeyUpdate, NewSessionTicket, TLS 1.2 의 HelloRequest, 경보,
 * close_notify, 그리고 그것 없이 끝나는 연결.
 */
public class RecordProtocolTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RecordProtocolTest() {
    }

    /**
     * Converts ASCII text to bytes.
     * <p>
     * ASCII 문자열을 바이트로 바꾼다.
     *
     * @param s the text / 문자열
     * @return the bytes / 바이트
     */
    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts bytes to ASCII text.
     * <p>
     * 바이트를 ASCII 문자열로 바꾼다.
     *
     * @param b the bytes / 바이트
     * @return the text / 문자열
     */
    private static String text(byte[] b) {
        try {
            return new String(b, "US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a server of a version with an RSA certificate and a script.
     * <p>
     * RSA 인증서와 스크립트를 가진, 그 버전의 서버를 만든다.
     *
     * @param tls13  true for TLS 1.3 / TLS 1.3 이면 true
     * @param script the script after the handshake / 핸드셰이크 뒤의 스크립트
     * @return the server / 서버
     */
    private static ScriptedServer server(boolean tls13, ScriptedServer.Script script) {
        ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
        if (!tls13) {
            server.tls13 = false;
            server.suite = 0xc02f;
        }
        server.script = script;
        return server;
    }

    /**
     * Expects the next read of the client to fail with a protocol error and the server to receive the alert.
     * <p>
     * 클라이언트의 다음 읽기가 프로토콜 오류로 실패하고 서버가 그 경보를 받음을 기대한다.
     *
     * @param what   the description / 설명
     * @param server the started server whose script ends with {@link ScriptedServer#awaitAlert()} / 스크립트가 {@link ScriptedServer#awaitAlert()} 로 끝나는 시작된 서버
     * @param alert  the expected alert / 기대하는 경보
     * @throws Exception on failure / 실패 시
     */
    private static void readFails(String what, ScriptedServer server, int alert) throws Exception {
        server.start();
        final TlsFixture.Client client = TlsFixture.connect(server, TlsFixture.parameters());
        try {
            client.connection.handshake();
            Tls13Exception e = (Tls13Exception) Check.throwsException(Tls13Exception.class, new Block() {
                public void run() throws Exception {
                    client.read(1);
                }
            }, what);
            Check.equal(Tls13Exception.Kind.PROTOCOL, e.kind(), what + ": kind (" + e.getMessage() + ")");
            Check.equal(Tls13Exception.alertName(alert), Tls13Exception.alertName(e.alert()), what + ": alert (" + e.getMessage() + ")");
            Throwable again = Check.throwsException(Tls13Exception.class, new Block() {
                public void run() throws Exception {
                    client.read(1);
                }
            }, what + ": reading again");
            Check.same(e, again, what + ": the same failure again");
            Check.throwsException(IOException.class, new Block() {
                public void run() throws Exception {
                    client.write(new byte[1]);
                }
            }, what + ": writing after the failure");
            server.finish();
            Check.equal(Tls13Exception.alertName(alert), Tls13Exception.alertName(server.clientAlert), what + ": alert that the server received");
            Check.equal(2, server.clientAlertLevel, what + ": fatal");
        } finally {
            server.join();
            client.socket.close();
        }
    }

    /**
     * Handshake messages arrive one byte per record, three bytes per record, or all in one record, in both versions; TLS 1.3 records
     * carry padding.
     * <p>
     * 두 버전 모두에서 핸드셰이크 메시지가 레코드당 1바이트, 3바이트, 또는 한 레코드에 모두 담겨 온다. TLS 1.3 레코드에는 패딩이 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFragmentedAndPaddedHandshake() throws Exception {
        int[] fragments = { 1, 3, 97 };
        // 조각 크기와 버전마다 연결 하나를 수행한다.
        for (int i = 0; i < fragments.length; i++) {
            ScriptedServer v13 = server(true, null);
            v13.fragment = fragments[i];
            v13.padding = i * 13;
            HandshakeTest.roundTrip("TLS 1.3, " + fragments[i] + " handshake bytes per record, padding " + v13.padding, v13, TlsFixture.parameters(), "TLSv1.3",
                    "TLS_AES_128_GCM_SHA256");
            ScriptedServer v12 = server(false, null);
            v12.fragment = fragments[i];
            HandshakeTest.roundTrip("TLS 1.2, " + fragments[i] + " handshake bytes per record", v12, TlsFixture.parameters(), "TLSv1.2",
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        }
        ScriptedServer hrr = TlsFixture.server(TlsFixture.p256Key());
        hrr.group = ClientHello.GROUP_SECP384R1;
        hrr.fragment = 2;
        hrr.padding = 255;
        Check.isTrue(HandshakeTest.roundTrip("HelloRetryRequest in fragments", hrr, TlsFixture.parameters(), "TLSv1.3", "TLS_AES_128_GCM_SHA256").retried,
                "HelloRetryRequest");
    }

    /**
     * After the handshake: empty application data records are skipped, NewSessionTicket messages (also two in one record and one split
     * over two records) are ignored, a KeyUpdate without request changes only the keys of the server, and a KeyUpdate with request is
     * answered before the client changes its own keys.
     * <p>
     * 핸드셰이크 뒤: 빈 응용 데이터 레코드를 건너뛰고, NewSessionTicket 메시지(한 레코드에 둘, 두 레코드에 걸친 하나 포함)를 무시하며, 요청 없는
     * KeyUpdate 는 서버의 키만 바꾸고, 요청 있는 KeyUpdate 에는 클라이언트가 자기 키를 바꾸기 전에 답한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls13PostHandshakeMessages() throws Exception {
        final byte[] ticket = new ByteWriter().u24(0).u8(1).u8(2).u8(3).u8(4).vector8(new byte[] { 9 }).vector16(new byte[40]).vector16(new byte[0]).toByteArray();
        ScriptedServer server = server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                Check.equal("ping", text(s.readData(4)), "server: first data of the client");
                s.sendData(new byte[0]);
                s.sendPostHandshake(HandshakeIo.NEW_SESSION_TICKET, ticket);
                byte[] two = HandshakeIo.encode(HandshakeIo.NEW_SESSION_TICKET, ticket);
                s.writeRecord(RecordLayer.HANDSHAKE, org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes.concat(two, Arrays.copyOf(two, 10)));
                s.writeRecord(RecordLayer.HANDSHAKE, Arrays.copyOfRange(two, 10, two.length));
                s.sendData(ascii("one"));

                s.sendPostHandshake(HandshakeIo.KEY_UPDATE, new byte[] { 0 });
                s.updateWriteKeys();
                s.sendData(ascii("two"));

                s.sendPostHandshake(HandshakeIo.KEY_UPDATE, new byte[] { 1 });
                s.updateWriteKeys();
                s.sendData(ascii("three"));
                RecordLayer.Record answer = s.readRecord();
                Check.equal(RecordLayer.HANDSHAKE, answer.type, "server: the client answers the KeyUpdate");
                Check.equal("[24, 0, 0, 1, 0]", Arrays.toString(answer.data), "server: KeyUpdate with update_not_requested, under the old keys");
                s.updateReadKeys();
                Check.equal("more", text(s.readData(4)), "server: data of the client under its new keys");
                s.sendData(ascii("four"));
                s.sendAlert(1, 0);
                s.awaitAlert();
            }
        });
        server.start();
        TlsFixture.Client client = TlsFixture.connect(server, TlsFixture.parameters());
        try {
            client.write(ascii("ping"));
            Check.equal("one", text(client.read(3)), "data after empty record and tickets");
            Check.equal("two", text(client.read(3)), "data after a KeyUpdate without request");
            Check.equal("three", text(client.read(5)), "data after a KeyUpdate with request");
            client.write(ascii("more"));
            Check.equal("four", text(client.read(4)), "data after both sides changed keys");
            Check.equal(0, client.read(1).length, "close_notify of the server");
            server.finish();
            Check.equal(0, server.clientAlert, "the client answers close_notify with close_notify");
            Check.equal(1, server.clientAlertLevel, "as a warning");
        } finally {
            server.join();
            client.socket.close();
        }
    }

    /**
     * TLS 1.3 records and messages that the client must refuse after the handshake.
     * <p>
     * 핸드셰이크 뒤에 클라이언트가 거절해야 하는 TLS 1.3 레코드와 메시지.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls13RefusedRecords() throws Exception {
        readFails("damaged record", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.damageNextRecord();
                s.sendData(ascii("data"));
                s.awaitAlert();
            }
        }), Tls13Exception.BAD_RECORD_MAC);
        readFails("record longer than 2^14 + 256", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRaw(new byte[] { 23, 3, 3, 0x41, 0x01 });
                s.awaitAlert();
            }
        }), Tls13Exception.RECORD_OVERFLOW);
        readFails("plaintext longer than 2^14", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRecord(RecordLayer.APPLICATION_DATA, new byte[RecordLayer.MAX_PLAINTEXT + 1]);
                s.awaitAlert();
            }
        }), Tls13Exception.RECORD_OVERFLOW);
        ScriptedServer padded = server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                // 내용 100바이트와 패딩을 합쳐 2^14 + 1 을 넘는 내부 평문을 직접 만든다(바깥 길이는 한도 안이다).
                byte[] content = new byte[RecordLayer.MAX_PLAINTEXT + 200];
                content[100] = (byte) RecordLayer.APPLICATION_DATA;
                s.writeRecord(0, content);
                s.awaitAlert();
            }
        });
        readFails("padding that makes the inner plaintext longer than 2^14 + 1", padded, Tls13Exception.RECORD_OVERFLOW);
        readFails("unprotected alert after the handshake", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRaw(new byte[] { 21, 3, 3, 0, 2, 2, 40 });
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("change_cipher_spec after the handshake", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRaw(new byte[] { 20, 3, 3, 0, 1, 1 });
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("bytes that are not a TLS record", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRaw(ascii("HTTP/1.1 400 Bad Request\r\n\r\n"));
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("record that is only padding", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRecord(0, new byte[0]);
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("KeyUpdate with an invalid request value", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.sendPostHandshake(HandshakeIo.KEY_UPDATE, new byte[] { 2 });
                s.awaitAlert();
            }
        }), Tls13Exception.ILLEGAL_PARAMETER);
        readFails("KeyUpdate followed by another message in the same record", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                byte[] update = HandshakeIo.encode(HandshakeIo.KEY_UPDATE, new byte[] { 0 });
                s.writeRecord(RecordLayer.HANDSHAKE, org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes.concat(update, new byte[] { 4 }));
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("certificate request after the handshake", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.sendPostHandshake(HandshakeIo.CERTIFICATE_REQUEST, new byte[] { 1, 7, 0, 0 });
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("empty handshake record", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRecord(RecordLayer.HANDSHAKE, new byte[0]);
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("malformed alert", server(true, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRecord(RecordLayer.ALERT, new byte[] { 2 });
                s.awaitAlert();
            }
        }), Tls13Exception.DECODE_ERROR);
    }

    /**
     * TLS 1.2 after the handshake: a HelloRequest is refused with the warning no_renegotiation and the connection goes on; warning
     * alerts are ignored; records with a random explicit nonce are read; a damaged record, an unprotected record and another handshake
     * message are refused.
     * <p>
     * 핸드셰이크 뒤의 TLS 1.2: HelloRequest 를 경고 no_renegotiation 으로 거절하고 연결은 계속된다. 경고 수준의 경보는 무시한다. 임의의 명시적
     * nonce 를 가진 레코드를 읽는다. 손상된 레코드, 보호되지 않은 레코드, 다른 핸드셰이크 메시지는 거절한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls12PostHandshake() throws Exception {
        ScriptedServer server = server(false, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                Check.equal("ping", text(s.readData(4)), "server: first data of the client");
                s.sendPostHandshake(HandshakeIo.HELLO_REQUEST, new byte[0]);
                s.sendAlert(1, 90);
                s.sendData(new byte[0]);
                s.sendData(ascii("one"));
                RecordLayer.Record refusal = s.readRecord();
                Check.equal(RecordLayer.ALERT, refusal.type, "server: the client answers the HelloRequest with an alert");
                Check.equal("[1, 100]", Arrays.toString(refusal.data), "server: warning no_renegotiation");
                Check.equal("more", text(s.readData(4)), "server: the connection goes on");
                s.sendData(ascii("two"));
                s.sendAlert(1, 0);
                s.awaitAlert();
            }
        });
        server.start();
        TlsFixture.Client client = TlsFixture.connect(server, TlsFixture.parameters());
        try {
            client.write(ascii("ping"));
            Check.equal("one", text(client.read(3)), "data after HelloRequest, warning alert and empty record");
            client.write(ascii("more"));
            Check.equal("two", text(client.read(3)), "data after the refusal");
            Check.equal(0, client.read(1).length, "close_notify of the server");
            server.finish();
            Check.equal(0, server.clientAlert, "the client answers close_notify");
        } finally {
            server.join();
            client.socket.close();
        }

        readFails("TLS 1.2 damaged record", server(false, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.damageNextRecord();
                s.sendData(ascii("data"));
                s.awaitAlert();
            }
        }), Tls13Exception.BAD_RECORD_MAC);
        readFails("TLS 1.2 unprotected record after the handshake", server(false, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRaw(new byte[] { 23, 3, 3, 0, 4, 1, 2, 3, 4 });
                s.awaitAlert();
            }
        }), Tls13Exception.BAD_RECORD_MAC);
        readFails("TLS 1.2 record longer than 2^14 + 2048", server(false, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.writeRaw(new byte[] { 23, 3, 3, 0x48, 0x01 });
                s.awaitAlert();
            }
        }), Tls13Exception.RECORD_OVERFLOW);
        readFails("TLS 1.2 handshake message after the handshake", server(false, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.sendPostHandshake(HandshakeIo.NEW_SESSION_TICKET, new byte[6]);
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
        readFails("TLS 1.2 HelloRequest with a body", server(false, new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.sendPostHandshake(HandshakeIo.HELLO_REQUEST, new byte[1]);
                s.awaitAlert();
            }
        }), Tls13Exception.UNEXPECTED_MESSAGE);
    }

    /**
     * How connections end, in both versions: a fatal alert of the server is reported with its code and is not answered; a connection
     * that is closed between records without close_notify reads as the end of the stream; one that is cut inside a record is an
     * error; user_canceled is ignored.
     * <p>
     * 두 버전에서 연결이 끝나는 방식: 서버의 치명적 경보는 그 코드와 함께 보고되고 답하지 않는다. close_notify 없이 레코드 사이에서 닫힌 연결은 스트림
     * 끝으로 읽힌다. 레코드 도중에 끊긴 연결은 오류이다. user_canceled 는 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAlertsAndTruncation() throws Exception {
        // 두 버전에서 같은 사례를 본다.
        for (int version = 0; version < 2; version++) {
            boolean v13 = version == 0;
            String tag = v13 ? "TLS 1.3: " : "TLS 1.2: ";

            ScriptedServer fatal = server(v13, new ScriptedServer.Script() {
                public void run(ScriptedServer s) throws Exception {
                    s.sendAlert(1, 90);
                    s.sendData(ascii("data"));
                    s.sendAlert(2, 80);
                    s.awaitAlert();
                }
            });
            fatal.start();
            final TlsFixture.Client a = TlsFixture.connect(fatal, TlsFixture.parameters());
            Check.equal("data", text(a.read(4)), tag + "data after user_canceled");
            Tls13Exception e = (Tls13Exception) Check.throwsException(Tls13Exception.class, new Block() {
                public void run() throws Exception {
                    a.read(1);
                }
            }, tag + "fatal alert");
            Check.equal(Tls13Exception.Kind.PEER_ALERT, e.kind(), tag + "kind");
            Check.equal(Tls13Exception.INTERNAL_ERROR, e.alert(), tag + "alert code");
            Check.isTrue(e.received(), tag + "received");
            a.socket.close();
            fatal.finish();
            Check.equal(-1, fatal.clientAlert, tag + "a fatal alert is not answered");

            ScriptedServer silent = server(v13, new ScriptedServer.Script() {
                public void run(ScriptedServer s) throws Exception {
                    s.sendData(ascii("data"));
                }
            });
            silent.start();
            TlsFixture.Client b = TlsFixture.connect(silent, TlsFixture.parameters());
            Check.equal("data", text(b.read(4)), tag + "data before the close");
            Check.equal(0, b.read(1).length, tag + "a close between records is the end of the stream");
            Check.equal(0, b.read(1).length, tag + "and stays the end");
            b.socket.close();
            silent.finish();

            ScriptedServer cut = server(v13, new ScriptedServer.Script() {
                public void run(ScriptedServer s) throws Exception {
                    s.writeRaw(new byte[] { 23, 3, 3, 0, 40, 1, 2, 3 });
                }
            });
            cut.start();
            final TlsFixture.Client c = TlsFixture.connect(cut, TlsFixture.parameters());
            c.connection.handshake();
            Check.throwsException(EOFException.class, new Block() {
                public void run() throws Exception {
                    c.read(1);
                }
            }, tag + "a close inside a record");
            c.socket.close();
            cut.finish();
        }
    }
}
