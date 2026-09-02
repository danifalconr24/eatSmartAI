import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';

/// Cámara trasera compartida por todas las pantallas de escaneo.
/// iOS sólo permite una sesión de captura activa por app, así que no podemos
/// tener un CameraController por pantalla viva en el PageView.
class SharedCamera {
  SharedCamera._();

  static final SharedCamera instance = SharedCamera._();

  final ValueNotifier<CameraController?> controller =
      ValueNotifier<CameraController?>(null);

  bool _initializing = false;

  /// Inicializa la cámara una sola vez; llamadas concurrentes esperan la
  /// misma inicialización.
  Future<void> ensureInitialized() async {
    if (controller.value != null || _initializing) return;
    _initializing = true;
    try {
      final cameras = await availableCameras();
      if (cameras.isEmpty) return;
      final back = cameras.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.back,
        orElse: () => cameras.first,
      );
      final cam = CameraController(back, ResolutionPreset.high);
      await cam.initialize();
      controller.value = cam;
    } catch (_) {
      // Sin cámara disponible: controller.value se queda en null.
    } finally {
      _initializing = false;
    }
  }
}
