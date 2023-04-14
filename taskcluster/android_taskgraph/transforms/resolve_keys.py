from taskgraph.transforms.task import TransformSequence
from taskgraph.util.schema import Schema, optionally_keyed_by, resolve_keyed_by
from voluptuous import ALLOW_EXTRA, Optional

resolve_schema = Schema(
    {
        Optional("index"): optionally_keyed_by("build-type", dict),
        Optional("treeherder"): {
            Optional("job-symbol"): optionally_keyed_by("build-type", str),
        },
    },
    extra=ALLOW_EXTRA,
)

transforms = TransformSequence()
transforms.add_validate(resolve_schema)


@transforms.add
def resolve_keys(config, tasks):
    for task in tasks:
        for key in ("index", "treeherder.job-symbol"):
            resolve_keyed_by(
                task,
                key,
                item_name=task["name"],
                **{
                    "build-type": task["attributes"]["build-type"],
                    "level": config.params["level"],
                }
            )
        yield task
