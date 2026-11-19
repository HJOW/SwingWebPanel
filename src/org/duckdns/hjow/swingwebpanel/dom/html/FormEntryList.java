package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * "Constructing the entry list" of a form (HTML Standard 4.10.22.4): the list of name and value pairs that a submission (and
 * <code>new FormData(form)</code>) contains. It walks the submittable elements whose form owner is the form, in tree order, and skips the
 * controls that a submission must skip (disabled controls, controls without a name, unchecked checkboxes and radio buttons, buttons other
 * than the submitter, controls inside a <code>datalist</code>). Image buttons contribute the coordinates of the click, file upload
 * controls the selected files (or an empty file), select elements their selected options, and an <code>_charset_</code> hidden input the
 * name of the encoding.
 * <p>
 * 폼의 "constructing the entry list"(HTML 표준 4.10.22.4): 제출(과 <code>new FormData(form)</code>)이 담는 이름과 값 쌍의 목록. 폼 소유자가 그 폼인 제출 가능 요소를
 * 트리 순서로 훑으며 제출이 건너뛰어야 하는 컨트롤(비활성 컨트롤, 이름 없는 컨트롤, 체크되지 않은 체크박스와 라디오 버튼, submitter 가 아닌 버튼,
 * <code>datalist</code> 안의 컨트롤)을 건너뛴다. 이미지 버튼은 클릭 좌표를, 파일 업로드 컨트롤은 선택한 파일(또는 빈 파일)을, select 는 선택한 option 을,
 * <code>_charset_</code> hidden input 은 인코딩 이름을 낸다.
 */
public final class FormEntryList {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormEntryList() {
    }

    /**
     * Constructs the entry list of a form.
     * <p>
     * 폼의 항목 목록을 만든다.
     *
     * @param form      the form / 폼
     * @param submitter the submit button that started the submission, or null (for example for <code>new FormData(form)</code>) / 제출을 시작한 제출 버튼. 없으면
     *                  null(예: <code>new FormData(form)</code>)
     * @param encoding  the canonical name of the encoding of the submission (the value of an <code>_charset_</code> input) / 제출 인코딩의 표준 이름(<code>_charset_</code> input 의 값)
     * @return the entries in tree order / 트리 순서의 항목
     */
    public static List<FormEntry> construct(HtmlFormElement form, Element submitter, String encoding) {
        List<FormEntry> out = new ArrayList<FormEntry>();
        List<HtmlListedElement> controls = form.listedElements();
        // 폼의 제출 가능 요소를 트리 순서로 보며 항목을 만든다.
        for (int i = 0; i < controls.size(); i++) {
            HtmlListedElement f = controls.get(i);
            if (!f.isSubmittable() || f.isDisabled() || insideDataList(f)) {
                continue;
            }
            if (f instanceof HtmlButtonElement) {
                if (f == submitter) {
                    addIfNamed(out, f, ((HtmlButtonElement) f).getValue());
                }
            } else if (f instanceof HtmlInputElement) {
                addInput(out, (HtmlInputElement) f, submitter, encoding);
            } else if (f instanceof HtmlSelectElement) {
                String name = f.getName();
                if (name.length() > 0) {
                    List<HtmlOptionElement> sel = ((HtmlSelectElement) f).selectedOptionList();
                    // 선택된 option 중 비활성이 아닌 것의 값을 항목으로 낸다.
                    for (int k = 0; k < sel.size(); k++) {
                        if (!sel.get(k).isDisabled()) {
                            out.add(new FormEntry(name, sel.get(k).getValue()));
                        }
                    }
                }
            } else if (f instanceof HtmlTextAreaElement) {
                addIfNamed(out, f, ((HtmlTextAreaElement) f).getSubmissionValue());
                addDirname(out, f, ((HtmlTextAreaElement) f).getValue());
            }
        }
        // 마지막 단계: 이름과 (파일이 아닌) 값의 줄 바꿈을 CRLF 로 정규화한다.
        for (int i = 0; i < out.size(); i++) {
            FormEntry e = out.get(i);
            String n = HtmlText.toCrlf(e.getName());
            if (e.isFile()) {
                if (!n.equals(e.getName())) {
                    out.set(i, new FormEntry(n, e.getFile()));
                }
            } else {
                String v = HtmlText.toCrlf(e.getValue());
                if (!n.equals(e.getName()) || !v.equals(e.getValue())) {
                    out.set(i, new FormEntry(n, v));
                }
            }
        }
        return out;
    }

