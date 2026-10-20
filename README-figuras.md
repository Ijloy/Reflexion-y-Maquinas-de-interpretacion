# Figuras dinámicas — Reflexión y ejecución de código en tiempo de ejecución

Actividad de **ISWZ2202 – Diseño y Arquitectura de Software** (UDLA).
Se crea cualquier figura **a partir de su nombre y sus parámetros** —`("Circulo", radio=5)`— sin
conocer la clase al compilar, y se calculan su área y su perímetro. Las figuras pueden ser
clases Java de la aplicación, código Groovy enviado mientras la app corre, o código descargado
de un catálogo externo. Para la fábrica, las tres son exactamente lo mismo.

Todo vive en `com.udla.arquitectura.demo.figuras`. El catálogo de productos (caché, Proxy, CORS)
no se tocó; el único cambio compartido es que CORS ahora también permite `POST` en `/api/**`.

## Cómo ejecutarlo

**Requisito:** JDK 17 o superior (no hace falta instalar Maven: el proyecto trae el wrapper).

```powershell
# 1. Desde la carpeta del proyecto, arrancar la aplicación
.\mvnw.cmd spring-boot:run          # macOS / Linux: ./mvnw spring-boot:run

# 2. Esperar el mensaje "Tomcat started on port 8080" y abrir en el navegador:
#    http://localhost:8080/figuras.html

# 3. Para detenerla: Ctrl+C en esa terminal
```

En la página se elige la figura, se escriben sus medidas y se obtienen el área y el perímetro;
también se puede compilar el Pentágono en Groovy e importar Hexágono o Elipse del catálogo externo.
Las figuras registradas viven en memoria: al reiniciar la app solo quedan las nativas.

Para ejecutar las pruebas: `.\mvnw.cmd test`.

## Arquitectura

```
                       HTTP
                        │
          ┌─────────────▼─────────────┐
   api    │ FiguraController          │  traduce HTTP ⇄ casos de uso
          │ ManejadorErrores          │  excepciones → 404 / 400 / 422 / 502
          └─────────────┬─────────────┘
          ┌─────────────▼─────────────┐
 servicio │ ServicioFiguras           │  orquesta: calcular, registrar, importar
          └──┬──────────┬──────────┬──┘
             │          │          │
  reflexion  │          │          │  dinamico
 ┌───────────▼──┐  ┌────▼───────┐  │  ┌──────────────────────┐     ┌─────────────────────────┐
 │FabricaFiguras│─►│Registro    │◄─┼──│CompiladorGroovy      │     │ClienteCatalogoExterno   │
 │ getConstruc- │  │Figuras     │  └─►│ GroovyClassLoader +  │     │ RestClient → GET        │
 │ tors, Param. │  │ nombre →   │     │ SecureASTCustomizer  │     │ {figuras.catalogo.url}/ │
 │ getName,     │  │ Class      │     └──────────────────────┘     │ {nombre}  (text/plain)  │
 │ newInstance  │  │(forName o  │                                  └────────────┬────────────┘
 └──────┬───────┘  │ dinámicas) │                                               │ HTTP
        │          └────────────┘                                  ┌────────────▼────────────┐
        ▼                                                          │ simulador/              │
 dominio:  «interface» Figura  ◄── Circulo, Rectangulo,            │ CatalogoExternoController│
           area(), perimetro(),    Cuadrado, Triangulo,            │ /catalogo-externo/{n}   │
           nombre()                Pentagono*, Hexagono*, Elipse*  └─────────────────────────┘
                                   (* compiladas desde Groovy)
```

### Qué hace cada clase y por qué

