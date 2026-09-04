import 'package:flutter/material.dart';

import '../ads/ad_service.dart';
import '../ads/credit_service.dart';

/// Chip del AppBar que muestra "Créditos: N" y un botón "+" para ganar
/// créditos viendo un vídeo con recompensa.
class CreditsChip extends StatefulWidget {
  const CreditsChip({super.key});

  @override
  State<CreditsChip> createState() => _CreditsChipState();
}

class _CreditsChipState extends State<CreditsChip> {
  bool _showingAd = false;

  Future<void> _watchAd() async {
    if (_showingAd) return;
    setState(() => _showingAd = true);
    try {
      final creditsEarned = await AdService.instance.showRewarded();
      if (!mounted) return;
      if (creditsEarned > 0) {
        await CreditService.instance.addCredits(creditsEarned);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '¡Has ganado $creditsEarned créditos!',
            ),
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Mira el vídeo completo para ganar créditos. Si no hay anuncios disponibles, inténtalo más tarde.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _showingAd = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListenableBuilder(
      listenable: CreditService.instance,
      builder: (context, _) {
        // Acotar la escala de texto del chip para que quepa en el AppBar
        // incluso con tamaños de fuente del sistema muy grandes.
        final textScaler = MediaQuery.textScalerOf(
          context,
        ).clamp(maxScaleFactor: 1.3);
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(textScaler: textScaler),
          child: Container(
            margin: const EdgeInsets.symmetric(vertical: 10),
            padding: const EdgeInsets.only(left: 12, right: 4),
            decoration: BoxDecoration(
              color: theme.colorScheme.surfaceContainerHighest.withValues(
                alpha: 0.6,
              ),
              borderRadius: BorderRadius.circular(24),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Flexible(
                  child: Text(
                    'Créditos: ${CreditService.instance.balance}',
                    style: theme.textTheme.labelLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 1,
                  ),
                ),
                IconButton(
                  onPressed: _showingAd ? null : _watchAd,
                  icon: _showingAd
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.add_circle),
                  color: theme.colorScheme.primary,
                  tooltip: 'Ver vídeo para ganar créditos',
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
