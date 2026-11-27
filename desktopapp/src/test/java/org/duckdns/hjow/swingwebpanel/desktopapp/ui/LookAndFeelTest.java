package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.SettingsStore;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.TextFiles;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The choice of the Swing look and feel (plan items P31-12 and P31-13): the content section of the settings dialog offers the installed
 * ones, OK switches every window of the running program to the chosen one and saves it, the program starts with the saved one or else
 * with the default, Nimbus, and one that cannot be used leaves the look and feel as it is.
 * <p>
 * Swing 룩앤필 선택(계획 P31-12, P31-13): 설정 대화 상자의 콘텐츠 구역이 설치된 룩앤필을 제안하고, 확인은 실행 중인 프로그램의 모든 창을 고른 것으로
 * 바꾸고 저장한다. 프로그램은 저장된 것, 없으면 기본값인 Nimbus 로 시작하며, 쓸 수 없는 것을 고르면 룩앤필은 그대로이다.
 */
public class LookAndFeelTest extends WindowCase {

    /**
     * A look and feel that says the platform does not support it.
     * <p>
     * 플랫폼이 지원하지 않는다고 답하는 룩앤필.
     */
    public static final class Unsupported extends MetalLookAndFeel {

        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates the look and feel (Swing needs the public constructor).
         * <p>
         * 룩앤필을 만든다(Swing 이 공개 생성자를 필요로 한다).
         */
        public Unsupported() {
        }

        /**
         * Says that this look and feel is not supported.
         * <p>
         * 이 룩앤필은 지원되지 않는다고 답한다.
         *
         * @return false / false
         */
        public boolean isSupportedLookAndFeel() {
            return false;
        }
    }

    /**
     * A look and feel that fails after Swing has already put it in place.
     * <p>
     * Swing 이 이미 끼워 넣은 뒤에 실패하는 룩앤필.
     */
    public static final class Broken extends MetalLookAndFeel {

        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates the look and feel (Swing needs the public constructor).
         * <p>
         * 룩앤필을 만든다(Swing 이 공개 생성자를 필요로 한다).
         */
        public Broken() {
        }

