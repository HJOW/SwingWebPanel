package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.dom.Highlight;
import org.duckdns.hjow.swingwebpanel.dom.HighlightRegistry;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.StaticRange;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.dom.CssomMembers;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * The script interfaces of the CSS Custom Highlight API Level 1 (https://www.w3.org/TR/css-highlight-api-1/, W3C Working Draft of 14 December 2021):
 * <code>Highlight</code> (section 3.1: a constructor that takes ranges, the setlike members <code>add</code>, <code>delete</code>, <code>has</code>,
 * <code>clear</code>, <code>size</code>, <code>forEach</code>, <code>values</code>, <code>keys</code>, <code>entries</code> and the iterator, and the
 * attributes <code>priority</code> and <code>type</code>), <code>HighlightRegistry</code> (section 3.2: the maplike members <code>set</code>, <code>get</code>,
 * <code>has</code>, <code>delete</code>, <code>clear</code>, <code>size</code>, <code>forEach</code>, <code>keys</code>, <code>values</code>,
 * <code>entries</code> and the iterator) and <code>CSS.highlights</code>, the registry of the document. Every change of the registry or of a registered
 * highlight paints the page again (section 5.1); a script that moves a boundary of a registered live range causes a new painting at the end of its task.
 * <p>
 * CSS Custom Highlight API Level 1 의 스크립트 인터페이스(W3C Working Draft 2021-12-14): <code>Highlight</code>(3.1 절: 범위를 받는 생성자, setlike 멤버 <code>add</code>,
 * <code>delete</code>, <code>has</code>, <code>clear</code>, <code>size</code>, <code>forEach</code>, <code>values</code>, <code>keys</code>, <code>entries</code> 와 반복자,
 * 속성 <code>priority</code>, <code>type</code>), <code>HighlightRegistry</code>(3.2 절: maplike 멤버 <code>set</code>, <code>get</code>, <code>has</code>, <code>delete</code>,
 * <code>clear</code>, <code>size</code>, <code>forEach</code>, <code>keys</code>, <code>values</code>, <code>entries</code> 와 반복자), 문서의 등록부인
 * <code>CSS.highlights</code>. 등록부나 등록된 하이라이트의 모든 변경은 페이지를 다시 그리게 하며(5.1 절), 스크립트가 등록된 살아 있는 범위의 경계를 옮기면 그 작업이 끝날 때
 * 다시 그린다.
 */
final class HighlightApi {

    /** The page. / 페이지. */
    private final PageScripts page;

    /** The boundaries of the registered ranges at the last check. / 마지막 확인 때 등록된 범위의 경계. */
    private String lastBoundaries = "";

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    HighlightApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Installs <code>Highlight</code>, <code>HighlightRegistry</code> and <code>CSS.highlights</code>.
     * <p>
     * <code>Highlight</code>, <code>HighlightRegistry</code>, <code>CSS.highlights</code> 를 설치한다.
     *
     * @param css the <code>CSS</code> namespace object / <code>CSS</code> 네임스페이스 객체
     */
    void install(JsObject css) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /** Chooses the interfaces of the highlight objects. / 하이라이트 객체의 인터페이스를 고른다. */
            public String classify(Object o) {
                return o instanceof Highlight ? "Highlight" : o instanceof HighlightRegistry ? "HighlightRegistry" : null;
            }
        });
        final DomBridge.InterfaceInfo hl = bridge.defineInterface("Highlight", null, Highlight.class, new String[0]);
        installHighlight(bridge, realm, hl);
        DomBridge.InterfaceInfo reg = bridge.defineInterface("HighlightRegistry", null, HighlightRegistry.class, new String[0]);
        installRegistry(bridge, realm, reg);
        final HighlightRegistry registry = page.getDocument().getHighlights();
        registry.setChangeListener(new Runnable() {
            /** Asks the view to paint the page again. / 뷰에 페이지를 다시 그리도록 요청한다. */
            public void run() {
                repaint();
            }
        });
        css.defineAccessor("highlights", realm.newFunction("get highlights", 0, new NativeFunction.Call() {
            /** Returns the highlight registry of the document. / 문서의 하이라이트 등록부를 반환한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(registry);
            }
        }), null, true, true);
        page.onTaskEnd(new Runnable() {
            /** Paints again when a script moved a boundary of a registered range. / 스크립트가 등록된 범위의 경계를 옮겼으면 다시 그린다. */
            public void run() {
                String now = boundaries(registry);
                if (!now.equals(lastBoundaries)) {
                    lastBoundaries = now;
                    repaint();
                }
            }
        });
    }

    /**
     * Asks the view to build the display list again from the retained layout and to paint it.
     * <p>
     * 뷰에 보관한 배치로 디스플레이 리스트를 다시 만들어 그리도록 요청한다.
     */
    private void repaint() {
        HostEnvironment env = page.getEnvironment();
        if (env != null) {
            env.canvasChanged(null);
        }
    }

    /**
     * Describes the boundaries of all the ranges of the registered highlights (to notice that a script moved one).
     * <p>
     * 등록된 하이라이트의 모든 범위의 경계를 기술한다(스크립트가 하나를 옮겼는지 알기 위함).
     *
     * @param registry the registry / 등록부
     * @return the description / 기술
     */
    private static String boundaries(HighlightRegistry registry) {
        if (registry.getSize() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        List<Map.Entry<String, Highlight>> entries = registry.getEntries();
        // 등록된 하이라이트의 살아 있는 범위마다 경계 노드와 오프셋을 적는다.
        for (int i = 0; i < entries.size(); i++) {
            List<Object> ranges = entries.get(i).getValue().getRanges();
            // 하이라이트의 범위 중 살아 있는 범위만 적는다(정적 범위는 바뀌지 않는다).
            for (int k = 0; k < ranges.size(); k++) {
                if (ranges.get(k) instanceof Range) {
                    Range r = (Range) ranges.get(k);
                    b.append(System.identityHashCode(r.getStartContainer())).append(':').append(r.getStartOffset()).append('-')
                            .append(System.identityHashCode(r.getEndContainer())).append(':').append(r.getEndOffset()).append(';');
                }
            }
        }
        return b.toString();
    }

    /**
     * Converts a script value to a range (<code>AbstractRange</code>), or throws the TypeError of a wrong argument.
     * <p>
     * 스크립트 값을 범위(<code>AbstractRange</code>)로 바꾸고, 틀린 인수이면 TypeError 를 던진다.
     *
     * @param v      the value / 값
     * @param method the method name for the message / 메시지용 메서드 이름
     * @return the range / 범위
     */
    private static Object range(JsValue v, String method) {
        Object r = DomBridge.targetOf(v, Range.class);
        if (r == null) {
            r = DomBridge.targetOf(v, StaticRange.class);
        }
        if (r == null) {
            throw JsException.typeError("Failed to execute '" + method + "' on 'Highlight': parameter 1 is not of type 'AbstractRange'.");
        }
        return r;
    }

    /**
     * Installs the constructor and the members of <code>Highlight</code>.
     * <p>
     * <code>Highlight</code> 의 생성자와 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     * @param hl     the interface / 인터페이스
     */
    private void installHighlight(final DomBridge bridge, final JsRealm realm, final DomBridge.InterfaceInfo hl) {
        bridge.setConstructor("Highlight", new NativeFunction(realm, "Highlight", 0, new NativeFunction.Call() {
            /** Calling without new is an error. / new 없이 부르면 오류이다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'Highlight': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /** Creates a highlight with the given ranges, priority 0 and type highlight. / 주어진 범위, 우선순위 0, 종류 highlight 인 하이라이트를 만든다. */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Highlight h = new Highlight();
                // 인수마다 범위로 바꿔 넣는다(범위가 아니면 TypeError).
                for (int i = 0; i < args.length; i++) {
                    h.add(range(args[i], "constructor"));
                }
                HostObject w = bridge.wrap(h);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, hl.getPrototype()));
                return w;
            }
        }));
        CssomMembers.method(bridge, hl, "add", 1, new CssomMembers.Op() {
            /** Adds a range and returns the highlight. / 범위를 더하고 하이라이트를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                ((Highlight) target).add(range(Js.arg(args, 0), "add"));
                return b.toJs(target);
            }
        });
        CssomMembers.method(bridge, hl, "delete", 1, new CssomMembers.Op() {
            /** Removes a range. / 범위를 뺀다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                Object r = rangeOrNull(Js.arg(args, 0));
                return Js.bool(r != null && ((Highlight) target).delete(r));
            }
        });
        CssomMembers.method(bridge, hl, "has", 1, new CssomMembers.Op() {
            /** Tells whether a range is in the set. / 범위가 집합에 있는지 알려 준다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                Object r = rangeOrNull(Js.arg(args, 0));
                return Js.bool(r != null && ((Highlight) target).has(r));
            }
        });
        CssomMembers.method(bridge, hl, "clear", 0, new CssomMembers.Op() {
            /** Removes every range. / 모든 범위를 뺀다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                ((Highlight) target).clear();
                return Js.UNDEFINED;
            }
        });
        CssomMembers.accessor(bridge, hl, "size", new CssomMembers.Op() {
            /** Returns the number of ranges. / 범위의 수를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.num(((Highlight) target).getSize());
            }
        }, null);
        CssomMembers.accessor(bridge, hl, "priority", new CssomMembers.Op() {
            /** Returns the priority. / 우선순위를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.num(((Highlight) target).getPriority());
            }
        }, new CssomMembers.Op() {
            /** Sets the priority (converted to a long as WebIDL does). / 우선순위를 정한다(WebIDL 처럼 long 으로 바꾼다). */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                ((Highlight) target).setPriority(Ops.toInt32(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        CssomMembers.accessor(bridge, hl, "type", new CssomMembers.Op() {
            /** Returns the type. / 종류를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.str(((Highlight) target).getType());
            }
        }, new CssomMembers.Op() {
            /** Sets the type; an unknown value is ignored. / 종류를 정한다. 모르는 값은 무시한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                ((Highlight) target).setType(Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        CssomMembers.method(bridge, hl, "forEach", 1, new CssomMembers.Op() {
            /** Calls a function with every range twice and the set, as a Set does. / Set 처럼 범위마다 범위 둘과 집합으로 함수를 부른다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to execute 'forEach' on 'Highlight': The callback provided as parameter 1 is not a function.");
                }
                List<Object> ranges = ((Highlight) target).getRanges();
                // 범위의 스냅샷을 순서대로 콜백에 넘긴다.
                for (int i = 0; i < ranges.size(); i++) {
                    JsValue w = b.toJs(ranges.get(i));
                    Ops.call(cb, Js.arg(args, 1), new JsValue[] { w, w, b.toJs(target) });
                }
                return Js.UNDEFINED;
            }
        });
        CssomMembers.method(bridge, hl, "values", 0, new CssomMembers.Op() {
            /** Returns an iterator over the ranges. / 범위에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return iterator(b, realm, ((Highlight) target).getRanges(), null);
            }
        });
        hl.getPrototype().defineData("keys", hl.getPrototype().get("values"), true, false, true);
        CssomMembers.method(bridge, hl, "entries", 0, new CssomMembers.Op() {
            /** Returns an iterator over [range, range] pairs. / [범위, 범위] 쌍에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                List<Object> ranges = ((Highlight) target).getRanges();
                return iterator(b, realm, ranges, ranges);
            }
        });
        hl.getPrototype().defineData(WellKnownSymbols.ITERATOR, hl.getPrototype().get("values"), true, false, true);
    }

    /**
     * Converts a script value to a range, or returns null if it is not one (<code>delete</code> and <code>has</code> of a set answer false then).
     * <p>
     * 스크립트 값을 범위로 바꾼다. 범위가 아니면 null(그러면 집합의 <code>delete</code> 와 <code>has</code> 는 false 이다).
     *
     * @param v the value / 값
     * @return the range or null / 범위 또는 null
     */
    private static Object rangeOrNull(JsValue v) {
        Object r = DomBridge.targetOf(v, Range.class);
        return r != null ? r : DomBridge.targetOf(v, StaticRange.class);
    }

    /**
     * Installs the members of <code>HighlightRegistry</code>.
     * <p>
     * <code>HighlightRegistry</code> 의 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     * @param reg    the interface / 인터페이스
     */
    private void installRegistry(final DomBridge bridge, final JsRealm realm, final DomBridge.InterfaceInfo reg) {
        CssomMembers.method(bridge, reg, "set", 2, new CssomMembers.Op() {
            /** Registers a highlight under a name and returns the registry. / 이름으로 하이라이트를 등록하고 등록부를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                Object h = DomBridge.targetOf(Js.arg(args, 1), Highlight.class);
                if (h == null) {
                    throw JsException.typeError("Failed to execute 'set' on 'HighlightRegistry': parameter 2 is not of type 'Highlight'.");
                }
                ((HighlightRegistry) target).set(Ops.toString(Js.arg(args, 0)), (Highlight) h);
                return b.toJs(target);
            }
        });
        CssomMembers.method(bridge, reg, "get", 1, new CssomMembers.Op() {
            /** Returns the highlight of a name or undefined. / 이름의 하이라이트나 undefined 를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                Highlight h = ((HighlightRegistry) target).get(Ops.toString(Js.arg(args, 0)));
                return h == null ? Js.UNDEFINED : b.toJs(h);
            }
        });
        CssomMembers.method(bridge, reg, "has", 1, new CssomMembers.Op() {
            /** Tells whether a name is registered. / 이름이 등록되어 있는지 알려 준다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.bool(((HighlightRegistry) target).has(Ops.toString(Js.arg(args, 0))));
            }
        });
        CssomMembers.method(bridge, reg, "delete", 1, new CssomMembers.Op() {
            /** Removes a name. / 이름을 지운다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.bool(((HighlightRegistry) target).delete(Ops.toString(Js.arg(args, 0))));
            }
        });
        CssomMembers.method(bridge, reg, "clear", 0, new CssomMembers.Op() {
            /** Removes every name. / 모든 이름을 지운다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                ((HighlightRegistry) target).clear();
                return Js.UNDEFINED;
            }
        });
        CssomMembers.accessor(bridge, reg, "size", new CssomMembers.Op() {
            /** Returns the number of names. / 이름의 수를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.num(((HighlightRegistry) target).getSize());
            }
        }, null);
        CssomMembers.method(bridge, reg, "forEach", 1, new CssomMembers.Op() {
            /** Calls a function with every highlight, its name and the registry, as a Map does. / Map 처럼 하이라이트마다 하이라이트, 이름, 등록부로 함수를 부른다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to execute 'forEach' on 'HighlightRegistry': The callback provided as parameter 1 is not a function.");
                }
                List<Map.Entry<String, Highlight>> entries = ((HighlightRegistry) target).getEntries();
                // 항목의 스냅샷을 등록 순서로 콜백에 넘긴다.
                for (int i = 0; i < entries.size(); i++) {
                    Ops.call(cb, Js.arg(args, 1), new JsValue[] { b.toJs(entries.get(i).getValue()), Js.str(entries.get(i).getKey()), b.toJs(target) });
                }
                return Js.UNDEFINED;
            }
        });
        CssomMembers.method(bridge, reg, "entries", 0, new CssomMembers.Op() {
            /** Returns an iterator over [name, highlight] pairs. / [이름, 하이라이트] 쌍에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return mapIterator(b, realm, (HighlightRegistry) target, 2);
            }
        });
        CssomMembers.method(bridge, reg, "keys", 0, new CssomMembers.Op() {
            /** Returns an iterator over the names. / 이름에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return mapIterator(b, realm, (HighlightRegistry) target, 0);
            }
        });
        CssomMembers.method(bridge, reg, "values", 0, new CssomMembers.Op() {
            /** Returns an iterator over the highlights. / 하이라이트에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return mapIterator(b, realm, (HighlightRegistry) target, 1);
            }
        });
        reg.getPrototype().defineData(WellKnownSymbols.ITERATOR, reg.getPrototype().get("entries"), true, false, true);
    }

    /**
     * Creates an iterator over a snapshot of objects (as values, or as [value, value] pairs when <code>pairs</code> is given).
     * <p>
     * 객체 스냅샷에 대한 반복자를 만든다(값으로, 또는 <code>pairs</code> 가 있으면 [값, 값] 쌍으로).
     *
     * @param b     the bridge / 브리지
     * @param realm the realm / realm
     * @param items the objects / 객체
     * @param pairs the same list for pairs, or null / 쌍이면 같은 목록. 아니면 null
     * @return the iterator / 반복자
     */
    private static JsValue iterator(DomBridge b, JsRealm realm, List<Object> items, List<Object> pairs) {
        JsValue[] out = new JsValue[items.size()];
        // 객체의 스크립트 값(또는 쌍)으로 배열을 만든다.
        for (int i = 0; i < out.length; i++) {
            JsValue w = b.toJs(items.get(i));
            out[i] = pairs == null ? w : realm.newArray(new JsValue[] { w, w });
        }
        JsArray array = realm.newArray(out);
        return Ops.call(array.get("values"), array, new JsValue[0]);
    }

    /**
     * Creates an iterator over a snapshot of the registry.
     * <p>
     * 등록부 스냅샷에 대한 반복자를 만든다.
     *
     * @param b        the bridge / 브리지
     * @param realm    the realm / realm
     * @param registry the registry / 등록부
     * @param kind     0 for the names, 1 for the highlights, 2 for [name, highlight] pairs / 0 은 이름, 1 은 하이라이트, 2 는 [이름, 하이라이트] 쌍
     * @return the iterator / 반복자
     */
    private static JsValue mapIterator(DomBridge b, JsRealm realm, HighlightRegistry registry, int kind) {
        List<Map.Entry<String, Highlight>> entries = registry.getEntries();
        JsValue[] out = new JsValue[entries.size()];
        // 항목마다 원하는 모양의 값을 만든다.
        for (int i = 0; i < out.length; i++) {
            JsValue name = Js.str(entries.get(i).getKey());
            JsValue value = b.toJs(entries.get(i).getValue());
            out[i] = kind == 0 ? name : kind == 1 ? value : realm.newArray(new JsValue[] { name, value });
        }
        JsArray array = realm.newArray(out);
        return Ops.call(array.get("values"), array, new JsValue[0]);
    }
}
