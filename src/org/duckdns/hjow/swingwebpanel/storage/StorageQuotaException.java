package org.duckdns.hjow.swingwebpanel.storage;

/**
 * Thrown when a storage limit would be exceeded (see {@link StorageLimits}). The JavaScript layer converts it into a
 * <code>QuotaExceededError</code> DOMException.
 * <p>
 * 저장소 제한을 넘게 될 때 던진다({@link StorageLimits} 참조). JavaScript 계층은 이를 <code>QuotaExceededError</code> DOMException 으로 바꾼다.
 */
public class StorageQuotaException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message description / 설명
     */
    public StorageQuotaException(String message) {
        super(message);
    }
}
