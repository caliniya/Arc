package arc.scene.flabel;

/**
 * Simple listener for fancy label events.
 * 花式标签(fancy label)事件的简单监听器。
 */
public interface FListener{

    /**
     * Called each time an {@code EVENT} token is processed.
     * <p>
     * 每次处理 {@code EVENT} 令牌时调用。
     *
     * @param event Name of the event specified in the token. e.g. <tt>{EVENT=player_name}</tt> will have 事件名称,取自令牌中指定的名称。例如 <tt>{EVENT=player_name}</tt> 的事件名即为 <tt>player_name</tt>。
     * <tt>player_name</tt> as argument.
     */
    default void event(String event){
    }

    /**
     * Called when the char progression reaches the end.
     * 当字符推进到达末尾时调用。
     */
    default void end(){
    }

    /**
     * Called when variable tokens are replaced in text. This is an alternative method to deal with variables, other
     * than directly assigning replacement values to the label. Replacements returned by this method have priority over
     * direct values, unless {@code null} is returned.
     * <p>
     * 当文本中的变量令牌被替换时调用。这是处理变量的另一种方式,无需直接为标签指定替换值。此方法返回的替换值优先于直接设置的值,除非返回 {@code null}。
     *
     * @param variable The variable name assigned to the <tt>{VAR}</tt> token. For example, in <tt>{VAR=townName}</tt>, 赋给 <tt>{VAR}</tt> 令牌的变量名。例如在 <tt>{VAR=townName}</tt> 中,变量即为 <tt>townName</tt>。
     * the variable will be <tt>townName</tt>
     * @return The replacement String, or {@code null} if this method should be ignored and the regular values should be 返回替换字符串;若返回 {@code null},则忽略此方法并改用常规值。
     * used instead.
     */
    default String replaceVariable(String variable){
        return variable;
    }

    /**
     * Called when a new character is displayed. May be called many times per frame depending on the label
     * configurations and text speed. Useful to do a certain action each time a character is displayed, like playing a
     * sound effect.
     * <p>
     * 当显示一个新字符时调用。根据标签的配置和文本速度,每帧可能会被调用多次。适合用于在每次显示字符时执行特定操作,例如播放音效。
     */
    default void onChar(char ch){
    }

}
