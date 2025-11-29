package HM.LuckyDemon.utils;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import HM.LuckyDemon.HMPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookUtils {

    private static final String WEBHOOK_URL = "https://n8n.warevision.net/webhook/a3062da4-c838-48f8-b355-2a82065a798f";

    public static void sendDeathNotification(Player player, String deathCause, int remainingLives, int maxLives, String deathMessage) {
        Bukkit.getScheduler().runTaskAsynchronously(HMPlugin.getInstance(), () -> {
            try {
                URL url = new URL(WEBHOOK_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("playerName", player.getName());
                json.addProperty("playerUUID", player.getUniqueId().toString());
                json.addProperty("playerHeadUrl", "https://minotar.net/avatar/" + player.getUniqueId().toString() + "/100.png");
                json.addProperty("deathCause", deathCause);
                json.addProperty("remainingLives", remainingLives);
                json.addProperty("maxLives", maxLives);
                json.addProperty("deathMessage", deathMessage);
                json.addProperty("timestamp", System.currentTimeMillis());

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    Bukkit.getLogger().info("Webhook enviado correctamente para " + player.getName());
                } else {
                    Bukkit.getLogger().warning("Error al enviar webhook: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error al enviar webhook: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
