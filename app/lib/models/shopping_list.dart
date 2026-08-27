import 'package:uuid/uuid.dart';

/// Categorías fijas permitidas en las listas de la compra, en orden de
/// visualización. Deben coincidir con el backend.
const List<String> kShoppingCategories = [
  'Fruta y verdura',
  'Proteínas',
  'Lácteos y alternativas',
  'Despensa',
  'Panadería y cereales',
  'Congelados',
  'Bebidas',
  'Otros',
];

enum ShoppingListItemType { keep, replace, add }

class ShoppingListItem {
  ShoppingListItem({
    String? id,
    required this.name,
    required this.category,
    required this.type,
    this.replaces,
    this.reason,
    this.checked = false,
  }) : id = id ?? const Uuid().v4();

  final String id;
  final String name;

  /// Nombre de categoría fija (ver [kShoppingCategories]).
  final String category;
  final ShoppingListItemType type;
  final String? replaces;
  final String? reason;
  final bool checked;

  ShoppingListItem copyWith({
    String? name,
    String? category,
    ShoppingListItemType? type,
    String? replaces,
    String? reason,
    bool? checked,
  }) {
    return ShoppingListItem(
      id: id,
      name: name ?? this.name,
      category: category ?? this.category,
      type: type ?? this.type,
      replaces: replaces ?? this.replaces,
      reason: reason ?? this.reason,
      checked: checked ?? this.checked,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'category': category,
        'type': type.name,
        'replaces': replaces,
        'reason': reason,
        'checked': checked,
      };

  factory ShoppingListItem.fromJson(Map<String, dynamic> json) {
    return ShoppingListItem(
      id: json['id']?.toString(),
      name: json['name']?.toString() ?? '',
      category: json['category']?.toString() ?? 'Otros',
      type: ShoppingListItemType.values.asNameMap()[json['type']?.toString()] ??
          ShoppingListItemType.keep,
      replaces: json['replaces']?.toString(),
      reason: json['reason']?.toString(),
      checked: json['checked'] == true,
    );
  }
}

class ShoppingList {
  ShoppingList({
    String? id,
    DateTime? createdAt,
    required this.items,
  })  : id = id ?? const Uuid().v4(),
        createdAt = createdAt ?? DateTime.now();

  final String id;
  final DateTime createdAt;
  final List<ShoppingListItem> items;

  int get checkedCount => items.where((i) => i.checked).length;

  /// Artículos agrupados por categoría fija, en orden. Omite categorías vacías.
  Map<String, List<ShoppingListItem>> get itemsByCategory {
    final map = <String, List<ShoppingListItem>>{};
    for (final item in items) {
      map.putIfAbsent(item.category, () => []).add(item);
    }
    final ordered = <String, List<ShoppingListItem>>{};
    for (final category in kShoppingCategories) {
      final group = map.remove(category);
      if (group != null && group.isNotEmpty) ordered[category] = group;
    }
    // Categorías desconocidas (p. ej. de versiones antiguas) van al final.
    map.forEach((category, group) {
      if (group.isNotEmpty) ordered[category] = group;
    });
    return ordered;
  }

  ShoppingList copyWithItems(List<ShoppingListItem> items) =>
      ShoppingList(id: id, createdAt: createdAt, items: items);

  Map<String, dynamic> toJson() => {
        'id': id,
        'createdAt': createdAt.toIso8601String(),
        'items': items.map((i) => i.toJson()).toList(),
      };

  factory ShoppingList.fromJson(Map<String, dynamic> json) {
    return ShoppingList(
      id: json['id']?.toString(),
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
      items: (json['items'] as List<dynamic>? ?? [])
          .whereType<Map<String, dynamic>>()
          .map(ShoppingListItem.fromJson)
          .toList(),
    );
  }
}
