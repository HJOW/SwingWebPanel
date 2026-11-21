/**
 * The decoding of audio file data into <code>AudioBuffer</code>s for <code>decodeAudioData</code> (plan item P25-05), still without any
 * JavaScript: the sample rate converter {@link org.duckdns.hjow.swingwebpanel.media.webaudio.decode.Resampler}, the pipeline
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.decode.AudioBufferDecoder} that reuses the audio decoders of
 * {@link org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders} (chosen by the signature of the data) and resamples to the sample rate of
 * the context, and the background service {@link org.duckdns.hjow.swingwebpanel.media.webaudio.decode.AudioDecodeService} that decodes on
 * its own daemon threads, can cancel a request and reports the result through a callback on a thread of the caller's choice. The script
 * layer turns a callback into the resolution of a promise, detaches the <code>ArrayBuffer</code> and moves the callback to the document queue.
 * <p>
 * <code>decodeAudioData</code> 를 위해 오디오 파일 데이터를 <code>AudioBuffer</code> 로 디코딩하는 부분(계획 P25-05). 아직 JavaScript 와 무관하다.
 * 표본율 변환기 {@link org.duckdns.hjow.swingwebpanel.media.webaudio.decode.Resampler}, {@link org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders}
 * 의 오디오 디코더(데이터의 시그니처로 고른다)를 재사용하고 컨텍스트의 표본율로 리샘플링하는 파이프라인
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.decode.AudioBufferDecoder}, 자기 데몬 스레드에서 디코딩하고 요청을 취소할 수 있으며 호출한 쪽이
 * 고른 스레드에서 콜백으로 결과를 알리는 백그라운드 서비스 {@link org.duckdns.hjow.swingwebpanel.media.webaudio.decode.AudioDecodeService} 로 이루어진다.
 * 스크립트 계층이 콜백을 promise 의 이행으로 바꾸고 <code>ArrayBuffer</code> 를 분리(detach)하며 콜백을 문서 큐로 옮긴다.
 */
package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;
