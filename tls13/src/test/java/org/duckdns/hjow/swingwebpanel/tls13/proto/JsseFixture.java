package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;

/**
 * Starts a test-only JSSE server on a newer JRE with freshly made loopback credentials.
 * / 새 루프백 인증 자료로 더 새 JRE의 시험용 JSSE 서버를 시작한다.
 */
public final class JsseFixture {

    /** Server JVM. / 서버 JVM. */
    private final Process process;

    /** Root and server key material, removed after the server exits. / 서버 종료 후 지울 루트와 서버 키 자료. */
    private final File material;

    /** Optional legacy protocol policy for a single server process. / 서버 한 프로세스용 옛 프로토콜 정책. */
    private final File security;

    /** Port announced by the server. / 서버가 알린 포트. */
    private final int port;

    /** Connection schemes in order of arrival. / 도착 순서대로 저장한 연결 스킴. */
    private final List<String> connections = Collections.synchronizedList(new ArrayList<String>());

    /**
     * Starts the configured server or skips when the newer JRE is unavailable.
     * / 설정한 서버를 시작한다. 새 JRE가 없으면 시험을 건너뛴다.
     *
     * @param protocol enabled protocol / 활성 프로토콜
     * @param suite enabled suite / 활성 스위트
     * @param group enabled group / 활성 그룹
     * @param mode server behavior / 서버 동작
     * @param ecdsa whether to use an EC leaf / EC 서버 인증서 사용 여부
     * @throws Exception if the configured server fails to start / 지정한 서버가 시작하지 못할 때
     */
    public JsseFixture(String protocol, String suite, String group, String mode, boolean ecdsa) throws Exception {
        String home = System.getenv("SWP_TLS_SERVER_JAVA_HOME");
        boolean windows = File.separatorChar == '\\';
        File java = home == null ? null : new File(home, windows ? "bin/java.exe" : "bin/java");
        if (java == null || !java.isFile()) {
            Check.skip("set SWP_TLS_SERVER_JAVA_HOME to a JDK 11+ with TLS 1.3, X25519 and ChaCha20");
        }
        File keys = null;
        File policy = null;
        Process child = null;
        int readyPort = 0;
        try {
            TestKey key = ecdsa ? TlsFixture.p256Key() : TlsFixture.rsaKey();
            CertBuilder leaf = TlsFixture.certificate(key);
            if ("expired".equals(mode)) {
                long now = System.currentTimeMillis();
                leaf.validity(now - 3 * CertBuilder.DAY, now - CertBuilder.DAY);
            }
            keys = File.createTempFile("swp-tls-test-", ".keys");
            DataOutputStream out = new DataOutputStream(new FileOutputStream(keys));
            try {
                writeBytes(out, key.pkcs8());
                writeBytes(out, leaf.build());
                writeBytes(out, TlsFixture.root());
            } finally {
                out.close();
            }
            List<String> command = new ArrayList<String>();
            Collections.addAll(command, java.getAbsolutePath(), "-Djdk.tls.namedGroups=" + group,
                    "-Djdk.tls.useExtendedMasterSecret=" + !"no-ems".equals(mode));
            if ("TLSv1.1".equals(protocol) || "TLSv1".equals(protocol) || suite.startsWith("TLS_RSA_")) {
                // 정책 변경은 시험용 루프백 서버 JVM에만 적용된다.
                policy = File.createTempFile("swp-tls-test-", ".security");
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(policy), "US-ASCII");
                try {
                    writer.write("jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224\n");
                } finally {
                    writer.close();
                }
                command.add("-Djava.security.properties=" + policy.getAbsolutePath());
            }
            // 자식에게 서버 클래스만 담긴 경로를 주어 모듈과의 의존성이 없음을 확인한다.
            String classes = new File(JsseServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            Collections.addAll(command, "-cp", classes, JsseServer.class.getName(), keys.getAbsolutePath(), key.algorithm(), protocol, suite, mode);
            child = new ProcessBuilder(command).redirectErrorStream(true).start(); // sourcecheck:allow Independent loopback JSSE test process (P32-06).
            final BufferedReader announcements = new BufferedReader(new InputStreamReader(child.getInputStream(), "UTF-8"));
            FutureTask<Integer> startup = new FutureTask<Integer>(new Callable<Integer>() {
                /** Waits for the child port. / 자식 프로세스의 포트를 기다린다. */
                public Integer call() throws Exception {
                    String line;
                    StringBuilder diagnostics = new StringBuilder();
                    // 경고가 먼저 출력될 수 있으므로 포트 알림이나 프로세스 종료까지 읽는다.
                    while ((line = announcements.readLine()) != null) {
                        if (line.startsWith("port ")) {
                            return Integer.valueOf(line.substring(5));
                        }
                        diagnostics.append(line).append('\n');
                    }
                    throw new IllegalStateException("JSSE server did not start: " + diagnostics);
                }
            });
            Thread reader = new Thread(startup, "tls-test-server-startup");
            reader.setDaemon(true);
            reader.start();
            readyPort = startup.get(20, TimeUnit.SECONDS).intValue();
            Thread monitor = new Thread(new Runnable() {
                /** Counts the accepted connections until shutdown. / 종료까지 수락한 연결을 센다. */
                public void run() {
                    try {
                        String line;
                        // 프로세스 출력이 막히지 않도록 종료까지 읽는다.
                        while ((line = announcements.readLine()) != null) {
                            if ("https".equals(line) || "http".equals(line)) {
                                connections.add(line);
                            }
                        }
                    } catch (java.io.IOException closed) {
                        // 자식이 종료되면 파이프도 닫힌다.
                    }
                }
            }, "tls-test-server-connections");
            monitor.setDaemon(true);
            monitor.start();
        } finally {
            if (readyPort == 0) {
                stop(child, keys, policy);
            }
        }
        process = child;
        material = keys;
        security = policy;
        port = readyPort;
    }

