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
 * Sử dụng: hadoop jar wordcount-1.0.jar wordcount.WordCountDriver /input /output
 */
public class WordCountDriver {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        // Tạo job với tên "WordCountApp"
        Job job = Job.getInstance(conf, "WordCountApp");

        // Chỉ định class chứa file JAR
        job.setJarByClass(WordCountDriver.class);

        // Gán Mapper và Reducer
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);

        // Kiểu dữ liệu output: key = Text (từ), value = IntWritable (số đếm)
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // Đường dẫn input/output trên HDFS (truyền từ args)
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Chờ job hoàn thành
        if (!job.waitForCompletion(true)) return;
    }
}
