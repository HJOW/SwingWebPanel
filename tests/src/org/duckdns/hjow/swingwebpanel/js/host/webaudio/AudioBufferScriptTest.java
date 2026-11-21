package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>AudioBuffer</code> in scripts (plan item P25-05; Web Audio API 1.4): that <code>getChannelData()</code> gives the <em>same</em> <code>Float32Array</code> every time and
 * that what the script writes into it is the content of the buffer, <code>copyFromChannel</code> and <code>copyToChannel</code> with their offsets and truncation, and the rule of
 * the specification about <b>acquiring the content</b>: a node that started with a buffer plays the data of that moment, whatever the script writes into the array afterwards, and a
 * source that is started later with the same buffer plays the new data. The tests render with an <code>OfflineAudioContext</code> and read the result back.
 * <p>
 * 스크립트의 <code>AudioBuffer</code> 시험(계획 P25-05. Web Audio API 1.4): <code>getChannelData()</code> 가 매번 <em>같은</em> <code>Float32Array</code> 를 주고 스크립트가 거기에 쓴 것이 버퍼의
 * 내용인지, offset 과 잘림이 있는 <code>copyFromChannel</code>, <code>copyToChannel</code>, 그리고 <b>내용 가져오기</b>에 관한 규격의 규칙: 버퍼로 시작한 노드는 그 순간의 데이터를 재생하며
 * 나중에 스크립트가 배열에 무엇을 쓰든 상관없고, 같은 버퍼로 나중에 시작한 원천은 새 데이터를 재생한다. 시험은 <code>OfflineAudioContext</code> 로 렌더링하고 결과를 읽는다.
 */
public class AudioBufferScriptTest {

    /**
     * The page of the case.
     * <p>
     * 사례의 페이지.
     */
    private WebAudioPage page;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AudioBufferScriptTest() {
    }

    /**
     * Loads a page with the helpers.
     * <p>
     * 도우미가 있는 페이지를 로드한다.
     */
    public void setUp() {
        page = new WebAudioPage();
        page.run("var result = {done: false}; function go(ctx) { ctx.startRendering().then(function (b) { result.buf = b; result.done = true; }); }");
    }

    /**
     * Closes what the case opened.
     * <p>
     * 사례가 연 것을 닫는다.
     */
    public void tearDown() {
        page.fixture.scripts.dispose();
    }

