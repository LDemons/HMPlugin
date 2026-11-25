# HMPlugin

Plugin de Paper (26.3, Java 25) inspirado en Permadeath, a estilo propio. Cada dia del HM activa mecanicas nuevas que se acumulan con las anteriores. Autor: LDemons / LuckyDemons.

## Base (siempre)

- 3 vidas por jugador. Al perder una sale una animacion de corazon roto (requiere el resource pack, que el plugin envia solo al entrar).
- Sin vidas: permadeath y no puede volver a entrar (los op si).
- Mensajes y sonido de muerte propios, cabeza del jugador al morir, corazones en el tab, menciones en el chat (`@jugador`, `@everyone`) y avisos a Discord.
- En el tab y en el chat cada jugador sale con la cabeza de su skin y un corazon con sus vidas: verde 3, amarillo 2, rojo 1, gris 0.
- Sin PvP hasta el dia 40.
- **Death Train**: cada muerte desata (o alarga) una tormenta electrica. Dura tantas horas como el dia actual y la cuenta vuelve a 1 cada 25 dias (dia 26 = 1 h). Con tormenta no se puede dormir.
- Portal al End bloqueado hasta el dia 30.

## Dia 1

- Netherite baneada; los bastiones no dan nada de netherite (ni chatarra, ni equipo, ni ancient debris).
- Picar ancient debris crea una TNT que explota a los 2 s.

## Dia 10

- Mas mobs hostiles en el Overworld y el End: el tope pasa de 70 a 120 (el Nether queda en 70). En el Overworld y el Nether los mobs aparecen mas despacio (no reaparecen al instante al matarlos). Nunca mas de 8 magma cubes naturales alrededor de cada jugador.
- Para pasar la noche hay que ser al menos 4 jugadores conectados.
- Aranas con 1 a 3 efectos aleatorios (Speed III, Fuerza IV, Salto V, Regeneracion IV, Resistencia III, Invisibilidad, Glowing o Caida Lenta).
- Creaking con Fuerza I y Speed I.
- 3% de que salga una arana de cueva al romper una vasija.

## Dia 20

- La noche ya no se puede saltar (dormir solo reinicia el contador de los phantoms).
- KeepInventory.
- Animales pasivos, marinos y aldeanos agresivos (2 de dano); los pigmans se enojan al verte.
- Aranas con 3 a 5 efectos y un esqueleto de clase encima.
- Esqueletos de clase (version facil): Tactico (wither, cota de malla, arco Punch XX, 20 corazones), Infernal (hierro, hacha de hierro Fire Aspect II, 10 corazones), Guerrero (diamante, arco, 10 corazones), Asesino (oro, ballesta Sharpness XX, 20 corazones), Pesadilla (wither, cuero rojo, arco Power X, 20 corazones).
- **Phantom Gigante**: tamano 9 y vida x2.
- Golems, pigmans, ghasts, guardianes, magma cubes, endermans, brujas, wither skeletons, evokers, phantoms, slimes, drowned y blazes no sueltan drops ni experiencia.
- Ancient City con +64 de oro, cofres de Trial Chamber con +1 totem.
- 50% de Bad Omen V (100 min) al matar raiders; el Bad Omen tambien da Weakness.
- Los murcielagos dan ceguera 15 s al golpear.
- Wardens el doble de rapidos.
- Receta: skeleton skull + 8 bloques de oro = manzana de Notch.
- Los ravagers tienen 1% de soltar un totem.

## Dia 25

- Durante el Death Train todos los mobs hostiles tienen Fuerza I, Speed I y Resistencia I.
- Aranas con 5 efectos.
- Creaking con Fuerza II y Speed III.
- **Giga Slime** (tamano 15, vida x2) y **Giga Magma Cube** (tamano 16).
- **Ghast Demoniaco**: 40-60 de vida y explosiones mas fuertes.
- **Ravager Elite**: Fuerza II, Speed I, 20% de soltar totem.
- **Armadura Infernal** (solo dias 25 a 29): Botas por Ghast Demoniaco (30%), Pechera por Giga Slime chico (20%), Pantalones por Giga Magma Cube chico (20%), Casco por arana de cueva (30%). El set completo da +4 corazones.

## Dia 30

Desde aqui los mobs especiales llevan nombre propio (sale en las muertes).

