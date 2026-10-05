package arc.scene.event;

public class ResizeListener implements EventListener{

    @Override
    public boolean handle(SceneEvent event){
        if(event instanceof SceneResizeEvent){
            //always returns false, because resizing is global.
            // 总是返回 false,因为调整大小是全局的。
            resized();
        }
        return false;
    }

    public void resized(){

    }
}
