# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from taskgraph.parameters import extend_parameters_schema
from voluptuous import All, Any, Range, Required


def get_defaults(repo_root):
    return {
        "pull_request_number": None,
        "release_type": "",
    }


extend_parameters_schema(
    {
        Required("pull_request_number"): Any(All(int, Range(min=1)), None),
        Required("release_type"): str,
    },
    defaults_fn=get_defaults,
)
