import 'package:eatsmart_ai/data/shopping_list_repository.dart';
import 'package:eatsmart_ai/models/shopping_list.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late ShoppingListRepository repository;

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    repository = ShoppingListRepository();
  });

  ShoppingList sampleList({String? id, DateTime? createdAt}) => ShoppingList(
        id: id,
        createdAt: createdAt,
        items: [
          ShoppingListItem(
            id: 'item-1',
            name: 'Manzanas',
            category: 'Fruta y verdura',
            type: ShoppingListItemType.keep,
          ),
          ShoppingListItem(
            id: 'item-2',
            name: 'Pan integral',
            category: 'Panadería y cereales',
            type: ShoppingListItemType.replace,
            replaces: 'Pan blanco',
            reason: 'Más fibra',
            checked: true,
          ),
        ],
      );

  group('ShoppingListRepository', () {
    test('loadAll devuelve lista vacía sin datos previos', () async {
      expect(await repository.loadAll(), isEmpty);
    });

    test('save y loadAll hacen round-trip completo', () async {
      final list = sampleList(id: 'lista-1');
      await repository.save(list);

      final loaded = await repository.loadAll();
      expect(loaded, hasLength(1));
      expect(loaded.first.id, 'lista-1');
      expect(loaded.first.items, hasLength(2));
      final item = loaded.first.items
          .firstWhere((i) => i.id == 'item-2');
      expect(item.type, ShoppingListItemType.replace);
      expect(item.replaces, 'Pan blanco');
      expect(item.reason, 'Más fibra');
      expect(item.checked, isTrue);
    });

    test('loadAll ordena por fecha descendente', () async {
      await repository
          .save(sampleList(id: 'vieja', createdAt: DateTime(2024, 1, 1)));
      await repository
          .save(sampleList(id: 'nueva', createdAt: DateTime(2025, 1, 1)));

      final loaded = await repository.loadAll();
      expect(loaded.map((l) => l.id), ['nueva', 'vieja']);
    });

    test('update modifica una lista existente', () async {
      final list = sampleList(id: 'lista-1');
      await repository.save(list);
      final updated = list.copyWithItems([
        list.items.first.copyWith(checked: true),
      ]);
      await repository.update(updated);

      final loaded = await repository.loadAll();
      expect(loaded, hasLength(1));
      expect(loaded.first.items, hasLength(1));
      expect(loaded.first.items.first.checked, isTrue);
    });

    test('delete elimina la lista indicada', () async {
      await repository.save(sampleList(id: 'lista-1'));
      await repository.save(sampleList(id: 'lista-2'));
      await repository.delete('lista-1');

      final loaded = await repository.loadAll();
      expect(loaded.map((l) => l.id), ['lista-2']);
    });

    test('loadAll tolera JSON corrupto', () async {
      SharedPreferences.setMockInitialValues(
          {'shopping_lists_v1': '{no es json'});
      expect(await repository.loadAll(), isEmpty);
    });
  });

  group('ShoppingList modelos', () {
    test('itemsByCategory agrupa en orden de categorías fijas', () {
      final list = ShoppingList(items: [
        ShoppingListItem(
            name: 'Arroz',
            category: 'Despensa',
            type: ShoppingListItemType.add),
        ShoppingListItem(
            name: 'Manzanas',
            category: 'Fruta y verdura',
            type: ShoppingListItemType.keep),
      ]);

      expect(list.itemsByCategory.keys.toList(),
          ['Fruta y verdura', 'Despensa']);
    });

    test('item sin id genera UUID automáticamente', () {
      final item = ShoppingListItem(
          name: 'Leche', category: 'Lácteos y alternativas',
          type: ShoppingListItemType.keep);
      expect(item.id, isNotEmpty);
    });

    test('fromJson usa valores seguros ante campos desconocidos', () {
      final item = ShoppingListItem.fromJson({
        'name': 'Algo',
        'type': 'desconocido',
      });
      expect(item.category, 'Otros');
      expect(item.type, ShoppingListItemType.keep);
      expect(item.checked, isFalse);
    });
  });
}
