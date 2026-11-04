package ganymedes01.etfuturum.items.equipment;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModMaterials;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/**
 * The turtle shell (SPEC-modern-movement.md §69.13), 26.3's `turtle_helmet`: a helmet of 2 armour and 275 uses, mended
 * with turtle scutes; worn with its wearer's head out of water it gives Water Breathing for 10 s, topped up each tick
 * (26.3's `Player.turtleHelmetTick`), so it lasts 10 s under water. 26.3 hides that effect's particles; 1.7.10 has no
 * such flag, so it is ambient (fainter), until §72.
 */
public class ItemTurtleShell extends ItemArmor {

	public ItemTurtleShell() {
		super(ModMaterials.TURTLE_ARMOUR, 0, 0);
		setUnlocalizedName(McFly.name("turtle_helmet"));
		setTextureName("turtle_helmet");
		setCreativeTab(EtFuturum.creativeTabItems);
	}

	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
		if (!world.isRemote && !player.isInsideOfMaterial(Material.water)) {
			player.addPotionEffect(new PotionEffect(Potion.waterBreathing.id, 200, 0, true));
		}
	}

	@Override
	public boolean getIsRepairable(ItemStack shell, ItemStack material) {
		return material != null && material.getItem() == McFlyItems.TURTLE_SCUTE.get() || super.getIsRepairable(shell, material);
	}

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
		return "textures/models/armor/turtle_layer_1.png";
	}
}
