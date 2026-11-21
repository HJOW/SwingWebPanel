package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AnalyserNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBufferSourceNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioDestinationNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioParam;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioScheduledSourceNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BaseAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BiquadFilterNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ChannelMergerNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ChannelSplitterNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ConstantSourceNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ConvolverNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.DelayNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.DynamicsCompressorNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.GainNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.IIRFilterNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.MediaElementAudioSourceNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.MediaElementTap;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OscillatorNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.PannerNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.PeriodicWave;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ScriptProcessorNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.StereoPannerNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WaveShaperNode;

/**
 * The script interfaces of the audio nodes (Web Audio API 1.5 to 1.31): <code>AudioNode</code> with <code>connect</code> and <code>disconnect</code>,
 * <code>AudioScheduledSourceNode</code> with <code>start</code>, <code>stop</code> and <code>onended</code>, <code>AudioDestinationNode</code> and the nodes themselves, each with
 * its constructor <code>new XNode(context, options)</code> and the <code>createX()</code> factory of the context. A node is a {@link NodeValue} (an <code>EventTarget</code> that holds the engine
 * node); the members that follow from the Java API of the engine are installed by {@link Members} from short specifications, and the few that have script-only rules
 * are written out: the buffer of a buffer source (assigned once, acquired when the source starts, see {@link AudioBufferValue}) and of a convolver (acquired when it is set), the
 * curve of a wave shaper (copied when it is set), the oscillator's periodic wave, <code>getFrequencyResponse</code> and the analyser's data methods (which write into typed arrays).
 * <p>
 * Construction. The constructor and the factory create the engine node through the same code: {@link #create}. The options are a WebIDL dictionary: the members of
 * <code>AudioNodeOptions</code> (<code>channelCount</code>, <code>channelCountMode</code>, <code>channelInterpretation</code>) are applied first and the members of the node in alphabetical
 * order after them, as WebIDL converts dictionaries; a member that is undefined is left alone; an invalid enumeration name is a <code>TypeError</code>; a parameter member sets the
 * <code>value</code> of the parameter. A factory with arguments (<code>createDelay(max)</code>, <code>createChannelSplitter(n)</code>, <code>createIIRFilter(ff, fb)</code>) builds the same dictionary.
 * <p>
 * 오디오 노드의 스크립트 인터페이스(Web Audio API 1.5~1.31): <code>connect</code>, <code>disconnect</code> 를 가진 <code>AudioNode</code>, <code>start</code>, <code>stop</code>,
 * <code>onended</code> 를 가진 <code>AudioScheduledSourceNode</code>, <code>AudioDestinationNode</code> 와 노드들. 각각 생성자 <code>new XNode(context, options)</code> 와 컨텍스트의
 * <code>createX()</code> 팩터리를 가진다. 노드는 {@link NodeValue}(엔진 노드를 쥔 <code>EventTarget</code>)이며, 엔진의 Java API 에서 따라 나오는 멤버는 {@link Members} 가
 * 짧은 명세에서 설치하고, 스크립트에만 있는 규칙을 가진 몇몇은 직접 쓴다: 버퍼 원천의 버퍼(한 번만 대입, 원천이 시작할 때 가져감. {@link AudioBufferValue} 참고)와 컨볼버의 버퍼(정할 때
 * 가져감), 웨이브셰이퍼의 곡선(정할 때 복사), 발진기의 주기파, <code>getFrequencyResponse</code>, 형식화 배열에 쓰는 분석기의 데이터 메서드.
 * <p>
 * 생성. 생성자와 팩터리는 같은 코드 {@link #create} 로 엔진 노드를 만든다. 옵션은 WebIDL 사전이다: <code>AudioNodeOptions</code> 의 멤버(<code>channelCount</code>,
 * <code>channelCountMode</code>, <code>channelInterpretation</code>)를 먼저 적용하고 노드의 멤버를 알파벳 순으로 그 뒤에 적용한다(WebIDL 이 사전을 변환하는 순서). undefined 인 멤버는
 * 그대로 두고, 잘못된 열거형 이름은 <code>TypeError</code>, 파라미터 멤버는 파라미터의 <code>value</code> 를 정한다. 인수가 있는 팩터리(<code>createDelay(max)</code>,
 * <code>createChannelSplitter(n)</code>, <code>createIIRFilter(ff, fb)</code>)는 같은 사전을 만든다.
 */
final class NodeApi {

    /**
     * Creates the engine node of a type from the options of the script.
     * <p>
     * 스크립트의 옵션으로 어떤 형식의 엔진 노드를 만든다.
     */
    private interface Maker {

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param engine  the engine context / 엔진 컨텍스트
         * @param options the options, null for none / 옵션. 없으면 null
         * @return the node / 노드
         */
        AudioNode make(BaseAudioContext engine, JsObject options);
    }

    /**
     * Creates the whole node value of a type that needs more than the engine context to be made (the node of a media element connects the element of the page to the graph).
     * <p>
     * 엔진 컨텍스트보다 더 많은 것이 있어야 만들 수 있는 형식의 노드 값을 통째로 만든다(미디어 요소의 노드는 페이지의 요소를 그래프에 연결한다).
     */
    private interface ContextMaker {

        /**
         * Creates the node value.
         * <p>
         * 노드 값을 만든다.
         *
         * @param c       the context / 컨텍스트
         * @param options the options, null for none / 옵션. 없으면 null
         * @return the node value / 노드 값
         */
        NodeValue make(ContextValue c, JsObject options);
    }

    /**
     * One member of an options dictionary: a parameter (its value is set), an attribute (its setter is called with the conversion of the Java type) or a
     * member with rules of its own.
     * <p>
     * 옵션 사전의 멤버 하나: 파라미터(값을 정한다), 속성(Java 형식 변환으로 setter 를 부른다), 또는 자기 규칙이 있는 멤버.
     */
    private static final class Option {

        /**
         * The name of the member.
         * <p>
         * 멤버 이름.
         */
        final String name;

        /**
         * The kind: <code>p</code> parameter, <code>a</code> attribute, <code>x</code> special.
         * <p>
         * 종류: <code>p</code> 파라미터, <code>a</code> 속성, <code>x</code> 특수.
         */
        final char kind;

        /**
         * The getter of the parameter or the setter of the attribute, null for a special member.
         * <p>
         * 파라미터의 getter 또는 속성의 setter. 특수 멤버면 null.
         */
        final Method method;

