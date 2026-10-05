package arc.backend.robovm;

public final class IOSScreenBounds {

	/**
	 * Offset from top left corner in points
	 * 距左上角的偏移量(以点为单位)
	 */
	public final int x, y;

	/**
	 * Dimensions of drawing surface in points
	 * 绘图表面的尺寸(以点为单位)
	 */
	public final int width, height;

	/**
	 * Dimensions of drawing surface in pixels
	 * 绘图表面的尺寸(以像素为单位)
	 */
	public final int backBufferWidth, backBufferHeight;

	public IOSScreenBounds (int x, int y, int width, int height, int backBufferWidth, int backBufferHeight) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.backBufferWidth = backBufferWidth;
		this.backBufferHeight = backBufferHeight;
	}
}