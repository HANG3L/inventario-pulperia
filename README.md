# Mi Pulpería — Control de inventario

Aplicación Android para gestionar el inventario de una pulpería: registrar productos con su precio y stock, controlar entradas y salidas, ver el valor total del inventario y recibir alertas cuando un producto tiene poco stock.

| | |
|---|---|
| **Estudiante** | Angel Manuel Rivera |
| **Número de cuenta** | 202330010471 |
| **Clase** | Programación Móvil 1 |
| **Lenguaje** | Kotlin |
| **Base de datos** | SQLite (local, `SQLiteOpenHelper`) |
| **Arquitectura** | MVC (Modelo – Vista – Controlador) |
| **Moneda** | Lempira (L) |

---

## 1. Funcionamiento

La app tiene tres pantallas, a las que se llega con la barra de navegación inferior.

### 1.1 Resumen
- Tarjeta principal con el **valor total del inventario**, calculado como la suma de `precio × stock` de todos los productos.
- Cantidad de productos registrados y de unidades en existencia.
- **Alerta de stock bajo:** si hay productos cuyo stock es menor o igual a su stock mínimo, aparece una tarjeta roja con la lista de esos productos.
- **Top 5 por valor:** los cinco productos con mayor `precio × stock`, con una barra que compara cada uno contra el primero.

### 1.2 Productos (CRUD)
- **Crear:** botón «Nuevo producto» abre un formulario con nombre, precio, stock, stock mínimo y código de barras (opcional).
- **Leer:** lista de productos ordenada por nombre. Cada tarjeta muestra precio, código de barras y stock. El indicador de stock se pone **rojo** cuando el producto está en el mínimo o por debajo.
- **Buscar:** el campo superior filtra por nombre o por código de barras.
- **Actualizar:** al tocar un producto se abre el mismo formulario con sus datos.
- **Eliminar:** botón dentro del formulario de edición, con confirmación. Al borrar un producto también se borra su historial de movimientos.

### 1.3 Movimientos
- Historial de entradas y salidas, del más reciente al más antiguo, con filtros **Todos / Entradas / Salidas**.
- El botón «Registrar» abre un formulario donde se elige **Entrada** o **Salida**, el producto y la cantidad.
- Se puede **escribir el código de barras** y el producto se selecciona automáticamente.
- Una **entrada** suma al stock; una **salida** resta. Si se intenta sacar más de lo disponible, la app lo rechaza e indica cuántas unidades hay.
- Si una salida deja el producto en su stock mínimo o menos, aparece un **aviso de stock bajo**.

### 1.4 Reto adicional
| Requisito | Cómo se cumple |
|---|---|
| Registrar códigos de barras manualmente | Campo «Código de barras» en el formulario de producto. Es único: no permite repetir un código y dice a qué producto ya pertenece. |
| Alerta de stock mínimo | Cada producto tiene su propio `min_stock` (5 por defecto). Se alerta con la tarjeta del Resumen, el indicador rojo en la lista y un diálogo después de una salida que llegue al mínimo. |

---

## 2. Base de datos

SQLite, creada en `BaseDatos.kt`. Las tablas siguen el enunciado y se añadieron dos columnas para el reto.

**`products`**

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | INTEGER, PK autoincremental | Identificador |
| `name` | TEXT, no nulo | Nombre del producto |
| `price` | REAL, `>= 0` | Precio unitario |
| `stock` | INTEGER, `>= 0` | Unidades disponibles |
| `barcode` | TEXT, `UNIQUE` | Código de barras (opcional) *— reto* |
| `min_stock` | INTEGER | Stock mínimo para alertar *— reto* |

**`movements`**

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | INTEGER, PK autoincremental | Identificador |
| `product_id` | INTEGER, FK → `products.id` `ON DELETE CASCADE` | Producto afectado |
| `qty` | INTEGER, `> 0` | Cantidad movida |
| `type` | TEXT (`IN` / `OUT`) | Entrada o salida |
| `date` | INTEGER | Fecha y hora en milisegundos |

Decisiones importantes:
- **Transacción:** al registrar un movimiento, la actualización del stock y la inserción del movimiento se hacen juntas (`beginTransaction` … `setTransactionSuccessful`). Si algo falla, no se guarda ninguna de las dos y el stock nunca queda inconsistente.
- **Restricciones en la base:** `CHECK` para que el precio, el stock y la cantidad no sean negativos, y llaves foráneas activadas (`setForeignKeyConstraintsEnabled`).
- **Cálculos agregados en SQL:** el valor total (`SUM(price * stock)`), el conteo de productos con stock bajo y el top 5 (`ORDER BY price * stock DESC LIMIT 5`) se resuelven con consultas directas.

