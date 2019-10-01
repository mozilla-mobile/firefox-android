# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.target_tasks import _target_task, filter_for_tasks_for


@_target_task('default')
def target_tasks_default(full_task_graph, parameters, graph_config):
    """Target the tasks which have indicated they should be run on this project
    via the `run_on_projects` attributes."""
    def filter(t, params):
        if t.kind == 'old-decision':
            return True

        return filter_for_tasks_for(t, params)

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]


@_target_task("snapshot")
def target_tasks_snapshot(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for snapshot builds."""
    # TODO
    return [l for l, t in full_task_graph.tasks.iteritems() if filter_for_tasks_for(t, parameters)]
