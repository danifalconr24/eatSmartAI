import 'package:flutter/material.dart';

import '../data/shopping_list_repository.dart';
import '../models/shopping_list.dart';
import 'shopping_list_detail_screen.dart';

/// Historial de listas de la compra guardadas en el dispositivo.
class ShoppingListsScreen extends StatefulWidget {
  const ShoppingListsScreen({super.key});

  @override
  State<ShoppingListsScreen> createState() => _ShoppingListsScreenState();
}

class _ShoppingListsScreenState extends State<ShoppingListsScreen> {
  final ShoppingListRepository _repository = ShoppingListRepository();
  List<ShoppingList> _lists = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    final lists = await _repository.loadAll();
    if (!mounted) return;
    setState(() {
      _lists = lists;
      _loading = false;
    });
  }

  Future<void> _confirmDelete(ShoppingList list) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Borrar lista'),
        content: const Text(
            '¿Seguro que quieres borrar esta lista? No se puede deshacer.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Borrar'),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await _repository.delete(list.id);
      await _reload();
    }
  }

  String _formatDate(DateTime date) {
    final local = date.toLocal();
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(local.day)}/${two(local.month)}/${local.year} '
        '${two(local.hour)}:${two(local.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Listas de compra')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _lists.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(32),
                    child: Text(
                      'Aún no tienes listas guardadas.\nAnaliza un ticket y genera tu primera lista.',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(8),
                  itemCount: _lists.length,
                  itemBuilder: (context, index) {
                    final list = _lists[index];
                    return Card(
                      clipBehavior: Clip.antiAlias,
                      child: ListTile(
                        leading: const Icon(Icons.shopping_cart_outlined),
                        title: Text(_formatDate(list.createdAt)),
                        subtitle: Text(
                          '${list.items.length} artículos · '
                          '${list.checkedCount} comprados',
                        ),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete_outline),
                          tooltip: 'Borrar lista',
                          onPressed: () => _confirmDelete(list),
                        ),
                        onTap: () async {
                          await Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (_) =>
                                  ShoppingListDetailScreen(listId: list.id),
                            ),
                          );
                          await _reload();
                        },
                      ),
                    );
                  },
                ),
    );
  }
}
