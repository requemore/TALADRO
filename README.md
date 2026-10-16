# Taladro Autónomo 3×3 — v0.2.0

**Minecraft Java 1.21.11 · NeoForge 21.11.45 · Java 21**

> **ESTADO: CÓDIGO FUENTE EXPERIMENTAL.** Los archivos Java/JSON fueron revisados, pero el proyecto **no se ha compilado contra las librerías del juego ni se ha probado en Minecraft**. No es todavía un `.jar` listo para jugar. Crea copias de seguridad y úsalo inicialmente en un mundo de prueba.

## Características

- Cabezal de taladro que se coloca como un bloque y avanza en línea recta **1 bloque por segundo**.
- Excava automáticamente una sección de **3 bloques de ancho × 3 bloques de alto (9 bloques por paso)**, centrada en el cabezal. Puede requerir despejar un bloque lateral adicional para desplazar el segundo cofre.
- Los drops de minería van directamente a **dos cofres normales de Minecraft conectados como cofre doble**, con **54 espacios**, que avanzan con la máquina.
- Si los drops ya no caben, **no mina** ese paso y sigue a la espera de que vacíes el cofre.
- Consume **1 unidad de energía por bloque de avance**. Un carbón o carbón vegetal = **16 avances**; un cubo de lava = **200 avances**, y devuelve el cubo vacío. Combustible almacenado en la cabeza; se conserva al desplazar el taladro y al guardar/cargar el mundo.
- No rompe bedrock, bloques irrompibles, líquidos, bloques con inventario u otros bloques con entidades de bloque. No entra en chunks descargados.

## Fabricación

Puedes fabricar **el taladro** en una mesa de crafteo 3×3 de la siguiente forma:

| | | |
|---|---|---|
| Lingote de hierro | Pico de diamante | Lingote de hierro |
| Redstone | Horno | Redstone |
| Lingote de hierro | Redstone | Lingote de hierro |

Resultado: **1 Taladro autónomo 3×3**.

También fabrica **2 cofres normales** usando las recetas originales de Minecraft. Los cofres se colocan detrás del taladro: **esta versión fabrica y monta los 3 componentes por separado**, no despliega todo con un único clic. Por eso no crea cofres de la nada ni permite duplicarlos al retirar el taladro.

## Colocación: vista superior

Ejemplo con el taladro apuntando al **norte** (↑):

```text
        NORTE: avance ↑

      ← 3 bloques →
        [    ]
        [ D  ]   <-- taladro colocado a la altura CENTRAL del futuro túnel
        [    ]
          [A][B] <-- cofre doble unido detrás (A al sur de D; B al este de A)

    SUR
```

Posiciones en 3D con norte como avance:
- D = cabeza (x, y, z).
- A = cofre atrás de D (x, y, z + 1).
- B = cofre a la derecha de A (x + 1, y, z + 1).
- Para que los cofres sean reconocidos, **A y B deben ser mitades de un cofre doble vanilla, con la abertura hacia el norte**, igual que el taladro: A es la mitad `left` y B la mitad `right`.
- Conviene colocar los cofres primero y la cabeza delante. Comprueba que ves un cofre doble real al abrirlo.
- Los cofres deben estar a la **misma altura** que la cabeza del taladro (mitad de la sección 3×3).

El taladro no excava su propio bloque inicial: al moverse, vacía un plano 3×3 delante. Haz espacio detrás y a los lados del taladro antes de encenderlo. Si algo bloquea el camino o el depósito se llena, se queda en su lugar.

## Cómo usarlo

1. Construye el taladro y los 2 cofres, y móntalos como se muestra arriba.
2. Sostén carbón, carbón vegetal o un **cubo de lava** y **haz clic derecho sobre el taladro** para cargar combustible. Cada clic agrega una unidad de combustible; con lava devuelve un cubo vacío (en supervivencia).
3. Con la mano principal vacía, haz clic derecho para **encender o apagar**.
4. Agáchate y haz clic derecho con la mano vacía para ver la energía restante en avances.
5. Los objetos extraídos quedan en el cofre doble móvil. Vacíalo cuando quieras, idealmente después de apagar la máquina.
6. Si se queda sin combustible, no avanza; sigue activado y puedes cargarlo de nuevo para que reanude.

**Nota:** Si usas `/give @s taladro_auto:taladro` obtienes el bloque del taladro sin necesitar craftearlo. En creativo también aparece en la pestaña de redstone.

## Compilación en Windows

1. Instala **JDK 21** y comprueba `java -version`.
2. Descarga el [kit oficial para NeoForge 1.21.11 con ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle) mediante **Code → Download ZIP**.
3. De ese kit copia a esta carpeta `gradlew`, `gradlew.bat` y **la carpeta `gradle/wrapper`**, manteniendo intactos `build.gradle`, `settings.gradle`, `gradle.properties` y los archivos `src` de este proyecto.
4. Abre CMD en la raíz del proyecto y ejecuta:

   ```bat
   gradlew.bat build
   ```

5. Si compila, encontrarás `build/libs/taladro_auto-0.2.0.jar` (el nombre exacto depende de Gradle). Copia ese `.jar` a `.minecraft/mods` en una instalación de **NeoForge para Minecraft 1.21.11**.
6. Usa un **mundo de prueba** primero. Si la compilación informa un error de API, conserva la salida completa para poder corregirlo.

El proyecto no incluye el wrapper oficial ni librerías de Minecraft en el ZIP para evitar redistribuir paquetes que no se han podido descargar ni verificar en este entorno.

## Limitaciones del prototipo

- No hay barra de combustible ni GUI; se carga usando clic derecho.
- El movimiento es instantáneo (por bloques), sin animación de ruedas ni brocas; el aspecto usa texturas vanilla.
- El cofre doble no debe permanecer abierto cuando la máquina se mueve (la interfaz aún no se cierra automáticamente); apagar antes de abrir evita problemas.
- Si se rompe uno de los dos cofres, la máquina no puede continuar hasta que estén nuevamente conectados.
- No recupera experiencia de los minerales; recoge los objetos que `Block.getDrops` devuelve con herramientas adecuadas.
- No soporta bloqueos de protección de otros mods, servidores con plugins o regiones protegidas: las comprobaciones de permisos aún no están integradas.
- No se han probado transiciones entre chunks, guardado/carga durante movimiento, caídas de arena/grava ni roturas mientras se mueven cofres. No usarlo aún en mundos importantes.

## Archivos principales

- `src/main/java/com/ejemplo/taladroauto/TaladroAutoMod.java`: registros NeoForge.
- `src/main/java/com/ejemplo/taladroauto/TaladroBlock.java`: perforación 3×3, control, combustible, traslado de cofres.
- `src/main/java/com/ejemplo/taladroauto/TaladroBlockEntity.java`: energía persistente.
- `src/main/resources/data/taladro_auto/recipe/taladro.json`: receta de fabricación 3×3.
- `src/main/resources/assets/taladro_auto/`: texturas/modelos y nombres.
