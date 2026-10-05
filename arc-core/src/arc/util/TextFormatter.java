package arc.util;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * {@code TextFormatter} is used by {@link I18NBundle} to perform argument replacement.
 * <p>
 * {@code TextFormatter} 被 {@link I18NBundle} 用于执行参数替换。
 * @author davebaol
 */
public class TextFormatter{

    private MessageFormat messageFormat;
    private StringBuilder buffer;

    public TextFormatter(Locale locale, boolean useMessageFormat){
        buffer = new StringBuilder();
        if(useMessageFormat) messageFormat = new MessageFormat("", locale);
    }

    /**
     * Formats the given {@code pattern} replacing its placeholders with the actual arguments specified by {@code args}.
     * <p>
     * If this {@code TextFormatter} has been instantiated with {@link #TextFormatter(Locale, boolean) TextFormatter(locale, true)}
     * {@link MessageFormat} is used to process the pattern, meaning that the actual arguments are properly localized with the
     * locale of this {@code TextFormatter}.
     * <p>
     * On the contrary, if this {@code TextFormatter} has been instantiated with {@link #TextFormatter(Locale, boolean)
     * TextFormatter(locale, false)} pattern's placeholders are expected to be in the simplified form {0}, {1}, {2} and so on and
     * they will be replaced with the corresponding object from {@code args} converted to a string with {@code toString()}, so
     * without taking into account the locale.
     * <p>
     * In both cases, there's only one simple escaping rule, i.e. a left curly bracket must be doubled if you want it to be part of
     * your string.
     * <p>
     * It's worth noting that the rules for using single quotes within {@link MessageFormat} patterns have shown to be somewhat
     * confusing. In particular, it isn't always obvious to localizers whether single quotes need to be doubled or not. For this
     * very reason we decided to offer the simpler escaping rule above without limiting the expressive power of message format
     * patterns. So, if you're used to MessageFormat's syntax, remember that with {@code TextFormatter} single quotes never need to
     * be escaped!
     * <p>
     * 格式化给定的 {@code pattern},将其占位符替换为 {@code args} 指定的实际参数。
     * <p>
     * 若此 {@code TextFormatter} 是通过 {@link #TextFormatter(Locale, boolean) TextFormatter(locale, true)} 实例化的,则会使用 {@link MessageFormat} 处理模式,即实际参数会使用此 {@code TextFormatter} 的 locale 正确本地化。
     * <p>
     * 反之,若此 {@code TextFormatter} 是通过 {@link #TextFormatter(Locale, boolean) TextFormatter(locale, false)} 实例化的,则模式的占位符应采用 {0}、{1}、{2} 等简化形式,它们会被替换为 {@code args} 中对应的对象,并用 {@code toString()} 转换为字符串,即不考虑 locale。
     * <p>
     * 两种情况下都只有一条简单的转义规则:若希望左花括号成为字符串的一部分,必须把它写成两个。
     * <p>
     * 值得注意的是,在 {@link MessageFormat} 模式中使用单引号的规则有些令人困惑。特别是,对本地化人员来说,单引号是否需要加倍并不总是显而易见。正因如此,我们决定提供上述更简单的转义规则,同时不限制消息格式模式的表现力。所以,如果你习惯了 MessageFormat 的语法,请记住:使用 {@code TextFormatter} 时,单引号永远不需要转义!
     * @param pattern the pattern 模式
     * @param args the arguments 参数
     * @return the formatted pattern 格式化后的模式
     * @throws IllegalArgumentException if the pattern is invalid 若模式无效
     */
    public String format(String pattern, Object... args){
        if(messageFormat != null){
            messageFormat.applyPattern(replaceEscapeChars(pattern));
            return messageFormat.format(args);
        }
        return simpleFormat(pattern, args);
    }

