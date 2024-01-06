package schule.ngb.carrot.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.logging.*;

import static java.util.logging.Level.*;

/**
 * Einfache Logging-API, die auf {@link java.util.logging} aufsetzt.
 * <p>
 * Klassen, die Informations- oder Fehlernachrichten loggen wollen, erstellen
 * ein internes {@code LOG} Objekt dieser Klasse:
 * <pre>
 *     private static final schule.ngb.net.mail.util.Log LOG = new schule.ngb.net.mail.util.Log(Classname.class);
 * </pre>
 * <p>
 * Jedes {@code schule.ngb.net.mail.util.Log} nutzt intern einen {@link Logger}, der über
 * {@link Logger#getLogger(String)} abgerufen wird. Die {@code schule.ngb.net.mail.util.Log}-Objekte
 * selbst werden nicht weiter zwischengespeichert, aber in der Regel wird pro
 * Klasse nur genau ein {@code schule.ngb.net.mail.util.Log}-Objekt erstellt. Mehrere {@code schule.ngb.net.mail.util.Log}s nutzen
 * dann aber denselben {@code Logger}.
 * <p>
 * Die API orientiert sich lose an <a
 * href="https://logging.apache.org/log4j/2.x/">Log4j</a> und vereinfacht die
 * Nutzung der Java logging API für die häufigsten Anwendungsfälle.
 */
@SuppressWarnings("unused")
public final class Log {

    private static final String ROOT_LOGGER = "schule";

    private static final String DEFAULT_LOG_FORMAT = "[%1$tT] [%4$11s] %5$s %6$s%n";

    private static final String DEFAULT_DEBUG_FORMAT = "[%1$tT] [%4$11s] (%2$s) %5$s %6$s%n";

    private static boolean LOGGING_INIT = false;

    /**
     * Aktiviert das Logging in der Zeichenmaschine global.
     * <p>
     * Die Methode sollte einmalig möglichst früh im Programm aufgerufen werden,
     * um für alle bisher und danach erstellten {@link Logger} das minimale
     * Logging-Level auf {@link Level#FINE} zu setzen. Dies entspricht allen
     * Nachrichten die mit den Methoden (außer {@code trace}) eines {@link Log}
     * erzeugt werden.
     *
     * @see #enableGlobalLevel(Level)
     */
    public static void enableGlobalDebugging() {
        enableGlobalLevel(ALL);
    }

    /**
     * Setzt das Logging-Level aller bisher und danach erzeugten {@link Logger}
     * auf den angegebenen {@code Level}.
     * <p>
     * Das Level für bestehende {@code Logger} wird nur abgesenkt, so dass
     * Nachrrichten bis {@code level} auf jeden Fall ausgegeben werden. Besitzt
     * der {@code Logger} schon ein niedrigeres Level, wird dieses nicht
     * verändert. Gleiches gilt für alle {@link ConsoleHandler}, die den
     * bestehenden {@code Logger}n hinzugefügt wurden.
     * <p>
     * <strong>Hinweis:</strong> Das Setzen des Logging-Level während der
     * Programmausführung gilt als <em>bad practice</em>, also schlechter
     * Programmierstil. Im Kontext der Zeichenmaschine macht dies Sinn, um
     * Programmieranfängern eine einfache Möglichkeit zu geben, in ihren
     * Programmen auf Fehlersuche zu gehen. Für andere Einsatzszenarien sollte
     * auf die übliche Konfiguration des {@link java.util.logging} Pakets über
     * eine Konfigurationsdatei zurückgegriffen werden.
     *
     * @param level Das Level, auf das alle {@code Logger} und {@code Handler}
     * 	mindestens	herabgesenkt werden sollen.
     */
    public static void enableGlobalLevel( Level level ) {
        // int lvl = Validator.requireNotNull(level, "level").intValue();
        int lvl = level.intValue();
        ensureRootLoggerInitialized();

        // Decrease level of root level ConsoleHandlers for output
        Logger rootLogger = Logger.getLogger("");
        for( Handler handler : rootLogger.getHandlers() ) {
            if( handler instanceof ConsoleHandler ) {
                Level handlerLevel = handler.getLevel();
                if( handlerLevel == null || handler.getLevel().intValue() > lvl ) {
                    handler.setLevel(level);
                    handler.setFormatter(new LogFormatter());
                }
            }
        }

        // Decrease level of all existing ZM Loggers
        Iterator<String> loggerNames = LogManager.getLogManager().getLoggerNames().asIterator();
        while( loggerNames.hasNext() ) {
            String loggerName = loggerNames.next();

            if( loggerName.startsWith(ROOT_LOGGER) ) {
                Logger logger = Logger.getLogger(loggerName);
                logger.setLevel(level);

                for( Handler handler : logger.getHandlers() ) {
                    if( handler instanceof ConsoleHandler ) {
                        Level handlerLevel = handler.getLevel();
                        if( handlerLevel == null || handler.getLevel().intValue() > lvl ) {
                            handler.setLevel(level);
                        }
                    }
                }
            }
        }

        Logger LOG = Logger.getLogger(ROOT_LOGGER);
        LOG.fine("Debug logging enabled.");
    }

