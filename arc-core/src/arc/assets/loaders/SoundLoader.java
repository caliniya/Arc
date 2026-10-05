package arc.assets.loaders;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;

/**
 * {@link AssetLoader} to load {@link Sound} instances.
 * <p>
 * 用于加载 {@link Sound} 实例的 {@link AssetLoader}。
 * @author mzechner
 */
public class SoundLoader extends AsynchronousAssetLoader<Sound, SoundLoader.SoundParameter>{
    private Sound sound;

    public SoundLoader(FileHandleResolver resolver){
        super(resolver);
    }

    /**
     * Returns the {@link Sound} instance currently loaded by this
     * {@link SoundLoader}.
     * <p>
     * 返回此 {@link SoundLoader} 当前加载的 {@link Sound} 实例。
     * @return the currently loaded {@link Sound}, otherwise {@code null} if
     * no {@link Sound} has been loaded yet. 当前加载的 {@link Sound};如果尚未加载任何 {@link Sound},则为 {@code null}
     */
    protected Sound getLoadedSound(){
        return sound;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, SoundParameter parameter){
        if(parameter != null && parameter.sound != null){
            (sound = parameter.sound).load(file);
        }else{
            sound = Core.audio.newSound(file);
        }
    }

    @Override
    public Sound loadSync(AssetManager manager, String fileName, Fi file, SoundParameter parameter){
        Sound sound = this.sound;
        this.sound = null;
        return sound;
    }

    @Override
    public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, SoundParameter parameter){
        return null;
    }

    public static class SoundParameter extends AssetLoaderParameters<Sound>{
        public @Nullable Sound sound;

        public SoundParameter(){
        }

        public SoundParameter(@Nullable Sound sound){
            this.sound = sound;
        }

        public SoundParameter(LoadedCallback loadedCallback){
            super(loadedCallback);
        }
    }

}
