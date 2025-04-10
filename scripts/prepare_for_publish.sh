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
    echo -e "Example: $0 1.2.5"
    exit 1
fi

VERSION=$1
BRANCH_NAME="publish-$VERSION"
ROOT_DIR="$(pwd)"

# Validate semantic version format (simple check)
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Version should follow semantic versioning (e.g., 1.2.5)${NC}"
    exit 1
fi

echo -e "${BLUE}=== Preparing to publish version $VERSION ===${NC}"

# Create a new branch for publishing
git checkout -b $BRANCH_NAME
echo -e "${GREEN}Created branch: $BRANCH_NAME${NC}"

# List of all packages to update
PACKAGES=(
    "zikzak_inappwebview_internal_annotations"
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
    # Check if the package directory exists
    if [ ! -d "$ROOT_DIR/$pkg" ]; then
        echo -e "${RED}Warning: Package directory '$pkg' not found. Skipping.${NC}"
        continue
    fi
    
    # Update version in pubspec.yaml
    if [ -f "$ROOT_DIR/$pkg/pubspec.yaml" ]; then
        # Use awk for reliable version replacement
        awk -v version="$VERSION" '{
            if ($0 ~ /^version:/) {
                print "version: " version;
            } else {
                print $0;
            }
        }' "$ROOT_DIR/$pkg/pubspec.yaml" > "$ROOT_DIR/$pkg/pubspec.yaml.new"
        
        mv "$ROOT_DIR/$pkg/pubspec.yaml.new" "$ROOT_DIR/$pkg/pubspec.yaml"
        
        # Verify the update
        new_version=$(grep "^version:" "$ROOT_DIR/$pkg/pubspec.yaml" | sed 's/version: //' | tr -d '[:space:]')
        if [ "$new_version" != "$VERSION" ]; then
            echo -e "${RED}Failed to update version for $pkg to $VERSION. Current version: $new_version${NC}"
        else
            echo -e "${GREEN}Successfully updated $pkg to version $VERSION${NC}"
        fi
    else
        echo -e "${RED}Warning: pubspec.yaml not found in $pkg. Skipping.${NC}"
    fi
done

# Update dependencies in each package to use versioned dependencies instead of path
echo -e "${BLUE}Updating dependencies to use versioned references${NC}"

# First update internal_annotations dependency in platform_interface
if [ -f "$ROOT_DIR/zikzak_inappwebview_platform_interface/pubspec.yaml" ]; then
    echo -e "${YELLOW}Updating internal_annotations dependency in platform_interface${NC}"
    awk -v version="$VERSION" '
    {
        if ($0 ~ /zikzak_inappwebview_internal_annotations:/) {
            if ($0 ~ /^  zikzak_inappwebview_internal_annotations:$/) {
                print "  zikzak_inappwebview_internal_annotations: ^" version;
                getline; # skip the path line if it exists
                if ($0 !~ /path:/) {
                    print $0; # if not a path line, print it
                }
            } else {
                print "  zikzak_inappwebview_internal_annotations: ^" version;
            }
        } else if ($0 ~ /path: ..\/zikzak_inappwebview_internal_annotations/) {
            # Skip path lines
        } else {
            print $0;
        }
    }' "$ROOT_DIR/zikzak_inappwebview_platform_interface/pubspec.yaml" > "$ROOT_DIR/zikzak_inappwebview_platform_interface/pubspec.yaml.new"
    
    mv "$ROOT_DIR/zikzak_inappwebview_platform_interface/pubspec.yaml.new" "$ROOT_DIR/zikzak_inappwebview_platform_interface/pubspec.yaml"
fi

# Update platform_interface dependency in all platform packages
for pkg in "zikzak_inappwebview_android" "zikzak_inappwebview_ios" "zikzak_inappwebview_macos" "zikzak_inappwebview_web" "zikzak_inappwebview_windows"; do
    if [ -f "$ROOT_DIR/$pkg/pubspec.yaml" ]; then
        echo -e "${YELLOW}Updating platform_interface dependency in $pkg${NC}"
        awk -v version="$VERSION" '
        {
            if ($0 ~ /zikzak_inappwebview_platform_interface:/) {
                if ($0 ~ /^  zikzak_inappwebview_platform_interface:$/) {
                    print "  zikzak_inappwebview_platform_interface: ^" version;
                    getline; # skip the path line if it exists
                    if ($0 !~ /path:/) {
                        print $0; # if not a path line, print it
                    }
                } else {
                    print "  zikzak_inappwebview_platform_interface: ^" version;
                }
            } else if ($0 ~ /path: ..\/zikzak_inappwebview_platform_interface/) {
                # Skip path lines
            } else {
                print $0;
            }
        }' "$ROOT_DIR/$pkg/pubspec.yaml" > "$ROOT_DIR/$pkg/pubspec.yaml.new"
        
        mv "$ROOT_DIR/$pkg/pubspec.yaml.new" "$ROOT_DIR/$pkg/pubspec.yaml"
    else
        echo -e "${RED}Warning: pubspec.yaml not found in $pkg. Skipping.${NC}"
    fi
done

