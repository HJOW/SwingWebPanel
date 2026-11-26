package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.date.DateMath;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHandle;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone.Cell;

/**
 * Copies host message data without a script realm or a reference to a live Java value.
 * <p>
 * 스크립트 렐름이나 살아 있는 Java 객체 참조를 두지 않고 호스트 메시지 데이터를 복사한다.
 */
public final class HostMessageValues {

    /** Sum of values, string units and binary bytes permitted per message. / 메시지 하나에 허용하는 값·문자 단위·바이트의 합. */
    public static final int MAX_MESSAGE_UNITS = 262144;

    /** Maximum nesting along one branch. / 한 갈래에서 허용하는 최대 중첩. */
    static final int MAX_DEPTH = 128;

    /** Uses the static boundary methods. / 정적 경계 메서드로 사용한다. */
    private HostMessageValues() {
    }

    /**
     * Captures a Java value on the caller's thread for subsequent deserialization in the receiving realm.
     * <p>
     * 호출 스레드에서 Java 값을 복사한다. 나중에 수신 렐름에서 역직렬화할 수 있다.
     *
     * @param message the supported Java value / 지원하는 Java 값
     * @return a realm-independent message / 렐름에 의존하지 않는 메시지
     * @throws IllegalArgumentException for unsupported values, cycles or exceeded limits / 미지원 값, 순환 또는 한도 초과 시
     */
    public static ClonedValue fromJava(Object message) {
        JavaSnapshot snapshot = new JavaSnapshot();
        Object root = snapshot.capture(message, 0);
        Cell[] cells = snapshot.cells.toArray(new Cell[snapshot.cells.size()]);
        return new ClonedValue(root, cells, 2L * snapshot.units, null);
    }

    /**
     * Builds the serialized graph of one Java message.
     * <p>
     * Java 메시지 하나의 직렬화 그래프를 만든다.
     */
    private static final class JavaSnapshot {

        /** Cells in discovery order. / 처음 만난 순서의 칸. */
        final ArrayList<Cell> cells = new ArrayList<Cell>();

        /** Java identities; a negative index denotes a value currently being copied. / Java 정체성별 칸 번호. 음수이면 복사 중이다. */
        private final IdentityHashMap<Object, Integer> seen = new IdentityHashMap<Object, Integer>();

        /** Units already assigned to values and their data. / 값과 데이터에 이미 사용한 단위 수. */
        int units;

        /**
         * Accounts for data before including it in the snapshot.
         * <p>
         * 스냅샷에 데이터를 담기 전에 사용량을 계산한다.
         *
         * @param amount extra units / 추가 단위 수
         */
        private void charge(long amount) {
            if (amount > MAX_MESSAGE_UNITS - units) {
                throw new IllegalArgumentException("Web message is too large");
            }
            units += (int) amount;
        }

        /**
         * Converts a value to an inline primitive or a cell reference.
         * <p>
         * 값을 원시값 또는 칸 참조로 변환한다.
         *
         * @param value source value / 원본 값
         * @param depth current nesting / 현재 중첩 수준
         * @return serialized primitive or cell index / 직렬화한 원시값 또는 칸 번호
         */
        Object capture(Object value, int depth) {
            charge(1);
            if (value == null) {
                return StructuredClone.NULL;
            }
            // List·Map 인터페이스를 구현하는 살아 있는 스크립트 핸들도 일반 Java 값으로 받지 않는다.
            if (value instanceof JsValue || value instanceof JsHandle || value instanceof JsHostFunction) {
                throw new IllegalArgumentException("Live script values cannot be sent as web messages");
            }
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof BigInteger) {
                BigInteger integer = (BigInteger) value;
                if (integer.getClass() != BigInteger.class) {
                    byte[] bits = integer.toByteArray();
                    charge(bits.length);
                    return new BigInteger(bits);
                }
                charge((integer.bitLength() + 8L) / 8L);
                return integer;
            }
            if (value instanceof Number) {
                return Double.valueOf(((Number) value).doubleValue());
            }
            if (value instanceof CharSequence || value instanceof Character) {
                String text = value.toString();
                if (text == null) {
                    throw new IllegalArgumentException("A web message string must have text");
                }
                charge(text.length());
                return text;
            }
            if (depth >= MAX_DEPTH) {
                throw new IllegalArgumentException("Web message is nested too deeply");
            }
            Integer prior = seen.get(value);
            if (prior != null) {
                // 복사 중인 조상을 다시 만나면 순환이다. 완료한 칸은 같은 수신 객체를 가리킨다.
                if (prior.intValue() < 0) {
                    throw new IllegalArgumentException("A web message cannot contain a cycle");
                }
                return prior;
            }
            return captureObject(value, depth);
        }

        /**
         * Captures the supported object as a cell and assigns its shared reference number.
         * <p>
         * 지원 대상 객체를 칸에 복사하고 공유 참조 번호를 할당한다.
         *
         * @param value source object / 원본 객체
         * @param depth current nesting / 현재 중첩 수준
         * @return cell index / 칸 번호
         */
        private Integer captureObject(Object value, int depth) {
            int kind;
            // 바이트 배열은 일반 배열보다 먼저 판별한다. Java 객체의 클래스나 메서드는 전달하지 않는다.
            if (value instanceof byte[]) {
                kind = StructuredClone.BUFFER;
            } else if (value instanceof Date) {
                kind = StructuredClone.DATE;
            } else if (value instanceof Map<?, ?>) {
                kind = StructuredClone.OBJECT;
            } else if (value instanceof Collection<?> || value.getClass().isArray()) {
                kind = StructuredClone.ARRAY;
            } else {
                throw new IllegalArgumentException("Unsupported web message value: " + value.getClass().getName());
            }
            Cell cell = new Cell(kind);
            int index = cells.size();
            cells.add(cell);
            seen.put(value, Integer.valueOf(-index - 1));
            if (kind == StructuredClone.BUFFER) {
                byte[] bytes = (byte[]) value;
                charge(bytes.length);
                cell.bytes = bytes.clone();
            } else if (kind == StructuredClone.DATE) {
                long time = ((Date) value).getTime();
                if (time < -DateMath.MAX_TIME || time > DateMath.MAX_TIME) {
                    throw new IllegalArgumentException("A web message Date is outside the ECMAScript time range");
                }
                cell.number = time;
            } else if (kind == StructuredClone.OBJECT) {
                // 문자열 키를 순서대로 저장해 수신 객체의 자체 데이터 프로퍼티로 만든다.
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    Object key = entry.getKey();
                    if (!(key instanceof String)) {
                        throw new IllegalArgumentException("Web message map keys must be strings");
                    }
                    charge(((String) key).length());
                    cell.keys.add(key);
                    cell.values.add(capture(entry.getValue(), depth + 1));
                }
            } else if (value instanceof Collection<?>) {
                int i = 0;
                // 호출자가 제공하는 컬렉션을 반복 순서대로 복사한다.
                for (Object element : (Collection<?>) value) {
                    cell.keys.add(Integer.toString(i++));
                    cell.values.add(capture(element, depth + 1));
                }
                cell.arrayLength = i;
            } else {
                int length = Array.getLength(value);
                // 기본형 배열도 반사 API의 박싱 결과를 일반 원시값 처리와 공유한다.
                for (int i = 0; i < length; i++) {
                    cell.keys.add(Integer.toString(i));
                    cell.values.add(capture(Array.get(value, i), depth + 1));
                }
                cell.arrayLength = length;
            }
            Integer reference = Integer.valueOf(index);
            seen.put(value, reference);
            return reference;
        }
    }
}
