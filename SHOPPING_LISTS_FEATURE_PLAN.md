# Plan: listas de la compra sugeridas

## Objetivo

Permitir generar una lista de la compra desde resultado de análisis de ticket. Lista combina productos detectados con sustituciones saludables sugeridas por proveedor IA. App guarda listas localmente y permite consultarlas, editarlas, marcar artículos comprados y borrarlas.

## Decisiones cerradas

- Implementación en rama actual. Sin commit ni push.
- Lista contiene artículos, sin cantidades ni unidades propuestas por IA.
- Sustituciones son alternativas recomendadas para productos detectados en ticket.
- Categorías fijas. IA asigna cada artículo a una categoría permitida.
- Usuario puede editar artículos y borrar listas guardadas.
- Backend sigue sin persistencia, autenticación ni cuentas. Persistencia vive solo en dispositivo.

## Categorías fijas

- Fruta y verdura
- Proteínas
- Lácteos y alternativas
- Despensa
- Panadería y cereales
- Congelados
- Bebidas
- Otros

## Contrato backend

Crear `POST /api/shopping-lists/generate`.

Entrada JSON:

```json
{
  "products": ["..."],
  "suggestions": "...",
  "goal": "...",
  "dietPreference": "...",
  "budgetMatters": false,
  "allergies": "..."
}
```

- `products`, `suggestions` y `goal` obligatorios.
- Preferencias restantes opcionales, con misma semántica que análisis de ticket.
- No reenviar imagen: ticket ya fue escaneado.

Salida JSON:

```json
{
  "categories": [
    {
      "name": "Fruta y verdura",
      "items": [
        {
          "name": "Manzanas",
          "type": "KEEP",
          "replaces": null,
          "reason": null
        },
        {
          "name": "Yogur natural sin azucar",
          "type": "REPLACE",
          "replaces": "Yogur azucarado",
          "reason": "Menos azucar anadido"
        }
      ]
    }
  ]
}
```

- Tipos: `KEEP`, `REPLACE`, `ADD`.
- `replaces` y `reason` obligatorios para `REPLACE`, nulos para otros tipos.
- Categoría debe ser una categoría fija. Parser rechaza respuestas incompletas, categorías no permitidas, duplicados y campos inválidos.

## Backend

1. Crear modelos dominio inmutables `ShoppingList`, `ShoppingListCategory`, `ShoppingListItem` y enum `ShoppingListItemType` en `backend/src/main/java/com/eatsmart/domain/model/`.
2. Crear request/response REST separados en infraestructura. Mantener `ErrorResponse.message` para errores visibles en app.
3. Crear `GenerateShoppingListUseCase` en `application/`. Ordenar gateways por `@Priority`, omitir proveedores sin clave y hacer fallback solo ante `AnalysisException`, igual que ticket/producto.
4. Crear `ShoppingListPromptBuilder`. Incluir productos, sugerencias y perfil. Exigir JSON estricto, categorías permitidas y solo artículos sin cantidades. Pedir conservar productos apropiados, sustituir productos cuando sugerencias lo indiquen y añadir artículos necesarios coherentes con objetivo.
5. Crear `ShoppingListResultParser`. Validar JSON, tipos, reglas de sustitución, categorías fijas y contenido no vacío.
6. Añadir recurso REST `ShoppingListResource` para endpoint. El recurso llama únicamente al caso de uso.
7. Reutilizar `ReceiptAnalysisGateway`, `OpenRouterGateway` y `GeminiGateway`: puerto ya transporta prompt y respuesta IA genéricos. No duplicar proveedores.
8. Mantener backend stateless. No introducir base de datos, repositorios de persistencia ni historial servidor.

## Flutter

1. Añadir dependencia `shared_preferences` en `app/pubspec.yaml` para persistir JSON pequeño. No usar directorio temporal de `path_provider`.
2. Crear modelos `ShoppingList`, `ShoppingListCategory` y `ShoppingListItem` con identificador UUID, fecha de creación, estado `checked`, tipo, sustitución y motivo.
3. Crear repositorio local que cargue, guarde, actualice y borre colección JSON bajo clave versionada de `SharedPreferences`.
4. Añadir `generateShoppingList` a `app/lib/api_client.dart`, con modelos request/response y manejo actual de `DioException` que muestra campo backend `message`.
5. Conservar en resultado de ticket productos, sugerencias y preferencias del formulario necesarias para solicitud posterior.
6. Convertir `ResultScreen` en pantalla con estado. Añadir botón `Generar lista de la compra sugerida`; mientras solicita, deshabilitar botón y mostrar indicador de carga. Al éxito, persistir lista y abrir detalle. Al error, mostrar mensaje español recibido.
7. Añadir acceso `Listas de compra` desde `HomeScreen`.
8. Crear pantalla de historial: listas guardadas con fecha, resumen y acción borrar con confirmación.
9. Crear pantalla detalle: artículos agrupados por categorías fijas, checkbox de compra, sustitución/motivo cuando aplique, edición de nombre/categoría/estado y eliminación de artículos.
10. Mantener navegación imperativa existente con `Navigator` y `MaterialPageRoute`; no añadir router global para esta feature.

## Pruebas y validación

1. Backend: pruebas unitarias de prompt, parser y caso de uso para prioridad, proveedor deshabilitado, fallback y respuesta de negocio válida.
2. Backend: pruebas REST de validación request, respuesta JSON y mapeo de errores.
3. Flutter: pruebas de serialización/deserialización del repositorio local, carga, actualización y borrado.
4. Flutter: pruebas de parsing cliente API y widgets para estados carga, error, persistencia y navegación.
5. Ejecutar `./mvnw test` y `./mvnw clean package` desde `backend/`.
6. Ejecutar `flutter test` y análisis Flutter desde `app/`.

## Riesgos

- `suggestions` actual es Markdown libre. Solicitud nueva debe incluirlo como contexto, pero resultado nuevo debe ser JSON estructurado; nunca extraer sustituciones desde Markdown en cliente.
- IA puede inventar categorías, cantidades o ignorar alergias. Prompt y parser deben restringir categorías, prohibir cantidades y rechazar estructura inválida.
- Listas solo viven en dispositivo. Borrar datos de app o cambiar dispositivo elimina historial; comportamiento esperado mientras no exista backend con cuentas.
