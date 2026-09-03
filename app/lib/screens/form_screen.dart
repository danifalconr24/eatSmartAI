import 'dart:io';

import 'package:flutter/material.dart';

import '../ads/credit_service.dart';
import '../widgets/credits_dialog.dart';
import 'analysis_screen.dart';
import 'product_analysis_screen.dart';
import 'scan_screen.dart';

class FormScreen extends StatefulWidget {
  const FormScreen({super.key, required this.imageFile, required this.mode});

  final File imageFile;
  final ScanMode mode;

  @override
  State<FormScreen> createState() => _FormScreenState();
}

class _FormScreenState extends State<FormScreen> {
  String _goal = 'MAINTAIN';
  bool _budgetMatters = false;
  String _dietPreference = 'NONE';
  final TextEditingController _allergiesController = TextEditingController();

  bool get _isTicket => widget.mode == ScanMode.ticket;

  @override
  void dispose() {
    _allergiesController.dispose();
    super.dispose();
  }

  bool _submitting = false;

  Future<void> _submit() async {
    if (_submitting) return;
    setState(() => _submitting = true);
    try {
      var paid = await CreditService.instance.spendCredit();
      if (!mounted) return;
      if (!paid) {
        // Sin créditos: ofrecer ganar créditos viendo un vídeo. Si los gana,
        // reintentar el pago para continuar con el análisis.
        final earned = await showNoCreditsDialog(context);
        if (!mounted || !earned) return;
        paid = await CreditService.instance.spendCredit();
        if (!mounted || !paid) return;
      }
      if (_isTicket) {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => AnalysisScreen(
              imageFile: widget.imageFile,
              goal: _goal,
              budgetMatters: _budgetMatters,
              allergies: _allergiesController.text.trim(),
              dietPreference: _dietPreference,
            ),
          ),
        );
      } else {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => ProductAnalysisScreen(
              imageFile: widget.imageFile,
              goal: _goal,
              budgetMatters: _budgetMatters,
              allergies: _allergiesController.text.trim(),
              dietPreference: _dietPreference,
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tu perfil')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('¿Cuál es tu objetivo?',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          SegmentedButton<String>(
            segments: const [
              ButtonSegment(value: 'LOSE', label: Text('Perder peso')),
              ButtonSegment(value: 'MAINTAIN', label: Text('Mantenerme')),
              ButtonSegment(value: 'GAIN', label: Text('Ganar peso')),
            ],
            selected: {_goal},
            onSelectionChanged: (s) => setState(() => _goal = s.first),
          ),
          const SizedBox(height: 24),
          SwitchListTile(
            title: const Text('¿Te importa el presupuesto?'),
            value: _budgetMatters,
            onChanged: (v) => setState(() => _budgetMatters = v),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _allergiesController,
            decoration: const InputDecoration(
              labelText: 'Alergias o intolerancias (opcional)',
              hintText: 'p. ej. lactosa, gluten',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 24),
          DropdownButtonFormField<String>(
            initialValue: _dietPreference,
            decoration: const InputDecoration(
              labelText: 'Preferencia dietética',
              border: OutlineInputBorder(),
            ),
            items: const [
              DropdownMenuItem(
                  value: 'NONE', child: Text('Sin preferencia')),
              DropdownMenuItem(
                  value: 'VEGETARIAN', child: Text('Vegetariano')),
              DropdownMenuItem(value: 'VEGAN', child: Text('Vegano')),
              DropdownMenuItem(value: 'OTHER', child: Text('Otra')),
            ],
            onChanged: (v) => setState(() => _dietPreference = v ?? 'NONE'),
          ),
          const SizedBox(height: 32),
          FilledButton.icon(
            onPressed: _submitting ? null : _submit,
            icon: _submitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.analytics),
            label: Text(_isTicket ? 'Analizar ticket' : 'Analizar producto'),
          ),
        ],
      ),
    );
  }
}
