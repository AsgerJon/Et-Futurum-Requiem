package ganymedes01.etfuturum.client.gui.options;

import java.util.List;

/**
 * What a modern options screen shows, in order (SPEC-modern-movement.md §54.10), for the tests to read: they take the
 * names from the buttons themselves.
 */
public interface IOptionsLayout {

	/** The screen's entries top to bottom, left before right: its GuiButtons, and a String for each section header. */
	List<Object> etfu$entries();
}
