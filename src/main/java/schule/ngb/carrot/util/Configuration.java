package schule.ngb.carrot.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Speicher für den Abruf von Konfigurationsvariablen aus Java-Properties-Dateien.
 * <p>
 * Die Klasse arbeitet ähnlich zu {@link Properties}, ergänzt aber Methoden, um direkt verschiedenen
 * Datentypen abzurufen, Konfigurationen aus verschiedenene Quellen zu laden und eine lineare
 * Konfigurationsstruktur zu bauen, bei der Konfigurationsvariablen aus verschiedenen Quellen
 * abgerufen werden können. Auf diese Weise können mehrere Konfigurationen überlagert werden, aber
 * dennoch die Werte der unterliegenden Konfigurationen wiederhergestellt werden.
 * <p>
 * Eine {@code Configuration}-Instanz sollte mithilfe eines {@link ConfigurationBuilder} erstellt
 * werden, indem mehrere Konfigruationen geladen und überlagert werden. Nicht vorhandene Dateien
 * werden dabei ohne Fehler ignoriert.
 *
 * <pre><code>
 * Configuration config = Configuration.from(getClass().getRessource("default.config"))
 * 		.load(Paths.get("user/defaults.config"))
 * 		.load(someInputStream)
 * 		.load(args)
 * 		.get();
 * </code></pre>
 * <p>
 * Die so erzeugte Konfiguration priorisiert die Argumente der Kommandozeile aus {@code args}, dann
 * die über {@code someInputStream} übermittelten Variablen, in den Nutzereinstellungen
 * ({@code user/defaults.config}) und schließlich in den Standardeinstellungen der App
 * ({@code default.config}).
 */
@SuppressWarnings( "unused" )
public class Configuration implements Map<String, Object> {

	/**
	 * Trennzeichen für String-Felder, die serialisierte Arrays enthalten.
	 */
	public static final String ARRAY_DELIMITER = ",";


	/**
	 * @return
	 */
	public static ConfigurationBuilder from() {
		return new ConfigurationBuilder();
	}

	public static ConfigurationBuilder from( Path... paths ) {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		for( Path p : paths ) {
			cb.load(p);
		}
		return cb;
	}

	public static ConfigurationBuilder from( String[] args ) {
		return new ConfigurationBuilder().load(args);
	}

	public static ConfigurationBuilder from( URL u ) {
		return new ConfigurationBuilder().load(u);
	}

	public static ConfigurationBuilder from( InputStream in ) {
		return new ConfigurationBuilder().load(in);
	}

	public static ConfigurationBuilder from( Configuration config ) {
		return new ConfigurationBuilder(config);
	}

	public static String toString( Configuration config ) {
		StringBuilder sb = new StringBuilder();
		sb.append("Configuration[");
		sb.append(config.size());
		sb.append(" (");
		sb.append(config.keys().size());
		sb.append(")]\n  keys:   ");
		sb.append(config.keys().stream().sorted().collect(Collectors.joining(", ")));
		sb.append("\n   keySet: ");
		sb.append(config.keySet().stream().sorted().collect(Collectors.joining(", ")));

		int i = 0;
		Configuration cfg = config;
		while( cfg.hasDefaults() ) {
			cfg = cfg.getDefaults();
			sb.append("\n     defaults ");
			sb.append(++i);
			sb.append(": ");
			sb.append(cfg.keySet().stream().sorted().collect(Collectors.joining(", ")));
		}
		sb.append("\n   values:");
		for( String key : cfg.keys() ) {
			sb.append("\n     ");
			sb.append(key);
			sb.append(" = ");
			sb.append(config.getString(key));
		}

		return sb.toString();
	}


	private Configuration defaults = null;


	private final ConcurrentHashMap<String, Object> data;


	/**
	 * Creates an empty configuration
	 */
	public Configuration() {
		data = new ConcurrentHashMap<>();
	}

	public Configuration( Map<String, Object> copyOf ) {
		this();
		this.data.putAll(copyOf);
	}


	public Configuration getDefaults() {
		return this.defaults;
	}

