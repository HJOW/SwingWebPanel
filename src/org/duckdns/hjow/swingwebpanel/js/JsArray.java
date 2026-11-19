package org.duckdns.hjow.swingwebpanel.js;

import java.util.ArrayList;
import java.util.List;

/**
 * An Array exotic object (ECMA-262 6th edition 9.4.2). Elements are kept in a dense Java array while the array is "plain"
 * (all elements writable, enumerable and configurable, no huge gaps); the array silently switches to a sparse mode that stores
 * indices as ordinary properties when needed (for example after <code>Object.defineProperty(a, 0, {writable:false})</code>
 * or <code>a[1e9] = 1</code>). The virtual "length" property follows ArraySetLength (9.4.2.4).
 * <p>
 * Built-in authors can use {@link #add(JsValue)} to fill a fresh array quickly and {@link #getLength()} for the length;
 * generic algorithms should go through {@link JsObject#get(Object)} so that they also work for array-like objects.
 * <p>
 * Array 이국적 객체(ECMA-262 6판 9.4.2). 배열이 "평범"하면(모든 요소가 writable, enumerable, configurable 이고 큰 빈 구간이 없으면)
 * 요소를 조밀한 Java 배열에 보관하고, 필요하면(<code>Object.defineProperty(a, 0, {writable:false})</code> 나 <code>a[1e9] = 1</code> 등)
 * 인덱스를 일반 프로퍼티로 저장하는 희소 모드로 조용히 전환한다. 가상 "length" 프로퍼티는 ArraySetLength(9.4.2.4)를 따른다.
 * <p>
 * 내장 객체 작성자는 새 배열을 빠르게 채울 때 {@link #add(JsValue)}, 길이는 {@link #getLength()} 를 쓸 수 있다.
 * 일반 알고리즘은 유사 배열 객체에서도 동작하도록 {@link JsObject#get(Object)} 를 거치는 것이 좋다.
 */
public class JsArray extends JsObject {

    /**
     * Maximum gap between the dense capacity and a new index before switching to sparse mode.
     * <p>
     * 희소 모드로 전환하기 전에 허용하는, 조밀 배열 용량과 새 인덱스 사이의 최대 간격.
     */
    private static final int MAX_DENSE_GAP = 4096;

    /**
     * Largest dense array size.
     * <p>
     * 조밀 배열의 최대 크기.
     */
    private static final int MAX_DENSE_SIZE = 50000000;

    /**
     * Dense element storage; a Java null element is a hole. Null in sparse mode.
     * <p>
     * 조밀 요소 저장소. Java null 요소는 빈 칸(hole)이다. 희소 모드에서는 null.
     */
    private JsValue[] dense;

    /**
     * The array length (0 to 2^32 - 1).
     * <p>
     * 배열 길이(0 이상 2^32 - 1 이하).
     */
    private long length;

    /**
     * True when indices are stored in the property table.
     * <p>
     * 인덱스를 프로퍼티 표에 저장하면 true.
     */
    private boolean sparse;

    /**
     * The [[Writable]] attribute of the length property.
     * <p>
     * length 프로퍼티의 [[Writable]] 속성.
     */
    private boolean lengthWritable = true;

    /**
     * Creates an empty array.
     * <p>
     * 빈 배열을 만든다.
     *
     * @param proto the prototype (normally Array.prototype) / 프로토타입(보통 Array.prototype)
     */
    public JsArray(JsObject proto) {
        super(proto);
        this.dense = new JsValue[4];
    }

    /**
     * Creates an array that takes over a list of elements (no copy; null entries are holes).
     * <p>
     * 요소 목록을 넘겨받아 배열을 만든다(복사하지 않음. null 항목은 빈 칸).
     *
     * @param proto    the prototype / 프로토타입
     * @param elements the elements / 요소들
     */
    public JsArray(JsObject proto, JsValue[] elements) {
        super(proto);
        this.dense = elements;
        this.length = elements.length;
    }

    /**
     * Returns "Array".
     * <p>
     * "Array" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Array";
    }

    /**
     * Returns true: this is an Array exotic object.
     * <p>
     * 배열 이국적 객체이므로 true.
     *
     * @return true / true
     */
    public boolean isArray() {
        return true;
    }

