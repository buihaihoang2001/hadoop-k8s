package wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Driver (hàm main): Kết nối Mapper và Reducer, cấu hình và submit job
 *
 * Chạy: hadoop jar wordcount-1.0.jar wordcount.WordCountDriver
 * Input:  /input  (thư mục trên HDFS)
 * Output: /output (thư mục trên HDFS - không được tồn tại trước)
 */
public class WordCountDriver {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf, "WordCountApp");

        job.setJarByClass(WordCountDriver.class);

        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.setInputPaths(job, new Path("/input"));
        FileOutputFormat.setOutputPath(job, new Path("/output"));

        if (!job.waitForCompletion(true)) return;
    }
}