	public void setDefaults( Configuration defaults ) {
		this.defaults = defaults;
	}

	public boolean hasDefaults() {
		return this.defaults != null;
	}


	public void load( Path p ) throws IOException {
		try( InputStream in = Files.newInputStream(p) ) {
			load(in);
		}
	}

	public void load( InputStream in ) throws IOException {
		Properties prop = new Properties();
		prop.load(in);
		for( String key : prop.stringPropertyNames() ) {
			this.data.put(key, prop.get(key));
		}
	}


	public String getString( String key ) {
		if( data.containsKey(key) ) {
			return (String) data.get(key);
		} else if( defaults != null ) {
			return defaults.getString(key);
		}
		throw new NullPointerException("Key " + key + " is unknown in this configuration");
	}

	public String getString( String key, String defaultValue ) {
		try {
			return getString(key);
		} catch( NullPointerException | ClassCastException ignored ) {
			return defaultValue;
		}
	}

	public int getInt( String key ) {
		String value = getString(key);
		return Integer.parseInt(value);
	}

	public int getInt( String key, int defaultValue ) {
		try {
			return getInt(key);
		} catch( NullPointerException | ClassCastException ignored ) {
			return defaultValue;
		}
	}

	public double getDouble( String key ) {
		String value = getString(key);
		return Double.parseDouble(value);
	}

	public double getDouble( String key, double defaultValue ) {
		try {
			return getDouble(key);
		} catch( NullPointerException | ClassCastException ignored ) {
			return defaultValue;
		}
	}

	public boolean getBool( String key ) {
		String value = getString(key);
		return Boolean.parseBoolean(value);
	}

	public boolean getBool( String key, boolean defaultValue ) {
		try {
			return getBool(key);
		} catch( NullPointerException | ClassCastException ignored ) {
			return defaultValue;
		}
	}

	public void set( String key, String value ) {
		this.data.put(key, value);
	}

	public void set( String key, int value ) {
		set(key, Integer.toString(value));
	}

	public void set( String key, double value ) {
		set(key, Double.toString(value));
	}

	public void set( String key, boolean value ) {
		set(key, Boolean.toString(value));
	}

	public String[] getStringArray( String key ) {
		String value = getString(key);
		return value.split(ARRAY_DELIMITER);
	}

	public int[] getIntArray( String key ) {
		String[] values = getStringArray(key);
		return Arrays.stream(values).mapToInt(Integer::parseInt).toArray();
	}

	public double[] getDoubleArray( String key ) {
		String[] values = getStringArray(key);
		return Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
	}

