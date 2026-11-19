package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.HashMap;
import java.util.Map;

/**
 * The named colors of CSS Color Level 4 (148 keywords including the grey/gray spellings) and the system colors, with the values of the light color
 * scheme. <code>transparent</code> and <code>currentcolor</code> are handled by the callers.
 * <p>
 * CSS Color Level 4 의 이름 있는 색(grey/gray 철자를 포함한 148개 키워드)과 시스템 색(밝은 색 구성표의 값). <code>transparent</code> 와
 * <code>currentcolor</code> 는 호출하는 쪽이 처리한다.
 */
public final class NamedColors {

    /** Name to 0xRRGGBB. / 이름에서 0xRRGGBB 로. */
    private static final Map<String, Integer> COLORS = new HashMap<String, Integer>();

    /** System color name (lower case) to 0xRRGGBB. / 시스템 색 이름(소문자)에서 0xRRGGBB 로. */
    private static final Map<String, Integer> SYSTEM = new HashMap<String, Integer>();

    /** The color table as "name=rrggbb" pairs. / "이름=rrggbb" 쌍의 색 표. */
    private static final String TABLE = "aliceblue=f0f8ff;antiquewhite=faebd7;aqua=00ffff;aquamarine=7fffd4;azure=f0ffff;beige=f5f5dc;"
            + "bisque=ffe4c4;black=000000;blanchedalmond=ffebcd;blue=0000ff;blueviolet=8a2be2;brown=a52a2a;burlywood=deb887;"
            + "cadetblue=5f9ea0;chartreuse=7fff00;chocolate=d2691e;coral=ff7f50;cornflowerblue=6495ed;cornsilk=fff8dc;crimson=dc143c;"
            + "cyan=00ffff;darkblue=00008b;darkcyan=008b8b;darkgoldenrod=b8860b;darkgray=a9a9a9;darkgreen=006400;darkgrey=a9a9a9;"
            + "darkkhaki=bdb76b;darkmagenta=8b008b;darkolivegreen=556b2f;darkorange=ff8c00;darkorchid=9932cc;darkred=8b0000;"
            + "darksalmon=e9967a;darkseagreen=8fbc8f;darkslateblue=483d8b;darkslategray=2f4f4f;darkslategrey=2f4f4f;"
            + "darkturquoise=00ced1;darkviolet=9400d3;deeppink=ff1493;deepskyblue=00bfff;dimgray=696969;dimgrey=696969;"
            + "dodgerblue=1e90ff;firebrick=b22222;floralwhite=fffaf0;forestgreen=228b22;fuchsia=ff00ff;gainsboro=dcdcdc;"
            + "ghostwhite=f8f8ff;gold=ffd700;goldenrod=daa520;gray=808080;green=008000;greenyellow=adff2f;grey=808080;"
            + "honeydew=f0fff0;hotpink=ff69b4;indianred=cd5c5c;indigo=4b0082;ivory=fffff0;khaki=f0e68c;lavender=e6e6fa;"
            + "lavenderblush=fff0f5;lawngreen=7cfc00;lemonchiffon=fffacd;lightblue=add8e6;lightcoral=f08080;lightcyan=e0ffff;"
            + "lightgoldenrodyellow=fafad2;lightgray=d3d3d3;lightgreen=90ee90;lightgrey=d3d3d3;lightpink=ffb6c1;lightsalmon=ffa07a;"
            + "lightseagreen=20b2aa;lightskyblue=87cefa;lightslategray=778899;lightslategrey=778899;lightsteelblue=b0c4de;"
            + "lightyellow=ffffe0;lime=00ff00;limegreen=32cd32;linen=faf0e6;magenta=ff00ff;maroon=800000;mediumaquamarine=66cdaa;"
            + "mediumblue=0000cd;mediumorchid=ba55d3;mediumpurple=9370db;mediumseagreen=3cb371;mediumslateblue=7b68ee;"
            + "mediumspringgreen=00fa9a;mediumturquoise=48d1cc;mediumvioletred=c71585;midnightblue=191970;mintcream=f5fffa;"
            + "mistyrose=ffe4e1;moccasin=ffe4b5;navajowhite=ffdead;navy=000080;oldlace=fdf5e6;olive=808000;olivedrab=6b8e23;"
            + "orange=ffa500;orangered=ff4500;orchid=da70d6;palegoldenrod=eee8aa;palegreen=98fb98;paleturquoise=afeeee;"
            + "palevioletred=db7093;papayawhip=ffefd5;peachpuff=ffdab9;peru=cd853f;pink=ffc0cb;plum=dda0dd;powderblue=b0e0e6;"
            + "purple=800080;rebeccapurple=663399;red=ff0000;rosybrown=bc8f8f;royalblue=4169e1;saddlebrown=8b4513;salmon=fa8072;"
            + "sandybrown=f4a460;seagreen=2e8b57;seashell=fff5ee;sienna=a0522d;silver=c0c0c0;skyblue=87ceeb;slateblue=6a5acd;"
            + "slategray=708090;slategrey=708090;snow=fffafa;springgreen=00ff7f;steelblue=4682b4;tan=d2b48c;teal=008080;"
            + "thistle=d8bfd8;tomato=ff6347;turquoise=40e0d0;violet=ee82ee;wheat=f5deb3;white=ffffff;whitesmoke=f5f5f5;"
            + "yellow=ffff00;yellowgreen=9acd32";

