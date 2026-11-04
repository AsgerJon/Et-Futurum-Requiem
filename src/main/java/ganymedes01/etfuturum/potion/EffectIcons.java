package ganymedes01.etfuturum.potion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Effect icons by name (SPEC-modern-movement.md §70.3, stage C), as 26.3 keeps them: `textures/mob_effect/<name>.png`
 * in the namespace of the effect's name (`minecraft:speed` -> `minecraft:textures/mob_effect/speed.png`). EFR's own
 * effects draw theirs themselves (`ModPotions`); for every other effect with such a texture the inventory draws it.
 */
@SideOnly(Side.CLIENT)
public final class EffectIcons {

	/** Whether each effect has its texture, looked up once (a missing one costs a failed resource lookup). */
	private static final Map<Potion, ResourceLocation> FOUND = new IdentityHashMap<>();
	private static final ResourceLocation NONE = new ResourceLocation("etfuturum", "none");

	private EffectIcons() {
	}

	/** The effect's own icon, or null when it has none, or draws its own (EFR's). */
	public static ResourceLocation texture(Potion potion) {
		if (potion == null || potion instanceof ModPotions) {
			return null;
		}
		ResourceLocation found = FOUND.get(potion);
		if (found == null) {
			found = look(potion);
			FOUND.put(potion, found);
		}
		return found == NONE ? null : found;
	}

	private static ResourceLocation look(Potion potion) {
		String name = EffectRegistry.key(potion);
		if (name == null || name.indexOf(':') < 0) {
			return NONE;
		}
		ResourceLocation icon = new ResourceLocation(name.substring(0, name.indexOf(':')), "textures/mob_effect/" + name.substring(name.indexOf(':') + 1) + ".png");
		try {
			Minecraft.getMinecraft().getResourceManager().getResource(icon);
			return icon;
		} catch (IOException | RuntimeException e) {
			return NONE;
		}
	}
}