    /**
     * Returns the array length.
     * <p>
     * 배열 길이를 반환한다.
     *
     * @return the length / 길이
     */
    public long getLength() {
        return length;
    }

    /**
     * Appends an element at the end (fast path for built-ins creating fresh arrays). The array must be plain and extensible.
     * <p>
     * 끝에 요소를 추가한다(새 배열을 만드는 내장 객체용 빠른 경로). 배열은 평범하고 확장 가능해야 한다.
     *
     * @param v the element / 요소
     */
    public void add(JsValue v) {
        if (!sparse && lengthWritable && isExtensible() && length < MAX_DENSE_SIZE && length <= dense.length) {
            // 조밀 모드의 끝에 바로 붙인다.
            ensureCapacity((int) length + 1);
            dense[(int) length] = v;
            length++;
            return;
        }
        defineOwnProperty(PropertyKeys.fromIndex(length), PropertyDescriptor.data(v, true, true, true));
    }

    /**
     * Fast element read: returns the own element at an index, or null if it is a hole, absent or the array is sparse
     * (the caller then falls back to the generic [[Get]]).
     * <p>
     * 빠른 요소 읽기: 인덱스의 자체 요소를 반환한다. 빈 칸이거나 없거나 희소 배열이면 null(호출자는 일반 [[Get]] 으로 되돌아간다).
     *
     * @param i the index / 인덱스
     * @return the element or null / 요소 또는 null
     */
    public JsValue getOwnElement(int i) {
        if (!sparse && i >= 0 && i < dense.length && i < length) {
            return dense[i];
        }
        return null;
    }

    /**
     * Fast element write for an existing own element: returns true if the element was overwritten in place.
     * Otherwise the caller uses the generic [[Set]].
     * <p>
     * 이미 있는 자체 요소를 빠르게 덮어쓴다. 제자리에서 덮어썼으면 true. 아니면 호출자가 일반 [[Set]] 을 쓴다.
     *
     * @param i the index / 인덱스
     * @param v the value / 값
     * @return true if written / 썼으면 true
     */
    public boolean setOwnElement(int i, JsValue v) {
        if (!sparse && i >= 0 && i < dense.length && i < length && dense[i] != null) {
            dense[i] = v;
            return true;
        }
        return false;
    }

    /**
     * Exposes the virtual length and dense elements.
     * <p>
     * 가상 length 와 조밀 요소를 보여 준다.
     *
     * @param key the key / 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        if (key instanceof String) {
            String s = (String) key;
            if ("length".equals(s)) {
                return Property.data(JsNumber.of((double) length), lengthWritable, false, false);
            }
            if (!sparse) {
                long idx = PropertyKeys.arrayIndex(s);
                if (idx >= 0) {
                    if (idx < dense.length && idx < length && dense[(int) idx] != null) {
                        return Property.data(dense[(int) idx], true, true, true);
                    }
                    return null;
                }
            }
        }
        return super.getOwnPropertySlot(key);
    }

    /**
     * Implements [[DefineOwnProperty]] for arrays (9.4.2.1).
     * <p>
     * 배열의 [[DefineOwnProperty]] 를 구현한다(9.4.2.1).
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        if (key instanceof String) {
            String s = (String) key;
            if ("length".equals(s)) {
                return defineLength(desc);
            }
            long idx = PropertyKeys.arrayIndex(s);
            if (idx >= 0) {
                return defineIndex(idx, s, desc);
            }
        }
        return super.defineOwnProperty(key, desc);
    }

    /**
     * Defines an element.
     * <p>
     * 요소를 정의한다.
     *
     * @param idx  the index / 인덱스
     * @param key  the key string / 키 문자열
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    private boolean defineIndex(long idx, String key, PropertyDescriptor desc) {
        if (idx >= length && !lengthWritable) {
            return false;
        }
        boolean ok;
        if (!sparse) {
            boolean exists = idx < dense.length && idx < length && dense[(int) idx] != null;
            boolean plainExisting = !desc.isAccessor() && (desc.writable == null || desc.isWritableTrue())
                    && (desc.enumerable == null || desc.isEnumerableTrue())
                    && (desc.configurable == null || desc.isConfigurableTrue());
            boolean plainNew = !desc.isAccessor() && desc.isWritableTrue() && desc.isEnumerableTrue() && desc.isConfigurableTrue();
            if (exists && plainExisting) {
                if (desc.value != null) {
                    dense[(int) idx] = desc.value;
                }
                ok = true;
            } else if (!exists && plainNew && fitsDense(idx)) {
                if (!isExtensible()) {
                    return false;
                }
                ensureCapacity((int) idx + 1);
                dense[(int) idx] = desc.value != null ? desc.value : Js.UNDEFINED;
                ok = true;
            } else {
                // 평범하지 않은 속성이 필요하면 희소 모드로 바꾸고 일반 규칙을 적용한다.
                toSparse();
                ok = super.defineOwnProperty(key, desc);
            }
        } else {
            ok = super.defineOwnProperty(key, desc);
        }
        if (ok && idx >= length) {
            length = idx + 1;
        }
        return ok;
    }

    /**
     * Tests whether an index can be stored densely.
     * <p>
     * 인덱스를 조밀하게 저장할 수 있는지 검사한다.
     *
     * @param idx the index / 인덱스
     * @return true if dense storage is appropriate / 조밀 저장이 적절하면 true
     */
    private boolean fitsDense(long idx) {
        return idx < MAX_DENSE_SIZE && idx <= dense.length + MAX_DENSE_GAP;
    }