    /**
     * Writes one length-prefixed fixture value. / 길이가 붙은 시험 자료 값을 쓴다.
     *
     * @param out fixture stream / 시험 자료 스트림
     * @param value the encoded value / 부호화된 값
     * @throws java.io.IOException on write failure / 쓰기 실패 시
     */
    private static void writeBytes(DataOutputStream out, byte[] value) throws java.io.IOException {
        out.writeInt(value.length);
        out.write(value);
    }

    /**
     * Returns the listening port. / 수신 포트를 반환한다.
     *
     * @return TCP port / TCP 포트
     */
    public int port() {
        return port;
    }

    /**
     * Reports the number of accepted connections so far. / 지금까지 수락한 연결 수를 반환한다.
     *
     * @return number of connections / 연결 수
     */
    public int connections() {
        return connections.size();
    }

    /**
     * Reports the connection schemes in order. / 연결 스킴을 순서대로 보고한다.
     *
     * @return ordered list snapshot / 순서가 있는 목록 사본
     */
    public String connectionOrder() {
        return connections.toString();
    }

    /**
     * Returns the loopback HTTPS base. / 루프백 HTTPS 기준 주소를 반환한다.
     *
     * @return address without a path / 경로 없는 주소
     */
    public String base() {
        return "https://127.0.0.1:" + port;
    }

    /**
     * Returns a trust store with the test root only. / 시험 루트만 넣은 신뢰 저장소를 반환한다.
     *
     * @return test trust store / 시험 신뢰 저장소
     * @throws Exception if the store cannot be built / 저장소를 만들 수 없을 때
     */
    public static KeyStore trustKeys() throws Exception {
        KeyStore roots = KeyStore.getInstance("JKS");
        roots.load(null, null);
        roots.setCertificateEntry("test", CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(TlsFixture.root())));
        return roots;
    }

    /** Stops the child and removes the temporary material. / 자식을 종료하고 임시 자료를 지운다. */
    public void close() {
        stop(process, material, security);
    }

    /**
     * Releases resources even when startup failed. / 시작에 실패했어도 자원을 반납한다.
     *
     * @param child the child or null / 자식 프로세스 또는 null
     * @param keys the key file or null / 키 파일 또는 null
     * @param policy the policy file or null / 정책 파일 또는 null
     */
    private static void stop(Process child, File keys, File policy) {
        if (child != null) {
            child.destroy();
            try {
                child.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (keys != null && !keys.delete()) {
            keys.deleteOnExit();
        }
        if (policy != null && !policy.delete()) {
            policy.deleteOnExit();
        }
    }
}
