import 'dart:io';

import 'package:flutter/material.dart';

import '../api_client.dart';
import 'result_screen.dart';

class AnalysisScreen extends StatefulWidget {
  const AnalysisScreen({
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
  State<AnalysisScreen> createState() => _AnalysisScreenState();
}

class _AnalysisScreenState extends State<AnalysisScreen> {
  @override
  void initState() {
    super.initState();
    _run();
  }

  Future<void> _run() async {
    try {
      final result = await ApiClient().analyze(
        image: widget.imageFile,
        goal: widget.goal,
        budgetMatters: widget.budgetMatters,
        allergies: widget.allergies,
        dietPreference: widget.dietPreference,
      );
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => ResultScreen(
            result: result,
            goal: widget.goal,
            budgetMatters: widget.budgetMatters,
            allergies: widget.allergies,
            dietPreference: widget.dietPreference,
          ),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      _showErrorAndGoBack(e.message);
    }
  }

  void _showErrorAndGoBack(String message) {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Error'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              Navigator.of(context).pop();
            },
            child: const Text('Volver'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              _run();
            },
            child: const Text('Reintentar'),
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
                  Icons.receipt_long,
                  size: 64,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
              ),
              const SizedBox(height: 32),
              Text(
                'Analizando tu ticket...',
                style: theme.textTheme.titleLarge,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                'Nuestro nutricionista está revisando tu compra',
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
