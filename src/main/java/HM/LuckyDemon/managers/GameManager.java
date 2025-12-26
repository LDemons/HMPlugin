package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class GameManager {

    private static GameManager instance;
    private int currentDay;
    private LocalDate startDate;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private GameManager() {
        loadFromConfig();
        startDayChecker();
    }

    /**
     * Cargar configuración desde config.yml
     */
    private void loadFromConfig() {
        String startDateStr = HMPlugin.getInstance().getConfig().getString("game.start_date", "");

        if (startDateStr == null || startDateStr.isEmpty()) {
            // Primera vez - establecer fecha de inicio como hoy
            this.startDate = LocalDate.now();
            this.currentDay = 1;
            saveToConfig();
        } else {
            try {
                this.startDate = LocalDate.parse(startDateStr, DATE_FORMAT);
                // Calcular el día actual basado en la fecha de inicio
                this.currentDay = calculateCurrentDay();
            } catch (Exception e) {
                // Si hay error, resetear a hoy
                this.startDate = LocalDate.now();
                this.currentDay = 1;
                saveToConfig();
            }
        }
    }

    /**
     * Guardar configuración en config.yml
     */
    private void saveToConfig() {
        HMPlugin.getInstance().getConfig().set("game.day", currentDay);
        HMPlugin.getInstance().getConfig().set("game.start_date", startDate.format(DATE_FORMAT));
        HMPlugin.getInstance().saveConfig();
    }

    /**
     * Calcular el día actual basado en la fecha de inicio
     */
    private int calculateCurrentDay() {
        long daysSinceStart = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        return (int) daysSinceStart + 1; // +1 porque el día 1 es el día de inicio
    }

    /**
     * Iniciar el verificador de días (cada minuto)
     */
    private void startDayChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int calculatedDay = calculateCurrentDay();

                // Si el día calculado es diferente al actual, avanzar
                if (calculatedDay > currentDay) {
                    setDay(calculatedDay, true); // true = es cambio automático
                }
            }
        }.runTaskTimer(HMPlugin.getInstance(), 20L * 60, 20L * 60); // Cada minuto (1200 ticks)
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public int getDay() {
        return currentDay;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Cambiar el día (manual)
     */
    public void setDay(int day) {
        setDay(day, false);
    }

    /**
     * Cambiar el día
     * 
     * @param day       Nuevo día
     * @param automatic Si es true, es un cambio automático por fecha (sin anuncio)
     */
    public void setDay(int day, boolean automatic) {
        int previousDay = this.currentDay;
        this.currentDay = day;

        // Si es cambio manual, ajustar la fecha de inicio para que coincida
        if (!automatic) {
            // Calcular nueva fecha de inicio para que el día actual sea el especificado
            this.startDate = LocalDate.now().minusDays(day - 1);
        }

        saveToConfig();

        HMPlugin.getInstance().getDifficultyManager().setCurrentDay(day);

        // Solo anunciar si es cambio manual
        if (!automatic) {
            Bukkit.broadcast(MessageUtils.format("<yellow>¡El tiempo ha cambiado! Ahora es el día <red><bold>" + day));

            int diffLevel = HMPlugin.getInstance().getDifficultyManager().getDifficultyLevel();
            if (diffLevel > 0 && day % 10 == 0) {
                Bukkit.broadcast(MessageUtils.format("<red><bold>⚠ ¡NIVEL DE DIFICULTAD AUMENTADO! ⚠"));
                Bukkit.broadcast(MessageUtils.format("<gold>La supervivencia será más difícil..."));
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // DÍA 20: Activar KeepInventory (sin anuncio)
        if (day >= 20 && previousDay < 20) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.KEEP_INVENTORY, true);
            }
        }

        // Si baja de día 20, desactivar KeepInventory
        if (day < 20 && previousDay >= 20) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.KEEP_INVENTORY, false);
            }
        }

        // DÍA 40: Aplicar slots bloqueados (sin anuncio)
        if (day >= 40 && previousDay < 40) {
            HM.LuckyDemon.events.Day40InventoryListener.applyToAllPlayers();
        }

        // DÍA 40+: Reducir vida a todos los jugadores conectados (solo una vez)
        if (day >= 40) {
            org.bukkit.NamespacedKey healthReducedKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "day40_health_reduced");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getPersistentDataContainer().has(healthReducedKey,
                        org.bukkit.persistence.PersistentDataType.BYTE)) {
                    org.bukkit.attribute.AttributeInstance maxHealth = p
                            .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (maxHealth != null) {
                        double currentMax = maxHealth.getBaseValue();
                        // Reducir 8 puntos (4 corazones), mínimo 12 (6 corazones)
                        double newMax = Math.max(12.0, currentMax - 8.0);
                        maxHealth.setBaseValue(newMax);
                        if (p.getHealth() > newMax) {
                            p.setHealth(newMax);
                        }
                        // Marcar como reducido
                        p.getPersistentDataContainer().set(healthReducedKey,
                                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                        Bukkit.getLogger().info(
                                "[HMPlugin] Vida reducida para " + p.getName() + ": " + currentMax + " -> " + newMax);
                    }
                }
            }
        }

        // Si baja de día 40, remover slots bloqueados
        if (day < 40 && previousDay >= 40) {
            HM.LuckyDemon.events.Day40InventoryListener.removeFromAllPlayers();
        }

        // DÍA 40: Desactivar generación de estructuras (requiere reinicio)
        if (day >= 40 && previousDay < 40) {
            setServerProperty("generate-structures", "false");
            Bukkit.broadcast(MessageUtils.format("<red>⚠ La generación de estructuras ha sido desactivada."));
            Bukkit.broadcast(MessageUtils.format("<yellow>Este cambio aplicará al reiniciar el servidor."));
        }

        // Si baja de día 40, reactivar generación de estructuras
        if (day < 40 && previousDay >= 40) {
            setServerProperty("generate-structures", "true");
            Bukkit.broadcast(MessageUtils.format("<green>✓ La generación de estructuras ha sido reactivada."));
            Bukkit.broadcast(MessageUtils.format("<yellow>Este cambio aplicará al reiniciar el servidor."));
        }
    }

    public void advanceDay() {
        setDay(currentDay + 1, false);
    }

    public void reset() {
        this.currentDay = 1;
        this.startDate = LocalDate.now();
        saveToConfig();

        HMPlugin.getInstance().getDifficultyManager().reset();

        // Desactivar KeepInventory al resetear
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }

        // Remover slots bloqueados
        HM.LuckyDemon.events.Day40InventoryListener.removeFromAllPlayers();

        Bukkit.broadcast(MessageUtils.format("<gold>⚠ El juego ha sido reseteado al día 1"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }
    }

    public void showInfo(Player player) {
        World world = player.getWorld();

        String weatherStatus = "";
        if (world.hasStorm()) {
            int secondsLeft = world.getWeatherDuration() / 20;
            weatherStatus = " <gray>| <gradient:aqua:blue>⛈ DEATH TRAIN: <white>"
                    + MessageUtils.formatTime(secondsLeft);
        }

        Component actionBar = MessageUtils.format(
                "<gradient:red:gold><bold>PERMADEATH</gradient> <gray>» <yellow>Día " + currentDay + weatherStatus);

        player.sendActionBar(actionBar);
    }

    /**
     * Modificar una propiedad en server.properties
     * NOTA: El cambio solo aplica al reiniciar el servidor
     */
    public static void setServerProperty(String key, String value) {
        try {
            java.io.File serverProperties = new java.io.File("server.properties");
            java.util.Properties props = new java.util.Properties();

            // Leer propiedades actuales
            try (java.io.FileInputStream in = new java.io.FileInputStream(serverProperties)) {
                props.load(in);
            }

            // Modificar la propiedad
            props.setProperty(key, value);

            // Guardar
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(serverProperties)) {
                props.store(out, "Minecraft server properties");
            }

            Bukkit.getLogger().info("[HMPlugin] server.properties modificado: " + key + "=" + value);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[HMPlugin] Error modificando server.properties: " + e.getMessage());
        }
    }
}