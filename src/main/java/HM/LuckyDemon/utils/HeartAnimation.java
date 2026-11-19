package HM.LuckyDemon.utils;

import HM.LuckyDemon.HMPlugin;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

/**
 * Animacion de corazon perdido a pantalla completa. Los fotogramas son caracteres de la fuente
 * hmplugin:heart del resource pack (resourcepack/generar_pack.py): aparece, late dos veces,
 * se agrieta, destella y se parte en dos. Al perder la ultima vida el corazon es palido, con mas
 * grietas, la grieta tarda 1.5 s mas y tiembla antes de romperse.
 */
public final class HeartAnimation {

    // Deben coincidir con generar_pack.py
    private static final char NORMAL = '';   // 20 fotogramas
    private static final char PALE = '';     // 26 fotogramas: los mismos + 6 de temblor despues de la grieta
    private static final int CRACK = 10, CRACK_FRAMES = 3, SHAKE_FRAMES = 6;
    private static final int TICKS = 2, SLOW_CRACK_TICKS = 12; // 3 fotogramas de grieta x 10 ticks extra = 1.5 s

    // stay largo: el titulo se reenvia cada tick, asi un fotograma lento no se apaga
    private static final Title.Times FRAME = Title.Times.times(Duration.ZERO, Duration.ofMillis(200), Duration.ZERO);
    private static final Title.Times LAST = Title.Times.times(Duration.ZERO, Duration.ofMillis(100), Duration.ofMillis(300));

    private HeartAnimation() {
    }

    public static void play(Player player, int remaining, int max) {
        boolean lastLife = remaining == 0;
        char first = lastLife ? PALE : NORMAL;
        int shake = lastLife ? SHAKE_FRAMES : 0;
        int shakeStart = CRACK + CRACK_FRAMES;
        int shatter = shakeStart + shake + 1;      // despues del destello
        int frames = 20 + shake;

        new BukkitRunnable() {
            int frame = 0;
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (tick == 0) onFrameStart();
                if (frame == frames) return;

                String image = "<!shadow><font:hmplugin:heart>" + (char) (first + frame) + "</font>";
                player.showTitle(Title.title(MessageUtils.format(image), MessageUtils.format(""),
                        frame == frames - 1 ? LAST : FRAME));
                player.sendActionBar(MessageUtils.format(counter(remaining, max, frame, shatter)));

                boolean slow = lastLife && frame >= CRACK && frame < shakeStart;
                if (++tick >= (slow ? SLOW_CRACK_TICKS : TICKS)) {
                    tick = 0;
                    frame++;
                }
            }

            private void onFrameStart() {
                if (frame == 5 || frame == 8) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, frame == 5 ? 0.9f : 1.2f);
                } else if (frame >= CRACK && frame < shakeStart && (frame == CRACK || lastLife)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_HIT, 1.0f, 0.6f - (frame - CRACK) * 0.1f);
                } else if (frame >= shakeStart && frame < shakeStart + shake) {
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_HIT, 0.5f, 0.8f + (frame % 2) * 0.2f);
                } else if (frame == shatter) {
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.6f);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40,
                            0.4, 0.5, 0.4, new Particle.DustOptions(lastLife ? Color.fromRGB(230, 170, 180) : Color.RED, 1.5f));
                } else if (frame == frames) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
                    player.sendActionBar(MessageUtils.format(counter(remaining, max, frame, shatter)));
                    cancel();
                }
            }
        }.runTaskTimer(HMPlugin.getInstance(), 0L, 1L);
    }

    // Contador en la barra de accion: el corazon perdido parpadea hasta que se rompe
    private static String counter(int remaining, int max, int frame, int shatter) {
        boolean broken = frame >= shatter;
        String lost = broken ? "<dark_gray>❤" : (frame % 2 == 0 ? "<red>❤" : "<white>❤");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(i < remaining ? "<red>❤" : i == remaining ? lost : "<dark_gray>❤").append(" ");
        }
        return sb.append("<yellow>").append(broken ? remaining : remaining + 1).append("/").append(max)
                .append(" vidas").toString();
    }
}
