package org.duckdns.hjow.swingwebpanel.download;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

/**
 * The popup of one download (plan item P11-09): a non-modal dialog that shows the file name, the source address, the target path with a
 * button to choose another place (a <code>JFileChooser</code>), the progress bar (indeterminate if the size is unknown), the number of
 * bytes and the speed, a Cancel button that becomes Close when the download has ended, and a result line. Closing the window cancels a
 * running download. Every method must be called on the event dispatch thread.
 * <p>
 * 다운로드 하나의 팝업(계획 P11-09): 파일 이름, 출처 주소, 대상 경로와 다른 곳을 고르는 버튼(<code>JFileChooser</code>), 진행 막대(크기를 모르면
 * 불확정), 받은 바이트와 속도, 다운로드가 끝나면 닫기로 바뀌는 취소 버튼, 결과 줄을 보여 주는 모달이 아닌 대화상자이다. 창을 닫으면 실행 중인
 * 다운로드를 취소한다. 모든 메서드는 이벤트 디스패치 스레드에서 호출해야 한다.
 */
public class SwingDownloadDialog extends JDialog implements DownloadHandle {

    /** Serialization version id. 직렬화 버전 식별자. */
    private static final long serialVersionUID = 1L;

    /** Distance between cascaded dialogs in pixels. 겹쳐 놓은 대화상자 사이의 간격(픽셀). */
    private static final int CASCADE_STEP = 24;

    /** Number of dialogs opened so far, to cascade them. 겹쳐 놓기 위해 지금까지 연 대화상자 수. */
    private static int opened;

    /** The control to ask for cancellation and target changes. 취소와 대상 변경을 요청할 제어 객체. */
    private final transient DownloadControl control;

    /** Shows the target path. 대상 경로를 보여 준다. */
    private final JLabel targetLabel = new JLabel();

    /** Shows the progress. 진행 상황을 보여 준다. */
    private final JProgressBar bar = new JProgressBar(0, 100);

    /** Shows bytes and speed. 바이트와 속도를 보여 준다. */
    private final JLabel detailLabel = new JLabel(" ");

    /** Shows the result line. 결과 줄을 보여 준다. */
    private final JLabel resultLabel = new JLabel(" ");

    /** Shows the total size; filled in once the size is known. 전체 크기를 보여 준다. 크기를 알게 되면 채운다. */
    private final JLabel sizeLabel = new JLabel("unknown");

    /** Cancel / Close button. 취소/닫기 버튼. */
    private final JButton cancelButton = new JButton("Cancel");

    /** Button to choose another target. 다른 대상을 고르는 버튼. */
    private final JButton saveAsButton = new JButton("Save as...");

    /** True once the download ended. 다운로드가 끝났으면 true. */
    private boolean ended;

    /** Time of the first progress report. 첫 진행 보고 시각. */
    private long startMillis;

