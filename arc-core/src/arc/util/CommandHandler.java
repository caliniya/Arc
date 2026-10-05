package arc.util;


import arc.struct.Ar;
import arc.struct.ObjectMap;
import arc.func.Cons;

/**
 * Parses command syntax.
 * 解析命令语法。
 */
public class CommandHandler{
    public String prefix = "";

    private final ObjectMap<String, Command> commands = new ObjectMap<>();
    private final Ar<Command> orderedCommands = new Ar<>();

    /**
     * Creates a command handler with a specific command prefix.
     * 使用指定的命令前缀创建命令处理器。
     */
    public CommandHandler(String prefix){
        this.prefix = prefix;
    }

    public void setPrefix(String prefix){
        this.prefix = prefix;
    }
    
    public String getPrefix(){
        return prefix;   
    }

    /**
     * Handles a message with no additional parameters.
     * 处理不带额外参数的消息。
     */
    public CommandResponse handleMessage(String message){
        return handleMessage(message, null);
    }

    /** Handles a message with optional extra parameters. Runs the command if successful.
     * <p>
     * 处理带有可选额外参数的消息。若成功则执行命令。
     * @return a response detailing whether or not the command was handled, and what went wrong, if applicable. 返回描述命令是否被处理以及(如适用)出错原因的响应。 */
    public CommandResponse handleMessage(String message, Object params){
        if(message == null || (!message.startsWith(prefix)))
            return new CommandResponse(ResponseType.noCommand, null, null);

        message = message.substring(prefix.length());

        String commandstr = message.contains(" ") ? message.substring(0, message.indexOf(" ")) : message;
        String argstr = message.contains(" ") ? message.substring(commandstr.length() + 1) : "";

        Ar<String> result = new Ar<>();

        Command command = commands.get(commandstr);

        if(command != null){
            int index = 0;
            boolean satisfied = false;

            while(true){
                if(index >= command.params.length && !argstr.isEmpty()){
                    return new CommandResponse(ResponseType.manyArguments, command, commandstr);
                }else if(argstr.isEmpty()) break;

                if(command.params[index].optional || index >= command.params.length - 1 || command.params[index + 1].optional){
                    satisfied = true;
                }

                if(command.params[index].variadic){
                    result.add(argstr);
                    break;
                }

                int next = argstr.indexOf(" ");
                if(next == -1){
                    if(!satisfied){
                        return new CommandResponse(ResponseType.fewArguments, command, commandstr);
                    }
                    result.add(argstr);
                    break;
                }else{
                    String arg = argstr.substring(0, next);
                    argstr = argstr.substring(arg.length() + 1);
                    result.add(arg);
                }

                index++;
            }

            if(!satisfied && command.params.length > 0 && !command.params[0].optional){
                return new CommandResponse(ResponseType.fewArguments, command, commandstr);
            }

            command.runner.accept(result.toArray(String.class), params);

            return new CommandResponse(ResponseType.valid, command, commandstr);
        }else{
            return new CommandResponse(ResponseType.unknownCommand, null, commandstr);
        }
    }

    public void removeCommand(String text){
        Command c = commands.get(text);
        if(c == null) return;
        commands.remove(text);
        orderedCommands.remove(c);
    }

    /**
     * Register a command which handles a zero-sized list of arguments and one parameter.
     * 注册一个处理空参数列表和一个参数的命令。
     */
    public <T> Command register(String text, String description, CommandRunner<T> runner){
        return register(text, "", description, runner);
    }

