from dotenv import load_dotenv
import json
import os
import sys

from lib.testrail_conn import APIClient

try:
    MOBILE_HEAD_REF = os.environ.get('MOBILE_HEAD_REF')
except KeyError:
    print("ERROR: MOBILE_HEAD_REF env var not set")
    # sys.exit()

load_dotenv("test_status.env")

for key, value in os.environ.items():
    print(f"{key}={value}")

TEST_STATUS = os.getenv("TEST_STATUS")
print(f"Your TEST_STATUS value: {TEST_STATUS}")

if not os.path.isfile("test_status.env"):
    print("test_status.env path is incorrect.")
else:
    with open("test_status.env", "r") as file:
        file_contents = file.read()
        print(f"content of test_status.env:\n{file_contents}")

def release_number(MOBILE_HEAD_REF):
    parts = MOBILE_HEAD_REF.split('_')
    return parts[1]

def create_milestone_name(product_name, release_type, release_version):
    # Manual and automated smoke testing sign-off - Firefox Beta 116.0b8
    prefix = "Manual and automated smoke testing sign-off -"
    product_name = "Firefox"
    release_type = "Beta"
    release_version = "116.0b8"
    milestone_name = f'{prefix} {product_name} {release_type} {release_version}'
    return milestone_name

def test_status(TEST_STATUS):
    if TEST_STATUS == 'PASS':
        return True
    elif TEST_STATUS == 'FAIL':
        return False
    else:
        print(f"ERROR: TEST_STATUS value of {TEST_STATUS} is not valid.")
        # sys.exit(1)
class TestRail():

    def __init__(self):
        try:
            TESTRAIL_HOST = os.environ['TESTRAIL_HOST']
            self.client = APIClient(TESTRAIL_HOST)
            self.client.user = os.environ['TESTRAIL_USERNAME']
            self.client.password = os.environ['TESTRAIL_PASSWORD']
        except KeyError:
            print("ERROR: Missing testrail env var")
            sys.exit(1)

    # API: Projects
    def projects(self):
        return self.client.send_get('get_projects')

    def project(self, testrail_project_id):
        return self.client.send_get(
            'get_project/{0}'.format(testrail_project_id))

    # API: Cases
    def test_cases(self, testrail_project_id, testrail_test_suite_id):
        return self.client.send_get(
            'get_cases/{0}&suite_id={1}'
            .format(testrail_project_id, testrail_test_suite_id))
    
    # API: Projects
    def milestones(self, testrail_project_id):
        return self.client.send_get(
            'get_milestones/{0}'.format(testrail_project_id))

    def milestone(self, testrail_milestone_id):
        return self.client.send_get(
            'get_milestone/{0}'.format(testrail_milestone_id))
    
    def milestone_add(self, testrail_project_id, title, description):
        data = {"name": title, "description": description}
        return self.client.send_post(
            'add_milestone/{0}'.format(testrail_project_id), data)
    # API: Runs
    # def test_runs(self, testrail_project_id, start_date='', end_date=''):
    #     date_range = ''
    #     if start_date:
    #         after = dt.convert_datetime_to_epoch(start_date)
    #         date_range += '&created_after={0}'.format(after)
    #     if end_date:
    #         before = dt.convert_datetime_to_epoch(end_date)
    #         date_range += '&created_before={0}'.format(before)
    #     return self.client.send_get('get_runs/{0}{1}'.format(testrail_project_id, date_range)) # noqa

    # def test_run(self, run_id):
    #     return self.client.send_get('get_run/{0}'.format(run_id))
    
    def test_cases_automated():
        # need to query full functional suite to return a list of case_ids where Automation field
        # equals 'Completed' (#4)
        # 'automation_status' = 4
        pass

    def test_run_add(self, testrail_project_id, testrail_milestone_id, name_run):
        """
        {
            "suite_id": 1,
            "name": "This is a new test run",
            "assignedto_id": 5,
            "refs": "SAN-1, SAN-2",
            "include_all": false,
            "case_ids": [1, 2, 3, 4, 7, 8]
        }
        """
        data = {"name": name_run, "milestone_id": testrail_milestone_id}
        return self.client.send_post('add_run/{0}'.format(testrail_project_id), data)
    
class DemoClient():
    def __init__(self):
        self.testrail = TestRail()

    def milestone_description(self):
        # need to be parameterized with real values
        '''
        RELEASE : Firefox Beta 117.0b5
        RELEASE_TAG_URL: https://github.com/mozilla-mobile/fenix/releases
        RELEASE_DATE : August 9th, 2023
        '''
        # this blurb can be added as-is
        '''
        Confluence page: https://mozilla-hub.atlassian.net/wiki/spaces/MTE/pages/21561505/Build+Validation+-+Firefox
        Known issues: https://bugzilla.mozilla.org/buglist.cgi?quicksearch=1813103%2C%201833085%2C%201843647%2C%201820923%2C%201815732%2C%201809247%2C%201750918%2C%201815638%2C%201810646&list_id=16678527
        Verified issues: https://bugzilla.mozilla.org/buglist.cgi?quicksearch=1842736%201839158&list_id=16678682
        New issue: https://bugzilla.mozilla.org/show_bug.cgi?id=1847893

        '''
        # this blurb is here for reference. qa will need to fill this in manually
        '''
        TESTING_STATUS: [GREEN] / [DONE]
        QA_RECOMMENDATION : [ “Ship it.” ]
        QA_RECOMMENDATION_VERBOSE :["One new issue was discovered, but it is also present in the Nightly and RC. "]
        TESTING_SUMMARY
        * Ran the second half of the medium/low priority tests (tier 2), on phones & tablets;
        * Ran 2 sets of automated smoke and sanity tests;
        '''
        description = "this is my description"
        release = '\n'
        release_tag_url = ''
        release_date = ''
        return description
    
    def setup(self, testrail_project_id):
        title = "jackie's title 3"
        name_run = "jackie's run 3"
        description = self.milestone_description()
        response = self.testrail.milestone_add(testrail_project_id, title, description)
        testrail_milestone_id = response["id"]
        self.testrail.test_run_add(testrail_project_id, testrail_milestone_id, name_run)

if __name__ == "__main__":
    """
    1. get MOBILE_HEAD_REF
    2. parse MOBILE_HEAD_REF for version number
    3. parse ui test results from live.log
    if tests pass:
        4. create milestone name with version number
        5. POST to TestRail with new milestone name
        6. create test run name with version number + milestone_id
            Format: Automated smoke test - {Release Type} {version_number}
        7. query testrail full functional suite where automation field = 'Completed' (returns list of case_ids)
        7. POST to TestRail milestone with new test run
            Format: Automated smoke test - {Device} {Device API}
        8. POST test results to testrail with test run with run results (TBD)
    else: abort
    """
    x = release_number(MOBILE_HEAD_REF)
    print(x)
    
    if test_status(TEST_STATUS):
        print("Creating Milestone...")
        # milestone_name = create_milestone_name(product_name, release_type, release_version)
        # testrail.milestone_add(testrail_project_id, milestone_name)
        # test_run_name = create_test_run_name(product_name, release_type, release_version, milestone_id)
        # testrail.test_run_add(testrail_project_id, testrail_milestone_id, test_run_name)
        # testrail.test_results_add(testrail_project_id, testrail_test_run_id, testrail_test_case_id, testrail_test_status_id, testrail_test_comment)
    else:
        print("Tests failed")
