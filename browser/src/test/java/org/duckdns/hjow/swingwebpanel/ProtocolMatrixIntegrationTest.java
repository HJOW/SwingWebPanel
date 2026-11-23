package org.duckdns.hjow.swingwebpanel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.duckdns.hjow.swingwebpanel.download.DownloadControl;
import org.duckdns.hjow.swingwebpanel.download.DownloadHandle;
import org.duckdns.hjow.swingwebpanel.download.DownloadRequest;
import org.duckdns.hjow.swingwebpanel.download.DownloadState;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestFtpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestSsl;

/**
 * The addresses, the content types, the error pages and the files that a host application meets, one by one through the real component
 * (plan item P22-06): <code>http:</code>, <code>https:</code> with the overridden <code>configureSsl</code> and with the retry over http,
 * <code>ftp:</code>, <code>classpath:</code> and the special addresses, the four content types, the 404 and 500 pages of a server, a
 * refused connection, a blocked scheme, the pages that ask for WebGL or WebGPU, and the download popup for attachments, for files that
 * cannot be shown and for <code>download</code> links. The component is a subclass that overrides the protected methods that the README
 * names for the SSL trust, the download popup and the download directory.
 * <p>
 * 호스트 응용이 만나는 주소, 콘텐츠 타입, 오류 화면, 파일을 실제 컴포넌트로 하나씩 확인한다(계획 P22-06): <code>http:</code>, 재정의한
 * <code>configureSsl</code> 을 쓰는 <code>https:</code> 와 http 재시도, <code>ftp:</code>, <code>classpath:</code> 와 특수 주소, 네 콘텐츠 타입, 서버의 404 와
 * 500 화면, 거부된 연결, 차단된 스킴, WebGL 이나 WebGPU 를 요청하는 페이지, 첨부 파일과 보여 줄 수 없는 파일과 <code>download</code> 링크의 다운로드
 * 팝업. 컴포넌트는 README 가 이름을 밝힌 SSL 신뢰, 다운로드 팝업, 다운로드 디렉터리의 보호 메서드를 재정의한 하위 클래스이다.
 */
public class ProtocolMatrixIntegrationTest {

    /**
     * A component whose protected methods are overridden the way a host application does it.
     * <p>
     * 호스트 응용이 하는 것처럼 보호 메서드를 재정의한 컴포넌트.
     */
    private static final class HostPanel extends SwingWebPanel {
        /** The number of calls of <code>configureSsl</code>. / <code>configureSsl</code> 호출 수. */
        volatile int sslCalls;
        /** The popups that were created: address, file name and type. / 만든 팝업: 주소, 파일 이름, 타입. */
        final List<String> popups = Collections.synchronizedList(new ArrayList<String>());
        /** The states that the popups were told. / 팝업에 알린 상태. */
        final List<DownloadState> states = Collections.synchronizedList(new ArrayList<DownloadState>());
        /** The directory of the downloads. / 다운로드 디렉터리. */
        final File dir;

        /**
         * Creates the component.
         * <p>
         * 컴포넌트를 만든다.
         *
         * @param dir the directory of the downloads / 다운로드 디렉터리
         */
        HostPanel(File dir) {
            this.dir = dir;
        }

        /**
         * Trusts the test certificate on this connection only and counts the call.
         * <p>
         * 이 연결에만 시험 인증서를 신뢰하고 호출을 센다.
         *
         * @param connection the https connection / https 연결
         * @throws IOException if the trust cannot be set / 신뢰를 정할 수 없으면
         */
        protected void configureSsl(HttpsURLConnection connection) throws IOException {
            sslCalls++;
            try {
                connection.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
            } catch (Exception e) {
                throw new IOException(e.toString());
            }
        }

