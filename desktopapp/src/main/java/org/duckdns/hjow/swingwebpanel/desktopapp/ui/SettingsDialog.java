package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.ZoomSteps;

/**
 * The settings dialog (plan item P31-07): the home page and what the program and new tabs start with, tabs, popups, the default fonts
 * with a preview, scripts, the User-Agent, downloads, zoom, search and printing. Nothing changes while the user works in the dialog;
 * OK writes all values into the settings of the application at once, which applies them to every open window and tab and saves them,
 * and Cancel leaves everything as it was. Use it on the event dispatch thread.
 * <p>
 * 설정 대화 상자(계획 P31-07): 홈 페이지, 프로그램과 새 탭이 시작할 때 여는 것, 탭, 팝업, 미리보기가 있는 기본 글꼴, 스크립트, User-Agent, 다운로드,
 * 확대, 검색, 인쇄. 사용자가 대화 상자에서 작업하는 동안에는 아무것도 바뀌지 않는다. 확인은 모든 값을 응용 프로그램의 설정에 한 번에 적고, 설정이
 * 열린 모든 창과 탭에 적용하고 저장한다. 취소는 모든 것을 그대로 둔다. 이벤트 디스패치 스레드에서 쓴다.
 */
final class SettingsDialog extends JDialog {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The text of the font choice that means "the built-in font". / "내장 글꼴"을 뜻하는 글꼴 선택 항목의 글. */
    private static final String BUILT_IN_FONT = Texts.of("(default)", "(기본값)");

    /**
     * One control of the dialog as a text value.
     * <p>
     * 대화 상자의 컨트롤 하나를 텍스트 값으로 다룬다.
     */
    private interface Field {

        /**
         * Returns the value of the control as it is stored in the settings.
         * <p>
         * 컨트롤의 값을 설정에 저장하는 형태로 반환한다.
         *
         * @return the value / 값
         */
        String get();

        /**
         * Shows a value of the settings in the control.
         * <p>
         * 설정의 값을 컨트롤에 보인다.
         *
         * @param value the value / 값
         */
        void set(String value);
    }

    /** The window that owns the dialog. / 대화 상자를 소유한 창. */
    private final transient BrowserWindow window;

    /** The application. / 응용 프로그램. */
    private final transient BrowserApp app;

    /** The controls by the key of their setting. / 설정 키별 컨트롤. */
    private final transient Map<String, Field> fields = new LinkedHashMap<String, Field>();

    /** The sample text that is shown in the chosen standard font. / 고른 표준 글꼴로 보이는 표본 글. */
    private final JLabel preview = new JLabel("AaBbCc 123 가나다라 한글 漢字");

    /** The sample text that is shown in the chosen monospace font. / 고른 고정폭 글꼴로 보이는 표본 글. */
    private final JLabel monoPreview = new JLabel("if (x == 0) { return; } 가나다");

    /**
     * Creates the dialog with the values of the settings; it is not shown yet.
     * <p>
     * 설정의 값으로 대화 상자를 만든다. 아직 보이지는 않는다.
     *
     * @param window the window that owns the dialog / 대화 상자를 소유할 창
     * @param app    the application / 응용 프로그램
     */
    SettingsDialog(BrowserWindow window, BrowserApp app) {
        super(window, Texts.of("Settings", "설정"), true);
        this.window = window;
        this.app = app;
        JTabbedPane sections = new JTabbedPane();
        sections.addTab(Texts.of("General", "일반"), general());
        sections.addTab(Texts.of("Tabs and Windows", "탭과 창"), tabsAndWindows());
        sections.addTab(Texts.of("Fonts", "글꼴"), fonts());
        sections.addTab(Texts.of("Content", "콘텐츠"), content());
        sections.addTab(Texts.of("Printing", "인쇄"), printing());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        buttons.add(button("defaults", Texts.of("Restore Defaults", "기본값으로"), new Runnable() {
            /** Shows the default values in the controls. / 컨트롤에 기본값을 보인다. */
            public void run() {
                showDefaults();
            }
        }));
        JButton ok = button("ok", Texts.of("OK", "확인"), new Runnable() {
            /** Applies the values and closes. / 값을 적용하고 닫는다. */
            public void run() {
                accept();
            }
        });
        buttons.add(ok);
        buttons.add(button("cancel", Texts.of("Cancel", "취소"), new Runnable() {
            /** Closes without applying. / 적용하지 않고 닫는다. */
            public void run() {
                cancel();
            }
        }));
        JPanel all = new JPanel(new BorderLayout());
        all.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
        all.add(sections, BorderLayout.CENTER);
        all.add(buttons, BorderLayout.SOUTH);
        setContentPane(all);
        getRootPane().setDefaultButton(ok);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        load(app.getSettings().toMap());
        pack();
        setLocationRelativeTo(window);
    }

