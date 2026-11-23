package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.xpath.NamespaceResolver;
import org.duckdns.hjow.swingwebpanel.dom.xpath.XPathException;
import org.duckdns.hjow.swingwebpanel.dom.xpath.XPathExpression;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * DOM XPath for the scripts of a page (WHATWG DOM section 8, "XPath", last modified 15 November 2026, fetched 2026-11-18; see <code>docs/progress/webapi2.md</code>):
 * <code>document.evaluate()</code>, <code>document.createExpression()</code>, <code>document.createNSResolver()</code>, the interfaces <code>XPathResult</code>,
 * <code>XPathExpression</code> and <code>XPathEvaluator</code> (which has the same three methods; it can be constructed). The expressions are evaluated by
 * {@link XPathExpression}. The result types are those of the specification: the node set result of <code>ANY_TYPE</code> is an unordered node iterator (which is in
 * document order here), an iterator becomes invalid (<code>invalidIteratorState</code>, and <code>iterateNext()</code> throws <code>InvalidStateError</code>) when
 * the document changes after the result was made, the other conversions follow <code>number()</code>, <code>string()</code> and <code>boolean()</code>, and a node
 * type that is asked for a value that is not a node set is a <code>TypeError</code>. A syntax error is a <code>SyntaxError</code> and an unresolvable prefix a
 * <code>NamespaceError</code>. The <code>result</code> argument (to reuse an object) is ignored: a new result is made. A resolver is a function, an object with a
 * <code>lookupNamespaceURI</code> method, or a node (<code>createNSResolver(node)</code> returns the node itself). Every method runs on the thread that owns the
 * scripts of the page.
 * <p>
 * 페이지 스크립트의 DOM XPath(WHATWG DOM 8절 "XPath", 2026-11-15 수정본, 2026-11-18 조회, <code>docs/progress/webapi2.md</code> 참조): <code>document.evaluate()</code>,
 * <code>document.createExpression()</code>, <code>document.createNSResolver()</code>, 인터페이스 <code>XPathResult</code>, <code>XPathExpression</code>,
 * <code>XPathEvaluator</code>(같은 세 메서드가 있으며 생성할 수 있다). 식은 {@link XPathExpression} 이 평가한다. 결과 종류는 규격대로이다: <code>ANY_TYPE</code> 의 노드 집합
 * 결과는 순서 없는 노드 반복자이고(여기서는 문서 순서) 결과를 만든 뒤 문서가 바뀌면 반복자는 무효가 되며(<code>invalidIteratorState</code>,
 * <code>iterateNext()</code> 는 <code>InvalidStateError</code>) 다른 변환은 <code>number()</code>, <code>string()</code>, <code>boolean()</code> 을 따르고 노드 집합이 아닌
 * 값에 노드 종류를 요청하면 <code>TypeError</code> 이다. 구문 오류는 <code>SyntaxError</code>, 해석할 수 없는 접두사는 <code>NamespaceError</code> 이다. <code>result</code>
 * 인수(객체 재사용)는 무시하고 새 결과를 만든다. 리졸버는 함수, <code>lookupNamespaceURI</code> 메서드가 있는 객체, 노드이다(<code>createNSResolver(node)</code> 는 노드를
 * 그대로 반환한다). 모든 메서드는 페이지 스크립트를 소유한 스레드에서 실행된다.
 */
final class XPathApi {

    /**
     * The result type numbers of <code>XPathResult</code>.
     * <p>
     * <code>XPathResult</code> 의 결과 종류 번호.
     */
    private static final String[] TYPE_NAMES = { "ANY_TYPE", "NUMBER_TYPE", "STRING_TYPE", "BOOLEAN_TYPE", "UNORDERED_NODE_ITERATOR_TYPE", "ORDERED_NODE_ITERATOR_TYPE",
        "UNORDERED_NODE_SNAPSHOT_TYPE", "ORDERED_NODE_SNAPSHOT_TYPE", "ANY_UNORDERED_NODE_TYPE", "FIRST_ORDERED_NODE_TYPE" };

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The number of changes of the document so far (a node iterator is valid while it did not change since the result was made).
     * <p>
     * 지금까지 문서가 바뀐 횟수(노드 반복자는 결과를 만든 뒤 바뀌지 않은 동안 유효하다).
     */
    private int mutations;

