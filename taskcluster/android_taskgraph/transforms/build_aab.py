# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""
Apply some defaults and minor modifications to the jobs defined in the
build-bundle (build AAB) kind.
"""

from android_taskgraph.build_config import get_variant

from taskgraph.transforms.base import TransformSequence
from taskgraph.util.schema import resolve_keyed_by


transforms = TransformSequence()


@transforms.add
def add_variant_config(config, tasks):
    for task in tasks:
        attributes = task.setdefault("attributes", {})
        if not attributes.get("build-type"):
            attributes["build-type"] = task["name"]
        yield task


@transforms.add
def resolve_keys(config, tasks):
    for task in tasks:
        for field in ("optimization",):
            resolve_keyed_by(
                task,
                field,
                item_name=task["name"],
                **{
                    "tasks-for": config.params["tasks_for"],
                },
            )

        yield task


@transforms.add
def build_pre_gradle_command(config, tasks):
    for task in tasks:
        source_project_name = task["source-project-name"]
        pre_gradlew = task["run"].setdefault("pre-gradlew", [])
        pre_gradlew.append(["cd", source_project_name])

        yield task


@transforms.add
def build_gradle_command(config, tasks):
    for task in tasks:
        gradle_build_type = task["run"]["gradle-build-type"]
        gradle_build_name = task["run"]["gradle-build-name"]
        variant_config = get_variant(gradle_build_type, gradle_build_name)
        variant_name = variant_config["name"][0].upper() + variant_config["name"][1:]
        gradle_command = [
            "clean",
            f"bundle{variant_name}",
        ]

        task["run"]["gradlew"] = gradle_command

        yield task


@transforms.add
def add_artifacts(config, tasks):
    for task in tasks:
        variant = task["attributes"]["build-type"]
        artifacts = task.setdefault("worker", {}).setdefault("artifacts", [])
        gradle_build_type = task["run"].pop("gradle-build-type")
        gradle_build_name = task["run"].pop("gradle-build-name")
        gradle_build = task["run"].pop("gradle-build")
        source_project_name = task.pop("source-project-name")
        variant_config = get_variant(gradle_build_type, gradle_build_name)
        variant_name = variant_config["name"]
        if "aab-artifact-template" in task:
            artifact_template = task.pop("aab-artifact-template")
            artifacts.append(
                {
                    "type": artifact_template["type"],
                    "name": artifact_template["name"],
                    "path": artifact_template["path"].format(
                        gradle_build_type=gradle_build_type,
                        gradle_build=gradle_build,
                        source_project_name=source_project_name,
                        variant_name=variant_name,
                    ),
                }
            )
            task["attributes"]["aab"] = artifact_template["name"]

        yield task


@transforms.add
def add_release_version(config, tasks):
    for task in tasks:
        if task.pop("include-release-version", False):
            task["run"]["gradlew"].extend(
                ["-PversionName={}".format(config.params["version"]), "-Pofficial"]
            )
        yield task


@transforms.add
def add_disable_optimization(config, tasks):
    for task in tasks:
        if task.pop("disable-optimization", False):
            task["run"]["gradlew"].append("-PdisableOptimization")
        yield task


@transforms.add
def add_nightly_version(config, tasks):
    for task in tasks:
        if task.pop("include-nightly-version", False):
            task["run"]["gradlew"].extend(
                [
                    # We only set the `official` flag here. The actual version name will be determined
                    # by Gradle (depending on the Gecko/A-C version being used)
                    "-Pofficial"
                ]
            )
        yield task
