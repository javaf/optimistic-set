import java.util.concurrent.locks.*;

class Node<T> {
  Lock lock;
  T value;
  int key;
  Node<T> next;

  public Node(T v, int k) {
    lock = new ReentrantLock();
    value = v;
    key = k;
  }
  public Node(T v) {
    this(v, v.hashCode());
  }

  public void lock() {
    lock.lock();
  }

  public void unlock() {
    lock.unlock();
  }
}