| Clase | Responsabilidad (una sola) | Por qué así |
|---|---|---|
| `dominio.Figura` | Contrato: `area()`, `perimetro()`, `nombre()` por defecto y la validación `Figura.positivo(...)` | Es la **única abstracción** que conoce el resto del sistema. Nada depende de `Circulo`; todo depende de `Figura` (inversión de dependencias). |
| `Circulo`, `Rectangulo`, `Cuadrado`, `Triangulo` | Geometría y validación de sus propios datos (`> 0`, desigualdad triangular) | Un objeto inválido no se puede construir: la regla vive junto a los datos que protege. |
| `reflexion.NombreFigura` | Normaliza el nombre (`"Pentágono"` → `Pentagono`) y rechaza lo que no sea un identificador simple | Evita que llegue a `Class.forName` algo como `java.lang.Runtime`. |
| `reflexion.RegistroFiguras` | Traduce un nombre a una `Class<? extends Figura>`: primero las dinámicas, luego `Class.forName("…dominio." + nombre)` | Aquí es donde las figuras dinámicas se unen a las nativas. Descubre las nativas escaneando el paquete, **sin una lista escrita a mano**: agregar `Rombo.java` no exige tocar nada más (abierto/cerrado). |
| `reflexion.FabricaFiguras` | Crea la instancia **solo con reflexión**: `getConstructors()` → elige el constructor cuyos `Parameter.getName()` coinciden con las claves del `Map<String, Double>` → `newInstance(args)` | No hay ni un `new Circulo(...)`. Los nombres de los parámetros existen en tiempo de ejecución porque el parent de Spring Boot compila con `-parameters` (y Groovy con `setParameters(true)`). |
| `dinamico.CompiladorGroovy` | Convierte texto Groovy en una clase JVM (`GroovyClassLoader.parseClass`) y verifica que implemente `Figura` | Solo compila: no registra ni instancia. Así puede probarse aislado (`CompiladorGroovyTest`). |
| `dinamico.ClienteCatalogoExterno` | Descarga el código Groovy por HTTP con `RestClient` | Solo transporta texto; no sabe compilar. La URL es configuración, no código. |
| `servicio.ServicioFiguras` | Orquesta los casos de uso combinando las piezas anteriores | Contiene el *flujo* (si la figura no existe → importar → compilar → registrar → fabricar), no los detalles técnicos. |
| `api.FiguraController` | Adapta HTTP a llamadas al servicio | Sin lógica de negocio. |
| `api.ManejadorErrores` | `@RestControllerAdvice` que traduce excepciones a `ProblemDetail` (RFC 7807) | Los controladores no tienen `try/catch`; cada excepción del módulo tiene un único código HTTP. |
| `simulador.CatalogoExternoController` | **Simula** el servicio externo: publica `Hexagono` y `Elipse` | La app lo consume por HTTP, igual que a un servidor remoto. |

### Por qué una figura Groovy es "igual que una nativa"

`CompiladorGroovy` produce un `Class` real de la JVM. `RegistroFiguras` lo guarda y, a partir de ahí,
`FabricaFiguras` hace exactamente lo mismo que con `Circulo`: `getConstructors()`,
`Parameter.getName()` y `newInstance(...)`. No hay ningún `if (esGroovy)` en la fábrica ni en el
servicio; lo único que cambia es el campo informativo `origen` (`NATIVA` / `DINAMICA` / `EXTERNA`).

### Seguridad del código Groovy

`CompiladorGroovy` usa `CompilerConfiguration` + `SecureASTCustomizer` para rechazar en tiempo de
compilación (→ **422**) el uso de `System`, `Runtime`, `ProcessBuilder`, `Process`, `Thread`,
`File`, `Files`, `Path`, `URL`, `Socket`, `ClassLoader`, `Class`, `GroovyShell`, `Eval`, los paquetes
`java.io`, `java.nio`, `java.net` y `java.lang.reflect`, y métodos o propiedades como `exit`, `execute`,
`forName`, `getClassLoader`, `.class` y `.metaClass`.

> **Hallazgo:** en Groovy 4, `SecureASTCustomizer` solo revisa el cuerpo del *script*, **no el interior
> de las clases**. Con la configuración "de manual", `class X implements Figura { X(double l) { System.exit(1) } }`
> compilaba sin errores. Por eso `CompiladorGroovy` usa una subclase (`SecureASTCustomizerDeClases`) que
> aplica el mismo visitor a constructores, métodos e inicializadores. `CompiladorGroovyTest` cubre 10 intentos de escape.

Aun así, esto **no es un sandbox completo**: compilar código recibido por la red equivale a ejecución
remota de código. En un sistema real habría que autenticar quién registra figuras, firmar las
definiciones del catálogo externo y ejecutar el código en un proceso aislado.

## Catálogo externo (extra)

Si se pide una figura desconocida, `ServicioFiguras` la busca en el catálogo externo, compila el
código descargado y la registra con origen `EXTERNA`. También puede importarse a mano con
`POST /api/figuras/importar/{nombre}`.

Contrato: `GET {figuras.catalogo.url}/{nombre}` → `200 text/plain` con el código Groovy, o `404`.

```properties
# application.properties — apunta al simulador incluido en esta misma app
figuras.catalogo.url=http://localhost:${local.server.port:${server.port}}/catalogo-externo
figuras.catalogo.importar-automaticamente=true
```

**Para usar un servicio real basta con cambiar `figuras.catalogo.url`** por la URL de cualquier
servidor que cumpla ese contrato; no hay que tocar código.

## Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/figuras` | Figuras disponibles: `nombre`, `origen`, `parametros` |
| GET | `/api/figuras/{nombre}` | Descripción de una figura |
| POST | `/api/figuras/calcular` | Body `{"figura":"Circulo","parametros":{"radio":5}}` |
| GET | `/api/figuras/{nombre}/calcular?radio=5` | Lo mismo por query string |
| POST | `/api/figuras/definiciones` | Body `text/plain` con código Groovy → **201** |
| POST | `/api/figuras/importar/{nombre}` | Importa del catálogo externo → **201** |
| GET | `/catalogo-externo` · `/catalogo-externo/{nombre}` | Simulador del servicio externo |

Respuesta de un cálculo:

```json
{ "figura": "Circulo", "origen": "NATIVA", "parametros": { "radio": 5.0 },
  "area": 78.53981633974483, "perimetro": 31.41592653589793 }
```

Errores (`application/problem+json`):

| Código | Cuándo |
|---|---|
| 404 | La figura no existe (ni localmente ni en el catálogo externo) |
| 400 | Faltan parámetros, sobran, no son numéricos, son `<= 0` o no cumplen la desigualdad triangular |
| 422 | El código Groovy no compila, no implementa `Figura`, usa llamadas prohibidas o intenta redefinir una nativa |
| 502 | El catálogo externo no responde |

## Demo en PowerShell

Con la app en marcha (ver [Cómo ejecutarlo](#cómo-ejecutarlo)), en otra terminal:

```powershell
$api = 'http://localhost:8080/api/figuras'

# 1. Figuras disponibles (solo las nativas al inicio)
Invoke-RestMethod $api | Format-Table

# 2. Figura nativa creada por reflexión
Invoke-RestMethod -Method Post -Uri "$api/calcular" -ContentType 'application/json' `
  -Body '{"figura":"Circulo","parametros":{"radio":5}}'
# figura=Circulo  origen=NATIVA  area=78.5398...  perimetro=31.4159...

Invoke-RestMethod "$api/Triangulo/calcular?a=3&b=4&c=5"   # area=6, perimetro=12

# 3. Pentagono todavía no existe -> 404
try { Invoke-RestMethod "$api/Pentagono/calcular?lado=2" } catch { $_.Exception.Response.StatusCode }

# 4. Se compila desde Groovy en caliente -> 201
Invoke-RestMethod -Method Post -Uri "$api/definiciones" -ContentType 'text/plain; charset=utf-8' `
  -InFile .\ejemplos\Pentagono.groovy

# 5. Ahora funciona igual que una nativa (y acepta tildes)
Invoke-RestMethod "$api/Pentágono/calcular?lado=2"        # origen=DINAMICA area=6.8819 perimetro=10

# 6. Extra: Hexagono no existe localmente -> se descarga del catálogo externo automáticamente
Invoke-RestMethod "$api/Hexagono/calcular?lado=2"         # origen=EXTERNA area=10.3923 perimetro=12
Invoke-RestMethod -Method Post "$api/importar/Elipse"     # 201, parametros = a, b

# 7. Errores (Invoke-RestMethod lanza excepción; se muestra el código HTTP)
try { Invoke-RestMethod "$api/Circulo/calcular?radio=-1" } catch { [int]$_.Exception.Response.StatusCode }   # 400
try {
  Invoke-RestMethod -Method Post -Uri "$api/definiciones" -ContentType 'text/plain' `
    -Body 'class Malo implements Figura { Malo(double l) { System.exit(1) }; double area() { 0 }; double perimetro() { 0 } }'
} catch { [int]$_.Exception.Response.StatusCode }                                                           # 422

# Para ver el cuerpo del error (ProblemDetail) en Windows PowerShell 5.1, curl.exe es lo más directo:
curl.exe -s "$api/Circulo/calcular?radio=-1"
# {"title":"Parámetros inválidos","status":400,"detail":"El parámetro 'radio' debe ser un número mayor que 0 (recibido: -1.0)",...}
```

También puede probarse desde el navegador en **http://localhost:8080/figuras.html**.

## Pruebas

```powershell
.\mvnw.cmd test
```

- `FigurasApiTest` (`@SpringBootTest`, servidor real en puerto aleatorio): Circulo radio=5
  (área 78.5398, perímetro 31.4159), mayúsculas/tildes, query string, Triángulo, listado,
  Pentágono vía Groovy funcionando como nativo, Hexágono importado automáticamente del catálogo
  externo, Elipse importada a mano, y los errores 404 / 400 / 422.
- `CompiladorGroovyTest` (sin Spring): compila una figura válida conservando los nombres de
  parámetros y rechaza 10 intentos de escape (`System.exit`, `Runtime.exec`, `ProcessBuilder`,
  `File`, `Files`, `execute()`, `Class.forName`, `.class.classLoader`, `URL`, `GroovyShell`).
