# Contributors guide

## Picking up issues

If you want to code something small in Hermes, search for `ideal-for-contribution` tagged issues.

## Commit message format

```
resolve #<issue number> | <commit message>
```

i.e. `resolve #123 | my commit message`

You can also use other *magic words* from [GitHub handbook](https://help.github.com/articles/closing-issues-via-commit-messages/).

## Pull requests

* for small changes, no need to add separate issue, defining problem in pull request is enough
* if issue exists, reference it from PR title or description using GitHub magic words like *resolves #issue-number*
* create pull requests to **master** branch
* it would be nice to squash commits before creating pull requests
* it's required to squash commits before merge
* pay attention to changing documentation in `docs/`

## Coding style

* use `spock` when writing new unit tests in all modules
* when changing old tests use your best judgement as to when rewrite them to `spock`
* use `JUnit5` with defined environment in `integration-tests` module
* prepend configuration options with module name, i.e. `frontend.` or `consumer.` when it applies to single module
