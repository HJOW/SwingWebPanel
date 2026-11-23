package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBus;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioParam;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode;

/**
 * Calls the <code>process()</code> method of one <code>AudioWorkletProcessor</code> instance for every render quantum (Web Audio API 1.0, 2.4 "rendering an audio graph" and 1.32.4.3):
 * it turns the audio of the engine into the arguments of the method, calls it, and turns what the method wrote into the audio of the engine.
 * <ul>
 * <li><strong>Arguments.</strong> <code>inputs[n][m]</code> is a <code>Float32Array</code> with the samples of channel <em>m</em> of input <em>n</em>; <code>inputs[n]</code> is an empty
 * array while no actively processing node is connected to the input. <code>outputs[n][m]</code> is a zero filled <code>Float32Array</code>. <code>parameters</code> is an
 * object with a <code>Float32Array</code> for every parameter, named like the descriptor: one element if the value is constant over the quantum, otherwise one per frame.</li>
 * <li><strong>Reuse.</strong> The specification says that the arrays are reused between calls so that nothing is allocated, and that new arrays are made if the topology changes (the
 * number of channels of an input or an output, the length of a parameter array) or if an array was transferred. This class keeps one <code>Float32Array</code> and one
 * <code>byte[]</code> per channel and per parameter and fills them in place; the outer arrays and the <code>parameters</code> object are frozen (<code>FrozenArray</code>), so they are
 * made again only when the topology changes. Per quantum the only allocations are the call itself and what the script creates.</li>
 * <li><strong>Result.</strong> The return value is converted with <code>ToBoolean</code>: it is the active source flag. The method is looked up with <code>Get(O, "process")</code> for
 * every quantum, as the specification says; if it is not callable the call fails with a <code>TypeError</code>.</li>
 * </ul>
 * All methods run with the lock of the script realm held.
 * <p>
 * 렌더링 quantum 마다 <code>AudioWorkletProcessor</code> 인스턴스 하나의 <code>process()</code> 메서드를 호출한다(Web Audio API 1.0, 2.4 "rendering an audio graph" 와 1.32.4.3). 엔진의
 * 오디오를 메서드의 인수로 바꾸고, 호출하고, 메서드가 쓴 것을 엔진의 오디오로 바꾼다.
 * <ul>
 * <li><strong>인수.</strong> <code>inputs[n][m]</code> 은 입력 <em>n</em> 의 채널 <em>m</em> 의 표본을 가진 <code>Float32Array</code> 이다. 활성으로 처리하는 노드가 입력에 연결되어 있지 않으면
 * <code>inputs[n]</code> 은 빈 배열이다. <code>outputs[n][m]</code> 은 0 으로 채운 <code>Float32Array</code> 이다. <code>parameters</code> 는 설명의 이름으로 파라미터마다
 * <code>Float32Array</code> 를 가진 객체이며, 값이 quantum 동안 일정하면 요소 하나, 아니면 프레임마다 하나이다.</li>
 * <li><strong>재사용.</strong> 규격은 아무것도 할당하지 않도록 호출 사이에 배열을 재사용하고, 토폴로지(입력이나 출력의 채널 수, 파라미터 배열의 길이)가 바뀌거나 배열이 전달되었으면 새 배열을
 * 만든다고 한다. 이 클래스는 채널마다, 파라미터마다 <code>Float32Array</code> 하나와 <code>byte[]</code> 하나를 두고 제자리에서 채운다. 바깥 배열과 <code>parameters</code> 객체는 얼려 있으므로
 * (<code>FrozenArray</code>) 토폴로지가 바뀔 때만 다시 만든다. quantum 마다 할당하는 것은 호출 자체와 스크립트가 만드는 것뿐이다.</li>
 * <li><strong>결과.</strong> 반환 값은 <code>ToBoolean</code> 으로 바꾼다: active source flag 이다. 메서드는 규격대로 quantum 마다 <code>Get(O, "process")</code> 로 찾으며, 호출할 수 없으면
 * <code>TypeError</code> 로 실패한다.</li>
 * </ul>
 * 모든 메서드는 스크립트 realm 의 잠금을 쥔 채 실행된다.
 */
final class ProcessorRunner implements AudioWorkletNode.Processor {

