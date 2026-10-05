package arc;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class Settings{
    protected final static byte typeBool = 0, typeInt = 1, typeLong = 2, typeFloat = 3, typeString = 4, typeBinary = 5;
    protected final static int maxBackups = 10, minBackupIntervalMs = 1000 * 60 * 2;

    //general state data
    // 常规状态数据
    protected Fi dataDirectory;
    protected String appName = "app";
    protected ObjectMap<String, Object> defaults = new ObjectMap<>();
    protected HashMap<String, Object> values = new HashMap<>();
    protected boolean modified;
    protected Cons<Throwable> errorHandler;
    protected boolean hasErrored;
    protected boolean shouldAutosave = true;
    protected boolean loaded = false;
    protected boolean writeCompressed = false;
    private long lastBackupTime;
    protected ExecutorService executor = Threads.executor("Settings Backup", 1);

    //IO utility objects
    // IO 工具对象
    protected ByteArrayOutputStream byteStream = new ByteArrayOutputStream(32);
    protected ReusableByteInStream byteInputStream = new ReusableByteInStream();
    protected Json json = new Json();

    public void setJson(Json json){
        this.json = json;
    }

    public void setCompressed(boolean compressed){
        this.writeCompressed = compressed;
    }

    public String getAppName(){
        return appName;
    }

    public void setAppName(String name){
        appName = name;
    }

    /**Sets the error handler function.
     * This function gets called when {@link #forceSave} or {@link #load} fails. This can occur most often on browsers,
     * where extensions can block writing to local storage.
     * <p>
     * 设置错误处理函数。
     * 当 {@link #forceSave} 或 {@link #load} 失败时会调用此函数。这种情况最常发生在浏览器上,因为浏览器扩展可能会阻止写入本地存储。
     */
    public void setErrorHandler(Cons<Throwable> handler){
        errorHandler = handler;
    }

    /** Set whether the data should autosave immediately upon changing a value.
     * Default value: true.
     * <p>
     * 设置更改值时是否立即自动保存。默认值:true。
     */
    public void setAutosave(boolean autosave){
        this.shouldAutosave = autosave;
    }

    public boolean modified(){
        return modified;
    }

    /**
     * Loads all values and keybinds.
     * 加载所有值和按键绑定。
     */
    public synchronized void load(){
        try{
            loadValues();
        }catch(Throwable error){
            Log.err("Error loading settings", error);
            if(errorHandler != null){
                if(!hasErrored) errorHandler.get(error);
            }else{
                throw error;
            }
            hasErrored = true;
        }
        //if loading failed, it still counts
        // 即使加载失败,也算作已加载
        loaded = true;
    }

    /**
     * Saves all values and keybinds.
     * 保存所有值和按键绑定。
     */
    public synchronized void forceSave(){
        //never loaded, nothing to save
        // 从未加载过,没有可保存的内容
        if(!loaded) return;
        try{
            saveValues();
        }catch(Throwable error){
            Log.err("Error writing settings", error);
            if(errorHandler != null){
                if(!hasErrored) errorHandler.get(error);
            }else{
                throw error;
            }
            hasErrored = true;
        }
        modified = false;
    }

    /**
     * Manually save, if the settings have been loaded at some point.
     * 手动保存,前提是设置曾在某个时刻被加载过。
     */
    public synchronized void manualSave(){
        if(loaded){
            forceSave();
        }
    }

    /**
     * Saves if any modifications were done.
     * 如果有任何修改,则进行保存。
     */
    public synchronized void autosave(){
        if(modified && shouldAutosave){
            forceSave();
            modified = false;
        }
    }

    /**
     * Loads a settings file into {@link #values} using the specified appName.
     * 使用指定的 appName 将设置文件加载到 {@link #values} 中。
     */
    public synchronized void loadValues(){
        //don't load settings files if neither of them exist
        // 如果两个设置文件都不存在,则不加载
        if(!getSettingsFile().exists() && !getBackupSettingsFile().exists()){
            return;
        }

        try{
            loadValues(getSettingsFile());

            //back up the save file, as the values have now been loaded successfully
            // 备份存档文件,因为值现已成功加载
            getSettingsFile().copyTo(getBackupSettingsFile());
        }catch(Throwable e){
            Log.err("Failed to load base settings file, attempting to load backup.", e);

            Ar<Fi> attempts = getBackupFolder().seq().add(getBackupSettingsFile());
            //sort with latest modified file first
            // 排序时将最近修改的文件排在最前
            attempts.sort(Structs.comparingLong(f -> -f.lastModified()));

            for(Fi attempt : attempts){
                try{

                    loadValues(attempt);
                    attempt.copyTo(getSettingsFile());

                    Log.info("Loaded backup settings file successfully!");

                    //break out of loop, we're done here
                    // 跳出循环,到此完成
                    return;
                }catch(Throwable e3){
                    Log.err("Failed to load backup settings file.", e3);
                }
            }
        }
    }

    public synchronized void loadValues(Fi file) throws IOException{
        //read the first few bytes to check if it is compressed.
        // 读取前几个字节,以检查是否为压缩格式。
        byte[] header = new byte[2];
        file.readBytes(header, 0, 2);
        boolean compressed = header[0] == (byte)0x78 && (header[1] == (byte)0x01 || header[1] == (byte)0x5E || header[1] == (byte)0x9c || header[1] == (byte)0xda);

        try(DataInputStream stream = new DataInputStream(compressed ? new InflaterInputStream(file.read(8192)) : file.read(8192))){
            int amount = stream.readInt();
            //current theory: when corruptions happen, the only things written to the stream are a bunch of zeroes
            // 目前的推测:发生损坏时,写入流中的只有一堆零
            //try to anticipate this case and throw an exception when 0 values are written
            // 尝试预判这种情况,并在写入 0 个值时抛出异常
            if(amount <= 0) throw new IOException("0 values are not allowed.");
            for(int i = 0; i < amount; i++){
                String key = stream.readUTF();

                byte type = stream.readByte();

                switch(type){
                    case typeBool:
                        values.put(key, stream.readBoolean());
                        break;
                    case typeInt:
                        values.put(key, stream.readInt());
                        break;
                    case typeLong:
                        values.put(key, stream.readLong());
                        break;
                    case typeFloat:
                        values.put(key, stream.readFloat());
                        break;
                    case typeString:
                        values.put(key, stream.readUTF());
                        break;
                    case typeBinary:
                        int length = stream.readInt();
                        byte[] bytes = new byte[length];
                        stream.readFully(bytes);
                        values.put(key, bytes);
                        break;
                    default:
                        throw new IOException("Unknown key type: " + type);
                }
            }
            //make sure all data was read - this helps with potential corruption
            // 确保所有数据都被读取 - 这有助于发现潜在的损坏
            int end = stream.read();
            if(end != -1){
                throw new IOException("Trailing settings data; expected EOF, but got: " + end);
            }
        }
    }

    /**
     * Saves all entries from {@link #values} into the correct location.
     * 将 {@link #values} 中的所有条目保存到正确的位置。
     */
    public synchronized void saveValues(){
        Fi file = getSettingsFile();

        try(DataOutputStream stream = new DataOutputStream(writeCompressed ? new FastDeflaterOutputStream(file.write(false, 8192)) : file.write(false, 8192))){
            stream.writeInt(values.size());

            for(Map.Entry<String, Object> entry : values.entrySet()){
                stream.writeUTF(entry.getKey());

                Object value = entry.getValue();

                if(value instanceof Boolean){
                    stream.writeByte(typeBool);
                    stream.writeBoolean((Boolean)value);
                }else if(value instanceof Integer){
                    stream.writeByte(typeInt);
                    stream.writeInt((Integer)value);
                }else if(value instanceof Long){
                    stream.writeByte(typeLong);
                    stream.writeLong((Long)value);
                }else if(value instanceof Float){
                    stream.writeByte(typeFloat);
                    stream.writeFloat((Float)value);
                }else if(value instanceof String){
                    stream.writeByte(typeString);
                    stream.writeUTF((String)value);
                }else if(value instanceof byte[]){
                    stream.writeByte(typeBinary);
                    stream.writeInt(((byte[])value).length);
                    stream.write((byte[])value);
                }
            }

        }catch(Throwable e){
            //file is now corrupt, delete it
            // 文件现已损坏,将其删除
            file.delete();
            throw new RuntimeException("Error writing preferences: " + file, e);
        }

        if(Time.timeSinceMillis(lastBackupTime) > minBackupIntervalMs){
            lastBackupTime = Time.millis();

            executor.submit(() -> {
                //make sure two backups can't happen at once.
                // 确保不会同时进行两个备份。
                synchronized(this){
                    Fi backupFolder = getBackupFolder();

                    Ar<Fi> previous = backupFolder.seq();
                    //make sure first file is most recent, last is oldest
                    // 确保第一个文件是最近的,最后一个是最早的
                    previous.sort(Structs.comparingLong(f -> -f.lastModified()));

                    //create new entry in the backup folder
                    // 在备份文件夹中创建新条目
                    file.copyTo(backupFolder.child(System.currentTimeMillis() + ".bin"));

                    //delete older backups if they exceed the max backup count
                    // 如果备份超过最大数量,则删除较旧的备份
                    while(previous.size >= maxBackups){
                        previous.pop().delete();
                    }
                }
            });
        }
    }

    /**
     * Returns the file used for writing settings to. Not available on all platforms!
     * 返回用于写入设置的文件。并非在所有平台上都可用!
     */
    public Fi getSettingsFile(){
        return getDataDirectory().child("settings.bin");
    }

    public Fi getBackupFolder(){
        return getDataDirectory().child("settings_backups");
    }

    public Fi getBackupSettingsFile(){
        return getDataDirectory().child("settings_backup.bin");
    }

    /**
     * Returns the directory where all settings and data is placed.
     * 返回存放所有设置和数据的目录。
     */
    public Fi getDataDirectory(){
        return dataDirectory == null ? Core.files.absolute(OS.getAppDataDirectoryString(appName)) : dataDirectory;
    }

    /**
     * Sets the settings file where everything is written to.
     * 设置所有内容写入的目标设置文件。
     */
    public void setDataDirectory(Fi file){
        this.dataDirectory = file;
    }

    /**
     * Set up a list of defaults values.
     * Format: name1, default1, name2, default2, etc
     * <p>
     * 设置一组默认值。格式:name1, default1, name2, default2 等
     */
    public synchronized void defaults(Object... objects){
        for(int i = 0; i < objects.length; i += 2){
            defaults.put((String)objects[i], objects[i + 1]);
        }
    }

    /**
     * Clears all preference values.
     * 清除所有偏好设置值。
     */
    public synchronized void clear(){
        values.clear();
    }

    public synchronized Object getDefault(String name){
        return defaults.get(name);
    }

    public synchronized boolean has(String name){
        return values.containsKey(name);
    }

    public synchronized Object get(String name, Object def){
        return values.containsKey(name) ? values.get(name) : def;
    }

    public boolean isModified(){
        return modified;
    }

    public synchronized void putJson(String name, Object value){
        putJson(name, null, value);
    }

    public synchronized void putJson(String name, Class<?> elementType, Object value){
        byteStream.reset();

        json.toUBJson(value, value == null ? null : value.getClass(), elementType, byteStream);

        put(name, byteStream.toByteArray());

        modified = true;
    }

    public synchronized <T> T getJson(String name, Class<T> type, Class elementType, Prov<T> def){
        try{
            if(!has(name)) return def.get();
            byteInputStream.setBytes(getBytes(name));
            return json.readValue(type, elementType, UBJson.read(byteInputStream));
        }catch(Throwable e){
            Log.err("Error reading JSON with key '" + name + "'", e);
            return def.get();
        }
    }

    public <T> T getJson(String name, Class<T> type, Prov<T> def){
        return getJson(name, type, null, def);
    }

    public float getFloat(String name, float def){
        return (float)get(name, def);
    }

    public long getLong(String name, long def){
        return (long)get(name, def);
    }

    public Long getLong(String name){
        return getLong(name, 0);
    }

    public int getInt(String name, int def){
        return (int)get(name, def);
    }

    public boolean getBool(String name, boolean def){
        Object val = get(name, def);
        return val instanceof Boolean ? (Boolean)val : def;
    }

    public byte[] getBytes(String name, byte[] def){
        return (byte[])get(name, def);
    }

    public String getString(String name, String def){
        return (String)get(name, def);
    }

    public float getFloat(String name){
        return getFloat(name, (float)defaults.get(name, 0f));
    }

    public int getInt(String name){
        return getInt(name, (int)defaults.get(name, 0));
    }

    public boolean getBool(String name){
        return getBool(name, (boolean)defaults.get(name, false));
    }

    /**
     * Runs the specified code once, and never again.
     * 只运行一次指定的代码,之后不再运行。
     */
    public void getBoolOnce(String name, Runnable run){
        if(!getBool(name, false)){
            run.run();
            put(name, true);
        }
    }

    /**
     * Returns true once, and never again.
     * 只返回一次 true,之后不再返回。
     */
    public boolean getBoolOnce(String name){
        boolean val = getBool(name, false);
        put(name, true);
        return !val;
    }

    public byte[] getBytes(String name){
        return getBytes(name, (byte[])defaults.get(name));
    }

    public String getString(String name){
        return getString(name, (String)defaults.get(name));
    }

    public void putAll(ObjectMap<String, Object> map){
        for(Entry<String, Object> entry : map.entries()){
            put(entry.key, entry.value);
        }
    }

    /**
     * Toggles a boolean value.
     * 切换一个布尔值。
     */
    public void toggle(String name){
        put(name, !getBool(name));
    }

    /**
     * JS compatibility method.
     * JS 兼容方法。
     */
    public void putInt(String name, int value){
        put(name, value);
    }

    /**
     * JS compatibility method.
     * JS 兼容方法。
     */
    public void putFloat(String name, float value){
        put(name, value);
    }

    /**
     * Stores an object in the preference map.
     * 在偏好设置映射中存储一个对象。
     */
    public synchronized void put(String name, Object object){
        if(object instanceof Float || object instanceof Integer || object instanceof Boolean || object instanceof Long
        || object instanceof String || object instanceof byte[]){
            values.put(name, object);
            modified = true;
        }else{
            throw new IllegalArgumentException("Invalid object stored: " + (object == null ? null : object.getClass()) + ".");
        }
    }

    public synchronized void remove(String name){
        values.remove(name);
        modified = true;
    }

    public synchronized Iterable<String> keys(){
        return values.keySet();
    }

    public synchronized int keySize(){
        return values.size();
    }
}
