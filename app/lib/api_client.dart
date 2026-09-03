import 'dart:io';

import 'package:dio/dio.dart';

/// URL base del backend. Por defecto apunta al emulador de Android.
/// Sobreescribible con: flutter run --dart-define=BACKEND_URL=http://192.168.1.X:8080
const String kBackendBaseUrl = String.fromEnvironment(
  'BACKEND_URL',
  defaultValue: 'http://10.0.2.2:8080',
);

class AnalysisResult {
  AnalysisResult({
    required this.products,
    required this.suggestions,
    required this.score,
  });

  final List<String> products;
  final String suggestions;

  /// Puntuación de saludabilidad de la compra, de 0 a 10.
  final int score;
}

class ProductAnalysisResult {
  ProductAnalysisResult({
    required this.product,
    required this.score,
    required this.nutrition,
    this.alternative,
  });

  final String product;
  final int score;
  final String nutrition;
  final ProductAlternative? alternative;
}

class ProductAlternative {
  ProductAlternative({required this.name, required this.reason});

  final String name;
  final String reason;
}

class ApiException implements Exception {
  ApiException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// Respuesta de negocio válida del backend indicando que la imagen no sirve
/// ("ticket no legible", "producto no reconocible"...). Es un HTTP 400: la
/// petición consumió análisis del servicio, así que cuenta como uso (gasta
/// crédito), a diferencia de un fallo técnico (502/red), que no lo gasta.
class UnreadableImageException extends ApiException {
  UnreadableImageException(super.message);
}

class ShoppingListItemResult {
  ShoppingListItemResult({
    required this.name,
    required this.category,
    required this.type,
    this.replaces,
    this.reason,
  });

  final String name;
  final String category;

  /// KEEP, REPLACE o ADD (tal como los emite el backend).
  final String type;
  final String? replaces;
  final String? reason;
}

class ShoppingListResult {
  ShoppingListResult({required this.items});

  final List<ShoppingListItemResult> items;
}

/// Un mensaje del chat con el nutricionista ('user' o 'assistant').
class ChatMessage {
  ChatMessage({required this.role, required this.content});

  final String role;
  final String content;

  Map<String, dynamic> toJson() => {'role': role, 'content': content};
}

/// Contexto del análisis sobre el que versa el chat: ticket (products +
/// suggestions) o producto (product + nutrition). El backend es stateless:
/// se envía en cada petición.
class ChatContextData {
  ChatContextData({
    this.products,
    this.suggestions,
    this.product,
    this.nutrition,
    this.score,
    required this.goal,
    required this.budgetMatters,
    required this.allergies,
    required this.dietPreference,
  });

  /// Chat sobre análisis de ticket.
  factory ChatContextData.receipt({
    required List<String> products,
    required String suggestions,
    required int score,
    required String goal,
    required bool budgetMatters,
    required String allergies,
    required String dietPreference,
  }) =>
      ChatContextData(
        products: products,
        suggestions: suggestions,
        score: score,
        goal: goal,
        budgetMatters: budgetMatters,
        allergies: allergies,
        dietPreference: dietPreference,
      );

  /// Chat sobre análisis de producto.
  factory ChatContextData.product({
    required String product,
    required String nutrition,
    required int score,
    required String goal,
    required bool budgetMatters,
    required String allergies,
    required String dietPreference,
  }) =>
      ChatContextData(
        product: product,
        nutrition: nutrition,
        score: score,
        goal: goal,
        budgetMatters: budgetMatters,
        allergies: allergies,
        dietPreference: dietPreference,
      );

  final List<String>? products;
  final String? suggestions;
  final String? product;
  final String? nutrition;
  final int? score;
  final String goal;
  final bool budgetMatters;
  final String allergies;
  final String dietPreference;

  Map<String, dynamic> toJson() => {
        if (products != null) 'products': products,
        if (suggestions != null) 'suggestions': suggestions,
        if (product != null) 'product': product,
        if (nutrition != null) 'nutrition': nutrition,
        if (score != null) 'score': score,
        'goal': goal,
        'budgetMatters': budgetMatters,
        'allergies': allergies,
        'dietPreference': dietPreference,
      };
}

class ApiClient {
  ApiClient()
      : _dio = Dio(BaseOptions(
          baseUrl: kBackendBaseUrl,
          connectTimeout: const Duration(seconds: 15),
          receiveTimeout: const Duration(seconds: 120),
          sendTimeout: const Duration(seconds: 60),
        ));

