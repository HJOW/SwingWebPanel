package org.duckdns.hjow.swingwebpanel.js.ast;

/**
 * Integer codes for operators, declaration kinds and property kinds used by the syntax tree.
 * <p>
 * 구문 트리가 쓰는 연산자, 선언 종류, 프로퍼티 종류의 정수 코드.
 */
public final class Op {

    /** Private constructor: constants holder. / 비공개 생성자: 상수 보관용 클래스. */
    private Op() {
    }

    /** Addition or string concatenation (+). / 덧셈 또는 문자열 연결(+). */
    public static final int ADD = 1;
    /** Subtraction operator (-). / 뺄셈(-). */
    public static final int SUB = 2;
    /** Multiplication operator (*). / 곱셈(*). */
    public static final int MUL = 3;
    /** Division operator (/). / 나눗셈(/). */
    public static final int DIV = 4;
    /** Remainder operator (%). / 나머지(%). */
    public static final int MOD = 5;
    /** Left shift (&lt;&lt;). / 왼쪽 시프트(&lt;&lt;). */
    public static final int SHL = 6;
    /** Signed right shift (&gt;&gt;). / 부호 있는 오른쪽 시프트(&gt;&gt;). */
    public static final int SHR = 7;
    /** Unsigned right shift (&gt;&gt;&gt;). / 부호 없는 오른쪽 시프트(&gt;&gt;&gt;). */
    public static final int USHR = 8;
    /** Less than (&lt;). / 보다 작음(&lt;). */
    public static final int LT = 9;
    /** Greater than (&gt;). / 보다 큼(&gt;). */
    public static final int GT = 10;
    /** Less than or equal (&lt;=). / 작거나 같음(&lt;=). */
    public static final int LE = 11;
    /** Greater than or equal (&gt;=). / 크거나 같음(&gt;=). */
    public static final int GE = 12;
    /** Loose equality (==). / 느슨한 동등(==). */
    public static final int EQ = 13;
    /** Loose inequality (!=). / 느슨한 부등(!=). */
    public static final int NE = 14;
    /** Strict equality (===). / 엄격한 동등(===). */
    public static final int SEQ = 15;
    /** Strict inequality (!==). / 엄격한 부등(!==). */
    public static final int SNE = 16;
    /** Bitwise and (&amp;). / 비트 AND(&amp;). */
    public static final int BITAND = 17;
    /** Bitwise or (|). / 비트 OR(|). */
    public static final int BITOR = 18;
    /** Bitwise xor (^). / 비트 XOR(^). */
    public static final int BITXOR = 19;
    /** The in operator. / in 연산자. */
    public static final int IN = 20;
    /** The instanceof operator. / instanceof 연산자. */
    public static final int INSTANCEOF = 21;
    /** Logical and (&amp;&amp;). / 논리 AND(&amp;&amp;). */
    public static final int LOGAND = 22;
    /** Logical or (||). / 논리 OR(||). */
    public static final int LOGOR = 23;
    /** Exponentiation (**). / 거듭제곱(**). */
    public static final int EXP = 24;

    /** Unary minus (-x). / 단항 마이너스(-x). */
    public static final int NEG = 30;
    /** Unary plus (+x). / 단항 플러스(+x). */
    public static final int POS = 31;
    /** Logical not (!x). / 논리 부정(!x). */
    public static final int NOT = 32;
    /** Bitwise not (~x). / 비트 부정(~x). */
    public static final int BITNOT = 33;
    /** The typeof operator. / typeof 연산자. */
    public static final int TYPEOF = 34;
    /** The void operator. / void 연산자. */
    public static final int VOID = 35;
    /** The delete operator. / delete 연산자. */
    public static final int DELETE = 36;
    /** Increment operator (++). / 증가(++). */
    public static final int INC = 37;
    /** Decrement operator (--). / 감소(--). */
    public static final int DEC = 38;

    /** Declaration kind var. / 선언 종류 var. */
    public static final int VAR = 40;
    /** Declaration kind let. / 선언 종류 let. */
    public static final int LET = 41;
    /** Declaration kind const. / 선언 종류 const. */
    public static final int CONST = 42;

    /** Property kind: key: value. / 프로퍼티 종류: key: value. */
    public static final int PROP_INIT = 50;
    /** Property kind: shorthand {a}. / 프로퍼티 종류: 축약 {a}. */
    public static final int PROP_SHORTHAND = 51;
    /** Property kind: shorthand with default {a = 1}, valid only as a pattern. / 프로퍼티 종류: 기본값이 있는 축약 {a = 1}. 패턴으로만 유효. */
    public static final int PROP_COVER_INIT = 52;
    /** Property kind: method. / 프로퍼티 종류: 메서드. */
    public static final int PROP_METHOD = 53;
    /** Property kind: getter. / 프로퍼티 종류: getter. */
    public static final int PROP_GET = 54;
    /** Property kind: setter. / 프로퍼티 종류: setter. */
    public static final int PROP_SET = 55;

    /**
     * Returns the source text of a binary or compound assignment operator code, for messages.
     * <p>
     * 이항 또는 복합 대입 연산자 코드의 소스 텍스트를 메시지용으로 반환한다.
     *
     * @param op the operator code / 연산자 코드
     * @return the operator text / 연산자 문자열
     */
    public static String text(int op) {
        switch (op) {
        case ADD:
            return "+";
        case SUB:
            return "-";
        case MUL:
            return "*";
        case DIV:
            return "/";
        case MOD:
            return "%";
        case SHL:
            return "<<";
        case SHR:
            return ">>";
        case USHR:
            return ">>>";
        case LT:
            return "<";
        case GT:
            return ">";
        case LE:
            return "<=";
        case GE:
            return ">=";
        case EQ:
            return "==";
        case NE:
            return "!=";
        case SEQ:
            return "===";
        case SNE:
            return "!==";
        case BITAND:
            return "&";
        case BITOR:
            return "|";
        case BITXOR:
            return "^";
        case IN:
            return "in";
        case INSTANCEOF:
            return "instanceof";
        case LOGAND:
            return "&&";
        case LOGOR:
            return "||";
        case EXP:
            return "**";
        default:
            return "?";
        }
    }
}
