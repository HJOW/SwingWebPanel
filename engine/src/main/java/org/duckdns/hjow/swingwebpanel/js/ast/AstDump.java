package org.duckdns.hjow.swingwebpanel.js.ast;

import java.math.BigInteger;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsString;

/**
 * Converts a syntax tree to a compact S-expression string such as <code>(+ a (* b c))</code>. It is a debugging and testing
 * aid that shows the exact tree shape (operator precedence, associativity, nesting) in one line.
 * <p>
 * 구문 트리를 <code>(+ a (* b c))</code> 같은 간결한 S-식 문자열로 바꾼다. 연산자 우선순위, 결합 방향, 중첩 같은 트리의 정확한 모양을
 * 한 줄로 보여 주는 디버깅과 시험 보조 도구이다.
 */
public final class AstDump {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private AstDump() {
    }

    /**
     * Dumps a node (null is shown as an underscore).
     * <p>
     * 노드를 덤프한다(null 은 밑줄로 표시).
     *
     * @param n the node or null / 노드 또는 null
     * @return the S-expression / S-식
     */
    public static String dump(Node n) {
        StringBuilder sb = new StringBuilder();
        write(n, sb);
        return sb.toString();
    }

    /**
     * Appends a list of nodes separated by spaces.
     * <p>
     * 노드 목록을 공백으로 구분해 덧붙인다.
     *
     * @param list the nodes / 노드들
     * @param sb   the output / 출력
     */
    private static void writeList(Node[] list, StringBuilder sb) {
        if (list == null) {
            return;
        }
        // 목록의 각 노드를 앞에 공백을 붙여 출력한다.
        for (int i = 0; i < list.length; i++) {
            sb.append(' ');
            write(list[i], sb);
        }
    }

    /**
     * Appends a child node preceded by a space.
     * <p>
     * 자식 노드를 앞에 공백을 붙여 덧붙인다.
     *
     * @param n  the node / 노드
     * @param sb the output / 출력
     */
    private static void child(Node n, StringBuilder sb) {
        sb.append(' ');
        write(n, sb);
    }

    /**
     * Returns the text of a unary or update operator.
     * <p>
     * 단항 또는 증감 연산자의 텍스트를 반환한다.
     *
     * @param op the code / 코드
     * @return the text / 텍스트
     */
    private static String unaryText(int op) {
        switch (op) {
        case Op.NEG:
            return "-";
        case Op.POS:
            return "+";
        case Op.NOT:
            return "!";
        case Op.BITNOT:
            return "~";
        case Op.TYPEOF:
            return "typeof";
        case Op.VOID:
            return "void";
        case Op.DELETE:
            return "delete";
        case Op.INC:
            return "++";
        case Op.DEC:
            return "--";
        default:
            return "?";
        }
    }

    /**
     * Returns the property kind name.
     * <p>
     * 프로퍼티 종류 이름을 반환한다.
     *
     * @param op the code / 코드
     * @return the name / 이름
     */
    private static String propKind(int op) {
        switch (op) {
        case Op.PROP_INIT:
            return "init";
        case Op.PROP_SHORTHAND:
            return "short";
        case Op.PROP_COVER_INIT:
            return "cover";
        case Op.PROP_METHOD:
            return "method";
        case Op.PROP_GET:
            return "get";
        case Op.PROP_SET:
            return "set";
        case Op.PROP_FIELD:
            return "field";
        default:
            return "?";
        }
    }

    /**
     * Writes a key node: a literal name as is, a computed key in brackets.
     * <p>
     * 키 노드를 출력한다: 리터럴 이름은 그대로, 계산된 키는 대괄호로 감싼다.
     *
     * @param key      the key node / 키 노드
     * @param computed true if computed / 계산된 키이면 true
     * @param sb       the output / 출력
     */
    private static void writeKey(Node key, boolean computed, StringBuilder sb) {
        if (computed) {
            sb.append('[');
            write(key, sb);
            sb.append(']');
        } else if (key.name != null) {
            sb.append(key.name);
        } else {
            write(key, sb);
        }
    }

