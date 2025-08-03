# VelocitySystem API Integration

Diese Dokumentation erklärt, wie du das VelocitySystem mit dem Dashboard API verbindest.

## 🚀 Setup

### 1. Automatische Token-Generierung (Empfohlen)

Das Plugin generiert automatisch einen sicheren Token beim ersten Start:

1. **Plugin starten** - Ein Token wird automatisch generiert
2. **Token anzeigen** - Verwende `/generatetoken` im Spiel
3. **Token kopieren** - Kopiere den angezeigten Token

### 2. Manuelle Token-Generierung

Verwende den Command `/generatetoken`:

```bash
/generatetoken          # Generiert einen neuen Token
/generatetoken force    # Überschreibt existierenden Token
```

### 3. Token-Konfiguration (Optional)

Falls du manuell konfigurieren möchtest, erstelle eine `token.yml`:

```yaml
# Server Token Configuration for VelocitySystem API
server:
  token: "dein_server_token_hier"
  api_url: "http://45.86.155.38:25664"
  
# API Configuration
api:
  enabled: true
  timeout: 5000  # milliseconds
  retry_attempts: 3
  
# Endpoints
endpoints:
  health: "/health"
  bans: "/api/bans"
  mutes: "/api/mutes"
  reports: "/api/reports"
```

## 🔧 Konfiguration

### API-Einstellungen

- **enabled:** `true`/`false` - Aktiviert/Deaktiviert die API-Integration
- **timeout:** Zeit in Millisekunden für API-Requests
- **retry_attempts:** Anzahl der Wiederholungsversuche bei Fehlern

### Server-Einstellungen

- **token:** Dein Server-Token für die API-Authentifizierung
- **api_url:** Die URL deines Dashboard-APIs

## 📊 Automatische Synchronisation

Das Plugin sendet automatisch folgende Daten an das Dashboard:

### Bans
- Spieler-Name
- Grund
- Admin-Name
- Dauer
- Zeitstempel

### Mutes
- Spieler-Name
- Grund
- Admin-Name
- Dauer
- Zeitstempel

### Reports
- Reporter-Name
- Ziel-Spieler
- Grund
- Server-Name
- Zeitstempel

## 🔍 Monitoring

### Logs

Das Plugin loggt alle API-Aktivitäten:

```
[INFO] API Manager initialized with token: Velo***1234
[INFO] API request successful: /api/bans
[WARNING] API request failed (attempt 1/3): /api/mutes - HTTP 401
[INFO] API health check successful
```

### Health Check

Das Plugin führt automatisch Health Checks durch:

- Beim Plugin-Start
- Bei API-Fehlern
- Regelmäßige Überprüfungen

## 🛠️ Troubleshooting

### Häufige Probleme

1. **Token nicht gefunden**
   ```
   [WARNING] API Manager disabled - no token configured or API disabled
   ```
   **Lösung:** Überprüfe deine `token.yml` Konfiguration

2. **API nicht erreichbar**
   ```
   [WARNING] API health check failed: HTTP 404
   ```
   **Lösung:** Überprüfe die `api_url` in der `token.yml`

3. **Authentifizierungsfehler**
   ```
   [WARNING] API request failed: HTTP 401
   ```
   **Lösung:** Überprüfe deinen Server-Token

### Debug-Modus

Aktiviere detaillierte Logs in der `config.yml`:

```yaml
debug:
  api: true
```

## 📈 Dashboard Features

Mit der API-Integration kannst du im Dashboard:

- ✅ Alle Bans in Echtzeit sehen
- ✅ Alle Mutes verwalten
- ✅ Reports überwachen
- ✅ Server-Statistiken einsehen
- ✅ Admin-Aktivitäten verfolgen

## 🔐 Sicherheit

- **Token-Schutz:** Der Token wird in Logs maskiert angezeigt
- **Automatische Generierung:** Sichere, kryptographisch zufällige Tokens
- **HTTPS:** Alle API-Requests verwenden sichere Verbindungen
- **Rate Limiting:** Automatische Wiederholungsversuche mit Backoff
- **Timeout:** Konfigurierbare Timeouts verhindern hängende Requests

### Token-Sicherheit

- **Länge:** 32 Zeichen (kryptographisch sicher)
- **Zeichensatz:** A-Z, a-z, 0-9
- **Präfix:** "VelocitySystem_" für einfache Identifikation
- **Maskierung:** In Logs wird nur der Anfang und das Ende angezeigt

## 📝 Beispiel-Konfiguration

```yaml
# token.yml
server:
  token: "VelocitySystem1234"
  api_url: "http://45.86.155.38:25664"
  
api:
  enabled: true
  timeout: 5000
  retry_attempts: 3
```

## 🆘 Support

Bei Problemen:

1. Überprüfe die Plugin-Logs
2. Teste die API-URL im Browser
3. Überprüfe deine Token-Konfiguration
4. Kontaktiere den Support

---

**Hinweis:** Die API-Integration ist optional. Das Plugin funktioniert auch ohne Dashboard-Verbindung.

## 🔄 Unterschiede zu BungeeCord

### Velocity-spezifische Features:

- **Moderne API:** Verwendet Velocity's moderne Command-API
- **Adventure Text:** Nutzt Adventure Text für bessere Formatierung
- **Asynchrone Verarbeitung:** Optimiert für Velocity's Event-System
- **Bessere Performance:** Nutzt Velocity's optimierte Architektur

### Kompatibilität:

- ✅ Gleiche Token-Generierung wie BungeeCord
- ✅ Gleiche API-Endpoints
- ✅ Gleiche Konfigurationsdateien
- ✅ Gleiche Dashboard-Integration

### Commands:

- `/generatetoken` - Generiert neuen Token
- `/generatetoken force` - Überschreibt existierenden Token
- Alle anderen Commands funktionieren identisch zu BungeeCord 