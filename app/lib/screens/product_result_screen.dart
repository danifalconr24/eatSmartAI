import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../api_client.dart';
import '../data/chat_session.dart';
import '../widgets/score_header.dart';
import 'chat_screen.dart';

class ProductResultScreen extends StatefulWidget {
  const ProductResultScreen({
    super.key,
    required this.result,
    required this.goal,
    required this.budgetMatters,
    required this.allergies,
    required this.dietPreference,
  });

  final ProductAnalysisResult result;
  final String goal;
  final bool budgetMatters;
  final String allergies;
  final String dietPreference;

  @override
  State<ProductResultScreen> createState() => _ProductResultScreenState();
}

class _ProductResultScreenState extends State<ProductResultScreen> {
  ProductAnalysisResult get result => widget.result;

  /// Vive aquí (no en el popup) para conservar el historial al cerrar y
  /// reabrir el chat mientras la pantalla de resultados siga viva.
  late final ChatSession _chatSession = ChatSession(
    analysisContext: ChatContextData.product(
      product: widget.result.product,
      nutrition: widget.result.nutrition,
      score: widget.result.score,
      goal: widget.goal,
      budgetMatters: widget.budgetMatters,
      allergies: widget.allergies,
      dietPreference: widget.dietPreference,
    ),
  );

  @override
  void dispose() {
    _chatSession.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tu análisis'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilledButton.tonalIcon(
              onPressed: () => showChatPopup(context, _chatSession),
              icon: const Icon(Icons.question_answer, size: 18),
              label: const Text('Chat'),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          ScoreHeader(
            score: result.score,
            label: productScoreLabel(result.score),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Producto detectado',
                style: theme.textTheme.titleSmall,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Chip(
                label: Text(result.product),
                visualDensity: VisualDensity.compact,
              ),
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: Markdown(
              data: result.nutrition,
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
          if (result.alternative != null)
            Container(
              width: double.infinity,
              margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: theme.colorScheme.secondaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.swap_vert,
                        color: theme.colorScheme.onSecondaryContainer,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        'Alternativa más saludable',
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: theme.colorScheme.onSecondaryContainer,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    result.alternative!.name,
                    style: theme.textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  if (result.alternative!.reason.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      result.alternative!.reason,
                      style: theme.textTheme.bodyMedium,
                    ),
                  ],
                ],
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
                  label: const Text('Escanear otro producto'),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
