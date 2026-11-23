package org.duckdns.hjow.swingwebpanel.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestFtpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of {@link ClasspathLoader} (plan item P05-06) with a directory and a JAR on a private class loader, and of the
 * scheme dispatch of {@link ResourceFetcher} (plan items P04-02, P05-05): classpath normalization, traversal attempts,
 * directories, resources inside JARs, blocked schemes never opening anything, and remote pages not reading the class path.
 * <p>
 * 전용 클래스 로더 위의 디렉터리와 JAR 로 {@link ClasspathLoader} 를 시험하고(계획 P05-06), {@link ResourceFetcher} 의 스킴 분기를
 * 시험한다(계획 P04-02, P05-05): 클래스패스 정규화, 경로 탈출 시도, 디렉터리, JAR 안의 리소스, 아무것도 열지 않는 차단 스킴,
 * 원격 페이지가 클래스패스를 읽지 못함.
 */
public class ClasspathAndDispatchTest {

    /**
     * Temporary directory acting as a class path directory.
     * <p>
     * 클래스패스 디렉터리 역할을 하는 임시 디렉터리.
     */
    private File dir;
    /**
     * Temporary JAR file acting as a class path JAR.
     * <p>
     * 클래스패스 JAR 역할을 하는 임시 JAR 파일.
     */
    private File jar;
    /**
     * Private class loader over the directory and the JAR.
     * <p>
     * 디렉터리와 JAR 위의 전용 클래스 로더.
     */
    private URLClassLoader loader;
    /**
     * Servers started by a case.
     * <p>
     * 사례가 시작한 서버들.
     */
    private final List<Object> servers = new ArrayList<Object>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ClasspathAndDispatchTest() {
    }

    /**
     * Builds the directory and the JAR.
     * <p>
     * 디렉터리와 JAR 를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        File base = File.createTempFile("swpcp", "");
        base.delete();
        dir = new File(base, "classes");
        new File(dir, "sub").mkdirs();
        write(new File(dir, "hello.html"), "<p>hello</p>");
        write(new File(dir, "with space.txt"), "spaced");
        write(new File(dir, "sub/page.css"), "body{}");
        jar = new File(base, "res.jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar));
        try {
            jos.putNextEntry(new JarEntry("jarred/"));
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("jarred/in-jar.html"));
            jos.write("<p>from jar</p>".getBytes("UTF-8"));
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("jarred/readme"));
            jos.write("no extension".getBytes("UTF-8"));
            jos.closeEntry();
        } finally {
            jos.close();
        }
        loader = new URLClassLoader(new URL[] { dir.toURI().toURL(), jar.toURI().toURL() }, null);
    }

    /**
     * Writes a small UTF-8 file creating its parent directories.
     * <p>
     * 작은 UTF-8 파일을 부모 디렉터리와 함께 만든다.
     *
     * @param f    file / 파일
     * @param text content / 내용
     * @throws IOException on failure / 실패 시
     */
    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /**
     * Stops servers and deletes the temporary files.
     * <p>
     * 서버를 멈추고 임시 파일을 지운다.
     */
    public void tearDown() {
        // 이 사례가 시작한 서버를 모두 멈춘다.
        for (int i = 0; i < servers.size(); i++) {
            Object s = servers.get(i);
            if (s instanceof TestHttpServer) {
                ((TestHttpServer) s).stop();
            } else {
                ((TestFtpServer) s).stop();
            }
        }
        servers.clear();
        delete(dir.getParentFile());
    }

    /**
     * Deletes a file or directory tree.
     * <p>
     * 파일 또는 디렉터리 트리를 지운다.
     *
     * @param f file or directory / 파일 또는 디렉터리
     */
    private static void delete(File f) {
        File[] kids = f.listFiles();
        // 하위 항목부터 지운다.
        for (int i = 0; kids != null && i < kids.length; i++) {
            delete(kids[i]);
        }
        f.delete();
    }

    /**
     * Loads a classpath URL with the private loader.
     * <p>
     * 전용 로더로 클래스패스 URL 을 읽는다.
     *
     * @param url classpath URL / 클래스패스 URL
     * @return the response / 응답
     */
    private ResourceResponse load(String url) {
        return new ClasspathLoader(loader, null).load(new ResourceRequest(url), null);
    }

