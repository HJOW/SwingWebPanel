package org.duckdns.hjow.swingwebpanel.media.vtt;

/**
 * A WebVTT region (WebVTT 3.4 and the <code>VTTRegion</code> interface of 9.2): the data of a <code>REGION</code> block of a file or of a
 * <code>VTTRegion</code> that a script created. The fields are public because the class is a plain record that the file parser fills and the
 * script interface checks; the defaults are those of the constructor of <code>VTTRegion</code>.
 * <p>
 * WebVTT region(WebVTT 3.4 와 9.2 의 <code>VTTRegion</code> 인터페이스): 파일의 <code>REGION</code> 블록 또는 스크립트가 만든 <code>VTTRegion</code>
 * 의 데이터. 클래스는 파일 파서가 채우고 스크립트 인터페이스가 검사하는 단순한 기록이므로 필드는 public 이다. 기본값은 <code>VTTRegion</code> 생성자의 값이다.
 */
public final class VttRegion {

    /**
     * The identifier of the region (empty if it has none).
     * <p>
     * region 의 식별자(없으면 빈 문자열).
     */
    public String id = "";

    /**
     * The width as a percentage of the video width, 0 to 100.
     * <p>
     * 비디오 폭에 대한 백분율의 폭(0에서 100).
     */
    public double width = 100;

    /**
     * The height as a number of lines.
     * <p>
     * 줄 수로 나타낸 높이.
     */
    public long lines = 3;

    /**
     * The horizontal anchor point inside the region as a percentage of the region width.
     * <p>
     * region 폭에 대한 백분율의 region 안 가로 고정점.
     */
    public double regionAnchorX;

    /**
     * The vertical anchor point inside the region as a percentage of the region height.
     * <p>
     * region 높이에 대한 백분율의 region 안 세로 고정점.
     */
    public double regionAnchorY = 100;

    /**
     * The horizontal anchor point in the viewport as a percentage of the video width.
     * <p>
     * 비디오 폭에 대한 백분율의 뷰포트 안 가로 고정점.
     */
    public double viewportAnchorX;

    /**
     * The vertical anchor point in the viewport as a percentage of the video height.
     * <p>
     * 비디오 높이에 대한 백분율의 뷰포트 안 세로 고정점.
     */
    public double viewportAnchorY = 100;

    /**
     * The scroll setting: the empty string for none or <code>up</code>.
     * <p>
     * 스크롤 설정: 없으면 빈 문자열, 아니면 <code>up</code>.
     */
    public String scroll = "";

    /**
     * An object that the host attaches to the region, for example the script object that stands for it, so that the same region always has the same
     * object (the parser itself does not use it).
     * <p>
     * 호스트가 region 에 붙이는 객체. 예를 들어 region 을 나타내는 스크립트 객체이며, 같은 region 이 언제나 같은 객체를 갖게 한다(파서 자신은 쓰지 않는다).
     */
    public Object host;

    /**
     * Creates a region with the defaults of <code>new VTTRegion()</code>.
     * <p>
     * <code>new VTTRegion()</code> 의 기본값으로 region 을 만든다.
     */
    public VttRegion() {
    }
}
