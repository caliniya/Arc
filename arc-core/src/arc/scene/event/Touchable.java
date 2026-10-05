package arc.scene.event;

/**
 * Determines how touch input events are distributed to an element and any children.
 * <p>
 * 决定触摸输入事件如何分配给一个元素及其子元素。
 * @author Nathan Sweet
 */
public enum Touchable{
    /**
     * All touch input events will be received by the element and any children.
     * 所有触摸输入事件都会被该元素及其子元素接收。
     */
    enabled,
    /**
     * No touch input events will be received by the element or any children.
     * 该元素及其子元素都不会接收任何触摸输入事件。
     */
    disabled,
    /**
     * No touch input events will be received by the element, but children will still receive events. Note that events on the
     * children will still bubble to the parent.
     * <p>
     * 该元素不会接收任何触摸输入事件,但子元素仍会接收事件。注意,子元素上的事件仍会向上冒泡到父级。
     */
    childrenOnly
}
