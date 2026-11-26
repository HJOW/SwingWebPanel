package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone.Cell;

/**
 * Reads structured clone records as Java host data without resolving them in a script realm.
 * <p>
 * 스크립트 렐름에 복원하는 과정 없이 구조화 복제 기록을 Java 호스트 데이터로 읽는다.
 */
final class HostMessageReader {

    /** Serialized cells from a single message. / 메시지 하나에서 직렬화한 칸. */
    private final List<Cell> cells;

    /** Converted object for each finished cell. / 완료된 칸별 변환 객체. */
    private final Object[] values;

    /** Cells on the current branch, used to detect cycles. / 순환을 찾을 때 쓰는 현재 경로의 칸. */
    private final boolean[] active;

    /** Number of host data and size units charged so far. / 지금까지 사용한 호스트 데이터·크기 단위 수. */
    private int units;

    /**
     * Allocates the per-message reading state.
     * <p>
     * 메시지 하나의 읽기 상태를 할당한다.
     *
     * @param cells the serialized cells / 직렬화한 칸
     */
    private HostMessageReader(List<Cell> cells) {
        this.cells = cells;
        values = new Object[cells.size()];
        active = new boolean[cells.size()];
    }

    /**
     * Converts the graph and accounts for transferred buffers outside the root.
     * <p>
     * 그래프를 변환하고 루트에서 참조하지 않는 전달 버퍼도 크기에 포함한다.
     *
     * @param root the serialized root / 직렬화한 루트
     * @param cells the serialized cells / 직렬화한 칸
     * @return Java host data / Java 호스트 데이터
     */
    static Object read(Object root, List<Cell> cells) {
        HostMessageReader reader = new HostMessageReader(cells);
        Object data = reader.convert(root, 0);
        // 루트에서 사용하지 않은 전달 버퍼도 분리하기 전에 크기를 검사한다.
        for (int i = 0; i < cells.size(); i++) {
            if (reader.values[i] == null) {
                reader.charge(1L + cells.get(i).bytes.length);
            }
        }
        return data;
    }

    /**
     * Checks that further data can fit in the message.
     * <p>
     * 추가 데이터가 메시지 한도 안에 들어가는지 검사한다.
     *
     * @param amount the additional units / 추가 단위 수
     */
    private void checkRoom(long amount) {
        if (amount > HostMessageValues.MAX_MESSAGE_UNITS - units) {
            throw new StructuredCloneException("Web message is too large");
        }
    }

    /**
     * Charges copied data before allocating its result.
     * <p>
     * 결과를 할당하기 전에 복사할 데이터의 크기를 더한다.
     *
     * @param amount the additional units / 추가 단위 수
     */
    private void charge(long amount) {
        checkRoom(amount);
        units += (int) amount;
    }

    /**
     * Resolves an inline value or a cell reference.
     * <p>
     * 바로 담긴 값이나 칸 참조를 해석한다.
     *
     * @param serialized the inline value or an Integer cell number / 값 또는 Integer 칸 번호
     * @param depth branching depth / 현재 중첩
     * @return the Java value / Java 값
     */
    private Object convert(Object serialized, int depth) {
        charge(1);
        if (serialized == StructuredClone.UNDEFINED) {
            return JsEngine.UNDEFINED;
        }
        if (serialized == StructuredClone.NULL) {
            return null;
        }
        if (serialized instanceof String) {
            charge(((String) serialized).length());
        } else if (serialized instanceof BigInteger) {
            charge((((BigInteger) serialized).bitLength() + 8L) / 8L);
        } else if (serialized instanceof Integer) {
            int index = ((Integer) serialized).intValue();
            if (depth >= HostMessageValues.MAX_DEPTH) {
                throw new StructuredCloneException("Web message is nested too deeply");
            }
            if (active[index]) {
                throw new StructuredCloneException("A web message cannot contain a cycle");
            }
            // 완료된 칸은 Java 객체 하나로 공유하고 처리 중인 칸을 다시 만나면 순환으로 거부한다.
            if (values[index] == null) {
                active[index] = true;
                values[index] = convertCell(cells.get(index), depth + 1);
                active[index] = false;
            }
            return values[index];
        }
        return serialized;
    }

