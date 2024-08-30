#!/bin/bash
set -eu

script_name=""

case "$(uname -sr)" in

   Darwin*)
     script_name="google-java-format_darwin-arm64"
     ;;

   Linux*)
     script_name="google-java-format_linux-x86-64"
     ;;
   *)
     echo 'Unsupported OS'
     exit 1
     ;;
esac

JAVA_FILES=$(find . -name "*.java" -type f)

invalid_files=0

echo "Following files are formatted incorrectly:";
# TODO: remove '--skip-reflowing-long-strings' once https://github.com/google/google-java-format/issues/566 is fixed
for FILE in $JAVA_FILES; do
  ./$script_name -r --skip-reflowing-long-strings "$FILE" > /dev/null
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
