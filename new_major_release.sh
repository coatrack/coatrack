#!/bin/bash

# Parameter input for new release version
release_major_version=${1?"please provide release version as first argument e.g. 1 or 2"}
echo "#################################################################################"
echo "Make sure you are on the master branch you want to create a release for"
echo "#################################################################################"
git branch
#
# Check if person is on the right branch
# Provide with y or n
#
read -p "Are you on the right branch? (y/n) " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
# If person answered with y continue
then
    # Variable release name with parameter input of 12 which will result in  release/12
    release_name="release/$release_major_version"
    # Add 1 of the parameter input to create a new SNAPSHOT version
    dev_version=$((release_major_version+1))

    # Go to master branch, releases are created from master branch only. Pull in latest changes
    git checkout master
    git pull
    # Set release version variable added minor and patch version since this is a new major the other 2 will result in 0
    release_version="$release_major_version.0.0"
    # Build the repository with new version
    bash build.sh $release_version
    # add all the changes and commit with commit message prepare release
    git add .
    git commit -m "prepare release $release_major_version"
    # Create tag for new release tag will result in major version with minor and patch e.g. 12.0.0
    git tag -a $release_version -m "$release_name"
    git push origin $release_version
    # Create branch with new release name e.g. release/12
    git checkout -b $release_name $release_version
    # Change pom version for major release e.g. 12.0.0
    mvn versions:set -DnewVersion=$release_version
    # Add all changes and push the commit with setting pom version as message
    git add .
    git commit -m "setting pom version"
    git push --set-upstream origin $release_name

    # Change back to master branch and set pom version to new SNAPSHOT version e.g. 13.0-SNAPSHOT
    git checkout master
    git pull
    mvn versions:set -DnewVersion="$dev_version.0.0-SNAPSHOT"
    bash build.sh "$dev_version.0.0-SNAPSHOT"
    # Add the pom changes and push these to master
    git add .
    git commit -m "new development version $dev_version"

    git push
fi
