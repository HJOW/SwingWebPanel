package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * One simple selector: a type or universal selector, an id, a class, an attribute selector or a pseudo-class. A compound selector is a sequence
 * of these. Instances are immutable; the parser builds them with the static factory methods.
 * <p>
 * 단순 선택자 하나: 타입 또는 전체 선택자, id, 클래스, 속성 선택자, 의사 클래스. 복합 선택자는 이들의 나열이다. 인스턴스는 불변이며
 * 파서가 정적 팩토리 메서드로 만든다.
 */
public final class SimpleSelector {

    /** Kind: type selector. / 종류: 타입 선택자. */
    public static final int TYPE = 1;
    /** Kind: universal selector. / 종류: 전체 선택자. */
    public static final int UNIVERSAL = 2;
    /** Kind: id selector. / 종류: id 선택자. */
    public static final int ID = 3;
    /** Kind: class selector. / 종류: 클래스 선택자. */
    public static final int CLASS = 4;
    /** Kind: attribute selector. / 종류: 속성 선택자. */
    public static final int ATTRIBUTE = 5;
    /** Kind: pseudo-class. / 종류: 의사 클래스. */
    public static final int PSEUDO_CLASS = 6;

    /** Attribute operator: presence. / 속성 연산자: 존재. */
    public static final int OP_EXISTS = 0;
    /** Attribute operator: equals. / 속성 연산자: 같음. */
    public static final int OP_EQUALS = 1;
    /** Attribute operator: whitespace separated list contains (<code>~=</code>). / 속성 연산자: 공백 구분 목록에 포함(<code>~=</code>). */
    public static final int OP_INCLUDES = 2;
    /** Attribute operator: equals or starts with value and a hyphen (<code>|=</code>). / 속성 연산자: 같거나 값과 하이픈으로 시작(<code>|=</code>). */
    public static final int OP_DASH = 3;
    /** Attribute operator: starts with (<code>^=</code>). / 속성 연산자: 로 시작(<code>^=</code>). */
    public static final int OP_PREFIX = 4;
    /** Attribute operator: ends with (<code>$=</code>). / 속성 연산자: 로 끝남(<code>$=</code>). */
    public static final int OP_SUFFIX = 5;
    /** Attribute operator: contains (<code>*=</code>). / 속성 연산자: 포함(<code>*=</code>). */
    public static final int OP_SUBSTRING = 6;

    /** Case mode: default for the attribute. / 대소문자 모드: 속성의 기본값. */
    public static final int CASE_DEFAULT = 0;
    /** Case mode: ASCII case-insensitive (<code>i</code>). / 대소문자 모드: ASCII 대소문자 무시(<code>i</code>). */
    public static final int CASE_INSENSITIVE = 1;
    /** Case mode: case-sensitive (<code>s</code>). / 대소문자 모드: 대소문자 구분(<code>s</code>). */
    public static final int CASE_SENSITIVE = 2;

    /** Specificity unit of an id. / id 의 구체성 단위. */
    public static final long SPEC_ID = 1L << 40;
    /** Specificity unit of a class, attribute or pseudo-class. / 클래스, 속성, 의사 클래스의 구체성 단위. */
    public static final long SPEC_CLASS = 1L << 20;
    /** Specificity unit of a type. / 타입의 구체성 단위. */
    public static final long SPEC_TYPE = 1L;

    /** Kind constant. / 종류 상수. */
    private final int kind;

    /** Local name, id, class name, attribute local name or pseudo-class name. / 지역 이름, id, 클래스 이름, 속성 지역 이름, 의사 클래스 이름. */
    private final String name;

    /** True if the namespace matches any namespace. / 모든 네임스페이스와 일치하면 true. */
    private final boolean nsAny;

    /** Namespace URI to match when not "any", null meaning no namespace. / "any" 가 아닐 때 일치할 네임스페이스 URI. null 이면 네임스페이스 없음. */
    private final String ns;

    /** Namespace text as written (for example <code>svg|</code>), or an empty string. / 쓰인 그대로의 네임스페이스 문자열(예: <code>svg|</code>) 또는 빈 문자열. */
    private final String nsText;

    /** Attribute operator constant. / 속성 연산자 상수. */
    private final int op;

    /** Attribute value. / 속성 값. */
    private final String value;

