package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.decode.AudioBufferDecoder;
import org.duckdns.hjow.swingwebpanel.media.webaudio.decode.DecodeResult;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of <code>decodeAudioData</code> through the script interface for the files that the wide decoding path of the decoders (P25-05, step B2) handles: damaged and cut-off files (the
 * decoder returns what it decoded, the promise resolves, the buffer is shorter and a warning reaches the console of the page; a file without a single frame is an <code>EncodingError</code>),
 * and files with more than two channels and with more precision than 16 bits (a 5.1 buffer has six channels, a 7.1 FLAC file of 24 bits keeps its precision, a 24-bit and a float WAV file keep their
 * exact values, 32 channels are the most). The inputs are the files of <code>tests/resources/media/audio</code> (real encoders, so a decoder is tested against what encoders write) and WAV files that
 * a script builds sample by sample (so the expected values do not depend on a decoder). The page tests run on a page with a manual clock; one test runs the whole path in the real component and
 * observes the console listener on the event thread.
 * <p>
 * B2 단계의 디코더의 넓은 디코딩 경로(P25-05)가 다루는 파일에 대한 스크립트 인터페이스를 통한 <code>decodeAudioData</code> 시험: 손상되었거나 잘린 파일(디코더가 디코딩한 만큼을 돌려주고
 * 프라미스는 resolve 되며 버퍼는 더 짧고 경고가 페이지의 콘솔에 닿는다. 프레임이 하나도 없는 파일은 <code>EncodingError</code>), 그리고 채널이 둘을 넘거나 정밀도가 16비트를 넘는 파일(5.1 버퍼는
 * 채널이 여섯, 24비트 7.1 FLAC 은 정밀도를 유지, 24비트와 float WAV 는 정확한 값을 유지, 최대 32채널). 입력은 <code>tests/resources/media/audio</code> 의 파일(실제 인코더가 만든 것이므로
 * 디코더를 인코더가 쓴 것과 비교한다)과 스크립트가 표본 단위로 만드는 WAV 파일(기대값이 디코더에 의존하지 않는다)이다. 페이지 시험은 수동 시계의 페이지에서 돌고, 시험 하나는 실제 컴포넌트에서
 * 전체 경로를 돌려 콘솔 리스너를 이벤트 스레드에서 관찰한다.
 */
public class DecodeDamagedDataTest {

    /**
     * The long files below <code>trunc</code>: name and sample rate (every one is stereo and two seconds long).
     * <p>
     * <code>trunc</code> 아래의 긴 파일: 이름과 샘플링 주파수(모두 스테레오이고 2 초이다).
     */
    private static final Object[][] LONG_FILES = {{"long_vorbis_44100_st.ogg", Integer.valueOf(44100)}, {"long_opus_48000_st.opus", Integer.valueOf(48000)},
            {"long_flac_8000_st.flac", Integer.valueOf(8000)}, {"long_oggflac_8000_st.oga", Integer.valueOf(8000)}, {"long_mp3_44100_st.mp3", Integer.valueOf(44100)},
            {"long_aac_44100_st.aac", Integer.valueOf(44100)}};

    /**
     * The prefix of the console line that the warning of a damaged file makes in the fixture (<code>level: text</code>).
     * <p>
     * 손상된 파일의 경고가 시험 도구에 만드는 콘솔 줄의 접두사(<code>수준: 문자열</code>).
     */
    private static final String WARN_PREFIX = "warn: decodeAudioData: ";

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
    public DecodeDamagedDataTest() {
    }

