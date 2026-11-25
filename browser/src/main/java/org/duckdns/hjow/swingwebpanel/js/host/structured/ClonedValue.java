package org.duckdns.hjow.swingwebpanel.js.host.structured;

/**
 * The serialized form of a script value made by {@link StructuredClone#serialize}: an immutable tree of Java data that belongs to no
 * realm. Primitive values are stored as Java values, objects as numbered cells (so cycles and shared references need no Java object
 * cycles). Ordinary data can be stored for later deserialization. A shared buffer cell holds a reference to shared storage
 * and requires a compatible receiving scope.
 * <p>
 * {@link StructuredClone#serialize} 가 만든 스크립트 값의 직렬화된 형태: 어떤 realm 에도 속하지 않는 Java 데이터의 불변 트리. 원시 값은 Java 값으로,
 * 객체는 번호가 붙은 칸으로 저장한다. 일반 데이터는 나중에 역직렬화할 수 있다.
 * 공유 버퍼 칸은 공유 저장소 참조를 간직하며 호환되는 수신 범위에서만 복원할 수 있다.
 */
public final class ClonedValue {

    /**
     * The root: <code>StructuredClone.UNDEFINED</code>, <code>StructuredClone.NULL</code>, a Boolean, a Double, a BigInteger, a String, or an Integer
     * (the number of a cell).
     * <p>
     * 루트: <code>StructuredClone.UNDEFINED</code>, <code>StructuredClone.NULL</code>, Boolean, Double, BigInteger, String, 또는 Integer(칸 번호).
     */
    final Object root;

    /**
     * The cells of all objects, in order of discovery.
     * <p>
     * 모든 객체의 칸(발견한 순서).
     */
    final StructuredClone.Cell[] cells;

    /** Sending scope if the value contains shared memory. / 값에 공유 메모리가 있으면 보낸 쪽 범위. */
    final SharedMemoryScope sharedScope;

    /**
     * The estimated size in bytes.
     * <p>
     * 추정 크기(바이트).
     */
    private final long size;

    /**
     * Creates the serialized form.
     * <p>
     * 직렬화된 형태를 만든다.
     *
     * @param root  the root / 루트
     * @param cells the cells / 칸
     * @param size  the estimated size / 추정 크기
     * @param sharedScope the sending scope or null for ordinary data / 보낸 쪽 범위. 일반 데이터이면 null
     */
    ClonedValue(Object root, StructuredClone.Cell[] cells, long size, SharedMemoryScope sharedScope) {
        this.root = root;
        this.cells = cells;
        this.size = size;
        this.sharedScope = sharedScope;
    }

    /**
     * Returns the estimated size of the data in bytes (strings count two bytes per character).
     * <p>
     * 데이터의 추정 크기(바이트)를 반환한다(문자열은 문자당 2바이트).
     *
     * @return the size / 크기
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the number of objects in the serialized form.
     * <p>
     * 직렬화된 형태에 있는 객체 수를 반환한다.
     *
     * @return the number of cells / 칸 수
     */
    public int getObjectCount() {
        return cells.length;
    }

    /**
     * Tells whether the value is a primitive (it has no object cells).
     * <p>
     * 값이 원시 값인지(객체 칸이 없는지) 알려 준다.
     *
     * @return true for a primitive / 원시 값이면 true
     */
    public boolean isPrimitive() {
        return !(root instanceof Integer);
    }

    /**
     * Describes the value for debugging.
     * <p>
     * 디버깅용으로 값을 설명한다.
     *
     * @return a short text / 짧은 문자열
     */
    public String toString() {
        return "ClonedValue[" + (isPrimitive() ? String.valueOf(root) : cells.length + " objects") + ", " + size + " bytes]";
    }
}
