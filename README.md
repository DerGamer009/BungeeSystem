# BungeeSystem

## Beschreibung
Das **BungeeSystem** ist ein Plugin für BungeeCord und seit Version 1.2.1 auch für Velocity, das verschiedene administrative und spielerbezogene Funktionen bietet. Es ermöglicht eine zentrale Steuerung des Netzwerks mit nützlichen Befehlen und Automatisierungen.

## Funktionen
- **Netzwerkweite Verwaltung**: Steuere dein BungeeCord- oder Velocity-Netzwerk mit einfachen Befehlen.
- **Benutzerfreundliche GUI**: Erleichtert die Nutzung für Admins und Moderatoren.
- **Bann- und Mutesystem**: Verwalte Bestrafungen direkt über BungeeCord.
- **Automatische Nachrichten**: Ankündigungen und Auto-Broadcasts für Spieler.
- **Proxy-übergreifender Chat**: Ermöglicht globale und private Nachrichten zwischen Servern.
- **Spielerstatistiken**: Tracking von Spieleraktivitäten (Onlinezeit, Logins, Votes) mit Ranglisten.
- **Quality-of-Life Funktionen**: AFK-Status, Nicknamen, Spielersuche und mehr.
- **Verbesserte Datenbankverwaltung**: Effiziente Speicherung und Abruf von Daten mit Caching.
- **Report-System**: Spieler können Regelverstöße melden; optionaler Discord-Webhook zur Benachrichtigung.

## Installation
1. Lade das Plugin von [GitHub](https://github.com/DerGamer009/BungeeSystem) herunter.
2. Platziere die `.jar`-Datei im `plugins`-Ordner deines **BungeeCord**- oder **Velocity**-Proxys.
3. Starte den Proxy neu oder lade das Plugin mit `/bungee reload` (bzw. bei Velocity mit `/velocity reload`).
4. Passe die Konfigurationsdatei in `plugins/BungeeSystem/config.yml` nach deinen Wünschen an.

## Befehle
| Befehl                | Beschreibung                                |
|----------------------|----------------------------------------|
| `/reloadconfig`      | Lädt die Plugin-Konfiguration neu      |
| `/maintenance`       | Aktiviert/Deaktiviert den Wartungsmodus |
| `/restart`           | Startet den BungeeCord-Proxy neu      |
| `/ban <Spieler>`     | Bannt einen Spieler                    |
| `/unban <Spieler>`   | Entbannt einen Spieler                 |
| `/mute <Spieler>`    | Mutet einen Spieler                    |
| `/unmute <Spieler>`  | Entmutet einen Spieler                 |
| `/msg <Spieler> <Nachricht>` | Sendet eine private Nachricht |
| `/stats [Spieler]`   | Zeigt Statistiken eines Spielers an    |
| `/top <onlinetime/logins/votes>` | Zeigt Ranglisten der Spieler |
| `/afk`               | Setzt den AFK-Status                   |
| `/seen <Spieler>`    | Zeigt die letzte Aktivität eines Spielers |
| `/nick <Nickname>`   | Ändert deinen Anzeigenamen             |
| `/whois <Spieler>`   | Zeigt Informationen über einen Spieler |
| `/bsversion`         | Zeigt die aktuell installierte Version (Velocity) |

## Berechtigungen
| Permission             | Beschreibung |
|----------------------|-------------|
| `bungee.admin`      | Zugriff auf alle Admin-Befehle |
| `bungee.moderator`  | Zugriff auf Moderations-Befehle |
| `bungee.chat`       | Nutzung des globalen Chats |
| `bungee.stats.view` | Statistiken anderer Spieler einsehen |
| `bungee.stats.top`  | Zugriff auf Top-Ranglisten |

## Architektur
Das Plugin verwendet ein Manager-System für bessere Organisation:
- **CommandManager**: Zentrale Verwaltung aller Befehle
- **ListenerManager**: Verwaltet Event-Listener
- **DatabaseManager**: Datenbankverbindungen und -abfragen
- **StatsManager**: Verfolgt und verwaltet Spielerstatistiken
- **ChatManager**: Verwaltet Chat-Funktionalitäten
- **PunishmentManager**: Handhabt das Bestrafungssystem

## Konfiguration
Die Konfigurationsdatei befindet sich unter `plugins/BungeeSystem/config.yml` und erlaubt die Anpassung von Nachrichten, Berechtigungen und weiteren Funktionen.
Um Discord-Benachrichtigungen für neue Reports zu aktivieren, setze dort die Option `webhookUrl` auf deinen Webhook-Link.

## Lizenz
Dieses Plugin wird unter der **MIT-Lizenz** veröffentlicht. Mehr Details findest du in der `LICENSE`-Datei.

## Änderungsprotokoll
Eine vollständige Liste der Änderungen findest du in der [CHANGELOG.md](CHANGELOG.md) Datei.

## Kontakt
Falls du Fragen oder Verbesserungsvorschläge hast, kannst du ein Issue im [GitHub-Repository](https://github.com/DerGamer009/BungeeSystem) erstellen oder den Entwickler direkt kontaktieren.
