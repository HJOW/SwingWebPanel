package org.duckdns.hjow.swingwebpanel.html;

import java.util.HashMap;
import java.util.Map;

/**
 * Numeric ids of the element names that the tree builder treats specially, plus the name categories of the HTML Standard (special,
 * formatting, scope boundaries). The tree builder compares small integers in <code>switch</code> statements instead of strings, which
 * Java 6 cannot switch on. Id 0 stands for every other name. The names are lower case; the SVG element <code>foreignObject</code> is
 * known as <code>foreignobject</code> because lookups use the lower-cased local name.
 * <p>
 * 트리 생성기가 특별히 다루는 요소 이름의 숫자 번호와 HTML 표준의 이름 범주(special, formatting, 범위 경계). 트리 생성기는 Java 6 에서 switch 할
 * 수 없는 문자열 대신 작은 정수를 <code>switch</code> 로 비교한다. 번호 0 은 그 밖의 모든 이름이다. 이름은 소문자이며, SVG 요소
 * <code>foreignObject</code> 는 소문자로 바꾼 로컬 이름으로 찾으므로 <code>foreignobject</code> 로 알려져 있다.
 */
public final class HtmlTags {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HtmlTags() {
    }

    /**
     * Tag id of <code>a</code>.
     * <p>
     * <code>a</code> 의 태그 번호.
     */
    public static final int A = 1;

    /**
     * Tag id of <code>address</code>.
     * <p>
     * <code>address</code> 의 태그 번호.
     */
    public static final int ADDRESS = 2;

    /**
     * Tag id of <code>annotation-xml</code>.
     * <p>
     * <code>annotation-xml</code> 의 태그 번호.
     */
    public static final int ANNOTATION_XML = 3;

    /**
     * Tag id of <code>applet</code>.
     * <p>
     * <code>applet</code> 의 태그 번호.
     */
    public static final int APPLET = 4;

    /**
     * Tag id of <code>area</code>.
     * <p>
     * <code>area</code> 의 태그 번호.
     */
    public static final int AREA = 5;

    /**
     * Tag id of <code>article</code>.
     * <p>
     * <code>article</code> 의 태그 번호.
     */
    public static final int ARTICLE = 6;

    /**
     * Tag id of <code>aside</code>.
     * <p>
     * <code>aside</code> 의 태그 번호.
     */
    public static final int ASIDE = 7;

    /**
     * Tag id of <code>b</code>.
     * <p>
     * <code>b</code> 의 태그 번호.
     */
    public static final int B = 8;

    /**
     * Tag id of <code>base</code>.
     * <p>
     * <code>base</code> 의 태그 번호.
     */
    public static final int BASE = 9;

    /**
     * Tag id of <code>basefont</code>.
     * <p>
     * <code>basefont</code> 의 태그 번호.
     */
    public static final int BASEFONT = 10;

    /**
     * Tag id of <code>bgsound</code>.
     * <p>
     * <code>bgsound</code> 의 태그 번호.
     */
    public static final int BGSOUND = 11;

    /**
     * Tag id of <code>big</code>.
     * <p>
     * <code>big</code> 의 태그 번호.
     */
    public static final int BIG = 12;

    /**
     * Tag id of <code>blockquote</code>.
     * <p>
     * <code>blockquote</code> 의 태그 번호.
     */
    public static final int BLOCKQUOTE = 13;

    /**
     * Tag id of <code>body</code>.
     * <p>
     * <code>body</code> 의 태그 번호.
     */
    public static final int BODY = 14;

    /**
     * Tag id of <code>br</code>.
     * <p>
     * <code>br</code> 의 태그 번호.
     */
    public static final int BR = 15;

    /**
     * Tag id of <code>button</code>.
     * <p>
     * <code>button</code> 의 태그 번호.
     */
    public static final int BUTTON = 16;

    /**
     * Tag id of <code>caption</code>.
     * <p>
     * <code>caption</code> 의 태그 번호.
     */
    public static final int CAPTION = 17;

