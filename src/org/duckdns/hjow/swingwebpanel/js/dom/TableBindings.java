package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.TableOps;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The script side of the table object model (HTML5 2014 REC section 4.9): <code>HTMLTableElement</code> (<code>caption</code>,
 * <code>tHead</code>, <code>tFoot</code>, <code>tBodies</code>, <code>rows</code>, <code>createCaption</code>, <code>deleteCaption</code>,
 * <code>createTHead</code>, <code>deleteTHead</code>, <code>createTFoot</code>, <code>deleteTFoot</code>, <code>createTBody</code>,
 * <code>insertRow</code>, <code>deleteRow</code>), <code>HTMLTableSectionElement</code> (<code>rows</code>, <code>insertRow</code>,
 * <code>deleteRow</code>), <code>HTMLTableRowElement</code> (<code>rowIndex</code>, <code>sectionRowIndex</code>, <code>cells</code>,
 * <code>insertCell</code>, <code>deleteCell</code>) and <code>HTMLTableCellElement</code> (<code>cellIndex</code>). The work is done by
 * {@link TableOps}; this class converts the arguments (an omitted index means -1) and turns a DOM exception into a script exception.
 * <p>
 * 표 객체 모델(HTML5 2014 REC 4.9절)의 스크립트 쪽: <code>HTMLTableElement</code>(<code>caption</code>, <code>tHead</code>, <code>tFoot</code>,
 * <code>tBodies</code>, <code>rows</code>, <code>createCaption</code>, <code>deleteCaption</code>, <code>createTHead</code>, <code>deleteTHead</code>,
 * <code>createTFoot</code>, <code>deleteTFoot</code>, <code>createTBody</code>, <code>insertRow</code>, <code>deleteRow</code>),
 * <code>HTMLTableSectionElement</code>(<code>rows</code>, <code>insertRow</code>, <code>deleteRow</code>), <code>HTMLTableRowElement</code>
 * (<code>rowIndex</code>, <code>sectionRowIndex</code>, <code>cells</code>, <code>insertCell</code>, <code>deleteCell</code>),
 * <code>HTMLTableCellElement</code>(<code>cellIndex</code>). 일은 {@link TableOps} 가 하며 이 클래스는 인수(생략한 인덱스는 -1)를 바꾸고 DOM 예외를 스크립트
 * 예외로 바꾼다.
 */
final class TableBindings {

    /**
     * What an accessor computes from the receiver.
     * <p>
     * 접근자가 수신자로부터 계산하는 것.
     */
    private interface Getter {
        /**
         * Computes the value.
         * <p>
         * 값을 계산한다.
         *
         * @param bridge the bridge / 브리지
         * @param e      the receiver / 수신자
         * @return the value / 값
         */
        JsValue get(DomBridge bridge, Element e);
    }

    /**
     * What a setter does with the receiver and the value.
     * <p>
     * 설정자가 수신자와 값으로 하는 일.
     */
    private interface Setter {
        /**
         * Applies the value.
         * <p>
         * 값을 적용한다.
         *
         * @param bridge the bridge / 브리지
         * @param e      the receiver / 수신자
         * @param value  the value / 값
         */
        void set(DomBridge bridge, Element e, JsValue value);
    }

    /**
     * What a method does with the receiver and the arguments.
     * <p>
     * 메서드가 수신자와 인수로 하는 일.
     */
    private interface Method {
        /**
         * Runs the method.
         * <p>
         * 메서드를 실행한다.
         *
         * @param bridge the bridge / 브리지
         * @param e      the receiver / 수신자
         * @param args   the arguments / 인수
         * @return the result / 결과
         */
        JsValue call(DomBridge bridge, Element e, JsValue[] args);
    }

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private TableBindings() {
    }

