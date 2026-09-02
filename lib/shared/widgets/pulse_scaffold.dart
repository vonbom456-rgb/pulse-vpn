import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';

class PulseScaffold extends StatelessWidget {
  const PulseScaffold({
    required this.title,
    required this.child,
    super.key,
    this.actions,
  });

  final String title;
  final Widget child;
  final List<Widget>? actions;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        leading: IconButton(
          onPressed: context.pop,
          icon: const Icon(Icons.arrow_back_rounded),
        ),
        title: Text(title),
        actions: actions,
      ),
      body: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            PulseSpace.page,
            PulseSpace.sm,
            PulseSpace.page,
            PulseSpace.page,
          ),
          child: child,
        ),
      ),
    );
  }
}