    /**
     * The audio of one channel or parameter: the bytes, the buffer over them and the <code>Float32Array</code> that the script sees.
     * <p>
     * 채널이나 파라미터 하나의 오디오: 바이트, 그 위의 버퍼, 스크립트가 보는 <code>Float32Array</code>.
     */
    private static final class Chan {

        /**
         * The storage of the elements, little endian.
         * <p>
         * 요소의 저장소(리틀 엔디언).
         */
        final byte[] bytes;

        /**
         * The buffer over the bytes; the script detaches it when it transfers it.
         * <p>
         * 바이트 위의 버퍼. 스크립트가 전달하면 분리된다.
         */
        final ArrayBufferObject buffer;

        /**
         * The array that the script sees.
         * <p>
         * 스크립트가 보는 배열.
         */
        final TypedArrayObject array;

        /**
         * Creates the storage of an array of a length.
         * <p>
         * 길이가 있는 배열의 저장소를 만든다.
         *
         * @param realm  the realm / realm
         * @param length the number of elements / 요소 수
         */
        Chan(JsRealm realm, int length) {
            this.bytes = new byte[length * 4];
            this.buffer = TypedArrays.newArrayBuffer(realm, bytes);
            this.array = TypedArrays.newFloat32Array(realm, buffer, 0, length);
        }

        /**
         * Tells whether the array can be reused for a length: the script did not transfer the buffer.
         * <p>
         * 배열을 어떤 길이에 다시 쓸 수 있는지 알려 준다: 스크립트가 버퍼를 전달하지 않았다.
         *
         * @param length the number of elements that is needed / 필요한 요소 수
         * @return true if it can be reused / 다시 쓸 수 있으면 true
         */
        boolean usable(int length) {
            return !buffer.isDetached() && bytes.length == length * 4;
        }
    }

    /**
     * The arrays of the inputs or the outputs: <code>[input][channel]</code> storage, the topology that the script saw last, and the frozen arrays that it was given.
     * <p>
     * 입력이나 출력의 배열: <code>[입력][채널]</code> 저장소, 스크립트가 마지막으로 본 토폴로지, 스크립트에 준 얼린 배열.
     */
    private final class Bank {

        /**
         * The storage per input or output; an entry may hold more channels than are in use.
         * <p>
         * 입력이나 출력별 저장소. 항목은 쓰이는 것보다 많은 채널을 가질 수 있다.
         */
        final Chan[][] chans;

        /**
         * The number of channels per input or output that the script saw last; -1 for none.
         * <p>
         * 스크립트가 마지막으로 본 입력이나 출력별 채널 수. 없으면 -1.
         */
        final int[] signature;

        /**
         * The outer array that the script was given, null before the first call.
         * <p>
         * 스크립트에 준 바깥 배열. 첫 호출 전에는 null.
         */
        JsObject outer;

        /**
         * True if the arrays have to be made again.
         * <p>
         * 배열을 다시 만들어야 하면 true.
         */
        boolean changed = true;

        /**
         * Creates the bank for a number of inputs or outputs.
         * <p>
         * 입력이나 출력의 수에 대한 뱅크를 만든다.
         *
         * @param count the number of inputs or outputs / 입력이나 출력의 수
         */
        Bank(int count) {
            chans = new Chan[count][];
            signature = new int[count];
            Arrays.fill(signature, Integer.MIN_VALUE);
        }

        /**
         * Prepares one input or output for a quantum: gives it the channel storage that it needs, and notes a change of the topology.
         * <p>
         * 입력이나 출력 하나를 quantum 에 맞게 준비한다: 필요한 채널 저장소를 주고 토폴로지가 바뀐 것을 기록한다.
         *
         * @param index    the index / 인덱스
         * @param channels the number of channels, -1 for none / 채널 수. 없으면 -1
         * @param frames   the number of frames / 프레임 수
         */
        void prepare(int index, int channels, int frames) {
            // 채널이 있으면 저장소가 모자라거나 쓸 수 없는 것(전달되어 분리됨)을 새로 만든다.
            if (channels > 0) {
                Chan[] row = chans[index];
                if (row == null || row.length < channels) {
                    Chan[] bigger = new Chan[channels];
                    if (row != null) {
                        System.arraycopy(row, 0, bigger, 0, row.length);
                    }
                    row = bigger;
                    chans[index] = row;
                }
                // 채널마다 저장소가 없거나 쓸 수 없으면 새로 만든다.
                for (int c = 0; c < channels; c++) {
                    if (row[c] == null || !row[c].usable(frames)) {
                        row[c] = new Chan(realm(), frames);
                        changed = true;
                    }
                }
            }
            if (signature[index] != channels) {
                signature[index] = channels;
                changed = true;
            }
        }

