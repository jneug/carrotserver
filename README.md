<img src="assets/CarrotServer_Logo.png" width="128" style="margin:0 auto; display: block;"/>

# CarrotServ

**CarrotServer** ist ein kleiner, lokaler Server, der für den Einsatz im
Bildungsbereich entwickelt wurde. Die App erlaubt die Erkundung verschiedener (
hauptsächlich textbasierter)
Internetprotokolle in einer lokalen, sicheren Umgebung.

> [!CAUTION]
> **CarrotServ** ist **nicht** für den produktiven Einsatz
> vorgesehen!

Ziele der Entwicklung waren unter anderem:

- Plattformunabhängigkeit
- Einfache Nutzung und Konfiguration
- Transparente Datenspeicherung
- Einfache Erweiterbarkeit

Derzeit unterstützt **CarrotServer** folgende Protokolle:

- [echo](https://datatracker.ietf.org/doc/html/rfc862/)
- [POP3](https://datatracker.ietf.org/doc/html/rfc1939/)
- [SMTP](https://datatracker.ietf.org/doc/html/rfc5321/)
- Stein-Schere-Papier

Weitere Protokolle sollen in Zukunft ergänzt werden.

## Start der App

Die App steht als JAR-Archiv zur Verfügung und kann auf jedem System mit
geeignetem JAVA Runtime Environment (ab Java Version 11) ausgeführt werden.

## Konfiguration

Eine vollständige [Liste alle Konfigurationsoptionen](https://github.com/jneug/carrotserver/wiki/Konfiguration) finden sich im Wiki.

## Erweiterungen

**CarrotServer** lässt sich einfach um weitere Protokolle erweitern. Die
Erweiterungen können als java-Dateien im Daten-Ordner abgelegt werden und werden
automatisch kompiliert und in die App integriert.

Details zur [Implementierung eigener Protokoll](https://github.com/jneug/carrotserver/wiki/Erweiterungen-programmieren) finden sich im Wiki.
