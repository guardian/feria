# Feria

> And henceforth every day shall be a *feria*, and you shall make a sacrifice to the great god Janus and receive credentials in return.
>
> -- Chris

Uses Selenium to access Janus, retrieve developer credentials for a given AWS account and update the corresponding profile credentials.

## How to install

1. Clone this repo

2. (Optional) Add the bin directory to your `PATH` by running [`install.sh`](./install.sh)

## How to run

Whenever you want to update your profile credentials, run Feria and pass it the name of the profile. e.g.

```
$ feria capi
```

It will take a while the first time you run it, because it has to build itself.

## Usage

```
Usage: feria [options] profiles

  --access <value>
        The type of access you need. One of [s3-all, lambda, dev, cloudformation, sqs-consumer, kinesis-read, s3-read]. Default = dev
  profiles
        The AWS profile ID(s), e.g. capi frontend
```

## Requirements/restrictions

* Expects `sbt` and `aws` to be on the `PATH`
* Currently only supports Firefox. Expects you to be logged in to Google in Firefox's default profile.