- Pisar bedrock te lanza por los aires.
- **Creeper Electrico** (todos los creepers).
- **Pillager Invisible** con ballesta Quick Charge X.
- **Pigman Blindado** (armadura de diamante).
- Murcielagos -> **Blaze Infernal** (Resistencia II). Si ya hay 4 blazes a 64 bloques, no sale nada.
- Calamares -> **Guardian Acuatico** (Speed II). Si ya hay 4 guardianes a 64 bloques, no sale nada.
- Piglins -> **Hoglin Salvaje**.
- **Allay Explosivo**: invisible, el doble de grande, Speed II, TNT en mano; explota al tocarte o al morir.
- **Shulker Explosivo**: sus balas y su muerte dejan TNT (shulker shells al 20%).
- **Ender Creeper** (End): electrico, invisible y esquiva las flechas teletransportandose.
- **Ender Ghast** (End): reemplaza al 1% de los endermans y sale uno al romper cada cristal de los pilares.
- **Permadeath Demon** (el dragon del End, tambien el que se respawnee): 1000 corazones y se cura la mitad. Las torres de obsidiana son de bedrock. Cada 30-90 s suelta 6 TNT (no rompen bloques, lanzan end stone), caen rayos cerca del portal y los cristales de los pilares vuelven a salir a los 1-8 min. Cada 60 s ataca: lluvia de rayos sobre los jugadores, nube verde de 15 s sobre 4 endermans (si mueren dentro, les salta un totem), o vision nocturna de aviso y a los 5 s una nube de Dano II donde estes. Al posarse en el portal suele girar 15 s rodeado de nubes de Dano IV. Su aliento convierte el end stone en bedrock. Con 300 corazones o menos se enfurece: ataca cada 40 s, los rayos tambien hacen dano y la mitad de su aliento es humo de Dano II que quita todos los efectos.
- Todos los esqueletos tienen clase aleatoria y llevan flechas de Dano II (los wither skeletons del Nether no; solo los jinetes de clase). Las clases se vuelven mas dificiles (todas con 20 corazones): Tactico arco Punch XXX y Power XXV, Infernal hacha de diamante Fire Aspect X, Guerrero diamante con Proteccion IV, Asesino ballesta Sharpness XXV y Speed II, Pesadilla arco Power L.
- **Zombie Jinete**: los zombies salen montados en un caballo zombie.
- Golems con Speed IV, endermans con Fuerza II, silverfish con 5 efectos.
- Totems con 1% de fallo.

## Dia 40

**Jugadores**
- PvP activado.
- Vida maxima -4 corazones.
- Totems con 3% de fallo y gastan 2.
- 5 slots del inventario bloqueados (los libera la **Reliquia del Fin**: 2 shulker shells y un bloque de diamante; las teclas numericas y la F tampoco sirven para sacar los bloqueadores).
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
- Endermans del Nether -> Ender Creepers (nunca mas de 8 creepers alrededor de cada jugador); 10% de **Endermans Hostiles**.
- Creepers con Speed II y Resistencia II; golems con Fuerza I; guardianes con Speed III, Resistencia II y el rayo carga el doble de rapido.
- **Phantom Jinete** (phantom con esqueleto de clase).
- Lobos -> gatos; todo gato u ocelote es un **Gato Supernova** (se anuncia y explota a los 30 s, maximo 2 a la vez).
- **Bruja Imposible**: vida x2, Speed II, pociones de Dano IV, Veneno III o Lentitud V.
- **Demonio Flotante** (75% de los ghasts): su bola da Levitacion y Wither V.
- 5% de **pigmans montados** en cerdo, abeja, ghast, magma cube o **Ultra Ravager** (20% cada uno).
- **Ultra Ravager** (250 corazones, Speed II, Fuerza II, suelta 1 totem) lleva encima a **Carlos el Esclavo** (pigman de 75 corazones con cota de malla completa Proteccion II y espada de madera Empuje X, suelta 32 lingotes de oro) y sobre el a **Jess la Emperatriz** (aldeana de 120 corazones, suelta 2 manzanas doradas).
- **Sulfur Cube Ardiente**: lleva un bloque de magma dentro.
- **Nautilus Zombie Jinete**: zombie nautilus con un drowned encima.
- **Armadillo Acorazado**: 500 de vida; suelta 64 Blaze Rods o 64 bloques de Lapis.
- Conejos con 1 de vida, Speed 50 y 10 de dano.
- Ranas: al comerse un slime sueltan 5 Super Golden Apple +, al comerse un magma cube 5 manzanas de Notch.

