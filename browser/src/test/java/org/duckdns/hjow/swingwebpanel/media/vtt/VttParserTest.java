package org.duckdns.hjow.swingwebpanel.media.vtt;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the WebVTT file parser (plan item P20-13; FEATURE_MATRIX VID-02): the file signature, the header, the cue blocks with their
 * timestamps (minutes and hours form, bad forms) and cue settings (vertical, line, position, size, align, region), the <code>REGION</code>
 * and <code>STYLE</code> blocks, comments, the recovery from a broken cue block, the line ends, the byte order mark and the UTF-8 decoding.
 * <p>
 * WebVTT 파일 파서의 시험(계획 P20-13, FEATURE_MATRIX VID-02): 파일 서명, 머리, 타임스탬프(분과 시 형태, 잘못된 형태)와 cue 설정(vertical, line, position,
 * size, align, region)이 있는 cue 블록, <code>REGION</code>, <code>STYLE</code> 블록, 주석, 깨진 cue 블록의 복구, 줄 끝, 바이트 순서 표시, UTF-8 해독.
 */
public class VttParserTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VttParserTest() {
    }

    /**
     * The file must start with <code>WEBVTT</code> followed by the end, a space, a tab or a line feed; a byte order mark in front is allowed.
     * <p>
     * 파일은 <code>WEBVTT</code> 로 시작하고 끝, 스페이스, 탭, 줄 바꿈이 이어져야 한다. 앞의 바이트 순서 표시는 허용한다.
     */
    public void testSignature() {
        Check.isTrue(VttParser.parse("WEBVTT").valid, "the bare signature");
        Check.isTrue(VttParser.parse("WEBVTT\n").valid, "with a line feed");
        Check.isTrue(VttParser.parse("WEBVTT - the title\n\n").valid, "with a header text");
        Check.isTrue(VttParser.parse("WEBVTT\tx").valid, "with a tab");
        Check.isFalse(VttParser.parse("WEBVTTX\n").valid, "a letter after the signature");
        Check.isFalse(VttParser.parse("WEBVT").valid, "too short");
        Check.isFalse(VttParser.parse("").valid, "empty");
        Check.isFalse(VttParser.parse(" WEBVTT\n").valid, "a space before the signature");
        Check.isFalse(VttParser.parse("<html></html>").valid, "a different file");
        Check.isTrue(VttParser.parse((char) 0xFEFF + "WEBVTT\n\n00:01.000 --> 00:02.000\nx").valid, "a byte order mark is dropped");
        Check.equal(1, VttParser.parse((char) 0xFEFF + "WEBVTT\n\n00:01.000 --> 00:02.000\nx").cues.size(), "the cue after the mark");
        byte[] bytes = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'W', 'E', 'B', 'V', 'T', 'T', '\n', '\n', '0', '0', ':', '0', '1', '.', '0', '0', '0', ' ', '-', '-', '>',
            ' ', '0', '0', ':', '0', '2', '.', '0', '0', '0', '\n', 'x' };
        Check.equal(1, VttParser.parse(bytes).cues.size(), "the UTF-8 bytes with a byte order mark");
        Check.equal(0, VttParser.parse("WEBVTT").cues.size(), "no cues");
    }

    /**
     * A simple file: identifiers, times, multi-line text, the order of the cues, and the three kinds of line ends.
     * <p>
     * 단순한 파일: 식별자, 시각, 여러 줄 텍스트, cue 의 순서, 세 종류의 줄 끝.
     */
    public void testCuesAndLineEnds() {
        String body = "WEBVTT\n\n1\n00:00:01.000 --> 00:00:04.000\nHello\nworld\n\nsecond\n00:05.500 --> 01:02:03.456\nOK\n\n00:07.000 --> 00:08.000 align:start\nno id";
        VttFile f = VttParser.parse(body);
        Check.equal(3, f.cues.size(), "three cues");
        Check.equal("1", f.cues.get(0).id, "the identifier");
        Check.equal(1.0, f.cues.get(0).startTime, "start");
        Check.equal(4.0, f.cues.get(0).endTime, "end");
        Check.equal("Hello\nworld", f.cues.get(0).text, "two text lines");
        Check.equal("second", f.cues.get(1).id, "a textual identifier");
        Check.equal(5.5, f.cues.get(1).startTime, "start in the minutes form");
        Check.near(3723.456, f.cues.get(1).endTime, 1e-9, "end in the hours form");
        Check.equal("", f.cues.get(2).id, "no identifier");
        Check.equal("no id", f.cues.get(2).text, "the last cue ends with the file");
        Check.equal("start", f.cues.get(2).align, "a setting after the end time");
        String crlf = body.replace("\n", "\r\n");
        Check.equal(3, VttParser.parse(crlf).cues.size(), "CRLF line ends");
        Check.equal("Hello\nworld", VttParser.parse(crlf).cues.get(0).text, "CRLF in the text becomes a line feed");
        Check.equal("Hello\nworld", VttParser.parse(body.replace("\n", "\r")).cues.get(0).text, "CR line ends");
        Check.equal("a\n" + (char) 0xFFFD + "b", VttParser.parse("WEBVTT\n\n00:01.000 --> 00:02.000\na\n" + (char) 0 + "b").cues.get(0).text, "NUL becomes the replacement character");
    }

    /**
     * Timestamps: the forms that are accepted and the forms that make a cue block wrong (which drops that cue and nothing else).
     * <p>
     * 타임스탬프: 받아들이는 형태와 cue 블록을 틀리게 하는 형태(그 cue 만 버려지고 다른 것은 영향이 없다).
     */
    public void testTimestamps() {
        String[] good = { "00:00.000", "59:59.999", "01:00:00.000", "1:00:00.000", "100:00:00.000", "99:59:59.999" };
        double[] values = { 0, 3599.999, 3600, 3600, 360000, 359999.999 };
        // 받아들이는 형태마다 값이 맞는지 본다.
        for (int i = 0; i < good.length; i++) {
            VttFile f = VttParser.parse("WEBVTT\n\n" + good[i] + " --> 200:00:00.000\nx");
            Check.equal(1, f.cues.size(), "accepted: " + good[i]);
            Check.near(values[i], f.cues.get(0).startTime, 1e-9, "value of " + good[i]);
        }
        String[] bad = { "0:00.000", "00:0.000", "00:00.00", "00:00.0000", "00:60.000", "00:00:60.000", "60:00:00.00", "00.000", "00:00,000", "ab:cd.efg", "1:2:3.456" };
        // 틀린 형태는 그 cue 를 버리고 뒤의 cue 는 살린다.
        for (int i = 0; i < bad.length; i++) {
            VttFile f = VttParser.parse("WEBVTT\n\n" + bad[i] + " --> 00:09.000\nbad\n\n00:01.000 --> 00:02.000\ngood");
            Check.equal(1, f.cues.size(), "a cue with the start " + bad[i] + " is dropped");
            Check.equal("good", f.cues.get(0).text, "the next cue survives " + bad[i]);
        }
        Check.equal(0, VttParser.parse("WEBVTT\n\n00:01.000 -> 00:02.000\nx").cues.size(), "a single hyphen is not an arrow");
        Check.equal(0, VttParser.parse("WEBVTT\n\n00:01.000 --> \nx").cues.size(), "no end time");
        Check.equal(1, VttParser.parse("WEBVTT\n\n  00:01.000   -->   00:02.000   \nx").cues.size(), "spaces around the arrow");
        Check.equal(1, VttParser.parse("WEBVTT\n\n00:02.000 --> 00:01.000\nx").cues.size(), "an end before the start is still a cue (the file parser does not check)");
    }

    /**
     * The cue settings: each one that is right applies, each one that is wrong is ignored and the others still apply.
     * <p>
     * cue 설정: 올바른 것은 적용되고 틀린 것은 무시되며 나머지는 그대로 적용된다.
     */
    public void testCueSettings() {
        VttCue d = VttParser.parse("WEBVTT\n\n00:01.000 --> 00:02.000\nx").cues.get(0);
        Check.equal("", d.vertical, "default vertical");
        Check.isTrue(d.snapToLines, "default snap to lines");
        Check.isTrue(VttCue.isAuto(d.line), "default line");
        Check.equal("start", d.lineAlign, "default line align");
        Check.isTrue(VttCue.isAuto(d.position), "default position");
        Check.equal("auto", d.positionAlign, "default position align");
        Check.equal(100.0, d.size, "default size");
        Check.equal("center", d.align, "default align");
        Check.isNull(d.region, "default region");
        VttCue c = cue("vertical:rl line:-2 position:30%,line-right size:40% align:end");
        Check.equal("rl", c.vertical, "vertical");
        Check.equal(-2.0, c.line, "a negative line");
        Check.isTrue(c.snapToLines, "a line number snaps");
        Check.equal(30.0, c.position, "position");
        Check.equal("line-right", c.positionAlign, "position align");
        Check.equal(40.0, c.size, "size");
        Check.equal("end", c.align, "align");
        c = cue("line:50%,center");
        Check.equal(50.0, c.line, "a percentage line");
        Check.isFalse(c.snapToLines, "a percentage does not snap");
        Check.equal("center", c.lineAlign, "line align");
        c = cue("line:2.5 vertical:lr");
        Check.equal(2.5, c.line, "a fractional line");
        Check.equal("lr", c.vertical, "vertical lr");
        // 틀린 설정은 무시하고 기본값을 둔다.
        String[] wrong = { "line:abc", "line:1-2", "line:1.", "line:.5", "line:1,bogus", "line:101%", "line:-1%", "position:10", "position:10%,bogus", "size:abc",
            "size:150%", "align:middle", "vertical:up", "region:none", "line", ":x", "line:", "foo:bar" };
        // 틀린 설정마다 기본값이 그대로인지 본다.
        for (int i = 0; i < wrong.length; i++) {
            VttCue w = cue(wrong[i]);
            Check.isTrue(VttCue.isAuto(w.line) && VttCue.isAuto(w.position) && w.size == 100 && "center".equals(w.align) && "".equals(w.vertical), "ignored: " + wrong[i]);
        }
        c = cue("size:abc align:left line:3");
        Check.equal("left", c.align, "the other settings still apply after a wrong one");
        Check.equal(3.0, c.line, "and the line too");
    }

    /**
     * <code>REGION</code> blocks define regions that cues refer to with <code>region:id</code>; a cue with a vertical writing direction, a line or
     * a size drops out of the region; the last region of an identifier wins; the settings of a region are checked one by one.
     * <p>
     * <code>REGION</code> 블록은 cue 가 <code>region:id</code> 로 참조하는 region 을 정의한다. 세로 쓰기, 줄, 크기가 있는 cue 는 region 에서 빠지고, 식별자가
     * 같으면 마지막 region 이 이긴다. region 의 설정은 하나씩 검사한다.
     */
    public void testRegions() {
        String file = "WEBVTT\n\nREGION\nid:one width:40% lines:5\nregionanchor:10%,90%\nviewportanchor:20%,80% scroll:up\n\n"
                + "REGION\nid:one width:55%\n\nREGION id:ignored\nid:bogus\n\nREGION\nid:two lines:x width:120%\n\n"
                + "00:01.000 --> 00:02.000 region:one\nA\n\n00:03.000 --> 00:04.000 region:one vertical:rl\nB\n\n00:05.000 --> 00:06.000 region:one line:3\nC\n\n"
                + "00:07.000 --> 00:08.000 region:one size:50%\nD\n\n00:09.000 --> 00:10.000 region:nope\nE\n\n00:11.000 --> 00:12.000 region:two\nF";
        VttFile f = VttParser.parse(file);
        Check.equal(3, f.regions.size(), "three region blocks (text on the same line as REGION makes it no region block)");
        VttRegion one = f.regions.get(0);
        Check.equal("one", one.id, "the identifier");
        Check.equal(40.0, one.width, "width");
        Check.equal(5L, one.lines, "lines");
        Check.equal(10.0, one.regionAnchorX, "region anchor x");
        Check.equal(90.0, one.regionAnchorY, "region anchor y");
        Check.equal(20.0, one.viewportAnchorX, "viewport anchor x");
        Check.equal(80.0, one.viewportAnchorY, "viewport anchor y");
        Check.equal("up", one.scroll, "scroll");
        Check.equal(3L, f.regions.get(2).lines, "a wrong lines setting leaves the default");
        Check.equal(100.0, f.regions.get(2).width, "a wrong width leaves the default");
        Check.same(f.regions.get(1), f.cues.get(0).region, "the last region with the identifier wins");
        Check.equal(55.0, f.cues.get(0).region.width, "and it is the second block");
        Check.isNull(f.cues.get(1).region, "a vertical cue has no region");
        Check.isNull(f.cues.get(2).region, "a cue with a line has no region");
        Check.isNull(f.cues.get(3).region, "a cue with a size has no region");
        Check.isNull(f.cues.get(4).region, "an unknown region");
        Check.same(f.regions.get(2), f.cues.get(5).region, "the region with the wrong settings still exists");
    }

    /**
     * <code>STYLE</code> blocks are collected before the first cue; <code>NOTE</code> blocks, header blocks and garbage are skipped; a
     * <code>STYLE</code> or <code>REGION</code> block after a cue is only a text block that no rule accepts; a block that contains an arrow in
     * its second line without a cue before it ends at that line.
     * <p>
     * <code>STYLE</code> 블록은 첫 cue 전에 모으고, <code>NOTE</code> 블록, 머리 블록, 쓰레기는 건너뛴다. cue 뒤의 <code>STYLE</code>, <code>REGION</code>
     * 블록은 어떤 규칙도 받아들이지 않는 텍스트 블록이다. 둘째 줄에 화살표가 있는 블록은 그 줄에서 끝난다.
     */
    public void testStyleNoteAndGarbage() {
        String file = "WEBVTT - header\nKind: captions\nLanguage: en\n\nSTYLE\n::cue { color: red }\n\nNOTE this is a comment\nover two lines\n\n"
                + "garbage block\nwithout times\n\n00:01.000 --> 00:02.000\nfirst\n\nSTYLE\n::cue { color: blue }\n\nREGION\nid:late\n\n"
                + "NOTE\n00:03.000 --> 00:04.000\ncomment text becomes a cue by the rule\n\nid\n00:05.000 --> 00:06.000\nlast";
        VttFile f = VttParser.parse(file);
        Check.equal(1, f.stylesheets.size(), "one style sheet before the first cue");
        Check.equal("::cue { color: red }", f.stylesheets.get(0), "its text");
        Check.equal(0, f.regions.size(), "a region block after a cue is no region");
        Check.equal(3, f.cues.size(), "first, the cue in the note-like block and the last");
        Check.equal("first", f.cues.get(0).text, "the first cue is not hurt by the blocks around it");
        Check.equal("NOTE", f.cues.get(1).id, "a NOTE line in front of a timing line is the cue identifier by the rule");
        Check.equal("last", f.cues.get(2).text, "the cue after it");
        VttFile broken = VttParser.parse("WEBVTT\n\n00:01.000 --> 00:02.000\nfirst line\n00:03.000 --> 00:04.000\nsecond cue directly after the text");
        Check.equal(2, broken.cues.size(), "an arrow line ends the previous cue (a missing blank line)");
        Check.equal("first line", broken.cues.get(0).text, "the text before the arrow");
        Check.equal("second cue directly after the text", broken.cues.get(1).text, "the text after it");
    }

    /**
     * A file that has only a header or only blank lines is valid and has no cue; the last cue may end at the end of the file with or without
     * a line feed; many cues keep their order.
     * <p>
     * 머리만 있거나 빈 줄뿐인 파일은 유효하고 cue 가 없다. 마지막 cue 는 줄 바꿈이 있든 없든 파일의 끝에서 끝날 수 있다. 많은 cue 는 순서를 유지한다.
     */
    public void testEdges() {
        Check.equal(0, VttParser.parse("WEBVTT\n\n\n\n").cues.size(), "blank lines");
        Check.isTrue(VttParser.parse("WEBVTT\nX-Header: 1\n").valid, "a header block");
        Check.equal("x", VttParser.parse("WEBVTT\n\n00:01.000 --> 00:02.000\nx\n").cues.get(0).text, "a final line feed");
        Check.equal("", VttParser.parse("WEBVTT\n\n00:01.000 --> 00:02.000").cues.get(0).text, "a cue without text at the end");
        StringBuilder sb = new StringBuilder("WEBVTT\n\n");
        // 많은 cue 를 만들어 순서가 유지되는지 본다.
        for (int i = 0; i < 500; i++) {
            sb.append(i).append("\n00:00:").append(i % 60 < 10 ? "0" : "").append(i % 60).append(".000 --> 01:00:00.000\ncue ").append(i).append("\n\n");
        }
        VttFile f = VttParser.parse(sb.toString());
        Check.equal(500, f.cues.size(), "500 cues");
        Check.equal("cue 499", f.cues.get(499).text, "the order is the order of the file");
        String decoded = VttParser.parse(new byte[] { 'W', 'E', 'B', 'V', 'T', 'T', '\n', '\n', '0', '0', ':', '0', '1', '.', '0', '0', '0', ' ', '-', '-', '>', ' ', '0', '0', ':',
            '0', '2', '.', '0', '0', '0', '\n', (byte) 0xED, (byte) 0x95, (byte) 0x9C, (byte) 0xFF }).cues.get(0).text;
        Check.equal(String.valueOf((char) 0xD55C) + (char) 0xFFFD, decoded, "Korean text decodes and a bad byte becomes U+FFFD");
    }

    /**
     * Makes the first cue of a file with a settings string after the end time.
     * <p>
     * 끝 시각 뒤에 설정 문자열이 있는 파일의 첫 cue 를 만든다.
     *
     * @param settings the settings / 설정
     * @return the cue / cue
     */
    private static VttCue cue(String settings) {
        return VttParser.parse("WEBVTT\n\n00:01.000 --> 00:02.000 " + settings + "\nx").cues.get(0);
    }
}
