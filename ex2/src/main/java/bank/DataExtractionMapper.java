package bank;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Hàm Map - Job 1: Lọc khách hàng thỏa điều kiện:
 *   - Nghề nghiệp là "businessman"
 *   - Tổng thu nhập >= 300,000$
 *   - Sở hữu ô tô (hasCar = "Y")
 *   - Có cung cấp số điện thoại (hasPhone = "1")
 *
 * Input:  mỗi dòng CSV từ applications.csv
 * Output: ("1", dòng CSV gốc) nếu thỏa điều kiện
 */
public class DataExtractionMapper extends Mapper<LongWritable, Text, Text, Text> {

    public void map(LongWritable ikey, Text ivalue, Context context)
            throws IOException, InterruptedException {
        String row = ivalue.toString();
        String[] cells = row.split(",");
        try {
            // Đọc các trường cần thiết theo index trong CSV
            String incomeType  = cells[12]; // Loại thu nhập / nghề nghiệp
            float  incomeTotal = Float.parseFloat(cells[7]);  // Tổng thu nhập
            String hasCar      = cells[4];  // Có xe không (Y/N)
            String hasPhone    = cells[26]; // Có số điện thoại không (0/1)

            // Điều kiện lọc
            boolean condition = incomeType.toLowerCase().equals("businessman")
                             && incomeTotal >= 300000
                             && hasCar.toLowerCase().equals("y")
                             && hasPhone.equals("1");

            if (condition) {
                // Key = "1" (tất cả về cùng 1 reducer), value = dòng dữ liệu gốc
                context.write(new Text("1"), ivalue);
            }
        } catch (Exception ex) {
            // Bỏ qua dòng lỗi (header hoặc dữ liệu không hợp lệ)
        }
    }
}
