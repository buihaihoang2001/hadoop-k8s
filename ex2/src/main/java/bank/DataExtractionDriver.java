package bank;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Driver - Job 1: BankDataExtraction
 * Chạy: hadoop jar BankDataExtraction.jar bank.DataExtractionDriver
 * Input:  /data/bank/raw   (trên HDFS)
 * Output: /data/bank/output1 (trên HDFS)
 */
public class DataExtractionDriver {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "BankDataExtraction");

        job.setJarByClass(DataExtractionDriver.class);
        job.setMapperClass(DataExtractionMapper.class);
        job.setReducerClass(DataExtractionReducer.class);

        // Output key/value đều là Text
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Hardcode path để tránh lỗi Maven cache args[]
        FileInputFormat.setInputPaths(job, new Path("/data/bank/raw"));
        FileOutputFormat.setOutputPath(job, new Path("/data/bank/output1"));

        if (!job.waitForCompletion(true)) return;
    }
}
