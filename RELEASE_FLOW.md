# Release and versioning model

Hermes is versioned according to [SemVer](http://semver.org/).

## When do we release new version?

### Enhancements and bugfixes

Enhancements and bugfixes are released with each **patch** version. Releasing this version means, that the enhancement
has been tested on our test environment under some considerable load.

### Major features

Major features (e.g. AVRO support) start their life in *incubating* state. While in this state, they might be released
in **patch** versions, but should not be considered production-ready. They are released in stable state as soon
as they are used in Allegro production environment and we are sure there are no major bugs to fix. Stable release means
**minor** version change.

## Incubation period

Each incubating feature is opt-in and must contain feature-switch, which is off by default. During the period of incubation
it is possible that feature contains some bugs. Features like this are not considered stable, for as long as we don't
use them on production environment. They also might not be documented.

## Workflow

**master** branch is the most important branch. It contains **hermes-{version}** tags pointing to stable code that can be run on production.
When the last commit on **master** is untagged then there are commits waiting for release.

### General rules

* squash commits after getting all approves and before merging
* delete unused remote branch after merge, all branches older than 30 days can be removed without prior notice

### Creating enhancement/bugfix

Branch from **master** branch, code, merge back when enhancement/bugfix is ready.

### Creating major feature

Create branch from **master** with short and descriptive name (e.g. *avro_support*). This will be a long-living branch.
Create other branches to code portions of new feature and create pull requests to feature branch to start reviews as
early as possible. When it is finished and with feature switch in place, it will be merged to **master** and scheduled for
next **patch** release in *incubating* state.

## Example workflow

Full workflow from creating an issue to releasing it:

1. I want to code feature: *change subscription endpoint*
2. I create branch *feature/change_subscription_endpoint* from **master** branch
3. I code the feature
4. I test the feature
5. I create pull request from feature branch to **master**
6. After pull request is accepted I squash commits into one, merge it and delete unused feature branch
7. Hermes team member releases Hermes:
    1. Updates CHANGELOG.md about what is new in upcoming release
    2. Tags HEAD of master branch with following version pattern **hermes-x.y.z**
    3. From the tagged code builds and publishes Hermes packages to maven Sonatype repository