    /**
     * Tag id of <code>center</code>.
     * <p>
     * <code>center</code> 의 태그 번호.
     */
    public static final int CENTER = 18;

    /**
     * Tag id of <code>code</code>.
     * <p>
     * <code>code</code> 의 태그 번호.
     */
    public static final int CODE = 19;

    /**
     * Tag id of <code>col</code>.
     * <p>
     * <code>col</code> 의 태그 번호.
     */
    public static final int COL = 20;

    /**
     * Tag id of <code>colgroup</code>.
     * <p>
     * <code>colgroup</code> 의 태그 번호.
     */
    public static final int COLGROUP = 21;

    /**
     * Tag id of <code>dd</code>.
     * <p>
     * <code>dd</code> 의 태그 번호.
     */
    public static final int DD = 22;

    /**
     * Tag id of <code>desc</code>.
     * <p>
     * <code>desc</code> 의 태그 번호.
     */
    public static final int DESC = 23;

    /**
     * Tag id of <code>details</code>.
     * <p>
     * <code>details</code> 의 태그 번호.
     */
    public static final int DETAILS = 24;

    /**
     * Tag id of <code>dialog</code>.
     * <p>
     * <code>dialog</code> 의 태그 번호.
     */
    public static final int DIALOG = 25;

    /**
     * Tag id of <code>dir</code>.
     * <p>
     * <code>dir</code> 의 태그 번호.
     */
    public static final int DIR = 26;

    /**
     * Tag id of <code>div</code>.
     * <p>
     * <code>div</code> 의 태그 번호.
     */
    public static final int DIV = 27;

    /**
     * Tag id of <code>dl</code>.
     * <p>
     * <code>dl</code> 의 태그 번호.
     */
    public static final int DL = 28;

    /**
     * Tag id of <code>dt</code>.
     * <p>
     * <code>dt</code> 의 태그 번호.
     */
    public static final int DT = 29;

    /**
     * Tag id of <code>em</code>.
     * <p>
     * <code>em</code> 의 태그 번호.
     */
    public static final int EM = 30;

    /**
     * Tag id of <code>embed</code>.
     * <p>
     * <code>embed</code> 의 태그 번호.
     */
    public static final int EMBED = 31;

    /**
     * Tag id of <code>fieldset</code>.
     * <p>
     * <code>fieldset</code> 의 태그 번호.
     */
    public static final int FIELDSET = 32;

    /**
     * Tag id of <code>figcaption</code>.
     * <p>
     * <code>figcaption</code> 의 태그 번호.
     */
    public static final int FIGCAPTION = 33;

    /**
     * Tag id of <code>figure</code>.
     * <p>
     * <code>figure</code> 의 태그 번호.
     */
    public static final int FIGURE = 34;

    /**
     * Tag id of <code>font</code>.
     * <p>
     * <code>font</code> 의 태그 번호.
     */
    public static final int FONT = 35;

    /**
     * Tag id of <code>footer</code>.
     * <p>
     * <code>footer</code> 의 태그 번호.
     */
    public static final int FOOTER = 36;

    /**
     * Tag id of <code>foreignobject</code>.
     * <p>
     * <code>foreignobject</code> 의 태그 번호.
     */
    public static final int FOREIGNOBJECT = 37;

    /**
     * Tag id of <code>form</code>.
     * <p>
     * <code>form</code> 의 태그 번호.
     */
    public static final int FORM = 38;

    /**
     * Tag id of <code>frame</code>.
     * <p>
     * <code>frame</code> 의 태그 번호.
     */
    public static final int FRAME = 39;

    /**
     * Tag id of <code>frameset</code>.
     * <p>
     * <code>frameset</code> 의 태그 번호.
     */
    public static final int FRAMESET = 40;

    /**
     * Tag id of <code>h1</code>.
     * <p>
     * <code>h1</code> 의 태그 번호.
     */
    public static final int H1 = 41;

    /**
     * Tag id of <code>h2</code>.
     * <p>
     * <code>h2</code> 의 태그 번호.
     */
    public static final int H2 = 42;

