# Bài tập 2 — MapReduce Bank Data Analysis

## Mô tả bài toán

Ngân hàng có file `applications.csv` chứa dữ liệu hồ sơ vay tiền (hàng chục triệu dòng, ~30GB).
Thực hiện 2 job MapReduce:

| Job | Tên | Mục tiêu |
|---|---|---|
| Job 1 | BankDataExtraction | Lọc khách hàng: businessman + thu nhập ≥ 300k$ + có xe + có phone |
| Job 2 | BankDataReport | Thống kê số lượng hồ sơ theo nhóm tuổi |

---

## Cấu trúc thư mục

```
ex2/
├── src/main/java/bank/
│   ├── DataExtractionMapper.java  # Map  - Job 1
│   ├── DataExtractionReducer.java # Reduce - Job 1
│   ├── DataExtractionDriver.java  # Main - Job 1
│   ├── ReportMapper.java          # Map  - Job 2
│   ├── ReportReducer.java         # Reduce - Job 2
│   └── ReportDriver.java          # Main - Job 2
├── applications.csv               # File dữ liệu mẫu
├── pom.xml                        # Maven build config
└── README.md
```

---

## Job 1 — BankDataExtraction

### Luồng xử lý

```
applications.csv (mỗi dòng)
    │
    ▼ [MAP - DataExtractionMapper]
    Đọc các cột:
      cells[12] = incomeType  → phải là "businessman"
      cells[7]  = incomeTotal → phải >= 300000
      cells[4]  = hasCar      → phải là "Y"
      cells[26] = hasPhone    → phải là "1"
    
    Nếu THỎA → emit ("1", dòng_CSV_gốc)
    Nếu KHÔNG → bỏ qua
    │
    ▼ [SHUFFLE & SORT — Hadoop tự xử lý]
    ("1", [dòng_A, dòng_B, dòng_C, ...])
    │
    ▼ [REDUCE - DataExtractionReducer]
    Ghi từng dòng ra output:
    context.write(null, dòng_A)
    context.write(null, dòng_B)
    ...
    │
    ▼
/data/bank/output1/part-r-00000
(danh sách các dòng khách hàng thỏa điều kiện)
```

### Giải thích code

| Thành phần | Giải thích |
|---|---|
| `cells[12]` | Cột NAME_INCOME_TYPE — loại nghề nghiệp |
| `cells[7]` | Cột AMT_INCOME_TOTAL — tổng thu nhập |
| `cells[4]` | Cột FLAG_OWN_CAR — "Y" = có xe |
| `cells[26]` | Cột FLAG_PHONE — "1" = có số điện thoại |
| `context.write("1", ivalue)` | Dùng key cố định "1" để gom tất cả vào 1 reducer |
| `context.write(null, val)` | Reducer ghi ra không có key — chỉ cần danh sách dòng |

---

## Job 2 — BankDataReport

### Luồng xử lý

```
applications.csv (mỗi dòng)
    │
    ▼ [MAP - ReportMapper]
    Đọc cells[17] = DAYS_BIRTH (số âm, VD: -16425)
    Tính tuổi: age = ceil(abs(DAYS_BIRTH) / 365.0)
    Phân nhóm:
      age < 30  → "<30"
      age < 45  → "[30,45)"
      age < 60  → "[45,60)"
      age >= 60 → ">=60"
    
    emit (nhóm_tuổi, 1)
    │
    ▼ [SHUFFLE & SORT]
    ("<30",    [1,1,1,...])
    ("[30,45)",[1,1,1,...])
    ("[45,60)",[1,1,1,...])
    (">=60",   [1,1,1,...])
    │
    ▼ [REDUCE - ReportReducer]
    Cộng tổng mỗi nhóm
    │
    ▼
/data/bank/output2/part-r-00000
<30      1234
[30,45)  5678
[45,60)  9012
>=60     3456
```

### Giải thích tính tuổi

