/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sqoop.job.mr;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.security.Credentials;
import org.apache.sqoop.common.ImmutableContext;
import org.apache.sqoop.common.SqoopException;
import org.apache.sqoop.job.JobConstants;
import org.apache.sqoop.job.etl.Loader;
import org.apache.sqoop.job.io.Data;
import org.apache.sqoop.job.io.DataReader;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ConcurrentModificationException;
import java.util.concurrent.BrokenBarrierException;

public class TestSqoopOutputFormatLoadExecutor {

  private Configuration conf;

  public static class ThrowingLoader extends Loader {

    public ThrowingLoader() {

    }

    @Override
    public void load(ImmutableContext context, Object connectionConfiguration,
                     Object jobConfiguration, DataReader reader) throws Exception {
      reader.readContent(Data.CSV_RECORD);
      throw new BrokenBarrierException();
    }
  }

  public static class ThrowingContinuousLoader extends Loader {

    public ThrowingContinuousLoader() {
    }

    @Override
    public void load(ImmutableContext context, Object connectionConfiguration,
                     Object jobConfiguration, DataReader reader) throws Exception {
      int runCount = 0;
      Object o;
      String[] arr;
      while ((o = reader.readContent(Data.CSV_RECORD)) != null) {
        arr = o.toString().split(",");
        Assert.assertEquals(100, arr.length);
        for (int i = 0; i < arr.length; i++) {
          Assert.assertEquals(i, Integer.parseInt(arr[i]));
        }
        runCount++;
        if (runCount == 5) {
          throw new ConcurrentModificationException();
        }
      }
    }
  }

  public static class GoodLoader extends Loader {

    public GoodLoader() {

    }

    @Override
    public void load(ImmutableContext context, Object connectionConfiguration,
                     Object jobConfiguration, DataReader reader) throws Exception {
      String[] arr = reader.readContent(Data.CSV_RECORD).toString().split(",");
      Assert.assertEquals(100, arr.length);
      for (int i = 0; i < arr.length; i++) {
        Assert.assertEquals(i, Integer.parseInt(arr[i]));
      }
    }
  }

  public static class GoodContinuousLoader extends Loader {

    public GoodContinuousLoader() {

    }

    @Override
    public void load(ImmutableContext context, Object connectionConfiguration,
                     Object jobConfiguration, DataReader reader) throws Exception {
      int runCount = 0;
      Object o;
      String[] arr;
      while ((o = reader.readContent(Data.CSV_RECORD)) != null) {
        arr = o.toString().split(",");
        Assert.assertEquals(100, arr.length);
        for (int i = 0; i < arr.length; i++) {
          Assert.assertEquals(i, Integer.parseInt(arr[i]));
        }
        runCount++;
      }
      Assert.assertEquals(10, runCount);
    }
  }


  @Before
  public void setUp() {
    conf = new Configuration();

  }

  @Test(expected = BrokenBarrierException.class)
  public void testWhenLoaderThrows() throws Throwable {
    conf.set(JobConstants.JOB_TYPE, "EXPORT");
    conf.set(JobConstants.JOB_ETL_LOADER, ThrowingLoader.class.getName());
    SqoopOutputFormatLoadExecutor executor = new
        SqoopOutputFormatLoadExecutor(getJobContext());
    RecordWriter<Data, NullWritable> writer = executor.getRecordWriter();
    Data data = new Data();
    try {
      for (int count = 0; count < 100; count++) {
        data.setContent(String.valueOf(count), Data.CSV_RECORD);
        writer.write(data, null);
      }
    } catch (SqoopException ex) {
      throw ex.getCause();
    }
  }

  @Test
  public void testSuccessfulContinuousLoader() throws Throwable {
    conf.set(JobConstants.JOB_TYPE, "EXPORT");
    conf.set(JobConstants.JOB_ETL_LOADER, GoodContinuousLoader.class.getName());
    SqoopOutputFormatLoadExecutor executor = new
        SqoopOutputFormatLoadExecutor(getJobContext());
    RecordWriter<Data, NullWritable> writer = executor.getRecordWriter();
    Data data = new Data();
    for (int i = 0; i < 10; i++) {
      StringBuilder builder = new StringBuilder();
      for (int count = 0; count < 100; count++) {
        builder.append(String.valueOf(count));
        if (count != 99) {
          builder.append(",");
        }
      }
      data.setContent(builder.toString(), Data.CSV_RECORD);
      writer.write(data, null);
    }
    writer.close(getJobContext());
  }

  @Test
  public void testSuccessfulLoader() throws Throwable {
    conf.set(JobConstants.JOB_TYPE, "EXPORT");
    conf.set(JobConstants.JOB_ETL_LOADER, GoodLoader.class.getName());
    SqoopOutputFormatLoadExecutor executor = new
        SqoopOutputFormatLoadExecutor(getJobContext());
    RecordWriter<Data, NullWritable> writer = executor.getRecordWriter();
    Data data = new Data();
    StringBuilder builder = new StringBuilder();
    for (int count = 0; count < 100; count++) {
      builder.append(String.valueOf(count));
      if (count != 99) {
        builder.append(",");
      }
    }
    data.setContent(builder.toString(), Data.CSV_RECORD);
    writer.write(data, null);
    writer.close(getJobContext());
  }


