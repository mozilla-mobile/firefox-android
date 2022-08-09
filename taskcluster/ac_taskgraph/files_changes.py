import subprocess

from taskgraph.util.taskcluster import get_session


def get_files_changed_pr(base_repository, pull_request_number):
    url = base_repository.replace("github.com", "api.github.com/repos")
    url += "/pulls/%s/files" % pull_request_number

    r = get_session().get(url, timeout=60)
    r.raise_for_status()
    files = [f["filename"] for f in r.json()]
    return files


# TODO Use the function in standalone taskgraph once it's available
# https://github.com/taskcluster/taskgraph/issues/81
def get_files_changed_push(base_rev, head_rev):
    command = ["git", "diff", "--no-color", "--name-only", f"{base_rev}..{head_rev}"]
    files_changed_bytes = subprocess.check_output(command).splitlines()
    return [
        f.decode("utf-8") for f in files_changed_bytes
    ]

