package schule.ngb.carrot.util;

import java.util.concurrent.TimeUnit;

/**
 * Hilfsklasse zur Zeitmessung im Nanosekundenbereich.
 * <p>
 * Mit einem {@code Timer} kann zum Beispiel die Laufzeit eines Algorithmus
 * gemessen werden. Wie eine echte Stoppuhr läuft der {@code Timer} weiter, wenn
 * nach einem {@link #stop()} wieder {@link #start()} aufgerufen wird. Soll die
 * Zeitmessung wieder bei null beginnen, muss vorher {@link #reset()} genutzt
 * werden.
 * <p>
 * Die gemessene Zeit kann in {@link #getMillis() Millisekunden} oder
 * {@link #getSeconds() Sekunden} abgerufen werden. Wird eine noch größere
 * Genauigkeit benötigt, können mit {@link #getTime(TimeUnit)} beliebige
 * Zeiteinheiten (zum Beispiel {@link TimeUnit#NANOSECONDS Nanosekunden})
 * abgerufen werden.
 * <p>
 * Die Zeit kann auch bei laufender Uhr abgefragt werden. In dem Fall wird die
 * bis zu diesem Zeitpunkt gemessene Zeit zurückgegeben.
 */
@SuppressWarnings( "unused" )
public final class Timer {

	/**
	 * Die Basiseinheit für die Zeitmessung.
	 */
	private final TimeUnit baseUnit;

	/**
	 * Ob die Zeitmessung gerade läuft.
	 */
	private boolean running = false;

	/**
	 * Startzeit der Zeitmessung. -1, wenn noch keine Messung gestartet wurde.
	 */
	private long start = -1;

	/**
	 * Zeit, die bisher bei allen Zeitmessungen, die gestoppt wurden, insgesamt
	 * vergangen ist.
	 */
	private long elapsed = 0;

	/**
	 * Erstellt einen neuen {@code Timer} mit Millisekunden als Basiseinheit.
	 */
	public Timer() {
		this(TimeUnit.MILLISECONDS);
	}

	/**
	 * Erstellt einen {@code Timer}, der die angegebene Einheit als Basiseinheit
	 * für {@link #getTime()} benutzt.
	 * <p>
	 * Um eine Zeitmessung in Nanosekunden durchzuführen, kann der {@code Timer}
	 * beispielsweise so instanziiert werden:
	 * <pre><code>
	 * Timer clock = new Timer(TimeUnit.NANOSECONDS);
	 * </code></pre>
	 *
	 * @param baseUnit Die Basiseinheit für die Zeitmessung.
	 */
	public Timer( TimeUnit baseUnit ) {
		this.baseUnit = baseUnit;
	}

	/**
	 * Ob die Zeitmessung gerade läuft.
	 *
	 * @return {@code true}, wenn die Zeitmessung mit {@link #start()} gestartet
	 * 	wurde.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Startet die Zeitmessung.
	 * <p>
	 * Wenn zuvor schon eine Zeitmessung gestartet wurde, wird die neue Messung
	 * zur Summe aller Messungen hinzuaddiert. Soll die Messung bei null
	 * starten, muss vorher {@link #reset()} verwendet werden:
	 *
	 * <pre><code>
	 * // Timer auf null stellen und sofort starten
	 * timer.reset().start();
	 * </code></pre>
	 *
	 * @return Dieser {@code Timer} selbst (method chaining).
	 */
	@SuppressWarnings( "UnusedReturnValue" )
	public Timer start() {
		start = System.nanoTime();
		running = true;

		return this;
	}

	/**
	 * Stoppt den {@code Timer}, wenn er derzeit läuft. Die gemessene Dauer wird
	 * zur Summe aller gemessenen Zeiten hinzuaddiert.
	 *
	 * @return Dieser {@code Timer} selbst (method chaining).
	 */
	@SuppressWarnings( "UnusedReturnValue" )
	public Timer stop() {
		if( running ) {
			running = false;
			elapsed += System.nanoTime() - start;
		}
		return this;
	}

	/**
	 * Setzt den {@code Timer} auf den Startzustand und löscht alle bisher
	 * gemessenen Zeiten. Falls die Zeitmessung gerade läuft, stoppt sie nicht,
	 * sondern startet vom Zeitpunkt des Aufrufs neu.
	 *
	 * @return Dieser {@code Timer} selbst (method chaining).
	 */
	@SuppressWarnings( "UnusedReturnValue" )
	public Timer reset() {
		elapsed = 0;
		start = -1;

		if( running ) {
			start = System.nanoTime();
		}

		return this;
	}

	/**
	 * Gibt die Zeit in der Basiseinheit zurück.
	 *
	 * @return Die bisher insgesamt gemessene Zeit.
	 */
	public long getTime() {
		return getTime(baseUnit);
	}

	/**
	 * Gibt die Zeit in der angegebenen Einheit zurück.
	 * <p>
	 * Größere Zeiteinheiten werden gerundet und verlieren daher an Genauigkeit.
	 * Eine Zeitmessung von 999 Millisekunden wird als 0 Sekunden
	 * zurückgegeben.
	 * <p>
	 * Um genauere Ergebnisse zu erhalten, kann mit {@link #getMillis()} und
	 * {@link #getSeconds()} die gemessene Zeit in Minuten beziehungsweise
	 * Sekunden als Kommazahl abgefragt werden.
	 *
	 * @param unit Zeiteinheit
	 * @return Die bisher insgesamt gemessene Zeit in der gewählten Zeiteinheit.
	 */
	public long getTime( TimeUnit unit ) {
		if( running ) {
			return unit.convert(System.nanoTime() - start + elapsed, TimeUnit.NANOSECONDS);
		} else {
			return unit.convert(elapsed, TimeUnit.NANOSECONDS);
		}
	}

	/**
	 * Gibt die gemessene Zeit in Millisekunden (gerundet) zurück.
	 *
	 * @return Die gemessene Zeit in ms.
	 */
	public int getMillis() {
		if( running ) {
			return (int) ((System.nanoTime() - start + elapsed) / 1000000);
		} else {
			return (int) (elapsed / 1000000);
		}
	}

	/**
	 * Gibt die gemessene Zeit in Sekunden zurück.
	 *
	 * @return Die gemessene Zeit in s.
	 */
	public double getSeconds() {
		if( running ) {
			return (System.nanoTime() - start + elapsed) / 1000000000.0;
		} else {
			return elapsed / 1000000000.0;
		}
	}

}