    public static Log getLogger( Class<?> clazz ) {
        ensureRootLoggerInitialized();
        return new Log(clazz);
    }

    private static void ensureRootLoggerInitialized() {
        if( LOGGING_INIT ) {
            return;
        }

        if( System.getProperty("java.util.logging.SimpleFormatter.format") == null ) {
            System.setProperty("java.util.logging.SimpleFormatter.format", DEFAULT_LOG_FORMAT);
        }
        Logger rootLogger = Logger.getLogger(ROOT_LOGGER);
        rootLogger.setLevel(Level.INFO);

        if( System.getProperty("java.util.logging.SimpleFormatter.format") == null
            && LogManager.getLogManager().getProperty("java.util.logging.SimpleFormatter.format") == null ) {
            rootLogger.addHandler(new StreamHandler(System.err, new LogFormatter()) {
                @Override
                public synchronized void publish( final LogRecord record ) {
                    super.publish(record);
                    flush();
                }
            });
        }

        LOGGING_INIT = true;
    }

    private final Logger LOGGER;

    private final Class<?> sourceClass;

    private Log( final Class<?> clazz ) {
        sourceClass = clazz;
        if( !clazz.getName().startsWith(ROOT_LOGGER) ) {
            LOGGER = Logger.getLogger(ROOT_LOGGER + "." + clazz.getSimpleName());
        } else {
            LOGGER = Logger.getLogger(clazz.getName());
        }
    }

	/*public schule.ngb.net.mail.util.Log( final String name ) {
		LOGGER = Logger.getLogger(name);
	}*/

    public void log( Level level, final CharSequence msg ) {
        //LOGGER.log(level, msg::toString);
        if( LOGGER.isLoggable(level) ) {
            //LOGGER.logp(level, sourceClass.getName(), inferCallerName(), msg::toString);
            doLog(level, null, msg::toString);
        }
    }

    public void log( Level level, final CharSequence msg, Object... params ) {
        if( LOGGER.isLoggable(level) ) {
            //LOGGER.logp(level, sourceClass.getName(), inferCallerName(), () -> String.format(msg.toString(), params));
            doLog(level, null, () -> String.format(msg.toString(), params));
        }
    }

    public void log( Level level, final Supplier<String> msgSupplier ) {
        if( LOGGER.isLoggable(level) ) {
            //LOGGER.logp(level, sourceClass.getName(), inferCallerName(), msgSupplier);
            doLog(level, null, msgSupplier);
        }
    }

    public void log( Level level, final Throwable throwable, final CharSequence msg, Object... params ) {
        if( LOGGER.isLoggable(level) ) {
            //LOGGER.logp(level, sourceClass.getName(), inferCallerName(), throwable, () -> String.format(msg.toString(), params));
            doLog(level, throwable, () -> String.format(msg.toString(), params));
        }
    }

    public void log( Level level, final Throwable throwable, final Supplier<String> msgSupplier ) {
        if( LOGGER.isLoggable(level) ) {
            //LOGGER.logp(level, sourceClass.getName(), inferCallerName(), throwable, msgSupplier);
            doLog(level, throwable, msgSupplier);
        }
    }

