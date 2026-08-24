import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../api_client.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({super.key, required this.result});

  final AnalysisResult result;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tu análisis')),
      body: Column(
        children: [
          if (result.products.isNotEmpty)
            SizedBox(
              height: 56,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                children: [
                  for (final p in result.products)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Chip(label: Text(p)),
                    ),
                ],
              ),
            ),
          const Divider(height: 1),
          Expanded(
            child: Markdown(
              data: result.suggestions,
              padding: const EdgeInsets.all(16),
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
