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

ATASK=":adam:assemble :adam-junit4-android:assemble :adam-junit4-android-contract:assemble"
DTASK=":adam:publishDefaultPublicationToOSSHRRepository :adam-junit4-android:publishDefaultPublicationToOSSHRRepository :adam-junit4-android-contract:publishDefaultPublicationToOSSHRRepository"

if [ -n "$GIT_TAG_NAME" ]; then
  echo "on a tag -> deploy release version $GIT_TAG_NAME"
  ./gradlew "$ATASK" -PreleaseMode=RELEASE
  ./gradlew "$DTASK" -PreleaseMode=RELEASE
else
  echo "not on a tag -> deploy snapshot version"
  ./gradlew "$ATASK" -PreleaseMode=SNAPSHOT
  ./gradlew "$DTASK" -PreleaseMode=SNAPSHOT
fi