    /**
     * Resources load with the right media type; relative references resolve against the canonical form.
     * <p>
     * 리소스가 올바른 미디어 타입으로 읽히고 상대 참조는 정규 형식을 기준으로 풀린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoadsFromDirectoryAndJar() throws Exception {
        ResourceResponse r = load("classpath://hello.html");
        Check.equal(200, r.getStatus(), r.getFailureMessage());
        Check.equal("text/html", r.getMimeType(), "html mime");
        Check.equal("<p>hello</p>", new String(r.getBody(), "UTF-8"), "content");
        Check.equal("classpath://hello.html", r.getFinalUrl(), "canonical final url");
        Check.equal("text/css", load("classpath://sub/page.css").getMimeType(), "css mime");
        Check.equal("<p>from jar</p>", new String(load("classpath://jarred/in-jar.html").getBody(), "UTF-8"), "resource inside a jar");
        Check.equal("application/octet-stream", load("classpath://jarred/readme").getMimeType(), "no extension");
        String css = UrlUtil.resolve("classpath://sub/index.html", "page.css");
        Check.equal("classpath://sub/page.css", css, "relative resource of a classpath document");
        Check.equal(200, load(css).getStatus(), "the resolved url loads");
        Check.equal(200, load("classpath:///hello.html").getStatus(), "extra slashes");
        Check.equal(200, load("classpath://hello.html#frag").getStatus(), "fragment ignored");
        Check.equal("classpath://hello.html#frag", load("classpath://hello.html#frag").getFinalUrl(), "fragment kept in the url");
    }

    /**
     * Percent-encoded names (spaces, Hangul) are decoded once; dot segments are normalized.
     * <p>
     * 퍼센트 인코딩된 이름(공백, 한글)은 한 번 디코딩하고 점 세그먼트는 정규화한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPercentEncodingAndNormalization() throws Exception {
        Check.equal("spaced", new String(load("classpath://with%20space.txt").getBody(), "UTF-8"), "%20");
        Check.equal("spaced", new String(load("classpath://with space.txt").getBody(), "UTF-8"), "a raw space is encoded for the user");
        Check.equal(200, load("classpath://sub/./page.css").getStatus(), "dot segment");
        Check.equal(200, load("classpath://sub/x/../page.css").getStatus(), "dot dot segment");
        Check.equal(200, load("classpath://sub/%2E%2E/hello.html").getStatus(), "encoded dot dot stays inside the root");
        Check.equal("with space.txt", ClasspathLoader.resourceName("classpath://with%20space.txt"), "resource name decoded once");
        Check.equal(404, load("classpath://with%2520space.txt").getStatus(), "no double decoding");
    }

    /**
     * Attempts to leave the class path root or to reach files and URLs are answered with 404 / NOT_FOUND.
     * <p>
     * 클래스패스 루트를 벗어나거나 파일/URL 에 닿으려는 시도는 404 / NOT_FOUND 로 답한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTraversalAndEscapesAreRejected() throws Exception {
        String[] bad = { "classpath://%2e%2e/etc/passwd", "classpath://sub/%2e%2e/%2e%2e/x", "classpath://..%2F..%2Fsecret",
            "classpath://a%5Cb", "classpath://hello.html%00.txt", "classpath://file:/etc/passwd", "classpath://jar:file:/x.jar!/a",
            "classpath://sub%2F..%2F..%2Fhello.html", "classpath://", "classpath:///" };
        // 위험하거나 잘못된 주소는 모두 찾을 수 없음으로 답해야 한다.
        for (int i = 0; i < bad.length; i++) {
            ResourceResponse r = load(bad[i]);
            Check.equal(404, r.getStatus(), "status for " + bad[i]);
            Check.equal(FailureKind.NOT_FOUND, r.getFailureKind(), "kind for " + bad[i]);
            Check.equal(0, r.getBody().length, "no body for " + bad[i]);
        }
        Check.isNull(ClasspathLoader.resourceName("classpath://../x"), "an unencoded .. is clamped by URL resolution, but a decoded one is refused");
        Check.equal("x", ClasspathLoader.resourceName(UrlUtil.normalize("classpath://../x")), "URL normalization clamps at the root");
    }

    /**
     * Directories and missing resources are not found; directory contents are never listed.
     * <p>
     * 디렉터리와 없는 리소스는 찾을 수 없음이며 디렉터리 내용은 나열하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDirectoriesAndMissingResources() throws Exception {
        String[] none = { "classpath://sub", "classpath://sub/", "classpath://jarred", "classpath://jarred/", "classpath://missing.html" };
        // 디렉터리 이름이든 없는 이름이든 모두 404 이어야 한다.
        for (int i = 0; i < none.length; i++) {
            ResourceResponse r = load(none[i]);
            Check.equal(404, r.getStatus(), "status for " + none[i]);
            Check.equal(FailureKind.NOT_FOUND, r.getFailureKind(), "kind for " + none[i]);
        }
    }

    /**
     * A size limit applies in memory but not when streaming.
     * <p>
     * 크기 제한은 메모리에만 적용되고 스트리밍에는 적용되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizeLimit() throws Exception {
        ClasspathLoader small = new ClasspathLoader(loader, FetchLimits.DEFAULT.withMaxMemoryBytes(5));
        Check.equal(FailureKind.TOO_LARGE, small.load(new ResourceRequest("classpath://hello.html"), null).getFailureKind(), "over the limit");
        final java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream();
        ResourceResponse streamed = small.load(new ResourceRequest("classpath://hello.html"), new ResponseHandler() {
            /**
             * Streams the body.
             * <p>
             * 본문을 스트리밍한다.
             *
             * @param head response head / 응답 머리
             * @return the sink / sink
             */
            public java.io.OutputStream onResponse(ResourceResponse head) {
                return sink;
            }
        });
        Check.equal(FailureKind.NONE, streamed.getFailureKind(), "streaming is unlimited");
        Check.equal("<p>hello</p>", new String(sink.toByteArray(), "UTF-8"), "streamed content");
    }

    /**
     * The fetcher dispatches by scheme and answers about:blank itself.
     * <p>
     * 가져오기 객체가 스킴에 따라 분기하고 about:blank 는 스스로 답한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDispatchByScheme() throws Exception {
        TestHttpServer http = new TestHttpServer();
        servers.add(http);
        http.routeFixed("/x", TestHttpServer.Response.html("via http"));
        http.start();
        TestFtpServer ftp = new TestFtpServer();
        servers.add(ftp);
        ftp.addFile("/f.txt", "via ftp".getBytes("UTF-8"));
        ftp.start();
        ResourceFetcher f = new ResourceFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000), loader);
        Check.equal("via http", new String(f.fetch(new ResourceRequest(http.url("/x"))).getBody(), "UTF-8"), "http");
        Check.equal("via ftp", new String(f.fetch(new ResourceRequest(ftp.url("/f.txt"))).getBody(), "UTF-8"), "ftp");
        Check.equal("<p>hello</p>", new String(f.fetch(new ResourceRequest("classpath://hello.html")).getBody(), "UTF-8"), "classpath");
        ResourceResponse blank = f.fetch(new ResourceRequest("about:blank"));
        Check.equal(200, blank.getStatus(), "about:blank");
        Check.equal(0, blank.getBody().length, "empty page");
    }

    /**
     * Forbidden schemes are refused by the single entry point and never touch the network or the file system.
     * <p>
     * 금지된 스킴은 단일 진입점에서 거부되며 네트워크나 파일 시스템을 건드리지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedSchemesNeverConnect() throws Exception {
        TestHttpServer http = new TestHttpServer();
        servers.add(http);
        http.start();
        ResourceFetcher f = new ResourceFetcher(null, FetchLimits.DEFAULT.withTimeouts(2000, 2000), loader);
        File marker = new File(dir, "marker.txt");
        write(marker, "inside classpath dir");
        String[] blocked = { marker.toURI().toString(), "file:///" + marker.getPath().replace('\\', '/'), "jar:" + jar.toURI() + "!/jarred/readme",
            "mailto:a@b.c", "javascript:alert(1)", "data:text/html,x", "blob:http://h/1", "ws://localhost:" + http.getPort() + "/",
            "wss://localhost:" + http.getPort() + "/", "about:text", "about:srcdoc", "gopher://localhost/" };
        // 금지 스킴마다 BLOCKED_SCHEME 이고 본문이 없어야 한다.
        for (int i = 0; i < blocked.length; i++) {
            ResourceResponse r = f.fetch(new ResourceRequest(blocked[i]));
            Check.equal(FailureKind.BLOCKED_SCHEME, r.getFailureKind(), "blocked: " + blocked[i]);
            Check.equal(0, r.getBody().length, "nothing was read for " + blocked[i]);
        }
        Check.equal(FailureKind.INVALID_URL, f.fetch(new ResourceRequest("relative.html")).getFailureKind(), "relative url");
        Check.equal(0, http.totalRequests(), "the server saw nothing");
    }

    /**
     * Pages from the network cannot read class path resources, but class path pages can load from everywhere.
     * <p>
     * 네트워크에서 온 페이지는 클래스패스 리소스를 읽을 수 없고, 클래스패스 페이지는 어디서든 읽을 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRemotePagesCannotReadTheClasspath() throws Exception {
        ResourceFetcher f = new ResourceFetcher(null, FetchLimits.DEFAULT, loader);
        ResourceResponse fromHttp = f.fetch(new ResourceRequest("classpath://hello.html").setInitiator(Origin.of("http://evil.example/")));
        Check.equal(FailureKind.BLOCKED_SCHEME, fromHttp.getFailureKind(), "http page");
        ResourceResponse fromFtp = f.fetch(new ResourceRequest("classpath://hello.html").setInitiator(Origin.of("ftp://evil.example/")));
        Check.equal(FailureKind.BLOCKED_SCHEME, fromFtp.getFailureKind(), "ftp page");
        ResourceResponse fromCp = f.fetch(new ResourceRequest("classpath://hello.html").setInitiator(Origin.of("classpath://other.html")));
        Check.equal(200, fromCp.getStatus(), "classpath page");
        Check.equal(200, f.fetch(new ResourceRequest("classpath://hello.html").setInitiator(Origin.opaque())).getStatus(), "opaque initiator (about:blank, about:text)");
        Check.equal(200, f.fetch(new ResourceRequest("classpath://hello.html")).getStatus(), "no initiator (top level navigation)");
    }
}
