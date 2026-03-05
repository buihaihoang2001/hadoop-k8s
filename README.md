# Hadoop 3-Node Cluster trên Kubernetes (k3s) - Persistent Storage

## Mô hình triển khai

```
Máy cá nhân (Mac/Windows)
        │
        └── SSH ──► Ubuntu Server 20.04 (10.10.80.10)
                          │
                          ├── /data/hadoop/namenode   ← Lưu trữ vĩnh viễn trên host
                          ├── /data/hadoop/datanode1  ← Lưu trữ vĩnh viễn trên host
                          ├── /data/hadoop/datanode2  ← Lưu trữ vĩnh viễn trên host
                          │
                          └── k3s (Kubernetes)
                                  ├── Pod: hadoop-master   → NameNode, SecondaryNameNode, ResourceManager, JobHistoryServer
                                  ├── Pod: hadoop-slave01  → DataNode, NodeManager
                                  └── Pod: hadoop-slave02  → DataNode, NodeManager
```

---

## So sánh: Slide (VirtualBox) vs Thực tế (Kubernetes)

| Tiêu chí | Slide (VirtualBox) | Thực tế (Kubernetes/k3s) |
|---|---|---|
| **Nền tảng** | VirtualBox 6.1.16 | k3s (Kubernetes nhẹ) |
| **Đơn vị "máy"** | Virtual Machine (VM) | Pod |
| **Hệ điều hành** | Ubuntu 20.04 Desktop | Container `apache/hadoop:3` (CentOS 7) |
| **IP máy master** | 10.0.2.195 (static) | Pod IP động, truy cập qua NodePort |
| **IP slave01** | 10.0.2.196 (static) | Pod IP động, DNS nội bộ K8s |
| **IP slave02** | 10.0.2.197 (static) | Pod IP động, DNS nội bộ K8s |
| **Kết nối giữa các máy** | SSH (ssh-keygen, ssh-copy-id) | Kubernetes Headless Service + DNS |
| **Java** | Cài thủ công `openjdk-8-jre-headless` | Có sẵn trong image (`/usr/lib/jvm/jre`) |
| **Hadoop** | Cài thủ công vào `/usr/local/hadoop` | Có sẵn trong image tại `/opt/hadoop` |
| **User chạy Hadoop** | `hduser` (group: `hadoop`) | User `hadoop` có sẵn trong image |
| **Config Hadoop** | Sửa file trực tiếp trên từng máy | Kubernetes ConfigMap, mount vào Pod |
| **Khởi động Hadoop** | `start-all.sh` (dùng SSH) | `hdfs --daemon start` trực tiếp từng Pod |
| **Dữ liệu HDFS** | Lưu trong VM disk | **PersistentVolume trên host** (`/data/hadoop/`) |
| **Restart tự động** | Không | Có (`restartPolicy: Always`) |
| **Format lại khi restart** | Không | Không (kiểm tra VERSION file trước khi format) |
| **Web UI - NameNode** | http://master:9870 | http://10.10.80.10:30870 |
| **Web UI - YARN** | http://master:8088 | http://10.10.80.10:30088 |
| **Web UI - JobHistory** | http://master:19888 | http://10.10.80.10:31988 |
| **RAM cần thiết** | ~6GB (2GB/VM × 3) | ~3-4GB tổng |
| **Thời gian setup** | 1-2 giờ | 15-20 phút |

---

## Tính bền vững dữ liệu

| Tình huống | K8s/Pods | Dữ liệu HDFS |
|---|---|---|
| Reboot server | ✅ Tự phục hồi | ✅ Giữ nguyên |
| Delete Pod | ✅ Tự tạo lại | ✅ Giữ nguyên |
| Delete namespace hadoop | ❌ Phải apply lại | ✅ Giữ nguyên (PV có `Retain` policy) |
| Xóa thủ công `/data/hadoop/` | ❌ | ❌ Mất hoàn toàn |

---

## Cấu trúc file

```
hadoop-k8s/
├── 01-namespace.yaml   # Tạo namespace "hadoop"
├── 02-configmap.yaml   # Config: core-site, hdfs-site, mapred-site, yarn-site
├── 03-storage.yaml     # PersistentVolume + PersistentVolumeClaim (3 PV/PVC)
├── 04-services.yaml    # Services: Headless DNS + NodePort Web UI
├── 05-master.yaml      # Pod master với PVC namenode
├── 06-slaves.yaml      # Pod slave01 + slave02 với PVC datanode
└── README.md           # Tài liệu này
```

---

## Hướng dẫn deploy từ đầu

### Yêu cầu
- Ubuntu Server 20.04 với k3s đã cài
- kubectl được cấu hình (`~/.kube/config`)

### Bước 1: Cài k3s (nếu chưa có)
```bash
curl -sfL https://get.k3s.io | sh -

mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc

# Kiểm tra
kubectl get nodes
```