        /**
         * Returns the outer array for the script, made again if the topology changed.
         * <p>
         * 스크립트에 줄 바깥 배열을 반환한다. 토폴로지가 바뀌었으면 다시 만든다.
         *
         * @return the frozen array / 얼린 배열
         */
        JsObject array() {
            if (outer == null || changed) {
                JsValue[] items = new JsValue[chans.length];
                // 입력이나 출력마다 채널 배열을 만들어 얼린다.
                for (int i = 0; i < items.length; i++) {
                    int n = Math.max(0, signature[i]);
                    JsValue[] arrays = new JsValue[n];
                    // 쓰이는 채널의 Float32Array 를 모은다.
                    for (int c = 0; c < n; c++) {
                        arrays[c] = chans[i][c].array;
                    }
                    JsObject inner = realm().newArray(arrays);
                    scope.freeze(inner);
                    items[i] = inner;
                }
                outer = realm().newArray(items);
                scope.freeze(outer);
                changed = false;
            }
            return outer;
        }
    }

    /**
     * The scope that owns the realm.
     * <p>
     * realm 을 소유한 스코프.
     */
    private final WorkletScope scope;

    /**
     * The processor instance.
     * <p>
     * 프로세서 인스턴스.
     */
    private final JsObject processor;

    /**
     * The inputs; created at the first call, when the number of inputs is known.
     * <p>
     * 입력. 입력 수를 알게 되는 첫 호출에서 만든다.
     */
    private Bank inputs;

    /**
     * The outputs.
     * <p>
     * 출력.
     */
    private Bank outputs;

    /**
     * The storage of the parameter arrays, by the index of the parameter.
     * <p>
     * 파라미터 배열의 저장소(파라미터 인덱스별).
     */
    private Chan[] paramChans;

    /**
     * The <code>parameters</code> object that the script was given, or null; made again when the length of a parameter array changed.
     * <p>
     * 스크립트에 준 <code>parameters</code> 객체. 없으면 null. 파라미터 배열의 길이가 바뀌면 다시 만든다.
     */
    private JsObject parameterObject;

    /**
     * The names of the parameters, in the order of the descriptors.
     * <p>
     * 파라미터 이름(설명의 순서).
     */
    private final String[] paramNames;

    /**
     * The values of a parameter, copied from the engine.
     * <p>
     * 엔진에서 복사한 파라미터 값.
     */
    private float[] scratch;

    /**
     * Creates the runner of a processor instance.
     * <p>
     * 프로세서 인스턴스의 실행기를 만든다.
     *
     * @param scope     the scope that owns the realm / realm 을 소유한 스코프
     * @param processor the instance / 인스턴스
     * @param params    the parameters of the node, in the order of the descriptors / 노드의 파라미터(설명의 순서)
     */
    ProcessorRunner(WorkletScope scope, JsObject processor, List<AudioParam> params) {
        this.scope = scope;
        this.processor = processor;
        this.paramNames = new String[params.size()];
        // 파라미터 이름을 순서대로 기억한다.
        for (int i = 0; i < paramNames.length; i++) {
            paramNames[i] = params.get(i).getName();
        }
    }

    /**
     * Returns the realm of the scope.
     * <p>
     * 스코프의 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    private JsRealm realm() {
        return scope.realm();
    }

    /**
     * Processes one quantum on the rendering thread: the scope runs it with the lock held and the limits of a call (see {@link WorkletScope}).
     * <p>
     * quantum 하나를 렌더링 스레드에서 처리한다: 스코프가 잠금을 쥐고 호출의 한도 안에서 실행한다({@link WorkletScope} 참고).
     *
     * @param node   the node / 노드
     * @param frames the number of frames / 프레임 수
     * @return the result / 결과
     */
    public int process(AudioWorkletNode node, int frames) {
        return scope.runProcessor(this, node, frames);
    }

