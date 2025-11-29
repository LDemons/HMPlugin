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
        MessageUtils.send(this.getServer().getConsoleSender(), "<gradient:red:dark_red>Permadeath Reborn (HMPluggin) activado correctamente.</gradient>");
        MessageUtils.send(this.getServer().getConsoleSender(), "<gray>Versión corriendo en: <yellow>" + getServer().getVersion());

        // Registrar comando
        getCommand("hm").setExecutor(new HM.LuckyDemon.commands.MainCommand());

        // Registrar Recetas
        HM.LuckyDemon.recipes.RecipeManager.registerRecipes();

        // Registrar eventos
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new HM.LuckyDemon.events.EntityListener(this), this);

        // Tarea de Tormenta
        // Se ejecuta cada 20 ticks (1 segundo) para actualizar el contador
        new HM.LuckyDemon.tasks.StormTask().runTaskTimer(this, 0L, 20L);

        difficultyManager = new DifficultyManager(this);

    }

    @Override
    public void onDisable() {
        MessageUtils.send(this.getServer().getConsoleSender(), "<red>Permadeath desactivado.");
    }

    // Metodo estático para obtener la instancia del plugin desde cualquier lado

    public static HMPlugin getInstance() {
        return instance;
    }

    public DifficultyManager getDifficultyManager() {
        return difficultyManager;
    }
}