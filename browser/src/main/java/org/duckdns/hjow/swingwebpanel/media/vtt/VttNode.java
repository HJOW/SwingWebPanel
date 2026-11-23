package org.duckdns.hjow.swingwebpanel.media.vtt;

import java.util.ArrayList;
import java.util.List;

/**
 * A WebVTT node object (WebVTT 6.4): a node of the tree that the cue text parser builds. An internal node (the root list, a class span, italic,
 * bold, underline, ruby, ruby text, voice or language span) has children, applicable classes and an applicable language; a leaf is a text or a
 * timestamp. The fields are public because the class is a plain record.
 * <p>
 * WebVTT 노드 객체(WebVTT 6.4): cue 텍스트 파서가 만드는 트리의 노드. 내부 노드(루트 목록, 클래스/기울임/굵게/밑줄/루비/루비 텍스트/목소리/언어 span)는
 * 자식, 적용되는 클래스, 적용되는 언어를 가지며, 잎은 텍스트 또는 타임스탬프이다. 클래스는 단순한 기록이므로 필드는 public 이다.
 */
public final class VttNode {

    /**
     * The kind: the root list of nodes.
     * <p>
     * 종류: 노드의 루트 목록.
     */
    public static final int LIST = 0;

    /**
     * The kind: a class span (<code>c</code>).
     * <p>
     * 종류: 클래스 span(<code>c</code>).
     */
    public static final int CLASS = 1;

    /**
     * The kind: italic (<code>i</code>).
     * <p>
     * 종류: 기울임(<code>i</code>).
     */
    public static final int ITALIC = 2;

    /**
     * The kind: bold (<code>b</code>).
     * <p>
     * 종류: 굵게(<code>b</code>).
     */
    public static final int BOLD = 3;

    /**
     * The kind: underline (<code>u</code>).
     * <p>
     * 종류: 밑줄(<code>u</code>).
     */
    public static final int UNDERLINE = 4;

    /**
     * The kind: ruby (<code>ruby</code>).
     * <p>
     * 종류: 루비(<code>ruby</code>).
     */
    public static final int RUBY = 5;

    /**
     * The kind: ruby text (<code>rt</code>).
     * <p>
     * 종류: 루비 텍스트(<code>rt</code>).
     */
    public static final int RUBY_TEXT = 6;

    /**
     * The kind: a voice span (<code>v</code>) whose {@link #value} is the name of the voice.
     * <p>
     * 종류: 목소리 span(<code>v</code>). {@link #value} 가 목소리의 이름이다.
     */
    public static final int VOICE = 7;

    /**
     * The kind: a language span (<code>lang</code>).
     * <p>
     * 종류: 언어 span(<code>lang</code>).
     */
    public static final int LANGUAGE = 8;

    /**
     * The kind: a text; {@link #value} is the text.
     * <p>
     * 종류: 텍스트. {@link #value} 가 텍스트이다.
     */
    public static final int TEXT = 9;

    /**
     * The kind: a timestamp; {@link #time} is the time.
     * <p>
     * 종류: 타임스탬프. {@link #time} 이 시각이다.
     */
    public static final int TIMESTAMP = 10;

    /**
     * The kind of the node, one of the constants of this class.
     * <p>
     * 노드의 종류(이 클래스의 상수 중 하나).
     */
    public final int kind;

    /**
     * The children of an internal node (empty for a leaf).
     * <p>
     * 내부 노드의 자식(잎은 비어 있음).
     */
    public final List<VttNode> children = new ArrayList<VttNode>();

    /**
     * The parent, or null for the root.
     * <p>
     * 부모. 루트는 null.
     */
    public VttNode parent;

    /**
     * The applicable classes of an internal node.
     * <p>
     * 내부 노드에 적용되는 클래스.
     */
    public final List<String> classes = new ArrayList<String>();

    /**
     * The applicable language of an internal node, or null.
     * <p>
     * 내부 노드에 적용되는 언어. 없으면 null.
     */
    public String language;

    /**
     * The text of a text node, or the voice of a voice span.
     * <p>
     * 텍스트 노드의 텍스트 또는 목소리 span 의 목소리.
     */
    public String value = "";

    /**
     * The time of a timestamp node in seconds.
     * <p>
     * 타임스탬프 노드의 시각(초).
     */
    public double time;

    /**
     * Creates a node.
     * <p>
     * 노드를 만든다.
     *
     * @param kind the kind / 종류
     */
    public VttNode(int kind) {
        this.kind = kind;
    }

    /**
     * Appends a child.
     * <p>
     * 자식을 덧붙인다.
     *
     * @param child the child / 자식
     */
    void append(VttNode child) {
        child.parent = this;
        children.add(child);
    }

    /**
     * Tells whether the node is an internal node.
     * <p>
     * 노드가 내부 노드인지 알려 준다.
     *
     * @return false for a text or a timestamp / 텍스트나 타임스탬프이면 false
     */
    public boolean isInternal() {
        return kind != TEXT && kind != TIMESTAMP;
    }
}
