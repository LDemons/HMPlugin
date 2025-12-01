package HM.LuckyDemon;

import HM.LuckyDemon.managers.DifficultyManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class HMPlugin extends JavaPlugin {
    private DifficultyManager difficultyManager;

    private static HMPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        // Crear la carpeta de configuración si no existe
        saveDefaultConfig();

        // Mensaje de inicio en consola usando el nuevo sistema
        MessageUtils.send(this.getServer().getConsoleSender(),
                "<gradient:red:dark_red>Permadeath Reborn (HMPluggin) activado correctamente.</gradient>");
        MessageUtils.send(this.getServer().getConsoleSender(),
                "<gray>Versión corriendo en: <yellow>" + getServer().getVersion());

        // Registrar comando
        getCommand("hm").setExecutor(new HM.LuckyDemon.commands.MainCommand());

        // Registrar Recetas
        HM.LuckyDemon.recipes.RecipeManager.registerRecipes();

        // Registrar eventos
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.EntityListener(this), this);

        // Registrar mecánicas día 20+
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.Day20MechanicsListener(), this);
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.LootModifierListener(), this);
        
        // Registrar mecánicas día 25+ (Ghast demoníaco)
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.GhastFireballListener(), this);

        // Tarea de Tormenta
        new HM.LuckyDemon.tasks.StormTask().runTaskTimer(this, 0L, 20L);

        difficultyManager = new DifficultyManager(this);

        // Activar KeepInventory si el día actual es >= 20
        int currentDay = HM.LuckyDemon.managers.GameManager.getInstance().getDay();
        if (currentDay >= 20) {
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
            }
            MessageUtils.send(this.getServer().getConsoleSender(),
                    "<green>KeepInventory activado (Día " + currentDay + " >= 20)");
        }
    }

    @Override
    public void onDisable() {
        MessageUtils.send(this.getServer().getConsoleSender(), "<red>Permadeath desactivado.");
    }

    public static HMPlugin getInstance() {
        return instance;
    }

    public DifficultyManager getDifficultyManager() {
        return difficultyManager;
    }
}