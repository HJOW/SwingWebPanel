package org.duckdns.hjow.swingwebpanel.dom;

/**
 * One item of a data transfer (HTML <code>DataTransferItem</code>): its kind (this component only has text items, kind
 * <code>string</code>), its type string and its data. An item stays tied to its {@link ClipboardData}; once it is removed from it, or the
 * data transfer is disabled, the item reads as disabled (empty kind and type).
 * <p>
 * 데이터 전달 객체의 항목 하나(HTML <code>DataTransferItem</code>): 종류(이 컴포넌트는 텍스트 항목, 종류 <code>string</code> 만 가진다), 타입 문자열, 데이터.
 * 항목은 자기 {@link ClipboardData} 에 묶여 있으며, 거기서 제거되었거나 데이터 전달 객체가 무효화되면 비활성(빈 종류와 타입)으로 읽힌다.
 */
public final class DataTransferItem {

    /**
     * The data transfer that holds this item, or null once it was removed.
     * <p>
     * 이 항목을 가진 데이터 전달 객체. 제거되었으면 null.
     */
    private ClipboardData owner;

    /**
     * The type string (lower case).
     * <p>
     * 타입 문자열(소문자).
     */
    private final String type;

    /**
     * The text data.
     * <p>
     * 텍스트 데이터.
     */
    private String data;

    /**
     * Creates an item (called by {@link ClipboardData}).
     * <p>
     * 항목을 만든다({@link ClipboardData} 가 부른다).
     *
     * @param owner the data transfer / 데이터 전달 객체
     * @param type  the type string / 타입 문자열
     * @param data  the data / 데이터
     */
    DataTransferItem(ClipboardData owner, String type, String data) {
        this.owner = owner;
        this.type = type;
        this.data = data;
    }

    /**
     * Returns the type string of the item (the data of <code>type</code>), the same value that the data transfer keeps for it.
     * <p>
     * 항목의 타입 문자열을 반환한다(<code>type</code>). 데이터 전달 객체가 그 항목에 대해 가진 값과 같다.
     *
     * @return the type, or an empty text when the item is disabled / 타입. 비활성이면 빈 문자열
     */
    public String getType() {
        return isEnabled() ? type : "";
    }

    /**
     * Returns the kind of the item: <code>string</code> (the only kind that this component has), or an empty text when the item is disabled.
     * <p>
     * 항목의 종류를 반환한다: <code>string</code>(이 컴포넌트가 가진 유일한 종류). 항목이 비활성이면 빈 문자열.
     *
     * @return the kind / 종류
     */
    public String getKind() {
        return isEnabled() ? "string" : "";
    }

    /**
     * Tells whether the item can be read (its data transfer is in the read/write or read-only mode and still holds it).
     * <p>
     * 항목을 읽을 수 있는지(데이터 전달 객체가 읽기/쓰기나 읽기 전용 모드이고 아직 이 항목을 가지고 있는지) 알려 준다.
     *
     * @return true if readable / 읽을 수 있으면 true
     */
    public boolean isReadable() {
        return owner != null && (owner.getMode() == ClipboardData.READ_WRITE || owner.getMode() == ClipboardData.READ_ONLY);
    }

    /**
     * Tells whether the item is not disabled (its data transfer is still associated with a drag data store and still holds the item).
     * <p>
     * 항목이 비활성이 아닌지(데이터 전달 객체가 아직 드래그 데이터 저장소와 연결되어 있고 이 항목을 가지고 있는지) 알려 준다.
     *
     * @return true if enabled / 활성이면 true
     */
    public boolean isEnabled() {
        return owner != null && owner.getMode() != ClipboardData.DISABLED;
    }

    /**
     * Returns the data of the item for <code>getAsString</code>: null unless the item is readable.
     * <p>
     * <code>getAsString</code> 을 위해 항목의 데이터를 반환한다. 읽을 수 없으면 null.
     *
     * @return the data or null / 데이터. 없으면 null
     */
    public String getAsString() {
        return isReadable() ? data : null;
    }

    /**
     * Returns the file of the item (<code>getAsFile</code>): this component has text items only, so there is never a file.
     * <p>
     * 항목의 파일을 반환한다(<code>getAsFile</code>): 이 컴포넌트는 텍스트 항목만 가지므로 파일은 없다.
     *
     * @return null / null
     */
    public Object getAsFile() {
        return null;
    }

    /**
     * Returns the data of the item without checking the mode (for the component itself).
     * <p>
     * 모드를 확인하지 않고 항목의 데이터를 반환한다(컴포넌트 자신용).
     *
     * @return the data / 데이터
     */
    String rawData() {
        return data;
    }

    /**
     * Replaces the data of the item (called by {@link ClipboardData}).
     * <p>
     * 항목의 데이터를 바꾼다({@link ClipboardData} 가 부른다).
     *
     * @param text the data / 데이터
     */
    void replaceData(String text) {
        this.data = text;
    }

    /**
     * Detaches the item (it was removed from its data transfer).
     * <p>
     * 항목을 뗀다(데이터 전달 객체에서 제거되었다).
     */
    void detach() {
        this.owner = null;
    }

    /**
     * Returns the type string without checking the mode (for the component itself).
     * <p>
     * 모드를 확인하지 않고 타입 문자열을 반환한다(컴포넌트 자신용).
     *
     * @return the type / 타입
     */
    String rawType() {
        return type;
    }
}
