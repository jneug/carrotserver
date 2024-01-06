package schule.ngb.carrot;

import dorkbox.annotation.AnnotationDefaults;
import dorkbox.annotation.AnnotationDetector;
import schule.ngb.carrot.protocol.*;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Erstellt eine Liste mit Protokollen (bzw. {@link ProtocolHandler}n), die bei diesem Start vom
 * Server verfügbar gemacht werden sollen.
 * <p>
 * Dazu werden die nativen Protokolle aus {@link schule.ngb.carrot.protocol} geladen, sowie
 * mögliche Erweiterungen, die im {@code DATA_PATH} abgelegt wurden.
 * <p>
 * Erweiterungen können vorkompilierte {@link .class} Dateien sein, oder {@code .java} Dateien, die
 * dynamisch bei Programmstart kompiliert werden. Eine Protokoll-Erweiterung muss das
 * {@link ProtocolHandler} Interface implementieren und mit der {@link Protocol} Annotation versehen
 * sein.
 */
public final class ProtocolLoader {

	// Logger
	private static final Log LOG = Log.getLogger(ProtocolLoader.class);


	/**
	 * Globaler, einheitlicher Pfad für Erweiterungen.
	 */
	public static final String EXT_PATH = "extensions";

	/**
	 * Globaler, einheitlicher Pfad für Protokoll-Erweiterungen.
	 */
	public static final String EXT_PROTOCOLS_PATH = "protocols";


	/**
	 * Globale Konfiguration
	 */
	private final Configuration config;

	/**
	 * Menge der aktiven Protokolle, die in der Konfiguration definiert wurden.
	 */
	private final Set<String> activeProtocols;

	/**
	 * Erstellt einen {@code ProtocolLoader} mit der angegebenen Konfiguration.
	 * <p>
	 * Die Konfiguration bestimmt den {@code DATA} Pfad für das Laden von Erweiterungen, sowie die
	 * Liste der aktiven Protokolle ({@code PROTOCOLS}). Ist die Liste der Protokolle leer, werden
	 * alle eingebauten Protokolle und Erweiterungen geladen.
	 *
	 * @param config Globale konfiguration der App.
	 */
	public ProtocolLoader( Configuration config ) {
		this.config = config;

		// Menge der aktiven Protokolle vorbereiten (liegt als String-Array vor).
		String[] configProtocols = config.getStringArray("PROTOCOLS");
		if( configProtocols != null ) {
			activeProtocols = Arrays.stream(configProtocols).map(String::toUpperCase).collect(Collectors.toSet());
			LOG.debug("Configured to load protocols %s", activeProtocols);
		} else {
			activeProtocols = Set.of();
		}
	}

