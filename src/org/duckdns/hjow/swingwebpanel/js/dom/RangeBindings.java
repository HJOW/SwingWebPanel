package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.NodeFilter;
import org.duckdns.hjow.swingwebpanel.dom.NodeIterator;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.Selection;
import org.duckdns.hjow.swingwebpanel.dom.TreeWalker;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The JavaScript interfaces of ranges, selections and traversal (plan item P20-03): <code>Range</code> with its constructor,
 * <code>Selection</code>, <code>NodeIterator</code>, <code>TreeWalker</code> and the <code>NodeFilter</code> constants, and the members that
 * create them: <code>document.createRange()</code>, <code>createNodeIterator()</code>, <code>createTreeWalker()</code>,
 * <code>document.getSelection()</code> and <code>window.getSelection()</code>. The geometry of ranges (<code>getBoundingClientRect</code>)
 * needs the layout of the page and is added by the host (see <code>RangeGeometry</code> of the host package).
 * <p>
 * 범위, 선택, 탐색의 JavaScript 인터페이스(계획 P20-03): 생성자가 있는 <code>Range</code>, <code>Selection</code>, <code>NodeIterator</code>,
 * <code>TreeWalker</code>, <code>NodeFilter</code> 상수와 이것들을 만드는 멤버 <code>document.createRange()</code>, <code>createNodeIterator()</code>,
 * <code>createTreeWalker()</code>, <code>document.getSelection()</code>, <code>window.getSelection()</code>. 범위의 기하(<code>getBoundingClientRect</code>)는
 * 페이지의 레이아웃이 필요하므로 호스트가 더한다(호스트 패키지의 <code>RangeGeometry</code> 참조).
 */
public final class RangeBindings {

    /**
     * The bit mask that shows every kind of node (an unsigned 32 bit number does not fit an int constant specification).
     * <p>
     * 모든 노드 종류를 보이는 비트 마스크(부호 없는 32비트 수는 int 상수 명세에 들어가지 않는다).
     */
    private static final double SHOW_ALL = 4294967295.0;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RangeBindings() {
    }

