package HM.LuckyDemon.beginning;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.structure.Structure;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.List;
import java.util.Random;

/**
 * Mundo de The Beginning: islas de purpur por ruido a Y=100 con "arboles" de sea lantern y muros de end stone,
 * islas chicas sueltas en el vacio y ciudades Ytic. Las estructuras se pegan por trozos, cada chunk pega solo
 * los bloques que le caen dentro, asi no hace falta cargar chunks vecinos.
 */
public class BeginningGenerator extends ChunkGenerator {

    private static final int HEIGHT = 100;
    private static final int ISLAND_Y = 120;
    /** Altura de cada isla respecto a ISLAND_Y (el offset de WorldEdit de cada schematic original). */
    private static final int[] ISLAND_OFFSET_Y = { -14, -17, -13, -15, -9 };
    /** Una Ytic por cada region de YTIC_CELL x YTIC_CELL bloques (512 al principio, Nacho las pidio mas comunes). */
    private static final int YTIC_CELL = 256;

    /** Bloques de una estructura ya leidos (getBlocks crea los BlockState de nuevo en cada llamada). */
    private record Piece(List<BlockState> blocks, int sizeX, int sizeZ) {
        Piece(Structure s) {
            this(s.getPalettes().get(0).getBlocks(), s.getSize().getBlockX(), s.getSize().getBlockZ());
        }
    }

    private final List<Piece> islands;
    private final Piece ytic;
    private volatile SimplexOctaveGenerator noise;

    public BeginningGenerator(List<Structure> islands, Structure ytic) {
        this.islands = islands.stream().map(Piece::new).toList();
        this.ytic = new Piece(ytic);
    }

    private SimplexOctaveGenerator noise(long seed) {
        SimplexOctaveGenerator n = noise;
        if (n == null) {
            n = new SimplexOctaveGenerator(new Random(seed), 8);
            n.setScale(0.02D);
            noise = n;
        }
        return n;
    }

    private int height(long seed, int x, int z) {
        return (int) (noise(seed).noise(x, z, 0.5D, 0.5D) * 15);
    }

    private static Random chunkRandom(long seed, int cx, int cz, long salt) {
        return new Random(seed ^ (cx * 341873128712L + cz * 132897987541L) ^ salt);
    }

    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int h = height(info.getSeed(), chunkX * 16 + x, chunkZ * 16 + z);
                for (int i = 0; i < h / 3; i++) chunk.setBlock(x, HEIGHT + i, z, Material.PURPUR_BLOCK);
                for (int i = 0; i < h; i++) chunk.setBlock(x, HEIGHT - i - 1, z, Material.PURPUR_BLOCK);
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(org.bukkit.World world) {
        return List.of(new BlockPopulator() {
            @Override
            public void populate(WorldInfo info, Random random, int chunkX, int chunkZ, LimitedRegion region) {
                long seed = info.getSeed();

                // Islas chicas: 1 de cada 20 chunks de vacio. Las de los chunks vecinos pueden asomar a este
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int cx = chunkX + dx, cz = chunkZ + dz;
                        Random r = chunkRandom(seed, cx, cz, 1L);
                        if (r.nextInt(20) != 0 || height(seed, cx * 16 + 8, cz * 16 + 8) > 0) continue;
                        int i = r.nextInt(islands.size());
                        Piece island = islands.get(i);
                        paste(region, island, chunkX, chunkZ, cx * 16 + 8 - island.sizeX() / 2,
                                ISLAND_Y + ISLAND_OFFSET_Y[i], cz * 16 + 8 - island.sizeZ() / 2);
                    }
                }

                // Ytic: una por region, entera dentro de ella (asi solo hay que mirar la region de este chunk)
                int rx = Math.floorDiv(chunkX * 16, YTIC_CELL), rz = Math.floorDiv(chunkZ * 16, YTIC_CELL);
                Random r = chunkRandom(seed, rx, rz, 2L);
                paste(region, ytic, chunkX, chunkZ,
                        rx * YTIC_CELL + r.nextInt(YTIC_CELL - ytic.sizeX()), 0,
                        rz * YTIC_CELL + r.nextInt(YTIC_CELL - ytic.sizeZ()));

                // "Arbol": chorus con las flores cambiadas por sea lantern y las ramas por muros de end stone
                int x = chunkX * 16 + random.nextInt(16), z = chunkZ * 16 + random.nextInt(16);
                int y = HEIGHT + 10;
                while (y > HEIGHT - 5 && region.getType(x, y, z) == Material.AIR) y--;
                if (y >= HEIGHT && y < HEIGHT + 5 && region.getType(x, y, z) == Material.PURPUR_BLOCK) {
                    region.generateTree(new Location(null, x, y + 1, z), random, TreeType.CHORUS_PLANT,
                            (BlockState s) -> {
                                if (s.getType() == Material.CHORUS_FLOWER) s.setType(Material.SEA_LANTERN);
                                else if (s.getType() == Material.CHORUS_PLANT) s.setType(Material.END_STONE_BRICK_WALL);
                            });
                }
            }
        });
    }

    /** Pega la parte de la estructura (esquina minima en ox, oy, oz) que cae dentro del chunk chunkX, chunkZ. */
    private static void paste(LimitedRegion region, Piece piece, int chunkX, int chunkZ, int ox, int oy, int oz) {
        int minX = chunkX * 16, minZ = chunkZ * 16;
        if (ox > minX + 15 || oz > minZ + 15 || ox + piece.sizeX() <= minX || oz + piece.sizeZ() <= minZ) {
            return;
        }
        for (BlockState b : piece.blocks()) {
            int x = ox + b.getX(), z = oz + b.getZ();
            if (x >> 4 == chunkX && z >> 4 == chunkZ) {
                int y = oy + b.getY();
                region.setBlockData(x, y, z, b.getBlockData());
                // Datos de spawners y cofres: setBlockState pide que el bloque ya este puesto y solo acepta TileState.
                // Un fallo aca tumba todo el sistema de chunks (cierra el server), asi que se avisa y se sigue
                if (b instanceof org.bukkit.block.TileState) {
                    try {
                        // Spawner de armor stand = bloque de Netherite Infernal. Se deja vacio (el armor stand
                        // asomaba por arriba) y lo tapa un display con la textura del bloque
                        if (b instanceof org.bukkit.block.CreatureSpawner s
                                && s.getSpawnedType() == org.bukkit.entity.EntityType.ARMOR_STAND) {
                            region.spawn(new Location(null, x + 0.5, y + 0.5, z + 0.5),
                                    org.bukkit.entity.ItemDisplay.class, BeginningManager::infernalDisplay);
                        } else {
                            region.setBlockState(x, y, z, b);
                        }
                    } catch (RuntimeException e) {
                        org.bukkit.Bukkit.getLogger().warning("[HMPlugin] The Beginning: no se pudo copiar "
                                + b.getType() + " en " + x + " " + y + " " + z + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}
