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
      content: Text(
          'Necesitas 1 crédito para esta acción.\n\nMira un vídeo y gana ${CreditService.creditsPerReward} créditos.'),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('Cancelar'),
        ),
        FilledButton.icon(
          onPressed: () => Navigator.of(dialogContext).pop(true),
          icon: const Icon(Icons.play_circle_outline, size: 18),
          label: const Text('Ver vídeo'),
        ),
      ],
    ),
  );
  if (watch != true || !context.mounted) return false;

  final earned = await AdService.instance.showRewarded();
  if (!context.mounted) return false;
  if (earned) {
    await CreditService.instance.addCredits();
    if (!context.mounted) return false;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content:
            Text('¡Has ganado ${CreditService.creditsPerReward} créditos!'),
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
