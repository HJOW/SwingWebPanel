package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsErrorObject;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsStringObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.builtins.datajson.DataAccess;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.DataViewObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.SharedBufferReference;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;

/**
 * The structured clone algorithm of the HTML Standard (<code>StructuredSerialize</code> and <code>StructuredDeserialize</code>, section
 * "Safe passing of structured data"): a script value of one realm is turned into a realm independent, immutable Java tree
 * ({@link ClonedValue}), and that tree is turned into a fresh value of the same or another realm. It copies primitives (also -0 and NaN),
 * Boolean, Number and String wrapper objects, Date, RegExp (<code>lastIndex</code> becomes 0), Array (holes and length), ordinary objects (own
 * enumerable string keyed properties; getters are read), Map, Set, ArrayBuffer, the typed arrays, DataView and
 * Error objects (name, message and stack), and it keeps cycles and shared references. Functions, symbols, DOM nodes and other host objects,
 * Proxy objects, WeakMap, Promise and so on cannot be cloned and raise a {@link StructuredCloneException}. A depth limit, a cell limit and a
 * byte limit protect the process; the walk is iterative, so a deep structure never overflows the Java stack. ArrayBuffers in the transfer
 * list are moved instead of copied (the source is detached). The messaging entry points preserve shared buffers within an agent cluster.
 * <p>
 * Usage: <code>ClonedValue c = StructuredClone.serialize(realm, value); JsValue copy = StructuredClone.deserialize(otherRealm, c);</code>
 * <p>
 * HTML 표준의 구조화 복제 알고리즘("Safe passing of structured data" 절의 <code>StructuredSerialize</code>, <code>StructuredDeserialize</code>):
 * 한 realm 의 스크립트 값을 realm 에 독립적인 불변 Java 트리({@link ClonedValue})로 바꾸고, 그 트리를 같은 또는 다른 realm 의 새 값으로 바꾼다.
 * 원시 값(-0 과 NaN 포함), Boolean/Number/String 래퍼 객체, Date, RegExp(<code>lastIndex</code> 는 0), Array(빈 칸과 length), 일반 객체(자체
 * 열거 가능한 문자열 키 프로퍼티, getter 는 값을 읽음), Map, Set, ArrayBuffer, 형식화 배열, DataView, Error 객체(name,
 * message, stack)를 복사하며 순환과 공유 참조를 유지한다. 함수, 심볼, DOM 노드 등 호스트 객체, Proxy, WeakMap, Promise 등은 복제할 수 없어
 * {@link StructuredCloneException} 을 던진다. 깊이, 칸 수, 바이트 한도가 프로세스를 보호하며 탐색은 반복문이므로 깊은 구조도 Java 스택을 넘치게 하지
 * 않는다. 전달 목록의 ArrayBuffer 는 복사하지 않고 옮긴다(원본은 분리된다). 메시징 진입점은 같은 에이전트 클러스터에서 버퍼 공유를 유지한다.
 * <p>
 * 사용법: <code>ClonedValue c = StructuredClone.serialize(realm, value); JsValue copy = StructuredClone.deserialize(otherRealm, c);</code>
 */
public final class StructuredClone {

    /**
     * The deepest nesting of objects that is serialized.
     * <p>
     * 직렬화하는 객체 중첩의 최대 깊이.
     */
    public static final int MAX_DEPTH = 10000;

    /**
     * The largest number of objects of one serialized value.
     * <p>
     * 직렬화한 값 하나의 최대 객체 수.
     */
    public static final int MAX_CELLS = 4000000;

    /**
     * The default limit of the estimated size of a serialized value in bytes.
     * <p>
     * 직렬화한 값의 추정 크기에 대한 기본 한도(바이트).
     */
    public static final long DEFAULT_MAX_BYTES = 64L * 1024L * 1024L;

    /**
     * The marker of the value undefined in a serialized tree.
     * <p>
     * 직렬화된 트리에서 undefined 값의 표식.
     */
    static final Object UNDEFINED = new Object();

    /**
     * The marker of the value null in a serialized tree.
     * <p>
     * 직렬화된 트리에서 null 값의 표식.
     */
    static final Object NULL = new Object();

    /**
     * Cell kind: Boolean wrapper object.
     * <p>
     * 칸 종류: Boolean 래퍼 객체.
     */
    static final int BOOLEAN_OBJECT = 1;

    /**
     * Cell kind: Number wrapper object.
     * <p>
     * 칸 종류: Number 래퍼 객체.
     */
    static final int NUMBER_OBJECT = 2;

    /**
     * Cell kind: String wrapper object.
     * <p>
     * 칸 종류: String 래퍼 객체.
     */
    static final int STRING_OBJECT = 3;

    /**
     * Cell kind: Date.
     * <p>
     * 칸 종류: Date.
     */
    static final int DATE = 4;

    /**
     * Cell kind: RegExp.
     * <p>
     * 칸 종류: RegExp.
     */
    static final int REGEXP = 5;

    /**
     * Cell kind: Array.
     * <p>
     * 칸 종류: Array.
     */
    static final int ARRAY = 6;

    /**
     * Cell kind: ordinary object.
     * <p>
     * 칸 종류: 일반 객체.
     */
    static final int OBJECT = 7;

    /**
     * Cell kind: Map.
     * <p>
     * 칸 종류: Map.
     */
    static final int MAP = 8;

    /**
     * Cell kind: Set.
     * <p>
     * 칸 종류: Set.
     */
    static final int SET = 9;

    /**
     * Cell kind: ArrayBuffer.
     * <p>
     * 칸 종류: ArrayBuffer.
     */
    static final int BUFFER = 10;

    /**
     * Cell kind: typed array.
     * <p>
     * 칸 종류: 형식화 배열.
     */
    static final int TYPED = 11;

    /**
     * Cell kind: DataView.
     * <p>
     * 칸 종류: DataView.
     */
    static final int VIEW = 12;

    /**
     * Cell kind: Error object.
     * <p>
     * 칸 종류: Error 객체.
     */
    static final int ERROR = 13;

    /**
     * Cell kind: an object that the transfer handler moved (a MessagePort); the cell holds its opaque payload.
     * <p>
     * 칸 종류: 전달 처리기가 옮긴 객체(MessagePort). 칸이 불투명한 내용물을 가진다.
     */
    static final int TRANSFERRED = 14;

    /**
     * Cell kind: BigInt wrapper object.
     * <p>
     * 칸 종류: BigInt 래퍼 객체.
     */
    static final int BIGINT_OBJECT = 15;

    /** Cell kind for a SharedArrayBuffer reference. / SharedArrayBuffer 참조를 담는 칸의 종류. */
    static final int SHARED_BUFFER = 16;

    /** Cell kind holding the serialized DOMException name and message. / DOMException 이름과 메시지를 담는 칸의 종류. */
    static final int DOM_EXCEPTION = 17;

