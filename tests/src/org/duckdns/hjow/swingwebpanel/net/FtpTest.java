package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestFtpServer;

/**
 * Tests of the FTP support (plan items P05-01 to P05-05 and P05-07): the listing parsers, the generated HTML page and the
 * whole fetch with the local {@link TestFtpServer}: anonymous login, EPSV/PASV, MLSD with LIST fallback, UNIX and DOS
 * formats, multi-line replies, Hangul and space names, directory navigation, files, failures, command injection
 * protection, truncation and cancellation.
 * <p>
 * FTP 지원 시험(계획 P05-01~P05-05, P05-07): 목록 해석기, 생성한 HTML 페이지, 로컬 {@link TestFtpServer} 를 쓴 전체 가져오기.
 * 익명 로그인, EPSV/PASV, LIST 로 대체되는 MLSD, UNIX/DOS 형식, 여러 줄 응답, 한글과 공백 이름, 디렉터리 이동, 파일, 실패,
 * 명령 주입 방어, 잘림, 취소를 확인한다.
 */
public class FtpTest {

    /**
     * Server of the current case.
     * <p>
     * 현재 사례의 서버.
     */
    private TestFtpServer server;
    /**
     * Fetcher with short timeouts.
     * <p>
     * 짧은 제한 시간을 쓰는 가져오기 객체.
     */
    private final FtpFetcher fetcher = new FtpFetcher(FetchLimits.DEFAULT.withTimeouts(5000, 5000));

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FtpTest() {
    }

    /**
     * Creates the server.
     * <p>
     * 서버를 만든다.
     */
    public void setUp() {
        server = new TestFtpServer();
    }

    /**
     * Stops the server.
     * <p>
     * 서버를 멈춘다.
     */
    public void tearDown() {
        server.stop();
    }

    /**
     * Fills a typical tree and starts the server.
     * <p>
     * 전형적인 트리를 채우고 서버를 시작한다.
     *
     * @throws IOException on failure / 실패 시
     */
    private void startWithTree() throws IOException {
        server.addDir("/pub");
        server.addDir("/pub/sub dir/한글 폴더");
        server.addFile("/pub/readme.txt", "hello ftp".getBytes("UTF-8"));
        server.addFile("/pub/한글 파일 (1).txt", "한글 내용".getBytes("UTF-8"));
        server.addFile("/pub/data.bin", new byte[] { 0, 1, 2, (byte) 255 });
        server.addFile("/top.html", "<p>top</p>".getBytes("UTF-8"));
        server.addSymlink("/pub/latest", "readme.txt");
        server.start();
    }

    /**
     * Returns the HTML body of a response.
     * <p>
     * 응답의 HTML 본문을 돌려준다.
     *
     * @param r response / 응답
     * @return text / 문자열
     * @throws IOException never in practice / 실제로는 발생하지 않음
     */
    private static String html(ResourceResponse r) throws IOException {
        return new String(r.getBody(), "UTF-8");
    }

    /**
     * Parses UNIX style lines: directories, files, links, names with spaces and Hangul, missing group, ISO dates.
     * <p>
     * UNIX 형식 줄을 해석한다. 디렉터리, 파일, 링크, 공백/한글이 든 이름, 그룹 없음, ISO 날짜.
     */
    public void testParseUnixList() {
        List<FtpEntry> out = new ArrayList<FtpEntry>();
        List<String> diag = new ArrayList<String>();
        String text = "total 12\r\n"
                + "drwxr-xr-x   2 ftp      ftp          4096 Jan  1  2020 sub dir\r\n"
                + "-rw-r--r--   1 ftp      ftp          1234 Mar 15 10:30 file with  two spaces.txt\r\n"
                + "-rw-r--r--   1 ftp      ftp            10 Jan  1  2020 한글 파일.txt\r\n"
                + "lrwxrwxrwx   1 ftp      ftp             6 Jan  1  2020 latest -> target/file\r\n"
                + "-rw-r--r--   1 owner       99 Jan  1  2020 nogroup.txt\r\n"
                + "-rw-r--r--   1 ftp ftp 5 2020-01-02 03:04 iso.txt\r\n"
                + "drwxr-xr-x   2 ftp      ftp          4096 Jan  1  2020 .\r\n"
                + "drwxr-xr-x   2 ftp      ftp          4096 Jan  1  2020 ..\r\n"
                + "crw-rw-rw-   1 root     root       1,   3 Jan  1  2020 null\r\n";
        FtpListParser.parse(text, false, out, diag);
        Check.equal("DIRECTORY:sub dir", out.get(0).toString(), "directory with a space");
        Check.equal("FILE:file with  two spaces.txt(1234)", out.get(1).toString(), "two spaces are preserved");
        Check.equal("FILE:한글 파일.txt(10)", out.get(2).toString(), "hangul");
        Check.equal("SYMLINK:latest->target/file(6)", out.get(3).toString(), "symlink with target");
        Check.equal("FILE:nogroup.txt(99)", out.get(4).toString(), "no group column");
        Check.equal("FILE:iso.txt(5)", out.get(5).toString(), "iso date");
        Check.equal(7, out.size(), "dot entries and total line skipped");
        Check.equal(FtpEntry.Type.UNKNOWN, out.get(6).getType(), "a device is neither file nor directory");
        Check.equal("null", out.get(6).getName(), "the device keeps its own name and is not treated as a file or directory");
    }