    /**
     * Tag id of <code>h3</code>.
     * <p>
     * <code>h3</code> 의 태그 번호.
     */
    public static final int H3 = 43;

    /**
     * Tag id of <code>h4</code>.
     * <p>
     * <code>h4</code> 의 태그 번호.
     */
    public static final int H4 = 44;

    /**
     * Tag id of <code>h5</code>.
     * <p>
     * <code>h5</code> 의 태그 번호.
     */
    public static final int H5 = 45;

    /**
     * Tag id of <code>h6</code>.
     * <p>
     * <code>h6</code> 의 태그 번호.
     */
    public static final int H6 = 46;

    /**
     * Tag id of <code>head</code>.
     * <p>
     * <code>head</code> 의 태그 번호.
     */
    public static final int HEAD = 47;

    /**
     * Tag id of <code>header</code>.
     * <p>
     * <code>header</code> 의 태그 번호.
     */
    public static final int HEADER = 48;

    /**
     * Tag id of <code>hgroup</code>.
     * <p>
     * <code>hgroup</code> 의 태그 번호.
     */
    public static final int HGROUP = 49;

    /**
     * Tag id of <code>hr</code>.
     * <p>
     * <code>hr</code> 의 태그 번호.
     */
    public static final int HR = 50;

    /**
     * Tag id of <code>html</code>.
     * <p>
     * <code>html</code> 의 태그 번호.
     */
    public static final int HTML = 51;

    /**
     * Tag id of <code>i</code>.
     * <p>
     * <code>i</code> 의 태그 번호.
     */
    public static final int I = 52;

    /**
     * Tag id of <code>iframe</code>.
     * <p>
     * <code>iframe</code> 의 태그 번호.
     */
    public static final int IFRAME = 53;

    /**
     * Tag id of <code>image</code>.
     * <p>
     * <code>image</code> 의 태그 번호.
     */
    public static final int IMAGE = 54;

    /**
     * Tag id of <code>img</code>.
     * <p>
     * <code>img</code> 의 태그 번호.
     */
    public static final int IMG = 55;

    /**
     * Tag id of <code>input</code>.
     * <p>
     * <code>input</code> 의 태그 번호.
     */
    public static final int INPUT = 56;

    /**
     * Tag id of <code>keygen</code>.
     * <p>
     * <code>keygen</code> 의 태그 번호.
     */
    public static final int KEYGEN = 57;

    /**
     * Tag id of <code>li</code>.
     * <p>
     * <code>li</code> 의 태그 번호.
     */
    public static final int LI = 58;

    /**
     * Tag id of <code>link</code>.
     * <p>
     * <code>link</code> 의 태그 번호.
     */
    public static final int LINK = 59;

    /**
     * Tag id of <code>listing</code>.
     * <p>
     * <code>listing</code> 의 태그 번호.
     */
    public static final int LISTING = 60;

    /**
     * Tag id of <code>main</code>.
     * <p>
     * <code>main</code> 의 태그 번호.
     */
    public static final int MAIN = 61;

    /**
     * Tag id of <code>malignmark</code>.
     * <p>
     * <code>malignmark</code> 의 태그 번호.
     */
    public static final int MALIGNMARK = 62;

    /**
     * Tag id of <code>marquee</code>.
     * <p>
     * <code>marquee</code> 의 태그 번호.
     */
    public static final int MARQUEE = 63;

    /**
     * Tag id of <code>math</code>.
     * <p>
     * <code>math</code> 의 태그 번호.
     */
    public static final int MATH = 64;

    /**
     * Tag id of <code>menu</code>.
     * <p>
     * <code>menu</code> 의 태그 번호.
     */
    public static final int MENU = 65;

    /**
     * Tag id of <code>meta</code>.
     * <p>
     * <code>meta</code> 의 태그 번호.
     */
    public static final int META = 66;

    /**
     * Tag id of <code>mglyph</code>.
     * <p>
     * <code>mglyph</code> 의 태그 번호.
     */
    public static final int MGLYPH = 67;