    /**
     * Adds the entries of an input.
     * <p>
     * input 의 항목을 추가한다.
     *
     * @param out       receives the entries / 항목을 받는다
     * @param in        the input / input
     * @param submitter the submitter or null / submitter 또는 null
     * @param encoding  the encoding name / 인코딩 이름
     */
    private static void addInput(List<FormEntry> out, HtmlInputElement in, Element submitter, String encoding) {
        int k = in.kind();
        String name = in.getName();
        if (k == InputKinds.IMAGE) {
            if (in != submitter) {
                return;
            }
            String prefix = name.length() > 0 ? name + "." : "";
            int[] xy = in.selectedCoordinate();
            out.add(new FormEntry(prefix + "x", Integer.toString(xy[0])));
            out.add(new FormEntry(prefix + "y", Integer.toString(xy[1])));
            return;
        }
        if (k == InputKinds.SUBMIT || k == InputKinds.RESET || k == InputKinds.BUTTON) {
            if (in == submitter && k == InputKinds.SUBMIT) {
                addIfNamed(out, in, in.getValue());
            }
            return;
        }
        if ((k == InputKinds.CHECKBOX || k == InputKinds.RADIO) && !in.isChecked()) {
            return;
        }
        if (name.length() == 0) {
            return;
        }
        if (k == InputKinds.FILE) {
            List<FileEntry> files = in.getSelectedFiles();
            if (files.isEmpty()) {
                out.add(new FormEntry(name, new FileEntry("", "application/octet-stream", 0, new byte[0])));
            }
            // 선택한 파일마다 항목을 하나씩 낸다.
            for (int i = 0; i < files.size(); i++) {
                out.add(new FormEntry(name, files.get(i)));
            }
            return;
        }
        if (k == InputKinds.HIDDEN && "_charset_".equals(HtmlText.lower(name))) {
            out.add(new FormEntry(name, encoding));
            return;
        }
        out.add(new FormEntry(name, in.getValue()));
        if (InputKinds.isTextLike(k)) {
            addDirname(out, in, in.getValue());
        }
    }

    /**
     * Adds an entry if the control has a non-empty name.
     * <p>
     * 컨트롤에 비어 있지 않은 이름이 있으면 항목을 추가한다.
     *
     * @param out   receives the entry / 항목을 받는다
     * @param f     the control / 컨트롤
     * @param value the value / 값
     */
    private static void addIfNamed(List<FormEntry> out, HtmlListedElement f, String value) {
        String name = f.getName();
        if (name.length() > 0) {
            out.add(new FormEntry(name, value));
        }
    }

    /**
     * Adds the <code>dirname</code> entry of a text control: the name from the <code>dirname</code> attribute and <code>ltr</code> or
     * <code>rtl</code>.
     * <p>
     * 텍스트 컨트롤의 <code>dirname</code> 항목을 추가한다: <code>dirname</code> 속성의 이름과 <code>ltr</code> 또는 <code>rtl</code>.
     *
     * @param out   receives the entry / 항목을 받는다
     * @param f     the control / 컨트롤
     * @param value the value of the control / 컨트롤의 값
     */
    private static void addDirname(List<FormEntry> out, HtmlListedElement f, String value) {
        String dirname = f.getAttribute("dirname");
        if (dirname == null || dirname.length() == 0) {
            return;
        }
        out.add(new FormEntry(dirname, directionality(f, value)));
    }

    /**
     * Returns the directionality of a control: the nearest <code>dir</code> attribute (<code>auto</code> is decided by the first strong
     * character of the value), left-to-right by default.
     * <p>
     * 컨트롤의 방향성을 반환한다: 가장 가까운 <code>dir</code> 속성(<code>auto</code> 는 값의 첫 강한 문자로 정함). 기본은 왼쪽에서 오른쪽.
     *
     * @param f     the control / 컨트롤
     * @param value the value / 값
     * @return ltr or rtl / ltr 또는 rtl
     */
    private static String directionality(HtmlListedElement f, String value) {
        // 컨트롤에서 위로 올라가며 dir 속성을 찾는다.
        for (Node n = f; n instanceof Element; n = n.getParentNode()) {
            String d = HtmlText.lower(HtmlText.strip(((Element) n).getAttribute("dir") == null ? "" : ((Element) n).getAttribute("dir")));
            if ("ltr".equals(d) || "rtl".equals(d)) {
                return d;
            }
            if ("auto".equals(d)) {
                // 값의 문자를 앞에서부터 보며 방향성이 강한 첫 문자로 정한다.
                for (int i = 0; i < value.length(); i++) {
                    byte dir = Character.getDirectionality(value.charAt(i));
                    if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                        return "rtl";
                    }
                    if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                        return "ltr";
                    }
                }
                return "ltr";
            }
        }
        return "ltr";
    }

    /**
     * Tells whether an element has a <code>datalist</code> ancestor.
     * <p>
     * 요소에 <code>datalist</code> 조상이 있는지 알려 준다.
     *
     * @param e the element / 요소
     * @return true if inside a datalist / datalist 안이면 true
     */
    private static boolean insideDataList(Element e) {
        // 조상 중에 datalist 가 있는지 찾는다.
        for (Node p = e.getParentNode(); p != null; p = p.getParentNode()) {
            if (p instanceof HtmlDataListElement) {
                return true;
            }
        }
        return false;
    }
}
