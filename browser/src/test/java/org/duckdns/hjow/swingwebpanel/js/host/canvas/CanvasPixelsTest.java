package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.js.host.ImageRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.render.ImageResource;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the pixels that enter and leave the canvas (plan item P20-05): <code>ImageData</code> and its typed array, <code>getImageData</code>
 * and <code>putImageData</code>, <code>drawImage</code> with canvases and <code>img</code> elements in all three forms, the origin-clean
 * rule (cross-origin images, CORS permission, taint through canvases and patterns), <code>toDataURL</code> and <code>toBlob</code> (the files
 * are decoded again with ImageIO), resetting by the size attributes, the size limits, and the release of the bitmaps with the page.
 * <p>
 * 캔버스로 들어오고 나가는 화소의 시험(계획 P20-05): <code>ImageData</code> 와 그 형식화 배열, <code>getImageData</code> 와 <code>putImageData</code>,
 * 캔버스와 <code>img</code> 요소를 세 형식으로 쓰는 <code>drawImage</code>, origin-clean 규칙(교차 오리진 이미지, CORS 허용, 캔버스와 패턴을 통한 오염),
 * <code>toDataURL</code> 과 <code>toBlob</code>(파일을 ImageIO 로 다시 디코딩), 크기 속성에 의한 초기화, 크기 한도, 페이지와 함께 비트맵 해제.
 */
public class CanvasPixelsTest {

