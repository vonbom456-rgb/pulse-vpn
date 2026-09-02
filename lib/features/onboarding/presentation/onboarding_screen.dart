import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _controller = PageController();
  var _page = 0;

  static const _slides = [
    ('Живой интернет', 'Маршрут адаптируется к сети и держит скорость в ритме.', Icons.graphic_eq_rounded),
    ('Приватность без шума', 'Современное шифрование работает тихо — вы просто продолжаете жить.', Icons.blur_on_rounded),
    ('Один импульс', 'Выберите локацию и подключитесь одним касанием.', Icons.touch_app_outlined),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(onPressed: () => context.go('/'), child: const Text('Пропустить')),
          ),
          Expanded(
            child: PageView.builder(
              controller: _controller,
              itemCount: _slides.length,
              onPageChanged: (page) {
                HapticFeedback.selectionClick();
                setState(() => _page = page);
              },
              itemBuilder: (context, index) => AnimatedBuilder(
                animation: _controller,
                builder: (context, child) {
                  final current = _controller.hasClients && _controller.position.haveDimensions
                      ? (_controller.page ?? 0)
                      : 0.0;
                  final delta = index - current;
                  return Transform.translate(offset: Offset(delta * -28, 0), child: child);
                },
                child: _Slide(data: _slides[index]),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(PulseSpace.page),
            child: Column(children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(_slides.length, (index) => AnimatedContainer(
                  duration: PulseMotion.standard,
                  margin: const EdgeInsets.symmetric(horizontal: PulseSpace.xxs),
                  width: index == _page ? 28 : 7,
                  height: 7,
                  decoration: BoxDecoration(
                    gradient: index == _page ? PulseColors.pulseGradient : null,
                    color: index == _page ? null : PulseColors.textSecondary.withValues(alpha: .28),
                    borderRadius: BorderRadius.circular(PulseRadius.pill),
                  ),
                )),
              ),
              const SizedBox(height: PulseSpace.lg),
              SizedBox(
                width: double.infinity,
                height: 58,
                child: DecoratedBox(
                  decoration: BoxDecoration(gradient: PulseColors.pulseGradient, borderRadius: BorderRadius.circular(PulseRadius.pill)),
                  child: TextButton(
                    onPressed: () {
                      if (_page == _slides.length - 1) {
                        context.go('/');
                      } else {
                        _controller.nextPage(duration: PulseMotion.standard, curve: PulseMotion.routeCurve);
                      }
                    },
                    child: Text(_page == _slides.length - 1 ? 'Начать' : 'Дальше', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
                  ),
                ),
              ),
            ]),
          ),
        ]),
      ),
    );
  }
}

class _Slide extends StatelessWidget {
  const _Slide({required this.data});
  final (String, String, IconData) data;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: PulseSpace.xl),
      child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        Container(
          width: 220,
          height: 220,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: RadialGradient(colors: [PulseColors.indigo.withValues(alpha: .28), Colors.transparent]),
          ),
          child: ShaderMask(
            shaderCallback: PulseColors.pulseGradient.createShader,
            child: Icon(data.$3, size: 92, color: Colors.white),
          ),
        ),
        const SizedBox(height: PulseSpace.xl),
        Text(data.$1, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineLarge),
        const SizedBox(height: PulseSpace.md),
        Text(data.$2, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyLarge?.copyWith(color: PulseColors.textSecondary)),
      ]),
    );
  }
}

