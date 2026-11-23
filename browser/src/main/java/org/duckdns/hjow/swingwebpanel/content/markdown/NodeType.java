package org.duckdns.hjow.swingwebpanel.content.markdown;

/**
 * Kinds of nodes of the Markdown syntax tree: the block kinds of CommonMark, its inline kinds and the GFM additions.
 * <p>
 * Markdown 구문 트리의 노드 종류: CommonMark 의 블록 종류와 인라인 종류, GFM 에서 더해진 종류.
 */
enum NodeType {
    /** The root of the tree. 트리의 루트. */
    DOCUMENT,
    /** A block quote. 인용 블록. */
    BLOCK_QUOTE,
    /** A list (bullet or ordered). 목록(순서 없음 또는 있음). */
    LIST,
    /** A list item. 목록 항목. */
    ITEM,
    /** An indented or fenced code block. 들여쓰기 또는 울타리 코드 블록. */
    CODE_BLOCK,
    /** A raw HTML block. 원시 HTML 블록. */
    HTML_BLOCK,
    /** A text paragraph. 텍스트 문단. */
    PARAGRAPH,
    /** An ATX or setext heading. ATX 또는 setext 제목. */
    HEADING,
    /** A thematic break. 주제 분리선. */
    THEMATIC_BREAK,
    /** A GFM table block. GFM 표 블록. */
    TABLE,
    /** A GFM table row. GFM 표의 행. */
    TABLE_ROW,
    /** A GFM table cell. GFM 표의 셀. */
    TABLE_CELL,
    /** Literal text. 글자 그대로의 텍스트. */
    TEXT,
    /** A soft line break. 부드러운 줄바꿈. */
    SOFTBREAK,
    /** A hard line break. 강제 줄바꿈. */
    LINEBREAK,
    /** A code span. 코드 스팬. */
    CODE,
    /** Emphasis or italic text. 강조(기울임) 텍스트. */
    EMPH,
    /** Strong emphasis. 센 강조. */
    STRONG,
    /** GFM strikethrough. GFM 취소선. */
    DEL,
    /** A hyperlink node. 하이퍼링크 노드. */
    LINK,
    /** An image. 이미지. */
    IMAGE,
    /** Inline raw HTML. 인라인 원시 HTML. */
    HTML_INLINE
}