# Update all dependencies in the main package
if [ -f "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml" ]; then
    echo -e "${YELLOW}Updating all dependencies in main package${NC}"
    
    # Process each package dependency one by one
    for dep_pkg in "zikzak_inappwebview_internal_annotations" "zikzak_inappwebview_platform_interface" "zikzak_inappwebview_android" "zikzak_inappwebview_ios" "zikzak_inappwebview_macos" "zikzak_inappwebview_web" "zikzak_inappwebview_windows"; do
        awk -v pkg="$dep_pkg" -v version="$VERSION" '
        {
            if ($0 ~ pkg ":") {
                if ($0 ~ "^  " pkg ":$") {
                    print "  " pkg ": ^" version;
                    getline; # skip the path line if it exists
                    if ($0 !~ /path:/) {
                        print $0; # if not a path line, print it
                    }
                } else {
                    print "  " pkg ": ^" version;
                }
            } else if ($0 ~ "path: ..\\/" pkg) {
                # Skip path lines
            } else {
                print $0;
            }
        }' "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml" > "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml.new"
        
        mv "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml.new" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
    done
else
    echo -e "${RED}Warning: pubspec.yaml not found for main package. Skipping.${NC}"
fi

# Update generators package to use hosted dependency instead of path
if [ -f "$ROOT_DIR/dev_packages/generators/pubspec.yaml" ]; then
    echo -e "${YELLOW}Updating internal_annotations dependency in generators package${NC}"
    awk -v version="$VERSION" '
    {
        if ($0 ~ /zikzak_inappwebview_internal_annotations:/) {
            if ($0 ~ /^  zikzak_inappwebview_internal_annotations:$/) {
                print "  zikzak_inappwebview_internal_annotations: ^" version;
                getline; # skip the path line if it exists
                if ($0 !~ /path:/) {
                    print $0; # if not a path line, print it
                }
            } else {
                print "  zikzak_inappwebview_internal_annotations: ^" version;
            }
        } else if ($0 ~ /path: ..\/..\/zikzak_inappwebview_internal_annotations/) {
            # Skip path lines
        } else {
            print $0;
        }
    }' "$ROOT_DIR/dev_packages/generators/pubspec.yaml" > "$ROOT_DIR/dev_packages/generators/pubspec.yaml.new"
    
    mv "$ROOT_DIR/dev_packages/generators/pubspec.yaml.new" "$ROOT_DIR/dev_packages/generators/pubspec.yaml"
    echo -e "${GREEN}Successfully updated generators package to use hosted dependency${NC}"
else
    echo -e "${RED}Warning: pubspec.yaml not found for generators package. Skipping.${NC}"
fi

# Update CHANGELOG.md files with new version
CURRENT_DATE=$(date +"%Y-%m-%d")
for pkg in "${PACKAGES[@]}"; do
    if [ -f "$ROOT_DIR/$pkg/CHANGELOG.md" ]; then
        echo -e "${BLUE}Updating CHANGELOG.md in $pkg${NC}"
        # Add new version entry at the top of the CHANGELOG
        sed -i '' "1s/^/## $VERSION - $CURRENT_DATE\n\n* Bump version\n* Updated dependencies to use hosted references\n* Improved iOS 18+ compatibility\n\n/" "$ROOT_DIR/$pkg/CHANGELOG.md"
    else
        echo -e "${RED}Warning: CHANGELOG.md not found in $pkg. Skipping.${NC}"
    fi
done

# Function to check if a package version is already published on pub.dev
check_package_on_pubdev() {
    local package_name=$1
    local version=$2
    
    echo -e "${YELLOW}Checking if $package_name version $version is already on pub.dev...${NC}"
    
    # Use curl to query the pub.dev API
    local response=$(curl -s "https://pub.dev/api/packages/$package_name")
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" "https://pub.dev/api/packages/$package_name")
    
    # Check if the package exists
    if [ "$http_code" != "200" ]; then
        echo -e "${BLUE}Package $package_name not found on pub.dev. Will be published for the first time.${NC}"
        return 1
    fi
    
    # Check if the version exists in the package versions
    if echo "$response" | grep -q "\"version\":\"$version\""; then
        echo -e "${RED}Version $version of $package_name is already published on pub.dev!${NC}"
        return 0
    else
        echo -e "${GREEN}Version $version of $package_name is not yet published. Ready to publish.${NC}"
        return 1
    fi
}

# Check all packages on pub.dev and display a summary
echo -e "${BLUE}\n=== Checking packages on pub.dev ===${NC}"
declare -A package_status

for pkg in "${PACKAGES[@]}"; do
    # Extract package name from directory
    pkg_name=$(basename "$pkg")
    
    if check_package_on_pubdev "$pkg_name" "$VERSION"; then
        package_status["$pkg_name"]="Already published"
    else
        package_status["$pkg_name"]="Not published (ready to publish)"
    fi
done

# Display publication status summary
echo -e "${BLUE}\n=== Publication Status Summary ===${NC}"
for pkg_name in "${!package_status[@]}"; do
    status="${package_status[$pkg_name]}"
    if [[ "$status" == "Already published" ]]; then
        echo -e "${pkg_name}: ${RED}$status${NC}"
    else
        echo -e "${pkg_name}: ${GREEN}$status${NC}"
    fi
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
