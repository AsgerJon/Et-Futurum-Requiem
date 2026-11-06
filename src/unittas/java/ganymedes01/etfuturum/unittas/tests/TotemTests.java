package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.TotemActivation;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The totem of undying's effects as 26.3's (SPEC-modern-movement.md §55.2): the sound, the particles around the saved
 * entity and, for the player, the totem flying at the screen for 40 ticks.
 */
@TasSuite("quick2")
public class TotemTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!ModItems.TOTEM_OF_UNDYING.isEnabled()) {
			skipTest("the totem is off in the config");
		}
	}

	public void testPlayerSavedByATotem() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setHealth(1.0F);
		});
		setHeld(new ItemStack(ModItems.TOTEM_OF_UNDYING.get()));
		waitUntilOnClient("the totem in hand", () -> mc().thePlayer.getHeldItem() != null, 20);
		clearSounds();
		onServer(() -> player().attackEntityFrom(DamageSource.generic, 10.0F));
		assertTrue(onServer(() -> player().isEntityAlive()), "the totem didn't save the player (the test's setup)");
		waitUntilOnClient("the totem animation on the client", () -> TotemActivation.ticksLeft() > 0, 10);
		waitForTotemSound();
		ticks(6);
		assertGreater(onClient(TotemTests::totemParticles), 0);
		screenshot("totem.activation");
		ticks(40);
		assertEqual(0, onClient(TotemActivation::ticksLeft), "the animation is over after 40 ticks");
	}

	/** Someone else's totem: the sound and the particles, but the animation is only for one's own. */
	public void testZombieSavedByATotem() {
		EntityZombie zombie = spawn(EntityZombie::new, 0.5D, 4.0D, 4.5D);
		onServer(() -> {
			zombie.setCurrentItemOrArmor(0, new ItemStack(ModItems.TOTEM_OF_UNDYING.get()));
			zombie.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
		});
		ticks(10);
		clearSounds();
		onServer(() -> zombie.attackEntityFrom(DamageSource.generic, 100.0F));
		assertTrue(onServer(zombie::isEntityAlive), "the totem didn't save the zombie (the test's setup)");
		waitForTotemSound();
		ticks(6);
		assertGreater(onClient(TotemTests::totemParticles), 0);
		assertEqual(0, onClient(TotemActivation::ticksLeft), "an animation for someone else's totem");
	}

	private void waitForTotemSound() {
		waitUntilOnClient("the totem sound", () -> {
			for (String sound : soundsHeard()) {
				if (sound.endsWith("item.totem.use")) {
					return true;
				}
			}
			return false;
		}, 10);
	}

	/** The totem particles alive now: the effect renderer's particles whose class is named for the totem. */
	@SuppressWarnings("rawtypes")
	private static int totemParticles() throws ReflectiveOperationException {
		Field field;
		try {
			field = EffectRenderer.class.getDeclaredField("fxLayers");
		} catch (NoSuchFieldException e) {
			field = EffectRenderer.class.getDeclaredField("field_78876_b");
		}
		field.setAccessible(true);
		int count = 0;
		for (List layer : (List[]) field.get(mc().effectRenderer)) {
			for (Object particle : layer) {
				if (particle.getClass().getSimpleName().contains("Totem")) {
					count++;
				}
			}
		}
		return count;
	}
}
