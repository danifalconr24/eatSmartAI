import 'package:flutter/foundation.dart';

import '../api_client.dart';

/// Sesión de chat con el nutricionista ligada a una pantalla de resultados.
/// Vive fuera del popup ([ChatOverlay]) para que el historial se conserve al
/// cerrar y reabrir el chat mientras la pantalla de resultados siga viva.
class ChatSession extends ChangeNotifier {
  ChatSession({required this.analysisContext});

  /// Preguntas que el usuario puede hacer por análisis.
  static const int maxQuestions = 2;

  final ChatContextData analysisContext;

  final List<ChatMessage> messages = [];
  bool sending = false;

  /// Preguntas ya hechas por el usuario en esta sesión.
  int get questionsAsked => messages.where((m) => m.role == 'user').length;

  /// Preguntas que quedan disponibles (0 = límite alcanzado).
  int get questionsRemaining =>
      (maxQuestions - questionsAsked).clamp(0, maxQuestions);

  Future<void> send(String question) async {
    final trimmed = question.trim();
    if (trimmed.isEmpty || sending || questionsRemaining <= 0) return;

    sending = true;
    messages.add(ChatMessage(role: 'user', content: trimmed));
    notifyListeners();

    try {
      final answer = await ApiClient().askNutritionist(
        context: analysisContext,
        // Historial sin la pregunta actual (va en 'question').
        history: messages.sublist(0, messages.length - 1),
        question: trimmed,
      );
      messages.add(ChatMessage(role: 'assistant', content: answer));
    } on ApiException catch (e) {
      error = e.message;
    } finally {
      sending = false;
      notifyListeners();
    }
  }

  /// Último error de envío, consumido por la UI (SnackBar) y limpiado.
  String? error;

  String? consumeError() {
    final e = error;
    error = null;
    return e;
  }
}
