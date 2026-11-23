package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One token produced by the {@link HtmlTokenizer}: a DOCTYPE, a start tag, an end tag, a comment, a run of characters or the end of
 * the file. Character tokens are runs (not single characters) to keep the number of objects small; the tree builder splits them where
 * its rules need single characters. A token is not modified after the tokenizer has handed it out, so the tree builder may keep
 * references to it (the list of active formatting elements does).
 * <p>
 * {@link HtmlTokenizer} 가 만드는 토큰 하나: DOCTYPE, 시작 태그, 종료 태그, 주석, 문자 연속 또는 파일 끝. 문자 토큰은 객체 수를 줄이려고 한 글자가 아닌
 * 연속 구간이며, 트리 생성기가 규칙상 한 글자씩 필요할 때 나눈다. 토큰은 토큰화기가 내준 뒤에는 바뀌지 않으므로 트리 생성기가 참조를 보관해도 된다
 * (활성 서식 요소 목록이 그렇게 한다).
 */
public final class HtmlToken {

    /**
     * Token type: DOCTYPE.
     * <p>
     * 토큰 종류: DOCTYPE.
     */
    public static final int DOCTYPE = 1;

    /**
     * Token type: start tag.
     * <p>
     * 토큰 종류: 시작 태그.
     */
    public static final int START_TAG = 2;

    /**
     * Token type: end tag.
     * <p>
     * 토큰 종류: 종료 태그.
     */
    public static final int END_TAG = 3;

    /**
     * Token type: comment.
     * <p>
     * 토큰 종류: 주석.
     */
    public static final int COMMENT = 4;

    /**
     * Token type: run of characters.
     * <p>
     * 토큰 종류: 문자 연속 구간.
     */
    public static final int CHARACTER = 5;

    /**
     * Token type: end of file.
     * <p>
     * 토큰 종류: 파일 끝.
     */
    public static final int EOF = 6;

    /**
     * Token type, one of the type constants.
     * <p>
     * 토큰 종류. 종류 상수 중 하나.
     */
    final int type;

    /**
     * Tag name (lower case) or DOCTYPE name (lower case, null if missing).
     * <p>
     * 태그 이름(소문자) 또는 DOCTYPE 이름(소문자, 없으면 null).
     */
    String name;

    /**
     * Comment text or the characters of a character token.
     * <p>
     * 주석 텍스트 또는 문자 토큰의 문자들.
     */
    String data;

    /**
     * Attribute names in source order (duplicates removed), or null if there are none.
     * <p>
     * 소스 순서의 속성 이름(중복 제거됨). 없으면 null.
     */
    List<String> attrNames;

    /**
     * Attribute values in the same order as {@link #attrNames}, or null.
     * <p>
     * {@link #attrNames} 와 같은 순서의 속성 값. 없으면 null.
     */
    List<String> attrValues;

    /**
     * True if a start tag was written with a trailing solidus (<code>&lt;br/&gt;</code>).
     * <p>
     * 시작 태그를 끝 슬래시와 함께 썼으면(<code>&lt;br/&gt;</code>) true.
     */
    boolean selfClosing;

    /**
     * DOCTYPE force-quirks flag.
     * <p>
     * DOCTYPE 의 force-quirks 플래그.
     */
    boolean forceQuirks;

    /**
     * DOCTYPE public identifier, null if missing.
     * <p>
     * DOCTYPE 공개 식별자. 없으면 null.
     */
    String publicId;

    /**
     * DOCTYPE system identifier, null if missing.
     * <p>
     * DOCTYPE 시스템 식별자. 없으면 null.
     */
    String systemId;

    /**
     * Offset of the first character of the token in the input.
     * <p>
     * 입력에서 토큰의 첫 글자 위치.
     */
    int offset;

    /**
     * Tag id of {@link #name} (see {@link HtmlTags}), computed by the tree builder on first use; -1 until then.
     * <p>
     * {@link #name} 의 태그 번호({@link HtmlTags} 참조). 트리 생성기가 처음 쓸 때 계산하며 그 전에는 -1.
     */
    int tagId = -1;

    /**
     * Set by the tree builder when it handled the self-closing flag of a start tag ("acknowledge the token's self-closing flag").
     * <p>
     * 트리 생성기가 시작 태그의 self-closing 플래그를 처리했을 때("acknowledge the token's self-closing flag") 지정한다.
     */
    boolean selfClosingAcked;

    /**
     * Creates an empty token of a type.
     * <p>
     * 종류만 정한 빈 토큰을 만든다.
     *
     * @param type token type / 토큰 종류
     */
    HtmlToken(int type) {
        this.type = type;
    }

