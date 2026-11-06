package io.github.asgerjon.mcfly.model;

/**
 * One model drawn at a block (SPEC-modern-movement.md §84), as a 26.3 blockstate's variant or multipart entry names it:
 * the model, its turns (x then y, in degrees, quarter turns) and uvlock.
 */
public final class ModelPart {

	public final BlockModel model;
	public final int turnX;
	public final int turnY;
	public final boolean uvlock;

	public ModelPart(String model, int turnX, int turnY, boolean uvlock) {
		this.model = BlockModel.get(model);
		this.turnX = turnX;
		this.turnY = turnY;
		this.uvlock = uvlock;
	}

	public ModelPart(String model) {
		this(model, 0, 0, false);
	}
}
