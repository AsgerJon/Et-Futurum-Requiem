package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasChat;
import io.github.asgerjon.unittas.TasCommand;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

import java.util.List;

/** The modern chat input (spec §40.3): suggestions as you type, Tab, the 256-character limit. */
@TasSuite("commands")
public class ChatTests extends TasTestCase {

	public void testSlashListsCommands() {
		List<String> shown = type("/").suggestions();
		screenshot("ChatTests.slash");
		for (String command : new String[]{"effect", "enchant", "give", "summon"}) {
			assertIn(command, shown);
		}
	}

	public void testSelectorsSuggested() {
		List<String> shown = type("/effect give @").suggestions();
		for (String selector : new String[]{"@p", "@a", "@r", "@s", "@e", "@n"}) {
			assertIn(selector, shown);
		}
	}

	public void testTabTakesTheSuggestion() {
		TasChat chat = type("/effect gi").key(Keyboard.KEY_TAB);
		assertTrue(chat.text().startsWith("/effect give"), "the text is " + chat.text());
	}

	public void testEnterRunsTheTypedCommand() {
		TasCommand sent = type("/effect give @s speed").enter();
		assertSaid(sent, "Applied effect Speed to " + playerName());
		assertTrue(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "no Speed");
	}

	public void testLongMessageGoesThroughWhole() {
		StringBuilder message = new StringBuilder("unitTAS long message ");
		while (message.length() < 256) {
			message.append((char) ('a' + message.length() % 26));
		}
		TasChat chat = type(message.toString());
		assertEqual(256, chat.text().length());
		TasCommand sent = chat.enter();
		assertSaid(sent, message.substring(200));
	}

	public void testUnknownEffect() {
		assertError(command("/effect give " + playerName() + " nope"), "Can't find element 'minecraft:nope' of type 'minecraft:mob_effect'");
	}
}
