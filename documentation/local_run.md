# Tutorial: run a local Spark app with Delight

This document assumes that you have created an account and generated an API key on the [Delight website](https://www.datamechanics.co/delight).

You can try Delight with a local run on your machine!
[Download a Spark distribution](https://spark.apache.org/downloads.html) and run an instrumented Spark submit from the root folder.

Here's a working example for Spark 3.0.1:

```bash
# From the root folder of the Spark 3.0.1 distribution
./bin/spark-submit \
  --class org.apache.spark.examples.SparkPi \
  --master 'local[2]' \
  --packages co.datamechanics:delight_2.12:1.2.3 \ # TODO
  --conf spark.delight.apiKey.secret=<replace-with-your-api-key> \
  --conf spark.extraListeners=co.datamechanics.delight.DelightListener \
  examples/jars/spark-examples_2.12-3.0.1.jar \
  100
```