    /** Register a command which handles a list of arguments and one handler-specific parameter. <br>
     * argeter syntax is as follows: <br>
     * &lt;mandatory-arg-1&gt; &lt;mandatory-arg-2&gt; ... &lt;mandatory-arg-n&gt; [optional-arg-1] [optional-arg-2] <br>
     * Angle brackets indicate mandatory arguments. Square brackets to indicate optional arguments. <br>
     * All mandatory arguments must come before optional arguments. Arg names must not have spaces in them. <br>
     * You may also use the ... syntax after the arg name to designate that everything after it will not be split into extra arguments. 
     * There may only be one such argument, and it must be at the end. For example, the syntax
     * <p>
     * 注册一个处理参数列表和一个处理器特定参数的命令。<br>
     * argeter 语法如下:<br>
     * &lt;mandatory-arg-1&gt; &lt;mandatory-arg-2&gt; ... &lt;mandatory-arg-n&gt; [optional-arg-1] [optional-arg-2] <br>
     * 尖括号表示必选参数,方括号表示可选参数。<br>
     * 所有必选参数必须位于可选参数之前;参数名中不能包含空格。<br>
     * 还可以在参数名后使用 ... 语法,表示其后的所有内容不再拆分为额外参数。
     * 这类参数只能有一个,且必须位于末尾。例如,语法
     * &lt;arg1&gt [arg2...] 要求提供第一个参数,然后把其后的所有文本放入第二个参数(可选)。
     * &lt;arg1&gt [arg2...] will require a first argument, and then take any text after that and put it in the second argument, optionally.*/
    public <T> Command register(String text, String params, String description, CommandRunner<T> runner){
        //remove previously registered commands
        // 移除之前注册的命令
        orderedCommands.remove(c -> c.text.equals(text));

        Command cmd = new Command(text, params, description, runner);
        commands.put(text, cmd);
        orderedCommands.add(cmd);
        return cmd;
    }

    public Command register(String text, String description, Cons<String[]> runner){
        return register(text, description, (args, p) -> runner.get(args));
    }

    public Command register(String text, String params, String description, Cons<String[]> runner){
        return register(text, params, description, (args, p) -> runner.get(args));
    }

    public Ar<Command> getCommandList(){
        return orderedCommands;
    }

    public enum ResponseType{
        noCommand, unknownCommand, fewArguments, manyArguments, valid
    }

    public static class Command{
        public final String text;
        public final String paramText;
        public final String description;
        public final CommandParam[] params;
        final CommandRunner runner;

        public Command(String text, String paramText, String description, CommandRunner runner){
            this.text = text;
            this.paramText = paramText;
            this.runner = runner;
            this.description = description;

            String[] psplit = paramText.split(" ");
            if(paramText.length() == 0){
                params = new CommandParam[0];
            }else{
                params = new CommandParam[psplit.length];

                boolean hadOptional = false;

                for(int i = 0; i < params.length; i++){
                    String param = psplit[i];

                    if(param.length() <= 2) throw new IllegalArgumentException("Malformed param '" + param + "'");

                    char l = param.charAt(0), r = param.charAt(param.length() - 1);
                    boolean optional, variadic = false;

                    if(l == '<' && r == '>'){
                        if(hadOptional)
                            throw new IllegalArgumentException("Can't have non-optional param after optional param!");
                        optional = false;
                    }else if(l == '[' && r == ']'){
                        optional = true;
                    }else{
                        throw new IllegalArgumentException("Malformed param '" + param + "'");
                    }

                    if(optional) hadOptional = true;

                    String fname = param.substring(1, param.length() - 1);
                    if(fname.endsWith("...")){
                        if(i != params.length - 1)
                            throw new IllegalArgumentException("A variadic parameter should be the last parameter!");

                        fname = fname.substring(0, fname.length() - 3);
                        variadic = true;
                    }

                    params[i] = new CommandParam(fname, optional, variadic);

                }
            }
        }
    }

    public interface CommandRunner<T>{
        void accept(String[] args, T parameter);
    }

    public static class CommandParam{
        public final String name;
        public final boolean optional;
        public final boolean variadic;

        public CommandParam(String name, boolean optional, boolean variadic){
            this.name = name;
            this.optional = optional;
            this.variadic = variadic;
        }
    }

    public static class CommandResponse{
        public final ResponseType type;
        public final Command command;
        public final String runCommand;

        public CommandResponse(ResponseType type, Command command, String runCommand){
            this.type = type;
            this.command = command;
            this.runCommand = runCommand;
        }
    }
}
