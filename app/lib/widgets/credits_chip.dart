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
      final earned = await AdService.instance.showRewarded();
      if (!mounted) return;
      if (earned) {
        await CreditService.instance.addCredits();
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
                '¡Has ganado ${CreditService.creditsPerReward} créditos!'),
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
                'Mira el vídeo completo para ganar créditos. Si no hay anuncios disponibles, inténtalo más tarde.'),
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
        return Container(
          margin: const EdgeInsets.symmetric(vertical: 10),
          padding: const EdgeInsets.only(left: 12, right: 4),
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainerHighest
                .withValues(alpha: 0.6),
            borderRadius: BorderRadius.circular(24),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Créditos: ${CreditService.instance.balance}',
                style: theme.textTheme.labelLarge?.copyWith(
                  fontWeight: FontWeight.bold,
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
        );
      },
    );
  }
}