```java
// DAYS_BIRTH = -16425 nghĩa là sinh cách đây 16425 ngày
double age = Math.ceil(Math.abs(-16425) / 365.0);
// = Math.ceil(16425 / 365.0)
// = Math.ceil(44.99)
// = 45 tuổi → nhóm "[45,60)"
```

---

## Hướng dẫn chạy

### Bước 1: Build 2 JAR

```bash
cd ex2/
mvn clean package -DskipTests
# Output:
#   target/BankDataExtraction.jar
#   target/BankDataReport.jar
```

### Bước 2: Copy JAR + CSV vào master pod

```bash
# Copy file dữ liệu
kubectl cp applications.csv hadoop/hadoop-master:/tmp/applications.csv

# Copy 2 JAR
kubectl cp target/BankDataExtraction-extraction.jar hadoop/hadoop-master:/tmp/BankDataExtraction.jar
kubectl cp target/BankDataReport-report.jar hadoop/hadoop-master:/tmp/BankDataReport.jar```

### Bước 3: Upload dữ liệu lên HDFS

```bash
kubectl exec -it hadoop-master -n hadoop -- bash -c "
  /opt/hadoop/bin/hdfs dfs -mkdir -p /data/bank/raw
  /opt/hadoop/bin/hdfs dfs -put /tmp/applications.csv /data/bank/raw/
  /opt/hadoop/bin/hdfs dfs -ls /data/bank/raw
"
```

### Bước 4: Chạy Job 1 — Extraction

```bash
kubectl exec -it hadoop-master -n hadoop -- \
  /opt/hadoop/bin/hadoop jar /tmp/BankDataExtraction.jar bank.DataExtractionDriver
```

Xem kết quả:
```bash
kubectl exec -it hadoop-master -n hadoop -- \
  /opt/hadoop/bin/hdfs dfs -cat /data/bank/output1/part-r-00000
```

### Bước 5: Chạy Job 2 — Report

```bash
kubectl exec -it hadoop-master -n hadoop -- \
  /opt/hadoop/bin/hadoop jar /tmp/BankDataReport.jar bank.ReportDriver
```

Xem kết quả:
```bash
kubectl exec -it hadoop-master -n hadoop -- \
  /opt/hadoop/bin/hdfs dfs -cat /data/bank/output2/part-r-00000
```

Kết quả mong đợi (dạng):
```
<30       1234
[30,45)   5678
[45,60)   9012
>=60      3456
```

### Chạy lại (xóa output cũ)

```bash
kubectl exec -it hadoop-master -n hadoop -- bash -c "
  /opt/hadoop/bin/hdfs dfs -rm -r /data/bank/output1
  /opt/hadoop/bin/hdfs dfs -rm -r /data/bank/output2
"
```

### Theo dõi job

| Service | URL |
|---|---|
| YARN ResourceManager | http://10.10.80.10:30088 |
| JobHistory Server | http://10.10.80.10:31988 |
| HDFS NameNode | http://10.10.80.10:30870 |

---

## So sánh với slide gốc

| Slide (Eclipse + Linux VM) | Thực tế (Maven + k8s) |
|---|---|
| 2 project Eclipse riêng | 1 project Maven, 2 JAR |
| Build qua Eclipse GUI | `mvn clean package` tạo 2 JAR |
| Hadoop ở `/usr/local/hadoop` | Hadoop ở `/opt/hadoop` |
| Path qua `args[]` | Path hardcode (tránh lỗi Maven cache) |
| Input: `/data/bank/raw` | Giống slide |
| Output1: `/data/bank/output1` | Giống slide |
| Output2: `/data/bank/output2` | Giống slide |

---

## Lưu ý

> **applications.csv thực tế** cần download từ giáo viên cung cấp.
> File `applications.csv` trong thư mục này chỉ là **dữ liệu mẫu 5 dòng** để test.
> Khi chạy thật, thay bằng file đầy đủ trước khi upload lên HDFS.