        /**
         * Creates the member.
         * <p>
         * 멤버를 만든다.
         *
         * @param name   the name / 이름
         * @param kind   the kind / 종류
         * @param method the method / 메서드
         */
        Option(String name, char kind, Method method) {
            this.name = name;
            this.kind = kind;
            this.method = method;
        }
    }

    /**
     * The description of a node type.
     * <p>
     * 노드 형식의 기술.
     */
    private static final class Type {

        /**
         * The interface name.
         * <p>
         * 인터페이스 이름.
         */
        final String name;

        /**
         * The engine class.
         * <p>
         * 엔진 클래스.
         */
        final Class<?> engine;

        /**
         * The creator of the engine node.
         * <p>
         * 엔진 노드를 만드는 쪽.
         */
        final Maker maker;

        /**
         * The members of the options dictionary in the order of their application.
         * <p>
         * 적용 순서의 옵션 사전 멤버.
         */
        final List<Option> options = new ArrayList<Option>();

        /**
         * The prototype of the interface.
         * <p>
         * 인터페이스의 프로토타입.
         */
        JsObject proto;

        /**
         * The creator of the whole node value, or null if {@link #maker} creates the engine node (the usual case).
         * <p>
         * 노드 값을 통째로 만드는 쪽. {@link #maker} 가 엔진 노드를 만드는 보통의 경우에는 null.
         */
        ContextMaker contextMaker;

        /**
         * Creates the description.
         * <p>
         * 기술을 만든다.
         *
         * @param name   the interface name / 인터페이스 이름
         * @param engine the engine class / 엔진 클래스
         * @param maker  the creator of the node / 노드를 만드는 쪽
         */
        Type(String name, Class<?> engine, Maker maker) {
            this.name = name;
            this.engine = engine;
            this.maker = maker;
        }
    }

    /**
     * The owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * The member installer.
     * <p>
     * 멤버 설치기.
     */
    private final Members members;

    /**
     * The DOM bridge.
     * <p>
     * DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The realm.
     * <p>
     * The script realm.
     * <p>
     * 스크립트 realm.
     */
    private final JsRealm realm;

    /**
     * The interface name of every engine node class.
     * <p>
     * 엔진 노드 클래스마다의 인터페이스 이름.
     */
    private final Map<Class<?>, String> names = new HashMap<Class<?>, String>();

    /**
     * The node types by interface name.
     * <p>
     * 인터페이스 이름별 노드 형식.
     */
    private final Map<String, Type> types = new HashMap<String, Type>();

    /**
     * The members of <code>AudioNodeOptions</code>, applied to every node.
     * <p>
     * 모든 노드에 적용하는 <code>AudioNodeOptions</code> 의 멤버.
     */
    private final List<Option> nodeOptions = new ArrayList<Option>();

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api     the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param members the member installer / 멤버 설치기
     */
    NodeApi(WebAudioApi api, Members members) {
        this.api = api;
        this.members = members;
        this.bridge = api.bridge();
        this.realm = api.realm();
    }

    /**
     * Names the interface of a node value or an engine node class.
     * <p>
     * 노드 값의 인터페이스 이름을 정한다.
     *
     * @param target the Java object / Java 객체
     * @return the interface name, or null if the object is no node value / 인터페이스 이름. 노드 값이 아니면 null
     */
    String classify(Object target) {
        if (target instanceof NodeValue) {
            return names.get(((NodeValue) target).node().getClass());
        }
        return null;
    }

