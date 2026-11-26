package org.duckdns.hjow.swingwebpanel;

import java.lang.ref.WeakReference;
import java.util.EventObject;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.HostMessageValues;

/**
 * A copied message sent by a document through its Java host window.
 * The document URL and origin are captured at send time. The component and reply target may be collected independently.
 * <p>
 * 문서가 Java 호스트 창으로 보낸 복사 메시지. 송신 시의 문서 주소와 오리진을 보관한다.
 * 컴포넌트와 응답 대상은 메시지와 독립적으로 수거될 수 있다.
 */
public final class WebMessageEvent extends EventObject {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** Kind of browsing context that sent the message. / 메시지를 보낸 브라우징 문맥의 종류. */
    public enum SourceKind {
        /** The top-level document. / 최상위 문서. */
        DOCUMENT,
        /** A document in a nested frame. / 중첩 프레임의 문서. */
        FRAME
    }

    /**
     * Internal endpoint for a copied reply. An endpoint supplied by the browser holds its document weakly.
     * <p>
     * 복사한 응답의 내부 수신점. 브라우저가 제공하는 수신점은 문서를 약하게 보관한다.
     */
    public interface Reply {
        /**
         * Queues the reply if the original document is still accessible.
         * <p>
         * 원래 문서에 여전히 접근할 수 있으면 응답을 예약한다.
         *
         * @param message captured reply value / 복사한 응답 값
         */
        void send(ClonedValue message);
    }

    /** Weak Swing source. / 약한 Swing 원천. */
    private final transient WeakReference<SwingWebPanel> panel;
    /** Java copy of the message. / 메시지의 Java 복사본. */
    private final Object data;
    /** Sender origin at posting time. / 발신 시의 오리진. */
    private final String origin;
    /** Sender document URL at posting time. / 발신 시의 문서 URL. */
    private final String documentUrl;
    /** Sending context kind. / 송신 문맥 종류. */
    private final SourceKind sourceKind;
    /** Sequence assigned by the component. / 컴포넌트가 부여한 일련번호. */
    private final long sequence;
    /** UTC epoch milliseconds when accepted. / 메시지를 접수한 UTC 에포크 밀리초. */
    private final long timestamp;
    /** Weak document reply endpoint. / 약한 문서 응답 수신점. */
    private final transient Reply reply;

    /**
     * Creates a delivered message from the captured data and metadata.
     * <p>
     * 복사한 데이터와 메타데이터로 수신 메시지를 만든다.
     *
     * @param source Swing component / Swing 컴포넌트
     * @param data copied message / 복사한 메시지
     * @param origin sender origin / 송신 오리진
     * @param documentUrl sender URL / 송신 문서 URL
     * @param sourceKind sender context kind / 송신 문맥 종류
     * @param sequence component sequence / 컴포넌트 일련번호
     * @param timestamp epoch milliseconds / 에포크 밀리초
     * @param reply weak document endpoint / 문서의 약한 응답 수신점
     */
    public WebMessageEvent(SwingWebPanel source, Object data, String origin, String documentUrl, SourceKind sourceKind,
            long sequence, long timestamp, Reply reply) {
        super(source);
        panel = new WeakReference<SwingWebPanel>(source);
        this.source = null;
        this.data = data;
        this.origin = origin;
        this.documentUrl = documentUrl;
        this.sourceKind = sourceKind;
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.reply = reply;
    }

    /** Returns the Swing component while it is reachable. / 도달 가능한 동안 Swing 컴포넌트를 반환한다. */
    @Override
    public SwingWebPanel getSource() {
        return panel == null ? null : panel.get();
    }

    /** Returns the copied Java data. / 복사된 Java 데이터를 반환한다. */
    public Object getData() {
        return data;
    }

    /** Returns the origin or the string `"null"` for an opaque origin. / 오리진 또는 불투명 오리진을 나타내는 문자열 `"null"`을 반환한다. */
    public String getOrigin() {
        return origin;
    }

    /** Returns the document URL at send time. / 발신 시 문서 URL을 반환한다. */
    public String getDocumentUrl() {
        return documentUrl;
    }

    /** Returns the kind of sending context. / 송신 문맥 종류를 반환한다. */
    public SourceKind getSourceKind() {
        return sourceKind;
    }

    /** Returns the component's sequence number. / 컴포넌트 일련번호를 반환한다. */
    public long getSequence() {
        return sequence;
    }

    /** Returns the send timestamp in UTC epoch milliseconds. / 발신 시각을 UTC 에포크 밀리초로 반환한다. */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Copies and replies to the sending document. Call from any thread.
     * Check the sender origin and data before replying. An expired document discards the reply.
     * <p>
     * 값을 복사해 보낸 문서에 응답한다. 어느 스레드에서나 호출할 수 있다.
     * 응답을 보내기 전에 발신 오리진과 데이터를 확인한다. 문서 수명이 끝났으면 응답을 버린다.
     *
     * @param message plain Java data / Java 데이터
     * @throws IllegalArgumentException if the message cannot be copied / 메시지를 복사할 수 없으면
     */
    public void reply(Object message) {
        ClonedValue data = HostMessageValues.fromJava(message);
        if (reply != null) {
            reply.send(data);
        }
    }
}
