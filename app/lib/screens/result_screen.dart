import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../api_client.dart';
import '../data/shopping_list_repository.dart';
import '../models/shopping_list.dart';
import '../widgets/score_header.dart';
import 'shopping_list_detail_screen.dart';

class ResultScreen extends StatefulWidget {
  const ResultScreen({
    super.key,
    required this.result,
    required this.goal,
    required this.budgetMatters,
    required this.allergies,
    required this.dietPreference,
  });

  final AnalysisResult result;
  final String goal;
  final bool budgetMatters;
  final String allergies;
  final String dietPreference;

  @override
  State<ResultScreen> createState() => _ResultScreenState();
}

class _ResultScreenState extends State<ResultScreen> {
  final ShoppingListRepository _repository = ShoppingListRepository();
  bool _generating = false;

  Future<void> _generateShoppingList() async {
    setState(() => _generating = true);
    try {
      final generated = await ApiClient().generateShoppingList(
        products: widget.result.products,
        suggestions: widget.result.suggestions,
        goal: widget.goal,
        budgetMatters: widget.budgetMatters,
        allergies: widget.allergies,
        dietPreference: widget.dietPreference,
      );
      final list = ShoppingList(
        items: [
          for (final item in generated.items)
            ShoppingListItem(
              name: item.name,
              category: item.category,
              type: ShoppingListItemType.values
                      .asNameMap()[item.type.toLowerCase()] ??
                  ShoppingListItemType.keep,
              replaces: item.replaces,
              reason: item.reason,
            ),
        ],
      );
      await _repository.save(list);
      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => ShoppingListDetailScreen(listId: list.id),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.message)),
      );
    } finally {
      if (mounted) setState(() => _generating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final result = widget.result;
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
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _generating ? null : _generateShoppingList,
                  icon: _generating
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.shopping_cart),
                  label: Text(_generating
                      ? 'Generando lista...'
                      : 'Generar lista de la compra sugerida'),
                ),
              ),
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.tonalIcon(
                  onPressed: _generating
                      ? null
                      : () => Navigator.of(context)
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
