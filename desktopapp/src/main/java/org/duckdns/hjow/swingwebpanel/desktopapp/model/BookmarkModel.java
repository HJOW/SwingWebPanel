package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The bookmarks of the browser application (plan item P31-06): a tree of folders and bookmarks under one root folder. One model exists
 * per process and all windows share it, so a change made in one window is seen in the others at once; every change is announced to the
 * listeners. Use it on the event dispatch thread.
 * <p>
 * 브라우저 응용 프로그램의 북마크(계획 P31-06): 루트 폴더 하나 아래의 폴더와 북마크의 트리. 프로세스에 모델이 하나 있고 모든 창이 공유하므로, 한
 * 창에서 한 변경이 다른 창에 바로 보인다. 모든 변경은 리스너에 알린다. 이벤트 디스패치 스레드에서 쓴다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * BookmarkModel model = new BookmarkModel();
 * model.add(model.getRoot(), BookmarkNode.bookmark("Example", "https://example.com/"));
 * boolean known = model.contains("https://example.com/");
 * </pre>
 */
public final class BookmarkModel {

    /**
     * Receives the changes of the bookmarks.
     * <p>
     * 북마크의 변경을 받는다.
     */
    public interface Listener {

        /**
         * Called after the bookmarks changed.
         * <p>
         * 북마크가 바뀐 뒤에 호출된다.
         *
         * @param model the bookmarks / 북마크
         */
        void bookmarksChanged(BookmarkModel model);
    }

    /** The root folder; it has no name and is not shown itself. / 루트 폴더. 이름이 없고 자신은 보이지 않는다. */
    private final BookmarkNode root = BookmarkNode.folder("");

    /** The listeners. / 리스너. */
    private final List<Listener> listeners = new ArrayList<Listener>();

    /**
     * Creates empty bookmarks.
     * <p>
     * 빈 북마크를 만든다.
     */
    public BookmarkModel() {
    }

    /**
     * Returns the root folder.
     * <p>
     * 루트 폴더를 반환한다.
     *
     * @return the root / 루트
     */
    public BookmarkNode getRoot() {
        return root;
    }

    /**
     * Adds an entry at the end of a folder.
     * <p>
     * 폴더 끝에 항목을 더한다.
     *
     * @param parent the folder / 폴더
     * @param node   an entry that is not in a model / 모델에 없는 항목
     * @throws IllegalArgumentException if the parent is not a folder of this model or the entry is already in a folder / 부모가 이 모델의
     *                                  폴더가 아니거나 항목이 이미 폴더에 있으면
     */
    public void add(BookmarkNode parent, BookmarkNode node) {
        add(parent, node, parent == null ? 0 : parent.getChildCount());
    }

    /**
     * Adds an entry at a position of a folder.
     * <p>
     * 폴더의 한 위치에 항목을 더한다.
     *
     * @param parent the folder / 폴더
     * @param node   an entry that is not in a model / 모델에 없는 항목
     * @param index  the position, cut to the range of the folder / 위치. 폴더의 범위로 자른다
     * @throws IllegalArgumentException if the parent is not a folder of this model or the entry is already in a folder / 부모가 이 모델의
     *                                  폴더가 아니거나 항목이 이미 폴더에 있으면
     */
    public void add(BookmarkNode parent, BookmarkNode node, int index) {
        requireFolder(parent);
        if (node == null || node.getParent() != null || node == root) {
            throw new IllegalArgumentException("the entry must not be in a folder yet");
        }
        parent.insert(node, index);
        fireChanged();
    }

    /**
     * Removes an entry (a folder with everything in it).
     * <p>
     * 항목을 지운다(폴더이면 그 안의 모든 것과 함께).
     *
     * @param node an entry of this model other than the root / 루트가 아닌 이 모델의 항목
     * @return true if it was removed / 지웠으면 true
     */
    public boolean remove(BookmarkNode node) {
        if (node == null || node == root || node.getParent() == null || !node.isInside(root)) {
            return false;
        }
        node.getParent().detach(node);
        fireChanged();
        return true;
    }

