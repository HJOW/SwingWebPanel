package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting of images, animation frames, background images and the replacement drawings for images, audio and video that
 * cannot be shown (plan items P09-05, P18-06, P18-09 display part).
 * <p>
 * 이미지, 애니메이션 프레임, 배경 이미지, 보여 줄 수 없는 이미지/오디오/비디오의 대체 그림 그리기 시험(계획 P09-05, P18-06, P18-09 표시 부분).
 */
public class ImagePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImagePaintTest() {
    }

    /**
     * Finds the first command of a type.
     * <p>
     * 종류가 같은 첫 명령을 찾는다.
     *
     * @param p the page / 페이지
     * @param t the type / 종류
     * @return the command or null / 명령 또는 null
     */
    private static DisplayCommand find(RenderedPage p, DisplayCommand.Type t) {
        DisplayList l = p.getDisplayList();
        // 명령을 순서대로 보며 종류가 같은 것을 찾는다.
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getType() == t) {
                return l.get(i);
            }
        }
        return null;
    }

    /**
     * A loaded image is painted at its natural size, or scaled to the width and height attributes.
     * <p>
     * 읽은 이미지는 자연 크기로, 또는 width/height 속성에 맞춰 그려진다.
     */
    public void testLoadedImage() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/dir/a.png",
                ImageResource.loaded(RenderTestSupport.solid(4, 4, 0xFFFF0000)));
        RenderedPage natural = RenderTestSupport.page("<img src='a.png'>", 60, 30, src);
        BufferedImage a = RenderTestSupport.paint(natural, 60, 30);
        org.duckdns.hjow.swingwebpanel.layout.LayoutBox nb = RenderTestSupport.firstBox(natural, "img");
        int nx = (int) nb.getX();
        int ny = (int) Math.round(nb.getY());
        Check.equal(4.0, nb.getWidth(), "the layout uses the natural width");
        Check.equal(0xFF0000, RenderTestSupport.rgb(a, nx + 2, ny + 2), "natural size: red inside");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(a, nx + 6, ny + 2), "natural size: white outside (4px wide)");
        RenderedPage scaled = RenderTestSupport.page("<img src='a.png' width='20' height='10'>", 60, 30, src);
        BufferedImage b = RenderTestSupport.paint(scaled, 60, 30);
        org.duckdns.hjow.swingwebpanel.layout.LayoutBox sb = RenderTestSupport.firstBox(scaled, "img");
        int sx = (int) sb.getX();
        int sy = (int) Math.round(sb.getY());
        Check.equal(0xFF0000, RenderTestSupport.rgb(b, sx + 15, sy + 5), "scaled: red inside");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(b, sx + 25, sy + 5), "scaled: white outside");
        Check.notNull(find(scaled, DisplayCommand.Type.IMAGE), "an IMAGE command exists");
    }

    /**
     * The frame of an animation is chosen by the animation time.
     * <p>
     * 애니메이션의 프레임은 애니메이션 시각으로 고른다.
     */
    public void testAnimationFrames() {
        List<DecodedFrame> frames = new ArrayList<DecodedFrame>();
        int[] red = new int[16];
        int[] blue = new int[16];
        java.util.Arrays.fill(red, 0xFFFF0000);
        java.util.Arrays.fill(blue, 0xFF0000FF);
        frames.add(new DecodedFrame(red, 100));
        frames.add(new DecodedFrame(blue, 100));
        DecodedImage anim = new DecodedImage(4, 4, frames, 0);
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/dir/anim.gif", ImageResource.loaded(anim));
        RenderedPage p = RenderTestSupport.page("<img src='anim.gif'>", 20, 20, src);
        int[] times = { 0, 99, 100, 150, 199, 200, 250, 5000 };
        int[] expected = { 0xFF0000, 0xFF0000, 0x0000FF, 0x0000FF, 0x0000FF, 0xFF0000, 0xFF0000, 0xFF0000 };
        // 시각마다 보이는 프레임의 색을 확인한다(무한 반복).
        for (int i = 0; i < times.length; i++) {
            BufferedImage img = RenderTestSupport.blank(20, 20);
            java.awt.Graphics2D g = img.createGraphics();
            p.paint(g, new java.awt.Rectangle(0, 0, 20, 20), 0, 0, 1.0, times[i]);
            g.dispose();
            org.duckdns.hjow.swingwebpanel.layout.LayoutBox ab = RenderTestSupport.firstBox(p, "img");
            Check.equal(expected[i], RenderTestSupport.rgb(img, (int) ab.getX() + 1, (int) Math.round(ab.getY()) + 1), "frame at " + times[i] + " ms");
        }
        DecodedImage once = new DecodedImage(4, 4, frames, 1);
        Check.same(frames.get(1), DisplayCommands.frameAt(once, 100000), "an animation that plays once stays on its last frame");
    }

    /**
     * A failed image is replaced by a drawing with the unsupported URL and the alt text; a pending image paints nothing.
     * <p>
     * 실패한 이미지는 지원하지 않는 URL 과 alt 텍스트가 있는 그림으로 대체되고, 진행 중인 이미지는 아무것도 그리지 않는다.
     */
    public void testFailedImagePlaceholder() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/dir/bad.png", ImageResource.failed("404"))
                .put("http://t.test/dir/wait.png", ImageResource.PENDING);
        RenderedPage p = RenderTestSupport.page("<img src='bad.png' alt='A &lt;b&gt;picture&lt;/b&gt;' width='200' height='40'>", 220, 60, src);
        DisplayCommand c = find(p, DisplayCommand.Type.PLACEHOLDER);
        Check.notNull(c, "a placeholder exists");
        String[] lines = ((DisplayCommands.Placeholder) c).getLines();
        Check.equal(2, lines.length, "URL line and alt line");
        Check.equal(DisplayListBuilder.UNSUPPORTED_URL + "http://t.test/dir/bad.png", lines[0], "the unsupported URL is shown");
        Check.equal("A <b>picture</b>", lines[1], "the alt text is shown as text, not as markup");
        BufferedImage img = RenderTestSupport.paint(p, 220, 60);
        Check.isTrue(RenderTestSupport.ink(img, 0, 0, 200, 40) > 30, "the placeholder paints a frame and text");
        RenderedPage q = RenderTestSupport.page("<img src='wait.png' alt='x' width='50' height='20'><img src='none.png' width='50' height='20'>", 150, 40, src);
        Check.isNull(find(q, DisplayCommand.Type.PLACEHOLDER), "pending or unknown images paint no placeholder");
        Check.isNull(find(q, DisplayCommand.Type.IMAGE), "and no image");
        RenderedPage r = RenderTestSupport.page("<img alt='only alt' width='100' height='20'>", 150, 40, src);
        Check.equal("only alt", ((DisplayCommands.Placeholder) find(r, DisplayCommand.Type.PLACEHOLDER)).getLines()[0], "an image without src shows its alt text");
    }

    /**
     * A video is replaced by the notice that videos are not supported, in its own box.
     * <p>
     * 비디오는 자신의 상자 안에서 비디오를 지원하지 않는다는 안내로 대체된다.
     */
    public void testVideoNotice() {
        RenderedPage p = RenderTestSupport.page("<video src='v.mp4' width='240' height='80'></video>", 260, 100, null);
        DisplayCommand c = find(p, DisplayCommand.Type.PLACEHOLDER);
        Check.notNull(c, "the video placeholder exists");
        Check.equal(DisplayListBuilder.UNSUPPORTED_VIDEO, ((DisplayCommands.Placeholder) c).getLines()[0], "the notice text");
        java.awt.geom.Rectangle2D b = c.getBounds();
        Check.equal(240.0, b.getWidth(), "the original width is kept");
        Check.equal(80.0, b.getHeight(), "the original height is kept");
        BufferedImage img = RenderTestSupport.paint(p, 260, 100);
        Check.isTrue(RenderTestSupport.ink(img, 0, 0, 240, 80) > 30, "the notice is painted");
    }

    /**
     * Audio shows its controls when they are asked for, and the unsupported URL and alt text when the file cannot be used.
     * <p>
     * 오디오는 컨트롤을 요청하면 컨트롤을, 파일을 쓸 수 없으면 지원하지 않는 URL 과 alt 텍스트를 보여 준다.
     */
    public void testAudio() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/dir/bad.mp3", ImageResource.failed("format"))
                .put("http://t.test/dir/ok.mp3", ImageResource.AVAILABLE);
        RenderedPage ok = RenderTestSupport.page("<audio controls src='ok.mp3'></audio>", 320, 80, src);
        DisplayCommand bar = find(ok, DisplayCommand.Type.PLACEHOLDER);
        Check.notNull(bar, "the control bar exists");
        BufferedImage img = RenderTestSupport.paint(ok, 320, 80);
        Check.isTrue(RenderTestSupport.ink(img, 0, 0, 100, 54) > 20, "the control bar paints ink (play button and time)");
        RenderedPage bad = RenderTestSupport.page("<audio controls alt='sound' src='bad.mp3'></audio>", 320, 80, src);
        String[] lines = ((DisplayCommands.Placeholder) find(bad, DisplayCommand.Type.PLACEHOLDER)).getLines();
        Check.equal(DisplayListBuilder.UNSUPPORTED_URL + "http://t.test/dir/bad.mp3", lines[0], "unsupported audio URL");
        Check.equal("sound", lines[1], "alt text of the audio element");
        RenderedPage viaSource = RenderTestSupport.page("<audio controls><source src='bad.mp3'></audio>", 320, 80, src);
        Check.equal(DisplayListBuilder.UNSUPPORTED_URL + "http://t.test/dir/bad.mp3",
                ((DisplayCommands.Placeholder) find(viaSource, DisplayCommand.Type.PLACEHOLDER)).getLines()[0], "the source child is used");
    }

    /**
     * A background image repeats horizontally only when asked and starts at the position of the box.
     * <p>
     * 배경 이미지는 요청할 때만 가로로 반복하며 상자의 위치에서 시작한다.
     */
    public void testBackgroundImageRepeat() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/bg.png",
                ImageResource.loaded(RenderTestSupport.solid(4, 4, 0xFF00FF00)));
        RenderedPage p = RenderTestSupport.page("<div style='width:20px;height:8px;background:url(http://t.test/bg.png) repeat-x'></div>"
                + "<div style='margin-top:10px;width:20px;height:8px;background:url(http://t.test/bg.png) no-repeat'></div>"
                + "<div style='margin-top:10px;width:20px;height:8px;background:url(http://t.test/bg.png) repeat'></div>", 60, 60, src);
        BufferedImage img = RenderTestSupport.paint(p, 60, 60);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 18, 2), "repeat-x: the last tile of the first row");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 18, 6), "repeat-x: no second row of tiles");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 2, 20), "no-repeat: the single tile");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 10, 20), "no-repeat: nothing beyond the tile");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 18, 42), "repeat: tiles in both directions");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 18, 38), "repeat: first row");
    }

    /**
     * A background color paints below the background image.
     * <p>
     * 배경색은 배경 이미지 아래에 그려진다.
     */
    public void testBackgroundColorUnderImage() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/bg.png",
                ImageResource.loaded(RenderTestSupport.solid(4, 4, 0xFF00FF00)));
        RenderedPage p = RenderTestSupport.page("<div style='width:20px;height:8px;background:#ff0000 url(http://t.test/bg.png) no-repeat'></div>", 40, 20, src);
        BufferedImage img = RenderTestSupport.paint(p, 40, 20);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 2, 2), "the image covers the color");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 10, 2), "the color is visible beside the image");
    }
}
