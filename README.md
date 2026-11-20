# HMPlugin

Plugin de Paper (26.3, Java 25) inspirado en Permadeath, a estilo propio. Cada dia del HM activa mecanicas nuevas que se acumulan con las anteriores. Autor: LDemons / LuckyDemons.

## Base (siempre)

- 3 vidas por jugador. Al perder una sale una animacion de corazon roto (requiere el resource pack, que el plugin envia solo al entrar).
- Sin vidas: permadeath y no puede volver a entrar (los op si).
- Mensajes y sonido de muerte propios, cabeza del jugador al morir, corazones en el tab, menciones en el chat y avisos a Discord.
- Sin PvP hasta el dia 40.
- **Death Train**: al dormir se desata una tormenta que dura tantas horas como el dia actual (se reinicia cada 25 dias).
- **Super Golden Apple +** (8 lingotes de oro + manzana dorada): Health Boost I 5 min.
- **Hyper Golden Apple +** (8 bloques de oro + manzana dorada): +2 corazones permanentes, una sola vez por jugador.

## Dia 1

- Netherite baneada; los bastiones no dan netherite.
- Picar ancient debris crea una TNT que explota a los 2 s.

## Dia 10

- Creaking con Fuerza I y Speed I.
- 3% de que salga una arana de cueva al romper una vasija.

## Dia 20

- Animales pasivos y marinos agresivos; los pigmans se enojan al verte.
- Aranas con esqueleto jinete (5 clases).
- KeepInventory.
- Ancient City con +64 de oro, cofres de Trial Chamber con +1 totem.
- Bad Omen V (que da Weakness) al matar raiders.
- Murcielagos dan ceguera; Wardens con el doble de velocidad.
- Skeleton skull para craftear la manzana de Notch.

## Dia 25

- Creaking con Fuerza II y Speed III.
- Giga Slimes y Giga Magma Cubes.
- Ghasts Demoniacos.
- Ravagers (1% de soltar totem).
- Armadura Infernal.

## Dia 30

Desde aqui los mobs especiales llevan nombre propio (sale en las muertes).

- Creepers electricos, Pillagers Invisibles, Pigmans Blindados (diamante).
- Calamares pasan a guardianes, murcielagos a blazes, piglins a hoglins.
- Allays invisibles con TNT, shulkers explosivos.
- Ender Creepers y Ender Ghasts (en el End reemplazan al 1% de los endermans y sale uno al romper cada cristal de los pilares).
- Esqueletos con clase aleatoria: Tactico, Infernal, Guerrero, Asesino, Pesadilla (Infernal y Pesadilla son wither skeletons).
- **Zombie Jinete**: los zombies salen montados en un caballo zombie.
- Phantom Gigante.
- Golems con Speed IV, endermans con Fuerza II, silverfish con 5 efectos.
- Totems con 1% de fallo.

## Dia 40

**Jugadores**
- PvP activado.
- Vida maxima -4 corazones.
- Totems con 3% de fallo y gastan 2.
- 5 slots del inventario bloqueados (los libera la Reliquia del Fin, crafteable solo desde el dia 40).
- Antorchas no crafteables.
- Ceguera 1 min muy de vez en cuando bajo la lluvia a cielo abierto.
- Dano x4 al pisar magma o al recibir dano de un Sulfur Cube.

**Mundo y loot**
- Generacion de estructuras desactivada.
- Elytras casi rotas en los End Ships.
- 9 chorus flowers = dragon's breath.
- Shulker shells al 2%; receta Shulker Box -> 2 Shulker Shells.
- Mobs hostiles en las Mushroom Islands.
- Cofres de cobre que explotan al abrirlos (tambien si los abre un Copper Golem).
- Bad Omen al matar raiders: 1 de cada 5 y 10 min.

**Mobs**
- Zombies -> **Vindicator Furioso** (Fuerza I, vida x2).
- Aranas -> aranas de cueva (con esqueleto jinete: **Arana de Cueva Jinete**).
- Vacas, mooshrooms, ovejas, cerdos y pollos -> **Ravager de Granja**.
- Endermans del Nether -> Ender Creepers; 10% de **Endermans Hostiles**.
- Creepers con Speed II y Resistencia II; golems con Fuerza I; guardianes con Speed III y Resistencia II.
- **Phantom Jinete** (phantom con esqueleto de clase).
- Lobos -> gatos; todo gato u ocelote es un **Gato Supernova** (se anuncia y explota a los 30 s, maximo 2 a la vez).
- **Bruja Imposible**: vida x2, Speed II, pociones de Dano IV, Veneno III o Lentitud V.
- **Demonio Flotante** (75% de los ghasts): su bola da Levitacion y Wither V.
- 5% de **pigmans montados** en cerdo, abeja, ghast o magma cube.
- **Sulfur Cube Ardiente**: lleva un bloque de magma dentro.
- **Nautilus Zombie Jinete**: zombie nautilus con un drowned encima.
- **Armadillo Acorazado**: 300 de vida; suelta 64 Blaze Rods o 64 bloques de Lapis.
- Conejos con 1 de vida, Speed 50 y 10 de dano.
- Ranas: al comerse un slime sueltan 5 Super Golden Apple +, al comerse un magma cube 5 manzanas de Notch.

## Comandos

- `/hm` (alias `/permadeath`): `items`, `day`, `awake`, `mensaje`, `lives`, `resetlives`, `addlife`, `reducestorm`, `resetdifficulty`, `resethealth`, `netheriteArmor`, `finalmsg`.
- `/hmreload` (alias `/hmr`), permiso `hmplugin.reload`.
