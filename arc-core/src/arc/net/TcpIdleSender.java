

package arc.net;

abstract public class TcpIdleSender implements NetListener{
    boolean started;

    public void idle(Connection connection){
        if(!started){
            started = true;
            start();
        }
        do{
            Object object = next();
            if(object == null){
                connection.removeListener(this);
                break;
            }else{
                connection.sendTCP(object);
            }
        }while(connection.isIdle());
    }

    /**
     * Called once, before the first send. Subclasses can override this method
     * to send something so the receiving side expects subsequent objects.
     * <p>
     * 只调用一次,在第一次发送之前。子类可以重写此方法来发送一些内容,让接收端做好接收后续对象的准备。
     */
    protected void start(){
    }

    /**
     * Returns the next object to send, or null if no more objects will be sent.
     * <p>
     * 返回下一个要发送的对象;如果没有更多对象要发送,则返回 null。
     */
    abstract protected Object next();
}
