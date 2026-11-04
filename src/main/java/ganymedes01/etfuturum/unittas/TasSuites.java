package ganymedes01.etfuturum.unittas;

import ganymedes01.etfuturum.unittas.tests.AnvilTests;
import ganymedes01.etfuturum.unittas.tests.ZombieArmsTests;
import ganymedes01.etfuturum.unittas.tests.SkeletonArmsTests;
import ganymedes01.etfuturum.unittas.tests.HarnessTests;
import ganymedes01.etfuturum.unittas.tests.GlintTests;
import ganymedes01.etfuturum.unittas.tests.EfrBlockStateTests;
import ganymedes01.etfuturum.unittas.tests.StrongholdTests;
import ganymedes01.etfuturum.unittas.tests.SpyglassTests;
import ganymedes01.etfuturum.unittas.tests.HorseTests;
import ganymedes01.etfuturum.unittas.tests.GlowSquidTests;
import ganymedes01.etfuturum.unittas.tests.SignTextTests;
import ganymedes01.etfuturum.unittas.tests.ItemFrameTests;
import ganymedes01.etfuturum.unittas.tests.GlowViewTests;
import ganymedes01.etfuturum.unittas.tests.PhantomTests;
import ganymedes01.etfuturum.unittas.tests.PhantomSpawnTests;
import ganymedes01.etfuturum.unittas.tests.PhantomViewTests;
import ganymedes01.etfuturum.unittas.tests.PotionTests;
import ganymedes01.etfuturum.unittas.tests.BrewingTests;
import ganymedes01.etfuturum.unittas.tests.PotionItemsTests;
import ganymedes01.etfuturum.unittas.tests.ReagentItemsTests;
import ganymedes01.etfuturum.unittas.tests.EffectsTests;
import ganymedes01.etfuturum.unittas.tests.EffectSaveTests;
import ganymedes01.etfuturum.unittas.tests.EffectNetworkTests;
import ganymedes01.etfuturum.unittas.tests.TrialEffectsTests;
import ganymedes01.etfuturum.unittas.tests.ChestTests;
import ganymedes01.etfuturum.unittas.tests.OldSaveTests;
import ganymedes01.etfuturum.unittas.tests.OldWorldTests;
import ganymedes01.etfuturum.unittas.tests.QuickWins5ViewTests;
import ganymedes01.etfuturum.unittas.tests.AttachTests;
import ganymedes01.etfuturum.unittas.tests.BedTests;
import ganymedes01.etfuturum.unittas.tests.BlockTests;
import ganymedes01.etfuturum.unittas.tests.CameraTests;
import ganymedes01.etfuturum.unittas.tests.ChatTests;
import ganymedes01.etfuturum.unittas.tests.CombatTests;
import ganymedes01.etfuturum.unittas.tests.ComponentTests;
import ganymedes01.etfuturum.unittas.tests.ControlsTests;
import ganymedes01.etfuturum.unittas.tests.DamageTests;
import ganymedes01.etfuturum.unittas.tests.EffectTests;
import ganymedes01.etfuturum.unittas.tests.EnchantTests;
import ganymedes01.etfuturum.unittas.tests.EndTests;
import ganymedes01.etfuturum.unittas.tests.GiveTests;
import ganymedes01.etfuturum.unittas.tests.InventoryReport;
import ganymedes01.etfuturum.unittas.tests.ItemIdTests;
import ganymedes01.etfuturum.unittas.tests.LightningTests;
import ganymedes01.etfuturum.unittas.tests.LodestoneTests;
import ganymedes01.etfuturum.unittas.tests.LoyaltyImpalingTests;
import ganymedes01.etfuturum.unittas.tests.OffhandSwingTests;
import ganymedes01.etfuturum.unittas.tests.OptionsTests;
import ganymedes01.etfuturum.unittas.tests.PlacementShapeTests;
import ganymedes01.etfuturum.unittas.tests.PolarBearTests;
import ganymedes01.etfuturum.unittas.tests.PowderSnowCauldronTests;
import ganymedes01.etfuturum.unittas.tests.PowderSnowTests;
import ganymedes01.etfuturum.unittas.tests.PowderSnowViewTests;
import ganymedes01.etfuturum.unittas.tests.PolarBearViewTests;
import ganymedes01.etfuturum.unittas.tests.QuickWins3Tests;
import ganymedes01.etfuturum.unittas.tests.QuickWins4Tests;
import ganymedes01.etfuturum.unittas.tests.QuickWins4ViewTests;
import ganymedes01.etfuturum.unittas.tests.RiptideTests;
import ganymedes01.etfuturum.unittas.tests.SelectorTests;
import ganymedes01.etfuturum.unittas.tests.ShieldTests;
import ganymedes01.etfuturum.unittas.tests.SneakEdgeTests;
import ganymedes01.etfuturum.unittas.tests.StepSoundTests;
import ganymedes01.etfuturum.unittas.tests.SummonTests;
import ganymedes01.etfuturum.unittas.tests.ToggleKeyTests;
import ganymedes01.etfuturum.unittas.tests.TorchTests;
import ganymedes01.etfuturum.unittas.tests.TotemTests;
import ganymedes01.etfuturum.unittas.tests.TridentTests;
import ganymedes01.etfuturum.unittas.tests.TridentViewTests;
import ganymedes01.etfuturum.unittas.tests.VanillaCommandTests;
import ganymedes01.etfuturum.unittas.tests.VehicleTests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The suites ./unittas_mcfly.sh can ask for, each a list of test classes run in this order. A new test class goes here. */
final class TasSuites {

