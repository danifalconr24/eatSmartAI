import 'package:flutter/material.dart';

import '../data/shopping_list_repository.dart';
import '../models/shopping_list.dart';

/// Detalle de una lista de la compra: artículos por categoría, checkbox de
/// compra, edición y eliminación.
class ShoppingListDetailScreen extends StatefulWidget {
  const ShoppingListDetailScreen({super.key, required this.listId});

  final String listId;

  @override
  State<ShoppingListDetailScreen> createState() =>
      _ShoppingListDetailScreenState();
}

class _ShoppingListDetailScreenState extends State<ShoppingListDetailScreen> {
  final ShoppingListRepository _repository = ShoppingListRepository();
  ShoppingList? _list;
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
      _list = lists.where((l) => l.id == widget.listId).firstOrNull;
      _loading = false;
    });
  }

  Future<void> _persist(ShoppingList list) async {
    await _repository.update(list);
    await _reload();
  }

  Future<void> _toggleChecked(ShoppingListItem item) async {
    final list = _list;
    if (list == null) return;
    await _persist(list.copyWithItems([
      for (final i in list.items)
        i.id == item.id ? i.copyWith(checked: !i.checked) : i,
    ]));
  }

  Future<void> _deleteItem(ShoppingListItem item) async {
    final list = _list;
    if (list == null) return;
    await _persist(list.copyWithItems([
      for (final i in list.items)
        if (i.id != item.id) i,
    ]));
  }

  Future<void> _editItem(ShoppingListItem item) async {
    final list = _list;
    if (list == null) return;
    final nameController = TextEditingController(text: item.name);
    String category = item.category;
    final saved = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) => AlertDialog(
          title: const Text('Editar artículo'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameController,
                  autofocus: true,
                  decoration: const InputDecoration(
                    labelText: 'Nombre',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  initialValue: category,
                  decoration: const InputDecoration(
                    labelText: 'Categoría',
                    border: OutlineInputBorder(),
                  ),
                  items: [
                    for (final c in kShoppingCategories)
                      DropdownMenuItem(value: c, child: Text(c)),
                  ],
                  onChanged: (v) {
                    if (v != null) setDialogState(() => category = v);
                  },
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Guardar'),
            ),
          ],
        ),
      ),
    );
    final newName = nameController.text.trim();
    if (saved == true && newName.isNotEmpty) {
      await _persist(list.copyWithItems([
        for (final i in list.items)
          i.id == item.id ? i.copyWith(name: newName, category: category) : i,
      ]));
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      nameController.dispose();
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final list = _list;
    return Scaffold(
      appBar: AppBar(title: const Text('Lista de la compra')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : list == null
              ? const Center(child: Text('La lista ya no existe.'))
              : ListView(
                  padding: const EdgeInsets.only(bottom: 24),
                  children: [
                    for (final entry in list.itemsByCategory.entries) ...[
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
                        child: Text(
                          entry.key,
                          style: theme.textTheme.titleSmall?.copyWith(
                            color: theme.colorScheme.primary,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      for (final item in entry.value)
                        Dismissible(
                          key: ValueKey(item.id),
                          direction: DismissDirection.endToStart,
                          background: Container(
                            alignment: Alignment.centerRight,
                            padding: const EdgeInsets.only(right: 16),
                            color: theme.colorScheme.errorContainer,
                            child: Icon(
                              Icons.delete_outline,
                              color: theme.colorScheme.onErrorContainer,
                            ),
                          ),
                          onDismissed: (_) => _deleteItem(item),
                          child: CheckboxListTile(
                            value: item.checked,
                            onChanged: (_) => _toggleChecked(item),
                            controlAffinity: ListTileControlAffinity.leading,
                            title: Text(
                              item.name,
                              style: item.checked
                                  ? const TextStyle(
                                      decoration: TextDecoration.lineThrough)
                                  : null,
                            ),
                            subtitle: item.type ==
                                        ShoppingListItemType.replace &&
                                    item.replaces != null
                                ? Text(
                                    'Sustituye a ${item.replaces}'
                                    '${item.reason != null ? ' · ${item.reason}' : ''}',
                                  )
                                : null,
                            secondary: IconButton(
                              icon: const Icon(Icons.edit_outlined),
                              tooltip: 'Editar artículo',
                              onPressed: () => _editItem(item),
                            ),
                          ),
                        ),
                    ],
                  ],
                ),
    );
  }
}
