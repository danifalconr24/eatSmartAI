import 'package:flutter/material.dart';

import '../api_client.dart';
import '../data/chat_session.dart';
import '../widgets/markdown_sections.dart';
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
    final sections = parseMarkdownSections(result.nutrition);
    final alternative = result.alternative;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tu análisis'),
        actions: [
          TextButton.icon(
            onPressed: () => showChatPopup(context, _chatSession),
            icon: const Icon(Icons.question_answer, size: 18),
            label: const Text('Chat'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        children: [
          ScoreHeader(
            score: result.score,
            label: productScoreLabel(result.score),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Chip(
                avatar: Icon(
                  Icons.fastfood_outlined,
                  size: 18,
                  color: theme.colorScheme.primary,
                ),
                label: Text(result.product),
                visualDensity: VisualDensity.compact,
              ),
            ),
          ),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(vertical: 8),
              children: [
                if (sections.isEmpty)
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      result.nutrition,
                      style: theme.textTheme.bodyMedium,
                    ),
                  )
                else
                  for (var i = 0; i < sections.length; i++)
                    ExpandableSectionCard(
                      section: sections[i],
                      initiallyExpanded: i == 0,
                    ),
                if (alternative != null)
                  _AlternativeCard(alternative: alternative),
              ],
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
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

class _AlternativeCard extends StatelessWidget {
  const _AlternativeCard({required this.alternative});

  final ProductAlternative alternative;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(16, 6, 16, 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.swap_horiz,
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
            alternative.name,
            style: theme.textTheme.bodyLarge?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          if (alternative.reason.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              alternative.reason,
              style: theme.textTheme.bodyMedium,
            ),
          ],
        ],
      ),
    );
  }
}
