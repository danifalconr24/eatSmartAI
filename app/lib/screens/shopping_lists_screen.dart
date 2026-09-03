import 'package:flutter/material.dart';

import '../data/shopping_list_repository.dart';
import '../models/shopping_list.dart';
import '../widgets/banner_ad_widget.dart';
import '../widgets/floating_nav_space.dart';
import 'shopping_list_detail_screen.dart';

/// Historial de listas de la compra guardadas en el dispositivo.
class ShoppingListsScreen extends StatefulWidget {
  const ShoppingListsScreen({super.key, this.embedded = false});

  /// Cuando es true, la pantalla se renderiza sin Scaffold/AppBar propios
  /// (pensada para vivir dentro del PageView de HomeScreen).
  final bool embedded;

  @override
  State<ShoppingListsScreen> createState() => ShoppingListsScreenState();
}

class ShoppingListsScreenState extends State<ShoppingListsScreen>
    with AutomaticKeepAliveClientMixin {
  final ShoppingListRepository _repository = ShoppingListRepository();
  List<ShoppingList> _lists = [];
  bool _loading = true;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  /// Recarga las listas desde el almacenamiento local. Público para que
  /// HomeScreen pueda refrescar al cambiar a esta pestaña.
  Future<void> reload() => _reload();

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

  Widget _buildBody(BuildContext context) {
    final theme = Theme.of(context);
    final Widget content;
    if (_loading) {
      content = const Center(child: CircularProgressIndicator());
    } else if (_lists.isEmpty) {
      content = Center(
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
      );
    } else {
      content = _buildList();
    }
    return Column(
      children: [
        const Center(child: BannerAdWidget()),
        Expanded(child: content),
      ],
    );
  }

  Widget _buildList() {
    return ListView.builder(
      // Espacio inferior extra para que la barra de navegación flotante
      // de HomeScreen no tape la última tarjeta cuando está incrustada.
      padding: EdgeInsets.fromLTRB(
        8,
        8,
        8,
        widget.embedded ? FloatingNavSpace.of(context) + 16 : 8,
      ),
      itemCount: _lists.length,
      itemBuilder: (context, index) {
        final list = _lists[index];
        return Card(
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
    );
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    if (widget.embedded) {
      return _buildBody(context);
    }
    return Scaffold(
      appBar: AppBar(title: const Text('Listas de compra')),
      body: _buildBody(context),
    );
  }
}