    /** The system color table as "name=rrggbb" pairs. / "이름=rrggbb" 쌍의 시스템 색 표. */
    private static final String SYSTEM_TABLE = "canvas=ffffff;canvastext=000000;linktext=0000ee;visitedtext=551a8b;activetext=ff0000;"
            + "buttonface=efefef;buttontext=000000;buttonborder=767676;field=ffffff;fieldtext=000000;highlight=3584e4;"
            + "highlighttext=ffffff;selecteditem=3584e4;selecteditemtext=ffffff;mark=ffff00;marktext=000000;graytext=808080;"
            + "accentcolor=3584e4;accentcolortext=ffffff;activeborder=b4b4b4;activecaption=999999;appworkspace=ababab;"
            + "background=6363ce;buttonhighlight=ffffff;buttonshadow=9e9e9e;captiontext=000000;inactiveborder=f4f7fc;"
            + "inactivecaption=bfcddb;inactivecaptiontext=434e54;infobackground=fbfcc5;infotext=000000;menu=f9f8f7;menutext=000000;"
            + "scrollbar=ffffff;threeddarkshadow=000000;threedface=efefef;threedhighlight=ffffff;threedlightshadow=ffffff;"
            + "threedshadow=000000;window=ffffff;windowframe=ccc;windowtext=000000";

    /** The system colors that differ in the dark color scheme (the others keep their light value), as "name=rrggbb" pairs. / 어두운 색 구성표에서 다른 시스템 색("이름=rrggbb" 쌍). 나머지는 밝은 값을 유지한다. */
    private static final String SYSTEM_DARK_TABLE = "canvas=121212;canvastext=ffffff;linktext=9e9eff;visitedtext=d0adf0;activetext=ff9e9e;"
            + "buttonface=6b6b6b;buttontext=ffffff;buttonborder=6b6b6b;field=3b3b3b;fieldtext=ffffff;highlight=99c8ff;highlighttext=000000;"
            + "selecteditem=99c8ff;selecteditemtext=000000;mark=665e00;marktext=ffffff;graytext=a9a9a9;window=121212;windowtext=ffffff;"
            + "menu=2d2d2d;menutext=ffffff;infotext=ffffff;infobackground=3b3b3b;threedface=6b6b6b";

    /** The dark system colors by name. / 이름별 어두운 시스템 색. */
    private static final Map<String, Integer> SYSTEM_DARK = new HashMap<String, Integer>();

    static {
        load(TABLE, COLORS);
        load(SYSTEM_TABLE, SYSTEM);
        load(SYSTEM_DARK_TABLE, SYSTEM_DARK);
    }

    /**
     * Private constructor: static table class.
     * <p>
     * 비공개 생성자: 정적 표 클래스이다.
     */
    private NamedColors() {
    }

    /**
     * Parses a "name=rrggbb;" table into a map.
     * <p>
     * "이름=rrggbb;" 형태의 표를 맵으로 읽는다.
     *
     * @param table the table text / 표 문자열
     * @param into  the target map / 대상 맵
     */
    private static void load(String table, Map<String, Integer> into) {
        String[] pairs = table.split(";");
        // 쌍마다 이름과 16진수 색을 읽는다(세 자리 줄임 표기도 허용).
        for (int i = 0; i < pairs.length; i++) {
            int eq = pairs[i].indexOf('=');
            String hex = pairs[i].substring(eq + 1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }
            into.put(pairs[i].substring(0, eq), Integer.valueOf(Integer.parseInt(hex, 16)));
        }
    }

    /**
     * Looks up a named color.
     * <p>
     * 이름 있는 색을 찾는다.
     *
     * @param name color name (case-insensitive) / 색 이름(대소문자 무시)
     * @return 0xRRGGBB, or -1 if the name is not a color / 0xRRGGBB. 색 이름이 아니면 -1
     */
    public static int lookup(String name) {
        Integer v = COLORS.get(CssKeyword.lower(name));
        return v == null ? -1 : v.intValue();
    }

    /**
     * Looks up a system color (light color scheme).
     * <p>
     * 시스템 색을 찾는다(밝은 색 구성표).
     *
     * @param name system color name (case-insensitive) / 시스템 색 이름(대소문자 무시)
     * @return 0xRRGGBB, or -1 if the name is not a system color / 0xRRGGBB. 시스템 색이 아니면 -1
     */
    public static int lookupSystem(String name) {
        Integer v = SYSTEM.get(CssKeyword.lower(name));
        return v == null ? -1 : v.intValue();
    }

    /**
     * Looks up a system color in the dark color scheme (a color that the dark scheme does not change has its light value).
     * <p>
     * 어두운 색 구성표의 시스템 색을 찾는다(어두운 구성표가 바꾸지 않는 색은 밝은 값이다).
     *
     * @param name system color name (case-insensitive) / 시스템 색 이름(대소문자 무시)
     * @return 0xRRGGBB, or -1 if the name is not a system color / 0xRRGGBB. 시스템 색이 아니면 -1
     */
    public static int lookupSystemDark(String name) {
        Integer v = SYSTEM_DARK.get(CssKeyword.lower(name));
        return v == null ? lookupSystem(name) : v.intValue();
    }
}
