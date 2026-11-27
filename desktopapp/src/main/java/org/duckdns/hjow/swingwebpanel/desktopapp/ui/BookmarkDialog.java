package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkHtml;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkModel;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkNode;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.TextFiles;

/**
 * The bookmark manager (plan item P31-06): a window with the tree of the bookmarks, the name and address of the selected entry, and
 * buttons to open, add, change, delete, reorder, import and export. It works on the bookmark model of the application, so every change
 * shows in all windows at once and is saved by the application. Use it on the event dispatch thread.
 * <p>
 * 북마크 관리 창(계획 P31-06): 북마크 트리, 선택한 항목의 이름과 주소, 그리고 열기·추가·변경·삭제·순서 변경·가져오기·내보내기 단추가 있는 창.
 * 응용 프로그램의 북마크 모델을 다루므로 모든 변경이 모든 창에 바로 보이고 응용 프로그램이 저장한다. 이벤트 디스패치 스레드에서 쓴다.
 */
final class BookmarkDialog extends JDialog implements BookmarkModel.Listener {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** Opens a bookmark in the active tab. / 북마크를 활성 탭에서 연다. */
    static final int HERE = 0;

    /** Opens a bookmark in a new tab. / 북마크를 새 탭에서 연다. */
    static final int NEW_TAB = 1;

    /** Opens a bookmark in a new window. / 북마크를 새 창에서 연다. */
    static final int NEW_WINDOW = 2;

    /** The window that opens bookmarks for this manager. / 이 관리 창을 위해 북마크를 여는 창. */
    private final transient BrowserWindow window;

    /** The application. / 응용 프로그램. */
    private final transient BrowserApp app;

    /** The bookmarks. / 북마크. */
    private final transient BookmarkModel model;

    /** The listeners of the tree. / 트리의 리스너. */
    private final transient List<TreeModelListener> treeListeners = new ArrayList<TreeModelListener>();

    /** The tree of the bookmarks. / 북마크의 트리. */
    private final JTree tree;

    /** The name of the selected entry. / 선택한 항목의 이름. */
    private final JTextField nameField = new JTextField(30);

    /** The address of the selected entry. / 선택한 항목의 주소. */
    private final JTextField urlField = new JTextField(30);

    /**
     * Shows the bookmark tree to the Swing tree: the entries themselves are the tree nodes.
     * <p>
     * 북마크 트리를 Swing 트리에 보여 준다: 항목 자신이 트리 노드이다.
     */
    private final class Nodes implements TreeModel {

        /**
         * Returns the root folder.
         * <p>
         * 루트 폴더를 반환한다.
         *
         * @return the root / 루트
         */
        public Object getRoot() {
            return model.getRoot();
        }

        /**
         * Returns an entry of a folder.
         * <p>
         * 폴더의 항목 하나를 반환한다.
         *
         * @param parent the folder / 폴더
         * @param index  the position / 위치
         * @return the entry / 항목
         */
        public Object getChild(Object parent, int index) {
            return ((BookmarkNode) parent).getChild(index);
        }

        /**
         * Returns the number of entries of a folder.
         * <p>
         * 폴더의 항목 수를 반환한다.
         *
         * @param parent the folder / 폴더
         * @return the count / 개수
         */
        public int getChildCount(Object parent) {
            return ((BookmarkNode) parent).getChildCount();
        }

        /**
         * Tells whether an entry is a bookmark (a leaf of the tree).
         * <p>
         * 항목이 북마크(트리의 잎)인지 알려 준다.
         *
         * @param node the entry / 항목
         * @return true for a bookmark / 북마크이면 true
         */
        public boolean isLeaf(Object node) {
            return !((BookmarkNode) node).isFolder();
        }

        /**
         * Not used: the tree is not editable in place.
         * <p>
         * 쓰지 않는다: 트리는 제자리에서 편집하지 않는다.
         *
         * @param path     the path / 경로
         * @param newValue the value / 값
         */
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        /**
         * Returns the position of an entry in a folder.
         * <p>
         * 폴더에서 항목의 위치를 반환한다.
         *
         * @param parent the folder / 폴더
         * @param child  the entry / 항목
         * @return the position or -1 / 위치 또는 -1
         */
        public int getIndexOfChild(Object parent, Object child) {
            return parent == null || child == null ? -1 : ((BookmarkNode) parent).indexOf((BookmarkNode) child);
        }