    /**
     * Makes sure the dense array can hold the given number of elements.
     * <p>
     * 조밀 배열이 주어진 개수의 요소를 담을 수 있게 한다.
     *
     * @param needed required capacity / 필요한 용량
     */
    private void ensureCapacity(int needed) {
        if (needed > dense.length) {
            int cap = Math.max(needed, dense.length + (dense.length >> 1) + 4);
            JsValue[] n = new JsValue[cap];
            System.arraycopy(dense, 0, n, 0, dense.length);
            dense = n;
        }
    }

    /**
     * Converts to sparse mode: all dense elements move into the property table.
     * <p>
     * 희소 모드로 바꾼다: 조밀 요소가 모두 프로퍼티 표로 옮겨진다.
     */
    private void toSparse() {
        if (sparse) {
            return;
        }
        // 빈 칸이 아닌 요소를 일반 프로퍼티로 옮긴다.
        for (int i = 0; i < dense.length && i < length; i++) {
            if (dense[i] != null) {
                super.storeProperty(PropertyKeys.fromIndex(i), Property.data(dense[i], true, true, true));
            }
        }
        dense = null;
        sparse = true;
    }

    /**
     * Implements ArraySetLength (9.4.2.4).
     * <p>
     * ArraySetLength 를 구현한다(9.4.2.4).
     *
     * @param desc the descriptor for "length" / "length" 기술자
     * @return true on success / 성공하면 true
     */
    private boolean defineLength(PropertyDescriptor desc) {
        if (desc.isAccessor() || desc.isConfigurableTrue() || desc.isEnumerableTrue()) {
            return false;
        }
        if (desc.value == null) {
            // 값이 없으면 writable 만 바꿀 수 있다.
            if (desc.writable != null) {
                if (desc.writable.booleanValue() && !lengthWritable) {
                    return false;
                }
                if (!desc.writable.booleanValue()) {
                    lengthWritable = false;
                }
            }
            return true;
        }
        double numberLen = Ops.toNumber(desc.value);
        long newLen = Ops.toUint32(desc.value);
        if (newLen != numberLen) {
            throw JsException.rangeError("Invalid array length");
        }
        if (newLen == length) {
            if (desc.writable != null) {
                if (desc.writable.booleanValue() && !lengthWritable) {
                    return false;
                }
                if (!desc.writable.booleanValue()) {
                    lengthWritable = false;
                }
            }
            return true;
        }
        if (!lengthWritable) {
            return false;
        }
        boolean success = true;
        if (newLen > length) {
            length = newLen;
        } else {
            success = truncate(newLen);
        }
        if (desc.writable != null && !desc.writable.booleanValue()) {
            lengthWritable = false;
        }
        return success;
    }

