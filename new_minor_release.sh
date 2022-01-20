#!/bin/bash
# Get current release version
release_branch_version=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)
# Set variables major minor and patch from current release version
IFS=. read major minor patch <<<"${release_branch_version##*-}"

# Get previous minor version and increment with one
release_minor_version=$((minor+1))
echo "#################################################################################"
echo "Make sure you are on the release branch you want to create a minor release for"
echo "NOTE!!!! make sure you have all the latest changes from master in the release branch"
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
    # Set variables
    release_major_version=$major
    release_name="release/$release_major_version"
    release_version="$release_major_version.$release_minor_version.0"
    # Build repository with new release_verion e.g. 12.1.0 if major was 12 and minor was 1
    bash build.sh $release_version

    # Add,commit changes with release version as commit message
    git add .
    git commit -m "prepare release $release_major_version.$release_minor_version"
    # Create new tag for new minor release
    git tag -a $release_version -m "$release_version"
    git push origin $release_version
    # Change pom version to match new release
    mvn versions:set -DnewVersion=$release_version
    # Add and push pom changes to the major release branch
    # NOTE: creating a minor release will not create a new branch it will update the current release branch pom version
    git add .
    git commit -m "store pom minor release version changes"
    # Since some release branches were created with a wrong name changes to the release branch need to be pushed manually
    echo "#################################################################################"
    echo "NOTE: "
    echo "Manually push changes to release branch"
    echo "#################################################################################"
    # Commenting push untill all releases are created with the scripts since some are made manually and have a different name
#    git push origin $release_name
fi
