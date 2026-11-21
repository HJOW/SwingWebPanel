/**
 * Signal processing helpers of the Web Audio engine (plan items P25-02 to P25-04): the FFT, the biquad filter coefficients and kernel
 * and the general IIR kernel. The classes know nothing about the audio graph, so they can be tested with plain arrays.
 * <p>
 * Web Audio 엔진의 신호 처리 도우미(계획 P25-02~P25-04): FFT, 쌍이차 필터 계수와 커널, 일반 IIR 커널. 이 클래스들은 오디오 그래프를 모르므로
 * 단순한 배열로 시험할 수 있다.
 */
package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;
