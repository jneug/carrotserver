package schule.ngb.carrot;

import org.apache.commons.cli.*;
import org.ini4j.Ini;
import org.ini4j.Profile;
import schule.ngb.carrot.events.ServerEvent;
import schule.ngb.carrot.events.ServerListener;
import schule.ngb.carrot.gui.CarrotGUI;
import schule.ngb.carrot.protocol.ProtocolHandlerFactory;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Hauptklasse von <strong>CarrotServ</strong>.
 * <p>
 * Lädt {@link Configuration Konfigurationsdateien} und {@link ProtocolLoader Protokolle} und
 * initislisiert die {@link Server}, sowie die {@link CarrotGUI GUI}.
 */
public class CarrotServer {

	/**
	 * Eintrittspunkt der App.
	 *
	 * @param args Kommandozielenargumente
	 */
	public static void main( String[] args ) {
		// Kommandozeile parsen
		CommandLine cli = Configuration.parseCli(args);

		// Kommandozeile auf spezifische Config-Datei prüfen
		Path configFile = Paths.get(System.getProperty("user.dir"), CONFIG_FILE);
		if( cli != null && cli.hasOption("config") ) {
			configFile = Paths.get(cli.getOptionValue("config"));
			if( !configFile.isAbsolute() ) {
				configFile = Paths.get(System.getProperty("user.dir")).resolve(configFile);
			}
		}

		// App-Konfiguration aufbauen, beginnend mit der Default-Konfiguration
		Ini config;
		config = Configuration.from()
			.load(CarrotServer.class.getResourceAsStream(CONFIG_FILE))
			.load(configFile)
			.build();
		config = Configuration.from(config)
			.load(Paths.get(config.get("carrot", "data"), CONFIG_FILE))
			.load(cli)
			.build();
		// App information for dynamic replacements
		Profile.Section appSection = config.get("carrot").addChild("app");
		appSection.put("name", CarrotServer.APP_NAME);
		appSection.put("version", CarrotServer.APP_VERSION);

		// DEBUG-Modus einschalten.
		if( config.get("carrot", "debug", boolean.class) ) {
			Log.enableGlobalDebugging();
		}

		// Start der App
		CarrotServer app = new CarrotServer(config);
		app.start();
		if( !GraphicsEnvironment.isHeadless() && !config.get("carrot", "headless", boolean.class) ) {
			app.createGUI();
		}
	}

	/**
	 * Globaler Name der App.
	 */
	public static final String APP_NAME = "CarrotServer";

	/**
	 * Globale Version der App.
	 */
	public static final String APP_VERSION = "0.0.9";

	/**
	 * Globaler Pfad zum DATA-Ordner. Der {@code DATA_PATH} ist immer fest gesetzt.
	 */
	public static final String DATA_PATH = "data";

	/**
	 * Globaler, einheitlicher Name der Konfigurationsdateien.
	 */
	public static final String CONFIG_FILE = "carrot.config";

	/**
	 * Globaler, einheitlicher Name für die Datei mit Authentifizierungs-Daten.
	 */
	public static final String AUTH_FILE = "users.config";


	// Logger
	private static final Log LOG = Log.getLogger(CarrotServer.class);

	/**
	 * Globale Konfiguration der App.
	 */
	private final Ini config;

	/**
	 * Liste der verfügbaren Dienste (Protokolle) bei diesem Start der App.
	 */
	private List<Server> services;

	/**
	 * Erstellt die Hauptklasse der App.
	 *
	 * @param globalConfig Die globale Konfiguration, mit der diese App-Instanz läuft.
	 */
	public CarrotServer( Ini globalConfig ) {
		this.config = globalConfig;

		// Debugging-Ausgabe
//		if( !authConfig.isEmpty() ) {
//			LOG.debug("Authentication data loaded from %s.", authPath);
//			LOG.debug("Possible logins:");
//			for( String username : config.getConfig("USERS").keys() ) {
//				LOG.debug("    %s", username);
//			}
//		} else {
//			LOG.warn("No %s file found in the data dir. You won't be able to log in.", AUTH_FILE);
//		}
	}

	/**
	 * Erstellt eine GUI für dei App und zeigt sie an.
	 *
	 * @return Die neu erstellte GUI-Instanz.
	 */
	public CarrotGUI createGUI() {
		CarrotGUI.setLookAndFeel();
		CarrotGUI gui = new CarrotGUI(APP_NAME, this, this.config);
		gui.setVisible(true);
		return gui;
	}

	/**
	 * Startet die App.
	 * <p>
	 * Lädt die verfügbaren Protokolle für diesen Start und kompiliert gegebenenfalls vorhandene
	 * Erweiterungen. Die List der Dienste wird an {@link #start(List)} übergeben, um die Dienste zu
	 * starten.
	 */
	public void start() {
		ProtocolLoader protocolLoader = new ProtocolLoader(this.config);
		List<ProtocolHandlerFactory> protocolList = new LinkedList<>();
		// Eingebauten Protokolle laden.
		protocolList.addAll(protocolLoader.loadBuildinProtocols());
		// Erweiterungen laden (und ggf. kompilieren),
		protocolList.addAll(protocolLoader.loadExtensionProtocols());
		// Dienste starten.
		start(protocolList);
	}

	/**
	 * Startet die App.
	 * <p>
	 * Pro Dienst wird ein Socket auf dem eingestellten Port geöffnet.
	 */
	public void start( List<ProtocolHandlerFactory> protocols ) {
		this.services = new ArrayList<>();
		for( ProtocolHandlerFactory phf : protocols ) {
			Server s = new Server(phf.getPort(), phf);
			if( config.get("carrot", "timeout", int.class) > 0 ) {
				s.setConnectionTimeout(config.get("carrot", "timeout", int.class));
			}
			services.add(s);
			s.start();

			s.addListener(serverLogger);
		}
	}

	/**
	 * Liefert eine Liste der Dienste, die in dieser App laufen.
	 *
	 * @return Eine Liste der Dienste.
	 */
	public List<Server> getServices() {
		return services;
	}

	/**
	 * Liefert die globale Konfiguration dieser App.
	 *
	 * @return Die globale Konfiguration.
	 */
	public Ini getConfig() {
		return config;
	}

	/**
	 * Stoppt den Server, beendet alle Dienste und beendet die App.
	 */
	public void shutdown() {
		LOG.info("initialized orderly shutdown, closing open connections");
		for( Server server : services ) {
			server.close();
		}
	}

	// Interner ServerListener, hauptsächlich für das Logging.
	private static final ServerListener serverLogger = new ServerListener() {
		@Override
		public void started( ServerEvent e ) {
			LOG.info("Started service %s on port %d", e.server.getProtocolName(), e.server.getPort());
		}

		@Override
		public void stopped( ServerEvent e ) {
			LOG.info("Stopped service %s on port %d", e.server.getProtocolName(), e.server.getPort());
		}

		@Override
		public void clientConnected( ServerEvent e ) {
		}

		@Override
		public void clientDisconnected( ServerEvent e ) {
		}

		@Override
		public void clientTimeout( ServerEvent e ) {
		}

	};

}