    private void doLog( Level level, final Throwable throwable, final Supplier<String> msgSupplier ) {
        String clazz = sourceClass.getName();
        String method = "unknown";

        StackTraceElement[] trace = new Throwable().getStackTrace();
        for( StackTraceElement stackTraceElement : trace ) {
            if( !stackTraceElement.getClassName().equals(Log.class.getName()) ) {
                clazz = stackTraceElement.getClassName();
                method = stackTraceElement.getMethodName();
                break;
            }
        }

        if( throwable != null ) {
            LOGGER.logp(level, clazz, method, throwable, msgSupplier);
        } else {
            LOGGER.logp(level, clazz, method, msgSupplier);
        }
    }

    public boolean isLoggable( Level level ) {
        return LOGGER.isLoggable(level);
    }

    public void info( final CharSequence msg ) {
        this.log(INFO, msg);
    }

    public void info( final CharSequence msg, Object... params ) {
        this.log(INFO, () -> String.format(msg.toString(), params));
    }

    public void info( final Supplier<String> msgSupplier ) {
        this.log(INFO, msgSupplier);
    }

    public void info( final Throwable ex, final CharSequence msg, Object... params ) {
        this.log(INFO, ex, () -> String.format(msg.toString(), params));
    }

    public void info( final Throwable throwable, final Supplier<String> msgSupplier ) {
        this.log(INFO, throwable, msgSupplier);
    }

    public void warn( final CharSequence msg ) {
        this.log(WARNING, msg);
    }

    public void warn( final CharSequence msg, Object... params ) {
        this.log(WARNING, () -> String.format(msg.toString(), params));
    }

    public void warn( final Supplier<String> msgSupplier ) {
        this.log(WARNING, msgSupplier);
    }

    public void warn( final Throwable ex, final CharSequence msg, Object... params ) {
        this.log(WARNING, ex, () -> String.format(msg.toString(), params));
    }

    public void warn( final Throwable throwable, final Supplier<String> msgSupplier ) {
        this.log(WARNING, throwable, msgSupplier);
    }

    public void error( final CharSequence msg ) {
        this.log(SEVERE, msg);
    }

    public void error( final CharSequence msg, Object... params ) {
        this.log(SEVERE, () -> String.format(msg.toString(), params));
    }

    public void error( final Supplier<String> msgSupplier ) {
        this.log(SEVERE, msgSupplier);
    }

    public void error( final Throwable ex, final CharSequence msg, Object... params ) {
        this.log(SEVERE, ex, () -> String.format(msg.toString(), params));
    }

    public void error( final Throwable throwable, final Supplier<String> msgSupplier ) {
        this.log(SEVERE, throwable, msgSupplier);
    }

    public void debug( final CharSequence msg ) {
        this.log(FINE, msg);
    }

    public void debug( final CharSequence msg, Object... params ) {
        this.log(FINE, () -> String.format(msg.toString(), params));
    }

    public void debug( final Supplier<String> msgSupplier ) {
        this.log(FINE, msgSupplier);
    }

    public void debug( final Throwable ex, final CharSequence msg, Object... params ) {
        this.log(FINE, ex, () -> String.format(msg.toString(), params));
    }

    public void debug( final Throwable throwable, final Supplier<String> msgSupplier ) {
        this.log(FINE, throwable, msgSupplier);
    }

    public void trace( final CharSequence msg ) {
        this.log(FINER, msg);
    }

    public void trace( final CharSequence msg, Object... params ) {
        this.log(FINER, () -> String.format(msg.toString(), params));
    }

    public void trace( final Supplier<String> msgSupplier ) {
        this.log(FINER, msgSupplier);
    }

    public void trace( final Throwable ex, final CharSequence msg, Object... params ) {
        this.log(FINER, ex, () -> String.format(msg.toString(), params));
    }

    public void trace( final Throwable throwable, final Supplier<String> msgSupplier ) {
        this.log(FINER, throwable, msgSupplier);
    }

    static final class LogFormatter extends Formatter {

        @Override
        public String format( LogRecord record ) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
            String source;
            if( record.getSourceClassName() != null ) {
                source = record.getSourceClassName();
                if( record.getSourceMethodName() != null ) {
                    source += " " + record.getSourceMethodName();
                }
            } else {
                source = record.getLoggerName();
            }
            String message = formatMessage(record);
            String throwable = "";
            if( record.getThrown() != null ) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }
            return String.format(DEFAULT_DEBUG_FORMAT,
                zdt,
                source,
                record.getLoggerName(),
                record.getLevel().getLocalizedName(),
                message,
                throwable);
        }

    }

}