### The Beginning
- Aparece el dia 40: un portal flotante escondido en algun lugar del Overworld (a menos de 3000 bloques del centro).
- Antes del dia 50, entrar al portal te mata y se anuncia en el chat.
- Desde el dia 50 se puede entrar. Se cierra durante el Death Train (la lluvia normal no) y saca a todos los que esten dentro.
- Mundo de islas de purpur con arboles de sea lantern, islas chicas con spawners, cofres y bloques de Netherite Infernal, y ciudades Ytic (una cada 256x256 bloques).
- Mobs: **Wither Skeleton Rosáceo** (100 de vida, espada de netherite con Filo, Speed III), **Vex Definitivo**
  (calabaza encantada en la cabeza, cristal del End con Filo XV y Empuje XX; solo sale de los spawners de las estructuras),
  **Ender Ghast Definitivo** (150 de vida, explosion de potencia 10; aparece solo por encima de Y 95, mas abajo solo sale
  de spawners) y **Quantum Creeper**
  (invisible, electrico, 100 de vida, su explosion hace x5 de dano: mata casi todo lo que tenga cerca). Maximo 70 mobs en el mundo y 8 a menos de 32 bloques, y no salen en grupo.
- Los spawners de wither de las islas chicas sacan 3 **Mini Withers** (un tercio del tamano, 100 de vida) cada 5 minutos;
  los de las ciudades Ytic, un wither normal cada 15 minutos. Ninguno saca mas mientras quede un wither cerca.
- Los cofres traen loot al abrirlos: diamantes, oro, manzanas doradas, botellas de experiencia, cohetes y a veces un totem,
  repartido en varios montones por el cofre.
  Cada cofre trae al menos 3 objetos distintos.
- El cofre de herramientas de las ciudades Ytic trae herramientas de netherite irrompibles.
- No se pueden vaciar cubetas, no se pueden crear portales y nadie puede ser invisible.
- Picar un bloque de Netherite Infernal con pico suelta un **Bloque de Netherite Infernal**.
- **Armadura de Netherite Infernal** (roja): una pieza de netherite en el centro de la mesa y 4 Bloques de Netherite
  Infernal arriba, abajo y a los lados. Conserva los encantamientos de la pieza. Irrompible; el set completo da +5 corazones
  (en vez de los +4 de la Armadura Infernal) y Resistencia I mientras la lleves puesta.

## Dia 50
- Los totems tienen 5% de probabilidad de fallar.
- **Modo UHC**: durante el Death Train no hay regeneracion natural de vida en ningun mundo.
- Los ravagers de las raids ya no sueltan totem.
- Ganar una raid da Hero of the Village solo 5 minutos.
- Romper un bloque quita medio corazon, salvo con herramientas de netherite (las de The Beginning).
- Fundir hierro u oro da pepitas en vez de lingotes.
- Te ahogas 5 veces mas rapido.
- Estar sobre arena de almas da Slowness II.
- Los bloques de magma y el dano de los Sulfur Cube pasan a x8 (el doble que el dia 40).
- Los **Dried Ghast** se convierten en spawners trampa de ghast: se activan con un jugador a 8 bloques y sacan 1 ghast cada 10 minutos.
- Los **Happy Ghast** pasan a ser Ghast (tambien los domados; el arnes se suelta).
- Mucha mas ceguera bajo la lluvia.
- Los mobs hostiles tienen Resistencia al Fuego.
- Las arañas de cueva ya no pueden tener Glowing.
- Los Creepers Electricos pasan a ser **Quantum Creepers** (explosion mas grande y x5 de dano) y el 20% son **Ender Creepers**.
- No se puede recoger lava ni agua con un cubo (los cubos llenos que ya tengas siguen sirviendo).
- De noche, en el Overworld, a veces te da Levitacion de 3 a 20 segundos.
- En el Nether llueven pigmans: de vez en cuando caen de 1 a 3 en tres puntos sobre tu cabeza.
- **Camello de Guerra**: todo camello lleva un **Husk Lancero** (lanza de hierro) y un esqueleto de clase.
- **Giga Phantom**: phantom el doble de grande con 3 efectos al azar (sigue llevando su esqueleto). 1 de cada 100 viene con 4 **Ender Ghasts**.
- Los Gatos Supernova pasan a ser **Gatos Galacticos**: ya no explotan, pero al morir, tras una cuenta atras de 5 s, invocan un mob
  del plugin al azar (incluido un Gato Supernova, un Ultra Ravager, un Gigante, un Wither Skeleton Emperador o el **Permadeath Demon**, un dragon suelto de 1350 de vida que rompe bloques).
