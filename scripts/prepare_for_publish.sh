#!/bin/bash
set -e

# Color codes for better readability
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if version argument is provided
if [ "$#" -ne 1 ]; then
    echo -e "${RED}Error: Version number is required.${NC}"
    echo -e "Usage: $0 <version_number>"
    echo -e "Example: $0 1.2.0"
    exit 1
fi

VERSION=$1
BRANCH_NAME="publish-$VERSION"
ROOT_DIR="$(pwd)"

# Validate semantic version format (simple check)
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Version should follow semantic versioning (e.g., 1.2.0)${NC}"
    exit 1
fi

echo -e "${BLUE}=== Preparing to publish version $VERSION ===${NC}"

# Create a new branch for publishing
git checkout -b $BRANCH_NAME
echo -e "${GREEN}Created branch: $BRANCH_NAME${NC}"

# List of all packages to update
PACKAGES=(
    "zikzak_inappwebview_platform_interface"
    "zikzak_inappwebview_android"
    "zikzak_inappwebview_ios"
    "zikzak_inappwebview_macos"
    "zikzak_inappwebview_web"
    "zikzak_inappwebview_windows"
    "zikzak_inappwebview"
)

# Update versions in all package pubspec.yaml files
for pkg in "${PACKAGES[@]}"; do
    echo -e "${BLUE}Updating version in $pkg to $VERSION${NC}"
    # Update version in pubspec.yaml
    sed -i '' "s/^version: .*/version: $VERSION/" "$ROOT_DIR/$pkg/pubspec.yaml"
done

# Update dependencies in each package to use versioned dependencies instead of path
echo -e "${BLUE}Updating dependencies to use versioned references${NC}"

# Update platform packages to depend on platform_interface with version
for pkg in "zikzak_inappwebview_android" "zikzak_inappwebview_ios" "zikzak_inappwebview_macos" "zikzak_inappwebview_web" "zikzak_inappwebview_windows"; do
    echo -e "${YELLOW}Updating dependencies in $pkg${NC}"
    # Replace path dependency with versioned dependency for platform_interface
    sed -i '' "s|  zikzak_inappwebview_platform_interface:\n    path: ../zikzak_inappwebview_platform_interface|  zikzak_inappwebview_platform_interface: ^$VERSION|g" "$ROOT_DIR/$pkg/pubspec.yaml"
done

# Update main package dependencies
echo -e "${YELLOW}Updating dependencies in main package${NC}"
sed -i '' "s|  zikzak_inappwebview_platform_interface:\n    path: ../zikzak_inappwebview_platform_interface|  zikzak_inappwebview_platform_interface: ^$VERSION|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_android:\n    path: ../zikzak_inappwebview_android|  zikzak_inappwebview_android: ^$VERSION|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_ios:\n    path: ../zikzak_inappwebview_ios|  zikzak_inappwebview_ios: ^$VERSION|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_macos:\n    path: ../zikzak_inappwebview_macos|  zikzak_inappwebview_macos: ^$VERSION|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_web:\n    path: ../zikzak_inappwebview_web|  zikzak_inappwebview_web: ^$VERSION|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_windows:\n    path: ../zikzak_inappwebview_windows|  zikzak_inappwebview_windows: ^$VERSION|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"

# Update CHANGELOG.md files with new version
CURRENT_DATE=$(date +"%Y-%m-%d")
for pkg in "${PACKAGES[@]}"; do
    echo -e "${BLUE}Updating CHANGELOG.md in $pkg${NC}"
    # Add new version entry at the top of the CHANGELOG
    sed -i '' "1s/^/## $VERSION - $CURRENT_DATE\n\n* Bump version\n\n/" "$ROOT_DIR/$pkg/CHANGELOG.md"
done

echo -e "${GREEN}All packages updated to version $VERSION with versioned dependencies${NC}"
echo -e "${YELLOW}Next steps:${NC}"
echo -e "1. Review the changes with 'git diff'"
echo -e "2. Modify CHANGELOG.md files to add more detailed release notes if needed"
echo -e "3. Commit changes: git commit -am \"Prepare for publishing version $VERSION\""
echo -e "4. Publish packages in order using the publish.sh script"
echo -e "5. After publishing, switch back to main branch: git checkout main"
echo -e ""
echo -e "${BLUE}To revert to development setup (path dependencies), use:${NC}"
echo -e "./scripts/restore_dev_setup.sh"
