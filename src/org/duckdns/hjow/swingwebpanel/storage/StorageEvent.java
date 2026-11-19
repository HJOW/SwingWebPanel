package org.duckdns.hjow.swingwebpanel.storage;

import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * The <code>storage</code> event that a document receives when another document of the same origin changed a storage area. It carries the
 * key, the old and new values, the URL of the changing document and the storage area that changed.
 * <p>
 * 같은 오리진의 다른 문서가 저장 영역을 바꾸었을 때 문서가 받는 <code>storage</code> 이벤트. 키, 이전 값과 새 값, 변경한 문서의 URL, 바뀐 저장 영역을
 * 담고 있다.
 */
public class StorageEvent extends Event {

    /**
     * Changed key, null for clear.
     * <p>
     * 바뀐 키. clear 이면 null.
     */
    private final String key;

    /**
     * Previous value or null.
     * <p>
     * 이전 값 또는 null.
     */
    private final String oldValue;

    /**
     * New value or null.
     * <p>
     * 새 값 또는 null.
     */
    private final String newValue;

    /**
     * URL of the document that changed the storage.
     * <p>
     * 저장소를 바꾼 문서의 URL.
     */
    private final String url;

    /**
     * The storage area that changed.
     * <p>
     * 바뀐 저장 영역.
     */
    private final WebStorage storageArea;

    /**
     * Creates a storage event (type <code>storage</code>, does not bubble, not cancelable).
     * <p>
     * storage 이벤트를 만든다(종류 <code>storage</code>, 버블하지 않음, 취소 불가).
     *
     * @param key         changed key or null / 바뀐 키 또는 null
     * @param oldValue    previous value or null / 이전 값 또는 null
     * @param newValue    new value or null / 새 값 또는 null
     * @param url         URL of the changing document / 변경한 문서의 URL
     * @param storageArea the changed area / 바뀐 영역
     */
    public StorageEvent(String key, String oldValue, String newValue, String url, WebStorage storageArea) {
        super("storage", false, false);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.url = url;
        this.storageArea = storageArea;
    }

    /**
     * Returns the changed key.
     * <p>
     * 바뀐 키를 반환한다.
     *
     * @return key or null / 키 또는 null
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the previous value.
     * <p>
     * 이전 값을 반환한다.
     *
     * @return old value or null / 이전 값 또는 null
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Returns the new value.
     * <p>
     * 새 값을 반환한다.
     *
     * @return new value or null / 새 값 또는 null
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Returns the URL of the document that changed the storage.
     * <p>
     * 저장소를 바꾼 문서의 URL 을 반환한다.
     *
     * @return URL / URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the changed storage area.
     * <p>
     * 바뀐 저장 영역을 반환한다.
     *
     * @return area / 영역
     */
    public WebStorage getStorageArea() {
        return storageArea;
    }
}
