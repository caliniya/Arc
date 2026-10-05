package arc.scene.flabel;

import arc.graphics.*;
import arc.graphics.font.*;
import arc.graphics.g2d.*;
import arc.graphics.font.Font.*;
import arc.graphics.g2d.GlyphLayout.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

/**
 * An extension of {@link Label} that progressively shows the text as if it was being typed in real time, and allows the
 * use of tokens in the following format: <tt>{TOKEN=PARAMETER}</tt>.
 * Code taken and ported from https://github.com/rafaskb/typing-label
 * <p>
 * {@link Label} 的扩展,以类似实时打字的方式渐进显示文本,并允许使用如下格式的令牌:<tt>{TOKEN=PARAMETER}</tt>。代码取自 https://github.com/rafaskb/typing-label 并完成移植
 */
public class FLabel extends Label{

    // Collections
    // 集合
    private final ObjectMap<String, String> variables = new ObjectMap<>();
    protected final Ar<FParser.TokenEntry> tokenEntries = new Ar<>();

    // Config
    // 配置
    private Color clearColor = new Color(FConfig.defaultClearColor);
    private FListener listener = null;
    boolean forceMarkupColor = FConfig.forceColorMarkupByDefault;

    // Internal state
    // 内部状态
    private final StringBuilder originalText = new StringBuilder();
    private final Ar<FGlyph> glyphCache = new Ar<>();
    private final IntAr glyphRunCapacities = new IntAr();
    private final IntAr offsetCache = new IntAr();
    private final IntAr layoutLineBreaks = new IntAr();
    private final Ar<FEffect> activeEffects = new Ar<>();
    private float textSpeed = FConfig.defaultSpeedPerChar;
    private float charCooldown = textSpeed;
    private int rawCharIndex = -2; // All chars, including color codes
    // 所有字符,包括颜色代码
    private int glyphCharIndex = -1; // Only renderable chars, excludes color codes
    // 仅可渲染的字符,不包括颜色代码
    private int glyphCharCompensation = 0;
    private int cachedGlyphCharIndex = -1; // Last glyphCharIndex sent to the cache
    // 上一次发送到缓存的 glyphCharIndex
    private float lastLayoutX = 0;
    private float lastLayoutY = 0;
    private boolean parsed = false;
    private boolean paused = false;
    private boolean ended = false;
    private boolean skipping = false;
    private boolean ignoringEvents = false;
    private boolean ignoringEffects = false;
    private String defaultToken = "";

    public FLabel(CharSequence text){
        super(text);
        saveOriginalText();
    }

    /**
     * Modifies the text of this label. If the char progression is already running, it's highly recommended to use
     * {@link #restart(CharSequence)} instead.
     * <p>
     * 修改此标签的文本。如果字符推进已经在进行,强烈建议改用 {@link #restart(CharSequence)}。
     */
    @Override
    public void setText(CharSequence newText){
        this.setText(newText, true);
    }

    /**
     * Sets the text of this label.
     * <p>
     * 设置此标签的文本。
     *
     * @param modifyOriginalText Flag determining if the original text should be modified as well. If {@code false}, 标志位,决定是否同时修改原始文本。如果为 {@code false},则只更改显示文本,原始文本保持不变。
     * only the display text is changed while the original text is untouched.
     * @see #restart(CharSequence)
     */
    protected void setText(CharSequence newText, boolean modifyOriginalText){
        super.setText(newText);
        if(modifyOriginalText && originalText != null) saveOriginalText();
    }

    /**
     * Similar to {@link #getText()}, but returns the original text with all the tokens unchanged.
     * 类似于 {@link #getText()},但返回的是所有令牌保持原样的原始文本。
     */
    public StringBuilder getOriginalText(){
        return originalText;
    }

    /**
     * Copies the content of {@link #getText()} to the {@link StringBuilder} containing the original text with all
     * tokens unchanged.
     * <p>
     * 将 {@link #getText()} 的内容复制到保存原始文本(所有令牌保持原样)的 {@link StringBuilder} 中。
     */
    protected void saveOriginalText(){
        originalText.setLength(0);
        originalText.insert(0, this.getText());
        originalText.trimToSize();
    }

