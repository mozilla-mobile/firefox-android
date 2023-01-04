# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.


from taskgraph.loader.transform import loader as base_loader

from ..build_config import get_components, get_apk_based_projects


def components_loader(kind, path, config, params, loaded_tasks):
<<<<<<< HEAD
    """Loader that yields one task per android-component.

    Android-components are read from android-component/.buildconfig.yml
    """
    config["tasks"] = _get_components_tasks(config)
    return base_loader(kind, path, config, params, loaded_tasks)


def components_and_apks_loader(kind, path, config, params, loaded_tasks):
    """Loader that yield one task per android-componend and per apk-based project.

    For instance focus-android yields one task.
    Config is read from various .buildconfig.yml files.

    Additional tasks can be provided in the kind.yml under the key `tasks`.
    """

    components_tasks = _get_components_tasks(config, for_build_type="regular")
    apks_tasks = _get_apks_tasks(config)
    config["tasks"] = merge(config["tasks"], components_tasks, apks_tasks)
    return base_loader(kind, path, config, params, loaded_tasks)


def _get_components_tasks(config, for_build_type=None):
=======
>>>>>>> 4983e9ff87 (Bug 1807237 - part 4: Rename `build_config:loader` into `build_config:components_loader`)
    not_for_components = config.get("not-for-components", [])
    tasks = {
        '{}{}'.format(
            '' if build_type == 'regular' else build_type + '-',
            component['name']
        ): {
            'attributes': {
                'build-type': build_type,
                'component': component['name'],
                # Treeherder group are capped at 25 chars
                'treeherder-group': component['name'][:25],
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
<<<<<<< HEAD

    return tasks
=======
    config['tasks'] = tasks
>>>>>>> 2eec195dba (Bug 1807237 - part 5: Move too long treeherder group in `build_config` loader)


def _get_apks_tasks(config):
    not_for_apks = config.get("not-for-apks", [])
    tasks = {
        apk["name"]: {}
        for apk in get_apk_based_projects()
        if apk["name"] not in not_for_apks
    }
    return tasks