  @Test(expected = ConcurrentModificationException.class)
  public void testThrowingContinuousLoader() throws Throwable {
    conf.set(JobConstants.JOB_TYPE, "EXPORT");
    conf.set(JobConstants.JOB_ETL_LOADER, ThrowingContinuousLoader.class.getName());
    SqoopOutputFormatLoadExecutor executor = new
        SqoopOutputFormatLoadExecutor(getJobContext());
    RecordWriter<Data, NullWritable> writer = executor.getRecordWriter();
    Data data = new Data();
    try {
      for (int i = 0; i < 10; i++) {
        StringBuilder builder = new StringBuilder();
        for (int count = 0; count < 100; count++) {
          builder.append(String.valueOf(count));
          if (count != 99) {
            builder.append(",");
          }
        }
        data.setContent(builder.toString(), Data.CSV_RECORD);
        writer.write(data, null);
      }
      writer.close(getJobContext());
    } catch (SqoopException ex) {
      throw ex.getCause();
    }
  }


  private TaskAttemptContext getJobContext() {
    TaskAttemptContext context = new TaskAttemptContext() {
      @Override
      public Configuration getConfiguration() {
        return conf;
      }

      @Override
      public Credentials getCredentials() {
        return null;
      }

      @Override
      public JobID getJobID() {
        return null;
      }

      @Override
      public int getNumReduceTasks() {
        return 0;
      }

      @Override
      public Path getWorkingDirectory() throws IOException {
        return null;
      }

      @Override
      public Class<?> getOutputKeyClass() {
        return null;
      }

      @Override
      public Class<?> getOutputValueClass() {
        return null;
      }

      @Override
      public Class<?> getMapOutputKeyClass() {
        return null;
      }

      @Override
      public Class<?> getMapOutputValueClass() {
        return null;
      }

      @Override
      public String getJobName() {
        return null;
      }

      @Override
      public Class<? extends InputFormat<?, ?>> getInputFormatClass() throws ClassNotFoundException {
        return null;
      }

      @Override
      public Class<? extends Mapper<?, ?, ?, ?>> getMapperClass() throws ClassNotFoundException {
        return null;
      }

      @Override
      public Class<? extends Reducer<?, ?, ?, ?>> getCombinerClass() throws ClassNotFoundException {
        return null;
      }

      @Override
      public Class<? extends Reducer<?, ?, ?, ?>> getReducerClass() throws ClassNotFoundException {
        return null;
      }

      @Override
      public Class<? extends OutputFormat<?, ?>> getOutputFormatClass() throws ClassNotFoundException {
        return null;
      }

      @Override
      public Class<? extends Partitioner<?, ?>> getPartitionerClass() throws ClassNotFoundException {
        return null;
      }

      @Override
      public RawComparator<?> getSortComparator() {
        return null;
      }

      @Override
      public String getJar() {
        return null;
      }

      @Override
      public RawComparator<?> getGroupingComparator() {
        return null;
      }

      @Override
      public boolean getJobSetupCleanupNeeded() {
        return false;
      }

      @Override
      public boolean getTaskCleanupNeeded() {
        return false;
      }

      @Override
      public boolean getProfileEnabled() {
        return false;
      }

      @Override
      public String getProfileParams() {
        return null;
      }

      @Override
      public Configuration.IntegerRanges getProfileTaskRange(boolean isMap) {
        return null;
      }

      @Override
      public String getUser() {
        return null;
      }

      @Override
      public boolean getSymlink() {
        return false;
      }

      @Override
      public Path[] getArchiveClassPaths() {
        return new Path[0];
      }

      @Override
      public URI[] getCacheArchives() throws IOException {
        return new URI[0];
      }

      @Override
      public URI[] getCacheFiles() throws IOException {
        return new URI[0];
      }

      @Override
      public Path[] getLocalCacheArchives() throws IOException {
        return new Path[0];
      }

      @Override
      public Path[] getLocalCacheFiles() throws IOException {
        return new Path[0];
      }

      @Override
      public Path[] getFileClassPaths() {
        return new Path[0];
      }

      @Override
      public String[] getArchiveTimestamps() {
        return new String[0];
      }

      @Override
      public String[] getFileTimestamps() {
        return new String[0];
      }

      @Override
      public int getMaxMapAttempts() {
        return 0;
      }

      @Override
      public int getMaxReduceAttempts() {
        return 0;
      }

      @Override
      public TaskAttemptID getTaskAttemptID() {
        return null;
      }

      @Override
      public void setStatus(String msg) {

      }

      @Override
      public String getStatus() {
        return null;
      }

      @Override
      public float getProgress() {
        return 0;
      }

      @Override
      public Counter getCounter(Enum<?> counterName) {
        return null;
      }

      @Override
      public Counter getCounter(String groupName, String counterName) {
        return null;
      }

      @Override
      public void progress() {

      }
    };
    return context;
  }
}