  final Dio _dio;

  Future<AnalysisResult> analyze({
    required File image,
    required String goal,
    required bool budgetMatters,
    required String allergies,
    required String dietPreference,
  }) async {
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(image.path, filename: 'ticket.jpg'),
      'goal': goal,
      'budgetMatters': budgetMatters.toString(),
      'allergies': allergies,
      'dietPreference': dietPreference,
    });

    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/analyze',
        data: formData,
      );
      final data = response.data ?? {};
      return AnalysisResult(
        products: (data['products'] as List<dynamic>? ?? [])
            .map((e) => e.toString())
            .toList(),
        suggestions: data['suggestions']?.toString() ?? '',
        score: (data['score'] as num?)?.round().clamp(0, 10) ?? 0,
      );
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  Future<ProductAnalysisResult> analyzeProduct({
    required File image,
    required String goal,
    required bool budgetMatters,
    required String allergies,
    required String dietPreference,
  }) async {
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(image.path, filename: 'product.jpg'),
      'goal': goal,
      'budgetMatters': budgetMatters.toString(),
      'allergies': allergies,
      'dietPreference': dietPreference,
    });

    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/analyze/product',
        data: formData,
      );
      final data = response.data ?? {};
      final altData = data['alternative'] as Map<String, dynamic>?;
      return ProductAnalysisResult(
        product: data['product']?.toString() ?? '',
        score: (data['score'] as num?)?.round().clamp(0, 10) ?? 0,
        nutrition: data['nutrition']?.toString() ?? '',
        alternative: altData != null
            ? ProductAlternative(
                name: altData['name']?.toString() ?? '',
                reason: altData['reason']?.toString() ?? '',
              )
            : null,
      );
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  Future<ShoppingListResult> generateShoppingList({
    required List<String> products,
    required String suggestions,
    required String goal,
    required bool budgetMatters,
    required String allergies,
    required String dietPreference,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/shopping-lists/generate',
        data: {
          'products': products,
          'suggestions': suggestions,
          'goal': goal,
          'dietPreference': dietPreference,
          'budgetMatters': budgetMatters,
          'allergies': allergies,
        },
      );
      final data = response.data ?? {};
      final categories = data['categories'] as List<dynamic>? ?? [];
      final items = <ShoppingListItemResult>[];
      for (final category in categories.whereType<Map<String, dynamic>>()) {
        final categoryName = category['name']?.toString() ?? 'Otros';
        final categoryItems = category['items'] as List<dynamic>? ?? [];
        for (final item in categoryItems.whereType<Map<String, dynamic>>()) {
          items.add(ShoppingListItemResult(
            name: item['name']?.toString() ?? '',
            category: categoryName,
            type: item['type']?.toString() ?? 'KEEP',
            replaces: item['replaces']?.toString(),
            reason: item['reason']?.toString(),
          ));
        }
      }
      return ShoppingListResult(items: items);
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  /// Envía una pregunta al nutricionista sobre un análisis previo.
  /// [history] contiene los turnos anteriores de la conversación (sin la
  /// pregunta actual); el backend no guarda estado.
  Future<String> askNutritionist({
    required ChatContextData context,
    required List<ChatMessage> history,
    required String question,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/chat',
        data: {
          ...context.toJson(),
          'messages': history.map((m) => m.toJson()).toList(),
          'question': question,
        },
      );
      final data = response.data ?? {};
      return data['answer']?.toString() ?? '';
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  /// Traduce un error HTTP/red a la excepción de dominio correspondiente.
  /// Un 400 del backend es una respuesta de negocio ("imagen no válida"):
  /// se mapea a [UnreadableImageException]. El resto son fallos técnicos.
  ApiException _mapError(DioException e) {
    final serverMessage = _extractErrorMessage(e);
    if (serverMessage != null) {
      if (e.response?.statusCode == 400) {
        return UnreadableImageException(serverMessage);
      }
      return ApiException(serverMessage);
    }
    if (e.type == DioExceptionType.connectionError ||
        e.type == DioExceptionType.connectionTimeout) {
      return ApiException(
          'No se pudo conectar con el servidor. Comprueba que el backend está en marcha.');
    }
    return ApiException('Ha ocurrido un error inesperado. Inténtalo de nuevo.');
  }

  String? _extractErrorMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map<String, dynamic>) {
      final msg = data['message']?.toString();
      if (msg != null && msg.isNotEmpty) return msg;
    }
    return null;
  }
}
