package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;

/**
 * The text of one rendered page as the user sees it, for the search in the page (plan item P31-09): the painted text of the visible text
 * fragments in document order, joined the way a reader reads it. Fragments of one line are joined directly, or with one space where the
 * layout collapsed white space between them; lines of one block that were broken by wrapping are joined the same way, so a phrase is
 * found across a line break; different blocks (paragraphs, table cells) are separated by a character that no search text matches. Every
 * kind of white space counts as a space. A match is a pair of positions in this text; {@link #highlight} turns matches into the area
 * that they cover on the page. An instance belongs to one {@link RenderedPage} and reads only immutable layout data. Text inside form
 * controls and inside frames is not part of the text.
 * <p>
 * 사용자가 보는 그대로의 한 렌더링된 페이지의 글. 페이지 안 찾기에 쓴다(계획 P31-09): 보이는 텍스트 조각의 그려진 글을 문서 순서대로, 읽는 사람이
 * 읽는 방식으로 이어 붙인다. 한 줄의 조각은 그대로 잇거나, 레이아웃이 그 사이의 공백을 접었으면 공백 한 칸으로 잇는다. 한 블록에서 자동으로 줄이
 * 바뀐 곳도 같은 방식으로 이으므로 줄 바꿈에 걸친 구절도 찾는다. 다른 블록(문단, 표 셀) 사이에는 어떤 검색어와도 일치하지 않는 문자를 둔다. 모든
 * 종류의 공백은 공백 한 칸으로 친다. 일치는 이 글 안의 위치 한 쌍이며, {@link #highlight} 가 일치들을 페이지에서 덮는 영역으로 바꾼다. 인스턴스는
 * 한 {@link RenderedPage} 에 속하고 불변 레이아웃 데이터만 읽는다. 폼 컨트롤 안과 프레임 안의 글은 포함하지 않는다.
 */
public final class TextSearch {

    /** The character between different blocks; no search text contains it. / 다른 블록 사이의 문자. 어떤 검색어에도 없다. */
    private static final char BLOCK_BREAK = (char) 0xFFFF;

    /** The largest number of matches that a search reports. / 찾기가 보고하는 일치의 최대 개수. */
    public static final int MAX_MATCHES = 10000;

    /** The rendered page. / 렌더링된 페이지. */
    private final RenderedPage page;

    /** The text of the page as described in the class comment. / 클래스 설명대로의 페이지의 글. */
    private final String text;

    /** The same text folded for a search that ignores letter case, made when first needed. / 대소문자를 무시하는 찾기를 위해 접은 같은 글. 처음 필요할 때 만든다. */
    private String folded;

    /** For every character of the text the fragment that painted it, or null for a joining character. / 글의 문자마다 그것을 그린 조각. 이어 붙인 문자이면 null. */
    private final LayoutBox[] boxes;

    /** For every character of the text its index in the painted text of its fragment. / 글의 문자마다 그 조각의 그려진 글 안에서의 인덱스. */
    private final int[] indexes;

