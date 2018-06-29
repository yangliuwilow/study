# Java8 HashMap源码解析

HashMap 源码分析：

## 概述

基于Map接口实现、允许null键/值、非同步、不保证有序(比如插入的顺序)、也不保证顺序不随时间变化。 

https://blog.csdn.net/qazwyc/article/details/76686915

###  数据结构（数组 + 链表 + 红黑树 ）

![hashMap-tree](..\images\hashMap-tree.png)

### 1、类的继承关系　

~~~java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable
~~~

​       HashMap继承自父类（AbstractMap），实现了Map、Cloneable、Serializable接口

 

### 2、属性分析

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {  
    // 序列号
    private static final long serialVersionUID = 362498820763181265L;    
    // 创建数组的初始容量是16
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;   
    // 数组的最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30; 
    //数组长度到达75%时候扩容，16*0.75=12，到达12时候扩容
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    // 当桶(bucket)上的结点数大于这个值时会转成红黑树,(一个数组上链表的长度达到8时候换为红黑树)
    static final int TREEIFY_THRESHOLD = 8; 
    // 当桶(bucket)上的结点数小于这个值时树转链表 (一个数组上链表的长度小于6时候换为链表)
    static final int UNTREEIFY_THRESHOLD = 6;
    // 桶中结构转化为红黑树对应的数组的最小大小，如果当前容量小于它，就不会将链表转化为红黑树，而是用resize()代替
    static final int MIN_TREEIFY_CAPACITY = 64;
    // 存储元素的数组，总是2的幂
    transient Node<k,v>[] table; 
    // 存放具体元素的集
    transient Set<map.entry<k,v>> entrySet;
    // 存放元素的个数，注意这个不等于数组的长度。Map的大小
    transient int size;
    // 每次扩容和更改map结构的计数器
    transient int modCount;   
    // 临界值 当实际节点个数超过临界值(容量*填充因子)时，会进行扩容
    int threshold;
    // 填充因子
    final float loadFactor;   
}
  
```
### 3、key的计算

~~~java
//高16位和低16位进行异或运算，这样结果才可能尽可能不同
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
~~~

#### 3.1   得到hashCode  

~~~java
int hash= key.hashCode()
~~~

#### 3.2 得到0-15之间的数字，数组大小的范围内

```java
int index=hash%16     //  0-15
```



~~~java
resize() //数组初始话和数组的2倍的扩容
~~~





~~~java
final Node<K,V>[] resize() {
    // 当前table保存
    Node<K,V>[] oldTab = table;
    // 保存table大小
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    // 保存当前阈值 
    int oldThr = threshold;
    int newCap, newThr = 0;
    // 之前table大小大于0，即已初始化
    if (oldCap > 0) {
        // 超过最大值就不再扩充了，只设置阈值
        if (oldCap >= MAXIMUM_CAPACITY) {
            // 阈值为最大整形
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 容量翻倍
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            // 阈值翻倍
            newThr = oldThr << 1; // double threshold
    }
    // 初始容量已存在threshold中
    else if (oldThr > 0)            // initial capacity was placed in threshold
        newCap = oldThr;
    // 使用缺省值（使用默认构造函数初始化）
    else {                                  // zero initial threshold signifies using defaults
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    // 计算新阈值
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
    // 初始化table
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    // 之前的table已经初始化过
    if (oldTab != null) {
        // 复制元素，重新进行hash
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)         //桶中只有一个结点
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)         //红黑树
                    //根据(e.hash & oldCap)分为两个，如果哪个数目不大于UNTREEIFY_THRESHOLD，就转为链表
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { //链表
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    // 将同一桶中的元素根据(e.hash & oldCap)是否为0进行分割成两个不同的链表，完成rehash
                    do {
                        next = e.next;//保存下一个节点
                        //如果为0，保持在原来的位置不变
                        if ((e.hash & oldCap) == 0) {       //保留在低部分即原索引
                            if (loTail == null)//第一个结点让loTail和loHead都指向它
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        //如果不为0，加上原来的capacity
                        else {                                      //hash到高部分即原索引+oldCap
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
~~~

