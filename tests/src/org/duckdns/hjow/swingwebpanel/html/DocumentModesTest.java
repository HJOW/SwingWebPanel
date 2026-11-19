package org.duckdns.hjow.swingwebpanel.html;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the document mode detection (plan item P07-07) and of the tag id table that the tree builder uses.
 * <p>
 * 문서 모드 판정(계획 P07-07)과 트리 생성기가 쓰는 태그 번호 표의 시험.
 */
public class DocumentModesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DocumentModesTest() {
    }

    /**
     * Checks one DOCTYPE.
     * <p>
     * DOCTYPE 하나를 확인한다.
     *
     * @param expected expected mode / 기대하는 모드
     * @param name     name / 이름
     * @param pub      public id / 공개 식별자
     * @param sys      system id / 시스템 식별자
     */
    private static void mode(int expected, String name, String pub, String sys) {
        Check.equal(expected, DocumentModes.fromDoctype(name, pub, sys, false), "mode of " + name + " / " + pub + " / " + sys);
    }

    /**
     * Modern, legacy and broken DOCTYPEs.
     * <p>
     * 현대의, 옛, 깨진 DOCTYPE.
     */
    public void testModes() {
        mode(Document.NO_QUIRKS_MODE, "html", null, null);
        mode(Document.NO_QUIRKS_MODE, "html", null, "about:legacy-compat");
        mode(Document.NO_QUIRKS_MODE, "html", "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd");
        mode(Document.NO_QUIRKS_MODE, "html", "-//W3C//DTD XHTML 1.0 Strict//EN", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
        mode(Document.QUIRKS_MODE, "foo", null, null);
        mode(Document.QUIRKS_MODE, null, null, null);
        Check.equal(Document.QUIRKS_MODE, DocumentModes.fromDoctype("html", null, null, true), "force quirks");
        mode(Document.QUIRKS_MODE, "html", "-//W3C//DTD HTML 4.0 Transitional//EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//w3c//dtd html 3.2 final//en", null);
        mode(Document.QUIRKS_MODE, "html", "-//IETF//DTD HTML 2.0//EN", null);
        mode(Document.QUIRKS_MODE, "html", "HTML", null);
        mode(Document.QUIRKS_MODE, "html", "-/W3C/DTD HTML 4.0 Transitional/EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//W3O//DTD W3 HTML Strict 3.0//EN//", null);
        mode(Document.QUIRKS_MODE, "html", null, "http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd");
        mode(Document.QUIRKS_MODE, "html", "-//W3C//DTD HTML 4.01 Transitional//EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//W3C//DTD HTML 4.01 Frameset//EN", "");
        mode(Document.LIMITED_QUIRKS_MODE, "html", "-//W3C//DTD HTML 4.01 Transitional//EN", "http://www.w3.org/TR/html4/loose.dtd");
        mode(Document.LIMITED_QUIRKS_MODE, "html", "-//W3C//DTD HTML 4.01 Frameset//EN", "http://www.w3.org/TR/html4/frameset.dtd");
        mode(Document.LIMITED_QUIRKS_MODE, "html", "-//W3C//DTD XHTML 1.0 Transitional//EN", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
        mode(Document.LIMITED_QUIRKS_MODE, "html", "-//W3C//DTD XHTML 1.0 Frameset//EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//Netscape Comm. Corp.//DTD HTML//EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//Microsoft//DTD Internet Explorer 3.0 HTML//EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//SoftQuad//DTD HoTMetaL PRO 4.0::19971010::extensions to HTML 4.0//EN", null);
        mode(Document.QUIRKS_MODE, "html", "-//WebTechs//DTD Mozilla HTML//EN", null);
    }

    /**
     * Every id constant of {@link HtmlTags} names the table entry it is called after, and the categories are sensible.
     * <p>
     * {@link HtmlTags} 의 모든 번호 상수가 자기 이름의 표 항목을 가리키는지, 범주가 알맞은지 확인한다.
     *
     * @throws Exception on reflection problems / 리플렉션 문제 시
     */
    public void testTagIds() throws Exception {
        Field[] fields = HtmlTags.class.getDeclaredFields();
        int checked = 0;
        // 정수 상수를 하나씩 읽어 이름 표와 맞는지 확인한다.
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.getType() != int.class || !Modifier.isPublic(f.getModifiers()) || f.getName().equals("COUNT")) {
                continue;
            }
            int id = f.getInt(null);
            String expected = f.getName().toLowerCase().replace('_', '-');
            Check.equal(expected, HtmlTags.name(id), "name of constant " + f.getName());
            Check.equal(id, HtmlTags.id(expected), "id of name " + expected);
            checked++;
        }
        Check.equal(HtmlTags.COUNT - 1, checked, "all ids are checked");
        Check.equal(0, HtmlTags.id("blink"), "unknown name");
        Check.isTrue(HtmlTags.isSpecial(HtmlTags.DIV) && HtmlTags.isSpecial(HtmlTags.SEARCH) && HtmlTags.isSpecial(HtmlTags.KEYGEN), "special");
        Check.isFalse(HtmlTags.isSpecial(HtmlTags.SPAN) || HtmlTags.isSpecial(HtmlTags.B) || HtmlTags.isSpecial(HtmlTags.OPTION), "not special");
        Check.isTrue(HtmlTags.isFormatting(HtmlTags.NOBR) && HtmlTags.isFormatting(HtmlTags.A), "formatting");
        Check.isFalse(HtmlTags.isFormatting(HtmlTags.P), "p is not formatting");
        Check.isTrue(HtmlTags.isScopeBoundary(HtmlTags.TD) && !HtmlTags.isScopeBoundary(HtmlTags.SELECT), "scope boundary");
        Check.isTrue(HtmlTags.isImpliedEnd(HtmlTags.RTC) && !HtmlTags.isImpliedEnd(HtmlTags.TD), "implied end");
        Check.isTrue(HtmlTags.isOpenAllowedAtEnd(HtmlTags.TR) && !HtmlTags.isOpenAllowedAtEnd(HtmlTags.DIV), "open at end");
    }
}
