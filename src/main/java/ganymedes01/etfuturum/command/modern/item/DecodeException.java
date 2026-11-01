package ganymedes01.etfuturum.command.modern.item;

/** A component value the component can't take; the message is the reason, in English as modern's are (spec §45.2). */
public final class DecodeException extends Exception {

	public DecodeException(String message) {
		super(message);
	}
}
