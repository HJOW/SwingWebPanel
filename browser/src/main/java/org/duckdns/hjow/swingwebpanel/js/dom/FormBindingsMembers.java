package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.FileList;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlButtonElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptGroupElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionsCollection;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlTextAreaElement;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The members of the form interfaces that a specification string of {@link DomBridge#defineInterface(String, String, Class, String[])} cannot
 * express: nullable numbers (<code>selectionStart</code>), <code>valueAsDate</code> (a <code>Date</code> object), <code>files</code>,
 * <code>stepUp</code>/<code>stepDown</code> (the default argument is 1), <code>setRangeText</code> (two forms), and the
 * <code>add</code>/<code>remove</code> of <code>select</code> and its options collection (an element or an index).
 * <p>
 * {@link DomBridge#defineInterface(String, String, Class, String[])} 의 명세 문자열로 표현할 수 없는 폼 인터페이스의 멤버: null 이 될 수 있는 수
 * (<code>selectionStart</code>), <code>valueAsDate</code>(<code>Date</code> 객체), <code>files</code>, <code>stepUp</code>/<code>stepDown</code>(기본 인수가 1),
 * <code>setRangeText</code>(두 가지 형태), <code>select</code> 와 그 options 컬렉션의 <code>add</code>/<code>remove</code>(요소 또는 위치).
 */
final class FormBindingsMembers {

    /**
     * The code of a custom member: it gets the Java target and the arguments.
     * <p>
     * 사용자 정의 멤버의 코드: Java 대상과 인수를 받는다.
     */
    private abstract static class Op {
        /**
         * Runs the member.
         * <p>
         * 멤버를 실행한다.
         *
         * @param target the Java target / Java 대상
         * @param args   the arguments / 인수
         * @return the result / 결과
         */
        abstract JsValue run(Object target, JsValue[] args);
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormBindingsMembers() {
    }

    /**
     * Defines a method on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the <code>length</code> of the function / 함수의 <code>length</code>
     * @param type   the Java class of the receiver / 수신자의 Java 클래스
     * @param op     the code / 코드
     */
    private static void method(final DomBridge bridge, JsObject proto, final String name, int length, final Class<?> type, final Op op) {
        proto.defineData(name, bridge.getRealm().newFunction(name, length, new NativeFunction.Call() {
            /**
             * Checks the receiver and runs the code; a DOM exception becomes a <code>DOMException</code>.
             * <p>
             * 수신자를 확인하고 코드를 실행한다. DOM 예외는 <code>DOMException</code> 이 된다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the result / 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Object t = DomBridge.targetOf(self, type);
                if (t == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                try {
                    return op.run(t, args);
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        }), true, false, true);
    }

    /**
     * Defines an accessor property on a prototype.
     * <p>
     * 프로토타입에 접근자 프로퍼티를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param type   the Java class of the receiver / 수신자의 Java 클래스
     * @param get    the getter code / getter 코드
     * @param set    the setter code, or null for a read-only attribute / setter 코드. 읽기 전용이면 null
     */
    private static void accessor(final DomBridge bridge, JsObject proto, final String name, final Class<?> type, final Op get, final Op set) {
        JsRealm realm = bridge.getRealm();
        JsObject getFn = realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            /**
             * Reads the attribute.
             * <p>
             * 속성을 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Object t = DomBridge.targetOf(self, type);
                if (t == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                return get.run(t, args);
            }
        });
        JsObject setFn = null;
        if (set != null) {
            setFn = realm.newFunction("set " + name, 1, new NativeFunction.Call() {
                /**
                 * Writes the attribute.
                 * <p>
                 * 속성을 쓴다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    Object t = DomBridge.targetOf(self, type);
                    if (t == null) {
                        throw JsException.typeError("Illegal invocation");
                    }
                    try {
                        set.run(t, args);
                    } catch (DomException e) {
                        throw bridge.domError(e);
                    }
                    return Js.UNDEFINED;
                }
            });
        }
        proto.defineAccessor(name, getFn, setFn, true, true);
    }

    /**
     * Converts a script value to an unsigned index argument (negative numbers become large like in WebIDL, so a bad index is out of range).
     * <p>
     * 스크립트 값을 부호 없는 위치 인수로 바꾼다(음수는 WebIDL 처럼 큰 수가 되어 잘못된 위치는 범위를 벗어난다).
     *
     * @param v the value / 값
     * @return the number, at most {@link Integer#MAX_VALUE} / 수. 최대 {@link Integer#MAX_VALUE}
     */
    private static int index(JsValue v) {
        long n = Ops.toUint32(v);
        return n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n;
    }

    /**
     * Installs the members of the selection text controls that need code (<code>selectionStart</code>, <code>selectionEnd</code>,
     * <code>setRangeText</code>) on an interface; the selection API of an input does not apply to every type.
     * <p>
     * 코드가 필요한 선택 텍스트 컨트롤의 멤버(<code>selectionStart</code>, <code>selectionEnd</code>, <code>setRangeText</code>)를 인터페이스에 설치한다. input 의 선택 API 는
     * 모든 종류에 적용되지 않는다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     */
    private static void installSelection(final DomBridge bridge, DomBridge.InterfaceInfo info) {
        method(bridge, info.getPrototype(), "setRangeText", 1, info.javaClass, new Op() {
            /**
             * Replaces the selected text or a range of the text.
             * <p>
             * 선택한 글이나 글의 범위를 바꾼다.
             *
             * @param t    the control / 컨트롤
             * @param args (replacement[, start, end[, mode]]) / (replacement[, start, end[, mode]])
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'setRangeText': 1 argument required, but only 0 present.");
                }
                String text = Ops.toString(args[0]);
                if (args.length >= 3) {
                    int start = index(args[1]);
                    int end = index(args[2]);
                    String mode = args.length > 3 && !args[3].isUndefined() ? Ops.toString(args[3]) : "preserve";
                    if (t instanceof HtmlInputElement) {
                        ((HtmlInputElement) t).setRangeText(text, start, end, mode);
                    } else {
                        ((HtmlTextAreaElement) t).setRangeText(text, start, end, mode);
                    }
                } else if (t instanceof HtmlInputElement) {
                    ((HtmlInputElement) t).setRangeText(text);
                } else {
                    ((HtmlTextAreaElement) t).setRangeText(text);
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the custom members of <code>HTMLInputElement</code>.
     * <p>
     * <code>HTMLInputElement</code> 의 사용자 정의 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     */
    static void installInput(final DomBridge bridge, DomBridge.InterfaceInfo info) {
        JsObject proto = info.getPrototype();
        accessor(bridge, proto, "selectionStart", HtmlInputElement.class, new Op() {
            /**
             * Reads the start of the selection, or null if the selection API does not apply to the type.
             * <p>
             * 선택의 시작을 읽는다. 이 종류에 선택 API 가 적용되지 않으면 null.
             *
             * @param t    the input / input
             * @param args unused / 쓰지 않음
             * @return the start or null / 시작 또는 null
             */
            JsValue run(Object t, JsValue[] args) {
                Integer v = ((HtmlInputElement) t).getSelectionStart();
                return v == null ? Js.NULL : Js.num(v.intValue());
            }
        }, new Op() {
            /**
             * Sets the start of the selection.
             * <p>
             * 선택의 시작을 지정한다.
             *
             * @param t    the input / input
             * @param args (value) / (값)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                ((HtmlInputElement) t).setSelectionStart(v.isNull() ? null : Integer.valueOf(index(v)));
                return Js.UNDEFINED;
            }
        });
        accessor(bridge, proto, "selectionEnd", HtmlInputElement.class, new Op() {
            /**
             * Reads the end of the selection, or null if the selection API does not apply to the type.
             * <p>
             * 선택의 끝을 읽는다. 이 종류에 선택 API 가 적용되지 않으면 null.
             *
             * @param t    the input / input
             * @param args unused / 쓰지 않음
             * @return the end or null / 끝 또는 null
             */
            JsValue run(Object t, JsValue[] args) {
                Integer v = ((HtmlInputElement) t).getSelectionEnd();
                return v == null ? Js.NULL : Js.num(v.intValue());
            }
        }, new Op() {
            /**
             * Sets the end of the selection.
             * <p>
             * 선택의 끝을 지정한다.
             *
             * @param t    the input / input
             * @param args (value) / (값)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                ((HtmlInputElement) t).setSelectionEnd(v.isNull() ? null : Integer.valueOf(index(v)));
                return Js.UNDEFINED;
            }
        });
        installSelection(bridge, info);
        accessor(bridge, proto, "valueAsDate", HtmlInputElement.class, new Op() {
            /**
             * Reads the value as a <code>Date</code>, or null.
             * <p>
             * 값을 <code>Date</code> 로 읽는다. 없으면 null.
             *
             * @param t    the input / input
             * @param args unused / 쓰지 않음
             * @return the date or null / 날짜 또는 null
             */
            JsValue run(Object t, JsValue[] args) {
                Double ms = ((HtmlInputElement) t).getValueAsDateMillis();
                if (ms == null) {
                    return Js.NULL;
                }
                JsValue date = bridge.getRealm().getGlobalObject().get("Date");
                return Ops.construct(date, new JsValue[] { Js.num(ms.doubleValue()) });
            }
        }, new Op() {
            /**
             * Sets the value from a <code>Date</code> (or null).
             * <p>
             * <code>Date</code>(또는 null)로 값을 지정한다.
             *
             * @param t    the input / input
             * @param args (date) / (날짜)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                if (v.isNull()) {
                    ((HtmlInputElement) t).setValueAsDateMillis(null);
                    return Js.UNDEFINED;
                }
                JsValue getTime = v.isObject() ? ((JsObject) v).get("getTime") : Js.UNDEFINED;
                if (!getTime.isCallable()) {
                    throw JsException.typeError("Failed to set the 'valueAsDate' property on 'HTMLInputElement': The provided value is not a Date.");
                }
                double ms = Ops.toNumber(Ops.call(getTime, v, Js.NO_ARGS));
                ((HtmlInputElement) t).setValueAsDateMillis(Double.isNaN(ms) ? null : Double.valueOf(ms));
                return Js.UNDEFINED;
            }
        });
        accessor(bridge, proto, "files", HtmlInputElement.class, new Op() {
            /**
             * Reads the selected files of a file upload control, or null for other types.
             * <p>
             * 파일 업로드 컨트롤의 선택한 파일을 읽는다. 다른 종류이면 null.
             *
             * @param t    the input / input
             * @param args unused / 쓰지 않음
             * @return the file list or null / 파일 목록 또는 null
             */
            JsValue run(Object t, JsValue[] args) {
                FileList l = ((HtmlInputElement) t).getFiles();
                return l == null ? Js.NULL : bridge.toJs(l);
            }
        }, null);
        method(bridge, proto, "stepUp", 0, HtmlInputElement.class, new Op() {
            /**
             * Steps the value up by n steps (default 1).
             * <p>
             * 값을 n 단계(기본 1) 올린다.
             *
             * @param t    the input / input
             * @param args (n) / (n)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                ((HtmlInputElement) t).stepUp(args.length > 0 && !args[0].isUndefined() ? Ops.toInt32(args[0]) : 1);
                return Js.UNDEFINED;
            }
        });
        method(bridge, proto, "stepDown", 0, HtmlInputElement.class, new Op() {
            /**
             * Steps the value down by n steps (default 1).
             * <p>
             * 값을 n 단계(기본 1) 내린다.
             *
             * @param t    the input / input
             * @param args (n) / (n)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                ((HtmlInputElement) t).stepDown(args.length > 0 && !args[0].isUndefined() ? Ops.toInt32(args[0]) : 1);
                return Js.UNDEFINED;
            }
        });
    }


    /**
     * Installs the custom members of <code>HTMLFormElement</code>: <code>requestSubmit</code>, which throws a <code>TypeError</code> for a submitter
     * that is not a submit button (the Java method reports an <code>InvalidStateError</code> to Java callers).
     * <p>
     * <code>HTMLFormElement</code> 의 사용자 정의 멤버를 설치한다: <code>requestSubmit</code> 은 제출 버튼이 아닌 submitter 에 <code>TypeError</code> 를 던진다
     * (Java 메서드는 Java 호출자에게 <code>InvalidStateError</code> 를 알린다).
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     */
    static void installForm(DomBridge bridge, DomBridge.InterfaceInfo info) {
        method(bridge, info.getPrototype(), "requestSubmit", 0, HtmlFormElement.class, new Op() {
            /**
             * Submits the form as if the submitter had been clicked.
             * <p>
             * submitter 를 클릭한 것처럼 폼을 제출한다.
             *
             * @param t    the form / 폼
             * @param args (submitter) or nothing / (submitter) 또는 없음
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                Element submitter = null;
                if (!v.isNullOrUndefined()) {
                    submitter = (Element) DomBridge.targetOf(v, Element.class);
                    boolean ok = (submitter instanceof HtmlButtonElement && ((HtmlButtonElement) submitter).isSubmitButton())
                            || (submitter instanceof HtmlInputElement && ((HtmlInputElement) submitter).isSubmitButton());
                    if (!ok) {
                        throw JsException.typeError("Failed to execute 'requestSubmit' on 'HTMLFormElement': The specified element is not a submit button.");
                    }
                }
                ((HtmlFormElement) t).requestSubmit(submitter);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the custom members of <code>HTMLTextAreaElement</code>.
     * <p>
     * <code>HTMLTextAreaElement</code> 의 사용자 정의 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     */
    static void installTextArea(DomBridge bridge, DomBridge.InterfaceInfo info) {
        installSelection(bridge, info);
    }

    /**
     * Converts the arguments of <code>add(element, before)</code> and calls the select.
     * <p>
     * <code>add(element, before)</code> 의 인수를 바꿔 select 에 전달한다.
     *
     * @param bridge the bridge / 브리지
     * @param select the select / select
     * @param args   (element, before) / (element, before)
     */
    private static void add(DomBridge bridge, HtmlSelectElement select, JsValue[] args) {
        if (args.length < 1) {
            throw JsException.typeError("Failed to execute 'add' on 'HTMLSelectElement': 1 argument required, but only 0 present.");
        }
        Element element = (Element) DomBridge.targetOf(args[0], Element.class);
        if (!(element instanceof HtmlOptionElement) && !(element instanceof HtmlOptGroupElement)) {
            throw JsException.typeError("Failed to execute 'add' on 'HTMLSelectElement': parameter 1 is not of type 'HTMLOptionElement or "
                    + "HTMLOptGroupElement'.");
        }
        JsValue before = Js.arg(args, 1);
        if (before.isNullOrUndefined()) {
            select.add(element, null);
        } else if (before instanceof org.duckdns.hjow.swingwebpanel.js.JsNumber) {
            select.addBeforeIndex(element, Ops.toInt32(before));
        } else {
            Element ref = (Element) DomBridge.targetOf(before, Element.class);
            if (ref == null) {
                throw JsException.typeError("Failed to execute 'add' on 'HTMLSelectElement': parameter 2 is not of type 'HTMLElement or long'.");
            }
            select.add(element, ref);
        }
    }

    /**
     * Installs the custom members of <code>HTMLSelectElement</code>: <code>add</code> and <code>remove</code>.
     * <p>
     * <code>HTMLSelectElement</code> 의 사용자 정의 멤버 <code>add</code>, <code>remove</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     */
    static void installSelect(final DomBridge bridge, DomBridge.InterfaceInfo info) {
        method(bridge, info.getPrototype(), "add", 1, HtmlSelectElement.class, new Op() {
            /**
             * Adds an option or optgroup before an element or index.
             * <p>
             * option 이나 optgroup 을 요소나 위치 앞에 추가한다.
             *
             * @param t    the select / select
             * @param args (element, before) / (element, before)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                add(bridge, (HtmlSelectElement) t, args);
                return Js.UNDEFINED;
            }
        });
        method(bridge, info.getPrototype(), "remove", 0, HtmlSelectElement.class, new Op() {
            /**
             * Without an argument the select removes itself from its parent; with an index it removes that option.
             * <p>
             * 인수가 없으면 select 가 부모에서 스스로를 제거하고, 위치가 있으면 그 option 을 제거한다.
             *
             * @param t    the select / select
             * @param args (index) or nothing / (index) 또는 없음
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                if (args.length == 0) {
                    ((HtmlSelectElement) t).remove();
                } else {
                    ((HtmlSelectElement) t).removeOption(Ops.toInt32(args[0]));
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>add</code> of <code>HTMLOptionsCollection</code>.
     * <p>
     * <code>HTMLOptionsCollection</code> 의 <code>add</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     */
    static void installOptionsAdd(final DomBridge bridge, DomBridge.InterfaceInfo info) {
        method(bridge, info.getPrototype(), "add", 1, HtmlOptionsCollection.class, new Op() {
            /**
             * Adds an option or optgroup before an element or index.
             * <p>
             * option 이나 optgroup 을 요소나 위치 앞에 추가한다.
             *
             * @param t    the options collection / options 컬렉션
             * @param args (element, before) / (element, before)
             * @return undefined / undefined
             */
            JsValue run(Object t, JsValue[] args) {
                add(bridge, ((HtmlOptionsCollection) t).getSelect(), args);
                return Js.UNDEFINED;
            }
        });
    }
}
