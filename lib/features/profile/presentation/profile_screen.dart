import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_profile_manager.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profiles = ref.watch(vpnProfilesProvider);
    return PulseScaffold(
      title: 'VPN-профили',
      child: ListView(children: [
        _BrandCard(profileCount: profiles.asData?.value.length ?? 0),
        const SizedBox(height: PulseSpace.lg),
        profiles.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, _) => GlassCard(
            child: Text('Не удалось прочитать профили: $error'),
          ),
          data: (items) => items.isEmpty
              ? _EmptyProfiles(onAdd: () => context.push('/import'))
              : Column(
                  children: [
                    for (final profile in items) ...[
                      _ProfileCard(
                        profile: profile,
                        onSelect: () async {
                          await ref
                              .read(vpnProfilesProvider.notifier)
                              .select(profile.id);
                          await HapticFeedback.selectionClick();
                        },
                        onDelete: () => _delete(context, ref, profile),
                      ),
                      const SizedBox(height: PulseSpace.sm),
                    ],
                  ],
                ),
        ),
        const SizedBox(height: PulseSpace.md),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: () => context.push('/import'),
            icon: const Icon(Icons.add_rounded),
            label: const Text('Добавить подписку'),
          ),
        ),
        const SizedBox(height: PulseSpace.xl),
        Center(
          child: Text(
            'Pulse VPN · 0.2.0 · sing-box',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
      ]),
    );
  }

  Future<void> _delete(
    BuildContext context,
    WidgetRef ref,
    VpnProfile profile,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Удалить профиль?'),
        content: Text('«${profile.name}» и локальная конфигурация будут удалены.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Удалить'),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await ref.read(vpnProfilesProvider.notifier).delete(profile.id);
    }
  }
}

class _BrandCard extends StatelessWidget {
  const _BrandCard({required this.profileCount});
  final int profileCount;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(PulseSpace.lg),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF29234F), Color(0xFF103E42)],
          ),
          borderRadius: BorderRadius.circular(PulseRadius.lg),
          boxShadow: PulseShadows.glow(PulseColors.indigo),
        ),
        child: Row(children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(PulseRadius.md),
            child: Image.asset(
              'assets/images/pulse_app_icon.png',
              width: 72,
              height: 72,
            ),
          ),
          const SizedBox(width: PulseSpace.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('PULSE CORE', style: Theme.of(context).textTheme.labelLarge),
                const SizedBox(height: PulseSpace.xs),
                Text(
                  '$profileCount ${_profileWord(profileCount)}',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                Text('Хранятся локально', style: Theme.of(context).textTheme.bodyMedium),
              ],
            ),
          ),
        ]),
      );

  static String _profileWord(int count) {
    if (count == 1) return 'профиль';
    if (count >= 2 && count <= 4) return 'профиля';
    return 'профилей';
  }
}

class _ProfileCard extends StatelessWidget {
  const _ProfileCard({
    required this.profile,
    required this.onSelect,
    required this.onDelete,
  });

  final VpnProfile profile;
  final VoidCallback onSelect;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) => GlassCard(
        onTap: onSelect,
        child: Row(children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: (profile.isSelected ? PulseColors.success : PulseColors.indigo)
                  .withValues(alpha: .14),
              borderRadius: BorderRadius.circular(PulseRadius.sm),
            ),
            child: Icon(
              profile.isSelected ? Icons.graphic_eq_rounded : Icons.route_rounded,
              color: profile.isSelected ? PulseColors.success : PulseColors.teal,
            ),
          ),
          const SizedBox(width: PulseSpace.sm),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  profile.name,
                  style: Theme.of(context).textTheme.titleMedium,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  '${profile.outboundsCount} маршрутов${_expiry(profile.expiresAt)}',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
          if (profile.isSelected)
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: PulseSpace.sm,
                vertical: PulseSpace.xxs,
              ),
              decoration: BoxDecoration(
                color: PulseColors.success.withValues(alpha: .12),
                borderRadius: BorderRadius.circular(PulseRadius.pill),
              ),
              child: const Text(
                'АКТИВЕН',
                style: TextStyle(
                  color: PulseColors.success,
                  fontSize: 10,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          IconButton(
            onPressed: onDelete,
            icon: const Icon(Icons.more_vert_rounded),
          ),
        ]),
      );

  String _expiry(DateTime? date) {
    if (date == null) return '';
    final local = date.toLocal();
    return ' · до ${local.day.toString().padLeft(2, '0')}.${local.month.toString().padLeft(2, '0')}.${local.year}';
  }
}

class _EmptyProfiles extends StatelessWidget {
  const _EmptyProfiles({required this.onAdd});
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) => GlassCard(
        onTap: onAdd,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: PulseSpace.lg),
          child: Column(children: [
            const Icon(Icons.add_link_rounded, color: PulseColors.teal, size: 36),
            const SizedBox(height: PulseSpace.sm),
            Text('Профилей пока нет', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: PulseSpace.xs),
            Text(
              'Добавьте подписку, чтобы включить VPN',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ]),
        ),
      );
}