    /**
     * Builds the section with the pages that the program starts with, the search and the downloads.
     * <p>
     * 프로그램이 시작할 때 여는 페이지, 검색, 다운로드에 관한 구역을 만든다.
     *
     * @return the panel / 패널
     */
    private JPanel general() {
        Rows rows = new Rows();
        final JTextField home = text(Settings.HOME_PAGE, 32);
        rows.add(Texts.of("Home page:", "홈 페이지:"), home);
        JPanel homeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        homeButtons.add(button("home-current", Texts.of("Use Current Page", "현재 페이지 사용"), new Runnable() {
            /** Takes the address of the active tab. / 활성 탭의 주소를 가져온다. */
            public void run() {
                home.setText(window.getActiveTab().getUrl());
            }
        }));
        homeButtons.add(button("home-blank", Texts.of("Use Blank Page", "빈 페이지 사용"), new Runnable() {
            /** Takes the empty page. / 빈 페이지로 한다. */
            public void run() {
                home.setText(BrowserApp.BLANK);
            }
        }));
        rows.add("", homeButtons);
        rows.add(Texts.of("At start, open:", "시작할 때 열기:"), choice(Settings.START_PAGE,
                new String[] { Settings.START_HOME, Settings.START_BLANK, Settings.START_RESTORE },
                new String[] { Texts.of("The home page", "홈 페이지"), Texts.of("A blank page", "빈 페이지"),
                        Texts.of("The windows and tabs of last time", "지난번의 창과 탭") }));
        rows.add(Texts.of("A new tab shows:", "새 탭에 보일 것:"), choice(Settings.NEW_TAB_PAGE,
                new String[] { Settings.NEW_TAB_BLANK, Settings.NEW_TAB_HOME },
                new String[] { Texts.of("A blank page", "빈 페이지"), Texts.of("The home page", "홈 페이지") }));
        JTextField search = text(Settings.SEARCH_TEMPLATE, 32);
        search.setToolTipText(Texts.of("An address with %s where the search words go; empty to turn searching off",
                "검색어가 들어갈 자리에 %s 를 둔 주소. 비우면 검색하지 않습니다"));
        rows.add(Texts.of("Search address:", "검색 주소:"), search);
        final JTextField dir = text(Settings.DOWNLOAD_DIRECTORY, 26);
        JPanel dirPanel = new JPanel(new BorderLayout(4, 0));
        dirPanel.add(dir, BorderLayout.CENTER);
        dirPanel.add(button("download-browse", Texts.of("Browse...", "찾아보기..."), new Runnable() {
            /** Asks for a folder. / 폴더를 묻는다. */
            public void run() {
                File chosen = app.getPrompts().chooseDirectory(SettingsDialog.this, dir.getText().trim().length() == 0 ? null : new File(dir.getText().trim()));
                if (chosen != null) {
                    dir.setText(chosen.getPath());
                }
            }
        }), BorderLayout.EAST);
        rows.add(Texts.of("Download folder:", "다운로드 폴더:"), dirPanel);
        rows.add(Texts.of("Default zoom (%):", "기본 확대 비율(%):"), spinner(Settings.DEFAULT_ZOOM, 25, 500, 5));
        return rows.panel();
    }

