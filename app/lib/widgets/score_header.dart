import 'package:flutter/material.dart';

class ScoreHeader extends StatelessWidget {
  const ScoreHeader({super.key, required this.score, required this.label});

  final int score;
  final String label;

  static const _scoreGood = Color(0xFF2E7D32);
  static const _scoreMid = Color(0xFFF9A825);
  static const _scoreBad = Color(0xFFC62828);

  Color get _color {
    if (score >= 7) return _scoreGood;
    if (score >= 4) return _scoreMid;
    return _scoreBad;
  }

  IconData get _face {
    if (score >= 7) return Icons.sentiment_very_satisfied;
    if (score >= 4) return Icons.sentiment_neutral;
    return Icons.sentiment_very_dissatisfied;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = _color;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          Icon(_face, size: 56, color: color),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: color,
                  ),
                ),
                const SizedBox(height: 6),
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: score / 10,
                    minHeight: 8,
                    backgroundColor: color.withValues(alpha: 0.15),
                    valueColor: AlwaysStoppedAnimation<Color>(color),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Que el número se reduzca antes de desbordar en pantallas
          // estrechas o con tamaños de fuente del sistema grandes.
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Text(
              '$score/10',
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

String scoreLabel(int score) {
  if (score >= 7) return '¡Buena compra!';
  if (score >= 4) return 'Compra mejorable';
  return 'Compra poco saludable';
}

String productScoreLabel(int score) {
  if (score >= 7) return '¡Producto saludable!';
  if (score >= 4) return 'Producto mejorable';
  return 'Producto poco saludable';
}
