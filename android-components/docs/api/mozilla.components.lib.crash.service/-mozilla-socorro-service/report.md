[android-components](../../index.md) / [mozilla.components.lib.crash.service](../index.md) / [MozillaSocorroService](index.md) / [report](./report.md)

# report

`fun report(crash: `[`UncaughtExceptionCrash`](../../mozilla.components.lib.crash/-crash/-uncaught-exception-crash/index.md)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/crash/src/main/java/mozilla/components/lib/crash/service/MozillaSocorroService.kt#L96)

Overrides [CrashReporterService.report](../-crash-reporter-service/report.md)

Submits a crash report for this [Crash.UncaughtExceptionCrash](../../mozilla.components.lib.crash/-crash/-uncaught-exception-crash/index.md).

**Return**
Unique identifier that can be used by/with this crash reporter service to find this
crash - or null if no identifier can be provided.

`fun report(crash: `[`NativeCodeCrash`](../../mozilla.components.lib.crash/-crash/-native-code-crash/index.md)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/crash/src/main/java/mozilla/components/lib/crash/service/MozillaSocorroService.kt#L106)

Overrides [CrashReporterService.report](../-crash-reporter-service/report.md)

Submits a crash report for this [Crash.NativeCodeCrash](../../mozilla.components.lib.crash/-crash/-native-code-crash/index.md).

**Return**
Unique identifier that can be used by/with this crash reporter service to find this
crash - or null if no identifier can be provided.

`fun report(throwable: `[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/crash/src/main/java/mozilla/components/lib/crash/service/MozillaSocorroService.kt#L116)

Overrides [CrashReporterService.report](../-crash-reporter-service/report.md)

Submits a caught exception report for this [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html).

**Return**
Unique identifier that can be used by/with this crash reporter service to find this
crash - or null if no identifier can be provided.