    /**
     * Changes the name and the address of an entry.
     * <p>
     * 항목의 이름과 주소를 바꾼다.
     *
     * @param node an entry of this model / 이 모델의 항목
     * @param name the new name / 새 이름
     * @param url  the new address (ignored for a folder) / 새 주소(폴더이면 무시한다)
     */
    public void edit(BookmarkNode node, String name, String url) {
        if (node == null || node == root || !node.isInside(root)) {
            return;
        }
        String newName = name == null ? "" : name;
        String newUrl = node.isFolder() ? "" : (url == null ? "" : url);
        // 달라진 것이 없으면 알리지 않는다.
        if (newName.equals(node.getName()) && newUrl.equals(node.getUrl())) {
            return;
        }
        node.set(newName, newUrl);
        fireChanged();
    }

    /**
     * Moves an entry to a position of a folder. A folder cannot be moved into itself or into a folder inside it.
     * <p>
     * 항목을 폴더의 한 위치로 옮긴다. 폴더를 자기 자신이나 자기 안의 폴더로 옮길 수는 없다.
     *
     * @param node      an entry of this model other than the root / 루트가 아닌 이 모델의 항목
     * @param newParent the folder to move into / 옮겨 갈 폴더
     * @param index     the position in that folder as it is before the move, cut to its range / 옮기기 전 기준으로 그 폴더에서의 위치.
     *                  범위로 자른다
     * @return true if the entry was moved / 옮겼으면 true
     */
    public boolean move(BookmarkNode node, BookmarkNode newParent, int index) {
        if (node == null || node == root || node.getParent() == null || !node.isInside(root)) {
            return false;
        }
        if (newParent == null || !newParent.isFolder() || !newParent.isInside(root) || newParent.isInside(node)) {
            return false;
        }
        BookmarkNode oldParent = node.getParent();
        int from = oldParent.indexOf(node);
        int to = Math.max(0, Math.min(index, newParent.getChildCount()));
        // 같은 폴더 안에서 뒤로 옮기면, 자신을 뺀 뒤의 위치는 하나 앞이다.
        if (oldParent == newParent && to > from) {
            to--;
        }
        if (oldParent == newParent && to == from) {
            return false;
        }
        oldParent.detach(node);
        newParent.insert(node, to);
        fireChanged();
        return true;
    }

    /**
     * Moves an entry up or down inside its folder.
     * <p>
     * 항목을 그 폴더 안에서 위나 아래로 옮긴다.
     *
     * @param node  an entry of this model other than the root / 루트가 아닌 이 모델의 항목
     * @param delta -1 for one place up, 1 for one place down / 한 칸 위이면 -1, 한 칸 아래이면 1
     * @return true if the entry was moved / 옮겼으면 true
     */
    public boolean shift(BookmarkNode node, int delta) {
        if (node == null || node.getParent() == null || delta == 0) {
            return false;
        }
        BookmarkNode parent = node.getParent();
        int from = parent.indexOf(node);
        int to = from + (delta < 0 ? -1 : 1);
        if (to < 0 || to >= parent.getChildCount()) {
            return false;
        }
        // move 는 옮기기 전의 위치를 받으므로 아래로 갈 때는 한 칸 더 뒤를 가리킨다.
        return move(node, parent, delta < 0 ? to : to + 1);
    }

    /**
     * Replaces all bookmarks with the entries of another root folder; the entries are moved, not copied.
     * <p>
     * 모든 북마크를 다른 루트 폴더의 항목으로 바꾼다. 항목은 복사하지 않고 옮긴다.
     *
     * @param source a folder outside this model whose entries are taken / 항목을 가져올, 이 모델 밖의 폴더
     */
    public void replaceAll(BookmarkNode source) {
        root.clear();
        append(root, source, false);
        fireChanged();
    }

    /**
     * Appends the entries of a folder outside the model at the end of a folder of the model; the entries are moved, not copied.
     * <p>
     * 모델 밖 폴더의 항목을 모델의 폴더 끝에 더한다. 항목은 복사하지 않고 옮긴다.
     *
     * @param parent the folder of this model that receives the entries / 항목을 받을 이 모델의 폴더
     * @param source a folder outside this model whose entries are taken / 항목을 가져올, 이 모델 밖의 폴더
     * @throws IllegalArgumentException if the parent is not a folder of this model / 부모가 이 모델의 폴더가 아니면
     */
    public void addAll(BookmarkNode parent, BookmarkNode source) {
        requireFolder(parent);
        append(parent, source, true);
    }

