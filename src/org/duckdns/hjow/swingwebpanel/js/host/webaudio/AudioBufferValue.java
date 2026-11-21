package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;

/**
 * The script side of an <code>AudioBuffer</code> (Web Audio API 1.4): the PCM data in the form that scripts can reach, and the rule of the
 * specification that decides which party sees a later change of the data.
 * <p>
 * <b>Two stores.</b> The engine plays <code>float[]</code> channels ({@link AudioBuffer}); scripts see a <code>Float32Array</code> whose backing store is a
 * Java <code>byte[]</code> (little endian), and <code>getChannelData()</code> must return the <em>same</em> array object every time, whose content is the content of
 * the buffer. So every channel has exactly one authoritative store: the <code>float[]</code> as long as no script asked for the channel (a decoded or a
 * rendered buffer that scripts only play costs no copy and no second store), and the <code>byte[]</code> from the first <code>getChannelData()</code> (or
 * <code>copyToChannel()</code>) of that channel on; the <code>float[]</code> of the channel is dropped at that moment, so the memory is not doubled.
 * <p>
 * <b>Acquire the content</b> (1.4.3): a node that uses the buffer (a buffer source at <code>start()</code>, a convolver when its buffer is set) calls
 * {@link #acquire()} and gets an engine buffer that no script can change any more. A channel that no script exposed is shared (it is never written again), a
 * channel that scripts can write is copied from its bytes. The REC also says that the arrays returned earlier by <code>getChannelData()</code> are detached
 * at that moment and a later call returns fresh arrays; this implementation does <em>not</em> detach them (the array stays valid and writable, as in
 * Chromium), only the node sees the data of the moment of the acquisition. A script that follows the REC (it asks again after every acquisition) cannot tell the difference
 * except by comparing the identity of the arrays.
 * <p>
 * All methods are for the thread that owns the script realm; the buffers that {@link #acquire()} returns are read by the rendering thread and must stay unchanged.
 * <p>
 * <code>AudioBuffer</code> 의 스크립트 쪽(Web Audio API 1.4): 스크립트가 닿을 수 있는 형태의 PCM 데이터와, 나중에 데이터가 바뀌면 누가 그것을 보는지 정하는 규격의 규칙.
 * <p>
 * <b>저장소 둘.</b> 엔진은 <code>float[]</code> 채널({@link AudioBuffer})을 재생하고, 스크립트는 뒷받침 저장소가 Java <code>byte[]</code>(리틀 엔디언)인
 * <code>Float32Array</code> 를 본다. <code>getChannelData()</code> 는 매번 <em>같은</em> 배열 객체를 반환해야 하고 그 내용이 버퍼의 내용이어야 한다. 그래서 채널마다
 * 기준이 되는 저장소는 하나뿐이다: 스크립트가 그 채널을 요청하지 않은 동안은 <code>float[]</code>(스크립트가 재생만 하는 디코딩 결과나 렌더링 결과는 복사도 둘째
 * 저장소도 필요 없다), 그 채널의 첫 <code>getChannelData()</code>(또는 <code>copyToChannel()</code>)부터는 <code>byte[]</code> 이다. 그 순간 채널의
 * <code>float[]</code> 는 버려지므로 메모리가 두 배가 되지 않는다.
 * <p>
 * <b>내용 가져오기(acquire)</b>(1.4.3): 버퍼를 쓰는 노드(<code>start()</code> 때의 버퍼 원천, 버퍼를 정할 때의 컨볼버)는 {@link #acquire()} 를 불러 스크립트가 더는 바꿀
 * 수 없는 엔진 버퍼를 얻는다. 스크립트가 노출하지 않은 채널은 공유하고(다시 쓰이지 않는다), 스크립트가 쓸 수 있는 채널은 그 바이트에서 복사한다. REC 는 그 순간 앞서
 * <code>getChannelData()</code> 가 반환한 배열을 분리(detach)하고 다음 호출이 새 배열을 반환한다고도 하지만, 이 구현은 분리하지 <em>않는다</em>(배열은 유효하고 쓸 수
 * 있는 채로 남는다. Chromium 과 같다). 노드만 가져온 순간의 데이터를 본다. REC 를 따르는 스크립트(가져온 뒤마다 다시 요청한다)는 배열의 동일성을 비교하지 않는 한 차이를 알 수 없다.
 * <p>
 * 모든 메서드는 스크립트 realm 을 소유한 스레드용이다. {@link #acquire()} 가 반환한 버퍼는 렌더링 스레드가 읽으므로 바뀌지 않아야 한다.
 */
final class AudioBufferValue {

    /**
     * The number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * The length in sample frames.
     * <p>
     * 길이(샘플 프레임).
     */
    private final int length;

    /**
     * The sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz).
     */
    private final float rate;

    /**
     * The samples of the channels that no script reached yet, null for the others.
     * <p>
     * 스크립트가 아직 닿지 않은 채널의 표본. 닿은 채널은 null.
     */
    private final float[][] data;

