package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Base of the nodes that hold character data: text, comments, CDATA sections and processing instructions. Offsets and counts are in
 * UTF-16 code units, as in the DOM Standard. Every change of the data is reported to the document's listeners with the old value.
 * <p>
 * 문자 데이터를 가진 노드(텍스트, 주석, CDATA 섹션, 처리 명령)의 기반. 오프셋과 개수는 DOM 표준과 같이 UTF-16 코드 단위이다.
 * 데이터의 모든 변경은 이전 값과 함께 문서의 리스너에 보고된다.
 */
public abstract class CharacterData extends Node {

    /**
     * The data.
     * <p>
     * 데이터.
     */
    private String data;

    /**
     * Creates a character node.
     * <p>
     * 문자 노드를 만든다.
     *
     * @param owner owner document / 소유 문서
     * @param data  initial data / 초기 데이터
     */
    CharacterData(Document owner, String data) {
        super(owner);
        this.data = data == null ? "" : data;
    }

    /**
     * Returns the data.
     * <p>
     * 데이터를 반환한다.
     *
     * @return data / 데이터
     */
    public String getData() {
        return data;
    }

    /**
     * Replaces the data.
     * <p>
     * 데이터를 교체한다.
     *
     * @param value new data, null is treated as empty / 새 데이터(null 은 빈 문자열로 취급)
     */
    public void setData(String value) {
        replaceData(0, data.length(), value == null ? "" : value);
    }

    /**
     * Returns the length of the data in UTF-16 code units.
     * <p>
     * 데이터의 길이를 UTF-16 코드 단위로 반환한다.
     *
     * @return length / 길이
     */
    public int getLength() {
        return data.length();
    }

    /**
     * Returns a part of the data.
     * <p>
     * 데이터의 일부를 반환한다.
     *
     * @param offset start offset / 시작 위치
     * @param count  number of code units / 코드 단위 수
     * @return the substring / 부분 문자열
     * @throws DomException IndexSizeError if the offset is beyond the data / 오프셋이 데이터를 넘으면 IndexSizeError
     */
    public String substringData(int offset, int count) {
        checkOffset(offset);
        return data.substring(offset, (int) Math.min((long) offset + Math.max(count, 0), data.length()));
    }

    /**
     * Appends text to the data.
     * <p>
     * 데이터 끝에 텍스트를 덧붙인다.
     *
     * @param text text to append / 덧붙일 텍스트
     */
    public void appendData(String text) {
        replaceData(data.length(), 0, text);
    }

    /**
     * Inserts text into the data.
     * <p>
     * 데이터에 텍스트를 끼워 넣는다.
     *
     * @param offset insertion offset / 삽입 위치
     * @param text   text to insert / 끼워 넣을 텍스트
     * @throws DomException IndexSizeError if the offset is beyond the data / 오프셋이 데이터를 넘으면 IndexSizeError
     */
    public void insertData(int offset, String text) {
        replaceData(offset, 0, text);
    }

    /**
     * Deletes a part of the data.
     * <p>
     * 데이터의 일부를 삭제한다.
     *
     * @param offset start offset / 시작 위치
     * @param count  number of code units / 코드 단위 수
     * @throws DomException IndexSizeError if the offset is beyond the data / 오프셋이 데이터를 넘으면 IndexSizeError
     */
    public void deleteData(int offset, int count) {
        replaceData(offset, count, "");
    }

    /**
     * Replaces a part of the data (<code>replaceData</code>).
     * <p>
     * 데이터의 일부를 교체한다(<code>replaceData</code>).
     *
     * @param offset start offset / 시작 위치
     * @param count  number of code units to replace, clamped to the end / 교체할 코드 단위 수(끝으로 제한됨)
     * @param text   replacement text, null is treated as empty / 대체 텍스트(null 은 빈 문자열)
     * @throws DomException IndexSizeError if the offset is beyond the data / 오프셋이 데이터를 넘으면 IndexSizeError
     */
    public void replaceData(int offset, int count, String text) {
        checkOffset(offset);
        int end = (int) Math.min((long) offset + Math.max(count, 0), data.length());
        String insert = text == null ? "" : text;
        String old = data;
        data = old.substring(0, offset) + insert + old.substring(end);
        // 살아 있는 범위의 경계점을 먼저 조정한 뒤 변경을 알린다.
        doc.liveDataReplaced(this, offset, end - offset, insert.length());
        doc.recordCharacterData(this, old);
    }

    /**
     * Throws IndexSizeError if an offset is outside the data.
     * <p>
     * 오프셋이 데이터 밖이면 IndexSizeError 를 던진다.
     *
     * @param offset offset to check / 검사할 오프셋
     */
    private void checkOffset(int offset) {
        if (offset < 0 || offset > data.length()) {
            throw DomException.indexSize("offset " + offset + " is outside the data (length " + data.length() + ")");
        }
    }

    /**
     * Returns the data as node value.
     * <p>
     * 데이터를 노드 값으로 반환한다.
     *
     * @return data / 데이터
     */
    public String getNodeValue() {
        return data;
    }

    /**
     * Sets the data as node value.
     * <p>
     * 노드 값으로 데이터를 지정한다.
     *
     * @param value new data / 새 데이터
     */
    public void setNodeValue(String value) {
        setData(value);
    }

    /**
     * Compares the data with another character node of the same type.
     * <p>
     * 같은 종류의 다른 문자 노드와 데이터를 비교한다.
     *
     * @param other node of the same type / 같은 종류의 노드
     * @return true if the data are equal / 데이터가 같으면 true
     */
    boolean equalOwnData(Node other) {
        return data.equals(((CharacterData) other).data);
    }
}