    /**
     * Writes the S-expression of a node.
     * <p>
     * 노드의 S-식을 출력한다.
     *
     * @param n  the node or null / 노드 또는 null
     * @param sb the output / 출력
     */
    private static void write(Node n, StringBuilder sb) {
        if (n == null) {
            sb.append('_');
            return;
        }
        switch (n.type) {
        case NodeType.LITERAL: {
            Object v = n.value;
            if (v instanceof JsString) {
                sb.append('"').append(((JsString) v).getValue()).append('"');
            } else if (v instanceof JsNumber || v instanceof JsBigInt || v instanceof JsBoolean || v instanceof BigInteger) {
                sb.append(v.toString());
            } else {
                sb.append("null");
            }
            return;
        }
        case NodeType.IDENT:
            sb.append(n.name);
            return;
        case NodeType.PRIVATE_NAME:
            sb.append(n.name);
            return;
        case NodeType.THIS:
            sb.append("this");
            return;
        case NodeType.SUPER:
            sb.append("super");
            return;
        case NodeType.NEW_TARGET:
            sb.append("new.target");
            return;
        case NodeType.REGEXP:
            sb.append('/').append(n.name).append('/').append(n.value);
            return;
        case NodeType.TEMPLATE: {
            TemplateParts tp = (TemplateParts) n.value;
            sb.append("(template");
            // 문자열 조각과 치환식을 번갈아 출력한다.
            for (int i = 0; i < tp.cooked.length; i++) {
                sb.append(' ').append(tp.cooked[i] == null ? "undefined" : '"' + tp.cooked[i] + '"');
                if (i < n.list.length) {
                    child(n.list[i], sb);
                }
            }
            sb.append(')');
            return;
        }
        case NodeType.TAGGED_TEMPLATE:
            sb.append("(tagged");
            child(n.a, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.ARRAY:
            sb.append("(array");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.OBJECT:
            sb.append("(object");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.PROPERTY:
            sb.append('(').append(propKind(n.op)).append(' ');
            writeKey(n.a, n.flag, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.FUNCTION: {
            FunctionInfo f = n.fn;
            if (f.async) {
                sb.append(f.arrow ? "(async-arrow" : f.generator ? "(async-function*" : f.method ? "(async-method" : "(async-function");
            } else {
                sb.append(f.arrow ? "(arrow" : f.generator ? "(function*" : f.method ? "(method" : "(function");
            }
            if (!f.arrow) {
                sb.append(' ').append(f.name.length() == 0 ? "_" : f.name);
            }
            sb.append(" (");
            for (int i = 0; i < f.params.length; i++) { // 매개변수를 차례로 출력한다.
                if (i > 0) {
                    sb.append(' ');
                }
                write(f.params[i], sb);
            }
            sb.append(')');
            child(f.body, sb);
            sb.append(')');
            return;
        }
        case NodeType.CLASS:
            sb.append("(class ").append(n.name == null ? "_" : n.name);
            child(n.a, sb);
            child(n.b, sb);
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.CLASS_MEMBER:
            sb.append(n.flag ? "(static-" : "(").append(propKind(n.op)).append(' ');
            writeKey(n.a, n.value == Boolean.TRUE, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.CLASS_STATIC_BLOCK:
            sb.append("(static-block");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.UNARY:
            sb.append('(').append(unaryText(n.op));
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.UPDATE:
            sb.append('(');
            if (n.flag) {
                sb.append("pre").append(unaryText(n.op));
                child(n.a, sb);
            } else {
                sb.append("post").append(unaryText(n.op));
                child(n.a, sb);
            }
            sb.append(')');
            return;
        case NodeType.BINARY:
        case NodeType.LOGICAL:
            sb.append('(').append(Op.text(n.op));
            child(n.a, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.ASSIGN:
            sb.append('(').append(n.op == 0 ? "=" : Op.text(n.op) + "=");
            child(n.a, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.CONDITIONAL:
            sb.append("(?:");
            child(n.a, sb);
            child(n.b, sb);
            child(n.c, sb);
            sb.append(')');
            return;
        case NodeType.CALL:
            sb.append(n.optional ? "(call?" : "(call");
            child(n.a, sb);
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.NEW:
            sb.append("(new");
            child(n.a, sb);
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.MEMBER:
            if (n.flag) {
                sb.append(n.optional ? "([]?" : "([]");
                child(n.a, sb);
                child(n.b, sb);
            } else {
                sb.append(n.optional ? "(.?" : "(.");
                child(n.a, sb);
                sb.append(' ').append(n.name);
            }
            sb.append(')');
            return;
        case NodeType.SEQUENCE:
            sb.append("(,");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.SPREAD:
            sb.append("(...");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.YIELD:
            sb.append(n.flag ? "(yield*" : "(yield");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.OPTIONAL_CHAIN:
            sb.append("(chain");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.AWAIT:
            sb.append("(await");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.IMPORT_META:
            sb.append("import.meta");
            return;
        case NodeType.IMPORT_CALL:
            sb.append("(import-call");
            child(n.a, sb);
            if (n.b != null) {
                child(n.b, sb);
            }
            sb.append(')');
            return;
        case NodeType.ARRAY_PATTERN:
            sb.append("(arrpat");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.OBJECT_PATTERN:
            sb.append("(objpat");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.PATTERN_PROP:
            sb.append("(pp ");
            writeKey(n.a, n.flag, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.REST:
            sb.append("(rest");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.DEFAULT:
            sb.append("(default");
            child(n.a, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        default:
            writeStatement(n, sb);
        }
    }

    /**
     * Writes the S-expression of a statement or declaration node.
     * <p>
     * 문장 또는 선언 노드의 S-식을 출력한다.
     *
     * @param n  the node / 노드
     * @param sb the output / 출력
     */
    private static void writeStatement(Node n, StringBuilder sb) {
        switch (n.type) {
        case NodeType.PROGRAM:
            sb.append("(program");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.VAR_DECL:
            sb.append(n.op == Op.VAR ? "(var" : n.op == Op.LET ? "(let" : "(const");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.DECLARATOR:
            sb.append("(d");
            child(n.a, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.EXPR_STMT:
            sb.append("(expr");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.BLOCK:
            sb.append("(block");
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.EMPTY:
            sb.append("(empty)");
            return;
        case NodeType.IF:
            sb.append("(if");
            child(n.a, sb);
            child(n.b, sb);
            child(n.c, sb);
            sb.append(')');
            return;
        case NodeType.FOR:
            sb.append("(for");
            child(n.a, sb);
            child(n.b, sb);
            child(n.c, sb);
            child(n.d, sb);
            sb.append(')');
            return;
        case NodeType.FOR_IN:
        case NodeType.FOR_OF:
            sb.append(n.type == NodeType.FOR_IN ? "(for-in" : n.flag ? "(for-await" : "(for-of");
            child(n.a, sb);
            child(n.b, sb);
            child(n.d, sb);
            sb.append(')');
            return;
        case NodeType.WHILE:
            sb.append("(while");
            child(n.a, sb);
            child(n.d, sb);
            sb.append(')');
            return;
        case NodeType.DO_WHILE:
            sb.append("(do");
            child(n.d, sb);
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.RETURN:
            sb.append("(return");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.BREAK:
            sb.append("(break ").append(n.name == null ? "_" : n.name).append(')');
            return;
        case NodeType.CONTINUE:
            sb.append("(continue ").append(n.name == null ? "_" : n.name).append(')');
            return;
        case NodeType.THROW:
            sb.append("(throw");
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.TRY:
            sb.append("(try");
            child(n.a, sb);
            child(n.b, sb);
            child(n.c, sb);
            child(n.d, sb);
            sb.append(')');
            return;
        case NodeType.SWITCH:
            sb.append("(switch");
            child(n.a, sb);
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.CASE:
            sb.append("(case");
            child(n.a, sb);
            writeList(n.list, sb);
            sb.append(')');
            return;
        case NodeType.LABELED:
            sb.append("(label ").append(n.name);
            child(n.a, sb);
            sb.append(')');
            return;
        case NodeType.WITH:
            sb.append("(with");
            child(n.a, sb);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.DEBUGGER:
            sb.append("(debugger)");
            return;
        case NodeType.FUNCTION_DECL:
            sb.append("(fdecl ").append(n.a.name);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.CLASS_DECL:
            sb.append("(cdecl ").append(n.a.name);
            child(n.b, sb);
            sb.append(')');
            return;
        case NodeType.IMPORT:
            sb.append("(import \"").append(n.name).append("\")");
            return;
        case NodeType.EXPORT:
            sb.append("(export");
            child(n.a, sb);
            sb.append(')');
            return;
        default:
            sb.append("(?").append(n.type).append(')');
        }
    }
}
