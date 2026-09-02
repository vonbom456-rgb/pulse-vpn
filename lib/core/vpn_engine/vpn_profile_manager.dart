import 'dart:io';

import 'package:flutter_sing_box/flutter_sing_box.dart';
import 'package:pulse_vpn/core/vpn_engine/sing_box_runtime.dart';

class VpnProfile {
  const VpnProfile({
    required this.id,
    required this.name,
    required this.outboundsCount,
    required this.isSelected,
    this.subscriptionUrl,
    this.expiresAt,
  });

  final int id;
  final String name;
  final int outboundsCount;
  final bool isSelected;
  final String? subscriptionUrl;
  final DateTime? expiresAt;
}

abstract interface class VpnProfileManager {
  Future<List<VpnProfile>> load();
  Future<VpnProfile> import(String source, {String? name});
  Future<void> select(int id);
  Future<void> delete(int id);
}

class SingBoxProfileManager implements VpnProfileManager {
  SingBoxProfileManager(this._runtime);

  final SingBoxRuntime _runtime;

  @override
  Future<List<VpnProfile>> load() async {
    await _runtime.ensureInitialized();
    final storage = ProfileStorage();
    final selectedId = storage.getSelectedProfile()?.id;
    return storage
        .getProfiles()
        .map((profile) => _toVpnProfile(profile, selectedId))
        .toList(growable: false);
  }

  @override
  Future<VpnProfile> import(String source, {String? name}) async {
    await _runtime.ensureInitialized();
    final value = source.trim();
    if (value.isEmpty) {
      throw const FormatException('Вставьте ссылку или конфигурацию');
    }

    final uri = Uri.tryParse(value);
    final isRemote = uri != null && (uri.scheme == 'https' || uri.scheme == 'http');
    final importUri = isRemote ? uri : await _writeLocalImport(value);
    final profile = await ProfileService().importProfile(
      subscribeLink: importUri,
      name: name,
    );
    ProfileStorage().setSelectedProfile(profile.id);
    return _toVpnProfile(profile, profile.id);
  }

  @override
  Future<void> select(int id) async {
    await _runtime.ensureInitialized();
    final storage = ProfileStorage();
    if (storage.getProfile(id) == null) {
      throw StateError('Профиль не найден');
    }
    storage.setSelectedProfile(id);
  }

  @override
  Future<void> delete(int id) async {
    await _runtime.ensureInitialized();
    ProfileStorage().deleteProfile(id);
  }

  Future<Uri> _writeLocalImport(String content) async {
    final directory = await ProfileStorage().getStorageDirectory();
    final file = File.fromUri(
      directory.uri.resolve(
        'pulse_import_${DateTime.now().microsecondsSinceEpoch}.txt',
      ),
    );
    await file.writeAsString(content, flush: true);
    return file.uri;
  }

  VpnProfile _toVpnProfile(Profile profile, int? selectedId) {
    final expires = profile.userInfo?.expire;
    return VpnProfile(
      id: profile.id,
      name: profile.name,
      outboundsCount: profile.outboundsCount ?? 0,
      isSelected: profile.id == selectedId,
      subscriptionUrl: profile.typed.subscribeUrl,
      expiresAt: expires == null
          ? null
          : DateTime.fromMillisecondsSinceEpoch(expires * 1000),
    );
  }
}
