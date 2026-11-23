package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script side of image loading: the <code>load</code> and <code>error</code> events of <code>img</code> elements, the state
 * that scripts read (<code>complete</code>, <code>naturalWidth</code>, <code>currentSrc</code>), <code>new Image()</code>,
 * <code>decode()</code>, stale results, and the delay of the window <code>load</code> event (plan items P18-05, P15-04-b-2). The images are
 * served by a fake service that the test completes by hand.
 * <p>
 * 이미지 로딩의 스크립트 쪽 시험: <code>img</code> 요소의 <code>load</code>, <code>error</code> 이벤트, 스크립트가 읽는 상태(<code>complete</code>,
 * <code>naturalWidth</code>, <code>currentSrc</code>), <code>new Image()</code>, <code>decode()</code>, 오래된 결과, window <code>load</code>
 * 이벤트 지연(계획 P18-05, P15-04-b-2). 이미지는 시험이 손으로 완료시키는 가짜 서비스가 제공한다.
 */
public class ImageLoadsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageLoadsTest() {
    }

    /**
     * A fake image service: it records the requests and completes them when the test says so.
     * <p>
     * 가짜 이미지 서비스: 요청을 기록하고 시험이 지시할 때 완료시킨다.
     */
    private static final class FakeImages implements ImageRequestSource {
        /** The requested URLs in order. / 요청된 URL(순서대로). */
        final List<String> urls = new ArrayList<String>();
        /** The receivers in the order of the requests. / 요청 순서대로의 받는 쪽. */
        final List<Callback> callbacks = new ArrayList<Callback>();

        /**
         * Records a request.
         * <p>
         * 요청을 기록한다.
         *
         * @param absoluteUrl the URL / URL
         * @param callback    the receiver / 받는 쪽
         */
        public void request(String absoluteUrl, Callback callback) {
            urls.add(absoluteUrl);
            callbacks.add(callback);
        }

        /**
         * Completes the request with the given index successfully.
         * <p>
         * 주어진 번호의 요청을 성공으로 완료시킨다.
         *
         * @param index  the request number / 요청 번호
         * @param width  the natural width / 자연 너비
         * @param height the natural height / 자연 높이
         */
        void ok(int index, int width, int height) {
            callbacks.get(index).done(width, height, null);
        }

        /**
         * Completes the request with the given index as failed.
         * <p>
         * 주어진 번호의 요청을 실패로 완료시킨다.
         *
         * @param index the request number / 요청 번호
         */
        void fail(int index) {
            callbacks.get(index).done(0, 0, "404");
        }
    }

    /**
     * Creates a fixture with a fake image service and the log array of the page.
     * <p>
     * 가짜 이미지 서비스와 페이지의 로그 배열이 있는 시험 도구를 만든다.
     *
     * @param images the service or null / 서비스 또는 null
     * @return the fixture (not loaded yet) / 시험 도구(아직 로드 전)
     */
    private static PageFixture fixture(FakeImages images) {
        PageFixture f = new PageFixture();
        f.env.images = images;
        return f;
    }

    /**
     * Runs the queued tasks of the page.
     * <p>
     * 페이지에 올라온 작업을 실행한다.
     *
     * @param f the fixture / 시험 도구
     */
    private static void drain(PageFixture f) {
        f.env.scheduler.advance(0);
    }

    /**
     * The events fire in the order of completion, the state is readable inside the handlers, and the window load event waits for all images
     * that started while the document loaded, whether they succeeded or failed.
     * <p>
     * 이벤트는 완료 순서대로 발생하고, 핸들러 안에서 상태를 읽을 수 있으며, window load 이벤트는 문서를 로딩하는 동안 시작한 모든 이미지를
     * 성공하든 실패하든 기다린다.
     */
    public void testEventsAndLoadDelay() {
        FakeImages images = new FakeImages();
        PageFixture f = fixture(images);
        f.start("http://example.test/dir/page.html", "<script>var log = []; window.addEventListener('load', function () { log.push('window-load'); });</script>"
                + "<img id=a src='a.png' onload=\"log.push('a:' + this.naturalWidth + 'x' + this.naturalHeight + ':' + this.complete)\">"
                + "<img id=b src='/b.png' onerror=\"log.push('b-error:' + this.complete + ':' + this.naturalWidth)\">");
        Check.equal(2, images.urls.size(), "two requests were started while parsing");
        Check.equal("http://example.test/dir/a.png", images.urls.get(0), "the URL is resolved against the document");
        Check.equal("http://example.test/b.png", images.urls.get(1), "an absolute path is resolved too");
        Check.equal("interactive", f.scripts.getReadyState(), "the document waits for its images");
        Check.equal("false,0,http://example.test/dir/a.png", f.scripts.evaluate("var a = document.getElementById('a'); [a.complete, a.naturalWidth, a.currentSrc].join(',')", "t"),
                "while loading: not complete, no size, but currentSrc is known");
        images.ok(0, 30, 20);
        drain(f);
        Check.equal("a:30x20:true", f.scripts.evaluate("log.join('|')", "t"), "load fired once with the state readable");
        Check.equal("interactive", f.scripts.getReadyState(), "one image is still pending");
        images.fail(1);
        drain(f);
        Check.equal("a:30x20:true|b-error:true:0|window-load", f.scripts.evaluate("log.join('|')", "t"),
                "the error event of the failed image comes before the window load event");
        Check.equal("complete", f.scripts.getReadyState(), "all images ended: the document is complete");
        Check.equal("true,30", f.scripts.evaluate("[a.complete, a.naturalWidth].join(',')", "t"), "the state stays");
    }

    /**
     * An image that starts after the window load event does not delay anything; <code>new Image()</code> loads without being in the document;
     * <code>decode()</code> follows the result.
     * <p>
     * window load 이벤트 뒤에 시작한 이미지는 아무것도 늦추지 않는다. <code>new Image()</code> 는 문서에 없어도 로드한다. <code>decode()</code> 는 결과를
     * 따른다.
     */
    public void testNewImageAndDecode() {
        FakeImages images = new FakeImages();
        PageFixture f = fixture(images).load("<script>var log = [];</script>");
        Check.equal("complete", f.scripts.getReadyState(), "no images: the page is complete");
        f.scripts.evaluate("var img = new Image(5, 6); img.onload = function () { log.push('load:' + img.width + 'x' + img.height + ':' + img.naturalWidth); };"
                + "img.src = 'n.png'; log.push('src-set');"
                + "img.decode().then(function () { log.push('decoded'); }, function (e) { log.push('decode-failed:' + e.name); });", "t");
        Check.equal(1, images.urls.size(), "setting src on an image that is not in the document starts a load");
        Check.equal("http://example.test/dir/n.png", images.urls.get(0), "resolved");
        Check.equal("true,false", f.scripts.evaluate("[img instanceof HTMLImageElement, img.complete].join(',')", "t"), "an Image is an HTMLImageElement");
        images.ok(0, 12, 8);
        drain(f);
        Check.equal("src-set|load:5x6:12|decoded", f.scripts.evaluate("log.join('|')", "t"), "load, then the decode promise resolves");
        f.scripts.evaluate("var bad = new Image(); bad.src = 'bad.png'; bad.decode().then(function () { log.push('bad-decoded'); }, function (e) { log.push('bad:' + e.name); });", "t");
        images.fail(1);
        drain(f);
        Check.equal("src-set|load:5x6:12|decoded|bad:EncodingError", f.scripts.evaluate("log.join('|')", "t"), "a failed image rejects decode() with EncodingError");
        Check.equal("true,true", f.scripts.evaluate("[new Image().complete, document.createElement('img').complete].join(',')", "t"),
                "an image without a source is complete");
    }

    /**
     * A result of an older source is dropped after the source changed; the same source is not requested twice; a failed one is retried.
     * <p>
     * 소스가 바뀐 뒤에는 오래된 소스의 결과를 버린다. 같은 소스를 두 번 요청하지 않고, 실패한 것은 다시 시도한다.
     */
    public void testStaleResultsAndRetry() {
        FakeImages images = new FakeImages();
        PageFixture f = fixture(images).load("<script>var log = [];</script><img id=i>");
        f.scripts.evaluate("var i = document.getElementById('i'); i.onload = function () { log.push('load:' + i.currentSrc.split('/').pop()); };"
                + "i.onerror = function () { log.push('error'); }; i.src = 'one.png'; i.src = 'one.png'; i.src = 'two.png';", "t");
        Check.equal("[one.png, two.png]", images.urls.toString().replace("http://example.test/dir/", ""), "the same value is not requested twice in a row");
        images.ok(0, 1, 1);
        drain(f);
        Check.equal("", f.scripts.evaluate("log.join('|')", "t"), "the result of the replaced source is ignored");
        images.ok(1, 2, 2);
        drain(f);
        Check.equal("load:two.png", f.scripts.evaluate("log.join('|')", "t"), "only the current source fires load");
        f.scripts.evaluate("i.src = 'gone.png';", "t");
        images.fail(2);
        drain(f);
        Check.equal("load:two.png|error", f.scripts.evaluate("log.join('|')", "t"), "the failure fires error");
        f.scripts.evaluate("i.src = 'gone.png';", "t");
        Check.equal(4, images.urls.size(), "a source that failed is requested again");
    }

    /**
     * An empty <code>src</code> is an error; an element without a source is complete and silent; removing the source resets the state.
     * <p>
     * 비어 있는 <code>src</code> 는 오류이다. 소스가 없는 요소는 완료되고 조용하며, 소스를 지우면 상태가 초기화된다.
     */
    public void testEmptyAndRemovedSource() {
        FakeImages images = new FakeImages();
        PageFixture f = fixture(images).load("<script>var log = [];</script><img id=e src='' onerror=\"log.push('empty-error')\"><img id=n onerror=\"log.push('n-error')\" onload=\"log.push('n-load')\">");
        drain(f);
        Check.equal("empty-error", f.scripts.evaluate("log.join('|')", "t"), "an empty src fires error, an element without src fires nothing");
        Check.equal(0, images.urls.size(), "nothing was requested");
        Check.equal("true,0,", f.scripts.evaluate("var n = document.getElementById('n'); [n.complete, n.naturalWidth, n.currentSrc].join(',')", "t"), "no source: complete and empty");
        f.scripts.evaluate("n.src = 'x.png';", "t");
        images.ok(0, 9, 9);
        drain(f);
        Check.equal("empty-error|n-load", f.scripts.evaluate("log.join('|')", "t"), "load after setting the source");
        f.scripts.evaluate("n.removeAttribute('src');", "t");
        Check.equal("true,0,", f.scripts.evaluate("[n.complete, n.naturalWidth, n.currentSrc].join(',')", "t"), "removing the source empties the state");
    }

    /**
     * <code>srcset</code>, <code>sizes</code> and <code>picture</code> choose the file; the density scales the natural size; changing a
     * <code>source</code> of the picture loads the new choice.
     * <p>
     * <code>srcset</code>, <code>sizes</code>, <code>picture</code> 가 파일을 고르고, 밀도는 자연 크기를 줄이며, picture 의 <code>source</code> 를
     * 바꾸면 새로 고른 것을 로드한다.
     */
    public void testSourceSelection() {
        FakeImages images = new FakeImages();
        PageFixture f = fixture(images).load("<script>var log = [];</script>"
                + "<img id=a srcset='lo.png 1x, hi.png 2x' src='fallback.png'>"
                + "<img id=b srcset='only2x.png 2x'>"
                + "<picture id=p><source id=s srcset='wide.png' media='(min-width: 10000px)'><img id=c src='narrow.png'></picture>");
        Check.equal("[lo.png, only2x.png, narrow.png]", images.urls.toString().replace("http://example.test/dir/", ""),
                "dpr 1: the explicit 1x candidate wins (src is then not a candidate); a lone 2x is used; the picture falls back to its img");
        images.ok(1, 40, 20);
        drain(f);
        Check.equal("20,10", f.scripts.evaluate("var b = document.getElementById('b'); [b.naturalWidth, b.naturalHeight].join(',')", "t"),
                "a 2x image is half as large in CSS px");
        f.scripts.evaluate("document.getElementById('s').setAttribute('media', 'all');", "t");
        Check.equal(4, images.urls.size(), "changing the media of the source selects the new file");
        Check.equal("http://example.test/dir/wide.png", images.urls.get(3), "the picture now uses wide.png");
        Check.equal("http://example.test/dir/wide.png", f.scripts.evaluate("document.getElementById('c').currentSrc", "t"), "currentSrc follows");
    }

    /**
     * Without an image service the page cannot load images: nothing fires, <code>complete</code> stays false and the load event is not
     * delayed. Disposal drops results that arrive later.
     * <p>
     * 이미지 서비스가 없으면 페이지가 이미지를 로드할 수 없다: 아무 이벤트도 발생하지 않고 <code>complete</code> 는 false 로 남으며 load 이벤트는
     * 지연되지 않는다. 폐기하면 나중에 도착하는 결과는 버린다.
     */
    public void testNoServiceAndDisposal() {
        PageFixture f = fixture(null).load("<script>var log = [];</script><img id=a src='a.png' onload=\"log.push('load')\" onerror=\"log.push('error')\">");
        Check.equal("complete", f.scripts.getReadyState(), "the load event is not delayed without a service");
        Check.equal("false", f.scripts.evaluate("String(document.getElementById('a').complete)", "t"), "the image never completes");
        Check.equal("", f.scripts.evaluate("log.join('|')", "t"), "and fires nothing");
        FakeImages images = new FakeImages();
        PageFixture g = fixture(images);
        g.start("http://example.test/dir/page.html", "<script>var log = [];</script><img id=a src='a.png' onload=\"log.push('load')\">");
        g.scripts.dispose();
        images.ok(0, 3, 3);
        drain(g);
        Check.isTrue(g.console.isEmpty(), "a result after disposal is dropped without any error: " + g.console);
        Check.isTrue(g.scripts.isDisposed(), "the page stays disposed");
    }
}
