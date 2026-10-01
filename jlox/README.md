# TODO: [6](https://craftinginterpreters.com/evaluating-expressions.html)

# Scanner

## 框架

首先我们先来打一个框架, 这个框架用于定义整个程序的执行规则.

Main函数用于分辨执行模式, 文件执行模式或者是命令执行模式. 但是这两个模式最终都收缩到`run`这个统一的执行函数上

> **先放大, 再缩小**

## 词法解析器

核心是一个指向所有的字符串的"指针", 他会解析一个个token. 工具函数都多是围绕我们自己制定的规则或者是这三个指针展开的.

自己的规则: string(), number(), 解析这个token的完整字符串, 解析这个token的完整数字.

指针: advance(), match(), ... 都是对current这个指针做移动操作.

## 一些感想

java是一个架构语言, 函数是生长出来的, 我在看着本书的时候, 意识到, 这个作者并不是写好函数内容, 然后再把它独立出去, 封装成一个函数, 而是当作这个这个函数已经存在了, 直接去使用它, 当功能完成之后, 在去补全这些函数. 这点让我受益匪浅.

# AST

## 访客模式

- [访客模式](https://refactoringguru.cn/design-patterns/visitor)

在OOP中实现类似于函数式编程语言的使用体验.

使用这个设计模式的原因在于OOP的编程体验的缺失, 我们有一个表格, 

# Parser Expression

对一个表达式进行解析
