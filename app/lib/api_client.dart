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

class ApiException implements Exception {
  ApiException(this.message);

  final String message;

  @override
  String toString() => message;
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
      final serverMessage = e.response?.data is Map<String, dynamic>
          ? (e.response!.data as Map<String, dynamic>)['message']?.toString()
          : null;
      if (serverMessage != null && serverMessage.isNotEmpty) {
        throw ApiException(serverMessage);
      }
      if (e.type == DioExceptionType.connectionError ||
          e.type == DioExceptionType.connectionTimeout) {
        throw ApiException(
            'No se pudo conectar con el servidor. Comprueba que el backend está en marcha.');
      }
      throw ApiException('Ha ocurrido un error inesperado. Inténtalo de nuevo.');
    }
  }
}
