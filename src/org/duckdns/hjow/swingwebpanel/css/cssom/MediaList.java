package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.DomException;

/**
 * The CSSOM <code>MediaList</code> of a style sheet, of an <code>@media</code> rule or of an <code>@import</code> rule: the media queries as text
 * (<code>mediaText</code>) and as a list of query strings (<code>length</code>, <code>item()</code>), changed through <code>mediaText</code>,
 * <code>appendMedium()</code> and <code>deleteMedium()</code>. Every change replaces the media query list of the owner, marks a sheet as changed by a
 * script and tells the sheet, so that the style resolver evaluates the media again. The list parsed from empty text has no medium and the text
 * <code>""</code> but still matches every environment.
 * <p>
 * 스타일 시트, <code>@media</code> 규칙, <code>@import</code> 규칙의 CSSOM <code>MediaList</code>: 미디어 질의를 텍스트(<code>mediaText</code>)와 질의 문자열 목록
 * (<code>length</code>, <code>item()</code>)으로 보여 주며 <code>mediaText</code>, <code>appendMedium()</code>, <code>deleteMedium()</code> 으로 바꾼다. 바꿀 때마다
 * 소유자의 미디어 질의 목록을 교체하고 시트를 스크립트가 바꿨다고 표시하며 시트에 알려, 스타일 리졸버가 미디어를 다시 평가하게 한다. 빈 텍스트에서 파싱한 목록은
 * 미디어가 없고 텍스트가 <code>""</code> 이지만 여전히 모든 환경에 일치한다.
 */
public final class MediaList {

    /** The sheet whose media this list shows, or null. / 이 목록이 미디어를 보여 주는 시트. 없으면 null. */
    private final StyleSheet sheet;

    /** The media rule whose media this list shows, or null. / 이 목록이 미디어를 보여 주는 media 규칙. 없으면 null. */
    private final GroupRule group;

    /** The import rule whose media this list shows, or null. / 이 목록이 미디어를 보여 주는 import 규칙. 없으면 null. */
    private final ImportRule importRule;

    /**
     * Creates the media list of a sheet.
     * <p>
     * 시트의 미디어 목록을 만든다.
     *
     * @param sheet the sheet / 시트
     */
    MediaList(StyleSheet sheet) {
        this.sheet = sheet;
        this.group = null;
        this.importRule = null;
    }

    /**
     * Creates the media list of an <code>@media</code> rule.
     * <p>
     * <code>@media</code> 규칙의 미디어 목록을 만든다.
     *
     * @param group the rule / 규칙
     */
    MediaList(GroupRule group) {
        this.sheet = null;
        this.group = group;
        this.importRule = null;
    }

    /**
     * Creates the media list of an <code>@import</code> rule.
     * <p>
     * <code>@import</code> 규칙의 미디어 목록을 만든다.
     *
     * @param importRule the rule / 규칙
     */
    MediaList(ImportRule importRule) {
        this.sheet = null;
        this.group = null;
        this.importRule = importRule;
    }

    /**
     * Returns the media query list of the owner.
     * <p>
     * 소유자의 미디어 질의 목록을 반환한다.
     *
     * @return the list / 목록
     */
    private MediaQueryList current() {
        return sheet != null ? sheet.getMedia() : group != null ? group.getMedia() : importRule.getMedia();
    }

    /**
     * Replaces the media query list of the owner and tells the sheet.
     * <p>
     * 소유자의 미디어 질의 목록을 바꾸고 시트에 알린다.
     *
     * @param m the new list / 새 목록
     */
    private void store(MediaQueryList m) {
        StyleSheet owning;
        if (sheet != null) {
            sheet.setMedia(m);
            owning = sheet;
        } else if (group != null) {
            group.setMedia(m);
            owning = group.getParentStyleSheet();
        } else {
            importRule.setMedia(m);
            owning = importRule.getParentStyleSheet();
        }
        if (owning != null) {
            owning.markModified();
        }
    }

    /**
     * Returns the media queries as text (CSSOM <code>mediaText</code>): the queries separated by a comma and a space, the empty string for an empty list.
     * <p>
     * 미디어 질의를 텍스트로 반환한다(CSSOM <code>mediaText</code>): 쉼표와 공백으로 구분한 질의. 빈 목록이면 빈 문자열.
     *
     * @return the text / 텍스트
     */
    public String getMediaText() {
        MediaQueryList m = current();
        return m.isEmptyList() ? "" : m.toCssText();
    }

