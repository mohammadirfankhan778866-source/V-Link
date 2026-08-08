#!/bin/bash
# Firebase Connection Verification & Diagnostics Script for Pulse Chat

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0;m' # No Color

echo -e "${BLUE}===================================================================${NC}"
echo -e "${BLUE}          Firebase & Google Sign-In Diagnostics Script            ${NC}"
echo -e "${BLUE}===================================================================${NC}"

# Define constants
EXPECTED_APP_ID="com.aistudio.pulsechat.kxmpzq"
EXPECTED_PROJECT_ID="v-link-b259e"

# 1. Check if google-services.json exists
echo -e "\n${BLUE}[1/5] Verifying google-services.json Existence...${NC}"
GS_FILE="app/google-services.json"
if [ -f "$GS_FILE" ]; then
    echo -e "${GREEN}✔ Found google-services.json at $GS_FILE${NC}"
else
    echo -e "${RED}✘ google-services.json is MISSING from $GS_FILE!${NC}"
    exit 1
fi

# 2. Check Package Name (applicationId) in google-services.json
echo -e "\n${BLUE}[2/5] Parsing google-services.json Package Name...${NC}"
PACKAGE_NAME=$(grep -o '"package_name": "[^"]*"' "$GS_FILE" | head -n 1 | cut -d'"' -f4)
if [ "$PACKAGE_NAME" = "$EXPECTED_APP_ID" ]; then
    echo -e "${GREEN}✔ Package name in google-services.json matches: '$PACKAGE_NAME' (Expected: '$EXPECTED_APP_ID')${NC}"
else
    echo -e "${RED}✘ Package name mismatch! Found: '$PACKAGE_NAME', Expected: '$EXPECTED_APP_ID'${NC}"
    exit 1
fi

# 3. Check Firebase Project ID in google-services.json
echo -e "\n${BLUE}[3/5] Parsing google-services.json Project ID...${NC}"
PROJECT_ID=$(grep -o '"project_id": "[^"]*"' "$GS_FILE" | head -n 1 | cut -d'"' -f4)
if [ "$PROJECT_ID" = "$EXPECTED_PROJECT_ID" ]; then
    echo -e "${GREEN}✔ Project ID in google-services.json matches: '$PROJECT_ID' (Expected: '$EXPECTED_PROJECT_ID')${NC}"
else
    echo -e "${RED}✘ Project ID mismatch! Found: '$PROJECT_ID', Expected: '$EXPECTED_PROJECT_ID'${NC}"
    exit 1
fi

# 4. Check Gradle Configurations (applicationId and plugins)
echo -e "\n${BLUE}[4/5] Verifying Gradle Configurations...${NC}"
APP_GRADLE="app/build.gradle.kts"

# Check applicationId in build.gradle.kts
GRADLE_APP_ID=$(grep -o 'applicationId = "[^"]*"' "$APP_GRADLE" | head -n 1 | cut -d'"' -f2)
if [ "$GRADLE_APP_ID" = "$EXPECTED_APP_ID" ]; then
    echo -e "${GREEN}✔ applicationId in $APP_GRADLE matches: '$GRADLE_APP_ID'${NC}"
else
    echo -e "${RED}✘ applicationId mismatch in $APP_GRADLE! Found: '$GRADLE_APP_ID', Expected: '$EXPECTED_APP_ID'${NC}"
    exit 1
fi

# Check Google Services plugin in app/build.gradle.kts
if grep -q "google.services" "$APP_GRADLE"; then
    echo -e "${GREEN}✔ Google Services plugin is applied in $APP_GRADLE${NC}"
else
    echo -e "${RED}✘ Google Services plugin is NOT applied in $APP_GRADLE!${NC}"
    exit 1
fi

# Check root build.gradle.kts plugin inclusion
ROOT_GRADLE="build.gradle.kts"
if grep -q "google.services" "$ROOT_GRADLE"; then
    echo -e "${GREEN}✔ Google Services plugin is declared in root $ROOT_GRADLE${NC}"
else
    echo -e "${RED}✘ Google Services plugin is NOT declared in root $ROOT_GRADLE!${NC}"
    exit 1
fi

# 5. Check Source Code Initialization Safety
echo -e "\n${BLUE}[5/5] Verifying Source Code Initialization Safety...${NC}"
APP_INIT_FILE="app/src/main/java/com/example/PulseApplication.kt"
if grep -q "com.google.firebase.FirebaseApp.initializeApp" "$APP_INIT_FILE"; then
    echo -e "${GREEN}✔ FirebaseApp.initializeApp is called in PulseApplication.onCreate()${NC}"
else
    echo -e "${RED}✘ FirebaseApp.initializeApp is MISSING in $APP_INIT_FILE!${NC}"
    exit 1
fi

# Check diagnostics run
if grep -q "FirebaseDiagnostics.runDiagnostics" "$APP_INIT_FILE"; then
    echo -e "${GREEN}✔ FirebaseDiagnostics verification is hooked into application startup${NC}"
else
    echo -e "${YELLOW}⚠ FirebaseDiagnostics is not currently hooked in PulseApplication${NC}"
fi

echo -e "\n${GREEN}===================================================================${NC}"
echo -e "${GREEN}             ALL CONNECTION & INTEGRATION CHECKS PASSED!            ${NC}"
echo -e "${GREEN}===================================================================${NC}"
