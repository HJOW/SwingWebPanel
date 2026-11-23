package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The tokenizer of the HTML Standard ("tokenization", 13.2.5) as an explicit state machine with all of its 80 states: data, RCDATA,
 * RAWTEXT, script data (with its escaped and double escaped variants), PLAINTEXT, tags and attributes, comments, DOCTYPE, CDATA
 * sections and the character reference states. It is pull based: {@link #nextToken()} runs the machine just far enough to produce the
 * next token, so the tree builder can change the state ({@link #setState(int)}, e.g. after <code>&lt;script&gt;</code>) between two tokens
 * exactly as the standard describes.
 * <p>
 * The input must already be normalized ({@link InputPreprocessor#normalize(String)}: only LF line ends). All parse errors are reported
 * to the {@link ParseErrorLog} with the standard's error codes; tokenization never stops because of an error. Characters are emitted
 * as runs (NUL characters stay inside a run in the data and CDATA states and are dealt with by the tree builder; in the other states
 * they are already replaced by U+FFFD). Text can be inserted at the current position ({@link #insertText(String)}), which is what
 * <code>document.write</code> needs.
 * <p>
 * The processing instruction states that the newest draft of the standard adds (<code>&lt;?target ...?&gt;</code>) are not implemented: as in
 * the 2014 recommendation and in the browsers, <code>&lt;?</code> starts a bogus comment (documented deviation).
 * <p>
 * HTML 표준의 토큰화("tokenization", 13.2.5)를 80 개 상태 전부를 갖춘 명시적 상태 기계로 구현한다: data, RCDATA, RAWTEXT, script data(escaped 와
 * double escaped 변형 포함), PLAINTEXT, 태그와 속성, 주석, DOCTYPE, CDATA 섹션, 문자 참조 상태. 당겨 가는(pull) 방식이다: {@link #nextToken()} 이
 * 다음 토큰을 만들 만큼만 기계를 돌리므로, 트리 생성기가 표준에 적힌 대로 두 토큰 사이에(예: <code>&lt;script&gt;</code> 뒤에) 상태를 바꿀 수 있다
 * ({@link #setState(int)}).
 * <p>
 * 입력은 이미 정규화되어 있어야 한다({@link InputPreprocessor#normalize(String)}: 줄 끝은 LF 뿐). 모든 파싱 오류는 표준의 오류 코드로
 * {@link ParseErrorLog} 에 보고되며, 오류 때문에 토큰화가 멈추지 않는다. 문자는 연속 구간으로 내보낸다(NUL 문자는 data 와 CDATA 상태에서는 구간 안에
 * 남겨 트리 생성기가 처리하고, 다른 상태에서는 이미 U+FFFD 로 바뀐다). 현재 위치에 문자열을 삽입할 수 있다({@link #insertText(String)}).
 * <code>document.write</code> 가 필요로 하는 기능이다.
 * <p>
 * 표준 최신 초안이 추가한 처리 명령 상태(<code>&lt;?target ...?&gt;</code>)는 구현하지 않았다. 2014 권고안과 브라우저처럼 <code>&lt;?</code> 는 bogus
 * 주석을 시작한다(문서화된 차이).
 */
public final class HtmlTokenizer {

    /**
     * State: data.
     * <p>
     * 상태: data.
     */
    public static final int DATA = 0;

    /**
     * State: RCDATA (<code>title</code>, <code>textarea</code>).
     * <p>
     * 상태: RCDATA(<code>title</code>, <code>textarea</code>).
     */
    public static final int RCDATA = 1;

    /**
     * State: RAWTEXT (<code>style</code>, <code>xmp</code>, ...).
     * <p>
     * 상태: RAWTEXT(<code>style</code>, <code>xmp</code> 등).
     */
    public static final int RAWTEXT = 2;

    /**
     * State: script data.
     * <p>
     * 상태: script data.
     */
    public static final int SCRIPT_DATA = 3;

    /**
     * State: PLAINTEXT.
     * <p>
     * 상태: PLAINTEXT.
     */
    public static final int PLAINTEXT = 4;

    /**
     * Internal state: tag open.
     * <p>
     * 내부 상태: tag open.
     */
    private static final int TAG_OPEN = 5;

    /**
     * Internal state: end tag open.
     * <p>
     * 내부 상태: end tag open.
     */
    private static final int END_TAG_OPEN = 6;

    /**
     * Internal state: tag name.
     * <p>
     * 내부 상태: tag name.
     */
    private static final int TAG_NAME = 7;

    /**
     * Internal state: RCDATA less-than sign.
     * <p>
     * 내부 상태: RCDATA less-than sign.
     */
    private static final int RCDATA_LT = 8;

    /**
     * Internal state: RCDATA end tag open.
     * <p>
     * 내부 상태: RCDATA end tag open.
     */
    private static final int RCDATA_END_TAG_OPEN = 9;

    /**
     * Internal state: RCDATA end tag name.
     * <p>
     * 내부 상태: RCDATA end tag name.
     */
    private static final int RCDATA_END_TAG_NAME = 10;

    /**
     * Internal state: RAWTEXT less-than sign.
     * <p>
     * 내부 상태: RAWTEXT less-than sign.
     */
    private static final int RAWTEXT_LT = 11;

    /**
     * Internal state: RAWTEXT end tag open.
     * <p>
     * 내부 상태: RAWTEXT end tag open.
     */
    private static final int RAWTEXT_END_TAG_OPEN = 12;

    /**
     * Internal state: RAWTEXT end tag name.
     * <p>
     * 내부 상태: RAWTEXT end tag name.
     */
    private static final int RAWTEXT_END_TAG_NAME = 13;

    /**
     * Internal state: script data less-than sign.
     * <p>
     * 내부 상태: script data less-than sign.
     */
    private static final int SCRIPT_LT = 14;

    /**
     * Internal state: script data end tag open.
     * <p>
     * 내부 상태: script data end tag open.
     */
    private static final int SCRIPT_END_TAG_OPEN = 15;

    /**
     * Internal state: script data end tag name.
     * <p>
     * 내부 상태: script data end tag name.
     */
    private static final int SCRIPT_END_TAG_NAME = 16;

    /**
     * Internal state: script data escape start.
     * <p>
     * 내부 상태: script data escape start.
     */
    private static final int SCRIPT_ESCAPE_START = 17;

    /**
     * Internal state: script data escape start dash.
     * <p>
     * 내부 상태: script data escape start dash.
     */
    private static final int SCRIPT_ESCAPE_START_DASH = 18;

    /**
     * Internal state: script data escaped.
     * <p>
     * 내부 상태: script data escaped.
     */
    private static final int SCRIPT_ESCAPED = 19;

    /**
     * Internal state: script data escaped dash.
     * <p>
     * 내부 상태: script data escaped dash.
     */
    private static final int SCRIPT_ESCAPED_DASH = 20;

    /**
     * Internal state: script data escaped dash dash.
     * <p>
     * 내부 상태: script data escaped dash dash.
     */
    private static final int SCRIPT_ESCAPED_DASH_DASH = 21;

    /**
     * Internal state: script data escaped less-than sign.
     * <p>
     * 내부 상태: script data escaped less-than sign.
     */
    private static final int SCRIPT_ESCAPED_LT = 22;

    /**
     * Internal state: script data escaped end tag open.
     * <p>
     * 내부 상태: script data escaped end tag open.
     */
    private static final int SCRIPT_ESCAPED_END_TAG_OPEN = 23;

    /**
     * Internal state: script data escaped end tag name.
     * <p>
     * 내부 상태: script data escaped end tag name.
     */
    private static final int SCRIPT_ESCAPED_END_TAG_NAME = 24;

    /**
     * Internal state: script data double escape start.
     * <p>
     * 내부 상태: script data double escape start.
     */
    private static final int SCRIPT_DOUBLE_ESCAPE_START = 25;

    /**
     * Internal state: script data double escaped.
     * <p>
     * 내부 상태: script data double escaped.
     */
    private static final int SCRIPT_DOUBLE_ESCAPED = 26;

    /**
     * Internal state: script data double escaped dash.
     * <p>
     * 내부 상태: script data double escaped dash.
     */
    private static final int SCRIPT_DOUBLE_ESCAPED_DASH = 27;

    /**
     * Internal state: script data double escaped dash dash.
     * <p>
     * 내부 상태: script data double escaped dash dash.
     */
    private static final int SCRIPT_DOUBLE_ESCAPED_DASH_DASH = 28;

    /**
     * Internal state: script data double escaped less-than sign.
     * <p>
     * 내부 상태: script data double escaped less-than sign.
     */
    private static final int SCRIPT_DOUBLE_ESCAPED_LT = 29;

    /**
     * Internal state: script data double escape end.
     * <p>
     * 내부 상태: script data double escape end.
     */
    private static final int SCRIPT_DOUBLE_ESCAPE_END = 30;

    /**
     * Internal state: before attribute name.
     * <p>
     * 내부 상태: before attribute name.
     */
    private static final int BEFORE_ATTR_NAME = 31;

    /**
     * Internal state: attribute name.
     * <p>
     * 내부 상태: attribute name.
     */
    private static final int ATTR_NAME = 32;

    /**
     * Internal state: after attribute name.
     * <p>
     * 내부 상태: after attribute name.
     */
    private static final int AFTER_ATTR_NAME = 33;

    /**
     * Internal state: before attribute value.
     * <p>
     * 내부 상태: before attribute value.
     */
    private static final int BEFORE_ATTR_VALUE = 34;

    /**
     * Internal state: attribute value (double-quoted).
     * <p>
     * 내부 상태: attribute value (double-quoted).
     */
    private static final int ATTR_VALUE_DQ = 35;

    /**
     * Internal state: attribute value (single-quoted).
     * <p>
     * 내부 상태: attribute value (single-quoted).
     */
    private static final int ATTR_VALUE_SQ = 36;

    /**
     * Internal state: attribute value (unquoted).
     * <p>
     * 내부 상태: attribute value (unquoted).
     */
    private static final int ATTR_VALUE_UNQ = 37;

    /**
     * Internal state: after attribute value (quoted).
     * <p>
     * 내부 상태: after attribute value (quoted).
     */
    private static final int AFTER_ATTR_VALUE_QUOTED = 38;

    /**
     * Internal state: self-closing start tag.
     * <p>
     * 내부 상태: self-closing start tag.
     */
    private static final int SELF_CLOSING_START = 39;

    /**
     * Internal state: bogus comment.
     * <p>
     * 내부 상태: bogus comment.
     */
    private static final int BOGUS_COMMENT = 40;

    /**
     * Internal state: markup declaration open.
     * <p>
     * 내부 상태: markup declaration open.
     */
    private static final int MARKUP_DECL_OPEN = 41;

    /**
     * Internal state: comment start.
     * <p>
     * 내부 상태: comment start.
     */
    private static final int COMMENT_START = 42;

    /**
     * Internal state: comment start dash.
     * <p>
     * 내부 상태: comment start dash.
     */
    private static final int COMMENT_START_DASH = 43;

    /**
     * Internal state: comment.
     * <p>
     * 내부 상태: comment.
     */
    private static final int COMMENT = 44;

    /**
     * Internal state: comment less-than sign.
     * <p>
     * 내부 상태: comment less-than sign.
     */
    private static final int COMMENT_LT = 45;

    /**
     * Internal state: comment less-than sign bang.
     * <p>
     * 내부 상태: comment less-than sign bang.
     */
    private static final int COMMENT_LT_BANG = 46;

    /**
     * Internal state: comment less-than sign bang dash.
     * <p>
     * 내부 상태: comment less-than sign bang dash.
     */
    private static final int COMMENT_LT_BANG_DASH = 47;

    /**
     * Internal state: comment less-than sign bang dash dash.
     * <p>
     * 내부 상태: comment less-than sign bang dash dash.
     */
    private static final int COMMENT_LT_BANG_DASH_DASH = 48;

    /**
     * Internal state: comment end dash.
     * <p>
     * 내부 상태: comment end dash.
     */
    private static final int COMMENT_END_DASH = 49;

    /**
     * Internal state: comment end.
     * <p>
     * 내부 상태: comment end.
     */
    private static final int COMMENT_END = 50;

    /**
     * Internal state: comment end bang.
     * <p>
     * 내부 상태: comment end bang.
     */
    private static final int COMMENT_END_BANG = 51;

    /**
     * Internal state: DOCTYPE.
     * <p>
     * 내부 상태: DOCTYPE.
     */
    private static final int DOCTYPE = 52;

    /**
     * Internal state: before DOCTYPE name.
     * <p>
     * 내부 상태: before DOCTYPE name.
     */
    private static final int BEFORE_DOCTYPE_NAME = 53;

    /**
     * Internal state: DOCTYPE name.
     * <p>
     * 내부 상태: DOCTYPE name.
     */
    private static final int DOCTYPE_NAME = 54;

    /**
     * Internal state: after DOCTYPE name.
     * <p>
     * 내부 상태: after DOCTYPE name.
     */
    private static final int AFTER_DOCTYPE_NAME = 55;

    /**
     * Internal state: after DOCTYPE public keyword.
     * <p>
     * 내부 상태: after DOCTYPE public keyword.
     */
    private static final int AFTER_DOCTYPE_PUBLIC_KEYWORD = 56;

    /**
     * Internal state: before DOCTYPE public identifier.
     * <p>
     * 내부 상태: before DOCTYPE public identifier.
     */
    private static final int BEFORE_DOCTYPE_PUBLIC_ID = 57;

    /**
     * Internal state: DOCTYPE public identifier (double-quoted).
     * <p>
     * 내부 상태: DOCTYPE public identifier (double-quoted).
     */
    private static final int DOCTYPE_PUBLIC_ID_DQ = 58;

    /**
     * Internal state: DOCTYPE public identifier (single-quoted).
     * <p>
     * 내부 상태: DOCTYPE public identifier (single-quoted).
     */
    private static final int DOCTYPE_PUBLIC_ID_SQ = 59;

    /**
     * Internal state: after DOCTYPE public identifier.
     * <p>
     * 내부 상태: after DOCTYPE public identifier.
     */
    private static final int AFTER_DOCTYPE_PUBLIC_ID = 60;

    /**
     * Internal state: between DOCTYPE public and system identifiers.
     * <p>
     * 내부 상태: between DOCTYPE public and system identifiers.
     */
    private static final int BETWEEN_PUBLIC_AND_SYSTEM = 61;

    /**
     * Internal state: after DOCTYPE system keyword.
     * <p>
     * 내부 상태: after DOCTYPE system keyword.
     */
    private static final int AFTER_DOCTYPE_SYSTEM_KEYWORD = 62;

    /**
     * Internal state: before DOCTYPE system identifier.
     * <p>
     * 내부 상태: before DOCTYPE system identifier.
     */
    private static final int BEFORE_DOCTYPE_SYSTEM_ID = 63;

    /**
     * Internal state: DOCTYPE system identifier (double-quoted).
     * <p>
     * 내부 상태: DOCTYPE system identifier (double-quoted).
     */
    private static final int DOCTYPE_SYSTEM_ID_DQ = 64;

    /**
     * Internal state: DOCTYPE system identifier (single-quoted).
     * <p>
     * 내부 상태: DOCTYPE system identifier (single-quoted).
     */
    private static final int DOCTYPE_SYSTEM_ID_SQ = 65;

    /**
     * Internal state: after DOCTYPE system identifier.
     * <p>
     * 내부 상태: after DOCTYPE system identifier.
     */
    private static final int AFTER_DOCTYPE_SYSTEM_ID = 66;

    /**
     * Internal state: bogus DOCTYPE.
     * <p>
     * 내부 상태: bogus DOCTYPE.
     */
    private static final int BOGUS_DOCTYPE = 67;

    /**
     * Internal state: CDATA section.
     * <p>
     * 내부 상태: CDATA section.
     */
    private static final int CDATA_SECTION = 68;

    /**
     * Internal state: CDATA section bracket.
     * <p>
     * 내부 상태: CDATA section bracket.
     */
    private static final int CDATA_BRACKET = 69;

    /**
     * Internal state: CDATA section end.
     * <p>
     * 내부 상태: CDATA section end.
     */
    private static final int CDATA_END = 70;

    /**
     * Replacement characters of the numeric character references 0x80-0x9F (0 means no replacement), indexed by code minus 0x80.
     * <p>
     * 숫자 문자 참조 0x80-0x9F 의 치환 문자(0 이면 치환 없음). 코드에서 0x80 을 뺀 값으로 찾는다.
     */
    private static final int[] C1_REPLACEMENTS = {0x20AC, 0, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6, 0x2030, 0x0160, 0x2039,
            0x0152, 0, 0x017D, 0, 0, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0,
            0x017E, 0x0178};

    /**
     * The input text, normalized (LF line ends). It grows when text is inserted.
     * <p>
     * 입력 문자열(줄 끝은 LF 로 정규화됨). 문자열이 삽입되면 늘어난다.
     */
    private final StringBuilder in;

    /**
     * Index of the next input character.
     * <p>
     * 다음 입력 문자의 위치.
     */
    private int pos;

    /**
     * Current state.
     * <p>
     * 현재 상태.
     */
    private int state = DATA;

    /**
     * Pending character run that has not been turned into a token yet.
     * <p>
     * 아직 토큰으로 만들지 않은 문자 연속 구간.
     */
    private final StringBuilder text = new StringBuilder();

    /**
     * Tokens that are ready to be returned.
     * <p>
     * 돌려줄 준비가 된 토큰.
     */
    private final LinkedList<HtmlToken> queue = new LinkedList<HtmlToken>();

    /**
     * Error collector.
     * <p>
     * 오류 수집기.
     */
    private final ParseErrorLog errors;

    /**
     * Name of the last start tag that was emitted (for the "appropriate end tag token" test), or null.
     * <p>
     * 마지막으로 내보낸 시작 태그의 이름("appropriate end tag token" 판정용). 없으면 null.
     */
    private String lastStartTag;

    /**
     * Whether a CDATA section is allowed (the adjusted current node is in a foreign namespace).
     * <p>
     * CDATA 섹션을 허용하는지(조정된 현재 노드가 외부 네임스페이스에 있음).
     */
    private boolean allowCdata;

    /**
     * True after the end-of-file token was produced.
     * <p>
     * 파일 끝 토큰을 만든 뒤에는 true.
     */
    private boolean finished;

    /**
     * Type of the tag under construction ({@link HtmlToken#START_TAG} or {@link HtmlToken#END_TAG}).
     * <p>
     * 만드는 중인 태그의 종류({@link HtmlToken#START_TAG} 또는 {@link HtmlToken#END_TAG}).
     */
    private int tagType;

    /**
     * Name of the tag under construction.
     * <p>
     * 만드는 중인 태그의 이름.
     */
    private final StringBuilder tagName = new StringBuilder();

    /**
     * Self-closing flag of the tag under construction.
     * <p>
     * 만드는 중인 태그의 self-closing 플래그.
     */
    private boolean tagSelfClosing;

    /**
     * Attribute names collected for the tag under construction.
     * <p>
     * 만드는 중인 태그에 모인 속성 이름.
     */
    private final List<String> attrNames = new ArrayList<String>();

    /**
     * Attribute values collected for the tag under construction.
     * <p>
     * 만드는 중인 태그에 모인 속성 값.
     */
    private final List<String> attrValues = new ArrayList<String>();

    /**
     * Name of the attribute being read.
     * <p>
     * 읽는 중인 속성의 이름.
     */
    private final StringBuilder curName = new StringBuilder();

    /**
     * Value of the attribute being read.
     * <p>
     * 읽는 중인 속성의 값.
     */
    private final StringBuilder curValue = new StringBuilder();

    /**
     * Offset where the name of the attribute being read ended (the position of a duplicate attribute error).
     * <p>
     * 읽는 중인 속성의 이름이 끝난 위치(중복 속성 오류의 위치).
     */
    private int curNameEnd;

    /**
     * True while an attribute is being read and has not been committed.
     * <p>
     * 속성을 읽는 중이고 아직 확정하지 않았으면 true.
     */
    private boolean curActive;

    /**
     * Data of the comment token under construction.
     * <p>
     * 만드는 중인 주석 토큰의 데이터.
     */
    private final StringBuilder comment = new StringBuilder();

    /**
     * The temporary buffer of the standard.
     * <p>
     * 표준의 임시 버퍼.
     */
    private final StringBuilder temp = new StringBuilder();

    /**
     * DOCTYPE name under construction, null if missing.
     * <p>
     * 만드는 중인 DOCTYPE 이름. 없으면 null.
     */
    private StringBuilder doctypeName;

    /**
     * DOCTYPE public identifier under construction, null if missing.
     * <p>
     * 만드는 중인 DOCTYPE 공개 식별자. 없으면 null.
     */
    private StringBuilder doctypePublic;

    /**
     * DOCTYPE system identifier under construction, null if missing.
     * <p>
     * 만드는 중인 DOCTYPE 시스템 식별자. 없으면 null.
     */
    private StringBuilder doctypeSystem;

    /**
     * DOCTYPE force-quirks flag under construction.
     * <p>
     * 만드는 중인 DOCTYPE 의 force-quirks 플래그.
     */
    private boolean doctypeQuirks;

    /**
     * Offset of the <code>&lt;</code> that started the current tag, comment or DOCTYPE.
     * <p>
     * 현재 태그, 주석, DOCTYPE 을 시작한 <code>&lt;</code> 의 위치.
     */
    private int tokenOffset;

    /**
     * Creates a tokenizer over normalized input.
     * <p>
     * 정규화된 입력에 대한 토큰화기를 만든다.
     *
     * @param input  input with LF line ends / 줄 끝이 LF 인 입력
     * @param errors error collector / 오류 수집기
     */
    public HtmlTokenizer(CharSequence input, ParseErrorLog errors) {
        this.in = new StringBuilder(input);
        this.errors = errors;
    }

    /**
     * Switches the state. The tree builder uses it with {@link #DATA}, {@link #RCDATA}, {@link #RAWTEXT}, {@link #SCRIPT_DATA} and
     * {@link #PLAINTEXT}.
     * <p>
     * 상태를 바꾼다. 트리 생성기가 {@link #DATA}, {@link #RCDATA}, {@link #RAWTEXT}, {@link #SCRIPT_DATA}, {@link #PLAINTEXT} 로 쓴다.
     *
     * @param newState new state / 새 상태
     */
    public void setState(int newState) {
        state = newState;
    }

    /**
     * Returns the current state.
     * <p>
     * 현재 상태를 반환한다.
     *
     * @return state constant / 상태 상수
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the name of the last start tag; the fragment parsing algorithm uses it for the context element.
     * <p>
     * 마지막 시작 태그의 이름을 지정한다. 조각 파싱 알고리즘이 컨텍스트 요소에 쓴다.
     *
     * @param name tag name / 태그 이름
     */
    public void setLastStartTag(String name) {
        lastStartTag = name;
    }

    /**
     * Allows or forbids CDATA sections (allowed when the adjusted current node is not in the HTML namespace).
     * <p>
     * CDATA 섹션을 허용하거나 금지한다(조정된 현재 노드가 HTML 네임스페이스가 아닐 때 허용된다).
     *
     * @param allowed true to allow / 허용하면 true
     */
    public void setAllowCdata(boolean allowed) {
        allowCdata = allowed;
    }

    /**
     * Inserts text into the input just before the next input character (<code>document.write</code>).
     * <p>
     * 다음 입력 문자 바로 앞의 입력에 문자열을 삽입한다(<code>document.write</code>).
     *
     * @param s text, already normalized to LF line ends by the caller / 문자열(호출자가 줄 끝을 LF 로 정규화한 것)
     */
    public void insertText(String s) {
        in.insert(pos, s);
    }

    /**
     * Returns the index of the next input character.
     * <p>
     * 다음 입력 문자의 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getPosition() {
        return pos;
    }

    /**
     * Tells whether the end-of-file token has been produced.
     * <p>
     * 파일 끝 토큰을 이미 만들었는지 알려 준다.
     *
     * @return true after the EOF token / 파일 끝 토큰 이후이면 true
     */
    public boolean isFinished() {
        return finished && queue.isEmpty();
    }

    /**
     * Produces the next token. After the end of the input it returns end-of-file tokens forever.
     * <p>
     * 다음 토큰을 만든다. 입력이 끝난 뒤에는 파일 끝 토큰을 계속 반환한다.
     *
     * @return the token / 토큰
     */
    public HtmlToken nextToken() {
        // 반환할 토큰이 생길 때까지 상태 기계를 돌린다.
        while (queue.isEmpty()) {
            if (finished) {
                return new HtmlToken(HtmlToken.EOF);
            }
            run();
        }
        return queue.removeFirst();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 보조 메서드
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Reports a parse error at the current position.
     * <p>
     * 현재 위치에 파싱 오류를 보고한다.
     *
     * @param code error code / 오류 코드
     */
    private void error(String code) {
        errors.add(code, pos);
    }

    /**
     * Consumes the next input character.
     * <p>
     * 다음 입력 문자를 소비한다.
     *
     * @return the character, or -1 at the end of the input (the position does not advance then) / 문자. 입력 끝이면 -1(이때 위치는 움직이지 않는다)
     */
    private int next() {
        if (pos >= in.length()) {
            return -1;
        }
        return in.charAt(pos++);
    }

    /**
     * Reconsumes a character: the position moves back unless the character was the end of the input.
     * <p>
     * 문자를 다시 소비하도록 되돌린다. 입력 끝이었다면 위치는 그대로 둔다.
     *
     * @param c the character that was just consumed / 방금 소비한 문자
     */
    private void unread(int c) {
        if (c >= 0) {
            pos--;
        }
    }

    /**
     * Tells whether a character is ASCII white space as the tokenizer sees it (tab, LF, FF, space; CR no longer occurs).
     * <p>
     * 문자가 토큰화기가 보는 ASCII 공백(탭, LF, FF, 공백. CR 은 더 이상 나타나지 않음)인지 알려 준다.
     *
     * @param c character / 문자
     * @return true if white space / 공백이면 true
     */
    private static boolean isSpace(int c) {
        return c == ' ' || c == '\n' || c == '\t' || c == '\f';
    }

    /**
     * Tells whether a character is an ASCII upper case letter.
     * <p>
     * 문자가 ASCII 대문자인지 알려 준다.
     *
     * @param c character / 문자
     * @return true if A-Z / A-Z 이면 true
     */
    private static boolean isUpper(int c) {
        return c >= 'A' && c <= 'Z';
    }

    /**
     * Tells whether a character is an ASCII letter.
     * <p>
     * 문자가 ASCII 알파벳인지 알려 준다.
     *
     * @param c character / 문자
     * @return true if a letter / 알파벳이면 true
     */
    private static boolean isAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Tells whether a character is an ASCII letter or digit.
     * <p>
     * 문자가 ASCII 알파벳이나 숫자인지 알려 준다.
     *
     * @param c character / 문자
     * @return true if alphanumeric / 알파벳이나 숫자이면 true
     */
    private static boolean isAlnum(int c) {
        return isAlpha(c) || (c >= '0' && c <= '9');
    }

    /**
     * Returns the lower case of an ASCII upper case letter.
     * <p>
     * ASCII 대문자의 소문자를 반환한다.
     *
     * @param c upper case letter / 대문자
     * @return lower case letter / 소문자
     */
    private static char lower(int c) {
        return (char) (c + 32);
    }

    /**
     * Turns the pending character run into a token.
     * <p>
     * 대기 중인 문자 연속 구간을 토큰으로 만든다.
     */
    private void emitText() {
        if (text.length() > 0) {
            HtmlToken t = new HtmlToken(HtmlToken.CHARACTER);
            t.data = text.toString();
            t.offset = Math.max(0, pos - t.data.length());
            text.setLength(0);
            queue.add(t);
        }
    }

    /**
     * Emits the end-of-file token (after the pending characters).
     * <p>
     * 파일 끝 토큰을 내보낸다(대기 중인 문자 뒤에).
     */
    private void emitEof() {
        emitText();
        HtmlToken t = new HtmlToken(HtmlToken.EOF);
        t.offset = pos;
        queue.add(t);
        finished = true;
    }

    /**
     * Emits the comment under construction and returns to the data state.
     * <p>
     * 만드는 중인 주석을 내보내고 data 상태로 돌아간다.
     */
    private void emitComment() {
        emitText();
        HtmlToken t = new HtmlToken(HtmlToken.COMMENT);
        t.data = comment.toString();
        t.offset = tokenOffset;
        queue.add(t);
    }

    /**
     * Starts a new tag token.
     * <p>
     * 새 태그 토큰을 시작한다.
     *
     * @param type {@link HtmlToken#START_TAG} or {@link HtmlToken#END_TAG} / 시작 태그 또는 종료 태그
     */
    private void startTag(int type) {
        tagType = type;
        tagName.setLength(0);
        tagSelfClosing = false;
        attrNames.clear();
        attrValues.clear();
        curActive = false;
    }

    /**
     * Starts a new attribute in the tag under construction, committing the previous one.
     * <p>
     * 만드는 중인 태그에서 새 속성을 시작하고 이전 속성을 확정한다.
     */
    private void startAttribute() {
        commitAttribute();
        curName.setLength(0);
        curValue.setLength(0);
        curActive = true;
    }

    /**
     * Commits the attribute being read, dropping it (with an error) if the tag already has one of that name.
     * <p>
     * 읽던 속성을 확정한다. 같은 이름의 속성이 이미 있으면 오류를 보고하고 버린다.
     */
    private void commitAttribute() {
        if (!curActive) {
            return;
        }
        curActive = false;
        String n = curName.toString();
        if (attrNames.contains(n)) {
            errors.add("duplicate-attribute", curNameEnd);
            return;
        }
        attrNames.add(n);
        attrValues.add(curValue.toString());
    }

    /**
     * Emits the tag under construction.
     * <p>
     * 만드는 중인 태그를 내보낸다.
     */
    private void emitTag() {
        commitAttribute();
        emitText();
        HtmlToken t = new HtmlToken(tagType);
        t.name = tagName.toString();
        t.offset = tokenOffset;
        if (tagType == HtmlToken.END_TAG) {
            if (!attrNames.isEmpty()) {
                error("end-tag-with-attributes");
            }
            if (tagSelfClosing) {
                error("end-tag-with-trailing-solidus");
            }
        } else {
            t.selfClosing = tagSelfClosing;
            lastStartTag = t.name;
            if (!attrNames.isEmpty()) {
                t.attrNames = new ArrayList<String>(attrNames);
                t.attrValues = new ArrayList<String>(attrValues);
            }
        }
        queue.add(t);
    }

    /**
     * Tells whether the end tag under construction is the "appropriate end tag token" (same name as the last start tag).
     * <p>
     * 만드는 중인 종료 태그가 "appropriate end tag token"(마지막 시작 태그와 이름이 같음)인지 알려 준다.
     *
     * @return true if appropriate / 적절하면 true
     */
    private boolean isAppropriateEndTag() {
        return lastStartTag != null && lastStartTag.contentEquals(tagName);
    }

    /**
     * Starts a DOCTYPE token under construction.
     * <p>
     * 만드는 중인 DOCTYPE 토큰을 시작한다.
     */
    private void startDoctype() {
        doctypeName = null;
        doctypePublic = null;
        doctypeSystem = null;
        doctypeQuirks = false;
    }

    /**
     * Emits the DOCTYPE token under construction.
     * <p>
     * 만드는 중인 DOCTYPE 토큰을 내보낸다.
     */
    private void emitDoctype() {
        emitText();
        HtmlToken t = new HtmlToken(HtmlToken.DOCTYPE);
        t.name = doctypeName == null ? null : doctypeName.toString();
        t.publicId = doctypePublic == null ? null : doctypePublic.toString();
        t.systemId = doctypeSystem == null ? null : doctypeSystem.toString();
        t.forceQuirks = doctypeQuirks;
        t.offset = tokenOffset;
        queue.add(t);
    }

    /**
     * Tells whether the input at the current position (after the character that was just consumed) continues with a string, ignoring
     * ASCII case for letters.
     * <p>
     * 방금 소비한 문자 뒤의 입력이 문자열로 이어지는지 알려 준다. 알파벳은 ASCII 대소문자를 무시한다.
     *
     * @param s          lower case or exact string to look for / 찾을 문자열
     * @param ignoreCase true to ignore ASCII case / ASCII 대소문자를 무시하려면 true
     * @return true if the next characters match / 다음 문자들이 일치하면 true
     */
    private boolean lookingAt(String s, boolean ignoreCase) {
        if (pos + s.length() > in.length()) {
            return false;
        }
        // 문자열의 글자를 입력과 하나씩 비교한다.
        for (int i = 0; i < s.length(); i++) {
            char a = in.charAt(pos + i);
            char b = s.charAt(i);
            if (a != b && !(ignoreCase && isAlpha(a) && (a | 0x20) == (b | 0x20))) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 문자 참조
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Consumes a character reference after an ampersand that has already been consumed, and appends its characters (or the literal text
     * if it is not a reference) to a buffer. This is the "character reference state" with the named and numeric reference states.
     * <p>
     * 이미 소비한 앰퍼샌드 뒤의 문자 참조를 소비해 그 문자(또는 참조가 아니면 원래 텍스트)를 버퍼에 덧붙인다. "character reference state" 와
     * 이름 있는/숫자 참조 상태에 해당한다.
     *
     * @param out    buffer that receives the text / 문자를 받는 버퍼
     * @param inAttr true if the reference is inside an attribute value / 속성 값 안의 참조이면 true
     */
    private void characterReference(StringBuilder out, boolean inAttr) {
        int n = in.length();
        if (pos >= n) {
            out.append('&');
            return;
        }
        char c = in.charAt(pos);
        if (c == '#') {
            numericReference(out);
            return;
        }
        if (!isAlnum(c)) {
            // 앰퍼샌드 뒤에 영숫자가 없으면 참조가 아니다.
            out.append('&');
            return;
        }
        int start = pos;
        int p = pos;
        int limit = Math.min(n, pos + NamedCharacterReferences.MAX_NAME_LENGTH);
        // 영숫자가 이어지는 구간(최대 32 글자)을 찾는다.
        while (p < limit && isAlnum(in.charAt(p))) {
            p++;
        }
        int run = p - start;
        String replacement = null;
        int matched = 0;
        if (p < n && in.charAt(p) == ';' && run < NamedCharacterReferences.MAX_NAME_LENGTH) {
            // 세미콜론으로 끝나는 이름이 표에 있는지 먼저 본다.
            replacement = NamedCharacterReferences.lookup(in.substring(start, p + 1));
            if (replacement != null) {
                matched = run + 1;
            }
        }
        if (replacement == null) {
            // 세미콜론 없는 옛 이름 중 가장 긴 접두사를 찾는다(가장 짧은 옛 이름은 두 글자이다).
            for (int len = run; len >= 2; len--) {
                String r = NamedCharacterReferences.lookup(in.substring(start, start + len));
                if (r != null) {
                    replacement = r;
                    matched = len;
                    break;
                }
            }
        }
        if (replacement == null) {
            // 일치하는 이름이 없다: 앰퍼샌드와 영숫자는 그대로 두고, 영숫자 뒤에 세미콜론이 오면 오류를 보고한다.
            out.append('&');
            int q = start;
            // 영숫자가 끝나는 위치를 찾는다.
            while (q < n && isAlnum(in.charAt(q))) {
                q++;
            }
            if (q < n && in.charAt(q) == ';') {
                errors.add("unknown-named-character-reference", q);
            }
            return;
        }
        boolean semicolon = in.charAt(start + matched - 1) == ';';
        if (!semicolon && inAttr) {
            // 속성 값 안에서 세미콜론 없는 참조 뒤에 '=' 나 영숫자가 오면 역사적 이유로 참조로 보지 않는다.
            int after = start + matched;
            if (after < n && (in.charAt(after) == '=' || isAlnum(in.charAt(after)))) {
                out.append('&').append(in, start, after);
                pos = after;
                return;
            }
        }
        if (!semicolon) {
            errors.add("missing-semicolon-after-character-reference", start + matched);
        }
        out.append(replacement);
        pos = start + matched;
    }

    /**
     * Consumes a numeric character reference (the position is at the number sign).
     * <p>
     * 숫자 문자 참조를 소비한다(위치는 숫자 기호에 있다).
     *
     * @param out buffer that receives the text / 문자를 받는 버퍼
     */
    private void numericReference(StringBuilder out) {
        int n = in.length();
        int p = pos + 1;
        boolean hex = false;
        if (p < n && (in.charAt(p) == 'x' || in.charAt(p) == 'X')) {
            hex = true;
            p++;
        }
        int digits = p;
        long code = 0;
        // 숫자(16진수 또는 10진수)를 읽으며 값을 모으고, 유니코드 범위를 넘으면 값을 고정한다.
        while (p < n) {
            char d = in.charAt(p);
            int v;
            if (d >= '0' && d <= '9') {
                v = d - '0';
            } else if (hex && d >= 'a' && d <= 'f') {
                v = d - 'a' + 10;
            } else if (hex && d >= 'A' && d <= 'F') {
                v = d - 'A' + 10;
            } else {
                break;
            }
            code = code * (hex ? 16 : 10) + v;
            if (code > 0x110000) {
                code = 0x110000;
            }
            p++;
        }
        if (p == digits) {
            // 숫자가 하나도 없으면 참조가 아니다: "&#" 이나 "&#x" 를 그대로 둔다.
            errors.add("absence-of-digits-in-numeric-character-reference", p);
            out.append('&').append(in, pos, digits);
            pos = digits;
            return;
        }
        if (p < n && in.charAt(p) == ';') {
            p++;
        } else {
            errors.add("missing-semicolon-after-character-reference", p);
        }
        pos = p;
        int cp = (int) code;
        if (cp == 0) {
            errors.add("null-character-reference", p);
            cp = 0xFFFD;
        } else if (cp > 0x10FFFF) {
            errors.add("character-reference-outside-unicode-range", p);
            cp = 0xFFFD;
        } else if (cp >= 0xD800 && cp <= 0xDFFF) {
            errors.add("surrogate-character-reference", p);
            cp = 0xFFFD;
        } else {
            if ((cp >= 0xFDD0 && cp <= 0xFDEF) || (cp & 0xFFFE) == 0xFFFE) {
                errors.add("noncharacter-character-reference", p);
            }
            boolean control = cp < 0x20 || (cp >= 0x7F && cp <= 0x9F);
            if (cp == 0x0D || (control && cp != 0x09 && cp != 0x0A && cp != 0x0C && cp != 0x20)) {
                errors.add("control-character-reference", p);
                if (cp >= 0x80 && cp <= 0x9F && C1_REPLACEMENTS[cp - 0x80] != 0) {
                    cp = C1_REPLACEMENTS[cp - 0x80];
                }
            }
        }
        out.appendCodePoint(cp);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 상태 기계
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Runs the state machine until at least one token has been queued.
     * <p>
     * 토큰이 하나 이상 대기열에 들어갈 때까지 상태 기계를 돌린다.
     */
    private void run() {
        // 토큰이 만들어지거나 입력이 끝날 때까지 상태에 따라 문자를 소비한다.
        while (queue.isEmpty() && !finished) {
            int c;
            switch (state) {
            case DATA:
                scanText(true, true);
                c = next();
                if (c < 0) {
                    emitEof();
                } else if (c == '&') {
                    characterReference(text, false);
                } else {
                    // '<' 이다: 태그 시작 위치를 기억한다.
                    tokenOffset = pos - 1;
                    state = TAG_OPEN;
                }
                break;
            case RCDATA:
                scanText(true, false);
                c = next();
                if (c < 0) {
                    emitEof();
                } else if (c == '&') {
                    characterReference(text, false);
                } else {
                    tokenOffset = pos - 1;
                    state = RCDATA_LT;
                }
                break;
            case RAWTEXT:
                scanText(false, false);
                c = next();
                if (c < 0) {
                    emitEof();
                } else {
                    tokenOffset = pos - 1;
                    state = RAWTEXT_LT;
                }
                break;
            case SCRIPT_DATA:
                scanText(false, false);
                c = next();
                if (c < 0) {
                    emitEof();
                } else {
                    tokenOffset = pos - 1;
                    state = SCRIPT_LT;
                }
                break;
            case PLAINTEXT:
                c = next();
                if (c < 0) {
                    emitEof();
                } else if (c == 0) {
                    error("unexpected-null-character");
                    text.append((char) 0xFFFD);
                } else {
                    text.append((char) c);
                }
                break;
            case TAG_OPEN:
                c = next();
                if (c == '!') {
                    state = MARKUP_DECL_OPEN;
                } else if (c == '/') {
                    state = END_TAG_OPEN;
                } else if (isAlpha(c)) {
                    startTag(HtmlToken.START_TAG);
                    unread(c);
                    state = TAG_NAME;
                } else if (c == '?') {
                    error("unexpected-question-mark-instead-of-tag-name");
                    comment.setLength(0);
                    unread(c);
                    state = BOGUS_COMMENT;
                } else if (c < 0) {
                    error("eof-before-tag-name");
                    text.append('<');
                    emitEof();
                } else {
                    error("invalid-first-character-of-tag-name");
                    text.append('<');
                    unread(c);
                    state = DATA;
                }
                break;
            case END_TAG_OPEN:
                c = next();
                if (isAlpha(c)) {
                    startTag(HtmlToken.END_TAG);
                    unread(c);
                    state = TAG_NAME;
                } else if (c == '>') {
                    error("missing-end-tag-name");
                    state = DATA;
                } else if (c < 0) {
                    error("eof-before-tag-name");
                    text.append("</");
                    emitEof();
                } else {
                    error("invalid-first-character-of-tag-name");
                    comment.setLength(0);
                    unread(c);
                    state = BOGUS_COMMENT;
                }
                break;
            case TAG_NAME:
                c = next();
                if (isSpace(c)) {
                    state = BEFORE_ATTR_NAME;
                } else if (c == '/') {
                    state = SELF_CLOSING_START;
                } else if (c == '>') {
                    state = DATA;
                    emitTag();
                } else if (isUpper(c)) {
                    tagName.append(lower(c));
                } else if (c == 0) {
                    error("unexpected-null-character");
                    tagName.append((char) 0xFFFD);
                } else if (c < 0) {
                    error("eof-in-tag");
                    emitEof();
                } else {
                    tagName.append((char) c);
                }
                break;
            case RCDATA_LT:
                c = next();
                if (c == '/') {
                    temp.setLength(0);
                    state = RCDATA_END_TAG_OPEN;
                } else {
                    text.append('<');
                    unread(c);
                    state = RCDATA;
                }
                break;
            case RCDATA_END_TAG_OPEN:
                endTagOpen(RCDATA, RCDATA_END_TAG_NAME);
                break;
            case RCDATA_END_TAG_NAME:
                specialEndTagName(RCDATA);
                break;
            case RAWTEXT_LT:
                c = next();
                if (c == '/') {
                    temp.setLength(0);
                    state = RAWTEXT_END_TAG_OPEN;
                } else {
                    text.append('<');
                    unread(c);
                    state = RAWTEXT;
                }
                break;
            case RAWTEXT_END_TAG_OPEN:
                endTagOpen(RAWTEXT, RAWTEXT_END_TAG_NAME);
                break;
            case RAWTEXT_END_TAG_NAME:
                specialEndTagName(RAWTEXT);
                break;
            case SCRIPT_LT:
                c = next();
                if (c == '/') {
                    temp.setLength(0);
                    state = SCRIPT_END_TAG_OPEN;
                } else if (c == '!') {
                    state = SCRIPT_ESCAPE_START;
                    text.append("<!");
                } else {
                    text.append('<');
                    unread(c);
                    state = SCRIPT_DATA;
                }
                break;
            case SCRIPT_END_TAG_OPEN:
                endTagOpen(SCRIPT_DATA, SCRIPT_END_TAG_NAME);
                break;
            case SCRIPT_END_TAG_NAME:
                specialEndTagName(SCRIPT_DATA);
                break;
            case SCRIPT_ESCAPE_START:
                c = next();
                if (c == '-') {
                    state = SCRIPT_ESCAPE_START_DASH;
                    text.append('-');
                } else {
                    unread(c);
                    state = SCRIPT_DATA;
                }
                break;
            case SCRIPT_ESCAPE_START_DASH:
                c = next();
                if (c == '-') {
                    state = SCRIPT_ESCAPED_DASH_DASH;
                    text.append('-');
                } else {
                    unread(c);
                    state = SCRIPT_DATA;
                }
                break;
            case SCRIPT_ESCAPED:
                c = next();
                if (c == '-') {
                    state = SCRIPT_ESCAPED_DASH;
                    text.append('-');
                } else if (c == '<') {
                    tokenOffset = pos - 1;
                    state = SCRIPT_ESCAPED_LT;
                } else {
                    escapedCharacter(c, SCRIPT_ESCAPED);
                }
                break;
            case SCRIPT_ESCAPED_DASH:
                c = next();
                if (c == '-') {
                    state = SCRIPT_ESCAPED_DASH_DASH;
                    text.append('-');
                } else if (c == '<') {
                    tokenOffset = pos - 1;
                    state = SCRIPT_ESCAPED_LT;
                } else {
                    state = SCRIPT_ESCAPED;
                    escapedCharacter(c, SCRIPT_ESCAPED);
                }
                break;
            case SCRIPT_ESCAPED_DASH_DASH:
                c = next();
                if (c == '-') {
                    text.append('-');
                } else if (c == '<') {
                    tokenOffset = pos - 1;
                    state = SCRIPT_ESCAPED_LT;
                } else if (c == '>') {
                    state = SCRIPT_DATA;
                    text.append('>');
                } else {
                    state = SCRIPT_ESCAPED;
                    escapedCharacter(c, SCRIPT_ESCAPED);
                }
                break;
            case SCRIPT_ESCAPED_LT:
                c = next();
                if (c == '/') {
                    temp.setLength(0);
                    state = SCRIPT_ESCAPED_END_TAG_OPEN;
                } else if (isAlpha(c)) {
                    temp.setLength(0);
                    text.append('<');
                    unread(c);
                    state = SCRIPT_DOUBLE_ESCAPE_START;
                } else {
                    text.append('<');
                    unread(c);
                    state = SCRIPT_ESCAPED;
                }
                break;
            case SCRIPT_ESCAPED_END_TAG_OPEN:
                endTagOpen(SCRIPT_ESCAPED, SCRIPT_ESCAPED_END_TAG_NAME);
                break;
            case SCRIPT_ESCAPED_END_TAG_NAME:
                specialEndTagName(SCRIPT_ESCAPED);
                break;
            case SCRIPT_DOUBLE_ESCAPE_START:
                c = next();
                if (isSpace(c) || c == '/' || c == '>') {
                    state = "script".contentEquals(temp) ? SCRIPT_DOUBLE_ESCAPED : SCRIPT_ESCAPED;
                    text.append((char) c);
                } else if (isUpper(c)) {
                    temp.append(lower(c));
                    text.append((char) c);
                } else if (isAlpha(c)) {
                    temp.append((char) c);
                    text.append((char) c);
                } else {
                    unread(c);
                    state = SCRIPT_ESCAPED;
                }
                break;
            case SCRIPT_DOUBLE_ESCAPED:
                c = next();
                if (c == '-') {
                    state = SCRIPT_DOUBLE_ESCAPED_DASH;
                    text.append('-');
                } else if (c == '<') {
                    state = SCRIPT_DOUBLE_ESCAPED_LT;
                    text.append('<');
                } else {
                    escapedCharacter(c, SCRIPT_DOUBLE_ESCAPED);
                }
                break;
            case SCRIPT_DOUBLE_ESCAPED_DASH:
                c = next();
                if (c == '-') {
                    state = SCRIPT_DOUBLE_ESCAPED_DASH_DASH;
                    text.append('-');
                } else if (c == '<') {
                    state = SCRIPT_DOUBLE_ESCAPED_LT;
                    text.append('<');
                } else {
                    state = SCRIPT_DOUBLE_ESCAPED;
                    escapedCharacter(c, SCRIPT_DOUBLE_ESCAPED);
                }
                break;
            case SCRIPT_DOUBLE_ESCAPED_DASH_DASH:
                c = next();
                if (c == '-') {
                    text.append('-');
                } else if (c == '<') {
                    state = SCRIPT_DOUBLE_ESCAPED_LT;
                    text.append('<');
                } else if (c == '>') {
                    state = SCRIPT_DATA;
                    text.append('>');
                } else {
                    state = SCRIPT_DOUBLE_ESCAPED;
                    escapedCharacter(c, SCRIPT_DOUBLE_ESCAPED);
                }
                break;
            case SCRIPT_DOUBLE_ESCAPED_LT:
                c = next();
                if (c == '/') {
                    temp.setLength(0);
                    state = SCRIPT_DOUBLE_ESCAPE_END;
                    text.append('/');
                } else {
                    unread(c);
                    state = SCRIPT_DOUBLE_ESCAPED;
                }
                break;
            case SCRIPT_DOUBLE_ESCAPE_END:
                c = next();
                if (isSpace(c) || c == '/' || c == '>') {
                    state = "script".contentEquals(temp) ? SCRIPT_ESCAPED : SCRIPT_DOUBLE_ESCAPED;
                    text.append((char) c);
                } else if (isUpper(c)) {
                    temp.append(lower(c));
                    text.append((char) c);
                } else if (isAlpha(c)) {
                    temp.append((char) c);
                    text.append((char) c);
                } else {
                    unread(c);
                    state = SCRIPT_DOUBLE_ESCAPED;
                }
                break;
            default:
                runMarkup();
                break;
            }
        }
    }

    /**
     * Scans a run of ordinary characters of the data-like states directly into the pending text.
     * <p>
     * data 계열 상태의 일반 문자 구간을 대기 텍스트에 곧바로 읽어 들인다.
     *
     * @param stopAmpersand true if an ampersand ends the run / 앰퍼샌드에서 구간을 끝내면 true
     * @param reportNul     true if NUL characters are kept in the run (data state); false if they become U+FFFD / NUL 을 구간에 남기면(data 상태) true,
     *                      U+FFFD 로 바꾸면 false
     */
    private void scanText(boolean stopAmpersand, boolean reportNul) {
        int n = in.length();
        // 특수 문자('<', 필요하면 '&', NUL)를 만날 때까지 문자를 대기 텍스트에 옮긴다.
        while (pos < n) {
            char ch = in.charAt(pos);
            if (ch == '<' || (stopAmpersand && ch == '&')) {
                return;
            }
            if (ch == 0) {
                error("unexpected-null-character");
                text.append(reportNul ? (char) 0 : (char) 0xFFFD);
            } else {
                text.append(ch);
            }
            pos++;
        }
    }

    /**
     * Handles one character of the script data (double) escaped states that is not a dash or a less-than sign.
     * <p>
     * script data (double) escaped 상태에서 대시나 꺾쇠 여는 기호가 아닌 문자 하나를 처리한다.
     *
     * @param c     the character / 문자
     * @param state state to stay in / 머무를 상태
     */
    private void escapedCharacter(int c, int state) {
        if (c == 0) {
            error("unexpected-null-character");
            text.append((char) 0xFFFD);
        } else if (c < 0) {
            error("eof-in-script-html-comment-like-text");
            emitEof();
        } else {
            text.append((char) c);
        }
        this.state = state;
    }

    /**
     * The "end tag open" states of RCDATA, RAWTEXT and the script data states.
     * <p>
     * RCDATA, RAWTEXT, script data 계열의 "end tag open" 상태.
     *
     * @param fallback state to return to if no tag name follows / 태그 이름이 이어지지 않을 때 돌아갈 상태
     * @param nameState state that reads the tag name / 태그 이름을 읽는 상태
     */
    private void endTagOpen(int fallback, int nameState) {
        int c = next();
        if (isAlpha(c)) {
            startTag(HtmlToken.END_TAG);
            unread(c);
            state = nameState;
        } else {
            text.append("</");
            unread(c);
            state = fallback;
        }
    }

    /**
     * The "end tag name" states of RCDATA, RAWTEXT and the script data states: only the appropriate end tag ends the special content.
     * <p>
     * RCDATA, RAWTEXT, script data 계열의 "end tag name" 상태: 적절한 종료 태그만 특수 내용을 끝낸다.
     *
     * @param fallback state to return to if the tag is not the appropriate end tag / 적절한 종료 태그가 아닐 때 돌아갈 상태
     */
    private void specialEndTagName(int fallback) {
        int c = next();
        if (isSpace(c) && isAppropriateEndTag()) {
            state = BEFORE_ATTR_NAME;
        } else if (c == '/' && isAppropriateEndTag()) {
            state = SELF_CLOSING_START;
        } else if (c == '>' && isAppropriateEndTag()) {
            state = DATA;
            emitTag();
        } else if (isUpper(c)) {
            tagName.append(lower(c));
            temp.append((char) c);
        } else if (isAlpha(c)) {
            tagName.append((char) c);
            temp.append((char) c);
        } else {
            // 적절한 종료 태그가 아니면 지금까지 읽은 "</이름" 을 문자로 되돌려 보낸다.
            text.append("</").append(temp);
            unread(c);
            state = fallback;
        }
    }

    /**
     * Runs the states of tags, attributes, comments, DOCTYPE and CDATA sections (all states after script data).
     * <p>
     * 태그, 속성, 주석, DOCTYPE, CDATA 섹션 상태(script data 이후의 모든 상태)를 처리한다.
     */
    private void runMarkup() {
        int c = next();
        switch (state) {
        case BEFORE_ATTR_NAME:
            if (isSpace(c)) {
                break;
            }
            if (c == '/' || c == '>' || c < 0) {
                unread(c);
                state = AFTER_ATTR_NAME;
            } else if (c == '=') {
                error("unexpected-equals-sign-before-attribute-name");
                startAttribute();
                curName.append('=');
                state = ATTR_NAME;
            } else {
                startAttribute();
                unread(c);
                state = ATTR_NAME;
            }
            break;
        case ATTR_NAME:
            if (isSpace(c) || c == '/' || c == '>' || c < 0) {
                unread(c);
                curNameEnd = pos;
                state = AFTER_ATTR_NAME;
            } else if (c == '=') {
                curNameEnd = pos;
                state = BEFORE_ATTR_VALUE;
            } else if (isUpper(c)) {
                curName.append(lower(c));
            } else if (c == 0) {
                error("unexpected-null-character");
                curName.append((char) 0xFFFD);
            } else {
                if (c == '"' || c == '\'' || c == '<') {
                    error("unexpected-character-in-attribute-name");
                }
                curName.append((char) c);
            }
            break;
        case AFTER_ATTR_NAME:
            if (isSpace(c)) {
                break;
            }
            if (c == '/') {
                state = SELF_CLOSING_START;
            } else if (c == '=') {
                state = BEFORE_ATTR_VALUE;
            } else if (c == '>') {
                state = DATA;
                emitTag();
            } else if (c < 0) {
                error("eof-in-tag");
                emitEof();
            } else {
                startAttribute();
                unread(c);
                state = ATTR_NAME;
            }
            break;
        case BEFORE_ATTR_VALUE:
            if (isSpace(c)) {
                break;
            }
            if (c == '"') {
                state = ATTR_VALUE_DQ;
            } else if (c == '\'') {
                state = ATTR_VALUE_SQ;
            } else if (c == '>') {
                error("missing-attribute-value");
                state = DATA;
                emitTag();
            } else {
                unread(c);
                state = ATTR_VALUE_UNQ;
            }
            break;
        case ATTR_VALUE_DQ:
        case ATTR_VALUE_SQ:
            attributeValueQuoted(c, state == ATTR_VALUE_DQ ? '"' : '\'');
            break;
        case ATTR_VALUE_UNQ:
            if (isSpace(c)) {
                state = BEFORE_ATTR_NAME;
            } else if (c == '&') {
                characterReference(curValue, true);
            } else if (c == '>') {
                state = DATA;
                emitTag();
            } else if (c == 0) {
                error("unexpected-null-character");
                curValue.append((char) 0xFFFD);
            } else if (c < 0) {
                error("eof-in-tag");
                emitEof();
            } else {
                if (c == '"' || c == '\'' || c == '<' || c == '=' || c == '`') {
                    error("unexpected-character-in-unquoted-attribute-value");
                }
                curValue.append((char) c);
            }
            break;
        case AFTER_ATTR_VALUE_QUOTED:
            if (isSpace(c)) {
                state = BEFORE_ATTR_NAME;
            } else if (c == '/') {
                state = SELF_CLOSING_START;
            } else if (c == '>') {
                state = DATA;
                emitTag();
            } else if (c < 0) {
                error("eof-in-tag");
                emitEof();
            } else {
                error("missing-whitespace-between-attributes");
                unread(c);
                state = BEFORE_ATTR_NAME;
            }
            break;
        case SELF_CLOSING_START:
            if (c == '>') {
                tagSelfClosing = true;
                state = DATA;
                emitTag();
            } else if (c < 0) {
                error("eof-in-tag");
                emitEof();
            } else {
                error("unexpected-solidus-in-tag");
                unread(c);
                state = BEFORE_ATTR_NAME;
            }
            break;
        case BOGUS_COMMENT:
            if (c == '>') {
                state = DATA;
                emitComment();
            } else if (c < 0) {
                emitComment();
                emitEof();
            } else if (c == 0) {
                error("unexpected-null-character");
                comment.append((char) 0xFFFD);
            } else {
                comment.append((char) c);
            }
            break;
        case MARKUP_DECL_OPEN:
            unread(c);
            markupDeclarationOpen();
            break;
        default:
            runCommentAndDoctype(c);
            break;
        }
    }

    /**
     * Handles a quoted attribute value character.
     * <p>
     * 따옴표로 묶인 속성 값의 문자 하나를 처리한다.
     *
     * @param c     character / 문자
     * @param quote the quote character that ends the value / 값을 끝내는 따옴표 문자
     */
    private void attributeValueQuoted(int c, char quote) {
        if (c == quote) {
            state = AFTER_ATTR_VALUE_QUOTED;
        } else if (c == '&') {
            characterReference(curValue, true);
        } else if (c == 0) {
            error("unexpected-null-character");
            curValue.append((char) 0xFFFD);
        } else if (c < 0) {
            error("eof-in-tag");
            emitEof();
        } else {
            curValue.append((char) c);
        }
    }

    /**
     * The "markup declaration open" state: decides between a comment, a DOCTYPE, a CDATA section and a bogus comment.
     * <p>
     * "markup declaration open" 상태: 주석, DOCTYPE, CDATA 섹션, bogus 주석 중 무엇인지 정한다.
     */
    private void markupDeclarationOpen() {
        comment.setLength(0);
        if (lookingAt("--", false)) {
            pos += 2;
            state = COMMENT_START;
        } else if (lookingAt("doctype", true)) {
            pos += 7;
            state = DOCTYPE;
        } else if (lookingAt("[CDATA[", false)) {
            pos += 7;
            if (allowCdata) {
                state = CDATA_SECTION;
            } else {
                error("cdata-in-html-content");
                comment.append("[CDATA[");
                state = BOGUS_COMMENT;
            }
        } else {
            error("incorrectly-opened-comment");
            state = BOGUS_COMMENT;
        }
    }

    /**
     * Runs the comment, DOCTYPE and CDATA states.
     * <p>
     * 주석, DOCTYPE, CDATA 상태를 처리한다.
     *
     * @param c the consumed character / 소비한 문자
     */
    private void runCommentAndDoctype(int c) {
        switch (state) {
        case COMMENT_START:
            if (c == '-') {
                state = COMMENT_START_DASH;
            } else if (c == '>') {
                error("abrupt-closing-of-empty-comment");
                state = DATA;
                emitComment();
            } else {
                unread(c);
                state = COMMENT;
            }
            break;
        case COMMENT_START_DASH:
            if (c == '-') {
                state = COMMENT_END;
            } else if (c == '>') {
                error("abrupt-closing-of-empty-comment");
                state = DATA;
                emitComment();
            } else if (c < 0) {
                commentEof();
            } else {
                comment.append('-');
                unread(c);
                state = COMMENT;
            }
            break;
        case COMMENT:
            if (c == '<') {
                comment.append('<');
                state = COMMENT_LT;
            } else if (c == '-') {
                state = COMMENT_END_DASH;
            } else if (c == 0) {
                error("unexpected-null-character");
                comment.append((char) 0xFFFD);
            } else if (c < 0) {
                commentEof();
            } else {
                comment.append((char) c);
            }
            break;
        case COMMENT_LT:
            if (c == '!') {
                comment.append('!');
                state = COMMENT_LT_BANG;
            } else if (c == '<') {
                comment.append('<');
            } else {
                unread(c);
                state = COMMENT;
            }
            break;
        case COMMENT_LT_BANG:
            if (c == '-') {
                state = COMMENT_LT_BANG_DASH;
            } else {
                unread(c);
                state = COMMENT;
            }
            break;
        case COMMENT_LT_BANG_DASH:
            if (c == '-') {
                state = COMMENT_LT_BANG_DASH_DASH;
            } else {
                unread(c);
                state = COMMENT_END_DASH;
            }
            break;
        case COMMENT_LT_BANG_DASH_DASH:
            if (c != '>' && c >= 0) {
                error("nested-comment");
            }
            unread(c);
            state = COMMENT_END;
            break;
        case COMMENT_END_DASH:
            if (c == '-') {
                state = COMMENT_END;
            } else if (c < 0) {
                commentEof();
            } else {
                comment.append('-');
                unread(c);
                state = COMMENT;
            }
            break;
        case COMMENT_END:
            if (c == '>') {
                state = DATA;
                emitComment();
            } else if (c == '!') {
                state = COMMENT_END_BANG;
            } else if (c == '-') {
                comment.append('-');
            } else if (c < 0) {
                commentEof();
            } else {
                comment.append("--");
                unread(c);
                state = COMMENT;
            }
            break;
        case COMMENT_END_BANG:
            if (c == '-') {
                comment.append("--!");
                state = COMMENT_END_DASH;
            } else if (c == '>') {
                error("incorrectly-closed-comment");
                state = DATA;
                emitComment();
            } else if (c < 0) {
                commentEof();
            } else {
                comment.append("--!");
                unread(c);
                state = COMMENT;
            }
            break;
        case CDATA_SECTION:
            if (c == ']') {
                state = CDATA_BRACKET;
            } else if (c < 0) {
                error("eof-in-cdata");
                emitEof();
            } else {
                text.append((char) c);
            }
            break;
        case CDATA_BRACKET:
            if (c == ']') {
                state = CDATA_END;
            } else {
                text.append(']');
                unread(c);
                state = CDATA_SECTION;
            }
            break;
        case CDATA_END:
            if (c == ']') {
                text.append(']');
            } else if (c == '>') {
                state = DATA;
            } else {
                text.append("]]");
                unread(c);
                state = CDATA_SECTION;
            }
            break;
        default:
            runDoctype(c);
            break;
        }
    }

    /**
     * Ends the input inside a comment: reports the error, emits the comment and the end of the file.
     * <p>
     * 주석 안에서 입력이 끝난 경우: 오류를 보고하고 주석과 파일 끝을 내보낸다.
     */
    private void commentEof() {
        error("eof-in-comment");
        emitComment();
        emitEof();
    }

    /**
     * Ends the input inside a DOCTYPE: reports the error, emits the DOCTYPE with the force-quirks flag and the end of the file.
     * <p>
     * DOCTYPE 안에서 입력이 끝난 경우: 오류를 보고하고 force-quirks 플래그가 켜진 DOCTYPE 과 파일 끝을 내보낸다.
     */
    private void doctypeEof() {
        error("eof-in-doctype");
        doctypeQuirks = true;
        emitDoctype();
        emitEof();
    }

    /**
     * Reports an error, sets the force-quirks flag, emits the DOCTYPE and returns to the data state.
     * <p>
     * 오류를 보고하고 force-quirks 플래그를 켠 뒤 DOCTYPE 을 내보내고 data 상태로 돌아간다.
     *
     * @param code error code / 오류 코드
     */
    private void doctypeAbort(String code) {
        error(code);
        doctypeQuirks = true;
        state = DATA;
        emitDoctype();
    }

    /**
     * Reports an error, sets the force-quirks flag and switches to the bogus DOCTYPE state, reconsuming a character.
     * <p>
     * 오류를 보고하고 force-quirks 플래그를 켠 뒤 문자를 다시 소비하도록 하며 bogus DOCTYPE 상태로 간다.
     *
     * @param code error code / 오류 코드
     * @param c    character to reconsume / 다시 소비할 문자
     */
    private void doctypeBogus(String code, int c) {
        error(code);
        doctypeQuirks = true;
        unread(c);
        state = BOGUS_DOCTYPE;
    }

    /**
     * Runs the DOCTYPE states.
     * <p>
     * DOCTYPE 상태를 처리한다.
     *
     * @param c the consumed character / 소비한 문자
     */
    private void runDoctype(int c) {
        switch (state) {
        case DOCTYPE:
            startDoctype();
            if (isSpace(c)) {
                state = BEFORE_DOCTYPE_NAME;
            } else if (c == '>') {
                unread(c);
                state = BEFORE_DOCTYPE_NAME;
            } else if (c < 0) {
                doctypeEof();
            } else {
                error("missing-whitespace-before-doctype-name");
                unread(c);
                state = BEFORE_DOCTYPE_NAME;
            }
            break;
        case BEFORE_DOCTYPE_NAME:
            if (isSpace(c)) {
                break;
            }
            if (isUpper(c)) {
                doctypeName = new StringBuilder().append(lower(c));
                state = DOCTYPE_NAME;
            } else if (c == 0) {
                error("unexpected-null-character");
                doctypeName = new StringBuilder().append((char) 0xFFFD);
                state = DOCTYPE_NAME;
            } else if (c == '>') {
                doctypeAbort("missing-doctype-name");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeName = new StringBuilder().append((char) c);
                state = DOCTYPE_NAME;
            }
            break;
        case DOCTYPE_NAME:
            if (isSpace(c)) {
                state = AFTER_DOCTYPE_NAME;
            } else if (c == '>') {
                state = DATA;
                emitDoctype();
            } else if (isUpper(c)) {
                doctypeName.append(lower(c));
            } else if (c == 0) {
                error("unexpected-null-character");
                doctypeName.append((char) 0xFFFD);
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeName.append((char) c);
            }
            break;
        case AFTER_DOCTYPE_NAME:
            if (isSpace(c)) {
                break;
            }
            if (c == '>') {
                state = DATA;
                emitDoctype();
            } else if (c < 0) {
                doctypeEof();
            } else {
                // 지금 문자부터 "PUBLIC" 또는 "SYSTEM" 인지 (대소문자 무시) 확인한다.
                unread(c);
                if (lookingAt("public", true)) {
                    pos += 6;
                    state = AFTER_DOCTYPE_PUBLIC_KEYWORD;
                } else if (lookingAt("system", true)) {
                    pos += 6;
                    state = AFTER_DOCTYPE_SYSTEM_KEYWORD;
                } else {
                    pos++;
                    doctypeBogus("invalid-character-sequence-after-doctype-name", c);
                }
            }
            break;
        case AFTER_DOCTYPE_PUBLIC_KEYWORD:
            if (isSpace(c)) {
                state = BEFORE_DOCTYPE_PUBLIC_ID;
            } else if (c == '"' || c == '\'') {
                error("missing-whitespace-after-doctype-public-keyword");
                doctypePublic = new StringBuilder();
                state = c == '"' ? DOCTYPE_PUBLIC_ID_DQ : DOCTYPE_PUBLIC_ID_SQ;
            } else if (c == '>') {
                doctypeAbort("missing-doctype-public-identifier");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeBogus("missing-quote-before-doctype-public-identifier", c);
            }
            break;
        case BEFORE_DOCTYPE_PUBLIC_ID:
            if (isSpace(c)) {
                break;
            }
            if (c == '"' || c == '\'') {
                doctypePublic = new StringBuilder();
                state = c == '"' ? DOCTYPE_PUBLIC_ID_DQ : DOCTYPE_PUBLIC_ID_SQ;
            } else if (c == '>') {
                doctypeAbort("missing-doctype-public-identifier");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeBogus("missing-quote-before-doctype-public-identifier", c);
            }
            break;
        case DOCTYPE_PUBLIC_ID_DQ:
        case DOCTYPE_PUBLIC_ID_SQ:
            if (c == (state == DOCTYPE_PUBLIC_ID_DQ ? '"' : '\'')) {
                state = AFTER_DOCTYPE_PUBLIC_ID;
            } else if (c == 0) {
                error("unexpected-null-character");
                doctypePublic.append((char) 0xFFFD);
            } else if (c == '>') {
                doctypeAbort("abrupt-doctype-public-identifier");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypePublic.append((char) c);
            }
            break;
        case AFTER_DOCTYPE_PUBLIC_ID:
            if (isSpace(c)) {
                state = BETWEEN_PUBLIC_AND_SYSTEM;
            } else if (c == '>') {
                state = DATA;
                emitDoctype();
            } else if (c == '"' || c == '\'') {
                error("missing-whitespace-between-doctype-public-and-system-identifiers");
                doctypeSystem = new StringBuilder();
                state = c == '"' ? DOCTYPE_SYSTEM_ID_DQ : DOCTYPE_SYSTEM_ID_SQ;
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeBogus("missing-quote-before-doctype-system-identifier", c);
            }
            break;
        case BETWEEN_PUBLIC_AND_SYSTEM:
            if (isSpace(c)) {
                break;
            }
            if (c == '>') {
                state = DATA;
                emitDoctype();
            } else if (c == '"' || c == '\'') {
                doctypeSystem = new StringBuilder();
                state = c == '"' ? DOCTYPE_SYSTEM_ID_DQ : DOCTYPE_SYSTEM_ID_SQ;
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeBogus("missing-quote-before-doctype-system-identifier", c);
            }
            break;
        case AFTER_DOCTYPE_SYSTEM_KEYWORD:
            if (isSpace(c)) {
                state = BEFORE_DOCTYPE_SYSTEM_ID;
            } else if (c == '"' || c == '\'') {
                error("missing-whitespace-after-doctype-system-keyword");
                doctypeSystem = new StringBuilder();
                state = c == '"' ? DOCTYPE_SYSTEM_ID_DQ : DOCTYPE_SYSTEM_ID_SQ;
            } else if (c == '>') {
                doctypeAbort("missing-doctype-system-identifier");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeBogus("missing-quote-before-doctype-system-identifier", c);
            }
            break;
        case BEFORE_DOCTYPE_SYSTEM_ID:
            if (isSpace(c)) {
                break;
            }
            if (c == '"' || c == '\'') {
                doctypeSystem = new StringBuilder();
                state = c == '"' ? DOCTYPE_SYSTEM_ID_DQ : DOCTYPE_SYSTEM_ID_SQ;
            } else if (c == '>') {
                doctypeAbort("missing-doctype-system-identifier");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeBogus("missing-quote-before-doctype-system-identifier", c);
            }
            break;
        case DOCTYPE_SYSTEM_ID_DQ:
        case DOCTYPE_SYSTEM_ID_SQ:
            if (c == (state == DOCTYPE_SYSTEM_ID_DQ ? '"' : '\'')) {
                state = AFTER_DOCTYPE_SYSTEM_ID;
            } else if (c == 0) {
                error("unexpected-null-character");
                doctypeSystem.append((char) 0xFFFD);
            } else if (c == '>') {
                doctypeAbort("abrupt-doctype-system-identifier");
            } else if (c < 0) {
                doctypeEof();
            } else {
                doctypeSystem.append((char) c);
            }
            break;
        case AFTER_DOCTYPE_SYSTEM_ID:
            if (isSpace(c)) {
                break;
            }
            if (c == '>') {
                state = DATA;
                emitDoctype();
            } else if (c < 0) {
                doctypeEof();
            } else {
                error("unexpected-character-after-doctype-system-identifier");
                unread(c);
                state = BOGUS_DOCTYPE;
            }
            break;
        default:
            // BOGUS_DOCTYPE: '>' 까지의 문자를 버린다.
            if (c == '>') {
                state = DATA;
                emitDoctype();
            } else if (c == 0) {
                error("unexpected-null-character");
            } else if (c < 0) {
                emitDoctype();
                emitEof();
            }
            break;
        }
    }
}
