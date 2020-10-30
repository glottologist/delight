# Installation steps for AWS EMR

This document details instructions to install Delight on AWS EMR.

It assumes that you have created an account and generated an API key on the [Delight website](https://www.datamechanics.co/delight).

There are two ways (or "launch modes") to run a EMR spark cluster:

- the Step execution launch mode creates an ephemeral cluster to run operations on it,
- the Cluster launch mode creates a cluster that you can connect to to run Spark applications.

We detail instructions for both cases below.

## First option: Step execution launch mode

When creating a cluster in Step execution launch mode, AWS EMR lets you add a [Spark Step](https://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-spark-submit-step.html) to run a Spark application:

![spark application step on EMR](images/emr_step.png)

In the configuration window of the Spark step, add the following lines in the text box named `Spark-submit options`:

```java
--packages co.datamechanics:delight_2.12:1.2.3
--conf spark.extraListeners=co.datamechanics.delight.DelightListener
--conf spark.delight.apiKey.secret=<your-api-key>
```

![configure spark application step on EMR](images/emr_step_configure.png)

## Second option: Connect to the master node

Follow instructions to [connect to the master node over SSH](https://docs.aws.amazon.com/emr/latest/ManagementGuide/emr-connect-master-node.html).

Once connected, the `spark-submit` CLI will be available to you.
Please follow the [instructions to install Delight with the `spark-submit` CLI](spark_submit.md).
