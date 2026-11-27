package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Config;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Failures;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13SocketFactory;

/**
 * The public face of the module (plan item P32-05-e): {@link Tls13SocketFactory}, its <code>SSLSocket</code> and
 * <code>SSLSession</code>, {@link Tls13Config} and {@link Tls13Failures}, used the way the JRE and the browser use them: layered over
 * a connected socket, through the streams, with timeouts, closed from another thread, read and written concurrently, and installed in
 * an <code>HttpsURLConnection</code> (which must send the server name although the factory refuses unconnected sockets).
 * <p>
 * 모듈의 공개 면(계획 P32-05-e): {@link Tls13SocketFactory}, 그 <code>SSLSocket</code> 과 <code>SSLSession</code>, {@link Tls13Config},
 * {@link Tls13Failures}. JRE 와 브라우저가 쓰는 방식대로 쓴다: 연결된 소켓 위에 얹기, 스트림 사용, 제한 시간, 다른 스레드에서 닫기, 동시 읽기·쓰기,
 * <code>HttpsURLConnection</code> 에 설치하기(팩터리가 연결되지 않은 소켓을 거절해도 서버 이름을 보내야 한다).
 */
public class SocketFactoryTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SocketFactoryTest() {
    }

    /**
     * Returns a factory that trusts the test root.
     * <p>
     * 시험용 루트를 신뢰하는 팩터리를 반환한다.
     *
     * @return the factory / 팩터리
     */
    private static Tls13SocketFactory factory() {
        return new Tls13SocketFactory(Tls13Config.defaults().withTrustStore(TlsFixture.trust()));
    }

    /**
     * Connects a TLS socket of the factory to a started server, for the test host name.
     * <p>
     * 팩터리의 TLS 소켓을 시작한 서버에 시험용 호스트 이름으로 연결한다.
     *
     * @param factory the factory / 팩터리
     * @param server  the server / 서버
     * @return the socket / 소켓
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    private static SSLSocket open(Tls13SocketFactory factory, ScriptedServer server) throws IOException {
        Socket plain = new Socket("127.0.0.1", server.port());
        plain.setSoTimeout(20000);
        return (SSLSocket) factory.createSocket(plain, TlsFixture.HOST, server.port(), true);
    }

    /**
     * Reads until a number of bytes arrived or the stream ended.
     * <p>
     * 정한 바이트 수가 오거나 스트림이 끝날 때까지 읽는다.
     *
     * @param in the stream / 스트림
     * @param n  the number of bytes / 바이트 수
     * @return the bytes read / 읽은 바이트
     * @throws IOException on a failure / 실패 시
     */
    private static byte[] readFully(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int have = 0;
        // 원하는 만큼 찰 때까지 읽는다.
        while (have < n) {
            int r = in.read(buf, have, n - have);
            if (r < 0) {
                break;
            }
            have += r;
        }
        return Arrays.copyOf(buf, have);
    }

    /**
     * The factory refuses unconnected sockets in the way that makes the JRE use the layered form, and refuses a missing host or an
     * unconnected socket.
     * <p>
     * 팩터리는 JRE 가 얹는 형식을 쓰게 만드는 방식으로 연결되지 않은 소켓을 거절하고, 호스트가 없거나 소켓이 연결되지 않았으면 거절한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefusals() throws Exception {
        final Tls13SocketFactory factory = factory();
        Throwable t = Check.throwsException(SocketException.class, new Block() {
            public void run() throws Exception {
                factory.createSocket();
            }
        }, "unconnected socket");
        Check.isTrue(t.getCause() instanceof UnsupportedOperationException, "the cause tells the JRE to connect the socket itself");
        Check.throwsException(SocketException.class, new Block() {
            public void run() throws Exception {
                factory.createSocket(new Socket(), TlsFixture.HOST, 443, true);
            }
        }, "a socket that is not connected");
        ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey()).start();
        final Socket plain = new Socket("127.0.0.1", server.port());
        Check.throwsException(SocketException.class, new Block() {
            public void run() throws Exception {
                factory.createSocket(plain, null, 443, true);
            }
        }, "no host name");
        plain.close();
        server.join();
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                new Tls13SocketFactory(null);
            }
        }, "no configuration");
        Check.same(Tls13SocketFactory.getInstance(), Tls13SocketFactory.getInstance(), "one shared default factory");
        Check.equal(9, Tls13SocketFactory.getInstance().getDefaultCipherSuites().length, "default cipher suites");
        Check.equal(9, factory.getSupportedCipherSuites().length, "supported cipher suites");
    }

    /**
     * A socket layered over a connected socket: handshake, listener, session, streams, and an orderly close that also closes the
     * socket below when asked to.
     * <p>
     * 연결된 소켓 위에 얹은 소켓: 핸드셰이크, 리스너, 세션, 스트림, 그리고 요청하면 아래 소켓도 닫는 정상 종료.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLayeredSocketAndSession() throws Exception {
        // 아래 소켓을 함께 닫는 경우와 남기는 경우를 본다.
        for (int round = 0; round < 2; round++) {
            boolean autoClose = round == 0;
            ScriptedServer server = TlsFixture.server(round == 0 ? TlsFixture.rsaKey() : TlsFixture.p256Key()).start();
            Socket plain = new Socket("127.0.0.1", server.port());
            SSLSocket tls = (SSLSocket) factory().createSocket(plain, TlsFixture.HOST, server.port(), autoClose);
            final int[] told = new int[1];
            final SSLSession[] seen = new SSLSession[1];
            tls.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    told[0]++;
                    seen[0] = event.getSession();
                }
            });
            Check.isTrue(tls.getUseClientMode(), "client mode");
            Check.equal("[TLSv1.3, TLSv1.2]", Arrays.toString(tls.getEnabledProtocols()), "enabled protocols");
            Check.equal(9, tls.getEnabledCipherSuites().length, "enabled cipher suites");
            tls.startHandshake();
            tls.startHandshake();
            Check.equal(1, told[0], "the listener is told once");
            SSLSession session = tls.getSession();
            Check.same(session, seen[0], "the listener sees the session of the socket");
            Check.same(session, tls.getSession(), "one session per socket");
            Check.equal("TLSv1.3", session.getProtocol(), "protocol");
            Check.equal("TLS_AES_128_GCM_SHA256", session.getCipherSuite(), "cipher suite");
            Check.isTrue(session.isValid(), "valid");
            Check.equal(TlsFixture.HOST, session.getPeerHost(), "peer host");
            Check.equal(server.port(), session.getPeerPort(), "peer port");
            Check.equal(32, session.getId().length, "session identifier");
            Check.isNull(session.getSessionContext(), "no session cache");
            Check.isNull(session.getLocalCertificates(), "no client certificate");
            X509Certificate leaf = (X509Certificate) session.getPeerCertificates()[0];
            Check.contains(leaf.getSubjectX500Principal().getName(), "CN=" + TlsFixture.HOST, "subject of the server certificate");
            Check.equal(leaf.getSubjectX500Principal(), session.getPeerPrincipal(), "peer principal");
            try {
                Check.equal(1, session.getPeerCertificateChain().length, "old style certificate chain");
            } catch (SSLPeerUnverifiedException e) {
                // 새 JRE(15 이상)에는 javax.security.cert 의 구현이 없다. 그 밖의 JRE 에서는 실패이다.
                Check.isFalse(System.getProperty("java.specification.version").startsWith("1."), "old style certificates on an old JRE: " + e);
            }
            session.putValue("k", "v");
            Check.equal("v", session.getValue("k"), "session values");
            Check.equal(server.port(), tls.getPort(), "port of the socket below");
            Check.isTrue(tls.isConnected() && !tls.isClosed(), "connected");

            OutputStream out = tls.getOutputStream();
            InputStream in = tls.getInputStream();
            out.write('a');
            out.write(new byte[] { 'b', 'c', 'd' }, 1, 2);
            out.flush();
            Check.equal((int) 'a', in.read(), "single byte read");
            Check.equal("[99, 100]", Arrays.toString(readFully(in, 2)), "echo");
            out.write(new byte[] { 1, 2, 3, 4, 5 });
            Check.equal(1, in.read(), "first byte of a record");
            Check.equal(4, in.available(), "the rest of the record is available");
            Check.equal(4, readFully(in, 4).length, "the rest");
            Check.equal(0, in.read(new byte[0], 0, 0), "a zero length read returns 0");

            tls.close();
            tls.close();
            Check.isTrue(tls.isClosed(), "closed");
            Check.equal(autoClose, plain.isClosed(), "the socket below is closed only with autoClose");
            final SSLSocket closed = tls;
            Check.throwsException(SocketException.class, new Block() {
                public void run() throws Exception {
                    closed.getInputStream();
                }
            }, "streams of a closed socket");
            server.finish();
            Check.equal(0, server.clientAlert, "close_notify was sent");
            Check.equal(TlsFixture.HOST, server.sni, "server name");
            plain.close();
        }
    }

    /**
     * The handshake starts with the first write; the protocol and cipher suite settings of the socket reach the ClientHello; names
     * that the module does not have are refused.
     * <p>
     * 핸드셰이크는 첫 쓰기에서 시작한다. 소켓의 프로토콜·암호 스위트 설정이 ClientHello 에 반영된다. 모듈에 없는 이름은 거절한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLazyHandshakeAndSocketSettings() throws Exception {
        ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
        server.tls13 = false;
        server.suite = 0xcca8;
        server.start();
        final SSLSocket tls = open(factory(), server);
        tls.setEnabledProtocols(new String[] { "TLSv1.2" });
        tls.setEnabledCipherSuites(new String[] { "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_AES_128_GCM_SHA256" });
        Check.equal("[TLSv1.2]", Arrays.toString(tls.getEnabledProtocols()), "enabled protocols");
        Check.equal("[TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256]", Arrays.toString(tls.getEnabledCipherSuites()),
                "enabled cipher suites of the enabled protocol");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                tls.setEnabledProtocols(new String[] { "TLSv1" });
            }
        }, "TLS 1.0 cannot be enabled");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                tls.setEnabledProtocols(new String[0]);
            }
        }, "no protocol");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                tls.setEnabledCipherSuites(new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA" });
            }
        }, "a suite that the module does not have");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                tls.setUseClientMode(false);
            }
        }, "server mode");
        tls.getOutputStream().write(new byte[] { 7, 8, 9 });
        Check.equal("[7, 8, 9]", Arrays.toString(readFully(tls.getInputStream(), 3)), "echo after the implicit handshake");
        Check.equal("TLSv1.2", tls.getSession().getProtocol(), "protocol");
        Check.equal("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", tls.getSession().getCipherSuite(), "cipher suite");
        tls.setEnabledProtocols(new String[] { "TLSv1.3" });
        Check.equal("[TLSv1.2]", Arrays.toString(tls.getEnabledProtocols()), "settings are fixed once the handshake started");
        tls.close();
        server.finish();
        Check.equal("[]", server.offeredVersions.toString(), "TLS 1.2 only ClientHello");
        Check.equal("[52392, 49199]", server.offeredSuites.toString(), "the enabled suites in the given order");
    }

    /**
     * A read timeout does not lose data: a record that arrives in two parts with a pause is read after the timeout, and a thread that
     * waits in a read is released when another thread closes the socket.
     * <p>
     * 읽기 제한 시간은 데이터를 잃지 않는다: 멈춤을 두고 두 부분으로 오는 레코드를 제한 시간 뒤에 읽는다. 읽기에서 기다리는 스레드는 다른 스레드가
     * 소켓을 닫으면 풀려난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTimeoutAndCloseFromAnotherThread() throws Exception {
        ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
        server.script = new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                s.readData(1);
                s.splitDelay = 800;
                s.sendData(new byte[] { 10, 20, 30, 40, 50 });
                s.splitDelay = 0;
                s.awaitAlert();
            }
        };
        server.start();
        final SSLSocket tls = open(factory(), server);
        tls.startHandshake();
        tls.getOutputStream().write(1);
        tls.setSoTimeout(250);
        Check.equal(250, tls.getSoTimeout(), "the timeout is the one of the socket below");
        final InputStream in = tls.getInputStream();
        int timeouts = 0;
        byte[] got = null;
        // 제한 시간이 지나면 다시 읽는다. 레코드의 앞부분은 이미 받은 상태로 남아 있어야 한다.
        for (int i = 0; i < 20 && got == null; i++) {
            try {
                got = readFully(in, 5);
            } catch (SocketTimeoutException e) {
                timeouts++;
            }
        }
        Check.isTrue(timeouts >= 1, "at least one timeout while the record was incomplete");
        Check.equal("[10, 20, 30, 40, 50]", Arrays.toString(got), "the record is complete after the timeouts");

        tls.setSoTimeout(0);
        final Throwable[] result = new Throwable[1];
        final int[] value = { 12345 };
        Thread reader = new Thread(new Runnable() {
            public void run() {
                try {
                    value[0] = in.read();
                } catch (Throwable t) {
                    result[0] = t;
                }
            }
        }, "blocked-reader");
        reader.start();
        Thread.sleep(300);
        Check.isTrue(reader.isAlive(), "the reader waits");
        tls.close();
        reader.join(5000);
        Check.isFalse(reader.isAlive(), "the reader is released by the close");
        Check.isTrue(result[0] instanceof IOException || value[0] == -1, "the read ends with an exception or the end of the stream: " + result[0]);
        server.join();
    }

    /**
     * One thread writes while another reads, 300 KiB each way.
     * <p>
     * 한 스레드가 쓰는 동안 다른 스레드가 읽는다. 각 방향 300 KiB.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConcurrentReadAndWrite() throws Exception {
        // 두 버전에서 본다.
        for (int version = 0; version < 2; version++) {
            ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
            if (version == 1) {
                server.tls13 = false;
                server.suite = 0xc030;
            }
            server.start();
            final SSLSocket tls = open(factory(), server);
            tls.startHandshake();
            final byte[] data = new byte[300 * 1024];
            // 위치마다 다른 값을 채운다.
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i * 31 + (i >>> 8));
            }
            final Throwable[] failure = new Throwable[1];
            Thread writer = new Thread(new Runnable() {
                public void run() {
                    try {
                        OutputStream out = tls.getOutputStream();
                        // 크기가 다른 조각으로 나눠 쓴다.
                        for (int at = 0; at < data.length;) {
                            int n = Math.min(data.length - at, 1 + (at % 9973));
                            out.write(data, at, n);
                            at += n;
                        }
                    } catch (Throwable t) {
                        failure[0] = t;
                    }
                }
            }, "writer");
            writer.start();
            byte[] echoed = readFully(tls.getInputStream(), data.length);
            writer.join(20000);
            Check.isNull(failure[0], "the writer did not fail");
            Check.isTrue(Arrays.equals(data, echoed), "all bytes came back in order (version " + version + ")");
            tls.close();
            server.finish();
        }
    }

    /**
     * A failed handshake: the exception of the module with its kind, an invalid session without certificates, the same failure on
     * later use, and the classification by {@link Tls13Failures}.
     * <p>
     * 실패한 핸드셰이크: 종류를 가진 모듈의 예외, 인증서가 없는 유효하지 않은 세션, 이후 사용에서 같은 실패, {@link Tls13Failures} 의 분류.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailedHandshake() throws Exception {
        ScriptedServer old = TlsFixture.server(TlsFixture.rsaKey());
        old.faults.add(ScriptedServer.Fault.TLS11_HELLO);
        old.start();
        final SSLSocket tls = open(factory(), old);
        Throwable t = Check.throwsException(Tls13Exception.class, new Block() {
            public void run() throws Exception {
                tls.startHandshake();
            }
        }, "TLS 1.1 server");
        Check.isTrue(Tls13Failures.isNegotiationFailure(t), "negotiation failure");
        final SSLSession none = tls.getSession();
        Check.isFalse(none.isValid(), "invalid session");
        Check.equal("SSL_NULL_WITH_NULL_NULL", none.getCipherSuite(), "null cipher suite");
        Check.throwsException(SSLPeerUnverifiedException.class, new Block() {
            public void run() throws Exception {
                none.getPeerCertificates();
            }
        }, "no peer certificates");
        Throwable again = Check.throwsException(Tls13Exception.class, new Block() {
            public void run() throws Exception {
                tls.getOutputStream().write(1);
            }
        }, "writing after the failed handshake");
        Check.same(t, again, "the same failure");
        tls.close();
        old.join();

        ScriptedServer untrusted = TlsFixture.server(TlsFixture.rsaKey()).start();
        final SSLSocket second = open(Tls13SocketFactory.getInstance(), untrusted);
        Throwable rejected = Check.throwsException(Tls13Exception.class, new Block() {
            public void run() throws Exception {
                second.getInputStream().read();
            }
        }, "the default factory does not trust the test root");
        Check.equal(Tls13Exception.Kind.CERTIFICATE, ((Tls13Exception) rejected).kind(), "certificate failure");
        Check.isFalse(Tls13Failures.isNegotiationFailure(rejected), "not retried");
        Check.isTrue(Tls13Failures.isModuleFailure(rejected), "a failure of the module");
        second.close();
        untrusted.join();
    }

    /**
     * The connecting variants of the factory: by host name (server name sent) and by address (no server name; the certificate must
     * name the address).
     * <p>
     * 팩터리의 연결 메서드: 호스트 이름으로(서버 이름을 보낸다), 주소로(서버 이름 없음. 인증서에 그 주소가 있어야 한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConnectingVariants() throws Exception {
        ScriptedServer byName = TlsFixture.server(TlsFixture.rsaKey()).start();
        SSLSocket a = (SSLSocket) factory().createSocket("localhost", byName.port());
        a.startHandshake();
        a.close();
        byName.finish();
        Check.equal("localhost", byName.sni, "server name of the host name variant");

        ScriptedServer byAddress = TlsFixture.server(TlsFixture.rsaKey()).start();
        SSLSocket b = (SSLSocket) factory().createSocket(InetAddress.getByName("127.0.0.1"), byAddress.port());
        b.startHandshake();
        Check.equal("127.0.0.1", b.getSession().getPeerHost(), "the address is the peer host");
        b.close();
        byAddress.finish();
        Check.isNull(byAddress.sni, "no server name for an address");

        ScriptedServer local = TlsFixture.server(TlsFixture.rsaKey()).start();
        SSLSocket c = (SSLSocket) factory().createSocket("localhost", local.port(), InetAddress.getByName("127.0.0.1"), 0);
        c.startHandshake();
        Check.equal("127.0.0.1", c.getLocalAddress().getHostAddress(), "bound to the local address");
        c.close();
        local.finish();

        Check.throwsException(IOException.class, new Block() {
            public void run() throws Exception {
                ScriptedServer gone = TlsFixture.server(TlsFixture.rsaKey()).start();
                int port = gone.port();
                new Socket("127.0.0.1", port).close();
                gone.join();
                factory().createSocket("127.0.0.1", port);
            }
        }, "connection refused");
    }

    /**
     * The settings object: defaults, restrictions, and names that are refused.
     * <p>
     * 설정 객체: 기본값, 제한, 거절하는 이름.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConfig() throws Exception {
        Tls13Config d = Tls13Config.defaults();
        Check.equal("[TLSv1.3, TLSv1.2]", Arrays.toString(d.protocols()), "default protocols");
        Check.equal(9, d.cipherSuites().length, "default cipher suites");
        Check.equal("TLS_AES_128_GCM_SHA256", d.cipherSuites()[0], "first cipher suite");
        Check.isTrue(d.trustStore().source().indexOf("ccadb") >= 0 || d.trustStore().source().length() > 0, "the default trust store is the bundled list");
        Tls13Config only12 = d.withProtocols("TLSv1.2");
        Check.equal("[TLSv1.2]", Arrays.toString(only12.protocols()), "TLS 1.2 only");
        Check.equal(6, only12.cipherSuites().length, "TLS 1.2 suites only");
        Check.equal(9, d.cipherSuites().length, "the original is unchanged");
        Check.equal("[TLS_CHACHA20_POLY1305_SHA256]", Arrays.toString(d.withCipherSuites("TLS_CHACHA20_POLY1305_SHA256").cipherSuites()), "one suite");
        final Tls13Config c = d;
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                c.withProtocols("TLSv1.1");
            }
        }, "TLS 1.1");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                c.withProtocols();
            }
        }, "no protocol");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                c.withCipherSuites("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
            }
        }, "CBC suite");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                c.withGroups("ffdhe2048");
            }
        }, "finite field group");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                c.withTrustStore(null);
            }
        }, "no trust store");

        ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
        server.group = ClientHello.GROUP_SECP384R1;
        server.suite = 0x1303;
        server.start();
        Tls13SocketFactory restricted = new Tls13SocketFactory(d.withTrustStore(TlsFixture.trust()).withProtocols("TLSv1.3").withGroups("secp384r1", "x25519")
                .withCipherSuites("TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));
        SSLSocket tls = open(restricted, server);
        tls.startHandshake();
        Check.equal("TLS_CHACHA20_POLY1305_SHA256", tls.getSession().getCipherSuite(), "cipher suite");
        tls.close();
        server.finish();
        Check.equal("[24, 29]", server.offeredGroups.toString(), "configured groups");
        Check.equal("[4867, 4865]", server.offeredSuites.toString(), "configured suites");
        Check.equal("[772]", server.offeredVersions.toString(), "configured versions");
        Check.isFalse(server.retried, "the first configured group is the key share");
    }

    /**
     * Creates a server that answers one HTTP request.
     * <p>
     * HTTP 요청 하나에 답하는 서버를 만든다.
     *
     * @param key the key of the server / 서버의 키
     * @return the server / 서버
     */
    private static ScriptedServer httpServer(TestKey key) {
        ScriptedServer server = TlsFixture.server(key);
        server.script = new ScriptedServer.Script() {
            public void run(ScriptedServer s) throws Exception {
                ByteArrayOutputStream request = new ByteArrayOutputStream();
                // 빈 줄이 올 때까지 요청 헤더를 읽는다.
                while (request.toString("US-ASCII").indexOf("\r\n\r\n") < 0) {
                    RecordLayer.Record r = s.readRecord();
                    if (r == null || r.type != RecordLayer.APPLICATION_DATA) {
                        throw new IllegalStateException("no complete HTTP request: " + request.toString("US-ASCII"));
                    }
                    request.write(r.data, 0, r.data.length);
                }
                String first = request.toString("US-ASCII");
                first = first.substring(0, first.indexOf("\r\n"));
                byte[] body = ("you asked: " + first).getBytes("US-ASCII");
                s.sendData(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
                s.sendData(body);
                s.sendAlert(1, 0);
                s.awaitAlert();
            }
        };
        return server;
    }

    /**
     * The factory installed in an <code>HttpsURLConnection</code> of the running JRE: the request succeeds with RSA and ECDSA server
     * certificates, the server name is sent (the JRE connects the socket and passes the host name), and a certificate that the
     * factory does not trust fails with an exception of the module in the cause chain.
     * <p>
     * 실행 중인 JRE 의 <code>HttpsURLConnection</code> 에 설치한 팩터리: RSA·ECDSA 서버 인증서로 요청이 성공하고, 서버 이름을 보내며(JRE 가 소켓을
     * 연결하고 호스트 이름을 넘긴다), 팩터리가 신뢰하지 않는 인증서는 원인 사슬에 모듈의 예외를 가진 실패로 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsUrlConnection() throws Exception {
        TestKey[] keys = { TlsFixture.rsaKey(), TlsFixture.p256Key() };
        // 서버 인증서의 키 종류마다 요청 하나를 보낸다.
        for (int i = 0; i < keys.length; i++) {
            ScriptedServer server = httpServer(keys[i]).start();
            HttpsURLConnection c = (HttpsURLConnection) new URL("https://localhost:" + server.port() + "/page?x=" + i).openConnection(Proxy.NO_PROXY);
            c.setSSLSocketFactory(factory());
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            Check.equal(200, c.getResponseCode(), "response code");
            Check.equal("TLS_AES_128_GCM_SHA256", c.getCipherSuite(), "cipher suite as the connection reports it");
            Check.equal(1, c.getServerCertificates().length, "server certificates as the connection reports them");
            InputStream in = c.getInputStream();
            String body = new String(readFully(in, 200), "US-ASCII");
            in.close();
            Check.equal("you asked: GET /page?x=" + i + " HTTP/1.1", body, "response body");
            c.disconnect();
            server.finish();
            Check.equal("localhost", server.sni, "the JRE passed the host name, so the server name was sent");
        }

        ScriptedServer untrusted = httpServer(TlsFixture.rsaKey()).start();
        final HttpsURLConnection c = (HttpsURLConnection) new URL("https://localhost:" + untrusted.port() + "/").openConnection(Proxy.NO_PROXY);
        c.setSSLSocketFactory(Tls13SocketFactory.getInstance());
        Throwable t = Check.throwsException(IOException.class, new Block() {
            public void run() throws Exception {
                c.getResponseCode();
            }
        }, "certificate of an unknown root");
        Check.isTrue(Tls13Failures.isModuleFailure(t), "the failure is found in the cause chain: " + t);
        Check.isFalse(Tls13Failures.isNegotiationFailure(t), "and is not a negotiation failure");
        untrusted.join();
    }
}
