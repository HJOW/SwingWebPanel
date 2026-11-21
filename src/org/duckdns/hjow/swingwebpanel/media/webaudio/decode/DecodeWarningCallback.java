package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

/**
 * A {@link DecodeCallback} that also wants to know when the data was decoded only in part: the specification (Web Audio API 1.1 draft) encourages
 * an implementation to warn the author when the compressed data is corrupted but the decoding can otherwise proceed, for example in the developer tools. The
 * service calls {@link #onWarning(String)} <em>before</em> {@link #onSuccess} of the same result, on the same thread, only for a result that is truncated
 * (see {@link DecodeResult#isTruncated()}); a callback that does not implement this interface simply does not hear of it, and
 * {@link DecodeCallback#onSuccess} is unchanged.
 * <p>
 * 데이터가 일부만 디코딩되었을 때 그것도 알고 싶은 {@link DecodeCallback}: 규격(Web Audio API 1.1 초안)은 압축된 데이터가 손상되었지만 디코딩을 계속할 수 있을 때 구현이
 * 작성자에게(예: 개발자 도구에) 경고하도록 권장한다. 서비스는 잘린 결과에 대해서만 같은 결과의 {@link #onSuccess} <em>앞에서</em> 같은 스레드에서
 * {@link #onWarning(String)} 을 부른다. 이 인터페이스를 구현하지 않은 콜백은 그것을 듣지 못할 뿐이며 {@link DecodeCallback#onSuccess} 는 바뀌지 않는다.
 */
public interface DecodeWarningCallback extends DecodeCallback {

    /**
     * Called before <code>onSuccess</code> when the buffer holds only the part of the data that could be decoded.
     * <p>
     * 버퍼가 디코딩할 수 있었던 데이터의 일부만 담고 있을 때 <code>onSuccess</code> 앞에서 불린다.
     *
     * @param message what was wrong with the data / 데이터가 어땠는지
     */
    void onWarning(String message);
}
