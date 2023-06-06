/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

// If you ever need to force a toolchain rebuild (taskcluster) then edit the following comment.
// FORCE REBUILD 2023-05-24

class DependenciesPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) = Unit
}

// Synchronized version numbers for dependencies used by (some) modules
object Versions {
    const val kotlin = "1.8.21"
    const val coroutines = "1.6.4"
    const val serialization = "1.5.1"
    const val python_envs_plugin = "0.0.31"

    const val junit = "4.13.2"
    const val robolectric = "4.10.1"
    const val mockito = "5.3.1"
    const val maven_ant_tasks = "2.1.3"
    const val jacoco = "0.8.10"

    // TO-DO: These could be kept in sync in the future
    const val mockwebserver = "3.10.0"
    const val mockwebserver_focus = "4.11.0"
    const val okhttp = "3.13.1"

    const val android_gradle_plugin = "7.4.2"

    // This has to be synced to the gradlew plugin version. See
    // http://googlesamples.github.io/android-custom-lint-rules/api-guide/example.md.html#example:samplelintcheckgithubproject/lintversion?
    const val lint = "30.4.2"
    const val detekt = "1.23.0"
    const val ktlint = "0.48.2"

    const val sentry_latest = "6.21.0"

    // zxing 3.4+ requires a minimum API of 24 or higher
    const val zxing = "3.3.3"

    const val disklrucache = "2.0.2"
    const val leakcanary = "2.11"

    // DO NOT MODIFY MANUALLY. This is auto-updated along with GeckoView.
    const val mozilla_glean = "52.7.0"

    const val material = "1.9.0"
    const val ksp = "1.0.11"

    // see https://android-developers.googleblog.com/2022/06/independent-versioning-of-Jetpack-Compose-libraries.html
    // for Jetpack Compose libraries versioning
    const val compose_version = "1.4.3"
    const val compose_compiler = "1.4.7"

    object AndroidX {
        const val activityCompose = "1.7.2"
        const val annotation = "1.6.0"
        const val appcompat = "1.6.1"
        const val autofill = "1.1.0"
        const val browser = "1.5.0"
        const val biometric = "1.1.0"
        const val cardview = "1.0.0"
        const val compose = compose_version
        const val constraintlayout = "2.1.4"
        const val constraintlayout_compose = "1.0.1"
        const val coordinatorlayout = "1.2.0"
        const val core = "1.10.1"
        const val drawerlayout = "1.2.0"
        const val fragment = "1.5.7"
        const val recyclerview = "1.3.0"
        const val test = "1.5.0"
        const val test_ext = "1.1.5"
        const val test_runner = "1.5.2"
        const val espresso = "3.5.1"
        const val room = "2.4.3"
        const val savedstate = "1.2.1"
        const val splashscreen = "1.0.1"
        const val transition = "1.4.1"
        const val paging = "2.1.2"
        const val palette = "1.0.0"
        const val preferences = "1.1.1"
        const val preferences_focus = "1.2.0"
        const val lifecycle = "2.6.1"
        const val media = "1.6.0"
        const val navigation = "2.5.3"
        const val work = "2.7.1"
        const val arch = "2.2.0"
        const val uiautomator = "2.2.0"
        const val localbroadcastmanager = "1.0.0"
        const val swiperefreshlayout = "1.1.0"
        const val data_store_preferences="1.0.0"
    }

    object Adjust {
        const val adjust = "4.33.0"
        const val install_referrer = "2.2"
    }

    object Firebase {
        const val messaging = "23.1.2"
    }

    object Google {
        const val play = "1.10.3"
    }

    object Testing {
        const val androidx_orchestrator = "1.4.2"
        const val falcon = "2.2.0"
        const val fastlane = "2.1.1"
        const val junit = "5.9.3"
    }

    object ThirdParty {
        const val osslicenses_plugin = "0.10.4"
    }
}