    /**
     * Restores the original text with all tokens unchanged to this label. Make sure to call {@link #parseTokens()} to
     * parse the tokens again.
     * <p>
     * 将所有令牌保持原样的原始文本恢复到此标签。请务必调用 {@link #parseTokens()} 重新解析令牌。
     */
    protected void restoreOriginalText(){
        super.setText(originalText);
        this.parsed = false;
    }

    /**
     * Returns the {@link FListener} associated with this label. May be {@code null}.
     * 返回与此标签关联的 {@link FListener}。可能为 {@code null}。
     */
    public FListener getTypingListener(){
        return listener;
    }

    /**
     * Sets the {@link FListener} associated with this label, or {@code null} to remove the current one.
     * 设置与此标签关联的 {@link FListener},传 {@code null} 可移除当前的监听器。
     */
    public void setTypingListener(FListener listener){
        this.listener = listener;
    }

    /**
     * Returns a {@link Color} instance with the color to be used on {@code CLEARCOLOR} tokens. Modify this instance to
     * change the token color. Default value is specified by {@link FConfig}.
     * <p>
     * 返回一个 {@link Color} 实例,即 {@code CLEARCOLOR} 令牌要使用的颜色。修改此实例即可更改该令牌的颜色。默认值由 {@link FConfig} 指定。
     *
     * @see FConfig#defaultClearColor
     */
    public Color getClearColor(){
        return clearColor;
    }

    /**
     * Sets whether or not this instance should enable markup color by force.
     * <p>
     * 设置此实例是否强制启用标记颜色。
     *
     * @see FConfig#forceColorMarkupByDefault
     */
    public void setForceMarkupColor(boolean forceMarkupColor){
        this.forceMarkupColor = forceMarkupColor;
    }

    /**
     * Returns the default token being used in this label. Defaults to empty string.
     * 返回此标签使用的默认令牌。默认为空字符串。
     */
    public String getDefaultToken(){
        return defaultToken;
    }

    /**
     * Sets the default token being used in this label. This token will be used before the label's text, and after each
     * {RESET} call. Useful if you want a certain token to be active at all times without having to type it all the
     * time.
     * <p>
     * 设置此标签使用的默认令牌。该令牌会被用在标签文本之前,以及每次 {RESET} 调用之后。如果你想让某个令牌始终生效而不必每次都输入它,这会很有用。
     */
    public void setDefaultToken(String defaultToken){
        this.defaultToken = defaultToken == null ? "" : defaultToken;
        this.parsed = false;
    }

    /**
     * Parses all tokens of this label. Use this after setting the text and any variables that should be replaced.
     * 解析此标签的所有令牌。在设置文本以及任何需要替换的变量之后调用此方法。
     */
    public void parseTokens(){
        this.setText(getDefaultToken() + getText(), false);
        FParser.parseTokens(this);
        parsed = true;
    }

    /**
     * Skips the char progression to the end, showing the entire label. Useful for when users don't want to wait for too
     * long. Ignores all subsequent events by default.
     * <p>
     * 将字符推进跳到末尾,显示整个标签。适用于用户不想等待太久的情况。默认忽略所有后续事件。
     */
    public void skipToTheEnd(){
        skipToTheEnd(true);
    }

    /**
     * Skips the char progression to the end, showing the entire label. Useful for when users don't want to wait for too
     * long.
     * <p>
     * 将字符推进跳到末尾,显示整个标签。适用于用户不想等待太久的情况。
     *
     * @param ignoreEvents If {@code true}, skipped events won't be reported to the listener. 如果为 {@code true},被跳过的事件不会报告给监听器。
     */
    public void skipToTheEnd(boolean ignoreEvents){
        skipToTheEnd(ignoreEvents, false);
    }

