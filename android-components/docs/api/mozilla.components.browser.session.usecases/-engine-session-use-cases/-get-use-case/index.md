[android-components](../../../index.md) / [mozilla.components.browser.session.usecases](../../index.md) / [EngineSessionUseCases](../index.md) / [GetUseCase](./index.md)

# GetUseCase

`class GetUseCase` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/session/src/main/java/mozilla/components/browser/session/usecases/EngineSessionUseCases.kt#L51)

Use case for getting an [EngineSession](../../../mozilla.components.concept.engine/-engine-session/index.md) for a tab.

### Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | `operator fun invoke(tabId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`EngineSession`](../../../mozilla.components.concept.engine/-engine-session/index.md)`?`<br>Gets the linked engine session for a tab (if it exists). |
