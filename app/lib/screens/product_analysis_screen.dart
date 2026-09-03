import 'dart:io';

import 'package:flutter/material.dart';

import '../ads/credit_service.dart';
import '../api_client.dart';
import 'product_result_screen.dart';

class ProductAnalysisScreen extends StatefulWidget {
  const ProductAnalysisScreen({
    super.key,
    required this.imageFile,
    required this.goal,
    required this.budgetMatters,
    required this.allergies,
    required this.dietPreference,
  });

  final File imageFile;
  final String goal;
  final bool budgetMatters;
  final String allergies;
  final String dietPreference;

  @override
  State<ProductAnalysisScreen> createState() => _ProductAnalysisScreenState();
}

class _ProductAnalysisScreenState extends State<ProductAnalysisScreen> {
  @override
  void initState() {
    super.initState();
    _run();
  }

  Future<void> _run() async {
    try {
      final result = await ApiClient().analyzeProduct(
        image: widget.imageFile,
        goal: widget.goal,
        budgetMatters: widget.budgetMatters,
        allergies: widget.allergies,
        dietPreference: widget.dietPreference,
      );
      if (!mounted) return;
      // El crédito se descuenta solo cuando el análisis se ha recibido con
      // éxito; si falla (error del servicio, imagen no válida...) no se cobra.
      await CreditService.instance.spendCredit();
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => ProductResultScreen(
            result: result,
            goal: widget.goal,
            budgetMatters: widget.budgetMatters,
            allergies: widget.allergies,
            dietPreference: widget.dietPreference,
          ),
        ),
      );
    } on UnreadableImageException catch (e) {
      // Respuesta de negocio válida ("producto no reconocible"): la
      // petición consumió análisis del servicio, así que también descuenta
      // crédito.
      await CreditService.instance.spendCredit();
      if (!mounted) return;
      _showErrorAndGoBack(e.message);
    } on ApiException catch (e) {
      // Fallo técnico (proveedor caído, red...): no se descuenta crédito.
      if (!mounted) return;
      _showErrorAndGoBack(e.message);
    }
  }

  void _showErrorAndGoBack(String message) {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        icon: Icon(
          Icons.error_outline,
          size: 40,
          color: Theme.of(dialogContext).colorScheme.error,
        ),
        title: const Text('Error'),
        content: Text(message, textAlign: TextAlign.center),
        actionsAlignment: MainAxisAlignment.center,
        actionsOverflowAlignment: OverflowBarAlignment.center,
        actionsOverflowDirection: VerticalDirection.down,
        actionsOverflowButtonSpacing: 8,
        actions: [
          FilledButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              _run();
            },
            child: const Text('Reintentar'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              Navigator.of(context).pop();
            },
            child: const Text('Volver'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('eatSmartAI')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.inventory_2,
                  size: 64,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
              ),
              const SizedBox(height: 32),
              Text(
                'Analizando tu producto...',
                style: theme.textTheme.titleLarge,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                'Nuestro nutricionista está revisando la información',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 32),
              const SizedBox(
                width: 160,
                child: LinearProgressIndicator(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