    /**
     * Skips the char progression to the end, showing the entire label. Useful for when users don't want to wait for too
     * long.
     * <p>
     * 将字符推进跳到末尾,显示整个标签。适用于用户不想等待太久的情况。
     *
     * @param ignoreEvents If {@code true}, skipped events won't be reported to the listener. 如果为 {@code true},被跳过的事件不会报告给监听器。
     * @param ignoreEffects If {@code true}, all text effects will be instantly cancelled. 如果为 {@code true},所有文本效果都会被立即取消。
     */
    public void skipToTheEnd(boolean ignoreEvents, boolean ignoreEffects){
        skipping = true;
        ignoringEvents = ignoreEvents;
        ignoringEffects = ignoreEffects;
    }

    /**
     * Cancels calls to {@link #skipToTheEnd()}. Useful if you need to restore the label's normal behavior at some event
     * after skipping.
     * <p>
     * 取消对 {@link #skipToTheEnd()} 的调用。如果你需要在跳过之后的某个事件中恢复标签的正常行为,这会很有用。
     */
    public void cancelSkipping(){
        if(skipping){
            skipping = false;
            ignoringEvents = false;
            ignoringEffects = false;
        }
    }

    /**
     * Returns whether or not this label is paused.
     * 返回此标签是否已暂停。
     */
    public boolean isPaused(){
        return paused;
    }

    /**
     * Pauses this label's character progression.
     * 暂停此标签的字符推进。
     */
    public void pause(){
        paused = true;
    }

    /**
     * Resumes this label's character progression.
     * 恢复此标签的字符推进。
     */
    public void resume(){
        paused = false;
    }

    /**
     * Returns whether or not this label's char progression has ended.
     * 返回此标签的字符推进是否已结束。
     */
    public boolean hasEnded(){
        return ended;
    }

    /**
     * Restarts this label with the original text and starts the char progression right away. All tokens are
     * automatically parsed.
     * <p>
     * 使用原始文本重启此标签,并立即开始字符推进。所有令牌都会被自动解析。
     */
    public void restart(){
        restart(getOriginalText());
    }

    /**
     * Restarts this label with the given text and starts the char progression right away. All tokens are automatically
     * parsed.
     * <p>
     * 使用给定文本重启此标签,并立即开始字符推进。所有令牌都会被自动解析。
     */
    public void restart(CharSequence newText){
        // Reset cache collections
        // 重置缓存集合
        Pools.freeAll(glyphCache);
        glyphCache.clear();
        glyphRunCapacities.clear();
        offsetCache.clear();
        layoutLineBreaks.clear();
        activeEffects.clear();

        // Reset state
        // 重置状态
        textSpeed = FConfig.defaultSpeedPerChar;
        charCooldown = textSpeed;
        rawCharIndex = -2;
        glyphCharIndex = -1;
        glyphCharCompensation = 0;
        cachedGlyphCharIndex = -1;
        lastLayoutX = 0;
        lastLayoutY = 0;
        parsed = false;
        paused = false;
        ended = false;
        skipping = false;
        ignoringEvents = false;
        ignoringEffects = false;

        // Set new text
        // 设置新文本
        this.setText(newText);
        invalidate();

        // Parse tokens
        // 解析令牌
        tokenEntries.clear();
        parseTokens();
    }

    /**
     * Returns an {@link ObjectMap} with all the variable names and their respective replacement values.
     * 返回一个 {@link ObjectMap},包含所有变量名及其对应的替换值。
     */
    public ObjectMap<String, String> getVariables(){
        return variables;
    }

    /**
     * Registers a variable and its respective replacement value to this label.
     * 向此标签注册一个变量及其对应的替换值。
     */
    public void setVariable(String var, String value){
        variables.put(var.toUpperCase(), value);
    }

    /**
     * Registers a set of variables and their respective replacement values to this label.
     * 向此标签注册一组变量及其对应的替换值。
     */
    public void setVariables(ObjectMap<String, String> variableMap){
        this.variables.clear();
        variableMap.each((key, val) -> variables.put(key.toUpperCase(), val));
    }

