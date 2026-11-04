package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.potion.EffectIcons;
import ganymedes01.etfuturum.potion.EffectNumbers;
import ganymedes01.etfuturum.potion.EffectRegistry;
import ganymedes01.etfuturum.unittas.TasTestCase;
import io.netty.buffer.Unpooled;
import net.minecraft.command.CommandEffect;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;

/**
 * Named effects, stage C (SPEC-modern-movement.md §70.3, §70.5): the effect packets carry numbers past a byte; the
 * server's name -> number table goes to the client at join, so a client whose numbers differ maps them; icons by
 * name from `textures/mob_effect/<name>.png`; 1.7.10's `/effect` by name.
 */
public class EffectNetworkTests extends TasTestCase {

	/** A test effect past an unsigned byte's 255, made once a run. */
	private static Potion wide;

	private Potion wide() {
		onServer(() -> {
			if (wide == null) {
				wide = new TestEffect(300, "wide");
			}
		});
		return wide;
	}

	/** The packet that gives an entity an effect carries 300 whole (1.7.10 kept it in a byte: 44). */
	public void testEffectPacketWide() throws Exception {
		Potion potion = wide();
		S1DPacketEntityEffect sent = new S1DPacketEntityEffect(5, new PotionEffect(potion.id, 200, 1));
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		sent.writePacketData(buffer);
		S1DPacketEntityEffect read = new S1DPacketEntityEffect();
		read.readPacketData(buffer);
		assertEqual(potion.id, EffectNumbers.effectOf(sent), "the effect in the packet made");
		assertEqual(potion.id, EffectNumbers.effectOf(read), "the effect in the packet read");
		assertEqual(200, (int) read.func_149425_g(), "its duration");
	}

	/** The packet that takes an effect away carries 300 whole too (1.7.10 sent an unsigned byte: up to 255). */
	public void testRemovePacketWide() throws Exception {
		Potion potion = wide();
		S1EPacketRemoveEntityEffect sent = new S1EPacketRemoveEntityEffect(5, new PotionEffect(potion.id, 200, 0));
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		sent.writePacketData(buffer);
		S1EPacketRemoveEntityEffect read = new S1EPacketRemoveEntityEffect();
		read.readPacketData(buffer);
		assertEqual(potion.id, read.func_149075_d(), "the effect in the packet read");
	}

	/** The player gets an effect past 127 on the server and has it on the client; taken away, it goes there too. */
	public void testPlayerSeesAWideEffect() {
		Potion potion = wide();
		onServer(() -> player().addPotionEffect(new PotionEffect(potion.id, 400, 0)));
		waitUntilOnClient("the client player to have the effect at 300", () -> mc().thePlayer.isPotionActive(potion.id), 40);
		onServer(() -> player().removePotionEffect(potion.id));
		waitUntilOnClient("the client player to lose it", () -> !mc().thePlayer.isPotionActive(potion.id), 40);
	}

	/** The server's table came at join; a client whose numbers differ maps them both ways. */
	public void testJoinTableMaps() {
		Potion potion = wide();
		assertTrue(onClient(EffectNumbers::hasServerTable), "no table from the server at join");
		try {
			onClient(() -> {
				Map<String, Integer> table = new HashMap<>();
				for (String name : EffectRegistry.names()) {
					table.put(name, EffectRegistry.get(name).id);
				}
				// the server has the wide effect at 777 in this pretend
				table.put(EffectRegistry.key(potion), 777);
				EffectNumbers.applyServerTable(table);
			});
			assertEqual(potion.id, (int) onClient(() -> EffectNumbers.toClient(777)), "the server's 777 on this client");
			assertEqual(777, (int) onClient(() -> EffectNumbers.toServer(potion.id)), "this client's number for the server");
			assertEqual(Potion.moveSpeed.id, (int) onClient(() -> EffectNumbers.toClient(Potion.moveSpeed.id)), "speed, the same on both");
		} finally {
			onClient(() -> {
				Map<String, Integer> table = new HashMap<>();
				for (String name : EffectRegistry.names()) {
					table.put(name, EffectRegistry.get(name).id);
				}
				EffectNumbers.applyServerTable(table);
			});
		}
	}

	/** An effect's icon by its name: `textures/mob_effect/<name>.png` where there is one, else 1.7.10's. */
	public void testIconsByName() {
		Potion potion = wide();
		assertEqual("minecraft:textures/mob_effect/speed.png", onClient(() -> String.valueOf(EffectIcons.texture(Potion.moveSpeed))), "speed's icon");
		assertEqual("null", onClient(() -> String.valueOf(EffectIcons.texture(potion))), "an effect with no icon of its own");
	}

	/** The inventory with effects on, for a look: 26.3's icons by name (speed, blindness), EFR's (slow falling), none (the test effect). */
	public void testIconsInTheInventory() {
		Potion potion = wide();
		onServer(() -> {
			player().addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 2400, 1));
			player().addPotionEffect(new PotionEffect(Potion.blindness.id, 2400, 0));
			player().addPotionEffect(new PotionEffect(potion.id, 2400, 0));
			if (ganymedes01.etfuturum.potion.ModPotions.slowFalling != null) {
				player().addPotionEffect(new PotionEffect(ganymedes01.etfuturum.potion.ModPotions.slowFalling.id, 2400, 0));
			}
		});
		waitUntilOnClient("the client to have the effects", () -> mc().thePlayer.isPotionActive(Potion.blindness), 40);
		onClient(() -> {
			mc().displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc().thePlayer));
		});
		ticks(5);
		String shot = screenshot("effects.inventory");
		onClient(() -> {
			mc().displayGuiScreen(null);
		});
		onServer(() -> player().clearActivePotions());
		assertTrue(shot != null, "no picture taken");
	}

	/** 1.7.10's own `/effect` takes a name as well as a number. */
	public void testVanillaEffectCommandByName() {
		onServer(() -> {
			player().removePotionEffect(Potion.digSpeed.id);
			new CommandEffect().processCommand(player(), new String[]{player().getCommandSenderName(), "minecraft:haste", "10"});
		});
		assertTrue(onServer(() -> player().isPotionActive(Potion.digSpeed)), "no haste from /effect by name");
		onServer(() -> player().removePotionEffect(Potion.digSpeed.id));
	}

	/** A test effect: neither good nor bad, white. */
	private static final class TestEffect extends Potion {
		TestEffect(int id, String name) {
			super(id, false, 0xFFFFFF);
			setPotionName("potion.unittas." + name);
		}
	}
}