    /** Attribute case mode. / 속성 대소문자 모드. */
    private final int caseMode;

    /** Pseudo-class id (see {@link PseudoClasses}). / 의사 클래스 id({@link PseudoClasses} 참조). */
    private final int pseudoId;

    /** An+B argument of nth pseudo-classes. / nth 의사 클래스의 An+B 인수. */
    private final AnPlusB anb;

    /** Selector list argument. / 선택자 목록 인수. */
    private final SelectorList list;

    /** Language ranges or direction keyword. / 언어 범위 또는 방향 키워드. */
    private final List<String> words;

    /**
     * Creates a simple selector (private: use the factories).
     * <p>
     * 단순 선택자를 만든다(비공개: 팩토리를 쓴다).
     *
     * @param kind     kind / 종류
     * @param name     name / 이름
     * @param nsAny    any namespace / 모든 네임스페이스
     * @param ns       namespace URI / 네임스페이스 URI
     * @param nsText   namespace text / 네임스페이스 문자열
     * @param op       attribute operator / 속성 연산자
     * @param value    attribute value / 속성 값
     * @param caseMode attribute case mode / 속성 대소문자 모드
     * @param pseudoId pseudo-class id / 의사 클래스 id
     * @param anb      An+B / An+B
     * @param list     selector list / 선택자 목록
     * @param words    words / 단어들
     */
    private SimpleSelector(int kind, String name, boolean nsAny, String ns, String nsText, int op, String value, int caseMode, int pseudoId,
            AnPlusB anb, SelectorList list, List<String> words) {
        this.kind = kind;
        this.name = name;
        this.nsAny = nsAny;
        this.ns = ns;
        this.nsText = nsText;
        this.op = op;
        this.value = value;
        this.caseMode = caseMode;
        this.pseudoId = pseudoId;
        this.anb = anb;
        this.list = list;
        this.words = words == null ? null : Collections.unmodifiableList(new ArrayList<String>(words));
    }

    /**
     * Creates a type selector.
     * <p>
     * 타입 선택자를 만든다.
     *
     * @param name   element local name / 요소 지역 이름
     * @param nsAny  match any namespace / 모든 네임스페이스와 일치
     * @param ns     namespace URI or null / 네임스페이스 URI 또는 null
     * @param nsText namespace text as written / 쓰인 그대로의 네임스페이스 문자열
     * @return the selector / 선택자
     */
    public static SimpleSelector type(String name, boolean nsAny, String ns, String nsText) {
        return new SimpleSelector(TYPE, name, nsAny, ns, nsText, 0, null, 0, 0, null, null, null);
    }

    /**
     * Creates a universal selector.
     * <p>
     * 전체 선택자를 만든다.
     *
     * @param nsAny  match any namespace / 모든 네임스페이스와 일치
     * @param ns     namespace URI or null / 네임스페이스 URI 또는 null
     * @param nsText namespace text as written / 쓰인 그대로의 네임스페이스 문자열
     * @return the selector / 선택자
     */
    public static SimpleSelector universal(boolean nsAny, String ns, String nsText) {
        return new SimpleSelector(UNIVERSAL, "*", nsAny, ns, nsText, 0, null, 0, 0, null, null, null);
    }

    /**
     * Creates an id selector.
     * <p>
     * id 선택자를 만든다.
     *
     * @param id the id / id
     * @return the selector / 선택자
     */
    public static SimpleSelector id(String id) {
        return new SimpleSelector(ID, id, true, null, "", 0, null, 0, 0, null, null, null);
    }

    /**
     * Creates a class selector.
     * <p>
     * 클래스 선택자를 만든다.
     *
     * @param cls the class name / 클래스 이름
     * @return the selector / 선택자
     */
    public static SimpleSelector cls(String cls) {
        return new SimpleSelector(CLASS, cls, true, null, "", 0, null, 0, 0, null, null, null);
    }

