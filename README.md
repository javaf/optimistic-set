Fine Set is a collection of unique elements
maintained as a linked list. The list of nodes
are arranged in ascending order by their key,
which is obtained using `hashCode()`. This
facilitates the search of a item within the
list. When the list is empty, it contains two
sentinel nodes `head` and `tail` with minimum
and maximum key values respectively. These
sentinel nodes are not part of the set.

Each node has an associated lock (fine-grained)
that enables locking specific nodes, instead of
locking down the whole list for all method
calls. Traversing the list (find) is done in
a hand-holding manner, as children do with an
overhead ladder. Initially two nodes are locked.
While moving to the next node, we unlock the
first node, and lock the third node, and so on.
This prevents any thread from adding or
removing threads in between, allowing them to
execute in pipelined fashion.

As this set uses fine-grained locks (per node),
it performs well when contention is medium. Due
to acquiring of locks in hand-holding fashion,
threads traversing the list concurrently will
be stacked behind each other. This forced
pipelining occurs even if they want to modify
completely different parts of the list.

```java
add():
1. Create new node beforehand.
2. Find node after which to insert.
3. Add node, only if key is unique.
4. Increment size if node was added.
5. Unlock node pairs locked by find.
```

```java
remove():
1. Find node after which to remove.
2. Remove node, only if key matches.
3. Decrement size if node was removed.
4. Unlock node pairs locked by find.
```

```java
contains():
1. Find node previous to search key.
2. Check if next node matches search key.
3. Unlock node pairs locked by find.
```

See [FineSet.java] for code, [Main.java] for test, and [repl.it] for output.

[FineSet.java]: https://repl.it/@wolfram77/fine-set#FineSet.java
[Main.java]: https://repl.it/@wolfram77/fine-set#Main.java
[repl.it]: https://fine-set.wolfram77.repl.run


### references

- [The Art of Multiprocessor Programming :: Maurice Herlihy, Nir Shavit](https://dl.acm.org/doi/book/10.5555/2385452)