    /**
     * The array of a channel is the same object every time, is a <code>Float32Array</code> of the length of the buffer on its own <code>ArrayBuffer</code>, and what is written into it is
     * seen by <code>copyFromChannel</code> (and the other way round with <code>copyToChannel</code>).
     * <p>
     * 채널의 배열은 매번 같은 객체이며 버퍼 길이의 <code>Float32Array</code> 로 자기 <code>ArrayBuffer</code> 를 가지고, 거기에 쓴 것을 <code>copyFromChannel</code> 이 본다(반대로
     * <code>copyToChannel</code> 이 쓴 것을 배열이 본다).
     */
    public void testChannelDataIsTheSameArray() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var b = ctx.createBuffer(2, 8, 8000); var a0 = b.getChannelData(0), a1 = b.getChannelData(1);");
        Check.equal("true:true:8:32:false", page.run("[a0 === b.getChannelData(0), a1 === b.getChannelData(1), a0.length, a0.byteLength, a0 === a1].join(':')"), "the same array each time");
        Check.equal("true:Float32Array", page.run("[a0 instanceof Float32Array, Object.prototype.toString.call(a0).slice(8, -1)].join(':')"), "a Float32Array");
        page.run("a0[3] = 0.5; a1[7] = -0.25;");
        Check.equal("0.5:-0.25", page.run("var dst = new Float32Array(8); b.copyFromChannel(dst, 0); var dst1 = new Float32Array(8); b.copyFromChannel(dst1, 1); [dst[3], dst1[7]].join(':')"),
                "a write into the array is the content of the buffer");
        page.run("b.copyToChannel(new Float32Array([1, 2, 3]), 0, 2);");
        Check.equal("0:0:1:2:3:0", page.run("[a0[0], a0[1], a0[2], a0[3], a0[4], a0[5]].join(':')"), "copyToChannel is seen by the array");
        // 끝을 넘는 복사: 들어가는 만큼만 복사하고, offset 이 길이 이상이면 아무것도 복사하지 않는다(오류가 아니다).
        page.run("b.copyToChannel(new Float32Array([7, 8, 9, 10]), 1, 6); b.copyToChannel(new Float32Array([5]), 1, 8); b.copyToChannel(new Float32Array([5]), 1, 4294967295);");
        Check.equal("7:8:0:0", page.run("var a1b = b.getChannelData(1); [a1b[6], a1b[7], a1b[5], a1b[0]].join(':')"), "the copy stops at the end of the channel");
        page.run("var small = new Float32Array(3); b.copyFromChannel(small, 0, 2); var view = new Float32Array(new ArrayBuffer(32), 8, 2); b.copyFromChannel(view, 0, 3); var tail = new Float32Array(4); b.copyFromChannel(tail, 0, 6);");
        Check.equal("1:2:3", page.run("small.join(':')"), "copyFromChannel with an offset");
        Check.equal("2:3", page.run("view.join(':')"), "copyFromChannel into a view with a byte offset");
        Check.equal("0:0:0:0", page.run("tail.join(':')"), "copyFromChannel near the end copies what is there");
        Check.equal("IndexSizeError", page.errorOf("b.copyFromChannel(new Float32Array(1), 2)"), "a channel out of range");
        Check.equal("IndexSizeError", page.errorOf("b.copyToChannel(new Float32Array(1), 2)"), "a channel out of range");
        Check.equal("TypeError", page.errorOf("b.copyFromChannel(new Float64Array(1), 0)"), "a Float64Array is not a Float32Array");
        Check.equal("TypeError", page.errorOf("b.copyFromChannel(null, 0)"), "null");
        Check.equal("TypeError", page.errorOf("b.getChannelData()"), "a missing argument");
    }

    /**
     * An <code>ArrayBuffer</code> of a channel that a script moves to somewhere else (a transfer detaches it) does not break the buffer: the next call returns a fresh array with the
     * same content.
     * <p>
     * 스크립트가 채널의 <code>ArrayBuffer</code> 를 다른 곳으로 옮겨도(전달하면 분리된다) 버퍼는 깨지지 않는다: 다음 호출은 같은 내용의 새 배열을 반환한다.
     */
    public void testDetachedChannelArray() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var b = ctx.createBuffer(1, 4, 8000); var a = b.getChannelData(0); a[2] = 0.75; var moved = structuredClone(a.buffer, {transfer: [a.buffer]});");
        Check.equal("0:true", page.run("[a.length, moved.byteLength === 16].join(':')"), "the old array is detached");
        Check.equal("4:0.75:false", page.run("var a2 = b.getChannelData(0); [a2.length, a2[2], a2 === a].join(':')"), "a fresh array with the same content");
        page.run("a2[1] = 0.5; var ch = new Float32Array(4); b.copyFromChannel(ch, 0);");
        Check.equal("0.5:0.75", page.run("[ch[1], ch[2]].join(':')"), "and it is the live content");
    }

    /**
     * Acquiring the content: a source that was started plays the data of the moment of <code>start()</code>; the script may change the array afterwards (it is not detached) and a source
     * that is started later with the same buffer plays the new data. A buffer that is assigned after <code>start()</code> is acquired when it is assigned.
     * <p>
     * 내용 가져오기: 시작한 원천은 <code>start()</code> 순간의 데이터를 재생한다. 스크립트는 나중에 배열을 바꿀 수 있고(분리되지 않는다) 같은 버퍼로 나중에 시작한 원천은 새 데이터를
     * 재생한다. <code>start()</code> 뒤에 대입한 버퍼는 대입할 때 가져간다.
     */
    public void testAcquireTheContent() {
        page.run("var ctx = new OfflineAudioContext(1, 4096, 8000); var b = ctx.createBuffer(1, 128, 8000); var data = b.getChannelData(0); data.fill(0.25);"
                + " var s1 = ctx.createBufferSource(); s1.buffer = b; s1.connect(ctx.destination); s1.start(0); data.fill(0.5);"
                + " var s2 = ctx.createBufferSource(); s2.buffer = b; s2.connect(ctx.destination); s2.start(1024 / 8000); data.fill(0.75);"
                + " var s3 = ctx.createBufferSource(); s3.connect(ctx.destination); s3.start(2048 / 8000); s3.buffer = b; data.fill(1);");
        page.run("go(ctx); 1");
        page.waitFor("result.done", 10000, "the rendering ends");
        Check.equal("0.25:0.25:0.5:0.5:0.75:0.75", page.run("var o = result.buf.getChannelData(0); [o[0], o[127], o[1024], o[1151], o[2048], o[2175]].join(':')"),
                "each source plays the data of its start (s1), of its start (s2) and of the assignment after start (s3)");
        Check.equal("0:0:0", page.run("[o[128], o[1152], o[2176]].join(':')"), "and then silence");
        Check.equal("true:1", page.run("String(data === b.getChannelData(0)) + ':' + data[0]"), "the array stays valid and writable");
    }

    /**
     * The rule <code>[[buffer set]]</code>: a buffer that is not null can be assigned to a source once; null is always allowed; the attribute returns the object that was assigned;
     * <code>start()</code> twice and invalid arguments are errors; a buffer source without a buffer is silent.
     * <p>
     * <code>[[buffer set]]</code> 규칙: null 이 아닌 버퍼는 원천에 한 번만 대입할 수 있고, null 은 언제나 된다. 속성은 대입한 객체를 반환한다. <code>start()</code> 를 두 번 부르는 것과 잘못된 인수는
     * 오류이다. 버퍼가 없는 버퍼 원천은 무음이다.
     */
    public void testBufferSetOnce() {
        page.run("var ctx = new OfflineAudioContext(1, 256, 8000); var b = ctx.createBuffer(1, 16, 8000); var b2 = ctx.createBuffer(1, 16, 8000); var s = ctx.createBufferSource();");
        Check.equal("null", page.run("String(s.buffer)"), "no buffer at first");
        Check.equal("true", page.run("s.buffer = b; String(s.buffer === b)"), "the attribute returns the object");
        Check.equal("InvalidStateError", page.errorOf("s.buffer = b2"), "a second non-null buffer");
        Check.equal("no error", page.errorOf("s.buffer = null"), "null is always allowed");
        Check.equal("null", page.run("String(s.buffer)"), "and it clears the attribute");
        Check.equal("InvalidStateError", page.errorOf("s.buffer = b"), "the rule remembers that a buffer was set");
        Check.equal("TypeError", page.errorOf("ctx.createBufferSource().buffer = {}"), "not an AudioBuffer");
        Check.equal("TypeError", page.errorOf("ctx.createBufferSource().buffer = new Float32Array(4)"), "a typed array is not an AudioBuffer");
        Check.equal("RangeError", page.errorOf("ctx.createBufferSource().start(0, -1)"), "a negative offset");
        Check.equal("RangeError", page.errorOf("ctx.createBufferSource().start(0, 0, -1)"), "a negative duration");
        Check.equal("TypeError", page.errorOf("ctx.createBufferSource().start(0, NaN)"), "a non-finite offset");
        Check.equal("InvalidStateError", page.errorOf("var t = ctx.createBufferSource(); t.start(); t.start()"), "start twice");
        page.run("var silent = ctx.createBufferSource(); silent.connect(ctx.destination); silent.start(); go(ctx);");
        page.waitFor("result.done", 10000, "the rendering ends");
        Check.near(0.0, page.number("result.buf.getChannelData(0)[10]"), 0, "a buffer source without a buffer is silent");
    }

    /**
     * The buffer that a rendering produces can be read and changed like any other buffer, and a buffer that a script filled can be played by a source of another context of the same
     * sample rate.
     * <p>
     * 렌더링이 만든 버퍼는 다른 버퍼와 똑같이 읽고 바꿀 수 있고, 스크립트가 채운 버퍼는 같은 샘플링 주파수의 다른 컨텍스트의 원천이 재생할 수 있다.
     */
    public void testRenderedBufferIsAnOrdinaryBuffer() {
        page.run("var ctx = new OfflineAudioContext(1, 256, 8000); var c = ctx.createConstantSource(); c.offset.value = 0.5; c.connect(ctx.destination); c.start(); go(ctx);");
        page.waitFor("result.done", 10000, "the rendering ends");
        page.run("var r = result.buf; var ch = r.getChannelData(0); var first = ch[0]; ch[0] = 0.125; var ctx2 = new OfflineAudioContext(1, 256, 8000); var s = ctx2.createBufferSource(); s.buffer = r;"
                + " s.connect(ctx2.destination); s.start(); result.done = false; go(ctx2);");
        page.waitFor("result.done", 10000, "the second rendering ends");
        Check.equal("0.5:0.125:0.5", page.run("var o = result.buf.getChannelData(0); [first, o[0], o[1]].join(':')"), "the rendered buffer was changed by the script and played by another context");
    }
}
