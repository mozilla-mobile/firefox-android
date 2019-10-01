# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
Decision task
"""

from __future__ import print_function

import argparse
import itertools
import os
import taskcluster
import sys

from lib.build_config import components_version, components, generate_snapshot_timestamp
from lib.tasks import TaskBuilder


AAR_EXTENSIONS = ('.aar', '.pom', '-sources.jar')
HASH_EXTENSIONS = ('', '.sha1', '.md5')


def create_module_tasks(builder, module):
    def gradle_module_task_name(module, gradle_task_name):
        return "%s:%s" % (module, gradle_task_name)

    def craft_definition(module, variant, run_tests=True, lint_task=None):
        module_task = gradle_module_task_name(module, "assemble%s" % variant)
        subtitle = "assemble"

        if run_tests:
            module_task = "%s %s" % (module_task, gradle_module_task_name(module, "test%sDebugUnitTest" % variant))
            subtitle = "%sAndTest" % subtitle

        scheduled_lint = False
        if lint_task and variant in lint_task:
            module_task = "%s %s" % (module_task, gradle_module_task_name(module, lint_task))
            subtitle = "%sAndLintDebug" % subtitle
            scheduled_lint = True

        return builder.craft_build_task(
            module_name=module,
            gradle_tasks=module_task,
            subtitle=subtitle + variant,
            run_coverage=True,
        ), scheduled_lint

    # Takes list of variants and produces taskcluster task definitions.
    # Schedules a specified lint task at most once.
    def variants_to_definitions(variants, run_tests=True, lint_task=None):
        scheduled_lint = False
        definitions = []
        for variant in variants:
            task_definition, definition_has_lint = craft_definition(
                module, variant, run_tests=run_tests, lint_task=lint_task)
            if definition_has_lint:
                scheduled_lint = True
            lint_task = lint_task if lint_task and not definition_has_lint else None
            definitions.append(task_definition)
        return definitions, scheduled_lint

    # Module settings that describe which task definitions will be created.
    # Absence of a module override means that default definition pattern will be used for the module.
    module_overrides = {
        ":samples-browser": {
            "assembleOnly/Test": (
                ["System"],
                [
                    "GeckoBeta",
                    "GeckoNightly",
                    "GeckoRelease"
                ]
            ),
            "lintTask": "lintGeckoNightly"
        },
        ":support-test": {
            "lintTask": "lint"
        },
        ":tooling-lint": {
            "lintTask": "lint"
        }
    }

    override = module_overrides.get(module, {})
    task_definitions = []

    # If assemble/test variant lists are specified, create individual tasks as appropriate,
    # while also checking if we can sneak in a specified lint task along with a matching assemble
    # or test variant. If lint task isn't specified, it's created separately.
    if "assembleOnly/Test" in override:
        assemble_only, assemble_and_test = override["assembleOnly/Test"]

        # As an optimization, lint task will appear at most once in our final list of definitions.
        lint_task = override.get("lintTask", "lintRelease")

        # Assemble-only definitions with an optional lint task (if it matches variants).
        assemble_only_definitions, scheduled_lint_with_assemble = variants_to_definitions(
            assemble_only, run_tests=False, lint_task=lint_task)
        # Assemble-and-test definitions with an optional lint task if it wasn't present
        # above and matches variants.
        assemble_and_test_definitions, scheduled_lint_with_test = variants_to_definitions(
            assemble_and_test, run_tests=True, lint_task=lint_task if lint_task and not scheduled_lint_with_assemble else None)
        # Combine two lists of task definitions.
        task_definitions += assemble_only_definitions + assemble_and_test_definitions

        # If neither list has a lint task within it, append a separate lint task.
        # Overhead of a separate task just for linting is quite significant, but this should be a rare case.
        if not scheduled_lint_with_assemble and not scheduled_lint_with_test:
            task_definitions.append(
                builder.craft_build_task(
                    module_name=module,
                    gradle_tasks=gradle_module_task_name(module, lint_task),
                    subtitle="onlyLintRelease",
                    run_coverage=True,
                )
            )

    # If only the lint task override is specified, 'assemble' and 'test' are left as-is.
    elif "lintTask" in override:
        gradle_tasks = " ".join([
            gradle_module_task_name(module, gradle_task)
            for gradle_task in ['assemble', 'assembleAndroidTest', 'test', override["lintTask"]]
        ])
        task_definitions.append(
            builder.craft_build_task(
                module_name=module,
                gradle_tasks=gradle_tasks,
                subtitle="assembleAndTestAndCustomLintAll",
                run_coverage=True,
            )
        )

    # Default module task configuration is a single gradle task which runs assemble, test and lint for every
    # variant configured for the given module.
    else:
        gradle_tasks = " ".join([
            gradle_module_task_name(module, gradle_task)
            for gradle_task in ['assemble', 'assembleAndroidTest', 'test', 'lintRelease']
        ])
        task_definitions.append(
            builder.craft_build_task(
                module_name=module,
                gradle_tasks=gradle_tasks,
                subtitle="assembleAndTestAndLintReleaseAll",
                run_coverage=True,
            )
        )

    return task_definitions


def pr(builder, artifacts_info):
    modules = [_get_gradle_module_name(artifact_info) for artifact_info in artifacts_info]
    build_tasks = [
        task_def
        for module in modules
        for task_def in create_module_tasks(builder, module)
    ]
    other_tasks = [
        craft_function()
        for craft_function in
        (builder.craft_detekt_task, builder.craft_ktlint_task, builder.craft_compare_locales_task)
    ]

    return build_tasks + other_tasks


def push(builder, artifacts_info):
    all_tasks = pr(builder, artifacts_info)
    all_tasks.append(builder.craft_ui_tests_task())
    return all_tasks


def _get_gradle_module_name(artifact_info):
    return ':{}'.format(artifact_info['name'])


def _get_release_gradle_tasks(module_name, is_snapshot):
    gradle_tasks = ' '.join(
        '{}:{}{}'.format(
            module_name,
            gradle_task,
            '' if is_snapshot else 'Release'
        )
        for gradle_task in ('assemble', 'test', 'lint')
    )

    rename_step = '{}:renameSnapshotArtifacts'.format(module_name) if is_snapshot else ''
    return "{} {}:publish {}".format(gradle_tasks, module_name, rename_step, module_name)


def _to_release_artifact(extension, version, component, timestamp=None):
    if timestamp:
        artifact_filename = '{}-{}-{}{}'.format(component['name'], version, timestamp, extension)
    else:
        artifact_filename = '{}-{}{}'.format(component['name'], version, extension)

    return {
        'taskcluster_path': 'public/build/{}'.format(artifact_filename),
        'build_fs_path': '{}/build/maven/org/mozilla/components/{}/{}/{}'.format(
            os.path.abspath(component['path']),
            component['name'],
            version if not timestamp else version + '-SNAPSHOT',
            artifact_filename),
        'maven_destination': 'maven2/org/mozilla/components/{}/{}/{}'.format(
            component['name'],
            version if not timestamp else version + '-SNAPSHOT',
            artifact_filename,
        )
    }


def release(builder, components, is_snapshot, is_staging):
    version = components_version()

    build_tasks = {}
    wait_on_builds_tasks = {}
    sign_tasks = {}
    beetmover_tasks = {}
    other_tasks = {}
    wait_on_builds_task_id = taskcluster.slugId()

    timestamp = None
    if is_snapshot:
        timestamp = generate_snapshot_timestamp()

    for component in components:
        build_task_id = taskcluster.slugId()
        module_name = _get_gradle_module_name(component)
        build_tasks[build_task_id] = builder.craft_build_task(
            module_name=module_name,
            gradle_tasks=_get_release_gradle_tasks(module_name, is_snapshot),
            subtitle='({}{})'.format(version, '-SNAPSHOT' if is_snapshot else ''),
            run_coverage=False,
            is_snapshot=is_snapshot,
            component=component,
            artifacts=[_to_release_artifact(extension + hash_extension, version, component, timestamp)
                       for extension, hash_extension in
                       itertools.product(AAR_EXTENSIONS, HASH_EXTENSIONS)],
            timestamp=timestamp,
        )

        sign_task_id = taskcluster.slugId()
        sign_tasks[sign_task_id] = builder.craft_sign_task(
            build_task_id, wait_on_builds_task_id, [_to_release_artifact(extension, version, component, timestamp)
                            for extension in AAR_EXTENSIONS],
            component['name'], is_staging,
        )

        beetmover_build_artifacts = [_to_release_artifact(extension + hash_extension, version, component, timestamp)
                                     for extension, hash_extension in
                                     itertools.product(AAR_EXTENSIONS, HASH_EXTENSIONS)]
        beetmover_sign_artifacts = [_to_release_artifact(extension + '.asc', version, component, timestamp)
                                    for extension in AAR_EXTENSIONS]
        beetmover_tasks[taskcluster.slugId()] = builder.craft_beetmover_task(
            build_task_id, sign_task_id, beetmover_build_artifacts, beetmover_sign_artifacts,
            component['name'], is_snapshot, is_staging
        )

    wait_on_builds_tasks[wait_on_builds_task_id] = builder.craft_barrier_task(build_tasks.keys())

    if is_snapshot:     # XXX These jobs perma-fail on release
        for craft_function in (builder.craft_detekt_task, builder.craft_ktlint_task, builder.craft_compare_locales_task):
            other_tasks[taskcluster.slugId()] = craft_function()

    return (build_tasks, wait_on_builds_tasks, sign_tasks, beetmover_tasks, other_tasks)