// Synchronized dependencies used by (some) modules
@Suppress("Unused", "MaxLineLength")
object ComponentsDependencies {
    const val kotlin_coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val kotlin_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val kotlin_json = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}"

    const val testing_junit = "junit:junit:${Versions.junit}"
    const val testing_junit_api = "org.junit.jupiter:junit-jupiter-api:${Versions.Testing.junit}"
    const val testing_junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.Testing.junit}"
    const val testing_junit_params = "org.junit.jupiter:junit-jupiter-params:${Versions.Testing.junit}"
    const val testing_robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val testing_mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val testing_mockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.mockwebserver}"
    const val testing_mockwebserver_focus = "com.squareup.okhttp3:mockwebserver:${Versions.mockwebserver_focus}"
    const val testing_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    const val testing_maven_ant_tasks = "org.apache.maven:maven-ant-tasks:${Versions.maven_ant_tasks}"
    const val testing_leakcanary = "com.squareup.leakcanary:leakcanary-android-instrumentation:${Versions.leakcanary}"
    const val testing_fastlane = "tools.fastlane:screengrab:${Versions.Testing.fastlane}"
    const val testing_falcon = "com.jraska:falcon:${Versions.Testing.falcon}"

    const val androidx_activity_compose = "androidx.activity:activity-compose:${Versions.AndroidX.activityCompose}"
    const val androidx_annotation = "androidx.annotation:annotation:${Versions.AndroidX.annotation}"
    const val androidx_appcompat = "androidx.appcompat:appcompat:${Versions.AndroidX.appcompat}"
    const val androidx_autofill = "androidx.autofill:autofill:${Versions.AndroidX.autofill}"
    const val androidx_arch_core_common = "androidx.arch.core:core-common:${Versions.AndroidX.arch}"
    const val androidx_arch_core_testing = "androidx.arch.core:core-testing:${Versions.AndroidX.arch}"
    const val androidx_biometric = "androidx.biometric:biometric:${Versions.AndroidX.biometric}"
    const val androidx_browser = "androidx.browser:browser:${Versions.AndroidX.browser}"
    const val androidx_cardview = "androidx.cardview:cardview:${Versions.AndroidX.cardview}"
    const val androidx_compose_ui = "androidx.compose.ui:ui:${Versions.AndroidX.compose}"
    const val androidx_compose_ui_graphics = "androidx.compose.ui:ui-graphics:${Versions.AndroidX.compose}"
    const val androidx_compose_ui_test = "androidx.compose.ui:ui-test-junit4:${Versions.AndroidX.compose}"
    const val androidx_compose_ui_test_manifest = "androidx.compose.ui:ui-test-manifest:${Versions.AndroidX.compose}"
    const val androidx_compose_ui_tooling = "androidx.compose.ui:ui-tooling:${Versions.AndroidX.compose}"
    const val androidx_compose_ui_tooling_preview = "androidx.compose.ui:ui-tooling-preview:${Versions.AndroidX.compose}"
    const val androidx_compose_foundation = "androidx.compose.foundation:foundation:${Versions.AndroidX.compose}"
    const val androidx_compose_material = "androidx.compose.material:material:${Versions.AndroidX.compose}"
    const val androidx_compose_runtime_livedata = "androidx.compose.runtime:runtime-livedata:${Versions.AndroidX.compose}"
    const val androidx_compose_navigation = "androidx.navigation:navigation-compose:${Versions.AndroidX.navigation}"
    const val androidx_constraintlayout = "androidx.constraintlayout:constraintlayout:${Versions.AndroidX.constraintlayout}"
    const val androidx_constraintlayout_compose =
        "androidx.constraintlayout:constraintlayout-compose:${Versions.AndroidX.constraintlayout_compose}"
    const val androidx_core = "androidx.core:core:${Versions.AndroidX.core}"
    const val androidx_core_ktx = "androidx.core:core-ktx:${Versions.AndroidX.core}"
    const val androidx_splashscreen = "androidx.core:core-splashscreen:${Versions.AndroidX.splashscreen}"
    const val androidx_coordinatorlayout = "androidx.coordinatorlayout:coordinatorlayout:${Versions.AndroidX.coordinatorlayout}"
    const val androidx_drawerlayout = "androidx.drawerlayout:drawerlayout:${Versions.AndroidX.drawerlayout}"
    const val androidx_fragment = "androidx.fragment:fragment:${Versions.AndroidX.fragment}"
    const val androidx_lifecycle_livedata = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.AndroidX.lifecycle}"
    const val androidx_lifecycle_runtime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.AndroidX.lifecycle}"
    const val androidx_lifecycle_service = "androidx.lifecycle:lifecycle-service:${Versions.AndroidX.lifecycle}"
    const val androidx_lifecycle_process = "androidx.lifecycle:lifecycle-process:${Versions.AndroidX.lifecycle}"
    const val androidx_lifecycle_compiler = "androidx.lifecycle:lifecycle-compiler:${Versions.AndroidX.lifecycle}"
    const val androidx_lifecycle_viewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.AndroidX.lifecycle}"
    const val androidx_media = "androidx.media:media:${Versions.AndroidX.media}"
    const val androidx_paging = "androidx.paging:paging-runtime:${Versions.AndroidX.paging}"
    const val androidx_palette = "androidx.palette:palette-ktx:${Versions.AndroidX.palette}"
    const val androidx_preferences = "androidx.preference:preference-ktx:${Versions.AndroidX.preferences}"
    const val androidx_preferences_focus = "androidx.preference:preference-ktx:${Versions.AndroidX.preferences_focus}"
    const val androidx_recyclerview = "androidx.recyclerview:recyclerview:${Versions.AndroidX.recyclerview}"
    const val androidx_room_runtime = "androidx.room:room-ktx:${Versions.AndroidX.room}"
    const val androidx_room_compiler = "androidx.room:room-compiler:${Versions.AndroidX.room}"
    const val androidx_room_testing = "androidx.room:room-testing:${Versions.AndroidX.room}"
    const val androidx_savedstate = "androidx.savedstate:savedstate-ktx:${Versions.AndroidX.savedstate}"
    const val androidx_test_core = "androidx.test:core-ktx:${Versions.AndroidX.test}"
    const val androidx_test_junit = "androidx.test.ext:junit-ktx:${Versions.AndroidX.test_ext}"
    const val androidx_test_orchestrator = "androidx.test:orchestrator:${Versions.Testing.androidx_orchestrator}"
    const val androidx_test_runner = "androidx.test:runner:${Versions.AndroidX.test_runner}"
    const val androidx_test_rules = "androidx.test:rules:${Versions.AndroidX.test}"
    const val androidx_test_uiautomator = "androidx.test.uiautomator:uiautomator:${Versions.AndroidX.uiautomator}"
    const val androidx_work_runtime = "androidx.work:work-runtime-ktx:${Versions.AndroidX.work}"
    const val androidx_work_testing = "androidx.work:work-testing:${Versions.AndroidX.work}"
    const val androidx_espresso_core = "androidx.test.espresso:espresso-core:${Versions.AndroidX.espresso}"
    const val androidx_espresso_idling_resource = "androidx.test.espresso:espresso-idling-resource:${Versions.AndroidX.espresso}"
    const val androidx_espresso_intents = "androidx.test.espresso:espresso-intents:${Versions.AndroidX.espresso}"
    const val androidx_espresso_web = "androidx.test.espresso:espresso-web:${Versions.AndroidX.espresso}"
    const val androidx_espresso_contrib = "androidx.test.espresso:espresso-contrib:${Versions.AndroidX.espresso}"
    const val androidx_localbroadcastmanager = "androidx.localbroadcastmanager:localbroadcastmanager:${Versions.AndroidX.localbroadcastmanager}"
    const val androidx_swiperefreshlayout = "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.AndroidX.swiperefreshlayout}"
    const val androidx_data_store_preferences = "androidx.datastore:datastore-preferences:${Versions.AndroidX.data_store_preferences}"
    const val androidx_transition = "androidx.transition:transition:${Versions.AndroidX.transition}"

    const val google_material = "com.google.android.material:material:${Versions.material}"
    const val google_play = "com.google.android.play:core:${Versions.Google.play}"

    const val plugin_ksp = "com.google.devtools.ksp:symbol-processing-gradle-plugin:${Versions.kotlin}-${Versions.ksp}"
    const val plugin_serialization = "org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:${Versions.kotlin}"

    const val leakcanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakcanary}"

    const val tools_androidgradle = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    const val tools_kotlingradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"

    const val tools_lint = "com.android.tools.lint:lint:${Versions.lint}"
    const val tools_lintapi = "com.android.tools.lint:lint-api:${Versions.lint}"
    const val tools_lintchecks = "com.android.tools.lint:lint-checks:${Versions.lint}"
    const val tools_linttests = "com.android.tools.lint:lint-tests:${Versions.lint}"

    const val tools_detekt_api = "io.gitlab.arturbosch.detekt:detekt-api:${Versions.detekt}"
    const val tools_detekt_test = "io.gitlab.arturbosch.detekt:detekt-test:${Versions.detekt}"

    val mozilla_geckoview = "org.mozilla.geckoview:${Gecko.channel.artifactName}:${Gecko.version}"
    val mozilla_fxa = "${ApplicationServicesConfig.groupId}:fxaclient:${ApplicationServicesConfig.version}"
    val mozilla_nimbus = "${ApplicationServicesConfig.groupId}:nimbus:${ApplicationServicesConfig.version}"
    const val mozilla_glean_forUnitTests = "org.mozilla.telemetry:glean-native-forUnitTests:${Versions.mozilla_glean}"
    val mozilla_sync_autofill = "${ApplicationServicesConfig.groupId}:autofill:${ApplicationServicesConfig.version}"
    val mozilla_sync_logins = "${ApplicationServicesConfig.groupId}:logins:${ApplicationServicesConfig.version}"
    val mozilla_places = "${ApplicationServicesConfig.groupId}:places:${ApplicationServicesConfig.version}"
    val mozilla_sync_manager = "${ApplicationServicesConfig.groupId}:syncmanager:${ApplicationServicesConfig.version}"
    val mozilla_push = "${ApplicationServicesConfig.groupId}:push:${ApplicationServicesConfig.version}"
    val mozilla_remote_tabs = "${ApplicationServicesConfig.groupId}:tabs:${ApplicationServicesConfig.version}"
    val mozilla_httpconfig = "${ApplicationServicesConfig.groupId}:httpconfig:${ApplicationServicesConfig.version}"
    val mozilla_full_megazord = "${ApplicationServicesConfig.groupId}:full-megazord:${ApplicationServicesConfig.version}"
    val mozilla_full_megazord_forUnitTests = "${ApplicationServicesConfig.groupId}:full-megazord-forUnitTests:${ApplicationServicesConfig.version}"

    val mozilla_errorsupport = "${ApplicationServicesConfig.groupId}:errorsupport:${ApplicationServicesConfig.version}"
    val mozilla_rustlog = "${ApplicationServicesConfig.groupId}:rustlog:${ApplicationServicesConfig.version}"
    val mozilla_sync15 = "${ApplicationServicesConfig.groupId}:sync15:${ApplicationServicesConfig.version}"

    const val adjust = "com.adjust.sdk:adjust-android:${Versions.Adjust.adjust}"
    const val install_referrer = "com.android.installreferrer:installreferrer:${Versions.Adjust.install_referrer}"

    const val thirdparty_okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val thirdparty_okhttp_urlconnection = "com.squareup.okhttp3:okhttp-urlconnection:${Versions.okhttp}"
    const val thirdparty_sentry_latest = "io.sentry:sentry-android:${Versions.sentry_latest}"
    const val thirdparty_zxing = "com.google.zxing:core:${Versions.zxing}"
    const val thirdparty_disklrucache = "com.jakewharton:disklrucache:${Versions.disklrucache}"
    const val thirdparty_osslicenses = "com.google.android.gms:oss-licenses-plugin:${Versions.ThirdParty.osslicenses_plugin}"

    const val firebase_messaging = "com.google.firebase:firebase-messaging:${Versions.Firebase.messaging}"
}
