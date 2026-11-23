package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * The data of a clipboard operation or of a drag and drop (HTML <code>DataTransfer</code>): the clipboard part of it
 * (<code>ClipboardEvent.clipboardData</code>, <code>InputEvent.dataTransfer</code>) and the drag-and-drop part (<code>DragEvent.dataTransfer</code>,
 * <code>effectAllowed</code>, <code>dropEffect</code>, <code>items</code>). It holds the drag data store: a list of text items, one per type string
 * (lower case, <code>text</code> means <code>text/plain</code> and <code>url</code> means <code>text/uri-list</code>, the legacy names of the
 * specification). For a <code>copy</code> event it starts with what the user selected (<code>text/plain</code> and <code>text/html</code>); a script that
 * calls <code>setData</code> and cancels the event puts its own data on the clipboard instead. This component has text items only, no files.
 * <p>
 * The <em>mode</em> says what a script may do (HTML Standard, drag data store mode): read/write (the clipboard data of <code>copy</code> and
 * <code>cut</code>, and <code>dragstart</code>: data can be read and added), read-only (<code>paste</code> and <code>drop</code>: data can be read, nothing can be
 * added) and protected (the other drag events: the types can be listed, the data cannot be read). In the disabled mode (the data transfer is
 * no longer tied to a drag operation) nothing is visible. The component itself fills and reads the data through {@link #put} and
 * {@link #peek}, which ignore the mode.
 * <p>
 * 클립보드 작업이나 끌어다 놓기의 데이터(HTML <code>DataTransfer</code>): 클립보드 부분(<code>ClipboardEvent.clipboardData</code>, <code>InputEvent.dataTransfer</code>)과 끌어다 놓기
 * 부분(<code>DragEvent.dataTransfer</code>, <code>effectAllowed</code>, <code>dropEffect</code>, <code>items</code>). 드래그 데이터 저장소를 가진다: 타입 문자열(소문자.
 * <code>text</code> 는 <code>text/plain</code>, <code>url</code> 은 <code>text/uri-list</code> 를 뜻하며 규격의 옛 이름이다)마다 하나인 텍스트 항목의 목록. <code>copy</code>
 * 이벤트에서는 사용자가 선택한 것(<code>text/plain</code> 과 <code>text/html</code>)으로 시작하며, <code>setData</code> 를 부르고 이벤트를 취소한 스크립트는 대신 자기
 * 데이터를 클립보드에 넣는다. 이 컴포넌트는 텍스트 항목만 가지며 파일은 없다.
 * <p>
 * <em>모드</em>는 스크립트가 할 수 있는 일을 정한다(HTML 표준의 드래그 데이터 저장소 모드): 읽기/쓰기(<code>copy</code>, <code>cut</code> 의 클립보드 데이터와
 * <code>dragstart</code>: 데이터를 읽고 더할 수 있다), 읽기 전용(<code>paste</code>, <code>drop</code>: 읽을 수 있고 더할 수 없다), 보호(나머지 끌기 이벤트: 종류는 나열할 수
 * 있고 데이터는 읽을 수 없다). 무효화 모드(데이터 전달 객체가 더는 끌기 작업에 묶여 있지 않다)에서는 아무것도 보이지 않는다. 컴포넌트 자신은 모드를 무시하는
 * {@link #put} 과 {@link #peek} 로 데이터를 채우고 읽는다.
 */
public final class ClipboardData {

    /**
     * The read/write mode: data can be read and added.
     * <p>
     * 읽기/쓰기 모드: 데이터를 읽고 더할 수 있다.
     */
    public static final int READ_WRITE = 0;

    /**
     * The read-only mode: data can be read, nothing can be added or removed.
     * <p>
     * 읽기 전용 모드: 데이터를 읽을 수 있고 더하거나 지울 수 없다.
     */
    public static final int READ_ONLY = 1;

    /**
     * The protected mode: the types can be listed, the data cannot be read.
     * <p>
     * 보호 모드: 종류는 나열할 수 있고 데이터는 읽을 수 없다.
     */
    public static final int PROTECTED = 2;

    /**
     * The disabled mode: the data transfer is no longer tied to an operation, nothing is visible.
     * <p>
     * 무효화 모드: 데이터 전달 객체가 더는 작업에 묶여 있지 않으며 아무것도 보이지 않는다.
     */
    public static final int DISABLED = 3;

    /**
     * The values that <code>effectAllowed</code> may be set to.
     * <p>
     * <code>effectAllowed</code> 에 정할 수 있는 값.
     */
    private static final String[] EFFECTS_ALLOWED = { "none", "copy", "copyLink", "copyMove", "link", "linkMove", "move", "all", "uninitialized" };

    /**
     * The values that <code>dropEffect</code> may be set to.
     * <p>
     * <code>dropEffect</code> 에 정할 수 있는 값.
     */
    private static final String[] DROP_EFFECTS = { "none", "copy", "link", "move" };

    /**
     * The items in the order they were added.
     * <p>
     * 더한 순서의 항목.
     */
    private final List<DataTransferItem> items = new ArrayList<DataTransferItem>();

    /**
     * The view of the items that scripts use (made on demand, always the same object).
     * <p>
     * 스크립트가 쓰는 항목의 뷰(필요할 때 만들며 항상 같은 객체).
     */
    private DataTransferItemList itemList;

    /**
     * The empty list of files (made on demand, always the same object).
     * <p>
     * 파일의 빈 목록(필요할 때 만들며 항상 같은 객체).
     */
    private org.duckdns.hjow.swingwebpanel.dom.html.FileList fileList;

    /**
     * True if a script changed the data (<code>setData</code> or <code>clearData</code>) after the component filled it.
     * <p>
     * 컴포넌트가 채운 뒤에 스크립트가 데이터를 바꾸었으면(<code>setData</code>, <code>clearData</code>) true.
     */
    private boolean changed;

    /**
     * What a script may do with the data (one of the mode constants).
     * <p>
     * 스크립트가 데이터로 할 수 있는 일(모드 상수 중 하나).
     */
    private int mode = READ_WRITE;

    /**
     * The value of <code>effectAllowed</code>.
     * <p>
     * <code>effectAllowed</code> 의 값.
     */
    private String effectAllowed = "none";

    /**
     * The value of <code>dropEffect</code>.
     * <p>
     * <code>dropEffect</code> 의 값.
     */
    private String dropEffect = "none";

    /**
     * The element that <code>setDragImage</code> chose as the picture of the drag, or null.
     * <p>
     * <code>setDragImage</code> 가 끌기의 그림으로 고른 요소. 없으면 null.
     */
    private Element dragImage;

    /**
     * The x of the hot spot that <code>setDragImage</code> gave.
     * <p>
     * <code>setDragImage</code> 가 준 핫 스팟의 x.
     */
    private int dragImageX;

    /**
     * The y of the hot spot that <code>setDragImage</code> gave.
     * <p>
     * <code>setDragImage</code> 가 준 핫 스팟의 y.
     */
    private int dragImageY;

    /**
     * Creates empty data in the read/write mode (<code>new DataTransfer()</code>: both effects are <code>none</code>).
     * <p>
     * 읽기/쓰기 모드의 빈 데이터를 만든다(<code>new DataTransfer()</code>: 두 효과는 <code>none</code>).
     */
    public ClipboardData() {
    }

    /**
     * Returns the canonical type of a format name: lower case, with the legacy names mapped.
     * <p>
     * 형식 이름의 정규 종류를 반환한다: 소문자이며 옛 이름은 바꾼다.
     *
     * @param format the format name / 형식 이름
     * @return the type / 종류
     */
    public static String canonicalType(String format) {
        String f = format == null ? "" : format.trim().toLowerCase(java.util.Locale.ENGLISH);
        if ("text".equals(f)) {
            return "text/plain";
        }
        return "url".equals(f) ? "text/uri-list" : f;
    }

    /**
     * Returns the canonical type of a format name (see {@link #canonicalType}).
     * <p>
     * 형식 이름의 정규 종류를 반환한다({@link #canonicalType} 참조).
     *
     * @param format the format name / 형식 이름
     * @return the type / 종류
     */
    static String canonical(String format) {
        return canonicalType(format);
    }

    /**
     * Returns the index of the item of a type, or -1.
     * <p>
     * 종류의 항목 위치를 반환한다. 없으면 -1.
     *
     * @param type the canonical type / 정규 종류
     * @return the index or -1 / 위치. 없으면 -1
     */
    private int indexOf(String type) {
        // 항목을 차례로 훑어 종류가 같은 것을 찾는다.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).rawType().equals(type)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Puts the data of a type (replacing earlier data of that type) without counting it as a change by a script and whatever the mode is;
     * the component fills the data of an event with it before the event is dispatched.
     * <p>
     * 한 종류의 데이터를 넣는다(그 종류의 이전 데이터를 대체). 스크립트의 변경으로 치지 않으며 모드와 무관하다. 컴포넌트가 이벤트를 전달하기 전에 이것으로 데이터를 채운다.
     *
     * @param type the type / 종류
     * @param data the string / 문자열
     */
    public void put(String type, String data) {
        String t = canonicalType(type);
        String d = data == null ? "" : data;
        int i = indexOf(t);
        // 같은 종류의 항목이 있으면 데이터만 바꾸어 항목의 동일성을 유지하고, 없으면 새 항목을 끝에 더한다.
        if (i >= 0) {
            items.get(i).replaceData(d);
        } else {
            items.add(new DataTransferItem(this, t, d));
        }
    }

    /**
     * Returns the data of a type whatever the mode is (for the component itself); an empty string if there is none.
     * <p>
     * 모드와 무관하게 한 종류의 데이터를 반환한다(컴포넌트 자신용). 없으면 빈 문자열.
     *
     * @param type the type / 종류
     * @return the string / 문자열
     */
    public String peek(String type) {
        int i = indexOf(canonicalType(type));
        return i < 0 ? "" : items.get(i).rawData();
    }

    /**
     * Sets the data of a type as a script does (<code>setData</code>), which counts as a change; nothing happens outside the read/write mode.
     * <p>
     * 스크립트가 하듯 한 종류의 데이터를 지정한다(<code>setData</code>). 변경으로 친다. 읽기/쓰기 모드가 아니면 아무 일도 없다.
     *
     * @param format the format name / 형식 이름
     * @param data   the string / 문자열
     */
    public void setData(String format, String data) {
        if (mode != READ_WRITE) {
            return;
        }
        changed = true;
        put(format, data);
    }

    /**
     * Returns the data of a type (<code>getData</code>): an empty string if there is none or if the data may not be read (the protected and
     * disabled modes). <code>url</code> gives the first address of the <code>text/uri-list</code> data.
     * <p>
     * 한 종류의 데이터를 반환한다(<code>getData</code>). 없거나 읽을 수 없으면(보호, 무효화 모드) 빈 문자열. <code>url</code> 은 <code>text/uri-list</code> 데이터의 첫 주소이다.
     *
     * @param format the format name / 형식 이름
     * @return the string, empty if there is none / 문자열. 없으면 빈 문자열
     */
    public String getData(String format) {
        if (mode == PROTECTED || mode == DISABLED) {
            return "";
        }
        String f = format == null ? "" : format.trim().toLowerCase(java.util.Locale.ENGLISH);
        String v = peek(f);
        // url 형식은 text/uri-list 의 첫 주소를 돌려준다.
        if ("url".equals(f)) {
            String[] lines = v.split("\r?\n");
            // 줄을 차례로 보며 주석 줄과 빈 줄은 건너뛴다.
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    return line;
                }
            }
            return "";
        }
        return v;
    }

    /**
     * Tells whether a type has data.
     * <p>
     * 한 종류에 데이터가 있는지 알려 준다.
     *
     * @param format the format name / 형식 이름
     * @return true if there is data / 있으면 true
     */
    public boolean hasData(String format) {
        return indexOf(canonicalType(format)) >= 0;
    }

    /**
     * Removes the data of a type (<code>clearData(format)</code>), or all data if the format is null; nothing happens outside the
     * read/write mode.
     * <p>
     * 한 종류의 데이터를 지운다(<code>clearData(format)</code>). 형식이 null 이면 모든 데이터를 지운다. 읽기/쓰기 모드가 아니면 아무 일도 없다.
     *
     * @param format the format name or null / 형식 이름. 없으면 null
     */
    public void clearData(String format) {
        if (mode != READ_WRITE) {
            return;
        }
        changed = true;
        if (format == null) {
            clearAllItems();
        } else {
            int i = indexOf(canonicalType(format));
            if (i >= 0) {
                removeItemAt(i);
            }
        }
    }

    /**
     * Returns the types that have data (<code>types</code>), in the order they were added (empty in the disabled mode).
     * <p>
     * 데이터가 있는 종류를 반환한다(<code>types</code>). 더한 순서이다(무효화 모드에서는 비어 있다).
     *
     * @return a new list of the types / 종류의 새 목록
     */
    public List<String> getTypes() {
        List<String> types = new ArrayList<String>();
        // 무효화 모드가 아니면 종류를 더한 순서대로 목록에 옮긴다.
        for (int i = 0; mode != DISABLED && i < items.size(); i++) {
            types.add(items.get(i).rawType());
        }
        return types;
    }

    /**
     * Tells whether a script changed the data after the component filled it.
     * <p>
     * 컴포넌트가 채운 뒤에 스크립트가 데이터를 바꾸었는지 알려 준다.
     *
     * @return true if changed / 바뀌었으면 true
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * Returns what a script may do with the data (one of the mode constants).
     * <p>
     * 스크립트가 데이터로 할 수 있는 일을 반환한다(모드 상수 중 하나).
     *
     * @return the mode / 모드
     */
    public int getMode() {
        return mode;
    }

    /**
     * Sets what a script may do with the data (the component sets the mode of a drag data store for every event).
     * <p>
     * 스크립트가 데이터로 할 수 있는 일을 정한다(컴포넌트가 이벤트마다 드래그 데이터 저장소의 모드를 정한다).
     *
     * @param newMode one of the mode constants / 모드 상수 중 하나
     */
    public void setMode(int newMode) {
        this.mode = newMode;
    }

    /**
     * Returns the value of <code>effectAllowed</code>.
     * <p>
     * <code>effectAllowed</code> 의 값을 반환한다.
     *
     * @return the value / 값
     */
    public String getEffectAllowed() {
        return effectAllowed;
    }

    /**
     * Sets <code>effectAllowed</code> as a script does: only in the read/write mode and only to one of its values (<code>none</code>,
     * <code>copy</code>, <code>copyLink</code>, <code>copyMove</code>, <code>link</code>, <code>linkMove</code>, <code>move</code>, <code>all</code>,
     * <code>uninitialized</code>); anything else is ignored.
     * <p>
     * 스크립트가 하듯 <code>effectAllowed</code> 를 정한다: 읽기/쓰기 모드에서 값(<code>none</code>, <code>copy</code>, <code>copyLink</code>, <code>copyMove</code>,
     * <code>link</code>, <code>linkMove</code>, <code>move</code>, <code>all</code>, <code>uninitialized</code>) 중 하나일 때만. 나머지는 무시한다.
     *
     * @param value the new value / 새 값
     */
    public void setEffectAllowed(String value) {
        if (mode == READ_WRITE && contains(EFFECTS_ALLOWED, value)) {
            effectAllowed = value;
        }
    }

    /**
     * Sets <code>effectAllowed</code> without the checks of a script (for the component).
     * <p>
     * 스크립트의 검사 없이 <code>effectAllowed</code> 를 정한다(컴포넌트용).
     *
     * @param value the value / 값
     */
    public void forceEffectAllowed(String value) {
        effectAllowed = value;
    }

    /**
     * Returns the value of <code>dropEffect</code>.
     * <p>
     * <code>dropEffect</code> 의 값을 반환한다.
     *
     * @return the value / 값
     */
    public String getDropEffect() {
        return dropEffect;
    }

    /**
     * Sets <code>dropEffect</code> as a script does: only to <code>none</code>, <code>copy</code>, <code>link</code> or <code>move</code>; other values
     * are ignored.
     * <p>
     * 스크립트가 하듯 <code>dropEffect</code> 를 정한다: <code>none</code>, <code>copy</code>, <code>link</code>, <code>move</code> 중 하나로만. 나머지는 무시한다.
     *
     * @param value the new value / 새 값
     */
    public void setDropEffect(String value) {
        if (contains(DROP_EFFECTS, value)) {
            dropEffect = value;
        }
    }

    /**
     * Tells whether an array holds a text.
     * <p>
     * 배열에 텍스트가 있는지 알려 준다.
     *
     * @param values the array / 배열
     * @param value  the text / 텍스트
     * @return true if it holds it / 있으면 true
     */
    private static boolean contains(String[] values, String value) {
        // 값을 하나씩 비교한다.
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the files of the data transfer (<code>files</code>): this component has text items only, so the list is always empty; it is
     * always the same object.
     * <p>
     * 데이터 전달 객체의 파일을 반환한다(<code>files</code>): 이 컴포넌트는 텍스트 항목만 가지므로 목록은 항상 비어 있다. 항상 같은 객체이다.
     *
     * @return the empty list / 빈 목록
     */
    public org.duckdns.hjow.swingwebpanel.dom.html.FileList getFiles() {
        if (fileList == null) {
            fileList = org.duckdns.hjow.swingwebpanel.dom.html.FileList.empty();
        }
        return fileList;
    }

    /**
     * Returns a new data transfer with the same items (copies of them), effects and drag image, in the read/write mode and unchanged: the
     * component makes one for every drag event, on top of the drag data store of the operation.
     * <p>
     * 같은 항목(의 복사본), 효과, 끌기 그림을 가진 새 데이터 전달 객체를 반환한다. 읽기/쓰기 모드이고 바뀌지 않은 상태이다. 컴포넌트가 끌기 이벤트마다
     * 작업의 드래그 데이터 저장소 위에 하나씩 만든다.
     *
     * @return the copy / 복사본
     */
    public ClipboardData duplicate() {
        ClipboardData copy = new ClipboardData();
        // 항목을 순서대로 복사한다.
        for (int i = 0; i < items.size(); i++) {
            copy.put(items.get(i).rawType(), items.get(i).rawData());
        }
        copy.effectAllowed = effectAllowed;
        copy.dropEffect = dropEffect;
        copy.dragImage = dragImage;
        copy.dragImageX = dragImageX;
        copy.dragImageY = dragImageY;
        return copy;
    }

    /**
     * Returns the view of the items for scripts (<code>items</code>); it is always the same object.
     * <p>
     * 스크립트가 쓰는 항목의 뷰를 반환한다(<code>items</code>). 항상 같은 객체이다.
     *
     * @return the item list / 항목 목록
     */
    public DataTransferItemList getItems() {
        if (itemList == null) {
            itemList = new DataTransferItemList(this);
        }
        return itemList;
    }

    /**
     * Returns the number of items.
     * <p>
     * 항목 수를 반환한다.
     *
     * @return the number / 수
     */
    int itemCount() {
        return items.size();
    }

    /**
     * Returns the item at an index.
     * <p>
     * 위치의 항목을 반환한다.
     *
     * @param index the index / 위치
     * @return the item / 항목
     */
    DataTransferItem itemAt(int index) {
        return items.get(index);
    }

    /**
     * Returns the item of a type, or null.
     * <p>
     * 종류의 항목을 반환한다. 없으면 null.
     *
     * @param type the canonical type / 정규 종류
     * @return the item or null / 항목. 없으면 null
     */
    DataTransferItem itemFor(String type) {
        int i = indexOf(type);
        return i < 0 ? null : items.get(i);
    }

    /**
     * Removes the item at an index (nothing happens for a wrong index) and detaches it.
     * <p>
     * 위치의 항목을 제거하고(잘못된 위치이면 아무 일도 없다) 뗀다.
     *
     * @param index the index / 위치
     */
    void removeItemAt(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index).detach();
        }
    }

    /**
     * Removes all items and detaches them.
     * <p>
     * 모든 항목을 제거하고 뗀다.
     */
    void clearAllItems() {
        // 항목을 모두 떼어 낸 뒤 목록을 비운다.
        for (int i = 0; i < items.size(); i++) {
            items.get(i).detach();
        }
        items.clear();
    }

    /**
     * Sets the picture of the drag (<code>setDragImage</code>): nothing happens outside the read/write mode.
     * <p>
     * 끌기의 그림을 정한다(<code>setDragImage</code>). 읽기/쓰기 모드가 아니면 아무 일도 없다.
     *
     * @param image the element that gives the picture / 그림을 주는 요소
     * @param x     the x of the hot spot / 핫 스팟의 x
     * @param y     the y of the hot spot / 핫 스팟의 y
     */
    public void setDragImage(Element image, int x, int y) {
        if (mode == READ_WRITE) {
            dragImage = image;
            dragImageX = x;
            dragImageY = y;
        }
    }

    /**
     * Returns the element that <code>setDragImage</code> chose as the picture of the drag, or null.
     * <p>
     * <code>setDragImage</code> 가 끌기의 그림으로 고른 요소를 반환한다. 없으면 null.
     *
     * @return the element or null / 요소. 없으면 null
     */
    public Element getDragImage() {
        return dragImage;
    }

    /**
     * Returns the x of the hot spot of the drag picture.
     * <p>
     * 끌기 그림의 핫 스팟 x 를 반환한다.
     *
     * @return x / x
     */
    public int getDragImageX() {
        return dragImageX;
    }

    /**
     * Returns the y of the hot spot of the drag picture.
     * <p>
     * 끌기 그림의 핫 스팟 y 를 반환한다.
     *
     * @return y / y
     */
    public int getDragImageY() {
        return dragImageY;
    }

    /**
     * Ends the association with the drag operation (the disabled mode): after the event the data transfer shows nothing any more.
     * <p>
     * 끌기 작업과의 연결을 끊는다(무효화 모드): 이벤트가 끝나면 데이터 전달 객체는 더는 아무것도 보이지 않는다.
     */
    public void disable() {
        mode = DISABLED;
    }
}
