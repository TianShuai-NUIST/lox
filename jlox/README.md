# TODO: [6](https://craftinginterpreters.com/evaluating-expressions.html)

# 4. Scanner

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

# 5. 表示代码

## BNF

- [巴科斯范式](https://zh.wikipedia.org/zh-cn/巴科斯范式)

BNF是一种上下文无关语法, 用于描述一类形式语言.
> **递归**是一个好标志, 用于表示定义的语言是上下文无关的.
> 特别是, 递归非终止符两边都有生成式的递归, 意味着语言不是正则的.


### 表达式的定义

```c
expression -> literal | unary | binary | grouping ;
// 字面量表达式
literal    -> NUMBER | STRING | "true" | "false" | "nil" ;
// 组合
grouping   -> "(" expression ")" ;
// 一元表达式
unary      -> ( "-" | "!" ) expression ;
// 二元表达式
binary     -> expression operator expression ;
// 操作符
operator   -> "==" | "!=" | "<" | "<=" | ">" | ">=" | "+" | "-" | "*" | "/" ;
```

但是这种方式定义的表达式, 他不具备运算符的优先级. 但是对于一个语法体系的构成是非常清楚的.

```c
// 表达式
expression -> equality ;
// 相等
equality   -> comparison ( ( "!=" | "==" ) comparison )* ;
// 比较
comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// 加减运算
term       -> factor ( ( "-" | "+" ) factor )* ;
// 乘除运算
factor     -> unary ( ( "/" | "*" ) unary )* ;
// 一元运算符
unary      -> ( "!" | "-" ) unary | primary ;
// 最低级运算
primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
```

这种方式构建了具有优先级的表达式, 我们注意到, 越早被表达式解释的, 这往往意味着他的优先级更低.

## 访客模式

- [访客模式](https://refactoringguru.cn/design-patterns/visitor)

在OOP中实现类似于函数式编程语言的使用体验.

使用这个设计模式的原因在于OOP的编程体验的缺失, 我们有一个表格, 

# 6. 解析表达式

对一个表达式进行解析

# 7. 表达式求值

# 8. 表达式和状态

定义程序如下

```c
program        → statement* EOF ;

statement      → exprStmt | printStmt ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
```

为了引入变量的作用域, 我们不得不修改定义如下

```c
program        → declaration* EOF ;

declaration    → varDecl 
               | statement ;

statement      → exprStmt 
               | printStmt ;
```

然后我们如下声明一个变量声明的定义
```c
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
```

为了访问变量, 我们需要重新定义基本类型.

```c
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```

现在变量可以被定义, 访问, 但是对于赋值的规则依然不够完善, 我们再次完善赋值的语法.

```c
expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | equality ;
```

现在我们可以添加语句块的语法了, 这样我们的程序就会支持语句块.

```c
statement      → exprStmt
               | printStmt
               | block ;

block          → "{" declaration* "}" ;
```
我们注意到, 我们的语句块是由一个`{}`包裹的语句组, 而里面的语句组是可以没有任何一个语句的.


