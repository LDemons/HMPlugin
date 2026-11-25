package HM.LuckyDemon.utils;

import HM.LuckyDemon.HMPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tragamonedas de eventos para todos los conectados, arriba en el centro (donde va el tab): tres rodillos con
 * las cabezas de los jugadores que tienen evento propio y un signo de pregunta (evento del server). Frenan y
 * paran uno por uno; los tres caen en el mismo simbolo y se anuncia el evento.
 * Es el texto de una barra de jefe blanca (invisible en el pack) con la fuente hmplugin:slot de
 * resourcepack/generar_pack.py: las cabezas van dibujadas en el pack porque las de skin no se pueden mover.
 * Los eventos todavia no hacen nada: solo se anuncian (ideas en Obsidian, "Ideas para el plugin").
 */
public final class EventSlot {

    // Mismo orden que SLOT_HEADS en generar_pack.py (para agregar uno: su skin en texturas/cabezas y regenerar)
    private record Event(String name, String description) {
    }

    // Textos provisionales sacados de las ideas de Obsidian
    private static final Map<String, Event> PLAYER_EVENTS = new LinkedHashMap<>();
    static {
        PLAYER_EVENTS.put("Palgato", new Event("Lluvia ácida",
                "En los próximos 10 minutos habrá lluvia ácida. Busca refugio."));
        PLAYER_EVENTS.put("Pelaoordinario", new Event("Loot x3 en The Beginning",
                "Durante 20 minutos The Beginning tiene loot x3, a cambio de que: los cofres pueden explotar, "
                        + "los creepers son 3 veces más rápidos, los ghasts son 3 veces más grandes, los withers están "
                        + "al revés, los vex son abejas y los wither skeletons les tienen miedo a los creepers."));
        PLAYER_EVENTS.put("Eledimins", new Event("Enfermedad",
                "Todos los jugadores sufren una enfermedad al azar."));
    }

    private static final List<Event> SERVER_EVENTS = List.of(
            new Event("Al cielo", "Tienen 5 minutos para subir a la capa 250 o empezarán a recibir daño. "
                    + "Después deben quedarse ahí arriba otros 5 minutos."),
            new Event("Al abismo", "Tienen 5 minutos para bajar a la capa -50 o empezarán a recibir daño. "
                    + "Después deben quedarse ahí abajo otros 5 minutos."),
            new Event("Levitación", "En 10 segundos todos tendrán Levitación 50 durante 3 segundos. Busquen un techo."),
            new Event("Lluvia de creepers", "Durante 5 minutos aparecerán creepers eléctricos con 1 de vida "
                    + "en la posición de los jugadores."),
            new Event("Luna asesina", "Mirar a la luna te mata de un golpe. Dura hasta que alguien la mire."),
            new Event("Branquias", "Ahora respiran bajo el agua, pero se asfixian fuera de ella."),
            new Event("Bloques que desaparecen", "Durante 3 minutos, cada 10 segundos desaparece un tipo de bloque "
                    + "en los chunks cercanos a cada jugador."),
            new Event("Warden cazador", "A cada jugador le aparece un Warden brillante durante 3 minutos. "
                    + "Te ve a través de las paredes y te perseguirá hasta que acabe el tiempo."),
            new Event("Mob a matar", "A cada jugador le saldrá un mob que debe matar. "
                    + "Si no lo hace, jugará con 2 corazones menos durante 1 hora."),
            new Event("Creepers cambiantes", "Durante 5 minutos aparecerán creepers eléctricos con 1 de vida en tu "
                    + "posición. A los 3 minutos empezarán a cambiarte de lugar con otro jugador al azar."),
            new Event("Todos a 0 0", "Tienen 5 minutos para llegar a 0 0 o sufrirán las consecuencias. Tras 2 minutos "
                    + "ahí, les dará ceguera y aparecerán creepers eléctricos con 1 de vida durante 3 minutos."),
            new Event("Shinzo wo Sasageyo!", "Apareció un Wither Gigante en el techo del Nether. Mátenlo antes de "
                    + "que acabe el tiempo o perderán 3 corazones el resto del día."),
            new Event("Dimensiones tóxicas", "Durante 20 minutos el Overworld, el Nether y el End son tóxicos: "
                    + "quedarse en cualquiera de ellos hace daño."),
            new Event("in extrema res", "Todos son llevados a The Beginning y deben salir antes de que acabe el "
                    + "tiempo. Al salir aparecen en el End, después en el Nether, hasta volver al Overworld. Cada "
                    + "ciertos minutos una dimensión se vuelve tóxica hasta que solo el Overworld es seguro."));

