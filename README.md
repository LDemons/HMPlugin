# HMPlugin

Plugin de Paper (26.3, Java 25) inspirado en Permadeath, a estilo propio. Cada dia del HM activa mecanicas nuevas que se acumulan con las anteriores. Autor: LDemons / LuckyDemons.

## Base (siempre)

- 3 vidas por jugador. Al perder una sale una animacion de corazon roto (requiere el resource pack, que el plugin envia solo al entrar).
- Sin vidas: permadeath y no puede volver a entrar (los op si).
- Mensajes y sonido de muerte propios, cabeza del jugador al morir, corazones en el tab, menciones en el chat (`@jugador`, `@everyone`) y avisos a Discord.
- Sin PvP hasta el dia 40.
- **Death Train**: cada muerte desata (o alarga) una tormenta electrica. Dura tantas horas como el dia actual y la cuenta vuelve a 1 cada 25 dias (dia 26 = 1 h). Con tormenta no se puede dormir.
- Portal al End bloqueado hasta el dia 30.

## Dia 1

- Netherite baneada; los bastiones no dan netherite.
- Picar ancient debris crea una TNT que explota a los 2 s.

## Dia 10

- Para pasar la noche hay que ser al menos 4 jugadores conectados.
- Aranas con 1 a 3 efectos aleatorios (Speed III, Fuerza IV, Salto V, Regeneracion IV, Resistencia III, Invisibilidad, Glowing o Caida Lenta).
- Creaking con Fuerza I y Speed I.
- 3% de que salga una arana de cueva al romper una vasija.

## Dia 20

- La noche ya no se puede saltar (dormir solo reinicia el contador de los phantoms).
- KeepInventory.
- Animales pasivos, marinos y aldeanos agresivos (2 de dano); los pigmans se enojan al verte.
- Aranas con 3 a 5 efectos y un esqueleto de clase encima.
- **Phantom Gigante**: tamano 9 y vida x2.
- Golems, pigmans, ghasts, guardianes, magma cubes, endermans, brujas, wither skeletons, evokers, phantoms, slimes, drowned y blazes no sueltan drops ni experiencia.
- Ancient City con +64 de oro, cofres de Trial Chamber con +1 totem.
- 50% de Bad Omen V (100 min) al matar raiders; el Bad Omen tambien da Weakness.
- Los murcielagos dan ceguera 15 s al golpear.
- Wardens el doble de rapidos.
- Receta: skeleton skull + 8 bloques de oro = manzana de Notch.

## Dia 25

- Durante el Death Train todos los mobs hostiles tienen Fuerza I, Speed I y Resistencia I.
- Aranas con 5 efectos.
- Creaking con Fuerza II y Speed III.
- **Creeper Electrico** (todos los creepers).
- **Giga Slime** (tamano 15, vida x2) y **Giga Magma Cube** (tamano 16).
- **Ghast Demoniaco**: 40-60 de vida y explosiones mas fuertes.
- **Ravager Elite**: Fuerza II, Speed I, 1% de soltar totem.
- **Armadura Infernal** (solo dias 25 a 29): Botas por Ghast Demoniaco (30%), Pechera por Giga Slime chico (20%), Pantalones por Giga Magma Cube chico (20%), Casco por arana de cueva (30%). El set completo da +4 corazones.

## Dia 30

Desde aqui los mobs especiales llevan nombre propio (sale en las muertes).

- Pisar bedrock te lanza por los aires.
- **Pillager Invisible** con ballesta Quick Charge X.
- **Pigman Blindado** (armadura de diamante).
- Murcielagos: 30% pasan a **Blaze Infernal** (Resistencia II), el resto no aparece.
- Calamares: 30% pasan a **Guardian Acuatico** (Speed II), el resto no aparece.
- Piglins -> **Hoglin Salvaje**.
- **Allay Explosivo**: invisible, el doble de grande, Speed II, TNT en mano; explota al tocarte o al morir.
- **Shulker Explosivo**: sus balas y su muerte dejan TNT (shulker shells al 20%).
- **Ender Creeper** (End): electrico, invisible y esquiva las flechas teletransportandose.
- **Ender Ghast** (End): reemplaza al 1% de los endermans y sale uno al romper cada cristal de los pilares.
- Esqueletos con clase aleatoria: Tactico, Infernal, Guerrero, Asesino, Pesadilla (Tactico y Pesadilla son wither skeletons). Llevan flechas de Dano II.
- **Zombie Jinete**: los zombies salen montados en un caballo zombie.
- Golems con Speed IV, endermans con Fuerza II, silverfish con 5 efectos.
- Totems con 1% de fallo.

## Dia 40

**Jugadores**
- PvP activado.
- Vida maxima -4 corazones.
- Totems con 3% de fallo y gastan 2.
- 5 slots del inventario bloqueados (los libera la **Reliquia del Fin**: shulker shells, bloques de diamante y tinte celeste).
- **Super Golden Apple +** (8 lingotes de oro + manzana dorada): Health Boost I 5 min.
- **Hyper Golden Apple +** (8 bloques de oro + manzana dorada): Regeneracion III 30 s, Absorcion III 2 min y +2 corazones permanentes (estos solo una vez por jugador).
- Antorchas no crafteables.
- Ceguera 1 min muy de vez en cuando bajo la lluvia a cielo abierto.
- Dano x4 al pisar magma o al recibir dano de un Sulfur Cube.

**Mundo y loot**
- Generacion de estructuras desactivada (al reiniciar).
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

## Logros

Salen en el menu de logros (tecla L), pestana **HM**.
- **¿Quién necesita ver?**: mata a un mob hostil mientras estas ciego por un murcielago (dia 20+).
- **Ahorro de Espacio**: sobrevive 2 dias seguidos desde el dia 40 sin tener la Reliquia del Fin.
- **No es mi Día**: ten Hambre, Debilidad y Ceguera a la vez.
- **Duro como la piedra**: sobrevive a 3 explosiones en menos de 1 minuto.

## Comandos

- `/hm` (alias `/permadeath`): `items`, `day`, `awake`, `mensaje`, `lives`, `resetlives`, `addlife`, `reducestorm`, `resetdifficulty`, `resethealth`, `netheriteArmor`, `finalmsg`.
- `/hmreload` (alias `/hmr`), permiso `hmplugin.reload`.
