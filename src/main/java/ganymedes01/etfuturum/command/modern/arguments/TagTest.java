package ganymedes01.etfuturum.command.modern.arguments;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

/** `#tag[name=value]{data}` (spec §49.1): a block of the tag that has each named property at that value, and the data. */
final class TagTest implements BlockTest {

	private final List<BlockIds.Type> members;
	private final Map<String, String> properties;
	private final NBTTagCompound data;

	TagTest(List<BlockIds.Type> members, Map<String, String> properties, NBTTagCompound data) {
		this.members = members;
		this.properties = properties;
		this.data = data;
	}

	@Override
	public boolean test(World world, int x, int y, int z) {
		BlockIds.Found found = BlockIds.find(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
		if (found == null || !members.contains(found.type())) {
			return false;
		}
		for (Map.Entry<String, String> property : properties.entrySet()) {
			String name = property.getKey();
			if (!found.type().properties.containsKey(name)) {
				return false;
			}
			if (found.type().isStored(name) && !property.getValue().equals(found.state().properties().get(name))) {
				return false;
			}
		}
		return BlockTest.dataMatches(world, x, y, z, data);
	}
}
