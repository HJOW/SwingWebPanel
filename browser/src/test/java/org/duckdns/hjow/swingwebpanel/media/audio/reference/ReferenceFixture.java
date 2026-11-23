package org.duckdns.hjow.swingwebpanel.media.audio.reference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;

/**
 * One line of <code>tests/resources/media/audio/index.txt</code>: an audio file that a real encoder (ffmpeg with libmp3lame, the native AAC encoder,
 * libvorbis, libopus, flac, the PCM and ADPCM encoders) wrote from a synthetic signal, together with the 16-bit PCM that ffmpeg's own decoder produced
 * from it (the reference). The file format of the index is described in <code>generation/make_audio_fixtures.py</code>.
 * <p>
 * <code>tests/resources/media/audio/index.txt</code> 의 한 줄: 실제 인코더(ffmpeg 의 libmp3lame, 기본 AAC 인코더, libvorbis, libopus, flac, PCM/ADPCM
 * 인코더)가 합성 신호로 만든 오디오 파일과, ffmpeg 자신의 디코더가 그것에서 만든 16비트 PCM(참조). 색인의 파일 형식은
 * <code>generation/make_audio_fixtures.py</code> 에 설명되어 있다.
 */
public final class ReferenceFixture {

    /**
     * Name of the fixture (also the name of the file without the extension).
     * <p>
     * 시험 자료의 이름(확장자를 뺀 파일 이름이기도 하다).
     */
    public final String name;

    /**
     * Path of the file below the resource directory <code>/media/audio</code>.
     * <p>
     * 자원 디렉터리 <code>/media/audio</code> 아래의 파일 경로.
     */
    public final String path;

    /**
     * MIME type that a server would send (used as the hint of the registry).
     * <p>
     * 서버가 보낼 MIME 타입(등록부의 힌트로 쓴다).
     */
    public final String mime;

    /**
     * Group of the codec or container (mp3, aac, vorbis, flac, opus, wav, aiff, au, webm).
     * <p>
     * 코덱이나 컨테이너의 그룹(mp3, aac, vorbis, flac, opus, wav, aiff, au, webm).
     */
    public final String group;

    /**
     * Kind: <code>exact</code> (bit for bit) or <code>lossy</code> (within a tolerance).
     * <p>
     * 종류: <code>exact</code>(비트 단위로 같음) 또는 <code>lossy</code>(허용 오차 안).
     */
    public final String kind;

    /**
     * Sample rate of the reference in Hz.
     * <p>
     * 참조의 샘플링 주파수(Hz).
     */
    public final int rate;

    /**
     * Number of channels of the reference (the product mixes more than two channels down to stereo).
     * <p>
     * 참조의 채널 수(제품은 두 개를 넘는 채널을 스테레오로 섞는다).
     */
    public final int channels;

    /**
     * Speaker roles of the channels of the reference, in order (FL, FR, FC, LFE, BL, BR, BC, SL, SR, FLC, FRC).
     * <p>
     * 참조 채널의 스피커 역할(순서대로: FL, FR, FC, LFE, BL, BR, BC, SL, SR, FLC, FRC).
     */
    public final String[] roles;

    /**
     * Number of sample frames of the reference (what ffmpeg's decoder outputs).
     * <p>
     * 참조의 샘플 프레임 수(ffmpeg 의 디코더가 출력하는 수).
     */
    public final int frames;

    /**
     * Number of sample frames that the product has to output: the length of the source signal for the containers that say where the signal starts and
     * ends (LAME tag, edit list, granule positions, pre-skip), else the number of frames of ffmpeg.
     * <p>
     * 제품이 출력해야 하는 샘플 프레임 수: 신호가 어디서 시작하고 끝나는지 알려 주는 컨테이너(LAME 태그, 편집 목록, 그래뉼 위치, 사전 건너뛰기)는 원본 신호의
     * 길이, 아니면 ffmpeg 의 프레임 수.
     */
    public final int expected;

    /**
     * The reference: <code>md5:&lt;hex&gt;</code> or <code>flac:&lt;file&gt;[+&lt;file&gt;...]</code>.
     * <p>
     * 참조: <code>md5:&lt;hex&gt;</code> 또는 <code>flac:&lt;파일&gt;[+&lt;파일&gt;...]</code>.
     */
    public final String reference;

    /**
     * How the file was made and what its stream contains.
     * <p>
     * 파일을 만든 방법과 스트림에 든 내용.
     */
    public final String note;

    /**
     * Creates a fixture from the fields of an index line.
     * <p>
     * 색인 줄의 필드로 시험 자료를 만든다.
     *
     * @param f the fields of the line / 줄의 필드
     */
    private ReferenceFixture(String[] f) {
        name = f[0];
        path = f[1];
        mime = f[2];
        group = f[3];
        kind = f[4];
        rate = Integer.parseInt(f[5]);
        channels = Integer.parseInt(f[6]);
        roles = f[7].split(",");
        frames = Integer.parseInt(f[8]);
        expected = Integer.parseInt(f[9]);
        reference = f[10];
        note = f.length > 11 ? f[11] : "";
    }

    /**
     * Reads the index.
     * <p>
     * 색인을 읽는다.
     *
     * @return all fixtures in the order of the index / 색인 순서의 모든 시험 자료
     * @throws IOException if the index is missing / 색인이 없으면
     */
    public static List<ReferenceFixture> loadAll() throws IOException {
        String text = new String(resource("/media/audio/index.txt"), "UTF-8");
        List<ReferenceFixture> all = new ArrayList<ReferenceFixture>();
        // 주석과 빈 줄을 건너뛰고 줄마다 시험 자료 하나를 만든다.
        for (String line : text.split("\n")) {
            if (line.trim().length() == 0 || line.startsWith("#")) {
                continue;
            }
            all.add(new ReferenceFixture(line.split("\\|", -1)));
        }
        return all;
    }

