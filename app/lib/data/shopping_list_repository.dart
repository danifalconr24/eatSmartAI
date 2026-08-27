import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/shopping_list.dart';

/// Repositorio local de listas de la compra. Persiste la colección como JSON
/// en SharedPreferences bajo una clave versionada.
class ShoppingListRepository {
  static const String _storageKey = 'shopping_lists_v1';

  Future<List<ShoppingList>> loadAll() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_storageKey);
    if (raw == null || raw.isEmpty) return [];
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! List<dynamic>) return [];
      final lists = decoded
          .whereType<Map<String, dynamic>>()
          .map(ShoppingList.fromJson)
          .toList();
      lists.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      return lists;
    } on FormatException {
      return [];
    }
  }

  Future<ShoppingList> save(ShoppingList list) async {
    final lists = await loadAll();
    lists.insert(0, list);
    await _persist(lists);
    return list;
  }

  Future<void> update(ShoppingList list) async {
    final lists = await loadAll();
    final index = lists.indexWhere((l) => l.id == list.id);
    if (index < 0) return;
    lists[index] = list;
    await _persist(lists);
  }

  Future<void> delete(String id) async {
    final lists = await loadAll();
    lists.removeWhere((l) => l.id == id);
    await _persist(lists);
  }

  Future<void> _persist(List<ShoppingList> lists) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      _storageKey,
      jsonEncode(lists.map((l) => l.toJson()).toList()),
    );
  }
}
