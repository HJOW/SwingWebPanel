package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * The Java side of a <code>ProgressEvent</code> (XMLHttpRequest Standard, section 5): an event that tells how many bytes of a known or
 * unknown total were transferred. The events of <code>XMLHttpRequest</code> (<code>loadstart</code>, <code>progress</code>, <code>load</code>,
 * <code>error</code>, <code>abort</code>, <code>timeout</code>, <code>loadend</code>) use this class; scripts can also create their own.
 * <p>
 * <code>ProgressEvent</code> 의 Java 쪽(XMLHttpRequest 표준 5절): 알려진 또는 모르는 전체 중 몇 바이트를 전송했는지 알리는 이벤트.
 * <code>XMLHttpRequest</code> 의 이벤트(<code>loadstart</code>, <code>progress</code>, <code>load</code>, <code>error</code>, <code>abort</code>,
 * <code>timeout</code>, <code>loadend</code>)가 이 클래스를 쓰며 스크립트가 직접 만들 수도 있다.
 */
public final class ProgressEvent extends Event {

    /** True if the total is known. / 전체 크기를 알면 true. */
    private final boolean lengthComputable;

    /** The transferred bytes. / 전송한 바이트. */
    private final double loaded;

    /** The total bytes, 0 if unknown. / 전체 바이트. 모르면 0. */
    private final double total;

    /**
     * Creates a progress event.
     * <p>
     * 진행 이벤트를 만든다.
     *
     * @param type             the event type / 이벤트 종류
     * @param bubbles          whether it bubbles / 버블링 여부
     * @param cancelable       whether it can be cancelled / 취소 가능 여부
     * @param lengthComputable whether the total is known / 전체 크기를 아는지
     * @param loaded           the transferred bytes / 전송한 바이트
     * @param total            the total bytes / 전체 바이트
     */
    public ProgressEvent(String type, boolean bubbles, boolean cancelable, boolean lengthComputable, double loaded, double total) {
        super(type, bubbles, cancelable);
        this.lengthComputable = lengthComputable;
        this.loaded = loaded;
        this.total = total;
    }

    /**
     * Tells whether the total is known.
     * <p>
     * 전체 크기를 아는지 알려 준다.
     *
     * @return true if known / 알면 true
     */
    public boolean isLengthComputable() {
        return lengthComputable;
    }

    /**
     * Returns the transferred bytes.
     * <p>
     * 전송한 바이트를 반환한다.
     *
     * @return the bytes / 바이트
     */
    public double getLoaded() {
        return loaded;
    }

    /**
     * Returns the total bytes.
     * <p>
     * 전체 바이트를 반환한다.
     *
     * @return the bytes, 0 if unknown / 바이트. 모르면 0
     */
    public double getTotal() {
        return total;
    }
}
