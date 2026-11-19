package webfetch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command line web page fetcher used by the SwingWebPanel development tools.
 * It downloads one URL, follows redirects, optionally decodes a Base64 body (Gitiles
 * <code>?format=TEXT</code>) or strips HTML markup, and prints source metadata
 * (URL, retrieval date, status, JRE version) to the error stream so the result can be
 * quoted with its provenance. Written in Java 6 language level; uses only public JRE APIs.
 * <p>
 * Usage: <code>java -cp build/classes webfetch.WebFetch [options] URL</code>
 * <p>
 * 명령행 웹 페이지 조회 도구. 하나의 URL을 내려받아 리디렉션을 따라가고, 필요하면 Base64 본문
 * (Gitiles <code>?format=TEXT</code>)을 해독하거나 HTML 태그를 제거한다. 원본 URL, 조회 날짜,
 * 상태 코드, JRE 버전 같은 출처 정보는 오류 스트림에 출력하여 결과 인용 시 근거로 남긴다.
 * Java 6 문법으로 작성했고 JRE 공개 API만 사용한다.
 * <p>
 * 사용법: <code>java -cp build/classes webfetch.WebFetch [옵션] URL</code>
 */
public class WebFetch {

    /**
     * Exit code: success. / 종료 코드: 성공.
     */
    public static final int EXIT_OK = 0;

    /**
     * Exit code: invalid command line usage. / 종료 코드: 잘못된 명령행 사용.
     */
    public static final int EXIT_USAGE = 1;

    /**
     * Exit code: network or TLS failure. / 종료 코드: 네트워크 또는 TLS 실패.
     */
    public static final int EXIT_NETWORK = 2;

    /**
     * Exit code: the server answered with an HTTP error status (400 or above).
     * / 종료 코드: 서버가 HTTP 오류 상태(400 이상)로 응답함.
     */
    public static final int EXIT_HTTP = 3;

    /**
     * Maximum number of redirects followed. / 따라갈 최대 리디렉션 횟수.
     */
    public static final int MAX_REDIRECTS = 10;

    /**
     * Default connect and read timeout in milliseconds. / 기본 연결 및 읽기 제한 시간(밀리초).
     */
    public static final int DEFAULT_TIMEOUT_MS = 30000;

    /**
     * Default maximum body size in bytes (32 MB). / 기본 최대 본문 크기(바이트, 32MB).
     */
    public static final int DEFAULT_MAX_BYTES = 32 * 1024 * 1024;

