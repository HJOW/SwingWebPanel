#!/bin/sh
# Prop.java를 PropertyTable.java의 DEFS 행에서 다시 만든다. 사용: browser/src/test/resources/css/generate-prop.sh
# 행을 더하거나 순서를 바꾼 뒤 반드시 실행하고, PropertyRegistry 의 일치 시험(PropertyTableTest)으로 확인한다.
cd "$(dirname "$0")/../../../main/java/org/duckdns/hjow/swingwebpanel/css/props" || exit 1
awk '
/static final String\[\]\[\] DEFS/ {on=1; next}
on && /^        \{"/ { match($0, /\{"[a-z0-9-]+"/); n=substr($0, RSTART+2, RLENGTH-3); names[++c]=n }
END {
  print "package org.duckdns.hjow.swingwebpanel.css.props;"
  print ""
  print "/**"
  print " * The numeric ids of the longhand properties. The constants are generated from the rows of {@link PropertyTable}: constant N is the"
  print " * N-th row, and the name is the property name in upper case with hyphens replaced by underscores. Layout and rendering code switches on"
  print " * these ids and reads them with <code>ComputedStyle.get(Prop.WIDTH)</code>."
  print " * <p>"
  print " * 개별 속성의 숫자 id. 상수는 {@link PropertyTable} 의 행에서 생성한다: 상수 N 은 N 번째 행이며 이름은 속성 이름을 대문자로 하고 하이픈을 밑줄로 바꾼 것이다."
  print " * 레이아웃과 렌더링 코드는 이 id 로 분기하고 <code>ComputedStyle.get(Prop.WIDTH)</code> 로 읽는다."
  print " */"
  print "public final class Prop {"
  print ""
  print "    /**"
  print "     * Private constructor: constants only."
  print "     * <p>"
  print "     * 비공개 생성자: 상수만 있다."
  print "     */"
  print "    private Prop() {"
  print "    }"
  print ""
  for (i = 1; i <= c; i++) {
    u = toupper(names[i]); gsub(/-/, "_", u)
    print "    /** Property id of <code>" names[i] "</code>. / <code>" names[i] "</code> 속성의 id. */"
    print "    public static final int " u " = " i ";"
  }
  print ""
  print "    /** Number of longhand properties; ids run from 1 to COUNT. / 개별 속성의 수. id 는 1 부터 COUNT 까지이다. */"
  print "    public static final int COUNT = " c ";"
  print "}"
}' PropertyTable.java > Prop.java