    /**
     * Parses DOS/IIS style lines.
     * <p>
     * DOS/IIS 형식 줄을 해석한다.
     */
    public void testParseDosList() {
        List<FtpEntry> out = new ArrayList<FtpEntry>();
        List<String> diag = new ArrayList<String>();
        FtpListParser.parse("01-01-20  10:15AM       <DIR>          my dir\r\n"
                + "12-31-2019  09:05PM                 1234 report final.doc\r\n", false, out, diag);
        Check.equal("[DIRECTORY:my dir, FILE:report final.doc(1234)]", out.toString(), "dos entries");
        Check.isTrue(diag.isEmpty(), "no diagnostics");
    }

    /**
     * Parses MLSD lines: facts, types, links; current and parent directory entries are skipped.
     * <p>
     * MLSD 줄을 해석한다. 사실, 종류, 링크. 현재/상위 디렉터리 항목은 건너뛴다.
     */
    public void testParseMlsd() {
        List<FtpEntry> out = new ArrayList<FtpEntry>();
        List<String> diag = new ArrayList<String>();
        FtpListParser.parse("type=cdir;modify=20200101101500; .\r\ntype=pdir; ..\r\n"
                + "type=dir;modify=20200101101500; 한글 폴더\r\n"
                + "Type=File;Size=42;modify=20200101101500; a b.txt\r\n"
                + "type=OS.unix=slink:/x/y;size=3; ln\r\n"
                + "type=weird;size=1; thing\r\n", true, out, diag);
        Check.equal("[DIRECTORY:한글 폴더, FILE:a b.txt(42), SYMLINK:ln->/x/y(3), UNKNOWN:thing(1)]", out.toString(), "mlsd entries");
    }

    /**
     * A line that matches no format is kept raw as UNKNOWN with a diagnostic and never shown as a directory.
     * <p>
     * 어떤 형식에도 맞지 않는 줄은 진단과 함께 UNKNOWN 으로 원문 보관하며 디렉터리로 보이지 않는다.
     */
    public void testUnparsableLineIsNeverADirectory() {
        List<FtpEntry> out = new ArrayList<FtpEntry>();
        List<String> diag = new ArrayList<String>();
        FtpListParser.parse("some weird <DIR> looking line\r\n", false, out, diag);
        Check.equal(1, out.size(), "kept");
        Check.equal(FtpEntry.Type.UNKNOWN, out.get(0).getType(), "unknown type");
        Check.isNull(out.get(0).getName(), "no name was guessed");
        Check.equal("some weird <DIR> looking line", out.get(0).getRaw(), "raw text kept");
        Check.equal(1, diag.size(), "diagnostic recorded");
    }

