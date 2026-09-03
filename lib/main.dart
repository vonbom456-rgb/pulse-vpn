import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mmkv/mmkv.dart';
import 'package:pulse_vpn/app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await MMKV.initialize();
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(const ProviderScope(child: PulseApp()));
}