### Bước 2: Tạo thư mục lưu trữ trên server
```bash
sudo mkdir -p /data/hadoop/namenode
sudo mkdir -p /data/hadoop/datanode1
sudo mkdir -p /data/hadoop/datanode2
sudo chmod -R 777 /data/hadoop
```

### Bước 3: Clone repo và deploy
```bash
git clone <your-repo-url>
cd hadoop-k8s

kubectl apply -f 01-namespace.yaml
kubectl apply -f 02-configmap.yaml
kubectl apply -f 03-storage.yaml
kubectl apply -f 04-services.yaml
kubectl apply -f 05-master.yaml
kubectl apply -f 06-slaves.yaml
```

### Bước 4: Chờ pods ready
```bash
kubectl get pods -n hadoop -w
# Đợi 3 pods STATUS = Running (~1-2 phút)
```

### Bước 5: Kiểm tra cluster
```bash
# Kiểm tra 2 DataNode đã kết nối
kubectl exec -it hadoop-master -n hadoop -- /opt/hadoop/bin/hdfs dfsadmin -report

# Kiểm tra YARN nodes
kubectl exec -it hadoop-master -n hadoop -- /opt/hadoop/bin/yarn node -list

# Kiểm tra IP pods
kubectl get pods -n hadoop -o wide

# Ping giữa các pods
kubectl exec -it hadoop-master -n hadoop -- ping -c 3 hadoop-slave01
kubectl exec -it hadoop-master -n hadoop -- ping -c 3 hadoop-slave02
```

---

## Truy cập Web UI

| Service | URL | Mô tả |
|---|---|---|
| HDFS NameNode | http://10.10.80.10:30870 | Quản lý HDFS, xem DataNodes |
| YARN ResourceManager | http://10.10.80.10:30088 | Quản lý jobs, xem cluster nodes |
| JobHistory Server | http://10.10.80.10:31988 | Lịch sử MapReduce jobs |

---

## Truy cập vào từng Pod

```bash
# Vào master
kubectl exec -it hadoop-master -n hadoop -- bash

# Vào slave01
kubectl exec -it hadoop-slave01 -n hadoop -- bash

# Vào slave02
kubectl exec -it hadoop-slave02 -n hadoop -- bash
```

---

## Reset hoàn toàn (xóa cả dữ liệu)

```bash
# Xóa K8s resources
kubectl delete namespace hadoop
kubectl delete pv hadoop-namenode-pv hadoop-datanode1-pv hadoop-datanode2-pv

# Xóa dữ liệu trên host
sudo rm -rf /data/hadoop

# Tạo lại từ đầu
sudo mkdir -p /data/hadoop/namenode /data/hadoop/datanode1 /data/hadoop/datanode2
sudo chmod -R 777 /data/hadoop
kubectl apply -f 01-namespace.yaml
kubectl apply -f 02-configmap.yaml
kubectl apply -f 03-storage.yaml
kubectl apply -f 04-services.yaml
kubectl apply -f 05-master.yaml
kubectl apply -f 06-slaves.yaml
```

---

## Test WordCount (MapReduce)

```bash
kubectl exec -it hadoop-master -n hadoop -- bash -c "
  /opt/hadoop/bin/hdfs dfs -mkdir -p /user/hadoop/input
  echo 'hadoop spark kafka hadoop spark hadoop' > /tmp/test.txt
  /opt/hadoop/bin/hdfs dfs -put /tmp/test.txt /user/hadoop/input/
  /opt/hadoop/bin/yarn jar /opt/hadoop/share/hadoop/mapreduce/hadoop-mapreduce-examples-*.jar \
    wordcount /user/hadoop/input /user/hadoop/output
  /opt/hadoop/bin/hdfs dfs -cat /user/hadoop/output/part-r-00000
"
```

---

## Lưu ý kỹ thuật

**Tại sao không dùng `start-all.sh`?**
Image `apache/hadoop:3` không có SSH server. Script `start-all.sh` dùng SSH để remote start các node nên không hoạt động. Thay vào đó mỗi Pod tự start service của mình bằng `--daemon start`.

**Tại sao dùng Headless Service?**
K8s Headless Service (`clusterIP: None`) cho phép các Pod tìm nhau qua DNS hostname (`hadoop-master`, `hadoop-slave01`, `hadoop-slave02`) thay vì cần cấu hình `/etc/hosts` thủ công như trong slide.

**Tại sao có `initContainer fix-permissions`?**
Volume mount từ host (`hostPath`) mặc định thuộc `root`. Container chạy với user `hadoop` (uid 1000) cần quyền ghi vào thư mục đó. `initContainer` chạy trước để `chown` đúng owner.

**Logic format NameNode thông minh:**
Script kiểm tra file `/data/hdfs/namenode/current/VERSION` - nếu đã tồn tại thì bỏ qua format, giữ nguyên dữ liệu HDFS cũ. Chỉ format lần đầu tiên khi chưa có dữ liệu.
