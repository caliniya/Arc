package arc.graphics;

import arc.struct.*;

import java.util.*;

/**
 * A general purpose class containing named colors that can be changed at will. For example, the markup language defined by the
 * {@code BitmapFontCache} class uses this class to retrieve colors. Custom colors can be defined here.
 * <p>
 * 通用类,包含可随意修改的命名颜色。例如,{@code BitmapFontCache} 类定义的标记语言就用此类来获取颜色。可以在此定义自定义颜色。
 * @author davebaol
 */
public final class Colors{

    private static final OrderedMap<String, Color> map = new OrderedMap<>();

    static{
        reset();
    }

    private Colors(){

    }

    /**
     * Returns the color map.
     * 返回颜色表。
     */
    public static OrderedMap<String, Color> getColors(){
        return map;
    }

    /**
     * Convenience method to lookup a color by {@code name}. The invocation of this method is equivalent to the expression
     * {@code Colors.getColors().get(name)}
     * <p>
     * 按 {@code name} 查找颜色的便捷方法。调用此方法等价于表达式 {@code Colors.getColors().get(name)}
     * @param name the name of the color 颜色的名称
     * @return the color to which the specified {@code name} is mapped, or {@code null} if there was no mapping for {@code name}
     * . 指定 {@code name} 映射到的颜色,若 {@code name} 没有映射则为 {@code null} 。
     */
    public static Color get(String name){
        return map.get(name);
    }

    /**
     * Convenience method to add a {@code color} with its {@code name}. The invocation of this method is equivalent to the
     * expression {@code Colors.getColors().put(name, color)}
     * <p>
     * 添加 {@code name} 对应 {@code color} 的便捷方法。调用此方法等价于表达式 {@code Colors.getColors().put(name, color)}
     * @param name the name of the color 颜色的名称
     * @param color the color 颜色
     * @return the previous {@code color} associated with {@code name}, or {@code null} if there was no mapping for {@code name}
     * . 与 {@code name} 关联的上一个 {@code color},若 {@code name} 没有映射则为 {@code null} 。
     */
    public static Color put(String name, Color color){
        return map.put(name, color);
    }

    /**
     * Resets the color map to the predefined colors.
     * 将颜色表重置为预定义颜色。
     */
    public static void reset(){
        map.clear();
        map.put("CLEAR", Color.clear);
        map.put("BLACK", Color.black);

        map.put("WHITE", Color.white);
        map.put("LIGHT_GRAY", Color.lightGray);
        map.put("GRAY", Color.gray);
        map.put("DARK_GRAY", Color.darkGray);
        map.put("LIGHT_GREY", Color.lightGray);
        map.put("GREY", Color.gray);
        map.put("DARK_GREY", Color.darkGray);

        map.put("BLUE", Color.royal); //overridden for better visuals
        // 为更好的视觉效果而重写
        map.put("NAVY", Color.navy);
        map.put("ROYAL", Color.royal);
        map.put("SLATE", Color.slate);
        map.put("SKY", Color.sky);
        map.put("CYAN", Color.cyan);
        map.put("TEAL", Color.teal);

        map.put("GREEN", Color.valueOf("38d667")); //overridden for better visuals
        // 为更好的视觉效果而重写
        map.put("ACID", Color.acid);
        map.put("LIME", Color.lime);
        map.put("FOREST", Color.forest);
        map.put("OLIVE", Color.olive);

        map.put("YELLOW", Color.yellow);
        map.put("GOLD", Color.gold);
        map.put("GOLDENROD", Color.goldenrod);
        map.put("ORANGE", Color.orange);

        map.put("BROWN", Color.brown);
        map.put("TAN", Color.tan);
        map.put("BRICK", Color.brick);

        map.put("RED", Color.valueOf("e55454")); //overridden for better visuals
        // 为更好的视觉效果而重写
        map.put("SCARLET", Color.scarlet);
        map.put("CRIMSON", Color.crimson);
        map.put("CORAL", Color.coral);
        map.put("SALMON", Color.salmon);
        map.put("PINK", Color.pink);
        map.put("MAGENTA", Color.magenta);

        map.put("PURPLE", Color.purple);
        map.put("VIOLET", Color.violet);
        map.put("MAROON", Color.maroon);

        //lowercase versions
        // 小写版本

        map.copy().each((key, val) -> map.put(key.toLowerCase(Locale.ROOT).replace("_", ""), val));
    }

}
