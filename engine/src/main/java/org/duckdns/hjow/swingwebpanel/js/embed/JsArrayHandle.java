package org.duckdns.hjow.swingwebpanel.js.embed;

import java.util.AbstractList;
import java.util.RandomAccess;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * A live view of a script array as a <code>List&lt;Object&gt;</code>. The size is the <code>length</code> of the array; reading and writing an
 * element reads and writes the array; <code>add</code> and <code>remove</code> change the array the way <code>push</code> and
 * <code>splice</code> do. Holes of a sparse array read as <code>JsEngine.UNDEFINED</code>. Elements follow the conversion rules of
 * {@link JsEngine}. Two handles are equal when they refer to the same script array; the <code>equals</code>, <code>hashCode</code> and
 * <code>toString</code> of the <code>List</code> interface are not used because they would not terminate for an array that contains itself.
 * <p>
 * 스크립트 배열을 <code>List&lt;Object&gt;</code> 로 보여 주는 살아 있는 보기. 크기는 배열의 <code>length</code> 이고, 요소를 읽고 쓰면 배열을 읽고 쓰며,
 * <code>add</code> 와 <code>remove</code> 는 <code>push</code> 와 <code>splice</code> 처럼 배열을 바꾼다. 희소 배열의 구멍은
 * <code>JsEngine.UNDEFINED</code> 로 읽힌다. 요소는 {@link JsEngine} 의 변환 규칙을 따른다. 핸들 두 개는 같은 스크립트 배열을 가리킬 때 같다. 자기 자신을
 * 포함한 배열에서는 끝나지 않으므로 <code>List</code> 인터페이스의 <code>equals</code>, <code>hashCode</code>, <code>toString</code> 은 쓰지 않는다.
 */
public final class JsArrayHandle extends AbstractList<Object> implements JsHandle, RandomAccess {

    /**
     * The engine that owns the array.
     * <p>
     * 배열을 가진 엔진.
     */
    private final JsEngine engine;

    /**
     * The script array.
     * <p>
     * 스크립트 배열.
     */
    private final JsObject array;

    /**
     * Creates a handle (use {@link JsEngine#newArray()} or convert a script value to get one).
     * <p>
     * 핸들을 만든다({@link JsEngine#newArray()} 를 쓰거나 스크립트 값을 바꿔 얻는다).
     *
     * @param engine the engine / 엔진
     * @param array  the script array / 스크립트 배열
     */
    JsArrayHandle(JsEngine engine, JsObject array) {
        this.engine = engine;
        this.array = array;
    }

    /**
     * Returns the engine that owns the array.
     * <p>
     * 배열을 가진 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    public JsEngine getEngine() {
        return engine;
    }

    /**
     * Returns the script array itself.
     * <p>
     * 스크립트 배열 자체를 반환한다.
     *
     * @return the array / 배열
     */
    public JsObject getJsObject() {
        return array;
    }

    /**
     * Tells whether the array is callable (never).
     * <p>
     * 배열이 호출 가능한지 알려 준다(항상 false).
     *
     * @return false / false
     */
    public boolean isCallable() {
        return false;
    }

