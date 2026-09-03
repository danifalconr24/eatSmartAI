import 'package:flutter/material.dart';

import '../ads/credit_service.dart';
import '../api_client.dart';
import '../data/chat_session.dart';
import '../data/shopping_list_repository.dart';
import '../models/shopping_list.dart';
import '../widgets/credits_dialog.dart';
import '../widgets/markdown_sections.dart';
import '../widgets/score_header.dart';
import 'chat_screen.dart';
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

  /// Vive aquí (no en el popup) para conservar el historial al cerrar y
  /// reabrir el chat mientras la pantalla de resultados siga viva.
  late final ChatSession _chatSession = ChatSession(
    analysisContext: ChatContextData.receipt(
      products: widget.result.products,
      suggestions: widget.result.suggestions,
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

  Future<void> _generateShoppingList() async {
    setState(() => _generating = true);
    try {
      // Solo se comprueba el saldo aquí: el crédito se descuenta cuando la
      // lista se ha generado con éxito (si falla, no se cobra).
      if (!CreditService.instance.hasCredits) {
        final earned = await showNoCreditsDialog(context);
        if (!mounted || !earned) return;
        if (!CreditService.instance.hasCredits) return;
      }
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
      // Cobrar el crédito solo tras generar y guardar la lista con éxito.
      await CreditService.instance.spendCredit();
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
    final sections = parseMarkdownSections(result.suggestions);
    // Intercepta también el gesto/botón atrás del sistema: siempre va a la
    // pantalla principal, nunca de vuelta al formulario.
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        Navigator.of(context).popUntil((route) => route.isFirst);
      },
      child: Scaffold(
        appBar: AppBar(
          // Volver lleva siempre a la pantalla principal, no al formulario.
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () =>
                Navigator.of(context).popUntil((route) => route.isFirst),
          ),
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
            label: scoreLabel(result.score),
          ),
          if (result.products.isNotEmpty)
            SizedBox(
              height: 48,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                children: [
                  for (final p in result.products)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Chip(
                        avatar: Icon(
                          Icons.shopping_basket,
                          size: 18,
                          color: theme.colorScheme.primary,
                        ),
                        label: Text(p),
                        visualDensity: VisualDensity.compact,
                      ),
                    ),
                ],
              ),
            ),
          Expanded(
            child: sections.isEmpty
                ? _FallbackSuggestions(suggestions: result.suggestions)
                : ListView(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    children: [
                      for (var i = 0; i < sections.length; i++)
                        ExpandableSectionCard(
                          section: sections[i],
                          initiallyExpanded: i == 0,
                        ),
                    ],
                  ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
              child: Column(
                children: [
                  SizedBox(
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
                  const SizedBox(height: 8),
                  SizedBox(
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
                ],
              ),
            ),
          ),
        ],
        ),
      ),
    );
  }
}

class _FallbackSuggestions extends StatelessWidget {
  const _FallbackSuggestions({required this.suggestions});

  final String suggestions;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Text(suggestions, style: theme.textTheme.bodyMedium),
    );
  }
}
