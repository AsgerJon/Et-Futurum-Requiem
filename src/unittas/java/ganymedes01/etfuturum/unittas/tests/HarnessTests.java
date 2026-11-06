package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import io.github.asgerjon.unittas.TasTrace;

import java.util.List;

/** unitTAS's own machinery: the loading trace (TasTrace, handoff upcoming 39) has its hooks in. */
@TasSuite("harness")
public class HarnessTests extends TasTestCase {

	/** This run's load went through every step, in order; a missing one is a hook that didn't apply. */
	public void testLoadingTraceHasEveryStep() {
		List<String> lines = TasTrace.lines();
		String all = String.join("\n", lines);
		String[] inOrder = {"launching the integrated server", "the server's local endpoint is bound", "client connected to",
				"server handshake: to LOGIN", "server login start", "server accepts the login", "client login success",
				"client joins the game", "the test world is loaded"};
		int at = -1;
		for (String step : inOrder) {
			int found = -1;
			for (int i = at + 1; i < lines.size(); i++) {
				if (lines.get(i).contains(step)) {
					found = i;
					break;
				}
			}
			assertTrue(found >= 0, "no \"" + step + "\" after the steps before it in the trace:\n" + all);
			at = found;
		}
		for (String step : new String[]{"client channel active", "server channel active", "the server's tick lists 1 connection",
				"client sends C00Handshake", "server reads C00Handshake", "server reads C00PacketLoginStart", "server sends S02PacketLoginSuccess",
				"client reads S02PacketLoginSuccess", "the server's network tick runs", "client processes its packets", "server processes its packets"}) {
			assertTrue(all.contains(step), "no \"" + step + "\" in the trace:\n" + all);
		}
	}
}