    /**
     * Tag id of <code>mi</code>.
     * <p>
     * <code>mi</code> 의 태그 번호.
     */
    public static final int MI = 68;

    /**
     * Tag id of <code>mn</code>.
     * <p>
     * <code>mn</code> 의 태그 번호.
     */
    public static final int MN = 69;

    /**
     * Tag id of <code>mo</code>.
     * <p>
     * <code>mo</code> 의 태그 번호.
     */
    public static final int MO = 70;

    /**
     * Tag id of <code>ms</code>.
     * <p>
     * <code>ms</code> 의 태그 번호.
     */
    public static final int MS = 71;

    /**
     * Tag id of <code>mtext</code>.
     * <p>
     * <code>mtext</code> 의 태그 번호.
     */
    public static final int MTEXT = 72;

    /**
     * Tag id of <code>nav</code>.
     * <p>
     * <code>nav</code> 의 태그 번호.
     */
    public static final int NAV = 73;

    /**
     * Tag id of <code>nobr</code>.
     * <p>
     * <code>nobr</code> 의 태그 번호.
     */
    public static final int NOBR = 74;

    /**
     * Tag id of <code>noembed</code>.
     * <p>
     * <code>noembed</code> 의 태그 번호.
     */
    public static final int NOEMBED = 75;

    /**
     * Tag id of <code>noframes</code>.
     * <p>
     * <code>noframes</code> 의 태그 번호.
     */
    public static final int NOFRAMES = 76;

    /**
     * Tag id of <code>noscript</code>.
     * <p>
     * <code>noscript</code> 의 태그 번호.
     */
    public static final int NOSCRIPT = 77;

    /**
     * Tag id of <code>object</code>.
     * <p>
     * <code>object</code> 의 태그 번호.
     */
    public static final int OBJECT = 78;

    /**
     * Tag id of <code>ol</code>.
     * <p>
     * <code>ol</code> 의 태그 번호.
     */
    public static final int OL = 79;

    /**
     * Tag id of <code>optgroup</code>.
     * <p>
     * <code>optgroup</code> 의 태그 번호.
     */
    public static final int OPTGROUP = 80;

    /**
     * Tag id of <code>option</code>.
     * <p>
     * <code>option</code> 의 태그 번호.
     */
    public static final int OPTION = 81;

    /**
     * Tag id of <code>p</code>.
     * <p>
     * <code>p</code> 의 태그 번호.
     */
    public static final int P = 82;

    /**
     * Tag id of <code>param</code>.
     * <p>
     * <code>param</code> 의 태그 번호.
     */
    public static final int PARAM = 83;

    /**
     * Tag id of <code>plaintext</code>.
     * <p>
     * <code>plaintext</code> 의 태그 번호.
     */
    public static final int PLAINTEXT = 84;

    /**
     * Tag id of <code>pre</code>.
     * <p>
     * <code>pre</code> 의 태그 번호.
     */
    public static final int PRE = 85;

    /**
     * Tag id of <code>rb</code>.
     * <p>
     * <code>rb</code> 의 태그 번호.
     */
    public static final int RB = 86;

    /**
     * Tag id of <code>rp</code>.
     * <p>
     * <code>rp</code> 의 태그 번호.
     */
    public static final int RP = 87;

    /**
     * Tag id of <code>rt</code>.
     * <p>
     * <code>rt</code> 의 태그 번호.
     */
    public static final int RT = 88;

    /**
     * Tag id of <code>rtc</code>.
     * <p>
     * <code>rtc</code> 의 태그 번호.
     */
    public static final int RTC = 89;

    /**
     * Tag id of <code>ruby</code>.
     * <p>
     * <code>ruby</code> 의 태그 번호.
     */
    public static final int RUBY = 90;

    /**
     * Tag id of <code>s</code>.
     * <p>
     * <code>s</code> 의 태그 번호.
     */
    public static final int S = 91;

    /**
     * Tag id of <code>script</code>.
     * <p>
     * <code>script</code> 의 태그 번호.
     */
    public static final int SCRIPT = 92;

