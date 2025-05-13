# Changelog

Alle bemerkenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/).

## [1.2.0] - 2023

### Hinzugefügt
- Statistik-System mit Spielerstatistiken (Onlinezeit, Logins, Votes)
- `/stats` Befehl zum Anzeigen von Spielerstatistiken
- `/top` Befehl zum Anzeigen von Ranglisten nach verschiedenen Kategorien
- StatsManager für das Tracking und Abrufen von Spielerstatistiken
  - Tracking von Spieleraktivitäten wie Login-Anzahl, Votes und gesendete Nachrichten
  - Speicherung der ersten und letzten Login-Zeit für jeden Spieler
  - Abruf von Toplisten nach verschiedenen Kategorien (Onlinezeit, Logins, Votes)
- Caching-System für verbesserte Performance bei Statistik-Abfragen
- Vollständiges Bestrafungssystem mit Befehlen:
  - `/ban`, `/unban` - für permanente und temporäre Spieler-Sperren
  - `/mute`, `/unmute` - zum Stummschalten von Spielern
  - `/warn` - für Spieler-Verwarnungen
  - `/report`, `/reports` - zur Meldung und Verwaltung von Regelverstößen

### Verbessert
- Refactoring des Codes zur Nutzung von Manager-Klassen für bessere Organisation
  - CommandManager für die zentrale Verwaltung von Befehlen
  - ListenerManager für die zentrale Verwaltung von Event-Listenern
  - UpdateManager für die Prüfung und Verwaltung von Plugin-Updates
  - StartupManager für die Organisation des Plugin-Starts
  - PunishmentManager für die Verwaltung von Spieler-Bestrafungen
  - StatsManager für das Tracking und Abrufen von Spielerstatistiken
- Optimierte Datenbankverbindung mit automatischer Tabellenerstellung
- Verbesserte Fehlerbehandlung bei Datenbankoperationen

## [1.1.0] - 2023

### Hinzugefügt
- Quality-of-Life Funktionen:
  - `/afk` Befehl für das Setzen des Abwesenheitsstatus
  - `/seen` Befehl zum Prüfen der letzten Aktivität eines Spielers
  - `/nick` Befehl zum Ändern des Spielernamens
  - `/whois` Befehl zur Anzeige von Spielerinformationen

### Verbessert
- Verbesserung der Datenbankverbindung mit detaillierter Fehlerdiagnose
- Erweitertes Logging für bessere Fehlerbehebung

## [1.0.0] - 2023

### Hinzugefügt
- System- und Admin-Befehle:
  - `/reloadconfig` zum Neuladen der Plugin-Konfiguration
  - `/maintenance` für den Wartungsmodus
  - `/restart` zum Neustarten des Proxys
- Bann- und Mute-System mit Datenbankunterstützung
- Benutzerdefinierte Nachrichten und Broadcast-Funktionen
- Server-Management-Befehle
- Globaler und Team-Chat
- Private Nachrichten-Funktionen mit `/msg` und `/reply`
- Datenbank-Integration für dauerhafte Speicherung
- Konfigurierbare Präfixe und Farben 