        /**
         * Fails instead of giving the defaults.
         * <p>
         * 기본값을 주지 않고 실패한다.
         *
         * @return nothing / 없음
         */
        public UIDefaults getDefaults() {
            throw new IllegalStateException("a broken look and feel");
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LookAndFeelTest() {
    }

    /**
     * Closes the windows and puts the look and feel back that Java started with, so that the other suites are not affected.
     * <p>
     * 창을 닫고 Java 가 처음에 쓴 룩앤필로 되돌려, 다른 스위트가 영향을 받지 않게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            onEdt(new Runnable() {
                /** Restores the look and feel. / 룩앤필을 되돌린다. */
                public void run() {
                    LookAndFeels.apply(LookAndFeels.initialClassName());
                }
            });
        }
    }

    /**
     * Returns an installed look and feel that differs from the default of the application, the one Java started with if it differs.
     * <p>
     * 응용 프로그램의 기본값과 다른 설치된 룩앤필을 반환한다. Java 가 처음에 쓴 것이 다르면 그것이다.
     *
     * @return the class name, or null if there is no other one / 클래스 이름. 다른 것이 없으면 null
     */
    private static String another() {
        UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
        String found = null;
        // 기본값과 다른 것을 찾되 Java 가 처음에 쓴 것을 우선한다.
        for (int i = 0; i < installed.length; i++) {
            if (!installed[i].getClassName().equals(LookAndFeels.defaultClassName())
                    && (found == null || installed[i].getClassName().equals(LookAndFeels.initialClassName()))) {
                found = installed[i].getClassName();
            }
        }
        return found;
    }

    /**
     * Finds a component by its name.
     * <p>
     * 이름으로 컴포넌트를 찾는다.
     *
     * @param root the container to search / 찾을 컨테이너
     * @param name the component name / 컴포넌트 이름
     * @return the component, or null / 컴포넌트. 없으면 null
     */
    private static Component named(Container root, String name) {
        Component[] children = root.getComponents();
        // 자식마다 이름을 비교하고 컨테이너이면 안을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (name.equals(children[i].getName())) {
                return children[i];
            }
            if (children[i] instanceof Container) {
                Component inner = named((Container) children[i], name);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * Presses the OK button of a settings dialog.
     * <p>
     * 설정 대화 상자의 확인 단추를 누른다.
     *
     * @param dialog the dialog / 대화 상자
     */
    private static void ok(SettingsDialog dialog) {
        ((AbstractButton) named(dialog, "ok")).doClick(0);
    }

    /**
     * Checks that the window of the case and its tabs look like new components of the look and feel in use, and that the borders that
     * the application chose stayed.
     * <p>
     * 사례의 창과 그 탭이 지금 룩앤필의 새 컴포넌트와 같은 모양이고, 응용 프로그램이 고른 테두리가 남아 있는지 확인한다.
     *
     * @param when what happened before / 그 전에 일어난 일
     */
    private void checkWindowLook(String when) {
        Check.equal(new JMenuBar().getUI().getClass(), window.getJMenuBar().getUI().getClass(), "the menu bar " + when);
        String family = new JLabel().getFont().getFamily();
        // 탭마다 탭 머리가 제자리에 있고, 새 룩앤필의 글꼴을 쓰며, 스크롤 창에 테두리가 생기지 않았는지 본다.
        for (int i = 0; i < window.getTabs().size(); i++) {
            TabHeader header = window.getTabHeader(i);
            Check.isTrue(SwingUtilities.isDescendingFrom(header, window), "the header of tab " + i + " stays in the window " + when);
            JLabel title = (JLabel) header.getComponent(0);
            Check.equal(family, title.getFont().getFamily(), "the title of tab " + i + " has the font of the look and feel " + when);
            Check.isFalse(title.getFont().isBold() || title.getFont().isItalic(), "the title of a loaded tab is plain " + when);
            JScrollPane scroll = (JScrollPane) window.getTabs().get(i).getComponent();
            Insets edges = scroll.getBorder() == null ? new Insets(0, 0, 0, 0) : scroll.getBorder().getBorderInsets(scroll);
            Check.equal(new Insets(0, 0, 0, 0), edges, "the page of tab " + i + " gets no border " + when);
            Check.equal(new Insets(2, 2, 2, 2), header.getCloseButton().getInsets(), "the close button of tab " + i + " keeps its small border " + when);
        }
    }

    /**
     * Draws a component into an image, so that a look and feel that cannot paint a component fails the case.
     * <p>
     * 컴포넌트를 이미지에 그린다. 컴포넌트를 그리지 못하는 룩앤필이면 사례가 실패한다.
     *
     * @param root the component to draw / 그릴 컴포넌트
     * @return the rendered image / 그린 이미지
     */
    private static BufferedImage paint(Component root) {
        BufferedImage image = new BufferedImage(Math.max(1, root.getWidth()), Math.max(1, root.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            root.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Compares the navigation bar and tab strip after switching to Nimbus with a window created under Nimbus.
     * <p>
     * Nimbus로 바꾼 탐색 도구 모음과 탭 줄을 처음부터 Nimbus로 만든 창과 비교한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNimbusNavigationAndTabsMatchNewWindow() throws Exception {
        if (skipped()) {
            return;
        }
        String installedNimbus = null;
        // JRE마다 Nimbus 클래스 이름이 다르므로 설치 목록에서 찾는다.
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                installedNimbus = info.getClassName();
                break;
            }
        }
        if (installedNimbus == null) {
            Check.skip("Nimbus is not installed");
            return;
        }
        final String nimbus = installedNimbus;
        onEdt(new Runnable() {
            /** Compares the two layouts and paint results. / 두 창의 배치와 그리기 결과를 비교한다. */
            public void run() {
                app.getSettings().set(Settings.LOOK_AND_FEEL, MetalLookAndFeel.class.getName());
                window.newTab(null, true);
                window.addNotify();
                window.validate();
                app.getSettings().set(Settings.LOOK_AND_FEEL, nimbus);
                Check.equal(nimbus, LookAndFeels.currentClassName(), "Nimbus is in use");
                Check.equal(window.getTabs().get(1), window.getActiveTab(), "the active tab remains selected");
                BrowserWindow fresh = app.openWindow(null);
                fresh.newTab(null, true);
                fresh.setSize(window.getSize());
                fresh.addNotify();
                try {
                    Check.isTrue(Arrays.equals(navigationAndTabPixels(fresh), navigationAndTabPixels(window)),
                            "changing to Nimbus draws the navigation bar and tabs like a new Nimbus window");
                } finally {
                    fresh.closeWindow();
                }
            }
        });
    }

    /**
     * Draws the window and reads the pixels from the navigation bar through the tab row.
     * <p>
     * 창을 그린 뒤 탐색 도구 모음부터 탭 줄 끝까지의 픽셀을 읽는다.
     *
     * @param browser the window to lay out / 배치할 창
     * @return the painted pixels above the page / 페이지 위쪽의 그린 픽셀
     */
    private static int[] navigationAndTabPixels(BrowserWindow browser) {
        browser.validate();
        JTabbedPane pane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, browser.getTabHeader(0));
        Container content = browser.getContentPane();
        Rectangle tab = SwingUtilities.convertRectangle(pane, pane.getBoundsAt(0), content);
        int height = tab.y + tab.height;
        Check.isTrue(tab.height > 0, "the tabs have been laid out");
        BufferedImage image = paint(content);
        return image.getRGB(0, 0, image.getWidth(), height, null, 0, image.getWidth());
    }

    /**
     * The content section offers the installed look and feels. Cancel changes nothing; OK switches the open windows, also hidden
     * helper windows, to the chosen one and saves it; the default brings Nimbus back.
     * <p>
     * 콘텐츠 구역은 설치된 룩앤필을 제안한다. 취소는 아무것도 바꾸지 않고, 확인은 숨겨 둔 보조 창을 포함한 열린 창을 고른 것으로 바꾸고 저장하며,
     * 기본값은 Nimbus 로 되돌린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDialogChoosesAndAppliesLookAndFeel() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("look-a.html", "Look A", "<p>one fish</p>");
        final String[] chosen = new String[1];
        final ConsoleWindow[] console = new ConsoleWindow[1];
        onEdt(new Runnable() {
            /** Opens pages, a helper window and the find bar. / 페이지, 보조 창, 찾기 막대를 연다. */
            public void run() {
                chosen[0] = another();
                // 창은 보이지 않는다(보이는 창의 설정 대화 상자는 모달이라 사례를 막는다). 배치하고 그릴 수 있게만 만든다.
                window.addNotify();
                window.navigate(a);
                window.newTab(null, false);
                console[0] = window.showConsole();
                window.perform(Commands.FIND);
            }
        });
        if (chosen[0] == null) {
            Check.skip("only one look and feel is installed");
            return;
        }
        awaitTab(0, "Look A");
        onEdt(new Runnable() {
            /** Searches for a text that is not on the page. / 페이지에 없는 글을 찾는다. */
            public void run() {
                window.getFindBar().getField().setText("whale");
            }
        });
        await(new TestPages.Condition() {
            /** The find bar reports the missing text. / 찾기 막대가 없는 글을 알린다. */
            public boolean holds() {
                return "No matches".equals(window.getFindBar().getCountText());
            }
        }, "the missing text is reported");
        onEdt(new Runnable() {
            /** Uses the dialog. / 대화 상자를 쓴다. */
            public void run() {
                Check.equal(LookAndFeels.defaultClassName(), LookAndFeels.currentClassName(), "the program starts with the default look and feel");
                SettingsDialog d = window.showSettings();
                Check.equal("", d.getValue(Settings.LOOK_AND_FEEL), "the default is the empty value");
                JComboBox box = (JComboBox) named(d, Settings.LOOK_AND_FEEL);
                JTabbedPane sections = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, box);
                Check.isTrue(SwingUtilities.isDescendingFrom(box, sections.getComponentAt(sections.indexOfTab("Content"))),
                        "the list is in the content section");
                List<String> shown = new ArrayList<String>();
                // 목록의 글을 모은다.
                for (int i = 0; i < box.getItemCount(); i++) {
                    shown.add(String.valueOf(box.getItemAt(i)));
                }
                List<String> expected = new ArrayList<String>();
                expected.add("(default)");
                UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
                // 설치된 룩앤필의 이름을 순서대로 기대한다.
                for (int i = 0; i < installed.length; i++) {
                    expected.add(installed[i].getName());
                }
                Check.equal(expected, shown, "the list offers the default and the installed look and feels");
                d.setValue(Settings.LOOK_AND_FEEL, chosen[0]);
                Check.equal(LookAndFeels.defaultClassName(), LookAndFeels.currentClassName(), "nothing changes before OK");
                d.cancel();
                Check.equal(LookAndFeels.defaultClassName(), LookAndFeels.currentClassName(), "Cancel changes nothing");
                Check.equal("", app.getSettings().get(Settings.LOOK_AND_FEEL), "and no setting");

                d = window.showSettings();
                d.setValue(Settings.LOOK_AND_FEEL, chosen[0]);
                ok(d);
                Check.equal(chosen[0], LookAndFeels.currentClassName(), "OK switches the look and feel");
                Check.equal(chosen[0], app.getSettings().get(Settings.LOOK_AND_FEEL), "and writes the setting");
                Check.equal(new ArrayList<String>(), prompts.messages, "without a message");
                checkWindowLook("after the change");
                Check.equal(new JComboBox().getUI().getClass(), console[0].getLevelBox().getUI().getClass(),
                        "a helper window that is hidden changes too");
                Check.equal(new java.awt.Color(0xFFD9D9), window.getFindBar().getField().getBackground(),
                        "the find field keeps the color of a missing text");
                window.newTab(null, false);
                checkWindowLook("with a tab opened after the change");
                window.validate();
                paint(window.getRootPane());
                SettingsDialog again = window.showSettings();
                Check.equal(chosen[0], again.getValue(Settings.LOOK_AND_FEEL), "the dialog shows the chosen look and feel");
                paint(again.getRootPane());
                again.cancel();
                window.selectTab(0);
                window.getFindBar().getField().setText("fish");
            }
        });
        await(new TestPages.Condition() {
            /** The find bar reports the match. / 찾기 막대가 일치를 알린다. */
            public boolean holds() {
                return "1 / 1".equals(window.getFindBar().getCountText());
            }
        }, "the text is found");
        onEdt(new Runnable() {
            /** Goes back to the default. / 기본값으로 되돌린다. */
            public void run() {
                Check.equal(new JTextField().getBackground(), window.getFindBar().getField().getBackground(),
                        "the find field gets the background of the new look and feel");
                SettingsDialog d = window.showSettings();
                d.setValue(Settings.LOOK_AND_FEEL, "");
                ok(d);
                Check.equal(LookAndFeels.defaultClassName(), LookAndFeels.currentClassName(), "the default brings Nimbus back");
                Check.equal(new JTextField().getBackground(), window.getFindBar().getField().getBackground(),
                        "and the find field follows");
                checkWindowLook("after going back");
                window.validate();
                paint(window.getRootPane());
                app.getSettings().set(Settings.LOOK_AND_FEEL, chosen[0]);
                Check.equal(chosen[0], LookAndFeels.currentClassName(), "a change of the setting applies it");
            }
        });
        String text = TextFiles.read(new File(home, SettingsStore.FILE_NAME));
        Check.contains(text, "view.lookAndFeel=" + chosen[0] + "\n", "the settings file holds the look and feel");
    }

    /**
     * The next run of the program uses the saved look and feel from its first window on.
     * <p>
     * 프로그램의 다음 실행은 첫 창부터 저장된 룩앤필을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSavedLookAndFeelIsUsedAtStart() throws Exception {
        if (skipped()) {
            return;
        }
        onEdt(new Runnable() {
            /** Saves a choice and starts again. / 선택을 저장하고 다시 시작한다. */
            public void run() {
                String chosen = another();
                if (chosen == null) {
                    return;
                }
                app.getSettings().set(Settings.LOOK_AND_FEEL, chosen);
                // 고른 것이 아닌 기본 룩앤필로 되돌린 뒤 시작해, 시작할 때 저장된 것이 적용되는지 본다.
                LookAndFeels.apply("");
                BrowserApp next = new BrowserApp(new Runnable() {
                    /** Does nothing at the end. / 끝날 때 아무것도 하지 않는다. */
                    public void run() {
                    }
                });
                Check.equal(chosen, LookAndFeels.currentClassName(), "the saved look and feel is applied at the start");
                Check.isNull(next.takeLookAndFeelProblem(), "without a problem");
                BrowserWindow first = next.openWindow(null);
                try {
                    Check.equal(new JMenuBar().getUI().getClass(), first.getJMenuBar().getUI().getClass(), "the first window has it");
                } finally {
                    first.closeWindow();
                }
            }
        });
    }

    /**
     * Without a choice the program starts with Nimbus, also when the settings file holds the empty value that an earlier version wrote,
     * and a saved look and feel that cannot be used gives the default at the start.
     * <p>
     * 선택이 없으면 프로그램은 Nimbus 로 시작한다. 설정 파일에 이전 판이 적은 빈 값이 있어도 같으며, 저장된 룩앤필을 쓸 수 없으면 시작할 때 기본값을
     * 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNimbusIsTheDefaultAtStart() throws Exception {
        if (skipped()) {
            return;
        }
        boolean installed = false;
        // Nimbus 가 설치된 런타임인지 본다.
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            installed = installed || "Nimbus".equals(info.getName());
        }
        if (!installed) {
            Check.skip("Nimbus is not installed");
            return;
        }
        final String[] saved = { "", "no.such.LookAndFeel" };
        // 빈 값과 쓸 수 없는 값을 저장한 설정 파일로 각각 시작한다.
        for (int i = 0; i < saved.length; i++) {
            final String value = saved[i];
            TextFiles.write(new File(home, SettingsStore.FILE_NAME), "settings.version=1\nview.lookAndFeel=" + value + "\n");
            onEdt(new Runnable() {
                /** Starts the program again. / 프로그램을 다시 시작한다. */
                public void run() {
                    // 새 프로세스처럼 Java 의 처음 룩앤필에서 시작한다.
                    LookAndFeels.apply(LookAndFeels.initialClassName());
                    BrowserApp next = new BrowserApp(new Runnable() {
                        /** Does nothing at the end. / 끝날 때 아무것도 하지 않는다. */
                        public void run() {
                        }
                    });
                    Check.equal("Nimbus", UIManager.getLookAndFeel().getName(), "Nimbus is in use with the saved value '" + value + "'");
                    Check.equal(value, next.getSettings().get(Settings.LOOK_AND_FEEL), "the saved value is kept");
                    Check.equal(Boolean.valueOf(value.length() > 0), Boolean.valueOf(next.takeLookAndFeelProblem() != null),
                            "only an unusable value is a problem");
                    BrowserWindow first = next.openWindow(null);
                    try {
                        Check.equal(new JMenuBar().getUI().getClass(), first.getJMenuBar().getUI().getClass(), "the first window has it");
                    } finally {
                        first.closeWindow();
                    }
                }
            });
        }
    }

    /**
     * Every installed look and feel can be used while windows are open, and the window can be drawn with it.
     * <p>
     * 설치된 모든 룩앤필은 창이 열려 있는 동안 쓸 수 있고, 그것으로 창을 그릴 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryInstalledLookAndFeel() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("look-b.html", "Look B", "<p>text</p><form><input value='in'><select><option>one</option></select></form>");
        onEdt(new Runnable() {
            /** Opens pages. / 페이지를 연다. */
            public void run() {
                // 창은 보이지 않고, 배치하고 그릴 수 있게만 만든다.
                window.addNotify();
                window.navigate(a);
                window.newTab(null, false);
            }
        });
        awaitTab(0, "Look B");
        onEdt(new Runnable() {
            /** Applies them one after the other. / 차례로 적용한다. */
            public void run() {
                UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
                // 룩앤필마다 적용하고 창과 설정 대화 상자를 그려 본다.
                for (int i = 0; i < installed.length; i++) {
                    app.getSettings().set(Settings.LOOK_AND_FEEL, installed[i].getClassName());
                    Check.equal(installed[i].getClassName(), LookAndFeels.currentClassName(), installed[i].getName() + " is used");
                    checkWindowLook("with " + installed[i].getName());
                    window.validate();
                    paint(window.getRootPane());
                    SettingsDialog d = window.showSettings();
                    paint(d.getRootPane());
                    d.cancel();
                }
            }
        });
    }

    /**
     * A look and feel that is missing, is not one, is not supported or breaks while it is put in place leaves the look and feel as it
     * is; the setting is kept and the user is told once.
     * <p>
     * 없거나, 룩앤필이 아니거나, 지원되지 않거나, 끼워 넣는 도중에 고장 나는 룩앤필은 룩앤필을 그대로 둔다. 설정은 보존하고 사용자에게 한 번 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnusableLookAndFeelIsKeptAndReported() throws Exception {
        if (skipped()) {
            return;
        }
        onEdt(new Runnable() {
            /** Chooses look and feels that cannot be used. / 쓸 수 없는 룩앤필을 고른다. */
            public void run() {
                String initial = LookAndFeels.currentClassName();
                String[] unusable = { "no.such.LookAndFeel", String.class.getName(), Unsupported.class.getName(), Broken.class.getName() };
                // 쓸 수 없는 것마다 거부되고 쓰던 것이 남는지 본다.
                for (int i = 0; i < unusable.length; i++) {
                    Check.isFalse(LookAndFeels.apply(unusable[i]), unusable[i] + " is refused");
                    Check.equal(initial, LookAndFeels.currentClassName(), "the look and feel stays after " + unusable[i]);
                }
                paint(window.getRootPane());

                SettingsDialog d = window.showSettings();
                d.setValue(Settings.LOOK_AND_FEEL, "no.such.LookAndFeel");
                Check.equal("no.such.LookAndFeel", d.getValue(Settings.LOOK_AND_FEEL), "a look and feel that is not installed is listed");
                ok(d);
                Check.equal(initial, LookAndFeels.currentClassName(), "OK leaves the look and feel");
                Check.equal("no.such.LookAndFeel", app.getSettings().get(Settings.LOOK_AND_FEEL), "the setting is kept");
                Check.equal(1, prompts.messages.size(), "the user is told");
                Check.contains(prompts.messages.get(0), "no.such.LookAndFeel", "which look and feel it is");
                d = window.showSettings();
                Check.equal("no.such.LookAndFeel", d.getValue(Settings.LOOK_AND_FEEL), "the dialog still shows it");
                ok(d);
                app.getSettings().set(Settings.SHOW_STATUS_BAR, "false");
                Check.equal(1, prompts.messages.size(), "and the user is told only once");
                Check.isNull(app.takeLookAndFeelProblem(), "another setting does not try it again");

                BrowserApp next = new BrowserApp(new Runnable() {
                    /** Does nothing at the end. / 끝날 때 아무것도 하지 않는다. */
                    public void run() {
                    }
                });
                Check.contains(String.valueOf(next.takeLookAndFeelProblem()), "no.such.LookAndFeel", "the next start remembers the problem");
                Check.equal(LookAndFeels.defaultClassName(), LookAndFeels.currentClassName(), "and starts with the default look and feel");
            }
        });
    }
}
