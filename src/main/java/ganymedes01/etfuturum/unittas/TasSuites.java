package ganymedes01.etfuturum.unittas;

import ganymedes01.etfuturum.unittas.tests.AnvilTests;
import ganymedes01.etfuturum.unittas.tests.AttachTests;
import ganymedes01.etfuturum.unittas.tests.BedTests;
import ganymedes01.etfuturum.unittas.tests.BlockTests;
import ganymedes01.etfuturum.unittas.tests.CameraTests;
import ganymedes01.etfuturum.unittas.tests.ChatTests;
import ganymedes01.etfuturum.unittas.tests.CombatTests;
import ganymedes01.etfuturum.unittas.tests.ComponentTests;
import ganymedes01.etfuturum.unittas.tests.DamageTests;
import ganymedes01.etfuturum.unittas.tests.EffectTests;
import ganymedes01.etfuturum.unittas.tests.EnchantTests;
import ganymedes01.etfuturum.unittas.tests.EndTests;
import ganymedes01.etfuturum.unittas.tests.GiveTests;
import ganymedes01.etfuturum.unittas.tests.ItemIdTests;
import ganymedes01.etfuturum.unittas.tests.OffhandSwingTests;
import ganymedes01.etfuturum.unittas.tests.PlacementShapeTests;
import ganymedes01.etfuturum.unittas.tests.SelectorTests;
import ganymedes01.etfuturum.unittas.tests.ShieldTests;
import ganymedes01.etfuturum.unittas.tests.SneakEdgeTests;
import ganymedes01.etfuturum.unittas.tests.StepSoundTests;
import ganymedes01.etfuturum.unittas.tests.SummonTests;
import ganymedes01.etfuturum.unittas.tests.TorchTests;
import ganymedes01.etfuturum.unittas.tests.TotemTests;
import ganymedes01.etfuturum.unittas.tests.VanillaCommandTests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The suites ./unittas.sh can ask for, each a list of test classes run in this order. A new test class goes here. */
final class TasSuites {

	private static final Map<String, List<Class<? extends TasTestCase>>> SUITES = new LinkedHashMap<>();

	static {
		SUITES.put("commands", Arrays.asList(ChatTests.class, EffectTests.class, SelectorTests.class, EnchantTests.class,
			GiveTests.class, ComponentTests.class, ItemIdTests.class, SummonTests.class, BlockTests.class, VanillaCommandTests.class));
		SUITES.put("combat", Arrays.asList(CombatTests.class, DamageTests.class, ShieldTests.class));
		// the batch of small fixes of 2026-10-30 (spec §53)
		SUITES.put("quick", Arrays.asList(EndTests.class, BedTests.class, AttachTests.class, CameraTests.class, OffhandSwingTests.class,
			StepSoundTests.class, SneakEdgeTests.class, PlacementShapeTests.class));
		// batch 2 (spec §55), written before the fixes
		SUITES.put("quick2", Arrays.asList(AnvilTests.class, TotemTests.class));
		// parked by the user (2026-10-30): EFR's observer clock pulses 1 tick in 3, 26.3's code gives 2-tick pulses
		SUITES.put("redstone", Arrays.asList(TorchTests.class));
	}

	private TasSuites() {
	}

	static Collection<String> names() {
		return Collections.unmodifiableSet(SUITES.keySet());
	}

	/** The suite's classes, or null when there is no suite of that name. */
	static List<Class<? extends TasTestCase>> get(String name) {
		return SUITES.get(name);
	}
}
