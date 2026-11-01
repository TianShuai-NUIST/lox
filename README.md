# lox

按《Crafting Interpreters》复刻的解释器。

## 进度

| 部分 | 状态 |
|---|---|
| jlox（Java 实现的树遍历解释器） | 词法扫描 → 递归下降解析 → AST 求值，全链路打通 |
| 作用域链 | Environment 嵌套 |
| 一等函数 | LoxCallable / LoxFunction |
| clox（C 实现的字节码虚拟机） | 进行中 |

## jlox 的几个点

文件执行和 REPL 两种入口最终调用同一个 `run`。解析器围绕一个 `current` 指针，靠 `advance()` / `match()` 移动，不在各处散落索引运算。

作用域是链状的：每进一个块就新建一层 Environment 指向父层，查找沿链上溯。闭包捕获的是环境对象，不是快照。

## 怎么跑

```bash
cd jlox && make run
```

## 已知限制

- 遇到语法错误直接抛，不做同步恢复
- 未解析变量只能在运行期报错
- clox 还没接上字节码生成
