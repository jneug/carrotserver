package schule.ngb.carrot;

import org.apache.commons.cli.*;
import schule.ngb.carrot.events.ServerEvent;
import schule.ngb.carrot.events.ServerListener;
import schule.ngb.carrot.gui.CarrotGUI;
import schule.ngb.carrot.protocol.ProtocolHandlerFactory;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
	 * @todo Argumente implementieren für einen headless-start.
	 */
	public static void main( String[] args ) {
		// Prepare options parsing (using apache commons-cli)
		Options options = new Options();
		options.addOption("c", "config", true, "define a path or name of a config file to use");
		options.addOption("h", "host", true, "set the hostname");
		options.addOption("d", "data", true, "set the data storage folder");
		options.addOption(Option.builder("HEADLESS").longOpt("headless").desc("start without gui").build());
		options.addOption(Option.builder("DEBUG").longOpt("debug").desc("show debugging information").build());

		CommandLine cli = null;
		try {
			CommandLineParser parser = new DefaultParser();
			cli = parser.parse(options, args);
		} catch( ParseException ignored ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar " + APP_NAME + ".jar", options);
			System.exit(1);
		}

		// Kommandozeile auf spezifische Config-Datei prüfen
		Path configFile = null;
		if( cli != null && cli.hasOption("config") ) {
			configFile = Paths.get(cli.getOptionValue("config"));
			if( !configFile.isAbsolute() ) {
				configFile = Paths.get(System.getProperty("user.dir")).resolve(configFile);
			}
		} else {
			configFile = Paths.get(System.getProperty("user.dir"), CONFIG_FILE);
		}

		// Die Hauptkonfiguration muss vorab geladen werden,
		// um die DEBUG-Einstellung abzurufen.
		Configuration config = Configuration
			.from(CarrotServer.class.getResourceAsStream(CONFIG_FILE))
			.load(configFile)
			.load(cli)
			.get();

		// Bis hier könnte sich der DATA_PATH geändert haben.
		// Dort noch nach einer Konfiguration schauen.
		config = Configuration.from(config)
			.load(Paths.get(config.getString("DATA", DATA_PATH), CONFIG_FILE))
			.get();


		// DEBUG-Modus einschalten.
		if( config.getBool("DEBUG", false) ) {
			Log.enableGlobalDebugging();
		}

		// Start der App
		CarrotServer app = new CarrotServer(config);
		app.start();
		if( !GraphicsEnvironment.isHeadless() && !config.getBool("HEADLESS") ) {
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
	private final Configuration config;

	/**
	 * Liste der verfügbaren Dienste (Protokolle) bei diesem Start der App.
	 */
	private List<Server> services;

	/**
	 * Erstellt die Hauptklasse der App.
	 *
	 * @param globalConfig Die globale Konfiguration, mit der diese App-Instanz läuft.
	 */
	public CarrotServer( Configuration globalConfig ) {
		this.config = globalConfig;

		// Auth-DAten laden und der Konfiguration hinzufügen.
		Path authPath = Paths.get(config.getString("DATA", DATA_PATH), AUTH_FILE);
		Configuration authConfig = Configuration.from(authPath).get();
		config.set("USERS", authConfig);

		// Debugging-Ausgabe
		if( !authConfig.isEmpty() ) {
			LOG.debug("Authentication data loaded from %s.", authPath);
			LOG.debug("Possible logins:");
			for( String username : config.getConfig("USERS").keys() ) {
				LOG.debug("    %s", username);
			}
		} else {
			LOG.warn("No %s file found in the data dir. You won't be able to log in.", AUTH_FILE);
		}
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
			if( config.getInt("TIMEOUT") > 0 ) {
				s.setConnectionTimeout(config.getInt("TIMEOUT"));
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
	public Configuration getConfig() {
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
			LOG.info("Started service %s on port %d", e.server.getName(), e.server.getPort());
		}

		@Override
		public void stopped( ServerEvent e ) {
			LOG.info("Stopped service %s on port %d", e.server.getName(), e.server.getPort());
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
