#!/usr/bin/env bash
cd $(dirname $0)/..

if [ -z "$GITHUB_MAVEN_USERNAME" ]; then
  echo "error: please set GITHUB_MAVEN_USERNAME environment variable"
  exit 1
fi

if [ -z "$GITHUB_TOKEN" ]; then
  echo "error: please set GITHUB_MAVEN_PASSWORD environment variable"
  exit 1
fi

if [ -z "$GPG_PASSPHRASE" ]; then
  echo "error: please set GPG_PASSPHRASE environment variable"
  exit 1
fi

ATASK=":adam:assemble :android-junit4:assemble :android-testrunner-contract:assemble"
DTASK=":adam:publishDefaultPublicationToGitHubRepository :android-junit4:publishDefaultPublicationToGitHubRepository :android-testrunner-contract:publishDefaultPublicationToGitHubRepository"

if [ -n "$GIT_TAG_NAME" ]; then
  echo "on a tag -> deploy release version $GIT_TAG_NAME"
  ./gradlew "$ATASK" -PreleaseMode=RELEASE
  ./gradlew "$DTASK" -PreleaseMode=RELEASE
else
  echo "not on a tag -> deploy snapshot version"
  ./gradlew "$ATASK" -PreleaseMode=SNAPSHOT
  ./gradlew "$DTASK" -PreleaseMode=SNAPSHOT
fi