    /**
     * The prototype of the results.
     * <p>
     * 결과의 프로토타입.
     */
    private JsObject resultProto;

    /**
     * The prototype of the compiled expressions.
     * <p>
     * 컴파일된 식의 프로토타입.
     */
    private JsObject expressionProto;

    /**
     * A result: the type and the value in the form that the type needs.
     * <p>
     * 결과: 종류와 그 종류에 필요한 형태의 값.
     */
    private static final class ResultObject extends JsObject {
        /** The result type. / 결과 종류. */
        final int type;
        /** The number value. / 숫자 값. */
        final double number;
        /** The string value. / 문자열 값. */
        final String string;
        /** The boolean value. / 불리언 값. */
        final boolean bool;
        /** The nodes in document order (null for a value that is not a node set). / 문서 순서의 노드(노드 집합이 아닌 값이면 null). */
        final List<Object> nodes;
        /** The value of {@link XPathApi#mutations} when the result was made. / 결과를 만들 때의 {@link XPathApi#mutations} 값. */
        final int stamp;
        /** The index of the next node of an iterator. / 반복자의 다음 노드 인덱스. */
        int index;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param proto  the prototype / 프로토타입
         * @param type   the type / 종류
         * @param number the number / 숫자
         * @param string the string / 문자열
         * @param bool   the boolean / 불리언
         * @param nodes  the nodes or null / 노드. 없으면 null
         * @param stamp  the change count / 변경 횟수
         */
        ResultObject(JsObject proto, int type, double number, String string, boolean bool, List<Object> nodes, int stamp) {
            super(proto);
            this.type = type;
            this.number = number;
            this.string = string;
            this.bool = bool;
            this.nodes = nodes;
            this.stamp = stamp;
        }
    }

    /**
     * A compiled expression with the resolver that it was created with.
     * <p>
     * 만들 때 받은 리졸버와 함께 컴파일된 식.
     */
    private static final class ExpressionObject extends JsObject {
        /** The compiled expression that this object evaluates. / 이 객체가 평가하는 컴파일된 식. */
        final XPathExpression expression;
        /** The resolver. / 리졸버. */
        final NamespaceResolver resolver;

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto      the prototype / 프로토타입
         * @param expression the expression / 식
         * @param resolver   the resolver or null / 리졸버. 없으면 null
         */
        ExpressionObject(JsObject proto, XPathExpression expression, NamespaceResolver resolver) {
            super(proto);
            this.expression = expression;
            this.resolver = resolver;
        }
    }

    /**
     * Creates the helper.
     * <p>
     * 도우미를 만든다.
     *
     * @param page the page / 페이지
     */
    XPathApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Counts a change of the document (node iterators that were made before it are invalid).
     * <p>
     * 문서의 변경을 센다(그 전에 만든 노드 반복자는 무효가 된다).
     *
     * @param record the change / 변경
     */
    void mutated(MutationRecord record) {
        mutations++;
    }

    /**
     * Creates an interface object that the page cannot construct, with a prototype that names the interface.
     * <p>
     * 페이지가 만들 수 없는 인터페이스 객체를 프로토타입과 함께 만든다. 프로토타입이 인터페이스 이름을 가진다.
     *
     * @param g    the global object / 전역 객체
     * @param name the interface name / 인터페이스 이름
     * @return the prototype / 프로토타입
     */
    private JsObject illegal(JsObject g, String name) {
        return InterfaceNames.define(page, g, name);
    }

