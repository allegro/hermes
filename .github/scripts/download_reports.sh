TMPDIR="test"
mkdir $TMPDIR

REPO="allegro/hermes"
BUILDS_FILE="builds.json"
PAST_BUILDS="$TMPDIR/$BUILDS_FILE"

UNIT_TEST_ARTIFACT="check-test-report"
E2E_REPORT_ARTIFACT="integrationTest-test-report"

gh run list --repo $REPO --branch master --workflow ci --json "status,databaseId" --limit 20 >> $PAST_BUILDS

cd $TMPDIR

jq -c '.[]' "$BUILDS_FILE" | while read i; do
    STATUS=$(echo $i | jq '.status')
    if [[ "$STATUS" == 'completed' ]]; then
        continue
    fi
    RUN_ID=$(echo $i | jq '.databaseId')

    echo "downloading results for run: $RUN_ID"
    RUN_DIR=$RUN_ID

    mkdir $RUN_DIR
    echo "creating dir $RUN_DIR"
    cd $RUN_DIR

    mkdir $UNIT_TEST_ARTIFACT
    cd $UNIT_TEST_ARTIFACT
    gh run download --repo $REPO -n $UNIT_TEST_ARTIFACT $RUN_ID
    echo "Downloaded unit test report"
    cd ..

    mkdir $E2E_REPORT_ARTIFACT
    cd $E2E_REPORT_ARTIFACT
    gh run download --repo $REPO -n $E2E_REPORT_ARTIFACT $RUN_ID
    echo "Downloaded integrationTest report"

    cd ../..
done