    /**
     * The helper script of the page.
     * <p>
     * 페이지의 도우미 스크립트.
     */
    private static final String HELPERS = CanvasBasicsTest.HELPERS + "function alpha(x, y) { return ctx.getImageData(x, y, 1, 1).data[3]; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasPixelsTest() {
    }

    /**
     * A fake image service that completes at once with the images the test registered (or fails).
     * <p>
     * 시험이 등록한 이미지로 곧바로 완료하는(없으면 실패하는) 가짜 이미지 서비스.
     */
    private static final class Immediate implements ImageRequestSource {
        /**
         * The registered images by URL.
         * <p>
         * URL 별로 등록된 이미지.
         */
        private final Map<String, ImageResource> images;

        /**
         * Creates the service.
         * <p>
         * 서비스를 만든다.
         *
         * @param images the registered images / 등록된 이미지
         */
        Immediate(Map<String, ImageResource> images) {
            this.images = images;
        }

        /**
         * Completes the request.
         * <p>
         * 요청을 완료시킨다.
         *
         * @param absoluteUrl the URL / URL
         * @param callback    the receiver / 받는 쪽
         */
        public void request(String absoluteUrl, Callback callback) {
            ImageResource r = images.get(absoluteUrl);
            if (r == null || r.getImage() == null) {
                callback.done(0, 0, "404");
            } else {
                callback.done(r.getImage().getWidth(), r.getImage().getHeight(), null);
            }
        }
    }

    /**
     * Makes an image whose left half and right half have two colors.
     * <p>
     * 왼쪽 절반과 오른쪽 절반이 두 가지 색인 이미지를 만든다.
     *
     * @param w     width / 너비
     * @param h     height / 높이
     * @param left  the ARGB color of the left half / 왼쪽 절반의 ARGB 색
     * @param right the ARGB color of the right half / 오른쪽 절반의 ARGB 색
     * @return the image / 이미지
     */
    private static DecodedImage twoColors(int w, int h, int left, int right) {
        int[] argb = new int[w * h];
        // 왼쪽 절반과 오른쪽 절반을 각자의 색으로 채운다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                argb[y * w + x] = x < w / 2 ? left : right;
            }
        }
        return new DecodedImage(w, h, argb);
    }

    /**
     * Creates a fixture whose document is at <code>http://example.test/dir/page.html</code> and which knows the given images.
     * <p>
     * 문서가 <code>http://example.test/dir/page.html</code> 에 있고 주어진 이미지를 아는 시험 도구를 만든다.
     *
     * @param body the markup of the body / body 의 마크업
     * @return the fixture / 시험 도구
     */
    private static PageFixture withImages(String body) {
        PageFixture f = new PageFixture();
        f.env.images = new Immediate(f.env.loadedImages);
        f.env.loadedImages.put("http://example.test/dir/same.png", ImageResource.loaded(twoColors(4, 4, 0xFFFF0000, 0xFF0000FF),
                "http://example.test/dir/same.png", null, null));
        f.env.loadedImages.put("http://other.test/foreign.png", ImageResource.loaded(twoColors(4, 4, 0xFF00FF00, 0xFFFF00FF), "http://other.test/foreign.png",
                null, null));
        f.env.loadedImages.put("http://other.test/open.png", ImageResource.loaded(twoColors(4, 4, 0xFF00FF00, 0xFFFF00FF), "http://other.test/open.png", "*",
                null));
        f.env.loadedImages.put("http://other.test/exact.png", ImageResource.loaded(twoColors(4, 4, 0xFF00FF00, 0xFFFF00FF), "http://other.test/exact.png",
                "http://example.test", "true"));
        f.load(body);
        f.scripts.evaluate(HELPERS, "helpers");
        f.env.scheduler.advance(0);
        return f;
    }

    /**
     * Evaluates a script.
     * <p>
     * 스크립트를 평가한다.
     *
     * @param f    the page / 페이지
     * @param code the script / 스크립트
     * @return the result / 결과
     */
    private static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * Creates a page with a canvas of a size.
     * <p>
     * 크기가 정해진 캔버스가 있는 페이지를 만든다.
     *
     * @param w the width / 너비
     * @param h the height / 높이
     * @return the page / 페이지
     */
    private static PageFixture page(int w, int h) {
        PageFixture f = new PageFixture().load("<canvas id=c width=" + w + " height=" + h + "></canvas>");
        f.scripts.evaluate(HELPERS, "helpers");
        return f;
    }

    /**
     * ImageData objects: sizes, the typed array and its clamping, the constructors and their errors.
     * <p>
     * ImageData 객체: 크기, 형식화 배열과 그 자르기, 생성자와 오류.
     */
    public void testImageDataObjects() {
        PageFixture f = page(10, 10);
        Check.equal("2,3,24,true,srgb", js(f, "var d = ctx.createImageData(2, 3); [d.width, d.height, d.data.length, d.data instanceof Uint8ClampedArray, d.colorSpace].join()"),
                "createImageData");
        Check.equal("true", js(f, "var z = true; for (var i = 0; i < d.data.length; i++) { if (d.data[i] !== 0) z = false; } String(z && d.data === d.data)"),
                "transparent black, and the same array every time");
        Check.equal("2,3", js(f, "var d2 = ctx.createImageData(-2, 3); [d2.width, d2.height].join()"), "a negative size is made positive");
        Check.equal("2,3", js(f, "var d3 = ctx.createImageData(d); [d3.width, d3.height].join()"), "createImageData(imagedata) copies the size");
        Check.equal("IndexSizeError", js(f, "try { ctx.createImageData(0, 3) } catch (e) { e.name }"), "a zero size");
        Check.equal("TypeError", js(f, "try { ctx.createImageData({}) } catch (e) { e.name }"), "a non-ImageData object");
        Check.equal("255,0,0,5", js(f, "d.data[0] = 300; d.data[1] = -5; d.data[2] = 0; d.data[3] = 4.6; [d.data[0], d.data[1] + 0, d.data[2], d.data[3]].join()"),
                "the Uint8ClampedArray clamps and rounds");
        Check.equal("2,2,16", js(f, "var n = new ImageData(2, 2); [n.width, n.height, n.data.length].join()"), "new ImageData(w, h)");
        Check.equal("IndexSizeError", js(f, "try { new ImageData(0, 2) } catch (e) { e.name }"), "a zero width");
        Check.equal("TypeError", js(f, "try { ImageData(2, 2) } catch (e) { e.name }"), "the constructor needs new");
        Check.equal("TypeError", js(f, "try { new ImageData(2) } catch (e) { e.name }"), "the height is required");
        Check.equal("true,2,2", js(f, "var arr = new Uint8ClampedArray(16); var w = new ImageData(arr, 2); [w.data === arr, w.width, w.height].join()"),
                "new ImageData(array, width) shares the array and derives the height");
        Check.equal("1,4", js(f, "var w2 = new ImageData(new Uint8ClampedArray(16), 1); [w2.width, w2.height].join()"), "another shape of the same data");
        Check.equal("InvalidStateError", js(f, "try { new ImageData(new Uint8ClampedArray(15), 2) } catch (e) { e.name }"), "a length that is no multiple of 4");
        Check.equal("IndexSizeError", js(f, "try { new ImageData(new Uint8ClampedArray(16), 3) } catch (e) { e.name }"), "a width that does not divide the pixels");
        Check.equal("IndexSizeError", js(f, "try { new ImageData(new Uint8ClampedArray(16), 2, 3) } catch (e) { e.name }"), "a height that does not fit");
        Check.equal("TypeError", js(f, "try { new ImageData(new Uint8Array(16), 2) } catch (e) { e.name }"), "only a Uint8ClampedArray");
        Check.equal("[object ImageData]", js(f, "Object.prototype.toString.call(d)"), "interface name");
    }

    /**
     * Shared and resizable pixel arrays are rejected before Canvas creates a live ImageData wrapper.
     * <p>
     * Canvas가 ImageData 래퍼를 만들기 전에 공유 및 가변 픽셀 배열을 거부한다.
     */
    public void testSharedMemoryImageDataBoundary() {
        PageFixture f = page(2, 2);
        String script = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "var s=new Uint8ClampedArray(new SharedArrayBuffer(4)), r=new Uint8ClampedArray(new ArrayBuffer(4,{maxByteLength:8}));"
                + "var old=new ArrayBuffer(4), detached=new Uint8ClampedArray(old);old.transfer();s[0]=42;"
                + "[fail(()=>new ImageData(s,1)),fail(()=>new ImageData(r,1)),fail(()=>new ImageData(detached,1)),s[0]].join('|')";
        Check.equal("TypeError|TypeError|InvalidStateError|42", js(f, script), "pixel data uses an eligible fixed backing buffer");
    }

    /**
     * <code>getImageData</code> reads the pixels (also outside the canvas and with negative sizes) and <code>putImageData</code> writes them
     * without matrix, clip, alpha, shadow or operator, with dirty rectangles.
     * <p>
     * <code>getImageData</code> 는 화소를 읽고(캔버스 밖과 음수 크기도), <code>putImageData</code> 는 행렬, 클립, 알파, 그림자, 연산자 없이 쓰며 변경 사각형을
     * 지원한다.
     */
    public void testGetAndPutImageData() {
        PageFixture f = page(10, 10);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(2, 2, 4, 4); var g = ctx.getImageData(1, 1, 6, 6);");
        Check.equal("6,6,144", js(f, "[g.width, g.height, g.data.length].join()"), "size of the result");
        Check.equal("255,0,0,255", js(f, "var o = (1 * 6 + 1) * 4; [g.data[o], g.data[o + 1], g.data[o + 2], g.data[o + 3]].join()"), "the pixel (2, 2) is at (1, 1) of the result");
        Check.equal("0,0,0,0", js(f, "[g.data[0], g.data[1], g.data[2], g.data[3]].join()"), "the pixel (1, 1) is transparent");
        Check.equal("0,0,0,0,0,0,0,0", js(f, "var out = ctx.getImageData(-5, -5, 2, 2); out.data.slice(0, 8).join()"), "outside the canvas everything is transparent black");
        Check.equal("true", js(f, "var part = ctx.getImageData(-1, -1, 4, 4); String(part.data[(1 * 4 + 1) * 4 + 3] === 0 && part.data[(3 * 4 + 3) * 4 + 3] === 255)"),
                "a rectangle that is partly outside keeps the position of the inside pixels");
        Check.equal("4,4", js(f, "var neg = ctx.getImageData(5, 5, -4, -4); [neg.width, neg.height].join()"), "negative sizes mirror the rectangle");
        Check.equal("255", js(f, "String(neg.data[(3 * 4 + 3) * 4 + 3])"), "it covers pixels 1 to 4: the last one is the red pixel (4, 4)");
        Check.equal("IndexSizeError", js(f, "try { ctx.getImageData(0, 0, 0, 1) } catch (e) { e.name }"), "a zero width");
        Check.equal("IndexSizeError", js(f, "try { ctx.getImageData(0, 0, 1, 0) } catch (e) { e.name }"), "a zero height");
        Check.equal("TypeError", js(f, "try { ctx.getImageData(0, 0, 1) } catch (e) { e.name }"), "four arguments are required");
        js(f, "ctx.clearRect(0, 0, 10, 10); var id = ctx.createImageData(2, 2); id.data.set([0, 255, 0, 255, 0, 0, 255, 255, 255, 255, 0, 128, 1, 2, 3, 4]);"
                + " ctx.putImageData(id, 3, 3);");
        Check.equal("0,255,0,255", js(f, "px(3, 3)"), "the first pixel of the image data");
        Check.equal("0,0,255,255", js(f, "px(4, 3)"), "the second");
        Check.equal("255,255,0,128", js(f, "px(3, 4)"), "a translucent pixel is stored without premultiplication, so it reads back exactly");
        Check.equal("1,2,3,4", js(f, "px(4, 4)"), "even with a tiny alpha");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10); ctx.save(); ctx.globalAlpha = 0.1; ctx.globalCompositeOperation = 'xor'; ctx.translate(100, 100);"
                + " ctx.shadowColor = '#000000'; ctx.shadowOffsetX = 5; ctx.beginPath(); ctx.rect(0, 0, 1, 1); ctx.clip(); ctx.putImageData(id, 3, 3); ctx.restore();");
        Check.equal("0,255,0,255", js(f, "px(3, 3)"), "putImageData ignores alpha, operator, matrix, shadow and clip");
        Check.equal("255,255,0,128", js(f, "px(3, 4)"), "and replaces the pixels instead of blending (the red below is gone)");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10); ctx.putImageData(id, 5, 5, 1, 1, 1, 1);");
        Check.equal("1,2,3,4", js(f, "px(6, 6)"), "a dirty rectangle writes only its pixels (image pixel (1, 1) lands at (6, 6))");
        Check.equal("255,0,0,255", js(f, "px(5, 5)"), "the others stay");
        js(f, "ctx.fillRect(0, 0, 10, 10); ctx.putImageData(id, 0, 0, 2, 2, -2, -2);");
        Check.equal("0,255,0,255", js(f, "px(0, 0)"), "a negative dirty size mirrors the rectangle: pixels (0, 0) to (1, 1) are written");
        js(f, "ctx.fillRect(0, 0, 10, 10); ctx.putImageData(id, 9, 9); ctx.putImageData(id, -1, -1);");
        Check.equal("0,255,0,255", js(f, "px(9, 9)"), "pixels outside the canvas are cut off at the edge");
        Check.equal("1,2,3,4", js(f, "px(0, 0)"), "and placed at (-1, -1) the last pixel of the image data lands at (0, 0)");
        Check.equal("TypeError", js(f, "try { ctx.putImageData({}, 0, 0) } catch (e) { e.name }"), "not an ImageData");
        Check.equal("TypeError", js(f, "try { ctx.putImageData(id, 0, 0, 1) } catch (e) { e.name }"), "a partial dirty rectangle is a TypeError");
        Check.equal("true", js(f, "var cp = ctx.getImageData(0, 0, 2, 2); cp.data[0] = 99; String(ctx.getImageData(0, 0, 1, 1).data[0] !== 99)"), "the result is a copy of the pixels");
    }

    /**
     * <code>drawImage</code> with another canvas, in the forms with 3, 5 and 9 arguments, with mirroring, clipping and the canvas itself.
     * <p>
     * 다른 캔버스를 쓰는 <code>drawImage</code>(인수 3, 5, 9 개의 형식), 뒤집기, 자르기, 캔버스 자신.
     */
    public void testDrawImageCanvas() {
        PageFixture f = page(30, 30);
        js(f, "var src = document.createElement('canvas'); src.width = 4; src.height = 4; var s = src.getContext('2d'); s.fillStyle = '#ff0000'; s.fillRect(0, 0, 2, 4);"
                + " s.fillStyle = '#0000ff'; s.fillRect(2, 0, 2, 4); ctx.imageSmoothingEnabled = false; ctx.drawImage(src, 2, 3);");
        Check.equal("255,0,0,255", js(f, "px(2, 3)"), "3 arguments: the image at its size, the top left pixel");
        Check.equal("0,0,255,255", js(f, "px(5, 6)"), "the bottom right pixel is blue");
        Check.equal("0,0", js(f, "[alpha(6, 3), alpha(2, 7)].join()"), "and nothing outside");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.drawImage(src, 0, 0, 8, 8);");
        Check.equal("255,0,0,255", js(f, "px(3, 7)"), "5 arguments: scaled up twice without smoothing, so the pixels stay sharp (red up to x = 3)");
        Check.equal("0,0,255,255", js(f, "px(4, 0)"), "and blue from x = 4");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.drawImage(src, 2, 0, 2, 4, 10, 10, 2, 4);");
        Check.equal("0,0,255,255", js(f, "px(10, 10)"), "9 arguments: the right half of the source (blue)");
        Check.equal("0,0,255,255", js(f, "px(11, 13)"), "to its place");
        Check.equal("0,0,0,0", js(f, "px(12, 10)"), "and not wider");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.drawImage(src, 0, 0, 4, 4, 10, 0, -4, 4);");
        Check.equal("255,0,0,255", js(f, "px(6, 1)"), "a negative destination width names the rectangle left of the point (6 to 10) and does not mirror: red on the left");
        Check.equal("0,0,255,255", js(f, "px(9, 1)"), "and blue on the right");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.drawImage(src, 2, 0, 4, 4, 0, 0, 8, 8);");
        Check.equal("0,0,255,255", js(f, "px(1, 1)"), "a source rectangle that reaches beyond the image is cut: the visible half is scaled to its half of the destination");
        Check.equal("0,0,0,0", js(f, "px(6, 1)"), "and the rest of the destination stays empty");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.drawImage(src, 0, 0, 0, 4, 0, 0, 4, 4); ctx.drawImage(src, 0, 0, 4, 4, 0, 0, 0, 4); ctx.drawImage(src, 10, 10, 4, 4, 0, 0, 4, 4);");
        Check.equal("0,0,0,0", js(f, "px(1, 1)"), "empty rectangles and a source rectangle outside the image draw nothing");
        js(f, "ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 4, 4); ctx.drawImage(cv, 0, 0, 4, 4, 20, 20, 4, 4);");
        Check.equal("0,255,0,255", js(f, "px(20, 20)"), "a canvas can be drawn onto itself (the pixels are read before they are written)");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.save(); ctx.translate(10, 10); ctx.scale(2, 2); ctx.globalAlpha = 0.5; ctx.drawImage(src, 0, 0); ctx.restore();");
        Check.equal("255,0,0,128", js(f, "px(10, 10)"), "the matrix and the global alpha apply");
        Check.equal("0,0,255,128", js(f, "px(17, 17)"), "at the doubled size (8 by 8 pixels: blue starts at 14)");
        Check.equal("InvalidStateError", js(f, "var zero = document.createElement('canvas'); zero.width = 0; try { ctx.drawImage(zero, 0, 0) } catch (e) { e.name }"),
                "a canvas without area is an InvalidStateError");
        Check.equal("TypeError", js(f, "try { ctx.drawImage({}, 0, 0) } catch (e) { e.name }"), "not an image source");
        Check.equal("TypeError", js(f, "try { ctx.drawImage(src, 0, 0, 4) } catch (e) { e.name }"), "four arguments are not a valid form");
        Check.equal("TypeError", js(f, "try { ctx.drawImage(src, 0) } catch (e) { e.name }"), "too few arguments");
        Check.equal("undefined", js(f, "String(ctx.drawImage(src, NaN, 0))"), "NaN arguments are ignored");
    }

    /**
     * <code>drawImage</code> with <code>img</code> elements: loaded images are drawn, images that are not complete are skipped, broken ones are
     * an error.
     * <p>
     * <code>img</code> 요소를 쓰는 <code>drawImage</code>: 읽은 이미지는 그려지고, 완료되지 않은 이미지는 건너뛰며, 깨진 이미지는 오류이다.
     */
    public void testDrawImageElement() {
        PageFixture f = withImages("<canvas id=c width=20 height=20></canvas><img id=ok src='same.png'><img id=bad src='missing.png'><img id=none>");
        Check.equal("true,true", js(f, "[document.getElementById('ok').complete, document.getElementById('bad').complete].join()"), "both images ended");
        js(f, "ctx.imageSmoothingEnabled = false; ctx.drawImage(document.getElementById('ok'), 0, 0);");
        Check.equal("255,0,0,255", js(f, "px(0, 0)"), "the left half of the loaded image is red");
        Check.equal("0,0,255,255", js(f, "px(3, 3)"), "the right half is blue");
        Check.equal("InvalidStateError", js(f, "try { ctx.drawImage(document.getElementById('bad'), 0, 0) } catch (e) { e.name }"), "a broken image is an InvalidStateError");
        Check.equal("undefined", js(f, "String(ctx.drawImage(document.getElementById('none'), 0, 0))"), "an img without a source draws nothing and does not fail");
        js(f, "var late = new Image(); late.src = 'same.png'; ctx.drawImage(late, 10, 10);");
        Check.equal("0,0,0,0", js(f, "px(10, 10)"), "an image that has not finished loading draws nothing");
        f.env.scheduler.advance(0);
        js(f, "ctx.drawImage(late, 10, 10);");
        Check.equal("255,0,0,255", js(f, "px(10, 10)"), "after it loaded it is drawn");
        Check.equal("true", js(f, "var again = ctx.getImageData(0, 0, 1, 1); String(again.data[0] === 255)"), "reading the pixels back works after a same-origin image");
        js(f, "ctx.clearRect(0, 0, 20, 20); var pat = ctx.createPattern(document.getElementById('ok'), 'repeat'); ctx.fillStyle = pat; ctx.fillRect(0, 0, 8, 4);");
        Check.equal("255,0,0,255", js(f, "px(4, 0)"), "createPattern takes an img: the tile repeats after 4 pixels");
        Check.equal("null", js(f, "var pending = new Image(); pending.src = 'same.png'; String(ctx.createPattern(pending, 'repeat'))"), "an image that is not complete gives null");
        Check.equal("InvalidStateError", js(f, "try { ctx.createPattern(document.getElementById('bad'), 'repeat') } catch (e) { e.name }"), "a broken image is an error");
    }

    /**
     * The origin-clean rule: cross-origin images taint the canvas, CORS permission keeps it clean, tainting passes on through canvases and
     * patterns, and every way to read pixels back fails with <code>SecurityError</code> until the size is set again.
     * <p>
     * origin-clean 규칙: 교차 오리진 이미지는 캔버스를 오염시키고, CORS 허용은 깨끗하게 유지하며, 오염은 캔버스와 패턴을 통해 전달되고, 화소를 읽어 오는 모든
     * 방법은 크기를 다시 지정할 때까지 <code>SecurityError</code> 로 실패한다.
     */
    public void testTaint() {
        PageFixture f = withImages("<canvas id=c width=20 height=20></canvas><canvas id=d width=20 height=20></canvas><img id=same src='same.png'>"
                + "<img id=foreign src='http://other.test/foreign.png'><img id=open crossorigin src='http://other.test/open.png'>"
                + "<img id=openNoAttr src='http://other.test/open.png'><img id=exact crossorigin src='http://other.test/exact.png'>"
                + "<img id=credentials crossorigin=use-credentials src='http://other.test/open.png'><img id=creds2 crossorigin=use-credentials src='http://other.test/exact.png'>"
                + "<img id=noCors crossorigin src='http://other.test/foreign.png'>");
        js(f, "var dctx = document.getElementById('d').getContext('2d'); function image(id) { return document.getElementById(id); }"
                + " function reads(c) { var r = []; try { c.getContext('2d').getImageData(0, 0, 1, 1); r.push('ok'); } catch (e) { r.push(e.name); }"
                + " try { c.toDataURL(); r.push('ok'); } catch (e) { r.push(e.name); } try { c.toBlob(function () {}); r.push('ok'); } catch (e) { r.push(e.name); } return r.join(); }");
        Check.equal("ok,ok,ok", js(f, "ctx.drawImage(image('same'), 0, 0); reads(cv)"), "a same-origin image does not taint");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "ctx.drawImage(image('foreign'), 0, 0); reads(cv)"), "a cross-origin image taints: no way to read back");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "ctx.clearRect(0, 0, 20, 20); reads(cv)"), "the taint is not removed by clearing");
        Check.equal("ok,ok,ok", js(f, "cv.width = cv.width; reads(cv)"), "setting the size gives a new clean bitmap");
        Check.equal("ok,ok,ok", js(f, "ctx.drawImage(image('open'), 0, 0); reads(cv)"), "crossorigin and Access-Control-Allow-Origin: * keep the canvas clean");
        js(f, "cv.width = cv.width;");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "ctx.drawImage(image('openNoAttr'), 0, 0); reads(cv)"),
                "the same response without the crossorigin attribute taints (the request was not a CORS request)");
        js(f, "cv.width = cv.width;");
        Check.equal("ok,ok,ok", js(f, "ctx.drawImage(image('exact'), 0, 0); reads(cv)"), "an exact origin match with credentials allowed keeps the canvas clean");
        js(f, "cv.width = cv.width;");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "ctx.drawImage(image('credentials'), 0, 0); reads(cv)"),
                "use-credentials does not accept the wildcard origin");
        js(f, "cv.width = cv.width;");
        Check.equal("ok,ok,ok", js(f, "ctx.drawImage(image('creds2'), 0, 0); reads(cv)"), "use-credentials with the exact origin and Allow-Credentials is clean");
        js(f, "cv.width = cv.width;");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "ctx.drawImage(image('noCors'), 0, 0); reads(cv)"), "crossorigin without permission taints (the image would not load in a browser)");
        js(f, "cv.width = cv.width; dctx.drawImage(cv, 0, 0);");
        Check.equal("ok,ok,ok", js(f, "reads(document.getElementById('d'))"), "drawing a clean canvas does not taint");
        js(f, "ctx.drawImage(image('foreign'), 0, 0); dctx.drawImage(cv, 0, 0);");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "reads(document.getElementById('d'))"), "drawing a tainted canvas taints the other canvas");
        js(f, "document.getElementById('d').width = 20; ctx.fillStyle = ctx.createPattern(image('foreign'), 'repeat'); dctx.fillStyle = ctx.createPattern(image('foreign'), 'repeat');");
        Check.equal("ok,ok,ok", js(f, "reads(document.getElementById('d'))"), "creating a pattern from a foreign image does not taint by itself");
        js(f, "dctx.fillRect(0, 0, 5, 5);");
        Check.equal("SecurityError,SecurityError,SecurityError", js(f, "reads(document.getElementById('d'))"), "painting with it does");
        Check.equal("ok,ok,ok", js(f, "var e = document.createElement('canvas'); e.width = 5; e.height = 5; var ectx = e.getContext('2d'); ectx.fillText('x', 0, 4);"
                + " ectx.fillRect(0, 0, 2, 2); reads(e)"), "text and shapes never taint");
        Check.equal("SecurityError", js(f, "try { ctx.getImageData(0, 0, 1, 1) } catch (x) { x.name }"), "the page-level check after the pattern above: the first canvas is still tainted");
    }

    /**
     * <code>toDataURL</code> and <code>toBlob</code> produce decodable PNG and JPEG files; unknown types give PNG, and a canvas without area
     * gives <code>data:,</code>.
     * <p>
     * <code>toDataURL</code> 과 <code>toBlob</code> 은 디코딩할 수 있는 PNG 와 JPEG 파일을 만든다. 모르는 형식은 PNG 이고, 면적이 없는 캔버스는
     * <code>data:,</code> 이다.
     *
     * @throws Exception if decoding fails / 디코딩에 실패하면
     */
    public void testExport() throws Exception {
        PageFixture f = page(8, 6);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 4, 6); ctx.fillStyle = 'rgba(0, 0, 255, 0.5)'; ctx.fillRect(4, 0, 4, 6);");
        String url = js(f, "cv.toDataURL()");
        Check.equal(true, url.startsWith("data:image/png;base64,"), "the default type is PNG");
        BufferedImage png = decode(url);
        Check.equal("8x6", png.getWidth() + "x" + png.getHeight(), "size of the decoded PNG");
        Check.equal(0xFFFF0000, png.getRGB(1, 1), "the red half survives exactly");
        int half = png.getRGB(6, 1);
        Check.equal(true, ((half >>> 24) == 127 || (half >>> 24) == 128) && (half & 0xFFFFFF) == 0x0000FF, "the translucent half keeps its color and alpha ("
                + Integer.toHexString(half) + ")");
        Check.equal(url, js(f, "cv.toDataURL('image/webp')"), "a type that is not supported gives PNG");
        Check.equal(url, js(f, "cv.toDataURL('bogus')"), "so does a type that is not an image type");
        String jpeg = js(f, "cv.toDataURL('image/jpeg', 0.9)");
        Check.equal(true, jpeg.startsWith("data:image/jpeg;base64,/9j/"), "a JPEG data URL");
        BufferedImage j = decode(jpeg);
        int jr = (j.getRGB(1, 1) >> 16) & 0xFF;
        int jb = j.getRGB(1, 1) & 0xFF;
        Check.equal(true, jr > 200 && jb < 60, "the JPEG of the red half is red (" + Integer.toHexString(j.getRGB(1, 1)) + ")");
        Check.equal("data:,", js(f, "var z = document.createElement('canvas'); z.width = 0; z.toDataURL()"), "no area, no file");
        Check.equal("TypeError", js(f, "try { cv.toBlob() } catch (e) { e.name }"), "toBlob needs a callback");
        Check.equal("TypeError", js(f, "try { cv.toBlob(5) } catch (e) { e.name }"), "the callback must be a function");
        js(f, "var got = []; cv.toBlob(function (b) { got.push(b === null ? 'null' : b.type + ':' + (b.size > 20)); }); got.push('sync');");
        Check.equal("sync", js(f, "got.join()"), "the callback is not called synchronously");
        f.env.scheduler.advance(0);
        Check.equal("sync,image/png:true", js(f, "got.join()"), "it gets a Blob of the PNG in a later task");
        js(f, "var got2 = []; cv.toBlob(function (b) { got2.push(b.type); }, 'image/jpeg', 0.5);");
        f.env.scheduler.advance(0);
        Check.equal("image/jpeg", js(f, "got2.join()"), "toBlob with a type");
        js(f, "var got3 = []; z.toBlob(function (b) { got3.push(String(b)); });");
        f.env.scheduler.advance(0);
        Check.equal("null", js(f, "got3.join()"), "a canvas without area gives null");
    }

    /**
     * Decodes a PNG or JPEG data URL with ImageIO.
     * <p>
     * PNG 나 JPEG data URL 을 ImageIO 로 디코딩한다.
     *
     * @param url the data URL / data URL
     * @return the image / 이미지
     * @throws Exception if it is not decodable / 디코딩할 수 없으면
     */
    private static BufferedImage decode(String url) throws Exception {
        String b64 = url.substring(url.indexOf(',') + 1);
        byte[] bytes = new byte[b64.length() * 3 / 4];
        int n = 0;
        int acc = 0;
        int bits = 0;
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        // Base64 문자를 하나씩 읽어 여섯 비트씩 모아 바이트로 만든다.
        for (int i = 0; i < b64.length(); i++) {
            int v = alphabet.indexOf(b64.charAt(i));
            if (v < 0) {
                continue;
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                bytes[n++] = (byte) (acc >> bits);
            }
        }
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes, 0, n));
        Check.equal(true, img != null, "ImageIO can read the file");
        return img;
    }

    /**
     * Setting the size attributes, also to the same value, replaces the bitmap and resets the state of the context, which stays the same
     * object; the limits make huge canvases unusable but harmless.
     * <p>
     * 크기 속성을 (같은 값으로도) 지정하면 비트맵이 교체되고 컨텍스트의 상태가 초기화된다(컨텍스트는 같은 객체). 한도는 거대한 캔버스를 쓸 수 없지만 해롭지
     * 않게 만든다.
     */
    public void testResetAndLimits() {
        PageFixture f = page(10, 10);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.lineWidth = 7; ctx.translate(3, 3); ctx.save(); ctx.fillRect(0, 0, 10, 10); cv.width = 10;");
        Check.equal("0,0,0,0", js(f, "px(5, 5)"), "setting the width to the same value clears the bitmap");
        Check.equal("#000000,1,1,0", js(f, "var m = ctx.getTransform(); [ctx.fillStyle, ctx.lineWidth, m.a, m.e].join()"), "and resets the state of the context");
        js(f, "ctx.restore(); ctx.fillRect(0, 0, 3, 3);");
        Check.equal("0,0,0,255", js(f, "px(1, 1)"), "the saved state is gone, so restore did nothing");
        Check.equal("true", js(f, "String(cv.getContext('2d') === ctx)"), "the context is the same object");
        js(f, "ctx.fillRect(0, 0, 3, 3); cv.setAttribute('height', '10');");
        Check.equal("0,0,0,0", js(f, "px(1, 1)"), "setAttribute resets too");
        js(f, "ctx.fillRect(0, 0, 3, 3); cv.height = 20; cv.width = 15;");
        Check.equal("15,20,0,0,0,0", js(f, "var big = ctx.getImageData(0, 0, 15, 20); [big.width, big.height, big.data[0], big.data[1], big.data[2], big.data[3]].join()"),
                "the bitmap has the new size and is empty");
        Check.equal("15,20", js(f, "[cv.width, cv.height].join()"), "the attributes read back");
        js(f, "cv.width = 20000; cv.height = 20000; ctx.fillRect(0, 0, 10, 10); ctx.drawImage(cv, 0, 0); ctx.fillText('x', 1, 1); ctx.clip(); ctx.putImageData(new ImageData(1, 1), 0, 0);");
        Check.equal("20000,20000", js(f, "[cv.width, cv.height].join()"), "a canvas beyond the limit keeps its attributes and its context");
        Check.equal("InvalidStateError", js(f, "try { ctx.getImageData(0, 0, 1, 1) } catch (e) { e.name }"), "but reading its pixels is an error");
        Check.equal("data:,", js(f, "cv.toDataURL()"), "and it exports nothing");
        js(f, "cv.width = 0; ctx.fillRect(0, 0, 10, 10);");
        Check.equal("0,0,0,0", js(f, "px(0, 0)"), "a canvas without area has no pixels but does not fail (the pixel read is transparent black)");
        Check.equal("true", js(f, "cv.width = 100; cv.height = 100; ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 100, 100); String(px(50, 50) === '0,255,0,255')"),
                "it can be made usable again");
    }

    /**
     * A context created with <code>alpha: false</code> is opaque: it starts black, whatever is drawn is flattened onto black, clearing gives
     * black, and neither scripts nor the renderer see a transparent pixel.
     * <p>
     * <code>alpha: false</code> 로 만든 컨텍스트는 불투명하다: 검정으로 시작하고, 그려진 것은 검정 위에 합쳐지며, 지우면 검정이 되고, 스크립트도 렌더러도 투명한
     * 화소를 보지 못한다.
     */
    public void testOpaqueCanvas() {
        PageFixture f = new PageFixture().load("<canvas id=c width=10 height=10></canvas>");
        f.scripts.evaluate("var cv = document.getElementById('c'); var ctx = cv.getContext('2d', {alpha: false});"
                + "function px(x, y) { var d = ctx.getImageData(x, y, 1, 1).data; return d[0] + ',' + d[1] + ',' + d[2] + ',' + d[3]; }", "helpers");
        Check.equal("0,0,0,255", js(f, "px(5, 5)"), "it starts as opaque black");
        Check.equal("false,true", js(f, "[ctx.getContextAttributes().alpha, cv.getContext('2d', {alpha: true}) === ctx].join()"), "the attribute is read once, at creation");
        js(f, "ctx.fillStyle = 'rgba(255, 0, 0, 0.5)'; ctx.fillRect(0, 0, 5, 10);");
        String half = js(f, "px(2, 5)");
        Check.equal(true, ("127,0,0,255".equals(half) || "128,0,0,255".equals(half)), "translucent red over black stays opaque (" + half + ")");
        js(f, "ctx.clearRect(0, 0, 10, 10);");
        Check.equal("0,0,0,255", js(f, "px(2, 5)"), "clearing gives opaque black");
        js(f, "ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 2, 2);");
        CanvasBindings bindings = (CanvasBindings) f.scripts.canvasSource();
        int[] argb = bindings.getCanvasImage(f.document.getElementById("c")).getFrames().get(0).getArgb();
        Check.equal(0xFF00FF00, argb[0], "the renderer sees the drawing");
        Check.equal(0xFF000000, argb[9 * 10 + 9], "and opaque black elsewhere");
    }

    /**
     * The renderer reads the picture only through the page: after disposal the canvases are gone and drawing is harmless.
     * <p>
     * 렌더러는 페이지를 통해서만 그림을 읽는다. 폐기 뒤에는 캔버스가 사라지고 그리기는 해롭지 않다.
     */
    public void testDisposal() {
        PageFixture f = page(10, 10);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10); var saved = ctx;");
        CanvasBindings bindings = (CanvasBindings) f.scripts.canvasSource();
        Check.equal(1, bindings.surfaceCount(), "one canvas has a surface");
        Check.equal(true, bindings.getCanvasImage(f.document.getElementById("c")) != null, "and a picture");
        f.scripts.dispose();
        Check.equal(0, bindings.surfaceCount(), "disposal drops the surfaces");
        Check.equal(true, bindings.getCanvasImage(f.document.getElementById("c")) == null, "the renderer sees no picture any more");
    }

    /**
     * Creates a page with a drawn canvas, disposes it and returns weak references to the surface and to its picture, without keeping
     * anything else alive.
     * <p>
     * 그려진 캔버스가 있는 페이지를 만들어 폐기하고, 다른 것을 살려 두지 않은 채 표면과 그 그림에 대한 약한 참조를 반환한다.
     *
     * @return the references: the surface and the snapshot / 참조: 표면과 스냅샷
     */
    private static WeakReference<?>[] createAndDispose() {
        PageFixture f = page(200, 100);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 200, 100); ctx.font = '12px serif'; ctx.fillText('x', 5, 5);");
        CanvasBindings bindings = (CanvasBindings) f.scripts.canvasSource();
        Object picture = bindings.getCanvasImage(f.document.getElementById("c"));
        WeakReference<?>[] refs = { new WeakReference<CanvasSurface>(bindings.surfaceOf(f.document.getElementById("c"))), new WeakReference<Object>(picture) };
        f.scripts.dispose();
        return refs;
    }

    /**
     * Nothing outside the page keeps a canvas alive: after the page is disposed and dropped, the surface (with its bitmap) and the
     * snapshot are collected (the collection time itself is up to the garbage collector, so the test asks for it repeatedly).
     * <p>
     * 페이지 밖의 어떤 것도 캔버스를 살려 두지 않는다: 페이지를 폐기하고 버리면 표면(비트맵 포함)과 스냅샷이 수거된다(수거 시점은 가비지 컬렉터에 달려 있으므로
     * 시험은 반복해서 요청한다).
     *
     * @throws Exception if interrupted / 인터럽트되면
     */
    public void testCollectedWithThePage() throws Exception {
        WeakReference<?>[] refs = createAndDispose();
        long end = System.currentTimeMillis() + 20000;
        // 두 참조가 모두 지워질 때까지 수거를 요청하며 기다린다.
        while ((refs[0].get() != null || refs[1].get() != null) && System.currentTimeMillis() < end) {
            System.gc();
            Thread.sleep(50);
        }
        Check.equal(true, refs[0].get() == null, "the surface was collected");
        Check.equal(true, refs[1].get() == null, "the snapshot was collected");
    }
}
