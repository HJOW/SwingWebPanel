package org.duckdns.hjow.swingwebpanel.storage;

/**
 * The size limits of web storage (plan item P16-07). Sizes are counted in UTF-16 code units of keys plus values, as web browsers do
 * (5 MiB per origin = 2.5 million code units at 2 bytes each). Exceeding a limit makes <code>setItem</code> fail with a
 * {@link StorageQuotaException}, which the JavaScript layer reports as <code>QuotaExceededError</code>; the storage is left unchanged.
 * <p>
 * 웹 저장소의 크기 제한(계획 P16-07). 크기는 웹 브라우저처럼 키와 값의 UTF-16 코드 단위 수로 센다(오리진당 5 MiB = 코드 단위당 2 바이트로
 * 250만 코드 단위). 제한을 넘으면 <code>setItem</code> 이 {@link StorageQuotaException} 으로 실패하고 JavaScript 계층은 이를
 * <code>QuotaExceededError</code> 로 보고하며, 저장소는 바뀌지 않는다.
 */
public final class StorageLimits {

    /**
     * Maximum code units (keys plus values) of one origin: 2,621,440 (5 MiB).
     * <p>
     * 한 오리진의 최대 코드 단위 수(키와 값 합계): 2,621,440(5 MiB).
     */
    public static final int MAX_ORIGIN_CHARS = 2621440;

    /**
     * Maximum number of origins that may hold data in the process-wide localStorage.
     * <p>
     * 프로세스 공용 localStorage 에 데이터를 둘 수 있는 최대 오리진 수.
     */
    public static final int MAX_ORIGINS = 1000;

    /**
     * Maximum code units of the whole process-wide localStorage (all origins): 16,777,216 (32 MiB of characters).
     * <p>
     * 프로세스 공용 localStorage 전체(모든 오리진)의 최대 코드 단위 수: 16,777,216(문자 32 MiB).
     */
    public static final int MAX_TOTAL_CHARS = 16777216;

    /**
     * Maximum number of items of one origin.
     * <p>
     * 한 오리진의 최대 항목 수.
     */
    public static final int MAX_ITEMS_PER_ORIGIN = 100000;

    /**
     * Private constructor: constants holder.
     * <p>
     * 비공개 생성자: 상수 보관용 클래스이다.
     */
    private StorageLimits() {
    }
}
