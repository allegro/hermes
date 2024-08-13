PULL_EVERY=0
DIR="e2e_runs"

while :; do
    case $1 in
        --pull-every)
            PULL_EVERY=1
            ;;
         --dir)
            if [ "$2" ]; then
                DIR=$2
                shift
            else
                die 'ERROR: "--dir" requires a non-empty option argument.'
            fi
            ;;
        --)
            shift
            break
            ;;
        -?*)
            printf 'WARN: Unknown option (ignored): %s\n' "$1" >&2
            ;;
        *)
            break
    esac
    shift
done


ROOT_DIR="$(pwd)/$DIR"
CONFLUENT_IMAGES_TAG="7.2.2"

echo "root dir: $ROOT_DIR"
echo "confluent images tags: $CONFLUENT_IMAGES_TAG"
echo "pull every: $PULL_EVERY"

mkdir -p $ROOT_DIR
chmod +x gradlew
for i in run{1..50};do

  if [[ "$PULL_EVERY" -eq 1 ]]; then
    echo "Pruning docker"
    rm -rf .gradle
    docker container rm -f $(docker container ls -aq) || true
    docker image rm -f $(docker image ls -aq) || true
    docker system prune --volumes --force || true
  fi

  RUN_DIR="$ROOT_DIR/$i"
  mkdir -p $RUN_DIR
  echo "Run id: $i, run dir: $RUN_DIR"
  find .gradle -type f -name "*.lock" -delete
  ./gradlew clean integrationTest -PtestBuildDir=$RUN_DIR -PconfluentImagesTag=$CONFLUENT_IMAGES_TAG &> "$RUN_DIR/logs.txt" || true
  echo "Run finished"
done