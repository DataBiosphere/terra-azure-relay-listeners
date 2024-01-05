# contributing to terra-azure-relay-listeners

## Updating leonardo

In order to finish releasing your changes, you will need to update `leonardo`
with your commit [git-sha from `main`](https://github.com/DataBiosphere/terra-azure-relay-listeners/commits/main/).

At the time of this writing, there are two files that need to be modified with
the git-sha corresponding to your commit on `main`.

- [reference.conf](https://github.com/databiosphere/leonardo/blob/develop/http/src/main/resources/reference.conf#L219)
- [ConfigReaderSpec.scala](https://github.com/databiosphere/leonardo/blob/develop/http/src/test/scala/org/broadinstitute/dsde/workbench/leonardo/http/ConfigReaderSpec.scala#L71)

To ensure you have covered all references, feel free to execute the following
from `leonardo/`'s root directory:

```shell
git grep terra-azure-relay-listeners
```

