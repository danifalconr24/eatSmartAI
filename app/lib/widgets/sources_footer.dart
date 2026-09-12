import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api_client.dart';

/// Fuentes autorizadas que se muestran como fallback cuando el proveedor no
/// devuelve citas dinámicas.
const List<Source> kDefaultSources = [
  Source(
    title: 'OMS - Nutrición',
    url: 'https://www.who.int/es/health-topics/nutrition',
  ),
  Source(
    title: 'NIH - Información de salud',
    url: 'https://www.niddk.nih.gov/health-information/informacion-de-la-salud',
  ),
  Source(
    title: 'Harvard Nutrition Source',
    url: 'https://www.hsph.harvard.edu/nutritionsource/',
  ),
];

/// Abre [url] en el navegador del sistema. Silencia errores.
Future<void> openSourceUrl(String url) async {
  final uri = Uri.tryParse(url);
  if (uri == null || !{'http', 'https'}.contains(uri.scheme)) return;
  try {
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  } catch (_) {
    // Ignorar: el enlace seguirá siendo visible para el usuario.
  }
}

/// Renderiza chips clicables de fuentes. Si no hay fuentes dinámicas, muestra
/// un fallback con fuentes autorizadas.
class SourcesFooter extends StatelessWidget {
  const SourcesFooter({
    super.key,
    required this.sources,
    this.fallbackLabel = 'Fuentes',
  });

  final List<Source> sources;
  final String fallbackLabel;

  List<Source> get _displaySources => sources.isNotEmpty ? sources : kDefaultSources;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.menu_book_outlined, size: 18, color: theme.colorScheme.primary),
              const SizedBox(width: 8),
              Text(
                sources.isNotEmpty ? 'Fuentes' : fallbackLabel,
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
          if (sources.isEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                'Información basada en guías de nutrición reconocidas.',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final source in _displaySources)
                ActionChip(
                  avatar: Icon(Icons.open_in_new, size: 16, color: theme.colorScheme.primary),
                  label: Text(source.title),
                  visualDensity: VisualDensity.compact,
                  onPressed: () => openSourceUrl(source.url),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Versión compacta para mostrar dentro de tarjetas (alternativa, items de lista).
class SourceChipList extends StatelessWidget {
  const SourceChipList({super.key, required this.sources});

  final List<Source> sources;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final display = sources.isNotEmpty ? sources : kDefaultSources;
    return Wrap(
      spacing: 8,
      runSpacing: 4,
      children: [
        for (final source in display)
          ActionChip(
            avatar: Icon(Icons.open_in_new, size: 14, color: theme.colorScheme.primary),
            label: Text(source.title),
            visualDensity: VisualDensity.compact,
            labelStyle: theme.textTheme.labelSmall,
            padding: EdgeInsets.zero,
            onPressed: () => openSourceUrl(source.url),
          ),
      ],
    );
  }
}
