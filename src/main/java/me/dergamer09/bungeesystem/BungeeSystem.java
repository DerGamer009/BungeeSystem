package me.dergamer09.bungeesystem;

import me.dergamer09.bungeesystem.commands.BlockBungeeCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class BungeeSystem extends Plugin {

    private final String prefix = ChatColor.DARK_GRAY + "| " + ChatColor.RED + "ᴍɪɴᴇᴄᴏꜱɪᴀ " + ChatColor.GRAY + "» ";
    private final String webhookUrl = "https://discord.com/api/webhooks/1278349809530437683/1oYWODkc92wE_Q3B_xUQDD3NpS_2_shkiwI0shXKGAs0UjlQqQ_ntoecL5f6bgtOatgE";

    private final String currentVersion = "1.2-SNAPSHOT";  // Deine aktuelle Version
    private final String jenkinsApiUrl = "http://ci.dergamer09.me/job/BungeeSystem/lastSuccessfulBuild/api/json";  // API-Endpunkt für die neueste Version

    @Override
    public void onEnable() {
        // Registrierung der Befehle
        getProxy().getPluginManager().registerCommand(this, new LobbyCommand("l"));
        getProxy().getPluginManager().registerCommand(this, new LobbyCommand("lobby"));
        getProxy().getPluginManager().registerCommand(this, new LobbyCommand("hub"));
        getProxy().getPluginManager().registerCommand(this, new BlockBungeeCommand());
        getProxy().getPluginManager().registerCommand(this, new ReportCommand());

        getLogger().info(prefix + ChatColor.GRAY + "-------------------------------------");
        getLogger().info(prefix + ChatColor.GREEN + "Plugin wurde erfolgreich gestartet!");
        getLogger().info(prefix + ChatColor.DARK_AQUA + "Plugin by DerGamer09");
        getLogger().info(prefix + ChatColor.DARK_AQUA + "Version 1.1-SNAPSHOT");
        getLogger().info(prefix + ChatColor.GRAY + "-------------------------------------");

        checkForUpdates();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void checkForUpdates() {
        try {
            URL url = new URL(jenkinsApiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();

            String jsonResponse = content.toString();
            String latestVersion = extractVersionFromJson(jsonResponse);

            if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                getLogger().warning(ChatColor.YELLOW + "Ein neues Update ist verfügbar: " + latestVersion + " (aktuelle Version: " + currentVersion + ")");
            } else {
                getLogger().info(ChatColor.GREEN + "Dein Plugin ist auf dem neuesten Stand.");
            }

        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "Fehler beim Überprüfen auf Updates: " + e.getMessage());
        }
    }

    private String extractVersionFromJson(String jsonResponse) {
        try {
            // Parsing der JSON-Antwort, um die Version herauszufiltern
            // Annahme: Deine Jenkins-API gibt ein JSON mit einem Feld 'displayName' oder 'fullDisplayName' zurück, das die Version enthält
            int index = jsonResponse.indexOf("\"displayName\":\"");
            if (index != -1) {
                int start = index + 14; // 14 = Länge von "displayName":"
                int end = jsonResponse.indexOf("\"", start);
                return jsonResponse.substring(start, end);
            }
        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "Fehler beim Extrahieren der Version aus der JSON-Antwort: " + e.getMessage());
        }
        return null;
    }

    // LobbyCommand Klasse für /l, /lobby und /hub
    public class LobbyCommand extends Command {
        public LobbyCommand(String name) {
            super(name);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(prefix + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;
            ServerInfo lobby = getProxy().getServerInfo("Lobby");

            if (lobby != null) {
                player.connect(lobby);
                player.sendMessage(prefix + ChatColor.GREEN + "Du wirst zur" + ChatColor.YELLOW + " Lobby" + ChatColor.GREEN + " teleportiert...");
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Die" + ChatColor.YELLOW + " Lobby" + ChatColor.RED + " ist derzeit nicht verfügbar.");
            }
        }
    }

    // ReportCommand Klasse
    public class ReportCommand extends Command {
        public ReportCommand() {
            super("report");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(prefix + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;

            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.GRAY + "/report" + ChatColor.DARK_AQUA + " <Spieler>" + ChatColor.DARK_AQUA + " <Grund>");
                return;
            }

            String reportedPlayer = args[0];
            String reason = joinArray(args, 1, args.length);

            player.sendMessage(prefix + ChatColor.GREEN + "Danke für deinen "+ ChatColor.YELLOW + "Report! " + ChatColor.GREEN + "Wir werden den Fall prüfen.");

            // Nachricht an Discord Webhook senden
            sendReportToDiscord(player.getName(), reportedPlayer, reason);
        }

        private void sendReportToDiscord(String reporter, String reportedPlayer, String reason) {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = String.format(
                        "{\"content\": null, \"embeds\": [{\"title\": \"Neuer Report\",\"color\": 14177041,\"fields\": [" +
                                "{\"name\": \"Reporter\",\"value\": \"%s\",\"inline\": true}," +
                                "{\"name\": \"Gemeldeter Spieler\",\"value\": \"%s\",\"inline\": true}," +
                                "{\"name\": \"Grund\",\"value\": \"%s\",\"inline\": false}]}]}",
                        reporter, reportedPlayer, reason
                );

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes());
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Erfolg: Verbindung wird automatisch geschlossen
                } else {
                    // Fehlerbehandlung falls benötigt
                    System.err.println(prefix + ChatColor.RED + "Fehler beim Senden des Reports. HTTP Fehlercode: " + responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String joinArray(String[] array, int start, int end) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(array[i]);
        }
        return result.toString();
    }

}
