package ganymedes01.etfuturum.client.gui;

/** A screen whose mouse wheel cycles the GUI scale once armed (the video settings, after a click on GUI Scale). */
public interface IGuiScaleWheel {
	boolean etfu$wheelChangesGuiScale();

	/** A wheel notch: positive for a larger scale, negative for a smaller one; applied before the next frame. */
	void etfu$guiScaleWheel(int wheel);
}
