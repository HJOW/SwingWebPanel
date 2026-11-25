package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises the Annex B legacy collection in the DOM binding.
 * <p>
 * DOM 바인딩에서 부속서 B의 레거시 컬렉션을 검증한다.
 */
public final class HtmlAllCollectionTest {

    /** A document with id and name overlaps. / id와 name이 겹치는 문서. */
    private static final String PAGE = "<!doctype html><title>legacy</title><input id=duplicate name=first>"
            + "<div name=ignored></div><form name=duplicate></form><div id=item></div>"
            + "<div id=01></div><span id=undefined></span>";

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HtmlAllCollectionTest() {
    }

    /**
     * Checks the three legacy language operations without affecting identity, callability, or nullish operations.
     * <p>
     * 레거시 언어 연산 세 곳을 확인하고 동일성, 호출 가능 여부와 nullish 연산을 검증한다.
     */
    public void testLegacyLanguageSemantics() {
        DomTestEnv env = new DomTestEnv(PAGE);
        Check.equal("undefined,false,true,true,true,true,false,false,true,true,true", env.eval(
                "var a=document.all; [typeof a,Boolean(a),a==null,null==a,a==undefined,undefined==a,"
                + "a===undefined,a==false,a===document.all,(a??42)===a,({_a:1})!==a].join()"), "legacy slot is selective");
        Check.equal("function,true,false,HTML,undefined", env.eval(
                "var p=new Proxy(a,{}); [typeof p,Boolean(p),p==undefined,p(0).tagName,typeof a.call].join()"),
                "proxies retain callability without inheriting the legacy slot");
        Check.equal("[object HTMLAllCollection],true,INPUT,TypeError", env.eval(
                "var error; try { new a() } catch (e) { error=e.name }"
                + "[Object.prototype.toString.call(a),a instanceof HTMLAllCollection,"
                + "Function.prototype.call.call(a,null,'first').tagName,error].join()"), "prototype and custom call");
    }

    /**
     * Checks indexed access, ordered names and live duplicate results.
     * <p>
     * 인덱스 접근과 이름 순서, 중복 이름의 라이브 결과를 확인한다.
     */
    public void testIndexedAndNamedAccess() {
        DomTestEnv env = new DomTestEnv(PAGE);
        Check.equal("HTML,HEAD,FORM,DIV,true,true,true,true,true,true", env.eval(
                "var a=document.all, both=a.duplicate; [a[0].tagName,a.item(1).tagName,both[1].tagName,"
                + "a('01').tagName,both instanceof HTMLCollection,a.namedItem('ignored')===null,a.namedItem('item').id==='item',"
                + "a.item()===null,a.item(undefined)===null,a.namedItem(undefined).id==='undefined'].join()"),
                "index, name, and optional DOMString lookup");
        Check.equal("duplicate,first,01,undefined,extra|true,false,false,true,false,true", env.eval(
                "a.extra=1; var d=Object.getOwnPropertyDescriptor(a,'first');"
                + "Reflect.ownKeys(a).slice(a.length).join()+'|'+[Object.keys(a).indexOf('first')===-1,"
                + "d.writable,d.enumerable,d.configurable,delete a.first,delete a[200]].join()"),
                "indices and names are ordered before expandos and named entries are unenumerable");
        Check.equal("1,INPUT,SELECT,true,true", env.eval(
                "both[1].remove(); var current=both.length; var e=document.createElement('select');"
                + "e.name='newName'; document.body.appendChild(e);"
                + "[current,a.duplicate.tagName,a.newName.tagName,a[a.length-1]===e,"
                + "Reflect.ownKeys(a).indexOf('newName')>=0].join()"), "collection follows tree mutations");
    }
}
