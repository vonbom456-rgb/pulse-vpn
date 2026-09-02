import 'package:flutter_sing_box/flutter_sing_box.dart';
import 'package:pulse_vpn/core/vpn_engine/sing_box_runtime.dart';

class VpnRoute {
  const VpnRoute({
    required this.groupTag,
    required this.tag,
    required this.type,
    required this.delayMs,
    required this.isSelected,
  });

  final String groupTag;
  final String tag;
  final String type;
  final int delayMs;
  final bool isSelected;
}

class VpnRouteGroup {
  const VpnRouteGroup({
    required this.tag,
    required this.routes,
  });

  final String tag;
  final List<VpnRoute> routes;
}

class VpnRouteManager {
  VpnRouteManager(this._runtime);
  final SingBoxRuntime _runtime;

  Stream<List<VpnRouteGroup>> watchGroups() async* {
    await _runtime.ensureInitialized();
    yield* _runtime.client.groupStream.map(_mapGroups);
  }

  Future<void> select(VpnRoute route) async {
    await _runtime.ensureInitialized();
    await _runtime.client.selectOutbound(
      groupTag: route.groupTag,
      outboundTag: route.tag,
    );
  }

  Future<void> test(String groupTag) async {
    await _runtime.ensureInitialized();
    await _runtime.client.urlTest(groupTag: groupTag);
  }

  List<VpnRouteGroup> _mapGroups(List<ClientGroup> groups) {
    return groups
        .where((group) => group.selectable && group.items?.isNotEmpty == true)
        .map(
          (group) => VpnRouteGroup(
            tag: group.tag,
            routes: group.items!
                .map(
                  (item) => VpnRoute(
                    groupTag: group.tag,
                    tag: item.tag,
                    type: item.type,
                    delayMs: item.urlTestDelay,
                    isSelected: item.tag == group.selected,
                  ),
                )
                .toList(growable: false),
          ),
        )
        .toList(growable: false);
  }
}