	public <T> void set( String key, T[] values ) {
		String value = Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(ARRAY_DELIMITER));
		set(key, value);
	}

	public Configuration getConfig( String key ) {
		if( data.containsKey(key) ) {
			return (Configuration) this.data.get(key);
		} else if( defaults != null ) {
			return defaults.getConfig(key);
		}
		throw new NullPointerException("Key " + key + " is unknown in this configuration");
	}

	public void set( String key, Configuration config ) {
		this.data.put(key, config);
	}

	// Map interface
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public boolean containsKey( Object key ) {
		return data.containsKey(key);
	}

	@Override
	public boolean containsValue( Object value ) {
		return data.containsValue(value);
	}


	@Override
	public Object get( Object key ) {
		return data.get(key);
	}

	@Override
	public Object put( String key, Object value ) {
		return data.put(key, value);
	}

	@Override
	public Object remove( Object key ) {
		return data.remove(key);
	}

	public Object remove( Object key, boolean cascade ) {
		Object value = data.remove(key);
		if( cascade && defaults != null ) {
			value = value == null ? defaults.remove(key, cascade) : value;
		}
		return value;
	}

	@Override
	public void putAll( Map<? extends String, ?> m ) {
		data.putAll(m);
	}

	@Override
	public void clear() {
		data.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.data.keySet();
	}

	public Set<String> keys() {
		Set<String> keySet = new HashSet<>(data.keySet());
		if( defaults != null ) {
			keySet.addAll(defaults.keys());
		}
		return keySet;
	}

	@Override
	public Collection<Object> values() {
		return data.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return data.entrySet();
	}


	// Internal Builder class

	private static final Log LOG = Log.getLogger(Configuration.class);

	public static final class ConfigurationBuilder {

		private Configuration config = null;

		public ConfigurationBuilder() {
		}

		public ConfigurationBuilder( Configuration config ) {
			this.config = config;
		}

		private void add( Configuration newConfig ) {
			if( this.config != null && newConfig != null && this.config.size() > 0 ) {
				// Find last default in tree and append there
				Configuration current = newConfig;
				while( current.getDefaults() != null ) {
					current = current.getDefaults();
				}
				current.setDefaults(config);
			}

			if( newConfig != null ) {
				this.config = newConfig;
			}
		}

		public ConfigurationBuilder load( Configuration other ) {
			if( other != null ) {
				Configuration newConfig = new Configuration();
				newConfig.putAll(other);
				add(newConfig);
			}
			return this;
		}

		public ConfigurationBuilder load( InputStream in ) {
			if( in != null ) {
				try {
					Configuration newConfig = new Configuration();
					newConfig.load(in);
					add(newConfig);
				} catch( IOException ignored ) {
					LOG.debug("Failed to load configuration from input stream");
				}
			}
			return this;
		}

		public ConfigurationBuilder load( Path p ) {
			if( p != null ) {
				try( InputStream in = Files.newInputStream(p) ) {
					this.load(in);
				} catch( IOException ignored ) {
					LOG.debug("Failed to load configuration from path %s", p);
				}
			}
			return this;
		}

		public ConfigurationBuilder load( File f ) {
			if( f != null ) {
				try( InputStream in = new FileInputStream(f) ) {
					this.load(in);
				} catch( IOException ignored ) {
					LOG.debug("Failed to load configuration from file %s", f);
				}
			}
			return this;
		}

		public ConfigurationBuilder load( URI u ) {
			if( u != null ) {
				this.load(new File(u));
			}
			return this;
		}

		public ConfigurationBuilder load( URL u ) {
			if( u != null ) {
				try {
					this.load(new File(u.toURI()));
				} catch( URISyntaxException ignored ) {
				}
			}
			return this;
		}

		public ConfigurationBuilder load( Map<Object, Object> map ) {
			Configuration newConfig = new Configuration();
			for( Map.Entry<Object, Object> e : map.entrySet() ) {
				String key = e.getKey() instanceof String ? (String) e.getKey() : e.toString();
				newConfig.put(key, e.getValue());
			}
			add(newConfig);
			return this;
		}

		public ConfigurationBuilder load( String[] args ) {
			Configuration newConfig = new Configuration();

			String key = null;
			for( int i = 0; i < args.length; i++ ) {
				if( args[i].startsWith("--") ) {
					if( key == null ) {
						key = args[i].substring(2).toUpperCase();
					} else {
						newConfig.put(key, "true");
						key = args[i].substring(2).toUpperCase();
					}
				} else if( key != null ) {
					newConfig.put(key, args[i]);
					key = null;
				}
			}
			if( key != null ) {
				newConfig.put(key, "true");
			}

			if( !newConfig.isEmpty() ) {
				add(newConfig);
			}

			return this;
		}

		public ConfigurationBuilder load( CommandLine cli ) {
			if( cli != null ) {
				Configuration newConfig = new Configuration();

				for( Option opt : cli.getOptions() ) {
					String value = cli.getOptionValue(opt);
					if( value == null ) {
						value = "true";
					}

					if( opt.getLongOpt() != null ) {
						newConfig.put(opt.getLongOpt().toUpperCase(), value);
					} else if( opt.getOpt() != null ) {
						newConfig.put(opt.getOpt().toUpperCase(), value);
					}
				}

				if( !newConfig.isEmpty() ) {
					add(newConfig);
				}
			}
			return this;
		}

		public Configuration get() {
			if( this.config == null ) {
				return new Configuration();
			} else {
				return this.config;
			}
		}

	}

}
