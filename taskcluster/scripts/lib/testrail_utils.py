#!/usr/bin/env python3

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""
This script contains utility functions designed to support the integration of automated 
testing processes with TestRail, a test case management tool. The primary focus is on 
creating and managing milestones in TestRail based on automated smoke tests for product 
releases. It includes functions for building milestone names and descriptions, determining 
release types, and loading TestRail credentials.

Functions:
- build_milestone_name(product_type, release_type, version_number): Constructs a formatted 
  milestone name based on the product type, release type, and version number.
- build_milestone_description(milestone_name): Generates a detailed description for the 
  milestone, including the release date and placeholders for testing status and QA recommendations.
- get_release_version(): Reads and returns the release version number from a 'version.txt' file.
- get_release_type(version): Determines the release type (e.g., Alpha, Beta, RC) based on 
  the version string.
- load_testrail_credentials(json_file_path): Loads TestRail credentials from a JSON file 
  and handles potential errors during the loading process.
"""

from datetime import datetime
import json
import os
import textwrap


def build_milestone_name(product_type, release_type, version_number):
    return f"Build Validation sign-off - {product_type} {release_type} {version_number}"


def build_milestone_description(milestone_name):
    current_date = datetime.now()
    formatted_date = current_date = current_date.strftime("%B %d, %Y")
    return textwrap.dedent(
        f"""
        RELEASE: {milestone_name}\n\n\
        RELEASE_TAG_URL: https://github.com/mozilla-mobile/firefox-android/releases\n\n\
        RELEASE_DATE: {formatted_date}\n\n\
        TESTING_STATUS: [ TBD ]\n\n\
        QA_RECOMMENDATION:[ TBD ]\n\n\
        QA_RECOMENTATION_VERBOSE: \n\n\
        TESTING_SUMMARY\n\n\
        Known issues: n/a\n\
        New issue: n/a\n\
        Verified issue: 
    """
    )


def get_release_version():
    # Get the current script's directory (absolute path)
    script_dir = os.path.dirname(os.path.abspath(__file__))

    # Initialize root_dir as script_dir initially
    root_dir = script_dir

    # Loop to traverse up until you find the version.txt or reach the filesystem root
    while root_dir != os.path.dirname(
        root_dir
    ):  # Check if root_dir has reached the filesystem root
        version_file_path = os.path.join(root_dir, "version.txt")
        if os.path.isfile(version_file_path):
            break
        root_dir = os.path.dirname(root_dir)  # Move one directory up

    # Check if version.txt was found
    if not os.path.isfile(version_file_path):
        raise FileNotFoundError(
            "version.txt not found in any of the parent directories."
        )

    # Read the version from the file
    with open(version_file_path, "r") as file:
        version = file.readline().strip()

    return version


def get_release_type(version):
    release_map = {"a": "Alpha", "b": "Beta"}
    # use generator expression to check each char for key else default to 'RC'
    product_type = next(
        (release_map[char] for char in version if char in release_map), "RC"
    )
    return product_type


def load_testrail_credentials(json_file_path):
    try:
        with open(json_file_path, "r") as file:
            credentials = json.load(file)
        return credentials
    except json.JSONDecodeError as e:
        raise ValueError(f"Failed to load TestRail credentials: {e}")
