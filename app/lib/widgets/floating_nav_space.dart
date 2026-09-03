import 'package:flutter/material.dart';

/// Espacio reservado para la barra de navegación flotante de HomeScreen.
///
/// HomeScreen mide la altura real renderizada de su barra (incluyendo el
/// SafeArea y el margen inferior) y la expone a las pantallas incrustadas,
/// para que ningún contenido quede tapado por la barra en ningún dispositivo.
class FloatingNavSpace extends InheritedNotifier<ValueNotifier<double>> {
  const FloatingNavSpace({
    super.key,
    required ValueNotifier<double> height,
    required super.child,
  }) : super(notifier: height);

  /// Altura total que ocupa la barra desde el borde inferior de la pantalla
  /// (0 si aún no se ha medido).
  static double of(BuildContext context) {
    final scope =
        context.dependOnInheritedWidgetOfExactType<FloatingNavSpace>();
    return scope?.notifier?.value ?? 0;
  }
}