    /**
     * The little endian bytes of the channels that a script reached, null for the others.
     * <p>
     * 스크립트가 닿은 채널의 리틀 엔디언 바이트. 닿지 않은 채널은 null.
     */
    private final byte[][] bytes;

    /**
     * The <code>Float32Array</code> that <code>getChannelData()</code> returned, per channel.
     * <p>
     * <code>getChannelData()</code> 가 반환한 <code>Float32Array</code>(채널별).
     */
    private final JsObject[] arrays;

    /**
     * The result of the last {@link #acquire()}, valid while no channel was reached by a script, else null.
     * <p>
     * 마지막 {@link #acquire()} 의 결과. 스크립트가 닿은 채널이 없는 동안 유효하고 그렇지 않으면 null.
     */
    private AudioBuffer snapshot;

    /**
     * Creates the value over the channels of an engine buffer, which it adopts (nobody else may use or change that buffer afterwards).
     * <p>
     * 엔진 버퍼의 채널 위에 값을 만든다. 버퍼를 넘겨받으므로 그 뒤에 다른 곳에서 그 버퍼를 쓰거나 바꾸면 안 된다.
     *
     * @param buffer the engine buffer / 엔진 버퍼
     */
    private AudioBufferValue(AudioBuffer buffer) {
        this.channels = buffer.getNumberOfChannels();
        this.length = buffer.getLength();
        this.rate = buffer.getSampleRate();
        this.data = new float[channels][];
        this.bytes = new byte[channels][];
        this.arrays = new JsObject[channels];
        // 채널마다 엔진 버퍼의 배열을 그대로 가져간다.
        for (int c = 0; c < channels; c++) {
            data[c] = buffer.getChannelData(c);
        }
    }

    /**
     * Adopts an engine buffer (a decoded or a rendered one, which no other party uses).
     * <p>
     * 엔진 버퍼를 넘겨받는다(다른 곳에서 쓰지 않는 디코딩 결과나 렌더링 결과).
     *
     * @param buffer the engine buffer / 엔진 버퍼
     * @return the value / 값
     */
    static AudioBufferValue adopt(AudioBuffer buffer) {
        return new AudioBufferValue(buffer);
    }

    /**
     * Creates a silent buffer.
     * <p>
     * 무음 버퍼를 만든다.
     *
     * @param numberOfChannels the number of channels / 채널 수
     * @param length           the length in frames / 길이(프레임)
     * @param sampleRate       the sample rate / 샘플링 주파수
     * @return the value / 값
     * @throws AudioException <code>NotSupportedError</code> if an argument is out of range / 인수가 범위 밖이면 <code>NotSupportedError</code>
     */
    static AudioBufferValue create(int numberOfChannels, int length, float sampleRate) {
        return new AudioBufferValue(new AudioBuffer(numberOfChannels, length, sampleRate));
    }

    /**
     * Returns the number of channels.
     * <p>
     * 채널 수를 반환한다.
     *
     * @return the number of channels / 채널 수
     */
    int getNumberOfChannels() {
        return channels;
    }

    /**
     * Returns the length in sample frames.
     * <p>
     * 길이(샘플 프레임)를 반환한다.
     *
     * @return the length / 길이
     */
    int getLength() {
        return length;
    }

    /**
     * Returns the sample rate.
     * <p>
     * 샘플링 주파수를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    float getSampleRate() {
        return rate;
    }

    /**
     * Returns the duration in seconds.
     * <p>
     * 재생 시간(초)을 반환한다.
     *
     * @return the duration / 재생 시간
     */
    double getDuration() {
        return length / (double) rate;
    }