    /**
     * Tag id of <code>search</code>.
     * <p>
     * <code>search</code> 의 태그 번호.
     */
    public static final int SEARCH = 93;

    /**
     * Tag id of <code>section</code>.
     * <p>
     * <code>section</code> 의 태그 번호.
     */
    public static final int SECTION = 94;

    /**
     * Tag id of <code>select</code>.
     * <p>
     * <code>select</code> 의 태그 번호.
     */
    public static final int SELECT = 95;

    /**
     * Tag id of <code>small</code>.
     * <p>
     * <code>small</code> 의 태그 번호.
     */
    public static final int SMALL = 96;

    /**
     * Tag id of <code>source</code>.
     * <p>
     * <code>source</code> 의 태그 번호.
     */
    public static final int SOURCE = 97;

    /**
     * Tag id of <code>span</code>.
     * <p>
     * <code>span</code> 의 태그 번호.
     */
    public static final int SPAN = 98;

    /**
     * Tag id of <code>strike</code>.
     * <p>
     * <code>strike</code> 의 태그 번호.
     */
    public static final int STRIKE = 99;

    /**
     * Tag id of <code>strong</code>.
     * <p>
     * <code>strong</code> 의 태그 번호.
     */
    public static final int STRONG = 100;

    /**
     * Tag id of <code>style</code>.
     * <p>
     * <code>style</code> 의 태그 번호.
     */
    public static final int STYLE = 101;

    /**
     * Tag id of <code>sub</code>.
     * <p>
     * <code>sub</code> 의 태그 번호.
     */
    public static final int SUB = 102;

    /**
     * Tag id of <code>summary</code>.
     * <p>
     * <code>summary</code> 의 태그 번호.
     */
    public static final int SUMMARY = 103;

    /**
     * Tag id of <code>sup</code>.
     * <p>
     * <code>sup</code> 의 태그 번호.
     */
    public static final int SUP = 104;

    /**
     * Tag id of <code>svg</code>.
     * <p>
     * <code>svg</code> 의 태그 번호.
     */
    public static final int SVG = 105;

    /**
     * Tag id of <code>table</code>.
     * <p>
     * <code>table</code> 의 태그 번호.
     */
    public static final int TABLE = 106;

    /**
     * Tag id of <code>tbody</code>.
     * <p>
     * <code>tbody</code> 의 태그 번호.
     */
    public static final int TBODY = 107;

    /**
     * Tag id of <code>td</code>.
     * <p>
     * <code>td</code> 의 태그 번호.
     */
    public static final int TD = 108;

    /**
     * Tag id of <code>template</code>.
     * <p>
     * <code>template</code> 의 태그 번호.
     */
    public static final int TEMPLATE = 109;

    /**
     * Tag id of <code>textarea</code>.
     * <p>
     * <code>textarea</code> 의 태그 번호.
     */
    public static final int TEXTAREA = 110;

    /**
     * Tag id of <code>tfoot</code>.
     * <p>
     * <code>tfoot</code> 의 태그 번호.
     */
    public static final int TFOOT = 111;

    /**
     * Tag id of <code>th</code>.
     * <p>
     * <code>th</code> 의 태그 번호.
     */
    public static final int TH = 112;

    /**
     * Tag id of <code>thead</code>.
     * <p>
     * <code>thead</code> 의 태그 번호.
     */
    public static final int THEAD = 113;

    /**
     * Tag id of <code>title</code>.
     * <p>
     * <code>title</code> 의 태그 번호.
     */
    public static final int TITLE = 114;

    /**
     * Tag id of <code>tr</code>.
     * <p>
     * <code>tr</code> 의 태그 번호.
     */
    public static final int TR = 115;

    /**
     * Tag id of <code>track</code>.
     * <p>
     * <code>track</code> 의 태그 번호.
     */
    public static final int TRACK = 116;

    /**
     * Tag id of <code>tt</code>.
     * <p>
     * <code>tt</code> 의 태그 번호.
     */
    public static final int TT = 117;

