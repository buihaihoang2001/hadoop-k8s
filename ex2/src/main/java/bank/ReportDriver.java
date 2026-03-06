package bank;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ReportDriver {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "BankLoansReport");

        job.setJarByClass(ReportDriver.class);
        job.setMapperClass(ReportMapper.class);
        job.setReducerClass(ReportReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.setInputPaths(job, new Path("/data/bank/raw"));
        FileOutputFormat.setOutputPath(job, new Path("/data/bank/output2"));

        if (!job.waitForCompletion(true)) return;
    }
}