    /**
     * Returns the node value of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 노드 값을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the value / 값
     */
    static NodeValue nodeOf(JsValue self) {
        NodeValue v = (NodeValue) DomBridge.targetOf(self, NodeValue.class);
        if (v == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return v;
    }

    /**
     * Wraps an engine node: creates its value, and lets the <code>ended</code> event of a source reach the script.
     * <p>
     * 엔진 노드를 감싼다: 값을 만들고, 원천의 <code>ended</code> 이벤트가 스크립트에 닿게 한다.
     *
     * @param c    the context / 컨텍스트
     * @param node the engine node / 엔진 노드
     * @return the value / 값
     */
    NodeValue wrap(ContextValue c, AudioNode node) {
        final NodeValue v = new NodeValue(c, node);
        // 컨텍스트가 스크립트에 닿을 수 없게 되었는지 알 수 있도록 노드의 스크립트 객체를 약하게 기억한다(같은 객체를 곧 스크립트가 받는다).
        c.trackNodeWrapper(bridge.wrap(v));
        if (node instanceof AudioScheduledSourceNode) {
            // 원천이 끝나면 렌더링 스레드가 작업 싱크로 알리고, 페이지의 작업에서 ended 이벤트를 발생시킨다.
            ((AudioScheduledSourceNode) node).setEndedListener(new Runnable() {
                /**
                 * Fires <code>ended</code> at the node.
                 * <p>
                 * 노드에 <code>ended</code> 를 발생시킨다.
                 */
                public void run() {
                    api.fire(v, "ended");
                }
            });
        }
        return v;
    }

    /**
     * Creates a node of a type through the code that the constructor and the factory share.
     * <p>
     * 생성자와 팩터리가 함께 쓰는 코드로 어떤 형식의 노드를 만든다.
     *
     * @param c       the context / 컨텍스트
     * @param type    the interface name of the node / 노드의 인터페이스 이름
     * @param options the options of the script, null for none / 스크립트의 옵션. 없으면 null
     * @return the node value / 노드 값
     */
    NodeValue create(ContextValue c, String type, JsObject options) {
        Type t = types.get(type);
        NodeValue v = t.contextMaker != null ? t.contextMaker.make(c, options) : wrap(c, t.maker.make(c.engine(), options));
        if (options != null) {
            apply(v, t, options);
        }
        return v;
    }

    /**
     * Applies the options to a new node: <code>AudioNodeOptions</code> first, then the members of the type.
     * <p>
     * 새 노드에 옵션을 적용한다: 먼저 <code>AudioNodeOptions</code>, 그 다음 형식의 멤버.
     *
     * @param v       the node / 노드
     * @param t       the type / 형식
     * @param options the options / 옵션
     */
    private void apply(NodeValue v, Type t, JsObject options) {
        // 두 목록(노드 공통, 형식별)의 멤버를 차례로 적용한다.
        for (int pass = 0; pass < 2; pass++) {
            List<Option> list = pass == 0 ? nodeOptions : t.options;
            // 목록의 멤버마다 옵션에 값이 있으면 적용한다.
            for (int i = 0; i < list.size(); i++) {
                Option o = list.get(i);
                JsValue x = options.get(o.name);
                if (x.isUndefined()) {
                    continue;
                }
                if (o.kind == 'p') {
                    AudioParam p = (AudioParam) Members.invoke(o.method, v.node(), new Object[0]);
                    p.setValue(Idl.flt(x));
                } else if (o.kind == 'a') {
                    Class<?> pt = o.method.getParameterTypes()[0];
                    Members.invoke(o.method, v.node(), new Object[] {members.fromJs(pt, x, true, pt.getSimpleName())});
                } else {
                    special(v, o.name, x);
                }
            }
        }
    }

    /**
     * Applies a member with rules of its own.
     * <p>
     * 자기 규칙이 있는 멤버를 적용한다.
     *
     * @param v    the node / 노드
     * @param name the member / 멤버
     * @param x    the value / 값
     */
    private void special(NodeValue v, String name, JsValue x) {
        if ("buffer".equals(name)) {
            if (v.node() instanceof ConvolverNode) {
                setConvolverBuffer(v, x);
            } else {
                setSourceBuffer(v, x);
            }
        } else if ("curve".equals(name)) {
            setCurve(v, x);
        } else if ("periodicWave".equals(name)) {
            setPeriodicWave(v, x);
        } else if ("disableNormalization".equals(name)) {
            ((ConvolverNode) v.node()).setNormalize(!Ops.toBoolean(x));
        }
    }

    /**
     * Adds a node type: its interface with the constructor, the members from the specifications and the options dictionary.
     * <p>
     * 노드 형식을 더한다: 생성자를 가진 인터페이스, 명세에서 만든 멤버, 옵션 사전.
     *
     * @param name       the interface name / 인터페이스 이름
     * @param parent     the parent interface / 부모 인터페이스
     * @param engine     the engine class / 엔진 클래스
     * @param maker      the creator of the node / 노드를 만드는 쪽
     * @param attributes the attribute specifications / 속성 명세
     * @param methods    the operation specifications / 연산 명세
     * @param options    the option specifications, <code>name:p</code> (parameter), <code>name:a</code> (attribute), <code>name:x</code> (special) / 옵션 명세
     * @return the type / 형식
     */
    private Type define(final String name, String parent, Class<?> engine, Maker maker, String[] attributes, String[] methods, String[] options) {
        final Type t = new Type(name, engine, maker);
        types.put(name, t);
        names.put(engine, name);
        final DomBridge.InterfaceInfo info = bridge.defineInterface(name, parent, NodeValue.class, new String[0]);
        t.proto = info.getPrototype();
        members.attributes(t.proto, engine, name, attributes);
        members.methods(t.proto, engine, name, methods);
        // 옵션 명세마다 이름, 종류, 대응하는 Java 메서드를 정한다.
        for (int i = 0; i < options.length; i++) {
            int colon = options[i].indexOf(':');
            String member = options[i].substring(0, colon);
            char kind = options[i].charAt(colon + 1);
            String cap = Character.toUpperCase(member.charAt(0)) + member.substring(1);
            Method m = kind == 'p' ? Idl.find(engine, "get" + cap, 0) : kind == 'a' ? Idl.find(engine, "set" + cap, 1) : null;
            t.options.add(new Option(member, kind, m));
        }
        bridge.setConstructor(name, new NativeFunction(realm, bridge.getInterface(parent).getConstructor(), name, 1, new NativeFunction.Call() {
            /**
             * Calling the constructor without <code>new</code> is an error.
             * <p>
             * <code>new</code> 없이 생성자를 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a node of the type in a context.
             * <p>
             * 컨텍스트에 이 형식의 노드를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the context and the options / 인수: 컨텍스트와 옵션
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Idl.requireCtorArgs(args, 1, name);
                ContextValue c = ContextApi.contextOf(args[0], name);
                JsObject dict = Idl.dictionary(Js.arg(args, 1), "Failed to construct '" + name + "': The provided value");
                NodeValue v;
                try {
                    v = create(c, name, dict);
                } catch (org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException e) {
                    throw api.error(e);
                }
                HostObject w = bridge.wrap(v);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, t.proto));
                return w;
            }
        }));
        return t;
    }

    /**
     * Installs the interfaces of all nodes.
     * <p>
     * 모든 노드의 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        installAudioNode();
        installScheduledSource();
        installDestination();
        installTypes();
        installMediaElementSource();
        installScriptProcessor();
    }

    /**
     * Installs <code>ScriptProcessorNode</code> (deprecated; a script cannot construct it, <code>createScriptProcessor</code> is the way): the read only <code>bufferSize</code> and
     * the <code>onaudioprocess</code> handler. The node is created and wired to its event by {@code ContextApi}.
     * <p>
     * <code>ScriptProcessorNode</code> 를 설치한다(폐기 예정. 스크립트가 생성할 수 없고 <code>createScriptProcessor</code> 가 유일한 방법이다): 읽기 전용 <code>bufferSize</code> 와
     * <code>onaudioprocess</code> 핸들러. 노드의 생성과 이벤트 연결은 {@code ContextApi} 가 한다.
     */
    private void installScriptProcessor() {
        DomBridge.InterfaceInfo info = bridge.defineInterface("ScriptProcessorNode", "AudioNode", NodeValue.class, new String[0]);
        names.put(ScriptProcessorNode.class, "ScriptProcessorNode");
        members.attributes(info.getPrototype(), ScriptProcessorNode.class, "ScriptProcessorNode", "a:bufferSize");
        bridge.getEventHandlers().define(info.getPrototype(), NodeValue.class, new String[] {"audioprocess"});
    }

    /**
     * Installs <code>MediaElementAudioSourceNode</code> (1.22): <code>new MediaElementAudioSourceNode(context, {mediaElement})</code> and
     * <code>AudioContext.createMediaElementSource(element)</code> connect an <code>audio</code> element of the page to the graph of a real-time context: the sound of the element is no
     * longer played to the speakers, it goes through the graph, with the graph as the clock of the element (see {@code MediaElementTap}). An element can be connected once, for good (a
     * second node is an <code>InvalidStateError</code>); a <code>video</code> element is a <code>NotSupportedError</code> (the project has no video playback) and anything that is no
     * media element, or an offline context, is a <code>TypeError</code>. The node holds the element (the read only <code>mediaElement</code>), and the context releases the tap
     * when it is closed.
     * <p>
     * <code>MediaElementAudioSourceNode</code> 를 설치한다(1.22): <code>new MediaElementAudioSourceNode(context, {mediaElement})</code> 와
     * <code>AudioContext.createMediaElementSource(element)</code> 는 페이지의 <code>audio</code> 요소를 실시간 컨텍스트의 그래프에 연결한다: 요소의 소리는 더는 스피커로 재생되지 않고
     * 그래프를 거치며, 그래프가 요소의 시계가 된다({@code MediaElementTap} 참고). 요소는 한 번만, 영구히 연결할 수 있다(두 번째 노드는 <code>InvalidStateError</code>).
     * <code>video</code> 요소는 <code>NotSupportedError</code> 이고(프로젝트에는 비디오 재생이 없다) 미디어 요소가 아닌 것이나 오프라인 컨텍스트는 <code>TypeError</code> 이다.
     * 노드가 요소를 붙잡고(읽기 전용 <code>mediaElement</code>) 컨텍스트는 닫힐 때 탭을 놓는다.
     */
    private void installMediaElementSource() {
        Type t = define("MediaElementAudioSourceNode", "AudioNode", MediaElementAudioSourceNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                throw new IllegalStateException("the node of a media element is created through its context maker");
            }
        }, new String[0], new String[0], new String[] {"mediaElement:x"});
        t.contextMaker = new ContextMaker() {
            /**
             * Connects the element of the options to the graph of the context and creates the node.
             * <p>
             * 옵션의 요소를 컨텍스트의 그래프에 연결하고 노드를 만든다.
             *
             * @param c       the context / 컨텍스트
             * @param options the options / 옵션
             * @return the node value / 노드 값
             */
            public NodeValue make(ContextValue c, JsObject options) {
                return createMediaElementSource(c, required(options, "mediaElement", "MediaElementAudioSourceNode"));
            }
        };
        api.accessor(t.proto, "mediaElement", new WebAudioApi.Op() {
            /**
             * Returns the media element of the node.
             * <p>
             * 노드의 미디어 요소를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element / 요소
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                if (!(v.node() instanceof MediaElementAudioSourceNode)) {
                    throw JsException.typeError("Illegal invocation");
                }
                return v.mediaElement == null ? Js.NULL : v.mediaElement;
            }
        }, null);
    }

    /**
     * Connects a media element to the graph of a context and creates its source node.
     * <p>
     * 미디어 요소를 컨텍스트의 그래프에 연결하고 그 원천 노드를 만든다.
     *
     * @param c       the context / 컨텍스트
     * @param element the script value of the element / 요소의 스크립트 값
     * @return the node value / 노드 값
     */
    private NodeValue createMediaElementSource(ContextValue c, JsValue element) {
        if (c.isOffline()) {
            throw JsException.typeError("Failed to construct 'MediaElementAudioSourceNode': parameter 1 is not of type 'AudioContext'.");
        }
        Element el = (Element) DomBridge.targetOf(element, Element.class);
        boolean media = el != null && Namespaces.HTML.equals(el.getNamespaceURI()) && ("audio".equals(el.getLocalName()) || "video".equals(el.getLocalName()));
        if (!media) {
            throw JsException.typeError("Failed to construct 'MediaElementAudioSourceNode': Failed to read the 'mediaElement' property from 'MediaElementAudioSourceOptions': "
                    + "The provided value is not of type 'HTMLMediaElement'.");
        }
        // 비디오는 재생하지 않으므로 요소를 건드리지 않고 거부한다.
        if ("video".equals(el.getLocalName())) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "Failed to construct 'MediaElementAudioSourceNode': video elements are not supported.");
        }
        if (api.isDisposed() || c.closeRequested) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "Failed to construct 'MediaElementAudioSourceNode': the context is closed or the document is not fully active.");
        }
        MediaElementTap tap;
        try {
            tap = api.page().connectMediaElement(el, c.engine().getSampleRate());
        } catch (IllegalStateException e) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "Failed to construct 'MediaElementAudioSourceNode': HTMLMediaElement already connected previously to a different"
                    + " MediaElementSourceNode");
        } catch (IllegalArgumentException e) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "Failed to construct 'MediaElementAudioSourceNode': " + e.getMessage());
        }
        NodeValue v = wrap(c, new MediaElementAudioSourceNode(c.engine(), tap));
        v.mediaElement = (JsObject) element;
        c.addTap(tap);
        return v;
    }

    /**
     * Installs <code>AudioNode</code>: the attributes that follow from the engine, <code>context</code>, <code>connect</code> and <code>disconnect</code>.
     * <p>
     * <code>AudioNode</code> 를 설치한다: 엔진에서 따라 나오는 속성, <code>context</code>, <code>connect</code>, <code>disconnect</code>.
     */
    private void installAudioNode() {
        DomBridge.InterfaceInfo info = bridge.defineInterface("AudioNode", "EventTarget", NodeValue.class, new String[0]);
        JsObject proto = info.getPrototype();
        members.attributes(proto, AudioNode.class, "AudioNode", "a:numberOfInputs", "a:numberOfOutputs", "a:channelCount=", "a:channelCountMode=",
                "a:channelInterpretation=");
        api.accessor(proto, "context", new WebAudioApi.Op() {
            /**
             * Returns the context of the node.
             * <p>
             * 노드의 컨텍스트를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the context / 컨텍스트
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(nodeOf(self).context());
            }
        }, null);
        nodeOptions.add(new Option("channelCount", 'a', Idl.find(AudioNode.class, "setChannelCount", 1)));
        nodeOptions.add(new Option("channelCountMode", 'a', Idl.find(AudioNode.class, "setChannelCountMode", 1)));
        nodeOptions.add(new Option("channelInterpretation", 'a', Idl.find(AudioNode.class, "setChannelInterpretation", 1)));
        api.method(proto, "connect", 1, new WebAudioApi.Op() {
            /**
             * Connects an output of the node to an input of another node or to a parameter.
             * <p>
             * 노드의 출력을 다른 노드의 입력이나 파라미터에 연결한다.
             *
             * @param self this value / this 값
             * @param args arguments: the destination, the output and the input / 인수: 대상, 출력, 입력
             * @return the destination node, or undefined for a parameter / 대상 노드. 파라미터이면 undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue from = nodeOf(self);
                Idl.requireArgs(args, 1, "connect", "AudioNode");
                int output = args.length > 1 && !args[1].isUndefined() ? Idl.ulong(args[1]) : 0;
                NodeValue to = (NodeValue) DomBridge.targetOf(args[0], NodeValue.class);
                // 대상이 노드이면 입력까지, 파라미터이면 출력만 지정해 연결한다.
                if (to != null) {
                    from.node().connect(to.node(), output, args.length > 2 && !args[2].isUndefined() ? Idl.ulong(args[2]) : 0);
                    return args[0];
                }
                AudioParam p = (AudioParam) DomBridge.targetOf(args[0], AudioParam.class);
                if (p == null) {
                    throw JsException.typeError("Failed to execute 'connect' on 'AudioNode': Overload resolution failed.");
                }
                from.node().connect(p, output);
                return Js.UNDEFINED;
            }
        });
        api.method(proto, "disconnect", 0, new WebAudioApi.Op() {
            /**
             * Disconnects connections of the node: all of them, one output, or those to a destination.
             * <p>
             * 노드의 연결을 끊는다: 모두, 한 출력, 또는 어느 대상으로 가는 것.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue from = nodeOf(self);
                int argc = args.length;
                // 끝의 undefined 는 없는 인수로 본다.
                while (argc > 0 && args[argc - 1].isUndefined()) {
                    argc--;
                }
                AudioNode n = from.node();
                if (argc == 0) {
                    n.disconnect();
                } else if (!args[0].isObject()) {
                    n.disconnect(Idl.ulong(args[0]));
                } else {
                    NodeValue to = (NodeValue) DomBridge.targetOf(args[0], NodeValue.class);
                    AudioParam p = (AudioParam) DomBridge.targetOf(args[0], AudioParam.class);
                    if (to != null) {
                        if (argc == 1) {
                            n.disconnect(to.node());
                        } else if (argc == 2) {
                            n.disconnect(to.node(), Idl.ulong(args[1]));
                        } else {
                            n.disconnect(to.node(), Idl.ulong(args[1]), Idl.ulong(args[2]));
                        }
                    } else if (p != null) {
                        if (argc == 1) {
                            n.disconnect(p);
                        } else {
                            n.disconnect(p, Idl.ulong(args[1]));
                        }
                    } else {
                        throw JsException.typeError("Failed to execute 'disconnect' on 'AudioNode': Overload resolution failed.");
                    }
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>AudioScheduledSourceNode</code>: <code>start</code>, <code>stop</code> and <code>onended</code>.
     * <p>
     * <code>AudioScheduledSourceNode</code> 를 설치한다: <code>start</code>, <code>stop</code>, <code>onended</code>.
     */
    private void installScheduledSource() {
        DomBridge.InterfaceInfo info = bridge.defineInterface("AudioScheduledSourceNode", "AudioNode", NodeValue.class, new String[0]);
        JsObject proto = info.getPrototype();
        bridge.getEventHandlers().define(proto, NodeValue.class, new String[] {"ended"});
        api.method(proto, "start", 0, new WebAudioApi.Op() {
            /**
             * Schedules the node to start.
             * <p>
             * 노드가 시작하도록 예약한다.
             *
             * @param self this value / this 값
             * @param args arguments: the time / 인수: 시간
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                if (!(v.node() instanceof AudioScheduledSourceNode)) {
                    throw JsException.typeError("Illegal invocation");
                }
                ((AudioScheduledSourceNode) v.node()).start(WebAudioApi.optionalDouble(args, 0, 0.0));
                return Js.UNDEFINED;
            }
        });
        api.method(proto, "stop", 0, new WebAudioApi.Op() {
            /**
             * Schedules the node to stop.
             * <p>
             * 노드가 멈추도록 예약한다.
             *
             * @param self this value / this 값
             * @param args arguments: the time / 인수: 시간
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                if (!(v.node() instanceof AudioScheduledSourceNode)) {
                    throw JsException.typeError("Illegal invocation");
                }
                ((AudioScheduledSourceNode) v.node()).stop(WebAudioApi.optionalDouble(args, 0, 0.0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>AudioDestinationNode</code> (a script cannot construct it; every context has one).
     * <p>
     * <code>AudioDestinationNode</code> 를 설치한다(스크립트는 생성할 수 없다. 모든 컨텍스트가 하나를 가진다).
     */
    private void installDestination() {
        DomBridge.InterfaceInfo info = bridge.defineInterface("AudioDestinationNode", "AudioNode", NodeValue.class, new String[0]);
        names.put(AudioDestinationNode.class, "AudioDestinationNode");
        members.attributes(info.getPrototype(), AudioDestinationNode.class, "AudioDestinationNode", "a:maxChannelCount");
    }

    /**
     * Reads a required option of a node, or throws the TypeError that names it.
     * <p>
     * 노드의 필수 옵션을 읽거나 그 이름을 적은 TypeError 를 던진다.
     *
     * @param options the options / 옵션
     * @param member  the member / 멤버
     * @param type    the interface name / 인터페이스 이름
     * @return the value / 값
     */
    private static JsValue required(JsObject options, String member, String type) {
        JsValue v = Idl.member(options, member);
        if (v.isUndefined()) {
            throw JsException.typeError("Failed to construct '" + type + "': required member " + member + " is undefined.");
        }
        return v;
    }

    /**
     * Installs every node type with its members and its options.
     * <p>
     * 모든 노드 형식을 멤버와 옵션과 함께 설치한다.
     */
    private void installTypes() {
        final String[] none = new String[0];
        define("GainNode", "AudioNode", GainNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createGain();
            }
        }, new String[] {"p:gain"}, none, new String[] {"gain:p"});
        define("DelayNode", "AudioNode", DelayNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                JsValue max = Idl.member(o, "maxDelayTime");
                return max.isUndefined() ? e.createDelay() : e.createDelay(Idl.dbl(max));
            }
        }, new String[] {"p:delayTime"}, none, new String[] {"delayTime:p"});
        define("StereoPannerNode", "AudioNode", StereoPannerNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createStereoPanner();
            }
        }, new String[] {"p:pan"}, none, new String[] {"pan:p"});
        define("ChannelSplitterNode", "AudioNode", ChannelSplitterNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                JsValue n = Idl.member(o, "numberOfOutputs");
                return e.createChannelSplitter(n.isUndefined() ? 6 : Idl.ulong(n));
            }
        }, none, none, none);
        define("ChannelMergerNode", "AudioNode", ChannelMergerNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                JsValue n = Idl.member(o, "numberOfInputs");
                return e.createChannelMerger(n.isUndefined() ? 6 : Idl.ulong(n));
            }
        }, none, none, none);
        define("ConstantSourceNode", "AudioScheduledSourceNode", ConstantSourceNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createConstantSource();
            }
        }, new String[] {"p:offset"}, none, new String[] {"offset:p"});
        define("DynamicsCompressorNode", "AudioNode", DynamicsCompressorNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createDynamicsCompressor();
            }
        }, new String[] {"p:threshold", "p:knee", "p:ratio", "p:attack", "p:release", "a:reduction"}, none,
                new String[] {"attack:p", "knee:p", "ratio:p", "release:p", "threshold:p"});
        installOscillator();
        installBufferSource();
        installBiquad();
        installIir();
        installWaveShaper();
        installConvolver();
        installAnalyser();
        installPanner();
    }

    /**
     * Installs the oscillator node interface, <code>OscillatorNode</code>.
     * <p>
     * <code>OscillatorNode</code> 인터페이스를 설치한다.
     */
    private void installOscillator() {
        Type t = define("OscillatorNode", "AudioScheduledSourceNode", OscillatorNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createOscillator();
            }
        }, new String[] {"a:type=", "p:frequency", "p:detune"}, new String[0], new String[] {"detune:p", "frequency:p", "periodicWave:x", "type:a"});
        api.method(t.proto, "setPeriodicWave", 1, new WebAudioApi.Op() {
            /**
             * Sets the periodic wave that the oscillator plays (its type becomes <code>custom</code>).
             * <p>
             * 발진기가 재생할 주기파를 정한다(종류가 <code>custom</code> 이 된다).
             *
             * @param self this value / this 값
             * @param args arguments: the wave / 인수: 파형
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                Idl.requireArgs(args, 1, "setPeriodicWave", "OscillatorNode");
                setPeriodicWave(v, args[0]);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Sets the periodic wave of an oscillator from a script value.
     * <p>
     * 스크립트 값으로 발진기의 주기파를 정한다.
     *
     * @param v the node / 노드
     * @param x the value, a <code>PeriodicWave</code> / 값. <code>PeriodicWave</code>
     */
    private void setPeriodicWave(NodeValue v, JsValue x) {
        PeriodicWave w = (PeriodicWave) DomBridge.targetOf(x, PeriodicWave.class);
        if (w == null) {
            throw JsException.typeError("Failed to execute 'setPeriodicWave' on 'OscillatorNode': parameter 1 is not of type 'PeriodicWave'.");
        }
        ((OscillatorNode) v.node()).setPeriodicWave(w);
    }

    /**
     * Installs <code>AudioBufferSourceNode</code>: the buffer is assigned once and acquired when the source starts.
     * <p>
     * <code>AudioBufferSourceNode</code> 를 설치한다: 버퍼는 한 번만 대입하고 원천이 시작할 때 가져간다.
     */
    private void installBufferSource() {
        Type t = define("AudioBufferSourceNode", "AudioScheduledSourceNode", AudioBufferSourceNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createBufferSource();
            }
        }, new String[] {"p:playbackRate", "p:detune", "a:loop=", "a:loopStart=", "a:loopEnd="}, new String[0],
                new String[] {"buffer:x", "detune:p", "loop:a", "loopEnd:a", "loopStart:a", "playbackRate:p"});
        api.accessor(t.proto, "buffer", new WebAudioApi.Op() {
            /**
             * Returns the buffer that the script assigned.
             * <p>
             * 스크립트가 대입한 버퍼를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the buffer or null / 버퍼. 없으면 null
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                return v.assignedBuffer == null ? Js.NULL : bridge.toJs(v.assignedBuffer);
            }
        }, new WebAudioApi.Op() {
            /**
             * Assigns the buffer.
             * <p>
             * 버퍼를 대입한다.
             *
             * @param self this value / this 값
             * @param args arguments: the buffer / 인수: 버퍼
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                setSourceBuffer(nodeOf(self), Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        api.method(t.proto, "start", 0, new WebAudioApi.Op() {
            /**
             * Schedules the source to start; the content of the buffer is acquired now.
             * <p>
             * 원천이 시작하도록 예약한다. 버퍼의 내용을 지금 가져간다.
             *
             * @param self this value / this 값
             * @param args arguments: the time, the offset and the duration / 인수: 시간, offset, duration
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                AudioBufferSourceNode n = (AudioBufferSourceNode) v.node();
                double when = WebAudioApi.optionalDouble(args, 0, 0.0);
                boolean hasOffset = args.length > 1 && !args[1].isUndefined();
                boolean hasDuration = args.length > 2 && !args[2].isUndefined();
                double offset = hasOffset ? Idl.dbl(args[1]) : 0.0;
                double duration = hasDuration ? Idl.dbl(args[2]) : 0.0;
                // 아직 시작하지 않았다면 이 순간의 버퍼 내용을 가져간다(이미 시작했으면 재생 중인 내용을 건드리지 않고 엔진이 오류를 던진다).
                if (!n.isStarted() && v.assignedBuffer != null) {
                    n.acquireBuffer(v.assignedBuffer.acquire());
                }
                if (hasDuration) {
                    n.start(when, offset, duration);
                } else if (hasOffset) {
                    n.start(when, offset);
                } else {
                    n.start(when);
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Assigns the buffer of a buffer source (the rule <code>[[buffer set]]</code>: a non-null buffer can be assigned once); a source that was started acquires the
     * content of the new buffer at once.
     * <p>
     * 버퍼 원천의 버퍼를 대입한다(<code>[[buffer set]]</code> 규칙: null 이 아닌 버퍼는 한 번만 대입할 수 있다). 이미 시작한 원천은 새 버퍼의 내용을 즉시 가져간다.
     *
     * @param v the node / 노드
     * @param x the value, an <code>AudioBuffer</code> or null / 값. <code>AudioBuffer</code> 또는 null
     */
    private void setSourceBuffer(NodeValue v, JsValue x) {
        AudioBufferSourceNode n = (AudioBufferSourceNode) v.node();
        AudioBufferValue b = x.isNullOrUndefined() ? null : BufferApi.bufferArg(x, "Failed to set the 'buffer' property on 'AudioBufferSourceNode': The provided value");
        if (b != null) {
            if (v.bufferSet) {
                throw api.domError("InvalidStateError", "Failed to set the 'buffer' property on 'AudioBufferSourceNode': Cannot set buffer to non-null after it has been already been set to a non-null buffer");
            }
            v.bufferSet = true;
        }
        v.assignedBuffer = b;
        if (b != null && n.isStarted()) {
            n.acquireBuffer(b.acquire());
        }
    }

    /**
     * Installs the biquad filter node interface, <code>BiquadFilterNode</code>.
     * <p>
     * <code>BiquadFilterNode</code> 인터페이스를 설치한다.
     */
    private void installBiquad() {
        Type t = define("BiquadFilterNode", "AudioNode", BiquadFilterNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createBiquadFilter();
            }
        }, new String[] {"a:type=", "p:frequency", "p:detune", "p:Q", "p:gain"}, new String[0], new String[] {"Q:p", "detune:p", "frequency:p", "gain:p", "type:a"});
        api.method(t.proto, "getFrequencyResponse", 3, new WebAudioApi.Op() {
            /**
             * Calculates the frequency response of the filter.
             * <p>
             * 필터의 주파수 응답을 계산한다.
             *
             * @param self this value / this 값
             * @param args arguments: the frequencies, the magnitudes and the phases / 인수: 주파수, 크기, 위상
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                if (!(v.node() instanceof BiquadFilterNode)) {
                    throw JsException.typeError("Illegal invocation");
                }
                frequencyResponse(v, args, "BiquadFilterNode");
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>IIRFilterNode</code>; its coefficients are required.
     * <p>
     * <code>IIRFilterNode</code> 를 설치한다. 계수는 필수이다.
     */
    private void installIir() {
        Type t = define("IIRFilterNode", "AudioNode", IIRFilterNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                JsValue ff = required(o, "feedforward", "IIRFilterNode");
                JsValue fb = required(o, "feedback", "IIRFilterNode");
                return e.createIIRFilter(Idl.doubleSequence(realm, ff, "The provided value for 'feedforward'"), Idl.doubleSequence(realm, fb, "The provided value for 'feedback'"));
            }
        }, new String[0], new String[0], new String[0]);
        api.method(t.proto, "getFrequencyResponse", 3, new WebAudioApi.Op() {
            /**
             * Calculates the frequency response of the filter.
             * <p>
             * 필터의 주파수 응답을 계산한다.
             *
             * @param self this value / this 값
             * @param args arguments: the frequencies, the magnitudes and the phases / 인수: 주파수, 크기, 위상
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                if (!(v.node() instanceof IIRFilterNode)) {
                    throw JsException.typeError("Illegal invocation");
                }
                frequencyResponse(v, args, "IIRFilterNode");
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Implements <code>getFrequencyResponse(frequencyHz, magResponse, phaseResponse)</code> of the two filters: three <code>Float32Array</code> of the same length.
     * <p>
     * 두 필터의 <code>getFrequencyResponse(frequencyHz, magResponse, phaseResponse)</code> 를 구현한다: 같은 길이의 <code>Float32Array</code> 셋.
     *
     * @param v     the node / 노드
     * @param args  the arguments / 인수
     * @param iface the interface name / 인터페이스 이름
     */
    private void frequencyResponse(NodeValue v, JsValue[] args, String iface) {
        Idl.requireArgs(args, 3, "getFrequencyResponse", iface);
        TypedArrayObject freq = Idl.typedArray(args[0], ElementType.FLOAT32, "Failed to execute 'getFrequencyResponse' on '" + iface + "': parameter 1");
        TypedArrayObject mag = Idl.typedArray(args[1], ElementType.FLOAT32, "Failed to execute 'getFrequencyResponse' on '" + iface + "': parameter 2");
        TypedArrayObject phase = Idl.typedArray(args[2], ElementType.FLOAT32, "Failed to execute 'getFrequencyResponse' on '" + iface + "': parameter 3");
        if (freq.getLength() != mag.getLength() || freq.getLength() != phase.getLength()) {
            throw api.domError("InvalidAccessError", "Failed to execute 'getFrequencyResponse' on '" + iface + "': The arrays provided must be of the same length.");
        }
        float[] f = Idl.floatsOf(freq);
        float[] m = new float[f.length];
        float[] p = new float[f.length];
        if (v.node() instanceof BiquadFilterNode) {
            ((BiquadFilterNode) v.node()).getFrequencyResponse(f, m, p);
        } else {
            ((IIRFilterNode) v.node()).getFrequencyResponse(f, m, p);
        }
        Idl.storeFloats(mag, m, m.length);
        Idl.storeFloats(phase, p, p.length);
    }

    /**
     * Installs <code>WaveShaperNode</code>: the curve is copied when it is set.
     * <p>
     * <code>WaveShaperNode</code> 를 설치한다: 곡선은 정할 때 복사한다.
     */
    private void installWaveShaper() {
        Type t = define("WaveShaperNode", "AudioNode", WaveShaperNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createWaveShaper();
            }
        }, new String[] {"a:oversample="}, new String[0], new String[] {"curve:x", "oversample:a"});
        api.accessor(t.proto, "curve", new WebAudioApi.Op() {
            /**
             * Returns the array that the script assigned as the curve.
             * <p>
             * 스크립트가 곡선으로 대입한 배열을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the array or null / 배열. 없으면 null
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                return v.assignedCurve == null ? Js.NULL : v.assignedCurve;
            }
        }, new WebAudioApi.Op() {
            /**
             * Assigns the curve.
             * <p>
             * 곡선을 대입한다.
             *
             * @param self this value / this 값
             * @param args arguments: the array / 인수: 배열
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                setCurve(nodeOf(self), Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Assigns the curve of a wave shaper: a <code>Float32Array</code> that the node copies, or null.
     * <p>
     * 웨이브셰이퍼의 곡선을 대입한다: 노드가 복사하는 <code>Float32Array</code>, 또는 null.
     *
     * @param v the node / 노드
     * @param x the value / 값
     */
    private void setCurve(NodeValue v, JsValue x) {
        WaveShaperNode n = (WaveShaperNode) v.node();
        if (x.isNullOrUndefined()) {
            n.setCurve(null);
            v.assignedCurve = null;
            return;
        }
        TypedArrayObject a = Idl.typedArray(x, ElementType.FLOAT32, "Failed to set the 'curve' property on 'WaveShaperNode': The provided value");
        n.setCurve(Idl.floatsOf(a));
        v.assignedCurve = a;
    }

    /**
     * Installs <code>ConvolverNode</code>: the buffer is acquired when it is set.
     * <p>
     * <code>ConvolverNode</code> 를 설치한다: 버퍼는 정할 때 가져간다.
     */
    private void installConvolver() {
        Type t = define("ConvolverNode", "AudioNode", ConvolverNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createConvolver();
            }
        }, new String[] {"a:normalize="}, new String[0], new String[] {"disableNormalization:x", "buffer:x"});
        api.accessor(t.proto, "buffer", new WebAudioApi.Op() {
            /**
             * Returns the buffer that the script assigned.
             * <p>
             * 스크립트가 대입한 버퍼를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the buffer or null / 버퍼. 없으면 null
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                return v.assignedBuffer == null ? Js.NULL : bridge.toJs(v.assignedBuffer);
            }
        }, new WebAudioApi.Op() {
            /**
             * Assigns the impulse response.
             * <p>
             * 임펄스 응답을 대입한다.
             *
             * @param self this value / this 값
             * @param args arguments: the buffer / 인수: 버퍼
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                setConvolverBuffer(nodeOf(self), Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Assigns the impulse response of a convolver: the node acquires the content of the buffer now.
     * <p>
     * 컨볼버의 임펄스 응답을 대입한다: 노드가 지금 버퍼의 내용을 가져간다.
     *
     * @param v the node / 노드
     * @param x the value, an <code>AudioBuffer</code> or null / 값. <code>AudioBuffer</code> 또는 null
     */
    private void setConvolverBuffer(NodeValue v, JsValue x) {
        ConvolverNode n = (ConvolverNode) v.node();
        if (x.isNullOrUndefined()) {
            n.setBuffer(null);
            v.assignedBuffer = null;
            return;
        }
        AudioBufferValue b = BufferApi.bufferArg(x, "Failed to set the 'buffer' property on 'ConvolverNode': The provided value");
        n.setBuffer(b.acquire());
        v.assignedBuffer = b;
    }

    /**
     * Installs <code>AnalyserNode</code>; its four data methods write into typed arrays.
     * <p>
     * <code>AnalyserNode</code> 를 설치한다. 데이터 메서드 넷은 형식화 배열에 쓴다.
     */
    private void installAnalyser() {
        Type t = define("AnalyserNode", "AudioNode", AnalyserNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createAnalyser();
            }
        }, new String[] {"a:fftSize=", "a:frequencyBinCount", "a:minDecibels=", "a:maxDecibels=", "a:smoothingTimeConstant="}, new String[0],
                new String[] {"fftSize:a", "maxDecibels:a", "minDecibels:a", "smoothingTimeConstant:a"});
        api.method(t.proto, "getFloatFrequencyData", 1, analyserData(true, true));
        api.method(t.proto, "getByteFrequencyData", 1, analyserData(false, true));
        api.method(t.proto, "getFloatTimeDomainData", 1, analyserData(true, false));
        api.method(t.proto, "getByteTimeDomainData", 1, analyserData(false, false));
    }

    /**
     * Creates the body of one of the four data methods of the analyser.
     * <p>
     * 분석기의 데이터 메서드 넷 중 하나의 본문을 만든다.
     *
     * @param floats    true for a <code>Float32Array</code>, false for a <code>Uint8Array</code> / <code>Float32Array</code> 이면 true, <code>Uint8Array</code> 이면 false
     * @param frequency true for the frequency data, false for the time domain data / 주파수 데이터이면 true, 시간 영역 데이터이면 false
     * @return the operation / 연산
     */
    private WebAudioApi.Op analyserData(final boolean floats, final boolean frequency) {
        final String name = (floats ? "getFloat" : "getByte") + (frequency ? "FrequencyData" : "TimeDomainData");
        return new WebAudioApi.Op() {
            /**
             * Fills the array with the data of the analyser.
             * <p>
             * 분석기의 데이터로 배열을 채운다.
             *
             * @param self this value / this 값
             * @param args arguments: the array / 인수: 배열
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                NodeValue v = nodeOf(self);
                if (!(v.node() instanceof AnalyserNode)) {
                    throw JsException.typeError("Illegal invocation");
                }
                AnalyserNode n = (AnalyserNode) v.node();
                Idl.requireArgs(args, 1, name, "AnalyserNode");
                TypedArrayObject a = Idl.typedArray(args[0], floats ? ElementType.FLOAT32 : ElementType.UINT8, "Failed to execute '" + name + "' on 'AnalyserNode': parameter 1");
                // 배열이 분석기가 줄 수 있는 양보다 길면 앞부분만 채우므로, 임시 배열도 그만큼만 만든다.
                int count = Math.min(a.getLength(), frequency ? n.getFrequencyBinCount() : n.getFftSize());
                if (floats) {
                    float[] tmp = new float[count];
                    if (frequency) {
                        n.getFloatFrequencyData(tmp);
                    } else {
                        n.getFloatTimeDomainData(tmp);
                    }
                    Idl.storeFloats(a, tmp, count);
                } else {
                    byte[] tmp = new byte[count];
                    if (frequency) {
                        n.getByteFrequencyData(tmp);
                    } else {
                        n.getByteTimeDomainData(tmp);
                    }
                    System.arraycopy(tmp, 0, a.getBuffer().getData(), a.getByteOffset(), count);
                }
                return Js.UNDEFINED;
            }
        };
    }

    /**
     * Installs the panner node interface, <code>PannerNode</code>.
     * <p>
     * <code>PannerNode</code> 인터페이스를 설치한다.
     */
    private void installPanner() {
        define("PannerNode", "AudioNode", PannerNode.class, new Maker() {
            public AudioNode make(BaseAudioContext e, JsObject o) {
                return e.createPanner();
            }
        }, new String[] {"p:positionX", "p:positionY", "p:positionZ", "p:orientationX", "p:orientationY", "p:orientationZ", "a:panningModel=", "a:distanceModel=",
            "a:refDistance=", "a:maxDistance=", "a:rolloffFactor=", "a:coneInnerAngle=", "a:coneOuterAngle=", "a:coneOuterGain="},
                new String[] {"setPosition", "setOrientation"}, new String[] {"coneInnerAngle:a", "coneOuterAngle:a", "coneOuterGain:a", "distanceModel:a", "maxDistance:a",
                    "orientationX:p", "orientationY:p", "orientationZ:p", "panningModel:a", "positionX:p", "positionY:p", "positionZ:p", "refDistance:a", "rolloffFactor:a"});
    }
}
