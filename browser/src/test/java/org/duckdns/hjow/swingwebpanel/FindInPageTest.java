package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The search in the page of a component (API contract 5.29, cases C-FIND-1 to C-FIND-5): counting and stepping through the matches,
 * what counts as the text of the page, the painted highlights and the scrolling, following a page that changes, and the rules of the
 * methods themselves.
 * <p>
 * 컴포넌트의 페이지 안 찾기(API 계약 5.29, 사례 C-FIND-1 ~ C-FIND-5): 일치를 세고 차례로 가기, 무엇이 페이지의 글인지, 그려지는 강조와 스크롤,
 * 바뀌는 페이지 따라가기, 메서드 자체의 규칙.
 */
public class FindInPageTest {

    /** The component of the case. / 사례의 컴포넌트. */
    private SwingWebPanel panel;

    /** The results that were announced, with a mark if one came on another thread than the event dispatch thread. / 알려진 결과. 이벤트 디스패치 스레드가 아닌 스레드에서 왔으면 표시가 붙는다. */
    private final List<String> announced = new ArrayList<String>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FindInPageTest() {
    }

    /**
     * Creates the component.
     * <p>
     * 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component with a size. / 크기가 있는 컴포넌트를 만든다. */
            public void run() {
                panel = new SwingWebPanel();
                panel.setSize(600, 400);
            }
        });
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_FIND_RESULT, new PropertyChangeListener() {
            /** Remembers the result and the thread. / 결과와 스레드를 기억한다. */
            public void propertyChange(PropertyChangeEvent e) {
                synchronized (announced) {
                    announced.add(e.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "" : "@other"));
                }
            }
        });
    }

    /**
     * Disposes the component.
     * <p>
     * 컴포넌트를 폐기한다.
     */
    public void tearDown() {
        panel.dispose();
    }

    /**
     * Shows a page and waits until it is laid out.
     * <p>
     * 페이지를 보이고 배치될 때까지 기다린다.
     *
     * @param title the title of the page / 페이지의 제목
     * @param body  the markup of the body / 본문 마크업
     * @throws Exception on failure / 실패 시
     */
    private void show(String title, String body) throws Exception {
        panel.setContent("<!doctype html><title>" + title + "</title><body style='margin:0'>" + body + "</body>", "text/html");
        long end = System.currentTimeMillis() + 30000;
        // 제목이 도착하고 읽기가 끝날 때까지 짧게 쉬며 기다린다.
        while (!(title.equals(panel.getTitle()) && panel.getProgress() == 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(title, panel.getTitle(), "the page is shown");
    }

    /**
     * Waits until the result of the search is the expected one.
     * <p>
     * 찾기의 결과가 기대한 것이 될 때까지 기다린다.
     *
     * @param text    the expected search text / 기대하는 검색어
     * @param count   the expected number of matches / 기대하는 일치 개수
     * @param index   the expected number of the current match / 기대하는 현재 일치의 번호
     * @param message what the result means / 결과가 뜻하는 것
     * @throws Exception on failure / 실패 시
     */
    private void awaitResult(String text, int count, int index, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        FindResult r = panel.getFindResult();
        // 결과가 될 때까지 짧게 쉬며 기다린다.
        while (!(r.getText().equals(text) && r.getCount() == count && r.getIndex() == index) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            r = panel.getFindResult();
        }
        Check.equal(index + "/" + count + " of [" + text + "]", r.getIndex() + "/" + r.getCount() + " of [" + r.getText() + "]", message);
    }

    /**
     * Paints the component and counts the pixels of the top band that have the color of the other matches and of the current match.
     * <p>
     * 컴포넌트를 그리고, 위쪽 띠에서 다른 일치의 색과 현재 일치의 색을 가진 화소를 센다.
     *
     * @param height the height of the band / 띠의 높이
     * @return {yellow pixels, orange pixels} / {노란 화소 수, 주황 화소 수}
     * @throws Exception on failure / 실패 시
     */
    private int[] paintedMarks(final int height) throws Exception {
        final int[] counts = new int[2];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints into an image and classifies the pixels. / 이미지에 그리고 화소를 분류한다. */
            public void run() {
                BufferedImage image = new BufferedImage(panel.getWidth(), height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                // 화소마다 흰 바탕 위의 노란 강조와 주황 강조의 색인지 본다.
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int gr = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        if (r > 240 && gr > 225 && b > 140 && b < 200) {
                            counts[0]++;
                        } else if (r > 240 && gr > 150 && gr < 200 && b > 70 && b < 130) {
                            counts[1]++;
                        }
                    }
                }
            }
        });
        return counts;
    }

    /**
     * C-FIND-1: a search counts the matches and makes the first one current; the same text again goes to the next or the previous
     * match and around at the ends; letter case matters only when asked; a text that is not on the page gives zero matches.
     * <p>
     * C-FIND-1: 찾기는 일치를 세고 첫 일치를 현재 일치로 만든다. 같은 글로 다시 부르면 다음이나 이전 일치로 가고 끝에서는 반대쪽 끝으로 돈다.
     * 대소문자는 요청했을 때만 구분한다. 페이지에 없는 글은 일치가 0 개이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FIND_1_CountAndStep() throws Exception {
        Check.equal(FindResult.NONE, panel.getFindResult(), "no search at first");
        show("one", "<p>Apple pie and apple juice.</p><p>An APPLE a day; pineapple too.</p>");
        panel.find("apple", false, true);
        awaitResult("apple", 4, 1, "four places match without regard to case, the first is current");
        panel.find("apple", false, true);
        awaitResult("apple", 4, 2, "the same text goes to the next match");
        panel.find("apple", false, false);
        awaitResult("apple", 4, 1, "backwards goes to the previous match");
        panel.find("apple", false, false);
        awaitResult("apple", 4, 4, "backwards from the first match goes around to the last");
        panel.find("apple", false, true);
        awaitResult("apple", 4, 1, "forwards from the last match goes around to the first");
        panel.find("apple", true, true);
        awaitResult("apple", 2, 1, "with match case only the lower case places match");
        Check.isTrue(panel.getFindResult().isMatchCase() && panel.getFindResult().isActive(), "the result tells how it was searched");
        panel.find("banana", false, true);
        awaitResult("banana", 0, 0, "a text that is not on the page has no match but the search is active");
        panel.clearFind();
        awaitResult("", 0, 0, "clearing ends the search");
        Check.equal(FindResult.NONE, panel.getFindResult(), "the result is the empty one again");
    }

    /**
     * C-FIND-2: the search sees the text as it is displayed: across inline elements, with collapsed white space and across a wrapped
     * line, but not across blocks, not in hidden text and not in text that is not rendered.
     * <p>
     * C-FIND-2: 찾기는 표시되는 그대로의 글을 본다: 인라인 요소에 걸쳐, 접힌 공백으로, 자동으로 바뀐 줄에 걸쳐 찾지만, 블록에 걸쳐서는 찾지 않고
     * 숨겨진 글과 렌더링되지 않는 글에서도 찾지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FIND_2_DisplayedText() throws Exception {
        show("two", "<p>hel<b>lo wor</b>ld</p><p>many     spaces\n here</p>"
                + "<div style='width:60px;font:16px monospace'>wrapped words</div>"
                + "<p>first</p><p>second</p><p style='visibility:hidden'>ghost</p><p style='display:none'>phantom</p>"
                + "<p>가나다 <i>라마</i>바</p><script>var skipped = 'scripttext';</" + "script>");
        panel.find("hello world", false, true);
        awaitResult("hello world", 1, 1, "a phrase is found across inline elements");
        panel.find("many spaces here", false, true);
        awaitResult("many spaces here", 1, 1, "collapsed white space is one space");
        panel.find("many   spaces", false, true);
        awaitResult("many spaces", 1, 1, "white space of the search text is collapsed too");
        panel.find("wrapped words", false, true);
        awaitResult("wrapped words", 1, 1, "a phrase is found across a wrapped line");
        panel.find("firstsecond", false, true);
        awaitResult("firstsecond", 0, 0, "text of two paragraphs is not joined");
        panel.find("first second", false, true);
        awaitResult("first second", 0, 0, "and not joined with a space either");
        panel.find("ghost", false, true);
        awaitResult("ghost", 0, 0, "hidden text is not found");
        panel.find("phantom", false, true);
        awaitResult("phantom", 0, 0, "text that is not rendered is not found");
        panel.find("scripttext", false, true);
        awaitResult("scripttext", 0, 0, "the text of a script is not found");
        panel.find("다 라마바", false, true);
        awaitResult("다 라마바", 1, 1, "Hangul is found across inline elements");
    }

    /**
     * C-FIND-3: the matches are painted over the page, the current one in another color; clearing removes the marks; the view scrolls
     * to a current match that is outside the visible area.
     * <p>
     * C-FIND-3: 일치는 페이지 위에 그려지고 현재 일치는 다른 색이다. 지우면 표시가 사라진다. 현재 일치가 보이는 영역 밖이면 뷰가 그곳으로
     * 스크롤한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FIND_3_HighlightsAndScrolling() throws Exception {
        final JScrollPane[] holder = new JScrollPane[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Puts the component into a scroll pane. / 컴포넌트를 스크롤 창에 넣는다. */
            public void run() {
                holder[0] = new JScrollPane(panel);
                holder[0].setSize(620, 220);
                holder[0].doLayout();
                holder[0].getViewport().doLayout();
            }
        });
        final JScrollPane pane = holder[0];
        show("three", "<p style='margin:0;font:20px monospace'>mark here, mark there</p><div style='height:1500px'></div>"
                + "<p style='margin:0;font:20px monospace'>the last mark</p><div style='height:300px'></div>");
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Lays the pane out for the content. / 내용에 맞춰 창을 배치한다. */
            public void run() {
                pane.doLayout();
                pane.getViewport().doLayout();
            }
        });
        int[] plain = paintedMarks(40);
        Check.equal(0, plain[0] + plain[1], "nothing is marked before a search");
        panel.find("mark", false, true);
        awaitResult("mark", 3, 1, "three matches");
        int[] marked = paintedMarks(40);
        Check.isTrue(marked[0] > 50, "the second match of the first line is painted in the color of the matches: " + marked[0]);
        Check.isTrue(marked[1] > 50, "the current match is painted in its own color: " + marked[1]);
        panel.find("mark", false, true);
        awaitResult("mark", 3, 2, "the second match is current");
        int[] moved = paintedMarks(40);
        Check.isTrue(moved[0] > 50 && moved[1] > 50, "both colors are still on the first line: " + moved[0] + ", " + moved[1]);
        Check.equal(0, pane.getViewport().getViewPosition().y, "a match that is visible does not scroll the view");
        panel.find("mark", false, true);
        awaitResult("mark", 3, 3, "the last match is current");
        long end = System.currentTimeMillis() + 10000;
        // 뷰가 아래로 스크롤될 때까지 짧게 쉬며 기다린다.
        while (pane.getViewport().getViewPosition().y <= 1000 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(pane.getViewport().getViewPosition().y > 1000, "the view scrolls to the current match: " + pane.getViewport().getViewPosition());
        panel.clearFind();
        awaitResult("", 0, 0, "the search ended");
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Scrolls back to the top. / 맨 위로 되돌린다. */
            public void run() {
                pane.getViewport().setViewPosition(new Point(0, 0));
            }
        });
        int[] cleared = paintedMarks(40);
        Check.equal(0, cleared[0] + cleared[1], "clearing removes the marks");
    }

    /**
     * C-FIND-4: an active search follows the page: a script that adds text changes the count; showing another page ends the search;
     * every result is announced on the event dispatch thread.
     * <p>
     * C-FIND-4: 활성 찾기는 페이지를 따라간다: 스크립트가 글을 더하면 개수가 바뀐다. 다른 페이지를 보이면 찾기는 끝난다. 모든 결과는 이벤트
     * 디스패치 스레드에서 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FIND_4_FollowsThePage() throws Exception {
        show("four", "<p id=p>token one</p><script>window.addEventListener('message', function () {"
                + " var q = document.createElement('p'); q.textContent = 'token two and token three'; document.body.appendChild(q); });</" + "script>");
        panel.find("token", false, true);
        awaitResult("token", 1, 1, "one match at first");
        panel.postMessage("grow");
        awaitResult("token", 3, 1, "the count follows the page and the current match stays");
        show("other", "<p>token token</p>");
        awaitResult("", 0, 0, "another page ends the search");
        synchronized (announced) {
            Check.isTrue(announced.size() >= 3, "the results were announced: " + announced);
            // 알려진 결과마다 스레드 표시가 없는지 본다.
            for (int i = 0; i < announced.size(); i++) {
                Check.isFalse(announced.get(i).endsWith("@other"), "announced on the event dispatch thread: " + announced.get(i));
            }
            Check.equal(FindResult.NONE.toString(), announced.get(announced.size() - 1), "the last announcement ends the search");
        }
    }

    /**
     * C-FIND-5: the rules of the methods: a null text is refused, an empty text clears, the blank page has no matches, a search of a
     * page without scripts works, and a disposed component refuses.
     * <p>
     * C-FIND-5: 메서드의 규칙: null 글은 거부하고, 빈 글은 찾기를 지우며, 빈 페이지에는 일치가 없고, 스크립트 없는 페이지의 찾기도 동작하며, 폐기된
     * 컴포넌트는 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FIND_5_Rules() throws Exception {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Passes a null text. / null 글을 넘긴다. */
            public void run() {
                panel.find(null, false, true);
            }
        }, "a null text is refused");
        panel.find("anything", false, true);
        awaitResult("anything", 0, 0, "the blank page has no matches");
        panel.find("   ", false, true);
        awaitResult("", 0, 0, "a text of white space only clears the search");
        panel.setJavascriptEnabled(false);
        panel.setContent("plain text with a needle in it\nand a second needle", "text/plain");
        long end = System.currentTimeMillis() + 30000;
        // 텍스트 페이지가 도착할 때까지 기다린다.
        while (!"text/plain".equals(panel.getMimeType()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        panel.find("NEEDLE", false, true);
        awaitResult("NEEDLE", 2, 1, "a text page without scripts is searched");
        final SwingWebPanel gone = new SwingWebPanel();
        gone.dispose();
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Searches in a disposed component. / 폐기된 컴포넌트에서 찾는다. */
            public void run() {
                gone.find("x", false, true);
            }
        }, "a disposed component refuses a search");
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Clears in a disposed component. / 폐기된 컴포넌트에서 지운다. */
            public void run() {
                gone.clearFind();
            }
        }, "a disposed component refuses to clear");
        Check.equal(FindResult.NONE, gone.getFindResult(), "a disposed component has the empty result");
    }
}
