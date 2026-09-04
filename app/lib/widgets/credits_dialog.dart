import 'package:flutter/material.dart';

import '../ads/ad_service.dart';
import '../ads/credit_service.dart';

/// Diálogo que se muestra cuando el usuario no tiene créditos suficientes.
/// Ofrece ver un vídeo con recompensa para ganar créditos al momento.
///
/// Devuelve `true` si el usuario ganó créditos (para que la pantalla pueda
/// reintentar la acción), `false` si canceló o no se ganó la recompensa.
Future<bool> showNoCreditsDialog(BuildContext context) async {
  final watch = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('Sin créditos'),
      content: const Text(
          'Necesitas 1 crédito para esta acción.\n\nMira un vídeo y gana créditos.'),
      actionsAlignment: MainAxisAlignment.end,
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('Cancelar'),
        ),
        FilledButton.icon(
          // El tema global fuerza ancho completo (Size.fromHeight(56)) en
          // FilledButton; dentro de un diálogo el botón debe ajustarse a su
          // contenido para alinearse con "Cancelar".
          style: FilledButton.styleFrom(
            minimumSize: const Size(0, 44),
            padding: const EdgeInsets.symmetric(horizontal: 20),
          ),
          onPressed: () => Navigator.of(dialogContext).pop(true),
          icon: const Icon(Icons.play_circle_outline, size: 18),
          label: const Text('Ver vídeo'),
        ),
      ],
    ),
  );
  if (watch != true || !context.mounted) return false;

  final creditsEarned = await AdService.instance.showRewarded();
  if (!context.mounted) return false;
  if (creditsEarned > 0) {
    await CreditService.instance.addCredits(creditsEarned);
    if (!context.mounted) return false;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('¡Has ganado $creditsEarned créditos!'),
      ),
    );
    return true;
  }
  ScaffoldMessenger.of(context).showSnackBar(
    const SnackBar(
      content: Text(
          'Mira el vídeo completo para ganar créditos. Si no hay anuncios disponibles, inténtalo más tarde.'),
    ),
  );
  return false;
}
