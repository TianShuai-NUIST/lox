package com.craftinginterpreters.lox;

import java.util.List;

// 解释器类
// 实现 1. 表达式访客模式的所有类
//      2. 语句访客模式的所有类
// 解释一下访客模式的函数是如何调用的, 答案是通过`accept()`调用的
// 所以会在如下两个情况调用
//  1. 执行一个语句
//  2. 对一个语句求值
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

  // 作用域定义
  private Environment environment = new Environment();

  // =========== 表达式访客模式实现 ===========

  // 字面量
  // literal    -> NUMBER | STRING | "true" | "false" | "nil" ;
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  // 组合
  // grouping   -> "(" expression ")" ;
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    // 组合是表达式的嵌套, 直接计算子表达式即可
    ///  **递归** 处理子表达式
    return evaluate(expr.expression); 
  }

  // 一元表达式
  // unary      -> ( "-" | "!" ) expression ;
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    ///  **递归** 处理子表达式
    Object right = evaluate(expr.right);

    switch(expr.operator.type) {
      case MINUS:
        // 负号只能作用于数字, 所以我们需要检查这个右操作数是不是Double
        checkNumberOperand(expr.operator, right);
        return -(double)right;
      case BANG:
        // isTruthy 返回类型一定是Boolean, 所以不需要查
        return !isTruthy(right);
    }
    return null;
  }

  // 获取变量的值
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  // 二元表达式
  // binary     -> expression operator expression ;
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    ///  **递归** 处理子表达式
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      // 大小比较只有数字可以
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double) right;

      // 相等判断 不需要类型检查
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);

      // 四则运算也只支持数字
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double) right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double) right;
      case PLUS:
        // 加法
        if(left instanceof Double && right instanceof Double){
          return (double)left + (double) right;
        }
        // 连接字符串
        if(left instanceof String && right instanceof String){
          return (String)left + (String) right;
        }
        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
    }
    return null;
  }

  // 执行赋值语句
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    // 先对右值求值
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

// =========== 语句访客模式实现 ===========

  // 表达式语句
  // exprStmt    -> expression ";" ;
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    // 表达式语句求值
    evaluate(stmt.expression);
    return null;
  }

  // 打印语句
  // printStmt   -> "print" expression ";" ;
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    // 打印对象求值
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  // 变量声明语句
  // varDecl     -> "var" IDENTIFIER ( "=" expression )? ";" ;
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    // 初始化器未必有
    if(stmt.initializer != null) {
      // 有的话求值
      value = evaluate(stmt.initializer);
    }

    // 定义变量
    /// 调用的是define函数不是assign函数, 二者不同
    /// 使用一个声明语句的时候, 哪怕上级有这个变量, 我们也会使用新的变量, 也就是**屏蔽**
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  // 语句块
  @Override
  public Void visitBlockStmt(Stmt.Block stmt){
    /// 执行语句, 语句块应该带有当前的上下文, 也就是**环境**
    /// 另外, 我们应该注意的是, 我们这里传入的环境是**子环境**, 子环境被使用**父环境**初始化了
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

// =========== 工具函数 ===========

  // 求值函数 (表达式)
  private Object evaluate(Expr expr) {
    // 这里的accept其实是一个递归, accept会调用expr中的子函数
    // expr会被具象化到Expr的子类 随后调用对应的Visitor函数
    // 而visit函数都是Expr中的接口, 在这个类被实例化
    // 所以最后调用的一定是上面的几个**访客模式**实例化的函数
    return expr.accept(this);
  }

  // 执行函数 (语句)
  private void execute(Stmt stmt) {
    // 同上
    stmt.accept(this);
  }

  // 判断真假
  private boolean isTruthy(Object object){
    // 在lox中, 只有nil和false是 '假' 其他的都是真
    if(object == null) return false;                      // nil   -> false
    if(object instanceof Boolean) return (boolean)object; // false -> false
    return true;
  }

  // 检查一元运算符
  private void checkNumberOperand(Token operator, Object operand){
    // 看看operand是不是Double的实例
    if(operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  // 检查二元运算符
  private void checkNumberOperands(Token operator, Object left, Object right){
    // left 和 right 都必须是 Double 的实例
    if(left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be numbers.");
  }

  // 执行语句块
  void executeBlock(List<Stmt> statements, Environment environment) {
    // 保存父环境
    /// NOTE: this 指向的是当前的Interpreter对象
    /// 也就是this.environment指向包含语句块的作用域
    Environment previous = this.environment;
    try {
      // 切换当当前环境(子环境)
      this.environment = environment;
      for(Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      // 切换环境回到父环境
      this.environment = previous;
    }
  }

  // 解释执行语句
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch(RuntimeError error) {
      /// 这里出了错误说明是在运行的时候出的错误, 所以是**运行时错误**
      Lox.runtimeError(error);
    }
  }

  // 表达式字符串化
  private String stringify(Object object) {
    // null -> nil
    if(object == null) return "nil";

    // 数字显示
    if(object instanceof Double) {
      String text = object.toString();
      // 整数不显示 ".0"
      if(text.endsWith(".0")){
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    // 其余类型直接显示即可
    return object.toString();
  }

  // 相等判断
  private boolean isEqual(Object a, Object b) {
    // 除非 都是空, 否则不相等
    if(a == null && b == null) return true;
    if(a == null) return false;

    return a.equals(b);
  }

}
