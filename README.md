# BungeeSystem

## Beschreibung
Das **BungeeSystem** ist ein Plugin für BungeeCord, das verschiedene administrative und spielerbezogene Funktionen bietet. Es ermöglicht eine zentrale Steuerung des Netzwerks mit nützlichen Befehlen und Automatisierungen.

## Funktionen
- **Netzwerkweite Verwaltung**: Steuere dein BungeeCord-Netzwerk mit einfachen Befehlen.
- **Benutzerfreundliche GUI**: Erleichtert die Nutzung für Admins und Moderatoren.
- **Bann- und Mutesystem**: Verwalte Bestrafungen direkt über BungeeCord.
- **Automatische Nachrichten**: Ankündigungen und Auto-Broadcasts für Spieler.
- **Proxy-übergreifender Chat**: Ermöglicht globale und private Nachrichten zwischen Servern.

## Installation
1. Lade das Plugin von [GitHub](https://github.com/DerGamer009/BungeeSystem) herunter.
2. Platziere die `.jar`-Datei im `plugins`-Ordner deines **BungeeCord**-Proxys.
3. Starte den Proxy neu oder lade das Plugin mit `/bungee reload`.
4. Passe die Konfigurationsdatei in `plugins/BungeeSystem/config.yml` nach deinen Wünschen an.

## Befehle
| Befehl                | Beschreibung                                |
|----------------------|----------------------------------------|
| `/bungee reload`    | Lädt das Plugin neu                    |
| `/ban <Spieler>`    | Bannt einen Spieler                    |
| `/unban <Spieler>`  | Entbannt einen Spieler                 |
| `/mute <Spieler>`   | Mutet einen Spieler                    |
| `/unmute <Spieler>` | Entmutet einen Spieler                 |
| `/msg <Spieler> <Nachricht>` | Sendet eine private Nachricht |

## Berechtigungen
| Permission             | Beschreibung |
|----------------------|-------------|
| `bungee.admin`      | Zugriff auf alle Admin-Befehle |
| `bungee.moderator`  | Zugriff auf Moderations-Befehle |
| `bungee.chat`       | Nutzung des globalen Chats |

## Konfiguration
Die Konfigurationsdatei befindet sich unter `plugins/BungeeSystem/config.yml` und erlaubt die Anpassung von Nachrichten, Berechtigungen und weiteren Funktionen.

## Lizenz
Dieses Plugin wird unter der **MIT-Lizenz** veröffentlicht. Mehr Details findest du in der `LICENSE`-Datei.

## Kontakt
Falls du Fragen oder Verbesserungsvorschläge hast, kannst du ein Issue im [GitHub-Repository](https://github.com/DerGamer009/BungeeSystem) erstellen oder den Entwickler direkt kontaktieren.
