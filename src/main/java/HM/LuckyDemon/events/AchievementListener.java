package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logros propios del server. Son logros nativos (menu L y aviso arriba a la derecha) con un criterio
 * "impossible" que solo da el plugin.
 */
public class AchievementListener implements Listener {

    private static final String CRITERION = "hm";
    private static final long EXPLOSION_WINDOW_MS = 60_000;
    private static final long BAT_BLIND_MS = 15_000;

    private final Map<UUID, Long> batBlindUntil = new HashMap<>();
    private final Map<UUID, Deque<Long>> explosions = new HashMap<>();
    private final NamespacedKey relicFreeSince = new NamespacedKey(HMPlugin.getInstance(), "ach_relic_free_since");

    public AchievementListener() {
        registerAdvancements();
        // Ahorro de Espacio: cada 10 s mira la Reliquia y los dias seguidos sin ella
        Bukkit.getScheduler().runTaskTimer(HMPlugin.getInstance(), this::checkRelicFree, 200L, 200L);
    }

    private static NamespacedKey key(String id) {
        return new NamespacedKey(HMPlugin.getInstance(), id);
    }

    private static void registerAdvancements() {
        Map<String, String> defs = new LinkedHashMap<>();
        defs.put("root", """
                {"criteria":{"tick":{"trigger":"minecraft:tick"}},
                 "display":{"icon":{"id":"minecraft:wither_skeleton_skull"},
                   "title":{"text":"HM","color":"dark_red"},
                   "description":{"text":"Logros del HM"},
                   "background":"minecraft:gui/advancements/backgrounds/nether",
                   "show_toast":false,"announce_to_chat":false}}""");
        defs.put("quien_necesita_ver", child("minecraft:ender_eye", "¿Quién necesita ver?",
                "Mata a un mob hostil mientras estás ciego por un murciélago", "task"));
        defs.put("ahorro_de_espacio", child("minecraft:barrier", "Ahorro de Espacio",
                "Sobrevive 2 días seguidos desde el día 40 sin la Reliquia del Fin", "challenge"));
        defs.put("no_es_mi_dia", child("minecraft:rotten_flesh", "No es mi Día",
                "Ten Hambre, Debilidad y Ceguera a la vez", "task"));
        defs.put("duro_como_la_piedra", child("minecraft:tnt", "Duro como la piedra",
                "Sobrevive a 3 explosiones en menos de 1 minuto", "goal"));

        Map<net.kyori.adventure.key.Key, String> toLoad = new LinkedHashMap<>();
        defs.forEach((id, json) -> {
            NamespacedKey k = key(id);
            // Bukkit puede guardar los logros cargados en el datapack del mundo: se borran para cargar la version actual
            if (Bukkit.getAdvancement(k) != null) Bukkit.getUnsafe().removeAdvancement(k);
            toLoad.put(k, json);
        });
        Bukkit.getUnsafe().loadAdvancements(toLoad, true);
    }

    private static String child(String icon, String title, String desc, String frame) {
        return """
                {"parent":"hmplugin:root",
                 "criteria":{"%s":{"trigger":"minecraft:impossible"}},
                 "display":{"icon":{"id":"%s"},"title":{"text":"%s"},"description":{"text":"%s"},
                   "frame":"%s","show_toast":true,"announce_to_chat":true}}"""
                .formatted(CRITERION, icon, title, desc, frame);
    }

    public static void grant(Player p, String id) {
        Advancement adv = Bukkit.getAdvancement(key(id));
        if (adv == null) return;
        AdvancementProgress progress = p.getAdvancementProgress(adv);
        if (!progress.isDone()) progress.awardCriteria(CRITERION);
    }

    // --- ¿Quién necesita ver? ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBatHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Bat && e.getEntity() instanceof Player p
                && GameManager.getInstance().getDay() >= 20) {
            batBlindUntil.put(p.getUniqueId(), System.currentTimeMillis() + BAT_BLIND_MS);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || !(e.getEntity() instanceof Enemy)) return;
        Long until = batBlindUntil.get(killer.getUniqueId());
        if (until != null && until > System.currentTimeMillis() && killer.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            grant(killer, "quien_necesita_ver");
        }
    }

    // --- No es mi Dia ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffect(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getNewEffect() == null) return;
        // Un tick despues el efecto nuevo ya esta puesto
        Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> {
            if (p.hasPotionEffect(PotionEffectType.HUNGER) && p.hasPotionEffect(PotionEffectType.WEAKNESS)
                    && p.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                grant(p, "no_es_mi_dia");
            }
        });
    }

    // --- Duro como la piedra ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (p.getHealth() - e.getFinalDamage() <= 0) return; // esta no la sobrevive

        long now = System.currentTimeMillis();
        Deque<Long> times = explosions.computeIfAbsent(p.getUniqueId(), u -> new ArrayDeque<>());
        times.addLast(now);
        while (now - times.peekFirst() > EXPLOSION_WINDOW_MS) times.removeFirst();
        if (times.size() >= 3) grant(p, "duro_como_la_piedra");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        explosions.remove(p.getUniqueId());
        batBlindUntil.remove(p.getUniqueId());
        int day = GameManager.getInstance().getDay();
        if (day >= 40) p.getPersistentDataContainer().set(relicFreeSince, PersistentDataType.INTEGER, day);
    }

    // --- Ahorro de Espacio ---

    private void checkRelicFree() {
        int day = GameManager.getInstance().getDay();
        if (day < 40) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            Integer since = pdc.get(relicFreeSince, PersistentDataType.INTEGER);
            if (since == null || Day40InventoryListener.hasEndRelic(p)) {
                pdc.set(relicFreeSince, PersistentDataType.INTEGER, day);
            } else if (day - since >= 2) {
                grant(p, "ahorro_de_espacio");
            }
        }
    }
}