    /**
     * Creates an attribute selector.
     * <p>
     * 속성 선택자를 만든다.
     *
     * @param local    attribute local name / 속성 지역 이름
     * @param nsAny    match any namespace / 모든 네임스페이스와 일치
     * @param ns       namespace URI or null for none / 네임스페이스 URI. 없으면 null
     * @param nsText   namespace text as written / 쓰인 그대로의 네임스페이스 문자열
     * @param op       operator constant / 연산자 상수
     * @param value    value or null / 값 또는 null
     * @param caseMode case mode constant / 대소문자 모드 상수
     * @return the selector / 선택자
     */
    public static SimpleSelector attribute(String local, boolean nsAny, String ns, String nsText, int op, String value, int caseMode) {
        return new SimpleSelector(ATTRIBUTE, local, nsAny, ns, nsText, op, value, caseMode, 0, null, null, null);
    }

    /**
     * Creates a pseudo-class without arguments.
     * <p>
     * 인수 없는 의사 클래스를 만든다.
     *
     * @param name display name (lower case) / 표시 이름(소문자)
     * @param id   pseudo-class id / 의사 클래스 id
     * @return the selector / 선택자
     */
    public static SimpleSelector pseudo(String name, int id) {
        return new SimpleSelector(PSEUDO_CLASS, name, true, null, "", 0, null, 0, id, null, null, null);
    }

    /**
     * Creates an nth pseudo-class.
     * <p>
     * nth 의사 클래스를 만든다.
     *
     * @param name display name / 표시 이름
     * @param id   pseudo-class id / 의사 클래스 id
     * @param anb  An+B / An+B
     * @param of   the <code>of S</code> list or null / <code>of S</code> 목록 또는 null
     * @return the selector / 선택자
     */
    public static SimpleSelector nth(String name, int id, AnPlusB anb, SelectorList of) {
        return new SimpleSelector(PSEUDO_CLASS, name, true, null, "", 0, null, 0, id, anb, of, null);
    }

    /**
     * Creates a pseudo-class with a selector list argument.
     * <p>
     * 선택자 목록 인수를 가진 의사 클래스를 만든다.
     *
     * @param name display name / 표시 이름
     * @param id   pseudo-class id / 의사 클래스 id
     * @param list the argument / 인수
     * @return the selector / 선택자
     */
    public static SimpleSelector withList(String name, int id, SelectorList list) {
        return new SimpleSelector(PSEUDO_CLASS, name, true, null, "", 0, null, 0, id, null, list, null);
    }

    /**
     * Creates a pseudo-class with word arguments (<code>:lang()</code>, <code>:dir()</code>).
     * <p>
     * 단어 인수를 가진 의사 클래스를 만든다(<code>:lang()</code>, <code>:dir()</code>).
     *
     * @param name  display name / 표시 이름
     * @param id    pseudo-class id / 의사 클래스 id
     * @param words arguments / 인수
     * @return the selector / 선택자
     */
    public static SimpleSelector withWords(String name, int id, List<String> words) {
        return new SimpleSelector(PSEUDO_CLASS, name, true, null, "", 0, null, 0, id, null, null, words);
    }