    /**
     * Returns the token type.
     * <p>
     * 토큰 종류를 반환한다.
     *
     * @return one of the type constants / 종류 상수 중 하나
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the tag name or the DOCTYPE name.
     * <p>
     * 태그 이름 또는 DOCTYPE 이름을 반환한다.
     *
     * @return name, or null / 이름 또는 null
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the comment text or the characters.
     * <p>
     * 주석 텍스트 또는 문자들을 반환한다.
     *
     * @return data, or null / 데이터 또는 null
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the number of attributes of a tag.
     * <p>
     * 태그의 속성 수를 반환한다.
     *
     * @return count / 개수
     */
    public int getAttributeCount() {
        return attrNames == null ? 0 : attrNames.size();
    }

    /**
     * Returns the name of an attribute.
     * <p>
     * 속성의 이름을 반환한다.
     *
     * @param index index / 위치
     * @return name / 이름
     */
    public String getAttributeName(int index) {
        return attrNames.get(index);
    }

    /**
     * Returns the value of an attribute.
     * <p>
     * 속성의 값을 반환한다.
     *
     * @param index index / 위치
     * @return value / 값
     */
    public String getAttributeValue(int index) {
        return attrValues.get(index);
    }

    /**
     * Returns the value of the attribute with a name.
     * <p>
     * 이름이 같은 속성의 값을 반환한다.
     *
     * @param attributeName attribute name (lower case) / 속성 이름(소문자)
     * @return value, or null if there is no such attribute / 값. 그런 속성이 없으면 null
     */
    public String getAttribute(String attributeName) {
        if (attrNames == null) {
            return null;
        }
        int i = attrNames.indexOf(attributeName);
        return i < 0 ? null : attrValues.get(i);
    }

    /**
     * Tells whether a start tag was self-closing.
     * <p>
     * 시작 태그가 자기 닫기였는지 알려 준다.
     *
     * @return true if written with a trailing solidus / 끝 슬래시와 함께 썼으면 true
     */
    public boolean isSelfClosing() {
        return selfClosing;
    }

    /**
     * Returns the DOCTYPE public identifier.
     * <p>
     * DOCTYPE 공개 식별자를 반환한다.
     *
     * @return identifier, or null if missing / 식별자. 없으면 null
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Returns the DOCTYPE system identifier.
     * <p>
     * DOCTYPE 시스템 식별자를 반환한다.
     *
     * @return identifier, or null if missing / 식별자. 없으면 null
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the DOCTYPE force-quirks flag.
     * <p>
     * DOCTYPE 의 force-quirks 플래그를 반환한다.
     *
     * @return flag / 플래그
     */
    public boolean isForceQuirks() {
        return forceQuirks;
    }

    /**
     * Returns the offset of the token in the input.
     * <p>
     * 입력에서 토큰의 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Adds an attribute (the caller has checked for duplicates).
     * <p>
     * 속성을 추가한다(중복 검사는 호출자가 했다).
     *
     * @param n attribute name / 속성 이름
     * @param v attribute value / 속성 값
     */
    void addAttribute(String n, String v) {
        if (attrNames == null) {
            attrNames = new ArrayList<String>(4);
            attrValues = new ArrayList<String>(4);
        }
        attrNames.add(n);
        attrValues.add(v);
    }

    /**
     * Returns a start tag token with the same name and attributes but another name (the "image" to "img" change).
     * <p>
     * 이름만 다르고 속성은 같은 시작 태그 토큰을 반환한다("image" 를 "img" 로 바꾸는 데 쓴다).
     *
     * @param newName new tag name / 새 태그 이름
     * @return the copy / 복사본
     */
    HtmlToken renamed(String newName) {
        HtmlToken t = new HtmlToken(type);
        t.name = newName;
        t.attrNames = attrNames;
        t.attrValues = attrValues;
        t.selfClosing = selfClosing;
        t.offset = offset;
        return t;
    }

    /**
     * Returns a token of the same type and name without any attribute (used when the standard says "with no attributes").
     * <p>
     * 종류와 이름이 같고 속성이 없는 토큰을 반환한다(표준이 "속성 없이" 라고 할 때 쓴다).
     *
     * @param tagType token type / 토큰 종류
     * @param tagName tag name / 태그 이름
     * @param at      offset / 위치
     * @return the token / 토큰
     */
    static HtmlToken synthetic(int tagType, String tagName, int at) {
        HtmlToken t = new HtmlToken(tagType);
        t.name = tagName;
        t.offset = at;
        return t;
    }

    /**
     * Returns an unmodifiable view of the attribute names, for diagnostics.
     * <p>
     * 진단용으로 속성 이름의 수정할 수 없는 뷰를 반환한다.
     *
     * @return names / 이름
     */
    public List<String> getAttributeNames() {
        return attrNames == null ? Collections.<String>emptyList() : Collections.unmodifiableList(attrNames);
    }

    /**
     * Returns a short description for diagnostics.
     * <p>
     * 진단용 짧은 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        switch (type) {
        case DOCTYPE:
            return "DOCTYPE " + name;
        case START_TAG:
            return "<" + name + ">";
        case END_TAG:
            return "</" + name + ">";
        case COMMENT:
            return "<!--" + data + "-->";
        case CHARACTER:
            return "chars(" + data + ")";
        default:
            return "EOF";
        }
    }
}