    // This code is needed because a simple replacement like
    // 需要这段代码是因为像下面这样简单的替换
    // pattern.replace("'", "''").replace("{{", "'{'");
    // can't properly manage some special cases.
    // 无法正确处理一些特殊情况。
    // For example, the expected output for {{{{ is {{ but you get {'{ instead.
    // 例如,{{{{ 的期望输出是 {{,但实际得到的是 {'{。
    // Also this code is optimized since a new string is returned only if something has been replaced.
    // 此代码还做了优化:只有发生了替换才会返回新字符串。
    private String replaceEscapeChars(String pattern){
        buffer.setLength(0);
        boolean changed = false;
        int len = pattern.length();
        for(int i = 0; i < len; i++){
            char ch = pattern.charAt(i);
            if(ch == '\''){
                changed = true;
                buffer.append("''");
            }else if(ch == '{'){
                int j = i + 1;
                while(j < len && pattern.charAt(j) == '{')
                    j++;
                int escaped = (j - i) / 2;
                if(escaped > 0){
                    changed = true;
                    buffer.append('\'');
                    do{
                        buffer.append('{');
                    }while((--escaped) > 0);
                    buffer.append('\'');
                }
                if((j - i) % 2 != 0) buffer.append('{');
                i = j - 1;
            }else{
                buffer.append(ch);
            }
        }
        return changed ? buffer.toString() : pattern;
    }

    /**
     * Formats the given {@code pattern} replacing any placeholder of the form {0}, {1}, {2} and so on with the corresponding
     * object from {@code args} converted to a string with {@code toString()}, so without taking into account the locale.
     * <p>
     * This method only implements a small subset of the grammar supported by {@link java.text.MessageFormat}. Especially,
     * placeholder are only made up of an index; neither the type nor the style are supported.
     * <p>
     * If nothing has been replaced this implementation returns the pattern itself.
     * <p>
     * 格式化给定的 {@code pattern},将 {0}、{1}、{2} 等形式的占位符替换为 {@code args} 中对应的对象,并用 {@code toString()} 转换为字符串,即不考虑 locale。
     * <p>
     * 此方法只实现了 {@link java.text.MessageFormat} 所支持语法的一小部分。特别是,占位符仅由索引组成;不支持类型和样式。
     * <p>
     * 若没有发生任何替换,此实现将返回模式本身。
     * @param pattern the pattern 模式
     * @param args the arguments 参数
     * @return the formatted pattern 格式化后的模式
     * @throws IllegalArgumentException if the pattern is invalid 若模式无效
     */
    private String simpleFormat(String pattern, Object... args){
        buffer.setLength(0);
        boolean changed = false;
        int placeholder = -1;
        int patternLength = pattern.length();
        for(int i = 0; i < patternLength; ++i){
            char ch = pattern.charAt(i);
            if(placeholder < 0){ // processing constant part
            // 处理常量部分
                if(ch == '{'){
                    changed = true;
                    if(i + 1 < patternLength && pattern.charAt(i + 1) == '{'){
                        buffer.append(ch); // handle escaped '{'
                        // 处理转义的 '{'
                        ++i;
                    }else{
                        placeholder = 0; // switch to placeholder part
                        // 切换到占位符部分
                    }
                }else{
                    buffer.append(ch);
                }
            }else{ // processing placeholder part
            // 处理占位符部分
                if(ch == '}'){
                    if(placeholder >= args.length)
                        throw new IllegalArgumentException("Argument index out of bounds: " + placeholder);
                    if(pattern.charAt(i - 1) == '{')
                        throw new IllegalArgumentException("Missing argument index after a left curly brace");
                    if(args[placeholder] == null)
                        buffer.append("null"); // append null argument
                        // 追加 null 参数
                    else
                        buffer.append(args[placeholder].toString()); // append actual argument
                        // 追加实际参数
                    placeholder = -1; // switch to constant part
                    // 切换回常量部分
                }else{
                    if(ch < '0' || ch > '9')
                        throw new IllegalArgumentException("Unexpected '" + ch + "' while parsing argument index");
                    placeholder = placeholder * 10 + (ch - '0');
                }
            }
        }
        if(placeholder >= 0) throw new IllegalArgumentException("Unmatched braces in the pattern.");

        return changed ? buffer.toString() : pattern;
    }
}