    // Caracteres de la fuente hmplugin:slot (generar_pack.py)
    private static final char LIGHTS_A = '', LIGHTS_ON = '', QUESTION = '', FIRST_HEAD = '';
    private static final char BACK = '', GAP_EDGE = '', GAP = '';
    private static final Key FONT = Key.key("hmplugin", "slot");

    private static final int[] STOPS = {40, 60, 85}; // tick en que para cada rodillo
    private static final int SLOW = 15;              // ticks antes de parar en que gira a 1/3 de velocidad
    private static final int HOLD = 80;              // ticks que queda el resultado

    private static boolean spinning;

    private EventSlot() {
    }

    /** Gira para todos los conectados. Devuelve false si ya habia una tirada en curso. */
    public static boolean spin() {
        if (spinning) return false;
        spinning = true;

        List<String> names = new ArrayList<>(PLAYER_EVENTS.keySet());
        int n = names.size() + 1; // simbolos: cabezas + "?" (el ultimo)

        Random r = ThreadLocalRandom.current();
        int result = r.nextBoolean() ? n - 1 : r.nextInt(names.size()); // 50% evento del server
        boolean server = result == n - 1;
        Event event = server ? SERVER_EVENTS.get(r.nextInt(SERVER_EVENTS.size())) : PLAYER_EVENTS.get(names.get(result));
        String owner = server ? null : names.get(result);
        int[] pos = {r.nextInt(n), r.nextInt(n), r.nextInt(n)};

        BossBar bar = BossBar.bossBar(row(LIGHTS_A, pos, n), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        Bukkit.getServer().showBossBar(bar);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > STOPS[2]) {
                    if (tick >= STOPS[2] + HOLD) {
                        Bukkit.getServer().hideBossBar(bar);
                        spinning = false;
                        cancel();
                    }
                    return;
                }

                boolean moved = false;
                for (int i = 0; i < 3; i++) {
                    if (tick < STOPS[i] && (STOPS[i] - tick > SLOW || tick % 3 == 0)) {
                        pos[i] = (pos[i] + 1) % n;
                        moved = true;
                    } else if (tick == STOPS[i]) {
                        pos[i] = result;
                        sound(Sound.BLOCK_BELL_USE, 0.8f, 0.5f + i * 0.05f); // campana grave, cada vez un poco mas aguda
                    }
                }
                if (moved) sound(Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 1.2f + r.nextFloat() * 0.4f);

                boolean done = tick == STOPS[2];
                bar.name(row(done ? LIGHTS_ON : (char) (LIGHTS_A + tick / 4 % 2), pos, n)); // luces alternando
                if (!done) return;

                sound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
                sound(Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
                Bukkit.getServer().sendActionBar(MessageUtils.format("<dark_red><bold>EVENTO DE DIFICULTAD</bold> <red><italic>"
                        + event.name()));
                // Titulo: cabeza del jugador (si el evento es suyo) y nombre en cursiva
                Component title = MessageUtils.format("<red><bold><italic>" + event.name());
                if (owner != null) title = head(owner).append(Component.space()).append(title);
                String line = "<dark_red><strikethrough>" + " ".repeat(50) + "</strikethrough>";
                Bukkit.broadcast(MessageUtils.format(line + "\n<dark_red><bold>EVENTO DE DIFICULTAD</bold>\n")
                        .append(title)
                        .append(MessageUtils.format("\n<gray>" + event.description() + "\n" + line)));
            }
        }.runTaskTimer(HMPlugin.getInstance(), 0L, 1L);
        return true;
    }

    // Marco, vuelta al inicio y los tres simbolos en sus ventanas; el ancho total es el del marco (queda centrado)
    private static Component row(char panel, int[] pos, int n) {
        String s = "" + panel + BACK + GAP_EDGE + symbol(pos[0], n) + GAP + symbol(pos[1], n) + GAP
                + symbol(pos[2], n) + GAP_EDGE;
        return Component.text(s, NamedTextColor.WHITE).font(FONT).shadowColor(ShadowColor.none());
    }

    private static char symbol(int index, int n) {
        return index == n - 1 ? QUESTION : (char) (FIRST_HEAD + index);
    }

    // Igual que el tab: con el jugador conectado y con skin se usa su perfil; si no, el cliente la busca por nombre
    private static Component head(String name) {
        Player p = Bukkit.getPlayerExact(name);
        ObjectContents head = p != null && p.getPlayerProfile().hasTextures()
                ? ObjectContents.playerHead(p)
                : ObjectContents.playerHead(name);
        return Component.object(head).color(NamedTextColor.WHITE);
    }

    private static void sound(Sound sound, float volume, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, sound, volume, pitch);
    }
}