    /**
     * Calls a method of the array, for example <code>join</code>: <code>array[name](args)</code>.
     * <p>
     * 배열의 메서드를 호출한다. 예: <code>join</code>. <code>array[name](args)</code>.
     *
     * @param name the method name / 메서드 이름
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    public Object callMember(String name, Object... args) {
        return engine.invokeMethod(this, name, args);
    }

    /**
     * Returns the length of the array.
     * <p>
     * 배열의 길이를 반환한다.
     *
     * @return the number of elements (at most Integer.MAX_VALUE) / 요소 수(최대 Integer.MAX_VALUE)
     */
    public int size() {
        synchronized (engine) {
            try {
                double n = Ops.lengthOf(array);
                return n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Reads an element.
     * <p>
     * 요소를 읽는다.
     *
     * @param index the index / 인덱스
     * @return the element / 요소
     * @throws IndexOutOfBoundsException if the index is outside the array / 인덱스가 배열 밖이면
     */
    public Object get(int index) {
        synchronized (engine) {
            try {
                check(index, size());
                return JavaValues.toJava(engine, array.get(PropertyKeys.fromIndex(index)));
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Replaces an element.
     * <p>
     * 요소를 바꾼다.
     *
     * @param index   the index / 인덱스
     * @param element the new element / 새 요소
     * @return the previous element / 이전 요소
     * @throws IndexOutOfBoundsException if the index is outside the array / 인덱스가 배열 밖이면
     */
    public Object set(int index, Object element) {
        synchronized (engine) {
            try {
                Object previous = get(index);
                array.setStrict(PropertyKeys.fromIndex(index), JavaValues.toJs(engine, element));
                return previous;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Inserts an element (like <code>splice(index, 0, element)</code>, or <code>push</code> at the end).
     * <p>
     * 요소를 삽입한다(<code>splice(index, 0, element)</code> 처럼. 끝이면 <code>push</code>).
     *
     * @param index   the index, 0 to size() / 인덱스. 0 이상 size() 이하
     * @param element the element / 요소
     * @throws IndexOutOfBoundsException if the index is outside 0..size() / 인덱스가 0..size() 밖이면
     */
    public void add(int index, Object element) {
        synchronized (engine) {
            try {
                int n = size();
                if (index < 0 || index > n) {
                    throw new IndexOutOfBoundsException("index " + index + ", size " + n);
                }
                JsValue v = JavaValues.toJs(engine, element);
                // 끝이면 push, 중간이면 splice 로 넣는다.
                if (index == n) {
                    engine.invokeValue(array, "push", new JsValue[] { v });
                } else {
                    engine.invokeValue(array, "splice", new JsValue[] { Js.num(index), Js.num(0), v });
                }
                modCount++;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Removes an element (like <code>splice(index, 1)</code>).
     * <p>
     * 요소를 제거한다(<code>splice(index, 1)</code> 처럼).
     *
     * @param index the index / 인덱스
     * @return the removed element / 제거한 요소
     * @throws IndexOutOfBoundsException if the index is outside the array / 인덱스가 배열 밖이면
     */
    public Object remove(int index) {
        synchronized (engine) {
            try {
                Object previous = get(index);
                engine.invokeValue(array, "splice", new JsValue[] { Js.num(index), Js.num(1) });
                modCount++;
                return previous;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Removes all elements (sets the length to 0).
     * <p>
     * 모든 요소를 제거한다(길이를 0 으로 한다).
     */
    public void clear() {
        synchronized (engine) {
            try {
                array.setStrict("length", Js.num(0));
                modCount++;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Checks an index.
     * <p>
     * 인덱스를 검사한다.
     *
     * @param index the index / 인덱스
     * @param size  the size / 크기
     * @throws IndexOutOfBoundsException if the index is outside 0..size()-1 / 인덱스가 0..size()-1 밖이면
     */
    private static void check(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
    }

    /**
     * Tells whether another object is a handle of the same script array.
     * <p>
     * 다른 객체가 같은 스크립트 배열의 핸들인지 알려 준다.
     *
     * @param o the other object / 다른 객체
     * @return true if both refer to the same script array / 같은 스크립트 배열을 가리키면 true
     */
    public boolean equals(Object o) {
        return o instanceof JsHandle && ((JsHandle) o).getJsObject() == array;
    }

    /**
     * Returns a hash code of the script array's identity.
     * <p>
     * 스크립트 배열의 동일성에 대한 해시 코드를 반환한다.
     *
     * @return the hash code / 해시 코드
     */
    public int hashCode() {
        return System.identityHashCode(array);
    }

    /**
     * Describes the array (without running script code).
     * <p>
     * 배열을 설명한다(스크립트 코드를 실행하지 않는다).
     *
     * @return the description / 설명
     */
    public String toString() {
        return "[script Array]";
    }
}
