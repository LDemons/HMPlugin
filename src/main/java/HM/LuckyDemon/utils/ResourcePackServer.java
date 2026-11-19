package HM.LuckyDemon.utils;

import HM.LuckyDemon.HMPlugin;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Sirve el HMPack.zip que va dentro del jar por HTTP y se lo envia (obligatorio) a cada jugador al entrar.
 * El puerto (resource_pack.port) tiene que estar abierto en el servidor para los jugadores de fuera.
 */
public class ResourcePackServer implements Listener {

    private HttpServer http;
    private int port;
    private String sha1;
    private UUID packId;

    public boolean start() {
        HMPlugin plugin = HMPlugin.getInstance();
        if (!plugin.getConfig().getBoolean("resource_pack.enabled", true)) return false;
        port = plugin.getConfig().getInt("resource_pack.port", 8124);

        try (InputStream in = plugin.getResource("HMPack.zip")) {
            if (in == null) throw new IOException("HMPack.zip no esta dentro del jar");
            byte[] pack = in.readAllBytes();
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(pack));
            packId = UUID.nameUUIDFromBytes(pack);

            http = HttpServer.create(new InetSocketAddress(port), 0);
            http.createContext("/HMPack.zip", ex -> {
                ex.getResponseHeaders().add("Content-Type", "application/zip");
                ex.sendResponseHeaders(200, pack.length);
                try (OutputStream out = ex.getResponseBody()) {
                    out.write(pack);
                }
            });
            http.start();
            plugin.getLogger().info("Resource pack servido en el puerto " + port + " (sha1 " + sha1 + ")");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("No se pudo servir el resource pack: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (http != null) http.stop(0);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        // Se usa la misma direccion con la que el jugador se conecto al server, asi le sirve desde donde este
        InetSocketAddress joined = player.getVirtualHost();
        String host = joined != null ? joined.getHostString()
                : Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
        URI url = URI.create("http://" + host + ":" + port + "/HMPack.zip");

        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(packId, url, sha1))
                .required(true)
                .prompt(MessageUtils.format("<red>HMPlugin necesita este pack para sus animaciones.")));
    }
}
