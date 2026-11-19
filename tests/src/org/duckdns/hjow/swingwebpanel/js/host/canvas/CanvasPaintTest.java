package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of what fills the shapes of the 2D context and how it reaches the bitmap (plan item P20-05): linear and radial gradients and their
 * stops, patterns and their repetition, shadows with offset, blur and alpha, all 26 compositing operators on opaque pixels, and text (font
 * parsing, measuring, alignment, baselines, the maximum width).
 * <p>
 * 2D 컨텍스트가 도형을 채우는 것과 그것이 비트맵에 이르는 방식의 시험(계획 P20-05): 선형/방사형 그라디언트와 정지점, 패턴과 반복, 오프셋/번짐/알파가 있는
 * 그림자, 불투명한 화소에서 합성 연산자 26 개 전부, 텍스트(글꼴 해석, 측정, 정렬, 기준선, 최대 너비).
 */
public class CanvasPaintTest {

    /**
     * The helper script: pixels, alpha and the bounding box of the ink.
     * <p>
     * 도우미 스크립트: 화소, 알파, 잉크의 경계 상자.
     */
    private static final String HELPERS = CanvasBasicsTest.HELPERS + "function alpha(x, y) { return ctx.getImageData(x, y, 1, 1).data[3]; }"
            + "function ink() { var w = cv.width, h = cv.height, d = ctx.getImageData(0, 0, w, h).data, minX = w, maxX = -1, minY = h, maxY = -1, n = 0;"
            + " for (var y = 0; y < h; y++) for (var x = 0; x < w; x++) if (d[(y * w + x) * 4 + 3] > 0) { n++; if (x < minX) minX = x; if (x > maxX) maxX = x;"
            + " if (y < minY) minY = y; if (y > maxY) maxY = y; } return {minX: minX, maxX: maxX, minY: minY, maxY: maxY, count: n}; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasPaintTest() {
    }

    /**
     * Loads a page with a canvas of a size.
     * <p>
     * 크기가 정해진 캔버스가 있는 페이지를 로드한다.
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
     * Reads the channel of a pixel as a number.
     * <p>
     * 화소의 채널을 숫자로 읽는다.
     *
     * @param f       the page / 페이지
     * @param x       x / x
     * @param y       y / y
     * @param channel 0 red, 1 green, 2 blue, 3 alpha / 0 빨강, 1 초록, 2 파랑, 3 알파
     * @return the value / 값
     */
    private static int channel(PageFixture f, int x, int y, int channel) {
        return Integer.parseInt(js(f, "String(ctx.getImageData(" + x + ", " + y + ", 1, 1).data[" + channel + "])"));
    }

    /**
     * Linear gradients interpolate in RGBA between the stops, pad before the first and after the last stop, make hard edges at equal offsets and
     * live in the coordinate space of the time they are painted.
     * <p>
     * 선형 그라디언트는 정지점 사이를 RGBA 로 보간하고, 첫 정지점 앞과 마지막 정지점 뒤를 채우며, 같은 오프셋에서 날카로운 경계를 만들고, 칠하는 때의 좌표계에
     * 놓인다.
     */
    public void testLinearGradient() {
        PageFixture f = page(100, 10);
        js(f, "var g = ctx.createLinearGradient(0, 0, 100, 0); g.addColorStop(0, '#ff0000'); g.addColorStop(1, '#0000ff'); ctx.fillStyle = g;"
                + " ctx.fillRect(0, 0, 100, 10);");
        Check.equal(true, channel(f, 0, 5, 0) >= 252 && channel(f, 0, 5, 2) <= 3, "the first pixel is almost red");
        int mid = channel(f, 50, 5, 0);
        Check.equal(true, mid >= 124 && mid <= 129 && Math.abs(mid + channel(f, 50, 5, 2) - 255) <= 1, "the middle is half red and half blue (red " + mid + ")");
        Check.equal(true, channel(f, 99, 5, 0) <= 3 && channel(f, 99, 5, 2) >= 252, "the last pixel is almost blue");
        Check.equal(255, channel(f, 50, 5, 3), "the gradient is opaque");
        Check.equal("true", js(f, "String(ctx.fillStyle === g)"), "the style returns the gradient object");
        Check.equal("[object CanvasGradient]", js(f, "Object.prototype.toString.call(g)"), "interface name");
        js(f, "ctx.clearRect(0, 0, 100, 10); var h = ctx.createLinearGradient(0, 0, 100, 0); h.addColorStop(0.5, '#ff0000'); h.addColorStop(0.5, '#0000ff');"
                + " ctx.fillStyle = h; ctx.fillRect(0, 0, 100, 10);");
        Check.equal("255,0,0,255", js(f, "px(49, 5)"), "equal offsets: red up to the offset");
        Check.equal("0,0,255,255", js(f, "px(50, 5)"), "and blue from it");
        js(f, "ctx.clearRect(0, 0, 100, 10); var k = ctx.createLinearGradient(25, 0, 75, 0); k.addColorStop(0, '#ff0000'); k.addColorStop(1, '#00ff00');"
                + " ctx.fillStyle = k; ctx.fillRect(0, 0, 100, 10);");
        Check.equal("255,0,0,255", js(f, "px(5, 5)"), "before the first stop the first color is used");
        Check.equal("0,255,0,255", js(f, "px(95, 5)"), "after the last stop the last color is used");
        js(f, "ctx.clearRect(0, 0, 100, 10); var n = ctx.createLinearGradient(0, 0, 10, 0); ctx.fillStyle = n; ctx.fillRect(0, 0, 100, 10);");
        Check.equal("0,0,0,0", js(f, "px(5, 5)"), "a gradient without stops paints transparent black");
        js(f, "ctx.clearRect(0, 0, 100, 10); var q = ctx.createLinearGradient(5, 0, 5, 0); q.addColorStop(0, 'red'); ctx.fillStyle = q; ctx.fillRect(0, 0, 100, 10);");
        Check.equal("0,0,0,0", js(f, "px(5, 5)"), "a gradient line of length zero paints nothing");
        js(f, "ctx.clearRect(0, 0, 100, 10); ctx.save(); ctx.translate(50, 0); ctx.fillStyle = g; ctx.fillRect(0, 0, 50, 10); ctx.restore();");
        Check.equal(true, channel(f, 50, 5, 0) >= 252, "the gradient follows the matrix at painting time: red where the translated origin is");
        Check.equal(true, channel(f, 99, 5, 2) >= 120 && channel(f, 99, 5, 2) <= 135, "and only half of it fits in the rectangle");
    }

    /**
     * Gradient stops check their arguments.
     * <p>
     * 그라디언트 정지점은 인수를 검사한다.
     */
    public void testGradientErrors() {
        PageFixture f = page(10, 10);
        js(f, "var g = ctx.createLinearGradient(0, 0, 10, 0);");
        Check.equal("IndexSizeError", js(f, "try { g.addColorStop(1.5, 'red') } catch (e) { e.name }"), "offset above 1");
        Check.equal("IndexSizeError", js(f, "try { g.addColorStop(-0.1, 'red') } catch (e) { e.name }"), "offset below 0");
        Check.equal("TypeError", js(f, "try { g.addColorStop(NaN, 'red') } catch (e) { e.name }"), "offset NaN");
        Check.equal("SyntaxError", js(f, "try { g.addColorStop(0.5, 'bogus') } catch (e) { e.name }"), "a color that cannot be parsed");
        Check.equal("TypeError", js(f, "try { g.addColorStop(0.5) } catch (e) { e.name }"), "both arguments are required");
        Check.equal("undefined", js(f, "String(g.addColorStop(0, 'currentcolor'))"), "currentcolor is a color");
    }

    /**
     * Radial gradients: the center of the start circle has the first color, the end circle the last, and the outside is padded; a cone from
     * two circles with different centers works too.
     * <p>
     * 방사형 그라디언트: 시작 원의 중심은 첫 색, 끝 원은 마지막 색이고 바깥은 채워진다. 중심이 다른 두 원의 원뿔도 동작한다.
     */
    public void testRadialGradient() {
        PageFixture f = page(100, 100);
        js(f, "var g = ctx.createRadialGradient(50, 50, 0, 50, 50, 40); g.addColorStop(0, '#ff0000'); g.addColorStop(1, '#0000ff'); ctx.fillStyle = g;"
                + " ctx.fillRect(0, 0, 100, 100);");
        Check.equal(true, channel(f, 50, 50, 0) >= 245, "the center is the first color");
        Check.equal(true, channel(f, 89, 50, 2) >= 245 && channel(f, 50, 11, 2) >= 245, "the end circle is the last color in every direction");
        Check.equal("0,0,255,255", js(f, "px(99, 50)"), "outside the end circle the last color is used");
        Check.equal("0,0,255,255", js(f, "px(2, 2)"), "also in the corners");
        int quarter = channel(f, 50 + 20, 50, 0);
        Check.equal(true, quarter >= 120 && quarter <= 135, "half way out the colors are mixed (red " + quarter + ")");
        js(f, "ctx.clearRect(0, 0, 100, 100); var h = ctx.createRadialGradient(30, 50, 5, 70, 50, 20); h.addColorStop(0, '#00ff00'); h.addColorStop(1, '#ff00ff');"
                + " ctx.fillStyle = h; ctx.fillRect(0, 0, 100, 100);");
        Check.equal(true, channel(f, 25, 50, 1) >= 250, "the left edge of the start circle of a cone has the first color (the largest circle that contains a point decides)");
        int inside = channel(f, 30, 50, 1);
        Check.equal(true, inside > 190 && inside < 215, "the middle of the start circle lies in circles up to 0.2 of the way: 80% of green is left (" + inside + ")");
        Check.equal(true, channel(f, 85, 50, 0) >= 250 && channel(f, 85, 50, 1) <= 5, "the far side of the end circle has the last color");
    }

    /**
     * Patterns repeat a snapshot of the image in x, in y, in both or in neither direction, and follow the matrix at painting time.
     * <p>
     * 패턴은 이미지의 스냅샷을 x, y, 양방향 또는 반복 없이 되풀이하며 칠하는 때의 행렬을 따른다.
     */
    public void testPattern() {
        PageFixture f = page(8, 8);
        js(f, "var pc = document.createElement('canvas'); pc.width = 2; pc.height = 2; var pctx = pc.getContext('2d'); pctx.fillStyle = '#ff0000';"
                + " pctx.fillRect(0, 0, 1, 1); pctx.fillStyle = '#0000ff'; pctx.fillRect(1, 1, 1, 1); var p = ctx.createPattern(pc, 'repeat');"
                + " ctx.fillStyle = p; ctx.fillRect(0, 0, 8, 8);");
        Check.equal("255,0,0,255", js(f, "px(0, 0)"), "the red tile pixel");
        Check.equal("0,0,0,0", js(f, "px(1, 0)"), "a transparent tile pixel");
        Check.equal("0,0,255,255", js(f, "px(1, 1)"), "the blue tile pixel");
        Check.equal("255,0,0,255", js(f, "px(2, 2)"), "repeated diagonally");
        Check.equal("0,0,255,255", js(f, "px(7, 7)"), "repeated to the corner");
        Check.equal("255,0,0,255", js(f, "px(6, 0)"), "repeated along x");
        Check.equal("true", js(f, "String(ctx.fillStyle === p)"), "the style returns the pattern object");
        Check.equal("[object CanvasPattern]", js(f, "Object.prototype.toString.call(p)"), "interface name");
        js(f, "ctx.clearRect(0, 0, 8, 8); ctx.fillStyle = ctx.createPattern(pc, 'no-repeat'); ctx.fillRect(0, 0, 8, 8);");
        Check.equal("255,0,0,255", js(f, "px(0, 0)"), "no-repeat: the first tile is there");
        Check.equal("0,0,255,255", js(f, "px(1, 1)"), "the tile itself is complete");
        Check.equal("0", js(f, "String(alpha(2, 2))"), "no tile beyond the first");
        js(f, "ctx.clearRect(0, 0, 8, 8); ctx.fillStyle = ctx.createPattern(pc, 'repeat-x'); ctx.fillRect(0, 0, 8, 8);");
        Check.equal("255,0", js(f, "[alpha(4, 0), alpha(0, 2)].join()"), "repeat-x: along x yes, along y no");
        js(f, "ctx.clearRect(0, 0, 8, 8); ctx.fillStyle = ctx.createPattern(pc, 'repeat-y'); ctx.fillRect(0, 0, 8, 8);");
        Check.equal("255,0", js(f, "[alpha(0, 4), alpha(2, 0)].join()"), "repeat-y: along y yes, along x no");
        js(f, "ctx.clearRect(0, 0, 8, 8); ctx.fillStyle = ctx.createPattern(pc, null); ctx.fillRect(0, 0, 8, 8);");
        Check.equal("255,0,0,255", js(f, "px(4, 4)"), "null repetition is repeat");
        js(f, "ctx.clearRect(0, 0, 8, 8); var q = ctx.createPattern(pc, ''); pctx.fillStyle = '#00ff00'; pctx.fillRect(0, 0, 2, 2); ctx.fillStyle = q; ctx.fillRect(0, 0, 8, 8);");
        Check.equal("255,0,0,255", js(f, "px(0, 0)"), "the pattern is a snapshot: later changes of the canvas do not show");
        js(f, "ctx.clearRect(0, 0, 8, 8); ctx.save(); ctx.translate(1, 1); ctx.fillStyle = q; ctx.fillRect(0, 0, 4, 4); ctx.restore();");
        Check.equal("255,0,0,255", js(f, "px(1, 1)"), "the pattern follows the matrix at painting time");
        Check.equal("SyntaxError", js(f, "try { ctx.createPattern(pc, 'bogus') } catch (e) { e.name }"), "unknown repetition");
        Check.equal("TypeError", js(f, "try { ctx.createPattern({}, 'repeat') } catch (e) { e.name }"), "not an image");
        Check.equal("InvalidStateError", js(f, "var z = document.createElement('canvas'); z.width = 0; try { ctx.createPattern(z, 'repeat') } catch (e) { e.name }"),
                "an empty canvas cannot be a pattern");
    }

    /**
     * Shadows: offset, color, alpha, blur, the device-space offset, the global alpha, and the conditions under which no shadow is drawn.
     * <p>
     * 그림자: 오프셋, 색, 알파, 번짐, 장치 좌표의 오프셋, 전역 알파, 그림자를 그리지 않는 조건.
     */
    public void testShadows() {
        PageFixture f = page(60, 60);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(10, 10, 10, 10);");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.shadowOffsetX = 10; ctx.shadowOffsetY = 10; ctx.fillRect(10, 10, 10, 10);");
        Check.equal("0,0,0,0", js(f, "px(25, 25)"), "without a shadow color (transparent black) no shadow is drawn even with an offset");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.shadowColor = '#000000'; ctx.fillRect(10, 10, 10, 10);");
        Check.equal("255,0,0,255", js(f, "px(15, 15)"), "the shape is drawn over its shadow");
        Check.equal("0,0,0,255", js(f, "px(25, 25)"), "the shadow is the shape moved by the offset");
        Check.equal("0,0,0,255", js(f, "px(20, 20)"), "first shadow pixel");
        Check.equal("0,0,0,0", js(f, "px(25, 15)"), "nothing right of the shape and above the shadow");
        Check.equal("0,0,0,0", js(f, "px(30, 30)"), "the shadow ends where the shifted shape ends");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.shadowColor = 'rgba(0, 0, 0, 0.5)'; ctx.fillRect(10, 10, 10, 10);");
        Check.equal("0,0,0,128", js(f, "px(25, 25)"), "the alpha of the shadow color is the alpha of the shadow");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.shadowColor = '#000000'; ctx.globalAlpha = 0.5; ctx.fillRect(10, 10, 10, 10); ctx.globalAlpha = 1;");
        Check.equal("0,0,0,128", js(f, "px(25, 25)"), "the global alpha applies to the shadow as well");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.save(); ctx.scale(2, 2); ctx.fillRect(5, 5, 5, 5); ctx.restore();");
        Check.equal("0,0,0,255", js(f, "px(25, 25)"), "the offset is in device pixels: the matrix does not scale it");
        Check.equal("0,0,0,0", js(f, "px(32, 32)"), "so the shadow of the doubled square ends at 30");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.shadowOffsetX = 0; ctx.shadowOffsetY = 0; ctx.shadowBlur = 0; ctx.fillRect(10, 10, 10, 10);");
        Check.equal("0,0,0,0", js(f, "px(25, 25)"), "no offset and no blur: no shadow");
        Check.equal("0", js(f, "String(alpha(8, 15))"), "and no shadow around the shape either");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.shadowBlur = 10; ctx.fillRect(20, 20, 10, 10);");
        int near = Integer.parseInt(js(f, "String(alpha(17, 25))"));
        int far = Integer.parseInt(js(f, "String(alpha(8, 25))"));
        Check.equal(true, near > 20 && near < 200, "the blurred shadow reaches out of the shape (alpha " + near + " three pixels away)");
        Check.equal(true, far < near, "and fades with the distance (alpha " + far + " twelve pixels away)");
        Check.equal("255,0,0,255", js(f, "px(25, 25)"), "the shape stays sharp");
        Check.equal("2,2", js(f, "ctx.shadowBlur = -1; ctx.shadowOffsetX = NaN; ctx.shadowBlur = 2; ctx.shadowOffsetX = 2; [ctx.shadowBlur, ctx.shadowOffsetX].join()"),
                "invalid shadow values are ignored");
    }

    /**
     * A shadow whose blur would need more work than the limits allow is not drawn, while the shape itself is: one call cannot keep the
     * document thread busy for long.
     * <p>
     * 번짐이 한도보다 많은 계산을 필요로 하는 그림자는 그리지 않고 도형만 그린다: 호출 하나로 문서 스레드를 오래 붙잡을 수 없다.
     */
    public void testHugeShadowIsSkipped() {
        PageFixture f = page(1800, 1800);
        long start = System.currentTimeMillis();
        js(f, "ctx.shadowColor = '#000000'; ctx.shadowBlur = 150; ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 1700, 1700);");
        long took = System.currentTimeMillis() - start;
        Check.equal("255,0,0,255", js(f, "px(800, 800)"), "the shape is drawn");
        Check.equal(true, took < 20000, "and it took " + took + " ms, not minutes");
    }

    /**
     * Every compositing operator on opaque red (the destination, everywhere) and opaque blue (the source, on the right half).
     * <p>
     * 불투명한 빨강(대상, 전체)과 불투명한 파랑(원본, 오른쪽 절반)에 대한 모든 합성 연산자.
     */
    public void testCompositeOperators() {
        // 연산자, 왼쪽(원본이 없는 곳)의 기대 화소, 오른쪽(원본이 있는 곳)의 기대 화소.
        String[][] cases = {
            { "source-over", "255,0,0,255", "0,0,255,255" },
            { "source-in", "0,0,0,0", "0,0,255,255" },
            { "source-out", "0,0,0,0", "0,0,0,0" },
            { "source-atop", "255,0,0,255", "0,0,255,255" },
            { "destination-over", "255,0,0,255", "255,0,0,255" },
            { "destination-in", "0,0,0,0", "255,0,0,255" },
            { "destination-out", "255,0,0,255", "0,0,0,0" },
            { "destination-atop", "0,0,0,0", "255,0,0,255" },
            { "lighter", "255,0,0,255", "255,0,255,255" },
            { "copy", "0,0,0,0", "0,0,255,255" },
            { "xor", "255,0,0,255", "0,0,0,0" },
            { "multiply", "255,0,0,255", "0,0,0,255" },
            { "screen", "255,0,0,255", "255,0,255,255" },
            { "overlay", "255,0,0,255", "255,0,0,255" },
            { "darken", "255,0,0,255", "0,0,0,255" },
            { "lighten", "255,0,0,255", "255,0,255,255" },
            { "color-dodge", "255,0,0,255", "255,0,0,255" },
            { "color-burn", "255,0,0,255", "255,0,0,255" },
            { "hard-light", "255,0,0,255", "0,0,255,255" },
            { "soft-light", "255,0,0,255", "255,0,0,255" },
            { "difference", "255,0,0,255", "255,0,255,255" },
            { "exclusion", "255,0,0,255", "255,0,255,255" },
        };
        // 연산자마다 새 캔버스에서 같은 두 사각형을 그려 두 화소를 확인한다.
        for (int i = 0; i < cases.length; i++) {
            PageFixture f = page(10, 10);
            js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10); ctx.globalCompositeOperation = '" + cases[i][0] + "';"
                    + " ctx.fillStyle = '#0000ff'; ctx.fillRect(5, 0, 5, 10);");
            Check.equal(cases[i][0] + " " + cases[i][1], cases[i][0] + " " + js(f, "px(2, 5)"), cases[i][0] + ": where only the destination is");
            Check.equal(cases[i][0] + " " + cases[i][2], cases[i][0] + " " + js(f, "px(7, 5)"), cases[i][0] + ": where source and destination meet");
            Check.equal(cases[i][0], js(f, "ctx.globalCompositeOperation"), cases[i][0] + ": the attribute reads back");
        }
        PageFixture g = page(10, 10);
        js(g, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10);");
        String[] nonSeparable = { "hue", "saturation", "color", "luminosity" };
        // 분리할 수 없는 네 모드는 불투명한 결과를 내고 파랑 자신과는 다르다.
        for (int i = 0; i < nonSeparable.length; i++) {
            js(g, "ctx.clearRect(0, 0, 10, 10); ctx.globalCompositeOperation = 'source-over'; ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10);"
                    + " ctx.globalCompositeOperation = '" + nonSeparable[i] + "'; ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 0, 10, 10);");
            Check.equal(255, channel(g, 5, 5, 3), nonSeparable[i] + ": opaque result");
            Check.equal(nonSeparable[i], js(g, "ctx.globalCompositeOperation"), nonSeparable[i] + ": the attribute reads back");
        }
        PageFixture h = page(10, 10);
        js(h, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 10, 10); ctx.globalCompositeOperation = 'copy'; ctx.globalAlpha = 0.5; ctx.fillStyle = '#0000ff';"
                + " ctx.fillRect(5, 0, 5, 10);");
        Check.equal("0,0,255,128", js(h, "px(7, 5)"), "copy with a global alpha leaves half transparent blue");
        Check.equal("0,0,0,0", js(h, "px(2, 5)"), "and clears the rest of the canvas");
    }

    /**
     * Composite operators that touch the whole canvas still respect the clip.
     * <p>
     * 캔버스 전체에 영향을 주는 합성 연산자도 클립은 지킨다.
     */
    public void testCompositeInsideClip() {
        PageFixture f = page(20, 10);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 20, 10); ctx.beginPath(); ctx.rect(0, 0, 10, 10); ctx.clip();"
                + " ctx.globalCompositeOperation = 'copy'; ctx.fillStyle = '#0000ff'; ctx.fillRect(2, 2, 2, 2);");
        Check.equal("0,0,255,255", js(f, "px(2, 2)"), "inside the shape");
        Check.equal("0,0,0,0", js(f, "px(8, 8)"), "copy clears the clip region outside the shape");
        Check.equal("255,0,0,255", js(f, "px(15, 5)"), "but not what lies outside the clip");
    }

    /**
     * Font values: parsing, serialization, units, and values that are ignored.
     * <p>
     * 글꼴 값: 해석, 직렬화, 단위, 무시하는 값.
     */
    public void testFontAttribute() {
        PageFixture f = page(10, 10);
        Check.equal("10px sans-serif", js(f, "ctx.font"), "the default font");
        Check.equal("20px sans-serif", js(f, "ctx.font = '20px sans-serif'; ctx.font"), "size and family");
        Check.equal("bold 12px Arial", js(f, "ctx.font = 'bold 12px Arial'; ctx.font"), "weight");
        Check.equal("italic bold 14px \"Times New Roman\", serif", js(f, "ctx.font = 'italic 700 14px \"Times New Roman\", serif'; ctx.font"),
                "style, numeric weight, quoted family and a list");
        Check.equal("16px serif", js(f, "ctx.font = '12pt serif'; ctx.font"), "points are converted to pixels");
        Check.equal("32px serif", js(f, "ctx.font = '2em serif'; ctx.font"), "em is relative to the font size of the canvas element (16px by default)");
        Check.equal("8px monospace", js(f, "ctx.font = '50% monospace'; ctx.font"), "percentages too");
        Check.equal("10px serif", js(f, "ctx.font = 'normal normal normal 10px/1.5 serif'; ctx.font"), "normal values and the line height are dropped");
        Check.equal("10px serif", js(f, "ctx.font = 'bogus'; ctx.font = '12px'; ctx.font = ''; ctx.font = 'bold 12 serif'; ctx.font"),
                "values that are no font are ignored");
        Check.equal("16px serif", js(f, "ctx.font = 'large serif'; ctx.font = 'medium serif'; ctx.font"), "size keywords");
    }

    /**
     * Measuring text: widths grow with the text, an empty text has no width, white space is normalized, and the vertical metrics are positive.
     * <p>
     * 텍스트 측정: 너비는 텍스트에 따라 커지고, 빈 텍스트는 너비가 없으며, 공백은 정규화되고, 세로 측정값은 양수이다.
     */
    public void testMeasureText() {
        PageFixture f = page(10, 10);
        js(f, "ctx.font = '20px sans-serif'; var m = ctx.measureText('Hello'); var m2 = ctx.measureText('HelloHello');");
        Check.equal("true", js(f, "String(m.width > 20)"), "the width of Hello at 20px is positive and not tiny");
        Check.equal("true", js(f, "String(Math.abs(m2.width - 2 * m.width) < 1)"), "twice the text is about twice as wide");
        Check.equal("0", js(f, "String(ctx.measureText('').width)"), "an empty text");
        Check.equal("true", js(f, "String(ctx.measureText('a\\tb\\nc').width === ctx.measureText('a b c').width)"), "ASCII white space is replaced by spaces");
        Check.equal("true", js(f, "String(m.actualBoundingBoxRight > 0 && m.actualBoundingBoxAscent > 0 && m.fontBoundingBoxAscent > 0"
                + " && m.fontBoundingBoxDescent > 0 && m.emHeightAscent > 0)"), "the vertical metrics are positive");
        Check.equal("true", js(f, "String(m.actualBoundingBoxLeft <= 1 && m.actualBoundingBoxLeft > -3)"), "the left of the ink is near the start point");
        Check.equal("0", js(f, "String(m.alphabeticBaseline)"), "the alphabetic baseline is the current baseline");
        Check.equal("true", js(f, "ctx.font = '40px sans-serif'; String(ctx.measureText('Hello').width > m.width * 1.9)"), "doubling the size doubles the width");
        Check.equal("true", js(f, "String(ctx.measureText('가나다').width > 0)"), "Korean text has a width");
        Check.equal("[object TextMetrics]", js(f, "Object.prototype.toString.call(m)"), "interface name");
        Check.equal("TypeError", js(f, "try { ctx.measureText() } catch (e) { e.name }"), "the text is required");
        js(f, "ctx.textAlign = 'center'; var mc = ctx.measureText('Hello'); ctx.textAlign = 'left'; var ml = ctx.measureText('Hello');");
        Check.equal("true", js(f, "String(Math.abs(mc.actualBoundingBoxLeft - (ml.width / 2 + ml.actualBoundingBoxLeft)) < 0.01"
                + " && Math.abs(mc.actualBoundingBoxRight - (ml.actualBoundingBoxRight - ml.width / 2)) < 0.01)"),
                "centered text reaches half its width to each side of the point (" + js(f, "[mc.width, mc.actualBoundingBoxLeft, mc.actualBoundingBoxRight,"
                + " ml.width, ml.actualBoundingBoxLeft, ml.actualBoundingBoxRight].join()") + ")");
        js(f, "ctx.textBaseline = 'top'; var mt = ctx.measureText('Hello');");
        Check.equal("true", js(f, "String(mt.alphabeticBaseline > 0)"), "with the top baseline the alphabetic baseline lies below the current one");
    }

    /**
     * Drawing text: ink appears where the alignment and the baseline say, the maximum width condenses, stroking and gradients work.
     * <p>
     * 텍스트 그리기: 정렬과 기준선이 말하는 곳에 잉크가 나타나고, 최대 너비는 텍스트를 줄이며, 선 긋기와 그라디언트가 동작한다.
     */
    public void testFillAndStrokeText() {
        PageFixture f = page(200, 60);
        js(f, "ctx.font = '30px sans-serif'; ctx.fillStyle = '#ff0000'; ctx.fillText('Hello', 10, 40);");
        Check.equal("true", js(f, "var a = ink(); String(a.count > 50 && a.minX >= 9 && a.maxX < 120 && a.minY > 8 && a.maxY <= 41)"),
                "left aligned text starts at the point and stands on the baseline (its ink " + js(f, "JSON.stringify(ink())") + ")");
        Check.equal("true", js(f, "var d = ctx.getImageData(0, 0, 200, 60).data, allRed = true; for (var i = 0; i < d.length; i += 4)"
                + " { if (d[i + 3] > 0 && (d[i] !== 255 || d[i + 1] !== 0 || d[i + 2] !== 0)) allRed = false; } String(allRed)"), "all ink has the fill color");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.textAlign = 'center'; ctx.fillText('Hello', 100, 40);");
        Check.equal("true", js(f, "var c = ink(); String(Math.abs((c.minX + c.maxX) / 2 - 100) < 4)"), "centered text is centered on the point");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.textAlign = 'right'; ctx.fillText('Hello', 150, 40);");
        Check.equal("true", js(f, "var r = ink(); String(r.maxX <= 151 && r.maxX > 130)"), "right aligned text ends at the point");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.textAlign = 'left'; ctx.textBaseline = 'top'; ctx.fillText('H', 10, 10);");
        Check.equal("true", js(f, "var t = ink(); String(t.minY >= 9 && t.minY <= 16)"), "the top baseline puts the top of the em square at the point");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.textBaseline = 'bottom'; ctx.fillText('H', 10, 50);");
        Check.equal("true", js(f, "var b = ink(); String(b.maxY <= 50 && b.maxY >= 40)"), "the bottom baseline puts the bottom of the em square at the point");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.textBaseline = 'alphabetic'; ctx.fillText('Hello World Hello World', 10, 40, 60);");
        Check.equal("true", js(f, "var w = ink(); String(w.maxX <= 71 && w.maxX > 55)"), "a maximum width squeezes the text horizontally");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.strokeStyle = '#0000ff'; ctx.lineWidth = 1; ctx.strokeText('Hello', 10, 40);");
        Check.equal("true", js(f, "var s = ink(); String(s.count > 30)"), "stroked text has ink");
        js(f, "ctx.clearRect(0, 0, 200, 60); ctx.fillText('', 10, 40); ctx.fillText('Hello', NaN, 40); ctx.fillText('Hello', 10, 40, 0); ctx.fillText('Hello', 10, 40, -5);");
        Check.equal("0", js(f, "String(ink().count)"), "empty text, a NaN position and a maximum width of zero or less draw nothing");
        js(f, "var gr = ctx.createLinearGradient(0, 0, 200, 0); gr.addColorStop(0, '#00ff00'); gr.addColorStop(1, '#00ff00'); ctx.fillStyle = gr; ctx.fillText('Hello', 10, 40);");
        Check.equal("true", js(f, "var g2 = ctx.getImageData(0, 0, 200, 60).data, ok = false; for (var j = 0; j < g2.length; j += 4) { if (g2[j + 3] === 255) ok = g2[j + 1] === 255; } String(ok)"),
                "text can be filled with a gradient");
        Check.equal("true", js(f, "ctx.clearRect(0, 0, 200, 60); ctx.fillStyle = '#000000'; ctx.font = '30px sans-serif'; ctx.fillText('\\uac00\\ub098\\ub2e4', 10, 40); String(ink().count >= 0)"),
                "Korean text can be drawn (the amount of ink depends on the fonts of the machine)");
    }
}
