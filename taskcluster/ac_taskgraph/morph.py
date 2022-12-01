import os
import logging

from pathlib import Path
from itertools import islice
from taskgraph import MAX_DEPENDENCIES
from taskgraph.morph import register_morph, amend_taskgraph
from taskgraph.task import Task
from slugid import nice as slugid


CURRENT_FILE_PATH = Path(__file__)
REPO_ROOT_DIR = (Path(__file__).parent / ".." / "..").resolve()
CURRENT_RELATIVE_FILE_PATH = CURRENT_FILE_PATH.relative_to(REPO_ROOT_DIR)

logger = logging.getLogger(__name__)


@register_morph
def add_complete_task(taskgraph, label_to_taskid, parameters, graph_config):
    logger.debug("Morphing: adding complete task")

    if parameters["tasks_for"] not in (
        "github-push",
        "github-pull-request",
        "github-pull-request-untrusted",
    ):
        return taskgraph, label_to_taskid
    task_label = "complete-push" if parameters["tasks_for"] == "github-push" else "complete-pr"

    code_review_tasks = {
        task.label: task.task_id
        for task in taskgraph.tasks.values()
        if task.attributes.get("code-review")
    }

    if code_review_tasks:
        if len(code_review_tasks) <= MAX_DEPENDENCIES:
            tasks_to_amend = [
                _build_complete_task(taskgraph, parameters, task_label, code_review_tasks)
            ]
        else:
            main_complete_task_deps = []
            for i, dep_tasks in enumerate(divide_dict_into_chunks(code_review_tasks, MAX_DEPENDENCIES)):
                main_complete_task_deps.append(
                    _build_complete_task(taskgraph, parameters, task_label=f"{task_label}-{i}", dependencies=dep_tasks)
                )

            tasks_to_amend = main_complete_task_deps + [
                _build_complete_task(taskgraph, parameters, task_label, dependencies={
                    dep.label: dep.task_id for dep in main_complete_task_deps
                })
            ]

        taskgraph, label_to_taskid = amend_taskgraph(taskgraph, label_to_taskid, tasks_to_amend)
        logger.info(f"Added {len(tasks_to_amend)} complete task(s).")

    return taskgraph, label_to_taskid


def divide_dict_into_chunks(dict, chunk_size):
    it = iter(sorted(dict))
    for _ in range(0, len(dict), chunk_size):
        yield {key: dict[key] for key in islice(it, chunk_size)}


def _build_complete_task(taskgraph, parameters, task_label, dependencies):
    sorted_deps = sorted(dependencies.values())
    complete_task_def = {
        "provisionerId": f"mobile-{parameters['level']}",
        "workerType": "b-linux-gcp",
        "dependencies": sorted_deps,
        # This option permits to run the task
        # regardless of the dependencies tasks exit status
        # as we are interested in the task failures
        "requires": "all-resolved",
        "created": {"relative-datestamp": "0 seconds"},
        "deadline": {"relative-datestamp": "1 day"},
        # no point existing past the parent task's deadline
        "expires": {"relative-datestamp": "1 day"},
        "metadata": {
            "name": task_label,
            "description": "Tasks that indicate whether a push/PR is sane",
            "owner": parameters["owner"],
            "source": _get_morph_url(),
        },
        "scopes": [],
        "payload": {
            "command": [
                "/usr/local/bin/run-task",
                "--mobile-checkout=/builds/worker/checkouts/vcs/",
                "--",
                "bash",
                "-cx",
                f"/builds/worker/checkouts/vcs/taskcluster/scripts/are_dependencies_completed.py {' '.join(sorted_deps)}"
            ],
            "env": {
                "VCS_PATH": "/builds/worker/checkouts/vcs",
                "REPOSITORIES": "{\"mobile\": \"firefox-android\"}",
                "MOZ_SCM_LEVEL": parameters["level"],
                "MOZ_AUTOMATION": "1",
                "MOBILE_HEAD_REF": parameters["head_ref"],
                "MOBILE_HEAD_REV": parameters["head_rev"],
                "MOBILE_BASE_REPOSITORY": parameters["base_repository"],
                "MOBILE_HEAD_REPOSITORY": parameters["head_repository"],
                "MOBILE_REPOSITORY_TYPE": "git"
            },
            "features": {
                "taskclusterProxy": True,
            },
            "image": {
                "path": "public/image.tar.zst",
                "type": "task-image",
                "taskId": _find_task_id(taskgraph, "build-docker-image-base")
            },
            "maxRunTime": 600,
            "onExitStatus": {
                "retry": [
                    72
                ],
                "purgeCaches": [
                    72
                ],
            }
        },
        "routes": [
            "checks",
        ],
        "extra": {},
    }
    task = Task(
        kind="complete",
        label=task_label,
        attributes={},
        task=complete_task_def,
        dependencies=dependencies,
    )
    task.task_id = slugid()

    return task


def _get_morph_url():
    """
    Guess a URL for the current file, for source metadata for created tasks.
    """
    taskgraph_repo = os.environ.get(
        "MOBILE_HEAD_REPOSITORY", "https://github.com/mozilla-mobile/firefox-android"
    )
    taskgraph_rev = os.environ.get("MOBILE_HEAD_REV", "main")
    return f"{taskgraph_repo}/raw/{taskgraph_rev}/{CURRENT_RELATIVE_FILE_PATH}"


def _find_task_id(taskgraph, label):
    for task in taskgraph.tasks.values():
        if task.label == label:
            return task.task_id
