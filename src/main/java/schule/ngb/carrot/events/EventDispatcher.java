package schule.ngb.carrot.events;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

/**
 * Implementierung einer EventListener-API basierend auf dem Artikel
 * <a href="https://org.coloradomesa.edu/~wmacevoy/listen/paper.html">Skilled
 * Listening in Java</a> von Dr. Warren D. MacEvoy.
 * <p>
 * Der {@code EventDispatcher} verwaltet eine Liste von angemeldeten Listenern
 * in einem {@link CopyOnWriteArraySet}, dass besonders für Situationen geeignet
 * ist, in denen in der Regel wenige Objekte verwaltet werden müssen, auf die
 * schnell zugegriffen werden soll und die sich selten ändern. Das trifft in den
 * meisten Fällen auf die Listener eines Objekts zu.
 * <p>
 * Um einen {@code EventDispatcher} zu nutzen, muss eine Schnittstelle als
 * Unterinterface von {@link Listener} erstellt werden, die Methoden für die
 * einzelnen Events definiert. In der Regel wird eine Methode pro Event
 * angelegt, aber dies ist nicht unbedingt notwendig.
 * <p>
 * Ein Objekt, dass Events für diese Art von Listener erzeugt, erstellt einen
 * {@code EventDispatcher} und
 * {@link #registerEventType(String, BiConsumer) registriert} die notwendigen
 * Events.
 *
 * <pre><code>
 * EventDispatcher&lt;MyEvent, MyEventListener&gt; dispatcher = new EventDispatcher&lt;&gt;();
 * dispatcher.registerEventType("start", (evt, listener) -> listener.started(evt));
 * dispatcher.registerEventType("stop", (evt, listener) -> listener.stopped(evt));
 * </code></pre>
 * <p>
 * Hier werden zwei Events registriert "start" und "stop". Die Bezeichnung der
 * Events wird nut intern verwendet. Jedes Event registriert eine Funktion, die
 * den Aufruf der Listener-Schnittstelle umsetzt. In der Regel ist dies ein
 * Lambda-Ausdruck von zwei Parametern: dem Event-Objekt vom Typ {@code E} und
 * der Listener, der aufgerufen werden soll, vom Typ {@code L}.
 * <p>
 * Nun können {@code MyEventListener} angemeldet und Events ausgelöst werden.
 *
 * <pre><code>
 * public void addEventListener( MyEventListener listener ) {
 *     dispatcher.addListener(listener);
 * }
 *
 * public void removeEventListener( MyEventListener listener ) {
 *     dispatcher.removeListener(listener);
 * }
 *
 * public void methodThatCreatesEvents() {
 *     dispatcher.dispatchEvent("start", new MyEvent());
 *     // Do something
 *     dispatcher.dispatchEvent("stop", new MyEvent());
 * }
 * </code></pre>
 * <p>
 *
 * @param <E> Typ der Event-Objekte.
 * @param <L> Typ der verwendeten Listener-Schnittstelle.
 */
public class EventDispatcher<E, L extends Listener<E>> {

	private final CopyOnWriteArraySet<L> listeners;

	private final ConcurrentMap<String, BiConsumer<E, L>> eventRegistry;

	public EventDispatcher() {
		listeners = new CopyOnWriteArraySet<>();
		eventRegistry = new ConcurrentHashMap<>();
	}

	public void registerEventType( String eventKey, BiConsumer<E, L> dispatcher ) {
		Objects.requireNonNull(eventKey, "eventKey must not be null");
		Objects.requireNonNull(dispatcher, "dispatcher must not be null");

		if( !eventRegistered(eventKey) ) {
			eventRegistry.put(eventKey, dispatcher);
		}
	}

	public void addListener( L listener ) {
		listeners.add(listener);
	}

	public void removeListener( L listener ) {
		listeners.remove(listener);
	}

	@SuppressWarnings( "unused" )
	public boolean hasListeners() {
		return !listeners.isEmpty();
	}

	public boolean eventRegistered( String eventKey ) {
		return eventRegistry.containsKey(eventKey);
	}

	public void dispatchEvent( String eventKey, final E event ) {
		Objects.requireNonNull(eventKey, "eventKey");
		Objects.requireNonNull(event, "event");

		if( eventRegistered(eventKey) ) {
			final BiConsumer<E, L> dispatcher = eventRegistry.get(eventKey);
			listeners.forEach(( listener ) -> dispatcher.accept(event, listener));
		}
	}

}