    /**
     * Tag id of <code>u</code>.
     * <p>
     * <code>u</code> 의 태그 번호.
     */
    public static final int U = 118;

    /**
     * Tag id of <code>ul</code>.
     * <p>
     * <code>ul</code> 의 태그 번호.
     */
    public static final int UL = 119;

    /**
     * Tag id of <code>var</code>.
     * <p>
     * <code>var</code> 의 태그 번호.
     */
    public static final int VAR = 120;

    /**
     * Tag id of <code>wbr</code>.
     * <p>
     * <code>wbr</code> 의 태그 번호.
     */
    public static final int WBR = 121;

    /**
     * Tag id of <code>xmp</code>.
     * <p>
     * <code>xmp</code> 의 태그 번호.
     */
    public static final int XMP = 122;

    /**
     * Number of ids (the largest id plus one).
     * <p>
     * 번호의 수(가장 큰 번호 더하기 하나).
     */
    public static final int COUNT = 123;

    /**
     * Names by id (index 0 is the empty name of "other").
     * <p>
     * 번호별 이름(0번 칸은 "그 밖의 것"을 뜻하는 빈 이름).
     */
    private static final String[] NAMES = {"",
            "a", "address", "annotation-xml", "applet", "area", "article", "aside", "b",
            "base", "basefont", "bgsound", "big", "blockquote", "body", "br", "button",
            "caption", "center", "code", "col", "colgroup", "dd", "desc", "details",
            "dialog", "dir", "div", "dl", "dt", "em", "embed", "fieldset",
            "figcaption", "figure", "font", "footer", "foreignobject", "form", "frame", "frameset",
            "h1", "h2", "h3", "h4", "h5", "h6", "head", "header",
            "hgroup", "hr", "html", "i", "iframe", "image", "img", "input",
            "keygen", "li", "link", "listing", "main", "malignmark", "marquee", "math",
            "menu", "meta", "mglyph", "mi", "mn", "mo", "ms", "mtext",
            "nav", "nobr", "noembed", "noframes", "noscript", "object", "ol", "optgroup",
            "option", "p", "param", "plaintext", "pre", "rb", "rp", "rt",
            "rtc", "ruby", "s", "script", "search", "section", "select", "small",
            "source", "span", "strike", "strong", "style", "sub", "summary", "sup",
            "svg", "table", "tbody", "td", "template", "textarea", "tfoot", "th",
            "thead", "title", "tr", "track", "tt", "u", "ul", "var",
            "wbr", "xmp"
    };

    /**
     * Name to id table.
     * <p>
     * 이름에서 번호로의 표.
     */
    private static final Map<String, Integer> IDS = new HashMap<String, Integer>();

    /**
     * Special category of the HTML namespace, by id.
     * <p>
     * HTML 네임스페이스의 special 범주(번호별).
     */
    private static final boolean[] SPECIAL = new boolean[COUNT];

    /**
     * Formatting category, by id.
     * <p>
     * formatting 범주(번호별).
     */
    private static final boolean[] FORMATTING = new boolean[COUNT];

    /**
     * Elements that end the default "has an element in scope" search in the HTML namespace, by id.
     * <p>
     * HTML 네임스페이스에서 기본 "has an element in scope" 검색을 끝내는 요소(번호별).
     */
    private static final boolean[] SCOPE_HTML = new boolean[COUNT];

    /**
     * Elements that end the "implied end tags" generation (the elements whose end tag may be omitted), by id.
     * <p>
     * "implied end tags" 생성 대상(종료 태그를 생략할 수 있는 요소)(번호별).
     */
    private static final boolean[] IMPLIED_END = new boolean[COUNT];

    /**
     * Elements that are allowed to stay open when the body ends ("a node that is not one of ... is a parse error"), by id.
     * <p>
     * body 가 끝날 때 열려 있어도 되는 요소("... 가 아닌 노드가 있으면 파싱 오류")(번호별).
     */
    private static final boolean[] OPEN_OK_AT_END = new boolean[COUNT];

