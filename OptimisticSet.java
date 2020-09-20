import java.util.*;
import java.util.function.*;
import java.util.concurrent.atomic.*;

// Fine Set is a collection of unique elements
// maintained as a linked list. The list of nodes
// are arranged in ascending order by their key,
// which is obtained using `hashCode()`. This
// facilitates the search of a item within the
// list. When the list is empty, it contains two
// sentinel nodes `head` and `tail` with minimum
// and maximum key values respectively. These
// sentinel nodes are not part of the set.
// 
// Each node has an associated lock (fine-grained)
// that enables locking specific nodes, instead of
// locking down the whole list for all method
// calls. Traversing the list (find) is done in
// a hand-holding manner, as children do with an
// overhead ladder. Initially two nodes are locked.
// While moving to the next node, we unlock the
// first node, and lock the third node, and so on.
// This prevents any thread from adding or
// removing threads in between, allowing them to
// execute in pipelined fashion.
// 
// As this set uses fine-grained locks (per node),
// it performs well when contention is medium. Due
// to acquiring of locks in hand-holding fashion,
// threads traversing the list concurrently will
// be stacked behind each other. This forced
// pipelining occurs even if they want to modify
// completely different parts of the list.

class OptimisticSet<T> extends AbstractSet<T> {
  final AtomicInteger size;
  final Node<T> head;
  // size: number of items in set
  // head: points to begin of nodes in set

  public OptimisticSet() {
    size = new AtomicInteger(0);
    head = new Node<>(null, Integer.MIN_VALUE);
    head.next = new Node<>(null, Integer.MAX_VALUE);
  }

  // 1. Create new node beforehand.
  // 2. Find node after which to insert.
  // 3. Add node, only if key is unique.
  // 4. Increment size if node was added.
  // 5. Unlock node pairs locked by find.
  @Override
  public boolean add(T v) {
    Node<T> x = new Node<>(v);
    if (!test(x.key, p -> addNode(p, x)))
      return false;
    size.incrementAndGet(); // 4
    return true;
  }

  // 1. Find node after which to remove.
  // 2. Remove node, only if key matches.
  // 3. Decrement size if node was removed.
  // 4. Unlock node pairs locked by find.
  @Override
  public boolean remove(Object v) {
    int k = v.hashCode();
    if (!test(k, p -> removeNode(p, k)))
      return false;
    size.decrementAndGet(); // 3
    return true;
  }

  // 1. Find node previous to search key.
  // 2. Check if next node matches search key.
  // 3. Unlock node pairs locked by find.
  @Override
  public boolean contains(Object v) {
    int k = v.hashCode();
    return test(k, p -> p.next.key == k);
  }

  // 1. Check if already exists.
  // 2. Insert new node in between.
  private boolean addNode(Node<T> p, Node<T> x) {
    Node<T> q = p.next;
    if (q.key == x.key) return false; // 1
    x.next = q; // 2
    p.next = x; // 2
    return true;
  }

  // 1. Check if does not exist.
  // 2. Detach the node.
  private boolean removeNode(Node<T> p, int k) {
    Node<T> q = p.next;
    if (q.key != k) return false; // 1
    p.next = q.next; // 2
    return true;
  }

  public boolean test(int k, Predicate<Node<T>> fn) {
    boolean okay = false;
    boolean done = false;
    do {
      Node<T> p = findNode(k);
      Node<T> q = p.next;
      okay = validate(p, q);
      done = okay && fn.test(p);
      unlockPair(p, q);
    } while (!okay);
    return done;
  }

  // 1. Lock first node pair.
  // 2. As long as key too low:
  // 3. Traverse in hand-holding fashion.
  private Node<T> findNode(int k) {
    Node<T> p = head;
    while (p.next.key < k) // 2
      p = p.next;          // 3
    lockPair(p);
    return p;
  }

  private boolean validate(Node<T> p, Node<T> q) {
    Node<T> x = head;
    while (x.key < p.key)
      x = x.next;
    return x == p && p.next == q;
  }
  
  // 1. Lock 1st node.
  // 2. Lock 2nd node.
  private void lockPair(Node<T> p) {
    p.lock();      // 1
    p.next.lock(); // 2
  }

  // 1. Unlock 2nd node.
  // 2. Unlock 1st node.
  private void unlockPair(Node<T> p, Node<T> q) {
    q.unlock(); // 1
    p.unlock(); // 2
  }

  @Override
  public Iterator<T> iterator() {
    Collection<T> a = new ArrayList<>();
    Node<T> p = head;
    while (p.next.next != null) {
      a.add(p.next.value);
      p = p.next;
    }
    return a.iterator();
  }

  @Override
  public int size() {
    return size.get();
  }
}