    /**
     * Builds the section about tabs, new windows and popups.
     * <p>
     * 탭, 새 창, 팝업에 관한 구역을 만든다.
     *
     * @return the panel / 패널
     */
    private JPanel tabsAndWindows() {
        Rows rows = new Rows();
        rows.add("", check(Settings.SELECT_NEW_TABS, Texts.of("Switch to a link that is opened in a new tab at once", "링크를 새 탭에서 열면 바로 그 탭으로 전환")));
        rows.add("", check(Settings.CONFIRM_CLOSE_TABS, Texts.of("Ask before closing a window with several tabs", "탭이 여럿인 창을 닫기 전에 확인")));
        rows.add(Texts.of("Links that ask for a new window open in:", "새 창을 요청하는 링크를 열 곳:"), choice(Settings.LINK_PLACE,
                new String[] { Settings.PLACE_TAB, Settings.PLACE_WINDOW },
                new String[] { Texts.of("A new tab", "새 탭"), Texts.of("A new window", "새 창") }));
        rows.add("", check(Settings.BLOCK_POPUPS, Texts.of("Block popups that the user did not ask for", "사용자가 요청하지 않은 팝업 차단")));
        rows.add(Texts.of("Windows opened by scripts (popups) open in:", "스크립트가 연 창(팝업)을 열 곳:"), choice(Settings.POPUP_PLACE,
                new String[] { Settings.PLACE_POPUP, Settings.PLACE_WINDOW, Settings.PLACE_TAB },
                new String[] { Texts.of("A popup window", "팝업 창"), Texts.of("A new window", "새 창"), Texts.of("A new tab", "새 탭") }));
        rows.add("", check(Settings.SHOW_TOOLBAR, Texts.of("Show the toolbar", "도구 모음 보이기")));
        rows.add("", check(Settings.SHOW_STATUS_BAR, Texts.of("Show the status bar", "상태 표시줄 보이기")));
        return rows.panel();
    }

    /**
     * Builds the section with the default fonts and their preview.
     * <p>
     * 기본 글꼴과 그 미리보기가 있는 구역을 만든다.
     *
     * @return the panel / 패널
     */
    private JPanel fonts() {
        Rows rows = new Rows();
        rows.add(Texts.of("Standard font:", "표준 글꼴:"), font(Settings.FONT_STANDARD));
        rows.add(Texts.of("Serif font:", "Serif 글꼴:"), font(Settings.FONT_SERIF));
        rows.add(Texts.of("Sans-serif font:", "Sans-serif 글꼴:"), font(Settings.FONT_SANS_SERIF));
        rows.add(Texts.of("Fixed-width font:", "고정폭 글꼴:"), font(Settings.FONT_MONOSPACE));
        rows.add(Texts.of("Default size:", "기본 크기:"), spinner(Settings.FONT_SIZE, 6, 72, 1));
        rows.add(Texts.of("Fixed-width size:", "고정폭 크기:"), spinner(Settings.FONT_MONOSPACE_SIZE, 6, 72, 1));
        preview.setBorder(BorderFactory.createEmptyBorder(6, 2, 2, 2));
        monoPreview.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
        rows.add(Texts.of("Preview:", "미리보기:"), preview);
        rows.add("", monoPreview);
        rows.add("", new JLabel(Texts.of("These fonts are used where a page names no font of its own.", "페이지가 자기 글꼴을 지정하지 않은 곳에 이 글꼴을 씁니다.")));
        return rows.panel();
    }

    /**
     * Builds the section about scripts and the User-Agent.
     * <p>
     * 스크립트와 User-Agent 에 관한 구역을 만든다.
     *
     * @return the panel / 패널
     */
    private JPanel content() {
        Rows rows = new Rows();
        rows.add("", check(Settings.JAVASCRIPT, Texts.of("Run JavaScript", "JavaScript 사용")));
        rows.add("", check(Settings.SCRIPTS_CLOSE_WINDOWS, Texts.of("Let scripts close tabs that I opened", "내가 연 탭을 스크립트가 닫도록 허용")));
        rows.add("", new JLabel(Texts.of("Warning: with this on, any page can close its tab, and the program when it is the last tab.",
                "경고: 켜 두면 어떤 페이지든 자기 탭을 닫을 수 있고, 마지막 탭이면 프로그램이 끝납니다.")));
        JTextField agent = text(Settings.USER_AGENT, 32);
        agent.setToolTipText(Texts.of("Empty for the default of SwingWebPanel", "비우면 SwingWebPanel 의 기본값을 씁니다"));
        rows.add(Texts.of("User-Agent:", "User-Agent:"), agent);
        return rows.panel();
    }

