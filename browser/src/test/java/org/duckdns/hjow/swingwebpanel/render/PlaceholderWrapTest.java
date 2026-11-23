package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the splitting of the text lines of a replacement box (a failed image or audio): a long address without spaces must not be cut off at the
 * edge of the box (found in the real window check, <code>docs/progress/uicheck.md</code>).
 * <p>
 * 대체 상자(실패한 이미지나 오디오)의 글줄 나누기를 시험한다: 공백 없는 긴 주소가 상자 가장자리에서 잘리면 안 된다(실제 창 확인에서 발견됨,
 * <code>docs/progress/uicheck.md</code>).
 */
public class PlaceholderWrapTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PlaceholderWrapTest() {
    }

    /**
     * Returns the metrics of the font that the replacement box uses.
     * <p>
     * 대체 상자가 쓰는 글꼴의 측정값을 반환한다.
     *
     * @return the metrics / 측정값
     */
    private static FontMetrics metrics() {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        FontMetrics fm = g.getFontMetrics(new Font("Dialog", Font.PLAIN, 12));
        g.dispose();
        return fm;
    }

    /**
     * Joins the pieces.
     * <p>
     * 조각을 이어 붙인다.
     *
     * @param parts the pieces / 조각들
     * @return the text / 글
     */
    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        // 조각을 차례로 이어 붙인다.
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /**
     * A text that fits stays one piece, and an empty text is one empty piece.
     * <p>
     * 들어가는 글은 한 조각으로 남고, 빈 글은 빈 조각 하나이다.
     */
    public void testShortAndEmptyText() {
        FontMetrics fm = metrics();
        List<String> one = DisplayCommands.Placeholder.wrapLine("alt text", fm, 300);
        Check.equal(1, one.size(), "a short text is one piece");
        Check.equal("alt text", one.get(0), "unchanged");
        List<String> none = DisplayCommands.Placeholder.wrapLine("", fm, 300);
        Check.equal(1, none.size(), "an empty text is one piece");
        Check.equal("", none.get(0), "and it is empty");
    }

    /**
     * A long address without spaces is cut between characters so that every piece fits, and nothing is lost.
     * <p>
     * 공백 없는 긴 주소는 글자 사이에서 나뉘어 모든 조각이 폭에 들어가며 잃는 글이 없다.
     */
    public void testLongAddressWithoutSpaces() {
        FontMetrics fm = metrics();
        String url = "지원하지 않는 URL (Unsupported URL): classpath://org/duckdns/hjow/swingwebpanel/examples/pages/images/missing-picture-with-a-very-long-name.png";
        double width = 200;
        List<String> parts = DisplayCommands.Placeholder.wrapLine(url, fm, width);
        Check.isTrue(parts.size() > 1, "the line is split (" + parts.size() + " pieces)");
        // 모든 조각이 폭 안에 들어오는지 확인한다.
        for (int i = 0; i < parts.size(); i++) {
            Check.isTrue(fm.stringWidth(parts.get(i)) <= width, "piece " + i + " fits: " + parts.get(i));
        }
        Check.equal(url, join(parts), "the pieces make the original text again");
    }

    /**
     * Words are kept together when there is a space to break at.
     * <p>
     * 나눌 공백이 있으면 단어를 그대로 둔다.
     */
    public void testBreaksAtSpaces() {
        FontMetrics fm = metrics();
        String text = "one two three four five six seven eight nine ten eleven twelve";
        List<String> parts = DisplayCommands.Placeholder.wrapLine(text, fm, 90);
        Check.isTrue(parts.size() > 1, "split");
        // 각 조각은 글자 하나가 나뉜 단어로 시작하지 않아야 한다: 이어 붙이면 원문이고 공백 뒤에서 시작한다.
        for (int i = 1; i < parts.size(); i++) {
            String previous = parts.get(i - 1);
            Check.equal(' ', previous.charAt(previous.length() - 1), "piece " + (i - 1) + " ends with the space it was broken at: '" + previous + "'");
        }
        Check.equal(text, join(parts), "nothing is lost");
    }

    /**
     * A surrogate pair is never split in the middle.
     * <p>
     * 서로게이트 쌍은 가운데에서 나뉘지 않는다.
     */
    public void testSurrogatePairsStayTogether() {
        FontMetrics fm = metrics();
        StringBuilder sb = new StringBuilder();
        // 보충 문자(U+1F600)를 여러 개 이어 붙인다.
        for (int i = 0; i < 30; i++) {
            sb.appendCodePoint(0x1F600);
        }
        String text = sb.toString();
        List<String> parts = DisplayCommands.Placeholder.wrapLine(text, fm, 40);
        // 어느 조각도 낱개 서로게이트로 시작하거나 끝나지 않아야 한다.
        for (int i = 0; i < parts.size(); i++) {
            String piece = parts.get(i);
            Check.isTrue(piece.length() > 0, "no empty piece");
            Check.isTrue(!Character.isLowSurrogate(piece.charAt(0)), "piece " + i + " does not start with a low surrogate");
            Check.isTrue(!Character.isHighSurrogate(piece.charAt(piece.length() - 1)), "piece " + i + " does not end with a high surrogate");
        }
        Check.equal(text, join(parts), "nothing is lost");
    }
}
