package wordcount;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Hàm Reduce: Nhận danh sách các giá trị của cùng một từ, cộng tổng lại
 *
 * Input:  (Deer, [1, 1])
 * Output: (Deer, 2)
 */
public class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values, Context context)
            throws IOException, InterruptedException {
        int sum = 0;
        // Cộng tất cả giá trị 1 của cùng một từ
        for (IntWritable val : values) {
            sum += val.get();
        }
        result.set(sum);
        // Emit kết quả cuối: (từ, tổng_số_lần_xuất_hiện)
        context.write(key, result);
    }
}