    /**
     * Installs the interfaces and the methods of <code>Document</code>.
     * <p>
     * 인터페이스와 <code>Document</code> 의 메서드를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        resultProto = illegal(g, "XPathResult");
        JsObject resultCtor = (JsObject) resultProto.get("constructor");
        // 결과 종류의 상수는 인터페이스 객체와 프로토타입에 모두 있다.
        for (int i = 0; i < TYPE_NAMES.length; i++) {
            resultCtor.defineData(TYPE_NAMES[i], Js.num(i), false, true, false);
            resultProto.defineData(TYPE_NAMES[i], Js.num(i), false, true, false);
        }
        getter("resultType", new NativeFunction.Call() {
            /**
             * Returns the result type.
             * <p>
             * 결과 종류를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the type / 종류
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(result(self).type);
            }
        });
        getter("numberValue", new NativeFunction.Call() {
            /**
             * Returns the number of a <code>NUMBER_TYPE</code> result.
             * <p>
             * <code>NUMBER_TYPE</code> 결과의 숫자를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the number / 숫자
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 1, "numberValue", "NUMBER_TYPE");
                return Js.num(o.number);
            }
        });
        getter("stringValue", new NativeFunction.Call() {
            /**
             * Returns the string of a <code>STRING_TYPE</code> result.
             * <p>
             * <code>STRING_TYPE</code> 결과의 문자열을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the string / 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 2, "stringValue", "STRING_TYPE");
                return Js.str(o.string);
            }
        });
        getter("booleanValue", new NativeFunction.Call() {
            /**
             * Returns the boolean of a <code>BOOLEAN_TYPE</code> result.
             * <p>
             * <code>BOOLEAN_TYPE</code> 결과의 불리언을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the boolean / 불리언
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 3, "booleanValue", "BOOLEAN_TYPE");
                return Js.bool(o.bool);
            }
        });
        getter("singleNodeValue", new NativeFunction.Call() {
            /**
             * Returns the node of an <code>ANY_UNORDERED_NODE_TYPE</code> or <code>FIRST_ORDERED_NODE_TYPE</code> result.
             * <p>
             * <code>ANY_UNORDERED_NODE_TYPE</code> 나 <code>FIRST_ORDERED_NODE_TYPE</code> 결과의 노드를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the node or null / 노드 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 8 || o.type == 9, "singleNodeValue", "ANY_UNORDERED_NODE_TYPE or FIRST_ORDERED_NODE_TYPE");
                return o.nodes.isEmpty() ? Js.NULL : page.getBridge().toJs(o.nodes.get(0));
            }
        });
        getter("invalidIteratorState", new NativeFunction.Call() {
            /**
             * Tells whether a node iterator was made invalid by a change of the document.
             * <p>
             * 노드 반복자가 문서의 변경으로 무효가 되었는지 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return true if invalid / 무효이면 true
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                return Js.bool((o.type == 4 || o.type == 5) && o.stamp != mutations);
            }
        });
        getter("snapshotLength", new NativeFunction.Call() {
            /**
             * Returns the number of nodes of a snapshot.
             * <p>
             * 스냅숏의 노드 수를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the length / 길이
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 6 || o.type == 7, "snapshotLength", "UNORDERED_NODE_SNAPSHOT_TYPE or ORDERED_NODE_SNAPSHOT_TYPE");
                return Js.num(o.nodes.size());
            }
        });
        realm.method(resultProto, "iterateNext", 0, new NativeFunction.Call() {
            /**
             * Returns the next node of a node iterator, or null at the end.
             * <p>
             * 노드 반복자의 다음 노드를 반환한다. 끝이면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the node or null / 노드 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 4 || o.type == 5, "iterateNext", "UNORDERED_NODE_ITERATOR_TYPE or ORDERED_NODE_ITERATOR_TYPE");
                if (o.stamp != mutations) {
                    throw page.getBridge().domError(DomException.invalidState("The document has been mutated since the result was returned."));
                }
                return o.index < o.nodes.size() ? page.getBridge().toJs(o.nodes.get(o.index++)) : Js.NULL;
            }
        });
        realm.method(resultProto, "snapshotItem", 1, new NativeFunction.Call() {
            /**
             * Returns a node of a snapshot, or null for an index that is out of range.
             * <p>
             * 스냅숏의 노드를 반환한다. 범위 밖의 인덱스이면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the node or null / 노드 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ResultObject o = result(self);
                require(o.type == 6 || o.type == 7, "snapshotItem", "UNORDERED_NODE_SNAPSHOT_TYPE or ORDERED_NODE_SNAPSHOT_TYPE");
                double d = Ops.toNumber(Js.arg(args, 0));
                int i = Double.isNaN(d) ? 0 : (int) Math.max(-1, Math.min(Integer.MAX_VALUE, d));
                return i >= 0 && i < o.nodes.size() ? page.getBridge().toJs(o.nodes.get(i)) : Js.NULL;
            }
        });
        expressionProto = illegal(g, "XPathExpression");
        realm.method(expressionProto, "evaluate", 1, new NativeFunction.Call() {
            /**
             * Evaluates the expression for a context node.
             * <p>
             * 컨텍스트 노드에 대해 식을 평가한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the context node, the result type and a result to reuse / 인수: 컨텍스트 노드, 결과 종류, 재사용할 결과
             * @return the result / 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (!(self instanceof ExpressionObject)) {
                    throw JsException.typeError("Illegal invocation");
                }
                ExpressionObject e = (ExpressionObject) self;
                return run(e.expression, contextArg(args, 0, "evaluate"), e.resolver, Js.arg(args, 1));
            }
        });
        installEvaluatorMethods(g, realm);
    }

    /**
     * Defines the three methods on <code>XPathEvaluator.prototype</code> and on <code>Document.prototype</code>, and the constructor <code>XPathEvaluator</code>.
     * <p>
     * 세 메서드를 <code>XPathEvaluator.prototype</code> 과 <code>Document.prototype</code> 에 정의하고 생성자 <code>XPathEvaluator</code> 를 만든다.
     *
     * @param g     the global object / 전역 객체
     * @param realm the realm / realm
     */
    private void installEvaluatorMethods(JsObject g, JsRealm realm) {
        final JsObject evaluatorProto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, "XPathEvaluator", 0, new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'XPathEvaluator': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an evaluator (an object with the three methods).
             * <p>
             * 평가기(세 메서드가 있는 객체)를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the evaluator / 평가기
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return new JsObject(Ops.getPrototypeFromConstructor(r, newTarget, evaluatorProto));
            }
        });
        ctor.defineData("prototype", evaluatorProto, false, false, false);
        evaluatorProto.defineBuiltin("constructor", ctor);
        evaluatorProto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(Js.str("XPathEvaluator"), false, false, true));
        JsObject documentProto = page.getBridge().getInterface("Document").getPrototype();
        JsObject[] targets = { evaluatorProto, documentProto };
        // 같은 세 메서드를 두 프로토타입에 정의한다(규격: Document 가 XPathEvaluatorBase 를 포함한다).
        for (int k = 0; k < targets.length; k++) {
            realm.method(targets[k], "createExpression", 1, new NativeFunction.Call() {
                /**
                 * Compiles an expression.
                 * <p>
                 * 식을 컴파일한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments: the expression and a resolver / 인수: 식과 리졸버
                 * @return the compiled expression / 컴파일된 식
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    if (args.length == 0) {
                        throw JsException.typeError("Failed to execute 'createExpression': 1 argument required, but only 0 present.");
                    }
                    return new ExpressionObject(expressionProto, compile(Ops.toString(args[0])), resolver(Js.arg(args, 1)));
                }
            });
            realm.method(targets[k], "createNSResolver", 1, new NativeFunction.Call() {
                /**
                 * Returns the node itself: a node resolves the prefixes of its scope with <code>lookupNamespaceURI</code>.
                 * <p>
                 * 노드를 그대로 반환한다: 노드는 자기 범위의 접두사를 <code>lookupNamespaceURI</code> 로 해석한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments: the node / 인수: 노드
                 * @return the node / 노드
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    if (DomBridge.targetOf(Js.arg(args, 0), Node.class) == null) {
                        throw JsException.typeError("Failed to execute 'createNSResolver': parameter 1 is not of type 'Node'.");
                    }
                    return args[0];
                }
            });
            realm.method(targets[k], "evaluate", 2, new NativeFunction.Call() {
                /**
                 * Compiles and evaluates an expression.
                 * <p>
                 * 식을 컴파일하고 평가한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments: the expression, the context node, the resolver, the result type and a result to reuse / 인수: 식, 컨텍스트 노드, 리졸버, 결과 종류, 재사용할 결과
                 * @return the result / 결과
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    if (args.length < 2) {
                        throw JsException.typeError("Failed to execute 'evaluate': 2 arguments required, but only " + args.length + " present.");
                    }
                    XPathExpression e = compile(Ops.toString(args[0]));
                    return run(e, contextArg(args, 1, "evaluate"), resolver(Js.arg(args, 2)), Js.arg(args, 3));
                }
            });
        }
        g.defineBuiltin("XPathEvaluator", ctor);
    }

    /**
     * Defines a read-only accessor of the results.
     * <p>
     * 결과의 읽기 전용 접근자를 정의한다.
     *
     * @param name the attribute / 속성
     * @param get  the getter / 게터
     */
    private void getter(String name, NativeFunction.Call get) {
        resultProto.defineAccessor(name, page.getRealm().newFunction("get " + name, 0, get), null, true, true);
    }

    /**
     * Returns the result behind a receiver or throws a TypeError.
     * <p>
     * 수신자 뒤의 결과를 반환하거나 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the result / 결과
     */
    private static ResultObject result(JsValue self) {
        if (self instanceof ResultObject) {
            return (ResultObject) self;
        }
        throw JsException.typeError("Illegal invocation");
    }

    /**
     * Throws a TypeError if a condition does not hold (the result is not of the type that the member needs).
     * <p>
     * 조건이 맞지 않으면(결과가 멤버에 필요한 종류가 아니면) TypeError 를 던진다.
     *
     * @param ok     the condition / 조건
     * @param member the member / 멤버
     * @param type   the type name for the message / 메시지의 종류 이름
     */
    private static void require(boolean ok, String member, String type) {
        if (!ok) {
            throw JsException.typeError("Failed to read '" + member + "' of 'XPathResult': The result type is not " + type + ".");
        }
    }

    /**
     * Reads the context node (a node or an attribute) of an argument.
     * <p>
     * 인수의 컨텍스트 노드(노드나 속성)를 읽는다.
     *
     * @param args the arguments / 인수
     * @param i    the index / 인덱스
     * @param what the method name / 메서드 이름
     * @return the node or attribute / 노드나 속성
     */
    private static Object contextArg(JsValue[] args, int i, String what) {
        Object n = DomBridge.targetOf(Js.arg(args, i), Node.class);
        if (n == null) {
            n = DomBridge.targetOf(Js.arg(args, i), Attr.class);
        }
        if (n == null) {
            throw JsException.typeError("Failed to execute '" + what + "': parameter " + (i + 1) + " is not of type 'Node'.");
        }
        return n;
    }

    /**
     * Compiles an expression and turns its errors into the errors that the page sees.
     * <p>
     * 식을 컴파일하고 오류를 페이지가 보는 오류로 바꾼다.
     *
     * @param text the expression / 식
     * @return the expression / 식
     */
    private XPathExpression compile(String text) {
        try {
            return XPathExpression.compile(text);
        } catch (XPathException e) {
            throw error(e);
        }
    }

    /**
     * Converts the error of the XPath engine: a syntax error is a <code>SyntaxError</code>, a namespace error a <code>NamespaceError</code>, a type error a
     * <code>TypeError</code>.
     * <p>
     * XPath 엔진의 오류를 바꾼다: 구문 오류는 <code>SyntaxError</code>, 네임스페이스 오류는 <code>NamespaceError</code>, 형식 오류는 <code>TypeError</code>.
     *
     * @param e the error / 오류
     * @return the exception to throw / 던질 예외
     */
    private JsException error(XPathException e) {
        if (e.getKind() == XPathException.SYNTAX) {
            return page.getBridge().domError(DomException.syntax(e.getMessage()));
        } else if (e.getKind() == XPathException.NAMESPACE) {
            return page.getBridge().domError(DomException.namespace(e.getMessage()));
        }
        return JsException.typeError(e.getMessage());
    }

    /**
     * Wraps the resolver that a page gave (a function, an object with <code>lookupNamespaceURI</code>, or a node).
     * <p>
     * 페이지가 준 리졸버(함수, <code>lookupNamespaceURI</code> 가 있는 객체, 노드)를 감싼다.
     *
     * @param v the value / 값
     * @return the resolver, or null for none / 리졸버. 없으면 null
     */
    private NamespaceResolver resolver(final JsValue v) {
        if (v.isNullOrUndefined()) {
            return null;
        }
        if (!v.isObject()) {
            throw JsException.typeError("The resolver must be a function or an object with a lookupNamespaceURI method.");
        }
        final JsObject o = (JsObject) v;
        return new NamespaceResolver() {
            /**
             * Asks the resolver of the page.
             * <p>
             * 페이지의 리졸버에 묻는다.
             *
             * @param prefix the prefix / 접두사
             * @return the namespace or null / 네임스페이스 또는 null
             */
            public String lookupNamespaceURI(String prefix) {
                JsValue answer;
                if (o.isCallable()) {
                    answer = Ops.call(o, Js.UNDEFINED, new JsValue[] { Js.str(prefix) });
                } else {
                    JsValue f = o.get("lookupNamespaceURI");
                    if (!f.isCallable()) {
                        return null;
                    }
                    answer = Ops.call(f, o, new JsValue[] { Js.str(prefix) });
                }
                if (answer.isNullOrUndefined()) {
                    return null;
                }
                String text = Ops.toString(answer);
                return text.length() == 0 ? null : text;
            }
        };
    }

    /**
     * Evaluates an expression and makes the result of the requested type.
     * <p>
     * 식을 평가하고 요청한 종류의 결과를 만든다.
     *
     * @param e        the expression / 식
     * @param context  the context node or attribute / 컨텍스트 노드나 속성
     * @param resolver the resolver or null / 리졸버. 없으면 null
     * @param typeArg  the requested type value (undefined for <code>ANY_TYPE</code>) / 요청한 종류의 값(<code>ANY_TYPE</code> 이면 undefined)
     * @return the result object / 결과 객체
     */
    private JsValue run(XPathExpression e, Object context, NamespaceResolver resolver, JsValue typeArg) {
        double requested = typeArg.isUndefined() ? 0 : Ops.toNumber(typeArg);
        int type = Double.isNaN(requested) ? 0 : (int) requested;
        if (type < 0 || type > 9) {
            throw page.getBridge().domError(DomException.notSupported("The type '" + type + "' is not a valid XPathResult type."));
        }
        Object value;
        try {
            value = e.evaluate(context, resolver);
        } catch (XPathException x) {
            throw error(x);
        }
        boolean isSet = value instanceof List;
        // ANY_TYPE 은 값의 종류에 따라 정한다. 노드 집합이면 순서 없는 반복자이다.
        if (type == 0) {
            type = value instanceof Double ? 1 : value instanceof String ? 2 : value instanceof Boolean ? 3 : 4;
        } else if (type >= 4 && !isSet) {
            throw JsException.typeError("The result is not a node set, and therefore cannot be converted to the desired type.");
        }
        List<Object> nodes = null;
        if (isSet) {
            @SuppressWarnings("unchecked")
            List<Object> set = (List<Object>) value;
            nodes = type == 8 || type == 9 ? (set.isEmpty() ? set : set.subList(0, 1)) : set;
        }
        return new ResultObject(resultProto, type, XPathExpression.toNum(value), XPathExpression.toStr(value), XPathExpression.toBool(value), nodes, mutations);
    }
}
