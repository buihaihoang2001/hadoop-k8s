package bank;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


public class ReportMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

    private static final IntWritable one = new IntWritable(1);

    public void map(LongWritable ikey, Text ivalue, Context context)
            throws IOException, InterruptedException {
        String row = ivalue.toString();
        String[] cells = row.split(",");
        try {
            int daysBirth = Integer.parseInt(cells[17]);
            String ageRange = toAgeRange(daysBirth);
            context.write(new Text(ageRange), one);
        } catch (Exception ex) {
        }
    }

    /**
     * Chuyển DAYS_BIRTH thành nhóm tuổi
     * @param daysbirth số ngày từ ngày sinh (âm)
     * @return nhóm tuổi dạng String
     */
    private String toAgeRange(int daysbirth) {
        double age = Math.ceil(Math.abs(daysbirth) / 365.0);
        return age < 30  ? "<30"      :
               age < 45  ? "[30,45)"  :
               age < 60  ? "[45,60)"  : ">=60";
    }
}