        /**
         * Adds a listener of the tree.
         * <p>
         * 트리의 리스너를 더한다.
         *
         * @param l the listener / 리스너
         */
        public void addTreeModelListener(TreeModelListener l) {
            treeListeners.add(l);
        }

        /**
         * Removes a listener of the tree.
         * <p>
         * 트리의 리스너를 뺀다.
         *
         * @param l the listener / 리스너
         */
        public void removeTreeModelListener(TreeModelListener l) {
            treeListeners.remove(l);
        }
    }

    /**
     * Creates the manager for a window; it is not shown yet.
     * <p>
     * 창을 위한 관리 창을 만든다. 아직 보이지는 않는다.
     *
     * @param window the window that owns the dialog and opens the bookmarks / 대화 상자를 소유하고 북마크를 여는 창
     * @param app    the application / 응용 프로그램
     */
    BookmarkDialog(BrowserWindow window, BrowserApp app) {
        super(window, Texts.of("Bookmarks", "북마크"), false);
        this.window = window;
        this.app = app;
        this.model = app.getBookmarks();
        tree = new JTree(new Nodes());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            /**
             * Shows the selected entry in the fields.
             * <p>
             * 선택한 항목을 필드에 보인다.
             *
             * @param e the event / 이벤트
             */
            public void valueChanged(TreeSelectionEvent e) {
                showSelection();
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            /**
             * Opens a bookmark that is double-clicked.
             * <p>
             * 두 번 누른 북마크를 연다.
             *
             * @param e the event / 이벤트
             */
            public void mouseClicked(MouseEvent e) {
                BookmarkNode n = getSelected();
                if (e.getClickCount() == 2 && n != null && !n.isFolder()) {
                    open(HERE);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(420, 300));

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        fields.add(new JLabel(Texts.of("Name:", "이름:")), c);
        c.gridy = 1;
        fields.add(new JLabel(Texts.of("Address:", "주소:")), c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        fields.add(nameField, c);
        c.gridy = 1;
        fields.add(urlField, c);

        JPanel open = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        open.add(button("open", Texts.of("Open", "열기"), new Runnable() {
            /** Opens the bookmark in the active tab. / 북마크를 활성 탭에서 연다. */
            public void run() {
                open(HERE);
            }
        }));
        open.add(button("open-tab", Texts.of("Open in New Tab", "새 탭에서 열기"), new Runnable() {
            /** Opens the bookmark in a new tab. / 북마크를 새 탭에서 연다. */
            public void run() {
                open(NEW_TAB);
            }
        }));
        open.add(button("open-window", Texts.of("Open in New Window", "새 창에서 열기"), new Runnable() {
            /** Opens the bookmark in a new window. / 북마크를 새 창에서 연다. */
            public void run() {
                open(NEW_WINDOW);
            }
        }));

        JPanel edit = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        edit.add(button("apply", Texts.of("Save Changes", "변경 저장"), new Runnable() {
            /** Writes the fields into the selected entry. / 필드를 선택한 항목에 적는다. */
            public void run() {
                apply();
            }
        }));
        edit.add(button("new-folder", Texts.of("New Folder", "새 폴더"), new Runnable() {
            /** Adds a folder. / 폴더를 더한다. */
            public void run() {
                newFolder();
            }
        }));
        edit.add(button("delete", Texts.of("Delete", "삭제"), new Runnable() {
            /** Deletes the selected entry. / 선택한 항목을 지운다. */
            public void run() {
                delete();
            }
        }));
        edit.add(button("up", Texts.of("Up", "위로"), new Runnable() {
            /** Moves the selected entry up. / 선택한 항목을 위로 옮긴다. */
            public void run() {
                shift(-1);
            }
        }));
        edit.add(button("down", Texts.of("Down", "아래로"), new Runnable() {
            /** Moves the selected entry down. / 선택한 항목을 아래로 옮긴다. */
            public void run() {
                shift(1);
            }
        }));
        edit.add(button("move", Texts.of("Move to Folder...", "폴더로 옮기기..."), new Runnable() {
            /** Asks for a folder and moves the selected entry into it. / 폴더를 물어 선택한 항목을 그 안으로 옮긴다. */
            public void run() {
                askMove();
            }
        }));

        JPanel files = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        files.add(button("import", Texts.of("Import...", "가져오기..."), new Runnable() {
            /** Imports a bookmark file. / 북마크 파일을 가져온다. */
            public void run() {
                importFile();
            }
        }));
        files.add(button("export", Texts.of("Export...", "내보내기..."), new Runnable() {
            /** Exports a bookmark file. / 북마크 파일을 내보낸다. */
            public void run() {
                exportFile();
            }
        }));
        files.add(button("close", Texts.of("Close", "닫기"), new Runnable() {
            /** Closes the manager. / 관리 창을 닫는다. */
            public void run() {
                close();
            }
        }));

        JPanel south = new JPanel(new GridBagLayout());
        GridBagConstraints s = new GridBagConstraints();
        s.gridx = 0;
        s.weightx = 1;
        s.fill = GridBagConstraints.HORIZONTAL;
        s.gridy = 0;
        south.add(fields, s);
        s.gridy = 1;
        south.add(open, s);
        s.gridy = 2;
        south.add(edit, s);
        s.gridy = 3;
        south.add(files, s);

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(scroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(window);
        model.addListener(this);
        showSelection();
    }

    /**
     * Creates a button that runs an action.
     * <p>
     * 동작을 실행하는 단추를 만든다.
     *
     * @param name   the component name, for tests / 시험용 컴포넌트 이름
     * @param text   the text / 글
     * @param action the action / 동작
     * @return the button / 단추
     */
    private static JButton button(String name, String text, final Runnable action) {
        JButton b = new JButton(text);
        b.setName(name);
        b.addActionListener(new ActionListener() {
            /** Runs the action of the button. / 단추의 동작을 실행한다. */
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return b;
    }

    /**
     * Returns the selected entry.
     * <p>
     * 선택한 항목을 반환한다.
     *
     * @return the entry, or null if nothing is selected / 항목. 선택한 것이 없으면 null
     */
    BookmarkNode getSelected() {
        TreePath path = tree.getSelectionPath();
        return path == null ? null : (BookmarkNode) path.getLastPathComponent();
    }

    /**
     * Selects an entry in the tree.
     * <p>
     * 트리에서 항목을 선택한다.
     *
     * @param node an entry of the bookmarks, or null to select nothing / 북마크의 항목. 아무것도 선택하지 않으려면 null
     */
    void select(BookmarkNode node) {
        if (node == null || node == model.getRoot() || !node.isInside(model.getRoot())) {
            tree.clearSelection();
        } else {
            TreePath path = pathOf(node);
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
        showSelection();
    }

    /**
     * Returns the tree path of an entry.
     * <p>
     * 항목의 트리 경로를 반환한다.
     *
     * @param node the entry / 항목
     * @return the path from the root / 루트부터의 경로
     */
    private static TreePath pathOf(BookmarkNode node) {
        List<Object> chain = new ArrayList<Object>();
        // 항목에서 루트까지 올라가며 경로를 거꾸로 모은다.
        for (BookmarkNode n = node; n != null; n = n.getParent()) {
            chain.add(0, n);
        }
        return new TreePath(chain.toArray());
    }

    /**
     * Shows the name and the address of the selected entry in the fields.
     * <p>
     * 선택한 항목의 이름과 주소를 필드에 보인다.
     */
    private void showSelection() {
        BookmarkNode n = getSelected();
        nameField.setText(n == null ? "" : n.getName());
        urlField.setText(n == null ? "" : n.getUrl());
        nameField.setEnabled(n != null);
        urlField.setEnabled(n != null && !n.isFolder());
    }

    /**
     * Sets the text of the fields; for tests.
     * <p>
     * 필드의 글을 정한다. 시험용이다.
     *
     * @param name the name / 이름
     * @param url  the address / 주소
     */
    void type(String name, String url) {
        nameField.setText(name);
        urlField.setText(url);
    }

    /**
     * Writes the fields into the selected entry.
     * <p>
     * 필드를 선택한 항목에 적는다.
     */
    void apply() {
        BookmarkNode n = getSelected();
        if (n != null) {
            model.edit(n, nameField.getText().trim(), urlField.getText().trim());
        }
    }

    /**
     * Returns the folder that a new entry goes into: the selected folder, or the folder of the selected bookmark, or the root.
     * <p>
     * 새 항목이 들어갈 폴더를 반환한다: 선택한 폴더, 또는 선택한 북마크의 폴더, 또는 루트.
     *
     * @return the folder / 폴더
     */
    private BookmarkNode targetFolder() {
        BookmarkNode n = getSelected();
        if (n == null) {
            return model.getRoot();
        }
        return n.isFolder() ? n : n.getParent();
    }

    /**
     * Asks for a name and adds a folder with it.
     * <p>
     * 이름을 물어 그 이름의 폴더를 더한다.
     */
    void newFolder() {
        String name = app.getPrompts().ask(this, Texts.of("Name of the new folder:", "새 폴더의 이름:"), Texts.of("New folder", "새 폴더"));
        if (name == null || name.trim().length() == 0) {
            return;
        }
        BookmarkNode folder = BookmarkNode.folder(name.trim());
        model.add(targetFolder(), folder);
        select(folder);
    }

    /**
     * Deletes the selected entry; a folder that is not empty is deleted only after the user agreed.
     * <p>
     * 선택한 항목을 지운다. 비어 있지 않은 폴더는 사용자가 동의한 뒤에만 지운다.
     */
    void delete() {
        BookmarkNode n = getSelected();
        if (n == null) {
            return;
        }
        // 안에 항목이 있는 폴더를 지우기 전에 확인한다.
        if (n.isFolder() && n.getChildCount() > 0
                && !app.getPrompts().confirm(this, Texts.of("Delete the folder and everything in it?\n", "폴더와 그 안의 모든 것을 지울까요?\n") + n.getName())) {
            return;
        }
        model.remove(n);
    }

    /**
     * Moves the selected entry up or down inside its folder.
     * <p>
     * 선택한 항목을 그 폴더 안에서 위나 아래로 옮긴다.
     *
     * @param delta -1 for up, 1 for down / 위이면 -1, 아래이면 1
     */
    void shift(int delta) {
        BookmarkNode n = getSelected();
        if (n != null && model.shift(n, delta)) {
            select(n);
        }
    }

    /**
     * Moves the selected entry to the end of a folder.
     * <p>
     * 선택한 항목을 폴더의 끝으로 옮긴다.
     *
     * @param folder the folder to move into / 옮겨 갈 폴더
     * @return true if the entry was moved / 옮겼으면 true
     */
    boolean moveTo(BookmarkNode folder) {
        BookmarkNode n = getSelected();
        if (n == null || folder == null || !model.move(n, folder, folder.getChildCount())) {
            return false;
        }
        select(n);
        return true;
    }

    /**
     * Lets the user pick a folder from a list and moves the selected entry into it.
     * <p>
     * 사용자가 목록에서 폴더를 고르게 하고 선택한 항목을 그 안으로 옮긴다.
     */
    private void askMove() {
        BookmarkNode n = getSelected();
        if (n == null) {
            return;
        }
        List<BookmarkNode> folders = new ArrayList<BookmarkNode>();
        List<String> names = new ArrayList<String>();
        collectFolders(model.getRoot(), "", n, folders, names);
        Object chosen = javax.swing.JOptionPane.showInputDialog(this, Texts.of("Move into the folder:", "옮겨 갈 폴더:"), getTitle(),
                javax.swing.JOptionPane.QUESTION_MESSAGE, null, names.toArray(), names.get(0));
        int at = chosen == null ? -1 : names.indexOf(chosen);
        if (at >= 0) {
            moveTo(folders.get(at));
        }
    }

    /**
     * Collects the folders that an entry can be moved into, with a readable path for each.
     * <p>
     * 항목을 옮겨 갈 수 있는 폴더를 읽기 좋은 경로와 함께 모은다.
     *
     * @param folder  the folder to start at / 시작할 폴더
     * @param path    the readable path of that folder / 그 폴더의 읽기 좋은 경로
     * @param moving  the entry that is moved; it and the folders inside it are left out / 옮기는 항목. 자신과 그 안의 폴더는 뺀다
     * @param folders receives the folders / 폴더를 받는다
     * @param names   receives the paths / 경로를 받는다
     */
    private static void collectFolders(BookmarkNode folder, String path, BookmarkNode moving, List<BookmarkNode> folders, List<String> names) {
        folders.add(folder);
        names.add(path.length() == 0 ? Texts.of("(top level)", "(최상위)") : path);
        // 하위 폴더마다 내려간다. 옮기는 항목 자신은 대상이 될 수 없다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode c = folder.getChild(i);
            if (c.isFolder() && c != moving) {
                collectFolders(c, path.length() == 0 ? c.getName() : path + " / " + c.getName(), moving, folders, names);
            }
        }
    }

    /**
     * Opens the selected bookmark, or all bookmarks of the selected folder in new tabs.
     * <p>
     * 선택한 북마크를 연다. 폴더를 선택했으면 그 폴더의 모든 북마크를 새 탭에 연다.
     *
     * @param where {@link #HERE}, {@link #NEW_TAB} or {@link #NEW_WINDOW} / {@link #HERE}, {@link #NEW_TAB}, {@link #NEW_WINDOW} 중 하나
     */
    void open(int where) {
        BookmarkNode n = getSelected();
        if (n == null) {
            return;
        }
        if (n.isFolder()) {
            window.openBookmarkFolder(n);
        } else {
            window.openBookmark(n.getUrl(), where);
        }
    }

    /**
     * Asks for a bookmark file and adds its entries to the bookmarks, inside a new folder named after the file.
     * <p>
     * 북마크 파일을 물어 그 항목을 북마크에 더한다. 파일 이름을 딴 새 폴더 안에 넣는다.
     */
    void importFile() {
        File f = app.getPrompts().chooseFile(this, false, null);
        if (f == null) {
            return;
        }
        try {
            String text = TextFiles.read(f);
            BookmarkNode imported = BookmarkHtml.parse(text);
            // 항목이 하나도 없으면 북마크 파일이 아닌 것으로 알린다.
            if (imported.getChildCount() == 0) {
                app.getPrompts().inform(this, Texts.of("No bookmarks were found in the file.", "파일에서 북마크를 찾지 못했습니다."));
                return;
            }
            BookmarkNode folder = BookmarkNode.folder(Texts.of("Imported from ", "가져옴: ") + f.getName());
            model.add(model.getRoot(), folder);
            model.addAll(folder, imported);
            select(folder);
        } catch (IOException e) {
            app.getPrompts().inform(this, Texts.of("The file could not be read: ", "파일을 읽을 수 없습니다: ") + e.getMessage());
        }
    }

    /**
     * Asks for a file and writes all bookmarks into it as a bookmark file.
     * <p>
     * 파일을 물어 모든 북마크를 북마크 파일로 쓴다.
     */
    void exportFile() {
        File f = app.getPrompts().chooseFile(this, true, "bookmarks.html");
        if (f == null) {
            return;
        }
        try {
            TextFiles.write(f, BookmarkHtml.export(model.getRoot()));
        } catch (IOException e) {
            app.getPrompts().inform(this, Texts.of("The file could not be written: ", "파일을 쓸 수 없습니다: ") + e.getMessage());
        }
    }

    /**
     * Brings the tree up to date after the bookmarks changed: the folders that were open stay open and the selected entry stays
     * selected if it still exists.
     * <p>
     * 북마크가 바뀐 뒤 트리를 갱신한다: 열려 있던 폴더는 열린 채로 두고, 선택한 항목이 아직 있으면 선택을 유지한다.
     *
     * @param changed the bookmarks / 북마크
     */
    public void bookmarksChanged(BookmarkModel changed) {
        BookmarkNode selected = getSelected();
        List<BookmarkNode> expanded = new ArrayList<BookmarkNode>();
        Enumeration<TreePath> open = tree.getExpandedDescendants(new TreePath(model.getRoot()));
        // 열려 있던 폴더를 기억한다.
        while (open != null && open.hasMoreElements()) {
            expanded.add((BookmarkNode) open.nextElement().getLastPathComponent());
        }
        TreeModelEvent event = new TreeModelEvent(this, new Object[] { model.getRoot() });
        List<TreeModelListener> copy = new ArrayList<TreeModelListener>(treeListeners);
        // 트리 전체가 바뀌었다고 알린다.
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).treeStructureChanged(event);
        }
        // 아직 있는 폴더를 다시 연다.
        for (int i = 0; i < expanded.size(); i++) {
            BookmarkNode n = expanded.get(i);
            if (n != model.getRoot() && n.isInside(model.getRoot())) {
                tree.expandPath(pathOf(n));
            }
        }
        select(selected);
    }

    /**
     * Closes the manager and stops listening to the bookmarks.
     * <p>
     * 관리 창을 닫고 북마크의 변경을 더 받지 않는다.
     */
    void close() {
        model.removeListener(this);
        dispose();
    }

    /**
     * Stops listening to the bookmarks when the dialog is disposed.
     * <p>
     * 대화 상자가 폐기될 때 북마크의 변경을 더 받지 않는다.
     */
    public void dispose() {
        model.removeListener(this);
        super.dispose();
    }
}
