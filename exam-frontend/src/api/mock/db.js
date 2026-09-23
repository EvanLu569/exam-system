// Mock 数据库：使用 localStorage 持久化，首次加载时写入种子数据。
// 结构与接口文档「二、数据库表设计」一一对应：
// user / question / exam_paper / paper_question / exam_record / answer_record / wrong_question

const DB_KEY = 'exam_mock_db_v2'

function seed() {
  const users = [
    { id: 1, username: 'admin', password: '123456', role: 'admin', createTime: '2026-09-01T10:00:00' },
    { id: 2, username: 'student', password: '123456', role: 'student', createTime: '2026-09-01T10:00:00' },
    { id: 3, username: 'zhangsan', password: '123456', role: 'student', createTime: '2026-09-02T14:30:00' }
  ]

  // type: 1 单选 / 2 多选 / 3 判断；difficulty: 1 简单 / 2 中等 / 3 困难
  const questions = [
    // ---- Java 集合 ----
    { id: 1, content: '以下哪个是 Java 集合框架中的接口？', options: ['List', 'Thread', 'Servlet', 'Socket'], correctAnswer: 'A', score: 5, type: 1, difficulty: 1, knowledgePoint: '集合', createTime: '2026-09-10T10:00:00' },
    { id: 2, content: 'ArrayList 的底层数据结构是？', options: ['数组', '链表', '哈希表', '红黑树'], correctAnswer: 'A', score: 5, type: 1, difficulty: 1, knowledgePoint: '集合', createTime: '2026-09-10T10:00:00' },
    { id: 3, content: '关于 HashMap，下列说法正确的是？', options: ['键可以重复', '键和值均可以为 null', '键必须实现 Comparable', '线程安全'], correctAnswer: 'B', score: 5, type: 1, difficulty: 2, knowledgePoint: '集合', createTime: '2026-09-10T10:00:00' },
    { id: 4, content: '以下哪些属于 Map 接口的实现类？', options: ['HashMap', 'TreeMap', 'ArrayList', 'Hashtable'], correctAnswer: 'ABD', score: 10, type: 2, difficulty: 2, knowledgePoint: '集合', createTime: '2026-09-10T10:00:00' },
    { id: 5, content: '关于 HashMap 与 Hashtable 的区别，正确的是？', options: ['HashMap 线程不安全', 'Hashtable 线程安全', 'HashMap 允许 null 键值', 'Hashtable 允许 null 键值'], correctAnswer: 'ABC', score: 10, type: 2, difficulty: 3, knowledgePoint: '集合', createTime: '2026-09-10T10:00:00' },

    // ---- Java 基础语法 ----
    { id: 6, content: 'Java 中用于定义常量的关键字是？', options: ['final', 'static', 'const', 'abstract'], correctAnswer: 'A', score: 5, type: 1, difficulty: 1, knowledgePoint: '基础语法', createTime: '2026-09-10T10:00:00' },
    { id: 7, content: '下列哪个不是 Java 的基本数据类型？', options: ['int', 'String', 'boolean', 'char'], correctAnswer: 'B', score: 5, type: 1, difficulty: 1, knowledgePoint: '基础语法', createTime: '2026-09-10T10:00:00' },
    { id: 8, content: '关于构造方法，说法正确的是？', options: ['构造方法名与类名相同', '构造方法有返回值', '构造方法不能被重载', '构造方法不是方法'], correctAnswer: 'A', score: 5, type: 1, difficulty: 2, knowledgePoint: '基础语法', createTime: '2026-09-10T10:00:00' },
    { id: 9, content: 'String 类是不可变的。', options: null, correctAnswer: 'T', score: 5, type: 3, difficulty: 1, knowledgePoint: '基础语法', createTime: '2026-09-10T10:00:00' },
    { id: 10, content: '一个 Java 源文件中可以定义多个 public 类。', options: null, correctAnswer: 'F', score: 5, type: 3, difficulty: 1, knowledgePoint: '基础语法', createTime: '2026-09-10T10:00:00' },

    // ---- Java 多线程 ----
    { id: 11, content: '创建线程的方式不包括？', options: ['继承 Thread', '实现 Runnable', '实现 Callable', '继承 Exception'], correctAnswer: 'D', score: 5, type: 1, difficulty: 1, knowledgePoint: '多线程', createTime: '2026-09-10T10:00:00' },
    { id: 12, content: 'synchronized 关键字的主要作用是？', options: ['保证线程安全', '提高执行性能', '使对象不可变', '实现类继承'], correctAnswer: 'A', score: 5, type: 1, difficulty: 2, knowledgePoint: '多线程', createTime: '2026-09-10T10:00:00' },
    { id: 13, content: '以下哪些是实现线程同步的方式？', options: ['synchronized', 'ReentrantLock', 'volatile', 'static 关键字'], correctAnswer: 'ABC', score: 10, type: 2, difficulty: 2, knowledgePoint: '多线程', createTime: '2026-09-10T10:00:00' },
    { id: 14, content: 'volatile 关键字能保证操作的原子性。', options: null, correctAnswer: 'F', score: 5, type: 3, difficulty: 2, knowledgePoint: '多线程', createTime: '2026-09-10T10:00:00' },

    // ---- JVM ----
    { id: 15, content: 'Java 源文件编译后生成的文件是？', options: ['.class', '.java', '.jar', '.exe'], correctAnswer: 'A', score: 5, type: 1, difficulty: 1, knowledgePoint: 'JVM', createTime: '2026-09-10T10:00:00' },
    { id: 16, content: 'JVM 的垃圾回收主要针对哪个区域？', options: ['堆', '栈', '方法区', '程序计数器'], correctAnswer: 'A', score: 5, type: 1, difficulty: 2, knowledgePoint: 'JVM', createTime: '2026-09-10T10:00:00' },
    { id: 17, content: '关于 JVM，下列说法正确的是？', options: ['负责执行字节码', '是 Java 跨平台的基础', '可以直接执行 C 语言代码', '不同平台有对应的 JVM 实现'], correctAnswer: 'ABD', score: 10, type: 2, difficulty: 1, knowledgePoint: 'JVM', createTime: '2026-09-10T10:00:00' },
    { id: 18, content: 'JVM 是 Java 跨平台的基础。', options: null, correctAnswer: 'T', score: 5, type: 3, difficulty: 1, knowledgePoint: 'JVM', createTime: '2026-09-10T10:00:00' },

    // ---- 数据结构 ----
    { id: 19, content: '栈的特点是？', options: ['先进先出', '后进先出', '随机存取', '只能存取一端'], correctAnswer: 'B', score: 5, type: 1, difficulty: 1, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 20, content: '队列的特点是？', options: ['先进先出', '后进先出', '随机存取', '只能存取一端'], correctAnswer: 'A', score: 5, type: 1, difficulty: 1, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 21, content: '二叉树第 i 层最多有多少个结点？', options: ['2^(i-1)', '2^i', '2^i - 1', 'i'], correctAnswer: 'A', score: 5, type: 1, difficulty: 2, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 22, content: '下列属于非线性结构的是？', options: ['线性表', '栈', '树', '队列'], correctAnswer: 'C', score: 5, type: 1, difficulty: 2, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 23, content: '以下属于线性结构的是？', options: ['顺序表', '链表', '栈', '树'], correctAnswer: 'ABC', score: 10, type: 2, difficulty: 2, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 24, content: '关于链表，说法正确的是？', options: ['插入删除方便', '支持随机访问', '需要额外存储指针', '长度固定'], correctAnswer: 'AC', score: 10, type: 2, difficulty: 1, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 25, content: '栈是一种先进先出的数据结构。', options: null, correctAnswer: 'F', score: 5, type: 3, difficulty: 1, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' },
    { id: 26, content: '哈希表查找的平均时间复杂度是 O(1)。', options: null, correctAnswer: 'T', score: 5, type: 3, difficulty: 2, knowledgePoint: '数据结构', createTime: '2026-09-10T10:00:00' }
  ]

  const papers = [
    { id: 1, name: 'Java 基础摸底考试', durationMinutes: 60, totalScore: 100, startTime: '2026-09-18T08:00:00', endTime: '2026-12-31T23:59:59', status: 1, createTime: '2026-09-15T10:00:00' },
    { id: 2, name: '数据结构期中考试', durationMinutes: 45, totalScore: 50, startTime: '2026-10-01T08:00:00', endTime: '2026-10-01T18:00:00', status: 0, createTime: '2026-09-15T10:00:00' },
    { id: 3, name: 'Java 综合模拟卷', durationMinutes: 90, totalScore: 100, startTime: '2026-09-18T08:00:00', endTime: '2026-12-31T23:59:59', status: 1, createTime: '2026-09-15T10:00:00' }
  ]

  // paper_question 关联：questionId + 该卷分值 + 排序
  const paperQuestions = []

  function link(paperId, list) {
    list.forEach((item, i) => {
      const questionId = Array.isArray(item) ? item[0] : item.questionId
      const score = Array.isArray(item) ? item[1] : item.score
      const q = questions.find((x) => x.id === questionId)
      paperQuestions.push({
        id: paperQuestions.length + 1,
        paperId,
        questionId,
        score: score ?? q?.score ?? 5,
        sortOrder: i + 1
      })
    })
  }

  // 试卷 1：Java 基础摸底考试（单选 8×5 + 多选 3×10 + 判断 4×5 = 90 分）
  link(1, [
    [1], [2], [6], [7], [8], [11], [12], [15],
    [4], [13], [17],
    [9], [10], [14], [18]
  ])
  // 试卷 2：数据结构期中考试（单选 4×5 + 多选 2×10 + 判断 2×5 = 50 分）
  link(2, [[19], [20], [21], [22], [23], [24], [25], [26]])
  // 试卷 3：Java 综合模拟卷
  link(3, [[1], [3], [7], [8], [12], [16], [4], [5], [13], [17], [9], [14], [18]])

  return {
    users,
    questions,
    papers,
    paperQuestions,
    records: [],
    answerRecords: [],
    wrongQuestions: []
  }
}

function load() {
  const raw = localStorage.getItem(DB_KEY)
  if (raw) {
    try {
      return JSON.parse(raw)
    } catch {
      // 数据损坏则重建
    }
  }
  const db = seed()
  save(db)
  return db
}

function save(db) {
  localStorage.setItem(DB_KEY, JSON.stringify(db))
}

export function getDB() {
  return load()
}

export function saveDB(db) {
  save(db)
}

export function resetDB() {
  localStorage.removeItem(DB_KEY)
  localStorage.removeItem('exam_token')
  localStorage.removeItem('exam_user')
  return load()
}

export function nextId(list) {
  return list.length ? Math.max(...list.map((i) => i.id)) + 1 : 1
}