    /**
     * Removes all variables from this label.
     * 移除此标签中的所有变量。
     */
    public void clearVariables(){
        this.variables.clear();
    }

    @Override
    public void act(float delta){
        super.act(delta);

        // Force token parsing
        // 强制解析令牌
        if(!parsed){
            parseTokens();
        }

        // Update cooldown and process char progression
        // 更新冷却时间并处理字符推进
        if(skipping || (!ended && !paused)){
            if(skipping || (charCooldown -= delta) < 0.0f){
                processCharProgression();
            }
        }

        // Restore glyph offsets
        // 恢复字形偏移
        if(activeEffects.size > 0){
            for(int i = 0; i < glyphCache.size; i++){
                FGlyph glyph = glyphCache.get(i);
                glyph.xoffset = offsetCache.get(i * 2);
                glyph.yoffset = offsetCache.get(i * 2 + 1);
            }
        }

        // Apply effects
        // 应用效果
        if(!ignoringEffects){
            for(int i = activeEffects.size - 1; i >= 0; i--){
                FEffect effect = activeEffects.get(i);
                effect.update(delta);
                int start = effect.indexStart;
                int end = effect.indexEnd >= 0 ? effect.indexEnd : glyphCharIndex;

                // If effect is finished, remove it
                // 如果效果已结束,则将其移除
                if(effect.isFinished()){
                    activeEffects.remove(i);
                    continue;
                }

                // Apply effect to glyph
                // 将效果应用到字形
                for(int j = Math.max(0, start); j <= glyphCharIndex && j <= end && j < glyphCache.size; j++){
                    FGlyph glyph = glyphCache.get(j);
                    effect.apply(this, glyph, j, delta);
                }
            }
        }
    }

