#!/bin/bash
set -e

# Color codes for better readability
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

ROOT_DIR="$(pwd)"

echo -e "${BLUE}=== Restoring development setup with path dependencies ===${NC}"

# Update platform packages to use path dependency for platform_interface
for pkg in "zikzak_inappwebview_android" "zikzak_inappwebview_ios" "zikzak_inappwebview_macos" "zikzak_inappwebview_web" "zikzak_inappwebview_windows"; do
    echo -e "${YELLOW}Restoring path dependencies in $pkg${NC}"
    # Replace versioned dependency with path dependency for platform_interface
    sed -i '' "s|  zikzak_inappwebview_platform_interface: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_platform_interface:\n    path: ../zikzak_inappwebview_platform_interface|g" "$ROOT_DIR/$pkg/pubspec.yaml"
done

# Update main package dependencies
echo -e "${YELLOW}Restoring path dependencies in main package${NC}"
sed -i '' "s|  zikzak_inappwebview_platform_interface: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_platform_interface:\n    path: ../zikzak_inappwebview_platform_interface|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_android: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_android:\n    path: ../zikzak_inappwebview_android|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_ios: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_ios:\n    path: ../zikzak_inappwebview_ios|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_macos: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_macos:\n    path: ../zikzak_inappwebview_macos|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_web: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_web:\n    path: ../zikzak_inappwebview_web|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"
sed -i '' "s|  zikzak_inappwebview_windows: \^[0-9]\+\.[0-9]\+\.[0-9]\+|  zikzak_inappwebview_windows:\n    path: ../zikzak_inappwebview_windows|g" "$ROOT_DIR/zikzak_inappwebview/pubspec.yaml"

echo -e "${GREEN}Successfully restored development setup with path dependencies${NC}"
echo -e "${YELLOW}Run 'flutter pub get' in each package directory to update dependencies${NC}"
