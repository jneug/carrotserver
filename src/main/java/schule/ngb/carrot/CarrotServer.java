package schule.ngb.carrot;

import org.apache.commons.cli.CommandLine;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.*;

/**
 * Hauptklasse von <strong>CarrotServ</strong>.
 * <p>
 * Lädt {@link Configuration Konfigurationsdateien} und {@link ProtocolLoader Protokolle} und
 * initialisiert die {@link Server}, sowie die {@link CarrotGUI GUI}.
 */
public class CarrotServer {

	/**
	 * Eintrittspunkt der App.
	 *
	 * @param args Kommandozielenargumente
	 */
	public static void main( String[] args ) throws IOException {
		// Kommandozeile parsen (Apache commons-cli)
		CommandLine cli = Configuration.parseCli(args);

		// Kommandozeile auf spezifische Config-Datei prüfen
		Path configFile = Paths.get(System.getProperty("user.dir"), CONFIG_FILE);
		if( cli != null && cli.hasOption("config") ) {
			configFile = Paths.get(cli.getOptionValue("config"));
			if( !configFile.isAbsolute() ) {
				configFile = Paths.get(System.getProperty("user.dir")).resolve(configFile);
			}
		}

		// App-Konfiguration aufbauen, beginnend mit der Default-Konfiguration (ini4j)
		Ini config;
		config = Configuration.from()
			.load(CarrotServer.class.getResourceAsStream(CONFIG_FILE))
			.load(configFile)
			.build();
		// Der DATA_PATH könnte sich bis hier geändert haben, dort nach carrot.config suchen.
		config = Configuration.from(config)
			.load(Paths.get(config.get(Configuration.SECTION_MAIN, "data"), CONFIG_FILE))
			.load(cli)
			.build();
		// Statische App-Informationen ergänzen
		Profile.Section appSection = config.get(Configuration.SECTION_MAIN).addChild("app");
		appSection.put("name", CarrotServer.APP_NAME);
		appSection.put("version", CarrotServer.APP_VERSION);

		// ggf. DEBUG-Modus einschalten.
		if( config.get(Configuration.SECTION_MAIN, "debug", boolean.class) ) {
			Log.enableGlobalDebugging();

			FileHandler fh = new FileHandler("carrot.log", true);
			fh.setFormatter(new SimpleFormatter());

			Logger rootLogger = Logger.getLogger("");
			rootLogger.addHandler(fh);
		}

		// Start der App
		CarrotServer app = new CarrotServer(config);
		app.start();
		// ggf. GUI initialisieren
		if( !GraphicsEnvironment.isHeadless() && !config.get(Configuration.SECTION_MAIN, "headless", boolean.class) ) {
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
	 * Globaler, einheitlicher Name der Konfigurationsdateien.
	 */
	public static final String CONFIG_FILE = "carrot.config";


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
		LOG.debug("Starting %s with configuration:", APP_NAME);
		LOG.debug(this.config::toString);

		if( !this.config.get("users").isEmpty() ) {
			LOG.debug("Possible logins:");
			for( String username : config.get("users").keySet() ) {
				LOG.debug("    %s", username);
			}
		} else {
			LOG.warn("No authentication data found in config. You won't be able to log in.");
		}

		try {
			LOG.info("%s running on %s / 127.0.0.1", APP_NAME, InetAddress.getLocalHost().getHostAddress());
		} catch( UnknownHostException ignored ) {
		}
	}

	/**
	 * Erstellt eine GUI für die App und zeigt sie an.
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
		public void serverStarted( ServerEvent e ) {
			LOG.info("Started service %s on port %d", e.server.getProtocolName(), e.server.getPort());
		}

		@Override
		public void serverStopped( ServerEvent e ) {
			LOG.info("Stopped service %s on port %d", e.server.getProtocolName(), e.server.getPort());
		}

		@Override
		public void clientConnected( ServerEvent e ) {
			LOG.info("%s connected on port %d (%s)", e.clientHandler.getSocket().getRemoteSocketAddress(), e.server.getPort(), e.server.getProtocolName());
		}

		@Override
		public void clientDisconnected( ServerEvent e ) {
			LOG.info("%s disconnected on port %d (%s)", e.clientHandler.getSocket().getRemoteSocketAddress(), e.server.getPort(), e.server.getProtocolName());
		}

		@Override
		public void clientTimeout( ServerEvent e ) {
			LOG.info("%s timed out on port %d (%s)", e.clientHandler.getSocket().getRemoteSocketAddress(), e.server.getPort(), e.server.getProtocolName());
		}

	};

}
