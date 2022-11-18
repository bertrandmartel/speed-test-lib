#!/bin/sh

set -e -o pipefail

if [ "$#" -eq 0 ]; then
    exit 1
fi

case "$1" in

    "incrememnt" | "inc" | "version-inc" | "version-increment")
        echo "Incrementing version..."
        sh gradlew versionPatch
    ;;

esac
