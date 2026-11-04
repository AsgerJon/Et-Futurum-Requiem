package ganymedes01.etfuturum.mixinplugin;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ganymedes01.etfuturum.lib.Reference.MOD_ID;

@LateMixin
public class EtFuturumLateMixins implements ILateMixinLoader {
	@Override
	public String getMixinConfig() {
		return "mixins." + MOD_ID + ".late.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedMods) {
		List<String> mixins = new ArrayList<>();

		return mixins;
	}
}
