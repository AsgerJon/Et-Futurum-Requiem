package ganymedes01.etfuturum.client.gui.options;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.gui.GuiAttackIndicatorButton;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import io.github.asgerjon.mcfly.mixins.early.modernoptions.client.AccessorGuiVideoSettings;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.client.settings.GameSettings;

import java.util.List;

import static ganymedes01.etfuturum.client.gui.options.OptionWidgets.of;

/**
 * 26.3's Video Settings (spec §54.4, §54.10): Display, Quality & Performance and Preferences, each under its header,
 * with 1.7.10's options in 26.3's order and 1.7.10's own (Graphics first, 3D Anaglyph and Advanced OpenGL last) in
 * Quality. Only the list is EFR's: it goes into 1.7.10's list field, so 1.7.10's clicks, drawing, Done and new GUI
 * scale handling work on it, and with them EFR's GUI scale by mouse wheel (guiscale.client).
 */
public class GuiModernVideoSettings extends GuiVideoSettings implements IOptionsLayout {

	private OptionsRowList list;
	private GuiButton done;

	public GuiModernVideoSettings(GuiScreen parent, GameSettings settings) {
		super(parent, settings);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		screenTitle = I18n.format("options.videoTitle");
		buttonList.clear();
		done = new GuiButton(200, width / 2 - 100, height - 27, I18n.format("gui.done"));
		buttonList.add(done);
		list = new OptionsRowList(mc, width, height, 32, height - 32, button -> {
		});
		list.header(I18n.format("options.video.display.header"))
			.add(of(Options.FRAMERATE_LIMIT), of(Options.ENABLE_VSYNC), of(Options.GUI_SCALE), of(Options.USE_FULLSCREEN), of(Options.GAMMA));
		list.header(I18n.format("options.video.quality.header"))
			.add(of(Options.GRAPHICS), of(Options.RENDER_DISTANCE), of(Options.AMBIENT_OCCLUSION), of(Options.RENDER_CLOUDS), of(Options.PARTICLES),
				of(Options.MIPMAP_LEVELS), of(Options.ANISOTROPIC_FILTERING), of(Options.ANAGLYPH), OpenGlHelper.field_153197_d ? of(Options.ADVANCED_OPENGL) : null);
		if (McFlyConfig.modernCombat) {
			list.header(I18n.format("options.video.preferences.header")).add(new GuiAttackIndicatorButton(1001, 0, 0));
		}
		((AccessorGuiVideoSettings) this).etfu$setOptionsRowList(list);
	}

	@Override
	public List<Object> etfu$entries() {
		List<Object> entries = list.entries();
		entries.add(done);
		return entries;
	}
}
