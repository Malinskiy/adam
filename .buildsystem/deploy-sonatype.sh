#!/usr/bin/env bash
cd $(dirname $0)/..

if [ -z "$SONATYPE_USERNAME" ]; then
  echo "error: please set SONATYPE_USERNAME environment variable"
  exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ]; then
  echo "error: please set SONATYPE_PASSWORD environment variable"
  exit 1
fi

if [ -z "$GPG_PASSPHRASE" ]; then
  echo "error: please set GPG_PASSPHRASE environment variable"
  exit 1
fi

ATASK=""
DTASK=""
for i in ":adam" ":android-junit4" ":android-testrunner-contract" ":server-stub"; do
  ATASK="$ATASK $i:assemble"
  DTASK="$DTASK $i:publishDefaultPublicationToOSSHRRepository"
done

echo "Value of TEST_ENVVAR is $TEST_ENVVAR"

if [ -z "$GIT_TAG_NAME" ]; then
  echo "not on a tag -> deploy snapshot version"
  ./gradlew $ATASK -PreleaseMode=SNAPSHOT
  ./gradlew $DTASK -PreleaseMode=SNAPSHOT
else
  echo "on a tag -> deploy release version $GIT_TAG_NAME"
  ./gradlew $ATASK -PreleaseMode=RELEASE
  ./gradlew $DTASK -PreleaseMode=RELEASE
fi
