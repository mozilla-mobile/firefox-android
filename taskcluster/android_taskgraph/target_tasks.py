# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import os
import re
from redo import retry

from mozilla_version.maven import MavenVersion
from taskgraph.target_tasks import _target_task, target_tasks_default
from taskgraph.util.taskcluster import find_task_id
from taskgraph.util.vcs import get_repository

from android_taskgraph.release_type import does_task_match_release_type


def index_exists(index_path, reason=""):
    print(f"Looking for existing index {index_path} {reason}...")
    try:
        task_id = find_task_id(index_path)
        print(f"Index {index_path} exists: taskId {task_id}")
        return True
    except KeyError:
        print(f"Index {index_path} doesn't exist.")
        return False


@_target_task("nightly")
def target_tasks_nightly(full_task_graph, parameters, graph_config):
    def filter(task, parameters):
        build_type = task.attributes.get("build-type", "")
        return build_type == "nightly" or build_type == "focus-nightly"

    index_path = (
        f"{graph_config['trust-domain']}.v2.{parameters['project']}.branch."
        f"{parameters['head_ref']}.revision.{parameters['head_rev']}.taskgraph.decision-nightly"
    )
    if os.environ.get("MOZ_AUTOMATION") and retry(
        index_exists,
        args=(index_path,),
        kwargs={
            "reason": "to avoid triggering multiple nightlies off the same revision",
        },
    ):
        return []

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task("promote")
def target_tasks_promote(full_task_graph, parameters, graph_config):
    def filter(task, parameters):
        if (
            task.attributes.get("shipping_phase") == "promote"
            and does_task_match_release_type(task, parameters["release_type"])
        ):
            return True

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task("ship")
def target_tasks_ship(full_task_graph, parameters, graph_config):
    filtered_for_candidates = target_tasks_promote(
        full_task_graph,
        parameters,
        graph_config,
    )

    def filter(task, parameters):
        # Include promotion tasks; these will be optimized out
        if task.label in filtered_for_candidates:
            return True

        if (
            task.attributes.get("shipping_phase") == "ship"
            and does_task_match_release_type(task, parameters["release_type"])
        ):
            return True

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task("default")
def target_tasks_ac_default(full_task_graph, parameters, graph_config):
    def filter(task):
        # Trigger the nightly cron hook when the GV major version changes
        if task.kind != "trigger-nightly":
            return True
        repo = get_repository(os.getcwd())
        if get_gv_version(repo, parameters["base_rev"]) != get_gv_version(
            repo, parameters["head_rev"]
        ):
            return True
        return False

    return [
        l
        for l, t in full_task_graph.tasks.items()
        if l in target_tasks_default(full_task_graph, parameters, graph_config)
        and filter(t)
    ]


def get_gv_version(repo, revision):
    gecko_kt = repo.run(
        "show", f"{revision}:android-components/buildSrc/src/main/java/Gecko.kt"
    )
    match = re.search(r'version = "([^"]*)"', gecko_kt, re.MULTILINE)
    if not match:
        raise Exception(f"Couldn't parse geckoview version on commit {revision}")
    return MavenVersion.parse(match.group(1)).major_number