    /**
     * Creates the Java representation of an object cell.
     * <p>
     * 객체 칸의 Java 표현을 만든다.
     *
     * @param cell the source cell / 원본 칸
     * @param depth branching depth / 현재 중첩
     * @return the Java representation / Java 표현
     */
    private Object convertCell(Cell cell, int depth) {
        switch (cell.kind) {
        case StructuredClone.ARRAY:
            return copyArray(cell, depth);
        case StructuredClone.OBJECT:
            Map<String, Object> object = new LinkedHashMap<String, Object>();
            // 구조화 복제의 자체 키 순서로 Java 맵을 만든다.
            for (int i = 0; i < cell.keys.size(); i++) {
                String key = (String) cell.keys.get(i);
                charge(key.length());
                object.put(key, convert(cell.values.get(i), depth));
            }
            return Collections.unmodifiableMap(object);
        case StructuredClone.MAP:
            return Collections.unmodifiableMap(copyCollection(cell, depth));
        case StructuredClone.SET:
            return Collections.unmodifiableSet(copyCollection(cell, depth).keySet());
        case StructuredClone.DATE:
            if (Double.isNaN(cell.number)) {
                throw new StructuredCloneException("An invalid Date has no Java message value");
            }
            return new Date((long) cell.number);
        case StructuredClone.BOOLEAN_OBJECT:
            return new Boolean(cell.flag);
        case StructuredClone.NUMBER_OBJECT:
            return new Double(cell.number);
        case StructuredClone.BIGINT_OBJECT:
            charge(cell.text.length());
            return new BigInteger(cell.text);
        case StructuredClone.STRING_OBJECT:
            charge(cell.text.length());
            return new String(cell.text);
        case StructuredClone.BUFFER:
            charge(cell.bytes.length);
            return cell.bytes.clone();
        case StructuredClone.TYPED:
        case StructuredClone.VIEW:
            return copyView(cell, depth);
        default:
            throw new StructuredCloneException("The structured clone type is not supported by Java web messages");
        }
    }

    /**
     * Builds a dense read-only list from indexed array properties and fills holes with undefined.
     * <p>
     * 배열의 인덱스 프로퍼티를 읽기 전용 리스트로 만들고 빈 칸을 undefined로 채운다.
     *
     * @param cell the serialized array / 직렬화한 배열
     * @param depth branching depth / 현재 중첩
     * @return the Java list / Java 리스트
     */
    private List<Object> copyArray(Cell cell, int depth) {
        checkRoom(cell.arrayLength);
        int length = (int) cell.arrayLength;
        int holes = length;
        List<Object> array = new ArrayList<Object>(Collections.nCopies(length, JsEngine.UNDEFINED));
        // 인덱스는 리스트에 넣고 인덱스 외 자체 값은 변환만 해 중첩·순환 제약을 확인한다.
        for (int i = 0; i < cell.keys.size(); i++) {
            String key = (String) cell.keys.get(i);
            long index = PropertyKeys.arrayIndex(key);
            Object value = convert(cell.values.get(i), depth);
            if (index >= 0 && index < length) {
                array.set((int) index, value);
                holes--;
            } else {
                charge(key.length());
            }
        }
        charge(holes);
        return Collections.unmodifiableList(array);
    }

    /**
     * Copies a script Map or the members of a Set in their stored order.
     * <p>
     * 스크립트 Map 또는 Set의 원소를 저장한 순서대로 복사한다.
     *
     * @param cell map or set / 맵 또는 집합
     * @param depth branching depth / 현재 중첩
     * @return the Java collection storage / Java 컬렉션 저장소
     */
    private HostCollectionMap copyCollection(Cell cell, int depth) {
        HostCollectionMap out = new HostCollectionMap();
        boolean map = cell.kind == StructuredClone.MAP;
        int stride = map ? 2 : 1;
        // Map의 키·값 쌍 또는 Set의 원소를 담으며 원본의 객체 키 정체성을 보존한다.
        for (int i = 0; i < cell.values.size(); i += stride) {
            Object key = cell.values.get(i);
            Object converted = convert(key, depth);
            Object value = map ? convert(cell.values.get(i + 1), depth) : null;
            out.append(converted, value, key instanceof Integer);
        }
        return out;
    }

    /**
     * Copies only a view's valid bytes, after resolving its backing buffer.
     * <p>
     * 뷰의 저장 버퍼를 해석한 뒤 유효 바이트 구간을 복사한다.
     *
     * @param cell a typed array or DataView / 형식화 배열 또는 DataView
     * @param depth branching depth / 현재 중첩
     * @return the view's copied bytes / 뷰의 바이트 사본
     */
    private byte[] copyView(Cell cell, int depth) {
        byte[] buffer = (byte[]) convert(Integer.valueOf(cell.bufferRef), depth);
        int width = cell.kind == StructuredClone.VIEW ? 1 : StructuredClone.elementType(cell.text).getSize();
        int available = buffer.length - cell.offset;
        long declared = cell.length < 0 ? (long) Math.max(0, available / width) * width : (long) cell.length * width;
        // 전달 중 버퍼 크기가 줄어 유효 구간을 잃은 뷰는 빈 바이트 배열로 나타낸다.
        if (available < declared) {
            return new byte[0];
        }
        int length = (int) declared;
        charge(length);
        return Arrays.copyOfRange(buffer, cell.offset, cell.offset + length);
    }
}
