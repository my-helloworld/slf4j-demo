# slf4j-api 的源码解析与一个简单实现

最近的工作里有好几个同事向我问起了`slf4j`的一些配置和使用问题。
本着问题需要被从根本解决的原则，我决定写一个样例项目来解释一下`slf4j-api`的一些接口问题，以及`log4j`对其的实现原理。

项目地址为 [https://github.com/my-helloworld]

本项目就是一个`slf4j-api`简单的异步日志实现，原理参考`slf4j-log4j12`。代码精简易懂，方便初学者了解`slf4j-log4j`的一些实现细节。

## 1. 关于 slf4j

[简单日志门面(Simple Logging Facade for java, SLF4J)](https://github.com/qos-ch/slf4j)为各种日志框架提供了统一的接口封装，
包括`java.util.logging`,`logback`以及`Log4j`等，
使得最终用户能够在部署的时候灵活配置自己希望的Loging APIs实现。
在应用开发中，需要统一按照SLF4J的API进行开发，在部署时，选择不同的日志系统包加入到JAVA CLASSPATH中，
即可自动转换到不同的日志框架上。SLF4J隐藏了具体的转换、适配细节，将应用和具体日志框架解耦开来，
如果在类路径中没有发现绑定的日志实现，SLF4J默认使用NOP实现。

简言之，`slf4j-api`提供了一套标准的实现推展接口。
抽离日志接口API有利于解决日志实现的依赖问题, 任何一个模块只要引入 `slf4j-api` 而不需要关心实现。
而在构建应用入口的时候，在编译(或部署)时可以根据不同应用场景去引入不同的实现。
而针对不同实现，可以添加一些额外的配置(如`kafka broker server`、`email`等特殊实现所依赖的参数)。
这是典型的用插件化开发实现切片编程的思想。

由于`slf4j`影响广泛，几乎所有的近代`JEE`项目均采用这个接口标准进行接口拓展。

## 2. 从 slf4j-api 开始 

我们在使用`slf4j`的时候会创建一个`Logger`对象，这个创建过程通常是

```
org.slf4j.Logger LOGGER = LoggerFactory.getLogger("some-logger-name");
```

因此我们分析该方法, 能定位到其实现的绑定方法`LoggerFactory#getILoggerFactory()`

```
public static ILoggerFactory getILoggerFactory() {
    if (INITIALIZATION_STATE == UNINITIALIZED) {
        synchronized (LoggerFactory.class) {
            if (INITIALIZATION_STATE == UNINITIALIZED) {
                INITIALIZATION_STATE = ONGOING_INITIALIZATION;
                performInitialization();
            }
        }
    }
    switch (INITIALIZATION_STATE) {
    case SUCCESSFUL_INITIALIZATION:
        return StaticLoggerBinder.getSingleton().getLoggerFactory();
    case NOP_FALLBACK_INITIALIZATION:
        return NOP_FALLBACK_FACTORY;
    case FAILED_INITIALIZATION:
        throw new IllegalStateException(UNSUCCESSFUL_INIT_MSG);
    case ONGOING_INITIALIZATION:
        // support re-entrant behavior.
        // See also http://jira.qos.ch/browse/SLF4J-97
        return SUBST_FACTORY;
    }
    throw new IllegalStateException("Unreachable code");
}
```

此方法中通过符号引用`org.slf4j.impl.StaticLoggerBinder.getSingleton().getLoggerFactory();`获取了一个`ILoggerFactory`对象。
而正如我们所求的，`ILoggerFactory` 正好就是需要我们实现的`Logger`工厂接口

```
public interface ILoggerFactory {

    public Logger getLogger(String name);
}

```

前面之所以称符号引用，因为`slf4j-api`中并没有`StaticLoggerBinder`这个类。

参考`slf4j`文档，`slf4j-api` 需要我们实现一个类 `org.slf4j.impl.StaticLoggerBinder`, 同时这个类需要具有如下特征:

- 获取Binder单例的静态方法 `public static StaticLoggerBinder getSingleton()`

- 实现接口`ILoggerFactoryBinder`

```
public interface LoggerFactoryBinder {

    public ILoggerFactory getLoggerFactory();

    public String getLoggerFactoryClassStr();
}
```

## 3. 同步日志与异步日志

日志的本意就是将一些半结构化数据落盘，为未来的危险预警，灾难恢复以及数据分析等业务场景提供数据源。

常见的输出策略有
- 进程标准输出(std_out/std_err)
- 持久化本地(或HDFS)文件
- 作为消息队列生产者(如:ELK框架)

对此我们着重分析本地文件落盘的场景。

在讲文件写入之前，我们需要先了解下操作系统对文件读写提供了一个怎么样的IO模型。

### 文件接口

文件读写主要牵涉到了如下五个操作：打开、关闭、读、写、定位。在Linux系统中，提供了两套API，

一套是C标准API：fopen、fclose、fread、fwrite、fseek，

另一套则是POSIX定义的系统API：open、close、read、write、seek。

其中POSIX定义的API是系统API，而C标准API是基于系统API的封装，并且提供了额外的缓冲的功能。因此也可以把它们叫做缓冲I/O函数和非缓冲I/O函数。

相信各位java开发者对这些方法应该很熟了，确实java里面使用了`*Stream`类去包装这些文件系统直接提供的文件操作方法。

需要一提的是`FileOutputStream::write()`是一个native实现，且该操作不是一个线程安全的操作。

通常情况下要解决这个矛盾我们无非是通过加锁去实现, 当A,B两个线程竞争写文件f时，会分别执行

```
[::lock()] -> [A::write()] -> [::unlock()] -> [::lock()] -> [B::write()] -> [::unlock()] ...
```

这样通过 `::lock()`/`::unlock()` 的方式竞争资源能保证并发环境下写入的线程安全性，这种写入方法我们称之为同步解决方案。

事实上这样的模型还能进一步进行抽象, 假定我们分配了一个独立线程加消息队`queue`列用于处理读写。
在该模型下，写操作会向该消息队列中添加写消息，而该线程只负责消费消息队列中的写事件。(参考`Android Framework` 中的`HandlerThread`)

```
Thread A:
    queue::add(A) # with lock
    queue::add(B) # with lock
    ...
    
Thread B:
    queue::poll() # fetch A and write, without lock
    queue::poll() # fetch B and write, without lock
    ...
```

这个过程并没有消除竞争状态，但好处在于将写时的锁竞争抽象到了消息队列的`queue::add()`上执行，
而消费线程上执行的`queue::poll()`则不需要关心任何资源竞争的场景。

这种解决方案我们称之为异步解决方案

因此我们将高效写文件的核心资源竞争问题，递归到寻找一个高效的消息队列实现的问题上。

### JDK 内置队列

Java的内置队列如下表所示。

| 队列 |有界性 | 锁 | 数据结构 |
|:---:|:---:|:---:|:---:|
| ArrayBlockingQueue | bounded | 加锁 | arraylist |
| LinkedBlockingQueue | optionally-bounded | 加锁 | linkedlist |
| ConcurrentLinkedQueue | unbounded | 无锁 | linkedlist |
| LinkedTransferQueue | unbounded | 无锁 | linkedlist |
| PriorityBlockingQueue | unbounded | 加锁 | heap |
| DelayQueue | unbounded | 加锁 | heap |

队列的底层一般分成三种：数组、链表和堆。其中，堆一般情况下是为了实现带有优先级特性的队列，暂且不考虑。

我们就从数组和链表两种数据结构来看，基于数组线程安全的队列，
比较典型的是`ArrayBlockingQueue`，它主要通过加锁的方式来保证线程安全；
基于链表的线程安全队列分成`LinkedBlockingQueue`和`ConcurrentLinkedQueue`两大类，前者也通过锁的方式来实现线程安全，
而后者以及上面表格中的`LinkedTransferQueue`都是通过原子变量compare and swap（以下简称“CAS”）这种不加锁的方式来实现的。

通过不加锁的方式实现的队列都是无界的（无法保证队列的长度在确定的范围内）；
而加锁的方式，可以实现有界队列。在稳定性要求特别高的系统中，为了防止生产者速度过快，导致内存溢出，只能选择有界队列；
同时，为了减少Java的垃圾回收对系统性能的影响，会尽量选择array/heap格式的数据结构。
这样筛选下来，符合条件的队列就只有`ArrayBlockingQueue`。

那么我们将目光聚焦在`java`内置的`ArrayBlockingQueue`上，其默认实现方案为基于非公平重入锁。

> ArrayBlockingQueue 的重入锁锁声明

```
/** Main lock guarding all access */
final ReentrantLock lock;
    
public ArrayBlockingQueue(int capacity) {
    this(capacity, false);
}

public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}
```

> 添加与消费

```
public boolean offer(E e) {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count == items.length)
            return false;
        else {
            enqueue(e);
            return true;
        }
    } finally {
        lock.unlock();
    }
}

public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
```

### slf4j-log4j12 的 AsyncAppender 实现

相信配置过log4j的同学应该对这个类还是比较熟悉的，但可能并没有仔细去阅读过其实现细节。

`AsyncAppender`主要处理分发`log4j-api` 定义的`LoggingEvent`事件，并在一个`dispatcher`线程中执行日志写入

它使用了`ArrayList`作事件容器，并使用同步关键字`synchronized`对容器进行读写同步

> 事件添加

```
/**
 * Event buffer, also used as monitor to protect itself and
 * discardMap from simulatenous modifications.
 */
private final List buffer = new ArrayList();

public void append(final LoggingEvent event) {
    //
    //   if dispatcher thread has died then
    //      append subsequent events synchronously
    //   See bug 23021
    if ((dispatcher == null) || !dispatcher.isAlive() || (bufferSize <= 0)) {
      synchronized (appenders) {
        appenders.appendLoopOnAppenders(event);
      }
    
      return;
    }
    
    // Set the NDC and thread name for the calling thread as these
    // LoggingEvent fields were not set at event creation time.
    event.getNDC();
    event.getThreadName();
    // Get a copy of this thread's MDC.
    event.getMDCCopy();
    if (locationInfo) {
      event.getLocationInformation();
    }
    event.getRenderedMessage();
    event.getThrowableStrRep();
    
    synchronized (buffer) {
      while (true) {
        int previousSize = buffer.size();
    
        if (previousSize < bufferSize) {
          buffer.add(event);
    
          //
          //   if buffer had been empty
          //       signal all threads waiting on buffer
          //       to check their conditions.
          //
          if (previousSize == 0) {
            buffer.notifyAll();
          }
    
          break;
        }
    
        //
        //   Following code is only reachable if buffer is full
        //
        //
        //   if blocking and thread is not already interrupted
        //      and not the dispatcher then
        //      wait for a buffer notification
        boolean discard = true;
        if (blocking
                && !Thread.interrupted()
                && Thread.currentThread() != dispatcher) {
          try {
            buffer.wait();
            discard = false;
          } catch (InterruptedException e) {
            //
            //  reset interrupt status so
            //    calling code can see interrupt on
            //    their next wait or sleep.
            Thread.currentThread().interrupt();
          }
        }
    
        //
        //   if blocking is false or thread has been interrupted
        //   add event to discard map.
        //
        if (discard) {
          String loggerName = event.getLoggerName();
          DiscardSummary summary = (DiscardSummary) discardMap.get(loggerName);
    
          if (summary == null) {
            summary = new DiscardSummary(event);
            discardMap.put(loggerName, summary);
          } else {
            summary.add(event);
          }
    
          break;
        }
      }
    }
}
```

> 消费过程

消费过程比较复杂，buffer容器被传递到了一个`Dispatcher`中(概念同`Android`中的`Handler`, `Disruptor`中的`EventHandler`)。
这个`Dispatcher`在异步线程中顺序消费事件，执行`epoll`循环。

每个周期都获取所有入队事件，并一次执行`appenders.appendLoopOnAppenders(events)`调用实际的消费逻辑

```
public void run() {
  boolean isActive = true;

  //
  //   if interrupted (unlikely), end thread
  //
  try {
    //
    //   loop until the AsyncAppender is closed.
    //
    while (isActive) {
      LoggingEvent[] events = null;

      //
      //   extract pending events while synchronized
      //       on buffer
      //
      synchronized (buffer) {
        int bufferSize = buffer.size();
        isActive = !parent.closed;

        while ((bufferSize == 0) && isActive) {
          buffer.wait();
          bufferSize = buffer.size();
          isActive = !parent.closed;
        }

        if (bufferSize > 0) {
          events = new LoggingEvent[bufferSize + discardMap.size()];
          buffer.toArray(events);

          //
          //   add events due to buffer overflow
          //
          int index = bufferSize;

          for (
            Iterator iter = discardMap.values().iterator();
              iter.hasNext();) {
            events[index++] = ((DiscardSummary) iter.next()).createEvent();
          }

          //
          //    clear buffer and discard map
          //
          buffer.clear();
          discardMap.clear();

          //
          //    allow blocked appends to continue
          buffer.notifyAll();
        }
      }

      //
      //   process events after lock on buffer is released.
      //
      if (events != null) {
        for (int i = 0; i < events.length; i++) {
          synchronized (appenders) {
            appenders.appendLoopOnAppenders(events[i]);
          }
        }
      }
    }
  } catch (InterruptedException ex) {
    Thread.currentThread().interrupt();
  }
}
```

相比`ArrayBlockingQueue`实现，使用内置同步关键字的该实现更轻量级。
由于`ArrayBlockingQueue`默认使用非公平模式(java 的`synchronized`也没有保证公平行为)，甚至行为上二者行为上都是类似的。
唯一的区别可能就在于竞争时，线程状态是 `WAITING/TIMED_WAITING` 还是 `BLOCKING` 的区别。

### 无锁消息队列 disruptor

无锁消息队列 disruptor 原理参考美团的一篇博客[高性能队列——Disruptor](https://tech.meituan.com/disruptor.html)，本篇不做重复赘述。

## 4. 基于 disruptor 的异步日志实现

本`demo`项目目的在于实现一个`DemoAsyncLogger`实现对接`slf4j`，假设我们的日志事件定义如下:

```
public class LogEvent {

    public Level level;

    /**
     * 这里让框架日志生成
     */
    public Supplier<String> msgSupplier;
}
```

> 实现日志工厂

日志工厂中需要读取配置文件，`slf4j-api` 中提供了一套解析配置的工具类，本文不对配置进行深入讨论，仅介绍一下`disruptor`的使用

```
/**
 * Logger的单例工厂，读取日志系统配置，并对日志落盘行为进行统一管理
 */
public enum DemoAsyncLoggerFactory implements ILoggerFactory {

    /**
     * 工厂单例
     */
    INSTANCE;

    /**
     * 异步落盘线程的执行队列，使用了无锁内存队列进行日志事件的管理
     */
    private final Disruptor<LogEvent> disruptor;

    /**
     * 日志等级，实际的项目中会通过配置管理来约束这个level, 可以设置为进程参数或是其他配置管理策略
     */
    private final Level level = Level.TRACE;

    /**
     * 创建一个日志工厂单例，该工厂将统一
     */
    DemoAsyncLoggerFactory() {
        try {
            // 这里为一个DEMO, 正式实现中会定义完整读取配置的方式
            String file = String.format("/tmp/%s.log", UUID.randomUUID().toString());
            
            //初始化 disruptor 进程
            disruptor = new Disruptor<>(LogEvent::new, 1024, new LogThreadFactory(file));
            disruptor.handleEventsWith(new LogEventHandler(
                    new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)))));
            disruptor.start();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取 Logger 实例(Logger工厂方法)
     *
     * @param name 日志名称
     * @return Logger 实例
     */
    @Override
    public Logger getLogger(String name) {
        return new DemoAsyncLogger(name, disruptor);
    }

}
```

> 消息队列写入

```
public class DemoAsyncLogger implements Logger {

    //Override methods, ...

    private void asyncLog(Level level, String msg, Throwable err) {
        if (msg == null && err == null) {
            throw new IllegalArgumentException("both message and error are null");
        }
        StringBuilder msgBuilder = new StringBuilder();
        if (msg != null) {
            msgBuilder.append(msg).append("\n");
        }
        if (err != null) {
            msgBuilder.append(err.toString());
            for (StackTraceElement stackTrace : err.getStackTrace()) {
                msgBuilder.append(stackTrace).append("\n");
            }
        }
        msgBuilder.setLength(msgBuilder.length() - 1);
        asyncLog(level, msgBuilder.toString());
    }

    /**
     * 实际调用的事件的入队方法
     */
    private void asyncLog(Level level, String msg) {
        long sequence = disruptor.getRingBuffer().next();
        try {
            LogEvent event = disruptor.getRingBuffer().get(sequence);
            event.setLevel(level);
            event.setMsgSupplier(() -> String.format("%s\t%s", name, msg));
        } finally {
            disruptor.getRingBuffer().publish(sequence);
        }
    }
}
```

> 消息队列的消费

在工厂方法中我们看到了`Disruptor`在创建之后设置了一个handler用于在`Disruptor`工作线程中处理事件

```
public class LogEventHandler implements EventHandler<LogEvent> {

    private final PrintWriter printer;

    public LogEventHandler(PrintWriter printer) {
        this.printer = printer;
    }

    /**
     * 此函数会在`Disrtuptor`线程中调用, 因此在这里执行文件读写
     */
    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.printf("%d [%s] Thread %d-%s: %s%n",
                sequence,
                event.getLevel(),
                Thread.currentThread().getId(),
                Thread.currentThread().getName(),
                event);
        printer.printf("%d [%s] Thread %d-%s: %s%n",
                sequence,
                event.getLevel(),
                Thread.currentThread().getId(),
                Thread.currentThread().getName(),
                event);
        printer.flush();
    }

}
```

> 实现slf4j的接口绑定

第二章我们提到，slf4j需要我们实现类`StaticLoggerBinder`单例

```
/**
 * slf4j 实现对接的接口类
 */
public enum StaticLoggerBinder implements LoggerFactoryBinder {

    /**
     * Binder 单例
     */
    INSTANCE;

    /**
     * Logger Factory name
     */
    private static final String LOGGER_FACTORY_NAME = DemoAsyncLogger.class.getName();

    /**
     * StaticLoggerBinder 单例, slf4j-api 将调用该方法进行实现绑定
     *
     * @return StaticLoggerBinder实例
     * @see LoggerFactory#bind()
     */
    public static StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return DemoAsyncLoggerFactory.INSTANCE;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_NAME;
    }

}
```

> 调用测试

```
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws InterruptedException {
        LOG.trace("test");
        LOG.debug("test1");
        LOG.info("test2");
        Thread.sleep(100);
        LOG.warn("test2");
        LOG.error("test3");
    }
}

//--------------------------------
//--------------------------------

disruptor thread is started, output log file: /tmp/134fd4e4-7ecb-4b68-86cb-b4e8c1e335c5.log
0 [TRACE] Thread 12-demo-log: com.chpengzh.slf4j.Main	test
1 [DEBUG] Thread 12-demo-log: com.chpengzh.slf4j.Main	test1
2 [INFO] Thread 12-demo-log: com.chpengzh.slf4j.Main	test2
3 [WARN] Thread 12-demo-log: com.chpengzh.slf4j.Main	test2
4 [ERROR] Thread 12-demo-log: com.chpengzh.slf4j.Main	test3
```

## 5 小结与参考链接

本文主要是研究了日志框架`sfl4j`的接口实现层对接策略，以及异步日志框架的一般实现思路。
在一些本地进程锁竞争较为激烈的场景，使用无锁消息队列的解决方案成为了一些较为简单的解决方案。
同时，以`slf4j`为首的插件化思想也是值得参考的一种架构层设计模式。

- [SLF4J、Log4j、日志框架众](http://www.luohw.com/notes/slf4j-log4j-%E6%97%A5%E5%BF%97%E6%A1%86%E6%9E%B6%E4%BC%97.html)
- [《UNIX环境高级编程》](https://item.jd.com/11469694.html)
- [高性能队列——Disruptor](https://tech.meituan.com/disruptor.html)