    /**
     * The call itself, with the lock of the realm held and the limits set: builds the arguments, calls <code>process()</code>, takes the output, and runs the microtasks.
     * <p>
     * 호출 자체. realm 의 잠금을 쥐고 한도를 정한 상태에서 인수를 만들고, <code>process()</code> 를 호출하고, 출력을 가져오고, 마이크로태스크를 실행한다.
     *
     * @param node   the node / 노드
     * @param frames the number of frames / 프레임 수
     * @return {@link AudioWorkletNode#ACTIVE} or {@link AudioWorkletNode#INACTIVE} / {@link AudioWorkletNode#ACTIVE} 또는 {@link AudioWorkletNode#INACTIVE}
     * @throws JsException if the script threw / 스크립트가 던졌으면
     */
    int invoke(AudioWorkletNode node, int frames) {
        int inCount = node.getNumberOfInputs();
        int outCount = node.getNumberOfOutputs();
        if (inputs == null) {
            inputs = new Bank(inCount);
            outputs = new Bank(outCount);
            paramChans = new Chan[paramNames.length];
            scratch = new float[Math.max(frames, 1)];
        }
        // 입력마다 활성이면 오디오를 바이트로 복사하고, 활성 연결이 없으면 빈 배열로 둔다.
        for (int i = 0; i < inCount; i++) {
            AudioBus in = node.getInput(i);
            inputs.prepare(i, in == null ? -1 : in.getNumberOfChannels(), frames);
            // 채널마다 표본을 바이트로 쓴다.
            for (int c = 0; in != null && c < in.getNumberOfChannels(); c++) {
                FloatBytes.write(in.channel(c), frames, inputs.chans[i][c].bytes);
            }
        }
        // 출력마다 0 으로 채운 채널 저장소를 준비한다.
        for (int j = 0; j < outCount; j++) {
            AudioBus out = node.getOutput(j);
            outputs.prepare(j, out.getNumberOfChannels(), frames);
            // 채널마다 바이트를 0 으로 채운다.
            for (int c = 0; c < out.getNumberOfChannels(); c++) {
                Arrays.fill(outputs.chans[j][c].bytes, (byte) 0);
            }
        }
        JsObject parameters = prepareParameters(node, frames);
        JsValue method = processor.get("process");
        if (!(method instanceof JsObject) || !((JsObject) method).isCallable()) {
            throw JsException.typeError("The 'process' property of the AudioWorkletProcessor is not a function.");
        }
        JsValue result = Ops.call(method, processor, new JsValue[] {inputs.array(), outputs.array(), parameters});
        // 출력 채널마다 스크립트가 쓴 바이트를 엔진의 오디오로 되돌린다.
        for (int j = 0; j < outCount; j++) {
            AudioBus out = node.getOutput(j);
            // 채널마다 바이트를 표본으로 읽는다.
            for (int c = 0; c < out.getNumberOfChannels(); c++) {
                FloatBytes.read(outputs.chans[j][c].bytes, frames, out.channel(c));
            }
        }
        boolean flag = Ops.toBoolean(result);
        realm().runJobs();
        return flag ? AudioWorkletNode.ACTIVE : AudioWorkletNode.INACTIVE;
    }

    /**
     * Fills the arrays of the parameters and returns the <code>parameters</code> object: arrays of one element for a constant value, of one per frame otherwise.
     * <p>
     * 파라미터의 배열을 채우고 <code>parameters</code> 객체를 반환한다: 값이 일정하면 요소 하나, 아니면 프레임마다 하나.
     *
     * @param node   the node / 노드
     * @param frames the number of frames / 프레임 수
     * @return the frozen object / 얼린 객체
     */
    private JsObject prepareParameters(AudioWorkletNode node, int frames) {
        boolean changed = parameterObject == null;
        // 파라미터마다 값을 복사하고, 개수가 달라졌거나 쓸 수 없는 배열은 새로 만든다.
        for (int k = 0; k < paramNames.length; k++) {
            int count = node.copyParameter(k, scratch);
            if (paramChans[k] == null || !paramChans[k].usable(count)) {
                paramChans[k] = new Chan(realm(), count);
                changed = true;
            }
            FloatBytes.write(scratch, count, paramChans[k].bytes);
        }
        if (changed) {
            JsObject object = realm().newObject();
            // 파라미터마다 이름으로 배열을 둔다.
            for (int k = 0; k < paramNames.length; k++) {
                object.createDataProperty(paramNames[k], paramChans[k].array);
            }
            scope.freeze(object);
            parameterObject = object;
        }
        return parameterObject;
    }
}
