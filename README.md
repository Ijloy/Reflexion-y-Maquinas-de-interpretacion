<<<<<<< HEAD
# Catálogo de Productos — Caché, Proxy y CORS con Spring Boot

Actividad de la materia **ISWZ2202 – Diseño y Arquitectura de Software** (UDLA).

API REST de un catálogo de productos construida con Spring Boot. El repositorio de datos simula una latencia de **1.5 s** por consulta (como si fuera una base de datos o un servicio remoto lento). El objetivo de la actividad es reducir esa latencia en pedidos repetidos aplicando **caché**, interponer un **patrón Proxy** delante del repositorio y conectar un **frontend** resolviendo el problema de **CORS** en desarrollo.

## Resultado

Pedir dos veces seguidas el mismo producto:

```
GET /api/productos/1   →  1.72 s    (va al repositorio)
GET /api/productos/1   →  0.008 s   (sale de la caché)
```

La segunda petición es unas **200 veces más rápida**.

## Tecnologías

- Java 17 (Eclipse Temurin)
- Spring Boot 3.3.4 (Spring Web, Spring Cache, DevTools)
- Maven (wrapper incluido: `mvnw` / `mvnw.cmd`)
- HTML, CSS y JavaScript (frontend)

## Arquitectura

```
Frontend (HTML/JS)
      │  HTTP (CORS habilitado)
      ▼
ProductoController          GET /api/productos, GET /api/productos/{id}
      │
      ▼
[Proxy de caché de Spring]  @Cacheable → si el dato ya está, responde al instante
      │
      ▼
ProductoService             lógica de negocio, depende de la interfaz ProductoRepository
      │
      ▼
ProductoRepositoryProxy     patrón Proxy: registra cada acceso y su duración
      │
      ▼
RepositorioProductoEnMemoria   repositorio real (latencia simulada de 1.5 s)
```

### Estructura del proyecto

```
src/main/java/com/udla/arquitectura/demo/
├── DemoApplication.java                  # punto de entrada, @EnableCaching
├── config/                               # configuración CORS (WebMvcConfigurer)
├── controller/
│   └── ProductoController.java
├── model/
│   └── Producto.java
├── repository/
│   ├── ProductoRepository.java           # interfaz (abstracción)
│   ├── RepositorioProductoEnMemoria.java # implementación real, lenta
│   └── ProductoRepositoryProxy.java      # Proxy (@Primary)
└── service/
    └── ProductoService.java              # @Cacheable
```

## Decisiones de diseño

### 1. Caché (`@EnableCaching` + `@Cacheable`)

La caché se activa en `DemoApplication` y se aplica en la capa de servicio:

```java
@Cacheable("productos")
public List<Producto> listarTodos() { ... }

@Cacheable(value = "producto", key = "#id")
public Producto buscarPorId(Long id) { ... }
```

- Se usan dos cachés separadas: una para el listado completo y otra para cada producto por `id`.
- Se aplica en el **servicio** y no en el controlador ni en el repositorio, porque es ahí donde vive la lógica de negocio y el repositorio sigue siendo una fuente de datos "pura".
- El controlador no sabe que existe la caché: es transparente para quien consume el servicio.

### 2. Patrón Proxy (`ProductoRepositoryProxy`)

El proxy **implementa la misma interfaz `ProductoRepository`** que el repositorio real, se coloca delante de él y delega cada llamada, añadiendo el registro (log) del acceso y del tiempo que tardó.

- Se marca con `@Primary` para que Spring lo inyecte en lugar del repositorio real.
- Recibe el repositorio real mediante `@Qualifier`.
- `ProductoService` **no se modificó**: depende de la interfaz, no de la clase concreta. Esto demuestra **bajo acoplamiento** y el uso de **abstracciones** (principio de inversión de dependencias).

Con los logs del proxy se puede comprobar la caché: en un pedido repetido, el proxy **no registra ningún acceso**, porque la petición nunca llega al repositorio.

### 3. CORS

El frontend se ejecuta en un origen distinto al de la API (`localhost:8080`), así que el navegador bloquea las peticiones por la **política de mismo origen**. Se resolvió con una configuración global mediante un bean `WebMvcConfigurer` que habilita CORS para las rutas `/api/**` en desarrollo.

> En producción conviene restringir los orígenes permitidos a los dominios reales del frontend.

### 4. Frontend

Página HTML/JS que muestra el catálogo en tarjetas e incluye un panel que visualiza la latencia de cada petición: **verde** cuando la respuesta sale de la caché y **naranja** cuando llega hasta el repositorio.

## Cómo ejecutarlo

**Requisitos:** JDK 17 o superior.

```bash
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

La API queda disponible cuando aparece `Tomcat started on port 8080`.

Para el frontend, abre el archivo HTML en el navegador con la API en ejecución.

## Endpoints

| Método | Ruta                  | Descripción                  |
|--------|-----------------------|------------------------------|
| GET    | `/api/productos`      | Lista todos los productos    |
| GET    | `/api/productos/{id}` | Devuelve un producto por id  |

### Probar la caché

```powershell
curl.exe -w "\nTiempo: %{time_total}s\n" http://localhost:8080/api/productos/1
curl.exe -w "\nTiempo: %{time_total}s\n" http://localhost:8080/api/productos/1
```

La primera llamada tarda ~1.5 s; la segunda, unos milisegundos.

## Figuras dinámicas — Reflexión y Groovy en tiempo de ejecución

Segunda actividad sobre la misma app, en el paquete independiente `com.udla.arquitectura.demo.figuras`.
Diseño, endpoints y comandos de demostración en **[README-figuras.md](README-figuras.md)**.
Página de prueba: http://localhost:8080/figuras.html

## Autor

**Dereck** — Ingeniería de Software, Universidad de las Américas (UDLA), Quito.
GitHub: [@Ijloy](https://github.com/Ijloy)
=======
# Reflexion-y-Maquinas-de-interpretacion
>>>>>>> 3c3577c68f0b726203efe273e15463fe4ac1bf31
