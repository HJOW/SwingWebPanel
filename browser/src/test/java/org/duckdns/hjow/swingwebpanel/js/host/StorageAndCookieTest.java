package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager;
import org.duckdns.hjow.swingwebpanel.storage.SessionStorage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>document.cookie</code>, <code>localStorage</code>, <code>sessionStorage</code> and the <code>storage</code> event as scripts
 * see them (plan items P15-09, P16-01, P16-08, P16-10): cookie scope and HttpOnly, named properties, quota errors, opaque origins, sharing
 * between pages and notification of the other documents only.
 * <p>
 * 스크립트가 보는 <code>document.cookie</code>, <code>localStorage</code>, <code>sessionStorage</code>, <code>storage</code> 이벤트의 시험(계획
 * P15-09, P16-01, P16-08, P16-10): 쿠키 범위와 HttpOnly, 이름 있는 프로퍼티, 쿼터 오류, 불투명 오리진, 페이지 사이의 공유, 다른 문서에만 하는 통지.
 */
public class StorageAndCookieTest {

    /**
     * The temporary directory of the case.
     * <p>
     * 사례의 임시 디렉터리.
     */
    private File dir;

    /**
     * The shared localStorage manager of the case.
     * <p>
     * 사례의 공용 localStorage 관리자.
     */
    private LocalStorageManager local;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StorageAndCookieTest() {
    }

    /**
     * Creates a private store file and manager for each case.
     * <p>
     * 사례마다 전용 저장소 파일과 관리자를 만든다.
     *
     * @throws Exception if the directory cannot be created / 디렉터리를 만들 수 없으면
     */
    public void setUp() throws Exception {
        dir = File.createTempFile("swp-jsstore-", ".d");
        dir.delete();
        dir.mkdirs();
        local = new LocalStorageManager(new File(dir, "store.dat"));
    }

    /**
     * Removes the files of the case.
     * <p>
     * 사례의 파일을 지운다.
     */
    public void tearDown() {
        File[] children = dir.listFiles();
        if (children != null) {
            // 디렉터리 안의 파일을 하나씩 지운다.
            for (File c : children) {
                c.delete();
            }
        }
        dir.delete();
    }

    /**
     * Creates a fixture that uses the shared localStorage and its own sessionStorage and cookies.
     * <p>
     * 공용 localStorage 와 자신의 sessionStorage, 쿠키를 쓰는 시험 도구를 만든다.
     *
     * @return the fixture (not loaded yet) / 시험 도구(아직 로드하지 않음)
     */
    private PageFixture fixture() {
        PageFixture f = new PageFixture();
        f.env.local = local;
        f.env.session = new SessionStorage();
        f.env.cookies = new CookieStore();
        return f;
    }

    /**
     * P15-09: document.cookie shows the cookies of the document URL without HttpOnly ones and stores cookies set from script.
     * <p>
     * P15-09: document.cookie 는 문서 URL 의 쿠키를 HttpOnly 를 뺀 채 보여 주고 스크립트가 지정한 쿠키를 저장한다.
     */
    public void testCookies() {
        PageFixture f = fixture();
        List<String> headers = new ArrayList<String>(Arrays.asList("h=secret; Path=/; HttpOnly", "v=2; Path=/", "o=3; Path=/other"));
        f.env.cookies.storeFromResponse("http://example.test/dir/page.html", headers);
        f.load("<p></p>");
        String shown = f.scripts.evaluate("document.cookie", "t");
        Check.isTrue(shown.indexOf("v=2") >= 0, "ordinary cookie visible: " + shown);
        Check.isTrue(shown.indexOf("h=") < 0, "HttpOnly cookie hidden: " + shown);
        Check.isTrue(shown.indexOf("o=") < 0, "cookie of another path hidden: " + shown);
        f.scripts.evaluate("document.cookie = 'a=1; path=/'; document.cookie = 'b=2; path=/; max-age=3600';", "t");
        String after = f.scripts.evaluate("document.cookie", "t");
        Check.isTrue(after.indexOf("a=1") >= 0 && after.indexOf("b=2") >= 0 && after.indexOf("v=2") >= 0, "script cookies stored: " + after);
        f.scripts.evaluate("document.cookie = 'a=; path=/; max-age=0';", "t");
        Check.isTrue(f.scripts.evaluate("document.cookie", "t").indexOf("a=") < 0, "a cookie is deleted by max-age=0");
        f.scripts.evaluate("document.cookie = 'h=overwritten; path=/'", "t");
        Check.isTrue(f.env.cookies.getCookies().indexOf("secret") >= 0, "script cannot overwrite an HttpOnly cookie");
        PageFixture g = new PageFixture().load("<p></p>");
        Check.equal("", g.scripts.evaluate("document.cookie", "t"), "no cookie store: empty string, writes ignored");
    }

