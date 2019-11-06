[android-components](../../index.md) / [mozilla.components.service.fxa](../index.md) / [SyncEngine](index.md) / [Passwords](./-passwords.md)

# Passwords

`object Passwords : `[`SyncEngine`](index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/firefox-accounts/src/main/java/mozilla/components/service/fxa/Config.kt#L73)

A 'logins/passwords' engine. When using this engine, make sure to set an encryption key used to unlock the store
as 'passwords_key' via [SecureAbove22Preferences](../../mozilla.components.lib.dataprotect/-secure-above22-preferences/index.md).

### Inherited Properties

| Name | Summary |
|---|---|
| [nativeName](native-name.md) | `val nativeName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Internally, Rust SyncManager represents engines as strings. Forward-compatibility with new engines is one of the reasons for this. E.g. during any sync, an engine may appear that we do not know about. At the public API level, we expose a concrete [SyncEngine](index.md) type to allow for more robust integrations. We do not expose "unknown" engines via our public API, but do handle them internally (by persisting their enabled/disabled status). |
