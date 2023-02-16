# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.


from taskgraph.transforms.base import TransformSequence

from ..build_config import get_path, get_upstream_deps_for_all_gradle_projects
from ..gradle import get_gradle_project

transforms = TransformSequence()


@transforms.add
def set_treeherder_config(config, tasks):
    for task in tasks:
        treeherder = task.setdefault("treeherder", {})
        if not treeherder.get("symbol"):
            gradle_project = get_gradle_project(task)
            treeherder_group = task.get("attributes", {}).get("treeherder-group", gradle_project)
            treeherder["symbol"] = f"{treeherder_group}(egd)"

        yield task





@transforms.add
def extend_resources(config, tasks):
    deps_per_gradle_project = get_upstream_deps_for_all_gradle_projects()

    for task in tasks:
        run = task.setdefault("run", {})
        resources = run.setdefault("resources", [])

        gradle_project = get_gradle_project(task)
        if gradle_project:
            dependencies = deps_per_gradle_project[gradle_project]
            gradle_project_and_deps = [gradle_project] + dependencies

            resources.extend([
                path
                for gradle_project in gradle_project_and_deps
                for path in _get_build_gradle_paths(gradle_project)
            ])

        run["resources"] = sorted(list(set(resources)))

        yield task


def _get_build_gradle_paths(gradle_project):
    project_dir = _get_gradle_project_dir(gradle_project)

    if gradle_project in ("focus", "fenix"):
        project_subdir = "app"
    elif gradle_project == "service-telemetry":
        project_subdir = "service-telemetry"
    else:
        project_subdir = get_path(gradle_project)

    return [
        "android-components/plugins/dependencies/build.gradle",
        "android-components/plugins/publicsuffixlist/build.gradle",
        f"{project_dir}/build.gradle",
        f"{project_dir}/buildSrc/build.gradle",
        f"{project_dir}/{project_subdir}/build.gradle",
    ]


def _get_gradle_project_dir(gradle_project):
    if gradle_project in ("focus", "service-telemetry"):
        project_dir = "focus-android"
    elif gradle_project == "fenix":
        project_dir = "fenix"
    else:
        project_dir = "android-components"
    return project_dir


@transforms.add
def set_command_arguments(config, tasks):
    for task in tasks:
        run = task.setdefault("run", {})
        arguments = run.setdefault("arguments", [])
        if not arguments:
            gradle_project = get_gradle_project(task)
            project_dir = _get_gradle_project_dir(gradle_project)

            arguments.append(project_dir)
            gradle_task_template = "{gradle_task_name}" if gradle_project in ("focus", "fenix") else "{gradle_project}:{gradle_task_name}"
            arguments.extend([
                gradle_task_template.format(
                    gradle_project=gradle_project,
                    gradle_task_name=gradle_task_name,
                )
                for gradle_task_name in _get_gradle_task_names(gradle_project)
            ])

        yield task


def _get_gradle_task_names(gradle_project):
    gradle_tasks_name = []
    if gradle_project == "focus":
        gradle_tasks_name.extend(["assembleFocusDebug", "assembleAndroidTest", "testFocusDebugUnitTest", "lint"])
    elif gradle_project == "fenix":
        gradle_tasks_name.extend(["assemble", "assembleAndroidTest", "testClasses", "test", "lint"])
    else:
        lint_task_name = "lint" if gradle_project in ("tooling-lint", "samples-browser") else "lintRelease"
        gradle_tasks_name.extend(["assemble", "assembleAndroidTest", "test", lint_task_name])
    return tuple(gradle_tasks_name)