    /**
     * Replaces the media queries by the parsed text (CSSOM <code>mediaText</code> setter); a query that cannot be parsed becomes <code>not all</code>.
     * <p>
     * 미디어 질의를 파싱한 텍스트로 바꾼다(CSSOM <code>mediaText</code> 설정). 파싱할 수 없는 질의는 <code>not all</code> 이 된다.
     *
     * @param text the new media queries / 새 미디어 질의
     */
    public void setMediaText(String text) {
        store(MediaQueryList.parse(text == null ? "" : text));
    }

    /**
     * Returns the number of media queries (CSSOM <code>length</code>).
     * <p>
     * 미디어 질의 수를 반환한다(CSSOM <code>length</code>).
     *
     * @return the count, 0 for an empty list / 개수. 빈 목록이면 0
     */
    public int getLength() {
        return current().toQueryTexts().size();
    }

    /**
     * Returns the media query at an index as text (CSSOM <code>item</code>).
     * <p>
     * 위치의 미디어 질의를 텍스트로 반환한다(CSSOM <code>item</code>).
     *
     * @param index the position / 위치
     * @return the query, or null if the index is out of range / 질의. 위치가 범위를 벗어나면 null
     */
    public String item(int index) {
        List<String> t = current().toQueryTexts();
        return index >= 0 && index < t.size() ? t.get(index) : null;
    }

    /**
     * Parses one medium; returns its serialized form, or null if the text is not a single valid media query.
     * <p>
     * 미디어 하나를 파싱한다. 직렬화한 형태를 반환하며, 올바른 미디어 질의 하나가 아니면 null.
     *
     * @param medium the text / 텍스트
     * @return the query text or null / 질의 문자열 또는 null
     */
    private static String single(String medium) {
        String trimmed = medium == null ? "" : medium.trim();
        List<String> t = MediaQueryList.parse(trimmed).toQueryTexts();
        // 질의가 정확히 하나여야 하며, 파싱 실패가 만든 not all 은 입력이 not all 일 때만 올바르다.
        if (t.size() != 1 || (t.get(0).equals("not all") && !trimmed.equalsIgnoreCase("not all"))) {
            return null;
        }
        return t.get(0);
    }

    /**
     * Appends a medium unless the list already has it (CSSOM <code>appendMedium</code>); a text that is not a media query is ignored.
     * <p>
     * 목록에 없으면 미디어를 덧붙인다(CSSOM <code>appendMedium</code>). 미디어 질의가 아닌 텍스트는 무시한다.
     *
     * @param medium the media query / 미디어 질의
     */
    public void appendMedium(String medium) {
        String q = single(medium);
        if (q == null) {
            return;
        }
        List<String> t = current().toQueryTexts();
        // 같은 질의가 이미 있으면(대소문자 무시) 덧붙이지 않는다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).equalsIgnoreCase(q)) {
                return;
            }
        }
        store(MediaQueryList.parse(t.isEmpty() ? q : getMediaText() + ", " + q));
    }

    /**
     * Removes a medium from the list (CSSOM <code>deleteMedium</code>).
     * <p>
     * 목록에서 미디어를 지운다(CSSOM <code>deleteMedium</code>).
     *
     * @param medium the media query; text that is not a media query does nothing / 미디어 질의. 미디어 질의가 아닌 텍스트는 아무것도 하지 않는다
     * @throws DomException <code>NotFoundError</code> if the list does not have the medium / 목록에 그 미디어가 없으면 <code>NotFoundError</code>
     */
    public void deleteMedium(String medium) {
        String q = single(medium);
        // 미디어 질의로 파싱할 수 없는 텍스트는 CSSOM 대로 아무것도 하지 않는다.
        if (q == null) {
            return;
        }
        List<String> t = current().toQueryTexts();
        StringBuilder rest = new StringBuilder();
        boolean found = false;
        // 같은 질의를 모두 빼고 나머지를 이어 쓴다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).equalsIgnoreCase(q)) {
                found = true;
            } else {
                rest.append(rest.length() > 0 ? ", " : "").append(t.get(i));
            }
        }
        if (!found) {
            throw DomException.notFound("The media list does not contain '" + medium + "'.");
        }
        store(MediaQueryList.parse(rest.toString()));
    }

    /**
     * Returns the media text (the CSSOM stringifier).
     * <p>
     * 미디어 텍스트를 반환한다(CSSOM 문자열화).
     *
     * @return the text / 텍스트
     */
    public String toString() {
        return getMediaText();
    }
}