    static {
        // 이름 표를 채운다.
        for (int i = 1; i < NAMES.length; i++) {
            IDS.put(NAMES[i], Integer.valueOf(i));
        }
        mark(SPECIAL, "address applet area article aside base basefont bgsound blockquote body br button caption center col colgroup dd "
                + "details dir div dl dt embed fieldset figcaption figure footer form frame frameset h1 h2 h3 h4 h5 h6 head header hgroup hr "
                + "html iframe img input keygen li link listing main marquee menu meta nav noembed noframes noscript object ol p param "
                + "plaintext pre script search section select source style summary table tbody td template textarea tfoot th thead title tr "
                + "track ul wbr xmp");
        mark(FORMATTING, "a b big code em font i nobr s small strike strong tt u");
        mark(SCOPE_HTML, "applet caption html table td th marquee object template");
        mark(IMPLIED_END, "dd dt li optgroup option p rb rp rt rtc");
        mark(OPEN_OK_AT_END, "dd dt li optgroup option p rb rp rt rtc tbody td tfoot th thead tr body html");
    }

    /**
     * Marks the ids of a space separated list of names in a table.
     * <p>
     * 공백으로 구분한 이름 목록의 번호를 표에 표시한다.
     *
     * @param table table to fill / 채울 표
     * @param list  space separated names / 공백으로 구분한 이름
     */
    private static void mark(boolean[] table, String list) {
        String[] names = list.split(" ");
        // 이름마다 번호를 찾아 표시한다.
        for (int i = 0; i < names.length; i++) {
            table[id(names[i])] = true;
        }
    }

    /**
     * Returns the id of a lower case name.
     * <p>
     * 소문자 이름의 번호를 반환한다.
     *
     * @param name lower case name / 소문자 이름
     * @return id, or 0 if the name has none / 번호. 없으면 0
     */
    public static int id(String name) {
        Integer i = IDS.get(name);
        return i == null ? 0 : i.intValue();
    }

    /**
     * Returns the name of an id.
     * <p>
     * 번호의 이름을 반환한다.
     *
     * @param id id / 번호
     * @return name / 이름
     */
    public static String name(int id) {
        return NAMES[id];
    }

    /**
     * Tells whether an HTML element with this id is in the special category.
     * <p>
     * 이 번호의 HTML 요소가 special 범주인지 알려 준다.
     *
     * @param id id / 번호
     * @return true if special / special 이면 true
     */
    public static boolean isSpecial(int id) {
        return SPECIAL[id];
    }

    /**
     * Tells whether an HTML element with this id is in the formatting category.
     * <p>
     * 이 번호의 HTML 요소가 formatting 범주인지 알려 준다.
     *
     * @param id id / 번호
     * @return true if formatting / formatting 이면 true
     */
    public static boolean isFormatting(int id) {
        return FORMATTING[id];
    }

    /**
     * Tells whether an HTML element with this id ends the default scope search.
     * <p>
     * 이 번호의 HTML 요소가 기본 범위 검색을 끝내는지 알려 준다.
     *
     * @param id id / 번호
     * @return true if it is a scope boundary / 범위 경계이면 true
     */
    public static boolean isScopeBoundary(int id) {
        return SCOPE_HTML[id];
    }

    /**
     * Tells whether an HTML element with this id gets its end tag generated by "generate implied end tags".
     * <p>
     * 이 번호의 HTML 요소가 "generate implied end tags" 로 종료 태그가 만들어지는 요소인지 알려 준다.
     *
     * @param id id / 번호
     * @return true if implied / 암시적 종료 대상이면 true
     */
    public static boolean isImpliedEnd(int id) {
        return IMPLIED_END[id];
    }

    /**
     * Tells whether an HTML element with this id may be open when the body or the document ends without a parse error.
     * <p>
     * 이 번호의 HTML 요소가 body 나 문서가 끝날 때 열려 있어도 파싱 오류가 아닌지 알려 준다.
     *
     * @param id id / 번호
     * @return true if allowed / 허용되면 true
     */
    public static boolean isOpenAllowedAtEnd(int id) {
        return OPEN_OK_AT_END[id];
    }
}
