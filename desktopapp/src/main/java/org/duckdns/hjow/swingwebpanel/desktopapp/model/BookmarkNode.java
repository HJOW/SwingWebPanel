package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry of the bookmarks (plan item P31-06): a bookmark with a name and an address, or a folder with a name and entries inside it.
 * An entry is created with {@link #bookmark(String, String)} or {@link #folder(String)} and then put into a {@link BookmarkModel};
 * after that only the model changes it, so that every change is announced to the listeners of the model.
 * <p>
 * 북마크의 항목 하나(계획 P31-06): 이름과 주소가 있는 북마크, 또는 이름과 그 안의 항목들이 있는 폴더. 항목은 {@link #bookmark(String, String)} 이나
 * {@link #folder(String)} 로 만든 뒤 {@link BookmarkModel} 에 넣는다. 그 뒤에는 모델만 항목을 바꾸므로 모든 변경이 모델의 리스너에 알려진다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * BookmarkNode news = BookmarkNode.folder("News");
 * model.add(model.getRoot(), news);
 * model.add(news, BookmarkNode.bookmark("Example", "https://example.com/"));
 * </pre>
 */
public final class BookmarkNode {

    /** True for a folder, false for a bookmark. / 폴더이면 true, 북마크이면 false. */
    private final boolean folder;

    /** The name that is shown. / 보이는 이름. */
    private String name;

    /** The address of a bookmark; the empty string for a folder. / 북마크의 주소. 폴더이면 빈 문자열. */
    private String url;

    /** The folder that holds this entry, or null. / 이 항목을 담은 폴더. 없으면 null. */
    private BookmarkNode parent;

    /** The entries of a folder in their order; empty for a bookmark. / 폴더의 항목(순서대로). 북마크이면 비어 있다. */
    private final List<BookmarkNode> children = new ArrayList<BookmarkNode>();

    /**
     * Creates an entry.
     * <p>
     * 항목을 만든다.
     *
     * @param folder true for a folder / 폴더이면 true
     * @param name   the name / 이름
     * @param url    the address / 주소
     */
    private BookmarkNode(boolean folder, String name, String url) {
        this.folder = folder;
        this.name = name == null ? "" : name;
        this.url = url == null ? "" : url;
    }

    /**
     * Creates a bookmark that is not in a model yet.
     * <p>
     * 아직 모델에 없는 북마크를 만든다.
     *
     * @param name the name; null gives the empty name / 이름. null 이면 빈 이름
     * @param url  the address; null gives the empty address / 주소. null 이면 빈 주소
     * @return the bookmark / 북마크
     */
    public static BookmarkNode bookmark(String name, String url) {
        return new BookmarkNode(false, name, url);
    }

    /**
     * Creates an empty folder that is not in a model yet.
     * <p>
     * 아직 모델에 없는 빈 폴더를 만든다.
     *
     * @param name the name; null gives the empty name / 이름. null 이면 빈 이름
     * @return the folder / 폴더
     */
    public static BookmarkNode folder(String name) {
        return new BookmarkNode(true, name, "");
    }

    /**
     * Tells whether the entry is a folder.
     * <p>
     * 항목이 폴더인지 알려 준다.
     *
     * @return true for a folder / 폴더이면 true
     */
    public boolean isFolder() {
        return folder;
    }

    /**
     * Returns the name of the entry.
     * <p>
     * 항목의 이름을 반환한다.
     *
     * @return the name; never null / 이름. null 이 아니다
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the address of a bookmark.
     * <p>
     * 북마크의 주소를 반환한다.
     *
     * @return the address; the empty string for a folder / 주소. 폴더이면 빈 문자열
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the folder that holds the entry.
     * <p>
     * 항목을 담은 폴더를 반환한다.
     *
     * @return the folder, or null for the root and for an entry outside a model / 폴더. 루트이거나 모델 밖의 항목이면 null
     */
    public BookmarkNode getParent() {
        return parent;
    }

    /**
     * Returns the number of entries of a folder.
     * <p>
     * 폴더의 항목 수를 반환한다.
     *
     * @return the count; 0 for a bookmark / 개수. 북마크이면 0
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Returns an entry of a folder.
     * <p>
     * 폴더의 항목 하나를 반환한다.
     *
     * @param index the position / 위치
     * @return the entry / 항목
     */
    public BookmarkNode getChild(int index) {
        return children.get(index);
    }

    /**
     * Returns the entries of a folder.
     * <p>
     * 폴더의 항목들을 반환한다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    public List<BookmarkNode> getChildren() {
        return new ArrayList<BookmarkNode>(children);
    }

    /**
     * Returns the position of an entry in this folder.
     * <p>
     * 이 폴더에서 항목의 위치를 반환한다.
     *
     * @param child the entry / 항목
     * @return the position, or -1 if it is not in this folder / 위치. 이 폴더에 없으면 -1
     */
    public int indexOf(BookmarkNode child) {
        // 같은 객체인지로 찾는다(이름과 주소가 같은 항목이 여럿일 수 있다).
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == child) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tells whether this entry is the given folder or lies inside it at any depth.
     * <p>
     * 이 항목이 주어진 폴더 자신이거나 그 안(깊이와 무관)에 있는지 알려 준다.
     *
     * @param ancestor the folder / 폴더
     * @return true if so / 그러면 true
     */
    public boolean isInside(BookmarkNode ancestor) {
        // 루트 쪽으로 올라가며 찾는다.
        for (BookmarkNode n = this; n != null; n = n.parent) {
            if (n == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the name and the address; used by the model.
     * <p>
     * 이름과 주소를 정한다. 모델이 쓴다.
     *
     * @param newName the name / 이름
     * @param newUrl  the address (ignored for a folder) / 주소(폴더이면 무시한다)
     */
    void set(String newName, String newUrl) {
        name = newName == null ? "" : newName;
        if (!folder) {
            url = newUrl == null ? "" : newUrl;
        }
    }

    /**
     * Puts an entry into this folder; used by the model.
     * <p>
     * 이 폴더에 항목을 넣는다. 모델이 쓴다.
     *
     * @param child the entry / 항목
     * @param index the position, cut to the range of the folder / 위치. 폴더의 범위로 자른다
     */
    void insert(BookmarkNode child, int index) {
        int at = Math.max(0, Math.min(index, children.size()));
        children.add(at, child);
        child.parent = this;
    }

    /**
     * Takes an entry out of this folder; used by the model.
     * <p>
     * 이 폴더에서 항목을 뺀다. 모델이 쓴다.
     *
     * @param child the entry / 항목
     * @return true if it was in this folder / 이 폴더에 있었으면 true
     */
    boolean detach(BookmarkNode child) {
        int at = indexOf(child);
        if (at < 0) {
            return false;
        }
        children.remove(at);
        child.parent = null;
        return true;
    }

    /**
     * Removes all entries of this folder; used by the model.
     * <p>
     * 이 폴더의 항목을 모두 뺀다. 모델이 쓴다.
     */
    void clear() {
        // 항목마다 부모를 끊는다.
        for (int i = 0; i < children.size(); i++) {
            children.get(i).parent = null;
        }
        children.clear();
    }

    /**
     * Returns the name, so that a tree shows the entry by its name.
     * <p>
     * 이름을 반환한다. 트리가 항목을 그 이름으로 보이게 한다.
     *
     * @return the name, or the address of a bookmark without a name / 이름. 이름 없는 북마크이면 주소
     */
    public String toString() {
        return name.length() == 0 && !folder ? url : name;
    }
}