    /**
     * The hook through which objects other than ArrayBuffers (for example a <code>MessagePort</code>) are put in a transfer list. The
     * serializer calls {@link #prepare(JsObject)} for every such entry without changing anything, then {@link #commit(JsObject, Object)} once
     * the whole serialization succeeded (that is where the source gives up the object), and a receiver turns the payload into an object of its
     * own realm with {@link #receive(JsRealm, Object)}. A payload is moved, so it can be received once.
     * <p>
     * ArrayBuffer 가 아닌 객체(예: <code>MessagePort</code>)를 전달 목록에 넣기 위한 훅. 직렬화기는 그런 항목마다 아무것도 바꾸지 않고
     * {@link #prepare(JsObject)} 를 부르고, 직렬화 전체가 성공하면 {@link #commit(JsObject, Object)} 를 한 번씩 부르며(여기서 원본이 객체를 내놓는다),
     * 받는 쪽은 {@link #receive(JsRealm, Object)} 로 내용물을 자기 realm 의 객체로 바꾼다. 내용물은 옮겨지므로 한 번만 받을 수 있다.
     */
    public interface TransferHandler {
        /**
         * Checks an object of the transfer list and returns its payload; nothing may change here.
         * <p>
         * 전달 목록의 객체를 검사하고 내용물을 반환한다. 여기서는 아무것도 바꾸면 안 된다.
         *
         * @param object the object of the transfer list / 전달 목록의 객체
         * @return the payload, or null if the object cannot be transferred / 내용물. 전달할 수 없는 객체이면 null
         * @throws StructuredCloneException if the object is in a state that cannot be transferred / 전달할 수 없는 상태이면
         */
        Object prepare(JsObject object);

        /**
         * Called once for every prepared object after the whole serialization succeeded: the source object is given up.
         * <p>
         * 직렬화 전체가 성공한 뒤 준비된 객체마다 한 번 불린다: 원본 객체를 내놓는다.
         *
         * @param object  the source object / 원본 객체
         * @param payload the payload that {@link #prepare(JsObject)} returned / {@link #prepare(JsObject)} 가 반환한 내용물
         */
        void commit(JsObject object, Object payload);

        /**
         * Creates the object of a payload in the realm of the receiver.
         * <p>
         * 내용물로 받는 쪽 realm 의 객체를 만든다.
         *
         * @param realm   the realm of the receiver / 받는 쪽 realm
         * @param payload the payload / 내용물
         * @return the new object / 새 객체
         */
        JsObject receive(JsRealm realm, Object payload);
    }

    /**
     * One serialized object. Only {@link StructuredClone} reads and writes its fields; once a {@link ClonedValue} is returned nothing changes
     * them any more.
     * <p>
     * 직렬화된 객체 하나. {@link StructuredClone} 만 필드를 읽고 쓰며, {@link ClonedValue} 가 반환된 뒤에는 아무도 바꾸지 않는다.
     */
    static final class Cell {
        /**
         * The kind of the object (one of the kind constants).
         * <p>
         * 객체의 종류(종류 상수 중 하나).
         */
        final int kind;

        /**
         * The number of a Number wrapper or the time value of a Date.
         * <p>
         * Number 래퍼의 수 또는 Date 의 시간 값.
         */
        double number;

        /**
         * The value of a Boolean wrapper.
         * <p>
         * Boolean 래퍼의 값.
         */
        boolean flag;

        /**
         * The text of a String wrapper, the source of a RegExp, the constructor name of a typed array, or the name of an error.
         * <p>
         * String 래퍼의 문자열, RegExp 의 원문, 형식화 배열의 생성자 이름, 오류의 이름.
         */
        String text;

        /**
         * The flags of a RegExp or the message of an error (null if absent).
         * <p>
         * RegExp 의 플래그 또는 오류의 메시지(없으면 null).
         */
        String text2;

        /**
         * The stack text of an error (null if absent).
         * <p>
         * 오류의 stack 문자열(없으면 null).
         */
        String text3;

        /** The serialized own cause of an error, or Java null if absent. / 오류의 자체 cause를 직렬화한 값. 없으면 Java null. */
        Object cause;

        /** The serialized own errors of an AggregateError, or Java null if absent. / AggregateError의 자체 errors를 직렬화한 값. 없으면 Java null. */
        Object errors;

        /**
         * The bytes of an ArrayBuffer.
         * <p>
         * ArrayBuffer 의 바이트.
         */
        byte[] bytes;

        /** The resizable limit of a cloned buffer, or -1 for a fixed buffer. / 복제한 가변 버퍼의 상한. 고정 버퍼이면 -1. */
        int maxByteLength = -1;

        /**
         * The payload of a transferred object or a shared buffer reference.
         * <p>
         * 옮긴 객체의 내용물 또는 공유 버퍼 참조.
         */
        Object payload;

        /**
         * The place of a transferred object in the transfer list (among the objects of the handler).
         * <p>
         * 옮긴 객체가 (처리기의 객체들 가운데) 전달 목록에서 차지하는 순서.
         */
        int ordinal;

        /**
         * The cell number of the buffer of a typed array or DataView.
         * <p>
         * 형식화 배열 또는 DataView 의 버퍼 칸 번호.
         */
        int bufferRef;

        /**
         * The byte offset of a typed array or DataView.
         * <p>
         * 형식화 배열 또는 DataView 의 바이트 위치.
         */
        int offset;

        /**
         * The element count of a typed array or the byte length of a DataView, or -1 for length tracking.
         * <p>
         * 형식화 배열의 요소 수 또는 DataView의 바이트 길이. 길이를 추적하면 -1.
         */
        int length;

        /**
         * The length of an Array.
         * <p>
         * 배열(Array)의 length 값.
         */
        long arrayLength;

        /**
         * The keys of an Array or object (Strings, parallel to the values), or null.
         * <p>
         * Array 또는 객체의 키(String, 값과 나란히), 없으면 null.
         */
        ArrayList<Object> keys;

        /**
         * The values of an Array or object, the keys and values of a Map (alternating) or the values of a Set, or null.
         * <p>
         * Array 또는 객체의 값, Map 의 키와 값(번갈아), Set 의 값, 없으면 null.
         */
        ArrayList<Object> values;

