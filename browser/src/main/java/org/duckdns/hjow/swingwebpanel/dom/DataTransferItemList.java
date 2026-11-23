package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The list of the items of a data transfer (HTML <code>DataTransferItemList</code>, <code>dataTransfer.items</code>): a view of the
 * {@link ClipboardData} that made it, so it shows what the data transfer holds now, and it follows the mode of the data transfer (it can
 * be read in every mode but the disabled one, and changed in the read/write mode only).
 * <p>
 * 데이터 전달 객체의 항목 목록(HTML <code>DataTransferItemList</code>, <code>dataTransfer.items</code>): 이것을 만든 {@link ClipboardData} 의 뷰이므로 데이터 전달 객체가
 * 지금 가진 것을 보이며, 데이터 전달 객체의 모드를 따른다(무효화 모드를 빼고 읽을 수 있고, 읽기/쓰기 모드에서만 바꿀 수 있다).
 */
public final class DataTransferItemList {

    /**
     * The data transfer that this list is a view of.
     * <p>
     * 이 목록이 뷰인 데이터 전달 객체.
     */
    private final ClipboardData owner;

    /**
     * Creates the view (called by {@link ClipboardData}).
     * <p>
     * 뷰를 만든다({@link ClipboardData} 가 부른다).
     *
     * @param owner the data transfer / 데이터 전달 객체
     */
    DataTransferItemList(ClipboardData owner) {
        this.owner = owner;
    }

    /**
     * Returns the number of items (0 in the disabled mode).
     * <p>
     * 항목 수를 반환한다(무효화 모드에서는 0).
     *
     * @return the length / 길이
     */
    public int getLength() {
        return owner.getMode() == ClipboardData.DISABLED ? 0 : owner.itemCount();
    }

    /**
     * Returns the item at an index, or null.
     * <p>
     * 위치의 항목을 반환한다. 없으면 null.
     *
     * @param index the index / 위치
     * @return the item or null / 항목. 없으면 null
     */
    public DataTransferItem item(int index) {
        return index >= 0 && index < getLength() ? owner.itemAt(index) : null;
    }

    /**
     * Adds a text item (<code>items.add(data, type)</code>). Nothing is added outside the read/write mode (the result is null).
     * <p>
     * 텍스트 항목을 더한다(<code>items.add(data, type)</code>). 읽기/쓰기 모드가 아니면 아무것도 더하지 않는다(결과는 null).
     *
     * @param data the text / 텍스트
     * @param type the type string / 타입 문자열
     * @return the new item, or null / 새 항목. 없으면 null
     * @throws DomException <code>NotSupportedError</code> if an item of this type exists / 같은 타입의 항목이 있으면 <code>NotSupportedError</code>
     */
    public DataTransferItem add(String data, String type) {
        if (owner.getMode() != ClipboardData.READ_WRITE) {
            return null;
        }
        String t = ClipboardData.canonicalType(type);
        // 같은 타입의 텍스트 항목이 이미 있으면 NotSupportedError 이다.
        if (owner.hasData(t)) {
            throw DomException.notSupported("An item already exists for type '" + t + "'.");
        }
        owner.setData(t, data);
        return owner.itemFor(t);
    }

    /**
     * Removes the item at an index (<code>items.remove(index)</code>).
     * <p>
     * 위치의 항목을 제거한다(<code>items.remove(index)</code>).
     *
     * @param index the index / 위치
     * @throws DomException <code>InvalidStateError</code> outside the read/write mode / 읽기/쓰기 모드가 아니면 <code>InvalidStateError</code>
     */
    public void remove(int index) {
        if (owner.getMode() != ClipboardData.READ_WRITE) {
            throw DomException.invalidState("The data transfer is not in the read/write mode.");
        }
        owner.removeItemAt(index);
    }

    /**
     * Removes all items (<code>items.clear()</code>); nothing happens outside the read/write mode.
     * <p>
     * 모든 항목을 제거한다(<code>items.clear()</code>). 읽기/쓰기 모드가 아니면 아무 일도 없다.
     */
    public void clear() {
        if (owner.getMode() == ClipboardData.READ_WRITE) {
            owner.clearAllItems();
        }
    }
}
