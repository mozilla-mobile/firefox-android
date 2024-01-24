#!/bin/bash

set -ex

if ! hg id >/dev/null 2>&1; then
    echo >&2 This script should run from a mercurial gecko checkout
    exit 1
fi
if [ -n "$(hg status mobile/android)" ]; then
    echo >&2 'Dirty working directory, please ensure mobile/android does not have uncommitted changes'
    exit 1
fi

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