        /**
         * Records the popup and returns a handle that shows nothing and accepts the saving.
         * <p>
         * 팝업을 기록하고 아무것도 보여 주지 않으며 저장을 받아들이는 핸들을 반환한다.
         *
         * @param request the request / 요청
         * @param control the control / 제어 객체
         * @return the handle / 핸들
         */
        protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
            popups.add(request.getUrl() + "|" + request.getFileName() + "|" + request.getMimeType());
            return new DownloadHandle() {
                /**
                 * Ignores the target.
                 * <p>
                 * 대상을 무시한다.
                 *
                 * @param target the target / 대상
                 */
                public void setTarget(File target) {
                    return;
                }

                /**
                 * Ignores the progress.
                 * <p>
                 * 진행을 무시한다.
                 *
                 * @param done  the bytes / 바이트
                 * @param total the total or -1 / 전체 또는 -1
                 */
                public void setProgress(long done, long total) {
                    return;
                }

                /**
                 * Records the state.
                 * <p>
                 * 상태를 기록한다.
                 *
                 * @param state   the state / 상태
                 * @param message the message / 메시지
                 */
                public void setState(DownloadState state, String message) {
                    states.add(state);
                }

                /**
                 * Keeps the proposed target.
                 * <p>
                 * 제안한 대상을 그대로 쓴다.
                 *
                 * @param current the proposal / 제안
                 * @return null to keep it / 그대로 쓰려면 null
                 */
                public File chooseTarget(File current) {
                    return null;
                }

                /**
                 * Accepts the overwriting.
                 * <p>
                 * 덮어쓰기를 받아들인다.
                 *
                 * @param existing the existing file / 기존 파일
                 * @return true / true
                 */
                public boolean confirmOverwrite(File existing) {
                    return true;
                }

                /**
                 * Does nothing.
                 * <p>
                 * 아무것도 하지 않는다.
                 */
                public void dispose() {
                    return;
                }
            };
        }

