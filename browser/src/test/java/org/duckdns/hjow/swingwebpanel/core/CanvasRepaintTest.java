package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.Executor;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.render.CanvasSource;
import org.duckdns.hjow.swingwebpanel.render.DisplayCommand;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The repaint path of the canvas (plan item P20-05): when only the picture of a canvas changed, the page keeps its layout and builds a new
 * display list that asks the canvas source for the new picture; a change of the DOM or of the viewport lays the page out again; a rendering
 * without any change is the same object. This keeps an animated canvas from laying the page out for every frame.
 * <p>
 * 캔버스의 다시 그리기 경로(계획 P20-05): 캔버스의 그림만 바뀌면 페이지는 배치를 유지하고 새 그림을 캔버스 공급원에 물어 새 디스플레이 리스트를 만든다.
 * DOM 이나 뷰포트가 바뀌면 페이지를 다시 배치한다. 아무것도 바뀌지 않은 렌더링은 같은 객체이다. 그래서 움직이는 캔버스가 프레임마다 페이지를 다시 배치하지
 * 않는다.
 */
public class CanvasRepaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasRepaintTest() {
    }

    /**
     * A canvas source that counts the questions of the renderer.
     * <p>
     * 렌더러의 질문을 세는 캔버스 공급원.
     */
    private static final class CountingSource implements CanvasSource {
        /**
         * The number of questions.
         * <p>
         * 질문 수.
         */
        int calls;

        /**
         * The picture that is handed out.
         * <p>
         * 내주는 그림.
         */
        DecodedImage picture = new DecodedImage(2, 2, new int[] { 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000 });

        /**
         * Counts and answers.
         * <p>
         * 세고 답한다.
         *
         * @param canvas the canvas element / 캔버스 요소
         * @return the picture / 그림
         */
        public DecodedImage getCanvasImage(Element canvas) {
            calls++;
            return picture;
        }
    }

    /**
     * Counts the image commands of a rendering.
     * <p>
     * 렌더링의 이미지 명령을 센다.
     *
     * @param page the rendering / 렌더링
     * @return the number of image commands / 이미지 명령 수
     */
    private static int images(RenderedPage page) {
        int n = 0;
        // 디스플레이 리스트의 명령 중 이미지 명령만 센다.
        for (DisplayCommand c : page.getDisplayList().getCommands()) {
            if (c.getType() == DisplayCommand.Type.IMAGE) {
                n++;
            }
        }
        return n;
    }

    /**
     * Only the picture of a canvas changed: the layout object is reused and the display list is new; other changes lay the page out again.
     * <p>
     * 캔버스의 그림만 바뀌었다: 배치 객체를 재사용하고 디스플레이 리스트는 새로 만든다. 다른 변경은 페이지를 다시 배치한다.
     */
    public void testPictureChangeReusesLayout() {
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        doc.setUrl("http://example.test/page.html");
        new HtmlParser(doc, "<!doctype html><body style='margin:0'><canvas id=c width=40 height=20></canvas><p id=p>text</p></body>").run();
        PagePresentation presentation = new PagePresentation(doc, new PageRequestContext(null), new ResourceFetcher(null, FetchLimits.DEFAULT, null),
                new CookieStore(), new Executor() {
                    /**
                     * Runs nothing: this test loads no resources.
                     * <p>
                     * 아무것도 실행하지 않는다. 이 시험은 리소스를 읽지 않는다.
                     *
                     * @param command the work / 작업
                     */
                    public void execute(Runnable command) {
                        command.run();
                    }
                });
        presentation.parsingFinished();
        CountingSource source = new CountingSource();
        presentation.setCanvasSource(source);
        try {
            RenderedPage first = presentation.render(300, 200);
            Check.equal(1, images(first), "the canvas is one image command");
            Check.equal(1, source.calls, "the renderer asked for the picture once");
            Check.equal(true, presentation.render(300, 200) == first, "nothing changed: the same rendering");
            Check.equal(1, source.calls, "and nobody asked again");
            presentation.canvasChanged();
            RenderedPage second = presentation.render(300, 200);
            Check.equal(true, second != first, "a changed picture gives a new rendering");
            Check.equal(true, second.getLayout() == first.getLayout(), "with the same layout");
            Check.equal(2, source.calls, "the renderer asked for the new picture");
            Check.equal(1, images(second), "still one image command");
            presentation.canvasChanged();
            presentation.canvasChanged();
            RenderedPage third = presentation.render(300, 200);
            Check.equal(true, third.getLayout() == first.getLayout(), "several changes before the next rendering are one repaint of the same layout");
            Check.equal(3, source.calls, "asked once for them");
            doc.getElementById("p").setTextContent("changed text");
            RenderedPage fourth = presentation.render(300, 200);
            Check.equal(true, fourth.getLayout() != third.getLayout(), "a change of the DOM lays the page out again");
            presentation.render(300, 200);
            RenderedPage resized = presentation.render(250, 200);
            Check.equal(true, resized.getLayout() != fourth.getLayout(), "so does a new viewport size");
            presentation.canvasChanged();
            presentation.setCanvasSource(null);
            RenderedPage without = presentation.render(250, 200);
            Check.equal(0, images(without), "without a canvas source the canvas paints nothing");
        } finally {
            presentation.retire();
        }
    }
}
