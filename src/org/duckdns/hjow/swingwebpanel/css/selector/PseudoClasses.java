package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The registry of the pseudo-classes and pseudo-elements the selector parser knows. Each pseudo-class has a numeric id (used by the matcher
 * instead of string comparisons) and an argument kind that tells the parser what its parentheses may contain. A name that is not in the registry
 * makes the whole selector invalid, exactly as in a browser. Some pseudo-classes are known and valid but can never match in this engine
 * (for example <code>:fullscreen</code>); they are registered with {@link #NEVER}.
 * <p>
 * 선택자 파서가 아는 의사 클래스와 의사 요소의 등록부. 각 의사 클래스에는 숫자 id(매처가 문자열 비교 대신 사용)와 괄호 안에 올 수 있는 내용을
 * 알려 주는 인수 종류가 있다. 등록부에 없는 이름은 브라우저처럼 선택자 전체를 무효로 만든다. 일부 의사 클래스(예: <code>:fullscreen</code>)는
 * 올바르지만 이 엔진에서는 일치할 수 없으며 {@link #NEVER} 로 등록한다.
 */
public final class PseudoClasses {

    /** Id: <code>:root</code>. / 의사 클래스 id: <code>:root</code>. */
    public static final int ROOT = 1;
    /** Id: <code>:empty</code>. / 의사 클래스 id: <code>:empty</code>. */
    public static final int EMPTY = 2;
    /** Id: <code>:first-child</code>. / 의사 클래스 id: <code>:first-child</code>. */
    public static final int FIRST_CHILD = 3;
    /** Id: <code>:last-child</code>. / 의사 클래스 id: <code>:last-child</code>. */
    public static final int LAST_CHILD = 4;
    /** Id: <code>:only-child</code>. / 의사 클래스 id: <code>:only-child</code>. */
    public static final int ONLY_CHILD = 5;
    /** Id: <code>:first-of-type</code>. / 의사 클래스 id: <code>:first-of-type</code>. */
    public static final int FIRST_OF_TYPE = 6;
    /** Id: <code>:last-of-type</code>. / 의사 클래스 id: <code>:last-of-type</code>. */
    public static final int LAST_OF_TYPE = 7;
    /** Id: <code>:only-of-type</code>. / 의사 클래스 id: <code>:only-of-type</code>. */
    public static final int ONLY_OF_TYPE = 8;
    /** Id: <code>:nth-child()</code>. / 의사 클래스 id: <code>:nth-child()</code>. */
    public static final int NTH_CHILD = 9;
    /** Id: <code>:nth-last-child()</code>. / 의사 클래스 id: <code>:nth-last-child()</code>. */
    public static final int NTH_LAST_CHILD = 10;
    /** Id: <code>:nth-of-type()</code>. / 의사 클래스 id: <code>:nth-of-type()</code>. */
    public static final int NTH_OF_TYPE = 11;
    /** Id: <code>:nth-last-of-type()</code>. / 의사 클래스 id: <code>:nth-last-of-type()</code>. */
    public static final int NTH_LAST_OF_TYPE = 12;
    /** Id: <code>:not()</code>. / 의사 클래스 id: <code>:not()</code>. */
    public static final int NOT = 13;
    /** Id: <code>:is()</code> (and <code>:matches()</code>, <code>:-webkit-any()</code>). / 의사 클래스 id: <code>:is()</code>(와 <code>:matches()</code>, <code>:-webkit-any()</code>). */
    public static final int IS = 14;
    /** Id: <code>:where()</code>. / 의사 클래스 id: <code>:where()</code>. */
    public static final int WHERE = 15;
    /** Id: <code>:has()</code>. / 의사 클래스 id: <code>:has()</code>. */
    public static final int HAS = 16;
    /** Id: <code>:hover</code>. / 의사 클래스 id: <code>:hover</code>. */
    public static final int HOVER = 17;
    /** Id: <code>:active</code>. / 의사 클래스 id: <code>:active</code>. */
    public static final int ACTIVE = 18;
    /** Id: <code>:focus</code>. / 의사 클래스 id: <code>:focus</code>. */
    public static final int FOCUS = 19;
    /** Id: <code>:focus-visible</code>. / 의사 클래스 id: <code>:focus-visible</code>. */
    public static final int FOCUS_VISIBLE = 20;
    /** Id: <code>:focus-within</code>. / 의사 클래스 id: <code>:focus-within</code>. */
    public static final int FOCUS_WITHIN = 21;
    /** Id: <code>:link</code>. / 의사 클래스 id: <code>:link</code>. */
    public static final int LINK = 22;
    /** Id: <code>:any-link</code>. / 의사 클래스 id: <code>:any-link</code>. */
    public static final int ANY_LINK = 23;
    /** Id: <code>:visited</code>. / 의사 클래스 id: <code>:visited</code>. */
    public static final int VISITED = 24;
    /** Id: <code>:target</code>. / 의사 클래스 id: <code>:target</code>. */
    public static final int TARGET = 25;
    /** Id: <code>:scope</code>. / 의사 클래스 id: <code>:scope</code>. */
    public static final int SCOPE = 26;
    /** Id: <code>:enabled</code>. / 의사 클래스 id: <code>:enabled</code>. */
    public static final int ENABLED = 27;
    /** Id: <code>:disabled</code>. / 의사 클래스 id: <code>:disabled</code>. */
    public static final int DISABLED = 28;
    /** Id: <code>:checked</code>. / 의사 클래스 id: <code>:checked</code>. */
    public static final int CHECKED = 29;
    /** Id: <code>:indeterminate</code>. / 의사 클래스 id: <code>:indeterminate</code>. */
    public static final int INDETERMINATE = 30;
    /** Id: <code>:default</code>. / 의사 클래스 id: <code>:default</code>. */
    public static final int DEFAULT = 31;
    /** Id: <code>:required</code>. / 의사 클래스 id: <code>:required</code>. */
    public static final int REQUIRED = 32;
    /** Id: <code>:optional</code>. / 의사 클래스 id: <code>:optional</code>. */
    public static final int OPTIONAL = 33;
    /** Id: <code>:read-only</code>. / 의사 클래스 id: <code>:read-only</code>. */
    public static final int READ_ONLY = 34;
    /** Id: <code>:read-write</code>. / 의사 클래스 id: <code>:read-write</code>. */
    public static final int READ_WRITE = 35;
    /** Id: <code>:placeholder-shown</code>. / 의사 클래스 id: <code>:placeholder-shown</code>. */
    public static final int PLACEHOLDER_SHOWN = 36;
    /** Id: <code>:valid</code>. / 의사 클래스 id: <code>:valid</code>. */
    public static final int VALID = 37;
    /** Id: <code>:invalid</code>. / 의사 클래스 id: <code>:invalid</code>. */
    public static final int INVALID = 38;
    /** Id: <code>:in-range</code>. / 의사 클래스 id: <code>:in-range</code>. */
    public static final int IN_RANGE = 39;
    /** Id: <code>:out-of-range</code>. / 의사 클래스 id: <code>:out-of-range</code>. */
    public static final int OUT_OF_RANGE = 40;
    /** Id: <code>:lang()</code>. / 의사 클래스 id: <code>:lang()</code>. */
    public static final int LANG = 41;
    /** Id: <code>:dir()</code>. / 의사 클래스 id: <code>:dir()</code>. */
    public static final int DIR = 42;
    /** Id: <code>:defined</code>. / 의사 클래스 id: <code>:defined</code>. */
    public static final int DEFINED = 43;
    /** Id: <code>:open</code>. / 의사 클래스 id: <code>:open</code>. */
    public static final int OPEN = 44;
    /** Id: <code>:modal</code> (a dialog in the top layer). / 의사 클래스 id: <code>:modal</code>(최상위 계층의 대화상자). */
    public static final int MODAL = 45;
    /** Id: valid pseudo-classes that never match in this engine. / 의사 클래스 id: 올바르지만 이 엔진에서는 일치하지 않는 의사 클래스. */
    public static final int NEVER = 99;

    /** Argument kind: no parentheses. / 인수 종류: 괄호 없음. */
    public static final int ARG_NONE = 0;
    /** Argument kind: An+B only. / 인수 종류: An+B 만. */
    public static final int ARG_NTH = 1;
    /** Argument kind: An+B with an optional <code>of S</code> clause. / 인수 종류: 선택적 <code>of S</code> 절이 붙은 An+B. */
    public static final int ARG_NTH_OF = 2;
    /** Argument kind: forgiving selector list (invalid entries are ignored). / 인수 종류: 관대한 선택자 목록(잘못된 항목은 무시). */
    public static final int ARG_FORGIVING = 3;
    /** Argument kind: strict selector list. / 인수 종류: 엄격한 선택자 목록. */
    public static final int ARG_LIST = 4;
    /** Argument kind: relative selector list. / 인수 종류: 상대 선택자 목록. */
    public static final int ARG_RELATIVE = 5;
    /** Argument kind: language ranges. / 인수 종류: 언어 범위. */
    public static final int ARG_LANG = 6;
    /** Argument kind: a direction keyword. / 인수 종류: 방향 키워드. */
    public static final int ARG_DIR = 7;
    /** Argument kind: any component values (accepted and ignored). / 인수 종류: 임의의 컴포넌트 값(받아들이고 무시). */
    public static final int ARG_ANY = 8;

    /** Name to id table. / 이름에서 id 로 가는 표. */
    private static final Map<String, int[]> TABLE = new HashMap<String, int[]>();

    /** Pseudo-element names (without colons). / 의사 요소 이름(콜론 없음). */
    private static final Set<String> ELEMENTS = new HashSet<String>();

    /** Pseudo-elements that may be written with a single colon for compatibility. / 호환을 위해 콜론 하나로 쓸 수 있는 의사 요소. */
    private static final Set<String> LEGACY_ELEMENTS = new HashSet<String>();

    static {
        add("root", ROOT, ARG_NONE);
        add("empty", EMPTY, ARG_NONE);
        add("first-child", FIRST_CHILD, ARG_NONE);
        add("last-child", LAST_CHILD, ARG_NONE);
        add("only-child", ONLY_CHILD, ARG_NONE);
        add("first-of-type", FIRST_OF_TYPE, ARG_NONE);
        add("last-of-type", LAST_OF_TYPE, ARG_NONE);
        add("only-of-type", ONLY_OF_TYPE, ARG_NONE);
        add("nth-child", NTH_CHILD, ARG_NTH_OF);
        add("nth-last-child", NTH_LAST_CHILD, ARG_NTH_OF);
        add("nth-of-type", NTH_OF_TYPE, ARG_NTH);
        add("nth-last-of-type", NTH_LAST_OF_TYPE, ARG_NTH);
        add("not", NOT, ARG_LIST);
        add("is", IS, ARG_FORGIVING);
        add("matches", IS, ARG_FORGIVING);
        add("-webkit-any", IS, ARG_FORGIVING);
        add("-moz-any", IS, ARG_FORGIVING);
        add("where", WHERE, ARG_FORGIVING);
        add("has", HAS, ARG_RELATIVE);
        add("hover", HOVER, ARG_NONE);
        add("active", ACTIVE, ARG_NONE);
        add("focus", FOCUS, ARG_NONE);
        add("focus-visible", FOCUS_VISIBLE, ARG_NONE);
        add("focus-within", FOCUS_WITHIN, ARG_NONE);
        add("link", LINK, ARG_NONE);
        add("any-link", ANY_LINK, ARG_NONE);
        add("visited", VISITED, ARG_NONE);
        add("target", TARGET, ARG_NONE);
        add("scope", SCOPE, ARG_NONE);
        add("enabled", ENABLED, ARG_NONE);
        add("disabled", DISABLED, ARG_NONE);
        add("checked", CHECKED, ARG_NONE);
        add("indeterminate", INDETERMINATE, ARG_NONE);
        add("default", DEFAULT, ARG_NONE);
        add("required", REQUIRED, ARG_NONE);
        add("optional", OPTIONAL, ARG_NONE);
        add("read-only", READ_ONLY, ARG_NONE);
        add("read-write", READ_WRITE, ARG_NONE);
        add("placeholder-shown", PLACEHOLDER_SHOWN, ARG_NONE);
        add("valid", VALID, ARG_NONE);
        add("invalid", INVALID, ARG_NONE);
        add("in-range", IN_RANGE, ARG_NONE);
        add("out-of-range", OUT_OF_RANGE, ARG_NONE);
        add("lang", LANG, ARG_LANG);
        add("dir", DIR, ARG_DIR);
        add("defined", DEFINED, ARG_NONE);
        add("open", OPEN, ARG_NONE);
        add("modal", MODAL, ARG_NONE);
        String[] never = {"fullscreen", "popover-open", "picture-in-picture", "playing", "paused", "seeking", "buffering", "stalled",
            "muted", "volume-locked", "autofill", "-webkit-autofill", "target-within", "local-link", "current", "past", "future",
            "user-valid", "user-invalid", "blank", "-moz-focusring", "-moz-placeholder", "-ms-input-placeholder"};
        // 항상 일치하지 않지만 문법상 올바른 의사 클래스를 등록한다.
        for (int i = 0; i < never.length; i++) {
            add(never[i], NEVER, ARG_NONE);
        }
        add("host", NEVER, ARG_ANY);
        add("host-context", NEVER, ARG_ANY);
        add("state", NEVER, ARG_ANY);
        String[] elems = {"before", "after", "first-line", "first-letter", "marker", "placeholder", "selection", "backdrop",
            "file-selector-button", "cue"};
        // 지원하는 의사 요소 이름을 등록한다.
        for (int i = 0; i < elems.length; i++) {
            ELEMENTS.add(elems[i]);
        }
        LEGACY_ELEMENTS.add("before");
        LEGACY_ELEMENTS.add("after");
        LEGACY_ELEMENTS.add("first-line");
        LEGACY_ELEMENTS.add("first-letter");
    }

    /**
     * Private constructor: static registry class.
     * <p>
     * 비공개 생성자: 정적 등록부 클래스이다.
     */
    private PseudoClasses() {
    }

    /**
     * Registers one pseudo-class.
     * <p>
     * 의사 클래스 하나를 등록한다.
     *
     * @param name lower-case name / 소문자 이름
     * @param id   numeric id / 숫자 id
     * @param arg  argument kind / 인수 종류
     */
    private static void add(String name, int id, int arg) {
        TABLE.put(name, new int[] {id, arg});
    }

    /**
     * Looks up a pseudo-class.
     * <p>
     * 의사 클래스를 찾는다.
     *
     * @param name lower-case name without the colon / 콜론 없는 소문자 이름
     * @return an array {id, argument kind}, or null if the name is unknown / {id, 인수 종류} 배열. 이름을 모르면 null
     */
    public static int[] lookup(String name) {
        return TABLE.get(name);
    }

    /**
     * Tells whether a name is a known pseudo-element.
     * <p>
     * 이름이 알려진 의사 요소인지 알려 준다.
     *
     * @param name lower-case name without the colons / 콜론 없는 소문자 이름
     * @return true if known / 알려져 있으면 true
     */
    public static boolean isPseudoElement(String name) {
        return ELEMENTS.contains(name);
    }

    /**
     * Tells whether a pseudo-element may be written with a single colon.
     * <p>
     * 의사 요소를 콜론 하나로 쓸 수 있는지 알려 준다.
     *
     * @param name lower-case name / 소문자 이름
     * @return true for the legacy forms / 이전 형식이면 true
     */
    public static boolean isLegacyPseudoElement(String name) {
        return LEGACY_ELEMENTS.contains(name);
    }
}
