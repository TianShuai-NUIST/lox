# Lox 语法表示

```c
// 程序
program         -> declaration* EOF ;

// 声明
declaration     -> varDecl | statement ;

// 变量声明语句
varDecl         -> "var" IDENTIFIER ( "=" expression )? ";" ;

// 语句
statement       -> exprStmt | forStmt | ifStmt |  printStmt | whileStmt | block ;

// if语句
ifStmt          -> "if" "(" expression ")" statement ( "else" statement)? ;

// for语句
forStmt         -> "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression? ")" statement ;

// while语句
whileStmt       -> "while" "(" expression ")" statement ;
// 语句块
block           -> "{" expression* "}" ;
// 表达式语句
exprStmt        -> expression ";" ;
// 打印语句
printStmt       -> "print" expression ";" ;

// 表达式
expression      -> assignment ;
// 赋值
assignment      -> IDENTIFIER "=" assignment | logic_or ;
// 逻辑或
logic_or        -> logic_and ( "or" logic_and )* ;
// 逻辑与
logic_and       -> equality ( "and" equality )* ;
// 相等
equality        -> comparison ( ( "!=" | "==" ) comparison )* ;
// 比较
comparison      -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// 加减
term            -> factor ( ( "-" | "+" ) factor )* ;
// 乘除
factor          -> unary ( ( "/" | "*" ) unary )* ;
// 一元运算符
unary           -> ( "!" | "-" ) unary | primary ;
// 基本表达式
primary         -> NUMBER | STRING // 字面量
  | "true" | "false" | "nil"     // 三值逻辑
  | "(" expression ")"           // 表达式
  | IDENTIFIER ;                 // 标识符
```