    /**
     * Program entry point. Calls {@link #run(String[], PrintStream, PrintStream)} and exits with its code.
     * / 프로그램 진입점. {@link #run(String[], PrintStream, PrintStream)}를 호출하고 그 코드로 종료한다.
     *
     * @param args command line arguments / 명령행 인수
     */
    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        System.exit(code);
    }

    /**
     * Runs the tool. The body is written to the output stream as raw bytes, metadata goes to the error stream.
     * / 도구를 실행한다. 본문은 원시 바이트로 출력 스트림에, 출처 정보는 오류 스트림에 출력한다.
     *
     * @param args command line arguments / 명령행 인수
     * @param out  stream receiving the body / 본문을 받는 스트림
     * @param err  stream receiving metadata and messages / 출처 정보와 메시지를 받는 스트림
     * @return exit code, one of the EXIT_ constants / EXIT_ 상수 중 하나인 종료 코드
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        String url = null;
        String outFile = null;
        boolean text = false;
        boolean base64 = false;
        boolean quiet = false;
        int timeout = DEFAULT_TIMEOUT_MS;
        int maxBytes = DEFAULT_MAX_BYTES;
        String userAgent = "swingwebpanel-webfetch/1.0";
        List<String[]> headers = new ArrayList<String[]>();

        // 명령행 인수를 순서대로 해석한다.
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--out".equals(a) && i + 1 < args.length) {
                outFile = args[++i];
            } else if ("--text".equals(a)) {
                text = true;
            } else if ("--base64".equals(a)) {
                base64 = true;
            } else if ("--quiet".equals(a)) {
                quiet = true;
            } else if ("--timeout".equals(a) && i + 1 < args.length) {
                timeout = parseInt(args[++i], DEFAULT_TIMEOUT_MS);
            } else if ("--max-bytes".equals(a) && i + 1 < args.length) {
                maxBytes = parseInt(args[++i], DEFAULT_MAX_BYTES);
            } else if ("--user-agent".equals(a) && i + 1 < args.length) {
                userAgent = args[++i];
            } else if ("--header".equals(a) && i + 1 < args.length) {
                String h = args[++i];
                int idx = h.indexOf(':');
                if (idx <= 0) {
                    err.println("webfetch: invalid --header (use Name: value)");
                    return EXIT_USAGE;
                }
                headers.add(new String[] { h.substring(0, idx).trim(), h.substring(idx + 1).trim() });
            } else if (a.startsWith("--")) {
                err.println("webfetch: unknown option " + a);
                return usage(err);
            } else if (url == null) {
                url = a;
            } else {
                err.println("webfetch: only one URL is accepted");
                return usage(err);
            }
        }
        if (url == null) {
            return usage(err);
        }

        try {
            URL current = new URL(url);
            HttpURLConnection conn = null;
            int status = 0;
            // 리디렉션을 직접 따라간다. 스킴이 달라져도(http<->https) 허용한다.
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                URLConnection raw = current.openConnection();
                if (!(raw instanceof HttpURLConnection)) {
                    err.println("webfetch: only http and https are supported");
                    return EXIT_USAGE;
                }
                conn = (HttpURLConnection) raw;
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setRequestProperty("Accept", "*/*");
                // 사용자가 지정한 추가 헤더를 요청에 모두 설정한다.
                for (int h = 0; h < headers.size(); h++) {
                    conn.setRequestProperty(headers.get(h)[0], headers.get(h)[1]);
                }
                status = conn.getResponseCode();
                if (status >= 300 && status < 400 && conn.getHeaderField("Location") != null) {
                    String loc = conn.getHeaderField("Location");
                    conn.disconnect();
                    if (hop == MAX_REDIRECTS) {
                        err.println("webfetch: too many redirects");
                        return EXIT_NETWORK;
                    }
                    current = new URL(current, loc);
                    continue;
                }
                break;
            }

            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] body = in == null ? new byte[0] : readAll(in, maxBytes);
            if (in != null) {
                in.close();
            }

            if (base64) {
                body = decodeBase64(body);
            }
            String charset = charsetOf(conn.getContentType());
            if (text) {
                String decoded = new String(body, charset);
                body = stripHtml(decoded).getBytes("UTF-8");
            }

            if (!quiet) {
                printMeta(err, url, current, status, conn, body.length);
            }
            if (outFile != null) {
                OutputStream fo = new FileOutputStream(new File(outFile));
                try {
                    fo.write(body);
                } finally {
                    fo.close();
                }
            } else {
                out.write(body, 0, body.length);
                out.flush();
            }
            conn.disconnect();
            return status >= 400 ? EXIT_HTTP : EXIT_OK;
        } catch (IOException e) {
            err.println("webfetch: network failure: " + e);
            err.println("webfetch: running JRE = " + System.getProperty("java.version")
                    + " (JRE 6u45 cannot negotiate TLS 1.2 / SNI; retry with a newer JRE)");
            return EXIT_NETWORK;
        }
    }

    /**
     * Prints provenance metadata (original URL, final URL, retrieval date, status, JRE version, validators).
     * / 출처 메타데이터(원본 URL, 최종 URL, 조회 날짜, 상태, JRE 버전, 검증자)를 출력한다.
     *
     * @param err    metadata stream / 메타데이터 스트림
     * @param url    requested URL / 요청 URL
     * @param fin    final URL after redirects / 리디렉션 이후 최종 URL
     * @param status HTTP status code / HTTP 상태 코드
     * @param conn   last connection / 마지막 연결
     * @param length output body length / 출력 본문 길이
     */
    private static void printMeta(PrintStream err, String url, URL fin, int status,
            HttpURLConnection conn, int length) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        err.println("# source-url: " + url);
        err.println("# final-url: " + fin);
        err.println("# retrieved: " + today);
        err.println("# status: " + status);
        err.println("# content-type: " + conn.getContentType());
        err.println("# last-modified: " + conn.getHeaderField("Last-Modified"));
        err.println("# etag: " + conn.getHeaderField("ETag"));
        err.println("# body-bytes: " + length);
        err.println("# jre: " + System.getProperty("java.version"));
    }

    /**
     * Prints the usage message. / 사용법을 출력한다.
     *
     * @param err error stream / 오류 스트림
     * @return {@link #EXIT_USAGE}
     */
    private static int usage(PrintStream err) {
        err.println("usage: java webfetch.WebFetch [--out FILE] [--text] [--base64] [--quiet]");
        err.println("                              [--timeout MS] [--max-bytes N] [--user-agent UA]");
        err.println("                              [--header \"Name: value\"] URL");
        return EXIT_USAGE;
    }

    /**
     * Parses an integer option, falling back to a default for invalid text.
     * / 정수 옵션을 해석하며 잘못된 문자열이면 기본값을 사용한다.
     *
     * @param s   text / 문자열
     * @param def default value / 기본값
     * @return parsed value / 해석한 값
     */
    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Reads a whole stream with a size limit. / 크기 제한을 두고 스트림 전체를 읽는다.
     *
     * @param in    input stream / 입력 스트림
     * @param limit maximum bytes / 최대 바이트
     * @return the bytes / 읽은 바이트
     * @throws IOException on read failure or when the limit is exceeded / 읽기 실패나 제한 초과 시
     */
    static byte[] readAll(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        // 제한 크기까지 반복해서 읽는다.
        while ((n = in.read(buf)) >= 0) {
            bos.write(buf, 0, n);
            if (bos.size() > limit) {
                throw new IOException("response exceeds " + limit + " bytes");
            }
        }
        return bos.toByteArray();
    }

    /**
     * Extracts the charset parameter of a Content-Type header, defaulting to UTF-8.
     * / Content-Type 헤더의 charset 매개변수를 추출하며 기본값은 UTF-8이다.
     *
     * @param contentType header value or null / 헤더 값 또는 null
     * @return charset name / 문자셋 이름
     */
    static String charsetOf(String contentType) {
        if (contentType != null) {
            Matcher m = Pattern.compile("charset=\"?([A-Za-z0-9_\\-:.]+)", Pattern.CASE_INSENSITIVE).matcher(contentType);
            if (m.find()) {
                String cs = m.group(1);
                if (java.nio.charset.Charset.isSupported(cs)) {
                    return cs;
                }
            }
        }
        return "UTF-8";
    }

    /**
     * Decodes Base64 text, ignoring whitespace and padding. Own implementation because
     * <code>java.util.Base64</code> does not exist in Java 6.
     * / Base64 문자열을 해독한다(공백과 패딩 무시). Java 6에는 <code>java.util.Base64</code>가 없어 직접 구현했다.
     *
     * @param data Base64 bytes / Base64 바이트
     * @return decoded bytes / 해독된 바이트
     */
    static byte[] decodeBase64(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int acc = 0;
        int bits = 0;
        // 문자 하나마다 6비트를 누적하고 8비트가 차면 바이트를 낸다.
        for (int i = 0; i < data.length; i++) {
            int c = data[i];
            int v;
            if (c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if (c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if (c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if (c == '+' || c == '-') {
                v = 62;
            } else if (c == '/' || c == '_') {
                v = 63;
            } else {
                continue;
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                bos.write((acc >> bits) & 0xFF);
            }
        }
        return bos.toByteArray();
    }

    /**
     * Converts HTML to readable plain text: removes script/style blocks and tags, decodes common entities,
     * and collapses blank lines.
     * / HTML을 읽기 쉬운 텍스트로 바꾼다. script/style 블록과 태그를 제거하고 주요 문자 참조를 해독하며 빈 줄을 줄인다.
     *
     * @param html HTML text / HTML 문자열
     * @return plain text / 평문
     */
    static String stripHtml(String html) {
        String s = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        s = s.replaceAll("(?i)<br\\s*/?>|</(p|div|li|tr|h[1-6]|pre|section|dt|dd)>", "\n");
        s = s.replaceAll("(?s)<[^>]*>", "");
        s = s.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        s = s.replaceAll("[ \\t\\r\\f]+", " ");
        s = s.replaceAll("(?m)^ +| +$", "");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim() + "\n";
    }
}