        /**
         * Saves into the directory of the case.
         * <p>
         * 사례의 디렉터리에 저장한다.
         *
         * @return the directory / 디렉터리
         */
        protected File getDownloadDirectory() {
            return dir;
        }
    }

    /**
     * The components of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer http;

    /**
     * The download directory of the case.
     * <p>
     * 사례의 다운로드 디렉터리.
     */
    private File dir;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ProtocolMatrixIntegrationTest() {
    }

    /**
     * Creates the server with the pages and the download directory.
     * <p>
     * 페이지가 있는 서버와 다운로드 디렉터리를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        dir = File.createTempFile("swp-matrix-", ".d");
        dir.delete();
        dir.mkdirs();
        http = new TestHttpServer();
        http.routeFixed("/t.txt", TestHttpServer.Response.ok("text/plain; charset=UTF-8", "<b>plain</b>\r\n한글 text"));
        http.routeFixed("/m.md", TestHttpServer.Response.ok("text/markdown; charset=UTF-8", "# Title\n\n* one\n* two\n"));
        http.routeFixed("/x.xhtml", TestHttpServer.Response.ok("application/xhtml+xml; charset=UTF-8",
                "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>X-title</title></head><body><p>xhtml body</p></body></html>"));
        http.routeFixed("/h.html", TestHttpServer.Response.ok("TEXT/HTML ; charset=UTF-8", "<title>H-title</title><p>html body</p>"));
        http.routeFixed("/gone", new TestHttpServer.Response(404).text("text/html; charset=UTF-8", "<p>custom not found</p>"));
        http.routeFixed("/boom", new TestHttpServer.Response(500).text("text/html; charset=UTF-8", "<p>custom server error</p>"));
        http.routeFixed("/gl", TestHttpServer.Response.html("<!doctype html><canvas id=c></canvas><script>var c = document.getElementById('c');"
                + "var r = [];"
                + "['webgl', 'webgl2', 'experimental-webgl', 'webgpu'].forEach(function (n) { r.push(n + ':' + (c.getContext(n) === null)); });"
                + "r.push('gpu:' + (typeof navigator.gpu)); r.push('WebGLRenderingContext:' + (typeof WebGLRenderingContext));"
                + "r.push('2d:' + (c.getContext('2d') !== null));"
                + "sessionStorage.setItem('gl', r.join(','));</" + "script>"));
        http.routeFixed("/attach", TestHttpServer.Response.ok("text/plain", "attached data")
                .header("Content-Disposition", "attachment; filename*=UTF-8''%ED%95%9C%EA%B8%80.txt"));
        http.routeFixed("/data.zip", new TestHttpServer.Response(200).bytes("application/zip", new byte[] { 'P', 'K', 3, 4, 1, 2, 3, 4 }));
        http.routeFixed("/dl", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>"
                + "<a href=\"/t.txt\" download=\"saved.txt\" style=\"display:block;height:40px;width:200px\">save</a>"));
        http.start();
    }

    /**
     * Disposes the components, stops the server and removes the downloads.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈추며 다운로드를 지운다.
     */
    public void tearDown() {
        // 사례가 만든 컴포넌트를 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        http.stop();
        File[] files = dir.listFiles();
        // 다운로드 디렉터리의 파일을 지운다.
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        dir.delete();
    }

    /**
     * Creates the component of the case.
     * <p>
     * 사례의 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     */
    private HostPanel panel() {
        HostPanel p = new HostPanel(dir);
        panels.add(p);
        return p;
    }

    /**
     * Waits until the component confirmed an address and finished loading.
     * <p>
     * 컴포넌트가 주소를 확정하고 로딩을 끝낼 때까지 기다린다.
     *
     * @param p   the component / 컴포넌트
     * @param url the address / 주소
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel p, String url) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "confirmed page");
        Check.equal(100, p.getProgress(), "finished loading " + url);
    }

    /**
     * Returns the value of a session storage entry, or null.
     * <p>
     * 세션 저장소 항목의 값을 반환한다. 없으면 null.
     *
     * @param p   the component / 컴포넌트
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String session(SwingWebPanel p, String key) {
        Collection<StorageEntry> entries = p.getSessionStorage();
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : entries) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * The four content types over http: the source is the received text, the title proves that markup was parsed, and a text file is not
     * taken for markup.
     * <p>
     * http 의 네 콘텐츠 타입: 원문은 수신한 문자열이고, 제목은 마크업을 해석했음을 보여 주며, 텍스트 파일은 마크업으로 취급하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFourContentTypesOverHttp() throws Exception {
        HostPanel p = panel();
        p.setPage(http.url("/t.txt"));
        await(p, http.url("/t.txt"));
        Check.equal("<b>plain</b>\r\n한글 text", p.getContent(), "plain text is kept verbatim");
        Check.isNull(session(p, "x"), "nothing runs");
        p.setPage(http.url("/m.md"));
        await(p, http.url("/m.md"));
        Check.equal("# Title\n\n* one\n* two\n", p.getContent(), "markdown is kept as written");
        p.setPage(http.url("/x.xhtml"));
        await(p, http.url("/x.xhtml"));
        Check.equal("X-title", p.getTitle(), "xhtml is parsed");
        Check.contains(p.getContent(), "xhtml body", "xhtml source");
        p.setPage(http.url("/h.html"));
        await(p, http.url("/h.html"));
        Check.equal("H-title", p.getTitle(), "html is parsed (the type is read case-insensitively with its parameters)");
        Check.equal("<title>H-title</title><p>html body</p>", p.getContent(), "html source");
        Check.equal(5, p.getHistories().size(), "every page is an entry: " + p.getHistories());
    }

    /**
     * A 404 and a 500 of a server keep the body of the server in the source and report the error, a refused connection and a blocked scheme
     * become error pages without an exception, and the special addresses behave.
     * <p>
     * 서버의 404 와 500 은 서버의 본문을 원문에 유지하고 오류를 알리며, 거부된 연결과 차단된 스킴은 예외 없이 오류 화면이 되고, 특수 주소는 규칙대로
     * 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorPagesAndSpecialAddresses() throws Exception {
        HostPanel p = panel();
        p.setPage(http.url("/gone"));
        await(p, http.url("/gone"));
        Check.equal("Error 404", p.getStatusText(), "status of a 404");
        Check.equal("<p>custom not found</p>", p.getContent(), "the body of the server is the source");
        p.setPage(http.url("/boom"));
        await(p, http.url("/boom"));
        Check.equal("Error 500", p.getStatusText(), "status of a 500");
        Check.equal("<p>custom server error</p>", p.getContent(), "the body of the 500");
        int port = http.getPort();
        TestHttpServer other = new TestHttpServer();
        other.start();
        String closed = "http://127.0.0.1:" + other.getPort() + "/x";
        other.stop();
        p.setPage(closed);
        await(p, closed);
        Check.isTrue(p.getStatusText().startsWith("Error"), "a refused connection is an error page: " + p.getStatusText());
        Check.contains(p.getContent(), "127.0.0.1", "the page names the address");
        p.setPage("mailto:someone@example.com");
        await(p, "mailto:someone@example.com");
        Check.isTrue(p.getStatusText().startsWith("Error"), "a blocked scheme is an error page: " + p.getStatusText());
        p.setPage("about:blank");
        await(p, "about:blank");
        Check.equal("", p.getContent(), "about:blank is empty");
        p.setContent("<p>text</p>", "text/html");
        await(p, "about:text");
        Check.equal("about:text", p.getPage(), "setContent is about:text");
        Check.isTrue(port > 0, "sanity");
    }

    /**
     * The class path: a page of the test class path loads, a missing resource is a 404 page.
     * <p>
     * 클래스패스: 시험 클래스패스의 페이지가 로드되고 없는 리소스는 404 화면이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClasspathPages() throws Exception {
        HostPanel p = panel();
        String ok = "classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html";
        p.setPage(ok);
        await(p, ok);
        Check.isTrue(p.getContent().length() > 0, "the class path page has a source");
        String missing = "classpath://org/duckdns/hjow/swingwebpanel/testdata/not-there.html";
        p.setPage(missing);
        await(p, missing);
        Check.equal("Error 404", p.getStatusText(), "a missing class path resource is a 404 page");
    }

    /**
     * Anonymous ftp: the listing has the directories as links and the parent link, a failure is a 404 page.
     * <p>
     * 익명 ftp: 목록은 디렉터리를 링크로 가지고 상위 링크가 있으며, 실패는 404 화면이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFtpListingAndFailure() throws Exception {
        TestFtpServer ftp = new TestFtpServer();
        ftp.addDir("/pub").addDir("/pub/sub").addFile("/pub/readme.txt", "hello".getBytes("UTF-8"));
        ftp.start();
        try {
            HostPanel p = panel();
            String root = ftp.url("/pub/");
            p.setPage(root);
            await(p, root);
            Check.contains(p.getContent(), "sub/", "the directory is listed: " + p.getContent());
            Check.contains(p.getContent(), "readme.txt", "the file is listed");
            Check.contains(p.getContent(), "href=", "the entries are links");
            String sub = ftp.url("/pub/sub/");
            p.setPage(sub);
            await(p, sub);
            Check.contains(p.getContent(), "../", "a sub directory has the link to the parent");
            String bad = ftp.url("/nothing/here/");
            p.setPage(bad);
            await(p, bad);
            Check.equal("Error 404", p.getStatusText(), "a failed ftp access is a 404 page");
        } finally {
            ftp.stop();
        }
    }

    /**
     * https with the overridden <code>configureSsl</code> works and the override is called; https to a port that only speaks http is
     * retried once over http and the page shows the real http address.
     * <p>
     * 재정의한 <code>configureSsl</code> 로 https 가 동작하고 재정의가 호출되며, http 만 쓰는 포트로의 https 는 http 로 한 번 재시도하여 페이지가 실제 http
     * 주소를 보여 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsOverrideAndHttpRetry() throws Exception {
        TestHttpServer secure = new TestHttpServer(TestSsl.serverContext());
        secure.routeFixed("/s", TestHttpServer.Response.html("<title>Secure</title><p>over tls</p>"));
        secure.start();
        try {
            HostPanel p = panel();
            p.setPage(secure.url("/s"));
            await(p, secure.url("/s"));
            Check.equal("Secure", p.getTitle(), "the https page was shown");
            Check.isTrue(p.sslCalls >= 1, "the overridden method was called: " + p.sslCalls);
            HostPanel q = panel();
            String tlsToPlain = "https://localhost:" + http.getPort() + "/h.html";
            q.setPage(tlsToPlain);
            await(q, http.url("/h.html"));
            Check.equal("H-title", q.getTitle(), "the retry over http showed the page");
            Check.equal(http.url("/h.html"), q.getPage(), "the real http address is shown");
        } finally {
            secure.stop();
        }
    }

    /**
     * A page that asks for WebGL or WebGPU gets <code>null</code> and sees no such interface, while the 2D context works.
     * <p>
     * WebGL 이나 WebGPU 를 요청하는 페이지는 <code>null</code> 을 받고 그런 인터페이스를 보지 못하며, 2D 컨텍스트는 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWebGlAndWebGpuAreNotThere() throws Exception {
        HostPanel p = panel();
        p.setPage(http.url("/gl"));
        await(p, http.url("/gl"));
        long end = System.currentTimeMillis() + 15000;
        // 스크립트가 결과를 저장할 때까지 기다린다.
        while (session(p, "gl") == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("webgl:true,webgl2:true,experimental-webgl:true,webgpu:true,gpu:undefined,WebGLRenderingContext:undefined,2d:true",
                session(p, "gl"), "3D contexts are null and their interfaces are absent, 2D works");
    }

    /**
     * Files go to the download popup and not into the page: an attachment with a Korean file name, a type that cannot be shown, and a
     * <code>download</code> link; the page stays, the history is not changed and the file is saved with a safe name.
     * <p>
     * 파일은 페이지가 아니라 다운로드 팝업으로 간다: 한글 파일 이름의 첨부, 보여 줄 수 없는 타입, <code>download</code> 링크. 페이지는 그대로이고 이력은
     * 바뀌지 않으며 파일은 안전한 이름으로 저장된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDownloadsGoToThePopup() throws Exception {
        HostPanel p = panel();
        p.setPage(http.url("/dl"));
        await(p, http.url("/dl"));
        p.setPage(http.url("/attach"));
        awaitFile("한글.txt", "attached data");
        Check.equal(http.url("/dl"), p.getPage(), "an attachment does not replace the page");
        p.setPage(http.url("/data.zip"));
        awaitFile("data.zip", null);
        Check.equal(http.url("/dl"), p.getPage(), "a type that cannot be shown does not replace the page");
        Check.equal(2, p.getHistories().size(), "the history did not change: " + p.getHistories());
        Check.equal(2, p.popups.size(), "one popup for each: " + p.popups);
        Check.isTrue(p.states.contains(DownloadState.COMPLETED), "the popups were told that the files are complete: " + p.states);
        long end = System.currentTimeMillis() + 15000;
        // 문서가 배치되어 링크를 클릭할 수 있을 때까지 기다린다.
        while (p.getPreferredSize().height < 40 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        final HostPanel clicking = p;
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches a click on the download link.
             * <p>
             * 다운로드 링크에 클릭을 전달한다.
             */
            public void run() {
                clicking.setSize(300, 200);
                clicking.dispatchEvent(new java.awt.event.MouseEvent(clicking, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 20, 20, 1,
                        false, java.awt.event.MouseEvent.BUTTON1));
            }
        });
        awaitFile("saved.txt", "<b>plain</b>\r\n한글 text");
        Check.equal(http.url("/dl"), p.getPage(), "a download link does not replace the page");
    }

    /**
     * Waits until a file exists in the download directory and, if a text is given, has that content.
     * <p>
     * 다운로드 디렉터리에 파일이 생기고, 문자열이 주어지면 그 내용이 될 때까지 기다린다.
     *
     * @param name the file name / 파일 이름
     * @param text the content as UTF-8 text or null / UTF-8 문자열 내용 또는 null
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitFile(String name, String text) throws Exception {
        File f = new File(dir, name);
        long end = System.currentTimeMillis() + 30000;
        // 파일이 완성될 때까지 짧게 쉬며 기다린다.
        while (!(f.isFile() && (text == null || new String(read(f), "UTF-8").equals(text))) && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.isTrue(f.isFile(), "the file was saved: " + name + " in " + java.util.Arrays.asList(dir.list()));
        if (text != null) {
            Check.equal(text, new String(read(f), "UTF-8"), "content of " + name);
        }
    }

    /**
     * Reads a file.
     * <p>
     * 파일을 읽는다.
     *
     * @param f the file / 파일
     * @return the bytes / 바이트
     * @throws IOException on failure / 실패 시
     */
    private static byte[] read(File f) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            // 파일을 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