    /**
     * Builds the section about printing.
     * <p>
     * 인쇄에 관한 구역을 만든다.
     *
     * @return the panel / 패널
     */
    private JPanel printing() {
        Rows rows = new Rows();
        rows.add("", check(Settings.PRINT_BACKGROUNDS, Texts.of("Print background colors and images", "배경색과 배경 이미지 인쇄")));
        rows.add("", check(Settings.PRINT_HEADERS, Texts.of("Print headers and footers (title, address, page number, date)",
                "머리글과 바닥글 인쇄(제목, 주소, 쪽 번호, 날짜)")));
        return rows.panel();
    }

    /**
     * Lays out the rows of a section: a label and a control per row.
     * <p>
     * 구역의 행을 배치한다: 행마다 레이블과 컨트롤 하나.
     */
    private static final class Rows {

        /** The panel that holds the rows. / 행을 담는 패널. */
        private final JPanel grid = new JPanel(new GridBagLayout());

        /** The number of rows so far. / 지금까지의 행 수. */
        private int count;

        /**
         * Creates an empty section.
         * <p>
         * 빈 구역을 만든다.
         */
        Rows() {
        }

        /**
         * Adds a row.
         * <p>
         * 행을 더한다.
         *
         * @param label   the text of the label, empty for none / 레이블의 글. 없으면 빈 글
         * @param control the control / 컨트롤
         */
        void add(String label, JComponent control) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = count++;
            c.insets = new Insets(3, 4, 3, 4);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            grid.add(new JLabel(label), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(control, c);
        }

        /**
         * Returns the section with its rows at the top.
         * <p>
         * 행을 위쪽에 둔 구역을 반환한다.
         *
         * @return the panel / 패널
         */
        JPanel panel() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            p.add(grid, BorderLayout.NORTH);
            return p;
        }
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
     * Creates a text field for a setting.
     * <p>
     * 설정을 위한 텍스트 필드를 만든다.
     *
     * @param key     the key of the setting / 설정의 키
     * @param columns the width in columns / 폭(열 수)
     * @return the field / 필드
     */
    private JTextField text(String key, int columns) {
        final JTextField field = new JTextField(columns);
        field.setName(key);
        fields.put(key, new Field() {
            /** Returns the trimmed text. / 앞뒤 공백을 뗀 글을 반환한다. */
            public String get() {
                return field.getText().trim();
            }

            /** Shows the value. / 값을 보인다. */
            public void set(String value) {
                field.setText(value);
                field.setCaretPosition(0);
            }
        });
        return field;
    }

    /**
     * Creates a check box for a setting that is true or false.
     * <p>
     * 참/거짓 설정을 위한 체크 상자를 만든다.
     *
     * @param key  the key of the setting / 설정의 키
     * @param text the text of the check box / 체크 상자의 글
     * @return the check box / 체크 상자
     */
    private JCheckBox check(String key, String text) {
        final JCheckBox box = new JCheckBox(text);
        box.setName(key);
        fields.put(key, new Field() {
            /** Returns true or false as text. / 참/거짓을 글로 반환한다. */
            public String get() {
                return String.valueOf(box.isSelected());
            }

            /** Shows the value. / 값을 보인다. */
            public void set(String value) {
                box.setSelected("true".equalsIgnoreCase(value.trim()));
            }
        });
        return box;
    }