	private static final Map<String, List<Class<? extends TasTestCase>>> SUITES = new LinkedHashMap<>();

	static {
		SUITES.put("commands", Arrays.asList(ChatTests.class, EffectTests.class, SelectorTests.class, EnchantTests.class,
			GiveTests.class, ComponentTests.class, ItemIdTests.class, SummonTests.class, BlockTests.class, VanillaCommandTests.class));
		SUITES.put("combat", Arrays.asList(CombatTests.class, DamageTests.class, ShieldTests.class));
		// the batch of small fixes of 2026-10-30 (spec §53)
		SUITES.put("quick", Arrays.asList(EndTests.class, BedTests.class, AttachTests.class, CameraTests.class, OffhandSwingTests.class,
			StepSoundTests.class, SneakEdgeTests.class, PlacementShapeTests.class, VehicleTests.class));
		// batch 2 (spec §55), written before the fixes
		SUITES.put("quick2", Arrays.asList(AnvilTests.class, TotemTests.class));
		// batch 3 (spec §61), written before the fixes
		SUITES.put("quick3", Arrays.asList(QuickWins3Tests.class));
		SUITES.put("quick4", Arrays.asList(QuickWins4Tests.class, ZombieArmsTests.class, QuickWins4ViewTests.class));
		SUITES.put("quick5", Arrays.asList(SkeletonArmsTests.class, GlintTests.class, EfrBlockStateTests.class, StrongholdTests.class, SpyglassTests.class, HorseTests.class, QuickWins5ViewTests.class));
		SUITES.put("glow", Arrays.asList(GlowSquidTests.class, SignTextTests.class, ItemFrameTests.class, GlowViewTests.class));
		SUITES.put("phantom", Arrays.asList(PhantomTests.class, PhantomSpawnTests.class, PhantomViewTests.class));
		SUITES.put("potions", Arrays.asList(PotionTests.class, BrewingTests.class, PotionItemsTests.class, ReagentItemsTests.class));
		SUITES.put("effects", Arrays.asList(EffectsTests.class, EffectSaveTests.class, EffectNetworkTests.class, TrialEffectsTests.class));
		SUITES.put("chests", Arrays.asList(ChestTests.class));
		SUITES.put("oldworld", Arrays.asList(OldWorldTests.class));
		// vanilla 1.7.10's worlds opened with the release jar (spec §75): only in save mode, skipped otherwise
		SUITES.put("oldsaves", Arrays.asList(OldSaveTests.class));
		// banners as one category (spec §66): EFR's banners as they are, the loom
		// the End as 26.3 (spec §76)
		// our own base (spec §79): the base class and our options in a file of our own
		SUITES.put("base", Arrays.asList(ganymedes01.etfuturum.unittas.tests.BaseTests.class));
		SUITES.put("end", Arrays.asList(ganymedes01.etfuturum.unittas.tests.EndTerrainTests.class, ganymedes01.etfuturum.unittas.tests.EndFightTests.class, ganymedes01.etfuturum.unittas.tests.EndRespawnTests.class, ganymedes01.etfuturum.unittas.tests.EndDragonTests.class, ganymedes01.etfuturum.unittas.tests.EndBreathTests.class, ganymedes01.etfuturum.unittas.tests.EndGatewayTests.class, ganymedes01.etfuturum.unittas.tests.EndFeatureTests.class, ganymedes01.etfuturum.unittas.tests.StructureTemplateTests.class, ganymedes01.etfuturum.unittas.tests.EndCityTests.class, ganymedes01.etfuturum.unittas.tests.LootTests.class, ganymedes01.etfuturum.unittas.tests.DragonHeadTests.class, ganymedes01.etfuturum.unittas.tests.ShulkerTests.class, ganymedes01.etfuturum.unittas.tests.EndViewTests.class));
		SUITES.put("banners", Arrays.asList(ganymedes01.etfuturum.unittas.tests.BannerTests.class, ganymedes01.etfuturum.unittas.tests.LoomTests.class, ganymedes01.etfuturum.unittas.tests.BannerViewTests.class, ganymedes01.etfuturum.unittas.tests.ShieldPatternTests.class));
		// parked by the user (2026-10-30): EFR's observer clock pulses 1 tick in 3, 26.3's code gives 2-tick pulses
		SUITES.put("redstone", Arrays.asList(TorchTests.class));
		// modern options (spec §54), growing with each stage: stage 1 (§54.10), stage 2a, the Hold/Toggle keys (§54.11)
		SUITES.put("options", Arrays.asList(OptionsTests.class, ToggleKeyTests.class, ControlsTests.class));
		// lodestones and lodestone compasses (spec §57)
		SUITES.put("lodestone", Arrays.asList(LodestoneTests.class));
		// the trident (spec §58), by stage: 1 the trident itself, 2 Riptide, 3 Channeling and lightning, 4 Loyalty and Impaling
		SUITES.put("trident", Arrays.asList(TridentTests.class, RiptideTests.class, LightningTests.class, LoyaltyImpalingTests.class, TridentViewTests.class));
		// the polar bear, the first new mob (spec §60)
		SUITES.put("polarbear", Arrays.asList(PolarBearTests.class, PolarBearViewTests.class));
		SUITES.put("powdersnow", Arrays.asList(PowderSnowTests.class, PowderSnowCauldronTests.class, PowderSnowViewTests.class));
		// unitTAS's own machinery (the loading trace, handoff upcoming 39)
		SUITES.put("harness", Arrays.asList(HarnessTests.class));
		// not tests: lists for planning, written to the results folder
		SUITES.put("inventory", Arrays.asList(InventoryReport.class));
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