    /**
     * Creates and shows the dialog.
     * <p>
     * 대화상자를 만들어 보여 준다.
     *
     * @param parent  the window the dialog belongs to, may be null / 대화상자가 속한 창. null 가능
     * @param request what is downloaded / 내려받는 대상
     * @param control control to cancel or change the target / 취소하거나 대상을 바꾸는 제어 객체
     */
    public SwingDownloadDialog(Window parent, DownloadRequest request, DownloadControl control) {
        super(parent, "Download - " + request.getFileName(), Dialog.ModalityType.MODELESS);
        this.control = control;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        addRow(info, 0, "File:", label(request.getFileName(), request.getFileName()));
        addRow(info, 1, "From:", label(shorten(request.getUrl(), 70), request.getUrl()));
        addRow(info, 2, "Save to:", targetLabel);
        // 요청 때 크기를 알면 그 크기를, 아니면 "unknown" 을 보여 주고 응답 머리를 받아 크기를 알게 되면 진행 보고에서 고친다.
        if (request.getLength() >= 0) {
            sizeLabel.setText(format(request.getLength()));
        }
        addRow(info, 3, "Size:", sizeLabel);
        JPanel progress = new JPanel(new BorderLayout(0, 4));
        progress.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        bar.setStringPainted(true);
        progress.add(bar, BorderLayout.NORTH);
        progress.add(detailLabel, BorderLayout.CENTER);
        progress.add(resultLabel, BorderLayout.SOUTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveAsButton);
        buttons.add(cancelButton);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(info, BorderLayout.NORTH);
        getContentPane().add(progress, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        saveAsButton.addActionListener(new ActionListener() {
            /**
             * Lets the user choose another place.
             * <p>
             * 사용자가 다른 곳을 고르게 한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                DownloadControl c = SwingDownloadDialog.this.control;
                // 선택기가 열려 있는 동안 전송이 끝나도 파일을 옮기지 않고 사용자의 선택을 기다리게 한다.
                c.holdForChoice(true);
                try {
                    File chosen = chooseTarget(c.getTarget());
                    if (chosen != null) {
                        c.changeTarget(chosen);
                    }
                } finally {
                    c.holdForChoice(false);
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            /**
             * Cancels a running download or closes the dialog.
             * <p>
             * 실행 중인 다운로드를 취소하거나 대화상자를 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                closeOrCancel();
            }
        });
        addWindowListener(new WindowAdapter() {
            /**
             * Closing the window cancels a running download.
             * <p>
             * 창을 닫으면 실행 중인 다운로드를 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosing(WindowEvent e) {
                closeOrCancel();
            }
        });
        bar.setIndeterminate(request.getLength() < 0);
        pack();
        setLocationRelativeTo(parent);
        int step = (opened++ % 8) * CASCADE_STEP;
        setLocation(getX() + step, getY() + step);
        setVisible(true);
    }

    /**
     * Cancels the download if it is running, otherwise closes the dialog.
     * <p>
     * 실행 중이면 다운로드를 취소하고 아니면 대화상자를 닫는다.
     */
    private void closeOrCancel() {
        if (ended) {
            dispose();
        } else {
            control.cancel();
            cancelButton.setEnabled(false);
        }
    }

    /**
     * Creates a value label with an optional tool tip.
     * <p>
     * 도구 설명이 있을 수 있는 값 레이블을 만든다.
     *
     * @param text    the text / 텍스트
     * @param tooltip the tool tip or null / 도구 설명 또는 null
     * @return the label / 레이블
     */
    private static JLabel label(String text, String tooltip) {
        JLabel l = new JLabel(text);
        l.setToolTipText(tooltip);
        return l;
    }

    /**
     * Adds a captioned row to the information panel.
     * <p>
     * 정보 패널에 제목이 붙은 줄을 더한다.
     *
     * @param p       the panel / 패널
     * @param row     row index / 줄 번호
     * @param caption the caption / 제목
     * @param value   the value component / 값 컴포넌트
     */
    private static void addRow(JPanel p, int row, String caption, JLabel value) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(2, 0, 2, 8);
        c.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(caption), c);
        GridBagConstraints v = new GridBagConstraints();
        v.gridy = row;
        v.gridx = 1;
        v.weightx = 1;
        v.fill = GridBagConstraints.HORIZONTAL;
        v.anchor = GridBagConstraints.WEST;
        p.add(value, v);
    }

    /**
     * Shortens a long text in the middle.
     * <p>
     * 긴 텍스트를 가운데에서 줄인다.
     *
     * @param s   the text / 텍스트
     * @param max the longest length / 최대 길이
     * @return the text, shortened with an ellipsis / 말줄임표로 줄인 텍스트
     */
    private static String shorten(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        int head = max / 2;
        return s.substring(0, head) + "..." + s.substring(s.length() - (max - head - 3));
    }

    /**
     * Formats a number of bytes for people.
     * <p>
     * 바이트 수를 사람이 읽기 쉽게 표시한다.
     *
     * @param bytes the number / 수
     * @return text such as 1.5 MB / 1.5 MB 같은 텍스트
     */
    static String format(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double v = bytes;
        String[] units = { "KB", "MB", "GB", "TB" };
        int u = -1;
        // 1024 미만이 될 때까지 단위를 올린다.
        while (v >= 1024 && u < units.length - 1) {
            v /= 1024;
            u++;
        }
        return String.format(java.util.Locale.ENGLISH, "%.1f %s", Double.valueOf(v), units[u]);
    }

    /**
     * Shows the target path.
     * <p>
     * 대상 경로를 보여 준다.
     *
     * @param target the target file / 대상 파일
     */
    public void setTarget(File target) {
        targetLabel.setText(shorten(target.getPath(), 70));
        targetLabel.setToolTipText(target.getPath());
    }

    /**
     * Shows the progress bar and the byte counts.
     * <p>
     * 진행 막대와 바이트 수를 보여 준다.
     *
     * @param done  bytes received / 받은 바이트
     * @param total total bytes or -1 / 전체 바이트 또는 -1
     */
    public void setProgress(long done, long total) {
        long now = System.currentTimeMillis();
        if (startMillis == 0) {
            startMillis = now;
        }
        long elapsed = Math.max(1, now - startMillis);
        String speed = format(done * 1000 / elapsed) + "/s";
        if (total >= 0) {
            // 응답을 받아 전체 크기를 알게 되면 크기 줄도 같은 값으로 고친다.
            sizeLabel.setText(format(total));
            int percent = total == 0 ? 100 : (int) Math.min(100, done * 100 / total);
            bar.setIndeterminate(false);
            bar.setValue(percent);
            bar.setString(percent + "%");
            detailLabel.setText(format(done) + " of " + format(total) + " - " + speed);
        } else {
            bar.setIndeterminate(true);
            // 크기를 모르면 백분율이 없으므로 막대에 글을 쓰지 않는다(null 이면 "0%" 가 그려진다).
            bar.setString("");
            detailLabel.setText(format(done) + " - " + speed);
        }
    }

    /**
     * Shows the state: the result line and the buttons.
     * <p>
     * 상태를 보여 준다: 결과 줄과 버튼.
     *
     * @param state   the state / 상태
     * @param message the result line / 결과 줄
     */
    public void setState(DownloadState state, String message) {
        resultLabel.setText(message);
        if (state == DownloadState.COMPLETED || state == DownloadState.FAILED || state == DownloadState.CANCELLED) {
            ended = true;
            bar.setIndeterminate(false);
            if (state == DownloadState.COMPLETED) {
                bar.setValue(100);
                bar.setString("100%");
            }
            cancelButton.setText("Close");
            cancelButton.setEnabled(true);
            saveAsButton.setEnabled(false);
        }
    }

    /**
     * Lets the user choose another target file with a file chooser.
     * <p>
     * 파일 선택기로 사용자가 다른 대상 파일을 고르게 한다.
     *
     * @param current the current target / 현재 대상
     * @return the chosen file or null / 고른 파일 또는 null
     */
    public File chooseTarget(File current) {
        JFileChooser chooser = new JFileChooser(current == null ? null : current.getParentFile());
        chooser.setDialogTitle("Save as");
        if (current != null) {
            chooser.setSelectedFile(current);
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Asks whether an existing file may be replaced.
     * <p>
     * 기존 파일을 바꿔도 되는지 묻는다.
     *
     * @param existing the existing file / 기존 파일
     * @return true to replace / 바꾸려면 true
     */
    public boolean confirmOverwrite(File existing) {
        return JOptionPane.showConfirmDialog(this, existing.getPath() + " already exists.\nDo you want to replace it?", "Replace file",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