---

## 3. Arquitectura MVC

```
com.example.inventario
├── modelo/        Producto, Movimiento, TipoMovimiento, Totales
├── datos/         BaseDatos (SQLite)
├── controlador/   ProductoControlador, MovimientoControlador, ResumenControlador
├── vista/         MainActivity, ResumenFragment, ProductosFragment, MovimientosFragment,
│                  ProductoSheet, MovimientoSheet
└── util/          Formato (moneda y fechas)

res/layout/        Layouts XML de todas las pantallas
```

| Capa | Responsabilidad | Dónde |
|---|---|---|
| **Modelo** | Datos del negocio y acceso a SQLite. Define qué es un producto, un movimiento y cómo se guardan. | `modelo/`, `datos/` |
| **Vista** | Mostrar información y capturar la interacción del usuario. No tiene SQL ni reglas de inventario. | `vista/`, `res/layout/` |
| **Controlador** | Validar lo que escribe el usuario y aplicar las reglas del inventario. Devuelve resultados; la vista decide qué mensaje mostrar. | `controlador/` |

Ejemplo del flujo al registrar una salida:
1. La vista (`MovimientoSheet`) lee el producto, la cantidad y el tipo, y llama a `MovimientoControlador.registrar(...)`.
2. El controlador valida la cantidad y pide al modelo (`BaseDatos.registrarMovimiento`) aplicar el cambio.
3. El modelo comprueba que haya stock suficiente, actualiza el stock y guarda el movimiento en una transacción.
4. El controlador devuelve un resultado (`Exito`, `SinStock`, `Invalido`). Si fue éxito y el stock quedó bajo, indica que hay que alertar.
5. La vista muestra el error en el campo, o cierra el formulario y avisa a la pantalla de Movimientos para que se refresque y muestre la alerta.

---

## 4. Cómo se hizo

**Tecnologías**
- Kotlin con interfaz en **XML** y **ViewBinding** (acceso a las vistas sin `findViewById`).
- **Material Design 3** (componentes de `com.google.android.material`): tarjetas, campos de texto, chips, botones flotantes, hojas inferiores y barra de navegación.
- `RecyclerView` con `ListAdapter` y `DiffUtil` para las listas.
- `SQLiteOpenHelper` directamente, sin librerías adicionales, para practicar SQLite como pide la clase.

**Navegación**
Una sola `MainActivity` contiene la barra inferior y un contenedor de fragments. Cada pestaña es un `Fragment`. Los formularios son `BottomSheetDialogFragment` que se abren desde abajo y avisan a la pantalla que los abrió mediante `setFragmentResult`, para que recargue su lista.

**Diseño**
- Paleta verde y ámbar pensada para un negocio de abarrotes, con soporte automático de **modo oscuro** (`values-night/colors.xml`).
- Los colores se definen una sola vez como recursos y el tema los asigna a los roles de Material 3, así todas las pantallas son consistentes.
- Verde para entradas y naranja para salidas; rojo reservado para alertas de stock bajo.
- Soporte de pantalla completa (edge-to-edge) respetando las barras del sistema.

**Convenciones de código**
- Variables, funciones y clases propias en **español**, con nombres descriptivos y cortos, sin la letra ñ (ver `CLAUDE.md`).
- Los textos visibles están en `strings.xml`, no escritos dentro del código.
- La moneda se cambia en un solo lugar: `Formato.MONEDA`.

---

## 5. Cómo ejecutarla

**Requisitos:** Android Studio, JDK 17 o superior y un teléfono Android 7.0 (API 24) o superior.

1. Abrir la carpeta del proyecto en Android Studio y esperar a que termine la sincronización de Gradle.
2. En el teléfono, activar *Opciones de desarrollador* y *Depuración USB*, y conectarlo por cable.
3. Elegir el teléfono en la barra superior y pulsar **Run ▶**.

También se puede compilar desde la terminal con `gradlew assembleDebug`. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## 6. Prueba rápida sugerida

1. En **Productos**, crear «Arroz 1 lb» con precio 25, stock 10 y mínimo 5.
2. En **Movimientos**, registrar una **salida** de 6 unidades: el stock baja a 4 y aparece la alerta de stock bajo.
3. Intentar una salida de 50: la app la rechaza indicando que solo hay 4 unidades.
4. Registrar una **entrada** de 20: el stock sube a 24 y la alerta desaparece.
5. En **Resumen**, comprobar que el valor total es `24 × 25 = L 600.00` y que el producto aparece en el top 5.