    /**
     * The HTML page escapes names, encodes links, orders entries and has ../ only below the root.
     * <p>
     * HTML 페이지는 이름을 이스케이프하고 링크를 인코딩하고 항목을 정렬하며 루트 아래에서만 ../ 를 가진다.
     */
    public void testListingHtml() {
        List<FtpEntry> e = new ArrayList<FtpEntry>();
        e.add(new FtpEntry("z.txt", FtpEntry.Type.FILE, 5, null, null, ""));
        e.add(new FtpEntry("<b>bold</b>&\"'.txt", FtpEntry.Type.FILE, -1, null, null, ""));
        e.add(new FtpEntry("a:b", FtpEntry.Type.DIRECTORY, -1, null, null, ""));
        e.add(new FtpEntry("한글 폴더", FtpEntry.Type.DIRECTORY, -1, null, null, ""));
        e.add(new FtpEntry("ln", FtpEntry.Type.SYMLINK, 3, null, "../x", ""));
        e.add(new FtpEntry(null, FtpEntry.Type.UNKNOWN, -1, null, null, "garbage <script>alert(1)</script>"));
        String root = FtpListing.toHtml("/", e);
        Check.isFalse(root.contains("href=\"../\""), "no parent link at the root");
        String sub = FtpListing.toHtml("/pub/", e);
        Check.contains(sub, "<li class=\"up\"><a href=\"../\">../</a></li>", "parent link");
        Check.isTrue(sub.indexOf("class=\"up\"") < sub.indexOf("class=\"dir\""), "the parent link comes first");
        Check.isFalse(sub.contains("<b>"), "names cannot become tags");
        Check.isFalse(sub.contains("<script>"), "raw lines cannot become tags");
        Check.contains(sub, "&lt;b&gt;bold&lt;/b&gt;&amp;&quot;&#39;.txt", "escaped name");
        Check.contains(sub, "href=\"./a%3Ab/\"", "colon is encoded so it cannot look like a scheme");
        Check.contains(sub, "href=\"./%ED%95%9C%EA%B8%80%20%ED%8F%B4%EB%8D%94/\"", "hangul and space are percent-encoded");
        Check.isTrue(sub.indexOf("a:b/</a>") < sub.indexOf("z.txt"), "directories before files");
        Check.contains(sub, "-&gt; ../x", "link target shown as text");
        Check.isFalse(sub.contains("href=\"./garbage"), "unparsable lines have no link");
        Check.contains(sub, "<title>Index of /pub/</title>", "title");
    }