    /**
     * Returns the fixtures of one group.
     * <p>
     * 한 그룹의 시험 자료를 반환한다.
     *
     * @param group the group (see {@link #group}) / 그룹
     * @return the fixtures / 시험 자료
     * @throws IOException if the index is missing / 색인이 없으면
     */
    public static List<ReferenceFixture> ofGroup(String group) throws IOException {
        List<ReferenceFixture> r = new ArrayList<ReferenceFixture>();
        // 그룹이 같은 것만 모은다.
        for (ReferenceFixture f : loadAll()) {
            if (f.group.equals(group)) {
                r.add(f);
            }
        }
        return r;
    }

    /**
     * Reads a classpath resource completely.
     * <p>
     * 클래스패스 자원을 모두 읽는다.
     *
     * @param name resource path (absolute) / 자원 경로(절대 경로)
     * @return the bytes / 바이트
     * @throws IOException if the resource is missing / 자원이 없으면
     */
    static byte[] resource(String name) throws IOException {
        InputStream in = ReferenceFixture.class.getResourceAsStream(name);
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
     * Reads the encoded file.
     * <p>
     * 인코딩된 파일을 읽는다.
     *
     * @return the bytes / 바이트
     * @throws IOException if missing / 없으면
     */
    public byte[] readFile() throws IOException {
        return resource("/media/audio/" + path);
    }

    /**
     * Tells whether the reference is the md5 of the PCM (exact fixtures with one or two channels) rather than the PCM itself.
     * <p>
     * 참조가 PCM 자체가 아니라 PCM 의 md5 인지 알려 준다(채널이 하나나 둘인 exact 시험 자료).
     *
     * @return true for an md5 reference / md5 참조이면 true
     */
    public boolean isMd5() {
        return reference.startsWith("md5:");
    }

    /**
     * Tells whether the product is expected to refuse the file with an UNSUPPORTED_FORMAT error (ffmpeg decodes it, the product does not).
     * <p>
     * 제품이 UNSUPPORTED_FORMAT 오류로 파일을 거부해야 하는지 알려 준다(ffmpeg 는 디코딩하지만 제품은 하지 않는다).
     *
     * @return true for a file that has to be refused / 거부해야 하는 파일이면 true
     */
    public boolean isRefused() {
        return kind.equals("refuse");
    }

    /**
     * Returns the md5 of the reference PCM.
     * <p>
     * 참조 PCM 의 md5 를 반환한다.
     *
     * @return lower case hexadecimal md5 / 소문자 16진수 md5
     */
    public String md5() {
        return reference.substring(4);
    }

    /**
     * Reads the reference PCM of a fixture whose reference is stored as FLAC (decoded with the product's FLAC decoder, which is verified bit for bit
     * against ffmpeg by the exact FLAC fixtures of the same test class).
     * <p>
     * 참조가 FLAC 으로 저장된 시험 자료의 참조 PCM 을 읽는다(제품의 FLAC 디코더로 디코딩한다. 이 디코더는 같은 시험 클래스의 exact FLAC 시험 자료로
     * ffmpeg 와 비트 단위로 같음을 확인한다).
     *
     * @return the interleaved 16-bit samples of all channels of the reference / 참조의 모든 채널의 인터리브된 16비트 샘플
     * @throws IOException    if a file is missing / 파일이 없으면
     * @throws MediaException if a reference file cannot be decoded / 참조 파일을 디코딩할 수 없으면
     */
    public short[] readReferencePcm() throws IOException, MediaException {
        String[] files = reference.substring("flac:".length()).split("\\+");
        short[][] parts = new short[files.length][];
        // 파일마다 FLAC 을 디코딩한다.
        for (int i = 0; i < files.length; i++) {
            PcmSource s = AudioDecoders.open(resource("/media/audio/" + files[i]), "audio/flac", DecodeLimits.DEFAULT);
            try {
                parts[i] = AudioKit.toShorts(AudioKit.readAll(s));
            } finally {
                s.close();
            }
        }
        if (parts.length == 1) {
            return parts[0];
        }
        // 채널마다 한 파일이면 인터리브한다.
        short[] all = new short[parts[0].length * parts.length];
        for (int c = 0; c < parts.length; c++) {
            // 채널의 샘플을 프레임 순서대로 자리에 넣는다.
            for (int i = 0; i < parts[c].length; i++) {
                all[i * parts.length + c] = parts[c][i];
            }
        }
        return all;
    }

    /**
     * Decodes the fixture with the product.
     * <p>
     * 시험 자료를 제품으로 디코딩한다.
     *
     * @param mimeHint the MIME type hint, or null / MIME 타입 힌트. null 가능
     * @return the interleaved 16-bit samples that the product produced / 제품이 만든 인터리브된 16비트 샘플
     * @throws IOException    if the file is missing / 파일이 없으면
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public short[] decode(String mimeHint) throws IOException, MediaException {
        PcmSource s = AudioDecoders.open(readFile(), mimeHint, DecodeLimits.DEFAULT);
        try {
            return AudioKit.toShorts(AudioKit.readAll(s));
        } finally {
            s.close();
        }
    }

    /**
     * Returns the name of the fixture (shown in failure messages).
     * <p>
     * 시험 자료의 이름을 반환한다(실패 메시지에 나온다).
     *
     * @return the name / 이름
     */
    @Override
    public String toString() {
        return name;
    }
}
