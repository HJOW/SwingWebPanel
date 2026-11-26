package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * How the text of the address bar becomes an address (plan item P31-03): schemes are kept, hosts get <code>https://</code>, file paths
 * become <code>file:</code> addresses and searches use the template of the settings.
 * <p>
 * 주소 표시줄의 글이 주소가 되는 방법(계획 P31-03): 스킴은 유지하고, 호스트에는 <code>https://</code> 를 붙이며, 파일 경로는 <code>file:</code>
 * 주소가 되고, 검색은 설정의 서식을 쓴다.
 */
public class AddressInputTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AddressInputTest() {
    }

    /**
     * Resolves an input without a search template and returns "KIND url".
     * <p>
     * 검색 서식 없이 입력을 해석하여 "종류 주소" 를 반환한다.
     *
     * @param input the input / 입력
     * @return the kind and the address / 종류와 주소
     */
    private static String plain(String input) {
        AddressInput.Result r = AddressInput.resolve(input, "");
        return r.getKind() + " " + r.getUrl();
    }

    /**
     * Empty input gives nothing; input with a scheme is kept as typed, including schemes that the component blocks.
     * <p>
     * 빈 입력은 아무것도 주지 않는다. 스킴이 있는 입력은 컴포넌트가 차단하는 스킴까지 포함해 입력한 그대로 유지한다.
     */
    public void testEmptyAndSchemes() {
        Check.equal("EMPTY null", plain(null), "null");
        Check.equal("EMPTY null", plain("   "), "blank");
        Check.equal("ADDRESS http://example.com/a?b=1#c", plain("  http://example.com/a?b=1#c "), "http, trimmed");
        Check.equal("ADDRESS https://example.com", plain("https://example.com"), "https");
        Check.equal("ADDRESS ftp://ftp.example.com/pub/", plain("ftp://ftp.example.com/pub/"), "ftp");
        Check.equal("ADDRESS classpath://www/index.html", plain("classpath://www/index.html"), "classpath");
        Check.equal("ADDRESS about:blank", plain("about:blank"), "about");
        Check.equal("ADDRESS file:///C:/a/b.html", plain("file:///C:/a/b.html"), "file");
        Check.equal("ADDRESS blob:https://example.com/1-2", plain("blob:https://example.com/1-2"), "blob");
        Check.equal("ADDRESS mailto:someone@example.com", plain("mailto:someone@example.com"), "a blocked scheme is left to the component");
        Check.equal("ADDRESS javascript:alert(1)", plain("javascript:alert(1)"), "another blocked scheme");
        Check.equal("ADDRESS HTTP://EXAMPLE.COM/", plain("HTTP://EXAMPLE.COM/"), "the case of the scheme is kept");
    }

    /**
     * A host or an address without scheme gets <code>https://</code>; a port number is not a scheme.
     * <p>
     * 스킴 없는 호스트나 주소에는 <code>https://</code> 를 붙인다. 포트 번호는 스킴이 아니다.
     */
    public void testHostsGetHttps() {
        Check.equal("HOST https://example.com", plain("example.com"), "a host");
        Check.equal("HOST https://example.com/a/b?q=1", plain("example.com/a/b?q=1"), "a host with a path");
        Check.equal("HOST https://localhost", plain("localhost"), "localhost is a host although it has no dot");
        Check.equal("HOST https://localhost:8080", plain("localhost:8080"), "host and port");
        Check.equal("HOST https://example.com:8443/x", plain("example.com:8443/x"), "host, port and path");
        Check.equal("HOST https://192.168.0.1", plain("192.168.0.1"), "an IPv4 address");
        Check.equal("HOST https://intranet/page", plain("intranet/page"), "a word with a path is an address");
        Check.equal("HOST https://intranet", plain("intranet"), "one word is a host when there is no search template");
        Check.equal(0, AddressInput.schemeLength("localhost:80"), "a port is not a scheme");
        Check.equal(4, AddressInput.schemeLength("http://x"), "a scheme");
        Check.equal(0, AddressInput.schemeLength("1abc:x"), "a scheme starts with a letter");
        Check.equal(6, AddressInput.schemeLength("mailto:80a"), "digits followed by other text are not a port");
    }

    /**
     * A search goes to the template of the settings with the query encoded; without a template text with white space cannot be opened.
     * <p>
     * 검색은 질의를 인코딩하여 설정의 서식으로 간다. 서식이 없으면 공백 있는 글은 열 수 없다.
     */
    public void testSearch() {
        AddressInput.Result none = AddressInput.resolve("swing web panel", null);
        Check.equal(AddressInput.Kind.NO_SEARCH, none.getKind(), "no template");
        Check.isNull(none.getUrl(), "no address without a template");
        AddressInput.Result r = AddressInput.resolve("swing web panel", "https://search.example/?q=%s&x=1");
        Check.equal(AddressInput.Kind.SEARCH, r.getKind(), "search");
        Check.equal("https://search.example/?q=swing+web+panel&x=1", r.getUrl(), "the query replaces the placeholder");
        Check.equal("https://search.example/?q=%ED%95%9C%EA%B8%80+a%26b", AddressInput.resolve("한글 a&b", "https://search.example/?q=%s").getUrl(),
                "the query is encoded as UTF-8");
        Check.equal("https://search.example/find/word", AddressInput.resolve("word", "https://search.example/find/").getUrl(),
                "a template without placeholder gets the query at its end");
        Check.equal(AddressInput.Kind.SEARCH, AddressInput.resolve("word", "https://s.example/?q=%s").getKind(), "one word is a search with a template");
        Check.equal(AddressInput.Kind.HOST, AddressInput.resolve("example.com", "https://s.example/?q=%s").getKind(), "a host is not a search");
        Check.equal("https://s.example/?a=x+y&b=x+y", AddressInput.resolve("x y", "https://s.example/?a=%s&b=%s").getUrl(), "every placeholder is replaced");
    }

    /**
     * A local path becomes a <code>file:</code> address: a drive path and a UNC path always, a path that starts with a slash only when it
     * exists.
     * <p>
     * 로컬 경로는 <code>file:</code> 주소가 된다: 드라이브 경로와 UNC 경로는 항상, 슬래시로 시작하는 경로는 실제로 있을 때만.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFilePaths() throws Exception {
        AddressInput.Result drive = AddressInput.resolve("C:\\dir\\a b.html", "https://s.example/?q=%s");
        Check.equal(AddressInput.Kind.FILE, drive.getKind(), "a drive path is a file even with white space and a search template");
        Check.equal("file:///C:/dir/a%20b.html", drive.getUrl(), "the address of the file, with the white space encoded");
        Check.isTrue(drive.getUrl().startsWith("file:"), "a file address: " + drive.getUrl());
        Check.equal(AddressInput.Kind.FILE, AddressInput.resolve("D:/x/y.html", "").getKind(), "forward slashes");
        long before = System.currentTimeMillis();
        AddressInput.Result unc = AddressInput.resolve("\\\\server\\share\\x.html", "");
        Check.equal(AddressInput.Kind.FILE, unc.getKind(), "a UNC path");
        Check.equal("file:////server/share/x.html", unc.getUrl(), "the address of a UNC path");
        Check.isTrue(System.currentTimeMillis() - before < 1000, "resolving a UNC path does not wait for the network");
        File temp = File.createTempFile("swp-address", ".html");
        try {
            String path = temp.getAbsolutePath();
            Check.equal(AddressInput.Kind.FILE, AddressInput.resolve(path, "").getKind(), "an existing absolute path: " + path);
            String uri = temp.toURI().toString();
            Check.equal("file:///" + uri.substring("file:/".length()), AddressInput.resolve(path, "").getUrl(), "its address");
        } finally {
            Check.isTrue(temp.delete(), "the temporary file is removed");
        }
        Check.equal("HOST https:///no/such/path/swp", plain("/no/such/path/swp"), "a path that does not exist is not a file");
    }
}