    /**
     * The root listing: anonymous login, passive mode and MLSD; the page lists directories and files.
     * <p>
     * 루트 목록: 익명 로그인, 수동 모드, MLSD. 페이지에 디렉터리와 파일이 나온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRootListing() throws Exception {
        startWithTree();
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("")), null);
        Check.equal(FailureKind.NONE, r.getFailureKind(), r.getFailureMessage());
        Check.equal(200, r.getStatus(), "status");
        Check.equal("text/html", r.getMimeType(), "mime");
        Check.equal("UTF-8", r.getCharset(), "charset");
        Check.isTrue(r.isDirectoryListing(), "flag");
        Check.equal(server.url("/"), r.getFinalUrl(), "final url ends with a slash");
        String h = html(r);
        Check.contains(h, "href=\"./pub/\"", "pub directory link");
        Check.contains(h, "href=\"./top.html\"", "file link");
        Check.isFalse(h.contains("href=\"../\""), "no parent link at the root");
        List<String> cmds = server.commands();
        Check.equal("USER anonymous", cmds.get(0), "anonymous user");
        Check.equal("PASS anonymous@", cmds.get(1), "anonymous password");
        Check.isTrue(cmds.contains("EPSV"), "EPSV is tried first");
        Check.isTrue(cmds.contains("MLSD"), "MLSD is preferred");
        Check.isFalse(cmds.contains("LIST"), "LIST is not needed");
        Check.equal("QUIT", cmds.get(cmds.size() - 1), "polite end");
    }

    /**
     * Sub directories with Hangul and spaces (percent-encoded in the URL), up links and navigation by relative links.
     * <p>
     * 한글과 공백이 있는 하위 디렉터리(URL 에서는 퍼센트 인코딩), 상위 링크, 상대 링크를 통한 이동.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDirectoryNavigation() throws Exception {
        startWithTree();
        ResourceResponse pub = fetcher.fetch(new ResourceRequest(server.url("/pub")), null);
        Check.equal(server.url("/pub/"), pub.getFinalUrl(), "a directory url without a slash is reported with one");
        String h = html(pub);
        Check.contains(h, "href=\"../\"", "up link");
        Check.contains(h, "href=\"./sub%20dir/\"", "directory with a space");
        Check.contains(h, "href=\"./%ED%95%9C%EA%B8%80%20%ED%8C%8C%EC%9D%BC%20%281%29.txt\"", "hangul file name");
        Check.contains(h, "latest</a> -&gt; readme.txt", "symlink shown");
        String subUrl = UrlUtil.resolve(pub.getFinalUrl(), "./sub%20dir/");
        ResourceResponse sub = fetcher.fetch(new ResourceRequest(subUrl), null);
        Check.equal(200, sub.getStatus(), "subdirectory");
        Check.isTrue(server.commands().contains("CWD /pub/sub dir/"), "the decoded path was sent: " + server.commands());
        Check.contains(html(sub), "href=\"./%ED%95%9C%EA%B8%80%20%ED%8F%B4%EB%8D%94/\"", "nested hangul directory");
        ResourceResponse deep = fetcher.fetch(new ResourceRequest(UrlUtil.resolve(sub.getFinalUrl(), "./%ED%95%9C%EA%B8%80%20%ED%8F%B4%EB%8D%94/")), null);
        Check.equal(200, deep.getStatus(), "deep hangul directory");
        ResourceResponse up = fetcher.fetch(new ResourceRequest(UrlUtil.resolve(deep.getFinalUrl(), "../")), null);
        Check.equal(sub.getFinalUrl(), up.getFinalUrl(), "../ leads to the parent directory");
        ResourceResponse root = fetcher.fetch(new ResourceRequest(UrlUtil.resolve(pub.getFinalUrl(), "../")), null);
        Check.equal(server.url("/"), root.getFinalUrl(), "up from /pub is the root");
        ResourceResponse above = fetcher.fetch(new ResourceRequest(UrlUtil.resolve(root.getFinalUrl(), "../../")), null);
        Check.equal(server.url("/"), above.getFinalUrl(), "nothing is above the root");
    }

    /**
     * Without MLSD the client falls back to LIST (UNIX and DOS formats); EPSV refusal falls back to PASV; a multi-line
     * greeting and a wrong PASV address are handled.
     * <p>
     * MLSD 가 없으면 LIST(UNIX, DOS 형식)로 대체하고, EPSV 거부는 PASV 로 대체한다. 여러 줄 인사말과 잘못된 PASV 주소도 처리한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFallbacksAndProtocolVariants() throws Exception {
        server.setMlsdEnabled(false).setEpsvEnabled(false).setMultilineGreeting(true).setAdvertisedPasvAddress("10.9.9.9");
        startWithTree();
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/pub/")), null);
        Check.equal(200, r.getStatus(), r.getFailureMessage());
        Check.contains(html(r), "readme.txt", "unix LIST parsed");
        List<String> cmds = server.commands();
        Check.isTrue(cmds.contains("MLSD") && cmds.contains("LIST"), "MLSD refused, LIST used: " + cmds);
        Check.isTrue(cmds.contains("EPSV") && cmds.contains("PASV"), "EPSV refused, PASV used: " + cmds);
        server.stop();
        server = new TestFtpServer();
        server.setMlsdEnabled(false).setListStyle(TestFtpServer.ListStyle.DOS);
        startWithTree();
        ResourceResponse dos = fetcher.fetch(new ResourceRequest(server.url("/pub/")), null);
        String h = html(dos);
        Check.contains(h, "href=\"./sub%20dir/\"", "dos directory");
        Check.contains(h, "href=\"./readme.txt\"", "dos file");
    }

    /**
     * A PASV reply with an impossible number (above 255) is refused as an FTP failure instead of connecting anywhere.
     * <p>
     * 불가능한 숫자(255 초과)가 든 PASV 응답은 어디에도 연결하지 않고 FTP 실패로 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidPasvReplyIsRejected() throws Exception {
        server.setEpsvEnabled(false).setAdvertisedPasvAddress("300.1.1.1");
        startWithTree();
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/pub/")), null);
        Check.equal(404, r.getStatus(), "refused");
        Check.equal(FailureKind.FTP_FAILURE, r.getFailureKind(), "kind");
        Check.contains(r.getFailureMessage(), "PASV", "message names the problem");
        Check.equal(0, server.getDataConnections(), "no data connection was opened");
    }

    /**
     * Unparsable raw lines stay plain text in the page.
     * <p>
     * 해석할 수 없는 원문 줄은 페이지에서 평문으로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnparsableLinesInThePage() throws Exception {
        server.setMlsdEnabled(false).addRawListLine("/pub", "WEIRD-FORMAT <DIR> mystery");
        startWithTree();
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/pub/")), null);
        String h = html(r);
        Check.contains(h, "<li class=\"unparsed\">WEIRD-FORMAT &lt;DIR&gt; mystery</li>", "kept as plain escaped text");
        Check.isFalse(h.contains("mystery</a>"), "no link for an unparsable line");
    }

    /**
     * Files: media type by name, body in memory, streaming to a handler, progress, names with Hangul and spaces.
     * <p>
     * 파일: 이름으로 정한 미디어 타입, 메모리 본문, 처리기로의 스트리밍, 진행률, 한글/공백 이름.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileRetrieval() throws Exception {
        startWithTree();
        ResourceResponse txt = fetcher.fetch(new ResourceRequest(server.url("/pub/readme.txt")), null);
        Check.equal(200, txt.getStatus(), txt.getFailureMessage());
        Check.equal("text/plain", txt.getMimeType(), "mime by extension");
        Check.equal("hello ftp", html(txt), "content");
        Check.isFalse(txt.isDirectoryListing(), "a file is not a listing");
        Check.equal(9, txt.getDeclaredLength(), "SIZE gave the length");
        ResourceResponse hangul = fetcher.fetch(new ResourceRequest(server.url("/pub/%ED%95%9C%EA%B8%80%20%ED%8C%8C%EC%9D%BC%20%281%29.txt")), null);
        Check.equal("한글 내용", html(hangul), "hangul file name");
        Check.isTrue(server.commands().contains("RETR /pub/한글 파일 (1).txt"), "decoded RETR path: " + server.commands());
        ResourceResponse bin = fetcher.fetch(new ResourceRequest(server.url("/pub/data.bin")), null);
        Check.equal("application/octet-stream", bin.getMimeType(), "unknown types are octet-stream (download)");
        Check.equal(4, bin.getBody().length, "binary body");
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        final List<long[]> progress = new ArrayList<long[]>();
        ResourceResponse streamed = fetcher.fetch(new ResourceRequest(server.url("/pub/readme.txt")).setProgressListener(new ProgressListener() {
            /**
             * Records progress.
             * <p>
             * 진행 상황을 기록한다.
             *
             * @param received bytes so far / 지금까지의 바이트
             * @param total    total or -1 / 전체 또는 -1
             */
            public void onProgress(long received, long total) {
                progress.add(new long[] { received, total });
            }
        }), new ResponseHandler() {
            /**
             * Streams the body.
             * <p>
             * 본문을 스트리밍한다.
             *
             * @param head response head / 응답 머리
             * @return the sink / sink
             */
            public OutputStream onResponse(ResourceResponse head) {
                return sink;
            }
        });
        Check.isTrue(streamed.isStreamed(), "streamed");
        Check.equal("hello ftp", new String(sink.toByteArray(), "UTF-8"), "streamed bytes");
        Check.equal(9, progress.get(progress.size() - 1)[0], "progress counts the bytes");
        Check.equal(9, progress.get(0)[1], "progress knows the total from SIZE");
    }

    /**
     * Failures become 404 / FTP_FAILURE and close all connections: login, missing path, failing listing, refused connection.
     * <p>
     * 실패는 404 / FTP_FAILURE 가 되고 모든 연결을 닫는다. 로그인, 없는 경로, 실패하는 목록, 연결 거부.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailuresAreReportedAs404() throws Exception {
        startWithTree();
        ResourceResponse missingDir = fetcher.fetch(new ResourceRequest(server.url("/nothing/")), null);
        Check.equal(404, missingDir.getStatus(), "missing directory");
        Check.equal(FailureKind.FTP_FAILURE, missingDir.getFailureKind(), "kind");
        ResourceResponse missingFile = fetcher.fetch(new ResourceRequest(server.url("/pub/nothing.txt")), null);
        Check.equal(404, missingFile.getStatus(), "missing file");
        Check.equal(FailureKind.FTP_FAILURE, missingFile.getFailureKind(), "kind");
        server.setFailList(true);
        ResourceResponse badList = fetcher.fetch(new ResourceRequest(server.url("/pub/")), null);
        Check.equal(404, badList.getStatus(), "failing listing");
        Check.isTrue(badList.isError(), "error flag");
        server.setFailList(false).setFailLogin(true);
        ResourceResponse badLogin = fetcher.fetch(new ResourceRequest(server.url("/")), null);
        Check.equal(404, badLogin.getStatus(), "refused login");
        Check.contains(badLogin.getFailureMessage(), "login", "message");
        ServerSocket probe = new ServerSocket(0);
        int free = probe.getLocalPort();
        probe.close();
        ResourceResponse refused = fetcher.fetch(new ResourceRequest("ftp://localhost:" + free + "/"), null);
        Check.equal(404, refused.getStatus(), "connection refused is a 404 too");
        Check.equal(FailureKind.FTP_FAILURE, refused.getFailureKind(), "kind");
    }

    /**
     * Control characters in the path never reach the server, and user info is never used for the login.
     * <p>
     * 경로의 제어 문자는 서버에 닿지 않으며 사용자 정보는 로그인에 쓰지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoCommandInjectionAndAnonymousOnly() throws Exception {
        startWithTree();
        String[] evil = { "/pub%0D%0ADELE%20readme.txt", "/pub%0ADELE%20x", "/pub%00x", "/pub/readme.txt%0d%0aQUIT" };
        // 제어 문자가 든 경로는 모두 연결 전에 거부되어야 한다.
        for (int i = 0; i < evil.length; i++) {
            ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url(evil[i])), null);
            Check.equal(FailureKind.INVALID_URL, r.getFailureKind(), "rejected: " + evil[i]);
        }
        Check.equal(0, server.getControlConnections(), "the server was never contacted");
        Check.isTrue(server.commands().isEmpty(), "no command was sent");
        fetcher.fetch(new ResourceRequest("ftp://bob:secret@localhost:" + server.getPort() + "/pub/"), null);
        List<String> cmds = server.commands();
        Check.equal("USER anonymous", cmds.get(0), "user info is ignored");
        // 받은 명령에 사용자 정보가 한 번도 나오지 않았는지 확인한다.
        for (int i = 0; i < cmds.size(); i++) {
            Check.isFalse(cmds.get(i).contains("bob") || cmds.get(i).contains("secret"), "credentials never sent: " + cmds.get(i));
        }
    }

    /**
     * A short transfer is a failure and a file over the memory limit is refused (but can be streamed).
     * <p>
     * 짧게 끝난 전송은 실패이고 메모리 제한을 넘는 파일은 거부된다(스트리밍은 가능).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedAndOversizedFiles() throws Exception {
        startWithTree();
        server.setTruncateRetrAt(3);
        ResourceResponse t = fetcher.fetch(new ResourceRequest(server.url("/pub/readme.txt")), null);
        Check.equal(FailureKind.TRUNCATED, t.getFailureKind(), "short transfer");
        server.setTruncateRetrAt(-1);
        FtpFetcher small = new FtpFetcher(FetchLimits.DEFAULT.withTimeouts(5000, 5000).withMaxMemoryBytes(4));
        ResourceResponse big = small.fetch(new ResourceRequest(server.url("/pub/readme.txt")), null);
        Check.equal(FailureKind.TOO_LARGE, big.getFailureKind(), "over the memory limit");
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ResourceResponse streamed = small.fetch(new ResourceRequest(server.url("/pub/readme.txt")), new ResponseHandler() {
            /**
             * Streams the body.
             * <p>
             * 본문을 스트리밍한다.
             *
             * @param head response head / 응답 머리
             * @return the sink / sink
             */
            public OutputStream onResponse(ResourceResponse head) {
                return sink;
            }
        });
        Check.equal(FailureKind.NONE, streamed.getFailureKind(), "streaming is not limited");
        Check.equal("hello ftp", new String(sink.toByteArray(), "UTF-8"), "streamed content");
    }

    /**
     * Cancelling during a slow transfer ends the fetch promptly and closes the connections.
     * <p>
     * 느린 전송 중에 취소하면 가져오기가 곧바로 끝나고 연결을 닫는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        server.setDataDelayMs(8000);
        startWithTree();
        final CancelToken token = new CancelToken();
        final ResourceResponse[] holder = new ResourceResponse[1];
        final ResourceRequest req = new ResourceRequest(server.url("/pub/")).setCancelToken(token);
        Thread t = new Thread(new Runnable() {
            /**
             * Runs the slow fetch.
             * <p>
             * 느린 가져오기를 실행한다.
             */
            public void run() {
                holder[0] = new FtpFetcher(FetchLimits.DEFAULT.withTimeouts(5000, 20000)).fetch(req, null);
            }
        });
        t.start();
        long wait = System.currentTimeMillis() + 5000;
        // 서버가 목록 명령을 받을 때까지 기다린 뒤 취소한다.
        while (!server.commands().contains("MLSD") && System.currentTimeMillis() < wait) {
            Thread.sleep(20);
        }
        long t0 = System.currentTimeMillis();
        token.cancel();
        t.join(5000);
        Check.isFalse(t.isAlive(), "the fetch ended");
        Check.isTrue(System.currentTimeMillis() - t0 < 4000, "cancel was prompt");
        Check.equal(FailureKind.CANCELLED, holder[0].getFailureKind(), "cancelled");
    }
}
