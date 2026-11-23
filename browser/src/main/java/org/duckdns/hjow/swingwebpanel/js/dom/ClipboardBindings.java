package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The JavaScript interfaces of the clipboard events (plan item P11-08): <code>ClipboardEvent</code> (<code>clipboardData</code>, the events
 * <code>copy</code>, <code>cut</code> and <code>paste</code>) and the clipboard part of <code>DataTransfer</code> (<code>getData</code>,
 * <code>setData</code>, <code>clearData</code>, <code>types</code>). The component fires <code>copy</code> when the user copies the
 * selection; a listener that calls <code>setData</code> and <code>preventDefault</code> puts its own data on the clipboard. Files, items and
 * the drag and drop members of <code>DataTransfer</code> are not part of this interface (drag and drop is plan item P20-06).
 * <p>
 * 클립보드 이벤트의 JavaScript 인터페이스(계획 P11-08): <code>ClipboardEvent</code>(<code>clipboardData</code>, <code>copy</code>, <code>cut</code>,
 * <code>paste</code> 이벤트)와 <code>DataTransfer</code> 의 클립보드 부분(<code>getData</code>, <code>setData</code>, <code>clearData</code>,
 * <code>types</code>). 컴포넌트는 사용자가 선택을 복사할 때 <code>copy</code> 를 보내며, <code>setData</code> 와 <code>preventDefault</code> 를 부르는
 * 리스너는 자기 데이터를 클립보드에 넣는다. 파일, 항목, <code>DataTransfer</code> 의 끌어다 놓기 멤버는 이 인터페이스에 없다(끌어다 놓기는 계획 P20-06).
 */
public final class ClipboardBindings {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ClipboardBindings() {
    }

