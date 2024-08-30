#!/bin/bash

JAVA_FILES=$(find . -name "*.java" -type f)

invalid_files=0

echo "Following files are formatted incorrectly:";
for FILE in $JAVA_FILES; do
  java -jar "google-java-format-$1-all-deps.jar" --set-exit-if-changed "$FILE" > /dev/null
  if [ $? -ne 0 ]; then
    echo "$FILE"
    ((invalid_files++))
  fi
done

if [ "$invalid_files" -ne 0 ]; then
  echo "Found $invalid_files incorrectly formatted files (listed above), run google-java-format to fix them.";
  exit 1
else
   echo "All files are formatted correctly."
fi

