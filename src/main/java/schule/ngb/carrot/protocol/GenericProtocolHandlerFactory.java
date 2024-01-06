package schule.ngb.carrot.protocol;

import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.file.Paths;

public class GenericProtocolHandlerFactory implements ProtocolHandlerFactory {

	// Logger
	private static final Log LOG = Log.getLogger(GenericProtocolHandlerFactory.class);


	/**
	 * Interner Name des Protokolls.
	 */
	protected String name;

	/**
	 * Eingestellter Port des Dienstes.
	 */
	private int port = 0;

	/**
	 * Dateiname der Konfigurationsdatei, die geladen werden soll.
	 */
	private String configFile = null;

	/**
	 * Konfiguration des Protokolls.
	 */
	protected final Configuration config;

	/**
	 * Klasse des
	 */
	protected final Class<? extends ProtocolHandler> type;

	public GenericProtocolHandlerFactory( Configuration globalConfig, Class<? extends ProtocolHandler> type ) {
		this.type = type;

		processAnnotation();

		this.config = Configuration
			.from(globalConfig)
			.load(type.getResourceAsStream(this.configFile))
			.load(Paths.get(globalConfig.getString("DATA"), this.configFile))
			.get();
		// Look for custom port in configuration
		if( this.config.containsKey("PORT") ) {
			this.port = this.config.getInt("PORT");
		}
	}

	/**
	 * Prüft den {@link #type }ProtocolHandler} auf die {@link Protocol} Annotation und lädt die
	 * bereitgestellten Daten.
	 */
	private void processAnnotation() {
		String name = "", configFile = "";

		Protocol annotation = type.getAnnotation(Protocol.class);
		if( annotation != null ) {
			name = annotation.name();
			configFile = annotation.config();
			this.port = annotation.port();
		}
		this.name = name.isEmpty() ? type.getSimpleName() : name;
		this.configFile = configFile.isBlank() ? this.name.toLowerCase() + ".config" : configFile;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return this.port;
	}

	public Configuration getConfig() {
		return config;
	}

	@Override
	public ProtocolHandler create( Socket clientSocket ) {
		try {
			return type.getDeclaredConstructor(Socket.class, Configuration.class).newInstance(
				clientSocket, this.config
			);
		} catch( InstantiationException | IllegalAccessException | InvocationTargetException |
				 NoSuchMethodException e ) {
			LOG.error(e, "Failed to instantiate protocol class %s", type.getSimpleName());
			throw new RuntimeException(e);
		}
	}

}