    /**
     * Checks a channel index.
     * <p>
     * 채널 인덱스를 검사한다.
     *
     * @param c the channel / 채널
     * @throws AudioException <code>IndexSizeError</code> if it is out of range / 범위 밖이면 <code>IndexSizeError</code>
     */
    private void checkChannel(int c) {
        if (c < 0 || c >= channels) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "channel index (" + c + ") exceeds number of channels (" + channels + ")");
        }
    }

    /**
     * Makes the bytes the authoritative store of a channel (the samples are encoded once and dropped).
     * <p>
     * 바이트를 채널의 기준 저장소로 만든다(표본은 한 번 인코딩되고 버려진다).
     *
     * @param c the channel / 채널
     */
    private void ensureBytes(int c) {
        if (bytes[c] == null) {
            bytes[c] = Idl.toBytes(data[c]);
            data[c] = null;
            snapshot = null;
        }
    }

    /**
     * Returns the <code>Float32Array</code> of a channel: the same object every time, whose elements are the samples of the buffer.
     * <p>
     * 채널의 <code>Float32Array</code> 를 반환한다. 매번 같은 객체이며 요소가 버퍼의 표본이다.
     *
     * @param realm the realm / realm
     * @param c     the channel / 채널
     * @return the array / 배열
     * @throws AudioException <code>IndexSizeError</code> if the channel is out of range / 채널이 범위 밖이면 <code>IndexSizeError</code>
     */
    JsObject channelData(JsRealm realm, int c) {
        checkChannel(c);
        ensureBytes(c);
        // 스크립트가 배열의 ArrayBuffer 를 다른 곳으로 옮겼으면(분리) 그 바이트를 더는 우리 것으로 쓰지 않고 복사해서 새 배열을 만든다.
        if (arrays[c] != null && ((TypedArrayObject) arrays[c]).getBuffer().isDetached()) {
            bytes[c] = bytes[c].clone();
            arrays[c] = null;
        }
        if (arrays[c] == null) {
            arrays[c] = TypedArrays.newFloat32Array(realm, TypedArrays.newArrayBuffer(realm, bytes[c]), 0, length);
        }
        return arrays[c];
    }

    /**
     * Copies a channel of the buffer into a <code>Float32Array</code> (<code>copyFromChannel</code>): <code>max(0, min(length - offset, destination.length))</code>
     * samples starting at the offset; an offset behind the end copies nothing.
     * <p>
     * 버퍼의 채널을 <code>Float32Array</code> 로 복사한다(<code>copyFromChannel</code>): offset 부터 <code>max(0, min(length - offset, destination.length))</code> 개.
     * 끝을 넘은 offset 은 아무것도 복사하지 않는다.
     *
     * @param destination the array that receives the samples / 표본을 받는 배열
     * @param c           the channel / 채널
     * @param offset      the first frame to copy / 복사할 첫 프레임
     * @throws AudioException <code>IndexSizeError</code> if the channel is out of range / 채널이 범위 밖이면 <code>IndexSizeError</code>
     */
    void copyFromChannel(TypedArrayObject destination, int c, int offset) {
        checkChannel(c);
        int n = Math.max(0, Math.min(length - offset, destination.getLength()));
        if (offset >= length || n == 0) {
            return;
        }
        byte[] dst = destination.getBuffer().getData();
        int base = destination.getByteOffset();
        if (bytes[c] != null) {
            System.arraycopy(bytes[c], offset * 4, dst, base, n * 4);
        } else {
            float[] src = data[c];
            // 표본을 대상 배열의 리틀 엔디언 바이트로 쓴다.
            for (int i = 0; i < n; i++) {
                Idl.writeInt(dst, base + i * 4, Float.floatToRawIntBits(src[offset + i]));
            }
        }
    }

    /**
     * Copies a <code>Float32Array</code> into a channel of the buffer (<code>copyToChannel</code>): <code>max(0, min(length - offset, source.length))</code>
     * samples starting at the offset; an offset behind the end copies nothing.
     * <p>
     * <code>Float32Array</code> 를 버퍼의 채널로 복사한다(<code>copyToChannel</code>): offset 부터 <code>max(0, min(length - offset, source.length))</code> 개.
     * 끝을 넘은 offset 은 아무것도 복사하지 않는다.
     *
     * @param source the array that holds the samples / 표본을 가진 배열
     * @param c      the channel / 채널
     * @param offset the first frame to write / 쓸 첫 프레임
     * @throws AudioException <code>IndexSizeError</code> if the channel is out of range / 채널이 범위 밖이면 <code>IndexSizeError</code>
     */
    void copyToChannel(TypedArrayObject source, int c, int offset) {
        checkChannel(c);
        int n = Math.max(0, Math.min(length - offset, source.getLength()));
        if (offset >= length || n == 0) {
            return;
        }
        ensureBytes(c);
        System.arraycopy(source.getBuffer().getData(), source.getByteOffset(), bytes[c], offset * 4, n * 4);
    }

    /**
     * Acquires the content of the buffer (1.4.3): returns an engine buffer with the data of this moment that nobody changes any more. Channels that no script
     * reached are shared with this value and with the other results (they are never written again); channels that a script reached are copied from their bytes.
     * <p>
     * 버퍼의 내용을 가져온다(1.4.3): 이 순간의 데이터를 담고 이제 누구도 바꾸지 않는 엔진 버퍼를 반환한다. 스크립트가 닿지 않은 채널은 이 값 및 다른 결과와 공유하고(다시 쓰이지 않는다),
     * 스크립트가 닿은 채널은 그 바이트에서 복사한다.
     *
     * @return the engine buffer / 엔진 버퍼
     */
    AudioBuffer acquire() {
        if (snapshot != null) {
            return snapshot;
        }
        float[][] copy = new float[channels][];
        boolean exposed = false;
        // 채널마다 스크립트가 닿았으면 바이트에서 복사하고 아니면 배열을 공유한다.
        for (int c = 0; c < channels; c++) {
            if (bytes[c] != null) {
                copy[c] = Idl.fromBytes(bytes[c]);
                exposed = true;
            } else {
                copy[c] = data[c];
            }
        }
        AudioBuffer result = AudioBuffer.wrap(copy, rate);
        // 스크립트가 닿은 채널이 없을 때만 결과를 기억한다(있으면 다음 가져오기가 달라질 수 있다).
        if (!exposed) {
            snapshot = result;
        }
        return result;
    }
}