    /**
     * Returns the receiver as an element or throws the standard TypeError.
     * <p>
     * 수신자를 요소로 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element receiver(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Defines an accessor on a prototype.
     * <p>
     * 프로토타입에 접근자를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the property name / 프로퍼티 이름
     * @param getter the getter / 읽기
     * @param setter the setter or null / 쓰기. 없으면 null
     */
    private static void accessor(final DomBridge bridge, JsObject proto, String name, final Getter getter, final Setter setter) {
        JsRealm realm = bridge.getRealm();
        JsObject get = realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            /**
             * Reads the property.
             * <p>
             * 프로퍼티를 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return getter.get(bridge, receiver(self));
            }
        });
        JsObject set = setter == null ? null : realm.newFunction("set " + name, 1, new NativeFunction.Call() {
            /**
             * Writes the property.
             * <p>
             * 프로퍼티를 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                try {
                    setter.set(bridge, receiver(self), Js.arg(args, 0));
                } catch (DomException ex) {
                    throw bridge.domError(ex);
                }
                return Js.UNDEFINED;
            }
        });
        proto.defineAccessor(name, get, set, true, true);
    }

    /**
     * Defines a method on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the method name / 메서드 이름
     * @param arity  the declared number of arguments / 선언한 인수 개수
     * @param method the work / 하는 일
     */
    private static void method(final DomBridge bridge, JsObject proto, String name, int arity, final Method method) {
        proto.defineBuiltin(name, bridge.getRealm().newFunction(name, arity, new NativeFunction.Call() {
            /**
             * Runs the method and turns a DOM exception into a script exception.
             * <p>
             * 메서드를 실행하고 DOM 예외를 스크립트 예외로 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the result / 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                try {
                    return method.call(bridge, receiver(self), args);
                } catch (DomException ex) {
                    throw bridge.domError(ex);
                }
            }
        }));
    }

    /**
     * Returns the index argument of <code>insertRow</code> and the like: an omitted or undefined argument is -1.
     * <p>
     * <code>insertRow</code> 등의 인덱스 인수를 반환한다. 생략했거나 undefined 이면 -1.
     *
     * @param args the arguments / 인수
     * @return the index / 인덱스
     */
    private static int index(JsValue[] args) {
        return args.length == 0 || args[0].isUndefined() ? -1 : Ops.toInt32(args[0]);
    }

    /**
     * Returns the index argument that is required (<code>deleteRow</code>, <code>deleteCell</code>).
     * <p>
     * 필수 인덱스 인수를 반환한다(<code>deleteRow</code>, <code>deleteCell</code>).
     *
     * @param args the arguments / 인수
     * @param name the method name for the message / 메시지용 메서드 이름
     * @return the index / 인덱스
     */
    private static int requiredIndex(JsValue[] args, String name) {
        if (args.length == 0) {
            throw JsException.typeError("Failed to execute '" + name + "': 1 argument required, but only 0 present.");
        }
        return Ops.toInt32(args[0]);
    }

    /**
     * Installs the members. The interfaces must exist in the bridge.
     * <p>
     * 멤버를 설치한다. 인터페이스가 브리지에 있어야 한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void install(final DomBridge bridge) {
        JsObject table = bridge.getInterface("HTMLTableElement").getPrototype();
        accessor(bridge, table, "caption", new Getter() {
            /**
             * Returns the caption.
             * <p>
             * caption 을 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the table / 표
             * @return the caption or null / caption 또는 null
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.caption(e));
            }
        }, new Setter() {
            /**
             * Replaces the caption.
             * <p>
             * caption 을 바꾼다.
             *
             * @param b     the bridge / 브리지
             * @param e     the table / 표
             * @param value the new caption or null / 새 caption 또는 null
             */
            public void set(DomBridge b, Element e, JsValue value) {
                TableOps.setCaption(e, elementOrNull(value));
            }
        });
        accessor(bridge, table, "tHead", new Getter() {
            /**
             * Returns the head.
             * <p>
             * 머리를 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the table / 표
             * @return the thead or null / thead 또는 null
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.tHead(e));
            }
        }, new Setter() {
            /**
             * Replaces the head.
             * <p>
             * 머리를 바꾼다.
             *
             * @param b     the bridge / 브리지
             * @param e     the table / 표
             * @param value the new head or null / 새 머리 또는 null
             */
            public void set(DomBridge b, Element e, JsValue value) {
                TableOps.setTHead(e, elementOrNull(value));
            }
        });
        accessor(bridge, table, "tFoot", new Getter() {
            /**
             * Returns the foot.
             * <p>
             * 바닥을 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the table / 표
             * @return the tfoot or null / tfoot 또는 null
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.tFoot(e));
            }
        }, new Setter() {
            /**
             * Replaces the foot.
             * <p>
             * 바닥을 바꾼다.
             *
             * @param b     the bridge / 브리지
             * @param e     the table / 표
             * @param value the new foot or null / 새 바닥 또는 null
             */
            public void set(DomBridge b, Element e, JsValue value) {
                TableOps.setTFoot(e, elementOrNull(value));
            }
        });
        accessor(bridge, table, "tBodies", new Getter() {
            /**
             * Returns the live collection of the bodies.
             * <p>
             * 본문의 라이브 컬렉션을 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the table / 표
             * @return the collection / 컬렉션
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.tBodies(e));
            }
        }, null);
        accessor(bridge, table, "rows", new Getter() {
            /**
             * Returns the live collection of the rows.
             * <p>
             * 행의 라이브 컬렉션을 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the table / 표
             * @return the collection / 컬렉션
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.rows(e));
            }
        }, null);
        method(bridge, table, "createCaption", 0, new Method() {
            /**
             * Creates the caption.
             * <p>
             * caption 을 만든다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return the caption / caption
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.createCaption(e));
            }
        });
        method(bridge, table, "deleteCaption", 0, new Method() {
            /**
             * Deletes the caption.
             * <p>
             * caption 을 지운다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                TableOps.deleteCaption(e);
                return Js.UNDEFINED;
            }
        });
        method(bridge, table, "createTHead", 0, new Method() {
            /**
             * Creates the head.
             * <p>
             * 머리를 만든다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return the thead / thead
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.createTHead(e));
            }
        });
        method(bridge, table, "deleteTHead", 0, new Method() {
            /**
             * Deletes the head.
             * <p>
             * 머리를 지운다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                TableOps.deleteTHead(e);
                return Js.UNDEFINED;
            }
        });
        method(bridge, table, "createTFoot", 0, new Method() {
            /**
             * Creates the foot.
             * <p>
             * 바닥을 만든다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return the tfoot / tfoot
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.createTFoot(e));
            }
        });
        method(bridge, table, "deleteTFoot", 0, new Method() {
            /**
             * Deletes the foot.
             * <p>
             * 바닥을 지운다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                TableOps.deleteTFoot(e);
                return Js.UNDEFINED;
            }
        });
        method(bridge, table, "createTBody", 0, new Method() {
            /**
             * Creates a body.
             * <p>
             * 본문을 만든다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return the tbody / tbody
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.createTBody(e));
            }
        });
        method(bridge, table, "insertRow", 0, new Method() {
            /**
             * Inserts a row.
             * <p>
             * 행을 넣는다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return the row / 행
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.insertRow(e, index(args)));
            }
        });
        method(bridge, table, "deleteRow", 1, new Method() {
            /**
             * Deletes a row.
             * <p>
             * 행을 지운다.
             *
             * @param b    the bridge / 브리지
             * @param e    the table / 표
             * @param args the arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                TableOps.deleteRow(e, requiredIndex(args, "deleteRow"));
                return Js.UNDEFINED;
            }
        });
        JsObject section = bridge.getInterface("HTMLTableSectionElement").getPrototype();
        accessor(bridge, section, "rows", new Getter() {
            /**
             * Returns the live collection of the rows of the section.
             * <p>
             * 구역의 행의 라이브 컬렉션을 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the section / 구역
             * @return the collection / 컬렉션
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.sectionRows(e));
            }
        }, null);
        method(bridge, section, "insertRow", 0, new Method() {
            /**
             * Inserts a row into the section.
             * <p>
             * 구역에 행을 넣는다.
             *
             * @param b    the bridge / 브리지
             * @param e    the section / 구역
             * @param args the arguments / 인수
             * @return the row / 행
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.insertSectionRow(e, index(args)));
            }
        });
        method(bridge, section, "deleteRow", 1, new Method() {
            /**
             * Deletes a row of the section.
             * <p>
             * 구역의 행을 지운다.
             *
             * @param b    the bridge / 브리지
             * @param e    the section / 구역
             * @param args the arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                TableOps.deleteSectionRow(e, requiredIndex(args, "deleteRow"));
                return Js.UNDEFINED;
            }
        });
        JsObject row = bridge.getInterface("HTMLTableRowElement").getPrototype();
        accessor(bridge, row, "rowIndex", new Getter() {
            /**
             * Returns the position in the rows of the table.
             * <p>
             * 표의 행 목록에서의 위치를 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the row / 행
             * @return the index / 인덱스
             */
            public JsValue get(DomBridge b, Element e) {
                return Js.num(TableOps.rowIndex(e));
            }
        }, null);
        accessor(bridge, row, "sectionRowIndex", new Getter() {
            /**
             * Returns the position in the rows of the parent.
             * <p>
             * 부모의 행 목록에서의 위치를 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the row / 행
             * @return the index / 인덱스
             */
            public JsValue get(DomBridge b, Element e) {
                return Js.num(TableOps.sectionRowIndex(e));
            }
        }, null);
        accessor(bridge, row, "cells", new Getter() {
            /**
             * Returns the live collection of the cells.
             * <p>
             * 셀의 라이브 컬렉션을 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the row / 행
             * @return the collection / 컬렉션
             */
            public JsValue get(DomBridge b, Element e) {
                return b.toJs(TableOps.cells(e));
            }
        }, null);
        method(bridge, row, "insertCell", 0, new Method() {
            /**
             * Inserts a cell.
             * <p>
             * 셀을 넣는다.
             *
             * @param b    the bridge / 브리지
             * @param e    the row / 행
             * @param args the arguments / 인수
             * @return the cell / 셀
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                return b.toJs(TableOps.insertCell(e, index(args)));
            }
        });
        method(bridge, row, "deleteCell", 1, new Method() {
            /**
             * Deletes a cell.
             * <p>
             * 셀을 지운다.
             *
             * @param b    the bridge / 브리지
             * @param e    the row / 행
             * @param args the arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(DomBridge b, Element e, JsValue[] args) {
                TableOps.deleteCell(e, requiredIndex(args, "deleteCell"));
                return Js.UNDEFINED;
            }
        });
        JsObject cell = bridge.getInterface("HTMLTableCellElement").getPrototype();
        accessor(bridge, cell, "cellIndex", new Getter() {
            /**
             * Returns the position in the cells of the row.
             * <p>
             * 행의 셀 목록에서의 위치를 반환한다.
             *
             * @param b the bridge / 브리지
             * @param e the cell / 셀
             * @return the index / 인덱스
             */
            public JsValue get(DomBridge b, Element e) {
                return Js.num(TableOps.cellIndex(e));
            }
        }, null);
    }

    /**
     * Converts a script value to an element, or null for <code>null</code> (anything else is a TypeError).
     * <p>
     * 스크립트 값을 요소로 바꾼다. <code>null</code> 은 null, 그 밖은 TypeError.
     *
     * @param value the value / 값
     * @return the element or null / 요소 또는 null
     */
    private static Element elementOrNull(JsValue value) {
        if (value.isNullOrUndefined()) {
            return null;
        }
        Element e = (Element) DomBridge.targetOf(value, Element.class);
        if (e == null) {
            throw JsException.typeError("The provided value is not of type 'HTMLElement'.");
        }
        return e;
    }
}