    /**
     * Returns the document of a <code>this</code> value.
     * <p>
     * <code>this</code> 값의 문서를 반환한다.
     *
     * @param self this value / this 값
     * @return the document / 문서
     */
    private static Document documentOf(JsValue self) {
        Document d = (Document) DomBridge.targetOf(self, Document.class);
        if (d == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return d;
    }

    /**
     * Returns the node that an argument must be (a TypeError otherwise, as WebIDL says for a non-nullable interface argument).
     * <p>
     * 인수가 가져야 하는 노드를 반환한다(WebIDL 의 null 불가 인터페이스 인수처럼 아니면 TypeError).
     *
     * @param v    the argument / 인수
     * @param what the description for the message / 메시지용 설명
     * @return the node / 노드
     */
    private static Node nodeArg(JsValue v, String what) {
        Node n = (Node) DomBridge.targetOf(v, Node.class);
        if (n == null) {
            throw JsException.typeError("Failed to execute '" + what + "': parameter 1 is not of type 'Node'.");
        }
        return n;
    }

    /**
     * Returns the document that a global object of the page stands for (the document of <code>new Range()</code> and
     * <code>getSelection()</code>).
     * <p>
     * 페이지의 전역 객체가 가리키는 문서를 반환한다(<code>new Range()</code> 와 <code>getSelection()</code> 의 문서).
     *
     * @param realm the realm / realm
     * @return the document / 문서
     */
    private static Document globalDocument(JsRealm realm) {
        Document d = (Document) DomBridge.targetOf(realm.getGlobalObject().get("document"), Document.class);
        if (d == null) {
            throw JsException.typeError("the page has no document");
        }
        return d;
    }

    /**
     * Installs the interfaces and the members that create them.
     * <p>
     * 인터페이스와 그것을 만드는 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a range, selection or traversal object.
             * <p>
             * 범위, 선택, 탐색 객체의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof Range) {
                    return "Range";
                } else if (o instanceof Selection) {
                    return "Selection";
                } else if (o instanceof NodeIterator) {
                    return "NodeIterator";
                } else if (o instanceof TreeWalker) {
                    return "TreeWalker";
                }
                return null;
            }
        });
        DomBridge.InterfaceInfo nf = bridge.defineInterface("NodeFilter", null, NodeFilter.class, new String[] { "c:FILTER_ACCEPT=1", "c:FILTER_REJECT=2",
            "c:FILTER_SKIP=3", "c:SHOW_ELEMENT=1", "c:SHOW_ATTRIBUTE=2", "c:SHOW_TEXT=4", "c:SHOW_CDATA_SECTION=8", "c:SHOW_ENTITY_REFERENCE=16",
            "c:SHOW_ENTITY=32", "c:SHOW_PROCESSING_INSTRUCTION=64", "c:SHOW_COMMENT=128", "c:SHOW_DOCUMENT=256", "c:SHOW_DOCUMENT_TYPE=512",
            "c:SHOW_DOCUMENT_FRAGMENT=1024", "c:SHOW_NOTATION=2048" });
        nf.getPrototype().defineData("SHOW_ALL", Js.num(SHOW_ALL), false, true, false);
        nf.getConstructor().defineData("SHOW_ALL", Js.num(SHOW_ALL), false, true, false);
        final DomBridge.InterfaceInfo range = bridge.defineInterface("Range", null, Range.class, new String[] { "c:START_TO_START=0", "c:START_TO_END=1",
            "c:END_TO_END=2", "c:END_TO_START=3", "a:startContainer", "a:startOffset", "a:endContainer", "a:endOffset", "a:collapsed@Collapsed",
            "a:commonAncestorContainer", "m:setStart(Node,int)", "m:setEnd(Node,int)", "m:setStartBefore(Node)", "m:setStartAfter(Node)",
            "m:setEndBefore(Node)", "m:setEndAfter(Node)", "m:collapse(boolean*)", "m:selectNode(Node)", "m:selectNodeContents(Node)",
            "m:compareBoundaryPoints(short,Range)", "m:deleteContents()", "m:extractContents()", "m:cloneContents()", "m:insertNode(Node)",
            "m:surroundContents(Node)", "m:cloneRange()", "m:detach()", "m:isPointInRange(Node,int)", "m:comparePoint(Node,int)",
            "m:intersectsNode(Node)", "m:toString()", "m:createContextualFragment(String)" });
        bridge.setConstructor("Range", new NativeFunction(realm, realm.getFunctionPrototype(), "Range", 0, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'Range': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a range at the start of the document of the page.
             * <p>
             * 페이지 문서의 시작에 범위를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the range wrapper / 범위 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(globalDocument(r).createRange());
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, range.getPrototype()));
                return w;
            }
        }));
        bridge.defineInterface("Selection", null, Selection.class, new String[] { "a:anchorNode", "a:anchorOffset", "a:focusNode", "a:focusOffset",
            "a:isCollapsed@Collapsed", "a:rangeCount", "a:type", "a:direction", "m:getRangeAt(int)", "m:addRange(Range)", "m:removeRange(Range)",
            "m:removeAllRanges()", "m:empty()@removeAllRanges", "m:collapse(Node?,int*)", "m:setPosition(Node?,int*)@collapse", "m:collapseToStart()",
            "m:collapseToEnd()", "m:extend(Node,int*)", "m:setBaseAndExtent(Node,int,Node,int)", "m:selectAllChildren(Node)",
            "m:deleteFromDocument()", "m:containsNode(Node,boolean*)", "m:toString()" });
        DomBridge.InterfaceInfo it = bridge.defineInterface("NodeIterator", null, NodeIterator.class, new String[] { "a:root", "a:referenceNode",
            "a:pointerBeforeReferenceNode@PointerBeforeReferenceNode", "a:whatToShow", "m:nextNode()", "m:previousNode()", "m:detach()" });
        defineFilterAttribute(bridge, it.getPrototype(), NodeIterator.class);
        DomBridge.InterfaceInfo tw = bridge.defineInterface("TreeWalker", null, TreeWalker.class, new String[] { "a:root", "a:whatToShow",
            "a:currentNode=", "m:parentNode()", "m:firstChild()", "m:lastChild()", "m:previousSibling()", "m:nextSibling()", "m:previousNode()",
            "m:nextNode()" });
        defineFilterAttribute(bridge, tw.getPrototype(), TreeWalker.class);
        installDocumentMembers(bridge);
        installGlobalSelection(bridge);
    }

    /**
     * Defines the read-only <code>filter</code> attribute of a traversal interface: the function or object that the script gave, or null.
     * <p>
     * 탐색 인터페이스의 읽기 전용 <code>filter</code> 속성을 정의한다: 스크립트가 준 함수나 객체, 없으면 null.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype of the interface / 인터페이스의 프로토타입
     * @param cls    the Java class of the interface / 인터페이스의 Java 클래스
     */
    private static void defineFilterAttribute(final DomBridge bridge, JsObject proto, final Class<?> cls) {
        proto.defineAccessor("filter", bridge.getRealm().newFunction("get filter", 0, new NativeFunction.Call() {
            /**
             * Returns the filter of the script.
             * <p>
             * 스크립트의 필터를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the filter or null / 필터 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Object t = DomBridge.targetOf(self, cls);
                if (t == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                NodeFilter f = cls == NodeIterator.class ? ((NodeIterator) t).getFilter() : ((TreeWalker) t).getFilter();
                return f instanceof JsNodeFilter ? ((JsNodeFilter) f).getCallback() : Js.NULL;
            }
        }), null, true, true);
    }

    /**
     * Defines the members of <code>document</code> that create ranges, iterators, walkers and the selection.
     * <p>
     * 범위, 반복자, 걷는 도구, 선택을 만드는 <code>document</code> 의 멤버를 정의한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installDocumentMembers(final DomBridge bridge) {
        JsObject proto = bridge.getInterface("Document").getPrototype();
        JsRealm realm = bridge.getRealm();
        proto.defineData("createRange", realm.newFunction("createRange", 0, new NativeFunction.Call() {
            /**
             * Creates a range at the start of the document.
             * <p>
             * 문서의 시작에 범위를 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the range / 범위
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.wrap(documentOf(self).createRange());
            }
        }), true, true, true);
        proto.defineData("createNodeIterator", realm.newFunction("createNodeIterator", 1, new NativeFunction.Call() {
            /**
             * Creates a node iterator.
             * <p>
             * 노드 반복자를 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: root, whatToShow, filter / 인수: root, whatToShow, filter
             * @return the iterator / 반복자
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Document d = documentOf(self);
                return bridge.wrap(d.createNodeIterator(nodeArg(Js.arg(args, 0), "createNodeIterator"), whatToShow(Js.arg(args, 1)),
                        JsNodeFilter.of(bridge, Js.arg(args, 2))));
            }
        }), true, true, true);
        proto.defineData("createTreeWalker", realm.newFunction("createTreeWalker", 1, new NativeFunction.Call() {
            /**
             * Creates a tree walker.
             * <p>
             * 트리 걷기 도구를 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: root, whatToShow, filter / 인수: root, whatToShow, filter
             * @return the walker / 걷는 도구
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Document d = documentOf(self);
                return bridge.wrap(d.createTreeWalker(nodeArg(Js.arg(args, 0), "createTreeWalker"), whatToShow(Js.arg(args, 1)),
                        JsNodeFilter.of(bridge, Js.arg(args, 2))));
            }
        }), true, true, true);
        proto.defineData("getSelection", realm.newFunction("getSelection", 0, new NativeFunction.Call() {
            /**
             * Returns the selection of the document.
             * <p>
             * 문서의 선택을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the selection / 선택
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.wrap(documentOf(self).getSelection());
            }
        }), true, true, true);
    }

    /**
     * Converts the <code>whatToShow</code> argument (an unsigned long that defaults to the mask of every kind).
     * <p>
     * <code>whatToShow</code> 인수를 바꾼다(모든 종류의 마스크가 기본값인 unsigned long).
     *
     * @param v the argument / 인수
     * @return the mask / 마스크
     */
    private static long whatToShow(JsValue v) {
        return v.isUndefined() ? NodeFilter.SHOW_ALL : Ops.toUint32(v);
    }

    /**
     * Defines <code>window.getSelection()</code> on the global object (it returns the selection of the document of the page).
     * <p>
     * 전역 객체에 <code>window.getSelection()</code> 을 정의한다(페이지 문서의 선택을 반환한다).
     *
     * @param bridge the bridge / 브리지
     */
    private static void installGlobalSelection(final DomBridge bridge) {
        JsRealm realm = bridge.getRealm();
        realm.getGlobalObject().defineData("getSelection", realm.newFunction("getSelection", 0, new NativeFunction.Call() {
            /**
             * Returns the selection of the document of the page.
             * <p>
             * 페이지 문서의 선택을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the selection / 선택
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.wrap(globalDocument(r).getSelection());
            }
        }), true, true, true);
    }
}
