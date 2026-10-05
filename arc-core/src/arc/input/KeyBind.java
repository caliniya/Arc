package arc.input;

import arc.struct.*;
import arc.util.*;

import static arc.Core.*;

public class KeyBind{
    public static final Ar<KeyBind> all = new Ar<>();

    public final String name;
    public final KeybindValue defaultValue;
    public final @Nullable String category;
    public Axis value;

    /**
     * Registers a new key binding.
     * <p>
     * 注册一个新的按键绑定。
     * @param category Name of the category in the list of keybinds. This should be your mod name. In a bundle, it uses the key `category.{name}.name`. 按键绑定列表中类别的名称。这应该是你的模组名称。在 bundle 中使用键 `category.{name}.name`。
     * @param name Unique name of the keybind. 按键绑定的唯一名称。
     * @param defaultValue The default value for this key; can be an Axis or a KeyCode. 此按键的默认值;可以是 Axis 或 KeyCode。
     */
    public static KeyBind add(String name, KeybindValue defaultValue, String category){
        return new KeyBind(name, defaultValue, category);
    }

    /**
     * Registers a new key binding without a category. Not for use in mods. Use the constructor with a category.
     * <p>
     * 注册一个没有类别的按键绑定。不供模组使用。请使用带类别的构造函数。
     * @param name Unique name of the keybind. 按键绑定的唯一名称。
     * @param defaultValue The default value for this key; can be an Axis or a KeyCode. 此按键的默认值;可以是 Axis 或 KeyCode。
     */
    public static KeyBind add(String name, KeybindValue defaultValue){
        return new KeyBind(name, defaultValue, null);
    }

    public static void resetAll(){
        for(KeyBind def : all){
            def.resetToDefault();
        }
    }

    protected KeyBind(String name, KeybindValue defaultValue, String category){
        this.name = name;
        this.defaultValue = defaultValue;
        this.category = category;
        this.value = defaultValue instanceof Axis ? (Axis)defaultValue : new Axis((KeyCode)defaultValue);

        all.add(this);

        load();
    }

    /**
     * Saves this keybind to Settings. Call after modifying the value.
     * 将此按键绑定保存到 Settings。修改值后调用。
     */
    public void save(){
        String name = settingsKey();
        settings.put(name + "-single", value.key != null);

        if(value.key != null){
            settings.put(name + "-key", value.key.ordinal());
        }else{
            settings.put(name + "-min", value.min.ordinal());
            settings.put(name + "-max", value.max.ordinal());
        }
    }

    /**
     * Loads this keybind from settings. Calling this manually should not be necessary in most cases.
     * 从设置中加载此按键绑定。大多数情况下无需手动调用。
     */
    public void load(){
        if(settings == null) return; //headless usage
        // 无头模式使用

        Axis loaded;
        String name = settingsKey();
        if(settings.getBool(name + "-single", true)){
            int ordinal = settings.getInt(name + "-key", -1);
            loaded = ordinal < 0 ? null : new Axis(KeyCode.byOrdinal(ordinal));
        }else{
            KeyCode min = KeyCode.byOrdinal(settings.getInt(name + "-min", KeyCode.unset.ordinal()));
            KeyCode max = KeyCode.byOrdinal(settings.getInt(name + "-max", KeyCode.unset.ordinal()));
            loaded = min == KeyCode.unset || max == KeyCode.unset ? null : new Axis(min, max);
        }

        if(loaded != null){
            value = loaded;
        }
    }

    public void resetToDefault(){
        String name = settingsKey();
        settings.remove(name + "-single");
        settings.remove(name + "-key");
        settings.remove(name + "-min");
        settings.remove(name + "-max");

        if(defaultValue instanceof Axis){
            if(((Axis)defaultValue).min == null){
                value = new Axis(((Axis)defaultValue).key);
            }else{
                value = new Axis(((Axis)defaultValue).min, ((Axis)defaultValue).max);
            }
        }else{
            value = new Axis((KeyCode)defaultValue);
        }
    }

    public void unset(){
        value = new Axis(KeyCode.unset);
    }

    public boolean isDefault(){
        if(defaultValue instanceof Axis){
            if(((Axis)defaultValue).min == null){
                return ((Axis)defaultValue).key == value.key;
            }else{
                return ((Axis)defaultValue).max == value.max && ((Axis)defaultValue).min == value.min;
            }
        }else{
            return defaultValue == value.key;
        }
    }

    public boolean isUnset(){
        return value.key == KeyCode.unset;
    }

    String settingsKey(){
        return "keybind-default-keyboard-" + name;
    }

    /**
     * Represents an Axis or a KeyCode.
     * 表示一个 Axis 或 KeyCode。
     */
    public interface KeybindValue{}

    public static class Axis implements KeybindValue{
        public @Nullable KeyCode min, max;
        public @Nullable KeyCode key;

        /**
         * Cosntructor for axis-type keys only.
         * 仅用于轴类型按键的构造函数。
         */
        public Axis(KeyCode key){
            this.key = key;
            this.min = max = null;
        }

        /**
         * Constructor for keyboards/mice, or multiple buttons on a controller.
         * 用于键盘/鼠标,或手柄上多个按钮的构造函数。
         */
        public Axis(KeyCode min, KeyCode max){
            this.min = min;
            this.max = max;
            this.key = null;
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Axis axis = (Axis)o;
            return min == axis.min && max == axis.max && key == axis.key;
        }
    }
}
