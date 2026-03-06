package wordcount;

import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Hàm Map: Đọc từng dòng văn bản, tách thành từng từ
 * Với mỗi từ, emit cặp (từ, 1)
 *
 * Input:  (offset_dòng, "Deer Bear River")
 * Output: (Deer, 1), (Bear, 1), (River, 1)
 */
public class TokenizerMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        // Tách dòng văn bản thành từng từ
        StringTokenizer itr = new StringTokenizer(value.toString());
        while (itr.hasMoreTokens()) {
            word.set(itr.nextToken());
            // Emit cặp (từ, 1) cho mỗi từ tìm được
            context.write(word, one);
        }
    }
}
