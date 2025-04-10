#!/bin/bash
set -e

# Color codes for better readability
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

readonly SCRIPT_PATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
readonly PROJECT_DIR="$(dirname $SCRIPT_PATH)"

# The order of packages for publishing
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

# Function to publish a package
publish_package() {
    local package_dir="$1"
    
    echo -e "${BLUE}======================================${NC}"
    echo -e "${YELLOW}Publishing package: ${GREEN}$package_dir${NC}"
    echo -e "${BLUE}======================================${NC}"
    
    # Navigate to the package directory
    cd "$PROJECT_DIR/$package_dir"
    
    # Format the Dart code
    echo -e "${BLUE}Formatting Dart code...${NC}"
    if [ -d "lib" ]; then
        dart format lib/
    fi
    
    # Analyze the package
    echo -e "${BLUE}Analyzing package...${NC}"
    flutter analyze
    
    # Publish with confirmation
    echo -e "${BLUE}Running dry-run...${NC}"
    flutter pub publish --dry-run
    
    echo -e "${YELLOW}Ready to publish ${GREEN}$package_dir${YELLOW}? (y/n)${NC}"
    read -r answer
    if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
        echo -e "${BLUE}Publishing...${NC}"
        flutter pub publish -f
        echo -e "${GREEN}Package $package_dir published successfully!${NC}"
        # Wait a moment for pub.dev to process
        echo -e "${BLUE}Waiting for pub.dev to process the package...${NC}"
        sleep 60
    else
        echo -e "${RED}Skipping $package_dir...${NC}"
        return 1
    fi
    
    return 0
}

# Main script execution
echo -e "${BLUE}Starting publication process in the correct order${NC}"
echo -e "${YELLOW}Packages will be published in this order:${NC}"
for package in "${PACKAGES[@]}"; do
    echo -e "- $package"
done
echo

# Confirm publication of all packages
echo -e "${YELLOW}Do you want to proceed with publishing all packages? (y/n)${NC}"
read -r proceed
if [ "$proceed" != "y" ] && [ "$proceed" != "Y" ]; then
    echo -e "${RED}Publication process aborted.${NC}"
    exit 0
fi

# Publish each package in the defined order
for package in "${PACKAGES[@]}"; do
    if ! publish_package "$package"; then
        echo -e "${RED}Publication process stopped at $package.${NC}"
        exit 1
    fi
done

echo -e "${GREEN}All packages published successfully!${NC}"
echo -e "${BLUE}Remember to run './scripts/restore_dev_setup.sh' to revert to development mode.${NC}"