    /**
     * Moves the entries of a folder outside the model to the end of a folder of the model.
     * <p>
     * 모델 밖 폴더의 항목을 모델의 폴더 끝으로 옮긴다.
     *
     * @param parent the folder that receives the entries / 항목을 받을 폴더
     * @param source the folder that gives them / 항목을 주는 폴더
     * @param fire   true to tell the listeners / 리스너에 알리려면 true
     */
    private void append(BookmarkNode parent, BookmarkNode source, boolean fire) {
        if (source == null || source == root || source.isInside(root)) {
            return;
        }
        List<BookmarkNode> taken = source.getChildren();
        // 항목마다 원래 폴더에서 떼어 받는 폴더에 붙인다.
        for (int i = 0; i < taken.size(); i++) {
            source.detach(taken.get(i));
            parent.insert(taken.get(i), parent.getChildCount());
        }
        if (fire && !taken.isEmpty()) {
            fireChanged();
        }
    }

    /**
     * Tells whether an address is bookmarked.
     * <p>
     * 주소가 북마크되어 있는지 알려 준다.
     *
     * @param url the address / 주소
     * @return true if a bookmark has exactly this address / 정확히 이 주소인 북마크가 있으면 true
     */
    public boolean contains(String url) {
        return find(root, url) != null;
    }

    /**
     * Finds the first bookmark with an address.
     * <p>
     * 주소가 같은 첫 북마크를 찾는다.
     *
     * @param url the address / 주소
     * @return the bookmark, or null / 북마크. 없으면 null
     */
    public BookmarkNode find(String url) {
        return find(root, url);
    }

    /**
     * Finds the first bookmark with an address in a folder, in document order.
     * <p>
     * 폴더에서 주소가 같은 첫 북마크를 문서 순서로 찾는다.
     *
     * @param folder the folder / 폴더
     * @param url    the address / 주소
     * @return the bookmark, or null / 북마크. 없으면 null
     */
    private static BookmarkNode find(BookmarkNode folder, String url) {
        if (url == null) {
            return null;
        }
        // 항목마다 북마크이면 주소를 비교하고 폴더이면 안으로 들어간다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode n = folder.getChild(i);
            if (n.isFolder()) {
                BookmarkNode inner = find(n, url);
                if (inner != null) {
                    return inner;
                }
            } else if (url.equals(n.getUrl())) {
                return n;
            }
        }
        return null;
    }

    /**
     * Counts the bookmarks (not the folders) of the whole tree.
     * <p>
     * 트리 전체의 북마크(폴더 제외) 수를 센다.
     *
     * @return the count / 개수
     */
    public int countBookmarks() {
        return count(root);
    }

    /**
     * Counts the bookmarks of a folder at any depth.
     * <p>
     * 폴더의 북마크 수를 깊이와 무관하게 센다.
     *
     * @param folder the folder / 폴더
     * @return the count / 개수
     */
    private static int count(BookmarkNode folder) {
        int n = 0;
        // 항목마다 북마크는 세고 폴더는 안을 센다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode c = folder.getChild(i);
            n += c.isFolder() ? count(c) : 1;
        }
        return n;
    }

    /**
     * Checks that an entry is a folder of this model.
     * <p>
     * 항목이 이 모델의 폴더인지 확인한다.
     *
     * @param parent the entry / 항목
     * @throws IllegalArgumentException if it is not / 아니면
     */
    private void requireFolder(BookmarkNode parent) {
        if (parent == null || !parent.isFolder() || !parent.isInside(root)) {
            throw new IllegalArgumentException("the parent must be a folder of this model");
        }
    }

    /**
     * Adds a listener.
     * <p>
     * 리스너를 더한다.
     *
     * @param listener the listener / 리스너
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 뺀다.
     *
     * @param listener the listener / 리스너
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Tells the listeners that the bookmarks changed.
     * <p>
     * 북마크가 바뀌었음을 리스너에 알린다.
     */
    private void fireChanged() {
        List<Listener> copy = new ArrayList<Listener>(listeners);
        // 리스너마다 변경을 알린다.
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).bookmarksChanged(this);
        }
    }
}