    /**
     * P16-01: Storage objects: methods, named properties, enumeration, deletion and identity.
     * <p>
     * P16-01: Storage 객체: 메서드, 이름 있는 프로퍼티, 열거, 삭제, 동일성.
     */
    public void testStorageApi() {
        PageFixture f = fixture().load("<p></p>");
        Check.equal("[object Storage],true,0", f.scripts.evaluate("Object.prototype.toString.call(localStorage) + ',' + "
                + "(localStorage === localStorage) + ',' + localStorage.length", "t"), "type, identity and empty length");
        Check.equal("1,v,null", f.scripts.evaluate("localStorage.setItem('k', 'v'); localStorage.length + ',' + localStorage.getItem('k') + ',' + "
                + "localStorage.getItem('missing')", "t"), "setItem and getItem");
        Check.equal("w,w,2,k,n", f.scripts.evaluate("localStorage.n = 'w'; localStorage.k = 'w'; localStorage.n + ',' + localStorage['k'] + ',' + "
                + "localStorage.length + ',' + localStorage.key(0) + ',' + localStorage.key(1)", "t"), "named property access stores strings in order");
        Check.equal("5,5", f.scripts.evaluate("localStorage.num = 5; typeof localStorage.num === 'string' ? '5,5' : 'bad'", "t"),
                "values are converted to strings");
        Check.equal("k,n,num", f.scripts.evaluate("Object.keys(localStorage).join(',')", "t"), "property keys are the item keys");
        Check.equal("true,0", f.scripts.evaluate("delete localStorage.n; (localStorage.getItem('n') === null) + ',' + (localStorage.length - 2)", "t"),
                "delete removes the item");
        Check.equal("function,2", f.scripts.evaluate("var len = localStorage.length; localStorage.length = 99; typeof localStorage.getItem + ',' + "
                + "localStorage.length", "t"), "interface members win over item names");
        Check.equal("0", f.scripts.evaluate("localStorage.clear(); String(localStorage.length)", "t"), "clear");
        Check.equal("true", f.scripts.evaluate("sessionStorage.setItem('s', '1'); String(sessionStorage.getItem('s') === '1' && "
                + "localStorage.getItem('s') === null)", "t"), "sessionStorage is separate from localStorage");
        Check.equal(1, f.env.session.entries().size(), "the Java view of the component's sessionStorage has the item");
        Check.equal("TypeError", f.scripts.evaluate("try { localStorage.setItem('only-one') } catch (e) { e.name }", "t"), "missing argument");
    }

    /**
     * P16-07: a write over the quota throws QuotaExceededError and keeps the old data.
     * <p>
     * P16-07: 쿼터를 넘는 쓰기는 QuotaExceededError 를 던지고 옛 데이터를 유지한다.
     */
    public void testQuotaExceededError() {
        PageFixture f = fixture().load("<p></p>");
        f.scripts.evaluate("localStorage.setItem('keep', 'yes');", "t");
        Check.equal("QuotaExceededError,22,true", f.scripts.evaluate("var big = new Array(3000000).join('x'); try { localStorage.setItem('big', big); "
                + "'no error'; } catch (e) { e.name + ',' + e.code + ',' + (e instanceof DOMException) }", "t"), "quota error");
        Check.equal("yes,null", f.scripts.evaluate("localStorage.getItem('keep') + ',' + localStorage.getItem('big')", "t"), "old data is intact");
    }