	/**
	 * Liefert eine Liste aller vorhandenen Protokoll-Erweiterungen.
	 * <p>
	 * Die Rückgabe ist eine Liste von {@link ProtocolHandlerFactory} Instanzen, die für die
	 * Instanziierung der {@link ProtocolHandler} zuständig sind. Eine Erweiterung kann in der
	 * {@link Protocol} Annotation eine eigene Factory angeben. Ansonsten wird eine generische
	 * {@link GenericProtocolHandlerFactory} verwendet.
	 *
	 * @return Die Liste der geladenen Erweiterungen.
	 */
	public List<ProtocolHandlerFactory> loadExtensionProtocols() {
		// Pfad der Protokoll-Erweiterungen
		Path extPath = Paths.get(
			config.getString("DATA"),
			EXT_PATH,
			EXT_PROTOCOLS_PATH
		).toAbsolutePath();

		// Vorbereiten der Rückgabe.
		List<ProtocolHandlerFactory> extensionList = new ArrayList<>();

		if( Files.isDirectory(extPath) ) {
			if( config.getBool("DYNAMIC_COMPILATION") ) {
				// Falls dynamische Kompilierung aktiviert ist,
				try( Stream<Path> fileList = Files.list(extPath) ) {
					fileList
						// nach zu kompilierenden Dateien suchen
						.filter(( p ) -> p.getFileName().toString().endsWith(".java"))
						.filter(this::checkCompilationsRequirements)
						// und kompilieren.
						.forEach(this::compile);
				} catch( IOException e ) {
					LOG.error(e, "Error during compilation task");
				}
			}

			try( URLClassLoader classLoader = new URLClassLoader(new URL[]{
				extPath.toUri().toURL()
			}) ) {
				// Erweiterungsklassen laden und nach Annotation durchsuchen
				List<Class<?>> protocols = AnnotationDetector
					.scanFiles(classLoader, extPath.toFile())
					.forAnnotations(Protocol.class)
					.on(ElementType.TYPE)
					.filter(( File dir, String name ) -> Paths.get(dir.toURI()).startsWith(extPath))
					.collect(AnnotationDefaults.getType);

				// Factory-Klassen instanziieren.
				for( Class<?> ph : protocols ) {
					try {
						@SuppressWarnings( "unchecked" )
						ProtocolHandlerFactory phf = instantiateFactory((Class<? extends ProtocolHandler>) ph, classLoader);
						if( phf != null && (!activeProtocols.isEmpty() || activeProtocols.contains(phf.getName().toUpperCase())) ) {
							extensionList.add(phf);
							LOG.debug("Added extension protocol %s", phf.getName());
						}
					} catch( ClassCastException e ) {
						LOG.error(e, "Error creating extension protocol factory");
					}
				}
			} catch( IOException e ) {
				LOG.error(e, "Error loading extensions");
			}
		}

		return extensionList;
	}


	/**
	 * Liefert eine Liste aller in der App eingebauten Protokolle im Paket
	 * {@link schule.ngb.carrot.protocol}.
	 * <p>
	 * Die Rückgabe ist eine Liste von {@link ProtocolHandlerFactory} Instanzen, die für die
	 * Instanziierung der {@link ProtocolHandler} zuständig sind.
	 *
	 * @return Die Liste der geladenen Protokolle.
	 */
	public List<ProtocolHandlerFactory> loadBuildinProtocols() {
		List<ProtocolHandlerFactory> protocolList = new ArrayList<>(6);

		try {
			// Paket nach Annotation durchsuchen
			List<Class<?>> protocols = AnnotationDetector.scanClassPath("schule.ngb.carrot.protocol")
				.forAnnotations(Protocol.class)
				.on(ElementType.TYPE)
				.collect(AnnotationDefaults.getType);

			// Factory-Klassen instanziieren.
			for( Class<?> ph : protocols ) {
				try {
					@SuppressWarnings( "unchecked" )
					ProtocolHandlerFactory phf = instantiateFactory((Class<? extends ProtocolHandler>) ph);
					if( phf != null && (!activeProtocols.isEmpty() || activeProtocols.contains(phf.getName().toUpperCase())) ) {
						protocolList.add(phf);
						LOG.debug("Added protocol %s", phf.getName());
					} else if( phf != null ){
						LOG.debug("Skipped protocol %s, not in configuration", phf.getName());
					} else {
						LOG.warn("Failed to add protocol for %s", ph.getSimpleName());
					}
				} catch( ClassCastException e ) {
					LOG.error(e, "Error creating protocol factory");
				}
			}
		} catch( IOException e ) {
			LOG.error(e, "Error loading classes");
		}
		return protocolList;
	}

	/**
	 * Instanziiert eine {@link ProtocolHandlerFactory} für den angegebenen {@link ProtocolHandler}
	 * und verwendet dazu den {@link ClassLoader#getSystemClassLoader() ClassLoader des Systems}.
	 *
	 * @param protocolClass Die Protokoll-Klasse.
	 * @return Eine {@code ProtocolHandlerFactory}-Instanz, die {@code protocolClass}-Instanzen
	 * 	erzeugt.
	 */
	private ProtocolHandlerFactory instantiateFactory( Class<? extends ProtocolHandler> protocolClass ) {
		return instantiateFactory(protocolClass, ClassLoader.getSystemClassLoader());
	}

