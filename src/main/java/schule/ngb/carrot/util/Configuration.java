package schule.ngb.carrot.util;

import org.apache.commons.cli.*;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.spi.BeanTool;
import schule.ngb.carrot.CarrotServer;
import schule.ngb.carrot.protocol.POP3Handler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


public class Configuration {

	public static final String SECTION_MAIN = "carrot";

	public static final String SECTION_USERS = "users";


	public static final char PATH_SEPARATOR = '.';

	/**
	 * Trennzeichen für String-Felder, die serialisierte Arrays enthalten.
	 */
	public static final String ARRAY_DELIMITER = ",";


	public static ConfigBuilder from() {
		return new ConfigBuilder();
	}

	public static ConfigBuilder from( Ini base ) {
		return new ConfigBuilder(base);
	}

	public static Ini merge( Ini origin, Ini other ) {
		if( other != null ) {
			for( String sectionName : other.keySet() ) {
				Profile.Section sec = origin.get(sectionName);
				if( sec == null ) {
					sec = origin.add(sectionName);
				}

				sec.putAll(other.get(sectionName));
			}
		}
		return origin;
	}

	public static <T> String toString( T[] values ) {
		return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(ARRAY_DELIMITER));
	}

	@SuppressWarnings( "unchecked" )
	public static String[] toArray( String value ) {
		if( value != null ) {
			return value.split(ARRAY_DELIMITER);
		} else {
			return new String[]{};
		}
	}

	public static Options getCliOptions() {
		Options options = new Options();
		options.addOption("c", "config", true, "define a path or name of a config file to use");
		options.addOption("h", "host", true, "set the hostname");
		options.addOption("d", "data", true, "set the data storage folder");
		options.addOption(Option.builder().longOpt("headless").desc("start without gui").build());
		options.addOption(Option.builder().longOpt("debug").desc("show debugging information").build());
		options.addOption(Option.builder("D").hasArgs().valueSeparator('=')
			.desc("set arbitrary configuration properties for protocols and extensions").build());
		return options;
	}

	public static CommandLine parseCli( String[] args ) {
		Options options = getCliOptions();
		try {
			CommandLineParser parser = new DefaultParser();
			return parser.parse(options, args);
		} catch( ParseException ignored ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar " + CarrotServer.APP_NAME + ".jar", options);
			System.exit(1);
		}
		return null;
	}


	// Internal Builder class
	public static final class ConfigBuilder {

		private Ini ini = null;

		private ConfigBuilder() {
		}

		private ConfigBuilder( Ini base ) {
			this.ini = base;
		}

		private Ini init() {
			if( this.ini == null ) {
				this.ini = newIni();
			}
			return this.ini;
		}

		private Ini newIni() {
			Ini ini = new Ini();

//			Config cfg = Config.getGlobal().clone();
//			cfg.setMultiOption(false);
//			cfg.setMultiSection(false);
//			cfg.setPathSeparator(PATH_SEPARATOR);
//			ini.setConfig(cfg);

			return ini;
		}

		private Ini newIni( InputStream in ) {
			Ini ini = newIni();

			try {
				ini.load(in);
			} catch( IOException ignored ) {
			}

			return ini;
		}

		public ConfigBuilder load( Path source ) {
			try( InputStream in = Files.newInputStream(source) ) {
				load(in);
			} catch( IOException ignored ) {
			}
			return this;
		}

		public ConfigBuilder load( InputStream source ) {
			if( source != null ) {
				try {
					if( this.ini == null ) {
						init().load(source);
					} else {
						this.ini = merge(this.ini, newIni(source));
					}
				} catch( IOException ignored ) {
				}
			}
			return this;
		}

		public ConfigBuilder load( Map<String, Map<String, String>> source ) {
			return this;
		}

		public ConfigBuilder load( String sectionName, Map<String, String> values ) {
			return this;
		}

		public ConfigBuilder load( CommandLine cli ) {
			if( cli != null ) {
				init();
				for( Option opt : cli.getOptions() ) {
					if( opt.hasArgs() ) {
						Properties props = cli.getOptionProperties(opt);
						for( String prop : props.stringPropertyNames() ) {
							String section = SECTION_MAIN, name = prop;
							int idx = prop.indexOf(PATH_SEPARATOR);
							if( idx >= 0 ) {
								section = prop.substring(0, idx);
								name = prop.substring(idx + 1);
							}
							this.ini.put(section, name, props.get(prop));
						}
					} else {
						String key = opt.getLongOpt() == null ? opt.getOpt() : opt.getLongOpt();
						String value = cli.getOptionValue(opt);
						this.ini.put(SECTION_MAIN, key, value == null ? true : value);
					}
				}
			}
			return this;
		}


		public ConfigBuilder loadLeft( Path source ) {
			try( InputStream in = Files.newInputStream(source) ) {
				loadLeft(in);
			} catch( IOException ignored ) {
			}
			return this;
		}

		public ConfigBuilder loadLeft( InputStream source ) {
			if( source != null ) {
				try {
					if( this.ini == null ) {
						init().load(source);
					} else {
						this.ini = merge(newIni(source), this.ini);
					}
				} catch( IOException ignored ) {
				}
			}
			return this;
		}

		public Ini build() {
			return init();
		}

	}