    /**
     * Collects the text of a rendered page.
     * <p>
     * 렌더링된 페이지의 글을 모은다.
     *
     * @param page     the page, not null / 페이지(null 불가)
     * @param geometry the selection geometry of the same page / 같은 페이지의 선택 기하
     */
    public TextSearch(RenderedPage page, TextSelections geometry) {
        if (page == null || geometry == null || geometry.getPage() != page) {
            throw new IllegalArgumentException("the geometry must belong to the page");
        }
        this.page = page;
        LayoutBox[] all = geometry.textBoxes();
        StringBuilder sb = new StringBuilder();
        List<LayoutBox> owners = new ArrayList<LayoutBox>();
        List<Integer> places = new ArrayList<Integer>();
        LayoutBox prev = null;
        // 텍스트 조각을 문서 순서로 훑으며 보이는 것의 글을 이어 붙인다.
        for (int i = 0; i < all.length; i++) {
            LayoutBox b = all[i];
            String painted = b.getText();
            if (painted == null || painted.length() == 0 || !shown(b)) {
                continue;
            }
            if (prev != null) {
                char joiner = joiner(prev, b, sb, painted);
                if (joiner != 0) {
                    sb.append(joiner);
                    owners.add(null);
                    places.add(Integer.valueOf(0));
                }
            }
            // 조각의 글자마다 그것이 어느 조각의 몇 번째 글자인지 기록한다.
            for (int k = 0; k < painted.length(); k++) {
                char c = painted.charAt(k);
                sb.append(isSpace(c) ? ' ' : c);
                owners.add(b);
                places.add(Integer.valueOf(k));
            }
            prev = b;
        }
        text = sb.toString();
        boxes = owners.toArray(new LayoutBox[owners.size()]);
        indexes = new int[places.size()];
        // 목록의 인덱스를 배열로 옮긴다.
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = places.get(i).intValue();
        }
    }

    /**
     * Tells whether a character counts as a space in the search.
     * <p>
     * 문자가 찾기에서 공백으로 치는 문자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for white space and the no-break space / 공백 문자와 줄 바꿈 없는 공백이면 true
     */
    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || c == (char) 0x00A0;
    }

    /**
     * Tells whether the text of a fragment is shown: neither it nor a box around it is hidden with <code>visibility</code>.
     * <p>
     * 조각의 글이 보이는지 알려 준다: 조각도 그것을 둘러싼 상자도 <code>visibility</code> 로 숨겨지지 않았다.
     *
     * @param box the fragment / 조각
     * @return true if shown / 보이면 true
     */
    private static boolean shown(LayoutBox box) {
        ComputedStyle s = box.getStyle();
        // 텍스트 조각에 스타일이 없으면 가장 가까운 상자의 스타일을 본다(visibility 는 상속된다).
        for (LayoutBox b = box; s == null && b != null; b = b.getParent()) {
            s = b.getStyle();
        }
        String v = s == null ? null : s.keyword(Prop.VISIBILITY);
        return !"hidden".equals(v) && !"collapse".equals(v);
    }

    /**
     * Decides what joins the text so far and the next fragment.
     * <p>
     * 지금까지의 글과 다음 조각을 무엇으로 이을지 정한다.
     *
     * @param prev    the fragment before / 앞 조각
     * @param cur     the next fragment / 다음 조각
     * @param sofar   the text so far / 지금까지의 글
     * @param painted the painted text of the next fragment / 다음 조각의 그려진 글
     * @return a space, the block break, or 0 for nothing / 공백, 블록 구분 문자, 또는 아무것도 없으면 0
     */
    private static char joiner(LayoutBox prev, LayoutBox cur, StringBuilder sofar, String painted) {
        // 다른 블록의 글은 이어 읽지 않는다.
        if (TextSelections.blockOf(prev) != TextSelections.blockOf(cur)) {
            return BLOCK_BREAK;
        }
        boolean spaced = sofar.length() > 0 && sofar.charAt(sofar.length() - 1) == ' ' || isSpace(painted.charAt(0));
        if (spaced) {
            return 0;
        }
        // 한 줄이든 자동으로 바뀐 줄이든, 원본의 그 사이에 접힌 공백이 있었을 때만 공백 한 칸으로 잇는다(공백 없이 줄이 바뀐 글은 그대로 잇는다).
        return TextSelections.collapsedGap(prev, cur) ? ' ' : 0;
    }

    /**
     * Returns the rendered page that this text belongs to.
     * <p>
     * 이 글이 속한 렌더링된 페이지를 반환한다.
     *
     * @return the page / 페이지
     */
    public RenderedPage getPage() {
        return page;
    }

    /**
     * Returns the text of the page as the search sees it (block breaks appear as U+FFFF).
     * <p>
     * 찾기가 보는 페이지의 글을 반환한다(블록 구분은 U+FFFF 로 나타난다).
     *
     * @return the text / 글
     */
    public String getText() {
        return text;
    }

    /**
     * Prepares a search text: runs of white space become one space; white space at the ends is kept as one space, so a search for a
     * word with a space behind it does not match the same letters at the start of a longer word.
     * <p>
     * 검색어를 손질한다: 이어진 공백은 공백 한 칸이 된다. 양 끝의 공백은 한 칸으로 남기므로, 뒤에 공백을 둔 낱말을 찾을 때 더 긴 낱말의 앞부분은
     * 일치하지 않는다.
     *
     * @param query the text that the user typed / 사용자가 입력한 글
     * @return the text to search for / 찾을 글
     */
    public static String normalize(String query) {
        StringBuilder sb = new StringBuilder();
        // 글자마다 공백류는 공백 한 칸으로 모은다.
        for (int i = 0; query != null && i < query.length(); i++) {
            char c = query.charAt(i);
            if (isSpace(c)) {
                if (sb.length() == 0 || sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            } else if (c != BLOCK_BREAK) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Folds text for a comparison that ignores letter case, one character for one character so that positions stay the same.
     * <p>
     * 대소문자를 무시하는 비교를 위해 글을 접는다. 위치가 그대로이도록 문자 하나를 문자 하나로 바꾼다.
     *
     * @param s the text / 글
     * @return the folded text of the same length / 같은 길이의 접은 글
     */
    private static String fold(String s) {
        char[] out = s.toCharArray();
        // 글자마다 대문자를 거쳐 소문자로 접는다(대문자로만 같아지는 글자도 같게 본다).
        for (int i = 0; i < out.length; i++) {
            out[i] = Character.toLowerCase(Character.toUpperCase(out[i]));
        }
        return new String(out);
    }

    /**
     * Finds the places of a search text in the page, in document order, without overlaps.
     * <p>
     * 페이지에서 검색어가 있는 곳을 문서 순서로, 겹치지 않게 찾는다.
     *
     * @param query     the search text (it is normalized with {@link #normalize(String)}) / 검색어({@link #normalize(String)} 로 손질한다)
     * @param matchCase true to tell upper and lower case apart / 대소문자를 구분하려면 true
     * @return the matches as {start, end} positions in {@link #getText()}, at most {@link #MAX_MATCHES} / {@link #getText()} 안의
     *         {시작, 끝} 위치로 나타낸 일치. 최대 {@link #MAX_MATCHES} 개
     */
    public List<int[]> find(String query, boolean matchCase) {
        List<int[]> out = new ArrayList<int[]>();
        String needle = normalize(query);
        if (needle.length() == 0 || needle.length() > text.length()) {
            return out;
        }
        String hay = text;
        if (!matchCase) {
            if (folded == null) {
                folded = fold(text);
            }
            hay = folded;
            needle = fold(needle);
        }
        int from = 0;
        // 앞에서부터 차례로 찾고, 찾은 곳의 끝에서 다시 찾아 겹치지 않게 한다.
        while (out.size() < MAX_MATCHES) {
            int at = hay.indexOf(needle, from);
            if (at < 0) {
                break;
            }
            out.add(new int[] { at, at + needle.length() });
            from = at + needle.length();
        }
        return out;
    }

    /**
     * Computes the area that matches cover on the page.
     * <p>
     * 일치들이 페이지에서 덮는 영역을 계산한다.
     *
     * @param matches matches as returned by {@link #find} / {@link #find} 가 반환한 일치
     * @param from    the first match to include / 포함할 첫 일치
     * @param to      the match after the last one to include / 포함할 마지막 일치의 다음
     * @param skip    a match to leave out, or -1 / 뺄 일치. 없으면 -1
     * @param color   the ARGB color of the highlight / 강조의 ARGB 색
     * @return the highlight, or null if nothing of the matches is drawn / 강조. 일치 가운데 그려지는 것이 없으면 null
     */
    public SelectionHighlight highlight(List<int[]> matches, int from, int to, int skip, int color) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        boolean any = false;
        // 일치마다 그것이 걸친 조각들의 사각형을 경로에 더한다.
        for (int m = Math.max(0, from); m < to && m < matches.size(); m++) {
            if (m == skip) {
                continue;
            }
            int[] range = matches.get(m);
            int i = range[0];
            // 일치 안에서 같은 조각에 속한 연속 구간마다 사각형 하나를 낸다.
            while (i < range[1]) {
                LayoutBox b = boxes[i];
                if (b == null) {
                    i++;
                    continue;
                }
                int j = i;
                // 같은 조각의 이어진 글자를 한 구간으로 묶는다.
                while (j + 1 < range[1] && boxes[j + 1] == b && indexes[j + 1] == indexes[j] + 1) {
                    j++;
                }
                if (TextSelections.add(path, b, TextSelections.paintedRect(b, indexes[i], indexes[j] + 1))) {
                    any = true;
                }
                i = j + 1;
            }
        }
        return any ? new SelectionHighlight(page, path, color) : null;
    }

    /**
     * Returns the bounds of one match on the page (document coordinates), for scrolling to it.
     * <p>
     * 일치 하나의 페이지 위 경계(문서 좌표)를 반환한다. 그곳으로 스크롤할 때 쓴다.
     *
     * @param match a match as returned by {@link #find} / {@link #find} 가 반환한 일치
     * @return the bounds, or null if nothing of it is drawn / 경계. 그려지는 것이 없으면 null
     */
    public Rectangle2D boundsOf(int[] match) {
        List<int[]> one = new ArrayList<int[]>();
        one.add(match);
        SelectionHighlight h = highlight(one, 0, 1, -1, 0);
        return h == null ? null : h.getBounds();
    }

    /**
     * Returns the vertical position of the start of a match on the page (document coordinates), to pick the first match below the top
     * of the visible area.
     * <p>
     * 일치가 시작하는 곳의 페이지 위 세로 위치(문서 좌표)를 반환한다. 보이는 영역의 맨 위 아래에 있는 첫 일치를 고를 때 쓴다.
     *
     * @param match a match as returned by {@link #find} / {@link #find} 가 반환한 일치
     * @return the bottom edge of the fragment that holds the first character / 첫 글자가 든 조각의 아래쪽 가장자리
     */
    public double bottomOf(int[] match) {
        // 일치의 첫 그려진 글자가 속한 조각을 찾는다.
        for (int i = match[0]; i < match[1]; i++) {
            if (boxes[i] != null) {
                return boxes[i].getY() + boxes[i].getHeight();
            }
        }
        return 0;
    }
}
