import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../api_client.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({super.key, required this.result});

  final AnalysisResult result;

  static const _scoreGood = Color(0xFF2E7D32);
  static const _scoreMid = Color(0xFFF9A825);
  static const _scoreBad = Color(0xFFC62828);

  Color get _scoreColor {
    if (result.score >= 7) return _scoreGood;
    if (result.score >= 4) return _scoreMid;
    return _scoreBad;
  }

  IconData get _scoreFace {
    if (result.score >= 7) return Icons.sentiment_very_satisfied;
    if (result.score >= 4) return Icons.sentiment_neutral;
    return Icons.sentiment_very_dissatisfied;
  }

  String get _scoreLabel {
    if (result.score >= 7) return '¡Buena compra!';
    if (result.score >= 4) return 'Compra mejorable';
    return 'Compra poco saludable';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Tu análisis')),
      body: Column(
        children: [
          _ScoreHeader(
            score: result.score,
            color: _scoreColor,
            face: _scoreFace,
            label: _scoreLabel,
          ),
          if (result.products.isNotEmpty) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Productos detectados',
                  style: theme.textTheme.titleSmall,
                ),
              ),
            ),
            SizedBox(
              height: 52,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                children: [
                  for (final p in result.products)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Chip(
                        label: Text(p),
                        visualDensity: VisualDensity.compact,
                      ),
                    ),
                ],
              ),
            ),
          ],
          const Divider(height: 1),
          Expanded(
            child: Markdown(
              data: result.suggestions,
              padding: const EdgeInsets.all(16),
              styleSheet: MarkdownStyleSheet.fromTheme(theme).copyWith(
                h2: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.primary,
                ),
                h2Padding: const EdgeInsets.only(top: 16, bottom: 4),
              ),
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: () => Navigator.of(context)
                      .popUntil((route) => route.isFirst),
                  icon: const Icon(Icons.camera_alt),
                  label: const Text('Escanear otro ticket'),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ScoreHeader extends StatelessWidget {
  const _ScoreHeader({
    required this.score,
    required this.color,
    required this.face,
    required this.label,
  });

  final int score;
  final Color color;
  final IconData face;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
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
          Icon(face, size: 56, color: color),
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
          Text(
            '$score/10',
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