    /**
     * Loads a page with the helpers: the bytes of a file arrive as a hex text, <code>dec</code> decodes one and records what came out (the state of the promise, the buffer, which callback ran),
     * <code>pcmWav</code> builds a WAV file of the extensible form with the samples of rows, <code>pcm16Of</code> finds the samples of a 16-bit file, <code>maxDiff</code> compares two channels.
     * <p>
     * 도우미가 있는 페이지를 로드한다: 파일의 바이트는 16진수 문자열로 오고, <code>dec</code> 는 파일 하나를 디코딩해 나온 것(프라미스의 상태, 버퍼, 불린 콜백)을 기록하고,
     * <code>pcmWav</code> 는 행의 표본이 든 확장 형식의 WAV 파일을 만들고, <code>pcm16Of</code> 는 16비트 파일의 표본을 찾고, <code>maxDiff</code> 는 두 채널을 비교한다.
     */
    public void setUp() {
        page = new WebAudioPage();
        // 시작하지 않은 오프라인 컨텍스트는 페이지당 16 개까지이므로 샘플링 주파수마다 컨텍스트 하나를 만들어 모든 디코딩에 쓴다.
        page.run("var H = {}, res = {}, ctxs = {}; function ctxFor(rate) { return ctxs[rate] || (ctxs[rate] = new OfflineAudioContext(2, 128, rate)); }"
                + " function hex2buf(h) { var n = h.length / 2, u = new Uint8Array(n); for (var i = 0; i < n; i++) { u[i] = parseInt(h.substr(i * 2, 2), 16); } return u.buffer; }"
                + " function dec(key, ctx) { var r = res[key] = {state: 'pending', cb: '', buf: null};"
                + " ctx.decodeAudioData(hex2buf(H[key]), function (b) { r.cb = 'success'; console.log('success:' + key); }, function (e) { r.cb = 'error:' + e.name; })"
                + " .then(function (b) { r.state = 'ok'; r.buf = b; }, function (e) { r.state = 'error:' + e.name; r.err = e; }); }"
                + " function maxDiff(x, y) { var m = 0, n = Math.min(x.length, y.length); for (var i = 0; i < n; i++) { m = Math.max(m, Math.abs(x[i] - y[i])); } return m; }"
                + " function pcm16Of(ab) { var v = new DataView(ab), pos = 12; while (pos + 8 <= v.byteLength) {"
                + " var id = String.fromCharCode(v.getUint8(pos), v.getUint8(pos + 1), v.getUint8(pos + 2), v.getUint8(pos + 3)), size = v.getUint32(pos + 4, true);"
                + " if (id === 'data') { return {view: v, base: pos + 8, size: size}; } pos += 8 + size + (size & 1); } return null; }"
                + " function pcmWav(rate, ch, bits, isFloat, mask, rows) { var bps = bits / 8, n = rows.length, dataBytes = n * ch * bps, buf = new ArrayBuffer(68 + dataBytes), v = new DataView(buf);"
                + " function s(o, t) { for (var i = 0; i < t.length; i++) { v.setUint8(o + i, t.charCodeAt(i)); } }"
                + " s(0, 'RIFF'); v.setUint32(4, buf.byteLength - 8, true); s(8, 'WAVE'); s(12, 'fmt '); v.setUint32(16, 40, true); v.setUint16(20, 0xFFFE, true); v.setUint16(22, ch, true);"
                + " v.setUint32(24, rate, true); v.setUint32(28, rate * ch * bps, true); v.setUint16(32, ch * bps, true); v.setUint16(34, bits, true); v.setUint16(36, 22, true);"
                + " v.setUint16(38, bits, true); v.setUint32(40, mask, true); v.setUint16(44, isFloat ? 3 : 1, true);"
                + " var tail = [0, 0, 0, 0, 0x10, 0, 0x80, 0, 0, 0xaa, 0, 0x38, 0x9b, 0x71]; for (var t = 0; t < tail.length; t++) { v.setUint8(46 + t, tail[t]); }"
                + " s(60, 'data'); v.setUint32(64, dataBytes, true); var o = 68;"
                + " for (var i = 0; i < n; i++) { for (var c = 0; c < ch; c++) { var x = rows[i][c];"
                + " if (isFloat) { v.setFloat32(o, x, true); } else if (bits === 16) { v.setInt16(o, x, true); }"
                + " else { var u = x & 0xFFFFFF; v.setUint8(o, u & 255); v.setUint8(o + 1, (u >> 8) & 255); v.setUint8(o + 2, (u >> 16) & 255); } o += bps; } } return buf; }");
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
     * Reads a test resource below <code>/media/audio</code> completely.
     * <p>
     * <code>/media/audio</code> 아래의 시험 자원을 통째로 읽는다.
     *
     * @param name the path below the directory / 디렉터리 아래의 경로
     * @return its bytes / 바이트
     * @throws IOException if it is missing / 없으면
     */
    private static byte[] resource(String name) throws IOException {
        InputStream in = DecodeDamagedDataTest.class.getResourceAsStream("/media/audio/" + name);
        if (in == null) {
            throw new IOException("missing test resource " + name);
        }
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[65536];
            int r;
            // 끝까지 읽는다.
            while ((r = in.read(b)) > 0) {
                o.write(b, 0, r);
            }
            return o.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Writes bytes as a hex text (two digits for each byte), which a script turns back into an <code>ArrayBuffer</code>.
     * <p>
     * 바이트를 16진수 문자열(바이트마다 두 자리)로 쓴다. 스크립트가 이를 <code>ArrayBuffer</code> 로 되돌린다.
     *
     * @param data the bytes / 바이트
     * @return the text / 문자열
     */
    private static String hex(byte[] data) {
        StringBuilder s = new StringBuilder(data.length * 2);
        final String digits = "0123456789abcdef";
        // 바이트마다 16진수 두 자리를 쓴다.
        for (int i = 0; i < data.length; i++) {
            s.append(digits.charAt((data[i] >> 4) & 15)).append(digits.charAt(data[i] & 15));
        }
        return s.toString();
    }

    /**
     * Gives the bytes to the page under a key and starts the decoding in the context of the sample rate of the file (so that the sample rate does not change the frames; one context per rate serves
     * all decodings, as a page may have only 16 offline contexts that have not rendered).
     * <p>
     * 바이트를 키로 페이지에 넘기고 파일의 샘플링 주파수의 컨텍스트에서 디코딩을 시작한다(샘플링 주파수가 프레임을 바꾸지 않도록. 렌더링하지 않은 오프라인 컨텍스트는 페이지당 16 개까지이므로
     * 샘플링 주파수마다 컨텍스트 하나가 모든 디코딩에 쓰인다).
     *
     * @param key  the key / 키
     * @param data the bytes of the file / 파일의 바이트
     * @param rate the sample rate of the context / 컨텍스트의 샘플링 주파수
     */
    private void start(String key, byte[] data, int rate) {
        page.run("H['" + key + "'] = '" + hex(data) + "'; dec('" + key + "', ctxFor(" + rate + ")); 1");
    }

    /**
     * Waits until the decoding of a key ended and returns the state of the promise (<code>ok</code> or <code>error:Name</code>).
     * <p>
     * 키의 디코딩이 끝날 때까지 기다리고 프라미스의 상태(<code>ok</code> 또는 <code>error:이름</code>)를 반환한다.
     *
     * @param key the key / 키
     * @return the state / 상태
     */
    private String finish(String key) {
        page.waitFor("res['" + key + "'].state !== 'pending'", 30000, "the decoding of " + key + " ends");
        return page.run("res['" + key + "'].state");
    }

    /**
     * Returns the sum of <code>value * (frame + 1)</code> over the frames of a channel of a buffer of the engine (a checksum that the page computes the same way, in double precision).
     * <p>
     * 엔진 버퍼의 채널의 프레임에 대한 <code>값 * (프레임 + 1)</code> 의 합을 반환한다(페이지가 같은 방식으로 배정밀도로 계산하는 체크섬).
     *
     * @param b the buffer / 버퍼
     * @param c the channel / 채널
     * @return the checksum / 체크섬
     */
    private static double checksum(AudioBuffer b, int c) {
        float[] d = b.getChannelData(c);
        double s = 0;
        // 프레임마다 값에 위치를 곱해 더한다.
        for (int i = 0; i < d.length; i++) {
            s += (double) d[i] * (i + 1);
        }
        return s;
    }

    /**
     * Decodes the bytes in the page and checks the outcome against what the decoder of the engine returns for the same bytes: the promise resolves, the success callback runs, the buffer has the same
     * shape and the same samples (a checksum of each channel), and a warning is on the console of the page if and only if the engine marked the result truncated, with the text of the engine.
     * <p>
     * 바이트를 페이지에서 디코딩하고 결과를 엔진의 디코더가 같은 바이트에 돌려주는 것과 대조한다: 프라미스가 resolve 되고, 성공 콜백이 불리고, 버퍼의 모양과 표본(채널마다의 체크섬)이 같고,
     * 엔진이 결과를 잘렸다고 표시했을 때에만 엔진의 문구로 경고가 페이지의 콘솔에 있다.
     *
     * @param key  the key / 키
     * @param data the bytes of the file / 파일의 바이트
     * @param rate the sample rate / 샘플링 주파수
     * @return what the decoder of the engine returned / 엔진의 디코더가 돌려준 것
     */
    private DecodeResult decodeAndCompare(String key, byte[] data, int rate) {
        DecodeResult expect = AudioBufferDecoder.decodeResult(data, null, rate, null);
        start(key, data, rate);
        Check.equal("ok", finish(key), key + ": the promise resolves");
        Check.equal("success", page.run("res['" + key + "'].cb"), key + ": the success callback runs");
        Check.equal(expect.getBuffer().getLength(), (int) page.number("res['" + key + "'].buf.length"), key + ": the frames");
        Check.equal(expect.getBuffer().getNumberOfChannels(), (int) page.number("res['" + key + "'].buf.numberOfChannels"), key + ": the channels");
        // 채널마다 체크섬이 같은지 확인한다.
        for (int c = 0; c < expect.getBuffer().getNumberOfChannels(); c++) {
            double got = page.number("var d = res['" + key + "'].buf.getChannelData(" + c + "), s = 0; for (var i = 0; i < d.length; i++) { s += d[i] * (i + 1); } s");
            Check.isTrue(Math.abs(got - checksum(expect.getBuffer(), c)) < 1e-6 * Math.max(1.0, Math.abs(got)), key + ": the samples of channel " + c + " (" + got + " against "
                    + checksum(expect.getBuffer(), c) + ")");
        }
        boolean warned = page.fixture.console.contains(WARN_PREFIX + expect.getWarning());
        Check.equal(expect.isTruncated(), warned, key + ": the warning is on the console if and only if the result is truncated (" + expect.getWarning() + "): " + page.fixture.console);
        return expect;
    }

    /**
     * A FLAC file that is cut off (its header announces the length, so the decoder knows that frames are missing): the promise resolves with what could be decoded, the buffer is shorter than the whole
     * file's, the success callback runs after a warning that tells how much is missing, and the whole file makes no warning.
     * <p>
     * 잘린 FLAC 파일(머리가 길이를 알리므로 디코더는 프레임이 빠진 것을 안다): 프라미스는 디코딩할 수 있었던 만큼으로 resolve 되고, 버퍼는 전체 파일의 것보다 짧고, 성공 콜백은 얼마나 빠졌는지
     * 알리는 경고 뒤에 불리며, 전체 파일은 경고를 만들지 않는다.
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testCutFlacResolvesWithAWarning() throws Exception {
        byte[] data = resource("trunc/long_flac_8000_st.flac");
        DecodeResult whole = decodeAndCompare("whole", data, 8000);
        Check.isFalse(whole.isTruncated(), "the whole file is not truncated");
        Check.equal(16000, whole.getBuffer().getLength(), "the whole file has two seconds");
        Check.equal(Collections.emptyList().toString(), warnLines().toString(), "the whole file makes no warning");
        DecodeResult cut = decodeAndCompare("cut", Arrays.copyOf(data, data.length * 70 / 100), 8000);
        Check.isTrue(cut.isTruncated(), "the cut file is marked truncated by the engine");
        int frames = (int) page.number("res.cut.buf.length");
        Check.isTrue(frames > 0 && frames < 16000, "a part of the frames: " + frames);
        List<String> warnings = warnLines();
        Check.equal(1, warnings.size(), "one warning: " + warnings);
        // 경고는 성공 콜백보다 먼저 콘솔에 닿아야 한다(같은 작업 안에서 onWarning 이 onSuccess 앞에 불린다).
        int warning = page.fixture.console.indexOf(warnings.get(0));
        int success = page.fixture.console.indexOf("log: success:cut");
        Check.isTrue(warning >= 0 && success > warning, "the warning comes before the success callback: " + page.fixture.console);
    }

    /**
     * Returns the lines of the console that are warnings of <code>decodeAudioData</code>.
     * <p>
     * 콘솔 줄 중 <code>decodeAudioData</code> 의 경고인 것을 반환한다.
     *
     * @return the lines / 줄
     */
    private List<String> warnLines() {
        List<String> lines = new ArrayList<String>();
        // 콘솔 줄 중 경고 접두사로 시작하는 것을 모은다.
        for (String line : page.fixture.console) {
            if (line.startsWith(WARN_PREFIX)) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Every long file of the formats cut at 70 percent resolves with fewer frames than the whole file, and what the page gets equals what the engine decoder returns for the same bytes; the formats whose
     * header does not announce the length (Ogg, MP3, AAC) are short files for the decoder, so only the FLAC file warns (the page reports a warning for a result exactly when the engine marks it truncated).
     * <p>
     * 긴 파일을 70 % 에서 자른 것은 모든 형식에서 전체 파일보다 적은 프레임으로 resolve 되고, 페이지가 받는 것은 엔진 디코더가 같은 바이트에 돌려주는 것과 같다. 머리가 길이를 알리지 않는
     * 형식(Ogg, MP3, AAC)은 디코더에게 짧은 파일이므로 FLAC 파일만 경고한다(페이지는 엔진이 결과를 잘렸다고 표시할 때에만 경고를 보고한다).
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testCutFilesOfEveryFormatResolveShorter() throws Exception {
        int warnings = 0;
        // 형식마다 전체 길이를 구하고 70 % 에서 자른 사본을 페이지에서 디코딩해 대조한다.
        for (int i = 0; i < LONG_FILES.length; i++) {
            String name = (String) LONG_FILES[i][0];
            int rate = ((Integer) LONG_FILES[i][1]).intValue();
            byte[] data = resource("trunc/" + name);
            int whole = AudioBufferDecoder.decodeResult(data, null, rate, null).getBuffer().getLength();
            DecodeResult cut = decodeAndCompare("c" + i, Arrays.copyOf(data, data.length * 70 / 100), rate);
            Check.isTrue(cut.getBuffer().getLength() > 0 && cut.getBuffer().getLength() < whole, name + ": fewer frames than the whole file (" + cut.getBuffer().getLength() + " of " + whole + ")");
            Check.equal(cut.getBuffer().getLength(), (int) page.number("res.c" + i + ".buf.length"), name + ": the page has the frames of the engine");
            warnings += cut.isTruncated() ? 1 : 0;
        }
        Check.isTrue(warnings >= 1, "at least the FLAC file announces its length, so one result warns");
        Check.equal(warnings, warnLines().size(), "the warnings of the page are the warnings of the engine: " + warnLines());
    }

    /**
     * A damaged block (300 zero bytes in the middle of the file): the decoder returns what it decoded before the damage or conceals the damage, and the page gets the same buffer and the same warning as
     * the engine returns; the Vorbis file is the case in which the engine reports the damage, so a warning must be on the console for it.
     * <p>
     * 손상된 블록(파일 가운데의 0 바이트 300 개): 디코더는 손상 앞까지 디코딩한 것을 돌려주거나 손상을 가린다. 페이지는 엔진이 돌려주는 것과 같은 버퍼와 같은 경고를 받는다.
     * Vorbis 파일은 엔진이 손상을 알리는 경우이므로 콘솔에 경고가 있어야 한다.
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testDamagedBlockInTheMiddle() throws Exception {
        boolean vorbisWarned = false;
        // 형식마다 가운데 300 바이트를 0 으로 만든 사본을 페이지에서 디코딩해 대조한다.
        for (int i = 0; i < LONG_FILES.length; i++) {
            String name = (String) LONG_FILES[i][0];
            int rate = ((Integer) LONG_FILES[i][1]).intValue();
            byte[] bad = resource("trunc/" + name);
            Arrays.fill(bad, bad.length / 2, bad.length / 2 + 300, (byte) 0);
            DecodeResult r = decodeAndCompare("d" + i, bad, rate);
            if (name.indexOf("vorbis") >= 0) {
                vorbisWarned = r.isTruncated();
            }
        }
        Check.isTrue(vorbisWarned, "the damaged Vorbis file is reported by the engine as truncated, so the page warns: " + page.fixture.console);
    }

    /**
     * A WAV file whose <code>data</code> chunk announces more than the file holds: the promise resolves with the frames that are there (the decoder shortens the chunk to the file), and the page
     * reports a warning exactly when the engine marks the result truncated.
     * <p>
     * <code>data</code> 청크가 파일이 가진 것보다 많이 알리는 WAV 파일: 프라미스는 있는 프레임으로 resolve 된다(디코더가 청크를 파일에 맞게 줄인다). 페이지는 엔진이 결과를 잘렸다고 표시할 때에만
     * 경고를 보고한다.
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testCutWavResolves() throws Exception {
        byte[] full = resource("wav/wav_s16_44100_st.wav");
        int whole = AudioBufferDecoder.decodeResult(full, null, 44100f, null).getBuffer().getLength();
        DecodeResult cut = decodeAndCompare("wav", Arrays.copyOf(full, full.length / 2), 44100);
        Check.isTrue(cut.getBuffer().getLength() > 0 && cut.getBuffer().getLength() < whole, "the half file has fewer frames: " + cut.getBuffer().getLength() + " of " + whole);
        // 파일의 절반을 잘랐으므로 프레임은 전체의 절반 가까이이다(머리의 크기만큼 모자란다).
        Check.isTrue(Math.abs(cut.getBuffer().getLength() - whole / 2) <= 100, "the frames are about the half of the file: " + cut.getBuffer().getLength() + " of " + whole);
    }

    /**
     * A file without a single frame to give (cut inside its headers, a WAV file with no samples) is an <code>EncodingError</code> through the promise and through the error callback, with no success
     * callback and no warning.
     * <p>
     * 줄 프레임이 하나도 없는 파일(머리 안에서 자른 것, 표본이 없는 WAV 파일)은 프라미스와 오류 콜백으로 <code>EncodingError</code> 이고 성공 콜백도 경고도 없다.
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testNoFrameIsAnEncodingError() throws Exception {
        List<String> keys = new ArrayList<String>();
        // 형식마다 머리 안(앞 40 바이트)에서 자른 사본을 디코딩한다.
        for (int i = 0; i < LONG_FILES.length; i++) {
            int rate = ((Integer) LONG_FILES[i][1]).intValue();
            start("h" + i, Arrays.copyOf(resource("trunc/" + LONG_FILES[i][0]), 40), rate);
            keys.add("h" + i);
        }
        byte[] wav = resource("wav/wav_s16_44100_st.wav");
        // 머리만 있는 WAV 파일(데이터 청크의 내용이 없음)과 모두 0 인 파일도 디코딩한다.
        start("headerOnly", Arrays.copyOf(wav, 44), 44100);
        keys.add("headerOnly");
        start("zeros", new byte[500], 44100);
        keys.add("zeros");
        // 모든 디코딩이 EncodingError 로 끝났는지 확인한다.
        for (String key : keys) {
            Check.equal("error:EncodingError", finish(key), key + ": the promise is rejected with EncodingError");
            Check.equal("error:EncodingError", page.run("res['" + key + "'].cb"), key + ": the error callback ran");
            Check.equal("true", page.run("String(res['" + key + "'].err instanceof DOMException && res['" + key + "'].err.name === 'EncodingError')"), key + ": a DOMException");
        }
        Check.equal("[]", warnLines().toString(), "an error is no warning");
        Check.equal("", page.run("res.zeros.cb === 'success' ? 'success' : ''"), "no success callback");
    }

    /**
     * A 5.1 file is a buffer of six channels with its frames: a PCM16 WAV file is compared with the samples that the script reads from the file itself, a FLAC file with the mono decodings of its channel
     * references (FL FR FC LFE BL BR), and the lossy files (Opus, Vorbis, AAC) have the channels and the frames that the encoders wrote, each channel close to its own reference channel.
     * <p>
     * 5.1 파일은 프레임이 있는 채널 여섯의 버퍼이다: PCM16 WAV 파일은 스크립트가 파일에서 직접 읽은 표본과, FLAC 파일은 채널 참조의 모노 디코딩과(FL FR FC LFE BL BR), 손실 파일(Opus, Vorbis, AAC)은
     * 인코더가 쓴 채널과 프레임으로 비교하며 채널마다 자기 참조 채널과 가깝다.
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testFiveOneFiles() throws Exception {
        // 1) PCM16 WAV: 스크립트가 파일의 데이터 청크에서 읽은 표본과 채널/프레임 위치까지 같다.
        byte[] wav = resource("wav/wav_s16_16000_51.wav");
        start("wav51", wav, 16000);
        Check.equal("ok", finish("wav51"), "the 5.1 WAV file is decoded");
        Check.equal("6:500:16000", page.run("[res.wav51.buf.numberOfChannels, res.wav51.buf.length, res.wav51.buf.sampleRate].join(':')"), "six channels, 500 frames");
        double wavDiff = page.number("var info = pcm16Of(hex2buf(H.wav51)), m = 0; for (var c = 0; c < 6; c++) { var d = res.wav51.buf.getChannelData(c);"
                + " for (var i = 0; i < 500; i++) { m = Math.max(m, Math.abs(d[i] - info.view.getInt16(info.base + (i * 6 + c) * 2, true) / 32768)); } } m");
        Check.isTrue(wavDiff == 0, "every sample is the sample of the file in its channel: " + wavDiff);
        // 2) FLAC: 채널마다 모노 참조와 정확히 같다(16비트 FLAC 의 참조는 ffmpeg 의 16비트 디코딩).
        referenceCompare("flac51", "flac/flac_c5_16000_51.flac", "ref/flac_c5_16000_51", 6, 16000, 500, 500, 0.0);
        // 3) 손실 코덱: 채널 수와 프레임 수가 인코더가 쓴 것이고 채널마다 자기 참조와 가깝다(Vorbis 는 ffmpeg 가 1792 프레임을 주지만 파일이 알리는 길이는 2048 이다. 앞 1792 프레임을 비교한다).
        referenceCompare("opus51", "opus/opus_audio_96k_48000_51.opus", "ref/opus_audio_96k_48000_51", 6, 48000, 2880, 2880, 2e-4);
        referenceCompare("vorbis51", "vorbis/vorbis_q4_16000_51.ogg", "ref/vorbis_q4_16000_51", 6, 16000, 2048, 1792, 2e-4);
        referenceCompare("aac6", "aac/aac_adts_16000_6ch.aac", "ref/aac_adts_16000_6ch", 6, 16000, 3072, 3072, 2e-4);
    }

    /**
     * Decodes a multichannel file and the mono reference of each of its channels and compares the channels: the largest difference over all channels must not be above a limit (0 for the files that
     * are exact).
     * <p>
     * 다채널 파일과 채널마다의 모노 참조를 디코딩하고 채널을 비교한다. 모든 채널에 걸친 최대 차이가 한도를 넘으면 안 된다(정확한 파일은 0).
     *
     * @param key       the key of the file / 파일의 키
     * @param file      the file below <code>/media/audio</code> / <code>/media/audio</code> 아래의 파일
     * @param refBase   the path of the references without the channel number and the extension / 채널 번호와 확장자를 뺀 참조의 경로
     * @param channels  the number of channels / 채널 수
     * @param rate      the sample rate / 샘플링 주파수
     * @param frames    the frames that the file must give / 파일이 줘야 하는 프레임 수
     * @param refFrames the frames of each reference (the channels are compared over the frames that both have) / 참조의 프레임 수(채널은 둘 다 가진 프레임에 대해 비교한다)
     * @param tolerance the largest difference / 허용하는 최대 차이
     * @throws IOException if a file is missing / 파일이 없으면
     */
    private void referenceCompare(String key, String file, String refBase, int channels, int rate, int frames, int refFrames, double tolerance) throws IOException {
        start(key, resource(file), rate);
        // 채널마다 모노 참조를 디코딩한다.
        for (int c = 0; c < channels; c++) {
            page.run("H['" + key + "r" + c + "'] = '" + hex(resource(refBase + "." + c + ".flac")) + "'; dec('" + key + "r" + c + "', ctxFor(" + rate + ")); 1");
        }
        Check.equal("ok", finish(key), key + ": decoded");
        Check.equal(channels + ":" + frames + ":" + rate, page.run("[res['" + key + "'].buf.numberOfChannels, res['" + key + "'].buf.length, res['" + key + "'].buf.sampleRate].join(':')"),
                key + ": the channels, the frames and the sample rate");
        // 모든 채널에 대해 같은 위치의 참조 채널과의 최대 차이를 구한다.
        double worst = 0;
        for (int c = 0; c < channels; c++) {
            Check.equal("ok", finish(key + "r" + c), key + ": reference " + c + " decoded");
            Check.equal("1:" + refFrames, page.run("res['" + key + "r" + c + "'].buf.numberOfChannels + ':' + res['" + key + "r" + c + "'].buf.length"), key + ": reference " + c + " is mono");
            worst = Math.max(worst, page.number("maxDiff(res['" + key + "'].buf.getChannelData(" + c + "), res['" + key + "r" + c + "'].buf.getChannelData(0))"));
        }
        Check.isTrue(worst <= tolerance, key + ": each channel is its own reference, the largest difference " + worst + " (limit " + tolerance + ")");
    }

    /**
     * The 24-bit 7.1 FLAC file has eight channels and keeps more precision than 16 bits: every sample is within half a step of 16 bits of its 16-bit reference, and samples exist that are not a multiple
     * of the 16-bit step (the 16-bit path would have rounded them away).
     * <p>
     * 24비트 7.1 FLAC 파일은 채널이 여덟이고 16비트보다 높은 정밀도를 유지한다: 모든 표본은 16비트 참조에서 16비트 간격의 절반 이내이고, 16비트 간격의 배수가 아닌 표본이 있다(16비트 경로는
     * 반올림해 없앴을 것이다).
     *
     * @throws Exception if a file is missing / 파일이 없으면
     */
    public void testTwentyFourBitFlacKeepsPrecision() throws Exception {
        String key = "flac71";
        start(key, resource("flac/flac_c5_16000_71_24.flac"), 16000);
        // 채널마다 16비트 참조를 디코딩한다.
        for (int c = 0; c < 8; c++) {
            page.run("H['r" + c + "'] = '" + hex(resource("ref/flac_c5_16000_71_24." + c + ".flac")) + "'; dec('r" + c + "', ctxFor(16000)); 1");
        }
        Check.equal("ok", finish(key), "decoded");
        Check.equal("8:300:16000", page.run("[res.flac71.buf.numberOfChannels, res.flac71.buf.length, res.flac71.buf.sampleRate].join(':')"), "eight channels, 300 frames");
        double within = 0;
        double fractions = 0;
        // 채널마다 16비트 참조와의 차이(16비트 간격 단위)와 16비트 간격의 배수가 아닌 표본의 수를 구한다.
        for (int c = 0; c < 8; c++) {
            Check.equal("ok", finish("r" + c), "reference " + c + " decoded");
            within = Math.max(within, page.number("var a = res.flac71.buf.getChannelData(" + c + "), b = res.r" + c + ".buf.getChannelData(0), m = 0;"
                    + " for (var i = 0; i < a.length; i++) { m = Math.max(m, Math.abs(a[i] - b[i]) * 32768); } m"));
            fractions += page.number("var a = res.flac71.buf.getChannelData(" + c + "), n = 0; for (var i = 0; i < a.length; i++) { var q = a[i] * 32768; if (Math.abs(q - Math.round(q)) > 1e-6) { n++; } } n");
        }
        Check.isTrue(within <= 0.5 + 1e-6, "every sample is within half a 16-bit step of the 16-bit reference: " + within);
        Check.isTrue(fractions > 100, "samples exist that are not multiples of the 16-bit step: " + fractions);
    }

    /**
     * WAV files that the script builds sample by sample have exactly the values that went in: 24-bit integers (with the extremes) over six channels are the integer divided by 2^23, 32-bit floats over six
     * channels keep values beyond 1 (no clipping, no rounding to 16 bits), and 32 channels are decoded while 33 channels are an <code>EncodingError</code>.
     * <p>
     * 스크립트가 표본 단위로 만든 WAV 파일은 넣은 값을 정확히 가진다: 여섯 채널의 24비트 정수(극단값 포함)는 정수를 2^23 으로 나눈 값, 여섯 채널의 32비트 float 는 1 을 넘는 값을 유지하고(깎지도
     * 16비트로 반올림하지도 않음), 32채널은 디코딩되고 33채널은 <code>EncodingError</code> 이다.
     */
    public void testExactValuesOfSyntheticFiles() {
        // 24비트 정수 6채널: 극단값과 16비트로 표현할 수 없는 값을 포함한 8 프레임.
        page.run("var rows24 = []; for (var i = 0; i < 8; i++) { var r = []; for (var c = 0; c < 6; c++) { r.push(((i * 1000003 + c * 7919 + 12345) % 16777216) - 8388608); } rows24.push(r); }"
                + " rows24[0] = [8388607, -8388608, 1, -1, 0x123457, 0]; rows24[1] = [-0x123457, 255, -255, 8388606, -8388607, 65537];"
                + " H.w24 = ''; var f24 = pcmWav(8000, 6, 24, false, 0x3F, rows24); var u24 = new Uint8Array(f24), h24 = ''; for (var k = 0; k < u24.length; k++) { h24 += (u24[k] < 16 ? '0' : '') + u24[k].toString(16); } H.w24 = h24;"
                + " dec('w24', ctxFor(8000)); 1");
        Check.equal("ok", finish("w24"), "the 24-bit WAV file is decoded");
        Check.equal("6:8", page.run("res.w24.buf.numberOfChannels + ':' + res.w24.buf.length"), "six channels, eight frames");
        Check.equal("true", page.run("var ok24 = true; for (var c = 0; c < 6; c++) { var d = res.w24.buf.getChannelData(c); for (var i = 0; i < 8; i++) { if (d[i] !== rows24[i][c] / 8388608) { ok24 = false; } } } String(ok24)"),
                "every sample is the integer divided by 2^23, exactly");
        Check.equal("true", page.run("String(res.w24.buf.getChannelData(0)[0] === 8388607 / 8388608 && res.w24.buf.getChannelData(1)[0] === -1)"), "the extremes");
        // 32비트 float 6채널: 1 을 넘는 값과 작은 값이 그대로 남는다.
        page.run("var rowsF = []; for (var i = 0; i < 8; i++) { var r = []; for (var c = 0; c < 6; c++) { r.push((i - 3) * 1.5 + c * 0.25 + 0.0009765625); } rowsF.push(r); }"
                + " var fF = pcmWav(8000, 6, 32, true, 0x3F, rowsF), uF = new Uint8Array(fF), hF = ''; for (var k = 0; k < uF.length; k++) { hF += (uF[k] < 16 ? '0' : '') + uF[k].toString(16); } H.wf = hF;"
                + " dec('wf', ctxFor(8000)); 1");
        Check.equal("ok", finish("wf"), "the float WAV file is decoded");
        Check.equal("true", page.run("var okF = true; for (var c = 0; c < 6; c++) { var d = res.wf.buf.getChannelData(c); for (var i = 0; i < 8; i++) { if (d[i] !== rowsF[i][c]) { okF = false; } } } String(okF)"),
                "every float is kept exactly, also the values beyond 1");
        Check.isTrue(page.number("res.wf.buf.getChannelData(5)[7]") > 1, "a value beyond 1 is not clipped");
        // 32채널은 디코딩되고 33채널은 EncodingError 이다.
        page.run("function rowsOf(ch) { var rows = []; for (var i = 0; i < 4; i++) { var r = []; for (var c = 0; c < ch; c++) { r.push(c * 100 + i); } rows.push(r); } return rows; }"
                + " function hexOf(ab) { var u = new Uint8Array(ab), h = ''; for (var k = 0; k < u.length; k++) { h += (u[k] < 16 ? '0' : '') + u[k].toString(16); } return h; }"
                + " H.c32 = hexOf(pcmWav(8000, 32, 16, false, 0, rowsOf(32))); H.c33 = hexOf(pcmWav(8000, 33, 16, false, 0, rowsOf(33)));"
                + " dec('c32', ctxFor(8000)); dec('c33', ctxFor(8000)); 1");
        Check.equal("ok", finish("c32"), "32 channels are decoded");
        Check.equal("32:4", page.run("res.c32.buf.numberOfChannels + ':' + res.c32.buf.length"), "32 channels, four frames");
        Check.equal("true", page.run("var ok32 = true; for (var c = 0; c < 32; c++) { var d = res.c32.buf.getChannelData(c); for (var i = 0; i < 4; i++) { if (d[i] !== (c * 100 + i) / 32768) { ok32 = false; } } } String(ok32)"),
                "every sample of the 32 channels is in its channel");
        Check.equal("error:EncodingError", finish("c33"), "33 channels are more than a buffer can have");
    }

    /**
     * Creates the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(400, 300);
            }
        });
        return result[0];
    }

    /**
     * Waits until the page stored a value in the session storage of the component and returns it.
     * <p>
     * 페이지가 컴포넌트의 sessionStorage 에 값을 저장할 때까지 기다리고 그 값을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static String awaitStored(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        String value = null;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 복사한 저장소에서 찾는다.
        while (value == null && System.currentTimeMillis() < end) {
            for (StorageEntry entry : panel.getSessionStorage()) {
                if (key.equals(entry.getKey())) {
                    value = entry.getValue();
                }
            }
            if (value == null) {
                Thread.sleep(10);
            }
        }
        Check.isTrue(value != null, "the page stored " + key);
        return value;
    }

    /**
     * The whole path in the real component: a page fetches a cut-off FLAC file from a real HTTP server and decodes it; the promise resolves with the frames that could be decoded, and the console listener
     * of the component receives the warning (the level <code>WARN</code>, the text of the engine) on the event thread, before the <code>log</code> event of the success callback.
     * <p>
     * 실제 컴포넌트에서의 전체 경로: 페이지가 실제 HTTP 서버에서 잘린 FLAC 파일을 가져와 디코딩한다. 프라미스는 디코딩할 수 있었던 프레임으로 resolve 되고, 컴포넌트의 콘솔 리스너는 경고(수준
     * <code>WARN</code>, 엔진의 문구)를 이벤트 스레드에서, 성공 콜백의 <code>log</code> 이벤트보다 먼저 받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCutFlacWarnsThroughTheComponent() throws Exception {
        byte[] data = resource("trunc/long_flac_8000_st.flac");
        byte[] cut = Arrays.copyOf(data, data.length * 70 / 100);
        DecodeResult expect = AudioBufferDecoder.decodeResult(cut, null, 8000f, null);
        Check.isTrue(expect.isTruncated(), "the engine marks the cut file truncated");
        final List<ConsoleEvent> events = Collections.synchronizedList(new ArrayList<ConsoleEvent>());
        final List<Boolean> onEdt = Collections.synchronizedList(new ArrayList<Boolean>());
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            panel.addConsoleListener(new ConsoleListener() {
                /**
                 * Records the event and the thread.
                 * <p>
                 * 이벤트와 스레드를 기록한다.
                 *
                 * @param event the event / 이벤트
                 */
                public void consoleMessage(ConsoleEvent event) {
                    events.add(event);
                    onEdt.add(Boolean.valueOf(SwingUtilities.isEventDispatchThread()));
                }
            });
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><body><script>"
                    + "fetch('/cut.flac').then(function (r) { return r.arrayBuffer(); })"
                    + ".then(function (ab) { return new OfflineAudioContext(2, 128, 8000).decodeAudioData(ab, function (b) { console.log('success:' + b.length); }); })"
                    + ".then(function (b) { sessionStorage.setItem('decoded', b.length + ':' + b.numberOfChannels); })"
                    + ".catch(function (e) { sessionStorage.setItem('failure', String(e && e.name)); });</script></body>"));
            server.routeFixed("/cut.flac", new TestHttpServer.Response(200).bytes("audio/flac", cut));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal(expect.getBuffer().getLength() + ":2", awaitStored(panel, "decoded"), "the page decoded the part of the file that is there");
            long end = System.currentTimeMillis() + 10000;
            // 두 콘솔 이벤트(경고와 성공 콜백의 log)가 EDT 로 도착할 때까지 기다린다.
            while (events.size() < 2 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(2, events.size(), "the warning and the line of the success callback: " + events);
            ConsoleEvent warning = events.get(0);
            Check.equal(ConsoleEvent.Level.WARN, warning.getLevel(), "the first event is the warning");
            Check.equal("decodeAudioData: " + expect.getWarning(), warning.getMessage(), "with the text of the engine");
            Check.equal(server.url("/page"), warning.getDocumentUrl(), "of the page that decoded");
            Check.equal("success:" + expect.getBuffer().getLength(), events.get(1).getMessage(), "then the success callback");
            Check.equal(ConsoleEvent.Level.LOG, events.get(1).getLevel(), "a log event");
            Check.isFalse(onEdt.contains(Boolean.FALSE), "both events were delivered on the event thread: " + onEdt);
        } finally {
            server.stop();
            panel.dispose();
        }
    }
}