	/**
	 * Instanziiert eine {@link ProtocolHandlerFactory} für den angegebenen {@link ProtocolHandler}
	 * und verwendet dazu den angegebenen {@link ClassLoader}.
	 *
	 * @param protocolClass Die Protokoll-Klasse.
	 * @return Eine {@code ProtocolHandlerFactory}-Instanz, die {@code protocolClass}-Instanzen
	 * 	erzeugt.
	 */
	private ProtocolHandlerFactory instantiateFactory( Class<? extends ProtocolHandler> protocolClass, ClassLoader classLoader ) {
		Class<? extends ProtocolHandlerFactory> factory = GenericProtocolHandlerFactory.class;

		// Annotation der Klasse auf Factory-Klasse prüfen.
		Protocol annotation = protocolClass.getAnnotation(Protocol.class);
		if( annotation != null ) {
			factory = annotation.factory();
		}

		// Instanz erstellen.
		try {
			if( factory.equals(GenericProtocolHandlerFactory.class) ) {
				Constructor<?> constr = factory.getDeclaredConstructor(Configuration.class, Class.class);
				Object phf = constr.newInstance(this.config, protocolClass);
				return (ProtocolHandlerFactory) phf;
			} else {
				Constructor<?> constr = factory.getDeclaredConstructor(Configuration.class);
				Object phf = constr.newInstance(this.config);
				return (ProtocolHandlerFactory) phf;
			}
		} catch( NoSuchMethodException | InstantiationException | IllegalAccessException |
				 InvocationTargetException e ) {
			LOG.error(e, "Error instantiating protocol factory %s", factory.getSimpleName());
			return null;
		}
	}

	/**
	 * Kompiliert eine ".java" Datei.
	 *
	 * @param javaFile Pfad zur Java-Datei.
	 * @return Ob eine class-Datei vorhanden ist.
	 */
	private boolean compile( Path javaFile ) {
		if( !checkCompilationsRequirements(javaFile) ) {
			return true;
		}

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

		List<String> optionList = new ArrayList<>();
		optionList.add("-classpath");
		optionList.add(System.getProperty("java.class.path"));

		Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjects(javaFile);
		JavaCompiler.CompilationTask task = compiler.getTask(
			null,
			fileManager,
			diagnostics,
			optionList,
			null,
			compilationUnit);

		if( !task.call() ) {
			for( Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics() ) {
				LOG.warn("%s during compilation on line %d in %s:\n%s",
					diagnostic.getKind(),
					diagnostic.getLineNumber(),
					diagnostic.getSource().toUri(),
					diagnostic.getMessage(Locale.getDefault())
				);
			}
			return false;
		}

		LOG.info("Successfully compiled extension %s", javaFile.getFileName());
		return true;
	}

	/**
	 * Prüft, ob die angegebene Java-Datei kompiliert werden muss.
	 * <p>
	 * Die Methode liefert {@code true}, wenn keine passende class-Datei existiert, oder wenn das
	 * Änderungsdatum der java-Datei nach dem der class-Datei liegt.
	 *
	 * @param javaFile Pfad zu einer Java-Datei.
	 * @return {@code true}, wenn die Datei kompiliert werden muss, {@code false} sonst.
	 */
	private boolean checkCompilationsRequirements( Path javaFile ) {
		// Get Path of .class file
		String fileName = javaFile.getFileName().toString();
		int indexOfDot = fileName.lastIndexOf('.');
		if( indexOfDot >= 0 ) {
			fileName = fileName.substring(0, indexOfDot);
		}
		Path classFile = javaFile.resolveSibling(fileName + ".class");

		try {
			return !Files.exists(classFile) ||
				Files.getLastModifiedTime(classFile).compareTo(Files.getLastModifiedTime(javaFile)) < 0;
		} catch( IOException e ) {
			return false;
		}
	}


}
