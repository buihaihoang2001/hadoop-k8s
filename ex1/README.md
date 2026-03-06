# Bài tập 1 — MapReduce WordCount

## Mô tả bài toán

Viết chương trình MapReduce đếm số lần xuất hiện của mỗi từ trong tập văn bản.

```
Input:              Output:
Deer Bear River     Bear    2
Car Car River   →   Car     3
Deer Car Bear       Deer    2
                    River   2
```

---

## Cấu trúc thư mục

```
ex1/
├── src/main/java/wordcount/
│   ├── TokenizerMapper.java   # Hàm Map
│   ├── IntSumReducer.java     # Hàm Reduce
│   └── WordCountDriver.java   # Hàm main (Driver)
├── input.txt                  # File dữ liệu mẫu
├── pom.xml                    # Maven build config
└── README.md
```

---

## Giải thích luồng xử lý MapReduce

```
input.txt
    │
    ▼ [MAP phase]
    Mỗi dòng → tách từng từ → emit (từ, 1)
    "Deer Bear River" → (Deer,1), (Bear,1), (River,1)
    "Car Car River"   → (Car,1), (Car,1), (River,1)
    "Deer Car Bear"   → (Deer,1), (Car,1), (Bear,1)
    │
    ▼ [SHUFFLE & SORT phase — Hadoop tự xử lý]
    Gom các cặp có cùng key lại:
    (Bear,  [1,1])
    (Car,   [1,1,1])
    (Deer,  [1,1])
    (River, [1,1])
    │
    ▼ [REDUCE phase]
    Cộng tổng từng nhóm → emit (từ, tổng)
    (Bear, 2), (Car, 3), (Deer, 2), (River, 2)
    │
    ▼
output/part-r-00000
```

---

## Giải thích từng file Java

### `TokenizerMapper.java` — Hàm Map

| Thành phần | Giải thích |
|---|---|
| `extends Mapper<LongWritable, Text, Text, IntWritable>` | Input: (offset dòng, nội dung dòng) → Output: (từ, số nguyên) |
| `StringTokenizer` | Tách chuỗi thành từng token (từ) theo khoảng trắng |
| `context.write(word, one)` | Emit cặp (từ, 1) cho mỗi từ tìm được |

### `IntSumReducer.java` — Hàm Reduce

| Thành phần | Giải thích |
|---|---|
| `extends Reducer<Text, IntWritable, Text, IntWritable>` | Input: (từ, danh sách số) → Output: (từ, tổng) |
| `for (IntWritable val : values)` | Duyệt qua tất cả giá trị 1 của cùng một từ |
| `context.write(key, result)` | Emit kết quả cuối: (từ, tổng số lần xuất hiện) |

### `WordCountDriver.java` — Hàm Main

| Thành phần | Giải thích |
|---|---|
| `Job.getInstance(conf, "WordCountApp")` | Tạo một MapReduce job mới |
| `job.setMapperClass / setReducerClass` | Gán Mapper và Reducer cho job |
| `job.setOutputKeyClass / setOutputValueClass` | Khai báo kiểu dữ liệu đầu ra |
| `FileInputFormat.setInputPaths` | Chỉ định thư mục input trên HDFS |
| `FileOutputFormat.setOutputPath` | Chỉ định thư mục output trên HDFS |
| `job.waitForCompletion(true)` | Submit job và chờ kết quả |

### `pom.xml` — Maven Build Config

| Thành phần | Giải thích |
|---|---|
| `hadoop-client 3.3.0` | Thư viện Hadoop cần để compile (scope `provided` vì Hadoop đã có sẵn trên cluster) |
| `maven-jar-plugin` | Plugin đóng gói thành file `.jar`, chỉ định `mainClass` là `WordCountDriver` |

---

## Hướng dẫn chạy trên Hadoop Cluster (k8s/k3s)

### Yêu cầu
- Ubuntu server đã cài k3s, Hadoop cluster đang chạy (`hadoop-master` pod trong namespace `hadoop`)
- Maven + JDK 8 đã cài trên Ubuntu server

### Bước 1: Cài Maven và JDK (nếu chưa có)
```bash
sudo apt update
sudo apt install -y maven openjdk-8-jdk
```

### Bước 2: Build JAR
```bash
cd ex1/
mvn clean package -DskipTests
# Output: target/wordcount-1.0.jar
```

### Bước 3: Copy file vào master pod
```bash
kubectl cp input.txt hadoop/hadoop-master:/tmp/input.txt
kubectl cp target/wordcount-1.0.jar hadoop/hadoop-master:/tmp/wordcount.jar
```

### Bước 4: Upload input lên HDFS
```bash
kubectl exec -it hadoop-master -n hadoop -- bash -c "
  /opt/hadoop/bin/hdfs dfs -mkdir -p /input
  /opt/hadoop/bin/hdfs dfs -put /tmp/input.txt /input/
  /opt/hadoop/bin/hdfs dfs -ls /input
"
```

### Bước 5: Chạy MapReduce job
```bash
kubectl exec -it hadoop-master -n hadoop -- bash -c "
  /opt/hadoop/bin/hadoop jar /tmp/wordcount.jar wordcount.WordCountDriver /input /output
"
```

### Bước 6: Xem kết quả
```bash
kubectl exec -it hadoop-master -n hadoop -- \
  /opt/hadoop/bin/hdfs dfs -cat /output/part-r-00000
```

Kết quả mong đợi:
```
Bear    2
Car     3
Deer    2
River   2
```

### Chạy lại (nếu cần xóa output cũ)
```bash
kubectl exec -it hadoop-master -n hadoop -- \
  /opt/hadoop/bin/hdfs dfs -rm -r /output
```

### Theo dõi job
- YARN ResourceManager: http://10.10.80.10:30088
- JobHistory Server:     http://10.10.80.10:31988

---

## So sánh với slide gốc

| Slide (Eclipse + Linux VM) | Thực tế (Maven + k8s) |
|---|---|
| Tạo project qua Eclipse GUI | Tạo thư mục + `pom.xml` thủ công |
| Build bằng Eclipse Export JAR | Build bằng `mvn clean package` |
| Copy JAR vào máy master (SCP) | Copy vào pod bằng `kubectl cp` |
| Chạy `hadoop jar` trên máy master | Chạy `hadoop jar` bên trong pod |
| Hadoop ở `/usr/local/hadoop` | Hadoop ở `/opt/hadoop` (trong container) |

> **Kết quả cuối cùng giống nhau hoàn toàn** — chỉ khác cách build và deploy.