    /**
     * Proccess char progression according to current cooldown and process all tokens in the current index.
     * 根据当前冷却时间处理字符推进,并处理当前索引处的所有令牌。
     */
    private void processCharProgression(){
        // Keep a counter of how many chars we're processing in this tick.
        // 用一个计数器记录这一帧内处理了多少个字符。
        int charCounter = 0;

        // Process chars while there's room for it
        // 在还有余量时继续处理字符
        while(skipping || charCooldown < 0.0f){
            // Apply compensation to glyph index, if any
            // 对字形索引应用补偿(如有)
            if(glyphCharCompensation != 0){
                if(glyphCharCompensation > 0){
                    glyphCharIndex++;
                    glyphCharCompensation--;
                }else{
                    glyphCharIndex--;
                    glyphCharCompensation++;
                }

                // Increment cooldown and wait for it
                // 增加冷却时间并等待它
                charCooldown += textSpeed;
                continue;
            }

            // Increase raw char index
            // 增加原始字符索引
            rawCharIndex++;

            // Get next character and calculate cooldown increment
            // 获取下一个字符并计算冷却增量
            int safeIndex = Mathf.clamp(glyphCharIndex + 1, 0, glyphCache.size - 1);
            char primitiveChar = '\u0000'; // Null character by default
            // 默认为空字符(null character)
            if(glyphCache.size > 0){
                primitiveChar = (char)glyphCache.get(safeIndex).id;//getText().charAt(safeIndex);
                float intervalMultiplier = FConfig.intervalMultipliersByChar.get(primitiveChar, 1);
                charCooldown += textSpeed * intervalMultiplier;
            }

            // If char progression is finished, or if text is empty, notify listener and abort routine
            // 如果字符推进已结束,或文本为空,则通知监听器并中止本流程
            int textLen = getText().length();
            if(textLen == 0 || rawCharIndex >= textLen){
                if(!ended){
                    ended = true;
                    skipping = false;
                    if(listener != null) listener.end();
                }
                return;
            }

            // Detect layout line breaks
            // 检测布局换行
            boolean isLayoutLineBreak = false;
            if(layoutLineBreaks.contains(glyphCharIndex)){
                layoutLineBreaks.removeValue(glyphCharIndex);
                isLayoutLineBreak = true;
            }

            // Increase glyph char index for all characters, except new lines.
            // 为除换行符之外的所有字符增加字形字符索引。
            if(rawCharIndex >= 0 && primitiveChar != '\n' && !isLayoutLineBreak) glyphCharIndex++;

            // Process tokens according to the current index
            // 根据当前索引处理令牌
            while(tokenEntries.size > 0 && tokenEntries.peek().index == rawCharIndex){
                FParser.TokenEntry entry = tokenEntries.pop();
                String token = entry.token;
                FParser.TokenCategory category = entry.category;

                // Process tokens
                // 处理令牌
                switch(category){
                    case speed:
                        textSpeed = entry.floatValue;
                        continue;
                    case wait:
                        glyphCharIndex--;
                        glyphCharCompensation++;
                        charCooldown += entry.floatValue;
                        continue;
                    case skip:
                        if(entry.stringValue != null){
                            rawCharIndex += entry.stringValue.length();
                        }
                        continue;
                    case event:
                        if(this.listener != null && !ignoringEvents){
                            listener.event(entry.stringValue);
                        }
                        continue;
                    case effectStart:
                    case effectEnd:
                        // Get effect class
                        // 获取效果类
                        boolean isStart = category == FParser.TokenCategory.effectStart;

                        // End all effects of the same type
                        // 结束所有同类型的效果
                        for(int i = 0; i < activeEffects.size; i++){
                            FEffect effect = activeEffects.get(i);
                            if(effect.indexEnd < 0){
                                if(effect.endToken.equals(token)){
                                    effect.indexEnd = glyphCharIndex - 1;
                                }
                            }
                        }

                        // Create new effect if necessary
                        // 如有必要,创建新效果
                        if(isStart){
                            entry.effect.indexStart = glyphCharIndex;
                            activeEffects.add(entry.effect);
                        }

                        continue;
                }
            }

            // Increment char counter
            // 增加字符计数器
            charCounter++;

            // Break loop if this was our first glyph to prevent glyph issues.
            // 如果这是我们的第一个字形,则跳出循环以避免字形问题。
            if(glyphCharIndex == -1){
                charCooldown = textSpeed;
                break;
            }

            // Break loop if enough chars were processed
            // 如果已处理了足够多的字符,则跳出循环
            charCounter++;
            int charLimit = FConfig.charLimitPerFrame;
            if(!skipping && charLimit > 0 && charCounter > charLimit){
                charCooldown = textSpeed;
                break;
            }
        }
    }

    @Override
    public boolean remove(){
        Pools.freeAll(glyphCache);
        glyphCache.clear();
        return super.remove();
    }