        /**
         * Creates a cell.
         * <p>
         * 칸을 만든다.
         *
         * @param kind the kind / 종류
         */
        Cell(int kind) {
            this.kind = kind;
            if (kind == ARRAY || kind == OBJECT) {
                keys = new ArrayList<Object>();
            }
            if (kind == ARRAY || kind == OBJECT || kind == MAP || kind == SET) {
                values = new ArrayList<Object>();
            }
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private StructuredClone() {
    }

    /**
     * Serializes a value with the default size limit and no transfer list.
     * <p>
     * 기본 크기 한도로, 전달 목록 없이 값을 직렬화한다.
     *
     * @param realm the realm that the value belongs to / 값이 속한 realm
     * @param value the value / 값
     * @return the serialized form / 직렬화된 형태
     * @throws StructuredCloneException if the value cannot be cloned / 값을 복제할 수 없으면
     */
    public static ClonedValue serialize(JsRealm realm, JsValue value) {
        return serialize(realm, value, DEFAULT_MAX_BYTES, null);
    }

    /**
     * Serializes a value. Getters of the value run script code, so the call must be made on the thread that owns the realm. When the
     * transfer list holds ArrayBuffers, their bytes move into the result and the source buffers are detached after a successful
     * serialization.
     * <p>
     * 값을 직렬화한다. 값의 getter 는 스크립트 코드를 실행하므로 realm 을 소유한 스레드에서 불러야 한다. 전달 목록에 ArrayBuffer 가 있으면 그
     * 바이트는 결과로 옮겨지고 직렬화에 성공한 뒤 원본 버퍼는 분리된다.
     *
     * @param realm    the realm that the value belongs to / 값이 속한 realm
     * @param value    the value / 값
     * @param maxBytes the limit of the estimated size / 추정 크기의 한도
     * @param transfer ArrayBuffers to move, or null / 옮길 ArrayBuffer. 없으면 null
     * @return the serialized form / 직렬화된 형태
     * @throws StructuredCloneException if the value cannot be cloned or a transfer is invalid / 값을 복제할 수 없거나 전달이 잘못되었으면
     */
    public static ClonedValue serialize(JsRealm realm, JsValue value, long maxBytes, List<? extends JsObject> transfer) {
        return serialize(realm, value, maxBytes, transfer, null);
    }

    /**
     * Serializes a value like {@link #serialize(JsRealm, JsValue, long, List)}, and lets a handler take the objects of the transfer list that
     * are not ArrayBuffers (a <code>MessagePort</code>): they become cells of the result, and the handler gives them up after the whole
     * serialization succeeded.
     * <p>
     * {@link #serialize(JsRealm, JsValue, long, List)} 처럼 값을 직렬화하되, 전달 목록의 ArrayBuffer 가 아닌 객체(<code>MessagePort</code>)는 처리기가
     * 받는다: 그것들은 결과의 칸이 되고, 직렬화 전체가 성공한 뒤 처리기가 내놓는다.
     *
     * @param realm    the realm that the value belongs to / 값이 속한 realm
     * @param value    the value / 값
     * @param maxBytes the limit of the estimated size / 추정 크기의 한도
     * @param transfer the objects to move, or null / 옮길 객체. 없으면 null
     * @param handler  the handler of the objects that are not ArrayBuffers, or null for none / ArrayBuffer 가 아닌 객체의 처리기. 없으면 null
     * @return the serialized form / 직렬화된 형태
     * @throws StructuredCloneException if the value cannot be cloned or a transfer is invalid / 값을 복제할 수 없거나 전달이 잘못되었으면
     */
    public static ClonedValue serialize(JsRealm realm, JsValue value, long maxBytes, List<? extends JsObject> transfer, TransferHandler handler) {
        return serialize(value, maxBytes, transfer, handler, null);
    }

    /**
     * Serializes a browser message or a JavaScript structuredClone call, allowing shared memory in a bound realm.
     * <p>
     * 브라우저 메시지나 JavaScript structuredClone 호출을 직렬화한다. 호스트가 연결한 realm에서는 공유 메모리를 허용한다.
     *
     * @param realm the sending realm / 보내는 realm
     * @param value the value to clone / 복제할 값
     * @param maxBytes the size limit / 크기 한도
     * @param transfer the objects to move, or null / 옮길 객체. 없으면 null
     * @param handler the handler for external transferable objects, or null / 외부 전달 객체의 처리기. 없으면 null
     * @return the serialized message / 직렬화된 메시지
     */
    public static ClonedValue serializeForMessaging(JsRealm realm, JsValue value, long maxBytes, List<? extends JsObject> transfer,
            TransferHandler handler) {
        return serialize(value, maxBytes, transfer, handler, SharedMemoryScope.of(realm));
    }

    /**
     * Builds the serialized graph after validating the transfer list.
     * <p>
     * 전달 목록을 검사한 뒤 직렬화 그래프를 만든다.
     *
     * @param value value to serialize / 직렬화할 값
     * @param maxBytes the size limit / 크기 한도
     * @param transfer the objects to move, or null / 옮길 객체. 없으면 null
     * @param handler external transfer handler, or null / 외부 전달 처리기. 없으면 null
     * @param sharedScope scope that permits sharing, or null to disallow sharing / 공유를 허용하는 범위. 공유 금지이면 null
     * @return serialized graph / 직렬화 그래프
     */
    private static ClonedValue serialize(JsValue value, long maxBytes, List<? extends JsObject> transfer, TransferHandler handler,
            SharedMemoryScope sharedScope) {
        Serializer s = new Serializer(maxBytes, transfer, handler, sharedScope);
        Object root = s.value(value);
        s.run();
        s.prepareTransfers();
        s.commitTransfers();
        return new ClonedValue(root, s.cells.toArray(new Cell[s.cells.size()]), s.used, s.containsShared ? sharedScope : null);
    }

    /**
     * Serializes and converts a page message before committing any buffer transfer.
     * <p>
     * 페이지 메시지를 직렬화해 Java 값으로 바꾼 뒤 버퍼 전달을 확정한다.
     *
     * @param value sending script value / 발신 스크립트 값
     * @param transfer buffers to move / 옮길 버퍼
     * @return copied host data / 복사한 호스트 데이터
     */
    static HostMessageValues.JavaMessage serializeToHost(JsValue value, List<? extends JsObject> transfer) {
        Serializer s = new Serializer(128L * HostMessageValues.MAX_MESSAGE_UNITS, transfer, null, null);
        Object root = s.value(value);
        s.run();
        s.prepareTransfers();
        HostMessageValues.JavaMessage data = HostMessageReader.read(root, s.cells, s.used);
        s.commitTransfers();
        return data;
    }

    /**
     * Finds the element type named in a serialized typed array.
     * <p>
     * 직렬화한 형식화 배열의 이름에 해당하는 원소 타입을 찾는다.
     *
     * @param name typed array constructor name / 형식화 배열 생성자 이름
     * @return element type / 원소 타입
     */
    static ElementType elementType(String name) {
        // 생성자 이름이 같은 형식을 고른다.
        for (ElementType type : ElementType.values()) {
            if (type.getConstructorName().equals(name)) {
                return type;
            }
        }
        throw new IllegalStateException("unknown typed array " + name);
    }

    /**
     * Creates a fresh value of a realm from a serialized form. It can be called any number of times for the same form; every call makes
     * independent objects.
     * <p>
     * 직렬화된 형태로 realm 의 새 값을 만든다. 같은 형태로 몇 번이고 부를 수 있으며 부를 때마다 독립된 객체를 만든다.
     *
     * @param realm  the realm that receives the value / 값을 받는 realm
     * @param cloned the serialized form / 직렬화된 형태
     * @return the new value / 새 값
     */
    public static JsValue deserialize(JsRealm realm, ClonedValue cloned) {
        return deserialize(realm, cloned, null, null);
    }

    /**
     * Creates a fresh value like {@link #deserialize(JsRealm, ClonedValue)}, and also receives the objects that a handler moved (a
     * <code>MessagePort</code>): they are created by the handler in the realm of the receiver and collected in the order of the transfer list.
     * A form that carries moved objects can be received once.
     * <p>
     * {@link #deserialize(JsRealm, ClonedValue)} 처럼 새 값을 만들되, 처리기가 옮긴 객체(<code>MessagePort</code>)도 받는다: 그것들은 처리기가 받는 쪽
     * realm 에 만들며 전달 목록의 순서대로 모아 준다. 옮긴 객체를 가진 형태는 한 번만 받을 수 있다.
     *
     * @param realm    the realm that receives the value / 값을 받는 realm
     * @param cloned   the serialized form / 직렬화된 형태
     * @param handler  the handler that creates the moved objects, or null / 옮긴 객체를 만드는 처리기. 없으면 null
     * @param received a list that receives the moved objects in order of the transfer list, or null / 옮긴 객체를 전달 목록 순서로 받는 목록. 없으면 null
     * @return the new value / 새 값
     */
    public static JsValue deserialize(JsRealm realm, ClonedValue cloned, TransferHandler handler, List<JsObject> received) {
        return deserialize(realm, cloned, handler, received, null);
    }

    /**
     * Reads a browser message using the receiving realm's current agent-cluster identity.
     * <p>
     * 받는 realm의 현재 에이전트 클러스터 식별자를 사용해 브라우저 메시지를 읽는다.
     *
     * @param realm the receiving realm / 받는 realm
     * @param cloned the serialized message / 직렬화된 메시지
     * @param handler external transfer handler, or null / 외부 전달 처리기. 없으면 null
     * @param received destination list for transferred objects, or null / 전달 객체를 받을 목록. 없으면 null
     * @return the received value / 받은 값
     * @throws StructuredCloneException if the receiver cannot share the memory / 받는 쪽에서 메모리를 공유할 수 없으면
     */
    public static JsValue deserializeForMessaging(JsRealm realm, ClonedValue cloned, TransferHandler handler, List<JsObject> received) {
        return deserialize(realm, cloned, handler, received, SharedMemoryScope.of(realm));
    }

    /**
     * Checks whether all the graph's buffers may be shared before any objects are reconstituted.
     * <p>
     * 객체를 복원하기 전에 그래프의 모든 공유 버퍼를 이 범위에서 공유할 수 있는지 검사한다.
     *
     * @param realm the receiving realm / 받는 realm
     * @param cloned serialized graph / 직렬화 그래프
     * @param handler external transfer handler, or null / 외부 전달 처리기. 없으면 null
     * @param received destination list for transferred objects, or null / 전달 객체를 받을 목록. 없으면 null
     * @param sharedScope receiving scope, or null when sharing is forbidden / 받는 쪽 범위. 공유 금지이면 null
     * @return the received value / 받은 값
     */
    private static JsValue deserialize(JsRealm realm, ClonedValue cloned, TransferHandler handler, List<JsObject> received,
            SharedMemoryScope sharedScope) {
        if (cloned.sharedScope != null && !cloned.sharedScope.permits(sharedScope)) {
            throw new StructuredCloneException("SharedArrayBuffer is outside its agent cluster.");
        }
        Deserializer d = new Deserializer(realm, cloned, handler);
        JsValue v = d.run();
        // 옮긴 객체를 전달 목록의 순서로 정리해 돌려준다.
        if (received != null) {
            d.collect(received);
        }
        return v;
    }

    /**
     * Copies a value from one realm to another (serialize, then deserialize).
     * <p>
     * 값을 한 realm 에서 다른 realm 으로 복사한다(직렬화한 뒤 역직렬화).
     *
     * @param source the realm of the value / 값의 realm
     * @param target the realm that receives the copy / 복사본을 받는 realm
     * @param value  the value / 값
     * @return the copy / 복사본
     * @throws StructuredCloneException if the value cannot be cloned / 값을 복제할 수 없으면
     */
    public static JsValue copy(JsRealm source, JsRealm target, JsValue value) {
        return deserialize(target, serialize(source, value));
    }

    /**
     * Creates the exception for a value that cannot be cloned.
     * <p>
     * 복제할 수 없는 값에 대한 예외를 만든다.
     *
     * @param what a description of the value / 값의 설명
     * @return the exception / 예외
     */
    static StructuredCloneException cannotClone(String what) {
        return new StructuredCloneException(what + " could not be cloned.");
    }

    /**
     * One step of the depth first walk over the properties or entries of an object.
     * <p>
     * 객체의 프로퍼티나 항목을 깊이 우선으로 훑는 탐색의 한 단계.
     */
    private interface Frame {
        /**
         * Handles the next item of the object.
         * <p>
         * 객체의 다음 항목을 처리한다.
         *
         * @return true when the object is finished / 객체를 끝냈으면 true
         */
        boolean step();
    }

    /**
     * The serializing state of one call.
     * <p>
     * 호출 하나의 직렬화 상태.
     */
    private static final class Serializer {
        /**
         * The limit of the estimated size.
         * <p>
         * 추정 크기의 한도.
         */
        private final long maxBytes;

        /**
         * The estimated size so far.
         * <p>
         * 지금까지의 추정 크기.
         */
        long used;

        /**
         * The cell number of every object found so far.
         * <p>
         * 지금까지 찾은 모든 객체의 칸 번호.
         */
        private final IdentityHashMap<JsObject, Integer> memory = new IdentityHashMap<JsObject, Integer>();

        /**
         * The cells in order of discovery.
         * <p>
         * 발견한 순서대로의 칸.
         */
        final ArrayList<Cell> cells = new ArrayList<Cell>();

        /**
         * The objects whose items are still to be walked (the top is walked first).
         * <p>
         * 아직 항목을 훑어야 하는 객체(맨 위부터 훑는다).
         */
        private final ArrayList<Frame> stack = new ArrayList<Frame>();

        /**
         * The buffers to transfer.
         * <p>
         * 옮길 버퍼.
         */
        private final IdentityHashMap<JsObject, Boolean> transfer = new IdentityHashMap<JsObject, Boolean>();

        /**
         * The transferred buffers in list order.
         * <p>
         * 목록 순서대로의 옮긴 버퍼.
         */
        private final ArrayList<ArrayBufferObject> transferList = new ArrayList<ArrayBufferObject>();

        /**
         * The handler of the transfer list entries that are not ArrayBuffers, or null.
         * <p>
         * ArrayBuffer 가 아닌 전달 목록 항목의 처리기. 없으면 null.
         */
        private final TransferHandler handler;

        /** Source scope permitted to share buffers. / 버퍼를 공유할 수 있는 원본 범위. */
        private final SharedMemoryScope sharedScope;

        /** Whether a shared buffer has been serialized in the graph. / 그래프에 공유 버퍼를 직렬화했는지 여부. */
        private boolean containsShared;

        /**
         * The objects that the handler took, with their payloads (the cell is made from this).
         * <p>
         * 처리기가 받은 객체와 그 내용물(칸은 이것으로 만든다).
         */
        private final IdentityHashMap<JsObject, Object[]> handled = new IdentityHashMap<JsObject, Object[]>();

        /**
         * The objects that the handler took, in list order.
         * <p>
         * 처리기가 받은 객체(목록 순서).
         */
        private final ArrayList<JsObject> handledList = new ArrayList<JsObject>();

        /**
         * Creates the state and checks the transfer list.
         * <p>
         * 상태를 만들고 전달 목록을 검사한다.
         *
         * @param maxBytes the size limit / 크기 한도
         * @param list     the transfer list or null / 전달 목록 또는 null
         * @param handler  the handler of entries that are not ArrayBuffers, or null / ArrayBuffer 가 아닌 항목의 처리기. 없으면 null
         * @param sharedScope the source scope if sharing is allowed / 공유를 허용하는 원본 범위
         */
        Serializer(long maxBytes, List<? extends JsObject> list, TransferHandler handler, SharedMemoryScope sharedScope) {
            this.maxBytes = maxBytes;
            this.handler = handler;
            this.sharedScope = sharedScope;
            if (list == null) {
                return;
            }
            // 전달 목록의 각 항목은 분리되지 않은 ArrayBuffer 이거나 처리기가 받는 객체여야 하며 중복될 수 없다.
            for (int i = 0; i < list.size(); i++) {
                JsObject o = list.get(i);
                if (!(o instanceof ArrayBufferObject)) {
                    if (handler == null) {
                        throw new StructuredCloneException("Value not transferable.");
                    }
                    if (handled.containsKey(o)) {
                        throw new StructuredCloneException("Transfer list contains duplicate entries.");
                    }
                    Object payload = handler.prepare(o);
                    if (payload == null) {
                        throw new StructuredCloneException("Value not transferable.");
                    }
                    handled.put(o, new Object[] { payload, Integer.valueOf(handledList.size()) });
                    handledList.add(o);
                    continue;
                }
                ArrayBufferObject b = (ArrayBufferObject) o;
                if (b.isShared()) {
                    throw new StructuredCloneException("A SharedArrayBuffer cannot be transferred.");
                }
                if (b.isDetached()) {
                    throw new StructuredCloneException("An ArrayBuffer is detached and could not be cloned.");
                }
                if (transfer.containsKey(o)) {
                    throw new StructuredCloneException("Transfer list contains duplicate entries.");
                }
                transfer.put(o, Boolean.TRUE);
                transferList.add(b);
            }
        }

        /**
         * Adds to the estimated size and fails when the limit is passed.
         * <p>
         * 추정 크기에 더하고 한도를 넘으면 실패한다.
         *
         * @param n the bytes to add / 더할 바이트
         */
        void charge(long n) {
            used += n;
            if (used > maxBytes) {
                throw new StructuredCloneException("The data is too large to be cloned.");
            }
        }

        /**
         * Serializes a value: primitives are returned as Java values, objects as the number of their cell.
         * <p>
         * 값을 직렬화한다: 원시 값은 Java 값으로, 객체는 칸 번호로 반환한다.
         *
         * @param v the value / 값
         * @return the Java value or an Integer cell number / Java 값 또는 Integer 칸 번호
         */
        Object value(JsValue v) {
            if (v.isUndefined()) {
                return UNDEFINED;
            }
            if (v.isNull()) {
                return NULL;
            }
            if (v instanceof JsBoolean) {
                return Boolean.valueOf(((JsBoolean) v).getValue());
            }
            if (v instanceof JsNumber) {
                charge(8);
                return Double.valueOf(((JsNumber) v).getValue());
            }
            if (v instanceof JsBigInt) {
                BigInteger n = ((JsBigInt) v).getValue();
                charge(16L + (n.bitLength() + 7) / 8);
                return n;
            }
            if (v instanceof JsString) {
                String s = ((JsString) v).getValue();
                charge(2L * s.length() + 16);
                return s;
            }
            if (v instanceof JsSymbol) {
                throw cannotClone("A symbol");
            }
            return ref((JsObject) v);
        }

        /**
         * Returns the cell number of an object, serializing it first if it was not seen before.
         * <p>
         * 객체의 칸 번호를 반환한다. 처음 보는 객체이면 먼저 직렬화한다.
         *
         * @param o the object / 객체
         * @return the cell number / 칸 번호
         */
        Integer ref(JsObject o) {
            Integer known = memory.get(o);
            if (known != null) {
                return known;
            }
            // 판별한 종류에 맞는 칸을 만들고, 내용을 훑어야 하는 종류는 작업 목록에 올린다.
            Cell c = classify(o);
            charge(48);
            if (cells.size() >= MAX_CELLS) {
                throw new StructuredCloneException("The data has too many objects to be cloned.");
            }
            Integer id = Integer.valueOf(cells.size());
            cells.add(c);
            memory.put(o, id);
            if (c.kind == ARRAY || c.kind == OBJECT) {
                push(new PropertiesFrame(o, c));
            } else if (c.kind == MAP) {
                push(new MapFrame(DataAccess.mapEntries(o), c));
            } else if (c.kind == SET) {
                push(new SetFrame(DataAccess.setValues(o), c));
            } else if (c.kind == ERROR) {
                push(new ErrorFrame(o, c));
            }
            return id;
        }

        /**
         * Pushes a frame, failing when the nesting is too deep.
         * <p>
         * 작업을 올리며 중첩이 너무 깊으면 실패한다.
         *
         * @param f the frame / 작업
         */
        private void push(Frame f) {
            if (stack.size() >= MAX_DEPTH) {
                throw new StructuredCloneException("The data is nested too deeply to be cloned.");
            }
            stack.add(f);
        }

        /**
         * Chooses the kind of an object and fills the fixed fields of its cell.
         * <p>
         * 객체의 종류를 정하고 칸의 고정 필드를 채운다.
         *
         * @param o the object / 객체
         * @return the cell / 칸
         */
        private Cell classify(JsObject o) {
            Object[] taken = handled.get(o);
            // 처리기가 받은 객체(MessagePort)는 내용물을 가진 칸이 된다.
            if (taken != null) {
                Cell c = new Cell(TRANSFERRED);
                c.payload = taken[0];
                c.ordinal = ((Integer) taken[1]).intValue();
                return c;
            }
            if (o.getClass() == JsArray.class) {
                Cell c = new Cell(ARRAY);
                c.arrayLength = ((JsArray) o).getLength();
                return c;
            }
            if (o instanceof JsPrimitiveObject) {
                JsValue p = ((JsPrimitiveObject) o).getPrimitive();
                if (p instanceof JsBoolean) {
                    Cell c = new Cell(BOOLEAN_OBJECT);
                    c.flag = ((JsBoolean) p).getValue();
                    return c;
                }
                if (p instanceof JsNumber) {
                    Cell c = new Cell(NUMBER_OBJECT);
                    c.number = ((JsNumber) p).getValue();
                    return c;
                }
                if (p instanceof JsBigInt) {
                    Cell c = new Cell(BIGINT_OBJECT);
                    c.text = ((JsBigInt) p).getValue().toString();
                    charge(2L * c.text.length());
                    return c;
                }
                throw cannotClone("A Symbol object");
            }
            if (o instanceof JsStringObject) {
                Cell c = new Cell(STRING_OBJECT);
                c.text = ((JsStringObject) o).getStringValue();
                charge(2L * c.text.length());
                return c;
            }
            if (DataAccess.isDate(o)) {
                Cell c = new Cell(DATE);
                c.number = DataAccess.dateValue(o);
                return c;
            }
            if (DataAccess.isRegExp(o)) {
                Cell c = new Cell(REGEXP);
                c.text = DataAccess.regExpSource(o);
                c.text2 = DataAccess.regExpFlags(o);
                charge(2L * (c.text.length() + c.text2.length()));
                return c;
            }
            if (DataAccess.isMap(o)) {
                return new Cell(MAP);
            }
            if (DataAccess.isSet(o)) {
                return new Cell(SET);
            }
            if (o instanceof ArrayBufferObject) {
                return bufferCell((ArrayBufferObject) o);
            }
            if (o instanceof TypedArrayObject) {
                TypedArrayObject t = (TypedArrayObject) o;
                if (t.isOutOfBounds()) {
                    throw new StructuredCloneException("A typed array is outside its buffer and could not be cloned.");
                }
                Cell c = new Cell(TYPED);
                c.text = t.getType().getConstructorName();
                c.bufferRef = ref(t.getBuffer()).intValue();
                c.offset = t.getByteOffset();
                c.length = t.isLengthTracking() ? -1 : t.getLength();
                return c;
            }
            if (o instanceof DataViewObject) {
                DataViewObject d = (DataViewObject) o;
                if (d.isOutOfBounds()) {
                    throw new StructuredCloneException("A DataView is outside its buffer and could not be cloned.");
                }
                Cell c = new Cell(VIEW);
                c.bufferRef = ref(d.getBuffer()).intValue();
                c.offset = d.getByteOffset();
                c.length = d.isLengthTracking() ? -1 : d.getByteLength();
                return c;
            }
            if (o instanceof DomExceptionObjects.Value) {
                DomExceptionObjects.Value exception = (DomExceptionObjects.Value) o;
                Cell c = new Cell(DOM_EXCEPTION);
                c.text = exception.getDomName();
                c.text2 = exception.getDomMessage();
                charge(2L * (c.text.length() + (long) c.text2.length()));
                return c;
            }
            if (o instanceof JsErrorObject) {
                return errorCell(o);
            }
            if (o.isCallable()) {
                throw cannotClone("A function");
            }
            if (o.getClass() == JsObject.class) {
                return new Cell(OBJECT);
            }
            throw cannotClone("An object of type " + o.className());
        }

        /**
         * Creates the cell of an ArrayBuffer; a transferred buffer hands over its bytes, any other buffer is copied.
         * <p>
         * ArrayBuffer 의 칸을 만든다. 옮기는 버퍼는 바이트를 넘기고 다른 버퍼는 복사한다.
         *
         * @param b the buffer / 버퍼
         * @return the cell / 칸
         */
        private Cell bufferCell(ArrayBufferObject b) {
            if (b.isShared()) {
                if (sharedScope == null) {
                    throw new StructuredCloneException("SharedArrayBuffer is not permitted at this boundary.");
                }
                charge(b.getByteLength());
                Cell c = new Cell(SHARED_BUFFER);
                c.payload = new SharedBufferReference(b);
                containsShared = true;
                return c;
            }
            if (b.isDetached()) {
                throw new StructuredCloneException("An ArrayBuffer is detached and could not be cloned.");
            }
            Cell c = new Cell(BUFFER);
            // 전달 대상은 getter 실행이 끝난 뒤 실제로 옮길 바이트를 채운다.
            if (!transfer.containsKey(b)) {
                byte[] data = b.getData();
                charge(data.length);
                c.bytes = data.clone();
                c.maxByteLength = b.isResizable() ? b.getMaxByteLength() : -1;
            }
            return c;
        }

        /**
         * Creates the cell of an Error object (its <code>name</code> only counts when it is one of the native error names).
         * <p>
         * Error 객체의 칸을 만든다(<code>name</code> 은 기본 오류 이름 중 하나일 때만 인정한다).
         *
         * @param o the error object / 오류 객체
         * @return the cell / 칸
         */
        private Cell errorCell(JsObject o) {
            Cell c = new Cell(ERROR);
            JsValue n = o.get("name");
            String name = "Error";
            if (n instanceof JsString) {
                ErrorKind[] kinds = ErrorKind.values();
                // 기본 오류 이름이 아니면 Error 로 취급한다.
                for (int i = 0; i < kinds.length; i++) {
                    if (kinds[i].getErrorName().equals(((JsString) n).getValue())) {
                        name = kinds[i].getErrorName();
                    }
                }
            }
            c.text = name;
            PropertyDescriptor m = o.getOwnProperty("message");
            if (m != null && !m.isAccessor() && m.value != null) {
                c.text2 = Ops.toString(m.value);
                charge(2L * c.text2.length());
            }
            PropertyDescriptor st = o.getOwnProperty("stack");
            if (st != null && !st.isAccessor() && st.value instanceof JsString) {
                c.text3 = ((JsString) st.value).getValue();
                charge(2L * c.text3.length());
            }
            return c;
        }

        /**
         * Serializes an error's accompanying data after recording the source object's identity.
         * <p>
         * 원본 오류의 정체성을 등록한 뒤 함께 전달할 데이터를 직렬화한다.
         */
        private final class ErrorFrame implements Frame {
            /** The original error. / 원본 오류. */
            private final JsObject source;

            /** The serialized record. / 직렬화 기록. */
            private final Cell cell;

            /** The next field, in the order cause, errors, done. / 다음 단계. cause, errors, 완료 순이다. */
            private int field;

            /**
             * Creates a pending frame for the error's optional fields.
             * <p>
             * 오류의 선택 필드를 처리할 작업을 만든다.
             *
             * @param source the original error / 원본 오류
             * @param cell the error record / 오류 기록
             */
            ErrorFrame(JsObject source, Cell cell) {
                this.source = source;
                this.cell = cell;
            }

            /**
             * Serializes one accompanying field and lets any newly found object finish before proceeding.
             * <p>
             * 부가 필드 하나를 직렬화하며 새 객체를 발견하면 그 객체의 처리를 먼저 끝낸다.
             *
             * @return true when both fields were handled / 두 필드를 처리했으면 true
             */
            public boolean step() {
                // 각 값을 읽을 때 다른 작업이 추가될 수 있으므로 마지막 호출에서만 완료한다.
                if (field == 0) {
                    field++;
                    cell.cause = ownData("cause");
                    return false;
                }
                if (field == 1 && ErrorKind.AGGREGATE_ERROR.getErrorName().equals(cell.text)) {
                    field++;
                    cell.errors = ownData("errors");
                    return false;
                }
                return true;
            }

            /**
             * Serializes an own data property without invoking a getter.
             * <p>
             * getter를 호출하지 않고 자체 데이터 프로퍼티를 직렬화한다.
             *
             * @param key the field name / 필드 이름
             * @return encoded data, or Java null if the field is absent / 직렬화한 데이터. 필드가 없으면 Java null
             */
            private Object ownData(String key) {
                PropertyDescriptor desc = source.getOwnProperty(key);
                return desc == null || desc.isAccessor() ? null : value(desc.value);
            }
        }

        /**
         * Walks the pending objects depth first until everything reachable is serialized.
         * <p>
         * 대기 중인 객체를 깊이 우선으로 훑어 닿을 수 있는 모든 것이 직렬화될 때까지 진행한다.
         */
        void run() {
            // 맨 위 작업의 다음 항목을 처리하고, 끝난 작업은 내린다. 새 객체를 발견하면 그 작업이 위에 올라 먼저 처리된다.
            while (!stack.isEmpty()) {
                if (stack.get(stack.size() - 1).step()) {
                    stack.remove(stack.size() - 1);
                }
            }
        }

        /**
         * Checks the final state of every transfer and records its data, including values the root does not reach.
         * <p>
         * 모든 전달 대상의 최종 상태를 확인하고 루트가 닿지 않는 버퍼도 데이터를 기록한다.
         */
        void prepareTransfers() {
            // 값에서 닿지 않은 전달 대상 버퍼도 칸으로 만든다.
            for (int i = 0; i < transferList.size(); i++) {
                ref(transferList.get(i));
            }
            // 처리기가 받은 객체도 값에 닿지 않았더라도 칸으로 만든다(받는 쪽이 목록 순서대로 받을 수 있도록).
            for (int i = 0; i < handledList.size(); i++) {
                ref(handledList.get(i));
            }
            run();
            // getter가 바꾼 버퍼 상태와 한도를 모두 검사한 뒤에만 전달을 확정한다.
            for (int i = 0; i < transferList.size(); i++) {
                ArrayBufferObject buffer = transferList.get(i);
                if (buffer.isDetached()) {
                    throw new StructuredCloneException("An ArrayBuffer was detached before transfer.");
                }
                Cell c = cells.get(memory.get(buffer).intValue());
                c.bytes = buffer.getData();
                charge(c.bytes.length);
                c.maxByteLength = buffer.isResizable() ? buffer.getMaxByteLength() : -1;
            }
        }

        /**
         * Releases the originals after the entire message has been accepted.
         * <p>
         * 전체 메시지가 검증을 통과한 뒤 원본을 분리한다.
         */
        void commitTransfers() {
            // 모든 전달 대상이 유효하면 원본 버퍼를 분리한다.
            for (int i = 0; i < transferList.size(); i++) {
                transferList.get(i).detach();
            }
            // 처리기가 받은 객체의 원본도 내놓는다.
            for (int i = 0; i < handledList.size(); i++) {
                JsObject o = handledList.get(i);
                handler.commit(o, handled.get(o)[0]);
            }
        }

        /**
         * Walks the own enumerable string keyed properties of an Array or ordinary object.
         * <p>
         * Array 또는 일반 객체의 자체 열거 가능한 문자열 키 프로퍼티를 훑는다.
         */
        private final class PropertiesFrame implements Frame {
            /**
             * The source object.
             * <p>
             * 원본 객체.
             */
            private final JsObject src;

            /**
             * The cell that receives the properties.
             * <p>
             * 프로퍼티를 받는 칸.
             */
            private final Cell cell;

            /**
             * The keys, fixed when the walk starts.
             * <p>
             * 탐색을 시작할 때 정한 키 목록.
             */
            private final List<String> keyList;

            /**
             * The next key to handle.
             * <p>
             * 다음에 처리할 키.
             */
            private int pos;

            /**
             * Creates the walk.
             * <p>
             * 탐색을 만든다.
             *
             * @param src  the source object / 원본 객체
             * @param cell the cell / 칸
             */
            PropertiesFrame(JsObject src, Cell cell) {
                this.src = src;
                this.cell = cell;
                this.keyList = Ops.enumerableOwnKeys(src);
            }

            /**
             * Reads the next property (a getter runs here) and serializes its value.
             * <p>
             * 다음 프로퍼티를 읽고(getter 가 여기서 실행된다) 그 값을 직렬화한다.
             *
             * @return true when all keys are handled / 모든 키를 처리했으면 true
             */
            public boolean step() {
                if (pos >= keyList.size()) {
                    return true;
                }
                String key = keyList.get(pos++);
                // 앞선 getter 가 지운 프로퍼티는 건너뛴다.
                if (!src.hasOwnProperty(key)) {
                    return false;
                }
                JsValue v = src.get(key);
                charge(2L * key.length() + 16);
                cell.keys.add(key);
                cell.values.add(value(v));
                return false;
            }
        }

        /**
         * Walks a snapshot of the entries of a Map.
         * <p>
         * Map 항목의 복사본을 훑는다.
         */
        private final class MapFrame implements Frame {
            /**
             * The snapshot of the entries.
             * <p>
             * 항목의 복사본.
             */
            private final List<JsValue[]> entries;

            /**
             * The cell that receives the entries.
             * <p>
             * 항목을 받는 칸.
             */
            private final Cell cell;

            /**
             * The next entry.
             * <p>
             * 다음 항목.
             */
            private int pos;

            /**
             * Creates the walk.
             * <p>
             * 탐색을 만든다.
             *
             * @param entries the entries / 항목
             * @param cell    the cell / 칸
             */
            MapFrame(List<JsValue[]> entries, Cell cell) {
                this.entries = entries;
                this.cell = cell;
            }

            /**
             * Serializes the key and the value of the next entry.
             * <p>
             * 다음 항목의 키와 값을 직렬화한다.
             *
             * @return true when all entries are handled / 모든 항목을 처리했으면 true
             */
            public boolean step() {
                if (pos >= entries.size()) {
                    return true;
                }
                JsValue[] e = entries.get(pos++);
                cell.values.add(value(e[0]));
                cell.values.add(value(e[1]));
                return false;
            }
        }

        /**
         * Walks a snapshot of the values of a Set.
         * <p>
         * Set 값의 복사본을 훑는다.
         */
        private final class SetFrame implements Frame {
            /**
             * The snapshot of the values.
             * <p>
             * 값의 복사본.
             */
            private final List<JsValue> items;

            /**
             * The cell that receives the values.
             * <p>
             * 값을 받는 칸.
             */
            private final Cell cell;

            /**
             * The next value.
             * <p>
             * 다음 값.
             */
            private int pos;

            /**
             * Creates the walk.
             * <p>
             * 탐색을 만든다.
             *
             * @param items the values / 값
             * @param cell  the cell / 칸
             */
            SetFrame(List<JsValue> items, Cell cell) {
                this.items = items;
                this.cell = cell;
            }

            /**
             * Serializes the next value.
             * <p>
             * 다음 값을 직렬화한다.
             *
             * @return true when all values are handled / 모든 값을 처리했으면 true
             */
            public boolean step() {
                if (pos >= items.size()) {
                    return true;
                }
                cell.values.add(value(items.get(pos++)));
                return false;
            }
        }
    }

    /**
     * The deserializing state of one call.
     * <p>
     * 호출 하나의 역직렬화 상태.
     */
    private static final class Deserializer {
        /**
         * The realm that receives the objects.
         * <p>
         * 객체를 받는 realm.
         */
        private final JsRealm realm;

        /**
         * The serialized form.
         * <p>
         * 직렬화된 형태.
         */
        private final ClonedValue cloned;

        /**
         * The new objects by cell number.
         * <p>
         * 칸 번호별 새 객체.
         */
        private final JsObject[] objects;

        /**
         * The handler that creates the moved objects, or null.
         * <p>
         * 옮긴 객체를 만드는 처리기. 없으면 null.
         */
        private final TransferHandler handler;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param realm   the realm / realm
         * @param cloned  the serialized form / 직렬화된 형태
         * @param handler the handler that creates the moved objects, or null / 옮긴 객체를 만드는 처리기. 없으면 null
         */
        Deserializer(JsRealm realm, ClonedValue cloned, TransferHandler handler) {
            this.realm = realm;
            this.cloned = cloned;
            this.handler = handler;
            this.objects = new JsObject[cloned.cells.length];
        }

        /**
         * Adds the moved objects to a list in the order of the transfer list.
         * <p>
         * 옮긴 객체를 전달 목록의 순서대로 목록에 더한다.
         *
         * @param out the list / 목록
         */
        void collect(List<JsObject> out) {
            Cell[] cells = cloned.cells;
            int count = 0;
            // 옮긴 객체의 수를 센다.
            for (int i = 0; i < cells.length; i++) {
                if (cells[i].kind == TRANSFERRED) {
                    count++;
                }
            }
            JsObject[] ordered = new JsObject[count];
            // 칸의 순서 번호 자리에 객체를 놓는다.
            for (int i = 0; i < cells.length; i++) {
                if (cells[i].kind == TRANSFERRED) {
                    ordered[cells[i].ordinal] = objects[i];
                }
            }
            // 순서대로 목록에 더한다.
            for (int i = 0; i < ordered.length; i++) {
                out.add(ordered[i]);
            }
        }

        /**
         * Builds the value: first an empty object for every cell (so that cycles can be closed), then views over buffers, then the contents.
         * <p>
         * 값을 만든다: 먼저 칸마다 빈 객체를 만들고(순환을 닫을 수 있도록), 다음에 버퍼 위의 뷰, 마지막에 내용을 채운다.
         *
         * @return the value / 값
         */
        JsValue run() {
            Cell[] cells = cloned.cells;
            // 1단계: 다른 객체를 가리키지 않는 칸의 빈 객체(또는 완성된 객체)를 만든다.
            for (int i = 0; i < cells.length; i++) {
                if (cells[i].kind != TYPED && cells[i].kind != VIEW) {
                    objects[i] = shell(cells[i]);
                }
            }
            // 2단계: 버퍼를 가리키는 뷰를 만든다.
            for (int i = 0; i < cells.length; i++) {
                if (cells[i].kind == TYPED || cells[i].kind == VIEW) {
                    objects[i] = view(cells[i]);
                }
            }
            // 3단계: 배열, 객체, Map, Set 의 내용을 채운다.
            for (int i = 0; i < cells.length; i++) {
                fill(cells[i], objects[i]);
            }
            return resolve(cloned.root);
        }

        /**
         * Turns a serialized item into a value.
         * <p>
         * 직렬화된 항목을 값으로 바꾼다.
         *
         * @param r the item / 항목
         * @return the value / 값
         */
        private JsValue resolve(Object r) {
            if (r == UNDEFINED) {
                return Js.UNDEFINED;
            }
            if (r == NULL) {
                return Js.NULL;
            }
            if (r instanceof Boolean) {
                return Js.bool(((Boolean) r).booleanValue());
            }
            if (r instanceof Double) {
                return Js.num(((Double) r).doubleValue());
            }
            if (r instanceof BigInteger) {
                return JsBigInt.of((BigInteger) r);
            }
            if (r instanceof String) {
                return Js.str((String) r);
            }
            return objects[((Integer) r).intValue()];
        }

        /**
         * Creates the object of a cell that does not point at other objects.
         * <p>
         * 다른 객체를 가리키지 않는 칸의 객체를 만든다.
         *
         * @param c the cell / 칸
         * @return the object / 객체
         */
        private JsObject shell(Cell c) {
            switch (c.kind) {
            case BOOLEAN_OBJECT:
                return new JsPrimitiveObject(realm.getBooleanPrototype(), JsBoolean.of(c.flag), "Boolean");
            case NUMBER_OBJECT:
                return new JsPrimitiveObject(realm.getNumberPrototype(), Js.num(c.number), "Number");
            case BIGINT_OBJECT:
                return new JsPrimitiveObject(realm.getBigIntPrototype(), JsBigInt.of(new BigInteger(c.text)), "BigInt");
            case STRING_OBJECT:
                return new JsStringObject(realm.getStringPrototype(), c.text);
            case DATE:
                return DataAccess.newDate(realm, c.number);
            case REGEXP:
                return DataAccess.newRegExp(realm, c.text, c.text2);
            case ARRAY:
                return realm.newArray();
            case OBJECT:
                return realm.newObject();
            case MAP:
                return DataAccess.newMap(realm);
            case SET:
                return DataAccess.newSet(realm);
            case BUFFER:
                return TypedArrays.newArrayBuffer(realm, c.bytes.clone(), c.maxByteLength);
            case SHARED_BUFFER:
                return ((SharedBufferReference) c.payload).wrap(realm);
            case DOM_EXCEPTION:
                return DomExceptionObjects.create(realm, c.text, c.text2);
            case TRANSFERRED:
                if (handler == null) {
                    throw new IllegalStateException("A transferred object needs a transfer handler");
                }
                return handler.receive(realm, c.payload);
            default:
                return error(c);
            }
        }

        /**
         * Creates an Error object of a cell.
         * <p>
         * 칸의 Error 객체를 만든다.
         *
         * @param c the cell / 칸
         * @return the error object / 오류 객체
         */
        private JsObject error(Cell c) {
            ErrorKind kind = ErrorKind.ERROR;
            ErrorKind[] kinds = ErrorKind.values();
            // 이름이 같은 오류 종류를 찾는다.
            for (int i = 0; i < kinds.length; i++) {
                if (kinds[i].getErrorName().equals(c.text)) {
                    kind = kinds[i];
                }
            }
            JsObject e = realm.createError(kind, c.text2);
            if (c.text3 != null) {
                e.defineOwnProperty("stack", PropertyDescriptor.data(Js.str(c.text3), true, false, true));
            }
            return e;
        }

        /**
         * Creates a typed array or DataView over the buffer of its cell.
         * <p>
         * 칸의 버퍼 위에 형식화 배열 또는 DataView 를 만든다.
         *
         * @param c the cell / 칸
         * @return the view / 뷰
         */
        private JsObject view(Cell c) {
            ArrayBufferObject b = (ArrayBufferObject) objects[c.bufferRef];
            if (c.kind == VIEW) {
                return new DataViewObject(realm.getIntrinsic("%DataViewPrototype%"), b, c.offset, c.length);
            }
            JsObject prototype = realm.getIntrinsic("%" + c.text + "Prototype%");
            return new TypedArrayObject(prototype, elementType(c.text), b, c.offset, c.length);
        }

        /**
         * Fills the outgoing references of a cloned object after every object has been created.
         * <p>
         * 객체를 모두 생성한 뒤 복제된 객체의 참조를 채운다.
         *
         * @param c the cell / 칸
         * @param o the object of the cell / 칸의 객체
         */
        private void fill(Cell c, JsObject o) {
            if (c.kind == ARRAY || c.kind == OBJECT) {
                // 키와 값을 짝지어 자체 데이터 프로퍼티로 만든다(__proto__ 도 일반 프로퍼티이다).
                for (int i = 0; i < c.keys.size(); i++) {
                    o.createDataProperty(c.keys.get(i), resolve(c.values.get(i)));
                }
                if (c.kind == ARRAY) {
                    o.set("length", Js.num((double) c.arrayLength));
                }
            } else if (c.kind == MAP) {
                // 키와 값이 번갈아 저장되어 있다.
                for (int i = 0; i + 1 < c.values.size(); i += 2) {
                    DataAccess.mapPut(o, resolve(c.values.get(i)), resolve(c.values.get(i + 1)));
                }
            } else if (c.kind == SET) {
                // 저장된 순서대로 추가한다.
                for (int i = 0; i < c.values.size(); i++) {
                    DataAccess.setAdd(o, resolve(c.values.get(i)));
                }
            } else if (c.kind == ERROR) {
                // 표준 Error 생성자가 사용하는 속성으로 원인과 집계 목록을 연결한다.
                if (c.cause != null) {
                    o.defineOwnProperty("cause", PropertyDescriptor.data(resolve(c.cause), true, false, true));
                }
                if (c.errors != null) {
                    o.defineOwnProperty("errors", PropertyDescriptor.data(resolve(c.errors), true, false, true));
                }
            }
        }
    }
}
