# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.


from taskgraph.loader.transform import loader as base_loader
from taskgraph.util.templates import merge

from ..build_config import get_components, get_apks


# Treeherder group are capped at 25 chars
TREEHERDER_GROUPS_PER_TOO_LONG_COMPONENT_NAME = {
    "lib-fetch-httpurlconnection": "lib-fetch-httpurlconnect",
    "feature-webcompat-reporter": "feature-webcompat-report",
}


def components_loader(kind, path, config, params, loaded_tasks):
    config["tasks"] = _get_components_tasks(config)
    return base_loader(kind, path, config, params, loaded_tasks)


def components_and_apks_loader(kind, path, config, params, loaded_tasks):
    components_tasks = _get_components_tasks(config, for_build_type="regular")
    apks_tasks = _get_apks_tasks(config)
    config["tasks"] = merge(config["tasks"], components_tasks, apks_tasks)
    return base_loader(kind, path, config, params, loaded_tasks)


def _get_components_tasks(config, for_build_type=None):
    not_for_components = config.get("not-for-components", [])
    tasks = {
        '{}{}'.format(
            '' if build_type == 'regular' else build_type + '-',
            component['name']
        ): {
            'attributes': {
                'build-type': build_type,
                'component': component['name'],
                'treeherder-group': TREEHERDER_GROUPS_PER_TOO_LONG_COMPONENT_NAME.get(
                    component['name'], component['name']
                ),
            }
        }
        for component in get_components()
        for build_type in ('regular', 'nightly', 'release')
        if (
            component['name'] not in not_for_components
            and (component['shouldPublish'] or build_type == 'regular')
            and (for_build_type is None or build_type == for_build_type)
        )
    }

    return tasks

def _get_apks_tasks(config):
    not_for_apks = config.get("not-for-apks", [])
    tasks = {
        # TODO Support fenix
        "focus" if apk["name"] == "app" else apk["name"]: {}
        for apk in get_apks()
        if (
            apk["name"] not in not_for_apks
        )
    }
    return tasks
