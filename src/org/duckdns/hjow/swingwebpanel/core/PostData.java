package org.duckdns.hjow.swingwebpanel.core;

/**
 * The request body of a form submission that made a page (<code>POST</code>), kept with the page so that the page can be shown again
 * without sending the form a second time (history moves use the stored source) and sent again only after the user agreed (refresh).
 * A body larger than {@link #MAX_RETAINED_BYTES} is not kept; such a page can be shown again from its stored source but never be sent
 * again. Instances are immutable.
 * <p>
 * 페이지를 만든 폼 제출(<code>POST</code>)의 요청 본문. 페이지와 함께 보관하여, 폼을 다시 보내지 않고 페이지를 다시 보여 주고(이력 이동은 저장한 원문을
 * 쓴다) 사용자가 동의한 뒤에만 다시 보낼 수 있게 한다(새로고침). {@link #MAX_RETAINED_BYTES} 보다 큰 본문은 보관하지 않으며, 그런 페이지는 저장한
 * 원문으로 다시 보여 줄 수는 있어도 다시 보낼 수는 없다. 인스턴스는 불변이다.
 */
public final class PostData {

    /**
     * The largest body that is kept for sending the form again: 4 MiB. A bigger body (for example an upload of a large file) is dropped
     * once it was sent, so that a history of many submissions does not hold large uploads in memory.
     * <p>
     * 폼을 다시 보내려고 보관하는 가장 큰 본문: 4 MiB. 더 큰 본문(예: 큰 파일 업로드)은 한 번 보낸 뒤 버리므로, 제출이 많은 이력이 큰 업로드를 메모리에
     * 붙잡지 않는다.
     */
    public static final int MAX_RETAINED_BYTES = 4 * 1024 * 1024;

    /**
     * The media type of the body, for example <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data; boundary=...</code>.
     * <p>
     * 본문의 미디어 타입. 예: <code>application/x-www-form-urlencoded</code>, <code>multipart/form-data; boundary=...</code>.
     */
    private final String contentType;

    /**
     * The body, or null if it was too large to keep.
     * <p>
     * 본문. 보관하기에 너무 컸으면 null.
     */
    private final byte[] body;

    /**
     * The address of the page that contained the form (sent as the referrer), or null.
     * <p>
     * 폼이 있던 페이지의 주소(참조 주소로 보낸다). 없으면 null.
     */
    private final String referrer;

    /**
     * Creates the data of a submission; a body over {@link #MAX_RETAINED_BYTES} is not kept.
     * <p>
     * 제출의 데이터를 만든다. {@link #MAX_RETAINED_BYTES} 보다 큰 본문은 보관하지 않는다.
     *
     * @param contentType the media type of the body / 본문의 미디어 타입
     * @param body        the body, not null / 본문(null 불가)
     * @param referrer    the address of the page with the form, or null / 폼이 있던 페이지의 주소. 없으면 null
     * @throws IllegalArgumentException if the body is null / 본문이 null 이면
     */
    public PostData(String contentType, byte[] body, String referrer) {
        if (body == null) {
            throw new IllegalArgumentException("body must not be null");
        }
        this.contentType = contentType == null ? "application/x-www-form-urlencoded" : contentType;
        this.body = body.length > MAX_RETAINED_BYTES ? null : body.clone();
        this.referrer = referrer;
    }

    /**
     * Returns the media type of the body.
     * <p>
     * 본문의 미디어 타입을 반환한다.
     *
     * @return the media type / 미디어 타입
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns a copy of the body.
     * <p>
     * 본문의 복사본을 반환한다.
     *
     * @return the body, or null if it was too large to keep / 본문. 보관하기에 너무 컸으면 null
     */
    public byte[] getBody() {
        return body == null ? null : body.clone();
    }

    /**
     * Tells whether the form can be sent again, that is whether the body was kept.
     * <p>
     * 폼을 다시 보낼 수 있는지, 곧 본문을 보관했는지 알려 준다.
     *
     * @return true if the body was kept / 본문을 보관했으면 true
     */
    public boolean isResendable() {
        return body != null;
    }

    /**
     * Returns the address of the page that contained the form.
     * <p>
     * 폼이 있던 페이지의 주소를 반환한다.
     *
     * @return the address, or null / 주소. 없으면 null
     */
    public String getReferrer() {
        return referrer;
    }
}
