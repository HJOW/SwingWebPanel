package org.duckdns.hjow.swingwebpanel.media.vtt;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of parsing a WebVTT file (WebVTT 6.1): the cues in file order, the regions, the style sheet blocks, and whether the file had the
 * WebVTT file signature (a file without it is not a WebVTT file and was not processed).
 * <p>
 * WebVTT 파일을 해석한 결과(WebVTT 6.1): 파일 순서의 cue, region, 스타일 시트 블록, 파일에 WebVTT 파일 서명이 있었는지 여부(서명이 없는 파일은 WebVTT 파일이
 * 아니며 처리되지 않았다).
 */
public final class VttFile {

    /**
     * True if the input began with the WebVTT file signature.
     * <p>
     * 입력이 WebVTT 파일 서명으로 시작했으면 true.
     */
    public boolean valid;

    /**
     * The cues in the order of the file.
     * <p>
     * 파일 순서의 cue.
     */
    public final List<VttCue> cues = new ArrayList<VttCue>();

    /**
     * The regions in the order of the file.
     * <p>
     * 파일 순서의 region.
     */
    public final List<VttRegion> regions = new ArrayList<VttRegion>();

    /**
     * The text of the <code>STYLE</code> blocks in the order of the file (the style rules are not interpreted here).
     * <p>
     * 파일 순서의 <code>STYLE</code> 블록의 텍스트(스타일 규칙은 여기서 해석하지 않는다).
     */
    public final List<String> stylesheets = new ArrayList<String>();

    /**
     * Creates an empty result.
     * <p>
     * 빈 결과를 만든다.
     */
    VttFile() {
    }
}
