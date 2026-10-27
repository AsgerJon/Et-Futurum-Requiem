package ganymedes01.etfuturum.configuration;

import cpw.mods.fml.client.config.DummyConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The in-game config screen: one entry per config file, each holding that file's categories and their options.
 * Closing it with a change saves the file and reloads that config's values (ConfigBase.onConfigChanged); options
 * read at startup, the mixin toggles above all, still need a restart.
 */
public class ConfigGUI extends GuiConfig {

	public ConfigGUI(GuiScreen parent) {
		super(parent, getElements(), Reference.MOD_ID, Reference.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(Launch.minecraftHome + "config" + File.separator + Reference.MOD_ID));
	}

	@SuppressWarnings("rawtypes")
	private static List<IConfigElement> getElements() {
		List<IConfigElement> list = new ArrayList<>();
		List<ConfigBase> configs = new ArrayList<>(ConfigBase.all());
		configs.sort(Comparator.comparing(ConfigBase::getName));
		for (ConfigBase config : configs) {
			List<IConfigElement> categories = new ArrayList<>();
			// A category can be listed twice when two names map to it (Functions' "client" is also its commands category).
			for (ConfigCategory category : new LinkedHashSet<>(config.configCats)) {
				categories.add(new ConfigElement(category));
			}
			String name = config.getName();
			String label = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			list.add(new DummyConfigElement.DummyCategoryElement(label, label, categories));
		}
		return list;
	}
}