    @Override
    public void layout(){
        // --- SUPERCLASS IMPLEMENTATION (but with accessible getters instead) ---
        // --- 父类实现(但改用可访问的 getter)---
        FontCache cache = getFontCache();
        StringBuilder text = getText();
        GlyphLayout layout = super.getGlyphLayout();
        int lineAlign = getLineAlign();
        int labelAlign = getLabelAlign();
        LabelStyle style = getStyle();

        Font font = cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if(fontScaleChanged) font.getData().setScale(getFontScaleX(), getFontScaleY());

        boolean wrap = this.wrap && ellipsis == null;
        if(wrap){
            float prefHeight = getPrefHeight();
            if(prefHeight != lastPrefHeight){
                lastPrefHeight = prefHeight;
                invalidateHierarchy();
            }
        }

        float width = getWidth(), height = getHeight();
        Drawable background = style.background;
        float x = 0, y = 0;
        if(background != null){
            x = background.getLeftWidth();
            y = background.getBottomHeight();
            width -= background.getLeftWidth() + background.getRightWidth();
            height -= background.getBottomHeight() + background.getTopHeight();
        }

        float textWidth, textHeight;
        // if(wrap || text.indexOf("\n") != -1)
        {
            // If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
            // 如果文本可能跨越多行,则计算出文本的实际大小,以便在标签内进行对齐。
            layout.setText(font, text, 0, text.length(), Color.white, width, lineAlign, wrap, ellipsis);
            textWidth = layout.width;
            textHeight = layout.height;

            if((labelAlign & Align.left) == 0){
                if((labelAlign & Align.right) != 0)
                    x += width - textWidth;
                else
                    x += (width - textWidth) / 2;
            }
            // } else {
            // textWidth = width;
            // textHeight = font.getData().capHeight;
        }

        if((labelAlign & Align.top) != 0){
            y += cache.getFont().isFlipped() ? 0 : height - textHeight;
            y += style.font.getDescent();
        }else if((labelAlign & Align.bottom) != 0){
            y += cache.getFont().isFlipped() ? height - textHeight : 0;
            y -= style.font.getDescent();
        }else{
            y += (height - textHeight) / 2;
        }
        if(!cache.getFont().isFlipped()) y += textHeight;

        // Don't set the layout or cache now, since we progressively update both over time.
        // 现在不要设置布局或缓存,因为我们会随时间逐步更新这两者。
        // layout.setText(font, text, 0, text.length, Color.white, textWidth, lineAlign, wrap, ellipsis);
        // cache.setText(layout, x, y);
        if(fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);

        // --- END OF SUPERCLASS IMPLEMENTATION ---
        // --- 父类实现结束 ---

        // Store coordinates passed to FontCache
        // 保存传递给 FontCache 的坐标
        lastLayoutX = x;
        lastLayoutY = y;

        // Perform cache layout operation, where the magic happens
        // 执行缓存布局操作,关键之处就在这里
        Pools.freeAll(glyphCache);
        glyphCache.clear();
        layoutCache();
    }

    /**
     * Reallocate glyph clones according to the updated {@link GlyphLayout}. This should only be called when the text or
     * the layout changes.
     * <p>
     * 根据更新后的 {@link GlyphLayout} 重新分配字形克隆。只有在文本或布局发生变化时才应调用此方法。
     */
    private void layoutCache(){
        FontCache cache = getFontCache();
        GlyphLayout layout = super.getGlyphLayout();
        Ar<GlyphRun> runs = layout.runs;

        // Reset layout line breaks
        // 重置布局换行记录
        layoutLineBreaks.clear();

        // Store GlyphRun sizes and count how many glyphs we have
        // 记录各个 GlyphRun 的大小并统计字形总数
        int glyphCount = 0;
        glyphRunCapacities.setSize(runs.size);
        for(int i = 0; i < runs.size; i++){
            Ar<Glyph> glyphs = runs.get(i).glyphs;
            glyphRunCapacities.set(i, glyphs.size);
            glyphCount += glyphs.size;
        }

        // Make sure our cache array can hold all glyphs
        // 确保缓存数组能容纳所有字形
        if(glyphCache.size < glyphCount){
            glyphCache.setSize(glyphCount);
            offsetCache.setSize(glyphCount * 2);
        }

        // Clone original glyphs with independent instances
        // 用独立实例克隆原始字形
        int index = -1;
        float lastY = 0;
        for(int i = 0; i < runs.size; i++){
            GlyphRun run = runs.get(i);
            Ar<Glyph> glyphs = run.glyphs;
            for(int j = 0; j < glyphs.size; j++){

                // Detect and store layout line breaks
                // 检测并记录布局换行
                if(!Mathf.equal(run.y, lastY)){
                    lastY = run.y;
                    layoutLineBreaks.add(index);
                }

                // Increment index
                // 增加索引
                index++;

                // Get original glyph
                // 获取原始字形
                Glyph original = glyphs.get(j);

                // Get clone glyph
                // 获取克隆字形
                FGlyph clone = null;
                if(index < glyphCache.size){
                    clone = glyphCache.get(index);
                }
                if(clone == null){
                    clone = Pools.obtain(FGlyph.class, FGlyph::new);
                    glyphCache.set(index, clone);
                }
                clone.set(original);
                clone.width *= getFontScaleX();
                clone.height *= getFontScaleY();
                clone.xoffset *= getFontScaleX();
                clone.yoffset *= getFontScaleY();
                clone.run = run;

                // Store offset data
                // 保存偏移数据
                offsetCache.set(index * 2, clone.xoffset);
                offsetCache.set(index * 2 + 1, clone.yoffset);

                // Replace glyph in original array
                // 在原始数组中替换该字形
                glyphs.set(j, clone);
            }
        }

        // Remove exceeding glyphs from original array
        // 从原始数组中移除多余的字形
        int glyphCountdown = glyphCharIndex;
        for(int i = 0; i < runs.size; i++){
            Ar<Glyph> glyphs = runs.get(i).glyphs;
            if(glyphs.size < glyphCountdown){
                glyphCountdown -= glyphs.size;
                continue;
            }

            for(int j = 0; j < glyphs.size; j++){
                if(glyphCountdown < 0){
                    glyphs.removeRange(j, glyphs.size - 1);
                    break;
                }
                glyphCountdown--;
            }
        }

        // Pass new layout with custom glyphs to FontCache
        // 将带有自定义字形的新布局传递给 FontCache
        cache.setText(layout, lastLayoutX, lastLayoutY);
    }