    /**
     * P16-07: documents with an opaque origin have no storage (SecurityError); classpath documents share one origin.
     * <p>
     * P16-07: 불투명 오리진의 문서에는 저장소가 없고(SecurityError) classpath 문서는 한 오리진을 공유한다.
     */
    public void testOpaqueAndClasspathOrigins() {
        PageFixture blank = fixture().load("about:blank", "<p></p>");
        Check.equal("SecurityError,SecurityError", blank.scripts.evaluate("var r = []; try { localStorage } catch (e) { r.push(e.name); } "
                + "try { sessionStorage } catch (e) { r.push(e.name); } r.join(',')", "t"), "about:blank has no storage");
        PageFixture a = fixture().load("classpath:///docs/a.html", "<p></p>");
        a.scripts.evaluate("localStorage.setItem('shared', 'cp')", "t");
        PageFixture b = fixture().load("classpath:///other/b.html", "<p></p>");
        Check.equal("cp", b.scripts.evaluate("localStorage.getItem('shared')", "t"), "classpath documents share the origin");
        PageFixture w = fixture().load("http://web.test/x.html", "<p></p>");
        Check.equal("null", w.scripts.evaluate("String(localStorage.getItem('shared'))", "t"), "a web origin does not see it");
    }

    /**
     * P16-08/P16-10: two pages of the same origin share localStorage and the other page gets a storage event (not the writer); another
     * origin gets nothing; sessionStorage events stay inside the component.
     * <p>
     * P16-08/P16-10: 같은 오리진의 두 페이지는 localStorage 를 공유하고 다른 페이지가 storage 이벤트를 받는다(쓴 페이지는 받지 않는다). 다른 오리진은 받지
     * 않고 sessionStorage 이벤트는 컴포넌트 안에 머문다.
     */
    public void testStorageEvents() {
        PageFixture a = fixture();
        PageFixture b = fixture();
        PageFixture c = fixture();
        a.load("http://same.test/a.html", "<script>window.addEventListener('storage', function (e) { console.log('a:' + e.key + ':' + e.oldValue + "
                + "':' + e.newValue + ':' + (e.storageArea === localStorage)); }); localStorage.length;</script>");
        b.load("http://same.test/b.html", "<script>window.addEventListener('storage', function (e) { console.log('b:' + e.key + ':' + e.oldValue + "
                + "':' + e.newValue + ':' + (e.storageArea === localStorage) + ':' + (e instanceof StorageEvent) + ':' + e.url); }); localStorage.length;"
                + "</script>");
        c.load("http://other.test/c.html", "<script>window.addEventListener('storage', function (e) { console.log('c:' + e.key); }); "
                + "localStorage.length;</script>");
        a.scripts.evaluate("localStorage.setItem('k', 'v1'); localStorage.setItem('k', 'v1'); localStorage.setItem('k', 'v2'); "
                + "localStorage.removeItem('k'); localStorage.removeItem('k'); localStorage.setItem('z', '1'); localStorage.clear();", "t");
        a.advance(5);
        b.advance(5);
        c.advance(5);
        Check.equal("", a.log(), "the page that wrote gets no event");
        Check.equal("b:k:null:v1:true:true:http://same.test/b.html,b:k:v1:v2:true:true:http://same.test/b.html,b:k:v2:null:true:true:http://same.test/b.html,"
                + "b:z:null:1:true:true:http://same.test/b.html,b:null:null:null:true:true:http://same.test/b.html", b.log(),
                "set, replace, remove, set and clear are reported once each, unchanged values are not");
        Check.equal("", c.log(), "another origin gets nothing");
        PageFixture s1 = fixture();
        s1.load("http://same.test/s.html", "<script>sessionStorage.length</script>");
        s1.scripts.evaluate("sessionStorage.setItem('only', 'here')", "t");
        Check.equal("null", s1.scripts.evaluate("String(localStorage.getItem('only'))", "t"), "sessionStorage values never reach localStorage");
    }

    /**
     * P16-09/P01-16: after dispose the storage listener is removed and nothing is delivered.
     * <p>
     * P16-09/P01-16: 폐기 뒤에는 저장소 리스너가 제거되어 아무것도 전달되지 않는다.
     */
    public void testDisposeRemovesStorageListeners() {
        PageFixture a = fixture().load("http://same.test/a.html", "<script>window.addEventListener('storage', function (e) { "
                + "console.log('got'); }); localStorage.length;</script>");
        PageFixture b = fixture().load("http://same.test/b.html", "<script>localStorage.length;</script>");
        a.scripts.dispose();
        b.scripts.evaluate("localStorage.setItem('x', 'y')", "t");
        a.advance(10);
        Check.equal("", a.log(), "a disposed page gets no event");
    }
}