//	private static final Log LOG = Log.getLogger(Configuration.class);
//
//	public static final class ConfigurationBuilder {
//
//		private Configuration config = null;
//
//		public ConfigurationBuilder() {
//		}
//
//		public ConfigurationBuilder( Configuration config ) {
//			this.config = config;
//		}
//
//		private void add( Configuration newConfig ) {
//			if( this.config != null && newConfig != null && this.config.size() > 0 ) {
//				// Find last default in tree and append there
//				Configuration current = newConfig;
//				while( current.getDefaults() != null ) {
//					current = current.getDefaults();
//				}
//				current.setDefaults(config);
//			}
//
//			if( newConfig != null ) {
//				this.config = newConfig;
//			}
//		}
//
//		public ConfigurationBuilder load( Configuration other ) {
//			if( other != null ) {
//				Configuration newConfig = new Configuration();
//				newConfig.putAll(other);
//				add(newConfig);
//			}
//			return this;
//		}
//
//		public ConfigurationBuilder load( InputStream in ) {
//			if( in != null ) {
//				try {
//					Configuration newConfig = new Configuration();
//					newConfig.load(in);
//					add(newConfig);
//				} catch( IOException ignored ) {
//					LOG.debug("Failed to load configuration from input stream");
//				}
//			}
//			return this;
//		}
//
//		public ConfigurationBuilder load( Path p ) {
//			if( p != null ) {
//				try( InputStream in = Files.newInputStream(p) ) {
//					this.load(in);
//				} catch( IOException ignored ) {
//					LOG.debug("Failed to load configuration from path %s", p);
//				}
//			}
//			return this;
//		}
//
//		public ConfigurationBuilder load( File f ) {
//			if( f != null ) {
//				try( InputStream in = new FileInputStream(f) ) {
//					this.load(in);
//				} catch( IOException ignored ) {
//					LOG.debug("Failed to load configuration from file %s", f);
//				}
//			}
//			return this;
//		}
//
//		public ConfigurationBuilder load( URI u ) {
//			if( u != null ) {
//				this.load(new File(u));
//			}
//			return this;
//		}
//
//		public ConfigurationBuilder load( URL u ) {
//			if( u != null ) {
//				try {
//					this.load(new File(u.toURI()));
//				} catch( URISyntaxException ignored ) {
//				}
//			}
//			return this;
//		}
//
//		public ConfigurationBuilder load( Map<Object, Object> map ) {
//			Configuration newConfig = new Configuration();
//			for( Map.Entry<Object, Object> e : map.entrySet() ) {
//				String key = e.getKey() instanceof String ? (String) e.getKey() : e.toString();
//				newConfig.put(key, e.getValue());
//			}
//			add(newConfig);
//			return this;
//		}
//
//		public ConfigurationBuilder load( String[] args ) {
//			Configuration newConfig = new Configuration();
//
//			String key = null;
//			for( int i = 0; i < args.length; i++ ) {
//				if( args[i].startsWith("--") ) {
//					if( key == null ) {
//						key = args[i].substring(2).toUpperCase();
//					} else {
//						newConfig.put(key, "true");
//						key = args[i].substring(2).toUpperCase();
//					}
//				} else if( key != null ) {
//					newConfig.put(key, args[i]);
//					key = null;
//				}
//			}
//			if( key != null ) {
//				newConfig.put(key, "true");
//			}
//
//			if( !newConfig.isEmpty() ) {
//				add(newConfig);
//			}
//
//			return this;
//		}
//
//		public ConfigurationBuilder load( CommandLine cli ) {
//			if( cli != null ) {
//				Configuration newConfig = new Configuration();
//
//				for( Option opt : cli.getOptions() ) {
//					String value = cli.getOptionValue(opt);
//					if( value == null ) {
//						value = "true";
//					}
//
//					if( opt.getLongOpt() != null ) {
//						newConfig.put(opt.getLongOpt().toUpperCase(), value);
//					} else if( opt.getOpt() != null ) {
//						newConfig.put(opt.getOpt().toUpperCase(), value);
//					}
//				}
//
//				if( !newConfig.isEmpty() ) {
//					add(newConfig);
//				}
//			}
//			return this;
//		}
//
//		public Configuration get() {
//			if( this.config == null ) {
//				return new Configuration();
//			} else {
//				return this.config;
//			}
//		}
//
//	}

}