- Los pollos pasan a ser Silverfish (en vez de ravagers) y los salmones, **Pufferfish Invulnerables** (como mucho 2 juntos).
- El 1% de los pillagers sale como Evoker.
- Iron Golems con Resistencia II, Vex con Resistencia III y Blazes con 100 corazones.
- Los Drowned llevan tridente y los Vindicators hacha de diamante con Filo V.
- Las abejas hacen 15 corazones de dano.
- Los Shulkers explosivos son invulnerables.
- Los Giga Slimes y Giga Magma Cubes tienen el doble de vida.
- Las aranas de cueva dan Veneno III y Nausea; los escupitajos de llama, Veneno III 30 s, Nausea 10 s y empujan fuerte.
- La Mining Fatigue dura el doble y la leche no la quita.
- Intentar dormir solo tiene un 10% de reiniciar el contador de Phantoms.
- La tarta de calabaza da 5 s de Saturacion. El ojo de arana, la carne podrida, la patata venenosa y el pez globo dejan su efecto para siempre.
- Todos los ravagers del Overworld (los de raids y los que salen de animales de granja) son **Ultra Ravagers**: 250 corazones, Speed II y Fuerza II, sin jinetes. No sueltan nada.
- El 20% de los pigmans salen montados (antes 5%). Los drops del Ultra Ravager, Carlos y Jess bajan al 33%.
- Los osos polares, en vez de pegarte, explotan.
- **Bacalao de la Muerte**: todo bacalao lleva una espada de madera con Filo L y Empuje C.
- Esqueletos de clase mas fuertes: Guerrero con 50 corazones; Infernal con hacha de diamante Fire Aspect XX y Filo XXV; Asesino con ballesta Filo L; Tactico con arco Punch L y Power XL; Pesadilla con arco Power LX.
- **Wither Skeleton Emperador**: 1 de cada 50 wither skeletons del Nether. 40 corazones, estandarte y armadura de oro, arco con Power C y Punch V. Las flechas no le hacen dano. 50% de soltar una espada de netherite irrompible.
- **Strider Jinete**: todo strider adulto lleva un Piglin Brute encima.
- **Gigantes** en los Plains (1 de cada 500 zombies): 300 corazones, golpes que quitan muchisima vida y sueltan el **Arco de Gigante** (Power X).

## Dia 60
- Los totems gastan 3 y tienen 7% de probabilidad de fallar.
- Ya no se pueden conseguir totems de ninguna forma (ni mobs, ni cofres, ni The Beginning).
- Los cofres de The Beginning ya no traen loot.
- Se pueden craftear las **Elytras de Netherite Infernal**: elytra rodeada de 8 Bloques de Netherite Infernal (conserva sus encantamientos). Cuentan como pechera para el bonus del set de Netherite Infernal.
- Cada 60 minutos jugados en supervivencia te sale un **Wither** encima.
- Los Iron Golems tienen Fuerza IV, Velocidad IV, Resistencia IV y Resistencia al Fuego.
- Salen **Ender Quantum Creepers** a tu alrededor en cualquier bloque y con cualquier luz: explosion enorme (un poco menos que un Gato Supernova).
- Los creepers explotan el doble de rapido.
- Se puede comer una segunda Hyper Golden Apple + (otros +2 corazones).
- La tarta de calabaza es la peor comida del juego: da Dano Instantaneo IV.
- El 50% de los Vindicators (no los que salen de zombies) son **Evokers Acorazados** con Resistencia III.
- Gigantes y Wither Skeleton Emperadores son 4 veces mas comunes, pero no sueltan nada.
- Te ahogas 10 veces mas rapido.
- Todos pierden otros 4 corazones de vida maxima (8 menos en total).

## Logros

Salen en el menu de logros (tecla L), pestana **HM**.
- **¿Quién necesita ver?**: mata a un mob hostil mientras estas ciego por un murcielago (dia 20+).
- **Ahorro de Espacio**: sobrevive 2 dias seguidos desde el dia 40 sin tener la Reliquia del Fin.
- **No es mi Día**: ten Hambre, Debilidad y Ceguera a la vez.
- **Duro como la piedra**: sobrevive a 3 explosiones en menos de 1 minuto.
- **Yokoso Watashi no Soul Society**: se el primero en entrar a The Beginning.

## Eventos

- **Tragamonedas de eventos**: una tragamonedas arriba en el centro de la pantalla con las cabezas de los jugadores que tienen evento propio y signos de pregunta. Si cae en una cabeza sale el evento de ese jugador; si cae en "?", un evento del server al azar, con un cuadro en el chat que explica que hay que hacer. Por ahora la gira un OP con `/hm evento` y los eventos solo se anuncian.

## Comandos

- `/hm` (alias `/permadeath`): `items`, `day`, `awake`, `mensaje`, `lives`, `resetlives`, `addlife`, `reducestorm`, `resetdifficulty`, `resethealth`, `netheriteArmor`, `finalmsg`, `beginning [tp]` (OP: donde esta el portal a The Beginning y si esta abierto; con `tp` te lleva), `evento` (OP: gira la tragamonedas de eventos), `infernal` (OP: herramientas de netherite irrompibles y armadura de Netherite Infernal completa).
- `/hmreload` (alias `/hmr`), permiso `hmplugin.reload`.
