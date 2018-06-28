# Java8 HashMap源码解析

HashMap 源码分析：

## 概述

基于Map接口实现、允许null键/值、非同步、不保证有序(比如插入的顺序)、也不保证顺序不随时间变化。 

https://blog.csdn.net/qazwyc/article/details/76686915

###  一、数据结构（数组 + 链表 + 红黑树 ）

![hashMap-tree](..\images\hashMap-tree.png)

### 属性分析

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