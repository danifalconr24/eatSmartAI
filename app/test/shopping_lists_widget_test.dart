import 'package:eatsmart_ai/data/shopping_list_repository.dart';
import 'package:eatsmart_ai/models/shopping_list.dart';
import 'package:eatsmart_ai/screens/shopping_list_detail_screen.dart';
import 'package:eatsmart_ai/screens/shopping_lists_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  ShoppingList sampleList({String id = 'lista-1'}) => ShoppingList(
        id: id,
        createdAt: DateTime(2025, 3, 10, 12, 30),
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
          ),
        ],
      );

  group('ShoppingListsScreen', () {
    testWidgets('muestra estado vacío sin listas', (tester) async {
      SharedPreferences.setMockInitialValues({});
      await tester.pumpWidget(const MaterialApp(home: ShoppingListsScreen()));
      await tester.pumpAndSettle();

      expect(find.textContaining('Aún no tienes listas'), findsOneWidget);
    });

    testWidgets('muestra listas guardadas con resumen', (tester) async {
      SharedPreferences.setMockInitialValues({});
      await ShoppingListRepository().save(sampleList());
      await tester.pumpWidget(const MaterialApp(home: ShoppingListsScreen()));
      await tester.pumpAndSettle();

      expect(find.textContaining('2 artículos · 0 comprados'), findsOneWidget);
      expect(find.text('10/03/2025 12:30'), findsOneWidget);
    });

    testWidgets('borrar lista pide confirmación y la elimina', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final repository = ShoppingListRepository();
      await repository.save(sampleList());
      await tester.pumpWidget(const MaterialApp(home: ShoppingListsScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.delete_outline));
      await tester.pumpAndSettle();
      expect(find.text('Borrar lista'), findsOneWidget);

      await tester.tap(find.widgetWithText(FilledButton, 'Borrar'));
      await tester.pumpAndSettle();

      expect(find.textContaining('Aún no tienes listas'), findsOneWidget);
      expect(await repository.loadAll(), isEmpty);
    });
  });

  group('ShoppingListDetailScreen', () {
    testWidgets('agrupa artículos por categoría y muestra sustitución',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      await ShoppingListRepository().save(sampleList());
      await tester.pumpWidget(const MaterialApp(
          home: ShoppingListDetailScreen(listId: 'lista-1')));
      await tester.pumpAndSettle();

      expect(find.text('Fruta y verdura'), findsOneWidget);
      expect(find.text('Panadería y cereales'), findsOneWidget);
      expect(find.text('Manzanas'), findsOneWidget);
      expect(find.textContaining('Sustituye a Pan blanco'), findsOneWidget);
    });

    testWidgets('checkbox marca artículo como comprado y persiste',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      final repository = ShoppingListRepository();
      await repository.save(sampleList());
      await tester.pumpWidget(const MaterialApp(
          home: ShoppingListDetailScreen(listId: 'lista-1')));
      await tester.pumpAndSettle();

      final checkboxes = find.byType(Checkbox);
      await tester.tap(checkboxes.first);
      await tester.pumpAndSettle();

      final loaded = await repository.loadAll();
      expect(loaded.first.items.firstWhere((i) => i.id == 'item-1').checked,
          isTrue);
    });

    testWidgets('edición de nombre persiste el cambio', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final repository = ShoppingListRepository();
      await repository.save(sampleList());
      await tester.pumpWidget(const MaterialApp(
          home: ShoppingListDetailScreen(listId: 'lista-1')));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.edit_outlined).first);
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField), 'Peras');
      await tester.tap(find.widgetWithText(FilledButton, 'Guardar'));
      await tester.pumpAndSettle();

      final loaded = await repository.loadAll();
      expect(loaded.first.items.firstWhere((i) => i.id == 'item-1').name,
          'Peras');
    });

    testWidgets('descartar con swipe elimina el artículo', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final repository = ShoppingListRepository();
      await repository.save(sampleList());
      await tester.pumpWidget(const MaterialApp(
          home: ShoppingListDetailScreen(listId: 'lista-1')));
      await tester.pumpAndSettle();

      await tester.fling(
          find.text('Manzanas'), const Offset(-400, 0), 1000);
      await tester.pumpAndSettle();

      final loaded = await repository.loadAll();
      expect(loaded.first.items, hasLength(1));
      expect(find.text('Manzanas'), findsNothing);
    });
  });
}
