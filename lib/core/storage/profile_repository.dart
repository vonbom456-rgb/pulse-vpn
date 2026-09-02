import 'package:hive_flutter/hive_flutter.dart';

class StoredProfile {
  const StoredProfile({required this.id, required this.name, required this.uri});
  final String id;
  final String name;
  final String uri;
}

abstract interface class ProfileRepository {
  Future<List<StoredProfile>> readAll();
  Future<void> save(StoredProfile profile);
  Future<void> delete(String id);
}

class HiveProfileRepository implements ProfileRepository {
  static const _boxName = 'vpn_profiles';

  Future<Box<dynamic>> get _box => Hive.openBox<dynamic>(_boxName);

  @override
  Future<List<StoredProfile>> readAll() async {
    final box = await _box;
    return box.values.map((value) {
      final data = Map<String, Object?>.from(value as Map);
      return StoredProfile(
        id: data['id']! as String,
        name: data['name']! as String,
        uri: data['uri']! as String,
      );
    }).toList(growable: false);
  }

  @override
  Future<void> save(StoredProfile profile) async {
    final box = await _box;
    await box.put(profile.id, {
      'id': profile.id,
      'name': profile.name,
      'uri': profile.uri,
    });
  }

  @override
  Future<void> delete(String id) async => (await _box).delete(id);
}

