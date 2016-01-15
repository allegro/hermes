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

* **master** branch contains current stable code that can be run on production
* **develop** branch contains code that should be fairly stable, but needs more testing; **release-{version}** branches are created from it
* **release-{version}** branches contain code scheduled and prepared for next release, we merge them to **master**

### General rules

* squash commits after getting all approves and before merging
* delete unused remote branch after merge, all branches older than 30 days can be removed without prior notice

### Creating enhancement/bugfix

Branch from **rc** or **release-{version}** branch (if this is a bugfix for a scheduled release), code, merge back to
the branch that you used in the first place.

### Creating major feature

Create branch from **rc** with short and descriptive name (e.g. *avro_support*). This will be a long-living branch.
Create other branches to code portions of new feature and create pull requests to feature branch to start reviews as
early as possible. When it is finished and with feature switch in place, it will be merged to **rc** and scheduled for
next **patch** release in *incubating* state.

## Example workflow

Full workflow from creating an issue to releasing it:

* i want to code feature: *change subscription endpoint*
* i create branch *feature/change_subscription_endpoint*
* i code the feature
* i test the code create pull request from feature branch to **develop**
* after pul lrequest is accepted, i squash commits, merge it and delete unused feature branch
* i schedule the change for 0.1.1 version
* i create **release-0.1.1** branch from **develop** branch
* i test **release-0.1.1** branch, add changelog etc
* i merge **release-0.1.1** branch to **master**
* i create release from **master** - new stable version is **0.1.1**