    /**
     * Creates a list of choices for a setting.
     * <p>
     * 설정을 위한 선택 목록을 만든다.
     *
     * @param key    the key of the setting / 설정의 키
     * @param values the values that are stored / 저장하는 값
     * @param labels the texts that are shown, in the same order / 같은 순서로 보이는 글
     * @return the list / 목록
     */
    private JComboBox choice(String key, final String[] values, String[] labels) {
        final JComboBox box = new JComboBox(labels);
        box.setName(key);
        fields.put(key, new Field() {
            /** Returns the value of the chosen entry. / 고른 항목의 값을 반환한다. */
            public String get() {
                int i = box.getSelectedIndex();
                return values[i < 0 ? 0 : i];
            }

            /** Chooses the entry of the value; an unknown value chooses the first entry. / 값의 항목을 고른다. 모르는 값이면 첫 항목을 고른다. */
            public void set(String value) {
                int at = 0;
                // 값이 같은 항목을 찾는다.
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(value.trim())) {
                        at = i;
                    }
                }
                box.setSelectedIndex(at);
            }
        });
        return box;
    }

    /**
     * Creates a list of the installed font families for a font setting; the first entry stands for the built-in font.
     * <p>
     * 글꼴 설정을 위해 설치된 글꼴 패밀리의 목록을 만든다. 첫 항목은 내장 글꼴을 뜻한다.
     *
     * @param key the key of the setting / 설정의 키
     * @return the list / 목록
     */
    private JComboBox font(String key) {
        List<String> names = new ArrayList<String>();
        names.add(BUILT_IN_FONT);
        try {
            String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            // 설치된 패밀리 이름을 목록에 더한다.
            for (int i = 0; i < installed.length; i++) {
                names.add(installed[i]);
            }
        } catch (RuntimeException e) {
            // 글꼴 목록을 읽을 수 없으면 내장 글꼴만 고를 수 있다.
            names.clear();
            names.add(BUILT_IN_FONT);
        }
        final JComboBox box = new JComboBox(names.toArray());
        box.setName(key);
        box.addActionListener(new ActionListener() {
            /** Shows the chosen font in the preview. / 고른 글꼴을 미리보기에 보인다. */
            public void actionPerformed(ActionEvent e) {
                updatePreview();
            }
        });
        fields.put(key, new Field() {
            /** Returns the chosen family, empty for the built-in font. / 고른 패밀리를 반환한다. 내장 글꼴이면 빈 글이다. */
            public String get() {
                Object chosen = box.getSelectedItem();
                return box.getSelectedIndex() <= 0 || chosen == null ? "" : chosen.toString();
            }

            /** Chooses the family; a family that is not installed is added to the list so that the setting is not lost. / 패밀리를 고른다. 설치되지 않은 패밀리는 설정을 잃지 않도록 목록에 더한다. */
            public void set(String value) {
                String v = value.trim();
                if (v.length() == 0) {
                    box.setSelectedIndex(0);
                    return;
                }
                box.setSelectedItem(v);
                if (!v.equals(box.getSelectedItem())) {
                    box.addItem(v);
                    box.setSelectedItem(v);
                }
            }
        });
        return box;
    }

    /**
     * Creates a number field for a setting.
     * <p>
     * 설정을 위한 숫자 필드를 만든다.
     *
     * @param key  the key of the setting / 설정의 키
     * @param min  the smallest value / 가장 작은 값
     * @param max  the largest value / 가장 큰 값
     * @param step the step of the arrows / 화살표의 단계
     * @return the field / 필드
     */
    private JSpinner spinner(final String key, final int min, final int max, int step) {
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, step));
        spinner.setName(key);
        spinner.addChangeListener(new ChangeListener() {
            /** Shows the new size in the preview. / 새 크기를 미리보기에 보인다. */
            public void stateChanged(ChangeEvent e) {
                updatePreview();
            }
        });
        fields.put(key, new Field() {
            /** Returns the number as text. / 수를 글로 반환한다. */
            public String get() {
                return String.valueOf(((Number) spinner.getValue()).intValue());
            }

            /** Shows the number; text that is not a number in the range shows the default of the setting. / 수를 보인다. 범위 안의 수가 아닌 글이면 설정의 기본값을 보인다. */
            public void set(String value) {
                int v;
                try {
                    v = (int) Math.round(Double.parseDouble(value.trim()));
                } catch (NumberFormatException e) {
                    v = Integer.MIN_VALUE;
                }
                if (v < min || v > max) {
                    try {
                        v = Integer.parseInt(Settings.defaultOf(key).trim());
                    } catch (NumberFormatException e) {
                        v = min;
                    }
                }
                spinner.setValue(Integer.valueOf(Math.max(min, Math.min(max, v))));
            }
        });
        return spinner;
    }

    /**
     * Shows the chosen fonts and sizes in the preview texts.
     * <p>
     * 고른 글꼴과 크기를 미리보기 글에 보인다.
     */
    private void updatePreview() {
        Field size = fields.get(Settings.FONT_SIZE);
        Field monoSize = fields.get(Settings.FONT_MONOSPACE_SIZE);
        Field standard = fields.get(Settings.FONT_STANDARD);
        Field mono = fields.get(Settings.FONT_MONOSPACE);
        // 구역을 만드는 도중에는 아직 없는 컨트롤이 있다.
        if (size == null || monoSize == null || standard == null || mono == null) {
            return;
        }
        String family = standard.get();
        String fixed = mono.get();
        preview.setFont(new Font(family.length() == 0 ? Font.SERIF : family, Font.PLAIN, number(size.get(), 16)));
        monoPreview.setFont(new Font(fixed.length() == 0 ? Font.MONOSPACED : fixed, Font.PLAIN, number(monoSize.get(), 13)));
    }

    /**
     * Reads a whole number.
     * <p>
     * 정수를 읽는다.
     *
     * @param text     the text / 글
     * @param fallback the number for text that is not a number / 수가 아닌 글일 때의 수
     * @return the number / 수
     */
    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Shows values in the controls.
     * <p>
     * 값을 컨트롤에 보인다.
     *
     * @param values the values by key / 키별 값
     */
    private void load(Map<String, String> values) {
        // 컨트롤마다 그 설정의 값을 보인다.
        for (Map.Entry<String, Field> e : fields.entrySet()) {
            String v = values.get(e.getKey());
            e.getValue().set(v == null ? Settings.defaultOf(e.getKey()) : v);
        }
        updatePreview();
    }

    /**
     * Shows the default value of every setting in the controls; nothing is applied until OK.
     * <p>
     * 모든 설정의 기본값을 컨트롤에 보인다. 확인을 누를 때까지 아무것도 적용하지 않는다.
     */
    void showDefaults() {
        Map<String, String> defaults = new LinkedHashMap<String, String>();
        // 컨트롤이 있는 설정마다 기본값을 모은다.
        for (String key : fields.keySet()) {
            defaults.put(key, Settings.defaultOf(key));
        }
        load(defaults);
    }

    /**
     * Returns the value that a control shows; for tests.
     * <p>
     * 컨트롤이 보이는 값을 반환한다. 시험용이다.
     *
     * @param key the key of the setting / 설정의 키
     * @return the value, or null if the dialog has no control for the key / 값. 그 키의 컨트롤이 없으면 null
     */
    String getValue(String key) {
        Field f = fields.get(key);
        return f == null ? null : f.get();
    }

    /**
     * Shows a value in a control as the user would enter it; for tests.
     * <p>
     * 사용자가 입력하듯이 컨트롤에 값을 보인다. 시험용이다.
     *
     * @param key   the key of the setting / 설정의 키
     * @param value the value / 값
     */
    void setValue(String key, String value) {
        Field f = fields.get(key);
        if (f != null) {
            f.set(value);
            updatePreview();
        }
    }

    /**
     * Returns the keys of the settings that the dialog has a control for; for tests.
     * <p>
     * 대화 상자에 컨트롤이 있는 설정의 키를 반환한다. 시험용이다.
     *
     * @return the keys / 키
     */
    List<String> getKeys() {
        return new ArrayList<String>(fields.keySet());
    }

    /**
     * Returns the font of the preview of the standard font; for tests.
     * <p>
     * 표준 글꼴 미리보기의 글꼴을 반환한다. 시험용이다.
     *
     * @return the font / 글꼴
     */
    Font getPreviewFont() {
        return preview.getFont();
    }

    /**
     * Writes all values into the settings of the application at once (which applies them to every window and tab and saves them) and
     * closes the dialog. The zoom is cut to the range that the component supports.
     * <p>
     * 모든 값을 응용 프로그램의 설정에 한 번에 적고(설정이 모든 창과 탭에 적용하고 저장한다) 대화 상자를 닫는다. 확대 비율은 컴포넌트가 지원하는
     * 범위로 자른다.
     */
    void accept() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        // 컨트롤마다 값을 모은다.
        for (Map.Entry<String, Field> e : fields.entrySet()) {
            values.put(e.getKey(), e.getValue().get());
        }
        values.put(Settings.DEFAULT_ZOOM, ZoomSteps.toPercent(ZoomSteps.parsePercent(values.get(Settings.DEFAULT_ZOOM), 1.0)));
        if (values.get(Settings.HOME_PAGE).length() == 0) {
            values.put(Settings.HOME_PAGE, BrowserApp.BLANK);
        }
        app.getSettings().setAll(values);
        dispose();
        String problem = app.takeStorageProblem();
        if (problem != null) {
            app.getPrompts().inform(window, problem);
        }
    }

    /**
     * Closes the dialog without changing anything.
     * <p>
     * 아무것도 바꾸지 않고 대화 상자를 닫는다.
     */
    void cancel() {
        dispose();
    }
}