    /**
     * Removes elements at or above a new length, stopping at the first non-configurable element.
     * <p>
     * 새 길이 이상의 요소를 지운다. 설정 불가 요소를 만나면 거기서 멈춘다.
     *
     * @param newLen the requested length / 요청된 길이
     * @return true if the full truncation succeeded / 모두 지웠으면 true
     */
    private boolean truncate(long newLen) {
        if (!sparse) {
            int keep = (int) Math.min(newLen, dense.length);
            // 조밀 모드에서는 남길 길이 이후의 칸을 비운다.
            for (int i = keep; i < dense.length; i++) {
                dense[i] = null;
            }
            length = newLen;
            if (dense.length > 64 && newLen < dense.length / 4) {
                JsValue[] n = new JsValue[(int) Math.max(newLen, 4)];
                System.arraycopy(dense, 0, n, 0, Math.min(n.length, dense.length));
                dense = n;
            }
            return true;
        }
        List<Long> idx = new ArrayList<Long>();
        // 희소 모드에서는 새 길이 이상의 인덱스 키를 모은다.
        for (Object k : super.ownKeys()) {
            if (k instanceof String) {
                long i = PropertyKeys.arrayIndex((String) k);
                if (i >= newLen) {
                    idx.add(Long.valueOf(i));
                }
            }
        }
        // 큰 인덱스부터 지우다가 설정 불가 프로퍼티에서 멈춘다.
        for (int n = idx.size() - 1; n >= 0; n--) {
            long i = idx.get(n).longValue();
            if (!super.delete(PropertyKeys.fromIndex(i))) {
                length = i + 1;
                return false;
            }
        }
        length = newLen;
        return true;
    }

    /**
     * Deletes an element or rejects deleting "length".
     * <p>
     * 요소를 지운다. "length" 는 지울 수 없다.
     *
     * @param key the key / 키
     * @return true if gone afterwards / 삭제 후 없으면 true
     */
    public boolean delete(Object key) {
        if (key instanceof String) {
            String s = (String) key;
            if ("length".equals(s)) {
                return false;
            }
            if (!sparse) {
                long idx = PropertyKeys.arrayIndex(s);
                if (idx >= 0) {
                    if (idx < dense.length) {
                        dense[(int) idx] = null;
                    }
                    return true;
                }
            }
        }
        return super.delete(key);
    }

    /**
     * Lists own keys: indices ascending, "length", other strings, symbols.
     * <p>
     * 자체 키를 나열한다: 인덱스 오름차순, "length", 나머지 문자열, 심볼.
     *
     * @return keys / 키 목록
     */
    public List<Object> ownKeys() {
        List<Object> base = super.ownKeys();
        List<Object> result = new ArrayList<Object>();
        int pos = 0;
        if (!sparse) {
            // 조밀 모드: 빈 칸이 아닌 인덱스를 차례로 나열한다.
            long limit = Math.min((long) dense.length, length);
            for (int i = 0; i < limit; i++) {
                if (dense[i] != null) {
                    result.add(PropertyKeys.fromIndex(i));
                }
            }
        } else {
            // 희소 모드: 일반 프로퍼티 표의 인덱스 키가 앞에 온다.
            while (pos < base.size() && base.get(pos) instanceof String && PropertyKeys.arrayIndex((String) base.get(pos)) >= 0) {
                result.add(base.get(pos));
                pos++;
            }
        }
        result.add("length");
        while (pos < base.size()) { // 남은 키를 순서대로 옮긴다.
            result.add(base.get(pos));
            pos++;
        }
        return result;
    }

    /**
     * Prevents extensions; dense arrays stay valid because existing elements remain writable.
     * <p>
     * 확장을 막는다. 기존 요소는 계속 쓸 수 있으므로 조밀 배열도 유효하다.
     *
     * @return true / true
     */
    public boolean preventExtensions() {
        return super.preventExtensions();
    }
}
