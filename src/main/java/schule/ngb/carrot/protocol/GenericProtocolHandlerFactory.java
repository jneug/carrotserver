package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

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
	protected final Ini config;

	protected final Class<? extends ProtocolHandler> type;

	public GenericProtocolHandlerFactory( Ini globalConfig, Class<? extends ProtocolHandler> type ) {
		this.type = type;

		processAnnotation();

		this.config = this.loadConfig(globalConfig);

		LOG.debug("Created %s with configuration:", getClass().getSimpleName());
		LOG.debug(this.config::toString);

		// Look for custom port in configuration
		if( this.config.get(this.name, "port") != null ) {
			this.port = this.config.get(this.name, "port", int.class);
		}
	}

	/**
	 * Lädt die Konfiguration für dieses spezifische Protokoll, indem die {@var globalConfig} mit
	 * einer eventuell vorhandenen Protokoll-Konfiguration zusammengeführt wird.
	 * <p>
	 * Unterklassen können die Methode überschreiben, um die Konfigurration individuell zu
	 * modifizieren.
	 *
	 * @return Die Konfiguration für dieses Protokoll.
	 */
	protected Ini loadConfig( Ini globalConfig ) {
		return Configuration.from(globalConfig)
			.loadLeft(type.getResourceAsStream(this.configFile))
			.build();
	}

	/**
	 * Prüft den {@link #type ProtocolHandler} auf die {@link Protocol} Annotation und lädt die
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

	public String getConfigFile() {
		return configFile;
	}

	public Ini getConfig() {
		return config;
	}

	@Override
	public ProtocolHandler create( Socket clientSocket ) {
		try {
			return type.getDeclaredConstructor(Socket.class, Ini.class).newInstance(
				clientSocket, this.config
			);
		} catch( InstantiationException | IllegalAccessException | InvocationTargetException |
				 NoSuchMethodException e ) {
			LOG.error(e, "Failed to instantiate protocol class %s", type.getSimpleName());
			throw new RuntimeException(e);
		}
	}

}
