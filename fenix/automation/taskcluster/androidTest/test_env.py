import os
from lib.testrail_conn import APIClient
from dotenv import load_dotenv
from datetime import datetime

load_dotenv("test_status.env")

try:
    MOBILE_HEAD_REF = os.environ['MOBILE_HEAD_REF']
    TEST_STATUS = os.environ["TEST_STATUS"]

    if TEST_STATUS not in ('PASS', 'FAIL'):
        raise ValueError(f"ERROR: Invalid TEST_STATUS value: {TEST_STATUS}")
except KeyError as e:
    raise ValueError(f"ERROR: Missing Environment Variable: {e}")

def parse_release_number(MOBILE_HEAD_REF):
    parts = MOBILE_HEAD_REF.split('_')
    return parts[1]

def build_milestone_name():
    pass

def build_milestone_description():
    current_date = datetime.now()
    formatted_date = current_date = current_date.strftime("%B %d, %Y")
    description = f"""
        RELEASE: {milestone_name}\n\n\
        RELEASE_TAG_URL: https://github.com/mozilla-mobile/fenix/releases\n\n\
        RELEASE_DATE: {formatted_date}\n\n\
        TESTING_STATUS: [GREEN]/[DONE]\n\n\
        QA_RECOMMENDATION:[Ship it.]\n\n\
        QA_RECOMENTATION_VERBOSE: \n\n\
        TESTING_SUMMARY\n\n\
        Known issues: n/a\n\
        New issue: n\a\n\
        Verified issue: 
    """

class TestRail():

    def __init__(self):
        try:
            self.client = APIClient(os.environ['TESTRAIL_HOST'])
            self.client.user = os.environ['TESTRAIL_USERNAME']
            self.client.password = os.environ['TESTRAIL_PASSWORD']
        except KeyError as e:
            raise ValueError(f"ERROR: Missing Testrail Env Var: {e}")
    
    # Public Methods

    def create_milestone(self, testrail_project_id, title, description):
        data = {"name": title, "description": description}
        return self.client.send_post(f'add_milestone/{testrail_project_id}', data)
    
    def create_test_run(self, testrail_project_id, testrail_milestone_id, name_run, testrail_suite_id):
        data = {"name": name_run, "milestone_id": testrail_milestone_id, "suite_id": testrail_suite_id}
        return self.client.send_post(f'add_run/{testrail_project_id}', data)
    
    def update_test_cases_to_passed(self, testrail_project_id, testrail_run_id, testrail_suite_id):
        test_cases = self.get_test_cases(testrail_project_id, testrail_suite_id)
        data = { "results": [{"case_id": test_case['id'], "status_id": 1} for test_case in test_cases]}
        return testrail.update_test_run_results(testrail_run_id, data)

    # Private Methods

    def _get_test_cases(self, testrail_project_id, testrail_test_suite_id):
        return self.client.send_get(f'get_cases/{testrail_project_id}&suite_id={testrail_test_suite_id}')
    
    def _update_test_run_results(self, testrail_run_id, data):
        return self.client.send_post(f'add_results_for_cases/{testrail_run_id}', data)
    
if __name__ == "__main__":
    if TEST_STATUS != 'PASS':
        raise ValueError("Tests failed. Sending Slack Notification...")

    testrail = TestRail()
    milestone_name = f"Automated smoke testing sign-off - Jackie's Demo - {parse_release_number(MOBILE_HEAD_REF)}"
    milestone_description = "HELLO_WORLD:\r\nTWO:\r\nTHREE:"

    # Create milestone for Snippets Project and store the ID
    milestone_id = testrail.create_milestone(45, milestone_name, milestone_description)['id']

    # Create test run for each Device/API and update test cases to 'passed'
    for test_run_name in ['Google Pixel 32(Android11)', 'Google Pixel2(Android9)']:
        test_run_id = testrail.create_test_run(45, milestone_id, test_run_name, 45384)['id']
        testrail.update_test_cases_to_passed(45, test_run_id, 45384)
