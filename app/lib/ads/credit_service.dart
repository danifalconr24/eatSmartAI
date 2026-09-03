import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Sistema de créditos del usuario.
///
/// 1 crédito = 1 escaneo (ticket o producto) o 1 generación de lista de la
/// compra. Los créditos se ganan viendo anuncios de vídeo con recompensa y se
/// persisten en el dispositivo con shared_preferences.
class CreditService extends ChangeNotifier {
  CreditService._();

  static final CreditService instance = CreditService._();

  static const String _prefsKey = 'credits_balance';

  /// Créditos que se conceden por cada vídeo con recompensa visto completo.
  /// Sobreescribible con:
  /// flutter run --dart-define=CREDITS_PER_REWARD=5
  static const int creditsPerReward =
      int.fromEnvironment('CREDITS_PER_REWARD', defaultValue: 3);

  /// Créditos con los que empieza un usuario nuevo (primera ejecución).
  /// Sobreescribible con:
  /// flutter run --dart-define=INITIAL_CREDITS=5
  static const int initialCredits =
      int.fromEnvironment('INITIAL_CREDITS', defaultValue: 1);

  int _balance = 0;
  bool _loaded = false;

  int get balance => _balance;

  /// True si hay al menos 1 crédito disponible.
  bool get hasCredits => _balance > 0;

  /// Carga el saldo guardado. Idempotente. Si el usuario nunca ha tenido
  /// saldo guardado (primera ejecución), empieza con [initialCredits].
  Future<void> initialize() async {
    if (_loaded) return;
    _loaded = true;
    try {
      final prefs = await SharedPreferences.getInstance();
      _balance = prefs.getInt(_prefsKey) ?? initialCredits;
    } catch (e) {
      debugPrint('CreditService: no se pudo cargar el saldo: $e');
      _balance = initialCredits;
    }
    notifyListeners();
  }

  /// Añade [amount] créditos (por defecto, la recompensa de un vídeo).
  Future<void> addCredits([int amount = creditsPerReward]) async {
    _balance += amount;
    notifyListeners();
    await _persist();
  }

  /// Gasta 1 crédito. Devuelve false si no hay saldo suficiente.
  Future<bool> spendCredit() async {
    if (_balance <= 0) return false;
    _balance -= 1;
    notifyListeners();
    await _persist();
    return true;
  }

  Future<void> _persist() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(_prefsKey, _balance);
    } catch (e) {
      debugPrint('CreditService: no se pudo guardar el saldo: $e');
    }
  }
}
