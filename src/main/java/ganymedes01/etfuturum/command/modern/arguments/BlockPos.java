package ganymedes01.etfuturum.command.modern.arguments;

import com.github.bsideup.jabel.Desugar;

/** A block's position. */
@Desugar
public record BlockPos(int x, int y, int z) {
}