    /**
     * Returns the kind.
     * <p>
     * 종류를 반환한다.
     *
     * @return kind constant / 종류 상수
     */
    public int getKind() {
        return kind;
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 반환한다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Tells whether any namespace matches.
     * <p>
     * 모든 네임스페이스가 일치하는지 알려 준다.
     *
     * @return true for any namespace / 모든 네임스페이스이면 true
     */
    public boolean isNamespaceAny() {
        return nsAny;
    }

    /**
     * Returns the namespace URI to match.
     * <p>
     * 일치시킬 네임스페이스 URI 를 반환한다.
     *
     * @return URI or null for no namespace / URI. 네임스페이스 없음이면 null
     */
    public String getNamespace() {
        return ns;
    }

    /**
     * Returns the attribute operator.
     * <p>
     * 속성 연산자를 반환한다.
     *
     * @return operator constant / 연산자 상수
     */
    public int getOperator() {
        return op;
    }

    /**
     * Returns the attribute value.
     * <p>
     * 속성 값을 반환한다.
     *
     * @return value or null / 값 또는 null
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the attribute case mode.
     * <p>
     * 속성 대소문자 모드를 반환한다.
     *
     * @return case mode constant / 대소문자 모드 상수
     */
    public int getCaseMode() {
        return caseMode;
    }

    /**
     * Returns the pseudo-class id.
     * <p>
     * 의사 클래스 id 를 반환한다.
     *
     * @return id / id
     */
    public int getPseudoId() {
        return pseudoId;
    }

    /**
     * Returns the An+B argument.
     * <p>
     * An+B 인수를 반환한다.
     *
     * @return An+B or null / An+B 또는 null
     */
    public AnPlusB getAnPlusB() {
        return anb;
    }

    /**
     * Returns the selector list argument.
     * <p>
     * 선택자 목록 인수를 반환한다.
     *
     * @return list or null / 목록 또는 null
     */
    public SelectorList getList() {
        return list;
    }

    /**
     * Returns the word arguments.
     * <p>
     * 단어 인수를 반환한다.
     *
     * @return words or null / 단어들 또는 null
     */
    public List<String> getWords() {
        return words;
    }

    /**
     * Returns the specificity contribution of this simple selector as a packed number (id, class, type fields of 20 bits).
     * <p>
     * 이 단순 선택자의 구체성 기여분을 묶은 숫자(id, 클래스, 타입 각 20비트)로 반환한다.
     *
     * @return packed specificity / 묶은 구체성
     */
    public long specificity() {
        switch (kind) {
        case TYPE:
            return SPEC_TYPE;
        case UNIVERSAL:
            return 0;
        case ID:
            return SPEC_ID;
        case CLASS:
        case ATTRIBUTE:
            return SPEC_CLASS;
        default:
            break;
        }
        switch (pseudoId) {
        case PseudoClasses.WHERE:
            return 0;
        case PseudoClasses.NOT:
        case PseudoClasses.IS:
        case PseudoClasses.HAS:
            return list == null ? 0 : list.maxSpecificity();
        case PseudoClasses.NTH_CHILD:
        case PseudoClasses.NTH_LAST_CHILD:
            return SPEC_CLASS + (list == null ? 0 : list.maxSpecificity());
        default:
            return SPEC_CLASS;
        }
    }

    /**
     * Serializes the selector in the canonical form of CSSOM.
     * <p>
     * CSSOM 의 정규 형태로 선택자를 직렬화한다.
     *
     * @return selector text / 선택자 문자열
     */
    public String toCssText() {
        switch (kind) {
        case TYPE:
            return nsText + CssEscape.ident(name);
        case UNIVERSAL:
            return nsText + "*";
        case ID:
            return "#" + CssEscape.ident(name);
        case CLASS:
            return "." + CssEscape.ident(name);
        case ATTRIBUTE:
            return attributeText();
        default:
            return pseudoText();
        }
    }

    /**
     * Serializes an attribute selector.
     * <p>
     * 속성 선택자를 직렬화한다.
     *
     * @return text / 문자열
     */
    private String attributeText() {
        StringBuilder sb = new StringBuilder("[").append(nsText).append(CssEscape.ident(name));
        if (op != OP_EXISTS) {
            String[] ops = {"", "=", "~=", "|=", "^=", "$=", "*="};
            sb.append(ops[op]).append(CssEscape.string(value));
            if (caseMode == CASE_INSENSITIVE) {
                sb.append(" i");
            } else if (caseMode == CASE_SENSITIVE) {
                sb.append(" s");
            }
        }
        return sb.append(']').toString();
    }

    /**
     * Serializes a pseudo-class.
     * <p>
     * 의사 클래스를 직렬화한다.
     *
     * @return text / 문자열
     */
    private String pseudoText() {
        StringBuilder sb = new StringBuilder(":").append(name);
        if (anb != null) {
            sb.append('(').append(anb.toCssText());
            if (list != null) {
                sb.append(" of ").append(list.toCssText());
            }
            sb.append(')');
        } else if (list != null) {
            sb.append('(').append(list.toCssText()).append(')');
        } else if (words != null) {
            sb.append('(');
            // 단어를 쉼표로 이어 쓴다.
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(pseudoId == PseudoClasses.LANG && needsQuote(words.get(i)) ? CssEscape.string(words.get(i)) : CssEscape.ident(words.get(i)));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    /**
     * Tells whether a language range must be written as a string (it is not a valid identifier).
     * <p>
     * 언어 범위를 문자열로 써야 하는지(올바른 식별자가 아닌지) 알려 준다.
     *
     * @param s language range / 언어 범위
     * @return true if quoting is needed / 따옴표가 필요하면 true
     */
    private static boolean needsQuote(String s) {
        return s.length() == 0 || s.indexOf('*') >= 0;
    }
}
