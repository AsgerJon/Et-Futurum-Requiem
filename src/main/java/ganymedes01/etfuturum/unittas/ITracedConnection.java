package ganymedes01.etfuturum.unittas;

/**
 * A connection named in the loading trace (TasTrace; `mixins.early.unittas.client.MixinNetworkManagerTrace` adds it to
 * NetworkManager): which one it is and its channel's state, so a trace tells two connections apart.
 */
public interface ITracedConnection {

	/** "#1a2b3c server, channel none yet | open | CLOSED". */
	String etfu$traceDescription();

	static String describe(Object connection) {
		return connection instanceof ITracedConnection traced ? traced.etfu$traceDescription()
			: "#" + Integer.toHexString(System.identityHashCode(connection));
	}
}