    /**
     * Adds cached glyphs to the active FontCache as the char index progresses.
     * 随着字符索引的推进,将缓存中的字形添加到活动的 FontCache 中。
     */
    private void addMissingGlyphs(){
        // Add additional glyphs to layout array, if any
        // 向布局数组添加额外的字形(如有)
        int glyphLeft = glyphCharIndex - cachedGlyphCharIndex;
        if(glyphLeft < 1) return;

        // Get runs
        // 获取各个 run
        GlyphLayout layout = super.getGlyphLayout();
        Ar<GlyphRun> runs = layout.runs;

        // Iterate through GlyphRuns to find the next glyph spot
        // 遍历各个 GlyphRun,找到下一个字形的位置
        int glyphCount = 0;
        for(int runIndex = 0; runIndex < glyphRunCapacities.size; runIndex++){
            int runCapacity = glyphRunCapacities.get(runIndex);
            if((glyphCount + runCapacity) < cachedGlyphCharIndex){
                glyphCount += runCapacity;
                continue;
            }

            // Get run and increase glyphCount up to its current size
            // 获取该 run,并将 glyphCount 累加到其当前大小
            Ar<Glyph> glyphs = runs.get(runIndex).glyphs;
            glyphCount += glyphs.size;

            // Next glyphs go here
            // 接下来的字形将放在这里
            while(glyphLeft > 0){

                // Skip run if this one is full
                // 如果此 run 已满,则跳过
                int runSize = glyphs.size;
                if(runCapacity == runSize){
                    break;
                }

                // Put new glyph to this run
                // 将新字形放入此 run
                cachedGlyphCharIndex++;
                FGlyph glyph = glyphCache.get(cachedGlyphCharIndex);
                glyphs.add(glyph);

                // Cache glyph's vertex index
                // 缓存字形的顶点索引
                glyph.internalIndex = glyphCount;

                // Advance glyph count
                // 增加字形计数
                glyphCount++;
                glyphLeft--;

                // Notify listener about char progression
                // 通知监听器字符推进的情况
                if(listener != null){
                    listener.onChar((char)glyph.id);
                }
            }
        }
    }

    @Override
    public void draw(){
        super.validate();
        addMissingGlyphs();

        // Update cache with new glyphs
        // 用新字形更新缓存
        FontCache FontCache = getFontCache();
        getFontCache().setText(getGlyphLayout(), lastLayoutX, lastLayoutY);

        // Tint glyphs
        // 为字形着色
        for(FGlyph glyph : glyphCache){
            if(glyph.internalIndex >= 0 && glyph.color != null){
                FontCache.setColors(glyph.color, glyph.internalIndex, glyph.internalIndex + 1);
            }
        }

        super.draw();
    }

}
