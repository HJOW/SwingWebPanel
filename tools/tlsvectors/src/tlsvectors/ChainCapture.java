package tlsvectors;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.cert.Certificate;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Records the certificate chains that real HTTPS servers present, as test input for the certificate path validation of the TLS module
 * (plan item P32-04). The connection is made with the TLS implementation of the running JRE (which also validates the chain), so use a
 * current JRE. Run: <code>java -cp build/classes tlsvectors.ChainCapture OUTPUT_FILE host...</code>. Exit code: 0 = all hosts recorded,
 * 1 = usage error, 2 = a host could not be reached.
 * <p>
 * 실제 HTTPS 서버가 내놓는 인증서 사슬을 기록한다. TLS 모듈의 인증서 경로 검증 시험 입력으로 쓴다(계획 P32-04). 연결은 실행 중인 JRE 의 TLS 구현으로
 * 하므로(그 구현이 사슬도 검증한다) 최신 JRE 를 쓴다. 실행: <code>java -cp build/classes tlsvectors.ChainCapture 출력파일 호스트...</code>.
 * 종료 코드: 0 = 모든 호스트를 기록함, 1 = 사용법 오류, 2 = 닿지 못한 호스트가 있음.
 */
public final class ChainCapture {

    /** The Base64 alphabet. / Base64 알파벳. */
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * Private constructor: the class has only static methods.
     * <p>
     * 비공개 생성자: 이 클래스에는 정적 메서드만 있다.
     */
    private ChainCapture() {
    }

    /**
     * Encodes bytes as Base64 on one line.
     * <p>
     * 바이트를 한 줄의 Base64 로 부호화한다.
     *
     * @param data the bytes / 바이트
     * @return the text / 텍스트
     */
    private static String base64(byte[] data) {
        StringBuilder sb = new StringBuilder();
        // 세 바이트씩 네 글자로 바꾼다.
        for (int i = 0; i < data.length; i += 3) {
            int b0 = data[i] & 0xff;
            int b1 = i + 1 < data.length ? data[i + 1] & 0xff : 0;
            int b2 = i + 2 < data.length ? data[i + 2] & 0xff : 0;
            sb.append(ALPHABET.charAt(b0 >> 2)).append(ALPHABET.charAt(((b0 & 3) << 4) | (b1 >> 4)));
            sb.append(i + 1 < data.length ? ALPHABET.charAt(((b1 & 15) << 2) | (b2 >> 6)) : '=');
            sb.append(i + 2 < data.length ? ALPHABET.charAt(b2 & 63) : '=');
        }
        return sb.toString();
    }

    /**
     * Entry point.
     * <p>
     * 진입점.
     *
     * @param args the output file and the host names / 출력 파일과 호스트 이름들
     * @throws Exception if the output cannot be written / 출력을 쓸 수 없으면
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: ChainCapture OUTPUT_FILE host...");
            System.exit(1);
        }
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new java.io.FileOutputStream(args[0]), "UTF-8"));
        boolean failed = false;
        out.print("# Certificate chains presented by real HTTPS servers, recorded by tools/tlsvectors (ChainCapture) with the TLS implementation\n");
        out.print("# of the JRE below. The certificates are public data of the servers; they expire, so tests validate them at the recorded time.\n");
        out.print("# jre: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n");
        out.print("# format: chain <host> <time of the recording in milliseconds since 1970> <protocol> <certificates in Base64 DER, the server certificate first>\n");
        // 호스트마다 연결해 서버가 보낸 사슬을 기록한다.
        for (int i = 1; i < args.length; i++) {
            try {
                SSLSocket s = (SSLSocket) SSLSocketFactory.getDefault().createSocket(args[i], 443);
                try {
                    s.setSoTimeout(20000);
                    s.startHandshake();
                    Certificate[] chain = s.getSession().getPeerCertificates();
                    StringBuilder line = new StringBuilder("chain " + args[i] + " " + System.currentTimeMillis() + " " + s.getSession().getProtocol());
                    // 인증서를 차례로 덧붙인다.
                    for (int k = 0; k < chain.length; k++) {
                        line.append(' ').append(base64(chain[k].getEncoded()));
                    }
                    out.print(line.append('\n'));
                    System.err.println("# " + args[i] + ": " + chain.length + " certificates, " + s.getSession().getProtocol());
                } finally {
                    s.close();
                }
            } catch (Exception e) {
                System.err.println("[ERROR] " + args[i] + ": " + e);
                failed = true;
            }
        }
        out.close();
        System.exit(failed ? 2 : 0);
    }
}
