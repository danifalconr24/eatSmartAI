import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

/// One `##`-headed chunk of a markdown analysis response.
class MarkdownSection {
  const MarkdownSection({required this.title, required this.body});

  final String title;
  final String body;
}

/// Splits markdown produced by the backend prompts into its `##` sections.
/// Returns an empty list when the text has no parseable section.
List<MarkdownSection> parseMarkdownSections(String markdown) {
  final sections = <MarkdownSection>[];
  String? currentTitle;
  final buffer = StringBuffer();

  void flush() {
    final title = currentTitle;
    if (title == null) return;
    final body = buffer.toString().trim();
    if (body.isNotEmpty) {
      sections.add(MarkdownSection(title: title, body: body));
    }
    buffer.clear();
  }

  for (final line in markdown.split('\n')) {
    final match = RegExp(r'^##\s+(.+)$').firstMatch(line.trimRight());
    if (match != null) {
      flush();
      currentTitle = match.group(1)!.trim();
    } else if (currentTitle != null) {
      buffer.writeln(line);
    }
  }
  flush();
  return sections;
}

/// Icon for each known backend section title; falls back to a generic one.
IconData sectionIcon(String title) {
  final t = title.toLowerCase();
  if (t.contains('resumen') || t.contains('valoración')) {
    return Icons.summarize_outlined;
  }
  if (t.contains('faltan') || t.contains('grupos')) {
    return Icons.playlist_add;
  }
  if (t.contains('mejoras') || t.contains('selección')) {
    return Icons.swap_horiz;
  }
  if (t.contains('presupuesto')) return Icons.savings_outlined;
  if (t.contains('nutricional')) return Icons.fact_check_outlined;
  return Icons.lightbulb_outline;
}

/// Expandable card showing one markdown section behind an icon + title.
class ExpandableSectionCard extends StatelessWidget {
  const ExpandableSectionCard({
    super.key,
    required this.section,
    this.initiallyExpanded = false,
  });

  final MarkdownSection section;
  final bool initiallyExpanded;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.fromLTRB(16, 6, 16, 6),
      clipBehavior: Clip.antiAlias,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: theme.colorScheme.outlineVariant.withValues(alpha: 0.6),
        ),
      ),
      child: Theme(
        data: theme.copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          initiallyExpanded: initiallyExpanded,
          leading: Icon(
            sectionIcon(section.title),
            color: theme.colorScheme.primary,
          ),
          title: Text(
            section.title,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          tilePadding: const EdgeInsets.symmetric(horizontal: 16),
          childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: MarkdownBody(
                data: section.body,
                styleSheet: MarkdownStyleSheet.fromTheme(theme).copyWith(
                  p: theme.textTheme.bodyMedium,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