    /**
     * Installs <code>DataTransfer</code> and <code>ClipboardEvent</code> (call after the Event interface exists).
     * <p>
     * <code>DataTransfer</code> 와 <code>ClipboardEvent</code> 를 설치한다(Event 인터페이스가 생긴 뒤에 호출한다).
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        final JsObject eventCtor = (JsObject) realm.getGlobalObject().get("Event");
        final DomBridge.InterfaceInfo transfer = bridge.defineInterface("DataTransfer", null, ClipboardData.class,
                new String[] { "a:dropEffect=", "a:effectAllowed=", "a:items", "a:files", "a:types", "m:getData(String)", "m:setData(String,String)",
                    "m:clearData(String*)", "m:setDragImage(Element,int,int)" });
        installItems(bridge);
        bridge.setConstructor("DataTransfer", new NativeFunction(realm, "DataTransfer", 0, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'DataTransfer': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates empty clipboard data.
             * <p>
             * 빈 클립보드 데이터를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the data wrapper / 데이터 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(new ClipboardData());
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, transfer.getPrototype()));
                return w;
            }
        }));
        final DomBridge.InterfaceInfo clipboard = bridge.defineInterface("ClipboardEvent", "Event", ClipboardEvent.class,
                new String[] { "a:clipboardData" });
        bridge.setConstructor("ClipboardEvent", new NativeFunction(realm, eventCtor, "ClipboardEvent", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'ClipboardEvent': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a clipboard event with the <code>clipboardData</code> of the init dictionary (new empty data if there is none).
             * <p>
             * 초기화 사전의 <code>clipboardData</code> 로 클립보드 이벤트를 만든다(없으면 새 빈 데이터).
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'ClipboardEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                ClipboardData data = opts.isObject() ? (ClipboardData) DomBridge.targetOf(((JsObject) opts).get("clipboardData"), ClipboardData.class) : null;
                ClipboardEvent e = new ClipboardEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"),
                        data == null ? new ClipboardData() : data);
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, clipboard.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Installs <code>DataTransferItemList</code> (<code>length</code>, indexed access, <code>add</code>, <code>remove</code>, <code>clear</code>) and
     * <code>DataTransferItem</code> (<code>kind</code>, <code>type</code>, <code>getAsString</code>, <code>getAsFile</code>). The callback of
     * <code>getAsString</code> runs as a job of the realm (the closest thing this realm has to a queued task, after the current script).
     * <p>
     * <code>DataTransferItemList</code>(<code>length</code>, 위치 접근, <code>add</code>, <code>remove</code>, <code>clear</code>)와 <code>DataTransferItem</code>
     * (<code>kind</code>, <code>type</code>, <code>getAsString</code>, <code>getAsFile</code>)을 설치한다. <code>getAsString</code> 의 콜백은 realm 의 작업으로
     * 실행된다(현재 스크립트가 끝난 뒤. 이 realm 에서 대기열 작업에 가장 가까운 것).
     *
     * @param bridge the bridge / 브리지
     */
    private static void installItems(final DomBridge bridge) {
        bridge.defineInterface("DataTransferItemList", null, org.duckdns.hjow.swingwebpanel.dom.DataTransferItemList.class,
                new String[] { "a:length", "m:add(String,String)", "m:remove(int)", "m:clear()" });
        bridge.setIndexed("DataTransferItemList", new IndexedObject.Source() {
            /**
             * Returns the number of items.
             * <p>
             * 항목 수를 반환한다.
             *
             * @param t the list / 목록
             * @return the length / 길이
             */
            public int length(Object t) {
                return ((org.duckdns.hjow.swingwebpanel.dom.DataTransferItemList) t).getLength();
            }

            /**
             * Returns the item at an index.
             * <p>
             * 위치의 항목을 반환한다.
             *
             * @param t the list / 목록
             * @param i the index / 위치
             * @return the item / 항목
             */
            public Object item(Object t, int i) {
                return ((org.duckdns.hjow.swingwebpanel.dom.DataTransferItemList) t).item(i);
            }

            /**
             * The list has no named access.
             * <p>
             * 목록에는 이름 접근이 없다.
             *
             * @param t    the list / 목록
             * @param name the name / 이름
             * @return null / null
             */
            public Object named(Object t, String name) {
                return null;
            }
        });
        DomBridge.InterfaceInfo item = bridge.defineInterface("DataTransferItem", null, org.duckdns.hjow.swingwebpanel.dom.DataTransferItem.class,
                new String[] { "a:kind", "a:type", "m:getAsFile()" });
        item.getPrototype().defineBuiltin("getAsString", bridge.getRealm().newFunction("getAsString", 1, new NativeFunction.Call() {
            /**
             * Passes the string of the item to the callback after the current script (nothing happens for a null callback or when the item
             * cannot be read now).
             * <p>
             * 현재 스크립트가 끝난 뒤 콜백에 항목의 문자열을 준다(콜백이 null 이거나 항목을 지금 읽을 수 없으면 아무 일도 없다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                org.duckdns.hjow.swingwebpanel.dom.DataTransferItem target =
                        (org.duckdns.hjow.swingwebpanel.dom.DataTransferItem) DomBridge.targetOf(self, org.duckdns.hjow.swingwebpanel.dom.DataTransferItem.class);
                if (target == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                final JsValue callback = Js.arg(args, 0);
                if (callback.isNull() || callback.isUndefined()) {
                    return Js.UNDEFINED;
                }
                if (!callback.isCallable()) {
                    throw JsException.typeError("Failed to execute 'getAsString' on 'DataTransferItem': parameter 1 is not of type 'Function'.");
                }
                final String data = target.getAsString();
                // 읽을 수 없는 항목의 콜백은 부르지 않는다.
                if (data != null) {
                    r.enqueueJob(new Runnable() {
                        /** Calls the callback with the string. / 콜백을 문자열로 부른다. */
                        public void run() {
                            Ops.call(callback, Js.UNDEFINED, new JsValue[] { Js.str(data) });
                        }
                    });
                }
                return Js.UNDEFINED;
            }
        }));
    }

    /**
     * Reads a boolean member of an init dictionary.
     * <p>
     * 초기화 사전의 불리언 멤버를 읽는다.
     *
     * @param options the dictionary / 사전
     * @param name    the member / 멤버
     * @return the flag / 플래그
     */
    private static boolean flag(JsValue options, String name) {
        return options.isObject() && Ops.toBoolean(((JsObject) options).get(name));
    }
}
