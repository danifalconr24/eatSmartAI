import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../api_client.dart';
import '../widgets/score_header.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({super.key, required this.result});

  final AnalysisResult result;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Tu análisis')),
      body: Column(
        children: [
          ScoreHeader(
            score: result.score,
            label: scoreLabel(result.score),
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
