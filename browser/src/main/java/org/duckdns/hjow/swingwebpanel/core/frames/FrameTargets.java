package org.duckdns.hjow.swingwebpanel.core.frames;

/**
 * Classifies the name that selects a browsing context in <code>target</code> attributes and <code>window.open</code>: the keywords
 * <code>_self</code>, <code>_parent</code>, <code>_top</code> and <code>_blank</code> (ASCII case-insensitive) and plain names (HTML5 REC
 * 2014-10-28 5.1 "rules for choosing a browsing context"). Which context is chosen for a name is decided by the frame tree, not here.
 * <p>
 * <code>target</code> 속성과 <code>window.open</code> 에서 브라우징 문맥을 고르는 이름을 분류한다: 키워드 <code>_self</code>, <code>_parent</code>,
 * <code>_top</code>, <code>_blank</code>(ASCII 대소문자 무시)와 일반 이름(HTML5 REC 2014-10-28 5.1 "rules for choosing a browsing context").
 * 이름으로 어떤 문맥이 선택되는지는 프레임 트리가 정하며 여기서 정하지 않는다.
 */
public final class FrameTargets {

    /**
     * The current browsing context (<code>_self</code>, or an empty name).
     * <p>
     * 현재 브라우징 문맥(<code>_self</code> 또는 빈 이름).
     */
    public static final int SELF = 0;

    /**
     * The parent browsing context (<code>_parent</code>).
     * <p>
     * 부모 브라우징 문맥(<code>_parent</code>).
     */
    public static final int PARENT = 1;

    /**
     * The top-level browsing context (<code>_top</code>).
     * <p>
     * 최상위 브라우징 문맥(<code>_top</code>).
     */
    public static final int TOP = 2;

    /**
     * A new browsing context (<code>_blank</code>).
     * <p>
     * 새 브라우징 문맥(<code>_blank</code>).
     */
    public static final int BLANK = 3;

    /**
     * A browsing context that has this name.
     * <p>
     * 이 이름을 가진 브라우징 문맥.
     */
    public static final int NAMED = 4;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FrameTargets() {
    }

    /**
     * Classifies a target name. A null or empty name selects the current context.
     * <p>
     * 대상 이름을 분류한다. null 이나 빈 이름은 현재 문맥을 고른다.
     *
     * @param name the target name or null / 대상 이름 또는 null
     * @return {@link #SELF}, {@link #PARENT}, {@link #TOP}, {@link #BLANK} or {@link #NAMED} / 분류 결과
     */
    public static int classify(String name) {
        if (name == null || name.length() == 0) {
            return SELF;
        }
        if ("_self".equalsIgnoreCase(name)) {
            return SELF;
        }
        if ("_parent".equalsIgnoreCase(name)) {
            return PARENT;
        }
        if ("_top".equalsIgnoreCase(name)) {
            return TOP;
        }
        if ("_blank".equalsIgnoreCase(name)) {
            return BLANK;
        }
        return NAMED;
    }

    /**
     * Tells whether a string is a valid browsing context name: at least one character and not starting with an underscore.
     * <p>
     * 문자열이 유효한 브라우징 문맥 이름인지 알려 준다: 한 글자 이상이며 밑줄로 시작하지 않는다.
     *
     * @param name the candidate / 후보
     * @return true if it is valid / 유효하면 true
     */
    public static boolean isValidName(String name) {
        return name != null && name.length() > 0 && name.charAt(0) != '_';
    }
}
