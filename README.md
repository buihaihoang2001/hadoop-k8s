# Hadoop Cluster 3 Nodes tren Kubernetes (k3s)

## Mo hinh trien khai

```
May that (Mac/Windows)
    |
    SSH --> Ubuntu Server 20.04 (10.10.80.10)
                |
                k3s (Kubernetes)
                    |-- Pod: hadoop-master  --> NameNode, ResourceManager, JobHistoryServer
                    |-- Pod: hadoop-slave01 --> DataNode, NodeManager
                    |-- Pod: hadoop-slave02 --> DataNode, NodeManager
```

| STT | Ten may | Chuc nang |
|-----|---------|-----------|
| 1 | hadoop-master | NameNode, ResourceManager, JobHistoryServer |
| 2 | hadoop-slave01 | DataNode, NodeManager |
| 3 | hadoop-slave02 | DataNode, NodeManager |

---

## Yeu cau

- Ubuntu Server 20.04
- RAM toi thieu 4GB
- Quyen sudo

---

## Buoc 1: Cai k3s

```bash
curl -sfL https://get.k3s.io | sh -
```

Cau hinh kubectl khong can sudo:

```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc
```

Kiem tra:

```bash
kubectl get nodes
```

Ket qua mong doi:

```
NAME      STATUS   ROLES           AGE   VERSION
ubuntu    Ready    control-plane   1m    v1.34.x+k3s1
```

---

## Buoc 2: Clone source code

```bash
git clone https://github.com/buihaihoang2001/hadoop-k8s.git
cd hadoop-k8s
```

Cau truc thu muc:

```
hadoop-k8s/
|-- configmap.yaml
|-- services.yaml
|-- master.yaml
|-- slaves.yaml
```

---

## Buoc 3: Tao Namespace

```bash
kubectl create namespace hadoop
```

---

## Buoc 4: Apply tung file

```bash
kubectl apply -f configmap.yaml
kubectl apply -f services.yaml
kubectl apply -f master.yaml
kubectl apply -f slaves.yaml
```

Hoac apply toan bo mot lenh:

```bash
kubectl apply -f .
```

---

## Buoc 5: Kiem tra

Theo doi pods khoi dong:

```bash
kubectl get pods -n hadoop -w
```

Ket qua mong doi:

```
NAME             READY   STATUS    RESTARTS   AGE
hadoop-master    1/1     Running   0          2m
hadoop-slave01   1/1     Running   0          1m
hadoop-slave02   1/1     Running   0          1m
```

Kiem tra 2 DataNode da ket noi:

```bash
kubectl exec -it hadoop-master -n hadoop -- /opt/hadoop/bin/hdfs dfsadmin -report
```

Ket qua mong doi: `Live datanodes (2)`

Kiem tra YARN nodes:

```bash
kubectl exec -it hadoop-master -n hadoop -- /opt/hadoop/bin/yarn node -list
```

Ping giua cac pod:

```bash
kubectl exec -it hadoop-master -n hadoop -- ping -c 3 hadoop-slave01
kubectl exec -it hadoop-master -n hadoop -- ping -c 3 hadoop-slave02
```

---

## Buoc 6: Truy cap Web UI

Thay `<SERVER_IP>` bang IP may Ubuntu.

| Service | URL | Chuc nang |
|---------|-----|-----------|
| HDFS NameNode | http://<SERVER_IP>:30870 | Quan ly HDFS, DataNodes |
| YARN ResourceManager | http://<SERVER_IP>:30088 | Quan ly Jobs, Nodes |
| JobHistory Server | http://<SERVER_IP>:31988 | Lich su MapReduce jobs |

---

## Buoc 7: Truy cap vao tung Pod

```bash
# Vao Master
kubectl exec -it hadoop-master -n hadoop -- bash

# Vao Slave01
kubectl exec -it hadoop-slave01 -n hadoop -- bash

# Vao Slave02
kubectl exec -it hadoop-slave02 -n hadoop -- bash
```

---

## Buoc 8: Test WordCount

```bash
kubectl exec -it hadoop-master -n hadoop -- bash -c "
  /opt/hadoop/bin/hdfs dfs -mkdir -p /user/hadoop/input &&
  echo 'hadoop spark kafka hadoop spark hadoop' > /tmp/test.txt &&
  /opt/hadoop/bin/hdfs dfs -put /tmp/test.txt /user/hadoop/input/ &&
  /opt/hadoop/bin/yarn jar /opt/hadoop/share/hadoop/mapreduce/hadoop-mapreduce-examples-*.jar \
    wordcount /user/hadoop/input /user/hadoop/output &&
  /opt/hadoop/bin/hdfs dfs -cat /user/hadoop/output/part-r-00000
"
```

Ket qua mong doi:

```
hadoop  3
kafka   1
spark   2
```

---

## Quan ly Cluster

```bash
# Xem trang thai tat ca pods
kubectl get pods -n hadoop

# Xem log cua master
kubectl logs hadoop-master -n hadoop

# Xoa toan bo cluster
kubectl delete namespace hadoop

# Cai lai tu dau
kubectl create namespace hadoop
kubectl apply -f .
```

---

