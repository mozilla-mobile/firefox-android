#!/bin/bash

set -ex

git clone https://github.com/mozilla-mobile/firefox-android tmp-firefox-android

cd tmp-firefox-android

commit=$(git show-ref HEAD | awk '{print $1}')

rm -rf .git
rm -rf .github
rm -rf monorepo-migration
rm CODE_OF_CONDUCT.md
rm LICENSE
rm README.md

cd ..

cp -r tmp-firefox-android/{.*,*} mobile/android

rm -rf tmp-firefox-android

hg add mobile/android
hg commit -m "Import firefox-android commit $commit"

hg mv mobile/android/taskcluster/android_taskgraph taskcluster
hg mv mobile/android/taskcluster/scripts/* taskcluster/scripts
hg commit -m "Move android_taskgraph and scripts from mobile/android/taskcluster to taskcluster"
hg pull oak
hg rebase --hidden -r '(oak % central) - desc("Import firefox-android commit") - desc("Move android_taskgraph and scripts")' -d .
